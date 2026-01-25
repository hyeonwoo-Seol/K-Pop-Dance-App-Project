package com.example.kpopdancepracticeai.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["login_id"], unique = true),
        Index(value = ["email"], unique = true)
    ]
)
data class User(
    // Firebase UID를 저장할 Primary Key
    @PrimaryKey
    @ColumnInfo(name = "user_uuid")
    val userUuid: String,

    @ColumnInfo(name = "login_id")
    val loginId: String,

    @ColumnInfo(name = "email")
    val email: String,

    // [수정] 구글 로그인 등 소셜 로그인의 경우 비밀번호가 없으므로 Nullable로 변경
    @ColumnInfo(name = "password_hash")
    val passwordHash: String? = null,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "birth_date")
    val birthDate: String,

    @ColumnInfo(name = "gender")
    val gender: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)