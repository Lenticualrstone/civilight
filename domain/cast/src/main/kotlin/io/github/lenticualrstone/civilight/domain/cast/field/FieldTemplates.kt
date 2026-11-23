package io.github.lenticualrstone.civilight.domain.cast.field

import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.label
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.FieldTemplate
import io.github.lenticualrstone.civilight.model.app.TemplateField
import io.github.lenticualrstone.civilight.model.cast.CalcSpec
import io.github.lenticualrstone.civilight.model.cast.DatePrecision
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldLinkTargetKind
import io.github.lenticualrstone.civilight.model.cast.FieldOption
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.FieldBundle
import io.github.lenticualrstone.civilight.model.project.Project

/** One template field the apply preview reports, with the existing field it matched (if any). */
data class TemplatePreviewRow(val field: TemplateField, val name: String, val typeLabel: String)

/** What applying a template would do for one kind (CST-60), computed before anything is written. */
data class TemplateKindPreview(
    val kind: EntityKind,
    /** `더할 항목`: names in template order. */
    val added: List<TemplatePreviewRow>,
    /** `이미 있음`. */
    val present: List<TemplatePreviewRow>,
    /** `◇ 같은 이름 · 다른 종류`: never added, never changed. */
    val clashes: List<TemplatePreviewRow>,
)

/** The apply preview over the kinds asked for, in tab order. */
data class TemplatePreview(val kinds: List<TemplateKindPreview>) {
    val addedCount: Int get() = kinds.sumOf { it.added.size }
    fun of(kind: EntityKind): TemplateKindPreview? = kinds.firstOrNull { it.kind == kind }
}

/** What `항목 더하기` with a template did. */
data class TemplateApplyResult(val doc: ProjectDoc, val added: List<FieldDef>)

/** A `계산` field in the save sheet whose source is left out (`◇ 나이 · 출생 없이 · 직접 넣기`). */
data class CalcWithoutSource(val calcKey: Key, val calcName: String, val sourceName: String)

/**
 * 인물 템플릿 (CST-56 … CST-64): app-level, named field sets of any of the four kinds. Pure: every
 * function takes the templates or a document and returns new values; the store and the project
 * session write them. A template never holds values, project names or project keys, and saving one
 * never writes to the source project (matching back goes through [TemplateField.sourceDefKeys]).
 */
object FieldTemplates {
    /** Tab order of the kinds (`인물 · 장소 · 용어 · 세력`). */
    val KINDS: List<EntityKind> = listOf(EntityKind.CHARACTER, EntityKind.PLACE, EntityKind.TERM, EntityKind.FACTION)

    /** `숨긴 템플릿`: the lock row, the switch and the bundle name a hidden template leaves (CST-62). */
    const val HIDDEN_NAME = "숨긴 템플릿"
    const val NO_FIELDS = "항목 없음"
    const val ORIGIN_PREFIX = "tpl:"

    // --- Visibility (CST-62) ---

    /** Live templates the viewer may see: hidden ones only while the hidden group is open; by order. */
    fun visible(templates: List<FieldTemplate>, hiddenUnlocked: Boolean): List<FieldTemplate> =
        templates.filter { it.deletedAt == null && (!it.hidden || hiddenUnlocked) }.sortedWith(compareBy({ it.order }, { it.key }))

    /** `지운 템플릿` rows the viewer may see, newest first; hidden ones only while the group is open. */
    fun trashed(templates: List<FieldTemplate>, hiddenUnlocked: Boolean): List<FieldTemplate> =
        templates.filter { it.deletedAt != null && (!it.hidden || hiddenUnlocked) }.sortedByDescending { it.deletedAt }

    /** Whether the `숨긴 템플릿 · 지문으로 보기` lock row shows (no count is ever given). */
    fun showsLockRow(templates: List<FieldTemplate>, hiddenUnlocked: Boolean): Boolean =
        !hiddenUnlocked && templates.any { it.hidden && it.deletedAt == null }

    /** A template by key, only when the viewer may see it (a hidden one while locked reads as gone). */
    fun visibleByKey(templates: List<FieldTemplate>, key: Key?, hiddenUnlocked: Boolean): FieldTemplate? =
        key?.let { k -> templates.firstOrNull { it.key == k && it.deletedAt == null && (!it.hidden || hiddenUnlocked) } }

