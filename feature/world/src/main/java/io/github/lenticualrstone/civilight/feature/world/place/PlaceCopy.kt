package io.github.lenticualrstone.civilight.feature.world.place

import io.github.lenticualrstone.civilight.domain.world.common.Copy
import io.github.lenticualrstone.civilight.domain.world.item.BasisRefusal
import io.github.lenticualrstone.civilight.domain.world.item.BasisSlot
import io.github.lenticualrstone.civilight.ui.reorder.ReorderLabels

/**
 * (2026-11-26) Copy of the immersive place and faction cards and of `묶음 기준` (boards `Immersive` kind place,
 * `ImmContent` place branch, `ImmPlaceRules`). Words the domain already builds (`세력 2 · 인물 5`, `2곳`, `국적 X ·
 * 거주 Y`, `시점 1-5 · 소속 기준 ‘소속’`, `숨긴 인물 1`) come from `Copy.Affiliation`. Labels and fragments only.
 */
object PlaceCopy {
    const val SEP = Copy.SEP

    // --- Tabs (board `Immersive` kind place) ---
    const val TAB_OVERVIEW = "개요"
    const val TAB_AFFILIATION = "소속"
    const val TAB_APPEARANCE = "등장"
    val TABS: List<String> = listOf(TAB_OVERVIEW, TAB_AFFILIATION, TAB_APPEARANCE)

    // --- 개요 ---
    const val SEC_DESCRIPTION = "설명"
    const val SEC_FIELDS = "세부 항목"
    const val SEC_SUB_PLACES = "하위 지역"
    const val PARENT = "상위 지역"
    const val PICK_IMAGE = "이미지 고르기"

    // --- 등장 ---
    const val SEC_TIME_HERE = "이곳의 시간"
    const val SEC_MAPS = "지도"

    // --- 소속 세력 및 인물 ---
    const val LOOSE_NOTE = "이곳 거주${SEP}거주 정보 없는 국적자${SEP}세력 없음"
    const val GRIP = "순서 손잡이"
    val REORDER_LABELS = ReorderLabels(Copy.Affiliation.MOVE_UP, Copy.Affiliation.MOVE_DOWN, GRIP)

    // --- 세력 카드 ---
    const val SEC_MEMBERS = Copy.Affiliation.MEMBERS
    const val BASE = Copy.Affiliation.BASE
    const val BTN_CLEAR = "비우기"

    // --- 묶음 기준 ---
    const val BASIS_TITLE = "묶음 기준"
    const val BASIS_PICK = "고르기"
    const val BASIS_NOTE = "인물 ‘연결’ 항목${SEP}안 고른 칸은 안 씀"
    const val FIELD_SETTINGS = "인물 항목"

    /** `하위 지역 · 3` (section label). */
    fun subPlacesTitle(count: Int): String = "$SEC_SUB_PLACES$SEP$count"

    /** `세부 항목 · 3`. */
    fun fieldsTitle(count: Int): String = "$SEC_FIELDS$SEP$count"

    /** `하위 지역 3` (subtitle part). */
    fun subPlaces(count: Int): String = "$SEC_SUB_PLACES $count"

    /** `상위 지역 오리지아` (subtitle part). */
    fun parent(name: String): String = "$PARENT $name"

    /** `거점 하층 거리`. */
    fun base(name: String): String = "$BASE $name"

    /** `무소속 · 2`. */
    fun looseTitle(count: Int): String = "${Copy.Affiliation.LOOSE}$SEP$count"

    /** Expand chevron of a faction row: `등불 결사 펼치기` / `등불 결사 접기`. */
    fun expandLabel(name: String, open: Boolean): String = if (open) "$name 접기" else "$name 펼치기"

    /** `소속 · 거주 · 국적` slot words. */
    fun slot(slot: BasisSlot): String = slot.label

    /** What the picker needs: `세력 연결` / `장소 연결`. */
    fun slotTarget(slot: BasisSlot): String = when (slot) {
        BasisSlot.AFFILIATION -> "세력 연결"
        BasisSlot.RESIDENCE, BasisSlot.NATIONALITY -> "장소 연결"
    }

    /** The picker has no usable field: `세력 연결 항목 없음`. */
    fun noChoices(slot: BasisSlot): String = "${slotTarget(slot)} 항목 없음"

    /** One terse line for a refused or no longer usable field. */
    fun refusal(reason: BasisRefusal, slot: BasisSlot): String = when (reason) {
        BasisRefusal.NO_FIELD -> "항목 없음"
        BasisRefusal.TRASHED -> "휴지통"
        BasisRefusal.NOT_CHARACTER -> "인물 항목 아님"
        BasisRefusal.ONE_CHARACTER -> "이 인물만 항목"
        BasisRefusal.NOT_LINK -> "연결 아님"
        BasisRefusal.WRONG_TARGET -> "${slotTarget(slot)} 아님"
    }
}
