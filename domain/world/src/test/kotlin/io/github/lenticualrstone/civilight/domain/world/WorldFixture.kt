package io.github.lenticualrstone.civilight.domain.world

import io.github.lenticualrstone.civilight.domain.world.common.DocRuns
import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.domain.world.term.CandidateIndex
import io.github.lenticualrstone.civilight.domain.world.term.ScriptSpot
import io.github.lenticualrstone.civilight.domain.world.term.TermAccess
import io.github.lenticualrstone.civilight.domain.world.term.TermCandidate
import io.github.lenticualrstone.civilight.domain.world.term.TermQueries
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.UlidGenerator
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.world.Term

/** Fixture F of WLD §4.1 built from the example project, plus small edit helpers for tests. */
object WorldFixture {
    val K = ExampleKeys
    val T1: Key = ExampleKeys.TERM_ORIGINIUM_LANTERN

    /** Deterministic keys, clock and device. */
    fun ctx(seed: Long = 7): EditContext {
        val gen = UlidGenerator(clock = { ExampleClock.NOW }, random = java.util.Random(seed))
        return EditContext(newKey = gen::next, now = { ExampleClock.NOW }, device = "test-device")
    }

    /** The example project: T1 has aliases `오리지늄 · 빛돌` (state B). */
    fun stateB(): ProjectDoc = ExampleProject.build()

    /** State A: T1's aliases are `오리지늄` only (fixture.md conflict 8). */
    fun stateA(): ProjectDoc = withTerm(stateB(), T1) { it.copy(aliases = listOf("오리지늄")) }

    fun withTerm(doc: ProjectDoc, key: Key, f: (Term) -> Term): ProjectDoc =
        doc.copy(world = doc.world.copy(terms = doc.world.terms.map { if (it.key == key) f(it) else it }))

    fun addTerm(doc: ProjectDoc, key: Key, name: String, aliases: List<String> = emptyList()): ProjectDoc {
        val term = Term(key = key, order = OrderKeys.after(doc.world.terms.maxOf { it.order }), name = name, aliases = aliases)
        return doc.copy(world = doc.world.copy(terms = doc.world.terms + term))
    }

    fun index(doc: ProjectDoc, access: TermAccess = TermAccess()): CandidateIndex = CandidateIndex.build(doc, access)

    fun queries(doc: ProjectDoc, access: TermAccess = TermAccess()): TermQueries = TermQueries(doc, index(doc, access), access)

    /** Replaces a block's runs with one plain run. */
    fun withBlockText(doc: ProjectDoc, blockKey: Key, text: String): ProjectDoc = withBlockRuns(doc, blockKey, listOf(TextRun.Plain(text)))

    fun withBlockRuns(doc: ProjectDoc, blockKey: Key, runs: List<TextRun>): ProjectDoc =
        DocRuns.updateBlock(doc, blockKey) { it.copy(runs = runs) } ?: error("no block $blockKey")

    fun withBlock(doc: ProjectDoc, blockKey: Key, f: (Block) -> Block): ProjectDoc =
        DocRuns.updateBlock(doc, blockKey, f) ?: error("no block $blockKey")

    /** A new block right after [afterKey] in the same scene (number taken from the episode's counter range). */
    fun addBlockAfter(
        doc: ProjectDoc,
        afterKey: Key,
        newKey: Key,
        runs: List<TextRun>,
        kind: BlockKind = BlockKind.NARRATION,
        number: Int = 900,
        speakerKey: Key? = null,
    ): ProjectDoc {
        val after = doc.index.block(afterKey) ?: error("no block $afterKey")
        val siblings = doc.index.blocksOfScene(after.sceneKey)
        val next = siblings.getOrNull(siblings.indexOfFirst { it.key == afterKey } + 1)
        val block = Block(
            key = newKey, sceneKey = after.sceneKey, order = OrderKeys.between(after.order, next?.order), number = number,
            kind = kind.open(), speakerKey = speakerKey, runs = runs,
        )
        val group = doc.index.episodeGroupOfBlock(afterKey)!!
        val file = doc.episodes.getValue(group)
        return doc.copy(episodes = doc.episodes + (group to file.copy(blocks = file.blocks + block)))
    }

    /** Puts a block first in another scene (moving it across episodes regroups the files). */
    fun moveBlockToSceneStart(doc: ProjectDoc, blockKey: Key, sceneKey: Key): ProjectDoc {
        val first = doc.index.blocksOfScene(sceneKey).firstOrNull()
        val moved = withBlock(doc, blockKey) { it.copy(sceneKey = sceneKey, order = OrderKeys.before(first?.order)) }
        return moved.regroupBlocks()
    }

    fun link(key: Key, term: Key, text: String): TextRun = TextRun.Link(key, LinkTarget(LinkTargetKind.TERM, term), text)
    fun plain(text: String): TextRun = TextRun.Plain(text)

    fun addReveal(doc: ProjectDoc, recordKey: Key, term: Key, anchor: Anchor): ProjectDoc =
        doc.copy(changes = doc.changes.copy(records = doc.changes.records + ChangeRecord(recordKey, ChangeTarget.TermReveal(term), anchor = anchor)))

    fun removeReveal(doc: ProjectDoc, term: Key): ProjectDoc =
        doc.copy(changes = doc.changes.copy(records = doc.changes.records.filterNot { (it.target as? ChangeTarget.TermReveal)?.termKey == term }))

    fun trashBlock(doc: ProjectDoc, blockKey: Key): ProjectDoc = withBlock(doc, blockKey) { it.copy(deletedAt = ExampleClock.NOW) }

    fun candidatesAt(index: CandidateIndex, blockKey: Key, term: Key = T1): List<TermCandidate> =
        index.at(ScriptSpot.BlockText(blockKey)).filter { it.termKey == term }

    /** Test key `n` (valid ULID shape, distinct from the example's families). */
    fun key(n: Int): Key = ExampleKeys.TIME + "TEST" + n.toString().padStart(12, '0')
}
