package io.github.lenticualrstone.civilight.feature.map.text

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import io.github.lenticualrstone.civilight.feature.map.common.appear
import io.github.lenticualrstone.civilight.feature.map.common.MapMotion
import io.github.lenticualrstone.civilight.designsystem.theme.rememberLastNonNull
import io.github.lenticualrstone.civilight.designsystem.theme.animatedFraction
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import androidx.compose.runtime.key
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonRow
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.ConditionResult
import io.github.lenticualrstone.civilight.designsystem.component.ConditionRow
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.ReadOnlyBanner
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.world.gen.ConditionKind
import io.github.lenticualrstone.civilight.domain.world.gen.GeneratedWorld
import io.github.lenticualrstone.civilight.domain.world.gen.TextConditions
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.canvas.MapBase
import io.github.lenticualrstone.civilight.feature.map.canvas.MapCanvas
import io.github.lenticualrstone.civilight.feature.map.canvas.MapScene
import io.github.lenticualrstone.civilight.feature.map.canvas.Viewport
import io.github.lenticualrstone.civilight.feature.map.common.CardHeader
import io.github.lenticualrstone.civilight.feature.map.common.CardRow
import io.github.lenticualrstone.civilight.feature.map.common.GlassCard
import io.github.lenticualrstone.civilight.feature.map.common.MapEnv
import io.github.lenticualrstone.civilight.feature.map.common.MapNav
import io.github.lenticualrstone.civilight.feature.map.common.TopBarTapGuard
import io.github.lenticualrstone.civilight.feature.map.common.mapBack
import io.github.lenticualrstone.civilight.feature.map.gen.nextSeed
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.navigation.TextMap as TextMapRoute
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.picker.PlacePickerSheet
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The condition editor `조건`: a new condition or [key]'s (the draft lives in the screen's state). */
@Serializable
data class CondEditSheet(val key: Key?) : SheetKey

/** `연결 장소` of the condition being edited. */
@Serializable
data object CondPlaceSheet : SheetKey

/** `위치` / `위치 고르기` of a condition. */
@Serializable
data class CondPositionSheet(val key: Key) : SheetKey

private val DRAFT_JSON = Json { ignoreUnknownKeys = true }

/**
 * 글로 설명 (WLD-61, boards `MapText`, `MapTextResult`, `MMapText`). Input: the text, `함께 읽기`
 * chips, `읽어 낸 조건 · N` (read on device only), the condition rows (`⋯` edits), the hint and
 * `조건 · 이 조건으로 만들기`. Result: the map, `조건 4 · 3 반영 · 닫기`, rows with `위치` or
 * `고치기`, `다른 후보 3 · 이 지도로 시작`; a tapped row highlights its area. Inner screens show the
 * same in a 560 dp column / card (`[PROPOSED]`).
 */
