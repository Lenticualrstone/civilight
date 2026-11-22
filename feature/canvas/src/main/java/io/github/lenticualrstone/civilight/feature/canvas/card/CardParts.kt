package io.github.lenticualrstone.civilight.feature.canvas.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.card.CardKind
import io.github.lenticualrstone.civilight.designsystem.card.CardMetrics
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.ImageCard
import io.github.lenticualrstone.civilight.designsystem.component.ImageStatusChip
import io.github.lenticualrstone.civilight.designsystem.component.Rating
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

/** Copy of the card components (CNV-23, CNV-24; `scr_cards.py`, DS `ImageTile`). */
object CardCopy {
    /** Tag of the initial-letter placeholder; opens the image picker of the slot. */
    const val BTN_PICK_IMAGE = "이미지 고르기"
    const val HOLO = "홀로"
    const val LOCKED_SUB = "이름 · 표지 가림"
    const val BTN_UNLOCK = "지문으로 보기"
    const val NEW_PROJECT = "새 프로젝트"

    /** `숨긴 프로젝트 2`: one locked card stands for every hidden project (rule 19 §1). */
    fun hiddenProjects(count: Int): String = "숨긴 프로젝트 $count"

    /** `쓰는 중 · 홀로` (status chip of a holo-on cover). */
    fun statusChip(status: String?, holo: Boolean): String? =
        listOfNotNull(status?.takeIf { it.isNotBlank() }, HOLO.takeIf { holo }).joinToString(" · ").ifEmpty { null }
}

private val PlaceholderLetter = TextStyle(fontSize = 64.sp, lineHeight = 80.sp, fontWeight = FontWeight.ExtraBold)

/**
 * CNV-24 `이미지 없음`: fill `tint`, 1 dp inset `edge`, the first grapheme of the owner's name 800 64 / 80
 * `ink3` at (16, 8), and a tag at the bottom-left (12 dp) on `bg`: image icon 14 + `이미지 고르기`, which
 * opens the picker of the slot. Without [onPick] (read-only) the tag is not drawn.
 */
@Composable
fun BoxScope.ImagePlaceholder(name: String, onPick: (() -> Unit)?, compact: Boolean = false) {
    val colors = CivilightTheme.colors
    Box(Modifier.matchParentSize().background(colors.tint).insetOutline(colors.edge, 1.dp, androidx.compose.ui.graphics.RectangleShape)) {
        CivilightText(
            CardInitial.of(name),
            if (compact) PlaceholderLetter.merge(TextStyle(fontSize = 40.sp, lineHeight = 48.sp)) else PlaceholderLetter,
            Modifier.padding(start = 16.dp, top = 8.dp),
            color = colors.ink3,
            maxLines = 1,
        )
        if (onPick != null) {
            val shape = RoundedCornerShape(14.dp)
            Row(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .clip(shape)
                    .background(colors.bg)
                    .pressable(PressAction.Click(onPick), role = Role.Button, hitTarget = HitTarget.CHIP, shape = shape)
                    .heightIn(min = 28.dp)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CivilightIcon(IconId.Image, null, size = 14.dp, tint = colors.ink2)
                CivilightText(CardCopy.BTN_PICK_IMAGE, CivilightTheme.type.chip, color = colors.ink2, maxLines = 1)
            }
        }
    }
}

/**
 * `ProjectCard` (CNV-23): 3:4 at its reference size, the DS image card (gradient, rating badge, name,
 * meta, `⋯`, optional status chip).
 */
@Composable
fun ProjectCard(
    name: String,
    meta: String?,
    rating: Rating?,
    inner: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    status: String? = null,
    holo: Boolean = false,
    more: IconAction? = null,
    image: @Composable BoxScope.() -> Unit,
) {
    val size = CardMetrics.size(CardKind.PROJECT, inner)
    ImageCard(
        title = name,
        modifier = modifier.size(size.width.dp, size.height.dp),
        meta = meta,
        rating = rating,
        statusChip = CardCopy.statusChip(status, holo),
        more = more,
        onClick = onClick,
        image = image,
    )
}

/**
 * The locked card (CNV-23, rule 19 §1/§7): same size as a project card, fill `tint`, 1 dp inset `edge`;
 * lock icon 22 `ink2`, `숨긴 프로젝트 <N>`, `이름 · 표지 가림` and a primary button `지문으로 보기`.
 */
