package io.github.lenticualrstone.civilight.feature.world.time

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import io.github.lenticualrstone.civilight.feature.world.common.BodyStage
import io.github.lenticualrstone.civilight.feature.world.common.CardBodySwap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.FieldTableRow
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.LockRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.common.CardDescription
import io.github.lenticualrstone.civilight.feature.world.common.Restored
import io.github.lenticualrstone.civilight.feature.world.common.WorldCommandBar
import io.github.lenticualrstone.civilight.feature.world.common.WorldEnv
import io.github.lenticualrstone.civilight.feature.world.common.cardPadding
import io.github.lenticualrstone.civilight.feature.world.common.worldBack
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.navigation.EventDetail
import io.github.lenticualrstone.civilight.navigation.LensSheet
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.lens.lensChipLabel
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.CenterColumn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/** The event's `⋯` (feature-local): `고치기 · 지우기`. */
@Serializable
data class EventMenuSheet(val eventKey: Key) : SheetKey

/**
 * 사건 상세 (right pane of board `TwoTimeline`; full screen on the cover): pane title
 * `연표 · 세계 사건`, name, time, description, rows `장소 · 그때 나이 · 원고 · 관련 용어`, and
 * `이 사건에서 바뀐 것`. Every field opens the event sheet (WLD-32 `고치기`); `⋯` has `고치기 · 지우기`.
 * A trashed event shows its dotted label with `되살리기`.
 */
