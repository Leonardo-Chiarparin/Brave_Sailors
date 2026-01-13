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

    // Progression (Visible in StatisticsScreen - Bar and "Level")

    val level: Int = 1,
    val currentXp: Int = 0,
    val lastWinTimestamp: Long = 0L,

    // Ranking (Visible in RankingsScreen - "Score")
    val totalScore: Long = 0,

    // Match Statistics (Visible in StatisticsScreen)
    val wins: Int = 0,
    val losses: Int = 0,

    // Combat Statistics (Visible in StatisticsScreen)
    val shipsDestroyed: Int = 0,      // "Boats sunken"
    val totalShotsFired: Long = 0,    // "Shots"
    val totalShotsHit: Long = 0       // Required to calculate "Accuracy"
) {
    // DERIVED FIELDS (UI Helpers, not saved in DB)
    val totalGamesPlayed: Int
        get() = wins + losses

    val winRate: Float
        get() = if (totalGamesPlayed > 0) (wins.toFloat() / totalGamesPlayed) * 100f else 0f

    val accuracy: Float
        get() = if (totalShotsFired > 0) (totalShotsHit.toFloat() / totalShotsFired) * 100f else 0f
}