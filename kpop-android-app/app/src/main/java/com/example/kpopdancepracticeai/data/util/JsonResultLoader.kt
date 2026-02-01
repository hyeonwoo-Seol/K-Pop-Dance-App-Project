package com.example.kpopdancepracticeai.util

import android.content.Context
import android.util.Log
import com.example.kpopdancepracticeai.data.dto.AnalysisResultResponse
import com.google.gson.Gson
import java.io.File

/**
 * JSON 결과 파일 로더
 * 내부 저장소(filesDir/analysis_results/)에서 JSON 파일을 읽어 파싱
 * PresignedUrlUploader가 저장한 경로와 일치해야함.
 */
object JsonResultLoader {
    fun loadAnalysisResult(context: Context, jsonFileName: String): AnalysisResultResponse? {
        return try {
            // 1. 파일 경로 설정 (PresignedUrlUploader.saveJsonToInternalStorage와 경로 일치)
            val directory = File(context.filesDir, "analysis_results")
            val file = File(directory, jsonFileName)

            if (!file.exists()) {
                Log.e("JsonLoader", "파일이 존재하지 않습니다: ${file.absolutePath}")
                return null
            }

            // 2. 파일 읽기 및 파싱
            val jsonString = file.readText()
            Gson().fromJson(jsonString, AnalysisResultResponse::class.java)
        } catch (e: Exception) {
            Log.e("JsonLoader", "JSON 파싱 실패", e)
            null
        }
    }
}