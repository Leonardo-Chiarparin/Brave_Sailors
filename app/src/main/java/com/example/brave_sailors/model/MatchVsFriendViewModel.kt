package com.example.brave_sailors.model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.concurrent.AtomicBoolean
import com.example.brave_sailors.LobbyPlayer
import com.example.brave_sailors.data.local.MatchStateStorage
import com.example.brave_sailors.data.local.database.FleetDao
import com.example.brave_sailors.data.local.database.UserDao
import com.example.brave_sailors.data.local.database.entity.MoveLog
import com.example.brave_sailors.data.local.database.entity.SavedShip
import com.example.brave_sailors.data.local.database.entity.User
import com.example.brave_sailors.data.repository.UserRepository
import com.example.brave_sailors.ui.utils.GameSettingsManager
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MatchFriendUiState(
    val playerUser: User? = null,
    val opponent: LobbyPlayer? = null,
    val isPlayerTurn: Boolean = true,
    val turnNumber: Int = 1,
    val playerGrid: List<List<GridCell>> = emptyList(),
    val enemyGrid: List<List<GridCell>> = emptyList(),
    val displayGrid: List<List<GridCell>> = emptyList(),
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
    context: Context,
    private val userDao: UserDao,
    private val fleetDao: FleetDao,
    private val userRepository: UserRepository
) : ViewModel() {
    private val appContext = context.applicationContext

    private val settingsManager = GameSettingsManager(context)

    private val _uiState = MutableStateFlow(MatchFriendUiState())
    val uiState = _uiState.asStateFlow()

    private val dbRef = FirebaseDatabase.getInstance().getReference("matches")
    private var matchIdString: String = ""
    private val gridSize = 8

    private var firingRule = "One shot"
    private val moveLogs = mutableListOf<MoveLog>()

    private var heartbeatJob: Job? = null
    private var watchdogJob: Job? = null
    private var opponentPresenceListener: ValueEventListener ?= null

    private var lastLocalReceiptTime: Long = System.currentTimeMillis()
    private val onlineTimeoutMs = 60000L

    private val isProcessingGameEnd = AtomicBoolean(false)

    fun initializeMatch(opponent: LobbyPlayer, rule: String, uniqueMatchId: String) {
        isProcessingGameEnd.set(false)

        _uiState.value = MatchFriendUiState()
        moveLogs.clear()

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

            val initialEnemyGrid = createEmptyGrid()

            val initialDisplayGrid = if (isMyTurn)
                getEnemyGridForDisplay(initialEnemyGrid)
            else
                pGrid


            _uiState.update {
                it.copy(
                    playerUser = user,
                    opponent = opponent,
                    playerGrid = pGrid,
                    enemyGrid = initialEnemyGrid,
                    displayGrid = initialDisplayGrid,
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

            startPresenceSystem(uniqueMatchId, opponentId)
        }
    }

    private fun endGame(
        isVictory: Boolean,
        reason: String,
        winnerName: String
    ) {
        if (isProcessingGameEnd.getAndSet(true)) {
            return
        }

        MatchStateStorage.clear(appContext)

        stopPresenceSystem()
        settingsManager.manageMusic(false)

        val currentState = _uiState.value
        _uiState.update {
            it.copy(
                isGameOver = true,
                isPlayerWinner = isVictory,
                winnerName = winnerName
            )
        }

        viewModelScope.launch {
            userRepository.saveMatchRecord(
                userId = currentState.playerUser?.id ?: "",
                opponentName = currentState.opponent?.name ?: "",
                isVictory = isVictory,
                difficulty = reason,
                moves = moveLogs
            )
        }
    }

    private fun startPresenceSystem(matchId: String, opponentId: String) {
        lastLocalReceiptTime = System.currentTimeMillis()

        val opponentPresenceRef = dbRef.child(matchId).child("presence").child(opponentId)
        opponentPresenceListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                lastLocalReceiptTime = System.currentTimeMillis()
            }
            override fun onCancelled(error: DatabaseError) { }
        }
        opponentPresenceRef.addValueEventListener(opponentPresenceListener!!)

        startHeartbeat()

        startWatchdog()
    }

    private fun stopPresenceSystem() {
        heartbeatJob?.cancel()
        watchdogJob?.cancel()

        val opponentId = _uiState.value.opponent?.id
        if (opponentId != null && opponentPresenceListener != null) {
            dbRef.child(matchIdString).child("presence").child(opponentId)
                .removeEventListener(opponentPresenceListener!!)
        }

        val currentUserId = _uiState.value.playerUser?.id
        if (currentUserId != null) {
            dbRef.child(matchIdString).child("presence").child(currentUserId).removeValue()
        }
    }

    private fun handleOpponentTimeout() {
        val state = _uiState.value

        if (state.isGameOver) return

        val opponentId = state.opponent?.id ?: "Opponent"
        dbRef.child(matchIdString).child("status").setValue("timeout_$opponentId")

        endGame(
            isVictory = true,
            reason = "Timeout",
            winnerName = state.playerUser?.name ?: "You"
        )
    }

    private fun startWatchdog() {
        watchdogJob?.cancel()

        watchdogJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(10000)

                val timeSinceLastSeen = System.currentTimeMillis() - lastLocalReceiptTime

                if (timeSinceLastSeen > onlineTimeoutMs) {
                    withContext(Dispatchers.Main) {
                        handleOpponentTimeout()
                    }
                    break
                }
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        val currentUserId = _uiState.value.playerUser?.id ?: return

        heartbeatJob = viewModelScope.launch(Dispatchers.IO) {
            val myPresenceRef = dbRef.child(matchIdString).child("presence").child(currentUserId)
            while (isActive) {
                try {
                    myPresenceRef.setValue(ServerValue.TIMESTAMP)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(5000)
            }
        }
    }

    fun onLifecyclePause() {
        settingsManager.onAppPause()

        watchdogJob?.cancel()
        heartbeatJob?.cancel()
    }

    fun onLifecycleResume() {
        val state = _uiState.value

        if (state.isGameOver) return
        settingsManager.onAppResume()


        if (state.opponent != null) {
            lastLocalReceiptTime = System.currentTimeMillis()

            startHeartbeat()
            startWatchdog()
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

    private fun getEnemyGridForDisplay(realEnemyGrid: List<List<GridCell>>): List<List<GridCell>> {
        return realEnemyGrid.map { row ->
            row.map { cell ->
                when (cell.status) {
                    CellStatus.HIT -> cell.copy(shipId = 0)
                    CellStatus.MISS -> cell.copy(shipId = 0)

                    CellStatus.SHIP -> cell.copy(status = CellStatus.EMPTY, shipId = 0)

                    else -> cell
                }
            }
        }
    }

    private fun listenForMatchStatus() {
        dbRef.child(matchIdString).child("status").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.getValue(String::class.java) ?: return
                val state = _uiState.value

                if (status.startsWith("surrender_", ignoreCase = true)) {
                    val surrenderingId = status.removePrefix("surrender_")
                    if (surrenderingId != state.playerUser?.id && !state.isGameOver) {
                        handleOpponentSurrender()
                    }
                }
                else if (status.startsWith("timeout_", ignoreCase = true)) {
                    val timedOutId = status.removePrefix("timeout_")

                    if (timedOutId == state.playerUser?.id && !state.isGameOver) {
                        endGame(false, "Timeout", state.opponent?.name ?: "Opponent")
                    }
                    else if (timedOutId != state.playerUser?.id && !state.isGameOver) {
                        endGame(true, "Timeout", state.playerUser?.name ?: "You")
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun handleOpponentSurrender() {
        val state = _uiState.value

        endGame(
            isVictory = true,
            reason = "Surrender",
            winnerName = state.playerUser?.name ?: "You"
        )
    }

    private fun listenForOpponentFleet(opponentId: String) {
        viewModelScope.launch {
            userRepository.listenForOpponentFleet(matchIdString, opponentId).collect { remoteFleet ->
                _uiState.update { state ->
                    val baseGrid = state.enemyGrid.ifEmpty { createEmptyGrid() }

                    placeFleetOnGrid(baseGrid, remoteFleet)

                    state.copy(
                        enemyGrid = baseGrid,
                        enemyShipsAlive = countShipsAlive(baseGrid),
                        enemyShipsComposition = calculateFleetComposition(baseGrid)
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

        var resultString = "MISS"

        if (isHit) {
            val intactParts = state.enemyGrid.flatten().count {
                it.shipId == targetCell.shipId && it.status == CellStatus.SHIP
            }

            resultString = if (intactParts == 1) "SUNK" else "HIT"
        }

        if (isHit) settingsManager.triggerVibration()

        moveLogs.add(
            MoveLog(
                matchId = 0L,
                turnNumber = state.turnNumber,
                playerId = state.playerUser?.id ?: "Player",
                row = row,
                col = col,
                result = resultString
            )
        )

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
        dbRef.child(matchIdString).child("moves").addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val state = _uiState.value
                val pId = snapshot.child("playerId").getValue(String::class.java)

                if (pId == state.playerUser?.id) return

                val r = snapshot.child("row").getValue(Int::class.java) ?: 0
                val c = snapshot.child("col").getValue(Int::class.java) ?: 0
                val res = snapshot.child("result").getValue(String::class.java) ?: "MISS"

                processMoveLocally(r, c, (res == "HIT" || res == "SUNK"), false)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun processMoveLocally(r: Int, c: Int, isHit: Boolean, isMyMove: Boolean) {
        if (!isMyMove && isHit) settingsManager.triggerVibration()

        var gameEnded = false
        var isWin = false
        var winnerName = ""

        _uiState.update { state ->
            val targetGrid = if (isMyMove) state.enemyGrid else state.playerGrid

            if (targetGrid.isEmpty()) return@update state

            val newStatus = if (isHit) CellStatus.HIT else CellStatus.MISS

            val updatedGrid = targetGrid.map { rowList ->
                rowList.map { cell ->
                    if (cell.row == r && cell.col == c)
                        cell.copy(status = newStatus)
                    else
                        cell
                }
            }

            val finalPlayerGrid = if (!isMyMove) updatedGrid else state.playerGrid
            val finalEnemyGrid = if (isMyMove) updatedGrid else state.enemyGrid

            val pAlive = countShipsAlive(finalPlayerGrid)
            val eAlive = countShipsAlive(finalEnemyGrid)

            val pComp = calculateFleetComposition(finalPlayerGrid)
            val eComp = calculateFleetComposition(finalEnemyGrid)

            var newShots = state.shotsRemaining - 1
            var turnEnds = false

            if (firingRule == "Sequential hits") {
                if (!isHit) { turnEnds = true; newShots = 0 } else { newShots = 1 }
            } else if (newShots <= 0) {
                turnEnds = true
            }

            isWin = isMyMove && (eAlive == 0)
            val isLoss = !isMyMove && (pAlive == 0)

            if (isWin || isLoss) {
                gameEnded = true
                winnerName = if (isWin) ( state.playerUser?.name ?: "You" ) else ( state.opponent?.name ?: "Opponent" )

                return@update state.copy(
                    playerGrid = finalPlayerGrid,
                    enemyGrid = finalEnemyGrid,
                    displayGrid = if (isWin) getEnemyGridForDisplay(finalEnemyGrid) else finalPlayerGrid,
                    isGameOver = true,
                    isPlayerWinner = isWin,
                    winnerName = winnerName,
                    playerShipsAlive = pAlive,
                    enemyShipsAlive = eAlive,
                    playerShipsComposition = pComp,
                    enemyShipsComposition = eComp
                )
            }

            if (turnEnds) {
                viewModelScope.launch {
                    delay(1000)
                    handleTurnSwitch(isWin, isLoss)
                }

                state.copy(
                    playerGrid = finalPlayerGrid,
                    enemyGrid = finalEnemyGrid,
                    displayGrid = if (isMyMove) getEnemyGridForDisplay(finalEnemyGrid) else finalPlayerGrid,
                    shotsRemaining = 0,
                    playerShipsAlive = pAlive,
                    enemyShipsAlive = eAlive,
                    playerShipsComposition = pComp,
                    enemyShipsComposition = eComp
                )
            }
            else {
                state.copy(
                    playerGrid = finalPlayerGrid,
                    enemyGrid = finalEnemyGrid,
                    displayGrid = if (isMyMove) getEnemyGridForDisplay(finalEnemyGrid) else finalPlayerGrid,
                    shotsRemaining = newShots,
                    playerShipsAlive = pAlive,
                    enemyShipsAlive = eAlive,
                    playerShipsComposition = pComp,
                    enemyShipsComposition = eComp
                )
            }
        }

        if (gameEnded) {
            endGame(
                isVictory = isWin,
                reason = "Online",
                winnerName = winnerName
            )
        }
    }

    private fun handleTurnSwitch(isWin: Boolean, isLoss: Boolean) {
        if (isWin || isLoss) return

        _uiState.update { state ->
            val nextIsPlayerTurn = !state.isPlayerTurn
            val pAlive = state.playerShipsAlive
            val eAlive = state.enemyShipsAlive

            if (nextIsPlayerTurn) {
                settingsManager.triggerTurnAlarm()
            }

            val nextDisplayGrid = if (nextIsPlayerTurn)
                getEnemyGridForDisplay(state.enemyGrid)
            else
                state.playerGrid

            state.copy(
                isPlayerTurn = nextIsPlayerTurn,
                turnNumber = state.turnNumber + 1,
                shotsRemaining = calculateShots(if (nextIsPlayerTurn) pAlive else eAlive),
                displayGrid = nextDisplayGrid,
                playerShipsComposition = calculateFleetComposition(state.playerGrid),
                enemyShipsComposition = calculateFleetComposition(state.enemyGrid)
            )
        }
    }

    fun onRetire() {
        val state = _uiState.value
        val user = state.playerUser ?: return

        dbRef.child(matchIdString).child("status").setValue("surrender_${user.id}")

        endGame(
            isVictory = false,
            reason = "Surrender",
            winnerName = state.opponent?.name ?: "Opponent"
        )
    }

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
                    val currentCell = grid[r][c]

                    currentCell.shipId = shipColorId
                    currentCell.originalSize = ship.size

                    if (currentCell.status != CellStatus.HIT && currentCell.status != CellStatus.MISS)
                        currentCell.status = CellStatus.SHIP
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
        stopPresenceSystem()
        settingsManager.releaseResources()
    }
}

class MatchVsFriendViewModelFactory(
    private val context: Context,
    private val userDao: UserDao,
    private val fleetDao: FleetDao,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MatchVsFriendViewModel(context, userDao, fleetDao, userRepository) as T
    }
}