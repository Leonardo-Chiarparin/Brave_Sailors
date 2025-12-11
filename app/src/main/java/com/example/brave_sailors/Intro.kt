package com.example.brave_sailors

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.brave_sailors.ui.components.Footer
import com.example.brave_sailors.ui.components.GridBackground
import com.example.brave_sailors.ui.components.Radar
import com.example.brave_sailors.ui.components.Title
import com.example.brave_sailors.ui.theme.DarkGrey
import com.example.brave_sailors.ui.theme.DeepBlue
import com.example.brave_sailors.ui.theme.Grey
import com.example.brave_sailors.ui.theme.Orange
import com.example.brave_sailors.ui.utils.RememberScaleConversion

@Composable
fun IntroScreen(innerPadding: PaddingValues = PaddingValues(0.dp)) {
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
                .background(DeepBlue),
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

    val maxWidth = scale.dp(648f) // 648px, etc.

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        GridBackground(Modifier.matchParentSize(), color = DarkGrey,14f)

        Box(
            modifier = Modifier
                .widthIn(max = maxWidth),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // [ OPTIONAL ]: Try to insert an image conversely ( otherwise remove this box ). Its shape is reported below
                Box(
                    modifier = Modifier
                        .width(scale.dp(668f))
                        .height(scale.dp(472f))
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawOval(
                            color = Grey,
                            topLeft = Offset.Zero,
                            size = size,
                            style = Stroke(width = scale.dp(1f).toPx())
                        )
                    }
                }

                Spacer(modifier = Modifier.height(scale.dp(96f)))

                Title()

                Spacer(modifier = Modifier.height(scale.dp(242f)))

                Radar(
                    modifier = Modifier
                        .size(scale.dp(144f))
                )

                Spacer(modifier = Modifier.height(scale.dp(70f)))

                Text(
                    text = "Prepare to engage in strategic naval operations, where every decision shapes the tide of battle.",
                    color = Orange,
                    fontFamily = FontFamily.SansSerif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    fontSize = scale.sp(26f),
                    textAlign = TextAlign.Center,
                    lineHeight = scale.sp(36f),
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

                Spacer(modifier = Modifier.weight(1f))
            }

            Footer(modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}