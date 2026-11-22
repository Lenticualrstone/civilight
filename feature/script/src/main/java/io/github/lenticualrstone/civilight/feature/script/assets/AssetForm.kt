package io.github.lenticualrstone.civilight.feature.script.assets

import io.github.lenticualrstone.civilight.domain.script.EditContext
import io.github.lenticualrstone.civilight.domain.script.ScriptAssetEdits
import io.github.lenticualrstone.civilight.domain.script.ScriptAssets
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.CueKind
import io.github.lenticualrstone.civilight.model.script.ScriptAssetKind
import io.github.lenticualrstone.civilight.navigation.SheetKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale

/**
 * The asset form (SCR-61 `[PROPOSED]`): [assetKey] null adds an asset of [kind] (the list's filter,
 * or the cue kind it was opened from), else changes that asset. Feature-local: the `에셋` screen and
 * the script's `연출 지시` sheet (`+ 에셋`) both draw it.
 */
@Serializable
data class AssetFormSheetKey(
    val projectKey: Key,
    val assetKey: Key? = null,
    val kind: ScriptAssetKind = ScriptAssetKind.BACKGROUND,
) : SheetKey

/** The form's typed state (saveable as JSON). The length is kept as typed (`161`, `0.8`). */
@Serializable
data class AssetFormDraft(
    val kind: ScriptAssetKind,
    val name: String = "",
    val fileName: String = "",
    val imageHash: String? = null,
    val lengthText: String = "",
    val loop: Boolean = false,
    val placeKey: Key? = null,
    /**
     * The image picked in this form, not yet in the project's blob index: it is listed in the same
     * update as `추가` / `저장`, so a cancelled form leaves no undo step and no stray index row.
     */
    val pickedBlob: Blob? = null,
)

/** What the form draws. */
data class AssetFormUi(
    val title: String,
    val adding: Boolean,
    /** Kinds offered while adding (`배경 · BGM · 효과음 · CG`); an existing asset's kind is fixed. */
    val kinds: List<Pair<ScriptAssetKind, String>>,
    val kindIndex: Int,
    val showImage: Boolean,
    val imageLine: String?,
    val showLength: Boolean,
    /** `2분 41초` under the typed seconds, or null. */
    val lengthPreview: String?,
    val showPlace: Boolean,
    val placeLine: String,
    val canSave: Boolean,
    val primary: String,
    val readOnly: Boolean,
)

object AssetForm {
    val KINDS: List<Pair<ScriptAssetKind, String>> = ScriptAssetKind.entries.map { it to ScriptAssets.kindWord(it) }

    /** The asset kind a cue kind picks from (`화면 효과` has none). */
    fun kindOf(cue: CueKind): ScriptAssetKind? = when (cue) {
        CueKind.BACKGROUND -> ScriptAssetKind.BACKGROUND
        CueKind.BGM -> ScriptAssetKind.BGM
        CueKind.SFX -> ScriptAssetKind.SFX
        CueKind.CG -> ScriptAssetKind.CG
        CueKind.SCREEN_EFFECT -> null
    }

    /** The starting draft: the asset's values, or an empty one of the key's kind. */
    fun initial(doc: ProjectDoc, key: AssetFormSheetKey): AssetFormDraft {
        val asset = key.assetKey?.let { k -> doc.assets.scriptAssets.firstOrNull { it.key == k } }
            ?: return AssetFormDraft(kind = key.kind)
        val d = ScriptAssetEdits.draftOf(asset)
        return AssetFormDraft(d.kind, d.name, d.fileName, d.imageHash, d.durationMs?.let(::lengthText).orEmpty(), d.loop, d.placeKey)
    }

