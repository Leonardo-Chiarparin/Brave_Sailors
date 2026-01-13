package com.example.brave_sailors.ui.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.OrientationEventListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

@Composable
fun LockScreenOrientation(isPortrait: Boolean) {
    val context = LocalContext.current
    val currentState = rememberUpdatedState(isPortrait)

    LaunchedEffect(Unit) {
        val activity = context.findActivity()

        if (activity != null) {
            if (currentState.value) {
                if((activity.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) && (activity.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT))
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            // else { ... }
        }
    }

    DisposableEffect(Unit) {
        val activity = context.findActivity() ?: return@DisposableEffect onDispose {  }

        if (currentState.value && (activity.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT))
           activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        val handler = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN)
                    return

                val target = when {
                    // Portrait
                    currentState.value && orientation in 165..195 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT

                    // Reverse Portrait
                    currentState.value && orientation !in 16..344 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

                    else -> null
                }

                if (target != null && activity.requestedOrientation != target)
                    activity.requestedOrientation = target
            }
        }

        if (handler.canDetectOrientation())
            handler.enable()

        onDispose {
            handler.disable()
        }
    }
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}