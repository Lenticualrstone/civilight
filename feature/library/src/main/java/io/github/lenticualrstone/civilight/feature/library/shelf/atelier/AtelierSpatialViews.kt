package io.github.lenticualrstone.civilight.feature.library.shelf.atelier

import androidx.compose.animation.core.animateFloatAsState
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCardItem
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfViewInput
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// ---------------------------------------------------------------------------
// 1. 유리 프리즘 (GLASS_PRISM) — 3D Refractive Polygonal Prism Carousel
// ---------------------------------------------------------------------------
@Composable
internal fun GlassPrismView(input: ShelfViewInput) {
    val items = remember(input.ui) { atelierItems(input) }
    val track = rememberAtelierTrack(items, input.selectedKey, input.preview)
    val density = LocalDensity.current

    AtelierStageFrame(input, items, track.activeIndex) { tilt ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(top = input.barTop.dp, bottom = (input.barBottom + 126f).dp)
                .atelierHorizontalDrag(!input.preview, items.size, with(density) { 170.dp.toPx() }, track),
            contentAlignment = Alignment.Center,
        ) {
            val cardW = (maxWidth * 0.56f).coerceIn(168.dp, 260.dp)
            val cardH = (cardW * 1.34f).coerceAtMost(maxHeight * 0.84f)
            val radiusPx = with(density) { (maxWidth * 0.42f).toPx() }
            val targetPx = with(density) { cardW.roundToPx() }

            // Subtle floor reflection disc
            Canvas(Modifier.fillMaxSize()) {
                drawOval(
                    brush = Brush.radialGradient(
                        listOf(Color.Black.copy(alpha = 0.28f), Color.Transparent),
                        center = Offset(size.width / 2f, size.height * 0.86f),
                        radius = size.width * 0.36f,
                    ),
                    topLeft = Offset(size.width * 0.14f, size.height * 0.78f),
                    size = Size(size.width * 0.72f, size.height * 0.16f),
                )
            }

            val pos = track.position.value
            val visibleIndices = items.indices.filter { abs(it - pos) <= 3.2f }
            visibleIndices.forEach { i ->
                val d = i - pos
                val angleRad = (d * 0.58f).coerceIn(-1.45f, 1.45f)
                val depth = cos(angleRad)
                val focused = i == track.activeIndex
                AtelierCoverCard(
                    item = items[i],
                    tilt = tilt,
                    focused = focused,
                    sheenBias = d,
                    targetWidthPx = targetPx,
                    input = input,
                    modifier = Modifier
                        .size(cardW, cardH)
                        .zIndex(10f - abs(d))
                        .graphicsLayer {
                            cameraDistance = 14f * density.density
                            translationX = sin(angleRad) * radiusPx + tilt.x * (12f * depth)
                            translationY = (1f - depth) * 28f + tilt.y * (8f * depth)
                            rotationY = (-d * 34f).coerceIn(-68f, 68f) + tilt.x * 6f
                            rotationX = -tilt.y * 5f
                            val s = (0.72f + 0.28f * depth).coerceIn(0.62f, 1.02f)
                            scaleX = s
                            scaleY = s
                            alpha = (0.24f + 0.76f * depth).coerceIn(0f, 1f)
                        },
                    onClick = {
                        if (focused) items[i].trigger(input) else track.animateTo(i, items.size)
                    },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 2. 깊이 서고 (ARCHIVE_VAULT) — 3D Z-Axis Perspective Depth Corridor
// ---------------------------------------------------------------------------
@Composable
internal fun ArchiveVaultView(input: ShelfViewInput) {
    val items = remember(input.ui) { atelierItems(input) }
    val track = rememberAtelierTrack(items, input.selectedKey, input.preview)
    val density = LocalDensity.current
    val colors = CivilightTheme.colors

    AtelierStageFrame(input, items, track.activeIndex) { tilt ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(top = input.barTop.dp, bottom = (input.barBottom + 126f).dp)
                .atelierVerticalDrag(!input.preview, items.size, with(density) { 140.dp.toPx() }, track),
            contentAlignment = Alignment.Center,
        ) {
            val cardW = (maxWidth * 0.64f).coerceIn(190.dp, 300.dp)
            val cardH = (cardW * 1.28f).coerceAtMost(maxHeight * 0.76f)
            val targetPx = with(density) { cardW.roundToPx() }

            // Procedural 3D perspective vanishing-point architectural ribs
            Canvas(Modifier.fillMaxSize()) {
                val vx = size.width * (0.5f + tilt.x * 0.06f)
                val vy = size.height * (0.24f + tilt.y * 0.04f)
                val lineColor = colors.line.copy(alpha = if (colors.isDark) 0.55f else 0.45f)
                // Four vanishing rails to the corners
                val corners = listOf(
                    Offset(0f, 0f), Offset(size.width, 0f),
                    Offset(0f, size.height), Offset(size.width, size.height),
                )
                corners.forEach { c ->
                    drawLine(lineColor, Offset(vx, vy), c, strokeWidth = 1.dp.toPx())
                }
                // Perspective depth frames receding toward (vx, vy)
                for (step in 1..4) {
                    val t = step / 5f
                    val w = size.width * t
                    val h = size.height * t
                    drawRoundRect(
                        color = lineColor.copy(alpha = lineColor.alpha * t),
                        topLeft = Offset(vx - w / 2f, vy - h * 0.28f),
                        size = Size(w, h * 0.85f),
                        cornerRadius = CornerRadius(14.dp.toPx() * t),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }
            }

            val pos = track.position.value
            val visibleIndices = items.indices.filter { it - pos in -1.2f..4.2f }.reversed()
            visibleIndices.forEach { i ->
                val d = i - pos // 0 = front focus, positive = deeper in vault, negative = passing overhead
                val focused = i == track.activeIndex
                AtelierCoverCard(
                    item = items[i],
                    tilt = tilt,
                    focused = focused,
                    sheenBias = -d * 0.6f,
                    targetWidthPx = targetPx,
                    input = input,
                    modifier = Modifier
                        .size(cardW, cardH)
                        .zIndex(20f - abs(d))
                        .graphicsLayer {
                            cameraDistance = 16f * density.density
                            val depthScale = when {
                                d < 0f -> 1f + abs(d) * 0.24f
                                else -> (1f - d * 0.14f).coerceAtLeast(0.45f)
                            }
                            scaleX = depthScale
                            scaleY = depthScale
                            translationY = when {
                                d < 0f -> -d * 180f // glides down & out of frame
                                else -> -d * 48f + tilt.y * (10f * depthScale)
                            }
                            translationX = tilt.x * (16f * depthScale)
                            rotationX = (d * 5.5f).coerceIn(-14f, 18f) - tilt.y * 4f
                            rotationY = tilt.x * 5f
                            alpha = when {
                                d < 0f -> (1f + d * 1.1f).coerceIn(0f, 1f)
                                else -> (1f - d * 0.21f).coerceIn(0.08f, 1f)
                            }
                        },
                    onClick = {
                        if (focused) items[i].trigger(input) else track.animateTo(i, items.size)
                    },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 3. 양장 서가 (BOOK_SPINE_3D) — Tactile 3D Hardcover Spines + Pull-Out Pivot
// ---------------------------------------------------------------------------
@Composable
internal fun BookSpine3dView(input: ShelfViewInput) {
    val items = remember(input.ui) { atelierItems(input) }
    val track = rememberAtelierTrack(items, input.selectedKey, input.preview)
    val density = LocalDensity.current
    val colors = CivilightTheme.colors

    AtelierStageFrame(input, items, track.activeIndex) { tilt ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(top = input.barTop.dp, bottom = (input.barBottom + 126f).dp)
                .atelierHorizontalDrag(!input.preview, items.size, with(density) { 84.dp.toPx() }, track),
            contentAlignment = Alignment.Center,
        ) {
            val bookH = (maxHeight * 0.72f).coerceIn(210.dp, 360.dp)
            val spineW = 46.dp
            val coverW = (bookH * 0.72f).coerceAtMost(maxWidth * 0.56f)
            val targetPx = with(density) { coverW.roundToPx() }
            val pos = track.position.value

            // Architectural stone/wood shelf plank + ambient occlusion shadow
            Canvas(Modifier.fillMaxSize()) {
                val plankY = size.height * 0.5f + bookH.toPx() * 0.5f + 2.dp.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            colors.raise,
                            colors.bg,
                        ),
                        startY = plankY,
                        endY = plankY + 26.dp.toPx(),
                    ),
                    topLeft = Offset(16.dp.toPx(), plankY),
                    size = Size(size.width - 32.dp.toPx(), 18.dp.toPx()),
                )
                drawLine(
                    color = colors.edge.copy(alpha = 0.45f),
                    start = Offset(16.dp.toPx(), plankY),
                    end = Offset(size.width - 16.dp.toPx(), plankY),
                    strokeWidth = 1.5.dp.toPx(),
                )
            }

            // Render spines + pull-out 3D cover for the focused book
            val visibleIndices = items.indices.filter { abs(it - pos) <= 4.5f }
            visibleIndices.forEach { i ->
                val d = i - pos
                val focusFactor = (1f - abs(d)).coerceIn(0f, 1f)
                val motion = CivilightTheme.motion
                val pulled by animateFloatAsState(
                    targetValue = if (i == track.activeIndex) 1f else 0f,
                    animationSpec = motion.move(CivilightMotion.standard),
                    label = "bookPull",
                )
                val item = items[i]
                val tone = spineTone(item.key.toString(), colors.isDark)
                val spreadPx = with(density) { (spineW + 10.dp).toPx() }
                val gapForFocusPx = with(density) { (coverW * 0.52f).toPx() }
                val xOffset = d * spreadPx + (if (d > 0.1f) gapForFocusPx * (1f - focusFactor) else if (d < -0.1f) -gapForFocusPx * (1f - focusFactor) else 0f)

                if (focusFactor > 0.52f) {
                    // Pulled-out 3D cover pivoting smoothly toward the reader
                    AtelierCoverCard(
                        item = item,
                        tilt = tilt,
                        focused = true,
                        sheenBias = d + (1f - pulled) * 0.5f,
                        targetWidthPx = targetPx,
                        input = input,
                        cornerRadius = 12.dp,
                        modifier = Modifier
                            .size(coverW, bookH)
                            .zIndex(20f)
                            .graphicsLayer {
                                cameraDistance = 15f * density.density
                                translationX = xOffset + tilt.x * 10f
                                translationY = -14.dp.toPx() * pulled + tilt.y * 6f
                                rotationY = (1f - pulled) * -42f + tilt.x * 6f
                                rotationX = -tilt.y * 4f
                                val s = 0.92f + 0.10f * pulled
                                scaleX = s
                                scaleY = s
                            },
                        onClick = { item.trigger(input) },
                    )
                } else {
                    // Procedural 3D cloth/leather hardcover spine
                    val spineShape = RoundedCornerShape(4.dp)
                    Box(
                        Modifier
                            .size(spineW, bookH * 0.94f)
                            .zIndex(10f - abs(d))
                            .graphicsLayer {
                                cameraDistance = 12f * density.density
                                translationX = xOffset + tilt.x * 4f
                                translationY = 4.dp.toPx()
                                rotationY = (-d * 6f).coerceIn(-22f, 22f)
                                alpha = (1f - abs(d) * 0.15f).coerceIn(0.32f, 1f)
                            }
                            .clip(spineShape)
                            .drawBehind {
                                // Cylindrical spine lighting gradient
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        listOf(
                                            tone.copy(alpha = 0.78f),
                                            tone,
                                            Color.White.copy(alpha = if (colors.isDark) 0.14f else 0.32f),
                                            tone,
                                            Color.Black.copy(alpha = 0.25f),
                                        ),
                                    ),
                                )
                                // Metallic foil bands at top & bottom of spine
                                val bandColor = colors.acc.copy(alpha = 0.55f)
                                drawRect(bandColor, Offset(0f, size.height * 0.11f), Size(size.width, 2.dp.toPx()))
                                drawRect(bandColor, Offset(0f, size.height * 0.87f), Size(size.width, 2.dp.toPx()))
                            }
                            .insetOutline(colors.line, 1.dp, spineShape)
                            .then(
                                if (input.preview) Modifier
                                else Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { track.animateTo(i, items.size) },
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 22.dp, horizontal = 4.dp),
                        ) {
                            if (item is ShelfCardItem.Lock) {
                                CivilightIcon(IconId.Lock, null, size = 16.dp, tint = colors.ink2)
                            } else if (item is ShelfCardItem.NewProject) {
                                CivilightIcon(IconId.Plus, null, size = 16.dp, tint = colors.acc)
                            } else {
                                // Vertical embossed characters for crisp Korean spine readability
                                item.displayTitle().take(7).forEach { ch ->
                                    CivilightText(
                                        ch.toString(),
                                        TextStyle(fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold),
                                        color = colors.ink.copy(alpha = 0.88f),
                                        maxLines = 1,
                                    )
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
// 4. 궤도 무대 (ORBIT_STAGE) — Tilted 3D Elliptical Orbital Ring + Spotlight
// ---------------------------------------------------------------------------
@Composable
internal fun OrbitStageView(input: ShelfViewInput) {
    val items = remember(input.ui) { atelierItems(input) }
    val track = rememberAtelierTrack(items, input.selectedKey, input.preview)
    val density = LocalDensity.current
    val colors = CivilightTheme.colors

    AtelierStageFrame(input, items, track.activeIndex) { tilt ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(top = input.barTop.dp, bottom = (input.barBottom + 126f).dp)
                .atelierHorizontalDrag(!input.preview, items.size, with(density) { 130.dp.toPx() }, track),
            contentAlignment = Alignment.Center,
        ) {
            val cardW = (maxWidth * 0.46f).coerceIn(144.dp, 224.dp)
            val cardH = (cardW * 1.32f).coerceAtMost(maxHeight * 0.72f)
            val targetPx = with(density) { cardW.roundToPx() }
            val rx = with(density) { (maxWidth * 0.37f).toPx() }
            val ry = with(density) { (maxHeight * 0.14f).toPx() }

            // Procedural orbital rings + degree tick marks + front spotlight
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height * 0.56f
                val ringColor = colors.line.copy(alpha = if (colors.isDark) 0.7f else 0.55f)
                drawOval(
                    color = ringColor,
                    topLeft = Offset(cx - rx, cy - ry),
                    size = Size(rx * 2f, ry * 2f),
                    style = Stroke(width = 1.dp.toPx()),
                )
                drawOval(
                    color = ringColor.copy(alpha = ringColor.alpha * 0.5f),
                    topLeft = Offset(cx - rx * 0.76f, cy - ry * 0.76f),
                    size = Size(rx * 1.52f, ry * 1.52f),
                    style = Stroke(width = 1.dp.toPx()),
                )
                // 24 orbital compass ticks
                for (t in 0 until 24) {
                    val a = (t / 24f) * (2f * PI.toFloat())
                    val p0 = Offset(cx + cos(a) * rx * 0.96f, cy + sin(a) * ry * 0.96f)
                    val p1 = Offset(cx + cos(a) * rx * 1.03f, cy + sin(a) * ry * 1.03f)
                    drawLine(ringColor, p0, p1, strokeWidth = 1.dp.toPx())
                }
                // Front spotlight pool under active work
                drawOval(
                    brush = Brush.radialGradient(
                        listOf(colors.acc.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(cx, cy + ry),
                        radius = rx * 0.55f,
                    ),
                    topLeft = Offset(cx - rx * 0.5f, cy + ry * 0.45f),
                    size = Size(rx, ry * 1.1f),
                )
            }

            val pos = track.position.value
            val count = items.size.coerceAtLeast(1)
            val stepRad = (2f * PI.toFloat()) / count.coerceAtLeast(5).coerceAtMost(8)
            items.indices.forEach { i ->
                val d = i - pos
                if (abs(d) > 3.6f) return@forEach
                val theta = d * stepRad // 0 = front (closest)
                val zNorm = cos(theta) // +1 front, -1 back
                val xOrbit = sin(theta) * rx
                val yOrbit = (cos(theta) - 1f) * -ry * 0.55f - (1f - zNorm) * 24f
                val focused = i == track.activeIndex
                AtelierCoverCard(
                    item = items[i],
                    tilt = tilt,
                    focused = focused,
                    sheenBias = sin(theta),
                    targetWidthPx = targetPx,
                    input = input,
                    modifier = Modifier
                        .size(cardW, cardH)
                        .zIndex(10f + zNorm * 5f)
                        .graphicsLayer {
                            cameraDistance = 14f * density.density
                            translationX = xOrbit + tilt.x * (12f * (0.5f + 0.5f * zNorm))
                            translationY = yOrbit + tilt.y * (8f * (0.5f + 0.5f * zNorm))
                            val s = (0.64f + 0.36f * ((zNorm + 1f) / 2f)).coerceIn(0.55f, 1.02f)
                            scaleX = s
                            scaleY = s
                            rotationY = (-sin(theta) * 26f) + tilt.x * 5f
                            rotationX = -tilt.y * 4f
                            alpha = (0.28f + 0.72f * ((zNorm + 1f) / 2f)).coerceIn(0.15f, 1f)
                        },
                    onClick = {
                        if (focused) items[i].trigger(input) else track.animateTo(i, items.size)
                    },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 5. 화첩 펼침 (FOLIO_SPREAD) — 3D Left-Spine Hinge Page-Turn Book Folio
// ---------------------------------------------------------------------------
@Composable
internal fun FolioSpreadView(input: ShelfViewInput) {
    val items = remember(input.ui) { atelierItems(input) }
    val track = rememberAtelierTrack(items, input.selectedKey, input.preview)
    val density = LocalDensity.current
    val colors = CivilightTheme.colors

    AtelierStageFrame(input, items, track.activeIndex) { tilt ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(top = input.barTop.dp, bottom = (input.barBottom + 126f).dp)
                .atelierHorizontalDrag(!input.preview, items.size, with(density) { 210.dp.toPx() }, track),
            contentAlignment = Alignment.Center,
        ) {
            val pageW = (maxWidth * 0.64f).coerceIn(190.dp, 290.dp)
            val pageH = (pageW * 1.34f).coerceAtMost(maxHeight * 0.84f)
            val targetPx = with(density) { pageW.roundToPx() }
            val pos = track.position.value
            val baseIndex = pos.toInt().coerceIn(0, (items.size - 1).coerceAtLeast(0))
            val frac = (pos - baseIndex).coerceIn(0f, 1f)

            // Book binding shadow + subtle stacked page edges underneath
            Box(Modifier.size(pageW + 10.dp, pageH + 6.dp), contentAlignment = Alignment.CenterStart) {
                // Stacked pages right edge
                Canvas(Modifier.fillMaxSize()) {
                    for (layer in 3 downTo 1) {
                        val dx = layer * 2.5.dp.toPx()
                        drawRoundRect(
                            color = colors.raise.copy(alpha = 0.7f),
                            topLeft = Offset(dx, layer * 1.dp.toPx()),
                            size = Size(pageW.toPx(), pageH.toPx()),
                            cornerRadius = CornerRadius(14.dp.toPx()),
                            style = Stroke(1.dp.toPx()),
                        )
                    }
                }

                // Underlying next page (revealed as current page turns)
                val nextIndex = (baseIndex + 1).coerceAtMost(items.lastIndex)
                if (nextIndex != baseIndex || items.size == 1) {
                    val underItem = items[if (frac > 0.01f && nextIndex != baseIndex) nextIndex else baseIndex]
                    AtelierCoverCard(
                        item = underItem,
                        tilt = tilt,
                        focused = frac > 0.5f,
                        sheenBias = (1f - frac) * -0.4f,
                        targetWidthPx = targetPx,
                        input = input,
                        cornerRadius = 14.dp,
                        modifier = Modifier
                            .size(pageW, pageH)
                            .graphicsLayer {
                                val reveal = 0.94f + 0.06f * frac
                                scaleX = reveal
                                scaleY = reveal
                                alpha = (0.45f + 0.55f * frac).coerceIn(0.45f, 1f)
                            },
                        onClick = { underItem.trigger(input) },
                    )
                }

                // Active turning page anchored at the left spine hinge (TransformOrigin 0f, 0.5f)
                if (items.isNotEmpty()) {
                    val topItem = items[baseIndex]
                    val turnDeg = -frac * 142f
                    // Dynamic fold shadow peaks at 90 degrees
                    val foldShadow = sin(frac * PI.toFloat()).coerceIn(0f, 1f) * 0.48f
                    Box(
                        Modifier
                            .size(pageW, pageH)
                            .zIndex(10f)
                            .graphicsLayer {
                                cameraDistance = 18f * density.density
                                transformOrigin = TransformOrigin(0f, 0.5f)
                                rotationY = turnDeg + tilt.x * 4f
                                rotationX = -tilt.y * 3f
                                alpha = if (frac > 0.88f) ((1f - frac) / 0.12f).coerceIn(0f, 1f) else 1f
                            },
                    ) {
                        AtelierCoverCard(
                            item = topItem,
                            tilt = tilt,
                            focused = frac <= 0.5f,
                            sheenBias = frac * 1.2f,
                            targetWidthPx = targetPx,
                            input = input,
                            cornerRadius = 14.dp,
                            modifier = Modifier.fillMaxSize(),
                            onClick = { topItem.trigger(input) },
                        )
                        // Spine crease + page-turn shadow overlay
                        Canvas(Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))) {
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.32f + foldShadow * 0.4f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = foldShadow),
                                    ),
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
