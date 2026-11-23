package io.github.lenticualrstone.civilight.domain.cast.field

import io.github.lenticualrstone.civilight.model.game.GameTools
import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.DisplayNames
import io.github.lenticualrstone.civilight.domain.cast.FirstAppearance
import io.github.lenticualrstone.civilight.domain.cast.StoryLabels
import io.github.lenticualrstone.civilight.domain.cast.timed.ReadPoint
import io.github.lenticualrstone.civilight.domain.cast.timed.TimedValues
import io.github.lenticualrstone.civilight.domain.cast.voice.VoiceLines
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.isPlanned
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.text.RichDoc

/** A row of the record's field table or `항목` list (CST-07, CST-08). */
data class FieldRow(
    val label: String,
    val value: String,
    /** `1-3부터` when a record is in effect. */
    val since: String? = null,
    /** `◆`: the value has history. */
    val hasHistory: Boolean = false,
    /** Dimmed `예정 · 2-5` row under the field, with the planned value. */
    val planned: Pair<String, String>? = null,
    val defKey: Key? = null,
    val display: FieldDisplay? = null,
    /** An `이 인물만` field: shown with a small mark (CST-22). */
    val characterOnly: Boolean = false,
)

/** The record header (CST-07). */
data class RecordHeader(
    /** `인물 기록 007 · 시점 1-5`. */
    val title: String,
    val name: String,
    val oneLiner: String?,
    val rows: List<FieldRow>,
    /** `항목 더 보기` when the cover hides rows. */
    val moreLabel: String?,
    /** `예정 · 집필 선(1-7 씬 3) 뒤 기록` when a planned row is shown. */
    val plannedFootnote: String?,
)

/** A long-text section of `개요` (`Ⅱ 성격`). */
data class LongTextSection(val numeral: String, val title: String, val defKey: Key, val body: RichDoc)

/** `개요` body (CST-08). */
data class Overview(
    val items: List<FieldRow>,
    val emptyCount: Int,
    /** `빈 항목 3 · 보기`, or null when nothing is empty. */
    val emptyRow: String?,
    val addLabel: String,
    val sections: List<LongTextSection>,
)

/** `비밀` tab (CST-18): secret fields only shown here, plus the free prose body. */
data class SecretTab(val rows: List<FieldRow>, val body: RichDoc)

/** Composition of the character record, list rows and script panel lines from field readings. */
object Profile {
    const val COVER_ROWS = 6
    const val MORE = "항목 더 보기"
    const val ADD_FIELD = "+ 항목"
    private val NUMERALS = listOf("Ⅰ", "Ⅱ", "Ⅲ", "Ⅳ", "Ⅴ", "Ⅵ", "Ⅶ", "Ⅷ", "Ⅸ", "Ⅹ", "Ⅺ", "Ⅻ")

    fun numeral(n: Int): String = NUMERALS.getOrNull(n - 1) ?: n.toString()

    /** Visible, non-secret field definitions of a character in order. */
    fun publicDefs(doc: ProjectDoc, characterKey: Key): List<FieldDef> =
        FieldDefinitions.visibleFor(doc, EntityKind.CHARACTER, characterKey).filterNot { it.isSecret }

    fun reading(doc: ProjectDoc, characterKey: Key, def: FieldDef, point: ReadPoint, dates: StoryDateSource): FieldReading =
        FieldReader.read(doc, EntityKind.CHARACTER, characterKey, def, point, dates)

    /** A field row at a lens, with `부터`, `◆` and the planned row. */
    fun row(doc: ProjectDoc, characterKey: Key, def: FieldDef, lens: LensPoint, dates: StoryDateSource = StoryDates.Default): FieldRow {
        val r = reading(doc, characterKey, def, ReadPoint.AtLens(lens), dates)
        val display = FieldText.format(doc, def, r.value)
        val target = FieldReader.target(EntityKind.CHARACTER, characterKey, def)
        val next = if (r.readOnly) null else TimedValues.nextAfter(doc, target, lens)?.takeIf { doc.isPlanned(it) }
        val planned = next?.let { rec ->
            "${CastCopy.PLANNED} · ${StoryLabels.anchorPoint(doc, rec.anchor)}" to FieldText.format(doc, def, rec.valueAs(FieldValue.serializer())).text
        }
        return FieldRow(
            label = def.name,
            value = display.text,
            since = r.record?.let { "${StoryLabels.anchorPoint(doc, it.anchor)}부터" },
            hasHistory = r.hasHistory,
            planned = planned,
            defKey = def.key,
            display = display,
            characterOnly = def.scopeCharacterKey != null,
        )
    }

    /** `소속 | 등불 결사 1-3부터 ◆`: the row's value column as one string. */
    fun rowText(row: FieldRow): String = listOfNotNull(row.value, row.since, if (row.hasHistory) CastCopy.CHANGE_MARK else null).joinToString(" ")

