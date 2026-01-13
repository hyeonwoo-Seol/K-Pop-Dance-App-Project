package com.example.kpopdancepracticeai.data.mapper

import com.example.kpopdancepracticeai.data.dto.AnalysisResultResponse
import com.example.kpopdancepracticeai.data.entity.PracticeHistory
import com.example.kpopdancepracticeai.util.FilenameParser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AnalysisMapper {

    // 현재 시간을 문자열 포맷으로 반환하는 헬퍼 함수
    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    fun mapToPracticeHistory(
        analysisResult: AnalysisResultResponse,
        metadata: FilenameParser.ParsedMetadata,
        videoPath: String, // 녹화된 사용자 영상 경로
        fullJsonPath: String // 저장된 전체 분석 JSON 파일 경로
    ): PracticeHistory {

        val summary = analysisResult.summary
        val meta = analysisResult.metadata

        return PracticeHistory(
            // AutoGenerate ID는 0으로 설정하면 Room이 자동 생성
            resultId = 0,

            // 1. 식별자 정보 (파일명 파싱 데이터)
            userUuid = metadata.userId,
            songId = metadata.songId.toLongOrNull() ?: 0L,
            partNumber = metadata.partNumber.toIntOrNull() ?: 1,
            artistName = metadata.artist,

            // 2. 핵심 분석 결과 (DTO summary)
            totalScore = summary.totalScore,
            grade = summary.accuracyGrade,

            // TypeConverter가 Map<String, Int> -> JSON String 자동 변환 처리
            partAccuracies = summary.partAccuracies,

            // TypeConverter가 List<String> -> JSON String 자동 변환 처리
            worstPoints = summary.worstPoints,

            // 3. 영상 메타데이터 (DTO metadata)
            durationSec = meta.durationSec,
            fps = meta.fps,
            videoWidth = meta.videoWidth,
            videoHeight = meta.videoHeight,
            totalFrames = meta.totalFrames,

            // 4. 파일 경로 및 생성 시간
            createdAt = getCurrentDateTime(),
            fullJsonPath = fullJsonPath,
            userVideoPath = videoPath
        )
    }
}