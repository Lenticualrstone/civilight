package io.github.lenticualrstone.civilight.feature.library.shelf.flat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ReportBottomChrome
import io.github.lenticualrstone.civilight.designsystem.component.rememberSystemBottom
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.glass.GlassBackground
import io.github.lenticualrstone.civilight.designsystem.glass.GlassMode
import io.github.lenticualrstone.civilight.designsystem.glass.GlassSurface
import io.github.lenticualrstone.civilight.designsystem.glass.OnGlass
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.unhide
import io.github.lenticualrstone.civilight.feature.library.shelf.CoverArt
import io.github.lenticualrstone.civilight.feature.library.shelf.LibraryTourSteps
import io.github.lenticualrstone.civilight.feature.library.shelf.ProjectRowUi
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCardItem
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCards
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCopy
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfViewInput
import io.github.lenticualrstone.civilight.feature.library.shelf.views.ItemProtection
import io.github.lenticualrstone.civilight.feature.library.shelf.views.previewInert
import io.github.lenticualrstone.civilight.feature.library.tour.tourTarget
import io.github.lenticualrstone.civilight.model.app.LibraryStyle
import io.github.lenticualrstone.civilight.model.project.ProjectFormat

/**
 * The board-only colours of the flat views (r3 `scr_v_lib.py` `TOK`): 원고지 paper and rules, envelopes and the
 * letter, the form seals, the cells. Everything else is the theme's own colour.
 */
internal class FlatLook(
    val bg2: Color, val paper: Color, val pline: Color, val pgut: Color,
    val env: Color, val flap: Color, val letter: Color, val lettink: Color, val lettink2: Color,
    val s1: Color, val s1i: Color, val s2: Color, val s2i: Color, val s3: Color, val s3i: Color,
    val cell: Color, val scrim: Color, val raise: Color, val shadow: Color,
    /** The letter's own accent and button (fixed in both themes, board `.lt .bar i`, `.lt .bt`). */
    val letterAcc: Color, val letterBtn: Color, val letterOnBtn: Color,
) {
    companion object {
        val dark = FlatLook(
            bg2 = Color(0xFF111318), paper = Color(0xFF16181C), pline = Color(201, 138, 98).copy(alpha = .20f), pgut = Color(201, 138, 98).copy(alpha = .34f),
            env = Color(0xFF1F2228), flap = Color(0xFF24272E), letter = Color(0xFFD8D2C4), lettink = Color(0xFF1E1A16), lettink2 = Color(0xFF4D463C),
            s1 = Color(0xFF8FB8E6), s1i = Color(0xFF031C35), s2 = Color(0xFFD29A72), s2i = Color(0xFF2A1407), s3 = Color(0xFFA9C2A4), s3i = Color(0xFF122010),
            cell = Color.White.copy(alpha = .08f), scrim = Color(8, 9, 11).copy(alpha = .72f), raise = Color(0xFF16181C), shadow = Color.Black.copy(alpha = .5f),
            letterAcc = Color(0xFF1F5590), letterBtn = Color(0xFF031C35), letterOnBtn = Color(0xFFF6F5F1),
        )
        val light = FlatLook(
            bg2 = Color(0xFFEFEDE7), paper = Color(0xFFFFFDF8), pline = Color(179, 38, 30).copy(alpha = .16f), pgut = Color(179, 38, 30).copy(alpha = .30f),
            env = Color(0xFFFFFDF8), flap = Color(0xFFE9E4D8), letter = Color(0xFFFFFFFF), lettink = Color(0xFF16171A), lettink2 = Color(0xFF4A4C52),
            s1 = Color(0xFF1F5590), s1i = Color(0xFFF6F5F1), s2 = Color(0xFF9A5A32), s2i = Color(0xFFFFF6EE), s3 = Color(0xFF4F6B4C), s3i = Color(0xFFF3F8F1),
            cell = Color(22, 23, 26).copy(alpha = .07f), scrim = Color(10, 10, 12).copy(alpha = .66f), raise = Color(0xFFECEAE4), shadow = Color(40, 36, 28).copy(alpha = .18f),
            letterAcc = Color(0xFF1F5590), letterBtn = Color(0xFF031C35), letterOnBtn = Color(0xFFF6F5F1),
        )

        @Composable
        fun current(): FlatLook = if (CivilightTheme.colors.isDark) dark else light
    }
}

