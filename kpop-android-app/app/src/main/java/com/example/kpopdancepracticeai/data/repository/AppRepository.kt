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
    // 앱 실행 시점부터 시간 추적 시작
    private var lastSyncTime: Long = System.currentTimeMillis()

    // 헬퍼: 현재 시간 문자열
    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    // ----------------------------------------------------------------
    // [추가] User 정보 관리 (로그인/회원가입 연동)
    // ----------------------------------------------------------------

    // 로그인 시 DB에 유저 정보가 있는지 확인
    suspend fun getUserByUuid(userUuid: String): User? {
        return userDao.getUser(userUuid)
    }

    // 회원가입 완료 또는 프로필 수정 시 유저 정보 저장
    suspend fun saveUser(user: User) {
        userDao.insertUser(user)
    }

    // ----------------------------------------------------------------
    // 기존 기능 유지
    // ----------------------------------------------------------------

    // --- User Statistics ---
    fun getUserStats(userId: String): Flow<UserStats?> = userDao.getUserStats(userId)

    suspend fun fetchInitialData(userId: String) {
        val existingStats = userDao.getUserStatsOneShot(userId)
        if (existingStats == null) {
            // [수정] MD 파일에 명시된 기본값으로 초기화
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
        // 초기화 시점의 시간을 동기화 기준으로 설정
        lastSyncTime = System.currentTimeMillis()
    }

    // 프로필 화면 등에서 주기적으로 호출하여 접속 시간 갱신
    suspend fun syncAppUsageTime(userId: String) {
        val currentTime = System.currentTimeMillis()
        val timeElapsed = currentTime - lastSyncTime

        // 1초 이상 지났을 때만 업데이트 (DB 부하 방지 및 유효성 검사)
        if (timeElapsed > 1000) {
            val currentStats = userDao.getUserStatsOneShot(userId)
            if (currentStats != null) {
                // [중요 수정] 밀리초(ms) -> 초(s) 단위 변환
                // MD 파일의 total_play_time은 "초 단위"입니다.
                val addedSeconds = timeElapsed / 1000

                val updatedStats = currentStats.copy(
                    totalPlayTime = currentStats.totalPlayTime + addedSeconds,
                    lastUpdated = getCurrentTime()
                )
                userDao.insertOrUpdate(updatedStats)
            }
            // 갱신 성공 여부와 상관없이 기준 시간 업데이트 (중복 누적 방지)
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

        // 2. 유저 통계 업데이트
        val currentStats = userDao.getUserStatsOneShot(result.userUuid) // userId -> userUuid

        if (currentStats != null) {
            val currentTime = System.currentTimeMillis()
            val timeElapsed = currentTime - lastSyncTime

            // [중요 수정] 시간 갱신 (밀리초 -> 초 변환)
            // 연습하는 동안 흐른 시간도 앱 사용 시간에 포함시킵니다.
            val addedSeconds = if (timeElapsed > 0) timeElapsed / 1000 else 0L
            val newTotalPlayTime = currentStats.totalPlayTime + addedSeconds

            // 정확도 갱신
            // [수정] result.score -> result.totalScore
            val oldAvg = currentStats.avgAccuracy
            val count = currentStats.completedParts
            val newScore = result.totalScore.toDouble()

            // 평균 계산: (기존평균 * 횟수 + 새점수) / (횟수 + 1)
            val newAvgAccuracy = ((oldAvg * count) + newScore) / (count + 1)
            val newCompletedParts = count + 1

            val updatedStats = currentStats.copy(
                totalPlayTime = newTotalPlayTime,
                completedParts = newCompletedParts,
                avgAccuracy = newAvgAccuracy,
                lastUpdated = getCurrentTime()
            )

            userDao.insertOrUpdate(updatedStats)

            // 통계 업데이트 시점 기준으로 시간 동기화
            lastSyncTime = currentTime
        }
    }

    // [수정] userId 파라미터명 일치
    fun getRecentHistory(userId: String): Flow<List<PracticeHistory>> = historyDao.getRecentHistory(userId)
}