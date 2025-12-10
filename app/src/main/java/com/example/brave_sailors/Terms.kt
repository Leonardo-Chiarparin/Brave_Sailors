package com.example.brave_sailors

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
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

    // GOOGLE SIGN-IN OPTIONS
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
    }

    val googleSignInClient = remember(context, gso) {
        GoogleSignIn.getClient(context, gso)
    }

    // RISULTATO LOGIN
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
                playerPhoto = acc?.photoUrl?.toString()

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

    // FUNZIONE LOGIN
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

    // RETURN PRIVACY
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && privacyLaunched) {
                privacyPolicyAccepted = true
                privacyLaunched = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    // UI
    Box(
        modifier = Modifier.widthIn(max = maxWidth),
        contentAlignment = Alignment.TopCenter
    ) {
        PlayGamesWelcomePopup(
            name = playerName,
            photoUrl = playerPhoto,
            visible = showPopup,
            onDismiss = { showPopup = false }
        )

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

                    Spacer(Modifier.height(scale.dp(60f)))

                    SecondaryButton(
                        text = "Privacy Policy",
                        onClick = {
                            privacyLaunched = true
                            openPrivacyPolicy(
                                context,
                                "https://privacy-service-101333280904.europe-west1.run.app/privacy-policy"
                            )
                        }
                    )

                    Spacer(Modifier.height(scale.dp(60f)))

                    when {
                        isSigningIn -> {
                            CircularProgressIndicator(color = Orange)
                        }
                        isSignedIn -> {
                            val fullName =
                                listOfNotNull(account?.givenName, account?.familyName)
                                    .joinToString(" ")
                                    .ifBlank { account?.displayName ?: "User" }

                            val avatarUrl = account?.photoUrl?.toString()

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Welcome,", color = White)
                                Spacer(Modifier.height(16.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    AsyncImage(
                                        model = avatarUrl,
                                        placeholder = painterResource(id = R.drawable.placeholder_avatar),
                                        error = painterResource(id = R.drawable.placeholder_avatar),
                                        contentDescription = "avatar",
                                        modifier = Modifier
                                            .size(70.dp)
                                            .clip(CutCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )

                                    Spacer(Modifier.width(16.dp))

                                    Column(horizontalAlignment = Alignment.Start) {
                                        Text(
                                            text = fullName,
                                            color = Orange,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = scale.sp(26f)
                                        )
                                        Text(
                                            text = account?.email ?: "",
                                            color = White,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = scale.sp(18f)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                        errorMessage != null -> {
                            Text(errorMessage!!, color = Color.Red)
                        }
                    }

                    Spacer(Modifier.height(scale.dp(50f)))

                    PrimaryButton(
                        text = "START",
                        onClick = { },
                        enabled = privacyPolicyAccepted
                    )
                }
            }
        }

        Box(Modifier.zIndex(2f)) {
            Tab(text = "Consensus")
        }
    }
}
