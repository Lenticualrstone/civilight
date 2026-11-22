package io.github.lenticualrstone.civilight.feature.story

import io.github.lenticualrstone.civilight.domain.library.masking.HiddenAccess
import io.github.lenticualrstone.civilight.domain.story.search.SearchFilter
import io.github.lenticualrstone.civilight.domain.story.search.SearchSectionKind
import io.github.lenticualrstone.civilight.feature.story.search.CrossProject
import io.github.lenticualrstone.civilight.feature.story.search.LibrarySearchModel
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import io.github.lenticualrstone.civilight.ui.security.UnlockState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 모든 프로젝트에서 찾기 (LIB-07 scope over STO-37): projects in scope, sections, footer and the empty line. */
class LibrarySearchModelTest {
    private val example = Fx.doc()
    private val hidden = Fx.mini(listOf("씬"), Fx.node("S1", "오리지늄 창고", text = "오리지늄 상자가 있었다.")).let { d ->
        d.copy(project = d.project.copy(protections = d.project.protections.copy(hidden = true)))
    }

    private fun run(access: HiddenAccess = HiddenAccess(), query: String = "오리지", filter: SearchFilter = SearchFilter.ALL) =
        LibrarySearchModel.search(listOf(CrossProject(example, isExample = true), CrossProject(hidden)), access, AppSettings(), { false }, query, filter)

    @Test
    fun lib07_hiddenProjectsStayOutWhileLockedAndTheFooterSaysSo() {
        val ui = run()!!
        assertEquals(listOf(K.PROJECT), ui.sections.map { it.projectKey })
        assertEquals("잿빛 새벽의 등불", ui.sections.single().title)
        assertEquals("숨긴 프로젝트 제외", ui.footer)
        assertNull(ui.emptyLine)
        assertTrue(ui.sections.single().result.sections.none { it.kind == SearchSectionKind.COMMAND })
        assertFalse(SearchFilter.COMMAND in LibrarySearchModel.filters)
    }

    @Test
    fun nothingFoundGivesTheEmptyLineWithTheScope() {
        val ui = run(query = "없는말없는말")!!
        assertTrue(ui.sections.isEmpty())
        assertEquals("‘없는말없는말’ · 숨긴 프로젝트 제외", ui.emptyLine)
        assertNull(run(query = " "))
    }

    @Test
    fun theUnlockStateBecomesTheLibrarysAccess() {
        val access = LibrarySearchModel.access(UnlockState(hiddenGroupUnlockedAt = 1L, keptOpenSince = mapOf("K" to 2L)))
        assertTrue(access.groupOpen)
        assertEquals(setOf("K"), access.keptOpen)
    }
}
