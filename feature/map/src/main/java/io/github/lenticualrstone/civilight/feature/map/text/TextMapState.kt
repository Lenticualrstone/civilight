package io.github.lenticualrstone.civilight.feature.map.text

import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.domain.world.gen.ConditionReader
import io.github.lenticualrstone.civilight.domain.world.gen.GenSession
import io.github.lenticualrstone.civilight.domain.world.gen.GeneratedWorld
import io.github.lenticualrstone.civilight.domain.world.gen.KeywordConditionReader
import io.github.lenticualrstone.civilight.domain.world.gen.LanguageModelConditionReader
import io.github.lenticualrstone.civilight.domain.world.gen.ReadResult
import io.github.lenticualrstone.civilight.domain.world.gen.StepChoices
import io.github.lenticualrstone.civilight.domain.world.gen.TextConditions
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.ConditionStatus
import io.github.lenticualrstone.civilight.model.map.GenerationStep
import io.github.lenticualrstone.civilight.model.map.MapFormat
import io.github.lenticualrstone.civilight.model.map.MapStyle
import io.github.lenticualrstone.civilight.model.map.TextCondition
import io.github.lenticualrstone.civilight.model.map.TextSpan
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.ui.richtext.RichDocEdit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * One condition as saved screen state (WLD-61): 무엇 · 어디 (cells 0–8 of the 3 × 3 grid) · 특징 ·
 * 연결 장소, the author's `위치`, the phrase it came from, and whether it was read from the text
 * ([fromText]; the author's own conditions survive a new reading).
 */
@Serializable
data class CondState(
    val key: Key,
    val what: String,
    val where: List<Int> = emptyList(),
    val traits: List<String> = emptyList(),
    val placeKey: Key? = null,
    val userPosition: List<Int>? = null,
    val spanStart: Int? = null,
    val spanEnd: Int? = null,
    val fromText: Boolean = false,
) {
    fun toModel(): TextCondition = TextCondition(
        key = key, what = what.trim(), where = where.toSet(), traits = traits, placeKey = placeKey,
        status = ConditionStatus.NEEDS_CHECK.open(), userPosition = userPosition?.toSet(),
        sourceSpan = if (spanStart != null && spanEnd != null) TextSpan(spanStart, spanEnd) else null,
    )

    companion object {
        fun of(c: TextCondition, fromText: Boolean) = CondState(
            c.key, c.what, c.where.sorted(), c.traits, c.placeKey, c.userPosition?.sorted(), c.sourceSpan?.start, c.sourceSpan?.end, fromText,
        )
    }
}

/**
 * 글로 설명 state, saveable as JSON: the page (0 input, 1 result), the text, the engine (0 `기기 안`,
 * 1 `클라우드`), the `함께 읽기` sources switched off, the conditions, the seeds of the current
 * candidate and of `다른 후보 3`, and the condition highlighted on the result map.
 */
@Serializable
data class TextMapState(
    val page: Int = 0,
    val text: String = "",
    val engine: Int = 0,
    val offSources: List<String> = emptyList(),
    val conditions: List<CondState> = emptyList(),
    val seeds: List<Int> = emptyList(),
    val candidates: List<List<Int>> = emptyList(),
    val highlight: Int? = null,
    /** The last reading found no reader (cloud off, no on-device model). */
    val unreadable: Boolean = false,
    /** What was last read ([TextMapFlow.readSignature]); an unchanged input is not read again (fold, restore). */
    val readOf: String? = null,
) {
    fun encode(): String = JSON.encodeToString(serializer(), this)

    companion object {
        private val JSON = Json { ignoreUnknownKeys = true }

        fun decode(text: String?): TextMapState? = text?.let { runCatching { JSON.decodeFromString(serializer(), it) }.getOrNull() }

        /** The `장소 N` source of `함께 읽기`. */
        const val SOURCE_PLACES = "places"

        /** A page source: `page:<key>`. */
        fun pageSource(key: Key) = "page:$key"
    }
}

/** A `함께 읽기` chip: `장소 6`, `페이지 ‘하층 구역의 빛’`. */
data class SourceChip(val id: String, val label: String, val on: Boolean)

/** A condition row (DS `ConditionRow`): name = 무엇, small = 어디 · 특징 · 연결 장소. */
data class CondRowUi(val index: Int, val name: String, val small: String, val cells: Set<Int>, val applied: Boolean?)

/**
 * 글로 설명 (WLD-61) as pure functions. Reading: the on-device language model reader is a stub
 * (`needsExternal`; `LanguageModelConditionReader`), so `기기 안` falls back to the on-device keyword
 * reader; `클라우드` is a stub that sends nothing and reads nothing, so it is not offered (0.3.0). Names link only to live project
 * places (`TextConditions.resolve`: never invented). Conditions bias the generator's input; the
 * map is always generated on device and stays seed-reproducible.
 */
object TextMapFlow {
    /** The only engine offered: `기기 안` (the keyword reader on device; nothing is sent anywhere). */
    const val ON_DEVICE: Int = 0

    /** Live, unhidden pages with text, as `함께 읽기` sources. */
    fun pages(doc: ProjectDoc): List<Pair<Key, String>> = doc.pages.values
        .filter { it.deletedAt == null && !it.hidden && RichDocEdit.textOf(it.blocks).isNotBlank() }
        .sortedBy { it.order }
        .map { it.key to it.name }

    fun placeNames(doc: ProjectDoc): List<String> = doc.world.places.filter { it.deletedAt == null && !it.hidden }.map { it.name }

    fun sources(doc: ProjectDoc, s: TextMapState): List<SourceChip> =
        listOf(SourceChip(TextMapState.SOURCE_PLACES, MapCopy.sourceCount(MapCopy.SOURCE_PLACES, placeNames(doc).size), TextMapState.SOURCE_PLACES !in s.offSources)) +
            pages(doc).map { (key, name) -> TextMapState.pageSource(key).let { id -> SourceChip(id, MapCopy.sourcePage(name), id !in s.offSources) } }

    fun toggleSource(s: TextMapState, id: String): TextMapState =
        s.copy(offSources = if (id in s.offSources) s.offSources - id else s.offSources + id)

    /** The reader for the engine: the model stub first (on device or cloud), then the keyword reader on device. */
    fun readWith(engine: Int, text: String, names: List<String>): ReadResult {
        val model: ConditionReader = LanguageModelConditionReader(onDevice = engine == 0)
        val first = model.read(text, names)
        if (first is ReadResult.Read || engine != 0) return first
        return KeywordConditionReader.read(text, names)
    }

    /**
     * Reads the text (plus the pages read along) into conditions. The author's own conditions stay;
     * the earlier read ones are replaced. A reading with no reader keeps everything and marks it.
     */
    /** The input of a reading: text and sources (one engine, on device, so the stored engine is not part of it). */
    fun readSignature(s: TextMapState): String = listOf(s.text, s.offSources.sorted().joinToString(",")).joinToString("\u0001")

    /** True when [s]'s input was already read. */
    fun isRead(s: TextMapState): Boolean = s.readOf == readSignature(s)

    fun read(doc: ProjectDoc, s0: TextMapState, ctx: EditContext): TextMapState {
        val s = s0.copy(readOf = readSignature(s0))
        val names = if (TextMapState.SOURCE_PLACES in s.offSources) emptyList() else placeNames(doc)
        val pageTexts = pages(doc).filter { (k, _) -> TextMapState.pageSource(k) !in s.offSources }
            .mapNotNull { (k, _) -> doc.pages[k]?.let { RichDocEdit.textOf(it.blocks) } }
        val full = (listOf(s.text) + pageTexts).joinToString("\n")
        if (s.text.isBlank()) return s.copy(conditions = s.conditions.filterNot { it.fromText }, unreadable = false)
        // On device only (0.3.0): a stored `클라우드` (engine 1) from an older draft reads on device too.
        return when (val r = readWith(ON_DEVICE, full, names)) {
            is ReadResult.Unavailable -> s.copy(unreadable = true)
            is ReadResult.Read -> {
                val read = TextConditions.resolve(doc, r.readings, ctx).map { c ->
                    // A phrase from a page read along has no place in the typed text.
                    val inText = c.sourceSpan?.takeIf { it.end <= s.text.length }
                    CondState.of(c.copy(sourceSpan = inText), fromText = true)
                }
                s.copy(conditions = s.conditions.filterNot { it.fromText } + read, unreadable = false)
            }
        }
    }

    /** An added or edited condition (the editor `조건`); an edited read condition becomes the author's own. */
    fun upsert(s: TextMapState, c: CondState): TextMapState {
        val i = s.conditions.indexOfFirst { it.key == c.key }
        val own = c.copy(fromText = false)
        return s.copy(conditions = if (i < 0) s.conditions + own else s.conditions.mapIndexed { j, x -> if (j == i) own else x })
    }

    /** `눈 덮임 · 메마름` (or comma-separated) typed in `특징`, as traits. */
    fun traits(text: String): List<String> = text.split('·', ',').map { it.trim() }.filter { it.isNotEmpty() }

    fun remove(s: TextMapState, key: Key): TextMapState = s.copy(conditions = s.conditions.filterNot { it.key == key }, highlight = null)

    /** `위치` / `위치 고르기`: the author's cells for a condition. */
    fun setPosition(s: TextMapState, key: Key, cells: Set<Int>): TextMapState =
        s.copy(conditions = s.conditions.map { if (it.key == key) it.copy(userPosition = cells.sorted().ifEmpty { null }, fromText = false) else it })

    fun conditions(s: TextMapState): List<TextCondition> = s.conditions.map { it.toModel() }

    /** The generator settings of the current candidate (detailed terrain, `색면`). */
    fun session(s: TextMapState, style: MapStyle = MapStyle.SOFT): GenSession {
        val steps = StepChoices.ORDER.mapIndexed { i, k -> GenerationStep(k.open(), seed = s.seeds.getOrElse(i) { 0 }) }
        // No model read anything (the readers are stubs), so no model is recorded.
        return GenSession(format = MapFormat.DETAIL, style = style, steps = steps, conditions = conditions(s), textModel = null)
    }

    /** `이 조건으로 만들기`: the result page with new seeds. */
    fun make(s: TextMapState, nextSeed: () -> Int): TextMapState =
        s.copy(page = 1, seeds = StepChoices.ORDER.map { nextSeed() }, candidates = emptyList(), highlight = null)

    /** `다른 후보 3`: three more candidates to choose from. */
    fun otherCandidates(s: TextMapState, nextSeed: () -> Int): TextMapState =
        s.copy(candidates = (0 until 3).map { StepChoices.ORDER.map { nextSeed() } })

    fun chooseCandidate(s: TextMapState, index: Int): TextMapState = s.candidates.getOrNull(index)?.let { s.copy(seeds = it) } ?: s

    fun highlight(s: TextMapState, index: Int): TextMapState = s.copy(highlight = if (s.highlight == index) null else index)

    /** Input rows (no result yet). */
    fun inputRows(doc: ProjectDoc, s: TextMapState): List<CondRowUi> = s.conditions.mapIndexed { i, c ->
        val m = c.toModel()
        CondRowUi(i, m.what, TextConditions.row(doc, m).small, TextConditions.area(m), null)
    }

    /** Result rows: `반영` or `확인 필요` against [world]; unmet rows read `… → 위치 고르기`. */
    fun resultRows(doc: ProjectDoc, s: TextMapState, world: GeneratedWorld): List<CondRowUi> {
        val evaluated = TextConditions.evaluate(world, conditions(s))
        return evaluated.mapIndexed { i, c ->
            val met = c.status.known == ConditionStatus.APPLIED
            val place = c.placeKey?.let { doc.index.place(it)?.name }
            val name = if (place != null) "${c.what}${MapCopy.SEP}$place" else c.what
            val small = TextConditions.row(doc, c.copy(placeKey = null)).small
            CondRowUi(i, name, if (met) small else "$small → ${MapCopy.PICK_POSITION}", TextConditions.area(c), met)
        }
    }

    /** `조건 4 · 3 반영`. */
    fun header(s: TextMapState, world: GeneratedWorld): String = TextConditions.header(TextConditions.evaluate(world, conditions(s)))

    /** `읽어 낸 조건 · 4 | 키워드` (the engine is the keyword reader unless a model read the text). */
    fun readLine(s: TextMapState): String = TextConditions.readLine(conditions(s), null)
}
