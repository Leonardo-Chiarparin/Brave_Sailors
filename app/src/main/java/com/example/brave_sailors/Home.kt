package com.example.brave_sailors

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.example.brave_sailors.data.local.MatchStateStorage
import com.example.brave_sailors.data.local.database.AppDatabase
import com.example.brave_sailors.data.remote.api.RetrofitClient
import com.example.brave_sailors.data.repository.UserRepository
import com.example.brave_sailors.model.ProfileViewModel
import com.example.brave_sailors.model.ProfileViewModelFactory
import com.example.brave_sailors.model.RegisterUiState
import com.example.brave_sailors.model.RegisterViewModel
import com.example.brave_sailors.model.RegisterViewModelFactory
import com.example.brave_sailors.ui.challenge.BilgeScreen
import com.example.brave_sailors.ui.components.ActiveDialog
import com.example.brave_sailors.ui.components.Arrow
import com.example.brave_sailors.ui.components.DialogAccess
import com.example.brave_sailors.ui.components.DialogDeleteAccount
import com.example.brave_sailors.ui.components.DialogError
import com.example.brave_sailors.ui.components.DialogFilter
import com.example.brave_sailors.ui.components.DialogFlag
import com.example.brave_sailors.ui.components.DialogFleetConfirm
import com.example.brave_sailors.ui.components.DialogFriend
import com.example.brave_sailors.ui.components.DialogInstructions
import com.example.brave_sailors.ui.components.DialogLoading
import com.example.brave_sailors.ui.components.DialogName
import com.example.brave_sailors.ui.components.DialogPassword
import com.example.brave_sailors.ui.components.DialogRegister
import com.example.brave_sailors.ui.components.DialogSource
import com.example.brave_sailors.ui.components.MosaicBackground
import com.example.brave_sailors.ui.components.NavigationBar
import com.example.brave_sailors.ui.components.NavigationConstants.TOTAL_DURATION
import com.example.brave_sailors.ui.components.NavigationItem
import com.example.brave_sailors.ui.components.Popup
import com.example.brave_sailors.ui.components.TertiaryButton
import com.example.brave_sailors.ui.theme.LightGrey
import com.example.brave_sailors.ui.theme.White
import com.example.brave_sailors.ui.utils.BackPress
import com.example.brave_sailors.ui.utils.GameModeIcons
import com.example.brave_sailors.ui.utils.RememberScaleConversion
import com.example.brave_sailors.ui.utils.gameModes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class OverlayHomeState {
    IDLE,
    EXITING_HOME,
    SHOWING_DETAILS,
    EXITING_DETAILS,
    SHOWING_LOBBY,
    LOADING_MATCH,
    SHOWING_GUEST_FLEET,
    EXITING_GUEST_FLEET,
    PLAYING_VS_COMPUTER,
    PLAYING_VS_GUEST,
    PLAYING_VS_FRIEND
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomeScreen(innerPadding: PaddingValues = PaddingValues(0.dp), viewModel: ProfileViewModel, onRestart: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope() // Scope to launch clearCredentialState
    
    var currentScreen by remember { mutableStateOf(NavigationItem.Home) }

    var isContentVisible by remember { mutableStateOf(false) }

    // -- DIALOG STATE --
    var activeDialog by remember { mutableStateOf<ActiveDialog>(ActiveDialog.None) }

    // -- NAVIGATION STATE --
    var overlayChallengeState by remember { mutableStateOf(OverlayChallengeState.IDLE) }
    var targetChallengeState by remember { mutableStateOf(OverlayChallengeState.IDLE) }

    var overlayProfileState by remember { mutableStateOf(OverlayProfileState.IDLE) }
    var targetProfileState by remember { mutableStateOf(OverlayProfileState.IDLE) }

    var overlayMenuState by remember { mutableStateOf(OverlayMenuState.IDLE) }
    var targetMenuState by remember { mutableStateOf(OverlayMenuState.IDLE) }

    var overlayHomeState by remember { mutableStateOf(OverlayHomeState.IDLE) }
    var targetHomeState by remember { mutableStateOf(OverlayHomeState.IDLE) }

    // -- MATCH SETTINGS --
    var chosenGameModeIndex by remember { mutableIntStateOf(0) }
    var chosenDifficulty: String? by remember { mutableStateOf("Normal") }
    var chosenFiringRule by remember { mutableStateOf("Chain attacks") }

    // [ LOGIC ]: State to hold the chosen opponent for the online match
    var selectedOpponent by remember { mutableStateOf<LobbyPlayer?>(null) }
    var activeMatchId by remember { mutableStateOf<String?>(null) }

    // --- DEPENDENCIES & VIEW MODELS ---
    val db = remember { AppDatabase.getDatabase(context) }
    val api = RetrofitClient.api

    val repository = remember { UserRepository(api, db.userDao(), db.fleetDao(), db.friendDao(), db.matchDao()) }

    val registerViewModel: RegisterViewModel = viewModel(factory = RegisterViewModelFactory(repository))
    val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(db.userDao(), db.fleetDao(), repository))

    // -- USER DATA --
    val uiStateAS = registerViewModel.uiState

    val scale = RememberScaleConversion()

    var showGooglePopup by remember { mutableStateOf(false) }

    // -- USER AND FLAGS --
    val flagList = viewModel.flagList.collectAsState()
    val userState = viewModel.userState.collectAsState()

    val availableFlags = flagList.value
    val entry = userState.value

    val hasLinkedEmail = !entry?.registerEmail.isNullOrEmpty()
    val hasLinkedPassword = !entry?.password.isNullOrEmpty()

    val forceLogout by viewModel.forceLogoutEvent.collectAsState()

    BackPress {
        // It returns true if an overlay is present ( consuming the event and going back to the previous screen ), conversely the app will be minimized ( suspended in background )
        if (activeDialog != ActiveDialog.None) {
            when (val dialog = activeDialog) {
                is ActiveDialog.Register,
                is ActiveDialog.Access -> {
                    registerViewModel.resetState()
                    activeDialog = ActiveDialog.None
                }
                is ActiveDialog.Error -> {
                    activeDialog = when (dialog.source) {
                        DialogSource.REGISTER -> { registerViewModel.resetState(); ActiveDialog.Register }
                        DialogSource.ACCESS -> { registerViewModel.resetState(); ActiveDialog.Access }
                        DialogSource.PASSWORD -> ActiveDialog.None
                        DialogSource.DELETE -> ActiveDialog.None
                        DialogSource.FRIEND -> ActiveDialog.Friend
                        else -> ActiveDialog.None
                    }
                }
                else -> {
                    activeDialog = ActiveDialog.None
                }
            }

            true
        }
        else {
            when {
                overlayChallengeState != OverlayChallengeState.IDLE -> {
                    overlayChallengeState = OverlayChallengeState.IDLE
                    true
                }

                overlayMenuState != OverlayMenuState.IDLE -> {
                    overlayMenuState = OverlayMenuState.IDLE
                    true
                }

                overlayProfileState != OverlayProfileState.IDLE -> {
                    overlayProfileState = OverlayProfileState.IDLE
                    true
                }

                overlayHomeState != OverlayHomeState.IDLE -> {
                    when (overlayHomeState) {
                        OverlayHomeState.SHOWING_LOBBY, OverlayHomeState.SHOWING_GUEST_FLEET -> {
                            overlayHomeState = OverlayHomeState.SHOWING_DETAILS
                            true
                        }

                        OverlayHomeState.SHOWING_DETAILS -> {
                            overlayHomeState = OverlayHomeState.IDLE
                            true
                        }

                        OverlayHomeState.PLAYING_VS_COMPUTER, OverlayHomeState.PLAYING_VS_GUEST, OverlayHomeState.PLAYING_VS_FRIEND -> {
                            false
                        }

                        else -> {
                            overlayHomeState = OverlayHomeState.IDLE
                            true
                        }
                    }
                }

                else -> false
            }
        }
    }

    LaunchedEffect(viewModel.showHomeWelcome, entry) {
        if (viewModel.showHomeWelcome && entry != null) {
            launch(Dispatchers.IO) {

                entry.googlePhotoUrl?.let { url ->
                    if (url.isNotEmpty()) {
                        val request = ImageRequest.Builder(context)
                            .data(url)
                            .build()
                        context.imageLoader.execute(request)
                    }
                }

                showGooglePopup = true
            }
        }
    }

    LaunchedEffect(currentScreen) {
        isContentVisible = false
        delay(TOTAL_DURATION.toLong())
        isContentVisible = true
    }

    // Security check: perform logout if session expires or conflicts are detected
    if (forceLogout) {
        LaunchedEffect(Unit) {
            viewModel.removeSessionListener()

            MatchStateStorage.clear(context)

            try {
                val credentialManager = CredentialManager.create(context)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                e.printStackTrace()
            }

            repository.clearAllLocalData()

            onRestart()
        }
    }

    LaunchedEffect(uiStateAS) {
        when (uiStateAS) {
            is RegisterUiState.Success -> {
                registerViewModel.resetState()

                if (activeDialog is ActiveDialog.Register)
                    activeDialog = ActiveDialog.None

                if (activeDialog is ActiveDialog.Access) {
                    activeDialog = ActiveDialog.None
                    onRestart()
                }
            }

            is RegisterUiState.Error -> {
                val source = when (activeDialog) {
                    is ActiveDialog.Register -> DialogSource.REGISTER
                    is ActiveDialog.Access -> DialogSource.ACCESS
                    is ActiveDialog.Password -> DialogSource.PASSWORD
                    is ActiveDialog.DeleteAccount -> DialogSource.DELETE
                    is ActiveDialog.Friend -> DialogSource.FRIEND
                    else -> DialogSource.NONE
                }

                activeDialog = ActiveDialog.Error(uiStateAS.message, source)
            }

            else -> {  }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        MosaicBackground()

        Scaffold(
            modifier = Modifier
                .fillMaxSize(),
            containerColor = Color.Transparent,
            bottomBar = {
                val isPlayingMiniGame = overlayChallengeState != OverlayChallengeState.IDLE

                AnimatedVisibility(
                    visible = overlayHomeState == OverlayHomeState.IDLE && !isPlayingMiniGame,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(durationMillis = 300)
                    ) + fadeIn(animationSpec = tween(300)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(durationMillis = 200)
                    ) + fadeOut(animationSpec = tween(200))
                ) {
                    NavigationBar(
                        currentScreen = currentScreen,
                        onItemClick = { newItem ->
                            if (currentScreen != newItem) {
                                currentScreen = newItem
                                isContentVisible = false
                                overlayProfileState = OverlayProfileState.IDLE
                                overlayMenuState = OverlayMenuState.IDLE
                                overlayHomeState = OverlayHomeState.IDLE
                                overlayChallengeState = OverlayChallengeState.IDLE
                            }
                        }
                    )
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                when (currentScreen) {
                    NavigationItem.Home -> {
                        LaunchedEffect(overlayHomeState) { 
                            if (overlayHomeState == OverlayHomeState.EXITING_HOME || overlayHomeState == OverlayHomeState.EXITING_DETAILS || overlayHomeState == OverlayHomeState.EXITING_GUEST_FLEET ) {
                                delay(200)
                                overlayHomeState = targetHomeState
                            }
                        }

                        AnimatedVisibility(
                            visible = overlayHomeState == OverlayHomeState.IDLE,
                            enter = fadeIn(animationSpec = tween(300)),
                            exit = fadeOut(animationSpec = tween(200))
                        ) {
                            Modal(
                                onOpenGameModes = { index ->
                                    chosenGameModeIndex = index
                                    overlayHomeState = OverlayHomeState.EXITING_HOME
                                    targetHomeState = OverlayHomeState.SHOWING_DETAILS
                                }
                            )
                        }

                        // -- GAME MODE DETAILS --
                        AnimatedVisibility(
                            visible = overlayHomeState == OverlayHomeState.SHOWING_DETAILS,
                            enter = fadeIn(animationSpec = tween(300)),
                            exit = fadeOut(animationSpec = tween(200))
                        ) {
                            GameModeDetailsScreen(
                                db = db,
                                gameModeIndex = chosenGameModeIndex,
                                onBack = { overlayHomeState = OverlayHomeState.IDLE },
                                onContinue = { difficulty, rule ->
                                    chosenDifficulty = difficulty
                                    chosenFiringRule = rule

                                    overlayHomeState = OverlayHomeState.EXITING_DETAILS
                                    targetHomeState = when (chosenGameModeIndex) {
                                        0 -> {
                                            OverlayHomeState.PLAYING_VS_COMPUTER
                                        }
                                        1 -> {
                                            OverlayHomeState.SHOWING_GUEST_FLEET
                                        }
                                        else -> {
                                            OverlayHomeState.SHOWING_LOBBY
                                        }
                                    }

                                }
                            )
                        }

                        // -- GUEST FLEET DEPLOYMENT --
                        AnimatedVisibility(
                            visible = overlayHomeState == OverlayHomeState.SHOWING_GUEST_FLEET,
                            enter = fadeIn(animationSpec = tween(300)),
                            exit = fadeOut(animationSpec = tween(200))
                        ) {
                            FleetGuestScreen(
                                firingRule = chosenFiringRule,
                                onBack = {
                                    overlayHomeState = OverlayHomeState.SHOWING_DETAILS
                                },
                                onStartMatch = { rule ->
                                    chosenFiringRule = rule

                                    overlayHomeState = OverlayHomeState.EXITING_GUEST_FLEET
                                    targetHomeState = OverlayHomeState.PLAYING_VS_GUEST
                                }
                            )
                        }

                        // -- LOBBY --
                        AnimatedVisibility(
                            visible = overlayHomeState == OverlayHomeState.SHOWING_LOBBY || overlayHomeState == OverlayHomeState.LOADING_MATCH,
                            enter = fadeIn(animationSpec = tween(300)),
                            exit = fadeOut(animationSpec = tween(200))
                        ) {
                            LaunchedEffect(Unit) {
                                selectedOpponent = null
                                activeMatchId = null
                            }

                            GameLobbyScreen(
                                db = db,
                                availableFlags = availableFlags,
                                selectedFiringRule = chosenFiringRule,
                                onBack = {
                                    overlayHomeState = OverlayHomeState.SHOWING_DETAILS
                                },
                                onMatchStart = { opponent, matchId ->
                                    selectedOpponent = opponent
                                    activeMatchId = matchId

                                    overlayHomeState = OverlayHomeState.PLAYING_VS_FRIEND
                                }
                            )
                        }

                        val context = LocalContext.current
                        val db = remember { AppDatabase.getDatabase(context) }

                        // -- PLAYING VS COMPUTER --
                        AnimatedVisibility(
                            visible = overlayHomeState == OverlayHomeState.PLAYING_VS_COMPUTER,
                            enter = fadeIn(animationSpec = tween(300)),
                            exit = fadeOut(animationSpec = tween(200))
                        ) {
                            MatchVsComputerScreen(
                                db = db,
                                profileViewModel = profileViewModel,
                                difficulty = chosenDifficulty ?: "Normal",
                                firingRule = chosenFiringRule,
                                user = entry,
                                flag = availableFlags.find { it.code == entry?.countryCode },
                                onRetire = {
                                    overlayHomeState = OverlayHomeState.IDLE
                                },
                                onComplete = {
                                    overlayHomeState = OverlayHomeState.IDLE
                                }
                            )
                        }

                        // -- PLAYING VS GUEST --
                        AnimatedVisibility(
                            visible = overlayHomeState == OverlayHomeState.PLAYING_VS_GUEST,
                            enter = fadeIn(animationSpec = tween(300)),
                            exit = fadeOut(animationSpec = tween(200))
                        ) {
                            MatchVsGuestScreen(
                                profileViewModel = profileViewModel,
                                firingRule = chosenFiringRule,
                                user = entry,
                                flag = availableFlags.find { it.code == entry?.countryCode },
                                onRetire = {
                                    overlayHomeState = OverlayHomeState.IDLE
                                },
                                onComplete = {
                                    overlayHomeState = OverlayHomeState.IDLE
                                }
                            )
                        }

                        // -- PLAYING VS FRIEND ( ONLINE ) --
                        AnimatedVisibility(
                            visible = overlayHomeState == OverlayHomeState.PLAYING_VS_FRIEND && selectedOpponent != null,
                            enter = fadeIn(animationSpec = tween(300)),
                            exit = fadeOut(animationSpec = tween(200))
                        ) {
                            selectedOpponent?.let { currentOpponent ->
                                activeMatchId?.let { matchId ->
                                    MatchVsFriendScreen(
                                        db = db,
                                        profileViewModel = profileViewModel,
                                        opponent = currentOpponent,
                                        firingRule = chosenFiringRule,
                                        matchId = matchId,
                                        availableFlags = availableFlags,
                                        onHome = {
                                            overlayHomeState = OverlayHomeState.IDLE
                                        }
                                    )
                                }
                            }
                        }

                    }
                    NavigationItem.Profile -> {
                        LaunchedEffect(overlayProfileState) {
                            if (overlayProfileState == OverlayProfileState.EXITING_PROFILE) {
                                delay(200)
                                overlayProfileState = targetProfileState
                            }
                        }

                        ProfileScreen(
                            isVisible = isContentVisible && (overlayProfileState == OverlayProfileState.IDLE),
                            user = entry,
                            availableFlags = availableFlags,
                            viewModel = viewModel,
                            onGetPhoto = { uri ->
                                activeDialog = ActiveDialog.Filter(uri)
                            },
                            onOpenChangeFlag = { activeDialog = ActiveDialog.Flag },
                            onOpenChangeName = { activeDialog = ActiveDialog.Name },
                            onOpenStatistics = {
                                overlayProfileState = OverlayProfileState.EXITING_PROFILE
                                targetProfileState = OverlayProfileState.SHOWING_STATS
                            },
                            onOpenRankings = {
                                overlayProfileState = OverlayProfileState.EXITING_PROFILE
                                targetProfileState = OverlayProfileState.SHOWING_RANKINGS
                            },
                            onOpenFriends = { activeDialog = ActiveDialog.Friend }
                        )

                        // -- STATISTICS --
                        if (overlayProfileState == OverlayProfileState.SHOWING_STATS) {
                            StatisticsScreen(
                                user = entry,
                                onBack = { overlayProfileState = OverlayProfileState.IDLE }
                            )
                        }

                        // -- RANKINGS --
                        if (overlayProfileState == OverlayProfileState.SHOWING_RANKINGS && entry != null) {
                            RankingsScreen(
                                user = entry,
                                availableFlags,
                                viewModel = viewModel,
                                onBack = { overlayProfileState = OverlayProfileState.IDLE }
                            )
                        }

                    }
                    NavigationItem.Menu -> {
                        LaunchedEffect(overlayMenuState) {
                            if (overlayMenuState == OverlayMenuState.EXITING_MENU) {
                                delay(200)
                                overlayMenuState = targetMenuState
                            }
                        }

                        AnimatedVisibility(
                            visible = overlayMenuState == OverlayMenuState.IDLE || overlayMenuState == OverlayMenuState.SHOWING_INSTRUCTIONS,
                            enter = slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = tween(durationMillis = 0)
                            ) + fadeIn(animationSpec = tween(300)),
                            exit = slideOutVertically(
                                targetOffsetY = { it },
                                animationSpec = tween(durationMillis = 200)
                            ) + fadeOut(animationSpec = tween(200))
                        ) {
                            MenuScreen(
                                onOpenGameOptions = {
                                    overlayMenuState = OverlayMenuState.EXITING_MENU
                                    targetMenuState = OverlayMenuState.SHOWING_OPTIONS
                                },
                                onOpenAccountSettings = {
                                    overlayMenuState = OverlayMenuState.EXITING_MENU
                                    targetMenuState = OverlayMenuState.SHOWING_SETTINGS
                                },
                                onOpenInstructions = {
                                    overlayMenuState = OverlayMenuState.SHOWING_INSTRUCTIONS
                                }
                            )
                        }

                        AnimatedVisibility(
                            visible = overlayMenuState == OverlayMenuState.SHOWING_OPTIONS,
                            enter = slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = tween(durationMillis = 0)
                            ) + fadeIn(animationSpec = tween(300)),
                            exit = slideOutVertically(
                                targetOffsetY = { it },
                                animationSpec = tween(durationMillis = 200)
                            ) + fadeOut(animationSpec = tween(200))
                        ) {
                            GameOptionsScreen(
                                viewModel = viewModel,
                                onBack = { overlayMenuState = OverlayMenuState.IDLE },
                                onGetAiPhoto = { uri ->
                                    activeDialog = ActiveDialog.AiFilter(uri)
                                }
                            )
                        }

                        AnimatedVisibility(
                            visible = overlayMenuState == OverlayMenuState.SHOWING_SETTINGS,
                            enter = slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = tween(durationMillis = 0)
                            ) + fadeIn(animationSpec = tween(300)),
                            exit = slideOutVertically(
                                targetOffsetY = { it },
                                animationSpec = tween(durationMillis = 200)
                            ) + fadeOut(animationSpec = tween(200))
                        ) {
                            AccountSettingsScreen(
                                user = entry,
                                onBack = { overlayMenuState = OverlayMenuState.IDLE },
                                onOpenPasswordRecovery = {
                                   activeDialog = ActiveDialog.Password(entry?.registerEmail ?: "Type here...")
                                },
                                onOpenRegister = {
                                    activeDialog = if (!hasLinkedEmail || !hasLinkedPassword)
                                        ActiveDialog.Register
                                    else
                                        ActiveDialog.Error("The account is already linked to: ${entry.registerEmail}.", DialogSource.NONE)
                                },
                                onOpenAccess = { activeDialog = ActiveDialog.Access },
                                onOpenDeleteAccount = {
                                    activeDialog = if (hasLinkedEmail && hasLinkedPassword) {
                                        ActiveDialog.DeleteAccount
                                    } else {
                                        ActiveDialog.Error(
                                            "The account has not been registered yet, therefore it cannot be deleted.",
                                            DialogSource.NONE
                                        )
                                    }
                                }
                            )
                        }
                    }
                    NavigationItem.Fleet -> {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(300)),
                            exit = fadeOut(tween(200))
                        ) {
                            FleetScreen(
                                db = db,
                                repository = repository,
                                onConfirm = {
                                    activeDialog = ActiveDialog.Deployment
                                }
                            )
                        }
                    }
                    NavigationItem.Game -> {
                        LaunchedEffect(overlayChallengeState) {
                            if (overlayChallengeState == OverlayChallengeState.EXITING_CHALLENGE) {
                                delay(200)
                                overlayChallengeState = targetChallengeState
                            }
                        }

                        // -- CHALLENGES MENU --
                        AnimatedVisibility(
                            visible = overlayChallengeState == OverlayChallengeState.IDLE,
                            enter = fadeIn(animationSpec = tween(300)),
                            exit = fadeOut(animationSpec = tween(200))
                        ) {
                            ChallengeScreen(
                                viewModel = profileViewModel,
                                onOpenTorpedo = {
                                    overlayChallengeState = OverlayChallengeState.EXITING_CHALLENGE
                                    overlayChallengeState = OverlayChallengeState.SHOWING_TORPEDO
                                },
                                onOpenCargo = {
                                    overlayChallengeState = OverlayChallengeState.EXITING_CHALLENGE
                                    overlayChallengeState = OverlayChallengeState.SHOWING_CARGO
                                },
                                onOpenBilge = {
                                    overlayChallengeState = OverlayChallengeState.EXITING_CHALLENGE
                                    overlayChallengeState = OverlayChallengeState.SHOWING_BILGE
                                }
                            )
                        }

                        // -- TORPEDO --
                        AnimatedVisibility(
                            visible = overlayChallengeState == OverlayChallengeState.SHOWING_TORPEDO,
                            enter = fadeIn(animationSpec = tween(300)),
                            exit = fadeOut(animationSpec = tween(200))
                        ) {
                            TorpedoScreen(
                                onGameResult = { isWin ->
                                    if (isWin) {
                                        // [ ACTION ]: Reward the player with XP and Score
                                        entry?.let { user ->
                                            profileViewModel.addMiniGameWinReward(user.id)
                                        }
                                    }
                                    overlayChallengeState = OverlayChallengeState.IDLE
                                }
                            )
                        }

                        // -- CARGO --
                        AnimatedVisibility(
                            visible = overlayChallengeState == OverlayChallengeState.SHOWING_CARGO,
                            enter = fadeIn(animationSpec = tween(300)),
                            exit = fadeOut(animationSpec = tween(200))
                        ) {
                            CargoScreen(
                                onGameResult = { isWin ->
                                    if (isWin) {
                                        entry?.let { user ->
                                            profileViewModel.addMiniGameWinReward(user.id)
                                        }
                                    }
                                    overlayChallengeState = OverlayChallengeState.IDLE
                                }
                            )
                        }

                        // -- BILGE --
                        AnimatedVisibility(
                            visible = overlayChallengeState == OverlayChallengeState.SHOWING_BILGE,
                            enter = fadeIn(animationSpec = tween(300)),
                            exit = fadeOut(animationSpec = tween(200))
                        ) {
                            BilgeScreen(
                                onGameResult = { isWin ->
                                    if (isWin) {
                                        entry?.let { user ->
                                            profileViewModel.addMiniGameWinReward(user.id)
                                        }
                                    }
                                    overlayChallengeState = OverlayChallengeState.IDLE
                                }
                            )
                        }
                    }
                }
            }
        }

        // -- CUSTOM POPUP "WELCOME BACK" --
        if (showGooglePopup && entry != null) {
            Popup(
                start = true,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = scale.dp(98f))
                    .zIndex(2f),
                avatar = {
                    AsyncImage(
                        model = entry.googlePhotoUrl,
                        error = painterResource(id = R.drawable.ic_terms),
                        contentDescription = "avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                },
                content = {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Hello, ${entry.googleName.ifEmpty { entry.name }}",
                            color = Color.Black,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            fontSize = scale.sp(22f),
                            letterSpacing = scale.sp(2f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(
                                    includeFontPadding = false
                                ),
                                lineHeightStyle = LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Center,
                                    trim = LineHeightStyle.Trim.Both
                                )
                            )
                        )

                        Spacer(modifier = Modifier.height(scale.dp(8f)))

                        Text(
                            text = entry.email,
                            color = LightGrey,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            fontSize = scale.sp(20f),
                            letterSpacing = scale.sp(2f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(
                                    includeFontPadding = false
                                ),
                                lineHeightStyle = LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Center,
                                    trim = LineHeightStyle.Trim.Both
                                )
                            )
                        )
                    }
                },
                onDone = {
                    showGooglePopup = false
                    viewModel.showHomeWelcome = false
                }
            )
        }
    }

    // -- DIALOGS HANDLER --
    when (val currentDialog = activeDialog) {
        is ActiveDialog.None -> {  }

        // -- PROFILE DIALOGS --
        is ActiveDialog.Filter -> {
            DialogFilter(
                imageUri = currentDialog.uri,
                onDismiss = { activeDialog = ActiveDialog.None },
                onConfirm = { bitmap ->
                    viewModel.updateProfilePicture(context, bitmap)
                    activeDialog = ActiveDialog.None
                }
            )
        }

        is ActiveDialog.Flag -> {
            DialogFlag(
                availableFlags = availableFlags,
                currentCode = entry?.countryCode ?: "IT",
                onDismiss = { activeDialog = ActiveDialog.None },
                onConfirm = { flag ->
                    viewModel.updateCountry(flag.code)
                    activeDialog = ActiveDialog.None
                }
            )
        }

        is ActiveDialog.Name -> {
            DialogName(
                currentName = entry?.name ?: "Sailor",
                onDismiss = { activeDialog = ActiveDialog.None },
                onConfirm = { newName ->
                    viewModel.updateName(newName)
                    activeDialog = ActiveDialog.None
                }
            )
        }

        is ActiveDialog.Friend -> {
            DialogFriend(
                onDismiss = { activeDialog = ActiveDialog.None },
                onConfirm = { friendId ->
                    viewModel.sendFriendRequest(friendId) { success, message ->
                        activeDialog = if (!success) {
                            ActiveDialog.Error(message, DialogSource.FRIEND)
                        } else
                            ActiveDialog.None
                    }
                }
            )
        }

        // -- ACCOUNT DIALOGS --
        is ActiveDialog.Register -> {
            DialogRegister(
                onDismiss = { activeDialog = ActiveDialog.None; registerViewModel.resetState() },
                onConfirm = { email, password, confirmPassword ->
                    registerViewModel.register(email, password, confirmPassword)
                }
            )
        }

        is ActiveDialog.Access -> {
            val isLoading = uiStateAS is RegisterUiState.Loading
            
            DialogAccess(
                onDismiss = { activeDialog = ActiveDialog.None; registerViewModel.resetState() },
                onConfirm = { email, password -> registerViewModel.login(context, email, password) },
                isLoading = isLoading
            )
        }

        is ActiveDialog.Password -> {
            DialogPassword(
                email = currentDialog.emailToPrefill,
                onDismiss = { activeDialog = ActiveDialog.None },
                onConfirm = { email ->
                    profileViewModel.resetPassword(email) { success, message ->
                        activeDialog = if (!success) {
                            ActiveDialog.Error(message, DialogSource.PASSWORD)
                        } else {
                            ActiveDialog.None
                        }
                    }
                }
            )
        }

        is ActiveDialog.DeleteAccount -> {
            if (entry != null) {
                DialogDeleteAccount(
                    onDismiss = { activeDialog = ActiveDialog.None },
                    onConfirm = {
                        profileViewModel.deleteAccount(entry) { success, errorMessage ->
                            if (success) {
                                MatchStateStorage.clear(context)

                                coroutineScope.launch {
                                    try {
                                        val credentialManager = CredentialManager.create(context)
                                        credentialManager.clearCredentialState(
                                            ClearCredentialStateRequest()
                                        )
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        onRestart()
                                    }
                                }
                            } else {
                                val finalMessage = errorMessage ?: "Failed to delete the account."

                                activeDialog = ActiveDialog.Error(
                                    finalMessage,
                                    DialogSource.DELETE
                                )
                            }
                        }
                    }
                )
            }
        }

        // -- GAME OPTIONS DIALOGS --
        is ActiveDialog.AiFilter -> {
            DialogFilter(
                imageUri = currentDialog.uri,
                onDismiss = { activeDialog = ActiveDialog.None },
                onConfirm = { bitmap ->
                    profileViewModel.updateAiAvatar(context, bitmap)
                    activeDialog = ActiveDialog.None
                }
            )
        }

        // -- ARMADA DIALOGS --
        is ActiveDialog.Deployment -> {
            DialogFleetConfirm {
                activeDialog = ActiveDialog.None
            }
        }

        // -- SYSTEM / ERROR DIALOGS --
        is ActiveDialog.Error -> {
            DialogError(
                errorMessage = currentDialog.message,
                onDismiss = {
                    activeDialog = when (currentDialog.source) {
                        DialogSource.REGISTER -> {
                            registerViewModel.resetState(); ActiveDialog.Register
                        }

                        DialogSource.ACCESS -> {
                            registerViewModel.resetState(); ActiveDialog.Access
                        }

                        DialogSource.PASSWORD -> ActiveDialog.None
                        DialogSource.DELETE -> ActiveDialog.None
                        DialogSource.FRIEND -> ActiveDialog.Friend
                        else -> ActiveDialog.None
                    }
                }
            )
        }
    }

    // -- MenuScreen --
    if (overlayMenuState == OverlayMenuState.SHOWING_INSTRUCTIONS) {
        DialogInstructions(
            onDismiss = { overlayMenuState = OverlayMenuState.IDLE }
        )
    }

    // -- GameLobbyScreen --
    if (overlayHomeState == OverlayHomeState.LOADING_MATCH) {
        DialogLoading(
            onDismiss = {
                // [ TO - DO ]: Cancel the match request

                overlayHomeState = OverlayHomeState.SHOWING_LOBBY
            }
        )

        LaunchedEffect(Unit) {
            delay(4500) // [ TO - DO ]: Change this value in order to wait until the match can be started ( min. 4500L * 2 as the Intro.kt radar )
            overlayHomeState = OverlayHomeState.PLAYING_VS_FRIEND
        }
    }
}

