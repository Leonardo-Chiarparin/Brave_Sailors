package com.example.brave_sailors.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.brave_sailors.ui.theme.DarkBlue
import com.example.brave_sailors.ui.theme.LightGrey
import com.example.brave_sailors.ui.theme.Orange
import com.example.brave_sailors.ui.theme.TransparentGrey
import com.example.brave_sailors.ui.theme.White
import com.example.brave_sailors.ui.utils.RememberScaleConversion
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@SuppressLint("UnrememberedMutableState")
@Composable
fun PrimaryButton(paddingH: Float, paddingV: Float, text: String, onClick: () -> Unit, enabled: Boolean = true) {
    val scale = RememberScaleConversion()

    val lifecycleOwner = LocalLifecycleOwner.current
    var isResumed by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isResumed = (event == Lifecycle.Event.ON_RESUME)
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val shineProgress = remember { Animatable(-0.4f) }

    LaunchedEffect(enabled, isResumed) {
        if (enabled && isResumed) {
            while (isActive) {
                shineProgress.animateTo(
                    targetValue = 1.6f,
                    animationSpec = tween(
                        durationMillis = 850,
                        easing = LinearEasing
                    )
                )

                delay(4150)

                shineProgress.snapTo(-0.4f)
            }
        }
        else
            shineProgress.snapTo(-0.4f)
    }

    val progress = shineProgress.value

    val cutSizeDp = scale.dp(36f)
    val paddingHorizontal = scale.dp(paddingH)
    val paddingVertical = scale.dp(paddingV)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bgColor by animateColorAsState(targetValue = if (isPressed) Orange.copy(alpha = 0.75f) else Orange.copy(alpha = 0.90f), label = "ButtonColorAnimation")
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

                    if (progress > -0.4f)
                        drawRect(brush = brush, size = this.size)
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
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
fun SecondaryButton(paddingH: Float, paddingV: Float, text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val scale = RememberScaleConversion()

    val paddingHorizontal = scale.dp(paddingH)
    val paddingVertical = scale.dp(paddingV)

    val cutSizeDp = scale.dp(12f)
    val buttonShape = CutCornerShape(
        bottomStart = cutSizeDp,
        bottomEnd = cutSizeDp,
        topStart = 0.dp,
        topEnd = 0.dp
    )

    val baseColor = TransparentGrey.copy(alpha = 0.75f)
    val pressedColor = TransparentGrey.copy(alpha = 0.5f)

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
                enabled = enabled,
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

@SuppressLint("UnrememberedMutableState")
@Composable
fun TertiaryButton(paddingH: Float, paddingV: Float, text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icons: @Composable () -> Unit) {
    val scale = RememberScaleConversion()

    val lifecycleOwner = LocalLifecycleOwner.current
    var isResumed by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isResumed = (event == Lifecycle.Event.ON_RESUME)
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bgScale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        label = "BackgroundScale")

    val borderOffset by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        label = "BorderOffset"
    )

    val shineProgress = remember { Animatable(-0.4f) }

    LaunchedEffect(enabled, isResumed) {
        if (enabled && isResumed) {
            while (isActive) {
                shineProgress.animateTo(
                    targetValue = 1.6f,
                    animationSpec = tween(durationMillis = 850, easing = LinearEasing)
                )

                delay(4150)
                shineProgress.snapTo(-0.4f)
            }
        } else shineProgress.snapTo(-0.4f)
    }

    val progress = shineProgress.value

    val cutSizeDp = scale.dp(16f)
    val paddingHorizontal = scale.dp(paddingH)
    val paddingVerticalBottom = scale.dp(paddingV)
    val paddingVerticalTop = scale.dp(paddingV / 2f)

    val widthDp = scale.dp(320f)
    val heightDp = scale.dp(154f)

    val bgColor by animateColorAsState(targetValue = if (isPressed) Orange.copy(alpha = 0.75f) else Orange.copy(alpha = 0.90f), label = "ButtonColorAnimation")

    val maxShift = scale.dp(2f)

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.75f)
            .height(heightDp)
            .width(widthDp)
            .drawBehind {
                val w = size.width
                val h = size.height
                val r = cutSizeDp.toPx()

                val borderShift = maxShift.toPx() * borderOffset

                scale(scale = bgScale, pivot = center) {
                    drawRoundRect(
                        color = bgColor,
                        cornerRadius = CornerRadius(r, r),
                        size = size
                    )
                }

                val strokeWidth = scale.dp(2f).toPx()
                val halfStroke = strokeWidth / 2f
                val arcSize = Size(2 * r, 2 * r)

                translate(left = -borderShift, top = 0f) {
                    val leftPath = Path().apply {
                        arcTo(
                            rect = Rect(offset = Offset(halfStroke, halfStroke), size = arcSize),
                            startAngleDegrees = 270f,
                            sweepAngleDegrees = -90f,
                            forceMoveTo = true
                        )

                        lineTo(halfStroke, h - r)

                        arcTo(
                            rect = Rect(
                                offset = Offset(halfStroke, h - 2 * r - halfStroke),
                                size = arcSize
                            ),
                            startAngleDegrees = 180f,
                            sweepAngleDegrees = -90f,
                            forceMoveTo = false
                        )
                    }

                    drawPath(path = leftPath, color = White, style = Stroke(width = strokeWidth))
                }

                translate(left = borderShift, top = 0f) {
                    val rightPath = Path().apply {
                        arcTo(
                            rect = Rect(
                                offset = Offset(w - 2 * r - halfStroke, halfStroke),
                                size = arcSize
                            ),
                            startAngleDegrees = 270f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = true
                        )

                        lineTo(w - halfStroke, h - r)

                        arcTo(
                            rect = Rect(
                                offset = Offset(w - 2 * r - halfStroke, h - 2 * r - halfStroke),
                                size = arcSize
                            ),
                            startAngleDegrees = 0f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false
                        )
                    }

                    drawPath(path = rightPath, color = White, style = Stroke(width = strokeWidth))
                }

                if (enabled) {
                    scale(scale = bgScale, pivot = center) {
                        clipPath(
                            path = Path().apply {
                                addRoundRect(
                                    RoundRect(
                                        rect = Rect(0f, 0f, w, h),
                                        cornerRadius = CornerRadius(r, r)
                                    )
                                )
                            }
                        ) {
                            val shineWidth = scale.dp(72f).toPx()
                            val startX = (progress * (w + shineWidth)) - shineWidth
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

                            if (progress > -0.4f)
                                drawRect(brush = brush, size = size)
                        }
                    }
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = paddingHorizontal, end = paddingHorizontal, bottom = paddingVerticalBottom, top = paddingVerticalTop)
                .graphicsLayer {
                    scaleX = bgScale
                    scaleY = bgScale
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            icons()

            Text(
                text = text,
                color = White,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = scale.sp(26f),
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
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = Offset(2f, 2f),
                        blurRadius = 4f
                    )
                )
            )
        }
    }
}

