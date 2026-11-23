package io.github.lenticualrstone.civilight.ui.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.CreateCard
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardMode
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardOverlay
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardState
import io.github.lenticualrstone.civilight.designsystem.component.CreateField
import io.github.lenticualrstone.civilight.designsystem.component.CreateFieldType
import io.github.lenticualrstone.civilight.designsystem.component.CreateFields
import io.github.lenticualrstone.civilight.designsystem.component.CreateKind
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.Tag
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.store.ReadOnlyProjectException
import io.github.lenticualrstone.civilight.ui.project.LocalProjectStore
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * (0.2.0, U-15.3, U-15.6) What a picker's create card holds, read out of the DS card state: [kind] and the
 * typed or picked values. Only [name] is required. [tagKeys] are live tags checked on a character card;
 * [parentKey] is a place card's `상위 지역`; [alias] a term's `다른 이름`; [note] the `한 줄 소개` / `한 줄 설명`.
 */
data class PickerCreateValues(
    val kind: CreateKind,
    val name: String,
    val displayName: String? = null,
    val note: String? = null,
    val alias: String? = null,
    val tagKeys: List<Key> = emptyList(),
    val parentKey: Key? = null,
)

/**
 * (0.2.0, U-15.3) The create row of a picker sheet: [kinds] it can make (the `@` picker follows its kind chip,
 * every other picker has one kind) and the one write [write] (one edit, one undo step `만들기`) returning the
 * new document and the new item's key, or null when nothing was made. Callers pass null for a read-only
 * project (U-15.10) and for pickers that must pick an existing item (merge targets).
 */
class PickerCreator(
    val kinds: Set<CreateKind>,
    val write: (doc: ProjectDoc, values: PickerCreateValues) -> Pair<ProjectDoc, Key>?,
) {
    constructor(kind: CreateKind, write: (doc: ProjectDoc, values: PickerCreateValues) -> Pair<ProjectDoc, Key>?) : this(setOf(kind), write)
}

/** Pure rules of the picker create card (JVM tested). */
object PickerCreateCards {
    /** Multi-pick values are stored in the card as keys joined with this separator. */
    private const val SEP = ","

    /**
     * The fields the card draws for [kind]: text fields, `태그` (character) and `상위 지역` (place). `이미지` is
     * left out: an image pick needs an owner that exists before `만들기` (hidden, not drawn).
     */
    fun fields(kind: CreateKind): List<CreateField> = kind.fields.filter { f ->
        f.type == CreateFieldType.TEXT || f.key == CreateFields.tags.key || f.key == CreateFields.parentRegion.key
    }

    /**
     * The card a create row opens (U-15.3): the name holds the trimmed query only when it matched nothing (the
     * row read `‘세라’ 새 인물`); with a matching or blank query the card opens empty.
     */
    fun open(kind: CreateKind, query: String, matched: Boolean): CreateCardState =
        CreateCardState.open(kind, CreateCardMode.PICKER, query.trim().takeIf { !matched && it.isNotEmpty() })

    fun tagKeys(state: CreateCardState): List<Key> = state.values[CreateFields.tags.key].orEmpty().split(SEP).filter { it.isNotBlank() }

    fun toggleTag(state: CreateCardState, tagKey: Key): CreateCardState {
        val now = tagKeys(state)
        return state.with(CreateFields.tags.key, (if (tagKey in now) now - tagKey else now + tagKey).joinToString(SEP))
    }

    fun parentKey(state: CreateCardState): Key? = state.values[CreateFields.parentRegion.key]?.takeIf { it.isNotBlank() }

    fun withParent(state: CreateCardState, key: Key?): CreateCardState = state.with(CreateFields.parentRegion.key, key.orEmpty())

    /** The values handed to [PickerCreator.write]; text is trimmed, blanks are null. */
    fun values(state: CreateCardState): PickerCreateValues {
        fun text(field: CreateField) = state.values[field.key]?.trim()?.takeIf { it.isNotEmpty() }
        return PickerCreateValues(
            kind = state.kind,
            name = state.name.trim(),
            displayName = text(CreateFields.displayName),
            // `한 줄 소개` and `한 줄 설명` share the key `oneLine`.
            note = text(CreateFields.oneLineIntro),
            alias = text(CreateFields.aliases),
            tagKeys = tagKeys(state),
            parentKey = parentKey(state),
        )
    }

    /** `만들기` is on while the name is not blank. */
    fun canCreate(state: CreateCardState): Boolean = CreateCard.canCreate(state.name)

    /** Live project tags in tag order (trashed tags are never offered, N-44). */
    fun liveTags(doc: ProjectDoc): List<Tag> = doc.project.tags.filter { it.deletedAt == null }.sortedBy { it.order }
}

/** Saves an open picker card with its sheet (`rememberSaveable` of a string). */
object PickerCreateCodec {
    @Serializable
    private data class Saved(val kind: String, val values: Map<String, String>)

    private val json = Json { ignoreUnknownKeys = true }

    fun encode(state: CreateCardState?): String =
        state?.let { json.encodeToString(Saved.serializer(), Saved(it.kind.name, it.values)) }.orEmpty()

    fun decode(text: String): CreateCardState? = if (text.isBlank()) {
        null
    } else {
        runCatching {
            val saved = json.decodeFromString(Saved.serializer(), text)
            CreateCardState(CreateKind.valueOf(saved.kind), CreateCardMode.PICKER, saved.values)
        }.getOrNull()
    }

    val saver: Saver<MutableState<CreateCardState?>, String> = Saver(
        save = { encode(it.value) },
        restore = { mutableStateOf(decode(it)) },
    )
}