    /** The name to show for [template]: [HIDDEN_NAME] while it is hidden and the group is locked. */
    fun shownName(template: FieldTemplate, hiddenUnlocked: Boolean): String =
        if (template.hidden && !hiddenUnlocked) HIDDEN_NAME else template.name

    /** The name a project keeps in `시작 묶음 · <name>`: never a hidden template's own name. */
    fun bundleName(template: FieldTemplate): String = if (template.hidden) HIDDEN_NAME else template.name

    /** `◇ 같은 이름 템플릿 있음` when a visible live template has [name] (NFC, trimmed). */
    fun hasSameName(visible: List<FieldTemplate>, name: String, exceptKey: Key? = null): Boolean {
        val n = name.trim().nfc()
        return n.isNotEmpty() && visible.any { it.key != exceptKey && it.deletedAt == null && it.name.nfc() == n }
    }

    /** `인물 11 · 장소 2 · 세력 1`, kinds with fields only; `항목 없음` when empty. */
    fun kindCounts(template: FieldTemplate): String {
        val parts = KINDS.mapNotNull { kind ->
            val n = template.fields.count { it.entityKind.known == kind }
            if (n == 0) null else "${kind.label} $n"
        }
        return if (parts.isEmpty()) NO_FIELDS else CastCopy.joinAll(parts)
    }

    /** Fields of one kind in order. */
    fun fieldsOf(template: FieldTemplate, kind: EntityKind): List<TemplateField> =
        template.fields.filter { it.entityKind.known == kind }.sortedWith(compareBy({ it.order }, { it.key }))

    // --- Save (CST-57) ---

    /** Definitions a template may take for [kind]: live project fields (not `이 인물만`), hidden ones included. */
    fun candidates(doc: ProjectDoc, kind: EntityKind): List<FieldDef> = FieldSettings.projectFields(doc, kind)

    /** The save sheet starts with every candidate checked except hidden definitions. */
    fun defaultPicks(doc: ProjectDoc): Set<Key> =
        KINDS.flatMap { candidates(doc, it) }.filterNot { it.definitionHidden }.mapTo(LinkedHashSet()) { it.key }

    /** `이 인물만 항목 N · 빠짐`. */
    fun scopedCount(doc: ProjectDoc): Int = doc.project.fieldDefs.count { it.deletedAt == null && it.scopeCharacterKey != null }

    /** Picked `계산` fields whose (live) source is not picked (TC-F-33). */
    fun calcWithoutSource(doc: ProjectDoc, picks: Set<Key>): List<CalcWithoutSource> =
        KINDS.flatMap { candidates(doc, it) }.filter { it.key in picks && it.type.known == FieldType.CALC }.mapNotNull { calc ->
            val source = FieldReader.liveCalcSource(doc, calc) ?: return@mapNotNull null
            if (source.key in picks) null else CalcWithoutSource(calc.key, calc.name, source.name)
        }

    /** A new template from the picked definitions (CST-57 `새 템플릿`); [after] = the last template's order. */
    fun fromProject(doc: ProjectDoc, picks: Set<Key>, name: String, hidden: Boolean, ctx: EditContext, after: String? = null): FieldTemplate {
        val clean = name.trim().nfc()
        require(clean.isNotEmpty()) { "template name is empty" }
        return FieldTemplate(
            key = ctx.newKey(),
            name = clean,
            hidden = hidden,
            fields = templateFields(doc, picks, emptyList(), ctx),
            order = OrderKeys.after(after),
            createdAt = ctx.now,
            updatedAt = ctx.now,
        )
    }

    /**
     * `있는 템플릿 바꾸기` (CST-57, TC-F-45): the template's field list becomes the picked definitions. A
     * definition whose key is in a template field's `sourceDefKeys`, or whose bundle id equals its
     * origin, keeps that field's key and origin; the others become new fields. [name] renames it.
     */
    fun replaceFields(template: FieldTemplate, doc: ProjectDoc, picks: Set<Key>, ctx: EditContext, name: String? = null, hidden: Boolean? = null): FieldTemplate =
        template.copy(
            name = name?.trim()?.nfc()?.takeIf { it.isNotEmpty() } ?: template.name,
            hidden = hidden ?: template.hidden,
            fields = templateFields(doc, picks, template.fields, ctx),
            updatedAt = ctx.now,
        )

