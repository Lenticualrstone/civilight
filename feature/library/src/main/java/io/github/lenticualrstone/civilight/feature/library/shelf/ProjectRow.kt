package io.github.lenticualrstone.civilight.feature.library.shelf

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.RatingBadge
import io.github.lenticualrstone.civilight.designsystem.component.selectedRowBackground
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.ui.theme.ThemeBridge

/** Rating badge followed by a small meta line (`R-15 1장 7/10 · 인물 12 · 오늘 08:12`). */
@Composable
internal fun BadgeLine(rating: Rating, text: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        RatingBadge(ThemeBridge.rating(rating))
        Spacer(Modifier.width(6.dp))
        CivilightText(text, CivilightTheme.type.small, color = CivilightTheme.colors.ink3, maxLines = 1)
    }
}

/**
 * One 서재 project row (LIB-01): the 48 dp cover thumbnail (D6), title 800 20 / 28, then badge + meta;
 * right `쓰는 중` (`acc`), optional [extra] (`숨김 풀기` in the open group) and the ⋯ button. Tapping the
 * row opens it. [thumbModifier] carries the shared cover of the `목록 ↔ 카드` switch (CNV-45);
 * [menuModifier] goes on the ⋯ button (the `서재 · 숨기기` tour's hole, `LibraryTourSteps`).
 */
@Composable
internal fun ProjectRow(
    row: ProjectRowUi,
    onOpen: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    thumbModifier: Modifier = Modifier,
    extra: (@Composable () -> Unit)? = null,
    menuModifier: Modifier = Modifier,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectedRowBackground(selected, colors.sel, colors.acc)
            .topHairline(colors.line)
            .pressable(onClick = onOpen, role = Role.Button, scaleOnPress = false, onClickLabel = row.title)
            .heightIn(min = 44.dp)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverThumb(row.thumb, thumbModifier)
        Column(Modifier.weight(1f)) {
            CivilightText(row.title, type.title.merge(TextStyle(fontSize = 20.sp, lineHeight = 28.sp)), color = colors.ink, maxLines = 1)
            BadgeLine(row.rating, row.meta, Modifier.padding(top = 2.dp))
        }
        if (row.writing) {
            CivilightText(ShelfCopy.WRITING, type.chip.merge(TextStyle(fontWeight = FontWeight.SemiBold)), color = colors.acc, maxLines = 1)
        }
        extra?.invoke()
        CivilightIconButton(IconId.More, ShelfCopy.MORE_LABEL, onMenu, Modifier.offset(x = 12.dp).then(menuModifier))
    }
}
