package io.github.lenticualrstone.civilight.feature.library.shelf.atelier

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.card.DeviceTilt
import io.github.lenticualrstone.civilight.designsystem.card.rememberTilt
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.glass.GlassBackground
import io.github.lenticualrstone.civilight.designsystem.glass.GlassSurface
import io.github.lenticualrstone.civilight.designsystem.glass.OnGlass
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.FixedColors
import io.github.lenticualrstone.civilight.designsystem.theme.HapticToken
import io.github.lenticualrstone.civilight.designsystem.theme.rememberHaptics
import io.github.lenticualrstone.civilight.designsystem.theme.unhide
import io.github.lenticualrstone.civilight.feature.library.shelf.CoverArt
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCardItem
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCards
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCopy
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfViewInput
import io.github.lenticualrstone.civilight.model.app.LibraryStyle
import io.github.lenticualrstone.civilight.model.project.ProjectFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** Terse Korean copy for all 10 Atelier library views (CopyLint-verified). */
object AtelierCopy {
    const val GROUP_SPATIAL = "공간 · 3D"
    const val GROUP_SPATIAL_SHORT = "3D"
    const val GROUP_KINETIC = "모션 · 작업"
    const val GROUP_KINETIC_SHORT = "작업"

    const val GLASS_PRISM = "유리 프리즘"
    const val ARCHIVE_VAULT = "깊이 서고"
    const val BOOK_SPINE_3D = "양장 서가"
    const val ORBIT_STAGE = "궤도 무대"
    const val FOLIO_SPREAD = "화첩 펼침"
    const val KINETIC_DECK = "탄성 덱"
    const val BENTO_MOSAIC = "모자이크"
    const val FILM_REEL = "필름 릴"
    const val PULSE_TIMELINE = "시간 축"
    const val STUDIO_LIST = "스튜디오 목록"

    const val FRAG_GLASS_PRISM = "3D 굴절 · 회전 프리즘"
    const val FRAG_ARCHIVE_VAULT = "3D 원근 통로 · 깊이 이동"
    const val FRAG_BOOK_SPINE_3D = "3D 양장 책등 · 꺼내 펼침"
    const val FRAG_ORBIT_STAGE = "3D 타원 궤도 · 스포트라이트"
    const val FRAG_FOLIO_SPREAD = "3D 제본 축 · 표지 젖힘"
    const val FRAG_KINETIC_DECK = "스프링 물리 · 부채꼴 덱"
    const val FRAG_BENTO_MOSAIC = "에디토리얼 격자 · 포커스"
    const val FRAG_FILM_REEL = "35mm 필름 · 내부 패럴랙스"
    const val FRAG_PULSE_TIMELINE = "맥동 노드 · 작업 연대기"
    const val FRAG_STUDIO_LIST = "모션 목록 · 빠른 작업"

    const val OPEN = "열기"
    const val UNLOCK = "잠금 풀기"
    const val NEW_PROJECT = "새 프로젝트"
    const val LOCKED_PROJECT = "잠긴 프로젝트"
    const val RECENT_FILES = "최근 연 파일"
    const val MENU = "메뉴"
    const val FORMAT_SERIAL = "연재"
    const val FORMAT_SHORT = "단편"
    const val FORMAT_SETTING = "설정"

    val all: List<String> = listOf(
        GROUP_SPATIAL, GROUP_SPATIAL_SHORT, GROUP_KINETIC, GROUP_KINETIC_SHORT,
        GLASS_PRISM, ARCHIVE_VAULT, BOOK_SPINE_3D, ORBIT_STAGE, FOLIO_SPREAD,
        KINETIC_DECK, BENTO_MOSAIC, FILM_REEL, PULSE_TIMELINE, STUDIO_LIST,
        FRAG_GLASS_PRISM, FRAG_ARCHIVE_VAULT, FRAG_BOOK_SPINE_3D, FRAG_ORBIT_STAGE, FRAG_FOLIO_SPREAD,
        FRAG_KINETIC_DECK, FRAG_BENTO_MOSAIC, FRAG_FILM_REEL, FRAG_PULSE_TIMELINE, FRAG_STUDIO_LIST,
        OPEN, UNLOCK, NEW_PROJECT, LOCKED_PROJECT, RECENT_FILES, MENU,
        FORMAT_SERIAL, FORMAT_SHORT, FORMAT_SETTING,
    )

