package com.example.brave_sailors

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.brave_sailors.ui.theme.*
import com.example.brave_sailors.ui.components.GridBackground
import com.example.brave_sailors.model.ProfileViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale

// Data class for flag management
data class Flag(val name: String, val code: String, val resourceId: Int)

val availableFlags = listOf(
    Flag("Italy", "IT", R.drawable.flag_italy),
    Flag("Spain", "ES", R.drawable.flag_spain),
    Flag("France", "FR", R.drawable.flag_france),
    Flag("United Kingdom", "GB", R.drawable.flag_uk),
    Flag("Belgium", "BE", R.drawable.flag_belgium),
    Flag("China", "CN", R.drawable.flag_china),
    Flag("Russia", "RU", R.drawable.flag_russia)

)

@Composable
fun ProfileScreen(
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: ProfileViewModel,
    onLeaderboardClick: () -> Unit = {},
    onStatsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    // Observing user state from DB (Room)
    val user by viewModel.userState.collectAsState()

    var showFlagDialog by remember { mutableStateOf(false) }

    // [NEW] State for Name Change Dialog
    var showNameDialog by remember { mutableStateOf(false) }

    var selectedFlag by remember { mutableStateOf(availableFlags.first()) }

    // --- 1. INITIAL SYNC DB -> UI ---
    LaunchedEffect(user) {
        user?.let { u ->
            val code = u.countryCode
            if (!code.isNullOrEmpty()) {
                val savedFlag = availableFlags.find { it.code.equals(code, ignoreCase = true) }
                if (savedFlag != null) selectedFlag = savedFlag
            }
        }
    }

    // --- 2. PERMISSION LAUNCHER (MULTIPLE) ---
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineGranted || coarseGranted) {
            detectCountryFromLocation(context) { countryCode ->
                val foundFlag = availableFlags.find { it.code.equals(countryCode, ignoreCase = true) }
                if (foundFlag != null) viewModel.updateCountry(foundFlag.code)
            }
        }
    }

    // --- 3. STARTUP CHECK ---
    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            detectCountryFromLocation(context) { countryCode ->
                val foundFlag = availableFlags.find { it.code.equals(countryCode, ignoreCase = true) }
                if (foundFlag != null) viewModel.updateCountry(foundFlag.code)
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // --- 4. CAMERA LAUNCHER ---
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) viewModel.updateProfilePicture(context, bitmap)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) cameraLauncher.launch(null)
    }

    // --- 5. DIALOGS ---

    // Flag Dialog
    if (showFlagDialog) {
        FlagSelectionDialog(
            onDismiss = { showFlagDialog = false },
            onFlagSelected = { flag ->
                viewModel.updateCountry(flag.code)
                showFlagDialog = false
            }
        )
    }

    // [NEW] Name Change Dialog
    if (showNameDialog) {
        ChangeNameDialog(
            currentName = user?.name ?: "",
            onDismiss = { showNameDialog = false },
            onNameConfirmed = { newName ->
                // Assicurati che questa funzione esista nel tuo ViewModel
                viewModel.updateName(newName)
                showNameDialog = false
            }
        )
    }

    // --- UI STRUCTURE ---
    Column(
        modifier = Modifier.fillMaxSize().padding(paddingValues).background(DeepBlue)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(LocalConfiguration.current.screenHeightDp.dp * 0.12f)) {
            BarPattern(color = White.copy(alpha = 0.05f))
        }

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().background(DarkBlue),
            contentAlignment = Alignment.TopCenter
        ) {
            GridBackground(Modifier.matchParentSize(), color = LightGrey.copy(alpha = 0.1f), dimension = 40f)

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(64.dp))

                // Username Row
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(modifier = Modifier.weight(1f), color = Blue, shape = RoundedCornerShape(4.dp)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.width(3.dp).height(20.dp).background(Orange))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(user?.name ?: "Loading...", color = White, fontSize = 16.sp)
                        }
                    }

                    // [UPDATED] Change Name Button
                    Button(
                        onClick = { showNameDialog = true },
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Grey)
                    ) {
                        Text("Change name", color = White)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Portrait & Flag
                Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ProfileCustomizationItem(
                        modifier = Modifier.weight(1f).clickable { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                        title = "Portrait"
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val path = user?.profilePictureUrl
                            if (!path.isNullOrEmpty()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(path)
                                        .crossfade(true)
                                        .setParameter("key", user?.lastUpdated ?: System.currentTimeMillis())
                                        .build(),
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.Person, null, modifier = Modifier.size(50.dp), tint = White)
                            }
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PhotoCamera, "Take Photo", tint = White.copy(alpha = 0.9f), modifier = Modifier.size(28.dp))
                            }
                        }
                    }

                    ProfileCustomizationItem(
                        modifier = Modifier.weight(1f).clickable { showFlagDialog = true },
                        title = "Flag"
                    ) {
                        Image(
                            painter = painterResource(selectedFlag.resourceId),
                            contentDescription = selectedFlag.name,
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            contentScale = ContentScale.Fit
                        )
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = White.copy(alpha = 0.5f), modifier = Modifier.padding(8.dp).size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileActionButton(Modifier.weight(1f), "Stats", Icons.Default.BarChart, onClick = onStatsClick)
                    ProfileActionButton(Modifier.weight(1f), "Rank", Icons.Default.EmojiEvents, onClick = onLeaderboardClick)
                    ProfileActionButton(Modifier.weight(1f), "Fleet", Icons.Default.ViewModule)
                }
            }
            HeaderTab(text = "PROFILE")
        }

        Box(modifier = Modifier.fillMaxWidth().height(LocalConfiguration.current.screenHeightDp.dp * 0.12f)) {
            BarPattern(color = White.copy(alpha = 0.05f))
        }
    }
}

