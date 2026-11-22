package io.github.lenticualrstone.civilight.domain.story.lens

import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.history
import io.github.lenticualrstone.civilight.model.doc.isLinkBeforeReveal
import io.github.lenticualrstone.civilight.model.doc.isPlanned
import io.github.lenticualrstone.civilight.model.doc.recordAt
import io.github.lenticualrstone.civilight.model.doc.recordBeforeBlock
import io.github.lenticualrstone.civilight.model.doc.recordsHere
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.position.Position
import io.github.lenticualrstone.civilight.model.story.StoryUnit

/** The lens chip (`LensChip`): `최신 · 1-7`, `최신 · 전체`, `시점 · 1-5`; [accented] = `acc` 1 px border. */
data class LensChipState(val label: String, val accented: Boolean)

/** One row of the lens sheet: code + sub; [unwritten] rows are `ink3` (`아직 쓰지 않음`); [viewing] shows `보는 중`. */
data class LensSheetRow(val point: LensPoint, val code: String, val sub: String, val unwritten: Boolean, val viewing: Boolean)

/** `APPLIED` or `PLANNED` relative to 최신 (STO-14). */
enum class RecordStatus { APPLIED, PLANNED }

/**
 * Lens reading for screens (rule 11; STO-12 … STO-14): chip and sheet labels, `예정`, and the
 * `valueAt` family over the core position engine.
 */
class LensView(val doc: ProjectDoc, val codes: UnitCodes = UnitCodes(doc)) {
    private val manuscript = doc.manuscript

    /** The scene whose end is the 집필 선, or null. */
    val writingLineScene: StoryUnit? get() = manuscript.writingLineSceneKey?.let(doc.index::unit)

    /** `1-7` (episode of the 집필 선 scene), the scene label without an episode level, or `전체`. */
    fun latestCode(): String {
        val scene = writingLineScene ?: return "전체"
        return codes.episodeOf(scene)?.let(codes::episodeCode) ?: codes.sceneLabel(scene)
    }

    /** Code of a lens point: `1-5`, `1-7 씬 2`, `1장`; `최신` for the latest point. */
    fun pointCode(point: LensPoint): String = when (point) {
        LensPoint.Latest -> "최신"
        is LensPoint.UnitEnd -> codes.unitLabel(point.unitKey) ?: "최신"
    }

    fun chip(point: LensPoint): LensChipState = when (point) {
        LensPoint.Latest -> LensChipState("최신 · ${latestCode()}", accented = false)
        is LensPoint.UnitEnd ->
            if (doc.index.unit(point.unitKey) == null) chip(LensPoint.Latest) else LensChipState("시점 · ${pointCode(point)}", accented = true)
    }

    // --- valueAt family -------------------------------------------------------------------------

    /** The record in force at [point], or null (base value / `만나기 전`). */
    fun recordAt(target: ChangeTarget, point: LensPoint): ChangeRecord? = doc.recordAt(target, point)

    /** Script panel: the record in force just before [blockKey]. */
    fun stateBefore(target: ChangeTarget, blockKey: Key): ChangeRecord? = doc.recordBeforeBlock(target, blockKey)

    /** Script panel `여기서` rows of a block. */
    fun changesAt(blockKey: Key): List<ChangeRecord> = doc.recordsHere(blockKey)

    /** Live records of a target in story order. */
    fun history(target: ChangeTarget): List<ChangeRecord> = doc.history(target)

    fun status(record: ChangeRecord): RecordStatus = if (doc.isPlanned(record)) RecordStatus.PLANNED else RecordStatus.APPLIED

    /** Records of a target that are `예정` at 최신. */
    fun planned(target: ChangeTarget): List<ChangeRecord> = history(target).filter { doc.isPlanned(it) }

    // --- Labels ------------------------------------------------------------------------------------

