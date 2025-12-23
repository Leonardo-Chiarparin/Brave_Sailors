package com.example.brave_sailors.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.brave_sailors.ui.theme.White
import com.example.brave_sailors.ui.utils.RememberScaleConversion

@Composable
fun Arrow(
    isLeft: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = RememberScaleConversion()

    Canvas(
        modifier = modifier
            .height(scale.dp(94f))
            .width(scale.dp(48f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        val gap = scale.dp(24f).toPx()
        val h = size.height
        val w = size.width

        val path = Path().apply {
            if (isLeft) {
                moveTo(0f, h * 0.5f)
                lineTo(w, 0f)
                lineTo(w, gap)
                lineTo(gap, h * 0.5f)
                lineTo(w, h - gap)
                lineTo(w, h)
                close()
            }
            else {
                moveTo(0f, 0f)
                lineTo(w, h * 0.5f)
                lineTo(0f, h)
                lineTo(0f, h - gap)
                lineTo(w - gap, h * 0.5f)
                lineTo(0f, gap)
                close()
            }
        }

        drawPath(
            path = path,
            color = White,
            style = Stroke(
                width = 1f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        drawPath(
            path = path,
            color = White
        )
    }
}