package com.example.brave_sailors.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.brave_sailors.data.local.database.FleetDao
import com.example.brave_sailors.data.local.database.UserDao
import com.example.brave_sailors.data.local.database.entity.SavedShip
import com.example.brave_sailors.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class FleetUiEvent {
    data class Message(val text: String) : FleetUiEvent()
    // Added a specific event for navigation/success if needed
    object SaveSuccess : FleetUiEvent()
}

class FleetViewModel(
    private val userDao: UserDao,
    private val fleetDao: FleetDao,
    private val userRepository: UserRepository // <--- 1. Added Repository dependency
) : ViewModel() {

    private val GRID_SIZE = 8
    val initialShipsToPlace = listOf(4, 3, 3, 2, 2, 2, 1, 1)

    private val _state = MutableStateFlow(FleetUiState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<FleetUiEvent>()
    val events = _events.asSharedFlow()

    init {
        loadFleetFromDb()
    }

    private fun loadFleetFromDb() {
        viewModelScope.launch(Dispatchers.IO) {
            val user = userDao.getCurrentUser()
            if (user == null) {
                _state.update { it.copy(isLoading = false) }
                return@launch
            }

            val saved = fleetDao.getUserFleet(user.id)
            if (saved.isEmpty()) {
                _state.update { it.copy(isLoading = false, shipsToPlace = initialShipsToPlace) }
                return@launch
            }

            val placedShips = saved.map { s ->
                FleetPlacedShip(s.shipId, s.size, s.row, s.col, s.isHorizontal)
            }

            val grid = rebuildGrid(placedShips)
            val shipsLeft = initialShipsToPlace.toMutableList()
            // Remove placed ships from the available list based on size count
            placedShips.forEach { shipsLeft.remove(it.size) }

            _state.update {
                it.copy(
                    grid = grid,
                    placedShips = placedShips,
                    shipsToPlace = shipsLeft,
                    isLoading = false,
                    isSaved = true
                )
            }
        }
    }

    // --- DRAG AND DROP LOGIC ---

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
                shipId = _state.value.placedShips.size, // Simple ID generation
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
                    draggedShipSize = null,
                    isSaved = false
                )
            }
        } else {
            onDragEnd()
        }
    }

    private fun isValidPlacement(row: Int, col: Int, size: Int, horizontal: Boolean): Boolean {
        // A. Boundary Check
        for (i in 0 until size) {
            val r = if (horizontal) row else row + i
            val c = if (horizontal) col + i else col
            if (r !in 0 until GRID_SIZE || c !in 0 until GRID_SIZE) return false
        }

        // B. Collisions
        val newShipCoordinates = mutableSetOf<Pair<Int, Int>>()
        for (i in 0 until size) {
            val r = if (horizontal) row else row + i
            val c = if (horizontal) col + i else col
            newShipCoordinates.add(r to c)
        }

        for (ship in _state.value.placedShips) {
            for (i in 0 until ship.size) {
                val r = if (ship.isHorizontal) ship.row else ship.row + i
                val c = if (ship.isHorizontal) ship.col + i else ship.col
                if (newShipCoordinates.contains(r to c)) return false
            }
        }
        return true
    }

    fun onRotate() {
        val next = if (_state.value.currentOrientation == ShipOrientation.HORIZONTAL)
            ShipOrientation.VERTICAL else ShipOrientation.HORIZONTAL
        _state.update { it.copy(currentOrientation = next) }

        // Refresh preview immediately if dragging
        val preview = _state.value.placementPreview
        val currentSize = _state.value.draggedShipSize
        if (preview != null && preview.coordinates.isNotEmpty() && currentSize != null) {
            val (r, c) = preview.coordinates.first()
            onDragHover(r, c, currentSize)
        }
    }

    fun onReset() {
        _state.update {
            FleetUiState(
                shipsToPlace = initialShipsToPlace,
                isLoading = false,
                grid = emptyGrid() // Ensure grid is visually cleared
            )
        }
    }

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

    // --- AUTO PLACEMENT LOGIC ---

    fun autoPlaceFleet() {
        // Don't auto-place if we are already saved (prevent accidental overwrite)
        // OR remove this check if you want the "Auto" button to work always as a reset+randomize
        // if (_state.value.isSaved) return

        onReset()

        val shipsToDistribute = initialShipsToPlace
        val newPlacedShips = mutableListOf<FleetPlacedShip>()
        val occupiedCoordinates = mutableSetOf<Pair<Int, Int>>()

        for (size in shipsToDistribute) {
            var placed = false
            var attempts = 0

            while (!placed && attempts < 100) {
                attempts++
                val isHorizontal = kotlin.random.Random.nextBoolean()
                val row = kotlin.random.Random.nextInt(0, GRID_SIZE)
                val col = kotlin.random.Random.nextInt(0, GRID_SIZE)

                if (isValidPlacementForAuto(row, col, size, isHorizontal, occupiedCoordinates, GRID_SIZE)) {
                    newPlacedShips.add(FleetPlacedShip(
                        shipId = newPlacedShips.size,
                        row = row,
                        col = col,
                        size = size,
                        isHorizontal = isHorizontal
                    ))

                    for (i in 0 until size) {
                        val r = if (isHorizontal) row else row + i
                        val c = if (isHorizontal) col + i else col
                        occupiedCoordinates.add(r to c)
                    }
                    placed = true
                }
            }
        }

        // <--- 2. FIX: Rebuild Grid for UI updates --->
        val newGrid = rebuildGrid(newPlacedShips)

        _state.update {
            it.copy(
                grid = newGrid, // Update the visual grid!
                placedShips = newPlacedShips,
                shipsToPlace = emptyList(),
                placementPreview = null,
                isSaved = false
            )
        }
    }

    private fun isValidPlacementForAuto(
        r: Int, c: Int, size: Int, isHorizontal: Boolean,
        occupied: Set<Pair<Int, Int>>, gridSize: Int
    ): Boolean {
        // 1. Boundary Check
        if (isHorizontal) {
            if (c + size > gridSize) return false
        } else {
            if (r + size > gridSize) return false
        }

        // 2. Overlap Check
        for (i in 0 until size) {
            val checkR = if (isHorizontal) r else r + i
            val checkC = if (isHorizontal) c + i else c
            if (occupied.contains(checkR to checkC)) return false
        }
        return true
    }

    // --- SAVE LOGIC ---

    // <--- 3. FIX: Consolidated Save Logic --->
    fun saveFleetToDb() {
        viewModelScope.launch(Dispatchers.IO) {
            val user = userDao.getCurrentUser()
            if (user == null) {
                _events.emit(FleetUiEvent.Message("No user found"))
                return@launch
            }

            if (_state.value.shipsToPlace.isNotEmpty()) {
                _events.emit(FleetUiEvent.Message("Complete formation first!"))
                return@launch
            }

            // 1. Prepare Data
            val shipsToSave = _state.value.placedShips.map { s ->
                SavedShip(
                    userId = user.id,
                    shipId = s.shipId,
                    size = s.size,
                    row = s.row,
                    col = s.col,
                    isHorizontal = s.isHorizontal
                )
            }

            // 2. Save Local
            fleetDao.replaceUserFleet(user.id, shipsToSave)

            // 3. Sync Cloud (Now works because Repository is injected)
            try {
                userRepository.syncLocalToCloud(user.id)
                _state.update { it.copy(isSaved = true) }
                _events.emit(FleetUiEvent.SaveSuccess)
            } catch (e: Exception) {
                _events.emit(FleetUiEvent.Message("Saved locally, but cloud sync failed."))
                // Still mark as saved locally
                _state.update { it.copy(isSaved = true) }
            }
        }
    }
}

// <--- 4. Updated Factory --->
class FleetViewModelFactory(
    private val u: UserDao,
    private val f: FleetDao,
    private val r: UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = FleetViewModel(u, f, r) as T
}