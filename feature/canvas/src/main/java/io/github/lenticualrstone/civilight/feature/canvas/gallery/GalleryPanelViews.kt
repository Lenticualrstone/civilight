package io.github.lenticualrstone.civilight.feature.canvas.gallery

import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonSpec
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.model.Key

/**
 * What the screen can do with an item; null actions are not offered (their sheet is not registered yet, or the
 * project is read-only), so no button ever leads to a stand-in.
 */
class GalleryHandlers(
    val tap: (Key) -> Unit,
    val view: (Key) -> Unit,
    val toggleMore: () -> Unit,
    val openScene: (Key) -> Unit,
    val unlock: () -> Unit,
    val openOwner: (Key) -> Unit,
    val backToPick: () -> Unit,
    val menu: ((Key) -> Unit)?,
    val edit: ((Key, Boolean) -> Unit)?,
)

/** The arrow above the panel pointing at the picked card (GAL-08); it slides on `standard` when the pick moves. */
@Composable
fun PanelArrow(xDp: Float?, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val motion = CivilightTheme.motion
    val target = xDp ?: 0f
    val x by animateFloatAsState(target, motion.move(CivilightMotion.standard), label = "panelArrow")
    val shown = xDp != null
    Canvas(modifier.fillMaxWidth().height(ARROW_H.dp)) {
        if (!shown) return@Canvas
        val cx = x.dp.toPx()
        val w = ARROW_W.dp.toPx()
        val h = size.height
        val path = Path().apply {
            moveTo(cx - w / 2, h + 1f)
            lineTo(cx, 0f)
            lineTo(cx + w / 2, h + 1f)
            close()
        }
        drawPath(path, colors.raise)
    }
}

private const val ARROW_W = 16f
private const val ARROW_H = 8f

/**
 * The 설명 판 (GAL-08): title and position (or `고른 카드로` when the card is away), meta (scene link, tag chips
 * or `연결 없음`), the description clamped to [clampLines] with `더 보기` · `접기`, and the buttons. [twoColumns]
 * = inner `카드` (text left, buttons right, GAL-23). Content crossfades when the pick moves (GAL-24).
 */
@Composable
fun GalleryPanelCard(
    panel: PanelModel,
    clampLines: Int?,
    expanded: Boolean,
    away: Boolean,
    twoColumns: Boolean,
    handlers: GalleryHandlers,
    modifier: Modifier = Modifier,
) {
    val colors = CivilightTheme.colors
    val motion = CivilightTheme.motion
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.raise)
            .insetOutline(colors.line, 1.dp, shape)
            .animateContentSize(motion.move(CivilightMotion.standard)),
    ) {
        AnimatedContent(panel, transitionSpec = { motion.swap() }, contentKey = { it.key }, label = "panelSwap") { p ->
            val overflowed = remember(p.key) { mutableStateOf(false) }
            if (twoColumns) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PanelTitleRow(p, away, handlers)
                        PanelMeta(p, away, handlers)
                        PanelDescription(p, clampLines, expanded) { overflowed.value = true }
                    }
                    Column(Modifier.width(168.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PanelButtons(p, handlers, vertical = true)
                        PanelMoreToggle(p, clampLines, expanded, overflowed.value, handlers)
                    }
                }
            } else {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PanelTitleRow(p, away, handlers)
                    PanelMeta(p, away, handlers)
                    PanelDescription(p, clampLines, expanded) { overflowed.value = true }
                    PanelMoreToggle(p, clampLines, expanded, overflowed.value, handlers)
                    PanelButtons(p, handlers, vertical = false)
                }
            }
        }
    }
}

@Composable
private fun PanelTitleRow(panel: PanelModel, away: Boolean, handlers: GalleryHandlers) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(verticalAlignment = Alignment.CenterVertically) {
        CivilightText(panel.title, type.title, Modifier.weight(1f), color = if (panel.untitled) colors.ink3 else colors.ink, maxLines = 2)
        if (away) BackToPick(handlers.backToPick)
        else CivilightText(panel.position, type.number, color = colors.ink3, maxLines = 1)
    }
}

/** `고른 카드로` with a back chevron (`acc`): scrolls the row back so the picked card is at the row start. */
@Composable
private fun BackToPick(onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    Row(
        Modifier
            .heightIn(min = 44.dp)
            .pressable(PressAction.Click(onClick), role = Role.Button)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        CivilightIcon(IconId.Back, null, size = 16.dp, tint = colors.acc)
        CivilightText(GalleryUiCopy.BTN_BACK_TO_PICK, CivilightTheme.type.label, color = colors.acc, maxLines = 1)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PanelMeta(panel: PanelModel, away: Boolean, handlers: GalleryHandlers) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), itemVerticalAlignment = Alignment.CenterVertically) {
        val scene = panel.scene
        when {
            scene == null -> Unit
            scene.tappable && scene.sceneKey != null -> {
                val key = scene.sceneKey
                CivilightText(
                    scene.text,
                    type.label,
                    Modifier.heightIn(min = 32.dp).pressable(PressAction.Click({ handlers.openScene(key) }), role = Role.Button).padding(vertical = 6.dp),
                    color = colors.acc,
                    maxLines = 1,
                )
            }
            else -> CivilightText(
                scene.text,
                type.label.merge(if (scene.dotted) androidx.compose.ui.text.TextStyle(textDecoration = TextDecoration.Underline) else androidx.compose.ui.text.TextStyle()),
                color = colors.ink3,
                maxLines = 1,
            )
        }
        panel.tags.forEach { TagChip(it) }
        if (panel.noLink) CivilightText(GalleryUiCopy.NO_LINK, type.label, color = colors.ink3, maxLines = 1)
        if (away) CivilightText(panel.position, type.number, color = colors.ink3, maxLines = 1)
    }
}

