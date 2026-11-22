package io.github.lenticualrstone.civilight.feature.script.panel

import io.github.lenticualrstone.civilight.domain.trash.Fixes
import io.github.lenticualrstone.civilight.domain.trash.TrashContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/**
 * `이대로 두기` on a line whose speaker was deleted (SCR-34, SCR-57, rule 20 §2): the line keeps the
 * deleted speaker (dotted look stays) and the reference moves to `맞다고 표시한 것`. One reference =
 * the acknowledgement `[character, block]` that `domain:trash` `Fixes.keep` writes (DECISIONS §1:
 * deleted-item fixes go through `domain:trash`).
 */
object SpeakerKeep {
    fun keep(doc: ProjectDoc, characterKey: Key, blockKey: Key, ctx: TrashContext): ProjectDoc =
        Fixes.keep(doc, characterKey, ctx, referencingKey = blockKey).doc

    /** This line, or the character as a whole, was kept. */
    fun isKept(doc: ProjectDoc, characterKey: Key, blockKey: Key): Boolean =
        Fixes.isKept(doc, characterKey) || doc.checks.acknowledgements.any {
            it.checkType == CheckType.DELETED_ITEM && it.subjectKeys == listOf(characterKey, blockKey)
        }
}