/** What every flat view gets: the items, the pick and the actions (all from the masked ShelfUi). */
@Stable
internal class FlatArgs(
    val style: LibraryStyle,
    val items: List<ShelfCardItem>,
    val index: Int,
    /** A pick has been made (cover views that open on tap show no ring until then). */
    val picked: Boolean,
    val wide: Boolean,
    val input: ShelfViewInput,
    val select: (Int) -> Unit,
    val tap: (Int) -> Unit,
    val openPicked: () -> Unit,
) {
    val selected: ShelfCardItem? get() = items.getOrNull(index)
    val count: Int get() = items.size
    val preview: Boolean get() = input.preview

    /** The ring is drawn on the picked item (inner boards and the cover boards with a detail block always). */
    fun isOn(i: Int): Boolean = i == index && (picked || wide || !FlatViews.opensOnTap(style, wide))
}

/**
 * (0.2.3, r3 `libviews`) One flat 서재 view. The view area sits between the glass header and the search bar
 * (8 dp from each, 12 dp at the sides), every view stretches its rows or columns to fill it and only scrolls
 * when the user's projects do not fit at the smallest size. The scene's background runs under both bars. Under
 * the area, the peek row `최근 연 파일 ⌃` (as in the 3D views; not on the boards, see release notes) keeps the
 * 최근 연 파일 sheet and the 라이프 레코더 reachable. Draws from the masked [ShelfViewInput.ui] only.
 */
