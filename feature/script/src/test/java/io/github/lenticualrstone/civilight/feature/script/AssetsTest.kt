package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.domain.script.ScriptCommands
import io.github.lenticualrstone.civilight.feature.script.assets.AssetFilter
import io.github.lenticualrstone.civilight.feature.script.assets.AssetForm
import io.github.lenticualrstone.civilight.feature.script.assets.AssetFormDraft
import io.github.lenticualrstone.civilight.feature.script.assets.AssetFormSheetKey
import io.github.lenticualrstone.civilight.feature.script.assets.AssetThumb
import io.github.lenticualrstone.civilight.feature.script.assets.AssetsCopy
import io.github.lenticualrstone.civilight.feature.script.assets.AssetsModel
import io.github.lenticualrstone.civilight.feature.script.state.CommandAction
import io.github.lenticualrstone.civilight.feature.script.state.CommandRouting
import io.github.lenticualrstone.civilight.feature.script.state.CommandSuggestions
import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.script.CueKind
import io.github.lenticualrstone.civilight.model.script.ScriptAssetKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** SCR-61 `에셋` list, picker and form; SCR-68 `에셋` command (example project). */
class AssetsTest {
    private val doc = Fx.example
    private fun ui(filter: AssetFilter = AssetFilter.ALL, query: String = "", readOnly: Boolean = false) =
        AssetsModel.of(doc, filter, query, K.EPISODE_1_7, readOnly)

    @Test
    fun `SCR-61 sections per kind with the project's counts, rows in the list's order`() {
        val all = ui()
        assertEquals(listOf("배경 · 3", "BGM · 2", "효과음 · 1", "CG · 1"), all.sections.map { it.title })
        assertEquals(listOf("하층 거리(새벽)", "결사의 문(아침)", "잿더미 시장(밤)"), all.sections[0].rows.map { it.name })
        assertEquals("잿빛 새벽의 등불", all.projectName)
        assertEquals(7, all.total)
        assertFalse(all.nothingFound)
        assertFalse(all.noAssets)
    }

    @Test
    fun `SCR-61 rows - sublines, first episode, the empty CG and thumbnails`() {
        val rows = ui().sections.flatMap { it.rows }.associateBy { it.key }
        val embers = rows.getValue(K.ASSET_BGM_EMBERS)
        assertEquals("bgm_embers · 2분 34초 · 반복", embers.subline)
        assertEquals(AssetThumb.Audio, embers.thumb)
        val cg = rows.getValue(K.ASSET_CG_FLICKER)
        assertEquals("cg_flicker · 아직 이미지 없음", cg.subline)
        assertEquals("비어 있음", cg.trailing)
        assertTrue(cg.trailingWarn)
        assertEquals(AssetThumb.Image(null), cg.thumb)
        // Used in the episode being written (1-7 #041): `이 에피소드 n회`; used elsewhere only: the total.
        val street = rows.getValue(K.ASSET_BG_LOWER_STREET_DAWN)
        assertTrue(street.subline, street.subline.startsWith("bg_lower_street_dawn · 이 에피소드 "))
        val gate = rows.getValue(K.ASSET_BG_SOCIETY_GATE_MORNING)
        assertEquals("bg_society_gate_morning · 1회", gate.subline)
        assertFalse(gate.trailingWarn)
        assertTrue(gate.trailing!!.matches(Regex("""\d+-\d+""")))
        assertNull(rows.getValue(K.ASSET_BGM_UPPER_BELL).trailing)
    }

    @Test
    fun `SCR-61 chips - one kind, or 안 쓰는 것 = no cue references it`() {
        assertEquals(listOf("전체", "배경", "BGM", "효과음", "CG", "안 쓰는 것"), AssetFilter.entries.map { it.label })
        val bgm = ui(AssetFilter.BGM)
        assertEquals(listOf("BGM · 2"), bgm.sections.map { it.title })
        assertEquals(2, bgm.sections.single().rows.size)
        val unused = ui(AssetFilter.UNUSED)
        assertEquals(listOf(K.ASSET_BGM_UPPER_BELL), unused.sections.flatMap { it.rows }.map { it.key })
        // The section keeps the project's count of the kind, not the filtered count.
        assertEquals("BGM · 2", unused.sections.single().title)
    }

