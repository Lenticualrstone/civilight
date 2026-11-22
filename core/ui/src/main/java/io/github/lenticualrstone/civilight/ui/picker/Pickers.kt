package io.github.lenticualrstone.civilight.ui.picker

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.labels.UnitLabels
import io.github.lenticualrstone.civilight.ui.security.AuthCopy
import io.github.lenticualrstone.civilight.ui.security.AuthRequest
import io.github.lenticualrstone.civilight.ui.security.AuthResult
import kotlinx.coroutines.launch

/** Whether a project's hidden items are open, and how to open them (LIB-36: one unlock per project). */
data class HiddenGate(val open: Boolean, val unlock: () -> Unit)

/** [HiddenGate] for [projectKey] from the shell (biometric prompt → `UnlockController.unlockItems`). */
@Composable
fun rememberHiddenGate(projectKey: Key): HiddenGate {
    val deps = LocalFeatureDeps.current
    val state by deps.unlock.state.collectAsState()
    val scope = rememberCoroutineScope()
    return HiddenGate(open = state.itemsOpen(projectKey)) {
        scope.launch {
            // LIB-36: `지문으로 보기` on a hidden row uses the same prompt as LIB-32.
            if (deps.biometric.authenticate(AuthRequest(AuthCopy.HIDDEN_PROJECTS)) == AuthResult.SUCCESS) {
                deps.unlock.unlockItems(projectKey)
            }
        }
    }
}

/**
 * 화자 (SCR-19, CST-37): search, tag chips `전체 · <tags>`, sections `이 씬 등장 · 최근 · 전체`, the
 * deleted current speaker on top, locked hidden characters counted at the end. Picking calls
 * [onPick] with the character key; the caller closes the sheet.
 */
@Composable
fun ColumnScope.CharacterPickerSheet(
    doc: ProjectDoc,
    gate: HiddenGate,
    onPick: (Key) -> Unit,
    onClose: () -> Unit,
    title: String = PickerCopy.SPEAKER_TITLE,
    sceneCast: List<Key> = emptyList(),
    recent: List<Key> = emptyList(),
    currentKey: Key? = null,
    onRestoreDeleted: ((Key) -> Unit)? = null,
    subtitleOf: ((Character) -> String?)? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var tag by rememberSaveable { mutableStateOf<String?>(null) }
    val model = remember(doc, query, tag, sceneCast, recent, gate.open, currentKey) {
        PickerModels.characters(
            doc, query, tag, sceneCast, recent, gate.open, currentKey,
            subtitleOf = subtitleOf ?: { PickerModels.defaultSubtitle(doc, it) },
        )
    }
    val chips = remember(doc) {
        listOf(PickerChip(null, PickerCopy.ALL)) + doc.project.tags.sortedBy { it.order }.map { PickerChip(it.key, it.name) }
    }
    ItemPickerSheet(
        title = title,
        model = model,
        query = query,
        onQueryChange = { query = it },
        onPick = { onPick(it.key) },
        onClose = onClose,
        selectedKey = currentKey,
        chips = chips,
        selectedChip = tag,
        onChip = { tag = it },
        onUnlockHidden = gate.unlock,
        onRestoreDeleted = onRestoreDeleted?.let { restore -> { row -> restore(row.key) } },
    )
}

/** 용어 고르기: [onPick] gets the term key and the text to insert (the alias when an alias matched). */
@Composable
fun ColumnScope.TermPickerSheet(
    doc: ProjectDoc,
    gate: HiddenGate,
    title: String,
    onPick: (key: Key, text: String) -> Unit,
    onClose: () -> Unit,
    selectedKey: Key? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val model = remember(doc, query, gate.open) { PickerModels.terms(doc, query, gate.open) }
    ItemPickerSheet(title, model, query, { query = it }, { onPick(it.key, it.insertText) }, onClose, selectedKey = selectedKey, onUnlockHidden = gate.unlock)
}

/** 장소 고르기 (WLD-52): places, and map regions when [includeRegions]; [onPick] gets the scene's `PlaceRef`. */
@Composable
fun ColumnScope.PlacePickerSheet(
    doc: ProjectDoc,
    gate: HiddenGate,
    title: String,
    onPick: (PlaceRef) -> Unit,
    onClose: () -> Unit,
    selected: PlaceRef? = null,
    includeRegions: Boolean = true,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val model = remember(doc, query, gate.open, includeRegions) { PickerModels.places(doc, query, gate.open, includeRegions) }
    ItemPickerSheet(
        title, model, query, { query = it },
        onPick = { item -> PickerModels.placeRefKind(item)?.let { onPick(PlaceRef(it, item.key)) } },
        onClose = onClose,
        selectedKey = selected?.key,
        onUnlockHidden = gate.unlock,
    )
}

/** 세력 고르기. */
@Composable
fun ColumnScope.FactionPickerSheet(
    doc: ProjectDoc,
    gate: HiddenGate,
    title: String,
    onPick: (Key) -> Unit,
    onClose: () -> Unit,
    selectedKey: Key? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val model = remember(doc, query, gate.open) { PickerModels.factions(doc, query, gate.open) }
    ItemPickerSheet(title, model, query, { query = it }, { onPick(it.key) }, onClose, selectedKey = selectedKey, onUnlockHidden = gate.unlock)
}

/** 단위 고르기: chapters, episodes and scenes as an indented tree; [levels] limits the rows. */
@Composable
fun ColumnScope.UnitPickerSheet(
    doc: ProjectDoc,
    gate: HiddenGate,
    title: String,
    onPick: (Key) -> Unit,
    onClose: () -> Unit,
    selectedKey: Key? = null,
    levels: Set<Int>? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val labels = remember(doc) { UnitLabels(doc) }
    val model = remember(doc, query, gate.open, levels) { PickerModels.units(doc, query, levels, gate.open, labels) }
    ItemPickerSheet(title, model, query, { query = it }, { onPick(it.key) }, onClose, selectedKey = selectedKey, onUnlockHidden = gate.unlock)
}

/** 사건 고르기: live events in timeline order; [onPick] gets the event key. */
@Composable
fun ColumnScope.EventPickerSheet(
    doc: ProjectDoc,
    title: String,
    onPick: (Key) -> Unit,
    onClose: () -> Unit,
    selectedKey: Key? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val labels = remember(doc) { UnitLabels(doc) }
    val model = remember(doc, query) { PickerModels.events(doc, query, labels) }
    ItemPickerSheet(title, model, query, { query = it }, { onPick(it.key) }, onClose, selectedKey = selectedKey)
}

/**
 * `@` 연결 (SCR-55): people, terms and places ([kinds] adds factions for document text); [onPick] gets
 * the kind, key and the text to insert.
 */
@Composable
fun ColumnScope.LinkPickerSheet(
    doc: ProjectDoc,
    gate: HiddenGate,
    title: String,
    onPick: (kind: PickKind, key: Key, text: String) -> Unit,
    onClose: () -> Unit,
    initialQuery: String = "",
    kinds: Set<PickKind> = PickerModels.SCRIPT_LINK_KINDS,
) {
    var query by rememberSaveable { mutableStateOf(initialQuery) }
    val model = remember(doc, query, gate.open, kinds) { PickerModels.links(doc, query, gate.open, kinds) }
    ItemPickerSheet(title, model, query, { query = it }, { onPick(it.kind, it.key, it.insertText) }, onClose, onUnlockHidden = gate.unlock)
}