    private fun templateFields(doc: ProjectDoc, picks: Set<Key>, existing: List<TemplateField>, ctx: EditContext): List<TemplateField> {
        val used = HashSet<Key>()
        val usedOrigins = HashSet<String>()
        // Picked definition key → its template field key, so `계산` sources can be wired after.
        val keyOf = HashMap<Key, Key>()
        val built = ArrayList<Pair<FieldDef, TemplateField>>()
        for (kind in KINDS) {
            val defs = candidates(doc, kind).filter { it.key in picks }
            val orders = OrderKeys.evenlySpaced(defs.size)
            defs.forEachIndexed { i, def ->
                val match = existing.firstOrNull { it.key !in used && it.entityKind.known == kind && def.key in it.sourceDefKeys }
                    ?: existing.firstOrNull { it.key !in used && it.entityKind.known == kind && def.bundleOrigin != null && it.origin == def.bundleOrigin }
                val key = match?.key ?: ctx.newKey()
                used += key
                val origin = match?.origin
                    ?: def.bundleOrigin?.takeIf { it !in usedOrigins && existing.none { e -> e.origin == it && e.key !in used } }
                    ?: "$ORIGIN_PREFIX$key"
                usedOrigins += origin
                keyOf[def.key] = key
                built += def to fieldOf(def, key, origin, orders[i], ((match?.sourceDefKeys.orEmpty()) + def.key).distinct())
            }
        }
        return built.map { (def, field) ->
            val source = def.calc?.sourceFieldDefKey?.takeIf { def.type.known == FieldType.CALC }
            field.copy(calcSourceKey = source?.let { keyOf[it] })
        }
    }

    private fun fieldOf(def: FieldDef, key: Key, origin: String, order: String, sourceDefKeys: List<Key>): TemplateField {
        val type = def.type.known
        return TemplateField(
            key = key,
            origin = origin,
            entityKind = def.entityKind,
            name = def.name,
            exportName = def.exportName,
            type = def.type,
            order = order,
            inHeader = def.inHeader,
            inList = def.inList,
            inFilter = def.inFilter,
            isSecret = def.isSecret,
            shortTextMultiple = def.shortTextMultiple,
            unit = def.unit,
            allowFreeText = def.allowFreeText,
            options = if (type != null && FieldTypes.hasOptions(type)) def.options.orEmpty().sortedWith(compareBy({ it.order }, { it.key })).map { it.label } else emptyList(),
            datePrecision = def.datePrecision,
            linkTargetKind = def.linkTargetKind,
            sourceDefKeys = sourceDefKeys,
        )
    }

    // --- Apply (CST-60, CST-61) ---

    private sealed interface Match {
        data class Present(val def: FieldDef) : Match
        data object Clash : Match
        data object Missing : Match
    }

    private class Matcher(doc: ProjectDoc, kind: EntityKind) {
        private val existing = FieldDefinitions.live(doc, kind)
        private val byKey = existing.associateBy { it.key }
        private val byOrigin = existing.filter { it.bundleOrigin != null }.associateBy { it.bundleOrigin!! }
        private val byName = existing.associateBy { it.name.nfc() }

        fun match(f: TemplateField): Match {
            f.sourceDefKeys.firstNotNullOfOrNull { byKey[it] }?.let { return Match.Present(it) }
            byOrigin[f.origin]?.let { return Match.Present(it) }
            val same = byName[f.name.nfc()] ?: return Match.Missing
            return if (same.type.raw == f.type.raw) Match.Present(same) else Match.Clash
        }
    }

    /** The apply preview (TC-F-34): kinds in tab order, only those in [kinds] with template fields. */
    fun preview(doc: ProjectDoc, template: FieldTemplate, kinds: Set<EntityKind>): TemplatePreview = TemplatePreview(
        KINDS.filter { it in kinds }.mapNotNull { kind ->
            val fields = fieldsOf(template, kind)
            if (fields.isEmpty()) return@mapNotNull null
            val matcher = Matcher(doc, kind)
            val added = ArrayList<TemplatePreviewRow>()
            val present = ArrayList<TemplatePreviewRow>()
            val clashes = ArrayList<TemplatePreviewRow>()
            for (f in fields) {
                val row = TemplatePreviewRow(f, f.name, f.type.known?.let(FieldTypes::label) ?: f.type.raw)
                when (matcher.match(f)) {
                    is Match.Present -> present += row
                    Match.Clash -> clashes += row
                    Match.Missing -> added += row
                }
            }
            TemplateKindPreview(kind, added, present, clashes)
        },
    )

