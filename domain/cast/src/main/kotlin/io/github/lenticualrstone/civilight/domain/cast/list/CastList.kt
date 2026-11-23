package io.github.lenticualrstone.civilight.domain.cast.list

import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.CharacterEdits
import io.github.lenticualrstone.civilight.domain.cast.DisplayNames
import io.github.lenticualrstone.civilight.domain.cast.FirstAppearance
import io.github.lenticualrstone.civilight.domain.cast.ItemRefs
import io.github.lenticualrstone.civilight.domain.cast.ResolvedItem
import io.github.lenticualrstone.civilight.domain.cast.StoryLabels
import io.github.lenticualrstone.civilight.domain.cast.field.FieldText
import io.github.lenticualrstone.civilight.domain.cast.field.Profile
import io.github.lenticualrstone.civilight.domain.cast.field.StoryDateSource
import io.github.lenticualrstone.civilight.domain.cast.field.StoryDates
import io.github.lenticualrstone.civilight.domain.cast.timed.ReadPoint
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.cast.Tag
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc
import io.github.lenticualrstone.civilight.model.position.LensPoint

/** A tag filter chip: `전체 11` or a tag name; one is selected at a time (CST-02). */
data class TagChip(val tagKey: Key?, val label: String, val selected: Boolean)

/** A row of the character list (`ListRow .li`, CST-01). */
data class CastRow(
    val key: Key,
    val name: String,
    /** The name's first letter, shown when there is no portrait. */
    val initial: String,
    /** `17세 · 등불 결사 · 막내 등불지기`. */
    val subtitle: String,
    /** First appearance episode at the right (`1-2`), or null. */
    val firstAppearance: String?,
    /** Unlocked hidden character: a small `숨김` tag. */
    val hiddenTag: Boolean,
)

/** A tag section (`.sec`): `주연`, `등불 결사`, …; untagged characters come last under [UNTAGGED]. */
data class CastGroup(val tagKey: Key?, val title: String, val rows: List<CastRow>)

/** `숨긴 인물 1 · 지문 · PIN으로 열림 · 지문으로 보기` (rule 19 §6, `ItemHide`). */
data class LockRow(val count: Int) {
    val title: String get() = "${CastCopy.HIDDEN_CHARACTER} $count"
    val sub: String get() = "지문 · PIN으로 열림"
    val action: String get() = "지문으로 보기"
    val text: String get() = CastCopy.join(title, sub, action)
}

/** `EmptyCast`: no character yet. */
data class EmptyCast(
    val chip: String = "전체 0",
    val title: String = "인물 없음",
    val sub: String = "이름만 적어도 시작 · 나머지는 나중에",
    val primary: String = "인물 추가",
    val secondary: String = "라이프 레코더에서",
)

/** The character list screen (CST-01 … CST-02). */
data class CastListView(
    val projectName: String,
    val title: String,
    val lensChip: String,
    /** `목록 | 관계도` view modes. */
    val modes: List<String>,
    val chips: List<TagChip>,
    val groups: List<CastGroup>,
    /** Locked hidden characters collapse into one row. */
    val lockRow: LockRow?,
    /** Only when the project has no visible character at all. */
    val empty: EmptyCast?,
    /** The query matched nothing: the shared empty-search state (`결과 없음`). */
    val noResults: Boolean,
    val searchHint: String,
) {
    val rows: List<CastRow> get() = groups.flatMap { it.rows }
}

/** A card of the card view (CST-04). */
data class CastCard(
    val key: Key,
    /** The display name in effect (`???`) or the name. */
    val title: String,
    /** `하르트 · 이름 공개 1-9` or list fields and one-liner. */
    val subtitle: String,
    val firstAppearance: String?,
    /** The portrait uses the holo effect (`홀로` badge). */
    val holo: Boolean,
    val initial: String,
)

/** One horizontally scrolling row of cards: `<tag> · <count>`. */
data class CardGroup(val tagKey: Key?, val header: String, val cards: List<CastCard>)

