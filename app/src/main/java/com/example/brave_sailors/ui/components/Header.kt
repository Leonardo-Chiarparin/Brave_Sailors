package com.example.brave_sailors.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.brave_sailors.R
import com.example.brave_sailors.model.ProfileViewModel
import com.example.brave_sailors.ui.theme.Grey
import com.example.brave_sailors.ui.theme.White
import com.example.brave_sailors.ui.utils.RememberScaleConversion

@Composable
fun Tab(paddingH: Float, paddingV: Float, text: String) {
    val scale = RememberScaleConversion()

    val paddingHorizontal = scale.dp(paddingH)
    val paddingVertical = scale.dp(paddingV)

    val cutSizeDp = scale.dp(20f)

    Box(
        modifier = Modifier
            .drawBehind {
                val cut = cutSizeDp.toPx()
                val h = size.height
                val w = size.width

                val path = Path().apply {
                    moveTo(0f, cut)
                    lineTo(0f, cut)
                    lineTo(cut, 0f)
                    lineTo(w - cut, 0f)
                    lineTo(w, cut)
                    lineTo(w, h - cut)
                    lineTo(w - cut, h)
                    lineTo(cut, h)
                    lineTo(0f, h - cut)
                    close()
                }
                drawPath(path, color = Grey)
            }
            .padding(horizontal = paddingHorizontal, vertical = paddingVertical),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = White,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = scale.sp(32f),
            letterSpacing = scale.sp(2f),
            maxLines = 1,
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

@Composable
fun Profile(viewModel: ProfileViewModel) {
    val flagList = viewModel.flagList.collectAsState()
    val availableFlags = flagList.value

    val user by viewModel.userState.collectAsState()

    val scale = RememberScaleConversion()

    val steelBgColor = remember {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF606060), Color(0xFF404040))
        )
    }

    val darkSteelBgColor = remember {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF3C3C3C), Color(0xFF303030))
        )
    }

    val orangeBgColor = remember {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF723E20), Color(0xFF965C30), Color(0xFF723E20))
        )
    }

    val beigeBgColor = remember {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFB8A08E), Color(0xFFC4AC92))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = scale.dp(4f)),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(scale.dp(52f))
                    .background(Color(0xFFA0A0A0))
            ) {  }

            Box(
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(scale.dp(184f))
                        .background(Color(0xFF243036)),
                    contentAlignment = Alignment.TopCenter
                ) {}

                Column(
                    modifier = Modifier
                        .padding(top = scale.dp(4f)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(scale.dp(124f)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(scale.dp(4f))
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(steelBgColor)
                                .drawBehind {
                                    val stroke = scale.dp(1f).toPx()
                                    val halfStroke = stroke / 2f
                                    val w = size.width
                                    val h = size.height

                                    drawLine(
                                        color = Color(0xFF9C9C9C),
                                        start = Offset(0f, halfStroke),
                                        end = Offset(w, halfStroke),
                                        strokeWidth = stroke
                                    )

                                    drawLine(
                                        color = Color(0xFF484848),
                                        start = Offset(0f, h - halfStroke),
                                        end = Offset(w, h - halfStroke),
                                        strokeWidth = stroke
                                    )

                                    drawLine(
                                        color = Color(0xFF484848),
                                        start = Offset(halfStroke, 0f),
                                        end = Offset(halfStroke, h),
                                        strokeWidth = stroke
                                    )

                                    drawLine(
                                        color = Color(0xFF484848),
                                        start = Offset(w - halfStroke, 0f),
                                        end = Offset(w - halfStroke, h),
                                        strokeWidth = stroke
                                    )
                                }
                                .padding(top = scale.dp(32f)),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Text(
                                text = user?.name ?: "Sailor",
                                color = White,
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

                        Box(
                            modifier = Modifier
                                .width(scale.dp(126f))
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            val painter = if (!user?.profilePictureUrl.isNullOrEmpty()) {
                                rememberAsyncImagePainter(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(user?.profilePictureUrl)
                                        .setParameter("key", user?.lastUpdated ?: System.currentTimeMillis())
                                        .build(),
                                    error = painterResource(R.drawable.ic_avatar_placeholder)
                                )
                            } else {
                                painterResource(id = R.drawable.ic_avatar_placeholder)
                            }

                            Image(
                                painter = painter,
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(steelBgColor)
                                .drawBehind {
                                    val stroke = scale.dp(1f).toPx()
                                    val halfStroke = stroke / 2f

                                    val w = size.width
                                    val h = size.height

                                    drawLine(
                                        color = Color(0xFF9C9C9C),
                                        start = Offset(0f, halfStroke),
                                        end = Offset(w, halfStroke),
                                        strokeWidth = stroke
                                    )

                                    drawLine(
                                        color = Color(0xFF484848),
                                        start = Offset(0f, h - halfStroke),
                                        end = Offset(w, h - halfStroke),
                                        strokeWidth = stroke
                                    )

                                    drawLine(
                                        color = Color(0xFF484848),
                                        start = Offset(halfStroke, 0f),
                                        end = Offset(halfStroke, h),
                                        strokeWidth = stroke
                                    )

                                    drawLine(
                                        color = Color(0xFF484848),
                                        start = Offset(w - halfStroke, 0f),
                                        end = Offset(w - halfStroke, h),
                                        strokeWidth = stroke
                                    )
                                }
                                .padding(all = scale.dp(14f)),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            val currentFlag = remember(user?.countryCode, availableFlags) {
                                if (user?.countryCode == null) {
                                    null
                                } else {
                                    availableFlags.find { it.code.trim().equals(user?.countryCode?.trim(), ignoreCase = true) }
                                }
                            }

                            if (currentFlag != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(currentFlag.flagUrl)
                                            .build()
                                    ),
                                    contentDescription = currentFlag.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .width(scale.dp(168f))
                                        .fillMaxHeight()
                                )
                            }
                            else
                                Box(modifier = Modifier.width(scale.dp(168f)).fillMaxHeight())
                        }
                    }

                    Spacer(modifier = Modifier.height(scale.dp(4f)))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(scale.dp(76f)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(scale.dp(4f))
                    ) {
                        val cutSizeDp = scale.dp(24f)

                        val shape = CutCornerShape(
                            bottomStart = cutSizeDp,
                            bottomEnd = cutSizeDp,
                            topStart = 0.dp,
                            topEnd = 0.dp
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(shape)
                                .background(darkSteelBgColor)
                                .drawBehind {
                                    val cutSize = cutSizeDp.toPx()
                                    val stroke = scale.dp(1f).toPx()
                                    val halfStroke = stroke / 2f
                                    val w = size.width
                                    val h = size.height

                                    drawLine(
                                        color = Color(0xFF8C8E90),
                                        start = Offset(0f, halfStroke),
                                        end = Offset(w, halfStroke),
                                        strokeWidth = stroke
                                    )

                                    val path = Path().apply {
                                        moveTo(halfStroke, 0f)
                                        lineTo(halfStroke, h - cutSize)
                                        lineTo(cutSize, h - halfStroke)
                                        lineTo(w - cutSize, h - halfStroke)
                                        lineTo(w - halfStroke, h - cutSize)
                                        lineTo(w - halfStroke, 0f)
                                    }

                                    drawPath(
                                        path = path,
                                        color = Color(0xFF1C2024),
                                        style = Stroke(
                                            width = 1f,
                                            cap = StrokeCap.Butt,
                                            join = StrokeJoin.Miter
                                        )
                                    )
                                }
                        ) {}

                        Box(
                            modifier = Modifier
                                .width(scale.dp(126f))
                                .fillMaxHeight()
                                .clip(shape)
                                .background(orangeBgColor)
                                .drawBehind {
                                    val cutSize = cutSizeDp.toPx()
                                    val stroke = scale.dp(1f).toPx()
                                    val halfStroke = stroke / 2f
                                    val w = size.width
                                    val h = size.height

                                    val path = Path().apply {
                                        moveTo(halfStroke, 0f)
                                        lineTo(halfStroke, h - cutSize)
                                        lineTo(cutSize, h - halfStroke)
                                        lineTo(w - cutSize, h - halfStroke)
                                        lineTo(w - halfStroke, h - cutSize)
                                        lineTo(w - halfStroke, 0f)
                                        close()
                                    }

                                    drawPath(
                                        path = path,
                                        color = Color(0xFF58443C),
                                        style = Stroke(
                                            width = 1f,
                                            cap = StrokeCap.Butt,
                                            join = StrokeJoin.Miter
                                        )
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(scale.dp(12f))
                                    .fillMaxHeight()
                                    .background(beigeBgColor)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(shape)
                                .background(darkSteelBgColor)
                                .drawBehind {
                                    val cutSize = cutSizeDp.toPx()
                                    val stroke = scale.dp(1f).toPx()
                                    val halfStroke = stroke / 2f
                                    val w = size.width
                                    val h = size.height

                                    drawLine(
                                        color = Color(0xFF8C8E90),
                                        start = Offset(0f, halfStroke),
                                        end = Offset(w, halfStroke),
                                        strokeWidth = stroke
                                    )

                                    val path = Path().apply {
                                        moveTo(halfStroke, 0f)
                                        lineTo(halfStroke, h - cutSize)
                                        lineTo(cutSize, h - halfStroke)
                                        lineTo(w - cutSize, h - halfStroke)
                                        lineTo(w - halfStroke, h - cutSize)
                                        lineTo(w - halfStroke, 0f)
                                    }

                                    drawPath(
                                        path = path,
                                        color = Color(0xFF1C2024),
                                        style = Stroke(
                                            width = 1f,
                                            cap = StrokeCap.Butt,
                                            join = StrokeJoin.Miter
                                        )
                                    )
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(
                                        top = scale.dp(8f),
                                        bottom = scale.dp(14f),
                                        start = scale.dp(28f),
                                        end = scale.dp(28f)
                                    ),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "TOTAL MATCHES",
                                    color = White,
                                    textAlign = TextAlign.Center,
                                    fontSize = scale.sp(16f),
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

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "WON\t\t\t${user?.wins ?: 0}",
                                        color = White,
                                        textAlign = TextAlign.Center,
                                        fontSize = scale.sp(18f),
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
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

                                    Text(
                                        "LOST\t\t\t${user?.losses ?: 0}",
                                        color = White,
                                        textAlign = TextAlign.Center,
                                        fontSize = scale.sp(18f),
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
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
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Match(
    name: String,
    avatarUrl: String?,
    flagUrl: String?,
    wins: String,
    losses: String,
    isAi: Boolean
) {
    val context = LocalContext.current
    val scale = RememberScaleConversion()

    val steelBgColor = remember {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF606060), Color(0xFF404040))
        )
    }

    val darkSteelBgColor = remember {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF3C3C3C), Color(0xFF303030))
        )
    }

    val orangeBgColor = remember {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF723E20), Color(0xFF965C30), Color(0xFF723E20))
        )
    }

    val beigeBgColor = remember {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFB8A08E), Color(0xFFC4AC92))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = scale.dp(4f)),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(scale.dp(52f))
                    .background(Color(0xFFA0A0A0))
            ) {  }

            Box(
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(scale.dp(184f))
                        .background(Color(0xFF243036)),
                    contentAlignment = Alignment.TopCenter
                ) {}

                Column(
                    modifier = Modifier
                        .padding(top = scale.dp(4f)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(scale.dp(124f)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(scale.dp(4f))
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(steelBgColor)
                                .drawBehind {
                                    val stroke = scale.dp(1f).toPx()
                                    val halfStroke = stroke / 2f
                                    val w = size.width
                                    val h = size.height

                                    drawLine(
                                        color = Color(0xFF9C9C9C),
                                        start = Offset(0f, halfStroke),
                                        end = Offset(w, halfStroke),
                                        strokeWidth = stroke
                                    )

                                    drawLine(
                                        color = Color(0xFF484848),
                                        start = Offset(0f, h - halfStroke),
                                        end = Offset(w, h - halfStroke),
                                        strokeWidth = stroke
                                    )

                                    drawLine(
                                        color = Color(0xFF484848),
                                        start = Offset(halfStroke, 0f),
                                        end = Offset(halfStroke, h),
                                        strokeWidth = stroke
                                    )

                                    drawLine(
                                        color = Color(0xFF484848),
                                        start = Offset(w - halfStroke, 0f),
                                        end = Offset(w - halfStroke, h),
                                        strokeWidth = stroke
                                    )
                                }
                                .padding(top = scale.dp(32f)),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Text(
                                text = name,
                                color = White,
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

                        Box(
                            modifier = Modifier
                                .width(scale.dp(126f))
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            val painter = if (!avatarUrl.isNullOrEmpty()) {
                                rememberAsyncImagePainter(
                                    model = ImageRequest.Builder(context)
                                        .data(avatarUrl)
                                        .build(),
                                    error = if (!isAi) painterResource(id = R.drawable.ic_avatar_placeholder) else painterResource(id = R.drawable.ic_ai_avatar_placeholder)
                                )
                            } else {
                                if (!isAi)
                                    painterResource(id = R.drawable.ic_avatar_placeholder)
                                else
                                    painterResource(id = R.drawable.ic_ai_avatar_placeholder)
                            }

                            Image(
                                painter = painter,
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(steelBgColor)
                                .drawBehind {
                                    val stroke = scale.dp(1f).toPx()
                                    val halfStroke = stroke / 2f

                                    val w = size.width
                                    val h = size.height

                                    drawLine(
                                        color = Color(0xFF9C9C9C),
                                        start = Offset(0f, halfStroke),
                                        end = Offset(w, halfStroke),
                                        strokeWidth = stroke
                                    )

                                    drawLine(
                                        color = Color(0xFF484848),
                                        start = Offset(0f, h - halfStroke),
                                        end = Offset(w, h - halfStroke),
                                        strokeWidth = stroke
                                    )

                                    drawLine(
                                        color = Color(0xFF484848),
                                        start = Offset(halfStroke, 0f),
                                        end = Offset(halfStroke, h),
                                        strokeWidth = stroke
                                    )

                                    drawLine(
                                        color = Color(0xFF484848),
                                        start = Offset(w - halfStroke, 0f),
                                        end = Offset(w - halfStroke, h),
                                        strokeWidth = stroke
                                    )
                                }
                                .padding(all = scale.dp(14f)),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            if (!isAi) {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        model = flagUrl
                                    ),
                                    contentDescription = "Flag",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .width(scale.dp(168f))
                                        .fillMaxHeight()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(scale.dp(4f)))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(scale.dp(76f)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(scale.dp(4f))
                    ) {
                        val cutSizeDp = scale.dp(24f)

                        val shape = CutCornerShape(
                            bottomStart = cutSizeDp,
                            bottomEnd = cutSizeDp,
                            topStart = 0.dp,
                            topEnd = 0.dp
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(shape)
                                .background(darkSteelBgColor)
                                .drawBehind {
                                    val cutSize = cutSizeDp.toPx()
                                    val stroke = scale.dp(1f).toPx()
                                    val halfStroke = stroke / 2f
                                    val w = size.width
                                    val h = size.height

                                    drawLine(
                                        color = Color(0xFF8C8E90),
                                        start = Offset(0f, halfStroke),
                                        end = Offset(w, halfStroke),
                                        strokeWidth = stroke
                                    )

                                    val path = Path().apply {
                                        moveTo(halfStroke, 0f)
                                        lineTo(halfStroke, h - cutSize)
                                        lineTo(cutSize, h - halfStroke)
                                        lineTo(w - cutSize, h - halfStroke)
                                        lineTo(w - halfStroke, h - cutSize)
                                        lineTo(w - halfStroke, 0f)
                                    }

                                    drawPath(
                                        path = path,
                                        color = Color(0xFF1C2024),
                                        style = Stroke(
                                            width = 1f,
                                            cap = StrokeCap.Butt,
                                            join = StrokeJoin.Miter
                                        )
                                    )
                                }
                        ) {}

                        Box(
                            modifier = Modifier
                                .width(scale.dp(126f))
                                .fillMaxHeight()
                                .clip(shape)
                                .background(orangeBgColor)
                                .drawBehind {
                                    val cutSize = cutSizeDp.toPx()
                                    val stroke = scale.dp(1f).toPx()
                                    val halfStroke = stroke / 2f
                                    val w = size.width
                                    val h = size.height

                                    val path = Path().apply {
                                        moveTo(halfStroke, 0f)
                                        lineTo(halfStroke, h - cutSize)
                                        lineTo(cutSize, h - halfStroke)
                                        lineTo(w - cutSize, h - halfStroke)
                                        lineTo(w - halfStroke, h - cutSize)
                                        lineTo(w - halfStroke, 0f)
                                        close()
                                    }

                                    drawPath(
                                        path = path,
                                        color = Color(0xFF58443C),
                                        style = Stroke(
                                            width = 1f,
                                            cap = StrokeCap.Butt,
                                            join = StrokeJoin.Miter
                                        )
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(scale.dp(12f))
                                    .fillMaxHeight()
                                    .background(beigeBgColor)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(shape)
                                .background(darkSteelBgColor)
                                .drawBehind {
                                    val cutSize = cutSizeDp.toPx()
                                    val stroke = scale.dp(1f).toPx()
                                    val halfStroke = stroke / 2f
                                    val w = size.width
                                    val h = size.height

                                    drawLine(
                                        color = Color(0xFF8C8E90),
                                        start = Offset(0f, halfStroke),
                                        end = Offset(w, halfStroke),
                                        strokeWidth = stroke
                                    )

                                    val path = Path().apply {
                                        moveTo(halfStroke, 0f)
                                        lineTo(halfStroke, h - cutSize)
                                        lineTo(cutSize, h - halfStroke)
                                        lineTo(w - cutSize, h - halfStroke)
                                        lineTo(w - halfStroke, h - cutSize)
                                        lineTo(w - halfStroke, 0f)
                                    }

                                    drawPath(
                                        path = path,
                                        color = Color(0xFF1C2024),
                                        style = Stroke(
                                            width = 1f,
                                            cap = StrokeCap.Butt,
                                            join = StrokeJoin.Miter
                                        )
                                    )
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(
                                        top = scale.dp(8f),
                                        bottom = scale.dp(14f),
                                        start = scale.dp(28f),
                                        end = scale.dp(28f)
                                    ),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "TOTAL MATCHES",
                                    color = White,
                                    textAlign = TextAlign.Center,
                                    fontSize = scale.sp(16f),
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

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "WON\t\t\t$wins",
                                        color = White,
                                        textAlign = TextAlign.Center,
                                        fontSize = scale.sp(18f),
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
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

                                    Text(
                                        "LOST\t\t\t$losses",
                                        color = White,
                                        textAlign = TextAlign.Center,
                                        fontSize = scale.sp(18f),
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
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
                        }
                    }
                }
            }
        }
    }
}