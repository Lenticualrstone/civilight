package io.github.lenticualrstone.civilight.feature.cast.gametext

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.common.CastWrites
import io.github.lenticualrstone.civilight.feature.cast.common.CastViewModel
import io.github.lenticualrstone.civilight.feature.cast.common.Step
import io.github.lenticualrstone.civilight.feature.cast.common.editContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.text.RichDoc
import io.github.lenticualrstone.civilight.navigation.ArchiveDocEdit
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.picker.UnitPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import io.github.lenticualrstone.civilight.ui.project.ProjectSessions
import io.github.lenticualrstone.civilight.ui.richtext.RichTextEditor
import io.github.lenticualrstone.civilight.ui.richtext.rememberRichTextEditorState
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.CenterColumn
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicReference

/** The archive editor's view model (reads; writes go through [CastWrites] so the last one survives leaving). */
class ArchiveDocViewModel(projectKey: Key, sessions: ProjectSessions, contextOf: () -> io.github.lenticualrstone.civilight.domain.cast.EditContext) :
    CastViewModel(projectKey, sessions, contextOf)

/** The unlock point picker (`열리는 시점`); feature-local. */
@Serializable
data class ArchiveUnlockSheet(val projectKey: Key) : SheetKey

/**
 * `아카이브 문서` editor (CST-17; S7 `Voice`, spec [PROPOSED] full-width document editor): `이름`,
 * `열리는 시점` (an episode or chapter; the document opens after it), the Myeongjo body with `@`
 * links, and the character count. It saves as you type (one undo step per typing run); a new
 * document is created on its first save, so leaving an untouched new editor leaves nothing behind.
 * Removing a document is not offered: no source draws it, and a plain removal would bypass the trash.
 */
@Composable
fun ArchiveDocScreen(route: ArchiveDocEdit, deps: FeatureDeps) {
    val vm = entryViewModel { ArchiveDocViewModel(route.projectKey, deps.projects, deps::editContext) }
    val load by vm.state.collectAsState()
    val ready = load as? ProjectLoad.Ready ?: return
    val doc = ready.doc
    val p = route.projectKey
    val c = route.characterKey
    val character = doc.index.character(c)
    if (character == null || (route.docKey != null && GameTextModel.archive(doc, c, route.docKey) == null)) {
        LaunchedEffect(Unit) { deps.navigator.pop() }
        return
    }
    val writes = remember(p) { CastWrites(deps, p) }
    // The key saved so far survives a state restore, so a new document is never created twice.
    var savedKey by rememberSaveable { mutableStateOf(route.docKey) }
    val created = remember { AtomicReference<Key?>(savedKey) }
    val initial = remember { GameTextModel.archiveView(doc, c, savedKey) }
    var title by rememberSaveable { mutableStateOf(initial.title) }
    val body = rememberRichTextEditorState(initial.body, key = "archive-$c-${route.docKey}")
    var lastSaved by remember { mutableStateOf(initial.title to initial.body) }
    val view = GameTextModel.archiveView(doc, c, savedKey, body.result)
    val editable = !ready.readOnly
    val sheets = rememberSheetHost()
    val gate = rememberHiddenGate(p)

    fun save(t: String, b: RichDoc) {
        if (!editable || (t to b) == lastSaved) return
        lastSaved = t to b
        writes.launch(CastUiCopy.ARCHIVE, { d, ctx ->
            GameTextModel.saveArchive(d, c, created.get(), t, b, ctx.edit)?.let { (next, key) ->
                created.set(key)
                Step(next, key)
            }
        }, mergeKey = "archive-$c-${route.docKey}") { key -> savedKey = key }
    }
    val currentSave by rememberUpdatedState({ save(title, body.result) })
    LaunchedEffect(Unit) {
        snapshotFlow { title to body.result }.drop(1).collectLatest { (t, b) ->
            delay(SAVE_DELAY_MS)
            save(t, b)
        }
    }
    DisposableEffect(Unit) { onDispose { currentSave() } }

    CenterColumn {
        CivilightScreen(
            title = view.title.ifEmpty { CastUiCopy.ARCHIVE },
            smallLine = character.name,
            back = IconAction(CastUiCopy.BACK) { deps.navigator.pop() },
            sheets = sheets,
            localSheets = { key ->
                when (key) {
                    is ArchiveUnlockSheet -> { { UnlockPicker(doc, gate, view.unlockUnitKey) { unit -> setUnlock(writes, c, created, unit) } } }
                    else -> null
                }
            },
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(PaddingValues(start = 24.dp, end = 24.dp, top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 24.dp)),
            ) {
                CivilightTextField(title, { title = it }, Modifier.fillMaxWidth(), label = CastUiCopy.NAME, enabled = editable)
                ListRow(
                    CastUiCopy.UNLOCK,
                    subtitle = view.unlock ?: CastUiCopy.EMPTY_VALUE,
                    onClick = if (editable && savedKey != null) ({ sheets.open(ArchiveUnlockSheet(p)) }) else null,
                    compact = true,
                    modifier = Modifier.padding(top = 12.dp),
                )
                RichTextEditor(
                    state = body,
                    projectKey = p,
                    linkPickerTitle = CastUiCopy.LINK_PICKER_TITLE,
                    openSheet = sheets::open,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    enabled = editable,
                    minHeight = 320.dp,
                )
                CivilightText(view.count, CivilightTheme.type.small, Modifier.padding(top = 8.dp), color = CivilightTheme.colors.ink3)
            }
        }
    }
}

private const val SAVE_DELAY_MS = 600L

private fun setUnlock(writes: CastWrites, characterKey: Key, created: AtomicReference<Key?>, unitKey: Key) {
    val key = created.get() ?: return
    writes.launch(CastUiCopy.UNLOCK, { d, ctx -> Step(GameTextModel.setUnlock(d, characterKey, key, unitKey, ctx.edit), Unit) })
}

@Composable
private fun SheetScope.UnlockPicker(
    doc: io.github.lenticualrstone.civilight.model.doc.ProjectDoc,
    gate: io.github.lenticualrstone.civilight.ui.picker.HiddenGate,
    selected: Key?,
    onPick: (Key) -> Unit,
) {
    UnitPickerSheet(doc, gate, CastUiCopy.UNLOCK, onPick = { onPick(it); close() }, onClose = ::close, selectedKey = selected)
}
