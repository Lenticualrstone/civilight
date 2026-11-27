package io.github.lenticualrstone.civilight.feature.library.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.EmptyBlock
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.LockRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import io.github.lenticualrstone.civilight.domain.library.common.TimeLabels
import io.github.lenticualrstone.civilight.domain.library.lock.Relock
import io.github.lenticualrstone.civilight.domain.library.newproject.ProjectDeleteDialog
import io.github.lenticualrstone.civilight.domain.library.newproject.ProjectMenuAction
import io.github.lenticualrstone.civilight.domain.library.newproject.RowMeta
import io.github.lenticualrstone.civilight.domain.library.rating.RatingLabels
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.feature.library.common.LibraryDocs
import io.github.lenticualrstone.civilight.feature.library.common.sheetBody
import io.github.lenticualrstone.civilight.feature.library.shelf.DeleteProjectDialog
import io.github.lenticualrstone.civilight.feature.library.shelf.ProjectMenuContent
import io.github.lenticualrstone.civilight.feature.library.shelf.ProjectRow
import io.github.lenticualrstone.civilight.feature.library.shelf.ProjectRowUi
import io.github.lenticualrstone.civilight.feature.library.shelf.RenameContent
import io.github.lenticualrstone.civilight.feature.library.shelf.RenameSheet
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCards
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCopy
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfMenuSheet
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfState
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.Protections
import io.github.lenticualrstone.civilight.navigation.ExportSheet
import io.github.lenticualrstone.civilight.navigation.ImagePickSheet
import io.github.lenticualrstone.civilight.navigation.Library
import io.github.lenticualrstone.civilight.navigation.ProjectHome
import io.github.lenticualrstone.civilight.navigation.ProjectRoute
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.resetTo
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.store.ReadOnlyProjectException
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.security.AuthCopy
import io.github.lenticualrstone.civilight.ui.security.AuthRequest
import io.github.lenticualrstone.civilight.ui.security.AuthResult
import io.github.lenticualrstone.civilight.ui.security.ProtectWindow
import io.github.lenticualrstone.civilight.ui.security.WindowProtection
import io.github.lenticualrstone.civilight.ui.window.CenterColumn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Dedicated Settings vault for hidden projects (`설정 › 보관 · 보안 › 숨긴 항목 보기`).
 * Hidden projects never appear on the main Library shelf; they are only loaded and displayed here
 * after passing biometric / device-credential authentication.
 */
