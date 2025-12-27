package com.example.brave_sailors

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
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

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

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

                    NavHost(navController = navController, startDestination = "loading") {

                        composable("loading") {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                             }

                            LaunchedEffect(Unit) {
                                val user = withContext(Dispatchers.IO) {
                                    userDao.getCurrentUser()
                                }

                                if (user != null) {
                                    profileViewModel.loadUser(user.id)
                                    profileViewModel.showHomeWelcome = true
                                    navController.navigate("intro") {
                                        popUpTo("loading") { inclusive = true }
                                    }
                                } else {
                                    navController.navigate("terms") {
                                        popUpTo("loading") { inclusive = true }
                                    }
                                }
                            }
                        }

                        // 2. Terms Screen
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

                        // 3. Intro Screen
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
                                viewModel = profileViewModel,
                                onNavigateToTerms = {
                                    navController.navigate("terms") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}