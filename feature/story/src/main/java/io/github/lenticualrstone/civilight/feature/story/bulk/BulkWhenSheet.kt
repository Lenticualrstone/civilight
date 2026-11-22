package io.github.lenticualrstone.civilight.feature.story.bulk

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.OutlinedSegmented
import io.github.lenticualrstone.civilight.designsystem.component.SegmentItem
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.story.bulk.WhenMode
import io.github.lenticualrstone.civilight.domain.story.bulk.WhenPreviewRow
import io.github.lenticualrstone.civilight.domain.story.time.word
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.story.TimePrecision
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore
import kotlinx.serialization.Serializable

/** `때 정하기 · 씬 4개` over 장소 · 때 한꺼번에 (STO-33); local to that screen, saved with it. */
@Serializable
data class BulkWhenSheet(val projectKey: Key, val sceneKeys: List<Key>) : SheetKey

/**
 * [BulkWhenSheet] content (board `BulkWhen`): `직전 씬 기준 차례로 | 모두 같은 때`, `씬 사이` chips
 * `같은 날 · +1일 · +2일 · 씬마다 고르기`, `시간대` chips `그대로 · 비워 둠 · 새벽 … 밤`, `정밀도 | 정확 · 대략`
 * (default `대략`), the `미리 보기` rows computed on the current doc, the note and `씬 4개에 넣기`. Nothing is
 * stored before the button; the whole application is one undo step (BLK-02).
 */
@Composable
fun SheetScope.BulkWhenContent(sheet: BulkWhenSheet, vm: BulkViewModel) {
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    var encoded by rememberSaveable { mutableStateOf(BulkWhenForm().encode()) }
    val form = BulkWhenForm.decode(encoded)
    fun set(next: BulkWhenForm) {
        encoded = next.encode()
    }
    val plan = remember(doc, sheet.sceneKeys, encoded) { form.plan(doc, sheet.sceneKeys) }

    SheetHeader(plan.title, onClose = ::close)
    SheetScrollBody {
        ChipFlow(Modifier.padding(top = 10.dp)) {
            FilterChip(BulkCopy.CHAIN, form.mode == WhenMode.CHAIN, { set(form.copy(mode = WhenMode.CHAIN)) })
            FilterChip(BulkCopy.ALL_SAME_TIME, form.mode == WhenMode.ALL_SAME, { set(form.copy(mode = WhenMode.ALL_SAME)) })
        }
        Label(BulkCopy.BETWEEN)
        ChipFlow(Modifier.padding(top = 6.dp)) {
            BulkWhenForm.DAY_CHIPS.forEach { n ->
                FilterChip(dayWord(n), !form.perScene && form.dayChip == n, { set(form.copy(dayChip = n, perScene = false)) })
            }
            if (form.mode == WhenMode.CHAIN) FilterChip(BulkCopy.PER_SCENE, form.perScene, { set(form.copy(perScene = true)) })
        }
        Label(BulkCopy.BAND)
        ChipFlow(Modifier.padding(top = 6.dp)) {
            FilterChip(BulkCopy.KEEP, form.band == BulkWhenForm.KEEP, { set(form.copy(band = BulkWhenForm.KEEP)) })
            FilterChip(BulkCopy.CLEAR, form.band == BulkWhenForm.CLEAR, { set(form.copy(band = BulkWhenForm.CLEAR)) })
            Band.entries.forEach { band -> FilterChip(band.word, form.band == band.name, { set(form.copy(band = band.name)) }) }
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            CivilightText(BulkCopy.PRECISION, type.small, Modifier.weight(1f), color = colors.ink3)
            OutlinedSegmented(
                items = listOf(SegmentItem(BulkCopy.EXACT), SegmentItem(BulkCopy.APPROX)),
                selectedIndex = if (form.precision == TimePrecision.EXACT) 0 else 1,
                onSelect = { set(form.copy(precision = if (it == 0) TimePrecision.EXACT else TimePrecision.APPROX)) },
                height = 32.dp,
            )
        }
        CivilightText(BulkCopy.PREVIEW, type.sec, Modifier.padding(top = 14.dp, bottom = 4.dp), color = colors.ink3)
        plan.rows.forEach { row ->
            PreviewRow(row, perScene = form.perScene && form.mode == WhenMode.CHAIN, chip = form.chipOf(row.sceneKey)) { n ->
                set(form.copy(perSceneChips = form.perSceneChips + (row.sceneKey to n)))
            }
        }
        if (form.precision == TimePrecision.APPROX) CivilightText(BulkCopy.APPROX_NOTE, type.small, Modifier.padding(top = 8.dp), color = colors.ink3)
    }
    CivilightButton(
        text = plan.button,
        onClick = {
            vm.times(sheet.sceneKeys, form)
            close()
        },
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        enabled = !store.readOnly && plan.rows.isNotEmpty(),
    )
}

@Composable
private fun Label(text: String) {
    CivilightText(text, CivilightTheme.type.small, Modifier.padding(top = 12.dp), color = CivilightTheme.colors.ink3)
}

/** `1-6 씬 2 | 1-6 · 2일째 · 대략 | 직전 1-6 씬 1 · 1일째 낮`; under `씬마다 고르기` the row's own day chips. */
@Composable
private fun PreviewRow(row: WhenPreviewRow, perScene: Boolean, chip: Int, onChip: (Int) -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(Modifier.fillMaxWidth().topHairline(colors.line).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            CivilightText(row.scene, type.itemTitleCompact.merge(TextStyle(fontWeight = FontWeight.Bold)), Modifier.weight(1f), color = colors.ink, maxLines = 1)
            CivilightText(row.label, type.dir, color = colors.ink, maxLines = 1)
        }
        CivilightText(row.baseNote, type.small, color = colors.ink3)
        if (perScene) {
            ChipFlow(Modifier.padding(top = 4.dp)) {
                BulkWhenForm.DAY_CHIPS.forEach { n -> FilterChip(dayWord(n), chip == n, { onChip(n) }) }
            }
        }
    }
}

private fun dayWord(n: Int): String = when (n) {
    0 -> BulkCopy.SAME_DAY
    1 -> BulkCopy.PLUS_ONE
    else -> BulkCopy.PLUS_TWO
}
