package io.github.lenticualrstone.civilight.domain.library.trash

import io.github.lenticualrstone.civilight.domain.library.common.TimeLabels
import io.github.lenticualrstone.civilight.domain.library.common.UnitCodes
import io.github.lenticualrstone.civilight.domain.library.hiding.HideableKind
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.cast.EntityRef
import io.github.lenticualrstone.civilight.model.cast.RelationEndKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.recorder.RecordKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderRecord
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.trash.NameTag
import io.github.lenticualrstone.civilight.model.trash.NameTagKind
import io.github.lenticualrstone.civilight.model.trash.NameTagState
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import io.github.lenticualrstone.civilight.model.trash.TrashReason
import java.time.ZoneId

/** Filter chips of 휴지통 (single choice, LIB-38; R20 §5 list, C-20 kinds). */
enum class TrashFilter(val label: String) {
    ALL("전체"), PROJECT("프로젝트"), SCENE("씬"), BLOCK("블록"), CHARACTER("인물"), WORLD("세계"), CHANGE("변화"), RECORDER("레코더");

    /** Pages and canvases appear under `전체` only (C-20). */
    fun matches(kind: TrashKind?): Boolean = when (this) {
        ALL -> true
        PROJECT -> kind == TrashKind.PROJECT
        SCENE -> kind == TrashKind.SCENE
        BLOCK -> kind == TrashKind.BLOCK_GROUP
        CHARACTER -> kind == TrashKind.CHARACTER || kind == TrashKind.FIELD_DEF
        WORLD -> kind == TrashKind.PLACE || kind == TrashKind.TERM || kind == TrashKind.FACTION || kind == TrashKind.MAP || kind == TrashKind.EVENT
        CHANGE -> kind == TrashKind.CHANGE
        RECORDER -> kind == TrashKind.RECORDER
    }

    /** Chip text: `전체 <n>` for ALL, the plain label otherwise. */
    fun chip(total: Int): String = if (this == ALL) "$label $total" else label
}

/** The 30-day countdown (LIB-38, LIB-39). */
object TrashDays {
    /** `d = 30 − (calendar days between the deletion date and today)` in [zone]. */
    fun daysLeft(deletedAt: Timestamp, now: Timestamp, zone: ZoneId): Long =
        TrashEntry.RETENTION_DAYS - TimeLabels.daysBetween(deletedAt, now, zone)

    fun label(deletedAt: Timestamp, now: Timestamp, zone: ZoneId): String = "${daysLeft(deletedAt, now, zone)}일 남음"

    /** Automatic purge when `d ≤ 0`. */
    fun isPurgeDue(entry: TrashEntry, now: Timestamp, zone: ZoneId): Boolean = daysLeft(entry.deletedAt, now, zone) <= 0
}

/** One 휴지통 row (`Trash` board). */
data class TrashRow(
    val entryKey: Key,
    val title: String,
    val sub: String,
    val daysLeft: String,
    /** Hidden things: masked while locked; `되살리기` asks for authentication. */
    val restoreNeedsAuth: Boolean,
) {
    val tags: List<String> get() = listOf("되살리기", "영구 삭제")

    /** `숨긴 인물 · 3일 남음`. */
    val titleWithDays: String get() = "$title · $daysLeft"
}

/**
 * Builds 휴지통 rows (LIB-38). Reference counts are computed at display time, never stored. This
 * area is the host; what `되살리기` does is the owning area's.
 */