@Composable
fun TextMapScreen(route: TextMapRoute, deps: FeatureDeps) {
    val vm = entryViewModel { TextMapViewModel(route.projectKey, MapEnv.of(deps)) }
    val doc by vm.doc.collectAsState()
    val readOnly by vm.readOnly.collectAsState()
    val sheets = rememberSheetHost()
    val gate = rememberHiddenGate(route.projectKey)
    val inner = LocalWindowLayout.current.isInner
    var stateText by rememberSaveable { mutableStateOf<String?>(null) }
    var draftText by rememberSaveable { mutableStateOf<String?>(null) }
    var positionDraft by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) }
    var viewportArray by rememberSaveable { mutableStateOf(Viewport.UNSET.toArray()) }
    val d = doc
    // First arrival: an existing map's stored conditions (TextMap(mapKey)), else an empty input.
    LaunchedEffect(d != null) {
        if (stateText == null && d != null) {
            val stored = route.mapKey?.let { d.maps[it]?.generation?.conditions }.orEmpty()
            stateText = TextMapState(conditions = stored.map { CondState.of(it, fromText = false) }).encode()
        }
    }
    val state = remember(stateText) { TextMapState.decode(stateText) ?: TextMapState() }
    fun set(s: TextMapState) { stateText = s.encode() }
    val draft = remember(draftText) { draftText?.let { runCatching { DRAFT_JSON.decodeFromString(CondState.serializer(), it) }.getOrNull() } }
    fun setDraft(c: CondState?) { draftText = c?.let { DRAFT_JSON.encodeToString(CondState.serializer(), it) } }

    // Reading follows the text (0.5 s after the last change), the engine and the sources.
    LaunchedEffect(state.text, state.engine, state.offSources) {
        if (TextMapFlow.isRead(state)) return@LaunchedEffect
        delay(500)
        val current = vm.doc.value ?: return@LaunchedEffect
        set(TextMapFlow.read(current, TextMapState.decode(stateText) ?: state, vm.editContext()))
    }
    val session = remember(stateText) { TextMapFlow.session(state) }
    val world by produceState<GeneratedWorld?>(null, stateText, state.page) {
        value = if (state.page == 1 && state.seeds.isNotEmpty()) vm.world(session) else null
    }

    CivilightScreen(
        title = if (state.page == 1) MapCopy.defaultMapName((d?.maps?.size ?: 0) + 1) else MapCopy.TEXT_TITLE,
        smallLine = if (state.page == 1) MapCopy.TEXT_RESULT_TITLE else MapCopy.TEXT_SMALL,
        back = mapBack(deps),
        banner = if (readOnly) ({ ReadOnlyBanner(MapCopy.READ_ONLY) }) else null,
        sheets = sheets,
        localSheets = { key ->
            val doc0 = d
            if (doc0 == null) null else when (key) {
                is CondEditSheet -> {
                    {
                        val fallbackKey = remember { key.key ?: Keys.newKey() }
                        val c = draft ?: CondState(key = fallbackKey, what = "")
                        CondEditor(doc0, c, editing = key.key != null, onChange = { setDraft(it) }, onPlace = { open(CondPlaceSheet) },
                            onDone = { done -> close(); if (done.what.isNotBlank()) set(TextMapFlow.upsert(state, done)); setDraft(null) },
                            onRemove = { close(); set(TextMapFlow.remove(state, c.key)); setDraft(null) },
                            onClose = { close(); setDraft(null) })
                    }
                }
                CondPlaceSheet -> {
                    {
                        PlacePickerSheet(doc0, gate, MapCopy.COND_PLACE, onPick = { ref -> draft?.let { setDraft(it.copy(placeKey = ref.key)) }; close() }, onClose = ::close, includeRegions = false)
                    }
                }
                is CondPositionSheet -> {
                    {
                        val sheet = this
                        SheetHeader(MapCopy.PICK_POSITION, onClose = ::close)
                        SheetScrollBody { CellGrid(positionDraft.toSet()) { cell -> positionDraft = if (cell in positionDraft) positionDraft - cell else positionDraft + cell } }
                        SheetActions { CivilightButton(MapCopy.BTN_DONE, { sheet.close(); set(TextMapFlow.setPosition(state, key.key, positionDraft.toSet())) }, Modifier.weight(1f)) }
                    }
                }
                else -> null
            }
        },
    ) { padding ->
        val doc0 = d ?: return@CivilightScreen
        // The input page and the result page swap with a crossfade (motion audit row 12).
        ContentSwap(state.page, Modifier.fillMaxSize()) { page ->
        if (page == 0) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 16.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(Modifier.widthIn(max = 560.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    InputPage(doc0, state, readOnly, ::set,
                        onEdit = { c -> setDraft(c); sheets.open(CondEditSheet(c.key)) },
                        onAdd = { setDraft(CondState(key = Keys.newKey(), what = "")); sheets.open(CondEditSheet(null)) },
                        onMake = { set(TextMapFlow.make(state, ::nextSeed)) })
                }
            }
        } else {
            val motion = CivilightTheme.motion
            Box(Modifier.fillMaxSize()) {
                val w = world
                val shownWorld = rememberLastNonNull(w)
                val area = state.highlight?.let { TextConditions.area(TextMapFlow.conditions(state)[it]) }.orEmpty()
                // The highlight fades in and out; the last area stays drawn while it fades.
                val areaShown = rememberLastNonNull(area.takeIf { it.isNotEmpty() }).orEmpty()
                val highlight by animatedFraction(area.isNotEmpty(), label = "condHighlight")
                val acc = CivilightTheme.colors.acc
                // The world appears when it is ready and leaves with the old one on a new reading.
                AnimatedVisibility(w != null, enter = motion.enter(CivilightMotion.enter), exit = motion.exit(CivilightMotion.exit), label = "textWorld") {
                    shownWorld?.let { shown ->
                        MapCanvas(
                            base = MapBase.Generated(shown, null),
                            scene = MapScene(),
                            viewport = Viewport.fromArray(viewportArray),
                            onViewportChange = { viewportArray = it.toArray() },
                            extraDraw = { scope ->
                                // The highlighted condition's 3 × 3 cells.
                                areaShown.forEach { cell ->
                                    val a = scope.screen(io.github.lenticualrstone.civilight.model.Point((cell % 3) / 3f, (cell / 3) / 3f))
                                    val b = scope.screen(io.github.lenticualrstone.civilight.model.Point((cell % 3 + 1) / 3f, (cell / 3 + 1) / 3f))
                                    drawRect(acc.copy(alpha = 0.22f * highlight), Offset(a.x, a.y), Size(b.x - a.x, b.y - a.y))
                                }
                            },
                        )
                    }
                }
                TopBarTapGuard(padding.calculateTopPadding())
                val cardMod = if (inner) Modifier.align(Alignment.TopEnd).padding(top = padding.calculateTopPadding() + 8.dp, end = 12.dp).widthIn(max = 380.dp)
                else Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = padding.calculateBottomPadding() + 12.dp)
                GlassCard(cardMod.heightIn(max = 460.dp).animateContentSize(motion.move(CivilightMotion.standard))) {
                    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ResultCard(doc0, state, w, readOnly, ::set,
                            onPosition = { c -> positionDraft = (c.userPosition ?: c.where); sheets.open(CondPositionSheet(c.key)) },
                            onStart = { vm.start(session) { key -> deps.navigator.replace(MapNav.routeOf(vm.doc.value, route.projectKey, key)) } })
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun ColumnScope.InputPage(
    doc: io.github.lenticualrstone.civilight.model.doc.ProjectDoc,
    state: TextMapState,
    readOnly: Boolean,
    set: (TextMapState) -> Unit,
    onEdit: (CondState) -> Unit,
    onAdd: () -> Unit,
    onMake: () -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    CivilightTextField(state.text, { set(state.copy(text = it)) }, Modifier.fillMaxWidth().heightIn(min = 120.dp), singleLine = false)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CivilightText(MapCopy.READ_ALONG, type.label, color = colors.ink3)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TextMapFlow.sources(doc, state).forEach { chip -> FilterChip(chip.label, chip.on, { set(TextMapFlow.toggleSource(state, chip.id)) }) }
        }
    }
    // (0.3.0) On device only: the keyword reader runs here and nothing leaves the device, so there is no
    // `기기 안 · 클라우드` choice (the cloud reader reads nothing).
    if (state.unreadable) CivilightText(MapCopy.TEXT_UNREADABLE, type.small, color = colors.warn)
    CivilightText(TextMapFlow.readLine(state), type.sec, color = colors.ink3)
    // A row that is read from the text appears on its own, one after another (`rowIn`, D1).
    TextMapFlow.inputRows(doc, state).forEach { row ->
        key(row.index) {
            ConditionRow(
                row.name, Modifier.appear(delayMs = MapMotion.stagger(row.index), token = CivilightMotion.rowIn),
                detail = row.small, coveredCells = row.cells, more = IconAction(MapCopy.CONDITION) { onEdit(state.conditions[row.index]) },
            )
        }
    }
    CivilightText(TextConditions.HINT, type.small, color = colors.ink3)
    ButtonRow(Modifier.padding(top = 6.dp)) {
        CivilightButton(MapCopy.BTN_CONDITION, onAdd, Modifier.weight(1f), variant = ButtonVariant.GHOST)
        CivilightButton(MapCopy.BTN_MAKE_FROM_CONDITIONS, onMake, Modifier.weight(1f), enabled = state.conditions.isNotEmpty() && !readOnly)
    }
}

@Composable
private fun ColumnScope.ResultCard(
    doc: io.github.lenticualrstone.civilight.model.doc.ProjectDoc,
    state: TextMapState,
    world: GeneratedWorld?,
    readOnly: Boolean,
    set: (TextMapState) -> Unit,
    onPosition: (CondState) -> Unit,
    onStart: () -> Unit,
) {
    CardHeader(world?.let { TextMapFlow.header(state, it) } ?: MapCopy.TEXT_RESULT_TITLE, { set(state.copy(page = 0, highlight = null)) })
    if (world != null) {
        TextMapFlow.resultRows(doc, state, world).forEach { row ->
            key(row.index) {
            Row(Modifier.appear(delayMs = MapMotion.stagger(row.index), token = CivilightMotion.rowIn), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).clickable { set(TextMapFlow.highlight(state, row.index)) }) {
                    ConditionRow(row.name, detail = row.small, result = if (row.applied == true) ConditionResult.APPLIED else ConditionResult.NEEDS_CHECK)
                }
                CivilightButton(
                    if (row.applied == true) MapCopy.BTN_POSITION else MapCopy.BTN_FIX,
                    { onPosition(state.conditions[row.index]) },
                    variant = ButtonVariant.COMPACT_GHOST,
                )
            }
            }
        }
    }
    if (state.candidates.isNotEmpty()) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.candidates.forEachIndexed { i, seeds ->
                FilterChip(MapCopy.seed(io.github.lenticualrstone.civilight.domain.world.gen.StepChoices.seedLabel(seeds.first())), seeds == state.seeds, { set(TextMapFlow.chooseCandidate(state, i)) })
            }
        }
    }
    ButtonRow(Modifier.padding(top = 6.dp)) {
        CivilightButton(MapCopy.otherCandidates(3), { set(TextMapFlow.otherCandidates(state, ::nextSeed)) }, Modifier.weight(1f), variant = ButtonVariant.GHOST)
        CivilightButton(MapCopy.BTN_START_MAP, onStart, Modifier.weight(1f), enabled = world != null && !readOnly)
    }
}

