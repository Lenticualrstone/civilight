package io.github.lenticualrstone.civilight.feature.world.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.FieldTableRow
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.LockRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.text.DecoratedText
import io.github.lenticualrstone.civilight.designsystem.text.MarkerKind
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.world.WorldCopy

/** Content padding of a card body under the glass bars (board: 20 at the sides). */
fun cardPadding(bars: PaddingValues): PaddingValues = PaddingValues(
    start = 20.dp,
    end = 20.dp,
    top = bars.calculateTopPadding(),
    bottom = bars.calculateBottomPadding() + 24.dp,
)

/**
 * The 16:9 image slot of a card (WLD-07 owns images): with no image, the initial-letter card. The
 * term card labels it `참고 이미지` (board `Term`).
 */
@Composable
fun CardArt(name: String, label: String?, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(shape)
            .background(colors.raise),
    ) {
        CivilightText(
            text = name.trim().take(1),
            style = type.hero,
            color = colors.ink3,
            modifier = Modifier.align(Alignment.Center),
        )
        if (label != null) {
            CivilightText(
                text = label,
                style = type.micro,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

/** Eyebrow (`.sec`), name (Myeongjo 800) and description with its links (WLD-08 body 2). */
@Composable
fun CardHead(head: CardHeadUi, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(modifier.fillMaxWidth()) {
        SectionLabel(head.eyebrow, Modifier.padding(top = 20.dp))
        CivilightText(head.name, type.pageTitle, Modifier.padding(top = 6.dp), color = colors.ink)
        CardDescription(head.description)
    }
}

/** The description paragraphs (Myeongjo body) with link decorations. */
@Composable
fun CardDescription(paragraphs: List<ParagraphUi>, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(modifier.fillMaxWidth()) {
        paragraphs.forEach { p ->
            DecoratedText(
                text = AnnotatedString(p.text),
                style = type.body,
                modifier = Modifier.padding(top = 8.dp),
                color = colors.ink,
                decorations = p.decorations,
            )
        }
    }
}

/** A fixed row of the field table (`첫 공개 | 1-7 씬 3 · #044`); editable rows open their editor. */
@Composable
fun FixedRow(row: FixedRowUi, onEdit: () -> Unit) {
    FieldTableRow(label = row.label, value = row.value, onClick = if (row.editable) onEdit else null)
}

/**
 * Project fields of a card (WLD-06): filled rows (`◆` when the value has history), then the line
 * `빈 항목 N · 보기` and `+ 항목`. [showEmpty] lists the empty fields too; every row opens the field
 * sheet (cast's `FieldValueEditSheet`, with `이 값 고치기 · 여기서부터 바뀜`).
 */
@Composable
fun FieldSection(
    section: FieldSectionUi,
    showEmpty: Boolean,
    readOnly: Boolean,
    onShowEmpty: () -> Unit,
    onEdit: (FieldRowUi) -> Unit,
    onAdd: () -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(Modifier.fillMaxWidth()) {
        val rows = if (showEmpty) section.filled + section.empty else section.filled
        rows.forEach { row ->
            FieldTableRow(
                label = row.label,
                value = row.value ?: WorldCopy.EMPTY_VALUE,
                marker = if (row.changed) MarkerKind.CHANGE else null,
                onClick = if (readOnly) null else ({ onEdit(row) }),
            )
        }
        val emptyLine = section.emptyLine
        if ((emptyLine != null && !showEmpty) || !readOnly) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .topHairline(colors.line)
                    .heightIn(min = 44.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (emptyLine != null && !showEmpty) {
                    TextAction(emptyLine, onShowEmpty, color = colors.ink3, style = type.small)
                } else {
                    Box(Modifier)
                }
                if (!readOnly) TextAction(WorldCopy.ADD_FIELD, onAdd, color = colors.acc, style = type.small)
            }
        }
    }
}

/** The whole card while its item is hidden and locked: `숨긴 용어 1 · 지문으로 보기`. */
@Composable
fun LockedCard(text: String, onUnlock: () -> Unit) {
    LockRow(title = text, actionLabel = WorldCopy.UNLOCK, onAction = onUnlock, modifier = Modifier.padding(top = 12.dp))
}

/** The card of a trashed or purged item: its dotted reference label only (rule 20). */
@Composable
fun GoneCard(text: String, dotted: Boolean) {
    if (text.isNotEmpty()) ListRow(title = text, titleDotted = dotted, modifier = Modifier.padding(top = 12.dp))
}

/** A plain `.small` notice line under the field table. */
@Composable
fun NoticeLine(text: String, modifier: Modifier = Modifier) {
    CivilightText(text, CivilightTheme.type.small, modifier.padding(top = 8.dp), color = CivilightTheme.colors.ink3)
}
