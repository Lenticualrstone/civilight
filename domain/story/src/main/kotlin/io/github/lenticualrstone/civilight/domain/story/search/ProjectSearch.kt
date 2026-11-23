package io.github.lenticualrstone.civilight.domain.story.search

import io.github.lenticualrstone.civilight.domain.story.lens.LensView
import io.github.lenticualrstone.civilight.domain.story.text.BlockText
import io.github.lenticualrstone.civilight.domain.story.text.Sentences
import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.time.TimeLabels
import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.isLinkBeforeReveal
import io.github.lenticualrstone.civilight.model.doc.isPlanned
import io.github.lenticualrstone.civilight.model.doc.history
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.world.ItemRefKind

/** Filter chips `전체 · 인물 · 용어 · 대사 · 씬 · 명령` (single choice). */
enum class SearchFilter(val word: String) { ALL("전체"), CHARACTER("인물"), TERM("용어"), LINE("대사"), SCENE("씬"), COMMAND("명령") }

/** Sections in chip order; 장소 · 세력 · 사건 only under `전체` (`ED`). */
enum class SearchSectionKind(val title: String) {
    CHARACTER("인물"), TERM("용어"), LINE("대사"), SCENE("씬"), COMMAND("명령"), PLACE("장소"), FACTION("세력"), EVENT("사건"),
}

/** What a command row does (STO-38). */
enum class SearchCommand { CHANGE_LENS, LENS_TO_SCENE, OPEN_CHECKS, RENUMBER }

/** One result row. Tap targets: term → card, character → profile, scene → script head, line → block, command → run. */
sealed interface SearchItem {
    val key: Key?

    /** 인물: name with hit box, one-line intro (`ED`). */
    data class CharacterItem(override val key: Key, val name: Highlight, val intro: String?) : SearchItem

    /** 용어: name (or `<alias> · <name>의 별칭`) with hit box, sub `첫 공개 1-7 #044`, right `카드`. */
    data class TermItem(override val key: Key, val name: Highlight, val sub: String) : SearchItem {
        val action: String get() = "카드"
    }

    /** 씬: `1-7 씬 3 · 꺼진 등불` with hit box, sub = short 때 label. */
    data class SceneItem(override val key: Key, val title: Highlight, val sub: String) : SearchItem

    /**
     * 대사 (any game-output block): small [location] `1-4 씬 2 · 세라`, then the whole [sentence] with
     * the hits boxed inline; [beforeReveal] shows `▲ 공개 전 사용` right of the label.
     */
    data class LineItem(
        override val key: Key,
        val sceneKey: Key,
        val location: String,
        val sentence: Highlight,
        val beforeReveal: Boolean,
    ) : SearchItem {
        val flag: String? get() = if (beforeReveal) "▲ 공개 전 사용" else null
    }

    /** `› 점검 센터 열기 | 확인할 항목 3`. */
    data class CommandItem(val command: SearchCommand, val label: Highlight, val sub: String?) : SearchItem {
        override val key: Key? get() = null
        val line: String get() = listOfNotNull("› ${label.text}", sub).joinToString(" | ")
    }

    /** 장소 · 세력 · 사건 names (under `전체`). */
    data class NamedItem(val kind: SearchSectionKind, override val key: Key, val name: Highlight) : SearchItem
}

data class SearchSection(val kind: SearchSectionKind, val items: List<SearchItem>) {
    /** `용어`, `대사 · 3` (count when more than one, `ED`). */
    val header: String get() = if (items.size > 1) "${kind.title} · ${items.size}" else kind.title
}

/** A `비슷한 이름` row: `오리지늄 등불 · 용어 · 첫 공개 1-7`. */
data class SimilarName(val key: Key, val kind: SearchSectionKind, val name: String, val sub: String?) {
    val line: String get() = listOfNotNull(name, kind.title, sub).joinToString(" · ")
}

/** `결과 없음` (`EmptySearch`). */
data class EmptySearch(val query: String, val hiddenExcluded: Boolean, val similar: List<SimilarName>) {
    val title: String get() = "결과 없음"

    /** `‘오리지누’ · 숨긴 항목 제외` (`ED` inside a project). */
    val line: String get() = "‘$query’" + if (hiddenExcluded) " · 숨긴 항목 제외" else ""
    val similarHeader: String get() = "비슷한 이름"

    /** `‘오리지누’ 용어로 만들기` (world term creation with that name). */
    val createAction: String get() = "‘$query’ 용어로 만들기"
}

