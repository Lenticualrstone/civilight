package io.github.lenticualrstone.civilight.feature.script.links

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.Popover
import io.github.lenticualrstone.civilight.designsystem.component.PopoverCard
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.world.term.CandidatePopover
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.picker.HiddenGate
import io.github.lenticualrstone.civilight.ui.picker.LinkPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.PickKind
import kotlinx.serialization.Serializable

/** Copy of the link card and the undo-step names of the link and candidate edits (SCR-55, SCR-56). */
object LinkCopy {
    const val RELINK = "다른 항목에 잇기"
    const val UNLINK = "글로 풀기"
    const val RESTORE = "되살리기"
    const val KEEP = "이대로 두기"
    const val LINK = "잇기"
    const val REJECT = "아님"

    /** Title of the picker behind `다른 항목에 잇기` (the button's own words). */
    const val PICK_TITLE = RELINK

    val all: List<String> = listOf(RELINK, UNLINK, RESTORE, KEEP, LINK, REJECT)
    val buttons: List<String> = listOf(RELINK, UNLINK, RESTORE, KEEP, LINK, REJECT)
}

/** The card of a link whose target is deleted (SCR-55, rule 20): [linkKey] in [blockKey]'s text. */
@Serializable
data class LinkCardSheetKey(val projectKey: Key, val blockKey: Key, val linkKey: Key) : SheetKey

/** What the card's buttons do. */
class LinkCardActions(
    val relink: (kind: PickKind, key: Key) -> Unit,
    val unlink: () -> Unit,
    val restore: (kind: LinkTargetKind?, key: Key) -> Unit,
    val keep: (key: Key) -> Unit,
)

/**
 * `지운 인물 · 노아` (or `영구 삭제 · 노아`) with `다른 항목에 잇기 · 글로 풀기 · 되살리기 · 이대로 두기`;
 * `되살리기` is gone after permanent deletion and `이대로 두기` once the reference is kept. A
 * read-only project shows the card only. `다른 항목에 잇기` swaps the sheet for the `@` picker.
 */
@Composable
fun SheetScope.LinkCardSheet(key: LinkCardSheetKey, doc: ProjectDoc, gate: HiddenGate, readOnly: Boolean, actions: LinkCardActions) {
    var picking by rememberSaveable { mutableStateOf(false) }
    if (picking) {
        LinkPickerSheet(
            doc = doc,
            gate = gate,
            title = LinkCopy.PICK_TITLE,
            onPick = { kind, target, _ ->
                actions.relink(kind, target)
                close()
            },
            onClose = { picking = false },
        )
        return
    }
    val card = LinkFixes.card(doc, key.blockKey, key.linkKey, gate.open)
    SheetHeader(title = card?.title.orEmpty(), onClose = ::close)
    if (card == null || readOnly) return
    val kept = LinkFixes.isKept(doc, card.targetKey, key.blockKey)
    val host = this
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        CardButton(LinkCopy.RELINK) { picking = true }
        CardButton(LinkCopy.UNLINK) {
            actions.unlink()
            host.close()
        }
        if (card.canRestore) {
            CardButton(LinkCopy.RESTORE) {
                actions.restore(card.targetKind, card.targetKey)
                host.close()
            }
        }
        if (!kept) {
            CardButton(LinkCopy.KEEP) {
                actions.keep(card.targetKey)
                host.close()
            }
        }
    }
}

@Composable
private fun CardButton(label: String, onClick: () -> Unit) {
    CivilightButton(
        label, onClick,
        Modifier.fillMaxWidth().topHairline(CivilightTheme.colors.line).padding(vertical = 6.dp),
        variant = ButtonVariant.GHOST,
    )
}

/**
 * The small window under a tapped candidate (board `RTCandidate`, DS-26): the term and its tag
 * (`후보 · 끊김`), `첫 공개 1-7 · 잇기 시 ▲ 공개 전 사용` when linking would be a pre-reveal use, then
 * `잇기` (primary) · `아님` (ghost). Read-only projects show the window without the buttons.
 */
@Composable
fun CandidateWindow(popover: CandidatePopover, readOnly: Boolean, onLink: () -> Unit, onReject: () -> Unit, onDismiss: () -> Unit) {
    val line = listOfNotNull(popover.revealLine, popover.warning).joinToString(" ").ifEmpty { null }
    Popover(onDismissRequest = onDismiss) {
        PopoverCard(
            title = popover.title,
            tag = popover.tag,
            line = line,
            actions = if (readOnly) null else ({
                CivilightButton(popover.linkLabel, onLink, Modifier.weight(1f))
                CivilightButton(popover.rejectLabel, onReject, Modifier.weight(1f), variant = ButtonVariant.GHOST)
            }),
        )
    }
}