    /** Kinds other than [kind] where applying would add something (the `장소 · 용어 · 세력도` checkbox). */
    fun otherKindsWithAdds(doc: ProjectDoc, template: FieldTemplate, kind: EntityKind): List<TemplateKindPreview> =
        preview(doc, template, KINDS.toSet() - kind).kinds.filter { it.added.isNotEmpty() }

    /**
     * `항목 더하기` with a template (CST-60): missing fields of [kinds] are added at the end of each
     * kind in template order, with new keys and option keys and `bundleOrigin` = the template origin.
     * Existing fields, options and values never change; clashes are skipped. Each kind that got
     * fields shows `시작 묶음 · <name>` ([bundleName]).
     */
    fun apply(doc: ProjectDoc, template: FieldTemplate, kinds: Set<EntityKind>, ctx: EditContext): TemplateApplyResult {
        var next = doc
        val added = ArrayList<FieldDef>()
        val named = HashSet<EntityKind>()
        for (kind in KINDS.filter { it in kinds }) {
            val fields = fieldsOf(template, kind)
            if (fields.isEmpty()) continue
            val matcher = Matcher(next, kind)
            val verdicts = fields.associate { it.key to matcher.match(it) }
            val bundle = fields.map { bundleFieldOf(it, kind, template) }
            val byOrigin = fields.associateBy { it.origin }
            val result = FieldBundles.addFields(
                next, bundle, kind, ctx,
                match = { b -> (verdicts[byOrigin.getValue(b.origin).key] as? Match.Present)?.def },
                skip = { b -> verdicts[byOrigin.getValue(b.origin).key] == Match.Clash },
            )
            if (result.added.isNotEmpty()) {
                next = result.doc
                added += result.added
                named += kind
            }
        }
        if (named.isNotEmpty()) next = withBundleName(next, named, bundleName(template), ctx)
        return TemplateApplyResult(next, added)
    }

    /**
     * New-project step 4 (CST-61): every kind at once; `fieldBundleAtCreation = TEMPLATE` and the
     * character bundle name is the template's ([HIDDEN_NAME] for a hidden one).
     */
    fun forProjectCreation(doc: ProjectDoc, template: FieldTemplate, ctx: EditContext): ProjectDoc {
        val applied = apply(doc, template, KINDS.toSet(), ctx).doc
        val named = withBundleName(applied, setOf(EntityKind.CHARACTER), bundleName(template), ctx)
        return named.copy(project = named.project.copy(fieldBundleAtCreation = FieldBundle.TEMPLATE.open()))
    }

    private fun bundleFieldOf(f: TemplateField, kind: EntityKind, template: FieldTemplate): BundleField {
        val type = f.type.known ?: FieldType.SHORT_TEXT
        val source = f.calcSourceKey?.let { k -> template.fields.firstOrNull { it.key == k && it.entityKind.known == kind } }
        return BundleField(
            origin = f.origin,
            field = NewField(
                name = f.name, type = type, entityKind = kind, exportName = f.exportName,
                inHeader = f.inHeader, inList = f.inList, inFilter = f.inFilter, isSecret = f.isSecret,
                shortTextMultiple = f.shortTextMultiple, unit = f.unit, options = f.options,
                allowFreeText = f.allowFreeText, datePrecision = f.datePrecision?.known,
                linkTargetKind = f.linkTargetKind?.known, bundleOrigin = f.origin,
            ),
            calcSourceOrigin = source?.origin,
        )
    }

    private fun withBundleName(doc: ProjectDoc, kinds: Set<EntityKind>, name: String, ctx: EditContext): ProjectDoc {
        var names = doc.project.cast.bundleName
        for (kind in kinds) {
            names = when (kind) {
                EntityKind.CHARACTER -> names.copy(character = name)
                EntityKind.PLACE -> names.copy(place = name)
                EntityKind.TERM -> names.copy(term = name)
                EntityKind.FACTION -> names.copy(faction = name)
            }
        }
        val p = doc.project
        return doc.copy(project = p.copy(cast = p.cast.copy(bundleName = names), updatedAt = ctx.now, updatedBy = ctx.device))
    }

    // --- Template editor (CST-59) over a field-only document ---

    /** Project key of the scratch document of a template (never a real project). */
    fun scratchKey(template: FieldTemplate): Key = "template:${template.key}"

