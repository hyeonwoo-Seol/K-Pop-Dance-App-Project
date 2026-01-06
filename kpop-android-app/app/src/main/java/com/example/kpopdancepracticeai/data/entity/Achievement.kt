package com.example.kpopdancepracticeai.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 업적 및 도전과제 테이블 (Achievements)
 * 역할: 업적의 진행 상황 추적
 */
@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey
    val achievementId: String, // 업적 고유 ID (예: "acc_95_5times", "total_time_100h")

    val title: String, // 업적 이름 (예: "완벽주의자")
    val description: String, // 업적 설명 (예: "95% 이상의 정확도 5회 달성")

    val currentProgress: Int = 0, // 현재 진행도 (예: 3)
    val targetValue: Int, // 목표 값 (예: 5)

    val isCompleted: Boolean = false, // 달성 여부
    val rewardXp: Int = 0, // 달성 시 보상 경험치

    val iconResName: String = "" // 아이콘 리소스 이름 (필요시 사용)
)