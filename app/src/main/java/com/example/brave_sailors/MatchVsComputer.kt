package com.example.brave_sailors

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.brave_sailors.data.local.database.AppDatabase
import com.example.brave_sailors.data.local.database.entity.User
import com.example.brave_sailors.data.remote.api.Flag
import com.example.brave_sailors.data.remote.api.RetrofitClient
import com.example.brave_sailors.data.repository.UserRepository
import com.example.brave_sailors.model.CellStatus
import com.example.brave_sailors.model.GridCell
import com.example.brave_sailors.model.MatchVsComputerViewModel
import com.example.brave_sailors.model.MatchVsComputerViewModelFactory
import com.example.brave_sailors.ui.components.DialogMatchResult
import com.example.brave_sailors.ui.components.DialogRetire
import com.example.brave_sailors.ui.components.FleetStatus
import com.example.brave_sailors.ui.components.GridLinesOverlay
import com.example.brave_sailors.ui.components.Match
import com.example.brave_sailors.ui.theme.LightGrey
import com.example.brave_sailors.ui.theme.Orange
import com.example.brave_sailors.ui.theme.White
import com.example.brave_sailors.ui.utils.RememberScaleConversion

val Red = Color(0xFFD32F2F)

@Composable
fun MatchVsComputerScreen(
    db: AppDatabase,
    difficulty: String,
    firingRule: String,
    user: User?,
    flag: Flag?,
    onRetire: () -> Unit,
    onComplete: (Boolean) -> Unit
) {
    val context = LocalContext.current

    // --- SETUP VIEWMODEL ---
    val repository = remember {
        UserRepository(
            RetrofitClient.api,
            db.userDao(),
            db.fleetDao(),
            db.friendDao(),
            db.matchDao()
        )
    }

    val viewModel: MatchVsComputerViewModel = viewModel(
        factory = MatchVsComputerViewModelFactory(context, db.userDao(), db.fleetDao(), repository)
    )

    LaunchedEffect(Unit) {
        viewModel.initializeMatch(difficulty, firingRule)
    }
    // -----------------------

    Modal(viewModel, difficulty, firingRule, user, flag, onRetire, onComplete)
}

@Composable
private fun Modal(
    viewModel: MatchVsComputerViewModel,
    difficulty: String,
    firingRule: String,
    user: User?,
    flag: Flag?,
    onRetire: () -> Unit,
    onComplete: (Boolean) -> Unit
) {
    val scale = RememberScaleConversion()
    val maxWidth = scale.dp(720f)

    val uiState by viewModel.uiState.collectAsState()

    val isPlayerTurn = uiState.isPlayerTurn
    val turnNumber = uiState.turnNumber

    var showDialogRetire by remember { mutableStateOf(false) }
    var showDialogMatchResult by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isGameOver) {
        if (uiState.isGameOver) {
            showDialogMatchResult = true
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bgColor by animateColorAsState(targetValue = if (isPressed) Orange.copy(alpha = 0.75f) else Orange.copy(alpha = 0.90f), label = "ButtonColorAnimation")

    val shrink by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        label = "ButtonSqueezeAnimation"
    )

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
                        .border(
                            BorderStroke(scale.dp(1f), LightGrey.copy(alpha = 0.75f)),
                            buttonShape
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            enabled = !uiState.isGameOver,
                            onClick = { showDialogRetire = true }
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
                targetState = isPlayerTurn,
                transitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(300)
                    ) togetherWith slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -fullWidth },
                        animationSpec = tween(200)
                    )
                },
                label = "HeaderAnimation"
            ) { isPlayer ->
                if (isPlayer) {
                    Match(
                        name = user?.name ?: "Sailor",
                        avatarUrl = user?.profilePictureUrl,
                        flagUrl = flag?.flagUrl,
                        wins = user?.wins.toString(),
                        losses = user?.losses.toString(),
                        isAi = false
                    )
                } else {
                    // [ FIX ]: Check if there is a custom AI image.
                    // If 'aiAvatarPath' exists and is not the default placeholder, pass it to 'avatarUrl'.
                    // Otherwise pass null to let the Match component use the default robot icon.
                    val aiPath = user?.aiAvatarPath
                    val avatarToDisplay = if (aiPath != null && aiPath != "ic_ai_avatar_placeholder") aiPath else null

                    Match(
                        name = "$difficulty AI",
                        avatarUrl = avatarToDisplay, // Pass the path here
                        flagUrl = null,
                        wins = "-",
                        losses = "-",
                        isAi = true
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
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val targetFleetMap = if (isPlayerTurn) uiState.aiShipsComposition else uiState.playerShipsComposition

                FleetStatus(
                    turn = turnNumber,
                    isPlayerTurn = isPlayerTurn,
                    shipsRemaining = targetFleetMap
                )

                Spacer(modifier = Modifier.height(scale.dp(28f)))

                val gridToShow = if (isPlayerTurn) uiState.aiGrid else uiState.playerGrid
                val showShips = (!isPlayerTurn) || uiState.isGameOver

                GameGrid(
                    grid = gridToShow,
                    isPlayerTurn = isPlayerTurn,
                    showShips = showShips,
                    onCellClick = { r, c ->
                        if (isPlayerTurn && !uiState.isGameOver) {
                            viewModel.onPlayerFire(r, c)
                        }
                    }
                )
            }
        }

        // --- DIALOGS ---
        if (showDialogRetire) {
            DialogRetire(
                onDismiss = { showDialogRetire = false },
                onConfirm = {
                    showDialogRetire = false
                    viewModel.onRetire()
                    onRetire()
                }
            )
        }

        if (showDialogMatchResult) {
            val winnerName = uiState.winnerName

            DialogMatchResult(
                turnNumber = turnNumber,
                winnerName = winnerName,
                onConfirm = {
                    showDialogMatchResult = false
                    onComplete(uiState.isPlayerWinner)
                }
            )
        }
    }
}

