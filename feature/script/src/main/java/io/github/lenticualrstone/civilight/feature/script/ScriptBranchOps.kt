package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.domain.actions.SceneActions
import io.github.lenticualrstone.civilight.domain.actions.VersionActions
import io.github.lenticualrstone.civilight.domain.script.BlockOps
import io.github.lenticualrstone.civilight.domain.script.BlockPosition
import io.github.lenticualrstone.civilight.domain.script.DraftRules
import io.github.lenticualrstone.civilight.domain.script.ScriptWrite
import io.github.lenticualrstone.civilight.domain.script.WriteDraft
import io.github.lenticualrstone.civilight.feature.script.input.DraftMark
import io.github.lenticualrstone.civilight.feature.script.input.Drafts
import io.github.lenticualrstone.civilight.feature.script.state.ScriptOps
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.file.EpisodeBranch
import io.github.lenticualrstone.civilight.model.file.EpisodeFile
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.version.VersionOrigin

/**
 * (0.7.0) Zero-clutter episode-local IF branches and Plot Timeline scene reordering.
 * Branches never affect global lens/checks; adopting a branch auto-saves the prior main route as a version snapshot.
 */
object BranchOps {
    fun branchesOf(doc: ProjectDoc, unitKey: Key): List<EpisodeBranch> =
        doc.episodes[unitKey]?.branches.orEmpty()

    fun branchOf(doc: ProjectDoc, unitKey: Key, branchKey: Key?): EpisodeBranch? =
        branchKey?.let { key -> branchesOf(doc, unitKey).firstOrNull { it.key == key } }

    /** Up to 2 main-route blocks right before the fork point for dimmed context above a branch. */
    fun contextBlocks(doc: ProjectDoc, branch: EpisodeBranch): List<Block> {
        val parentKey = branch.parentBlockKey ?: return emptyList()
        val sceneBlocks = doc.manuscript.liveBlocksOf(branch.sceneKey)
        val idx = sceneBlocks.indexOfFirst { it.key == parentKey }
        if (idx < 0) return emptyList()
        val start = (idx - 1).coerceAtLeast(0)
        return sceneBlocks.subList(start, idx + 1)
    }

    fun create(doc: ProjectDoc, unitKey: Key, sceneKey: Key, parentBlockKey: Key?, now: Long): Pair<ProjectDoc, EpisodeBranch> {
        val ep = doc.episodes[unitKey] ?: EpisodeFile(unitKey = unitKey)
        val name = "분기 ${ep.branches.size + 1}"
        val branch = EpisodeBranch(
            key = Keys.newKey(),
            name = name,
            sceneKey = sceneKey,
            parentBlockKey = parentBlockKey,
            blocks = emptyList(),
            createdAt = now,
        )
        val nextEp = ep.copy(branches = ep.branches + branch)
        return doc.copy(episodes = doc.episodes + (unitKey to nextEp)) to branch
    }

    fun appendBlock(doc: ProjectDoc, unitKey: Key, branchKey: Key, write: WriteDraft, now: Long, device: String): Pair<ProjectDoc, Block>? {
        val ep = doc.episodes[unitKey] ?: return null
        val branch = ep.branches.firstOrNull { it.key == branchKey } ?: return null
        val runs = DraftRules.normalized(write.runs)
        if (runs.isEmpty() && write.cues.isNullOrEmpty()) return null
        val order = OrderKeys.between(branch.blocks.lastOrNull()?.order, null)
        val block = Block(
            key = Keys.newKey(),
            sceneKey = branch.sceneKey,
            order = order,
            number = branch.blocks.size + 1,
            kind = write.kind.open(),
            speakerKey = if (write.kind == BlockKind.DIALOGUE) write.speakerKey else null,
            coSpeakerKeys = if (write.kind == BlockKind.DIALOGUE) write.coSpeakerKeys else emptyList(),
            runs = runs,
            cues = if (write.kind == BlockKind.DIRECTION) write.cues.orEmpty() else emptyList(),
            createdAt = now,
            updatedAt = now,
            updatedBy = device,
        )
        val nextBranch = branch.copy(blocks = branch.blocks + block)
        val nextEp = ep.copy(branches = ep.branches.map { if (it.key == branchKey) nextBranch else it })
        return doc.copy(episodes = doc.episodes + (unitKey to nextEp)) to block
    }

