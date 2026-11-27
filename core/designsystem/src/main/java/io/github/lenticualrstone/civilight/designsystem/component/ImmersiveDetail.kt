package io.github.lenticualrstone.civilight.designsystem.component

import android.os.Build
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.glass.GlassBackground
import io.github.lenticualrstone.civilight.designsystem.glass.GlassMode
import io.github.lenticualrstone.civilight.designsystem.glass.GlassSource
import io.github.lenticualrstone.civilight.designsystem.glass.GlassSurface
import io.github.lenticualrstone.civilight.designsystem.glass.OnGlass
import io.github.lenticualrstone.civilight.designsystem.glass.glassSource
import io.github.lenticualrstone.civilight.designsystem.glass.rememberGlassSource
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.FixedColors
import io.github.lenticualrstone.civilight.designsystem.theme.HapticToken
import io.github.lenticualrstone.civilight.designsystem.theme.LocalGlassMode
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import io.github.lenticualrstone.civilight.designsystem.theme.animatedFraction
import io.github.lenticualrstone.civilight.designsystem.theme.rememberHaptics

/** Accessibility names of the immersive header buttons (same words as the rest of the app). */
object ImmersiveDetailCopy {
    const val BACK = "뒤로"
    const val MORE = "메뉴"
    val all: List<String> = listOf(BACK, MORE)
}

/**
 * Scroll state of [ImmersiveDetailScaffold]. Read [progress], [easedProgress] and [headerAlpha] only inside
 * `graphicsLayer` / draw lambdas or `derivedStateOf` (they change every scroll frame); [headerShown] and
 * [titleDocked] are already derived and safe to read in composition.
 */
@Stable
class ImmersiveDetailState(val scrollState: ScrollState) {
    internal var viewportHeightPx by mutableFloatStateOf(0f)
    internal var dockDistancePx by mutableFloatStateOf(0f)
    internal var reduced by mutableStateOf(false)

    private val activeDistancePx: Float
        get() = if (dockDistancePx > 0f) dockDistancePx else ImmersiveMath.distance(viewportHeightPx)

    /** All driving values of the current frame (p, e, hp, q). Reduced motion: e and hp step at p 0.5. */
    val frame: ImmersiveFrame
        get() = ImmersiveFrame.atDistance(scrollState.value.toFloat(), activeDistancePx, reduced)

    /** p = clamp(scroll / D, 0, 1), D = panelStartY - headerHeight. */
    val progress: Float get() = ImmersiveMath.progressForDistance(scrollState.value.toFloat(), activeDistancePx)

    /** smoothstep(p) (hero blur, tone, panel); a 0 / 1 step under reduced motion. */
    val easedProgress: Float get() = frame.e

    /** Header glass alpha, p 0.5 … 0.9. */
    val headerAlpha: Float get() = frame.hp

    /** D in px (0 until laid out). */
    val distancePx: Float get() = activeDistancePx

    /** True once the header glass starts to show (p > 0.5). */
    val headerShown: Boolean by derivedStateOf { frame.hp > 0f }

    /** True when the title sits in the header (p = 1, or p ≥ 0.5 with reduced motion). */
    val titleDocked: Boolean by derivedStateOf { frame.e >= 1f }
}

@Composable
fun rememberImmersiveDetailState(scrollState: ScrollState = rememberScrollState()): ImmersiveDetailState =
    remember(scrollState) { ImmersiveDetailState(scrollState) }

/** What header buttons inside the scaffold read: the header alpha, backdrop blur source, and whether hero has art. */
private class ImmersiveChrome(val headerAlpha: () -> Float, val source: GlassSource?, val hasHeroArt: Boolean)

private val LocalImmersiveChrome = staticCompositionLocalOf<ImmersiveChrome?> { null }

/** Black of the over-image shade and button circle (board `rgba(0,0,0,…)`; no theme token). */
private val ImageDark = Color.Black

