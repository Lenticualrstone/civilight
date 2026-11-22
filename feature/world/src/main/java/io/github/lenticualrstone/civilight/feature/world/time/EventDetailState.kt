package io.github.lenticualrstone.civilight.feature.world.time

import io.github.lenticualrstone.civilight.domain.trash.ItemId
import io.github.lenticualrstone.civilight.domain.trash.ItemKind
import io.github.lenticualrstone.civilight.domain.trash.TrashCopy
import io.github.lenticualrstone.civilight.domain.world.time.EventDetails
import io.github.lenticualrstone.civilight.domain.world.time.EventMasks
import io.github.lenticualrstone.civilight.domain.world.time.StoryEngine
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.common.CardText
import io.github.lenticualrstone.civilight.feature.world.common.ItemLifecycle
import io.github.lenticualrstone.civilight.feature.world.common.ParagraphUi
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/** `이 사건에서 바뀐 것` row: `하층 구역 · 상태 | 빛 있음 → 대정전 이후`. */
data class EventChangeUi(val id: String, val label: String, val value: String)

/** Whether the detail can show its event. */
sealed interface EventAccess {
    data object Open : EventAccess

    /** A story event on a hidden scene while locked: `숨긴 씬` with `지문으로 보기` (rule 19). */
    data object Locked : EventAccess

    /** In the trash (`지운 사건 · 대정전`, dotted, with `되살리기` when its entry exists) or unknown. */
    data class Gone(val text: String, val restorable: Boolean) : EventAccess
}

/**
 * 사건 상세 (board `TwoTimeline` right pane): pane title `연표 · 세계 사건`, name, time, description
 * with links, rows `장소 · 그때 나이 · 원고 · 관련 용어`, and `이 사건에서 바뀐 것`.
 */
data class EventDetailUi(
    val eventKey: Key,
    val access: EventAccess,
    val paneTitle: String,
    val name: String,
    val time: String,
    val description: List<ParagraphUi>,
    val rows: List<Pair<String, String>>,
    val changes: List<EventChangeUi>,
    /** Description holds links to hidden items while locked: its editor needs `지문으로 보기`. */
    val descriptionLocked: Boolean,
    /** `+ 변화` can add a `◆` anchored on this event (live world event, WLD-33). */
    val changeOffered: Boolean = false,
)

/** Builds the event detail from `domain:world` [EventDetails] (WLD-32 editing, WLD-33 view). */
object EventDetailState {
    fun build(doc: ProjectDoc, eventKey: Key, hiddenOpen: Boolean): EventDetailUi {
        val event = doc.index.event(eventKey)
        if (event == null || event.deletedAt != null) {
            val text = event?.let { TrashCopy.deletedLabel(ItemKind.EVENT, it.title) }.orEmpty()
            val restorable = event != null && ItemLifecycle.restorable(doc, ItemId.event(eventKey))
            return EventDetailUi(eventKey, EventAccess.Gone(text, restorable), WorldCopy.TIMELINE_TITLE, "", "", emptyList(), emptyList(), emptyList(), false)
        }
        if (!hiddenOpen && EventMasks.sceneHidden(doc, event)) {
            return EventDetailUi(eventKey, EventAccess.Locked, WorldCopy.TIMELINE_TITLE, WorldCopy.HIDDEN_SCENE, "", emptyList(), emptyList(), emptyList(), true)
        }
        val detail = EventDetails(doc, StoryEngine(doc)).detail(eventKey, hiddenUnlocked = hiddenOpen)!!
        return EventDetailUi(
            eventKey = eventKey,
            access = EventAccess.Open,
            paneTitle = detail.paneTitle,
            name = detail.name,
            time = detail.time,
            description = CardText.paragraphs(doc, event.description, hiddenOpen),
            rows = detail.rows,
            changes = detail.changes.map { EventChangeUi(it.record.key, it.label, it.value) },
            descriptionLocked = CardText.hasLockedLinks(doc, event.description, hiddenOpen),
            changeOffered = EventChanges.offered(doc, eventKey),
        )
    }
}
