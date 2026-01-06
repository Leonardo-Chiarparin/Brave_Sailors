package com.example.brave_sailors

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
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
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.brave_sailors.data.local.database.entity.User
import com.example.brave_sailors.data.remote.api.Flag
import com.example.brave_sailors.model.ProfileViewModel
import com.example.brave_sailors.ui.theme.DarkBlue
import com.example.brave_sailors.ui.theme.Orange
import com.example.brave_sailors.ui.theme.TransparentGrey
import com.example.brave_sailors.ui.theme.White
import com.example.brave_sailors.ui.utils.RememberScaleConversion

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RankingsScreen(
    user: User?,
    availableFlags: List<Flag>,
    viewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    Modal(user, availableFlags, viewModel, onBack)
}

@Composable
private fun Modal(
    user: User?,
    availableFlags: List<Flag>,
    viewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scale = RememberScaleConversion()
    val interactionSource = remember { MutableInteractionSource() }
    val maxWidth = scale.dp(720f)
    val strokeDp = scale.dp(1f)

    val rankingList by viewModel.leaderboard.collectAsState()

    val rowHeight = scale.dp(74f)
    val spacerHeight = scale.dp(16f)
    val visibleItems = 7

    val listHeight = (rowHeight * visibleItems) + (spacerHeight * (visibleItems - 1)) - scale.dp(4f)

    val closeButtonShape = CutCornerShape(bottomStart = scale.dp(34f))

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
                    .padding(horizontal = scale.dp(22f), vertical = scale.dp(20f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(scale.dp(16f)))
                            .background(TransparentGrey.copy(alpha = 0.75f))
                            .padding(all = scale.dp(48f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Global",
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

                    Spacer(modifier = Modifier.height(scale.dp(62f)))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(listHeight)
                    ) {
                        if (!rankingList.isEmpty()) {
                            CompositionLocalProvider(
                                LocalOverscrollFactory provides null
                            ) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(spacerHeight),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    items(rankingList, key = { it.rank }) { player ->
                                        val playerFlag = availableFlags.find { it.code == player.countryCode }
                                        val flagUrl = playerFlag?.flagUrl

                                        val borderColor = if (user != null && player.id == user.id) Orange else White

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(rowHeight)
                                                .border(scale.dp(1f), borderColor)
                                                .background(Color(0xFF505058))
                                                .padding(horizontal = scale.dp(38f)),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "#${player.rank}",
                                                color = White,
                                                fontSize = scale.sp(20f),
                                                fontFamily = FontFamily.SansSerif,
                                                fontWeight = FontWeight.Medium,
                                                letterSpacing = scale.sp(2f),
                                                style = TextStyle(
                                                    platformStyle = PlatformTextStyle(
                                                        includeFontPadding = false
                                                    ),
                                                    lineHeightStyle = LineHeightStyle(
                                                        LineHeightStyle.Alignment.Center,
                                                        LineHeightStyle.Trim.Both
                                                    ),
                                                    shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)
                                                )
                                            )

                                            Spacer(modifier = Modifier.width(scale.dp(42f)))

                                            val avatarRequest = ImageRequest.Builder(context)
                                                .data(player.avatarUrl)
                                                .build()

                                            AsyncImage(
                                                model = avatarRequest,
                                                contentDescription = "Avatar",
                                                placeholder = painterResource(R.drawable.ic_avatar_placeholder),
                                                error = painterResource(R.drawable.ic_avatar_placeholder),
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(scale.dp(62f))
                                            )

                                            Spacer(modifier = Modifier.width(scale.dp(8f)))

                                            AsyncImage(
                                                model = ImageRequest.Builder(context).data(flagUrl)
                                                    .build(),
                                                contentDescription = "Flag",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .width(scale.dp(48f))
                                                    .height(scale.dp(32f))
                                            )

                                            Spacer(modifier = Modifier.width(scale.dp(12f)))

                                            Text(
                                                text = player.name,
                                                color = White,
                                                fontSize = scale.sp(18f),
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f),
                                                fontFamily = FontFamily.SansSerif,
                                                letterSpacing = scale.sp(2f),
                                                style = TextStyle(
                                                    platformStyle = PlatformTextStyle(
                                                        includeFontPadding = false
                                                    ),
                                                    lineHeightStyle = LineHeightStyle(
                                                        LineHeightStyle.Alignment.Center,
                                                        LineHeightStyle.Trim.Both
                                                    ),
                                                    shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)
                                                )
                                            )

                                            Text(
                                                text = player.score + " pt",
                                                color = White,
                                                fontSize = scale.sp(18f),
                                                fontFamily = FontFamily.SansSerif,
                                                fontWeight = FontWeight.Medium,
                                                letterSpacing = scale.sp(2f),
                                                style = TextStyle(
                                                    platformStyle = PlatformTextStyle(
                                                        includeFontPadding = false
                                                    ),
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
                        }
                    }

                    Spacer(modifier = Modifier.height(scale.dp(54f)))
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
                    modifier = Modifier.size(scale.dp(28f))
                )
            }
        }
    }
}