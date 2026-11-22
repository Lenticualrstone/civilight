package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.script.ScriptAssetKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** SCR-61 `[PROPOSED]` asset form writes, and SCR-65 PDF lines for a plan's scene keys. */
class AssetEditsAndPdfPlanTest {
    private val doc = F1.build()

    @Test
    fun `a new asset keeps only the fields of its kind and lands at the end`() {
        val draft = ScriptAssetEdits.Draft(
            kind = ScriptAssetKind.BGM, name = " 대정전 ", fileName = "bgm_blackout",
            imageHash = "abc", durationMs = 185_000, loop = false, placeKey = K.PLACE_LOWER_STREET,
        )
        val (after, key) = ScriptAssetEdits.add(doc, draft, testCtx())
        val asset = after.assets.scriptAssets.last()
        assertEquals(key, asset.key)
        assertEquals("대정전", asset.name)
        assertNull(asset.fileHash)
        assertNull(asset.placeKey)
        assertEquals(185_000L, asset.durationMs)
        assertEquals(doc.assets.scriptAssets.size + 1, after.assets.scriptAssets.size)
        val usage = ScriptAssets.usage(after).single { it.asset.key == key }
        // Board `Assets`: `bgm_blackout · 3분 05초`.
        assertEquals("bgm_blackout · 3분 05초", ScriptAssets.subline(usage))
        assertTrue(usage.unused)
    }

    @Test
    fun `a draft without a name or file name is not saved`() {
        val blank = ScriptAssetEdits.Draft(ScriptAssetKind.SFX, name = "  ", fileName = "se_x")
        assertFalse(ScriptAssetEdits.canSave(blank))
        assertSame(doc, ScriptAssetEdits.add(doc, blank, testCtx()).first)
        assertSame(doc, ScriptAssetEdits.update(doc, K.ASSET_SFX_WIND, blank, testCtx()))
    }

    @Test
    fun `changing an asset keeps its kind and its uses`() {
        val old = doc.assets.scriptAssets.single { it.key == K.ASSET_CG_FLICKER }
        val draft = ScriptAssetEdits.draftOf(old).copy(kind = ScriptAssetKind.BGM, name = "꺼진 등불", imageHash = "hash1")
        val after = ScriptAssetEdits.update(doc, old.key, draft, testCtx())
        val changed = after.assets.scriptAssets.single { it.key == old.key }
        assertEquals(ScriptAssetKind.CG, changed.kind.known)
        assertEquals("꺼진 등불", changed.name)
        assertEquals("hash1", changed.fileHash)
        val usage = ScriptAssets.usage(after).single { it.asset.key == old.key }
        assertEquals(ScriptAssets.usage(doc).single { it.asset.key == old.key }.total, usage.total)
        assertEquals(usage.firstEpisodeCode, ScriptAssets.trailing(usage))
        // The same values again change nothing.
        assertSame(after, ScriptAssetEdits.update(after, old.key, ScriptAssetEdits.draftOf(changed), testCtx(now = 1L)))
    }

    @Test
    fun `an imported image is listed in the blob index once`() {
        val blob = Blob(hash = "h", fileExt = "png", mime = "image/png", bytes = 3, source = BlobSource.GALLERY.open(), importedAt = 0)
        val once = ScriptAssetEdits.withBlob(doc, blob)
        assertEquals(doc.assets.blobs.size + 1, once.assets.blobs.size)
        assertSame(once, ScriptAssetEdits.withBlob(once, blob))
    }

    @Test
    fun `pdf lines for a plan's scene keys equal the scope's and skip gone scenes`() {
        val times = StoryTimes.source(doc)
        val byScope = ScriptPdfContent.lines(doc, ExportScope.pdf(listOf(F1.S3, F1.S4)), times, includeDirectionAndParens = false)
        val byKeys = ScriptPdfContent.linesOf(doc, listOf(F1.S3, F1.S4, "no-such-scene"), times, includeDirectionAndParens = false)
        assertEquals(byScope, byKeys)
        assertEquals(ScriptPdfContent.Line.Title("씬 3 · 결사의 문 · 2일째 새벽 05:40"), byKeys.first())
    }
}
