package com.example.brave_sailors.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.brave_sailors.data.local.database.entity.SavedShip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class FleetGuestViewModel : ViewModel() {

    private val GRID_SIZE = 8
    // Inventory for Guest
    val initialShipsToPlace = listOf(4, 3, 3, 2, 2, 2, 1, 1)

    private val _state = MutableStateFlow(FleetUiState())
    val state = _state.asStateFlow()

    init {
        // Initialize with empty grid and full inventory
        _state.update {
            it.copy(
                grid = emptyGrid(),
                shipsToPlace = initialShipsToPlace,
                isLoading = false,
                isSaved = false
            )
        }
    }

    // --- DRAG & DROP LOGIC (Identical to FleetViewModel but local) ---

    fun onDragStart(size: Int) {
        _state.update { it.copy(draggedShipSize = size) }
    }

    fun onDragHover(row: Int, col: Int, size: Int) {
        val horizontal = _state.value.currentOrientation == ShipOrientation.HORIZONTAL
        val isValid = isValidPlacement(row, col, size, horizontal)

        val coordinates = mutableListOf<Pair<Int, Int>>()
        for (i in 0 until size) {
            val r = if (horizontal) row else row + i
            val c = if (horizontal) col + i else col
            if (r in 0 until GRID_SIZE && c in 0 until GRID_SIZE) {
                coordinates.add(r to c)
            }
        }
        _state.update { it.copy(placementPreview = PlacementPreview(coordinates, isValid)) }
    }

    fun onDragEnd() {
        _state.update { it.copy(placementPreview = null, draggedShipSize = null) }
    }

    fun onDropShip(row: Int, col: Int, size: Int) {
        val horizontal = _state.value.currentOrientation == ShipOrientation.HORIZONTAL

        if (!_state.value.shipsToPlace.contains(size)) {
            onDragEnd()
            return
        }

        if (isValidPlacement(row, col, size, horizontal)) {
            val newShip = FleetPlacedShip(
                shipId = _state.value.placedShips.size + 100, // Fake ID
                size = size,
                row = row,
                col = col,
                isHorizontal = horizontal
            )

            val updatedPlacedShips = _state.value.placedShips + newShip
            val newGrid = rebuildGrid(updatedPlacedShips)
            val newShipsLeft = _state.value.shipsToPlace.toMutableList()
            newShipsLeft.remove(size)

            _state.update {
                it.copy(
                    grid = newGrid,
                    placedShips = updatedPlacedShips,
                    shipsToPlace = newShipsLeft,
                    placementPreview = null,
                    draggedShipSize = null
                )
            }
        } else {
            onDragEnd()
        }
    }

    // --- BUTTON ACTIONS ---

    fun onRotate() {
        val next = if (_state.value.currentOrientation == ShipOrientation.HORIZONTAL)
            ShipOrientation.VERTICAL else ShipOrientation.HORIZONTAL
        _state.update { it.copy(currentOrientation = next) }
    }

    fun onReset() {
        _state.update {
            it.copy(
                grid = emptyGrid(),
                placedShips = emptyList(),
                shipsToPlace = initialShipsToPlace,
                placementPreview = null
            )
        }
    }

    fun autoPlaceFleet() {
        onReset() // Clear first

        val newPlacedShips = mutableListOf<FleetPlacedShip>()
        val occupiedCoordinates = mutableSetOf<Pair<Int, Int>>()

        for (size in initialShipsToPlace) {
            var placed = false
            var attempts = 0
            while (!placed && attempts < 100) {
                attempts++
                val isHorizontal = Random.nextBoolean()
                val row = Random.nextInt(0, GRID_SIZE)
                val col = Random.nextInt(0, GRID_SIZE)

                if (isValidPlacementForAuto(row, col, size, isHorizontal, occupiedCoordinates)) {
                    newPlacedShips.add(FleetPlacedShip(
                        shipId = newPlacedShips.size + 200,
                        row = row,
                        col = col,
                        size = size,
                        isHorizontal = isHorizontal
                    ))
                    // Mark coordinates
                    for (i in 0 until size) {
                        val r = if (isHorizontal) row else row + i
                        val c = if (isHorizontal) col + i else col
                        occupiedCoordinates.add(r to c)
                    }
                    placed = true
                }
            }
        }

        val newGrid = rebuildGrid(newPlacedShips)
        _state.update {
            it.copy(
                grid = newGrid,
                placedShips = newPlacedShips,
                shipsToPlace = emptyList()
            )
        }
    }

    // --- SUBMISSION ---

    fun submitGuestFleet(): Boolean {
        if (_state.value.shipsToPlace.isNotEmpty()) return false

        // Convert UI ships to SavedShip format for the Game Engine
        val guestShips = _state.value.placedShips.mapIndexed { index, s ->
            SavedShip(
                userId = "guest_p2", // Fake ID
                shipId = index + 1,
                size = s.size,
                row = s.row,
                col = s.col,
                isHorizontal = s.isHorizontal
            )
        }

        // Save to Singleton for the Match Screen to retrieve
        GuestDataHolder.p2Fleet = guestShips
        return true
    }

    // --- HELPERS ---

    private fun rebuildGrid(ships: List<FleetPlacedShip>): List<List<Cell>> {
        val m = emptyGrid().map { it.toMutableList() }.toMutableList()
        ships.forEach { ship ->
            for (i in 0 until ship.size) {
                val r = if (ship.isHorizontal) ship.row else ship.row + i
                val c = if (ship.isHorizontal) ship.col + i else ship.col
                if (r in 0 until GRID_SIZE && c in 0 until GRID_SIZE) {
                    m[r][c] = m[r][c].copy(status = CellStatus.SHIP)
                }
            }
        }
        return m.map { it.toList() }
    }

    private fun isValidPlacement(row: Int, col: Int, size: Int, horizontal: Boolean): Boolean {
        // Boundary
        for (i in 0 until size) {
            val r = if (horizontal) row else row + i
            val c = if (horizontal) col + i else col
            if (r !in 0 until GRID_SIZE || c !in 0 until GRID_SIZE) return false
        }
        // Collision
        val newCoordinates = (0 until size).map {
            if (horizontal) row to col + it else row + it to col
        }.toSet()

        for (ship in _state.value.placedShips) {
            for (i in 0 until ship.size) {
                val r = if (ship.isHorizontal) ship.row else ship.row + i
                val c = if (ship.isHorizontal) ship.col + i else ship.col
                if (newCoordinates.contains(r to c)) return false
            }
        }
        return true
    }

    private fun isValidPlacementForAuto(r: Int, c: Int, size: Int, hor: Boolean, occupied: Set<Pair<Int, Int>>): Boolean {
        if (hor && c + size > GRID_SIZE) return false
        if (!hor && r + size > GRID_SIZE) return false
        for (i in 0 until size) {
            val nr = if (hor) r else r + i
            val nc = if (hor) c + i else c
            if (occupied.contains(nr to nc)) return false
        }
        return true
    }
}

// Simple Factory since no dependencies needed
class FleetGuestViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FleetGuestViewModel() as T
    }
}