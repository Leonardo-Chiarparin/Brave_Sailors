package com.example.brave_sailors.model

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.brave_sailors.data.local.MatchStateStorage
import com.example.brave_sailors.data.local.database.FleetDao
import com.example.brave_sailors.data.local.database.UserDao
import com.example.brave_sailors.data.local.database.entity.MoveLog
import com.example.brave_sailors.data.local.database.entity.SavedShip
import com.example.brave_sailors.data.local.database.entity.User
import com.example.brave_sailors.data.repository.UserRepository
import com.example.brave_sailors.ui.utils.GameSettingsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// --- UI STATE FOR 2-PLAYER LOCAL MATCH ---
data class MatchGuestUiState(
    val playerUser: User? = null,
    val isPlayer1Turn: Boolean = true,
    val turnNumber: Int = 1,
    val p1Grid: List<List<GridCell>> = emptyList(),
    val p2Grid: List<List<GridCell>> = emptyList(),
    val p1ShipsAlive: Int = 0,
    val p2ShipsAlive: Int = 0,
    val currentEnemyFleetComposition: Map<Int, Int> = emptyMap(),
    val shotsRemaining: Int = 1,
    val isGameOver: Boolean = false,
    val winnerName: String = "",
    val isPlayer1Winner: Boolean = false,
    val showTurnDialog: Boolean = true
)

object GuestDataHolder {
    var p2Fleet: List<SavedShip> = emptyList()
}

