package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.domain.script.StoryTimes
import io.github.lenticualrstone.civilight.feature.script.layers.LayerRow
import io.github.lenticualrstone.civilight.feature.script.layers.LayersModel
import io.github.lenticualrstone.civilight.feature.script.settings.ScriptFieldsModel
import io.github.lenticualrstone.civilight.feature.script.state.BlockRow
import io.github.lenticualrstone.civilight.feature.script.state.CommandRow
import io.github.lenticualrstone.civilight.feature.script.state.EditorRules
import io.github.lenticualrstone.civilight.feature.script.state.EditorState
import io.github.lenticualrstone.civilight.feature.script.state.EmptySceneRow
import io.github.lenticualrstone.civilight.feature.script.state.HeadRow
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRows
import io.github.lenticualrstone.civilight.model.project.VoiceIdFormatMode
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.ScriptLayers
import io.github.lenticualrstone.civilight.model.script.ScriptPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** SCR-26 … SCR-29 (`표시` sheet, previews, 호칭 힌트), SCR-30 command row, SCR-24/25 settings. */
class LayersAndSettingsTest {
    private val doc = Fx.example
    private val time = StoryTimes.source(doc)

    private fun rows(layers: ScriptLayers = ScriptLayers(), preview: ScriptPreview = ScriptPreview.WRITE, d: io.github.lenticualrstone.civilight.model.doc.ProjectDoc = doc) =
        ScriptRows.build(d, K.EPISODE_1_7, time, layers, preview, unlocked = false, readOnly = false, zone = Fx.zone)

    // --- SCR-26 표시 ---

    @Test
    fun `SCR-26 the 표시 rows read as the board, in order, with the spec defaults`() {
        assertEquals(
            listOf("연출 지시", "작가 메모", "시스템 텍스트", "변화 표식", "호칭 힌트", "괄호 지문", "용어 후보"),
            LayersModel.ROWS.map { it.title },
        )
        assertEquals("[기록] 같은 화면 문구 · 전환 문구", LayerRow.SYSTEM.sub)
        val defaults = ScriptLayers()
        assertEquals(listOf(true, true, true, true, false, true, false), LayersModel.ROWS.map { LayersModel.isOn(defaults, it) })
        for (row in LayersModel.ROWS) {
            val off = LayersModel.set(defaults, row, false)
            assertFalse(LayersModel.isOn(off, row))
            assertTrue(LayersModel.isOn(LayersModel.set(off, row, true), row))
        }
    }

    @Test
    fun `SCR-26 a layer only changes what is drawn - memo and direction rows go, the scene head stays`() {
        val all = rows()
        val noMemo = rows(ScriptLayers(memo = false, direction = false))
        assertTrue(all.any { it is BlockRow && it.blockKey == K.BLOCK_046 })
        assertTrue(noMemo.none { it is BlockRow && (it.blockKey == K.BLOCK_046 || it.blockKey == K.BLOCK_041) })
        assertEquals(all.filterIsInstance<HeadRow>().size, noMemo.filterIsInstance<HeadRow>().size)
    }

    @Test
    fun `SCR-26 a hidden layer takes the selection and the caret off its block`() {
        val editing = EditorRules.edit(EditorState(), K.BLOCK_046)
        val hidden = rows(ScriptLayers(memo = false)).mapTo(HashSet()) { it.key }
        val after = EditorRules.dropHidden(editing, doc, hidden)!!
        assertNull(after.selected)
        assertFalse(after.editing)
        val shown = rows().mapTo(HashSet()) { it.key }
        assertNull(EditorRules.dropHidden(editing, doc, shown))
    }

    @Test
    fun `SCR-29 호칭 힌트 is off by default and shows the speaker's address for the scene's others`() {
        val off = rows().filterIsInstance<BlockRow>().single { it.blockKey == K.BLOCK_043 }
        assertTrue(off.addressHints.isEmpty())
        val on = rows(ScriptLayers(addressHint = true)).filterIsInstance<BlockRow>()
        assertEquals(listOf("리엔을 ‘꼬마’"), on.single { it.blockKey == K.BLOCK_043 }.addressHints)
        assertTrue(on.single { it.blockKey == K.BLOCK_044 }.addressHints.isEmpty())
    }

    // --- SCR-27 / SCR-28 미리 보기 ---

    @Test
    fun `SCR-27 대사만 keeps dialogue blocks and scene heads only, with no empty-scene line, and is read-only`() {
        val dialogue = rows(preview = ScriptPreview.DIALOGUE_ONLY)
        assertTrue(dialogue.filterIsInstance<BlockRow>().all { it.kind == BlockKind.DIALOGUE })
        assertTrue(dialogue.none { it is EmptySceneRow })
        assertTrue(dialogue.any { it is BlockRow && it.blockKey == K.BLOCK_043 })
        assertTrue(dialogue.none { it is BlockRow && it.blockKey == K.BLOCK_044 })
        assertFalse(LayersModel.isEditable(ScriptPreview.DIALOGUE_ONLY))
        assertTrue(LayersModel.isEditable(ScriptPreview.WRITE))
        assertEquals("미리 보기 · 대사만", LayersModel.headerChip(ScriptPreview.DIALOGUE_ONLY))
        assertNull(LayersModel.headerChip(ScriptPreview.WRITE))
    }

