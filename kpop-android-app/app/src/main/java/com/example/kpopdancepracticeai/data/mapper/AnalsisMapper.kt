package com.example.kpopdancepracticeai.data.mapper

import com.google.gson.Gson
import com.example.kpopdancepracticeai.data.dto.AnalysisResultResponse
import com.example.kpopdancepracticeai.data.entity.PracticeHistory
import com.example.kpopdancepracticeai.util.FilenameParser

/**
 * DTO -> Entity 변환 매퍼
 * 역할: 서버 응답(DTO) + 파일명 메타데이터 -> DB 엔티티(PracticeHistory) 변환
 */
object AnalysisMapper {
    private val gson = Gson()

    /**
     * FilenameParser.ParsedMetadata를 활용하는 개선된 매핑 함수
     * @param videoPath DB 저장 시 필수인 영상 경로 (추가됨)
     */
    fun mapToPracticeHistory(
        analysisResult: AnalysisResultResponse,
        metadata: FilenameParser.ParsedMetadata,
        songTitle: String,
        videoPath: String // [수정] Entity 필수 필드 추가
    ): PracticeHistory {

        val summary = analysisResult.summary

        // [수정] Entity에 별도 컬럼이 없는 상세 데이터는 JSON으로 묶어서 저장합니다.
        // 나중에 꺼내 쓸 때 Gson으로 다시 파싱해서 사용하면 됩니다.
        val detailData = mapOf(
            "artistName" to metadata.artist,
            "partName" to "Part ${metadata.partNumber}",
            "songTitle" to songTitle,
            "grade" to summary.accuracyGrade,
            "partAccuracies" to summary.partAccuracies,
            "worstPoints" to summary.worstPoints
        )

        return PracticeHistory(
            // 1. 필수 식별 정보
            userId = metadata.userId,
            songId = metadata.songId.toLongOrNull() ?: 0L,
            partId = metadata.partNumber.toLongOrNull() ?: 1L, // partNumber -> partId 매핑

            // 2. 핵심 결과
            score = summary.totalScore,
            date = System.currentTimeMillis(),
            videoPath = videoPath,

            // 3. 상세 분석 데이터 (JSON String으로 저장)
            analysisResultJson = gson.toJson(detailData),

            // 4. 상태 및 기본값
            isSynced = false,
            analysisStatus = "DONE", // 분석이 완료된 상태로 매핑

            // 5. 기타 필수 필드 기본값 채우기
            durationSec = 0.0,
            fps = 0.0,
            videoWidth = 0,
            videoHeight = 0,
            totalFrames = 0,
            serverResultId = null,
            uploadedUrl = null
        )
    }
}