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

    // (2026-11-26) Tag scope (board ImmTermTagSheet): `범위` row of the new-tag card and the tag rows.
    const val TAG_SCOPE = "범위"
    const val TAG_SCOPE_ALL = "모든 항목"
    const val TAG_SCOPE_ALL_SUB = "인물 · 장소 · 용어 · 세력"
    const val TAG_SCOPE_DEFAULT = "기본"
    const val TAG_SCOPE_WIDEN_ONLY = "모든 항목으로 만든 태그는 나중에 범위를 줄일 수 없음 · 넓히기만 가능"
    const val TAG_SCOPE_NARROW_REFUSED = "범위는 넓히기만 가능"
    const val TAG_WIDEN = "모든 항목으로 넓히기"
    const val TAG_USE_EXISTING = "이 태그 쓰기"

    /** `용어 전용` (the card kind's own scope); `모든 항목` for all. */
    fun tagScopeLabel(scope: io.github.lenticualrstone.civilight.model.cast.TagScope): String =
        if (scope == io.github.lenticualrstone.civilight.model.cast.TagScope.ALL) TAG_SCOPE_ALL else "${tagKindWord(scope)} 전용"

    /** Short scope word of a tag row (`용어` · `전체`, board ImmTermTagSheet). */
    fun tagScopeShort(scope: io.github.lenticualrstone.civilight.model.cast.TagScope): String =
        if (scope == io.github.lenticualrstone.civilight.model.cast.TagScope.ALL) "전체" else tagKindWord(scope)

    /** `인물 · 용어 · 장소 · 세력` of a tag scope that is one kind. */
    fun tagKindWord(scope: io.github.lenticualrstone.civilight.model.cast.TagScope): String = when (scope) {
        io.github.lenticualrstone.civilight.model.cast.TagScope.CHARACTER -> "인물"
        io.github.lenticualrstone.civilight.model.cast.TagScope.TERM -> "용어"
        io.github.lenticualrstone.civilight.model.cast.TagScope.PLACE -> "장소"
        io.github.lenticualrstone.civilight.model.cast.TagScope.FACTION -> "세력"
        io.github.lenticualrstone.civilight.model.cast.TagScope.ALL -> "전체"
    }

    /** The name is taken by a live tag (any scope): `같은 이름 태그 · 광물 · 용어`. */
    fun tagNameTaken(name: String, scope: io.github.lenticualrstone.civilight.model.cast.TagScope?): String =
        joinAll(listOf("같은 이름 태그", name, scope?.let(::tagScopeShort)))

    /** `a · b · c`, skipping blanks. */
    fun join(vararg parts: String?): String = joinAll(parts.toList())

    fun joinAll(parts: List<String?>): String = parts.filterNot { it.isNullOrBlank() }.joinToString(SEP)
}
