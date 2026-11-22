package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.domain.script.StoryTimes
import io.github.lenticualrstone.civilight.domain.script.VoiceIds
import io.github.lenticualrstone.civilight.domain.trash.TrashContext
import io.github.lenticualrstone.civilight.feature.script.panel.InSceneRow
import io.github.lenticualrstone.civilight.feature.script.panel.PanelTab
import io.github.lenticualrstone.civilight.feature.script.panel.PropsModel
import io.github.lenticualrstone.civilight.feature.script.panel.RefRow
import io.github.lenticualrstone.civilight.feature.script.panel.RefTab
import io.github.lenticualrstone.civilight.feature.script.panel.SpeakerKeep
import io.github.lenticualrstone.civilight.feature.script.panel.StageField
import io.github.lenticualrstone.civilight.feature.script.panel.TimeTab
import io.github.lenticualrstone.civilight.feature.script.panel.VoiceField
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.VoiceIdFormatMode
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.ScriptLayers
import io.github.lenticualrstone.civilight.model.script.ScriptPanelTab
import io.github.lenticualrstone.civilight.model.text.RecorderSource
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/**
 * The panel tabs on the example project (SCR-31 … SCR-34, SCR-23, SCR-25). The example's 1-7 `#043`
 * carries three `◆` (리엔의 마음, 하르트의 마음, 하르트의 호칭), where spec F1 has one.
 */
class PanelTabsTest {
    private val doc = Fx.example
    private val time = StoryTimes.source(doc)

    private fun ProjectDoc.mapBlock(key: Key, transform: (Block) -> Block): ProjectDoc =
        copy(episodes = episodes.mapValues { (_, file) -> file.copy(blocks = file.blocks.map { if (it.key == key) transform(it) else it }) })

    private fun ProjectDoc.withScript(transform: (io.github.lenticualrstone.civilight.model.project.ScriptSettings) -> io.github.lenticualrstone.civilight.model.project.ScriptSettings) =
        copy(project = project.copy(script = transform(project.script)))

    private fun props(d: ProjectDoc, key: Key?, readOnly: Boolean = false) = PropsModel.of(d, key, readOnly, unlocked = true, zone = Fx.zone)

    // --- SCR-31 container ---

    @Test
    fun `SCR-31 the panel shows the built tabs in order and opens at a remembered built tab`() {
        assertEquals(listOf(PanelTab.TIME, PanelTab.REF, PanelTab.PROPS, PanelTab.MODS), PanelTab.BUILT)
        assertEquals(listOf("시간", "참고", "속성", "모듈"), PanelTab.BUILT.map { it.label })
        // The swipe row and the command row keep three buttons (SCR-30).
        assertEquals(listOf("시간", "참고", "속성"), PanelTab.SWIPE.map { it.label })
        assertEquals(PanelTab.REF, PanelTab.fromStored(ScriptPanelTab.REF))
        // SCR-35: a stored 모듈 opens at 모듈.
        assertEquals(PanelTab.MODS, PanelTab.fromStored(ScriptPanelTab.MODULES))
    }

    @Test
    fun `SCR-31 without a selected block 참고 and 속성 read 고른 블록 없음 and 시간 shows the header's scene`() {
        assertEquals("고른 블록 없음", RefTab.of(doc, null).message)
        assertEquals("고른 블록 없음", props(doc, null).title)
        assertNull(props(doc, null).blockKey)
        val t = TimeTab.of(doc, null, K.SCENE_1_7_3, time)
        assertNull(t.blockTitle)
        assertEquals(K.SCENE_1_7_3, t.scene!!.sceneKey)
        assertTrue(t.inScene.none { it.selected })
    }

    // --- SCR-32 시간 ---

