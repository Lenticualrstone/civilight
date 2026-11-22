package io.github.lenticualrstone.civilight.domain.story.structure

import io.github.lenticualrstone.civilight.domain.story.scene.EditContext
import io.github.lenticualrstone.civilight.domain.story.text.BlockText
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Level names (STO-02) and `오늘 쓴 글자` (STO-06). */
object StructureEdit {
    /**
     * `바꾸기 ›` on `단계 이름`: renames one level; keys, order, codes and data stay. A blank name is
     * refused (the doc is returned unchanged).
     */
    fun renameLevel(doc: ProjectDoc, levelIndex: Int, name: String, ctx: EditContext? = null): ProjectDoc {
        val levels = doc.project.structure.levels
        val clean = name.trim().nfc()
        if (levelIndex !in levels.indices || clean.isEmpty()) return doc
        val structure = doc.project.structure.copy(levels = levels.toMutableList().also { it[levelIndex] = clean })
        val project = doc.project.copy(structure = structure).let { p -> ctx?.let { p.copy(updatedAt = it.now, updatedBy = it.device) } ?: p }
        return doc.copy(project = project)
    }

    /** `ED` (Q1): the number of levels can change only while no unit below the top level exists. */
    fun canChangeLevelCount(doc: ProjectDoc): Boolean = doc.story.units.none { it.level > 0 && doc.liveness.isUnitLive(it.key) }

    /**
     * `오늘 쓴 글자` (`ED`, Q4): over live blocks, `max(0, length now − length at the start of [today])`
     * from the day's baselines; a block created today without a baseline counts in full, any other
     * block without a baseline counts 0. Lengths use the `N자` rule.
     */
    fun todayChars(doc: ProjectDoc, today: LocalDate, zone: ZoneId = ZoneId.of("Asia/Seoul")): Int {
        val baselines = doc.app.activity.dayBaselines.filter { it.localDate == today }.associateBy { it.blockKey }
        return doc.manuscript.liveBlocks.sumOf { b ->
            val now = BlockText.charCount(b)
            val base = baselines[b.key]?.lengthAtDayStart
            when {
                base != null -> maxOf(0, now - base)
                Instant.ofEpochMilli(b.createdAt).atZone(zone).toLocalDate() == today -> now
                else -> 0
            }
        }
    }
}
