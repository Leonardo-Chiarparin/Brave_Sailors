package com.example.brave_sailors

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import coil.compose.rememberAsyncImagePainter
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

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // ---------------- GOOGLE SIGN-IN OPTIONS ----------------

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
    }

    val googleSignInClient = remember(context, gso) {
        GoogleSignIn.getClient(context, gso)
    }

    // ---------------- LOGIN RISULTATO ----------------

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

                Log.d("GOOGLE", "Nome: ${acc.givenName}")
                Log.d("GOOGLE", "Cognome: ${acc.familyName}")
                Log.d("GOOGLE", "Email: ${acc.email}")
                Log.d("GOOGLE", "Avatar: ${acc.photoUrl}")

            } catch (e: ApiException) {
                isSignedIn = false
                errorMessage = "Google login failed (${e.statusCode})"
            }
        } else {
            errorMessage = "Login cancelled."
        }
    }

    // ---------------- FUNZIONE LOGIN GOOGLE ----------------

    fun signIn() {
        isSigningIn = true

        val last = GoogleSignIn.getLastSignedInAccount(context)
        if (last != null) {
            account = last
            isSignedIn = true
            isSigningIn = false
            return
        }

        signInLauncher.launch(googleSignInClient.signInIntent)
    }

    // ---------------- LOGIN AUTOMATICO ----------------
    LaunchedEffect(Unit) { signIn() }

    // ---------------- HANDLE RETURN PRIVACY ----------------

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

    val startButtonAlpha by animateFloatAsState(
        if (privacyPolicyAccepted) 1f else 0.5f
    )

    // -------------------- UI --------------------

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

                    // ------------------- STATO LOGIN -------------------

                    when {
                        isSigningIn -> CircularProgressIndicator(color = Orange)

                        isSignedIn -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                                Text("Welcome,", color = White)

                                val fullName =
                                    listOfNotNull(account?.givenName, account?.familyName)
                                        .joinToString(" ")
                                        .ifBlank { account?.displayName ?: "User" }

                                val avatarUrl = account?.photoUrl?.toString()

                                Spacer(Modifier.height(16.dp))

                                // FOTO + NOME + EMAIL IN RIGA
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {


                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            model = avatarUrl,
                                            placeholder = painterResource(id = R.drawable.placeholder_avatar),
                                            error = painterResource(id = R.drawable.placeholder_avatar)
                                        ),
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

                        errorMessage != null ->
                            Text(errorMessage!!, color = Color.Red)
                    }

                    Spacer(Modifier.height(scale.dp(50f)))

                    PrimaryButton(
                        text = "START",
                        onClick = { /* navigate */ },
                        enabled = privacyPolicyAccepted,
                        disableEffects = true,
                        modifier = Modifier.alpha(startButtonAlpha)
                    )
                }
            }
        }

        Box(Modifier.zIndex(2f)) {
            Tab(text = "Consensus")
        }
    }
}
