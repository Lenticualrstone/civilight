package io.github.lenticualrstone.civilight.feature.canvas.page

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.MenuDeleteRow
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.text.DocBlockKind
import io.github.lenticualrstone.civilight.navigation.SheetKey
import kotlinx.serialization.Serializable

/** `⋯` of a page (Q-PG-02: contents not drawn; see Defaults used). */
@Serializable
data class PageMenuSheet(val projectKey: Key, val pageKey: Key) : SheetKey

/** `/로 블록 추가`: the block kinds of a page (`본문 · 소제목 · ⌁ 작가 메모`). */
@Serializable
data class BlockAddSheet(val projectKey: Key, val pageKey: Key) : SheetKey

/** The card of a link to a deleted item (rule 20 §2 row `@연결`). */
@Serializable
data class DeletedLinkSheet(val projectKey: Key, val pageKey: Key, val linkKey: Key) : SheetKey

/** `다른 항목에 잇기`: the link picker stacked over [DeletedLinkSheet]. */
@Serializable
data class RelinkSheet(val projectKey: Key, val pageKey: Key, val linkKey: Key) : SheetKey

/** One action of a page menu or card; a [destructive] one (`지우기`) is drawn last as the shared `⋯` delete row. */
data class SheetAction(val label: String, val onClick: () -> Unit, val dotted: Boolean = false, val destructive: Boolean = false)

/** A sheet of [actions] under a header ([title], [subtitle]): rows that read as buttons. */
@Composable
fun ColumnScope.ActionRows(title: String, subtitle: String?, actions: List<SheetAction>, onClose: () -> Unit, titleNote: String? = null) {
    SheetHeader(title = title, subtitle = subtitle, onClose = onClose, closeLabel = PageCopy.BTN_CLOSE)
    titleNote?.let { ListRow(title = it, compact = true, titleDotted = true, modifier = Modifier.padding(top = 8.dp)) }
    val (deletes, rest) = actions.partition { it.destructive }
    rest.forEachIndexed { i, a ->
        ListRow(title = a.label, onClick = a.onClick, compact = true, modifier = if (i == 0) Modifier.padding(top = 8.dp) else Modifier)
    }
    // `지우기` is always last, under its own divider, in `warn` with the trash icon (0.5 interface review).
    deletes.forEach { MenuDeleteRow(it.label, it.onClick) }
}

/** The block kinds `/` adds, in the order of the PageBlock table. */
val PageBlockKinds: List<Pair<DocBlockKind, String>> = listOf(
    DocBlockKind.PARAGRAPH to PageCopy.BLOCK_PARAGRAPH,
    DocBlockKind.HEADING to PageCopy.BLOCK_HEADING,
    DocBlockKind.MEMO to PageCopy.BLOCK_MEMO,
)
