package com.example.kpopdancepracticeai.ui.test

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kpopdancepracticeai.data.dto.AnalysisResultResponse
import com.example.kpopdancepracticeai.data.mapper.AnalysisMapper
import com.example.kpopdancepracticeai.ui.SkeletonOverlay
import com.example.kpopdancepracticeai.util.FilenameParser
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader


@Composable
fun IntegrationTestScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var logText by remember { mutableStateOf("테스트 준비 완료.\n[Step 1]을 눌러 assets 폴더의 JSON 파일을 로드하세요.") }
    var isParsingSuccess by remember { mutableStateOf(false) }

    // 오버레이 데이터 상태
    var testKeyPoints by remember { mutableStateOf<List<com.example.kpopdancepracticeai.ui.KeyPoint>>(emptyList()) }
    var testErrors by remember { mutableStateOf<List<Int>>(emptyList()) }
    var showOverlay by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "통합 테스트 (파일명 파싱)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // --- Step 1: 파일 로드 및 파싱 ---
        Button(
            onClick = {
                scope.launch {
                    try {
                        logText = "> [Step 1] assets 폴더 검색 중...\n"
                        isParsingSuccess = false
                        showOverlay = false

                        // 1. Assets에서 .json 파일 목록 찾기
                        val assetManager = context.assets
                        val jsonFiles = assetManager.list("")?.filter { it.endsWith(".json") } ?: emptyList()

                        if (jsonFiles.isEmpty()) {
                            logText += "[실패] assets 폴더에 .json 파일이 없습니다.\n"
                            return@launch
                        }

                        // 랜덤으로 하나 선택
                        val targetFilename = jsonFiles.random()
                        logText += "[정보] 파일 선택됨: $targetFilename\n"

                        // 2. 파일 내용 읽기
                        val jsonString = try {
                            assetManager.open(targetFilename).use { inputStream ->
                                InputStreamReader(inputStream).use { reader ->
                                    BufferedReader(reader).readText()
                                }
                            }
                        } catch (e: Exception) {
                            logText += "[실패] 파일 읽기 실패: ${e.message}\n"
                            return@launch
                        }

                        logText += "[성공] 파일 로드 성공 (${jsonString.length} bytes)\n"

                        // 3. 파일명 파싱 (UserId, SongId, Artist, Part 추출)
                        // 구조 예시: kim889_540_원영_1_result.json -> [kim889, 540, 원영, 1, result]
                        val nameWithoutExt = targetFilename.substringBeforeLast(".")
                        val parts = nameWithoutExt.split("_")

                        // 변수 초기화
                        var metaUserId = ""
                        var metaSongId = ""
                        var metaArtist = ""
                        // [수정] Part 번호를 String으로 처리 (ParsedMetadata가 String을 요구함)
                        var parsedPartNumber = "1"

                        if (parts.size >= 4) {
                            metaUserId = parts[0]
                            metaSongId = parts[1]
                            metaArtist = parts[2]
                            // [수정] String 그대로 유지
                            parsedPartNumber = parts[3]

                            logText += """
                                [성공] 파일명 파싱 성공:
                                  - User: $metaUserId
                                  - SongID: $metaSongId
                                  - Artist: $metaArtist
                                  - Part: $parsedPartNumber
                            """.trimIndent() + "\n"
                        } else {
                            logText += "[주의] 파일명 형식이 예상과 다릅니다 (${parts.size}구획). 파싱 결과가 부정확할 수 있습니다.\n"
                        }

                        // 4. JSON 파싱
                        val gson = Gson()
                        val response = gson.fromJson(jsonString, AnalysisResultResponse::class.java)

                        if (response.summary == null) {
                            logText += "[주의] 경고: Summary 데이터가 null입니다. JSON 필드명(snake_case)이 DTO와 일치하는지 확인하세요.\n"
                        } else {
                            logText += "[성공] JSON 파싱 성공 (총점: ${response.summary.totalScore}점)\n"
                        }

                        logText += "   - 프레임 수: ${response.frames.size}\n"

                        // 5. DB Entity 매핑 테스트 (파싱한 메타데이터 활용)
                        try {
                            // [수정] String 타입으로 PartNumber 전달
                            val customMetadata = FilenameParser.ParsedMetadata(
                                metaUserId,
                                metaSongId,
                                metaArtist,
                                parsedPartNumber
                            )

                            val historyEntity = AnalysisMapper.mapToPracticeHistory(
                                analysisResult = response,
                                metadata = customMetadata,
                                songTitle = "테스트 곡 ($metaArtist)" // 곡 제목은 파일명에 없으므로 임시 값
                            )
                            logText += "[성공] DB 매핑 성공: ${historyEntity.score}점 / 날짜: ${historyEntity.practiceDate}\n"
                        } catch (e: Exception) {
                            logText += "[에러] DB 매핑 에러: ${e.message} (FilenameParser 구조 확인 필요)\n"
                        }

                        // 6. 오버레이용 데이터 변환
                        // keypoints가 비어있을 수 있으므로 첫 번째 유효한 프레임 또는 그냥 첫 프레임 확인
                        val firstValidFrame = response.frames.firstOrNull { it.keypoints.isNotEmpty() }
                            ?: response.frames.firstOrNull()

                        if (firstValidFrame != null) {
                            if (firstValidFrame.keypoints.isEmpty()) {
                                logText += "[주의] 선택된 프레임의 Keypoints가 비어있습니다. 오버레이가 그려지지 않을 수 있습니다.\n"
                            }

                            testKeyPoints = DataConverter.convertToKeyPoints(firstValidFrame)
                            testErrors = firstValidFrame.errors

                            if (testKeyPoints.isNotEmpty()) {
                                logText += "[성공] 오버레이 데이터 변환 완료 (${testKeyPoints.size} 관절)\n"
                            } else {
                                logText += "[정보] 변환된 관절 데이터가 0개입니다 (Keypoints가 비어있음).\n"
                            }

                            isParsingSuccess = true
                            logText += "\n[완료] 파싱 완료! [Step 2] 버튼을 누르세요."
                        } else {
                            logText += "[실패] 프레임 데이터가 없습니다.\n"
                        }

                    } catch (e: Exception) {
                        logText += "[에러] 에러 발생: ${e.message}\n"
                        e.printStackTrace()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
        ) {
            Text("[Step 1] 랜덤 파일 로드 & 파싱")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Step 2: 오버레이 시각화 ---
        Button(
            onClick = {
                if (isParsingSuccess) {
                    showOverlay = true
                    logText += "\n> [Step 2] 오버레이 시각화 시도"
                }
            },
            enabled = isParsingSuccess,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF03DAC5),
                disabledContainerColor = Color.Gray
            )
        ) {
            Text("[Step 2] 오버레이 시각화 확인")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 로그 출력 영역 ---
        Text("실행 로그:", fontWeight = FontWeight.Bold)
        SelectionContainer {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp) // 로그창 높이 약간 증가
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = logText, fontSize = 13.sp, color = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- 오버레이 미리보기 영역 ---
        Text("오버레이 화면:", fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
        ) {
            if (showOverlay) {
                if (testKeyPoints.isNotEmpty()) {
                    SkeletonOverlay(
                        keyPoints = testKeyPoints,
                        errors = testErrors,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = "[주의] 표시할 관절 데이터가 없습니다.\n(JSON의 keypoints 필드가 비어있음)",
                        color = Color.Yellow,
                        modifier = Modifier.align(Alignment.Center),
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = if (isParsingSuccess) "터치하여 오버레이 확인" else "데이터 준비 중...",
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}