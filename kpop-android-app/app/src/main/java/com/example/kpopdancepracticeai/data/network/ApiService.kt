package com.example.kpopdancepracticeai.data.network

import com.example.kpopdancepracticeai.data.entity.Achievement
import com.example.kpopdancepracticeai.data.entity.UserStats
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * 3단계: 서버 통신 인터페이스
 * AWS API Gateway와 통신하여 유저 통계 및 업적 정보를 받아옵니다.
 */
interface ApiService {
    // 1. 사용자 통계 정보 가져오기 (예: 연습 횟수, 총 시간, 레벨 등)
    @GET("user-stats/{userId}")
    suspend fun getUserStats(@Path("userId") userId: String): Response<UserStats>

    // 2. 사용자 업적 리스트 가져오기
    @GET("achievements/{userId}")
    suspend fun getUserAchievements(@Path("userId") userId: String): Response<List<Achievement>>
}