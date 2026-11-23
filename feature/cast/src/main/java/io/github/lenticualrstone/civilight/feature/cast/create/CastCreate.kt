package io.github.lenticualrstone.civilight.feature.cast.create

import io.github.lenticualrstone.civilight.designsystem.component.CreateCard
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardMode
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardState
import io.github.lenticualrstone.civilight.designsystem.component.CreateField
import io.github.lenticualrstone.civilight.designsystem.component.CreateFieldType
import io.github.lenticualrstone.civilight.designsystem.component.CreateFields
import io.github.lenticualrstone.civilight.designsystem.component.CreateKind
import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.actions.TagActions
import io.github.lenticualrstone.civilight.domain.cast.CharacterEdits
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.feature.cast.list.CastListUi
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc

/** What `만들기` / `만들고 열기` did: the new item's key and whether to open it. */
data class Created(val key: Key, val open: Boolean)

/**
 * (0.2.0, U-15, CST-66, CST-75) The cast's create cards over the shared DS-72 card: field sets, the
 * values the card holds, and the one write each create makes. Pure, JVM tested.
 *
 * Fields that need a control `core:designsystem` / `core:ui` do not offer yet are left out of the card
 * rather than drawn without a working control (release rule: nothing visible that does nothing):
 * - `이미지` (CST-66): the shared image pick needs an owner key, and the character does not exist
 *   before `만들기`, so the pick cannot be held by the card. Requested.
 * - `색` (CST-75): there is no tag palette in `core:designsystem`. Requested.
 */
object CastCreate {
    /** Multi-pick values are stored in the card as keys joined with this separator. */
    private const val SEP = ","

    /** Field types the cast cards can draw today. */
    private val DRAWN = setOf(CreateFieldType.TEXT, CreateFieldType.MULTI_PICK)

    /** The fields the card shows for [kind], in the DS-72 order, without the ones listed above. */
    fun fields(kind: CreateKind): List<CreateField> = kind.fields.filter { it.type in DRAWN }

    /** Hidden fields of [kind] (reported, never drawn). */
    fun hiddenFields(kind: CreateKind): List<CreateField> = kind.fields.filterNot { it.type in DRAWN }

    /** A new character card: from a list row (empty), or from a picker with its query as the name (U-15.3). */
    fun openCharacter(mode: CreateCardMode, query: String? = null): CreateCardState = CreateCardState.open(CreateKind.CHARACTER, mode, query)

    /**
     * The name a picker's card opens with (U-15.3): the trimmed query when it matched nothing (the row read
     * `‘세라’ 새 인물`), else nothing.
     */
    fun pickerQuery(query: String, matched: Boolean): String? = query.trim().takeIf { !matched && it.isNotEmpty() }

    /** The tag keys checked on a character card. */
    fun tagKeys(state: CreateCardState): List<Key> =
        state.values[CreateFields.tags.key].orEmpty().split(SEP).filter { it.isNotBlank() }

    /** Tapping a tag chip on the card: on when off, off when on. */
    fun toggleTag(state: CreateCardState, tagKey: Key): CreateCardState {
        val now = tagKeys(state)
        val next = if (tagKey in now) now - tagKey else now + tagKey
        return state.with(CreateFields.tags.key, next.joinToString(SEP))
    }

    /** `만들기` / `만들고 열기` are on while the name is not blank (U-15.5). */
    fun canCreate(state: CreateCardState): Boolean = CreateCard.canCreate(state.name)

    /**
     * The character write (CST-66): one edit through `CharacterEdits.create` with the name (trimmed, NFC),
     * `표시 이름` as the base display name, `한 줄 소개` and the checked live tags. Null for a blank name.
     */
    fun createCharacter(doc: ProjectDoc, state: CreateCardState, ctx: EditContext): Pair<ProjectDoc, Key>? {
        if (!canCreate(state)) return null
        val (next, c) = CharacterEdits.create(
            doc,
            state.name,
            ctx,
            oneLiner = state.values[CreateFields.oneLineIntro.key],
            displayNameBase = state.values[CreateFields.displayName.key],
            tagKeys = tagKeys(state),
        )
        return next to c.key
    }

    /**
     * After `만들기` on the list (CST-66, §8.3 Q46): if a tag chip filter would hide the new row, the filter
     * returns to `전체`; nothing is pre-checked from the filter. A typed search or field filter is handled by
     * [revealCreated]: kept when the new row matches it, cleared when it would hide the row.
     */
    fun afterCreate(ui: CastListUi, newTags: List<Key>): CastListUi =
        if (ui.selectedTag != null && ui.selectedTag !in newTags) ui.copy(selectedTag = null) else ui

