package io.github.lenticualrstone.civilight.feature.library.shelf.atelier

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.lenticualrstone.civilight.designsystem.card.DeviceTilt
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.FixedColors
import io.github.lenticualrstone.civilight.designsystem.theme.HapticToken
import io.github.lenticualrstone.civilight.designsystem.theme.rememberHaptics
import io.github.lenticualrstone.civilight.designsystem.theme.unhide
import io.github.lenticualrstone.civilight.feature.library.shelf.CoverArt
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCardItem
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfViewInput
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// ===========================================================================
// 1. CD 케이스 (CD_JEWEL_CASE) — Hyper-Detailed 3D Polycarbonate Jewel Case
//    with Ribbed Clear Spine, Hinged Acrylic Lid, Corner Label, and
//    Iridescent Holographic Compact Disc (0.4.0: clean floating stage, no
//    unnatural floor shadow, configurable mechanical 1-step detent resistance)
// ===========================================================================

@Composable
internal fun CdJewelCaseView(input: ShelfViewInput) {
    val items = remember(input.ui) { atelierItems(input) }
    val track = rememberAtelierTrack(items, input.selectedKey, input.preview, input.dragResistance)
    val density = LocalDensity.current
    val motion = CivilightTheme.motion
    val haptics = rememberHaptics()
    // Which case currently has its acrylic lid swung open & holographic CD peeking out
    var openedIdx by remember { mutableIntStateOf(-1) }
    LaunchedEffect(track.activeIndex) {
        if (openedIdx != track.activeIndex) openedIdx = -1
    }

    AtelierStageFrame(input, items, track.activeIndex) { tilt ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(top = input.barTop.dp, bottom = (input.barBottom + 126f).dp)
                .atelierHorizontalDrag(!input.preview, items.size, with(density) { 195.dp.toPx() }, track),
            contentAlignment = Alignment.Center,
        ) {
            val caseSize = (minOf(maxWidth * 0.70f, maxHeight * 0.72f)).coerceIn(200.dp, 296.dp)
            val caseW = caseSize * 1.09f // Authentic 142mm x 125mm jewel case ratio
            val caseH = caseSize
            val targetPx = with(density) { caseSize.roundToPx() }
            val pos = track.position.value
            val impulse = track.dragImpulse.floatValue

            val visible = items.indices.filter { abs(it - pos) <= 2.5f }
            visible.forEach { i ->
                val d = i - pos
                val focused = i == track.activeIndex
                val isOpen = focused && openedIdx == i && !input.preview
                val lidOpen by animateFloatAsState(
                    targetValue = if (isOpen) 1f else 0f,
                    animationSpec = motion.crispSpring(),
                    label = "cdLid_$i",
                )

                val item = items[i]
                val holoEnabled = (item as? ShelfCardItem.Project)?.card?.cover?.holo == true

                Box(
                    Modifier
                        .size(caseW, caseH)
                        .zIndex(10f - abs(d))
                        .graphicsLayer {
                            cameraDistance = 16f * density.density
                            val spread = caseW.toPx() * 0.86f
                            translationX = d * spread + tilt.x * (10f / (1f + abs(d))) - lidOpen * 26.dp.toPx()
                            translationY = abs(d) * 18.dp.toPx() + tilt.y * (7f / (1f + abs(d)))
                            // Crisp mechanical tilt responds immediately to live detent tension
                            rotationY = (-d * 24f).coerceIn(-52f, 52f) + impulse * 6f + tilt.x * 8f
                            rotationX = -tilt.y * 6f
                            val s = (1f - abs(d) * 0.14f).coerceIn(0.72f, 1.02f)
                            scaleX = s
                            scaleY = s
                            alpha = (1f - abs(d) * 0.32f).coerceIn(0.22f, 1f)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    // 1) Sliding Holographic Compact Disc (peeks out right + spins gently on open!)
                    val discDiameter = caseH * 0.88f
                    HolographicCdDisc(
                        catalogCode = "CV-%02d".format(i + 1),
                        tilt = tilt,
                        holoBoost = holoEnabled,
                        spinDegrees = lidOpen * 42f + d * 18f,
                        modifier = Modifier
                            .size(discDiameter)
                            .zIndex(1f)
                            .graphicsLayer {
                                val peek = if (focused) 0.14f + lidOpen * 0.34f else 0.04f
                                translationX = caseW.toPx() * peek
                            },
                    )

                    // 2) Inner Dark Polycarbonate Disc Tray + Spindle Rosette
                    JewelCaseTray(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(2f),
                    )

                    // 3) Clear Acrylic Hinged Front Cover Lid + Booklet + Corner Label
                    JewelCaseFrontLid(
                        item = item,
                        catalogCode = "CV-%02d".format(i + 1),
                        tilt = tilt,
                        sheenBias = d + lidOpen * 0.6f,
                        holoBoost = holoEnabled,
                        focused = focused,
                        targetWidthPx = targetPx,
                        input = input,
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(3f)
                            .graphicsLayer {
                                cameraDistance = 18f * density.density
                                transformOrigin = TransformOrigin(0.13f, 0.5f)
                                rotationY = -lidOpen * 54f
                            }
                            .then(
                                if (input.preview) Modifier
                                else Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        when {
                                            !focused -> track.animateTo(i, items.size)
                                            openedIdx != i -> {
                                                haptics.perform(HapticToken.TICK)
                                                openedIdx = i
                                            }
                                            else -> item.trigger(input)
                                        }
                                    },
                                ),
                            ),
                    )
                }
            }
        }
    }
}