// --- ROBUST LOCATION LOGIC ---

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
                    } else {
                        android.widget.Toast.makeText(context, "Could not find location. Activate GPS!", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener {
                    it.printStackTrace()
                    android.widget.Toast.makeText(context, "Error GPS: ${it.message}", android.widget.Toast.LENGTH_LONG).show()
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

// --- UI COMPONENTS ---

@Composable fun HeaderTab(text: String) {
    Surface(color = Orange, shape = CutCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp), modifier = Modifier.zIndex(2f)) {
        Text(text, Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = DeepBlue, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable fun ProfileCustomizationItem(modifier: Modifier, title: String, content: @Composable () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title.uppercase(), color = LightGrey, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Surface(Modifier.fillMaxWidth().height(120.dp), shape = CutCornerShape(12.dp), color = Blue, border = BorderStroke(1.dp, TransparentGrey)) {
            Box(contentAlignment = Alignment.Center) { content() }
        }
    }
}

@Composable fun ProfileActionButton(modifier: Modifier, title: String, icon: ImageVector, onClick: () -> Unit = {}) {
    Surface(modifier.height(90.dp).clickable { onClick() }, shape = CutCornerShape(8.dp), color = Blue, border = BorderStroke(1.dp, TransparentGrey)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = Orange, modifier = Modifier.size(28.dp))
            Text(title, color = White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun FlagSelectionDialog(onDismiss: () -> Unit, onFlagSelected: (Flag) -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp), shape = CutCornerShape(12.dp), color = Blue, border = BorderStroke(1.dp, TransparentGrey)) {
            Column {
                Text("SELECT FLAG", Modifier.padding(16.dp), color = White, fontWeight = FontWeight.Bold)
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    items(availableFlags) { flag ->
                        Row(Modifier.fillMaxWidth().clickable { onFlagSelected(flag) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Image(painterResource(flag.resourceId), null, Modifier.size(40.dp, 25.dp))
                            Spacer(Modifier.width(16.dp))
                            Text(flag.name, color = White)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onNameConfirmed: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = CutCornerShape(12.dp),
            color = Blue,
            border = BorderStroke(1.dp, TransparentGrey)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("CHANGE NAME", color = White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 20) name = it },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = White,
                        unfocusedTextColor = White,
                        focusedContainerColor = DeepBlue,
                        unfocusedContainerColor = DeepBlue,
                        cursorColor = Orange,
                        focusedIndicatorColor = Orange,
                        unfocusedIndicatorColor = LightGrey,
                        focusedLabelColor = Orange,
                        unfocusedLabelColor = LightGrey
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = TransparentGrey),
                        shape = CutCornerShape(4.dp)
                    ) {
                        Text("CANCEL", color = White)
                    }
                    Button(
                        onClick = { if (name.isNotBlank()) onNameConfirmed(name) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Orange),
                        shape = CutCornerShape(4.dp)
                    ) {
                        Text("SAVE", color = DeepBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable fun BarPattern(color: Color) {
    val density = LocalDensity.current
    Canvas(Modifier.fillMaxSize()) {
        val step = with(density) { 20.dp.toPx() }
        var x = 0f
        while (x < size.width + size.height) {
            drawLine(color, Offset(x, 0f), Offset(x - size.height, size.height), strokeWidth = with(density) { 10.dp.toPx() })
            x += step
        }
    }
}