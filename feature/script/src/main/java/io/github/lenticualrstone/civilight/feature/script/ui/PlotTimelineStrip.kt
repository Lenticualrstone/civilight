package io.github.lenticualrstone.civilight.feature.script.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.glass.GlassBackground
import io.github.lenticualrstone.civilight.designsystem.glass.GlassSurface
import io.github.lenticualrstone.civilight.designsystem.glass.OnGlass
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.script.BranchOps
import io.github.lenticualrstone.civilight.feature.script.layout.ScriptLayout
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.ProfileShape
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.file.EpisodeBranch
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.plainText
import io.github.lenticualrstone.civilight.ui.image.CharacterPortraitTile

/**
 * (0.7.0) Horizontal Plot Timeline strip inspired by Sonnet-Create's scene strip:
 * - Compact frosted scene cards (`S#N · title`, `대사 N · 약 M분`, subtle dialogue density bar).
 * - Tap to jump directly to a scene; long press to reorder (`앞으로 · 뒤로`) or open scene settings.
 * - Zero-clutter IF branch pills (`⑂ 분기 N`) appear only when branches exist in the episode.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PlotTimelineStrip(
    doc: ProjectDoc,
    unitKey: Key,
    currentSceneKey: Key?,
    activeBranchKey: Key?,
    readOnly: Boolean,
    unlocked: Boolean,
    modifier: Modifier = Modifier,
    onJumpScene: (Key) -> Unit,
    onSceneWhere: (Key) -> Unit,
    onMoveScene: (sceneKey: Key, afterSceneKey: Key?) -> Unit,
    onSelectBranch: (Key?) -> Unit,
    onNewScene: (() -> Unit)?,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val scenes = remember(doc, unitKey, unlocked) {
        doc.manuscript.liveScenes.filter {
            doc.index.numberingUnitKeyOfScene(it.key) == unitKey && (unlocked || !it.hidden)
        }
    }
    val branches = remember(doc, unitKey) { BranchOps.branchesOf(doc, unitKey) }
    val branchesByScene = remember(branches) { branches.groupBy { it.sceneKey } }
    var editingSceneKey by remember { mutableStateOf<Key?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            scenes.forEachIndexed { idx, scene ->
                val isCurrent = activeBranchKey == null && scene.key == currentSceneKey
                val isEditing = editingSceneKey == scene.key
                val blocks = remember(doc, scene.key) { doc.manuscript.liveBlocksOf(scene.key) }
                val dialogueCount = remember(blocks) { blocks.count { it.kind.known == BlockKind.DIALOGUE } }
                val chars = remember(blocks) { blocks.sumOf { it.runs.plainText.length } }
                val minutes = remember(chars) { (chars / 320).coerceAtLeast(if (chars > 0) 1 else 0) }
                val dialogueRatio = if (blocks.isEmpty()) 0f else (dialogueCount.toFloat() / blocks.size.toFloat()).coerceIn(0.08f, 1f)
                val shape = RoundedCornerShape(14.dp)

                Column(
                    Modifier
                        .widthIn(min = 122.dp, max = 180.dp)
                        .clip(shape)
                        .background(if (isCurrent) colors.hl else colors.tint)
                        .insetOutline(
                            if (isCurrent || isEditing) colors.acc.copy(alpha = 0.55f) else colors.line,
                            1.dp,
                            shape,
                        )
                        .combinedClickable(
                            onClick = {
                                editingSceneKey = null
                                if (activeBranchKey != null) onSelectBranch(null)
                                onJumpScene(scene.key)
                            },
                            onLongClick = {
                                if (!readOnly) editingSceneKey = if (isEditing) null else scene.key
                            },
                        )
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        CivilightText(
                            "S#${idx + 1}",
                            type.number.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                            color = if (isCurrent) colors.acc else colors.ink3,
                            maxLines = 1,
                        )
                        CivilightText(
                            scene.title.orEmpty().ifBlank { "제목 없음" },
                            type.chip.copy(fontSize = 12.sp, fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium),
                            color = if (isCurrent) colors.ink else colors.ink2,
                            maxLines = 1,
                        )
                    }
                    CivilightText(
                        if (minutes > 0) "대사 ${dialogueCount} · 약 ${minutes}분" else "대사 ${dialogueCount}",
                        type.micro,
                        color = colors.ink3,
                        maxLines = 1,
                    )
                    // Subtle 2 dp bottom accent bar reflecting scene dialogue density.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .clip(CircleShape)
                            .background(colors.line),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(dialogueRatio)
                                .fillMaxHeight()
                                .background(if (isCurrent) colors.acc else colors.ink3.copy(alpha = 0.45f)),
                        )
                    }
                }

                // Zero-clutter branch chips: only appear when branches exist on this scene.
                branchesByScene[scene.key].orEmpty().forEach { branch ->
                    val branchActive = activeBranchKey == branch.key
                    val pillShape = RoundedCornerShape(14.dp)
                    Row(
                        Modifier
                            .clip(pillShape)
                            .background(if (branchActive) colors.hl else colors.tint)
                            .insetOutline(if (branchActive) colors.acc else colors.line, 1.dp, pillShape)
                            .pressable(
                                onClick = { onSelectBranch(if (branchActive) null else branch.key) },
                                hitTarget = HitTarget.CHIP,
                                shape = pillShape,
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .semantics {
                                role = Role.Tab
                                selected = branchActive
                                contentDescription = branch.name
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        CivilightIcon(IconId.Branch, contentDescription = null, size = 14.dp, tint = colors.acc)
                        CivilightText(
                            branch.name,
                            type.chip.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                            color = if (branchActive) colors.acc else colors.ink,
                            maxLines = 1,
                        )
                    }
                }
            }

            if (onNewScene != null && !readOnly) {
                val addShape = RoundedCornerShape(14.dp)
                Row(
                    Modifier
                        .clip(addShape)
                        .background(colors.tint)
                        .insetOutline(colors.line, 1.dp, addShape)
                        .pressable(onClick = onNewScene, hitTarget = HitTarget.CHIP, shape = addShape)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    CivilightIcon(IconId.Plus, contentDescription = null, size = 14.dp, tint = colors.ink2)
                    CivilightText("장면", type.chip.copy(fontSize = 12.sp), color = colors.ink2, maxLines = 1)
                }
            }
        }

        // Inline reorder / scene edit bar when user long-pressed a scene card.
        val editingIdx = editingSceneKey?.let { key -> scenes.indexOfFirst { it.key == key } } ?: -1
        if (editingIdx >= 0 && !readOnly) {
            val targetScene = scenes[editingIdx]
            val barShape = RoundedCornerShape(12.dp)
            Row(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(barShape)
                    .background(colors.raise.copy(alpha = 0.90f))
                    .insetOutline(colors.edge, 1.dp, barShape)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CivilightText(
                    "S#${editingIdx + 1} ${targetScene.title.orEmpty().ifBlank { "제목 없음" }}",
                    type.small,
                    Modifier.weight(1f),
                    color = colors.ink,
                    maxLines = 1,
                )
                TimelineMiniAction("앞으로", enabled = editingIdx > 0) {
                    val after = if (editingIdx >= 2) scenes[editingIdx - 2].key else null
                    onMoveScene(targetScene.key, after)
                    editingSceneKey = null
                }
                TimelineMiniAction("뒤로", enabled = editingIdx < scenes.lastIndex) {
                    val after = scenes[editingIdx + 1].key
                    onMoveScene(targetScene.key, after)
                    editingSceneKey = null
                }
                TimelineMiniAction("장소 · 때", enabled = true) {
                    editingSceneKey = null
                    onSceneWhere(targetScene.key)
                }
                TimelineMiniAction("닫기", enabled = true) { editingSceneKey = null }
            }
        }
    }
}

@Composable
private fun TimelineMiniAction(label: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier
            .alpha(if (enabled) 1f else 0.35f)
            .clip(shape)
            .background(colors.tint)
            .pressable(onClick = onClick, enabled = enabled, hitTarget = HitTarget.SMALL, shape = shape)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        CivilightText(label, CivilightTheme.type.micro, color = colors.ink, maxLines = 1)
    }
}

/**
 * (0.7.0) Episode-local IF Branch view:
 * Shows dimmed pre-fork context blocks, a clear branch control banner (`본편 채택`, `지우기`, `본편으로`),
 * and the branch's draft blocks written through the same Docked Glass Arch composer.
 */