    @Test
    fun `SCR-32 시간 for #043 - record cards, the scene's place and time, and the scene's rows`() {
        val t = TimeTab.of(doc, K.BLOCK_043, null, time)
        assertEquals("고른 블록 · #043", t.blockTitle)
        assertEquals("고른 블록 · #043 하르트", t.blockTitleWide)
        val first = t.cards.first()
        assertEquals("◆ 관계 · 리엔의 마음 → 하르트", first.title)
        assertEquals("만나기 전 → 경계 · 1-7 #043부터", first.line)
        assertEquals(K.RIEN, first.mapFocus)
        assertFalse(first.dotted)
        assertEquals(3, t.cards.size)
        val scene = t.scene!!
        assertEquals("전환 문구 없음", scene.transitionState)
        assertEquals("1-7 · 2일째 · 새벽", scene.headline)
        assertEquals("결사의 문", scene.place)
        assertEquals("직전 씬 +1일 · 정확 · 이야기 약 13일째", scene.detail)
        val rows = t.inScene.associateBy { it.blockKey to it.left }
        assertEquals("하층 거리", rows[K.BLOCK_041 to "05:40 배경"]!!.right)
        assertEquals("변화 없음", rows[K.BLOCK_042 to "#042"]!!.right)
        val r43 = rows[K.BLOCK_043 to "◆ #043"]!!
        assertEquals("관계", r43.right)
        assertTrue(r43.selected)
        val r44 = rows[K.BLOCK_044 to "◇ #044"]!!
        assertEquals("용어 첫 공개", r44.right)
        assertEquals("용어 첫 공개 · 오리지늄 등불", r44.rightWide)
        assertEquals(1, t.inScene.count { it.selected })
    }

    @Test
    fun `SCR-32 a first reveal card and a block without records`() {
        val reveal = TimeTab.of(doc, K.BLOCK_044, null, time).cards.single()
        assertEquals("◇ 첫 공개 · 오리지늄 등불", reveal.title)
        assertEquals("이 블록 · 1-7 #044", reveal.line)
        assertNull(reveal.edit)
        assertTrue(TimeTab.of(doc, K.BLOCK_042, null, time).cards.isEmpty())
    }

    @Test
    fun `T-12-25 a point-seat record shows as a line in 이 씬 안 of the unit's first scene only, not with the marks layer off`() {
        val point = ChangeRecord(
            key = "01JZ000000CHGRTEST00000001",
            target = ChangeTarget.DisplayName(K.RIEN),
            value = JsonPrimitive("등불지기"),
            anchor = Anchor.UnitStart(K.EPISODE_1_7),
        )
        val d = doc.copy(changes = doc.changes.copy(records = doc.changes.records + point))
        val first = TimeTab.of(d, null, K.SCENE_1_7_1, StoryTimes.source(d)).inScene
        val line = first.first()
        assertEquals(InSceneRow(null, "◆ 1-7부터 · 근거 블록 없음", "", point = true, recordKey = point.key), line)
        assertTrue(TimeTab.of(d, null, K.SCENE_1_7_3, StoryTimes.source(d)).inScene.none { it.point })
        assertTrue(TimeTab.of(d, null, K.SCENE_1_7_1, StoryTimes.source(d), ScriptLayers(marks = false)).inScene.none { it.point })
    }

    // --- SCR-33 참고 ---

    @Test
    fun `T-11-01 the 관계 row reads the state just before the block, with 여기서 on the block that changes it`() {
        assertEquals(RefRow("하르트", "만나기 전"), RefTab.of(doc, K.BLOCK_042).relations.first())
        val here = RefTab.of(doc, K.BLOCK_043).relations.first()
        assertEquals("리엔", here.left)
        assertEquals("→ 만나기 전 → 보호 ◆ 여기서", here.right)
        assertEquals("← 만나기 전 → 경계 ◆ 여기서", here.sub)
        assertEquals(RefRow("하르트", "→ 경계 · ← 보호"), RefTab.of(doc, K.BLOCK_045).relations.first())
    }

