package com.example.brave_sailors.ui.utils

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
fun BackPress(onOverlayCloseLogic: () -> Boolean) {
    val context = LocalContext.current

    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    val doublePressInterval = 2000L // 2 seconds to confirm the operation

    BackHandler(enabled = true) {
        val overlayWasOpen = onOverlayCloseLogic()

        if (!overlayWasOpen) {
            val currentTime = System.currentTimeMillis()

            if (currentTime - lastBackPressTime < doublePressInterval) {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }

                context.startActivity(homeIntent)
            }
            else
                lastBackPressTime = currentTime
        }
    }
}