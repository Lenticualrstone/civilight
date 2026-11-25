package io.github.lenticualrstone.civilight.feature.cast.profile

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CheckboxRow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.EmptyBlock
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.RadioRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.cast.AppearanceBars
import io.github.lenticualrstone.civilight.domain.cast.Appearances
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.ProfileTabConfig
import io.github.lenticualrstone.civilight.model.app.ProfileTabKind
import io.github.lenticualrstone.civilight.model.app.ProfileTabPreset
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun

private fun List<TextRun>.flatText(): String = joinToString("") { run ->
    when (run) {
        is TextRun.Plain -> run.text
        is TextRun.Paren -> "(${run.text})"
        is TextRun.Link -> run.text
        TextRun.Break -> " "
        else -> ""
    }
}.trim()

/**
 * (0.5.2) Clean horizontal scrollable underline tab bar matching the night-manuscript-r3 Profile aesthetic:
 * transparent background over BottomGlassBar, bold `ink` active label + crisp `2.dp` accent underline.
 */
@Composable
internal fun ModularProfileTabBar(
    tabs: List<ProfileTabConfig>,
    selectedId: String,
    onSelect: (ProfileTabConfig) -> Unit,
    onManageTemplates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val scroll = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            val active = tab.id == selectedId
            val fg by animateColorAsState(if (active) colors.ink else colors.ink3, label = "profileTabFg")
            val barColor by animateColorAsState(if (active) colors.acc else Color.Transparent, label = "profileTabBar")
            Column(
                Modifier
                    .pressable(onClick = { onSelect(tab) }, scaleOnPress = false)
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CivilightText(
                    tab.label,
                    type.small.copy(fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal),
                    color = fg,
                    maxLines = 1,
                )
                Box(
                    Modifier
                        .height(2.dp)
                        .size(width = 24.dp, height = 2.dp)
                        .background(barColor, RoundedCornerShape(1.dp)),
                )
            }
        }
        Box(
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .pressable(onClick = onManageTemplates, scaleOnPress = false, shape = RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            CivilightIcon(IconId.Sliders, contentDescription = "탭 프리셋 설정", size = 15.dp, tint = colors.ink3)
        }
    }
}

/** One script appearance entry extracted automatically from the manuscript for `작중 행적`. */
internal data class ScriptAppearanceEntry(
    val sceneKey: Key,
    val sceneTitle: String,
    val blockKey: Key,
    val isDialogue: Boolean,
    val excerpt: String,
)

/** Scans the live manuscript for every dialogue spoken by or `@` mention referencing [characterKey]. */
internal fun scanCharacterAppearances(
    doc: ProjectDoc,
    characterKey: Key,
    hiddenUnlocked: Boolean,
): List<ScriptAppearanceEntry> {
    val result = mutableListOf<ScriptAppearanceEntry>()
    val sceneLevel = doc.project.structure.levels.lastIndex
    val liveScenes = doc.story.units.filter { unit ->
        unit.deletedAt == null &&
            unit.level == sceneLevel &&
            (hiddenUnlocked || !unit.hidden)
    }

    for (scene in liveScenes) {
        val unitBlocks = doc.manuscript.liveBlocksOf(scene.key)
        val title = scene.title?.takeIf { it.isNotBlank() } ?: "씬"
        for (block in unitBlocks) {
            val isSpeaker = block.kind.known == BlockKind.DIALOGUE && block.speakerKey == characterKey
            val isMentioned = !isSpeaker && block.runs.any { run ->
                run is TextRun.Link && run.target.kind.known == LinkTargetKind.CHARACTER && run.target.key == characterKey
            }
            if (!isSpeaker && !isMentioned) continue
            val plain = block.runs.flatText()
            if (plain.isNotBlank()) {
                result += ScriptAppearanceEntry(
                    sceneKey = scene.key,
                    sceneTitle = title,
                    blockKey = block.key,
                    isDialogue = isSpeaker,
                    excerpt = if (plain.length > 96) plain.take(96) + "…" else plain,
                )
            }
        }
    }
    return result
}

/**
 * (0.5.0) `작중 행적` tab: combines appearance bar summary with an interactive scene-by-scene
 * script trace where tapping any excerpt jumps directly to that block in the Script Editor.
 */