    @Test
    fun `SCR-33 참고 for 하르트 - header, 말투, 호칭, 표시 이름, 최근 대사, 사이`() {
        val ref = RefTab.of(doc, K.BLOCK_047)
        assertEquals("화자 · 하르트 · 이 블록 기준", ref.title)
        assertEquals(
            listOf(RefRow("말투", "반말. 문장을 끝까지 말하지 않는다."), RefRow("리엔을", "‘꼬마’"), RefRow("표시 이름", "??? (1-9 공개)")),
            ref.rows,
        )
        assertEquals("“멈추지 마. 저건 등불이 아니야.”", ref.recent.first().text)
        assertEquals("1-7 씬 3", ref.recent.first().location)
        assertEquals(RefTab.RECENT, ref.recent.size)
        assertEquals(RefRow("세라", "사이 동료"), ref.relations[1])
        // The 호칭 set on #043 itself reads as its own change.
        assertEquals(RefRow("리엔을", "만나기 전 → ‘꼬마’ ◆ 여기서"), RefTab.of(doc, K.BLOCK_043).rows[1])
    }

    @Test
    fun `SCR-33 말투 skips the tab's own heading`() {
        assertEquals("존댓말. 낯선 사람에게도 ‘씨’를 붙인다.", RefTab.of(doc, K.BLOCK_045).rows.first().right)
    }

    @Test
    fun `Q-15 a block that is not a dialogue line has no 참고 yet`() {
        assertEquals(ScriptCopy.REF_NOT_DIALOGUE, RefTab.of(doc, K.BLOCK_044).message)
    }

    // --- SCR-34 속성, SCR-25, SCR-23 ---

    @Test
    fun `SCR-34 속성 of #043 - kind, speaker, 표시 이름 with the cover and inner notes, voice, 변화 연결`() {
        val p = props(doc, K.BLOCK_043)
        assertEquals("하르트", p.speaker!!.value)
        assertTrue(p.speaker!!.canChange)
        assertEquals("???", p.displayName!!.value)
        assertEquals("1-9에서 공개", p.displayName!!.note)
        assertEquals("1-9에서 이름 공개", p.displayName!!.noteWide)
        assertEquals(VoiceField("HRT_0107_043", "자동", editable = false), p.voice)
        assertEquals("◆ 관계 변화 · 리엔 → 하르트", p.changes.first())
        assertEquals(3, p.changes.size)
        assertNull(p.stage)
        assertNull(props(doc, K.BLOCK_042).displayName!!.note)
    }

    @Test
    fun `T-12-18 the voice field reads 자동 before the first export and 고정 with its date after`() {
        assertEquals("자동", props(doc, K.BLOCK_043).voice!!.state)
        val at = LocalDate.of(2026, 11, 20).atTime(10, 0).atZone(Fx.zone).toInstant().toEpochMilli()
        val exported = doc.mapBlock(K.BLOCK_043) { it.copy(voice = VoiceIds.exported(it.voice, "HRT_0107_043", "멈추지 마.", K.HART, at)) }
        val voice = props(exported, K.BLOCK_043).voice!!
        assertEquals(VoiceField("HRT_0107_043", "고정 · 11월 20일 내보냄", editable = false), voice)
        // A fixed ID stays when the speaker changes; the automatic one follows the speaker.
        val rien = exported.mapBlock(K.BLOCK_043) { it.copy(speakerKey = K.RIEN) }
        assertEquals("HRT_0107_043", props(rien, K.BLOCK_043).voice!!.id)
        assertEquals("RIE_0107_043", props(doc.mapBlock(K.BLOCK_043) { it.copy(speakerKey = K.RIEN) }, K.BLOCK_043).voice!!.id)
    }

    @Test
    fun `T-12-23 with the format 직접 the voice field takes typing`() {
        val manual = doc.withScript { it.copy(voiceIdFormat = it.voiceIdFormat.copy(mode = VoiceIdFormatMode.MANUAL.open())) }
        val field = props(manual, K.BLOCK_043).voice!!
        assertTrue(field.editable)
        assertNull(field.id)
        assertFalse(props(manual, K.BLOCK_043, readOnly = true).voice!!.editable)
    }

