package io.github.lenticualrstone.civilight.domain.library.newproject

import io.github.lenticualrstone.civilight.domain.library.common.UnitCodes
import io.github.lenticualrstone.civilight.domain.library.rating.NewProjectProtections
import io.github.lenticualrstone.civilight.model.app.RatingDefaults
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.ProjectFormat
import io.github.lenticualrstone.civilight.model.project.WorldTimeMode
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.story.StoryUnit

/** One `시작해 볼 것` item (LIB-13); items complete by fact. */
data class ChecklistItem(val id: Id, val title: String, val sub: String, val done: Boolean) {
    enum class Id { PROTAGONIST, LINKED_RECORDS, TERM, FIRST_LINE }
}

/** The 만든 직후 block on the new project's home (LIB-13). */
data class StartBlock(
    /** `연재 장편 · 장 › 에피소드 › 씬 · 1200년` (+ ` · 숨김 · 끔`). */
    val summary: String,
    /** `1장 · 에피소드 1-1 · 씬 1`, or null without a manuscript. */
    val quickWrite: String?,
    val items: List<ChecklistItem>,
) {
    val doneCount: Int get() = items.count { it.done }

    /** `시작해 볼 것 · <done>/4`. */
    val header: String get() = "시작해 볼 것 · $doneCount/$TOTAL"
    val dismiss: String get() = "목록 닫기"
    val writeButton: String get() = "쓰기"

    companion object {
        const val TOTAL = 4
        const val TODO_COPY_LIB13_PROTAGONIST_SUB = "TODO_COPY_LIB13_PROTAGONIST_SUB"

        fun formatLabel(format: ProjectFormat?): String = when (format) {
            ProjectFormat.SERIAL, null -> "연재 장편"
            ProjectFormat.SHORT -> "단편"
            ProjectFormat.SETTING_ONLY -> "설정만"
        }

        fun of(doc: ProjectDoc, recorder: RecorderLibrary, protections: NewProjectProtections? = null): StartBlock {
            val p = doc.project
            val year = p.worldTime.storyStartYear?.takeIf { p.worldTime.mode.known != WorldTimeMode.NONE }?.let { "${it}년" }
            val summary = listOfNotNull(
                formatLabel(p.format.known),
                p.structure.levels.joinToString(" › "),
                year,
                protections?.summaryMarker,
            ).joinToString(" · ")

            val firstCharacter = doc.cast.characters.filter { it.deletedAt == null }.minByOrNull { it.order }
            val linked = recorder.recordsLinkedTo(doc.key).filter { it.deletedAt == null }
            val items = buildList {
                add(
                    ChecklistItem(
                        ChecklistItem.Id.PROTAGONIST, "주인공 만들기",
                        firstCharacter?.let { "${it.name} · 완료" } ?: TODO_COPY_LIB13_PROTAGONIST_SUB, firstCharacter != null,
                    ),
                )
                if (linked.isNotEmpty()) {
                    add(
                        ChecklistItem(
                            ChecklistItem.Id.LINKED_RECORDS, "연결된 기록 ${linked.size}개 정리하기", "기록함",
                            linked.all { !RecordStates.isUnsorted(recorder, it) },
                        ),
                    )
                }
                add(ChecklistItem(ChecklistItem.Id.TERM, "세계 용어 하나 적기", "공개 시점 포함", doc.world.terms.any { it.deletedAt == null }))
                add(
                    ChecklistItem(
                        ChecklistItem.Id.FIRST_LINE, "첫 대사 쓰기", "대사 블록 · 화자 고르기",
                        doc.manuscript.liveBlocks.any { it.kind.known == BlockKind.DIALOGUE },
                    ),
                )
            }
            return StartBlock(summary, quickWriteLine(doc), items)
        }

        /**
         * The block for a project home that has only the document (LIB-13, R19 §2): whether the
         * author turned the hide default off is derived from the project's rating and the current
         * 설정 · 수위 등급 [defaults]. The rating hides by default and the project is not hidden →
         * the summary ends with `숨김 · 끔` (also when hiding was refused for want of a device lock,
         * which leaves the project visible all the same).
         */
        fun ofProject(doc: ProjectDoc, recorder: RecorderLibrary, defaults: RatingDefaults): StartBlock {
            val rating = doc.project.rating.known ?: Rating.R18
            val protections = NewProjectProtections.of(rating, defaults, step4Hide = doc.project.protections.hidden)
            return of(doc, recorder, protections)
        }

        /**
         * `바로 쓰기` line from the first unit of every level: top level `<n><level>` (`1장`), middle
         * levels `<level> <code>` (`에피소드 1-1`), then the scene's title or `<scene level> <n>`.
         */
        fun quickWriteLine(doc: ProjectDoc): String? {
            val codes = UnitCodes(doc)
            val levels = doc.project.structure.levels
            val chain = ArrayList<StoryUnit>()
            var parent: String? = null
            while (true) {
                val next = doc.index.children(parent).filter { doc.liveness.isLive(it) }.minByOrNull { it.order } ?: break
                chain += next
                parent = next.key
            }
            if (chain.isEmpty() || chain.last().level != doc.project.structure.sceneLevel) return null
            return chain.mapIndexed { i, unit ->
                val name = levels.getOrElse(unit.level) { "" }
                when {
                    unit.level == doc.project.structure.sceneLevel -> unit.title ?: codes.sceneOrdinalLabel(unit.key).orEmpty()
                    i == 0 -> "${codes.code(unit.key)}$name"
                    else -> "$name ${codes.code(unit.key)}"
                }
            }.joinToString(" · ")
        }
    }
}
