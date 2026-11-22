package io.github.lenticualrstone.civilight.feature.cast.trash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import io.github.lenticualrstone.civilight.domain.trash.TrashCopy
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.common.CastWrites
import io.github.lenticualrstone.civilight.feature.cast.common.Step
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.navigation.DeleteImpactSheet
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.security.AuthCopy
import io.github.lenticualrstone.civilight.ui.security.AuthRequest
import io.github.lenticualrstone.civilight.ui.security.AuthResult
import kotlinx.coroutines.launch

/**
 * The delete, restore and keep flows of one project (CST-40, CST-41, CST-43). Every write runs
 * through [CastWrites] on the app scope, so the toast and its `되돌리기` still work after the record
 * that deleted itself has left the screen.
 */
class ItemTrashFlow(private val deps: FeatureDeps, private val projectKey: Key) {
    private val writes = CastWrites(deps, projectKey)

    /**
     * `지우기`: the impact sheet when the item has references or owned records, else delete at once
     * with the toast (TC-20-03). [openSheet] opens the shared [DeleteImpactSheet].
     */
    fun requestDelete(kind: EntityKind, key: Key, openSheet: (SheetKey) -> Unit) {
        deps.appScope.launch {
            deps.stores.recorder.ensureLoaded()
            val doc = deps.projects.peekDoc(projectKey) ?: return@launch
            // Decided before any sheet opens (row 9): a sheet is only opened when it has something to show.
            val needsSheet = ItemTrash.needsSheet(doc, kind, key, deps.stores.recorder.state.value.library) ?: return@launch
            if (needsSheet) openSheet(DeleteImpactSheet(projectKey, kind, key)) else delete(kind, key)
        }
    }

    /** `휴지통으로`, then the toast `인물 노아 지움 · 참조 20 · 점검 센터` with `되돌리기`. */
    fun delete(kind: EntityKind, key: Key) {
        deps.appScope.launch {
            deps.stores.recorder.ensureLoaded()
            val recorder = deps.stores.recorder.state.value.library
            writes.launch(TrashCopy.TO_TRASH, { doc, ctx -> ItemTrash.delete(doc, kind, key, ctx, recorder)?.let { Step(it.doc, it) } }) { done ->
                deps.toasts.show(done.toast.message, done.toast.action ?: TrashCopy.UNDO) { writes.revert(TrashCopy.UNDO, done.change) }
            }
        }
    }

    /** A field definition to the trash (CST-23), then [then] (the edit screen leaves) and the toast with `되돌리기`. */
    fun deleteFieldDef(defKey: Key, then: () -> Unit) {
        writes.launch(TrashCopy.TO_TRASH, { doc, ctx -> ItemTrash.deleteFieldDef(doc, defKey, ctx)?.let { Step(it.doc, it) } }) { done ->
            then()
            deps.toasts.show(done.toast.message, done.toast.action ?: TrashCopy.UNDO) { writes.revert(TrashCopy.UNDO, done.change) }
        }
    }

    /** `되살리기` (CST-43). A hidden item asks the biometric prompt first; cancel or failure does nothing (LIB-37). */
    fun restore(kind: EntityKind, key: Key) {
        restore(kind, key, authenticated = false)
    }

    private fun restore(kind: EntityKind, key: Key, authenticated: Boolean) {
        var needsAuth = false
        writes.launch(
            CastUiCopy.BTN_RESTORE,
            { doc, ctx ->
                val r = ItemTrash.restore(doc, kind, key, ctx, authenticated)
                needsAuth = r.needsAuth
                r.doc?.let { Step(it, r) }
            },
        ) { done ->
            // A restore toast reports counts; it has no `되돌리기` (the item can be deleted again).
            done.toast?.let { deps.toasts.show(it.message, it.action, null) }
        }.invokeOnCompletion {
            if (needsAuth && !authenticated) {
                deps.appScope.launch {
                    if (deps.biometric.authenticate(AuthRequest(AuthCopy.HIDDEN_PROJECTS)) == AuthResult.SUCCESS) restore(kind, key, authenticated = true)
                }
            }
        }
    }

    /** `이대로 두기` on a deleted item's check row (CST-42): it moves to `맞다고 표시한 것`. */
    fun keepItem(itemKey: Key) {
        writes.launch(CastUiCopy.BTN_KEEP, { doc, ctx -> Step(ItemTrash.keepItem(doc, itemKey, ctx), Unit) })
    }

    /** `이대로 두기` on one dotted row (CST-41): the dotted mark stays, the fix buttons go. */
    fun keep(itemKey: Key, referencingKey: Key) {
        writes.launch(CastUiCopy.BTN_KEEP, { doc, ctx -> Step(ItemTrash.keep(doc, itemKey, referencingKey, ctx), Unit) })
    }
}

/** The project's [ItemTrashFlow]; it also loads the recorder library, whose chips count as references. */
@Composable
fun rememberItemTrash(projectKey: Key): ItemTrashFlow {
    val deps = LocalFeatureDeps.current
    LaunchedEffect(Unit) { deps.stores.recorder.ensureLoaded() }
    return remember(deps, projectKey) { ItemTrashFlow(deps, projectKey) }
}