    fun label(style: LibraryStyle): String = when (style) {
        LibraryStyle.GLASS_PRISM -> GLASS_PRISM
        LibraryStyle.ARCHIVE_VAULT -> ARCHIVE_VAULT
        LibraryStyle.BOOK_SPINE_3D -> BOOK_SPINE_3D
        LibraryStyle.ORBIT_STAGE -> ORBIT_STAGE
        LibraryStyle.FOLIO_SPREAD -> FOLIO_SPREAD
        LibraryStyle.KINETIC_DECK -> KINETIC_DECK
        LibraryStyle.BENTO_MOSAIC -> BENTO_MOSAIC
        LibraryStyle.FILM_REEL -> FILM_REEL
        LibraryStyle.PULSE_TIMELINE -> PULSE_TIMELINE
        LibraryStyle.LIST -> STUDIO_LIST
    }

    fun frag(style: LibraryStyle): String = when (style) {
        LibraryStyle.GLASS_PRISM -> FRAG_GLASS_PRISM
        LibraryStyle.ARCHIVE_VAULT -> FRAG_ARCHIVE_VAULT
        LibraryStyle.BOOK_SPINE_3D -> FRAG_BOOK_SPINE_3D
        LibraryStyle.ORBIT_STAGE -> FRAG_ORBIT_STAGE
        LibraryStyle.FOLIO_SPREAD -> FRAG_FOLIO_SPREAD
        LibraryStyle.KINETIC_DECK -> FRAG_KINETIC_DECK
        LibraryStyle.BENTO_MOSAIC -> FRAG_BENTO_MOSAIC
        LibraryStyle.FILM_REEL -> FRAG_FILM_REEL
        LibraryStyle.PULSE_TIMELINE -> FRAG_PULSE_TIMELINE
        LibraryStyle.LIST -> FRAG_STUDIO_LIST
    }

    fun formatTag(format: ProjectFormat): String = when (format) {
        ProjectFormat.SERIAL -> FORMAT_SERIAL
        ProjectFormat.SHORT -> FORMAT_SHORT
        ProjectFormat.SETTING_ONLY -> FORMAT_SETTING
    }
}

/**
 * Continuous spring-physics position state shared by the spatial & kinetic Atelier stages.
 * Keeps drag deltas in stationary parent coordinates (zero jitter) and fires a subtle haptic
 * tick when crossing an item index.
 */
@Stable
internal class AtelierTrackState(
    initialIndex: Int,
    private val scope: CoroutineScope,
    private val reduced: Boolean,
    private val onTick: () -> Unit,
) {
    val position = Animatable(initialIndex.toFloat())
    var lastHapticIndex by mutableIntStateOf(initialIndex)
        private set

    val activeIndex: Int
        get() = position.value.roundToInt()

    fun dragBy(deltaItems: Float, count: Int) {
        if (count <= 0) return
        val max = (count - 1).toFloat()
        val next = (position.value + deltaItems).coerceIn(-0.35f, max + 0.35f)
        scope.launch { position.snapTo(next) }
        val crossed = next.roundToInt().coerceIn(0, count - 1)
        if (crossed != lastHapticIndex) {
            lastHapticIndex = crossed
            onTick()
        }
    }

    fun settle(count: Int, velocityBias: Float = 0f) {
        if (count <= 0) return
        val target = (position.value + velocityBias).roundToInt().coerceIn(0, count - 1)
        animateTo(target, count)
    }

    fun animateTo(index: Int, count: Int) {
        if (count <= 0) return
        val target = index.coerceIn(0, count - 1)
        lastHapticIndex = target
        scope.launch {
            if (reduced) {
                position.snapTo(target.toFloat())
            } else {
                position.animateTo(
                    target.toFloat(),
                    CivilightMotion.spec(CivilightMotion.swipe, reduced = false),
                )
            }
        }
    }
}