    /**
     * A field-only [ProjectDoc] standing for [template], so the project editor's models run on it:
     * each template field becomes a definition with the same key, `bundleOrigin` = its origin, and
     * option keys derived from the field key.
     */
    fun toScratchDoc(template: FieldTemplate): ProjectDoc {
        val defs = template.fields.mapNotNull { f ->
            val kind = f.entityKind.known ?: return@mapNotNull null
            val type = f.type.known
            val optionOrders = OrderKeys.evenlySpaced(f.options.size)
            FieldDef(
                key = f.key,
                entityKind = kind.open(),
                name = f.name,
                exportName = f.exportName,
                type = f.type,
                order = f.order,
                inHeader = f.inHeader,
                inList = f.inList,
                inFilter = f.inFilter,
                isSecret = f.isSecret,
                shortTextMultiple = f.shortTextMultiple,
                unit = f.unit,
                options = if (type != null && FieldTypes.hasOptions(type)) f.options.mapIndexed { i, label -> FieldOption(optionKey(f.key, i, f.options.size), label, optionOrders[i]) } else null,
                allowFreeText = f.allowFreeText,
                datePrecision = f.datePrecision,
                linkTargetKind = f.linkTargetKind,
                calc = if (type == FieldType.CALC) f.calcSourceKey?.let { CalcSpec(sourceFieldDefKey = it) } else null,
                bundleOrigin = f.origin,
            )
        }
        return ProjectDoc(Project(key = scratchKey(template), name = template.name, fieldDefs = defs))
    }

    /**
     * Key of option [index] of [count] in a scratch document. It changes when the count changes, so
     * an editor keyed by option never shows a removed option's text in the next one's place.
     */
    fun optionKey(fieldKey: Key, index: Int, count: Int): Key = "$fieldKey:o$index/$count"

    /**
     * The template after editing its scratch document: live definitions in order; keys, origins and
     * `sourceDefKeys` survive; a new definition gets `tpl:<key>`; a `계산` whose source is gone has none.
     */
    fun fromScratchDoc(doc: ProjectDoc, template: FieldTemplate, now: Timestamp): FieldTemplate {
        val old = template.fields.associateBy { it.key }
        val live = doc.project.fieldDefs.filter { it.deletedAt == null && it.scopeCharacterKey == null }
        val liveKeys = live.mapTo(HashSet()) { it.key }
        val fields = KINDS.flatMap { kind ->
            live.filter { it.entityKind.known == kind }.sortedWith(compareBy({ it.order }, { it.key })).map { def ->
                val origin = old[def.key]?.origin ?: def.bundleOrigin ?: "$ORIGIN_PREFIX${def.key}"
                fieldOf(def, def.key, origin, def.order, old[def.key]?.sourceDefKeys.orEmpty())
                    .copy(calcSourceKey = def.calc?.sourceFieldDefKey?.takeIf { def.type.known == FieldType.CALC && it in liveKeys })
            }
        }
        if (fields == template.fields) return template
        return template.copy(fields = fields, updatedAt = now)
    }

    /** Every type is offered in a template: there are no values to convert (TC-F-44). */
    val ALL_TYPES: List<FieldType> = FieldType.entries.toList()

    /** Date fields of the kind a template `계산` may use as its source. */
    fun calcSources(doc: ProjectDoc, kind: EntityKind, exceptKey: Key): List<FieldDef> =
        FieldDefinitions.live(doc, kind).filter { it.key != exceptKey && it.type.known == FieldType.DATE }

    /**
     * Type change in a template field (no values, so no `◇` preview): type settings reset to the new
     * type's defaults; `연결` takes [linkTarget], `계산` takes [calcSource] (null = typed age).
     */
    fun setType(
        doc: ProjectDoc,
        defKey: Key,
        type: FieldType,
        ctx: EditContext,
        linkTarget: FieldLinkTargetKind? = null,
        calcSource: Key? = null,
    ): ProjectDoc = FieldDefinitions.update(doc, defKey, ctx) { d ->
        d.copy(
            type = type.open(),
            inFilter = d.inFilter && FieldTypes.canFilter(type),
            shortTextMultiple = if (type == FieldType.SHORT_TEXT) d.shortTextMultiple ?: false else null,
            unit = if (type == FieldType.NUMBER) d.unit else null,
            options = if (FieldTypes.hasOptions(type)) d.options.orEmpty() else null,
            allowFreeText = if (type == FieldType.CHOICE) d.allowFreeText ?: false else null,
            datePrecision = if (type == FieldType.DATE) d.datePrecision ?: DatePrecision.YEAR.open() else null,
            linkTargetKind = if (type == FieldType.LINK) (linkTarget ?: d.linkTargetKind?.known ?: FieldLinkTargetKind.FACTION).open() else null,
            calc = if (type == FieldType.CALC) calcSource?.let { CalcSpec(sourceFieldDefKey = it) } else null,
        )
    }

