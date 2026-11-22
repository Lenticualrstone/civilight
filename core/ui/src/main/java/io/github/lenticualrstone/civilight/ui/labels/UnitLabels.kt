package io.github.lenticualrstone.civilight.ui.labels

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.liveChildren
import io.github.lenticualrstone.civilight.model.story.StoryUnit

/**
 * Unit codes and labels (STO-03), derived from order and never stored. Ordinals count live units
 * only: trashed units do not count, hidden ones do (R19 §6).
 *
 * - top level: `1장` (header `Ⅰ 1장 · 등불지기`), custom `<n><level name>` (`1막`)
 * - episode level below the top: `1-7` (all ancestor ordinals for 4 levels: `1-2-3`)
 * - scene level: `씬 3`, full reference `1-7 씬 3`, with title `1-7 씬 3 · 꺼진 등불`
 * - a 1-level project: `<level name> <n>`
 *
 * Shared by the shell's pickers and lens sheet; the story domain owns the same rule for its screens.
 */
class UnitLabels(private val doc: ProjectDoc) {
    private val structure = doc.project.structure
    private val levelNames = structure.levels
    private val ordinals: Map<Key, Int> by lazy {
        val out = HashMap<Key, Int>()
        fun visit(parent: Key?) {
            doc.liveChildren(parent).forEachIndexed { i, unit ->
                out[unit.key] = i + 1
                visit(unit.key)
            }
        }
        visit(null)
        out
    }

    /** 1-based position among live siblings, or null for a trashed or unknown unit. */
    fun ordinal(unitKey: Key): Int? = ordinals[unitKey]

    fun isScene(unit: StoryUnit): Boolean = unit.level == structure.sceneLevel

    fun isEpisode(unit: StoryUnit): Boolean = unit.level == structure.episodeLevel

    /** The short code of a unit: `1장`, `1-7`, `씬 3`; null for a trashed unit. */
    fun code(unit: StoryUnit): String? {
        val n = ordinal(unit.key) ?: return null
        val name = levelNames.getOrElse(unit.level) { "" }
        return when {
            levelNames.size == 1 -> "$name $n"
            unit.level == structure.sceneLevel -> "$name $n"
            unit.level == 0 -> "$n$name"
            else -> path(unit)
        }
    }

    fun code(unitKey: Key): String? = doc.index.unit(unitKey)?.let(::code)

    /** `1-7 씬 3` (scene with its episode code), `씬 3` without an episode level; other units: [code]. */
    fun reference(unit: StoryUnit): String? {
        val own = code(unit) ?: return null
        if (!isScene(unit)) return own
        val episode = episodeOf(unit) ?: return own
        val episodeCode = code(episode) ?: return own
        return "$episodeCode $own"
    }

    fun reference(unitKey: Key): String? = doc.index.unit(unitKey)?.let(::reference)

    /** `1-7 씬 3 · 꺼진 등불`, or the reference alone for an untitled unit. */
    fun referenceWithTitle(unit: StoryUnit): String? {
        val ref = reference(unit) ?: return null
        val title = unit.title?.takeIf { it.isNotBlank() } ?: return ref
        return "$ref$SEP$title"
    }

    /** Chapter header `Ⅰ 1장 · 등불지기` (STO-01, STO-04); other levels: [referenceWithTitle]. */
    fun header(unit: StoryUnit): String? {
        if (unit.level != 0 || levelNames.size == 1) return referenceWithTitle(unit)
        val n = ordinal(unit.key) ?: return null
        val base = "${roman(n)} ${code(unit)}"
        val title = unit.title?.takeIf { it.isNotBlank() } ?: return base
        return "$base$SEP$title"
    }

    /** The episode-level ancestor of a scene (or the unit itself when it is an episode). */
    fun episodeOf(unit: StoryUnit): StoryUnit? {
        val level = structure.episodeLevel ?: return null
        if (unit.level == level) return unit
        return doc.index.ancestors(unit).firstOrNull { it.level == level }
    }

    private fun path(unit: StoryUnit): String {
        val chain = (doc.index.ancestors(unit).reversed() + unit)
        return chain.mapNotNull { ordinal(it.key) }.joinToString("-")
    }

    companion object {
        const val SEP = " · "
        private val ROMAN = listOf("Ⅰ", "Ⅱ", "Ⅲ", "Ⅳ", "Ⅴ", "Ⅵ", "Ⅶ", "Ⅷ", "Ⅸ", "Ⅹ", "Ⅺ", "Ⅻ")
        private val ROMAN_PARTS = listOf(
            1000 to "M", 900 to "CM", 500 to "D", 400 to "CD", 100 to "C", 90 to "XC",
            50 to "L", 40 to "XL", 10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I",
        )

        /** `Ⅰ`…`Ⅻ` as single characters, then plain Roman letters. */
        fun roman(n: Int): String {
            if (n in 1..ROMAN.size) return ROMAN[n - 1]
            var rest = n
            return buildString {
                for ((value, letters) in ROMAN_PARTS) while (rest >= value) {
                    append(letters)
                    rest -= value
                }
            }
        }
    }
}
