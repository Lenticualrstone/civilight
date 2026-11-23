package io.github.lenticualrstone.civilight.store

import io.github.lenticualrstone.civilight.model.app.FieldTemplate
import io.github.lenticualrstone.civilight.model.app.FieldTemplatesFile
import io.github.lenticualrstone.civilight.model.app.TemplateField
import io.github.lenticualrstone.civilight.model.cast.DatePrecision
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.store.templates.FieldTemplateStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** CST-63: `files/app/fieldTemplates.json`. */
@OptIn(ExperimentalCoroutinesApi::class)
class FieldTemplateStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private val day = 24L * 60 * 60 * 1000

    private fun template(key: String, deletedAt: Long? = null) = FieldTemplate(
        key = key,
        name = "내 기본",
        hidden = key == "H",
        order = "a$key",
        deletedAt = deletedAt,
        fields = listOf(
            TemplateField(
                key = "F1", origin = "basic.birth", entityKind = EntityKind.CHARACTER.open(), name = "출생",
                type = FieldType.DATE.open(), order = "a", datePrecision = DatePrecision.YEAR.open(), sourceDefKeys = listOf("D1"),
            ),
            TemplateField(
                key = "F2", origin = "basic.age", entityKind = EntityKind.CHARACTER.open(), name = "나이",
                type = FieldType.CALC.open(), order = "b", calcSourceKey = "F1",
            ),
        ),
    )

    @Test
    fun `the file round-trips and keeps every template field`() {
        val file = FieldTemplatesFile(templates = listOf(template("T"), template("H")))
        val text = CivilightJson.encode(FieldTemplatesFile.serializer(), file)
        assertEquals(file, CivilightJson.decode(FieldTemplatesFile.serializer(), text))
        assertTrue(text.contains("\"formatVersion\":1"))
    }

    @Test
    fun `missing file is empty, writes land in app and trashed templates go after 30 days`() = runTest {
        val clock = ManualClock(Fx.T0)
        val dir = File(tmp.root, "app")
        val store = FieldTemplateStore(dir, testEnv(clock))
        store.ensureLoaded()
        assertEquals(emptyList<FieldTemplate>(), store.state.value.templates)

        store.put(template("T"))
        store.put(template("OLD", deletedAt = clock.now() - 31 * day))
        store.put(template("NEW", deletedAt = clock.now() - 29 * day))
        store.flush()
        assertTrue(File(dir, FieldTemplateStore.FILE).isFile)

        val reloaded = FieldTemplateStore(dir, testEnv(clock))
        reloaded.ensureLoaded()
        assertEquals(listOf("T", "NEW"), reloaded.state.value.templates.map { it.key })

        assertEquals(emptyList<String>(), reloaded.purgeDeleted(clock.now()))
        assertEquals(listOf("NEW"), reloaded.purgeDeleted(clock.now() + 2 * day))
    }

    @Test
    fun `an unreadable file reads as empty and is set aside`() = runTest {
        val dir = File(tmp.root, "app").apply { mkdirs() }
        File(dir, FieldTemplateStore.FILE).writeText("{ broken")
        val store = FieldTemplateStore(dir, testEnv())
        store.ensureLoaded()
        assertEquals(emptyList<FieldTemplate>(), store.state.value.templates)
        assertTrue(dir.listFiles().orEmpty().any { it.name.startsWith("${FieldTemplateStore.FILE}.damaged-") })
    }
}
