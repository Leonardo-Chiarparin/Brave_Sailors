package com.example.brave_sailors.data.remote.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// --- DATA MODELS ---
data class GridRequest(
    val grid: List<List<Int>>,
    val difficulty: String
)

data class AiMoveResponse(
    val row: Int,
    val col: Int
)

// --- INTERFACE ---
interface AiService {
    @POST("/predict")
    suspend fun getNextMove(@Body request: GridRequest): AiMoveResponse
}

// --- CLIENT ---
object AiNetworkClient {

    private const val BASE_URL = "https://battleship-brain-101333280904.europe-west1.run.app/"

    val service: AiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AiService::class.java)
    }
}