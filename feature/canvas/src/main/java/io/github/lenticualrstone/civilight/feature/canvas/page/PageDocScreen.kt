package io.github.lenticualrstone.civilight.feature.canvas.page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CommandBar
import io.github.lenticualrstone.civilight.designsystem.component.EmptyBlock
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.LockRow
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.designsystem.theme.FoldRevealValue
import io.github.lenticualrstone.civilight.designsystem.theme.LocalBackHandler
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import io.github.lenticualrstone.civilight.designsystem.theme.rememberUnhide
import io.github.lenticualrstone.civilight.designsystem.theme.unhide
import io.github.lenticualrstone.civilight.domain.recorder.put.SourceMarkTarget
import io.github.lenticualrstone.civilight.domain.trash.ItemKind
import io.github.lenticualrstone.civilight.domain.trash.Toast
import io.github.lenticualrstone.civilight.domain.trash.TrashCopy
import io.github.lenticualrstone.civilight.feature.canvas.common.canvasEditContext
import io.github.lenticualrstone.civilight.feature.canvas.image.SlotImage
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.Ratio
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.text.RichDoc
import io.github.lenticualrstone.civilight.model.serialName
import io.github.lenticualrstone.civilight.navigation.ImagePickSheet
import io.github.lenticualrstone.civilight.navigation.PageDoc
import io.github.lenticualrstone.civilight.navigation.RecorderRecordDetail
import io.github.lenticualrstone.civilight.navigation.Trash
import io.github.lenticualrstone.civilight.navigation.TrashFilter
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.picker.HiddenGate
import io.github.lenticualrstone.civilight.ui.picker.LinkPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.PickerModels
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import io.github.lenticualrstone.civilight.ui.richtext.LinkLook
import io.github.lenticualrstone.civilight.ui.richtext.LinkPick
import io.github.lenticualrstone.civilight.ui.richtext.RichDocEdit
import io.github.lenticualrstone.civilight.ui.richtext.RichTextEditor
import io.github.lenticualrstone.civilight.ui.richtext.rememberRichTextEditorState
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.SheetHost
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.CenterColumn
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

private val NameStyle = TextStyle(fontSize = 30.sp, lineHeight = 38.sp)

