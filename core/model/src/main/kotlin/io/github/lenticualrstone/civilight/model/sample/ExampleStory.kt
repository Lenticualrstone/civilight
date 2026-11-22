package io.github.lenticualrstone.civilight.model.sample

import io.github.lenticualrstone.civilight.model.ClockTime
import io.github.lenticualrstone.civilight.model.IntSpan
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.sample.ExampleClock.DEVICE
import io.github.lenticualrstone.civilight.model.sample.ExampleClock.LAST_EDIT
import io.github.lenticualrstone.civilight.model.sample.ExampleClock.at
import io.github.lenticualrstone.civilight.model.story.FlashbackMode
import io.github.lenticualrstone.civilight.model.story.FlashbackRelation
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import io.github.lenticualrstone.civilight.model.story.TimePrecision
import io.github.lenticualrstone.civilight.model.story.TransitionSnapshot
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/**
 * `story/index.json` of the example: 1장 `등불지기` (1-1 … 1-10, 31 live scenes) and 2장 `잿불`
 * (2-1 … 2-8, scenes only in 2-3 and 2-5). Scene 때 follow STO Fixture T (S1 … S12).
 */
internal object ExampleStory {
    /** Block counters per episode (`StoryUnit.nextBlockNumber`). */
    val nextBlockNumbers: Map<Pair<Int, Int>, Int> = mapOf(
        (1 to 1) to 12, (1 to 2) to 12, (1 to 3) to 12, (1 to 4) to 10, (1 to 5) to 18,
        (1 to 6) to 33, (1 to 7) to 58, (1 to 8) to 61,
    )

    /** When 1-6 씬 5 went to the trash (`12일 남음` on 2026-11-22). */
    val SCENE_1_6_5_DELETED_AT: Long = at("2026-11-04T01:00:00Z")

    private val SOCIETY_GATE = PlaceRef(PlaceRefKind.PLACE, K.PLACE_SOCIETY_GATE)
    private val LOWER_STREET = PlaceRef(PlaceRefKind.PLACE, K.PLACE_LOWER_STREET)
    private val ASH_MARKET = PlaceRef(PlaceRefKind.PLACE, K.PLACE_ASH_MARKET)
    private val UPPER_GATE = PlaceRef(PlaceRefKind.PLACE, K.PLACE_UPPER_GATE)
    private val PYLON_RUINS = PlaceRef(PlaceRefKind.PLACE, K.PLACE_PYLON_RUINS)
    private val ARCHIVE_ROOM = PlaceRef(PlaceRefKind.PLACE, K.PLACE_ARCHIVE_ROOM)
    private val LOWER_DISTRICT = PlaceRef(PlaceRefKind.REGION, K.REGION_LOWER_DISTRICT)

    private fun rel(base: Key, days: Int, band: Band? = null, clock: ClockTime? = null) =
        SceneTime.Relative(baseKey = base, dayMin = days, dayMax = days, band = band?.open(), clock = clock)

    private class SceneSpec(
        val c: Int,
        val e: Int,
        val s: Int,
        val title: String?,
        val place: PlaceRef? = null,
        val time: SceneTime = SceneTime.None,
        val silentCast: List<Key> = emptyList(),
        val memo: String? = null,
        val transitionMode: TransitionMode? = null,
        val transitionRuns: List<TextRun>? = null,
        val transitionSnapshot: TransitionSnapshot? = null,
        val deletedAt: Long? = null,
        val trashEntryKey: Key? = null,
    )

