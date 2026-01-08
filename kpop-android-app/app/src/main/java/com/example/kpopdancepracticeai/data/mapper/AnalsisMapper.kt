package com.example.kpopdancepracticeai.data.mapper

import com.example.kpopdancepracticeai.data.dto.AnalysisResultResponse
import com.example.kpopdancepracticeai.data.entity.PracticeHistory
import com.example.kpopdancepracticeai.util.FilenameParser

/**
 * DTO -> Entity 변환 매퍼
 * 역할: 서버 응답(DTO) + 파일명 메타데이터 -> DB 엔티티(PracticeHistory) 변환
 */
object AnalysisMapper {

    /**
     * FilenameParser.ParsedMetadata를 활용하는 개선된 매핑 함수
     * * @param analysisResult 서버에서 받은 JSON 파싱 결과 (점수, 프레임 데이터 등)
     * @param metadata 파일명에서 파싱한 정보 (userId, songId, artist, partNumber)
     * @param songTitle 파일명에는 없어서 따로 받아야 하는 곡 제목
     */
    fun mapToPracticeHistory(
        analysisResult: AnalysisResultResponse,
        metadata: FilenameParser.ParsedMetadata, // 파싱된 메타데이터 객체를 통째로 받음
        songTitle: String
    ): PracticeHistory {

        val summary = analysisResult.summary

        return PracticeHistory(
            // 1. 메타데이터에서 추출한 정보 사용 (파일명 유래)
            userId = metadata.userId,
            songId = metadata.songId,
            artistName = metadata.artist,
            partName = metadata.partNumber,

            // 2. 외부에서 따로 받은 정보 사용
            songTitle = songTitle,

            // 3. 현재 시간 기록 (저장 시점)
            practiceDate = System.currentTimeMillis(),

            // 4. 분석 결과 매핑 (JSON 유래)
            score = summary.totalScore,
            // 필요 시 정확도를 별도 계산하거나 점수와 동일하게 처리
            accuracy = summary.totalScore.toFloat(),

            // 5. 동기화 상태 (로컬 우선 저장 -> 추후 전송)
            isSynced = false
        )
    }
}