@Composable
internal fun BranchManuscriptArea(
    doc: ProjectDoc,
    branch: EpisodeBranch,
    padding: PaddingValues,
    layout: ScriptLayout,
    profileShape: ProfileShape,
    readOnly: Boolean,
    onBackToMain: () -> Unit,
    onAdopt: () -> Unit,
    onDeleteBranch: () -> Unit,
    onDeleteBlock: (Key) -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val contextBlocks = remember(doc, branch) { BranchOps.contextBlocks(doc, branch) }
    val column = if (layout.wide) Modifier.padding(start = layout.columnStart.dp).width(layout.columnWidth.dp) else Modifier.fillMaxWidth()

    LazyColumn(
        modifier = Modifier.fillMaxHeight().then(column),
        contentPadding = PaddingValues(
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (contextBlocks.isNotEmpty()) {
            item(key = "branch-context-label") {
                CivilightText(
                    "분기 이전 본편 흐름",
                    type.micro,
                    Modifier.padding(horizontal = 24.dp),
                    color = colors.ink3,
                    maxLines = 1,
                )
            }
            items(contextBlocks, key = { "ctx-${it.key}" }) { block ->
                BranchBlockCard(
                    doc = doc,
                    block = block,
                    profileShape = profileShape,
                    dimmed = true,
                    onDelete = null,
                )
            }
        }

        item(key = "branch-banner") {
            val shape = RoundedCornerShape(16.dp)
            Box(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(shape),
            ) {
                GlassBackground(
                    surface = GlassSurface.PANEL,
                    modifier = Modifier.matchParentSize(),
                    shape = shape,
                    fill = colors.glass,
                )
                OnGlass {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .insetOutline(colors.acc.copy(alpha = 0.55f), 1.dp, shape)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CivilightIcon(IconId.Branch, contentDescription = null, size = 18.dp, tint = colors.acc)
                            CivilightText(branch.name, type.itemTitle, Modifier.weight(1f), color = colors.ink, maxLines = 1)
                            CivilightText("본편과 분리됨", type.micro, color = colors.ink3, maxLines = 1)
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CivilightButton("본편으로", onBackToMain, Modifier.weight(1f), ButtonVariant.COMPACT_GHOST)
                            if (!readOnly) {
                                CivilightButton("지우기", onDeleteBranch, Modifier.weight(1f), ButtonVariant.COMPACT_GHOST)
                                CivilightButton("본편 채택", onAdopt, Modifier.weight(1.2f), ButtonVariant.COMPACT_PRIMARY)
                            }
                        }
                    }
                }
            }
        }

        if (branch.blocks.isEmpty()) {
            item(key = "branch-empty") {
                CivilightText(
                    "아래 입력창에서 다른 전개를 작성",
                    type.small,
                    Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    color = colors.ink3,
                )
            }
        } else {
            items(branch.blocks, key = { "br-${it.key}" }) { block ->
                BranchBlockCard(
                    doc = doc,
                    block = block,
                    profileShape = profileShape,
                    dimmed = false,
                    onDelete = if (readOnly) null else ({ onDeleteBlock(block.key) }),
                )
            }
        }
    }
}

