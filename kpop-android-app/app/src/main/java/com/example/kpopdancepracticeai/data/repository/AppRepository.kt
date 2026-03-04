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
    // 앱이 메모리에 올라와서 실행되는 순간을 기준 시간으로 바로 잡습니다.
    private var sessionStartTime: Long = System.currentTimeMillis()

    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    // --- Time Tracking ---
    fun onAppForeground() {
        sessionStartTime = System.currentTimeMillis()
    }

    suspend fun onAppBackground(userId: String) {
        syncAppUsageTime(userId)
    }

    suspend fun syncAppUsageTime(userId: String) {
        val currentTime = System.currentTimeMillis()
        val durationMs = currentTime - sessionStartTime

        // 1초라도 지났으면 과거의 총 시간(totalPlayTime)에 누적해서 더합니다.
        if (durationMs >= 1000) {
            val addedSeconds = durationMs / 1000
            val currentStats = userDao.getUserStatsOneShot(userId)

            if (currentStats != null) {
                val updatedStats = currentStats.copy(
                    totalPlayTime = currentStats.totalPlayTime + addedSeconds,
                    lastUpdated = getCurrentTime()
                )
                userDao.insertOrUpdate(updatedStats)
            }
            // 누적했으므로 시작 시간을 '현재'로 리셋하여 중복 계산을 방지합니다.
            sessionStartTime = currentTime
        }
    }

    // --- User Statistics & Profile ---
    fun getUserStats(userId: String): Flow<UserStats?> = userDao.getUserStats(userId)
    fun getUserProfile(userId: String): Flow<User?> = userDao.getUserProfile(userId)
    suspend fun getUserProfileOneShot(userId: String): User? = userDao.getUserProfileOneShot(userId)

    suspend fun updateUserProfile(user: User) {
        userDao.updateUser(user)
    }

    suspend fun fetchInitialData(userId: String) {
        // 1. 통계 데이터 초기화
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

        // 2. 사용자 프로필 초기화
        val existingUser = userDao.getUserProfileOneShot(userId)
        if (existingUser == null) {
            val newUser = User(
                userUuid = userId,
                loginId = "user_$userId",
                email = "user@example.com",
                passwordHash = "",
                name = "New Dancer",
                birthDate = "2000-01-01",
                gender = "Unknown",
                joinDate = getCurrentTime()
            )
            userDao.insertUser(newUser)
        }

        // 3. 초기 업적 데이터 세팅
        val initialAchievements = listOf(
            UserAchievementProgress(userUuid = userId, achievementCode = "PERFECTIONIST", currentStep = 0, goalStep = 5, isCompleted = false, achievedDate = null),
            UserAchievementProgress(userUuid = userId, achievementCode = "PRACTICE_BUG", currentStep = 0, goalStep = 100, isCompleted = false, achievedDate = null),
            UserAchievementProgress(userUuid = userId, achievementCode = "BTS_MASTER", currentStep = 0, goalStep = 10, isCompleted = false, achievedDate = null),
            UserAchievementProgress(userUuid = userId, achievementCode = "CHALLENGE_HUNTER", currentStep = 0, goalStep = 10, isCompleted = false, achievedDate = null),
            UserAchievementProgress(userUuid = userId, achievementCode = "NEW_DANCER", currentStep = 1, goalStep = 1, isCompleted = true, achievedDate = getCurrentTime())
        )
        achievementDao.insertProgress(initialAchievements)

        // 4. 초기 배지 세팅
        if (existingStats == null) {
            achievementDao.insertBadge(
                Badge(
                    id = "badge_new_dancer_$userId",
                    userUuid = userId,
                    name = "신입 댄서",
                    description = "첫 연습 영상 업로드",
                    iconResName = "ic_badge_default",
                    category = "초보자",
                    isUnlocked = true,
                    obtainedAt = System.currentTimeMillis()
                )
            )
        }
    }

    //  회원가입 정보 등록
    suspend fun registerUser(userId: String, email: String, passwordHash: String, name: String, birthDate: String) {
        // 기존 통계 초기화 등 기본 정보 세팅 (없을 경우 생성)
        fetchInitialData(userId)

        // 초기화된 프로필 정보를 가입 시 입력받은 정보로 덮어쓰기
        val existingUser = userDao.getUserProfileOneShot(userId)
        if (existingUser != null) {
            val updatedUser = existingUser.copy(
                email = email,
                passwordHash = passwordHash,
                name = name,
                birthDate = birthDate,
                loginId = if(email.isNotBlank()) email else existingUser.loginId
            )
            userDao.updateUser(updatedUser)
        }
    }

    // --- Achievements & Badges ---
    fun getUserAchievements(userId: String): Flow<List<UserAchievementProgress>> = achievementDao.getUserAchievementProgress(userId)
    fun getUserBadges(userId: String): Flow<List<Badge>> = achievementDao.getUserBadges(userId)

    // --- Song ---
    val allSongs: Flow<List<Song>> = songDao.getAllSongs()
    fun getSongParts(songId: Long): Flow<List<SongPart>> = songDao.getPartsBySongId(songId)
    fun searchSongs(query: String): Flow<List<Song>> = songDao.searchSongs(query)

    // --- History & Stats Update ---
    suspend fun savePracticeResult(result: PracticeHistory) {
        historyDao.insertHistory(result)

        val currentStats = userDao.getUserStatsOneShot(result.userUuid)
        if (currentStats != null) {
            val oldAvg = currentStats.avgAccuracy
            val count = currentStats.completedParts
            val newScore = result.totalScore.toDouble()

            // 정확도 갱신
            val newAvgAccuracy = if (count == 0) newScore else ((oldAvg * count) + newScore) / (count + 1)
            val newCompletedParts = count + 1

            val gainedExp = (result.totalScore / 10) + 50
            val newTotalScore = currentStats.achievementScore + gainedExp

            val updatedStats = currentStats.copy(
                completedParts = newCompletedParts,
                avgAccuracy = newAvgAccuracy,
                achievementScore = newTotalScore,
                lastUpdated = getCurrentTime()
            )
            userDao.insertOrUpdate(updatedStats)
        }
    }

    fun getRecentHistory(userId: String): Flow<List<PracticeHistory>> = historyDao.getRecentHistory(userId)
    fun getAllHistory(userId: String): Flow<List<PracticeHistory>> = historyDao.getAllHistory(userId)
}
