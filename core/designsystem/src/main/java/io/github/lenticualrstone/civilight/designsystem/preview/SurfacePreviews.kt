package io.github.lenticualrstone.civilight.designsystem.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ArrowEdge
import io.github.lenticualrstone.civilight.designsystem.component.BlockTextKind
import io.github.lenticualrstone.civilight.designsystem.component.BottomGlassBar
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightSheetLayout
import io.github.lenticualrstone.civilight.designsystem.component.CivilightToast
import io.github.lenticualrstone.civilight.designsystem.component.CommandBar
import io.github.lenticualrstone.civilight.designsystem.component.DialogCard
import io.github.lenticualrstone.civilight.designsystem.component.EmptyBlock
import io.github.lenticualrstone.civilight.designsystem.component.ErrorBanner
import io.github.lenticualrstone.civilight.designsystem.component.FieldTableRow
import io.github.lenticualrstone.civilight.designsystem.component.GlassScaffold
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.LensChip
import io.github.lenticualrstone.civilight.designsystem.component.LensChipState
import io.github.lenticualrstone.civilight.designsystem.component.LensEditBanner
import io.github.lenticualrstone.civilight.designsystem.component.NoResults
import io.github.lenticualrstone.civilight.designsystem.component.PagerMarks
import io.github.lenticualrstone.civilight.designsystem.component.PanelCard
import io.github.lenticualrstone.civilight.designsystem.component.Pill
import io.github.lenticualrstone.civilight.designsystem.component.PickerRow
import io.github.lenticualrstone.civilight.designsystem.component.PopoverCard
import io.github.lenticualrstone.civilight.designsystem.component.ProgressBar
import io.github.lenticualrstone.civilight.designsystem.component.ReadOnlyBanner
import io.github.lenticualrstone.civilight.designsystem.component.ScriptBlock
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SidePanel
import io.github.lenticualrstone.civilight.designsystem.component.SpeakerLabel
import io.github.lenticualrstone.civilight.designsystem.component.SpeakerPill
import io.github.lenticualrstone.civilight.designsystem.component.StepBarsRow
import io.github.lenticualrstone.civilight.designsystem.component.StepList
import io.github.lenticualrstone.civilight.designsystem.component.TabSets
import io.github.lenticualrstone.civilight.designsystem.component.TopGlassBar
import io.github.lenticualrstone.civilight.designsystem.component.TourCard
import io.github.lenticualrstone.civilight.designsystem.component.TourDim
import io.github.lenticualrstone.civilight.designsystem.component.TourProgress
import io.github.lenticualrstone.civilight.designsystem.glass.GlassMode
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.text.ScriptRun
import io.github.lenticualrstone.civilight.designsystem.text.SpeakerMarker
import io.github.lenticualrstone.civilight.designsystem.text.MarkerKind

@Composable
private fun SampleManuscript() {
    Column {
        ScriptBlock(listOf(ScriptRun.Plain("“멈추지 마. 저건 등불이 아니야.”")), speaker = SpeakerLabel("리엔", "리엔", "#042"))
        Spacer(Modifier.height(26.dp))
        ScriptBlock(
            listOf(ScriptRun.Paren("(숨을 삼키며) "), ScriptRun.Plain("오리지늄은 타지 않아. 빛만 남지.")),
            speaker = SpeakerLabel("???", "하르트", "#043", listOf(SpeakerMarker(MarkerKind.CHANGE))),
            selected = true,
        )
        Spacer(Modifier.height(26.dp))
        ScriptBlock(listOf(ScriptRun.Plain("바람이 잿더미를 쓸고 지나갔다.")), kind = BlockTextKind.NARRATION)
    }
}

