package io.github.lenticualrstone.civilight.ui.scaffold

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.BottomGlassBar
import io.github.lenticualrstone.civilight.designsystem.component.OutlinedSegmented
import io.github.lenticualrstone.civilight.designsystem.component.ReportBottomChrome
import io.github.lenticualrstone.civilight.designsystem.component.SegmentItem
import io.github.lenticualrstone.civilight.designsystem.component.rememberSystemBottom
import io.github.lenticualrstone.civilight.designsystem.glass.OnGlass
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.LayoutMetrics
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import io.github.lenticualrstone.civilight.ui.nav.PaneRole
import kotlin.math.roundToInt

/** Which pane of `목록 + 상세` the frame's one command bar acts for (DS-65). */
enum class CommandScope {
    LIST,
    DETAIL;

    val other: CommandScope get() = if (this == LIST) DETAIL else LIST

    companion object {
        /**
         * The scope after [event]: a detail opening or being replaced → `DETAIL`; the detail closing → `LIST`; a
         * scope-switch cell → that pane; the scope pane locking → the other pane. Touching, scrolling or typing in
         * a pane never moves it (no guessing, memory `feedback-no-guessing-interactions`).
         */
        fun next(current: CommandScope, event: CommandScopeEvent): CommandScope = when (event) {
            CommandScopeEvent.DetailOpened, CommandScopeEvent.DetailReplaced -> DETAIL
            CommandScopeEvent.DetailClosed -> LIST
            is CommandScopeEvent.Picked -> event.scope
            is CommandScopeEvent.PaneLocked -> if (event.scope == current) current.other else current
            is CommandScopeEvent.TouchedIn -> current
        }

        /** The scope of a pane role (`SINGLE` has none). */
        fun of(role: PaneRole): CommandScope? = when (role) {
            PaneRole.LIST -> LIST
            PaneRole.DETAIL -> DETAIL
            PaneRole.SINGLE -> null
        }
    }
}

/** What can change the [CommandScope] (DS-65 §4). */
sealed interface CommandScopeEvent {
    data object DetailOpened : CommandScopeEvent
    data object DetailReplaced : CommandScopeEvent
    data object DetailClosed : CommandScopeEvent
    data class Picked(val scope: CommandScope) : CommandScopeEvent
    data class PaneLocked(val scope: CommandScope) : CommandScopeEvent
    data class TouchedIn(val scope: CommandScope) : CommandScopeEvent

    companion object {
        /** The event when the detail pane goes from [before] to [after] (content keys; null = no detail). */
        fun ofDetail(before: Any?, after: Any?): CommandScopeEvent? = when {
            before == after -> null
            before == null -> DetailOpened
            after == null -> DetailClosed
            else -> DetailReplaced
        }
    }
}

/**
 * One pane's command-bar declaration as the frame sees it. [title] is exactly the title its top bar shows (masked
 * as shown); a [locked] pane (a lock screen, a locked hidden project or item) contributes nothing.
 */
data class PaneCommand(
    val declared: Boolean,
    val title: String?,
    val hasExtraRow: Boolean = false,
    val locked: Boolean = false,
) {
    val active: Boolean get() = declared && !locked
}

/** The frame's bar (DS-65): how many, whether the scope switch shows and with which cells, and its size. */
data class CommandBarLayout(
    val barCount: Int,
    val showsSwitch: Boolean,
    val cells: List<String>,
    val cellScopes: List<CommandScope>,
    val target: CommandScope?,
    val barHeight: Int,
    val barWidth: Float,
    val commandRowWidth: Float,
)

/** DS-65 geometry and the frame layout, pure. */
object CommandBarRules {
    const val BAR: Int = 130
    const val BAR_WITH_ROW: Int = 150
    const val ROW_MAX_INNER: Float = 720f
    const val ROW_SIDE_INNER: Float = 24f
    const val ROW_SIDE_COVER: Float = 12f
    const val EXTRA_ROW_INSET: Float = 12f

