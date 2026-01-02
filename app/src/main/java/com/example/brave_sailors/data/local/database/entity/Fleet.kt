package com.example.brave_sailors.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_fleet",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE // If you delete the user, you delete also its ships
        )
    ],
    indices = [Index(value = ["userId"])]
)

data class SavedShip(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val shipId: Int, // ID that identifies the ship in the database (es. 0, 1, 2...)
    val size: Int,
    val row: Int,
    val col: Int,
    val isHorizontal: Boolean
)