    /**
     * Where a record starts: `1-3` (unit start of an episode), `1-7 씬 2`, `2장`, `1-7 #043` (block),
     * `1-7 씬 3 뒤` (unit end, `ED`), the event title for a world event, `—` when unplaced.
     */
    fun anchorLabel(anchor: Anchor): String = when (anchor) {
        is Anchor.UnitStart -> codes.unitLabel(anchor.unitKey) ?: "—"
        is Anchor.UnitEnd -> codes.unitLabel(anchor.unitKey)?.let { "$it 뒤" } ?: "—"
        is Anchor.AtBlock -> doc.index.block(anchor.blockKey)?.let { b ->
            val episode = codes.episodeOf(b.sceneKey)?.let(codes::episodeCode)
            listOfNotNull(episode, "#" + b.number.toString().padStart(3, '0')).joinToString(" ")
        } ?: "—"
        is Anchor.WorldEvent -> doc.index.event(anchor.eventKey)?.title ?: "—"
        else -> "—"
    }

    /** Episode-level point of an anchor for `예정 · 2-5` (`ED`: the anchor's episode code). */
    fun anchorPoint(anchor: Anchor): String = when (anchor) {
        is Anchor.UnitStart, is Anchor.UnitEnd -> {
            val key = if (anchor is Anchor.UnitStart) anchor.unitKey else (anchor as Anchor.UnitEnd).unitKey
            doc.index.unit(key)?.let { u -> codes.episodeOf(u)?.let(codes::episodeCode) ?: codes.unitLabel(u) } ?: "—"
        }
        is Anchor.AtBlock -> doc.index.block(anchor.blockKey)?.let { codes.episodeOf(it.sceneKey)?.let(codes::episodeCode) } ?: anchorLabel(anchor)
        else -> anchorLabel(anchor)
    }

    /** `예정 · 2-5` for a planned record, else null. */
    fun plannedLabel(record: ChangeRecord): String? = if (doc.isPlanned(record)) "예정 · ${anchorPoint(record.anchor)}" else null

    /** `예정 · 집필 선(1-7 씬 3) 뒤 기록`, or null without a 집필 선. */
    fun plannedFootnote(): String? = writingLineScene?.let { "예정 · 집필 선(${codes.sceneRef(it)}) 뒤 기록" }

    /**
     * The value before the record in force at [point], with the last point it held: `??? (1-8까지)`
     * (LENS-06, cast list). Null when no record is in force or nothing precedes it in the story.
     * The end point is the episode before a unit-start anchor, the block before a block anchor, the
     * scene itself for a unit-end anchor (`ED`).
     */
    fun previousValueLabel(target: ChangeTarget, point: LensPoint, values: ValueText = ValueText(doc)): String? {
        val current = recordAt(target, point) ?: return null
        val all = history(target)
        val before = all.getOrNull(all.indexOfFirst { it.key == current.key } - 1)
        val previous = before?.let { values.of(target, it.value) } ?: values.base(target)
        val until = untilPoint(current.anchor) ?: return null
        return "$previous (${until}까지)"
    }

    private fun untilPoint(anchor: Anchor): String? = when (anchor) {
        is Anchor.UnitStart -> doc.index.unit(anchor.unitKey)?.let { u ->
            val episode = codes.episodeOf(u) ?: u
            val all = episodes()
            all.getOrNull(all.indexOfFirst { it.key == episode.key } - 1)?.let(codes::unitLabel)
        }
        is Anchor.AtBlock -> {
            val blocks = manuscript.liveBlocks
            val i = blocks.indexOfFirst { it.key == anchor.blockKey }
            blocks.getOrNull(i - 1)?.let { anchorLabel(Anchor.AtBlock(it.key)) }
        }
        is Anchor.UnitEnd -> codes.unitLabel(anchor.unitKey)
        else -> null
    }

    /** `▲ 공개 전 사용` for a link to [termKey] in [blockKey] (block order, LENS-08). */
    fun isLinkBeforeReveal(termKey: Key, blockKey: Key): Boolean = doc.isLinkBeforeReveal(termKey, blockKey)

