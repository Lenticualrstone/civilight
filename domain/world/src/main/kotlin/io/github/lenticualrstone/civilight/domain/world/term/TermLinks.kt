package io.github.lenticualrstone.civilight.domain.world.term

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.position.Rank
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun

/**
 * One `@` link run to a term (WLD-17). Offsets are in the spot's text; [rank] is the block's or
 * transition's manuscript rank (null outside the manuscript).
 */
data class TermLink(
    val linkKey: Key,
    val termKey: Key,
    val text: String,
    val spot: TextSpot,
    val start: Int,
    val end: Int,
    val rank: Rank?,
) {
    /** Usage proper: a link in game-output manuscript text (`쓰인 곳`). */
    val isUsage: Boolean get() = spot is ScriptSpot
}

/** Finds term links in runs. */
object TermLinks {
    /** Term links in one spot. */
    fun inSpot(spotText: SpotText): List<TermLink> {
        val out = ArrayList<TermLink>()
        var cursor = 0
        for (run in spotText.runs) {
            val len = run.shownText.length
            if (run is TextRun.Link && run.target.kind.known == LinkTargetKind.TERM) {
                out += TermLink(run.linkKey, run.target.key, run.text, spotText.spot, cursor, cursor + len, spotText.rank)
            }
            cursor += len
        }
        return out
    }
}
