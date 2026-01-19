package com.example.brave_sailors.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import com.example.brave_sailors.ui.utils.RememberScaleConversion
import kotlin.math.sqrt

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

@Composable
fun MosaicBackground() {
    val bgColor = Color(0xFF001848)

    val darkFillBottom = Color(0xFF082C70)
    val darkFillTop = Color(0xFF143C7C)

    val darkBorderRight = Color(0xFF244888)
    val darkBorderLeft = Color(0xFF103272)

    val lightFillTop = Color(0xFF204886)
    val lightFillBottom = Color(0xFF2A508C)

    val lightBorderRight = Color(0xFF346296)
    val lightBorderLeft = Color(0xFF18407E)

    val density = LocalDensity.current
    val scale = RememberScaleConversion()

    val gap = with(density) { scale.dp(10f).toPx() }
    val tile = with(density) { scale.dp(108f).toPx() }

    val radius = with(density) { scale.dp(18f).toPx() }
    val stroke = with(density) { scale.dp(4f).toPx() }

    val step = tile + gap

    val darkFillBrush = remember {
        Brush.linearGradient(
            colors = listOf(darkFillBottom, darkFillTop),
            start = Offset(0f, tile),
            end = Offset(tile, 0f)
        )
    }

    val darkBorderBrush = remember {
        Brush.linearGradient(
            colors = listOf(darkBorderRight, darkBorderLeft),
            start = Offset(tile, 0f),
            end = Offset(0f, 0f)
        )
    }

    val lightFillBrush = remember {
        Brush.linearGradient(
            colors = listOf(lightFillBottom, lightFillTop),
            start = Offset(0f, tile),
            end = Offset(tile, 0f)
        )
    }

    val lightBorderBrush = remember {
        Brush.linearGradient(
            colors = listOf(lightBorderRight, lightBorderLeft),
            start = Offset(tile, 0f),
            end = Offset(0f, 0f)
        )
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val h = size.height
        val w = size.width

        drawRect(color = bgColor)

        val screenDiagonal = sqrt((w * w) + (h * h))
        val count = (screenDiagonal / step).toInt() + 2

        rotate(degrees = -45f, pivot = center) {
            for (i in -count..count) {
                for (j in -count..count) {

                    val left = center.x + (i * step) - (tile / 2)
                    val top = center.y + (j * step) - (tile / 2)

                    val isDark = (i + j) % 2 == 0

                    val currentFillBrush = if (isDark) darkFillBrush else lightFillBrush
                    val currentBorderBrush = if (isDark) darkBorderBrush else lightBorderBrush

                    translate(left, top) {
                        drawRoundRect(
                            brush = currentFillBrush,
                            size = Size(tile, tile),
                            cornerRadius = CornerRadius(radius, radius)
                        )

                        drawRoundRect(
                            brush = currentBorderBrush,
                            size = Size(tile, tile),
                            cornerRadius = CornerRadius(radius, radius),
                            style = Stroke(width = stroke)
                        )
                    }
                }
            }
        }
    }
}