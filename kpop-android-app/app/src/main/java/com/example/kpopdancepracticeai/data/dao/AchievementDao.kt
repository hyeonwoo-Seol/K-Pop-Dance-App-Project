package com.example.kpopdancepracticeai.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kpopdancepracticeai.data.entity.Achievement
import com.example.kpopdancepracticeai.data.entity.Badge
import com.example.kpopdancepracticeai.data.entity.LightStick
import com.example.kpopdancepracticeai.data.entity.UserAchievementProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    // 사용자의 모든 업적 진행도 조회
    @Query("SELECT * FROM user_achievement_progress WHERE user_uuid = :userId")
    fun getUserAchievementProgress(userId: String): Flow<List<UserAchievementProgress>>

    @Query("SELECT * FROM user_achievement_progress WHERE user_uuid = :userId AND achievement_code = :code LIMIT 1")
    suspend fun getUserAchievementProgressOneShot(userId: String, code: String): UserAchievementProgress?

    @Query("SELECT * FROM achievements WHERE id = :code LIMIT 1")
    suspend fun getAchievementByCode(code: String): Achievement?

    // [수정됨] 이제 Badge.kt에 user_uuid가 추가되었으므로 오류 없이 정상 작동합니다!
    @Query("SELECT * FROM badges WHERE user_uuid = :userId AND isUnlocked = 1")
    fun getUserBadges(userId: String): Flow<List<Badge>>

    // 초기 데이터 세팅용: 업적 메타 데이터 삽입
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievements(achievements: List<Achievement>)

    // 초기 데이터 세팅용: 업적 진행도 삽입
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProgress(progress: List<UserAchievementProgress>)

    // 초기 데이터 세팅용: 응원봉 메타 데이터 삽입
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLightSticks(lightSticks: List<LightStick>)

    // 배지 획득 처리
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBadge(badge: Badge)

    // 업적 진행도 업데이트
    @Query("UPDATE user_achievement_progress SET current_step = :step, is_completed = :completed, achieved_date = :date WHERE user_uuid = :userId AND achievement_code = :code")
    suspend fun updateProgress(userId: String, code: String, step: Int, completed: Boolean, date: String?)

    @Query("UPDATE badges SET isUnlocked = 1, obtainedAt = :obtainedAt WHERE user_uuid = :userId AND id = :badgeId AND isUnlocked = 0")
    suspend fun unlockBadge(userId: String, badgeId: String, obtainedAt: Long): Int

    @Query("UPDATE light_sticks SET is_owned = 1, obtained_at = :obtainedAt WHERE id = :lightStickId AND is_owned = 0")
    suspend fun unlockLightStick(lightStickId: String, obtainedAt: Long): Int
}
