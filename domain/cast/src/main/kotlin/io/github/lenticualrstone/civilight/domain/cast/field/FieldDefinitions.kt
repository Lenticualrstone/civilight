package io.github.lenticualrstone.civilight.domain.cast.field

import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.label
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.cast.CalcSpec
import io.github.lenticualrstone.civilight.model.cast.DatePrecision
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FallbackReason
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldLinkTargetKind
import io.github.lenticualrstone.civilight.model.cast.FieldOption
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import io.github.lenticualrstone.civilight.model.trash.TrashKind

/** Input of the `항목 넣기` sheet (CST-21, CST-22). */
data class NewField(
    val name: String,
    val type: FieldType,
    val entityKind: EntityKind = EntityKind.CHARACTER,
    val exportName: String? = null,
    val inHeader: Boolean = false,
    val inList: Boolean = false,
    val inFilter: Boolean = false,
    val isSecret: Boolean = false,
    val shortTextMultiple: Boolean? = null,
    val unit: String? = null,
    val options: List<String> = emptyList(),
    val allowFreeText: Boolean? = null,
    val datePrecision: DatePrecision? = null,
    val linkTargetKind: FieldLinkTargetKind? = null,
    val calcSourceKey: Key? = null,
    /** `이 인물만` (CST-22); null = `모든 인물에`. */
    val scopeCharacterKey: Key? = null,
    val bundleOrigin: String? = null,
)

/** State of the `넣기` button and the notice under the name input. */
data class NewFieldCheck(val canSave: Boolean, val sameNameNotice: String?)

/** `어디에` in `+ 항목` from a record (CST-22). */
enum class FieldScope(val title: String) {
    ALL_CHARACTERS("모든 인물에"),
    THIS_CHARACTER("이 인물만"),
}

/**
 * Field definitions (CST-19, CST-21 … CST-23). Every change to options or type rewrites the values
 * of every entity of the kind (live and trashed) and the values of matching change records.
 */
object FieldDefinitions {
    const val SAME_NAME = "◇ 같은 이름 항목 있음"
    const val UNIT_NOTE = "환산하지 않음"
    const val NUMBER_UNIT_NOTE = "값은 바꿔 계산 안 함"

    /** Live definitions of a kind in order (hidden definitions included). */
    fun live(doc: ProjectDoc, kind: EntityKind): List<FieldDef> = doc.index.fieldDefs(kind).filter { it.deletedAt == null }

    /** Live, not hidden definitions that apply to [characterKey] (project fields plus its own `이 인물만` fields). */
    fun visibleFor(doc: ProjectDoc, kind: EntityKind, entityKey: Key): List<FieldDef> = live(doc, kind)
        .filter { !it.definitionHidden && (it.scopeCharacterKey == null || it.scopeCharacterKey == entityKey) }

    /** `넣기` enabled iff the name is not blank; same-name notice when a field of the kind has the name (CST-21). */
    fun check(doc: ProjectDoc, field: NewField): NewFieldCheck {
        val name = field.name.trim().nfc()
        val same = name.isNotEmpty() && live(doc, field.entityKind).any { it.name.nfc() == name }
        return NewFieldCheck(canSave = name.isNotEmpty(), sameNameNotice = if (same) SAME_NAME else null)
    }

    /** Adds a field at the end of its kind's order. `필터` is dropped for types that cannot filter (TC-F-20). */
    fun add(doc: ProjectDoc, field: NewField, ctx: EditContext): Pair<ProjectDoc, FieldDef> {
        val name = field.name.trim().nfc()
        require(name.isNotEmpty()) { "field name is empty" }
        val last = doc.index.fieldDefs(field.entityKind).lastOrNull()?.order
        val def = FieldDef(
            key = ctx.newKey(),
            entityKind = field.entityKind.open(),
            name = name,
            exportName = field.exportName?.trim()?.takeIf { it.isNotEmpty() },
            type = field.type.open(),
            order = OrderKeys.after(last),
            inHeader = field.inHeader,
            inList = field.inList,
            inFilter = field.inFilter && FieldTypes.canFilter(field.type),
            isSecret = field.isSecret,
            scopeCharacterKey = field.scopeCharacterKey,
            shortTextMultiple = if (field.type == FieldType.SHORT_TEXT) field.shortTextMultiple ?: false else null,
            unit = if (field.type == FieldType.NUMBER) field.unit?.trim() else null,
            options = if (FieldTypes.hasOptions(field.type)) newOptions(field.options, ctx) else null,
            allowFreeText = if (field.type == FieldType.CHOICE) field.allowFreeText ?: false else null,
            datePrecision = if (field.type == FieldType.DATE) (field.datePrecision ?: DatePrecision.YEAR).open() else null,
            linkTargetKind = if (field.type == FieldType.LINK) (field.linkTargetKind ?: FieldLinkTargetKind.FACTION).open() else null,
            calc = if (field.type == FieldType.CALC) field.calcSourceKey?.let { CalcSpec(sourceFieldDefKey = it) } else null,
            bundleOrigin = field.bundleOrigin,
            updatedAt = ctx.now,
            updatedBy = ctx.device,
        )
        return withDefs(doc, doc.project.fieldDefs + def, ctx) to def
    }

