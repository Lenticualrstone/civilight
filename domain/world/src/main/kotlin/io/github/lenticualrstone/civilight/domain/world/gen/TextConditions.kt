package io.github.lenticualrstone.civilight.domain.world.gen

import io.github.lenticualrstone.civilight.domain.world.common.Copy
import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.ConditionStatus
import io.github.lenticualrstone.civilight.model.map.TextCondition
import io.github.lenticualrstone.civilight.model.map.TextModelInfo
import io.github.lenticualrstone.civilight.model.map.TextSpan
import io.github.lenticualrstone.civilight.model.map.Terrain
import io.github.lenticualrstone.civilight.model.open

/** What a condition asks the generator for (WLD-61). [words] are the 무엇 words that map to it. */
enum class ConditionKind(val words: List<String>) {
    PORT(listOf("항구 도시", "항구", "포구")),
    MOUNTAINS(listOf("산맥", "산지", "봉우리", "산")),
    DESERT(listOf("사막", "황무지")),
    FOREST(listOf("숲", "밀림", "삼림")),
    SWAMP(listOf("늪", "습지")),
    SNOW(listOf("설원", "빙원", "눈밭")),
    LAKE(listOf("호수")),
    RIVER(listOf("강", "하천")),
    SEA(listOf("바다", "해안", "만")),
    TOWN(listOf("도시", "마을", "성채", "요새")),
    PLAIN(listOf("평원", "초원", "들판"));

    companion object {
        /** The kind of a 무엇 text, or null when the generator cannot act on it (`확인 필요`). */
        fun of(what: String): ConditionKind? {
            val clean = what.trim()
            return entries.firstOrNull { k -> k.words.any { it == clean } }
        }
    }
}

/** One condition as a reader proposes it, before names are checked against the project. */
data class ConditionReading(
    val what: String,
    val where: Set<Int>,
    val traits: List<String> = emptyList(),
    /** A place name the reader saw; kept only if a live project place has exactly this name. */
    val placeName: String? = null,
    val span: IntRange? = null,
)

/** Result of reading a description. */
sealed interface ReadResult {
    data class Read(val readings: List<ConditionReading>, val model: TextModelInfo?) : ReadResult

    /** No reader is available (no on-device model, cloud off): nothing is invented. */
    data class Unavailable(val reason: String) : ReadResult
}

/** Reads `글로 설명` text into conditions. [placeNames] are the project records read along (`함께 읽기`). */
fun interface ConditionReader {
    fun read(text: String, placeNames: List<String>): ReadResult
}

/**
 * The on-device language-model reader of WLD-61 (`needsExternal`). Stub: no model is bundled, so it
 * always reports [ReadResult.Unavailable]; the keyword reader is the working fallback.
 */
class LanguageModelConditionReader(private val onDevice: Boolean) : ConditionReader {
    override fun read(text: String, placeNames: List<String>): ReadResult =
        ReadResult.Unavailable(if (onDevice) "기기 안 모델 없음" else "클라우드 읽기 꺼짐")
}

/**
 * A simple keyword reader (on device, no model): splits the text into clauses at punctuation, and
 * in each clause finds a direction (`북쪽`, `남동쪽`, `가운데` …; never a bare `서` as in `에서`), the
 * first 무엇 word standing as a word (a one-syllable word only before a particle: `강에서`, not `강한`), trait words
 * (`눈 덮인` → `눈 덮임`) and the longest project place name. A clause without a 무엇 word yields
 * nothing; a clause without a direction covers the whole map.
 */
object KeywordConditionReader : ConditionReader {
    private val DIRECTIONS: List<Pair<String, Set<Int>>> = listOf(
        "북동쪽" to setOf(2), "북서쪽" to setOf(0), "남동쪽" to setOf(8), "남서쪽" to setOf(6),
        "북쪽" to setOf(0, 1, 2), "남쪽" to setOf(6, 7, 8), "동쪽" to setOf(2, 5, 8), "서쪽" to setOf(0, 3, 6),
        "한가운데" to setOf(4), "가운데" to setOf(4), "중앙" to setOf(4),
        "북동" to setOf(2), "북서" to setOf(0), "남동" to setOf(8), "남서" to setOf(6),
    )

    /** What may follow a one-syllable 무엇 word (`강에서`, `숲속에`): rule-21 particles and `속`. */
    private val SHORT_WORD_TAILS: List<String> = (io.github.lenticualrstone.civilight.domain.world.term.Particles.BUILT_IN + "속").sortedByDescending { it.length }