/** Procedural 3D Holographic Compact Disc with dynamic gyro diffraction & concentric tracks. */
@Composable
private fun HolographicCdDisc(
    catalogCode: String,
    tilt: DeviceTilt,
    holoBoost: Boolean,
    spinDegrees: Float,
    modifier: Modifier = Modifier,
) {
    val colors = CivilightTheme.colors
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val holoAlpha = if (holoBoost) 0.56f else 0.34f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFE7EBF0), Color(0xFFBFC8D4), Color(0xFFD5DCE4), Color(0xFF9EA7B3)),
                    center = center,
                    radius = r,
                ),
                radius = r,
                center = center,
            )

            rotate(degrees = spinDegrees + tilt.x * 46f + tilt.y * 24f, pivot = center) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colorStops = arrayOf(
                            0.00f to Color.White.copy(alpha = holoAlpha * 0.9f),
                            0.12f to Color(0xFF8FB8E6).copy(alpha = holoAlpha),
                            0.24f to Color(0xFFC4A1F5).copy(alpha = holoAlpha * 0.95f),
                            0.36f to Color(0xFFF3B5C8).copy(alpha = holoAlpha * 0.85f),
                            0.48f to Color(0xFFF2D694).copy(alpha = holoAlpha * 0.75f),
                            0.60f to Color(0xFF9DE2D0).copy(alpha = holoAlpha * 0.9f),
                            0.74f to Color.White.copy(alpha = holoAlpha),
                            0.88f to Color(0xFF8FB8E6).copy(alpha = holoAlpha * 0.9f),
                            1.00f to Color.White.copy(alpha = holoAlpha * 0.9f),
                        ),
                        center = center,
                    ),
                    radius = r * 0.97f,
                    center = center,
                )
            }

            for (step in 5..9) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.11f),
                    radius = r * (step / 10f),
                    center = center,
                    style = Stroke(width = 0.7.dp.toPx()),
                )
            }

            drawCircle(Color(0xFF20242B).copy(alpha = 0.28f), radius = r * 0.34f, center = center, style = Stroke(width = 1.dp.toPx()))
            drawCircle(Color.White.copy(alpha = 0.36f), radius = r * 0.29f, center = center)
            drawCircle(Color(0xFF14161A).copy(alpha = 0.32f), radius = r * 0.25f, center = center, style = Stroke(width = 1.2.dp.toPx()))

            drawCircle(colors.bg, radius = r * 0.14f, center = center)
            drawCircle(Color.White.copy(alpha = 0.55f), radius = r * 0.14f, center = center, style = Stroke(width = 1.dp.toPx()))
            drawCircle(Color.White.copy(alpha = 0.55f), radius = r - 0.8.dp.toPx(), center = center, style = Stroke(width = 1.2.dp.toPx()))
        }
        Column(
            Modifier.align(Alignment.TopCenter).padding(top = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CivilightText(
                "$catalogCode · 74M",
                TextStyle(fontSize = 8.sp, lineHeight = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                color = Color(0xFF181C22).copy(alpha = 0.72f),
                maxLines = 1,
            )
        }
    }
}

