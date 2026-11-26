package io.github.lenticualrstone.civilight.feature.world.place

import io.github.lenticualrstone.civilight.domain.actions.AffiliationActions
import io.github.lenticualrstone.civilight.domain.world.item.AffiliationMember
import io.github.lenticualrstone.civilight.domain.world.item.AffiliationSection
import io.github.lenticualrstone.civilight.domain.world.item.FactionMembers
import io.github.lenticualrstone.civilight.domain.world.item.PlaceName
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint

/** A person row under a faction (board `ImmContent` `mem`): avatar, name, one-liner, `2곳`; [deleted] = dotted (rule 20). */
data class MemberRowUi(
    val key: Key,
    val name: String,
    val role: String?,
    val multi: String?,
    val deleted: Boolean,
)

/**
 * A faction row (board `org`): chevron (only with members), name, `거점 <place>` ([baseDeleted] dotted), count
 * (`인물 3`, none without a `소속` basis). [deleted] = the faction is in the trash (dotted, rule 20).
 */
data class FactionRowUi(
    val key: Key,
    val name: String,
    val base: String?,
    val baseKey: Key?,
    val baseDeleted: Boolean,
    val count: String?,
    val deleted: Boolean,
    val members: List<MemberRowUi>,
) {
    val expandable: Boolean get() = members.isNotEmpty()
}

/** A `무소속` row (board `memLoose`): name, one-liner, `국적 X · 거주 Y`. */
data class LooseRowUi(val key: Key, val name: String, val role: String?, val line: String, val deleted: Boolean)

/**
 * `소속 세력 및 인물` of the place card (board `ImmContent` place branch, rules `ImmPlaceRules`). Never hidden as a
 * whole: without a `소속` basis the factions come by `거점` only and [pickBasis] shows the `소속 기준 고르기` line.
 */
data class AffiliationUi(
    val title: String,
    /** `세력 2 · 인물 4`. */
    val header: String,
    val includeSubPlaces: Boolean,
    /** `시점 1-5 · 소속 기준 ‘소속’`, or null without a basis. */
    val basisLine: String?,
    /** `소속 기준 고르기`, or null. */
    val pickBasis: String?,
    val factions: List<FactionRowUi>,
    /** `무소속 · 2`, or null when neither `거주` nor `국적` is set. */
    val looseTitle: String?,
    val loose: List<LooseRowUi>,
    /** `숨긴 세력 1 · 숨긴 인물 2` (the row adds `지문으로 보기`), or null. */
    val lockLine: String?,
    /** `순서 바꾸기` shows (an editable project with something to order). */
    val canReorder: Boolean,
) {
    companion object {
        const val LIST_FACTIONS = "factions"
        const val LIST_LOOSE = "loose"

        fun membersList(factionKey: Key): String = "members:$factionKey"
    }
}

/** `소속 인물` of the faction card: the same rows, `거점`, and the same lines. */
data class FactionMembersUi(
    val factionKey: Key,
    val base: String?,
    val baseKey: Key?,
    val baseDeleted: Boolean,
    val members: List<MemberRowUi>,
    val pickBasis: String?,
    val lockLine: String?,
    val canReorder: Boolean,
)

/** Pure mapping of the domain's `Affiliations` to the card rows. */
object AffiliationState {
    fun place(doc: ProjectDoc, placeKey: Key, lens: LensPoint, includeSubPlaces: Boolean, hiddenOpen: Boolean, readOnly: Boolean): AffiliationUi? {
        val s = AffiliationActions.section(doc, placeKey, lens, includeSubPlaces, hiddenOpen) ?: return null
        return of(doc, s, readOnly)
    }

    fun of(doc: ProjectDoc, s: AffiliationSection, readOnly: Boolean): AffiliationUi {
        val factions = s.factions.map { g ->
            FactionRowUi(
                key = g.factionKey,
                name = g.name,
                base = g.base?.let { PlaceCopy.base(it.name) },
                baseKey = g.base?.key,
                baseDeleted = g.base?.deleted == true,
                count = if (s.membersAvailable) g.countLabel else null,
                deleted = g.deleted,
                members = g.members.map { member(doc, it) },
            )
        }
        val loose = s.loose.map { LooseRowUi(it.characterKey, it.name, role(doc, it.characterKey), it.line, it.deleted) }
        val orderable = factions.size > 1 || factions.any { it.members.size > 1 } || loose.size > 1
        return AffiliationUi(
            title = s.title,
            header = s.header,
            includeSubPlaces = s.includeSubPlaces,
            basisLine = s.basisLine,
            pickBasis = s.pickBasisLine,
            factions = factions,
            looseTitle = if (s.looseAvailable) PlaceCopy.looseTitle(loose.count { !it.deleted }) else null,
            loose = loose,
            lockLine = s.lockLine,
            canReorder = !readOnly && orderable,
        )
    }

    fun faction(doc: ProjectDoc, factionKey: Key, lens: LensPoint, hiddenOpen: Boolean, readOnly: Boolean): FactionMembersUi? {
        val m: FactionMembers = AffiliationActions.factionMembers(doc, factionKey, lens, hiddenOpen) ?: return null
        return FactionMembersUi(
            factionKey = factionKey,
            base = m.base?.name,
            baseKey = m.base?.key,
            baseDeleted = m.base?.deleted == true,
            members = m.members.map { member(doc, it) },
            pickBasis = m.pickBasisLine,
            lockLine = m.lockLine,
            canReorder = !readOnly && m.members.size > 1,
        )
    }

    /** `거점 <place>` subtitle of the faction card, or null without a base. */
    fun baseSubtitle(base: PlaceName?): String? = base?.let { PlaceCopy.base(it.name) }

    private fun member(doc: ProjectDoc, m: AffiliationMember) =
        MemberRowUi(m.characterKey, m.name, role(doc, m.characterKey), m.multiLabel, m.deleted)

    /** The person's one-liner (`서고 관리인`), else nothing (no guessing from other fields). */
    private fun role(doc: ProjectDoc, key: Key): String? =
        doc.index.character(key)?.oneLiner?.trim()?.takeIf { it.isNotEmpty() }
}
