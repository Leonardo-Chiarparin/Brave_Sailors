package com.example.brave_sailors

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
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
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.example.brave_sailors.ui.theme.DarkBlue
import com.example.brave_sailors.ui.theme.Orange
import com.example.brave_sailors.ui.theme.TransparentGrey
import com.example.brave_sailors.ui.theme.White
import com.example.brave_sailors.ui.utils.RememberScaleConversion

data class RankingPlayer(
    val rank: Int,
    val name: String,
    val score: String,
    val countryCode: String, // es. "IT", "US"
    val avatarUrl: String?
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RankingsScreen(
    user: User?,
    availableFlags: List<Flag>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scale = RememberScaleConversion()
    val interactionSource = remember { MutableInteractionSource() }
    val maxWidth = scale.dp(720f)
    val strokeDp = scale.dp(1f)

    val rowHeight = scale.dp(74f)
    val spacerHeight = scale.dp(16f)
    val visibleItems = 7

    val listHeight = (rowHeight * visibleItems) + (spacerHeight * (visibleItems - 1)) - scale.dp(4f)

    val closeButtonShape = CutCornerShape(bottomStart = scale.dp(34f))

    // [ TO - DO ]: Modify them to be managed dynamically (max. 25 items)
    // Static entities
    val rankingList = listOf(
        RankingPlayer(1, "PlayerOne", "53.692.728", "PS", null),
        RankingPlayer(2, "Horace Nelson", "50.422.871", "GB", null),
        RankingPlayer(3, "Drybcius", "42.890.304", "PL", null),
        RankingPlayer(4, "MAGA !!!", "42.871.096", "US", null),
        RankingPlayer(5, "Soozy78#YES2", "40.963.814", "SC", null),
        RankingPlayer(6, "popeye epinard", "37.501.503", "FR", null),
        RankingPlayer(7, "TYR", "37.261.448", "GB", null),
        RankingPlayer(8, "Sailor Moon", "35.100.000", "JP", null),
        RankingPlayer(9, user?.name ?: "Sailor", "10.000", user?.countryCode ?: "IT", user?.profilePictureUrl)
    )

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
                        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(spacerHeight),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                itemsIndexed(rankingList) { _, player ->
                                    val isCurrentUser = user != null && player.name == user.name
                                    val borderColor = if (isCurrentUser) Orange else White

                                    val playerFlag = availableFlags.find { it.code == player.countryCode }
                                    val flagUrl = playerFlag?.flagUrl

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
                                            text = "#" + player.rank.toString(),
                                            color = White,
                                            fontSize = scale.sp(20f),
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
                                            model = ImageRequest.Builder(context).data(flagUrl).build(),
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
                                                platformStyle = PlatformTextStyle(includeFontPadding = false),
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