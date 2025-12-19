package com.example.brave_sailors.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val profilePictureUrl: String?,
    val countryCode: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)