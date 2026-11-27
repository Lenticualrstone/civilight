package io.github.lenticualrstone.civilight.feature.world.place

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTabs
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ImmersiveDetailScaffold
import io.github.lenticualrstone.civilight.designsystem.component.ImmersiveMath
import io.github.lenticualrstone.civilight.designsystem.component.LensChip
import io.github.lenticualrstone.civilight.designsystem.component.ReportBottomChrome
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.ShardArt
import io.github.lenticualrstone.civilight.designsystem.component.holoImage
import io.github.lenticualrstone.civilight.designsystem.component.rememberImmersiveDetailState
import io.github.lenticualrstone.civilight.designsystem.glass.GlassBackground
import io.github.lenticualrstone.civilight.designsystem.glass.GlassSurface
import io.github.lenticualrstone.civilight.designsystem.text.DecoratedText
import io.github.lenticualrstone.civilight.designsystem.text.DecorationRange
import io.github.lenticualrstone.civilight.designsystem.text.InlineDecoration
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.world.common.CardImageUi
import io.github.lenticualrstone.civilight.feature.world.common.CardImages
import io.github.lenticualrstone.civilight.feature.world.common.LensBannerUi
import io.github.lenticualrstone.civilight.feature.world.common.ParagraphUi
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.designsystem.component.LensEditBanner
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.lens.LensChipLabel
import io.github.lenticualrstone.civilight.ui.nav.LocalPaneRole
import io.github.lenticualrstone.civilight.ui.nav.PaneRole
import io.github.lenticualrstone.civilight.ui.scaffold.CommandScope
import io.github.lenticualrstone.civilight.ui.scaffold.LocalCommandBarHost
import io.github.lenticualrstone.civilight.ui.scaffold.LocalSheets
import io.github.lenticualrstone.civilight.ui.scaffold.SheetHost
import io.github.lenticualrstone.civilight.ui.scaffold.SheetHostLayout
import io.github.lenticualrstone.civilight.ui.window.CenterColumn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The immersive frame of the place and faction cards (boards `Immersive`, `ImmSpec`), the record's shell: the
 * screen's sheets, the pane sizing (Fold inner right pane: the command bar of `목록 + 상세` sits under the pane, the
 * card stops above it) and [ImmersiveDetailScaffold] with the bottom tabs. In the detail pane there is no back.
 * The scroll is saved per item, so each card opens at its own place.
 */
@Composable
fun WorldImmersiveFrame(
    itemKey: Key,
    title: String,
    subtitle: String?,
    image: CardImageUi?,
    sheets: SheetHost,
    localSheets: LocalSheets,
    onBack: () -> Unit,
    tabs: List<String>,
    tab: Int,
    onTab: (Int) -> Unit,
    onMore: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit,
    onHeroClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val paneHost = if (CommandScope.of(LocalPaneRole.current) != null) LocalCommandBarHost.current else null
    val bottomInset = paneHost?.panePadding?.takeIf { it > 0.dp }
    val inDetailPane = LocalPaneRole.current == PaneRole.DETAIL
    val hasArt = image is CardImageUi.Picked || image is CardImageUi.Preset
    val scroll = rememberSaveable(itemKey, saver = ScrollState.Saver) { ScrollState(0) }
    val immersive = rememberImmersiveDetailState(scroll)
    if (paneHost == null && tabs.size > 1) ReportBottomChrome(ImmersiveMath.BAR_ROW_DP.dp)
    CenterColumn {
        SheetHostLayout(sheets, localSheets = localSheets) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                ImmersiveDetailScaffold(
                    title = title,
                    subtitle = subtitle,
                    onBack = if (inDetailPane) null else onBack,
                    hero = { WorldHero(image, title) },
                    actions = actions,
                    bottomBar = if (tabs.size > 1) ({ CivilightTabs(tabs = tabs, selectedIndex = tab, onSelect = onTab) }) else null,
                    state = immersive,
                    onMore = onMore,
                    bottomInset = bottomInset,
                    hasHeroArt = hasArt,
                    onHeroClick = onHeroClick,
                    heroLabel = onHeroClick?.let { PlaceCopy.PICK_IMAGE },
                    content = content,
                )
            }
        }
    }
}

