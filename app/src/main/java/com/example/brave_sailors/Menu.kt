package com.example.brave_sailors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import com.example.brave_sailors.ui.components.GridBackground
import com.example.brave_sailors.ui.components.QuaternaryButton
import com.example.brave_sailors.ui.theme.DarkBlue
import com.example.brave_sailors.ui.theme.LightBlue
import com.example.brave_sailors.ui.theme.Orange
import com.example.brave_sailors.ui.utils.RememberScaleConversion

@Composable
fun MenuScreen() {
    Modal()
}

@Composable
private fun Modal() {
    // -- SCALE ( used for applying conversions ) --
    // [ MEMO ]: Sizes are taken from 720 x 1600px mockup ( with 72dpi ) using the Redmi Note 10S
    val scale = RememberScaleConversion()

    val maxWidth = scale.dp(720f)
    val strokeDp = scale.dp(1f)

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
                }
                .padding(horizontal = scale.dp(16f), vertical = scale.dp(4f)),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                GridBackground(Modifier.matchParentSize(), color = LightBlue, 14f)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = scale.dp(132f), top = scale.dp(220f)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(scale.dp(20f))
                ) {
                    QuaternaryButton(
                        text = "Game\noptions",
                        icon = Icons.Default.VideogameAsset,
                        onClick = {  }
                    )

                    QuaternaryButton(
                        text = "Account\nsettings",
                        icon = Icons.Default.Settings,
                        onClick = {  }
                    )

                    QuaternaryButton(
                        text = "Instructions",
                        icon = Icons.Default.QuestionMark,
                        onClick = {  }
                    )
                }
            }
        }
    }
}