@Composable
internal fun AppearancesTraceTab(
    doc: ProjectDoc,
    characterKey: Key,
    hiddenUnlocked: Boolean,
    onJumpToBlock: (Key) -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val bars: AppearanceBars = remember(doc, characterKey, hiddenUnlocked) { Appearances.of(doc, characterKey, hiddenUnlocked) }
    val entries = remember(doc, characterKey, hiddenUnlocked) { scanCharacterAppearances(doc, characterKey, hiddenUnlocked) }
    val grouped = remember(entries) { entries.groupBy { it.sceneKey to it.sceneTitle } }
    val dialogueCount = remember(entries) { entries.count { it.isDialogue } }
    val mentionCount = remember(entries) { entries.count { !it.isDialogue } }

    Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppearanceSection(bars)
        if (entries.isEmpty()) {
            EmptyBlock(
                icon = IconId.Edit,
                title = "아직 스크립트에 등장하지 않았습니다",
                subtitle = "원고에서 이 인물의 대사를 쓰거나 @로 인용하면 작중 행적이 자동으로 정리됩니다.",
            )
        } else {
            SectionLabel("스크립트 행적 · 대사 ${dialogueCount}회 · 언급 ${mentionCount}회")
            grouped.forEach { (scenePair, items) ->
                val (_, sceneTitle) = scenePair
                val cardShape = RoundedCornerShape(14.dp)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(cardShape)
                        .background(colors.tint.copy(alpha = 0.45f))
                        .insetOutline(colors.line, 1.dp, cardShape)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CivilightText(sceneTitle, type.itemTitleCompact, color = colors.ink, maxLines = 1)
                        CivilightText("${items.size}건", type.small, color = colors.acc)
                    }
                    items.take(8).forEach { entry ->
                        val rowShape = RoundedCornerShape(10.dp)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(rowShape)
                                .background(colors.bg.copy(alpha = 0.65f))
                                .pressable(onClick = { onJumpToBlock(entry.blockKey) }, scaleOnPress = false, shape = rowShape)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CivilightIcon(
                                if (entry.isDialogue) IconId.Edit else IconId.User,
                                contentDescription = null,
                                size = 14.dp,
                                tint = if (entry.isDialogue) colors.acc else colors.ink3,
                            )
                            CivilightText(
                                entry.excerpt,
                                type.small,
                                color = colors.ink,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            CivilightIcon(IconId.Next, contentDescription = null, size = 14.dp, tint = colors.ink3)
                        }
                    }
                }
            }
        }
    }
}

/**
 * (0.5.0) Modular section tab (`장비`, `능력`, `과거`, `여담`, or user-added custom tabs).
 * Displays matching custom fields + quick field add button, and for `여담` also exposes
 * existing speech summary & secret note links so zero legacy data is ever hidden.
 */
@Composable
internal fun ModularSectionTab(
    doc: ProjectDoc,
    tab: ProfileTabConfig,
    state: ProfileState,
    actions: RecordActions,
    speechSummary: String?,
    onEditSpeechSummary: (() -> Unit)?,
    onOpenSpeech: () -> Unit,
    onOpenSecret: () -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val keywords = remember(tab) {
        when (tab.kind) {
            ProfileTabKind.EQUIPMENT -> listOf("장비", "무기", "소지", "의상", "도구", "아이템", "착용")
            ProfileTabKind.ABILITIES -> listOf("능력", "스킬", "마법", "특기", "약점", "전투", "스탯", "기술")
            ProfileTabKind.PAST -> listOf("과거", "배경", "출신", "이력", "역사", "기억", "사건")
            ProfileTabKind.TRIVIA -> listOf("여담", "취미", "버릇", "좋아", "싫어", "성격", "트리비아", "메모")
            else -> listOf(tab.label)
        }
    }
    val allRows = remember(state) { state.overview.items + state.header.rows }
    val matchedRows = remember(allRows, keywords) {
        allRows.filter { row -> keywords.any { kw -> row.label.contains(kw, ignoreCase = true) } }
            .distinctBy { it.label }
    }

    Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel(tab.label)
            if (actions.editable) {
                TextAction("+ 항목 추가", actions.onAddField, color = colors.acc, style = type.small)
            }
        }
        if (matchedRows.isEmpty()) {
            val hint = when (tab.kind) {
                ProfileTabKind.EQUIPMENT -> "무기 · 의상 · 소지품 항목을 추가해 정리해 보세요."
                ProfileTabKind.ABILITIES -> "스킬 · 특기 · 약점 항목을 추가해 정리해 보세요."
                ProfileTabKind.PAST -> "출신 · 배경 서사 · 과거 사건 항목을 추가해 정리해 보세요."
                ProfileTabKind.TRIVIA -> "버릇 · 취미 · 성격 여담을 자유롭게 기록해 보세요."
                else -> "${tab.label} 관련 커스텀 항목을 추가해 보세요."
            }
            EmptyBlock(
                icon = IconId.File,
                title = "${tab.label} 항목이 비어 있습니다",
                subtitle = hint,
                primaryLabel = if (actions.editable) "항목 추가" else null,
                onPrimary = if (actions.editable) actions.onAddField else null,
            )
        } else {
            matchedRows.forEach { row ->
                FieldRowItem(doc, row, actions)
            }
        }

        // Preserve effortless access to speech & secret data inside `여담` when using the default 7-tab preset.
        if (tab.kind == ProfileTabKind.TRIVIA) {
            Spacer(Modifier.height(6.dp))
            SectionLabel("말투 · 비밀 바로가기")
            ListRow(
                title = "말투 · 호칭",
                subtitle = speechSummary?.takeIf { it.isNotBlank() } ?: "말투 요약 및 인물별 호칭 관리",
                leading = { CivilightIcon(IconId.Edit, contentDescription = null, tint = colors.ink2) },
                trailing = { CivilightIcon(IconId.Next, contentDescription = null, tint = colors.ink3) },
                onClick = onOpenSpeech,
            )
            ListRow(
                title = "비밀 · 스포일러 노트",
                subtitle = "잠금 보호 가능한 비공개 설정 및 메모",
                leading = { CivilightIcon(IconId.Lock, contentDescription = null, tint = colors.ink2) },
                trailing = { CivilightIcon(IconId.Next, contentDescription = null, tint = colors.ink3) },
                onClick = onOpenSecret,
            )
        }
    }
}

