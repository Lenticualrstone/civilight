package io.github.lenticualrstone.civilight.store.project

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/**
 * Gives a project a new portable key (§6: `새 프로젝트로`, duplicate, re-keying a replaced or
 * trashed same-key copy). Item keys stay. Besides `project.key`, the project key is also the
 * numbering unit of a 1-level structure (episode map key and `EpisodeFile.unitKey`, §4.1) and the
 * `projectKey` of this project's trash rows.
 */
object ProjectRekey {
    fun rekey(doc: ProjectDoc, newKey: Key): ProjectDoc {
        val old = doc.project.key
        if (old == newKey) return doc
        val episodes = LinkedHashMap<Key, io.github.lenticualrstone.civilight.model.file.EpisodeFile>()
        for ((groupKey, file) in doc.episodes) {
            val key = if (groupKey == old) newKey else groupKey
            episodes[key] = if (file.unitKey == old) file.copy(unitKey = newKey) else file
        }
        return doc.copy(
            project = doc.project.copy(key = newKey),
            episodes = episodes,
            trash = doc.trash.copy(entries = doc.trash.entries.map { if (it.projectKey == old) it.copy(projectKey = newKey) else it }),
        )
    }
}
