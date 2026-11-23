package io.github.lenticualrstone.civilight.store.project

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/**
 * Gives a project a new portable key (§6: `새 프로젝트로`, duplicate, re-keying a replaced or
 * trashed same-key copy). Item keys stay. Besides `project.key`, the project key is also the
 * numbering unit of a 1-level structure (episode map key and `EpisodeFile.unitKey`, §4.1), the
 * `projectKey` of this project's trash rows and the owner of the project gallery. (0.2.0) Episode
 * versions (`versions/<unitKey>.json`) and a foreshadowing's `거둘 곳` of that unit move with it.
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
        // The project gallery is filed under the project key (55 §3.3).
        val gallery = io.github.lenticualrstone.civilight.model.gallery.GalleryFile.rekeyProject(doc.gallery, old, newKey)
        // (0.2.0, N-29) Versions of the 1-level numbering unit are filed under the project key.
        val versions = LinkedHashMap<Key, io.github.lenticualrstone.civilight.model.version.EpisodeVersionsFile>()
        for ((unitKey, file) in doc.versions) {
            versions[if (unitKey == old) newKey else unitKey] = if (file.unitKey == old) file.copy(unitKey = newKey) else file
        }
        // (0.2.0, N-41) `거둘 곳` may name the 1-level numbering unit.
        val foreshadows = doc.story.foreshadows.map { if (it.payoffUnitKey == old) it.copy(payoffUnitKey = newKey) else it }
        return doc.copy(
            project = doc.project.copy(key = newKey),
            episodes = episodes,
            gallery = gallery,
            versions = versions,
            story = if (foreshadows == doc.story.foreshadows) doc.story else doc.story.copy(foreshadows = foreshadows),
            trash = doc.trash.copy(entries = doc.trash.entries.map { if (it.projectKey == old) it.copy(projectKey = newKey) else it }),
        )
    }
}