/**
 * Immersive detail screen (`몰입 상세`, `Immersive.dc.html` / `ImmSpec.dc.html`).
 *
 * - [hero]: full-bleed art pinned behind everything, in a `fillMaxSize` box. Pass an image cropped to fill
 *   with top alignment (`ContentScale.Crop`, `Alignment.TopCenter`), or a colour field. The scaffold scales it
 *   1 → 1.08 and blurs it 0 → 22 → 48 dp as the page scrolls; tone and bottom shade are drawn over it.
 * - [title] / [subtitle]: bottom-left over the art; the title docks into the 56 dp header row (34 → 17,
 *   40 → 17 above 600 dp width), the subtitle fades out and never docks.
 * - [content]: the glass panel that starts just below the fold (top padding 28, side gutter 24 / 32 unless
 *   [contentGutter] is false, bottom padding for the bar and navigation inset).
 * - [actions]: header end, before the optional `메뉴` button ([onMore]). Use [ImmersiveHeaderButton] there.
 * - [bottomBar]: tabs (e.g. `CivilightTabs`) on the pinned bottom glass, gutter-inset, 6 dp above the inset.
 * - [onBack]: null draws no back button and leaves no slot for it (the docked title then starts at the gutter);
 *   [backLabel] is the button's accessibility name.
 * - [bottomInset]: replaces the navigation-bar inset (title block, panel padding, bar) when the screen already
 *   reserves the space below, so it is not added twice; null = the navigation-bar inset.
 * - [onHeroClick]: a tap on the art that is still uncovered (the first viewport above the panel) reaches it;
 *   vertical drags there still scroll. [heroLabel] is its accessibility name (a button).
 * - [onTitleClick]: the title is a tap target (44 dp at least, following its move and scale) while it rests over
 *   the art; docked in the header it is plain text again. [titleClickLabel] names the action for TalkBack.
 * - [onSubtitleClick]: the subtitle line likewise (44 dp tall at least) while it shows; [subtitleClickLabel].
 * - [heroTargets]: tap targets pinned over the art in hero coordinates (same box as [hero]), e.g. a card drawn
 *   by the hero; they sit above [onHeroClick] and below the panel, and scrolling still works over them.
 */
