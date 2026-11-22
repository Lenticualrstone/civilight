package io.github.lenticualrstone.civilight.designsystem.theme

import java.io.File

/** What [MotionLint] flags (motion audit §3.2). */
enum class MotionRule(val hint: String) {
    /** `tween(320)` / `tween(durationMillis = 320)`: use a `CivilightMotion` token through `motion.spec` / `move`. */
    TWEEN_LITERAL("tween with a literal duration; use motion.spec(token) / motion.move(token)"),

    /** `durationMillis = 400` / `delayMillis = 160` anywhere (keyframes, tween args). */
    DURATION_LITERAL("literal durationMillis / delayMillis; use a CivilightMotion token"),

    /** `Crossfade(x) { }` without `animationSpec`: the default is a 300 ms tween outside the tokens. */
    CROSSFADE_WITHOUT_SPEC("Crossfade without animationSpec; pass motion.spec(...) or use ContentSwap"),

    /** `Modifier.animateItem()` with default specs. */
    ANIMATE_ITEM_DEFAULT("animateItem() with default specs; use civilightItem()"),

    /** `rememberSnapFlingBehavior(...)`: its settle spec is not a token. */
    DEFAULT_SNAP_FLING("rememberSnapFlingBehavior has no token spec; use SnapCardRow or snapFlingBehavior(snapAnimationSpec = motion.spec(enter))"),

    /** `CubicBezierEasing(` outside the design system: curves live in `CivilightMotion`. */
    CUBIC_BEZIER_OUTSIDE_DS("CubicBezierEasing outside the design system; use a CivilightMotion token"),
}

/** One finding: [file] (as passed), 1-based [line], the [rule] and the source line. */
data class MotionIssue(val file: String, val line: Int, val rule: MotionRule, val snippet: String) {
    override fun toString(): String = "$file:$line ${rule.name} (${rule.hint}): ${snippet.trim()}"
}

/**
 * Flags hard-coded motion in Kotlin sources (motion audit §3.2), like [io.github.lenticualrstone.civilight.designsystem.text.CopyLint]
 * for copy. Comments and string contents are ignored. A line (or the line above it) holding the comment
 * `motion-lint: allow` is skipped, for the rare case the spec names a duration that is not a token.
 *
 * Feature tests call it on their own sources, e.g.
 * `assertEquals("", MotionLint.report(MotionLint.lintModule(File("."))))` (unit tests run in the module dir).
 */
object MotionLint {
    const val ALLOW_MARKER: String = "motion-lint: allow"

    private val tweenLiteral = Regex("""\btween\s*(?:<[^>()]*>)?\s*\(\s*(?:durationMillis\s*=\s*)?([0-9][0-9_]*)""")
    private val durationLiteral = Regex("""\b(?:durationMillis|delayMillis)\s*=\s*([0-9][0-9_]*)""")
    private val animateItemEmpty = Regex("""\banimateItem\s*\(\s*\)""")
    private val snapFling = Regex("""\brememberSnapFlingBehavior\s*\(""")
    private val cubic = Regex("""\bCubicBezierEasing\s*\(""")
    private val crossfade = Regex("""\bCrossfade\s*\(""")

    /**
     * Lints one source text. [allowEasingDefinitions] is true only for the design system, where the curves
     * are defined.
     */
    fun lint(name: String, source: String, allowEasingDefinitions: Boolean = false): List<MotionIssue> {
        val code = blankCommentsAndStrings(source)
        val rawLines = source.lines()
        val issues = mutableListOf<Pair<Int, MotionRule>>()
        tweenLiteral.findAll(code).forEach { m -> if (nonZero(m.groupValues[1])) issues += m.range.first to MotionRule.TWEEN_LITERAL }
        durationLiteral.findAll(code).forEach { m ->
            if (nonZero(m.groupValues[1]) && issues.none { it.first <= m.range.first && lineOf(code, it.first) == lineOf(code, m.range.first) && it.second == MotionRule.TWEEN_LITERAL }) {
                issues += m.range.first to MotionRule.DURATION_LITERAL
            }
        }
        animateItemEmpty.findAll(code).forEach { issues += it.range.first to MotionRule.ANIMATE_ITEM_DEFAULT }
        snapFling.findAll(code).forEach { issues += it.range.first to MotionRule.DEFAULT_SNAP_FLING }
        if (!allowEasingDefinitions) cubic.findAll(code).forEach { issues += it.range.first to MotionRule.CUBIC_BEZIER_OUTSIDE_DS }
        crossfade.findAll(code).forEach { m ->
            val args = balancedArgs(code, m.range.last) ?: return@forEach
            if (!args.contains("animationSpec")) issues += m.range.first to MotionRule.CROSSFADE_WITHOUT_SPEC
        }
        return issues
            .map { (offset, rule) -> lineOf(code, offset) to rule }
            .distinct()
            .filterNot { (line, _) -> allowed(rawLines, line) }
            .sortedBy { it.first }
            .map { (line, rule) -> MotionIssue(name, line, rule, rawLines.getOrElse(line - 1) { "" }) }
    }