@Composable
fun QuaternaryButton(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scale = RememberScaleConversion()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bgScale by animateFloatAsState(targetValue = if (isPressed) 0.975f else 1f, label = "bgScale")

    val heightDp = scale.dp(122f)
    val widthDp = scale.dp(366f)
    val cutSizeDp = scale.dp(16f)

    val baseColor = TransparentGrey.copy(alpha = 0.75f)
    val pressedColor = TransparentGrey.copy(alpha = 0.5f)

    val borderColor = White

    val bgColor by animateColorAsState(
        targetValue = if (isPressed) pressedColor else baseColor,
        label = "ButtonColorAnimation"
    )

    Box(
        modifier = modifier
            .height(heightDp)
            .width(widthDp)
            .drawBehind {
                val w = size.width
                val h = size.height
                val r = cutSizeDp.toPx()

                scale(scale = bgScale, pivot = center) {
                    drawRoundRect(
                        color = bgColor,
                        cornerRadius = CornerRadius(r, r),
                        size = size
                    )
                }

                val strokeWidth = scale.dp(2f).toPx()
                val halfStroke = strokeWidth / 2f
                val arcSize = Size(2 * r, 2 * r)

                translate(left = 0f, top = 0f) {
                    val leftPath = Path().apply {
                        arcTo(Rect(Offset(halfStroke, halfStroke), arcSize), 270f, -90f, true)
                        lineTo(halfStroke, h - r)
                        arcTo(Rect(Offset(halfStroke, h - 2 * r - halfStroke), arcSize), 180f, -90f, false)
                    }

                    drawPath(path = leftPath, color = borderColor, style = Stroke(width = strokeWidth))
                }

                translate(left = 0f, top = 0f) {
                    val rightPath = Path().apply {
                        arcTo(Rect(Offset(w - 2 * r - halfStroke, halfStroke), arcSize), 270f, 90f, true)
                        lineTo(w - halfStroke, h - r)
                        arcTo(Rect(Offset(w - 2 * r - halfStroke, h - 2 * r - halfStroke), arcSize), 0f, 90f, false)
                    }

                    drawPath(path = rightPath, color = borderColor, style = Stroke(width = strokeWidth))
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = bgScale
                    scaleY = bgScale
                }
                .padding(horizontal = scale.dp(28f)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = White,
                modifier = Modifier
                    .size(scale.dp(84f))
            )

            Box(
                modifier = Modifier
                    .width(scale.dp(182f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = White,
                    textAlign = TextAlign.Center,
                    fontSize = scale.sp(28f),
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
    }
}

@Composable
fun FifthButton(text: String, onClick: () -> Unit) {
    val scale = RememberScaleConversion()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bgScale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "bgScale")
    val bgColor by animateColorAsState(
        targetValue = if (isPressed) TransparentGrey.copy(alpha = 0.50f) else TransparentGrey.copy(alpha = 0.75f),
        label = "color"
    )

    Box(
        modifier = Modifier
            .height(scale.dp(92f))
            .drawBehind {
                drawButtonBorder(
                    this,
                    bgScale,
                    bgColor,
                    scale.dp(16f).toPx(),
                    scale.dp(2f).toPx(),
                    White
                )
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = scale.dp(6f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = White,
            fontSize = scale.sp(24f),
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            letterSpacing = scale.sp(2f),
            style = TextStyle(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(
                    LineHeightStyle.Alignment.Center,
                    LineHeightStyle.Trim.Both
                ),
                shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)
            ),
            modifier = Modifier.graphicsLayer { scaleX = bgScale; scaleY = bgScale }
        )
    }
}

@Composable
fun SixthButton(onClick: () -> Unit, imagePainter: Painter) {
    val scale = RememberScaleConversion()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bgScale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "bgScale")

    val bgColor by animateColorAsState(
        targetValue = if (isPressed) TransparentGrey.copy(alpha = 0.50f) else TransparentGrey.copy(alpha = 0.75f),
        label = "color"
    )

    val sizeDp = scale.dp(120f)

    val cornerRadiusDp = scale.dp(16f)

    Box(
        modifier = Modifier
            .size(sizeDp)
            .drawBehind {
                drawButtonBorder(
                    this,
                    bgScale,
                    bgColor,
                    cornerRadiusDp.toPx(),
                    scale.dp(2f).toPx(),
                    White
                )
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(all = scale.dp(6f)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = imagePainter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = bgScale
                    scaleY = bgScale
                }
                .clip(RoundedCornerShape(scale.dp(12f)))
        )
    }
}

@Composable
fun SeventhButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    val scale = RememberScaleConversion()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bgScale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "bgScale")
    val bgColor by animateColorAsState(
        targetValue = if (isPressed) TransparentGrey.copy(alpha = 0.50f) else TransparentGrey.copy(alpha = 0.75f),
        label = "color"
    )

    Box(
        modifier = Modifier
            .width(scale.dp(156f))
            .height(scale.dp(108f))
            .drawBehind {
                drawButtonBorder(
                    this,
                    bgScale,
                    bgColor,
                    scale.dp(16f).toPx(),
                    scale.dp(2f).toPx(),
                    White
                )
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = scale.dp(8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.graphicsLayer { scaleX = bgScale; scaleY = bgScale },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = White, modifier = Modifier.size(scale.dp(50f)))
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = text,
                color = White,
                fontSize = scale.sp(22f),
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                letterSpacing = scale.sp(2f),
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = LineHeightStyle(
                        LineHeightStyle.Alignment.Center,
                        LineHeightStyle.Trim.Both
                    ),
                    shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)
                )
            )
        }
    }
}