@Composable
internal fun FlatScene(style: LibraryStyle, input: ShelfViewInput) {
    val ui = input.ui
    val wide = input.wide
    val items = remember(ui.cards, ui.lockedGroup) { ShelfCards.cardItems(ui, wide = false) }
    val keys = remember(items) { items.map { it.key.toString() } }
    var selKey by rememberSaveable { mutableStateOf<String?>(null) }
    var picked by rememberSaveable { mutableStateOf(false) }
    val index = keys.indexOf(selKey).takeIf { it >= 0 } ?: 0
    // LIB-06: the project in the detail pane is the picked one.
    // Keyed on keys too: the picked project may join the items after the key arrives (index loading, unlock).
    // Each detail key is applied once, so a later items change does not pull a local pick back to it.
    var synced by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(input.selectedKey, keys) {
        val k = input.selectedKey?.toString() ?: return@LaunchedEffect
        if (k != synced && k in keys) {
            selKey = k
            picked = true
            synced = k
        }
    }
    val select: (Int) -> Unit = { i -> keys.getOrNull(i)?.let { selKey = it; picked = true } }
    val open: (Int) -> Unit = { i ->
        when (val item = items.getOrNull(i)) {
            is ShelfCardItem.Project -> input.onOpen(item.card.row.key)
            is ShelfCardItem.Lock -> input.onUnlock()
            ShelfCardItem.NewProject -> input.onNewProject()
            null -> Unit
        }
    }
    val tap: (Int) -> Unit = tap@{ i ->
        val item = items.getOrNull(i) ?: return@tap
        when {
            item is ShelfCardItem.NewProject -> input.onNewProject()
            // A cover board with no detail block unlocks at once (the prompt is the visible step).
            item is ShelfCardItem.Lock && FlatViews.opensOnTap(style, wide) -> { select(i); input.onUnlock() }
            FlatViews.opensOnTap(style, wide) -> { select(i); open(i) }
            // 포스터 (cover): a tap on the poster in front opens it (the control row shows `열기` too).
            i == index && style == LibraryStyle.POSTER && !wide -> open(i)
            // 진행 칸 (inner): the picked row opens on its second tap; the row also shows `열기`.
            i == index && picked && style == LibraryStyle.PROGRESS_CELLS && wide -> open(i)
            else -> select(i)
        }
    }
    val args = FlatArgs(style, items, index, picked, wide, input, select, tap, { open(index) })
    val colors = CivilightTheme.colors
    val look = FlatLook.current()
    val density = LocalDensity.current
    var peekH by remember { mutableIntStateOf(0) }
    val peekDp = with(density) { peekH.toDp() }
    Box(
        Modifier
            .fillMaxSize()
            // Board `.lv`: `linear-gradient(180deg, bg2, bg 60%)`, under both glass bars.
            .background(Brush.verticalGradient(0f to look.bg2, .6f to colors.bg, 1f to colors.bg))
            .then(if (input.preview) Modifier.previewInert() else Modifier),
    ) {
        for (item in items) key(item.key) {
            ItemProtection(item)
            // `화면 캡처 가리기`: a project whose last place (a scene title) is drawn protects the window too.
            val row = (item as? ShelfCardItem.Project)?.card?.row
            if (row != null && !item.card.hiddenOpen && row.protections.captureBlock && row.facts?.place != null) {
                io.github.lenticualrstone.civilight.ui.security.ProtectWindow(io.github.lenticualrstone.civilight.ui.security.WindowProtection.forProject(row.protections))
            }
        }
        val peek = input.onRecents.takeIf { !input.preview }
        Box(
            Modifier
                .fillMaxSize()
                .padding(
                    top = input.barTop.dp + AREA_GAP,
                    bottom = input.barBottom.dp + AREA_GAP + (if (peek != null) peekDp + AREA_GAP else 0.dp),
                    start = AREA_SIDE,
                    end = AREA_SIDE,
                ),
        ) {
            when (style) {
                LibraryStyle.TIMELINE -> TimelineView(args)
                LibraryStyle.MANUSCRIPT_GRID -> PaperView(args)
                LibraryStyle.MAILBOX -> MailView(args)
                LibraryStyle.PROGRESS_CELLS -> CellsView(args)
                LibraryStyle.COVER_WALL -> WallView(args)
                LibraryStyle.POSTER -> PosterView(args)
                LibraryStyle.TABLE -> TableView(args)
                // Drawn by ShelfScene3D or the list rows, never here. Exhaustive on purpose: a new flat style must
                // name its renderer here or fail to compile, never draw a blank area.
                LibraryStyle.BOOKSHELF, LibraryStyle.COVER_SPIN, LibraryStyle.MANUSCRIPT_STACK, LibraryStyle.ARCHIVE_HALL,
                LibraryStyle.INDEX_DRAWER, LibraryStyle.CONSTELLATION, LibraryStyle.LIST -> Unit
            }
        }
        if (peek != null) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = input.barBottom.dp + AREA_GAP, start = AREA_SIDE, end = AREA_SIDE)
                    .onSizeChanged { peekH = it.height },
            ) {
                if (peekH > 0 && !input.covered) ReportBottomChrome(input.barBottom.dp + AREA_GAP + peekDp - rememberSystemBottom())
                PeekPill(peek)
            }
        }
    }
}

/** The area's gaps (board `.area`: top 72 = 8 + 56 + 8, sides 12). */
internal val AREA_GAP = 8.dp
internal val AREA_SIDE = 12.dp

