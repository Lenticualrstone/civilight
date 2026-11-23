package io.github.lenticualrstone.civilight.feature.library.openfile

import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonSpec
import kotlinx.coroutines.delay
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.KeyValueLine
import io.github.lenticualrstone.civilight.designsystem.component.PasswordField
import io.github.lenticualrstone.civilight.designsystem.component.RadioRow
import io.github.lenticualrstone.civilight.designsystem.component.RatingBadge
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.library.crypto.EncryptedFileSheet
import io.github.lenticualrstone.civilight.domain.library.format.NewerFileSheet
import io.github.lenticualrstone.civilight.domain.library.importing.OpenFileSheet
import io.github.lenticualrstone.civilight.domain.library.importing.SameProjectChoice
import io.github.lenticualrstone.civilight.domain.library.rating.RatingLabels
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.feature.library.common.sheetBody
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.navigation.OpenFile
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.security.ProtectWindow
import io.github.lenticualrstone.civilight.ui.security.WindowProtection
import io.github.lenticualrstone.civilight.ui.theme.ThemeBridge
import kotlinx.serialization.Serializable

/** The one local sheet of [OpenFileScreen]; it holds no state (the view model does). */
@Serializable
data class OpenFileSheetKey(val uri: String) : SheetKey

/**
 * 파일에서 열기 (LIB-52 … LIB-54). `OpenFile` is a plain route, so the flow is a sheet over an empty
 * `bg` frame (`ED`; boards draw it over the system file app): bottom sheet on the cover screen,
 * centred sheet inside. Closing the sheet leaves the route.
 */
@Composable
internal fun OpenFileScreen(route: OpenFile, services: LibraryServices) {
    val context = LocalContext.current.applicationContext
    val vm = entryViewModel { OpenFileViewModel(route.uri, services, context) }
    val finished by vm.finished.collectAsState()
    val sheets = rememberSheetHost()
    var opened by rememberSaveable { mutableStateOf(false) }
    val navigator = services.deps.navigator

    val motion = CivilightTheme.motion
    // The sheet opens once the shell's push has settled (audit row 13), not over a screen still sliding in.
    LaunchedEffect(Unit) {
        if (!opened) {
            delay(motion.delayMs(if (motion.reduced) CivilightMotion.reducedFadeMs else CivilightMotion.enter.durationMs))
            sheets.open(OpenFileSheetKey(route.uri))
            opened = true
        }
    }
    // The sheet was closed (닫기, 취소, a drag) or the flow ended: leave the route once, after the sheet's
    // exit has played (the pop would otherwise cut it).
    LaunchedEffect(finished, sheets.sheets, opened) {
        if (finished) {
            delay(motion.delayMs(if (motion.reduced) CivilightMotion.reducedFadeMs else CivilightMotion.exit.durationMs))
            if (navigator.current == route) navigator.pop()
        } else if (opened && sheets.sheets.isEmpty()) {
            vm.close()
        }
    }

    CivilightScreen(
        title = OpenFileCopy.TITLE,
        smallLine = OpenFileCopy.SMALL,
        sheets = sheets,
        localSheets = { key ->
            when (key) {
                is OpenFileSheetKey -> sheetBody { OpenFileSheetContent(vm) }
                else -> null
            }
        },
    ) { }
}

@Composable
private fun SheetScope.OpenFileSheetContent(vm: OpenFileViewModel) {
    val stage by vm.stage.collectAsState()
    val finished by vm.finished.collectAsState()
    val scope = this
    LaunchedEffect(finished) { if (finished) scope.close() }
    val onClose = { vm.close() }
    SheetHead(onClose)
    // Reading → password → ask → failed: the stage body swaps in place (audit row 5, P3); the sheet's own
    // height follows. A stage that only changes its data (busy, an error line) keeps its body.
    ContentSwap(stage, Modifier.fillMaxWidth(), contentKey = { it::class }, label = "openFileStage") { s ->
        Column {
            when (s) {
                OpenStage.Reading -> Unit
                is OpenStage.Failed -> FailedBody(s.message, onClose)
                is OpenStage.Password -> PasswordBody(s, onClose, vm::unlock)
                is OpenStage.Newer -> NewerBody(s.sheet, s.summary, onClose)
                is OpenStage.Ask -> AskBody(s, onClose, vm::choose, vm::import)
                is OpenStage.Recorder -> RecorderBody(s, onClose, vm::importRecorder)
            }
        }
    }
}

/** `Civilight · 파일에서 열기` (small) and `닫기`. */
@Composable
private fun SheetHead(onClose: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        CivilightText(OpenFileSheet.HEADER, type.small, Modifier.weight(1f), color = colors.ink3, maxLines = 1)
        // DS-70 A6: the sheet head `닫기` is the `close` icon with its name.
        CivilightIconButton(IconButtonSpec.CLOSE, onClose)
    }
}

@Composable
private fun TitleAndSummary(title: String, rating: Rating?, summary: String) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    CivilightText(title, type.emptyTitle, Modifier.padding(top = 6.dp), color = colors.ink, maxLines = 2)
    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        // (0.2.0, LIB-92, S-4) No rating badge anywhere; the file's stored rating is kept as it is.
        CivilightText(summary, type.dir, color = colors.ink3, maxLines = 1)
    }
}

