package io.github.lenticualrstone.civilight.domain.cast

/**
 * Injected clock, device id and key source of every mutating cast function: the story domain's
 * context, so one value serves story, script and cast edits. Main code never reads a clock or mints
 * keys on its own.
 */
typealias EditContext = io.github.lenticualrstone.civilight.domain.story.scene.EditContext

/** Shared copy fragments of this area (UI copy, verbatim from 30-cast.md). */
object CastCopy {
    const val SEP = " · "
    const val UNKNOWN = "??"
    const val NOT_MET = "만나기 전"
    const val NO_MIND = "마음 적지 않음"
    const val NOT_WRITTEN = "적지 않음"
    const val PLANNED = "예정"
    const val AFTER_LENS = "지금 시점보다 뒤"
    const val HERE = "◆ 여기서"
    const val CHANGE_MARK = "◆"
    const val SOFT_MARK = "◇"
    const val WARN_MARK = "▲"
    const val HIDDEN_CHARACTER = "숨긴 인물"
    const val HIDDEN_ITEM = "숨긴 항목"
    const val NOT_YET_APPEARED = "아직 등장 전"
    const val NO_POSITION = "위치 없음"

    /** `a · b · c`, skipping blanks. */
    fun join(vararg parts: String?): String = joinAll(parts.toList())

    fun joinAll(parts: List<String?>): String = parts.filterNot { it.isNullOrBlank() }.joinToString(SEP)
}
