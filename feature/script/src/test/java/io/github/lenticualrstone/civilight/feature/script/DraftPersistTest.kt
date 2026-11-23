package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.feature.script.input.Draft
import io.github.lenticualrstone.civilight.feature.script.input.DraftMark
import io.github.lenticualrstone.civilight.feature.script.input.Drafts
import io.github.lenticualrstone.civilight.model.app.ProjectViewFile
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.ScriptViewState
import io.github.lenticualrstone.civilight.model.text.TextRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** (0.2.0, U-14) The unsent draft is kept in `_app/view.json › script.draft` and survives process death. */
class DraftPersistTest {
    @Test
    fun `an empty draft is not kept, a typed one round-trips through view json`() {
        assertNull(persistedDraft(Draft(kind = BlockKind.DIALOGUE)))
        val typed = Draft(runs = listOf(TextRun.Plain("비가 온다")), selStart = 5, selEnd = 5, kind = BlockKind.DIRECTION, mark = DraftMark.After("B1"))
        val stored = persistedDraft(typed)
        val file = ProjectViewFile(script = ScriptViewState(draft = stored))
        val json = CivilightJson.json.encodeToString(ProjectViewFile.serializer(), file)
        val back = CivilightJson.json.decodeFromString(ProjectViewFile.serializer(), json)
        assertEquals(typed, Drafts.decode(back.script.draft))
        // A 0.1.0 view.json has no draft: it reads as none.
        assertNull(CivilightJson.json.decodeFromString(ProjectViewFile.serializer(), "{}").script.draft)
    }
}
