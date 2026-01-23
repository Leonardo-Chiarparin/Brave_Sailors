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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun GameOptionsScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onGetAiPhoto: (Uri) -> Unit
) {
    Modal(viewModel, onBack, onGetAiPhoto)
}

@Composable
private fun Modal(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onGetAiPhoto: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val scale = RememberScaleConversion()
    val interactionSource = remember { MutableInteractionSource() }
    val maxWidth = scale.dp(720f)
    val strokeDp = scale.dp(1f)

    val closeButtonShape = CutCornerShape(bottomStart = scale.dp(34f))

    val settingManager = remember { GameSettingsManager(context) }

    var isAlarmOn by remember { mutableStateOf(settingManager.isAlarmOn) }
    var isMusicOn by remember { mutableStateOf(settingManager.isMusicOn) }
    var isVibrationOn by remember { mutableStateOf(settingManager.isVibrationOn) }

    val userState by viewModel.userState.collectAsState()
    val aiAvatarPath = userState?.aiAvatarPath ?: "ic_ai_avatar_placeholder"

    val cropOptions = remember {
        CropImageContractOptions(
            uri = null,
            cropImageOptions = CropImageOptions(
                imageSourceIncludeGallery = true,
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
                scope.launch {
                    onGetAiPhoto(uri)
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cropImageLauncher.launch(cropOptions)
        }
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
                            lineHeightStyle = LineHeightStyle(
                                LineHeightStyle.Alignment.Center,
                                LineHeightStyle.Trim.Both
                            ),
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
                                lineHeightStyle = LineHeightStyle(
                                    LineHeightStyle.Alignment.Center,
                                    LineHeightStyle.Trim.Both
                                ),
                                shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)
                            )
                        )

                        Spacer(modifier = Modifier.height(scale.dp(8f)))

                        DividerOrange()

                        Spacer(modifier = Modifier.height(scale.dp(46f)))

                        OptionItem("Alarm \"Your turn\"", isAlarmOn) {
                            isAlarmOn = it
                            settingManager.isAlarmOn = it
                        }

                        Spacer(modifier = Modifier.height(scale.dp(28f)))

                        OptionItem("Music", isMusicOn) {
                            isMusicOn = it
                            settingManager.isMusicOn = it
                        }

                        Spacer(modifier = Modifier.height(scale.dp(28f)))

                        OptionItem("Vibration", isVibrationOn) {
                            isVibrationOn = it
                            settingManager.isVibrationOn = it
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
                                    lineHeightStyle = LineHeightStyle(
                                        LineHeightStyle.Alignment.Center,
                                        LineHeightStyle.Trim.Both
                                    ),
                                    shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)
                                )
                            )

                            Box(
                                modifier = Modifier
                                    .padding(horizontal = scale.dp(28f))
                                    .size(scale.dp(70f)),
                                contentAlignment = Alignment.Center
                            ) {
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

                                SixthButton(
                                    onClick = onAiPortraitClick,
                                    imagePainter = aiAvatarPainter                                 )
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

@Composable
fun OptionItem(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val scale = RememberScaleConversion()

    Row(
        modifier = Modifier
            .fillMaxWidth(),
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
                lineHeightStyle = LineHeightStyle(
                    LineHeightStyle.Alignment.Center,
                    LineHeightStyle.Trim.Both
                ),
                shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)
            )
        )
        GameOptionSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun GameOptionSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val scale = RememberScaleConversion()
    val width = scale.dp(122f)
    val height = scale.dp(64f)
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .border(scale.dp(2f), Orange)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onCheckedChange(!checked) }
    ) {
        if (checked) {
            Box(
                modifier = Modifier
                    .size(width / 2, height)
                    .background(Orange),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = null,
                    tint = DarkBlue,
                    modifier = Modifier
                        .graphicsLayer(rotationZ = 90f)
                        .size(scale.dp(22f))
                )
            }
            Box(
                modifier = Modifier
                    .size(width / 2, height)
                    .background(DarkBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = White,
                    modifier = Modifier.size(scale.dp(36f))
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(width / 2, height)
                    .background(DarkBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = White,
                    modifier = Modifier.size(scale.dp(42f))
                )
            }
            Box(
                modifier = Modifier
                    .size(width / 2, height)
                    .background(Orange),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier
                        .graphicsLayer(rotationZ = 90f)
                        .size(scale.dp(22f))
                )
            }
        }
    }
}