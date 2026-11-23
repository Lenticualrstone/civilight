package io.github.lenticualrstone.civilight.feature.cast.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.EmptyBlock
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.LockRow
import io.github.lenticualrstone.civilight.designsystem.component.RadioRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.SwitchRow
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import io.github.lenticualrstone.civilight.domain.cast.field.FieldBundles
import io.github.lenticualrstone.civilight.domain.cast.field.FieldTemplates
import io.github.lenticualrstone.civilight.feature.cast.common.editContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.navigation.TemplateEdit
import io.github.lenticualrstone.civilight.navigation.TemplateList
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.security.AuthCopy
import io.github.lenticualrstone.civilight.ui.security.AuthRequest
import io.github.lenticualrstone.civilight.ui.security.AuthResult
import io.github.lenticualrstone.civilight.ui.security.ProtectWindow
import io.github.lenticualrstone.civilight.ui.window.CenterColumn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/** `템플릿 만들기` (CST-58, S33). Feature-local. */
@Serializable
data object NewTemplateSheet : SheetKey

/**
 * `설정 › 인물 템플릿` (CST-58, S32) and, with [TemplateList.trash], `지운 템플릿` (S38). Hidden
 * templates show only while the hidden group is open; while it is locked one `LockRow` without a
 * count stands for them (CST-62).
 */
@Composable
fun TemplateListScreen(route: TemplateList, deps: FeatureDeps) {
    val now = rememberTemplatesNow(deps)
    val writes = remember(deps) { TemplateWrites(deps) }
    val scope = rememberCoroutineScope()
    val sheets = rememberSheetHost()
    fun unlock() {
        scope.launch {
            if (deps.biometric.authenticate(AuthRequest(AuthCopy.HIDDEN_PROJECTS)) == AuthResult.SUCCESS) deps.unlock.unlockHiddenGroup()
        }
    }
    CenterColumn {
        CivilightScreen(
            title = if (route.trash) TemplateCopy.TRASH_TITLE else TemplateCopy.TITLE,
            smallLine = if (route.trash) TemplateCopy.TITLE else TemplateCopy.HEAD,
            back = IconAction(TemplateCopy.BACK) { deps.navigator.pop() },
            sheets = sheets,
            localSheets = { key -> if (key is NewTemplateSheet) ({ NewTemplateContent(now) }) else null },
        ) { padding ->
            val contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp)
            if (route.trash) {
                val rows = remember(now) { TemplateListModel.trashRows(now.all, now.hiddenUnlocked, deps.clock.zone) }
                LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
                    items(rows, key = { it.key }) { row ->
                        ListRow(
                            row.title,
                            civilightItem(),
                            subtitle = row.sub,
                            trailing = {
                                TextAction(TemplateCopy.BTN_RESTORE, {
                                    writes.edit(row.key) { FieldTemplates.restore(it, deps.clock.now()) }
                                })
                            },
                        )
                    }
                }
                return@CivilightScreen
            }
            val view = remember(now) { TemplateListModel.view(now.all, now.hiddenUnlocked) }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
                if (view.empty) {
                    item(key = "empty") {
                        EmptyBlock(
                            TemplateCopy.EMPTY, civilightItem().padding(top = 16.dp), subtitle = TemplateCopy.EMPTY_SUB,
                            primaryLabel = TemplateCopy.MAKE, onPrimary = { sheets.open(NewTemplateSheet) },
                        )
                    }
                } else {
                    item(key = "head") { SectionLabel(view.sectionTitle, civilightItem().padding(top = 8.dp, bottom = 6.dp)) }
                    items(view.rows, key = { it.key }) { row ->
                        if (row.lockMark) ProtectWindow(HiddenTemplateProtection)
                        ListRow(
                            row.title,
                            civilightItem(),
                            subtitle = row.sub,
                            leading = if (row.lockMark) ({ CivilightIcon(IconId.Lock, contentDescription = TemplateCopy.HIDDEN, tint = CivilightTheme.colors.ink3) }) else null,
                            trailing = { CivilightText(TemplateCopy.BTN_EDIT, CivilightTheme.type.small, color = CivilightTheme.colors.ink2, maxLines = 1) },
                            onClick = { deps.navigator.push(TemplateEdit(row.key)) },
                        )
                    }
                    if (view.lockRow) {
                        item(key = "lock") { LockRow(TemplateCopy.HIDDEN, TemplateCopy.BTN_UNLOCK, ::unlock, civilightItem().padding(top = 8.dp)) }
                    }
                    item(key = "add") { PlusRow(TemplateCopy.ADD_ROW, civilightItem()) { sheets.open(NewTemplateSheet) } }
                }
                view.trashRow?.let { label ->
                    item(key = "trash") {
                        ListRow(
                            label, civilightItem().padding(top = 12.dp), compact = true,
                            trailing = { CivilightIcon(IconId.Next, contentDescription = null, tint = CivilightTheme.colors.ink3) },
                            onClick = { deps.navigator.push(TemplateList(trash = true)) },
                        )
                    }
                }
            }
        }
    }
}

