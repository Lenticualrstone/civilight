package io.github.lenticualrstone.civilight.designsystem.preview

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CropRatioRow
import io.github.lenticualrstone.civilight.designsystem.component.FadingHeaderArt
import io.github.lenticualrstone.civilight.designsystem.component.FloatingDialogCard
import io.github.lenticualrstone.civilight.designsystem.component.FullScreenError
import io.github.lenticualrstone.civilight.designsystem.component.GlassPill
import io.github.lenticualrstone.civilight.designsystem.component.GlassRoundButton
import io.github.lenticualrstone.civilight.designsystem.component.GlowMode
import io.github.lenticualrstone.civilight.designsystem.component.RecorderGlowBand
import io.github.lenticualrstone.civilight.designsystem.component.ShardArt
import io.github.lenticualrstone.civilight.designsystem.component.SheetTabsHeader
import io.github.lenticualrstone.civilight.designsystem.component.TabSets
import io.github.lenticualrstone.civilight.designsystem.component.ToastHost
import io.github.lenticualrstone.civilight.designsystem.component.ValueUnderEditRow
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.FixedColors
import io.github.lenticualrstone.civilight.designsystem.theme.ResolvedTheme

@Preview(name = "profile art · dark", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 390, heightDp = 460)
@Preview(name = "profile art · light", widthDp = 390, heightDp = 460)
@Composable
private fun ProfileArtPreview() = PreviewSurface(padding = false) {
    val theme = if (CivilightTheme.colors.isDark) ResolvedTheme.DARK else ResolvedTheme.LIGHT
    Box {
        FadingHeaderArt { ShardArt(FixedColors.mapArt(theme)) }
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, top = 16.dp, end = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassRoundButton(IconId.Back, "인물", {})
            GlassPill("시점 · 1-5", {})
        }
    }
}

@Preview(name = "error · dark", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 390, heightDp = 520)
@Preview(name = "error · light", widthDp = 390, heightDp = 520)
@Composable
private fun FullScreenErrorPreview() = PreviewSurface(padding = false) {
    FullScreenError(
        icon = IconId.Mic,
        title = "마이크 꺼짐",
        primaryLabel = "다시",
        onPrimary = {},
        modifier = Modifier.fillMaxWidth().height(480.dp),
        secondaryLabel = "글로 쓰기",
        onSecondary = {},
    )
}

@CivilightPreviews
@Composable
private fun SheetPiecesPreview() = PreviewSurface {
    SheetTabsHeader(TabSets.scriptPanel, 1, {}, onClose = {})
    ValueUnderEditRow("소속", "1-5부터 · 다음 변화 2-5", "등불 결사 · 견습", {})
    CropRatioRow(listOf("카드 3:4", "목록 1:1"), 0, {}, note = "규격 고정")
    FloatingDialogCard(
        title = "지문 인식 5회 실패",
        onCancel = {},
        primaryLabel = "PIN 사용",
        onPrimary = {},
        header = "시스템 생체 인증",
        subtitle = "30초 뒤 다시 · 지금은 기기 PIN",
        icon = IconId.Fingerprint,
    )
    ToastHost("블록 #043 지움 · ◆ 1개는 #044 자리로", onAction = {})
}

@Preview(name = "glow recording · dark", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 390, heightDp = 360)
@Preview(name = "glow recording · light", widthDp = 390, heightDp = 360)
@Composable
private fun GlowRecordingPreview() = PreviewSurface(padding = false) {
    Box(Modifier.fillMaxSize()) { RecorderGlowBand(GlowMode.RECORDING, Modifier.align(Alignment.BottomCenter), level = { 0.8f }) }
}

@Preview(name = "glow processing · dark", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 390, heightDp = 360)
@Preview(name = "glow processing · light", widthDp = 390, heightDp = 360)
@Composable
private fun GlowProcessingPreview() = PreviewSurface(padding = false) {
    Box(Modifier.fillMaxSize()) { RecorderGlowBand(GlowMode.PROCESSING, Modifier.align(Alignment.BottomCenter)) }
}
