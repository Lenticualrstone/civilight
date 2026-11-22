package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.SlotValue
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.change.RelationSlotName
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.isLinkBeforeReveal
import io.github.lenticualrstone.civilight.model.doc.recordBeforeBlock
import io.github.lenticualrstone.civilight.model.doc.recordsHere
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import kotlinx.serialization.builtins.serializer
import java.time.ZoneId

/** Side panel content rules (SCR-32/33/34) that do not depend on layout. */
object Panels {
    /** Word of a relation slot value: the relation kind's name, else its fallback text. */
    fun slotWord(doc: ProjectDoc, value: SlotValue?): String? =
        value?.kindKey?.let { doc.index.relationKind(it)?.name } ?: value?.kindFallbackText

    private fun recordWord(doc: ProjectDoc, record: ChangeRecord): String? = when (val t = record.target) {
        is ChangeTarget.RelationSlot -> when (t.slot.known) {
            RelationSlotName.ADDRESS_A, RelationSlotName.ADDRESS_B -> record.valueAs(String.serializer())
            else -> slotWord(doc, record.valueAs(SlotValue.serializer()))
        }
        is ChangeTarget.DisplayName -> record.valueAs(String.serializer())
        else -> record.value?.toString()
    }

    private fun baseWord(doc: ProjectDoc, target: ChangeTarget.RelationSlot): String? {
        val r = doc.index.relation(target.relationKey) ?: return null
        return when (target.slot.known) {
            RelationSlotName.BETWEEN -> slotWord(doc, r.between)
            RelationSlotName.MIND_A -> slotWord(doc, r.mindA)
            RelationSlotName.MIND_B -> slotWord(doc, r.mindB)
            RelationSlotName.ADDRESS_A -> r.addressA
            RelationSlotName.ADDRESS_B -> r.addressB
            null -> null
        }
    }

    /**
     * Block-level time rule (SCR-33, rule 11 §2): the value just before the block (`만나기 전` when
     * the slot was never set); a change made by the block itself reads `{before} → {after} ◆ 여기서`.
     */
    fun relationStateAt(doc: ProjectDoc, target: ChangeTarget.RelationSlot, blockKey: Key): String {
        val before = doc.recordBeforeBlock(target, blockKey)?.let { recordWord(doc, it) } ?: baseWord(doc, target) ?: "만나기 전"
        val here = doc.recordsHere(blockKey).lastOrNull { it.target.sameAs(target) } ?: return before
        return "$before → ${recordWord(doc, here) ?: "만나기 전"} ◆ 여기서"
    }

    /**
     * `▲ 공개 전 사용` links of a block (margin `▲`, never hidden by layers): term links before
     * the term's first reveal in manuscript order. Memos and direction cues never raise it.
     */
    fun preRevealLinks(doc: ProjectDoc, block: Block): List<Key> {
        if (block.kind.known == BlockKind.MEMO || block.kind.known == BlockKind.DIRECTION) return emptyList()
        return (block.runs + block.options.flatMap { it.runs }).filterIsInstance<TextRun.Link>()
            .filter { doc.isLinkBeforeReveal(it.target.key, block.key) }.map { it.linkKey }
    }

    /** 속성 tab speaker field for a deleted speaker: `지운 인물 · 노아` and its three buttons (SCR-57). */
    data class DeletedSpeakerField(val text: String, val buttons: List<String>)

    fun deletedSpeakerField(doc: ProjectDoc, block: Block): DeletedSpeakerField? {
        val key = block.speakerKey ?: return null
        val c = doc.index.character(key)
        return when {
            c != null && c.deletedAt != null -> DeletedSpeakerField("지운 인물 · ${c.name}", listOf("화자 바꾸기", "되살리기", "이대로 두기"))
            c == null -> doc.index.nameTag(key)?.let { DeletedSpeakerField("영구 삭제 · ${it.name}", listOf("화자 바꾸기", "이대로 두기")) }
            else -> null
        }
    }

    /** `시간` tab section title `고른 블록 · #043`. */
    fun selectedBlockTitle(block: Block): String = "고른 블록 · ${Labels.number(block.number)}"

    /** Orphaned record card subline `근거 #043 지워짐 · #044로 옮겨 옴` and the trash note. */
    fun orphanCard(doc: ProjectDoc, record: ChangeRecord): Pair<String, String?>? {
        val original = (record.originalAnchor as? io.github.lenticualrstone.civilight.model.change.Anchor.AtBlock)?.let { doc.index.block(it.blockKey) } ?: return null
        val now = (record.anchor as? io.github.lenticualrstone.civilight.model.change.Anchor.AtBlock)?.let { doc.index.block(it.blockKey) }
        val line = "근거 ${Labels.number(original.number)} 지워짐 · " + (now?.let { "${Labels.number(it.number)}로 옮겨 옴" } ?: "예정으로 옮겨 옴")
        val note = if (original.deletedAt != null) "${Labels.number(original.number)} · 휴지통 · 30일 · 되살리면 다시 이어짐" else null
        return line to note
    }

    /** Orphan card buttons (SCR-43). */
    val ORPHAN_BUTTONS: List<String> = listOf("다른 블록에 잇기", "이대로 두기", "기록 지우기")

    /** 속성 tab `보이스 ID` value (null when the setting is off or there is no ID). */
    fun voiceField(doc: ProjectDoc, block: Block, zone: ZoneId): String? =
        if (!doc.project.script.voiceIdEnabled) null else VoiceIds.stateLabel(doc, block, zone)
}
