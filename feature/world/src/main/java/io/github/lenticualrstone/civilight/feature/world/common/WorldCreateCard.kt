package io.github.lenticualrstone.civilight.feature.world.common

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.CreateCard
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardMode
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardOverlay
import io.github.lenticualrstone.civilight.designsystem.component.CreateField
import io.github.lenticualrstone.civilight.designsystem.component.CreateFieldType
import io.github.lenticualrstone.civilight.designsystem.component.CreateFields
import io.github.lenticualrstone.civilight.designsystem.component.CreateKind
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.MotionToken
import io.github.lenticualrstone.civilight.domain.world.item.NewWorldItem
import io.github.lenticualrstone.civilight.domain.world.item.WorldCreate
import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.domain.world.time.WorldClock
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.time.EventDraft
import io.github.lenticualrstone.civilight.feature.world.time.EventForms
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.ui.labels.UnitLabels
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * (0.2.0, U-15, WLD-70, WLD-74) The draft of a world create card: a place, term or faction ([kind] =
 * [WorldKind] name) or an event ([kind] = [EVENT]). Saved with the screen as JSON, so a covered card (the
 * `상위 지역` or `씬 고르기` picker on top) and process death keep what was typed.
 */
@Serializable
data class WorldCreateDraft(
    val kind: String,
    val mode: String = CreateCardMode.LIST.name,
    val name: String = "",
    val alias: String = "",
    val note: String = "",
    val parentKey: Key? = null,
    val year: String = "",
    val month: String = "",
    val day: String = "",
    val sceneKey: Key? = null,
    /** (0.2.0, WLD-74) An event's `관련 인물`. */
    val characterKeys: List<Key> = emptyList(),
) {
    val isEvent: Boolean get() = kind == EVENT
    val worldKind: WorldKind? get() = WorldKind.entries.firstOrNull { it.name == kind }
    val cardMode: CreateCardMode get() = CreateCardMode.entries.firstOrNull { it.name == mode } ?: CreateCardMode.LIST

    /** Something typed or picked: a scrim tap then does nothing (DS-72). */
    val hasContent: Boolean
        get() = listOf(name, alias, note, year, month, day).any { it.isNotBlank() } || parentKey != null || sceneKey != null || characterKeys.isNotEmpty()

    fun encode(): String = json.encodeToString(serializer(), this)

    companion object {
        const val EVENT = "EVENT"
        private val json = Json { ignoreUnknownKeys = true }

        fun decode(text: String?): WorldCreateDraft? = text?.let { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() }

        fun of(kind: WorldKind, mode: CreateCardMode = CreateCardMode.LIST, query: String? = null): WorldCreateDraft =
            WorldCreateDraft(kind.name, mode.name, name = query?.trim().orEmpty())

        fun event(): WorldCreateDraft = WorldCreateDraft(EVENT)
    }
}

/**
 * Pure rules of the world create cards (U-15.7): which fields are drawn, the card title, and what is
 * written. The `이미지` field of places and factions is left out in 0.2.0 (hidden, see the lane's
 * Requests): an image needs an owner that exists before `만들기`. Hidden fields are not drawn at all.
 */
object WorldCreateCards {
    /** The DS create kind of a world kind. */
    fun createKind(kind: WorldKind): CreateKind = when (kind) {
        WorldKind.PLACE -> CreateKind.PLACE
        WorldKind.TERM -> CreateKind.TERM
        WorldKind.FACTION -> CreateKind.FACTION
    }

    /** Keys of the fields left out in 0.2.0 (see the class comment). */
    val HIDDEN_FIELD_KEYS: Set<String> = setOf(CreateFields.image.key)

    /** The drawn fields of [kind], in the order of U-15.7. */
    fun fields(kind: CreateKind): List<CreateField> = kind.fields.filterNot { it.key in HIDDEN_FIELD_KEYS }

    fun fields(draft: WorldCreateDraft): List<CreateField> =
        if (draft.isEvent) fields(CreateKind.EVENT) else draft.worldKind?.let { fields(createKind(it)) }.orEmpty()

    fun title(draft: WorldCreateDraft): String =
        CreateCard.title(if (draft.isEvent) CreateKind.EVENT else createKind(draft.worldKind ?: WorldKind.TERM))

    /** What `만들기` writes for a place, term or faction. */
    fun item(draft: WorldCreateDraft): NewWorldItem = NewWorldItem(
        name = draft.name,
        description = draft.note,
        alias = if (draft.worldKind == WorldKind.TERM) draft.alias else "",
        parentPlaceKey = if (draft.worldKind == WorldKind.PLACE) draft.parentKey else null,
    )

    /**
     * The event of the card (WLD-74): `때` is a year (plus month and day in calendar mode) for a `WORLD`
     * event, or a scene for a `STORY` event; left empty, a `WORLD` event with no date. `종류` follows `때`.
     */
    fun eventDraft(draft: WorldCreateDraft): EventDraft = EventDraft(
        story = draft.sceneKey != null,
        title = draft.name,
        year = draft.year,
        month = draft.month,
        day = draft.day,
        sceneKey = draft.sceneKey,
        characterKeys = draft.characterKeys,
    )

    fun canCreate(doc: ProjectDoc?, draft: WorldCreateDraft): Boolean {
        if (!WorldCreate.canCreate(draft.name)) return false
        if (!draft.isEvent) return true
        return doc != null && EventForms.ui(doc, eventDraft(draft), editing = false, hiddenOpen = true).canSave
    }

