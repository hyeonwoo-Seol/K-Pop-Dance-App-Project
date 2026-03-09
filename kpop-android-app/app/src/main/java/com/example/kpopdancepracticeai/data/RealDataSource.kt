package com.example.kpopdancepracticeai.data

import com.example.kpopdancepracticeai.data.entity.Song
import com.example.kpopdancepracticeai.data.entity.SongPart

object RealDataSource {
    // 1. IVE - ELEVEN 곡 정보
    val getRealSongs = listOf(
        Song(
            songId = 1L,
            titleKr = "ELEVEN",
            titleEn = "ELEVEN",
            artistKr = "아이브",
            artistEn = "IVE",
            artistGender = "Female",
            genre = "Dance",
            tempo = "Medium",
            difficulty = "보통",
            coverUrl = "android.resource://com.example.kpopdancepracticeai/drawable/cover_eleven",
            releaseDate = "2021-12-01"
        )
    )

    // 2. ELEVEN에 해당하는 4개의 파트 영상 정보 (실제 파일명 적용)
    val getRealSongParts = listOf(
        // 첫 번째 파트
        SongPart(
            partId = 1L,
            songId = 1L,
            partNumber = 1,
            partName = "1절 코러스",
            durationSec = 60,
            videoUrl = "asset:///540_원영_1.mp4",
            skeletonUrl = null,
            startTimeMs = 0L,
            endTimeMs = 60000L
        ),
        // 두 번째 파트
        SongPart(
            partId = 2L,
            songId = 1L,
            partNumber = 2,
            partName = "2절 코러스",
            durationSec = 60,
            videoUrl = "asset:///540_원영_2.mp4",
            skeletonUrl = null,
            startTimeMs = 0L,
            endTimeMs = 60000L
        ),
        // 세 번째 파트
        SongPart(
            partId = 3L,
            songId = 1L,
            partNumber = 3,
            partName = "댄스 브레이크",
            durationSec = 45,
            videoUrl = "asset:///540_원영_3.mp4",
            skeletonUrl = null,
            startTimeMs = 0L,
            endTimeMs = 45000L
        ),
        // 네 번째 파트
        SongPart(
            partId = 4L,
            songId = 1L,
            partNumber = 4,
            partName = "아웃트로",
            durationSec = 30,
            videoUrl = "asset:///540_원영_4.mp4",
            skeletonUrl = null,
            startTimeMs = 0L,
            endTimeMs = 30000L
        )
    )
}