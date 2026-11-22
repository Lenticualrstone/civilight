package io.github.lenticualrstone.civilight.feature.library.backup

import io.github.lenticualrstone.civilight.domain.library.crypto.RecoveryKeys
import io.github.lenticualrstone.civilight.domain.library.hiding.HiddenItems
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/** The 4-bar meter under `복구 암호`: [bars] lit of 4 and `강도 · <word> · <n>자`. */
data class StrengthUi(val bars: Int, val line: String)

/** A `label · value` row of `함께 잠금`. */
data class RecoveryRow(val label: String, val value: String)

/** 복구 암호 정하기 (LIB-58, board `RHRecoveryPw`). */
data class RecoveryPasswordUi(
    /** `숨긴 프로젝트 <n>` on the card. */
    val cardTitle: String,
    val strength: StrengthUi?,
    /** Shown under `한 번 더` once something typed there differs. */
    val mismatch: String?,
    val rows: List<RecoveryRow>,
    val canSet: Boolean,
)

/** Pure rules of 복구 암호 정하기 (LIB-58). */
object RecoveryPasswordState {
    /**
     * Strength (`ED`; only `좋음` is in the sources, drawn for 14 characters with 3 bars): one bar
     * below the 8-character minimum, two for 8 … 11, three from 12, four from 16 with three kinds of
     * characters (letters, digits, others). A password of one kind of character loses a bar.
     */
    fun strength(password: CharArray): StrengthUi? {
        if (password.isEmpty()) return null
        val length = password.size
        val kinds = listOf(
            password.any { it.isLetter() },
            password.any { it.isDigit() },
            password.any { !it.isLetterOrDigit() },
        ).count { it }
        var bars = when {
            length < RecoveryKeys.MIN_LENGTH -> 1
            length < 12 -> 2
            length < 16 || kinds < 3 -> 3
            else -> 4
        }
        if (kinds <= 1 && bars > 1) bars--
        val word = when (bars) {
            1 -> RecoveryCopy.TODO_COPY_LIB58_STRENGTH_WEAK
            2 -> RecoveryCopy.TODO_COPY_LIB58_STRENGTH_FAIR
            3 -> RecoveryCopy.STRENGTH_GOOD
            else -> RecoveryCopy.TODO_COPY_LIB58_STRENGTH_STRONG
        }
        return StrengthUi(bars, "${RecoveryCopy.STRENGTH} · $word · $length${RecoveryCopy.CHARS}")
    }

    /**
     * The screen. [visibleDocs] are the live projects that are not hidden: their hidden items are
     * locked together (`숨긴 항목 <n>` · names). Hidden projects are only counted, never named.
     */
    fun build(password: CharArray, repeat: CharArray, hiddenProjects: Int, visibleDocs: List<ProjectDoc>): RecoveryPasswordUi {
        val withItems = visibleDocs.map { it to HiddenItems(it).count }.filter { it.second > 0 }
        val rows = buildList {
            if (withItems.isNotEmpty()) {
                add(RecoveryRow("${RecoveryCopy.HIDDEN_ITEMS} ${withItems.sumOf { it.second }}", withItems.joinToString(" · ") { it.first.project.name }))
            }
            add(RecoveryRow(RecoveryCopy.RESTORE_WHEN, RecoveryCopy.RESTORE_WHEN_VALUE))
        }
        return RecoveryPasswordUi(
            cardTitle = "${RecoveryCopy.HIDDEN_PROJECTS} $hiddenProjects",
            strength = strength(password),
            mismatch = if (repeat.isNotEmpty() && !password.contentEquals(repeat)) RecoveryCopy.TODO_COPY_LIB58_MISMATCH else null,
            rows = rows,
            canSet = RecoveryKeys.canSet(password, repeat),
        )
    }
}
