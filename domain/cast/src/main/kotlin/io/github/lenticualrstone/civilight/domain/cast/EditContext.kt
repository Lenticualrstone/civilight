package io.github.lenticualrstone.civilight.domain.cast

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Everything a mutating cast function needs from the outside world. Main code never reads a clock
 * or mints keys on its own, so every edit is reproducible in tests.
 */
data class EditContext(
    /** Time written to `updatedAt` / `createdAt`. */
    val now: Timestamp,
    /** Device id written to `updatedBy`. */
    val by: String,
    /** Zone of local dates (trash purge day). */
    val zone: ZoneId,
    /** Source of new ULID keys. */
    val newKey: () -> Key,
) {
    /** Local date of [now]. */
    val today: LocalDate get() = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
}

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