/** `최근 연 파일 ⌃` on a small glass pill (the 3D dock's peek row). */
@Composable
private fun PeekPill(onOpen: () -> Unit) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(20.dp)
    val mode = CivilightTheme.glassMode.let { if (it == GlassMode.BLUR_COSTLY) GlassMode.SCRIM else it }
    Box(Modifier.fillMaxWidth().height(40.dp).clip(shape)) {
        GlassBackground(GlassSurface.BANNER, Modifier.matchParentSize(), shape = shape, mode = mode)
        OnGlass {
            Row(
                Modifier
                    .fillMaxSize()
                    .insetOutline(colors.line, 1.dp, shape)
                    .pressable(onClick = onOpen, role = Role.Button, scaleOnPress = false, onClickLabel = ShelfCopy.RECENTS),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            ) {
                CivilightText(ShelfCopy.RECENTS, CivilightTheme.type.small, color = colors.ink2, maxLines = 1)
                CivilightIcon(IconId.Up, contentDescription = null, size = 16.dp, tint = colors.ink2)
            }
        }
    }
}

// ---------------------------------------------------------------- shared parts

internal fun formWord(f: ProjectFormat?): String = when (f) {
    ProjectFormat.SHORT -> FlatViewCopy.FORM_SHORT
    ProjectFormat.SETTING_ONLY -> FlatViewCopy.FORM_SETTING
    ProjectFormat.SERIAL, null -> FlatViewCopy.FORM_SERIAL
}

/** `연재 장편 · 오늘 08:12`. */
internal fun formLine(row: ProjectRowUi): String = "${formWord(row.format)} · ${row.edited}"

/** `에피소드 6 / 20`, or `카드 · 인물 N` for 설정만. */
internal fun progressLine(row: ProjectRowUi): String =
    row.facts?.progress?.line ?: (row.facts?.let { FlatViewCopy.cast(it.cast) } ?: "")

/** The hidden-open project's unhide (CNV-43) on whatever draws it. */
@Composable
internal fun Modifier.unhideOf(item: ShelfCardItem, args: FlatArgs): Modifier {
    val u = args.input.unhide
    return if (u != null && item is ShelfCardItem.Project && item.card.hiddenOpen) this.unhide(u) else this
}

/** A project's cover (image, preset or initial), or the lock tile. */
@Composable
internal fun Art(item: ShelfCardItem?, modifier: Modifier, radius: Dp = 0.dp, targetWidth: Int = 360, lockSize: Dp = 24.dp) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(radius)
    when (item) {
        is ShelfCardItem.Project -> Box(modifier.clip(shape).background(colors.tint)) { CoverArt(item.card.cover, targetWidth) }
        else -> Box(modifier.clip(shape).background(colors.tint).insetOutline(colors.line, 1.dp, shape), contentAlignment = Alignment.Center) {
            if (item is ShelfCardItem.Lock) CivilightIcon(IconId.Lock, contentDescription = null, size = lockSize, tint = colors.ink2)
        }
    }
}

/** The lock mark (board `.lkm`), named for accessibility. */
@Composable
internal fun LockMark(size: Dp = 22.dp, tint: Color = CivilightTheme.colors.ink2) {
    CivilightIcon(IconId.Lock, contentDescription = FlatViewCopy.LOCKED_PROJECT, size = size, tint = tint)
}

/** Board `.bar`: 4 dp track, accent fill. */
@Composable
internal fun ProgressBar(percent: Int, modifier: Modifier = Modifier, track: Color = FlatLook.current().cell, fill: Color = CivilightTheme.colors.acc) {
    val p = (percent.coerceIn(0, 100)) / 100f
    Canvas(modifier.height(4.dp)) {
        val r = CornerRadius(size.height / 2f)
        drawRoundRect(track, cornerRadius = r)
        if (p > 0f) drawRoundRect(fill, size = size.copy(width = size.width * p), cornerRadius = r)
    }
}

/** Board `.nb`: a 44 dp round ◀ / ▶. */
@Composable
internal fun RoundNav(icon: IconId, label: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    Box(Modifier.size(44.dp).clip(CircleShape).background(colors.tint).insetOutline(colors.line, 1.dp, CircleShape), contentAlignment = Alignment.Center) {
        CivilightIconButton(icon, label, onClick, enabled = enabled, tint = colors.ink)
    }
}