@Composable
private fun GameGrid(
    grid: List<List<GridCell>>,
    isPlayerTurn: Boolean,
    showShips: Boolean,
    onCellClick: (Int, Int) -> Unit
) {
    val scale = RememberScaleConversion()
    val cellSize = scale.dp(64f)

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
            for (i in 1..GRID_SIZE) {
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

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .padding(bottom = scale.dp(6f), start = scale.dp(2f))
            ) {
                val letters = ('A'..'H').toList()
                for (i in 0 until GRID_SIZE) {
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

            Box(
                modifier = Modifier
                    .padding(scale.dp(2f) / 2)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF5A70B2),
                                Color(0xFF3A5086),
                                Color(0xFF24305C)
                            )
                        )
                    )
            ) {
                Column {
                    for (row in 0 until GRID_SIZE) {
                        Row {
                            for (col in 0 until GRID_SIZE) {
                                val cell = if (grid.isNotEmpty() && row < grid.size && col < grid[0].size) grid[row][col] else GridCell(row, col)

                                Box(
                                    modifier = Modifier
                                        .size(cellSize)
                                        .border(0.5.dp, Color.White.copy(alpha = 0.1f))
                                        .clickable(
                                            enabled = isPlayerTurn && cell.status != CellStatus.HIT && cell.status != CellStatus.MISS,
                                            onClick = { onCellClick(row, col) }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val isShipPresent = cell.status == CellStatus.SHIP || (cell.status == CellStatus.HIT && cell.shipId > 0)

                                    if (showShips && isShipPresent) {
                                        val colorKey = if (cell.shipId > 0) (cell.shipId % 4) + 1 else 1
                                        val color = SHIP_COLORS[colorKey] ?: White
                                        Box(
                                            modifier = Modifier.fillMaxSize().padding(3.dp).background(color, RoundedCornerShape(4.dp))
                                        )
                                    }

                                    if (cell.status == CellStatus.HIT) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Hit", tint = Red, modifier = Modifier.size(scale.dp(48f)))
                                    }

                                    if (cell.status == CellStatus.MISS) {
                                        Box(modifier = Modifier.size(scale.dp(16f)).clip(CircleShape).background(White.copy(alpha = 0.5f)))
                                    }
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(cellSize * GRID_SIZE)
                ) {
                    GridLinesOverlay(GRID_SIZE, cellSize)
                }
            }
        }
    }
}