    /** `+ 항목` from a record (CST-22): `모든 인물에` (default) or `이 인물만`. */
    fun addFromRecord(
        doc: ProjectDoc,
        characterKey: Key,
        name: String,
        type: FieldType,
        scope: FieldScope,
        ctx: EditContext,
    ): Pair<ProjectDoc, FieldDef> = add(
        doc,
        NewField(name = name, type = type, scopeCharacterKey = if (scope == FieldScope.THIS_CHARACTER) characterKey else null),
        ctx,
    )

    /** `모든 인물에 넣기`: a character-only field becomes a project field; values are kept. */
    fun promoteToAll(doc: ProjectDoc, defKey: Key, ctx: EditContext): ProjectDoc = update(doc, defKey, ctx) { it.copy(scopeCharacterKey = null) }

    /** Rename (`키 → 신장`): values, history and export name are kept (TC-F-11). */
    fun rename(doc: ProjectDoc, defKey: Key, name: String, ctx: EditContext): ProjectDoc {
        val n = name.trim().nfc()
        require(n.isNotEmpty()) { "field name is empty" }
        return update(doc, defKey, ctx) { it.copy(name = n) }
    }

    fun setExportName(doc: ProjectDoc, defKey: Key, exportName: String?, ctx: EditContext): ProjectDoc =
        update(doc, defKey, ctx) { it.copy(exportName = exportName?.trim()?.takeIf { s -> s.isNotEmpty() }) }

    /** Grip-handle reorder: places [defKey] between [beforeKey] and [afterKey] (neighbours after the move). */
    fun move(doc: ProjectDoc, defKey: Key, afterKey: Key?, beforeKey: Key?, ctx: EditContext): ProjectDoc {
        val a = afterKey?.let { doc.index.fieldDef(it)?.order }
        val b = beforeKey?.let { doc.index.fieldDef(it)?.order }
        return update(doc, defKey, ctx) { it.copy(order = OrderKeys.between(a, b)) }
    }

    /** Unit change never converts numbers (TC-F-10); the sheet shows [UNIT_NOTE]. */
    fun setUnit(doc: ProjectDoc, defKey: Key, unit: String?, ctx: EditContext): ProjectDoc =
        update(doc, defKey, ctx) { it.copy(unit = unit?.trim()?.takeIf { s -> s.isNotEmpty() }) }

    /** `보이는 곳` `머리 · 목록 · 필터`; `필터` stays off for types that cannot filter. */
    fun setPlaces(doc: ProjectDoc, defKey: Key, inHeader: Boolean, inList: Boolean, inFilter: Boolean, ctx: EditContext): ProjectDoc =
        update(doc, defKey, ctx) { d ->
            d.copy(inHeader = inHeader, inList = inList, inFilter = inFilter && d.type.known?.let(FieldTypes::canFilter) == true)
        }

    fun setSecret(doc: ProjectDoc, defKey: Key, secret: Boolean, ctx: EditContext): ProjectDoc = update(doc, defKey, ctx) { it.copy(isSecret = secret) }

    /** `숨기기` / unhide: values stay and come back (TC-F-21). */
    fun setHidden(doc: ProjectDoc, defKey: Key, hidden: Boolean, ctx: EditContext): ProjectDoc =
        update(doc, defKey, ctx) { it.copy(definitionHidden = hidden) }

    fun setAllowFreeText(doc: ProjectDoc, defKey: Key, allow: Boolean, ctx: EditContext): ProjectDoc =
        update(doc, defKey, ctx) { it.copy(allowFreeText = allow) }

