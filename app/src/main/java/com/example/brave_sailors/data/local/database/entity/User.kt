package com.example.brave_sailors.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val email: String,
    val sessionToken: String? = null,
    val name: String,
    val profilePictureUrl: String?,

    val googleName: String,
    val googlePhotoUrl: String?,

    val countryCode: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)