/** 페이지 (CNV-13, CNV-15, CNV-16; board `Page`). */
@Composable
fun PageDocScreen(route: PageDoc, deps: FeatureDeps) {
    val vm = entryViewModel { PageDocViewModel(route.projectKey, deps.projects, deps::canvasEditContext) }
    val load by vm.state.collectAsState()
    val recorder by deps.stores.recorder.state.collectAsState()
    LaunchedEffect(Unit) { deps.stores.recorder.ensureLoaded() }
    val sheets = rememberSheetHost()
    val gate = rememberHiddenGate(route.projectKey)
    val ready = load as? ProjectLoad.Ready
    val zone = deps.clock.zone
    val view = remember(ready, gate.open, recorder.library) {
        ready?.let { PageViews.build(it.doc, route.pageKey, gate.open, it.readOnly, zone, recorder.library) }
    }
    var editing by rememberSaveable(route.pageKey) { mutableStateOf(false) }
    /** Editing began by itself (a blank page just made); then back leaves the page instead of ending editing. */
    var autoStarted by rememberSaveable(route.pageKey) { mutableStateOf(false) }
    /** Bumped when `/` rewrites the body, so the editor starts again from the new blocks. */
    var bodyVersion by rememberSaveable(route.pageKey) { mutableIntStateOf(0) }
    // Every editing session (and every `/` rewrite) gets its own editor key: the editor's saveable draft
    // would otherwise come back on the next session and overwrite changes made in read mode since
    // (`글로 풀기`, `다른 항목에 잇기`, undo).
    val beginEditing = { auto: Boolean ->
        if (!editing) {
            bodyVersion++
            editing = true
            autoStarted = auto
        }
    }
    val readOnlyToast = { deps.toasts.show(PageCopy.READ_ONLY, actionLabel = null) }

    // A page just made opens straight into editing: a blank page, or one made with `만들고 열기` (U-15) whose
    // name is already set. The mark is read once the page has loaded, so it is not lost while loading.
    LaunchedEffect(view?.key, view?.isBlank) {
        if (view != null && PageAutoEdit.shouldStart(view, JustCreatedPages.take(view.key))) beginEditing(true)
    }
    val stopEditing = { editing = false }
    // A page that opened straight into editing leaves on the first back (nothing visible changes when such a
    // session ends, so that back read as ignored); editing the user started ends first, back to links.
    LocalBackHandler.current(PageBackRules.onBack(editing, autoStarted) == PageBack.STOP_EDITING, stopEditing)

    val startEditing: () -> Unit = {
        when {
            view == null || view.access != PageAccess.OPEN -> Unit
            view.readOnly -> readOnlyToast()
            // The editor shows stored link text: links to hidden items wait for `지문으로 보기`.
            !PageAccessRules.canEdit(view) -> gate.unlock()
            else -> beginEditing(false)
        }
    }
    val caret = remember { CaretHolder() }

    CivilightScreen(
        title = "",
        smallLine = PageCopy.SMALL_LINE,
        back = IconAction(PageCopy.BACK) {
            when (PageBackRules.onBack(editing, autoStarted)) {
                PageBack.STOP_EDITING -> stopEditing()
                PageBack.LEAVE -> deps.navigator.pop()
            }
        },
        actions = {
            FoldReveal(view != null && view.access == PageAccess.OPEN, horizontal = true) {
                CivilightIconButton(IconId.More, PageCopy.MENU, { sheets.open(PageMenuSheet(route.projectKey, route.pageKey)) })
            }
        },
        commandBar = {
            CommandBar(
                hint = PageCopy.HINT,
                onClick = {
                    // `/로 블록 추가`: the command bar starts editing and offers the block kinds.
                    if (view != null && PageAccessRules.canEdit(view)) {
                        beginEditing(false)
                        sheets.open(BlockAddSheet(route.projectKey, route.pageKey))
                    } else {
                        startEditing()
                    }
                },
                micLabel = PageCopy.MIC,
            )
        },
        sheets = sheets,
        localSheets = { key -> ready?.let { pageSheet(key, it.doc, view, vm, deps, gate, sheets, caret, readOnlyToast, startEditing) { bodyVersion++ } } },
    ) { padding ->
        CenterColumn {
            // Loading and the page crossfade (audit §4 canvas 8, P3).
            ContentSwap(ready != null && view != null, Modifier.fillMaxSize(), label = "pageDocBody") { loaded ->
                if (loaded && ready != null && view != null) {
                    PageContent(route, deps, vm, ready.doc, view, gate, padding, editing, bodyVersion, startEditing, sheets, caret)
                }
            }
        }
    }
}