/** The open card of a picker (null = closed), kept with the sheet. */
@Composable
internal fun rememberPickerCard(): MutableState<CreateCardState?> =
    rememberSaveable(saver = PickerCreateCodec.saver) { mutableStateOf(null) }

/**
 * Draws a picker sheet and its create card. While the card's `상위 지역` is being picked, a plain place list
 * (no create row, so no nested card) takes the sheet's place, and the card comes back when it closes.
 * [picker] draws the picker with the given create callback (null = no create row).
 */
@Composable
internal fun ColumnScope.PickerWithCreate(
    doc: ProjectDoc,
    gate: HiddenGate,
    creator: PickerCreator?,
    onCreated: (PickerCreateValues, Key) -> Unit,
    picker: @Composable ColumnScope.(onCreate: ((kind: CreateKind, query: String, matched: Boolean) -> Unit)?) -> Unit,
) {
    val cardState = rememberPickerCard()
    var pickingParent by rememberSaveable { mutableStateOf(false) }
    // The store is read only for a picker with a create row: a picker without one works outside a project frame
    // as before; no project store (or a read-only project) means no create row (U-15.10).
    val store = if (creator != null) LocalProjectStore.current else null
    val canCreate = creator != null && store != null && !store.readOnly
    val card = cardState.value.takeIf { canCreate }
    if (card != null && pickingParent) {
        PlacePickerSheet(
            doc = doc,
            gate = gate,
            title = PickerCopy.PICK_PARENT,
            onPick = { ref ->
                cardState.value = PickerCreateCards.withParent(card, ref.key)
                pickingParent = false
            },
            onClose = { pickingParent = false },
            selected = PickerCreateCards.parentKey(card)?.let { io.github.lenticualrstone.civilight.model.story.PlaceRef(io.github.lenticualrstone.civilight.model.story.PlaceRefKind.PLACE, it) },
            includeRegions = false,
        )
        return
    }
    picker(if (canCreate) ({ kind, query, matched -> cardState.value = PickerCreateCards.open(kind, query, matched) }) else null)
    if (card == null || creator == null || store == null) return
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    PickerCreateCardOverlay(
        state = card,
        doc = doc,
        hiddenOpen = gate.open,
        canCreate = !busy && PickerCreateCards.canCreate(card),
        onChange = { cardState.value = it },
        onCancel = { cardState.value = null },
        onPickParent = { pickingParent = true },
        onCreate = {
            val values = PickerCreateCards.values(card)
            busy = true
            scope.launch {
                var made: Key? = null
                try {
                    // One edit, one undo step `만들기`; the card closes only once the write applied.
                    store.update(io.github.lenticualrstone.civilight.designsystem.component.CreateCardCopy.CREATE) { d ->
                        creator.write(d, values)?.let { (next, key) -> made = key; next } ?: d
                    }
                } catch (e: ReadOnlyProjectException) {
                    made = null
                } finally {
                    busy = false
                }
                made?.let { key ->
                    cardState.value = null
                    onCreated(values, key)
                }
            }
        },
    )
}

/** The DS-72 card in picker mode (`취소 · 만들기`) with the fields of [PickerCreateCards.fields]. */
@Composable
fun PickerCreateCardOverlay(
    state: CreateCardState,
    doc: ProjectDoc,
    hiddenOpen: Boolean,
    canCreate: Boolean,
    onChange: (CreateCardState) -> Unit,
    onCancel: () -> Unit,
    onCreate: () -> Unit,
    onPickParent: () -> Unit,
) {
    CreateCardOverlay(
        title = CreateCard.title(state.kind),
        mode = CreateCardMode.PICKER,
        canCreate = canCreate,
        onCancel = onCancel,
        onCreate = onCreate,
        dismissOnScrim = CreateCard.dismissOnScrim(state),
    ) {
        PickerCardFields(state, doc, hiddenOpen, onChange, onPickParent)
    }
}

@Composable
private fun ColumnScope.PickerCardFields(
    state: CreateCardState,
    doc: ProjectDoc,
    hiddenOpen: Boolean,
    onChange: (CreateCardState) -> Unit,
    onPickParent: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    val tags = remember(doc) { PickerCreateCards.liveTags(doc) }
    for (field in PickerCreateCards.fields(state.kind)) {
        when {
            field.type == CreateFieldType.TEXT -> CivilightTextField(
                value = state.values[field.key].orEmpty(),
                onValueChange = { onChange(state.with(field.key, it)) },
                label = field.label,
                modifier = Modifier.fillMaxWidth().then(if (field.key == CreateFields.NAME) Modifier.focusRequester(focus) else Modifier),
            )
            field.key == CreateFields.tags.key -> if (tags.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CivilightText(field.label, CivilightTheme.type.label, color = CivilightTheme.colors.ink2)
                    val picked = PickerCreateCards.tagKeys(state)
                    ChipFlow {
                        for (tag in tags) FilterChip(tag.name, tag.key in picked, { onChange(PickerCreateCards.toggleTag(state, tag.key)) }, tagColor = tag.color)
                    }
                }
            }
            field.key == CreateFields.parentRegion.key -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CivilightText(field.label, CivilightTheme.type.label, color = CivilightTheme.colors.ink2)
                val parent = PickerCreateCards.parentKey(state)?.let(doc.index::place)?.takeIf { it.deletedAt == null }
                val shown = parent?.let { if (it.hidden && !hiddenOpen) PickerCopy.HIDDEN_ITEM else it.name } ?: PickerCopy.EMPTY_VALUE
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    CivilightText(shown, CivilightTheme.type.body, Modifier.weight(1f), maxLines = 1)
                    TextAction(PickerCopy.PICK_PARENT, onPickParent)
                }
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}
