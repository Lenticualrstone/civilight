package io.github.lenticualrstone.civilight.feature.script.input

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ComposerTab
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.tokenShadow
import io.github.lenticualrstone.civilight.designsystem.glass.GlassBackground
import io.github.lenticualrstone.civilight.designsystem.glass.GlassSurface
import io.github.lenticualrstone.civilight.designsystem.glass.OnGlass
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.LocalBackHandler
import io.github.lenticualrstone.civilight.feature.script.state.ScriptOps
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.ProfileShape
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.ui.image.CharacterPortraitTile

/**
 * (0.7.0) Frosted glass popover anchored directly above the input dock's left contextual slot:
 * - Dialogue: quick speaker picker (scene cast -> recent -> all, tap = switch, + button or long press = multi-speaker).
 * - Narration: `//` comment memo toggle, `@` link picker, and (when GameTools ON) direction cue attachment.
 * - Scene: create untitled scene immediately, edit current scene place/time, open scene outline.
 */
@Composable
internal fun ComposerLeadingPopup(
    tab: ComposerTab,
    doc: ProjectDoc,
    sceneKey: Key?,
    unlocked: Boolean,
    gameTools: Boolean,
    profileShape: ProfileShape,
    primarySpeakerKey: Key?,
    coSpeakerKeys: List<Key>,
    bottomOffset: Dp,
    startPadding: Dp,
    onSelectSpeaker: (Key?) -> Unit,
    onToggleCoSpeaker: (Key) -> Unit,
    onOpenFullSpeakerPicker: () -> Unit,
    onInsertMemoPrefix: () -> Unit,
    onOpenCuePicker: () -> Unit,
    onOpenLinkPicker: () -> Unit,
    onQuickNewScene: () -> Unit,
    onEditCurrentSceneWhere: () -> Unit,
    onOpenOutline: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(20.dp)
    LocalBackHandler.current(true) { onDismiss() }
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
    ) {
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = startPadding, bottom = bottomOffset + 8.dp)
                .width(260.dp)
                .heightIn(max = 320.dp)
                .tokenShadow(shape, CivilightTheme.elevation.popover(colors))
                .clip(shape)
                .pointerInput(Unit) { detectTapGestures { } },
        ) {
            GlassBackground(
                surface = GlassSurface.POPOVER,
                modifier = Modifier.matchParentSize(),
                shape = shape,
                fill = colors.glass.copy(alpha = if (colors.isDark) 0.72f else 0.82f),
            )
            OnGlass {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .insetOutline(colors.edge, 1.dp, shape)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                ) {
                    when (tab) {
                        ComposerTab.DIALOGUE -> SpeakerQuickList(
                            doc = doc,
                            sceneKey = sceneKey,
                            unlocked = unlocked,
                            profileShape = profileShape,
                            primarySpeakerKey = primarySpeakerKey,
                            coSpeakerKeys = coSpeakerKeys,
                            onSelect = { key ->
                                onSelectSpeaker(key)
                                onDismiss()
                            },
                            onToggleCo = onToggleCoSpeaker,
                            onOpenFull = {
                                onDismiss()
                                onOpenFullSpeakerPicker()
                            },
                        )
                        ComposerTab.NARRATION -> {
                            PopupActionRow(
                                icon = IconId.Paper,
                                label = "주석 메모 (//)",
                                sub = "출력 제외",
                                onClick = {
                                    onDismiss()
                                    onInsertMemoPrefix()
                                },
                            )
                            if (gameTools) {
                                PopupActionRow(
                                    icon = IconId.Spark,
                                    label = "연출 큐 첨부",
                                    sub = "배경 · BGM · 효과음 · CG",
                                    onClick = {
                                        onDismiss()
                                        onOpenCuePicker()
                                    },
                                )
                            }
                            PopupActionRow(
                                icon = IconId.Link,
                                label = "@ 항목 잇기",
                                sub = "인물 · 용어 · 장소 · 사건",
                                onClick = {
                                    onDismiss()
                                    onOpenLinkPicker()
                                },
                            )
                        }
                        ComposerTab.SCENE -> {
                            PopupActionRow(
                                icon = IconId.NextScene,
                                label = "빈 장면 바로 만들기",
                                sub = "제목 미정",
                                onClick = {
                                    onDismiss()
                                    onQuickNewScene()
                                },
                            )
                            if (sceneKey != null) {
                                PopupActionRow(
                                    icon = IconId.Pin,
                                    label = "지금 씬 장소 · 때",
                                    sub = "배경 · 시간 설정",
                                    onClick = {
                                        onDismiss()
                                        onEditCurrentSceneWhere()
                                    },
                                )
                            }
                            PopupActionRow(
                                icon = IconId.Outline,
                                label = "씬 목차 열기",
                                sub = "장면 이동 · 관리",
                                onClick = {
                                    onDismiss()
                                    onOpenOutline()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SpeakerQuickList(
    doc: ProjectDoc,
    sceneKey: Key?,
    unlocked: Boolean,
    profileShape: ProfileShape,
    primarySpeakerKey: Key?,
    coSpeakerKeys: List<Key>,
    onSelect: (Key?) -> Unit,
    onToggleCo: (Key) -> Unit,
    onOpenFull: () -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val ordered = remember(doc, sceneKey, unlocked) {
        val visible = doc.characters.filter { it.deletedAt == null && (unlocked || !it.hidden) }
        val byKey = visible.associateBy { it.key }
        val sceneOrder = ScriptOps.sceneCast(doc, sceneKey).mapNotNull { byKey[it] }
        val recentOrder = doc.app.view.script.recentSpeakers.mapNotNull { byKey[it] }
        (sceneOrder + recentOrder + visible).distinctBy { it.key }.take(10)
    }
    val selectedSet = remember(primarySpeakerKey, coSpeakerKeys) {
        buildSet {
            primarySpeakerKey?.let(::add)
            addAll(coSpeakerKeys)
        }
    }

    if (ordered.isEmpty()) {
        CivilightText("등록된 인물 없음", type.small, Modifier.padding(horizontal = 14.dp, vertical = 10.dp), color = colors.ink3)
    } else {
        CivilightText(
            "화자 교체 · 길게 눌러 다중 화자",
            type.micro,
            Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
            color = colors.ink3,
            maxLines = 1,
        )
        ordered.forEach { ch ->
            val isSelected = ch.key in selectedSet
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .background(if (isSelected) colors.sel else androidx.compose.ui.graphics.Color.Transparent)
                    .combinedClickable(
                        onClick = { onSelect(ch.key) },
                        onLongClick = { onToggleCo(ch.key) },
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CharacterPortraitTile(
                    doc = doc,
                    characterKey = ch.key,
                    displayName = ch.name,
                    shape = profileShape,
                    size = 30.dp,
                )
                CivilightText(
                    ch.name,
                    type.itemTitleCompact,
                    Modifier.weight(1f),
                    color = if (isSelected) colors.acc else colors.ink,
                    maxLines = 1,
                )
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) colors.acc.copy(alpha = 0.16f) else colors.tint.copy(alpha = 0.6f))
                        .pressable(onClick = { onToggleCo(ch.key) }, hitTarget = HitTarget.SMALL, shape = CircleShape)
                        .semantics { contentDescription = "${ch.name} 다중 화자"; role = Role.Checkbox },
                    contentAlignment = Alignment.Center,
                ) {
                    CivilightIcon(
                        if (isSelected) IconId.Check else IconId.Plus,
                        contentDescription = null,
                        size = 14.dp,
                        tint = if (isSelected) colors.acc else colors.ink3,
                    )
                }
            }
        }
    }

    PopupActionRow(
        icon = IconId.Plus,
        label = "인물 전체 · 추가",
        sub = null,
        onClick = onOpenFull,
    )
}

@Composable
private fun PopupActionRow(
    icon: IconId,
    label: String,
    sub: String?,
    onClick: () -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .pressable(onClick = onClick, hitTarget = HitTarget.BUTTON, scaleOnPress = false)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CivilightIcon(icon, contentDescription = null, size = 18.dp, tint = colors.ink2)
        Column(Modifier.weight(1f)) {
            CivilightText(label, type.itemTitleCompact, color = colors.ink, maxLines = 1)
            if (sub != null) CivilightText(sub, type.small, color = colors.ink3, maxLines = 1)
        }
    }
}
