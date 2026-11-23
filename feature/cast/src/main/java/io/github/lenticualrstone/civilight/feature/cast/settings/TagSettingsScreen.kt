package io.github.lenticualrstone.civilight.feature.cast.settings

import androidx.compose.runtime.saveable.Saver
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ConfirmDialog
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardCopy
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardMode
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardState
import io.github.lenticualrstone.civilight.designsystem.component.CreateKind
import io.github.lenticualrstone.civilight.designsystem.component.ListCreateRow
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.actions.TagActions
import io.github.lenticualrstone.civilight.domain.trash.TagRestore
import io.github.lenticualrstone.civilight.domain.trash.TrashCopy
import io.github.lenticualrstone.civilight.feature.cast.common.CastWrites
import io.github.lenticualrstone.civilight.feature.cast.common.Step
import io.github.lenticualrstone.civilight.feature.cast.create.CastCreate
import io.github.lenticualrstone.civilight.feature.cast.create.CastCreateCard
import io.github.lenticualrstone.civilight.feature.cast.create.CreateCardCodec
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.AddChip
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.field.FieldSettings
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.common.CastViewModel
import io.github.lenticualrstone.civilight.feature.cast.common.editContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.navigation.TagSettings
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.ProjectSessions
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.CenterColumn
import kotlinx.serialization.Serializable

class TagSettingsViewModel(projectKey: Key, sessions: ProjectSessions, contextOf: () -> EditContext) : CastViewModel(projectKey, sessions, contextOf)

/** One tag's name (feature-local; not drawn); [tagKey] null = a new tag. */
@Serializable
data class TagNameSheet(val projectKey: Key, val tagKey: Key? = null) : SheetKey

/**
 * `TagSettings` (CST-12, CST-75; no board): the live project tags in order with their use (`인물 3 · 이미지 1`).
 * (0.2.0, U-15) The top row `＋ 새 태그` opens the create card (`이름`; `색` waits for the tag palette),
 * `취소 · 만들고 열기 · 만들기`. A tag opens its sheet: `이름` with `저장` (U-10) and the ghost `지우기`
 * (N-44), which confirms with `쓰는 인물 n` and sends only the tag to the trash. Read-only projects show
 * neither the create row nor `지우기`.
 */
@Composable
fun TagSettingsScreen(route: TagSettings, deps: FeatureDeps) {
    val vm = entryViewModel { TagSettingsViewModel(route.projectKey, deps.projects, deps::editContext) }
    val load by vm.state.collectAsState()
    val ready = load as? ProjectLoad.Ready ?: return
    val gate = rememberHiddenGate(route.projectKey)
    val rows = remember(ready.doc, gate.open) { TagSettingsModel.rows(ready.doc, gate.open) }
    val sheets = rememberSheetHost()
    val editable = !ready.readOnly
    val writes = remember(deps, route.projectKey) { CastWrites(deps, route.projectKey) }
    var creating by rememberSaveable(stateSaver = TagCardSaver) { mutableStateOf<CreateCardState?>(null) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    CenterColumn {
        CivilightScreen(
            title = CastUiCopy.TAGS,
            smallLine = FieldSettings.HEAD,
            back = IconAction(CastUiCopy.BACK) { deps.navigator.pop() },
            sheets = sheets,
            localSheets = { key ->
                when (key) {
                    is TagNameSheet -> { { TagNameContent(key, ready.doc, vm, onDelete = { tag -> close(); deleting = tag }) } }
                    else -> null
                }
            },
        ) { padding ->
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 24.dp),
            ) {
                if (editable) {
                    item(key = "create") { ListCreateRow(CreateKind.TAG, { creating = CreateCardState.open(CreateKind.TAG, CreateCardMode.LIST) }, civilightItem()) }
                }
                items(rows, key = { it.key }) { row ->
                    ListRow(
                        row.name,
                        civilightItem(),
                        subtitle = row.line,
                        compact = true,
                        onClick = if (editable) ({ sheets.open(TagNameSheet(route.projectKey, row.key)) }) else null,
                    )
                }
            }
        }
        creating?.takeIf { editable }?.let { card ->
            var busy by remember { mutableStateOf(false) }
            val finish: (Boolean) -> Unit = { openAfter ->
                val made = card
                busy = true
                // The card closes only once the write made the tag; otherwise the typed name stays.
                writes.launch(CreateCardCopy.CREATE, { d, ctx -> CastCreate.createTag(d, made, ctx)?.let { (next, key) -> Step(next, key) } }) { key ->
                    creating = null
                    if (openAfter) sheets.open(TagNameSheet(route.projectKey, key))
                }.invokeOnCompletion { busy = false }
            }
            CastCreateCard(
                state = card,
                onChange = { creating = it },
                onCancel = { creating = null },
                onCreate = { finish(false) },
                onCreateAndOpen = { finish(true) },
                canCreate = !busy && CastCreate.canCreateTag(ready.doc, card),
            )
        }
        deleting?.let { tagKey ->
            val confirm = TagDeleteModel.confirm(ready.doc, tagKey, gate.open)
            if (confirm == null || !editable) {
                deleting = null
            } else {
                ConfirmDialog(
                    title = confirm.title,
                    confirmLabel = CastUiCopy.BTN_TO_TRASH,
                    onConfirm = {
                        deleting = null
                        writes.launch(CastUiCopy.BTN_DELETE, { d, ctx -> TagActions.trash(d, tagKey, ctx)?.let { (next, entry) -> Step(next, entry.key) } }) { entryKey ->
                            deps.toasts.show(CastUiCopy.tagDeleted(confirm.name), TrashCopy.UNDO) {
                                writes.launch(TrashCopy.UNDO, { d, ctx -> (TagActions.restore(d, entryKey, ctx) as? TagRestore.Restored)?.let { Step(it.doc, Unit) } })
                            }
                        }
                    },
                    onDismissRequest = { deleting = null },
                    cancelLabel = CastUiCopy.BTN_CANCEL,
                ) {
                    for (line in confirm.lines) CivilightText(line, CivilightTheme.type.body, color = CivilightTheme.colors.ink)
                    CivilightText(CastUiCopy.TAG_TRASH_FOOT, CivilightTheme.type.small, Modifier.padding(top = 6.dp), color = CivilightTheme.colors.ink3)
                }
            }
        }
    }
}

