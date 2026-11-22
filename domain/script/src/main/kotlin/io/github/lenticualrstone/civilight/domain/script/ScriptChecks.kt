package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.change.AnchorState
import io.github.lenticualrstone.civilight.model.checks.AckKind
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.project.VoiceIdFormatMode
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import io.github.lenticualrstone.civilight.model.text.plainText

/**
 * Check items computed from script data (SCR-62). The check center lists, filters and stores
 * `맞음`; items acknowledged with a matching fingerprint are left out here.
 */
object ScriptChecks {
    enum class Mark { WARN, NOTICE }

    data class Item(
        val checkType: String,
        val mark: Mark,
        val title: String,
        val subjectKeys: List<Key>,
        val filter: String,
        val buttons: List<String>,
        /** Extra lines: the quoted line, `대사 블록 {n}개`, a location. */
        val details: List<String> = emptyList(),
        val fingerprint: String = "",
    )

    fun compute(doc: ProjectDoc, time: SceneTimeSource): List<Item> =
        speakerMissing(doc) + anchorDeleted(doc) + voiceFormatDiffers(doc) + voiceDuplicates(doc) +
            transitionStale(doc, time) + transitionPlaceBeforeReveal(doc)

    private fun acked(doc: ProjectDoc, checkType: String, subjects: List<Key>, fingerprint: String): Boolean =
        doc.checks.acknowledgements.any { it.checkType == checkType && it.subjectKeys == subjects && it.fingerprint == fingerprint }

    /** `▲ 화자 없음`: a non-empty dialogue block with no speaker (one item per block). */
    fun speakerMissing(doc: ProjectDoc): List<Item> {
        val blocks = doc.manuscript.liveBlocks.filter { it.kind.known == BlockKind.DIALOGUE && it.speakerKey == null && it.hasContent() }
        return blocks.map { b ->
            Item(
                CheckType.SPEAKER_MISSING, Mark.WARN, "▲ 화자 없음", listOf(b.key), "화자", listOf("화자 고르기", "나레이션으로 바꾸기"),
                details = listOf("“${Runs.displayText(b.runs)}”", "대사 블록 ${blocks.size}개", Labels.sceneLocation(doc, b.sceneKey)),
            )
        }
    }

    /** `◇ 근거 지워진 변화 {n}`: one item over every `orphaned` record (`모두 지우기` with several). */
    fun anchorDeleted(doc: ProjectDoc): List<Item> {
        val records = doc.changes.records.filter { it.deletedAt == null && it.anchorState.known == AnchorState.ORPHANED }
        if (records.isEmpty()) return emptyList()
        val buttons = listOf("다른 블록에 잇기", "이대로 두기", "기록 지우기") + if (records.size > 1) listOf("모두 지우기") else emptyList()
        return listOf(Item(CheckType.CHANGE_ANCHOR_DELETED, Mark.NOTICE, "◇ 근거 지워진 변화 ${records.size}", records.map { it.key }, "변화", buttons))
    }

    /** Fingerprint of a fixed ID that differs: the stored string and what the format gives now. */
    private fun voiceFingerprint(fixed: String?, now: String?) = "voice:$fixed→$now"

    /** `◇ 보이스 ID가 화자 · 에피소드와 다름` with item text `형식과 다른 ID {n}` (group `보이스`). */
    fun voiceFormatDiffers(doc: ProjectDoc): List<Item> {
        val subjects = mutableListOf<List<Key>>()
        for (b in doc.manuscript.liveBlocks) {
            if (VoiceIds.differsFromFormat(doc, b)) {
                val fp = voiceFingerprint(b.voice?.fixedId, VoiceIds.autoId(doc, b))
                if (!acked(doc, CheckType.VOICE_FORMAT_DIFFERS, listOf(b.key), fp)) subjects += listOf(b.key)
            }
            for (o in b.options) if (VoiceIds.differsFromFormat(doc, b, o)) {
                val fp = voiceFingerprint(o.voice?.fixedId, VoiceIds.autoIdOfOption(doc, b, o))
                if (!acked(doc, CheckType.VOICE_FORMAT_DIFFERS, listOf(b.key, o.key), fp)) subjects += listOf(b.key, o.key)
            }
        }
        if (subjects.isEmpty()) return emptyList()
        return listOf(
            Item(
                CheckType.VOICE_FORMAT_DIFFERS, Mark.NOTICE, "◇ 보이스 ID가 화자 · 에피소드와 다름", subjects.flatten().distinct(), "보이스",
                listOf("새 ID로", "그대로"), details = listOf("형식과 다른 ID ${subjects.size}"),
            ),
        )
    }

