package io.github.lenticualrstone.civilight.feature.cast.kinds

import io.github.lenticualrstone.civilight.designsystem.component.CreateCardCopy
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardMode
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardState
import io.github.lenticualrstone.civilight.designsystem.component.CreateKind
import io.github.lenticualrstone.civilight.feature.cast.create.CastCreate
import io.github.lenticualrstone.civilight.feature.cast.create.CastCreateCard
import io.github.lenticualrstone.civilight.feature.cast.create.CreateCardCodec
import io.github.lenticualrstone.civilight.ui.project.EditResult

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.AddChip
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.ConfirmDialog
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SegmentedControl
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.field.FieldSettings
import io.github.lenticualrstone.civilight.domain.cast.relation.KindRow
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationKinds
import io.github.lenticualrstone.civilight.domain.trash.TrashCopy
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.common.CastViewModel
import io.github.lenticualrstone.civilight.feature.cast.common.editContext
import io.github.lenticualrstone.civilight.feature.cast.field.rememberCastEditor
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.RelationCategory
import io.github.lenticualrstone.civilight.model.cast.RelationSymmetry
import io.github.lenticualrstone.civilight.navigation.RelationKindSettings
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import io.github.lenticualrstone.civilight.ui.project.ProjectSessions
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.CenterColumn
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class RelationKindViewModel(projectKey: Key, sessions: ProjectSessions, contextOf: () -> EditContext) : CastViewModel(projectKey, sessions, contextOf)

/** `+ 새 종류` (feature-local; not drawn) opened from the `사이` or `마음` section. */
@Serializable
data class NewKindSheet(val projectKey: Key, val category: RelationCategory) : SheetKey

/** One kind's names, shape, switch preview and delete (feature-local; not drawn). */
@Serializable
data class KindEditSheet(val projectKey: Key, val kindKey: Key) : SheetKey

private val ShapeSaver: Saver<KindShape, String> = Saver(
    save = { Json.encodeToString(KindShape.serializer(), it) },
    restore = { runCatching { Json.decodeFromString(KindShape.serializer(), it) }.getOrNull() },
)

/**
 * `프로젝트 설정 › 관계 종류` (CST-32, `RCRelTypes`): `사이 · N` (`둘에게 같은 사실`) with `대칭` chips and
 * `짝` rows (`선배 ↔ 후배 · 쓰임 1 ›`), `마음 · N` chips, and `+ 새 종류` in each. A kind opens its
 * edit sheet. Also the target of `+ 새 종류` in every kind picker (canvas line labels).
 */