    private val scenes: List<SceneSpec> = listOf(
        // 1-1
        SceneSpec(1, 1, 1, "첫 순찰", LOWER_STREET, SceneTime.Direct(storyDay = 1)),
        SceneSpec(1, 1, 2, "오웬의 노점", ASH_MARKET, rel(K.SCENE_1_1_1, 0, Band.EVENING)),
        SceneSpec(1, 1, 3, "결사의 문지기", SOCIETY_GATE, rel(K.SCENE_1_1_2, 0, Band.NIGHT)),
        // 1-2
        SceneSpec(1, 2, 1, "새벽 순찰", LOWER_STREET, rel(K.SCENE_1_1_1, 1, Band.DAWN)),
        SceneSpec(1, 2, 2, "고아원의 지붕", LOWER_STREET, silentCast = listOf(K.NOAH)),
        SceneSpec(1, 2, 3, "빛 한 줌", ASH_MARKET),
        // 1-3
        SceneSpec(1, 3, 1, "등불 결사의 소문"),
        SceneSpec(
            1, 3, 2, "결사 합류", SOCIETY_GATE, rel(K.SCENE_1_2_1, 1, Band.MORNING),
            transitionMode = TransitionMode.AUTO,
        ),
        SceneSpec(1, 3, 3, "첫 등불", SOCIETY_GATE),
        // 1-4
        SceneSpec(1, 4, 1, "결사의 아침", SOCIETY_GATE),
        SceneSpec(
            1, 4, 2, "세라의 회상", LOWER_STREET,
            SceneTime.Flashback(
                mode = FlashbackMode.ANCHOR.open(), eventKey = K.EVENT_BLACKOUT, relation = FlashbackRelation.ON.open(),
            ),
            silentCast = listOf(K.RIEN),
        ),
        SceneSpec(1, 4, 3, "기록실의 먼지", ARCHIVE_ROOM, rel(K.SCENE_1_3_2, 1, Band.EVENING)),
        // 1-5
        SceneSpec(
            1, 5, 1, "하층 구역 순찰", LOWER_DISTRICT, rel(K.SCENE_1_4_3, 1, Band.DAWN, ClockTime.of(6, 20)),
            memo = "새벽 표기 그대로 · 시각은 06:20",
        ),
        SceneSpec(1, 5, 2, "두 갈래 길", LOWER_STREET, rel(K.SCENE_1_5_1, 0, Band.MORNING)),
        SceneSpec(1, 5, 3, "등불 수리", null, rel(K.SCENE_1_5_2, 1, Band.DAY), silentCast = listOf(K.NOAH)),
        SceneSpec(1, 5, 4, "기록실의 저녁", ARCHIVE_ROOM, rel(K.SCENE_1_5_3, 0, Band.EVENING)),
        // 1-6
        SceneSpec(
            1, 6, 1, "잿더미 시장", ASH_MARKET,
            SceneTime.Relative(
                baseKey = K.SCENE_1_3_2, dayMin = 5, dayMax = 9, precision = TimePrecision.APPROX.open(), band = Band.DAY.open(),
            ),
        ),
        SceneSpec(1, 6, 2, "노아의 심부름"),
        SceneSpec(1, 6, 3, "시장의 저녁"),
        SceneSpec(1, 6, 4, "오웬의 장부", ASH_MARKET),
        SceneSpec(
            1, 6, 5, "시장의 밤", ASH_MARKET,
            deletedAt = SCENE_1_6_5_DELETED_AT, trashEntryKey = K.TRASH_SCENE_1_6_5,
        ),
        // 1-7
        SceneSpec(1, 7, 1, "첫 번째 깜박임", LOWER_STREET, rel(K.SCENE_1_6_1, 2, Band.DAWN)),
        SceneSpec(1, 7, 2, "상층 관문", UPPER_GATE, rel(K.SCENE_1_7_1, 0, Band.EVENING), silentCast = listOf(K.ELIO)),
        SceneSpec(
            1, 7, 3, "꺼진 등불", SOCIETY_GATE, rel(K.SCENE_1_7_2, 1, clock = ClockTime.of(5, 40)),
            silentCast = listOf(K.MARTHA), memo = "등불이 두 번째로 깜박이는 타이밍",
        ),
        SceneSpec(
            1, 7, 4, null, SOCIETY_GATE, rel(K.SCENE_1_7_3, 0, clock = ClockTime.of(6, 10)),
            transitionMode = TransitionMode.DIRECT,
            transitionRuns = listOf(TextRun.Plain("결사의 문 앞 · 06:10")),
            transitionSnapshot = TransitionSnapshot(place = SOCIETY_GATE, dayRange = IntSpan(11, 15), tRange = IntSpan(130, 130)),
        ),
        // 1-8
        SceneSpec(1, 8, 1, "송전탑 아래", PYLON_RUINS, rel(K.SCENE_1_7_4, 2, Band.DAWN)),
        SceneSpec(
            1, 8, 2, null,
            transitionMode = TransitionMode.DIRECT,
            transitionRuns = listOf(TextRun.Plain("기록실 · 같은 날 밤")),
            transitionSnapshot = TransitionSnapshot(),
        ),
        // 1-9
        SceneSpec(1, 9, 1, null),
        SceneSpec(1, 9, 2, "다시 관문", UPPER_GATE, SceneTime.Direct(storyDay = 20, band = Band.DAY.open())),
        // 1-10
        SceneSpec(1, 10, 1, null, time = rel(K.SCENE_1_9_2, 3, Band.NIGHT)),
        SceneSpec(1, 10, 2, null),
        SceneSpec(1, 10, 3, null),
        // 2장
        SceneSpec(2, 3, 1, null),
        SceneSpec(2, 3, 2, "경비대 검문", LOWER_STREET, silentCast = listOf(K.HART)),
        SceneSpec(2, 5, 1, null),
    )

