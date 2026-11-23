package io.github.lenticualrstone.civilight.feature.library.backup

import io.github.lenticualrstone.civilight.domain.cast.field.FieldTemplates
import io.github.lenticualrstone.civilight.domain.library.crypto.DecryptResult
import io.github.lenticualrstone.civilight.domain.library.crypto.PackageCrypto
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.FieldTemplate
import io.github.lenticualrstone.civilight.model.app.FieldTemplatesFile
import io.github.lenticualrstone.civilight.model.json.CivilightJson

/** What one backup run uploads of the 인물 템플릿 (CST-63, TC-F-46). */
data class TemplateBackupPlan(
    /** Non-hidden templates (live and in `지운 템플릿`), uploaded in plain form. */
    val visible: List<FieldTemplate>,
    /** Hidden templates, uploaded only encrypted with the 복구 암호; empty when they are left out. */
    val hidden: List<FieldTemplate>,
    /** Hidden templates exist but are left out (no 복구 암호, or `숨긴 것도 백업` off). */
    val hiddenLeftOut: Boolean,
) {
    /** 설정 › 보관 warning `숨긴 템플릿 · 백업 안 됨` (no count, CST-62). */
    val warnings: List<String> get() = if (hiddenLeftOut) listOf(BackupCopy.HIDDEN_TEMPLATES) else emptyList()
}

/**
 * The template unit of the Drive backup (CST-63): `fieldTemplates.json` next to the settings JSON;
 * hidden templates go in their own file, encrypted with the 복구 암호 like hidden projects, only when
 * a 복구 암호 exists and `숨긴 것도 백업` is on. Restoring merges by key (newer `updatedAt` wins,
 * nothing local is deleted).
 */
object TemplateBackup {
    const val FILE = "fieldTemplates.json"
    const val HIDDEN_FILE = "fieldTemplates-hidden.bin"

    fun plan(templates: List<FieldTemplate>, settings: AppSettings, recoverySet: Boolean): TemplateBackupPlan {
        val (hidden, visible) = templates.partition { it.hidden }
        val include = recoverySet && settings.backupHidden
        return TemplateBackupPlan(visible, if (include) hidden else emptyList(), hiddenLeftOut = hidden.isNotEmpty() && !include)
    }

    fun encode(templates: List<FieldTemplate>): ByteArray =
        CivilightJson.encode(FieldTemplatesFile.serializer(), FieldTemplatesFile(templates = templates)).toByteArray()

    /** The templates of a downloaded file; [password] opens an encrypted one. Null when unreadable. */
    fun decode(bytes: ByteArray, encrypted: Boolean, password: CharArray?): List<FieldTemplate>? {
        val plain = if (!encrypted) {
            bytes
        } else {
            val pw = password ?: return null
            (PackageCrypto.decrypt(bytes, pw) as? DecryptResult.Ok)?.inner ?: return null
        }
        return try {
            CivilightJson.decode(FieldTemplatesFile.serializer(), plain.toString(Charsets.UTF_8)).templates
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /** Restore merge by key (CST-63). */
    fun merge(local: List<FieldTemplate>, incoming: List<FieldTemplate>): List<FieldTemplate> = FieldTemplates.mergeByKey(local, incoming)
}