@Composable
private fun BranchBlockCard(
    doc: ProjectDoc,
    block: Block,
    profileShape: ProfileShape,
    dimmed: Boolean,
    onDelete: (() -> Unit)?,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val kind = block.kind.known
    val speaker = block.speakerKey?.let(doc.index::character)
    val text = block.runs.plainText
    Row(
        Modifier
            .fillMaxWidth()
            .alpha(if (dimmed) 0.48f else 1f)
            .padding(horizontal = 24.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (kind == BlockKind.DIALOGUE) {
            Column(
                Modifier.width(52.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                CharacterPortraitTile(
                    doc = doc,
                    characterKey = speaker?.key,
                    displayName = speaker?.name.orEmpty(),
                    shape = profileShape,
                    size = 40.dp,
                )
                if (speaker != null) {
                    val label = if (block.coSpeakerKeys.isNotEmpty()) "${speaker.name} 외 ${block.coSpeakerKeys.size}명" else speaker.name
                    CivilightText(label, type.micro, color = colors.ink2, maxLines = 1)
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val textColor = when (kind) {
                BlockKind.DIALOGUE -> colors.ink
                BlockKind.MEMO -> colors.ink3
                else -> colors.ink2
            }
            val style = when (kind) {
                BlockKind.DIALOGUE -> type.line
                BlockKind.MEMO -> type.dir
                else -> type.body
            }
            CivilightText(if (kind == BlockKind.MEMO) "// $text" else text, style, color = textColor)
        }
        if (onDelete != null) {
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .pressable(onClick = onDelete, hitTarget = HitTarget.SMALL, shape = CircleShape)
                    .semantics { contentDescription = "분기 블록 지우기"; role = Role.Button },
                contentAlignment = Alignment.Center,
            ) {
                CivilightIcon(IconId.Trash, contentDescription = null, size = 15.dp, tint = colors.ink3)
            }
        }
    }
}
