package io.github.lenticualrstone.civilight.feature.library.shelf

import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.domain.library.common.TimeLabels
import io.github.lenticualrstone.civilight.domain.library.masking.ProjectProfile
import io.github.lenticualrstone.civilight.domain.library.masking.RecentTarget
import io.github.lenticualrstone.civilight.domain.library.masking.Recents
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.RecentItem
import io.github.lenticualrstone.civilight.model.app.RecentKind
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.Protections
import io.github.lenticualrstone.civilight.navigation.CharacterProfile
import io.github.lenticualrstone.civilight.navigation.FactionCard
import io.github.lenticualrstone.civilight.navigation.PageDoc
import io.github.lenticualrstone.civilight.navigation.PlaceCard
import io.github.lenticualrstone.civilight.navigation.Route
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.TermCard
import io.github.lenticualrstone.civilight.ui.labels.UnitLabels
import java.time.ZoneId

/** One 최근 연 파일 card (LIB-05): `<kind> · <project>`, the item title (null when masked), time. */
data class RecentCardUi(
    val item: RecentItem,
    val kindLine: String,
    val title: String?,
    val time: String,
    /** The project's protections: a card that names content protects the window (LIB-26, S-12). */
    val protections: Protections,
    /** True when the card shows only the masked `R-18 프로젝트` label (no window protection, S-12). */
    val masked: Boolean,
    /** Blob hash of the item's first image, or null (the card draws its art then). */
    val imageHash: String?,
)

/**
 * 최근 연 파일 (LIB-05): what an open of a screen records, and the cards after masking. Opens happen
 * in other areas' screens; the 서재 watches the back stack and records the top route here
 * (`ED`, because no area calls `AppStateStore.recordRecent` yet).
 */
object RecentsModel {
    /** Cover 3, inner 4 (LIB-04, LIB-05). */
    const val COVER_COUNT = 3
    const val WIDE_COUNT = 4

    /**
     * The [RecentItem] for an opened [route], or null when it is not a scene, character, page,
     * term, place or faction, or must not be remembered: the read-only example, a hidden project,
     * a hidden or trashed item (R19 §6 `최근 연 파일 · 빠짐`).
     */
    fun itemOf(route: NavKey, doc: ProjectDoc?, isExample: Boolean, now: Timestamp, newKey: () -> Key): RecentItem? {
        if (doc == null || isExample || doc.project.protections.hidden) return null
        val (kind, target) = targetOf(route, doc) ?: return null
        val t = targetInfo(doc, kind, target) ?: return null
        if (t.hidden || t.trashed) return null
        return RecentItem(key = newKey(), projectKey = doc.key, kind = kind, targetKey = target, openedAt = now)
    }

    private fun targetOf(route: NavKey, doc: ProjectDoc): Pair<RecentKind, Key>? = when (route) {
        is Script -> (route.sceneKey ?: route.blockKey?.let { doc.index.block(it)?.sceneKey })?.let { RecentKind.SCENE to it }
        is CharacterProfile -> RecentKind.CHARACTER to route.characterKey
        is PageDoc -> RecentKind.PAGE to route.pageKey
        is TermCard -> RecentKind.TERM to route.termKey
        is PlaceCard -> RecentKind.PLACE to route.placeKey
        is FactionCard -> RecentKind.FACTION to route.factionKey
        else -> null
    }

    /** Title, hidden and trashed state of a target, or null when it does not exist. */
    fun targetInfo(doc: ProjectDoc, kind: RecentKind, key: Key): RecentTarget? = when (kind) {
        RecentKind.SCENE -> doc.index.unit(key)?.let { u ->
            RecentTarget(UnitLabels(doc).referenceWithTitle(u) ?: u.title.orEmpty(), u.hidden, !doc.liveness.isUnitLive(key))
        }
        RecentKind.CHARACTER -> doc.index.character(key)?.let { RecentTarget(it.name, it.hidden, it.deletedAt != null) }
        RecentKind.PAGE -> doc.pages[key]?.let { RecentTarget(it.name, it.hidden, it.deletedAt != null) }
        RecentKind.TERM -> doc.index.term(key)?.let { RecentTarget(it.name, it.hidden, it.deletedAt != null) }
        RecentKind.PLACE -> doc.index.place(key)?.let { RecentTarget(it.name, it.hidden, it.deletedAt != null) }
        RecentKind.FACTION -> doc.index.faction(key)?.let { RecentTarget(it.name, it.hidden, it.deletedAt != null) }
    }

    /**
     * Cards newest first, at most [limit]: `domain:library` `Recents.cards` decides what is left out
     * and what is masked; only listed projects (live, not the example) count.
     */
    fun cards(
        items: List<RecentItem>,
        projects: List<ShelfProject>,
        now: Timestamp,
        zone: ZoneId,
        limit: Int,
    ): List<RecentCardUi> {
        val listed = projects.filter { it.entry.deletedAt == null && !it.entry.isExample }
        val docs = listed.mapNotNull { p -> p.doc?.let { p.entry.key to it } }.toMap()
        val profiles = docs.mapValues { (_, doc) -> ProjectProfile.of(doc.project) }
        return Recents.cards(items, profiles, { item -> docs[item.projectKey]?.let { targetInfo(it, item.kind, item.targetKey) } }, limit)
            .map { card ->
                val doc = docs.getValue(card.item.projectKey)
                RecentCardUi(
                    item = card.item,
                    kindLine = card.kindLine,
                    title = card.title,
                    time = TimeLabels.relative(card.openedAt, now, zone),
                    protections = doc.project.protections,
                    masked = card.title == null,
                    imageHash = if (card.title == null) null else imageOf(doc, card.item),
                )
            }
    }

    private fun imageOf(doc: ProjectDoc, item: RecentItem): String? {
        val owner = when (item.kind) {
            RecentKind.CHARACTER -> ImageOwnerKind.CHARACTER
            RecentKind.PAGE -> ImageOwnerKind.PAGE
            RecentKind.TERM -> ImageOwnerKind.TERM
            RecentKind.PLACE -> ImageOwnerKind.PLACE
            RecentKind.FACTION -> ImageOwnerKind.FACTION
            RecentKind.SCENE -> return null
        }
        return doc.assets.imageUses.firstOrNull { it.ownerKind.known == owner && it.ownerKey == item.targetKey }?.blobHash
    }

    /** The screen a card opens. */
    fun routeOf(item: RecentItem): Route = when (item.kind) {
        RecentKind.SCENE -> Script(item.projectKey, sceneKey = item.targetKey)
        RecentKind.CHARACTER -> CharacterProfile(item.projectKey, item.targetKey)
        RecentKind.PAGE -> PageDoc(item.projectKey, item.targetKey)
        RecentKind.TERM -> TermCard(item.projectKey, item.targetKey)
        RecentKind.PLACE -> PlaceCard(item.projectKey, item.targetKey)
        RecentKind.FACTION -> FactionCard(item.projectKey, item.targetKey)
    }
}