/** A hit of the character search (`ㄹㅇ` → 리엔). */
data class CastSearchResult(val rows: List<CastRow>, val footer: String?)

/**
 * The character list, its tag chips, search and card view (CST-01, CST-02, CST-04). Characters in
 * the trash never appear; locked hidden characters collapse into one [LockRow] and are never
 * counted, matched or named.
 */
object CastList {
    const val TITLE = "인물"
    const val UNTAGGED = "태그 없음"
    const val SEARCH_HINT = "이름 · 초성 · 태그로 찾기"
    const val HIDDEN_EXCLUDED = "숨긴 항목 제외"
    val MODES = listOf("목록", "관계도")
    val VIEW_TOGGLE = listOf("목록", "카드")

    /** Live characters split into (visible, locked hidden). */
    fun visible(doc: ProjectDoc, hiddenUnlocked: Boolean): Pair<List<Character>, List<Character>> =
        CharacterEdits.live(doc).partition { ItemRefs.character(doc, it.key, hiddenUnlocked) is ResolvedItem.Live }

    /**
     * The tags the list offers as chips: project tags (in tag order) held by at least one character the
     * list shows. The project tag list is shared with the gallery (55 §3.4), so a tag used only on gallery
     * items, only by characters in the trash, or only by locked hidden characters gets no chip: its chip
     * would open an empty list, and for a locked hidden character it would tell that the character exists.
     */
    fun chipTags(doc: ProjectDoc, hiddenUnlocked: Boolean = false): List<Tag> {
        val held = visible(doc, hiddenUnlocked).first.flatMapTo(HashSet()) { it.tagKeys }
        return CharacterEdits.tags(doc).filter { it.key in held }
    }

    /** `전체 N` + one chip per tag the shown characters hold ([chipTags]), in tag order. */
    fun chips(doc: ProjectDoc, selectedTag: Key?, hiddenUnlocked: Boolean = false): List<TagChip> {
        val count = visible(doc, hiddenUnlocked).first.size
        return listOf(TagChip(null, "전체 $count", selectedTag == null)) +
            chipTags(doc, hiddenUnlocked).map { TagChip(it.key, it.name, it.key == selectedTag) }
    }

    /**
     * Name, 초성 (`ㄹㅇ`) or tag name, NFC-normalised. A blank query matches everything.
     */
    fun matches(doc: ProjectDoc, character: Character, query: String): Boolean {
        val q = query.trim().nfc()
        if (q.isEmpty()) return true
        if (Hangul.matches(character.name, q)) return true
        return CharacterEdits.liveTagNames(doc, character).any { Hangul.matches(it, q) }
    }

    fun row(doc: ProjectDoc, c: Character, lens: LensPoint, dates: StoryDateSource = StoryDates.Default): CastRow = CastRow(
        key = c.key,
        name = c.name,
        initial = c.name.take(1),
        subtitle = Profile.listSubtitle(doc, c.key, lens, dates),
        firstAppearance = FirstAppearance.label(doc, c.key),
        hiddenTag = c.hidden,
    )

    /** The first live tag of a character (the card rows of CST-04), else null ([UNTAGGED]). */
    fun groupTag(doc: ProjectDoc, c: Character): Tag? = c.tagKeys.firstNotNullOfOrNull { CharacterEdits.liveTag(doc, it) }

    /**
     * (0.2.0, CST-68, N-44) Every section a character is listed under: each of its live tags, in tag
     * order. Empty means the untagged section ([UNTAGGED]), also when every tag it holds is trashed.
     */
    fun groupTags(doc: ProjectDoc, c: Character): List<Tag> {
        val held = c.tagKeys.toSet()
        return CharacterEdits.tags(doc).filter { it.key in held }
    }

