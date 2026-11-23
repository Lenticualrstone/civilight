package io.github.lenticualrstone.civilight.designsystem.icon

import io.github.lenticualrstone.civilight.designsystem.theme.MotionLint
import java.io.File

/** One icon-only call without a usable accessibility name: [file] (as passed), 1-based [line], the call text. */
data class IconButtonIssue(val file: String, val line: Int, val call: String, val snippet: String) {
    override fun toString(): String = "$file:$line $call without a non-blank contentDescription: ${snippet.trim()}"
}

/**
 * DS-70: every icon-only button (class a) carries a non-blank accessibility name. The Kotlin type already
 * forbids `null` on [CALLS]; this source lint (like [MotionLint]) also catches a blank literal (`""`, `" "`),
 * an explicit `null`, and a call that names no description at all. Calls that take an [IconButtonSpec]
 * (`CivilightIconButton(IconButtonSpec.SEND, …)`) are skipped: the spec carries the name.
 * Comments and string contents are not searched for calls.
 */
object IconButtonLint {
    /** Composables that draw an icon with no visible text. */
    val CALLS: List<String> = listOf("CivilightIconButton", "GlassRoundButton", "IconOnlyButton")

    private val call = Regex("""\b(${CALLS.joinToString("|")})\s*\(""")
    private val blankLiteral = Regex("""^"\s*"$""")

    fun lint(name: String, source: String): List<IconButtonIssue> {
        val code = MotionLint.blankCommentsAndStrings(source)
        val lines = source.lines()
        val out = mutableListOf<IconButtonIssue>()
        for (m in call.findAll(code)) {
            val before = code.substring(0, m.range.first).trimEnd()
            if (before.endsWith("fun") || before.endsWith(".")) continue
            val open = m.range.last
            val close = matching(code, open) ?: continue
            val args = topLevelArgs(code, open + 1, close).map { source.substring(it.first, it.last + 1).trim() }
            if (args.isEmpty()) {
                out += issue(name, code, lines, m.range.first, m.groupValues[1])
                continue
            }
            if (args.first().startsWith("IconButtonSpec.") || args.first().startsWith("spec") || args.any { it.startsWith("spec =") }) continue
            val named = args.firstOrNull { it.replace(" ", "").startsWith("contentDescription=") }
            val value = when {
                named != null -> named.substringAfter('=').trim()
                else -> args.filterNot { isNamed(it) }.getOrNull(1)
            }
            if (value == null || value == "null" || blankLiteral.matches(value)) {
                out += issue(name, code, lines, m.range.first, m.groupValues[1])
            }
        }
        return out
    }

    /** Lints every `.kt` file under [root]; `build` folders are skipped. */
    fun lintDir(root: File): List<IconButtonIssue> =
        if (!root.exists()) emptyList() else root.walkTopDown()
            .onEnter { it.name != "build" && !it.name.startsWith(".") }
            .filter { it.isFile && it.extension == "kt" }
            .sortedBy { it.path }
            .flatMap { f -> lint(f.relativeTo(root).path, f.readText()) }
            .toList()

    fun report(issues: List<IconButtonIssue>): String = issues.joinToString("\n")

    private fun isNamed(arg: String): Boolean = Regex("""^[A-Za-z_][A-Za-z0-9_]*\s*=[^=]""").containsMatchIn(arg)

    private fun issue(name: String, code: String, lines: List<String>, offset: Int, callName: String): IconButtonIssue {
        val line = code.substring(0, offset).count { it == '\n' } + 1
        return IconButtonIssue(name, line, callName, lines.getOrElse(line - 1) { "" })
    }

    private fun matching(text: String, openIndex: Int): Int? {
        var depth = 0
        for (i in openIndex until text.length) {
            when (text[i]) {
                '(', '[', '{' -> depth++
                ')', ']', '}' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return null
    }

    /** Index ranges of the top-level arguments between [from] and [to] (exclusive), commas inside brackets ignored. */
    private fun topLevelArgs(code: String, from: Int, to: Int): List<IntRange> {
        val out = mutableListOf<IntRange>()
        var depth = 0
        var start = from
        for (i in from until to) {
            when (code[i]) {
                '(', '[', '{' -> depth++
                ')', ']', '}' -> depth--
                ',' -> if (depth == 0) {
                    out += start until i
                    start = i + 1
                }
            }
        }
        if (code.substring(start, to).isNotBlank()) out += start until to
        return out.filter { code.substring(it.first, it.last + 1).isNotBlank() }
    }
}
