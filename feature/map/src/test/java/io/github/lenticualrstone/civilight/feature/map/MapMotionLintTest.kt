package io.github.lenticualrstone.civilight.feature.map

import io.github.lenticualrstone.civilight.designsystem.theme.MotionLint
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** Motion audit: no literal durations, bare `Crossfade`, default `animateItem()` or default snap fling in this module. */
class MapMotionLintTest {
    @Test
    fun `motion lint`() {
        val issues = MotionLint.lintModule(File("."))
        assertEquals(MotionLint.report(issues), 0, issues.size)
    }
}