    fun build(
        doc: ProjectDoc,
        lens: LensPoint,
        selectedTag: Key? = null,
        query: String = "",
        hiddenUnlocked: Boolean = false,
        dates: StoryDateSource = StoryDates.Default,
    ): CastListView {
        val (shown, locked) = visible(doc, hiddenUnlocked)
        val filtered = shown
            .filter { selectedTag == null || selectedTag in it.tagKeys }
            .filter { matches(doc, it, query) }
        val tags = CharacterEdits.tags(doc)
        val groups = if (selectedTag != null) {
            val tag = doc.index.tag(selectedTag)
            listOf(CastGroup(selectedTag, tag?.name.orEmpty(), filtered.map { row(doc, it, lens, dates) }))
        } else {
            // CST-68: a character sits in the section of each of its live tags; `전체 N` still counts it once.
            val rows = filtered.associate { it.key to row(doc, it, lens, dates) }
            val sections = filtered.associate { it.key to groupTags(doc, it).map { t -> t.key } }
            tags.map { t -> CastGroup(t.key, t.name, filtered.filter { t.key in sections.getValue(it.key) }.map { rows.getValue(it.key) }) } +
                listOf(CastGroup(null, UNTAGGED, filtered.filter { sections.getValue(it.key).isEmpty() }.map { rows.getValue(it.key) }))
        }.filter { it.rows.isNotEmpty() }
        return CastListView(
            projectName = doc.project.name,
            title = TITLE,
            lensChip = StoryLabels.lensChip(doc, lens),
            modes = MODES,
            chips = chips(doc, selectedTag, hiddenUnlocked),
            groups = groups,
            lockRow = if (locked.isNotEmpty() && query.isBlank()) LockRow(locked.size) else null,
            empty = if (shown.isEmpty() && locked.isEmpty()) EmptyCast() else null,
            noResults = query.isNotBlank() && filtered.isEmpty(),
            searchHint = SEARCH_HINT,
        )
    }

    /**
     * Character hits of a search (command bar, `@` candidates): trashed characters never match;
     * locked hidden ones are left out and the list ends with `숨긴 항목 제외`.
     */
    fun search(doc: ProjectDoc, query: String, lens: LensPoint, hiddenUnlocked: Boolean = false): CastSearchResult {
        val (shown, locked) = visible(doc, hiddenUnlocked)
        val rows = shown.filter { query.isNotBlank() && matches(doc, it, query) }.map { row(doc, it, lens) }
        return CastSearchResult(rows, if (locked.isNotEmpty()) HIDDEN_EXCLUDED else null)
    }

    // --- Card view (CST-04) ---

    fun card(doc: ProjectDoc, c: Character, lens: LensPoint, dates: StoryDateSource = StoryDates.Default): CastCard {
        val (title, revealSub) = DisplayNames.card(doc, c.key, lens)
        val subtitle = revealSub ?: CastCopy.joinAll(
            Profile.publicDefs(doc, c.key).filter { it.inList }
                .map { FieldText.format(doc, it, Profile.reading(doc, c.key, it, ReadPoint.AtLens(lens), dates).value) }
                .filterNot { it.isEmpty }.take(2).map { it.text } + c.oneLiner,
        )
        val holo = doc.index.imageUse(ImageOwnerKind.CHARACTER, c.key, ImageSlot.CHARACTER_PORTRAIT)?.holo == true
        return CastCard(c.key, title, subtitle, FirstAppearance.label(doc, c.key), holo, c.name.take(1))
    }

    /** One card row per tag group, header `주연 · 3`. */
    fun cards(doc: ProjectDoc, lens: LensPoint, hiddenUnlocked: Boolean = false, dates: StoryDateSource = StoryDates.Default): List<CardGroup> {
        val shown = visible(doc, hiddenUnlocked).first
        val byTag = shown.groupBy { groupTag(doc, it)?.key }
        return CharacterEdits.tags(doc).mapNotNull { t ->
            byTag[t.key]?.let { list -> CardGroup(t.key, "${t.name} · ${list.size}", list.map { card(doc, it, lens, dates) }) }
        } + listOfNotNull(byTag[null]?.let { list -> CardGroup(null, "$UNTAGGED · ${list.size}", list.map { card(doc, it, lens, dates) }) })
    }
}