    private fun isBoundary(c: Char): Boolean = !Character.isLetterOrDigit(c)

    /** True when [word] stands as a word at [at]: a boundary before it, and for one syllable a boundary, particle or `속` after it. */
    private fun standsAt(clause: String, at: Int, word: String): Boolean {
        if (at > 0 && !isBoundary(clause[at - 1])) return false
        if (word.length > 1) return true
        val end = at + word.length
        if (end >= clause.length || isBoundary(clause[end])) return true
        return SHORT_WORD_TAILS.any { clause.startsWith(it, end) }
    }

    private fun firstStanding(clause: String, word: String): Int {
        var from = 0
        while (true) {
            val at = clause.indexOf(word, from)
            if (at < 0) return -1
            if (standsAt(clause, at, word)) return at
            from = at + 1
        }
    }

    private val TRAITS: List<Pair<String, String>> = listOf(
        "눈 덮인" to "눈 덮임", "눈 덮임" to "눈 덮임", "메마른" to "메마름", "울창한" to "울창함", "높은" to "높음",
        "낮은" to "낮음", "넓은" to "넓음", "깊은" to "깊음", "얕은" to "얕음", "험한" to "험함", "해안" to "해안",
    )

    private val CLAUSE_BREAK = Regex("[,.;!?\\n。]")

    override fun read(text: String, placeNames: List<String>): ReadResult {
        val readings = ArrayList<ConditionReading>()
        var start = 0
        for (m in CLAUSE_BREAK.findAll(text + ".")) {
            val clause = text.substring(start, minOf(m.range.first, text.length))
            val offset = start
            start = m.range.last + 1
            readClause(clause, placeNames)?.let { r ->
                val lead = clause.length - clause.trimStart().length
                readings += r.copy(span = (offset + lead) until (offset + clause.trimEnd().length))
            }
        }
        return ReadResult.Read(readings, null)
    }

    private fun readClause(clause: String, placeNames: List<String>): ConditionReading? {
        // 무엇: the earliest kind word in the clause (longer words first at the same position).
        val words = ConditionKind.entries.flatMap { k -> k.words }.sortedByDescending { it.length }
        var what: String? = null
        var whatAt = Int.MAX_VALUE
        for (w in words) {
            if (w == "해안" || w == "만") continue
            val at = firstStanding(clause, w)
            if (at >= 0 && (at < whatAt || (at == whatAt && w.length > (what?.length ?: 0)))) {
                what = w
                whatAt = at
            }
        }
        if (what == null) {
            what = listOf("해안", "바다").firstOrNull { firstStanding(clause, it) >= 0 } ?: return null
        }
        val where = DIRECTIONS.firstOrNull { (word, _) -> clause.contains(word) }?.second ?: (0..8).toSet()
        val traits = TRAITS.filter { (word, _) -> clause.contains(word) && word != what }.map { it.second }.distinct()
        val place = placeNames.filter { it.isNotBlank() && clause.contains(it) }.maxByOrNull { it.length }
        return ConditionReading(what = what, where = where, traits = traits, placeName = place)
    }
}

/** Display parts of a `ConditionRow`: name = 무엇, small = 어디 · 특징 · 연결 장소. */
data class ConditionRowText(val name: String, val where: String, val small: String, val status: String)

/** Conditions of `글로 설명` (WLD-61): names only from project records, evaluation, header and rows. */
object TextConditions {
    const val HINT = "조건 → 생성기 입력 · 그림은 생성기가 · 이름은 프로젝트 기록에서만"

    /**
     * Turns readings into stored conditions. A place name links only to a live project place with
     * exactly that name; any other name is dropped (never invented); 무엇 · 어디 · 특징 are kept.
     */
    fun resolve(doc: ProjectDoc, readings: List<ConditionReading>, ctx: EditContext): List<TextCondition> {
        val places = doc.world.places.filter { it.deletedAt == null }
        return readings.map { r ->
            val place = r.placeName?.let { name -> places.firstOrNull { it.name == name.trim() } }
            TextCondition(
                key = ctx.newKey(),
                what = r.what.trim(),
                where = r.where.filter { it in 0..8 }.toSet(),
                traits = r.traits,
                placeKey = place?.key,
                status = ConditionStatus.NEEDS_CHECK.open(),
                sourceSpan = r.span?.let { TextSpan(it.first, it.last + 1) },
            )
        }
    }

