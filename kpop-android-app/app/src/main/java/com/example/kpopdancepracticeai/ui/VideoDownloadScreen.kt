package com.example.kpopdancepracticeai.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kpopdancepracticeai.util.NetworkUtils
import com.example.kpopdancepracticeai.viewmodel.VideoDownloadViewModel

@Composable
fun VideoDownloadScreen(
    viewModel: VideoDownloadViewModel = viewModel(),
    onDownloadComplete: () -> Unit // 다운로드가 완료되었을 때 홈 화면으로 이동하기 위한 콜백
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 데이터 요금 경고 팝업을 띄울지 결정하는 상태값
    var showDataWarningDialog by remember { mutableStateOf(false) }

    // 다운로드가 완료(isFinished == true)되면 즉시 콜백을 호출하여 화면 전환
    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) {
            onDownloadComplete()
        }
    }

    // 데이터 요금 경고 다이얼로그
    if (showDataWarningDialog) {
        AlertDialog(
            onDismissRequest = { showDataWarningDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = "경고", tint = Color.Red) },
            title = { Text("Wi-Fi 미연결 안내") },
            text = { Text("현재 Wi-Fi에 연결되어 있지 않습니다. 모바일 데이터(LTE/5G)로 다운로드할 경우 가입하신 요금제에 따라 추가 데이터 요금이 발생할 수 있습니다.\n\n계속하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    showDataWarningDialog = false
                    viewModel.startDownload()
                }) {
                    Text("다운로드 계속")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDataWarningDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 상단 아이콘
            Icon(
                imageVector = Icons.Default.CloudDownload,
                contentDescription = "다운로드 아이콘",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 메인 타이틀
            Text(
                text = "초기 설정 진행 중",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 상태에 따른 UI 변경 (에러 / 다운로드 중 / 대기 중)
            when {
                uiState.errorMessage != null -> {
                    // 에러 발생 시
                    Text(
                        text = uiState.errorMessage ?: "알 수 없는 에러",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { viewModel.retryDownload() }) {
                        Text("다시 시도")
                    }
                }
                uiState.isDownloading -> {
                    // 다운로드 진행 중 UI
                    Text(
                        text = "원활한 AI 분석을 위해 전문가의 댄스 영상을 다운로드하고 있습니다. 앱을 끄지 말고 잠시만 기다려주세요.",
                        textAlign = TextAlign.Center,
                        color = Color.Gray,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // 진행률 표시
                    val currentProgressRatio = uiState.currentProgress / 100f
                    LinearProgressIndicator(
                        progress = { currentProgressRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 다운로드 현황 텍스트 (예: 1 / 3 영상 완료 (45%))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${uiState.currentVideoIndex + 1} / ${uiState.totalVideos} 영상",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${uiState.currentProgress}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                else -> {
                    // 다운로드 대기 중 (최초 화면)
                    Text(
                        text = "앱을 이용하기 위해서는 전문가 댄스 영상 다운로드가 필요합니다.\n데이터 요금 절약을 위해 WIFI가 연결된 환경을 권장합니다.",
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    // 버튼 영역
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 다운 받기 버튼
                        Button(
                            onClick = {
                                if (NetworkUtils.isWifiConnected(context)) {
                                    // Wi-Fi 연결 시 바로 다운로드 시작
                                    viewModel.startDownload()
                                } else {
                                    // Wi-Fi 아닐 시 경고 다이얼로그 띄우기
                                    showDataWarningDialog = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("다운로드 시작", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        // 다운로드 건너뛰기 버튼 (테스트 및 나중에 받기)
                        OutlinedButton(
                            onClick = {
                                // Activity를 종료하지 않고, NavController를 통해 홈 화면으로 이동시킵니다.
                                onDownloadComplete()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("나중에 받기 (홈으로 이동)", color = Color.Gray, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}