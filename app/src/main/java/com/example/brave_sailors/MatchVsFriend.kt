package com.example.brave_sailors

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.brave_sailors.data.local.database.AppDatabase
import com.example.brave_sailors.data.remote.api.Flag
import com.example.brave_sailors.data.remote.api.RetrofitClient
import com.example.brave_sailors.data.repository.UserRepository
import com.example.brave_sailors.model.CellStatus
import com.example.brave_sailors.model.GridCell
import com.example.brave_sailors.model.MatchType
import com.example.brave_sailors.model.MatchVsFriendViewModel
import com.example.brave_sailors.model.MatchVsFriendViewModelFactory
import com.example.brave_sailors.model.ProfileViewModel
import com.example.brave_sailors.ui.components.DialogMatchResult
import com.example.brave_sailors.ui.components.DialogRetire
import com.example.brave_sailors.ui.components.FleetStatus
import com.example.brave_sailors.ui.components.GridLinesOverlay
import com.example.brave_sailors.ui.components.Match
import com.example.brave_sailors.ui.theme.LightGrey
import com.example.brave_sailors.ui.theme.Orange
import com.example.brave_sailors.ui.theme.White
import com.example.brave_sailors.ui.utils.RememberScaleConversion

val FRIEND_RED = Color(0xFFE00814)

@Composable
fun MatchVsFriendScreen(
    db: AppDatabase,
    profileViewModel: ProfileViewModel,
    opponent: LobbyPlayer,
    firingRule: String,
    matchId: String,
    availableFlags: List<Flag>,
    onHome: () -> Unit
) {
    val context = LocalContext.current

    val repository = remember {
        UserRepository(
            RetrofitClient.api,
            db.userDao(),
            db.fleetDao(),
            db.friendDao(),
            db.matchDao()
        )
    }

    val viewModel: MatchVsFriendViewModel = viewModel(
        factory = MatchVsFriendViewModelFactory(context, db.userDao(), db.fleetDao(), repository)
    )

    LaunchedEffect(Unit) {
        viewModel.initializeMatch(opponent, firingRule, matchId)

        profileViewModel.setMatchActive(
            context,
            matchId = matchId,
            opponentName = opponent.name,
            type = MatchType.FRIEND
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            profileViewModel.clearActiveMatch(context)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.onLifecyclePause()
            } else if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onLifecycleResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Modal(viewModel, availableFlags, onHome)
}

@Composable
private fun Modal(
    viewModel: MatchVsFriendViewModel,
    availableFlags: List<Flag>,
    onHome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scale = RememberScaleConversion()
    val maxWidth = scale.dp(720f)

    var showRetireDialog by remember { mutableStateOf(false) }
    var showResultDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (!uiState.isGameOver) {
            showRetireDialog = true
        } else {
            onHome()
        }
    }

    LaunchedEffect(uiState.isGameOver) {
        if (uiState.isGameOver) showResultDialog = true
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bgColor by animateColorAsState(targetValue = if (isPressed) Orange.copy(alpha = 0.75f) else Orange.copy(alpha = 0.90f), label = "BtnColor")
    val shrink by animateFloatAsState(targetValue = if (isPressed) 0.975f else 1f, label = "BtnScale")
    val buttonShape = RoundedCornerShape(scale.dp(14f))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = maxWidth),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(scale.dp(74f)))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = scale.dp(16f)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(scale.dp(92f))
                        .graphicsLayer {
                            scaleX = shrink
                            scaleY = shrink
                        }
                        .clip(buttonShape)
                        .background(bgColor, buttonShape)
                        .border(BorderStroke(scale.dp(1f), LightGrey.copy(alpha = 0.75f)), buttonShape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            enabled = !uiState.isGameOver,
                            onClick = { showRetireDialog = true }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = "Retire",
                        tint = White,
                        modifier = Modifier.size(scale.dp(64f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(scale.dp(42f)))

            AnimatedContent(
                targetState = uiState.isPlayerTurn,
                transitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(300)
                    ) togetherWith slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -fullWidth },
                        animationSpec = tween(200)
                    )
                },
                label = "HeaderAnim"
            ) { isMyTurn ->
                if (isMyTurn) {
                    val flag = availableFlags.find { it.code == uiState.playerUser?.countryCode }

                    Match(
                        name = uiState.playerUser?.name ?: "You",
                        avatarUrl = uiState.playerUser?.profilePictureUrl,
                        flagUrl = flag?.flagUrl,
                        wins = (uiState.playerUser?.wins ?: 0).toString(),
                        losses = (uiState.playerUser?.losses ?: 0).toString(),
                        isAi = false
                    )
                } else {
                    val flag = availableFlags.find { it.code == uiState.opponent?.countryCode }

                    Match(
                        name = uiState.opponent?.name ?: "Opponent",
                        avatarUrl = uiState.opponent?.avatarUrl,
                        flagUrl = flag?.flagUrl,
                        wins = (uiState.opponent?.wins ?: 0).toString(),
                        losses = (uiState.opponent?.losses ?: 0).toString(),
                        isAi = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(scale.dp(86f)))
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = scale.dp(34f + 188f))
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val targetComposition = if (uiState.isPlayerTurn) uiState.enemyShipsComposition else uiState.playerShipsComposition

                FleetStatus(
                    turn = uiState.turnNumber,
                    isPlayerTurn = uiState.isPlayerTurn,
                    shipsRemaining = targetComposition
                )

                Spacer(modifier = Modifier.height(scale.dp(28f)))

                val gridToShow = if (uiState.isPlayerTurn) uiState.enemyGrid else uiState.playerGrid
                val showShips = !uiState.isPlayerTurn || uiState.isGameOver
                val isInteractable = uiState.isPlayerTurn && !uiState.isGameOver

                GameGridFriend(
                    grid = gridToShow,
                    isInteractable = isInteractable,
                    showShips = showShips,
                    onCellClick = { row, column -> viewModel.onPlayerFire(row, column) }
                )
            }
        }

        if (showRetireDialog) {
            DialogRetire(
                onDismiss = { showRetireDialog = false },
                onConfirm = {
                    showRetireDialog = false
                    viewModel.onRetire()
                    onHome()
                }
            )
        }

        if (showResultDialog) {
            DialogMatchResult(
                turnNumber = uiState.turnNumber,
                winnerName = uiState.winnerName,
                onConfirm = {
                    showResultDialog = false
                    onHome()
                }
            )
        }
    }
}

