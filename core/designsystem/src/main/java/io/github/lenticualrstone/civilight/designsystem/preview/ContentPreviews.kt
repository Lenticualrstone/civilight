package io.github.lenticualrstone.civilight.designsystem.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.Avatar
import io.github.lenticualrstone.civilight.designsystem.component.AvatarSize
import io.github.lenticualrstone.civilight.designsystem.component.Breadcrumb
import io.github.lenticualrstone.civilight.designsystem.component.CheckboxMark
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.ConditionResult
import io.github.lenticualrstone.civilight.designsystem.component.ConditionRow
import io.github.lenticualrstone.civilight.designsystem.component.CropFrame
import io.github.lenticualrstone.civilight.designsystem.component.DirectionLine
import io.github.lenticualrstone.civilight.designsystem.component.FieldActionRow
import io.github.lenticualrstone.civilight.designsystem.component.FieldTableRow
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.FormatTile
import io.github.lenticualrstone.civilight.designsystem.component.GeneratorStepRow
import io.github.lenticualrstone.civilight.designsystem.component.GlowMode
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.ImageCard
import io.github.lenticualrstone.civilight.designsystem.component.ImageTile
import io.github.lenticualrstone.civilight.designsystem.component.KeyValueLine
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.LockRow
import io.github.lenticualrstone.civilight.designsystem.component.MapPin
import io.github.lenticualrstone.civilight.designsystem.component.MapToolbar
import io.github.lenticualrstone.civilight.designsystem.component.MapVertex
import io.github.lenticualrstone.civilight.designsystem.component.NodeCard
import io.github.lenticualrstone.civilight.designsystem.component.PersonChip
import io.github.lenticualrstone.civilight.designsystem.component.Rating
import io.github.lenticualrstone.civilight.designsystem.component.RatingBadge
import io.github.lenticualrstone.civilight.designsystem.component.RecorderGlowBand
import io.github.lenticualrstone.civilight.designsystem.component.RecordingMark
import io.github.lenticualrstone.civilight.designsystem.component.RouteStop
import io.github.lenticualrstone.civilight.designsystem.component.RowDivider
import io.github.lenticualrstone.civilight.designsystem.component.ScaleBar
import io.github.lenticualrstone.civilight.designsystem.component.SceneHead
import io.github.lenticualrstone.civilight.designsystem.component.ScriptBlock
import io.github.lenticualrstone.civilight.designsystem.component.SearchHit
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SettingsRow
import io.github.lenticualrstone.civilight.designsystem.component.ShardArt
import io.github.lenticualrstone.civilight.designsystem.component.SpeakerLabel
import io.github.lenticualrstone.civilight.designsystem.component.StepChip
import io.github.lenticualrstone.civilight.designsystem.component.SwipeRevealBox
import io.github.lenticualrstone.civilight.designsystem.component.SwipeTabsRow
import io.github.lenticualrstone.civilight.designsystem.component.TerrainSwatch
import io.github.lenticualrstone.civilight.designsystem.component.TimelineLabel
import io.github.lenticualrstone.civilight.designsystem.component.TravelBar
import io.github.lenticualrstone.civilight.designsystem.component.WhenPicker
import io.github.lenticualrstone.civilight.designsystem.component.WhenPickerModel
import io.github.lenticualrstone.civilight.designsystem.component.holoFoil
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.text.DirectionCue
import io.github.lenticualrstone.civilight.designsystem.text.MarkerKind
import io.github.lenticualrstone.civilight.designsystem.text.MarkerLine
import io.github.lenticualrstone.civilight.designsystem.text.ScriptRun
import io.github.lenticualrstone.civilight.designsystem.text.SpeakerMarker
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.FixedColors
import io.github.lenticualrstone.civilight.designsystem.theme.ResolvedTheme
import io.github.lenticualrstone.civilight.designsystem.theme.Terrain

