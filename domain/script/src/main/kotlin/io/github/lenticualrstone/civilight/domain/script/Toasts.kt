package io.github.lenticualrstone.civilight.domain.script

/**
 * Toast copy of script operations (SCR-15/37/38/40/52/59). Every toast has one action
 * [UNDO]; the strings here are the message part.
 */
object Toasts {
    const val UNDO = "되돌리기"
    const val GO_THERE = "가 보기"
    const val NOT_COPIED = "◆는 복사되지 않음"

    /** `◆ 3 · ◇ 1` + topic particle of the last count (`◆ 3 · ◇ 1은`). */
    private fun counts(diamonds: Int, reveals: Int): String =
        listOfNotNull(diamonds.takeIf { it > 0 }?.let { "◆ $it" }, reveals.takeIf { it > 0 }?.let { "◇ $it" })
            .joinToString(" · ") + Labels.topic(if (reveals > 0) reveals else diamonds)

    /** Merge: `◆ 1개 #042로`; null when nothing moved. */
    fun recordsTo(diamonds: Int, reveals: Int, target: String): String? = when {
        diamonds > 0 && reveals > 0 -> "◆ $diamonds · ◇ $reveals ${target}로"
        diamonds > 0 -> "◆ ${diamonds}개 ${target}로"
        reveals > 0 -> "◇ ${reveals}개 ${target}로"
        else -> null
    }

    /**
     * Block delete (SCR-38). [single] = the number label when one block went to the trash, else null
     * and [count] blocks. [target] = where the records went (`#044`, `예정` when null).
     */
    fun blocksDeleted(count: Int, single: String?, diamonds: Int, reveals: Int, target: String?): String {
        val head = if (single != null) "블록 $single 지움" else "블록 ${count}개 지움"
        if (diamonds == 0 && reveals == 0) return head
        val where = if (single == null) "다음 자리로" else if (target == null) "예정으로" else "$target 자리로"
        val records = when {
            diamonds > 0 && reveals > 0 -> "${counts(diamonds, reveals)} $where"
            diamonds > 0 -> if (single != null) "◆ ${diamonds}개는 $where" else "◆ ${diamonds}개 $where"
            else -> if (single != null) "◇ ${reveals}개는 $where" else "◇ ${reveals}개 $where"
        }
        return "$head · $records"
    }

    /** Block move: `◆ 1개 씬 3 → 씬 4` / `◆ 1개 1-7 → 1-8`; null without ◆. */
    fun moved(diamonds: Int, from: String, to: String): String? =
        diamonds.takeIf { it > 0 }?.let { "◆ ${it}개 $from → $to" }

    /** Reveal moved behind uses: `◇ 첫 공개 1-7 → 1-8 · ▲ 새로 2`. */
    fun revealMoved(from: String, to: String, newUses: Int): String = "◇ 첫 공개 $from → $to · ▲ 새로 $newUses"

    /** Scene delete (SCR-52): `씬 3 지움 · ◆ 3 · ◇ 1은 씬 4 처음으로`; [next] null = `예정으로`. */
    fun sceneDeleted(ordinal: Int, diamonds: Int, reveals: Int, next: String?): String {
        val head = "씬 $ordinal 지움"
        if (diamonds == 0 && reveals == 0) return head
        return "$head · ${counts(diamonds, reveals)} " + (next?.let { "$it 처음으로" } ?: "예정으로")
    }

    /** Recorder insert (SCR-59): `1-7 씬 3 · #058 작가 메모 넣음`. */
    fun recorderInserted(location: String, number: String): String = "$location · $number 작가 메모 넣음"
}
