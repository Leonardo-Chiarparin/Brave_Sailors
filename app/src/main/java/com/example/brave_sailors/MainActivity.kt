package com.example.brave_sailors

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.brave_sailors.data.local.database.AppDatabase
import com.example.brave_sailors.ProfileScreen
import com.example.brave_sailors.TermsScreen
import com.example.brave_sailors.ui.model.ProfileViewModel
import com.example.brave_sailors.ui.model.ProfileViewModelFactory
import com.example.brave_sailors.ui.theme.Brave_SailorsTheme
import com.example.brave_sailors.ui.utils.LockScreenOrientation
import com.google.android.gms.auth.api.signin.GoogleSignIn

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        // Database
        val db = AppDatabase.getDatabase(this)
        val userDao = db.userDao()

        setContent {
            Brave_SailorsTheme {
                LockScreenOrientation(isPortrait = true)

                val navController = rememberNavController()

                // ViewModel condiviso o inizializzato qui
                val profileViewModel: ProfileViewModel = viewModel(
                    factory = ProfileViewModelFactory(userDao)
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent
                ) { innerPadding ->

                    NavHost(navController = navController, startDestination = "terms") {

                        // SCHERMATA 1: Termini e Login
                        composable("terms") {
                            TermsScreen(
                                innerPadding = innerPadding,
                                onStartApp = { // Al click di START:
                                    // 1. Recupera l'ID Google
                                    val account = GoogleSignIn.getLastSignedInAccount(this@MainActivity)
                                    val userId = account?.id ?: "guest_id"

                                    // 2. Carica i dati nel ViewModel (così il profilo è pronto)
                                    profileViewModel.loadUser(userId)

                                    // 3. Naviga al Profilo
                                    navController.navigate("profile") {
                                        popUpTo("terms") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // SCHERMATA 2: Profilo
                        composable("profile") {
                            ProfileScreen(
                                paddingValues = innerPadding,
                                viewModel = profileViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}