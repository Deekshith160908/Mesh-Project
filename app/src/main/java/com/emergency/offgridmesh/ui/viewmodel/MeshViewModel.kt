package com.emergency.offgridmesh.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emergency.offgridmesh.alert.EmergencyAlertManager
import com.emergency.offgridmesh.cloud.CloudUplinkSyncManager
import com.emergency.offgridmesh.data.model.MeshEvent
import com.emergency.offgridmesh.data.model.MeshPacket
import com.emergency.offgridmesh.data.model.PeerNode
import com.emergency.offgridmesh.data.repository.MeshRepository
import com.emergency.offgridmesh.location.GpsLocationData
import com.emergency.offgridmesh.location.LocationProvider
import com.emergency.offgridmesh.mesh.MultiHopRouter
import com.emergency.offgridmesh.service.MeshForegroundService
import com.emergency.offgridmesh.util.DeviceIdProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MeshViewModel(
    private val context: Context,
    private val repository: MeshRepository,
    private val locationProvider: LocationProvider,
    private val alertManager: EmergencyAlertManager,
    private val cloudSyncManager: CloudUplinkSyncManager,
    private val meshRouter: MultiHopRouter
) : ViewModel() {

    val localNodeId: String = DeviceIdProvider.getDeviceId(context)
    val localNodeAlias: String = DeviceIdProvider.getNodeAlias(context)

    val allPackets: StateFlow<List<MeshPacket>> = repository.allPackets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sosAlerts: StateFlow<List<MeshPacket>> = repository.sosAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val textMessages: StateFlow<List<MeshPacket>> = repository.textMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val connectedPeers: StateFlow<List<PeerNode>> = repository.connectedPeers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isInternetAvailable: StateFlow<Boolean> = cloudSyncManager.isInternetAvailable
    val syncedPacketsCount: StateFlow<Int> = cloudSyncManager.syncedPacketsCount

    private val _currentLocation = MutableStateFlow<GpsLocationData?>(null)
    val currentLocation: StateFlow<GpsLocationData?> = _currentLocation.asStateFlow()

    private val _isAlarmActive = MutableStateFlow(false)
    val isAlarmActive: StateFlow<Boolean> = _isAlarmActive.asStateFlow()

    private val _sosCountdown = MutableStateFlow<Int?>(null)
    val sosCountdown: StateFlow<Int?> = _sosCountdown.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    private var countdownJob: Job? = null

    init {
        startLocationUpdates()
        observeMeshEvents()
    }

    private fun startLocationUpdates() {
        viewModelScope.launch {
            _currentLocation.value = locationProvider.getCurrentLocation()
            locationProvider.getLocationFlow().collect { loc ->
                _currentLocation.value = loc
            }
        }
    }

    private fun observeMeshEvents() {
        viewModelScope.launch {
            meshRouter.meshEvents.collect { event ->
                when (event) {
                    is MeshEvent.SosAlertTriggered -> {
                        _isAlarmActive.value = true
                        _userMessage.emit("SOS BEACON RECEIVED FROM: ${event.packet.senderId}")
                    }
                    is MeshEvent.PacketRelayed -> {
                        _userMessage.emit("Multi-hop relay: Packet forwarded (Hop #${event.relayHopCount})")
                    }
                    is MeshEvent.PeerConnected -> {
                        _userMessage.emit("New peer joined mesh: ${event.deviceName}")
                    }
                    is MeshEvent.PeerDisconnected -> {
                        _userMessage.emit("Peer left mesh: ${event.endpointId}")
                    }
                    else -> Unit
                }
            }
        }
    }

    fun startSosCountdown(customMessage: String = "") {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (i in 3 downTo 1) {
                _sosCountdown.value = i
                delay(1000)
            }
            _sosCountdown.value = null
            triggerSosBeacon(customMessage)
        }
    }

    fun cancelSosCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        _sosCountdown.value = null
    }

    fun triggerSosBeacon(customMessage: String = "") {
        viewModelScope.launch {
            val packet = meshRouter.createAndBroadcastSos(customMessage)
            alertManager.triggerEmergencyAlert(enableSiren = true, enableTorchStrobe = true)
            _isAlarmActive.value = true
            _userMessage.emit("Emergency SOS broadcasted across mesh (Packet ID: ${packet.packetId.take(8)})")
        }
    }

    fun stopAlarm() {
        alertManager.stopEmergencyAlert()
        _isAlarmActive.value = false
    }

    fun sendTextMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            meshRouter.createAndBroadcastTextMessage(text.trim())
        }
    }

    fun refreshLocation() {
        viewModelScope.launch {
            _currentLocation.value = locationProvider.getCurrentLocation()
        }
    }

    fun triggerCloudSync() {
        cloudSyncManager.triggerOpportunisticSync()
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAllPackets()
            _userMessage.emit("Cleared all local mesh packets.")
        }
    }

    fun startService(context: Context) {
        MeshForegroundService.startService(context)
    }

    fun stopService(context: Context) {
        MeshForegroundService.stopService(context)
    }
}
