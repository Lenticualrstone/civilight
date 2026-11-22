package io.github.lenticualrstone.civilight.feature.script.assets

import io.github.lenticualrstone.civilight.domain.script.ScriptAssets
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.CueKind
import io.github.lenticualrstone.civilight.model.script.ScriptAssetKind
import io.github.lenticualrstone.civilight.ui.search.KoreanSearch

/** Filter chips of `에셋` (SCR-61): `전체 · 배경 · BGM · 효과음 · CG · 안 쓰는 것`. */
enum class AssetFilter(val kind: ScriptAssetKind?) {
    ALL(null), BACKGROUND(ScriptAssetKind.BACKGROUND), BGM(ScriptAssetKind.BGM), SFX(ScriptAssetKind.SFX), CG(ScriptAssetKind.CG), UNUSED(null);

    /** The chip word, from the domain's list (same order). */
    val label: String get() = ScriptAssets.FILTERS[ordinal]
}

/** What the 56 × 40 thumbnail shows: the image for 배경 · CG, a waveform for BGM · 효과음. */
sealed interface AssetThumb {
    /** [hash] null = no image yet (a tinted box). */
    data class Image(val hash: String?) : AssetThumb
    data object Audio : AssetThumb
}

/** One row: name (serif bold 16), `{file} · {uses}` or `{file} · {length}[ · 반복]`, the first episode or `비어 있음`. */
data class AssetRow(
    val key: Key,
    val name: String,
    val subline: String,
    val trailing: String?,
    /** `비어 있음` (a CG without an image) is drawn in `warn`. */
    val trailingWarn: Boolean,
    val thumb: AssetThumb,
)

/** A section `배경 · 4` with its rows. */
data class AssetSection(val title: String, val rows: List<AssetRow>)

data class AssetsUi(
    val projectName: String,
    val filter: AssetFilter,
    val query: String,
    val sections: List<AssetSection>,
    val readOnly: Boolean,
    /** Assets in the project, whatever the filter. */
    val total: Int,
) {
    /** Nothing matches the chip or the search, while the project has assets. */
    val nothingFound: Boolean get() = sections.isEmpty() && total > 0

    /** The project has no asset yet. */
    val noAssets: Boolean get() = total == 0
}

/**
 * The `에셋` list (SCR-61, board `Assets`): sections per kind in the order `배경 · BGM · 효과음 · CG`,
 * titled with the count of that kind in the project (`배경 · 4`, as the domain's sections), rows in
 * the project's asset order. A filter keeps one kind, or the assets no direction cue references
 * (`안 쓰는 것`); the query matches the name or the file name (초성 too). `이 에피소드 n회` counts
 * the episode being written ([episodeKey]).
 */
object AssetsModel {
    fun of(doc: ProjectDoc, filter: AssetFilter, query: String, episodeKey: Key?, readOnly: Boolean): AssetsUi {
        val usage = ScriptAssets.usage(doc)
        val q = query.trim()
        val shown = usage.filter { u ->
            val kind = u.asset.kind.known
            val byFilter = when (filter) {
                AssetFilter.ALL -> true
                AssetFilter.UNUSED -> u.unused
                else -> kind == filter.kind
            }
            byFilter && (q.isEmpty() || KoreanSearch.matchesAny(listOf(u.asset.name, u.asset.fileName), q))
        }
        val sections = ScriptAssetKind.entries.mapNotNull { kind ->
            val rows = shown.filter { it.asset.kind.known == kind }.map { row(it, episodeKey) }
            if (rows.isEmpty()) return@mapNotNull null
            val total = doc.assets.scriptAssets.count { it.kind.known == kind }
            AssetSection("${ScriptAssets.kindWord(kind)} · $total", rows)
        }
        return AssetsUi(doc.project.name, filter, query, sections, readOnly, doc.assets.scriptAssets.size)
    }

    fun row(usage: ScriptAssets.Usage, episodeKey: Key?): AssetRow {
        val a = usage.asset
        val kind = a.kind.known
        val trailing = ScriptAssets.trailing(usage)
        return AssetRow(
            key = a.key,
            name = a.name,
            subline = ScriptAssets.subline(usage, episodeKey),
            trailing = trailing,
            trailingWarn = kind == ScriptAssetKind.CG && a.fileHash == null,
            thumb = if (kind == ScriptAssetKind.BGM || kind == ScriptAssetKind.SFX) AssetThumb.Audio else AssetThumb.Image(a.fileHash),
        )
    }

    /**
     * The cue picker (SCR-08 · SCR-61: "the same list, filtered to one kind"): the rows of [kind]'s
     * assets in the list's order, with the list's sublines for [episodeKey] and its thumbnails.
     * Empty for `화면 효과` (effects are not assets).
     */
    fun pickerRows(doc: ProjectDoc, kind: CueKind, episodeKey: Key?): List<AssetRow> {
        val usage = ScriptAssets.usage(doc).associateBy { it.asset.key }
        return ScriptAssets.forCue(doc, kind).mapNotNull { usage[it.key] }.map { row(it, episodeKey) }
    }

    /**
     * The episode `이 에피소드 n회` refers to: the numbering unit of the block written last
     * (`ProjectActivity`), which is the episode the script was showing. Null without one.
     */
    fun currentEpisode(doc: ProjectDoc): Key? {
        val block = doc.app.activity.activity.lastEditedBlockKey?.let(doc.index::block) ?: return null
        return doc.index.numberingUnitKeyOfScene(block.sceneKey)
    }
}