@Composable
private fun PageContent(
    route: PageDoc,
    deps: FeatureDeps,
    vm: PageDocViewModel,
    doc: ProjectDoc,
    view: PageView,
    gate: HiddenGate,
    padding: PaddingValues,
    editing: Boolean,
    bodyVersion: Int,
    startEditing: () -> Unit,
    sheets: SheetHost,
    caret: CaretHolder,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    // A hidden page that was just unlocked comes in through the unhide (CNV-43, P5); an open page starts clear.
    val unhide = rememberUnhide(view.access == PageAccess.OPEN)
    val onLink: (PageLink) -> Unit = { link ->
        when (link.state) {
            LinkState.LIVE -> link.route?.let(deps.navigator::push)
            LinkState.HIDDEN -> gate.unlock()
            LinkState.TRASHED, LinkState.GONE -> sheets.open(DeletedLinkSheet(route.projectKey, route.pageKey, link.linkKey))
            LinkState.OTHER -> Unit
        }
    }
    val content = PaddingValues(
        start = 24.dp,
        end = 24.dp,
        top = padding.calculateTopPadding() + 30.dp,
        bottom = padding.calculateBottomPadding() + 24.dp,
    )
    LazyColumn(Modifier.fillMaxSize(), contentPadding = content) {
        view.headerImage?.let { use ->
            item(key = "header") {
                Box(civilightItem().unhide(unhide).fillMaxWidth().aspectRatio(21f / 9f).clip(RoundedCornerShape(14.dp))) {
                    SlotImage(use, Ratio.R21X9, view.shownName, onPick = null, contentDescription = PageCopy.BTN_HEADER_IMAGE)
                }
            }
        }
        when (view.access) {
            PageAccess.HIDDEN_LOCKED -> item(key = "lock") {
                LockRow(title = PageCopy.HIDDEN_ITEM, actionLabel = PageCopy.BTN_UNLOCK, onAction = gate.unlock, modifier = civilightItem())
            }
            PageAccess.GONE -> item(key = "gone") {
                val name = doc.pages[route.pageKey]?.name?.ifBlank { PageCopy.UNNAMED } ?: PageCopy.UNNAMED
                EmptyBlock(title = TrashCopy.deletedLabel(ItemKind.PAGE, name), modifier = civilightItem())
            }
            PageAccess.OPEN -> {
                item(key = "head") {
                    Column(civilightItem().unhide(unhide).padding(top = if (view.headerImage != null) 18.dp else 0.dp)) {
                        CivilightText(view.sectionLine, type.sec, color = colors.ink3)
                        // Name: shown ↔ field crossfade in place; the typed line itself never moves.
                        ContentSwap(editing, Modifier.fillMaxWidth(), label = "pageName") { e ->
                            if (e) NameField(route.pageKey, view.name, vm, bodyVersion)
                            else CivilightText(view.shownName, type.pageTitle.merge(NameStyle), Modifier.padding(top = 8.dp), color = colors.ink)
                        }
                        CivilightText(view.meta, type.small, Modifier.padding(top = 6.dp), color = colors.ink3)
                        FoldRevealValue(view.sourceMark) { mark -> SourceMarkLine(mark, deps) }
                    }
                }
                if (editing) {
                    item(key = "editor-$bodyVersion") { Box(civilightItem()) { PageEditor(route, doc, vm, gate, sheets, bodyVersion, caret) } }
                } else {
                    itemsIndexed(view.paragraphs, key = { _, p -> p.key }) { i, p ->
                        PageBlockText(p, first = i == 0, onLink = onLink, onText = startEditing, modifier = civilightItem().unhide(unhide))
                    }
                }
            }
        }
    }
}

/** The page's own source mark (CNV-15): the record screen while the record lives, the recorder trash while it is there. */
@Composable
private fun SourceMarkLine(mark: SourceMarkUi, deps: FeatureDeps) {
    val colors = CivilightTheme.colors
    when (mark.target) {
        SourceMarkTarget.RECORD_SCREEN -> TextAction(mark.text, { deps.navigator.push(RecorderRecordDetail(mark.recordKey)) }, color = colors.ink3, style = CivilightTheme.type.small)
        SourceMarkTarget.TRASH_RECORDER_FILTER -> TextAction(mark.text, { deps.navigator.push(Trash(TrashFilter.RECORDER)) }, color = colors.ink3, style = CivilightTheme.type.small)
        SourceMarkTarget.NONE -> CivilightText(mark.text, CivilightTheme.type.small, Modifier.padding(top = 4.dp), color = colors.ink3)
    }
}