/** Dark charcoal inner polycarbonate tray + 12-tooth center spindle rosette + left ribbed spine. */
@Composable
private fun JewelCaseTray(modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    Canvas(modifier.clip(RoundedCornerShape(8.dp))) {
        val spineW = size.width * 0.13f
        drawRect(
            color = if (colors.isDark) Color(0xFF131519) else Color(0xFF23262C),
            topLeft = Offset(spineW, 0f),
            size = Size(size.width - spineW, size.height),
        )
        val wellCenter = Offset(spineW + (size.width - spineW) * 0.5f, size.height * 0.5f)
        val wellR = size.height * 0.44f
        drawCircle(Color.Black.copy(alpha = 0.45f), radius = wellR, center = wellCenter)
        drawCircle(Color.White.copy(alpha = 0.08f), radius = wellR, center = wellCenter, style = Stroke(1.dp.toPx()))
        for (t in 0 until 12) {
            val a = (t / 12f) * 2f * PI.toFloat()
            val p0 = Offset(wellCenter.x + cos(a) * wellR * 0.12f, wellCenter.y + sin(a) * wellR * 0.12f)
            val p1 = Offset(wellCenter.x + cos(a) * wellR * 0.24f, wellCenter.y + sin(a) * wellR * 0.24f)
            drawLine(Color.White.copy(alpha = 0.22f), p0, p1, strokeWidth = 1.4.dp.toPx())
        }

        drawRect(
            brush = Brush.horizontalGradient(
                listOf(
                    Color.White.copy(alpha = 0.22f),
                    Color.White.copy(alpha = 0.07f),
                    Color.White.copy(alpha = 0.16f),
                ),
                startX = 0f,
                endX = spineW,
            ),
            topLeft = Offset.Zero,
            size = Size(spineW, size.height),
        )
        val grooves = 12
        for (g in 1 until grooves) {
            val gx = (spineW / grooves) * g
            drawLine(
                color = Color.White.copy(alpha = if (g % 2 == 0) 0.14f else 0.05f),
                start = Offset(gx, 4.dp.toPx()),
                end = Offset(gx, size.height - 4.dp.toPx()),
                strokeWidth = 0.8.dp.toPx(),
            )
        }
        val pinColor = Color.White.copy(alpha = 0.38f)
        drawRoundRect(pinColor, Offset(spineW - 3.dp.toPx(), 10.dp.toPx()), Size(5.dp.toPx(), 14.dp.toPx()), CornerRadius(2.dp.toPx()))
        drawRoundRect(pinColor, Offset(spineW - 3.dp.toPx(), size.height - 24.dp.toPx()), Size(5.dp.toPx(), 14.dp.toPx()), CornerRadius(2.dp.toPx()))

        drawRoundRect(
            color = Color.White.copy(alpha = 0.34f),
            cornerRadius = CornerRadius(8.dp.toPx()),
            style = Stroke(width = 1.4.dp.toPx()),
        )
    }
}

