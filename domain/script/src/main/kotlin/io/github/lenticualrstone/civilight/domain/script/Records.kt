package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.AnchorState
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.change.RelationSlotName
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.script.ScriptLayers
import kotlinx.serialization.json.JsonElement

/** `◆` records and `◇` first reveals as the script sees them (SCR-41/42/45). */
object Records {
    data class Created(val doc: ProjectDoc, val recordKey: Key)

    /** Sheet subline `#043 · 1-7 씬 3`. */
    fun sheetSubline(doc: ProjectDoc, blockKey: Key): String? {
        val block = doc.index.block(blockKey) ?: return null
        return "${Labels.number(block.number)} · ${Labels.sceneLocation(doc, block.sceneKey)}"
    }

    /** Row `시작 시점 · 이 블록 · 1-7 #043`. */
    fun startRow(doc: ProjectDoc, blockKey: Key): String? {
        val block = doc.index.block(blockKey) ?: return null
        return "시작 시점 · 이 블록 · ${Labels.episodeCodeOfScene(doc, block.sceneKey)} ${Labels.number(block.number)}".replace("  ", " ")
    }

    /**
     * `기록` (SCR-41): a record with seat block on [blockKey], applying from that block inclusive. A
     * choice block's options cannot be chosen (the model has no option anchor). A term reveal keeps
     * one live record per term: an existing reveal is re-seated (same key) instead of a second one.
     */
    fun createAtBlock(
        doc: ProjectDoc,
        blockKey: Key,
        target: ChangeTarget,
        value: JsonElement?,
        ctx: EditContext,
        note: String? = null,
    ): Created? {
        if (!doc.manuscript.isLiveBlock(blockKey)) return null
        val anchor = Anchor.AtBlock(blockKey)
        if (target is ChangeTarget.TermReveal) {
            existingReveal(doc, target.termKey)?.let { old ->
                val out = doc.mapRecords {
                    if (it.key != old.key) it
                    else it.copy(anchor = anchor, anchorState = AnchorState.NORMAL.open(), originalAnchor = null, updatedAt = ctx.now, updatedBy = ctx.device)
                }
                return Created(out, old.key)
            }
        }
        val record = ChangeRecord(
            key = ctx.newKey(), target = target, value = if (target is ChangeTarget.TermReveal) null else value, note = note,
            anchor = anchor, createdAt = ctx.now, updatedAt = ctx.now, updatedBy = ctx.device,
        )
        return Created(doc.copy(changes = doc.changes.copy(records = doc.changes.records + record)), record.key)
    }

    /** The live `◇ 첫 공개` record of a term. */
    fun existingReveal(doc: ProjectDoc, termKey: Key): ChangeRecord? = doc.changes.records.firstOrNull {
        doc.liveness.isLive(it) && (it.target as? ChangeTarget.TermReveal)?.termKey == termKey
    }

    /** Before `기록` in the `용어 공개` tab: `첫 공개 {old} → {episode} #{n}` when the term already has a reveal elsewhere. */
    fun revealMovePreview(doc: ProjectDoc, termKey: Key, blockKey: Key): String? {
        val old = existingReveal(doc, termKey) ?: return null
        if ((old.anchor as? Anchor.AtBlock)?.blockKey == blockKey) return null
        val block = doc.index.block(blockKey) ?: return null
        return "첫 공개 ${anchorLocation(doc, old.anchor)} → ${Labels.episodeCodeOfScene(doc, block.sceneKey)} ${Labels.number(block.number)}"
    }

    /** `1-7 #043`, `2-5`, `1장`, `예정`. */
    fun anchorLocation(doc: ProjectDoc, anchor: Anchor): String = when (anchor) {
        is Anchor.AtBlock -> doc.index.block(anchor.blockKey)?.let { "${Labels.episodeCodeOfScene(doc, it.sceneKey)} ${Labels.number(it.number)}" } ?: "?"
        is Anchor.UnitStart -> Labels.unitLabel(doc, anchor.unitKey)
        is Anchor.UnitEnd -> Labels.unitLabel(doc, anchor.unitKey)
        is Anchor.WorldEvent -> doc.index.event(anchor.eventKey)?.title ?: "?"
        else -> "예정"
    }

    // --- Labels ---

    /** Kind word of a record: `관계`, a field name, `표시 이름`, `영역`, `첫 공개`. */
    fun kindLabel(doc: ProjectDoc, record: ChangeRecord): String = when (val t = record.target) {
        is ChangeTarget.RelationSlot -> "관계"
        is ChangeTarget.Field -> doc.index.fieldDef(t.fieldDefKey)?.name ?: "필드"
        is ChangeTarget.DisplayName -> "표시 이름"
        is ChangeTarget.RegionShape -> "영역"
        is ChangeTarget.TermReveal -> "첫 공개"
        is ChangeTarget.Unknown -> "변화"
    }

    private fun endName(doc: ProjectDoc, key: Key): String = Labels.speakerName(doc, key) ?: doc.index.faction(key)?.name ?: "?"