/** In-profile bottom sheet to switch character tab presets or toggle/customize tab modules. */
@Composable
internal fun ColumnScope.ProfileTabTemplateSheetContent(
    settings: AppSettings,
    onSelectPreset: (ProfileTabPreset) -> Unit,
    onUpdateTabs: (List<ProfileTabConfig>) -> Unit,
    onClose: () -> Unit,
) {
    val colors = CivilightTheme.colors
    val activeTabs = settings.customProfileTabs.ifEmpty { settings.profileTabPreset.defaultTabs() }
    var presetTab by remember { mutableStateOf(settings.profileTabPreset) }

    SheetHeader(title = "인물 카드 탭 템플릿", closeLabel = "닫기", onClose = onClose)
    SheetScrollBody {
        CivilightText("프리셋 불러오기", CivilightTheme.type.sec, color = colors.ink3, modifier = Modifier.padding(bottom = 6.dp))
        ProfileTabPreset.entries.forEach { preset ->
            val label = when (preset) {
                ProfileTabPreset.STANDARD -> "표준 창작 (기본)"
                ProfileTabPreset.MODERN_DRAMA -> "현대극 · 드라마"
                ProfileTabPreset.GAME_VN -> "게임 · VN 제작"
                ProfileTabPreset.MINIMAL -> "미니멀"
            }
            RadioRow(
                title = label,
                subtitle = preset.defaultTabs().joinToString(" · ") { it.label },
                selected = presetTab == preset && settings.customProfileTabs.isEmpty(),
                onClick = {
                    presetTab = preset
                    onSelectPreset(preset)
                },
            )
        }
        Spacer(Modifier.height(12.dp))
        CivilightText("표시할 탭 모듈 선택 · 순서", CivilightTheme.type.sec, color = colors.ink3, modifier = Modifier.padding(bottom = 6.dp))
        activeTabs.forEachIndexed { idx, tab ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    CheckboxRow(
                        label = tab.label,
                        checked = tab.enabled,
                        onCheckedChange = { checked ->
                            val next = activeTabs.map { if (it.id == tab.id) it.copy(enabled = checked) else it }
                            if (next.any { it.enabled }) onUpdateTabs(next)
                        },
                    )
                }
                if (idx > 0) {
                    TextAction(
                        "↑",
                        onClick = {
                            val list = activeTabs.toMutableList()
                            val moved = list.removeAt(idx)
                            list.add(idx - 1, moved)
                            onUpdateTabs(list)
                        },
                        color = colors.ink2,
                        style = CivilightTheme.type.small,
                    )
                }
                if (idx < activeTabs.lastIndex) {
                    TextAction(
                        "↓",
                        onClick = {
                            val list = activeTabs.toMutableList()
                            val moved = list.removeAt(idx)
                            list.add(idx + 1, moved)
                            onUpdateTabs(list)
                        },
                        color = colors.ink2,
                        style = CivilightTheme.type.small,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        var newTabName by remember { mutableStateOf("") }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField(
                value = newTabName,
                onValueChange = { newTabName = it },
                placeholder = "새 커스텀 탭 이름 (예: 대사집 · 소속)",
                modifier = Modifier.weight(1f),
            )
            TextAction(
                "추가",
                onClick = {
                    val trimmed = newTabName.trim()
                    if (trimmed.isNotEmpty()) {
                        val id = "custom_${System.currentTimeMillis()}"
                        onUpdateTabs(activeTabs + ProfileTabConfig(id = id, label = trimmed, kind = ProfileTabKind.CUSTOM, enabled = true))
                        newTabName = ""
                    }
                },
                color = colors.acc,
                style = CivilightTheme.type.small,
            )
        }
    }
}