    @Test
    fun `SCR-25 무대 위치 shows only when enabled and starts unset, 보이스 ID off hides the field`() {
        val stage = doc.withScript { it.copy(stagePositionEnabled = true) }
        assertEquals(StageField(null, enabled = true), props(stage, K.BLOCK_043).stage)
        assertEquals(StageField(null, enabled = false), props(stage, K.BLOCK_043, readOnly = true).stage)
        assertNull(props(stage, K.BLOCK_044).stage)
        assertNull(props(doc.withScript { it.copy(voiceIdEnabled = false) }, K.BLOCK_043).voice)
        assertEquals(listOf("왼쪽", "가운데", "오른쪽"), StageField.OPTIONS.map { it.second })
    }

    @Test
    fun `SCR-23 a spoken choice option shows its own voice ID`() {
        val p = props(doc, K.BLOCK_1_5_CHOICE)
        assertNull(p.voice)
        val option = p.optionVoices.single()
        assertEquals("#006-1", option.number)
        assertEquals(VoiceField("RIE_0105_006_1", "자동", editable = false), option.field)
        assertEquals("보이스 ID · #006-1", ScriptCopy.optionVoiceLabel(option.number))
    }

    @Test
    fun `T-20-01 a deleted speaker's line - dotted 지운 인물, 화자 바꾸기, 되살리기, 이대로 두기, the fixed voice kept`() {
        val line = doc.manuscript.liveBlocks.first { it.speakerKey == K.NOAH }
        val p = props(doc, line.key)
        assertEquals("지운 인물 · 노아", p.speaker!!.value)
        assertTrue(p.speaker!!.dotted)
        assertFalse(p.speaker!!.canChange)
        val deleted = p.deletedSpeaker!!
        assertEquals(K.NOAH, deleted.characterKey)
        assertTrue(deleted.canRestore)
        assertTrue(deleted.canKeep)
        assertEquals("NOA", p.voice!!.id!!.take(3))
        assertFalse(props(doc, line.key, readOnly = true).deletedSpeaker!!.canRestore)

        val ctx = TrashContext(now = 1L, zone = Fx.zone, deviceId = "test", newKey = { "01JZ000000ACKNTEST00000001" })
        val kept = SpeakerKeep.keep(doc, K.NOAH, line.key, ctx)
        assertTrue(SpeakerKeep.isKept(kept, K.NOAH, line.key))
        assertFalse(props(kept, line.key).deletedSpeaker!!.canKeep)
        // Keeping one line leaves the others open; keeping twice adds nothing.
        val other = doc.manuscript.liveBlocks.last { it.speakerKey == K.NOAH }
        assertTrue(props(kept, other.key).deletedSpeaker!!.canKeep)
        assertEquals(kept.checks.acknowledgements.size, SpeakerKeep.keep(kept, K.NOAH, line.key, ctx).checks.acknowledgements.size)
    }

    @Test
    fun `SCR-59 a recorder memo shows its source, other blocks show only the fields that apply`() {
        val memo = props(doc, K.BLOCK_046)
        assertEquals("작가 메모", memo.kind!!.value)
        assertNull(memo.speaker)
        assertNull(memo.voice)
        assertNull(memo.source)
        val at = LocalDate.of(2026, 11, 22).atTime(8, 41).atZone(Fx.zone).toInstant().toEpochMilli()
        val recorded = doc.mapBlock(K.BLOCK_046) { it.copy(recorderSource = RecorderSource("01JZ000000RCRDTEST00000001", at)) }
        val source = props(recorded, K.BLOCK_046).source
        assertNotNull(source)
        assertEquals("출처", source!!.label)
        assertEquals("레코더 11월 22일 08:41", source.value)
    }

    @Test
    fun `SCR-34 표시 이름 opens the character's record, where its history is`() {
        val hart = props(Fx.example, io.github.lenticualrstone.civilight.model.sample.ExampleKeys.block(1, 7, 43))
        assertNotNull(hart.displayName)
        assertEquals(io.github.lenticualrstone.civilight.model.sample.ExampleKeys.HART, hart.displayNameOwner)
        // A narration block has no 표시 이름 field and no owner.
        val narration = props(Fx.example, io.github.lenticualrstone.civilight.model.sample.ExampleKeys.block(1, 7, 44))
        assertNull(narration.displayName)
        assertNull(narration.displayNameOwner)
    }
}
