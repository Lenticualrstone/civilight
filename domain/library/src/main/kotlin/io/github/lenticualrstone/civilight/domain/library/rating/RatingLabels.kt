package io.github.lenticualrstone.civilight.domain.library.rating

import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.project.Rating

/** Display words of 수위 등급 (LIB-22). Display words are never stored (§1.5). */
object RatingLabels {
    /** Badge text: `ALL`, `R-15`, `R-18`. */
    fun badge(rating: Rating): String = when (rating) {
        Rating.ALL -> "ALL"
        Rating.R15 -> "R-15"
        Rating.R18 -> "R-18"
    }

    /** Badge of a stored value; an unknown value shows its raw string. */
    fun badge(rating: OpenEnum<Rating>): String = rating.known?.let(::badge) ?: rating.raw

    /** Gloss under the picker cell: `12세 이하`, `15세 이상`, `성인`. */
    fun gloss(rating: Rating): String = when (rating) {
        Rating.ALL -> "12세 이하"
        Rating.R15 -> "15세 이상"
        Rating.R18 -> "성인"
    }

    /** Long form in menus: `ALL(R-12)`, `R-15`, `R-18`. */
    fun long(rating: Rating): String = if (rating == Rating.ALL) "ALL(R-12)" else badge(rating)

    /** The 서재 ⋯ menu row sub: `ALL(R-12) · R-15 · R-18 · 지금 <rating>` (LIB-02). */
    fun menuSub(current: Rating): String = "ALL(R-12) · R-15 · R-18 · 지금 ${badge(current)}"

    /** R-18 badges are red (`r18`); ALL and R-15 are outlined `ink2`. */
    fun isRed(rating: Rating): Boolean = rating == Rating.R18

    /** Masked project label for widgets, notifications and recents (LIB-27, §6 Q8: always this literal). */
    const val MASKED_PROJECT = "R-18 프로젝트"

    /** Rating of an unknown stored value, read as the strictest known one (`ED`). */
    fun effective(rating: OpenEnum<Rating>): Rating = rating.known ?: Rating.R18
}
