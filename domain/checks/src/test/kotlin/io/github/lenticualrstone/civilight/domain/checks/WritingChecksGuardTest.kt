package io.github.lenticualrstone.civilight.domain.checks

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * (0.2.0, S-2, D33, DECISIONS §1) Every check list and count outside `domain:checks` goes through
 * [WritingChecks]: a screen that counted with [ProjectChecks] would include the gated voice, travel and
 * candidate kinds and show a number that does not match the 점검 센터.
 */
class WritingChecksGuardTest {
    private val pattern = Regex("""\bProjectChecks\s*(\.|::)\s*(run|check)\b""")

    @Test
    fun noProjectChecksCallsOutsideDomainChecks() {
        val root = generateSequence(File("").absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "settings.gradle.kts").isFile }
        assertNotNull("repository root not found", root)
        val ownMain = File(root, "domain/checks/src/main").canonicalPath
        val offenders = root!!.walkTopDown()
            .onEnter { dir -> dir.name != "build" && !dir.name.startsWith(".") }
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.canonicalPath.startsWith(ownMain) }
            .filterNot { f -> f.invariantSeparatorsPath.split('/').let { "test" in it || "androidTest" in it } }
            .flatMap { f ->
                f.readLines().withIndex()
                    .filter { (_, line) -> pattern.containsMatchIn(line) }
                    .map { (i, _) -> "${f.relativeTo(root).invariantSeparatorsPath}:${i + 1}" }
            }
            .toList()
        assertTrue("use WritingChecks.run / check instead: $offenders", offenders.isEmpty())
    }
}
