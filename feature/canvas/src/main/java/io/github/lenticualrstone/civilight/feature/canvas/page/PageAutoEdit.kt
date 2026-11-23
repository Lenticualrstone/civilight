package io.github.lenticualrstone.civilight.feature.canvas.page

import io.github.lenticualrstone.civilight.model.Key

/**
 * Pages the page list just made with `만들고 열기` (U-15): the page screen reads the mark once and opens the
 * page straight into editing, as the head `+` did in 0.1.0 (CNV-14). Feature-local and in memory only: a
 * page made with `만들기` and opened later opens in the read view, and nothing is stored in the document.
 */
object JustCreatedPages {
    private val keys = mutableSetOf<Key>()

    @Synchronized
    fun mark(key: Key) {
        keys += key
    }

    /** True once for a page [mark]ed before; the mark is gone afterwards. */
    @Synchronized
    fun take(key: Key): Boolean = keys.remove(key)

    @Synchronized
    internal fun clear() = keys.clear()
}

/** When the page screen starts editing by itself (JVM-tested). */
object PageAutoEdit {
    /**
     * A page with no name and no text ([PageView.isBlank]) or one just made from the create card ([justCreated])
     * opens into editing, when it is open and writable.
     */
    fun shouldStart(view: PageView, justCreated: Boolean): Boolean =
        (view.isBlank || justCreated) && !view.readOnly && view.access == PageAccess.OPEN
}
