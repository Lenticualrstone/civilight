package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.text.DecoratedText
import io.github.lenticualrstone.civilight.designsystem.text.searchHitDecorations
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

/**
 * DS-45 search result: [location] in `small` above (`1-4 씬 2 · 세라`), 4 dp gap, then the **whole
 * sentence** in 16 / 27 400 `ink` with each match boxed inline (`hl` fill, `ink` text, 1 dp `acc`, radius
 * 4); the sentence wraps as normal text. Optional trailing state (`▲ 공개 전 사용`) in `warn` 600 12 / 16.
 * [matches] are UTF-16 ranges of [sentence] (STO search). Boxes draw left → right 60 ms apart when
 * [animate] (CNV-44).
 */
@Composable
fun SearchHit(
    location: String,
    sentence: String,
    matches: List<IntRange>,
    modifier: Modifier = Modifier,
    trailingWarning: String? = null,
    animate: Boolean = true,
    divider: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val decorations = remember(sentence, matches) { searchHitDecorations(sentence, matches) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (divider) Modifier.topHairline(colors.line) else Modifier)
            .then(if (onClick != null) Modifier.pressable(PressAction.Click(onClick), role = Role.Button, scaleOnPress = false) else Modifier)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CivilightText(location, type.small, color = colors.ink3, maxLines = 1)
        DecoratedText(
            AnnotatedString(sentence),
            type.lead,
            color = colors.ink,
            decorations = decorations,
            revealBoxes = animate,
        )
        if (trailingWarning != null) {
            CivilightText(
                trailingWarning,
                type.chip.merge(TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold)),
                color = colors.warn,
            )
        }
    }
}
