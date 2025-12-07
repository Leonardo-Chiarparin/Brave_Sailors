package com.example.brave_sailors.ui.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

@SuppressLint("SourceLockedOrientationActivity")
@Composable
fun LockScreenOrientation() {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation

        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT

        onDispose {
            originalOrientation?.let { activity.requestedOrientation = it }
        }
    }
}