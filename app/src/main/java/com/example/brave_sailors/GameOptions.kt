package com.example.brave_sailors

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.example.brave_sailors.model.ProfileViewModel
import com.example.brave_sailors.ui.components.DividerOrange
import com.example.brave_sailors.ui.components.GridBackground
import com.example.brave_sailors.ui.components.SixthButton
import com.example.brave_sailors.ui.theme.DarkBlue
import com.example.brave_sailors.ui.theme.LightBlue
import com.example.brave_sailors.ui.theme.Orange
import com.example.brave_sailors.ui.theme.White
import com.example.brave_sailors.ui.utils.GameSettingsManager
import com.example.brave_sailors.ui.utils.RememberScaleConversion
import java.io.File

@Composable
fun GameOptionsScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel // [ INTEGRATION ]: Needed for saving AI Avatar
) {
    Modal(onBack, viewModel)
}

@Composable
private fun Modal(onBack: () -> Unit, viewModel: ProfileViewModel) {
    val context = LocalContext.current
    val scale = RememberScaleConversion()
    val interactionSource = remember { MutableInteractionSource() }
    val maxWidth = scale.dp(720f)
    val strokeDp = scale.dp(1f)

    val closeButtonShape = CutCornerShape(bottomStart = scale.dp(34f))

    // -- SETTINGS MANAGER --
    val settingsManager = remember { GameSettingsManager(context) }

    // States connected to SharedPreferences
    var isAlarmOn by remember { mutableStateOf(settingsManager.isAlarmOn) }
    var isMusicOn by remember { mutableStateOf(settingsManager.isMusicOn) }
    var isVibrationOn by remember { mutableStateOf(settingsManager.isVibrationOn) }

    // -- AI AVATAR LOGIC --
    val userState by viewModel.userState.collectAsState()
    val aiAvatarPath = userState?.aiAvatarPath ?: "ic_ai_avatar_placeholder"

    val cropOptions = remember {
        CropImageContractOptions(
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
    }

    val cropImageLauncher = rememberLauncherForActivityResult(contract = CropImageContract()) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { uri ->
                // Convert URI to Bitmap and Save via ViewModel
                val bitmap = android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                viewModel.updateAiAvatar(context, bitmap)
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) cropImageLauncher.launch(cropOptions)
    }

    val onAiPortraitClick: () -> Unit = {
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            cropImageLauncher.launch(cropOptions)
        } else {
            cameraPermissionLauncher.launch(permission)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = maxWidth)
                .background(DarkBlue)
                .drawBehind {
                    val h = size.height
                    val w = size.width
                    val stroke = strokeDp.toPx()
                    val halfStroke = stroke / 2f
                    drawLine(Orange, Offset(0f, halfStroke), Offset(w, halfStroke), stroke)
                    drawLine(Orange, Offset(0f, h - halfStroke), Offset(w, h - halfStroke), stroke)
                },
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = scale.dp(16f), end = scale.dp(16f), top = scale.dp(8f), bottom = scale.dp(4f)),
                contentAlignment = Alignment.Center
            ) {
                GridBackground(Modifier.matchParentSize(), color = LightBlue, 18f)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(scale.dp(42f)))

                    Text(
                        text = "GAME OPTIONS",
                        color = White,
                        fontSize = scale.sp(34f),
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = scale.sp(2f),
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
                            shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)
                        )
                    )

                    Spacer(modifier = Modifier.height(scale.dp(130f)))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = scale.dp(80f)),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Audio and graphics",
                            color = White,
                            fontSize = scale.sp(32f),
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = scale.sp(2f),
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
                                shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)
                            )
                        )

                        Spacer(modifier = Modifier.height(scale.dp(8f)))
                        DividerOrange()
                        Spacer(modifier = Modifier.height(scale.dp(46f)))

                        OptionItem("Alarm \"Your turn\"", isAlarmOn) {
                            isAlarmOn = it
                            settingsManager.isAlarmOn = it
                        }

                        Spacer(modifier = Modifier.height(scale.dp(28f)))

                        OptionItem("Music", isMusicOn) {
                            isMusicOn = it
                            settingsManager.isMusicOn = it
                        }

                        Spacer(modifier = Modifier.height(scale.dp(28f)))

                        OptionItem("Vibration", isVibrationOn) {
                            isVibrationOn = it
                            settingsManager.isVibrationOn = it
                        }

                        Spacer(modifier = Modifier.height(scale.dp(28f)))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Portrait AI",
                                color = White,
                                fontSize = scale.sp(26f),
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = scale.sp(2f),
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
                                    shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)
                                )
                            )

                            // AI Avatar Display
                            val aiAvatarPainter = if (aiAvatarPath != "ic_ai_avatar_placeholder") {
                                rememberAsyncImagePainter(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(File(aiAvatarPath))
                                        .build(),
                                    error = painterResource(R.drawable.ic_ai_avatar_placeholder)
                                )
                            } else {
                                painterResource(id = R.drawable.ic_ai_avatar_placeholder)
                            }

                            Box(
                                modifier = Modifier
                                    .padding(horizontal = scale.dp(28f))
                                    .size(scale.dp(70f)),
                                contentAlignment = Alignment.Center
                            ) {
                                SixthButton(
                                    onClick = onAiPortraitClick,
                                    imagePainter = aiAvatarPainter
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(scale.dp(50f)))
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(DarkBlue, shape = closeButtonShape)
                    .border(scale.dp(1f), Orange, shape = closeButtonShape)
                    .clip(closeButtonShape)
                    .size(scale.dp(92f))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onBack
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = White,
                    modifier = Modifier.size(scale.dp(38f))
                )
            }
        }
    }
}

// OptionItem and GameOptionSwitch remain unchanged
@Composable
fun OptionItem(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val scale = RememberScaleConversion()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = White,
            fontSize = scale.sp(26f),
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            letterSpacing = scale.sp(2f),
            style = TextStyle(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
                shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)
            )
        )
        GameOptionSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun GameOptionSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val scale = RememberScaleConversion()
    val width = scale.dp(122f)
    val height = scale.dp(64f)
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .border(scale.dp(2f), Orange)
            .clickable(interactionSource = interactionSource, indication = null) { onCheckedChange(!checked) }
    ) {
        if (checked) {
            Box(modifier = Modifier.size(width / 2, height).background(Orange), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Menu, null, tint = DarkBlue, modifier = Modifier.graphicsLayer(rotationZ = 90f).size(scale.dp(22f)))
            }
            Box(modifier = Modifier.size(width / 2, height).background(DarkBlue), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, null, tint = White, modifier = Modifier.size(scale.dp(36f)))
            }
        } else {
            Box(modifier = Modifier.size(width / 2, height).background(DarkBlue), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Close, null, tint = White, modifier = Modifier.size(scale.dp(42f)))
            }
            Box(modifier = Modifier.size(width / 2, height).background(Orange), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Menu, null, tint = Color.Black, modifier = Modifier.graphicsLayer(rotationZ = 90f).size(scale.dp(22f)))
            }
        }
    }
}