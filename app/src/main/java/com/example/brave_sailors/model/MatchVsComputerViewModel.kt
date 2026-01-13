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
import com.example.brave_sailors.data.remote.api.AiNetworkClient
import com.example.brave_sailors.data.remote.api.GridRequest
import com.example.brave_sailors.ui.utils.GameSettingsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

// --- DATA MODELS ---
data class GridCell(
    val row: Int,
    val col: Int,
    var status: CellStatus = CellStatus.EMPTY,
    var shipId: Int = -1,
    var originalSize: Int = 0
)

data class MatchUiState(
    val playerUser: User? = null,
    val isPlayerTurn: Boolean = true,
    val turnNumber: Int = 1,
    val playerGrid: List<List<GridCell>> = emptyList(),
    val aiGrid: List<List<GridCell>> = emptyList(),
    val playerShipsAlive: Int = 0,
    val aiShipsAlive: Int = 0,
    val playerShipsComposition: Map<Int, Int> = emptyMap(),
    val aiShipsComposition: Map<Int, Int> = emptyMap(),
    val shotsRemaining: Int = 1,
    val isGameOver: Boolean = false,
    val winnerName: String = "",
    val isPlayerWinner: Boolean = false
)

@SuppressLint("StaticFieldLeak")
class MatchVsComputerViewModel(
    private val context: Context,
    private val userDao: UserDao,
    private val fleetDao: FleetDao,
    private val userRepository: UserRepository
) : ViewModel() {

    private val settingsManager = GameSettingsManager(context)

    private val _uiState = MutableStateFlow(MatchUiState())
    val uiState = _uiState.asStateFlow()

    private val gridSize = 8
    private var difficulty = "Normal"
    private var firingRule = "One shot"

    private val aiTargetStack = ArrayDeque<Pair<Int, Int>>()
    private val aiHitMap = mutableSetOf<Pair<Int, Int>>()
    private val moveLogs = mutableListOf<MoveLog>()

    fun initializeMatch(difficulty: String, firingRule: String) {
        this.difficulty = difficulty
        this.firingRule = firingRule

        settingsManager.manageMusic(settingsManager.isMusicOn)

        _uiState.update {
            it.copy(
                isGameOver = false,
                winnerName = "",
                isPlayerWinner = false,
                shotsRemaining = 0
            )
        }

        aiTargetStack.clear()
        aiHitMap.clear()
        moveLogs.clear()

        viewModelScope.launch {
            val user = userDao.getCurrentUser() ?: return@launch
            val userFleet = fleetDao.getUserFleet(user.id)

            if (userFleet.isEmpty()) return@launch

            val pGrid = createEmptyGrid()
            val aGrid = createEmptyGrid()

            placeFleetOnGrid(pGrid, userFleet)
            val pComposition = calculateFleetComposition(pGrid)

            val aiFleetStruct = userFleet.map { it.size }
            placeAiShips(aGrid, aiFleetStruct)
            val aComposition = calculateFleetComposition(aGrid)

            _uiState.update {
                it.copy(
                    playerUser = user,
                    playerGrid = pGrid,
                    aiGrid = aGrid,
                    playerShipsAlive = userFleet.size,
                    aiShipsAlive = userFleet.size,
                    playerShipsComposition = pComposition,
                    aiShipsComposition = aComposition,
                    isPlayerTurn = true,
                    turnNumber = 1,
                    shotsRemaining = calculateShots(true, userFleet.size)
                )
            }

            settingsManager.triggerTurnAlarm()
        }
    }

    fun onPlayerFire(row: Int, col: Int) {
        val state = _uiState.value
        if (!state.isPlayerTurn || state.isGameOver || state.shotsRemaining <= 0) return

        val targetCell = state.aiGrid[row][col]
        if (targetCell.status == CellStatus.HIT || targetCell.status == CellStatus.MISS) return

        val isHit = (targetCell.status == CellStatus.SHIP)
        val newStatus = if (isHit) CellStatus.HIT else CellStatus.MISS

        if (isHit) settingsManager.triggerVibration()

        val updateAiGrid = state.aiGrid.map { rowList ->
            rowList.map { cell ->
                if (cell.row == row && cell.col == col)
                    cell.copy(status = newStatus)
                else
                    cell
            }
        }

        var resultLog = "MISS"
        if (isHit) {
            val sunk = isShipSunk(updateAiGrid, targetCell.shipId)
            resultLog = if (sunk) "SUNK" else "HIT"
        }

        var newShots = state.shotsRemaining
        var turnEnds = false

        if (firingRule == "Sequential hits") {
            if (!isHit) {
                newShots = 0
                turnEnds = true
            }
        } else {
            newShots -= 1
            if (newShots <= 0) turnEnds = true
        }

        val aiAlive = countShipsAlive(updateAiGrid)
        val aiComposition = calculateFleetComposition(updateAiGrid)
        val isWin = (aiAlive == 0)

        moveLogs.add(
            MoveLog(
                matchId = 0,
                turnNumber = state.turnNumber,
                playerId = state.playerUser?.id ?: "Player",
                row = row,
                col = col,
                result = resultLog
            )
        )

        _uiState.update {
            it.copy(
                aiGrid = updateAiGrid,
                shotsRemaining = newShots,
                aiShipsAlive = aiAlive,
                aiShipsComposition = aiComposition,
                isGameOver = isWin,
                isPlayerWinner = isWin,
                winnerName = if (isWin) (state.playerUser?.name ?: "Player") else ""
            )
        }

        if (isWin) {
            saveMatchToDb(true)
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
        val nextIsPlayer = !currentState.isPlayerTurn
        val shipsCountForNext = if (nextIsPlayer) currentState.playerShipsAlive else currentState.aiShipsAlive
        val shots = calculateShots(nextIsPlayer, shipsCountForNext)

        _uiState.update {
            it.copy(
                isPlayerTurn = nextIsPlayer,
                shotsRemaining = shots,
                turnNumber = it.turnNumber + 1
            )
        }

        if (nextIsPlayer) {
            settingsManager.triggerTurnAlarm()
        } else {
            viewModelScope.launch {
                performAiTurn()
            }
        }
    }

    private suspend fun performAiTurn() {
        var state = _uiState.value

        while (!state.isPlayerTurn && !state.isGameOver && state.shotsRemaining > 0) {

            delay(1000)

            var coordinates: Pair<Int, Int>

            if (difficulty == "Hard") {
                try {
                    val simpleGrid = convertGridForAi(state.playerGrid)
                    val aiMove = AiNetworkClient.service.getNextMove(
                        GridRequest(
                            simpleGrid,
                            difficulty
                        )
                    )
                    coordinates = aiMove.row to aiMove.col
                } catch (e: Exception) {
                    coordinates = calculateAiCoordinates(state.playerGrid)
                }
            } else {
                coordinates = calculateAiCoordinates(state.playerGrid)
            }

            val (r, c) = coordinates

            if (r !in 0 until gridSize || c !in 0 until gridSize || aiHitMap.contains(r to c)) {
                val safe = getRandomCoordinates()
                performShot(safe.first, safe.second)
            } else {
                performShot(r, c)
            }

            state = _uiState.value
        }
    }

    private fun performShot(r: Int, c: Int) {
        val state = _uiState.value
        val targetCell = state.playerGrid[r][c]
        val isHit = (targetCell.status == CellStatus.SHIP)

        val newStatus = if (isHit) CellStatus.HIT else CellStatus.MISS

        aiHitMap.add(r to c)

        if (isHit) settingsManager.triggerVibration()

        if (isHit && difficulty != "Easy") {
            addNeighborsToStack(r, c)
        }

        val updatedPlayerGrid = state.playerGrid.map { rowList ->
            rowList.map { cell ->
                if (cell.row == r && cell.col == c)
                    cell.copy(status = newStatus)
                else
                    cell
            }
        }

        var newShots = state.shotsRemaining
        var turnEnds = false

        if (firingRule == "Sequential hits") {
            if (!isHit) {
                newShots = 0
                turnEnds = true
            }
        } else {
            newShots -= 1
            if (newShots <= 0) turnEnds = true
        }

        val pAlive = countShipsAlive(updatedPlayerGrid)
        val pComposition = calculateFleetComposition(updatedPlayerGrid)
        val isAiWin = (pAlive == 0)

        var resultLog = "MISS"
        if (isHit) {
            val sunk = isShipSunk(updatedPlayerGrid, targetCell.shipId)
            resultLog = if (sunk) "SUNK" else "HIT"
        }

        moveLogs.add(
            MoveLog(
                matchId = 0,
                turnNumber = state.turnNumber,
                playerId = "AI",
                row = r,
                col = c,
                result = resultLog
            )
        )

        _uiState.update {
            it.copy(
                playerGrid = updatedPlayerGrid,
                shotsRemaining = newShots,
                playerShipsAlive = pAlive,
                playerShipsComposition = pComposition,
                isGameOver = isAiWin,
                isPlayerWinner = false,
                winnerName = if (isAiWin) "$difficulty AI" else ""
            )
        }

        if (isAiWin) {
            saveMatchToDb(false)
            settingsManager.manageMusic(false) 
        } else if (turnEnds) {
            viewModelScope.launch {
                delay(1000)
                switchTurn()
            }
        }
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

    private fun convertGridForAi(grid: List<List<GridCell>>): List<List<Int>> {
        return grid.map { row ->
            row.map { cell ->
                if (cell.shipId > 0 && isShipSunk(grid, cell.shipId))
                    3
                else {
                    when (cell.status) {
                        CellStatus.EMPTY -> 0
                        CellStatus.SHIP -> 0
                        CellStatus.HIT -> 1
                        CellStatus.MISS -> 2
                    }
                }
            }
        }
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

    private fun placeAiShips(grid: List<List<GridCell>>, shipSizes: List<Int>) {
        for ((index, size) in shipSizes.withIndex()) {
            var placed = false
            while (!placed) {
                val isHor = Random.nextBoolean()
                val row = Random.nextInt(gridSize)
                val col = Random.nextInt(gridSize)
                if (canPlace(grid, row, col, size, isHor)) {
                    val shipId = index + 1
                    for (i in 0 until size) {
                        val r = if (isHor) row else row + i
                        val c = if (isHor) col + i else col
                        grid[r][c].status = CellStatus.SHIP
                        grid[r][c].shipId = shipId
                        grid[r][c].originalSize = size
                    }
                    placed = true
                }
            }
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

    private fun createEmptyGrid(): List<List<GridCell>> {
        return List(gridSize) { r -> List(gridSize) { c -> GridCell(r, c) } }
    }

    private fun calculateAiCoordinates(grid: List<List<GridCell>>): Pair<Int, Int> {
        if (difficulty != "Easy" && aiTargetStack.isNotEmpty()) {
            val candidate = aiTargetStack.removeFirst()
            if (!aiHitMap.contains(candidate)) return candidate
            return calculateAiCoordinates(grid)
        }
        return when (difficulty) {
            "Easy" -> getRandomCoordinates()
            "Normal" -> getRandomCoordinates()
            "Hard" -> getHardCoordinates()
            else -> getRandomCoordinates()
        }
    }

    private fun getRandomCoordinates(): Pair<Int, Int> {
        var r: Int; var c: Int
        do { r = Random.nextInt(gridSize); c = Random.nextInt(gridSize) } while (aiHitMap.contains(r to c))
        return r to c
    }

    private fun getHardCoordinates(): Pair<Int, Int> {
        var r: Int; var c: Int; var attempts = 0
        do {
            r = Random.nextInt(gridSize); c = Random.nextInt(gridSize)
            attempts++
        } while (aiHitMap.contains(r to c) || ((r + c) % 2 != 0 && attempts < 100))
        return r to c
    }

    private fun addNeighborsToStack(r: Int, c: Int) {
        val offsets = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        for ((dr, dc) in offsets) {
            val nr = r + dr
            val nc = c + dc
            if (nr in 0 until gridSize && nc in 0 until gridSize) {
                if (!aiHitMap.contains(nr to nc)) aiTargetStack.addFirst(nr to nc)
            }
        }
    }

    private fun calculateShots(isPlayer: Boolean, shipsAlive: Int): Int {
        return when (firingRule) {
            "One shot" -> 1
            "Chain attacks" -> shipsAlive
            "Sequential hits" -> 1
            else -> 1
        }
    }

    private fun canPlace(grid: List<List<GridCell>>, r: Int, c: Int, size: Int, isHor: Boolean): Boolean {
        for (i in 0 until size) {
            val nr = if (isHor) r else r + i
            val nc = if (isHor) c + i else c
            if (nr >= gridSize || nc >= gridSize) return false
            if (grid[nr][nc].status != CellStatus.EMPTY) return false
        }
        return true
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

        val user = _uiState.value.playerUser ?: return

        viewModelScope.launch {
            userRepository.saveMatchRecord(user.id, "$difficulty AI", false, difficulty, moveLogs)
        }
    }

    private fun saveMatchToDb(isVictory: Boolean) {
        val currentState = MatchStateStorage.getState(context) ?: return

        MatchStateStorage.clear(context)

        val user = _uiState.value.playerUser ?: return

        viewModelScope.launch {
            userRepository.saveMatchRecord(user.id, "$difficulty AI", isVictory, difficulty, moveLogs)
        }
    }

    private fun isShipSunk(grid: List<List<GridCell>>, shipId: Int): Boolean {
        if (shipId <= 0) return false

        for (row in grid) {
            for (cell in row) {
                if (cell.shipId == shipId && cell.status == CellStatus.SHIP)
                    return false
            }
        }

        return true
    }

    override fun onCleared() {
        super.onCleared()
        settingsManager.releaseResources()
    }
}

class MatchVsComputerViewModelFactory(
    private val context: Context,
    private val userDao: UserDao,
    private val fleetDao: FleetDao,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MatchVsComputerViewModel::class.java)) {
            return MatchVsComputerViewModel(context, userDao, fleetDao, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}