package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.designsystem.text.CopyLint
import io.github.lenticualrstone.civilight.domain.trash.TrashCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** 0.5 interface review: every delete confirm reads `‘X’ 지우기`; an undoable one confirms with primary `휴지통으로`. */
class DeleteConfirmCopyTest {
    @Test
    fun `delete confirm titles share one form`() {
        assertEquals("‘주연’ 지우기", CastUiCopy.deleteTitle("주연"))
        assertEquals(CastUiCopy.deleteTitle("주연"), CastUiCopy.tagDeleteTitle("주연"))
        assertEquals(emptyList<Any>(), CopyLint.lint(CastUiCopy.deleteTitle("선배 ↔ 후배")))
    }

    @Test
    fun `the trash confirm label is the same word everywhere`() {
        assertEquals("휴지통으로", CastUiCopy.BTN_TO_TRASH)
        assertEquals(CastUiCopy.BTN_TO_TRASH, TrashCopy.TO_TRASH)
    }

    /** Field and template deletes go to trash: no `hardToUndo` (danger) on their confirm, titles in the shared form. */
    @Test
    fun `undoable delete confirms are primary`() {
        val root = File("src/main/java/io/github/lenticualrstone/civilight/feature/cast")
        val field = File(root, "settings/FieldDefEditScreen.kt").readText()
        val deleteBlock = field.substringAfter("Pending.Delete -> ConfirmDialog(").substringBefore("\n        )")
        assertTrue(deleteBlock.contains("CastUiCopy.deleteTitle("))
        assertTrue(!deleteBlock.contains("hardToUndo"))
        val template = File(root, "templates/TemplateEditScreen.kt").readText()
        val templateBlock = template.substringAfter("if (confirmDelete) {").substringBefore("\n        )")
        assertTrue(templateBlock.contains("CastUiCopy.deleteTitle("))
        assertTrue(templateBlock.contains("CastUiCopy.BTN_TO_TRASH"))
        assertTrue(!templateBlock.contains("hardToUndo"))
    }
}