    fun deleteBlock(doc: ProjectDoc, unitKey: Key, branchKey: Key, blockKey: Key): ProjectDoc {
        val ep = doc.episodes[unitKey] ?: return doc
        val nextEp = ep.copy(
            branches = ep.branches.map { b ->
                if (b.key == branchKey) b.copy(blocks = b.blocks.filterNot { it.key == blockKey }) else b
            },
        )
        return doc.copy(episodes = doc.episodes + (unitKey to nextEp))
    }

    fun deleteBranch(doc: ProjectDoc, unitKey: Key, branchKey: Key): ProjectDoc {
        val ep = doc.episodes[unitKey] ?: return doc
        val nextEp = ep.copy(branches = ep.branches.filterNot { it.key == branchKey })
        return doc.copy(episodes = doc.episodes + (unitKey to nextEp))
    }

    /**
     * Adopts [branchKey] into the main route:
     * 1) Saves current main episode into `지난 판` (`VersionActions.keep`) as automatic backup.
     * 2) Trashes main blocks after `branch.parentBlockKey` in `branch.sceneKey` and inserts the branch blocks.
     * 3) Removes the adopted branch from the episode.
     */
    fun adoptToMain(
        doc: ProjectDoc,
        unitKey: Key,
        branchKey: Key,
        ctx: io.github.lenticualrstone.civilight.domain.script.EditContext,
        actionCtx: io.github.lenticualrstone.civilight.domain.actions.ActionContext,
    ): ProjectDoc? {
        val branch = branchOf(doc, unitKey, branchKey) ?: return null
        if (!doc.manuscript.isLiveUnit(branch.sceneKey)) return null
        // 1. Auto-backup current main route into EpisodeVersions
        var out = VersionActions.capture(doc, unitKey, "${branch.name} 채택 전", VersionOrigin.MANUAL, actionCtx).first
        // 2. Remove main blocks after fork point in this scene
        val live = out.manuscript.liveBlocksOf(branch.sceneKey)
        val forkIdx = branch.parentBlockKey?.let { pk -> live.indexOfFirst { it.key == pk } } ?: -1
        val toReplace = if (forkIdx >= 0) live.drop(forkIdx + 1) else live
        if (toReplace.isNotEmpty()) {
            out = ScriptOps.deleteBlocks(out, toReplace.map { it.key }, ctx)?.doc ?: out
        }
        // 3. Append branch blocks in order
        var pos: BlockPosition = branch.parentBlockKey
            ?.takeIf { out.manuscript.isLiveBlock(it) }
            ?.let { BlockPosition.After(it) }
            ?: BlockPosition.SceneEnd(branch.sceneKey)
        for (b in branch.blocks) {
            val kind = b.kind.known ?: BlockKind.NARRATION
            val write = WriteDraft(
                kind = kind,
                runs = b.runs,
                speakerKey = b.speakerKey,
                cues = b.cues.takeIf { it.isNotEmpty() },
                coSpeakerKeys = b.coSpeakerKeys,
            )
            val sent = ScriptWrite.send(out, pos, write, gameTools = b.cues.isNotEmpty(), ctx = ctx)
            if (sent != null) {
                out = sent.doc
                pos = BlockPosition.After(sent.blockKey)
            }
        }
        // 4. Remove adopted branch
        return deleteBranch(out, unitKey, branchKey)
    }
}

fun ScriptViewModel.switchBranch(branchKey: Key?) {
    activeBranchFlow.value = branchKey
    closeMark()
}

