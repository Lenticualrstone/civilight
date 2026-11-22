package io.github.lenticualrstone.civilight.domain.cast

import io.github.lenticualrstone.civilight.domain.cast.timed.ReadPoint
import io.github.lenticualrstone.civilight.domain.cast.timed.TimedValues
import io.github.lenticualrstone.civilight.domain.cast.timed.recordAt
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.history
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.position.Position
import io.github.lenticualrstone.civilight.model.position.Rank
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement

/** The display name at a read point (CST-11). */
data class DisplayNameReading(
    val name: String,
    /** The override in effect (`???`), or null = show [name]. */
    val override: String?,
    /** The record in effect; null = `displayNameBase`. */
    val record: ChangeRecord?,
    /** The next record after the point that clears the override (the reveal), if any. */
    val reveal: ChangeRecord?,
) {
    /** What players see. */
    val shown: String get() = override ?: name
    val isOverridden: Boolean get() = override != null
}

/**
 * Display-name override (CST-11): one character-level timed value (`displayName` records; a null
 * value shows `이름`). Every presentation (list note, card, panels) is derived from the history.
 */
object DisplayNames {
    fun target(characterKey: Key): ChangeTarget.DisplayName = ChangeTarget.DisplayName(characterKey)

    private fun valueOf(record: ChangeRecord): String? = record.valueAs(String.serializer())?.takeIf { it.isNotBlank() }

    fun read(doc: ProjectDoc, characterKey: Key, point: ReadPoint): DisplayNameReading {
        val c = requireNotNull(doc.index.character(characterKey)) { "no character $characterKey" }
        val record = doc.recordAt(target(characterKey), point)
        val override = if (record != null) valueOf(record) else c.displayNameBase?.takeIf { it.isNotBlank() }
        val at = pointRank(doc, point)
        val reveal = if (override == null) null else doc.history(target(characterKey))
            .firstOrNull { doc.manuscript.rankOf(it.anchor) > at && valueOf(it) == null }
        return DisplayNameReading(c.name, override, record, reveal)
    }

    fun read(doc: ProjectDoc, characterKey: Key, lens: LensPoint): DisplayNameReading = read(doc, characterKey, ReadPoint.AtLens(lens))

    private fun pointRank(doc: ProjectDoc, point: ReadPoint): Rank = when (point) {
        is ReadPoint.AtLens -> doc.manuscript.rankOf(point.lens)
        is ReadPoint.BeforeBlock -> doc.manuscript.blockRankOf(point.blockKey)?.let { Rank(it.value - 1) } ?: Rank.UNPLACED
        is ReadPoint.AtEvent -> doc.manuscript.rankOf(Position.Event(point.eventKey)) ?: Rank(Long.MIN_VALUE)
    }

    /** `1-9`: the point of the reveal record. */
    fun revealPoint(doc: ProjectDoc, reading: DisplayNameReading): String? = reading.reveal?.let { StoryLabels.anchorPoint(doc, it.anchor) }

    /** The last live episode that ends before the reveal (`1-8` for a reveal at 1-9), or null. */
    fun lastEpisodeBefore(doc: ProjectDoc, reveal: ChangeRecord): String? {
        val rank = doc.manuscript.rankOf(reveal.anchor)
        return liveEpisodes(doc).lastOrNull { ep -> (doc.manuscript.rankOf(Position.End(ep.key)) ?: Rank.UNPLACED) < rank }
            ?.let { StoryLabels.unitCode(doc, it.key) }
    }

    /** List subtitle note: `표시 이름 ??? (1-8까지)`, `표시 이름 ???` without a reveal; null when no override. */
    fun listNote(doc: ProjectDoc, characterKey: Key, lens: LensPoint): String? {
        val r = read(doc, characterKey, lens)
        val override = r.override ?: return null
        val until = r.reveal?.let { lastEpisodeBefore(doc, it) }
        return if (until != null) "표시 이름 $override (${until}까지)" else "표시 이름 $override"
    }

