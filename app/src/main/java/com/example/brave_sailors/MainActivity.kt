package com.example.brave_sailors

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.brave_sailors.ui.theme.Brave_SailorsTheme
import com.example.brave_sailors.ui.utils.LockScreenOrientation
import com.example.brave_sailors.IntroScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.dark(
                android.graphics.Color.TRANSPARENT
            )
        )

        // Immersive Sticky Mode
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            Brave_SailorsTheme {
                LockScreenOrientation()

                // Mostra solo e sempre la schermata Intro.
                // La funzione onTimeout non farà nulla.
                IntroScreen()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Preview() {
    Brave_SailorsTheme {
        // La preview ora mostra Intro per coerenza.
        IntroScreen()
    }
}