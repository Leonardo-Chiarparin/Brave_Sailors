package com.example.brave_sailors.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import com.example.brave_sailors.ui.theme.LightGrey
import com.example.brave_sailors.ui.theme.Orange
import com.example.brave_sailors.ui.theme.TransparentGrey
import com.example.brave_sailors.ui.theme.White
import com.example.brave_sailors.ui.utils.RememberScaleConversion

@SuppressLint("UnrememberedMutableState")
@Composable
fun PrimaryButton(paddingH: Float, paddingV: Float, text: String, onClick: () -> Unit, enabled: Boolean = true) {
    val scale = RememberScaleConversion()

    val transition = rememberInfiniteTransition(label = "ShinyTransition")
    val progress by if (enabled) {
        transition.animateFloat(
            initialValue = -0.4f,
            targetValue = 1.6f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    delayMillis = 4150,
                    durationMillis = 850,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "ShinyOffset"
        )
    } else {
        mutableFloatStateOf(-0.4f)
    }

    val cutSizeDp = scale.dp(36f)
    val paddingHorizontal = scale.dp(paddingH)
    val paddingVertical = scale.dp(paddingV)

    val interactionSource = remember { MutableInteractionSource() }

    val bgColor = Orange.copy(alpha = 0.90f)
    val buttonShape = CutCornerShape(bottomStart = cutSizeDp, bottomEnd = cutSizeDp, topStart = 0.dp, topEnd = 0.dp)

    Box(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.75f)
            .clip(buttonShape)
            .border(BorderStroke(scale.dp(1f), LightGrey.copy(alpha = 0.75f)), buttonShape)
            .background(bgColor)
            .drawBehind {
                if (enabled) {
                    val h = size.height
                    val w = size.width

                    val shineWidth = scale.dp(72f).toPx()

                    val startX = ( progress * ( w + shineWidth ) ) - shineWidth
                    val endX = startX + shineWidth

                    val brush = Brush.linearGradient(
                        colors = listOf(
                            White.copy(alpha = 0.0f),
                            White.copy(alpha = 0.05f),
                            White.copy(alpha = 0.25f),
                            White.copy(alpha = 0.30f),
                            White.copy(alpha = 0.25f),
                            White.copy(alpha = 0.05f),
                            White.copy(alpha = 0.0f)
                        ),
                        start = Offset(startX, h),
                        end = Offset(endX, 0f)
                    )
                    drawRect(brush = brush, size = this.size)
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = true,
                    color = Color.Transparent
                ),
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = paddingHorizontal, vertical = paddingVertical),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = White,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = scale.sp(40f),
            letterSpacing = scale.sp(4f),
            style = TextStyle(
                platformStyle = PlatformTextStyle(
                    includeFontPadding = false
                ),
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both
                ),
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.5f),
                    offset = Offset(2f, 2f),
                    blurRadius = 4f
                )
            )
        )
    }
}

@Composable
fun SecondaryButton(paddingH: Float, paddingV: Float, text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scale = RememberScaleConversion()

    val paddingHorizontal = scale.dp(paddingH)
    val paddingVertical = scale.dp(paddingV)

    val cutSizeDp = scale.dp(18f)
    val buttonShape = CutCornerShape(
        bottomStart = cutSizeDp,
        bottomEnd = cutSizeDp,
        topStart = 0.dp,
        topEnd = 0.dp
    )

    val baseColor = TransparentGrey.copy(alpha = 0.90f)
    val pressedColor = TransparentGrey.copy(alpha = 0.75f)

    val borderColor = LightGrey

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bgColor by animateColorAsState(
        targetValue = if (isPressed) pressedColor else baseColor,
        label = "ButtonColorAnimation"
    )

    val shrink by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        label = "ButtonSqueezeAnimation"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = shrink
                scaleY = shrink
            }
            .clip(buttonShape)
            .background(bgColor, buttonShape)
            .drawBehind {
                val strokePx = scale.dp(1f).toPx()

                val cutPx = cutSizeDp.toPx()

                val halfStroke = strokePx / 2f

                val w = size.width
                val h = size.height

                val path = Path().apply {
                    moveTo(halfStroke, 0f)
                    lineTo(halfStroke, h - cutPx)
                    lineTo(cutPx, h - halfStroke)
                    moveTo(w - cutPx, h - halfStroke)
                    lineTo(w - halfStroke, h - cutPx)
                    lineTo(w - halfStroke, 0f)
                }

                drawPath(
                    path = path,
                    color = borderColor,
                    style = Stroke(
                        width = strokePx,
                        cap = StrokeCap.Butt,
                        join = StrokeJoin.Miter
                    )
                )
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = paddingHorizontal, vertical = paddingVertical),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = White,
            fontSize = scale.sp(22f),
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
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
                    color = Color.Black.copy(alpha = 0.75f),
                    offset = Offset(2f, 2f),
                    blurRadius = 4f
                )
            )
        )
    }
}