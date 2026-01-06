package com.example.brave_sailors.data.local.database.entity

data class RankingPlayer(
    val rank: Int,
    val id: String,
    val name: String,
    val score: String,
    val countryCode: String,
    val avatarUrl: String?
)