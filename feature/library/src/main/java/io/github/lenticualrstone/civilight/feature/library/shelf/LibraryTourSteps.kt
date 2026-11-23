package io.github.lenticualrstone.civilight.feature.library.shelf

import io.github.lenticualrstone.civilight.model.Key

/**
 * The `서재 · 숨기기` tour's targets (LIB-19; copy in `ModuleTours`): which control each step's hole
 * goes around, so no card floats with nothing marked (emulator QA 2026-11-23, f-07). Each step has one
 * own target and, while that is missing or off screen, the 설정 button as its fallback (always in the
 * header): an empty 서재 or one without hidden projects still points somewhere the step talks about.
 */
internal object LibraryTourSteps {
    /** `프로젝트 숨기기`: the ⋯ of the first project row (list) or the first project card (cards). */
    const val HIDE: Int = 0

    /** `숨긴 프로젝트`: the hidden group (lock row or open group; lock card in the card view). */
    const val HIDDEN_GROUP: Int = 1

    /** `설정 › 숨기기`: the 설정 button of the header. */
    const val SETTINGS: Int = 2

    /** Steps whose fallback is the 설정 button (every step but its own). */
    val SETTINGS_FALLBACK: List<Int> = listOf(HIDE, HIDDEN_GROUP)

    /**
     * The project whose ⋯ is the `프로젝트 숨기기` hole in the list view: the first shown row, else the
     * first row of the open hidden group; null when the 서재 shows no project (the 설정 fallback).
     */
    fun menuRow(rows: List<Key>, openGroupRows: List<Key>): Key? = rows.firstOrNull() ?: openGroupRows.firstOrNull()
}
