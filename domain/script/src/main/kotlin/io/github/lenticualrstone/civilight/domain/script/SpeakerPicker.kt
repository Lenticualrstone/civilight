package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.liveCharacters
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind

/** Speaker picker sheet content (SCR-19) and the select-mode bottom bar (SCR-36). */
object SpeakerPicker {
    data class Row(val characterKey: Key, val name: String, val initial: String, val subline: String)

    data class Sheet(
        /** `지운 인물 · 노아 · 되살리기` when the block's speaker is a deleted character. */
        val deletedSpeakerRow: String?,
        val inScene: List<Row>,
        val recent: List<Row>,
        val all: List<Row>,
        /** `숨긴 인물 {n} · 지문으로 보기` while hidden characters are locked. */
        val hiddenRow: String?,
    ) {
        val title: String get() = "화자"
        val searchHint: String get() = "초성 검색"
    }

    /**
     * Sections `이 씬 등장` (speakers and silent cast of the scene), `최근` (up to 5, `[PROPOSED]`),
     * `전체` (the rest by order). Locked hidden characters and deleted ones are not listed.
     */
    fun sheet(doc: ProjectDoc, block: Block, recentSpeakers: List<Key>, unlocked: Boolean = false): Sheet {
        val visible = doc.liveCharacters.filter { unlocked || !it.hidden }
        val byKey = visible.associateBy { it.key }
        val scene = doc.index.unit(block.sceneKey)
        val inSceneKeys = (doc.manuscript.liveBlocksOf(block.sceneKey).mapNotNull { it.speakerKey } + scene?.silentCast.orEmpty()).distinct()
        val inScene = inSceneKeys.mapNotNull(byKey::get)
        val recent = recentSpeakers.filter { it !in inSceneKeys }.mapNotNull(byKey::get).take(5)
        val listed = (inScene + recent).map { it.key }.toSet()
        val speaker = block.speakerKey?.takeIf { block.kind.known == BlockKind.DIALOGUE }
        val deleted = speaker?.let { k ->
            val c = doc.index.character(k)
            if (c != null && c.deletedAt != null) "지운 인물 · ${c.name} · 되살리기" else null
        }
        val hiddenCount = doc.liveCharacters.count { it.hidden }
        return Sheet(
            deletedSpeakerRow = deleted,
            inScene = inScene.map { row(doc, it, block) },
            recent = recent.map { row(doc, it, block) },
            all = visible.filter { it.key !in listed }.map { row(doc, it, block) },
            hiddenRow = if (!unlocked && hiddenCount > 0) "숨긴 인물 $hiddenCount · 지문으로 보기" else null,
        )
    }

    private fun row(doc: ProjectDoc, c: Character, block: Block): Row {
        val display = Manuscript.displayNameAt(doc, c.key, block.key)
        val tags = c.tagKeys.mapNotNull { doc.index.tag(it)?.takeIf { t -> t.isLive }?.name }
        val sub = if (display != null && display != c.name) "표시 이름 $display" else tags.joinToString(" · ")
        return Row(c.key, c.name, c.name.take(1), sub)
    }

    // --- Select mode (SCR-36) ---

    /** Bottom bar: `고른 블록 {n}개` and `#{first} ~ #{last} · ◆ {a} · ◇ {b} 같이 감`. */
    data class SelectionBar(val count: String, val range: String?) {
        val buttons: List<String> get() = listOf("옮기기", "지우기", "잘라내기", "복사")
    }

    fun selectionBar(doc: ProjectDoc, checked: Collection<Key>): SelectionBar {
        val set = checked.toSet()
        val blocks = doc.manuscript.liveBlocks.filter { it.key in set }
        if (blocks.isEmpty()) return SelectionBar("고른 블록 0개", null)
        val records = blocks.flatMap { doc.recordsAnchoredOn(it.key) }
        val range = "${Labels.number(blocks.first().number)} ~ ${Labels.number(blocks.last().number)} · " +
            "◆ ${records.count { !it.isReveal }} · ◇ ${records.count { it.isReveal }} 같이 감"
        return SelectionBar("고른 블록 ${blocks.size}개", range)
    }

    /** Select-mode label line `{label} #{n}`. */
    fun selectLabel(doc: ProjectDoc, block: Block): String = "${Labels.blockLabel(doc, block)} ${Labels.number(block.number)}"
}
