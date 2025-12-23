package com.example.brave_sailors.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.brave_sailors.ui.theme.White
import com.example.brave_sailors.ui.utils.RememberScaleConversion

// [ TO - DO ]: Provide a name for the penultimate item
enum class NavigationItem(
    val title: String,
    val icon: ImageVector,
    val textOnSelect: Boolean = false
) {
    Menu("MENU", Icons.Default.Menu, textOnSelect = true),
    Profile("PROFILE", Icons.Default.Person, textOnSelect = true),
    Home("HOME", Icons.Default.Anchor, textOnSelect = true),
    Game("", Icons.Default.Games, textOnSelect = true),
    Fleet("FLEET", Icons.Default.DirectionsBoat, textOnSelect = true),
}

// -- NAVIGATION BAR --
@Composable
fun NavigationBar(
    currentScreen: NavigationItem,
    onItemClick: (NavigationItem) -> Unit
) {
    val scale = RememberScaleConversion()

    val navHeight = scale.dp(188f)
    val spacing = scale.dp(4f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(navHeight),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.Bottom
    ) {
        NavigationItem.entries.forEach { item ->
            NavigationItemView(
                item = item,
                isSelected = item == currentScreen,
                onItemClick = { onItemClick(item) }
            )
        }
    }
}

@Composable
fun RowScope.NavigationItemView(
    item: NavigationItem,
    isSelected: Boolean,
    onItemClick: () -> Unit
) {
    val barAnimationDuration = 250
    val contentAnimationDuration = 300

    val interactionSource = remember { MutableInteractionSource() }
    val scale = RememberScaleConversion()

    val animationProgress by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = barAnimationDuration),
        label = "BarAnimation"
    )

    val animatedIconSize by animateDpAsState(
        targetValue = if (isSelected) scale.dp(120f) else scale.dp(84f),
        animationSpec = tween(durationMillis = contentAnimationDuration),
        label = "IconSizeAnimation"
    )

    val animatedWeight = 1f + animationProgress
    val cutSizeDp = scale.dp(34f)
    val topPaddingDp = cutSizeDp * (1f - animationProgress)

    val nonBgColorTop = Color(0xFF2A4080).copy(alpha = 0.75f)
    val nonBgColorBottom = Color(0xFF2A4080).copy(alpha = 0.90f)

    val bgColorBottom = Color(0xFF182A52)
    val bgColorTop = Color(0xFF1C3260)

    val selectedBrush = remember {
        Brush.verticalGradient(colors = listOf(bgColorTop, bgColorBottom))
    }

    val unselectedBrush = remember {
        Brush.linearGradient(
            colors = listOf(nonBgColorTop, nonBgColorBottom),
            start = Offset(0f, Float.POSITIVE_INFINITY),
            end = Offset(Float.POSITIVE_INFINITY, 0f)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .weight(animatedWeight)
            .padding(top = topPaddingDp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onItemClick
            )
            .drawBehind {
                val stroke = scale.dp(2f).toPx()

                val h = size.height
                val w = size.width

                val cutSize = ( cutSizeDp.toPx() ) * animationProgress

                val core = Path().apply {
                    moveTo(stroke, h)
                    lineTo(stroke, stroke + cutSize)
                    lineTo(cutSize + stroke, stroke)
                    lineTo(w - cutSize - stroke, stroke)
                    lineTo(w - stroke, stroke + cutSize)
                    lineTo(w - stroke, h)
                    close()
                }

                if (animationProgress > 0f)
                    drawPath(path = core, brush = selectedBrush, alpha = animationProgress)

                if (animationProgress < 1f)
                    drawRect(brush = unselectedBrush, alpha = 1f - animationProgress)

                if (animationProgress > 0f) {
                    val border = Path().apply {
                        moveTo(stroke, h)
                        lineTo(stroke, stroke + cutSize)
                        lineTo(cutSize + stroke, stroke)
                        lineTo(w - cutSize - stroke, stroke)
                        lineTo(w - stroke, stroke + cutSize)
                        lineTo(w - stroke, h)
                    }

                    drawPath(
                        path = border,
                        color = White.copy(alpha = animationProgress),
                        style = Stroke(
                            width = 4f,
                            join = StrokeJoin.Round,
                            cap = StrokeCap.Round
                        )
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (isSelected) Arrangement.SpaceBetween else Arrangement.Center,
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = if (isSelected) scale.dp(20f) else 0.dp)
                .wrapContentWidth(unbounded = true)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    modifier = Modifier.size(animatedIconSize),
                    tint = White
                )
            }

            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(
                    animationSpec = tween(durationMillis = contentAnimationDuration, delayMillis = barAnimationDuration)
                ) + scaleIn(
                    initialScale = 0f,
                    animationSpec = tween(durationMillis = contentAnimationDuration, delayMillis = barAnimationDuration)
                ) + expandVertically(
                    animationSpec = tween(durationMillis = contentAnimationDuration, delayMillis = barAnimationDuration)
                ),
                exit = fadeOut(
                    animationSpec = tween(durationMillis = 200)
                ) + scaleOut(
                    targetScale = 0f,
                    animationSpec = tween(durationMillis = 200)
                ) + shrinkVertically(
                    animationSpec = tween(durationMillis = 200)
                )
            ) {
                if (item.textOnSelect) {
                    Text(
                        text = item.title,
                        color = White,
                        textAlign = TextAlign.Center,
                        fontSize = scale.sp(28f),
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
                        softWrap = false
                    )
                }
            }
        }
    }
}