package com.example.brave_sailors.data.local.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.brave_sailors.data.local.database.entity.User
import com.example.brave_sailors.data.local.database.entity.FriendEntity
import com.example.brave_sailors.data.local.database.entity.MatchResult
import com.example.brave_sailors.data.local.database.entity.MoveLog
import kotlinx.coroutines.flow.Flow
import com.example.brave_sailors.data.local.database.entity.SavedShip

@Dao
interface UserDao {

    // Inserts a user into the local DB.
    // Used both for:
    // - Registration (new user)
    // - Syncing Google data (existing user)
    // REPLACE ensures we overwrite the local row if the same id already exists.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    // Returns a reactive stream of the (single) stored user.
    // Emits updates whenever the user row changes.
    @Query("SELECT * FROM users LIMIT 1")
    fun getUserFlow(): Flow<User?>

    // Retrieves a user by id (one-shot).
    // Used during Google sign-in to check whether the user already exists locally.
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): User?

    // Updates the entire user entity.
    // Use when you already have the full object with all fields.
    @Update
    suspend fun updateUser(user: User)

    // Observes a user by id as a Flow.
    // Useful for ViewModels to keep the UI in sync with DB changes.
    @Query("SELECT * FROM users WHERE id = :id")
    fun observeUserById(id: String): Flow<User?>

    // Returns the currently stored user (one-shot).
    // Used in MainActivity (or similar) to check if auto-login is possible.
    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getCurrentUser(): User?

    // --- SECURITY / SESSION MANAGEMENT ---

    // Clears all local users.
    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()

    // Deletes the provided user row.
    // Useful when you want to remove a specific user instance instead of wiping the whole table.
    @Delete
    suspend fun delete(user: User)

    // --- PARTIAL (FIELD-LEVEL) UPDATES ---

    // Updates only the user's name.
    // Avoids rewriting the whole row when only one field changes.
    @Query("UPDATE users SET name = :name WHERE id = :id")
    suspend fun updateUserName(id: String, name: String)

    // Updates only the user's country code.
    @Query("UPDATE users SET countryCode = :countryCode WHERE id = :id")
    suspend fun updateCountryCode(id: String, countryCode: String)

    // Updates only the profile picture URL and the lastUpdated timestamp.
    // Useful when the remote profile image changes and you want to track freshness locally.
    @Query("UPDATE users SET profilePictureUrl = :url, lastUpdated = :timestamp WHERE id = :id")
    suspend fun updateProfilePicture(id: String, url: String, timestamp: Long)

    // Updates game-related stats and the timestamp of the last win.
    // Keeping this as a partial update avoids overwriting unrelated fields.
    @Query("UPDATE users SET level = :level, currentXp = :xp, lastWinTimestamp = :timestamp WHERE id = :id")
    suspend fun updateGameStats(id: String, level: Int, xp: Int, timestamp: Long)

    // Updates ONLY the last win timestamp.
    // [ NOTE ]: Used to trigger the cooldown timer for training/mini-games.
    @Query("UPDATE users SET lastWinTimestamp = :timestamp WHERE id = :id")
    suspend fun updateLastWinTimestamp(id: String, timestamp: Long)

    // Clears the session token (sets it to NULL) for the given user.
    // Typically called on logout or when the token is invalid/expired.
    @Query("UPDATE users SET sessionToken = NULL WHERE id = :userId")
    suspend fun clearSession(userId: String)
}

@Dao
interface FleetDao {

    // Loads the saved fleet for a given user.
    // Returns a one-shot list (not reactive).
    @Query("SELECT * FROM user_fleet WHERE userId = :userId")
    suspend fun getUserFleet(userId: String): List<SavedShip>

    @Query("SELECT * FROM user_fleet WHERE userId = :userId")
    fun getUserFleetFlow(userId: String): Flow<List<SavedShip>>

    // Saves a list of ships to the local DB.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFleet(ships: List<SavedShip>)

    // Deletes all ships saved for the given user.
    // Used to reset the fleet locally (e.g., on logout or re-sync).
    @Query("DELETE FROM user_fleet WHERE userId = :userId")
    suspend fun clearUserFleet(userId: String)

    @Query("DELETE FROM user_fleet")
    suspend fun deleteAllFleets()

    // Atomically replaces the entire fleet for a user:
    // 1) clear existing fleet rows
    // 2) insert the new list
    // @Transaction ensures both steps are treated as one DB operation.
    @Transaction
    suspend fun replaceUserFleet(userId: String, ships: List<SavedShip>) {
        clearUserFleet(userId)
        saveFleet(ships)
    }
}

@Dao
interface FriendDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: FriendEntity)

    @Query("SELECT * FROM friends")
    fun getAllFriendsFlow(): Flow<List<FriendEntity>>

    @Query("DELETE FROM friends WHERE id NOT IN (:activeIds)")
    suspend fun deleteFriendsNotIn(activeIds: List<String>)

    @Query("DELETE FROM friends")
    suspend fun deleteAllFriends()
}

@Dao
interface MatchDao {

    @Insert
    suspend fun insertMatch(match: MatchResult): Long // Returns the generated ID

    @Insert
    suspend fun insertMoves(moves: List<MoveLog>)

    // Save everything together in a single transaction
    @Transaction
    suspend fun saveFullMatch(match: MatchResult, moves: List<MoveLog>) {
        val matchId = insertMatch(match)
        // Update the match ID in the moves (since matchId is auto-generated)
        val movesWithId = moves.map { it.copy(matchId = matchId) }
        insertMoves(movesWithId)
    }

    // Get the user's match history
    @Query("SELECT * FROM match_history WHERE player1Id = :userId ORDER BY timestamp DESC")
    fun getMatchHistory(userId: String): Flow<List<MatchResult>>

    // Get details (moves) of a specific match
    @Query("SELECT * FROM move_logs WHERE matchId = :matchId ORDER BY turnNumber ASC")
    suspend fun getMovesForMatch(matchId: Long): List<MoveLog>

    @Query("DELETE FROM match_history WHERE player1Id = :userId")
    suspend fun deleteMatchesForUser(userId: String)

    @Query("DELETE FROM match_history")
    suspend fun deleteAllMatches()

    @Query("DELETE FROM move_logs")
    suspend fun deleteAllMoves()
}