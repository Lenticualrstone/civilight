package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.script.ScriptAsset
import io.github.lenticualrstone.civilight.model.script.ScriptAssetKind

/**
 * Adding and changing `에셋` (SCR-61 `[PROPOSED]` form): kind, name, file name, an optional image
 * (배경 · CG), length and `반복` (BGM · 효과음) and a place link (배경). Fields a kind does not have
 * are never stored. The kind of an existing asset never changes, so a cue never points at an asset
 * of another kind. Deleting is not offered (Q-29 open).
 */
object ScriptAssetEdits {
    /** What the form holds; [kind] is ignored by [update]. */
    data class Draft(
        val kind: ScriptAssetKind,
        val name: String,
        val fileName: String,
        val imageHash: String? = null,
        val durationMs: Long? = null,
        val loop: Boolean = false,
        val placeKey: Key? = null,
    )

    /** Kinds that carry an image (`배경 · CG`). */
    fun hasImage(kind: ScriptAssetKind?): Boolean = kind == ScriptAssetKind.BACKGROUND || kind == ScriptAssetKind.CG

    /** Kinds that carry a length and `반복` (`BGM · 효과음`). */
    fun hasLength(kind: ScriptAssetKind?): Boolean = kind == ScriptAssetKind.BGM || kind == ScriptAssetKind.SFX

    /** Kinds that carry a place link (`배경`). */
    fun hasPlace(kind: ScriptAssetKind?): Boolean = kind == ScriptAssetKind.BACKGROUND

    /** A draft can be saved once it has a name and a file name. */
    fun canSave(draft: Draft): Boolean = draft.name.isNotBlank() && draft.fileName.isNotBlank()

    /** The draft of an existing asset. */
    fun draftOf(asset: ScriptAsset): Draft = Draft(
        kind = asset.kind.known ?: ScriptAssetKind.BACKGROUND,
        name = asset.name,
        fileName = asset.fileName,
        imageHash = asset.fileHash,
        durationMs = asset.durationMs,
        loop = asset.loop,
        placeKey = asset.placeKey,
    )

    /** Adds the asset at the end of the list; returns the doc and the new key (unchanged doc when it cannot be saved). */
    fun add(doc: ProjectDoc, draft: Draft, ctx: EditContext): Pair<ProjectDoc, Key?> {
        if (!canSave(draft)) return doc to null
        val key = ctx.newKey()
        val asset = fill(
            ScriptAsset(key = key, kind = draft.kind.open(), name = "", fileName = ""),
            draft.copy(kind = draft.kind),
            ctx,
        )
        return doc.copy(assets = doc.assets.copy(scriptAssets = doc.assets.scriptAssets + asset)) to key
    }

    /** Changes the asset [key] from [draft] (its kind stays); unchanged doc when nothing differs or it cannot be saved. */
    fun update(doc: ProjectDoc, key: Key, draft: Draft, ctx: EditContext): ProjectDoc {
        if (!canSave(draft)) return doc
        val list = doc.assets.scriptAssets
        val old = list.firstOrNull { it.key == key } ?: return doc
        val updated = fill(old, draft.copy(kind = old.kind.known ?: draft.kind), ctx)
        if (updated.copy(updatedAt = old.updatedAt, updatedBy = old.updatedBy) == old) return doc
        return doc.copy(assets = doc.assets.copy(scriptAssets = list.map { if (it.key == key) updated else it }))
    }

    /** Lists an imported image in the project's blob index (once per hash), so packages carry it. */
    fun withBlob(doc: ProjectDoc, blob: Blob): ProjectDoc =
        if (doc.assets.blobs.any { it.hash == blob.hash }) doc
        else doc.copy(assets = doc.assets.copy(blobs = doc.assets.blobs + blob))

    private fun fill(asset: ScriptAsset, draft: Draft, ctx: EditContext): ScriptAsset {
        val kind = draft.kind
        return asset.copy(
            name = draft.name.trim(),
            fileName = draft.fileName.trim(),
            fileHash = draft.imageHash.takeIf { hasImage(kind) },
            durationMs = draft.durationMs?.takeIf { hasLength(kind) && it > 0 },
            loop = hasLength(kind) && draft.loop,
            placeKey = draft.placeKey.takeIf { hasPlace(kind) },
            updatedAt = ctx.now,
            updatedBy = ctx.device,
        )
    }
}
