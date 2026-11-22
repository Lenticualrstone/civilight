package io.github.lenticualrstone.civilight.domain.cast.relation

import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.cast.Relation
import io.github.lenticualrstone.civilight.model.cast.RelationCategory
import io.github.lenticualrstone.civilight.model.cast.RelationKind
import io.github.lenticualrstone.civilight.model.cast.RelationSymmetry
import io.github.lenticualrstone.civilight.model.cast.SlotValue
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.change.RelationSlotName
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc
import io.github.lenticualrstone.civilight.model.open

/** A pair row of the settings screen: `선배 ↔ 후배` · `쓰임 1`. */
data class KindRow(val key: Key, val title: String, val usage: Int) {
    val usageLabel: String get() = "쓰임 $usage"
}

/** `프로젝트 설정 › 관계 종류` (`RCRelTypes`). */
data class KindSettings(
    /** `사이 · 11`. */
    val betweenTitle: String,
    val betweenNote: String,
    /** `대칭 · 7`. */
    val symmetricTitle: String,
    val symmetric: List<KindRow>,
    /** `짝 · 4`. */
    val pairTitle: String,
    val pairNote: String,
    val pairs: List<KindRow>,
    /** `마음 · 11`. */
    val mindTitle: String,
    val mindNote: String,
    val minds: List<KindRow>,
    val addLabel: String,
)

/** The effect of switching a kind's `사이 / 마음` or `대칭 / 짝` before it is applied (TC-13-20). */
data class KindSwitchPreview(val relationKeys: List<Key>, val pairs: List<String>) {
    /** `바뀌는 관계 1` [PROPOSED copy]. */
    val countLabel: String get() = "바뀌는 관계 ${relationKeys.size}"
}

/** Relation kinds (CST-32): default list, usage, add, rename, delete, pair rename, switch preview. */
object RelationKinds {
    const val ADD = "+ 새 종류"
    val DEFAULT_SYMMETRIC = listOf("동료", "형제자매", "배우자", "연인", "친구", "거래 상대", "적")
    val DEFAULT_PAIRS = listOf("부모" to "자식", "스승" to "제자", "상관" to "부하", "선배" to "후배")
    val DEFAULT_MINDS = listOf("신뢰", "의지", "동경", "호감", "보호", "경계", "불신", "질투", "증오", "죄책감", "무관심")

    /** The rule 13 §3 seed list for a new project (CST Q1 default). */
    fun defaults(ctx: EditContext): List<RelationKind> {
        val between = OrderKeys.evenlySpaced(DEFAULT_SYMMETRIC.size + DEFAULT_PAIRS.size)
        val minds = OrderKeys.evenlySpaced(DEFAULT_MINDS.size)
        return DEFAULT_SYMMETRIC.mapIndexed { i, n ->
            RelationKind(ctx.newKey(), RelationCategory.BETWEEN.open(), RelationSymmetry.SYMMETRIC.open(), n, null, between[i], ctx.now, ctx.by)
        } + DEFAULT_PAIRS.mapIndexed { i, (n, p) ->
            RelationKind(ctx.newKey(), RelationCategory.BETWEEN.open(), RelationSymmetry.PAIR.open(), n, p, between[DEFAULT_SYMMETRIC.size + i], ctx.now, ctx.by)
        } + DEFAULT_MINDS.mapIndexed { i, n ->
            RelationKind(ctx.newKey(), RelationCategory.MIND.open(), null, n, null, minds[i], ctx.now, ctx.by)
        }
    }

    fun ordered(doc: ProjectDoc): List<RelationKind> = doc.project.relationKinds.sortedWith(compareBy({ it.order }, { it.key }))

    fun of(doc: ProjectDoc, category: RelationCategory): List<RelationKind> = ordered(doc).filter { it.category.known == category }

    /** Kind chips of a picker (`경계 · 신뢰 · … · + 새 종류`). */
    fun chips(doc: ProjectDoc, category: RelationCategory): List<String> = of(doc, category).map { it.name } + ADD

