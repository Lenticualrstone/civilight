package io.github.lenticualrstone.civilight.feature.story.bulk

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.OutlinedSegmented
import io.github.lenticualrstone.civilight.designsystem.component.SegmentItem
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.designsystem.theme.ValueSwap
import io.github.lenticualrstone.civilight.domain.story.bulk.BulkPlace
import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.feature.story.common.MiniMapView
import io.github.lenticualrstone.civilight.feature.story.common.MiniMaps
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.navigation.PickOnMap
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore
import kotlinx.serialization.Serializable

/** `장소 정하기 · 씬 4개` over 장소 · 때 한꺼번에 (STO-32); local to that screen, saved with it. */
@Serializable
data class BulkPlaceSheet(val projectKey: Key, val sceneKeys: List<Key>, val chapterKey: Key? = null) : SheetKey

/** A place choice saved as `PLACE:<key>` / `REGION:<key>`. */
internal object PlaceCodec {
    fun encode(ref: PlaceRef): String = "${ref.kind.known ?: PlaceRefKind.PLACE}:${ref.key}"

    fun decode(s: String?): PlaceRef? {
        val (kind, key) = s?.split(":", limit = 2)?.takeIf { it.size == 2 } ?: return null
        return PlaceRef(PlaceRefKind.entries.firstOrNull { it.name == kind } ?: return null, key)
    }
}

/**
 * [BulkPlaceSheet] content (board `BulkPlace`): `모두 같은 장소 | 씬마다 차례로` and `장소 | 지역`, the place
 * map, places with their scene counts (`고름` on the chosen one), `새 장소 | 지도에 핀을 꽂으며 만들기`
 * (→ `PickOnMap`, which creates the pin), and the result button `씬 4개 → 잿더미 시장` (one undo step).
 * `씬마다 차례로` steps through the scenes: the chips name each scene, a pick fills the current one and
 * moves on.
 */