    /** The `.kt` files under [root] that [lintDir] reads. */
    fun sourceFiles(root: File): List<File> =
        if (!root.exists()) emptyList() else root.walkTopDown().filter { it.isFile && it.extension == "kt" }.sortedBy { it.path }.toList()

    /** Lints every `.kt` file under [root] (recursively); file names are reported relative to [root]. */
    fun lintDir(root: File, allowEasingDefinitions: Boolean = false): List<MotionIssue> =
        sourceFiles(root).flatMap { f -> lint(f.relativeTo(root).path, f.readText(), allowEasingDefinitions) }

    /**
     * Lints `src/main` of the module at [moduleDir] (a unit test's working directory is its module). Throws
     * when there is no Kotlin source there, so a wrong working directory cannot pass silently.
     */
    fun lintModule(moduleDir: File = File("."), allowEasingDefinitions: Boolean = false): List<MotionIssue> {
        val root = File(moduleDir, "src/main")
        check(sourceFiles(root).isNotEmpty()) { "MotionLint: no Kotlin sources under ${root.absolutePath}" }
        return lintDir(root, allowEasingDefinitions)
    }

    /** One line per issue; empty when clean (handy as an assertion message). */
    fun report(issues: List<MotionIssue>): String = issues.joinToString("\n")

    private fun nonZero(digits: String): Boolean = digits.replace("_", "").trimStart('0').isNotEmpty()

    private fun allowed(lines: List<String>, line: Int): Boolean {
        val here = lines.getOrNull(line - 1).orEmpty()
        val above = lines.getOrNull(line - 2).orEmpty()
        return here.contains(ALLOW_MARKER) || above.contains(ALLOW_MARKER)
    }

    private fun lineOf(text: String, offset: Int): Int {
        var line = 1
        for (i in 0 until offset.coerceAtMost(text.length)) if (text[i] == '\n') line++
        return line
    }

    /** The text between the `(` at [openIndex] and its matching `)`, or null when unbalanced. */
    private fun balancedArgs(text: String, openIndex: Int): String? {
        var depth = 0
        for (i in openIndex until text.length) {
            when (text[i]) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) return text.substring(openIndex + 1, i)
                }
            }
        }
        return null
    }

    /**
     * Replaces comments and the contents of string / char literals with spaces, keeping newlines, so offsets
     * and line numbers stay valid. `${…}` templates inside strings are blanked too (they rarely hold motion).
     */
    internal fun blankCommentsAndStrings(src: String): String {
        val out = StringBuilder(src.length)
        var i = 0
        val n = src.length
        fun keepNewlines(from: Int, to: Int) {
            for (k in from until to) out.append(if (src[k] == '\n') '\n' else ' ')
        }
        while (i < n) {
            val c = src[i]
            when {
                c == '/' && i + 1 < n && src[i + 1] == '/' -> {
                    val end = src.indexOf('\n', i).let { if (it < 0) n else it }
                    keepNewlines(i, end)
                    i = end
                }
                c == '/' && i + 1 < n && src[i + 1] == '*' -> {
                    var depth = 0
                    var j = i
                    while (j < n) {
                        if (j + 1 < n && src[j] == '/' && src[j + 1] == '*') { depth++; j += 2; continue }
                        if (j + 1 < n && src[j] == '*' && src[j + 1] == '/') { depth--; j += 2; if (depth == 0) break; continue }
                        j++
                    }
                    keepNewlines(i, j.coerceAtMost(n))
                    i = j.coerceAtMost(n)
                }
                c == '"' && src.startsWith("\"\"\"", i) -> {
                    val end = src.indexOf("\"\"\"", i + 3).let { if (it < 0) n else it + 3 }
                    out.append("\"\"\"")
                    keepNewlines(i + 3, (end - 3).coerceAtLeast(i + 3))
                    if (end - 3 >= i + 3) out.append("\"\"\"")
                    i = end
                }
                c == '"' || c == '\'' -> {
                    var j = i + 1
                    while (j < n && src[j] != c && src[j] != '\n') {
                        if (src[j] == '\\') j++
                        j++
                    }
                    val end = (j + 1).coerceAtMost(n)
                    out.append(c)
                    keepNewlines(i + 1, (end - 1).coerceAtLeast(i + 1))
                    if (end - 1 > i) out.append(src[end - 1])
                    i = end
                }
                else -> {
                    out.append(c)
                    i++
                }
            }
        }
        return out.toString()
    }
}
