package io.github.lenticualrstone.civilight.domain.cast.refs

import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.StoryLabels
import io.github.lenticualrstone.civilight.domain.cast.label
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationReader
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.isLinkBeforeReveal
import io.github.lenticualrstone.civilight.model.recorder.RecorderLink

/** A count row of the impact sheet. */
data class ImpactRow(val label: String, val count: Int, val sub: String? = null) {
    val text: String get() = "$label $count"
}

/** The impact sheet (`RCDeleteImpact`, CST-40). */
data class DeleteImpactSheet(
    /** `‘노아’ 지우기`. */
    val title: String,
    /** `인물 · 참조 20`. */
    val sub: String,
    val ownedTitle: String,
    val owned: List<ImpactRow>,
    val staysTitle: String,
    val stays: List<ImpactRow>,
    val checkRow: Pair<String, String>,
    val mergeRow: Triple<String, String, String>,
    val footnote: String,
    val cancelLabel: String,
    val confirmLabel: String,
)

/**
 * What deleting an item touches (rule 20 §4, CST-40): owned data that goes to the trash with it and
 * references that stay in place (dotted). Counts only live references; references inside trashed
 * scenes, blocks or pages are neither counted nor checked (CST-41). Plain text is never counted.
 */
data class DeleteImpact(
    val kind: EntityKind,
    val key: Key,
    val name: String,
    val hidden: Boolean,
    /** Partner names of relations that go to the trash with the item. */
    val relationPartners: List<String>,
    val records: Int,
    val gameTexts: Int,
    val speakerLines: Int,
    /** Scenes of the counted lines, in manuscript order. */
    val speakerScenes: List<Key>,
    val appearances: Int,
    val links: Int,
    val recorderChips: Int,
    val fieldLinks: Int,
    val sceneplaces: Int,
    val termRelated: Int,
    val canvasNodes: Int,
    /** Terms: `▲ 공개 전 사용` that will be held (not removed). */
    val heldWarnings: Int,
) {
    val ownedCount: Int get() = relationPartners.size + records + gameTexts

    /** `참조 N`: references that stay in place (14 + 2 + 3 + 1 = 20 for 노아). */
    val referenceCount: Int get() = speakerLines + appearances + links + recorderChips + fieldLinks + sceneplaces + termRelated

    /** No references and nothing owned: delete at once with a toast, no sheet (TC-20-03). */
    val needsSheet: Boolean get() = ownedCount + referenceCount > 0

    /** The impact sheet; a hidden item shows counts only, no locations (TC-20-10). */
    fun sheet(doc: ProjectDoc): DeleteImpactSheet {
        val owned = buildList {
            if (relationPartners.isNotEmpty()) add(ImpactRow("관계", relationPartners.size, if (hidden) null else CastCopy.joinAll(relationPartners)))
            if (records > 0) add(ImpactRow("◆ 변화 기록", records))
            if (gameTexts > 0) add(ImpactRow("게임 텍스트", gameTexts))
        }
        val stays = buildList {
            if (speakerLines > 0) add(ImpactRow("대사 화자", speakerLines, if (hidden) null else sceneList(doc, speakerScenes)))
            if (appearances > 0) add(ImpactRow("씬 등장", appearances))
            if (sceneplaces > 0) add(ImpactRow("씬 장소", sceneplaces))
            if (links > 0) add(ImpactRow("@연결", links))
            if (fieldLinks > 0) add(ImpactRow("연결 값", fieldLinks))
            if (termRelated > 0) add(ImpactRow("관련", termRelated))
            if (recorderChips > 0) add(ImpactRow("레코더 칩", recorderChips))
        }
        val word = kind.label
        return DeleteImpactSheet(
            title = "‘$name’ 지우기",
            sub = "$word · 참조 $referenceCount",
            ownedTitle = "같이 휴지통으로",
            owned = owned,
            staysTitle = "제자리에 남음 · 지운 ${word}${directionParticle(word)} 보임",
            stays = stays,
            checkRow = "점검 센터" to "◇ 1줄로 모임 · 한꺼번에 바꾸기",
            mergeRow = Triple("중복이면", "지우지 않고 합치기", "다른 ${word}${withParticle(word)} 합치기 ›"),
            footnote = "휴지통 · 30일 · 되살리면 다시 이어짐",
            cancelLabel = "취소",
            confirmLabel = "휴지통으로",
        )
    }

    /** Toast after deleting: `인물 노아 지움 · 참조 20 · 점검 센터 · 되돌리기`, or `인물 노아 지움 · 되돌리기`. */
    val deletedToast: String
        get() = buildList {
            add("${kind.label} $name 지움")
            if (referenceCount > 0) {
                add("참조 $referenceCount")
                if (heldWarnings > 0) add("▲ 공개 전 사용 $heldWarnings")
                add("점검 센터")
            }
            add("되돌리기")
        }.joinToString(CastCopy.SEP)

    /** `대사 14 · 씬 등장 2 · @ 3 · 레코더 1` (check row body, trash row). */
    val countsLine: String get() = countsLine(speakerLines, appearances, links, recorderChips, fieldLinks, sceneplaces)

    companion object {
        /** `과` after a final consonant (`인물과`, `세력과`), `와` otherwise (`장소와`, `용어와`). */
        fun withParticle(word: String): String {
            val last = word.lastOrNull() ?: return "와"
            val hasFinal = last.code in 0xAC00..0xD7A3 && (last.code - 0xAC00) % 28 != 0
            return if (hasFinal) "과" else "와"
        }

        /** `으로` after a final consonant other than ㄹ (`세력으로`), `로` otherwise (`인물로`, `장소로`). */
        fun directionParticle(word: String): String {
            val last = word.lastOrNull() ?: return "로"
            if (last.code !in 0xAC00..0xD7A3) return "로"
            val jong = (last.code - 0xAC00) % 28
            return if (jong == 0 || jong == 8) "로" else "으로"
        }

        fun countsLine(speaker: Int, appearances: Int, links: Int, recorder: Int, fieldLinks: Int = 0, places: Int = 0): String =
            CastCopy.joinAll(
                listOf(
                    speaker.takeIf { it > 0 }?.let { "대사 $it" },
                    places.takeIf { it > 0 }?.let { "씬 장소 $it" },
                    appearances.takeIf { it > 0 }?.let { "씬 등장 $it" },
                    links.takeIf { it > 0 }?.let { "@ $it" },
                    fieldLinks.takeIf { it > 0 }?.let { "연결 $it" },
                    recorder.takeIf { it > 0 }?.let { "레코더 $it" },
                ),
            )

        /** `1-6 씬 2 · 씬 4`: the full code for the first scene of each episode, `씬 N` after it. */
        fun sceneList(doc: ProjectDoc, scenes: List<Key>): String {
            var lastEpisode: String? = null
            return scenes.joinToString(CastCopy.SEP) { key ->
                val episode = StoryLabels.episodeCodeOf(doc, key)
                val code = StoryLabels.unitCode(doc, key).orEmpty()
                val text = if (episode != null && episode == lastEpisode) code.removePrefix("$episode ") else code
                lastEpisode = episode
                text
            }
        }

        /** Several items at once: one line of totals (`인물 3 · 대사 40 · …`, TC-20-11). */
        fun combinedSummary(impacts: List<DeleteImpact>): String {
            val byKind = impacts.groupBy { it.kind }.entries.joinToString(CastCopy.SEP) { (k, list) -> "${k.label} ${list.size}" }
            val counts = countsLine(
                impacts.sumOf { it.speakerLines }, impacts.sumOf { it.appearances }, impacts.sumOf { it.links },
                impacts.sumOf { it.recorderChips }, impacts.sumOf { it.fieldLinks }, impacts.sumOf { it.sceneplaces },
            )
            return CastCopy.join(byKind, counts)
        }

        /** Computes the impact of deleting a live item. [recorderLinks]: recorder links of this project. */
        fun of(doc: ProjectDoc, kind: EntityKind, key: Key, recorderLinks: List<RecorderLink> = emptyList()): DeleteImpact {
            val name = doc.index.nameOf(kind, key).orEmpty()
            val hidden = when (kind) {
                EntityKind.CHARACTER -> doc.index.character(key)?.hidden
                EntityKind.PLACE -> doc.index.place(key)?.hidden
                EntityKind.TERM -> doc.index.term(key)?.hidden
                EntityKind.FACTION -> doc.index.faction(key)?.hidden
            } ?: false
            val refs = References.scan(doc, kind, key, recorderLinks).filter { it.live }
            val speaker = refs.filter { it.kind == RefKind.SPEAKER }
            val scenes = speaker.mapNotNull { it.sceneKey }.distinct()
                .sortedBy { doc.manuscript.sceneIndex(it) ?: Int.MAX_VALUE }

            val ownRelations = if (kind == EntityKind.CHARACTER || kind == EntityKind.FACTION) {
                doc.cast.relations.filter { r ->
                    (r.endA.key == key || r.endB.key == key) && doc.liveness.isLive(r)
                }
            } else {
                emptyList()
            }
            val partners = ownRelations.map { r ->
                val other = if (r.endA.key == key) r.endB.key else r.endA.key
                doc.index.character(other)?.name ?: doc.index.faction(other)?.name.orEmpty()
            }
            val relationKeys = ownRelations.mapTo(HashSet()) { it.key }
            val records = doc.changes.records.count { r ->
                doc.liveness.isLive(r) && when (val t = r.target) {
                    is ChangeTarget.Field -> t.entityKind.known == kind && t.entityKey == key
                    is ChangeTarget.DisplayName -> kind == EntityKind.CHARACTER && t.characterKey == key
                    is ChangeTarget.RelationSlot -> t.relationKey in relationKeys
                    is ChangeTarget.TermReveal -> kind == EntityKind.TERM && t.termKey == key
                    else -> false
                }
            }
            val gameTexts = if (kind == EntityKind.CHARACTER) doc.index.voiceLines(key).size + doc.index.archiveDocs(key).size else 0
            val held = if (kind == EntityKind.TERM) {
                refs.count { r -> r.kind == RefKind.LINK && r.ownerKind == "block" && doc.isLinkBeforeReveal(key, r.ownerKey) }
            } else {
                0
            }
            return DeleteImpact(
                kind = kind, key = key, name = name, hidden = hidden,
                relationPartners = partners,
                records = records,
                gameTexts = gameTexts,
                speakerLines = speaker.size,
                speakerScenes = scenes,
                appearances = refs.count { it.kind == RefKind.APPEARANCE },
                links = refs.count { it.kind == RefKind.LINK },
                recorderChips = refs.count { it.kind == RefKind.RECORDER },
                fieldLinks = refs.count { it.kind == RefKind.FIELD_LINK },
                sceneplaces = refs.count { it.kind == RefKind.SCENE_PLACE },
                termRelated = refs.count { it.kind == RefKind.TERM_RELATED },
                canvasNodes = refs.count { it.kind == RefKind.CANVAS_NODE },
                heldWarnings = held,
            )
        }

        /** Relation partners of a character, for owned-relation listing. */
        internal fun relationKeysOf(doc: ProjectDoc, key: Key): List<Key> =
            doc.cast.relations.filter { RelationReader.sideOf(it, key) != null }.map { it.key }
    }
}
