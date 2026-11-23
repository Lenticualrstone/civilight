package io.github.lenticualrstone.civilight.designsystem.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardBody
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardFields
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardMode
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardState
import io.github.lenticualrstone.civilight.designsystem.component.CreateCard
import io.github.lenticualrstone.civilight.designsystem.component.CreateKind
import io.github.lenticualrstone.civilight.designsystem.component.EditSwipeButtonsRow
import io.github.lenticualrstone.civilight.designsystem.component.IconLabelButton
import io.github.lenticualrstone.civilight.designsystem.component.InputContext
import io.github.lenticualrstone.civilight.designsystem.component.InputKind
import io.github.lenticualrstone.civilight.designsystem.component.InputMark
import io.github.lenticualrstone.civilight.designsystem.component.InputSpeaker
import io.github.lenticualrstone.civilight.designsystem.component.ListCreateRow
import io.github.lenticualrstone.civilight.designsystem.component.PickerCreateRow
import io.github.lenticualrstone.civilight.designsystem.component.ScriptInputActions
import io.github.lenticualrstone.civilight.designsystem.component.ScriptInputBar
import io.github.lenticualrstone.civilight.designsystem.component.ScriptInputReadOnlyRow
import io.github.lenticualrstone.civilight.designsystem.component.ScriptInputState
import io.github.lenticualrstone.civilight.designsystem.component.SwipeSide
import io.github.lenticualrstone.civilight.designsystem.component.SwipeTabsRow
import io.github.lenticualrstone.civilight.designsystem.component.TwoSidedSwipeRow
import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonSpec
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

// Previews of the 0.2.0 writing components (U-14 input bar, SCR-30 swipe rows, U-15 create card, DS-70 buttons).
// The sample words (`리엔`, `세라`) are preview content, not UI copy.

private val noActions = ScriptInputActions(onEvent = {}, onSend = {})
private val rien = InputSpeaker("c-rien", "리엔")

@CivilightPreviews
@Composable
private fun InputBarDefaultPreview() = PreviewSurface(padding = false) {
    ScriptInputBar(ScriptInputState(speaker = rien, sceneNumber = 3), noActions, glass = false)
}

@CivilightPreviews
@Composable
private fun InputBarTypingPreview() = PreviewSurface(padding = false) {
    val text = "아직 문이 열려 있어.\n들어가 보자."
    ScriptInputBar(
        ScriptInputState(text = text, selectionStart = text.length, selectionEnd = text.length, speaker = rien, sceneNumber = 3, imeVisible = true, focused = true),
        noActions,
        context = InputContext(hasTargetBlock = true, canUndo = true),
        glass = false,
    )
}

@CivilightPreviews
@Composable
private fun InputBarAfterPreview() = PreviewSurface(padding = false) {
    ScriptInputBar(ScriptInputState(kind = InputKind.NARRATION, mark = InputMark.After("044"), sceneNumber = 3), noActions, glass = false)
}

@CivilightPreviews
@Composable
private fun InputBarEditingPreview() = PreviewSurface(padding = false) {
    val text = "멈춰. 거기서 더 오면 쏜다."
    ScriptInputBar(
        ScriptInputState(text = text, selectionStart = 3, selectionEnd = 3, speaker = rien, mark = InputMark.Editing("044"), sceneNumber = 3),
        noActions,
        mergeOffReason = "종류 다름",
        glass = false,
    )
}

@CivilightPreviews
@Composable
private fun InputBarReadOnlyPreview() = PreviewSurface(padding = false) {
    ScriptInputReadOnlyRow(onSecondary = {}, onPrimary = {}, glass = false)
}

@CivilightPreviews
@Composable
private fun SwipeRowsPreview() = PreviewSurface {
    Box(Modifier.fillMaxWidth().height(72.dp)) {
        EditSwipeButtonsRow({}, {}, {}, {}, {})
    }
    Box(Modifier.fillMaxWidth().height(72.dp)) {
        SwipeTabsRow({}, {}, {}, timeHasChange = true)
    }
    TwoSidedSwipeRow(
        side = SwipeSide.CLOSED,
        onSideChange = {},
        referenceActions = { SwipeTabsRow({}, {}, {}) },
        editActions = { EditSwipeButtonsRow({}, {}, {}, {}, {}) },
        modifier = Modifier.fillMaxWidth().height(72.dp),
    ) {
        CivilightText("블록", CivilightTheme.type.body, Modifier.fillMaxWidth().height(72.dp))
    }
}

@CivilightPreviews
@Composable
private fun CreateCardPreview() = PreviewSurface {
    val state = CreateCardState.open(CreateKind.CHARACTER, CreateCardMode.LIST, "세라")
    CreateCardBody(CreateCard.title(state.kind), state.mode, state.canCreate, {}, {}, {}) {
        CreateCardFields(state, { _, _ -> }, focusName = false)
    }
}

@CivilightPreviews
@Composable
private fun CreateRowsPreview() = PreviewSurface {
    ListCreateRow(CreateKind.PLACE, {})
    PickerCreateRow(CreateKind.CHARACTER, query = "세라", matched = false, onClick = {})
}

@CivilightPreviews
@Composable
private fun IconButtonClassesPreview() = PreviewSurface {
    Row {
        CivilightIconButton(IconButtonSpec.SEARCH, {})
        CivilightIconButton(IconButtonSpec.DISPLAY_LAYERS, {})
        CivilightIconButton(IconButtonSpec.CLOSE, {})
        CivilightIconButton(IconButtonSpec.VIEWER_PREV, {})
        CivilightIconButton(IconButtonSpec.VIEWER_NEXT, {})
    }
    Row {
        CivilightIconButton(IconButtonSpec.NEXT_SCENE, {})
        CivilightIconButton(IconButtonSpec.ADD_POINT, {})
        CivilightIconButton(IconButtonSpec.CAPTION_TOGGLE, {}, on = true)
    }
    IconLabelButton(IconId.Graph, "관계도", {})
}
