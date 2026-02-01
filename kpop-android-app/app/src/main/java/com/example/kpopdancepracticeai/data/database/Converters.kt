package com.example.kpopdancepracticeai.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room Database용 Type Converter
 * 복잡한 데이터(List, Map)를 DB에 저장하기 위해 JSON String으로 변환합니다.
 */
class Converters {
    private val gson = Gson()

    // --- 1. Map<String, Int> 변환기 (예: 부위별 정확도 {"LeftArm": 80, "Leg": 90}) ---
    @TypeConverter
    fun fromStringMap(value: String?): Map<String, Int>? {
        if (value == null) return null
        val type = object : TypeToken<Map<String, Int>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromMap(map: Map<String, Int>?): String? {
        return map?.let { gson.toJson(it) }
    }

    // --- 2. List<String> 변환기 (예: 많이 틀린 부위 ["Knee", "Elbow"]) ---
    @TypeConverter
    fun fromStringList(value: String?): List<String>? {
        if (value == null) return null
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromList(list: List<String>?): String? {
        return list?.let { gson.toJson(it) }
    }

}