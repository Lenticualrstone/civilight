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
    /**
     * (0.2.0, CST-66) `만들기` / `만들고 열기` of the create card: one write through `CharacterEdits.create`
     * (undo label `만들기`); [onCreated] receives the new key (not called when read-only).
     */
    fun createFromCard(state: io.github.lenticualrstone.civilight.designsystem.component.CreateCardState, onDone: () -> Unit = {}, onCreated: (Key) -> Unit) {
        var created: Key? = null
        act(io.github.lenticualrstone.civilight.designsystem.component.CreateCardCopy.CREATE, then = { result ->
            onDone()
            if (result == EditResult.APPLIED) created?.let(onCreated)
        }) { doc, ctx ->
            io.github.lenticualrstone.civilight.feature.cast.create.CastCreate.createCharacter(doc, state, ctx)?.let { (next, key) ->
                created = key
                next
            } ?: doc
        }
    }

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

/** (0.2.0) Kept for its tests; the list creates through [CastListViewModel.createFromCard] (CST-66). */
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
