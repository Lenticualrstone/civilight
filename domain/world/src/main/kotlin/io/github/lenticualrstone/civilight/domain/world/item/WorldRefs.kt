package io.github.lenticualrstone.civilight.domain.world.item

import io.github.lenticualrstone.civilight.domain.world.common.Copy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.trash.NameTagKind

/** How a reference to a world item reads (WLD-13, WLD-14). */
sealed interface RefLabel {
    val text: String

    /** The item is live: its name. */
    data class Live(override val text: String) : RefLabel

    /** In the trash: `지운 용어 · 오리지늄 등불`, dotted with `◇`. */
    data class Deleted(override val text: String, val name: String, val actions: List<String>) : RefLabel

    /** Purged: `영구 삭제 · 오리지늄 등불` from the name tag; no `되살리기`. */
    data class Purged(override val text: String, val name: String, val actions: List<String>) : RefLabel

    /** A hidden item while locked: `숨긴 항목` (the text is never altered). */
    data class Hidden(override val text: String = "숨긴 항목") : RefLabel

    /** Nothing known about the key. */
    data class Missing(override val text: String = "") : RefLabel

    val dotted: Boolean get() = this is Deleted || this is Purged
}

/** Where a dotted reference sits: its actions differ (WLD-14 table). */
enum class RefSite { LINK, SCENE_PLACE, FIELD_LINK, MAP_LINK }

/**
 * Labels of references to terms, places and factions: live names, `지운 … · <이름>` for trashed
 * items, `영구 삭제 · <이름>` from name tags after purge, `숨긴 항목` for hidden items while locked.
 */
object WorldRefs {
    private fun kindWord(kind: WorldKind): String = kind.label

    private fun actions(site: RefSite, restorable: Boolean): List<String> {
        val base = when (site) {
            RefSite.LINK -> listOf("다른 항목에 잇기", "글로 풀기", "되살리기", "이대로 두기")
            RefSite.SCENE_PLACE -> listOf("장소 고르기", "되살리기", "이대로 두기")
            RefSite.FIELD_LINK, RefSite.MAP_LINK -> listOf("다른 항목 고르기", "비우기", "되살리기", "이대로 두기")
        }
        return if (restorable) base else base - "되살리기"
    }

    /** The label of a term, place or faction [key] referenced from [site]. */
    fun label(doc: ProjectDoc, kind: WorldKind, key: Key, site: RefSite = RefSite.LINK, hiddenUnlocked: Boolean = false): RefLabel {
        val row = when (kind) {
            WorldKind.TERM -> doc.index.term(key)?.let { Triple(it.name, it.deletedAt, it.hidden) }
            WorldKind.PLACE -> doc.index.place(key)?.let { Triple(it.name, it.deletedAt, it.hidden) }
            WorldKind.FACTION -> doc.index.faction(key)?.let { Triple(it.name, it.deletedAt, it.hidden) }
        }
        if (row == null) {
            val tag = doc.index.nameTag(key) ?: return RefLabel.Missing()
            if (tag.hidden && !hiddenUnlocked) return RefLabel.Hidden()
            return RefLabel.Purged("영구 삭제${Copy.SEP}${tag.name}", tag.name, actions(site, restorable = false))
        }
        val (name, deletedAt, hidden) = row
        if (hidden && !hiddenUnlocked) return RefLabel.Hidden()
        if (deletedAt != null) return RefLabel.Deleted("지운 ${kindWord(kind)}${Copy.SEP}$name", name, actions(site, restorable = true))
        return RefLabel.Live(name)
    }

    /** The label of an `@` link run to a world item, or null for links to other kinds. */
    fun link(doc: ProjectDoc, run: TextRun.Link, hiddenUnlocked: Boolean = false): RefLabel? {
        val kind = when (run.target.kind.known) {
            LinkTargetKind.TERM -> WorldKind.TERM
            LinkTargetKind.PLACE -> WorldKind.PLACE
            LinkTargetKind.FACTION -> WorldKind.FACTION
            else -> return null
        }
        return when (val label = label(doc, kind, run.target.key, RefSite.LINK, hiddenUnlocked)) {
            is RefLabel.Live -> RefLabel.Live(run.text)
            else -> label
        }
    }

    /** A scene's `장소 · 지역`: the place, or `지운 장소 · 잿더미 시장`; regions on deleted maps read `지운 지역 · <이름>`. */
    fun scenePlace(doc: ProjectDoc, ref: PlaceRef, hiddenUnlocked: Boolean = false): RefLabel = when (ref.kind.known) {
        PlaceRefKind.PLACE -> label(doc, WorldKind.PLACE, ref.key, RefSite.SCENE_PLACE, hiddenUnlocked)
        PlaceRefKind.REGION -> {
            val (map, region) = doc.index.region(ref.key) ?: return RefLabel.Missing()
            if (doc.liveness.isMapLive(map.key)) RefLabel.Live(region.name)
            else RefLabel.Deleted("지운 지역${Copy.SEP}${region.name}", region.name, actions(RefSite.SCENE_PLACE, restorable = true))
        }
        null -> RefLabel.Missing()
    }

    /** The name-tag kind of a world item kind. */
    fun nameTagKind(kind: WorldKind): NameTagKind = when (kind) {
        WorldKind.TERM -> NameTagKind.TERM
        WorldKind.PLACE -> NameTagKind.PLACE
        WorldKind.FACTION -> NameTagKind.FACTION
    }
}