data class SearchResult(
    val query: String,
    val filter: SearchFilter,
    val sections: List<SearchSection>,
    /** Final line `숨긴 항목 제외` (locked, the project has hidden items). */
    val hiddenExcluded: Boolean,
    val empty: EmptySearch?,
    /**
     * Gallery images whose title, description or a tag matched (55 §3.5), filled only when the search is
     * asked for them (`includeGallery`), under `전체`. Not a [SearchSectionKind], so existing `when`s over
     * the kinds keep compiling; the screen draws them as a `갤러리` section.
     */
    val gallery: List<GalleryHit> = emptyList(),
) {
    val footer: String? get() = if (hiddenExcluded && empty == null) "숨긴 항목 제외" else null
    fun section(kind: SearchSectionKind): SearchSection? = sections.firstOrNull { it.kind == kind }
}

/**
 * Where the search is opened from: the script (no `시점 바꾸기`, but `렌즈를 이 씬으로 맞추기`), the
 * lens shown in `지금: 최신 · 1-7`, the open check count, the current episode/scene, and whether
 * hidden items are locked (LIB).
 */
data class SearchContext(
    val inScript: Boolean = false,
    val lens: LensPoint = LensPoint.Latest,
    val openCheckCount: Int = 0,
    val currentEpisodeKey: Key? = null,
    val currentSceneKey: Key? = null,
    val locked: Boolean = true,
)

/**
 * In-project search (STO-37, STO-38). Deleted items never appear; locked hidden items lose their card
 * results and hidden scenes are not searched while locked, but text in visible scenes is still found.
 * Results are derived and never stored.
 */
class ProjectSearch(val doc: ProjectDoc, val context: SearchContext = SearchContext()) {
    private val codes = UnitCodes(doc)
    private val labels = TimeLabels(SceneTimes(doc), codes)
    private val lens = LensView(doc, codes)

    private fun hiddenLocked(hidden: Boolean) = hidden && context.locked

    /** True while locked when the project has any hidden item (`숨긴 항목 제외`, `ED`). */
    val hiddenExcluded: Boolean by lazy {
        context.locked && (
            doc.cast.characters.any { it.hidden && it.deletedAt == null } ||
                doc.world.terms.any { it.hidden && it.deletedAt == null } ||
                doc.world.places.any { it.hidden && it.deletedAt == null } ||
                doc.world.factions.any { it.hidden && it.deletedAt == null } ||
                doc.manuscript.liveScenes.any { it.hidden }
            )
    }

    /** Live scenes that may be searched. */
    private val searchableScenes by lazy { doc.manuscript.liveScenes.filterNot { hiddenLocked(it.hidden) } }

    /**
     * Results for [raw] under [filter]. An empty query finds nothing, except under `명령`, which then lists
     * every command of this context (QA 2026-11-23 #11: the chip must show what it can run). With
     * [includeGallery], `전체` also searches gallery titles, descriptions and tags ([SearchResult.gallery]).
     */
    fun search(raw: String, filter: SearchFilter = SearchFilter.ALL, includeGallery: Boolean = false): SearchResult {
        val q = TextQuery(raw)
        if (q.isEmpty) {
            val sections = if (filter == SearchFilter.COMMAND) listOf(SearchSection(SearchSectionKind.COMMAND, commandList())) else emptyList()
            return SearchResult(q.text, filter, sections, hiddenExcluded, null)
        }
        val kinds = when (filter) {
            SearchFilter.ALL -> SearchSectionKind.entries
            SearchFilter.CHARACTER -> listOf(SearchSectionKind.CHARACTER)
            SearchFilter.TERM -> listOf(SearchSectionKind.TERM)
            SearchFilter.LINE -> listOf(SearchSectionKind.LINE)
            SearchFilter.SCENE -> listOf(SearchSectionKind.SCENE)
            SearchFilter.COMMAND -> listOf(SearchSectionKind.COMMAND)
        }
        val sections = kinds.mapNotNull { kind ->
            val items = when (kind) {
                SearchSectionKind.CHARACTER -> characters(q)
                SearchSectionKind.TERM -> terms(q)
                SearchSectionKind.LINE -> lines(q)
                SearchSectionKind.SCENE -> scenes(q)
                SearchSectionKind.COMMAND -> commands(q)
                SearchSectionKind.PLACE -> places(q)
                SearchSectionKind.FACTION -> factions(q)
                SearchSectionKind.EVENT -> events(q)
            }
            if (items.isEmpty()) null else SearchSection(kind, items)
        }
        val galleries = if (includeGallery && filter == SearchFilter.ALL) GallerySearch(doc, context.locked) else null
        val gallery = galleries?.search(q).orEmpty()
        val hidden = hiddenExcluded || galleries?.hiddenExcluded() == true
        val empty = if (sections.isEmpty() && gallery.isEmpty()) EmptySearch(q.text, hidden, similar(q)) else null
        return SearchResult(q.text, filter, sections, hidden, empty, gallery)
    }

    // --- Sections ----------------------------------------------------------------------------------

