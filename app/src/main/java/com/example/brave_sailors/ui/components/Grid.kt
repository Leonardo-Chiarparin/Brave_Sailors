package com.example.brave_sailors.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import com.example.brave_sailors.DEFAULT_SHIP_COLOR
import com.example.brave_sailors.GRID_BORDER_COLOR
import com.example.brave_sailors.GRID_SIZE
import com.example.brave_sailors.SHIP_COLORS
import com.example.brave_sailors.model.FleetPlacedShip
import com.example.brave_sailors.ui.theme.DarkBlue
import com.example.brave_sailors.ui.theme.Orange
import com.example.brave_sailors.ui.theme.White
import com.example.brave_sailors.ui.utils.RememberScaleConversion
import kotlin.math.roundToInt

@Composable
fun FleetStatus(
    turn: Int,
    isPlayerTurn: Boolean,
    shipsRemaining: Map<Int, Int>? = null
) {
    val scale = RememberScaleConversion()

    val boxShape = CutCornerShape(scale.dp(36f))
    val titleText = if (isPlayerTurn) "ENEMY FLEET" else "YOUR FLEET"

    val count1 = shipsRemaining?.get(1) ?: 2
    val count2 = shipsRemaining?.get(2) ?: 3
    val count3 = shipsRemaining?.get(3) ?: 2
    val count4 = shipsRemaining?.get(4) ?: 1

    Box(
        modifier = Modifier
            .width(scale.dp(456f))
            .height(scale.dp(220f))
            .background(DarkBlue.copy(alpha = 0.90f), shape = boxShape)
            .border(BorderStroke(scale.dp(1f), Orange), shape = boxShape)
            .clip(boxShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .padding(
                    top = scale.dp(20f),
                    bottom = scale.dp(24f),
                    start = scale.dp(36f),
                    end = scale.dp(36f)
                )
                .clip(RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = titleText,
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

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = scale.dp(16f)),
                            horizontalArrangement = Arrangement.spacedBy(scale.dp(10f)),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(scale.dp(6f))
                            ) {
                                ShipIcon(1)

                                Text(
                                    text = "$count1",
                                    color = Orange,
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
                                        )
                                    )
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(scale.dp(6f))
                            ) {
                                ShipIcon(2)

                                Text(
                                    text = "$count2",
                                    color = Orange,
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
                                        )
                                    )
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(scale.dp(6f))
                            ) {
                                ShipIcon(3)

                                Text(
                                    text = "$count3",
                                    color = Orange,
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
                                        )
                                    )
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(scale.dp(6f))
                            ) {
                                ShipIcon(4)

                                Text(
                                    text = "$count4",
                                    color = Orange,
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
                                        )
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(scale.dp(30f)))

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = scale.dp(4f))
                        .width(scale.dp(2f))
                        .background(Orange)
                )

                Spacer(modifier = Modifier.width(scale.dp(30f)))

                Box(
                    modifier = Modifier
                        .padding(horizontal = scale.dp(16f))
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Turn",
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
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both
                            ),
                            shadow = Shadow(
                                color = Color.Black,
                                offset = Offset(2f, 2f),
                                blurRadius = 4f
                            )
                        ),
                        modifier = Modifier.align(Alignment.TopCenter)
                    )

                    Text(
                        text = "$turn",
                        color = Orange,
                        fontSize = scale.sp(36f),
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
                            )
                        ),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
fun ShipIcon(blocks: Int) {
    val scale = RememberScaleConversion()

    val blockSize = scale.dp(20f)
    val gap = scale.dp(4f)

    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        repeat(blocks) {
            Box(modifier = Modifier
                .size(blockSize)
                .background(Orange))
        }
    }
}

