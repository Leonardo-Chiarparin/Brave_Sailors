package com.example.brave_sailors

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.example.brave_sailors.model.ProfileViewModel
import com.example.brave_sailors.ui.components.Arrow
import com.example.brave_sailors.ui.components.MosaicBackground
import com.example.brave_sailors.ui.components.NavigationBar
import com.example.brave_sailors.ui.components.NavigationItem
import com.example.brave_sailors.ui.components.Popup
import com.example.brave_sailors.ui.components.TertiaryButton
import com.example.brave_sailors.ui.theme.LightGrey
import com.example.brave_sailors.ui.theme.White
import com.example.brave_sailors.ui.utils.GameModeIcons
import com.example.brave_sailors.ui.utils.RememberScaleConversion
import com.example.brave_sailors.ui.utils.gameModes

@Composable
fun HomeScreen(
    innerPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: ProfileViewModel,
    onNavigateToTerms: () -> Unit
) {
    var currentScreen by remember { mutableStateOf(NavigationItem.Home) }
    var showDialogFlag by remember { mutableStateOf(false) }
    var showDialogName by remember { mutableStateOf(false) }
    val scale = RememberScaleConversion()

    // --- STATES ---
    var showGooglePopup by remember { mutableStateOf(false) }
    val flagsList by viewModel.flagList.collectAsState()
    val userState by viewModel.userState.collectAsState()


    val forceLogout by viewModel.forceLogoutEvent.collectAsState()

    // --- POPUP WELCOME LOGIC ---
    LaunchedEffect(Unit) {
        if (viewModel.showHomeWelcome) {
            showGooglePopup = true
        }
    }

    if (forceLogout) {
        AlertDialog(
            onDismissRequest = {  },
            title = { Text("Session Expired") },
            text = { Text("You have logged in on another device. You will be logged out from this device.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.performLocalLogout() // Pulisce DB
                        onNavigateToTerms() // Torna alla schermata iniziale
                    }
                ) {
                    Text("OK")
                }
            },
            containerColor = White,
            titleContentColor = Color.Black,
            textContentColor = Color.Black
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        MosaicBackground()

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(
                    currentScreen = currentScreen,
                    onItemClick = { currentScreen = it }
                )
            }
        ) { scaffoldPadding ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding),
                contentAlignment = Alignment.Center
            ) {
                when (currentScreen) {
                    NavigationItem.Home -> Modal()
                    NavigationItem.Profile -> ProfileScreen(
                        viewModel = viewModel,
                        onOpenChangeFlag = { showDialogFlag = true },
                        onOpenChangeName = { showDialogName = true }
                    )
                    NavigationItem.Menu -> MenuScreen()
                    NavigationItem.Fleet -> FleetScreen()
                    NavigationItem.Game -> {}
                }
            }
        }

        // --- CUSTOM POPUP "WELCOME BACK" ---
        if (showGooglePopup && userState != null) {
            val user = userState!!

            Popup(
                start = true,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = scale.dp(50f))
                    .zIndex(2f),
                avatar = {
                    // Usa i dati di backup Google (Immagine)
                    if (user.googlePhotoUrl.isNullOrEmpty() || user.googlePhotoUrl == "ic_terms") {
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
                            model = user.googlePhotoUrl,
                            placeholder = painterResource(id = R.drawable.ic_terms),
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
                            text = "Hello, ${user.googleName}",
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
                            text = user.email,
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

    if (showDialogFlag) {
        FlagSelectionDialog(
            availableFlags = flagsList,
            onDismiss = { showDialogFlag = false },
            onFlagSelected = { flag ->
                viewModel.updateCountry(flag.code)
                showDialogFlag = false
            }
        )
    }

    if (showDialogName) {
        DialogName(
            viewModel = viewModel,
            onDismiss = { showDialogName = false },
            onConfirm = { showDialogName = false }
        )
    }
}

// -- PLACEHOLDER SCREENS --
@Composable
fun FleetScreen() {  }

@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@Composable
private fun Modal() {
    val scale = RememberScaleConversion()
    val barHeight = 108f
    val maxWidth = scale.dp(720f)
    var currentMode by remember { mutableIntStateOf(0) }
    var isMovingForward by remember { mutableStateOf(true) }

    var totalDragDistance by remember { mutableFloatStateOf(0f) }
    var hasSwitchedThisGesture by remember { mutableStateOf(false) }

    fun changeMode(goForward: Boolean) {
        isMovingForward = goForward
        currentMode = if (goForward) (currentMode + 1) % gameModes.size else ((currentMode - 1) + gameModes.size) % gameModes.size
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { totalDragDistance = 0f; hasSwitchedThisGesture = false },
                    onDragEnd = { totalDragDistance = 0f; hasSwitchedThisGesture = false },
                    onDragCancel = { totalDragDistance = 0f; hasSwitchedThisGesture = false }
                ) { change, dragAmount ->
                    change.consume()
                    if (!hasSwitchedThisGesture) {
                        totalDragDistance += dragAmount
                        val threshold = scale.dp(24f).toPx()
                        if (totalDragDistance < -threshold) { changeMode(true); hasSwitchedThisGesture = true }
                        else if (totalDragDistance > threshold) { changeMode(false); hasSwitchedThisGesture = true }
                    }
                }
            }
    ) {
        val topOffset = (maxHeight / 2) + scale.dp(54f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = maxWidth)
                .align(Alignment.TopCenter)
                .padding(top = topOffset)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Top) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(scale.dp(barHeight))
                        .background(Color(0xFF26768E))
                        .padding(vertical = scale.dp(6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Arrow(isLeft = true, modifier = Modifier.align(Alignment.CenterStart), onClick = { changeMode(false) })
                    AnimatedContent(
                        targetState = gameModes[currentMode],
                        transitionSpec = {
                            val duration = 250
                            val slideIn = if (isMovingForward) slideInHorizontally(tween(duration)) { width -> width + (width / 2) } else slideInHorizontally(tween(duration)) { width -> -(width + (width / 2)) }
                            val slideOut = if (isMovingForward) slideOutHorizontally(tween(duration)) { width -> -width * 2 } else slideOutHorizontally(tween(duration)) { width -> width * 2 }
                            (slideIn + fadeIn(tween(duration))).togetherWith(slideOut + fadeOut(animationSpec = keyframes { durationMillis = duration; 1f at (duration * 0.5f).toInt(); 0f at duration }))
                                .using(SizeTransform(clip = false))
                        }, label = "TextAnimation"
                    ) { mode ->
                        Column(modifier = Modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                            Text(text = mode.title, color = White, textAlign = TextAlign.Center, fontSize = scale.sp(40f), fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, letterSpacing = scale.sp(2f), style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false), lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both), shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)))
                            Text(text = mode.sub, color = White, textAlign = TextAlign.Center, fontSize = scale.sp(28f), fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, letterSpacing = scale.sp(2f), style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false), lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both), shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)))
                        }
                    }
                    Arrow(isLeft = false, modifier = Modifier.align(Alignment.CenterEnd), onClick = { changeMode(true) })
                }
                AnimatedContent(
                    targetState = gameModes[currentMode],
                    transitionSpec = {
                        val duration = 250
                        val slideIn = if (isMovingForward) slideInHorizontally(tween(duration)) { width -> width + (width / 2) } else slideInHorizontally(tween(duration)) { width -> -(width + (width / 2)) }
                        val slideOut = if (isMovingForward) slideOutHorizontally(tween(duration)) { width -> -width * 2 } else slideOutHorizontally(tween(duration)) { width -> width * 2 }
                        (slideIn + fadeIn(tween(duration))).togetherWith(slideOut + fadeOut(animationSpec = keyframes { durationMillis = duration; 1f at (duration * 0.5f).toInt(); 0f at duration }))
                            .using(SizeTransform(clip = false))
                    }, label = "ButtonAnimation", modifier = Modifier.offset(y = scale.dp(12f))
                ) { targetMode ->
                    TertiaryButton(paddingH = 32f, paddingV = 16f, text = targetMode.button, onClick = { }, icons = { GameModeIcons() })
                }
            }
        }
    }
}