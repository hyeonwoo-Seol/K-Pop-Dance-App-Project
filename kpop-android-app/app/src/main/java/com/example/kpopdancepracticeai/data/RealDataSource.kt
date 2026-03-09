package com.example.kpopdancepracticeai.data

import com.example.kpopdancepracticeai.data.entity.Song
import com.example.kpopdancepracticeai.data.entity.SongPart

object RealDataSource {
    private const val EXPERT_VIDEO_BASE_PATH =
        "file:///data/data/com.example.kpopdancepracticeai/files/expert_videos"

    private fun expertVideoPath(fileName: String): String = "$EXPERT_VIDEO_BASE_PATH/$fileName"

    // 홈 화면 및 곡 선택 화면에서 사용할 실제 곡 메타데이터
    val getRealSongs = listOf(
        Song(
            songId = 540L,
            titleKr = "일레븐",
            titleEn = "ELEVEN",
            artistKr = "아이브 (원영)",
            artistEn = "IVE (Wonyoung)",
            artistGender = "Female",
            genre = "Dance",
            tempo = "Medium",
            difficulty = "보통",
            coverUrl = "android.resource://com.example.kpopdancepracticeai/drawable/cover_eleven",
            releaseDate = "2021-12-01"
        ),
        Song(
            songId = 541L,
            titleKr = "일레븐",
            titleEn = "ELEVEN",
            artistKr = "아이브 (리즈)",
            artistEn = "IVE (Liz)",
            artistGender = "Female",
            genre = "Dance",
            tempo = "Medium",
            difficulty = "보통",
            coverUrl = "android.resource://com.example.kpopdancepracticeai/drawable/cover_eleven",
            releaseDate = "2021-12-01"
        )
    )

    // 전문가 영상 파일명은 /data/data/com.example.kpopdancepracticeai/files/expert_videos 기준
    val getRealSongParts = listOf(
        SongPart(
            partId = 5401L,
            songId = 540L,
            partNumber = 1,
            partName = "1절 코러스 (도입부)",
            durationSec = 60,
            videoUrl = expertVideoPath("540_원영_1.mp4"),
            skeletonUrl = null,
            startTimeMs = 0L,
            endTimeMs = 60000L
        ),
        SongPart(
            partId = 5402L,
            songId = 540L,
            partNumber = 2,
            partName = "2절 코러스 (프리코러스)",
            durationSec = 60,
            videoUrl = expertVideoPath("540_원영_2.mp4"),
            skeletonUrl = null,
            startTimeMs = 0L,
            endTimeMs = 60000L
        ),
        SongPart(
            partId = 5403L,
            songId = 540L,
            partNumber = 3,
            partName = "3절 코러스",
            durationSec = 60,
            videoUrl = expertVideoPath("540_원영_3.mp4"),
            skeletonUrl = null,
            startTimeMs = 0L,
            endTimeMs = 60000L
        ),
        SongPart(
            partId = 5404L,
            songId = 540L,
            partNumber = 4,
            partName = "댄스 브레이크",
            durationSec = 45,
            videoUrl = expertVideoPath("540_원영_4.mp4"),
            skeletonUrl = null,
            startTimeMs = 0L,
            endTimeMs = 45000L
        ),
        SongPart(
            partId = 5411L,
            songId = 541L,
            partNumber = 1,
            partName = "1절 코러스 (도입부)",
            durationSec = 60,
            videoUrl = expertVideoPath("540_리즈_1.mp4"),
            skeletonUrl = null,
            startTimeMs = 0L,
            endTimeMs = 60000L
        ),
        SongPart(
            partId = 5412L,
            songId = 541L,
            partNumber = 2,
            partName = "2절 코러스 (프리코러스)",
            durationSec = 60,
            videoUrl = expertVideoPath("540_리즈_2.mp4"),
            skeletonUrl = null,
            startTimeMs = 0L,
            endTimeMs = 60000L
        ),
        SongPart(
            partId = 5413L,
            songId = 541L,
            partNumber = 3,
            partName = "3절 코러스",
            durationSec = 60,
            videoUrl = expertVideoPath("540_리즈_3.mp4"),
            skeletonUrl = null,
            startTimeMs = 0L,
            endTimeMs = 60000L
        ),
        SongPart(
            partId = 5414L,
            songId = 541L,
            partNumber = 4,
            partName = "댄스 브레이크",
            durationSec = 45,
            videoUrl = expertVideoPath("540_리즈_4.mp4"),
            skeletonUrl = null,
            startTimeMs = 0L,
            endTimeMs = 45000L
        )
    )
}