@Composable
internal fun rememberAtelierTrack(
    items: List<ShelfCardItem>,
    selectedKey: String?,
    preview: Boolean,
): AtelierTrackState {
    val scope = rememberCoroutineScope()
    val reduced = CivilightTheme.motion.reduced
    val haptics = rememberHaptics()
    val initial = remember(items.size) {
        selectedKey?.let { key -> items.indexOfFirst { (it as? ShelfCardItem.Project)?.card?.row?.key == key }.takeIf { it >= 0 } } ?: 0
    }
    val state = remember(reduced, preview) {
        AtelierTrackState(initial, scope, reduced) {
            if (!preview) haptics.perform(HapticToken.TICK)
        }
    }
    LaunchedEffect(selectedKey, items.size) {
        if (selectedKey != null && items.isNotEmpty()) {
            val idx = items.indexOfFirst { (it as? ShelfCardItem.Project)?.card?.row?.key == selectedKey }
            if (idx >= 0 && idx != state.activeIndex) state.animateTo(idx, items.size)
        } else if (items.isNotEmpty() && state.activeIndex >= items.size) {
            state.animateTo(items.lastIndex, items.size)
        }
    }
    return state
}

internal fun Modifier.atelierHorizontalDrag(
    enabled: Boolean,
    count: Int,
    pxPerItem: Float,
    track: AtelierTrackState,
): Modifier {
    if (!enabled || count <= 1) return this
    return this.pointerInput(count, pxPerItem, track) {
        var lastDelta = 0f
        detectHorizontalDragGestures(
            onDragEnd = {
                val bias = (-lastDelta / pxPerItem.coerceAtLeast(1f) * 2.2f).coerceIn(-0.85f, 0.85f)
                track.settle(count, bias)
                lastDelta = 0f
            },
            onDragCancel = {
                track.settle(count, 0f)
                lastDelta = 0f
            },
            onHorizontalDrag = { change, dragAmount ->
                change.consume()
                lastDelta = dragAmount
                track.dragBy(-dragAmount / pxPerItem.coerceAtLeast(1f), count)
            },
        )
    }
}

internal fun Modifier.atelierVerticalDrag(
    enabled: Boolean,
    count: Int,
    pxPerItem: Float,
    track: AtelierTrackState,
): Modifier {
    if (!enabled || count <= 1) return this
    return this.pointerInput(count, pxPerItem, track) {
        var lastDelta = 0f
        detectVerticalDragGestures(
            onDragEnd = {
                val bias = (-lastDelta / pxPerItem.coerceAtLeast(1f) * 2.2f).coerceIn(-0.85f, 0.85f)
                track.settle(count, bias)
                lastDelta = 0f
            },
            onDragCancel = {
                track.settle(count, 0f)
                lastDelta = 0f
            },
            onVerticalDrag = { change, dragAmount ->
                change.consume()
                lastDelta = dragAmount
                track.dragBy(-dragAmount / pxPerItem.coerceAtLeast(1f), count)
            },
        )
    }
}

/** Builds the masked list of ShelfCardItems for Atelier views. */
internal fun atelierItems(input: ShelfViewInput): List<ShelfCardItem> =
    ShelfCards.cardItems(input.ui, wide = true)

internal fun ShelfCardItem.trigger(input: ShelfViewInput) {
    when (this) {
        is ShelfCardItem.Project -> input.onOpen(card.row.key)
        is ShelfCardItem.Lock -> input.onUnlock()
        ShelfCardItem.NewProject -> input.onNewProject()
    }
}

internal fun ShelfCardItem.displayTitle(): String = when (this) {
    is ShelfCardItem.Project -> card.row.title
    is ShelfCardItem.Lock -> group.title
    ShelfCardItem.NewProject -> AtelierCopy.NEW_PROJECT
}

internal fun ShelfCardItem.displayMeta(): String = when (this) {
    is ShelfCardItem.Project -> card.row.meta
    is ShelfCardItem.Lock -> group.cardSub
    ShelfCardItem.NewProject -> AtelierCopy.NEW_PROJECT
}

/**
 * Shared Atelier stage wrapper: subtle ambient glow backdrop, gyro tilt provider,
 * main interactive viewport, and floating glass focus dock at the bottom.
 */
