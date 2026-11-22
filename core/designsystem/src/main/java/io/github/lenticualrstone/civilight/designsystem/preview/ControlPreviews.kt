package io.github.lenticualrstone.civilight.designsystem.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.AddChip
import io.github.lenticualrstone.civilight.designsystem.component.ButtonRow
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CheckboxMark
import io.github.lenticualrstone.civilight.designsystem.component.CheckboxRow
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.ChoiceCard
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightSwitch
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTabs
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.CommandBar
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.KeyboardTool
import io.github.lenticualrstone.civilight.designsystem.component.KeyboardToolsRow
import io.github.lenticualrstone.civilight.designsystem.component.LayerFlagChip
import io.github.lenticualrstone.civilight.designsystem.component.LensChip
import io.github.lenticualrstone.civilight.designsystem.component.LensChipState
import io.github.lenticualrstone.civilight.designsystem.component.MapValueChip
import io.github.lenticualrstone.civilight.designsystem.component.OutlinedSegmented
import io.github.lenticualrstone.civilight.designsystem.component.PasswordField
import io.github.lenticualrstone.civilight.designsystem.component.Pill
import io.github.lenticualrstone.civilight.designsystem.component.RadioMark
import io.github.lenticualrstone.civilight.designsystem.component.RadioRow
import io.github.lenticualrstone.civilight.designsystem.component.Rating
import io.github.lenticualrstone.civilight.designsystem.component.RatingBadge
import io.github.lenticualrstone.civilight.designsystem.component.RemovableChip
import io.github.lenticualrstone.civilight.designsystem.component.SearchField
import io.github.lenticualrstone.civilight.designsystem.component.SegmentItem
import io.github.lenticualrstone.civilight.designsystem.component.SegmentedControl
import io.github.lenticualrstone.civilight.designsystem.component.SpeakerPill
import io.github.lenticualrstone.civilight.designsystem.component.StepSlider
import io.github.lenticualrstone.civilight.designsystem.component.SwitchRow
import io.github.lenticualrstone.civilight.designsystem.component.TabGap
import io.github.lenticualrstone.civilight.designsystem.component.TabSets
import io.github.lenticualrstone.civilight.designsystem.component.TypeGridCell
import io.github.lenticualrstone.civilight.designsystem.component.ViewToggle
import io.github.lenticualrstone.civilight.designsystem.component.ViewToggleMode
import io.github.lenticualrstone.civilight.designsystem.component.ThemeSwatch
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.ThemeMode

@CivilightPreviews
@Composable
private fun ButtonsPreview() = PreviewSurface {
    ButtonRow(Modifier.fillMaxWidth()) {
        CivilightButton("취소", {}, Modifier.weight(1f), variant = ButtonVariant.GHOST)
        CivilightButton("휴지통으로", {}, Modifier.weight(1f), variant = ButtonVariant.DANGER)
    }
    ButtonRow {
        CivilightButton("쓰기 시작", {})
        CivilightButton("되돌리기", {}, variant = ButtonVariant.GHOST, icon = IconId.Undo)
        CivilightButton("다음", {}, enabled = false)
    }
    CivilightButton("다음", {}, Modifier.fillMaxWidth(), variant = ButtonVariant.PRIMARY_LARGE)
    ButtonRow {
        CivilightButton("최신으로", {}, variant = ButtonVariant.COMPACT_GHOST)
        CivilightButton("지문으로 보기", {}, variant = ButtonVariant.COMPACT_PRIMARY)
    }
    CivilightButton("이 값 고치기", {}, Modifier.fillMaxWidth(), variant = ButtonVariant.TWO_LINE, subText = "1-3 기록을 고침 · 1-3부터 새 값")
    CivilightButton("여기서부터 바뀜 · 1-7", {}, Modifier.fillMaxWidth(), variant = ButtonVariant.TWO_LINE_GHOST, subText = "새 ◆ · 집필 선 1-7 씬 3 뒤")
}

@CivilightPreviews
@Composable
private fun PillsAndToolsPreview() = PreviewSurface {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        SpeakerPill("리엔", "리 · 리엔", {})
        CivilightIconButton(IconId.Layers, "연출 · 메모 표시", {})
        Spacer(Modifier.weight(1f))
        Pill("줄바꿈", {}, primary = true)
    }
    KeyboardToolsRow {
        KeyboardTool({}, icon = IconId.Text, text = "글로 쓰기", on = true)
        KeyboardTool({}, icon = IconId.Link, contentDescription = "연결")
        KeyboardTool({}, text = "@")
    }
    CommandBar("명령 또는 검색 · 인물 · 블록 · 씬 이동", {}, onMic = {})
    CommandBar("명령 또는 검색", {}, typedText = "/대", onClose = {})
}

