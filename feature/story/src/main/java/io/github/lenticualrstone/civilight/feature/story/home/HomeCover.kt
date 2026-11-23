package io.github.lenticualrstone.civilight.feature.story.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.ui.image.CoverArt
import io.github.lenticualrstone.civilight.ui.image.CoverInitialStyle

/** Home head cover (U-08 LIB-83 `[PROPOSED]`): 84 × 112, radius 14; badge 24 inset 6 with a 14 pencil. */
private const val COVER_W = 84
private const val COVER_H = 112
private const val COVER_RADIUS = 14
private const val BADGE = 24
private const val BADGE_INSET = 6
private const val BADGE_ICON = 14

/**
 * The cover left of the project name on 프로젝트 홈 (U-08 LIB-83): the 3:4 cover as the 서재 card draws it
 * (picture, preset or initial-letter card, static foil when on). When [ui] is editable the whole cover is
 * the button (≥ 44 dp) and a visible pencil badge sits at its bottom right; the read-only example draws
 * the cover only.
 */
@Composable
internal fun HomeCoverImage(ui: HomeUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(COVER_RADIUS.dp)
    val targetPx = with(LocalDensity.current) { COVER_W.dp.roundToPx() }
    val tap = if (ui.coverEditable) {
        Modifier
            .pressable(onClick = onClick, shape = shape, onClickLabel = ui.coverLabel)
            .semantics { contentDescription = ui.coverLabel }
    } else {
        Modifier
    }
    Box(modifier.size(COVER_W.dp, COVER_H.dp).then(tap)) {
        Box(Modifier.matchParentSize().clip(shape)) {
            CoverArt(ui.cover, targetPx, CoverInitialStyle.HEAD)
        }
        if (ui.coverEditable) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(BADGE_INSET.dp)
                    .size(BADGE.dp)
                    .clip(CircleShape)
                    .background(colors.bg)
                    .insetOutline(colors.edge, 1.dp, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                CivilightIcon(IconId.Pencil, contentDescription = null, size = BADGE_ICON.dp, tint = colors.ink2)
            }
        }
    }
}
