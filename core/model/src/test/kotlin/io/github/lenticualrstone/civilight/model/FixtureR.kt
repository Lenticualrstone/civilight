package io.github.lenticualrstone.civilight.model

import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.EntityRef
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.cast.Relation
import io.github.lenticualrstone.civilight.model.cast.RelationEndKind
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.change.RelationSlotName
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.file.CastFile
import io.github.lenticualrstone.civilight.model.file.ChangesFile
import io.github.lenticualrstone.civilight.model.file.EpisodeFile
import io.github.lenticualrstone.civilight.model.file.StoryIndexFile
import io.github.lenticualrstone.civilight.model.file.WorldFile
import io.github.lenticualrstone.civilight.model.project.Project
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.world.Term
import kotlinx.serialization.json.JsonPrimitive

/**
 * Fixture R of 20-story-time.md §4.1 (records, rules 11/13): chapters 1 and 2, episodes 1-1 … 1-9
 * and 2-5; content in every scene up to and including 1-7 씬 3; 1-7 씬 4, 1-8, 1-9 and 2-5 empty.
 * 1-7 씬 3 holds #042 … #045. Keys are readable labels (the model never validates them).
 */
object FixtureR {
    const val PROJECT = "P"
    const val RIEN = "C_RIEN"
    const val HART = "C_HART"
    const val AFFILIATION = "FD_AFF"
    const val RELATION = "REL_RIEN_HART"
    const val ORIGINIUM = "T_ORI"

    fun ch(n: Int) = "CH$n"
    fun ep(c: Int, e: Int) = "E$c-$e"
    fun sc(c: Int, e: Int, s: Int) = "S$c-$e-$s"
    fun blk(n: Int) = "B%03d".format(n)

    /** Scenes per episode (1-7 has four). */
    private val episodes: List<Pair<Int, Int>> = (1..9).map { 1 to it } + listOf(2 to 5)

    private fun scenesOf(c: Int, e: Int) = if (c == 1 && e == 7) 4 else 1

    fun units(): List<StoryUnit> {
        val out = mutableListOf<StoryUnit>()
        val chapterOrders = OrderKeys.evenlySpaced(2)
        out += StoryUnit(key = ch(1), level = 0, order = chapterOrders[0])
        out += StoryUnit(key = ch(2), level = 0, order = chapterOrders[1])
        for (c in 1..2) {
            val eps = episodes.filter { it.first == c }.map { it.second }
            // Episode order keys follow the episode number so 2-5 sorts like a fifth episode.
            val epOrders = OrderKeys.evenlySpaced(9)
            for (e in eps) {
                out += StoryUnit(key = ep(c, e), parentKey = ch(c), level = 1, order = epOrders[e - 1])
                val n = scenesOf(c, e)
                val sceneOrders = OrderKeys.evenlySpaced(n)
                for (s in 1..n) out += StoryUnit(key = sc(c, e, s), parentKey = ep(c, e), level = 2, order = sceneOrders[s - 1])
            }
        }
        return out
    }

    fun narration(key: String, scene: String, order: OrderKey, number: Int, text: String = "본문") = Block(
        key = key, sceneKey = scene, order = order, number = number, kind = BlockKind.NARRATION.open(),
        runs = listOf(TextRun.Plain(text)),
    )

    fun dialogue(key: String, scene: String, order: OrderKey, number: Int, speaker: String, runs: List<TextRun>) = Block(
        key = key, sceneKey = scene, order = order, number = number, kind = BlockKind.DIALOGUE.open(),
        speakerKey = speaker, runs = runs,
    )

    /** Blocks: one narration block in every scene before 1-7 씬 3, then #042 … #045 in 1-7 씬 3. */
    fun episodesWithBlocks(): Map<String, EpisodeFile> {
        val out = LinkedHashMap<String, EpisodeFile>()
        for ((c, e) in episodes) out[ep(c, e)] = EpisodeFile(unitKey = ep(c, e))
        for (e in 1..6) {
            out[ep(1, e)] = EpisodeFile(ep(1, e), blocks = listOf(narration("B1-$e", sc(1, e, 1), OrderKeys.FIRST, 1)))
        }
        val orders = OrderKeys.evenlySpaced(4)
        val e7 = listOf(
            narration("B1-7-1", sc(1, 7, 1), OrderKeys.FIRST, 1),
            narration("B1-7-2", sc(1, 7, 2), OrderKeys.FIRST, 2),
            dialogue(blk(42), sc(1, 7, 3), orders[0], 42, RIEN, listOf(TextRun.Plain("저기… 불빛이 보여요."))),
            dialogue(blk(43), sc(1, 7, 3), orders[1], 43, HART, listOf(TextRun.Plain("멈추지 마. 저건 등불이 아니야."))),
            narration(blk(44), sc(1, 7, 3), orders[2], 44, "…오리지늄 등불이 한 번 깜박였다."),
            dialogue(blk(45), sc(1, 7, 3), orders[3], 45, RIEN, listOf(TextRun.Plain("하르트 씨, 방금…"))),
        )
        out[ep(1, 7)] = EpisodeFile(ep(1, 7), blocks = e7)
        return out
    }