fun drawButtonBorder(
    scope: DrawScope,
    scaleFactor: Float,
    bgColor: Color,
    cutSizePx: Float,
    strokeWidthPx: Float,
    borderColor: Color
) {
    with(scope) {
        val w = size.width
        val h = size.height

        scale(scale = scaleFactor, pivot = center) {
            drawRoundRect(
                color = bgColor,
                cornerRadius = CornerRadius(cutSizePx, cutSizePx),
                size = size
            )
        }

        val halfStroke = strokeWidthPx / 2f
        val arcSize = Size(2 * cutSizePx, 2 * cutSizePx)

        translate(left = 0f, top = 0f) {
            val leftPath = Path().apply {
                arcTo(Rect(Offset(halfStroke, halfStroke), arcSize), 270f, -90f, true)
                lineTo(halfStroke, h - cutSizePx)
                arcTo(Rect(Offset(halfStroke, h - 2 * cutSizePx - halfStroke), arcSize), 180f, -90f, false)
            }
            drawPath(path = leftPath, color = borderColor, style = Stroke(width = strokeWidthPx))
        }

        translate(left = 0f, top = 0f) {
            val rightPath = Path().apply {
                arcTo(Rect(Offset(w - 2 * cutSizePx - halfStroke, halfStroke), arcSize), 270f, 90f, true)
                lineTo(w - halfStroke, h - cutSizePx)
                arcTo(Rect(Offset(w - 2 * cutSizePx - halfStroke, h - 2 * cutSizePx - halfStroke), arcSize), 0f, 90f, false)
            }
            drawPath(path = rightPath, color = borderColor, style = Stroke(width = strokeWidthPx))
        }
    }
}

