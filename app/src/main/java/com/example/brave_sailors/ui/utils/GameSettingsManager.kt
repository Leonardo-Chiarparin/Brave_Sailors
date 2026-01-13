package com.example.brave_sailors.ui.utils

import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.brave_sailors.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manages Game Options: Alarm (Flashlight), Music, and Vibration.
 */
class GameSettingsManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("game_options", Context.MODE_PRIVATE)

    private var isAppPaused = false

    private var isGameRunning = false

    private var alarmJob: Job? = null

    companion object {
        private var mediaPlayer: MediaPlayer? = null
    }

    // -- SETTINGS PROPERTIES --
    var isAlarmOn: Boolean
        get() = prefs.getBoolean("alarm_on", true)
        set(value) = prefs.edit().putBoolean("alarm_on", value).apply()

    var isMusicOn: Boolean
        get() = prefs.getBoolean("music_on", false)
        set(value) {
            prefs.edit().putBoolean("music_on", value).apply()
            if (!isAppPaused && isGameRunning)
                manageMusic(value)
            else if (!value)
                manageMusic(false)
        }

    var isVibrationOn: Boolean
        get() = prefs.getBoolean("vibration_on", true)
        set(value) = prefs.edit().putBoolean("vibration_on", value).apply()

    fun onAppPause() {
        isAppPaused = true

        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        }

        try {
            getVibrator().cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        stopTorch()
    }

    fun onAppResume() {
        isAppPaused = false

        if (isMusicOn && isGameRunning)
            manageMusic(true)
    }

    // -- HARDWARE LOGIC --

    /**
     * Blinks the torch (flashlight) briefly if Alarm is ON.
     */
    fun triggerTurnAlarm() {
        if (!isAlarmOn || isAppPaused) return

        stopTorch()

        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0] // Usually back camera

            alarmJob = CoroutineScope(Dispatchers.Main).launch {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        cameraManager.setTorchMode(cameraId, true)
                        delay(300) // Blink duration
                        if (!isAppPaused) cameraManager.setTorchMode(cameraId, false)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Vibrates the device briefly if Vibration is ON.
     */
    fun triggerVibration() {
        if (!isVibrationOn || isAppPaused) return

        try {
            val vibrator = getVibrator()

            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(200)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Manages background music playback.
     */
    fun manageMusic(shouldPlay: Boolean) {
        if (shouldPlay) isGameRunning = true

        if (isAppPaused && shouldPlay) return

        try {
            if (shouldPlay) {
                if (mediaPlayer == null) {
                    mediaPlayer = MediaPlayer.create(context, R.raw.game_music)
                    mediaPlayer?.isLooping = true
                    mediaPlayer?.start()
                } else {
                    if (mediaPlayer?.isPlaying == false) {
                        mediaPlayer?.start()
                    }
                }
            } else {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun releaseResources() {
        isGameRunning = false

        // 1. Stop Music
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer!!.isPlaying) {
                    mediaPlayer?.stop()
                }
                mediaPlayer?.release()
                mediaPlayer = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Stop Vibration (Cancel any ongoing effect)
        try {
            getVibrator().cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        stopTorch()
    }

    private fun stopTorch() {
        try {
            alarmJob?.cancel()
            alarmJob = null
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            if (cameraManager.cameraIdList.isNotEmpty()) {
                val cameraId = cameraManager.cameraIdList[0]
                cameraManager.setTorchMode(cameraId, false)
            }
        } catch (e: Exception) {
            Log.e("GameSettings", "Torch error: ${e.message}")
        }
    }

    private fun getVibrator(): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
}