private val TagCardSaver: Saver<CreateCardState?, String> = Saver(
    save = { it?.let(CreateCardCodec::encode) ?: "" },
    restore = { CreateCardCodec.decode(it) },
)

/** The tag delete confirm (CST-75): `‘주연’ 지우기`, `쓰는 인물 3`, then `이미지 1` only when images hold it. */
data class TagDeleteConfirm(val name: String, val title: String, val lines: List<String>)

object TagDeleteModel {
    /** Null when [tagKey] is not a live tag. Counts follow the settings row (locked hidden characters are not counted). */
    fun confirm(doc: ProjectDoc, tagKey: String, hiddenUnlocked: Boolean): TagDeleteConfirm? {
        val row = TagSettingsModel.rows(doc, hiddenUnlocked).firstOrNull { it.key == tagKey } ?: return null
        val lines = listOf(CastUiCopy.tagUsers(row.usage.characters)) + listOfNotNull(row.usage.images.takeIf { it > 0 }?.let(CastUiCopy::tagImages))
        return TagDeleteConfirm(row.name, CastUiCopy.tagDeleteTitle(row.name), lines)
    }
}

@Composable
private fun SheetScope.TagNameContent(key: TagNameSheet, doc: ProjectDoc, vm: TagSettingsViewModel, onDelete: (String) -> Unit) {
    val current = key.tagKey?.let { k -> io.github.lenticualrstone.civilight.domain.cast.CharacterEdits.liveTag(doc, k) }
    // The sheet only renames (new tags come from the create card, DECISIONS §1): a tag trashed or gone while the
    // sheet is open closes it; a tag just made and not yet in this document waits a frame.
    var seen by remember(key) { mutableStateOf(false) }
    if (current == null) {
        val gone = seen || key.tagKey == null || doc.index.tag(key.tagKey) != null
        if (gone) LaunchedEffect(key) { close() }
        return
    }
    if (!seen) SideEffect { seen = true }
    var name by rememberSaveable(key) { mutableStateOf(current.name) }
    val usable = TagSettingsModel.canName(doc, name, except = key.tagKey) && name.trim() != current.name
    val scope = this
    SheetHeader(current.name, onClose = ::close, closeLabel = CastUiCopy.BTN_CLOSE)
    CivilightTextField(name, { name = it }, Modifier.fillMaxWidth().padding(top = 14.dp), label = CastUiCopy.NAME)
    SheetActions {
        // N-44: `지우기` (ghost) sends only the tag to the trash after a confirm.
        CivilightButton(CastUiCopy.BTN_DELETE, { onDelete(current.key) }, Modifier.weight(1f), variant = ButtonVariant.GHOST)
        CivilightButton(
            CastUiCopy.BTN_SAVE,
            {
                val typed = name
                val tagKey = current.key
                vm.act(CastUiCopy.BTN_SAVE) { d, ctx -> TagSettingsModel.rename(d, tagKey, typed, ctx) ?: d }
                scope.close()
            },
            Modifier.weight(1f),
            enabled = usable,
        )
    }
}