@Composable
fun CloseButton(
    onClick: () -> Unit,
    shape: CutCornerShape,
    modifier: Modifier = Modifier
) {
    val scale = RememberScaleConversion()
    val interactionSource = remember { MutableInteractionSource() }
    val buttonSize = scale.dp(74f)

    Box(
        modifier = modifier
            .padding(top = scale.dp(50f))
            .size(buttonSize)
            .background(DarkBlue, shape = shape)
            .border(BorderStroke(scale.dp(1f), Orange), shape = shape)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            tint = White,
            modifier = Modifier.size(scale.dp(26f))
        )
    }
}

@Composable
fun BackButton(text: String, onClick: () -> Unit) {
    val scale = RememberScaleConversion()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bgScale by animateFloatAsState(targetValue = if (isPressed) 0.975f else 1f, label = "bgScale")
    val borderOffset by animateFloatAsState(targetValue = if (isPressed) 1f else 0f, label = "borderOffset")

    val baseColor = TransparentGrey.copy(alpha = 0.75f)
    val pressedColor = TransparentGrey.copy(alpha = 0.5f)
    val bgColor by animateColorAsState(targetValue = if (isPressed) pressedColor else baseColor, label = "ButtonColorAnimation")

    val cutSizeDp = scale.dp(16f)
    val strokeWidthDp = scale.dp(2f)
    val maxShift = scale.dp(2f)

    Box(
        modifier = Modifier
            .width(scale.dp(288f))
            .height(scale.dp(172f))
            .drawBehind {
                val w = size.width
                val h = size.height
                val r = cutSizeDp.toPx()
                val strokeW = strokeWidthDp.toPx()
                val halfStroke = strokeW / 2f
                val shift = maxShift.toPx() * borderOffset

                scale(scale = bgScale, pivot = center) {
                    drawRoundRect(
                        color = bgColor,
                        cornerRadius = CornerRadius(r, r),
                        size = size
                    )
                }

                val arcSize = Size(2 * r, 2 * r)

                translate(left = -shift, top = 0f) {
                    val leftPath = Path().apply {
                        arcTo(Rect(Offset(halfStroke, halfStroke), arcSize), 270f, -90f, true)
                        lineTo(halfStroke, h - r)
                        arcTo(Rect(Offset(halfStroke, h - 2 * r - halfStroke), arcSize), 180f, -90f, false)
                    }
                    drawPath(leftPath, White, style = Stroke(strokeW))
                }

                translate(left = shift, top = 0f) {
                    val rightPath = Path().apply {
                        arcTo(Rect(Offset(w - 2 * r - halfStroke, halfStroke), arcSize), 270f, 90f, true)
                        lineTo(w - halfStroke, h - r)
                        arcTo(Rect(Offset(w - 2 * r - halfStroke, h - 2 * r - halfStroke), arcSize), 0f, 90f, false)
                    }
                    drawPath(rightPath, White, style = Stroke(strokeW))
                }
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = scale.dp(36f), start = scale.dp(18f))
                .graphicsLayer {
                    scaleX = bgScale
                    scaleY = bgScale
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(scale.dp(42f), scale.dp(84f))
            ) {
                Arrow(isLeft = true, modifier = Modifier.fillMaxSize())
            }

            Text(
                text = text,
                color = White,
                fontSize = scale.sp(34f),
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                letterSpacing = scale.sp(2f),
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = LineHeightStyle(
                        LineHeightStyle.Alignment.Center,
                        LineHeightStyle.Trim.Both
                    ),
                    shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)
                )
            )
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@Composable
fun ContinueButton(text: String, onClick: () -> Unit) {
    val scale = RememberScaleConversion()

    val lifecycleOwner = LocalLifecycleOwner.current
    var isResumed by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> isResumed = (event == Lifecycle.Event.ON_RESUME) }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bgScale by animateFloatAsState(targetValue = if (isPressed) 0.975f else 1f, label = "scale")
    val borderOffset by animateFloatAsState(targetValue = if (isPressed) 1f else 0f, label = "borderOffset")

    val shineProgress = remember { Animatable(-0.4f) }
    LaunchedEffect(isResumed) {
        if (isResumed) {
            while (isActive) {
                shineProgress.animateTo(2.0f, tween(850, easing = LinearEasing))
                delay(4150)
                shineProgress.snapTo(-0.4f)
            }
        }
        else
            shineProgress.snapTo(-0.4f)
    }

    val progress = shineProgress.value

    val bgColor by animateColorAsState(targetValue = if (isPressed) Orange.copy(alpha = 0.75f) else Orange.copy(alpha = 0.90f), label = "ButtonColorAnimation")
    val cutSizeDp = scale.dp(16f)
    val strokeWidthDp = scale.dp(2f)
    val maxShift = scale.dp(2f)

    Box(
        modifier = Modifier
            .width(scale.dp(288f))
            .height(scale.dp(172f))
            .drawBehind {
                val w = size.width
                val h = size.height
                val r = cutSizeDp.toPx()
                val strokeW = strokeWidthDp.toPx()
                val halfStroke = strokeW / 2f
                val shift = maxShift.toPx() * borderOffset

                scale(scale = bgScale, pivot = center) {
                    drawRoundRect(
                        color = bgColor,
                        cornerRadius = CornerRadius(r, r),
                        size = size
                    )
                }

                val arcSize = Size(2 * r, 2 * r)

                translate(left = -shift, top = 0f) {
                    val leftPath = Path().apply {
                        arcTo(Rect(Offset(halfStroke, halfStroke), arcSize), 270f, -90f, true)
                        lineTo(halfStroke, h - r)
                        arcTo(Rect(Offset(halfStroke, h - 2 * r - halfStroke), arcSize), 180f, -90f, false)
                    }
                    drawPath(leftPath, White, style = Stroke(strokeW))
                }

                translate(left = shift, top = 0f) {
                    val rightPath = Path().apply {
                        arcTo(Rect(Offset(w - 2 * r - halfStroke, halfStroke), arcSize), 270f, 90f, true)
                        lineTo(w - halfStroke, h - r)
                        arcTo(Rect(Offset(w - 2 * r - halfStroke, h - 2 * r - halfStroke), arcSize), 0f, 90f, false)
                    }
                    drawPath(rightPath, White, style = Stroke(strokeW))
                }

                scale(scale = bgScale, pivot = center) {
                    val clipPath = Path().apply {
                        addRoundRect(RoundRect(Rect(0f, 0f, w, h), CornerRadius(r, r)))
                    }

                    clipPath(clipPath) {
                        val shineWidth = scale.dp(72f).toPx()
                        val startX = (progress * (w + shineWidth)) - shineWidth
                        val endX = startX + shineWidth

                        val brush = Brush.linearGradient(
                            colors = listOf(White.copy(0f), White.copy(0.05f), White.copy(0.25f), White.copy(0.30f), White.copy(0.25f), White.copy(0.05f), White.copy(0f)),
                            start = Offset(startX, h), end = Offset(endX, 0f)
                        )
                        if (progress > -0.4f) drawRect(brush = brush, size = size)
                    }
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = scale.dp(36f), end = scale.dp(18f))
                .graphicsLayer {
                    scaleX = bgScale
                    scaleY = bgScale
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = text,
                color = White,
                fontSize = scale.sp(34f),
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                letterSpacing = scale.sp(2f),
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = LineHeightStyle(
                        LineHeightStyle.Alignment.Center,
                        LineHeightStyle.Trim.Both
                    ),
                    shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)
                )
            )

            Box(
                modifier = Modifier
                    .size(scale.dp(42f), scale.dp(84f))
            ) {
                Arrow(isLeft = false, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun Arrow(
    isLeft: Boolean,
    modifier: Modifier = Modifier
) {
    val scale = RememberScaleConversion()
    Canvas(modifier = modifier.height(scale.dp(84f)).width(scale.dp(42f))) {
        val gap = scale.dp(21f).toPx()
        val h = size.height
        val w = size.width
        val path = Path().apply {
            if (isLeft) {
                moveTo(0f, h * 0.5f); lineTo(w, 0f); lineTo(w, gap); lineTo(gap, h * 0.5f); lineTo(w, h - gap); lineTo(w, h); close()
            } else {
                moveTo(0f, 0f); lineTo(w, h * 0.5f); lineTo(0f, h); lineTo(0f, h - gap); lineTo(w - gap, h * 0.5f); lineTo(0f, gap); close()
            }
        }
        drawPath(path = path, color = White, style = Stroke(width = 1f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(path = path, color = White)
    }
}