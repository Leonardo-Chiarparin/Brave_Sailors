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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.dark(
                android.graphics.Color.TRANSPARENT
            )
        )

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        val db = AppDatabase.getDatabase(this)
        val userDao = db.userDao()
        val fleetDao = db.fleetDao()
        val friendDao = db.friendDao()
        val matchDao = db.matchDao()

        val apiService = RetrofitClient.api
        val userRepository = UserRepository(apiService, userDao, fleetDao, friendDao, matchDao)

        setContent {
            Brave_SailorsTheme {
                LockScreenOrientation(isPortrait = true)

                val context = LocalContext.current

                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val profileViewModel: ProfileViewModel = viewModel(
                    factory = ProfileViewModelFactory(userDao, fleetDao, userRepository)
                )

                val lifecycleOwner = LocalLifecycleOwner.current
                var lastBackgroundTime by remember { mutableLongStateOf(0L) }
                val timeoutDuration = 60 * 1000L

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
                                    val shouldRestart = currentRoute != null && currentRoute != "terms" && currentRoute != "loading"

                                    if (shouldRestart) {
                                        navController.navigate("loading") {
                                            popUpTo(navController.graph.startDestinationId) {
                                                inclusive = false
                                            }

                                            launchSingleTop = true
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
                                val pendingMatch = MatchStateStorage.getState(context)

                                if (pendingMatch != null)
                                    profileViewModel.handleTimeoutForfeit(context)


                                val userExists = withContext(Dispatchers.IO) {
                                    userDao.getCurrentUser() != null
                                }

                                if (userExists) {
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
                                viewModel = profileViewModel,
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