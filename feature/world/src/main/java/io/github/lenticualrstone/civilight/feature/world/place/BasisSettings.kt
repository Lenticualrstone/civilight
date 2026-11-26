package io.github.lenticualrstone.civilight.feature.world.place

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.RadioRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SettingsRow
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.FoldRevealValue
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import io.github.lenticualrstone.civilight.domain.actions.AffiliationActions
import io.github.lenticualrstone.civilight.domain.world.item.AffiliationSettings
import io.github.lenticualrstone.civilight.domain.world.item.BasisResult
import io.github.lenticualrstone.civilight.domain.world.item.BasisSlot
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.common.WorldEnv
import io.github.lenticualrstone.civilight.feature.world.common.WorldViewModel
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.AffiliationBasisSettings
import io.github.lenticualrstone.civilight.navigation.FieldSettings
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.CenterColumn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/** One `묶음 기준` row: `소속 · 소속` / `거주 · 고르기`; [note] = why a stored field is not used now (`휴지통`). */
data class BasisRowUi(val slot: BasisSlot, val title: String, val value: String, val picked: Boolean, val note: String?)

/** A picker row: a character `연결` field of the slot's target. */
data class BasisChoiceUi(val key: Key, val name: String, val selected: Boolean)

/**
 * (2026-11-26) 프로젝트 설정 › `묶음 기준` (board `ImmPlaceRules`): three separate slots `소속 · 거주 · 국적`, each a
 * character `연결` field the user picks (소속 → 세력, 거주 · 국적 → 장소). Nothing is guessed; an unpicked slot is
 * not used. A stored field that can no longer be used (trashed, type changed) shows one terse line.
 */
object BasisSettingsState {
    fun rows(doc: ProjectDoc): List<BasisRowUi> = BasisSlot.entries.map { slot ->
        val stored = AffiliationSettings.storedKey(doc.project.cast.affiliationBasis, slot)
        val usable = AffiliationSettings.usableDef(doc, slot)
        val note = if (stored != null && usable == null) {
            AffiliationSettings.refusal(doc.index.fieldDef(stored), slot)?.let { PlaceCopy.refusal(it, slot) }
        } else {
            null
        }
        BasisRowUi(
            slot = slot,
            title = PlaceCopy.slot(slot),
            value = usable?.name ?: PlaceCopy.BASIS_PICK,
            picked = usable != null,
            note = note,
        )
    }

    fun choices(doc: ProjectDoc, slot: BasisSlot): List<BasisChoiceUi> {
        val current = AffiliationSettings.usableDef(doc, slot)?.key
        return AffiliationSettings.choices(doc, slot).map { BasisChoiceUi(it.key, it.name, it.key == current) }
    }

    /** `소속 소속 · 거주 고르기 · 국적 국적` for the 프로젝트 설정 row (the slot word, then the field or `고르기`). */
    fun summary(doc: ProjectDoc): String = rows(doc).joinToString(PlaceCopy.SEP) { "${it.title} ${it.value}" }
}

/** The slot picker (feature-local). */
@Serializable
data class BasisPickSheet(val slot: BasisSlot) : SheetKey

class BasisSettingsViewModel(projectKey: Key, env: WorldEnv) : WorldViewModel(projectKey, env) {
    val rows: StateFlow<List<BasisRowUi>?> = doc
        .map { d -> d?.let(BasisSettingsState::rows) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Sets or clears [slot]; returns the refusal line, or null when applied. */
    suspend fun set(slot: BasisSlot, fieldKey: Key?): String? {
        val ctx = actionContext()
        var refused: String? = null
        val r = apply(AffiliationActions.UNDO_BASIS) { d ->
            when (val res = AffiliationActions.setBasis(d, slot, fieldKey, ctx)) {
                is BasisResult.Done -> res.doc
                is BasisResult.Refused -> d.also { refused = PlaceCopy.refusal(res.reason, slot) }
            }
        }
        return refused ?: if (r == EditResult.READ_ONLY) WorldCopy.READ_ONLY else null
    }
}

/** 프로젝트 설정 › 묶음 기준: three rows, each opening its picker. */
@Composable
fun BasisSettingsScreen(route: AffiliationBasisSettings, deps: FeatureDeps) {
    val vm = entryViewModel { BasisSettingsViewModel(route.projectKey, WorldEnv.of(deps)) }
    val rows by vm.rows.collectAsState()
    val doc by vm.doc.collectAsState()
    val readOnly by vm.readOnly.collectAsState()
    val sheets = rememberSheetHost()
    CivilightScreen(
        title = PlaceCopy.BASIS_TITLE,
        smallLine = WorldCopy.PROJECT_SETTINGS,
        back = IconAction(WorldCopy.BACK) { deps.navigator.pop() },
        sheets = sheets,
        localSheets = { key ->
            when (key) {
                is BasisPickSheet -> doc?.let { d -> { BasisPickContent(key.slot, d, readOnly, vm) { deps.navigator.push(FieldSettings(route.projectKey)) } } }
                else -> null
            }
        },
    ) { padding ->
        CenterColumn {
            val shown = rows ?: return@CenterColumn
            val colors = CivilightTheme.colors
            val type = CivilightTheme.type
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp),
            ) {
                item(key = "title") {
                    SectionLabel(PlaceCopy.BASIS_TITLE, civilightItem().padding(top = 12.dp, bottom = 2.dp))
                }
                item(key = "note") {
                    CivilightText(PlaceCopy.BASIS_NOTE, type.small, civilightItem().padding(bottom = 8.dp), color = colors.ink3)
                }
                items(shown, key = { it.slot.name }) { row ->
                    SettingsRow(
                        title = row.title,
                        value = row.value,
                        subtitle = row.note,
                        onClick = { sheets.open(BasisPickSheet(row.slot)) },
                        modifier = civilightItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetScope.BasisPickContent(slot: BasisSlot, doc: ProjectDoc, readOnly: Boolean, vm: BasisSettingsViewModel, onFields: () -> Unit) {
    val choices = remember(doc, slot) { BasisSettingsState.choices(doc, slot) }
    val current = AffiliationSettings.storedKey(doc.project.cast.affiliationBasis, slot)
    var refusal by rememberSaveable(slot) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val sheet = this
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    SheetHeader(title = PlaceCopy.slot(slot), onClose = ::close)
    SheetScrollBody {
        CivilightText(PlaceCopy.slotTarget(slot), type.small, Modifier.padding(top = 4.dp, bottom = 6.dp), color = colors.ink3)
        if (choices.isEmpty()) {
            CivilightText(PlaceCopy.noChoices(slot), type.small, Modifier.padding(top = 8.dp), color = colors.ink3)
            TextAction(PlaceCopy.FIELD_SETTINGS, { sheet.close(); onFields() }, color = colors.acc)
        }
        choices.forEach { choice ->
            RadioRow(
                title = choice.name,
                selected = choice.selected,
                enabled = !readOnly,
                onClick = {
                    scope.launch {
                        val r = vm.set(slot, choice.key)
                        refusal = r
                        if (r == null) sheet.close()
                    }
                },
            )
        }
        FoldRevealValue(refusal) { line -> CivilightText(line, type.small, Modifier.padding(top = 8.dp), color = colors.warn) }
    }
    if (current != null && !readOnly) {
        SheetActions {
            CivilightButton(
                text = PlaceCopy.BTN_CLEAR,
                onClick = { scope.launch { if (vm.set(slot, null) == null) sheet.close() } },
                modifier = Modifier.weight(1f),
                variant = ButtonVariant.GHOST,
            )
        }
    }
}
