package com.example.kpopdancepracticeai.data

import com.example.kpopdancepracticeai.data.entity.Song
import com.example.kpopdancepracticeai.data.entity.SongPart

object RealDataSource {
    private const val EXPERT_VIDEO_BASE_PATH =
        "file:///data/data/com.example.kpopdancepracticeai/files/expert_videos/"

    private fun expertVideo(fileName: String): String = "$EXPERT_VIDEO_BASE_PATH$fileName"

    val getRealSongs = listOf(
        Song(540L, "일레븐", "ELEVEN", "아이브 (원영)", "IVE (Wonyoung)", "Female", "Dance", "Medium", "보통", "https://via.placeholder.com/400x300.png?text=Wonyoung+Cover", "2021-12-01"),
        Song(541L, "일레븐", "ELEVEN", "아이브 (리즈)", "IVE (Liz)", "Female", "Dance", "Medium", "보통", "https://via.placeholder.com/400x300.png?text=Liz+Cover", "2021-12-01"),
        Song(542L, "일레븐", "ELEVEN", "아이브 (이서)", "IVE (Leeseo)", "Female", "Dance", "Medium", "쉬움", "https://via.placeholder.com/400x300.png?text=Leeseo+Cover", "2021-12-01"),
        Song(550L, "오오", "O.O", "엔믹스 (설윤)", "NMIXX (Sullyoon)", "Female", "Dance", "Fast", "어려움", "https://via.placeholder.com/400x300.png?text=Sullyoon+Cover", "2022-02-22"),
        Song(551L, "오오", "O.O", "엔믹스 (해원)", "NMIXX (Haewon)", "Female", "Dance", "Fast", "어려움", "https://via.placeholder.com/400x300.png?text=Haewon+Cover", "2022-02-22"),
        Song(450L, "워너비", "WANNABE", "있지 (류진)", "ITZY (Ryujin)", "Female", "Dance", "Fast", "어려움", "https://via.placeholder.com/400x300.png?text=Ryujin+Cover", "2020-03-09"),
        Song(451L, "워너비", "WANNABE", "있지 (리아)", "ITZY (Lia)", "Female", "Dance", "Fast", "보통", "https://via.placeholder.com/400x300.png?text=Lia+Cover", "2020-03-09"),
        Song(452L, "워너비", "WANNABE", "있지 (채령)", "ITZY (Chaeryeong)", "Female", "Dance", "Fast", "어려움", "https://via.placeholder.com/400x300.png?text=Chaeryeong+Cover", "2020-03-09"),
        Song(530L, "로코", "LOCO", "있지 (리아)", "ITZY (Lia)", "Female", "Dance", "Fast", "보통", "https://via.placeholder.com/400x300.png?text=Lia+Loco+Cover", "2021-09-24"),
        Song(531L, "로코", "LOCO", "있지 (채령)", "ITZY (Chaeryeong)", "Female", "Dance", "Fast", "어려움", "https://via.placeholder.com/400x300.png?text=Chaeryeong+Loco+Cover", "2021-09-24"),
        Song(556L, "러브다이브", "LOVE DIVE", "아이브 (원영)", "IVE (Wonyoung)", "Female", "Dance", "Medium", "보통", "https://via.placeholder.com/400x300.png?text=Wonyoung+LoveDive", "2022-04-05"),
        Song(557L, "러브다이브", "LOVE DIVE", "아이브 (이서)", "IVE (Leeseo)", "Female", "Dance", "Medium", "보통", "https://via.placeholder.com/400x300.png?text=Leeseo+LoveDive", "2022-04-05")
    )

