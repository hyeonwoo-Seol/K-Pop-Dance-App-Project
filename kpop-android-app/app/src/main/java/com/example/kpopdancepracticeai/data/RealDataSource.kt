package com.example.kpopdancepracticeai.data

import com.example.kpopdancepracticeai.data.entity.Song
import com.example.kpopdancepracticeai.data.entity.SongPart

object RealDataSource {
    // 1. IVE - ELEVEN 곡 정보로 변경
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
            // 이미지 파일명을 cover_eleven으로 변경
            coverUrl = "android.resource://com.example.kpopdancepracticeai/drawable/cover_eleven",
            releaseDate = "2021-12-01" // 발매일 수정
        )
    )

    // 2. ELEVEN에 해당하는 파트(영상) 정보로 변경
    val getRealSongParts = listOf(
        SongPart(
            partId = 1L,
            songId = 1L,
            partNumber = 1,
            partName = "1절 코러스 (거울모드)",
            durationSec = 60,
            // 영상 파일명을 eleven_practice.mp4로 변경
            videoUrl = "asset:///eleven_practice.mp4",
            skeletonUrl = null,
            startTimeMs = 0L,
            endTimeMs = 60000L
        )
    )
}