/** `+ 템플릿` (plus icon, label 600 14). */
@Composable
private fun PlusRow(label: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .pressable(onClick = onClick, scaleOnPress = false, onClickLabel = TemplateCopy.MAKE)
            .heightIn(min = 44.dp)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CivilightIcon(IconId.Plus, contentDescription = null, tint = colors.ink2)
        CivilightText(label, CivilightTheme.type.label, Modifier.weight(1f), color = colors.ink)
    }
}

/**
 * `템플릿 만들기`: 이름, `시작` (`비어 있음` · `기본` · `게임 프로필` · `템플릿에서` · `프로젝트에서`, the last two
 * expanding to their sources), the `숨긴 템플릿` switch when the source is hidden, then `만들기`, which
 * creates the template and opens `템플릿 고치기`.
 */
@Composable
private fun SheetScope.NewTemplateContent(now: TemplatesNow) {
    val host = this
    val deps = LocalFeatureDeps.current
    val writes = remember(deps) { TemplateWrites(deps) }
    val library by deps.stores.library.state.collectAsState()
    var name by rememberSaveable { mutableStateOf("") }
    var startName by rememberSaveable { mutableStateOf(TemplateStart.EMPTY.name) }
    val start = TemplateStart.valueOf(startName)
    var sourceKey by rememberSaveable { mutableStateOf<Key?>(null) }
    var hiddenChoice by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var project by remember { mutableStateOf<ProjectDoc?>(null) }
    LaunchedEffect(Unit) { deps.stores.library.ensureLoaded() }
    val visible = remember(now) { FieldTemplates.visible(now.all, now.hiddenUnlocked) }
    val projects = remember(library, now.hiddenUnlocked) {
        library.entries.filter { it.deletedAt == null && (!it.hidden || now.hiddenUnlocked) }.sortedBy { it.name }
    }
    LaunchedEffect(start, sourceKey) {
        project = null
        val key = sourceKey?.takeIf { start == TemplateStart.FROM_PROJECT } ?: return@LaunchedEffect
        val store = runCatching { deps.projects.acquire(key) }.getOrNull() ?: return@LaunchedEffect
        try {
            project = store.doc.value
        } finally {
            deps.projects.release(key)
        }
    }
    val sourceHidden = when (start) {
        TemplateStart.FROM_PROJECT -> projects.firstOrNull { it.key == sourceKey }?.hidden == true
        TemplateStart.FROM_TEMPLATE -> visible.firstOrNull { it.key == sourceKey }?.hidden == true
        else -> false
    }
    val hidden = hiddenChoice ?: sourceHidden
    SheetHeader(TemplateCopy.MAKE, onClose = ::close, closeLabel = TemplateCopy.BTN_CLOSE)
    SheetScrollBody(Modifier.padding(top = 14.dp)) {
        CivilightTextField(
            name, { name = it }, Modifier.fillMaxWidth(), label = TemplateCopy.NAME,
            hint = if (FieldTemplates.hasSameName(visible, name)) TemplateCopy.SAME_NAME else null,
        )
        SectionLabel(TemplateCopy.START, Modifier.padding(top = 16.dp, bottom = 4.dp))
        Column {
            for (s in TemplateListModel.STARTS) {
                RadioRow(TemplateListModel.startLabel(s), s == start, {
                    if (s != start) sourceKey = null
                    startName = s.name
                    hiddenChoice = null
                }, subtitle = when (s) {
                    TemplateStart.BASIC -> FieldBundles.fields(io.github.lenticualrstone.civilight.model.project.FieldBundle.BASIC).joinToString(" · ") { it.field.name }
                    else -> null
                })
                if (s == start && s == TemplateStart.FROM_TEMPLATE) {
                    for (t in visible) {
                        if (t.hidden) ProtectWindow(HiddenTemplateProtection)
                        RadioRow(t.name, t.key == sourceKey, { sourceKey = t.key; hiddenChoice = null }, Modifier.padding(start = 16.dp), subtitle = FieldTemplates.kindCounts(t))
                    }
                }
                if (s == start && s == TemplateStart.FROM_PROJECT) {
                    for (p in projects) {
                        if (p.hidden) ProtectWindow(HiddenTemplateProtection)
                        RadioRow(p.name, p.key == sourceKey, { sourceKey = p.key; hiddenChoice = null }, Modifier.padding(start = 16.dp))
                    }
                }
            }
        }
        if (sourceHidden || hiddenChoice != null) {
            SwitchRow(
                TemplateCopy.HIDDEN, hidden, { hiddenChoice = it }, Modifier.padding(top = 10.dp),
                subtitle = if (sourceHidden && !hidden) TemplateCopy.VISIBLE_NOTICE else TemplateCopy.HIDDEN_SUB,
            )
        }
    }
    val ready = TemplateListModel.canMake(name, start, sourceKey) && (start != TemplateStart.FROM_PROJECT || project != null)
    SheetActions {
        CivilightButton(TemplateCopy.BTN_MAKE, {
            val ctx = deps.editContext()
            val made = TemplateListModel.make(
                name, hidden, start, now.all, ctx,
                fromTemplate = visible.firstOrNull { it.key == sourceKey },
                fromProject = project,
            )
            writes.put(made) { deps.navigator.push(TemplateEdit(made.key)) }
            host.close()
        }, Modifier.weight(1f), enabled = ready)
    }
}
