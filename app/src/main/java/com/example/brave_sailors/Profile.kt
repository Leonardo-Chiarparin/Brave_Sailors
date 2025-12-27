package com.example.brave_sailors

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.example.brave_sailors.data.CountryFlag
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
import kotlinx.coroutines.delay

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

    val user by viewModel.userState.collectAsState()
    val flagsList by viewModel.flagList.collectAsState() // Raccogli la lista dal ViewModel

    var showFilterDialog by remember { mutableStateOf(false) }
    var showFlagSelectionDialog by remember { mutableStateOf(false) } // Stato per il dialog bandiere
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }

    val cropImageLauncher = rememberLauncherForActivityResult(contract = CropImageContract()) { result ->
        if (result.isSuccessful) {
            capturedImageUri = result.uriContent
            showFilterDialog = true
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

    val onTakePhotoClick = {
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            val options = CropImageContractOptions(
                uri = null,
                cropImageOptions = CropImageOptions(
                    imageSourceIncludeGallery = false, // Solo Camera
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

    LaunchedEffect(Unit) {
        delay(550.toLong())
        isVisible = true
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
                .widthIn(max = maxWidth)
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
                                        drawLine(Orange, Offset(0f, halfStroke), Offset(w, halfStroke), stroke)
                                        drawLine(Orange, Offset(0f, h - halfStroke), Offset(w, h - halfStroke), stroke)
                                    }
                                    .padding(horizontal = scale.dp(16f), vertical = scale.dp(4f)),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
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
                                            onOpenChangeFlag = { showFlagSelectionDialog = true }, // Apre il dialog
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


        if (showFilterDialog && capturedImageUri != null) {
            FilterSelectionDialog(
                imageUri = capturedImageUri!!,
                onDismiss = { showFilterDialog = false },
                onConfirm = { bitmapWithFilter ->
                    viewModel.updateProfilePicture(context, bitmapWithFilter)
                    showFilterDialog = false
                }
            )
        }


        if (showFlagSelectionDialog) {
            FlagSelectionDialog(
                availableFlags = flagsList, // Passa la lista scaricata dal VM
                onDismiss = { showFlagSelectionDialog = false },
                onFlagSelected = { selectedFlag ->
                    viewModel.updateCountry(selectedFlag.code) // Aggiorna usando il codice
                    showFlagSelectionDialog = false
                }
            )
        }
    }
}

@Composable
fun FilterSelectionDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onConfirm: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val scale = RememberScaleConversion()

    val originalBitmap = remember(imageUri) {
        if (Build.VERSION.SDK_INT < 28) {
            MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
                .copy(Bitmap.Config.ARGB_8888, true)
        } else {
            val source = ImageDecoder.createSource(context.contentResolver, imageUri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = true
            }
        }
    }

    var applyFilter by remember { mutableStateOf(false) }
    val blackAndWhiteMatrix = ColorMatrix().apply { setToSaturation(0f) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(scale.dp(16f)),
            shape = CutCornerShape(scale.dp(20f)),
            color = DarkBlue,
            border = BorderStroke(1.dp, Orange)
        ) {
            Column(
                modifier = Modifier.padding(scale.dp(24f)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "EDIT PHOTO",
                    color = White,
                    fontSize = scale.sp(24f),
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = scale.sp(2f)
                )

                Spacer(modifier = Modifier.height(scale.dp(24f)))

                Box(
                    modifier = Modifier
                        .size(scale.dp(250f))
                        .border(1.dp, Orange, RectangleShape)
                ) {
                    Image(
                        bitmap = originalBitmap.asImageBitmap(),
                        contentDescription = "Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        colorFilter = if (applyFilter) ColorFilter.colorMatrix(blackAndWhiteMatrix) else null
                    )
                }

                Spacer(modifier = Modifier.height(scale.dp(24f)))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { applyFilter = !applyFilter }
                        .padding(vertical = 8.dp)
                ) {
                    Checkbox(
                        checked = applyFilter,
                        onCheckedChange = { applyFilter = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Orange,
                            uncheckedColor = LightGrey,
                            checkmarkColor = DarkBlue
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Apply B&W Filter",
                        color = White,
                        fontSize = scale.sp(18f)
                    )
                }

                Spacer(modifier = Modifier.height(scale.dp(32f)))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SecondaryButton(
                        text = "CANCEL",
                        onClick = onDismiss,
                        paddingH = 24f,
                        paddingV = 12f
                    )

                    SecondaryButton(
                        text = "OK",
                        onClick = {
                            val finalBitmap = if (applyFilter) {
                                applyColorMatrixToBitmap(originalBitmap, blackAndWhiteMatrix)
                            } else {
                                originalBitmap
                            }
                            onConfirm(finalBitmap)
                        },
                        paddingH = 32f,
                        paddingV = 12f
                    )
                }
            }
        }
    }
}

