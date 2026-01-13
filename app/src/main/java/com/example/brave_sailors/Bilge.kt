package com.example.brave_sailors.ui.challenge

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.brave_sailors.model.minigame.BilgeViewModel
import com.example.brave_sailors.ui.components.DialogChallengeResult
import com.example.brave_sailors.ui.components.GridBackground
import com.example.brave_sailors.ui.minigame.TorpedoStatus
import com.example.brave_sailors.ui.theme.DarkGrey
import com.example.brave_sailors.ui.theme.DeepBlue
import com.example.brave_sailors.ui.theme.LightGrey
import com.example.brave_sailors.ui.theme.Orange
import com.example.brave_sailors.ui.theme.White
import com.example.brave_sailors.ui.utils.RememberScaleConversion
import com.example.brave_sailors.ui.utils.findActivity
import kotlin.math.PI
import kotlin.math.sin

// --- LOCAL CHALLENGE COLORS ---
private val WaterBlue = Color(0xFF006994)
private val WaterDark = Color(0xFF00334E)
private val AlarmRed = Color(0xFFFF0000)

@SuppressLint("ConfigurationScreenWidthHeight", "SourceLockedOrientationActivity")
@Composable
fun BilgeScreen(
    viewModel: BilgeViewModel = viewModel(),
    onGameResult: (Boolean) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scale = RememberScaleConversion()
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    // --- HARDWARE & ORIENTATION LOCK ---
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val safeY = y.coerceAtLeast(0.1f)

                viewModel.updateFrame(x, safeY, z)
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        viewModel.startGame()

        onDispose {
            sensorManager.unregisterListener(listener)
            activity?.requestedOrientation = originalOrientation
        }
    }


    // --- ANIMATIONS ---
    val infiniteTransition = rememberInfiniteTransition(label = "bilge_fx")
    val alarmAlpha by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "alarm"
    )
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "waves"
    )

    Box(modifier = Modifier.fillMaxSize().background(DeepBlue)) {

        // Background grid consistent with Torpedo and main UI
        GridBackground(modifier = Modifier.matchParentSize(), color = DarkGrey, 14f)

        // --- DYNAMIC WATER RENDERING ---
        val waterFillHeight = screenHeight * uiState.waterLevel

        Canvas(modifier = Modifier.fillMaxWidth().height(waterFillHeight).align(Alignment.BottomCenter)) {
            val width = size.width
            val height = size.height

            val waveAmplitude = 25f
            val dynamicAmplitude = minOf(waveAmplitude, height * 0.4f)

            val path = Path().apply {
                moveTo(0f, dynamicAmplitude * sin(wavePhase))

                for (x in 0..width.toInt() step 10) {
                    val y = dynamicAmplitude * sin((x * 0.02f) + wavePhase)
                    lineTo(x.toFloat(), y)
                }
                lineTo(width, height); lineTo(0f, height); close()
            }
            drawPath(path, Brush.verticalGradient(listOf(WaterBlue.copy(0.7f), WaterDark)))
        }

        // --- HUD ---
        if (uiState.status == TorpedoStatus.RUNNING) {
            // High-visibility Timer
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = scale.dp(60f))
                    .background(Color.Black.copy(0.6f), CutCornerShape(scale.dp(12f)))
                    .border(1.dp, if (uiState.timeRemaining <= 3) AlarmRed else Orange, CutCornerShape(scale.dp(12f)))
                    .padding(horizontal = scale.dp(32f), vertical = scale.dp(8f))
            ) {
                Text(
                    text = "${uiState.timeRemaining}",
                    color = if (uiState.timeRemaining <= 3) AlarmRed else White,
                    fontSize = scale.sp(48f),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            }

            // Gameplay Instructions
            Column(
                modifier = Modifier.align(Alignment.Center).offset(y = scale.dp(-40f)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Vibration, contentDescription = null,
                    tint = if (uiState.waterLevel > 0.7f) AlarmRed else White,
                    modifier = Modifier.size(scale.dp(100f)).graphicsLayer {
                        rotationZ = (sin(System.currentTimeMillis() / 50.0) * 15).toFloat()
                    }
                )
                Spacer(modifier = Modifier.height(scale.dp(16f)))
                Text("SHAKE THE DEVICE!", color = White, fontSize = scale.sp(36f), fontWeight = FontWeight.Black)
                Text("PUMP OUT THE WATER", color = LightGrey, fontSize = scale.sp(18f), fontWeight = FontWeight.Bold)
            }
        }

        // Critical Flood warning
        if (uiState.waterLevel > 0.75f && uiState.status == TorpedoStatus.RUNNING) {
            Box(modifier = Modifier.fillMaxSize().background(AlarmRed.copy(alpha = alarmAlpha)))
        }

        // --- END GAME MODAL ---
        if (uiState.status == TorpedoStatus.WON || uiState.status == TorpedoStatus.LOST) {
            val isWin = uiState.status == TorpedoStatus.WON

            Modal(
                isWin = isWin,
                time = null,
                buttonText = if (isWin) "HONOR" else "ABORT",
                onConfirm = {
                    // [ ACTION ]: Update XP and close the overlay
                    onGameResult(isWin)
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