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
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users LIMIT 1")
    fun getUserFlow(): Flow<User?>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): User?

    @Update
    suspend fun updateUser(user: User)

    @Query("SELECT * FROM users WHERE id = :id")
    fun observeUserById(id: String): Flow<User?>

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getCurrentUser(): User?

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()

    @Delete
    suspend fun delete(user: User)

    @Query("UPDATE users SET name = :name WHERE id = :id")
    suspend fun updateUserName(id: String, name: String)

    @Query("UPDATE users SET countryCode = :countryCode WHERE id = :id")
    suspend fun updateCountryCode(id: String, countryCode: String)

    @Query("UPDATE users SET profilePictureUrl = :url, lastUpdated = :timestamp WHERE id = :id")
    suspend fun updateProfilePicture(id: String, url: String, timestamp: Long)

    @Query("UPDATE users SET level = :level, currentXp = :xp, lastWinTimestamp = :timestamp WHERE id = :id")
    suspend fun updateGameStats(id: String, level: Int, xp: Int, timestamp: Long)

    @Query("UPDATE users SET lastWinTimestamp = :timestamp WHERE id = :id")
    suspend fun updateLastWinTimestamp(id: String, timestamp: Long)

    @Query("UPDATE users SET sessionToken = NULL WHERE id = :userId")
    suspend fun clearSession(userId: String)
}

@Dao
interface FleetDao {
    @Query("SELECT * FROM user_fleet WHERE userId = :userId")
    suspend fun getUserFleet(userId: String): List<SavedShip>

    @Query("SELECT * FROM user_fleet WHERE userId = :userId")
    fun getUserFleetFlow(userId: String): Flow<List<SavedShip>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFleet(ships: List<SavedShip>)

    @Query("DELETE FROM user_fleet WHERE userId = :userId")
    suspend fun clearUserFleet(userId: String)

    @Query("DELETE FROM user_fleet")
    suspend fun deleteAllFleets()

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
    suspend fun insertMatch(match: MatchResult): Long

    @Insert
    suspend fun insertMoves(moves: List<MoveLog>)

    @Transaction
    suspend fun saveFullMatch(match: MatchResult, moves: List<MoveLog>) {
        val matchId = insertMatch(match)
        val movesWithId = moves.map { it.copy(matchId = matchId) }
        insertMoves(movesWithId)
    }

    @Query("SELECT * FROM match_history WHERE player1Id = :userId ORDER BY timestamp DESC")
    fun getMatchHistory(userId: String): Flow<List<MatchResult>>

    @Query("SELECT * FROM move_logs WHERE matchId = :matchId ORDER BY turnNumber ASC")
    suspend fun getMovesForMatch(matchId: Long): List<MoveLog>

    @Query("DELETE FROM match_history WHERE player1Id = :userId")
    suspend fun deleteMatchesForUser(userId: String)

    @Query("DELETE FROM match_history")
    suspend fun deleteAllMatches()

    @Query("DELETE FROM move_logs")
    suspend fun deleteAllMoves()
}