package com.example.kpopdancepracticeai.data.repository

import com.example.kpopdancepracticeai.data.dao.AchievementDao
import com.example.kpopdancepracticeai.data.dao.HistoryDao
import com.example.kpopdancepracticeai.data.dao.SongDao
import com.example.kpopdancepracticeai.data.dao.UserDao
import com.example.kpopdancepracticeai.data.entity.*
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppRepository(
    private val userDao: UserDao,
    private val songDao: SongDao,
    private val historyDao: HistoryDao,
    private val achievementDao: AchievementDao
) {
    // 앱 세션 시작 시간 (앱이 포그라운드로 올 때 설정)
    private var sessionStartTime: Long = 0L

    // 헬퍼: 현재 시간 문자열
    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    // --- Lifecycle Aware Time Tracking (앱 사용 시간 추적) ---

    // 앱이 활성화될 때 (ON_START) 호출
    fun onAppForeground() {
        sessionStartTime = System.currentTimeMillis()
    }

    // 앱이 비활성화될 때 (ON_STOP) 호출 -> 사용 시간 저장
    suspend fun onAppBackground(userId: String) {
        // 세션 시작 시간이 0이면(초기화 안됨) 저장하지 않고 리턴
        if (sessionStartTime == 0L) return

        val currentTime = System.currentTimeMillis()
        val durationMs = currentTime - sessionStartTime

        // 최소 1초 이상 사용했을 때만 저장 (짧은 전환 무시)
        if (durationMs > 1000) {
            val addedSeconds = durationMs / 1000

            // DB에서 현재 통계 가져와서 시간 업데이트
            val currentStats = userDao.getUserStatsOneShot(userId)
            if (currentStats != null) {
                val updatedStats = currentStats.copy(
                    totalPlayTime = currentStats.totalPlayTime + addedSeconds,
                    lastUpdated = getCurrentTime()
                )
                userDao.insertOrUpdate(updatedStats)
            }
        }

        // 백그라운드로 갔으므로 세션 시간 초기화
        sessionStartTime = 0L
    }

    // [핵심 수정] 화면 진입 시 현재까지의 시간을 강제 저장하는 함수
    // ProfileViewModel 등에서 호출하여 실시간 갱신 효과를 줌
    suspend fun syncAppUsageTime(userId: String) {
        val currentTime = System.currentTimeMillis()

        // 만약 sessionStartTime이 0이라면(앱 실행 후 첫 진입 등), 현재 시간으로 초기화만 하고 빠져나감
        // 이렇게 해야 다음 번 호출 때 차이를 계산할 수 있음.
        if (sessionStartTime == 0L) {
            sessionStartTime = currentTime
            return
        }

        val durationMs = currentTime - sessionStartTime

        if (durationMs > 1000) { // 1초 이상 지났을 때만 반영
            val addedSeconds = durationMs / 1000
            val currentStats = userDao.getUserStatsOneShot(userId)
            if (currentStats != null) {
                val updatedStats = currentStats.copy(
                    totalPlayTime = currentStats.totalPlayTime + addedSeconds,
                    lastUpdated = getCurrentTime()
                )
                userDao.insertOrUpdate(updatedStats)
            }
            // [중요] 저장했으므로, 시작 시간을 '방금 저장한 시간(현재)'으로 갱신하여 연속 측정 유지
            sessionStartTime = currentTime
        }
    }

    // --- User Statistics ---
    fun getUserStats(userId: String): Flow<UserStats?> = userDao.getUserStats(userId)

    suspend fun fetchInitialData(userId: String) {
        val existingStats = userDao.getUserStatsOneShot(userId)
        if (existingStats == null) {
            val newStats = UserStats(
                userUuid = userId,
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
        // 데이터 초기화 시점에 세션이 시작된 것으로 간주할 수도 있음
        if (sessionStartTime == 0L) {
            sessionStartTime = System.currentTimeMillis()
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

        // 2. 유저 통계 업데이트 (평균 정확도, 완료 횟수 등)
        val currentStats = userDao.getUserStatsOneShot(result.userUuid)

        if (currentStats != null) {
            val oldAvg = currentStats.avgAccuracy
            val count = currentStats.completedParts
            val newScore = result.totalScore.toDouble()

            // 평균 계산: (기존평균 * 횟수 + 새점수) / (횟수 + 1)
            // count가 0일 때의 예외 처리 포함
            val newAvgAccuracy = if (count == 0) newScore else ((oldAvg * count) + newScore) / (count + 1)
            val newCompletedParts = count + 1

            val updatedStats = currentStats.copy(
                completedParts = newCompletedParts,
                avgAccuracy = newAvgAccuracy,
                lastUpdated = getCurrentTime()
            )

            userDao.insertOrUpdate(updatedStats)
        }
    }

    fun getRecentHistory(userId: String): Flow<List<PracticeHistory>> = historyDao.getRecentHistory(userId)
    fun getAllHistory(userId: String): Flow<List<PracticeHistory>> = historyDao.getAllHistory(userId)
}