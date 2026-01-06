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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import com.example.brave_sailors.data.local.database.entity.User
import com.example.brave_sailors.data.remote.api.Flag
import com.example.brave_sailors.ui.components.DialogMatchResult
import com.example.brave_sailors.ui.components.DialogRetire
import com.example.brave_sailors.ui.components.FleetStatus
import com.example.brave_sailors.ui.components.GridLinesOverlay
import com.example.brave_sailors.ui.components.Match
import com.example.brave_sailors.ui.theme.LightGrey
import com.example.brave_sailors.ui.theme.Orange
import com.example.brave_sailors.ui.theme.White
import com.example.brave_sailors.ui.utils.RememberScaleConversion

@Composable
fun MatchVsComputerScreen(
    difficulty: String, // such difficulty manages the AI's behavior ( using random search, DQN, etc. )
    firingRule: String, // this parameter defines how many times a player has to shoot ( once, up to the opponent's remaining ships, until he/she hits something )
    user: User?,
    flag: Flag?,
    onRetire: () -> Unit,
    onComplete: (Boolean) -> Unit
) {
    Modal(difficulty, firingRule, user, flag, onRetire, onComplete)
}

@Composable
private fun Modal(
    difficulty: String,
    firingRule: String,
    user: User?,
    flag: Flag?,
    onRetire: () -> Unit,
    onComplete: (Boolean) -> Unit
) {
    val scale = RememberScaleConversion()
    val maxWidth = scale.dp(720f)

    // [ TO - DO ]: Implement a real turn exchange mechanism, the following is just for example
    var isPlayerTurn by remember { mutableStateOf(false) }
    val turnNumber by remember { mutableIntStateOf(1) }

    var showDialogRetire by remember { mutableStateOf(false) }
    var showDialogMatchResult by remember { mutableStateOf(false) }

    var isPlayerWinner by remember { mutableStateOf(false) }

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
                            enabled = true, // it's always possible to give up ( for this case only, as well as the one related to the match via Lobby )
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
                // [ TO - DO ]: Adjust the player's statistics
                if (isPlayer)
                    Match(
                        name = user?.name ?: "Sailor",
                        avatarUrl = user?.profilePictureUrl,
                        flagUrl = flag?.flagUrl,
                        wins = (0).toString(),
                        losses = (0).toString(),
                        isAi = false
                    )
                else
                    Match(
                        name = "$difficulty AI",
                        avatarUrl = null, // Default avatar (ic_ai_avatar_placeholder)
                        flagUrl = null,
                        wins = "-",
                        losses = "-",
                        isAi = true
                    )
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
                // [ TO - DO ]: It must display the number of the opposing ships according to the turn
                FleetStatus(
                    turn = turnNumber,
                    isPlayerTurn = isPlayerTurn
                )

                Spacer(modifier = Modifier.height(scale.dp(28f)))

                GameGrid()
            }
        }

        // --- DIALOGS ---
        if (showDialogRetire) {
            DialogRetire(
                onDismiss = { showDialogRetire = false },
                onConfirm = {
                    showDialogRetire = false
                    onRetire()
                }
            )
        }

        if (showDialogMatchResult) {
            val winnerName = if (isPlayerWinner) (user?.name ?: "Sailor") else "$difficulty AI"

            DialogMatchResult(
                turnNumber = turnNumber,
                winnerName = winnerName,
                onConfirm = {
                    showDialogMatchResult = false
                    onComplete(isPlayerWinner)
                }
            )
        }
    }
}

@Composable
private fun GameGrid() {
    val scale = RememberScaleConversion()

    val cellSize = scale.dp(64f)

    // [ NOTE ]: The grid must have no open-close animation ( while being replaced across the turns depending on the current player )
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
                Box(
                    modifier = Modifier
                        .size(cellSize * GRID_SIZE)
                ) {
                    // -- PLACEMENT ( if the AI's turn is ongoing ) --

                    GridLinesOverlay(GRID_SIZE, cellSize)
                }
            }
        }
    }
}