package com.example.brave_sailors

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

// Colors
val OuterDeepBlue = Color(color = 0xFF020408)
val DeepBlue = Color(0xFF161B2E)
val CenterBg = Color(0xFF0A1931) // Changed to a navy blue
val BorderOrange = Color(0xFFE87A1E)
val HeaderGrey = Color(0xFF2D3545)
val TextWhite = Color(0xFFEEEEEE)
val BorderGlass = Color(0xFF758CA8)

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