/** The page name while editing: the same 30 / 38 type on `bg`, written through as it is typed. */
@Composable
private fun NameField(pageKey: Key, initial: String, vm: PageDocViewModel, session: Int) {
    val colors = CivilightTheme.colors
    val style = CivilightTheme.type.pageTitle.merge(NameStyle).merge(TextStyle(color = colors.ink))
    var name by rememberSaveable(pageKey, session) { mutableStateOf(initial) }
    BasicTextField(
        value = name,
        onValueChange = {
            name = it
            vm.rename(pageKey, it)
        },
        textStyle = style,
        cursorBrush = SolidColor(colors.acc),
        modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
        decorationBox = { inner ->
            Box {
                if (name.isEmpty()) CivilightText(PageCopy.UNNAMED, style, color = colors.ink3, maxLines = 1)
                inner()
            }
        },
    )
}

/**
 * The body while editing: the shared rich text editor (`@` links, `( )`, `⌁` memo paragraphs; link keys,
 * paragraph kinds and recorder sources survive edits). Every change is written through as one merged
 * undo step; hidden and deleted links keep their look.
 */
@Composable
private fun PageEditor(route: PageDoc, doc: ProjectDoc, vm: PageDocViewModel, gate: HiddenGate, sheets: SheetHost, bodyVersion: Int, caret: CaretHolder) {
    val page = doc.pages[route.pageKey] ?: return
    // After `/` rewrote the body the editor starts from the rewritten blocks (the store write may land a frame later).
    // Taken once: a later editor (stop editing, edit again) starts from the stored blocks, not a stale rewrite.
    val seed = remember(bodyVersion) { caret.rewritten.also { caret.rewritten = null } ?: page.blocks }
    val state = rememberRichTextEditorState(seed, key = "${route.pageKey}-$bodyVersion")
    // The last change is written when the editor leaves too (back in the same frame as the last key).
    val written = remember(state) { arrayOf(state.result) }
    LaunchedEffect(state) {
        snapshotFlow { state.result }.drop(1).collect {
            written[0] = it
            vm.setBlocks(route.pageKey, it)
        }
    }
    DisposableEffect(state) {
        onDispose {
            val last = state.result
            if (last != written[0]) vm.setBlocks(route.pageKey, last)
        }
    }
    caret.blockKey = { RichDocEdit.paragraphAt(state.doc, state.field.selection.min)?.key }
    caret.blocks = { state.doc }
    RichTextEditor(
        state = state,
        projectKey = route.projectKey,
        linkPickerTitle = PageCopy.LINK_PICKER_TITLE,
        openSheet = sheets::open,
        modifier = Modifier.padding(top = 18.dp),
        minHeight = 240.dp,
        linkLook = { link ->
            when (PageLinks.resolve(doc, link, gate.open).state) {
                LinkState.HIDDEN -> LinkLook.HIDDEN
                LinkState.TRASHED, LinkState.GONE -> LinkLook.DELETED
                else -> LinkLook.LIVE
            }
        },
    )
}

/** The open editor as the `/` sheet sees it: its current blocks and the block the caret is in. */
private class CaretHolder {
    var blockKey: () -> Key? = { null }
    var blocks: () -> RichDoc? = { null }

    /** Blocks `/` just wrote, for the editor that starts next. */
    var rewritten: RichDoc? = null
}

