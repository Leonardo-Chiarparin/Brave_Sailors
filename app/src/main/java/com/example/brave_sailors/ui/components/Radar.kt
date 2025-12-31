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
    val fixedOffset = with(density) { scale.dp(7f).toPx() }

    BoxWithConstraints(modifier = modifier) {
        val outerRadius = constraints.maxWidth.coerceAtMost(constraints.maxHeight).toFloat() / 2f
        val innerRadius = outerRadius - gapBetweenCircles

        val blips = remember(density, constraints) {
            val minRadius = gapBetweenCircles + fixedOffset
            val maxRadius = outerRadius - ((2 * gapBetweenCircles) + fixedOffset)

            val safeRadius = if (maxRadius > minRadius) maxRadius else minRadius

            val innerBoundaryRatio = minRadius / outerRadius
            val outerBoundaryRatio = safeRadius / outerRadius

            generateBlips(
                count = 5,
                minDistance = gapBetweenCircles * 1.25f,
                minRadius = innerBoundaryRatio,
                maxRadius = outerBoundaryRatio,
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
        val coneDegrees = 48f
        val sweepFactor = coneDegrees / 360f

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
        rotate(degrees = (angle - 90f) - coneDegrees, pivot = center) {
            drawArc(
                brush = Brush.sweepGradient(
                    colorStops = arrayOf(
                        0.0f to White.copy(alpha = 0.0f),
                        sweepFactor to White.copy(alpha = 0.75f),
                        sweepFactor + 0.01f to White.copy(alpha = 0f),
                        1.0f to White.copy(alpha = 0f)
                    ),
                    center = center
                ),
                startAngle = 0f,
                sweepAngle = coneDegrees,
                useCenter = true,
                topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                size = Size(outerRadius * 2f, outerRadius * 2f),
            )
        }

        // Blips
        fun normalizeAngle(ang: Float): Float = (ang % 360f + 360f) % 360f

        val currentHeadAngle = normalizeAngle(angle - 90f)
        val fadeRange = coneDegrees * 1.25f

        blips.forEach{ blip ->
            val blipRadians = Math.toRadians(blip.angle.toDouble())
            val blipX = center.x + (cos(blipRadians).toFloat() * (outerRadius * blip.distance))
            val blipY = center.y + (sin(blipRadians).toFloat() * (outerRadius * blip.distance))
            val blipCenter = Offset(blipX, blipY)

            val blipAngNormalized = normalizeAngle(blip.angle)
            val degreesSincePass = normalizeAngle(currentHeadAngle - blipAngNormalized)

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