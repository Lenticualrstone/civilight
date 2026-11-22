package io.github.lenticualrstone.civilight.feature.script.state

import io.github.lenticualrstone.civilight.domain.script.Labels
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/**
 * The scene head's line with the scene's title (QA 2026-11-23 #6): 스토리 and 홈 read `씬 1 · The Gate`,
 * so the head of a titled scene does too, followed by the place and time it knows. The placeholders
 * `장소 없음` and `때 없음` of SCR-46's table stay for an untitled scene only: they say nothing next to a
 * title. The head's other parts keep the domain's order (`회상` before the place, then the year).
 */
object HeadTitles {
    private const val SEP = " · "
    private val PLACEHOLDERS = setOf("장소 없음", "때 없음")

    /** [base] is the domain's head text (`씬 1 · 장소 없음 · 때 없음`); the title joins it after the ordinal. */
    fun of(doc: ProjectDoc, sceneKey: Key, base: String): String {
        val title = doc.index.unit(sceneKey)?.title?.trim()?.takeIf { it.isNotEmpty() } ?: return base
        val ordinal = Labels.sceneOrdinal(doc, sceneKey) ?: return base
        val prefix = "씬 $ordinal"
        if (base != prefix && !base.startsWith(prefix + SEP)) return base
        val rest = base.removePrefix(prefix).removePrefix(SEP).split(SEP).filter { it.isNotEmpty() && it !in PLACEHOLDERS }
        return (listOf(prefix, title) + rest).joinToString(SEP)
    }
}
