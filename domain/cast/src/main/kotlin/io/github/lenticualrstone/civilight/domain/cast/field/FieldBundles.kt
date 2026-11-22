package io.github.lenticualrstone.civilight.domain.cast.field

import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.app.LibraryEntry
import io.github.lenticualrstone.civilight.model.cast.CalcSpec
import io.github.lenticualrstone.civilight.model.cast.DatePrecision
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldLinkTargetKind
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.FieldBundle

/** One field of a start bundle; [origin] is the permanent bundle id (`basic.birth`). */
data class BundleField(
    val origin: String,
    val field: NewField,
    /** For `계산`: the origin of the source field. */
    val calcSourceOrigin: String? = null,
)

/** What `항목 더하기` did. */
data class BundleResult(val doc: ProjectDoc, val added: List<FieldDef>)

/**
 * Start bundles (CST-24): `기본`, `게임 프로필`, `비어 있음`, `다른 프로젝트에서`. Applying one only
 * adds missing fields (matched by bundle id, else by name) and never changes existing fields or
 * values. The default is always `기본`.
 */
object FieldBundles {
    const val ROW_TITLE = "인물 항목"
    val DEFAULT: FieldBundle = FieldBundle.BASIC

    fun label(bundle: FieldBundle): String = when (bundle) {
        FieldBundle.BASIC -> "기본"
        FieldBundle.GAME_PROFILE -> "게임 프로필"
        FieldBundle.EMPTY -> "비어 있음"
        FieldBundle.FROM_PROJECT -> "다른 프로젝트에서"
    }

    /** Picker order. */
    val PICKER: List<FieldBundle> = listOf(FieldBundle.BASIC, FieldBundle.GAME_PROFILE, FieldBundle.EMPTY, FieldBundle.FROM_PROJECT)

    private fun f(origin: String, field: NewField, calcSourceOrigin: String? = null) = BundleField(origin, field.copy(bundleOrigin = origin), calcSourceOrigin)

    /** `기본`: 출생 (날짜 · 연도) · 나이 (계산) · 생일 (기념일) · 소속 (연결 · 세력) · 성격 (긴 글). */
    val BASIC: List<BundleField> = listOf(
        f(FieldReader.ORIGIN_BIRTH, NewField("출생", FieldType.DATE, datePrecision = DatePrecision.YEAR)),
        f(FieldReader.ORIGIN_AGE, NewField("나이", FieldType.CALC, inHeader = true, inList = true), calcSourceOrigin = FieldReader.ORIGIN_BIRTH),
        f(FieldReader.ORIGIN_BIRTHDAY, NewField("생일", FieldType.ANNIVERSARY)),
        f(
            "basic.affiliation",
            NewField("소속", FieldType.LINK, inHeader = true, inList = true, inFilter = true, linkTargetKind = FieldLinkTargetKind.FACTION),
        ),
        f("basic.personality", NewField("성격", FieldType.LONG_TEXT)),
    )

    /** `게임 프로필` = 기본 + 코드네임 · 성별 · 종족 · 출신지 · 신장 · 특기. */
    val GAME_PROFILE: List<BundleField> = BASIC + listOf(
        f("gameProfile.codename", NewField("코드네임", FieldType.SHORT_TEXT, shortTextMultiple = false)),
        f("gameProfile.gender", NewField("성별", FieldType.CHOICE, inFilter = true, options = listOf("여", "남", "밝히지 않음"), allowFreeText = false)),
        f(
            "gameProfile.species",
            NewField("종족", FieldType.CHOICE, inHeader = true, inFilter = true, options = listOf("인간", "수인", "요정", "자동인형"), allowFreeText = false),
        ),
        f("gameProfile.origin", NewField("출신지", FieldType.LINK, inFilter = true, linkTargetKind = FieldLinkTargetKind.PLACE)),
        f("gameProfile.height", NewField("신장", FieldType.NUMBER, inHeader = true, unit = "cm", exportName = "height")),
        f(
            "gameProfile.specialty",
            NewField("특기", FieldType.MULTI_CHOICE, inFilter = true, options = listOf("등불 수리", "길 찾기", "검술", "기록", "흥정", "성직자", "치료")),
        ),
    )

    /** Fields of a built-in bundle for a kind (only characters have bundles; other kinds start empty). */
    fun fields(bundle: FieldBundle, kind: EntityKind = EntityKind.CHARACTER): List<BundleField> =
        if (kind != EntityKind.CHARACTER) emptyList() else when (bundle) {
            FieldBundle.BASIC -> BASIC
            FieldBundle.GAME_PROFILE -> GAME_PROFILE
            FieldBundle.EMPTY, FieldBundle.FROM_PROJECT -> emptyList()
        }

    /** New-project step 4 row: `인물 항목 | 출생 · 나이 · 생일 · 소속 · 성격 | 기본 ›` (TC-F-15). */
    fun newProjectRow(bundle: FieldBundle = DEFAULT): Triple<String, String, String> =
        Triple(ROW_TITLE, CastCopy.joinAll(fields(bundle).map { it.field.name }), "${label(bundle)} ›")

