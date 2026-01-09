package com.example.brave_sailors

import android.content.Context
import android.content.pm.ActivityInfo
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.brave_sailors.ui.components.GridBackground
import com.example.brave_sailors.ui.components.PrimaryButton
import com.example.brave_sailors.ui.components.SecondaryButton
import com.example.brave_sailors.ui.minigame.*
import com.example.brave_sailors.ui.theme.*
import com.example.brave_sailors.ui.utils.RememberScaleConversion
import com.example.brave_sailors.ui.utils.findActivity
import kotlinx.coroutines.android.awaitFrame
import kotlin.math.hypot

@Composable
fun CargoScreen(viewModel: CargoViewModel = viewModel(), onGameResult: (Boolean) -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scale = RememberScaleConversion()
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }

    // [ LOGIC ]: Lock orientation and listen to accelerometer sensors
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

    // [ LOGIC ]: Main game loop execution
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
            // Only init if we haven't started yet to prevent ghost restarts
            if (uiState.status == TorpedoStatus.WAITING_FOR_SIZE) {
                viewModel.initGame(it.size.width.toFloat(), it.size.height.toFloat())
            }
        }
    ) {
        // STYLE: Consistent grid background behind the action
        GridBackground(Modifier.matchParentSize(), color = DarkGrey, 14f)

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (uiState.status != TorpedoStatus.WAITING_FOR_SIZE) {
                // 1. FLOOR LIGHTING
                drawRect(Brush.radialGradient(listOf(Color(0xFF2C2C2C), Color(0xFF121212)), uiState.screenCenter, size.height))

                // 2. TARGET ZONE INDICATOR
                drawCircle(Orange.copy(0.3f), uiState.shipRadius, uiState.screenCenter, style = Stroke(8f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 20f))))

                // 3. CARGO CRATE RENDERING
                val cSize = 80f
                val topLeft = uiState.cargoPos - Offset(40f, 40f)
                drawOval(Color.Black.copy(0.3f), topLeft + Offset(10f, 10f), Size(cSize, cSize / 2))
                drawRoundRect(Color(0xFFFF8F00), topLeft, Size(cSize, cSize), CornerRadius(8f))
                drawRoundRect(Color(0xFFBF360C), topLeft, Size(cSize, cSize), CornerRadius(8f), style = Stroke(4f))

                // 4. CANNONBALLS (Always on top with glow effect)
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

        // Win or Loss Dialog (Both now wait for user interaction)
        if (uiState.status == TorpedoStatus.WON || uiState.status == TorpedoStatus.LOST) {
            Modal(
                isWin = uiState.status == TorpedoStatus.WON,
                onConfirm = {
                    onGameResult(uiState.status == TorpedoStatus.WON)
                    viewModel.resetGame()
                },
                onRetry = { viewModel.resetGame() }
            )
        }
    }
}

@Composable
private fun Modal(isWin: Boolean, onConfirm: () -> Unit, onRetry: () -> Unit) {
    val scale = RememberScaleConversion()
    val boxShape = CutCornerShape(scale.dp(24f))

    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.85f)), Alignment.Center) {
        Column(
            modifier = Modifier
                .width(scale.dp(500f))
                .background(DarkBlue, boxShape)
                .border(2.dp, Orange, boxShape)
                .padding(scale.dp(32f)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isWin) "CARGO SECURED!" else "CARGO LOST!",
                color = if (isWin) Color.Cyan else Color.Red,
                fontSize = scale.sp(34f),
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )

            if (isWin) {
                Spacer(modifier = Modifier.height(scale.dp(8f)))
                Text("Bonus: +5 XP earned", color = Orange, fontSize = scale.sp(20f), fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(scale.dp(48f)))

            // Primary Button: Main action (Finish or Abort)
            PrimaryButton(
                130f, 32f,
                text = if (isWin) "FINISH" else "ABORT",
                onClick = onConfirm
            )

            Spacer(modifier = Modifier.height(scale.dp(16f)))

            // Secondary Button: Slower Retry action
            SecondaryButton(
                48f, 8f,
                text = "RETRY",
                onClick = onRetry
            )
        }
    }
}