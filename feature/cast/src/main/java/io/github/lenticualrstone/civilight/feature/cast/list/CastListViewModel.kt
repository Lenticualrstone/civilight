package io.github.lenticualrstone.civilight.feature.cast.list

import io.github.lenticualrstone.civilight.domain.cast.CharacterEdits
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.common.CastViewModel
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.project.ProjectSessions

/** The character list (CST-01, CST-02) and `인물 추가` (CST-05). */
class CastListViewModel(
    projectKey: Key,
    sessions: ProjectSessions,
    contextOf: () -> EditContext,
) : CastViewModel(projectKey, sessions, contextOf) {
    /** Creates a character named [name]; [onCreated] receives its key (not called when read-only). */
    fun create(name: String, onCreated: (Key) -> Unit) {
        var created: Key? = null
        act(CastUiCopy.NEW_CHARACTER, then = { result -> if (result == EditResult.APPLIED) created?.let(onCreated) }) { doc, ctx ->
            NewCharacter.create(doc, name, ctx)?.let { (next, key) ->
                created = key
                next
            } ?: doc
        }
    }
}

/** `인물 추가` (CST-05): only the name is required. */
object NewCharacter {
    /** `넣기` is enabled iff the name is not blank. */
    fun canCreate(name: String): Boolean = name.isNotBlank()

    /** The document with the new character and its key, or null for a blank name. */
    fun create(doc: ProjectDoc, name: String, ctx: EditContext): Pair<ProjectDoc, Key>? {
        if (!canCreate(name)) return null
        val (next, character) = CharacterEdits.create(doc, name, ctx)
        return next to character.key
    }
}