    /** Fingerprint for a `그대로` (`맞음`) on a fixed ID that differs from the format. */
    fun voiceFormatFingerprint(doc: ProjectDoc, blockKey: Key, optionKey: Key? = null): String? {
        val b = doc.index.block(blockKey) ?: return null
        val o = optionKey?.let { k -> b.options.firstOrNull { it.key == k } }
        return if (o == null) voiceFingerprint(b.voice?.fixedId, VoiceIds.autoId(doc, b))
        else voiceFingerprint(o.voice?.fixedId, VoiceIds.autoIdOfOption(doc, b, o))
    }

    /**
     * `▲ 보이스 ID 겹침 {n}` (`[PROPOSED]`): IDs in use that are shared by two or more blocks or
     * options (format `직접`), or an automatic ID equal to a discarded ID (Q-07). One item; n = groups.
     */
    fun voiceDuplicates(doc: ProjectDoc): List<Item> {
        val inUse = mutableListOf<Pair<String, Key>>()
        for (b in doc.manuscript.liveBlocks) {
            VoiceIds.currentId(doc, b)?.let { inUse += it to b.key }
            for (o in b.options) VoiceIds.currentIdOfOption(doc, b, o)?.let { inUse += it to b.key }
        }
        val groups = inUse.groupBy({ it.first }, { it.second }).filterValues { it.size > 1 }.toMutableMap()
        if (doc.project.script.voiceIdFormat.mode.known != VoiceIdFormatMode.MANUAL) {
            val discarded = VoiceIds.discarded(doc).map { it.id }.toSet()
            for ((id, key) in inUse) if (id in discarded && !VoiceIds.isFixed(doc.index.block(key)?.voice)) groups.getOrPut(id) { mutableListOf(key) }
        }
        if (groups.isEmpty()) return emptyList()
        return listOf(
            Item(
                CheckType.VOICE_DUPLICATE, Mark.WARN, "▲ 보이스 ID 겹침 ${groups.size}", groups.values.flatten().distinct(), "보이스",
                listOf("원고로 가기"), details = groups.keys.sorted(),
            ),
        )
    }

    // --- Transition text ---

    private fun clockOrDay(tRange: io.github.lenticualrstone.civilight.model.IntSpan?, dayRange: io.github.lenticualrstone.civilight.model.IntSpan?): String =
        when {
            tRange != null && tRange.first == tRange.last -> Bands.clockOf(tRange.first).toString()
            dayRange != null -> if (dayRange.first == dayRange.last) "이야기 ${dayRange.first}일째" else "이야기 ${dayRange.first}~${dayRange.last}일째"
            else -> "때 없음"
        }

