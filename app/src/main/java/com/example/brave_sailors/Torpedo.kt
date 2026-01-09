package com.example.brave_sailors.ui.challenge

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.brave_sailors.ui.minigame.*
import com.example.brave_sailors.ui.theme.*
import com.example.brave_sailors.ui.utils.RememberScaleConversion
import com.example.brave_sailors.ui.components.PrimaryButton
import com.example.brave_sailors.ui.components.SecondaryButton
import com.example.brave_sailors.ui.components.GridBackground
import kotlinx.coroutines.android.awaitFrame

// --- LOCAL MINIGAME COLORS ---
private val SeaTop = Color(0xFF006994)
private val SeaBottom = Color(0xFF001e3b)
private val MetalDark = Color(0xFF2C3E50)
private val MetalLight = Color(0xFF95A5A6)
private val NeonCyan = Color(0xFF00FFFF)
private val TorpedoGold = Color(0xFFFFD700)
private val MineBody = Color(0xFF222222)
private val MineSpike = Color(0xFFCC0000)

@Composable
fun TorpedoScreen(
    viewModel: TorpedoGameViewModel = viewModel(),
    onGameResult: (Boolean) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scale = RememberScaleConversion()

    // Sensor handling for tilt movement
    var currentTiltX by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let { currentTiltX = -it.values[0] }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    // Game loop synchronized with display refresh rate
    LaunchedEffect(uiState.status) {
        if (uiState.status == TorpedoStatus.RUNNING) {
            while (true) {
                awaitFrame()
                viewModel.updateFrame(currentTiltX)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlue)
            .onGloballyPositioned { coords ->
                if (uiState.status == TorpedoStatus.WAITING_FOR_SIZE) {
                    viewModel.initGame(
                        coords.size.width.toFloat(),
                        coords.size.height.toFloat()
                    )
                }
            }
    ) {
        // Grid background behind the canvas
        GridBackground(modifier = Modifier.matchParentSize(), color = DarkGrey, 14f)

        // Main Game Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (uiState.status != TorpedoStatus.WAITING_FOR_SIZE) {
                drawRect(Brush.verticalGradient(listOf(SeaTop.copy(0.7f), SeaBottom.copy(0.9f))))
                drawSonarTarget(uiState.targetPos)
                uiState.walls.forEach { wall ->
                    drawRect(MetalDark, wall.rect.topLeft, wall.rect.size)
                    drawRect(MetalLight, wall.rect.topLeft, wall.rect.size, style = Stroke(4f))
                }
                uiState.holes.forEach { hole -> drawGameEntity(hole) }
                drawTorpedo(uiState.torpedoPos)
            }
        }

        // HUD - Time display during gameplay
        if (uiState.status == TorpedoStatus.RUNNING) {
            Text(
                text = "TIME: ${uiState.elapsedTime}s",
                color = NeonCyan,
                fontSize = scale.sp(24f),
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.align(Alignment.TopEnd).padding(scale.dp(32f))
            )
        }

        // Results Overlay (Win or Loss)
        if (uiState.status == TorpedoStatus.WON || uiState.status == TorpedoStatus.LOST) {
            Modal(
                isWin = uiState.status == TorpedoStatus.WON,
                time = uiState.elapsedTime,
                onConfirm = {
                    // [ ACTION ]: Update XP and close the overlay
                    onGameResult(uiState.status == TorpedoStatus.WON)
                    // [ FIX ]: Silent reset to ensure fresh start on next entry
                    viewModel.resetGame()
                },
                onRetry = { viewModel.resetGame() }
            )
        }
    }
}

@Composable
private fun Modal(
    isWin: Boolean,
    time: Long,
    onConfirm: () -> Unit,
    onRetry: () -> Unit
) {
    val scale = RememberScaleConversion()
    val boxShape = CutCornerShape(scale.dp(24f))

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(scale.dp(500f))
                .background(DarkBlue, boxShape)
                .border(2.dp, if (isWin) NeonCyan else Orange, boxShape)
                .padding(scale.dp(32f)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isWin) "TARGET DESTROYED!" else "MISSION FAILED",
                color = if (isWin) NeonCyan else Color.Red,
                fontSize = scale.sp(32f),
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )

            if (isWin) {
                Spacer(modifier = Modifier.height(scale.dp(16f)))
                Text(text = "Time: ${time}s", color = Color.White, fontSize = scale.sp(22f))
            }

            Spacer(modifier = Modifier.height(scale.dp(48f)))

            // [ REFINED ]: Smaller button sizes for better aesthetics
            PrimaryButton(
                130f, 32f, // Reduced from 200/40 to be more compact
                text = if (isWin) "FINISH" else "ABORT",
                onClick = onConfirm
            )

            Spacer(modifier = Modifier.height(scale.dp(16f)))

            SecondaryButton(
                48f, 8f, // Reduced from 64/12 for a slimmer retry action
                text = "RETRY",
                onClick = onRetry
            )
        }
    }
}

// Rendering helpers remain identical
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTorpedo(center: Offset) {
    val path = Path().apply {
        moveTo(center.x, center.y)
        lineTo(center.x - 15f, center.y + 70f)
        lineTo(center.x + 15f, center.y + 70f)
        close()
    }
    drawPath(path, Brush.verticalGradient(listOf(Color.White.copy(0.4f), Color.Transparent), center.y, center.y + 70f))
    drawCircle(Brush.radialGradient(listOf(TorpedoGold, Color(0xFFB8860B)), center), radius = 25f, center = center)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGameEntity(entity: GameEntity) {
    if (entity.isTurbo) {
        drawCircle(Color.Red.copy(0.2f), entity.radius + 10f, Offset(entity.x, entity.y))
        drawCircle(Color.Red, entity.radius, Offset(entity.x, entity.y), style = Stroke(2f))
    } else {
        drawCircle(MineBody, entity.radius, Offset(entity.x, entity.y))
        drawCircle(MineSpike, 8f, Offset(entity.x, entity.y))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSonarTarget(center: Offset) {
    val r = 60f
    drawCircle(NeonCyan, r, center, style = Stroke(2f))
    drawLine(NeonCyan, Offset(center.x - r, center.y), Offset(center.x + r, center.y), 1f)
    drawLine(NeonCyan, Offset(center.x, center.y - r), Offset(center.x, center.y + r), 1f)
}