    /**
     * The one bar of a frame [width] dp wide: one bar when any pane is active (never two); the switch only on the
     * inner screen when both panes are active with a title, cells left pane then right; the target is the scope
     * pane when active, else the other active pane. A locked pane's title is never a cell.
     */
    fun layout(width: Float, inner: Boolean, list: PaneCommand?, detail: PaneCommand?, scope: CommandScope): CommandBarLayout {
        val panes = listOf(CommandScope.LIST to list, CommandScope.DETAIL to detail).filter { it.second?.active == true }
        val withTitle = panes.filter { it.second?.title != null }
        val showsSwitch = inner && panes.size == 2 && withTitle.size == 2
        val target = when {
            panes.any { it.first == scope } -> scope
            panes.isNotEmpty() -> panes.first().first
            else -> null
        }
        val extra = if (inner) panes.any { it.second?.hasExtraRow == true } else panes.firstOrNull { it.first == target }?.second?.hasExtraRow == true
        val rowWidth = if (inner) minOf(width - 2 * ROW_SIDE_INNER, ROW_MAX_INNER) else width - 2 * ROW_SIDE_COVER
        return CommandBarLayout(
            barCount = if (panes.isEmpty()) 0 else 1,
            showsSwitch = showsSwitch,
            cells = if (showsSwitch) withTitle.mapNotNull { it.second?.title } else emptyList(),
            cellScopes = if (showsSwitch) withTitle.map { it.first } else emptyList(),
            target = target,
            barHeight = if (panes.isEmpty()) 0 else if (extra) BAR_WITH_ROW else BAR,
            barWidth = width,
            commandRowWidth = rowWidth.coerceAtLeast(0f),
        )
    }

    /** A pane's extra row: `[start, end]` in the frame, 12 dp inside the pane's edges (and the 1 dp divider). */
    fun extraRowSpan(scope: CommandScope, frameWidth: Float, leftWidth: Float, divider: Float = 1f): ClosedFloatingPointRange<Float> {
        val (from, to) = if (scope == CommandScope.LIST) 0f to leftWidth else (leftWidth + divider) to frameWidth
        val start = from + EXTRA_ROW_INSET
        return start..maxOf(start, to - EXTRA_ROW_INSET)
    }
}

/** Copy of the frame's command bar. */
object CommandBarCopy {
    /**
     * Accessibility label of the scope switch (DS-65 §3). No source gives one yet: `TODO_COPY_DS_COMMAND_SCOPE`
     * is listed under Requests in `docs/status/shell.md` (Q-DS-24). Until then the group has no label of its
     * own and each cell reads its pane title.
     */
    val SCOPE_GROUP: String? = null

    val all: List<String> = listOfNotNull(SCOPE_GROUP)
}

/**
 * The frame's one command bar in `목록 + 상세` (DS-65). Each pane's `CivilightScreen` hands its declaration here
 * ([declare]) instead of drawing a bar; the scene draws [CommandFrameBar] once over both panes and gives both
 * panes the same bottom padding ([panePadding]). Registrations are keyed by a per-screen token, so the outgoing
 * detail of a crossfade never removes the incoming one's; the newest registration of a role wins.
 */
@Stable
class CommandBarHost(initial: CommandScope = CommandScope.LIST) {
    internal class Registration(
        val token: Any,
        val scope: CommandScope,
        val title: String,
        val commandBar: (@Composable () -> Unit)?,
        val extraRow: (@Composable RowScope.() -> Unit)?,
        val locals: CompositionLocalContext,
    )

    private val registrations = mutableStateListOf<Registration>()
    private val sheets = mutableStateMapOf<Any, Boolean>()

    /** The scope pane (changed only by [onEvent]). */
    var scope by mutableStateOf(initial)
        private set

    /** The drawn bar's height including the navigation inset (kept while the bar is hidden under a sheet). */
    var barHeight by mutableStateOf(0.dp)
        internal set

    fun onEvent(event: CommandScopeEvent) {
        scope = CommandScope.next(scope, event)
    }

    internal fun declare(registration: Registration) {
        val i = registrations.indexOfFirst { it.token === registration.token }
        if (i >= 0) registrations[i] = registration else registrations.add(registration)
    }

    internal fun withdraw(token: Any) {
        registrations.removeAll { it.token === token }
        sheets.remove(token)
    }

    internal fun sheetOpen(token: Any, open: Boolean) {
        if (sheets[token] != open) sheets[token] = open
    }

    /** True while a pane shows a sheet: the frame bar steps aside so the sheet and its scrim are never under it. */
    val anySheetOpen: Boolean get() = sheets.values.any { it }

    internal fun active(scope: CommandScope): Registration? = registrations.lastOrNull { it.scope == scope && it.commandBar != null }

    /** Any pane declares a command bar. */
    val hasBar: Boolean get() = registrations.any { it.commandBar != null }

    /** The bottom padding every pane gets: the frame bar's height, or 0 with no bar. */
    val panePadding: Dp get() = if (hasBar) barHeight else 0.dp

    internal fun paneCommand(scope: CommandScope): PaneCommand? = active(scope)?.let {
        PaneCommand(declared = true, title = it.title, hasExtraRow = it.extraRow != null)
    }
}

/** The frame's [CommandBarHost], provided by the `목록 + 상세` scene (null elsewhere: screens draw their own bar). */
val LocalCommandBarHost = staticCompositionLocalOf<CommandBarHost?> { null }

