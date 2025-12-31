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
import com.example.brave_sailors.ui.theme.DarkBlue
import com.example.brave_sailors.ui.theme.DeepBlue
import com.example.brave_sailors.ui.theme.LightBlue
import com.example.brave_sailors.ui.theme.Orange
import com.example.brave_sailors.ui.theme.White
import com.example.brave_sailors.ui.utils.RememberScaleConversion

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
                                        .fillMaxWidth()
                                        .clickable(interactionSource = interactionSource, indication = null) { applyFilter = !applyFilter },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Spacer(modifier = Modifier.width(scale.dp(30f)))

                                    Box(
                                        modifier = Modifier
                                            .size(scale.dp(28f))
                                            .background(Color.Transparent, CircleShape)
                                            .border(scale.dp(1f), White, CircleShape)
                                            .clip(CircleShape)
                                            .background(if (applyFilter) Color(0xFF08F804) else Color.Transparent)
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

    val maxWidth = scale.dp(648f) // 648px, etc.

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

    val maxWidth = scale.dp(648f)
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
                                .height(scale.dp(1060f))
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
                                                text = "Topic",
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
                                        }
                                    }

                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(scale.dp(194f))
                                                .background(Color(0xFF323432)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Description",
                                                color = White,
                                                fontSize = scale.sp(24f),
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
                Tab(124f, 32f, text = "Instructions")
            }

            CloseButton(
                onClick = onDismiss,
                shape = closeButtonShape,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}