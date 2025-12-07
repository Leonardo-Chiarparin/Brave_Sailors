package com.example.brave_sailors

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.brave_sailors.ui.components.GridBackground
import com.example.brave_sailors.ui.theme.DarkGrey
import com.example.brave_sailors.ui.theme.DeepBlue
import com.example.brave_sailors.ui.theme.Grey
import com.example.brave_sailors.ui.theme.Orange
import com.example.brave_sailors.ui.theme.White
import com.example.brave_sailors.ui.utils.RememberScaleConversion
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

@Composable
fun IntroScreen(innerPadding: PaddingValues = PaddingValues(0.dp)) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(innerPadding)
    ) {
        // Center area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(DeepBlue),
            contentAlignment = Alignment.Center
        ) {
            Modal()
        }
    }
}

@Composable
private fun Modal() {
    // -- SCALE ( used for applying conversions )
    val scale = RememberScaleConversion()

    val maxWidth = scale.dp(648f) // 648px, etc.

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        GridBackground(Modifier.matchParentSize(), color = DarkGrey,14f)

        Box(
            modifier = Modifier
                .widthIn(max = maxWidth),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // [ OPTIONAL ]: Try to insert an image conversely ( otherwise remove this box ). Its shape is reported below
                Box(
                    modifier = Modifier
                        .width(scale.dp(672f))
                        .height(scale.dp(480f))
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawOval(
                            color = Grey,
                            topLeft = Offset.Zero,
                            size = size,
                            style = Stroke(width = scale.dp(1f).toPx())
                        )
                    }
                }

                Spacer(modifier = Modifier.height(scale.dp(88f)))

                Title()

                Spacer(modifier = Modifier.height(scale.dp(242f)))

                Radar(
                    modifier = Modifier
                        .size(scale.dp(148f))
                )

                Spacer(modifier = Modifier.height(scale.dp(70f)))

                Text(
                    text = "Prepare to engage in strategic naval operations, where every decision shapes the tide of battle.",
                    color = Orange,
                    fontFamily = FontFamily.SansSerif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    fontSize = scale.sp(26f),
                    textAlign = TextAlign.Center,
                    lineHeight = scale.sp(36f),
                    letterSpacing = scale.sp(2f),
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = false
                        ),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both
                        )
                    )
                )

                Spacer(modifier = Modifier.weight(1f))
            }

            Footer(modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
fun Title() {
    val scale = RememberScaleConversion()

    Box(
        modifier = Modifier
            .padding(start = scale.dp(106f), end = scale.dp(10f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
            ) {
                Text(
                    "BRAVE",
                    color = White,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = scale.sp(66f),
                    letterSpacing = scale.sp(8f),
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = false
                        ),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both
                        ),
                        shadow = Shadow(
                            color = Color.Black,
                            offset = Offset(2f, 2f),
                            blurRadius = 4f
                        )
                    )
                )

                Spacer(modifier = Modifier.height(scale.dp(8f)))

                Text(
                    "SAILORS",
                    color = White,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = scale.sp(54f),
                    lineHeight = scale.sp(54f),
                    letterSpacing = scale.sp(8f),
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = false
                        ),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both
                        ),
                        shadow = Shadow(
                            color = Color.Black,
                            offset = Offset(2f, 2f),
                            blurRadius = 4f
                        )
                    )
                )
            }

            Spacer(modifier = Modifier.width(scale.dp(16f)))

            VersionBanner(
                modifier = Modifier
                    .height(scale.dp(126f))
            )
        }
    }
}

