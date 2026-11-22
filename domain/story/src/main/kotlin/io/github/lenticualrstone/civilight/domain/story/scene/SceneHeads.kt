package io.github.lenticualrstone.civilight.domain.story.scene

import io.github.lenticualrstone.civilight.domain.story.time.ResolvedTime
import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.time.TimeLabels
import io.github.lenticualrstone.civilight.domain.story.transition.Transitions
import io.github.lenticualrstone.civilight.domain.story.units.PlaceNames
import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/**
 * A scene head (`씬 머리`, SCR-46): line 1 [text] from scene properties only, line 2 [transitionLine]
 * (hidden with the `시스템 텍스트` layer). [placeDeleted] draws the place dotted (R20).
 */
data class SceneHead(val sceneKey: Key, val text: String, val transitionLine: String, val placeDeleted: Boolean)

/** One row of the scene-head ⋯ sheet: label, value line, and whether it can be used. */
data class HeadMenuRow(val label: String, val value: String, val enabled: Boolean = true)

/** The scene-head ⋯ sheet (SCR-47, STO-07): title `씬 4 · 1-7` and rows. */
data class HeadMenu(val title: String, val rows: List<HeadMenuRow>)

/** Scene heads and their ⋯ menu (R22 §2–§3, SCR-46/47, STO-07). */
class SceneHeads(
    val doc: ProjectDoc,
    val times: SceneTimes = SceneTimes(doc),
    val codes: UnitCodes = UnitCodes(doc),
) {
    private val labels = TimeLabels(times, codes)
    private val places = PlaceNames(doc)
    private val transitions = Transitions(doc, times)

    /**
     * `씬 4 · 결사의 문 · 2일째 아침 06:10`, `씬 2 · 장소 없음 · 때 없음`, `씬 2 · 하층 거리 · 회상 · 1195`
     * (STO-24 order). A deleted place reads `지운 장소 · 잿더미 시장`.
     */
    fun text(sceneKey: Key): String? {
        val scene = doc.index.unit(sceneKey) ?: return null
        val place = places.label(scene.place)?.wide ?: "장소 없음"
        return "${codes.sceneLabel(scene)} · $place · ${labels.head(sceneKey)}"
    }

    fun head(sceneKey: Key): SceneHead? {
        val scene = doc.index.unit(sceneKey) ?: return null
        return SceneHead(sceneKey, text(sceneKey)!!, transitions.headLine(sceneKey), places.label(scene.place)?.deleted == true)
    }

    /** `결사의 문 · 2일째 아침 06:10` (`장소와 때` value in the ⋯ sheet); parts that are missing are left out. */
    fun placeAndTime(sceneKey: Key): String {
        val scene = doc.index.unit(sceneKey) ?: return ""
        val place = places.label(scene.place)?.wide
        val time = if (times.resolve(sceneKey) == ResolvedTime.None) null else labels.head(sceneKey)
        return listOfNotNull(place, time).joinToString(" · ").ifEmpty { "장소 없음 · 때 없음" }
    }

    /**
     * Rows `장소와 때`, `전환 문구`, `앞 씬과 합치기` (absent for the first scene of an episode),
     * `씬 지우기 | 휴지통 · 30일`, `앞에 새 씬 | 빈 씬 · 씬 4 앞` (`[PROPOSED]`). Read-only projects
     * get every mutating row disabled with the value `읽기 전용`.
     */
    fun menu(sceneKey: Key, readOnly: Boolean = false): HeadMenu? {
        val scene = doc.index.unit(sceneKey) ?: return null
        val episode = codes.episodeOf(scene)?.let(codes::episodeCode)
        val label = codes.sceneLabel(scene)
        val siblings = doc.index.children(scene.parentKey).filter { doc.liveness.isUnitLive(it.key) && it.level == scene.level }
        val previous = siblings.getOrNull(siblings.indexOfFirst { it.key == sceneKey } - 1)
        fun row(label: String, value: String) = if (readOnly) HeadMenuRow(label, "읽기 전용", enabled = false) else HeadMenuRow(label, value)
        val rows = buildList {
            add(row("장소와 때", placeAndTime(sceneKey)))
            add(row("전환 문구", transitions.menuValue(sceneKey)))
            previous?.let { add(row("앞 씬과 합치기", "${codes.sceneLabel(it)} · ${it.title ?: "미정"}")) }
            add(row("씬 지우기", "휴지통 · 30일"))
            add(row("앞에 새 씬", "빈 씬 · $label 앞"))
        }
        return HeadMenu(listOfNotNull(label, episode).joinToString(" · "), rows)
    }
}
