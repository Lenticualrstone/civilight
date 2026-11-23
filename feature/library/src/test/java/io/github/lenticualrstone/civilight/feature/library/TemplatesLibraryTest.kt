package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.domain.cast.field.FieldSettings
import io.github.lenticualrstone.civilight.domain.cast.field.FieldTemplates
import io.github.lenticualrstone.civilight.domain.library.crypto.PackageCrypto
import io.github.lenticualrstone.civilight.domain.library.crypto.RecoveryKeys
import io.github.lenticualrstone.civilight.feature.library.backup.BackupCopy
import io.github.lenticualrstone.civilight.feature.library.backup.BackupSettingsState
import io.github.lenticualrstone.civilight.feature.library.backup.TemplateBackup
import io.github.lenticualrstone.civilight.feature.library.newproject.CreatorPorts
import io.github.lenticualrstone.civilight.feature.library.newproject.NewProjectCopy
import io.github.lenticualrstone.civilight.feature.library.newproject.NewProjectForm
import io.github.lenticualrstone.civilight.feature.library.newproject.NewProjectRules
import io.github.lenticualrstone.civilight.feature.library.newproject.ProjectCreator
import io.github.lenticualrstone.civilight.feature.library.settings.SettingsCopy
import io.github.lenticualrstone.civilight.feature.library.settings.SettingsRootRow
import io.github.lenticualrstone.civilight.feature.library.settings.SettingsRoutes
import io.github.lenticualrstone.civilight.feature.library.settings.SettingsState
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.FieldTemplate
import io.github.lenticualrstone.civilight.model.app.RatingDefaults
import io.github.lenticualrstone.civilight.model.app.TemplateField
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.FieldBundle
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.navigation.TemplateList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

/** 인물 템플릿 in feature:library (30-cast.md §7: CST-58, CST-61, CST-63; TC-F-39, 42, 46). */
class TemplatesLibraryTest {
    private fun tf(key: String, name: String, kind: EntityKind = EntityKind.CHARACTER) =
        TemplateField(key = key, origin = "tpl:$key", entityKind = kind.open(), name = name, type = FieldType.SHORT_TEXT.open(), order = "a$key")

    /** `내 기본`: 인물 10, 장소 1. */
    private val mine = FieldTemplate(
        key = "T1", name = "내 기본", order = "a",
        fields = (1..10).map { tf("c$it", "항목$it") } + tf("p1", "기후", EntityKind.PLACE),
    )
    private val glass = FieldTemplate(key = "H1", name = "유리 정원 인물", hidden = true, order = "b", fields = listOf(tf("g", "유리 꽃")))

    private class Ports : CreatorPorts {
        val added = mutableListOf<ProjectDoc>()
        override suspend fun addProject(doc: ProjectDoc) {
            added += doc
        }
        override suspend fun updateRecorder(transform: (RecorderLibrary) -> RecorderLibrary) = Unit
    }

    @Test
    fun `TC-F-39 a template at step 4 applies every kind`() {
        val form = NewProjectForm(name = "새", bundle = FieldBundle.TEMPLATE, bundleTemplateKey = mine.key)
        assertEquals(Triple("인물 항목", "인물 10 · 장소 1", "내 기본 ›"), NewProjectRules.bundleRow(form, listOf(mine), hiddenUnlocked = false))
        val doc = ProjectCreator.build(form, RatingDefaults(), LibFx.ctx(), null, template = mine).doc
        assertEquals(FieldBundle.TEMPLATE, doc.project.fieldBundleAtCreation.known)
        assertEquals(10, FieldSettings.projectFields(doc, EntityKind.CHARACTER).size)
        assertEquals(1, FieldSettings.projectFields(doc, EntityKind.PLACE).size)
        // The fixed count follows `게임 제작 도구` (cast lane, G-2: a new project has it off), so only the template
        // name and the project-field count are this test's.
        val sub = FieldSettings.entryRow(doc).second
        assertTrue(sub, sub.startsWith("내 기본 · 고정 ") && sub.contains(" · 프로젝트 10"))
        // No choice: `기본` (TC-F-15 unchanged).
        assertEquals(FieldBundle.BASIC, NewProjectForm().bundle)
        assertEquals("기본 ›", NewProjectRules.bundleRow(NewProjectForm(), listOf(mine), false).third)
    }