    /** `계산` source in a template (`없음` = null). */
    fun setCalcSource(doc: ProjectDoc, defKey: Key, source: Key?, ctx: EditContext): ProjectDoc =
        FieldDefinitions.update(doc, defKey, ctx) { d -> d.copy(calc = source?.let { CalcSpec(sourceFieldDefKey = it) }) }

    /** Option `빼기` in a template: removed at once (no one holds it). */
    fun removeOption(doc: ProjectDoc, defKey: Key, optionKey: Key, ctx: EditContext): ProjectDoc =
        FieldDefinitions.update(doc, defKey, ctx) { d -> d.copy(options = d.options.orEmpty().filterNot { it.key == optionKey }) }

    /**
     * Field `빼기` from a template (`이 템플릿에서만 · 프로젝트 그대로`): the definition goes, and a `계산`
     * that used it loses its source.
     */
    fun removeField(doc: ProjectDoc, defKey: Key, ctx: EditContext): ProjectDoc {
        val defs = doc.project.fieldDefs.filterNot { it.key == defKey }.map { d ->
            if (d.calc?.sourceFieldDefKey == defKey) d.copy(calc = null, updatedAt = ctx.now, updatedBy = ctx.device) else d
        }
        return FieldDefinitions.withDefs(doc, defs, ctx)
    }

    // --- New template (CST-58 `템플릿 만들기`) ---

    /** An empty template (or one starting from [fields]); [after] = the last template's order. */
    fun create(name: String, hidden: Boolean, ctx: EditContext, after: String? = null, fields: List<TemplateField> = emptyList()): FieldTemplate {
        val clean = name.trim().nfc()
        require(clean.isNotEmpty()) { "template name is empty" }
        return FieldTemplate(key = ctx.newKey(), name = clean, hidden = hidden, fields = fields, order = OrderKeys.after(after), createdAt = ctx.now, updatedAt = ctx.now)
    }

    /** `기본` · `게임 프로필` as template fields (origins kept; `비어 있음` gives none). */
    fun fieldsFromBundle(bundle: FieldBundle, ctx: EditContext): List<TemplateField> {
        val scratch = FieldBundles.apply(ProjectDoc(Project(key = "bundle", name = "bundle")), bundle, ctx).doc
        return templateFields(scratch, scratch.project.fieldDefs.mapTo(HashSet()) { it.key }, emptyList(), ctx).map { it.copy(sourceDefKeys = emptyList()) }
    }

    /** A copy of another template's fields with new keys (origins kept, so projects still match them). */
    fun copyFields(from: FieldTemplate, ctx: EditContext): List<TemplateField> {
        val keys = from.fields.associate { it.key to ctx.newKey() }
        return from.fields.map { f -> f.copy(key = keys.getValue(f.key), calcSourceKey = f.calcSourceKey?.let { keys[it] }) }
    }

    // --- Trash (CST-58, CST-59) ---

    fun delete(template: FieldTemplate, now: Timestamp): FieldTemplate = template.copy(deletedAt = now, updatedAt = now)

    fun restore(template: FieldTemplate, now: Timestamp): FieldTemplate = template.copy(deletedAt = null, updatedAt = now)

    fun rename(template: FieldTemplate, name: String, now: Timestamp): FieldTemplate {
        val clean = name.trim().nfc()
        return if (clean.isEmpty() || clean == template.name) template else template.copy(name = clean, updatedAt = now)
    }

    fun setHidden(template: FieldTemplate, hidden: Boolean, now: Timestamp): FieldTemplate =
        if (template.hidden == hidden) template else template.copy(hidden = hidden, updatedAt = now)

    // --- Backup merge (CST-63) ---

    /** Restore merge by key: a newer `updatedAt` wins; nothing local is deleted. */
    fun mergeByKey(local: List<FieldTemplate>, incoming: List<FieldTemplate>): List<FieldTemplate> {
        val byKey = LinkedHashMap<Key, FieldTemplate>()
        local.forEach { byKey[it.key] = it }
        incoming.forEach { t -> val mine = byKey[t.key]; if (mine == null || t.updatedAt > mine.updatedAt) byKey[t.key] = t }
        return byKey.values.toList()
    }
}
