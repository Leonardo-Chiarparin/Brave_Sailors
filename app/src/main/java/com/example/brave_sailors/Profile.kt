package com.example.brave_sailors

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.brave_sailors.model.ProfileViewModel
import com.example.brave_sailors.ui.components.FifthButton
import com.example.brave_sailors.ui.components.GridBackground
import com.example.brave_sailors.ui.components.Profile
import com.example.brave_sailors.ui.components.SecondaryButton
import com.example.brave_sailors.ui.components.SeventhButton
import com.example.brave_sailors.ui.components.SixthButton
import com.example.brave_sailors.ui.components.Tab
import com.example.brave_sailors.ui.theme.DarkBlue
import com.example.brave_sailors.ui.theme.LightBlue
import com.example.brave_sailors.ui.theme.LightGrey
import com.example.brave_sailors.ui.theme.Orange
import com.example.brave_sailors.ui.theme.White
import com.example.brave_sailors.ui.utils.RememberScaleConversion
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.delay
import java.util.Locale

data class Flag(val name: String, val code: String, val resourceId: Int)

val availableFlags = listOf(
    Flag("Italy", "IT", R.drawable.ic_flag_italy),
    Flag("Spain", "ES", R.drawable.ic_flag_spain),
    Flag("France", "FR", R.drawable.ic_flag_france),
    Flag("United Kingdom", "GB", R.drawable.ic_flag_uk),
    Flag("Belgium", "BE", R.drawable.ic_flag_belgium),
    Flag("China", "CN", R.drawable.ic_flag_china),
    Flag("Russia", "RU", R.drawable.ic_flag_russia)
)

@Composable
fun ProfileScreen(viewModel: ProfileViewModel, onOpenChangeFlag: () -> Unit, onOpenChangeName: () -> Unit) {
    Modal(viewModel, onOpenChangeFlag, onOpenChangeName)
}

