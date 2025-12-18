package com.example.brave_sailors.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import com.example.brave_sailors.ui.theme.Grey
import com.example.brave_sailors.ui.theme.White
import com.example.brave_sailors.ui.utils.RadarBlip
import com.example.brave_sailors.ui.utils.RememberScaleConversion
import com.example.brave_sailors.ui.utils.generateBlips
import kotlin.collections.forEach
import kotlin.math.cos
import kotlin.math.sin

// [ TO - DO ]: If necessary, generalize the "Radar" function by adding some parameters that refer to the size of each visual element
@Composable
fun Radar(modifier: Modifier) {
    val density = LocalDensity.current
    val scale = RememberScaleConversion()

    val infiniteTransition = rememberInfiniteTransition(label = "RadarLoop")

    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                4500,
                easing = LinearEasing
            )
        ),
        label = "RadarAngle"
    )

    val gapBetweenCircles = with(density){ scale.dp(14f).toPx() } // blips' radius * 2f

    BoxWithConstraints(modifier = modifier) {
        val outerRadius = constraints.maxWidth.coerceAtMost(constraints.maxHeight).toFloat() / 2f
        val innerRadius = outerRadius - gapBetweenCircles

        val innerBoundaryRatio = if (outerRadius > 0) (innerRadius - (gapBetweenCircles / 2f)) / outerRadius else 1f - ((innerRadius - (gapBetweenCircles / 2f)) / outerRadius)

        val blips = remember(density, constraints) {
            generateBlips(
                count = 5,
                minDistance = gapBetweenCircles * 1.25f,
                minRadius = with(density) { scale.dp((1f - innerBoundaryRatio) / 2f).toPx() },
                maxRadius = with(density) { scale.dp(innerBoundaryRatio / 2f).toPx() },
                outerRadius = outerRadius
            )
        }

        DrawRadar(angle, blips, innerRadius, outerRadius)
    }
}

@Composable
fun DrawRadar(angle: Float, blips: List<RadarBlip>, innerRadius: Float, outerRadius: Float) {
    val scale = RememberScaleConversion()

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val coneDegrees = 60f

        // Outer circle
        drawCircle(
            color = Grey,
            radius = outerRadius,
            center = center,
            style = Stroke(scale.dp(3f).toPx())
        )

        // Inner section
        rotate(degrees = -angle, pivot = center) {
            val gap = scale.dp(18f).toPx()

            val segments = 8
            val step = 360f / segments

            for(segment in 0 until segments) {
                drawArc(
                    color = Grey,
                    startAngle = ( step * segment ) + ( gap / 2f ),
                    sweepAngle = step - gap,
                    useCenter = false,
                    topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
                    size = Size(innerRadius * 2f, innerRadius * 2f),
                    style = Stroke(
                        scale.dp(3f).toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(
                            intervals = floatArrayOf(gap, gap),
                            phase = 0f
                        )
                    )
                )
            }
        }

        // Radar ( with a 60-degree cone )
        rotate(degrees = angle, pivot = center) {
            drawArc(
                brush = Brush.sweepGradient(
                    colorStops = arrayOf(
                        0.0f to White,
                        0.25f to White.copy(alpha = 0.0f),
                        0.50f to White.copy(alpha = 0.25f),
                        0.75f to White.copy(alpha = 0.0f),
                        1.0f to White
                    ),
                    center = center
                ),
                startAngle = 0f,
                sweepAngle = -coneDegrees,
                useCenter = true,
                topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                size = Size(outerRadius * 2f, outerRadius * 2f),
            )
        }

        // Blips
        val currentAngle = angle % 360
        val fadeRange = coneDegrees + (coneDegrees / 2f) // a bit higher than the cone's degrees

        blips.forEach{ blip ->
            val blipRadians = Math.toRadians(blip.angle.toDouble())

            val blipX = center.x + ( cos(blipRadians).toFloat() * ( outerRadius * blip.distance ) )
            val blipY = center.y + ( sin(blipRadians).toFloat() * ( outerRadius * blip.distance ) )

            val blipCenter = Offset(blipX, blipY)
            val degreesSincePass = ( ( currentAngle - blip.angle ) + 360f ) % 360f

            if( degreesSincePass < fadeRange ) {
                val alpha = (1f - (degreesSincePass / fadeRange)).coerceIn(0f, 1f)

                drawCircle(
                    color = White.copy(alpha = alpha),
                    radius = scale.dp(7f).toPx(),
                    center = blipCenter
                )
            }
        }
    }
}