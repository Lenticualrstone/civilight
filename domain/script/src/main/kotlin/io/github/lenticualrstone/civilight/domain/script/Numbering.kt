package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.VoiceMode

/**
 * Block numbers per numbering unit (SCR-21, §4.1): issued from the unit's counter, which only
 * grows; numbers of deleted blocks are never reissued.
 */
object Numbering {
    /** The next number the unit would issue. Never below any number already used in the unit. */
    fun nextNumber(doc: ProjectDoc, numberingUnitKey: Key): Int {
        val stored = if (numberingUnitKey == doc.project.key) doc.project.counters.nextBlockNumber
        else doc.index.unit(numberingUnitKey)?.nextBlockNumber
        val used = doc.episodes[numberingUnitKey]?.blocks?.maxOfOrNull { it.number } ?: 0
        return maxOf(stored ?: 1, used + 1)
    }

    /** Issues [count] numbers from the unit's counter; returns the new doc and the numbers. */
    fun issue(doc: ProjectDoc, numberingUnitKey: Key, count: Int): Pair<ProjectDoc, List<Int>> {
        if (count == 0) return doc to emptyList()
        val first = nextNumber(doc, numberingUnitKey)
        val numbers = (first until first + count).toList()
        return setCounter(doc, numberingUnitKey, first + count) to numbers
    }

    private fun setCounter(doc: ProjectDoc, numberingUnitKey: Key, next: Int): ProjectDoc =
        if (numberingUnitKey == doc.project.key) {
            doc.copy(project = doc.project.copy(counters = doc.project.counters.copy(nextBlockNumber = next)))
        } else {
            doc.updateUnit(numberingUnitKey) { it.copy(nextBlockNumber = next) }
        }

    /** Live blocks of a numbering unit in manuscript order. */
    fun liveBlocksOfUnit(doc: ProjectDoc, numberingUnitKey: Key): List<Block> =
        doc.manuscript.liveBlocks.filter { doc.numberingUnitOf(it) == numberingUnitKey }

    // --- SCR-22 번호 다시 매기기 ---

    /** One block whose number changes. */
    data class RenumberRow(
        val blockKey: Key,
        val oldNumber: Int,
        val newNumber: Int,
        /** `씬 1 · 나레이션`, `씬 4 · 하르트`. */
        val where: String,
        /** The fixed voice ID, when the block has one. */
        val fixedVoiceId: String?,
    ) {
        /** `#056 → #020`. */
        val change: String get() = "${Labels.number(oldNumber)} → ${Labels.number(newNumber)}"

        /** `보이스 HRT_0107_051 · 고정 · 그대로` for a fixed ID. */
        val voiceLine: String? get() = fixedVoiceId?.let { "보이스 $it · 고정 · 그대로" }
    }

    /** The preview sheet; nothing changes before [apply]. */
    data class RenumberPreview(
        val numberingUnitKey: Key,
        val title: String,
        val total: Int,
        val rows: List<RenumberRow>,
        /** How many rows the sheet lists one by one; the rest go to [restLine]. */
        val shownRows: Int,
    ) {
        val subtitle: String get() = "원고 순서대로 #001부터"
        val changed: Int get() = rows.size
        val unchanged: Int get() = total - rows.size

        /** The three figures `{N} 블록 · {k} 바뀌는 번호 · {N−k} 그대로`. */
        val figures: List<Pair<String, String>>
            get() = listOf(total.toString() to "블록", changed.toString() to "바뀌는 번호", unchanged.toString() to "그대로")

        val sectionTitle: String get() = "바뀌는 번호 · $changed"
        val listedRows: List<RenumberRow> get() = rows.take(shownRows)

        /** `외 3개 · #055 → #038 · #052 → #046 고정 · …`, or null when every row is listed. */
        val restLine: String?
            get() {
                val rest = rows.drop(shownRows)
                if (rest.isEmpty()) return null
                return (listOf("외 ${rest.size}개") + rest.map { r -> r.change + if (r.fixedVoiceId != null) " 고정" else "" })
                    .joinToString(" · ")
            }

        val fixedCount: Int get() = rows.count { it.fixedVoiceId != null }
        val footer: List<String>
            get() = listOf("고정 보이스 ID ${fixedCount}개는 그대로", "번호만 다름 · ◇ 없음 · 변화 기록 · 블록 ID 그대로")
        val buttons: List<String> get() = listOf("취소", "다시 매기기")
    }

    /**
     * Preview of `번호 다시 매기기 · {episode}`: live blocks get 1…N in manuscript order (scene order,
     * then block order). Rows are listed in manuscript order; the first [shownRows] individually
     * (`ED`, see the status file).
     */
    fun renumberPreview(doc: ProjectDoc, numberingUnitKey: Key, shownRows: Int = 4): RenumberPreview {
        val blocks = liveBlocksOfUnit(doc, numberingUnitKey)
        val rows = blocks.mapIndexedNotNull { i, b ->
            val newNumber = i + 1
            if (b.number == newNumber) null
            else RenumberRow(
                blockKey = b.key, oldNumber = b.number, newNumber = newNumber,
                where = "씬 ${Labels.sceneOrdinal(doc, b.sceneKey) ?: "?"} · ${Labels.blockLabel(doc, b)}",
                fixedVoiceId = b.voice?.takeIf { it.mode.known == VoiceMode.FIXED }?.fixedId,
            )
        }
        val code = if (numberingUnitKey == doc.project.key) "" else Labels.unitCode(doc, numberingUnitKey)
        return RenumberPreview(numberingUnitKey, "번호 다시 매기기 · $code".trimEnd(' ', '·'), blocks.size, rows, shownRows)
    }

    /**
     * Applies renumbering: live blocks of the unit get 1…N in manuscript order. Keys, records,
     * acknowledgements, voice states and the counter are unchanged (Q-07 default); automatic voice
     * IDs follow the new numbers because they are computed.
     */
    fun renumber(doc: ProjectDoc, numberingUnitKey: Key, ctx: EditContext): ProjectDoc {
        val updates = liveBlocksOfUnit(doc, numberingUnitKey).mapIndexedNotNull { i, b ->
            if (b.number == i + 1) null else b.key to b.copy(number = i + 1, updatedAt = ctx.now, updatedBy = ctx.device)
        }.toMap()
        return doc.replaceBlocks(updates)
    }
}