    val affiliation = ChangeTarget.Field(EntityKind.CHARACTER, RIEN, AFFILIATION)
    val rienMind = ChangeTarget.RelationSlot(RELATION, RelationSlotName.MIND_A)
    val hartMind = ChangeTarget.RelationSlot(RELATION, RelationSlotName.MIND_B)
    val hartAddress = ChangeTarget.RelationSlot(RELATION, RelationSlotName.ADDRESS_B)
    val rienAddress = ChangeTarget.RelationSlot(RELATION, RelationSlotName.ADDRESS_A)
    val between = ChangeTarget.RelationSlot(RELATION, RelationSlotName.BETWEEN)
    val hartName = ChangeTarget.DisplayName(HART)
    val reveal = ChangeTarget.TermReveal(ORIGINIUM)

    fun record(key: String, target: ChangeTarget, anchor: Anchor, value: String?, createdAt: Long = 1L) = ChangeRecord(
        key = key, target = target, value = value?.let(::JsonPrimitive), anchor = anchor, createdAt = createdAt,
    )

    fun records(): List<ChangeRecord> = listOf(
        record("R1", affiliation, Anchor.UnitStart(ep(1, 1)), "없음"),
        record("R2", affiliation, Anchor.UnitStart(ep(1, 3)), "등불 결사"),
        record("R3", affiliation, Anchor.UnitStart(ep(2, 5)), "등불지기장"),
        record("R4", rienMind, Anchor.AtBlock(blk(43)), "경계"),
        record("R5", hartMind, Anchor.AtBlock(blk(43)), "보호"),
        record("R5a", hartAddress, Anchor.AtBlock(blk(43)), "꼬마"),
        record("R6", between, Anchor.UnitStart(ep(1, 9)), "동료"),
        record("R7", rienMind, Anchor.UnitStart(ep(1, 9)), "신뢰"),
        record("R7a", rienAddress, Anchor.UnitStart(ep(1, 9)), "하르트"),
        record("R8", hartName, Anchor.UnitStart(ep(1, 9)), "하르트"),
        record("R9", reveal, Anchor.AtBlock(blk(44)), null),
    )

    fun doc(): ProjectDoc = ProjectDoc(
        project = Project(
            key = PROJECT,
            name = "잿빛 새벽의 등불",
            fieldDefs = listOf(
                FieldDef(key = AFFILIATION, entityKind = EntityKind.CHARACTER.open(), name = "소속", type = FieldType.SHORT_TEXT.open(), order = "V"),
            ),
        ),
        story = StoryIndexFile(units()),
        episodes = episodesWithBlocks(),
        cast = CastFile(
            characters = listOf(
                Character(key = RIEN, recordNo = 1, name = "리엔", order = "V"),
                Character(key = HART, recordNo = 2, name = "하르트", displayNameBase = "???", order = "k"),
            ),
            relations = listOf(
                Relation(key = RELATION, endA = EntityRef(RelationEndKind.CHARACTER, RIEN), endB = EntityRef(RelationEndKind.CHARACTER, HART)),
            ),
        ),
        world = WorldFile(terms = listOf(Term(key = ORIGINIUM, order = "V", name = "오리지늄 등불", aliases = listOf("오리지늄", "빛돌")))),
        changes = ChangesFile(records()),
    )

    /** A term link run to 오리지늄 등불. */
    fun originiumLink(linkKey: String) = TextRun.Link(linkKey, LinkTarget(LinkTargetKind.TERM, ORIGINIUM), "오리지늄")

    /** Adds a block to a scene of the doc (after its current blocks). */
    fun ProjectDoc.withBlock(block: Block): ProjectDoc {
        val unitKey = index.numberingUnitKeyOfScene(block.sceneKey)!!
        val file = episodes[unitKey] ?: EpisodeFile(unitKey)
        return copy(episodes = episodes + (unitKey to file.copy(blocks = file.blocks + block)))
    }

    /** Replaces a unit (e.g. to trash it). */
    fun ProjectDoc.withUnit(key: String, change: (StoryUnit) -> StoryUnit): ProjectDoc =
        copy(story = story.copy(units = story.units.map { if (it.key == key) change(it) else it }))

    /** Replaces a block. */
    fun ProjectDoc.withBlockChanged(key: String, change: (Block) -> Block): ProjectDoc =
        copy(episodes = episodes.mapValues { (_, f) -> f.copy(blocks = f.blocks.map { if (it.key == key) change(it) else it }) })

    fun ProjectDoc.withRecord(record: ChangeRecord): ProjectDoc = copy(changes = changes.copy(records = changes.records + record))
}