@Composable
internal fun AtelierStageFrame(
    input: ShelfViewInput,
    items: List<ShelfCardItem>,
    activeIndex: Int,
    showDock: Boolean = true,
    content: @Composable BoxScope.(tilt: DeviceTilt) -> Unit,
) {
    val colors = CivilightTheme.colors
    val tilt = rememberTilt(enabled = input.tilt && !input.preview && !CivilightTheme.motion.reduced)
    val safeIndex = activeIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    val activeItem = items.getOrNull(safeIndex)
    val inertModifier = if (input.preview) Modifier.clearAndSetSemantics { } else Modifier

    Box(Modifier.fillMaxSize().background(colors.bg).then(inertModifier)) {
        // Soft atmospheric ambient light behind every Atelier scene.
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width * (0.5f + tilt.x * 0.08f)
            val cy = size.height * (0.42f + tilt.y * 0.06f)
            val radius = size.minDimension * 0.72f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors.acc.copy(alpha = if (colors.isDark) 0.10f else 0.07f),
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(cx, cy),
            )
        }

        Box(Modifier.fillMaxSize()) {
            content(tilt)
        }

        if (showDock && activeItem != null) {
            AtelierFocusDock(
                item = activeItem,
                index = safeIndex,
                total = items.size,
                input = input,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = (input.barBottom + 10f).dp,
                    ),
            )
        }
    }
}

