package com.example.kpopdancepracticeai.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * 3단계: Retrofit 클라이언트 싱글톤
 */
object RetrofitClient {
    // 사용자 데이터 수동 동기화에서 사용하는 AWS API Gateway 기본 주소
    private const val BASE_URL = "https://aujfpfdg6e.execute-api.ap-northeast-1.amazonaws.com/default/"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // JSON 파싱을 위해 Gson 사용
            .build()
            .create(ApiService::class.java)
    }
}
