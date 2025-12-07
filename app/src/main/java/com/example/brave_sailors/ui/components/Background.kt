package com.example.brave_sailors.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.brave_sailors.ui.utils.RememberScaleConversion

@Composable
fun GridBackground(modifier: Modifier = Modifier, color: Color, dimension: Float) {
    val scale = RememberScaleConversion()

    Canvas(modifier = modifier) {
        val step = scale.dp(dimension).toPx()
        val stroke = scale.dp(2f).toPx()

        val h = size.height
        val w = size.width

        val startX = ( w % step ) / 2f
        val startY = ( h % step ) / 2f

        var x = startX
        var y = startY

        while (x <= w) {
            drawLine(
                color = color,
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = stroke
            )
            x += step
        }

        while (y <= h) {
            drawLine(
                color = color,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = stroke
            )
            y += step
        }
    }
}