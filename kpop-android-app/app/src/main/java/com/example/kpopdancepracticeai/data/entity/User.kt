package com.example.kpopdancepracticeai.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 사용자 정보 (Notion Section 6)
 * Users 테이블 스키마 정의
 */
@Entity(
    tableName = "users",
    indices = [
        // login_id와 email은 중복 불가(Unique) 제약조건 설정
        Index(value = ["login_id"], unique = true),
        Index(value = ["email"], unique = true)
    ]
)
data class User(
    // 1. 시스템 내부 식별용 고유 ID (PK)
    @PrimaryKey
    @ColumnInfo(name = "user_uuid")
    val userId: String,

    // 2. 사용자가 설정한 ID (Unique)
    @ColumnInfo(name = "login_id")
    val loginId: String,

    // 3. 사용자 이메일 (Unique)
    @ColumnInfo(name = "email")
    val email: String,

    // 4. 암호화된 비밀번호
    @ColumnInfo(name = "password_hash")
    val passwordHash: String,

    // 5. 생년월일 (YYYY-MM-DD)
    @ColumnInfo(name = "birth_date")
    val birthDate: String,

    // 6. 댄스 실력 ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')
    @ColumnInfo(name = "dance_skill")
    val danceSkill: String,

    // 7. 관심 장르 목록 (JSON Array -> List<String> 변환)
    @ColumnInfo(name = "favorite_genres")
    val favoriteGenres: List<String>?,

    // 8. 자기소개
    @ColumnInfo(name = "bio")
    val bio: String?,

    // 9. 사용자 레벨 (기본값 1)
    @ColumnInfo(name = "app_level")
    val appLevel: Int = 1,

    // 10. 획득한 누적 경험치 (기본값 0)
    @ColumnInfo(name = "current_exp")
    val currentExp: Long = 0L,

    // 11. 회원가입 일시 (자동 생성)
    @ColumnInfo(name = "join_date")
    val joinDate: String
)