    val getRealSongParts = listOf(
        SongPart(5401L, 540L, 1, "1절 코러스 (도입부)", 60, expertVideo("540_원영_1.mp4"), null, 0L, 60000L),
        SongPart(5402L, 540L, 2, "2절 코러스 (프리코러스)", 60, expertVideo("540_원영_2.mp4"), null, 0L, 60000L),
        SongPart(5403L, 540L, 3, "3절 코러스", 60, expertVideo("540_원영_3.mp4"), null, 0L, 60000L),
        SongPart(5404L, 540L, 4, "댄스 브레이크", 45, expertVideo("540_원영_4.mp4"), null, 0L, 45000L),

        SongPart(5411L, 541L, 1, "1절 코러스 (도입부)", 60, expertVideo("540_리즈_1.mp4"), null, 0L, 60000L),
        SongPart(5412L, 541L, 2, "2절 코러스 (프리코러스)", 60, expertVideo("540_리즈_2.mp4"), null, 0L, 60000L),
        SongPart(5413L, 541L, 3, "3절 코러스", 60, expertVideo("540_리즈_3.mp4"), null, 0L, 60000L),
        SongPart(5414L, 541L, 4, "댄스 브레이크", 45, expertVideo("540_리즈_4.mp4"), null, 0L, 45000L),

        SongPart(5421L, 542L, 1, "1절 코러스 (도입부)", 60, expertVideo("540_이서_1.mp4"), null, 0L, 60000L),
        SongPart(5422L, 542L, 2, "2절 코러스 (프리코러스)", 60, expertVideo("540_이서_2.mp4"), null, 0L, 60000L),
        SongPart(5423L, 542L, 3, "3절 코러스", 60, expertVideo("540_이서_3.mp4"), null, 0L, 60000L),
        SongPart(5424L, 542L, 4, "아웃트로", 30, expertVideo("540_이서_4.mp4"), null, 0L, 30000L),

        SongPart(5501L, 550L, 1, "도입부", 45, expertVideo("550_설윤_1.mp4"), null, 0L, 45000L),
        SongPart(5502L, 550L, 2, "프리코러스", 45, expertVideo("550_설윤_2.mp4"), null, 0L, 45000L),
        SongPart(5503L, 550L, 3, "코러스", 60, expertVideo("550_설윤_3.mp4"), null, 0L, 60000L),
        SongPart(5504L, 550L, 4, "댄스 브레이크", 40, expertVideo("550_설윤_4.mp4"), null, 0L, 40000L),

        SongPart(5511L, 551L, 1, "도입부", 45, expertVideo("550_해원_1.mp4"), null, 0L, 45000L),
        SongPart(5512L, 551L, 2, "프리코러스", 45, expertVideo("550_해원_2.mp4"), null, 0L, 45000L),
        SongPart(5513L, 551L, 3, "코러스", 60, expertVideo("550_해원_3.mp4"), null, 0L, 60000L),
        SongPart(5514L, 551L, 4, "댄스 브레이크", 40, expertVideo("550_해원_4.mp4"), null, 0L, 40000L),

        SongPart(4501L, 450L, 1, "어깨춤 도입부", 50, expertVideo("450_류진_1.mp4"), null, 0L, 50000L),
        SongPart(4502L, 450L, 2, "프리코러스", 45, expertVideo("450_류진_2.mp4"), null, 0L, 45000L),
        SongPart(4503L, 450L, 3, "코러스", 60, expertVideo("450_류진_3.mp4"), null, 0L, 60000L),
        SongPart(4504L, 450L, 4, "댄스 브레이크", 45, expertVideo("450_류진_4.mp4"), null, 0L, 45000L),

        SongPart(4511L, 451L, 1, "도입부", 50, expertVideo("450_리아_1.mp4"), null, 0L, 50000L),
        SongPart(4512L, 451L, 2, "프리코러스", 45, expertVideo("450_리아_2.mp4"), null, 0L, 45000L),
        SongPart(4513L, 451L, 3, "코러스", 60, expertVideo("450_리아_3.mp4"), null, 0L, 60000L),
        SongPart(4514L, 451L, 4, "댄스 브레이크", 45, expertVideo("450_리아_4.mp4"), null, 0L, 45000L),

        SongPart(4521L, 452L, 1, "도입부", 50, expertVideo("450_채령_1.mp4"), null, 0L, 50000L),
        SongPart(4522L, 452L, 2, "프리코러스", 45, expertVideo("450_채령_2.mp4"), null, 0L, 45000L),
        SongPart(4523L, 452L, 3, "코러스", 60, expertVideo("450_채령_3.mp4"), null, 0L, 60000L),
        SongPart(4524L, 452L, 4, "댄스 브레이크", 45, expertVideo("450_채령_4.mp4"), null, 0L, 45000L),

        SongPart(5301L, 530L, 1, "도입부", 45, expertVideo("530_리아_1.mp4"), null, 0L, 45000L),
        SongPart(5302L, 530L, 2, "프리코러스", 50, expertVideo("530_리아_2.mp4"), null, 0L, 50000L),
        SongPart(5303L, 530L, 3, "코러스", 60, expertVideo("530_리아_3.mp4"), null, 0L, 60000L),
        SongPart(5304L, 530L, 4, "아웃트로", 35, expertVideo("530_리아_4.mp4"), null, 0L, 35000L),

        SongPart(5311L, 531L, 1, "도입부", 45, expertVideo("530_채령_1.mp4"), null, 0L, 45000L),
        SongPart(5312L, 531L, 2, "프리코러스", 50, expertVideo("530_채령_2.mp4"), null, 0L, 50000L),
        SongPart(5313L, 531L, 3, "코러스", 60, expertVideo("530_채령_3.mp4"), null, 0L, 60000L),
        SongPart(5314L, 531L, 4, "아웃트로", 35, expertVideo("530_채령_4.mp4"), null, 0L, 35000L),

        SongPart(5561L, 556L, 1, "도입부", 50, expertVideo("556_원영_1.mp4"), null, 0L, 50000L),
        SongPart(5562L, 556L, 2, "프리코러스", 45, expertVideo("556_원영_2.mp4"), null, 0L, 45000L),
        SongPart(5563L, 556L, 3, "코러스", 60, expertVideo("556_원영_3.mp4"), null, 0L, 60000L),
        SongPart(5564L, 556L, 4, "댄스 브레이크", 40, expertVideo("556_원영_4.mp4"), null, 0L, 40000L),

        SongPart(5571L, 557L, 1, "도입부", 50, expertVideo("556_이서_1.mp4"), null, 0L, 50000L),
        SongPart(5572L, 557L, 2, "프리코러스", 45, expertVideo("556_이서_2.mp4"), null, 0L, 45000L),
        SongPart(5573L, 557L, 3, "코러스", 60, expertVideo("556_이서_3.mp4"), null, 0L, 60000L),
        SongPart(5574L, 557L, 4, "댄스 브레이크", 40, expertVideo("556_이서_4.mp4"), null, 0L, 40000L)
    )
}