private fun applyColorMatrixToBitmap(src: Bitmap, matrix: ColorMatrix): Bitmap {
    val config = src.config ?: Bitmap.Config.ARGB_8888
    val dest = Bitmap.createBitmap(src.width, src.height, config)
    val canvas = android.graphics.Canvas(dest)
    val paint = android.graphics.Paint()
    val androidMatrix = android.graphics.ColorMatrix(matrix.values)
    paint.colorFilter = android.graphics.ColorMatrixColorFilter(androidMatrix)
    canvas.drawBitmap(src, 0f, 0f, paint)
    return dest
}

@Composable
private fun EditSection(viewModel: ProfileViewModel, onOpenChangeName: () -> Unit, onOpenChangeFlag: () -> Unit, onTakePhoto: () -> Unit) {
    val user by viewModel.userState.collectAsState()
    val flagsList by viewModel.flagList.collectAsState()
    val scale = RememberScaleConversion()


    val currentFlagUrl = remember(user?.countryCode, flagsList) {
        flagsList.find { it.code == user?.countryCode }?.flagUrl
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Top) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(scale.dp(18f)), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f).height(scale.dp(68f)).border(scale.dp(1f), LightGrey).background(DarkBlue).padding(horizontal = scale.dp(20f)), contentAlignment = Alignment.CenterStart) {
                Text(text = user?.name ?: "Sailor", color = White, fontSize = scale.sp(26f), fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, letterSpacing = scale.sp(2f), style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false), lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both), shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)))
            }
            FifthButton(text = "Change name", onClick = onOpenChangeName)
        }
        Spacer(modifier = Modifier.height(scale.dp(26f)))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = scale.dp(144f)), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.width(scale.dp(120f)), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(scale.dp(18f))) {
                Text(text = "Portrait", color = White, fontSize = scale.sp(26f), fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, letterSpacing = scale.sp(2f), style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false), lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both), shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)))

                val painter = if (!user?.profilePictureUrl.isNullOrEmpty()) {
                    rememberAsyncImagePainter(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(user?.profilePictureUrl)
                            .crossfade(true)
                            .setParameter("key", user?.lastUpdated ?: System.currentTimeMillis())
                            .build(),
                        error = painterResource(R.drawable.ic_avatar_placeholder)
                    )
                } else {
                    painterResource(id = R.drawable.ic_avatar_placeholder)
                }

                SixthButton(onClick = onTakePhoto, imagePainter = painter)
            }
            Spacer(modifier = Modifier.weight(1f))
            Column(modifier = Modifier.width(scale.dp(120f)), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(scale.dp(18f))) {
                Text(text = "Flag", color = White, fontSize = scale.sp(26f), fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, letterSpacing = scale.sp(2f), style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false), lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both), shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)))


                val flagPainter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(currentFlagUrl)
                        .crossfade(true)
                        .build(),
                    error = painterResource(R.drawable.ic_launcher_background), // Usa un placeholder
                    placeholder = painterResource(R.drawable.ic_launcher_background)
                )

                SixthButton(onClick = onOpenChangeFlag, imagePainter = flagPainter)
            }
        }
        Spacer(modifier = Modifier.height(scale.dp(132f)))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { OverviewSection() }
    }
}