/** ◀ `n / N` ▶ (board `navctl`). */
@Composable
internal fun RowScope.NavCtl(args: FlatArgs, countWeight: Boolean = false) {
    RoundNav(IconId.Prev, FlatViewCopy.PREV, args.index > 0) { args.select(FlatViews.step(args.index, -1, args.count)) }
    val text = if (args.count > 0) FlatViewCopy.count(args.index, args.count) else ""
    CivilightText(
        text,
        CivilightTheme.type.small,
        if (countWeight) Modifier.weight(1f) else Modifier.padding(horizontal = 4.dp),
        color = CivilightTheme.colors.ink3,
        maxLines = 1,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
    RoundNav(IconId.Next, FlatViewCopy.NEXT, args.index < args.count - 1) { args.select(FlatViews.step(args.index, 1, args.count)) }
}

/** `열기` or `잠금 풀기` for the picked item (board `.bt`, pill 44). */
@Composable
internal fun ActButton(args: FlatArgs, modifier: Modifier = Modifier, letter: Boolean = false) {
    val item = args.selected
    val look = FlatLook.current()
    if (item is ShelfCardItem.Lock) {
        CivilightButton(FlatViewCopy.UNLOCK, args.input.onUnlock, modifier.height(44.dp), variant = ButtonVariant.PRIMARY, icon = IconId.Fingerprint)
    } else if (item is ShelfCardItem.Project) {
        if (letter) {
            LetterButton(FlatViewCopy.OPEN, look, modifier, args.openPicked)
        } else {
            CivilightButton(FlatViewCopy.OPEN, args.openPicked, modifier.height(44.dp), variant = ButtonVariant.PRIMARY, icon = IconId.Book)
        }
    }
}

/** The letter's fixed-colour `열기` (board `.lt .bt`). */
@Composable
internal fun LetterButton(text: String, look: FlatLook, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier.height(44.dp).clip(shape).background(look.letterBtn).pressable(onClick = onClick, role = Role.Button, shape = shape).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CivilightIcon(IconId.Book, contentDescription = null, size = 18.dp, tint = look.letterOnBtn)
        CivilightText(text, CivilightTheme.type.button, color = look.letterOnBtn, maxLines = 1)
    }
}

/**
 * `⋯` (LIB-02) for a project: the boards draw none, but rename, hide, cover and delete must stay reachable in
 * every view. The LIB-19 `프로젝트 숨기기` step points at the tour's project.
 */
@Composable
internal fun MoreButton(args: FlatArgs, item: ShelfCardItem?, tint: Color = CivilightTheme.colors.ink2) {
    val project = item as? ShelfCardItem.Project ?: return
    val key = project.card.row.key
    val tour = !args.preview && key == args.input.tourMenuKey
    CivilightIconButton(IconId.More, ShelfCopy.MORE_LABEL, { args.input.onMenu(key) }, if (tour) Modifier.tourTarget(LibraryTourSteps.HIDE) else Modifier, tint = tint)
}

/** The lock item's tour target (LIB-19 `숨긴 프로젝트`), not in a preview. */
@Composable
internal fun Modifier.lockTour(args: FlatArgs, item: ShelfCardItem): Modifier =
    if (!args.preview && item is ShelfCardItem.Lock) this.tourTarget(LibraryTourSteps.HIDDEN_GROUP) else this

/** Name, `형식 · 고친 때`, the last place, the progress bar (board `detail_ok_lines`). */
@Composable
internal fun ColumnScope.DetailLines(row: ProjectRowUi, ink: Color = CivilightTheme.colors.ink, ink2: Color = CivilightTheme.colors.ink2, nameStyle: TextStyle = NAME_STYLE, nameLines: Int = 1, track: Color = FlatLook.current().cell, fill: Color = CivilightTheme.colors.acc) {
    val type = CivilightTheme.type
    CivilightText(row.title, nameStyle, color = ink, maxLines = nameLines)
    CivilightText(formLine(row), type.small, color = ink2, maxLines = 1)
    row.facts?.place?.let { CivilightText(it, type.small.merge(TextStyle(fontSize = 13.sp)), color = ink2, maxLines = 1) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ProgressBar(row.facts?.progress?.percent ?: 0, Modifier.weight(1f), track = track, fill = fill)
        CivilightText(progressLine(row), type.small.merge(TextStyle(fontWeight = FontWeight.SemiBold)), color = ink2, maxLines = 1)
    }
}

