package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.LibraryIndexFile
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.change.RelationSlotName
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecordKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import io.github.lenticualrstone.civilight.model.trash.TrashReason
import java.time.temporal.ChronoUnit

/** Trash filter chips (LIB-38, rule 20 §5): `세계` = 장소 · 용어 · 세력 (+ 지도, 사건); field definitions under `인물`. */
enum class TrashFilter(val label: String) {
    ALL("전체"),
    PROJECT("프로젝트"),
    SCENE("씬"),
    BLOCK("블록"),
    CHARACTER("인물"),
    WORLD("세계"),
    CHANGE("변화"),
    RECORDER("레코더");

    fun matches(kind: OpenEnum<TrashKind>): Boolean = when (this) {
        ALL -> true
        PROJECT -> kind.known == TrashKind.PROJECT
        SCENE -> kind.known == TrashKind.SCENE
        BLOCK -> kind.known == TrashKind.BLOCK_GROUP
        CHARACTER -> kind.known == TrashKind.CHARACTER || kind.known == TrashKind.FIELD_DEF
        WORLD -> kind.known in setOf(TrashKind.PLACE, TrashKind.TERM, TrashKind.FACTION, TrashKind.MAP, TrashKind.EVENT)
        CHANGE -> kind.known == TrashKind.CHANGE
        RECORDER -> kind.known == TrashKind.RECORDER
    }
}

/** One line of an expanded block group (`#043` + first line, its own `되살리기`). */
data class TrashBlockLine(val blockKey: Key, val number: Int, val firstLine: String)

/** One trash row (LIB-38): title, sub line, `<d>일 남음`, chips `되살리기 · 영구 삭제`. */
data class TrashRow(
    val entryKey: Key,
    val kind: OpenEnum<TrashKind>,
    val projectKey: Key?,
    val title: String,
    val sub: String,
    val daysLeft: Int,
    val deletedAt: Timestamp,
    /** Shown in the masked form (`숨긴 인물`, `숨긴 프로젝트`). */
    val masked: Boolean,
    /** Hidden things ask for authentication before `되살리기` (LIB-38). */
    val restoreNeedsAuth: Boolean,
    val lines: List<TrashBlockLine> = emptyList(),
) {
    val daysLeftLabel: String get() = TrashCopy.daysLeft(daysLeft)
    val actions: List<String> get() = listOf(TrashCopy.RESTORE, TrashCopy.PURGE)
}

/** The trash host's list (LIB-38): rows of every source, filters, countdown, masking. */
object TrashListing {
    /** The chips, in order: `전체 <n>` · `프로젝트` · `씬` · `블록` · `인물` · `세계` · `변화` · `레코더` (T-10). */
    fun chips(rows: List<TrashRow>): List<String> =
        TrashFilter.entries.map { if (it == TrashFilter.ALL) "${it.label} ${rows.size}" else it.label }

    /** Rows of [filter], newest deletion first (`ED`). */
    fun list(rows: List<TrashRow>, filter: TrashFilter): List<TrashRow> =
        rows.filter { filter.matches(it.kind) }.sortedWith(compareByDescending<TrashRow> { it.deletedAt }.thenBy { it.entryKey })

    /** Rows of one project's entries. [recorder] adds `레코더` reference counts where rows show counts. */
    fun projectItemRows(
        doc: ProjectDoc,
        ctx: TrashContext,
        locks: HidingLocks = HidingLocks(),
        recorder: RecorderLibrary? = null,
    ): List<TrashRow> {
        val projectMasked = locks.projectLocked && doc.project.protections.hidden
        val location = if (projectMasked) TrashCopy.HIDDEN_PROJECT else doc.project.name
        val codes = UnitCodes(doc)
        return doc.trash.entries.map { entry ->
            val kind = ItemKind.of(entry.kind)
            val key = entry.targetKeys.firstOrNull()
            val item = if (kind != null && key != null) ItemId(kind, key) else null
            val hiddenItem = entry.hiddenAtDelete || (item != null && doc.itemHidden(item))
            val masked = projectMasked || (hiddenItem && locks.itemsLocked)
            val (title, sub) = when {
                kind == null || item == null -> "" to location
                kind == ItemKind.BLOCK -> blockGroupCopy(doc, entry, codes, location, ctx)
                masked && kind != ItemKind.UNIT -> TrashCopy.hiddenKind(kind) to
                    TrashCopy.join(TrashCopy.kindWord(kind), location, TrashCopy.RESTORE_NEEDS_AUTH)
                else -> itemCopy(doc, entry, item, codes, location, masked, recorder)
            }
            TrashRow(
                entryKey = entry.key, kind = entry.kind, projectKey = entry.projectKey ?: doc.key,
                title = title, sub = sub, daysLeft = ctx.daysLeft(entry.deletedAt), deletedAt = entry.deletedAt,
                masked = masked, restoreNeedsAuth = hiddenItem || doc.project.protections.hidden,
                lines = if (kind == ItemKind.BLOCK && !masked) blockLines(doc, entry) else emptyList(),
            )
        }
    }

