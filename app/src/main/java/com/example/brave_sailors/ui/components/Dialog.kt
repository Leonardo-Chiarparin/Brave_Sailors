package com.example.brave_sailors.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollFactory
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import coil.compose.rememberAsyncImagePainter
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Size
import com.example.brave_sailors.data.remote.api.Flag
import com.example.brave_sailors.ui.theme.Blue
import com.example.brave_sailors.ui.theme.DarkBlue
import com.example.brave_sailors.ui.theme.DeepBlue
import com.example.brave_sailors.ui.theme.LightBlue
import com.example.brave_sailors.ui.theme.Orange
import com.example.brave_sailors.ui.theme.TransparentGrey
import com.example.brave_sailors.ui.theme.White
import com.example.brave_sailors.ui.utils.RememberScaleConversion

sealed interface ActiveDialog {
    data object None: ActiveDialog

    // Account Dialogs
    data object Register : ActiveDialog
    data object Access : ActiveDialog
    data class Password(val emailToPrefill: String) : ActiveDialog
    data object DeleteAccount : ActiveDialog

    // Profile Dialogs
    data class Filter(val uri: Uri) : ActiveDialog
    data object Flag : ActiveDialog
    data object Name : ActiveDialog
    data object Friend : ActiveDialog

    // Game Options Dialogs
    data class AiFilter(val uri: Uri) : ActiveDialog

    // Armada Dialogs
    data object Deployment : ActiveDialog

    // System / Errors
    data class Error(val message: String, val source: DialogSource) : ActiveDialog
}

enum class DialogSource {
    NONE, REGISTER, ACCESS, PASSWORD, DELETE, FRIEND
}

