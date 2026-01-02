package com.example.brave_sailors

import android.annotation.SuppressLint
import android.net.Uri
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.example.brave_sailors.data.local.database.AppDatabase
import com.example.brave_sailors.data.remote.api.RetrofitClient
import com.example.brave_sailors.data.repository.UserRepository
import com.example.brave_sailors.model.FleetPlacedShip
import com.example.brave_sailors.model.FleetUiEvent
import com.example.brave_sailors.model.FleetViewModel
import com.example.brave_sailors.model.FleetViewModelFactory
import com.example.brave_sailors.model.ProfileViewModel
import com.example.brave_sailors.model.ShipOrientation
import com.example.brave_sailors.ui.components.Arrow
import com.example.brave_sailors.ui.components.DialogAccess
import com.example.brave_sailors.ui.components.DialogDeleteAccount
import com.example.brave_sailors.ui.components.DialogError
import com.example.brave_sailors.ui.components.DialogFilter
import com.example.brave_sailors.ui.components.DialogFlag
import com.example.brave_sailors.ui.components.DialogFriend
import com.example.brave_sailors.ui.components.DialogInstructions
import com.example.brave_sailors.ui.components.DialogName
import com.example.brave_sailors.ui.components.DialogPassword
import com.example.brave_sailors.ui.components.DialogRegister
import com.example.brave_sailors.ui.components.MosaicBackground
import com.example.brave_sailors.ui.components.NavigationBar
import com.example.brave_sailors.ui.components.NavigationConstants.TOTAL_DURATION
import com.example.brave_sailors.ui.components.NavigationItem
import com.example.brave_sailors.ui.components.Popup
import com.example.brave_sailors.ui.components.TertiaryButton
import com.example.brave_sailors.ui.theme.LightGrey
import com.example.brave_sailors.ui.theme.White
import com.example.brave_sailors.ui.utils.GameModeIcons
import com.example.brave_sailors.ui.utils.RememberScaleConversion
import com.example.brave_sailors.ui.utils.gameModes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class OverlayHomeState {
    IDLE,
    EXITING_HOME,
    SHOWING_DETAILS
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomeScreen(innerPadding: PaddingValues = PaddingValues(0.dp), viewModel: ProfileViewModel, onNavigateToTerms: () -> Unit) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(NavigationItem.Home) }

    var isContentVisible by remember { mutableStateOf(false) }

    // -- PROFILE DIALOGS --
    var showDialogFilter by remember { mutableStateOf(false) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }

    var showDialogFlag by remember { mutableStateOf(false) }
    var showDialogName by remember { mutableStateOf(false) }
    var showDialogFriends by remember { mutableStateOf(false) }

    // -- PROFILE STATE --
    var overlayProfileState by remember { mutableStateOf(OverlayProfileState.IDLE) }
    var targetProfileState by remember { mutableStateOf(OverlayProfileState.IDLE) }

    // -- MENU STATE --
    var overlayMenuState by remember { mutableStateOf(OverlayMenuState.IDLE) }
    var targetMenuState by remember { mutableStateOf(OverlayMenuState.IDLE) }

    // -- ACCOUNT SETTINGS' DIALOGS --
    var showDialogPassword by remember { mutableStateOf(false) }
    var showDialogRegister by remember { mutableStateOf(false) }
    var showDialogAccess by remember { mutableStateOf(false) }
    var showDialogDeleteAccount by remember { mutableStateOf(false) }

    // -- HOME STATE --
    var overlayHomeState by remember { mutableStateOf(OverlayHomeState.IDLE) }
    var targetHomeState by remember { mutableStateOf(OverlayHomeState.IDLE) }
    var chosenGameModeIndex by remember { mutableIntStateOf(0) }

    // -- ERROR DIALOG --
    var showDialogError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("Please try again.") }

    val scale = RememberScaleConversion()

    // -- STATES --
    var showGooglePopup by remember { mutableStateOf(false) }
    val flagList = viewModel.flagList.collectAsState()
    val user = viewModel.userState.collectAsState()

    val availableFlags = flagList.value
    val entry = user.value

    val forceLogout by viewModel.forceLogoutEvent.collectAsState()

    LaunchedEffect(viewModel.showHomeWelcome, entry) {
        if (viewModel.showHomeWelcome && entry != null) {
            launch(Dispatchers.IO) {
                val photoUrl = entry.googlePhotoUrl

                if (!photoUrl.isNullOrEmpty()) {
                    val request = ImageRequest.Builder(context)
                        .data(photoUrl)
                        .build()
                    context.imageLoader.execute(request)
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

    if (forceLogout) {
        viewModel.performLocalLogout()
        onNavigateToTerms()
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
                AnimatedVisibility(
                    visible = overlayHomeState == OverlayHomeState.IDLE,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(durationMillis = 0)
                    ) + fadeIn(animationSpec = tween(0)),
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
                            if (overlayHomeState == OverlayHomeState.EXITING_HOME) {
                                delay(200)
                                overlayHomeState = targetHomeState
                            }
                        }

                        if ( overlayHomeState == OverlayHomeState.IDLE ) {
                            Modal(
                                onOpenGameModes = { index ->
                                    chosenGameModeIndex = index
                                    overlayHomeState = OverlayHomeState.EXITING_HOME
                                    targetHomeState = OverlayHomeState.SHOWING_DETAILS
                                }
                            )
                        }

                        // -- GAME MODE'S DETAILS --
                        if ( overlayHomeState == OverlayHomeState.SHOWING_DETAILS ) {
                            GameModeDetailsScreen(
                                gameModeIndex = chosenGameModeIndex,
                                onBack = { overlayHomeState = OverlayHomeState.IDLE },
                                onContinue = { /* [ TO - DO ] */ }
                            )
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
                            viewModel = viewModel,
                            onGetPhoto = { uri ->
                                capturedImageUri = uri
                                showDialogFilter = true
                            },
                            onOpenChangeFlag = {
                                showDialogFlag = true
                            },
                            onOpenChangeName = {
                                showDialogName = true
                            },
                            onOpenStatistics = {
                                overlayProfileState = OverlayProfileState.EXITING_PROFILE
                                targetProfileState = OverlayProfileState.SHOWING_STATS
                            },
                            onOpenRankings = {
                                overlayProfileState = OverlayProfileState.EXITING_PROFILE
                                targetProfileState = OverlayProfileState.SHOWING_RANKINGS
                            },
                            onOpenFriends = { showDialogFriends = true }
                        )

                        // -- STATISTICS --
                        if (overlayProfileState == OverlayProfileState.SHOWING_STATS) {
                            StatisticsScreen(
                                user = entry,
                                onBack = { overlayProfileState = OverlayProfileState.IDLE }
                            )
                        }

                        // -- RANKINGS --
                        if (overlayProfileState == OverlayProfileState.SHOWING_RANKINGS) {
                            RankingsScreen(
                                user = entry,
                                availableFlags,
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
                            ) + fadeIn(animationSpec = tween(0)),
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
                            ) + fadeIn(animationSpec = tween(0)),
                            exit = slideOutVertically(
                                targetOffsetY = { it },
                                animationSpec = tween(durationMillis = 200)
                            ) + fadeOut(animationSpec = tween(200))
                        ) {
                            GameOptionsScreen(
                                onBack = { overlayMenuState = OverlayMenuState.IDLE }
                            )
                        }

                        AnimatedVisibility(
                            visible = overlayMenuState == OverlayMenuState.SHOWING_SETTINGS,
                            enter = slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = tween(durationMillis = 0)
                            ) + fadeIn(animationSpec = tween(0)),
                            exit = slideOutVertically(
                                targetOffsetY = { it },
                                animationSpec = tween(durationMillis = 200)
                            ) + fadeOut(animationSpec = tween(200))
                        ) {
                            AccountSettingsScreen(
                                user = entry,
                                onBack = { overlayMenuState = OverlayMenuState.IDLE },
                                onOpenPasswordRecovery = { showDialogPassword = true },
                                onOpenRegister = { showDialogRegister = true },
                                onOpenAccess = { showDialogAccess = true },
                                onOpenDeleteAccount = { showDialogDeleteAccount = true }
                            )
                        }
                    }
                    NavigationItem.Fleet -> FleetScreen()
                    NavigationItem.Game -> {}
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
                    if (entry.googlePhotoUrl.isNullOrEmpty() || entry.googlePhotoUrl == "ic_terms") {
                        Image(
                            painter = painterResource(id = R.drawable.ic_terms),
                            contentDescription = "avatar",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        AsyncImage(
                            model = entry.googlePhotoUrl,
                            error = painterResource(id = R.drawable.ic_terms),
                            contentDescription = "avatar",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                },
                content = {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Hello, ${entry.googleName}",
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

    // -- DIALOGS --
    // -- ProfileScreen --
    if (showDialogFilter && capturedImageUri != null) {
        DialogFilter(
            imageUri = capturedImageUri!!,
            onDismiss = { showDialogFilter = false },
            onConfirm = { bitmap ->
                viewModel.updateProfilePicture(context, bitmap)
                showDialogFilter = false
            }
        )
    }

    if (showDialogFlag) {
        DialogFlag(
            availableFlags = availableFlags,
            currentCode = entry?.countryCode ?: "IT",
            onDismiss = { showDialogFlag = false },
            onConfirm = { flag ->
                viewModel.updateCountry(flag.code)
                showDialogFlag = false
            }
        )
    }

    if (showDialogName) {
        DialogName(
            currentName = entry?.name ?: "Sailor",
            onDismiss = { showDialogName = false },
            onConfirm = { newName ->
                viewModel.updateName(newName)
                showDialogName = false
            }
        )
    }

    if (showDialogFriends) {
        DialogFriend(
            onDismiss = { showDialogFriends = false },
            onConfirm = { friendId ->
                // [ TO - DO ]: Implement the logic through which it is possible to add a new friend having the specified ID for the current user
                showDialogFriends = false
            }
        )
    }

    // -- MenuScreen --
    if (overlayMenuState == OverlayMenuState.SHOWING_INSTRUCTIONS) {
        DialogInstructions(
            onDismiss = { overlayMenuState = OverlayMenuState.IDLE }
        )
    }

    // -- AccountSettingsScreen --
    if (showDialogPassword) {
        DialogPassword(
            onDismiss = { showDialogPassword = false },
            onConfirm = { email ->
                showDialogPassword = false

                // [ TO - DO ]: Verify using the ViewModel ( DB, etc. ) whether the email address used for registering the account matches, and, only in that case, send the password through that reference.
                // e.g. the following check may be applied
                if (!email.contains("@")) {
                    // errorMessage = "E-mail not found."
                    showDialogError = true
                }
            }
        )
    }

    if (showDialogRegister) {
        DialogRegister(
            onDismiss = { showDialogRegister = false },
            onConfirm = { email, password ->
                // [ TO - DO ]: ...
                showDialogRegister = false
            }
        )
    }

    if (showDialogAccess) {
        DialogAccess(
            onDismiss = { showDialogAccess = false },
            onConfirm = { email, password ->
                // [ TO - DO ]: ...
                showDialogAccess = false
            }
        )
    }

    if (showDialogDeleteAccount) {
        DialogDeleteAccount(
            onDismiss = { showDialogDeleteAccount = false },
            onConfirm = {
                // [ TO - DO ]: ...
                showDialogDeleteAccount = false
            }
        )
    }

    // -- DialogError --
    if (showDialogError) {
        DialogError(
            errorMessage = errorMessage,
            onDismiss = { showDialogError = false }
        )
    }

}

private val CELL_SIZE = 40.dp
private const val GRID_SIZE = 8
private val GRID_BORDER_COLOR = Color(0xFF4A90E2)
private val GRID_BG_COLOR = Color(0xFF1A3A4A).copy(alpha = 0.5f)
private val SHIP_COLOR = Color(0xFF8899A6)
private val PREVIEW_OK = Color(0xFF32CD32)
private val PREVIEW_BAD = Color(0xFFFF0000)

private val SHIP_ICONS = mapOf(
    5 to "🚢",
    4 to "🛳️",
    3 to "🛥️",
    2 to "⛵",
    1 to "🚤"
)
private const val DEFAULT_SHIP_ICON = "❓"

private data class DragState(
    val isDragging: Boolean = false,
    val draggedShipSize: Int = 0,
    val currentDragOffset: Offset = Offset.Zero
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FleetScreen() {

    // Fetch local database and remote API service
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val apiService = RetrofitClient.api

    // Initialize the ViewModel with its Factory
    val viewModel: FleetViewModel = viewModel(
        factory = FleetViewModelFactory(
            u = db.userDao(),
            f = db.fleetDao(),
            r = UserRepository(
                api = apiService,
                userDao = db.userDao(),
                fleetDao = db.fleetDao()
            )
        )
    )

    val state by viewModel.state.collectAsState()
    val density = LocalDensity.current
    val snackBarHostState = remember { SnackbarHostState() }

    // Listen for UI events from the ViewModel
    LaunchedEffect(Unit) {
        viewModel.events.collect { ev ->
            when (ev) {
                // Silently handle message events without showing a UI component
                is FleetUiEvent.Message -> {
                    /* No message displayed */
                }

                // Silently handle successful save and sync
                is FleetUiEvent.SaveSuccess -> {
                    /* No snackBar displayed as requested */
                }
            }
        }
    }

    // Drag state (ghost ship + current finger position)
    var dragState by remember { mutableStateOf(DragState()) }

    // Board metrics used for coordinate conversion
    var boardOffset by remember { mutableStateOf(Offset.Unspecified) }
    var boardCellPx by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "FLEET FORMATION",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 24.dp)
            )

            // --- BATTLE GRID ---
            Box(
                modifier = Modifier
                    .border(2.dp, GRID_BORDER_COLOR, RoundedCornerShape(8.dp))
                    .background(GRID_BG_COLOR, RoundedCornerShape(8.dp))
                    .graphicsLayer { clip = true; shape = RoundedCornerShape(8.dp) }
                    .padding(4.dp)
            ) {
                // Inner box: this is the TRUE origin of the cells (0,0).
                Box(
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        boardOffset = coordinates.positionInWindow()
                        boardCellPx = with(density) { CELL_SIZE.toPx() }
                    }
                ) {
                    // Layer 1: Placement preview overlay (green/red validation)
                    Column {
                        for (row in 0 until GRID_SIZE) {
                            Row {
                                for (col in 0 until GRID_SIZE) {
                                    val preview = state.placementPreview
                                    val isPreviewCell = preview?.coordinates?.contains(row to col) == true
                                    val isPreviewValid = preview?.isValid == true

                                    val color = when {
                                        isPreviewCell && isPreviewValid -> PREVIEW_OK.copy(alpha = 0.6f)
                                        isPreviewCell && !isPreviewValid -> PREVIEW_BAD.copy(alpha = 0.6f)
                                        else -> Color.Transparent
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(CELL_SIZE)
                                            .border(0.5.dp, Color.White.copy(alpha = 0.1f))
                                            .background(color)
                                    )
                                }
                            }
                        }
                    }

                    // Layer 2: Already placed ships
                    Box(Modifier.matchParentSize()) {
                        state.placedShips.forEach { ship ->
                            ShipDrawing(ship, CELL_SIZE)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // --- CONTROLS SECTION ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 1. ROTATE ACTION
                Button(
                    onClick = { viewModel.onRotate() },
                    enabled = !state.isSaved,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text("ROT", fontSize = 10.sp, maxLines = 1)
                    }
                }

                // 2. AUTO-PLACE ACTION
                Button(
                    onClick = { viewModel.autoPlaceFleet() },
                    enabled = !state.isSaved,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎲", fontSize = 18.sp)
                        Text("AUTO", fontSize = 10.sp, maxLines = 1)
                    }
                }

                // 3. RESET ACTION
                Button(
                    onClick = { viewModel.onReset() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🗑️", fontSize = 16.sp)
                        Text("RESET", fontSize = 10.sp, maxLines = 1)
                    }
                }

                // 4. SAVE & SYNC ACTION
                Button(
                    onClick = { viewModel.saveFleetToDb() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    enabled = state.shipsToPlace.isEmpty() && !state.isSaved,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text(if (state.isSaved) "OK" else "SAVE", fontSize = 10.sp, maxLines = 1)
                    }
                }
            }

            Spacer(Modifier.height(40.dp))

            // --- SHIP DOCK / STATUS BANNER ---
            if (state.isSaved) {
                // Displayed when the fleet configuration is successfully saved and synced
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(Color(0xFF1B5E20).copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.Green, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "FLEET READY",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "AWAITING ORDERS",
                            color = Color.LightGray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                if (state.shipsToPlace.isNotEmpty()) {
                    Text("Drag ships to grid:", color = Color.Gray)
                    Spacer(Modifier.height(12.dp))

                    val groupedShips =
                        state.shipsToPlace.groupingBy { it }.eachCount().toSortedMap(reverseOrder())

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .horizontalScroll(rememberScrollState())
                            .padding(16.dp)
                            .zIndex(1f),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        groupedShips.keys.toList().forEach { size ->
                            val count = groupedShips[size] ?: 0

                            ExactDraggableShipItem(
                                size = size,
                                count = count,
                                boardOffset = boardOffset,
                                boardCellPx = boardCellPx,
                                onDragStart = {
                                    viewModel.onDragStart(size)
                                    dragState = dragState.copy(isDragging = true, draggedShipSize = size)
                                },
                                onDragOffsetUpdate = { offset ->
                                    dragState = dragState.copy(currentDragOffset = offset)
                                },
                                onDragCell = { r, c ->
                                    viewModel.onDragHover(r, c, size)
                                },
                                onDragExit = { viewModel.onDragEnd() },
                                onDragEnd = {
                                    viewModel.onDragEnd()
                                    dragState = dragState.copy(isDragging = false)
                                },
                                onDragFinished = {
                                    viewModel.onDragEnd()
                                    dragState = dragState.copy(isDragging = false)
                                },
                                onDrop = { r, c ->
                                    viewModel.onDropShip(r, c, size)
                                    viewModel.onDragEnd()
                                    dragState = dragState.copy(isDragging = false)
                                }
                            )
                        }
                    }
                } else {
                    // Ready to save state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "FORMATION COMPLETE\nPRESS SAVE",
                            color = Color(0xFF69F0AE),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        // --- GHOST SHIP OVERLAY (DRAGGING) ---
        if (dragState.isDragging &&
            dragState.draggedShipSize > 0 &&
            dragState.currentDragOffset != Offset.Zero
        ) {
            val size = dragState.draggedShipSize
            val offsetPx = dragState.currentDragOffset
            val isHorizontal = state.currentOrientation == ShipOrientation.HORIZONTAL

            val (w, h) = with(density) {
                if (isHorizontal) (CELL_SIZE * size).toPx() to CELL_SIZE.toPx()
                else CELL_SIZE.toPx() to (CELL_SIZE * size).toPx()
            }

            val xOffset = offsetPx.x - (w / 2f)
            val yOffset = offsetPx.y - (h / 2f)

            Box(
                modifier = Modifier
                    .offset { IntOffset(xOffset.roundToInt(), yOffset.roundToInt()) }
                    .size(width = with(density) { w.toDp() }, height = with(density) { h.toDp() })
                    .zIndex(100f)
                    .graphicsLayer(alpha = 0.9f)
            ) {
                if (isHorizontal) {
                    Row(Modifier.fillMaxSize()) {
                        repeat(size) {
                            Box(
                                Modifier.weight(1f).fillMaxHeight()
                                    .background(SHIP_COLOR, RectangleShape)
                                    .border(1.dp, if (state.placementPreview?.isValid == true) Color.Green else Color.Red, RectangleShape)
                            )
                        }
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        repeat(size) {
                            Box(
                                Modifier.weight(1f).fillMaxWidth()
                                    .background(SHIP_COLOR, RectangleShape)
                                    .border(1.dp, if (state.placementPreview?.isValid == true) Color.Green else Color.Red, RectangleShape)
                            )
                        }
                    }
                }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = SHIP_ICONS[size] ?: DEFAULT_SHIP_ICON, fontSize = 24.sp)
                }
            }
        }

        SnackbarHost(
            hostState = snackBarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// --- DRAGGABLE ITEM COMPONENT ---
@Composable
fun ExactDraggableShipItem(
    size: Int,
    count: Int,
    boardOffset: Offset,
    boardCellPx: Float,
    onDragStart: () -> Unit,
    onDragOffsetUpdate: (Offset) -> Unit,
    onDragCell: (Int, Int) -> Unit,
    onDragExit: () -> Unit,
    onDragEnd: () -> Unit,
    onDragFinished: () -> Unit,
    onDrop: (Int, Int) -> Unit
) {
    var originInWindow by remember { mutableStateOf(Offset.Unspecified) }
    var currentPointerLocalOffset by remember { mutableStateOf(Offset.Zero) }
    var lastCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .onGloballyPositioned { originInWindow = it.positionInWindow() }
                .pointerInput(size) {
                    detectDragGestures(
                        onDragStart = { pointerOffset ->
                            currentPointerLocalOffset = pointerOffset
                            onDragStart()
                            onDragOffsetUpdate(originInWindow + currentPointerLocalOffset)

                            if (boardOffset != Offset.Unspecified && boardCellPx > 0f) {
                                val fingerAbsX = originInWindow.x + currentPointerLocalOffset.x
                                val fingerAbsY = originInWindow.y + currentPointerLocalOffset.y
                                val relX = fingerAbsX - boardOffset.x
                                val relY = fingerAbsY - boardOffset.y
                                val boardSizePx = boardCellPx * GRID_SIZE

                                if (relX in 0f..boardSizePx && relY in 0f..boardSizePx) {
                                    val c = (relX / boardCellPx).toInt().coerceIn(0, GRID_SIZE - 1)
                                    val r = (relY / boardCellPx).toInt().coerceIn(0, GRID_SIZE - 1)
                                    lastCell = r to c
                                    onDragCell(r, c)
                                } else {
                                    lastCell = null
                                    onDragExit()
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            currentPointerLocalOffset += dragAmount
                            onDragOffsetUpdate(originInWindow + currentPointerLocalOffset)

                            if (boardOffset != Offset.Unspecified && boardCellPx > 0f) {
                                val fingerAbsX = originInWindow.x + currentPointerLocalOffset.x
                                val fingerAbsY = originInWindow.y + currentPointerLocalOffset.y
                                val relX = fingerAbsX - boardOffset.x
                                val relY = fingerAbsY - boardOffset.y
                                val boardSizePx = boardCellPx * GRID_SIZE

                                if (relX in 0f..boardSizePx && relY in 0f..boardSizePx) {
                                    val c = (relX / boardCellPx).toInt().coerceIn(0, GRID_SIZE - 1)
                                    val r = (relY / boardCellPx).toInt().coerceIn(0, GRID_SIZE - 1)

                                    if (lastCell != (r to c)) {
                                        lastCell = r to c
                                        onDragCell(r, c)
                                    }
                                } else if (lastCell != null) {
                                    lastCell = null
                                    onDragExit()
                                }
                            }
                        },
                        onDragEnd = {
                            lastCell?.let { (r, c) -> onDrop(r, c) } ?: run { onDragExit() }
                            lastCell = null
                            onDragFinished()
                            onDragEnd()
                        },
                        onDragCancel = {
                            lastCell = null
                            onDragExit()
                            onDragFinished()
                        }
                    )
                }
                .size(CELL_SIZE * size, CELL_SIZE),
            contentAlignment = Alignment.Center
        ) {
            // LAYER 1: The Squares (Horizontal orientation in dock)
            Row(modifier = Modifier.fillMaxSize()) {
                repeat(size) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(SHIP_COLOR, RectangleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RectangleShape)
                    )
                }
            }
            // LAYER 2: Ship Emoji
            Text(text = SHIP_ICONS[size] ?: DEFAULT_SHIP_ICON, fontSize = 24.sp)
        }

        Text("x$count", color = Color.White, fontWeight = FontWeight.Bold)
    }
}

// --- RENDERED SHIP COMPONENT ---
@Composable
fun ShipDrawing(ship: FleetPlacedShip, cellSize: Dp) {
    val isHorizontal = ship.isHorizontal
    val width = if (isHorizontal) cellSize * ship.size else cellSize
    val height = if (isHorizontal) cellSize else cellSize * ship.size

    Box(
        modifier = Modifier
            .offset(x = cellSize * ship.col, y = cellSize * ship.row)
            .size(width, height)
    ) {
        // LAYER 1: SQUARES (Visual representation of ship segments)
        if (isHorizontal) {
            Row(modifier = Modifier.fillMaxSize()) {
                repeat(ship.size) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(SHIP_COLOR, RectangleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RectangleShape)
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                repeat(ship.size) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(SHIP_COLOR, RectangleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RectangleShape)
                    )
                }
            }
        }

        // LAYER 2: SHIP ICON (Centered over the segments)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = SHIP_ICONS[ship.size] ?: DEFAULT_SHIP_ICON,
                fontSize = 24.sp
            )
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