@Composable
private fun Modal(viewModel: ProfileViewModel, onOpenChangeFlag: () -> Unit, onOpenChangeName: () -> Unit) {
    val scale = RememberScaleConversion()
    val maxWidth = scale.dp(720f)
    var isVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var showFlagDialog by remember { mutableStateOf(false) }

    // [NEW] State for Name Change Dialog
    var showNameDialog by remember { mutableStateOf(false) }

    var selectedFlag by remember { mutableStateOf(availableFlags.first()) }

    // --- LOGIC INTEGRATION START ---

    // 1. Observing user state from DB
    val user by viewModel.userState.collectAsState()

    // 2. Camera Logic
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

    val onTakePhotoClick = {
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(null)
        } else {
            cameraPermissionLauncher.launch(permission)
        }
    }

    // 3. Location Logic (For Flag)
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

    LaunchedEffect(Unit) {
        delay(550.toLong()) // barAnimation + contentBarAnimation
        isVisible = true

        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            detectCountryFromLocation(context) { countryCode ->
                val foundFlag = availableFlags.find { it.code.equals(countryCode, ignoreCase = true) }
                if (foundFlag != null) viewModel.updateCountry(foundFlag.code)
            }
        } else {
            // Optional: Uncomment to ask permission automatically on start
            // locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = maxWidth) // [ MEMO ]: Remove it if not necessary
                .padding(top = scale.dp(208f))
        ) {
            Column(
                modifier = Modifier
                    .graphicsLayer(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(durationMillis = 300)
                    ) + fadeIn(animationSpec = tween(300)),
                    exit = slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = tween(durationMillis = 200)
                    ) + fadeOut(animationSpec = tween(200))
                ) {
                    Profile(viewModel = viewModel)
                }

                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(durationMillis = 300, delayMillis = 250)
                    ) + fadeIn(animationSpec = tween(300)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(durationMillis = 200)
                    ) + fadeOut(animationSpec = tween(200))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(scale.dp(232f)))

                        Box(
                            modifier = Modifier
                                .fillMaxSize(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkBlue)
                                    .drawBehind {
                                        val strokeDp = scale.dp(1f)

                                        val h = size.height
                                        val w = size.width

                                        val stroke = strokeDp.toPx()
                                        val halfStroke = stroke / 2f

                                        drawLine(
                                            color = Orange,
                                            start = Offset(0f, halfStroke),
                                            end = Offset(w, halfStroke),
                                            strokeWidth = stroke
                                        )

                                        drawLine(
                                            color = Orange,
                                            start = Offset(0f, h - halfStroke),
                                            end = Offset(w, h - halfStroke),
                                            strokeWidth = stroke
                                        )
                                    }
                                    .padding(horizontal = scale.dp(16f), vertical = scale.dp(4f)),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    GridBackground(Modifier.matchParentSize(), color = LightBlue, 14f)

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = scale.dp(22f), end = scale.dp(16f), bottom = scale.dp(36f), top = scale.dp(88f)),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(scale.dp(20f))
                                    ) {
                                        EditSection(
                                            viewModel = viewModel,
                                            onOpenChangeName = onOpenChangeName,
                                            onOpenChangeFlag = onOpenChangeFlag,
                                            onTakePhoto = onTakePhotoClick
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FlagSelectionDialog(onDismiss: () -> Unit, onFlagSelected: (Flag) -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            shape = CutCornerShape(12.dp),
            color = DarkBlue, // Changed to match theme
            border = BorderStroke(1.dp, Orange)
        ) {
            Column {
                Text(
                    "SELECT FLAG",
                    Modifier.padding(16.dp),
                    color = White,
                    fontWeight = FontWeight.Bold
                )
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    items(availableFlags) { flag ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onFlagSelected(flag) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painterResource(flag.resourceId),
                                null,
                                Modifier.size(40.dp, 25.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(flag.name, color = White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DialogName(
    viewModel: ProfileViewModel,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    // -- SCALE ( used for applying conversions ) --
    val scale = RememberScaleConversion()
    val user by viewModel.userState.collectAsState()

    // [ MEMO ]: Sizes are taken from 720 x 1600px mockup ( with 72dpi ) using the Redmi Note 10S
    val boxShape = CutCornerShape(scale.dp(28f)) // 28px
    val closeButtonShape = CutCornerShape(topEnd = scale.dp(24f), bottomStart = scale.dp(24f))

    val maxWidth = scale.dp(648f) // 648px, etc.

    var newName by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {  }
            .graphicsLayer(),
        contentAlignment = Alignment.Center
    ) {
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
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Spacer(modifier = Modifier.height(scale.dp(64f)))

                            Text(
                                text = "The username must be between 3 and 20 characters in length.",
                                color = White,
                                textAlign = TextAlign.Center,
                                fontSize = scale.sp(20f),
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
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

                            Spacer(modifier = Modifier.height(scale.dp(42f)))

                            TextField(
                                value = newName,
                                onValueChange = { if (it.length <= 20) newName = it },
                                placeholder = user?.name ?: "Sailor"
                            )

                            Spacer(modifier = Modifier.height(scale.dp(104f)))

                            SecondaryButton(
                                paddingH = 104f,
                                paddingV = 24f,
                                text = "OK",
                                onClick = {
                                    if (newName.isNotBlank()) {
                                        viewModel.updateName(newName)
                                        onConfirm()
                                    }
                                },
                                modifier = Modifier,
                                enabled = newName.isNotBlank()
                            )

                            Spacer(modifier = Modifier.height(scale.dp(28f)))
                        }
                    }
                }
            }

            Box(modifier = Modifier.zIndex(1f)) {
                Tab(114f, 32f, text = "Change name")
            }

            CloseButton(
                onClick = onDismiss,
                shape = closeButtonShape,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    val scale = RememberScaleConversion()

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(
            color = White,
            fontSize = scale.sp(26f)
        ),
        singleLine = true,
        cursorBrush = SolidColor(White),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(scale.dp(68f))
                    .border(1.dp, Orange)
                    .background(DarkBlue)
                    .padding(horizontal = scale.dp(20f)),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = TextStyle(
                            color = White,
                            fontSize = scale.sp(26f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun CloseButton(
    onClick: () -> Unit,
    shape: CutCornerShape,
    modifier: Modifier = Modifier
) {
    val scale = RememberScaleConversion()
    val interactionSource = remember { MutableInteractionSource() }
    val buttonSize = scale.dp(72f)

    Box(
        modifier = modifier
            .padding(top = scale.dp(50f))
            .size(buttonSize)
            .background(DarkBlue, shape = shape)
            .border(BorderStroke(scale.dp(1f), Orange), shape = shape)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            tint = White,
            modifier = Modifier.size(scale.dp(26f))
        )
    }
}

@Composable
private fun EditSection(
    viewModel: ProfileViewModel,
    onOpenChangeName: () -> Unit,
    onOpenChangeFlag: () -> Unit,
    onTakePhoto: () -> Unit
) {
    val user by viewModel.userState.collectAsState()

    val currentFlag = remember(user?.countryCode) {
        availableFlags.find { it.code == user?.countryCode } ?: availableFlags.first()
    }

    val scale = RememberScaleConversion()

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(scale.dp(18f)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(scale.dp(68f))
                    .border(scale.dp(1f), LightGrey)
                    .background(DarkBlue)
                    .padding(horizontal = scale.dp(20f)),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = user?.name ?: "Sailor",
                    color = White,
                    fontSize = scale.sp(26f),
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

            FifthButton(
                text = "Change name",
                onClick = onOpenChangeName
            )
        }

        Spacer(modifier = Modifier.height(scale.dp(26f)))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = scale.dp(144f)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .width(scale.dp(120f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(scale.dp(18f))
            ) {
                Text(
                    text = "Portrait",
                    color = White,
                    fontSize = scale.sp(26f),
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

                val painter = if (!user?.profilePictureUrl.isNullOrEmpty()) {
                    rememberAsyncImagePainter(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(user?.profilePictureUrl)
                            .crossfade(true)
                            // Force refresh if timestamp changes
                            .setParameter("key", user?.lastUpdated ?: System.currentTimeMillis())
                            .build(),
                        error = painterResource(R.drawable.ic_avatar_placeholder)
                    )
                } else {
                    painterResource(id = R.drawable.ic_avatar_placeholder)
                }

                SixthButton(
                    onClick = onTakePhoto, // Opens Camera
                    imagePainter = painter
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .width(scale.dp(120f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(scale.dp(18f))
            ) {
                Text(
                    text = "Flag",
                    color = White,
                    fontSize = scale.sp(26f),
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

                SixthButton(
                    onClick = onOpenChangeFlag,
                    imagePainter = painterResource(id = currentFlag.resourceId)
                )
            }
        }

        Spacer(modifier = Modifier.height(scale.dp(132f)))

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OverviewSection()
        }
    }
}

// --- UTILS (Location Logic) ---
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

@Composable
private fun OverviewSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SeventhButton(
            text = "Friends",
            icon = Icons.Default.Groups,
            onClick = {  }
        )

        SeventhButton(
            text = "Statistics",
            icon = Icons.Default.BarChart,
            onClick = {  }
        )

        SeventhButton(
            text = "Rankings",
            icon = Icons.Default.EmojiEvents,
            onClick = {  }
        )
    }
}