package com.example.brave_sailors

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.brave_sailors.ui.components.GridBackground
import com.example.brave_sailors.ui.components.PrimaryButton
import com.example.brave_sailors.ui.components.SecondaryButton
import com.example.brave_sailors.ui.components.Tab
import com.example.brave_sailors.ui.theme.Blue
import com.example.brave_sailors.ui.theme.DarkBlue
import com.example.brave_sailors.ui.theme.LightBlue
import com.example.brave_sailors.ui.theme.Orange
import com.example.brave_sailors.ui.theme.White
import com.example.brave_sailors.ui.utils.RememberScaleConversion

@Composable
fun TermsScreen(innerPadding: PaddingValues = PaddingValues(0.dp)) {
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
                .background(Blue),
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

    // [ MEMO ]: Sizes are taken from 720 x 1600px mockup ( with 72dpi ) using the Redmi Note 10S
    val boxShape = CutCornerShape(scale.dp(28f)) // 28px

    val maxWidth = scale.dp(648f) // 648px, etc.

    Box(
        modifier = Modifier
            .widthIn(max = maxWidth),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                // borderStroke + ( HeaderTab's height / 2 ) = 2 + ( ( 32 + 32 + 32 ) / 2 ), also taking into account the size of its content
                .padding(top = scale.dp(50f))
                .fillMaxWidth()
                .background(DarkBlue, shape = boxShape)
                .border(BorderStroke(scale.dp(1f), Orange), shape = boxShape)
                .clip(boxShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .padding(all = scale.dp(28f))
                    .clip(RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                GridBackground(Modifier.matchParentSize(), color = LightBlue, 14f)

                Box(
                    modifier = Modifier
                        .padding(horizontal = scale.dp(48f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(scale.dp(146f)))

                        Text(
                            text = "To use this service, users must review the following terms. Minors are requested to obtain permission from their parents or guardians.",
                            color = White,
                            textAlign = TextAlign.Center,
                            fontSize = scale.sp(26f),
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            lineHeight = scale.sp(32f),
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

                        Spacer(modifier = Modifier.height(scale.dp(114f)))

                        SecondaryButton(
                            text = "Privacy Policy",
                            onClick = {},
                            modifier = Modifier
                        )

                        Spacer(modifier = Modifier.height(scale.dp(82f)))

                        Text(
                            text = "By proceeding, the customer formally attests to their comprehension of such conditions.",
                            color = White,
                            textAlign = TextAlign.Center,
                            fontSize = scale.sp(20f),
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            lineHeight = scale.sp(24f),
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

                        Spacer(modifier = Modifier.height(scale.dp(70f)))

                        PrimaryButton(text = "START", onClick = {})

                        Spacer(modifier = Modifier.height(scale.dp(74f)))
                    }
                }
            }
        }

        Box(modifier = Modifier.zIndex(2f)) {
            Tab(text = "Consensus")
        }
    }
}