@Composable
fun RelationKindSettingsScreen(route: RelationKindSettings, deps: FeatureDeps) {
    val vm = entryViewModel { RelationKindViewModel(route.projectKey, deps.projects, deps::editContext) }
    val load by vm.state.collectAsState()
    val ready = load as? ProjectLoad.Ready ?: return
    val settings = remember(ready.doc) { RelationKinds.settings(ready.doc) }
    val sheets = rememberSheetHost()
    val editable = !ready.readOnly
    val p = route.projectKey
    // CST-76: `+ 새 종류` opens the create card; the section gives the category.
    var kindCard by rememberSaveable(stateSaver = KindCardSaver) { mutableStateOf<CreateCardState?>(null) }
    var kindCategory by rememberSaveable { mutableStateOf(RelationCategory.BETWEEN.name) }
    val openCard: (RelationCategory) -> Unit = { category ->
        kindCategory = category.name
        kindCard = CreateCardState.open(CreateKind.RELATION_KIND, CreateCardMode.LIST)
    }
    CenterColumn {
        CivilightScreen(
            title = CastUiCopy.KIND_TITLE,
            smallLine = FieldSettings.HEAD,
            back = IconAction(CastUiCopy.BACK) { deps.navigator.pop() },
            sheets = sheets,
            localSheets = { key ->
                when (key) {
                    is NewKindSheet -> { { NewKindContent(key) } }
                    is KindEditSheet -> { { KindEditContent(key) } }
                    else -> null
                }
            },
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 24.dp),
            ) {
                SectionLabel(settings.betweenTitle, trailing = settings.betweenNote, trailingAccent = false)
                CivilightText(settings.symmetricTitle, CivilightTheme.type.label, Modifier.padding(top = 12.dp), color = CivilightTheme.colors.ink2)
                KindChips(settings.symmetric) { sheets.open(KindEditSheet(p, it)) }
                Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    CivilightText(settings.pairTitle, CivilightTheme.type.label, Modifier.weight(1f), color = CivilightTheme.colors.ink2)
                    CivilightText(settings.pairNote, CivilightTheme.type.small, color = CivilightTheme.colors.ink3)
                }
                for (row in settings.pairs) {
                    ListRow(
                        row.title,
                        compact = true,
                        trailing = {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                CivilightText(row.usageLabel, CivilightTheme.type.small, color = CivilightTheme.colors.ink3)
                                CivilightIcon(IconId.Next, contentDescription = null, tint = CivilightTheme.colors.ink3)
                            }
                        },
                        onClick = { sheets.open(KindEditSheet(p, row.key)) },
                    )
                }
                if (editable) AddChip(CastUiCopy.KIND_NEW, { openCard(RelationCategory.BETWEEN) }, Modifier.padding(top = 10.dp))
                SectionLabel(settings.mindTitle, Modifier.padding(top = 26.dp), trailing = settings.mindNote, trailingAccent = false)
                KindChips(settings.minds) { sheets.open(KindEditSheet(p, it)) }
                if (editable) AddChip(CastUiCopy.KIND_NEW, { openCard(RelationCategory.MIND) }, Modifier.padding(top = 6.dp))
            }
        }
        var kindBusy by remember { mutableStateOf(false) }
        kindCard?.takeIf { editable }?.let { card ->
            val category = RelationCategory.valueOf(kindCategory)
            val finish: (Boolean) -> Unit = { openAfter ->
                val made = card
                kindBusy = true
                var created: Key? = null
                // The card closes only once the write applied; otherwise the typed values stay.
                vm.act(CreateCardCopy.CREATE, then = { result ->
                    kindBusy = false
                    if (result == EditResult.APPLIED && created != null) kindCard = null
                    if (openAfter && result == EditResult.APPLIED) created?.let { sheets.open(KindEditSheet(p, it)) }
                }) { d, ctx -> CastCreate.createKind(d, category, made, ctx)?.let { (next, k) -> created = k; next } ?: d }
            }
            CastCreateCard(
                state = card,
                onChange = { kindCard = it },
                onCancel = { kindCard = null },
                onCreate = { finish(false) },
                onCreateAndOpen = { finish(true) },
                // CST-76: a name the section already has is refused (the row is listed above).
                canCreate = !kindBusy && CastCreate.canCreateKind(ready.doc, category, card),
                fields = CastCreate.kindFields(between = category == RelationCategory.BETWEEN),
            )
        }
    }
}

private val KindCardSaver: Saver<CreateCardState?, String> = Saver(
    save = { it?.let(CreateCardCodec::encode) ?: "" },
    restore = { CreateCardCodec.decode(it) },
)

@Composable
private fun KindChips(rows: List<KindRow>, onOpen: (Key) -> Unit) {
    ChipFlow(Modifier.padding(top = 8.dp)) {
        for (row in rows) FilterChip(row.title, false, { onOpen(row.key) })
    }
}

