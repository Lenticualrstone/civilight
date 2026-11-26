package io.github.lenticualrstone.civilight.domain.world.common

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Keys

/**
 * What every world edit needs from outside: new keys, the clock and the device id written to
 * `updatedBy`. Tests pass deterministic ones.
 */
data class EditContext(
    val newKey: () -> Key = Keys::newKey,
    val now: () -> Long = System::currentTimeMillis,
    val device: String = "",
)

/** Separators and marks used in world copy (never an em dash). */
object Copy {
    const val SEP = " · "
    const val MINUS = "−"

    /** (2026-11-26) Place card `소속 세력 및 인물`, faction card `소속 인물 · 거점` (board `ImmPlaceRules`). */
    object Affiliation {
        const val TITLE = "소속 세력 및 인물"
        const val MEMBERS = "소속 인물"
        const val BASE = "거점"
        const val INCLUDE_SUB_PLACES = "하위 지역 포함"
        const val REORDER = "순서 바꾸기"
        const val REORDER_DONE = "완료"
        const val MOVE_UP = "위로 옮기기"
        const val MOVE_DOWN = "아래로 옮기기"
        const val PICK_BASIS = "소속 기준 고르기"
        const val LOOSE = "무소속"
        const val UNLOCK = "지문으로 보기"
        const val HIDDEN_FACTION = "숨긴 세력"
        const val HIDDEN_CHARACTER = "숨긴 인물"
        const val HIDDEN_PLACE = "숨긴 장소"
        const val DELETED_FACTION = "지운 세력"
        const val DELETED_PLACE = "지운 장소"
        const val NATIONALITY = "국적"
        const val RESIDENCE = "거주"
        const val SLOT_AFFILIATION = "소속"

        /** Rule 20 actions on a dotted reference. */
        val DELETED_ACTIONS = listOf("바꾸기", "되살리기", "이대로 두기")

        // Undo step labels.
        const val UNDO_FACTION_ORDER = "세력 순서 바꿈"
        const val UNDO_MEMBER_ORDER = "인물 순서 바꿈"
        const val UNDO_LOOSE_ORDER = "무소속 순서 바꿈"
        const val UNDO_BASIS = "묶음 기준 바꿈"
        const val UNDO_BASE = "거점 바꿈"

        /** `세력 2 · 인물 5`. */
        fun header(factions: Int, characters: Int): String = "세력 $factions${SEP}인물 $characters"

        /** `인물 3` (collapsed faction row). */
        fun count(characters: Int): String = "인물 $characters"

        /** `2곳` (a person in several factions here). */
        fun multi(n: Int): String = "${n}곳"

        /** `시점 1-5 · 소속 기준 ‘소속’`. */
        fun basisLine(lens: String, fieldName: String): String = "시점 $lens${SEP}소속 기준 ‘$fieldName’"
    }
}
