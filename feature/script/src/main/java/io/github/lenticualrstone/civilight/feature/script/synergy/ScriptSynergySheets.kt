package io.github.lenticualrstone.civilight.feature.script.synergy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.Avatar
import io.github.lenticualrstone.civilight.designsystem.component.AvatarSize
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.EmptyBlock
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.script.Labels
import io.github.lenticualrstone.civilight.feature.script.ScriptCopy
import io.github.lenticualrstone.civilight.feature.script.input.WriteCopy
import io.github.lenticualrstone.civilight.feature.script.panel.PlayTimeEstimator
import io.github.lenticualrstone.civilight.feature.script.scenes.SceneCopy
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRowKeys
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.RichDoc
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.navigation.SheetKey
import kotlinx.serialization.Serializable

/** (0.5.0) Obsidian-style compact `@` mention preview sheet before jumping away from the script. */
@Serializable
data class MentionPreviewSheetKey(
    val projectKey: Key,
    val targetKind: String,
    val targetKey: Key,
) : SheetKey

/** (0.5.0) Quick in-script scene outline with estimated play time and 1-tap scene jump. */
@Serializable
data class ScriptOutlineSheetKey(val projectKey: Key, val unitKey: Key) : SheetKey

/** (0.5.0) Speaker focus & dialogue share statistics sheet for 1-minute voice consistency review. */
@Serializable
data class SpeakerFocusSheetKey(val projectKey: Key, val unitKey: Key) : SheetKey

private fun List<TextRun>.flatText(): String = joinToString("") { run ->
    when (run) {
        is TextRun.Plain -> run.text
        is TextRun.Paren -> "(${run.text})"
        is TextRun.Link -> run.text
        TextRun.Break -> " "
        else -> ""
    }
}.trim()

private fun RichDoc.firstLine(): String? =
    joinToString(" ") { p -> p.runs.flatText() }.trim().takeIf { it.isNotBlank() }

@Composable
internal fun MentionPreviewSheetContent(
    doc: ProjectDoc,
    hiddenUnlocked: Boolean,
    targetKindName: String,
    targetKey: Key,
    onOpenDetail: () -> Unit,
    onOpenRefPanel: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val kind = LinkTargetKind.entries.firstOrNull { it.name == targetKindName }
    // Rules 19 and 20: re-checked while open (the gate can relock, the target can go to the trash).
    val previewable = remember(doc, kind, targetKey, hiddenUnlocked) {
        kind != null && MentionTap.previewable(doc, LinkTarget(kind, targetKey), hiddenUnlocked)
    }
    LaunchedEffect(previewable) { if (!previewable) onClose() }
    val info: Triple<String, String, String>? = remember(doc, kind, targetKey, previewable) {
        if (!previewable) null else when (kind) {
            LinkTargetKind.CHARACTER -> doc.index.character(targetKey)?.let { c ->
                Triple("인물", c.name, c.oneLiner?.takeIf { it.isNotBlank() } ?: c.speechSummary?.takeIf { it.isNotBlank() } ?: "등록된 인물 카드")
            }
            LinkTargetKind.TERM -> doc.index.term(targetKey)?.let { t ->
                Triple("용어", t.name, t.description.firstLine() ?: "등록된 세계관 용어")
            }
            LinkTargetKind.PLACE -> doc.index.place(targetKey)?.let { p ->
                Triple("장소", p.name, p.description.firstLine() ?: "등록된 장소")
            }
            LinkTargetKind.FACTION -> doc.index.faction(targetKey)?.let { f ->
                Triple("세력", f.name, f.description.firstLine() ?: "등록된 세력")
            }
            LinkTargetKind.EVENT -> doc.world.events.firstOrNull { it.key == targetKey && it.deletedAt == null }?.let { e ->
                Triple("사건", e.title, e.description.firstLine() ?: "연표 사건")
            }
            else -> null
        }
    }

    SheetHeader(title = info?.second ?: io.github.lenticualrstone.civilight.feature.script.ScriptCopy.QUOTE_PREVIEW, closeLabel = io.github.lenticualrstone.civilight.feature.script.ScriptCopy.CLOSE, onClose = onClose)
    if (info == null) return
    val (kindLabel, title, summary) = info
    val cardShape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 16.dp)
            .clip(cardShape)
            .background(colors.tint)
            .insetOutline(colors.line, 1.dp, cardShape)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (kind == LinkTargetKind.CHARACTER) {
            Avatar(
                name = title,
                size = AvatarSize.Picker,
                image = io.github.lenticualrstone.civilight.ui.image.rememberCharacterAvatarPainter(doc, targetKey),
            )
        } else {
            val icon = when (kind) {
                LinkTargetKind.TERM -> IconId.Book
                LinkTargetKind.PLACE -> IconId.Pin
                LinkTargetKind.EVENT -> IconId.Clock
                else -> IconId.Layers
            }
            CivilightIcon(icon, contentDescription = null, size = 22.dp, tint = colors.acc)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            CivilightText(kindLabel, type.sec, color = colors.acc)
            CivilightText(title, type.itemTitle, color = colors.ink, maxLines = 1)
            CivilightText(summary, type.small, color = colors.ink2, maxLines = 3)
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CivilightButton("참고 패널", onClick = { onClose(); onOpenRefPanel() }, variant = ButtonVariant.GHOST, modifier = Modifier.weight(1f))
        CivilightButton("자세히 열기", onClick = { onClose(); onOpenDetail() }, variant = ButtonVariant.PRIMARY, modifier = Modifier.weight(1f))
    }
}