    @Test
    fun `SCR-61 search matches the name or the file name, 초성 too`() {
        assertEquals(listOf(K.ASSET_BGM_EMBERS), ui(query = "잔불").sections.flatMap { it.rows }.map { it.key })
        assertEquals(listOf(K.ASSET_BG_ASH_MARKET_NIGHT), ui(query = "bg_ash").sections.flatMap { it.rows }.map { it.key })
        assertTrue(K.ASSET_SFX_WIND in ui(query = "ㅂㄹ").sections.flatMap { it.rows }.map { it.key })
        val none = ui(query = "없는 에셋")
        assertTrue(none.nothingFound)
        assertTrue(none.sections.isEmpty())
    }

    @Test
    fun `SCR-08 cue picker = the list filtered to the cue's kind, sublines for the block's episode`() {
        val list = ui(AssetFilter.BACKGROUND).sections.single().rows
        val picker = AssetsModel.pickerRows(doc, CueKind.BACKGROUND, K.EPISODE_1_7)
        assertEquals(list, picker)
        assertEquals(listOf(K.ASSET_BGM_EMBERS, K.ASSET_BGM_UPPER_BELL), AssetsModel.pickerRows(doc, CueKind.BGM, null).map { it.key })
        assertTrue(AssetsModel.pickerRows(doc, CueKind.SCREEN_EFFECT, null).isEmpty())
        assertEquals(ScriptAssetKind.CG, AssetForm.kindOf(CueKind.CG))
        assertNull(AssetForm.kindOf(CueKind.SCREEN_EFFECT))
    }

    @Test
    fun `form - typed length, kinds while adding, kind fixed on an existing asset`() {
        assertEquals(161_000L, AssetForm.parseLength("161"))
        assertEquals(800L, AssetForm.parseLength("0,8"))
        assertEquals(161_000L, AssetForm.parseLength("2:41"))
        assertNull(AssetForm.parseLength("2:61"))
        assertNull(AssetForm.parseLength(""))
        assertNull(AssetForm.parseLength("-3"))
        assertNull(AssetForm.parseLength("abc"))
        assertEquals("161", AssetForm.lengthText(161_000))
        assertEquals("4.2", AssetForm.lengthText(4_200))
        assertEquals("0.8", AssetForm.lengthText(800))

        val addKey = AssetFormSheetKey(K.PROJECT, kind = ScriptAssetKind.BGM)
        val empty = AssetForm.initial(doc, addKey)
        val adding = AssetForm.ui(doc, addKey, empty.copy(lengthText = "161"), readOnly = false)
        assertEquals(AssetsCopy.ADD_TITLE, adding.title)
        assertEquals(listOf("배경", "BGM", "효과음", "CG"), adding.kinds.map { it.second })
        assertEquals(1, adding.kindIndex)
        assertTrue(adding.showLength)
        assertFalse(adding.showImage)
        assertFalse(adding.showPlace)
        assertEquals("2분 41초", adding.lengthPreview)
        assertFalse(adding.canSave)
        assertEquals(AssetsCopy.ADD, adding.primary)

        val cgKey = AssetFormSheetKey(K.PROJECT, assetKey = K.ASSET_CG_FLICKER)
        val cg = AssetForm.ui(doc, cgKey, AssetForm.initial(doc, cgKey).copy(kind = ScriptAssetKind.BGM), readOnly = false)
        assertEquals("깜박이는 등불", cg.title)
        assertEquals(listOf(ScriptAssetKind.CG), cg.kinds.map { it.first })
        assertTrue(cg.showImage)
        assertEquals(AssetsCopy.NO_IMAGE, cg.imageLine)
        assertTrue(cg.canSave)
        assertEquals(AssetsCopy.SAVE, cg.primary)
        assertFalse(AssetForm.ui(doc, cgKey, AssetForm.initial(doc, cgKey), readOnly = true).canSave)

        val bgKey = AssetFormSheetKey(K.PROJECT, assetKey = K.ASSET_BG_LOWER_STREET_DAWN)
        val bg = AssetForm.ui(doc, bgKey, AssetForm.initial(doc, bgKey), readOnly = false)
        assertTrue(bg.showPlace)
        assertEquals(doc.index.place(K.PLACE_LOWER_STREET)?.name, bg.placeLine)
    }