@CivilightPreviews
@Composable
private fun ListRowsPreview() = PreviewSurface {
    SectionLabel("프로젝트 · 2", trailing = "레코더 · 정리 안 됨 1 ›", onTrailing = {})
    ListRow(
        "잿빛 새벽의 등불",
        subtitle = "씬 32 · 오늘 08:12",
        leading = { RatingBadge(Rating.R15) },
        trailing = { CivilightIconButton(IconId.More, "메뉴", {}) },
        onClick = {},
    )
    ListRow("리엔", subtitle = "인물 기록 007", leading = { Avatar("리엔") }, selected = true, onClick = {})
    ListRow("노아", subtitle = "◇ 지운 인물 · 관계 1", divider = RowDivider.DASHED, titleDotted = true)
    ListRow("블록 #043", leading = { CheckboxMark(true) })
    SettingsRow("글자 크기", {}, value = "보통", subtitle = "본문 18 · 행간 1.7")
    FieldTableRow("소속", "등불 결사", point = "1-3부터", marker = MarkerKind.CHANGE)
    FieldTableRow("키", "18 / 24", numeric = true)
    KeyValueLine("때", "1-7 · 2일째 · 새벽")
    FieldActionRow("장소", "결사의 문", "바꾸기", {})
    LockRow("숨긴 프로젝트 2", "지문으로 보기", {}, subtitle = "이름 가림 · 검색 · 위젯 · 알림 제외")
}

@CivilightPreviews
@Composable
private fun ScriptPreview() = PreviewSurface(padding = false) {
    SceneHead("씬 4 · 결사의 문 · 2일째 아침 06:10", subtitle = "전환 문구 · 결사의 문 앞 · 06:10 · 직접", menu = IconAction("씬 4 메뉴") {})
    DirectionLine(listOf(DirectionCue(MarkerKind.DIRECTION, "배경", "하층 거리(새벽)"), DirectionCue(MarkerKind.BGM, null, "잔불")))
    ScriptBlock(
        listOf(ScriptRun.Plain("“"), ScriptRun.Link("오리지늄 등불"), ScriptRun.Plain("은 타지 않아. "), ScriptRun.Candidate("잿더미 시장"), ScriptRun.Plain("에서 봤어.”")),
        speaker = SpeakerLabel("리엔", "리엔", "#042"),
        showCandidates = true,
    )
    ScriptBlock(
        listOf(ScriptRun.Paren("(숨을 삼키며) "), ScriptRun.Plain("“"), ScriptRun.HiddenLink("숨긴 인물"), ScriptRun.Plain("도 거기 있었지.”")),
        speaker = SpeakerLabel("???", "하르트", "#043", listOf(SpeakerMarker(MarkerKind.CHANGE, MarkerLine.DOTTED))),
        selected = true,
    )
    ScriptBlock(
        listOf(ScriptRun.Plain("“"), ScriptRun.DeletedRef("노아"), ScriptRun.Plain("는 오지 않아.”")),
        speaker = SpeakerLabel("노아", "노아", deleted = true),
    )
    ScriptBlock(listOf(ScriptRun.Plain("멈추지 마")), speaker = SpeakerLabel("리엔", "리엔"), typing = true)
    DirectionLine(listOf(DirectionCue(MarkerKind.MEMO, "작가 메모", "여기서 등불이 두 번째로 깜박이게 할지")))
}

@CivilightPreviews
@Composable
private fun SwipeAndSearchPreview() = PreviewSurface(padding = false) {
    SwipeRevealBox(revealed = true, onRevealedChange = {}, actions = { SwipeTabsRow({}, {}, {}, timeHasChange = true) }) {
        ScriptBlock(listOf(ScriptRun.Plain("“멈추지 마. 저건 등불이 아니야.”")), speaker = SpeakerLabel("리엔", "리엔"))
    }
    Box(Modifier.width(390.dp)) {
        SearchHit(
            "1-7 씬 3 · 나레이션",
            "“오리지늄은 타지 않아. 빛만 남지.” 오리지늄 등불이 꺼졌다.",
            listOf(1 until 4, 21 until 24),
            trailingWarning = "▲ 공개 전 사용",
            animate = false,
        )
    }
}

