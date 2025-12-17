package com.example.brave_sailors.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import com.example.brave_sailors.ui.theme.LightGrey
import com.example.brave_sailors.ui.theme.White
import com.example.brave_sailors.ui.utils.RememberScaleConversion

@Composable
fun Footer(modifier: Modifier = Modifier) {
    val scale = RememberScaleConversion()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = scale.dp(14f)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "© 2025 Ettore Cantile, Leonardo Chiarparin ",
            color = White,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = scale.sp(24f),
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

        Spacer(modifier = Modifier.height(scale.dp(20f)))

        // [ TO - DO ]: The following component must be modified according to the ( initial ) connection's state
        // The order of these sentences is:
        // 0) . initialize social gaming network ( ? )
        // 1) . connect to server
        // 2) . retrieve data from server
        // 3) . start up audio ( if we decide to implement such functionality )
        // 4) . check consent and permissions
        // 5) . starting
        Text(
            ". starting",
            color = LightGrey,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = scale.sp(16f),
            fontStyle = FontStyle.Italic,
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
            ),
            modifier = Modifier
                .align(Alignment.Start)
        )
    }
}