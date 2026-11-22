package io.github.lenticualrstone.civilight.ui.scaffold

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import io.github.lenticualrstone.civilight.designsystem.component.CivilightSheetLayout
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import io.github.lenticualrstone.civilight.navigation.RouteCodec
import io.github.lenticualrstone.civilight.navigation.SharedSheet
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.navigation.routeName
import io.github.lenticualrstone.civilight.ui.feature.SheetRegistry
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout

/**
 * The sheets open over one screen, as a stack (a sheet may open another; closing returns to the
 * one below). Saved with the screen (CNV-35) through [RouteCodec], so sheet keys must be
 * `@Serializable`.
 */
@Stable
class SheetHost(initial: List<SheetKey> = emptyList()) {
    private val stack = mutableStateListOf<SheetKey>().apply { addAll(initial) }

    val top: SheetKey? get() = stack.lastOrNull()

    val sheets: List<SheetKey> get() = stack.toList()

    fun open(sheet: SheetKey) {
        if (stack.lastOrNull() != sheet) stack.add(sheet)
    }

    /** Replaces the top sheet (tabs inside one sheet flow). */
    fun replace(sheet: SheetKey) {
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
        stack.add(sheet)
    }

    fun close() {
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
    }

    fun closeAll() = stack.clear()

    companion object {
        val Saver: Saver<SheetHost, String> = Saver(
            save = { RouteCodec.encodeAll(it.stack.toList()) },
            restore = { SheetHost(RouteCodec.decodeAll(it).filterIsInstance<SheetKey>()) },
        )
    }
}

/** A screen's sheet stack, kept across configuration changes and process death. */
@Composable
fun rememberSheetHost(): SheetHost = rememberSaveable(saver = SheetHost.Saver) { SheetHost() }

/** Content of shared sheets; provided by the shell from `FeatureDeps.sheets`. */
val LocalSheetRegistry = staticCompositionLocalOf { SheetRegistry() }

/** Content of a screen's own sheets: return null for keys the registry should draw. */
typealias LocalSheets = (SheetKey) -> (@Composable SheetScope.() -> Unit)?

/**
 * Draws [content] with the top sheet of [host] over it (DS-23: cover bottom sheet; inner screens
 * a centred sheet). The sheet's content comes from [localSheets] first, then the shared
 * [SheetRegistry]; an unregistered shared sheet shows a placeholder naming it.
 */
@Composable
fun SheetHostLayout(
    host: SheetHost,
    modifier: Modifier = Modifier,
    localSheets: LocalSheets = { null },
    content: @Composable () -> Unit,
) {
    val registry = LocalSheetRegistry.current
    val isInner = LocalWindowLayout.current.isInner
    // Keep the last sheet while it animates out.
    var shown by remember { mutableStateOf(host.top) }
    host.top?.let { shown = it }
    // Only the top sheet is composed. A sheet under another one keeps its saveable state (drafts, an
    // editor's text, a pending picker request) until it is closed; a closed sheet starts fresh.
    val holder = rememberSaveableStateHolder()
    val seen = remember { HashSet<String>() }
    val open = host.sheets.mapTo(HashSet()) { RouteCodec.encode(it) }
    SideEffect {
        val gone = seen - open
        gone.forEach(holder::removeState)
        seen.removeAll(gone)
    }
    // A sheet opened over an empty host starts fresh; only sheet-to-sheet changes crossfade (S4).
    val isOpen = host.top != null
    val track = remember { OpenTrack(isOpen) }
    val session = track.session(isOpen)
    val motion = LocalMotion.current
    CivilightSheetLayout(
        sheetVisible = isOpen,
        onDismissRequest = { host.close() },
        modifier = modifier,
        centered = isInner,
        sheet = {
            key(session) {
                // The sheet surface already animates its height (DS `animateContentSize`), so the swap itself
                // only crossfades and leaves sizing to the surface.
                AnimatedContent(
                    targetState = shown,
                    contentKey = { it?.let(RouteCodec::encode) },
                    transitionSpec = {
                        ContentTransform(
                            fadeIn(motion.spec(CivilightMotion.standard)),
                            fadeOut(motion.spec(CivilightMotion.exit)),
                            sizeTransform = null,
                        )
                    },
                    label = "sheetSwap",
                ) { sheet ->
                    if (sheet != null) {
                        Column {
                            val id = RouteCodec.encode(sheet)
                            if (id in open) seen += id
                            key(id) {
                                holder.SaveableStateProvider(id) {
                                    val column = this@Column
                                    val scope = remember(column, host, isInner) { HostSheetScope(column, host, isInner) }
                                    scope.SheetBody(sheet, localSheets, registry)
                                }
                            }
                        }
                    }
                }
            }
        },
        content = content,
    )
}

/** Counts openings of an empty host (plain values: read and written only while composing this host). */
private class OpenTrack(private var wasOpen: Boolean) {
    private var count = 0

    fun session(isOpen: Boolean): Int {
        if (isOpen && !wasOpen) count++
        wasOpen = isOpen
        return count
    }
}

/** The [SheetScope] of a host: the sheet's column plus the host's stack. */
private class HostSheetScope(column: ColumnScope, private val host: SheetHost, override val inner: Boolean) :
    SheetScope, ColumnScope by column {
    override fun close() = host.close()
    override fun open(sheet: SheetKey) = host.open(sheet)
}

@Composable
private fun SheetScope.SheetBody(sheet: SheetKey, localSheets: LocalSheets, registry: SheetRegistry) {
    val local = localSheets(sheet)
    val shared = registry.contentFor(sheet)
    when {
        local != null -> local()
        shared != null -> shared(sheet)
        else -> PlaceholderSheet(sheet, this)
    }
}

/** Stand-in for a sheet whose feature has not registered it yet (development only). */
@Composable
fun PlaceholderSheet(sheet: SheetKey, scope: SheetScope) {
    val owner = (sheet as? SharedSheet)?.owner?.gradlePath
    SheetHeader(title = sheet.routeName, onClose = scope::close, subtitle = owner)
    CivilightText(sheet.toString(), CivilightTheme.type.small, color = CivilightTheme.colors.ink3)
}
