package io.github.lenticualrstone.civilight.feature.world.time

import androidx.compose.runtime.saveable.Saver
import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.domain.world.time.EventEdit
import io.github.lenticualrstone.civilight.domain.world.time.EventEdits
import io.github.lenticualrstone.civilight.domain.world.time.EventMasks
import io.github.lenticualrstone.civilight.domain.world.time.WorldClock
import io.github.lenticualrstone.civilight.domain.world.time.WorldTimeEdits
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.common.RichTextEdit
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.PointRef
import io.github.lenticualrstone.civilight.model.project.WorldTimeMode
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.world.EventKind
import io.github.lenticualrstone.civilight.model.world.TimelineEvent
import io.github.lenticualrstone.civilight.model.world.WorldDate
import io.github.lenticualrstone.civilight.ui.labels.UnitLabels
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The 사건 sheet's draft (WLD-32): `종류`, `이름`, `설명`, the time (a world event's year, plus month
 * and day in calendar mode; a story event's scene), `장소`, `관련 용어`, `공개 시점`. Saved with the
 * sheet as JSON, so a covered sheet (picker on top) and process death keep it.
 */
@Serializable
data class EventDraft(
    val story: Boolean = false,
    val title: String = "",
    val description: String = "",
    val year: String = "",
    val month: String = "",
    val day: String = "",
    val sceneKey: Key? = null,
    val placeKind: String? = null,
    val placeKey: Key? = null,
    val termKeys: List<Key> = emptyList(),
    /** Unit whose start is the `공개 시점` (world events only). */
    val revealUnitKey: Key? = null,
) {
    val place: PlaceRef?
        get() = placeKey?.let { key -> PlaceRefKind.entries.firstOrNull { it.name == placeKind }?.let { PlaceRef(it, key) } }

    fun withPlace(ref: PlaceRef?): EventDraft = copy(placeKind = ref?.kind?.known?.name, placeKey = ref?.key)

    fun toggleTerm(key: Key): EventDraft = copy(termKeys = if (key in termKeys) termKeys - key else termKeys + key)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        val Saver: Saver<EventDraft, String> = Saver(
            save = { json.encodeToString(serializer(), it) },
            restore = { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() ?: EventDraft() },
        )

        /** The draft of an existing event (the edit sheet). */
        fun of(event: TimelineEvent): EventDraft = EventDraft(
            story = event.kind.known == EventKind.STORY,
            title = event.title,
            description = RichTextEdit.textOf(event.description),
            year = event.worldDate?.year?.toString().orEmpty(),
            month = event.worldDate?.month?.toString().orEmpty(),
            day = event.worldDate?.day?.toString().orEmpty(),
            sceneKey = event.sceneKey,
            placeKind = event.place?.kind?.known?.name,
            placeKey = event.place?.key,
            termKeys = event.relatedTermKeys,
            revealUnitKey = event.revealPoint?.unitKey,
        )
    }
}

/** What the sheet shows for a draft: which time inputs exist and whether `넣기` / `고치기` is enabled. */
data class EventFormUi(
    /** `세계 사건 · 이야기 사건` segments are offered (the kind of an existing event stays). */
    val kindChoice: Boolean,
    /** World event in a project with time: the `연도` field. */
    val showYear: Boolean,
    /** Calendar mode: `월` and `일` too. */
    val showMonthDay: Boolean,
    /** Story event: the `씬` row (time comes from the scene). */
    val showScene: Boolean,
    val sceneLabel: String,
    val placeLabel: String,
    val termsLabel: String,
    /** World events only. */
    val showReveal: Boolean,
    val revealLabel: String,
    /** The year, month or day typed is not a number in range. */
    val dateInvalid: Boolean,
    val canSave: Boolean,
)

/** The result of `넣기` / `고치기`. */
sealed interface EventSave {
    data class Saved(val doc: ProjectDoc, val eventKey: Key) : EventSave

    /** Nothing to save (empty name, a story event without a scene, an invalid date). */
    data object Invalid : EventSave
}

/** Pure rules of the 사건 sheet (WLD-32): what to show, when it may be saved, and the write. */
object EventForms {
    fun ui(doc: ProjectDoc, draft: EventDraft, editing: Boolean, hiddenOpen: Boolean): EventFormUi {
        val clock = WorldClock(doc.project.worldTime)
        val labels = UnitLabels(doc)
        val world = !draft.story
        val date = parseDate(doc, draft)
        val dateInvalid = world && date == null && !dateBlank(draft, clock.calendar != null)
        val scene = draft.sceneKey?.let { doc.index.unit(it) }
        val sceneLabel = scene?.let { labels.reference(it) } ?: WorldCopy.EMPTY_VALUE
        return EventFormUi(
            kindChoice = !editing,
            showYear = world && clock.hasTimeline,
            showMonthDay = world && clock.calendar != null,
            showScene = draft.story,
            sceneLabel = sceneLabel,
            placeLabel = placeLabel(doc, draft.place, hiddenOpen),
            termsLabel = draft.termKeys.mapNotNull { key ->
                doc.index.term(key)?.takeIf { it.deletedAt == null }?.let { if (it.hidden && !hiddenOpen) WorldCopy.HIDDEN_ITEM else it.name }
            }.joinToString(WorldCopy.SEP).ifEmpty { WorldCopy.EMPTY_VALUE },
            showReveal = world,
            revealLabel = draft.revealUnitKey?.let { labels.code(it) } ?: WorldCopy.EMPTY_VALUE,
            dateInvalid = dateInvalid,
            canSave = draft.title.isNotBlank() && !dateInvalid && (world || scene != null),
        )
    }