/** The condition editor: 무엇 (field and kind chips), 어디 (3 × 3 cells), 특징, 연결 장소. */
@Composable
private fun ColumnScope.CondEditor(
    doc: io.github.lenticualrstone.civilight.model.doc.ProjectDoc,
    c: CondState,
    editing: Boolean,
    onChange: (CondState) -> Unit,
    onPlace: () -> Unit,
    onDone: (CondState) -> Unit,
    onRemove: () -> Unit,
    onClose: () -> Unit,
) {
    val type = CivilightTheme.type
    // 특징 is typed freely and split into traits at `완료` (`눈 덮임 · 메마름`).
    var traitsText by rememberSaveable { mutableStateOf(c.traits.joinToString(MapCopy.SEP)) }
    SheetHeader(MapCopy.CONDITION, onClose = onClose)
    SheetScrollBody {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CivilightTextField(c.what, { onChange(c.copy(what = it)) }, Modifier.padding(top = 8.dp), label = MapCopy.COND_WHAT)
            ChipFlow { ConditionKind.entries.forEach { k -> val w = k.words.first(); FilterChip(w, c.what == w, { onChange(c.copy(what = w)) }) } }
            CivilightText(MapCopy.COND_WHERE, type.label, color = CivilightTheme.colors.ink3)
            CellGrid(c.where.toSet()) { cell -> onChange(c.copy(where = if (cell in c.where) c.where - cell else (c.where + cell).sorted())) }
            CivilightTextField(traitsText, { traitsText = it }, label = MapCopy.COND_TRAITS)
            CardRow(MapCopy.COND_PLACE, c.placeKey?.let { doc.index.place(it)?.name } ?: MapCopy.NONE, onClick = onPlace)
        }
    }
    SheetActions {
        if (editing) CivilightButton(MapCopy.BTN_REMOVE, onRemove, Modifier.weight(1f), variant = ButtonVariant.GHOST)
        CivilightButton(MapCopy.BTN_DONE, { onDone(c.copy(traits = TextMapFlow.traits(traitsText))) }, Modifier.weight(1f), enabled = c.what.isNotBlank())
    }
}

/** A 3 × 3 grid of 44 dp cells (0–8, row-major; north on top), each a visible toggle. */
@Composable
private fun CellGrid(selected: Set<Int>, onToggle: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { r ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { col ->
                    val cell = r * 3 + col
                    FilterChip((cell + 1).toString(), cell in selected, { onToggle(cell) }, Modifier.size(52.dp, 44.dp))
                }
            }
        }
    }
}