    /** Adds the bundle's missing fields at the end, in bundle order (`항목 더하기`). */
    fun apply(doc: ProjectDoc, bundle: FieldBundle, ctx: EditContext, kind: EntityKind = EntityKind.CHARACTER): BundleResult {
        val result = addMissing(doc, fields(bundle, kind), kind, ctx)
        val names = result.doc.project.cast.bundleName
        val named = when (kind) {
            EntityKind.CHARACTER -> names.copy(character = label(bundle))
            EntityKind.PLACE -> names.copy(place = label(bundle))
            EntityKind.TERM -> names.copy(term = label(bundle))
            EntityKind.FACTION -> names.copy(faction = label(bundle))
        }
        val p = result.doc.project
        return BundleResult(result.doc.copy(project = p.copy(cast = p.cast.copy(bundleName = named))), result.added)
    }

    /**
     * `다른 프로젝트에서`: copies [source]'s field definitions of [kind] (definitions only, no values;
     * character-only fields are skipped) with new keys.
     */
    fun applyFromProject(doc: ProjectDoc, source: ProjectDoc, ctx: EditContext, kind: EntityKind = EntityKind.CHARACTER): BundleResult {
        val defs = source.index.fieldDefs(kind).filter { it.deletedAt == null && it.scopeCharacterKey == null }
        val originOf = defs.associate { it.key to (it.bundleOrigin ?: "project:${it.key}") }
        val bundle = defs.map { d ->
            val type = d.type.known ?: FieldType.SHORT_TEXT
            BundleField(
                origin = originOf.getValue(d.key),
                field = NewField(
                    name = d.name, type = type, entityKind = kind, exportName = d.exportName,
                    inHeader = d.inHeader, inList = d.inList, inFilter = d.inFilter, isSecret = d.isSecret,
                    shortTextMultiple = d.shortTextMultiple, unit = d.unit,
                    options = d.options.orEmpty().sortedBy { it.order }.map { it.label },
                    allowFreeText = d.allowFreeText, datePrecision = d.datePrecision?.known,
                    linkTargetKind = d.linkTargetKind?.known, bundleOrigin = d.bundleOrigin,
                ),
                calcSourceOrigin = d.calc?.sourceFieldDefKey?.let { originOf[it] },
            )
        }
        return addMissing(doc, bundle, kind, ctx, keepOrigin = { b -> b.field.bundleOrigin })
    }

    /** Projects offered by `다른 프로젝트에서`: not this one, not in the trash; hidden ones only while unlocked (TC-F-16). */
    fun sourceProjects(entries: List<LibraryEntry>, currentKey: Key, hiddenUnlocked: Boolean): List<LibraryEntry> =
        entries.filter { it.key != currentKey && it.deletedAt == null && (!it.hidden || hiddenUnlocked) }

    private fun addMissing(
        doc: ProjectDoc,
        bundle: List<BundleField>,
        kind: EntityKind,
        ctx: EditContext,
        keepOrigin: (BundleField) -> String? = { it.origin },
    ): BundleResult {
        val existing = FieldDefinitions.live(doc, kind)
        val byOrigin = existing.filter { it.bundleOrigin != null }.associateBy { it.bundleOrigin!! }
        val byName = existing.associateBy { it.name.nfc() }
        fun match(b: BundleField): FieldDef? = byOrigin[b.origin] ?: b.field.bundleOrigin?.let { byOrigin[it] } ?: byName[b.field.name.nfc()]

        val missing = bundle.filter { match(it) == null }
        if (missing.isEmpty()) return BundleResult(doc, emptyList())
        val orders = OrderKeys.between(existing.maxOfOrNull { it.order }, null, missing.size)
        val newKeys = missing.associate { it.origin to ctx.newKey() }
        val added = missing.mapIndexed { i, b ->
            val f = b.field
            val calcSource = b.calcSourceOrigin?.let { src ->
                newKeys[src] ?: bundle.firstOrNull { it.origin == src }?.let(::match)?.key
            }
            FieldDef(
                key = newKeys.getValue(b.origin),
                entityKind = kind.open(),
                name = f.name.nfc(),
                exportName = f.exportName,
                type = f.type.open(),
                order = orders[i],
                inHeader = f.inHeader,
                inList = f.inList,
                inFilter = f.inFilter && FieldTypes.canFilter(f.type),
                isSecret = f.isSecret,
                shortTextMultiple = if (f.type == FieldType.SHORT_TEXT) f.shortTextMultiple ?: false else null,
                unit = if (f.type == FieldType.NUMBER) f.unit else null,
                options = if (FieldTypes.hasOptions(f.type)) FieldDefinitions.newOptions(f.options, ctx) else null,
                allowFreeText = if (f.type == FieldType.CHOICE) f.allowFreeText ?: false else null,
                datePrecision = if (f.type == FieldType.DATE) (f.datePrecision ?: DatePrecision.YEAR).open() else null,
                linkTargetKind = if (f.type == FieldType.LINK) (f.linkTargetKind ?: FieldLinkTargetKind.FACTION).open() else null,
                calc = if (f.type == FieldType.CALC) calcSource?.let { CalcSpec(sourceFieldDefKey = it) } else null,
                bundleOrigin = keepOrigin(b),
                updatedAt = ctx.now,
                updatedBy = ctx.by,
            )
        }
        return BundleResult(FieldDefinitions.withDefs(doc, doc.project.fieldDefs + added, ctx), added)
    }
}
