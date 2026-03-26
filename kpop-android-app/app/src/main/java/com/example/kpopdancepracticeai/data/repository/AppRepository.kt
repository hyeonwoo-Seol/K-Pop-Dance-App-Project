package com.example.kpopdancepracticeai.data.repository

import com.example.kpopdancepracticeai.data.dao.AchievementDao
import com.example.kpopdancepracticeai.data.dao.HistoryDao
import com.example.kpopdancepracticeai.data.dao.SongDao
import com.example.kpopdancepracticeai.data.dao.UserChoreoStatsDao
import com.example.kpopdancepracticeai.data.dao.UserDao
import com.example.kpopdancepracticeai.data.RealDataSource
import com.example.kpopdancepracticeai.data.entity.*
import com.example.kpopdancepracticeai.data.network.RemoteAchievementDto
import com.example.kpopdancepracticeai.data.network.RemoteUserDto
import com.example.kpopdancepracticeai.data.network.RemoteUserStatisticsDto
import com.example.kpopdancepracticeai.data.network.RetrofitClient
import com.google.gson.JsonElement
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random
import kotlin.math.min

class AppRepository(
    private val userDao: UserDao,
    private val songDao: SongDao,
    private val historyDao: HistoryDao,
    private val achievementDao: AchievementDao,
    private val userChoreoStatsDao: UserChoreoStatsDao
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

    suspend fun syncUserDataFromRemote(userId: String) {
        val remoteUserResponse = RetrofitClient.apiService.getRemoteUser(userId)
        val remoteStatsResponse = RetrofitClient.apiService.getRemoteUserStatistics(userId)
        val remoteAchievementsResponse = RetrofitClient.apiService.getRemoteAchievements(userId)

        if (!remoteUserResponse.isSuccessful) {
            error("Users 동기화 실패 (${remoteUserResponse.code()})")
        }
        if (!remoteStatsResponse.isSuccessful) {
            error("UserStatistics 동기화 실패 (${remoteStatsResponse.code()})")
        }
        if (!remoteAchievementsResponse.isSuccessful) {
            error("Achievements 동기화 실패 (${remoteAchievementsResponse.code()})")
        }

        val remoteUser = remoteUserResponse.body()
            ?: error("Users 응답이 비어 있습니다.")
        val remoteStats = remoteStatsResponse.body()
            ?: error("UserStatistics 응답이 비어 있습니다.")
        val remoteAchievements = remoteAchievementsResponse.body().orEmpty()

        val existingUser = userDao.getUserProfileOneShot(userId)
        val existingStats = userDao.getUserStatsOneShot(userId)

        userDao.insertUser(remoteUser.toLocalUser(existingUser))
        userDao.insertOrUpdate(remoteStats.toLocalUserStats(existingStats))

        achievementDao.insertAchievements(RealDataSource.getRealAchievements)
        achievementDao.deleteUserAchievementProgress(userId)
        if (remoteAchievements.isNotEmpty()) {
            achievementDao.insertProgress(remoteAchievements.map { it.toLocalProgress(userId) })
        }
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
        // 기존 스키마에서는 progress_id(autoGenerate)가 PK라 동일 사용자/업적코드라도 중복 삽입될 수 있으므로,
        // 최초 1회만 시드 데이터를 넣습니다.
        if (achievementDao.getUserAchievementProgressCount(userId) == 0) {
            val initialAchievements = RealDataSource.getInitialAchievementProgress(userId)
            achievementDao.insertProgress(initialAchievements)
        }

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

    suspend fun generateDeveloperPracticeHistory(userId: String): Int {
        val targetPartIds = listOf(5401L, 5404L, 5422L, 5424L, 5511L, 4522L, 5681L)
        val songParts = songDao.getSongPartsByPartIds(targetPartIds)
        if (songParts.isEmpty()) return 0

        val songsById = songDao.getAllSongsSync().associateBy { it.songId }

        var insertedCount = 0

        songParts.forEach { part ->
            val daysToGenerate = Random.nextInt(5, 9) // 5~8일
            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -(daysToGenerate - 1))
            }

            repeat(daysToGenerate) {
                val dailyPracticeCount = Random.nextInt(1, 4) // 1~3회
                repeat(dailyPracticeCount) { attemptIndex ->
                    val date = calendar.time
                    val timeStamp = String.format(
                        Locale.getDefault(),
                        "%02d:%02d:%02d",
                        9 + Random.nextInt(0, 12),
                        Random.nextInt(0, 60),
                        (attemptIndex * 11 + Random.nextInt(0, 40)) % 60
                    )
                    val createdAt = "${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)} $timeStamp"

                    val partAccuracies = createRandomPartAccuracies()
                    val worstPoints = partAccuracies
                        .toList()
                        .sortedBy { it.second }
                        .take(3)
                        .map { it.first }

                    val history = PracticeHistory(
                        userUuid = userId,
                        songId = part.songId,
                        partNumber = part.partNumber,
                        artistName = extractMemberName(songsById[part.songId]?.artistKr),
                        totalScore = Random.nextInt(62, 99),
                        grade = randomGrade(),
                        partAccuracies = partAccuracies,
                        worstPoints = worstPoints,
                        durationSec = part.durationSec.toDouble(),
                        fps = 30.0,
                        createdAt = createdAt,
                        fullJsonPath = "developer_generated/${part.partId}_${System.currentTimeMillis()}_${Random.nextInt(100, 999)}.json",
                        userVideoPath = "developer_generated/video_${part.partId}_${System.currentTimeMillis()}.mp4",
                        videoWidth = 1080,
                        videoHeight = 1920,
                        totalFrames = (part.durationSec * 30)
                    )

                    savePracticeResult(history)
                    insertedCount++
                }
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        return insertedCount
    }

    private fun createRandomPartAccuracies(): Map<String, Int> {
        val points = listOf(
            "Left Shoulder", "Right Shoulder", "Left Elbow", "Right Elbow",
            "Left Wrist", "Right Wrist", "Left Hip", "Right Hip",
            "Left Knee", "Right Knee", "Left Ankle", "Right Ankle"
        )
        return points.associateWith { Random.nextInt(55, 99) }
    }

    private fun randomGrade(): String {
        val bucket = Random.nextInt(0, 100)
        return when {
            bucket >= 92 -> "S"
            bucket >= 82 -> "A"
            bucket >= 70 -> "B"
            bucket >= 58 -> "C"
            else -> "F"
        }
    }

    private fun extractMemberName(artistKr: String?): String {
        if (artistKr.isNullOrBlank()) return "Unknown"
        val start = artistKr.indexOf('(')
        val end = artistKr.indexOf(')')
        return if (start >= 0 && end > start + 1) {
            artistKr.substring(start + 1, end).trim()
        } else {
            artistKr.trim()
        }
    }

    private fun RemoteUserDto.toLocalUser(existingUser: User?): User {
        return User(
            userUuid = userUuid,
            loginId = loginId ?: existingUser?.loginId ?: "user_$userUuid",
            email = email ?: existingUser?.email ?: "",
            passwordHash = passwordHash ?: existingUser?.passwordHash,
            name = name ?: existingUser?.name ?: "New Dancer",
            birthDate = birthDate ?: existingUser?.birthDate ?: "2000-01-01",
            gender = gender ?: existingUser?.gender ?: "Unknown",
            createdAt = existingUser?.createdAt ?: System.currentTimeMillis(),
            danceSkill = danceSkill ?: existingUser?.danceSkill ?: "BEGINNER",
            favoriteGenres = favoriteGenres.asFavoriteGenresString(existingUser?.favoriteGenres ?: ""),
            bio = bio ?: existingUser?.bio,
            joinDate = joinDate ?: existingUser?.joinDate ?: getCurrentTime(),
            profileImageUrl = profileImageUrl ?: existingUser?.profileImageUrl
        )
    }

    private fun RemoteUserStatisticsDto.toLocalUserStats(existingStats: UserStats?): UserStats {
        return UserStats(
            statId = existingStats?.statId ?: statId ?: 0,
            userUuid = userUuid,
            appLevel = appLevel ?: existingStats?.appLevel ?: 1,
            currentExp = currentExp ?: existingStats?.currentExp ?: 0,
            totalPlayTime = totalPlayTime ?: existingStats?.totalPlayTime ?: 0L,
            completedParts = completedParts ?: existingStats?.completedParts ?: 0,
            avgAccuracy = avgAccuracy ?: existingStats?.avgAccuracy ?: 0.0,
            badgeCount = badgeCount ?: existingStats?.badgeCount ?: 0,
            lightstickCount = lightstickCount ?: existingStats?.lightstickCount ?: 0,
            achievementScore = achievementScore ?: existingStats?.achievementScore ?: 0,
            lastUpdated = lastUpdated ?: existingStats?.lastUpdated ?: getCurrentTime()
        )
    }

    private fun RemoteAchievementDto.toLocalProgress(userId: String): UserAchievementProgress {
        return UserAchievementProgress(
            userUuid = userId,
            achievementCode = id,
            currentStep = currentCount ?: 0,
            goalStep = goalCount ?: 0,
            isCompleted = isUnlocked ?: false,
            achievedDate = achievedAt.asNormalizedString()
        )
    }

    private fun JsonElement?.asFavoriteGenresString(defaultValue: String): String {
        if (this == null || isJsonNull) return defaultValue
        return when {
            isJsonArray -> asJsonArray.mapNotNull { item ->
                item.takeUnless { it.isJsonNull }?.asString?.trim()?.takeIf { it.isNotEmpty() }
            }.joinToString(",")
            isJsonPrimitive -> asString.trim().removeSurrounding("[", "]")
                .split(",")
                .map { it.trim().trim('"') }
                .filter { it.isNotEmpty() }
                .joinToString(",")
            else -> defaultValue
        }
    }

    private fun JsonElement?.asNormalizedString(): String? {
        if (this == null || isJsonNull) return null
        return when {
            isJsonPrimitive -> asJsonPrimitive.let { primitive ->
                when {
                    primitive.isString -> primitive.asString
                    primitive.isNumber -> primitive.asNumber.toString()
                    primitive.isBoolean -> primitive.asBoolean.toString()
                    else -> primitive.toString()
                }
            }
            else -> toString()
        }
    }

    // --- History & Stats Update ---
    suspend fun savePracticeResult(result: PracticeHistory) {
        historyDao.insertHistory(result)
        upsertUserChoreoStats(result)

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
        val artistKey = resolveArtistKey(result.songId, result.artistName) ?: return
        incrementArtistAchievements(result.userUuid, artistKey)
    }



    private suspend fun upsertUserChoreoStats(result: PracticeHistory) {
        val existing = userChoreoStatsDao.getOne(result.userUuid, result.songId, result.partNumber)
        val updated = UserChoreoStats(
            id = "${result.userUuid}_${result.songId}_${result.partNumber}",
            userUuid = result.userUuid,
            songId = result.songId,
            partNumber = result.partNumber,
            practiceCount = (existing?.practiceCount ?: 0) + 1,
            lastPracticedAt = result.createdAt
        )
        userChoreoStatsDao.upsert(updated)
    }

    fun getRecentChoreoRows(userId: String) = userChoreoStatsDao.getRecentChoreoRows(userId)

    suspend fun markPracticePartCompleted(userId: String, songId: Long, partNumber: Int, artistName: String) {
        upsertLocalPracticeStats(userId, songId, partNumber)
        val artistKey = resolveArtistKey(songId, artistName) ?: return
        incrementArtistAchievements(userId, artistKey)
    }

    private suspend fun upsertLocalPracticeStats(userId: String, songId: Long, partNumber: Int) {
        val existing = userChoreoStatsDao.getOne(userId, songId, partNumber)
        val updated = UserChoreoStats(
            id = "${userId}_${songId}_${partNumber}",
            userUuid = userId,
            songId = songId,
            partNumber = partNumber,
            practiceCount = (existing?.practiceCount ?: 0) + 1,
            lastPracticedAt = getCurrentTime()
        )
        userChoreoStatsDao.upsert(updated)
    }

    private suspend fun incrementArtistAchievements(userId: String, artistKey: String) {
        val achievementCodes = listOf("${artistKey}_complete_01", "${artistKey}_complete_50")
        val nowMillis = System.currentTimeMillis()

        achievementCodes.forEach { code ->
            val progress = achievementDao.getUserAchievementProgressOneShot(userId, code) ?: return@forEach
            if (progress.isCompleted) return@forEach

            val updatedStep = min(progress.currentStep + 1, progress.goalStep)
            val completed = updatedStep >= progress.goalStep
            achievementDao.updateProgress(
                userId = userId,
                code = code,
                step = updatedStep,
                completed = completed,
                date = if (completed) getCurrentTime() else null
            )

            if (completed) {
                val achievementMeta = achievementDao.getAchievementByCode(code) ?: return@forEach
                when (achievementMeta.rewardType) {
                    "badge" -> {
                        val badgeBaseId = achievementMeta.rewardId ?: return@forEach
                        val badgeId = "${badgeBaseId}_$userId"
                        achievementDao.unlockBadge(userId, badgeId, nowMillis)
                    }

                    "icon" -> {
                        val lightStickId = achievementMeta.rewardId ?: return@forEach
                        achievementDao.unlockLightStick(lightStickId, nowMillis)
                    }
                }
            }
        }
    }

    private suspend fun resolveArtistKey(songId: Long, rawArtist: String): String? {
        val song = songDao.getSongById(songId)
        return normalizeArtistKey(rawArtist)
            ?: song?.artistKr?.let(::normalizeArtistKey)
            ?: song?.artistEn?.let(::normalizeArtistKey)
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
    fun getTopPracticedHistoryRows(userId: String) = historyDao.getTopPracticedHistoryRows(userId)
    suspend fun getBestScore(userId: String, songId: Long): Int? = historyDao.getBestScore(userId, songId)
    suspend fun getHistoryByJsonFileName(userId: String, jsonFileName: String): PracticeHistory? =
        historyDao.getHistoryByJsonFileName(userId, jsonFileName)

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