    private val chapterTitles = mapOf(1 to "등불지기", 2 to "잿불")
    private val chapterSynopses = mapOf(
        1 to "하층 거리의 꼬마 리엔이 등불 결사의 견습이 되고, 꺼진 등불이 다시 깜박인 새벽 이름 없는 남자를 만난다.",
        2 to "상층으로 올라간 리엔이 꺼진 빛의 값을 알게 된다.",
    )
    private val episodeCounts = mapOf(1 to 10, 2 to 8)

    private val episodeTitles: Map<Pair<Int, Int>, String> = mapOf(
        (1 to 1) to "잿빛 거리",
        (1 to 2) to "불빛을 세는 아이",
        (1 to 3) to "결사의 문",
        (1 to 4) to "그날의 빛",
        (1 to 5) to "갈림길",
        (1 to 6) to "잿더미 시장",
        (1 to 7) to "잿빛 새벽의 등불",
    )
    private val episodeSynopses: Map<Pair<Int, Int>, String> = mapOf(
        (1 to 9) to "하르트가 처음으로 자기 이름을 말한다.",
        (2 to 5) to "리엔이 등불지기장이 된다.",
    )

    fun units(): List<StoryUnit> {
        val out = mutableListOf<StoryUnit>()
        val trashed = mutableListOf<StoryUnit>()
        val chapterOrders = OrderKeys.evenlySpaced(episodeCounts.size)
        for ((ci, c) in episodeCounts.keys.sorted().withIndex()) {
            out += StoryUnit(
                key = K.chapter(c), level = 0, order = chapterOrders[ci], title = chapterTitles[c],
                synopsis = chapterSynopses[c], updatedAt = LAST_EDIT, updatedBy = DEVICE,
            )
            val count = episodeCounts.getValue(c)
            val episodeOrders = OrderKeys.evenlySpaced(count)
            for (e in 1..count) {
                out += StoryUnit(
                    key = K.episode(c, e), parentKey = K.chapter(c), level = 1, order = episodeOrders[e - 1],
                    title = episodeTitles[c to e], synopsis = episodeSynopses[c to e],
                    doneMarked = c == 1 && e <= 6,
                    nextBlockNumber = nextBlockNumbers[c to e] ?: 1,
                    updatedAt = LAST_EDIT, updatedBy = DEVICE,
                )
                val specs = scenes.filter { it.c == c && it.e == e }
                val sceneOrders = OrderKeys.evenlySpaced(specs.size)
                for ((si, spec) in specs.withIndex()) {
                    val unit = StoryUnit(
                        key = K.scene(c, e, spec.s), parentKey = K.episode(c, e), level = 2, order = sceneOrders[si],
                        title = spec.title, place = spec.place, time = spec.time, silentCast = spec.silentCast,
                        memo = spec.memo, transitionMode = spec.transitionMode?.open(),
                        transitionRuns = spec.transitionRuns, transitionSnapshot = spec.transitionSnapshot,
                        deletedAt = spec.deletedAt, trashEntryKey = spec.trashEntryKey,
                        updatedAt = spec.deletedAt ?: LAST_EDIT, updatedBy = DEVICE,
                    )
                    if (spec.deletedAt != null) trashed += unit else out += unit
                }
            }
        }
        // Non-live rows last: the order a split + reassemble round trip produces.
        return out + trashed
    }
}
