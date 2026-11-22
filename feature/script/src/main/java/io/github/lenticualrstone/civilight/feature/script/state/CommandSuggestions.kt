package io.github.lenticualrstone.civilight.feature.script.state

import io.github.lenticualrstone.civilight.domain.script.ScriptCommands
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.ui.search.KoreanSearch

/**
 * The script's commands shown as tappable rows over the command field while typing (SCR-68; board
 * `Search` draws commands as `› 점검 센터 열기` rows). Typed `/…` lists the matching block shortcuts
 * (`/대` …); other text lists the named commands it matches, 초성 included (`ㅇㅅ` → `에셋`). Nothing
 * runs before a row is tapped or the text is submitted (no guessing).
 */
object CommandSuggestions {
    const val MAX = 4

    fun of(doc: ProjectDoc, numberingUnitKey: Key, text: String): List<String> {
        val t = text.trim()
        if (t.isEmpty()) return emptyList()
        val all = ScriptCommands.commands(doc, numberingUnitKey)
        val (slash, named) = all.partition { it.startsWith("/") }
        val hits = if (t.startsWith("/")) {
            slash.filter { it.startsWith(t) && it != t }
        } else {
            named.filter { it != t && KoreanSearch.matches(it, t) }
        }
        return hits.take(MAX)
    }
}