@Composable
fun ImmersiveDetailScaffold(
    title: String,
    subtitle: String?,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    hero: @Composable BoxScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: (@Composable () -> Unit)? = null,
    state: ImmersiveDetailState = rememberImmersiveDetailState(),
    onMore: (() -> Unit)? = null,
    contentGutter: Boolean = true,
    backLabel: String = ImmersiveDetailCopy.BACK,
    bottomInset: Dp? = null,
    hasHeroArt: Boolean = true,
    onHeroClick: (() -> Unit)? = null,
    heroLabel: String? = null,
    onTitleClick: (() -> Unit)? = null,
    titleClickLabel: String? = null,
    onSubtitleClick: (() -> Unit)? = null,
    subtitleClickLabel: String? = null,
    heroTargets: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val blurTokens = CivilightTheme.blur
    val motion = LocalMotion.current
    val glassMode = LocalGlassMode.current
    val density = LocalDensity.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val canBlur = Build.VERSION.SDK_INT >= 31 && (glassMode == GlassMode.BLUR || glassMode == GlassMode.BLUR_COSTLY)
    val opaque = glassMode == GlassMode.OPAQUE
    val reduced = motion.reduced
    SideEffect { state.reduced = reduced }

    // Reduced motion: positions step at p 0.5; alpha and colour follow a 120 ms fade of that step.
    val stepOn by remember(state) { derivedStateOf { ImmersiveMath.snapped(state.progress) >= 1f } }
    val stepFade = animatedFraction(if (reduced) stepOn else false, label = "immersiveStep")
    val fadeE: () -> Float = remember(state, reduced, stepFade) { if (reduced) ({ stepFade.value }) else ({ state.frame.e }) }
    val hp: () -> Float = remember(state, reduced, stepFade) { if (reduced) ({ stepFade.value }) else ({ state.frame.hp }) }
    val pFade: () -> Float = remember(state, reduced, stepFade) { if (reduced) ({ stepFade.value }) else ({ state.progress }) }
    val showHeaderGlass by remember(state, reduced) { derivedStateOf { hp() > 0f } }

    val statusPx = WindowInsets.statusBars.getTop(density)
    val navPx = WindowInsets.navigationBars.getBottom(density)
    val statusDp = with(density) { statusPx.toDp() }
    // The screen may already reserve the space below (a pane padded by the frame bar): use its value, not twice.
    val navDp = ImmersiveMath.effectiveBottomInset(with(density) { navPx.toDp() }.value, bottomInset?.value).dp
    val hasBack = onBack != null
    val hasBar = bottomBar != null
    val hasSubtitle = !subtitle.isNullOrEmpty()
    val externalBottomBar = bottomInset != null && bottomInset > 0.dp

    BoxWithConstraints(modifier.fillMaxSize().background(colors.bg)) {
        val hPx = constraints.maxHeight.toFloat()
        val viewportH = maxHeight
        val panelStartDp = ImmersiveMath.panelStartY(maxHeight.value, navDp.value, hasBar).dp
        val panelStartPx = with(density) { panelStartDp.toPx() }
        val dockDp = ImmersiveMath.dockDistance(maxHeight.value, statusDp.value, navDp.value, hasBar)
        val dockPx = with(density) { dockDp.dp.toPx() }
        val wide = ImmersiveMath.isWide(maxWidth.value)
        val gutter = ImmersiveMath.gutterDp(wide).dp
        SideEffect {
            state.viewportHeightPx = hPx
            state.dockDistancePx = dockPx
        }

        // Spring detent tension in the hero <-> docked transition zone (0 < scroll < dockPx).
        var dragStartScroll by remember(state) { mutableFloatStateOf(0f) }
        var wasScrolling by remember(state) { mutableStateOf(false) }
        var flingSnapped by remember(state) { mutableStateOf(false) }
        val isScrolling = state.scrollState.isScrollInProgress
        LaunchedEffect(isScrolling, dockPx, reduced) {
            if (isScrolling && !wasScrolling) {
                dragStartScroll = state.scrollState.value.toFloat()
                flingSnapped = false
            } else if (!isScrolling && wasScrolling && !flingSnapped && dockPx > 1f) {
                val cur = state.scrollState.value.toFloat()
                if (cur > 1f && cur < dockPx - 1f) {
                    val target = ImmersiveMath.snapTarget(cur, dockPx, dragStartScroll, 0f).roundToInt()
                    if (target != state.scrollState.value) {
                        haptics.perform(HapticToken.TICK)
                        state.scrollState.animateScrollTo(target, motion.hingeTension(dampingRatio = 0.84f, stiffness = 420f))
                    }
                }
            }
            wasScrolling = isScrolling
        }
        val detentScroll = remember(state, dockPx, motion, haptics) {
            object : NestedScrollConnection {
                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (dockPx <= 1f) return Velocity.Zero
                    val cur = state.scrollState.value.toFloat()
                    if (cur <= 1f || cur >= dockPx - 1f) return Velocity.Zero
                    val scrollVel = -available.y
                    val target = ImmersiveMath.snapTarget(cur, dockPx, dragStartScroll, scrollVel).roundToInt()
                    flingSnapped = true
                    haptics.perform(HapticToken.TICK)
                    scope.launch {
                        state.scrollState.animateScrollTo(target, motion.hingeTension(dampingRatio = 0.84f, stiffness = 420f))
                    }
                    return available
                }
            }
        }

        val pageSource = rememberGlassSource()
        val heroSource = rememberGlassSource()

        // Title geometry, shared by the drawn title and its tap target (1:1 synced with the rising panel).
        val startSp = ImmersiveMath.titleStartSp(wide)
        var titleHeightPx by remember { mutableIntStateOf(0) }
        var titleWidthPx by remember { mutableIntStateOf(0) }
        val titleTappable = ImmersiveMath.titleTappable(onTitleClick != null, state.titleDocked)
        // The subtitle is a target while it shows (it fades out by p 0.32; reduced motion: until the step).
        val subtitleShown by remember(state, reduced) {
            derivedStateOf { if (reduced) ImmersiveMath.snapped(state.progress) < 1f else ImmersiveMath.subtitleAlpha(state.progress) > 0f }
        }
        val subtitleTappable = hasSubtitle && onSubtitleClick != null && subtitleShown
        var subtitleWidthPx by remember { mutableIntStateOf(0) }
        val subtitleBottomDp = maxHeight.value - ImmersiveMath.blockBottom(maxHeight.value, navDp.value, hasBar)
        val titleTransform: (androidx.compose.ui.graphics.GraphicsLayerScope, Float, Float) -> Unit = { layer, dxDp, dyDp ->
            with(layer) {
                val d = density.density
                val f = state.frame
                // Linear p tracks the rising panel 1:1 (no mid-scroll sprint or dead gap); reduced steps on e.
                val t = if (reduced) f.e else f.p
                val lh = titleHeightPx / d
                val hDp = hPx / d
                val y0 = ImmersiveMath.titleStartY(hDp, lh, navDp.value, hasBar, hasSubtitle)
                val y1 = ImmersiveMath.titleDockedY(statusDp.value, lh, wide)
                transformOrigin = TransformOrigin(0f, 0f)
                val s = ImmersiveMath.titleScale(t, wide)
                translationX = (ImmersiveMath.titleX(t, wide, hasBack) + dxDp * s) * d
                translationY = (ImmersiveMath.titleY(t, y0, y1) + dyDp * s) * d
                scaleX = s
                scaleY = s
            }
        }

        // ---- Page: hero group + scroller (recorded for the header, bar and buttons) ----
        Box(Modifier.fillMaxSize().glassSource(pageSource, enabled = canBlur)) {
            Box(Modifier.fillMaxSize().glassSource(heroSource, enabled = canBlur)) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .graphicsLayer {
                            val f = state.frame
                            val s = if (reduced) 1f else ImmersiveMath.heroScale(f.e)
                            scaleX = s
                            scaleY = s
                            transformOrigin = TransformOrigin(ImmersiveMath.HERO_ORIGIN_X, ImmersiveMath.HERO_ORIGIN_Y)
                            val r = ImmersiveMath.heroBlurDp(f.e, f.q, reduced, canBlur).dp.toPx()
                            renderEffect = if (r > 0.5f && Build.VERSION.SDK_INT >= 31) BlurEffect(r, r, TileMode.Clamp) else null
                        },
                    content = hero,
                )
                Spacer(
                    Modifier.fillMaxSize().drawBehind {
                        drawRect(colors.bg, alpha = ImmersiveMath.toneAlpha(fadeE(), canBlur))
                    },
                )
            }
            // Bottom shade sits OUTSIDE heroSource so the glass panel never blurs a dark smear into itself,
            // and only draws when hero art is present (never turning a coverless light surface into a gray wall).
            if (hasHeroArt) {
                val shade = remember {
                    val stops = ImmersiveMath.SHADE_STOPS.map { (at, a) -> (1f - at) to ImageDark.copy(alpha = a) }.sortedBy { it.first }
                    Brush.verticalGradient(*stops.toTypedArray())
                }
                Spacer(
                    Modifier.fillMaxSize().drawBehind {
                        val a = ImmersiveMath.shadeAlpha(pFade())
                        if (a > 0.001f) drawRect(shade, alpha = a)
                    },
                )
            }

            Column(Modifier.fillMaxSize().nestedScroll(detentScroll).verticalScroll(state.scrollState)) {
                // Hero spacer ends at panelStartDp so the glass panel rises immediately in lockstep with the title.
                Box(Modifier.fillMaxWidth().height(panelStartDp).clipToBounds()) {
                    if (onHeroClick != null || heroTargets != null || titleTappable || subtitleTappable) {
                        // Pinned like the hero: moved down by the scroll, so the targets stay over what they mark.
                        Box(Modifier.fillMaxWidth().height(viewportH).offset { IntOffset(0, state.scrollState.value) }) {
                            if (onHeroClick != null) {
                                // Not under the tab bar (its gaps and fade would otherwise open the art's picker).
                                val barDp = if (hasBar) ImmersiveMath.barHeight(navDp.value).dp else navDp
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height((viewportH - barDp).coerceAtLeast(0.dp))
                                        .pressable(onClick = onHeroClick, scaleOnPress = false, pressOverlay = false)
                                        .semantics { if (heroLabel != null) contentDescription = heroLabel },
                                )
                            }
                            heroTargets?.invoke(this)
                            if (subtitleTappable && onSubtitleClick != null && subtitleWidthPx > 0) {
                                val w = with(density) { subtitleWidthPx.toDp() }
                                val padY = ImmersiveMath.hitPadDp(ImmersiveMath.SUBTITLE_LINE_DP)
                                Box(
                                    Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(start = gutter, bottom = (subtitleBottomDp - padY).coerceAtLeast(0f).dp)
                                        .size(w, (ImmersiveMath.SUBTITLE_LINE_DP + padY * 2).dp)
                                        .graphicsLayer { if (!reduced) translationY = ImmersiveMath.subtitleShiftDp(state.progress).dp.toPx() }
                                        .clearAndSetSemantics { }
                                        .pressable(onClick = onSubtitleClick, role = null, scaleOnPress = false, pressOverlay = false),
                                )
                            }
                            if (titleTappable && onTitleClick != null && titleWidthPx > 0 && titleHeightPx > 0) {
                                val w = with(density) { titleWidthPx.toDp() }
                                val h = with(density) { titleHeightPx.toDp() }
                                val padX = ImmersiveMath.hitPadDp(w.value)
                                val padY = ImmersiveMath.hitPadDp(h.value)
                                Box(
                                    Modifier
                                        .size(w + (padX * 2).dp, h + (padY * 2).dp)
                                        .graphicsLayer { titleTransform(this, -padX, -padY) }
                                        .clearAndSetSemantics { }
                                        .pressable(onClick = onTitleClick, role = null, scaleOnPress = false, pressOverlay = false),
                                )
                            }
                        }
                    }
                }
                ImmersivePanel(
                    state = state,
                    viewportPx = hPx,
                    panelStartPx = panelStartPx,
                    minHeight = (viewportH - ImmersiveMath.headerHeight(statusDp.value).dp).coerceAtLeast(0.dp),
                    heroSource = heroSource,
                    canBlur = canBlur,
                    opaque = opaque,
                    fadeE = fadeE,
                    glassBlurPx = with(density) { blurTokens.glass.toPx() },
                    saturation = blurTokens.glassSaturation,
                    modifier = Modifier.padding(
                        top = ImmersiveMath.PANEL_TOP_PAD_DP.dp,
                        start = if (contentGutter) gutter else 0.dp,
                        end = if (contentGutter) gutter else 0.dp,
                        bottom = ImmersiveMath.panelBottomPadding(navDp.value, hasBar).dp,
                    ),
                    content = content,
                )
            }
        }

        // ---- Bottom glass bar ----
        if (bottomBar != null) {
            val barBoxModifier = if (externalBottomBar) {
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = navDp)
                    .fillMaxWidth()
                    .height((ImmersiveMath.BAR_ROW_DP + ImmersiveMath.BAR_FADE_DP).dp)
            } else {
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(ImmersiveMath.barHeight(navDp.value).dp)
            }
            val barContentBottom = if (externalBottomBar) ImmersiveMath.BAR_CONTENT_BOTTOM_DP.dp else navDp + ImmersiveMath.BAR_CONTENT_BOTTOM_DP.dp
            Box(barBoxModifier) {
                GlassBackground(GlassSurface.BOTTOM_BAR, Modifier.matchParentSize(), source = if (canBlur) pageSource else null)
                OnGlass {
                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(start = gutter, end = gutter, bottom = barContentBottom),
                    ) { bottomBar() }
                }
            }
        }

        // ---- Header: glass (p 0.5 … 0.9) + back / actions ----
        Box(Modifier.align(Alignment.TopStart).fillMaxWidth().height(ImmersiveMath.headerHeight(statusDp.value).dp)) {
            if (showHeaderGlass) {
                GlassBackground(
                    GlassSurface.TOP_BAR,
                    Modifier.matchParentSize().graphicsLayer { alpha = hp() },
                    source = if (canBlur) pageSource else null,
                )
            }
            val chrome = remember(hp, pageSource, canBlur, hasHeroArt) { ImmersiveChrome(hp, if (canBlur) pageSource else null, hasHeroArt) }
            CompositionLocalProvider(LocalImmersiveChrome provides chrome) {
                OnGlass {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                top = statusDp + ImmersiveMath.BUTTON_TOP_DP.dp,
                                start = gutter - ImmersiveMath.BUTTON_EDGE_PULL_DP.dp,
                                end = gutter - ImmersiveMath.BUTTON_EDGE_PULL_DP.dp,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (onBack != null) ImmersiveHeaderButton(IconId.Back, backLabel, onBack)
                        Spacer(Modifier.weight(1f))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            actions()
                            if (onMore != null) ImmersiveHeaderButton(IconId.More, ImmersiveDetailCopy.MORE, onMore)
                        }
                    }
                }
            }
        }

        // ---- Subtitle (fades, never docks) ----
        if (hasSubtitle) {
            val subStyle = remember(type, density, hasHeroArt, colors.ink2) {
                type.small.copy(
                    fontSize = 14.sp,
                    lineHeight = ImmersiveMath.SUBTITLE_LINE_DP.sp,
                    color = if (hasHeroArt) FixedColors.onImage.copy(alpha = ImmersiveMath.SUBTITLE_ALPHA) else colors.ink2,
                    shadow = if (hasHeroArt) {
                        Shadow(
                            ImageDark.copy(alpha = ImmersiveMath.SUBTITLE_SHADOW_ALPHA),
                            Offset(0f, with(density) { 1.dp.toPx() }),
                            with(density) { ImmersiveMath.SUBTITLE_SHADOW_BLUR_DP.dp.toPx() },
                        )
                    } else null,
                )
            }
            BasicText(
                text = subtitle.orEmpty(),
                style = subStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(
                        start = gutter,
                        end = gutter,
                        bottom = subtitleBottomDp.dp,
                    )
                    .onSizeChanged { subtitleWidthPx = it.width }
                    .semantics {
                        if (subtitleTappable && onSubtitleClick != null) {
                            onClick(label = subtitleClickLabel) {
                                onSubtitleClick()
                                true
                            }
                        }
                    }
                    .graphicsLayer {
                        alpha = if (reduced) 1f - stepFade.value else ImmersiveMath.subtitleAlpha(state.progress)
                        translationY = if (reduced) 0f else ImmersiveMath.subtitleShiftDp(state.progress).dp.toPx()
                    },
            )
        }

        // ---- Title (moves and scales in graphicsLayer; colour in draw) ----
        val titleStyle = remember(type, startSp) {
            type.headerTitle.copy(
                fontSize = startSp.sp,
                lineHeight = ImmersiveMath.titleLineHeight(startSp).sp,
                letterSpacing = (-0.01).em,
            )
        }
        val shadowStyle = remember(titleStyle, density) {
            titleStyle.copy(
                color = FixedColors.onImage,
                shadow = Shadow(
                    ImageDark.copy(alpha = ImmersiveMath.TITLE_SHADOW_ALPHA),
                    Offset(0f, with(density) { 1.dp.toPx() }),
                    with(density) { ImmersiveMath.TITLE_SHADOW_BLUR_DP.dp.toPx() },
                ),
            )
        }
        val ink = colors.ink
        val titleColor = remember(ink, hp, hasHeroArt) {
            ColorProducer { if (hasHeroArt) lerp(FixedColors.onImage, ink, ImmersiveMath.titleInkMix(hp())) else ink }
        }
        Box(
            Modifier
                .align(Alignment.TopStart)
                .widthIn(max = (maxWidth - gutter * 2).coerceAtLeast(0.dp))
                .onSizeChanged {
                    titleHeightPx = it.height
                    titleWidthPx = it.width
                }
                .graphicsLayer { titleTransform(this, 0f, 0f) }
                .semantics {
                    heading()
                    // The tap target itself is silent; TalkBack reaches the action on the title.
                    if (titleTappable && onTitleClick != null) {
                        onClick(label = titleClickLabel) {
                            onTitleClick()
                            true
                        }
                    }
                },
        ) {
            if (hasHeroArt) {
                BasicText(
                    text = title,
                    style = shadowStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clearAndSetSemantics { }
                        .graphicsLayer { alpha = 1f - ImmersiveMath.titleInkMix(hp()) },
                )
            }
            BasicText(
                text = title,
                style = titleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = titleColor,
            )
        }
    }
}

