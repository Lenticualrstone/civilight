package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.ClockTime
import io.github.lenticualrstone.civilight.model.IntSpan
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.file.CastFile
import io.github.lenticualrstone.civilight.model.file.EpisodeFile
import io.github.lenticualrstone.civilight.model.file.StoryIndexFile
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.project.Project
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.ChoiceOption
import io.github.lenticualrstone.civilight.model.script.CueKind
import io.github.lenticualrstone.civilight.model.script.DirectionCue
import io.github.lenticualrstone.civilight.model.script.VoiceMode
import io.github.lenticualrstone.civilight.model.script.VoiceState
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import java.time.ZoneId
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** A deterministic edit context: fixture "now", Asia/Seoul, keys `01JZ000000TEST000000000001`… */
fun testCtx(now: Long = ExampleClock.NOW): EditContext {
    var n = 0
    return EditContext(now = now, zone = ZoneId.of("Asia/Seoul"), device = "test", newKey = { "01JZ000000TEST" + (++n).toString().padStart(12, '0') })
}

fun ProjectDoc.block(key: Key): Block = index.block(key) ?: error("no block $key")
fun ProjectDoc.record(key: Key): ChangeRecord = index.record(key) ?: error("no record $key")
fun ProjectDoc.unit(key: Key): StoryUnit = index.unit(key) ?: error("no unit $key")
fun ProjectDoc.liveNumbers(sceneKey: Key): List<Int> = manuscript.liveBlocksOf(sceneKey).map { it.number }
fun ProjectDoc.withBlock(key: Key, transform: (Block) -> Block): ProjectDoc = replaceBlock(transform(block(key)))
fun ProjectDoc.withRecords(vararg records: ChangeRecord): ProjectDoc = copy(changes = changes.copy(records = changes.records + records))

fun fixed(id: String, at: Long = ExampleClock.at("2026-11-20T03:00:00Z")) =
    VoiceState(mode = VoiceMode.FIXED.open(), fixedId = id, fixedAt = at)

/** A `◆` record of a test (fixture-labelled records of the spec). */
fun record(key: Key, anchor: Anchor, target: ChangeTarget = ChangeTarget.DisplayName(K.SERA)) =
    ChangeRecord(key = key, target = target, anchor = anchor, createdAt = ExampleClock.NOW)

/**
 * SCR fixture F1 built on the example project (`ExampleProject.build()`): the example differs from
 * F1 (see the status file), so this overlay adds what F1 states and the example lacks:
 * - spec R1 = example `R4` (리엔의 마음 → 하르트, on #043); spec R4 = example `R5` moved to #042;
 *   spec R5 = example `R5A` moved to #045; the `◇` of 오리지늄 등불 = example `R9` on #044;
 * - 1-7 씬 4 blocks #049 (배경 결사의 문(아침)), #050 마르타, #051 리엔, #052 세라;
 * - 1-7 씬 5 (결사의 문, 씬 4 기준 같은 날 08:00) with #053 (narration).
 * The 1-7 counter stays 58; 1-8 씬 1 starts with #001 and its counter is 61.
 */
object F1 {
    val R1 = K.R4
    val R4 = K.R5
    val R5 = K.R5A
    val REVEAL = K.R9

    val S1 = K.SCENE_1_7_1
    val S2 = K.SCENE_1_7_2
    val S3 = K.SCENE_1_7_3
    val S4 = K.SCENE_1_7_4
    val S5 = K.scene(1, 7, 5)
    val E17 = K.EPISODE_1_7
    val E18 = K.EPISODE_1_8
    val S18_1 = K.SCENE_1_8_1

    fun b(n: Int): Key = K.block(1, 7, n)

    fun raw(): ProjectDoc = ExampleProject.build()

