package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.CueKind
import io.github.lenticualrstone.civilight.model.script.ScriptAsset
import io.github.lenticualrstone.civilight.model.script.ScriptAssetKind

/** `에셋` list (SCR-61): derived use counts, first-use episode, `안 쓰는 것`, row sublines. */
object ScriptAssets {
    /** Uses of one asset by live direction cues. */
    data class Usage(
        val asset: ScriptAsset,
        val total: Int,
        /** Numbering unit key → uses there. */
        val perEpisode: Map<Key, Int>,
        /** First episode in manuscript order that uses it (`1-7`), or null. */
        val firstEpisodeCode: String?,
    ) {
        val unused: Boolean get() = total == 0
    }

    /** Usage of every asset of the project, in the asset list's order. */
    fun usage(doc: ProjectDoc): List<Usage> {
        val counts = mutableMapOf<Key, MutableMap<Key, Int>>()
        val first = mutableMapOf<Key, Key>()
        for (b in doc.manuscript.liveBlocks) for (c in b.cues) {
            val asset = c.assetKey ?: continue
            if (c.kind.known == CueKind.SCREEN_EFFECT) continue
            val unit = doc.numberingUnitOf(b)
            counts.getOrPut(asset) { mutableMapOf() }.merge(unit, 1, Int::plus)
            first.putIfAbsent(asset, b.sceneKey)
        }
        return doc.assets.scriptAssets.map { a ->
            val per = counts[a.key].orEmpty()
            Usage(a, per.values.sum(), per, first[a.key]?.let { Labels.episodeCodeOfScene(doc, it) })
        }
    }

    /** Filter chips `전체 · 배경 · BGM · 효과음 · CG · 안 쓰는 것`. */
    val FILTERS: List<String> = listOf("전체", "배경", "BGM", "효과음", "CG", "안 쓰는 것")

    fun kindWord(kind: ScriptAssetKind?): String = when (kind) {
        ScriptAssetKind.BACKGROUND -> "배경"
        ScriptAssetKind.BGM -> "BGM"
        ScriptAssetKind.SFX -> "효과음"
        ScriptAssetKind.CG -> "CG"
        null -> "에셋"
    }

    /** Section headers with counts: `배경 · 4`. */
    fun sections(doc: ProjectDoc): List<String> = ScriptAssetKind.entries.mapNotNull { k ->
        doc.assets.scriptAssets.count { it.kind.known == k }.takeIf { it > 0 }?.let { "${kindWord(k)} · $it" }
    }

    /** Assets no direction cue references. */
    fun unused(doc: ProjectDoc): List<ScriptAsset> = usage(doc).filter { it.unused }.map { it.asset }

    /** `2분 41초`, `3분 05초` (seconds in two digits after minutes, board `Assets`), `0.8초`, `12초`. */
    fun duration(ms: Long): String {
        val totalSeconds = ms / 1000
        return when {
            ms >= 60_000 -> "${totalSeconds / 60}분 ${(totalSeconds % 60).toString().padStart(2, '0')}초"
            ms % 1000 == 0L -> "${totalSeconds}초"
            else -> "%.1f초".format(java.util.Locale.US, ms / 1000.0)
        }
    }

    /**
     * Row subline: images `{file} · 이 에피소드 3회` (uses in [currentEpisodeKey]) or `{file} · 6회`;
     * audio `{file} · 2분 41초 · 반복`; a CG without an image `{file} · 아직 이미지 없음`.
     */
    fun subline(usage: Usage, currentEpisodeKey: Key? = null): String {
        val a = usage.asset
        return when (a.kind.known) {
            ScriptAssetKind.BGM, ScriptAssetKind.SFX ->
                listOfNotNull(a.fileName, a.durationMs?.let(::duration), if (a.loop) "반복" else null).joinToString(" · ")
            ScriptAssetKind.CG -> if (a.fileHash == null) "${a.fileName} · 아직 이미지 없음" else uses(usage, currentEpisodeKey)
            else -> uses(usage, currentEpisodeKey)
        }
    }

    private fun uses(usage: Usage, currentEpisodeKey: Key?): String {
        val here = currentEpisodeKey?.let { usage.perEpisode[it] }
        return if (here != null) "${usage.asset.fileName} · 이 에피소드 ${here}회" else "${usage.asset.fileName} · ${usage.total}회"
    }

    /** Right side: the first episode that uses it (`1-7`), or `비어 있음` for a CG without an image. */
    fun trailing(usage: Usage): String? =
        if (usage.asset.kind.known == ScriptAssetKind.CG && usage.asset.fileHash == null) "비어 있음" else usage.firstEpisodeCode

    /** Picker list for a cue kind (SCR-08): assets of the matching kind. */
    fun forCue(doc: ProjectDoc, kind: CueKind): List<ScriptAsset> {
        val want = when (kind) {
            CueKind.BACKGROUND -> ScriptAssetKind.BACKGROUND
            CueKind.BGM -> ScriptAssetKind.BGM
            CueKind.SFX -> ScriptAssetKind.SFX
            CueKind.CG -> ScriptAssetKind.CG
            CueKind.SCREEN_EFFECT -> return emptyList()
        }
        return doc.assets.scriptAssets.filter { it.kind.known == want }
    }
}
