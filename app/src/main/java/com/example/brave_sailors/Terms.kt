package com.example.brave_sailors

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.example.brave_sailors.data.local.database.AppDatabase
import com.example.brave_sailors.data.local.database.entity.User
import com.example.brave_sailors.domain.use_case.OpenPrivacyPolicyUseCase
import com.example.brave_sailors.ui.components.*
import com.example.brave_sailors.ui.theme.*
import com.example.brave_sailors.ui.utils.RememberScaleConversion
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import kotlin.jvm.java

@Composable
fun TermsScreen(innerPadding: PaddingValues = PaddingValues(0.dp)) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(innerPadding)
    ) {
        // Center area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Blue),
            contentAlignment = Alignment.Center
        ) {
            Modal()
        }
    }
}

@Composable
private fun Modal() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // -- SCALE ( used for applying conversions )
    val scale = RememberScaleConversion()

    // [ MEMO ]: Sizes are taken from 720 x 1600px mockup ( with 72dpi ) using the Redmi Note 10S
    val boxShape = CutCornerShape(scale.dp(28f)) // 28px

    val maxWidth = scale.dp(648f) // 648px, etc.

    // -- STATE ( implemented to persist across recompositions )
    var privacyAccepted by rememberSaveable { mutableStateOf(false) }
    var privacyLaunched by rememberSaveable { mutableStateOf(false) }
    var showPopup by remember { mutableStateOf(false) }

    var isSignedIn by rememberSaveable { mutableStateOf(false) }
    var isSigningIn by remember { mutableStateOf(false) }

    var account by remember { mutableStateOf<GoogleSignInAccount?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    var playerName by remember { mutableStateOf("Player") }
    var playerPhoto by remember { mutableStateOf<String?>(null) }

    val openPrivacyPolicy = remember { OpenPrivacyPolicyUseCase() }
    val privacyUrl = "https://privacy-service-101333280904.europe-west1.run.app/privacy-policy"
    val userDao = remember(context) { AppDatabase.getDatabase(context).userDao() }

    val options = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
    }

    val googleSignInClient = remember(context, options) { GoogleSignIn.getClient(context, options) }

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isSigningIn = false

        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)

            try {
                account = task.getResult(ApiException::class.java)

                isSignedIn = true
                message = null

                playerName = account?.displayName ?: "Sailor"
                playerPhoto = account?.photoUrl?.toString() ?: "placeholder_avatar"

                account?.let { googleAccount ->
                    scope.launch {
                        val user = User(
                            id = googleAccount.id!!,
                            name = googleAccount.displayName ?: "Sailor",
                            email = googleAccount.email!!,
                            profilePictureUrl = googleAccount.photoUrl?.toString()
                        )
                        userDao.insertUser(user)
                    }
                }

                showPopup = true

            } catch (e: ApiException) {
                isSignedIn = false
                message = "Login failed: (${e.statusCode})"
            }
        } else {
            message = "Login cancelled."
        }
    }

    var didLogin by remember { mutableStateOf(false) }

    LaunchedEffect(didLogin) {
        if (!didLogin) {
            didLogin = true
            isSigningIn = true

            val last = GoogleSignIn.getLastSignedInAccount(context)

            if (last != null) {
                account = last
                isSignedIn = true
                isSigningIn = false

                playerName = last.displayName ?: "Sailor"
                playerPhoto = last.photoUrl?.toString()

                scope.launch {
                    val user = User(
                        id = last.id!!,
                        name = last.displayName ?: "Sailor",
                        email = last.email!!,
                        profilePictureUrl = last.photoUrl?.toString()
                    )
                    userDao.insertUser(user)
                }

                showPopup = true
            }
            else
                signInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    // [ NOTE ]: This block witnesses the app's state, keeping trace about the moment when it returns to foreground ( e.g., after the user reviews the "Privacy Policy" page )
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if ((event == Lifecycle.Event.ON_RESUME) && ((!privacyAccepted) && privacyLaunched))
                privacyAccepted = true
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        if (showPopup) {
            Popup(
                start = true,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = scale.dp(98f))
                    .zIndex(2f),
                avatar = {
                    AsyncImage(
                        model = if (playerPhoto.isNullOrEmpty() || playerPhoto == "placeholder_avatar")
                            R.drawable.ic_terms
                        else
                            playerPhoto,
                        contentDescription = "avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                },
                content = {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Hello, $playerName",
                            color = Color.Black,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            fontSize = scale.sp(22f),
                            letterSpacing = scale.sp(2f),
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
                        Spacer(modifier = Modifier.height(scale.dp(16f)))
                        Text(
                            text = account?.email ?: "",
                            color = LightGrey,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            fontSize = scale.sp(20f),
                            letterSpacing = scale.sp(2f),
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
                onDone = { showPopup = false }
            )
        }
    }

    Box(
        modifier = Modifier
            .widthIn(max = maxWidth),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                // borderStroke + ( HeaderTab's height / 2 ) = 2 + ( ( 32 + 32 + 32 ) / 2 ), also taking into account the size of its content
                .padding(top = scale.dp(50f))
                .fillMaxWidth()
                .background(DarkBlue, shape = boxShape)
                .border(BorderStroke(scale.dp(1f), Orange), shape = boxShape)
                .clip(boxShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .padding(all = scale.dp(28f))
                    .clip(RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                GridBackground(Modifier.matchParentSize(), color = LightBlue, 14f)

                Box(
                    modifier = Modifier
                        .padding(horizontal = scale.dp(48f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(scale.dp(146f)))

                        Text(
                            text = "To use this service, users must review the application's terms first. Minors have to obtain permission from their parents or legal guardians.",
                            color = White,
                            textAlign = TextAlign.Center,
                            fontSize = scale.sp(28f),
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            lineHeight = scale.sp(32f),
                            letterSpacing = scale.sp(2f),
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(
                                    includeFontPadding = false
                                ),
                                lineHeightStyle = LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Center,
                                    trim = LineHeightStyle.Trim.Both
                                ),
                                shadow = Shadow(
                                    color = Color.Black,
                                    offset = Offset(2f, 2f),
                                    blurRadius = 4f
                                )
                            )
                        )

                        Spacer(modifier = Modifier.height(scale.dp(114f)))

                        SecondaryButton(
                            paddingH = 54f,
                            paddingV = 22f,
                            text = "Privacy Policy",
                            onClick = {
                                privacyLaunched = true
                                openPrivacyPolicy(context, privacyUrl)
                            },
                            modifier = Modifier
                        )

                        Spacer(modifier = Modifier.height(scale.dp(82f)))

                        Text(
                            text = "By tapping \"START\", the customer formally agrees to these conditions.",
                            color = White,
                            textAlign = TextAlign.Center,
                            fontSize = scale.sp(20f),
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            lineHeight = scale.sp(24f),
                            letterSpacing = scale.sp(2f),
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(
                                    includeFontPadding = false
                                ),
                                lineHeightStyle = LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Center,
                                    trim = LineHeightStyle.Trim.Both
                                ),
                                shadow = Shadow(
                                    color = Color.Black,
                                    offset = Offset(2f, 2f),
                                    blurRadius = 4f
                                )
                            )
                        )

                        Spacer(modifier = Modifier.height(scale.dp(70f)))

                        PrimaryButton(
                            112f,
                            28f,
                            text = "START",
                            onClick = {},
                            enabled = privacyAccepted
                        )

                        Spacer(modifier = Modifier.height(scale.dp(80f)))
                    }
                }
            }
        }

        Box(modifier = Modifier.zIndex(1f)) {
            Tab(130f, 32f, text = "Consensus")
        }
    }
}