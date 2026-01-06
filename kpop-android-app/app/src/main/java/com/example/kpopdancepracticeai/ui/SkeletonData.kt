package com.example.kpopdancepracticeai.ui

import androidx.compose.ui.graphics.Color

// 1. 관절 포인트 데이터 (0.0 ~ 1.0 정규화된 좌표)
data class KeyPoint(
    val type: BodyPart,
    val x: Float,
    val y: Float,
    val confidence: Float = 1.0f // 인식 신뢰도 (0.0~1.0)
)

// 2. 신체 부위 열거형 (YOLO v11 기준 + Neck)

enum class BodyPart {
    NOSE,           // 0
    LEFT_EYE,       // 1
    RIGHT_EYE,      // 2
    LEFT_EAR,       // 3
    RIGHT_EAR,      // 4
    LEFT_SHOULDER,  // 5
    RIGHT_SHOULDER, // 6
    LEFT_ELBOW,     // 7
    RIGHT_ELBOW,    // 8
    LEFT_WRIST,     // 9
    RIGHT_WRIST,    // 10
    LEFT_HIP,       // 11
    RIGHT_HIP,      // 12
    LEFT_KNEE,      // 13
    RIGHT_KNEE,     // 14
    LEFT_ANKLE,     // 15
    RIGHT_ANKLE,    // 16
    NECK            // 17 (규격서 2.2에 따라 추가됨)
}

// 3. 뼈대 연결 정보 (어느 점과 어느 점을 이을지)
val bodyConnections = listOf(
    // 얼굴
    BodyPart.NOSE to BodyPart.LEFT_EYE, BodyPart.NOSE to BodyPart.RIGHT_EYE,
    BodyPart.LEFT_EYE to BodyPart.LEFT_EAR, BodyPart.RIGHT_EYE to BodyPart.RIGHT_EAR,

    // 상체
    BodyPart.LEFT_SHOULDER to BodyPart.RIGHT_SHOULDER,
    BodyPart.LEFT_SHOULDER to BodyPart.LEFT_HIP,
    BodyPart.RIGHT_SHOULDER to BodyPart.RIGHT_HIP,
    BodyPart.LEFT_SHOULDER to BodyPart.LEFT_ELBOW,
    BodyPart.LEFT_ELBOW to BodyPart.LEFT_WRIST,
    BodyPart.RIGHT_SHOULDER to BodyPart.RIGHT_ELBOW,
    BodyPart.RIGHT_ELBOW to BodyPart.RIGHT_WRIST,

    // 하체
    BodyPart.LEFT_HIP to BodyPart.RIGHT_HIP,
    BodyPart.LEFT_HIP to BodyPart.LEFT_KNEE,
    BodyPart.LEFT_KNEE to BodyPart.LEFT_ANKLE,
    BodyPart.RIGHT_HIP to BodyPart.RIGHT_KNEE,
    BodyPart.RIGHT_KNEE to BodyPart.RIGHT_ANKLE,

    // [선택 사항] Neck 연결 (필요 시 주석 해제하여 사용)
    // BodyPart.NOSE to BodyPart.NECK,
    // BodyPart.NECK to BodyPart.LEFT_SHOULDER,
    // BodyPart.NECK to BodyPart.RIGHT_SHOULDER
)

// 4. 테스트용 더미 스켈레톤 생성기(테스트 용이에요 나중에 지우거나 남겨두거나 하면 돼요)
object SkeletonDummyGenerator {
    // 재생 시간(progress: 0.0~1.0)에 따라 움직이는 스켈레톤 생성
    fun generateFrame(progress: Float): List<KeyPoint> {
        val time = progress * 20 // 움직임 속도 조절

        // 기본 포즈 (T-Pose 변형) 중심 좌표
        val cx = 0.5f
        val cy = 0.4f

        // 간단한 사인파(Sin) 애니메이션으로 팔다리 움직임 시뮬레이션
        val armSwing = kotlin.math.sin(time).toFloat() * 0.1f
        val legSwing = kotlin.math.cos(time).toFloat() * 0.05f

        return listOf(
            KeyPoint(BodyPart.NOSE, cx, cy - 0.25f),
            KeyPoint(BodyPart.LEFT_EYE, cx - 0.02f, cy - 0.27f),
            KeyPoint(BodyPart.RIGHT_EYE, cx + 0.02f, cy - 0.27f),
            KeyPoint(BodyPart.LEFT_EAR, cx - 0.04f, cy - 0.26f),
            KeyPoint(BodyPart.RIGHT_EAR, cx + 0.04f, cy - 0.26f),

            // Neck 추가 (어깨와 머리 사이 대략적인 위치)ㄱ
            KeyPoint(BodyPart.NECK, cx, cy - 0.20f),

            KeyPoint(BodyPart.LEFT_SHOULDER, cx - 0.15f, cy - 0.15f),
            KeyPoint(BodyPart.RIGHT_SHOULDER, cx + 0.15f, cy - 0.15f),

            // 팔 (움직임 적용)
            KeyPoint(BodyPart.LEFT_ELBOW, cx - 0.25f, cy + armSwing),
            KeyPoint(BodyPart.RIGHT_ELBOW, cx + 0.25f, cy - armSwing),
            KeyPoint(BodyPart.LEFT_WRIST, cx - 0.35f, cy + 0.1f + armSwing),
            KeyPoint(BodyPart.RIGHT_WRIST, cx + 0.35f, cy + 0.1f - armSwing),

            KeyPoint(BodyPart.LEFT_HIP, cx - 0.1f, cy + 0.15f),
            KeyPoint(BodyPart.RIGHT_HIP, cx + 0.1f, cy + 0.15f),

            // 다리 (움직임 적용)
            KeyPoint(BodyPart.LEFT_KNEE, cx - 0.1f + legSwing, cy + 0.4f),
            KeyPoint(BodyPart.RIGHT_KNEE, cx + 0.1f - legSwing, cy + 0.4f),
            KeyPoint(BodyPart.LEFT_ANKLE, cx - 0.1f + legSwing, cy + 0.65f),
            KeyPoint(BodyPart.RIGHT_ANKLE, cx + 0.1f - legSwing, cy + 0.65f)
        )
    }
}