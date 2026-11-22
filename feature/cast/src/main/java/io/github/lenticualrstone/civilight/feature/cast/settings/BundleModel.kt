package io.github.lenticualrstone.civilight.feature.cast.settings

import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.field.BundleResult
import io.github.lenticualrstone.civilight.domain.cast.field.FieldBundles
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

    /** `다른 프로젝트에서`: other live projects; hidden ones only while the hidden group is open (TC-F-16). */
    fun sources(entries: List<LibraryEntry>, currentKey: Key, hiddenUnlocked: Boolean): List<LibraryEntry> =
        FieldBundles.sourceProjects(entries, currentKey, hiddenUnlocked).sortedBy { it.name }
}
