package com.example.kpopdancepracticeai.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class PresignedUrlResponse(
    val key: String,
    val url: String,
    val expiresIn: Int
)

interface DownloadApiService {

    @GET("preSignedUrlDownloadExpertVideos")
    suspend fun getPresignedUrl(
        @Query("key") key: String
    ): PresignedUrlResponse
}

object ApiClient {
    private const val BASE_URL =
        "https://aujfpfdg6e.execute-api.ap-northeast-1.amazonaws.com/default/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val downloadService: DownloadApiService by lazy {
        retrofit.create(DownloadApiService::class.java)
    }
}