@Composable
private fun GameGridFriend(
    grid: List<List<GridCell>>,
    isInteractable: Boolean,
    showShips: Boolean,
    onCellClick: (Int, Int) -> Unit
) {
    val scale = RememberScaleConversion()
    val cellSize = scale.dp(64f)
    val gridSize = 8

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = scale.dp(24f)),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier
                .width(scale.dp(24f))
                .padding(top = scale.dp(32f)),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            for (i in 1..gridSize) {
                Box(
                    modifier = Modifier
                        .height(cellSize)
                        .padding(
                            top = if (i == 1) scale.dp(2f) / 2 else 0.dp,
                            bottom = scale.dp(2f)
                        ),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = i.toString(),
                        color = LABEL_COLOR,
                        fontSize = scale.sp(22f),
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
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.padding(bottom = scale.dp(6f), start = scale.dp(2f))) {
                val letters = ('A'..'H').toList()
                for (i in 0 until gridSize) {
                    Box(
                        modifier = Modifier
                            .width(cellSize)
                            .padding(
                                start = if (i == 0) scale.dp(2f) / 2 else 0.dp,
                                end = scale.dp(2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = letters.getOrElse(i) { "?" }.toString(),
                            color = LABEL_COLOR,
                            fontSize = scale.sp(22f),
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
                    }
                }
            }

            Box(
                modifier = Modifier
                    .padding(scale.dp(2f) / 2)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF5A70B2), Color(0xFF3A5086), Color(0xFF24305C))
                        )
                    )
            ) {
                Column {
                    for (row in 0 until gridSize) {
                        Row {
                            for (col in 0 until gridSize) {
                                val cell = if (grid.isNotEmpty() && row < grid.size && col < grid[0].size) grid[row][col] else GridCell(row, col)

                                Box(
                                    modifier = Modifier
                                        .size(cellSize)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            enabled = isInteractable && cell.status != CellStatus.HIT && cell.status != CellStatus.MISS,
                                            onClick = { onCellClick(row, col) }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val isShipPresent = cell.status == CellStatus.SHIP || (cell.status == CellStatus.HIT && cell.shipId > 0)

                                    if (showShips && isShipPresent) {
                                        val colorKey = if (cell.originalSize > 0) cell.originalSize else 1

                                        val color = SHIP_COLORS[colorKey] ?: White

                                        Box(
                                            modifier = Modifier.fillMaxSize().padding(scale.dp(3f)).background(color)
                                        )
                                    }

                                    if (cell.status == CellStatus.HIT) {
                                        Icon(imageVector = Icons.Default.AddCircleOutline, contentDescription = "Hit", tint = FRIEND_RED, modifier = Modifier.size(scale.dp(42f)))
                                    }

                                    if (cell.status == CellStatus.MISS) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Miss", tint = White, modifier = Modifier.size(scale.dp(42f)))
                                    }
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(cellSize * gridSize)
                ) {
                    GridLinesOverlay(gridSize, cellSize)
                }
            }
        }
    }
}