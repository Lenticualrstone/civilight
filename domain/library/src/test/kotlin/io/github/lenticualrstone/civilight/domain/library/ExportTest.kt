package io.github.lenticualrstone.civilight.domain.library

import io.github.lenticualrstone.civilight.domain.library.common.UnitCodes
import io.github.lenticualrstone.civilight.domain.library.export.CharacterProfile
import io.github.lenticualrstone.civilight.domain.library.export.CsvRowPlan
import io.github.lenticualrstone.civilight.domain.library.export.CsvRowState
import io.github.lenticualrstone.civilight.domain.library.export.CsvRows
import io.github.lenticualrstone.civilight.domain.library.export.ExportDefaults
import io.github.lenticualrstone.civilight.domain.library.export.ExportGate
import io.github.lenticualrstone.civilight.domain.library.export.ExportPlanner
import io.github.lenticualrstone.civilight.domain.library.export.ExportRow
import io.github.lenticualrstone.civilight.domain.library.export.ExportSheets
import io.github.lenticualrstone.civilight.domain.library.export.ExportStart
import io.github.lenticualrstone.civilight.domain.library.export.ExportWarning
import io.github.lenticualrstone.civilight.domain.library.export.HiddenSceneChoice
import io.github.lenticualrstone.civilight.domain.library.export.ProjectFileExport
import io.github.lenticualrstone.civilight.domain.library.export.VoiceFixation
import io.github.lenticualrstone.civilight.domain.library.export.WrittenLine
import io.github.lenticualrstone.civilight.domain.library.format.BlobReader
import io.github.lenticualrstone.civilight.domain.library.format.ProjectFileOptions
import io.github.lenticualrstone.civilight.domain.library.format.ProjectPackageWriter
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.app.ExportFormat
import io.github.lenticualrstone.civilight.model.app.ExportOptions
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.file.PackagePaths
import io.github.lenticualrstone.civilight.model.file.RecorderFile
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.ProjectFormat
import io.github.lenticualrstone.civilight.model.project.Protections
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.recorder.RecorderAudio
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.ChoiceOption
import io.github.lenticualrstone.civilight.model.script.DiscardReason
import io.github.lenticualrstone.civilight.model.script.DiscardedVoiceId
import io.github.lenticualrstone.civilight.model.script.VoiceMode
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.plainText
import io.github.lenticualrstone.civilight.domain.library.format.ZipCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** §4.5 export decisions (E-01 … E-20), LIB-41…47. */
class ExportTest {
    private val base = Fixtures.example()
    private val hiddenS4 = Fixtures.withSceneHidden(base, ExampleKeys.SCENE_1_7_4)

    private fun options(doc: ProjectDoc, format: ExportFormat) = ExportDefaults.initial(doc, format, Fixtures.NOW, Fixtures.ZONE)

    /** The automatic voice ID as the script area makes it (`{code}_{chapter2}{episode2}_{number3}`). */
    private fun voiceIdOf(doc: ProjectDoc): (WrittenLine) -> String? = { line ->
        val speaker = line.speakerKey?.let { doc.index.character(it) }
        val block = doc.index.block(line.blockKey)
        val scene = doc.index.unit(line.sceneKey)
        if (speaker?.voiceCode == null || block == null || scene == null || line.optionKey != null) {
            null
        } else {
            val episode = doc.index.unit(scene.parentKey!!)!!
            val codes = UnitCodes(doc)
            val chapter = codes.ordinalIncludingTrashed(doc.index.unit(episode.parentKey!!)!!)
            val e = codes.ordinalIncludingTrashed(episode)
            "${speaker.voiceCode}_${chapter.toString().padStart(2, '0')}${e.toString().padStart(2, '0')}_${block.number.toString().padStart(3, '0')}"
        }
    }

