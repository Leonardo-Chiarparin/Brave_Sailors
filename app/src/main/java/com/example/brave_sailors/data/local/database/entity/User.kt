package com.example.brave_sailors.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String = "",
    val email: String = "",
    val sessionToken: String? = null,
    val name: String = "",
    val profilePictureUrl: String? = null,

    val googleName: String = "",
    val googlePhotoUrl: String? = null,

    val registerEmail: String? = null,
    val password: String? = null,

    val aiAvatarPath: String? = null,

    val countryCode: String? = null,
    val lastUpdated: Long = System.currentTimeMillis(),

    val level: Int = 1,
    val currentXp: Int = 0,
    val lastWinTimestamp: Long = 0L,

    val totalScore: Long = 0,

    val wins: Int = 0,
    val losses: Int = 0,

    val shipsDestroyed: Int = 0,
    val totalShotsFired: Long = 0,
    val totalShotsHit: Long = 0
) {
    val totalGamesPlayed: Int
        get() = wins + losses

    val winRate: Float
        get() = if (totalGamesPlayed > 0) (wins.toFloat() / totalGamesPlayed) * 100f else 0f

    val accuracy: Float
        get() = if (totalShotsFired > 0) (totalShotsHit.toFloat() / totalShotsFired) * 100f else 0f
}