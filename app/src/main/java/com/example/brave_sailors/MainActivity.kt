package com.example.brave_sailors

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.brave_sailors.data.local.database.AppDatabase
import com.example.brave_sailors.data.remote.api.RetrofitClient
import com.example.brave_sailors.data.repository.UserRepository
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
        val fleetDao = db.fleetDao()
        val apiService = RetrofitClient.api
        val userRepository = UserRepository(apiService, userDao, fleetDao)

        setContent {
            Brave_SailorsTheme {
                // [ TO - DO ]: User's preferences regarding the screen orientation should be managed through a proper variable, whose state will be changed according to the settings inside the correspondent page ( accessible via Menu.kt )
                // e.g., val orientation by settingsViewModel.isPortrait.collectAsState(initial = true)
                // [ NOTE ]: Landscape shapes of the pages will be implemented as soon as possible
                LockScreenOrientation(isPortrait = true)

                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val profileViewModel: ProfileViewModel = viewModel(
                    factory = ProfileViewModelFactory(userDao, fleetDao, userRepository)
                )

                // --- GLOBAL APP TIMEOUT LOGIC (1 MINUTE) ---
                val lifecycleOwner = LocalLifecycleOwner.current
                var lastBackgroundTime by remember { mutableStateOf(0L) }
                val timeoutDuration = 60 * 1000L // 1 minute

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_STOP -> {
                                lastBackgroundTime = System.currentTimeMillis()
                            }

                            Lifecycle.Event.ON_RESUME -> {
                                val currentTime = System.currentTimeMillis()
                                val elapsedTime = currentTime - lastBackgroundTime

                                if (lastBackgroundTime != 0L && elapsedTime >= timeoutDuration) {
                                    // RESTART LOGIC:
                                    // Trigger if NOT on "terms" or "loading".
                                    // We allow trigger even if already on "intro" to restart the loading animation/data fetch.
                                    val shouldRestart = currentRoute != "terms" && currentRoute != "loading"

                                    if (shouldRestart) {
                                        navController.navigate("loading") {
                                            // Pop up to the very first route to reset the app state properly
                                            popUpTo(navController.graph.startDestinationId) {
                                                inclusive = false
                                            }
                                            launchSingleTop = true // Avoid multiple instances of "loading"
                                        }
                                    }
                                }

                                lastBackgroundTime = 0L
                            }

                            else -> {}
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                // State used to determine the startDestination
                var startDestination by remember { mutableStateOf<String?>(null) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent
                ) { innerPadding ->

                    NavHost(
                        navController = navController,
                        startDestination = "loading",
                        enterTransition = { EnterTransition.None },
                        exitTransition = { ExitTransition.None },
                        popEnterTransition = { EnterTransition.None },
                        popExitTransition = { ExitTransition.None }
                    ) {
                        composable("loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {  }

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

                        // 2. Terms
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

                        // 3. Intro
                        composable("intro") {
                            IntroScreen(
                                innerPadding = innerPadding,
                                viewModel = profileViewModel,
                                onFinished = {
                                    navController.navigate("home") {
                                        popUpTo("intro") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 4. Home
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

/*
    @Preview(showBackground = true)
    @Composable
    fun Preview() {
        Brave_SailorsTheme {
            IntroScreen()
        }
    }
*/
