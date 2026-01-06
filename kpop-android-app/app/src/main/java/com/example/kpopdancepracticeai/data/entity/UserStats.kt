package com.example.kpopdancepracticeai.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 사용자 요약 정보 테이블 (UserStats)
 * 역할: 사용자의 누적 데이터 저장 (총 연습 시간, 레벨, 경험치 등)
 * 특징: PK는 userId 하나만 존재하며, 계속 Update 되는 구조
 */
@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey
    val userId: String, // 사용자 고유 ID (로그인 ID와 동일)

    val totalPracticeTimeSeconds: Long = 0, // 총 연습 시간 (초 단위)
    val completedSongCount: Int = 0, // 완료한 곡 총 개수
    val completedPartCount: Int = 0, // 완료한 파트 총 개수

    // 복잡한 리스트 데이터는 JSON 문자열로 변환하여 저장 (전략 문서 B항 참조)
    // 예: ["badge_newbie", "badge_perfect_score"]
    val earnedBadgesJson: String = "[]",

    val currentLevel: Int = 1, // 현재 레벨
    val currentXp: Int = 0, // 현재 경험치
    val averageAccuracy: Float = 0f // 전체 평균 정확도
)