    /**
     * `◇ 전환 문구 · 쓴 뒤 때 바뀜 · 1-7 씬 4 · 06:10 → 07:00` (or `쓴 뒤 장소 바뀜`): direct text whose
     * snapshot differs from the scene's current place or resolved 때 (Q-24 default: resolved values).
     * Filter `씬` (Q-24 default).
     */
    fun transitionStale(doc: ProjectDoc, time: SceneTimeSource): List<Item> = doc.manuscript.liveScenes.mapNotNull { s ->
        if (s.transitionModeOrNone != TransitionMode.DIRECT) return@mapNotNull null
        val snap = s.transitionSnapshot ?: return@mapNotNull null
        val now = time.timeOf(s.key)
        val where = Labels.sceneLocation(doc, s.key)
        val placeChanged = !(snap.place?.sameAs(s.place) ?: (s.place == null))
        val timeChanged = snap.dayRange != now?.storyDay || snap.tRange != now?.tRange
        val title = when {
            placeChanged -> "◇ 전환 문구 · 쓴 뒤 장소 바뀜 · $where · ${SceneText.placeName(doc, snap.place) ?: "장소 없음"} → ${SceneText.placeName(doc, s.place) ?: "장소 없음"}"
            timeChanged -> "◇ 전환 문구 · 쓴 뒤 때 바뀜 · $where · ${clockOrDay(snap.tRange, snap.dayRange)} → ${clockOrDay(now?.tRange, now?.storyDay)}"
            else -> return@mapNotNull null
        }
        val fp = "transition:${s.transitionRuns?.plainText}|${s.place?.key}|${now?.storyDay}|${now?.tRange}"
        if (acked(doc, CheckType.TRANSITION_STALE, listOf(s.key), fp)) return@mapNotNull null
        Item(CheckType.TRANSITION_STALE, Mark.NOTICE, title, listOf(s.key), "씬", listOf("자동으로", "문구 고치기", "맞음"), fingerprint = fp)
    }

    /**
     * `◇ 공개 전 장소 · 전환 문구`: automatic text names a place whose map pins are all revealed only
     * after this scene (the text is still generated).
     */
    fun transitionPlaceBeforeReveal(doc: ProjectDoc): List<Item> = doc.manuscript.liveScenes.mapNotNull { s ->
        if (s.transitionModeOrNone != TransitionMode.AUTO || !placeBeforeReveal(doc, s)) return@mapNotNull null
        val fp = "placeReveal:${s.place?.key}"
        if (acked(doc, CheckType.TRANSITION_PLACE_BEFORE_REVEAL, listOf(s.key), fp)) return@mapNotNull null
        Item(
            CheckType.TRANSITION_PLACE_BEFORE_REVEAL, Mark.NOTICE, "◇ 공개 전 장소 · 전환 문구", listOf(s.key), "씬",
            listOf("직접 쓰기", "맞음"), details = listOf(Labels.sceneLocation(doc, s.key)), fingerprint = fp,
        )
    }

    private fun placeBeforeReveal(doc: ProjectDoc, scene: StoryUnit): Boolean {
        val place = scene.place?.takeIf { it.kind.known == PlaceRefKind.PLACE } ?: return false
        val rank = doc.manuscript.transitionRankOf(scene.key) ?: return false
        val pins = doc.maps.values.filter { doc.liveness.isMapLive(it.key) }.flatMap { m -> m.pins.filter { it.placeKey == place.key } }
        if (pins.isEmpty()) return false
        return pins.all { p -> p.revealPoint != null && doc.manuscript.rankOf(p.revealPoint!!) > rank }
    }

    /**
     * `▲ 숨긴 씬으로 가는 선택지 {n}` (export sheet, Q-28 default): choice blocks that are the last
     * live block of a visible scene directly followed by a hidden scene, when hidden scenes are left out.
     */
    fun choicesIntoHiddenScenes(doc: ProjectDoc, sceneKeys: List<Key>): List<Key> {
        val scenes = doc.manuscript.liveScenes
        return sceneKeys.mapNotNull { key ->
            val i = scenes.indexOfFirst { it.key == key }
            val scene = scenes.getOrNull(i)?.takeIf { !it.hidden } ?: return@mapNotNull null
            val next = scenes.getOrNull(i + 1)?.takeIf { it.hidden } ?: return@mapNotNull null
            if (doc.numberingUnitOfScene(next.key) != doc.numberingUnitOfScene(scene.key)) return@mapNotNull null
            doc.manuscript.liveBlocksOf(key).lastOrNull()?.takeIf { it.kind.known == BlockKind.CHOICE }?.key
        }
    }

    /** `▲ 숨긴 씬으로 가는 선택지 {n}`, or null for none. */
    fun choicesIntoHiddenLabel(count: Int): String? = count.takeIf { it > 0 }?.let { "▲ 숨긴 씬으로 가는 선택지 $it" }

    /** Ack kind the check center stores for each script item. */
    fun ackKindOf(checkType: String): AckKind? = CheckType.ACKABLE_AS[checkType]
}