    /** Recorder trash rows (`음성 0:12` · `라이프 레코더`; text records `글`; hidden `숨긴 기록`). */
    fun recorderRows(library: RecorderLibrary, entries: List<TrashEntry>, ctx: TrashContext, recordsLocked: Boolean = true): List<TrashRow> =
        entries.filter { it.kind.known == TrashKind.RECORDER }.map { entry ->
            val record = entry.targetKeys.firstOrNull()?.let(library::record)
            val hidden = entry.hiddenAtDelete || record?.hidden == true
            val masked = hidden && recordsLocked
            val title = when {
                masked -> "숨긴 기록"
                record?.kind?.known == RecordKind.VOICE -> "음성 ${duration(record.durationMs ?: 0L)}"
                else -> "글"
            }
            TrashRow(
                entryKey = entry.key, kind = entry.kind, projectKey = null, title = title, sub = TrashCopy.RECORDER,
                daysLeft = ctx.daysLeft(entry.deletedAt), deletedAt = entry.deletedAt, masked = masked, restoreNeedsAuth = hidden,
            )
        }

    /**
     * Project rows (`<name>` · `프로젝트 · 씬 <s>개 · 인물 <c>명`); counts come from [docs] when
     * given. A hidden project reads `숨긴 프로젝트` while its group is locked.
     */
    fun projectRows(
        entries: List<TrashEntry>,
        library: LibraryIndexFile,
        docs: Map<Key, ProjectDoc>,
        ctx: TrashContext,
        hiddenGroupLocked: Boolean = true,
    ): List<TrashRow> = entries.filter { it.kind.known == TrashKind.PROJECT }.map { entry ->
        val key = entry.targetKeys.firstOrNull()
        val row = library.projects.firstOrNull { it.key == key }
        val doc = key?.let(docs::get)
        val hidden = entry.hiddenAtDelete || row?.hidden == true
        val masked = hidden && hiddenGroupLocked
        val counts = doc?.let { d ->
            listOf("씬 ${d.manuscript.liveScenes.size}개", "인물 ${d.cast.characters.count { it.deletedAt == null }}명")
        }.orEmpty()
        TrashRow(
            entryKey = entry.key, kind = entry.kind, projectKey = key,
            title = if (masked) TrashCopy.HIDDEN_PROJECT else row?.name ?: doc?.project?.name.orEmpty(),
            sub = TrashCopy.join(listOf("프로젝트") + if (masked) listOf(TrashCopy.RESTORE_NEEDS_AUTH) else counts),
            daysLeft = ctx.daysLeft(entry.deletedAt), deletedAt = entry.deletedAt, masked = masked, restoreNeedsAuth = hidden,
        )
    }

    // --- Row copy ---

    private fun itemCopy(
        doc: ProjectDoc,
        entry: TrashEntry,
        item: ItemId,
        codes: UnitCodes,
        location: String,
        masked: Boolean,
        recorder: RecorderLibrary?,
    ): Pair<String, String> {
        val merged = entry.reason.known == TrashReason.MERGED
        val mergedInto = entry.mergedIntoKey?.let { doc.itemName(ItemId(item.kind, it)) }?.let { "합침 → $it" }
        return when (item.kind) {
            ItemKind.UNIT -> {
                val code = codes.code(item.key).orEmpty()
                val title = if (masked) TrashCopy.join(code, TrashCopy.hiddenKind(ItemKind.UNIT)) else codes.codeAndTitle(item.key).orEmpty()
                title to TrashCopy.join(TrashCopy.kindWord(ItemKind.UNIT), location)
            }
            ItemKind.CHARACTER -> {
                val counts = if (merged) {
                    listOf(mergedInto)
                } else {
                    val impact = Impact.of(doc, item, recorder)
                    listOf(countPart("대사", impact.count(RefKind.SPEAKER)), countPart("관계", impact.owned(OwnedKind.RELATION)))
                }
                doc.itemName(item).orEmpty() to TrashCopy.join(listOf(TrashCopy.kindWord(item.kind), location) + counts)
            }
            ItemKind.PLACE, ItemKind.TERM, ItemKind.FACTION -> {
                val counts = if (merged) {
                    listOf(mergedInto)
                } else {
                    listOf(countPart("참조", Impact.of(doc, item, recorder).referenceTotal))
                }
                doc.itemName(item).orEmpty() to TrashCopy.join(listOf(TrashCopy.kindWord(item.kind), location) + counts)
            }
            ItemKind.CHANGE -> changeTitle(doc, item.key) to TrashCopy.join(TrashCopy.kindWord(ItemKind.CHANGE), location)
            else -> doc.itemName(item).orEmpty() to TrashCopy.join(TrashCopy.kindWord(item.kind), location)
        }
    }