@Composable
fun SheetScope.BulkPlaceContent(sheet: BulkPlaceSheet, vm: BulkViewModel, deps: FeatureDeps, locked: Boolean) {
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    var oneByOne by rememberSaveable { mutableStateOf(false) }
    var regions by rememberSaveable { mutableStateOf(false) }
    var chosen by rememberSaveable { mutableStateOf<String?>(null) }
    var step by rememberSaveable { mutableStateOf(0) }
    var choices by rememberSaveable { mutableStateOf(listOf<String>()) }
    val keys = remember(doc, sheet.sceneKeys) { BulkPlaceForm.live(doc, sheet.sceneKeys) }
    val scope = remember(doc, sheet.chapterKey, locked) { BulkModel.scope(doc, sheet.chapterKey, locked).map { it.key } }
    val options = remember(doc, scope, regions, locked) { BulkPlaceForm.options(doc, scope, regions, locked) }
    val perScene: Map<Key, PlaceRef> = choices.mapNotNull { e ->
        val (k, v) = e.split("=", limit = 2).takeIf { it.size == 2 } ?: return@mapNotNull null
        PlaceCodec.decode(v)?.let { k to it }
    }.toMap().filterKeys { it in keys }
    val current = keys.getOrNull(step.coerceIn(0, (keys.size - 1).coerceAtLeast(0)))
    val chosenRef = if (oneByOne) current?.let(perScene::get) else PlaceCodec.decode(chosen)
    val codes = remember(doc) { UnitCodes(doc) }
    val mapRequest = rememberSaveable { "story.bulk.place.${sheet.projectKey}" }

    fun pick(ref: PlaceRef) {
        if (oneByOne && current != null) {
            choices = perScene.plus(current to ref).map { (k, v) -> "$k=${PlaceCodec.encode(v)}" }
            if (step < keys.lastIndex) step += 1
        } else {
            chosen = PlaceCodec.encode(ref)
        }
    }

    val latestPick by rememberUpdatedState(::pick)
    LaunchedEffect(mapRequest) {
        deps.results.results(mapRequest).collect {
            (deps.results.consume(mapRequest) as? PlaceRef)?.let { latestPick(it) }
        }
    }

    SheetHeader(BulkPlace.title(keys.size), onClose = ::close)
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        FilterChip(BulkCopy.ALL_SAME_PLACE, !oneByOne, { oneByOne = false })
        FilterChip(BulkCopy.ONE_BY_ONE, oneByOne, { oneByOne = true })
        Spacer(Modifier.weight(1f))
        OutlinedSegmented(
            items = listOf(SegmentItem(BulkCopy.KIND_PLACE), SegmentItem(BulkCopy.KIND_REGION)),
            selectedIndex = if (regions) 1 else 0,
            onSelect = { regions = it == 1 },
            height = 30.dp,
        )
    }
    // `씬마다 차례로` brings its scene chips and the step line in with a fold (P2).
    FoldReveal(oneByOne && keys.isNotEmpty()) {
        Column(Modifier.fillMaxWidth()) {
            ChipFlow(Modifier.padding(top = 10.dp)) {
                keys.forEachIndexed { i, k ->
                    val label = listOfNotNull(codes.sceneRef(k), perScene[k]?.let { BulkCopy.CHOSEN }).joinToString(BulkCopy.SEP)
                    FilterChip(label, i == step, { step = i })
                }
            }
            current?.let { k ->
                val scene = doc.index.unit(k)
                ValueSwap(
                    listOfNotNull(BulkCopy.step(step, keys.size), codes.sceneRef(k), scene?.title).joinToString(BulkCopy.SEP),
                    type.small,
                    Modifier.padding(top = 8.dp),
                    color = colors.ink3,
                )
            }
        }
    }
    val accent = setOfNotNull(chosenRef?.takeIf { it.kind.known == PlaceRefKind.PLACE }?.key)
    val map = remember(doc, accent, options, locked) { MiniMaps.places(doc, accent = accent, prefer = options.filter { it.sceneCount > 0 }.map { it.ref.key }.toSet(), exclude = BulkModel.hiddenPlaces(doc, locked)) }
    map?.let { m ->
        MiniMapView(m, Modifier.padding(top = 12.dp).fillMaxWidth().height(170.dp), onPin = if (regions) null else { k -> pick(PlaceRef(PlaceRefKind.PLACE, k)) })
    }
    SheetScrollBody(Modifier.padding(top = 8.dp)) {
        // Places and regions swap on the kind (P3); each list is computed for its own kind.
        ContentSwap(regions, Modifier.fillMaxWidth()) { regionList ->
            val listed = remember(doc, scope, regionList, locked) { BulkPlaceForm.options(doc, scope, regionList, locked) }
            Column(Modifier.fillMaxWidth()) {
                listed.forEach { option ->
                    val isChosen = chosenRef?.sameAs(option.ref) == true
                    ListRow(
                        title = option.name,
                        subtitle = option.countText,
                        leading = { CivilightIcon(IconId.Pin, contentDescription = null, size = 20.dp, tint = if (isChosen) colors.acc else colors.ink3) },
                        trailing = if (isChosen) { { CivilightText(BulkCopy.CHOSEN, type.small, color = colors.acc) } } else null,
                        onClick = { pick(option.ref) },
                        selected = isChosen,
                        compact = true,
                    )
                }
            }
        }
        ListRow(
            title = BulkCopy.NEW_PLACE,
            subtitle = BulkCopy.NEW_PLACE_SUB,
            leading = { CivilightIcon(IconId.Pin, contentDescription = null, size = 20.dp, tint = colors.ink3) },
            onClick = { deps.navigator.push(PickOnMap(sheet.projectKey, mapRequest, sceneKey = current.takeIf { oneByOne }, sceneCount = if (oneByOne) 1 else keys.size)) },
            compact = true,
        )
    }
    val refs = if (oneByOne) keys.mapNotNull(perScene::get) else listOfNotNull(chosenRef)
    val count = if (oneByOne) perScene.size else keys.size
    val footer = BulkPlaceForm.footer(doc, count, refs)
    CivilightButton(
        text = footer ?: BulkPlace.title(keys.size),
        onClick = {
            if (oneByOne) vm.placeEach(perScene) else chosenRef?.let { vm.placeSame(keys, it) }
            close()
        },
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        enabled = footer != null && !store.readOnly,
    )
}