    /**
     * The header table: non-empty `머리` fields in field order, then `첫 등장` and `보이스` unless
     * hidden in the project's cast settings. On the cover at most [COVER_ROWS] rows show, then
     * `항목 더 보기`.
     */
    fun header(
        doc: ProjectDoc,
        characterKey: Key,
        lens: LensPoint,
        cover: Boolean,
        dates: StoryDateSource = StoryDates.Default,
    ): RecordHeader {
        val c = requireNotNull(doc.index.character(characterKey))
        val settings = doc.project.cast
        val rows = buildList {
            for (def in publicDefs(doc, characterKey).filter { it.inHeader }) {
                val row = row(doc, characterKey, def, lens, dates)
                if (row.display?.isEmpty != true) add(row)
            }
            if (settings.showFirstAppearanceInHeader) {
                FirstAppearance.label(doc, characterKey)?.let { add(FieldRow("첫 등장", it)) }
            }
            // (0.2.0, CST-74) The 보이스 row belongs to the game tools: hidden while they are off, the setting kept.
            if (settings.showVoiceProgressInHeader && GameTools.on(doc)) {
                val p = VoiceLines.progress(doc, characterKey)
                if (p.total > 0) add(FieldRow("보이스", p.text))
            }
        }
        val shown = if (cover && rows.size > COVER_ROWS) rows.take(COVER_ROWS) else rows
        val footnote = if (shown.any { it.planned != null }) {
            StoryLabels.writingLineScene(doc)?.let { "${CastCopy.PLANNED} · 집필 선($it) 뒤 기록" }
        } else {
            null
        }
        return RecordHeader(
            title = "인물 기록 ${c.recordNo.toString().padStart(3, '0')} · ${StoryLabels.lensSection(doc, lens)}",
            name = c.name,
            oneLiner = c.oneLiner,
            rows = shown,
            moreLabel = if (shown.size < rows.size) MORE else null,
            plannedFootnote = footnote,
        )
    }

    /**
     * `개요`: the `항목` list (fields not `머리` and not `긴 글`), the empty-field row and the long-text
     * sections. Unknown (`??`) is not empty. [showEmpty] = `보기` (or edit mode): empty fields are listed.
     */
    fun overview(
        doc: ProjectDoc,
        characterKey: Key,
        lens: LensPoint,
        showEmpty: Boolean = false,
        dates: StoryDateSource = StoryDates.Default,
    ): Overview {
        val defs = publicDefs(doc, characterKey)
        val rows = defs.associate { it.key to row(doc, characterKey, it, lens, dates) }
        val emptyCount = rows.values.count { it.display?.isEmpty == true }
        val items = defs.filter { !it.inHeader && it.type.known != FieldType.LONG_TEXT }
            .map { rows.getValue(it.key) }
            .filter { showEmpty || it.display?.isEmpty != true }
        var n = 0
        val sections = defs.filter { it.type.known == FieldType.LONG_TEXT }.mapNotNull { def ->
            val value = reading(doc, characterKey, def, ReadPoint.AtLens(lens), dates).value
            val body = (value?.payload as? FieldPayload.Rich)?.doc
            if (body == null || FieldValueEdits.normalized(value) == null) null else LongTextSection(numeral(++n), def.name, def.key, body)
        }
        return Overview(
            items = items,
            emptyCount = emptyCount,
            emptyRow = if (emptyCount > 0) "빈 항목 $emptyCount · 보기" else null,
            addLabel = ADD_FIELD,
            sections = sections,
        )
    }

    /** `레코더 · 정리 안 됨 1 ›` beside the first section header (CST-49); null when none. */
    fun recorderLink(unsortedCount: Int): String? = if (unsortedCount > 0) "레코더 · 정리 안 됨 $unsortedCount ›" else null

    /** `비밀` tab: secret fields (non-empty) and the secret prose. */
    fun secretTab(doc: ProjectDoc, characterKey: Key, lens: LensPoint, dates: StoryDateSource = StoryDates.Default): SecretTab {
        val c = requireNotNull(doc.index.character(characterKey))
        val rows = FieldDefinitions.visibleFor(doc, EntityKind.CHARACTER, characterKey).filter { it.isSecret }
            .map { row(doc, characterKey, it, lens, dates) }
            .filter { it.display?.isEmpty != true }
        return SecretTab(rows, c.secretBody)
    }

    /**
     * List row subtitle (CST-01): up to two non-empty `목록` fields in field order, the one-liner,
     * the display-name note and `아직 등장 전`, joined by ` · `.
     */
    fun listSubtitle(doc: ProjectDoc, characterKey: Key, lens: LensPoint, dates: StoryDateSource = StoryDates.Default): String {
        val c = requireNotNull(doc.index.character(characterKey))
        val fields = publicDefs(doc, characterKey).filter { it.inList }
            .map { FieldText.format(doc, it, reading(doc, characterKey, it, ReadPoint.AtLens(lens), dates).value) }
            .filterNot { it.isEmpty }
            .take(2)
            .map { it.text }
        val notYet = if (FirstAppearance.isAfterLens(doc, characterKey, lens)) CastCopy.NOT_YET_APPEARED else null
        return CastCopy.joinAll(fields + listOf(c.oneLiner, DisplayNames.listNote(doc, characterKey, lens), notYet))
    }

    /** Script panel line (CST-38, TC-F-26): `화자 · 리엔 · 17세 · 등불 결사` from the first two `머리` values before the block. */
    fun panelLine(doc: ProjectDoc, characterKey: Key, blockKey: Key, dates: StoryDateSource = StoryDates.Default): String {
        val c = requireNotNull(doc.index.character(characterKey))
        val values = publicDefs(doc, characterKey).filter { it.inHeader }
            .map { FieldText.format(doc, it, reading(doc, characterKey, it, ReadPoint.BeforeBlock(blockKey), dates).value) }
            .filterNot { it.isEmpty }
            .take(2)
            .map { it.text }
        return CastCopy.joinAll(listOf("화자", c.name) + values)
    }
}
