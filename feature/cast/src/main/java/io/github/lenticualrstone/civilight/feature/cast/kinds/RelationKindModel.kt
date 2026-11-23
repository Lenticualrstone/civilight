package io.github.lenticualrstone.civilight.feature.cast.kinds

import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.relation.KindSwitchPreview
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationKinds
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.RelationCategory
import io.github.lenticualrstone.civilight.model.cast.RelationSymmetry
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc
import kotlinx.serialization.Serializable

/** The shape of a kind: `사이 / 마음`, and for 사이 `대칭 / 짝 이름` (with the other side's name). */
@Serializable
data class KindShape(
    val category: RelationCategory = RelationCategory.BETWEEN,
    val symmetry: RelationSymmetry = RelationSymmetry.SYMMETRIC,
    val name: String = "",
    val pairName: String = "",
) {
    val isPair: Boolean get() = category == RelationCategory.BETWEEN && symmetry == RelationSymmetry.PAIR
}

/** One kind opened from `관계 종류` (not drawn): its names, shape and usage. */
data class KindEditView(
    val key: Key,
    /** `경계`, or `선배 ↔ 후배`. */
    val title: String,
    /** `쓰임 1`. */
    val usage: String,
    val shape: KindShape,
)

/**
 * Pure model of `프로젝트 설정 › 관계 종류` (CST-32) over `domain:cast` [RelationKinds]: new kinds,
 * rename, pair rename (renames the written other side, TC-13-13), delete (slots keep the text with
 * `◇`, TC-13-12), and switching `사이 / 마음` or `대칭 / 짝` only after its preview (TC-13-20).
 */
object RelationKindModel {
    fun edit(doc: ProjectDoc, kindKey: Key): KindEditView? {
        val kind = doc.index.relationKind(kindKey) ?: return null
        val category = kind.category.known ?: return null
        val shape = KindShape(category, kind.symmetry?.known ?: RelationSymmetry.SYMMETRIC, kind.name, kind.pairName.orEmpty())
        return KindEditView(
            key = kind.key,
            title = if (shape.isPair) "${kind.name} ↔ ${kind.pairName.orEmpty()}" else kind.name,
            usage = CastUiCopy.usage(RelationKinds.usage(doc, kind.key)),
            shape = shape,
        )
    }

    /** `넣기` needs a name, and a pair kind also the other side's name. */
    fun canAdd(shape: KindShape): Boolean = shape.name.isNotBlank() && (!shape.isPair || shape.pairName.isNotBlank())

    fun add(doc: ProjectDoc, shape: KindShape, ctx: EditContext): ProjectDoc = if (!canAdd(shape)) doc else RelationKinds.add(
        doc,
        shape.category,
        shape.name,
        ctx,
        symmetry = if (shape.category == RelationCategory.BETWEEN) shape.symmetry else null,
        pairName = shape.pairName.takeIf { shape.isPair },
    ).first

    /** The names changed (`저장` enabled). */
    fun namesChanged(before: KindShape, after: KindShape): Boolean =
        after.name.trim().nfc() != before.name || (before.isPair && after.pairName.trim().nfc() != before.pairName)

    /** Rename and, for a pair, the other side's name (TC-13-11, TC-13-13); blank names change nothing. */
    fun rename(doc: ProjectDoc, kindKey: Key, after: KindShape, ctx: EditContext): ProjectDoc {
        val before = edit(doc, kindKey)?.shape ?: return doc
        var next = doc
        if (after.name.isNotBlank() && after.name.trim().nfc() != before.name) next = RelationKinds.rename(next, kindKey, after.name, ctx)
        if (before.isPair && after.pairName.isNotBlank() && after.pairName.trim().nfc() != before.pairName) {
            next = RelationKinds.renamePair(next, kindKey, after.pairName, ctx)
        }
        return next
    }

    /** The shape switch differs from the kind as stored (category, or symmetry of a 사이). */
    fun shapeChanged(before: KindShape, after: KindShape): Boolean =
        before.category != after.category || (after.category == RelationCategory.BETWEEN && before.symmetry != after.symmetry)

    /** A switch to `짝` needs the other side's name. */
    fun canSwitch(before: KindShape, after: KindShape): Boolean = shapeChanged(before, after) && (!after.isPair || after.pairName.isNotBlank())

    /** What the switch would touch, before anything changes (TC-13-20: `바뀌는 관계 1`, `리엔 · 오웬`). */
    fun switchPreview(doc: ProjectDoc, kindKey: Key): KindSwitchPreview = RelationKinds.switchPreview(doc, kindKey)

    /** Applies a confirmed switch; only the kind changes (slots keep their kind key, CST Q29). */
    fun applySwitch(doc: ProjectDoc, kindKey: Key, after: KindShape, ctx: EditContext): ProjectDoc = RelationKinds.applySwitch(
        doc,
        kindKey,
        after.category,
        ctx,
        symmetry = if (after.category == RelationCategory.BETWEEN) after.symmetry else null,
        pairName = after.pairName.takeIf { after.isPair },
    )

    fun delete(doc: ProjectDoc, kindKey: Key, ctx: EditContext): ProjectDoc = RelationKinds.delete(doc, kindKey, ctx)
}
