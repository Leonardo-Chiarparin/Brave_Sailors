package com.example.brave_sailors.ui.utils

import android.util.DisplayMetrics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// -- SCALING UTILS --
data class ScaleConverter(
    val scale: Float
) {
    fun dp(px: Float) = (px * scale).dp
    fun sp(px: Float) = (px * scale).sp
}

@Composable
fun RememberScaleConversion(
    widthDp: Float = 720f,
    heightDp: Float = 1600f
): ScaleConverter {
    val configuration = LocalConfiguration.current

    val w = configuration.screenWidthDp.toFloat()
    val h = configuration.screenHeightDp.toFloat()

    // [ TO - DO ]: Evaluate the possibility to allow portrait orientation only
    val side = minOf(w, h)
    val referenceSide = if( w <= h ) widthDp else heightDp

    val scale = side / referenceSide

    return remember(configuration.screenWidthDp, configuration.screenHeightDp) {
        ScaleConverter(scale)
    }
}