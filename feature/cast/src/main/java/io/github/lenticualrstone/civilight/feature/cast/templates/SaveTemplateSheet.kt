package io.github.lenticualrstone.civilight.feature.cast.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CheckboxRow
import io.github.lenticualrstone.civilight.designsystem.component.ChoiceCard
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.RadioRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.SwitchRow
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.field.FieldTemplates
import io.github.lenticualrstone.civilight.feature.cast.common.editContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import kotlinx.serialization.Serializable

/** `템플릿으로 저장` (CST-57, S31), opened from `프로젝트 설정 › 인물 항목`. Feature-local. */
@Serializable
data class SaveTemplateSheet(val projectKey: Key, val kind: EntityKind = EntityKind.CHARACTER) : SheetKey

/**
 * 이름 (with `◇ 같은 이름 템플릿 있음`), `어디에` (`새 템플릿` · `있는 템플릿 바꾸기`), `담을 항목` by kind
 * (hidden definitions start unchecked), `이 인물만 항목 N · 빠짐`, `항목 정의만 · 값 없음`, the
 * `숨긴 템플릿` switch (on in a hidden project), then `저장`. Nothing in the project changes.
 */
@Composable
fun SheetScope.SaveTemplateSheetContent(sheet: SaveTemplateSheet, doc: ProjectDoc) {
    val host = this
    val deps = LocalFeatureDeps.current
    val now = rememberTemplatesNow(deps)
    val visible = remember(now) { FieldTemplates.visible(now.all, now.hiddenUnlocked) }
    val writes = remember(deps) { TemplateWrites(deps) }
    var name by rememberSaveable(sheet) { mutableStateOf("") }
    var replacing by rememberSaveable(sheet) { mutableStateOf(false) }
    var replaceKey by rememberSaveable(sheet) { mutableStateOf<Key?>(null) }
    var pickList by rememberSaveable(sheet) { mutableStateOf(SaveTemplateModel.initialPicks(doc).toList()) }
    var hidden by rememberSaveable(sheet) { mutableStateOf(SaveTemplateModel.initialHidden(doc)) }
    val picks = pickList.toSet()
    val target = if (replacing) SaveTarget.REPLACE else SaveTarget.NEW
    val view = remember(doc, picks, name, hidden, visible, target, replaceKey) {
        SaveTemplateModel.view(doc, picks, name, hidden, visible, target, replaceKey)
    }

    SheetHeader(TemplateCopy.SAVE_ROW, onClose = ::close, closeLabel = TemplateCopy.BTN_CLOSE)
    SheetScrollBody(Modifier.padding(top = 14.dp)) {
        CivilightTextField(name, { name = it }, Modifier.fillMaxWidth(), label = TemplateCopy.NAME, hint = view.sameName)
        SectionLabel(TemplateCopy.WHERE, Modifier.padding(top = 16.dp, bottom = 6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceCard(TemplateCopy.NEW, !replacing, { replacing = false }, compact = true)
            ChoiceCard(
                TemplateCopy.REPLACE, replacing, { replacing = true }, subtitle = TemplateCopy.REPLACE_SUB, compact = true,
            )
        }
        if (replacing) {
            for (t in visible) {
                RadioRow(t.name, t.key == replaceKey, {
                    replaceKey = t.key
                    name = t.name
                }, Modifier.padding(start = 16.dp), subtitle = FieldTemplates.kindCounts(t))
            }
        }
        SectionLabel(view.picksTitle, Modifier.padding(top = 16.dp, bottom = 2.dp))
        for (group in view.groups) {
            CivilightText(group.label, CivilightTheme.type.label, Modifier.padding(top = 10.dp), color = CivilightTheme.colors.ink2)
            for (row in group.rows) {
                CheckboxRow(
                    row.name,
                    row.checked,
                    { on -> pickList = SaveTemplateModel.toggle(picks, row.key, on).toList() },
                    note = if (row.hidden) TemplateCopy.HIDDEN_MARK else null,
                    subtitle = CastCopy.joinAll(listOf(row.sub, view.calcNotices[row.key])),
                    dense = true,
                )
            }
        }
        view.scopedLine?.let { CivilightText(it, CivilightTheme.type.small, Modifier.padding(top = 10.dp), color = CivilightTheme.colors.ink3) }
        CivilightText(TemplateCopy.DEFS_ONLY, CivilightTheme.type.small, Modifier.padding(top = 4.dp), color = CivilightTheme.colors.ink3)
        SwitchRow(
            TemplateCopy.HIDDEN, hidden, { hidden = it }, Modifier.padding(top = 10.dp),
            subtitle = view.visibleNotice ?: TemplateCopy.HIDDEN_SUB,
        )
    }
    SheetActions {
        CivilightButton(TemplateCopy.BTN_SAVE, {
            val ctx = deps.editContext()
            val replace = if (replacing) visible.firstOrNull { it.key == replaceKey } else null
            val saved = SaveTemplateModel.save(doc, picks, name, hidden, now.all, ctx, replace)
            writes.put(saved) { deps.toasts.show(TemplateCopy.saved(saved.name), actionLabel = null) }
            host.close()
        }, Modifier.weight(1f), enabled = view.canSave)
    }
}
