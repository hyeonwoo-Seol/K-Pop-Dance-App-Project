package com.example.kpopdancepracticeai.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * 3단계: Retrofit 클라이언트 싱글톤
 */
object RetrofitClient {
    // 실제 AWS 배포 전까지는 임시 URL 사용 (나중에 실제 API Gateway 주소로 변경 필요)
    private const val BASE_URL = "https://api.example.com/"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // JSON 파싱을 위해 Gson 사용
            .build()
            .create(ApiService::class.java)
    }
}