@Composable
private fun AskBody(s: OpenStage.Ask, onClose: () -> Unit, onChoose: (SameProjectChoice) -> Unit, onOpen: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    // The file's name and counts show outside the project's own routes (LIB-26, LIB-29).
    ProtectWindow(WindowProtection.forProject(s.protections))
    TitleAndSummary(s.sheet.title, RatingLabels.effective(s.pkg.doc.project.rating), s.sheet.summary)
    s.sheet.failedNote?.let { CivilightText(it, type.small, Modifier.padding(top = 8.dp), color = colors.warn) }
    s.sheet.sameProject?.let { block ->
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CivilightText("◇", type.label, color = colors.ink2)
            Column(Modifier.weight(1f)) {
                CivilightText(block.heading, type.itemTitleCompact, color = colors.ink)
                CivilightText(block.existingSub, type.small, color = colors.ink3)
            }
        }
        Column(Modifier.padding(top = 10.dp)) {
            listOf(SameProjectChoice.AS_NEW_PROJECT, SameProjectChoice.OVERWRITE).forEach { choice ->
                RadioRow(
                    title = block.choices.title(choice),
                    selected = s.choice == choice,
                    onClick = { onChoose(choice) },
                    subtitle = block.choices.sub(choice),
                    enabled = !s.busy && (choice != SameProjectChoice.OVERWRITE || s.overwriteAllowed),
                )
            }
        }
    }
    SheetActions(Modifier.padding(bottom = 12.dp)) {
        CivilightButton(OpenFileSheet.CANCEL, onClose, Modifier.weight(1f), variant = ButtonVariant.GHOST)
        CivilightButton(OpenFileSheet.OPEN, onOpen, Modifier.weight(1f), enabled = !s.busy)
    }
}

/** `.civilight-recorder` (LIB-48 `ED`, undrawn): `라이프 레코더` · `기록 <n>`, `취소` · `열기`. */
@Composable
private fun RecorderBody(s: OpenStage.Recorder, onClose: () -> Unit, onOpen: () -> Unit) {
    TitleAndSummary(OpenFileCopy.RECORDER_TITLE, null, "${OpenFileCopy.RECORDS} ${s.records}")
    SheetActions(Modifier.padding(bottom = 12.dp)) {
        CivilightButton(OpenFileSheet.CANCEL, onClose, Modifier.weight(1f), variant = ButtonVariant.GHOST)
        CivilightButton(OpenFileSheet.OPEN, onOpen, Modifier.weight(1f), enabled = !s.busy)
    }
}

@Composable
private fun NewerBody(sheet: NewerFileSheet, summary: String, onClose: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val context = LocalContext.current
    TitleAndSummary(sheet.title, sheet.ratingBadge?.let(::ratingOfBadge), summary)
    CivilightText(sheet.heading, type.itemTitleCompact, Modifier.padding(top = 16.dp, bottom = 6.dp), color = colors.ink)
    sheet.rows.forEach { (label, value) -> KeyValueLine(label, value) }
    CivilightText(sheet.note, type.small, Modifier.padding(top = 10.dp), color = colors.ink3)
    SheetActions(Modifier.padding(bottom = 12.dp)) {
        CivilightButton(NewerFileSheet.CLOSE, onClose, Modifier.weight(1f), variant = ButtonVariant.GHOST)
        CivilightButton(NewerFileSheet.UPDATE, { openStoreListing(context) }, Modifier.weight(1f))
    }
}

@Composable
private fun PasswordBody(s: OpenStage.Password, onClose: () -> Unit, onOpen: (String) -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    // Never saved: the password lives only while this sheet is composed.
    var password by remember { mutableStateOf("") }
    Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(colors.tint), contentAlignment = Alignment.Center) {
            CivilightIcon(IconId.Lock, contentDescription = null, tint = colors.ink2)
        }
        Column(Modifier.weight(1f)) {
            CivilightText(EncryptedFileSheet.TITLE, type.itemTitleCompact, color = colors.ink)
            CivilightText(s.sheet.fileName, type.dir, color = colors.ink3, maxLines = 1)
        }
    }
    CivilightText(s.sheet.note, type.small, Modifier.padding(top = 10.dp), color = colors.ink3)
    PasswordField(
        value = password,
        onValueChange = { password = it },
        modifier = Modifier.padding(top = 14.dp),
        label = s.sheet.field.label,
        hint = s.sheet.field.hint,
        error = s.error,
    )
    CivilightText(s.sheet.afterNote, type.small, Modifier.padding(top = 10.dp), color = colors.ink3)
    SheetActions(Modifier.padding(bottom = 12.dp)) {
        CivilightButton(EncryptedFileSheet.CANCEL, onClose, Modifier.weight(1f), variant = ButtonVariant.GHOST)
        CivilightButton(EncryptedFileSheet.OPEN, { onOpen(password) }, Modifier.weight(1f), enabled = !s.busy && password.isNotEmpty())
    }
}

@Composable
private fun FailedBody(message: String, onClose: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    CivilightText(message, type.label, Modifier.padding(top = 12.dp), color = colors.ink)
    SheetActions(Modifier.padding(bottom = 12.dp)) {
        CivilightButton(OpenFileSheet.CLOSE, onClose, Modifier.weight(1f), variant = ButtonVariant.GHOST)
    }
}

private fun ratingOfBadge(badge: String): Rating? = Rating.entries.firstOrNull { RatingLabels.badge(it) == badge }

/** `Civilight 업데이트` (`ED`): the store listing of this app, else its web page. */
private fun openStoreListing(context: Context) {
    val id = context.packageName
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(market)
    } catch (e: ActivityNotFoundException) {
        val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$id")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(web) }
    }
}
