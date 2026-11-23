package io.github.lenticualrstone.civilight.store.templates

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.FieldTemplate
import io.github.lenticualrstone.civilight.model.app.FieldTemplatesFile
import io.github.lenticualrstone.civilight.store.StoreEnvironment
import io.github.lenticualrstone.civilight.store.io.JsonFile
import io.github.lenticualrstone.civilight.store.io.StoreDirectory
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * The user's 인물 템플릿 (CST-63): `files/app/fieldTemplates.json`, app level (class A content, never
 * in `.civilight`, not [io.github.lenticualrstone.civilight.model.app.AppSettings]). A missing or
 * unreadable file reads as empty. Templates trashed more than 30 days ago are dropped on load.
 */
class FieldTemplateStore(dir: File, private val env: StoreEnvironment) {
    private val file = JsonFile(StoreDirectory(dir), FILE, FieldTemplatesFile.serializer(), FieldTemplatesFile(), env)

    val state: StateFlow<FieldTemplatesFile> get() = file.state

    /** Loads the file, then drops templates trashed for 30 days or more. */
    suspend fun ensureLoaded() {
        file.ensureLoaded()
        purgeDeleted(env.clock.now())
    }

    suspend fun update(transform: (FieldTemplatesFile) -> FieldTemplatesFile): FieldTemplatesFile = file.update(transform)

    /** Replaces (or adds) one template by key. */
    suspend fun put(template: FieldTemplate): FieldTemplatesFile = update { f ->
        if (f.templates.any { it.key == template.key }) {
            f.copy(templates = f.templates.map { if (it.key == template.key) template else it })
        } else {
            f.copy(templates = f.templates + template)
        }
    }

    /** Changes one template; nothing happens when the key is unknown. */
    suspend fun edit(key: Key, change: (FieldTemplate) -> FieldTemplate): FieldTemplatesFile =
        update { f -> f.copy(templates = f.templates.map { if (it.key == key) change(it) else it }) }

    /** Drops templates whose `deletedAt` is [FieldTemplate.TRASH_MS] or more before [now]; returns their keys. */
    suspend fun purgeDeleted(now: Timestamp): List<Key> {
        var dropped = emptyList<Key>()
        update { f ->
            val (gone, keep) = f.templates.partition { isExpired(it, now) }
            dropped = gone.map { it.key }
            if (gone.isEmpty()) f else f.copy(templates = keep)
        }
        return dropped
    }

    suspend fun flush() = file.flush()

    companion object {
        const val FILE = "fieldTemplates.json"

        fun isExpired(template: FieldTemplate, now: Timestamp): Boolean {
            val at = template.deletedAt ?: return false
            return now - at >= FieldTemplate.TRASH_MS
        }
    }
}
