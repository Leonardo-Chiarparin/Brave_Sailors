package com.example.brave_sailors.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.min
import com.example.brave_sailors.ui.theme.Grey
import com.example.brave_sailors.ui.theme.LightGrey
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
    val scale = RememberScaleConversion()

    val minInnerWidth = scale.dp(88f)
    val minOuterWidth = scale.dp(104f)
    val maxWidth = scale.dp(632f)

    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(start) {
        if (start) {
            animationProgress.snapTo(0f)

            animationProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 4750,
                    easing = LinearEasing
                )
            )

            onDone()
        }
    }

    val progress = animationProgress.value

    val seg1Fin = 0.5f / 5.75f
    val seg2Fin = 0.75f / 5.75f
    val seg3Fin = 1.25f / 5.75f
    val seg4Fin = 1.5f / 5.75f
    val seg5Fin = 2.0f / 5.75f
    val seg6Fin = 3.25f / 5.75f
    val seg7Fin = 3.75f / 5.75f
    val seg8Fin = 4.0f / 5.75f
    val seg9Fin = 4.5f / 5.75f
    val seg10Fin = 4.75f / 5.75f
    val seg11Fin = 5.25f / 5.25f

    var alpha = 0f
    var innerSize: Dp = 0.dp
    var outerSize: Dp = 0.dp
    var width: Dp = 0.dp
    var isCircle = false
    var showContent = false

    when {
        progress <= seg1Fin -> {
            val fraction = (progress / seg1Fin).coerceIn(0f, 1f)

            alpha = fraction
            isCircle = true
            outerSize = lerp(0.dp, minOuterWidth, fraction)
        }

        progress <= seg2Fin -> {
            alpha = 1f
            isCircle = true
            outerSize = minOuterWidth
        }

        progress <= seg3Fin -> {
            val fraction = ((progress - seg2Fin) / (seg3Fin - seg2Fin)).coerceIn(0f, 1f)

            alpha = 1f
            isCircle = true
            innerSize = lerp(0.dp, minInnerWidth, fraction)
            outerSize = minOuterWidth
        }

        progress <= seg4Fin -> {
            alpha = 1f
            isCircle = true
            innerSize = minInnerWidth
            outerSize = minOuterWidth
        }

        progress <= seg5Fin -> {
            val fraction = ((progress - seg4Fin) / (seg5Fin - seg4Fin)).coerceIn(0f, 1f)

            alpha = 1f
            isCircle = false
            innerSize = minInnerWidth
            outerSize = minOuterWidth
            width = lerp(minOuterWidth, maxWidth, fraction)
        }

        progress <= seg6Fin -> {
            alpha = 1f
            isCircle = false
            innerSize = minInnerWidth
            outerSize = minOuterWidth
            showContent = true
            width = maxWidth
        }

        progress <= seg7Fin -> {
            val fraction = ((progress - seg6Fin) / (seg7Fin - seg6Fin)).coerceIn(0f, 1f)

            alpha = 1f
            isCircle = false
            innerSize = minInnerWidth
            outerSize = minOuterWidth
            width = lerp(maxWidth, minOuterWidth, fraction)
        }

        progress <= seg8Fin -> {
            alpha = 1f
            isCircle = true
            innerSize = minInnerWidth
            outerSize = minOuterWidth
            width = minOuterWidth
        }

        progress <= seg9Fin -> {
            val fraction = ((progress - seg8Fin) / (seg9Fin - seg8Fin)).coerceIn(0f, 1f)

            alpha = 1f
            isCircle = true
            innerSize = lerp(minInnerWidth, 0.dp, fraction)
            outerSize = minOuterWidth
        }

        // 🔧 CORRETTO: mancava innerSize = 0.dp
        progress <= seg10Fin -> {
            alpha = 1f
            isCircle = true
            innerSize = 0.dp          // <-- AGGIUNTO
            outerSize = minOuterWidth
        }

        // 🔧 CORRETTO: mancava innerSize = 0.dp durante fade-out
        progress < seg11Fin -> {
            val fraction = ((progress - seg10Fin) / (seg11Fin - seg10Fin)).coerceIn(0f, 1f)

            alpha = 1f - fraction
            isCircle = true
            innerSize = 0.dp          // <-- AGGIUNTO
            outerSize = lerp(minOuterWidth, 0.dp, fraction)
        }

        else -> {
            alpha = 0f
        }
    }

    if (alpha <= 0f)
        return

    Box(
        modifier = modifier.height(minOuterWidth),
        contentAlignment = Alignment.Center
    ) {
        if (isCircle && (outerSize > 0.dp)) {
            Box(
                modifier = Modifier
                    .graphicsLayer(alpha = alpha)
                    .size(outerSize)
                    .shadow(scale.dp(2f), CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(White),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(innerSize)
                        .clip(CircleShape)
                        .padding(all = scale.dp(8f)),
                    content = avatar
                )
            }
        } else {
            if (!isCircle && width > 0.dp) {
                Box(
                    modifier = Modifier
                        .graphicsLayer(alpha = alpha)
                        .width(width)
                        .height(minOuterWidth),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = White,
                        shape = RoundedCornerShape(50),
                        shadowElevation = scale.dp(2f),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Row(
                            modifier = Modifier.padding(all = scale.dp(8f)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(innerSize),
                                content = avatar
                            )

                            if (showContent) {
                                Spacer(modifier = Modifier.width(scale.dp(14f)))
                                content()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Screen() {
    val scale = RememberScaleConversion()
    var showPill by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showPill = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Popup(
            start = showPill,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = scale.dp(98f)),
            avatar = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Grey)
                )
            },
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Hello Username",
                        color = Color.Black,
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
                            )
                        )
                    )

                    Spacer(modifier = Modifier.height(scale.dp(16f)))

                    Text(
                        text = "user@example.com",
                        color = LightGrey,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        fontSize = scale.sp(20f),
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
                }
            },
            onDone = {  }
        )
    }
}