package com.example.brave_sailors.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey val id: String, // Kith's UID
    val name: String,
    val status: String, // es. "PENDING" | "ACCEPTED"
    val timestamp: Long,
    val wins: Int = 0,
    val losses: Int = 0,
    val countryCode: String? = null
)