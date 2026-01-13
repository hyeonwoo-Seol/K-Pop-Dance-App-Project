package com.example.kpopdancepracticeai.data.repository

import com.example.kpopdancepracticeai.data.dao.AchievementDao
import com.example.kpopdancepracticeai.data.dao.HistoryDao
import com.example.kpopdancepracticeai.data.dao.SongDao
import com.example.kpopdancepracticeai.data.dao.UserDao
import com.example.kpopdancepracticeai.data.entity.*
import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val userDao: UserDao,
    private val songDao: SongDao,
    private val historyDao: HistoryDao,
    private val achievementDao: AchievementDao
) {
    private var lastSyncTime: Long = System.currentTimeMillis()

    // --- User Statistics ---
    fun getUserStats(userId: String): Flow<UserStats?> = userDao.getUserStats(userId)

    suspend fun fetchInitialData(userId: String) {
        val existingStats = userDao.getUserStatsOneShot(userId)
        if (existingStats == null) {
            // [수정] 새로운 UserStats 구조로 초기화
            val newStats = UserStats(
                userId = userId,
                totalPlayTime = 0L,
                completedParts = 0,
                avgAccuracy = 0.0,
                badgeCount = 0,
                lightstickCount = 0,
                achievementScore = 0,
                lastUpdated = getCurrentTime()
            )
            userDao.insertOrUpdate(newStats)
        }
        lastSyncTime = System.currentTimeMillis()
    }

    suspend fun syncAppUsageTime(userId: String) {
        val currentTime = System.currentTimeMillis()
        val timeElapsed = currentTime - lastSyncTime // 밀리초 단위

        if (timeElapsed > 0) {
            // 초 단위로 변환하여 저장 (DB 스키마가 초 단위라고 명시되어 있음)
            // 하지만 정밀도를 위해 밀리초를 누적하거나, 로직에 따라 초로 변환
            // 여기서는 기존 데이터에 밀리초를 더하는 방식으로 가정하되, 필요시 /1000 처리
            val timeElapsedSec = timeElapsed / 1000 // 초 단위 변환 필요한 경우 사용

            // 안전하게 객체 로드 후 업데이트
            val currentStats = userDao.getUserStatsOneShot(userId)
            if (currentStats != null) {
                val updatedStats = currentStats.copy(
                    totalPlayTime = currentStats.totalPlayTime + timeElapsed, // 여기선 밀리초 누적 유지
                    lastUpdated = getCurrentTime()
                )
                userDao.insertOrUpdate(updatedStats)
            }
            lastSyncTime = currentTime
        }
    }

    // --- Song ---
    val allSongs: Flow<List<Song>> = songDao.getAllSongs()
    fun getSongParts(songId: Long): Flow<List<SongPart>> = songDao.getPartsBySongId(songId)
    fun searchSongs(query: String): Flow<List<Song>> = songDao.searchSongs(query)

    // --- History & Stats Update ---
    suspend fun savePracticeResult(result: PracticeHistory) {
        // 1. 히스토리 저장
        historyDao.insertHistory(result)

        // 2. 유저 통계 업데이트 (UserStatistics 명세 반영)
        val currentStats = userDao.getUserStatsOneShot(result.userId)

        if (currentStats != null) {
            val currentTime = System.currentTimeMillis()
            val timeElapsed = currentTime - lastSyncTime

            // 시간 갱신
            val newTotalPlayTime = currentStats.totalPlayTime + (if (timeElapsed > 0) timeElapsed else 0L)

            // 정확도 갱신 공식: (기존평균 * 횟수 + 새점수) / (횟수 + 1)
            val oldAvg = currentStats.avgAccuracy
            val count = currentStats.completedParts
            val newScore = result.score.toDouble()

            // 0으로 나누기 방지 및 공식 적용
            val newAvgAccuracy = ((oldAvg * count) + newScore) / (count + 1)
            val newCompletedParts = count + 1

            val updatedStats = currentStats.copy(
                totalPlayTime = newTotalPlayTime,
                completedParts = newCompletedParts,
                avgAccuracy = newAvgAccuracy,
                lastUpdated = getCurrentTime()
            )

            userDao.insertOrUpdate(updatedStats)
            lastSyncTime = currentTime
        }
    }

    fun getRecentHistory(userId: String): Flow<List<PracticeHistory>> = historyDao.getRecentHistory(userId)
}