/** The glass content panel: blurs the hero behind it, fill alpha by e, 1 px top line, top corners 24. */
@Composable
private fun ImmersivePanel(
    state: ImmersiveDetailState,
    viewportPx: Float,
    panelStartPx: Float,
    minHeight: androidx.compose.ui.unit.Dp,
    heroSource: GlassSource,
    canBlur: Boolean,
    opaque: Boolean,
    fadeE: () -> Float,
    glassBlurPx: Float,
    saturation: Float,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CivilightTheme.colors
    val shape = remember { RoundedCornerShape(topStart = ImmersiveMath.PANEL_RADIUS_DP.dp, topEnd = ImmersiveMath.PANEL_RADIUS_DP.dp) }
    val blurLayer = rememberGraphicsLayer()
    val effect = remember(glassBlurPx, canBlur) {
        if (canBlur && glassBlurPx > 0f && Build.VERSION.SDK_INT >= 31) BlurEffect(glassBlurPx, glassBlurPx, TileMode.Clamp) else null
    }
    val satFilter = remember(saturation) { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(saturation) }) }
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .graphicsLayer {
                this.shape = shape
                clip = true
            }
            .drawBehind {
                // Panel top in viewport (= hero source) coordinates; only the visible part is blurred.
                val top = panelStartPx - state.scrollState.value
                if (effect != null) {
                    val visibleFrom = (-top).coerceAtLeast(0f)
                    val visibleH = (viewportPx - top.coerceAtLeast(0f)).coerceIn(0f, size.height - visibleFrom)
                    if (size.width >= 1f && visibleH >= 1f) {
                        blurLayer.renderEffect = effect
                        blurLayer.colorFilter = satFilter
                        blurLayer.record(size = IntSize(size.width.toInt(), visibleH.toInt())) {
                            translate(0f, -(top + visibleFrom)) { drawLayer(heroSource.layer) }
                        }
                        translate(0f, visibleFrom) { drawLayer(blurLayer) }
                    }
                }
                val fill = if (opaque) colors.bg else colors.glass.copy(alpha = ImmersiveMath.panelGlassAlpha(fadeE(), colors.isDark, canBlur))
                drawRect(fill)
                drawRect(colors.line, size = Size(size.width, 1.dp.toPx()))
            }
            .then(modifier),
    ) {
        // The panel is glass: glass inside it falls back to scrim, never glass on glass (DS-22).
        OnGlass { Column(Modifier.fillMaxWidth()) { content() } }
    }
}