/** Hinged clear acrylic front cover with printed booklet, adhesive corner label, and dynamic glass reflections. */
@Composable
private fun JewelCaseFrontLid(
    item: ShelfCardItem,
    catalogCode: String,
    tilt: DeviceTilt,
    sheenBias: Float,
    holoBoost: Boolean,
    focused: Boolean,
    targetWidthPx: Int,
    input: ShelfViewInput,
    modifier: Modifier = Modifier,
) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(8.dp)
    val unhideMod = if (item is ShelfCardItem.Project && item.card.hiddenOpen && input.unhide != null) {
        Modifier.unhide(input.unhide)
    } else Modifier

    Box(
        modifier
            .then(unhideMod)
            .clip(shape)
            .drawWithContent {
                drawContent()
                val spineW = size.width * 0.13f
                val shift = ((sheenBias * 0.4f) + (tilt.x * 0.35f)).coerceIn(-0.8f, 0.8f)
                val x0 = spineW + size.width * (0.14f + shift)
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = if (focused) 0.24f else 0.12f),
                            Color.White.copy(alpha = 0.04f),
                            Color.White.copy(alpha = if (focused) 0.13f else 0.06f),
                            Color.Transparent,
                        ),
                        start = Offset(x0, 0f),
                        end = Offset(x0 + size.width * 0.42f, size.height),
                    ),
                )
                if (holoBoost) {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF8FB8E6).copy(alpha = 0.18f),
                                Color(0xFFD6A8F5).copy(alpha = 0.14f),
                                Color(0xFF9EE4D2).copy(alpha = 0.14f),
                                Color.Transparent,
                            ),
                            start = Offset(x0 - 20f, size.height * 0.15f),
                            end = Offset(x0 + size.width * 0.55f, size.height * 0.85f),
                        ),
                    )
                }
                val tab = Color.White.copy(alpha = 0.42f)
                drawRect(tab, Offset(spineW + 18.dp.toPx(), 2.dp.toPx()), Size(18.dp.toPx(), 2.5.dp.toPx()))
                drawRect(tab, Offset(size.width - 36.dp.toPx(), 2.dp.toPx()), Size(18.dp.toPx(), 2.5.dp.toPx()))
                drawRect(tab, Offset(spineW + 18.dp.toPx(), size.height - 4.5.dp.toPx()), Size(18.dp.toPx(), 2.5.dp.toPx()))
                drawRect(tab, Offset(size.width - 36.dp.toPx(), size.height - 4.5.dp.toPx()), Size(18.dp.toPx(), 2.5.dp.toPx()))
                drawRoundRect(
                    color = if (focused) colors.acc.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.30f),
                    cornerRadius = CornerRadius(8.dp.toPx()),
                    style = Stroke(width = if (focused) 1.5.dp.toPx() else 1.dp.toPx()),
                )
            },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(start = 28.dp, top = 4.dp, end = 4.dp, bottom = 4.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(colors.raise),
        ) {
            when (item) {
                is ShelfCardItem.Project -> {
                    CoverArt(item.card.cover, targetWidth = targetWidthPx)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(86.dp)
                            .align(Alignment.BottomCenter)
                            .background(Brush.verticalGradient(listOf(Color.Transparent, FixedColors.imageShadeBottom))),
                    )
                    Box(
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFF3EFE6).copy(alpha = 0.92f))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    ) {
                        CivilightText(
                            buildString {
                                append(catalogCode)
                                item.card.row.format?.let { append(" · "); append(AtelierCopy.formatTag(it)) }
                            },
                            TextStyle(fontSize = 9.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                            color = Color(0xFF1A1C20),
                            maxLines = 1,
                        )
                    }
                    Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                        CivilightText(
                            item.card.row.title,
                            TextStyle(fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold),
                            color = FixedColors.onImage,
                            maxLines = 2,
                        )
                        CivilightText(item.card.row.meta, CivilightTheme.type.micro, color = FixedColors.onImage2, maxLines = 1)
                    }
                }
                is ShelfCardItem.Lock -> Box(Modifier.fillMaxSize().background(colors.chip), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CivilightIcon(IconId.Lock, null, size = 26.dp, tint = colors.ink2)
                        CivilightText(item.group.title, CivilightTheme.type.heading, color = colors.ink, maxLines = 1)
                    }
                }
                ShelfCardItem.NewProject -> Box(Modifier.fillMaxSize().background(colors.tint), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CivilightIcon(IconId.Plus, null, size = 26.dp, tint = colors.acc)
                        CivilightText(AtelierCopy.NEW_PROJECT, CivilightTheme.type.heading, color = colors.ink, maxLines = 1)
                    }
                }
            }
        }
    }
}