    @Test
    fun `form apply - one write, the picked image joins the blob index in the same update`() {
        val blob = Blob(hash = "h1", fileExt = "png", mime = "image/png", bytes = 3, source = BlobSource.GALLERY.open(), importedAt = 0)
        val key = AssetFormSheetKey(K.PROJECT, kind = ScriptAssetKind.CG)
        val draft = AssetForm.withPicked(AssetFormDraft(ScriptAssetKind.CG, name = "등불", fileName = "cg_lamp"), blob)
        val after = AssetForm.apply(doc, key, draft, Fx.ctx())
        val added = after.assets.scriptAssets.last()
        assertEquals("등불", added.name)
        assertEquals("h1", added.fileHash)
        assertEquals(1, after.assets.blobs.count { it.hash == "h1" })
        // The image was removed again before saving: no index row for it.
        val removed = AssetForm.apply(doc, key, draft.copy(imageHash = null), Fx.ctx())
        assertTrue(removed.assets.blobs.none { it.hash == "h1" })
        // Nothing to save: nothing written.
        assertSame(doc, AssetForm.apply(doc, key, draft.copy(name = " "), Fx.ctx()))
        // An existing asset keeps its kind.
        val cgKey = AssetFormSheetKey(K.PROJECT, assetKey = K.ASSET_CG_FLICKER)
        val changed = AssetForm.apply(doc, cgKey, AssetForm.initial(doc, cgKey).copy(kind = ScriptAssetKind.BGM, name = "꺼진 등불"), Fx.ctx())
        val cg = changed.assets.scriptAssets.single { it.key == K.ASSET_CG_FLICKER }
        assertEquals(ScriptAssetKind.CG, cg.kind.known)
        assertEquals("꺼진 등불", cg.name)
    }

    @Test
    fun `form draft with a picked image survives saved state`() {
        val blob = Blob(hash = "h2", fileExt = "jpg", mime = "image/jpeg", bytes = 9, width = 4, height = 3, source = BlobSource.GALLERY.open(), importedAt = 5)
        val draft = AssetForm.withPicked(AssetFormDraft(ScriptAssetKind.BACKGROUND, "문", "bg_gate", lengthText = "", placeKey = K.PLACE_SOCIETY_GATE), blob)
        assertEquals(draft, AssetForm.decode(AssetForm.encode(draft)))
        assertNull(AssetForm.decode("not json"))
    }

    @Test
    fun `SCR-68 the 에셋 command and every suggested command route to a command, never to search`() {
        assertEquals(CommandAction.OpenAssets, CommandRouting.route(doc, K.EPISODE_1_7, AssetsCopy.TITLE))
        val commands = ScriptCommands.commands(doc, K.EPISODE_1_7)
        assertTrue(AssetsCopy.TITLE in commands)
        commands.forEach { command ->
            val action = CommandRouting.route(doc, K.EPISODE_1_7, command)
            assertFalse(command, action is CommandAction.Search)
        }
        assertTrue(AssetsCopy.TITLE in CommandSuggestions.of(doc, K.EPISODE_1_7, "ㅇㅅ"))
        // The text already typed in full is not offered again.
        assertFalse("/대" in CommandSuggestions.of(doc, K.EPISODE_1_7, "/대"))
        assertTrue(CommandSuggestions.of(doc, K.EPISODE_1_7, "/").all { it.startsWith("/") })
        assertTrue(CommandSuggestions.of(doc, K.EPISODE_1_7, "/").size <= CommandSuggestions.MAX)
        assertTrue(CommandSuggestions.of(doc, K.EPISODE_1_7, "  ").isEmpty())
    }
}
