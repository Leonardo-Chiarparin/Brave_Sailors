package com.example.brave_sailors.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.brave_sailors.data.local.database.FleetDao
import com.example.brave_sailors.data.local.database.UserDao
import com.example.brave_sailors.data.local.database.entity.MoveLog
import com.example.brave_sailors.data.local.database.entity.SavedShip
import com.example.brave_sailors.data.local.database.entity.User
import com.example.brave_sailors.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// --- UI STATE FOR 2-PLAYER LOCAL MATCH ---
// Holds the state for both players, including their grids and fleet status.
data class MatchGuestUiState(
    val playerUser: User? = null,
    val isPlayer1Turn: Boolean = true, // true = Player 1, false = Player 2
    val turnNumber: Int = 1,

    // Grids: P1's grid contains P1's ships (P2 fires here).
    val p1Grid: List<List<GridCell>> = emptyList(),
    // Grids: P2's grid contains P2's ships (P1 fires here).
    val p2Grid: List<List<GridCell>> = emptyList(),

    val p1ShipsAlive: Int = 0,
    val p2ShipsAlive: Int = 0,

    // Composition of the ENEMY fleet (what the current player sees on the radar)
    val currentEnemyFleetComposition: Map<Int, Int> = emptyMap(),

    val shotsRemaining: Int = 1,
    val isGameOver: Boolean = false,
    val winnerName: String = "",
    val isPlayer1Winner: Boolean = false,

    // Controls the "Pass the device" dialog between turns
    val showTurnDialog: Boolean = true
)

// --- SINGLETON TO HOLD GUEST DATA ---
// Used to pass the Guest's fleet from the setup screen to this ViewModel
object GuestDataHolder {
    var p2Fleet: List<SavedShip> = emptyList()
}

