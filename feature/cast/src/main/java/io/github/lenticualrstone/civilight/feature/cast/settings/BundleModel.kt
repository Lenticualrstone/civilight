package io.github.lenticualrstone.civilight.feature.cast.settings

import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.field.BundleResult
import io.github.lenticualrstone.civilight.domain.cast.field.FieldBundles
import io.github.lenticualrstone.civilight.domain.cast.field.FieldTemplates
import io.github.lenticualrstone.civilight.domain.cast.field.TemplatePreview
import io.github.lenticualrstone.civilight.domain.cast.label
import io.github.lenticualrstone.civilight.feature.cast.templates.TemplateCopy
import io.github.lenticualrstone.civilight.model.app.FieldTemplate
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.LibraryEntry
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.FieldBundle

/** A row of the bundle picker: `게임 프로필` with its fields `출생 · 나이 · … · 특기`. */
data class BundleRowView(val bundle: FieldBundle, val title: String, val sub: String?)

/**
 * Pure model of `항목 더하기` (CST-24): the four bundles, what applying one would add (only fields
 * that are missing; nothing existing changes), and the projects `다른 프로젝트에서` may copy from.
 */
object BundleModel {
    fun rows(kind: EntityKind): List<BundleRowView> = FieldBundles.PICKER.map { b ->
        val names = FieldBundles.fields(b, kind).map { it.field.name }
        BundleRowView(b, FieldBundles.label(b), names.takeIf { it.isNotEmpty() }?.let(CastCopy::joinAll))
    }

    /** Names of the fields a built-in bundle would add, in bundle order (TC-F-15). */
    fun preview(doc: ProjectDoc, bundle: FieldBundle, kind: EntityKind, ctx: EditContext): List<String> =
        FieldBundles.apply(doc, bundle, ctx, kind).added.map { it.name }

    /** Names of the definitions [source] would add (definitions only, no values; TC-F-16). */
    fun previewFrom(doc: ProjectDoc, source: ProjectDoc, kind: EntityKind, ctx: EditContext): List<String> =
        FieldBundles.applyFromProject(doc, source, ctx, kind).added.map { it.name }

    fun apply(doc: ProjectDoc, bundle: FieldBundle, kind: EntityKind, ctx: EditContext): BundleResult = FieldBundles.apply(doc, bundle, ctx, kind)

    fun applyFrom(doc: ProjectDoc, source: ProjectDoc, kind: EntityKind, ctx: EditContext): BundleResult =
        FieldBundles.applyFromProject(doc, source, ctx, kind)

    // --- Templates (CST-60) ---

    /** `템플릿 · n` rows: visible live templates (hidden ones only while the group is open), sub = per-kind counts. */
    fun templateRows(templates: List<FieldTemplate>, hiddenUnlocked: Boolean): List<Pair<FieldTemplate, String>> =
        FieldTemplates.visible(templates, hiddenUnlocked).map { it to FieldTemplates.kindCounts(it) }

    /** The kinds a template applies to from [kind]'s tab: the tab's own, plus the others when [others] is checked. */
    fun templateKinds(kind: EntityKind, others: Boolean): Set<EntityKind> = if (others) FieldTemplates.KINDS.toSet() else setOf(kind)

    /** The three-part preview (`더할 항목` · `이미 있음` · `◇ 같은 이름 · 다른 종류`), computed before anything is written. */
    fun templatePreview(doc: ProjectDoc, template: FieldTemplate, kind: EntityKind, others: Boolean): TemplatePreview =
        FieldTemplates.preview(doc, template, templateKinds(kind, others))

    /** The `장소 · 용어 · 세력도` checkbox: label and the kinds that would add something (`장소 1`), or null when none. */
    fun otherKinds(doc: ProjectDoc, template: FieldTemplate, kind: EntityKind): Pair<String, String>? {
        val adds = FieldTemplates.otherKindsWithAdds(doc, template, kind)
        if (adds.isEmpty()) return null
        val label = CastCopy.joinAll(FieldTemplates.KINDS.filter { it != kind }.map { it.label }) + "도"
        return label to TemplateCopy.otherKindsSub(adds.map { it.kind to it.added.size })
    }

    fun applyTemplate(doc: ProjectDoc, template: FieldTemplate, kind: EntityKind, others: Boolean, ctx: EditContext): ProjectDoc =
        FieldTemplates.apply(doc, template, templateKinds(kind, others), ctx).doc

    /** `다른 프로젝트에서`: other live projects; hidden ones only while the hidden group is open (TC-F-16). */
    fun sources(entries: List<LibraryEntry>, currentKey: Key, hiddenUnlocked: Boolean): List<LibraryEntry> =
        FieldBundles.sourceProjects(entries, currentKey, hiddenUnlocked).sortedBy { it.name }
}