fun ScriptViewModel.createBranchAt(sceneKey: Key, parentBlockKey: Key?) {
    if (readOnly) return
    val unit = unitKey ?: return
    enqueue {
        var createdKey: Key? = null
        runOp("분기 만들기") { d ->
            val (next, b) = BranchOps.create(d, unit, sceneKey, parentBlockKey, actionCtx().now)
            createdKey = b.key
            ScriptOps.Outcome(next)
        } ?: return@enqueue
        activeBranchFlow.value = createdKey
        closeMark()
    }
}

internal suspend fun ScriptViewModel.appendToBranch(branchKey: Key, write: WriteDraft) {
    val unit = unitKey ?: return
    val c = ctx()
    val outcome = runOp("분기 쓰기") { d ->
        BranchOps.appendBlock(d, unit, branchKey, write, c.now, c.device)?.let { (next, _) ->
            ScriptOps.Outcome(next)
        }
    } ?: return
    setDraft(Drafts.sent(draftFlow.value))
    outcome.toast?.let(::showUndoToast)
}

fun ScriptViewModel.deleteBranchBlock(branchKey: Key, blockKey: Key) {
    if (readOnly) return
    val unit = unitKey ?: return
    enqueue {
        runOp("분기 블록 지우기") { d -> ScriptOps.Outcome(BranchOps.deleteBlock(d, unit, branchKey, blockKey)) }
    }
}

fun ScriptViewModel.deleteActiveBranch(branchKey: Key) {
    if (readOnly) return
    val unit = unitKey ?: return
    enqueue {
        runOp("분기 지우기") { d -> ScriptOps.Outcome(BranchOps.deleteBranch(d, unit, branchKey)) }
        if (activeBranchFlow.value == branchKey) activeBranchFlow.value = null
    }
}

fun ScriptViewModel.adoptBranch(branchKey: Key) {
    if (readOnly) return
    val unit = unitKey ?: return
    enqueue {
        runOp("분기 본편 채택") { d ->
            BranchOps.adoptToMain(d, unit, branchKey, ctx(), actionCtx())?.let { ScriptOps.Outcome(it) }
        } ?: return@enqueue
        activeBranchFlow.value = null
        showUndoToast("분기 본편 채택 · 이전 본편은 지난 판에 보관")
    }
}

/** (0.7.0) Reorders a scene within the current episode from the Plot Timeline strip. */
fun ScriptViewModel.moveSceneInEpisode(sceneKey: Key, afterSceneKey: Key?) {
    if (readOnly) return
    enqueue {
        runOp("씬 순서 바꿈") { d ->
            val scene = d.index.unit(sceneKey) ?: return@runOp null
            val parent = scene.parentKey ?: return@runOp null
            val moved = SceneActions.move(d, sceneKey, parent, afterSceneKey, actionCtx()).result
            if (moved === d) null else ScriptOps.Outcome(moved)
        }
    }
}

/**
 * (0.7.3) Safely removes ONLY the scene boundary (`sceneKey`) by merging its blocks into the previous scene
 * (`SceneActions.merge`), matching Sonnet-Create's divider deletion where blocks below are never lost.
 */
fun ScriptViewModel.removeSceneDivider(sceneKey: Key) {
    if (readOnly) return
    enqueue {
        var blockedMessage: String? = null
        val outcome = runOp("장면 구분선 지움") { d ->
            when (val plan = SceneActions.merge(d, sceneKey, actionCtx())) {
                is io.github.lenticualrstone.civilight.domain.story.scene.MergePlan.Ready ->
                    ScriptOps.Outcome(plan.preview.result, toast = "장면 구분선 지움 · 아래 글은 앞 장면에 합침")
                is io.github.lenticualrstone.civilight.domain.story.scene.MergePlan.Blocked -> {
                    blockedMessage = plan.reason
                    null
                }
                io.github.lenticualrstone.civilight.domain.story.scene.MergePlan.NotOffered -> {
                    blockedMessage = "첫 장면 구분선은 지울 수 없음"
                    null
                }
            }
        }
        if (outcome != null) {
            outcome.toast?.let(::showUndoToast)
        } else {
            blockedMessage?.let { showUndoToast(it) }
        }
    }
}