@Composable
fun LockedCard(count: Int, inner: Boolean, onUnlock: () -> Unit, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val size = CardMetrics.size(CardKind.PROJECT, inner)
    val shape = RoundedCornerShape(CardMetrics.radius(CardKind.PROJECT).dp)
    Box(modifier.size(size.width.dp, size.height.dp).clip(shape).background(colors.tint).insetOutline(colors.edge, 1.dp, shape)) {
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CivilightIcon(IconId.Lock, null, size = 22.dp, tint = colors.ink2)
            CivilightText(CardCopy.hiddenProjects(count), type.title.merge(TextStyle(fontSize = 20.sp, lineHeight = 26.sp)), color = colors.ink)
            CivilightText(CardCopy.LOCKED_SUB, type.small, color = colors.ink3)
            CivilightButton(CardCopy.BTN_UNLOCK, onUnlock, Modifier.padding(top = 6.dp).fillMaxWidth(), variant = ButtonVariant.PRIMARY)
        }
    }
}

/** The new-item card (CNV-23, inner only): transparent, 1.5 dp inset `edge`, centred `+` 28 and its label. */
@Composable
fun NewItemCard(label: String, inner: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val size = CardMetrics.size(CardKind.PROJECT, inner)
    val shape = RoundedCornerShape(CardMetrics.radius(CardKind.PROJECT).dp)
    Column(
        modifier
            .size(size.width.dp, size.height.dp)
            .insetOutline(colors.edge, 1.5.dp, shape)
            .pressable(PressAction.Click(onClick), role = Role.Button, shape = shape),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CivilightIcon(IconId.Plus, null, size = 28.dp, tint = colors.ink2)
        CivilightText(label, CivilightTheme.type.label, color = colors.ink2, maxLines = 1)
    }
}

/**
 * The 최근 연 파일 card (CNV-23): image (cover 150 × 100, inner 176 × 110, radius 14 or [imageRadius]); 8 dp below: kind
 * line small, title 700 15 / 21 single line, time small. Also the page card of the page list.
 */
@Composable
fun RecentFileCard(
    kindLine: String,
    title: String,
    time: String,
    inner: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    imageModifier: Modifier = Modifier,
    imageRadius: Dp = CardMetrics.radius(CardKind.RECENT).dp,
    image: @Composable BoxScope.() -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val size = CardMetrics.size(CardKind.RECENT, inner)
    // [imageRadius] moves with the `목록 ↔ 카드` switch (CNV-45); the press shape stays the card's own.
    val shape = RoundedCornerShape(CardMetrics.radius(CardKind.RECENT).dp)
    val imageShape = RoundedCornerShape(imageRadius)
    Column(modifier.width(size.width.dp).pressable(PressAction.Select(selected, onClick), role = Role.Button, shape = shape)) {
        Box(imageModifier.size(size.width.dp, size.height.dp).clip(imageShape).background(colors.raise), content = image)
        CivilightText(kindLine, type.small, Modifier.padding(top = 8.dp), color = colors.ink3, maxLines = 1)
        CivilightText(title, type.itemTitleCompact.merge(TextStyle(lineHeight = 21.sp)), color = colors.ink, maxLines = 1)
        CivilightText(time, type.small, color = colors.ink3, maxLines = 1)
    }
}

/**
 * The portrait card (CNV-23): 4:5 (primary 196 × 245, other groups 132 × 165, radius 20); 10 dp below:
 * name 800 18 / 24 with the first appearance in mono small at the right, then a small sub line. A
 * holo-on portrait shows the chip `홀로`.
 */
@Composable
fun PortraitCard(
    name: String,
    firstAppearance: String?,
    sub: String?,
    large: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    holo: Boolean = false,
    image: @Composable BoxScope.() -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val size = CardMetrics.size(if (large) CardKind.PORTRAIT else CardKind.PORTRAIT_SMALL, inner = false)
    val shape = RoundedCornerShape(CardMetrics.radius(CardKind.PORTRAIT).dp)
    Column(modifier.width(size.width.dp).pressable(PressAction.Click(onClick), role = Role.Button, shape = shape)) {
        Box(Modifier.size(size.width.dp, size.height.dp).clip(shape).background(colors.raise)) {
            Box(Modifier.fillMaxSize(), content = image)
            if (holo) ImageStatusChip(CardCopy.HOLO, Modifier.align(Alignment.TopStart).padding(12.dp))
        }
        Row(Modifier.padding(top = 10.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CivilightText(name, type.title.merge(TextStyle(fontSize = 18.sp, lineHeight = 24.sp)), Modifier.weight(1f), color = colors.ink, maxLines = 1)
            firstAppearance?.let { CivilightText(it, type.number, color = colors.ink3, maxLines = 1) }
        }
        sub?.let { CivilightText(it, type.small, color = colors.ink3, maxLines = 1) }
    }
}
