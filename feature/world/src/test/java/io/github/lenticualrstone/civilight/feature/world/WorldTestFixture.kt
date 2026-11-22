package io.github.lenticualrstone.civilight.feature.world

import io.github.lenticualrstone.civilight.domain.world.common.DocRuns
import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.domain.world.term.CandidateIndex
import io.github.lenticualrstone.civilight.domain.world.term.TermAccess
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.UlidGenerator
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.world.Term

/** Fixture F of WLD §4.1 from the example project (fixture.md conflict 8: the project is state B). */
object WorldTestFixture {
    val K = ExampleKeys
    val T1: Key = ExampleKeys.TERM_ORIGINIUM_LANTERN
    const val PROJECT = "P"

    /** Deterministic keys, clock and device. */
    fun ctx(seed: Long = 7): EditContext {
        val gen = UlidGenerator(clock = { ExampleClock.NOW }, random = java.util.Random(seed))
        return EditContext(newKey = gen::next, now = { ExampleClock.NOW }, device = "test-device")
    }

    fun keys(seed: Long = 11): () -> Key {
        val gen = UlidGenerator(clock = { ExampleClock.NOW }, random = java.util.Random(seed))
        return gen::next
    }

    /** T1 with aliases `오리지늄 · 빛돌`. */
    fun stateB(): ProjectDoc = ExampleProject.build()

    /** T1 with alias `오리지늄` only. */
    fun stateA(): ProjectDoc = withTerm(stateB(), T1) { it.copy(aliases = listOf("오리지늄")) }

    fun withTerm(doc: ProjectDoc, key: Key, f: (Term) -> Term): ProjectDoc =
        doc.copy(world = doc.world.copy(terms = doc.world.terms.map { if (it.key == key) f(it) else it }))

    fun index(doc: ProjectDoc, hiddenOpen: Boolean = false): CandidateIndex = CandidateIndex.build(doc, TermAccess(hiddenOpen))

    /** The lens at the end of [unitKey] (e.g. episode 1-5). */
    fun atLens(doc: ProjectDoc, unitKey: Key): ProjectDoc =
        doc.copy(app = doc.app.copy(view = doc.app.view.copy(lens = doc.app.view.lens.copy(point = LensPoint.UnitEnd(unitKey)))))

    fun link(linkKey: Key, termKey: Key, text: String): TextRun.Link =
        TextRun.Link(linkKey = linkKey, target = LinkTarget(LinkTargetKind.TERM.open(), termKey), text = text)

    fun appendRun(doc: ProjectDoc, blockKey: Key, run: TextRun): ProjectDoc =
        DocRuns.updateBlock(doc, blockKey) { it.copy(runs = it.runs + run) } ?: error("no block $blockKey")
}
