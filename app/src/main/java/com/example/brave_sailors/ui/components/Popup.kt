package com.example.brave_sailors.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.example.brave_sailors.ui.theme.LightGreen
import com.example.brave_sailors.ui.theme.White
import com.example.brave_sailors.ui.utils.RememberScaleConversion

@Composable
fun Popup(
    start: Boolean,
    modifier: Modifier = Modifier,
    avatar: @Composable BoxScope.() -> Unit,
    content: @Composable RowScope.() -> Unit,
    onDone: () -> Unit = {}
) {
    val animationProgress = remember { Animatable(0f) }
    val scale = RememberScaleConversion()

    val minInnerWidth = scale.dp(88f)
    val minOuterWidth = scale.dp(104f)
    val maxWidth = scale.dp(632f)

    LaunchedEffect(start) {
        if (start) {
            animationProgress.snapTo(0f)

            animationProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 4500,
                    easing = LinearEasing
                )
            )

            onDone()
        }
    }

    fun calculateFraction(progress: Float, start: Float, end: Float): Float {
        return ((progress - start) / (end - start)).coerceIn(0f, 1f)
    }

    val progress = animationProgress.value

    val seg1Fin = 0.5f / 4.5f
    val seg2Fin = 0.75f / 4.5f
    val seg3Fin = 1.25f / 4.5f
    val seg4Fin = 1.5f / 4.5f
    val seg5Fin = 2.0f / 4.5f
    val seg6Fin = 3.25f / 4.5f
    val seg7Fin = 3.5f / 4.5f
    val seg8Fin = 3.75f / 4.5f
    val seg9Fin = 4.0f / 4.5f
    val seg10Fin = 4.25f / 4.5f
    val seg11Fin = 4.5f / 4.5f

    var alpha = 0f

    var innerSize: Dp = 0.dp
    var outerSize: Dp = 0.dp
    var width: Dp = 0.dp

    var isCircle = true
    var showContent = false

    when {
        progress <= seg1Fin -> {
            val fraction = calculateFraction(progress, 0f, seg1Fin)

            alpha = fraction
            outerSize = lerp(0.dp, minOuterWidth, fraction)
        }

        progress <= seg2Fin -> {
            alpha = 1f
            outerSize = minOuterWidth
        }

        progress <= seg3Fin -> {
            val fraction = calculateFraction(progress, seg2Fin, seg3Fin)

            alpha = 1f
            innerSize = lerp(0.dp, minInnerWidth, fraction)
            outerSize = minOuterWidth
        }

        progress <= seg4Fin -> {
            alpha = 1f
            innerSize = minInnerWidth
            outerSize = minOuterWidth
        }

        progress <= seg5Fin -> {
            val fraction = calculateFraction(progress, seg4Fin, seg5Fin)

            alpha = 1f
            isCircle = false
            innerSize = minInnerWidth
            outerSize = minOuterWidth
            width = lerp(outerSize, maxWidth, fraction)
        }

        progress <= seg6Fin -> {
            alpha = 1f
            isCircle = false
            innerSize = minInnerWidth
            showContent = true
            width = maxWidth
        }

        progress <= seg7Fin -> {
            val fraction = calculateFraction(progress, seg6Fin, seg7Fin)

            alpha = 1f
            isCircle = false
            innerSize = minInnerWidth
            width = lerp(maxWidth, minOuterWidth, fraction)
        }

        progress <= seg8Fin -> {
            alpha = 1f
            innerSize = minInnerWidth
            outerSize = minOuterWidth
        }

        progress <= seg9Fin -> {
            val fraction = calculateFraction(progress, seg8Fin, seg9Fin)

            alpha = 1f
            innerSize = lerp(minInnerWidth, 0.dp, fraction)
            outerSize = minOuterWidth
        }

        progress <= seg10Fin -> {
            alpha = 1f
            outerSize = minOuterWidth
        }

        progress <= seg11Fin -> {
            val fraction = calculateFraction(progress, seg10Fin, seg11Fin)

            alpha = (1f - fraction).coerceIn(0f, 1f)
            outerSize = lerp(minOuterWidth, 0.dp, fraction)
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer(alpha = alpha)
            .height(minOuterWidth),
        contentAlignment = Alignment.Center
    ) {
        if (isCircle) {
            Box(
                modifier = Modifier
                    .size(outerSize)
                    .shadow(scale.dp(2f), CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(White),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(innerSize)
                        .clip(CircleShape),
                    content = avatar
                )
            }
        }
        else {
            Box(
                modifier = Modifier
                    .width(width)
                    .height(minOuterWidth),
                contentAlignment = Alignment.CenterStart
            ) {
                Surface(
                    color = White,
                    shape = RoundedCornerShape(50),
                    shadowElevation = scale.dp(2f),
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier
                                .background(
                                    color = LightGreen,
                                    shape = RoundedCornerShape(50)
                                )
                                .widthIn(max = scale.dp(395f))
                                .wrapContentWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = (minOuterWidth - minInnerWidth) / 2f)
                                    .size(innerSize),
                                content = avatar
                            )

                            if (showContent) {
                                Spacer(modifier = Modifier.width(scale.dp(12f)))
                                content()
                                Spacer(modifier = Modifier.width(scale.dp(18f)))
                            }
                        }
                    }
                }
            }
        }
    }
}