@Composable
fun FlagSelectionDialog(
    availableFlags: List<CountryFlag>,
    onDismiss: () -> Unit,
    onFlagSelected: (CountryFlag) -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            shape = CutCornerShape(12.dp), color = DarkBlue, border = BorderStroke(1.dp, Orange)
        ) {
            Column {
                Text("SELECT FLAG", Modifier.padding(16.dp), color = White, fontWeight = FontWeight.Bold)
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    items(availableFlags) { flag ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onFlagSelected(flag) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(flag.flagUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = flag.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(40.dp, 25.dp).background(Color.Gray.copy(alpha = 0.3f))
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
fun DialogName(viewModel: ProfileViewModel, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = RememberScaleConversion()
    val user by viewModel.userState.collectAsState()
    val boxShape = CutCornerShape(scale.dp(28f))
    val closeButtonShape = CutCornerShape(topEnd = scale.dp(24f), bottomStart = scale.dp(24f))
    val maxWidth = scale.dp(648f)
    var newName by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)).clickable(interactionSource = interactionSource, indication = null) { }.graphicsLayer(), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.widthIn(max = maxWidth), contentAlignment = Alignment.TopCenter) {
            Box(modifier = Modifier.padding(top = scale.dp(50f)).fillMaxWidth().background(DarkBlue, shape = boxShape).border(BorderStroke(scale.dp(1f), Orange), shape = boxShape).clip(boxShape), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.padding(all = scale.dp(28f)).clip(RectangleShape), contentAlignment = Alignment.Center) {
                    GridBackground(Modifier.matchParentSize(), color = LightBlue, 14f)
                    Box(modifier = Modifier.padding(horizontal = scale.dp(48f)), contentAlignment = Alignment.Center) {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(modifier = Modifier.height(scale.dp(64f)))
                            Text(text = "The username must be between 3 and 20 characters in length.", color = White, textAlign = TextAlign.Center, fontSize = scale.sp(20f), fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, letterSpacing = scale.sp(2f), style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false), lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both), shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)))
                            Spacer(modifier = Modifier.height(scale.dp(42f)))
                            TextField(value = newName, onValueChange = { if (it.length <= 20) newName = it }, placeholder = user?.name ?: "Sailor")
                            Spacer(modifier = Modifier.height(scale.dp(104f)))
                            SecondaryButton(paddingH = 104f, paddingV = 24f, text = "OK", onClick = { if (newName.isNotBlank()) { viewModel.updateName(newName); onConfirm() } }, modifier = Modifier, enabled = newName.isNotBlank())
                            Spacer(modifier = Modifier.height(scale.dp(28f)))
                        }
                    }
                }
            }
            Box(modifier = Modifier.zIndex(1f)) { Tab(114f, 32f, text = "Change name") }
            CloseButton(onClick = onDismiss, shape = closeButtonShape, modifier = Modifier.align(Alignment.TopEnd))
        }
    }
}

@Composable
fun TextField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    val scale = RememberScaleConversion()
    BasicTextField(value = value, onValueChange = onValueChange, textStyle = TextStyle(color = White, fontSize = scale.sp(26f)), singleLine = true, cursorBrush = SolidColor(White), decorationBox = { innerTextField ->
        Box(modifier = Modifier.fillMaxWidth().height(scale.dp(68f)).border(1.dp, Orange).background(DarkBlue).padding(horizontal = scale.dp(20f)), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) { Text(text = placeholder, style = TextStyle(color = White, fontSize = scale.sp(26f)), modifier = Modifier.fillMaxWidth()) }
            innerTextField()
        }
    })
}

@Composable
private fun CloseButton(onClick: () -> Unit, shape: CutCornerShape, modifier: Modifier = Modifier) {
    val scale = RememberScaleConversion()
    val interactionSource = remember { MutableInteractionSource() }
    val buttonSize = scale.dp(72f)
    Box(modifier = modifier.padding(top = scale.dp(50f)).size(buttonSize).background(DarkBlue, shape = shape).border(BorderStroke(scale.dp(1f), Orange), shape = shape).clip(shape).clickable(interactionSource = interactionSource, indication = null, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = White, modifier = Modifier.size(scale.dp(26f)))
    }
}

@Composable
private fun OverviewSection() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        SeventhButton(text = "Friends", icon = Icons.Default.Groups, onClick = { })
        SeventhButton(text = "Statistics", icon = Icons.Default.BarChart, onClick = { })
        SeventhButton(text = "Rankings", icon = Icons.Default.EmojiEvents, onClick = { })
    }
}