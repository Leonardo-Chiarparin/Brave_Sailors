package com.example.brave_sailors.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "match_history")
data class MatchResult(
    @PrimaryKey(autoGenerate = true) val matchId: Long = 0,
    val player1Id: String,
    val opponentName: String,
    val isVictory: Boolean,
    val difficulty: String,
    val timestamp: Long = System.currentTimeMillis(),

    val totalMoves: Int,
    val totalHits: Int,
    val totalMisses: Int,
    val accuracy: Float
)