    private fun characters(q: TextQuery): List<SearchItem> = doc.cast.characters
        .filter { it.deletedAt == null && !hiddenLocked(it.hidden) }
        .sortedWith(compareBy({ it.order }, { it.key }))
        .mapNotNull { c -> q.highlight(c.name)?.let { SearchItem.CharacterItem(c.key, it, c.oneLiner) } }

    private fun terms(q: TextQuery): List<SearchItem> = doc.world.terms
        .filter { it.deletedAt == null && !hiddenLocked(it.hidden) }
        .sortedWith(compareBy({ it.order }, { it.key }))
        .mapNotNull { t ->
            val name = q.highlight(t.name) ?: t.aliases.firstNotNullOfOrNull { a ->
                q.highlight(a)?.let { h -> Highlight("${h.text} · ${t.name}의 별칭", h.hits) }
            } ?: return@mapNotNull null
            SearchItem.TermItem(t.key, name, termSub(t.key))
        }

    /**
     * `첫 공개 1-7 #044 · 장소: <related places>`; `공개 예정 2-5`; `첫 공개 없음`. The place part lists
     * the term's `관련` places (`ED`).
     */
    fun termSub(termKey: Key): String {
        val term = doc.index.term(termKey) ?: return ""
        val first = revealPart(termKey, withBlock = true)
        val places = term.related.filter { it.kind.known == ItemRefKind.PLACE }
            .mapNotNull { doc.index.place(it.key)?.takeIf { p -> p.deletedAt == null }?.name }
        return if (places.isEmpty()) first else "$first · 장소: ${places.joinToString(" · ")}"
    }

    /** `첫 공개 1-7 #044` (or `첫 공개 1-7` without the block), `공개 예정 2-5`, `첫 공개 없음`. */
    private fun revealPart(termKey: Key, withBlock: Boolean): String {
        val reveal = doc.history(ChangeTarget.TermReveal(termKey)).firstOrNull() ?: return "첫 공개 없음"
        return when {
            doc.isPlanned(reveal) -> "공개 예정 ${lens.anchorPoint(reveal.anchor)}"
            withBlock -> "첫 공개 ${lens.anchorLabel(reveal.anchor)}"
            else -> "첫 공개 ${lens.anchorPoint(reveal.anchor)}"
        }
    }

    private fun scenes(q: TextQuery): List<SearchItem> = searchableScenes.mapNotNull { s ->
        val title = "${codes.sceneRef(s)} · ${s.title ?: "미정"}"
        q.highlight(title)?.let { SearchItem.SceneItem(s.key, it, labels.storyColumn(s.key)) }
    }

    /** Game-output blocks, latest story position first; `#043` lists that block of the current episode (`ED`). */
    private fun lines(q: TextQuery): List<SearchItem> {
        val scenes = searchableScenes.map { it.key }.toSet()
        val blocks = doc.manuscript.liveBlocks.filter { it.sceneKey in scenes }
        val out = mutableListOf<SearchItem>()
        numberHit(q, blocks)?.let(out::add)
        for (block in blocks.asReversed()) {
            if (out.any { it.key == block.key }) continue
            val text = BlockText.gameText(block) ?: continue
            val hits = q.hits(text)
            if (hits.isEmpty()) continue
            val sentence = Sentences.around(text, hits.first().first)
            val inside = hits.filter { it.first >= sentence.first && it.last <= sentence.last }
                .map { (it.first - sentence.first)..(it.last - sentence.first) }
            out += SearchItem.LineItem(block.key, block.sceneKey, location(block), Highlight(text.substring(sentence), inside), beforeReveal(block))
        }
        return out
    }

    private fun numberHit(q: TextQuery, blocks: List<Block>): SearchItem? {
        val n = Regex("""^#(\d{1,4})$""").matchEntire(q.text)?.groupValues?.get(1)?.toInt() ?: return null
        val episode = context.currentEpisodeKey ?: return null
        val block = blocks.firstOrNull { it.number == n && doc.index.numberingUnitKeyOfScene(it.sceneKey) == episode } ?: return null
        val text = BlockText.gameText(block).orEmpty()
        val first = Sentences.split(text).firstOrNull()
        return SearchItem.LineItem(block.key, block.sceneKey, location(block), Highlight(first?.let { text.substring(it) } ?: text, emptyList()), beforeReveal(block))
    }

    /** `1-7 씬 3 · 나레이션`, `1-4 씬 2 · 세라`. */
    fun location(block: Block): String {
        val who = when (block.kind.known) {
            BlockKind.DIALOGUE -> block.speakerKey?.let { doc.index.character(it)?.name } ?: "화자 없음"
            BlockKind.NARRATION -> "나레이션"
            BlockKind.SYSTEM -> "시스템 텍스트"
            BlockKind.CHOICE -> "선택지"
            BlockKind.DIRECTION -> "연출"
            BlockKind.MEMO -> "작가 메모"
            null -> "블록"
        }
        return "${codes.sceneRef(block.sceneKey).orEmpty()} · $who"
    }

