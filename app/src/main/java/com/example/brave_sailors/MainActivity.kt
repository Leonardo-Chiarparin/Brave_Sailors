package com.example.brave_sailors

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.brave_sailors.ui.theme.Brave_SailorsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Immersive Sticky Mode
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            Brave_SailorsTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent
                ) { innerPadding ->
                    TermsScreen(innerPadding)
                }
            }
        }
    }
}

val OuterDeepBlue = Color(color = 0xFF020408)
val DeepBlue = Color(0xFF161B2E)
val CenterBg = Color(0xFF0B101C)
val BorderOrange = Color(0xFFE87A1E)
val HeaderGrey = Color(0xFF2D3545)
val TextWhite = Color(0xFFEEEEEE)
val BorderGlass = Color(0xFF758CA8)

@Composable
fun TermsScreen(innerPadding: PaddingValues = PaddingValues(0.dp)) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val barHeight = screenHeight * 0.12f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(innerPadding)
    ) {
        // Upper bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .background(OuterDeepBlue)
        ) {
            BarPattern()
        }

        // Center area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(CenterBg),
            contentAlignment = Alignment.Center
        ) {
            Modal()
        }

        // Lower bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .background(OuterDeepBlue)
        ) {
            BarPattern()
        }
    }
}

@Composable
fun BarPattern() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val path = Path()
        val w = size.width
        val h = size.height
        drawPath(
            path = path.apply {
                moveTo(0f, h); lineTo(w, h)
            },
            color = Color.Black.copy(alpha = 0.3f)
        )
    }
}

@Composable
fun GridBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val step = 14.dp.toPx()
        val gridColor = Color(0xFF5C78A5).copy(alpha = 0.12f)

        for (x in 0..size.width.toInt() step step.toInt()) {
            drawLine(
                color = gridColor,
                start = Offset(x.toFloat(), 0f),
                end = Offset(x.toFloat(), size.height),
                strokeWidth = 1f
            )
        }

        for (y in 0..size.height.toInt() step step.toInt()) {
            drawLine(
                color = gridColor,
                start = Offset(0f, y.toFloat()),
                end = Offset(size.width, y.toFloat()),
                strokeWidth = 1f
            )
        }
    }
}

@Composable
fun Modal() {
    val boxShape = CutCornerShape(16.dp)

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier.fillMaxWidth(0.90f)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 22.dp)
                .fillMaxWidth()
                .background(DeepBlue, shape = boxShape)
                .border(BorderStroke(1.dp, BorderOrange), shape = boxShape)
                .clip(boxShape)
        ) {
            GridBackground(Modifier.matchParentSize())

            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "To use this service, you must agree to the terms of usage. Minors must obtain permission from their parent or guardian.",
                    color = TextWhite,
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black,
                            offset = Offset(2f, 2f),
                            blurRadius = 4f
                        )
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassButton(
                        text = "Terms of Service",
                        onClick = {},
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    GlassButton(
                        text = "Privacy Policy",
                        onClick = {},
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Agree to the terms of usage and begin using the service.",
                    color = TextWhite,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black,
                            offset = Offset(1f, 1f),
                            blurRadius = 2f
                        )
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                PrimaryButton(text = "START", onClick = {})
            }
        }

        Box(modifier = Modifier.zIndex(2f)) {
            HeaderTab(text = "Consensus")
        }
    }
}

@Composable
fun GlassButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cutSizeDp = 16.dp
    val buttonShape = CutCornerShape(
        bottomStart = cutSizeDp,
        bottomEnd = cutSizeDp,
        topStart = 0.dp,
        topEnd = 0.dp
    )

    val bgColor = Color(0xFF2B3240).copy(alpha = 0.6f)
    val borderColor = BorderGlass.copy(alpha = 0.6f)

    Button(
        onClick = onClick,
        shape = buttonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        ),
        border = null,
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .height(48.dp)
            .background(bgColor, buttonShape)
            .drawBehind {
                val stroke = 1.dp.toPx()
                val cutPx = cutSizeDp.toPx()
                val w = size.width
                val h = size.height

                drawLine(
                    color = borderColor,
                    start = Offset(0f, 0f),
                    end = Offset(0f, h - cutPx),
                    strokeWidth = stroke
                )

                drawLine(
                    color = borderColor,
                    start = Offset(0f, h - cutPx),
                    end = Offset(cutPx, h),
                    strokeWidth = stroke
                )

                drawLine(
                    color = borderColor,
                    start = Offset(w, 0f),
                    end = Offset(w, h - cutPx),
                    strokeWidth = stroke
                )

                drawLine(
                    color = borderColor,
                    start = Offset(w, h - cutPx),
                    end = Offset(w - cutPx, h),
                    strokeWidth = stroke
                )
            }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = TextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.8f),
                        offset = Offset(1f, 1f),
                        blurRadius = 2f
                    )
                )
            )
        }
    }
}

@Composable
fun HeaderTab(text: String) {
    val width = 200.dp
    val height = 44.dp

    Box(
        modifier = Modifier
            .height(height)
            .width(width),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cut = 12.dp.toPx()
            val h = size.height
            val w = size.width

            val path = Path().apply {
                moveTo(cut, 0f)
                lineTo(w - cut, 0f)
                lineTo(w, cut)
                lineTo(w, h - cut)
                lineTo(w - cut, h)
                lineTo(cut, h)
                lineTo(0f, h - cut)
                lineTo(0f, cut)
                close()
            }
            drawPath(path, color = HeaderGrey)
            drawPath(
                path = path,
                color = HeaderGrey,
                style = Stroke(width = 3f)
            )
        }

        Text(
            text = text,
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            letterSpacing = 1.sp,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black,
                    offset = Offset(2f, 2f),
                    blurRadius = 4f
                )
            )
        )
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "ShinyTransition")

    val translateAnim by transition.animateFloat(
        initialValue = -300f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2500,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ), label = "ShinyOffset"
    )

    val buttonShape = CutCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp, topStart = 4.dp, topEnd = 4.dp)
    val buttonColor = Color(0xFFD66A13)

    Button(
        onClick = onClick,
        shape = buttonShape,
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .width(180.dp)
            .height(50.dp)
            .border(1.dp, Color(0xFFFCAE68), buttonShape)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val shineWidth = 40.dp.toPx()

                val brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.0f),
                        Color.White.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.0f)
                    ),
                    start = Offset(translateAnim, size.height),
                    end = Offset(translateAnim + shineWidth, 0f)
                )
                drawRect(brush = brush)
            }

            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = 1.sp,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = Offset(2f, 2f),
                        blurRadius = 2f
                    )
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TechPreview() {
    Brave_SailorsTheme {
        TermsScreen()
    }
}