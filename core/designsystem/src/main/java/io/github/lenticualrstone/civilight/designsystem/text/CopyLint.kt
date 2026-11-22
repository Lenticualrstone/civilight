package io.github.lenticualrstone.civilight.designsystem.text

/** DS-62 / DS-63 copy-lint findings. */
enum class LintIssue {
    /** `·` without a space on both sides. */
    UNSPACED_DOT,

    /** Em dash between words. */
    EM_DASH_SEPARATOR,

    /** ` / ` listing words. */
    SLASH_LIST,

    /** A short label listing fragments with commas. */
    COMMA_LIST,

    /** Emoji or a forbidden symbol. */
    SYMBOL,

    /** A button whose whole label is `확인`. */
    LONE_CONFIRM,

    /** A word replaced by the vocabulary (DS-63). */
    VOCAB,
}

/** A banned word and the allowed phrases containing it (DS-63 allowlist). */
private class VocabRule(banned: String, vararg allowed: String) {
    val banned = Regex(banned)
    val allowed = allowed.map(::Regex)

    fun violates(s: String): Boolean {
        val stripped = allowed.fold(s) { acc, r -> r.replace(acc, "\u0000") }
        return banned.containsMatchIn(stripped)
    }
}

/**
 * The UI-copy lint (DS-62 rules 1–4 and DS-63). Story content (sample dialogue, example project text)
 * is not UI copy and must not be passed in. [isButton] marks a button label.
 */
object CopyLint {
    private val unspacedDot = Regex("""\S·|·\S""")

    /** DS-62 states `\p{L} ?— ?\p{L}`; digits are included so `R-15 — 기본` is caught as T-DS-24 expects. */
    private val emDash = Regex("""[\p{L}\p{N}] ?— ?[\p{L}\p{N}]""")
    private val slashList = Regex("""(?<![\d/])\s/\s(?!\d)""")
    private val forbiddenSymbols = setOf(0x2713, 0x2605, 0x25CF, 0x25CB, 0x25A0, 0x25A1, 0x25CE)

    private val vocab = listOf(
        VocabRule(
            "삭제",
            "영구 삭제",
            """\d+일 뒤 (자동 )?삭제""",
            "온보딩 후 삭제",
            "예시 프로젝트 삭제됨",
            "‘애니메이션 삭제’",
        ),
        VocabRule("장면", "막 › 장면"),
        VocabRule("복원"),
        VocabRule("백업에서 가져오기"),
        VocabRule("미지정"),
        VocabRule("선택", "선택지", """\(선택\)""", "홀로 선택"),
        VocabRule("변경"),
        VocabRule("수정", "마지막 수정", """\d{1,2}:\d{2} 수정"""),
        VocabRule("넘기기"),
        VocabRule("미리보기"),
        VocabRule("접힌 화면"),
        VocabRule("지문으로만"),
        VocabRule("붙일 곳"),
        VocabRule("씬 전환 블록"),
    )

    fun lint(value: String, isButton: Boolean = false): List<LintIssue> {
        val issues = mutableListOf<LintIssue>()
        if (unspacedDot.containsMatchIn(value)) issues += LintIssue.UNSPACED_DOT
        if (emDash.containsMatchIn(value)) issues += LintIssue.EM_DASH_SEPARATOR
        if (slashList.containsMatchIn(value)) issues += LintIssue.SLASH_LIST
        if (value.length < 40 && value.count { it == ',' } >= 2) issues += LintIssue.COMMA_LIST
        if (hasForbiddenSymbol(value)) issues += LintIssue.SYMBOL
        if (isButton && value.trim() == "확인") issues += LintIssue.LONE_CONFIRM
        if (vocab.any { it.violates(value) }) issues += LintIssue.VOCAB
        return issues
    }

    private fun hasForbiddenSymbol(s: String): Boolean {
        var i = 0
        while (i < s.length) {
            val cp = s.codePointAt(i)
            if (cp in forbiddenSymbols || isEmojiPresentation(cp)) return true
            i += Character.charCount(cp)
        }
        return false
    }

    /** Emoji_Presentation=Yes code points (Unicode 15 table, plane-1 blocks taken whole) and VS16. */
    fun isEmojiPresentation(cp: Int): Boolean {
        if (cp == 0xFE0F) return true
        if (cp in 0x1F000..0x1FAFF) {
            return cp == 0x1F004 || cp == 0x1F0CF || cp == 0x1F18E || cp in 0x1F191..0x1F19A ||
                cp in 0x1F1E6..0x1F1FF || cp == 0x1F201 || cp == 0x1F21A || cp == 0x1F22F ||
                cp in 0x1F232..0x1F236 || cp in 0x1F238..0x1F23A || cp in 0x1F250..0x1F251 ||
                cp in 0x1F300..0x1F64F || cp in 0x1F680..0x1F6FF || cp in 0x1F7E0..0x1F7F0 || cp in 0x1F90C..0x1FAFF
        }
        return BMP_EMOJI.any { cp in it }
    }

    private val BMP_EMOJI: List<IntRange> = listOf(
        0x231A..0x231B, 0x23E9..0x23EC, 0x23F0..0x23F0, 0x23F3..0x23F3, 0x25FD..0x25FE, 0x2614..0x2615,
        0x2648..0x2653, 0x267F..0x267F, 0x2693..0x2693, 0x26A1..0x26A1, 0x26AA..0x26AB, 0x26BD..0x26BE,
        0x26C4..0x26C5, 0x26CE..0x26CE, 0x26D4..0x26D4, 0x26EA..0x26EA, 0x26F2..0x26F3, 0x26F5..0x26F5,
        0x26FA..0x26FA, 0x26FD..0x26FD, 0x2705..0x2705, 0x270A..0x270B, 0x2728..0x2728, 0x274C..0x274C,
        0x274E..0x274E, 0x2753..0x2755, 0x2757..0x2757, 0x2795..0x2797, 0x27B0..0x27B0, 0x27BF..0x27BF,
        0x2B1B..0x2B1C, 0x2B50..0x2B50, 0x2B55..0x2B55,
    )

    /** One finding of a string resource. */
    data class ResourceFinding(val key: String, val value: String, val issue: LintIssue)

    private val stringEntry = Regex("""<string\s+name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)

    /**
     * Lints a `values/strings.xml` text. Keys starting `btn_` or `action_` are buttons; keys starting
     * `sample_` or `content_` hold story content and are skipped (DS-62 rule 8; proposed key naming).
     */
    fun lintStringsXml(xml: String): List<ResourceFinding> = stringEntry.findAll(xml).flatMap { m ->
        val key = m.groupValues[1]
        if (key.startsWith("sample_") || key.startsWith("content_")) return@flatMap emptySequence()
        val value = decodeXml(m.groupValues[2])
        val isButton = key.startsWith("btn_") || key.startsWith("action_")
        lint(value, isButton).asSequence().map { ResourceFinding(key, value, it) }
    }.toList()

    private fun decodeXml(raw: String): String = raw
        .replace("<![CDATA[", "").replace("]]>", "")
        .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'")
        .replace("&amp;", "&").replace("\\'", "'").replace("\\\"", "\"").replace("\\n", "\n")
        .trim()
}

/** T-DS-24 … T-DS-28 entry point. */
fun lintString(value: String, isButton: Boolean = false): List<LintIssue> = CopyLint.lint(value, isButton)
