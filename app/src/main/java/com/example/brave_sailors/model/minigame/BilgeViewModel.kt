package com.example.brave_sailors.model.minigame

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.sqrt

data class BilgeUiState(
    val status: TorpedoStatus = TorpedoStatus.RUNNING,
    val waterLevel: Float = 0.4f,
    val timeRemaining: Long = 15L
)

class BilgeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(BilgeUiState())
    val uiState = _uiState.asStateFlow()

    private var startTime = 0L
    private val gameDuration = 15L

    private val baseRisingSpeed = 0.0035f
    private val pumpPower = 0.065f
    private val shakeThreshold = 13.5f
    private val pumpCooldown = 130L

    private var lastPumpTime = 0L

    fun startGame() {
        startTime = System.currentTimeMillis()
        lastPumpTime = 0L
        _uiState.update { BilgeUiState(status = TorpedoStatus.RUNNING, waterLevel = 0.4f, timeRemaining = gameDuration) }
    }

    fun updateFrame(accelX: Float, accelY: Float, accelZ: Float) {
        if (_uiState.value.status != TorpedoStatus.RUNNING) return

        val currentTime = System.currentTimeMillis()
        val elapsed = (currentTime - startTime) / 1000
        val remaining = gameDuration - elapsed

        val acceleration = sqrt(accelX*accelX + accelY*accelY + accelZ*accelZ)

        var pumpEffect = 0f
        if (acceleration > shakeThreshold) {
            if (currentTime - lastPumpTime > pumpCooldown) {
                pumpEffect = pumpPower
                lastPumpTime = currentTime
            }
        }

        val stressFactor = 1.0f + (elapsed.toFloat() / gameDuration.toFloat()) * 1.5f
        val currentRisingSpeed = baseRisingSpeed * stressFactor

        var newLevel = _uiState.value.waterLevel + currentRisingSpeed - pumpEffect
        if (newLevel < 0f) newLevel = 0f

        if (newLevel >= 1.0f) {
            _uiState.update { it.copy(status = TorpedoStatus.LOST, waterLevel = 1.0f) }
            return
        }

        if (remaining <= 0) {
            _uiState.update { it.copy(status = TorpedoStatus.WON, timeRemaining = 0) }
            return
        }

        _uiState.update { it.copy(waterLevel = newLevel, timeRemaining = remaining) }
    }

    fun resetGame() { startGame() }
}