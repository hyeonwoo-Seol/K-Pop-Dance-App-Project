package com.example.kpopdancepracticeai.data.dto

import com.google.gson.annotations.SerializedName


//AI 분석 서버로부터 받는 전체 응답 객체

data class AnalysisResultResponse(
    @SerializedName("metadata") val metadata: AnalysisMetadata,
    @SerializedName("summary") val summary: AnalysisSummary,
    @SerializedName("frames") val frames: List<FrameData>
)

data class AnalysisMetadata(
    @SerializedName("version") val version: String,
    @SerializedName("model") val model: String,
    @SerializedName("video_width") val videoWidth: Int,
    @SerializedName("video_height") val videoHeight: Int,
    @SerializedName("total_frames") val totalFrames: Int,
    @SerializedName("fps") val fps: Double,
    @SerializedName("duration_sec") val durationSec: Double
)

data class AnalysisSummary(
    @SerializedName("total_score") val totalScore: Int, // 종합 점수 (예: 88)
    @SerializedName("accuracy_grade") val accuracyGrade: String, // 등급 (예: "S")
    @SerializedName("worst_points") val worstPoints: List<String>, // 많이 틀린 부위 (예: ["Right Wrist", "Left Knee"])
    @SerializedName("part_accuracies") val partAccuracies: Map<String, Int> // 부위별 정확도 (예: {"Left Arm": 92})
)

data class FrameData(
    @SerializedName("frame_index") val frameIndex: Int,
    @SerializedName("timestamp") val timestamp: Double,
    @SerializedName("is_valid") val isValid: Boolean,
    @SerializedName("score") val score: Double, // 프레임별 점수

    // Keypoints: [x, y, confidence]의 배열 리스트
    @SerializedName("keypoints") val keypoints: List<List<Float>>,

    // Errors: 각 관절별 오류 여부 (0: 정상, 1: 오류)
    @SerializedName("errors") val errors: List<Int>
)