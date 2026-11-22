package io.github.lenticualrstone.civilight.designsystem.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.icon.AppMark
import io.github.lenticualrstone.civilight.designsystem.icon.BiometricButton
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.icon.MarkGeometry
import io.github.lenticualrstone.civilight.designsystem.icon.Wordmark
import io.github.lenticualrstone.civilight.designsystem.text.MarkerGlyph
import io.github.lenticualrstone.civilight.designsystem.text.MarkerKind
import io.github.lenticualrstone.civilight.designsystem.text.MarkerLine
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ColorRole
import io.github.lenticualrstone.civilight.designsystem.theme.TypeRole

@CivilightPreviews
@Composable
private fun ColorTokensPreview() = PreviewSurface {
    val colors = CivilightTheme.colors
    ChipFlow(gap = 8.dp) {
        ColorRole.entries.forEach { role ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(colors.of(role)))
                CivilightText(role.name.lowercase(), CivilightTheme.type.micro, color = colors.ink3)
            }
        }
    }
}

@CivilightPreviews
@Composable
private fun TypeRolesPreview() = PreviewSurface {
    val colors = CivilightTheme.colors
    listOf(
        TypeRole.DISPLAY to "리엔",
        TypeRole.TITLE to "화자",
        TypeRole.HEADER_TITLE to "잿빛 새벽의 등불",
        TypeRole.HEADING to "1-7 씬 3 · 꺼진 등불",
        TypeRole.ITEM_TITLE to "리엔",
        TypeRole.LINE to "“멈추지 마. 저건 등불이 아니야.”",
        TypeRole.BODY to "바람이 잿더미를 쓸고 지나갔다.",
        TypeRole.COMMAND to "명령 또는 검색",
        TypeRole.LABEL to "닫기",
        TypeRole.SPEAKER_NAME to "리엔",
        TypeRole.SMALL to "이름 가림 · 검색 제외",
        TypeRole.SEC to "인물 기록 007 · 시점 1-5",
        TypeRole.NUMBER to "05:40 · 1-7 · 18/24",
        TypeRole.DIR to "▸ 배경  하층 거리(새벽)   ♪ 잔불",
        TypeRole.WORDMARK to "CIVILIGHT",
    ).forEach { (role, sample) -> CivilightText(sample, CivilightTheme.type.of(role), color = colors.ink) }
}

@CivilightPreviews
@Composable
private fun IconsPreview() = PreviewSurface {
    ChipFlow(gap = 12.dp) {
        IconId.entries.forEach { CivilightIcon(it, contentDescription = it.name, size = 24.dp) }
    }
}

@CivilightPreviews
@Composable
private fun MarkersPreview() = PreviewSurface {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        MarkerKind.entries.forEach { MarkerGlyph(it, size = 20.dp) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        MarkerGlyph(MarkerKind.CHANGE, line = MarkerLine.DOTTED, size = 20.dp)
        MarkerGlyph(MarkerKind.NOTICE, line = MarkerLine.DOTTED, size = 20.dp)
    }
    CivilightText("◆ 관계 · 리엔의 마음 · 1-7 #043부터", CivilightTheme.type.command)
    CivilightText("◇ #044 용어 첫 공개 · 오리지늄 등불", CivilightTheme.type.command, color = CivilightTheme.colors.ink3)
    CivilightText("▲ 공개 전 사용", CivilightTheme.type.command, color = CivilightTheme.colors.warn)
    CivilightText("⌁ 작가 메모  여기서 등불이 두 번째로 깜박이게 할지", CivilightTheme.type.dir, color = CivilightTheme.colors.ink3)
}

@CivilightPreviews
@Composable
private fun AppMarkPreview() = PreviewSurface {
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
        AppMark(size = 96.dp)
        AppMark(size = 64.dp, geometry = MarkGeometry.Static64)
        AppMark(size = 64.dp, barProgress = 0.5f)
        BiometricButton(onClick = {}, contentDescription = "지문으로 열기")
    }
    Wordmark()
}