/** `글자 48,210` and `인물 12` lines (board `.ln`). */
@Composable
internal fun FactLine(label: String, value: String, ink: Color = CivilightTheme.colors.ink, ink2: Color = CivilightTheme.colors.ink2, line: Color = CivilightTheme.colors.line) {
    val type = CivilightTheme.type
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(line))
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            CivilightText(label, type.small.merge(TextStyle(fontSize = 13.sp)), color = ink2, maxLines = 1)
            CivilightText(value, type.small.merge(TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold)), color = ink, maxLines = 1)
        }
    }
}

/** `최근 위치` and the project's own 최근 연 파일 entries (masked recents of the 서재; none drawn without any). */
@Composable
internal fun RecentPlaces(args: FlatArgs, row: ProjectRowUi, n: Int, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val places = listOfNotNull(row.facts?.place) + args.input.ui.recents
        .filter { it.item.projectKey == row.key && !it.masked }
        .mapNotNull { r -> listOfNotNull(r.kindLine, r.title).joinToString(" · ").takeIf { it.isNotBlank() } }
    val shown = places.distinct().take(n)
    Column(modifier) {
        CivilightText(FlatViewCopy.RECENT, type.small, Modifier.padding(bottom = 4.dp), color = colors.ink3, maxLines = 1)
        shown.forEachIndexed { k, p ->
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
            CivilightText(
                p,
                type.small.merge(TextStyle(fontSize = 13.sp, fontWeight = if (k == 0) FontWeight.SemiBold else FontWeight.Medium)),
                Modifier.padding(vertical = 8.dp),
                color = if (k == 0) colors.acc else colors.ink,
                maxLines = 1,
            )
        }
    }
}

/** The pane / detail block of the picked item: project lines, or the lock mark with `잠금 풀기` only. */
@Composable
internal fun PickedPane(args: FlatArgs, modifier: Modifier, art: Boolean = true, facts: Boolean = true) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(20.dp)
    val item = args.selected
    Column(
        modifier.clip(shape).background(FlatLook.current().raise).insetOutline(colors.line, 1.dp, shape).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (item) {
            is ShelfCardItem.Project -> {
                val row = item.card.row
                if (art) Art(item, Modifier.fillMaxWidth().weight(1f).unhideOf(item, args), radius = 12.dp, targetWidth = 480)
                Column(Modifier.unhideOf(item, args), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DetailLines(row, nameLines = 2)
                    if (facts) {
                        FactLine(FlatViewCopy.CHARS, FlatViewCopy.number(row.facts?.chars ?: 0))
                        FactLine(FlatViewCopy.CAST, (row.facts?.cast ?: 0).toString())
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    ActButton(args, Modifier.weight(1f))
                    MoreButton(args, item)
                }
            }
            is ShelfCardItem.Lock -> Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
                LockMark(32.dp)
                ActButton(args)
            }
            else -> Box(Modifier.weight(1f))
        }
    }
}

/** Board `.it.on`: the 2 dp accent ring. */
internal fun Modifier.ring(on: Boolean, color: Color, radius: Dp): Modifier =
    if (on) this.insetOutline(color, 2.dp, RoundedCornerShape(radius)) else this

internal val NAME_STYLE = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)

/** The movement spec of the views (selection, envelope, pager); a snap under reduced motion. */
@Composable
internal fun <T> flatMove() = CivilightTheme.motion.move<T>(CivilightMotion.swipe)
