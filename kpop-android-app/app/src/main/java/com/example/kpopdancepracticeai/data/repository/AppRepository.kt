package com.example.kpopdancepracticeai.data.repository

import android.util.Log
import com.example.kpopdancepracticeai.data.dao.AchievementDao
import com.example.kpopdancepracticeai.data.dao.HistoryDao
import com.example.kpopdancepracticeai.data.dao.UserDao
import com.example.kpopdancepracticeai.data.entity.Achievement
import com.example.kpopdancepracticeai.data.entity.PracticeHistory
import com.example.kpopdancepracticeai.data.entity.UserStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * 앱 저장소 (Repository)
 * 역할: 데이터의 단일 진실 공급원 (Single Source of Truth)
 * UI는 이 클래스를 통해서만 데이터에 접근하며, 데이터가 로컬에서 오는지 서버에서 오는지 알 필요가 없습니다.
 */
class AppRepository(
    private val userDao: UserDao,
    private val historyDao: HistoryDao,
    private val achievementDao: AchievementDao
) {

    // --- 1. 사용자 통계 관련 ---

    // UI에 실시간 데이터 스트림 제공 (Room의 Flow 기능 활용)
    fun getUserStatsStream(userId: String): Flow<UserStats?> {
        return userDao.getUserStats(userId)
    }

    // --- 2. 연습 기록 관련 ---

    /**
     * 연습 결과를 저장하고, AWS 동기화 트리거
     * 1. 로컬 DB에 우선 저장 (isSynced = false) -> 화면 즉시 갱신
     * 2. 백그라운드에서 AWS 업로드 시도 (구현 예정)
     */
    suspend fun savePracticeResult(history: PracticeHistory) {
        // A. 로컬 저장
        val newId = historyDao.insertHistory(history)
        Log.d("AppRepository", "로컬 DB 저장 완료. ID: $newId")

        // B. 통계 업데이트 (연습 횟수, 총 시간 등)
        userDao.incrementSongCount(history.userId)
        // 예: 3분(180초) 연습했다고 가정
        userDao.updateTotalTime(history.userId, 180)

        // C. AWS 동기화 (비동기 호출)
        syncUnsyncedData()
    }

    // --- 3. 동기화 로직 (Offline-First 핵심) ---

    /**
     * 미전송 데이터(isSynced=false)를 찾아 서버로 전송
     */
    private fun syncUnsyncedData() {
        CoroutineScope(Dispatchers.IO).launch {
            val unsyncedList = historyDao.getUnsyncedData()
            if (unsyncedList.isNotEmpty()) {
                Log.d("AppRepository", "동기화 필요한 데이터 ${unsyncedList.size}건 발견. AWS 전송 시작...")

                unsyncedList.forEach { history ->
                    try {
                        // TODO: 실제 AWS API 호출 (Retrofit 또는 AWS SDK 사용)
                        // val response = apiService.uploadHistory(history)
                        // if (response.isSuccessful) {
                        //     historyDao.markAsSynced(history.id)
                        // }

                        // [시뮬레이션] 1초 뒤 성공 처리
                        kotlinx.coroutines.delay(1000)
                        historyDao.markAsSynced(history.id)
                        Log.d("AppRepository", "데이터 ID ${history.id} 동기화 완료")

                    } catch (e: Exception) {
                        Log.e("AppRepository", "동기화 실패: ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * 앱 실행 시 호출: 서버의 최신 데이터를 가져와 로컬 DB 업데이트 (Pull)
     */
    suspend fun fetchInitialData(userId: String) {
        // TODO: AWS에서 최신 JSON 받아오기
        // val serverStats = apiService.getUserStats(userId)
        // userDao.insertOrUpdate(serverStats)
    }

    // --- 4. 업적 관련 ---
    fun getAllAchievements(): Flow<List<Achievement>> {
        return achievementDao.getAllAchievements()
    }
}