    @Test
    fun `E-01 hidden scene defaults per format`() {
        assertEquals(HiddenSceneChoice.EXCLUDE, ExportDefaults.hiddenSceneDefault(ExportFormat.PDF))
        assertEquals(HiddenSceneChoice.INCLUDE, ExportDefaults.hiddenSceneDefault(ExportFormat.CSV))
        assertEquals(HiddenSceneChoice.INCLUDE, ExportDefaults.hiddenSceneDefault(ExportFormat.ENGINE_JSON))
        assertNull(ExportDefaults.hiddenSceneDefault(ExportFormat.PROJECT_FILE))
    }

    @Test
    fun `E-02 JSON sheet lists the hidden scene with 넣기`() {
        val o = options(hiddenS4, ExportFormat.ENGINE_JSON)
        assertEquals(listOf(ExampleKeys.CHAPTER_1), o.rangeKeys)
        val sheet = ExportSheets.build(hiddenS4, o)
        assertEquals("1장 전체", sheet.row<ExportRow.Range>()!!.value)
        val row = sheet.row<ExportRow.HiddenScenes>()!!
        assertEquals("숨긴 씬 1 · 1-7 씬 4", row.label)
        assertEquals(HiddenSceneChoice.INCLUDE, row.choice)
        assertEquals(listOf("넣기", "빼기"), row.options)
    }

    @Test
    fun `E-03 넣기 asks once and cancel writes nothing`() {
        val o = options(hiddenS4, ExportFormat.ENGINE_JSON)
        assertEquals(ExportGate.NeedsAuth, ExportGate.of(hiddenS4, o))
        assertEquals(ExportStart.Cancelled, ExportStart.of(hiddenS4, o, authenticated = false))
        val ok = ExportStart.of(hiddenS4, o, authenticated = true) as ExportStart.Write
        assertTrue(ok.plan.scenes.any { it.sceneKey == ExampleKeys.SCENE_1_7_4 })
    }

    @Test
    fun `E-04 빼기 needs no authentication and leaves the scene out`() {
        val o = options(hiddenS4, ExportFormat.ENGINE_JSON).copy(includeHiddenScenes = false)
        assertEquals(ExportGate.Open, ExportGate.of(hiddenS4, o))
        val plan = (ExportStart.of(hiddenS4, o, authenticated = false) as ExportStart.Write).plan
        assertFalse(plan.scenes.any { it.sceneKey == ExampleKeys.SCENE_1_7_4 })
        assertEquals(listOf(ExampleKeys.SCENE_1_7_4), plan.leftOutScenes)
    }

    @Test
    fun `E-05 hidden 노아 is exported with name only`() {
        val doc = Fixtures.withScene165Live(Fixtures.withNoahHidden())
        val plan = ExportPlanner.plan(doc, options(doc, ExportFormat.ENGINE_JSON).copy(rangeKeys = listOf(ExampleKeys.SCENE_1_6_5)))
        val noahLines = plan.writtenLines.filter { it.speakerKey == ExampleKeys.NOAH }
        assertTrue(noahLines.isNotEmpty())
        val noah = plan.characters.single { it.key == ExampleKeys.NOAH }
        assertEquals("노아", noah.name)
        assertEquals(CharacterProfile.NAME_ONLY, noah.profile)
        val sheet = ExportSheets.build(doc, options(doc, ExportFormat.ENGINE_JSON).copy(rangeKeys = listOf(ExampleKeys.SCENE_1_6_5)))
        assertEquals("숨긴 인물 1 · 노아", sheet.row<ExportRow.HiddenSpeakers>()!!.label)
        assertEquals("화자 이름만 들어감", sheet.row<ExportRow.HiddenSpeakers>()!!.info)
    }

