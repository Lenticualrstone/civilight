package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.designsystem.text.CopyLint
import io.github.lenticualrstone.civilight.domain.script.DirectionCues
import io.github.lenticualrstone.civilight.domain.script.ScriptAssets
import io.github.lenticualrstone.civilight.domain.script.ScriptCommands
import io.github.lenticualrstone.civilight.feature.script.assets.AssetsCopy
import io.github.lenticualrstone.civilight.feature.script.change.ChangeCopy
import io.github.lenticualrstone.civilight.feature.script.links.LinkCopy
import io.github.lenticualrstone.civilight.feature.script.panel.SubjectDeleted
import io.github.lenticualrstone.civilight.feature.script.readonly.ReadOnlyCopy
import io.github.lenticualrstone.civilight.feature.script.scenes.SceneCopy
import io.github.lenticualrstone.civilight.feature.script.terms.TermCopy
import io.github.lenticualrstone.civilight.feature.script.select.MoveCopy
import io.github.lenticualrstone.civilight.feature.script.settings.TransitionCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** DECISIONS §3: every UI string of the script screen passes the DS copy lint. */
class ScriptCopyLintTest {
    private fun lintAll(values: List<String>, button: Boolean = false): Map<String, Any> =
        values.filterNot { it.startsWith(TODO) }
            .associateWith { CopyLint.lint(it, isButton = button) }
            .filterValues { it.isNotEmpty() }

    @Test
    fun `script copy passes the lint`() {
        assertEquals(emptyMap<String, Any>(), lintAll(ScriptCopy.all))
        assertEquals(emptyMap<String, Any>(), lintAll(ScriptCopy.buttons, button = true))
        // Batch 4: the change sheet, select mode and the 전환 문구 setting.
        assertEquals(emptyMap<String, Any>(), lintAll(ChangeCopy.all + MoveCopy.all + TransitionCopy.all))
        assertEquals(emptyMap<String, Any>(), lintAll(ChangeCopy.buttons + MoveCopy.buttons, button = true))
        // Batch 5: scene sheets, the read-only example, term marks, link cards, `대상 지워짐`.
        assertEquals(emptyMap<String, Any>(), lintAll(batch5))
        assertEquals(emptyMap<String, Any>(), lintAll(SceneCopy.buttons + ReadOnlyCopy.buttons + LinkCopy.buttons, button = true))
        // Batch 6: the asset list and its form.
        assertEquals(emptyMap<String, Any>(), lintAll(AssetsCopy.all))
        assertEquals(emptyMap<String, Any>(), lintAll(AssetsCopy.buttons, button = true))
    }

    private val batch5: List<String>
        get() = SceneCopy.all + ReadOnlyCopy.all + TermCopy.all + LinkCopy.all + SubjectDeleted.LABEL

    @Test
    fun `domain strings the screen draws pass the lint`() {
        val drawn = ScriptCommands.ADD_ROWS.flatMap { listOf(it.name, it.description) } +
            listOf(ScriptCommands.SHEET_TITLE, ScriptCommands.READ_ONLY, ScriptCommands.HINT) +
            ScriptAssets.FILTERS + ScriptCommands.commands(Fx.example, io.github.lenticualrstone.civilight.model.sample.ExampleKeys.EPISODE_1_7) +
            DirectionCues.KINDS.map(DirectionCues::kindWord) + DirectionCues.EFFECTS.map { it.second } +
            io.github.lenticualrstone.civilight.domain.script.Numbering.renumberPreview(Fx.example, io.github.lenticualrstone.civilight.model.sample.ExampleKeys.EPISODE_1_7)
                .let { listOf(it.title, it.subtitle, it.sectionTitle) + it.footer + it.buttons + it.figures.map { f -> f.second } }
        assertEquals(emptyMap<String, Any>(), lintAll(drawn))
    }

    @Test
    fun `no TODO_COPY string is left where it can reach the screen, and no sentence ends in 요`() {
        val every = ScriptCopy.all + ChangeCopy.all + MoveCopy.all + TransitionCopy.all + batch5 + AssetsCopy.all + AssetsCopy.buttons +
            ScriptCopy.LINK_PICKER_TITLE + ScriptCopy.UNDO_TYPING + ScriptCopy.NO_ASSETS + ScriptCopy.REF_NOT_DIALOGUE +
            ScriptCopy.MODS_NO_CHECKS + ScriptCopy.MODS_SCENE_EMPTY + MoveCopy.PASTE + AssetsCopy.NO_ASSETS
        assertTrue(every.none { it.startsWith(TODO) })
        assertTrue(every.none { it.endsWith("요") || it.endsWith("요.") })
        assertEquals(emptyMap<String, Any>(), lintAll(every.distinct()))
    }

    @Test
    fun `no TODO_COPY constant stays in the module's sources`() {
        val left = java.io.File("src/main").walkTopDown().filter { it.isFile && it.extension == "kt" }
            .filter { it.readText().contains(TODO) }.map { it.name }.toList()
        assertEquals(emptyList<String>(), left)
    }

    private companion object {
        const val TODO = "TODO_COPY_"
    }
}