    /**
     * Script panel row for a block (LENS-07): `만나기 전`, `만나기 전 → 경계 · ◆ 여기서`, `경계`.
     */
    fun panelRow(target: ChangeTarget, blockKey: Key, values: ValueText = ValueText(doc)): String {
        val before = stateBefore(target, blockKey)?.let { values.of(target, it.value) } ?: values.base(target)
        val here = changesAt(blockKey).lastOrNull { it.target.sameAs(target) } ?: return before
        return "$before → ${values.of(target, here.value)} · ◆ 여기서"
    }

    // --- Lens sheet ------------------------------------------------------------------------------

    /** Live episodes in manuscript order (episode-level units; scenes in a 1-level structure). */
    fun episodes(): List<StoryUnit> {
        val level = codes.episodeLevel ?: codes.sceneLevel
        val seen = LinkedHashSet<Key>()
        val out = mutableListOf<StoryUnit>()
        fun visit(u: StoryUnit) {
            if (!doc.liveness.isUnitLive(u.key) || !seen.add(u.key)) return
            if (u.level == level) out += u else doc.index.children(u.key).forEach(::visit)
        }
        doc.index.children(null).forEach(::visit)
        return out
    }

    /** True when the episode starts after the 집필 선 (`아직 쓰지 않음`). */
    fun isUnwritten(episode: StoryUnit): Boolean {
        val line = manuscript.writingLineRank ?: return false
        val start = manuscript.rankOf(Position.Start(episode.key)) ?: return false
        return start > line
    }

    /**
     * Sheet rows (`ED`, Q11): `최신`, episodes where a record of [targets] starts, the viewed point and
     * the first unwritten episode, in story order. Sub = episode title (or `미정`), `아직 쓰지 않음`
     * beyond the 집필 선.
     */
    fun sheetRows(viewed: LensPoint, targets: List<ChangeTarget> = emptyList()): List<LensSheetRow> {
        val all = episodes()
        val wanted = LinkedHashSet<Key>()
        for (t in targets) for (r in history(t)) episodeKeyOf(r.anchor)?.let(wanted::add)
        (viewed as? LensPoint.UnitEnd)?.let { p -> doc.index.unit(p.unitKey)?.let { u -> (codes.episodeOf(u) ?: u).key }?.let(wanted::add) }
        all.firstOrNull(::isUnwritten)?.let { wanted += it.key }
        val rows = mutableListOf(LensSheetRow(LensPoint.Latest, "최신", "기본값 · 지금까지 쓴 모든 내용", false, viewed == LensPoint.Latest))
        for (e in all) {
            if (e.key !in wanted) continue
            val point = LensPoint.UnitEnd(e.key)
            val unwritten = isUnwritten(e)
            val viewing = viewed is LensPoint.UnitEnd && (viewed.unitKey == e.key || codes.episodeOf(viewed.unitKey)?.key == e.key)
            rows += LensSheetRow(point, codes.unitLabel(e), if (unwritten) "아직 쓰지 않음" else e.title ?: "미정", unwritten, viewing)
        }
        return rows
    }

    /** `더 자세히` chips: the episode and its neighbours (`1-4 · 1-5 · 1-6`). */
    fun neighbours(episodeKey: Key): List<String> {
        val all = episodes()
        val i = all.indexOfFirst { it.key == episodeKey }
        if (i < 0) return emptyList()
        return all.subList(maxOf(0, i - 1), minOf(all.size, i + 2)).map(codes::unitLabel)
    }

    private fun episodeKeyOf(anchor: Anchor): Key? = when (anchor) {
        is Anchor.AtBlock -> doc.index.block(anchor.blockKey)?.let { codes.episodeOf(it.sceneKey)?.key }
        is Anchor.UnitStart -> doc.index.unit(anchor.unitKey)?.let { codes.episodeOf(it)?.key ?: firstEpisodeUnder(it) }
        is Anchor.UnitEnd -> doc.index.unit(anchor.unitKey)?.let { codes.episodeOf(it)?.key }
        else -> null
    }

    private fun firstEpisodeUnder(unit: StoryUnit): Key? = episodes().firstOrNull { e -> doc.index.ancestors(e).any { it.key == unit.key } }?.key
}
