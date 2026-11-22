package io.github.lenticualrstone.civilight.designsystem.text

/** One run of manuscript text (DS-19). Content and logic belong to SCR / WLD / LIB / CST. */
sealed interface ScriptRun {
    val text: String

    data class Plain(override val text: String) : ScriptRun

    /** Parenthetical `(숨을 삼키며)`, set inline in the `paren` role. */
    data class Paren(override val text: String) : ScriptRun

    /** `@` link to an item: `ink` text with a 1 dp `ink3` underline. */
    data class Link(override val text: String) : ScriptRun

    /** Term candidate (`후보`): dotted underline when the `용어 후보` layer is on; [selected] when tapped. */
    data class Candidate(override val text: String, val selected: Boolean = false) : ScriptRun

    /** Link to a hidden item: the masked label (`숨긴 항목`) in place of the name, with the link underline. */
    data class HiddenLink(override val text: String) : ScriptRun

    /** Link to a deleted item: dotted underline (DS-18). */
    data class DeletedRef(override val text: String) : ScriptRun
}

/** Plain text plus the decoration ranges to draw over it. */
data class ManuscriptText(val text: String, val decorations: List<DecorationRange>)

/**
 * Joins [runs] into one paragraph and computes the decorations. [showParens] false hides parentheticals
 * (display layer `괄호 지문`); [showCandidates] false leaves candidates undecorated (layer `용어 후보` off).
 */
fun manuscriptText(
    runs: List<ScriptRun>,
    showParens: Boolean = true,
    showCandidates: Boolean = false,
): ManuscriptText {
    val sb = StringBuilder()
    val decorations = mutableListOf<DecorationRange>()
    runs.forEach { run ->
        if (run is ScriptRun.Paren && !showParens) return@forEach
        val start = sb.length
        sb.append(run.text)
        val end = sb.length
        val kind = when (run) {
            is ScriptRun.Plain -> null
            is ScriptRun.Paren -> InlineDecoration.PAREN
            is ScriptRun.Link -> InlineDecoration.LINK
            is ScriptRun.HiddenLink -> InlineDecoration.HIDDEN_LINK
            is ScriptRun.DeletedRef -> InlineDecoration.DELETED_REF
            is ScriptRun.Candidate -> when {
                run.selected -> InlineDecoration.CANDIDATE_SELECTED
                showCandidates -> InlineDecoration.CANDIDATE
                else -> null
            }
        }
        if (kind != null && end > start) decorations += DecorationRange(start, end, kind)
    }
    return ManuscriptText(sb.toString(), decorations)
}

/** One cue of a direction line: marker, label and value (`▸ 배경  하층 거리(새벽)`). */
data class DirectionCue(val marker: MarkerKind, val label: String?, val value: String)

/** DS-42 direction line text: two spaces after a label, three spaces before the next cue. */
fun directionLineText(cues: List<DirectionCue>): String = cues.joinToString("   ") { cue ->
    buildString {
        append(cue.marker.symbol)
        append(' ')
        if (cue.label != null) {
            append(cue.label)
            append("  ")
        }
        append(cue.value)
    }
}
