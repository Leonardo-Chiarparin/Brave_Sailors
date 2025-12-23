package com.example.brave_sailors

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.brave_sailors.data.local.database.AppDatabase
import com.example.brave_sailors.model.ProfileViewModel
import com.example.brave_sailors.model.ProfileViewModelFactory
import com.example.brave_sailors.ui.theme.Brave_SailorsTheme
import com.example.brave_sailors.ui.utils.LockScreenOrientation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // [ TO - DO ]: Change the theme according to the current page ( if necessary )
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

        // Database
        val db = AppDatabase.getDatabase(this)
        val userDao = db.userDao()

        setContent {
            Brave_SailorsTheme {
                // [ TO - DO ]: User's preferences regarding the screen orientation should be managed through a proper variable, whose state will be changed according to the settings inside the correspondent page ( accessible via Menu.kt )
                // e.g., val orientation by settingsViewModel.isPortrait.collectAsState(initial = true)
                // [ NOTE ]: Landscape shapes of the pages will be implemented as soon as possible
                LockScreenOrientation(isPortrait = true)

                val navController = rememberNavController()

                val profileViewModel: ProfileViewModel = viewModel(
                    factory = ProfileViewModelFactory(userDao)
                )

                // State to determine start destination
                var startDestination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    val user = withContext(Dispatchers.IO) {
                        userDao.getCurrentUser()
                    }
                    if (user != null) {
                        profileViewModel.loadUser(user.id)
                        profileViewModel.showHomeWelcome = true
                        startDestination = "intro"
                    } else {
                        startDestination = "terms"
                    }
                }

                if (startDestination != null) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Transparent
                    ) { innerPadding ->

                        NavHost(navController = navController, startDestination = startDestination!!) {
                            composable("terms") {
                                TermsScreen(
                                    innerPadding = innerPadding,
                                    viewModel = profileViewModel,
                                    onStartApp = {
                                        navController.navigate("intro") {
                                            popUpTo("terms") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable("intro") {
                                IntroScreen(
                                    innerPadding = innerPadding,
                                    onFinished = {
                                        navController.navigate("home") {
                                            popUpTo("intro") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable("home") {
                                HomeScreen(
                                    innerPadding = innerPadding,
                                    viewModel = profileViewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
