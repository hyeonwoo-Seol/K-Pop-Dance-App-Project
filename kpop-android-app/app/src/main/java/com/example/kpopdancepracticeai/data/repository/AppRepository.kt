package com.example.kpopdancepracticeai.data.repository

import com.example.kpopdancepracticeai.data.dao.AchievementDao
import com.example.kpopdancepracticeai.data.dao.HistoryDao
import com.example.kpopdancepracticeai.data.dao.SongDao
import com.example.kpopdancepracticeai.data.dao.UserDao
import com.example.kpopdancepracticeai.data.RealDataSource
import com.example.kpopdancepracticeai.data.entity.*
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

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

        // 3. 초기 업적/보상 메타데이터 세팅
        achievementDao.insertAchievements(RealDataSource.getRealAchievements)
        achievementDao.insertLightSticks(RealDataSource.getRealLightSticks)

        // 4. 사용자별 업적 진행도 초기화
        val initialAchievements = RealDataSource.getInitialAchievementProgress(userId)
        achievementDao.insertProgress(initialAchievements)

        // 5. 사용자별 배지 초기화
        if (existingStats == null) {
            RealDataSource.getInitialBadges(userId).forEach { badge ->
                achievementDao.insertBadge(badge)
            }
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

    // 💡 [문제 해결 부분!] ViewModel이 호출할 수 있도록 Dao와 연결해주는 다리 역할을 합니다.
    suspend fun insertSongs(songs: List<Song>) {
        songDao.insertSongs(songs)
    }

    suspend fun insertSongParts(parts: List<SongPart>) {
        songDao.insertSongParts(parts)
    }

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

        updateArtistAchievements(result)
    }

    private suspend fun updateArtistAchievements(result: PracticeHistory) {
        val artistKey = normalizeArtistKey(result.artistName) ?: return
        val achievementCodes = listOf("${artistKey}_complete_01", "${artistKey}_complete_50")

        achievementCodes.forEach { code ->
            val progress = achievementDao.getUserAchievementProgressOneShot(result.userUuid, code) ?: return@forEach
            if (progress.isCompleted) return@forEach

            val updatedStep = min(progress.currentStep + 1, progress.goalStep)
            val completed = updatedStep >= progress.goalStep
            achievementDao.updateProgress(
                userId = result.userUuid,
                code = code,
                step = updatedStep,
                completed = completed,
                date = if (completed) getCurrentTime() else null
            )
        }
    }

    private fun normalizeArtistKey(rawArtist: String): String? {
        val normalized = rawArtist.lowercase(Locale.getDefault()).replace(" ", "")
        return when {
            normalized.contains("itzy") || normalized.contains("있지") -> "itzy"
            normalized.contains("ive") || normalized.contains("아이브") -> "ive"
            normalized.contains("nmixx") || normalized.contains("엔믹스") -> "nmixx"
            normalized.contains("fromis") || normalized.contains("프로미스") -> "fromis9"
            normalized.contains("straykids") || normalized.contains("스트레이키즈") -> "straykids"
            else -> null
        }
    }

    fun getRecentHistory(userId: String): Flow<List<PracticeHistory>> = historyDao.getRecentHistory(userId)
    fun getAllHistory(userId: String): Flow<List<PracticeHistory>> = historyDao.getAllHistory(userId)

    // 💡 [추가됨] DB에서 전체 곡 데이터를 한 번만 읽어오는 동기식 메서드
    suspend fun getAllSongsSync(): List<Song> {
        return songDao.getAllSongsSync()
    }

    suspend fun prePopulateSongsIfNeeded() {
        if (songDao.getSongCount() > 0) return
        songDao.insertSongs(RealDataSource.getRealSongs)
        songDao.insertSongParts(RealDataSource.getRealSongParts)
    }
}