/**
 * 44 dp header button of the immersive screen: over the art a dark circle (.34) on a 12 dp backdrop blur,
 * both fading out as the header glass appears; the icon goes white → `ink` with it. Outside the scaffold
 * or when there is no hero art it draws a plain `ink` icon. [contentDescription] is required.
 */
@Composable
fun ImmersiveHeaderButton(
    icon: IconId,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chrome = LocalImmersiveChrome.current
    val ink = CivilightTheme.colors.ink
    val hasArt = chrome?.hasHeroArt ?: false
    val hp: () -> Float = if (hasArt) (chrome?.headerAlpha ?: { 1f }) else ({ 1f })
    val source = if (hasArt) chrome?.source else null
    val blurLayer = rememberGraphicsLayer()
    val circle = remember { Path() }
    var selfPos by remember { mutableStateOf(Offset.Zero) }
    val blurPx = with(LocalDensity.current) { ImmersiveMath.BUTTON_BLUR_DP.dp.toPx() }
    val effect = remember(blurPx) { if (Build.VERSION.SDK_INT >= 31) BlurEffect(blurPx, blurPx, TileMode.Clamp) else null }
    Box(
        modifier = modifier
            .pressable(onClick = onClick, hitTarget = HitTarget.ICON_BUTTON, shape = CircleShape)
            .size(ImmersiveMath.BUTTON_DP.dp)
            .semantics { this.contentDescription = contentDescription }
            .onGloballyPositioned { selfPos = it.positionInRoot() }
            .drawBehind {
                val h = hp()
                if (!hasArt || h >= 1f) return@drawBehind
                circle.rewind()
                circle.addOval(Rect(Offset.Zero, size))
                clipPath(circle) {
                    if (effect != null && source != null) {
                        blurLayer.renderEffect = effect
                        blurLayer.alpha = 1f - h
                        val dx = source.positionInRoot.x - selfPos.x
                        val dy = source.positionInRoot.y - selfPos.y
                        blurLayer.record { translate(dx, dy) { drawLayer(source.layer) } }
                        drawLayer(blurLayer)
                    }
                    drawRect(ImageDark, alpha = ImmersiveMath.buttonCircleAlpha(h))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        CivilightIcon(
            icon,
            contentDescription = null,
            size = CivilightTheme.sizes.icon,
            tint = if (hasArt) FixedColors.onImage else ink,
            modifier = if (hasArt) {
                Modifier
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(lerp(FixedColors.onImage, ink, ImmersiveMath.titleInkMix(hp())), blendMode = BlendMode.SrcIn)
                    }
            } else Modifier,
        )
    }
}
