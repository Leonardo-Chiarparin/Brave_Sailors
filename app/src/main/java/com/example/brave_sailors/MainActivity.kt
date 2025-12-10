package com.example.brave_sailors

import android.os.Bundle
// Rimuoviamo Toast perché non viene più usato qui
// import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
// Rimuoviamo LocalContext perché non viene più usato qui
// import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.brave_sailors.ui.theme.Brave_SailorsTheme
import com.example.brave_sailors.ui.utils.LockScreenOrientation
// AGGIUNGI: L'import per la nuova schermata, assicurati che il percorso sia corretto
import com.example.brave_sailors.TermsScreen

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

                // MODIFICA: Chiama TermsScreen senza parametri
                // La logica di navigazione o chiusura dell'app dovrà essere
                // gestita all'interno di TermsScreen stessa.
                TermsScreen()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Preview() {
    Brave_SailorsTheme {
        // MODIFICA: Aggiorna la preview per mostrare TermsScreen senza parametri.
        TermsScreen()
    }
}