/**
 * The hero: the item's 16:9 image filling the screen, cropped with top alignment (`ImmSpec`), a preset's art, or a
 * plain colour field when there is no image (no stand-in picture, no initial letter).
 */
@Composable
fun BoxScope.WorldHero(image: CardImageUi?, name: String) {
    val colors = CivilightTheme.colors
    val motion = CivilightTheme.motion
    val deps = LocalFeatureDeps.current
    Box(Modifier.fillMaxSize().background(colors.raise)) {
        when (image) {
            null, is CardImageUi.Initial -> Unit
            is CardImageUi.Preset -> ShardArt(listOf(colors.tint, colors.raise, colors.edge), Modifier.fillMaxSize())
            is CardImageUi.Picked -> {
                val bitmap by produceState<ImageBitmap?>(null, image) {
                    value = withContext(Dispatchers.IO) {
                        deps.stores.blobs.fileOf(image.hash)?.let { runCatching { CardImages.decode(it, image.crop, HERO_WIDTH) }.getOrNull() }?.asImageBitmap()
                    }
                }
                Crossfade(targetState = bitmap, modifier = Modifier.fillMaxSize(), animationSpec = motion.spec(CivilightMotion.standard), label = "worldHero") { shown ->
                    if (shown != null) {
                        Image(shown, contentDescription = name, modifier = Modifier.fillMaxSize().holoImage(image.holo), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
                    }
                }
            }
        }
    }
}

private const val HERO_WIDTH = 1848

/** The lens in the immersive header: the shared [LensChip] on a 44 dp glass pill (as on the character record). */
@Composable
fun WorldHeaderLens(lens: LensChipLabel, onLens: () -> Unit) {
    Box(Modifier.heightIn(min = 44.dp), contentAlignment = Alignment.Center) {
        GlassBackground(GlassSurface.CHIP, Modifier.matchParentSize(), shape = RoundedCornerShape(22.dp))
        LensChip(lens.text, lens.state, onLens, Modifier.padding(horizontal = 6.dp), small = true)
    }
}

/** The past-lens banner (WLD-12) at the top of the panel: `1-5 시점 값 고치는 중` · `최신으로`. */
@Composable
fun WorldLensBanner(banner: LensBannerUi?, onLatest: () -> Unit) {
    if (banner == null) return
    LensEditBanner(
        title = banner.title,
        subtitle = banner.subtitle,
        actionLabel = WorldCopy.BTN_TO_LATEST,
        onAction = onLatest,
        modifier = Modifier.padding(bottom = 12.dp),
    )
}

/** A section label of the panel (board `S.sec`: mono 11, `ink3`, 30 above, 10 below; the first one 0 above). */
@Composable
fun WorldSection(
    text: String,
    first: Boolean = false,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    onTrailing: (() -> Unit)? = null,
) {
    SectionLabel(text, modifier.padding(top = if (first) 0.dp else 30.dp, bottom = 10.dp), trailing = trailing, onTrailing = onTrailing)
}

/** `설명` (board `X.lead` · `X.p2`): the first paragraph large (Myeongjo 19 / 33, `ink`), the rest 16 / 28 `ink2`. */
@Composable
fun LeadDescription(paragraphs: List<ParagraphUi>) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val lead = type.body.merge(TextStyle(fontSize = 19.sp, lineHeight = 33.sp))
    Column(Modifier.fillMaxWidth()) {
        paragraphs.forEachIndexed { i, p ->
            DecoratedText(
                text = AnnotatedString(p.text),
                style = if (i == 0) lead else type.body,
                modifier = Modifier.padding(top = if (i == 0) 0.dp else 12.dp),
                color = if (i == 0) colors.ink else colors.ink2,
                decorations = p.decorations,
            )
        }
    }
}

/** A name that may be dotted (rule 20: a deleted reference). */
@Composable
fun RefText(text: String, style: TextStyle, dotted: Boolean, modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color = CivilightTheme.colors.ink) {
    if (dotted) {
        DecoratedText(
            AnnotatedString(text),
            style,
            modifier,
            color = color,
            decorations = listOf(DecorationRange(0, text.length, InlineDecoration.DELETED_REF)),
            maxLines = 1,
        )
    } else {
        CivilightText(text, style, modifier, color = color, maxLines = 1)
    }
}