    @Test
    fun `SCR-28 낭독 is shown disabled and a stored 낭독 draws as 쓰기`() {
        assertEquals(listOf("쓰기", "대사만", "낭독"), LayersModel.CHIPS.map { it.label })
        assertEquals(listOf(true, true, false), LayersModel.CHIPS.map { it.enabled })
        assertEquals(ScriptPreview.WRITE, LayersModel.effective(ScriptPreview.READ_ALOUD))
        assertEquals(ScriptPreview.WRITE, LayersModel.selectedChip(ScriptPreview.READ_ALOUD))
        assertTrue(LayersModel.isEditable(ScriptPreview.READ_ALOUD))
    }

    // --- SCR-30 command row (tap path to the swipe buttons, Q-02) ---

    @Test
    fun `SCR-30 a selected block outside editing gets the panel buttons wherever no side panel is shown`() {
        val selected = EditorRules.select(EditorState(), K.BLOCK_043)
        val cover = CommandRow.of(selected, readOnly = false, previewOnly = false, panelShown = false, wide = false)
        assertTrue(cover.panelTabs)
        assertTrue(cover.speaker)
        assertTrue(cover.lineBreak)
        assertFalse(cover.layers) // the cover header has its own layers button
        val innerOpen = CommandRow.of(selected, readOnly = false, previewOnly = false, panelShown = true, wide = true)
        assertFalse(innerOpen.panelTabs)
        assertTrue(innerOpen.layers)
        val innerClosed = CommandRow.of(selected, readOnly = false, previewOnly = false, panelShown = false, wide = true)
        assertTrue(innerClosed.panelTabs)
        assertTrue(innerClosed.layers)
        // Read-only (the example): no writing controls, but the panel buttons stay.
        val readOnly = CommandRow.of(selected, readOnly = true, previewOnly = false, panelShown = false, wide = false)
        assertEquals(CommandRow(speaker = false, layers = false, panelTabs = true, lineBreak = false), readOnly)
        // Typing hides the row (the keyboard toolbar takes its place); nothing selected shows nothing.
        assertFalse(CommandRow.of(EditorRules.edit(EditorState(), K.BLOCK_043), false, false, false, false).visible)
        assertFalse(CommandRow.of(EditorState(), false, false, false, false).visible)
        assertFalse(CommandRow.of(selected, readOnly = false, previewOnly = true, panelShown = false, wide = false).visible)
    }

    // --- SCR-24 / SCR-25 스크립트 추가 칸 ---

    @Test
    fun `SCR-24 and SCR-25 settings rows, the sample ID and the fixed-ID notice`() {
        val ui = ScriptFieldsModel.of(doc, readOnly = false)
        assertFalse(ui.stageOn)
        assertTrue(ui.voiceOn)
        val sample = ScriptFieldsModel.sampleId(doc)
        assertEquals("$sample · 인물 · 에피소드 · 블록 · 내보내면 고정", ui.formatSub)
        assertTrue(Regex("[A-Z]{3}_\\d{4}_\\d{3}").matches(sample))
        assertEquals(listOf(true, false), ui.choices.map { it.selected })
        assertEquals(listOf("고정 ID ${ui.fixedCount}개는 그대로", "고정 ID도 새 형식으로"), ui.notice)
        assertTrue(ui.fixedCount > 0)
        assertTrue(ui.canRefix)
        assertFalse(ScriptFieldsModel.of(doc, readOnly = true).canRefix)
    }

    @Test
    fun `SCR-24 switching to 직접 keeps the pattern and cannot re-fix, switches write only the setting`() {
        val manual = ScriptFieldsModel.withMode(doc, VoiceIdFormatMode.MANUAL)
        val ui = ScriptFieldsModel.of(manual, readOnly = false)
        assertEquals("직접 · 내보내면 고정", ui.formatSub)
        assertFalse(ui.canRefix)
        assertEquals(doc.project.script.voiceIdFormat.pattern, manual.project.script.voiceIdFormat.pattern)
        assertEquals(manual, ScriptFieldsModel.refix(manual, Fx.ctx()))
        val back = ScriptFieldsModel.withMode(manual, VoiceIdFormatMode.PATTERN)
        assertEquals(doc.project.script, back.project.script)
        assertTrue(ScriptFieldsModel.withStage(doc, true).project.script.stagePositionEnabled)
        val voiceOff = ScriptFieldsModel.withVoice(doc, false)
        assertFalse(voiceOff.project.script.voiceIdEnabled)
        assertEquals(doc.episodes, voiceOff.episodes) // fixed and discarded IDs stay in the data
    }

    @Test
    fun `SCR-24 고정 ID도 새 형식으로 re-fixes in the current pattern and records the old strings as 버린 ID`() {
        val changed = doc.copy(
            project = doc.project.copy(
                script = doc.project.script.copy(voiceIdFormat = doc.project.script.voiceIdFormat.copy(pattern = "{code}-{chapter2}{episode2}-{number3}")),
            ),
        )
        val before = io.github.lenticualrstone.civilight.domain.script.VoiceIds.discarded(changed).size
        val after = ScriptFieldsModel.refix(changed, Fx.ctx())
        val fixed = after.manuscript.liveBlocks.mapNotNull { it.voice?.fixedId }
        assertTrue(fixed.isNotEmpty())
        assertTrue(fixed.all { it.contains('-') })
        assertEquals(before + fixed.size, io.github.lenticualrstone.civilight.domain.script.VoiceIds.discarded(after).size)
        assertNotNull(after.project.script.voiceIdFormat)
    }
}