    // --- Options ---

    fun addOption(doc: ProjectDoc, defKey: Key, label: String, ctx: EditContext): ProjectDoc = update(doc, defKey, ctx) { d ->
        val options = d.options.orEmpty()
        d.copy(options = options + FieldOption(ctx.newKey(), label.trim().nfc(), OrderKeys.after(options.maxOfOrNull { it.order })))
    }

    /** Option rename (`여 → 여성`): every value with the option shows the new label (TC-F-09). */
    fun renameOption(doc: ProjectDoc, defKey: Key, optionKey: Key, label: String, ctx: EditContext): ProjectDoc =
        update(doc, defKey, ctx) { d ->
            d.copy(options = d.options.orEmpty().map { if (it.key == optionKey) it.copy(label = label.trim().nfc()) else it })
        }

    /** Names of live entities whose base value or any record uses the option, in entity order. */
    fun usersOfOption(doc: ProjectDoc, defKey: Key, optionKey: Key): List<String> {
        val def = doc.index.fieldDef(defKey) ?: return emptyList()
        val kind = def.entityKind.known ?: return emptyList()
        fun uses(v: FieldValue?) = (v?.payload as? FieldPayload.Choice)?.optionKeys?.contains(optionKey) == true
        val byRecord = doc.changes.records.filter { r ->
            val t = r.target as? ChangeTarget.Field
            t != null && t.fieldDefKey == defKey && doc.liveness.isLive(r) && uses(r.valueAs(FieldValue.serializer()))
        }.mapTo(HashSet()) { (it.target as ChangeTarget.Field).entityKey }
        return liveEntities(doc, kind).filter { (key, _, fields) -> uses(fields[defKey]) || key in byRecord }.map { it.second }
    }

    /** `리엔 외 3명` (TC-F-08); the single name when one; empty when none. */
    fun namesSummary(names: List<String>): String = when (names.size) {
        0 -> ""
        1 -> names[0]
        else -> "${names[0]} 외 ${names.size - 1}명"
    }

    /**
     * Option delete (CST-23): every value holding the option keeps its visible text as fallback
     * with `◇ 보기에 없는 값` (a multi-choice value keeps all its labels as one text).
     */
    fun removeOption(doc: ProjectDoc, defKey: Key, optionKey: Key, ctx: EditContext): ProjectDoc {
        val def = doc.index.fieldDef(defKey) ?: return doc
        val kind = def.entityKind.known ?: return doc
        val convert: (FieldValue) -> FieldValue = { v ->
            val p = v.payload as? FieldPayload.Choice
            if (p == null || optionKey !in p.optionKeys) {
                v
            } else {
                FieldValue.fallback(FieldText.payloadText(doc, def, p), FallbackReason.OPTION_REMOVED)
            }
        }
        val rewritten = mapValues(doc, kind, defKey, convert)
        return update(rewritten, defKey, ctx) { d -> d.copy(options = d.options.orEmpty().filterNot { it.key == optionKey }) }
    }

    // --- Delete and restore ---

    /** Live entities with a value (base, any state) or a live record for the field (`값이 있는 인물 12명`). */
    fun valueHolderCount(doc: ProjectDoc, defKey: Key): Int {
        val def = doc.index.fieldDef(defKey) ?: return 0
        val kind = def.entityKind.known ?: return 0
        val byRecord = doc.changes.records.filter { r ->
            val t = r.target as? ChangeTarget.Field
            t != null && t.fieldDefKey == defKey && r.deletedAt == null
        }.mapTo(HashSet()) { (it.target as ChangeTarget.Field).entityKey }
        return liveEntities(doc, kind).count { (key, _, fields) -> fields.containsKey(defKey) || key in byRecord }
    }

    /** Delete sheet line (TC-F-12). */
    fun deleteSummary(doc: ProjectDoc, defKey: Key): String {
        val def = doc.index.fieldDef(defKey)
        val word = def?.entityKind?.known?.label ?: "인물"
        return "값이 있는 $word ${valueHolderCount(doc, defKey)}명"
    }