@SuppressLint("StaticFieldLeak")
class MatchVsGuestViewModel(
    private val context: Context,
    private val userDao: UserDao,
    private val fleetDao: FleetDao,
    private val userRepository: UserRepository
) : ViewModel() {

    private val settingsManager = GameSettingsManager(context)

    private val _uiState = MutableStateFlow(MatchGuestUiState())
    val uiState = _uiState.asStateFlow()

    private val gridSize = 8
    private var firingRule = "One shot"
    private val moveLogs = mutableListOf<MoveLog>()

    fun initializeMatch(rule: String) {
        this.firingRule = rule

        settingsManager.manageMusic(settingsManager.isMusicOn)

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
            val p1Fleet = fleetDao.getUserFleet(user.id)
            val p2Fleet = GuestDataHolder.p2Fleet

            if (p1Fleet.isEmpty() || p2Fleet.isEmpty()) return@launch

            val grid1 = createEmptyGrid()
            placeFleetOnGrid(grid1, p1Fleet)

            val grid2 = createEmptyGrid()
            placeFleetOnGrid(grid2, p2Fleet)

            val p2Composition = calculateFleetComposition(grid2)

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
                    shotsRemaining = calculateShots(true, p1Fleet.size),
                    showTurnDialog = true,
                    isGameOver = false
                )
            }
        }
    }

    fun closeTurnDialog() {
        _uiState.update { it.copy(showTurnDialog = false) }
    }

    fun onFire(row: Int, col: Int) {
        val state = _uiState.value

        if (state.isGameOver || state.shotsRemaining <= 0 || state.showTurnDialog) return

        val targetGrid = if (state.isPlayer1Turn) state.p2Grid else state.p1Grid
        val targetCell = targetGrid[row][col]

        if (targetCell.status == CellStatus.HIT || targetCell.status == CellStatus.MISS) return

        val isHit = (targetCell.status == CellStatus.SHIP)
        val newStatus = if (isHit) CellStatus.HIT else CellStatus.MISS

        if (isHit) settingsManager.triggerVibration()

        val updatedGrid = targetGrid.map { rowList ->
            rowList.map { cell ->
                if (cell.row == row && cell.col == col)
                    cell.copy(status = newStatus)
                else
                    cell
            }
        }

        var resultLog = "MISS"

        if (isHit) {
            val sunk = isShipSunk(updatedGrid, targetCell.shipId)
            resultLog = if (sunk) "SUNK" else "HIT"
        }

        var newShots = state.shotsRemaining
        var turnEnds = false

        if (firingRule == "Sequential hits") {
            if (!isHit) {
                newShots = 0
                turnEnds = true
            } else {
                newShots = 1
            }
        } else {
            newShots -= 1
            if (newShots <= 0) turnEnds = true
        }

        val p1Alive = if (state.isPlayer1Turn) countShipsAlive(state.p1Grid) else countShipsAlive(updatedGrid)
        val p2Alive = if (state.isPlayer1Turn) countShipsAlive(updatedGrid) else countShipsAlive(state.p2Grid)

        val p1Wins = (p2Alive == 0)
        val p2Wins = (p1Alive == 0)
        val isGameOver = p1Wins || p2Wins

        val actorId = if (state.isPlayer1Turn) (state.playerUser?.id ?: "Player 1") else "Player 2"

        moveLogs.add(
            MoveLog(
                matchId = 0,
                turnNumber = state.turnNumber,
                playerId = actorId,
                row = row,
                col = col,
                result = resultLog
            )
        )

        val victimComp = calculateFleetComposition(updatedGrid)

        _uiState.update {
            it.copy(
                p1Grid = if (!state.isPlayer1Turn) updatedGrid else state.p1Grid,
                p2Grid = if (state.isPlayer1Turn) updatedGrid else state.p2Grid,
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
            saveMatchToDb(p1Wins)
            settingsManager.manageMusic(false)
        } else if (turnEnds) {
            viewModelScope.launch {
                delay(1000)
                switchTurn()
            }
        }
    }

    private fun switchTurn() {
        val currentState = _uiState.value
        val nextIsPlayer1 = !currentState.isPlayer1Turn

        val nextPlayerShipsCount = if (nextIsPlayer1) currentState.p1ShipsAlive else currentState.p2ShipsAlive
        val shots = calculateShots(nextIsPlayer1, nextPlayerShipsCount)

        val nextEnemyGrid = if (nextIsPlayer1) currentState.p2Grid else currentState.p1Grid
        val nextEnemyComp = calculateFleetComposition(nextEnemyGrid)

        _uiState.update {
            it.copy(
                isPlayer1Turn = nextIsPlayer1,
                shotsRemaining = shots,
                turnNumber = it.turnNumber + 1,
                showTurnDialog = true,
                currentEnemyFleetComposition = nextEnemyComp
            )
        }

        settingsManager.triggerTurnAlarm()
    }

    private fun calculateShots(isPlayer1: Boolean, myShipsAlive: Int): Int {
        return when (firingRule) {
            "One shot" -> 1
            "Chain attacks" -> myShipsAlive
            "Sequential hits" -> 1
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

        val composition = mutableMapOf<Int, Int>()
        for (size in allShips.values) {
            composition[size] = 0
        }

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

    fun onAppBackground() {
        settingsManager.onAppPause()
    }

    fun onAppForeground() {
        val state = _uiState.value

        if (!state.isGameOver)
            settingsManager.onAppResume()
    }

    fun onRetire() {
        settingsManager.manageMusic(false)

        val currentState = MatchStateStorage.getState(context) ?: return

        MatchStateStorage.clear(context)

        val state = _uiState.value
        val isP1Surrender = state.isPlayer1Turn
        val p1Wins = !isP1Surrender

        viewModelScope.launch {
            val uId = state.playerUser?.id ?: return@launch
            userRepository.saveMatchRecord(uId, "Player 2", p1Wins, "Local", moveLogs)
        }
    }

    private fun saveMatchToDb(p1Victory: Boolean) {
        val currentState = MatchStateStorage.getState(context) ?: return

        MatchStateStorage.clear(context)

        val user = _uiState.value.playerUser ?: return

        viewModelScope.launch {
            userRepository.saveMatchRecord(user.id, "Player 2", p1Victory, "Local", moveLogs)
        }
    }

    private fun isShipSunk(grid: List<List<GridCell>>, shipId: Int): Boolean {
        return grid.flatten().none { it.shipId == shipId && it.status != CellStatus.HIT }
    }

    override fun onCleared() {
        super.onCleared()
        settingsManager.releaseResources()
    }
}

class MatchVsGuestViewModelFactory(
    private val context: Context,
    private val userDao: UserDao,
    private val fleetDao: FleetDao,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MatchVsGuestViewModel::class.java)) {
            return MatchVsGuestViewModel(context, userDao, fleetDao, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}