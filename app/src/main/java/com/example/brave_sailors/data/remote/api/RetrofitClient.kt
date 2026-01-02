package com.example.brave_sailors.data.remote.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface SailorApi {
    @GET("api/flags")
    suspend fun getFlags(): List<Flag>
}

object RetrofitClient {
    private const val BASE_URL = "https://cantile.pythonanywhere.com/"

    val api: SailorApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SailorApi::class.java)
    }
}