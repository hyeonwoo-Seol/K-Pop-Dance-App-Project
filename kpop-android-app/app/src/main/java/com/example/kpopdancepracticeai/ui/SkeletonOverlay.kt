package com.example.kpopdancepracticeai.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import com.example.kpopdancepracticeai.ui.theme.PointGreen
import kotlin.math.max

// 에러 표시 색상 정의
private val ColorError = Color.Red
private val ColorNormal = PointGreen

@Composable
fun SkeletonOverlay(
    keyPoints: List<KeyPoint>,
    errors: List<Int>? = null, // 관절별 에러 정보 (0: 정상, 1: 오류)
    modifier: Modifier = Modifier,
    lineColor: Color = Color.White, // 뼈대 색상
    jointRadius: Float = 12f,
    lineWidth: Float = 8f
) {
    // 에러 발생 시 깜빡이는 효과 (Pulse Animation) 설정
    val infiniteTransition = rememberInfiniteTransition(label = "ErrorPulse")


    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // 1. 좌표 복원 (Denormalization)
        // 가이드에 따라 max(width, height)를 기준으로 정규화 해제
        val maxDim = max(canvasWidth, canvasHeight)

        // 화면 비율에 따른 오프셋 계산 (Letterbox 처리)
        // 영상이 화면 중앙에 위치한다고 가정 (Center Crop 방식 대응)
        val offsetX = (canvasWidth - maxDim) / 2
        val offsetY = (canvasHeight - maxDim) / 2

        val pointMap = keyPoints.associate { point ->
            val px = point.x * maxDim + offsetX
            val py = point.y * maxDim + offsetY
            point.type to Offset(px, py)
        }

        // 2. 뼈대 그리기 (선)
        bodyConnections.forEach { (startPart, endPart) ->
            // NECK이 포함된 연결선은 그리지 않으려면 여기서 필터링할 수 있습니다.
            // 현재 SkeletonData.kt의 bodyConnections에는 NECK 연결이 주석 처리되어 있으므로 그대로 둡니다.
            val start = pointMap[startPart]
            val end = pointMap[endPart]

            if (start != null && end != null) {
                drawLine(
                    color = lineColor,
                    start = start,
                    end = end,
                    strokeWidth = lineWidth,
                    cap = StrokeCap.Round
                )
            }
        }

        // 3. 관절 그리기 (점) 및 에러 시각화
        keyPoints.forEach { point ->
            // [수정] NECK(목) 부위는 오버레이에 표시하지 않음
            if (point.type == BodyPart.NECK) return@forEach

            val offset = pointMap[point.type] ?: return@forEach

            // 에러 여부 확인 (인덱스 범위 체크 포함)
            val isError = if (errors != null && point.type.ordinal < errors.size) {
                errors[point.type.ordinal] == 1
            } else {
                false
            }

            val color = if (isError) ColorError else ColorNormal
            // 관절 포인트 그리기
            drawCircle(
                color = color,
                radius = jointRadius,
                center = offset
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun SkeletonOverlayPreview() {
    // 테스트용 데이터 생성
    val originalData = SkeletonDummyGenerator.generateFrame(0.5f)


    // (실제 데이터 로직은 건드리지 않고, 미리보기 데이터만 조정)
    val narrowDummyData = originalData.map { point ->
        val centerX = 0.5f
        // 중심(0.5)을 기준으로 가로 너비를 65%로 줄임
        val newX = centerX + (point.x - centerX) * 0.65f
        point.copy(x = newX)
    }

    // 테스트용 에러 데이터 (일부 관절에 에러 표시)
    val dummyErrors = List(17) { index ->
        if (index == 5 || index == 10) 1 else 0
    }

    SkeletonOverlay(
        keyPoints = narrowDummyData,
        errors = dummyErrors
    )
}