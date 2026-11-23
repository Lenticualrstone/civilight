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
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.designsystem.component.CreateKind
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
    creator: PickerCreator? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var tag by rememberSaveable { mutableStateOf<String?>(null) }
    // N-44: trashed tags make no chip; a chip whose tag was trashed meanwhile falls back to `전체`.
    val liveTags = remember(doc) { PickerCreateCards.liveTags(doc) }
    val liveTag = tag?.takeIf { t -> liveTags.any { it.key == t } }
    val model = remember(doc, query, liveTag, sceneCast, recent, gate.open, currentKey) {
        PickerModels.characters(
            doc, query, liveTag, sceneCast, recent, gate.open, currentKey,
            subtitleOf = subtitleOf ?: { PickerModels.defaultSubtitle(doc, it) },
        )
    }
    val chips = remember(liveTags) { listOf(PickerChip(null, PickerCopy.ALL)) + liveTags.map { PickerChip(it.key, it.name) } }
    PickerWithCreate(doc, gate, creator?.takeIf { CreateKind.CHARACTER in it.kinds }, onCreated = { _, key -> onPick(key) }) { onCreate ->
        ItemPickerSheet(
            title = title,
            model = model,
            query = query,
            onQueryChange = { query = it },
            onPick = { onPick(it.key) },
            onClose = onClose,
            selectedKey = currentKey,
            chips = chips,
            selectedChip = liveTag,
            onChip = { tag = it },
            onUnlockHidden = gate.unlock,
            onRestoreDeleted = onRestoreDeleted?.let { restore -> { row -> restore(row.key) } },
            createKind = onCreate?.let { CreateKind.CHARACTER },
            onCreate = onCreate?.let { create -> { q -> create(CreateKind.CHARACTER, q, PickerCreate.matched(q, model)) } },
        )
    }
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
    creator: PickerCreator? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val model = remember(doc, query, gate.open) { PickerModels.terms(doc, query, gate.open) }
    PickerWithCreate(doc, gate, creator?.takeIf { CreateKind.TERM in it.kinds }, onCreated = { v, key -> onPick(key, v.name) }) { onCreate ->
        ItemPickerSheet(
            title, model, query, { query = it }, { onPick(it.key, it.insertText) }, onClose, selectedKey = selectedKey, onUnlockHidden = gate.unlock,
            createKind = onCreate?.let { CreateKind.TERM },
            onCreate = onCreate?.let { create -> { q -> create(CreateKind.TERM, q, PickerCreate.matched(q, model)) } },
        )
    }
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
    creator: PickerCreator? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val model = remember(doc, query, gate.open, includeRegions) { PickerModels.places(doc, query, gate.open, includeRegions) }
    // U-15.3, WLD-70: a new place is returned as the pick (a pin is placed later with the map `핀` tool).
    PickerWithCreate(doc, gate, creator?.takeIf { CreateKind.PLACE in it.kinds }, onCreated = { _, key -> onPick(PlaceRef(PlaceRefKind.PLACE, key)) }) { onCreate ->
        ItemPickerSheet(
            title, model, query, { query = it },
            onPick = { item -> PickerModels.placeRefKind(item)?.let { onPick(PlaceRef(it, item.key)) } },
            onClose = onClose,
            selectedKey = selected?.key,
            onUnlockHidden = gate.unlock,
            createKind = onCreate?.let { CreateKind.PLACE },
            onCreate = onCreate?.let { create -> { q -> create(CreateKind.PLACE, q, PickerCreate.matched(q, model)) } },
        )
    }
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
    creator: PickerCreator? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val model = remember(doc, query, gate.open) { PickerModels.factions(doc, query, gate.open) }
    PickerWithCreate(doc, gate, creator?.takeIf { CreateKind.FACTION in it.kinds }, onCreated = { _, key -> onPick(key) }) { onCreate ->
        ItemPickerSheet(
            title, model, query, { query = it }, { onPick(it.key) }, onClose, selectedKey = selectedKey, onUnlockHidden = gate.unlock,
            createKind = onCreate?.let { CreateKind.FACTION },
            onCreate = onCreate?.let { create -> { q -> create(CreateKind.FACTION, q, PickerCreate.matched(q, model)) } },
        )
    }
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
 *
 * (0.2.0, U-15.3, SCR-55) With a [creator], kind chips `전체 · 인물 · 용어 · 장소` sit under the search and the
 * create row is for the kind in view: the picked chip's kind, `인물` under `전체` (decision: the picker has no
 * other notion of a kind in view, and the row never guesses from the query). The new item is inserted as
 * the link at once, with its name as the text.
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
    creator: PickerCreator? = null,
) {
    var query by rememberSaveable { mutableStateOf(initialQuery) }
    var chip by rememberSaveable { mutableStateOf<String?>(null) }
    val creatable = creator?.takeIf { c -> kinds.any { k -> LinkCreate.createKind(k)?.let { it in c.kinds } == true } }
    val shownKinds = chip?.let { name -> kinds.filter { it.name == name }.toSet().ifEmpty { null } } ?: kinds
    val model = remember(doc, query, gate.open, shownKinds) { PickerModels.links(doc, query, gate.open, shownKinds) }
    val chips = remember(kinds, creatable != null) { if (creatable == null) emptyList() else LinkCreate.chips(kinds) }
    val createKind = LinkCreate.kindInView(chip, kinds)?.takeIf { k -> creatable?.kinds?.contains(k) == true }
    PickerWithCreate(doc, gate, creatable, onCreated = { v, key -> LinkCreate.pickKind(v.kind)?.let { onPick(it, key, v.name) } }) { onCreate ->
        ItemPickerSheet(
            title, model, query, { query = it }, { onPick(it.kind, it.key, it.insertText) }, onClose, onUnlockHidden = gate.unlock,
            chips = chips,
            selectedChip = chip,
            onChip = { chip = it },
            createKind = createKind.takeIf { onCreate != null },
            onCreate = if (onCreate != null && createKind != null) ({ q -> onCreate(createKind, q, PickerCreate.matched(q, model)) }) else null,
        )
    }
}