/** A tag chip of the panel (height 22). */
@Composable
fun TagChip(text: String) {
    val colors = CivilightTheme.colors
    CivilightText(
        text,
        CivilightTheme.type.small,
        Modifier.clip(RoundedCornerShape(11.dp)).background(colors.tint).heightIn(min = 22.dp).padding(horizontal = 8.dp, vertical = 2.dp),
        color = colors.ink2,
        maxLines = 1,
    )
}

/** The description as styled text: `@` links the viewer may see are drawn in `acc`. */
@Composable
fun captionText(spans: List<CaptionSpan>): AnnotatedString {
    val acc = CivilightTheme.colors.acc
    return remember(spans, acc) {
        buildAnnotatedString {
            spans.forEach { s -> if (s.link) withStyle(SpanStyle(color = acc)) { append(s.text) } else append(s.text) }
        }
    }
}

/**
 * The description, clamped to [clampLines] while not [expanded]. Overflow is a layout fact that turns false once
 * expanded, so [onOverflow] latches it per item and `접기` stays until the panel is collapsed again.
 */
@Composable
private fun PanelDescription(panel: PanelModel, clampLines: Int?, expanded: Boolean, onOverflow: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val caption = panel.caption
    if (caption == null) {
        CivilightText(GalleryUiCopy.NO_DESCRIPTION, type.body, color = colors.ink3)
        return
    }
    BasicText(
        captionText(caption),
        style = type.body.merge(androidx.compose.ui.text.TextStyle(color = colors.ink2)),
        maxLines = if (clampLines == null || expanded) Int.MAX_VALUE else clampLines,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { if (it.hasVisualOverflow) onOverflow() },
    )
}

/** `더 보기` · `접기` (`acc`), only when the description was clamped. */
@Composable
private fun PanelMoreToggle(panel: PanelModel, clampLines: Int?, expanded: Boolean, overflowed: Boolean, handlers: GalleryHandlers) {
    if (panel.caption == null || clampLines == null || !(overflowed || expanded)) return
    TextAction(if (expanded) GalleryUiCopy.BTN_LESS else GalleryUiCopy.BTN_MORE, handlers.toggleMore, color = CivilightTheme.colors.acc)
}

/** `보기` · `고치기` · `⋯`, or `설명 쓰기` · `보기` · `⋯` (GAL-08 item 4); unavailable actions are left out. */
@Composable
fun PanelButtons(panel: PanelModel, handlers: GalleryHandlers, vertical: Boolean) {
    val key = panel.key
    val buttons = panel.buttons.filter {
        when (it) {
            PanelButton.EDIT, PanelButton.WRITE -> handlers.edit != null
            PanelButton.MENU -> handlers.menu != null
            PanelButton.VIEW -> true
        }
    }
    // ICON A10: `보기` · `고치기` are icons next to `⋯`; `설명 쓰기` stays a text button (c), the main action.
    val write = PanelButton.WRITE in buttons
    val icons: @Composable () -> Unit = {
        buttons.forEach { b ->
            when (b) {
                PanelButton.VIEW -> CivilightIconButton(IconButtonSpec.CARD_VIEW, { handlers.view(key) })
                PanelButton.EDIT -> CivilightIconButton(IconButtonSpec.CARD_CAPTION_EDIT, { handlers.edit?.invoke(key, false) })
                PanelButton.MENU -> CivilightIconButton(IconId.More, b.label, { handlers.menu?.invoke(key) })
                PanelButton.WRITE -> Unit
            }
        }
    }
    val writeButton: @Composable (Modifier) -> Unit = { m ->
        CivilightButton(PanelButton.WRITE.label, { handlers.edit?.invoke(key, true) }, m, variant = ButtonVariant.PRIMARY)
    }
    if (vertical) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (write) writeButton(Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) { icons() }
        }
    } else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
            if (write) writeButton(Modifier.weight(1f))
            icons()
        }
    }
}

/** `카드 누르기 · 설명 보기` (GAL-08) or `이미지 누르기 · 설명 보기` (inner, GAL-23): small, `ink3`, centred. */
@Composable
fun GalleryHint(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().heightIn(min = 44.dp).semantics { contentDescription = text }, contentAlignment = Alignment.Center) {
        CivilightText(text, CivilightTheme.type.small, color = CivilightTheme.colors.ink3, maxLines = 1)
    }
}