    /** Settings row sub: `사이 11 · 마음 11`. */
    fun summary(doc: ProjectDoc): String =
        "사이 ${of(doc, RelationCategory.BETWEEN).size} · 마음 ${of(doc, RelationCategory.MIND).size}"

    /** `쓰임 N`: live relations using the kind in a base slot or any live record. */
    fun usage(doc: ProjectDoc, kindKey: Key): Int = relationsUsing(doc, kindKey).size

    /** Relations (live) whose base slots or live records use [kindKey]. */
    fun relationsUsing(doc: ProjectDoc, kindKey: Key): List<Relation> {
        val viaRecords = doc.changes.records.filter { r ->
            val t = r.target as? ChangeTarget.RelationSlot
            t != null && !RelationReader.isAddress(t.slot.known ?: RelationSlotName.ADDRESS_A) && doc.liveness.isLive(r) &&
                r.valueAs(SlotValue.serializer())?.kindKey == kindKey
        }.mapTo(HashSet()) { (it.target as ChangeTarget.RelationSlot).relationKey }
        return doc.cast.relations.filter { r ->
            doc.liveness.isLive(r) && (r.key in viaRecords || listOf(r.between, r.mindA, r.mindB).any { it?.kindKey == kindKey })
        }
    }

    fun settings(doc: ProjectDoc): KindSettings {
        val between = of(doc, RelationCategory.BETWEEN)
        val symmetric = between.filter { it.symmetry?.known != RelationSymmetry.PAIR }
        val pairs = between.filter { it.symmetry?.known == RelationSymmetry.PAIR }
        val minds = of(doc, RelationCategory.MIND)
        return KindSettings(
            betweenTitle = "사이 · ${between.size}",
            betweenNote = "둘에게 같은 사실",
            symmetricTitle = "대칭 · ${symmetric.size}",
            symmetric = symmetric.map { KindRow(it.key, it.name, usage(doc, it.key)) },
            pairTitle = "짝 · ${pairs.size}",
            pairNote = "한쪽 적으면 반대쪽도",
            pairs = pairs.map { KindRow(it.key, "${it.name} ↔ ${it.pairName.orEmpty()}", usage(doc, it.key)) },
            mindTitle = "마음 · ${minds.size}",
            mindNote = "한쪽만 · 반대쪽은 비워 둠",
            minds = minds.map { KindRow(it.key, it.name, usage(doc, it.key)) },
            addLabel = ADD,
        )
    }

    // --- Edits ---

    /** `+ 새 종류`: `사이 / 마음`; for 사이 `대칭 / 짝 이름` (짝 needs the other side's name). */
    fun add(
        doc: ProjectDoc,
        category: RelationCategory,
        name: String,
        ctx: EditContext,
        symmetry: RelationSymmetry? = null,
        pairName: String? = null,
    ): Pair<ProjectDoc, RelationKind> {
        val n = name.trim().nfc()
        require(n.isNotEmpty()) { "kind name is empty" }
        val sym = if (category == RelationCategory.BETWEEN) symmetry ?: RelationSymmetry.SYMMETRIC else null
        if (sym == RelationSymmetry.PAIR) require(!pairName.isNullOrBlank()) { "a pair kind needs the other side's name" }
        val kind = RelationKind(
            key = ctx.newKey(),
            category = category.open(),
            symmetry = sym?.open(),
            name = n,
            pairName = if (sym == RelationSymmetry.PAIR) pairName!!.trim().nfc() else null,
            order = OrderKeys.after(of(doc, category).lastOrNull()?.order),
            updatedAt = ctx.now,
            updatedBy = ctx.by,
        )
        return withKinds(doc, doc.project.relationKinds + kind, ctx) to kind
    }

    /** Rename: every relation shows the new name; records keep the same kind key (TC-13-11). */
    fun rename(doc: ProjectDoc, kindKey: Key, name: String, ctx: EditContext): ProjectDoc =
        update(doc, kindKey, ctx) { it.copy(name = name.trim().nfc()) }