class MatchVsGuestViewModel(
    private val userDao: UserDao,
    private val fleetDao: FleetDao,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MatchGuestUiState())
    val uiState = _uiState.asStateFlow()

    private val gridSize = 8
    private var firingRule = "One shot"
    private val moveLogs = mutableListOf<MoveLog>()

    // --- INITIALIZATION ---
    fun initializeMatch(rule: String) {
        this.firingRule = rule

        // Immediate reset to prevent UI glitches from previous states
        _uiState.update {
            it.copy(
                isGameOver = false,
                winnerName = "",
                isPlayer1Winner = false,
                shotsRemaining = 0,
                showTurnDialog = true
            )
        }

        moveLogs.clear()

        viewModelScope.launch {
            val user = userDao.getCurrentUser() ?: return@launch

            // 1. Load Player 1 Fleet from Database
            val p1Fleet = fleetDao.getUserFleet(user.id)

            // 2. Load Player 2 Fleet from Memory (passed via Singleton)
            val p2Fleet = GuestDataHolder.p2Fleet

            if (p1Fleet.isEmpty() || p2Fleet.isEmpty()) return@launch

            // 3. Create and Populate Grids
            val grid1 = createEmptyGrid()
            placeFleetOnGrid(grid1, p1Fleet) // Place P1 ships

            val grid2 = createEmptyGrid()
            placeFleetOnGrid(grid2, p2Fleet) // Place P2 ships

            // 4. Initial composition for the UI (Player 1 starts, so calculate P2's composition)
            val p2Composition = calculateFleetComposition(grid2)

            // 5. Update UI State
            _uiState.update {
                it.copy(
                    playerUser = user,
                    p1Grid = grid1,
                    p2Grid = grid2,
                    p1ShipsAlive = p1Fleet.size,
                    p2ShipsAlive = p2Fleet.size,
                    currentEnemyFleetComposition = p2Composition,
                    isPlayer1Turn = true,
                    turnNumber = 1,
                    shotsRemaining = calculateShots(true, p1Fleet.size), // Calculate P1 shots
                    showTurnDialog = true, // Force "Pass Device" dialog at start
                    isGameOver = false
                )
            }
        }
    }

    // Called when the user clicks "OK" / "Ready" on the turn change dialog
    fun closeTurnDialog() {
        _uiState.update { it.copy(showTurnDialog = false) }
    }

    // --- GAMEPLAY LOGIC ---
    fun onFire(row: Int, col: Int) {
        val state = _uiState.value

        // Prevent firing if game over, no shots, or dialog is open
        if (state.isGameOver || state.shotsRemaining <= 0 || state.showTurnDialog) return

        // Determine target grid: If P1 is playing, they target P2's grid, and vice versa.
        val targetGrid = if (state.isPlayer1Turn) state.p2Grid else state.p1Grid

        val targetCell = targetGrid[row][col]
        // Prevent hitting the same cell twice
        if (targetCell.status == CellStatus.HIT || targetCell.status == CellStatus.MISS) return

        val isHit = (targetCell.status == CellStatus.SHIP)
        targetCell.status = if (isHit) CellStatus.HIT else CellStatus.MISS

        // --- FIRING RULES IMPLEMENTATION ---
        var newShots = state.shotsRemaining
        var turnEnds = false

        if (firingRule == "Sequential hits") {
            if (!isHit) {
                // Miss = turn ends immediately
                newShots = 0
                turnEnds = true
            } else {
                // Hit = keep firing (shots remain at 1)
                newShots = 1
            }
        } else {
            // Standard rules or Chain attacks
            newShots -= 1
            if (newShots <= 0) turnEnds = true
        }

        // --- UPDATE STATS AND CHECK VICTORY ---
        val p1Alive = countShipsAlive(state.p1Grid)
        val p2Alive = countShipsAlive(state.p2Grid)

        // P1 wins if P2 has 0 ships. P2 wins if P1 has 0 ships.
        val p1Wins = (p2Alive == 0)
        val p2Wins = (p1Alive == 0)
        val isGameOver = p1Wins || p2Wins

        // Log the move
        val actorId = if (state.isPlayer1Turn) (state.playerUser?.id ?: "P1") else "Player 2"
        moveLogs.add(
            MoveLog(
                matchId = 0,
                turnNumber = state.turnNumber,
                playerId = actorId,
                row = row,
                col = col,
                result = if (isHit) "HIT" else "MISS"
            )
        )

        // Recalculate the victim's composition to show updated status to the attacker
        val victimGrid = if (state.isPlayer1Turn) state.p2Grid else state.p1Grid
        val victimComp = calculateFleetComposition(victimGrid)

        _uiState.update {
            it.copy(
                shotsRemaining = newShots,
                p1ShipsAlive = p1Alive,
                p2ShipsAlive = p2Alive,
                isGameOver = isGameOver,
                isPlayer1Winner = p1Wins,
                winnerName = if (p1Wins) (state.playerUser?.name ?: "Player 1") else "Player 2",
                currentEnemyFleetComposition = victimComp
            )
        }

        if (isGameOver) {
            // Save match record (only if P1 wins/loses, we treat P2 as "Guest")
            saveMatchToDb(p1Wins)
        } else if (turnEnds) {
            switchTurn()
        }
    }

    private fun switchTurn() {
        val currentState = _uiState.value
        val nextIsPlayer1 = !currentState.isPlayer1Turn

        // Determine how many ships the NEXT player has alive to calculate their shots
        val nextPlayerShipsCount = if (nextIsPlayer1) currentState.p1ShipsAlive else currentState.p2ShipsAlive
        val shots = calculateShots(nextIsPlayer1, nextPlayerShipsCount)

        // Prepare the Enemy Composition for the NEXT player (If next is P1, they see P2's fleet)
        val nextEnemyGrid = if (nextIsPlayer1) currentState.p2Grid else currentState.p1Grid
        val nextEnemyComp = calculateFleetComposition(nextEnemyGrid)

        _uiState.update {
            it.copy(
                isPlayer1Turn = nextIsPlayer1,
                shotsRemaining = shots,
                turnNumber = it.turnNumber + 1,
                showTurnDialog = true, // Trigger the "Pass Device" screen
                currentEnemyFleetComposition = nextEnemyComp
            )
        }
    }

    // --- HELPER FUNCTIONS (Copied from MatchVsComputer) ---

    private fun calculateShots(isPlayer1: Boolean, myShipsAlive: Int): Int {
        return when (firingRule) {
            "One shot" -> 1
            "Chain attacks" -> myShipsAlive
            "Sequential hits" -> 1 // Always starts with 1 shot in this mode
            else -> 1
        }
    }

    private fun countShipsAlive(grid: List<List<GridCell>>): Int {
        val aliveShipIds = mutableSetOf<Int>()
        for (row in grid) {
            for (cell in row) {
                if (cell.status == CellStatus.SHIP) {
                    aliveShipIds.add(cell.shipId)
                }
            }
        }
        return aliveShipIds.size
    }

    private fun calculateFleetComposition(grid: List<List<GridCell>>): Map<Int, Int> {
        val allShips = mutableMapOf<Int, Int>()
        val aliveShips = mutableSetOf<Int>()

        // Scan grid to find all ships and which are still alive
        for (row in grid) {
            for (cell in row) {
                if (cell.shipId > 0) {
                    allShips[cell.shipId] = cell.originalSize
                    if (cell.status == CellStatus.SHIP) {
                        aliveShips.add(cell.shipId)
                    }
                }
            }
        }

        // Initialize counts for all sizes
        val composition = mutableMapOf<Int, Int>()
        for (size in allShips.values) {
            composition[size] = 0
        }

        // Increment count for alive ships
        for (id in aliveShips) {
            val size = allShips[id] ?: 1
            composition[size] = (composition[size] ?: 0) + 1
        }

        return composition
    }

    private fun createEmptyGrid(): List<List<GridCell>> {
        return List(gridSize) { r -> List(gridSize) { c -> GridCell(r, c) } }
    }

    private fun placeFleetOnGrid(grid: List<List<GridCell>>, fleet: List<SavedShip>) {
        fleet.forEachIndexed { index, ship ->
            val shipColorId = index + 1
            for (i in 0 until ship.size) {
                val r = if (ship.isHorizontal) ship.row else ship.row + i
                val c = if (ship.isHorizontal) ship.col + i else ship.col
                if (r < gridSize && c < gridSize) {
                    grid[r][c].status = CellStatus.SHIP
                    grid[r][c].shipId = shipColorId
                    grid[r][c].originalSize = ship.size
                }
            }
        }
    }

    fun onRetire() {
        val state = _uiState.value
        // If it's Player 1's turn and they retire, P1 loses.
        // If it's Player 2's turn and they retire, P1 wins.
        val isP1Surrender = state.isPlayer1Turn
        val p1Wins = !isP1Surrender

        viewModelScope.launch {
            val uId = state.playerUser?.id ?: return@launch
            userRepository.saveMatchRecord(uId, "Player 2", p1Wins, "Local", moveLogs)
        }
    }

    private fun saveMatchToDb(p1Victory: Boolean) {
        val user = _uiState.value.playerUser ?: return
        viewModelScope.launch {
            // We always save the record for the logged-in user (Player 1)
            // Opponent name is hardcoded as "Player 2" for local guest matches
            userRepository.saveMatchRecord(user.id, "Player 2", p1Victory, "Local", moveLogs)
        }
    }
}

class MatchVsGuestViewModelFactory(
    private val userDao: UserDao,
    private val fleetDao: FleetDao,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MatchVsGuestViewModel::class.java)) {
            return MatchVsGuestViewModel(userDao, fleetDao, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}