/** (0.2.0, U-15.3) The `@` picker's kind chips and the kind its create row makes (JVM tested). */
object LinkCreate {
    private val ORDER = listOf(PickKind.CHARACTER, PickKind.TERM, PickKind.PLACE, PickKind.FACTION)

    fun createKind(kind: PickKind): CreateKind? = when (kind) {
        PickKind.CHARACTER -> CreateKind.CHARACTER
        PickKind.TERM -> CreateKind.TERM
        PickKind.PLACE -> CreateKind.PLACE
        PickKind.FACTION -> CreateKind.FACTION
        else -> null
    }

    fun pickKind(kind: CreateKind): PickKind? = when (kind) {
        CreateKind.CHARACTER -> PickKind.CHARACTER
        CreateKind.TERM -> PickKind.TERM
        CreateKind.PLACE -> PickKind.PLACE
        CreateKind.FACTION -> PickKind.FACTION
        else -> null
    }

    private fun word(kind: PickKind): String = when (kind) {
        PickKind.CHARACTER -> PickerCopy.KIND_CHARACTER
        PickKind.TERM -> PickerCopy.KIND_TERM
        PickKind.PLACE -> PickerCopy.KIND_PLACE
        PickKind.FACTION -> PickerCopy.KIND_FACTION
        else -> PickerCopy.KIND_ITEM
    }

    /** `전체` then one chip per offered kind, in the order 인물 · 용어 · 장소 · 세력. */
    fun chips(kinds: Set<PickKind>): List<PickerChip> =
        listOf(PickerChip(null, PickerCopy.ALL)) + ORDER.filter { it in kinds }.map { PickerChip(it.name, word(it)) }

    /** The kind in view: the picked chip's, else the first offered kind (인물 for the script). */
    fun kindInView(chip: String?, kinds: Set<PickKind>): CreateKind? {
        val picked = chip?.let { name -> kinds.firstOrNull { it.name == name } } ?: ORDER.firstOrNull { it in kinds }
        return picked?.let(::createKind)
    }
}
