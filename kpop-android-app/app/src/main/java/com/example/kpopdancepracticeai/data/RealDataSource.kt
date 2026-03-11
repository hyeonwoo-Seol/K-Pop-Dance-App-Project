package com.example.kpopdancepracticeai.data

import com.example.kpopdancepracticeai.data.entity.Song
import com.example.kpopdancepracticeai.data.entity.SongPart
import com.example.kpopdancepracticeai.data.entity.Achievement
import com.example.kpopdancepracticeai.data.entity.Badge
import com.example.kpopdancepracticeai.data.entity.LightStick
import com.example.kpopdancepracticeai.data.entity.UserAchievementProgress

object RealDataSource {
    private const val EXPERT_VIDEO_BASE_PATH =
        "file:///data/data/com.example.kpopdancepracticeai/files/expert_videos/"
    private const val APP_PACKAGE_NAME = "com.example.kpopdancepracticeai"

    private fun expertVideo(fileName: String): String = "$EXPERT_VIDEO_BASE_PATH$fileName"
    private fun drawableCover(fileName: String): String =
        "android.resource://$APP_PACKAGE_NAME/drawable/$fileName"

    private fun coverForSongTitle(titleEn: String): String = when (titleEn.lowercase()) {
        "eleven" -> drawableCover("cover_eleven")
        "o.o" -> drawableCover("cover_oo")
        "wannabe" -> drawableCover("cover_wannabe")
        "loco" -> drawableCover("cover_loco")
        "love dive" -> drawableCover("cover_lovedive")
        "stay this way" -> drawableCover("cover_staythisway")
        "sneakers" -> drawableCover("cover_sneakers")
        else -> drawableCover("cover_eleven")
    }

    val getRealSongs = listOf(
        Song(540L, "일레븐", "ELEVEN", "아이브 (원영)", "IVE (Wonyoung)", "Female", "Dance", "Medium", "보통", coverForSongTitle("ELEVEN"), "2021-12-01"),
        Song(541L, "일레븐", "ELEVEN", "아이브 (리즈)", "IVE (Liz)", "Female", "Dance", "Medium", "보통", coverForSongTitle("ELEVEN"), "2021-12-01"),
        Song(542L, "일레븐", "ELEVEN", "아이브 (이서)", "IVE (Leeseo)", "Female", "Dance", "Medium", "쉬움", coverForSongTitle("ELEVEN"), "2021-12-01"),
        Song(550L, "오오", "O.O", "엔믹스 (설윤)", "NMIXX (Sullyoon)", "Female", "Dance", "Fast", "어려움", coverForSongTitle("O.O"), "2022-02-22"),
        Song(551L, "오오", "O.O", "엔믹스 (해원)", "NMIXX (Haewon)", "Female", "Dance", "Fast", "어려움", coverForSongTitle("O.O"), "2022-02-22"),
        Song(450L, "워너비", "WANNABE", "있지 (류진)", "ITZY (Ryujin)", "Female", "Dance", "Fast", "어려움", coverForSongTitle("WANNABE"), "2020-03-09"),
        Song(451L, "워너비", "WANNABE", "있지 (리아)", "ITZY (Lia)", "Female", "Dance", "Fast", "보통", coverForSongTitle("WANNABE"), "2020-03-09"),
        Song(452L, "워너비", "WANNABE", "있지 (채령)", "ITZY (Chaeryeong)", "Female", "Dance", "Fast", "어려움", coverForSongTitle("WANNABE"), "2020-03-09"),
        Song(530L, "로코", "LOCO", "있지 (리아)", "ITZY (Lia)", "Female", "Dance", "Fast", "보통", coverForSongTitle("LOCO"), "2021-09-24"),
        Song(531L, "로코", "LOCO", "있지 (채령)", "ITZY (Chaeryeong)", "Female", "Dance", "Fast", "어려움", coverForSongTitle("LOCO"), "2021-09-24"),
        Song(556L, "러브다이브", "LOVE DIVE", "아이브 (원영)", "IVE (Wonyoung)", "Female", "Dance", "Medium", "보통", coverForSongTitle("LOVE DIVE"), "2022-04-05"),
        Song(557L, "러브다이브", "LOVE DIVE", "아이브 (이서)", "IVE (Leeseo)", "Female", "Dance", "Medium", "보통", coverForSongTitle("LOVE DIVE"), "2022-04-05"),
        Song(568L, "스테이디스웨이", "Stay This Way", "프로미스나인 (하영)", "fromis_9 (Hayoung)", "Female", "Dance", "Medium", "보통", coverForSongTitle("Stay This Way"), "2022-06-27"),
        Song(569L, "스테이디스웨이", "Stay This Way", "프로미스나인 (지헌)", "fromis_9 (Jiheon)", "Female", "Dance", "Medium", "보통", coverForSongTitle("Stay This Way"), "2022-06-27"),
        Song(571L, "스니커즈", "SNEAKERS", "있지 (채령)", "ITZY (Chaeryeong)", "Female", "Dance", "Fast", "어려움", coverForSongTitle("SNEAKERS"), "2022-07-15"),
        Song(572L, "스니커즈", "SNEAKERS", "있지 (예지)", "ITZY (Yeji)", "Female", "Dance", "Fast", "어려움", coverForSongTitle("SNEAKERS"), "2022-07-15"),
        Song(573L, "스니커즈", "SNEAKERS", "있지 (유나)", "ITZY (Yuna)", "Female", "Dance", "Fast", "어려움", coverForSongTitle("SNEAKERS"), "2022-07-15")

    )

