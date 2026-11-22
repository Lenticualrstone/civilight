package io.github.lenticualrstone.civilight.domain.world.time

import io.github.lenticualrstone.civilight.domain.world.common.Copy
import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.domain.world.common.StoryCodes
import io.github.lenticualrstone.civilight.domain.world.item.FieldText
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.position.PointRef
import io.github.lenticualrstone.civilight.model.position.Rank
import io.github.lenticualrstone.civilight.model.position.recordAt
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.text.DocBlock
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.docText
import io.github.lenticualrstone.civilight.model.world.EventKind
import io.github.lenticualrstone.civilight.model.world.TimelineEvent
import io.github.lenticualrstone.civilight.model.world.WorldDate

/** One `이 사건에서 바뀐 것` row: `하층 거리 · 상태 | 빛 있음 → 대정전 이후`. */
data class EventChangeRow(val record: ChangeRecord, val label: String, val value: String)

/** The event detail pane (WLD-33). */
data class EventDetail(
    val eventKey: Key,
    /** `연표 · 세계 사건` / `연표 · 이야기 사건`. */
    val paneTitle: String,
    val name: String,
    val time: String,
    val description: String,
    /** Field rows `장소 | 하층 거리`, `그때 나이 | 리엔 12세 · 세라 16세`, `원고 | 1-4 씬 2 · 회상`, `관련 용어 | …`. */
    val rows: List<Pair<String, String>>,
    val changes: List<EventChangeRow>,
)

/** Event detail and the `이 사건에서 바뀐 것` section. */
class EventDetails(private val doc: ProjectDoc, times: SceneTimeSource) {
    private val moments = Moments(doc, times)
    private val ages = Ages(doc)
    private val codes = StoryCodes(doc)

    fun detail(eventKey: Key): EventDetail? {
        val event = doc.index.event(eventKey) ?: return null
        val story = event.kind.known == EventKind.STORY
        val moment = moments.ofEvent(eventKey)
        val rows = ArrayList<Pair<String, String>>()
        placeName(event.place)?.let { rows += "장소" to it }
        val year = moment?.let(moments::yearOf)
        if (year != null) {
            val month = (moment as? Moment.WorldYear)?.month
            val day = (moment as? Moment.WorldYear)?.day
            val everyone = doc.cast.characters.filter { it.deletedAt == null }.sortedWith(compareBy({ it.order }, { it.key }))
            val line = everyone.mapNotNull { c -> ages.ageAt(c, year, month, day)?.takeIf { it is Age.Years }?.let { "${c.name} ${it.label()}" } }
            if (line.isNotEmpty()) rows += "그때 나이" to line.joinToString(Copy.SEP)
        }
        val manuscript = if (story) {
            event.sceneKey?.let { codes.sceneLabel(it) }
        } else {
            doc.manuscript.liveScenes.filter { (it.time as? SceneTime.Flashback)?.eventKey == eventKey }
                .mapNotNull { codes.sceneLabel(it.key)?.let { l -> "$l${Copy.SEP}회상" } }
                .joinToString(Copy.SEP).ifEmpty { null }
        }
        manuscript?.let { rows += "원고" to it }
        val terms = event.relatedTermKeys.mapNotNull { doc.index.term(it)?.takeIf { t -> t.deletedAt == null }?.name }
        if (terms.isNotEmpty()) rows += "관련 용어" to terms.joinToString(Copy.SEP)
        return EventDetail(
            eventKey = eventKey,
            paneTitle = if (story) "연표${Copy.SEP}이야기 사건" else "연표${Copy.SEP}세계 사건",
            name = event.title,
            time = when {
                story -> event.sceneKey?.let { codes.episodeCode(it) }.orEmpty()
                else -> event.worldDate?.year?.toString().orEmpty()
            },
            description = event.description.docText,
            rows = rows,
            changes = changes(eventKey),
        )
    }