    private fun countPart(word: String, count: Int): String? = if (count > 0) "$word $count" else null

    /** `◆ 세라 · 코드네임` (`ED`: LIB-38 has no copy for a `변화` row). */
    internal fun changeTitle(doc: ProjectDoc, recordKey: Key): String {
        val record = doc.index.record(recordKey) ?: return "◆"
        val label = when (val t = record.target) {
            is ChangeTarget.Field -> {
                val entity = t.entityKind.known?.let { doc.index.nameOf(it, t.entityKey) }
                TrashCopy.join(entity, doc.index.fieldDef(t.fieldDefKey)?.name)
            }
            is ChangeTarget.DisplayName -> TrashCopy.join(doc.index.character(t.characterKey)?.name, "표시 이름")
            is ChangeTarget.RelationSlot -> doc.index.relation(t.relationKey)?.let { r ->
                val a = doc.index.character(r.endA.key)?.name ?: doc.index.faction(r.endA.key)?.name
                val b = doc.index.character(r.endB.key)?.name ?: doc.index.faction(r.endB.key)?.name
                TrashCopy.join(a, b, slotWord(t.slot.known))
            }.orEmpty()
            is ChangeTarget.RegionShape -> TrashCopy.join(doc.index.region(t.regionKey)?.second?.name, "모양")
            is ChangeTarget.TermReveal -> return "◇ 첫 공개 · ${doc.index.term(t.termKey)?.name.orEmpty()}"
            is ChangeTarget.Unknown -> ""
        }
        return "◆ $label".trimEnd()
    }

    private fun slotWord(slot: RelationSlotName?): String = when (slot) {
        RelationSlotName.BETWEEN -> "사이"
        RelationSlotName.MIND_A, RelationSlotName.MIND_B -> "마음"
        RelationSlotName.ADDRESS_A, RelationSlotName.ADDRESS_B -> "호칭"
        null -> ""
    }

    private fun blockGroupCopy(doc: ProjectDoc, entry: TrashEntry, codes: UnitCodes, location: String, ctx: TrashContext): Pair<String, String> {
        val scene = entry.groupSceneKey ?: entry.targetKeys.firstOrNull()?.let { doc.index.block(it)?.sceneKey }
        val code = scene?.let(codes::code).orEmpty()
        val title = TrashCopy.join(code, "블록 ${entry.targetKeys.size}개")
        return title to TrashCopy.join(TrashCopy.kindWord(ItemKind.BLOCK), location, dayWord(entry, ctx))
    }

    /** `오늘 지움` · `어제 지움` · `11월 15일 지움` (`ED` day words). */
    internal fun dayWord(entry: TrashEntry, ctx: TrashContext): String {
        val day = entry.groupDate ?: ctx.localDate(entry.deletedAt)
        return when (ChronoUnit.DAYS.between(day, ctx.today)) {
            0L -> "오늘 지움"
            1L -> "어제 지움"
            else -> "${day.monthValue}월 ${day.dayOfMonth}일 지움"
        }
    }

    private fun blockLines(doc: ProjectDoc, entry: TrashEntry): List<TrashBlockLine> =
        entry.targetKeys.mapNotNull(doc.index::block)
            .sortedWith(compareBy({ it.trashPlacement?.ordinal ?: 0 }, { it.number }))
            .map { TrashBlockLine(it.key, it.number, it.firstLine()) }

    private fun duration(ms: Long): String {
        val seconds = ms / 1000
        return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
    }
}