@Composable
fun EventDetailScreen(route: EventDetail, deps: FeatureDeps) {
    val vm = entryViewModel { EventDetailViewModel(route.projectKey, route.eventKey, WorldEnv.of(deps)) }
    val ui by vm.ui.collectAsState()
    val doc by vm.doc.collectAsState()
    val readOnly by vm.readOnly.collectAsState()
    val sheets = rememberSheetHost()
    val gate = rememberHiddenGate(route.projectKey)
    val scope = rememberCoroutineScope()
    val detail = ui
    val edit: (() -> Unit)? = if (readOnly || detail?.access != EventAccess.Open) null else ({ sheets.open(EventEditSheet(route.eventKey)) })

    CivilightScreen(
        title = when (val a = detail?.access) {
            EventAccess.Open, EventAccess.Locked -> detail.name
            is EventAccess.Gone -> a.text
            null -> ""
        },
        smallLine = detail?.paneTitle,
        back = worldBack(route, deps),
        lens = doc?.let(::lensChipLabel),
        onLens = { sheets.open(LensSheet(route.projectKey)) },
        actions = {
            if (detail?.access == EventAccess.Open) {
                CivilightIconButton(
                    icon = IconId.More,
                    contentDescription = WorldCopy.menuLabel(detail.name),
                    onClick = { sheets.open(EventMenuSheet(route.eventKey)) },
                )
            }
        },
        commandBar = { WorldCommandBar(deps, route.projectKey, WorldCopy.SEARCH_CARD) },
        sheets = sheets,
        localSheets = { key ->
            when (key) {
                is EventEditSheet -> { { EventEditSheetContent(route.projectKey, key) } }
                is EventChangeSheet -> {
                    {
                        EventChangeContent(key.eventKey) { draft ->
                            vm.addChange(draft).also { added -> if (added) deps.toasts.show(WorldCopy.CHANGE_ADDED_TOAST) { vm.undoLast() } }
                        }
                    }
                }
                is EventMenuSheet -> {
                    {
                        EventMenuContent(
                            name = detail?.name.orEmpty(),
                            readOnly = readOnly,
                            onEdit = { open(EventEditSheet(key.eventKey)) },
                            onDelete = {
                                scope.launch {
                                    val deleted = vm.delete(key.eventKey) ?: return@launch
                                    deps.toasts.show(deleted.toast.message, actionLabel = deleted.toast.action) { vm.undoLast() }
                                }
                            },
                        )
                    }
                }
                else -> null
            }
        },
    ) { padding ->
        CenterColumn {
            CardBodySwap(detail, { eventStage(it?.access) }) { shown ->
                val detail = shown
                LazyColumn(Modifier.fillMaxSize(), contentPadding = cardPadding(padding)) {
                    if (detail == null) return@LazyColumn
                    when (val access = detail.access) {
                        EventAccess.Locked -> item(key = "locked") {
                            LockRow(title = WorldCopy.HIDDEN_SCENE, actionLabel = WorldCopy.UNLOCK, onAction = gate.unlock, modifier = civilightItem().padding(top = 12.dp))
                        }
                        is EventAccess.Gone -> item(key = "gone") {
                            Column(civilightItem()) {
                                if (access.text.isNotEmpty()) ListRow(title = access.text, titleDotted = true, modifier = Modifier.padding(top = 12.dp))
                                FoldReveal(access.restorable && !readOnly) {
                                    CivilightButton(
                                        text = WorldCopy.BTN_RESTORE,
                                        variant = ButtonVariant.GHOST,
                                        modifier = Modifier.padding(top = 12.dp),
                                        onClick = {
                                            scope.launch {
                                                when (val r = vm.restore(route.eventKey, authenticated = gate.open)) {
                                                    is Restored.Done -> deps.toasts.show(r.toast.message, actionLabel = r.toast.action) { vm.undoLast() }
                                                    Restored.NeedsAuth -> gate.unlock()
                                                    Restored.NotFound -> Unit
                                                }
                                            }
                                        },
                                    )
                                }
                            }
                        }
                        EventAccess.Open -> {
                            item(key = "head") {
                                Column(
                                    civilightItem()
                                        .fillMaxWidth()
                                        .then(if (edit != null) Modifier.pressable(PressAction.Click(edit), role = Role.Button, scaleOnPress = false) else Modifier),
                                ) {
                                    CivilightText(detail.name, CivilightTheme.type.pageTitle, Modifier.padding(top = 20.dp), color = CivilightTheme.colors.ink)
                                    if (detail.time.isNotEmpty()) {
                                        CivilightText(detail.time, CivilightTheme.type.number, Modifier.padding(top = 4.dp), color = CivilightTheme.colors.ink3)
                                    }
                                    CardDescription(detail.description)
                                }
                            }
                            item(key = "rows") {
                                Column(civilightItem().fillMaxWidth().padding(top = 14.dp)) {
                                    detail.rows.forEach { (label, value) -> FieldTableRow(label, value, onClick = edit) }
                                }
                            }
                            val canAdd = detail.changeOffered && !readOnly
                            if (detail.changes.isNotEmpty() || canAdd) {
                                item(key = "changes") { SectionLabel(WorldCopy.EVENT_CHANGES, civilightItem().padding(top = 22.dp, bottom = 2.dp)) }
                                items(detail.changes, key = { it.id }) { change -> Box(civilightItem()) { FieldTableRow(change.label, change.value) } }
                                if (canAdd) {
                                    item(key = "addChange") {
                                        TextAction(
                                            WorldCopy.CHANGE_ADD,
                                            { sheets.open(EventChangeSheet(route.eventKey)) },
                                            civilightItem().padding(top = 4.dp),
                                            color = CivilightTheme.colors.acc,
                                            style = CivilightTheme.type.small,
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
}

/** [EventAccess] as the body stage of the event screen. */
private fun eventStage(access: EventAccess?): BodyStage = when (access) {
    null -> BodyStage.LOADING
    EventAccess.Locked -> BodyStage.LOCKED
    is EventAccess.Gone -> BodyStage.GONE
    EventAccess.Open -> BodyStage.OPEN
}

@Composable
private fun SheetScope.EventMenuContent(name: String, readOnly: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val sheet = this
    SheetHeader(title = name, onClose = ::close)
    SheetScrollBody {
        ListRow(
            title = WorldCopy.BTN_EDIT,
            subtitle = if (readOnly) WorldCopy.READ_ONLY else null,
            onClick = if (readOnly) null else ({ sheet.close(); onEdit() }),
            compact = true,
        )
        ListRow(
            title = WorldCopy.MENU_DELETE,
            subtitle = if (readOnly) WorldCopy.READ_ONLY else null,
            onClick = if (readOnly) null else ({ sheet.close(); onDelete() }),
            compact = true,
        )
    }
}