@Composable
fun DialogFriend(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = RememberScaleConversion()

    val boxShape = CutCornerShape(scale.dp(24f))
    val closeButtonShape = CutCornerShape(topEnd = scale.dp(24f), bottomStart = scale.dp(24f))
    val maxWidth = scale.dp(646f)

    var friendIdText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(interactionSource = interactionSource, indication = null) {  }
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
                    .padding(top = scale.dp(50f))
                    .fillMaxWidth()
                    .background(DarkBlue, shape = boxShape)
                    .border(BorderStroke(scale.dp(1f), Orange), shape = boxShape)
                    .clip(boxShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = scale.dp(28f), end = scale.dp(28f), bottom = scale.dp(28f), top = scale.dp(60f))
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
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(scale.dp(64f)))

                            Text(
                                text = "Submitting a player's ID will initiate a new friendship. Contacts are visible on the multiplayer page via lobby.",
                                color = White,
                                textAlign = TextAlign.Center,
                                fontSize = scale.sp(22f),
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

                            BasicTextField(
                                value = friendIdText,
                                onValueChange = { friendIdText = it },
                                textStyle = TextStyle(
                                    color = White,
                                    fontSize = scale.sp(26f),
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = scale.sp(2f)
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(White),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(scale.dp(64f))
                                            .border(BorderStroke(scale.dp(1f), Orange))
                                            .background(DarkBlue)
                                            .padding(horizontal = scale.dp(24f)),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (friendIdText.isEmpty()) {
                                            Text(
                                                text = "Enter the ID ...",
                                                style = TextStyle(
                                                    color = Orange,
                                                    fontSize = scale.sp(26f),
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontWeight = FontWeight.Medium,
                                                    fontStyle = FontStyle.Italic,
                                                    letterSpacing = scale.sp(2f)
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(scale.dp(104f)))

                            SecondaryButton(
                                paddingH = 104f,
                                paddingV = 24f,
                                text = "OK",
                                onClick = {
                                    if (friendIdText.isNotBlank()) {
                                        onConfirm(friendIdText)
                                        onDismiss()
                                    }
                                },
                                modifier = Modifier,
                                enabled = friendIdText.isNotBlank()
                            )

                            Spacer(modifier = Modifier.height(scale.dp(28f)))
                        }
                    }
                }
            }

            Box(modifier = Modifier.zIndex(1f)) {
                Tab(114f, 32f, text = "Add kith")
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
fun DialogFilter(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onConfirm: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val scale = RememberScaleConversion()
    val interactionSource = remember { MutableInteractionSource() }

    // Logic for retrieving bitmap
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(imageUri) {
        val request = ImageRequest.Builder(context)
            .data(imageUri)
            .size(Size.ORIGINAL)
            .allowHardware(false)
            .build()

        val result = context.imageLoader.execute(request)

        if (result is coil.request.SuccessResult)
            originalBitmap = result.drawable.toBitmap()
    }

    var applyFilter by remember { mutableStateOf(false) }
    val blackAndWhiteMatrix = ColorMatrix().apply { setToSaturation(0f) }

    val boxShape = CutCornerShape(scale.dp(24f))
    val closeButtonShape = CutCornerShape(topEnd = scale.dp(24f), bottomStart = scale.dp(24f))
    val maxWidth = scale.dp(576f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(interactionSource = interactionSource, indication = null) {  }
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
                    .padding(top = scale.dp(50f))
                    .fillMaxWidth()
                    .background(DarkBlue, shape = boxShape)
                    .border(BorderStroke(scale.dp(1f), Orange), shape = boxShape)
                    .clip(boxShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = scale.dp(28f), end = scale.dp(28f), bottom = scale.dp(28f), top = scale.dp(60f))
                        .clip(RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    GridBackground(Modifier.matchParentSize(), color = LightBlue, 14f)

                    Box(
                        modifier = Modifier
                            .padding(horizontal = scale.dp(8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(scale.dp(80f)))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkBlue)
                                    .padding(all = scale.dp(4f)),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Shown below is the only option: a newly captured image taken using the camera.",
                                    color = White,
                                    textAlign = TextAlign.Center,
                                    fontSize = scale.sp(22f),
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

                                Spacer(modifier = Modifier.height(scale.dp(14f)))

                                Text(
                                    text = "A black-and-white filter may be applied by simply enabling the corresponding setting.",
                                    color = Color(0xFF94B4D8),
                                    textAlign = TextAlign.Center,
                                    fontSize = scale.sp(20f),
                                    fontFamily = FontFamily.SansSerif,
                                    fontStyle = FontStyle.Italic,
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

                                Spacer(modifier = Modifier.height(scale.dp(18f)))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(scale.dp(186f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (originalBitmap != null) {
                                            Image(
                                                bitmap = originalBitmap!!.asImageBitmap(),
                                                contentDescription = "Preview",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                                colorFilter = if (applyFilter) ColorFilter.colorMatrix(blackAndWhiteMatrix) else null
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(scale.dp(32f)))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Spacer(modifier = Modifier.width(scale.dp(30f)))

                                    Box(
                                        modifier = Modifier
                                            .size(scale.dp(28f))
                                            .background(if (applyFilter) Color(0xFF08F804) else Color.Transparent, CircleShape)
                                            .border(scale.dp(1f), White, CircleShape)
                                            .clip(CircleShape)
                                            .clickable(interactionSource = interactionSource, indication = null) { applyFilter = !applyFilter }
                                    )

                                    Spacer(modifier = Modifier.width(scale.dp(24f)))

                                    Text(
                                        text = "Customize the picture",
                                        color = White,
                                        fontSize = scale.sp(24f),
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
                                }
                            }

                            Spacer(modifier = Modifier.height(scale.dp(60f)))

                            SecondaryButton(
                                paddingH = 104f,
                                paddingV = 24f,
                                text = "OK",
                                onClick = {
                                    originalBitmap?.let { bitmap ->
                                        val finalBitmap = if (applyFilter) applyColorMatrixToBitmap(bitmap, blackAndWhiteMatrix) else bitmap
                                        onConfirm(finalBitmap)
                                    }
                                },
                                modifier = Modifier,
                                enabled = originalBitmap != null
                            )

                            Spacer(modifier = Modifier.height(scale.dp(32f)))
                        }
                    }
                }
            }

            Box(modifier = Modifier.zIndex(1f)) {
                Tab(64f, 32f, text = "Change portrait")
            }

            CloseButton(
                onClick = onDismiss,
                shape = closeButtonShape,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialogFlag(
    availableFlags: List<Flag>,
    currentCode: String,
    onDismiss: () -> Unit,
    onConfirm: (Flag) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = RememberScaleConversion()

    val boxShape = CutCornerShape(scale.dp(24f))
    val closeButtonShape = CutCornerShape(topEnd = scale.dp(24f), bottomStart = scale.dp(24f))
    val maxWidth = scale.dp(576f)

    val rowHeight = scale.dp(88f)
    val visibleItems = 6
    val listHeight = rowHeight * visibleItems + scale.dp(24f)

    var searchText by remember { mutableStateOf("") }

    val filteredFlags = remember(searchText) {
        if (searchText.isBlank()) availableFlags
        else availableFlags.filter { it.name.contains(searchText, ignoreCase = true) }
    }

    var selectedFlag by remember {
        mutableStateOf(availableFlags.find { it.code == currentCode } ?: availableFlags.first())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(interactionSource = interactionSource, indication = null) { },
        contentAlignment = Alignment.Center
    ) {
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
                Box(
                    modifier = Modifier
                        .padding(start = scale.dp(28f), end = scale.dp(28f), bottom = scale.dp(28f), top = scale.dp(60f))
                        .clip(RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    GridBackground(Modifier.matchParentSize(), color = LightBlue, 14f)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = scale.dp(14f)),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(scale.dp(40f)))

                        BasicTextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            textStyle = TextStyle(
                                color = White,
                                fontSize = scale.sp(26f),
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = scale.sp(2f)
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(White),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(scale.dp(64f))
                                        .border(BorderStroke(scale.dp(1f), Orange))
                                        .background(Color.Transparent)
                                        .padding(horizontal = scale.dp(24f)),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (searchText.isEmpty()) {
                                        Text(
                                            text = "Search ...",
                                            style = TextStyle(
                                                color = Orange,
                                                fontSize = scale.sp(26f),
                                                fontFamily = FontFamily.SansSerif,
                                                fontWeight = FontWeight.Medium,
                                                fontStyle = FontStyle.Italic,
                                                letterSpacing = scale.sp(2f)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(scale.dp(32f)))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(listHeight)
                        ) {
                            val listState = rememberLazyListState()

                            CompositionLocalProvider(
                                LocalOverscrollFactory provides null
                            ) {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    itemsIndexed(
                                        filteredFlags,
                                        key = { _, flag -> flag.code }
                                    ) { index, flag ->

                                        val bgColor = if (index % 2 == 0) Color.Transparent else DeepBlue.copy(alpha = 0.5f)
                                        val isSelected = flag.code == selectedFlag.code

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(rowHeight)
                                                .background(bgColor)
                                                .drawBehind {
                                                    if (isSelected) {
                                                        val h = size.height
                                                        val w = size.width

                                                        val stroke = scale.dp(1f).toPx()
                                                        val halfStroke = stroke / 2f

                                                        val path = Path().apply {
                                                            moveTo(halfStroke, halfStroke)
                                                            lineTo(w - halfStroke, halfStroke)
                                                            lineTo(w - halfStroke, h - halfStroke)
                                                            lineTo(halfStroke, h - halfStroke)
                                                            close()
                                                        }

                                                        drawPath(
                                                            path = path,
                                                            color = Orange,
                                                            style = Stroke(
                                                                width = stroke,
                                                                cap = StrokeCap.Butt,
                                                                join = StrokeJoin.Miter
                                                            )
                                                        )
                                                    }
                                                }
                                                .clickable(
                                                    interactionSource = interactionSource,
                                                    indication = null
                                                ) {
                                                    selectedFlag = flag
                                                }
                                                .padding(horizontal = scale.dp(14f)),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Image(
                                                painter = rememberAsyncImagePainter(
                                                    model = ImageRequest.Builder(LocalContext.current)
                                                        .data(flag.flagUrl)
                                                        .build()
                                                ),
                                                contentDescription = flag.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(scale.dp(108f), scale.dp(62f))
                                            )

                                            Spacer(modifier = Modifier.width(scale.dp(16f)))

                                            Text(
                                                text = flag.name,
                                                color = White,
                                                fontSize = scale.sp(26f),
                                                fontFamily = FontFamily.SansSerif,
                                                fontWeight = FontWeight.Medium,
                                                letterSpacing = scale.sp(2f),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
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
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(scale.dp(32f)))

                        SecondaryButton(
                            paddingH = 104f,
                            paddingV = 24f,
                            text = "OK",
                            onClick = {
                                onConfirm(selectedFlag)
                                onDismiss()
                            },
                            modifier = Modifier
                        )

                        Spacer(modifier = Modifier.height(scale.dp(6f)))
                    }
                }
            }

            Box(modifier = Modifier.zIndex(1f)) {
                Tab(80f, 32f, text = "Change flag")
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
fun DialogName(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    // -- SCALE ( used for applying conversions ) --
    val scale = RememberScaleConversion()

    // [ MEMO ]: Sizes are taken from 720 x 1600px mockup ( with 72dpi ) using the Redmi Note 10S
    val boxShape = CutCornerShape(scale.dp(24f)) // 24px
    val closeButtonShape = CutCornerShape(topEnd = scale.dp(24f), bottomStart = scale.dp(24f))

    val maxWidth = scale.dp(646f) // 646px, etc.

    var newName by remember(currentName) {
        mutableStateOf(
            TextFieldValue(
                text = currentName,
                selection = TextRange(currentName.length)
            )
        )
    }

    val content = newName.text

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
                        .padding(start = scale.dp(28f), end = scale.dp(28f), bottom = scale.dp(28f), top = scale.dp(60f))
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
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(scale.dp(64f)))

                            Text(
                                text = "The username must be 3-20 characters long, consisting of letters, numbers and symbols.",
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
                                onValueChange = { input ->
                                    if (input.text.length <= 20)
                                        newName = input
                                },
                                placeholder = ""
                            )

                            Spacer(modifier = Modifier.height(scale.dp(104f)))

                            SecondaryButton(
                                paddingH = 104f,
                                paddingV = 24f,
                                text = "OK",
                                onClick = {
                                    if (content.isNotBlank() && (content.length >= 3)) {
                                        onConfirm(content)
                                        onDismiss()
                                    }
                                },
                                modifier = Modifier,
                                enabled = content.isNotBlank()
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
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String
) {
    val scale = RememberScaleConversion()

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(
            color = White,
            fontSize = scale.sp(26f),
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            letterSpacing = scale.sp(2f)
        ),
        singleLine = true,
        cursorBrush = SolidColor(White),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(scale.dp(64f))
                    .border(BorderStroke(scale.dp(1f), Orange))
                    .background(DarkBlue)
                    .padding(horizontal = scale.dp(20f)),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.text.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = TextStyle(
                            color = White,
                            fontSize = scale.sp(26f),
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = scale.sp(2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                innerTextField()
            }
        }
    )
}

private fun applyColorMatrixToBitmap(src: Bitmap, matrix: ColorMatrix): Bitmap {
    val config = src.config ?: Bitmap.Config.ARGB_8888
    val dest = createBitmap(src.width, src.height, config)

    val canvas = android.graphics.Canvas(dest)
    val paint = android.graphics.Paint()

    val androidMatrix = android.graphics.ColorMatrix(matrix.values)
    paint.colorFilter = android.graphics.ColorMatrixColorFilter(androidMatrix)

    canvas.drawBitmap(src, 0f, 0f, paint)
    return dest
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialogInstructions(
    onDismiss: () -> Unit
) {
    val scale = RememberScaleConversion()
    val interactionSource = remember { MutableInteractionSource() }

    val maxWidth = scale.dp(646f)
    val boxShape = CutCornerShape(scale.dp(24f))
    val closeButtonShape = CutCornerShape(topEnd = scale.dp(24f), bottomStart = scale.dp(24f))

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
                        .padding(start = scale.dp(28f), end = scale.dp(28f), bottom = scale.dp(28f), top = scale.dp(60f))
                        .clip(RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    GridBackground(Modifier.matchParentSize(), color = LightBlue, 14f)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = scale.dp(28f)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.height(scale.dp(66f)))

                        // Start
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(scale.dp(1060f)) // ex. 322f
                        ) {
                            CompositionLocalProvider(
                                LocalOverscrollFactory provides null
                            ) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(scale.dp(30f))
                                ) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(scale.dp(100f))
                                                .background(Color(0xFF183868)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Questions?",
                                                color = White,
                                                fontSize = scale.sp(30f),
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
                                        }
                                    }

                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(scale.dp(192f))
                                                .background(Color(0xFF323432))
                                                .padding(horizontal = scale.dp(22f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "All responses will be provided during the demo presentation.",
                                                color = White,
                                                fontSize = scale.sp(26f),
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
                                        }
                                    }

                                    // Remaining parts...
                                }
                            }
                        }
                        // End

                        Spacer(modifier = Modifier.height(scale.dp(18f)))
                    }
                }
            }

            Box(modifier = Modifier.zIndex(1f)) {
                Tab(134f, 32f, text = "Instructions")
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
fun DialogPassword(
    email: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = RememberScaleConversion()

    val boxShape = CutCornerShape(scale.dp(24f))
    val closeButtonShape = CutCornerShape(topEnd = scale.dp(24f), bottomStart = scale.dp(24f))
    val maxWidth = scale.dp(646f)

    var emailText by remember { mutableStateOf("") }

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
                    .padding(top = scale.dp(50f))
                    .fillMaxWidth()
                    .background(DarkBlue, shape = boxShape)
                    .border(BorderStroke(scale.dp(1f), Orange), shape = boxShape)
                    .clip(boxShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = scale.dp(28f), end = scale.dp(28f), bottom = scale.dp(28f), top = scale.dp(58f))
                        .clip(RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    GridBackground(Modifier.matchParentSize(), color = LightBlue, 14f)

                    Box(
                        modifier = Modifier
                            .padding(horizontal = scale.dp(44f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(scale.dp(124f)))

                            Text(
                                text = "Enter the email address",
                                color = White,
                                textAlign = TextAlign.Center,
                                fontSize = scale.sp(26f),
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

                            Spacer(modifier = Modifier.height(scale.dp(94f)))

                            BasicTextField(
                                value = emailText,
                                onValueChange = { emailText = it },
                                textStyle = TextStyle(
                                    color = White,
                                    fontSize = scale.sp(26f),
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = scale.sp(2f)
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Done
                                ),
                                cursorBrush = SolidColor(Color.Transparent),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(scale.dp(64f))
                                            .border(BorderStroke(scale.dp(1f), Orange))
                                            .background(DarkBlue)
                                            .padding(horizontal = scale.dp(24f)),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (emailText.isEmpty()) {
                                            Text(
                                                text = email,
                                                style = TextStyle(
                                                    color = Orange,
                                                    fontSize = scale.sp(26f),
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontWeight = FontWeight.Medium,
                                                    fontStyle = FontStyle.Italic,
                                                    letterSpacing = scale.sp(2f)
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(scale.dp(102f)))

                            SecondaryButton(
                                paddingH = 104f,
                                paddingV = 24f,
                                text = "OK",
                                onClick = {
                                    if (emailText.isNotBlank()) {
                                        onConfirm(emailText.trim())
                                    }
                                },
                                modifier = Modifier,
                                enabled = emailText.isNotBlank()
                            )

                            Spacer(modifier = Modifier.height(scale.dp(34f)))
                        }
                    }
                }
            }

            Box(modifier = Modifier.zIndex(1f)) {
                Tab(58f, 32f, text = "Password recovery")
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
fun DialogRegister(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = RememberScaleConversion()

    val boxShape = CutCornerShape(scale.dp(24f))
    val closeButtonShape = CutCornerShape(topEnd = scale.dp(24f), bottomStart = scale.dp(24f))
    val maxWidth = scale.dp(646f)

    var emailText by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }
    var confirmPasswordText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { }
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
                    .padding(top = scale.dp(50f))
                    .fillMaxWidth()
                    .background(DarkBlue, shape = boxShape)
                    .border(BorderStroke(scale.dp(1f), Orange), shape = boxShape)
                    .clip(boxShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = scale.dp(28f), end = scale.dp(28f), bottom = scale.dp(28f), top = scale.dp(58f))
                        .clip(RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    GridBackground(Modifier.matchParentSize(), color = LightBlue, 14f)

                    Box(
                        modifier = Modifier
                            .padding(horizontal = scale.dp(38f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(scale.dp(36f)))

                            Text(
                                text = "Users can provide their email address in order to use the same profile across multiple devices.",
                                color = White,
                                fontSize = scale.sp(26f),
                                textAlign = TextAlign.Center,
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
                                ),
                                modifier = Modifier.padding(horizontal = scale.dp(28f))
                            )

                            Spacer(modifier = Modifier.height(scale.dp(124f)))

                            Text(
                                text = "Email",
                                color = White,
                                textAlign = TextAlign.Center,
                                fontSize = scale.sp(26f),
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

                            Spacer(modifier = Modifier.height(scale.dp(20f)))

                            BasicTextField(
                                value = emailText,
                                onValueChange = { emailText = it },
                                textStyle = TextStyle(
                                    color = White,
                                    fontSize = scale.sp(26f),
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = scale.sp(2f)
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                cursorBrush = SolidColor(Color.Transparent),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(scale.dp(64f))
                                            .border(BorderStroke(scale.dp(1f), Orange))
                                            .background(DarkBlue)
                                            .padding(horizontal = scale.dp(24f)),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (emailText.isEmpty()) {
                                            Text(
                                                text = "Enter the email address...",
                                                style = TextStyle(
                                                    color = Orange,
                                                    fontSize = scale.sp(26f),
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontWeight = FontWeight.Medium,
                                                    fontStyle = FontStyle.Italic,
                                                    letterSpacing = scale.sp(2f)
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(scale.dp(114f)))

                            Text(
                                text = "Password",
                                color = White,
                                textAlign = TextAlign.Center,
                                fontSize = scale.sp(26f),
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

                            Spacer(modifier = Modifier.height(scale.dp(20f)))

                            BasicTextField(
                                value = passwordText,
                                onValueChange = { passwordText = it },
                                textStyle = TextStyle(
                                    color = White,
                                    fontSize = scale.sp(26f),
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = scale.sp(2f)
                                ),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Next
                                ),
                                cursorBrush = SolidColor(Color.Transparent),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(scale.dp(64f))
                                            .border(BorderStroke(scale.dp(1f), Orange))
                                            .background(DarkBlue)
                                            .padding(horizontal = scale.dp(24f)),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (passwordText.isEmpty()) {
                                            Text(
                                                text = "Enter the password...",
                                                style = TextStyle(
                                                    color = Orange,
                                                    fontSize = scale.sp(26f),
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontWeight = FontWeight.Medium,
                                                    fontStyle = FontStyle.Italic,
                                                    letterSpacing = scale.sp(2f)
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(scale.dp(38f)))

                            Text(
                                text = "Re-enter password",
                                color = White,
                                textAlign = TextAlign.Center,
                                fontSize = scale.sp(26f),
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

                            Spacer(modifier = Modifier.height(scale.dp(20f)))

                            BasicTextField(
                                value = confirmPasswordText,
                                onValueChange = { confirmPasswordText = it },
                                textStyle = TextStyle(
                                    color = White,
                                    fontSize = scale.sp(26f),
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = scale.sp(2f)
                                ),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                cursorBrush = SolidColor(Color.Transparent),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(scale.dp(64f))
                                            .border(BorderStroke(scale.dp(1f), Orange))
                                            .background(DarkBlue)
                                            .padding(horizontal = scale.dp(24f)),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (confirmPasswordText.isEmpty()) {
                                            Text(
                                                text = "Enter the password...",
                                                style = TextStyle(
                                                    color = Orange,
                                                    fontSize = scale.sp(26f),
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontWeight = FontWeight.Medium,
                                                    fontStyle = FontStyle.Italic,
                                                    letterSpacing = scale.sp(2f)
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(scale.dp(134f)))

                            SecondaryButton(
                                paddingH = 74f,
                                paddingV = 24f,
                                text = "Register",
                                onClick = {
                                    if (emailText.isNotBlank() &&
                                        passwordText.isNotBlank() &&
                                        confirmPasswordText.isNotBlank()) {

                                        onConfirm(emailText, passwordText, confirmPasswordText)
                                    }
                                },
                                modifier = Modifier,
                                enabled = emailText.isNotBlank() &&
                                        passwordText.isNotBlank() &&
                                        confirmPasswordText.isNotBlank()
                            )

                            Spacer(modifier = Modifier.height(scale.dp(36f)))
                        }
                    }
                }
            }

            Box(modifier = Modifier.zIndex(1f)) {
                Tab(58f, 32f, text = "Email submission")
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
fun DialogAccess(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    isLoading: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = RememberScaleConversion()

    val boxShape = CutCornerShape(scale.dp(24f))
    val closeButtonShape = CutCornerShape(topEnd = scale.dp(24f), bottomStart = scale.dp(24f))
    val maxWidth = scale.dp(646f)

    var emailText by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(interactionSource = interactionSource, indication = null) {  }
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
                    .padding(top = scale.dp(50f))
                    .fillMaxWidth()
                    .background(DarkBlue, shape = boxShape)
                    .border(BorderStroke(scale.dp(1f), Orange), shape = boxShape)
                    .clip(boxShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = scale.dp(28f), end = scale.dp(28f), bottom = scale.dp(28f), top = scale.dp(58f))
                        .clip(RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    GridBackground(Modifier.matchParentSize(), color = LightBlue, 14f)

                    Box(
                        modifier = Modifier
                            .padding(horizontal = scale.dp(36f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(scale.dp(38f)))

                            Text(
                                text = "Account sign-in. Warning: local data will be overwritten!",
                                color = White,
                                textAlign = TextAlign.Center,
                                fontSize = scale.sp(26f),
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
                                ),
                                modifier = Modifier.padding(horizontal = scale.dp(22f))
                            )

                            Spacer(modifier = Modifier.height(scale.dp(40f)))

                            Text(
                                text = "Email",
                                color = White,
                                textAlign = TextAlign.Center,
                                fontSize = scale.sp(26f),
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

                            Spacer(modifier = Modifier.height(scale.dp(20f)))

                            BasicTextField(
                                value = emailText,
                                onValueChange = { emailText = it },
                                textStyle = TextStyle(
                                    color = White,
                                    fontSize = scale.sp(26f),
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = scale.sp(2f)
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                cursorBrush = SolidColor(Color.Transparent),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(scale.dp(64f))
                                            .border(BorderStroke(scale.dp(1f), Orange))
                                            .background(DarkBlue)
                                            .padding(horizontal = scale.dp(24f)),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (emailText.isEmpty()) {
                                            Text(
                                                text = "Enter the email address...",
                                                style = TextStyle(
                                                    color = Orange,
                                                    fontSize = scale.sp(26f),
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontWeight = FontWeight.Medium,
                                                    fontStyle = FontStyle.Italic,
                                                    letterSpacing = scale.sp(2f)
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(scale.dp(52f)))

                            Text(
                                text = "Password",
                                color = White,
                                textAlign = TextAlign.Center,
                                fontSize = scale.sp(26f),
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

                            Spacer(modifier = Modifier.height(scale.dp(20f)))

                            BasicTextField(
                                value = passwordText,
                                onValueChange = { passwordText = it },
                                textStyle = TextStyle(
                                    color = White,
                                    fontSize = scale.sp(26f),
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = scale.sp(2f)
                                ),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                cursorBrush = SolidColor(Color.Transparent),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(scale.dp(64f))
                                            .border(BorderStroke(scale.dp(1f), Orange))
                                            .background(DarkBlue)
                                            .padding(horizontal = scale.dp(24f)),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (passwordText.isEmpty()) {
                                            Text(
                                                text = "Enter the password...",
                                                style = TextStyle(
                                                    color = Orange,
                                                    fontSize = scale.sp(26f),
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontWeight = FontWeight.Medium,
                                                    fontStyle = FontStyle.Italic,
                                                    letterSpacing = scale.sp(2f)
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(scale.dp(124f)))

                            SecondaryButton(
                                paddingH = 74f,
                                paddingV = 24f,
                                text = "Access",
                                onClick = {
                                    if (emailText.isNotBlank() && passwordText.isNotBlank()) {
                                        onConfirm(emailText, passwordText)
                                    }
                                },
                                modifier = Modifier,
                                enabled = !isLoading && emailText.isNotBlank() && passwordText.isNotBlank()
                            )

                            Spacer(modifier = Modifier.height(scale.dp(30f)))
                        }
                    }
                }
            }

            Box(modifier = Modifier.zIndex(1f)) {
                Tab(134f, 32f, text = "Login")
            }

            CloseButton(onClick = onDismiss, shape = closeButtonShape, modifier = Modifier.align(Alignment.TopEnd))
        }
    }
}

@Composable
fun DialogDeleteAccount(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = RememberScaleConversion()

    val boxShape = CutCornerShape(scale.dp(24f))
    val closeButtonShape = CutCornerShape(topEnd = scale.dp(24f), bottomStart = scale.dp(24f))
    val maxWidth = scale.dp(646f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(interactionSource = interactionSource, indication = null) {  }
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
                    .padding(top = scale.dp(50f))
                    .fillMaxWidth()
                    .background(DarkBlue, shape = boxShape)
                    .border(BorderStroke(scale.dp(1f), Orange), shape = boxShape)
                    .clip(boxShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = scale.dp(28f), end = scale.dp(28f), bottom = scale.dp(28f), top = scale.dp(58f))
                        .clip(RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    GridBackground(Modifier.matchParentSize(), color = LightBlue, 14f)

                    Box(
                        modifier = Modifier
                            .padding(horizontal = scale.dp(12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(scale.dp(60f)))

                            Text(
                                text = "Proceed with account termination? Please note that erased data cannot be restored.",
                                color = White,
                                textAlign = TextAlign.Center,
                                fontSize = scale.sp(26f),
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

                            Spacer(modifier = Modifier.height(scale.dp(132f)))

                            SecondaryButton(
                                paddingH = 78f,
                                paddingV = 24f,
                                text = "Remove",
                                onClick = {
                                    onConfirm()
                                    onDismiss()
                                },
                                modifier = Modifier
                            )

                            Spacer(modifier = Modifier.height(scale.dp(34f)))
                        }
                    }
                }
            }

            Box(modifier = Modifier.zIndex(1f)) {
                Tab(70f, 32f, text = "Delete the profile")
            }

            CloseButton(onClick = onDismiss, shape = closeButtonShape, modifier = Modifier.align(Alignment.TopEnd))
        }
    }
}

@Composable
fun DialogDifficulty(
    currentDifficulty: String,
    onDismiss: (String) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = RememberScaleConversion()

    val boxShape = CutCornerShape(scale.dp(24f))
    val closeButtonShape = CutCornerShape(topEnd = scale.dp(24f), bottomStart = scale.dp(24f))
    val maxWidth = scale.dp(646f)

    var selectedDifficulty by remember { mutableStateOf(currentDifficulty) }

    val description = when (selectedDifficulty) {
        "Easy" -> "Random search. Chance target shots."
        "Normal" -> "Random search. Smart target shots."
        "Hard" -> "Intelligent search. Smart target shots."
        else -> ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(interactionSource = interactionSource, indication = null) {  }
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
                    .padding(top = scale.dp(50f))
                    .fillMaxWidth()
                    .background(DarkBlue, shape = boxShape)
                    .border(BorderStroke(scale.dp(1f), Orange), shape = boxShape)
                    .clip(boxShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = scale.dp(6f), end = scale.dp(6f), bottom = scale.dp(28f), top = scale.dp(60f))
                        .clip(RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    GridBackground(Modifier.matchParentSize(), color = LightBlue, 14f)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = scale.dp(28f)),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(scale.dp(64f)))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DifficultyOption(
                                text = "Easy",
                                isSelected = selectedDifficulty == "Easy",
                                onClick = { selectedDifficulty = "Easy" }
                            )

                            DifficultyOption(
                                text = "Normal",
                                isSelected = selectedDifficulty == "Normal",
                                onClick = { selectedDifficulty = "Normal" }
                            )

                            DifficultyOption(
                                text = "Hard",
                                isSelected = selectedDifficulty == "Hard",
                                onClick = { selectedDifficulty = "Hard" }
                            )
                        }

                        Spacer(modifier = Modifier.height(scale.dp(62f)))

                        Text(
                            text = description,
                            color = White,
                            textAlign = TextAlign.Center,
                            fontSize = scale.sp(22f),
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
                            ),
                            modifier = Modifier.padding(horizontal = scale.dp(42f))
                        )

                        Spacer(modifier = Modifier.height(scale.dp(58f)))
                    }
                }
            }

            Box(modifier = Modifier.zIndex(1f)) {
                Tab(132f, 32f, text = "DIFFICULTY")
            }

            CloseButton(
                onClick = {
                    onDismiss(selectedDifficulty)
                },
                shape = closeButtonShape,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
private fun DifficultyOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = RememberScaleConversion()
    val shape = CutCornerShape(scale.dp(14f))

    val bgColor = if (isSelected) Orange else TransparentGrey.copy(alpha = 0.75f)

    Box(
        modifier = Modifier
            .width(scale.dp(166f))
            .height(scale.dp(78f))
            .background(bgColor, shape)
            .clip(shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = White,
            textAlign = TextAlign.Center,
            fontSize = scale.sp(22f),
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
}

@Composable
fun DialogFiringRules(
    currentRule: String,
    onDismiss: (String) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = RememberScaleConversion()

    val boxShape = CutCornerShape(scale.dp(24f))
    val closeButtonShape = CutCornerShape(topEnd = scale.dp(24f), bottomStart = scale.dp(24f))
    val maxWidth = scale.dp(646f)

    var selectedRule by remember { mutableStateOf(currentRule) }

    val description = when (selectedRule) {
        "Sequential hits" -> "Keep firing until a shot misses."
        "Chain attacks" -> "Hit as long as ships remain."
        "One shot" -> "Lone strike per turn."
        else -> ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(interactionSource = interactionSource, indication = null) {  }
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
                    .padding(top = scale.dp(50f))
                    .fillMaxWidth()
                    .background(DarkBlue, shape = boxShape)
                    .border(BorderStroke(scale.dp(1f), Orange), shape = boxShape)
                    .clip(boxShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = scale.dp(6f), end = scale.dp(6f), bottom = scale.dp(28f), top = scale.dp(60f))
                        .clip(RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    GridBackground(Modifier.matchParentSize(), color = LightBlue, 14f)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = scale.dp(82f)),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(scale.dp(64f)))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RuleOption(
                                text = "Sequential hits",
                                isSelected = selectedRule == "Sequential hits",
                                onClick = { selectedRule = "Sequential hits" }
                            )

                            Spacer(modifier = Modifier.width(scale.dp(30f)))

                            RuleOption(
                                text = "Chain attacks",
                                isSelected = selectedRule == "Chain attacks",
                                onClick = { selectedRule = "Chain attacks" }
                            )
                        }

                        Spacer(modifier = Modifier.height(scale.dp(44f)))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RuleOption(
                                text = "One shot",
                                isSelected = selectedRule == "One shot",
                                onClick = { selectedRule = "One shot" }
                            )
                        }

                        Spacer(modifier = Modifier.height(scale.dp(50f)))

                        Text(
                            text = description,
                            color = White,
                            textAlign = TextAlign.Center,
                            fontSize = scale.sp(24f),
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
                            ),
                            modifier = Modifier.padding(horizontal = scale.dp(38f))
                        )

                        Spacer(modifier = Modifier.height(scale.dp(104f)))
                    }
                }
            }

            Box(modifier = Modifier.zIndex(1f)) {
                Tab(78f, 32f, text = "FIRING RULES")
            }

            CloseButton(
                onClick = {
                    onDismiss(selectedRule)
                },
                shape = closeButtonShape,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
private fun RuleOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = RememberScaleConversion()
    val shape = CutCornerShape(scale.dp(14f))

    val bgColor = if (isSelected) Orange else TransparentGrey.copy(alpha = 0.75f)

    Box(
        modifier = Modifier
            .width(scale.dp(216f))
            .height(scale.dp(78f))
            .background(bgColor, shape)
            .clip(shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = White,
            textAlign = TextAlign.Center,
            fontSize = scale.sp(24f),
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
}

@Composable
fun DialogError(
    errorMessage: String,
    onDismiss: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = RememberScaleConversion()

    val boxShape = CutCornerShape(scale.dp(24f))
    val closeButtonShape = CutCornerShape(topEnd = scale.dp(24f), bottomStart = scale.dp(24f))
    val maxWidth = scale.dp(646f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(interactionSource = interactionSource, indication = null) {  }
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
                    .padding(top = scale.dp(50f))
                    .fillMaxWidth()
                    .background(DarkBlue, shape = boxShape)
                    .border(BorderStroke(scale.dp(1f), Orange), shape = boxShape)
                    .clip(boxShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = scale.dp(28f), end = scale.dp(28f), bottom = scale.dp(28f), top = scale.dp(60f))
                        .clip(RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    GridBackground(Modifier.matchParentSize(), color = LightBlue, 14f)

                    Box(
                        modifier = Modifier
                            .padding(start = scale.dp(60f), end = scale.dp(60f), top = scale.dp(158f), bottom = scale.dp(256f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = errorMessage,
                                color = White,
                                textAlign = TextAlign.Center,
                                fontSize = scale.sp(24f),
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
                    }
                }
            }

            Box(modifier = Modifier.zIndex(1f)) {
                Tab(66f, 32f, text = "Error message")
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
fun DialogDeployment(
    onConfirm: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = RememberScaleConversion()

    val boxShape = CutCornerShape(scale.dp(24f))
    val maxWidth = scale.dp(646f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Blue)
            .clickable(
                interactionSource = interactionSource,
                indication = null) {  }
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
                    .padding(top = scale.dp(50f))
                    .fillMaxWidth()
                    .background(DarkBlue, shape = boxShape)
                    .border(BorderStroke(scale.dp(1f), Orange), shape = boxShape)
                    .clip(boxShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .padding(vertical = scale.dp(26f), horizontal = scale.dp(12f))
                        .clip(RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    GridBackground(Modifier.matchParentSize(), color = LightBlue, 14f)

                    Box(
                        modifier = Modifier
                            .padding(horizontal = scale.dp(60f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(scale.dp(146f)))

                            Text(
                                text = "Player 2, deploy the fleet.",
                                color = White,
                                textAlign = TextAlign.Center,
                                fontSize = scale.sp(30f),
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = scale.sp(2f),
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    lineHeightStyle = LineHeightStyle(
                                        alignment = LineHeightStyle.Alignment.Center,
                                        trim = LineHeightStyle.Trim.Both
                                    ),
                                    shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)
                                )
                            )

                            Spacer(modifier = Modifier.height(scale.dp(212f)))

                            PrimaryButton(
                                paddingH = 112f,
                                paddingV = 28f,
                                text = "OK",
                                onClick = onConfirm,
                                enabled = true
                            )

                            Spacer(modifier = Modifier.height(scale.dp(80f)))
                        }
                    }
                }
            }

            Box(modifier = Modifier.zIndex(1f)) {
                Tab(134f, 32f, text = "Formation")
            }
        }
    }
}

@Composable
fun DialogTurn(
    turnNumber: Int,
    playerName: String,
    onConfirm: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = RememberScaleConversion()

    val boxShape = CutCornerShape(scale.dp(24f))
    val maxWidth = scale.dp(646f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Blue)
            .clickable(
                interactionSource = interactionSource,
                indication = null) {  }
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
                    .padding(top = scale.dp(50f))
                    .fillMaxWidth()
                    .background(DarkBlue, shape = boxShape)
                    .border(BorderStroke(scale.dp(1f), Orange), shape = boxShape)
                    .clip(boxShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .padding(vertical = scale.dp(26f), horizontal = scale.dp(12f))
                        .clip(RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    GridBackground(Modifier.matchParentSize(), color = LightBlue, 14f)

                    Box(
                        modifier = Modifier
                            .padding(horizontal = scale.dp(130f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(scale.dp(146f)))

                            Text(
                                text = "It is $playerName's move.",
                                color = White,
                                textAlign = TextAlign.Center,
                                fontSize = scale.sp(30f),
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = scale.sp(2f),
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    lineHeightStyle = LineHeightStyle(
                                        alignment = LineHeightStyle.Alignment.Center,
                                        trim = LineHeightStyle.Trim.Both
                                    ),
                                    shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)
                                )
                            )

                            Spacer(modifier = Modifier.height(scale.dp(212f)))

                            PrimaryButton(
                                paddingH = 112f,
                                paddingV = 28f,
                                text = "OK",
                                onClick = onConfirm,
                                enabled = true
                            )

                            Spacer(modifier = Modifier.height(scale.dp(80f)))
                        }
                    }
                }
            }

            Box(modifier = Modifier.zIndex(1f)) {
                Tab(
                    paddingH = 134f,
                    paddingV = 32f,
                    text = "Turn $turnNumber"
                )
            }
        }
    }
}

@Composable
fun DialogMatchResult(
    turnNumber: Int,
    winnerName: String,
    onConfirm: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = RememberScaleConversion()

    val boxShape = CutCornerShape(scale.dp(24f))
    val maxWidth = scale.dp(646f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(
                interactionSource = interactionSource,
                indication = null) {  }
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
                    .padding(top = scale.dp(50f))
                    .fillMaxWidth()
                    .background(DarkBlue, shape = boxShape)
                    .border(BorderStroke(scale.dp(1f), Orange), shape = boxShape)
                    .clip(boxShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .padding(vertical = scale.dp(26f), horizontal = scale.dp(12f))
                        .clip(RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    GridBackground(Modifier.matchParentSize(), color = LightBlue, 14f)

                    Box(
                        modifier = Modifier
                            .padding(horizontal = scale.dp(102f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(scale.dp(146f)))

                            Text(
                                text = "$winnerName wins.",
                                color = White,
                                textAlign = TextAlign.Center,
                                fontSize = scale.sp(30f),
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = scale.sp(2f),
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    lineHeightStyle = LineHeightStyle(
                                        alignment = LineHeightStyle.Alignment.Center,
                                        trim = LineHeightStyle.Trim.Both
                                    ),
                                    shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)
                                )
                            )

                            Spacer(modifier = Modifier.height(scale.dp(210f)))

                            PrimaryButton(
                                paddingH = 112f,
                                paddingV = 28f,
                                text = "OK",
                                onClick = onConfirm,
                                enabled = true
                            )

                            Spacer(modifier = Modifier.height(scale.dp(80f)))
                        }
                    }
                }
            }

            Box(modifier = Modifier.zIndex(1f)) {
                Tab(
                    paddingH = 134f,
                    paddingV = 32f,
                    text = "Turn $turnNumber"
                )
            }
        }
    }
}

@Composable
fun DialogRetire(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = RememberScaleConversion()

    val boxShape = CutCornerShape(scale.dp(24f))
    val closeButtonShape = CutCornerShape(topEnd = scale.dp(24f), bottomStart = scale.dp(24f))
    val maxWidth = scale.dp(574f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(
                interactionSource = interactionSource,
                indication = null) {  }
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
                    .padding(top = scale.dp(50f))
                    .fillMaxWidth()
                    .background(DarkBlue, shape = boxShape)
                    .border(BorderStroke(scale.dp(1f), Orange), shape = boxShape)
                    .clip(boxShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .padding(
                            start = scale.dp(28f),
                            end = scale.dp(28f),
                            bottom = scale.dp(28f),
                            top = scale.dp(60f)
                        )
                        .clip(RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    GridBackground(Modifier.matchParentSize(), color = LightBlue, 14f)

                    Box(
                        modifier = Modifier
                            .padding(horizontal = scale.dp(36f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(scale.dp(208f)))

                            Text(
                                text = "Proceed with game cancellation?",
                                color = White,
                                textAlign = TextAlign.Center,
                                fontSize = scale.sp(26f),
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = scale.sp(2f),
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    lineHeightStyle = LineHeightStyle(
                                        alignment = LineHeightStyle.Alignment.Center,
                                        trim = LineHeightStyle.Trim.Both
                                    ),
                                    shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)
                                )
                            )

                            Spacer(modifier = Modifier.height(scale.dp(200f)))

                            SecondaryButton(
                                paddingH = 84f,
                                paddingV = 24f,
                                text = "Retreat",
                                onClick = {
                                    onConfirm()
                                    onDismiss()
                                },
                                modifier = Modifier
                            )

                            Spacer(modifier = Modifier.height(scale.dp(36f)))
                        }
                    }
                }
            }

            Box(modifier = Modifier.zIndex(1f)) {
                Tab(
                    paddingH = 50f,
                    paddingV = 32f,
                    text = "Forfeit the match"
                )
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
fun DialogLoading(
    text: String = "waiting...",
    onDismiss: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = RememberScaleConversion()

    val boxShape = CutCornerShape(scale.dp(24f))
    val closeButtonShape = CutCornerShape(topEnd = scale.dp(24f), bottomStart = scale.dp(24f))
    val maxWidth = scale.dp(430f)

    val buttonSize = scale.dp(74f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(
                interactionSource = interactionSource,
                indication = null) { }
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
                    .fillMaxWidth()
                    .background(DarkBlue, shape = boxShape)
                    .border(BorderStroke(scale.dp(1f), Orange), shape = boxShape)
                    .clip(boxShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .padding(all = scale.dp(20f))
                        .clip(RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    GridBackground(Modifier.matchParentSize(), color = LightBlue, 14f)

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(scale.dp(26f)))

                        Radar(
                            modifier = Modifier
                                .size(scale.dp(126f))
                        )

                        Spacer(modifier = Modifier.height(scale.dp(80f)))

                        Text(
                            text = text,
                            color = White,
                            textAlign = TextAlign.Center,
                            fontSize = scale.sp(20f),
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = scale.sp(2f),
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                lineHeightStyle = LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Center,
                                    trim = LineHeightStyle.Trim.Both
                                ),
                                shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)
                            )
                        )

                        Spacer(modifier = Modifier.height(scale.dp(156f)))
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(buttonSize)
                        .background(DarkBlue, shape = closeButtonShape)
                        .border(BorderStroke(scale.dp(1f), Orange), shape = closeButtonShape)
                        .clip(closeButtonShape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onDismiss
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
        }
    }
}

@Composable
fun DialogChallengeResult(
    isWin: Boolean,
    timeElapsed: Long? = null,
    buttonText: String,
    onConfirm: () -> Unit,
    onRetry: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = RememberScaleConversion()

    val boxShape = CutCornerShape(scale.dp(24f))
    val closeButtonShape = CutCornerShape(topEnd = scale.dp(24f), bottomStart = scale.dp(24f))

    val maxWidth = scale.dp(646f)

    val buttonSize = scale.dp(74f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(
                interactionSource = interactionSource,
                indication = null) {  }
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
                    .padding(top = scale.dp(50f))
                    .fillMaxWidth()
                    .background(DarkBlue, shape = boxShape)
                    .border(BorderStroke(scale.dp(1f), Orange), shape = boxShape)
                    .clip(boxShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .padding(vertical = scale.dp(26f), horizontal = scale.dp(12f))
                        .clip(RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    GridBackground(Modifier.matchParentSize(), color = LightBlue, 14f)

                    Box(
                        modifier = Modifier
                            .padding(horizontal = scale.dp(102f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(scale.dp(146f)))

                            Text(
                                text = if (isWin) {
                                    "Mission completed" + if (timeElapsed != null) " in ${timeElapsed}s." else "."
                                } else
                                     "Mission failed" + if (timeElapsed != null) " in ${timeElapsed}s." else ".",
                                color = White,
                                textAlign = TextAlign.Center,
                                fontSize = scale.sp(30f),
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = scale.sp(2f),
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    lineHeightStyle = LineHeightStyle(
                                        alignment = LineHeightStyle.Alignment.Center,
                                        trim = LineHeightStyle.Trim.Both
                                    ),
                                    shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)
                                )
                            )

                            Spacer(modifier = Modifier.height(scale.dp(210f)))

                            PrimaryButton(
                                paddingH = 112f,
                                paddingV = 28f,
                                text = buttonText,
                                onClick = onConfirm,
                                enabled = true
                            )

                            Spacer(modifier = Modifier.height(scale.dp(80f)))
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(buttonSize)
                        .background(DarkBlue, shape = closeButtonShape)
                        .border(BorderStroke(scale.dp(1f), Orange), shape = closeButtonShape)
                        .clip(closeButtonShape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onRetry
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
                        tint = White,
                        modifier = Modifier.size(scale.dp(26f))
                    )
                }
            }

            Box(modifier = Modifier.zIndex(1f)) {
                Tab(
                    paddingH = 124f,
                    paddingV = 32f,
                    text = "Result"
                )
            }
        }
    }
}

@Composable
fun DialogFleetConfirm(
    onConfirm: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = RememberScaleConversion()

    val boxShape = CutCornerShape(scale.dp(24f))
    val maxWidth = scale.dp(646f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Blue)
            .clickable(
                interactionSource = interactionSource,
                indication = null) {  }
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
                    .padding(top = scale.dp(50f))
                    .fillMaxWidth()
                    .background(DarkBlue, shape = boxShape)
                    .border(BorderStroke(scale.dp(1f), Orange), shape = boxShape)
                    .clip(boxShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .padding(vertical = scale.dp(26f), horizontal = scale.dp(12f))
                        .clip(RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    GridBackground(Modifier.matchParentSize(), color = LightBlue, 14f)

                    Box(
                        modifier = Modifier
                            .padding(horizontal = scale.dp(60f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(scale.dp(146f)))

                            Text(
                                text = "Armada deployed correctly.",
                                color = White,
                                textAlign = TextAlign.Center,
                                fontSize = scale.sp(30f),
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = scale.sp(2f),
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    lineHeightStyle = LineHeightStyle(
                                        alignment = LineHeightStyle.Alignment.Center,
                                        trim = LineHeightStyle.Trim.Both
                                    ),
                                    shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)
                                )
                            )

                            Spacer(modifier = Modifier.height(scale.dp(212f)))

                            PrimaryButton(
                                paddingH = 112f,
                                paddingV = 28f,
                                text = "OK",
                                onClick = onConfirm,
                                enabled = true
                            )

                            Spacer(modifier = Modifier.height(scale.dp(80f)))
                        }
                    }
                }
            }

            Box(modifier = Modifier.zIndex(1f)) {
                Tab(134f, 32f, text = "Formation")
            }
        }
    }
}