package io.github.lenticualrstone.civilight.feature.canvas.image.specs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageCopy
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageSlots
import io.github.lenticualrstone.civilight.feature.canvas.image.PresetArt
import io.github.lenticualrstone.civilight.feature.canvas.image.PresetImages
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.window.CenterColumn
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout

/**
 * `이미지 규격` (CNV-25, board `ImageSlots`): header `규격` over `이미지 · 자르기 · 홀로`; section
 * `이미지가 들어가는 곳` with one row per slot (name, where it is used, holo note, ratio glyphs with mono
 * labels), then `규칙` with four lines. Entry point not drawn (Q-IM-07).
 */
@Composable
fun ImageSpecsScreen(deps: FeatureDeps) {
    val side = if (LocalWindowLayout.current.isInner) 32.dp else 20.dp
    CivilightScreen(
        title = ImageCopy.SPECS_TITLE,
        smallLine = ImageCopy.SPECS_SMALL,
        back = IconAction(ImageCopy.BACK) { deps.navigator.pop() },
    ) { padding ->
        CenterColumn {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = side, end = side, top = padding.calculateTopPadding() + 12.dp, bottom = padding.calculateBottomPadding() + 24.dp),
            ) {
                item { SectionLabel(ImageCopy.SPECS_SECTION_SLOTS, Modifier.padding(bottom = 6.dp)) }
                items(ImageSlots.all, key = { it.slot.name }) { SlotRow(it.name, it.usedIn, it.holoNote, it.holoAllowed, it.ratios) }
                // The gallery's row (55 GAL-12): items are not an `ImageSlot` (core:model is frozen), so it is drawn here.
                item(key = "gallery") {
                    SlotRow(ImageCopy.SLOT_GALLERY, ImageCopy.USED_GALLERY, ImageCopy.HOLO_CHOICE, true, io.github.lenticualrstone.civilight.model.gallery.GalleryItem.RATIOS)
                }
                item { SectionLabel(ImageCopy.SPECS_SECTION_RULES, Modifier.padding(top = 24.dp, bottom = 6.dp)) }
                items(ImageCopy.SPECS_RULES) { line ->
                    val colors = CivilightTheme.colors
                    CivilightText(line, CivilightTheme.type.body, Modifier.fillMaxWidth().topHairline(colors.line).padding(vertical = 11.dp), color = colors.ink)
                }
            }
        }
    }
}

@Composable
private fun SlotRow(name: String, usedIn: String, holoNote: String, holoAllowed: Boolean, ratios: List<io.github.lenticualrstone.civilight.model.asset.Ratio>) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        Modifier.fillMaxWidth().topHairline(colors.line).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            CivilightText(name, type.itemTitleCompact, color = colors.ink, maxLines = 1)
            CivilightText(usedIn, type.small, color = colors.ink3, maxLines = 1)
            CivilightText(holoNote, type.small, color = if (holoAllowed) colors.acc else colors.ink3, maxLines = 1)
        }
        ratios.forEach { ratio ->
            val parts = ImageSlots.parts(ratio)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) { RatioGlyph(parts.value) }
                CivilightText(parts.label, type.number, color = colors.ink2, maxLines = 1)
            }
        }
    }
}

/** A ratio glyph: an outlined rectangle of the ratio inside a 36 dp square. */
@Composable
private fun RatioGlyph(ratio: Float) {
    val color = CivilightTheme.colors.ink2
    Canvas(Modifier.size(36.dp).clearAndSetSemantics { }) {
        val (w, h) = if (ratio >= 1f) size.width to size.width / ratio else size.height * ratio to size.height
        val stroke = 1.5.dp.toPx()
        drawRoundRect(color, Offset((size.width - w) / 2 + stroke / 2, (size.height - h) / 2 + stroke / 2), Size(w - stroke, h - stroke), CornerRadius(3.dp.toPx()), style = Stroke(stroke))
    }
}

/**
 * `기본 이미지` (CNV-26; `설정 · 지도 · 이미지 · 연표 › 기본 이미지 · 보기 ›`, view not drawn, Q-IM-07): the
 * eight presets in three columns with their names, under `8종 · 앱에 포함 · 오프라인`.
 */
@Composable
fun PresetImagesScreen(deps: FeatureDeps) {
    val side = if (LocalWindowLayout.current.isInner) 32.dp else 20.dp
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    CivilightScreen(
        title = ImageCopy.PRESETS_TITLE,
        smallLine = ImageCopy.PRESETS_SMALL,
        back = IconAction(ImageCopy.BACK) { deps.navigator.pop() },
    ) { padding ->
        CenterColumn {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = side, end = side, top = padding.calculateTopPadding() + 12.dp, bottom = padding.calculateBottomPadding() + 24.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    CivilightText(ImageCopy.PRESETS_SUB, type.small, Modifier.padding(bottom = 4.dp), color = colors.ink3)
                }
                items(PresetImages.all, key = { it.id }) { preset ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.fillMaxWidth().aspectRatio(108f / 144f).clip(RoundedCornerShape(12.dp))) { PresetArt(preset) }
                        CivilightText(preset.name, type.small, color = colors.ink2, maxLines = 1)
                    }
                }
            }
        }
    }
}
