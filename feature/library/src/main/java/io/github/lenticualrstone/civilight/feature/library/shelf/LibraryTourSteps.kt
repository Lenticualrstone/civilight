package io.github.lenticualrstone.civilight.feature.library.shelf

import io.github.lenticualrstone.civilight.model.Key

/**
 * The `서재 · 숨기기` tour's targets (LIB-19; copy in `ModuleTours`): which control each step's hole
 * goes around, so no card floats with nothing marked (emulator QA 2026-11-23, f-07). Each step has one
 * own target and, while that is missing or off screen, the 설정 button as its fallback (always in the
 * header): a 서재 without hidden projects still points somewhere the step talks about. A 서재 that shows
 * no project has no ⋯ to talk about, so `프로젝트 숨기기` is left out there ([absent]; tour QA 2026-11-23:
 * with 0 projects all three cards pointed at 설정).
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
     * first row of the open hidden group; null when the 서재 shows no project (the step is [absent]).
     */
    fun menuRow(rows: List<Key>, openGroupRows: List<Key>): Key? = rows.firstOrNull() ?: openGroupRows.firstOrNull()

    /**
     * The steps the 서재 has nothing for, given its [menuRow]: `프로젝트 숨기기` when no project row or card
     * is shown (the card view shows the same projects as the list). Never more, so the tour always runs.
     */
    fun absent(menuRow: Key?): Set<Int> = if (menuRow == null) setOf(HIDE) else emptySet()
}
