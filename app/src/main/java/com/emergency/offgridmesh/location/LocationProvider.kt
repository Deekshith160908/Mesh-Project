package com.emergency.offgridmesh.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class GpsLocationData(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val accuracy: Float,
    val timestamp: Long
)

class LocationProvider(private val context: Context) {

    companion object {
        private const val TAG = "LocationProvider"
        private const val UPDATE_INTERVAL_MS = 10000L
        private const val FASTEST_INTERVAL_MS = 5000L
    }

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @Volatile
    var lastKnownLocation: GpsLocationData? = null
        private set

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): GpsLocationData? {
        return suspendCancellableCoroutine { continuation ->
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                    if (loc != null) {
                        val data = GpsLocationData(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            altitude = loc.altitude,
                            accuracy = loc.accuracy,
                            timestamp = loc.time
                        )
                        lastKnownLocation = data
                        continuation.resume(data)
                    } else {
                        requestSingleFreshLocation { freshLoc ->
                            if (freshLoc != null) {
                                lastKnownLocation = freshLoc
                            }
                            continuation.resume(freshLoc)
                        }
                    }
                }.addOnFailureListener { e ->
                    Log.w(TAG, "Failed to get lastLocation: ${e.message}")
                    continuation.resume(lastKnownLocation)
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Location permission not granted: ${e.message}")
                continuation.resume(null)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestSingleFreshLocation(onResult: (GpsLocationData?) -> Unit) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMaxUpdates(1)
            .setDurationMillis(5000L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                fusedLocationClient.removeLocationUpdates(this)
                val loc = result.lastLocation
                if (loc != null) {
                    val data = GpsLocationData(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        altitude = loc.altitude,
                        accuracy = loc.accuracy,
                        timestamp = loc.time
                    )
                    onResult(data)
                } else {
                    onResult(null)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting fresh location: ${e.message}")
            onResult(null)
        }
    }

    @SuppressLint("MissingPermission")
    fun getLocationFlow(): Flow<GpsLocationData> = callbackFlow {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (loc in result.locations) {
                    val data = GpsLocationData(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        altitude = loc.altitude,
                        accuracy = loc.accuracy,
                        timestamp = loc.time
                    )
                    lastKnownLocation = data
                    trySend(data)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException in getLocationFlow: ${e.message}")
            close(e)
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }
}
