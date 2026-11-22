package io.github.lenticualrstone.civilight.store

import io.github.lenticualrstone.civilight.store.io.AtomicFiles
import io.github.lenticualrstone.civilight.store.io.SimulatedCrash
import io.github.lenticualrstone.civilight.store.io.StoreDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class StoreDirectoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun temps(dir: StoreDirectory) = dir.root.walk().filter { AtomicFiles.isTemp(it) }.toList()

    @Test
    fun atomicWriteReplacesTheFile() {
        val dir = StoreDirectory(tmp.newFolder("d"))
        dir.write("a/b.json", "1".toByteArray())
        dir.write("a/b.json", "2".toByteArray())
        assertEquals("2", dir.readText("a/b.json"))
        assertEquals(listOf("a/b.json"), dir.listFiles())
        assertTrue(temps(dir).isEmpty())
    }

    @Test
    fun aHalfWrittenTempNeverReplacesAGoodFile() {
        val dir = StoreDirectory(tmp.newFolder("d"))
        dir.write("cast.json", "good".toByteArray())
        dir.faults = CrashAt(halfWriteOf = "cast.json")
        runCatching { dir.write("cast.json", "new content that is long".toByteArray()) }
            .onSuccess { error("expected a crash") }
            .onFailure { assertTrue(it is SimulatedCrash) }
        assertEquals("good", dir.readText("cast.json"))
        assertEquals(1, temps(dir).size)
        // Next start: the temp is dropped, the good file stays.
        val restarted = StoreDirectory(dir.root)
        assertFalse(restarted.recover())
        assertEquals("good", restarted.readText("cast.json"))
        assertTrue(temps(restarted).isEmpty())
        assertEquals(listOf("cast.json"), restarted.listFiles())
    }

    @Test
    fun aTransactionInterruptedBeforeItsJournalIsDropped() {
        val dir = StoreDirectory(tmp.newFolder("d"))
        dir.commit(mapOf("a.json" to "a1".toByteArray(), "b.json" to "b1".toByteArray()))
        dir.faults = CrashAt(beforeJournal = true)
        runCatching { dir.commit(mapOf("a.json" to "a2".toByteArray(), "b.json" to "b2".toByteArray()), listOf("c.json")) }
        val restarted = StoreDirectory(dir.root)
        assertFalse(restarted.recover())
        assertEquals("a1", restarted.readText("a.json"))
        assertEquals("b1", restarted.readText("b.json"))
        assertTrue(temps(restarted).isEmpty())
    }

    @Test
    fun aTransactionInterruptedAfterItsJournalIsCompleted() {
        val dir = StoreDirectory(tmp.newFolder("d"))
        dir.commit(mapOf("a.json" to "a1".toByteArray(), "b.json" to "b1".toByteArray(), "c.json" to "c1".toByteArray()))
        dir.faults = CrashAt(renameNumber = 2)
        runCatching { dir.commit(mapOf("a.json" to "a2".toByteArray(), "b.json" to "b2".toByteArray()), listOf("c.json")) }
        // Mid-way: a renamed, b not yet, c not yet deleted.
        assertEquals("a2", dir.readText("a.json"))
        assertEquals("b1", dir.readText("b.json"))
        val restarted = StoreDirectory(dir.root)
        assertTrue(restarted.recover())
        assertEquals("a2", restarted.readText("a.json"))
        assertEquals("b2", restarted.readText("b.json"))
        assertNull(restarted.readText("c.json"))
        assertFalse(restarted.file(StoreDirectory.JOURNAL).exists())
        assertTrue(temps(restarted).isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun pathsCannotLeaveTheDirectory() {
        StoreDirectory(tmp.newFolder("d")).file("../x.json")
    }
}
