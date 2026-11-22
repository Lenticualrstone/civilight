package io.github.lenticualrstone.civilight.feature.recorder.link

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SearchField
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.ShownCopy
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.domain.recorder.picker.LinkCandidate
import io.github.lenticualrstone.civilight.domain.recorder.capture.Recording
import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureHub
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureProtection
import io.github.lenticualrstone.civilight.feature.recorder.capture.PickerRequest
import io.github.lenticualrstone.civilight.feature.recorder.capture.PickerTarget
import io.github.lenticualrstone.civilight.model.recorder.RecorderLink
import io.github.lenticualrstone.civilight.ui.picker.PickerCopy
import io.github.lenticualrstone.civilight.ui.security.ProtectWindow

/**
 * The link picker sheet (LRC-20; not drawn on any board): title (`TODO_COPY_LRC_07`), search by
 * name · 초성 · alias, then `글에 나온 이름` and the ranked rest. Rows already linked are checked and
 * not choosable again (a second add is a no-op, LRC-18). Choosing a row adds the link, or replaces
 * the tapped chip's target, and closes the sheet.
 */
@Composable
fun ColumnScope.LinkPickerContent(hub: CaptureHub, onClose: () -> Unit, request: PickerRequest? = null) {
    val state by hub.state.collectAsState()
    // [request] keeps the last request while the sheet leaves (the live one is already null).
    val request = request ?: state.picker
    val links = when (request?.target) {
        PickerTarget.MEMO -> state.memo?.draft?.pendingLinks
        PickerTarget.SESSION -> state.session?.pendingLinks
        null -> null
    }.orEmpty()
    val recordText = when (request?.target) {
        PickerTarget.MEMO -> state.memo?.let { Runs.text(it.draft.runs) }
        PickerTarget.SESSION -> state.session?.let { Recording.transcript(it).text }
        null -> null
    }
    LinkPickerBody(env = state.env, links = links, recordText = recordText, onPick = hub::pick, onClose = onClose)
}

/**
 * The picker's content for any link list: a capture's pending links or a saved record's links
 * ([links] are drawn checked; [recordText] feeds `글에 나온 이름`). The window takes the protection of
 * every project the list shows (LIB-26).
 */
@Composable
fun ColumnScope.LinkPickerBody(
    env: RecorderEnv,
    links: List<RecorderLink>,
    recordText: String?,
    onPick: (LinkCandidate) -> Unit,
    onClose: () -> Unit,
) {
    var field by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    val sections = remember(field.text, env, links, recordText) {
        LinkPickerRows.sections(env, field.text, links, recordText)
    }
    val shownProjects = remember(sections) { sections.flatMap { it.rows }.map { it.candidate.target.projectKey }.toSet() }
    ProtectWindow(CaptureProtection.of(env, shownProjects))
    SheetHeader(title = ShownCopy.PICKER_TITLE, onClose = onClose)
    SearchField(
        value = field,
        onValueChange = { field = it },
        modifier = Modifier.padding(top = 12.dp),
        placeholder = PickerCopy.SEARCH_PLACEHOLDER,
    )
    LazyColumn(Modifier.weight(1f, fill = false).padding(top = 8.dp)) {
        for ((index, section) in sections.withIndex()) {
            if (section.title != null) {
                item(key = "section-$index") { SectionLabel(section.title, civilightItem().padding(top = 12.dp, bottom = 4.dp)) }
            }
            items(section.rows, key = { "$index-${it.candidate.target}" }) { row ->
                ListRow(
                    title = row.title,
                    modifier = civilightItem(),
                    subtitle = row.sub.takeIf { it.isNotEmpty() },
                    compact = true,
                    onClick = if (row.linked) null else ({ onPick(row.candidate) }),
                    trailing = if (row.linked) ({ CivilightIcon(IconId.Check, contentDescription = null, size = 18.dp, tint = CivilightTheme.colors.acc) }) else null,
                )
            }
        }
    }
}
