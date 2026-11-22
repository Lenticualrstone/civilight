package io.github.lenticualrstone.civilight.feature.cast.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTabs
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.field.Profile
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationViews
import io.github.lenticualrstone.civilight.domain.cast.voice.ArchiveDocs
import io.github.lenticualrstone.civilight.domain.cast.voice.VoiceLines
import io.github.lenticualrstone.civilight.domain.story.search.CommandScreen
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.common.CastViewModel
import io.github.lenticualrstone.civilight.feature.cast.common.RecordFrame
import io.github.lenticualrstone.civilight.feature.cast.common.castLens
import io.github.lenticualrstone.civilight.feature.cast.common.editContext
import io.github.lenticualrstone.civilight.feature.cast.common.recordTopInset
import io.github.lenticualrstone.civilight.feature.cast.field.DisplayNameEditSheet
import io.github.lenticualrstone.civilight.feature.cast.field.DisplayNameHistoryContent
import io.github.lenticualrstone.civilight.feature.cast.field.DisplayNameHistorySheet
import io.github.lenticualrstone.civilight.feature.cast.field.DisplayNameSheetContent
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.text.docText
import io.github.lenticualrstone.civilight.navigation.AddFieldSheet
import io.github.lenticualrstone.civilight.navigation.CharacterProfile
import io.github.lenticualrstone.civilight.navigation.FieldHistorySheet
import io.github.lenticualrstone.civilight.navigation.FieldValueEditSheet
import io.github.lenticualrstone.civilight.navigation.LensSheet
import io.github.lenticualrstone.civilight.navigation.ProfileTab
import io.github.lenticualrstone.civilight.navigation.ProjectSearch
import io.github.lenticualrstone.civilight.navigation.Recording
import io.github.lenticualrstone.civilight.navigation.RelationEditSheet
import io.github.lenticualrstone.civilight.navigation.RelationMap
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.lens.lensChipLabel
import io.github.lenticualrstone.civilight.ui.nav.LocalPaneRole
import io.github.lenticualrstone.civilight.ui.nav.PaneRole
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import io.github.lenticualrstone.civilight.ui.project.ProjectSessions
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.CenterColumn
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout
import io.github.lenticualrstone.civilight.designsystem.component.CommandBar as CommandBarView
import io.github.lenticualrstone.civilight.domain.story.search.CommandBar as CommandHints

/** The character record (CST-07 … CST-12, CST-28); its sheets carry the edits. */
class ProfileViewModel(
    projectKey: Key,
    sessions: ProjectSessions,
    contextOf: () -> io.github.lenticualrstone.civilight.domain.cast.EditContext,
) : CastViewModel(projectKey, sessions, contextOf)

/** 인물 기록 (`Profile`, `RTEditTwo`, `RTPlanned`, `Relations`; inner in the `TwoCast` right pane or `ColCast`). */
@Composable
fun ProfileScreen(route: CharacterProfile, deps: FeatureDeps) {
    val vm = entryViewModel { ProfileViewModel(route.projectKey, deps.projects, deps::editContext) }
    val load by vm.state.collectAsState()
    val ready = load as? ProjectLoad.Ready ?: return
    val doc = ready.doc
    val p = route.projectKey
    val c = route.characterKey
    val gate = rememberHiddenGate(p)
    var tab by rememberSaveable { mutableIntStateOf(route.tab.ordinal) }
    var showEmpty by rememberSaveable { mutableStateOf(false) }
    var headerExpanded by rememberSaveable { mutableStateOf(false) }
    val cover = !LocalWindowLayout.current.isInner
    val ui = ProfileUi(ProfileTab.entries[tab], showEmpty, headerExpanded)
    val state = remember(doc, ui, cover, gate.open, ready.readOnly) { ProfileModel.build(doc, c, ui, cover, gate.open, ready.readOnly) }
    if (state == null) {
        // Trashed, gone or locked while open: leave the record.
        LaunchedEffect(c) { deps.navigator.pop() }
        return
    }
    val sheets = rememberSheetHost()
    val list = rememberLazyListState()
    val collapsed by remember { derivedStateOf { list.firstVisibleItemIndex > 0 || list.firstVisibleItemScrollOffset > 160 } }
    val inDetailPane = LocalPaneRole.current == PaneRole.DETAIL
    val editable = !state.readOnly
    val actions = RecordActions(
        editable = editable,
        onField = { sheets.open(FieldValueEditSheet(p, EntityKind.CHARACTER, c, it)) },
        onFieldHistory = { sheets.open(FieldHistorySheet(p, EntityKind.CHARACTER, c, it)) },
        onFixed = { field ->
            sheets.open(
                when (field) {
                    FixedField.NAME -> NameEditSheet(p, c)
                    FixedField.DISPLAY_NAME -> DisplayNameEditSheet(p, c)
                    FixedField.ONE_LINER -> OneLinerEditSheet(p, c)
                    FixedField.TAGS -> TagsEditSheet(p, c)
                },
            )
        },
        onDisplayNameHistory = { sheets.open(DisplayNameHistorySheet(p, c)) },
        onToggleEmpty = { showEmpty = !showEmpty },
        onAddField = { sheets.open(AddFieldSheet(p, EntityKind.CHARACTER, c)) },
        onMap = { deps.navigator.push(RelationMap(p, c)) },
        onAddRelation = { sheets.open(RelationEditSheet(p, endAKey = c)) },
    )
    CenterColumn {
        RecordFrame(
            back = if (inDetailPane) null else IconAction(CastUiCopy.BACK) { deps.navigator.pop() },
            lens = lensChipLabel(doc),
            onLens = { sheets.open(LensSheet(p)) },
            collapsed = collapsed,
            collapsedTitle = state.name,
            collapsedSmallLine = state.header.title.substringBefore(CastCopy.SEP),
            tabs = { CivilightTabs(CastUiCopy.TABS, tab, { tab = it }, Modifier.weight(1f)) },
            commandBar = {
                CommandBarView(
                    hint = CommandHints.placeholder(CommandScreen.PROFILE),
                    onClick = { deps.navigator.push(ProjectSearch(p)) },
                    onMic = { deps.navigator.push(Recording(projectKey = p, targetKind = RecorderTargetKind.CHARACTER.name, targetKey = c)) },
                    micLabel = CastUiCopy.MIC,
                )
            },
            sheets = sheets,
            localSheets = { key ->
                when (key) {
                    is NameEditSheet -> { { NameSheetContent(key) } }
                    is OneLinerEditSheet -> { { OneLinerSheetContent(key) } }
                    is TagsEditSheet -> { { TagsSheetContent(key) } }
                    is DisplayNameEditSheet -> { { DisplayNameSheetContent(key) } }
                    is DisplayNameHistorySheet -> { { DisplayNameHistoryContent(key) } }
                    else -> null
                }
            },
        ) { padding ->
            LazyColumn(
                state = list,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = recordTopInset(), bottom = padding.calculateBottomPadding() + 24.dp),
            ) {
                item(key = "head") { RecordHead(state, editable, actions) }
                item(key = "table") { HeaderTable(doc, state, actions) { headerExpanded = true } }
                item(key = "tab-${ui.tab}") { TabBody(doc, c, state, ui, gate.open, actions) }
            }
        }
    }
}