/** Local sheets of the page screen. */
private fun pageSheet(
    key: io.github.lenticualrstone.civilight.navigation.SheetKey,
    doc: ProjectDoc,
    view: PageView?,
    vm: PageDocViewModel,
    deps: FeatureDeps,
    gate: HiddenGate,
    sheets: SheetHost,
    caret: CaretHolder,
    readOnlyToast: () -> Unit,
    startEditing: () -> Unit,
    onBodyRewritten: () -> Unit,
): (@Composable io.github.lenticualrstone.civilight.ui.feature.SheetScope.() -> Unit)? {
    val done = { r: EditResult -> if (r == EditResult.READ_ONLY) readOnlyToast() }
    val showToast = { t: Toast? ->
        t?.let { toast -> deps.toasts.show(toast.message, actionLabel = toast.action) { vm.undoLast() } }
    }
    return when (key) {
        is PageMenuSheet -> {
            {
                val page = doc.pages[key.pageKey]
                val actions = buildList {
                    add(SheetAction(PageCopy.BTN_EDIT, { close(); startEditing() }))
                    add(SheetAction(PageCopy.BTN_HEADER_IMAGE, { close(); sheets.open(ImagePickSheet(key.projectKey, ImageOwnerKind.PAGE, key.pageKey, ImageSlot.PAGE_HEADER)) }))
                    if (page?.hidden == true) {
                        add(SheetAction(PageCopy.BTN_UNHIDE, { close(); vm.setHidden(key.pageKey, false, done) }))
                    } else {
                        add(SheetAction(PageCopy.BTN_HIDE, {
                            close()
                            vm.setHidden(key.pageKey, true, done)
                        }))
                    }
                    add(SheetAction(PageCopy.BTN_DELETE, destructive = true, onClick = {
                        close()
                        vm.delete(key.pageKey) { r, toast ->
                            done(r)
                            if (r == EditResult.APPLIED && toast != null) {
                                deps.toasts.show(toast.message, actionLabel = toast.action) {
                                    deps.appScope.launch { deps.projects.peek(key.projectKey)?.undo() }
                                }
                                deps.navigator.pop()
                            }
                        }
                    }))
                }
                ActionRows(view?.shownName ?: PageCopy.SMALL_LINE, view?.sectionLine, actions, ::close)
            }
        }
        is BlockAddSheet -> {
            {
                ActionRows(
                    PageCopy.BLOCK_ADD_TITLE,
                    null,
                    PageBlockKinds.map { (kind, label) ->
                        SheetAction(label, {
                            close()
                            val current = caret.blocks() ?: doc.pages[key.pageKey]?.blocks
                            if (current != null) {
                                val next = PageEdits.slash(current, kind, caret.blockKey(), Keys::newKey)
                                vm.setBlocks(key.pageKey, RichDocEdit.trimmed(next).takeIf { it.size >= next.size } ?: next)
                                caret.rewritten = next
                                onBodyRewritten()
                            }
                        })
                    },
                    ::close,
                )
            }
        }
        is DeletedLinkSheet -> {
            {
                val link = view?.links?.firstOrNull { it.linkKey == key.linkKey }
                if (link == null || !link.dotted) {
                    ActionRows("", null, emptyList(), ::close)
                } else {
                    val actions = link.actions.map { label ->
                        SheetAction(label, {
                            when (label) {
                                PageCopy.BTN_RELINK -> open(RelinkSheet(key.projectKey, key.pageKey, key.linkKey))
                                PageCopy.BTN_UNLINK -> { close(); vm.unlink(key.linkKey, done) }
                                PageCopy.BTN_KEEP -> { close(); link.item?.let { vm.keep(it, done) } }
                                PageCopy.BTN_RESTORE -> link.item?.let { item ->
                                    vm.restore(item, gate.open) { r, result ->
                                        done(r)
                                        if (result.needsAuth) gate.unlock() else { close(); showToast(result.toast) }
                                    }
                                }
                            }
                        })
                    }
                    ActionRows(link.cardTitle.orEmpty(), null, actions, ::close)
                }
            }
        }
        is RelinkSheet -> {
            {
                LinkPickerSheet(
                    doc = doc,
                    gate = gate,
                    title = PageCopy.LINK_PICKER_TITLE,
                    onPick = { kind, target, text ->
                        val targetKind = LinkPick(kind, target, text).targetKind
                        if (targetKind != null) vm.relink(key.pageKey, key.linkKey, targetKind, target, text, done)
                        close()
                        close()
                    },
                    onClose = ::close,
                    kinds = PickerModels.DOCUMENT_LINK_KINDS,
                )
            }
        }
        else -> null
    }
}