    private fun beforeReveal(block: Block): Boolean {
        val runs = block.runs + block.options.flatMap { it.runs }
        return runs.filterIsInstance<TextRun.Link>()
            .filter { it.target.kind.known == LinkTargetKind.TERM }
            .any { doc.isLinkBeforeReveal(it.target.key, block.key) }
    }

    private fun commandTriples(): List<Triple<SearchCommand, String, String?>> = buildList {
        if (!context.inScript) add(Triple(SearchCommand.CHANGE_LENS, "시점 바꾸기", "지금: ${lens.chip(context.lens).label}"))
        if (context.inScript && context.currentSceneKey != null) add(Triple(SearchCommand.LENS_TO_SCENE, "렌즈를 이 씬으로 맞추기", null))
        add(Triple(SearchCommand.OPEN_CHECKS, "점검 센터 열기", "확인할 항목 ${context.openCheckCount}"))
        context.currentEpisodeKey?.let { codes.unitLabel(it) }?.let { add(Triple(SearchCommand.RENUMBER, "번호 다시 매기기 · $it", null)) }
    }

    private fun commands(q: TextQuery): List<SearchItem> =
        commandTriples().mapNotNull { (cmd, label, sub) -> q.highlight(label)?.let { SearchItem.CommandItem(cmd, it, sub) } }

    /** Every command of this context, nothing boxed (the `명령` chip with an empty query). */
    fun commandList(): List<SearchItem> =
        commandTriples().map { (cmd, label, sub) -> SearchItem.CommandItem(cmd, Highlight(label, emptyList()), sub) }

    private fun places(q: TextQuery): List<SearchItem> = doc.world.places
        .filter { it.deletedAt == null && !hiddenLocked(it.hidden) }
        .sortedWith(compareBy({ it.order }, { it.key }))
        .mapNotNull { p -> q.highlight(p.name)?.let { SearchItem.NamedItem(SearchSectionKind.PLACE, p.key, it) } }

    private fun factions(q: TextQuery): List<SearchItem> = doc.world.factions
        .filter { it.deletedAt == null && !hiddenLocked(it.hidden) }
        .sortedWith(compareBy({ it.order }, { it.key }))
        .mapNotNull { f -> q.highlight(f.name)?.let { SearchItem.NamedItem(SearchSectionKind.FACTION, f.key, it) } }

    private fun events(q: TextQuery): List<SearchItem> = doc.world.events
        .filter { it.deletedAt == null }
        .sortedWith(compareBy({ it.order }, { it.key }))
        .mapNotNull { e -> q.highlight(e.title)?.let { SearchItem.NamedItem(SearchSectionKind.EVENT, e.key, it) } }

    /**
     * `비슷한 이름` (`ED`): live, not locked-hidden names and aliases of terms, characters, places and
     * factions whose syllable edit distance to the query is ≤ 1, or that start with the query's first
     * two syllables. One row per item.
     */
    fun similar(q: TextQuery): List<SimilarName> {
        val query = TextQuery.syllables(q.text)
        if (query.isEmpty()) return emptyList()
        val prefix = q.text.filter { !it.isWhitespace() }.take(2)
        fun close(name: String): Boolean {
            val s = TextQuery.syllables(name)
            return TextQuery.distance(query, s) <= 1 || (prefix.length == 2 && name.filter { !it.isWhitespace() }.startsWith(prefix))
        }
        val out = mutableListOf<SimilarName>()
        for (t in doc.world.terms.filter { it.deletedAt == null && !hiddenLocked(it.hidden) }) {
            if ((listOf(t.name) + t.aliases).any(::close)) {
                out += SimilarName(t.key, SearchSectionKind.TERM, t.name, revealPart(t.key, withBlock = false))
            }
        }
        for (c in doc.cast.characters.filter { it.deletedAt == null && !hiddenLocked(it.hidden) }) {
            if (close(c.name)) out += SimilarName(c.key, SearchSectionKind.CHARACTER, c.name, null)
        }
        for (p in doc.world.places.filter { it.deletedAt == null && !hiddenLocked(it.hidden) }) {
            if (close(p.name)) out += SimilarName(p.key, SearchSectionKind.PLACE, p.name, null)
        }
        for (f in doc.world.factions.filter { it.deletedAt == null && !hiddenLocked(it.hidden) }) {
            if (close(f.name)) out += SimilarName(f.key, SearchSectionKind.FACTION, f.name, null)
        }
        return out
    }
}
