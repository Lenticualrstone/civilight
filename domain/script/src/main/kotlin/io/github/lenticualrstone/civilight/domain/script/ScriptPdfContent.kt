package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.ScriptLayers

/**
 * What the script PDF prints (SCR-65); the renderer and the cover belong to the export area. Each
 * scene starts with its head text as a title line; the transition text is never printed
 * (`[PROPOSED]`); `연출 · 괄호 지문 포함` decides direction blocks and parentheticals; memo blocks follow
 * the `표시` sheet's memo layer (`[PROPOSED]`). Hidden scenes follow [ExportScope] (PDF default `빼기`).
 */
object ScriptPdfContent {
    sealed interface Line {
        data class Title(val text: String) : Line

        /** One block: [label] is the speaker name or a kind word, [text] what is printed. */
        data class Body(val label: String?, val text: String, val kind: BlockKind?) : Line
    }

    fun lines(
        doc: ProjectDoc,
        scope: ExportScope,
        time: SceneTimeSource,
        includeDirectionAndParens: Boolean,
        layers: ScriptLayers = ScriptLayers(),
    ): List<Line> = linesOf(doc, scope.scenes(doc).map { it.key }, time, includeDirectionAndParens, layers)

    /**
     * The same lines for the scenes an export plan wrote (`ExportPlan.scenes`, in that order), so the
     * PDF renderer prints exactly the planned range and hidden-scene choice (DECISIONS §1). Keys of
     * scenes that are not live are skipped.
     */
    fun linesOf(
        doc: ProjectDoc,
        sceneKeys: List<Key>,
        time: SceneTimeSource,
        includeDirectionAndParens: Boolean,
        layers: ScriptLayers = ScriptLayers(),
    ): List<Line> {
        val printLayers = ScriptLayers(
            direction = includeDirectionAndParens, paren = includeDirectionAndParens, memo = layers.memo,
            system = true, marks = false, addressHint = false, termCandidates = false,
        )
        val out = mutableListOf<Line>()
        for (sceneKey in sceneKeys) {
            if (!doc.manuscript.isLiveUnit(sceneKey)) continue
            out += Line.Title(SceneText.headText(doc, sceneKey, time.timeOf(sceneKey)))
            for (b in doc.manuscript.liveBlocksOf(sceneKey)) {
                if (!Manuscript.isShown(b.kind.known, printLayers)) continue
                val label = when (b.kind.known) {
                    BlockKind.DIALOGUE -> b.speakerKey?.let { Labels.speakerName(doc, it) } ?: Manuscript.SpeakerLabel.Missing.TEXT
                    BlockKind.CHOICE -> "선택지"
                    BlockKind.SYSTEM -> "시스템"
                    BlockKind.MEMO -> "⌁ 작가 메모"
                    else -> null
                }
                out += Line.Body(label, Manuscript.blockText(doc, b, printLayers), b.kind.known)
            }
        }
        return out
    }
}
