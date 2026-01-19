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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.brave_sailors.model.FleetGuestViewModel
import com.example.brave_sailors.model.FleetGuestViewModelFactory
import com.example.brave_sailors.model.ShipOrientation
import com.example.brave_sailors.ui.components.BeginButton
import com.example.brave_sailors.ui.components.ColoredShipBlock
import com.example.brave_sailors.ui.components.DialogDeployment
import com.example.brave_sailors.ui.components.ExactDraggableShipItem
import com.example.brave_sailors.ui.components.FleetButton
import com.example.brave_sailors.ui.components.GridBackground
import com.example.brave_sailors.ui.components.GridLinesOverlay
import com.example.brave_sailors.ui.components.ReturnButton
import com.example.brave_sailors.ui.components.ShipDrawing
import com.example.brave_sailors.ui.theme.DarkGrey
import com.example.brave_sailors.ui.theme.DeepBlue
import com.example.brave_sailors.ui.theme.White
import com.example.brave_sailors.ui.utils.RememberScaleConversion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
    val density = LocalDensity.current

    val scale = RememberScaleConversion()
    val scope = rememberCoroutineScope()

    val cellSize = scale.dp(64f)

    val maxWidth = scale.dp(720f)

    val viewModel: FleetGuestViewModel = viewModel(
        factory = FleetGuestViewModelFactory()
    )

    val state by viewModel.state.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }

    var dragState by remember { mutableStateOf(DragState()) }
    var boardOffset by remember { mutableStateOf(Offset.Unspecified) }
    var boardCellPx by remember { mutableFloatStateOf(0f) }

    var buttonsVisible by remember { mutableStateOf(false) }

    var showDialogDeployment by remember { mutableStateOf(true) }

    fun triggerExit(callback: () -> Unit) {
        scope.launch {
            buttonsVisible = false
            delay(200)
            callback()
        }
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
                                .onGloballyPositioned { coordinates ->
                                    boardOffset = coordinates.positionInWindow()
                                    boardCellPx = with(density) { cellSize.toPx() }
                                }
                                .size(cellSize * GRID_SIZE)
                        ) {
                            val preview = state.placementPreview

                            if (preview != null) {
                                val colorRaw = if (preview.isValid) PREVIEW_OK else PREVIEW_BAD
                                val previewColor = colorRaw.copy(alpha = 0.25f)

                                val (row, col) = preview.coordinates.firstOrNull() ?: (0 to 0)
                                val isHorizontal = state.currentOrientation == ShipOrientation.HORIZONTAL

                                val shipSize = if (dragState.isDragging && dragState.draggedShipSize > 0) {
                                    dragState.draggedShipSize
                                } else {
                                    state.draggedShipSize ?: 1
                                }

                                val w = if (isHorizontal) cellSize * shipSize else cellSize
                                val h = if (isHorizontal) cellSize else cellSize * shipSize

                                Box(
                                    modifier = Modifier
                                        .offset(x = cellSize * col, y = cellSize * row)
                                        .size(w, h)
                                ) {
                                    ColoredShipBlock(
                                        cellSize,
                                        shipSize = shipSize,
                                        isHorizontal = isHorizontal,
                                        previewColor = previewColor
                                    )
                                }
                            }

                            state.placedShips.forEach { ship ->
                                ShipDrawing(ship, cellSize)
                            }

                            GridLinesOverlay(GRID_SIZE, cellSize)
                        }
                    }
                }
            }

            if (state.shipsToPlace.isNotEmpty())
                Spacer(Modifier.height(scale.dp(60f)))
            else
                Spacer(Modifier.height(scale.dp(102f)))

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
                    onClick = { viewModel.autoPlaceFleet() }
                )

                Spacer(modifier = Modifier.width(scale.dp(168f)))

                FleetButton(
                    paddingH = 28f,
                    paddingV = 24f,
                    text = "RESET",
                    enabled = state.placedShips.isNotEmpty(),
                    onClick = { viewModel.onReset() }
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
                    enabled = dragState.isDragging,
                    onClick = { viewModel.onRotate() }
                )
            }
        }

        if (state.shipsToPlace.isNotEmpty()) {
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
                            val groupedShips = state.shipsToPlace
                                .groupingBy { it }
                                .eachCount()
                                .toSortedMap(reverseOrder())

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(scale.dp(26f)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                groupedShips.keys.forEach { size ->
                                    val count = groupedShips[size] ?: 0

                                    ExactDraggableShipItem(
                                        cellSize = cellSize,
                                        shipSize = size,
                                        count = count,
                                        boardOffset = boardOffset,
                                        boardCellPx = boardCellPx,
                                        isVertical = state.currentOrientation == ShipOrientation.VERTICAL,
                                        onDragStart = { grabOffset ->
                                            viewModel.onDragStart(size)

                                            if (state.currentOrientation == ShipOrientation.VERTICAL) {
                                                viewModel.onRotate()
                                            }

                                            dragState = dragState.copy(
                                                isDragging = true,
                                                draggedShipSize = size,
                                                grabOffset = grabOffset
                                            )
                                        },
                                        onDragOffsetUpdate = { offset ->
                                            dragState = dragState.copy(currentDragOffset = offset)
                                        },
                                        onDragCell = { r, c ->
                                            viewModel.onDragHover(r, c, size)
                                        },
                                        onDragExit = { viewModel.onDragEnd() },
                                        onDragFinished = {
                                            viewModel.onDragEnd()
                                            dragState = dragState.copy(isDragging = false)
                                        },
                                        onDrop = { r, c ->
                                            viewModel.onDropShip(r, c, size)
                                            if (state.currentOrientation == ShipOrientation.VERTICAL) {
                                                viewModel.onRotate()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

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
                            if (viewModel.submitGuestFleet())
                                onStartMatch(firingRule)
                        }
                    )
                }
            }
        }

        if (dragState.isDragging && dragState.draggedShipSize > 0) {
            val shipSize = dragState.draggedShipSize
            val offsetPx = dragState.currentDragOffset

            val isHorizontal = state.currentOrientation == ShipOrientation.HORIZONTAL

            val (w, h) = with(density) {
                if (isHorizontal) (cellSize * shipSize).toPx() to cellSize.toPx()
                else cellSize.toPx() to (cellSize * shipSize).toPx()
            }

            val xOffset = offsetPx.x - (w / 2f)
            val yOffset = offsetPx.y - (h / 2f)

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset(xOffset.roundToInt(), yOffset.roundToInt()) }
                    .size(width = with(density) { w.toDp() }, height = with(density) { h.toDp() })
                    .zIndex(1f)
                    .graphicsLayer()
            ) {
                ColoredShipBlock(cellSize, shipSize = shipSize, isHorizontal = isHorizontal)
            }
        }

        SnackbarHost(
            hostState = snackBarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

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