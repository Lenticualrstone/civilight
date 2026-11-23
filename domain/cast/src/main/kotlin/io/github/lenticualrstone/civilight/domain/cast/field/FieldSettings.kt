package io.github.lenticualrstone.civilight.domain.cast.field

import io.github.lenticualrstone.civilight.model.game.GameTools
import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.label
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/** A project field row: grip, name, `<종류> · <detail> · <places>`, chevron (`RCFields`). */
data class FieldSettingRow(
    val key: Key,
    val name: String,
    val sub: String,
    /** Definition hidden (`숨기기`): the row stays with a hidden mark (TC-F-21). */
    val hidden: Boolean,
    val gripLabel: String = "순서 바꾸기",
)

/** `프로젝트 설정 › 인물 항목` (CST-19) for one kind tab. */
data class FieldSettingsScreen(
    val head: String,
    val title: String,
    val tabs: List<String>,
    val kind: EntityKind,
    /** `고정 항목 · 7`. */
    val fixedTitle: String,
    val fixedFields: List<String>,
    /** `프로젝트 항목 · 11`. */
    val projectTitle: String,
    val reorderNote: String,
    val rows: List<FieldSettingRow>,
    val addLabel: String,
    /** `시작 묶음 · 게임 프로필`. */
    val bundleTitle: String,
    val bundleSub: String,
    val bundleAction: String,
    /** CST-54: the section label's trailing button `순서 바꾸기` (order mode). */
    val reorderAction: String = FieldSettings.REORDER_ACTION,
    /** CST-54: the order-mode note `끌기 · 앞으로 · 뒤로`. */
    val orderModeNote: String = FieldSettings.ORDER_NOTE,
)

/** Settings screen of the field engine (CST-19). */
object FieldSettings {
    const val HEAD = "프로젝트 설정"
    const val TITLE = "인물 항목"
    val TABS = listOf("인물", "장소", "용어", "세력")
    const val HIDDEN_MARK = "숨김"
    const val REORDER_ACTION = "순서 바꾸기"
    const val ORDER_NOTE = "끌기 · 앞으로 · 뒤로"

    /** The fixed fields of a character: they cannot be deleted. */
    val FIXED_CHARACTER = listOf("이름", "표시 이름", "한 줄 소개", "이미지", "태그", "첫 등장", "보이스 진행")

    /** Fixed fields of a world card [PROPOSED]: the name only. */
    val FIXED_OTHER = listOf("이름")

    fun fixedFields(kind: EntityKind): List<String> = if (kind == EntityKind.CHARACTER) FIXED_CHARACTER else FIXED_OTHER

    /** `보이스 진행`, the fixed field that belongs to the game tools (G-2). */
    const val VOICE_PROGRESS = "보이스 진행"

    /**
     * (0.2.0, CST-74, §7.9) The fixed fields shown: without `보이스 진행` while `게임 제작 도구` is off
     * (`고정 항목 · 6`). A template has no switch value, so its editor reads off.
     */
    fun fixedFields(kind: EntityKind, gameTools: Boolean): List<String> =
        fixedFields(kind).filter { gameTools || it != VOICE_PROGRESS }

    /** Project fields of a kind (live, not `이 인물만`), hidden ones included, in order. */
    fun projectFields(doc: ProjectDoc, kind: EntityKind): List<FieldDef> =
        FieldDefinitions.live(doc, kind).filter { it.scopeCharacterKey == null }

    /** The type-specific part of a row's sub (`연도`, `출생에서`, `세력`, `cm`, `보기 3`, `여러 개`). */
    fun detail(doc: ProjectDoc, def: FieldDef): String? = when (def.type.known) {
        FieldType.DATE -> def.datePrecision?.known?.let(FieldTypes::precisionLabel)
        FieldType.CALC -> FieldReader.liveCalcSource(doc, def)?.name?.let { "${it}에서" }
        FieldType.LINK -> def.linkTargetKind?.known?.label
        FieldType.NUMBER -> def.unit?.takeIf { it.isNotBlank() }
        FieldType.CHOICE, FieldType.MULTI_CHOICE -> "보기 ${def.options.orEmpty().size}"
        FieldType.SHORT_TEXT -> if (def.shortTextMultiple == true) "여러 개" else null
        else -> null
    }

    /** Where a field shows: `머리 · 목록 · 필터`; long text is a section of `개요`; secret `비밀`. */
    fun places(def: FieldDef): List<String> = buildList {
        if (def.isSecret) {
            add("비밀")
            return@buildList
        }
        if (def.type.known == FieldType.LONG_TEXT) add("개요")
        if (def.inHeader) add("머리")
        if (def.inList) add("목록")
        if (def.inFilter) add("필터")
    }

    fun row(doc: ProjectDoc, def: FieldDef): FieldSettingRow {
        val type = def.type.known?.let(FieldTypes::label) ?: def.type.raw
        val parts = listOf(type, detail(doc, def)) + places(def) + listOfNotNull(if (def.definitionHidden) HIDDEN_MARK else null)
        return FieldSettingRow(def.key, def.name, CastCopy.joinAll(parts), def.definitionHidden)
    }

    /** The bundle name shown for a kind (`게임 프로필`), defaulting to `기본` for characters and `비어 있음` for others. */
    fun bundleName(doc: ProjectDoc, kind: EntityKind): String {
        val names = doc.project.cast.bundleName
        val stored = when (kind) {
            EntityKind.CHARACTER -> names.character
            EntityKind.PLACE -> names.place
            EntityKind.TERM -> names.term
            EntityKind.FACTION -> names.faction
        }
        return stored ?: if (kind == EntityKind.CHARACTER) FieldBundles.label(FieldBundles.DEFAULT) else "비어 있음"
    }

    /** `프로젝트 설정` row: `게임 프로필 · 고정 7 · 프로젝트 11 · 장소 · 용어 · 세력도` with `고치기 ›`. */
    fun entryRow(doc: ProjectDoc): Triple<String, String, String> = Triple(
        TITLE,
        CastCopy.join(
            bundleName(doc, EntityKind.CHARACTER),
            "고정 ${fixedFields(EntityKind.CHARACTER, GameTools.on(doc)).size}",
            "프로젝트 ${projectFields(doc, EntityKind.CHARACTER).size}",
            "장소 · 용어 · 세력도",
        ),
        "고치기 ›",
    )

    fun screen(doc: ProjectDoc, kind: EntityKind = EntityKind.CHARACTER): FieldSettingsScreen {
        val defs = projectFields(doc, kind)
        val fixed = fixedFields(kind, GameTools.on(doc))
        return FieldSettingsScreen(
            head = HEAD,
            title = TITLE,
            tabs = TABS,
            kind = kind,
            fixedTitle = "고정 항목 · ${fixed.size}",
            fixedFields = fixed,
            projectTitle = "프로젝트 항목 · ${defs.size}",
            reorderNote = "끌어서 순서",
            rows = defs.map { row(doc, it) },
            addLabel = "+ 항목",
            bundleTitle = "시작 묶음 · ${bundleName(doc, kind)}",
            bundleSub = "빈 항목은 보기에서 숨김 · 값은 ?? 가능",
            bundleAction = "항목 더하기 ›",
        )
    }
}
