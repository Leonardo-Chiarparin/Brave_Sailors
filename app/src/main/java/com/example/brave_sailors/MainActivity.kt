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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.example.brave_sailors.data.local.MatchStateStorage
import com.example.brave_sailors.data.local.database.AppDatabase
import com.example.brave_sailors.data.remote.api.RetrofitClient
import com.example.brave_sailors.data.repository.UserRepository
import com.example.brave_sailors.model.ProfileViewModel
import com.example.brave_sailors.model.ProfileViewModelFactory
import com.example.brave_sailors.ui.theme.Brave_SailorsTheme
import com.example.brave_sailors.ui.utils.LockScreenOrientation
import com.example.brave_sailors.ui.utils.restartApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Activate Edge-To-Edge layout with transparent system bars
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.dark(
                android.graphics.Color.TRANSPARENT
            )
        )

        // Immersive Sticky Mode ( system bars are hidden, shown temporarily on swipe )
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        // -- DATABASE & REPOSITORY SETUP --
        val db = AppDatabase.getDatabase(this)
        val userDao = db.userDao()
        val fleetDao = db.fleetDao()
        val friendDao = db.friendDao()
        val matchDao = db.matchDao()

        val apiService = RetrofitClient.api
        val userRepository = UserRepository(apiService, userDao, fleetDao, friendDao, matchDao)

        setContent {
            Brave_SailorsTheme {
                // [ NOTE ]: Force portrait orientation for the entire application
                LockScreenOrientation(isPortrait = true)

                val context = LocalContext.current

                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // -- PROFILE'S VIEW MODEL ( an entity being shared across multiple screens ) --
                val profileViewModel: ProfileViewModel = viewModel(
                    factory = ProfileViewModelFactory(userDao, fleetDao, userRepository)
                )

                // -- GLOBAL TIMEOUT LOGIC ( 1 minute ) --
                // e.g., if the program stays in the background ( suspended ) for more than the previous duration, it will be restarted from the initial page ( except for specific routes )
                val lifecycleOwner = LocalLifecycleOwner.current
                var lastBackgroundTime by remember { mutableLongStateOf(0L) }
                val timeoutDuration = 60 * 1000L // 1 minute

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_STOP -> {
                                // Save the timestamp whenever the application goes to background
                                lastBackgroundTime = System.currentTimeMillis()
                            }

                            Lifecycle.Event.ON_RESUME -> {
                                // The program comes back to foreground: the elapsed time is checked
                                val currentTime = System.currentTimeMillis()
                                val elapsedTime = currentTime - lastBackgroundTime

                                if (lastBackgroundTime != 0L && elapsedTime >= timeoutDuration) {
                                    // RESTART LOGIC:
                                    // Trigger if NOT on "terms" or "loading".
                                    // We allow trigger even if already on "intro" to restart the loading animation/data fetch.
                                    val shouldRestart = currentRoute != null && currentRoute != "terms" && currentRoute != "loading"

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

                                // Reset the timestamp
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
                        // -- LOADING SCREEN --
                        composable("loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {  }

                            LaunchedEffect(Unit) {
                                val pendingMatch = MatchStateStorage.getState(context)

                                if (pendingMatch != null)
                                    profileViewModel.handleTimeoutForfeit(context)


                                // One-time DB control to decide the starting path
                                val userExists = withContext(Dispatchers.IO) {
                                    // [ NOTE ]: getCurrentUser must return User? ( not Flow )
                                    userDao.getCurrentUser() != null
                                }

                                if (userExists) {
                                    // profileViewModel observes the DB automatically, no manual loading is actually needed
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

                        // -- TERMS SCREEN --
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

                        // -- INTRO SCREEN --
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

                        // -- HOME SCREEN --
                        composable("home") {
                            HomeScreen(
                                innerPadding = innerPadding,
                                viewModel = profileViewModel,
                                onRestart = {
                                    restartApp(context)
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