    /**
     * Subject of a relation record, from the side whose slot it is: `리엔 → 하르트` for 리엔's 마음 or
     * 호칭, `리엔 · 하르트` for 사이. [possessive] gives the 시간 tab form `리엔의 마음 → 하르트`.
     */
    fun relationSubject(doc: ProjectDoc, target: ChangeTarget.RelationSlot, possessive: Boolean): String {
        val relation = doc.index.relation(target.relationKey) ?: return "?"
        val a = endName(doc, relation.endA.key)
        val b = endName(doc, relation.endB.key)
        return when (target.slot.known) {
            RelationSlotName.BETWEEN -> if (possessive) "$a · $b 사이" else "$a · $b"
            RelationSlotName.MIND_A -> if (possessive) "${a}의 마음 → $b" else "$a → $b"
            RelationSlotName.MIND_B -> if (possessive) "${b}의 마음 → $a" else "$b → $a"
            RelationSlotName.ADDRESS_A -> if (possessive) "${a}의 호칭 → $b" else "$a → $b"
            RelationSlotName.ADDRESS_B -> if (possessive) "${b}의 호칭 → $a" else "$b → $a"
            null -> "$a · $b"
        }
    }

    /** Subject of any record (`리엔 → 하르트`, `리엔`, `오리지늄 등불`). */
    fun subject(doc: ProjectDoc, record: ChangeRecord, possessive: Boolean = false): String = when (val t = record.target) {
        is ChangeTarget.RelationSlot -> relationSubject(doc, t, possessive)
        is ChangeTarget.Field -> doc.index.character(t.entityKey)?.name ?: doc.index.place(t.entityKey)?.name
            ?: doc.index.term(t.entityKey)?.name ?: doc.index.faction(t.entityKey)?.name ?: "?"
        is ChangeTarget.DisplayName -> Labels.speakerName(doc, t.characterKey) ?: "?"
        is ChangeTarget.RegionShape -> doc.index.region(t.regionKey)?.second?.name ?: "?"
        is ChangeTarget.TermReveal -> doc.index.term(t.termKey)?.name ?: "?"
        is ChangeTarget.Unknown -> "?"
    }

    /** `◆ 관계 · 리엔 → 하르트` or `첫 공개 · 오리지늄 등불` (scene split preview, 속성 tab). */
    fun shortTitle(doc: ProjectDoc, record: ChangeRecord): String =
        if (record.isReveal) "첫 공개 · ${subject(doc, record)}" else "◆ ${kindLabel(doc, record)} · ${subject(doc, record)}"

    // --- Point-seat lines (SCR-42) ---

    /** `◆ 1-7부터 · 근거 블록 없음` + `블록에 잇기`, drawn above the first block row of [sceneKey]. */
    data class PointLine(val recordKey: Key, val text: String, val atEnd: Boolean) {
        val button: String get() = "블록에 잇기"
    }

    /**
     * Point-seat records shown in the `시간` tab's `이 씬 안` list of [sceneKey]: a `unitStart` record
     * appears in the first live scene of its unit (`[PROPOSED]`), a `unitEnd` record at the end of
     * the unit's last live scene (`ED`). World-event records never appear; all lines follow the
     * `변화 표식` layer.
     */
    fun pointLines(doc: ProjectDoc, sceneKey: Key, layers: ScriptLayers = ScriptLayers()): List<PointLine> {
        if (!layers.marks) return emptyList()
        return doc.changes.records.filter { doc.liveness.isLive(it) }.mapNotNull { r ->
            val glyph = if (r.isReveal) "◇" else "◆"
            when (val a = r.anchor) {
                is Anchor.UnitStart -> if (Anchors.firstLiveSceneOf(doc, a.unitKey) == sceneKey) {
                    PointLine(r.key, "$glyph ${Labels.unitLabel(doc, a.unitKey)}부터 · 근거 블록 없음", atEnd = false)
                } else null
                is Anchor.UnitEnd -> if (lastLiveSceneOf(doc, a.unitKey) == sceneKey) {
                    PointLine(r.key, "$glyph ${Labels.unitLabel(doc, a.unitKey)} 끝부터 · 근거 블록 없음", atEnd = true)
                } else null
                else -> null
            }
        }
    }

    private fun lastLiveSceneOf(doc: ProjectDoc, unitKey: Key): Key? {
        val unit = doc.index.unit(unitKey) ?: return null
        if (unit.level == doc.project.structure.sceneLevel) return unitKey.takeIf { doc.manuscript.isLiveUnit(it) }
        return doc.manuscript.liveScenes.lastOrNull { s -> doc.index.ancestors(s).any { it.key == unitKey } }?.key
    }

    /** One mark on a block (SCR-42): `◆` or `◇`, dotted when attached to something deleted. */
    data class Mark(val recordKey: Key, val glyph: String, val dotted: Boolean, val targetDeleted: Boolean)

    /** Marks drawn on a block; empty when the `변화 표식` layer is off. */
    fun marksOn(doc: ProjectDoc, blockKey: Key, layers: ScriptLayers = ScriptLayers()): List<Mark> {
        if (!layers.marks) return emptyList()
        return doc.recordsAnchoredOn(blockKey).map { r ->
            val targetDeleted = !doc.liveness.isTargetLive(r.target)
            Mark(r.key, if (r.isReveal) "◇" else "◆", Anchors.isDotted(r) || targetDeleted, targetDeleted)
        }
    }
}
