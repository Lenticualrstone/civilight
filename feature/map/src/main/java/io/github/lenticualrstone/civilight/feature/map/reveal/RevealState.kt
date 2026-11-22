package io.github.lenticualrstone.civilight.feature.map.reveal

import io.github.lenticualrstone.civilight.domain.world.common.StoryCodes
import io.github.lenticualrstone.civilight.domain.world.map.MapReveal
import io.github.lenticualrstone.civilight.domain.world.map.MapViews
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.canvas.MapBase
import io.github.lenticualrstone.civilight.feature.map.canvas.MapBases
import io.github.lenticualrstone.civilight.feature.map.canvas.MapScene
import io.github.lenticualrstone.civilight.feature.map.canvas.MapScenes
import io.github.lenticualrstone.civilight.feature.map.canvas.SceneMode
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.story.StoryUnit

/** A stop of the point scrubber: an episode (`1-5`); [current] when the lens is at its end. */
data class RevealStop(val unitKey: Key, val code: String, val current: Boolean)

/** 시점별 공개 (WLD-49, boards `MapReveal`, `MMapReveal`). */
data class RevealUi(
    /** `세계 지도 › 하층 구역`. */
    val small: String,
    /** `1-5 시점 · 보이는 것만`, or `보이는 것만` at 최신. */
    val modeLine: String,
    /** `숨은 것 흐리게` (the map's stored setting). */
    val dim: Boolean,
    val stops: List<RevealStop>,
    /** `보임 · 핀 4 · 영역 1` and `아직 공개 전 · 2`, from `MapReveal.at`. */
    val visibleChip: String,
    val pendingChip: String,
    val base: MapBase,
    val scene: MapScene,
)

object RevealState {
    /** Live units of the episode level in manuscript order (scenes in a one-level structure). */
    fun episodes(doc: ProjectDoc): List<StoryUnit> {
        val structure = doc.project.structure
        val level = structure.episodeLevel ?: structure.sceneLevel
        val out = ArrayList<StoryUnit>()
        fun visit(parent: Key?) {
            for (u in doc.index.children(parent)) {
                if (!doc.liveness.isLive(u)) continue
                if (u.level == level) out += u else if (u.level < level) visit(u.key)
            }
        }
        visit(null)
        return out
    }

    /** The episode the lens stands on: the unit itself, or the episode of a scene point. */
    internal fun lensEpisode(doc: ProjectDoc, lens: LensPoint, episodes: List<StoryUnit>): Key? {
        val key = (lens as? LensPoint.UnitEnd)?.unitKey ?: return null
        if (episodes.any { it.key == key }) return key
        val unit = doc.index.unit(key) ?: return null
        return doc.index.ancestors(unit).firstOrNull { a -> episodes.any { it.key == a.key } }?.key
    }

    fun build(doc: ProjectDoc, mapKey: Key, lens: LensPoint, hiddenOpen: Boolean): RevealUi? {
        val map = doc.maps[mapKey]?.takeIf { doc.liveness.isMapLive(it.key) } ?: return null
        val codes = StoryCodes(doc)
        val episodes = episodes(doc)
        val at = lensEpisode(doc, lens, episodes)
        val view = MapReveal.at(doc, map, lens, hiddenOpen)
        val summary = view.summary.split(" | ")
        val lensCode = (lens as? LensPoint.UnitEnd)?.let { codes.unitLabel(it.unitKey) }
        return RevealUi(
            small = MapCopy.crumbs(MapViews.breadcrumb(doc, map).map { it.text }),
            modeLine = lensCode?.let(MapCopy::revealMode) ?: MapCopy.REVEAL_VISIBLE_ONLY,
            dim = map.revealViewDim,
            stops = episodes.map { RevealStop(it.key, codes.unitLabel(it.key).orEmpty(), it.key == at) },
            visibleChip = summary.first(),
            pendingChip = summary.getOrElse(1) { "" },
            base = MapBases.of(map),
            scene = MapScenes.build(doc, map, lens, hiddenOpen, SceneMode.REVEAL, revealDim = map.revealViewDim),
        )
    }
}
