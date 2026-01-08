package com.example.brave_sailors.model

// Constant for the board size (8x8)
const val GRID_SIZE = 8

// Enum representing the status of a single cell
enum class CellStatus { EMPTY, SHIP, HIT, MISS }

// Enum for the orientation of the ship being placed
enum class ShipOrientation { HORIZONTAL, VERTICAL }

// Data class representing a single cell on the game grid
data class Cell(
    val row: Int,
    val col: Int,
    val status: CellStatus = CellStatus.EMPTY
)

// Data class to hold preview information during drag-and-drop operations
data class PlacementPreview(
    val coordinates: List<Pair<Int, Int>>, // List of (row, col) to highlight
    val isValid: Boolean                   // True = Green (Valid), False = Red (Invalid)
)

// Data class representing a ship that has been successfully placed on the board
data class FleetPlacedShip(
    val shipId: Int,
    val size: Int,
    val row: Int,
    val col: Int,
    val isHorizontal: Boolean
) {
    // Helper function to calculate all coordinates occupied by this ship
    fun coordinates(): List<Pair<Int, Int>> =
        (0 until size).map { i ->
            val r = if (isHorizontal) row else row + i
            val c = if (isHorizontal) col + i else col
            r to c
        }
}

// Main UI State holder for the Fleet Configuration Screen
data class FleetUiState(
    val grid: List<List<Cell>> = emptyGrid(),                // The 8x8 matrix of cells
    val placedShips: List<FleetPlacedShip> = emptyList(),    // List of ships currently on the board
    val shipsToPlace: List<Int> = DEFAULT_SHIPS_TO_PLACE,    // Inventory of ships remaining to be placed
    val currentOrientation: ShipOrientation = ShipOrientation.HORIZONTAL, // Current rotation state
    val draggedShipSize: Int? = null,                        // Size of the ship currently being dragged (null if none)
    val placementPreview: PlacementPreview? = null,          // Ghost ship coordinates for visual feedback
    val isLoading: Boolean = true,                           // Loading state (e.g. fetching from Room DB)
    val isSaved: Boolean = false                             // Indicates if the fleet has been saved to DB
)

// Default fleet composition: 1 Carrier (4), 2 Battleships (3), 3 Cruisers (2), 2 Submarines (1)
val DEFAULT_SHIPS_TO_PLACE = listOf(4, 3, 3, 2, 2, 2, 1, 1)

// Helper function to generate an empty 8x8 grid
fun emptyGrid(): List<List<Cell>> =
    List(GRID_SIZE) { r -> List(GRID_SIZE) { c -> Cell(r, c, CellStatus.EMPTY) } }