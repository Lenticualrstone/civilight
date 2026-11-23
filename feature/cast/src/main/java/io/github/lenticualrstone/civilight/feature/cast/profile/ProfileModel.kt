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
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationReader
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationTab
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationViews
import io.github.lenticualrstone.civilight.domain.cast.timed.TimedValues
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.common.castLens
import io.github.lenticualrstone.civilight.feature.cast.image.CharacterImages
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.feature.cast.trash.ItemTrash
import io.github.lenticualrstone.civilight.domain.trash.Fixes
import io.github.lenticualrstone.civilight.model.nfc
import io.github.lenticualrstone.civilight.domain.trash.ItemId
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.isPlanned
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.navigation.ProfileTab
import kotlinx.serialization.builtins.serializer

/** The fixed fields a record edits from its `항목` list (CST-07, CST-11, CST-12). */
enum class FixedField { NAME, DISPLAY_NAME, ONE_LINER, PORTRAIT, BACKGROUND, TAGS }

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
    /** Relation of each deleted partner's dotted row (its `이대로 두기` acknowledges that one row). */
    val deletedPartnerRelations: Map<Key, Key> = emptyMap(),
    /** Deleted partners whose row was kept (`이대로 두기`): dotted, without fix buttons. */
    val keptPartners: Set<Key> = emptySet(),
    /** Hidden (CST-39): the record is open, so the gate is; the head shows the `숨김` tag. */
    val hidden: Boolean = false,
    /** `◇ 같은 이름 2 · 노아` while another live character has exactly this name (CST-46), with `합치기`. */
    val sameName: String? = null,
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
        val relations = RelationViews.tab(doc, characterKey, lens, hiddenUnlocked)
        val deletedRelations = relations.deletedRows.mapNotNull { row ->
            RelationReader.between(doc, characterKey, row.partnerKey)?.let { row.partnerKey to it.key }
        }.toMap()
        return ProfileState(
            characterKey = characterKey,
            name = c.name,
            oneLiner = c.oneLiner,
            header = header,
            overview = overview,
            fixedRows = fixedRows(doc, characterKey, lens, ui.showEmpty),
            emptyLongRows = emptyLong,
            emptyLabel = CastUiCopy.emptyRow(overview.emptyCount),
            relations = relations,
            readOnly = readOnly,
            deletedPartnerRelations = deletedRelations,
            keptPartners = deletedRelations.filter { (partner, relation) -> ItemTrash.isKept(doc, partner, relation) }.keys,
            hidden = c.hidden,
            sameName = sameNameNotice(doc, characterKey, hiddenUnlocked),
        )
    }

    /**
     * `◇ 같은 이름 2 · 노아` (CST-46, `domain:trash` [Fixes.sameNameNotice]). While hidden characters are
     * locked only visible ones count, so the row never reveals a hidden twin (rule 19 §6).
     */
    fun sameNameNotice(doc: ProjectDoc, characterKey: Key, hiddenUnlocked: Boolean): String? {
        if (hiddenUnlocked) return Fixes.sameNameNotice(doc, ItemId.character(characterKey))
        val c = doc.index.character(characterKey) ?: return null
        val name = c.name.nfc()
        val count = doc.cast.characters.count { it.deletedAt == null && !it.hidden && it.name.nfc() == name }
        return if (count < 2) null else CastUiCopy.sameName(count, c.name)
    }

    /**
     * Fixed rows: `표시 이름` while an override is set or it has history, the image slots that hold an
     * image, `태그` when there are tags; with `보기` all of them (`이름`, `표시 이름`, `한 줄 소개`,
     * `인물 초상`, `인물 배경`, `태그`) so each can be edited.
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
        val tagNames = io.github.lenticualrstone.civilight.domain.cast.CharacterEdits.liveTagNames(doc, c)
        val tags = FixedRow(FixedField.TAGS, CastUiCopy.TAGS, CastCopy.joinAll(tagNames))
        // CST-13: `인물 초상 · 인물 배경` read `잿빛 새벽` / `내 이미지`, or `이미지 고르기` while empty.
        val images = CharacterImages.slotRows(doc, characterKey).map { row ->
            FixedRow(if (row.slot == ImageSlot.CHARACTER_PORTRAIT) FixedField.PORTRAIT else FixedField.BACKGROUND, row.label, row.value) to row.chosen
        }
        return if (showEmpty) {
            listOf(
                FixedRow(FixedField.NAME, CastUiCopy.NAME, c.name),
                display,
                FixedRow(FixedField.ONE_LINER, CastUiCopy.ONE_LINER, c.oneLiner.orEmpty()),
            ) + images.map { it.first } + tags
        } else {
            listOfNotNull(display.takeIf { reading.isOverridden || hasHistory }) +
                images.filter { it.second }.map { it.first } +
                listOfNotNull(tags.takeIf { tagNames.isNotEmpty() })
        }
    }
}
