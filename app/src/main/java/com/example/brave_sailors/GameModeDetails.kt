package com.example.brave_sailors

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import com.example.brave_sailors.ui.components.BackButton
import com.example.brave_sailors.ui.components.ContinueButton
import com.example.brave_sailors.ui.components.DialogDifficulty
import com.example.brave_sailors.ui.components.DialogFiringRules
import com.example.brave_sailors.ui.theme.White
import com.example.brave_sailors.ui.utils.RememberScaleConversion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GameModeDetailsScreen(
    gameModeIndex: Int, // 0 = Single player vs. computer
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = false
        delay(200)
        isVisible = true
    }

    if (isVisible)
        Modal(gameModeIndex, onBack, onContinue)
}

@Composable
private fun Modal(
    gameModeIndex: Int,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val scale = RememberScaleConversion()
    val maxWidth = scale.dp(720f)

    val bgColor = Color(0xFF26768E)

    val scope = rememberCoroutineScope()

    var difficulty by remember { mutableStateOf("Normal") }
    var firingRule by remember { mutableStateOf("Chain attacks") }

    var buttonsVisible by remember { mutableStateOf(false) }

    var showDialogDifficulty by remember { mutableStateOf(false) }
    var showDialogFiringRules by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        buttonsVisible = true
    }

    fun triggerExit(callback: () -> Unit) {
        scope.launch {
            buttonsVisible = false
            delay(200)
            callback()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = maxWidth) // [ MEMO ]: Remove it if not necessary
                .padding(top = scale.dp(216f))
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = scale.dp(8f)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(scale.dp(80f)))

                    Box(
                        modifier = Modifier
                            .height(scale.dp(62f))
                            .background(bgColor)
                            .clip(RectangleShape)
                            .padding(horizontal = scale.dp(14f)),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "CUSTOM SETTINGS",
                            color = White,
                            fontSize = scale.sp(36f),
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

                Spacer(modifier = Modifier.height(scale.dp(74f)))

                if (gameModeIndex == 0) {
                    SettingsRow(
                        option = "DIFFICULTY",
                        value = difficulty,
                        color = bgColor,
                        onClick = { showDialogDifficulty = true }
                    )

                    Spacer(modifier = Modifier.height(scale.dp(26f)))
                }

                SettingsRow(
                    option = "FIRING RULES",
                    value = firingRule,
                    color = bgColor,
                    onClick = { showDialogFiringRules = true }
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = scale.dp(38f), end = scale.dp(38f), bottom = scale.dp(54f)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedVisibility(
                        visible = buttonsVisible,
                        enter = slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                        exit = slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(200)) + fadeOut(animationSpec = tween(200))
                    ) {
                        BackButton(
                            text = "Return",
                            onClick = { triggerExit(onBack) }
                        )
                    }

                    AnimatedVisibility(
                        visible = buttonsVisible,
                        enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                        exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(200)) + fadeOut(animationSpec = tween(200))
                    ) {
                        ContinueButton(
                            text = "Continue",
                            onClick = {  }
                        )
                    }
                }
            }
        }

        // -- DIALOGS --
        if (showDialogDifficulty) {
            DialogDifficulty(
                currentDifficulty = difficulty,
                onDismiss = { newDifficulty ->
                    if (difficulty != newDifficulty)
                        difficulty = newDifficulty

                    showDialogDifficulty = false
                }
            )
        }

        if (showDialogFiringRules) {
            DialogFiringRules(
                currentRule = firingRule,
                onDismiss = { newRule ->
                    if (firingRule != newRule)
                        firingRule = newRule

                    showDialogFiringRules = false
                }
            )
        }
    }
}

@Composable
private fun SettingsRow(
    option: String,
    value: String,
    color: Color,
    onClick: () -> Unit
) {
    val scale = RememberScaleConversion()

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bgScale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        label = "BgScale"
    )

    val animatedBgColor by animateColorAsState(
        targetValue = if (isPressed) color.copy(alpha = 0.75f) else color,
        label = "BgColor"
    )

    val borderOffset by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        label = "BorderOffset"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = scale.dp(40f)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = option,
            color = White,
            fontSize = scale.sp(26f),
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

        val widthDp = scale.dp(228f)
        val heightDp = scale.dp(92f)
        val cutSizeDp = scale.dp(16f)
        val maxShift = scale.dp(2f)

        Box(
            modifier = Modifier
                .width(widthDp)
                .height(heightDp)
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    val r = cutSizeDp.toPx()
                    val borderShift = maxShift.toPx() * borderOffset

                    scale(scale = bgScale, pivot = center) {
                        drawRoundRect(
                            color = animatedBgColor,
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
                        ) {  }
                    }
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value,
                color = White,
                textAlign = TextAlign.Center,
                fontSize = scale.sp(26f),
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
                modifier = Modifier.graphicsLayer {
                    scaleX = bgScale
                    scaleY = bgScale
                }
            )
        }
    }
}