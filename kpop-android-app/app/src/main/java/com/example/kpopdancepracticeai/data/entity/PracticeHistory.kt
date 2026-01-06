package com.example.kpopdancepracticeai.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 연습 기록 테이블 (PracticeHistory)
 * 역할: 한 번의 춤 연습 결과를 저장
 * 전략: 저장 시 isSynced = false로 저장하고, AWS 전송 성공 시 true로 업데이트
 */
@Entity(tableName = "practice_history")
data class PracticeHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // 로컬 DB 내 고유 ID (자동 증가)

    val userId: String, // 사용자 ID (Foreign Key 역할)
    val songId: String, // 서버의 Song ID (예: "bts_dynamite")
    val songTitle: String, // 곡 제목 (오프라인 표시용)
    val artistName: String, // 아티스트 이름
    val partName: String, // 연습한 파트 (예: "Part 2: 메인 파트")

    val practiceDate: Long, // 연습 날짜 (Timestamp: System.currentTimeMillis())
    val score: Int, // 점수 (0~100)
    val accuracy: Float, // 정확도 (상세 소수점)

    // AWS 동기화 핵심 필드
    // false: 아직 서버로 안 보냄 (동기화 필요)
    // true: 서버에 저장됨
    val isSynced: Boolean = false
)