/**
 * One row of `씬 목차`: the real scene code (`씬 3`, as the scene head, so hiding a scene leaves a
 * gap instead of renumbering), the title (`미정` without one) and the pacing line.
 */
internal data class OutlineRow(
    val sceneKey: Key,
    val ordinal: Int,
    val title: String,
    val blocks: Int,
    val chars: Int,
    val playTime: String,
) {
    val label: String get() = WriteCopy.sceneLabel(ordinal)

    /** The manuscript row a tap scrolls to: the scene head, drawn for every listed scene (blank ones too). */
    val jumpRowKey: String get() = ScriptRowKeys.head(sceneKey)
}

/**
 * `씬 목차` model (ordinal = the scene head's `Labels.sceneOrdinal`): the live scenes of the numbering unit [unitKey] in manuscript order (the scenes
 * the manuscript draws). Rule 19: while hidden items are locked, scenes hidden themselves or under a
 * hidden episode or chapter are left out (the same check as [SpeakerFocus]).
 */
internal object ScriptOutline {
    fun rows(doc: ProjectDoc, unitKey: Key, hiddenUnlocked: Boolean): List<OutlineRow> =
        doc.manuscript.liveScenes
            .filter { s -> (doc.index.numberingUnitKeyOfScene(s.key) ?: doc.project.key) == unitKey }
            .filter { s -> hiddenUnlocked || !(s.hidden || doc.index.ancestors(s).any { it.hidden }) }
            .map { scene ->
                val blocks = doc.manuscript.liveBlocksOf(scene.key)
                OutlineRow(
                    sceneKey = scene.key,
                    ordinal = Labels.sceneOrdinal(doc, scene.key) ?: 0,
                    title = scene.title?.takeIf { it.isNotBlank() } ?: SceneCopy.UNTITLED,
                    blocks = blocks.size,
                    chars = blocks.sumOf { it.runs.flatText().length },
                    playTime = PlayTimeEstimator.estimate(blocks).formatSummary(),
                )
            }
}

/**
 * `씬 목차` sheet: `렌즈를 이 씬으로 맞추기` for the scene in view (opens the position sheet), then
 * one row per scene. Tap a row → scroll to its head in this screen; `⋯` → the scene menu (`지우기` …).
 */