@CivilightPreviews
@Composable
private fun ControlsPreview() = PreviewSurface {
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
        CivilightSwitch(false, {})
        CivilightSwitch(true, {})
        CivilightSwitch(true, {}, enabled = false)
        RadioMark(false, onClick = {})
        RadioMark(true, onClick = {})
        CheckboxMark(false, onCheckedChange = {})
        CheckboxMark(true, onCheckedChange = {})
        CheckboxMark(true, dense = true)
    }
    SwitchRow("투명도 줄이기", false, {}, subtitle = "유리 대신 불투명 막대 · 흐림 없음")
    RadioRow("기기 설정 따름", true, {}, subtitle = "‘애니메이션 삭제’가 켜져 있으면 줄임")
    RadioRow("항상 줄이기", false, {}, subtitle = "이동 없이 페이드 120 ms")
    CheckboxRow("블록 #043", true, {})
    CheckboxRow("하르트", false, {}, note = "R-15 기본 · 끔")
    ChoiceCard("ALL", false, {}, subtitle = "12세 이하")
    ChoiceCard("R-15", true, {})
    ChoiceCard("R-18", true, {}, selectedOutline = io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme.colors.r18)
    Row { TypeGridCell("대사", true, {}); TypeGridCell("나레이션", false, {}) }
}

@CivilightPreviews
@Composable
private fun SegmentedAndSliderPreview() = PreviewSurface {
    SegmentedControl(listOf("기기 설정", "라이트", "다크"), 0, {})
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ViewToggle(showCards = false, onChange = {})
        ViewToggle(mode = ViewToggleMode.GRID, onChange = {})
        OutlinedSegmented(listOf(SegmentItem("정확"), SegmentItem("대략")), 1, {}, height = 32.dp)
    }
    StepSlider(value = 2, stepCount = 5, onValueChange = {})
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeSwatch(ThemeMode.SYSTEM, "기기 설정", true, {}, Modifier.weight(1f))
        ThemeSwatch(ThemeMode.LIGHT, "라이트", false, {}, Modifier.weight(1f))
        ThemeSwatch(ThemeMode.DARK, "다크", false, {}, Modifier.weight(1f))
    }
}

@CivilightPreviews
@Composable
private fun FieldsPreview() = PreviewSurface {
    CivilightTextField("등불 결사", {}, label = "소속")
    CivilightTextField("172", {}, label = "키", unit = "cm", hint = "단위 그대로")
    CivilightTextField("", {}, label = "확인: 프로젝트 이름 입력", error = "확인 필요")
    PasswordField("abcdefgh", {}, label = "복구 암호")
    SearchField(TextFieldValue("ㄹㅇ", TextRange(2)), {}, rightHint = "초성 검색")
}

@CivilightPreviews
@Composable
private fun ChipsPreview() = PreviewSurface {
    ChipFlow {
        FilterChip("전체 12", true, {})
        FilterChip("주연", false, {})
        FilterChip("등불 결사", false, {})
        FilterChip("이동 4", false, {}, enabled = false)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RemovableChip("연결 · 1-7 씬 3", {}, removeLabel = "빼기", leadingIcon = IconId.Link)
        AddChip("연결", {})
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        MapValueChip("도보 15 km/일", icon = IconId.Route)
        LayerFlagChip("표시 · 용어 후보", "켬", {})
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        RatingBadge(Rating.ALL)
        RatingBadge(Rating.R15)
        RatingBadge(Rating.R18)
    }
}

@CivilightPreviews
@Composable
private fun TabsAndLensPreview() = PreviewSurface {
    CivilightTabs(TabSets.characterRecord, 0, {})
    CivilightTabs(TabSets.scriptPanel, 1, {}, gap = TabGap.Panel)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LensChip("최신 · 1-7", LensChipState.LATEST, {})
        LensChip("시점 · 1-5", LensChipState.PAST, {})
        LensChip("이 씬 · 1-7", LensChipState.SCRIPT_POSITION, {})
    }
}
