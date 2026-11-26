package io.github.lenticualrstone.civilight.feature.canvas.gallery

import io.github.lenticualrstone.civilight.model.cast.fits
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.domain.actions.GalleryActions
import io.github.lenticualrstone.civilight.domain.actions.GalleryAddResult
import io.github.lenticualrstone.civilight.domain.actions.GalleryAddSource
import io.github.lenticualrstone.civilight.domain.actions.GalleryEdit
import io.github.lenticualrstone.civilight.domain.actions.GalleryMove
import io.github.lenticualrstone.civilight.domain.actions.GalleryOwner
import io.github.lenticualrstone.civilight.domain.cast.CharacterEdits
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.trash.Toast
import io.github.lenticualrstone.civilight.feature.canvas.common.CanvasViewModel
import io.github.lenticualrstone.civilight.feature.canvas.common.actions
import io.github.lenticualrstone.civilight.feature.canvas.image.CropSession
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageEdits
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageOwner
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.gallery.GalleryViewState
import io.github.lenticualrstone.civilight.model.text.RichDoc
import io.github.lenticualrstone.civilight.store.blob.BlobStore
import io.github.lenticualrstone.civilight.store.blob.import
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.project.ProjectSessions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** What an add or a copy did: the store's answer, the keys added (first = the one to pick) and the toast. */
data class GalleryAddOutcome(val result: EditResult, val added: List<Key>, val toast: Toast?)

/**
 * The gallery's view model (55 GAL-04 … GAL-20). Every document write goes through `GalleryActions` (DECISIONS
 * §1); each call is one undo step. The pick, panel and scroll are app-only state in `_app/view.json` (CNV-35,
 * never exported) and are written only when they change. A call whose action changes nothing (`move` at an end)
 * leaves the document as it is, so the store records no step.
 */