@Preview(name = "glass bars · dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, widthDp = 390, heightDp = 640)
@Preview(name = "glass bars · light", widthDp = 390, heightDp = 640)
@Composable
private fun GlassScaffoldPreview() = PreviewSurface(glassMode = GlassMode.SCRIM, padding = false) {
    GlassScaffold(
        topBar = {
            TopGlassBar(title = "잿빛 새벽의 등불", smallLine = "1-7 · 씬 3 · 2일째 새벽", back = IconAction("서재") {}, barHeight = 118.dp) {
                LensChip("이 씬 · 1-7", LensChipState.SCRIPT_POSITION, {})
                CivilightIconButton(IconId.Layers, "연출 · 메모 표시", {})
            }
        },
        bottomBar = {
            BottomGlassBar(extraRow = {
                SpeakerPill("리엔", "리 · 리엔", {})
                Spacer(Modifier.weight(1f))
                Pill("줄바꿈", {}, primary = true)
            }) { CommandBar("명령 또는 검색 · 인물 · 블록 · 씬 이동", {}, onMic = {}) }
        },
        overlay = { padding ->
            CivilightToast(
                "블록 #043 지움 · ◆ 1개는 #044 자리로",
                Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = padding.calculateBottomPadding() + 6.dp),
                onAction = {},
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) { SampleManuscript() }
    }
}

@Preview(name = "opaque · dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, widthDp = 390, heightDp = 300)
@Preview(name = "opaque · light", widthDp = 390, heightDp = 300)
@Composable
private fun OpaqueBarsPreview() = PreviewSurface(glassMode = GlassMode.OPAQUE, padding = false) {
    GlassScaffold(
        topBar = { TopGlassBar(title = "설정", smallLine = "서재") },
        bottomBar = { BottomGlassBar { CommandBar("명령 또는 검색", {}, onMic = {}) } },
    ) { SampleManuscript() }
}

@Preview(name = "sheet · dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, widthDp = 390, heightDp = 700)
@Preview(name = "sheet · light", widthDp = 390, heightDp = 700)
@Composable
private fun SheetPreview() = PreviewSurface(padding = false) {
    CivilightSheetLayout(
        sheetVisible = true,
        onDismissRequest = {},
        sidePadding = 16.dp,
        sheet = {
            SheetHeader("화자", onClose = {})
            PickerRow("리엔", selected = true, onClick = {}, subtitle = "등불 결사 · 견습")
            PickerRow("하르트", selected = false, onClick = {}, subtitle = "??? (1-8까지)")
            SheetActions {
                CivilightButton("취소", {}, Modifier.weight(1f), variant = ButtonVariant.GHOST)
                CivilightButton("바꾸기", {}, Modifier.weight(2f))
            }
        },
    ) { SampleManuscript() }
}

@Preview(name = "panel · dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, widthDp = 800, heightDp = 500)
@Preview(name = "panel · light", widthDp = 800, heightDp = 500)
@Composable
private fun SidePanelPreview() = PreviewSurface(padding = false) {
    GlassScaffold(overlay = {
        SidePanel(onClose = {}, tabs = TabSets.scriptPanel, selectedTab = 0, modifier = Modifier.align(Alignment.TopEnd)) {
            Spacer(Modifier.height(16.dp))
            PanelCard {
                FieldTableRow("관계", "리엔의 마음 · 경계", point = "1-7 #043부터", marker = MarkerKind.CHANGE, divider = false)
                CivilightButton("고치기", {}, variant = ButtonVariant.COMPACT_GHOST)
            }
        }
    }) { Box(Modifier.padding(start = 16.dp, top = 24.dp).fillMaxWidth(0.6f)) { SampleManuscript() } }
}

@CivilightPreviews
@Composable
private fun DialogsAndPopoverPreview() = PreviewSurface {
    DialogCard("‘유리 정원’ 지우기", Modifier.padding(0.dp)) {
        FieldTableRow("씬 · 인물 · 페이지", "6 · 4 · 3", numeric = true)
        FieldTableRow("이동", "휴지통 · 30일 보관")
        androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().padding(top = 16.dp)) {
            CivilightButton("취소", {}, Modifier.weight(1f), variant = ButtonVariant.GHOST)
            Spacer(Modifier.padding(4.dp))
            CivilightButton("휴지통으로", {}, Modifier.weight(1f), variant = ButtonVariant.DANGER)
        }
    }
    PopoverCard(
        "오리지늄 등불",
        tag = "후보 · 끊김",
        line = "첫 공개 1-7 · 잇기 시 ▲ 공개 전 사용",
        actions = {
            CivilightButton("잇기", {}, Modifier.weight(1f))
            CivilightButton("아님", {}, Modifier.weight(1f), variant = ButtonVariant.GHOST)
        },
    )
}

@CivilightPreviews
@Composable
private fun BannersAndToastPreview() = PreviewSurface {
    LensEditBanner("1-5 시점 값 고치는 중", "최신으로", {}, subtitle = "고치면 1-5부터 적용")
    ReadOnlyBanner("예시 프로젝트 · 읽기 전용", subtitle = "온보딩 후 삭제", stepCount = 4, currentStep = 1)
    CivilightToast("블록 #043 지움 · ◆ 1개는 #044 자리로", onAction = {})
}

@CivilightPreviews
@Composable
private fun StatesPreview() = PreviewSurface {
    EmptyBlock("인물 없음", subtitle = "이름만 적어도 시작 · 나머지는 나중에", primaryLabel = "인물 추가", onPrimary = {}, secondaryLabel = "라이프 레코더에서", onSecondary = {})
    NoResults("결과 없음", context = "‘오리지누’ · 숨긴 프로젝트 제외")
    ErrorBanner("백업 실패", subtitle = "Drive 연결 끊김") {
        CivilightButton("다시", {}, variant = ButtonVariant.COMPACT_GHOST)
    }
}

@CivilightPreviews
@Composable
private fun ProgressPreview() = PreviewSurface {
    StepBarsRow(4, 1, skipLabel = "건너뛰기", onSkip = {})
    TourProgress(5, 2, label = "모듈 안내")
    PagerMarks(4, 0)
    ProgressBar(0.64f, valueText = "218 / 340 MB", estimateText = "약 40초")
    StepList(listOf("파일 읽기", "되살리는 중", "확인"), current = 1)
}

@Preview(name = "tour · dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, widthDp = 390, heightDp = 420)
@Preview(name = "tour · light", widthDp = 390, heightDp = 420)
@Composable
private fun TourPreview() = PreviewSurface(padding = false) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp)) { SampleManuscript() }
        TourDim(Rect(24f, 40f, 300f, 120f))
        TourCard(
            stepCount = 4,
            stepIndex = 0,
            title = "명령 칸",
            nextLabel = "다음",
            onNext = {},
            label = "모듈 안내",
            subtitle = "인물 · 블록 · 씬 이동과 검색",
            skipLabel = "건너뛰기",
            onSkip = {},
            arrowEdge = ArrowEdge.TOP,
            modifier = Modifier.align(Alignment.Center).padding(16.dp),
        )
    }
}
