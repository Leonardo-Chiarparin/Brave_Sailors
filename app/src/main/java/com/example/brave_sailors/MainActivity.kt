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
import com.example.brave_sailors.model.*
import com.example.brave_sailors.model.ProfileViewModelFactory
import com.example.brave_sailors.model.ProfileViewModel
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


                val profileViewModel: ProfileViewModel = viewModel(
                    factory = ProfileViewModelFactory(userDao)
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent
                ) { innerPadding ->

                    NavHost(navController = navController, startDestination = "terms") {


                        composable("terms") {
                            TermsScreen(
                                innerPadding = innerPadding,
                                onStartApp = {

                                    val account = GoogleSignIn.getLastSignedInAccount(this@MainActivity)
                                    val userId = account?.id ?: "guest_id"


                                    profileViewModel.loadUser(userId)


                                    navController.navigate("profile") {
                                        popUpTo("terms") { inclusive = true }
                                    }
                                }
                            )
                        }

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