    fun ui(doc: ProjectDoc, key: AssetFormSheetKey, draft: AssetFormDraft, readOnly: Boolean): AssetFormUi {
        val asset = key.assetKey?.let { k -> doc.assets.scriptAssets.firstOrNull { it.key == k } }
        val adding = asset == null
        val kind = asset?.kind?.known ?: draft.kind
        val ms = parseLength(draft.lengthText)
        return AssetFormUi(
            title = if (adding) AssetsCopy.ADD_TITLE else asset.name,
            adding = adding,
            kinds = if (adding) KINDS else KINDS.filter { it.first == kind },
            kindIndex = KINDS.indexOfFirst { it.first == kind }.let { if (adding) it else 0 },
            showImage = ScriptAssetEdits.hasImage(kind),
            imageLine = if (draft.imageHash == null) AssetsCopy.NO_IMAGE else null,
            showLength = ScriptAssetEdits.hasLength(kind),
            lengthPreview = ms?.let(ScriptAssets::duration),
            showPlace = ScriptAssetEdits.hasPlace(kind),
            placeLine = draft.placeKey?.let { doc.index.place(it)?.name } ?: AssetsCopy.NO_PLACE,
            canSave = !readOnly && ScriptAssetEdits.canSave(domainDraft(draft.copy(kind = kind))),
            primary = if (adding) AssetsCopy.ADD else AssetsCopy.SAVE,
            readOnly = readOnly,
        )
    }

    /** Applies the form: adds or changes the asset (one undo step); unchanged when it cannot be saved. */
    fun apply(doc: ProjectDoc, key: AssetFormSheetKey, draft: AssetFormDraft, ctx: EditContext): ProjectDoc {
        val assetKey = key.assetKey
        val written = if (assetKey == null) ScriptAssetEdits.add(doc, domainDraft(draft), ctx).first
        else ScriptAssetEdits.update(doc, assetKey, domainDraft(draft), ctx)
        if (written === doc) return doc
        // The picked image joins the blob index only when the asset still points at it.
        val blob = draft.pickedBlob?.takeIf { it.hash == draft.imageHash && ScriptAssetEdits.hasImage(kindOf(doc, key, draft)) }
        return blob?.let { ScriptAssetEdits.withBlob(written, it) } ?: written
    }

    /** The kind the form writes: an existing asset's own kind, else the draft's. */
    fun kindOf(doc: ProjectDoc, key: AssetFormSheetKey, draft: AssetFormDraft): ScriptAssetKind =
        key.assetKey?.let { k -> doc.assets.scriptAssets.firstOrNull { it.key == k }?.kind?.known } ?: draft.kind

    private val draftJson = Json { ignoreUnknownKeys = true }

    /** The draft as saved state (`rememberSaveable`); [decode] gives null for anything unreadable. */
    fun encode(draft: AssetFormDraft): String = draftJson.encodeToString(AssetFormDraft.serializer(), draft)

    fun decode(text: String): AssetFormDraft? = runCatching { draftJson.decodeFromString(AssetFormDraft.serializer(), text) }.getOrNull()

    /** The form after an image was picked from the gallery (kept until the form is saved). */
    fun withPicked(draft: AssetFormDraft, blob: Blob): AssetFormDraft = draft.copy(imageHash = blob.hash, pickedBlob = blob)

    fun domainDraft(draft: AssetFormDraft): ScriptAssetEdits.Draft = ScriptAssetEdits.Draft(
        kind = draft.kind,
        name = draft.name,
        fileName = draft.fileName,
        imageHash = draft.imageHash,
        durationMs = parseLength(draft.lengthText),
        loop = draft.loop,
        placeKey = draft.placeKey,
    )

    /** Typed seconds → milliseconds: `161`, `0.8`, `2:41`. Null for blank or unreadable input. */
    fun parseLength(text: String): Long? {
        val t = text.trim().replace(',', '.')
        if (t.isEmpty()) return null
        val parts = t.split(':')
        val seconds = when (parts.size) {
            1 -> parts[0].toDoubleOrNull()
            2 -> {
                val m = parts[0].toLongOrNull()
                val s = parts[1].toDoubleOrNull()
                if (m == null || s == null || s >= 60) null else m * 60 + s
            }
            else -> null
        } ?: return null
        if (seconds <= 0 || seconds.isNaN() || seconds.isInfinite()) return null
        return Math.round(seconds * 1000)
    }

    /** Milliseconds → the typed form (`161`, `0.8`). */
    fun lengthText(ms: Long): String =
        if (ms % 1000 == 0L) (ms / 1000).toString() else String.format(Locale.US, "%.1f", ms / 1000.0).trimEnd('0').trimEnd('.')
}