    val getRealSongParts = listOf(
        SongPart(5401L, 540L, 1, "파트1", 42, expertVideo("540_원영_1.mp4"), null, 0L, 42000L),
        SongPart(5402L, 540L, 2, "파트2", 35, expertVideo("540_원영_2.mp4"), null, 0L, 35000L),
        SongPart(5403L, 540L, 3, "파트3", 49, expertVideo("540_원영_3.mp4"), null, 0L, 49000L),
        SongPart(5404L, 540L, 4, "파트4", 50, expertVideo("540_원영_4.mp4"), null, 0L, 50000L),

        SongPart(5411L, 541L, 1, "파트1", 59, expertVideo("540_리즈_1.mp4"), null, 0L, 59000L),
        SongPart(5412L, 541L, 2, "파트2", 35, expertVideo("540_리즈_2.mp4"), null, 0L, 35000L),
        SongPart(5413L, 541L, 3, "파트3", 29, expertVideo("540_리즈_3.mp4"), null, 0L, 29000L),
        SongPart(5414L, 541L, 4, "파트4", 50, expertVideo("540_리즈_4.mp4"), null, 0L, 50000L),

        SongPart(5421L, 542L, 1, "파트1", 39, expertVideo("540_이서_1.mp4"), null, 0L, 39000L),
        SongPart(5422L, 542L, 2, "파트2", 35, expertVideo("540_이서_2.mp4"), null, 0L, 35000L),
        SongPart(5423L, 542L, 3, "파트3", 49, expertVideo("540_이서_3.mp4"), null, 0L, 49000L),
        SongPart(5424L, 542L, 4, "파트4", 49, expertVideo("540_이서_4.mp4"), null, 0L, 49000L),

        SongPart(5501L, 550L, 1, "파트1", 45, expertVideo("550_설윤_1.mp4"), null, 0L, 45000L),
        SongPart(5502L, 550L, 2, "파트2", 39, expertVideo("550_설윤_2.mp4"), null, 0L, 39000L),
        SongPart(5503L, 550L, 3, "파트3", 32, expertVideo("550_설윤_3.mp4"), null, 0L, 32000L),
        SongPart(5504L, 550L, 4, "파트4", 54, expertVideo("550_설윤_4.mp4"), null, 0L, 54000L),

        SongPart(5511L, 551L, 1, "파트1", 45, expertVideo("550_해원_1.mp4"), null, 0L, 45000L),
        SongPart(5512L, 551L, 2, "파트2", 39, expertVideo("550_해원_2.mp4"), null, 0L, 39000L),
        SongPart(5513L, 551L, 3, "파트3", 39, expertVideo("550_해원_3.mp4"), null, 0L, 39000L),
        SongPart(5514L, 551L, 4, "파트4", 47, expertVideo("550_해원_4.mp4"), null, 0L, 47000L),

        SongPart(4501L, 450L, 1, "파트1", 39, expertVideo("450_류진_1.mp4"), null, 0L, 39000L),
        SongPart(4502L, 450L, 2, "파트2", 32, expertVideo("450_류진_2.mp4"), null, 0L, 32000L),
        SongPart(4503L, 450L, 3, "파트3", 62, expertVideo("450_류진_3.mp4"), null, 0L, 62000L),
        SongPart(4504L, 450L, 4, "파트4", 46, expertVideo("450_류진_4.mp4"), null, 0L, 46000L),

        SongPart(4511L, 451L, 1, "파트1", 49, expertVideo("450_리아_1.mp4"), null, 0L, 49000L),
        SongPart(4512L, 451L, 2, "파트2", 32, expertVideo("450_리아_2.mp4"), null, 0L, 32000L),
        SongPart(4513L, 451L, 3, "파트3", 62, expertVideo("450_리아_3.mp4"), null, 0L, 62000L),
        SongPart(4514L, 451L, 4, "파트4", 46, expertVideo("450_리아_4.mp4"), null, 0L, 46000L),

        SongPart(4521L, 452L, 1, "파트1", 39, expertVideo("450_채령_1.mp4"), null, 0L, 39000L),
        SongPart(4522L, 452L, 2, "파트2", 32, expertVideo("450_채령_2.mp4"), null, 0L, 32000L),
        SongPart(4523L, 452L, 3, "파트3", 62, expertVideo("450_채령_3.mp4"), null, 0L, 62000L),
        SongPart(4524L, 452L, 4, "파트4", 46, expertVideo("450_채령_4.mp4"), null, 0L, 46000L),

        SongPart(5301L, 530L, 1, "파트1", 49, expertVideo("530_리아_1.mp4"), null, 0L, 49000L),
        SongPart(5302L, 530L, 2, "파트2", 29, expertVideo("530_리아_2.mp4"), null, 0L, 29000L),
        SongPart(5303L, 530L, 3, "파트3", 29, expertVideo("530_리아_3.mp4"), null, 0L, 29000L),
        SongPart(5304L, 530L, 4, "파트4", 39, expertVideo("530_리아_4.mp4"), null, 0L, 39000L),

        SongPart(5311L, 531L, 1, "파트1", 49, expertVideo("530_채령_1.mp4"), null, 0L, 49000L),
        SongPart(5312L, 531L, 2, "파트2", 29, expertVideo("530_채령_2.mp4"), null, 0L, 29000L),
        SongPart(5313L, 531L, 3, "파트3", 29, expertVideo("530_채령_3.mp4"), null, 0L, 29000L),
        SongPart(5314L, 531L, 4, "파트4", 39, expertVideo("530_채령_4.mp4"), null, 0L, 39000L),

        SongPart(5561L, 556L, 1, "파트1", 40, expertVideo("556_원영_1.mp4"), null, 0L, 40000L),
        SongPart(5562L, 556L, 2, "파트2", 33, expertVideo("556_원영_2.mp4"), null, 0L, 33000L),
        SongPart(5563L, 556L, 3, "파트3", 49, expertVideo("556_원영_3.mp4"), null, 0L, 49000L),
        SongPart(5564L, 556L, 4, "파트4", 50, expertVideo("556_원영_4.mp4"), null, 0L, 50000L),

        SongPart(5571L, 557L, 1, "파트1", 25, expertVideo("556_이서_1.mp4"), null, 0L, 25000L),
        SongPart(5572L, 557L, 2, "파트2", 27, expertVideo("556_이서_2.mp4"), null, 0L, 27000L),
        SongPart(5573L, 557L, 3, "파트3", 30, expertVideo("556_이서_3.mp4"), null, 0L, 30000L),
        SongPart(5574L, 557L, 4, "파트4", 37, expertVideo("556_이서_4.mp4"), null, 0L, 37000L),

        SongPart(5681L, 568L, 1, "파트1", 18, expertVideo("568_하영_1.mp4"), null, 0L, 18000L),
        SongPart(5682L, 568L, 2, "파트2", 48, expertVideo("568_하영_2.mp4"), null, 0L, 48000L),
        SongPart(5683L, 568L, 3, "파트3", 40, expertVideo("568_하영_3.mp4"), null, 0L, 40000L),
        SongPart(5684L, 568L, 4, "파트4", 49, expertVideo("568_하영_4.mp4"), null, 0L, 49000L),

        SongPart(5691L, 569L, 1, "파트1", 32, expertVideo("568_지헌_1.mp4"), null, 0L, 32000L),
        SongPart(5692L, 569L, 2, "파트2", 40, expertVideo("568_지헌_2.mp4"), null, 0L, 40000L),
        SongPart(5693L, 569L, 3, "파트3", 54, expertVideo("568_지헌_3.mp4"), null, 0L, 54000L),
        SongPart(5694L, 569L, 4, "파트4", 49, expertVideo("568_지헌_4.mp4"), null, 0L, 49000L),

        SongPart(5711L, 571L, 1, "파트1", 47, expertVideo("571_채령_1.mp4"), null, 0L, 47000L),
        SongPart(5712L, 571L, 2, "파트2", 48, expertVideo("571_채령_2.mp4"), null, 0L, 48000L),
        SongPart(5713L, 571L, 3, "파트3", 44, expertVideo("571_채령_3.mp4"), null, 0L, 44000L),
        SongPart(5714L, 571L, 4, "파트4", 37, expertVideo("571_채령_4.mp4"), null, 0L, 37000L),

        SongPart(5721L, 572L, 1, "파트1", 47, expertVideo("571_예지_1.mp4"), null, 0L, 47000L),
        SongPart(5722L, 572L, 2, "파트2", 48, expertVideo("571_예지_2.mp4"), null, 0L, 48000L),
        SongPart(5723L, 572L, 3, "파트3", 44, expertVideo("571_예지_3.mp4"), null, 0L, 44000L),
        SongPart(5724L, 572L, 4, "파트4", 37, expertVideo("571_예지_4.mp4"), null, 0L, 37000L),

        SongPart(5731L, 573L, 1, "파트1", 47, expertVideo("571_유나_1.mp4"), null, 0L, 47000L),
        SongPart(5732L, 573L, 2, "파트2", 48, expertVideo("571_유나_2.mp4"), null, 0L, 48000L),
        SongPart(5733L, 573L, 3, "파트3", 44, expertVideo("571_유나_3.mp4"), null, 0L, 44000L),
        SongPart(5734L, 573L, 4, "파트4", 37, expertVideo("571_유나_4.mp4"), null, 0L, 37000L)



    )

