package io.github.lenticualrstone.civilight.feature.canvas.gallery

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.designsystem.theme.LocalBackHandler
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.gallery.GalleryItem

/** What the viewer shows for the current item. */
class ViewerItem(val item: GalleryItem, val panel: PanelModel, val facts: List<String>)

/**
 * 보기 (GAL-09): the whole original, fitted (a preset at its own art), with holo when on; pinch zooms and two
 * fingers pan while zoomed (viewing only). `이전` · `설명` · `다음` at the bottom: the ends are drawn at 38 % and do
 * nothing; no swipe paging (§6 Q6). `설명` slides a text panel up below the image (never over it) and is
 * remembered app-wide. Inner: image left, a 300 wide text column right with the original's facts.
 */
@Composable
fun GalleryViewer(
    nav: ViewerNav,
    shown: ViewerItem,
    ownerName: String,
    textOn: Boolean,
    inner: Boolean,
    sharedImage: @Composable (Key) -> Modifier,
    onMove: (Key) -> Unit,
    onText: (Boolean) -> Unit,
    onClose: () -> Unit,
    onEdit: ((Key) -> Unit)?,
    onMenu: ((Key) -> Unit)?,
    openScene: (Key) -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    LocalBackHandler.current(true, onClose)
    val handlers = GalleryHandlers(
        tap = {}, view = {}, toggleMore = {}, openScene = openScene, unlock = {}, openOwner = {}, backToPick = {},
        menu = onMenu, edit = onEdit?.let { e -> { k: Key, _: Boolean -> e(k) } },
    )
    Column(Modifier.fillMaxSize().background(colors.bg).statusBarsPadding().navigationBarsPadding()) {
        // Top: 닫기 · `<owner> · 갤러리` over `<i>/<N>` · ⋯
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            CivilightIconButton(IconId.X, GalleryUiCopy.BTN_CLOSE, onClose)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                CivilightText(GalleryUiCopy.ownerGallery(ownerName), type.number, color = colors.ink3, maxLines = 1)
                CivilightText(nav.position, type.number, color = colors.ink2, maxLines = 1)
            }
            if (onMenu != null) CivilightIconButton(IconId.More, GalleryUiCopy.MENU, { onMenu(nav.current) })
            else Box(Modifier.width(44.dp))
        }
        if (inner) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    ViewerImage(nav, shown, sharedImage, Modifier.weight(1f).fillMaxWidth())
                    ViewerButtons(nav, textOn, onMove, onText)
                }
                FoldReveal(textOn, Modifier.fillMaxHeight(), horizontal = true, label = "viewerColumn") {
                    Column(
                        Modifier.width(300.dp).fillMaxHeight().background(colors.raise).verticalScroll(rememberScrollState()).padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ViewerText(shown, handlers, onEdit)
                        shown.facts.forEach { CivilightText(it, type.number, color = colors.ink3) }
                    }
                }
            }
        } else {
            ViewerImage(nav, shown, sharedImage, Modifier.weight(1f).fillMaxWidth())
            FoldReveal(textOn, label = "viewerPanel") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(colors.raise)
                        .fadingBottom(colors.raise)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) { ViewerText(shown, handlers, onEdit) }
            }
            ViewerButtons(nav, textOn, onMove, onText)
        }
    }
}

/** The image, sliding in the direction of `이전` / `다음` (standard 200; fade when reduced). */
@Composable
private fun ViewerImage(nav: ViewerNav, shown: ViewerItem, sharedImage: @Composable (Key) -> Modifier, modifier: Modifier) {
    val motion = CivilightTheme.motion
    AnimatedContent(
        targetState = shown,
        transitionSpec = { motion.push(forward = nav.keys.indexOf(targetState.item.key) >= nav.keys.indexOf(initialState.item.key)) },
        contentKey = { it.item.key },
        modifier = modifier.clip(androidx.compose.ui.graphics.RectangleShape),
        label = "viewerImage",
    ) { s ->
        ZoomableImage(s.item, sharedImage)
    }
}

/** Pinch zoom 1…4 and two-finger pan (viewing only, GAL-09); one finger does nothing. */
@Composable
private fun ZoomableImage(item: GalleryItem, sharedImage: @Composable (Key) -> Modifier) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        var scale by remember(item.key) { mutableFloatStateOf(1f) }
        var dx by remember(item.key) { mutableFloatStateOf(0f) }
        var dy by remember(item.key) { mutableFloatStateOf(0f) }
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(item.key, w, h) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            if (event.changes.count { it.pressed } >= 2) {
                                scale = ViewerZoom.scale(scale, event.calculateZoom())
                                val pan = event.calculatePan()
                                dx = ViewerZoom.pan(dx, pan.x, w, w, scale)
                                dy = ViewerZoom.pan(dy, pan.y, h, h, scale)
                                event.changes.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = dx
                    translationY = dy
                },
        ) {
            Box(Modifier.matchParentSize().then(sharedImage(item.key))) {
                GalleryImage(item, ratio = null, maxSide = GalleryImageSizes.VIEWER, contentScale = ContentScale.Fit, contentDescription = item.title.ifBlank { null })
            }
        }
    }
}

/** `이전` · `설명` · `다음` (secondary, primary when on, secondary). */
@Composable
private fun ViewerButtons(nav: ViewerNav, textOn: Boolean, onMove: (Key) -> Unit, onText: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EndButton(GalleryUiCopy.BTN_PREV, nav.hasPrev, Modifier.weight(1f)) { onMove(nav.prev()) }
        CivilightButton(
            GalleryUiCopy.BTN_TEXT,
            { onText(!textOn) },
            Modifier.weight(1f).semantics { role = Role.Switch; toggleableState = ToggleableState(textOn) },
            variant = if (textOn) ButtonVariant.PRIMARY else ButtonVariant.GHOST,
        )
        EndButton(GalleryUiCopy.BTN_NEXT, nav.hasNext, Modifier.weight(1f)) { onMove(nav.next()) }
    }
}

/** At an end the button is drawn at 38 % (the DS disabled alpha, animated) and does nothing (board `GalViewOff`). */
@Composable
private fun EndButton(text: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    CivilightButton(text, onClick, modifier, variant = ButtonVariant.GHOST, enabled = enabled)
}

@Composable
private fun ViewerText(shown: ViewerItem, handlers: GalleryHandlers, onEdit: ((Key) -> Unit)?) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val p = shown.panel
    Row(verticalAlignment = Alignment.CenterVertically) {
        CivilightText(p.title, type.title, Modifier.weight(1f), color = if (p.untitled) colors.ink3 else colors.ink)
        if (onEdit != null) TextAction(GalleryUiCopy.BTN_EDIT, { onEdit(p.key) }, color = colors.acc)
    }
    PanelMeta(p, away = false, handlers = handlers)
    val caption = p.caption
    if (caption == null) CivilightText(GalleryUiCopy.NO_DESCRIPTION, type.body, color = colors.ink3)
    else BasicText(captionText(caption), style = type.body.merge(TextStyle(color = colors.ink2)), modifier = Modifier.semantics { contentDescription = p.captionText.orEmpty() })
}

/** The fade at the bottom edge of the scrolling text area. */
private fun Modifier.fadingBottom(color: Color): Modifier = drawWithContent {
    drawContent()
    val h = 24.dp.toPx()
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, color), startY = size.height - h, endY = size.height), topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - h))
}
