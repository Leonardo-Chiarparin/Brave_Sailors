package com.example.brave_sailors

import android.Manifest
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // <--- FONDAMENTALE PER LA LISTA
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign // <--- AGGIUNTO: Risolve l'errore TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.brave_sailors.R
import com.example.brave_sailors.ui.theme.*
import com.example.brave_sailors.ui.components.GridBackground
import com.example.brave_sailors.ui.model.ProfileViewModel

// Data class locale
data class Flag(val name: String, val resourceId: Int)

val availableFlags = listOf(
    Flag("Italy", R.drawable.flag_italy),
    Flag("Spain", R.drawable.flag_spain),
    Flag("France", R.drawable.flag_france),
)

@Composable
fun ProfileScreen(
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: ProfileViewModel
) {
    val context = LocalContext.current
    val user by viewModel.userState.collectAsState()

    var showFlagDialog by remember { mutableStateOf(false) }
    var selectedFlag by remember { mutableStateOf(availableFlags.first()) }

    // Launcher Fotocamera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let { viewModel.updateProfilePicture(context, it) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) cameraLauncher.launch()
    }

    if (showFlagDialog) {
        FlagSelectionDialog(
            onDismiss = { showFlagDialog = false },
            onFlagSelected = { flag -> selectedFlag = flag; showFlagDialog = false }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(paddingValues).background(DeepBlue)
    ) {
        // Barra Superiore
        Box(modifier = Modifier.fillMaxWidth().height(LocalConfiguration.current.screenHeightDp.dp * 0.12f)) {
            BarPattern(color = White.copy(alpha = 0.05f))
        }

        // Area Centrale
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

                // Riga Nome Utente
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(modifier = Modifier.weight(1f), color = Blue, shape = RoundedCornerShape(4.dp)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.width(3.dp).height(20.dp).background(Orange))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(user?.name ?: "Loading...", color = White, fontSize = 16.sp)
                        }
                    }
                    Button(onClick = { }, shape = RoundedCornerShape(4.dp), colors = ButtonDefaults.buttonColors(containerColor = Grey)) {
                        Text("Change name", color = White)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Portrait e Flag
                Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(16.dp)) {

                    // --- ITEM PORTRAIT ---
                    ProfileCustomizationItem(
                        modifier = Modifier.weight(1f).clickable {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        title = "Portrait"
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            // 1. IMMAGINE DI SFONDO
                            val path = user?.profilePictureUrl
                            if (!path.isNullOrEmpty()) {
                                AsyncImage(
                                    // TRUCCO: Usiamo 'lastUpdated' come firma.
                                    // Se il timestamp cambia, Coil ricarica l'immagine anche se il nome file è uguale.
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

                            // 2. ICONA FOTOCAMERA SOVRAPPOSTA (Overlay)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f)), // Velo scuro leggero
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Tap to take photo",
                                    tint = White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    // --- ITEM FLAG ---
                    ProfileCustomizationItem(
                        modifier = Modifier.weight(1f).clickable { showFlagDialog = true },
                        title = "Flag"
                    ) {
                        Image(painterResource(selectedFlag.resourceId), null, Modifier.fillMaxSize().padding(16.dp), contentScale = ContentScale.Fit)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Pulsanti Azione
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileActionButton(Modifier.weight(1f), "Stats", Icons.Default.BarChart)
                    ProfileActionButton(Modifier.weight(1f), "Rank", Icons.Default.EmojiEvents)
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

// --- COMPONENTI UI ---
@Composable fun HeaderTab(text: String) {
    Surface(color = Orange, shape = CutCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp), modifier = Modifier.zIndex(2f)) {
        Text(text, Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = DeepBlue, fontWeight = FontWeight.ExtraBold)
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

@Composable fun ProfileCustomizationItem(modifier: Modifier, title: String, content: @Composable () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title.uppercase(), color = LightGrey, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Surface(Modifier.fillMaxWidth().height(120.dp), shape = CutCornerShape(12.dp), color = Blue, border = BorderStroke(1.dp, TransparentGrey)) {
            Box(contentAlignment = Alignment.Center) { content() }
        }
    }
}

@Composable fun ProfileActionButton(modifier: Modifier, title: String, icon: ImageVector) {
    Surface(modifier.height(90.dp).clickable {}, shape = CutCornerShape(8.dp), color = Blue, border = BorderStroke(1.dp, TransparentGrey)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = Orange, modifier = Modifier.size(28.dp))
            // FIX: Ora TextAlign è importato e non darà errore
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
                    // FIX: Grazie all'import androidx.compose.foundation.lazy.items, questo ora funziona
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