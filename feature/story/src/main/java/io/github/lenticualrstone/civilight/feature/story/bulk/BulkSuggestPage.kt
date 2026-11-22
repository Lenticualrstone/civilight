package io.github.lenticualrstone.civilight.feature.story.bulk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.BottomGlassBar
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.text.DecoratedText
import io.github.lenticualrstone.civilight.designsystem.text.searchHitDecorations
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.story.bulk.SuggestSheet
import io.github.lenticualrstone.civilight.domain.story.bulk.SuggestionCard
import io.github.lenticualrstone.civilight.domain.story.bulk.Suggestions
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.LocalSheets
import io.github.lenticualrstone.civilight.ui.scaffold.SheetHost

/** The `제안 받기` cards still to decide: the chosen scenes minus those already put in or skipped. */
object BulkSuggestModel {
    /** [locked]: a proposal naming a hidden place reads as `제안 없음` (R19 §6: the name never shows). */
    fun sheet(doc: ProjectDoc, selected: List<Key>, handled: Collection<Key>, locked: Boolean = false): SuggestSheet {
        val all = Suggestions(doc).sheet(selected)
        val cards = all.cards.filterNot { it.sceneKey in handled }.map { c ->
            val s = c.suggestion
            if (s == null || BulkModel.visible(doc, s.place?.ref, locked)) c
            else c.copy(header = c.header.substringBefore(" | ") + " | 제안 없음", suggestion = null)
        }
        return all.copy(cards = cards)
    }

    /** The scenes `제안 n개 모두 넣기` puts in. */
    fun withProposal(sheet: SuggestSheet): List<Key> = sheet.cards.filter { it.suggestion != null }.map { it.sceneKey }
}

/**
 * `제안 받기` (STO-34, board `BulkSuggest`): a page over 장소 · 때 한꺼번에 (head `장소 · 때 한꺼번에 · 고른
 * 씬 4개`, title `제안 받기`), the reading line, one card per chosen scene (header with the source, the
 * evidence in a `tint` box with the matched parts boxed, the proposal as `acc` outlined values, and
 * `넣기` · `고치기` · `건너뛰기`; or `제안 없음` with its line), and the bottom bar `닫기` · `제안 n개 모두 넣기`.
 * Nothing changes before `넣기` (SUG-05).
 */
@Composable
fun BulkSuggestPage(
    doc: ProjectDoc,
    selected: List<Key>,
    handled: List<Key>,
    readOnly: Boolean,
    locked: Boolean,
    sheets: SheetHost,
    localSheets: LocalSheets,
    onBack: () -> Unit,
    onPut: (Key) -> Unit,
    onFix: (Key) -> Unit,
    onSkip: (Key) -> Unit,
    onPutAll: (List<Key>) -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val sheet = remember(doc, selected, handled, locked) { BulkSuggestModel.sheet(doc, selected, handled, locked) }
    val withProposal = BulkSuggestModel.withProposal(sheet)
    CivilightScreen(
        title = sheet.title,
        smallLine = sheet.head,
        back = IconAction(BulkCopy.BACK, onBack),
        sheets = sheets,
        localSheets = localSheets,
        bottomBar = {
            BottomGlassBar(barHeight = 100.dp) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CivilightButton(BulkCopy.CLOSE, onBack, Modifier.weight(1f), variant = ButtonVariant.GHOST)
                    CivilightButton(sheet.applyAllLabel, { onPutAll(withProposal) }, Modifier.weight(1.6f), enabled = !readOnly && withProposal.isNotEmpty())
                }
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().background(colors.bg),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp),
        ) {
            item(key = "line") { CivilightText(sheet.line, type.small, Modifier.padding(bottom = 8.dp), color = colors.ink3) }
            items(sheet.cards, key = { it.sceneKey }) { card ->
                SuggestCard(card, readOnly, { onPut(card.sceneKey) }, { onFix(card.sceneKey) }, { onSkip(card.sceneKey) })
            }
        }
    }
}

@Composable
private fun SuggestCard(card: SuggestionCard, readOnly: Boolean, onPut: () -> Unit, onFix: () -> Unit, onSkip: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val (title, source) = card.header.split(" | ", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
    Column(Modifier.fillMaxWidth().topHairline(colors.line).padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            CivilightText(title, type.itemTitleCompact.merge(TextStyle(fontWeight = FontWeight.Bold)), Modifier.weight(1f), color = colors.ink)
            CivilightText(source, type.small, color = colors.ink3)
        }
        val suggestion = card.suggestion
        if (suggestion == null) {
            card.emptyLine?.let { CivilightText(it, type.small, color = colors.ink3) }
            return@Column
        }
        val evidence = suggestion.evidence
        val shape = RoundedCornerShape(10.dp)
        DecoratedText(
            AnnotatedString(evidence.text),
            type.body,
            Modifier.fillMaxWidth().clip(shape).background(colors.tint).padding(horizontal = 10.dp, vertical = 8.dp),
            color = colors.ink2,
            decorations = remember(evidence) { searchHitDecorations(evidence.text, evidence.hits) },
        )
        val parts = suggestion.line.split(" | ")
        ChipFlow {
            parts.forEachIndexed { i, part ->
                if (part.startsWith("·")) {
                    CivilightText(" $part", type.small, Modifier.align(Alignment.CenterVertically), color = colors.warn)
                } else {
                    ProposalValue(part, pin = i == 0 && suggestion.place != null)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CivilightButton(BulkCopy.PUT, onPut, variant = ButtonVariant.COMPACT_PRIMARY, enabled = !readOnly)
            CivilightButton(BulkCopy.FIX, onFix, variant = ButtonVariant.COMPACT_GHOST, enabled = !readOnly)
            CivilightButton(BulkCopy.SKIP, onSkip, variant = ButtonVariant.COMPACT_GHOST)
        }
    }
}

/** A proposed value: `acc` outlined, 28 tall; the place carries the pin icon. */
@Composable
private fun ProposalValue(text: String, pin: Boolean) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.clip(shape).insetOutline(colors.acc, 1.dp, shape).padding(horizontal = 12.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (pin) io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon(IconId.Pin, contentDescription = null, size = 14.dp, tint = colors.ink)
        CivilightText(text, CivilightTheme.type.chip, color = colors.ink, maxLines = 1)
    }
}