    /** The cells a condition covers: the author's `위치` if set, else its 어디 (empty = whole map). */
    fun area(condition: TextCondition): Set<Int> =
        (condition.userPosition ?: condition.where).ifEmpty { (0..8).toSet() }

    /** Marks each condition `반영` or `확인 필요` against a generated world. */
    fun evaluate(world: GeneratedWorld, conditions: List<TextCondition>): List<TextCondition> =
        conditions.map { c -> c.copy(status = (if (isMet(world, c)) ConditionStatus.APPLIED else ConditionStatus.NEEDS_CHECK).open()) }

    fun isMet(world: GeneratedWorld, condition: TextCondition): Boolean {
        val kind = ConditionKind.of(condition.what) ?: return false
        val grid = Grid(world.w, world.h)
        val area = area(condition)
        val cells = (0 until grid.size).filter { grid.area(it) in area }
        fun any(vararg t: Terrain) = cells.any { world.terrain(it) in t }
        fun townIn(port: Boolean) = world.towns.any { grid.area(it.cell) in area && (!port || it.port) }
        return when (kind) {
            ConditionKind.MOUNTAINS -> any(Terrain.HILL, Terrain.MOUNTAIN, Terrain.PEAK)
            ConditionKind.DESERT -> any(Terrain.DESERT)
            ConditionKind.FOREST -> any(Terrain.FOREST)
            ConditionKind.SWAMP -> any(Terrain.SWAMP)
            ConditionKind.SNOW -> any(Terrain.SNOW, Terrain.PEAK)
            ConditionKind.PLAIN -> any(Terrain.PLAIN)
            ConditionKind.SEA -> cells.any { !world.isLand(it) } && cells.any { world.isLand(it) }
            ConditionKind.LAKE -> world.lakes.any { grid.area(it) in area } || cells.any { !world.isLand(it) }
            ConditionKind.RIVER -> world.rivers.any { r -> r.any { p -> grid.area(grid.cellAt(p.x, p.y)) in area } }
            ConditionKind.PORT -> townIn(port = true)
            ConditionKind.TOWN -> townIn(port = false)
        }
    }

    /** `조건 4 · 3 반영`. */
    fun header(conditions: List<TextCondition>): String =
        "조건 ${conditions.size}${Copy.SEP}${conditions.count { it.status.known == ConditionStatus.APPLIED }} 반영"

    /** `읽어 낸 조건 · N | 기기 안 모델` (the source line of the input screen). */
    fun readLine(conditions: List<TextCondition>, model: TextModelInfo?): String {
        val engine = when {
            model == null -> "키워드"
            model.onDevice -> "기기 안 모델"
            else -> "클라우드"
        }
        return "읽어 낸 조건${Copy.SEP}${conditions.size} | $engine"
    }

    /** `북쪽`, `남쪽 해안`, `가운데`, `전체`, or the area numbers. */
    fun whereLabel(condition: TextCondition): String {
        val area = area(condition)
        val base = when (area) {
            setOf(0, 1, 2) -> "북쪽"
            setOf(6, 7, 8) -> "남쪽"
            setOf(2, 5, 8) -> "동쪽"
            setOf(0, 3, 6) -> "서쪽"
            setOf(4) -> "가운데"
            setOf(0) -> "북서쪽"
            setOf(2) -> "북동쪽"
            setOf(6) -> "남서쪽"
            setOf(8) -> "남동쪽"
            (0..8).toSet() -> "전체"
            else -> area.sorted().joinToString(" ")
        }
        return if ("해안" in condition.traits) "$base 해안" else base
    }

    /** `항구 도시 · 남쪽 해안 · 장소 ‘잿더미 시장’`, `산맥 · 북쪽 · 눈 덮임`. */
    fun row(doc: ProjectDoc, condition: TextCondition): ConditionRowText {
        val traits = condition.traits.filter { it != "해안" }
        val place = condition.placeKey?.let { doc.index.place(it)?.name }?.let { "장소 ‘$it’" }
        val small = (listOf(whereLabel(condition)) + traits + listOfNotNull(place)).joinToString(Copy.SEP)
        val status = if (condition.status.known == ConditionStatus.APPLIED) "반영" else "확인 필요"
        return ConditionRowText(condition.what, whereLabel(condition), small, status)
    }
}
