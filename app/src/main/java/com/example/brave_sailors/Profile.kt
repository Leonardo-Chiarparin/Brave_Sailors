package com.example.brave_sailors

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.example.brave_sailors.data.local.database.entity.User
import com.example.brave_sailors.data.remote.api.Flag
import com.example.brave_sailors.model.ProfileViewModel
import com.example.brave_sailors.ui.components.FifthButton
import com.example.brave_sailors.ui.components.GridBackground
import com.example.brave_sailors.ui.components.Profile
import com.example.brave_sailors.ui.components.SeventhButton
import com.example.brave_sailors.ui.components.SixthButton
import com.example.brave_sailors.ui.theme.DarkBlue
import com.example.brave_sailors.ui.theme.LightBlue
import com.example.brave_sailors.ui.theme.LightGrey
import com.example.brave_sailors.ui.theme.Orange
import com.example.brave_sailors.ui.theme.White
import com.example.brave_sailors.ui.utils.RememberScaleConversion

enum class OverlayProfileState {
    IDLE,
    EXITING_PROFILE,
    SHOWING_STATS,
    SHOWING_RANKINGS
}

@Composable
fun ProfileScreen(isVisible: Boolean, viewModel: ProfileViewModel, onGetPhoto: (Uri) -> Unit, onOpenChangeFlag: () -> Unit, onOpenChangeName: () -> Unit, onOpenStatistics: () -> Unit, onOpenRankings: () -> Unit) {
    val context = LocalContext.current

    // --- LOGIC INTEGRATION START ---

    // 1. Observing user state from DB
    val user by viewModel.userState.collectAsState()
    val flagList = viewModel.flagList.collectAsState()
    val availableFlags = flagList.value

    // 2. Camera Logic
    var showDialogFilter by remember { mutableStateOf(false) }

    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }

    val cropImageLauncher = rememberLauncherForActivityResult(contract = CropImageContract()) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { uri ->
                onGetPhoto(uri)
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val options = CropImageContractOptions(
                uri = null,
                cropImageOptions = CropImageOptions(
                    imageSourceIncludeGallery = false,
                    imageSourceIncludeCamera = true,
                    guidelines = CropImageView.Guidelines.ON,
                    aspectRatioX = 1,
                    aspectRatioY = 1,
                    fixAspectRatio = true
                )
            )
            cropImageLauncher.launch(options)
        }
    }

    val onOpenChangePhoto: () -> Unit = {
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            val options = CropImageContractOptions(
                uri = null,
                cropImageOptions = CropImageOptions(
                    imageSourceIncludeGallery = false,
                    imageSourceIncludeCamera = true,
                    guidelines = CropImageView.Guidelines.ON,
                    aspectRatioX = 1,
                    aspectRatioY = 1,
                    fixAspectRatio = true
                )
            )
            cropImageLauncher.launch(options)
        } else {
            cameraPermissionLauncher.launch(permission)
        }
    }

    Modal(isVisible, user, availableFlags, viewModel, onOpenChangeFlag, onOpenChangeName, onOpenChangePhoto, onOpenStatistics, onOpenRankings)
}

@Composable
private fun Modal(
    isVisible: Boolean,
    user: User?,
    availableFlags: List<Flag>,
    viewModel: ProfileViewModel,
    onOpenChangeFlag: () -> Unit,
    onOpenChangeName: () -> Unit,
    onOpenChangePhoto: () -> Unit,
    onOpenStatistics: () -> Unit,
    onOpenRankings: () -> Unit
) {
    val scale = RememberScaleConversion()
    val maxWidth = scale.dp(720f)

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
                    // [ NOTE ]: A possible refactoring may be needed even at this point of the code
                    Profile(viewModel = viewModel)
                }

                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(durationMillis = 300, delayMillis = 250)
                    ) + fadeIn(animationSpec = tween(300, delayMillis = 250)),
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
                                            user = user,
                                            availableFlags = availableFlags,
                                            onOpenChangeName = onOpenChangeName,
                                            onOpenChangeFlag = onOpenChangeFlag,
                                            onOpenChangePhoto = onOpenChangePhoto,
                                            onOpenStatistics = onOpenStatistics,
                                            onOpenRankings = onOpenRankings
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
private fun EditSection(
    user: User?,
    availableFlags: List<Flag>,
    onOpenChangeName: () -> Unit,
    onOpenChangeFlag: () -> Unit,
    onOpenChangePhoto: () -> Unit,
    onOpenStatistics: () -> Unit,
    onOpenRankings: () -> Unit
) {
    val currentFlag = remember(user?.countryCode, availableFlags) {
        availableFlags.find { it.code == user?.countryCode } ?: availableFlags.firstOrNull()
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
                            .data(user.profilePictureUrl)
                            // Force refresh if timestamp changes
                            .setParameter("key", user.lastUpdated)
                            .build(),
                        error = painterResource(R.drawable.ic_avatar_placeholder)
                    )
                } else {
                    painterResource(id = R.drawable.ic_avatar_placeholder)
                }

                SixthButton(
                    onClick = onOpenChangePhoto, // Opens the camera
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
                    imagePainter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(currentFlag?.flagUrl)
                            .build()
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(scale.dp(132f)))

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OverviewSection(onOpenStatistics, onOpenRankings)
        }
    }
}

@Composable
private fun OverviewSection(onOpenStatistics: () -> Unit, onOpenRankings: () -> Unit) {
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
            onClick = onOpenStatistics
        )

        SeventhButton(
            text = "Rankings",
            icon = Icons.Default.EmojiEvents,
            onClick = onOpenRankings
        )
    }
}