    /** Changing a pair's other name renames the already written other side (TC-13-13). */
    fun renamePair(doc: ProjectDoc, kindKey: Key, pairName: String, ctx: EditContext): ProjectDoc =
        update(doc, kindKey, ctx) { it.copy(pairName = pairName.trim().nfc()) }

    /**
     * Delete (permanent, no trash entry): every slot using the kind keeps the kind's text as
     * `kindFallbackText` with `◇` and `kindKey = null` (TC-13-12). Pair kinds keep `선배 ↔ 후배`.
     */
    fun delete(doc: ProjectDoc, kindKey: Key, ctx: EditContext): ProjectDoc {
        val kind = doc.index.relationKind(kindKey) ?: return doc
        val text = if (kind.symmetry?.known == RelationSymmetry.PAIR && kind.pairName != null) "${kind.name} ↔ ${kind.pairName}" else kind.name
        fun fix(v: SlotValue?): SlotValue? =
            if (v?.kindKey == kindKey) v.copy(kindKey = null, kindFallbackText = text) else v
        val relations = doc.cast.relations.map { r ->
            if (listOf(r.between, r.mindA, r.mindB).none { it?.kindKey == kindKey }) r
            else r.copy(between = fix(r.between), mindA = fix(r.mindA), mindB = fix(r.mindB), updatedAt = ctx.now, updatedBy = ctx.by)
        }
        val records = doc.changes.records.map { r ->
            val t = r.target as? ChangeTarget.RelationSlot ?: return@map r
            if (RelationReader.isAddress(t.slot.known ?: return@map r)) return@map r
            val v = r.valueAs(SlotValue.serializer()) ?: return@map r
            if (v.kindKey != kindKey) r else r.copy(value = ChangeRecord.encodeValue(SlotValue.serializer(), fix(v)!!), updatedAt = ctx.now, updatedBy = ctx.by)
        }
        return withKinds(
            doc.copy(cast = doc.cast.copy(relations = relations), changes = doc.changes.copy(records = records)),
            doc.project.relationKinds.filterNot { it.key == kindKey },
            ctx,
        )
    }

    /**
     * Preview of switching `사이 / 마음` or `대칭 / 짝` while in use (TC-13-20): the affected relations
     * as `리엔 · 오웬`. Nothing changes until [applySwitch].
     */
    fun switchPreview(doc: ProjectDoc, kindKey: Key): KindSwitchPreview {
        val relations = relationsUsing(doc, kindKey)
        return KindSwitchPreview(
            relationKeys = relations.map { it.key },
            pairs = relations.map { r ->
                listOf(r.endA.key, r.endB.key).joinToString(CastCopy.SEP) { doc.index.character(it)?.name ?: doc.index.faction(it)?.name.orEmpty() }
            },
        )
    }

    /**
     * Applies a confirmed switch. Only the kind changes; slots keep their kind key (what an existing
     * value means after the switch is CST Q29, undecided).
     */
    fun applySwitch(
        doc: ProjectDoc,
        kindKey: Key,
        category: RelationCategory,
        ctx: EditContext,
        symmetry: RelationSymmetry? = null,
        pairName: String? = null,
    ): ProjectDoc = update(doc, kindKey, ctx) { k ->
        val sym = if (category == RelationCategory.BETWEEN) symmetry ?: RelationSymmetry.SYMMETRIC else null
        k.copy(
            category = category.open(),
            symmetry = sym?.open(),
            pairName = if (sym == RelationSymmetry.PAIR) pairName?.trim()?.nfc() ?: k.pairName else null,
        )
    }

    private fun update(doc: ProjectDoc, kindKey: Key, ctx: EditContext, change: (RelationKind) -> RelationKind): ProjectDoc =
        withKinds(doc, doc.project.relationKinds.map { if (it.key == kindKey) change(it).copy(updatedAt = ctx.now, updatedBy = ctx.by) else it }, ctx)

    private fun withKinds(doc: ProjectDoc, kinds: List<RelationKind>, ctx: EditContext): ProjectDoc =
        doc.copy(project = doc.project.copy(relationKinds = kinds, updatedAt = ctx.now, updatedBy = ctx.by))
}
