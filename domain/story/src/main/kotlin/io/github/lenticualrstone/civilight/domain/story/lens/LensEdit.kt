package io.github.lenticualrstone.civilight.domain.story.lens

import io.github.lenticualrstone.civilight.domain.story.scene.EditContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.LensState
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.cast.SlotValue
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.change.RelationSlotName
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.position.LensPoint
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

/** A stacked button of the edit sheet: label and its small second line. */
data class EditButton(val label: String, val sub: String)

/** The past-lens banner `1-5 시점 값 고치는 중 | 고치면 1-5부터 적용` with `최신으로`. */
data class LensBanner(val text: String, val action: String = "최신으로")

/** What the field editor shows when a value is edited under the lens (STO-15). */
sealed interface EditChoice {
    /** No change history at 최신: a plain edit, no buttons. */
    data object Plain : EditChoice

    /**
     * `이 값 고치기` (primary, default) overwrites [overwriteRecordKey] (null = the base value);
     * `여기서부터 바뀜` creates a new `◆` at [newAnchor].
     */
    data class TwoButtons(
        val overwrite: EditButton,
        val fromHere: EditButton,
        val overwriteRecordKey: Key?,
        val newAnchor: Anchor,
        val banner: LensBanner?,
    ) : EditChoice
}

/** Editing under the lens: the two-button rules (STO-15, R11 §1, IDX). */
object LensEdit {
    /**
     * At 최신 a field with history gets the two buttons; at a past (or future) lens every field does.
     * The new `◆` goes right after the 집필 선 at 최신 (`UnitEnd` of the 집필 선 scene, applied at
     * 최신, unseen by that scene's blocks, Q12) and at the start of the viewed unit at a past lens
     * (`1-5 #—`).
     */
    fun choice(doc: ProjectDoc, target: ChangeTarget, lens: LensPoint): EditChoice {
        val view = LensView(doc)
        val history = view.history(target)
        if (lens == LensPoint.Latest && history.isEmpty()) return EditChoice.Plain
        val inForce = view.recordAt(target, lens)
        val overwriteSub = inForce?.let { r ->
            val at = view.anchorLabel(r.anchor)
            "$at 기록을 고침 · ${at}부터 새 값"
        } ?: "기본값을 고침"
        val anchor = newAnchor(doc, lens)
        return when (lens) {
            LensPoint.Latest -> {
                val scene = view.writingLineScene
                EditChoice.TwoButtons(
                    overwrite = EditButton("이 값 고치기", overwriteSub),
                    fromHere = EditButton(
                        "여기서부터 바뀜 · ${view.latestCode()}",
                        scene?.let { "새 ◆ · 집필 선 ${view.codes.sceneRef(it)} 뒤" } ?: "새 ◆",
                    ),
                    overwriteRecordKey = inForce?.key,
                    newAnchor = anchor,
                    banner = null,
                )
            }
            is LensPoint.UnitEnd -> {
                val code = view.pointCode(lens)
                EditChoice.TwoButtons(
                    overwrite = EditButton("이 값 고치기", overwriteSub),
                    fromHere = EditButton("여기서부터 바뀜 · $code", "새 ◆ · $code #—"),
                    overwriteRecordKey = inForce?.key,
                    newAnchor = anchor,
                    banner = LensBanner("$code 시점 값 고치는 중 | 고치면 ${code}부터 적용"),
                )
            }
        }
    }

    /** Where `여기서부터 바뀜` anchors its new record. */
    fun newAnchor(doc: ProjectDoc, lens: LensPoint): Anchor = when (lens) {
        LensPoint.Latest -> doc.manuscript.writingLineSceneKey?.let { Anchor.UnitEnd(it) }
            ?: doc.index.children(null).firstOrNull()?.let { Anchor.UnitStart(it.key) }
            ?: Anchor.Unplaced
        is LensPoint.UnitEnd -> Anchor.UnitStart(lens.unitKey)
    }

    /** `이 값 고치기`: overwrites the record in force at the lens point (no new record), else the base value. */
    fun overwrite(doc: ProjectDoc, target: ChangeTarget, lens: LensPoint, value: JsonElement?, ctx: EditContext): ProjectDoc {
        val inForce = LensView(doc).recordAt(target, lens) ?: return editBase(doc, target, value, ctx)
        val records = doc.changes.records.map {
            if (it.key == inForce.key) it.copy(value = value, updatedAt = ctx.now, updatedBy = ctx.device) else it
        }
        return doc.copy(changes = doc.changes.copy(records = records))
    }

    /** `여기서부터 바뀜`: a new `◆` record with [value] at [newAnchor]. */
    fun changeFromHere(doc: ProjectDoc, target: ChangeTarget, lens: LensPoint, value: JsonElement?, ctx: EditContext, note: String? = null): ProjectDoc {
        val record = ChangeRecord(
            key = ctx.newKey(), target = target, value = value, note = note, anchor = newAnchor(doc, lens),
            createdAt = ctx.now, updatedAt = ctx.now, updatedBy = ctx.device,
        )
        return doc.copy(changes = doc.changes.copy(records = doc.changes.records + record))
    }