    val getRealAchievements = listOf(
        Achievement(
            id = "itzy_complete_01",
            title = "ITZY 첫 시작",
            description = "ITZY 노래 파트 하나 완료하기",
            goalCount = 1,
            currentCount = 0,
            isUnlocked = false,
            isCompleted = false,
            achievedAt = null,
            rewardType = "badge",
            rewardId = "badge_itzy_complete_01"
        ),
        Achievement(
            id = "ive_complete_01",
            title = "IVE 첫 시작",
            description = "IVE 노래 파트 하나 완료하기",
            goalCount = 1,
            currentCount = 0,
            isUnlocked = false,
            isCompleted = false,
            achievedAt = null,
            rewardType = "badge",
            rewardId = "badge_ive_complete_01"
        ),
        Achievement(
            id = "nmixx_complete_01",
            title = "NMIXX 첫 시작",
            description = "NMIXX 노래 파트 하나 완료하기",
            goalCount = 1,
            currentCount = 0,
            isUnlocked = false,
            isCompleted = false,
            achievedAt = null,
            rewardType = "badge",
            rewardId = "badge_nmixx_complete_01"
        ),
        Achievement(
            id = "fromis9_complete_01",
            title = "프로미스나인 첫 시작",
            description = "프로미스나인 노래 파트 하나 완료하기",
            goalCount = 1,
            currentCount = 0,
            isUnlocked = false,
            isCompleted = false,
            achievedAt = null,
            rewardType = "badge",
            rewardId = "badge_fromis9_complete_01"
        ),
        Achievement(
            id = "straykids_complete_01",
            title = "스트레이키즈 첫 시작",
            description = "스트레이키즈 노래 파트 하나 완료하기",
            goalCount = 1,
            currentCount = 0,
            isUnlocked = false,
            isCompleted = false,
            achievedAt = null,
            rewardType = "badge",
            rewardId = "badge_straykids_complete_01"
        ),
        Achievement(
            id = "itzy_complete_50",
            title = "ITZY 50회 달성",
            description = "ITZY 노래 파트 50회 완료",
            goalCount = 50,
            currentCount = 0,
            isUnlocked = false,
            isCompleted = false,
            achievedAt = null,
            rewardType = "icon",
            rewardId = "icon_itzy_complete_50"
        ),
        Achievement(
            id = "ive_complete_50",
            title = "IVE 50회 달성",
            description = "IVE 노래 파트 50회 완료",
            goalCount = 50,
            currentCount = 0,
            isUnlocked = false,
            isCompleted = false,
            achievedAt = null,
            rewardType = "icon",
            rewardId = "icon_ive_complete_50"
        ),
        Achievement(
            id = "nmixx_complete_50",
            title = "NMIXX 50회 달성",
            description = "NMIXX 노래 파트 50회 완료",
            goalCount = 50,
            currentCount = 0,
            isUnlocked = false,
            isCompleted = false,
            achievedAt = null,
            rewardType = "icon",
            rewardId = "icon_nmixx_complete_50"
        ),
        Achievement(
            id = "fromis9_complete_50",
            title = "프로미스나인 50회 달성",
            description = "프로미스나인 노래 파트 50회 완료",
            goalCount = 50,
            currentCount = 0,
            isUnlocked = false,
            isCompleted = false,
            achievedAt = null,
            rewardType = "icon",
            rewardId = "icon_fromis9_complete_50"
        ),
        Achievement(
            id = "straykids_complete_50",
            title = "스트레이키즈 50회 달성",
            description = "스트레이키즈 노래 파트 50회 완료",
            goalCount = 50,
            currentCount = 0,
            isUnlocked = false,
            isCompleted = false,
            achievedAt = null,
            rewardType = "icon",
            rewardId = "icon_straykids_complete_50"
        )
    )

