package io.github.lenticualrstone.civilight.feature.cast

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
    fun `only the 관계 tab is a hole, and it is the 관계 step`() {
        assertEquals("관계", CastUiCopy.TABS[ProfileTab.RELATIONS.ordinal])
        assertEquals(CastTourSteps.RELATIONS, CastTourSteps.stepOfTab(ProfileTab.RELATIONS.ordinal))
        ProfileTab.entries.filter { it != ProfileTab.RELATIONS }.forEach { assertNull(it.name, CastTourSteps.stepOfTab(it.ordinal)) }
    }
}
