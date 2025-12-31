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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.example.brave_sailors.ui.components.GridBackground
import com.example.brave_sailors.ui.components.SecondaryButton
import com.example.brave_sailors.ui.theme.DarkBlue
import com.example.brave_sailors.ui.theme.LightBlue
import com.example.brave_sailors.ui.theme.Orange
import com.example.brave_sailors.ui.theme.White
import com.example.brave_sailors.ui.utils.RememberScaleConversion

@Composable
fun AccountSettingsScreen(
    user: User?,
    onBack: () -> Unit
) {
    val scale = RememberScaleConversion()
    val interactionSource = remember { MutableInteractionSource() }
    val maxWidth = scale.dp(720f)
    val strokeDp = scale.dp(1f)

    val closeButtonShape = CutCornerShape(bottomStart = scale.dp(34f))

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
                GridBackground(Modifier.matchParentSize(), color = LightBlue, 18f)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(scale.dp(22f)))

                    Text(
                        text = "ACCOUNT SETTINGS AND\nSUPPORT",
                        color = White,
                        fontSize = scale.sp(34f),
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

                    Spacer(modifier = Modifier.height(scale.dp(76f)))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = scale.dp(72f)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        Text(
                            text = "Users can register their email address to play on various devices with the\nsame profile.",
                            color = White,
                            fontSize = scale.sp(24f),
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
                            ),
                            modifier = Modifier.padding(horizontal = scale.dp(20f))
                        )

                        Spacer(modifier = Modifier.height(scale.dp(108f)))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SecondaryButton(
                                paddingH = 34f,
                                paddingV = 20f,
                                text = "Reset",
                                onClick = { }
                            )

                            Spacer(modifier = Modifier.width(scale.dp(26f)))

                            SecondaryButton(
                                paddingH = 34f,
                                paddingV = 20f,
                                text = "Enter",
                                onClick = { }
                            )

                            Spacer(modifier = Modifier.width(scale.dp(26f)))

                            SecondaryButton(
                                paddingH = 34f,
                                paddingV = 20f,
                                text = "Login",
                                onClick = { }
                            )
                        }

                        Spacer(modifier = Modifier.height(scale.dp(82f)))

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Account ID: ${user?.id}",
                                color = White,
                                fontSize = scale.sp(20f),
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

                            Spacer(modifier = Modifier.height(scale.dp(8f)))

                            Text(
                                text = "GameVersion: 1.1.138",
                                color = White,
                                fontSize = scale.sp(20f),
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

                            Spacer(modifier = Modifier.height(scale.dp(8f)))

                            Text(
                                text = "Authentication: Google",
                                color = White,
                                fontSize = scale.sp(20f),
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
                        }

                        Spacer(modifier = Modifier.height(scale.dp(70f)))

                        SecondaryButton(
                            paddingH = 54f,
                            paddingV = 22f,
                            text = "Delete the account",
                            onClick = { },
                            modifier = Modifier
                        )

                        Spacer(modifier = Modifier.height(scale.dp(60f)))
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
                    modifier = Modifier.size(scale.dp(38f))
                )
            }
        }
    }
}