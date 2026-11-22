package io.github.lenticualrstone.civilight.domain.checks.sources

import io.github.lenticualrstone.civilight.domain.checks.CheckInputs
import io.github.lenticualrstone.civilight.domain.checks.CheckResult
import io.github.lenticualrstone.civilight.domain.story.transition.TransitionChecks
import io.github.lenticualrstone.civilight.domain.story.transition.Transitions
import io.github.lenticualrstone.civilight.domain.story.travel.TravelCheck
import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.Position
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind

/**
 * Shared state of one check run: the doc, inputs, the time engine, unit codes, hiding, and the
 * engines several check types read (computed once per run).
 */
internal class CheckScope(val doc: ProjectDoc, val inputs: CheckInputs) {
    val times: SceneTimes by lazy { SceneTimes(doc) }
    val codes: UnitCodes by lazy { UnitCodes(doc) }
    val hiding: Hiding = Hiding(doc, inputs.hiddenUnlocked)

    /** Rows 1–3 and the `점검 중` flag. */
    val terms: TermSource.Output by lazy { TermSource.compute(this) }

    val travel: TravelCheck by lazy { TravelCheck(doc, inputs.travel, times) }

    /** Rows 10–12 (one item per warning). */
    val travelWarnings: List<CheckResult> by lazy { travel.warnings.map { TravelSource.warning(this, it) } }

    val transitionChecks: TransitionChecks by lazy { TransitionChecks(Transitions(doc, times)) }

    /** Manuscript rank of a scene's start (row order). */
    fun sceneOrder(sceneKey: Key): Long? = doc.manuscript.rankOf(Position.Start(sceneKey))?.value

    fun blockOrder(blockKey: Key): Long? = doc.manuscript.blockRankOf(blockKey)?.value

    fun sceneRef(sceneKey: Key): String? = codes.sceneRef(sceneKey)
}

/**
 * Which subjects are hidden while the project's hidden items are locked (R19 §6). A scene is hidden
 * when it or any unit above it is; a block follows its scene.
 */
internal class Hiding(private val doc: ProjectDoc, unlocked: Boolean) {
    val locked: Boolean = !unlocked

    fun unitHidden(unitKey: Key): Boolean {
        val unit = doc.index.unit(unitKey) ?: return false
        return unit.hidden || doc.index.ancestors(unit).any { it.hidden }
    }

    fun scene(sceneKey: Key): Boolean = locked && unitHidden(sceneKey)

    fun block(blockKey: Key): Boolean = locked && doc.index.block(blockKey)?.let { unitHidden(it.sceneKey) } == true

    fun character(key: Key): Boolean = locked && doc.index.character(key)?.hidden == true

    fun term(key: Key): Boolean = locked && doc.index.term(key)?.hidden == true

    fun place(ref: PlaceRef?): Boolean = locked && ref != null && ref.kind.known == PlaceRefKind.PLACE &&
        doc.index.place(ref.key)?.hidden == true
}
