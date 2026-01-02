package com.example.brave_sailors.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.brave_sailors.ui.theme.Orange
import com.example.brave_sailors.ui.utils.RememberScaleConversion

@Composable
fun DividerOrange() {
    val scale = RememberScaleConversion()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(scale.dp(2f))
            .background(Orange)
    )
}