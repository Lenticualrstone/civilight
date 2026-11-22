package io.github.lenticualrstone.civilight.domain.story.units

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.story.StoryUnit

/**
 * Unit codes and labels (STO-03): `1장`, `1-7`, `씬 4`, `1-7 씬 3`. Ordinals are derived from order
 * among live siblings: units in the trash do not count, hidden ones do. A unit that is not live
 * itself gets the ordinal it would have (1 + live siblings before it).
 *
 * Custom hierarchies (`ED`, Q2): top level `<n><level name>`; the episode level `c-e` (all ancestor
 * ordinals joined by `-`, so `1-2-3` with four levels); when the episode level is the top level
 * (two levels) it is `<n><level name>`; the scene level `<level name> <n>`.
 */
class UnitCodes(private val doc: ProjectDoc) {
    private val structure = doc.project.structure
    private val levels: List<String> = structure.levels
    private val ordinals = HashMap<Key, Int>()

    val sceneLevel: Int get() = structure.sceneLevel
    val episodeLevel: Int? get() = structure.episodeLevel

    /** 1-based ordinal among live siblings. */
    fun ordinal(unit: StoryUnit): Int = ordinals.getOrPut(unit.key) {
        val siblings = doc.index.children(unit.parentKey).filter { it.level == unit.level }
        var n = 0
        for (s in siblings) {
            if (s.key == unit.key) return@getOrPut n + 1
            if (doc.liveness.isUnitLive(s.key)) n++
        }
        n + 1
    }

    fun ordinal(key: Key): Int? = doc.index.unit(key)?.let(::ordinal)

    /** `1장` for a top-level unit (or `<n><name>` for any level above the scene level). */
    fun levelLabel(unit: StoryUnit): String = "${ordinal(unit)}${levels.getOrElse(unit.level) { "" }}"

    /** The episode-level ancestor (or the unit itself), or null in a 1-level structure. */
    fun episodeOf(unit: StoryUnit): StoryUnit? {
        val level = episodeLevel ?: return null
        if (unit.level == level) return unit
        return doc.index.ancestors(unit).firstOrNull { it.level == level }
    }

    fun episodeOf(key: Key): StoryUnit? = doc.index.unit(key)?.let(::episodeOf)

    /** The top-level ancestor (chapter), or the unit itself. */
    fun chapterOf(unit: StoryUnit): StoryUnit = doc.index.ancestors(unit).lastOrNull() ?: unit

    /** `1-7`; `1막` when the episode level is the top level. */
    fun episodeCode(episode: StoryUnit): String {
        if (episode.level == 0) return levelLabel(episode)
        val path = doc.index.ancestors(episode).reversed() + episode
        return path.joinToString("-") { ordinal(it).toString() }
    }

    fun episodeCode(key: Key): String? = doc.index.unit(key)?.let(::episodeCode)

    /** `씬 4` (scene level name + ordinal). */
    fun sceneLabel(scene: StoryUnit): String = "${levels.last()} ${ordinal(scene)}"

    fun sceneLabel(key: Key): String? = doc.index.unit(key)?.let(::sceneLabel)

    /** `1-7 씬 3`; `씬 3` in a 1-level structure. */
    fun sceneRef(scene: StoryUnit): String {
        val episode = episodeOf(scene) ?: return sceneLabel(scene)
        return "${episodeCode(episode)} ${sceneLabel(scene)}"
    }

    fun sceneRef(key: Key): String? = doc.index.unit(key)?.let(::sceneRef)

    /** `1-7 씬 3 · 꺼진 등불` (title omitted when there is none). */
    fun sceneRefWithTitle(scene: StoryUnit): String =
        scene.title?.let { "${sceneRef(scene)} · $it" } ?: sceneRef(scene)

    /**
     * `씬 3` when [scene] is in the same episode as [other], else `1-8 씬 1` (`ED`, STO-29 chips and
     * route lines).
     */
    fun sceneRefRelativeTo(scene: StoryUnit, other: StoryUnit): String =
        if (episodeOf(scene)?.key != null && episodeOf(scene)?.key == episodeOf(other)?.key) sceneLabel(scene) else sceneRef(scene)

    /** Code of any unit: episode code, scene reference, or `<n><level name>`. */
    fun unitLabel(unit: StoryUnit): String = when {
        unit.level == sceneLevel -> sceneRef(unit)
        unit.level == episodeLevel -> episodeCode(unit)
        else -> levelLabel(unit)
    }

    fun unitLabel(key: Key): String? = doc.index.unit(key)?.let(::unitLabel)

    /** `Ⅰ 1장 · 등불지기` chapter header (title omitted when there is none). */
    fun chapterHeader(chapter: StoryUnit): String {
        val n = ordinal(chapter)
        val head = "${roman(n)} ${levelLabel(chapter)}"
        return chapter.title?.let { "$head · $it" } ?: head
    }

    /** `단계 이름: 장 › 에피소드 › 씬`. */
    fun levelLine(): String = "단계 이름: ${levels.joinToString(" › ")}"

    companion object {
        private val ROMAN_GLYPHS = listOf("Ⅰ", "Ⅱ", "Ⅲ", "Ⅳ", "Ⅴ", "Ⅵ", "Ⅶ", "Ⅷ", "Ⅸ", "Ⅹ", "Ⅺ", "Ⅻ")

        /** Roman numeral glyph (`Ⅰ` … `Ⅻ`); plain Latin letters beyond 12 (`ED`). */
        fun roman(n: Int): String {
            if (n in 1..12) return ROMAN_GLYPHS[n - 1]
            var rest = n
            val sb = StringBuilder()
            for ((value, letters) in listOf(1000 to "M", 900 to "CM", 500 to "D", 400 to "CD", 100 to "C", 90 to "XC", 50 to "L", 40 to "XL", 10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I")) {
                while (rest >= value) {
                    sb.append(letters)
                    rest -= value
                }
            }
            return sb.toString()
        }
    }
}
