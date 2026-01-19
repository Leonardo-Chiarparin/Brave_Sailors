package com.example.brave_sailors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.brave_sailors.ui.components.DialogChallengeResult
import com.example.brave_sailors.ui.components.GridBackground
import com.example.brave_sailors.model.minigame.GameEntity
import com.example.brave_sailors.model.minigame.TorpedoGameViewModel
import com.example.brave_sailors.model.minigame.TorpedoStatus
import com.example.brave_sailors.ui.theme.DarkGrey
import com.example.brave_sailors.ui.theme.DeepBlue
import com.example.brave_sailors.ui.utils.RememberScaleConversion
import kotlinx.coroutines.android.awaitFrame

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
            .onGloballyPositioned { coordinates ->
                if (uiState.status == TorpedoStatus.WAITING_FOR_SIZE) {
                    viewModel.initGame(
                        coordinates.size.width.toFloat(),
                        coordinates.size.height.toFloat()
                    )
                }
            }
    ) {
        GridBackground(modifier = Modifier.matchParentSize(), color = DarkGrey, 14f)

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

        if (uiState.status == TorpedoStatus.WON || uiState.status == TorpedoStatus.LOST) {
            val isWin = uiState.status == TorpedoStatus.WON

            Modal(
                isWin = isWin,
                time = uiState.elapsedTime,
                buttonText = if (isWin) "HONOR" else "ABORT",
                onConfirm = {
                    onGameResult(isWin)
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
    buttonText: String,
    onConfirm: () -> Unit,
    onRetry: () -> Unit
) {
    DialogChallengeResult(
        isWin = isWin,
        timeElapsed = time,
        buttonText = buttonText,
        onConfirm = onConfirm,
        onRetry = onRetry
    )
}

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