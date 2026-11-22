package io.github.lenticualrstone.civilight.feature.library.rating

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CheckboxRow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.RatingBadge
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.library.rating.Protection
import io.github.lenticualrstone.civilight.domain.library.rating.RatingLabels
import io.github.lenticualrstone.civilight.domain.library.rating.RatingSheetExtra
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.feature.library.common.rememberProjectDoc
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.navigation.RatingChangeSheet
import io.github.lenticualrstone.civilight.navigation.RecoveryPassword
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.security.ProtectWindow
import io.github.lenticualrstone.civilight.ui.security.WindowProtection
import io.github.lenticualrstone.civilight.ui.theme.ThemeBridge
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/** The 3-cell rating picker over a project (서재 ⋯ › 수위 등급, 프로젝트 설정 › 수위 등급). */
@Serializable
data class RatingPickSheet(val projectKey: Key) : SheetKey

/** Title row: `<project> · 수위 등급` small text with `닫기` on the right. */
@Composable
private fun SmallHeader(text: String, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        CivilightText(text, CivilightTheme.type.small, Modifier.weight(1f), color = CivilightTheme.colors.ink3, maxLines = 1)
        TextAction(RatingCopy.CLOSE, onClose)
    }
}

/**
 * `수위 등급`: the same 3 cells as step 1 (LIB-09). Picking the current rating closes the picker with
 * no change; another opens the `바뀌는 것` sheet in its place (LIB-02, LIB-25).
 */
@Composable
internal fun SheetScope.RatingPickContent(projectKey: Key, current: Rating) {
    val scope = this
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        CivilightText(RatingCopy.RATING, type.title, Modifier.weight(1f), color = colors.ink, maxLines = 1)
        TextAction(RatingCopy.CLOSE, scope::close)
    }
    CivilightText(RatingLabels.menuSub(current), type.small, Modifier.padding(top = 2.dp), color = colors.ink3)
    RatingCells(
        selected = current,
        onSelect = { to ->
            scope.close()
            if (to != current) scope.open(RatingChangeSheet(projectKey, to))
        },
        modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
    )
}

/**
 * The shared `바뀌는 것` sheet (LIB-24, boards `RHRatingSheet`, `RHRatingDown`), registered for every
 * area. Built fresh from the project each time it opens and then kept as built, so the write it makes
 * does not rebuild it under the author; the checks live only while it is open (R19-07).
 * `바꾸기` runs [io.github.lenticualrstone.civilight.feature.library.protection.ProtectionActions.changeRating]:
 * a cancelled prompt keeps the sheet open with nothing changed. The sheet closes itself exactly once
 * ([RatingSheetModel.CloseOnce]), so it never closes another sheet below it. A hidden project's sheet
 * closes when the hidden group locks (R19 §3).
 */
@Composable
internal fun SheetScope.RatingChangeContent(sheet: RatingChangeSheet, services: LibraryServices) {
    val scope = this
    val deps = services.deps
    val doc by rememberProjectDoc(deps.projects, sheet.projectKey)
    val settings by deps.settings.collectAsState()
    val recorder by deps.stores.recorder.state.collectAsState()
    val unlock by deps.unlock.state.collectAsState()
    LaunchedEffect(Unit) { deps.stores.recorder.ensureLoaded() }
    // The 복구 암호 and Drive facts come from the backup book (batch 5): read it before building.
    var bookLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        services.backups.book.ensureLoaded()
        bookLoaded = true
    }
    val closer = remember { RatingSheetModel.CloseOnce(scope::close) }
    var working by remember { mutableStateOf(false) }
    val current = doc
    val ready = current != null && recorder.loaded && bookLoaded
    // Built once, when the project and the recorder library are first read.
    val built = remember(ready) {
        current?.takeIf { ready }?.let { RatingSheetModel.Built(RatingSheetModel.build(it, sheet.to, settings.ratingDefaults, services.facts, recorder.library)) }
    }
    val locked = current != null && RatingSheetModel.lockedAway(current.project.protections, unlock.hiddenGroupOpen)
    LaunchedEffect(locked, working, built) {
        if ((locked && !working) || (built != null && built.sheet == null)) closer.close()
    }
    val model = built?.sheet ?: return
    if (locked && !working) return
    var checkedNames by rememberSaveable(sheet) { mutableStateOf(model.initialChecks.map { it.name }.toSet()) }
    val checked = checkedNames.mapTo(HashSet()) { Protection.valueOf(it) }
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    current?.let { ProtectWindow(WindowProtection.forProject(it.project.protections)) }

    SmallHeader(model.header, closer::close)
    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RatingBadge(ThemeBridge.rating(model.from))
        CivilightText(RatingCopy.ARROW, type.number, color = colors.ink3)
        RatingBadge(ThemeBridge.rating(model.to))
        CivilightText(model.titleSuffix, type.title, color = colors.ink, maxLines = 1)
    }
    CivilightText(model.smallLine, type.small, Modifier.padding(top = 4.dp), color = colors.ink3)
    SheetScrollBody(Modifier.padding(top = 8.dp)) {
        model.rows.forEach { row ->
            CheckboxRow(
                label = row.title,
                checked = row.protection in checked,
                onCheckedChange = { checkedNames = RatingSheetModel.toggle(checked, row.protection).mapTo(HashSet()) { it.name } },
                subtitle = row.sub,
                note = row.rightLabel,
            )
        }
        model.extraRows(checked).forEach { extra ->
            Row(
                Modifier.fillMaxWidth().topHairline(colors.line).heightIn(min = 44.dp).padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CivilightText("◇", type.command, Modifier.width(22.dp), color = colors.ink2)
                Column(Modifier.weight(1f)) {
                    CivilightText(extra.title, type.command, color = colors.ink)
                    CivilightText(extra.sub, type.small, color = colors.ink3)
                }
                when (extra.kind) {
                    RatingSheetExtra.NO_RECOVERY_PASSWORD ->
                        TextAction(extra.action, onClick = { deps.navigator.push(RecoveryPassword) }, style = type.small)
                    // LIB-57 (batch 5): the old unencrypted Drive versions of this project go.
                    RatingSheetExtra.UNENCRYPTED_BACKUPS ->
                        TextAction(extra.action, onClick = { deps.appScope.launch { services.backups.deleteUnencrypted(sheet.projectKey) } }, style = type.small)
                }
            }
        }
        model.note(checked)?.let { CivilightText(it, type.small, Modifier.padding(top = 10.dp), color = colors.ink3) }
    }
    SheetActions {
        CivilightButton(RatingCopy.CANCEL, closer::close, Modifier.weight(1f), variant = ButtonVariant.GHOST)
        CivilightButton(
            model.buttons.last(),
            onClick = {
                working = true
                deps.appScope.launch {
                    val outcome = services.protection.changeRating(sheet.projectKey, model.apply(checked))
                    services.report(outcome)
                    working = false
                    if (RatingSheetModel.closesAfter(outcome)) closer.close()
                }
            },
            modifier = Modifier.weight(1f),
            enabled = !working,
        )
    }
}
