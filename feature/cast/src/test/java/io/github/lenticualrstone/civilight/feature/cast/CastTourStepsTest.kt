package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.domain.cast.field.FieldRow
import io.github.lenticualrstone.civilight.domain.library.onboarding.ModuleTours
import io.github.lenticualrstone.civilight.domain.library.onboarding.TourModule
import io.github.lenticualrstone.civilight.feature.cast.profile.CastTourSteps
import io.github.lenticualrstone.civilight.navigation.ProfileTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** LIB-19 cast tour holes (emulator QA: its cards sat in the middle without a hole). */
class CastTourStepsTest {
    private val titles = ModuleTours.steps(TourModule.CAST).map { it.title }

    @Test
    fun `step ids follow the tour's copy`() {
        assertEquals(3, titles.size)
        assertEquals("시점 렌즈", titles[CastTourSteps.LENS])
        assertEquals("값이 바뀌는 때", titles[CastTourSteps.TIMED_VALUE])
        assertEquals("관계", titles[CastTourSteps.RELATIONS])
    }

    @Test
    fun `값이 바뀌는 때 points at a header field row, a changing one first`() {
        val age = FieldRow("나이", "17", defKey = "f-age")
        val rank = FieldRow("계급", "기사", since = "1-3부터", hasHistory = true, defKey = "f-rank")
        val firstSeen = FieldRow("첫 등장", "1-1")
        assertEquals("계급", CastTourSteps.timedValueRow(listOf(firstSeen, age, rank)))
        assertEquals("나이", CastTourSteps.timedValueRow(listOf(firstSeen, age)))
        // No field row: the lens chip takes the hole (its fallback), so the card never floats.
        assertNull(CastTourSteps.timedValueRow(listOf(firstSeen)))
        assertNull(CastTourSteps.timedValueRow(emptyList()))
    }

    @Test
    fun `only the 관계 tab is a hole, and it is the 관계 step`() {
        assertEquals("관계", CastUiCopy.TABS[ProfileTab.RELATIONS.ordinal])
        assertEquals(CastTourSteps.RELATIONS, CastTourSteps.stepOfTab(ProfileTab.RELATIONS.ordinal))
        ProfileTab.entries.filter { it != ProfileTab.RELATIONS }.forEach { assertNull(it.name, CastTourSteps.stepOfTab(it.ordinal)) }
    }
}
