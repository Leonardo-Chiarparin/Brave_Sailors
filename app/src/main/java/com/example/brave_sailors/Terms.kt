package com.example.brave_sailors

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.brave_sailors.domain.use_case.OpenPrivacyPolicyUseCase
import com.example.brave_sailors.ui.components.GridBackground
import com.example.brave_sailors.ui.components.PrimaryButton
import com.example.brave_sailors.ui.components.SecondaryButton
import com.example.brave_sailors.ui.components.Tab
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

    // ---- STATE ----
    var privacyPolicyAccepted by rememberSaveable { mutableStateOf(false) }
    var privacyLaunched by rememberSaveable { mutableStateOf(false) }

    // --- NEW STATES FOR AUTHENTICATION HANDLING ---
    var isSignedIn by rememberSaveable { mutableStateOf(false) }
    var isSigningIn by remember { mutableStateOf(false) }
    var signedInAccount by remember { mutableStateOf<GoogleSignInAccount?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val startButtonAlpha by animateFloatAsState(
        targetValue = if (privacyPolicyAccepted) 1f else 0.5f,
        label = "startBtnAlpha"
    )

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope() // To launch coroutines

    // ---- GOOGLE SIGN-IN SETUP ----
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .build()
    }

    val googleSignInClient = remember(context, gso) {
        GoogleSignIn.getClient(context, gso)
    }

    // --- RESULT HANDLING WITH COROUTINES ---
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("TermsScreen", "Sign-in result received. Result code: ${result.resultCode}")
        isSigningIn = false // The login operation (UI) is finished
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("TermsScreen", "Sign-in successful. Data: ${result.data}")
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                // Success! We get the account and update the state
                val account = task.getResult(ApiException::class.java)
                Log.d("TermsScreen", "Account retrieved: ${account?.displayName}")
                signedInAccount = account
                isSignedIn = true
                errorMessage = null // Clear any previous errors
            } catch (e: ApiException) {
                // Error: authentication failed
                Log.e("TermsScreen", "Sign-in failed with ApiException", e)
                errorMessage = "Sign-in failed. (Code: ${e.statusCode})"
                isSignedIn = false
            }
        } else {
            // The user cancelled the operation
            Log.w("TermsScreen", "Sign-in cancelled by user or failed. Result code: ${result.resultCode}")
            errorMessage = "Sign-in cancelled."
        }
    }

    // --- SIGN-IN FUNCTION REWRITTEN WITH COROUTINES ---
    fun signIn() {
        Log.d("TermsScreen", "signIn function called")
        coroutineScope.launch {
            isSigningIn = true // Show the loading indicator
            try {
                val account = GoogleSignIn.getLastSignedInAccount(context)
                if (account == null) {
                    Log.d("TermsScreen", "No account found, launching sign-in intent")
                    // No user already logged in, launch the login UI
                    signInLauncher.launch(googleSignInClient.signInIntent)
                } else {
                    // User already logged in! Update the state immediately.
                    Log.d("TermsScreen", "User already signed in: ${account.displayName}")
                    signedInAccount = account
                    isSignedIn = true
                    isSigningIn = false
                }
            } catch (e: Exception) {
                isSigningIn = false
                Log.e("TermsScreen", "An unexpected error occurred in signIn", e)
                errorMessage = "An unexpected error occurred."
            }
        }
    }

    // Effect to automatically sign in when the composable is first displayed
    LaunchedEffect(Unit) {
        signIn()
    }

    // Effect to observe the return from the Privacy Policy
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && privacyLaunched) {
                privacyPolicyAccepted = true
                privacyLaunched = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
            Box(modifier = Modifier.padding(all = scale.dp(28f))) {
                GridBackground(Modifier.matchParentSize(), color = LightBlue, 14f)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = scale.dp(48f)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ... (Policy text, etc. remain unchanged)
                    Spacer(modifier = Modifier.height(scale.dp(146f)))

                    Text(
                        text = "To use this service, users must review the following terms. Minors are requested to obtain permission from their parents or guardians.",
                        color = White,
                        textAlign = TextAlign.Center,
                        fontSize = scale.sp(26f),
                        fontWeight = FontWeight.Medium,
                        lineHeight = scale.sp(32f),
                    )

                    Spacer(modifier = Modifier.height(scale.dp(114f)))

                    val privacyUrl = "https://privacy-service-101333280904.europe-west1.run.app/privacy-policy"

                    SecondaryButton(
                        text = "Privacy Policy",
                        onClick = {
                            privacyLaunched = true
                            openPrivacyPolicy(context, privacyUrl)
                        }
                    )

                    Spacer(modifier = Modifier.height(scale.dp(82f)))

                    Text(
                        text = "By proceeding, a customer formally attests to their comprehension of such conditions.",
                        color = White,
                        textAlign = TextAlign.Center,
                        fontSize = scale.sp(20f),
                        fontWeight = FontWeight.Medium,
                        lineHeight = scale.sp(24f),
                    )

                    Spacer(modifier = Modifier.height(scale.dp(70f)))

                    // --- LOGIN STATUS (non sostituisce più il pulsante START) ---
                    if (isSigningIn) {
                        CircularProgressIndicator(color = Orange)
                    } else if (isSignedIn) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Welcome,", color = White)
                            Text(
                                signedInAccount?.displayName ?: "Sailor",
                                color = Orange,
                                fontWeight = FontWeight.Bold,
                                fontSize = scale.sp(24f)
                            )
                        }
                    } else if (errorMessage != null) {
                        Text(errorMessage!!, color = Color.Red)
                    }

                    Spacer(Modifier.height(scale.dp(46f)))

                    PrimaryButton(
                        text = "START",
                        onClick = { /* navigazione reale */ },
                        enabled = privacyPolicyAccepted,
                        disableEffects = true,
                        modifier = Modifier.alpha(startButtonAlpha)
                    )

                    Spacer(modifier = Modifier.height(scale.dp(74f)))
                }
            }
        }
        Box(modifier = Modifier.zIndex(2f)) {
            Tab(text = "Consensus")
        }
    }
}