@CivilightPreviews
@Composable
private fun CardsPreview() = PreviewSurface {
    val theme = if (CivilightTheme.colors.isDark) ResolvedTheme.DARK else ResolvedTheme.LIGHT
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ImageCard(
            "잿빛 새벽의 등불",
            Modifier.width(180.dp).height(240.dp),
            meta = "씬 32 · 1-7",
            rating = Rating.R18,
            statusChip = "쓰는 중 · 홀로",
            more = IconAction("메뉴") {},
        ) { ShardArt(FixedColors.mapArt(theme), Modifier.holoFoil(Offset(0.35f, -0.2f), theme)) }
        ImageTile("새벽 파편", selected = true, onClick = {}, modifier = Modifier.width(86.dp)) { ShardArt(FixedColors.mapArt(theme).reversed()) }
    }
    Box(Modifier.fillMaxWidth().height(200.dp)) {
        ShardArt(FixedColors.mapArt(theme))
        CropFrame(ratio = 3f / 4f)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar("리엔", size = AvatarSize.Pill)
        Avatar("하르트", size = AvatarSize.List)
        Avatar("세라", size = AvatarSize.Picker)
        NodeCard("등불 결사", subtitle = "세력")
        NodeCard("메모", memo = true)
        NodeCard("꺼진 등불", subtitle = "사건", selected = true)
    }
}

@CivilightPreviews
@Composable
private fun MapPreview() = PreviewSurface {
    MapToolbar(selectedIndex = 1, onSelect = {})
    GeneratorStepRow("해안선", locked = true, onToggleLock = {}, onReroll = {}, seed = "#4821", selected = true)
    GeneratorStepRow("산맥", locked = false, onToggleLock = {}, onReroll = {}, seed = "#1093")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        StepChip("한 칸", true, {}, icon = IconId.Brush)
        StepChip("채우기", false, {}, icon = IconId.Fill)
        TerrainSwatch(Terrain.FOREST, "숲", true, {})
        TerrainSwatch(Terrain.SEA, "바다", false, {})
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        MapPin("결사의 문", selected = true)
        MapPin("상층 관문", dim = true)
        MapVertex()
        MapVertex(last = true)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        RouteStop("1-7", chosen = true)
        RouteStop("1-5", chosen = false)
        PersonChip("하르트")
        ScaleBar("10 km")
    }
    Breadcrumb(listOf("세계", "하층 구역"))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TimelineLabel("1-6", current = false)
        TimelineLabel("1-7", current = true)
    }
    FormatTile(selected = true, onClick = {}, label = "색면") { ShardArt(FixedColors.terrain.values.take(3)) }
}

@CivilightPreviews
@Composable
private fun TimeAndTravelPreview() = PreviewSurface {
    WhenPicker(
        WhenPickerModel(
            header = "때 · 직전 씬 기준",
            base = "씬 2 · 1-7 · 1일째 저녁",
            dayOptions = listOf("같은 날", "+1일", "+2일", "직접"),
            selectedDay = 1,
            partOptions = listOf("새벽", "아침", "낮", "저녁", "밤"),
            selectedPart = 0,
            exactTime = "05:40",
            exactSelected = false,
            precisionLabel = "정밀도",
            precisionOptions = listOf("정확", "대략"),
            selectedPrecision = 0,
            resultTitle = "1-7 · 2일째 · 새벽",
            resultSubtitle = "이야기 전체 13일째",
        ),
        onDay = {}, onPart = {}, onExact = {}, onPrecision = {},
    )
    ConditionRow("산맥", detail = "북쪽 · 눈 덮임", coveredCells = setOf(0, 1, 2), more = IconAction("메뉴") {})
    ConditionRow("항구 도시", detail = "남쪽 해안 · 장소 ‘잿더미 시장’", result = ConditionResult.NEEDS_CHECK)
    TravelBar(
        requiredValue = "도보 2일 4시간",
        availableValue = "9시간 40분 – 12시간 40분",
        requiredMinutes = 3120,
        availableMinutes = 760,
        warning = "▲ 이동 시간 부족 · 경로 38 km · 도보 15 km/일",
        chips = { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { FilterChip("+2일로", false, {}) } },
    )
}

@Preview(name = "glow · dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, widthDp = 390, heightDp = 420)
@Preview(name = "glow · light", widthDp = 390, heightDp = 420)
@Composable
private fun RecorderGlowPreview() = PreviewSurface(padding = false) {
    Row(Modifier.size(390.dp, 40.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RecordingMark()
    }
    RecorderGlowBand(GlowMode.STILL)
}
