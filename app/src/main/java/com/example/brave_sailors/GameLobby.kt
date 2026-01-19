package com.example.brave_sailors

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.brave_sailors.data.local.database.AppDatabase
import com.example.brave_sailors.data.remote.api.Flag
import com.example.brave_sailors.model.GameLobbyViewModel
import com.example.brave_sailors.model.GameLobbyViewModelFactory
import com.example.brave_sailors.ui.components.DialogLoading
import com.example.brave_sailors.ui.components.GridBackground
import com.example.brave_sailors.ui.theme.DarkGrey
import com.example.brave_sailors.ui.theme.DeepBlue
import com.example.brave_sailors.ui.theme.Orange
import com.example.brave_sailors.ui.theme.TransparentGrey
import com.example.brave_sailors.ui.theme.White
import com.example.brave_sailors.ui.utils.RememberScaleConversion

data class LobbyPlayer(
    val id: String,
    val name: String,
    val countryCode: String,
    val avatarUrl: String? = null,
    val wins: Int = 0,
    val losses: Int = 0
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameLobbyScreen(
    db: AppDatabase,
    availableFlags: List<Flag>,
    selectedFiringRule: String,
    onBack: () -> Unit,
    onMatchStart: (LobbyPlayer, String) -> Unit
) {
    val viewModel: GameLobbyViewModel = viewModel(
        factory = GameLobbyViewModelFactory(db.userDao(), db.friendDao())
    )

    Modal(viewModel, availableFlags, selectedFiringRule, onBack, onMatchStart)
}

@Composable
private fun Modal(
    viewModel: GameLobbyViewModel,
    availableFlags: List<Flag>,
    selectedFiringRule: String,
    onBack: () -> Unit,
    onMatchStart: (LobbyPlayer, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.matchFound, uiState.activeMatchId) {
        if (uiState.matchFound && uiState.selectedOpponent != null && uiState.activeMatchId != null) {
            onMatchStart(uiState.selectedOpponent!!, uiState.activeMatchId!!)
            viewModel.onMatchStartedConsumed()
        }
    }

    GameLobbyContent(
        friendsList = uiState.friends,
        availableFlags = availableFlags,
        onBack = onBack,
        onChosenPlayer = { player ->
            viewModel.onSelectOpponent(player, selectedFiringRule)
        }
    )

    if (uiState.isMatching) {
        DialogLoading(
            text = "Waiting for ${uiState.selectedOpponent?.name} to accept...",
            onDismiss = {
                viewModel.onCancelMatching()
            }
        )
    }
}

@Composable
private fun GameLobbyContent(
    friendsList: List<LobbyPlayer>,
    availableFlags: List<Flag>,
    onBack: () -> Unit,
    onChosenPlayer: (LobbyPlayer) -> Unit
) {
    val scale = RememberScaleConversion()

    val closeButtonShape = CutCornerShape(bottomStart = scale.dp(24f), topEnd = scale.dp(24f))
    val maxWidth = scale.dp(720f)

    var searchText by remember { mutableStateOf("") }
    var appliedFilter by remember { mutableStateOf("") }

    val filteredList = if (appliedFilter.isBlank()) friendsList else friendsList.filter {
        it.name.contains(appliedFilter, ignoreCase = true)
    }

    val baseColor = TransparentGrey.copy(alpha = 0.75f)
    val pressedColor = TransparentGrey.copy(alpha = 0.5f)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bgColor by animateColorAsState(
        targetValue = if (isPressed) pressedColor else baseColor,
        label = "ButtonColorAnimation"
    )

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
                .padding(top = scale.dp(238f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = scale.dp(8f)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(scale.dp(108f)))

                    Text(
                        text = "PLAY WITH FRIENDS",
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

                    Spacer(modifier = Modifier.weight(1f))

                    Box(
                        modifier = Modifier
                            .background(Orange, shape = closeButtonShape)
                            .border(scale.dp(1f), White, shape = closeButtonShape)
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

                Spacer(modifier = Modifier.height(scale.dp(16f)))

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DeepBlue)
                        .drawWithContent {
                            drawContent()

                            val h = size.height

                            val stroke = scale.dp(2f).toPx()
                            val halfStroke = stroke / 2f

                            drawLine(
                                color = White,
                                start = Offset(halfStroke, halfStroke),
                                end = Offset(halfStroke, h - halfStroke),
                                strokeWidth = stroke
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    GridBackground(Modifier.matchParentSize(), color = DarkGrey, 14f)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(scale.dp(88f))
                        ) {
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
                                            .width(scale.dp(476f))
                                            .fillMaxHeight()
                                            .border(BorderStroke(scale.dp(1f), White))
                                            .background(Color.Transparent)
                                            .padding(horizontal = scale.dp(24f)),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (searchText.isEmpty()) {
                                            Text(
                                                text = "Provide the username...",
                                                style = TextStyle(
                                                    color = White,
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

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(bgColor)
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null
                                    ) {
                                        appliedFilter = searchText
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Search",
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

                        CompositionLocalProvider(
                            LocalOverscrollFactory provides null
                        ) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                contentPadding = PaddingValues(vertical = scale.dp(58f), horizontal = scale.dp(16f)),
                                verticalArrangement = Arrangement.spacedBy(scale.dp(44f)),
                                horizontalArrangement = Arrangement.spacedBy(scale.dp(44f)),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = scale.dp(6f))
                            ) {
                                items(filteredList) { player ->
                                    PlayerCard(
                                        player = player,
                                        availableFlags = availableFlags,
                                        onClick = { onChosenPlayer(player) }
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
private fun PlayerCard(
    player: LobbyPlayer,
    availableFlags: List<Flag>,
    onClick: () -> Unit
) {
    val scale = RememberScaleConversion()
    val context = LocalContext.current
    val cardShape = CutCornerShape(scale.dp(16f))

    val playerFlag = availableFlags.find { it.code == player.countryCode }
    val flagUrl = playerFlag?.flagUrl

    val baseColor = TransparentGrey.copy(alpha = 0.75f)
    val pressedColor = TransparentGrey.copy(alpha = 0.5f)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bgColor by animateColorAsState(
        targetValue = if (isPressed) pressedColor else baseColor,
        label = "ButtonColorAnimation"
    )

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

    val cutSizeDp = scale.dp(8f)

    val shape = CutCornerShape(
        bottomStart = cutSizeDp,
        bottomEnd = cutSizeDp,
        topStart = 0.dp,
        topEnd = 0.dp
    )

    Box(
        modifier = Modifier
            .height(scale.dp(294f))
            .background(bgColor, shape = cardShape)
            .clip(cardShape)
            .clickable (
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = scale.dp(4f)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(scale.dp(16f)))

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(flagUrl)
                    .build(),
                contentDescription = "Flag",
                modifier = Modifier
                    .width(scale.dp(50f))
                    .height(scale.dp(28f)),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_flag_italy)
            )

            Spacer(modifier = Modifier.height(scale.dp(18f)))

            Box(
                modifier = Modifier
                    .size(scale.dp(98f)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(player.avatarUrl)
                        .build(),
                    contentDescription = "Avatar",
                    placeholder = painterResource(R.drawable.ic_avatar_placeholder),
                    error = painterResource(R.drawable.ic_avatar_placeholder),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(scale.dp(18f)))

            Box(
                modifier = Modifier
                    .width(scale.dp(40f))
                    .height(scale.dp(28f))
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
                        .width(scale.dp(6f))
                        .fillMaxHeight()
                        .background(beigeBgColor)
                )
            }

            Spacer(modifier = Modifier.height(scale.dp(18f)))

            Text(
                text = player.name,
                color = White,
                fontSize = scale.sp(22f),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.SansSerif,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
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