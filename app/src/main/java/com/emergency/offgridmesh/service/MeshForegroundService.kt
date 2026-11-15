package com.emergency.offgridmesh.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.emergency.offgridmesh.MainActivity
import com.emergency.offgridmesh.MeshApplication
import com.emergency.offgridmesh.R
import com.emergency.offgridmesh.alert.EmergencyAlertManager
import com.emergency.offgridmesh.cloud.CloudUplinkSyncManager
import com.emergency.offgridmesh.data.model.PeerNode
import com.emergency.offgridmesh.data.repository.MeshRepository
import com.emergency.offgridmesh.location.LocationProvider
import com.emergency.offgridmesh.mesh.MultiHopRouter
import com.emergency.offgridmesh.mesh.NearbyMeshManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MeshForegroundService : Service() {

    companion object {
        private const val TAG = "MeshForegroundService"
        private const val CHANNEL_ID = "mesh_service_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START_SERVICE = "com.emergency.offgridmesh.ACTION_START"
        const val ACTION_STOP_SERVICE = "com.emergency.offgridmesh.ACTION_STOP"
        const val ACTION_BROADCAST_SOS = "com.emergency.offgridmesh.ACTION_SOS"

        fun startService(context: Context) {
            val intent = Intent(context, MeshForegroundService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, MeshForegroundService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }

    inner class MeshBinder : Binder() {
        fun getService(): MeshForegroundService = this@MeshForegroundService
    }

    private val binder = MeshBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var repository: MeshRepository
    lateinit var locationProvider: LocationProvider
    lateinit var alertManager: EmergencyAlertManager
    lateinit var cloudSyncManager: CloudUplinkSyncManager
    lateinit var meshRouter: MultiHopRouter
    lateinit var nearbyMeshManager: NearbyMeshManager

    private var isServiceRunning = false

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Initializing MeshForegroundService...")
        val app = application as MeshApplication

        repository = app.repository
        locationProvider = app.locationProvider
        alertManager = app.alertManager
        cloudSyncManager = app.cloudSyncManager
        meshRouter = app.meshRouter

        nearbyMeshManager = NearbyMeshManager(
            context = this,
            onPayloadReceivedListener = { bytes, endpointId ->
                meshRouter.handleIncomingPayload(bytes, endpointId)
            },
            onPeerStatusChangedListener = { endpointId, name, isConnected ->
                serviceScope.launch(Dispatchers.IO) {
                    if (isConnected) {
                        repository.upsertPeer(
                            PeerNode(
                                endpointId = endpointId,
                                deviceName = name,
                                isConnected = true,
                                lastSeenTimestamp = System.currentTimeMillis()
                            )
                        )
                    } else {
                        repository.markPeerDisconnected(endpointId)
                    }
                    updateNotification()
                }
            }
        )

        meshRouter.nearbyMeshManager = nearbyMeshManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVICE -> {
                if (!isServiceRunning) {
                    startMeshForeground()
                }
            }
            ACTION_STOP_SERVICE -> {
                stopMeshForeground()
            }
            ACTION_BROADCAST_SOS -> {
                serviceScope.launch(Dispatchers.IO) {
                    meshRouter.createAndBroadcastSos("EMERGENCY DISTRESS BEACON FROM NOTIFICATION")
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun startMeshForeground() {
        isServiceRunning = true
        val notification = buildNotification(0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            }
            startForeground(NOTIFICATION_ID, notification, foregroundServiceType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        nearbyMeshManager.startMesh()
        cloudSyncManager.startMonitoring()

        serviceScope.launch {
            repository.connectedPeers.collectLatest { peers ->
                updateNotification(peers.size)
            }
        }
    }

    private fun stopMeshForeground() {
        isServiceRunning = false
        nearbyMeshManager.stopMesh()
        cloudSyncManager.stopMonitoring()
        alertManager.stopEmergencyAlert()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_description)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(peerCount: Int = 0): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val sosIntent = Intent(this, MeshForegroundService::class.java).apply {
            action = ACTION_BROADCAST_SOS
        }
        val sosPendingIntent = PendingIntent.getService(
            this,
            1,
            sosIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val peerStatusText = if (peerCount == 0) {
            "Searching for nearby off-grid peers..."
        } else {
            "Connected to $peerCount mesh node(s) | Relaying signals"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Off-Grid SOS Mesh Active")
            .setContentText(peerStatusText)
            .setSmallIcon(R.drawable.ic_sos)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(R.drawable.ic_sos, "BROADCAST SOS", sosPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(peerCount: Int = nearbyMeshManager.getConnectedEndpointsCount()) {
        if (!isServiceRunning) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildNotification(peerCount))
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        nearbyMeshManager.stopMesh()
        cloudSyncManager.stopMonitoring()
        Log.i(TAG, "MeshForegroundService destroyed.")
    }
}