    @Test
    fun `E-06 a choice leading into a left-out scene raises a warning`() {
        val next = Fixtures.keys(40)
        val last = hiddenS4.index.blocksOfScene(ExampleKeys.SCENE_1_7_3).maxOf { it.order }
        val choice = Block(
            next(), ExampleKeys.SCENE_1_7_3, OrderKeys.between(last, null), 58, BlockKind.CHOICE.open(),
            options = listOf(
                ChoiceOption(next(), OrderKeys.between(null, null), listOf(TextRun.Plain("문을 연다"))),
                ChoiceOption(next(), OrderKeys.between("V", null), listOf(TextRun.Plain("돌아선다"))),
            ),
        )
        val inHidden = Block(next(), ExampleKeys.SCENE_1_7_4, OrderKeys.between(null, null), 59, BlockKind.NARRATION.open(), runs = listOf(TextRun.Plain("문 너머")))
        val doc = Fixtures.withBlocks(hiddenS4, listOf(choice, inHidden))
        val o = options(doc, ExportFormat.ENGINE_JSON).copy(includeHiddenScenes = false)
        val warning = ExportPlanner.plan(doc, o).warnings.single() as ExportWarning.ChoiceIntoLeftOutScene
        assertEquals("▲ 숨긴 씬으로 가는 선택지 1", warning.text)
        assertEquals(listOf(warning), ExportSheets.build(doc, o).warnings)
        assertTrue(ExportPlanner.plan(doc, o.copy(includeHiddenScenes = true)).warnings.isEmpty())
    }

    @Test
    fun `E-07 CSV 빼기 of a scene with fixed IDs writes no 버림 rows for it`() {
        val doc = Fixtures.withSceneHidden(base, ExampleKeys.SCENE_1_1_1)
        val fixedInScene = doc.manuscript.liveBlocksOf(ExampleKeys.SCENE_1_1_1).mapNotNull { it.voice?.fixedId }
        assertTrue(fixedInScene.isNotEmpty())
        val plan = ExportPlanner.plan(doc, options(doc, ExportFormat.CSV).copy(includeHiddenScenes = false))
        val rows = CsvRows.of(plan, voiceIdOf(doc))
        val ids = rows.mapNotNull { (it as? CsvRowPlan.Line)?.voiceId } + rows.mapNotNull { (it as? CsvRowPlan.Discarded)?.discarded?.id }
        assertTrue(fixedInScene.none { it in ids })
        assertTrue(rows.filterIsInstance<CsvRowPlan.Discarded>().none { it.discarded.id in fixedInScene })
        // The purged 1-2 #007 is still a 버림 row.
        assertTrue(rows.filterIsInstance<CsvRowPlan.Discarded>().any { it.discarded.id == "RIE_0102_007" })
    }

    @Test
    fun `E-08 encryption default`() {
        assertFalse(ExportDefaults.encryptDefault(base))
        assertTrue(ExportDefaults.encryptDefault(Fixtures.withNoahHidden()))
        val oath = Fixtures.projectWith("검은 서약 (가제)", Rating.R18, Protections(hidden = true, captureBlock = true, nameMask = true, recorderLinkMask = true), ProjectFormat.SETTING_ONLY)
        assertTrue(ExportDefaults.encryptDefault(oath))
        assertTrue(ExportDefaults.encryptDefault(Fixtures.withProtections(base, Rating.R18, Protections(captureBlock = true))))
        assertTrue(options(Fixtures.withNoahHidden(), ExportFormat.PROJECT_FILE).encrypt)
    }

    @Test
    fun `E-09 default file names`() {
        assertEquals("잿빛 새벽의 등불", ExportDefaults.defaultFileName(base, Fixtures.NOW, Fixtures.ZONE))
        val oath = Fixtures.projectWith("검은 서약 (가제)", Rating.R18, Protections(hidden = true))
        assertEquals("civilight-내보내기-1122", ExportDefaults.defaultFileName(oath, Fixtures.NOW, Fixtures.ZONE))
        val r18 = Fixtures.withProtections(base, Rating.R18, Protections())
        assertEquals("civilight-내보내기-1122", ExportDefaults.defaultFileName(r18, Fixtures.NOW, Fixtures.ZONE))
    }

