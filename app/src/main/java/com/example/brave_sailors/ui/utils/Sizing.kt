package com.example.brave_sailors.ui.utils

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ScaleConverter(
    val scale: Float
) {
    fun dp(px: Float) = (px * scale).dp
    fun sp(px: Float) = (px * scale).sp
}

@SuppressLint("ComposableNaming", "ConfigurationScreenWidthHeight" )
@Composable
fun RememberScaleConversion(
    widthDp: Float = 720f,
    heightDp: Float = 1600f
): ScaleConverter {
    val configuration = LocalConfiguration.current

    val w = configuration.screenWidthDp.toFloat()
    val h = configuration.screenHeightDp.toFloat()

    val side = minOf(w, h)
    val referenceSide = if( w <= h ) widthDp else heightDp

    val scale = side / referenceSide

    return remember(configuration.screenWidthDp, configuration.screenHeightDp) {
        ScaleConverter(scale)
    }
}