class GalleryViewModel(
    projectKey: Key,
    sessions: ProjectSessions,
    contextOf: () -> EditContext,
    private val blobs: BlobStore? = null,
) : CanvasViewModel(projectKey, sessions, contextOf) {
    /** Writes [ownerKey]'s saved gallery state; no write when nothing changes. */
    fun saveState(ownerKey: Key, transform: (GalleryViewState) -> GalleryViewState) {
        actApp { app ->
            val current = app.view.galleryOf(ownerKey)
            val next = transform(current)
            if (next == current) app else app.copy(view = app.view.withGallery(ownerKey, next))
        }
    }

    /**
     * The empty gallery's `초상 이미지도 넣기` / `표지 이미지도 넣기` (GAL-25): one item with the main image's
     * source (nothing copied). One undo step named `넣기`; [then] gets the added key and the toast.
     */
    fun addSource(owner: GalleryOwner, source: String, locked: Boolean, holo: Boolean, then: (EditResult, Key?, Toast?) -> Unit) {
        add(owner, listOf(GalleryAddSource(source)), locked, holo) { o -> then(o.result, o.added.firstOrNull(), o.toast) }
    }

    /** `넣기 · <k>` (GAL-11): the chosen sources in badge order, one undo step (T-GAL-09, T-GAL-33). */
    fun add(owner: GalleryOwner, sources: List<GalleryAddSource>, locked: Boolean, holo: Boolean, then: (GalleryAddOutcome) -> Unit) {
        var result: GalleryAddResult? = null
        act(GalleryUiCopy.BTN_ADD, then = { r -> then(GalleryAddOutcome(r, result?.added.orEmpty(), result?.toast)) }) { doc, ctx ->
            GalleryActions.add(doc, owner, sources, ctx.actions(), locked, holo).also { result = it }.doc
        }
    }

    /**
     * `사진` · `파일` (GAL-11): the originals are copied into the blob store (stored once by hash) and added in
     * the order the system returned them, as one `GalleryActions.add` (one undo step). Unreadable or non-image
     * files are left out; [then] gets null when nothing could be read.
     */
    fun importAndAdd(
        resolver: ContentResolver,
        uris: List<Uri>,
        source: BlobSource,
        owner: GalleryOwner,
        locked: Boolean,
        holo: Boolean,
        then: (GalleryAddOutcome?) -> Unit,
    ) {
        val store = blobs ?: return then(null)
        viewModelScope.launch {
            if (isReadOnly) return@launch then(GalleryAddOutcome(EditResult.READ_ONLY, emptyList(), null))
            val imported = ArrayList<Blob>()
            for (uri in uris) {
                val blob = try {
                    store.import(resolver, uri, source)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                }
                if (blob != null && blob.mime.startsWith("image/")) imported += blob
            }
            if (imported.isEmpty()) return@launch then(null)
            add(owner, imported.map { GalleryAddSource(it.hash, it) }, locked, holo, then)
        }
    }

    /** `다른 곳에도 넣기` (GAL-16): every field copied, one undo step; `0장 넣음` when the target holds them all. */
    fun copyTo(itemKeys: List<Key>, target: GalleryOwner, locked: Boolean, then: (GalleryAddOutcome) -> Unit) {
        var result: GalleryAddResult? = null
        act(GalleryUiCopy.BTN_COPY_TO, then = { r -> then(GalleryAddOutcome(r, result?.added.orEmpty(), result?.toast)) }) { doc, ctx ->
            GalleryActions.copyTo(doc, itemKeys, target, ctx.actions(), locked).also { result = it }.doc
        }
    }

    /** One edit of item fields named [label]; [mergeKey] joins typing in one field into one undo step (GAL-13). */
    fun editItem(label: String, mergeKey: String? = null, then: (EditResult) -> Unit = {}, change: (ProjectDoc, io.github.lenticualrstone.civilight.domain.actions.ActionContext) -> GalleryEdit?) =
        act(label, mergeKey, then) { doc, ctx -> change(doc, ctx.actions())?.doc ?: doc }

    fun setTitle(itemKey: Key, title: String) =
        editItem(GalleryUiCopy.FIELD_TITLE, "gallery-title-$itemKey") { d, c -> GalleryActions.setTitle(d, itemKey, title, c) }

    fun setCaption(itemKey: Key, caption: RichDoc) =
        editItem(GalleryUiCopy.FIELD_DESCRIPTION, "gallery-caption-$itemKey") { d, c -> GalleryActions.setCaption(d, itemKey, caption, c) }

    fun setScene(itemKey: Key, sceneKey: Key?) =
        editItem(GalleryUiCopy.FIELD_LINK) { d, c -> GalleryActions.setScene(d, itemKey, sceneKey, c) }

    /** Tags of several items at once (info sheet: one item; `여러 장 고르기 › 태그`: all of them), one undo step. */
    fun setTags(itemKeys: List<Key>, tagsOf: (List<Key>) -> List<Key>) =
        act(GalleryUiCopy.FIELD_TAGS) { doc, ctx ->
            itemKeys.fold(doc) { d, k ->
                val item = d.index.galleryItem(k) ?: return@fold d
                GalleryActions.setTags(d, k, tagsOf(item.tagKeys), ctx.actions())?.doc ?: d
            }
        }

    /**
     * `+ 태그` (GAL-13, T-GAL-31): a project tag through the one write path `CharacterEdits.addTag` (an existing
     * name returns that tag), put on the items in the same undo step.
     */
    fun addTag(itemKeys: List<Key>, name: String) {
        if (name.isBlank()) return
        act(GalleryUiCopy.FIELD_TAGS) { doc, ctx ->
            val (withTag, tag) = CharacterEdits.addTag(doc, name, ctx)
            // (2026-11-26) Names are unique across scopes: a `용어 전용` tag of that name is not put on images.
            if (!tag.fits(io.github.lenticualrstone.civilight.model.cast.TagScope.CHARACTER)) return@act doc
            itemKeys.fold(withTag) { d, k ->
                val item = d.index.galleryItem(k) ?: return@fold d
                if (tag.key in item.tagKeys) d else GalleryActions.setTags(d, k, item.tagKeys + tag.key, ctx.actions())?.doc ?: d
            }
        }
    }

    fun setHolo(itemKey: Key, on: Boolean) = editItem(GalleryUiCopy.HOLO) { d, c -> GalleryActions.setHolo(d, itemKey, on, c) }

    /** `숨기기` of one or several items (GAL-19, GAL-18), one undo step. The caller authenticates before unhiding. */
    fun setHidden(itemKeys: List<Key>, hidden: Boolean, then: (EditResult) -> Unit = {}) =
        act(GalleryUiCopy.BTN_HIDE, then = then) { doc, ctx ->
            itemKeys.fold(doc) { d, k -> GalleryActions.setHidden(d, k, hidden, ctx.actions())?.doc ?: d }
        }

    /** `앞으로` · `뒤로` · `맨 앞` · `맨 뒤` (GAL-15): one undo step per tap; nothing when not possible. */
    fun move(itemKey: Key, move: GalleryMove, locked: Boolean) {
        val label = when (move) {
            GalleryMove.FORWARD -> GalleryUiCopy.BTN_FORWARD
            GalleryMove.BACKWARD -> GalleryUiCopy.BTN_BACKWARD
            GalleryMove.FIRST -> GalleryUiCopy.BTN_FIRST
            GalleryMove.LAST -> GalleryUiCopy.BTN_LAST
        }
        editItem(label) { d, c -> GalleryActions.move(d, itemKey, move, c, locked) }
    }

    /** `이 뒤로` (null = `맨 앞으로`, GAL-15, T-GAL-25), one undo step. */
    fun moveAfter(itemKey: Key, afterKey: Key?, locked: Boolean) =
        editItem(GalleryUiCopy.BTN_AFTER_THIS) { d, c -> GalleryActions.moveAfter(d, itemKey, afterKey, c, locked) }

    /** `지우기` (GAL-20, GAL-18): to the trash, one undo step; [then] gets the toast (`이미지 지움 · <title>`). */
    fun delete(itemKeys: List<Key>, then: (EditResult, Toast?) -> Unit) {
        var toast: Toast? = null
        act(GalleryUiCopy.BTN_DELETE, then = { r -> then(r, toast) }) { doc, ctx ->
            val result = GalleryActions.delete(doc, itemKeys, ctx.actions()) ?: return@act doc
            toast = result.toast
            result.doc
        }
    }

    /** `적용` of the gallery crop (GAL-12): the edited ratios and the holo flag, one undo step named `적용`. */
    fun applyCrop(itemKey: Key, session: CropSession, label: String, then: (EditResult) -> Unit) =
        act(label, then = then) { doc, ctx ->
            var d = doc
            session.crops().forEach { (ratio, crop) -> d = GalleryActions.setCrop(d, itemKey, ratio, crop, ctx.actions())?.doc ?: d }
            GalleryActions.setHolo(d, itemKey, session.holo, ctx.actions())?.doc ?: d
        }

    /** `초상으로` · `배경으로` · `표지로` with a preset item (GAL-14): placed at once, one undo step. */
    fun placePreset(owner: ImageOwner, presetId: String, holo: Boolean, label: String, then: (EditResult) -> Unit) =
        act(label, then = then) { doc, ctx -> ImageEdits.placePreset(doc, owner, presetId, holo, ctx) }

    fun undoLast() {
        viewModelScope.launch { undo() }
    }

    companion object {
        /** The view model key of the gallery sheets inside whatever entry hosts them. */
        fun sheetKey(projectKey: Key): String = "gallery-edit-$projectKey"
    }
}