    @Test
    fun `E-10 PDF of 검은 서약 has the PDF password row on`() {
        val oath = Fixtures.projectWith("검은 서약 (가제)", Rating.R18, Protections(hidden = true, captureBlock = true, nameMask = true, recorderLinkMask = true))
        assertEquals(ExportRow.PdfPassword(on = true), ExportSheets.build(oath, options(oath, ExportFormat.PDF)).row<ExportRow.PdfPassword>())
        assertNull(ExportSheets.build(base, options(base, ExportFormat.PDF)).row<ExportRow.PdfPassword>())
        assertEquals("PDF 만들기", ExportSheets.build(base, options(base, ExportFormat.PDF)).primaryButton)
    }

    /** The fixture's four linked records, R2 hidden and R1 with kept audio. */
    private fun recorderWithHiddenR2(): RecorderLibrary {
        val lib = Fixtures.recorder()
        return lib.copy(
            records = lib.records.map {
                when (it.key) {
                    ExampleKeys.RECORDER_R2 -> it.copy(hidden = true)
                    ExampleKeys.RECORDER_R1 -> it.copy(audio = RecorderAudio("a".repeat(64), "audio/mp4", 1000, 24_000))
                    else -> it
                }
            },
        )
    }

    @Test
    fun `E-11 project file leaves the hidden record and audio out`() {
        val recorder = recorderWithHiddenR2()
        val o = options(base, ExportFormat.PROJECT_FILE)
        val row = ExportSheets.build(base, o, recorder).row<ExportRow.RecorderRecords>()!!
        assertEquals("레코더 기록 4 · 원본 음성 포함 · 숨긴 기록 1 제외", row.text)
        val content = ProjectPackageWriter.content(base, recorder, options = ProjectFileOptions(), writtenBy = "t", writtenAt = Fixtures.NOW)
        assertEquals(3, content.recorderRecordKeys.size)
        assertFalse(ExampleKeys.RECORDER_R2 in content.recorderRecordKeys)
        val file = CivilightJson.decode(RecorderFile.serializer(), content.files.getValue(PackagePaths.RECORDER).toString(Charsets.UTF_8))
        assertTrue(file.records.all { it.audio == null })
        assertTrue(content.files.keys.none { it.startsWith("assets/") && it != PackagePaths.ASSETS_INDEX })
    }

    @Test
    fun `E-12 hidden encrypted project includes its hidden record`() {
        val hidden = Fixtures.withProtections(base, Rating.R18, Protections(hidden = true, captureBlock = true, nameMask = true, recorderLinkMask = true))
        val content = ProjectPackageWriter.content(hidden, recorderWithHiddenR2(), options = ProjectFileOptions(encrypted = true), writtenBy = "t", writtenAt = Fixtures.NOW)
        assertTrue(ExampleKeys.RECORDER_R2 in content.recorderRecordKeys)
        val notEncrypted = ProjectPackageWriter.content(hidden, recorderWithHiddenR2(), options = ProjectFileOptions(encrypted = false), writtenBy = "t", writtenAt = Fixtures.NOW)
        assertFalse(ExampleKeys.RECORDER_R2 in notEncrypted.recorderRecordKeys)
    }

    @Test
    fun `E-13 reading and engine outputs carry no recorder records`() {
        for (format in listOf(ExportFormat.PDF, ExportFormat.CSV, ExportFormat.ENGINE_JSON)) {
            assertTrue(ExportPlanner.plan(base, options(base, format)).recorderRecordKeys.isEmpty())
        }
    }

    private val at1120 = Fixtures.at("2026-11-20T03:00:00Z")

    private fun firstCsv(): Pair<ProjectDoc, List<CsvRowPlan>> {
        val plan = ExportPlanner.plan(base, options(base, ExportFormat.CSV))
        val rows = CsvRows.of(plan, voiceIdOf(base))
        return VoiceFixation.apply(base, plan, at1120, voiceIdOf(base)) to rows
    }

