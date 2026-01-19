package com.example.brave_sailors.model

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.brave_sailors.LobbyPlayer
import com.example.brave_sailors.data.local.database.FriendDao
import com.example.brave_sailors.data.local.database.UserDao
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

data class LobbyUiState(
    val friends: List<LobbyPlayer> = emptyList(),
    val isMatching: Boolean = false,
    val matchFound: Boolean = false,
    val selectedOpponent: LobbyPlayer? = null,
    val activeMatchId: String? = null
)

class GameLobbyViewModel(
    private val userDao: UserDao,
    private val friendDao: FriendDao
) : ViewModel() {
    private val _uiState = MutableStateFlow(LobbyUiState())
    val uiState = _uiState.asStateFlow()

    private val dbRef = FirebaseDatabase.getInstance().getReference("matchmaking")

    private var matchmakingListener: ValueEventListener? = null
    private var handshakeNodeId: String? = null

    private val READY_TIMEOUT_MS = 60000L

    init {
        observeFriendsFromDatabase()
    }

    private fun observeFriendsFromDatabase() {
        viewModelScope.launch {
            friendDao.getAllFriendsFlow().collect { friendEntities ->
                val dbFriends = friendEntities
                    .filter { it.status == "ACCEPTED" }
                    .map { entity ->
                        LobbyPlayer(
                            id = entity.id,
                            name = entity.name,
                            countryCode = entity.countryCode ?: "IT",
                            avatarUrl = null,
                            wins = entity.wins,
                            losses = entity.losses
                        )
                    }

                _uiState.update { it.copy(friends = dbFriends) }
            }
        }
    }

    fun onSelectOpponent(player: LobbyPlayer, myRule: String) {
        viewModelScope.launch {
            val user = userDao.getCurrentUser() ?: return@launch

            val myId = user.id
            val opponentId = player.id
            val now = System.currentTimeMillis()

            val baseId = if (myId < opponentId) "${myId}_${opponentId}" else "${opponentId}_${myId}"
            handshakeNodeId = baseId

            if (myId < opponentId) {
                dbRef.child(baseId).child("gameId").removeValue()
            }

            _uiState.update { it.copy(isMatching = true, selectedOpponent = player) }

            val myData = mapOf(
                "timestamp" to now,
                "rule" to myRule
            )

            dbRef.child(baseId).child(myId).setValue(myData)

            matchmakingListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val opponentNode = snapshot.child(opponentId)

                    var isOpponentReady = false

                    val timestamp = opponentNode.child("timestamp").getValue(Long::class.java)
                    if (timestamp != null && abs(now - timestamp) < READY_TIMEOUT_MS) {

                        val opponentRule = opponentNode.child("rule").getValue(String::class.java)
                        if (opponentRule == myRule) {
                            isOpponentReady = true
                        } else {
                            Log.w("GameLobby", "Opponent ready but rules mismatch: Mine=$myRule vs Theirs=$opponentRule")
                        }
                    }

                    if (isOpponentReady) {
                        if (myId < opponentId) {
                            if (!snapshot.hasChild("gameId")) {
                                val uniqueGameId = "${baseId}_${System.currentTimeMillis()}"
                                dbRef.child(baseId).child("gameId").setValue(uniqueGameId)
                            }
                        }

                        val gameId = snapshot.child("gameId").getValue(String::class.java)

                        if (gameId != null) {
                            _uiState.update {
                                it.copy(
                                    isMatching = false,
                                    matchFound = true,
                                    activeMatchId = gameId
                                )
                            }
                            dbRef.child(baseId).child(myId).removeValue()
                            removeMatchmakingListener()
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            }
            dbRef.child(baseId).addValueEventListener(matchmakingListener!!)
        }
    }

    fun onCancelMatching() {
        viewModelScope.launch {
            val user = userDao.getCurrentUser() ?: return@launch
            handshakeNodeId?.let { id ->
                dbRef.child(id).child(user.id).removeValue()
            }
            removeMatchmakingListener()
            _uiState.update { it.copy(isMatching = false, selectedOpponent = null) }
        }
    }

    private fun removeMatchmakingListener() {
        matchmakingListener?.let {
            handshakeNodeId?.let { id -> dbRef.child(id).removeEventListener(it) }
        }
        matchmakingListener = null
    }

    fun onMatchStartedConsumed() {
        _uiState.update { it.copy(matchFound = false) }
    }

    override fun onCleared() {
        super.onCleared()
        removeMatchmakingListener()
    }
}

class GameLobbyViewModelFactory(
    private val userDao: UserDao,
    private val friendDao: FriendDao
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GameLobbyViewModel(userDao, friendDao) as T
    }
}