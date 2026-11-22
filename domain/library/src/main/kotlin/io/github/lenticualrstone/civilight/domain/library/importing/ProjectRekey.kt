package io.github.lenticualrstone.civilight.domain.library.importing

import io.github.lenticualrstone.civilight.domain.library.format.JsonScan
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.ProjectFileSet
import kotlinx.serialization.json.JsonElement

/**
 * Gives a project a new portable key (§6: `새 프로젝트로` import, and the old copy on `덮어쓰기`).
 * Item keys are kept. Every reference to the project key moves with it: the project row, trash
 * entries, the cover image owner, the 1-level numbering unit (file name and `unitKey`), and any
 * `ext` value holding it. Done on the file units, so nothing that holds the key as a string is missed.
 */
object ProjectRekey {
    fun rekey(doc: ProjectDoc, newKey: Key): ProjectDoc {
        val oldKey = doc.key
        if (oldKey == newKey) return doc
        val set = ProjectFileSet.split(doc)
        val files: Map<String, JsonElement> = set.encode().entries.associate { (path, element) ->
            path.replace(oldKey, newKey) to JsonScan.replaceString(element, oldKey, newKey)
        }
        return ProjectFileSet.decode(files).files.toDoc(app = doc.app, unknownFiles = doc.unknownFiles)
    }
}