    @Test
    fun `E-14 first CSV export fixes HRT_0107_043 as 새`() {
        assertNull(base.index.block(ExampleKeys.BLOCK_043)!!.voice)
        val (after, rows) = firstCsv()
        val row = rows.filterIsInstance<CsvRowPlan.Line>().single { it.line.blockKey == ExampleKeys.BLOCK_043 }
        assertEquals("HRT_0107_043", row.voiceId)
        assertEquals(CsvRowState.NEW, row.state)
        assertEquals("새", row.state!!.label)
        val voice = after.index.block(ExampleKeys.BLOCK_043)!!.voice!!
        assertEquals(VoiceMode.FIXED.open(), voice.mode)
        assertEquals("HRT_0107_043", voice.fixedId)
        assertEquals(java.time.LocalDate.of(2026, 11, 20), java.time.Instant.ofEpochMilli(voice.fixedAt!!).atZone(Fixtures.ZONE).toLocalDate())
        // 1-1 lines already exported and unchanged have no state.
        assertTrue(rows.filterIsInstance<CsvRowPlan.Line>().filter { it.line.voice?.lastExport != null }.all { it.state == null })
    }

    @Test
    fun `E-15 a speaker change after export is 바뀜 and keeps the ID`() {
        val (after, _) = firstCsv()
        val changed = after.copy(
            episodes = after.episodes.mapValues { (_, f) -> f.copy(blocks = f.blocks.map { if (it.key == ExampleKeys.BLOCK_043) it.copy(speakerKey = ExampleKeys.RIEN) else it }) },
        )
        val rows = CsvRows.of(ExportPlanner.plan(changed, options(changed, ExportFormat.CSV)), voiceIdOf(changed))
        val row = rows.filterIsInstance<CsvRowPlan.Line>().single { it.line.blockKey == ExampleKeys.BLOCK_043 }
        assertEquals(CsvRowState.CHANGED, row.state)
        assertEquals("HRT_0107_043", row.voiceId)
    }

    @Test
    fun `E-16 a deleted exported line is a 버림 row`() {
        val (after, _) = firstCsv()
        val unit = ExampleKeys.EPISODE_1_7
        val file = after.episodes.getValue(unit)
        val deleted = after.copy(
            episodes = after.episodes + (
                unit to file.copy(
                    blocks = file.blocks.map { if (it.key == ExampleKeys.BLOCK_043) it.copy(deletedAt = Fixtures.NOW, trashEntryKey = Fixtures.keys(41)()) else it },
                    discardedVoiceIds = file.discardedVoiceIds + DiscardedVoiceId("HRT_0107_043", ExampleKeys.BLOCK_043, discardedAt = Fixtures.NOW, reason = DiscardReason.DELETED.open()),
                )
                ),
        )
        val rows = CsvRows.of(ExportPlanner.plan(deleted, options(deleted, ExportFormat.CSV)), voiceIdOf(deleted))
        val row = rows.filterIsInstance<CsvRowPlan.Discarded>().single { it.discarded.id == "HRT_0107_043" }
        assertEquals(CsvRowState.DISCARDED, row.state)
        assertEquals("버림", row.state.label)
        assertTrue(rows.filterIsInstance<CsvRowPlan.Line>().none { it.line.blockKey == ExampleKeys.BLOCK_043 })
    }

    @Test
    fun `E-17 PDF and project file exports change no voice state`() {
        val pdf = ExportPlanner.plan(base, options(base, ExportFormat.PDF))
        assertFalse(pdf.fixesVoiceIds)
        assertEquals(base, VoiceFixation.apply(base, pdf, at1120, voiceIdOf(base)))
        val file = ProjectFileExport.export(
            base, options(base, ExportFormat.PROJECT_FILE), null, RecorderLibrary(), BlobReader.NONE, "t", Fixtures.NOW,
        )
        val read = Fixtures.open(file.bytes).doc
        assertEquals(base.allBlocks.filter { base.liveness.isLive(it) }.associate { it.key to it.voice }, read.allBlocks.associate { it.key to it.voice })
        assertEquals("잿빛 새벽의 등불.civilight", file.fileName)
    }