class TrashRows(private val now: Timestamp, private val zone: ZoneId) {
    /**
     * A row of an entry of [doc]'s project. [itemsLocked] = the project's hidden items are locked;
     * [projectLocked] = the project itself is hidden and locked (location shows `숨긴 프로젝트`).
     */
    fun row(entry: TrashEntry, doc: ProjectDoc, itemsLocked: Boolean = true, projectLocked: Boolean = false): TrashRow {
        val index = doc.index
        val codes = UnitCodes(doc)
        val location = if (projectLocked) HIDDEN_PROJECT else doc.project.name
        val days = TrashDays.label(entry.deletedAt, now, zone)
        val target = entry.targetKeys.firstOrNull()
        val kind = entry.kind.known
        val merged = entry.reason.known == TrashReason.MERGED
        val mergedSuffix = entry.mergedIntoKey?.let { m -> index.character(m)?.name ?: index.place(m)?.name ?: index.term(m)?.name ?: index.faction(m)?.name }
            ?.let { "합침 → $it" }

        val hiddenKind = when (kind) {
            TrashKind.CHARACTER -> HideableKind.CHARACTER
            TrashKind.PLACE -> HideableKind.PLACE
            TrashKind.TERM -> HideableKind.TERM
            TrashKind.FACTION -> HideableKind.FACTION
            TrashKind.SCENE -> HideableKind.SCENE
            TrashKind.PAGE -> HideableKind.PAGE
            else -> null
        }
        val masked = (entry.hiddenAtDelete && itemsLocked && hiddenKind != null)
        if (masked) {
            return TrashRow(
                entry.key, "숨긴 ${hiddenKind!!.word}", "${hiddenKind.word} · $location · 되살리기는 지문 확인", days, restoreNeedsAuth = true,
            )
        }
        val needsAuth = entry.hiddenAtDelete || projectLocked
        val (title, sub) = when (kind) {
            TrashKind.PROJECT -> doc.project.name to if (entry.reason.known == TrashReason.REPLACED_BY_OVERWRITE) {
                TODO_COPY_LIB38_REPLACED
            } else {
                "프로젝트 · 씬 ${doc.manuscript.liveScenes.size}개 · 인물 ${doc.cast.characters.count { it.deletedAt == null }}명"
            }
            TrashKind.SCENE -> {
                val scene = target?.let(index::unit)
                val label = target?.let(codes::sceneLabel).orEmpty()
                (if (scene?.title != null) "$label · ${scene.title}" else label) to "씬 · $location"
            }
            TrashKind.BLOCK_GROUP -> {
                val sceneKey = entry.groupSceneKey ?: target?.let(index::block)?.sceneKey
                val label = sceneKey?.let(codes::sceneLabel).orEmpty()
                "$label · 블록 ${entry.targetKeys.size}개" to "블록 · $location · ${TimeLabels.dayWord(entry.deletedAt, now, zone)} 지움"
            }
            TrashKind.CHARACTER -> {
                val c = target?.let(index::character)
                val name = c?.name.orEmpty()
                val sub = if (merged && mergedSuffix != null) "인물 · $location · $mergedSuffix" else {
                    val (lines, relations) = c?.let { referenceCounts(doc, it) } ?: (0 to 0)
                    "인물 · $location · 대사 $lines · 관계 $relations"
                }
                name to sub
            }
            TrashKind.PLACE, TrashKind.TERM, TrashKind.FACTION -> {
                val name = target?.let { index.place(it)?.name ?: index.term(it)?.name ?: index.faction(it)?.name }.orEmpty()
                val word = hiddenKind!!.word
                name to (listOf(word, location) + listOfNotNull(mergedSuffix.takeIf { merged })).joinToString(" · ")
            }
            TrashKind.CHANGE -> TODO_COPY_LIB38_CHANGE to location
            TrashKind.FIELD_DEF -> (target?.let(index::fieldDef)?.name ?: TODO_COPY_LIB38_FIELD_DEF) to "인물 · $location"
            else -> (target ?: "") to location
        }
        return TrashRow(entry.key, title, sub, days, needsAuth)
    }

    /** A recorder record row: `음성 0:12` · `라이프 레코더`. */
    fun recorderRow(entry: TrashEntry, record: RecorderRecord, locked: Boolean = true): TrashRow {
        val days = TrashDays.label(entry.deletedAt, now, zone)
        if (record.hidden && locked) return TrashRow(entry.key, TODO_COPY_LIB38_HIDDEN_RECORD, "라이프 레코더", days, restoreNeedsAuth = true)
        val title = when (record.kind.known) {
            RecordKind.VOICE -> "음성 ${duration(record.durationMs ?: record.audio?.durationMs ?: 0L)}"
            else -> TODO_COPY_LIB38_TEXT_RECORD
        }
        return TrashRow(entry.key, title, "라이프 레코더", days, restoreNeedsAuth = record.hidden)
    }