    fun build(): ProjectDoc {
        var doc = raw().mapRecords { r ->
            when (r.key) {
                R4 -> r.copy(anchor = Anchor.AtBlock(b(42)))
                R5 -> r.copy(anchor = Anchor.AtBlock(b(45)))
                else -> r
            }
        }
        val s4 = doc.unit(S4)
        val s5 = StoryUnit(
            key = S5, parentKey = E17, level = 2, order = OrderKeys.between(s4.order, null),
            place = PlaceRef(PlaceRefKind.PLACE, K.PLACE_SOCIETY_GATE),
            time = SceneTime.Relative(baseKey = S4, dayMin = 0, dayMax = 0, clock = ClockTime.of(8, 0)),
        )
        doc = doc.copy(story = doc.story.copy(units = doc.story.units + s5))
        val o4 = OrderKeys.evenlySpaced(4)
        val created = ExampleClock.at("2026-11-21T10:00:00Z")
        fun blk(n: Int, scene: Key, order: String, kind: BlockKind, speaker: Key? = null, runs: List<TextRun> = emptyList(), cues: List<DirectionCue> = emptyList()) =
            Block(key = b(n), sceneKey = scene, order = order, number = n, kind = kind.open(), speakerKey = speaker, runs = runs, cues = cues, createdAt = created)
        val added = listOf(
            blk(49, S4, o4[0], BlockKind.DIRECTION, cues = listOf(DirectionCue(K.directionCue(90), CueKind.BACKGROUND.open(), K.ASSET_BG_SOCIETY_GATE_MORNING))),
            blk(50, S4, o4[1], BlockKind.DIALOGUE, K.MARTHA, listOf(TextRun.Plain("문은 해가 뜨면 닫혀. 서둘러."))),
            blk(51, S4, o4[2], BlockKind.DIALOGUE, K.RIEN, listOf(TextRun.Paren("숨을 고르며"), TextRun.Plain("하르트 씨는요?"))),
            blk(52, S4, o4[3], BlockKind.DIALOGUE, K.SERA, listOf(TextRun.Plain("먼저 들어갔어. 너도 들어와."))),
            blk(53, S5, OrderKeys.FIRST, BlockKind.NARRATION, runs = listOf(TextRun.Plain("문 안쪽에서 첫 등이 켜졌다."))),
        )
        doc = doc.addBlocks(added)
        return doc.copy(deletions = doc.deletions.copy(entries = doc.deletions.entries.filterNot { e -> added.any { it.key == e.key } }))
    }

    /** Resolved 때 of F1 (SCR §4.1 table; the place & time area computes these in the app). */
    val times: Map<Key, ResolvedSceneTime> = mapOf(
        S1 to ResolvedSceneTime(IntSpan(10, 14), 1, band = Band.DAWN, tRange = IntSpan(0, 120)),
        S2 to ResolvedSceneTime(IntSpan(10, 14), 1, band = Band.EVENING, tRange = IntSpan(780, 960)),
        S3 to ResolvedSceneTime(IntSpan(11, 15), 2, band = Band.DAWN, clock = ClockTime.of(5, 40), tRange = IntSpan(100, 100)),
        S4 to ResolvedSceneTime(IntSpan(11, 15), 2, band = Band.MORNING, clock = ClockTime.of(6, 10), tRange = IntSpan(130, 130)),
        S5 to ResolvedSceneTime(IntSpan(11, 15), 2, band = Band.MORNING, clock = ClockTime.of(8, 0), tRange = IntSpan(240, 240)),
        S18_1 to ResolvedSceneTime(IntSpan(13, 17), 1, band = Band.DAWN, tRange = IntSpan(0, 120)),
    )

    val time: SceneTimeSource = SceneTimeSource.of(times)

    fun timeWith(vararg overrides: Pair<Key, ResolvedSceneTime?>): SceneTimeSource {
        val map = times.toMutableMap()
        for ((k, v) in overrides) if (v == null) map.remove(k) else map[k] = v
        return SceneTimeSource.of(map)
    }

    /** A link run to 오리지늄 등불 (fixture links of T-12-14, T-11-02). */
    fun lanternLink(key: Key, text: String = "오리지늄 등불") =
        TextRun.Link(key, LinkTarget(LinkTargetKind.TERM, K.TERM_ORIGINIUM_LANTERN), text)
}

/**
 * SCR fixture F2: episode 1-7 of another project with 48 live blocks at positions 1…48 whose number
 * is the position except p20 #056 (씬 1 나레이션), p31 #057 (씬 2 나레이션), p38 #055 (씬 3, 리엔 자동),
 * p45 #051 (하르트, 고정 HRT_0107_051), p46 #052 (리엔, 고정), p47 #053 (리엔, 고정), p48 #058
 * (하르트, 고정 HRT_0107_058). Scenes: 씬 1 = p1–20, 씬 2 = p21–31, 씬 3 = p32–40, 씬 4 = p41–48.
 * Counter 59.
 */