    /** Card title and subtitle: `???` / `하르트 · 이름 공개 1-9`; null subtitle when no override. */
    fun card(doc: ProjectDoc, characterKey: Key, lens: LensPoint): Pair<String, String?> {
        val r = read(doc, characterKey, lens)
        if (r.override == null) return r.name to null
        val sub = revealPoint(doc, r)?.let { "${r.name} · 이름 공개 $it" } ?: r.name
        return r.override to sub
    }

    /** Script `참고` row value: `??? (1-9 공개)`; the plain name when none. */
    fun referenceValue(doc: ProjectDoc, characterKey: Key, point: ReadPoint): String {
        val r = read(doc, characterKey, point)
        val override = r.override ?: return r.name
        return revealPoint(doc, r)?.let { "$override ($it 공개)" } ?: override
    }

    /** Script `속성` panel: `???` with `1-9에서 공개`. */
    fun propertiesValue(doc: ProjectDoc, characterKey: Key, point: ReadPoint): Pair<String, String?> {
        val r = read(doc, characterKey, point)
        return r.shown to revealPoint(doc, r)?.takeIf { r.override != null }?.let { "${it}에서 공개" }
    }

    /** Speaker picker row sub: `표시 이름 ???`, or null. */
    fun pickerNote(doc: ProjectDoc, characterKey: Key, point: ReadPoint): String? =
        read(doc, characterKey, point).override?.let { "표시 이름 $it" }

    /** Script speaker label: the display name with the real name beside it (`??? 하르트`). */
    fun speakerLabel(doc: ProjectDoc, characterKey: Key, point: ReadPoint): String {
        val r = read(doc, characterKey, point)
        return if (r.override != null) "${r.override} ${r.name}" else r.name
    }

    // --- Edits ---

    private fun encode(value: String?): JsonElement? =
        value?.takeIf { it.isNotBlank() }?.let { ChangeRecord.encodeValue(String.serializer(), it) }

    /** Base override (`displayNameBase`); null or blank = show `이름`. */
    fun setBase(doc: ProjectDoc, characterKey: Key, value: String?, ctx: EditContext): ProjectDoc = doc.copy(
        cast = doc.cast.copy(characters = doc.cast.characters.map { c ->
            if (c.key != characterKey) c else c.copy(displayNameBase = value?.takeIf { it.isNotBlank() }, updatedAt = ctx.now, updatedBy = ctx.by)
        }),
    )

    /** `이 값 고치기`. */
    fun correctInEffect(doc: ProjectDoc, characterKey: Key, value: String?, lens: LensPoint, ctx: EditContext): ProjectDoc =
        TimedValues.correctInEffect(doc, target(characterKey), encode(value), lens, ctx) { setBase(it, characterKey, value, ctx) }

    /** `여기서부터 바뀜`. */
    fun changeFromHere(doc: ProjectDoc, characterKey: Key, value: String?, lens: LensPoint, ctx: EditContext): ProjectDoc =
        TimedValues.changeFromHere(doc, target(characterKey), encode(value), lens, ctx, { setBase(it, characterKey, value, ctx) })

    /** Reveal from the start of [unitKey]: a record whose value clears the override. */
    fun revealAt(doc: ProjectDoc, characterKey: Key, unitKey: Key, ctx: EditContext, note: String? = "이름 공개"): ProjectDoc =
        TimedValues.addRecord(doc, target(characterKey), null, io.github.lenticualrstone.civilight.model.change.Anchor.UnitStart(unitKey), ctx, note).first

    /** Live episode-level units in manuscript order. */
    internal fun liveEpisodes(doc: ProjectDoc): List<StoryUnit> {
        val level = doc.project.structure.episodeLevel ?: return emptyList()
        val out = ArrayList<StoryUnit>()
        fun visit(parent: Key?) {
            for (u in doc.index.children(parent)) {
                if (!doc.liveness.isLive(u)) continue
                if (u.level == level) out += u else if (u.level < level) visit(u.key)
            }
        }
        visit(null)
        return out
    }
}
