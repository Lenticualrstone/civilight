package io.github.lenticualrstone.civilight.store

import io.github.lenticualrstone.civilight.model.app.CardStyle
import io.github.lenticualrstone.civilight.model.app.LibraryStyle
import io.github.lenticualrstone.civilight.model.app.ShownView
import io.github.lenticualrstone.civilight.model.app.ViewMode
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.store.project.ProjectStore
import io.github.lenticualrstone.civilight.store.settings.AppSettingsCodec
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Scope §8 acceptance 1 and 00 §13.8 DM-0.2-02, DM-0.2-04, DM-0.2-05: working-store files written
 * by the **v0.1.0 tag's code** (`src/test/resources/v010/`, see its README) open in 0.2.0, keep
 * every byte of user data, and get `게임 제작 도구` decided once by D6.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class V010UpgradeTest {
    @get:Rule val tmp = TemporaryFolder()

    private val key = ExampleKeys.PROJECT

    private fun fixture(name: String): File =
        File(requireNotNull(javaClass.classLoader!!.getResource("v010/$name")) { "fixture v010/$name" }.toURI())

    private fun copyOf(name: String): File {
        val dir = File(tmp.root, "projects/$name/$key")
        fixture(name).copyRecursively(dir)
        return dir
    }

    /** Relative path → bytes of every file under [dir]. */
    private fun tree(dir: File): Map<String, ByteArray> =
        dir.walkTopDown().filter { it.isFile }.associate { it.relativeTo(dir).invariantSeparatorsPath to it.readBytes() }

    private fun json(bytes: ByteArray): JsonElement = CivilightJson.json.parseToJsonElement(bytes.toString(Charsets.UTF_8))

    /** [after] equals [before] file by file; `project.json` differs only by `gameTools` = [gameTools]. */
    private fun assertKept(before: Map<String, ByteArray>, after: Map<String, ByteArray>, gameTools: Boolean) {
        assertEquals(before.keys, after.keys)
        for ((path, bytes) in before) {
            if (path == "project.json") continue
            assertEquals(path, json(bytes), json(after.getValue(path)))
        }
        val old = json(before.getValue("project.json")).jsonObject
        val new = json(after.getValue("project.json")).jsonObject
        assertFalse("0.1.0 never wrote the switch", "gameTools" in old)
        assertEquals(JsonPrimitive(gameTools), new["gameTools"])
        assertEquals("every other project.json field equal", old, JsonObject(new - "gameTools"))
    }

    @Test
    fun `a 0_1_0 project with game data opens with the switch on and keeps every byte`() = runTest {
        val dir = copyOf("store-rich")
        val before = tree(dir)
        val store = ProjectStore.open(key, dir, testEnv())
        val doc = store.doc.value
        assertEquals(true, store.loadReport.gameToolsDecided)
        assertEquals(true, doc.project.gameTools)
        assertTrue(store.loadReport.damaged.isEmpty())
        assertTrue(doc.unknownFiles.isEmpty())
        // D9: the project opens at 최신; the stored lens is kept (written back unchanged, below).
        assertEquals(io.github.lenticualrstone.civilight.model.position.LensPoint.Latest, doc.app.view.lens.point)
        assertEquals("unitEnd", (doc.app.lensKept!!.point as? io.github.lenticualrstone.civilight.model.position.LensPoint.UnitEnd)?.let { "unitEnd" })

        // Only project.json was rewritten, at once; every other file is byte-identical.
        val afterOpen = tree(dir)
        for ((path, bytes) in before) if (path != "project.json") assertArrayEquals(path, bytes, afterOpen.getValue(path))
        assertKept(before, afterOpen, gameTools = true)

        // A save with nothing changed writes nothing.
        val log = WriteLog(dir)
        store.storeDirectory.faults = log
        store.flush()
        assertEquals(emptyList<String>(), log.writes)
        store.close()

        // Written again in full by 0.2.0 core:store: the same files with the same content.
        val again = File(tmp.root, "again/$key")
        ProjectStore.writeNew(doc, again)
        assertKept(before, tree(again), gameTools = true)

        // Every 0.1.0 field survives the 0.2.0 reload, including rating and protections (D34).
        val reloaded = ProjectStore.read(key, again, testEnv())
        assertEquals(doc, reloaded)
        assertEquals("r15", reloaded.project.rating.raw)
        assertTrue(reloaded.project.protections.captureBlock)
        assertTrue(reloaded.cast.voiceLines.isNotEmpty())
        assertTrue(reloaded.allBlocks.any { b -> b.options.any { it.speakerKey != null } })
        assertTrue(reloaded.galleryItems.any { it.holo })
        assertTrue(reloaded.galleryItems.any { it.hidden })
        assertTrue(reloaded.cast.characters.any { it.hidden })
        assertTrue(reloaded.trash.entries.isNotEmpty())
        // Every row of the scope §8 table is still there.
        assertTrue("stage", reloaded.allBlocks.any { it.stage != null })
        assertTrue("manual voice ID", reloaded.allBlocks.any { it.voice?.manualId != null })
        assertTrue("fixed voice ID", reloaded.allBlocks.any { it.voice?.fixedId != null })
        assertTrue("discarded IDs", reloaded.episodes.values.any { it.discardedVoiceIds.isNotEmpty() })
        assertTrue("cues", reloaded.allBlocks.any { it.cues.isNotEmpty() })
        assertTrue("assets", reloaded.assets.scriptAssets.isNotEmpty())
        assertTrue("archive documents", reloaded.cast.archiveDocs.isNotEmpty())
        assertTrue("export history", reloaded.app.activity.exportRecords.isNotEmpty())
        assertEquals("lens state kept (not applied, D9)", "unitEnd", CivilightJson.json.parseToJsonElement(File(again, "_app/view.json").readText()).jsonObject["lens"]!!.jsonObject["point"]!!.jsonObject["kind"]!!.let { (it as JsonPrimitive).content })
        assertTrue("node map", reloaded.maps.values.any { it.baseKind.raw == "node" })
        assertTrue("travel overrides", reloaded.checks.travelOverrides.isNotEmpty())
        assertTrue("acknowledgements", reloaded.checks.acknowledgements.isNotEmpty())
        assertTrue("hand-set reveals", reloaded.changes.records.any { it.target is io.github.lenticualrstone.civilight.model.change.ChangeTarget.TermReveal })
        assertTrue("아님 decisions", reloaded.world.terms.any { it.rejections.isNotEmpty() })
        assertTrue("travel modes", reloaded.project.travelModes.isNotEmpty())
    }

    @Test
    fun `the stored 0_1_0 lens survives a save of other view state (D9, scope 8)`() = runTest {
        val dir = copyOf("store-rich")
        val store = ProjectStore.open(key, dir, testEnv())
        store.updateApp { it.copy(view = it.view.copy(worldTab = "terms")) }
        store.flush()
        store.close()
        val view = CivilightJson.json.parseToJsonElement(File(dir, "_app/view.json").readText()).jsonObject
        assertEquals(JsonPrimitive("terms"), view["worldTab"])
        assertEquals("unitEnd", (view["lens"]!!.jsonObject["point"]!!.jsonObject["kind"] as JsonPrimitive).content)
        // Reopened: still at 최신, the stored lens still kept.
        val reopened = ProjectStore.open(key, dir, testEnv())
        assertEquals(io.github.lenticualrstone.civilight.model.position.LensPoint.Latest, reopened.doc.value.app.view.lens.point)
        assertTrue(reopened.doc.value.app.lensKept!!.point is io.github.lenticualrstone.civilight.model.position.LensPoint.UnitEnd)
        reopened.close()
    }

    @Test
    fun `a 0_1_0 project without game data opens with the switch off`() = runTest {
        val dir = copyOf("store-plain")
        val before = tree(dir)
        val store = ProjectStore.open(key, dir, testEnv())
        assertEquals(false, store.loadReport.gameToolsDecided)
        assertEquals(false, store.doc.value.project.gameTools)
        store.close()
        assertKept(before, tree(dir), gameTools = false)
    }

    @Test
    fun `the decision runs once - a project turned off stays off`() = runTest {
        val dir = copyOf("store-rich")
        val store = ProjectStore.open(key, dir, testEnv())
        store.update("게임 제작 도구") { it.copy(project = it.project.copy(gameTools = false)) }
        store.close()
        val reopened = ProjectStore.open(key, dir, testEnv())
        assertNull(reopened.loadReport.gameToolsDecided)
        assertEquals(false, reopened.doc.value.project.gameTools)
        reopened.close()
    }

    @Test
    fun `a read-only load decides in memory and writes nothing`() = runTest {
        val dir = copyOf("store-rich")
        val before = tree(dir)
        val doc = ProjectStore.read(key, dir, testEnv())
        assertEquals(true, doc.project.gameTools)
        val after = tree(dir)
        assertEquals(before.keys, after.keys)
        for ((path, bytes) in before) assertArrayEquals(path, bytes, after.getValue(path))
    }

    @Test
    fun `0_1_0 settings read with the 3D views and are written back unchanged`() {
        val text = fixture("settings.json").readText()
        val settings = AppSettingsCodec.decode(text)
        assertEquals(mapOf("library" to ViewMode.LIST, "cast" to ViewMode.CARD), settings.viewByScreen)
        assertEquals(LibraryStyle.CD_JEWEL_CASE, ShownView.library(settings))
        assertEquals(CardStyle.FAN_DECK, ShownView.cast(settings))
        assertFalse("S-2 travel check off", settings.checkTravel)
        assertFalse(settings.recorder.lockScreenRecording)
        assertEquals(json(text.toByteArray()), json(AppSettingsCodec.encode(settings).toByteArray()))
    }
}
