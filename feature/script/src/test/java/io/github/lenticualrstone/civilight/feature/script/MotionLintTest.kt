package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.designsystem.theme.MotionLint
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** Motion audit §3.1: no literal durations, curves or default animations in this module. */
class MotionLintTest {
    @Test
    fun `motion lint`() {
        val issues = MotionLint.lintModule(File("."))
        assertEquals(MotionLint.report(issues), 0, issues.size)
    }
}