    /**
     * (CST-66) The list shows the new row after `만들기`: the tag chip as [afterCreate]; then, only when the
     * row is still not drawn, the typed search is cleared, then the field filters (the sort stays). A search
     * or filter the new row matches is kept. [ui] unchanged when the row is not in [doc] yet.
     */
    fun revealCreated(doc: ProjectDoc, ui: CastListUi, key: Key, hiddenUnlocked: Boolean, readOnly: Boolean): CastListUi {
        val c = doc.index.character(key) ?: return ui
        fun shows(u: CastListUi): Boolean {
            val s = io.github.lenticualrstone.civilight.feature.cast.list.CastListModel.build(doc, u, hiddenUnlocked, readOnly)
            val keys = io.github.lenticualrstone.civilight.feature.cast.list.CastListModel.itemKeys(s, io.github.lenticualrstone.civilight.feature.cast.list.CastListModel.showsCreate(s))
            return io.github.lenticualrstone.civilight.feature.cast.list.CastListModel.firstRowIndex(keys, s, key) >= 0
        }
        var next = afterCreate(ui, c.tagKeys)
        if (shows(next)) return next
        next = next.copy(query = "", searching = false)
        if (shows(next)) return next
        return next.copy(filters = next.filters.copy(selected = emptyMap(), ranges = emptyMap()))
    }

    // --- 관계 종류 (CST-76) ---

    /** The relation-kind card's fields: `이름`, plus `양쪽에서 부르는 이름` for a `사이` kind only. */
    fun kindFields(between: Boolean): List<CreateField> =
        fields(CreateKind.RELATION_KIND).filter { between || it.key != CreateFields.bothSides.key }

    /**
     * `만들기` of the relation-kind card from `관계 종류` settings (CST-76): the category is the section's
     * (never asked); a `사이` kind with the other side's name filled is a 짝 kind, empty a 대칭 one, a
     * `마음` kind has a name only. One write through `RelationKinds.add`; null for a blank name.
     */
    fun createKind(
        doc: ProjectDoc,
        category: io.github.lenticualrstone.civilight.model.cast.RelationCategory,
        state: CreateCardState,
        ctx: EditContext,
    ): Pair<ProjectDoc, Key>? {
        if (!canCreateKind(doc, category, state)) return null
        val between = category == io.github.lenticualrstone.civilight.model.cast.RelationCategory.BETWEEN
        val pair = pairName(state).takeIf { between }
        val (next, kind) = io.github.lenticualrstone.civilight.domain.cast.relation.RelationKinds.add(
            doc, category, state.name, ctx,
            symmetry = if (!between) null else if (pair != null) io.github.lenticualrstone.civilight.model.cast.RelationSymmetry.PAIR else io.github.lenticualrstone.civilight.model.cast.RelationSymmetry.SYMMETRIC,
            pairName = pair,
        )
        return next to kind.key
    }

    /**
     * `만들기` of the settings kind card is on while the name is not blank and no live kind of the section's
     * category has it (the relation sheet's card picks the existing kind instead; here the row is already listed).
     */
    fun canCreateKind(
        doc: ProjectDoc,
        category: io.github.lenticualrstone.civilight.model.cast.RelationCategory,
        state: CreateCardState,
    ): Boolean {
        if (!canCreate(state)) return false
        val name = state.name.trim().nfc()
        return io.github.lenticualrstone.civilight.domain.cast.relation.RelationKinds.of(doc, category).none { it.name.nfc() == name }
    }

    /** The other side's name typed on a kind card (`선배 ↔ [후배]`), or null. */
    fun pairName(state: CreateCardState): String? = state.values[CreateFields.bothSides.key]?.trim()?.takeIf { it.isNotEmpty() }

    // --- 태그 (CST-75) ---

    /** `만들기` of a tag card is on while the name is not blank and no **live** tag has it. */
    fun canCreateTag(doc: ProjectDoc, state: CreateCardState): Boolean =
        canCreate(state) && TagActions.canName(doc, state.name)

    /** The tag write: `TagActions.create` (DECISIONS §1). No colour until the palette exists. */
    fun createTag(doc: ProjectDoc, state: CreateCardState, ctx: ActionContext): Pair<ProjectDoc, Key>? =
        if (!canCreateTag(doc, state)) null else TagActions.create(doc, state.name.trim().nfc(), null, ctx)?.let { (d, t) -> d to t.key }

    /**
     * From the character `태그` sheet (picker mode): the tag is created and checked on the character at
     * once, in one write (one undo step).
     */
    fun createTagOn(doc: ProjectDoc, characterKey: Key, state: CreateCardState, ctx: ActionContext): Pair<ProjectDoc, Key>? {
        val (created, key) = createTag(doc, state, ctx) ?: return null
        val c = created.index.character(characterKey) ?: return null
        return CharacterEdits.setTags(created, c.key, c.tagKeys + key, ctx.edit) to key
    }
}

/** Saves an open create card with its screen (`rememberSaveable` of a string): kind, mode, typed values. */
object CreateCardCodec {
    @kotlinx.serialization.Serializable
    private data class Saved(val kind: String, val mode: String, val values: Map<String, String>)

    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    fun encode(state: CreateCardState): String =
        json.encodeToString(Saved.serializer(), Saved(state.kind.name, state.mode.name, state.values))

    fun decode(text: String): CreateCardState? = if (text.isBlank()) {
        null
    } else {
        runCatching {
            val saved = json.decodeFromString(Saved.serializer(), text)
            CreateCardState(CreateKind.valueOf(saved.kind), CreateCardMode.valueOf(saved.mode), saved.values)
        }.getOrNull()
    }
}
