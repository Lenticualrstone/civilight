package io.github.lenticualrstone.civilight.feature.canvas.holo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightSwitch
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTabs
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.HoloStrength
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.FixedColors
import io.github.lenticualrstone.civilight.designsystem.theme.ResolvedTheme
import io.github.lenticualrstone.civilight.feature.canvas.image.HoloTilt
import io.github.lenticualrstone.civilight.feature.canvas.image.PresetArt
import io.github.lenticualrstone.civilight.feature.canvas.image.PresetImages
import io.github.lenticualrstone.civilight.feature.canvas.image.ProvideHoloTilt
import io.github.lenticualrstone.civilight.feature.canvas.image.Tilt
import io.github.lenticualrstone.civilight.feature.canvas.image.holoDrag
import io.github.lenticualrstone.civilight.feature.canvas.image.holoFoilAt
import io.github.lenticualrstone.civilight.feature.canvas.image.rememberSpringTilt
import io.github.lenticualrstone.civilight.feature.canvas.image.rememberHoloTilt
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.window.CenterColumn

/**
 * `홀로 실험` (CNV-31, P2, experiment): the three experiment boards behind tabs. `은빛 홀로 카드`: a card that
 * turns with the tilt (gyro, else drag; still under reduced motion). `홀로 조각`: one shared tilt over title,
 * marks, badge, chip, button, progress, toggle and card border. `1장 완결`: the rare-moment screen; its two
 * buttons have no destination yet (Q-IM-11), so they are disabled.
 */