object F2 {
    const val PROJECT = "01JZ000000PRJT000000000002"
    const val HART = "01JZ000000CHAR000000000104"
    const val RIEN = "01JZ000000CHAR000000000107"
    fun episode(e: Int) = "01JZ000000EPSD0000000001" + e.toString().padStart(2, '0')
    fun scene(s: Int) = "01JZ000000SCNE00000001070$s"
    fun blockAt(p: Int) = "01JZ000000BKNM0000000000" + p.toString().padStart(2, '0')

    private val special = mapOf(20 to 56, 31 to 57, 38 to 55, 45 to 51, 46 to 52, 47 to 53, 48 to 58)

    fun build(): ProjectDoc {
        val chapter = StoryUnit(key = "01JZ000000CHAP000000000101", level = 0, order = OrderKeys.FIRST)
        val epOrders = OrderKeys.evenlySpaced(7)
        val episodes = (1..7).map { e ->
            StoryUnit(key = episode(e), parentKey = chapter.key, level = 1, order = epOrders[e - 1], nextBlockNumber = if (e == 7) 59 else 1)
        }
        val scOrders = OrderKeys.evenlySpaced(4)
        val scenes = (1..4).map { s -> StoryUnit(key = scene(s), parentKey = episode(7), level = 2, order = scOrders[s - 1]) }
        val ranges = listOf(1..20, 21..31, 32..40, 41..48)
        val blocks = ranges.flatMapIndexed { si, range ->
            val orders = OrderKeys.evenlySpaced(range.count())
            range.mapIndexed { i, p ->
                val number = special[p] ?: p
                val (kind, speaker, voice) = when (p) {
                    38 -> Triple(BlockKind.DIALOGUE, RIEN, null)
                    45 -> Triple(BlockKind.DIALOGUE, HART, fixed("HRT_0107_051"))
                    46 -> Triple(BlockKind.DIALOGUE, RIEN, fixed("RIE_0107_052"))
                    47 -> Triple(BlockKind.DIALOGUE, RIEN, fixed("RIE_0107_053"))
                    48 -> Triple(BlockKind.DIALOGUE, HART, fixed("HRT_0107_058"))
                    else -> Triple(BlockKind.NARRATION, null, null)
                }
                Block(
                    key = blockAt(p), sceneKey = scene(si + 1), order = orders[i], number = number, kind = kind.open(),
                    speakerKey = speaker, runs = listOf(TextRun.Plain("줄 $p")), voice = voice,
                )
            }
        }
        val characters = listOf(
            Character(key = HART, recordNo = 4, name = "하르트", voiceCode = "HRT", order = "a"),
            Character(key = RIEN, recordNo = 7, name = "리엔", voiceCode = "RIE", order = "b"),
        )
        return ProjectDoc(
            project = Project(key = PROJECT, name = "F2"),
            story = StoryIndexFile(units = listOf(chapter) + episodes + scenes),
            episodes = mapOf(episode(7) to EpisodeFile(unitKey = episode(7), blocks = blocks)),
            cast = CastFile(characters = characters),
        )
    }
}

/** SCR fixture F3 on the F1 overlay: #045 becomes a choice `따라간다` (하르트) · `불빛 쪽을 한 번 더 본다`, followed by #046. */
object F3 {
    val OPTION_1 = K.choiceOption(91)
    val OPTION_2 = K.choiceOption(92)

    fun build(): ProjectDoc = F1.build().withBlock(F1.b(45)) {
        val orders = OrderKeys.evenlySpaced(2)
        it.copy(
            kind = BlockKind.CHOICE.open(), speakerKey = null, runs = emptyList(),
            options = listOf(
                ChoiceOption(OPTION_1, orders[0], listOf(TextRun.Plain("따라간다")), speakerKey = K.HART),
                ChoiceOption(OPTION_2, orders[1], listOf(TextRun.Plain("불빛 쪽을 한 번 더 본다"))),
            ),
        )
    }
}
