package io.github.lenticualrstone.civilight.domain.checks

import io.github.lenticualrstone.civilight.domain.cast.voice.VoiceLines
import io.github.lenticualrstone.civilight.domain.story.scene.EditContext
import io.github.lenticualrstone.civilight.domain.story.travel.TravelSettings
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.AnchorState
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import io.github.lenticualrstone.civilight.model.story.SceneTime

/** The example project 「잿빛 새벽의 등불」 and helpers for small edits. */
object Fx {
    private var counter = 0

    fun doc(): ProjectDoc = ExampleProject.build()

    fun inputs(
        hiddenUnlocked: Boolean = false,
        travel: TravelSettings = TravelSettings(),
    ): CheckInputs = CheckInputs(
        now = ExampleClock.NOW,
        travel = travel,
        hiddenUnlocked = hiddenUnlocked,
        recorder = ExampleRecorder.library(),
    )

    fun run(doc: ProjectDoc, inputs: CheckInputs = inputs()): CheckReport = ProjectChecks.run(doc, inputs)

    fun ctx(): EditContext = EditContext(
        newKey = { "01JZ000000TEST" + (++counter).toString().padStart(12, '0') },
        now = ExampleClock.NOW,
        device = "test-device",
    )

    fun ProjectDoc.withUnit(key: Key, change: (StoryUnit) -> StoryUnit): ProjectDoc =
        copy(story = story.copy(units = story.units.map { if (it.key == key) change(it) else it }))

    fun ProjectDoc.withTime(key: Key, time: SceneTime): ProjectDoc = withUnit(key) { it.copy(time = time) }

    fun ProjectDoc.withBlock(key: Key, change: (Block) -> Block): ProjectDoc =
        copy(episodes = episodes.mapValues { (_, f) -> f.copy(blocks = f.blocks.map { if (it.key == key) change(it) else it }) })

    /** Moves a record to another anchor. */
    fun ProjectDoc.withAnchor(recordKey: Key, anchor: Anchor, state: AnchorState = AnchorState.NORMAL, original: Anchor? = null): ProjectDoc =
        copy(changes = changes.copy(records = changes.records.map {
            if (it.key == recordKey) it.copy(anchor = anchor, anchorState = state.open(), originalAnchor = original) else it
        }))

    /** 노아 back from the trash (fixture.md conflict 22: clear the mark and drop `TRASH_NOAH`). */
    fun ProjectDoc.withNoahLive(): ProjectDoc = copy(
        cast = cast.copy(characters = cast.characters.map { if (it.key == K.NOAH) it.copy(deletedAt = null, trashEntryKey = null) else it }),
        trash = trash.copy(entries = trash.entries.filterNot { it.key == K.TRASH_NOAH }),
    )

    fun ProjectDoc.withCharacter(key: Key, change: (Character) -> Character): ProjectDoc =
        copy(cast = cast.copy(characters = cast.characters.map { if (it.key == key) change(it) else it }))

    /** 리엔's empty voice lines removed (`▲ 보이스 비어 있음` resolved). */
    fun ProjectDoc.withoutEmptyVoiceLines(): ProjectDoc = copy(
        cast = cast.copy(voiceLines = cast.voiceLines.filterNot { VoiceLines.isEmpty(it.text) }),
    )

    /**
     * Every open item of the fixture resolved without a new acknowledgement: the reveal of
     * 오리지늄 등불 moved to the start of 1-1 (no ▲, no pre-reveal candidates), #027 given a
     * speaker, 노아 restored, 리엔's empty voice lines removed. (`◇ 장소 없음` is switched off by
     * the caller.)
     */
    fun allResolved(doc: ProjectDoc): ProjectDoc = doc
        .withAnchor(K.R9, Anchor.UnitStart(K.EPISODE_1_1))
        .withBlock(K.BLOCK_1_7_027) { it.copy(speakerKey = K.KAIL) }
        .withNoahLive()
        .withoutEmptyVoiceLines()

    fun CheckReport.types(): List<String> = open.map { it.checkType }

    fun CheckReport.dump(): String = results.joinToString("\n") { r ->
        (if (isAcknowledged(r)) "[ack] " else "[open] ") + r.checkType + " | " + r.title + " | " + r.location + " | " + r.line
    }
}
