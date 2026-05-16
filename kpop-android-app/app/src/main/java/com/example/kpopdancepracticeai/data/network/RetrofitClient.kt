package com.example.kpopdancepracticeai.data.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * 3단계: Retrofit 클라이언트 싱글톤
 */
object RetrofitClient {
    private const val TAG = "AwsSyncNetwork"

    // 실제 AWS 배포 전까지는 임시 URL 사용 (나중에 실제 API Gateway 주소로 변경 필요)
    private const val BASE_URL = "https://aujfpfdg6e.execute-api.ap-northeast-1.amazonaws.com/"

    private val loggingInterceptor = Interceptor { chain ->
        val request = chain.request()
        val hasAuthorization = !request.header("Authorization").isNullOrBlank()
        val hasApiKey = !request.header("x-api-key").isNullOrBlank()

        Log.d(
            TAG,
            "[REQ] ${request.method} ${request.url} | Authorization=${if (hasAuthorization) "present" else "missing"}, x-api-key=${if (hasApiKey) "present" else "missing"}"
        )

        val response = chain.proceed(request)
        Log.d(TAG, "[RES] ${request.method} ${request.url} -> HTTP ${response.code}")
        response
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create()) // JSON 파싱을 위해 Gson 사용
            .build()
            .create(ApiService::class.java)
    }
}