    @Test
    fun `TC-F-42 a hidden template picked at step 4 re-locks without its name and is still applied`() {
        val form = NewProjectForm(name = "새", bundle = FieldBundle.TEMPLATE, bundleTemplateKey = glass.key)
        assertEquals(Triple("인물 항목", "", "숨긴 템플릿 ›"), NewProjectRules.bundleRow(form, listOf(glass), hiddenUnlocked = false))
        assertEquals("유리 정원 인물 ›", NewProjectRules.bundleRow(form, listOf(glass), hiddenUnlocked = true).third)
        val doc = ProjectCreator.build(form, RatingDefaults(), LibFx.ctx(), null, template = glass).doc
        assertEquals(listOf("유리 꽃"), FieldSettings.projectFields(doc, EntityKind.CHARACTER).map { it.name })
        assertEquals("숨긴 템플릿", FieldSettings.bundleName(doc, EntityKind.CHARACTER))
        // The form keeps the key only, never the name.
        assertFalse(io.github.lenticualrstone.civilight.feature.library.newproject.NewProjectDraftHolder.encode(form).contains("유리"))
    }

    @Test
    fun `CST-61 a template deleted before 만들기 falls back to 기본 with a notice`() = runTest {
        val form = NewProjectForm(name = "새", bundle = FieldBundle.TEMPLATE, bundleTemplateKey = "gone")
        val ports = Ports()
        val result = ProjectCreator.create(ports, form, RatingDefaults(), LibFx.ctx(), null, template = null)
        assertTrue(result.templateGone)
        val doc = ports.added.single()
        assertEquals(FieldBundle.BASIC, doc.project.fieldBundleAtCreation.known)
        assertEquals(listOf("출생", "나이", "생일", "소속", "성격"), FieldSettings.projectFields(doc, EntityKind.CHARACTER).map { it.name })
        assertEquals("◇ 템플릿 없음 · 기본으로", NewProjectCopy.TEMPLATE_GONE)
        assertFalse(ProjectCreator.create(Ports(), NewProjectForm(name = "a"), RatingDefaults(), LibFx.ctx(), null).templateGone)
    }

    @Test
    fun `CST-58 the 설정 row counts visible templates only and opens the list`() {
        val ui = SettingsState.root(AppSettings(), 0, false, null, 0L, ZoneId.of("Asia/Seoul"), visibleTemplates = FieldTemplates.visible(listOf(mine, glass), false).size)
        assertEquals("인물 템플릿", ui.templates.title)
        assertEquals("템플릿 1", ui.templates.sub)
        assertEquals("없음", SettingsCopy.templatesSub(0))
        assertEquals("인물", SettingsCopy.SECTION_CAST)
        assertEquals(TemplateList(), SettingsRoutes.routeOf(SettingsRootRow.TEMPLATES))
    }

    @Test
    fun `TC-F-46 hidden templates are backed up only locked with the 복구 암호`() {
        val on = AppSettings(driveBackup = true)
        val noKey = TemplateBackup.plan(listOf(mine, glass), on, recoverySet = false)
        assertEquals(listOf("T1"), noKey.visible.map { it.key })
        assertTrue(noKey.hidden.isEmpty())
        assertEquals(listOf("숨긴 템플릿 · 백업 안 됨"), noKey.warnings)
        assertTrue(BackupSettingsState.warnings(emptyList(), on, false, listOf(mine, glass)).contains(BackupCopy.HIDDEN_TEMPLATES))
        // The visible file never names the hidden template.
        assertFalse(String(TemplateBackup.encode(noKey.visible)).contains("유리"))

        val withKey = TemplateBackup.plan(listOf(mine, glass), on.copy(backupHidden = true), recoverySet = true)
        assertEquals(listOf("H1"), withKey.hidden.map { it.key })
        assertTrue(withKey.warnings.isEmpty())
        val key = RecoveryKeys.create("등불지기의새벽1".toCharArray(), iterations = 1_000)
        val locked = PackageCrypto.encryptWithRecoveryKey(TemplateBackup.encode(withKey.hidden), key, time = 1L, mimetype = "application/json")
        assertFalse(String(locked, Charsets.ISO_8859_1).contains("유리"))
        assertEquals(withKey.hidden, TemplateBackup.decode(locked, encrypted = true, password = "등불지기의새벽1".toCharArray()))
        assertNull(TemplateBackup.decode(locked, encrypted = true, password = null))
        // Off: hidden templates stay out even with a 복구 암호.
        assertTrue(TemplateBackup.plan(listOf(glass), on.copy(backupHidden = false), recoverySet = true).hiddenLeftOut)
    }

    @Test
    fun `CST-63 restore merges by key`() {
        val local = listOf(mine.copy(updatedAt = 5))
        val incoming = listOf(mine.copy(name = "새 이름", updatedAt = 9), glass)
        val merged = TemplateBackup.merge(local, TemplateBackup.decode(TemplateBackup.encode(incoming), false, null)!!)
        assertEquals(listOf("T1", "H1"), merged.map { it.key })
        assertEquals("새 이름", merged.first().name)
    }
}
