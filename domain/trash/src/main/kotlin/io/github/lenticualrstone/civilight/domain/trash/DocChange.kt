package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.file.EpisodeFile

/**
 * The before and after state of one operation (delete, merge, bulk change …), used for the toast's
 * `되돌리기`. [revert] is row-level: rows the operation changed go back to their earlier value, rows
 * it added are removed, rows it removed come back at their old position; rows the operation did not
 * touch keep any later edit.
 */
data class DocChange(val before: ProjectDoc, val after: ProjectDoc) {
    /** [current] with this change undone. */
    fun revert(current: ProjectDoc): ProjectDoc {
        val b = before
        val a = after
        val c = current
        val fieldDefs = revertList(c.project.fieldDefs, b.project.fieldDefs, a.project.fieldDefs) { it.key }
        val projectCore = if (b.project.copy(fieldDefs = emptyList()) != a.project.copy(fieldDefs = emptyList())) b.project else c.project
        return c.copy(
            project = projectCore.copy(fieldDefs = fieldDefs),
            story = c.story.copy(
                units = revertList(c.story.units, b.story.units, a.story.units) { it.key },
                discardedVoiceIds = revertList(c.story.discardedVoiceIds, b.story.discardedVoiceIds, a.story.discardedVoiceIds) {
                    Triple(it.id, it.blockKey, it.optionKey)
                },
            ),
            episodes = revertEpisodes(c.episodes, b.episodes, a.episodes),
            cast = c.cast.copy(
                characters = revertList(c.cast.characters, b.cast.characters, a.cast.characters) { it.key },
                relations = revertList(c.cast.relations, b.cast.relations, a.cast.relations) { it.key },
                voiceLines = revertList(c.cast.voiceLines, b.cast.voiceLines, a.cast.voiceLines) { it.key },
                archiveDocs = revertList(c.cast.archiveDocs, b.cast.archiveDocs, a.cast.archiveDocs) { it.key },
            ),
            world = c.world.copy(
                places = revertList(c.world.places, b.world.places, a.world.places) { it.key },
                terms = revertList(c.world.terms, b.world.terms, a.world.terms) { it.key },
                factions = revertList(c.world.factions, b.world.factions, a.world.factions) { it.key },
                events = revertList(c.world.events, b.world.events, a.world.events) { it.key },
            ),
            maps = revertMap(c.maps, b.maps, a.maps),
            changes = c.changes.copy(records = revertList(c.changes.records, b.changes.records, a.changes.records) { it.key }),
            checks = c.checks.copy(
                acknowledgements = revertList(c.checks.acknowledgements, b.checks.acknowledgements, a.checks.acknowledgements) { it.key },
                travelOverrides = revertList(c.checks.travelOverrides, b.checks.travelOverrides, a.checks.travelOverrides) { it.key },
            ),
            tombstones = c.tombstones.copy(
                nameTags = revertList(c.tombstones.nameTags, b.tombstones.nameTags, a.tombstones.nameTags) { it.key },
                mergeRecords = revertList(c.tombstones.mergeRecords, b.tombstones.mergeRecords, a.tombstones.mergeRecords) { it.key },
            ),
            deletions = c.deletions.copy(
                entries = revertList(c.deletions.entries, b.deletions.entries, a.deletions.entries) { Triple(it.key, it.kind, it.deletedAt) },
            ),
            canvases = revertMap(c.canvases, b.canvases, a.canvases),
            pages = revertMap(c.pages, b.pages, a.pages),
            assets = c.assets.copy(
                blobs = revertList(c.assets.blobs, b.assets.blobs, a.assets.blobs) { it.hash },
                imageUses = revertList(c.assets.imageUses, b.assets.imageUses, a.assets.imageUses) { it.key },
                scriptAssets = revertList(c.assets.scriptAssets, b.assets.scriptAssets, a.assets.scriptAssets) { it.key },
            ),
            trash = c.trash.copy(entries = revertList(c.trash.entries, b.trash.entries, a.trash.entries) { it.key }),
        )
    }

    private fun revertEpisodes(
        current: Map<Key, EpisodeFile>,
        before: Map<Key, EpisodeFile>,
        after: Map<Key, EpisodeFile>,
    ): Map<Key, EpisodeFile> {
        val groups = LinkedHashSet<Key>().apply { addAll(current.keys); addAll(before.keys) }
        val out = LinkedHashMap<Key, EpisodeFile>()
        for (group in groups) {
            val b = before[group]
            val a = after[group]
            val c = current[group]
            if (b == null && a != null && c != null && a == c) continue // added by the change and untouched since
            val base = c ?: b ?: continue
            val empty = EpisodeFile(unitKey = group)
            out[group] = base.copy(
                blocks = revertList(c?.blocks.orEmpty(), (b ?: empty).blocks, (a ?: empty).blocks) { it.key },
                discardedVoiceIds = revertList(c?.discardedVoiceIds.orEmpty(), (b ?: empty).discardedVoiceIds, (a ?: empty).discardedVoiceIds) {
                    Triple(it.id, it.blockKey, it.optionKey)
                },
            )
        }
        return out
    }

    private fun <V> revertMap(current: Map<Key, V>, before: Map<Key, V>, after: Map<Key, V>): Map<Key, V> {
        val out = LinkedHashMap<Key, V>()
        for ((key, value) in current) {
            when {
                key !in before && key in after -> Unit // added by the change
                key in before && key in after && before[key] != after[key] -> out[key] = before.getValue(key)
                else -> out[key] = value
            }
        }
        for ((key, value) in before) if (key !in after && key !in out) out[key] = value
        return out
    }

    companion object {
        /**
         * [current] with the rows of [before] → [after] reverted, keyed by [key]; rows removed by the
         * change are re-inserted before the next surviving row they used to precede.
         */
        internal fun <T> revertList(current: List<T>, before: List<T>, after: List<T>, key: (T) -> Any?): List<T> {
            if (before === after || before == after) return current
            val b = before.associateBy(key)
            val a = after.associateBy(key)
            val added = a.keys - b.keys
            val changed = a.keys.filter { it in b && a[it] != b[it] }.toSet()
            val removed = b.keys - a.keys
            val result = current.filter { key(it) !in added }
                .map { val k = key(it); if (k in changed) b.getValue(k) else it }
                .toMutableList()
            if (removed.isEmpty()) return result
            val beforeKeys = before.map(key)
            for ((i, row) in before.withIndex()) {
                val k = beforeKeys[i]
                if (k !in removed || result.any { key(it) == k }) continue
                val nextPresent = beforeKeys.drop(i + 1).firstNotNullOfOrNull { nk ->
                    result.indexOfFirst { key(it) == nk }.takeIf { it >= 0 }
                }
                if (nextPresent == null) result += row else result.add(nextPresent, row)
            }
            return result
        }
    }
}
