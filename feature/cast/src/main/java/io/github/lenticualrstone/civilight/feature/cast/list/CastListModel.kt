package io.github.lenticualrstone.civilight.feature.cast.list

import io.github.lenticualrstone.civilight.domain.cast.CharacterEdits
import io.github.lenticualrstone.civilight.domain.cast.list.CastList
import io.github.lenticualrstone.civilight.domain.cast.list.CastListView
import io.github.lenticualrstone.civilight.feature.cast.common.castLens
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc

/**
 * What the user chose on the list screen: the tag chip (CST-02), the typed query, and whether the
 * command bar is in its typing state. View modes `목록 | 관계도` (CST-33) and `목록 | 카드` (CST-04)
 * join this state in later batches.
 */
data class CastListUi(
    val selectedTag: Key? = null,
    val query: String = "",
    val searching: Boolean = false,
)

/** The list screen at one moment: the domain view plus what the screen needs around it. */
data class CastListState(
    val view: CastListView,
    val readOnly: Boolean,
    /** The tag chip in effect (a chip whose tag was removed falls back to `전체`). */
    val selectedTag: Key?,
)

/** Pure builder of the list screen (CST-01, CST-02) over `domain:cast` [CastList]. */
object CastListModel {
    fun build(doc: ProjectDoc, ui: CastListUi, hiddenUnlocked: Boolean, readOnly: Boolean): CastListState {
        val tag = ui.selectedTag?.takeIf { key -> CharacterEdits.tags(doc).any { it.key == key } }
        val query = if (ui.searching) ui.query.nfc() else ""
        val view = CastList.build(doc, doc.castLens, tag, query, hiddenUnlocked)
        return CastListState(view, readOnly, tag)
    }

    /** Tapping a chip: the tag, or `전체` (null); one chip at a time (CST-02). */
    fun selectTag(ui: CastListUi, tagKey: Key?): CastListUi = ui.copy(selectedTag = tagKey)

    /** Leaving the typing state clears the query, so the full list returns. */
    fun stopSearch(ui: CastListUi): CastListUi = ui.copy(searching = false, query = "")
}
