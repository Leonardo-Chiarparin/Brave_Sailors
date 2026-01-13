package com.example.brave_sailors

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import com.example.brave_sailors.data.local.database.entity.User
import com.example.brave_sailors.ui.components.DividerOrange
import com.example.brave_sailors.ui.components.GridBackground
import com.example.brave_sailors.ui.theme.DarkBlue
import com.example.brave_sailors.ui.theme.DeepBlue
import com.example.brave_sailors.ui.theme.LightBlue
import com.example.brave_sailors.ui.theme.Orange
import com.example.brave_sailors.ui.theme.White
import com.example.brave_sailors.ui.utils.RememberScaleConversion
import java.util.Locale

@Composable
fun StatisticsScreen(
    user: User?,
    onBack: () -> Unit
) {
    Modal(user, onBack)
}

@Composable
private fun Modal(
    user: User?,
    onBack: () -> Unit
) {
    val scale = RememberScaleConversion()
    val interactionSource = remember { MutableInteractionSource() }
    val maxWidth = scale.dp(720f)
    val strokeDp = scale.dp(1f)

    val closeButtonShape = CutCornerShape(bottomStart = scale.dp(34f))

    // -- DYNAMIC DATA CALCULATION --
    val currentXp = user?.currentXp ?: 0
    val xpStep = 1000 // XP required to complete a single cycle/level

    // Calculate the target XP for the *next* milestone dynamically based on current XP.
    // Example: If current XP is 1500, we want the bar to show progress towards 2000.
    // Logic: ((1500 / 1000) + 1) * 1000 = 2000.
    val targetXp = ((currentXp / xpStep) + 1) * xpStep

    // Calculate the specific progress within the current 1000 XP chunk using modulo.
    // Example: 1500 XP -> 500 XP accumulated in current level.
    val xpInCurrentLevel = currentXp % xpStep

    // Compute bar's percentage (0.0 -> 1.0) based on the chunk, not total XP.
    val xpProgress = (xpInCurrentLevel.toFloat() / xpStep.toFloat()).coerceIn(0f, 1f)

    // -- RANK CALCULATION --
    // Determines the title based on total current XP (1 win = ~10 XP)
    val rankTitle = when {
        currentXp >= 1000 -> "Grand Admiral" // 100+ wins
        currentXp >= 600 -> "Admiral"        // 60+ wins
        currentXp >= 300 -> "Vice-Admiral"   // 30+ wins
        currentXp >= 150 -> "Captain"        // 15+ wins
        currentXp >= 50 -> "Vice-Captain"    // 5+ wins
        else -> "Sailor"                     // Beginner
    }

    // List of general stats pairs for display
    val stats = listOf(
        "Total games played" to (user?.totalGamesPlayed ?: 0).toString(),
        "Victories" to (user?.wins ?: 0).toString(),
        "Defeats" to (user?.losses ?: 0).toString(),
        "Win rate (%)" to String.format(Locale.US, "%.1f", user?.winRate ?: 0f)
    )

    // List of detailed battle info pairs for display
    val infos = listOf(
        "Shots" to (user?.totalShotsFired ?: 0).toString(),
        "Boats sunken" to (user?.shipsDestroyed ?: 0).toString(),
        "Accuracy (%)" to String.format(Locale.US, "%.1f", user?.accuracy ?: 0f)
    )

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

                    drawLine(
                        color = Orange,
                        start = Offset(0f, halfStroke),
                        end = Offset(w, halfStroke),
                        strokeWidth = stroke
                    )

                    drawLine(
                        color = Orange,
                        start = Offset(0f, h - halfStroke),
                        end = Offset(w, h - halfStroke),
                        strokeWidth = stroke
                    )
                },
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = scale.dp(16f), vertical = scale.dp(4f)),
                contentAlignment = Alignment.Center
            ) {
                GridBackground(Modifier.matchParentSize(), color = LightBlue, 14f)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = scale.dp(64f)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(scale.dp(22f)))

                    Text(
                        text = "GENERAL STATISTICS",
                        color = White,
                        fontSize = scale.sp(36f),
                        textAlign = TextAlign.Center,
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

                    Spacer(modifier = Modifier.height(scale.dp(80f)))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = scale.dp(32f)),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = rankTitle,
                                color = White,
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
                                )
                            )

                            Spacer(modifier = Modifier.height(scale.dp(36f)))

                            // XP Progress Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(scale.dp(16f))
                                    .border(scale.dp(1f), Orange)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(xpProgress)
                                        .background(Orange)
                                )
                            }

                            Spacer(modifier = Modifier.height(scale.dp(14f)))

                            Text(
                                text = "$currentXp / $targetXp",
                                color = White,
                                fontSize = scale.sp(22f),
                                textAlign = TextAlign.Center,
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

                        Spacer(modifier = Modifier.height(scale.dp(84f)))

                        DividerOrange()

                        Spacer(modifier = Modifier.height(scale.dp(42f)))

                        stats.forEachIndexed { index, (label, value) ->
                            val bgColor = if (index % 2 == 0) DeepBlue.copy(alpha = 0.5f) else Color.Transparent

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(bgColor)
                                    .padding(horizontal = scale.dp(12f), vertical = scale.dp(16f)),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    color = White,
                                    fontSize = scale.sp(24f),
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

                                Text(
                                    text = value,
                                    color = White,
                                    textAlign = TextAlign.Right,
                                    fontSize = scale.sp(24f),
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

                        Spacer(modifier = Modifier.height(scale.dp(48f)))

                        DividerOrange()

                        Spacer(modifier = Modifier.height(scale.dp(42f)))

                        infos.forEachIndexed { index, (label, value) ->
                            val bgColor = if (index % 2 == 0) DeepBlue.copy(alpha = 0.5f) else Color.Transparent

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(bgColor)
                                    .padding(horizontal = scale.dp(12f), vertical = scale.dp(16f)),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    color = White,
                                    fontSize = scale.sp(24f),
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

                                Text(
                                    text = value,
                                    color = White,
                                    textAlign = TextAlign.Right,
                                    fontSize = scale.sp(24f),
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

                        Spacer(modifier = Modifier.height(scale.dp(68f)))
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(DarkBlue, shape = closeButtonShape)
                    .border(scale.dp(1f), Orange, shape = closeButtonShape)
                    .clip(closeButtonShape)
                    .size(scale.dp(92f))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onBack
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = White,
                    modifier = Modifier.size(scale.dp(28f))
                )
            }
        }
    }
}