    /** `장소 | 하층 구역`: a place or region name, `숨긴 항목` while locked. */
    fun placeLabel(doc: ProjectDoc, ref: PlaceRef?, hiddenOpen: Boolean): String = when {
        ref == null -> null
        !hiddenOpen && EventMasks.placeHidden(doc, ref) -> WorldCopy.HIDDEN_ITEM
        ref.kind.known == PlaceRefKind.PLACE -> doc.index.place(ref.key)?.name
        ref.kind.known == PlaceRefKind.REGION -> doc.index.region(ref.key)?.second?.name
        else -> null
    } ?: WorldCopy.EMPTY_VALUE

    /** Month and day count only in calendar mode (their fields are not shown otherwise). */
    private fun dateBlank(draft: EventDraft, calendar: Boolean) =
        draft.year.isBlank() && (!calendar || (draft.month.isBlank() && draft.day.isBlank()))

    /**
     * The world date typed: null when blank or invalid. The year is any integer; month and day count
     * only in calendar mode and must lie inside the calendar (a day needs its month).
     */
    fun parseDate(doc: ProjectDoc, draft: EventDraft): WorldDate? {
        if (draft.year.isBlank()) return null
        val year = WorldTimeEdits.parseYear(draft.year) ?: return null
        val calendar = WorldClock(doc.project.worldTime).calendar ?: return WorldDate(year)
        val month = draft.month.trim().takeIf { it.isNotEmpty() }?.let { it.toIntOrNull()?.takeIf { m -> m in 1..calendar.months.size } ?: return null }
        val day = draft.day.trim().takeIf { it.isNotEmpty() }?.let { text ->
            val m = month ?: return null
            text.toIntOrNull()?.takeIf { d -> d in 1..calendar.months[m - 1].days.coerceAtLeast(1) } ?: return null
        }
        return WorldDate(year, month, day)
    }

    /** `넣기` (new event) or `고치기` ([eventKey] of the event being edited). */
    fun save(doc: ProjectDoc, draft: EventDraft, eventKey: Key?, ctx: EditContext): EventSave {
        val ui = ui(doc, draft, editing = eventKey != null, hiddenOpen = true)
        if (!ui.canSave) return EventSave.Invalid
        val date = parseDate(doc, draft)
        val reveal = draft.revealUnitKey?.let { PointRef.unitStart(it) }
        if (eventKey == null) {
            val result = if (draft.story) {
                EventEdits.createStoryEvent(doc, draft.title, draft.sceneKey!!, ctx, draft.description, draft.place, draft.termKeys)
            } else {
                EventEdits.createWorldEvent(doc, draft.title, date, ctx, draft.description, draft.place, draft.termKeys, reveal)
            }
            return when (result) {
                is EventEdit.Applied -> EventSave.Saved(result.doc, result.event.key)
                else -> EventSave.Invalid
            }
        }
        val event = doc.index.event(eventKey) ?: return EventSave.Invalid
        val clock = WorldClock(doc.project.worldTime)
        val noTime = clock.mode == WorldTimeMode.NONE
        // Without a calendar the month and day are not shown: a stored month and day stay (WLD-29: dates are never deleted).
        val kept = if (clock.calendar == null) date?.copy(month = event.worldDate?.month, day = event.worldDate?.day) else date
        val next = EventEdits.update(doc, eventKey, ctx) { e ->
            e.copy(
                title = draft.title.trim(),
                description = description(e, draft.description, ctx),
                // `시간 없이` shows no date field: the stored date stays.
                worldDate = if (draft.story) e.worldDate else if (noTime) e.worldDate else kept,
                sceneKey = if (draft.story) draft.sceneKey else e.sceneKey,
                place = draft.place,
                relatedTermKeys = draft.termKeys,
                revealPoint = if (draft.story) e.revealPoint else reveal,
            )
        }
        return EventSave.Saved(next, event.key)
    }

    /** The edited description: untouched runs (links, paragraph keys) stay ([RichTextEdit.apply]). */
    private fun description(event: TimelineEvent, text: String, ctx: EditContext) =
        if (text == RichTextEdit.textOf(event.description)) event.description else RichTextEdit.apply(event.description, text, ctx.newKey)

    /** The text the description field starts with (links show their text). */
    fun descriptionText(event: TimelineEvent): String = RichTextEdit.textOf(event.description)
}