    /**
     * `지우기`: the definition gets a trash mark and a `fieldDef` trash entry (filter `인물`) for
     * 30 days. Values and records stop being live by derivation and come back on restore.
     */
    fun delete(doc: ProjectDoc, defKey: Key, ctx: EditContext): ProjectDoc {
        val def = doc.index.fieldDef(defKey) ?: return doc
        if (def.deletedAt != null) return doc
        val entry = TrashEntry(
            key = ctx.newKey(),
            kind = TrashKind.FIELD_DEF.open(),
            projectKey = doc.project.key,
            targetKeys = listOf(defKey),
            deletedAt = ctx.now,
            purgeOn = TrashEntry.purgeDateFor(ctx.today),
        )
        val marked = update(doc, defKey, ctx) { it.copy(deletedAt = ctx.now, trashEntryKey = entry.key) }
        return marked.copy(trash = marked.trash.copy(entries = marked.trash.entries + entry))
    }

    /** Restore a deleted definition: values and `◆` records are live again (TC-F-12). */
    fun restore(doc: ProjectDoc, defKey: Key, ctx: EditContext): ProjectDoc {
        val def = doc.index.fieldDef(defKey) ?: return doc
        val entryKey = def.trashEntryKey
        val restored = update(doc, defKey, ctx) { it.copy(deletedAt = null, trashEntryKey = null) }
        return restored.copy(trash = restored.trash.copy(entries = restored.trash.entries.filterNot { it.key == entryKey }))
    }

    // --- Helpers ---

    internal fun update(doc: ProjectDoc, defKey: Key, ctx: EditContext, change: (FieldDef) -> FieldDef): ProjectDoc =
        withDefs(doc, doc.project.fieldDefs.map { if (it.key == defKey) change(it).copy(updatedAt = ctx.now, updatedBy = ctx.device) else it }, ctx)

    internal fun withDefs(doc: ProjectDoc, defs: List<FieldDef>, ctx: EditContext): ProjectDoc =
        doc.copy(project = doc.project.copy(fieldDefs = defs, updatedAt = ctx.now, updatedBy = ctx.device))

    internal fun newOptions(labels: List<String>, ctx: EditContext): List<FieldOption> {
        val clean = labels.map { it.trim().nfc() }.filter { it.isNotEmpty() }.distinct()
        val orders = OrderKeys.evenlySpaced(clean.size)
        return clean.mapIndexed { i, label -> FieldOption(ctx.newKey(), label, orders[i]) }
    }

    /** (key, name, fields) of live entities of a kind in their order. */
    internal fun liveEntities(doc: ProjectDoc, kind: EntityKind): List<Triple<Key, String, Map<Key, FieldValue>>> = when (kind) {
        EntityKind.CHARACTER -> doc.cast.characters.filter { it.deletedAt == null }.sortedWith(compareBy({ it.order }, { it.key }))
            .map { Triple(it.key, it.name, it.fields) }
        EntityKind.PLACE -> doc.world.places.filter { it.deletedAt == null }.sortedWith(compareBy({ it.order }, { it.key }))
            .map { Triple(it.key, it.name, it.fields) }
        EntityKind.TERM -> doc.world.terms.filter { it.deletedAt == null }.sortedWith(compareBy({ it.order }, { it.key }))
            .map { Triple(it.key, it.name, it.fields) }
        EntityKind.FACTION -> doc.world.factions.filter { it.deletedAt == null }.sortedWith(compareBy({ it.order }, { it.key }))
            .map { Triple(it.key, it.name, it.fields) }
    }

    /**
     * Applies [convert] to every stored value of the field: base values of all entities of [kind]
     * (live and trashed) and the values of all change records of the field. Unknown values are
     * passed through [convert] too, so it decides.
     */
    internal fun mapValues(doc: ProjectDoc, kind: EntityKind, defKey: Key, convert: (FieldValue) -> FieldValue?): ProjectDoc {
        val withBase = FieldValueEdits.updateAllFields(doc, kind) { fields ->
            val v = fields[defKey] ?: return@updateAllFields fields
            val next = FieldValueEdits.normalized(convert(v))
            if (next == null) fields - defKey else fields + (defKey to next)
        }
        val records = withBase.changes.records.map { r ->
            val t = r.target as? ChangeTarget.Field
            if (t == null || t.fieldDefKey != defKey) return@map r
            val v = r.valueAs(FieldValue.serializer()) ?: return@map r
            r.copy(value = FieldValueEdits.normalized(convert(v))?.let { ChangeRecord.encodeValue(FieldValue.serializer(), it) })
        }
        return withBase.copy(changes = withBase.changes.copy(records = records))
    }

    internal fun joinNames(names: List<String>): String = CastCopy.joinAll(names)
}
