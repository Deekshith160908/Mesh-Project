package com.emergency.offgridmesh.alert

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class EmergencyAlertManager(private val context: Context) {

    companion object {
        private const val TAG = "EmergencyAlertManager"

        // SOS Morse Code Timing in Milliseconds:
        // dot = 200ms, dash = 600ms, inner gap = 150ms, letter gap = 400ms, word gap = 1000ms
        private val SOS_VIBRATION_PATTERN = longArrayOf(
            0,
            200, 150, 200, 150, 200, // S: . . .
            400,
            600, 150, 600, 150, 600, // O: - - -
            400,
            200, 150, 200, 150, 200, // S: . . .
            1000
        )
    }

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val cameraManager: CameraManager? =
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    private var torchCameraId: String? = null
    private var strobeJob: Job? = null
    private var sirenJob: Job? = null
    private val alertScope = CoroutineScope(Dispatchers.Default)

    @Volatile
    var isAlertActive: Boolean = false
        private set

    init {
        findTorchCameraId()
    }

    private fun findTorchCameraId() {
        try {
            cameraManager?.let { manager ->
                for (id in manager.cameraIdList) {
                    val characteristics = manager.getCameraCharacteristics(id)
                    val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                        torchCameraId = id
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to find flashlight camera ID: ${e.message}")
        }
    }

    fun triggerEmergencyAlert(enableSiren: Boolean = true, enableTorchStrobe: Boolean = true) {
        if (isAlertActive) return
        isAlertActive = true
        startSosVibration()
        if (enableSiren) {
            startSirenSound()
        }
        if (enableTorchStrobe) {
            startTorchStrobe()
        }
    }

    fun stopEmergencyAlert() {
        isAlertActive = false
        stopSosVibration()
        sirenJob?.cancel()
        sirenJob = null
        strobeJob?.cancel()
        strobeJob = null
        setTorchState(false)
    }

    private fun startSosVibration() {
        try {
            vibrator?.let { vib ->
                if (vib.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val effect = VibrationEffect.createWaveform(SOS_VIBRATION_PATTERN, 0)
                        val attributes = AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build()
                        vib.vibrate(effect, attributes)
                    } else {
                        @Suppress("DEPRECATION")
                        vib.vibrate(SOS_VIBRATION_PATTERN, 0)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration failed: ${e.message}")
        }
    }

    private fun stopSosVibration() {
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Stop vibration failed: ${e.message}")
        }
    }

    private fun startSirenSound() {
        sirenJob?.cancel()
        sirenJob = alertScope.launch {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                while (isActive && isAlertActive) {
                    toneGen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 700)
                    delay(800)
                    toneGen.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 700)
                    delay(800)
                }
                toneGen.release()
            } catch (e: Exception) {
                Log.e(TAG, "Audio tone generation failed: ${e.message}")
            }
        }
    }

    private fun startTorchStrobe() {
        if (torchCameraId == null) return
        strobeJob?.cancel()
        strobeJob = alertScope.launch {
            try {
                while (isActive && isAlertActive) {
                    // S: 3 short flashes
                    repeat(3) {
                        setTorchState(true)
                        delay(150)
                        setTorchState(false)
                        delay(100)
                    }
                    delay(300)

                    // O: 3 long flashes
                    repeat(3) {
                        setTorchState(true)
                        delay(450)
                        setTorchState(false)
                        delay(100)
                    }
                    delay(300)

                    // S: 3 short flashes
                    repeat(3) {
                        setTorchState(true)
                        delay(150)
                        setTorchState(false)
                        delay(100)
                    }
                    delay(1000)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Strobe routine exception: ${e.message}")
            } finally {
                setTorchState(false)
            }
        }
    }

    private fun setTorchState(enabled: Boolean) {
        val camId = torchCameraId ?: return
        try {
            cameraManager?.setTorchMode(camId, enabled)
        } catch (e: CameraAccessException) {
            Log.w(TAG, "CameraAccessException toggling torch: ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "Exception toggling torch: ${e.message}")
        }
    }
}
