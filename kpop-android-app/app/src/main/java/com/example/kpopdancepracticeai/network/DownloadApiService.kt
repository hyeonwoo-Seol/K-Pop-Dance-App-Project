package com.example.kpopdancepracticeai.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 서버 API 응답을 매핑할 데이터 클래스입니다.
 * AWS API Gateway에서 {"download_url": "https://..."} 형태로 반환하므로,
 * 변수명을 JSON 키와 동일하게 맞추거나 @SerializedName 어노테이션을 사용해야 합니다.
 */
data class PresignedUrlResponse(
    val download_url: String
)

/**
 * API 호출 명세서 역할을 하는 인터페이스입니다.
 */
interface DownloadApiService {

    // TODO: AWS 관리자가 알려줄 정확한 세부 엔드포인트(예: "download" 또는 "api/v1/get-url")로 수정해야 합니다.
    @GET("download")
    suspend fun getPresignedUrl(
        @Query("classId") classId: Int,
        @Query("filename") filename: String
    ): PresignedUrlResponse
}

/**
 * 앱 전체에서 재사용할 Retrofit 클라이언트 싱글톤 객체입니다.
 */
object ApiClient {
    // TODO: AWS 관리자가 줄 API Gateway의 기본 주소(Base URL)를 입력합니다.
    // 주의: Base URL은 반드시 마지막에 슬래시('/')로 끝나야 합니다.
    private const val BASE_URL = "https://aujfpfdg6e.execute-api.ap-northeast-1.amazonaws.com/default/preSignedUrlDownloadExpertVideos/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        // JSON 응답을 코틀린 데이터 클래스로 변환하기 위해 Gson Converter를 사용합니다.
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val downloadService: DownloadApiService by lazy {
        retrofit.create(DownloadApiService::class.java)
    }
}