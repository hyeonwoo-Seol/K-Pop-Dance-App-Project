package com.example.kpopdancepracticeai.ui.test

import com.example.kpopdancepracticeai.data.dto.FrameData
import com.example.kpopdancepracticeai.ui.BodyPart
import com.example.kpopdancepracticeai.ui.KeyPoint

/**
 * 테스트용 데이터 변환기
 * 역할: 서버 DTO(FrameData)를 UI용 데이터(KeyPoint)로 변환
 */
object DataConverter {
    // DTO의 Raw Keypoints -> UI용 KeyPoint 객체 리스트로 변환
    fun convertToKeyPoints(frameData: FrameData): List<KeyPoint> {
        val keyPoints = mutableListOf<KeyPoint>()

        // BodyPart Enum 순서대로 매핑 (NOSE(0) ~ RIGHT_ANKLE(16))
        val parts = BodyPart.values()

        frameData.keypoints.forEachIndexed { index, rawPoint ->
            // Enum 범위 내에 있고, 좌표 데이터가 유효한 경우만 처리
            if (index < parts.size) {
                // rawPoint = [x, y, confidence] 형태라고 가정
                if (rawPoint.size >= 2) {
                    val x = rawPoint[0]
                    val y = rawPoint[1]
                    // 신뢰도 값이 있으면 사용, 없으면 0f 처리
                    val conf = if (rawPoint.size > 2) rawPoint[2] else 0f

                    keyPoints.add(
                        KeyPoint(
                            type = parts[index],
                            x = x,
                            y = y,
                            confidence = conf
                        )
                    )
                }
            }
        }
        return keyPoints
    }
}