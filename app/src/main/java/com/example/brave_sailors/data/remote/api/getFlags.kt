package com.example.brave_sailors.data.remote.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
data class Flag(
    val name: String,
    val code: String,
    val flagUrl: String
)

interface FlagsApi {
    @GET("api/flags")
    suspend fun getFlags(): List<Flag>
}

object RetrofitInstance {
    private const val BASE_URL = "https://Cantile.pythonanywhere.com/"

    val api: FlagsApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FlagsApi::class.java)
    }
}