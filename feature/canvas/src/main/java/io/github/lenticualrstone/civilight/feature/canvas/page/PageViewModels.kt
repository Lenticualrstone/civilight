package io.github.lenticualrstone.civilight.feature.canvas.page

import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.trash.ItemId
import io.github.lenticualrstone.civilight.domain.trash.Toast
import io.github.lenticualrstone.civilight.feature.canvas.common.CanvasViewModel
import io.github.lenticualrstone.civilight.feature.canvas.common.RestoreResult
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.RichDoc
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.project.ProjectSessions
import kotlinx.coroutines.launch

/**
 * The page screen (CNV-13, CNV-15, CNV-16). Typing in the name or the body writes through with one merged
 * undo step per field; every menu or card button is one undo step named by its copy. Read-only projects
 * answer [EditResult.READ_ONLY].
 */
class PageDocViewModel(
    projectKey: Key,
    sessions: ProjectSessions,
    contextOf: () -> EditContext,
) : CanvasViewModel(projectKey, sessions, contextOf) {
    fun rename(pageKey: Key, name: String) {
        act(PageCopy.UNDO_TEXT, mergeKey = "page-name-$pageKey") { doc, ctx -> PageEdits.rename(doc, pageKey, name, ctx) }
    }

    fun setBlocks(pageKey: Key, blocks: RichDoc) {
        act(PageCopy.UNDO_TEXT, mergeKey = "page-text-$pageKey") { doc, ctx -> PageEdits.setBlocks(doc, pageKey, blocks, ctx) }
    }

    fun setHidden(pageKey: Key, hidden: Boolean, then: (EditResult) -> Unit) {
        act(if (hidden) PageCopy.BTN_HIDE else PageCopy.BTN_UNHIDE, then = then) { doc, ctx -> PageEdits.setHidden(doc, pageKey, hidden, ctx) }
    }

    /** `지우기`: the page goes to the trash; [then] gets the toast to show (null when nothing changed). */
    fun delete(pageKey: Key, then: (EditResult, Toast?) -> Unit) {
        var toast: Toast? = null
        act(PageCopy.BTN_DELETE, then = { then(it, toast) }) { doc, ctx ->
            PageEdits.delete(doc, pageKey, ctx)?.let { (next, t) -> toast = t; next } ?: doc
        }
    }

    fun unlink(linkKey: Key, then: (EditResult) -> Unit) {
        act(PageCopy.BTN_UNLINK, then = then) { doc, ctx -> PageEdits.unlink(doc, linkKey, ctx) }
    }

    fun keep(item: ItemId, then: (EditResult) -> Unit) {
        act(PageCopy.BTN_KEEP, then = then) { doc, ctx -> PageEdits.keep(doc, item, ctx) }
    }

    fun relink(pageKey: Key, linkKey: Key, kind: LinkTargetKind, targetKey: Key, text: String, then: (EditResult) -> Unit) {
        act(PageCopy.BTN_RELINK, then = then) { doc, ctx -> PageEdits.relink(doc, pageKey, linkKey, kind, targetKey, text, ctx) }
    }

    /** `되살리기`; [then] gets the toast, or `needsAuth` when a hidden item asks for `지문으로 보기` first. */
    fun restore(item: ItemId, authenticated: Boolean, then: (EditResult, RestoreResult) -> Unit) {
        var result = RestoreResult(null, null)
        act(PageCopy.BTN_RESTORE, then = { then(it, result) }) { doc, ctx ->
            result = PageEdits.restore(doc, item, ctx, authenticated)
            result.doc ?: doc
        }
    }

    /** `되돌리기` of the last step (the delete and restore toasts). */
    fun undoLast() {
        viewModelScope.launch { undo() }
    }
}

/** The page list (CNV-14): making a page. */
class PageListViewModel(
    projectKey: Key,
    sessions: ProjectSessions,
    contextOf: () -> EditContext,
) : CanvasViewModel(projectKey, sessions, contextOf) {
    /** U-15: `만들기` of the create card; [then] gets the new page key. */
    fun create(name: String, then: (EditResult, Key?) -> Unit) {
        var made: Key? = null
        act(PageCopy.UNDO_NEW_PAGE, then = { then(it, made) }) { doc, ctx ->
            PageEdits.create(doc, name, ctx).also { made = it.second }.first
        }
    }
}
