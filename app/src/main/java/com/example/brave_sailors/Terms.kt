package com.example.brave_sailors

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.brave_sailors.data.local.database.AppDatabase
import com.example.brave_sailors.data.local.database.entity.User
import com.example.brave_sailors.domain.use_case.GoogleSigningUseCase
import com.example.brave_sailors.domain.use_case.OpenPrivacyPolicyUseCase
import com.example.brave_sailors.domain.use_case.SigningResult
import com.example.brave_sailors.model.ProfileViewModel
import com.example.brave_sailors.ui.components.*
import com.example.brave_sailors.ui.theme.*
import com.example.brave_sailors.ui.utils.RememberScaleConversion
import com.example.brave_sailors.ui.utils.findActivity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun TermsScreen(innerPadding: PaddingValues = PaddingValues(0.dp), viewModel: ProfileViewModel, onStartApp: () -> Unit) {
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
            Modal(viewModel = viewModel, onStartApp = onStartApp)
        }
    }
}

@Composable
private fun Modal(viewModel: ProfileViewModel, onStartApp: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // -- SCALE --
    val scale = RememberScaleConversion()
    val boxShape = CutCornerShape(scale.dp(28f))
    val maxWidth = scale.dp(648f)

    // -- STATE --
    var privacyAccepted by rememberSaveable { mutableStateOf(false) }
    var privacyLaunched by rememberSaveable { mutableStateOf(false) }
    var showPopup by remember { mutableStateOf(false) }

    // -- AUTHENTICATION --
    var isSignedIn by rememberSaveable { mutableStateOf(false) }
    var isSigningIn by remember { mutableStateOf(false) }

    var playerEmail by remember { mutableStateOf("") }
    var playerName by remember { mutableStateOf("Sailor") }
    var playerPhoto by remember { mutableStateOf<String?>(null) }

    var pendingUser by remember { mutableStateOf<User?>(null) }
    var isNewUser by remember { mutableStateOf(false) }

    // Detected Country State
    var detectedCountryCode by remember { mutableStateOf<String?>(null) }

    val openPrivacyPolicy = remember { OpenPrivacyPolicyUseCase() }
    val privacyUrl = stringResource(R.string.privacy_policy)
    val webClientID = stringResource(R.string.web_client)

    val googleSigning = remember(context) {
        val db = AppDatabase.getDatabase(context)
        GoogleSigningUseCase(db.userDao(), webClientID)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineGranted || coarseGranted) {
            detectCountryFromLocation(context) { countryCode ->
                detectedCountryCode = countryCode.uppercase()
            }
        }
    }

    // --- REAZIONE AL GPS ---
    // Appena viene rilevata una nazione, aggiorniamo i dati in tempo reale
    LaunchedEffect(detectedCountryCode) {
        if (detectedCountryCode != null) {
            Log.d("GPS", "Nazione rilevata: $detectedCountryCode")

            // 1. Se stiamo aspettando di cliccare START (Nuovo utente), aggiorniamo l'oggetto in memoria
            if (pendingUser != null) {
                pendingUser = pendingUser!!.copy(countryCode = detectedCountryCode!!)
            }

            // 2. Se l'utente è già loggato e visibile (Utente Esistente), aggiorniamo subito il DB
            if (isSignedIn && !isNewUser) {
                viewModel.updateCountry(detectedCountryCode!!)
            }
        }
    }

    fun performLogin() {
        if (!isSigningIn) {
            isSigningIn = true

            scope.launch {
                when (val result = googleSigning(context)) {
                    is SigningResult.Success -> {
                        val user = result.user
                        playerName = user.name
                        playerEmail = user.email
                        playerPhoto = user.profilePictureUrl

                        // Se il GPS ha già finito, applichiamo subito la nazione
                        val userWithCountry = if (detectedCountryCode != null) {
                            user.copy(countryCode = detectedCountryCode!!)
                        } else {
                            user
                        }

                        pendingUser = userWithCountry
                        isNewUser = result.isNewUser

                        if (!isNewUser) {
                            // Utente esistente: carichiamo subito
                            viewModel.loadUser(user.id)
                            // Se abbiamo una nazione rilevata, aggiorniamo il DB per sicurezza
                            if (detectedCountryCode != null) {
                                viewModel.updateCountry(detectedCountryCode!!)
                            }
                            viewModel.showHomeWelcome = true
                        } else {
                            // Nuovo utente: mostriamo popup
                            showPopup = true
                        }

                        isSignedIn = true
                        isSigningIn = false
                    }

                    is SigningResult.Cancelled -> {
                        Log.d("Auth", "Login cancelled.")
                        delay(500)
                        isSigningIn = false
                        performLogin()
                    }

                    is SigningResult.Error -> {
                        Log.e("Auth", "Login error: ${result.msg}")
                        delay(500)
                        isSigningIn = false
                        context.findActivity()?.recreate()
                    }
                }
            }
        }
    }

    var didLogin by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!didLogin) {
            didLogin = true
            performLogin()
        }

        // Location Check
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            detectCountryFromLocation(context) { countryCode ->
                detectedCountryCode = countryCode.uppercase()
            }
        } else {
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

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

    Box(modifier = Modifier.fillMaxSize().graphicsLayer(), contentAlignment = Alignment.TopCenter) {
        if (showPopup) {
            Popup(
                start = true,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = scale.dp(98f)).zIndex(2f),
                avatar = {
                    if (playerPhoto.isNullOrEmpty() || playerPhoto == "ic_terms") {
                        Image(painter = painterResource(id = R.drawable.ic_terms), contentDescription = "avatar", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                    } else {
                        AsyncImage(model = playerPhoto, placeholder = painterResource(id = R.drawable.ic_terms), error = painterResource(id = R.drawable.ic_terms), contentDescription = "avatar", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                    }
                },
                content = {
                    Column(modifier = Modifier.fillMaxHeight().weight(1f, fill = false), verticalArrangement = Arrangement.Center) {
                        Text(text = "Hello, $playerName", color = Color.Black, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = scale.sp(22f), letterSpacing = scale.sp(2f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false), lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.Both)))
                        Spacer(modifier = Modifier.height(scale.dp(8f)))
                        Text(text = playerEmail, color = LightGrey, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = scale.sp(20f), letterSpacing = scale.sp(2f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false), lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.Both)))
                    }
                },
                onDone = { showPopup = false }
            )
        }
    }

    Box(modifier = Modifier.widthIn(max = maxWidth).graphicsLayer(), contentAlignment = Alignment.TopCenter) {
        Box(modifier = Modifier.padding(top = scale.dp(50f)).fillMaxWidth().background(DarkBlue, shape = boxShape).border(BorderStroke(scale.dp(1f), Orange), shape = boxShape).clip(boxShape), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.padding(all = scale.dp(28f)).clip(RectangleShape), contentAlignment = Alignment.Center) {
                GridBackground(Modifier.matchParentSize(), color = LightBlue, 14f)
                Box(modifier = Modifier.padding(horizontal = scale.dp(48f)), contentAlignment = Alignment.Center) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(scale.dp(146f)))
                        Text(text = "To use this service, users must review the application's terms first. Minors have to obtain permission from their parents or legal guardians.", color = White, textAlign = TextAlign.Center, fontSize = scale.sp(28f), fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, lineHeight = scale.sp(32f), letterSpacing = scale.sp(2f), style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false), lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.Both), shadow = Shadow(color = Color.Black, offset = Offset(2f, 2f), blurRadius = 4f)))
                        Spacer(modifier = Modifier.height(scale.dp(114f)))
                        SecondaryButton(paddingH = 54f, paddingV = 22f, text = "Privacy policy", onClick = { privacyLaunched = true; openPrivacyPolicy(context, privacyUrl) }, modifier = Modifier, enabled = isSignedIn)
                        Spacer(modifier = Modifier.height(scale.dp(82f)))
                        Text(text = "By tapping \"START\", the customer formally agrees to these conditions.", color = White, textAlign = TextAlign.Center, fontSize = scale.sp(20f), fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, lineHeight = scale.sp(24f), letterSpacing = scale.sp(2f), style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false), lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.Both), shadow = Shadow(color = Color.Black, offset = Offset(2f, 2f), blurRadius = 4f)))
                        Spacer(modifier = Modifier.height(scale.dp(70f)))

                        PrimaryButton(
                            112f,
                            28f,
                            text = "START",
                            onClick = {
                                if (isNewUser) {
                                    // Se l'utente è nuovo, usa pendingUser (aggiornato dal LaunchedEffect se il GPS è arrivato)
                                    pendingUser?.let { user ->
                                        val finalUser = if (detectedCountryCode != null) {
                                            user.copy(countryCode = detectedCountryCode!!)
                                        } else user

                                        viewModel.registerUser(finalUser)
                                        viewModel.loadUser(finalUser.id)
                                    }
                                } else {
                                    // Se l'utente esiste già, assicuriamoci di aggiornare la nazione se il GPS l'ha trovata ora
                                    if (detectedCountryCode != null) {
                                        viewModel.updateCountry(detectedCountryCode!!)
                                    }
                                }
                                onStartApp()
                            },
                            enabled = privacyAccepted
                        )

                        Spacer(modifier = Modifier.height(scale.dp(80f)))
                    }
                }
            }
        }
        Box(modifier = Modifier.zIndex(1f)) { Tab(130f, 32f, text = "Consensus") }
    }
}

// Helper Functions
@SuppressLint("MissingPermission")
fun detectCountryFromLocation(context: Context, onCountryFound: (String) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val cancelTokenSource = CancellationTokenSource()

    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        if (location != null) {
            decodeAndNotify(context, location, onCountryFound)
        } else {
            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val priority = if (hasFine) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY

            fusedLocationClient.getCurrentLocation(priority, cancelTokenSource.token)
                .addOnSuccessListener { newLocation ->
                    if (newLocation != null) {
                        decodeAndNotify(context, newLocation, onCountryFound)
                    }
                }
        }
    }
}

fun decodeAndNotify(context: Context, location: android.location.Location, onCountryFound: (String) -> Unit) {
    try {
        val geocoder = Geocoder(context, Locale.getDefault())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                if (addresses.isNotEmpty()) onCountryFound(addresses[0].countryCode)
            }
        } else {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (!addresses.isNullOrEmpty()) onCountryFound(addresses[0].countryCode)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}