    val getRealLightSticks = listOf(
        LightStick(
            id = "icon_itzy_complete_50",
            name = "ITZY 응원봉",
            localImagePath = "",
            artist = "ITZY",
            isOwned = false,
            obtainedAt = null
        ),
        LightStick(
            id = "icon_ive_complete_50",
            name = "IVE 응원봉",
            localImagePath = "",
            artist = "IVE",
            isOwned = false,
            obtainedAt = null
        ),
        LightStick(
            id = "icon_nmixx_complete_50",
            name = "NMIXX 응원봉",
            localImagePath = "",
            artist = "NMIXX",
            isOwned = false,
            obtainedAt = null
        ),
        LightStick(
            id = "icon_fromis9_complete_50",
            name = "프로미스나인 응원봉",
            localImagePath = "",
            artist = "프로미스나인",
            isOwned = false,
            obtainedAt = null
        ),
        LightStick(
            id = "icon_straykids_complete_50",
            name = "스트레이키즈 응원봉",
            localImagePath = "",
            artist = "스트레이키즈",
            isOwned = false,
            obtainedAt = null
        )
    )

    fun getInitialBadges(userId: String): List<Badge> = listOf(
        Badge(
            id = "badge_itzy_complete_01_$userId",
            userUuid = userId,
            name = "ITZY 초보자",
            description = "ITZY 파트 하나라도 풀레이",
            category = "starter",
            isUnlocked = false,
            obtainedAt = null
        ),
        Badge(
            id = "badge_ive_complete_01_$userId",
            userUuid = userId,
            name = "IVE 초보자",
            description = "IVE 파트 하나라도 풀레이",
            category = "starter",
            isUnlocked = false,
            obtainedAt = null
        ),
        Badge(
            id = "badge_nmixx_complete_01_$userId",
            userUuid = userId,
            name = "NMIXX 초보자",
            description = "NMIXX 파트 하나라도 풀레이",
            category = "starter",
            isUnlocked = false,
            obtainedAt = null
        ),
        Badge(
            id = "badge_fromis9_complete_01_$userId",
            userUuid = userId,
            name = "프로미스나인 초보자",
            description = "프로미스나인 파트 하나라도 풀레이",
            category = "starter",
            isUnlocked = false,
            obtainedAt = null
        ),
        Badge(
            id = "badge_straykids_complete_01_$userId",
            userUuid = userId,
            name = "스트레이키즈 초보자",
            description = "스트레이키즈 파트 하나라도 풀레이",
            category = "starter",
            isUnlocked = false,
            obtainedAt = null
        )
    )

    fun getInitialAchievementProgress(userId: String): List<UserAchievementProgress> =
        getRealAchievements.map { achievement ->
            UserAchievementProgress(
                userUuid = userId,
                achievementCode = achievement.id,
                currentStep = achievement.currentCount,
                goalStep = achievement.goalCount,
                isCompleted = achievement.isCompleted,
                achievedDate = null
            )
        }
}
