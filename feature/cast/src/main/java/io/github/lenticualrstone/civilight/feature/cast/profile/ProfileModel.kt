package io.github.lenticualrstone.civilight.feature.cast.profile

import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.DisplayNames
import io.github.lenticualrstone.civilight.domain.cast.ItemRefs
import io.github.lenticualrstone.civilight.domain.cast.ResolvedItem
import io.github.lenticualrstone.civilight.domain.cast.StoryLabels
import io.github.lenticualrstone.civilight.domain.cast.field.FieldRow
import io.github.lenticualrstone.civilight.domain.cast.field.Overview
import io.github.lenticualrstone.civilight.domain.cast.field.Profile
import io.github.lenticualrstone.civilight.domain.cast.field.RecordHeader
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationTab
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationViews
import io.github.lenticualrstone.civilight.domain.cast.timed.TimedValues
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.common.castLens
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.isPlanned
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.navigation.ProfileTab
import kotlinx.serialization.builtins.serializer

/** The fixed fields a record edits from its `항목` list (CST-07, CST-11, CST-12). */
enum class FixedField { NAME, DISPLAY_NAME, ONE_LINER, TAGS }

/** A fixed field's row; same shape as a custom field row, keyed by [field]. */
data class FixedRow(
    val field: FixedField,
    val label: String,
    val value: String,
    val since: String? = null,
    val hasHistory: Boolean = false,
    /** Dimmed `예정 · 1-9 | 하르트` row under it. */
    val planned: Pair<String, String>? = null,
)

/** What the user chose on the record: the tab, `보기` (empty fields shown) and `항목 더 보기`. */
data class ProfileUi(
    val tab: ProfileTab = ProfileTab.OVERVIEW,
    val showEmpty: Boolean = false,
    val headerExpanded: Boolean = false,
)

/** The record at one moment (CST-07, CST-08, CST-28). */
data class ProfileState(
    val characterKey: Key,
    val name: String,
    val oneLiner: String?,
    val header: RecordHeader,
    val overview: Overview,
    /** `표시 이름`, `태그` (and, with `보기`, `이름` and `한 줄 소개`) at the top of the `항목` list. */
    val fixedRows: List<FixedRow>,
    /** Empty `긴 글` fields, listed as rows only while `보기` is on. */
    val emptyLongRows: List<FieldRow>,
    /** `빈 항목 N · 보기`. */
    val emptyLabel: String,
    val relations: RelationTab,
    val readOnly: Boolean,
)

/** Pure builder of the character record over `domain:cast` [Profile] and [RelationViews]. */
object ProfileModel {
    /** The record, or null when the character is not live (trashed, gone) or hidden and locked. */
    fun build(doc: ProjectDoc, characterKey: Key, ui: ProfileUi, cover: Boolean, hiddenUnlocked: Boolean, readOnly: Boolean): ProfileState? {
        if (ItemRefs.character(doc, characterKey, hiddenUnlocked) !is ResolvedItem.Live) return null
        val c = doc.index.character(characterKey) ?: return null
        val lens = doc.castLens
        val header = Profile.header(doc, characterKey, lens, cover = cover && !ui.headerExpanded)
        val overview = Profile.overview(doc, characterKey, lens, showEmpty = ui.showEmpty)
        val withSection = overview.sections.map { it.defKey }.toSet()
        val emptyLong = if (!ui.showEmpty) emptyList() else Profile.publicDefs(doc, characterKey)
            .filter { it.type.known == FieldType.LONG_TEXT && it.key !in withSection }
            .map { Profile.row(doc, characterKey, it, lens) }
        return ProfileState(
            characterKey = characterKey,
            name = c.name,
            oneLiner = c.oneLiner,
            header = header,
            overview = overview,
            fixedRows = fixedRows(doc, characterKey, lens, ui.showEmpty),
            emptyLongRows = emptyLong,
            emptyLabel = CastUiCopy.emptyRow(overview.emptyCount),
            relations = RelationViews.tab(doc, characterKey, lens, hiddenUnlocked),
            readOnly = readOnly,
        )
    }

    /**
     * Fixed rows: `표시 이름` while an override is set or it has history, `태그` when there are tags;
     * with `보기` all four (`이름`, `표시 이름`, `한 줄 소개`, `태그`) so each can be edited.
     */
    fun fixedRows(doc: ProjectDoc, characterKey: Key, lens: LensPoint, showEmpty: Boolean): List<FixedRow> {
        val c = doc.index.character(characterKey) ?: return emptyList()
        val target = DisplayNames.target(characterKey)
        val reading = DisplayNames.read(doc, characterKey, lens)
        val hasHistory = TimedValues.hasHistory(doc, target)
        val next = TimedValues.nextAfter(doc, target, lens)?.takeIf { doc.isPlanned(it) }
        val display = FixedRow(
            field = FixedField.DISPLAY_NAME,
            label = CastUiCopy.DISPLAY_NAME,
            value = reading.override.orEmpty(),
            since = reading.record?.let { CastUiCopy.since(StoryLabels.anchorPoint(doc, it.anchor)) },
            hasHistory = hasHistory,
            planned = next?.let { r ->
                "${CastCopy.PLANNED} · ${StoryLabels.anchorPoint(doc, r.anchor)}" to (r.valueAs(String.serializer())?.takeIf { it.isNotBlank() } ?: c.name)
            },
        )
        val tagNames = c.tagKeys.mapNotNull { doc.index.tag(it)?.name }
        val tags = FixedRow(FixedField.TAGS, CastUiCopy.TAGS, CastCopy.joinAll(tagNames))
        return if (showEmpty) {
            listOf(
                FixedRow(FixedField.NAME, CastUiCopy.NAME, c.name),
                display,
                FixedRow(FixedField.ONE_LINER, CastUiCopy.ONE_LINER, c.oneLiner.orEmpty()),
                tags,
            )
        } else {
            listOfNotNull(display.takeIf { reading.isOverridden || hasHistory }, tags.takeIf { tagNames.isNotEmpty() })
        }
    }
}
