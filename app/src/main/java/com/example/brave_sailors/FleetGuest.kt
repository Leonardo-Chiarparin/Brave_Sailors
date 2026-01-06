package com.example.brave_sailors

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
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
import com.example.brave_sailors.ui.components.BeginButton
import com.example.brave_sailors.ui.components.DialogDeployment
import com.example.brave_sailors.ui.components.FleetButton
import com.example.brave_sailors.ui.components.GridBackground
import com.example.brave_sailors.ui.components.GridLinesOverlay
import com.example.brave_sailors.ui.components.ReturnButton
import com.example.brave_sailors.ui.theme.DarkGrey
import com.example.brave_sailors.ui.theme.DeepBlue
import com.example.brave_sailors.ui.theme.White
import com.example.brave_sailors.ui.utils.RememberScaleConversion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FleetGuestScreen(
    firingRule: String,
    onBack: () -> Unit,
    onStartMatch: (String) -> Unit
) {
    Modal(firingRule, onBack, onStartMatch)
}

@Composable
private fun Modal(
    firingRule: String,
    onBack: () -> Unit,
    onStartMatch: (String) -> Unit
) {
    val scale = RememberScaleConversion()
    val scope = rememberCoroutineScope()

    // val density = LocalDensity.current

    val cellSize = scale.dp(64f)

    val maxWidth = scale.dp(720f)

    val snackBarHostState = remember { SnackbarHostState() }

    /*
        var boardOffset by remember { mutableStateOf(Offset.Unspecified) }
        var boardCellPx by remember { mutableFloatStateOf(0f) }
     */

    var buttonsVisible by remember { mutableStateOf(false) }

    var showDialogDeployment by remember { mutableStateOf(false) }

    fun triggerExit(callback: () -> Unit) {
        scope.launch {
            buttonsVisible = false
            delay(200)
            callback()
        }
    }

    LaunchedEffect(Unit) {
        showDialogDeployment = true
    }

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
            Spacer(modifier = Modifier.height(scale.dp(294f)))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = scale.dp(28f)),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(scale.dp(28f))
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
                                /*
                                    .onGloballyPositioned { coordinates ->
                                        boardOffset = coordinates.positionInWindow()
                                        boardCellPx = with(density) { cellSize.toPx() }
                                    }
                                */
                                .size(cellSize * GRID_SIZE)
                        ) {
                            // -- PREVIEW --

                            // -- PLACEMENT --

                            GridLinesOverlay(GRID_SIZE, cellSize)
                        }
                    }
                }
            }

            // If the user has placed all the ships on the grid, then the space between the grid and buttons should be expanded as follows
            // if (state.shipsToPlace.isNotEmpty())
                Spacer(Modifier.height(scale.dp(60f)))
            // else
            //    Spacer(Modifier.height(scale.dp(102f)))

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FleetButton(
                    paddingH = 34f,
                    paddingV = 24f,
                    text = "AUTO",
                    enabled = true,
                    onClick = {  }
                )

                Spacer(modifier = Modifier.width(scale.dp(168f)))

                FleetButton(
                    paddingH = 28f,
                    paddingV = 24f,
                    text = "RESET",
                    enabled = true, // state.placedShips.isNotEmpty()
                    onClick = {  } // onReset
                )
            }

            Spacer(Modifier.height(scale.dp(28f)))

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FleetButton(
                    paddingH = 28f,
                    paddingV = 24f,
                    text = "PIVOT",
                    enabled = true, // dragState.isDragging
                    onClick = {  } // onRotate
                )
            }
        }

        // If the guest has placed all the ships on the grid, then the "DeepBlue" section must be hidden
        // if (state.shipsToPlace.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = scale.dp(20f + 188f))
                    .fillMaxWidth()
                    .background(DeepBlue)
                    .drawBehind {
                        val h = size.height
                        val w = size.width

                        val stroke = scale.dp(1f).toPx()
                        val halfStroke = stroke / 2f

                        drawLine(White, Offset(0f, halfStroke), Offset(w, halfStroke), stroke)

                        drawLine(White, Offset(0f, h - halfStroke), Offset(w, h - halfStroke), stroke)
                    }
                    .padding(vertical = scale.dp(26f), horizontal = scale.dp(32f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    GridBackground(Modifier.matchParentSize(), color = DarkGrey, 14f)

                    CompositionLocalProvider(
                        LocalOverscrollFactory provides null
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = scale.dp(20f))
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = scale.dp(62f)),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // groupedShips

                            // ExactDraggableShipItem
                        }
                    }
                }
            }
        // }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = scale.dp(54f), start = scale.dp(42f), end = scale.dp(42f))
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                AnimatedVisibility(
                    visible = buttonsVisible,
                    enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                    exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(200)) + fadeOut(animationSpec = tween(200))
                ) {
                    ReturnButton(
                        text = "Return",
                        onClick = { triggerExit(onBack) }
                    )
                }

                AnimatedVisibility(
                    visible = buttonsVisible,
                    enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                    exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(200)) + fadeOut(animationSpec = tween(200))
                ) {
                    BeginButton(
                        text = "BEGIN",
                        onClick = {
                            onStartMatch(firingRule)
                        }
                    )
                }
            }
        }

        // --- GHOST SHIP OVERLAY (WHILE DRAGGING) ---

        SnackbarHost(
            hostState = snackBarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // -- DIALOGS --
        if (showDialogDeployment) {
            DialogDeployment(
                onConfirm = {
                    showDialogDeployment = false
                    buttonsVisible = true
                }
            )
        }
    }
}