@Composable
fun HoloExperimentScreen(deps: FeatureDeps) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    ProvideHoloTilt {
        CivilightScreen(
            title = HoloCopy.TITLE,
            smallLine = HoloCopy.SMALL,
            back = IconAction(HoloCopy.BACK) { deps.navigator.pop() },
        ) { padding ->
            CenterColumn {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp)
                        .padding(horizontal = 20.dp),
                ) {
                    CivilightTabs(HoloCopy.TABS, tab, { tab = it })
                    val tilt = rememberHoloTilt()
                    // The three experiments crossfade when the tab changes (audit §4 canvas 11, P3).
                    ContentSwap(tab, Modifier.fillMaxWidth(), label = "holoTab") { shown ->
                        Column(Modifier.fillMaxWidth()) {
                            when (shown) {
                                0 -> HoloCardPanel(tilt)
                                1 -> HoloKitPanel(tilt)
                                else -> HoloMomentPanel(tilt)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun theme(): ResolvedTheme = if (CivilightTheme.colors.isDark) ResolvedTheme.DARK else ResolvedTheme.LIGHT

/** Silver text: the holo silver stops as a text brush, shifted by the tilt (30 %, as the foil's silver layer). */
@Composable
private fun silverText(tilt: Tilt): TextStyle {
    val (a, b) = FixedColors.holoSilver(theme())
    val shift = tilt.x * 0.3f * 400f
    return TextStyle(brush = Brush.linearGradient(listOf(a, b, a, b), start = Offset(shift, 0f), end = Offset(shift + 400f, 120f)))
}

@Composable
private fun ColumnScope.HoloCardPanel(tilt: HoloTilt) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    // The card follows a sensor's tilt through a spring so a noisy reading is smooth; a dragging finger is tracked
    // as it is (the release sway is already a spring).
    val sprung = rememberSpringTilt(tilt.value)
    val t = if (tilt.usesDrag) tilt.value else sprung
    val shape = RoundedCornerShape(22.dp)
    val loops = CivilightTheme.motion.loops
    CivilightText(HoloCopy.CARD_LINE, type.sec, Modifier.padding(top = 18.dp), color = colors.ink3)
    CivilightText(HoloCopy.CARD_HINT, type.small, color = colors.ink3)
    Box(
        Modifier
            .padding(vertical = 24.dp)
            .align(Alignment.CenterHorizontally)
            .size(300.dp, 430.dp)
            .graphicsLayer {
                rotationX = -t.y * 14f
                rotationY = t.x * 16f
                cameraDistance = 12f * density
            }
            .clip(shape)
            .holoDrag(tilt)
            .holoFoilAt(t, theme(), HoloStrength.CARD_FOIL),
    ) {
        PresetImages.byId("ash-dawn")?.let { PresetArt(it) }
        Column(Modifier.align(Alignment.BottomStart).padding(20.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            CivilightText(HoloCopy.SAMPLE_RECORD_NO, type.number, color = FixedColors.onImage2)
            CivilightText(HoloCopy.SAMPLE_NAME, type.title.merge(TextStyle(fontSize = 28.sp, lineHeight = 34.sp)), color = FixedColors.onImage)
            CivilightText(HoloCopy.SAMPLE_SUB, type.small, color = FixedColors.onImage2)
        }
    }
    val input = when {
        !loops -> HoloCopy.INPUT_STILL
        tilt.usesDrag -> HoloCopy.INPUT_DRAG
        else -> HoloCopy.INPUT_GYRO
    }
    CivilightText(input, type.label, color = colors.ink2)
    CivilightText(HoloCopy.INPUT_RULE, type.small, Modifier.padding(top = 4.dp), color = colors.ink3)
}

@Composable
private fun ColumnScope.HoloKitPanel(tilt: HoloTilt) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val t = tilt.value
    val th = theme()
    Column(Modifier.holoDrag(tilt)) {
        CivilightText(HoloCopy.KIT_LINE, type.sec, Modifier.padding(top = 18.dp), color = colors.ink3)
        CivilightText(HoloCopy.SAMPLE_PROJECT, type.title, color = colors.ink)
        CivilightText(HoloCopy.KIT_SUB, type.small, Modifier.padding(bottom = 12.dp), color = colors.ink3)
        KitRow(HoloCopy.KIT_TITLE) { CivilightText(HoloCopy.SAMPLE_DONE, type.title.merge(silverText(t)), maxLines = 1) }
        KitRow(HoloCopy.KIT_MARKS) { CivilightText(HoloCopy.SAMPLE_MARKS, type.title.merge(silverText(t)), maxLines = 1) }
        KitRow(HoloCopy.KIT_BADGE) {
            Box(Modifier.clip(RoundedCornerShape(5.dp)).background(colors.raise).holoFoilAt(t, th).heightIn(min = 18.dp).padding(horizontal = 6.dp)) {
                CivilightText(HoloCopy.SAMPLE_BADGE, type.number, color = colors.ink, maxLines = 1)
            }
        }
        KitRow(HoloCopy.KIT_CHIP) {
            Box(Modifier.clip(RoundedCornerShape(15.dp)).background(colors.chip).holoFoilAt(t, th).heightIn(min = 30.dp).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                CivilightText(HoloCopy.SAMPLE_CHIP, type.chip, color = colors.ink, maxLines = 1)
            }
        }
        KitRow(HoloCopy.KIT_BUTTON) {
            Box(Modifier.clip(RoundedCornerShape(22.dp)).holoFoilAt(t, th)) {
                CivilightButton(HoloCopy.SAMPLE_BUTTON, {}, variant = ButtonVariant.PRIMARY, enabled = false)
            }
        }
        KitRow(HoloCopy.KIT_PROGRESS) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CivilightText(HoloCopy.SAMPLE_PROGRESS, type.number, color = colors.ink2)
                Box(Modifier.width(140.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(colors.line)) {
                    Box(Modifier.width(98.dp).height(4.dp).background(colors.acc).holoFoilAt(t, th))
                }
            }
        }
        KitRow(HoloCopy.KIT_TOGGLE) {
            Box(Modifier.clip(RoundedCornerShape(14.dp)).holoFoilAt(t, th)) { CivilightSwitch(checked = true, onCheckedChange = null) }
        }
        KitRow(HoloCopy.KIT_BORDER) {
            Column(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.raise)
                    .holoFoilAt(t, th, HoloStrength.CARD_FOIL)
                    .padding(2.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.raise)
                    .insetOutline(colors.line, 1.dp, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                CivilightText(HoloCopy.SAMPLE_BORDER_TITLE, type.itemTitleCompact, color = colors.ink, maxLines = 1)
                CivilightText(HoloCopy.SAMPLE_BORDER_SUB, type.small, color = colors.ink3, maxLines = 1)
            }
        }
        CivilightText(HoloCopy.KIT_NOTE, type.small, Modifier.padding(top = 12.dp), color = colors.ink3)
    }
}

@Composable
private fun KitRow(label: String, item: @Composable () -> Unit) {
    val colors = CivilightTheme.colors
    Row(
        Modifier.fillMaxWidth().topHairline(colors.line).heightIn(min = 56.dp).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CivilightText(label, CivilightTheme.type.small, Modifier.weight(1f), color = colors.ink3, maxLines = 1)
        item()
    }
}

@Composable
private fun ColumnScope.HoloMomentPanel(tilt: HoloTilt) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val t = tilt.value
    Column(
        Modifier.fillMaxWidth().holoDrag(tilt).padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CivilightText(HoloCopy.SAMPLE_PROJECT, type.label, color = colors.ink2)
        CivilightText(HoloCopy.SAMPLE_DONE, type.title.merge(TextStyle(fontSize = 40.sp, lineHeight = 48.sp)).merge(silverText(t)), maxLines = 1)
        CivilightText(HoloCopy.SAMPLE_MOMENT_SUB, type.small, color = colors.ink3)
        Row(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            HoloCopy.SAMPLE_STATS.forEach { (value, label) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CivilightText(value, type.numberHeader, color = colors.ink)
                    CivilightText(label, type.small, color = colors.ink3)
                }
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(top = 24.dp).topHairline(colors.line).padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CivilightText(HoloCopy.MOST_SPOKEN, type.small, color = colors.ink3)
            CivilightText(HoloCopy.SAMPLE_MOST_SPOKEN, type.itemTitleCompact, color = colors.ink)
        }
        Row(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CivilightButton(HoloCopy.BTN_SAVE_CARD, {}, Modifier.weight(1f), variant = ButtonVariant.GHOST, enabled = false)
            CivilightButton(HoloCopy.BTN_NEXT_CHAPTER, {}, Modifier.weight(1f), variant = ButtonVariant.PRIMARY, enabled = false)
        }
    }
}
