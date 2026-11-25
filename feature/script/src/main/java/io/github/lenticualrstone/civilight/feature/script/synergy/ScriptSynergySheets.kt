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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.Avatar
import io.github.lenticualrstone.civilight.designsystem.component.AvatarSize
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
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
import io.github.lenticualrstone.civilight.feature.script.panel.PlayTimeEstimator
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.BlockKind
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
    targetKindName: String,
    targetKey: Key,
    onOpenDetail: () -> Unit,
    onOpenRefPanel: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val kind = LinkTargetKind.entries.firstOrNull { it.name == targetKindName }
    val info: Triple<String, String, String>? = remember(doc, kind, targetKey) {
        when (kind) {
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

    SheetHeader(title = info?.second ?: "인용 미리보기", closeLabel = "닫기", onClose = onClose)
    if (info == null) {
        CivilightText("항목을 찾을 수 없거나 삭제되었습니다.", type.small, color = colors.ink3, modifier = Modifier.padding(vertical = 12.dp))
        return
    }
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
            Avatar(name = title, size = AvatarSize.Picker)
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

@Composable
internal fun ScriptOutlineSheetContent(
    doc: ProjectDoc,
    unitKey: Key,
    hiddenUnlocked: Boolean,
    onJumpScene: (Key) -> Unit,
    onClose: () -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val sceneLevel = doc.project.structure.levels.lastIndex
    val scenes = remember(doc, unitKey, hiddenUnlocked, sceneLevel) {
        doc.story.units.filter { u ->
            u.deletedAt == null &&
                u.level == sceneLevel &&
                (u.parentKey == unitKey || u.key == unitKey) &&
                (hiddenUnlocked || !u.hidden)
        }
    }

    Column(Modifier.fillMaxWidth()) {
        SheetHeader(title = "씬 목차 · 페이싱", closeLabel = "닫기", onClose = onClose)
        SheetScrollBody {
            if (scenes.isEmpty()) {
                EmptyBlock(title = "표시할 씬이 없습니다", icon = IconId.Outline)
            } else {
                scenes.forEachIndexed { idx, scene ->
                    val blocks = doc.manuscript.liveBlocksOf(scene.key)
                    val chars = blocks.sumOf { it.runs.flatText().length }
                    val playTime = PlayTimeEstimator.estimate(blocks).formatSummary()
                    val shape = RoundedCornerShape(14.dp)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(shape)
                            .background(colors.tint.copy(alpha = 0.55f))
                            .insetOutline(colors.line, 1.dp, shape)
                            .pressable(onClick = { onClose(); onJumpScene(scene.key) }, scaleOnPress = false, shape = shape)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            val title = scene.title?.takeIf { it.isNotBlank() } ?: "제목 없음"
                            CivilightText("씬 ${idx + 1} · $title", type.itemTitleCompact, color = colors.ink, maxLines = 1)
                            CivilightText("블록 ${blocks.size} · ${chars}자 · 예상 $playTime", type.small, color = colors.ink3, maxLines = 1)
                        }
                        CivilightIcon(IconId.Next, contentDescription = null, size = 16.dp, tint = colors.ink3)
                    }
                }
            }
        }
    }
}

@Composable
internal fun SpeakerFocusSheetContent(
    doc: ProjectDoc,
    unitKey: Key,
    focusedSpeakerKey: Key?,
    onSelectSpeaker: (Key?) -> Unit,
    onClose: () -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val stats = remember(doc, unitKey) {
        val sceneKeys = doc.story.units
            .filter { it.deletedAt == null && (it.parentKey == unitKey || it.key == unitKey) }
            .mapTo(HashSet()) { it.key }
        val dialogues = sceneKeys.flatMap { doc.manuscript.liveBlocksOf(it) }
            .filter { it.kind.known == BlockKind.DIALOGUE && it.speakerKey != null }
        val total = dialogues.size.coerceAtLeast(1)
        dialogues.groupBy { it.speakerKey!! }
            .mapNotNull { (key, list) ->
                val ch = doc.index.character(key)?.takeIf { it.deletedAt == null } ?: return@mapNotNull null
                Triple(ch, list.size, (list.size * 100f / total).toInt())
            }
            .sortedByDescending { it.second }
    }

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
                EmptyBlock(title = "아직 대사가 작성되지 않았습니다", icon = IconId.User)
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
                        leading = { Avatar(name = ch.name, size = AvatarSize.List) },
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
