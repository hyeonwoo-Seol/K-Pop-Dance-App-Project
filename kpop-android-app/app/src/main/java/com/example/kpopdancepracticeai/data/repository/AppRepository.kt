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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 앱 저장소 (Repository)
 * 역할: 데이터의 단일 진실 공급원 (Local DB + Remote Server)
 */
class AppRepository(
    private val userDao: UserDao,
    private val historyDao: HistoryDao,
    private val achievementDao: AchievementDao
) {


    // --- 1. 사용자 통계 관련 ---

    fun getUserStatsStream(userId: String): Flow<UserStats?> {
        return userDao.getUserStats(userId)
    }

    // --- 2. 연습 기록 관련 ---

    suspend fun savePracticeResult(history: PracticeHistory) {
        // A. 로컬 저장
        val newId = historyDao.insertHistory(history)
        Log.d("AppRepository", "로컬 DB 저장 완료. ID: $newId")

        // B. 통계 업데이트 (완료한 곡 횟수, 총 시간 등)
        updateUserStatsLocally(history)

        // C. AWS 동기화 (비동기 호출 - 보내는 로직)
        syncUnsyncedData()
    }

    /**
     * 유저 통계 수동 업데이트
     */
    private suspend fun updateUserStatsLocally(history: PracticeHistory) {
        val currentStats = userDao.getUserStatsOneShot(history.userId) ?: UserStats(userId = history.userId)

        val updatedStats = currentStats.copy(
            completedSongCount = currentStats.completedSongCount + 1,
            // 3분(180초) 연습했다고 가정 (실제로는 history.playTimeSeconds 사용 권장)
            totalPracticeTimeSeconds = currentStats.totalPracticeTimeSeconds + 180,
            lastPracticeDate = System.currentTimeMillis()
        )
        userDao.insertOrUpdate(updatedStats)
    }

    // --- 3. 동기화 로직 (보내기: Push) ---

    private fun syncUnsyncedData() {
        CoroutineScope(Dispatchers.IO).launch {
            val unsyncedList = historyDao.getUnsyncedData()
            if (unsyncedList.isNotEmpty()) {
                Log.d("AppRepository", "동기화 필요한 데이터 ${unsyncedList.size}건 발견.")
                // 실제 전송 로직...
                unsyncedList.forEach { history ->
                    try {
                        delay(1000) // 시뮬레이션
                        historyDao.markAsSynced(history.id)
                    } catch (e: Exception) {
                        Log.e("AppRepository", "동기화 실패: ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * [3단계 구현 완료] 서버 동기화 (받기: Pull)
     * 설정 화면에서 '최신 데이터 동기화' 버튼 클릭 시 호출
     */
    suspend fun fetchInitialData(userId: String) = withContext(Dispatchers.IO) {
        try {
            Log.d("AppRepository", "서버로부터 최신 데이터 요청 중... User: $userId")

            // --- [시뮬레이션 모드] ---
            // 실제 서버가 준비되면 RetrofitClient.apiService.getUserStats(userId) 호출로 대체
            delay(1500) // 네트워크 지연 1.5초 시뮬레이션

            // 서버에서 받아왔다고 가정하는 최신 데이터 (Mock)
            // 테스트를 위해 'completedSongCount'를 100으로 설정하여 UI 변화 확인
            val mockServerStats = UserStats(
                userId = userId,
                completedSongCount = 100, // 버튼 누르면 이 값으로 업데이트됨!
                totalPracticeTimeSeconds = 45000,
                currentLevel = 5,
                completedPartCount = 42,
                averageAccuracy = 92.5f,
                lastPracticeDate = System.currentTimeMillis()
            )

            // 로컬 DB 업데이트 -> Flow를 통해 UI 자동 갱신
            userDao.insertOrUpdate(mockServerStats)

            Log.d("AppRepository", "데이터 동기화 완료!")

        } catch (e: Exception) {
            Log.e("AppRepository", "데이터 가져오기 실패", e)
            throw e
        }
    }

    // --- 4. 업적 관련 ---
    fun getAllAchievements(): Flow<List<Achievement>> {
        return achievementDao.getAllAchievements()
    }
}