    @Test
    fun `E-18 the 1-7 씬 4 transition phrase is one CSV row`() {
        val rows = CsvRows.of(ExportPlanner.plan(base, options(base, ExportFormat.CSV)), voiceIdOf(base))
        val t = rows.filterIsInstance<CsvRowPlan.Transition>().single { it.sceneKey == ExampleKeys.SCENE_1_7_4 }
        assertEquals("전환 문구", t.speakerCell)
        assertEquals("결사의 문 앞 · 06:10", t.runs!!.plainText)
        assertEquals(TransitionMode.DIRECT, t.mode)
        assertNull(t.state)
        // The row comes before the scene's lines and after the previous scene's.
        val idx = rows.indexOf(t)
        assertTrue((rows[idx - 1] as CsvRowPlan.Line).line.sceneKey == ExampleKeys.SCENE_1_7_3)
        assertEquals(listOf("화자", "대사", "보이스 ID", "블록 ID", "상태"), CsvRowPlan.HEADER)
    }

    @Test
    fun `E-19 engine scene of 1-7 씬 4`() {
        val plan = ExportPlanner.plan(base, options(base, ExportFormat.ENGINE_JSON))
        val s = plan.scenes.single { it.sceneKey == ExampleKeys.SCENE_1_7_4 }
        assertEquals(ExampleKeys.PLACE_SOCIETY_GATE, s.place!!.key)
        assertEquals("결사의 문", base.index.place(s.place!!.key)!!.name)
        assertFalse(s.flashback)
        assertEquals(TransitionMode.DIRECT, s.transition!!.mode)
        assertEquals("결사의 문 앞 · 06:10", s.transition!!.runs!!.plainText)
        assertTrue(s.blocks.isEmpty())
        val time = s.time as SceneTime.Relative
        assertEquals("06:10", time.clock.toString())
    }

    @Test
    fun `E-20 secret fields stay out unless asked`() {
        val doc = base.copy(project = base.project.copy(fieldDefs = base.project.fieldDefs.map { if (it.key == ExampleKeys.FIELD_PERSONALITY) it.copy(isSecret = true) else it }))
        assertEquals("height", doc.index.fieldDef(ExampleKeys.FIELD_HEIGHT)!!.exportName)
        val plan = ExportPlanner.plan(doc, options(doc, ExportFormat.ENGINE_JSON))
        assertTrue(ExampleKeys.FIELD_HEIGHT in plan.fieldDefKeys)
        assertFalse(ExampleKeys.FIELD_PERSONALITY in plan.fieldDefKeys)
        assertTrue(ExampleKeys.FIELD_PERSONALITY in ExportPlanner.plan(doc, options(doc, ExportFormat.ENGINE_JSON).copy(includeSecretFields = true)).fieldDefKeys)
        assertEquals(ExportRow.SecretFields(on = false), ExportSheets.build(doc, options(doc, ExportFormat.ENGINE_JSON)).row<ExportRow.SecretFields>())
    }

    @Test
    fun `encrypted project file export opens with its password`() {
        val o = options(Fixtures.withNoahHidden(), ExportFormat.PROJECT_FILE)
        assertTrue(o.encrypt)
        val file = ProjectFileExport.export(
            Fixtures.withNoahHidden(), o, "열쇠 한 개".toCharArray(), RecorderLibrary(), BlobReader.NONE, "t", Fixtures.NOW, iterations = 1000,
        )
        assertTrue(file.encrypted)
        assertEquals("civilight-내보내기-1122.civilight".takeIf { false } ?: "잿빛 새벽의 등불.civilight", file.fileName)
        assertEquals(listOf(PackagePaths.MIMETYPE, PackagePaths.ENCRYPTION, PackagePaths.PAYLOAD), ZipCodec.read(file.bytes).paths)
    }

    @Test
    fun `layers row and range label`() {
        val pdf = ExportSheets.build(base, options(base, ExportFormat.PDF))
        assertEquals("연출 · 괄호 지문 포함", pdf.row<ExportRow.Layers>()!!.value)
        assertEquals("에피소드 1-7", ExportSheets.rangeLabel(base, listOf(ExampleKeys.EPISODE_1_7)))
        assertNotNull(pdf.row<ExportRow.FileName>())
    }
}
