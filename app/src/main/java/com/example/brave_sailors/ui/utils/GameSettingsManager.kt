package com.example.brave_sailors.ui.utils

import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.brave_sailors.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manages Game Options: Alarm (Flashlight), Music, and Vibration.
 */
class GameSettingsManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("game_options", Context.MODE_PRIVATE)

    // Statico per evitare che si creino più player sovrapposti
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
            // [ FIX ]: Rimosso manageMusic(value) qui.
            // La musica non deve partire nel menu opzioni, ma solo in partita.
        }

    var isVibrationOn: Boolean
        get() = prefs.getBoolean("vibration_on", true)
        set(value) = prefs.edit().putBoolean("vibration_on", value).apply()

    // -- HARDWARE LOGIC --

    /**
     * Blinks the torch (flashlight) briefly if Alarm is ON.
     */
    fun triggerTurnAlarm() {
        if (!isAlarmOn) return

        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0] // Usually back camera

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        cameraManager.setTorchMode(cameraId, true)
                        delay(300) // Blink duration
                        cameraManager.setTorchMode(cameraId, false)
                    }
                } catch (e: Exception) {
                    // Ignora errori fotocamera (es. in uso da altra app)
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace() // Camera non disponibile
        }
    }

    /**
     * Vibrates the device briefly if Vibration is ON.
     */
    fun triggerVibration() {
        if (!isVibrationOn) return

        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(200)
                }
            }
        } catch (e: Exception) {
            // [ FIX ]: Catch per evitare crash se manca il permesso o l'hardware
            e.printStackTrace()
        }
    }

    /**
     * Manages background music playback.
     */
    fun manageMusic(shouldPlay: Boolean) {
        try {
            if (shouldPlay) {
                if (mediaPlayer == null) {
                    // Crea il player solo se non esiste
                    mediaPlayer = MediaPlayer.create(context, R.raw.game_music)
                    mediaPlayer?.isLooping = true
                    mediaPlayer?.start()
                } else {
                    // Se esiste ma è in pausa, riprendi
                    if (mediaPlayer?.isPlaying == false) {
                        mediaPlayer?.start()
                    }
                }
            } else {
                // Metti in pausa
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Chiamato quando si esce dalla schermata della partita (onCleared del ViewModel)
    fun releaseMusic() {
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
    }
}