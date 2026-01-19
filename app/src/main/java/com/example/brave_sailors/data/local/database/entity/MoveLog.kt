package com.example.brave_sailors.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "move_logs",
    foreignKeys = [
        ForeignKey(
            entity = MatchResult::class,
            parentColumns = ["matchId"],
            childColumns = ["matchId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["matchId"])]
)
data class MoveLog(
    @PrimaryKey(autoGenerate = true) val moveId: Long = 0,
    val matchId: Long,
    val turnNumber: Int,
    val playerId: String,
    val row: Int,
    val col: Int,
    val result: String
)