package io.github.lenticualrstone.civilight.domain.story.scene

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.BlockKind

/**
 * 등장 of a scene (STO-26): speakers of its live dialogue blocks and spoken choice options (a fact,
 * first line first) plus the silent cast added by hand.
 */
data class Appearance(val speakers: List<Key>, val silent: List<Key>) {
    /** Everyone in the scene (speakers first). */
    val all: List<Key> get() = (speakers + silent).distinct()

    companion object {
        fun of(doc: ProjectDoc, sceneKey: Key): Appearance {
            val speakers = LinkedHashSet<Key>()
            for (b in doc.manuscript.liveBlocksOf(sceneKey)) {
                if (b.kind.known == BlockKind.DIALOGUE) b.speakerKey?.let(speakers::add)
                if (b.kind.known == BlockKind.CHOICE) b.options.mapNotNullTo(speakers) { it.speakerKey }
            }
            val silent = doc.index.unit(sceneKey)?.silentCast.orEmpty().filter { it !in speakers }
            return Appearance(speakers.toList(), silent)
        }

        /** Sheet row `리엔 · 하르트 | (대사) | + 세라` parts: automatic names, then silent names. */
        fun sheetRow(doc: ProjectDoc, sceneKey: Key): Pair<String, String> {
            val a = of(doc, sceneKey)
            fun name(k: Key) = doc.index.character(k)?.name ?: "?"
            return a.speakers.joinToString(" · ", transform = ::name) to a.silent.joinToString(" · ", transform = ::name)
        }
    }
}