/** `인물 기록 007 · 최신 1-7`, the name (40 / 48) and the one-liner; tapping them edits them. */
@Composable
private fun RecordHead(state: ProfileState, editable: Boolean, actions: RecordActions) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(Modifier.fillMaxWidth()) {
        SectionLabel(state.header.title)
        CivilightText(
            state.name,
            type.display,
            Modifier
                .padding(top = 8.dp)
                .then(if (editable) Modifier.pressable(onClick = { actions.onFixed(FixedField.NAME) }, scaleOnPress = false, onClickLabel = CastUiCopy.NAME) else Modifier),
            color = colors.ink,
        )
        state.oneLiner?.let { line ->
            CivilightText(
                line,
                type.lead,
                Modifier
                    .padding(top = 10.dp)
                    .then(if (editable) Modifier.pressable(onClick = { actions.onFixed(FixedField.ONE_LINER) }, scaleOnPress = false, onClickLabel = CastUiCopy.ONE_LINER) else Modifier),
                color = colors.ink2,
            )
        }
    }
}

/** The field table (CST-07): `머리` fields, `첫 등장`, `보이스`; `항목 더 보기` on the cover; `예정` footnote. */
@Composable
private fun HeaderTable(doc: ProjectDoc, state: ProfileState, actions: RecordActions, onMore: () -> Unit) {
    val colors = CivilightTheme.colors
    Column(Modifier.fillMaxWidth().padding(top = 18.dp)) {
        for (row in state.header.rows) FieldRowItem(doc, row, actions)
        state.header.moreLabel?.let { TextAction(it, onMore, color = colors.ink2, style = CivilightTheme.type.small) }
        state.header.plannedFootnote?.let { CivilightText(it, CivilightTheme.type.small, Modifier.padding(top = 14.dp), color = colors.ink3) }
    }
}

@Composable
private fun TabBody(doc: ProjectDoc, characterKey: Key, state: ProfileState, ui: ProfileUi, hiddenUnlocked: Boolean, actions: RecordActions) {
    when (ui.tab) {
        ProfileTab.OVERVIEW -> OverviewTab(doc, state, ui.showEmpty, actions)
        ProfileTab.RELATIONS -> RelationsTab(state, actions)
        ProfileTab.SPEECH -> {
            val c = doc.index.character(characterKey) ?: return
            val lens = doc.castLens
            val chips = remember(doc, hiddenUnlocked) { RelationViews.addressChips(doc, characterKey, lens, hiddenUnlocked).map { it.label } }
            val hidden = RelationViews.hiddenPartnerCount(doc, characterKey, hiddenUnlocked).takeIf { it > 0 }?.let(RelationViews::hiddenRow)
            SpeechTab(c.speechBody.docText, chips, hidden)
        }
        ProfileTab.GAME_TEXT -> {
            val progress = VoiceLines.progress(doc, characterKey)
            val archive = ArchiveDocs.rows(doc, characterKey).map { it.title to it.sub }
            GameTextTab(progress.sectionTitle, progress.fraction, VoiceLines.ORDER_LABEL, VoiceLines.rows(doc, characterKey), ArchiveDocs.sectionTitle(doc, characterKey), archive)
        }
        ProfileTab.SECRET -> {
            val secret = remember(doc) { Profile.secretTab(doc, characterKey, doc.castLens) }
            SecretTab(doc, secret.rows, secret.body.docText, actions)
        }
    }
}