@Composable
fun VersionBanner(modifier: Modifier) {
    val density = LocalDensity.current
    val scale = RememberScaleConversion()

    val bannerWidth = scale.dp(120f)
    val paddingDp = scale.dp(2f)

    val bannerShape = remember (bannerWidth, density, paddingDp) {
        GenericShape { size, _ ->
            val h = size.height
            val w = size.width

            val padding = with(density) { paddingDp.toPx() }

            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(with(density) { bannerWidth.toPx() }, h - padding)
            lineTo(0f, h - padding)
            close()
        }
    }

    val horizontalPadding = scale.dp(8f)
    val verticalPadding = scale.dp(10f)

    Box(
        modifier = modifier
            .clip(bannerShape)
            .background(Orange.copy(alpha = 0.90f))
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    "VERSION",
                    color = DeepBlue,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = scale.sp(14f),
                    letterSpacing = scale.sp(2f),
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = false
                        ),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both
                        )
                    )
                )

                Spacer(modifier = Modifier.height(scale.dp(6f)))

                Text(
                    "1.1",
                    color = DeepBlue,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = scale.sp(34f),
                    letterSpacing = scale.sp(4f),
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = false
                        ),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both
                        )
                    ),
                    modifier = Modifier
                        .offset(x = scale.dp(-6f))
                )
            }

            Text(
                "1.1.138",
                color = DeepBlue,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                fontSize = scale.sp(16f),
                letterSpacing = scale.sp(2f),
                style = TextStyle(
                    platformStyle = PlatformTextStyle(
                        includeFontPadding = false
                    ),
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.Both
                    )
                ),
                modifier = Modifier
                    .offset(x = scale.dp(-2f))
            )
        }
    }
}

data class RadarBlip(val angle: Float, val distance: Float)

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

    val blips = remember(density) {
        generateBlips(
            count = 5,
            minDistance = with(density){ scale.dp(0.25f).toPx()},
            minRadius = with(density){ scale.dp(0.20f).toPx()},
            maxRadius = with(density){ scale.dp(0.30f).toPx()}
        )
    }

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)

        val outerRadius = size.minDimension / 2f
        val innerRadius = outerRadius - scale.dp(14f).toPx()

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
                sweepAngle = -60f,
                useCenter = true,
                topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                size = Size(outerRadius * 2f, outerRadius * 2f),
            )
        }

        // Blips
        val currentAngle = angle % 360
        val fadeRange = 90f

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
                    radius = scale.dp(8f).toPx(),
                    center = blipCenter
                )
            }
        }
    }
}

fun generateBlips(count: Int, minDistance: Float, minRadius: Float, maxRadius: Float): List<RadarBlip> {
    val blips = mutableListOf<RadarBlip>()

    var attempts = 0
    val maxAttempts = 100

    while((blips.size < count) && (attempts < maxAttempts)) {
        val candidateAngle = Random.nextFloat() * 360f
        val candidateDistance = ( Random.nextFloat() * ( maxRadius - minRadius ) ) + minRadius

        val candidateRadians = Math.toRadians(candidateAngle.toDouble())

        val candidateX = candidateDistance * cos(candidateRadians)
        val candidateY = candidateDistance * sin(candidateRadians)

        var collides = false

        for(blip in blips) {
            val blipRadians = Math.toRadians(blip.angle.toDouble())

            val blipX = blip.distance * cos(blipRadians)
            val blipY = blip.distance * sin(blipRadians)

            val distance = sqrt((candidateX - blipX).pow(2) + (candidateY - blipY).pow(2))

            if(distance < minDistance) {
                collides = true
                break
            }
        }

        if(!collides)
            blips.add(RadarBlip(candidateAngle, candidateDistance))

        attempts++
    }

    return blips
}

@Composable
fun Footer(modifier: Modifier = Modifier) {
    val scale = RememberScaleConversion()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = scale.dp(14f)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "© 2025/26 Ettore Cantile, Leonardo Chiarparin",
            color = White,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = scale.sp(22f),
            letterSpacing = scale.sp(2f),
            style = TextStyle(
                platformStyle = PlatformTextStyle(
                    includeFontPadding = false
                ),
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both
                ),
                shadow = Shadow(
                    color = Color.Black,
                    offset = Offset(2f, 2f),
                    blurRadius = 4f
                )
            )
        )

        Spacer(modifier = Modifier.height(scale.dp(22f)))

        // [ TO - DO ]: The following component must be modified according to the ( initial ) connection's state
        Text(
            "... connect to server",
            color = Grey,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = scale.sp(16f),
            letterSpacing = scale.sp(2f),
            style = TextStyle(
                platformStyle = PlatformTextStyle(
                    includeFontPadding = false
                ),
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both
                ),
                shadow = Shadow(
                    color = Color.Black,
                    offset = Offset(2f, 2f),
                    blurRadius = 4f
                )
            ),
            modifier = Modifier
                .align(Alignment.Start)
                .padding(start = scale.dp(16f))
        )
    }
}