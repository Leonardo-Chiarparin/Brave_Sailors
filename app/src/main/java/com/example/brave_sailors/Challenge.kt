package com.example.brave_sailors

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.brave_sailors.model.ProfileViewModel
import com.example.brave_sailors.ui.components.GoButton
import com.example.brave_sailors.ui.components.GridBackground
import com.example.brave_sailors.ui.theme.DarkBlue
import com.example.brave_sailors.ui.theme.Grey
import com.example.brave_sailors.ui.theme.LightBlue
import com.example.brave_sailors.ui.theme.LightGrey
import com.example.brave_sailors.ui.theme.Orange
import com.example.brave_sailors.ui.theme.White
import com.example.brave_sailors.ui.utils.RememberScaleConversion
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

enum class OverlayChallengeState {
    IDLE,
    EXITING_CHALLENGE,
    SHOWING_TORPEDO,
    SHOWING_CARGO,
    SHOWING_BILGE
}

@Composable
fun ChallengeScreen(
    viewModel: ProfileViewModel,
    onOpenTorpedo: () -> Unit,
    onOpenCargo: () -> Unit,
    onOpenBilge: () -> Unit
) {
    Modal(viewModel, onOpenTorpedo, onOpenCargo, onOpenBilge)
}

@Composable
private fun Modal(
    viewModel: ProfileViewModel,
    onOpenTorpedo: () -> Unit,
    onOpenCargo: () -> Unit,
    onOpenBilge: () -> Unit
) {
    val scale = RememberScaleConversion()
    val maxWidth = scale.dp(720f)
    val strokeDp = scale.dp(1f)

    var expandedIndexTorpedo by remember { mutableIntStateOf(-1) }
    var expandedIndexCargo by remember { mutableIntStateOf(-1) }
    var expandedIndexBilge by remember { mutableIntStateOf(-1) }

    val userState by viewModel.userState.collectAsState()

    var cooldownMs by remember { mutableLongStateOf(viewModel.getCooldownRemaining()) }

    LaunchedEffect(userState) {
        while (isActive) {
            val remaining = viewModel.getCooldownRemaining()
            cooldownMs = remaining

            if (remaining <= 0L) {
                delay(2000)
            } else {
                delay(1000)
            }
        }
    }

    val isLocked = cooldownMs > 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = maxWidth)
                .background(DarkBlue)
                .drawBehind {
                    val h = size.height
                    val w = size.width
                    val stroke = strokeDp.toPx()
                    val halfStroke = stroke / 2f

                    drawLine(Orange, Offset(0f, halfStroke), Offset(w, halfStroke), stroke)
                    drawLine(Orange, Offset(0f, h - halfStroke), Offset(w, h - halfStroke), stroke)
                }
                .padding(horizontal = scale.dp(16f), vertical = scale.dp(4f)),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(modifier = Modifier
                .fillMaxWidth(), contentAlignment = Alignment.Center
            ) {
                GridBackground(Modifier.matchParentSize(), color = LightBlue, 14f)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = scale.dp(160f), top = scale.dp(78f), start = scale.dp(10f), end = scale.dp(10f)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CompositionLocalProvider(
                        LocalOverscrollFactory provides null
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .height(scale.dp(510f)),
                            verticalArrangement = Arrangement.spacedBy(scale.dp(26f)),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            item {
                                ChallengeItem(
                                    title = "Stealth Mission: TORPEDO RUN",
                                    description = if (isLocked) "Systems recharging..." else "Guide a depth charge using the gyroscope.",
                                    isExpanded = expandedIndexTorpedo == 0,
                                    icon = Icons.Default.Explore,
                                    isLocked = isLocked,
                                    cooldownTime = cooldownMs,
                                    onToggle = {
                                        expandedIndexTorpedo = if (expandedIndexTorpedo == 0) -1 else 0
                                    },
                                    onGo = onOpenTorpedo
                                )
                            }

                            item {
                                ChallengeItem(
                                    title = "Balance test: CARGO HOLD",
                                    description = if (isLocked) "Systems recharging..." else "Keep the ammo crate steady in rough seas.",
                                    isExpanded = expandedIndexCargo == 0,
                                    icon = Icons.Default.CheckBoxOutlineBlank,
                                    isLocked = isLocked,
                                    cooldownTime = cooldownMs,
                                    onToggle = {
                                        expandedIndexCargo = if (expandedIndexCargo == 0) -1 else 0
                                    },
                                    onGo = onOpenCargo
                                )
                            }

                            item {
                                ChallengeItem(
                                    title = "Field check: BILGE PUMP",
                                    description = if (isLocked) "Systems recharging..." else "Shake the device to expel water.",
                                    isExpanded = expandedIndexBilge == 0,
                                    icon = Icons.Default.Water,
                                    isLocked = isLocked,
                                    cooldownTime = cooldownMs,
                                    onToggle = {
                                        expandedIndexBilge = if (expandedIndexBilge == 0) -1 else 0
                                    },
                                    onGo = onOpenBilge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun ChallengeItem(
    title: String,
    description: String,
    isExpanded: Boolean,
    icon: ImageVector,
    isLocked: Boolean,
    cooldownTime: Long,
    onToggle: () -> Unit,
    onGo: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = RememberScaleConversion()

    val headerColor = Color(0xFF1C3260)

    val maxCornerSize = scale.dp(24f)

    val headerShape = CutCornerShape(
        bottomStart = maxCornerSize,
        topEnd = maxCornerSize
    )

    // Formatting timer string for the locked state UI
    val timerString = remember(cooldownTime) {
        if (cooldownTime <= 0) "00:00"
        else {
            val totalSeconds = cooldownTime / 1000
            val m = totalSeconds / 60
            val s = totalSeconds % 60
            "${if (m < 10) "0$m" else "$m"}:${if (s < 10) "0$s" else "$s"}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(headerShape)
            .background(headerColor)
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable (
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onToggle
                )
                .padding(start = scale.dp(20f), end = scale.dp(20f), bottom = scale.dp(18f), top = scale.dp(8f)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isLocked) Grey else Orange,
                modifier = Modifier
                    .size(scale.dp(50f))
            )

            Spacer(modifier = Modifier.width(scale.dp(16f)))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (isLocked) LightGrey else White,
                    fontSize = scale.sp(28f),
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = scale.sp(2f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            LineHeightStyle.Alignment.Center,
                            LineHeightStyle.Trim.Both
                        ),
                        shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)
                    )
                )

                // Show timer if it is still locked
                if (isLocked) {
                    Text(
                        text = "Reloading: $timerString",
                        color = Color(0xFFFE0000),
                        fontSize = scale.sp(16f),
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = scale.sp(2f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(tween(300)) + fadeIn(tween(300)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = scale.dp(22f))
            ) {
                Spacer(modifier = Modifier.height(scale.dp(12f)))

                Text(
                    text = description,
                    color = if (isLocked) LightGrey else White,
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
                    )
                )

                Spacer(modifier = Modifier.height(scale.dp(14f)))

                Box(
                    modifier = Modifier.align(Alignment.End)
                ) {
                    GoButton(enabled = !isLocked, onClick = onGo)
                }

                Spacer(modifier = Modifier.height(scale.dp(14f)))
            }
        }
    }
}

