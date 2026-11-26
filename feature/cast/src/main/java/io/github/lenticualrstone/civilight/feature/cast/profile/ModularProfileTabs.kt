package io.github.lenticualrstone.civilight.feature.cast.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTabs
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.TabGap
import io.github.lenticualrstone.civilight.ui.tour.tourTarget
import io.github.lenticualrstone.civilight.designsystem.component.EmptyBlock
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.cast.AppearanceBars
import io.github.lenticualrstone.civilight.domain.cast.Appearances
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.ProfileTabConfig
import io.github.lenticualrstone.civilight.model.app.ProfileTabKind
import io.github.lenticualrstone.civilight.model.app.ProfileTabPreset
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


/**
 * The record's tab row on the immersive bottom glass (`Immersive.dc.html`): the design-system [CivilightTabs]
 * (gap 18, `tab` role, selected state, 44 dp targets, sliding underline on Motion tokens) and the 44 dp `탭 구성`
 * icon button after it. The `관계` tab is the cast tour's `관계` hole (LIB-19).
 */
@Composable
internal fun ModularProfileTabBar(
    tabs: List<ProfileTabConfig>,
    selectedId: String,
    onSelect: (ProfileTabConfig) -> Unit,
    onManageTemplates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val labels = remember(tabs) { tabs.map { it.label } }
    val selected = tabs.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        CivilightTabs(
            tabs = labels,
            selectedIndex = selected,
            onSelect = { i -> tabs.getOrNull(i)?.let(onSelect) },
            modifier = Modifier.weight(1f),
            gap = TabGap.Default,
            tabModifier = { i -> if (tabs.getOrNull(i)?.kind == ProfileTabKind.RELATIONS) Modifier.tourTarget(CastTourSteps.RELATIONS) else Modifier },
        )
        CivilightIconButton(IconId.Sliders, ModularTabCopy.TAB_SETUP, onManageTemplates)
    }
}

/** The tabs the record shows: the enabled ones; `게임 텍스트` only while `게임 제작 도구` is on (CST-74). */
internal object ProfileTabSet {
    fun visible(configured: List<ProfileTabConfig>, gameTools: Boolean): List<ProfileTabConfig> =
        configured.filter { it.enabled && (gameTools || it.kind != ProfileTabKind.GAME_TEXT) }
            .ifEmpty { ProfileTabPreset.STANDARD.defaultTabs().filter { it.enabled && (gameTools || it.kind != ProfileTabKind.GAME_TEXT) } }
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
    // The manuscript scan runs off the main thread; null = not ready yet (no empty state flash).
    val groups by produceState<List<AppearanceSceneGroup>?>(null, doc, characterKey, hiddenUnlocked) {
        value = withContext(Dispatchers.Default) { AppearanceTrace.group(AppearanceTrace.scan(doc, characterKey, hiddenUnlocked)) }
    }
    var expanded by remember(characterKey, hiddenUnlocked) { mutableStateOf(emptySet<Key>()) }

    Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppearanceSection(bars)
        val loaded = groups ?: return@Column
        if (loaded.isEmpty()) {
            EmptyBlock(
                icon = IconId.Edit,
                title = ModularTabCopy.TRACE_EMPTY,
                subtitle = ModularTabCopy.TRACE_EMPTY_SUB,
            )
        } else {
            val entries = loaded.flatMap { it.entries }
            val dialogueCount = entries.count { it.isDialogue }
            val mentionCount = entries.size - dialogueCount
            SectionLabel(ModularTabCopy.traceTitle(dialogueCount, mentionCount))
            loaded.forEach { group ->
                val open = group.sceneKey in expanded
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
                        CivilightText(group.sceneTitle, type.itemTitleCompact, color = colors.ink, maxLines = 1)
                        CivilightText(ModularTabCopy.count(group.entries.size), type.small, color = colors.acc)
                    }
                    group.visible(open).forEach { entry ->
                        val rowShape = RoundedCornerShape(10.dp)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
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
                    val more = group.more(open)
                    if (more > 0) {
                        TextAction(
                            ModularTabCopy.more(more),
                            onClick = { expanded = expanded + group.sceneKey },
                            color = colors.acc,
                            style = type.small,
                        )
                    }
                }
            }
        }
    }
}

