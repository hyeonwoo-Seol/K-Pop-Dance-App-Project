package com.example.kpopdancepracticeai.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 사용자 통계 (Notion Section 7)
 * 테이블명: user_statistics
 */
@Entity(
    tableName = "user_statistics",
    indices = [Index(value = ["user_uuid"], unique = true)] // 유저 1명당 1개의 통계만 존재
)
data class UserStats(
    // 1. 통계 데이터 고유 ID (PK)
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "stat_id")
    val statId: Long = 0,

    // 2. 사용자 고유 ID (FK)
    @ColumnInfo(name = "user_uuid")
    val userId: String,

    // 3. 총 앱 실행/연습 시간 (초 단위) -> Long
    @ColumnInfo(name = "total_play_time")
    val totalPlayTime: Long = 0L,

    // 4. 연습 완료한 노래 파트 수
    @ColumnInfo(name = "completed_parts")
    val completedParts: Int = 0,

    // 5. 전체 파트 평균 정확도 (%) -> Double
    @ColumnInfo(name = "avg_accuracy")
    val avgAccuracy: Double = 0.0,

    // 6. 획득한 배지 개수
    @ColumnInfo(name = "badge_count")
    val badgeCount: Int = 0,

    // 7. 획득한 응원봉 개수
    @ColumnInfo(name = "lightstick_count")
    val lightstickCount: Int = 0,

    // 8. 업적 점수 (또는 달성 개수)
    @ColumnInfo(name = "achievement_score")
    val achievementScore: Int = 0,

    // 9. 마지막 통계 갱신 시간 (String)
    @ColumnInfo(name = "last_updated")
    val lastUpdated: String = getCurrentTime()
)

// 현재 시간을 문자열로 반환하는 헬퍼 함수
fun getCurrentTime(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date())
}