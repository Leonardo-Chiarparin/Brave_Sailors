package com.example.brave_sailors.model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.brave_sailors.LobbyPlayer
import com.example.brave_sailors.data.local.database.FleetDao
import com.example.brave_sailors.data.local.database.UserDao
import com.example.brave_sailors.data.local.database.entity.MoveLog
import com.example.brave_sailors.data.local.database.entity.SavedShip
import com.example.brave_sailors.data.local.database.entity.User
import com.example.brave_sailors.data.repository.UserRepository
import com.example.brave_sailors.ui.utils.GameSettingsManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MatchFriendUiState(
    val playerUser: User? = null,
    val opponent: LobbyPlayer? = null,
    val isPlayerTurn: Boolean = true,
    val turnNumber: Int = 1,
    val playerGrid: List<List<GridCell>> = emptyList(),
    val enemyGrid: List<List<GridCell>> = emptyList(),
    val playerShipsAlive: Int = 0,
    val enemyShipsAlive: Int = 0,
    val playerShipsComposition: Map<Int, Int> = emptyMap(),
    val enemyShipsComposition: Map<Int, Int> = emptyMap(),
    val shotsRemaining: Int = 1,
    val isGameOver: Boolean = false,
    val winnerName: String = "",
    val isPlayerWinner: Boolean = false
)