    /** `0:12`, `1:02`. */
    fun duration(ms: Long): String {
        val s = ms / 1000
        return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
    }

    /** Rows of [filter], newest deletion first (`ED`). */
    fun filter(entries: List<TrashEntry>, filter: TrashFilter): List<TrashEntry> =
        entries.filter { filter.matches(it.kind.known) }.sortedByDescending { it.deletedAt }

    companion object {
        const val HIDDEN_PROJECT = "숨긴 프로젝트"
        const val TODO_COPY_LIB38_REPLACED = "TODO_COPY_LIB38_REPLACED"
        const val TODO_COPY_LIB38_CHANGE = "TODO_COPY_LIB38_CHANGE"
        const val TODO_COPY_LIB38_FIELD_DEF = "TODO_COPY_LIB38_FIELD_DEF"
        const val TODO_COPY_LIB38_TEXT_RECORD = "TODO_COPY_LIB38_TEXT_RECORD"
        const val TODO_COPY_LIB38_HIDDEN_RECORD = "TODO_COPY_LIB38_HIDDEN_RECORD"

        /** `대사 <x> · 관계 <y>`: live dialogue lines spoken by [c] and relation rows with [c] as an end. */
        fun referenceCounts(doc: ProjectDoc, c: Character): Pair<Int, Int> {
            val lines = doc.manuscript.liveBlocks.sumOf { b ->
                (if (b.kind.known == BlockKind.DIALOGUE && b.speakerKey == c.key) 1 else 0) + b.options.count { it.speakerKey == c.key }
            }
            val me = EntityRef(RelationEndKind.CHARACTER, c.key)
            return lines to doc.cast.relations.count { it.involves(me) }
        }
    }
}

/** Block deletion into the trash (R12 §7, SCR-38): one group per scene per local day; empty blocks never enter. */
object TrashGrouping {
    fun needsEntry(block: Block): Boolean = block.hasContent()

    /** The existing block group of [sceneKey] for the local day of [deletedAt], or null for a new row. */
    fun groupFor(entries: List<TrashEntry>, sceneKey: Key, deletedAt: Timestamp, zone: ZoneId): TrashEntry? {
        val day = TimeLabels.localDate(deletedAt, zone)
        return entries.firstOrNull { it.kind.known == TrashKind.BLOCK_GROUP && it.groupSceneKey == sceneKey && it.groupDate == day }
    }

    /** Restore choices for a block whose scene is not live (LIB-38, T-14); empty when it can go back in place. */
    fun restoreChoices(doc: ProjectDoc, blockKey: Key): List<String> {
        val block = doc.index.block(blockKey) ?: return emptyList()
        return if (doc.liveness.isUnitLive(block.sceneKey)) emptyList() else listOf("씬도 되살리기", "다른 씬 끝에")
    }
}

/** Name tags after permanent delete (LIB-39, R20 §5). */
object NameTags {
    fun forCharacter(c: Character, purgedAt: Timestamp): NameTag =
        NameTag(c.key, NameTagKind.CHARACTER.open(), c.name, c.voiceCode, c.hidden, NameTagState.PURGED.open(), purgedAt)

    fun of(key: Key, kind: NameTagKind, name: String, hidden: Boolean, purgedAt: Timestamp, voiceCode: String? = null): NameTag =
        NameTag(key, kind.open(), name, voiceCode, hidden, NameTagState.PURGED.open(), purgedAt)

    /** What a reference to a purged item prints: `영구 삭제 · 노아`, or `영구 삭제 · 숨긴 항목` for a hidden one. */
    fun referenceLabel(tag: NameTag): String = if (tag.hidden) "영구 삭제 · 숨긴 항목" else "영구 삭제 · ${tag.name}"
}
