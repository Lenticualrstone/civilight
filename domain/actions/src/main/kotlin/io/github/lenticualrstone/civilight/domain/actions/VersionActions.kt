package io.github.lenticualrstone.civilight.domain.actions

import io.github.lenticualrstone.civilight.domain.library.export.ExportPlan
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.version.EpisodeVersion
import io.github.lenticualrstone.civilight.model.version.EpisodeVersionsFile
import io.github.lenticualrstone.civilight.model.version.VersionBlock
import io.github.lenticualrstone.civilight.model.version.VersionOption
import io.github.lenticualrstone.civilight.model.version.VersionOrigin
import io.github.lenticualrstone.civilight.model.version.VersionScene
import java.time.LocalDate

/** Words of version names (N-29, D21). */
object VersionCopy {
    const val DRAFT = "초고"
    const val EXPORTED = "내보낸 판"
    const val SEP = " · "

    /** `11월 23일`. */
    fun date(month: Int, day: Int): String = "${month}월 ${day}일"
}

/**
 * (0.2.0, N-29, 00 §13.5, SCR-76) Episode versions (`판`). [capture] is the one write of a version
 * (`판 남기기` and the automatic version of a PDF, CSV or engine JSON export); [restoreBlockText] is
 * `이 판 글로`. A version never touches `◆`, numbers, voice IDs, cues or acknowledgements.
 */
object VersionActions {
    /** `11월 23일 · 초고` or `11월 24일 · 내보낸 판`. */
    fun defaultName(date: LocalDate, origin: VersionOrigin): String =
        VersionCopy.date(date.monthValue, date.dayOfMonth) + VersionCopy.SEP + if (origin == VersionOrigin.EXPORT) VersionCopy.EXPORTED else VersionCopy.DRAFT

    /** Versions of a numbering unit, oldest first. */
    fun versionsOf(doc: ProjectDoc, unitKey: Key): List<EpisodeVersion> = doc.versions[unitKey]?.versions.orEmpty()

    /** Live scenes of a numbering unit, in manuscript order. */
    fun scenesOf(doc: ProjectDoc, unitKey: Key) =
        doc.manuscript.liveScenes.filter { doc.index.numberingUnitKeyOfScene(it.key) == unitKey }

    /**
     * Stores a version of [unitKey]: every live block of its live scenes in manuscript order. [name]
     * null takes [defaultName] of today. Returns the document and the version.
     */
    fun capture(
        doc: ProjectDoc,
        unitKey: Key,
        name: String?,
        origin: VersionOrigin,
        ctx: ActionContext,
    ): Pair<ProjectDoc, EpisodeVersion> {
        val scenes = scenesOf(doc, unitKey)
        val sceneKeys = scenes.map { it.key }.toSet()
        val blocks = doc.manuscript.liveBlocks.filter { it.sceneKey in sceneKeys }
        val version = EpisodeVersion(
            key = ctx.newKey(),
            name = name?.trim()?.ifEmpty { null } ?: defaultName(ctx.today, origin),
            origin = origin.open(),
            createdAt = ctx.now,
            createdBy = ctx.device,
            scenes = scenes.map { VersionScene(it.key, it.title) },
            blocks = blocks.map(::snapshot),
        )
        val file = doc.versions[unitKey] ?: EpisodeVersionsFile(unitKey = unitKey)
        return doc.copy(versions = doc.versions + (unitKey to file.copy(versions = file.versions + version))) to version
    }

    /** One automatic `내보낸 판` per numbering unit [plan] wrote (D21); PDF, CSV and engine JSON only. */
    fun captureForExport(doc: ProjectDoc, plan: ExportPlan, ctx: ActionContext): ProjectDoc =
        captureForExport(doc, plan.scenes.mapNotNull { doc.index.numberingUnitKeyOfScene(it.sceneKey) }.distinct(), ctx)

    fun captureForExport(doc: ProjectDoc, unitKeys: List<Key>, ctx: ActionContext): ProjectDoc =
        unitKeys.distinct().fold(doc) { d, unit -> capture(d, unit, null, VersionOrigin.EXPORT, ctx).first }

    /**
     * True when `이 판 글로` is offered for [versionBlock]: the block with the same key is live, and both
     * kinds are text kinds or both are `선택지` (SCR-76 `[DEFAULT]`).
     */
    fun canRestore(doc: ProjectDoc, versionBlock: VersionBlock): Boolean {
        val live = doc.index.block(versionBlock.key)?.takeIf { doc.liveness.isLive(it) } ?: return false
        val a = versionBlock.kind.known
        val b = live.kind.known
        return (a in TEXT_KINDS && b in TEXT_KINDS) || (a == BlockKind.CHOICE && b == BlockKind.CHOICE)
    }

    /**
     * `이 판 글로`: the live block with [blockKey] gets the version's text (choices: each option's
     * text by option key). Nothing else of the block, and no other block, changes. Null when not offered.
     */
    fun restoreBlockText(doc: ProjectDoc, unitKey: Key, versionKey: Key, blockKey: Key, ctx: ActionContext): ProjectDoc? {
        val version = versionsOf(doc, unitKey).firstOrNull { it.key == versionKey } ?: return null
        val vb = version.blocks.firstOrNull { it.key == blockKey } ?: return null
        if (!canRestore(doc, vb)) return null
        val live = doc.index.block(blockKey) ?: return null
        val next = if (live.kind.known == BlockKind.CHOICE) {
            val byKey = vb.options.associateBy { it.key }
            live.copy(options = live.options.map { o -> byKey[o.key]?.let { o.copy(runs = it.runs) } ?: o })
        } else {
            live.copy(runs = vb.runs)
        }
        if (next == live) return doc
        val stamped = next.copy(updatedAt = ctx.now, updatedBy = ctx.device)
        return doc.copy(
            episodes = doc.episodes.mapValues { (_, f) ->
                if (f.blocks.none { it.key == blockKey }) f else f.copy(blocks = f.blocks.map { if (it.key == blockKey) stamped else it })
            },
        )
    }

    private fun snapshot(b: Block): VersionBlock = VersionBlock(
        key = b.key,
        sceneKey = b.sceneKey,
        number = b.number,
        kind = b.kind,
        speakerKey = b.speakerKey,
        runs = b.runs,
        options = b.options.sortedWith(compareBy({ it.order }, { it.key })).map { VersionOption(it.key, it.runs) },
    )

    /** Kinds whose text is `runs` (direction has a text line since 0.2.0, SCR-08). */
    private val TEXT_KINDS = setOf(BlockKind.DIALOGUE, BlockKind.NARRATION, BlockKind.SYSTEM, BlockKind.MEMO, BlockKind.DIRECTION)
}
