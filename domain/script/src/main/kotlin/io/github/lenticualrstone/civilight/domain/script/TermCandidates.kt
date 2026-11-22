package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.isLinkBeforeReveal
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.ScriptLayers
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind

/**
 * The script side of term candidates (SCR-55/56). Matching (names, aliases, particles, `끊김`) and
 * `아님` storage belong to the world area; this renders a found candidate and links it.
 */
object TermCandidates {
    /** Game-output kinds whose text is scanned: dialogue, narration, system text, choice options. */
    val SCANNED: Set<BlockKind> = setOf(BlockKind.DIALOGUE, BlockKind.NARRATION, BlockKind.SYSTEM, BlockKind.CHOICE)

    /** Candidate underlines are drawn only with the `용어 후보` layer on (default off) and in scanned kinds. */
    fun showsUnderlines(layers: ScriptLayers, block: Block): Boolean =
        layers.termCandidates && block.kind.known in SCANNED

    /** Chip under the header while the layer is on (`[PROPOSED]` placement). */
    fun layerChip(layers: ScriptLayers): Pair<String, String>? =
        if (layers.termCandidates) "표시 · 용어 후보" to "켬" else null

    /**
     * The popover of a candidate: line 1 `{term}` + tag `후보 · 끊김` (or `후보`); line 2, only when
     * linking would be a pre-reveal use, `첫 공개 {unit} · 잇기 시` + `▲ 공개 전 사용`.
     */
    data class Popover(val term: String, val tag: String, val revealLine: Pair<String, String>?) {
        val buttons: List<String> get() = listOf("잇기", "아님")
    }

    fun popover(doc: ProjectDoc, blockKey: Key, termKey: Key, cutOff: Boolean): Popover? {
        val term = doc.index.term(termKey) ?: return null
        val revealLine = if (doc.isLinkBeforeReveal(termKey, blockKey)) {
            val where = Records.existingReveal(doc, termKey)?.anchor?.let { revealUnit(doc, it) } ?: "예정"
            "첫 공개 $where · 잇기 시" to "▲ 공개 전 사용"
        } else null
        return Popover(term.name, if (cutOff) "후보 · 끊김" else "후보", revealLine)
    }

    /** Unit of a reveal: the episode of its block (`1-7`), or the unit of a point (`2-5`, `2장`). */
    private fun revealUnit(doc: ProjectDoc, anchor: Anchor): String = when (anchor) {
        is Anchor.AtBlock -> doc.index.block(anchor.blockKey)?.let { Labels.episodeCodeOfScene(doc, it.sceneKey) } ?: "?"
        is Anchor.UnitStart -> Labels.unitLabel(doc, anchor.unitKey)
        is Anchor.UnitEnd -> Labels.unitLabel(doc, anchor.unitKey)
        else -> "예정"
    }

    /**
     * `잇기`: wraps exactly the shown range [start, end) of the block's text (or of one choice
     * option) into a link to the term with a new link key; the text is unchanged. Null when the
     * range is empty or crosses a run that is not plain text.
     */
    fun link(
        doc: ProjectDoc,
        blockKey: Key,
        start: Int,
        end: Int,
        termKey: Key,
        ctx: EditContext,
        optionKey: Key? = null,
    ): ProjectDoc? {
        val block = doc.index.block(blockKey)?.takeIf { it.kind.known in SCANNED } ?: return null
        val target = LinkTarget(LinkTargetKind.TERM, termKey)
        return if (optionKey == null) {
            val runs = Runs.linkRange(block.runs, start, end, ctx.newKey(), target) ?: return null
            doc.replaceBlock(block.copy(runs = runs, updatedAt = ctx.now, updatedBy = ctx.device))
        } else {
            val option = block.options.firstOrNull { it.key == optionKey } ?: return null
            val runs = Runs.linkRange(option.runs, start, end, ctx.newKey(), target) ?: return null
            doc.replaceBlock(
                block.copy(
                    options = block.options.map { if (it.key == optionKey) it.copy(runs = runs) else it },
                    updatedAt = ctx.now, updatedBy = ctx.device,
                ),
            )
        }
    }

    /** `@` picker row of an alias: `오리지늄 · 오리지늄 등불의 별칭`; choosing it inserts the alias text. */
    fun aliasRow(alias: String, termName: String): String = "$alias · ${termName}의 별칭"
}