@Composable
internal fun ScriptOutlineSheetContent(
    doc: ProjectDoc,
    unitKey: Key,
    hiddenUnlocked: Boolean,
    /** The scene in view, for the lens row; null hides the row. */
    lensScene: Key?,
    onSetLens: (sceneKey: Key, label: String) -> Unit,
    onJumpScene: (OutlineRow) -> Unit,
    onSceneMenu: (Key) -> Unit,
    onClose: () -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val rows = remember(doc, unitKey, hiddenUnlocked) { ScriptOutline.rows(doc, unitKey, hiddenUnlocked) }
    val lensRow = lensScene?.let { k -> rows.firstOrNull { it.sceneKey == k } }

    Column(Modifier.fillMaxWidth()) {
        SheetHeader(title = ScriptCopy.OUTLINE_TITLE, closeLabel = ScriptCopy.CLOSE, onClose = onClose)
        SheetScrollBody {
            if (lensRow != null) {
                ListRow(
                    title = ScriptCopy.SET_LENS_TO_SCENE,
                    subtitle = lensRow.label,
                    compact = true,
                    leading = { CivilightIcon(IconId.Clock, contentDescription = null, size = 18.dp, tint = colors.acc) },
                    onClick = { onSetLens(lensRow.sceneKey, lensRow.label) },
                )
                Spacer(Modifier.height(6.dp))
            }
            if (rows.isEmpty()) {
                EmptyBlock(title = ScriptCopy.NO_SCENES, icon = IconId.Outline)
            } else {
                rows.forEach { row ->
                    val shape = RoundedCornerShape(14.dp)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(shape)
                            .background(colors.tint.copy(alpha = 0.55f))
                            .insetOutline(colors.line, 1.dp, shape)
                            .pressable(onClick = { onClose(); onJumpScene(row) }, scaleOnPress = false, shape = shape)
                            .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            CivilightText("${row.label} · ${row.title}", type.itemTitleCompact, color = colors.ink, maxLines = 1)
                            CivilightText(ScriptCopy.outlineStats(row.blocks, row.chars, row.playTime), type.small, color = colors.ink3, maxLines = 1)
                        }
                        CivilightIconButton(
                            IconId.More,
                            ScriptCopy.sceneMenuLabel(row.ordinal),
                            { onSceneMenu(row.sceneKey) },
                            tint = colors.ink2,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SpeakerFocusSheetContent(
    doc: ProjectDoc,
    hiddenUnlocked: Boolean,
    unitKey: Key,
    focusedSpeakerKey: Key?,
    onSelectSpeaker: (Key?) -> Unit,
    onClose: () -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val stats = remember(doc, unitKey, hiddenUnlocked) { SpeakerFocus.stats(doc, unitKey, hiddenUnlocked) }

    Column(Modifier.fillMaxWidth()) {
        SheetHeader(title = "화자 포커스 · 대사 비중", closeLabel = "닫기", onClose = onClose)
        SheetScrollBody {
            if (focusedSpeakerKey != null) {
                CivilightButton(
                    "포커스 해제 (전체 보기)",
                    onClick = { onSelectSpeaker(null); onClose() },
                    variant = ButtonVariant.GHOST,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                )
            }
            if (stats.isEmpty()) {
                EmptyBlock(title = "대사 없음", icon = IconId.User)
            } else {
                SectionLabel("이 회차 화자별 대사 비중")
                Spacer(Modifier.height(6.dp))
                stats.forEach { (ch, count, pct) ->
                    val selected = ch.key == focusedSpeakerKey
                    ListRow(
                        title = ch.name,
                        subtitle = buildString {
                            append("대사 ${count}줄 (${pct}%)")
                            ch.speechSummary?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
                        },
                        leading = {
                            Avatar(
                                name = ch.name,
                                size = AvatarSize.List,
                                image = io.github.lenticualrstone.civilight.ui.image.rememberCharacterAvatarPainter(doc, ch.key),
                            )
                        },
                        trailing = {
                            if (selected) {
                                CivilightIcon(IconId.Check, contentDescription = null, size = 18.dp, tint = colors.acc)
                            } else {
                                CivilightText("${pct}%", type.small, color = colors.ink3)
                            }
                        },
                        onClick = {
                            onSelectSpeaker(if (selected) null else ch.key)
                            onClose()
                        },
                    )
                }
            }
        }
    }
}

/** One row of the speaker focus sheet. */
internal data class SpeakerShare(val character: io.github.lenticualrstone.civilight.model.cast.Character, val lines: Int, val percent: Int)

/**
 * (0.5.0) Dialogue share per speaker in the live scenes directly under [unitKey] (or [unitKey] itself).
 * Rule 19: while hidden items are locked, scenes hidden themselves or under a hidden episode/chapter and
 * hidden speakers are left out, and the share is taken over what remains, so no number reveals them.
 */
internal object SpeakerFocus {
    fun stats(doc: ProjectDoc, unitKey: Key, hiddenUnlocked: Boolean): List<SpeakerShare> {
        val scenes = doc.manuscript.liveScenes.filter { s ->
            (s.parentKey == unitKey || s.key == unitKey) &&
                (hiddenUnlocked || !(s.hidden || doc.index.ancestors(s).any { it.hidden }))
        }
        val dialogues = scenes.flatMap { doc.manuscript.liveBlocksOf(it.key) }
            .filter { it.kind.known == BlockKind.DIALOGUE && it.speakerKey != null }
            .mapNotNull { b ->
                val ch = doc.index.character(b.speakerKey!!)?.takeIf { it.deletedAt == null } ?: return@mapNotNull null
                if (!hiddenUnlocked && ch.hidden) null else ch
            }
        val total = dialogues.size.coerceAtLeast(1)
        return dialogues.groupBy { it.key }
            .map { (_, list) -> SpeakerShare(list.first(), list.size, (list.size * 100f / total).toInt()) }
            .sortedByDescending { it.lines }
    }
}