    /**
     * Writes a base value (no record in force): character/place/term/faction field values,
     * `displayNameBase`, relation base slots. Other targets have no base and are left unchanged.
     */
    fun editBase(doc: ProjectDoc, target: ChangeTarget, value: JsonElement?, ctx: EditContext): ProjectDoc {
        val v = value?.takeUnless { it is JsonNull }
        return when (target) {
            is ChangeTarget.Field -> {
                val fv = v?.let { CivilightJson.decodeElement(FieldValue.serializer(), it) }
                fun <T> Map<Key, FieldValue>.with(): Map<Key, FieldValue> = if (fv == null) this - target.fieldDefKey else this + (target.fieldDefKey to fv)
                when (target.entityKind.known) {
                    EntityKind.CHARACTER -> doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.map {
                        if (it.key == target.entityKey) it.copy(fields = it.fields.with<Unit>(), updatedAt = ctx.now, updatedBy = ctx.device) else it
                    }))
                    EntityKind.PLACE -> doc.copy(world = doc.world.copy(places = doc.world.places.map {
                        if (it.key == target.entityKey) it.copy(fields = it.fields.with<Unit>(), updatedAt = ctx.now, updatedBy = ctx.device) else it
                    }))
                    EntityKind.TERM -> doc.copy(world = doc.world.copy(terms = doc.world.terms.map {
                        if (it.key == target.entityKey) it.copy(fields = it.fields.with<Unit>(), updatedAt = ctx.now, updatedBy = ctx.device) else it
                    }))
                    EntityKind.FACTION -> doc.copy(world = doc.world.copy(factions = doc.world.factions.map {
                        if (it.key == target.entityKey) it.copy(fields = it.fields.with<Unit>(), updatedAt = ctx.now, updatedBy = ctx.device) else it
                    }))
                    null -> doc
                }
            }
            is ChangeTarget.DisplayName -> doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.map {
                if (it.key == target.characterKey) it.copy(displayNameBase = v?.let { e -> CivilightJson.decodeElement(String.serializer(), e) }, updatedAt = ctx.now, updatedBy = ctx.device) else it
            }))
            is ChangeTarget.RelationSlot -> doc.copy(cast = doc.cast.copy(relations = doc.cast.relations.map { r ->
                if (r.key != target.relationKey) return@map r
                val text = { v?.let { CivilightJson.decodeElement(String.serializer(), it) } }
                val slot = { v?.let { CivilightJson.decodeElement(SlotValue.serializer(), it) } }
                when (target.slot.known) {
                    RelationSlotName.BETWEEN -> r.copy(between = slot())
                    RelationSlotName.MIND_A -> r.copy(mindA = slot())
                    RelationSlotName.MIND_B -> r.copy(mindB = slot())
                    RelationSlotName.ADDRESS_A -> r.copy(addressA = text())
                    RelationSlotName.ADDRESS_B -> r.copy(addressB = text())
                    null -> r
                }.copy(updatedAt = ctx.now, updatedBy = ctx.device)
            }))
            else -> doc
        }
    }

    /** `지금 값 | 등불 결사 · 1-3부터` (the base value has no `부터` part). */
    fun currentLine(doc: ProjectDoc, target: ChangeTarget, lens: LensPoint): String {
        val view = LensView(doc)
        val values = ValueText(doc)
        val r = view.recordAt(target, lens) ?: return "지금 값 | ${values.base(target)}"
        return "지금 값 | ${values.of(target, r.value)} · ${view.anchorLabel(r.anchor)}부터"
    }

    /** `1-1 없음 · 1-3 등불 결사 · 예정 2-5 등불지기장`. */
    fun historyLine(doc: ProjectDoc, target: ChangeTarget): String {
        val view = LensView(doc)
        val values = ValueText(doc)
        return view.history(target).joinToString(" · ") { r ->
            val point = if (view.status(r) == RecordStatus.PLANNED) "예정 ${view.anchorPoint(r.anchor)}" else view.anchorLabel(r.anchor)
            "$point ${values.of(target, r.value)}"
        }
    }

    /** `저장 시 ◆ 변화 기록 · 1-5 #— · 이전 값 ‘등불 결사’` for `여기서부터 바뀜` at a past lens. */
    fun footnote(doc: ProjectDoc, target: ChangeTarget, lens: LensPoint): String {
        val view = LensView(doc)
        val values = ValueText(doc)
        val previous = view.recordAt(target, lens)?.let { values.of(target, it.value) } ?: values.base(target)
        val point = when (val a = newAnchor(doc, lens)) {
            is Anchor.UnitStart -> "${view.codes.unitLabel(a.unitKey)} #—"
            else -> view.anchorLabel(a)
        }
        return "저장 시 ◆ 변화 기록 · $point · 이전 값 ‘$previous’"
    }
}

/**
 * The script has no lens (STO-16, R11 §3): entering stores the current lens, leaving restores it;
 * `렌즈를 이 씬으로 맞추기` replaces the stored value with the scene's end.
 */
object ScriptLens {
    fun enter(state: LensState): LensState = state.copy(savedForScript = state.point)

    fun setLensToScene(state: LensState, sceneKey: Key): LensState = state.copy(savedForScript = LensPoint.UnitEnd(sceneKey))

    fun leave(state: LensState): LensState = LensState(point = state.savedForScript ?: state.point, savedForScript = null)
}
