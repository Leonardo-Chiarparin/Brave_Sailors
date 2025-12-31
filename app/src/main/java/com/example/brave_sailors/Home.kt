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
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.example.brave_sailors.model.ProfileViewModel
import com.example.brave_sailors.ui.components.Arrow
import com.example.brave_sailors.ui.components.DialogFilter
import com.example.brave_sailors.ui.components.DialogFlag
import com.example.brave_sailors.ui.components.DialogInstructions
import com.example.brave_sailors.ui.components.DialogName
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

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomeScreen(innerPadding: PaddingValues = PaddingValues(0.dp), viewModel: ProfileViewModel, onNavigateToTerms: () -> Unit) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(NavigationItem.Home) }

    var isContentVisible by remember { mutableStateOf(false) }

    var showDialogFilter by remember { mutableStateOf(false) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }

    var showDialogFlag by remember { mutableStateOf(false) }
    var showDialogName by remember { mutableStateOf(false) }

    // -- PROFILE STATE --
    var overlayProfileState by remember { mutableStateOf(OverlayProfileState.IDLE) }
    var targetProfileState by remember { mutableStateOf(OverlayProfileState.IDLE) }

    // -- MENU STATE --
    var overlayMenuState by remember { mutableStateOf(OverlayMenuState.IDLE) }
    var targetMenuState by remember { mutableStateOf(OverlayMenuState.IDLE) }


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
                NavigationBar(
                    currentScreen = currentScreen,
                    onItemClick = { newItem ->
                        if (currentScreen != newItem) {
                            currentScreen = newItem
                            isContentVisible = false
                            overlayProfileState = OverlayProfileState.IDLE
                            overlayMenuState = OverlayMenuState.IDLE
                        }
                    }
                )
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                when (currentScreen) {
                    NavigationItem.Home -> Modal()
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
                            }
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
                                onBack = { overlayMenuState = OverlayMenuState.IDLE }
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

    if (overlayMenuState == OverlayMenuState.SHOWING_INSTRUCTIONS) {
        DialogInstructions(
            onDismiss = { overlayMenuState = OverlayMenuState.IDLE }
        )
    }
}

// -- PLACEHOLDER SCREENS ( place them according to their forthcoming ".kt" file ) --
@Composable
fun FleetScreen() {  }

@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@Composable
private fun Modal() {
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

