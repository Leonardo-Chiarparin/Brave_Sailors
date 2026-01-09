package com.example.brave_sailors.ui.minigame

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.hypot
import kotlin.random.Random

enum class TorpedoStatus {
    WAITING_FOR_SIZE,
    RUNNING,
    WON,
    LOST
}

data class TorpedoUiState(
    val status: TorpedoStatus = TorpedoStatus.WAITING_FOR_SIZE,
    val torpedoPos: Offset = Offset.Zero,
    val holes: List<GameEntity> = emptyList(),
    val walls: List<GameWall> = emptyList(),
    val targetPos: Offset = Offset.Zero,
    val screenWidth: Float = 0f,
    val screenHeight: Float = 0f,
    val elapsedTime: Long = 0L
)

data class GameEntity(
    val x: Float,
    val y: Float,
    val radius: Float,
    val speedX: Float = 0f,
    val isTurbo: Boolean = false
)

data class GameWall(val rect: Rect)

class TorpedoGameViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TorpedoUiState())
    val uiState: StateFlow<TorpedoUiState> = _uiState.asStateFlow()

    private val torpedoRadius = 25f
    private val holeRadius = 35f
    private val targetRadius = 60f
    private val baseSpeedY = 4.0f
    private val sensitivityX = 6.0f
    private var startTime = 0L

    fun initGame(width: Float, height: Float) {
        if (_uiState.value.status != TorpedoStatus.WAITING_FOR_SIZE) return

        startTime = System.currentTimeMillis()
        val startX = width / 2
        val startY = height - 150f
        val targetX = width / 2
        val targetY = 150f

        val walls = mutableListOf<GameWall>()
        val holes = mutableListOf<GameEntity>()
        val safeZones = mutableListOf<Rect>()
        val wallThickness = 40f
        val gapSize = 280f

        // Obstacle Generation
        val barriersY = listOf(height * 0.75f, height * 0.5f)
        barriersY.forEach { y ->
            val gapX = if (Random.nextBoolean()) width * 0.3f else width * 0.7f
            safeZones.add(Rect(gapX - gapSize/2 - 20f, y - 100f, gapX + gapSize/2 + 20f, y + wallThickness + 100f))

            if (gapX - gapSize/2 > 0) walls.add(GameWall(Rect(0f, y, gapX - gapSize/2, y + wallThickness)))
            if (gapX + gapSize/2 < width) walls.add(GameWall(Rect(gapX + gapSize/2, y, width, y + wallThickness)))
            holes.add(GameEntity(gapX + (gapSize/2) - 30f, y + wallThickness + holeRadius + 10f, holeRadius, speedX = 0f, isTurbo = false))
        }

        val midY = height * 0.4f
        holes.add(GameEntity(width / 2, midY, holeRadius, speedX = 3.5f, isTurbo = false))

        val climaxY = height * 0.25f
        holes.add(GameEntity(width * 0.2f, climaxY, holeRadius, speedX = 12.0f, isTurbo = true))
        holes.add(GameEntity(width * 0.8f, climaxY + 60f, holeRadius, speedX = -12.0f, isTurbo = true))

        walls.add(GameWall(Rect(0f, climaxY - 50f, 60f, climaxY + 100f)))
        walls.add(GameWall(Rect(width - 60f, climaxY - 50f, width, climaxY + 100f)))
        walls.add(GameWall(Rect(0f, 0f, 20f, height)))
        walls.add(GameWall(Rect(width - 20f, 0f, width, height)))

        repeat(2) {
            val hx = Random.nextFloat() * (width - 150f) + 75f
            val hy = Random.nextFloat() * (height - 600f) + 400f
            val candidate = Offset(hx, hy)
            var isSafe = true
            for(w in walls) if(w.rect.inflate(50f).contains(candidate)) isSafe = false
            for(z in safeZones) if(z.contains(candidate)) isSafe = false
            if(isSafe) holes.add(GameEntity(hx, hy, holeRadius, isTurbo = false))
        }

        _uiState.update {
            it.copy(
                status = TorpedoStatus.RUNNING,
                screenWidth = width,
                screenHeight = height,
                torpedoPos = Offset(startX, startY),
                targetPos = Offset(targetX, targetY),
                holes = holes,
                walls = walls,
                elapsedTime = 0
            )
        }
    }

    fun updateFrame(tiltX: Float) {
        val currentState = _uiState.value
        if (currentState.status != TorpedoStatus.RUNNING) return

        val dx = tiltX * sensitivityX
        val dy = -baseSpeedY
        val newTorpedoPos = currentState.torpedoPos + Offset(dx, dy)

        val updatedHoles = currentState.holes.map { hole ->
            if (hole.speedX != 0f) {
                var newX = hole.x + hole.speedX
                var newSpeed = hole.speedX
                if (newX - hole.radius < 20f || newX + hole.radius > currentState.screenWidth - 20f) {
                    newSpeed = -newSpeed
                    newX = hole.x + newSpeed
                }
                if (!hole.isTurbo) {
                    val testPoint = Offset(newX, hole.y)
                    for (wall in currentState.walls) {
                        if (wall.rect.contains(testPoint)) {
                            newSpeed = -newSpeed
                            newX = hole.x + newSpeed
                            break
                        }
                    }
                }
                hole.copy(x = newX, speedX = newSpeed)
            } else hole
        }

        val testRect = Rect(center = newTorpedoPos, radius = torpedoRadius)
        for (wall in currentState.walls) {
            if (wall.rect.overlaps(testRect)) {
                _uiState.update { it.copy(status = TorpedoStatus.LOST, torpedoPos = newTorpedoPos) }
                return
            }
        }

        updatedHoles.forEach { hole ->
            if (checkCircleCollision(newTorpedoPos, torpedoRadius, Offset(hole.x, hole.y), hole.radius)) {
                _uiState.update { it.copy(status = TorpedoStatus.LOST, torpedoPos = newTorpedoPos, holes = updatedHoles) }
                return
            }
        }

        if (checkCircleCollision(newTorpedoPos, torpedoRadius, currentState.targetPos, targetRadius)) {
            _uiState.update { it.copy(status = TorpedoStatus.WON, torpedoPos = newTorpedoPos) }
            return
        }

        if (newTorpedoPos.y < (currentState.targetPos.y - targetRadius - 20f)) {
            _uiState.update { it.copy(status = TorpedoStatus.LOST, torpedoPos = newTorpedoPos) }
            return
        }

        val time = (System.currentTimeMillis() - startTime) / 1000
        _uiState.update { it.copy(torpedoPos = newTorpedoPos, holes = updatedHoles, elapsedTime = time) }
    }

    private fun checkCircleCollision(p1: Offset, r1: Float, p2: Offset, r2: Float): Boolean {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return hypot(dx, dy) < (r1 + r2)
    }

    fun resetGame() {
        val w = _uiState.value.screenWidth
        val h = _uiState.value.screenHeight

        // Resetting to WAITING_FOR_SIZE forces initGame to re-generate barriers
        _uiState.value = TorpedoUiState(status = TorpedoStatus.WAITING_FOR_SIZE)

        if (w > 0 && h > 0) initGame(w, h)
    }
}