    /** Typing a year makes the event a world event (the scene is dropped), and the reverse. */
    fun withYear(draft: WorldCreateDraft, year: String): WorldCreateDraft = draft.copy(year = year, sceneKey = null)

    fun withScene(draft: WorldCreateDraft, sceneKey: Key): WorldCreateDraft = draft.copy(sceneKey = sceneKey, year = "", month = "", day = "")
}

/**
 * The world create card: the shared DS-72 overlay with the world field set of [draft]. [onPickParent]
 * opens the `상위 지역` place picker (the screen hides this card while it is on top); [onPickScene] opens
 * `씬 고르기` for an event's `때`.
 */
@Composable
fun WorldCreateCard(
    draft: WorldCreateDraft,
    doc: ProjectDoc?,
    hiddenOpen: Boolean,
    onChange: (WorldCreateDraft) -> Unit,
    onCancel: () -> Unit,
    onCreate: () -> Unit,
    onCreateAndOpen: () -> Unit,
    onPickParent: () -> Unit = {},
    onPickScene: () -> Unit = {},
    onPickPeople: () -> Unit = {},
) {
    CreateCardOverlay(
        title = WorldCreateCards.title(draft),
        mode = draft.cardMode,
        canCreate = WorldCreateCards.canCreate(doc, draft),
        onCancel = onCancel,
        onCreate = onCreate,
        onCreateAndOpen = onCreateAndOpen,
        dismissOnScrim = !draft.hasContent,
    ) {
        WorldCreateFieldsContent(draft, doc, hiddenOpen, onChange, onPickParent, onPickScene, onPickPeople)
    }
}

@Composable
private fun ColumnScope.WorldCreateFieldsContent(
    draft: WorldCreateDraft,
    doc: ProjectDoc?,
    hiddenOpen: Boolean,
    onChange: (WorldCreateDraft) -> Unit,
    onPickParent: () -> Unit,
    onPickScene: () -> Unit,
    onPickPeople: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    WorldCreateCards.fields(draft).forEach { field ->
        when {
            field.key == CreateFields.NAME -> CivilightTextField(
                value = draft.name,
                onValueChange = { onChange(draft.copy(name = it)) },
                label = field.label,
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
            field.key == CreateFields.aliases.key -> CivilightTextField(
                value = draft.alias,
                onValueChange = { onChange(draft.copy(alias = it)) },
                label = field.label,
                modifier = Modifier.fillMaxWidth(),
            )
            field.type == CreateFieldType.TEXT -> CivilightTextField(
                value = draft.note,
                onValueChange = { onChange(draft.copy(note = it)) },
                label = field.label,
                modifier = Modifier.fillMaxWidth(),
            )
            field.key == CreateFields.parentRegion.key -> PickField(
                label = field.label,
                value = draft.parentKey?.let { key -> doc?.index?.place(key)?.let { if (it.hidden && !hiddenOpen) WorldCopy.HIDDEN_ITEM else it.name } },
                action = WorldCopy.PICK_PLACE,
                onPick = onPickParent,
            )
            field.key == CreateFields.time.key && doc != null -> EventTimeField(field.label, draft, doc, onChange, onPickScene)
            field.key == CreateFields.people.key && doc != null -> PickField(
                label = field.label,
                value = io.github.lenticualrstone.civilight.feature.world.time.EventForms.peopleLabel(doc, draft.characterKeys, hiddenOpen)
                    .takeIf { draft.characterKeys.isNotEmpty() },
                action = WorldCopy.PICK_PEOPLE,
                onPick = onPickPeople,
            )
        }
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

/** A field whose value is picked in a sheet: the label, the value (`—` when none) and the text button. */
@Composable
private fun PickField(label: String, value: String?, action: String, onPick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CivilightText(label, CivilightTheme.type.label, color = CivilightTheme.colors.ink2)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CivilightText(value ?: WorldCopy.EMPTY_VALUE, CivilightTheme.type.body, Modifier.weight(1f), maxLines = 1)
            TextAction(action, onPick)
        }
    }
}

/** `때`: the year (and month, day with a world calendar) or a scene picked with `씬 고르기` (WLD-74). */
@Composable
private fun EventTimeField(label: String, draft: WorldCreateDraft, doc: ProjectDoc, onChange: (WorldCreateDraft) -> Unit, onPickScene: () -> Unit) {
    val clock = WorldClock(doc.project.worldTime)
    val numbers = KeyboardOptions(keyboardType = KeyboardType.Number)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CivilightText(label, CivilightTheme.type.label, color = CivilightTheme.colors.ink2)
        if (clock.hasTimeline) {
            CivilightTextField(
                value = draft.year,
                onValueChange = { onChange(WorldCreateCards.withYear(draft, it)) },
                label = WorldCopy.EVENT_YEAR,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = numbers,
            )
            if (clock.calendar != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CivilightTextField(draft.month, { onChange(draft.copy(month = it, sceneKey = null)) }, Modifier.weight(1f), label = WorldCopy.EVENT_MONTH, keyboardOptions = numbers)
                    CivilightTextField(draft.day, { onChange(draft.copy(day = it, sceneKey = null)) }, Modifier.weight(1f), label = WorldCopy.EVENT_DAY, keyboardOptions = numbers)
                }
            }
        }
        val scene = draft.sceneKey?.let { doc.index.unit(it) }?.let { UnitLabels(doc).reference(it) }
        PickField(WorldCopy.EVENT_SCENE, scene, WorldCopy.PICK_SCENE, onPickScene)
    }
}