@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@Composable
private fun Modal(
    onOpenGameModes: (Int) -> Unit
) {
    // -- SCALE ( used for applying conversions ) --
    // [ MEMO ]: Sizes are taken from 720 x 1600px mockup ( with 72dpi ) using the Redmi Note 10S
    val scale = RememberScaleConversion()

    val barHeight = 108f
    val maxWidth = scale.dp(720f)

    val animationDuration = 250
    val stopDuration = animationDuration * 2.0f

    var currentMode by remember { mutableIntStateOf(0) }
    var isMovingForward by remember { mutableStateOf(true) } // False -> Backward / Right, True -> Forward / Left

    var isAnimating by remember { mutableStateOf(false) }

    // -- GESTURES --
    var totalDragDistance by remember { mutableFloatStateOf(0f) }
    var hasSwitchedThisGesture by remember { mutableStateOf(false) }

    LaunchedEffect(currentMode) {
        if (isAnimating) {
            delay(stopDuration.toLong())
            isAnimating = false
        }
    }

    fun changeMode(goForward: Boolean) {
        if (isAnimating)
            return

        isMovingForward = goForward
        isAnimating = true

        currentMode = if (goForward)
            (currentMode + 1) % gameModes.size
        else
            ((currentMode - 1) + gameModes.size) % gameModes.size
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        totalDragDistance = 0f
                        hasSwitchedThisGesture = false
                    },
                    onDragEnd = {
                        totalDragDistance = 0f
                        hasSwitchedThisGesture = false
                    },
                    onDragCancel = {
                        totalDragDistance = 0f
                        hasSwitchedThisGesture = false
                    }
                ) { change, dragAmount ->
                    change.consume()

                    if (!hasSwitchedThisGesture && !isAnimating) {
                        totalDragDistance += dragAmount

                        val threshold = scale.dp(24f).toPx()

                        if (totalDragDistance < -threshold) {
                            changeMode(true)
                            hasSwitchedThisGesture = true
                        } else {
                            if (totalDragDistance > threshold) {
                                changeMode(false)
                                hasSwitchedThisGesture = true
                            }
                        }
                    }
                }
            }
    ) {
        val topOffset = (maxHeight / 2) + scale.dp(54f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = maxWidth) // [ MEMO ]: Remove it if not necessary
                .align(Alignment.TopCenter)
                .padding(top = topOffset)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(scale.dp(barHeight))
                        .background(Color(0xFF26768E))
                        .padding(vertical = scale.dp(6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Arrow(
                        isLeft = true,
                        modifier = Modifier.align(Alignment.CenterStart),
                        onClick = { changeMode(false) }
                    )

                    AnimatedContent(
                        targetState = gameModes[currentMode],
                        contentKey = { it.title },
                        transitionSpec = {
                            val slideIn = if (isMovingForward)
                                slideInHorizontally(tween(animationDuration)) { width -> width + (width / 2) }
                            else
                                slideInHorizontally(tween(animationDuration)) { width -> -(width + (width / 2)) }

                            val slideOut = if (isMovingForward)
                                slideOutHorizontally(tween(animationDuration)) { width -> -width * 2 }
                            else
                                slideOutHorizontally(tween(animationDuration)) { width -> width * 2 }

                            val fadeInAnim = fadeIn(tween(animationDuration))

                            val fadeOutAnim = fadeOut(
                                animationSpec = keyframes {
                                    durationMillis = animationDuration
                                    1f at (animationDuration * 0.5f).toInt()
                                    0f at animationDuration
                                }
                            )

                            (slideIn + fadeInAnim).togetherWith(slideOut + fadeOutAnim)
                                .using(SizeTransform(clip = false))
                        },
                        label = "TextAnimation"
                    ) { mode ->
                        Column(
                            modifier = Modifier.fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = mode.title,
                                color = White,
                                textAlign = TextAlign.Center,
                                fontSize = scale.sp(40f),
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = scale.sp(2f),
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    lineHeightStyle = LineHeightStyle(
                                        LineHeightStyle.Alignment.Center,
                                        LineHeightStyle.Trim.Both
                                    ),
                                    shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)
                                )
                            )

                            Text(
                                text = mode.sub,
                                color = White,
                                textAlign = TextAlign.Center,
                                fontSize = scale.sp(28f),
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = scale.sp(2f),
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    lineHeightStyle = LineHeightStyle(
                                        LineHeightStyle.Alignment.Center,
                                        LineHeightStyle.Trim.Both
                                    ),
                                    shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)
                                )
                            )
                        }
                    }

                    Arrow(
                        isLeft = false,
                        modifier = Modifier.align(Alignment.CenterEnd),
                        onClick = { changeMode(true) }
                    )
                }

                AnimatedContent(
                    targetState = gameModes[currentMode],
                    contentKey = { it.button },
                    transitionSpec = {
                        val slideIn = if (isMovingForward)
                            slideInHorizontally(tween(animationDuration)) { width -> width + (width / 2) }
                        else
                            slideInHorizontally(tween(animationDuration)) { width -> -(width + (width / 2)) }

                        val slideOut = if (isMovingForward)
                            slideOutHorizontally(tween(animationDuration)) { width -> -width * 2 }
                        else
                            slideOutHorizontally(tween(animationDuration)) { width -> width * 2 }

                        val fadeInAnim = fadeIn(tween(animationDuration))

                        val fadeOutAnim = fadeOut(
                            animationSpec = keyframes {
                                durationMillis = animationDuration
                                1f at (animationDuration * 0.5f).toInt()
                                0f at animationDuration
                            }
                        )

                        (slideIn + fadeInAnim).togetherWith(slideOut + fadeOutAnim)
                            .using(SizeTransform(clip = false))
                    },
                    label = "ButtonAnimation",
                    modifier = Modifier.offset(y = scale.dp(12f))
                ) { targetMode ->
                    TertiaryButton(
                        paddingH = 32f,
                        paddingV = 16f,
                        text = targetMode.button,
                        onClick = {
                            // [ TO - DO ]: Redirect the user according to the chosen mode
                            // e.g., if (!isAnimating), if (currentMode.opponent == OpponentType.Search) { ... }
                            if (!isAnimating)
                                onOpenGameModes(currentMode)
                        },
                        icons = {
                            GameModeIcons()
                        }
                    )
                }
            }
        }
    }
}

