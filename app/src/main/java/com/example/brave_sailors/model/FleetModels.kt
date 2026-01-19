package com.example.brave_sailors.model

const val GRID_SIZE = 8

enum class CellStatus { EMPTY, SHIP, HIT, MISS }

enum class ShipOrientation { HORIZONTAL, VERTICAL }

data class Cell(
    val row: Int,
    val col: Int,
    val status: CellStatus = CellStatus.EMPTY
)

data class PlacementPreview(
    val coordinates: List<Pair<Int, Int>>,
    val isValid: Boolean
)

data class FleetPlacedShip(
    val shipId: Int,
    val size: Int,
    val row: Int,
    val col: Int,
    val isHorizontal: Boolean
) {
    fun coordinates(): List<Pair<Int, Int>> =
        (0 until size).map { i ->
            val r = if (isHorizontal) row else row + i
            val c = if (isHorizontal) col + i else col
            r to c
        }
}

data class FleetUiState(
    val grid: List<List<Cell>> = emptyGrid(),
    val placedShips: List<FleetPlacedShip> = emptyList(),
    val shipsToPlace: List<Int> = DEFAULT_SHIPS_TO_PLACE,
    val currentOrientation: ShipOrientation = ShipOrientation.HORIZONTAL,
    val draggedShipSize: Int? = null,
    val placementPreview: PlacementPreview? = null,
    val isLoading: Boolean = true,
    val isSaved: Boolean = false
)

val DEFAULT_SHIPS_TO_PLACE = listOf(4, 3, 3, 2, 2, 2, 1, 1)

fun emptyGrid(): List<List<Cell>> =
    List(GRID_SIZE) { r -> List(GRID_SIZE) { c -> Cell(r, c, CellStatus.EMPTY) } }