class MatchVsFriendViewModel(
    context: Context, // [ FIX ]
    private val userDao: UserDao,
    private val fleetDao: FleetDao,
    private val userRepository: UserRepository
) : ViewModel() {

    private val settingsManager = GameSettingsManager(context)

    private val _uiState = MutableStateFlow(MatchFriendUiState())
    val uiState = _uiState.asStateFlow()

    private val dbRef = FirebaseDatabase.getInstance().getReference("matches")
    private var matchIdString: String = ""
    private val gridSize = 8

    private var firingRule = "One shot"
    private val moveLogs = mutableListOf<MoveLog>()

    fun initializeMatch(opponent: LobbyPlayer, rule: String, uniqueMatchId: String) {
        _uiState.value = MatchFriendUiState()
        moveLogs.clear()

        // Avvia Musica
        settingsManager.manageMusic(settingsManager.isMusicOn)

        this.firingRule = rule
        this.matchIdString = uniqueMatchId

        viewModelScope.launch {
            val user = userDao.getCurrentUser() ?: return@launch
            val userFleet = fleetDao.getUserFleet(user.id)
            if (userFleet.isEmpty()) return@launch

            val opponentId = opponent.id

            if (user.id < opponentId) {
                dbRef.child(matchIdString).child("rule").setValue(rule)
            }

            val pGrid = createEmptyGrid()
            placeFleetOnGrid(pGrid, userFleet)

            val isMyTurn = user.id < opponentId

            _uiState.update {
                it.copy(
                    playerUser = user,
                    opponent = opponent,
                    playerGrid = pGrid,
                    playerShipsAlive = userFleet.size,
                    playerShipsComposition = calculateFleetComposition(pGrid),
                    isPlayerTurn = isMyTurn,
                    shotsRemaining = calculateShots(userFleet.size)
                )
            }

            if (isMyTurn) settingsManager.triggerTurnAlarm()

            userRepository.uploadFleetToMatch(matchIdString, user.id)

            listenForOpponentFleet(opponentId)
            listenForOpponentMoves()
            listenForGameRule()
            listenForMatchStatus()
        }
    }

    private fun listenForGameRule() {
        dbRef.child(matchIdString).child("rule").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val syncedRule = snapshot.getValue(String::class.java)
                if (syncedRule != null) {
                    firingRule = syncedRule
                    val currentShips = _uiState.value.playerShipsAlive
                    _uiState.update { it.copy(shotsRemaining = calculateShots(currentShips)) }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun listenForMatchStatus() {
        dbRef.child(matchIdString).child("status").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.getValue(String::class.java) ?: return
                val state = _uiState.value

                if (status.startsWith("surrender_")) {
                    val surrenderingId = status.removePrefix("surrender_")
                    if (surrenderingId != state.playerUser?.id && !state.isGameOver) {
                        handleOpponentSurrender()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun handleOpponentSurrender() {
        val state = _uiState.value
        _uiState.update {
            it.copy(
                isGameOver = true,
                isPlayerWinner = true,
                winnerName = state.playerUser?.name ?: "You"
            )
        }
        viewModelScope.launch {
            userRepository.saveMatchRecord(
                userId = state.playerUser?.id ?: "",
                opponentName = state.opponent?.name ?: "",
                isVictory = true,
                difficulty = "Online",
                moves = moveLogs
            )
        }
        settingsManager.manageMusic(false)
    }

    private fun listenForOpponentFleet(opponentId: String) {
        viewModelScope.launch {
            userRepository.listenForOpponentFleet(matchIdString, opponentId).collect { remoteFleet ->
                val eGrid = createEmptyGrid()
                placeFleetOnGrid(eGrid, remoteFleet)

                _uiState.update { state ->
                    state.copy(
                        enemyGrid = eGrid,
                        enemyShipsAlive = remoteFleet.size,
                        enemyShipsComposition = calculateFleetComposition(eGrid)
                    )
                }
            }
        }
    }

    fun onPlayerFire(row: Int, col: Int) {
        val state = _uiState.value
        if (!state.isPlayerTurn || state.isGameOver || state.shotsRemaining <= 0) return

        if (state.enemyGrid.isEmpty()) return
        val targetCell = state.enemyGrid[row][col]
        if (targetCell.status == CellStatus.HIT || targetCell.status == CellStatus.MISS) return

        val isHit = (targetCell.status == CellStatus.SHIP)
        val resultString = if (isHit) "HIT" else "MISS"

        // [ VIBRAZIONE ]
        if (isHit) settingsManager.triggerVibration()

        moveLogs.add(MoveLog(
            matchId = 0L,
            turnNumber = state.turnNumber,
            playerId = state.playerUser?.id ?: "Player",
            row = row,
            col = col,
            result = resultString
        ))

        val moveData = mapOf(
            "playerId" to state.playerUser?.id,
            "row" to row,
            "col" to col,
            "result" to resultString,
            "turnNumber" to state.turnNumber
        )
        dbRef.child(matchIdString).child("moves").push().setValue(moveData)

        processMoveLocally(row, col, isHit, true)
    }

    private fun listenForOpponentMoves() {
        dbRef.child(matchIdString).child("moves").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val state = _uiState.value
                val lastMove = snapshot.children.lastOrNull() ?: return

                val pId = lastMove.child("playerId").getValue(String::class.java)
                val r = lastMove.child("row").getValue(Int::class.java) ?: 0
                val c = lastMove.child("col").getValue(Int::class.java) ?: 0
                val res = lastMove.child("result").getValue(String::class.java) ?: "MISS"

                if (pId != state.playerUser?.id) {
                    processMoveLocally(r, c, res == "HIT", false)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun processMoveLocally(r: Int, c: Int, isHit: Boolean, isMyMove: Boolean) {
        // [ VIBRAZIONE ]: Vibra anche se colpisce l'avversario (feedback ricezione)
        if (!isMyMove && isHit) settingsManager.triggerVibration()

        _uiState.update { state ->
            val targetGrid = if (isMyMove) state.enemyGrid else state.playerGrid
            if (targetGrid.isEmpty()) return@update state

            targetGrid[r][c].status = if (isHit) CellStatus.HIT else CellStatus.MISS

            val pAlive = countShipsAlive(state.playerGrid)
            val eAlive = countShipsAlive(state.enemyGrid)

            var newShots = state.shotsRemaining - 1
            var turnEnds = false

            if (firingRule == "Sequential hits") {
                if (!isHit) { turnEnds = true; newShots = 0 } else { newShots = 1 }
            } else if (newShots <= 0) {
                turnEnds = true
            }

            val isWin = (eAlive == 0)
            val isLoss = (pAlive == 0)

            if (isWin || isLoss) {
                viewModelScope.launch {
                    userRepository.saveMatchRecord(
                        userId = state.playerUser?.id ?: "",
                        opponentName = state.opponent?.name ?: "",
                        isVictory = isWin,
                        difficulty = "Online",
                        moves = moveLogs
                    )
                }
                settingsManager.manageMusic(false)
            } else {
                // [ ALARM ]: Se il turno finisce e il prossimo NON è dell'altro (cioè è il mio)
                // Nota: nextIsPlayerTurn = if (turnEnds) !state.isPlayerTurn
                val nextIsPlayerTurn = if (turnEnds) !state.isPlayerTurn else state.isPlayerTurn
                if (turnEnds && nextIsPlayerTurn) {
                    settingsManager.triggerTurnAlarm()
                }
            }

            state.copy(
                playerShipsAlive = pAlive,
                enemyShipsAlive = eAlive,
                playerShipsComposition = calculateFleetComposition(state.playerGrid),
                enemyShipsComposition = calculateFleetComposition(state.enemyGrid),
                shotsRemaining = if (turnEnds) calculateShots(if (isMyMove) pAlive else eAlive) else newShots,
                isPlayerTurn = if (turnEnds) !state.isPlayerTurn else state.isPlayerTurn,
                turnNumber = if (turnEnds && !state.isPlayerTurn) state.turnNumber + 1 else state.turnNumber,
                isGameOver = isWin || isLoss,
                isPlayerWinner = isWin,
                winnerName = if (isWin) (state.playerUser?.name ?: "You") else if (isLoss) (state.opponent?.name ?: "Opponent") else ""
            )
        }
    }

    fun onRetire() {
        val state = _uiState.value
        val user = state.playerUser ?: return

        dbRef.child(matchIdString).child("status").setValue("surrender_${user.id}")

        _uiState.update {
            it.copy(
                isGameOver = true,
                isPlayerWinner = false,
                winnerName = state.opponent?.name ?: "Opponent"
            )
        }

        viewModelScope.launch {
            userRepository.saveMatchRecord(
                userId = user.id,
                opponentName = state.opponent?.name ?: "Opponent",
                isVictory = false,
                difficulty = "Online",
                moves = moveLogs
            )
        }
        settingsManager.manageMusic(false)
    }

    // --- HELPERS ---
    private fun createEmptyGrid() = List(8) { r -> List(8) { c -> GridCell(r, c) } }

    private fun countShipsAlive(grid: List<List<GridCell>>): Int {
        if (grid.isEmpty()) return 0
        val aliveShipIds = mutableSetOf<Int>()
        for (row in grid) {
            for (cell in row) {
                if (cell.status == CellStatus.SHIP) aliveShipIds.add(cell.shipId)
            }
        }
        return aliveShipIds.size
    }

    private fun calculateFleetComposition(grid: List<List<GridCell>>): Map<Int, Int> {
        if (grid.isEmpty()) return emptyMap()
        val allShips = mutableMapOf<Int, Int>()
        val aliveShips = mutableSetOf<Int>()
        for (row in grid) {
            for (cell in row) {
                if (cell.shipId > 0) {
                    allShips[cell.shipId] = cell.originalSize
                    if (cell.status == CellStatus.SHIP) aliveShips.add(cell.shipId)
                }
            }
        }
        val composition = mutableMapOf<Int, Int>()
        for (size in allShips.values) composition[size] = 0
        for (id in aliveShips) {
            val size = allShips[id] ?: 1
            composition[size] = (composition[size] ?: 0) + 1
        }
        return composition
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

    private fun calculateShots(shipsAlive: Int): Int {
        return when (firingRule) {
            "One shot" -> 1
            "Chain attacks" -> shipsAlive.coerceAtLeast(1)
            "Sequential hits" -> 1
            else -> 1
        }
    }

    override fun onCleared() {
        super.onCleared()
        settingsManager.releaseMusic()
    }
}

class MatchVsFriendViewModelFactory(
    private val context: Context, // [ FIX ]
    private val userDao: UserDao,
    private val fleetDao: FleetDao,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MatchVsFriendViewModel(context, userDao, fleetDao, userRepository) as T
    }
}