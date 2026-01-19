package com.example.brave_sailors

import android.content.Context
import android.content.pm.ActivityInfo
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.brave_sailors.model.minigame.CargoViewModel
import com.example.brave_sailors.ui.components.DialogChallengeResult
import com.example.brave_sailors.ui.components.GridBackground
import com.example.brave_sailors.model.minigame.TorpedoStatus
import com.example.brave_sailors.ui.theme.DarkGrey
import com.example.brave_sailors.ui.theme.DeepBlue
import com.example.brave_sailors.ui.theme.Orange
import com.example.brave_sailors.ui.theme.White
import com.example.brave_sailors.ui.utils.RememberScaleConversion
import com.example.brave_sailors.ui.utils.findActivity
import kotlinx.coroutines.android.awaitFrame

@Composable
fun CargoScreen(viewModel: CargoViewModel = viewModel(), onGameResult: (Boolean) -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scale = RememberScaleConversion()
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val activity = context.findActivity()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent?) {
                e?.let { tiltX = -it.values[0]; tiltY = it.values[1] }
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        sm.registerListener(listener, acc, SensorManager.SENSOR_DELAY_GAME)
        onDispose {
            sm.unregisterListener(listener)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    LaunchedEffect(uiState.status) {
        if (uiState.status == TorpedoStatus.RUNNING) {
            while (true) {
                awaitFrame()
                viewModel.updateFrame(tiltX, tiltY)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepBlue)
        .onGloballyPositioned {
            if (uiState.status == TorpedoStatus.WAITING_FOR_SIZE) {
                viewModel.initGame(it.size.width.toFloat(), it.size.height.toFloat())
            }
        }
    ) {
        GridBackground(Modifier.matchParentSize(), color = DarkGrey, 14f)

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (uiState.status != TorpedoStatus.WAITING_FOR_SIZE) {
                drawRect(Brush.radialGradient(listOf(Color(0xFF2C2C2C), Color(0xFF121212)), uiState.screenCenter, size.height))

                drawCircle(Orange.copy(0.3f), uiState.shipRadius, uiState.screenCenter, style = Stroke(8f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 20f))))

                val cSize = 80f
                val topLeft = uiState.cargoPos - Offset(40f, 40f)
                drawOval(Color.Black.copy(0.3f), topLeft + Offset(10f, 10f), Size(cSize, cSize / 2))
                drawRoundRect(Color(0xFFFF8F00), topLeft, Size(cSize, cSize), CornerRadius(8f))
                drawRoundRect(Color(0xFFBF360C), topLeft, Size(cSize, cSize), CornerRadius(8f), style = Stroke(4f))

                uiState.cannonballs.forEach { ball ->
                    drawCircle(Color.Red.copy(0.5f), ball.radius + 6f, ball.pos)
                    drawCircle(
                        brush = Brush.radialGradient(listOf(Color.White, Color.Gray, Color.Black), center = ball.pos, radius = ball.radius),
                        radius = ball.radius,
                        center = ball.pos
                    )
                }
            }
        }

        if (uiState.status == TorpedoStatus.RUNNING) {
            Text("${uiState.elapsedTime}s", color = White, fontSize = scale.sp(40f), fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopCenter).padding(top = 60.dp))
        }

        if (uiState.status == TorpedoStatus.WON || uiState.status == TorpedoStatus.LOST) {
            val isWin = uiState.status == TorpedoStatus.WON

            Modal(
                isWin = isWin,
                time = null,
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
    time: Long? = null,
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