package io.github.lenticualrstone.civilight.feature.library.shelf.atelier

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.LaunchedEffect
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.FixedColors
import io.github.lenticualrstone.civilight.designsystem.theme.unhide
import io.github.lenticualrstone.civilight.feature.library.shelf.CoverArt
import io.github.lenticualrstone.civilight.feature.library.shelf.CoverThumb
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCardItem
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfViewInput
import kotlin.math.abs

// ---------------------------------------------------------------------------
// 6. 탄성 덱 (KINETIC_DECK) — Mechanical Hinge-Tension Fanning Card Deck
//    (1:1 zero-lag tracking + mechanical detent notch + crisp velocity-preserving hinge snap)
// ---------------------------------------------------------------------------
@Composable
internal fun KineticDeckView(input: ShelfViewInput) {
    val items = remember(input.ui) { atelierItems(input) }
    val track = rememberAtelierTrack(items, input.selectedKey, input.preview, input.dragResistance)
    val density = LocalDensity.current

    AtelierStageFrame(input, items, track.activeIndex) { tilt ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(top = input.barTop.dp, bottom = (input.barBottom + 126f).dp)
                .atelierHorizontalDrag(!input.preview, items.size, with(density) { 185.dp.toPx() }, track),
            contentAlignment = Alignment.Center,
        ) {
            // 3:4 cover; a short stage shrinks width with height (never flattens).
            val (cardW, cardH) = AtelierCoverBox.deck(maxWidth, maxHeight)
            val targetPx = with(density) { cardW.roundToPx() }
            // Subtle mechanical detent notch curve: pulls firmly into each integer stop (like a physical hinge detent).
            // Read in layer/draw lambdas only, so the moving track does not recompose every frame.
            fun offsetOf(i: Int): Float {
                val pos = track.position.value
                val frac = pos - kotlin.math.floor(pos)
                val detentPull = kotlin.math.sin(frac * 2f * kotlin.math.PI.toFloat()) * 0.055f
                return (i - pos) + detentPull
            }

            val visible by remember(items.size, track) {
                derivedStateOf { items.indices.filter { abs(it - track.position.value) <= 3.5f } }
            }
            visible.forEach { i ->
                key(items[i].key) {
                // 1:1 immediate position tracking (zero underwater lag!) + live velocity torsion lean.
                val stackDepth by remember(i, track) {
                    derivedStateOf { atelierStackDistance(i, track.position.value).coerceIn(0f, 3.5f) }
                }
                val focused = i == track.activeIndex
                AtelierCoverCard(
                    item = items[i],
                    tilt = tilt,
                    focused = focused,
                    sheenBias = { offsetOf(i) * 0.75f + track.dragImpulse.floatValue * 0.35f },
                    targetWidthPx = targetPx,
                    input = input,
                    modifier = Modifier
                        .size(cardW, cardH)
                        .zIndex(15f - stackDepth)
                        .graphicsLayer {
                            val d = offsetOf(i)
                            val depth = abs(d).coerceIn(0f, 3.5f)
                            val impulse = track.dragImpulse.floatValue
                            cameraDistance = 15f * density.density
                            // Immediate 1:1 translation + crisp velocity-coupled torsion on trailing cards
                            val torsion = impulse * (1f + depth * 0.45f)
                            translationX = d * (cardW.toPx() * 0.44f) + tilt.x * (10f / (1f + depth))
                            translationY = depth * 22.dp.toPx() + tilt.y * (7f / (1f + depth))
                            rotationZ = (d * 8.5f + torsion * 5.5f).coerceIn(-28f, 28f)
                            rotationY = (-d * 10f - torsion * 8f).coerceIn(-32f, 32f) + tilt.x * 6f
                            rotationX = -tilt.y * 4f
                            val s = (1f - depth * 0.085f).coerceIn(0.70f, 1.02f)
                            scaleX = s
                            scaleY = s
                            alpha = (1f - depth * 0.21f).coerceIn(0.20f, 1f)
                        },
                    onClick = {
                        if (focused) items[i].trigger(input) else track.animateTo(i, items.size)
                    },
                )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 7. 모자이크 (BENTO_MOSAIC) — Responsive Editorial Bento Grid with Living Focus
// ---------------------------------------------------------------------------
@Composable
internal fun BentoMosaicView(input: ShelfViewInput) {
    val items = remember(input.ui) { atelierItems(input) }
    var focusedIdx by rememberSaveable(items.size) { mutableIntStateOf(0) }
    val density = LocalDensity.current

    AtelierStageFrame(input, items, focusedIdx, showDock = false) { tilt ->
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val cols = if (maxWidth >= 560.dp) 3 else 2
            // Every tile is a 3:4 cover. The hero spans (cols − 1) columns at 3:4 with a side
            // panel in the last column, so it reads as featured without becoming a flat banner.
            val heroCols = (cols - 1).coerceAtLeast(1)
            val targetPx = with(density) { (maxWidth / cols).roundToPx() }

            val motion = CivilightTheme.motion
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = (input.barTop + 12f).dp,
                    bottom = (input.barBottom + 24f).dp,
                    start = 16.dp,
                    end = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Hero featured tile for index 0
                items.firstOrNull()?.let { hero ->
                    item(key = "hero_${hero.key}") {
                        val isFocused = focusedIdx == 0
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            AtelierCoverCard(
                                item = hero,
                                tilt = tilt,
                                focused = isFocused,
                                sheenBias = { 0f },
                                targetWidthPx = targetPx * heroCols,
                                input = input,
                                modifier = Modifier.weight(heroCols.toFloat()).aspectRatio(AtelierCoverBox.COVER_ASPECT),
                                onClick = {
                                    if (isFocused) hero.trigger(input) else focusedIdx = 0
                                },
                            )
                            Column(
                                Modifier
                                    .weight(1f)
                                    .animateContentSize(motion.move(CivilightMotion.standard)),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                CivilightText(hero.displayTitle(), CivilightTheme.type.heading, color = CivilightTheme.colors.ink, maxLines = 3)
                                CivilightText(hero.displayMeta(), CivilightTheme.type.small, color = CivilightTheme.colors.ink3, maxLines = 2)
                                AnimatedVisibility(visible = isFocused && !input.preview, enter = motion.enter(expand = true), exit = motion.exit(shrink = true)) {
                                    BentoActionStrip(hero, input)
                                }
                            }
                        }
                    }
                }

                // Remaining items in responsive Bento rows
                val rest = items.drop(1)
                val rows = rest.chunked(cols)
                itemsIndexed(rows, key = { rIdx, _ -> "bento_row_$rIdx" }) { rIdx, rowItems ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        rowItems.forEachIndexed { cIdx, item ->
                            val globalIdx = 1 + rIdx * cols + cIdx
                            val isFocused = focusedIdx == globalIdx
                            Column(
                                Modifier
                                    .weight(1f)
                                    .animateContentSize(motion.move(CivilightMotion.standard)),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                AtelierCoverCard(
                                    item = item,
                                    tilt = tilt,
                                    focused = isFocused,
                                    sheenBias = { (cIdx - 0.5f) * 0.4f },
                                    targetWidthPx = targetPx,
                                    input = input,
                                    cornerRadius = 16.dp,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(AtelierCoverBox.COVER_ASPECT),
                                    onClick = {
                                        if (isFocused) item.trigger(input) else focusedIdx = globalIdx
                                    },
                                )
                                AnimatedVisibility(visible = isFocused && !input.preview, enter = motion.enter(expand = true), exit = motion.exit(shrink = true)) {
                                    BentoActionStrip(item, input, compact = true)
                                }
                            }
                        }
                        repeat(cols - rowItems.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BentoActionStrip(item: ShelfCardItem, input: ShelfViewInput, compact: Boolean = false) {
    val colors = CivilightTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val label = when (item) {
            is ShelfCardItem.Project -> AtelierCopy.OPEN
            is ShelfCardItem.Lock -> AtelierCopy.UNLOCK
            ShelfCardItem.NewProject -> AtelierCopy.NEW_PROJECT
        }
        CivilightButton(
            text = label,
            onClick = { item.trigger(input) },
            modifier = Modifier.weight(1f),
            variant = ButtonVariant.PRIMARY,
        )
        if (!compact && item is ShelfCardItem.Project) {
            CivilightIconButton(IconId.More, AtelierCopy.MENU, { input.onMenu(item.card.row.key) }, tint = colors.ink2)
        }
    }
}

// ---------------------------------------------------------------------------
// 8. 필름 릴 (FILM_REEL) — 35mm Cinema Contact Sheet with Optical Cover Parallax
// ---------------------------------------------------------------------------
@Composable
internal fun FilmReelView(input: ShelfViewInput) {
    val items = remember(input.ui) { atelierItems(input) }
    val listState = rememberLazyListState()
    val colors = CivilightTheme.colors
    val density = LocalDensity.current
    val activeIdx = listState.firstVisibleItemIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))

    AtelierStageFrame(input, items, activeIdx) { tilt ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(top = input.barTop.dp, bottom = (input.barBottom + 126f).dp),
            contentAlignment = Alignment.Center,
        ) {
            val containerW = maxWidth
            // Strip chrome around each window: sprocket rows (2 × 26dp padding + 4dp) + frame label (~18dp).
            val stripChrome = 56.dp
            val labelH = 18.dp
            // 3:4 aperture window; a short stage shrinks width with height.
            val (frameW, windowH) = AtelierCoverBox.reelWindow(containerW, maxHeight - stripChrome - labelH)
            val targetPx = with(density) { frameW.roundToPx() }
            val stripBg = if (colors.isDark) Color(0xFF101216) else Color(0xFFE5E2D9)

            // Continuous film strip backing + top & bottom 35mm sprocket holes
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(windowH + labelH + stripChrome)
                    .background(stripBg),
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val holeW = 12.dp.toPx()
                    val holeH = 8.dp.toPx()
                    val step = 24.dp.toPx()
                    val holeColor = colors.bg
                    var x = 8.dp.toPx()
                    while (x < size.width) {
                        drawRoundRect(holeColor, Offset(x, 8.dp.toPx()), Size(holeW, holeH), CornerRadius(2.dp.toPx()))
                        drawRoundRect(holeColor, Offset(x, size.height - 16.dp.toPx()), Size(holeW, holeH), CornerRadius(2.dp.toPx()))
                        x += step
                    }
                }

                LazyRow(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = (containerW - frameW) / 2f, vertical = 26.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    itemsIndexed(items, key = { _, it -> it.key }) { index, item ->
                        val frameCode = "%02dA".format(index + 1)
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            CivilightText(
                                "CIVILIGHT 400 · $frameCode",
                                CivilightTheme.type.micro,
                                color = colors.acc.copy(alpha = 0.78f),
                            )
                            // Optical aperture window: cover scales slightly larger and shifts inside clip bounds!
                            Box(
                                Modifier
                                    .size(frameW, windowH)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.raise)
                                    .insetOutline(colors.edge.copy(alpha = 0.4f), 1.dp, RoundedCornerShape(10.dp))
                                    .then(
                                        if (input.preview) Modifier
                                        else Modifier.clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = { item.trigger(input) },
                                        ),
                                    ),
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            // Scroll state read here (layer phase), not in composition.
                                            val scrollBias = (index - listState.firstVisibleItemIndex) -
                                                (listState.firstVisibleItemScrollOffset / frameW.toPx().coerceAtLeast(1f))
                                            // Internal optical parallax opposite to scroll direction
                                            scaleX = 1.14f
                                            scaleY = 1.14f
                                            translationX = (-scrollBias * 26.dp.toPx()).coerceIn(-36f, 36f) + tilt.x * 8f
                                            translationY = tilt.y * 6f
                                        },
                                ) {
                                    when (item) {
                                        is ShelfCardItem.Project -> CoverArt(item.card.cover, targetWidth = targetPx)
                                        is ShelfCardItem.Lock -> Box(Modifier.fillMaxSize().background(colors.chip), contentAlignment = Alignment.Center) {
                                            CivilightIcon(IconId.Lock, null, size = 28.dp, tint = colors.ink2)
                                        }
                                        ShelfCardItem.NewProject -> Box(Modifier.fillMaxSize().background(colors.tint), contentAlignment = Alignment.Center) {
                                            CivilightIcon(IconId.Plus, null, size = 28.dp, tint = colors.acc)
                                        }
                                    }
                                }
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(82.dp)
                                        .align(Alignment.BottomCenter)
                                        .background(Brush.verticalGradient(listOf(Color.Transparent, FixedColors.imageShadeBottom))),
                                )
                                Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                                    CivilightText(item.displayTitle(), CivilightTheme.type.heading, color = FixedColors.onImage, maxLines = 1)
                                    CivilightText(item.displayMeta(), CivilightTheme.type.micro, color = FixedColors.onImage2, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 9. 시간 축 (PULSE_TIMELINE) — Animated Chronology with Breathing Pulse Node
// ---------------------------------------------------------------------------
@Composable
internal fun PulseTimelineView(input: ShelfViewInput) {
    val items = remember(input.ui) { atelierItems(input) }
    var activeIdx by rememberSaveable(items.size) { mutableIntStateOf(0) }
    val colors = CivilightTheme.colors
    val motion = CivilightTheme.motion
    val loops = motion.loops && !input.preview
    val pulseAnim = remember { Animatable(0f) }
    LaunchedEffect(loops) {
        if (!loops) {
            pulseAnim.snapTo(0f)
            return@LaunchedEffect
        }
        while (true) {
            pulseAnim.snapTo(0f)
            pulseAnim.animateTo(1f, CivilightMotion.spec(CivilightMotion.enterLong, reduced = false))
            kotlinx.coroutines.delay(900L)
        }
    }

    AtelierStageFrame(input, items, activeIdx, showDock = false) { tilt ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = (input.barTop + 14f).dp,
                bottom = (input.barBottom + 24f).dp,
                start = 16.dp,
                end = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(items, key = { _, it -> it.key }) { index, item ->
                val isSelected = index == activeIdx
                val cardShape = RoundedCornerShape(18.dp)
                val rowBg = if (isSelected) colors.sel else colors.raise.copy(alpha = 0.72f)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(98.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Left chronology rail + breathing pulse node
                    Canvas(Modifier.width(34.dp).fillMaxHeight()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        if (index > 0) drawLine(colors.line, Offset(cx, 0f), Offset(cx, cy - 8.dp.toPx()), 1.5.dp.toPx())
                        if (index < items.lastIndex) drawLine(colors.line, Offset(cx, cy + 8.dp.toPx()), Offset(cx, size.height), 1.5.dp.toPx())
                        if (isSelected && loops) {
                            // Read in the draw phase only: the pulse redraws, the list does not recompose.
                            val pulse = pulseAnim.value
                            drawCircle(
                                color = colors.acc.copy(alpha = (1f - pulse) * 0.45f),
                                radius = 6.dp.toPx() + pulse * 11.dp.toPx(),
                                center = Offset(cx, cy),
                                style = Stroke(width = 1.5.dp.toPx()),
                            )
                        }
                        drawCircle(
                            color = if (isSelected) colors.acc else colors.ink3.copy(alpha = 0.55f),
                            radius = if (isSelected) 5.5.dp.toPx() else 3.5.dp.toPx(),
                            center = Offset(cx, cy),
                        )
                    }

                    // Editorial timeline card
                    Row(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(cardShape)
                            .background(rowBg)
                            .insetOutline(if (isSelected) colors.acc.copy(alpha = 0.65f) else colors.line, 1.dp, cardShape)
                            .then(
                                if (input.preview) Modifier
                                else Modifier.pressable(
                                    onClick = {
                                        if (isSelected) item.trigger(input) else activeIdx = index
                                    },
                                    role = Role.Button,
                                    shape = cardShape,
                                ),
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            Modifier
                                .size(54.dp, 72.dp)
                                .graphicsLayer {
                                    translationX = tilt.x * 3f
                                    translationY = tilt.y * 2f
                                },
                        ) {
                            when (item) {
                                is ShelfCardItem.Project -> CoverThumb(item.card.cover, Modifier.fillMaxSize())
                                is ShelfCardItem.Lock -> Box(Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).background(colors.chip), contentAlignment = Alignment.Center) {
                                    CivilightIcon(IconId.Lock, null, size = 18.dp, tint = colors.ink2)
                                }
                                ShelfCardItem.NewProject -> Box(Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).background(colors.tint), contentAlignment = Alignment.Center) {
                                    CivilightIcon(IconId.Plus, null, size = 18.dp, tint = colors.acc)
                                }
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            CivilightText(item.displayTitle(), CivilightTheme.type.heading, color = colors.ink, maxLines = 1)
                            CivilightText(item.displayMeta(), CivilightTheme.type.small, color = colors.ink3, maxLines = 1)
                            if (item is ShelfCardItem.Project) {
                                val progress = ((item.card.row.facts?.progress?.percent ?: 12) / 100f).coerceIn(0.08f, 1f)
                                Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(colors.line)) {
                                    Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(if (isSelected) colors.acc else colors.ink3.copy(alpha = 0.6f)))
                                }
                            }
                        }
                        if (item is ShelfCardItem.Project && !input.preview) {
                            CivilightIconButton(IconId.More, AtelierCopy.MENU, { input.onMenu(item.card.row.key) }, tint = colors.ink2)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 10. 스튜디오 목록 (LIST / STUDIO_LIST) — Elevated High-Craft Kinetic Worklist
// ---------------------------------------------------------------------------
@Composable
internal fun StudioListView(input: ShelfViewInput) {
    val items = remember(input.ui) { atelierItems(input) }
    val colors = CivilightTheme.colors

    AtelierStageFrame(input, items, activeIndex = 0, showDock = false) { tilt ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = (input.barTop + 10f).dp,
                bottom = (input.barBottom + 24f).dp,
                start = 16.dp,
                end = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(items, key = { _, it -> it.key }) { _, item ->
                val selected = item is ShelfCardItem.Project && item.card.row.key == input.selectedKey
                val barWidth by animateDpAsState(
                    targetValue = if (selected) 3.dp else 0.dp,
                    animationSpec = CivilightTheme.motion.move(CivilightMotion.standard),
                    label = "studioBar",
                )
                val rowShape = RoundedCornerShape(16.dp)
                val unhideMod = if (item is ShelfCardItem.Project && item.card.hiddenOpen && input.unhide != null) {
                    Modifier.unhide(input.unhide)
                } else {
                    Modifier
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .then(unhideMod)
                        .clip(rowShape)
                        .background(if (selected) colors.sel else colors.raise.copy(alpha = 0.6f))
                        .insetOutline(if (selected) colors.acc.copy(alpha = 0.5f) else colors.line, 1.dp, rowShape)
                        .then(
                            if (input.preview) Modifier
                            else Modifier.pressable(onClick = { item.trigger(input) }, role = Role.Button, shape = rowShape),
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (barWidth > 0.dp) {
                        Box(Modifier.width(barWidth).height(38.dp).clip(CircleShape).background(colors.acc))
                    }
                    Box(
                        Modifier
                            .size(48.dp)
                            .graphicsLayer {
                                translationX = tilt.x * 2.5f
                                translationY = tilt.y * 2f
                            },
                    ) {
                        when (item) {
                            is ShelfCardItem.Project -> CoverThumb(item.card.cover, Modifier.fillMaxSize())
                            is ShelfCardItem.Lock -> Box(Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).background(colors.chip), contentAlignment = Alignment.Center) {
                                CivilightIcon(IconId.Lock, null, size = 18.dp, tint = colors.ink2)
                            }
                            ShelfCardItem.NewProject -> Box(Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).background(colors.tint), contentAlignment = Alignment.Center) {
                                CivilightIcon(IconId.Plus, null, size = 18.dp, tint = colors.acc)
                            }
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        CivilightText(item.displayTitle(), CivilightTheme.type.itemTitle, color = colors.ink, maxLines = 1)
                        CivilightText(item.displayMeta(), CivilightTheme.type.small, color = colors.ink3, maxLines = 1)
                        if (item is ShelfCardItem.Project) {
                            val ratio = ((item.card.row.facts?.progress?.percent ?: 12) / 100f).coerceIn(0.06f, 1f)
                            Box(Modifier.fillMaxWidth(0.7f).height(2.dp).clip(CircleShape).background(colors.line)) {
                                Box(Modifier.fillMaxWidth(ratio).fillMaxHeight().background(if (selected) colors.acc else colors.ink3.copy(alpha = 0.5f)))
                            }
                        }
                    }
                    if (item is ShelfCardItem.Project && !input.preview) {
                        CivilightIconButton(IconId.More, AtelierCopy.MENU, { input.onMenu(item.card.row.key) }, tint = colors.ink2)
                    }
                }
            }
        }
    }
}
