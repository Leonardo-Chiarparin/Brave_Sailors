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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
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

@Composable
fun TermsScreen(innerPadding: PaddingValues = PaddingValues(0.dp)) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(innerPadding)
    ) {
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
    val scale = RememberScaleConversion()
    val boxShape = CutCornerShape(scale.dp(28f))
    val maxWidth = scale.dp(648f)
    val openPrivacyPolicy = remember { OpenPrivacyPolicyUseCase() }

    var privacyPolicyAccepted by rememberSaveable { mutableStateOf(false) }
    var privacyLaunched by rememberSaveable { mutableStateOf(false) }

    val privacyUrl = "https://privacy-service-101333280904.europe-west1.run.app/privacy-policy"

    var isSignedIn by rememberSaveable { mutableStateOf(false) }
    var isSigningIn by remember { mutableStateOf(false) }

    var account by remember { mutableStateOf<GoogleSignInAccount?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showPopup by remember { mutableStateOf(false) }
    var playerName by remember { mutableStateOf("Player") }
    var playerPhoto by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val userDao = remember(context) { AppDatabase.getDatabase(context).userDao() }

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
    }

    val googleSignInClient = remember(context, gso) {
        GoogleSignIn.getClient(context, gso)
    }

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isSigningIn = false

        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)

            try {
                val acc = task.getResult(ApiException::class.java)

                account = acc
                isSignedIn = true
                errorMessage = null

                playerName = acc?.displayName ?: "Sailor"
                playerPhoto = acc?.photoUrl?.toString() ?: "placeholder_avatar"


                acc?.let { googleAccount ->
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
                errorMessage = "Google login failed (${e.statusCode})"
            }
        } else {
            errorMessage = "Login cancelled."
        }
    }

    fun signIn() {
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
            return
        }

        signInLauncher.launch(googleSignInClient.signInIntent)
    }

    var didLogin by remember { mutableStateOf(false) }

    LaunchedEffect(didLogin) {
        if (!didLogin) {
            didLogin = true
            signIn()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if ((event == Lifecycle.Event.ON_RESUME) && ((!privacyPolicyAccepted) && privacyLaunched)) {
                privacyPolicyAccepted = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter){
        if (showPopup) {
            Popup(
                start = showPopup,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = scale.dp(98f))
                    .zIndex(2f),
                avatar = {
                    AsyncImage(
                        model = if (playerPhoto.isNullOrEmpty() || playerPhoto == "placeholder_avatar")
                                R.drawable.placeholder_avatar
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
        modifier = Modifier.widthIn(max = maxWidth),
        contentAlignment = Alignment.TopCenter
    ) {

        Box(
            modifier = Modifier
                .padding(top = scale.dp(50f))
                .fillMaxWidth()
                .background(DarkBlue, shape = boxShape)
                .border(BorderStroke(scale.dp(1f), Orange), shape = boxShape)
                .clip(boxShape),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.padding(scale.dp(28f))) {
                GridBackground(Modifier.matchParentSize(), LightBlue, 14f)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(scale.dp(120f)))

                    Text(
                        "To use this service, users must review the following terms.",
                        color = White,
                        textAlign = TextAlign.Center,
                        fontSize = scale.sp(26f)
                    )

                    Spacer(Modifier.height(scale.dp(114f)))

                    SecondaryButton(
                        paddingH = 54f,
                        paddingV = 22f,
                        text = "Privacy Policy",
                        onClick = {
                            privacyLaunched = true
                            openPrivacyPolicy(
                                context,
                                privacyUrl
                            )
                        },
                        modifier = Modifier
                    )

                    Spacer(Modifier.height(scale.dp(82f)))


                    Spacer(Modifier.height(scale.dp(50f)))

                    PrimaryButton(
                        paddingH = 112f,
                        paddingV = 28f,
                        text = "START",
                        onClick = { },
                        enabled = privacyPolicyAccepted
                    )
                }
            }
        }
        Box(Modifier.zIndex(1f)) {
            Tab(paddingH = 130f, paddingV = 32f, text = "Consensus")
        }
    }
}