/**
 * True in the one frame in which something was locked again (app lock, hidden-project relock, `다시 잠그기`);
 * provided by the shell. The pane that locks leaves only after that frame, so the frame bar must not show pane
 * titles meanwhile (a hidden project's name, LIB rule 19).
 */
val LocalRelocked = compositionLocalOf { false }

/**
 * The frame's one bottom glass bar (DS-65) over both panes: the scope switch (both panes declare a bar), the
 * target pane's command bar (crossfading on a scope change, the bar itself never moves), and each pane's extra
 * row confined to its column. [leftWidth] is the list pane's width in dp as drawn now. The bar steps aside
 * (fade) while a pane shows a sheet. [hideTitles] drops the switch (pane titles) for a frame being relocked.
 */
@Composable
fun CommandFrameBar(host: CommandBarHost, leftWidth: () -> Float, modifier: Modifier = Modifier, hideTitles: Boolean = false) {
    val motion = LocalMotion.current
    val density = LocalDensity.current
    val list = host.paneCommand(CommandScope.LIST)
    val detail = host.paneCommand(CommandScope.DETAIL)
    val layout = CommandBarRules.layout(width = 0f, inner = true, list = list, detail = detail, scope = host.scope)
    val target = layout.target?.let { host.active(it) }
    AnimatedVisibility(
        visible = target != null && !host.anySheetOpen,
        modifier = modifier,
        enter = motion.enter(CivilightMotion.enter),
        exit = motion.exit(CivilightMotion.exit),
        label = "frameBar",
    ) {
        val listRow = host.active(CommandScope.LIST)?.takeIf { it.extraRow != null }
        val detailRow = host.active(CommandScope.DETAIL)?.takeIf { it.extraRow != null }
        BottomGlassBar(
            modifier = Modifier.onSizeChanged { with(density) { host.barHeight = it.height.toDp() } },
            barHeight = layout.barHeight.takeIf { it > 0 }?.dp,
            inner = true,
            leading = if (layout.showsSwitch && !hideTitles) {
                {
                    OutlinedSegmented(
                        items = layout.cells.map { SegmentItem(it) },
                        selectedIndex = layout.cellScopes.indexOf(layout.target).coerceAtLeast(0),
                        onSelect = { i -> layout.cellScopes.getOrNull(i)?.let { host.onEvent(CommandScopeEvent.Picked(it)) } },
                        groupLabel = CommandBarCopy.SCOPE_GROUP,
                        maxCellWidth = LayoutMetrics.scopeCellMax,
                    )
                }
            } else null,
            paneRows = if (listRow != null || detailRow != null) {
                { PaneExtraRows(leftWidth, listRow, detailRow) }
            } else null,
        ) {
            // The target pane's command bar, with that pane's composition locals (view models, project frame).
            ContentSwap(target, contentKey = { it?.token }, contentAlignment = Alignment.CenterStart, label = "frameCommand") { reg ->
                if (reg?.commandBar != null) {
                    CompositionLocalProvider(reg.locals) { OnGlass { reg.commandBar.invoke() } }
                }
            }
        }
        // The frame's bar is the window's bottom chrome (toast slot, popover limit; DS-66, DS-67).
        ReportBottomChrome(host.barHeight - rememberSystemBottom())
    }
}

/** Each pane's extra row inside its own column, 12 dp from the pane edges and the divider (DS-65 §6). */
@Composable
private fun PaneExtraRows(
    leftWidth: () -> Float,
    list: CommandBarHost.Registration?,
    detail: CommandBarHost.Registration?,
) {
    val rows = listOf(list, detail)
    Layout(
        contents = rows.map { reg ->
            @Composable {
                if (reg?.extraRow != null) {
                    CompositionLocalProvider(reg.locals) {
                        OnGlass {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                reg.extraRow.invoke(this)
                            }
                        }
                    }
                }
            }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val left = leftWidth() * density
        val placeables = measurables.mapIndexed { i, ms ->
            val span = CommandBarRules.extraRowSpan(if (i == 0) CommandScope.LIST else CommandScope.DETAIL, width / density, left / density)
            val start = (span.start * density).roundToInt()
            val w = ((span.endInclusive - span.start) * density).roundToInt().coerceAtLeast(0)
            start to ms.map { it.measure(Constraints(minWidth = w, maxWidth = w)) }
        }
        val height = placeables.maxOfOrNull { (_, ps) -> ps.maxOfOrNull { it.height } ?: 0 } ?: 0
        layout(width, height) {
            placeables.forEach { (x, ps) -> ps.forEach { it.place(x, (height - it.height) / 2) } }
        }
    }
}
