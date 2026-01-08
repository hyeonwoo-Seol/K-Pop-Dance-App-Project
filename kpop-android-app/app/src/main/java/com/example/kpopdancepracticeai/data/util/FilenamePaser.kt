package com.example.kpopdancepracticeai.util


// 파일 이름에서 메타데이터를 추출하는 유틸리티

object FilenameParser {

    data class ParsedMetadata(
        val userId: String,
        val songId: String,
        val artist: String,
        val partNumber: String
    )

    fun parse(filename: String): ParsedMetadata? {
        try {
            // 1. 확장자 제거 (.txt 또는 .mp4 등)
            val nameWithoutExtension = filename.substringBeforeLast(".")

            // 2. 이중 언더바(__) 기준으로 분리
            val parts = nameWithoutExtension.split("__")

            // 3. 포맷 유효성 검사
            // 예상: [0]userId, [1]songId, [2]Artist, [3]PartNumber, [4]result
            // 최소 4개 이상이어야 함 (result가 없는 경우 대비)
            if (parts.size < 4) return null

            return ParsedMetadata(
                userId = parts[0],
                songId = parts[1],
                artist = parts[2],
                partNumber = parts[3]
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}