/** Terse copy of the modular section tabs' empty state (labels and fragments, no sentences). */
internal object ModularTabCopy {
    const val TAB_SETUP = "탭 구성"
    const val ADD_FIELD = "+ 항목"
    const val BTN_ADD_FIELD = "항목 더하기"
    const val TRACE_EMPTY = "등장 없음"
    const val TRACE_EMPTY_SUB = "대사 · @ 인용"
    const val SHORTCUTS = "말투 · 비밀 바로가기"
    const val SPEECH_ROW = "말투 · 호칭"
    const val SPEECH_ROW_SUB = "말투 요약 · 인물별 호칭"
    const val SECRET_ROW = "비밀 · 스포일러 노트"
    const val SECRET_ROW_SUB = "비공개 설정 · 메모 · 잠글 수 있음"
    const val NEW_VOICE_LINE = "보이스 대사 더하기"

    /** All constant labels (copy lint). */
    val all: List<String> = listOf(
        TAB_SETUP, ADD_FIELD, BTN_ADD_FIELD, TRACE_EMPTY, TRACE_EMPTY_SUB, SHORTCUTS,
        SPEECH_ROW, SPEECH_ROW_SUB, SECRET_ROW, SECRET_ROW_SUB, NEW_VOICE_LINE,
    )

    fun empty(label: String): String = "$label 없음"

    fun traceTitle(dialogue: Int, mention: Int): String = "스크립트 행적 · 대사 ${dialogue}회 · 언급 ${mention}회"

    fun count(n: Int): String = "${n}건"

    fun more(n: Int): String = "더 보기 · $n"

    /** `← 여담`: back from the 말투 or 비밀 view opened inside another tab (no particle to get wrong). */
    fun backTo(label: String): String = "← $label"

    /** What the tab collects; a custom tab has none. */
    fun hint(kind: ProfileTabKind): String? = when (kind) {
        ProfileTabKind.EQUIPMENT -> "무기 · 의상 · 소지품"
        ProfileTabKind.ABILITIES -> "스킬 · 특기 · 약점"
        ProfileTabKind.PAST -> "출신 · 배경 · 과거 사건"
        ProfileTabKind.TRIVIA -> "버릇 · 취미 · 성격"
        else -> null
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
                TextAction(ModularTabCopy.ADD_FIELD, actions.onAddField, color = colors.acc, style = type.small)
            }
        }
        if (matchedRows.isEmpty()) {
            EmptyBlock(
                icon = IconId.File,
                title = ModularTabCopy.empty(tab.label),
                subtitle = ModularTabCopy.hint(tab.kind),
                primaryLabel = if (actions.editable) ModularTabCopy.BTN_ADD_FIELD else null,
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
            SectionLabel(ModularTabCopy.SHORTCUTS)
            ListRow(
                title = ModularTabCopy.SPEECH_ROW,
                subtitle = speechSummary?.takeIf { it.isNotBlank() } ?: ModularTabCopy.SPEECH_ROW_SUB,
                leading = { CivilightIcon(IconId.Edit, contentDescription = null, tint = colors.ink2) },
                trailing = { CivilightIcon(IconId.Next, contentDescription = null, tint = colors.ink3) },
                onClick = onOpenSpeech,
            )
            ListRow(
                title = ModularTabCopy.SECRET_ROW,
                subtitle = ModularTabCopy.SECRET_ROW_SUB,
                leading = { CivilightIcon(IconId.Lock, contentDescription = null, tint = colors.ink2) },
                trailing = { CivilightIcon(IconId.Next, contentDescription = null, tint = colors.ink3) },
                onClick = onOpenSecret,
            )
        }
    }
}