/** `사이 · 마음`, and for 사이 `대칭 · 짝 이름` with the other side's name. */
@Composable
private fun ShapeControls(shape: KindShape, onShape: (KindShape) -> Unit) {
    SegmentedControl(
        listOf(CastUiCopy.KIND_BETWEEN, CastUiCopy.KIND_MIND),
        if (shape.category == RelationCategory.BETWEEN) 0 else 1,
        { onShape(shape.copy(category = if (it == 0) RelationCategory.BETWEEN else RelationCategory.MIND)) },
        Modifier.padding(top = 12.dp),
    )
    if (shape.category == RelationCategory.BETWEEN) {
        SegmentedControl(
            listOf(CastUiCopy.KIND_SYMMETRIC, CastUiCopy.KIND_PAIR_NAME),
            if (shape.symmetry == RelationSymmetry.PAIR) 1 else 0,
            { onShape(shape.copy(symmetry = if (it == 1) RelationSymmetry.PAIR else RelationSymmetry.SYMMETRIC)) },
            Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun SheetScope.NewKindContent(sheet: NewKindSheet) {
    val host = this
    val editor = rememberCastEditor(sheet.projectKey)
    var shape by rememberSaveable(sheet, stateSaver = ShapeSaver) { mutableStateOf(KindShape(category = sheet.category)) }
    SheetHeader(CastUiCopy.KIND_NEW, onClose = ::close, subtitle = CastUiCopy.KIND_TITLE, closeLabel = CastUiCopy.BTN_CLOSE)
    SheetScrollBody {
        ShapeControls(shape) { shape = it }
        CivilightTextField(shape.name, { shape = shape.copy(name = it) }, Modifier.fillMaxWidth().padding(top = 12.dp), label = CastUiCopy.NAME)
        if (shape.isPair) {
            CivilightTextField(shape.pairName, { shape = shape.copy(pairName = it) }, Modifier.fillMaxWidth().padding(top = 10.dp), label = CastUiCopy.KIND_PAIR_NAME)
        }
        Row(Modifier.fillMaxWidth().padding(top = 16.dp)) {
            CivilightButton(CastUiCopy.BTN_INSERT, {
                val saved = shape
                editor.act(CastUiCopy.KIND_NEW) { d, ctx -> RelationKindModel.add(d, saved, ctx) }
                host.close()
            }, Modifier.weight(1f), enabled = RelationKindModel.canAdd(shape))
        }
    }
}

/**
 * A kind (not drawn): `이름` (and `짝 이름`) with `저장`; the `사이 / 마음` and `대칭 / 짝`
 * switches, which show `바뀌는 관계 N` and the pairs before `바꾸기` applies them (TC-13-20); and
 * `지우기` (relations keep the text with `◇`, TC-13-12).
 */
@Composable
private fun SheetScope.KindEditContent(sheet: KindEditSheet) {
    val host = this
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val editor = rememberCastEditor(sheet.projectKey)
    val view = remember(doc, sheet.kindKey) { RelationKindModel.edit(doc, sheet.kindKey) }
    if (view == null) {
        LaunchedEffect(Unit) { close() }
        return
    }
    val editable = !store.readOnly
    var shape by rememberSaveable(sheet, stateSaver = ShapeSaver) { mutableStateOf(view.shape) }
    var confirm by rememberSaveable(sheet) { mutableStateOf<String?>(null) }
    SheetHeader(view.title, onClose = ::close, subtitle = view.usage, closeLabel = CastUiCopy.BTN_CLOSE)
    SheetScrollBody {
        if (!editable) return@SheetScrollBody
        CivilightTextField(shape.name, { shape = shape.copy(name = it) }, Modifier.fillMaxWidth().padding(top = 12.dp), label = CastUiCopy.NAME)
        if (shape.isPair) {
            CivilightTextField(shape.pairName, { shape = shape.copy(pairName = it) }, Modifier.fillMaxWidth().padding(top = 10.dp), label = CastUiCopy.KIND_PAIR_NAME)
        }
        if (view.shape.isPair == shape.isPair) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                CivilightButton(CastUiCopy.BTN_SAVE, {
                    val saved = shape
                    editor.act(CastUiCopy.BTN_SAVE) { d, ctx -> RelationKindModel.rename(d, sheet.kindKey, saved, ctx) }
                }, Modifier.weight(1f), enabled = RelationKindModel.namesChanged(view.shape, shape) && shape.name.isNotBlank())
            }
        }
        ShapeControls(shape) { shape = it }
        if (RelationKindModel.shapeChanged(view.shape, shape)) {
            val preview = remember(doc, sheet.kindKey) { RelationKindModel.switchPreview(doc, sheet.kindKey) }
            SectionLabel(preview.countLabel, Modifier.padding(top = 14.dp, bottom = 4.dp))
            for (pair in preview.pairs) CivilightText(pair, CivilightTheme.type.small, color = CivilightTheme.colors.ink2)
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CivilightButton(TrashCopy.CANCEL, { shape = view.shape }, Modifier.weight(1f), variant = ButtonVariant.GHOST)
                CivilightButton(CastUiCopy.BTN_CHANGE, {
                    val saved = shape
                    editor.act(CastUiCopy.BTN_CHANGE) { d, ctx -> RelationKindModel.applySwitch(d, sheet.kindKey, saved, ctx) }
                    host.close()
                }, Modifier.weight(1f), enabled = RelationKindModel.canSwitch(view.shape, shape))
            }
        }
        CivilightButton(CastUiCopy.BTN_PERMANENT_DELETE, { confirm = sheet.kindKey }, Modifier.fillMaxWidth().padding(top = 20.dp), variant = ButtonVariant.GHOST)
    }
    if (confirm != null) {
        // No trash entry (TC-13-12): `‘X’ 영구 삭제` title form, `danger` confirm (DS-25, 0.6 consistency).
        ConfirmDialog(
            title = CastUiCopy.permanentDeleteTitle(view.title),
            confirmLabel = CastUiCopy.BTN_PERMANENT_DELETE,
            onConfirm = {
                confirm = null
                editor.act(CastUiCopy.BTN_PERMANENT_DELETE) { d, ctx -> RelationKindModel.delete(d, sheet.kindKey, ctx) }
                host.close()
            },
            onDismissRequest = { confirm = null },
            facts = listOf(view.usage to ""),
            cancelLabel = TrashCopy.CANCEL,
            hardToUndo = true,
        )
    }
}
