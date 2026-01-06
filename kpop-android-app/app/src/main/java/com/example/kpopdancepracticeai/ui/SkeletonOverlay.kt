package com.example.kpopdancepracticeai.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import com.example.kpopdancepracticeai.ui.theme.PointGreen
import com.example.kpopdancepracticeai.ui.theme.PointPurple

@Composable
fun SkeletonOverlay(
    keyPoints: List<KeyPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = PointGreen, // 뼈대 색상 (초록색)
    jointColor: Color = Color.White, // 관절 색상 (흰색)
    lineWidth: Float = 8f,
    jointRadius: Float = 12f
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 1. 관절 좌표 매핑 (정규화 좌표 -> 픽셀 좌표)
        val pointMap = keyPoints.associate { point ->
            point.type to Offset(point.x * width, point.y * height)
        }

        // 2. 뼈대 그리기 (선)
        bodyConnections.forEach { (startPart, endPart) ->
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

        // 3. 관절 그리기 (점)
        val points = pointMap.values.toList()
        drawPoints(
            points = points,
            pointMode = PointMode.Points,
            color = jointColor,
            strokeWidth = jointRadius * 2, // 점의 지름
            cap = StrokeCap.Round
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun SkeletonOverlayPreview() {
    // 테스트용 데이터로 미리보기
    val dummyData = SkeletonDummyGenerator.generateFrame(0.5f)
    SkeletonOverlay(keyPoints = dummyData)
}