@Composable
fun ExactDraggableShipItem(
    cellSize: Dp,
    shipSize: Int,
    count: Int,
    boardOffset: Offset,
    boardCellPx: Float,
    isVertical: Boolean,
    onDragStart: (Offset) -> Unit,
    onDragOffsetUpdate: (Offset) -> Unit,
    onDragCell: (Int, Int) -> Unit,
    onDragExit: () -> Unit,
    onDragFinished: () -> Unit,
    onDrop: (Int, Int) -> Unit
) {
    var originInWindow by remember { mutableStateOf(Offset.Unspecified) }
    var currentPointerLocalOffset by remember { mutableStateOf(Offset.Zero) }
    var lastCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val currentIsVertical by rememberUpdatedState(isVertical)

    val scale = RememberScaleConversion()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .onGloballyPositioned { originInWindow = it.positionInWindow() }
                .pointerInput(shipSize) {
                    detectDragGestures(
                        onDragStart = { pointerOffset ->
                            currentPointerLocalOffset = pointerOffset
                            onDragStart(pointerOffset)
                            onDragOffsetUpdate(originInWindow + currentPointerLocalOffset)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            currentPointerLocalOffset += dragAmount
                            val currentFingerPos = originInWindow + currentPointerLocalOffset
                            onDragOffsetUpdate(currentFingerPos)

                            if (boardOffset != Offset.Unspecified && boardCellPx > 0f) {

                                val shipWidthPx: Float
                                val shipHeightPx: Float

                                if (currentIsVertical) {
                                    shipWidthPx = boardCellPx
                                    shipHeightPx = boardCellPx * shipSize
                                } else {
                                    shipWidthPx = boardCellPx * shipSize
                                    shipHeightPx = boardCellPx
                                }

                                val shipTopLeftX = currentFingerPos.x - (shipWidthPx / 2f)
                                val shipTopLeftY = currentFingerPos.y - (shipHeightPx / 2f)

                                val relX = (shipTopLeftX - boardOffset.x) + (boardCellPx / 2f)
                                val relY = (shipTopLeftY - boardOffset.y) + (boardCellPx / 2f)

                                val boardSizePx = boardCellPx * GRID_SIZE

                                if (relX >= 0 && relY >= 0 && relX < boardSizePx && relY < boardSizePx) {
                                    val c = (relX / boardCellPx).toInt().coerceIn(0, GRID_SIZE - 1)
                                    val r = (relY / boardCellPx).toInt().coerceIn(0, GRID_SIZE - 1)

                                    if (lastCell != (r to c)) {
                                        lastCell = r to c
                                        onDragCell(r, c)
                                    }
                                } else {
                                    if (lastCell != null) {
                                        lastCell = null
                                        onDragExit()
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            lastCell?.let { (r, c) -> onDrop(r, c) } ?: onDragExit()
                            lastCell = null
                            onDragFinished()
                        },
                        onDragCancel = {
                            lastCell = null
                            onDragExit()
                            onDragFinished()
                        }
                    )
                }
                .size(cellSize * shipSize, cellSize)
        ) {
            ColoredShipBlock(cellSize, shipSize = shipSize, isHorizontal = true)
        }

        Text(
            "x$count",
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
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both
                )
            ),
            modifier = Modifier.padding(top = scale.dp(8f))
        )
    }
}

@Composable
fun ShipDrawing(ship: FleetPlacedShip, cellSize: Dp) {
    val density = LocalDensity.current

    val isHorizontal = ship.isHorizontal

    val width = if (isHorizontal) cellSize * ship.size else cellSize
    val height = if (isHorizontal) cellSize else cellSize * ship.size

    Box(
        modifier = Modifier
            .offset {
                val cellSizePx = with(density) { cellSize.toPx() }

                val xPx = (cellSizePx * ship.col).roundToInt()
                val yPx = (cellSizePx * ship.row).roundToInt()

                IntOffset(xPx, yPx)
            }
            .size(width, height)
    ) {
        ColoredShipBlock(
            cellSize = cellSize,
            shipSize = ship.size,
            isHorizontal = isHorizontal
        )
    }
}

@Composable
fun ColoredShipBlock(
    cellSize: Dp,
    shipSize: Int,
    isHorizontal: Boolean,
    previewColor: Color? = null
) {
    val displayColor = previewColor ?: (SHIP_COLORS[shipSize] ?: DEFAULT_SHIP_COLOR)
    val scale = RememberScaleConversion()
    val density = LocalDensity.current

    val gapDp = scale.dp(6f)

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val gapPx = with(density) { gapDp.toPx() }
        val cellSizePx = with(density) { cellSize.toPx() }

        val padding = gapPx / 2f

        val blockSizePx = cellSizePx - gapPx

        for (i in 0 until shipSize) {
            val cellStart = (i * cellSizePx) + padding

            val left: Float
            val top: Float

            if (isHorizontal) {
                left = cellStart
                top = padding
            } else {
                left = padding
                top = cellStart
            }

            drawRect(
                color = displayColor,
                topLeft = Offset(x = left, y = top),
                size = Size(width = blockSizePx, height = blockSizePx)
            )
        }
    }
}

@Composable
fun GridLinesOverlay(gridSize: Int, cellSize: Dp) {
    val color = Color(0xFF96A8DE)

    val scale = RememberScaleConversion()
    val strokeWidth = scale.dp(2f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = strokeWidth.toPx()

        val cellSizePx = cellSize.toPx()
        val width = size.width
        val height = size.height

        for (i in 1 until gridSize) {
            val x = i * cellSizePx
            drawLine(
                color = color,
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = stroke
            )
        }

        for (j in 1 until gridSize) {
            val y = j * cellSizePx
            drawLine(
                color = color,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = stroke
            )
        }

        drawRect(
            color = GRID_BORDER_COLOR,
            style = Stroke(width = stroke)
        )
    }
}