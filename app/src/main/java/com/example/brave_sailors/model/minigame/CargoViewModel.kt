package com.example.brave_sailors.model.minigame

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import com.example.brave_sailors.ui.minigame.TorpedoStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.hypot

// [ DATA ]: Cannonball properties
data class Cannonball(
    val pos: Offset,
    val velocity: Offset,
    val radius: Float = 28f
)

// [ DATA ]: UI State
data class CargoUiState(
    val status: TorpedoStatus = TorpedoStatus.WAITING_FOR_SIZE,
    val cargoPos: Offset = Offset.Zero,
    val screenCenter: Offset = Offset.Zero,
    val shipRadius: Float = 0f,
    val elapsedTime: Long = 0,
    val cannonballs: List<Cannonball> = emptyList(),
    val screenWidth: Float = 0f,
    val screenHeight: Float = 0f
)

class CargoViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CargoUiState())
    val uiState = _uiState.asStateFlow()

    private var startTime = 0L
    private val targetTime = 15L
    private val cargoRadius = 38f

    // Physics constants (Slightly slowed down)
    private var velocityX = 0f
    private var velocityY = 0f
    private val friction = 0.92f // Higher friction for better control
    private val sensitivity = 0.25f // Reduced from 0.32f for slower movement

    private var lastSpawnTime = 0L
    private val spawnInterval = 2000L // Slower spawn rate

    /**
     * Starts the game only if it's in the initial waiting state.
     */
    fun initGame(w: Float, h: Float) {
        if (w <= 0 || h <= 0 || _uiState.value.status != TorpedoStatus.WAITING_FOR_SIZE) return

        velocityX = 0f
        velocityY = 0f
        startTime = System.currentTimeMillis()
        lastSpawnTime = startTime

        _uiState.update {
            it.copy(
                status = TorpedoStatus.RUNNING,
                cargoPos = Offset(w / 2, h / 2),
                screenCenter = Offset(w / 2, h / 2),
                screenWidth = w,
                screenHeight = h,
                shipRadius = w * 0.45f,
                elapsedTime = targetTime,
                cannonballs = emptyList()
            )
        }
    }

    /**
     * Logic loop with strict state check to prevent accidental restarts.
     */
    fun updateFrame(tiltX: Float, tiltY: Float) {
        val state = _uiState.value
        if (state.status != TorpedoStatus.RUNNING) return

        val now = System.currentTimeMillis()

        // 1. SPAWN LOGIC
        var currentBalls = state.cannonballs
        if (now - lastSpawnTime > spawnInterval) {
            currentBalls = currentBalls + createBall(state)
            lastSpawnTime = now
        }

        // 2. PROJECTILE MOVEMENT (Reduced speed in createBall)
        val movedBalls = currentBalls.map { it.copy(pos = it.pos + it.velocity) }
            .filter { it.pos.x in -200f..(state.screenWidth + 200f) &&
                    it.pos.y in -200f..(state.screenHeight + 200f) }

        // 3. SHIP PHYSICS
        velocityX += tiltX * sensitivity
        velocityY += tiltY * sensitivity
        velocityX *= friction
        velocityY *= friction
        val newCargoPos = state.cargoPos + Offset(velocityX, velocityY)

        // 4. ACCURATE COLLISION DETECTION
        movedBalls.forEach { ball ->
            val distance = hypot(ball.pos.x - newCargoPos.x, ball.pos.y - newCargoPos.y)
            if (distance < (ball.radius + cargoRadius - 5f)) {
                // Change status to LOST, modal will handle the rest
                _uiState.update { it.copy(status = TorpedoStatus.LOST) }
                return
            }
        }

        // 5. BOUNDARY CHECK
        val distFromCenter = hypot(newCargoPos.x - state.screenCenter.x, newCargoPos.y - state.screenCenter.y)
        if (distFromCenter > (state.shipRadius - cargoRadius + 2f)) {
            _uiState.update { it.copy(status = TorpedoStatus.LOST) }
            return
        }

        // 6. TIMER
        val elapsed = (now - startTime) / 1000
        val remaining = targetTime - elapsed

        if (remaining <= 0L) {
            _uiState.update { it.copy(status = TorpedoStatus.WON, elapsedTime = 0L) }
        } else {
            _uiState.update { it.copy(cargoPos = newCargoPos, cannonballs = movedBalls, elapsedTime = remaining) }
        }
    }

    private fun createBall(state: CargoUiState): Cannonball {
        val side = (0..3).random()
        val start = when(side) {
            0 -> Offset(-80f, (0..state.screenHeight.toInt()).random().toFloat())
            1 -> Offset(state.screenWidth + 80f, (0..state.screenHeight.toInt()).random().toFloat())
            2 -> Offset((0..state.screenWidth.toInt()).random().toFloat(), -80f)
            else -> Offset((0..state.screenWidth.toInt()).random().toFloat(), state.screenHeight + 80f)
        }
        val dir = state.screenCenter - start
        val dist = hypot(dir.x, dir.y)
        val speed = 7.0f // Reduced from 8.5f for slower projectile movement
        return Cannonball(start, Offset((dir.x / dist) * speed, (dir.y / dist) * speed))
    }

    fun resetGame() {
        velocityX = 0f
        velocityY = 0f
        _uiState.update { it.copy(status = TorpedoStatus.WAITING_FOR_SIZE, cannonballs = emptyList()) }
    }
}