@Composable
internal fun HiddenProjectsScreen(services: LibraryServices) {
    val deps = services.deps
    val navigator = deps.navigator
    val vm = entryViewModel { HiddenProjectsViewModel(services) }
    val unlock by deps.unlock.state.collectAsState()
    val settings by deps.settings.collectAsState()
    val rows by vm.hiddenRows.collectAsState()
    val docs by vm.documents.collectAsState()
    val sheets = rememberSheetHost()
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    var promptedOnce by rememberSaveable { mutableStateOf(false) }

    // Prompt biometric authentication once automatically upon entering while locked.
    LaunchedEffect(Unit) {
        if (!unlock.hiddenGroupOpen && !promptedOnce) {
            promptedOnce = true
            vm.unlock()
        }
    }

    // Never keep sheets or delete dialogs alive after a relock.
    LaunchedEffect(unlock.hiddenGroupOpen, rows) {
        if (!unlock.hiddenGroupOpen) {
            while (sheets.top != null) sheets.close()
            deleting = null
        } else if (deleting != null && rows.none { it.key == deleting }) {
            deleting = null
        }
    }

    CenterColumn {
        CivilightScreen(
            title = SettingsCopy.HIDDEN_VIEW_TITLE,
            smallLine = SettingsCopy.SECTION_KEEP_SECURITY,
            back = IconAction("뒤로") { navigator.pop() },
            actions = {
                if (unlock.hiddenGroupOpen) {
                    CivilightButton(ShelfCopy.RELOCK, vm::relock, variant = ButtonVariant.COMPACT_GHOST, icon = IconId.Lock)
                }
            },
            sheets = sheets,
            localSheets = { key ->
                if (!unlock.hiddenGroupOpen) return@CivilightScreen null
                when (key) {
                    is ShelfMenuSheet -> sheetBody {
                        docs[key.projectKey]?.let { doc ->
                            ProjectMenuContent(doc, false, deps.stores.blobs::contains) { action ->
                                when (action) {
                                    ProjectMenuAction.OPEN -> {
                                        close()
                                        navigator.push(ProjectHome(key.projectKey))
                                        val block = doc.app.activity.activity.lastEditedBlockKey?.let(doc.index::block)
                                        if (block != null && doc.liveness.isLive(block)) {
                                            navigator.push(Script(key.projectKey, blockKey = block.key, caretAtEnd = true))
                                        }
                                    }
                                    ProjectMenuAction.RENAME -> {
                                        close()
                                        open(RenameSheet(key.projectKey))
                                    }
                                    ProjectMenuAction.UNHIDE -> {
                                        close()
                                        vm.unhide(key.projectKey)
                                    }
                                    ProjectMenuAction.BACKUP_EXPORT -> {
                                        close()
                                        open(ExportSheet(key.projectKey))
                                    }
                                    ProjectMenuAction.DELETE -> {
                                        close()
                                        deleting = key.projectKey
                                    }
                                    ProjectMenuAction.COVER -> {
                                        close()
                                        open(ImagePickSheet(key.projectKey, ImageOwnerKind.PROJECT, key.projectKey, ImageSlot.PROJECT_COVER))
                                    }
                                    ProjectMenuAction.RATING, ProjectMenuAction.HIDE -> close()
                                }
                            }
                        }
                    }
                    is RenameSheet -> sheetBody {
                        docs[key.projectKey]?.let { doc ->
                            RenameContent(doc.project.name) { vm.rename(key.projectKey, it) }
                        }
                    }
                    else -> null
                }
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 12.dp,
                    bottom = padding.calculateBottomPadding() + 24.dp,
                    start = 24.dp,
                    end = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!unlock.hiddenGroupOpen) {
                    item(key = "locked") {
                        LockRow(
                            title = SettingsCopy.HIDDEN_VIEW_LOCKED,
                            subtitle = SettingsCopy.HIDDEN_VIEW_LOCKED_SUB,
                            actionLabel = SettingsCopy.HIDDEN_VIEW_UNLOCK,
                            onAction = vm::unlock,
                            actionIcon = IconId.Fingerprint,
                        )
                    }
                } else if (rows.isEmpty()) {
                    item(key = "empty") {
                        EmptyBlock(
                            title = SettingsCopy.HIDDEN_VIEW_EMPTY,
                            subtitle = Relock.openGroupNote(settings.relockPolicy),
                        )
                    }
                } else {
                    item(key = "section") {
                        SectionLabel("${SettingsCopy.HIDDEN_PROJECTS} · ${rows.size} · ${Relock.openGroupNote(settings.relockPolicy)}")
                    }
                    items(rows, key = { it.key }) { row ->
                        ProtectWindow(WindowProtection.forProject(row.protections))
                        ProjectRow(
                            row = row,
                            onOpen = { navigator.push(ProjectHome(row.key)) },
                            onMenu = { sheets.open(ShelfMenuSheet(row.key)) },
                            modifier = civilightItem(),
                            extra = {
                                TextAction(
                                    ShelfCopy.UNHIDE,
                                    { vm.unhide(row.key) },
                                    color = CivilightTheme.colors.ink3,
                                    style = CivilightTheme.type.small,
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    deleting?.takeIf { unlock.hiddenGroupOpen }?.let { key ->
        val doc = docs[key] ?: return@let
        DeleteProjectDialog(
            dialog = ProjectDeleteDialog.of(doc),
            protections = doc.project.protections,
            onConfirm = {
                deleting = null
                vm.moveToTrash(key)
            },
            onDismiss = { deleting = null },
        )
    }
}

internal class HiddenProjectsViewModel(private val services: LibraryServices) : ViewModel() {
    private val deps = services.deps
    private val stores = deps.stores
    private val libraryDocs = LibraryDocs(stores.library, viewModelScope)

    val documents: StateFlow<Map<Key, ProjectDoc?>> = libraryDocs.docs

    val hiddenRows: StateFlow<List<ProjectRowUi>> = combine(
        stores.library.state,
        libraryDocs.docs,
        deps.unlock.state,
    ) { library, docs, unlock ->
        if (!unlock.hiddenGroupOpen) return@combine emptyList()
        val now = deps.clock.now()
        val zone = deps.clock.zone
        val fileExists: (String) -> Boolean = { hash -> stores.blobs.contains(hash) }
        library.live
            .filter { it.hidden && !it.isExample }
            .sortedByDescending { it.updatedAt }
            .map { entry ->
                val doc = docs[entry.key]
                val name = doc?.project?.name ?: entry.name
                ProjectRowUi(
                    key = entry.key,
                    title = name,
                    rating = RatingLabels.effective(doc?.project?.rating ?: entry.rating),
                    meta = doc?.let { RowMeta.of(it, entry.updatedAt, now, zone) } ?: TimeLabels.relative(entry.updatedAt, now, zone),
                    writing = doc?.let(ShelfState::isWriting) ?: false,
                    protections = doc?.project?.protections ?: Protections(hidden = true),
                    readOnly = entry.readOnly,
                    thumb = ShelfCards.thumb(doc, name, fileExists),
                    format = doc?.project?.format?.known,
                    edited = TimeLabels.relative(entry.updatedAt, now, zone),
                    ageDays = ShelfState.ageDays(entry.updatedAt, now, zone),
                )
            }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun unlock() {
        viewModelScope.launch {
            if (deps.biometric.authenticate(AuthRequest(AuthCopy.HIDDEN_PROJECTS)) == AuthResult.SUCCESS) {
                deps.unlock.unlockHiddenGroup()
            }
        }
    }

    fun relock() = deps.unlock.relockNow()

    fun unhide(key: Key) {
        viewModelScope.launch { services.protection.unhideInOpenGroup(key) }
    }

    fun rename(key: Key, name: String) {
        viewModelScope.launch {
            runCatching { stores.library.rename(key, name) }
                .onFailure { if (it !is ReadOnlyProjectException && it !is IllegalArgumentException) throw it }
        }
    }

    fun moveToTrash(key: Key) {
        val navigator = deps.navigator
        if (navigator.backStack.any { (it as? ProjectRoute)?.projectKey == key }) navigator.resetTo(Library)
        viewModelScope.launch {
            runCatching { stores.library.moveToTrash(key) }
                .onSuccess { stores.appState.removeRecentsOf(key) }
                .onFailure { if (it !is ReadOnlyProjectException) throw it }
        }
    }
}