    /** `◆` records anchored on the event, with the value before them (WLD-33). */
    fun changes(eventKey: Key): List<EventChangeRow> {
        val isLive = { r: ChangeRecord -> doc.liveness.isLive(r) }
        return doc.changes.records
            .filter { isLive(it) && (it.anchor as? Anchor.WorldEvent)?.eventKey == eventKey && it.target is ChangeTarget.Field }
            .sortedWith(compareBy({ it.createdAt }, { it.key }))
            .map { record ->
                val target = record.target as ChangeTarget.Field
                val rank = doc.manuscript.rankOf(record.anchor)
                val before = doc.manuscript.recordAt(doc.changes.records, target, Rank(rank.value - 1), isLive)
                val oldValue = before?.valueAs(FieldValue.serializer())
                    ?: target.entityKind.known?.let { doc.index.fieldsOf(it, target.entityKey)?.get(target.fieldDefKey) }
                val newValue = record.valueAs(FieldValue.serializer())
                val item = target.entityKind.known?.let { doc.index.nameOf(it, target.entityKey) }.orEmpty()
                val field = doc.index.fieldDef(target.fieldDefKey)?.name.orEmpty()
                EventChangeRow(
                    record = record,
                    label = "$item${Copy.SEP}$field",
                    value = "${FieldText.of(doc, oldValue) ?: "없음"} → ${FieldText.of(doc, newValue) ?: "없음"}",
                )
            }
    }

    private fun placeName(ref: PlaceRef?): String? = when (ref?.kind?.known) {
        PlaceRefKind.PLACE -> doc.index.place(ref.key)?.name
        PlaceRefKind.REGION -> doc.index.region(ref.key)?.second?.name
        null -> null
    }
}

/** Result of an event edit. */
sealed interface EventEdit {
    data class Applied(val doc: ProjectDoc, val event: TimelineEvent) : EventEdit

    /** `시간 없이`: events cannot be dated (WLD-29, T51). */
    data object NoWorldTime : EventEdit

    data object EmptyTitle : EventEdit
}

/** Creating and editing timeline events (WLD-32). */
object EventEdits {
    private fun paragraph(ctx: EditContext, text: String): List<DocBlock> =
        if (text.isBlank()) emptyList() else listOf(DocBlock(ctx.newKey(), OrderKeys.FIRST, runs = listOf(TextRun.Plain(text.trim()))))

    /** `세계 사건` with a year (and month/day in calendar mode). */
    fun createWorldEvent(
        doc: ProjectDoc,
        title: String,
        date: WorldDate?,
        ctx: EditContext,
        description: String = "",
        place: PlaceRef? = null,
        relatedTermKeys: List<Key> = emptyList(),
        revealPoint: PointRef? = null,
    ): EventEdit {
        val clean = title.trim()
        if (clean.isEmpty()) return EventEdit.EmptyTitle
        if (date != null && !WorldClock(doc.project.worldTime).hasTimeline) return EventEdit.NoWorldTime
        val calendar = WorldClock(doc.project.worldTime).calendar
        val stored = if (calendar == null) date?.copy(month = null, day = null) else date
        return add(doc, TimelineEvent(
            key = ctx.newKey(), order = nextOrder(doc), kind = EventKind.WORLD.open(), title = clean,
            description = paragraph(ctx, description), worldDate = stored, place = place,
            relatedTermKeys = relatedTermKeys, revealPoint = revealPoint, updatedAt = ctx.now(), updatedBy = ctx.device,
        ))
    }

    /** `이야기 사건` on a scene (time comes from the scene). */
    fun createStoryEvent(
        doc: ProjectDoc,
        title: String,
        sceneKey: Key,
        ctx: EditContext,
        description: String = "",
        place: PlaceRef? = null,
        relatedTermKeys: List<Key> = emptyList(),
    ): EventEdit {
        val clean = title.trim()
        if (clean.isEmpty()) return EventEdit.EmptyTitle
        return add(doc, TimelineEvent(
            key = ctx.newKey(), order = nextOrder(doc), kind = EventKind.STORY.open(), title = clean,
            description = paragraph(ctx, description), sceneKey = sceneKey, place = place ?: doc.index.unit(sceneKey)?.place,
            relatedTermKeys = relatedTermKeys, updatedAt = ctx.now(), updatedBy = ctx.device,
        ))
    }

    /** Replaces fields of an event (tap a field in the detail). */
    fun update(doc: ProjectDoc, eventKey: Key, ctx: EditContext, f: (TimelineEvent) -> TimelineEvent): ProjectDoc =
        doc.copy(world = doc.world.copy(events = doc.world.events.map {
            if (it.key == eventKey) f(it).copy(updatedAt = ctx.now(), updatedBy = ctx.device) else it
        }))

    private fun nextOrder(doc: ProjectDoc): String = OrderKeys.after(doc.world.events.maxOfOrNull { it.order })

    private fun add(doc: ProjectDoc, event: TimelineEvent): EventEdit =
        EventEdit.Applied(doc.copy(world = doc.world.copy(events = doc.world.events + event)), event)
}
