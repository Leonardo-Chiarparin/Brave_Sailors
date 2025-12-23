package com.example.brave_sailors.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.brave_sailors.data.local.database.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): User?

    @Update
    suspend fun updateUser(user: User)

    @Query("SELECT * FROM users WHERE id = :id")
    fun observeUserById(id: String): Flow<User?>

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getCurrentUser(): User?

    @Query("UPDATE users SET name = :name WHERE id = :id")
    suspend fun updateUserName(id: String, name: String)

    @Query("UPDATE users SET countryCode = :countryCode WHERE id = :id")
    suspend fun updateCountryCode(id: String, countryCode: String)

    @Query("UPDATE users SET profilePictureUrl = :url, lastUpdated = :timestamp WHERE id = :id")
    suspend fun updateProfilePicture(id: String, url: String, timestamp: Long)
}