/** Floating glass focus dock showing active item metadata and instant Open / Menu actions. */
@Composable
private fun AtelierFocusDock(
    item: ShelfCardItem,
    index: Int,
    total: Int,
    input: ShelfViewInput,
    modifier: Modifier = Modifier,
) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .insetOutline(colors.line, 1.dp, shape),
    ) {
        GlassBackground(GlassSurface.CHIP, Modifier.matchParentSize(), shape = shape)
        OnGlass {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AnimatedContent(
                        targetState = item.key to (item.displayTitle() to item.displayMeta()),
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        modifier = Modifier.weight(1f),
                        label = "atelierDockText",
                    ) { (_, pair) ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            CivilightText(
                                pair.first,
                                CivilightTheme.type.heading,
                                color = colors.ink,
                                maxLines = 1,
                            )
                            CivilightText(
                                pair.second,
                                CivilightTheme.type.small,
                                color = colors.ink3,
                                maxLines = 1,
                            )
                        }
                    }
                    if (!input.preview) {
                        val actionLabel = when (item) {
                            is ShelfCardItem.Project -> AtelierCopy.OPEN
                            is ShelfCardItem.Lock -> AtelierCopy.UNLOCK
                            ShelfCardItem.NewProject -> AtelierCopy.NEW_PROJECT
                        }
                        CivilightButton(
                            text = actionLabel,
                            onClick = { item.trigger(input) },
                            variant = ButtonVariant.PRIMARY,
                        )
                        if (item is ShelfCardItem.Project) {
                            CivilightIconButton(
                                icon = IconId.More,
                                contentDescription = AtelierCopy.MENU,
                                onClick = { input.onMenu(item.card.row.key) },
                                tint = colors.ink2,
                            )
                        }
                    }
                }
                // Minimal progress dots + recents peek row
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        val shownDots = total.coerceAtMost(9)
                        repeat(shownDots) { i ->
                            val on = i == (index % shownDots)
                            Box(
                                Modifier
                                    .size(width = if (on) 14.dp else 5.dp, height = 5.dp)
                                    .clip(CircleShape)
                                    .background(if (on) colors.acc else colors.ink3.copy(alpha = 0.35f)),
                            )
                        }
                        CivilightText(
                            "${index + 1} / $total",
                            CivilightTheme.type.number,
                            color = colors.ink3,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                    val recents = input.onRecents
                    if (recents != null && !input.preview) {
                        CivilightText(
                            AtelierCopy.RECENT_FILES,
                            CivilightTheme.type.small,
                            color = colors.ink2,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .pressable(onClick = recents, role = Role.Button)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * High-craft cover card used across the Atelier views:
 * - Renders CoverArt (with shared LRU cache + EXIF upright) or a minimalist typographic cover
 * - Adds a live specular light streak driven by [sheenBias] + [tilt]
 * - Draws a crisp foil rim highlight when [focused]
 * - Safely masks [ShelfCardItem.Lock] and renders a clean outlined [ShelfCardItem.NewProject] card
 */
@Composable
internal fun AtelierCoverCard(
    item: ShelfCardItem,
    tilt: DeviceTilt,
    focused: Boolean,
    sheenBias: Float,
    targetWidthPx: Int,
    input: ShelfViewInput,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    onClick: () -> Unit = {},
) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(cornerRadius)
    val unhideMod = if (item is ShelfCardItem.Project && item.card.hiddenOpen && input.unhide != null) {
        Modifier.unhide(input.unhide)
    } else {
        Modifier
    }

    Box(
        modifier
            .then(unhideMod)
            .clip(shape)
            .background(colors.raise)
            .then(
                if (input.preview) Modifier
                else Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
            )
            .drawWithContent {
                drawContent()
                // Specular sheen sweep driven by scroll angle + gyro tilt (zero recomposition).
                val shift = ((sheenBias * 0.45f) + (tilt.x * 0.35f)).coerceIn(-0.9f, 0.9f)
                val x0 = size.width * (0.25f + shift)
                val x1 = x0 + size.width * 0.52f
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = if (focused) 0.16f else 0.07f),
                            Color.Transparent,
                        ),
                        start = Offset(x0, 0f),
                        end = Offset(x1, size.height),
                    ),
                )
            }
            .insetOutline(
                color = if (focused) colors.acc.copy(alpha = 0.72f) else colors.line,
                width = if (focused) 1.5.dp else 1.dp,
                shape = shape,
            )
            .semantics {
                if (!input.preview) contentDescription = "${item.displayTitle()} · ${item.displayMeta()}"
            },
    ) {
        when (item) {
            is ShelfCardItem.Project -> {
                CoverArt(item.card.cover, targetWidth = targetWidthPx)
                // Editorial bottom gradient scrim for legible title overlay
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, FixedColors.imageShadeBottom),
                            ),
                        ),
                )
                Column(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    item.card.chip?.let { chip ->
                        CivilightText(
                            chip,
                            CivilightTheme.type.micro,
                            color = FixedColors.onImage2,
                            maxLines = 1,
                        )
                    }
                    CivilightText(
                        item.card.row.title,
                        TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold),
                        color = FixedColors.onImage,
                        maxLines = 2,
                    )
                    CivilightText(
                        item.card.row.meta,
                        CivilightTheme.type.micro,
                        color = FixedColors.onImage2,
                        maxLines = 1,
                    )
                }
            }
            is ShelfCardItem.Lock -> {
                Box(Modifier.fillMaxSize().background(colors.chip), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(16.dp),
                    ) {
                        CivilightIcon(IconId.Lock, contentDescription = null, size = 28.dp, tint = colors.ink2)
                        CivilightText(item.group.title, CivilightTheme.type.heading, color = colors.ink, maxLines = 1)
                        CivilightText(item.group.cardSub, CivilightTheme.type.small, color = colors.ink3, maxLines = 1)
                    }
                }
            }
            ShelfCardItem.NewProject -> {
                Box(Modifier.fillMaxSize().background(colors.tint), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CivilightIcon(IconId.Plus, contentDescription = null, size = 28.dp, tint = colors.acc)
                        CivilightText(AtelierCopy.NEW_PROJECT, CivilightTheme.type.heading, color = colors.ink, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Deterministic accent hue tint for hardcover spines & timeline accents derived from project key. */
internal fun spineTone(key: String, isDark: Boolean): Color {
    val palette = if (isDark) {
        listOf(
            Color(0xFF233246),
            Color(0xFF3A2A24),
            Color(0xFF24352B),
            Color(0xFF2E293D),
            Color(0xFF333025),
            Color(0xFF1F3238),
        )
    } else {
        listOf(
            Color(0xFFD6E0EC),
            Color(0xFFE7D8CE),
            Color(0xFFD4E2D8),
            Color(0xFFDDD8EA),
            Color(0xFFE5DFCE),
            Color(0xFFD1E2E6),
        )
    }
    val idx = abs(key.hashCode()) % palette.size
    return palette[idx]
}
