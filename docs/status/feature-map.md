# feature:map

Package `io.github.lenticualrstone.civilight.feature.map`. Build and tests: `tools/gw :feature:map:compileDebugKotlin :feature:map:testDebugUnitTest :domain:world:test` exits 0. feature:map has 188 JVM tests (after the 0.1.0 motion wave), all passing; `domain:world` 123 (1 skipped by the world lane), all passing. Nothing was run on a device or emulator, and no APK was built (the APK hold).

- Batch 1 of 3 (2026-11-22): WLD-36, 37, 38, 39, 40, 41, 42, 43, 44 and 52.
- Batch 2 of 3 (2026-11-22): WLD-45, 46, 47, 48, 49, 50, 51, 53, 54 and 55.
- Batch 3 of 3 (2026-11-22): WLD-56, 57, 58, 59, 60, 61, 62, 65 and 66, plus the loose ends map rename and map delete (WLD-36), stamps and drawn lines on every map surface and in exports, and the format tool sets (WLD-40).
- Domain changes (additive, `domain:world` `map` package only):
  - Batch 2: `map/TravelModeEdits.kt`, `map/LayerOrder.kt`, `map/MapNesting.kt`, `map/NodeEdits.kt`, tested by `MapAuthoringTest` (WLD-45, 47, 48, 50).
  - Batch 3: `map/HexEdits.kt` (`줄` line, `paintCells`, `setOrientation`, `setCoordEvery`, `infoLine`) and `map/DetailEdits.kt` (author lines, line points flagged author-edited, stamps that attach to the river or road under them, `moveStamp`, `stampChip`), tested by `MapFormatEditsTest` (WLD-58, 60).


**0.2.0 (2026-11-24):** maps are simplified per S-3 / WLD-75 (`common/MapOffers.kt`, `common/HiddenRoute.kt`); see `feature-world.md` › `0.2.0 wave 2` for what was built, hidden and requested.

## Motion wave (0.1.0, 2026-11-23)

Motion audit §4 "feature:map", most visible first, through the shared helpers only (`motion.swap` / `move` / `enter` / `exit`, `ContentSwap`, `FoldReveal`, `civilightItem`, `animatedFraction` / `animatedAlpha` / `animatedColor`, `rememberUnhide`, `SidePanelHost`, `TabContent`, `SnapCardRow`). No duration or curve literal is left in the module: `MapMotionLintTest` runs `MotionLint.lintModule` and passes. Build line: `tools/gw :feature:map:compileDebugKotlin :feature:map:testDebugUnitTest` exits 0, 188 tests, nothing run on a device or emulator, so **every row below is partial until the orchestrator sees it on the emulator**. `domain:world` was not touched in this wave.

New shared pieces of this module (`common/MapMotion.kt`, `common/MapBarRules.kt`, `canvas/MapGestures.kt`, `canvas/HexSpread.kt`, `gen/GridFill.kt`):
- `MapBar(slot)`: `AnimatedContent` over plain data (`MapBarSlot(kind, ui)`), key = the kind, so the bar that leaves keeps its own last data and a change inside one kind updates in place. The new bar rises 12 dp on `enter`, the old fades on `exit`, size follows on `standard`; reduced: fades. Which bar shows is the pure `MapBars.view / edit / node` (tested).
- `FoldSwitch(inner)`: cover ↔ inner overlay layer crossfade on `foldOpen` / `foldClose`; the side panel goes through `SidePanelHost(inner)` (48 dp from the right on `foldOpen`).
- `Modifier.appear`, `markAlpha`, `markProgress`, `unhideIn`: appear-once helpers for marks that are new to a screen.

| Audit row | Done | Left |
| --- | --- | --- |
| 1 Place panel, bars, cover cards | `MapBar` in 지도 보기 (`핀 옮기기` · place sheet · tools), 편집 (move · shape choice · layers · properties · stamps), 노드 지도 (step · layers · properties), 지역 나누기 (split bar), 지도에서 고르기 (pick · new place). Tool bars use `animateContentSize(motion.move(standard))`. Side panels (편집, 노드, 지역, 칸 칠하기, 인물 동선) via `SidePanelHost` + `TabContent`. Hex cell line via `FoldRevealValue`. The exit of the inner place card is the same `MapBar` | – |
| 2 Programmatic viewport moves | `rememberViewportMover` (`foldOpen`, 360 ms as WLD-68; snap when reduced; the first UNSET resolve is a cut) for the focused pin (지도 보기), a picked node (노드 지도), a map chip (지도에서 고르기 → `Viewport()`) | – |
| 3 Pan / zoom settle | `detectMapTransform` (`awaitEachGesture` with an end hook, nothing consumed before touch slop) replaces `detectTransformGestures`. A pinch may pass 1 ×…8 × to 0.8 ×…10 × (`ViewportMath.transform(soft = true)`) and settles on release with `animateTo(clamp, standard)`; a pan carries on and decays (`exponentialDecay`). Reduced: snap, no decay. `transform` / `clamp` and their tests are unchanged (the soft path is separate) | `exponentialDecay()` uses the Compose default friction (no token exists) |
| 4 Fold layout | `FoldSwitch` in 보기, 편집, 노드, 지역, 칸 칠하기, 시점별 공개, 축척, 지형 만들기 (steps), 인물 동선 | 글로 설명 keeps its card placement (no cross-fade); the canvas inset of the inner steps page is inside the swapped layer |
| 5 Generate | Pages `AnimatedContent(page)` with `motion.push`; preview `Crossfade((world, format), motion.spec(token))`, `mapReroll` (400 ms) on the steps page, `standard` on the format tiles; the focused step's chips `FoldReveal`; candidate tiles `appear` one after another (keyed by history index); candidate ring `animatedColor` + width | The 1.2 s **step-order draw** (`mapGenerate`) and the per-layer re-roll flow (old layer out on `exit`, new in on `mapReroll`): the preview is one raster of all layers, so it needs layer-wise rasters from `GenRaster` (a bigger change, not half-built) |
| 6 Map list | `SnapCardRow` (DS, `showPager = false`), `ContentSwap` loading / empty / cards (keyed by kind), thumbnails fade in (row 9). QA 8: the empty state no longer repeats `지도 · 0` (the block shows it once) and has 24 dp side margins | To confirm: the DS row scales the thumbnail image 1.1× (card parallax), which crops about 5 % of each map thumbnail edge |
| 7 Overlay marks | `MapCanvas` keeps per-key looks: shapes (lines, regions, links, routes, stamps) fade to their dim alpha and back, ease the selected stroke on `standard`; labels, link labels, route chips, nodes and pins fade in when new to the scene; label / chip / node dim alpha via `markAlpha`; marks glide in **map coordinates** (`animateOffsetAsState`), so pan and zoom never lag. `MapPin` and `NodeCard` keep their own DS dim and select motion | **Exit** fades of removed marks and the layer filter fade (items leave the scene, they are not dimmed) need the removed marks kept for the fade; not built. Curved labels (`drawCurvedLabel`) still switch dim and selected colour in one frame (static alpha) |
| 8 Reveal by point | Track fill `animateFloatAsState(move(standard))`, chip counts `ValueSwap`, the pending chip folds in, newly visible marks `unhideIn` (blur and dim clear, 60 ms stagger; `MapCanvas(unhideFresh = true)`) | – |
| 9 Base image and tiles | Overview picture and generated raster fade in over the placeholder (`standard`); each sharper tile fades in; `DelayedProgress` in 지도 보기 (`지도 이미지 바꾸기` while replacing, `지도 내보내기` while exporting) and 노드 지도 (export) | The chip only names the work: there is no progress fraction in `MapExport.render` or `replaceBase`, and no indeterminate DS bar |
| 10 Edit drafts, vertex, region close, route, stamp, hex | Vertices that already existed use `MapVertex(popOnAppear = false)`, drafts pop (DS S18g); a closed region fills in over 200 ms (`standard`); a new route draws along its length (`PathMeasure`, `enter`); a new stamp drops 14 dp and settles on `stampDrop` (240 ms); painted hex cells spread by distance within one `standard` run (`HexSpread`, tested) | – |
| 11 Character track | Position chips walk along the leg on `enter` (`TrackPath`, tested; jump when reduced), the focused leg's stroke `animatedFraction`, the play pause is the named `MapMotion.PLAY_STEP_MS` (900) through `motion.delayMs`, panel tabs `TabContent` | 900 ms has no token (Request) |
| 12 Text to map | Pages `ContentSwap`; the world `AnimatedVisibility` (last world kept while it leaves); condition rows appear one after another on `rowIn` (250 ms, 60 ms step), in the input and result lists; the highlight fades (`animatedFraction`, last area kept); the card follows its height | – |
| 13 Panel tabs and lists | `LayersPanel` and `ItemsPanel` are keyed lazy lists with `civilightItem()` (height bounded at 320 dp because they sit in a scrolling panel); 이동 수단 list ↔ form, new-place list ↔ form: `ContentSwap`; new-map source buttons and the `등고선` switch: `FoldReveal`; 축척 card `FoldRevealValue`; A/B chips `appear` | – |

QA 2026-11-23:
- **#7 grid** (`양식` and `후보 기록`): fixed-width tiles (DS `FormatTile` 166 dp, candidate 150 dp) are laid out by the pure `GridFill`: the column count that fits with a 10 dp minimum gap, the leftover spread between the columns, so the last tile reaches the right margin (`GridFillTest`). The DS tile stays 166 dp (Request).
- **#9 zoom**: `ViewportMath.resolve(UNSET)` now opens fitted (zoom 1, the whole base inside, `Viewport()`), not at `fillZoom()` (296 % on a tall phone). `fillZoom()` stays for callers that want it. This follows the QA over the board wording "fill, no empty bands" (spec owner: say if the boards should win).
- **#9 bottom bar `새 지도 · 되돌리기` light in dark mode**: no map code draws it. It is the toast of `MapViewModelBase` (`STEP_NEW_MAP`, action `되돌리기`), and `toastStyle` is `ink` fill with `bg` text, i.e. inverted by design (DS-27). Nothing was hacked locally; see Requests and Spec conflicts.

Pane split (CNV-47): feature:map has no two-pane list + detail frame; its panels float over the canvas (glass cards, side panels). The closest thing to an own split is the inner `지형 만들기` steps page, which reserves a fixed 260 dp for its panel next to the preview (`StepsPage`). `PaneSplit.Modules` has no map id, so nothing was wired (Request below). "Copies that moved to core": a grep found no local `SnapCardRow`, `CardViewMotion`, `FoldMotion`, `RevealMotion` or `ListCardMotion` in this module.

Watch on the emulator (inferred from source, never run):
- The inner place card (`MapBar`, top-anchored) may still shift while its height settles.
- Over-pinch settle: release past 8 × or under 1 ×, tap during the 200 ms settle (the tap restarts it, `onEnd` always runs).
- A new map in `지도에서 고르기` fades every mark in (all keys are new on another map).
- The glass cards inside `MapBar` while a bar swaps (blur layers during the size animation).
- `LayersPanel` / `ItemsPanel` heights (bounded at 320 dp inside scrolling panels).

Copy: the nine `TODO_COPY_MAP_*` constants are gone. They now hold the nearest existing words (listed under "Copy to confirm" in Requests) and `MapCopyLintTest` fails on any `TODO_COPY_` value.

## Architecture

**Entries** (`MapEntries.kt`, `@file:JvmName("MapEntries")`, `mapEntries(deps)`):
- Registered: every map route. `MapList`, `MapView`, `MapEdit`, `PickOnMap`, `MapScale`, `MapReveal`, `NodeMap`, `MapFormatPicker`, `MapGenerate`, `SketchStraighten`, `DetailStamps`, `HexPaint`, `PolygonRegions`, `TextMap` and `CharacterTrack`.
- This module owns no shared sheet: `Sheets.kt` names none with owner MAP. Every sheet here is a feature-local `@Serializable SheetKey`.

**Ways in (batch 3; nobody outside this module pushes these routes):**
- `⋯` (`MapMoreSheet`, rows from the pure `view/MapMenu`) on 지도 보기, 지도 편집 / 상세 지형 · 편집 and 노드 지도: `이름` (rename), the format editor (`칸 칠하기` → `HexPaint`, `다각형 · 지역 나누기` → `PolygonRegions`, `상세 지형 · 편집` → `DetailStamps`), `단계 다시 만들기` (generated maps; disabled with `TODO_COPY_MAP_REROLL_OTHER_VERSION` for another generator version), `인물 동선` (지도 보기 → `CharacterTrack(p, mapKey)`), `지우기` (view and node map), then `축척 맞추기 · 시점별 공개 · 지도 내보내기`. The editor's `⋯` has no export.
- `편집` on 지도 보기 still opens `MapEdit` (pins, regions, routes, labels on every map); the format editors are separate screens, so pins and routes stay reachable on hex, polygon and detailed-terrain maps.
- `그림 · 색면` chip (DS `LayerFlagChip`) at the bottom of 지도 보기 for generated detailed-terrain and polygon maps (WLD-57), opening the local `MapStyleSheet`.
- `TextMap(p)` from the `글로 설명` card of `양식` (batch 2); `TextMap(p, mapKey)` opens with that map's stored conditions.
- `CharacterTrack(p, characterKey?, mapKey?)` can be pushed by other lanes (cast profile, see Requests).

**Packages**

| Package | What |
| --- | --- |
| `MapCopy.kt` | All UI copy (one object, DECISIONS §3), checked by `MapCopyLintTest`. `BTN_` constants are buttons. Undo labels (also the toast text) are `MapCopy.step(<source noun>, <subject>)`, e.g. `핀 · 결사의 문`, `핀 옮기기 · 결사의 문`, `영역 · 상층 관문 구역`. Missing copy is a `TODO_COPY_MAP_…` constant whose value is its own name (see Requests). |
| `common/` | **`MapEnv`**: the app-scoped deps a view model may keep (sessions, unlock, clock, device id, blobs, toasts, app scope).<br>**`MapScreenViewModel`**: the base of every view model, built on `ProjectViewModel`. It provides `readOnly`, `hiddenOpen`, `editContext()` and `lensOf(doc)`. Its `commit(label) { doc -> doc? }` and `commitValue(label) { doc -> (doc, value)? }` are the one write path: one `update` per user action, then the `되돌리기` toast; a null result means refused, and nothing happens. A read-only project shows the toast `읽기 전용`. `undoLast()` runs on the app scope, so the toast's `되돌리기` still works after the screen is popped.<br>**`MapNav`**: which route shows a map (`NodeMap` for node maps, else `MapView`) and breadcrumb navigation (back to a map already on the stack).<br>**`MapCreation`**: pure new-map and base-swap edits over `MapEdits`. They also add the base `Blob` row to `doc.assets.blobs`, because the package writer only exports catalogued blobs.<br>**`ScreenParts`**: `GlassCard` (consumes taps, so a tap on a card never reaches the map), `TopBarTapGuard` (the strip under the glass top bar), `blockMapTaps`, `CardHeader`, `CardRow` and `mapBack`. |
| `canvas/` | **One map surface for every screen.**<br>`Viewport`: centre and zoom, saved as a float array. `Viewport.UNSET` starts a screen at the fill zoom.<br>`ViewportMath`: pure map↔screen conversion, pinch around the centroid, pan, clamp, visible rect, `fillZoom` and `resolve`.<br>`MapScene` / `MapScenes.build(doc, map, lens, hiddenOpen, mode, layerFilter)`: the domain readings as drawables. It uses `MapReveal.at` for liveness, masking and layers, `RegionShapes.shapeAt` and `Routes.sheet(...).mapLabel`.<br>`MapBase` / `MapBases.of(map)`: `Image`, `Generated` (stored `gen-1` world raster with rivers and roads, or the map's hex cells), `Node` or `Blank`. WLD-57 styles plug in at `MapBase.Generated`.<br>`TilePlan`: the large-image plan.<br>`MapHitTest`: tap targets in px (44 dp; a pin's target is centred on its icon and covers its name tag).<br>`CurveGeometry`: curved labels.<br>`MapColors`: region tokens and terrain mapping.<br>`MapCanvas`: the composable (base, overlays, two-finger pan/zoom, taps with hit results; `extraDraw` and `marks` slots for drafts and handles). `BaseLayers` draws the bases. |
| `list/` | `MapListScreen` / `MapListViewModel` / `MapListState`, `NewMapSheet` (`parentMapKey`, `parentPinKey` for WLD-48) and `NewMapFlow` (`rememberNewMapFlow(projectKey, deps, vm, sheets)`: the `새 지도` sheet plus the system pickers and camera, usable from any map screen; the edit screen uses it for a pin's `하위 지도 · 만들기`). |
| `view/` | `MapViewScreen` / `MapViewViewModel` / `MapViewState`, `MovePinBar` (shared with edit) and `MapMoreSheet` (`⋯`: `축척 맞추기 · 시점별 공개 · 지도 내보내기` with `4K · 8K`; shared by the node map). |
| `edit/` | `EditorState` + `Editor`: a pure, saveable reducer (tools, region and route drafts, pending pin and label spots, `핀 옮기기`, chosen vertex or curve handle, panel tab).<br>`EditState`: the pure UI builder (`PropsUi` per selection or draft, layers, `목록` rows).<br>`MapEditViewModel` (open; `NodeMapViewModel` extends it): one commit per action over `MapEdits`, plus layers (add, rename, shown, order via `LayerOrder`), child maps (`MapNesting.setParent`) and route modes (stored first through `TravelModeEdits`).<br>`MapEditScreen`, `EditPanels` (`PropsPanel`, `LayersPanel`, `ItemsPanel`) and `EditSheets` (local sheet keys `PinPlaceSheet`, `LabelTextSheet`, `RevealSheet`, `LayerSheet`, `FactionSheet`, `KindSheet`, `RegionNameSheet`, `LayerNameSheet`, `ChildMapSheet`).<br>`PinPick`: the rows of `핀 · 장소 고르기`. |
| `sketch/` | `Straighten` (pure corners and output size), `SketchImages` (camera target through the module's own `MapSketchFileProvider`, capture import, EXIF-upright decode, `Matrix.setPolyToPoly` straightening into a blob), `StraightenScreen` (the corner UI) and `SketchStraightenScreen` (the `SketchStraighten` route: `펴기` as a back-stack entry, then the new map opens in `편집`). |
| `pick/` | `PickState` (pure; `sceneCount` for bulk `씬 4개 → …`) and `PickOnMapScreen` / `PickOnMapViewModel`. |
| `scale/` | WLD-46. `ScaleDraft` (saveable JSON: A, B, which one the next tap places, number, unit, mode index), `ScaleEdit` (pure reducer, labels, `save` = `TravelModeEdits.ensureStored` + `MapEdits.setScale`), `ScaleBars` (1 or 2 days by zoom), `MapScaleScreen` / `MapScaleViewModel`. |
| `travel/` | WLD-47. `TravelModeSheet` (feature-local key; `routeKey` = the route that takes a new mode, `forScale` = a pick returns the mode), `TravelModeRows`, `TravelModeForm` (pure), `TravelModeContent`. Writes go through `MapScreenViewModel.saveTravelMode` (`TravelModeEdits.add` / `update`). |
| `reveal/` | WLD-49. `RevealState` (pure: breadcrumb, `1-5 시점 · 보이는 것만`, episode stops, `MapReveal.at` summary chips, the scene in `SceneMode.REVEAL`) and `MapRevealScreen` / `MapRevealViewModel` (`숨은 것 흐리게` = `WorldMap.revealViewDim`, one undo step). |
| `node/` | WLD-50. `NodeEditorState` + `NodeEditor` (pure, saveable reducer: tool `이동 · 노드 · 연결 · 라벨 · 레이어`, selection, first node of a connection, node being moved, tapped spot), `NodeMapState` (pure UI: props, rows, search, step bars), `NodeMapViewModel` (extends `MapEditViewModel`; nodes and connections over `MapEdits.addNode` / `linkNodes` and `NodeEdits`, duplicate connections refused), `NodeMapScreen`, `NodePanels`, `NodeSheets` (local keys `NodeNameSheet`, `LinkLabelSheet`, `NodePlaceSheet`, `NodeSearchSheet`). |
| `export/` | WLD-51. `ExportPlan` (pure sizes, bands, file name), `MapExport` (Android: draws base + `MapScene` into a bitmap, bands for large images, PNG into `cache/map_export/`, system share sheet through the module's FileProvider), `MapExporter` / `rememberMapExporter(deps)` (one per screen; `exporting` state). |
| `common/MapMenuHost` | Batch 3. The map menu's actions and sheets for every map screen (`rememberMapMenuHost(projectKey, mapKey, deps, vm, sheets)`; `content(key, doc, readOnly)` in `localSheets`, `onRow(action, doc)`): `MapNameSheet`, `MapStyleSheet` (WLD-57), `RerollStepsSheet` → `RerollGuardSheet(step)` (domain `StepReroll.sheet`: title, line, two radios, note, `취소 · 다시 만들기`) → `StampCheckSheet(rows)` (`위치 확인`; the rows travel in the sheet key, because the re-roll result is not stored). `MapScreenViewModel` gained `renameMap`, `setStyle`, `reroll` (generator off the main thread; `RerollMerge.splice` puts only the re-rolled map row in, and only while that map is unchanged), `moveStampTo` and `deleteMap` (`domain:trash` `TrashOps.delete` + `Messages.deleteToast`). |
| `canvas/` (batch 3) | `GenRaster` (pure ARGB): the WLD-57 pictures `색면` (fields per nearest town in `map-art-2…4`, land-only blur, sea `map-art-1/5`), `먹선`, `색` (`ter-*`), `양피지` (sepia of `ter-*`), polygon cells by nearest site with province colours and darker borders (WLD-59), `등고선` (marching squares at 150 … 750), and `BaseLine`s (coast, ripple, ridges, rivers, roads). `BasePalette` carries the theme colours; `baseRaster` / `baseLines` (BaseLayers.kt) are shared by `MapCanvas` and `MapExport`, so exports match. `MapBase.Generated` now has `poly`, `style`, `contours` and `worldLines` (false on DETAIL maps, whose rivers and roads are `SceneLine`s). Hex bases draw the automatic marks (`HexMarks`) and `좌표 표시` labels. `MapScene` has `lines` (`SceneLine`) and `stamps` (`SceneStamp`, through `MapReveal`'s visibility like every overlay); `StampGlyphs` (12 glyphs, sizes, rotation) draws them in the canvas and the export. `MapHit.Stamp` and `MapHit.Line` are returned only with `MapHitTest.hit(..., terrain = true)` / `MapCanvas(hitTerrain = true)` (the detailed-terrain editor), so terrain never shadows regions elsewhere. A DETAIL map without a drawn river (or road) shows the stored world's (`worldRivers`, `worldRoads`). Hex exports draw the marks and coordinates too. |
| `edit/` (batch 3) | `EditTool` has `STAMP · RIVER · ROAD` and the lists `BASE` / `DETAIL`. `EditorState` gained `stampKind`, `stampSize`, `line` (the `강 · 길` draft), `movingStamp`, `shapeEdit` (WLD-65) and `compare`. `MapEditScreen(route)` and `DetailStampsScreen(route)` are one private `MapEditorScreen(..., detail)`: detail uses the DETAIL tools and the panel `도장 · 범례 · 레이어` (`StampPalette`, `LegendPanel`). Commits are read by the selected kind: `MoveVertex` on a drawn line moves a line point, `MoveCurveHandle` on a stamp is `StampHandles.ROTATE / SIZE` (tap the handle, then the map). `ShapeEdits` (WLD-65) routes a vertex move through `LensEdit.choice`; `ShapeChoiceBar` shows the two buttons. `PropsUi.StampProps`, `LineProps`, `LineDraftProps`; `RegionProps.shapes` (`1-9 ◆`) with `비교` chips. |
| `hex/` | WLD-58. `HexPaintState` + `HexPaint` (pure saveable reducer: `한 칸 · 줄 · 채우기 · 고르기`, terrain, `고르기` cell, `줄` start), `HexPaintUiBuilder`, `HexPaintViewModel`, `HexPaintScreen` (local sheets `HexCellKmSheet`, `HexCoordsSheet`, `HexPlaceSheet`). |
| `poly/` | WLD-59. `PolyState` + `PolyEditor` (pure: `고르기 · 칸 넣기 · 합치기 · 나누기 · 땅 · 바다 · 레이어`), `PolyUiBuilder`, `PolyViewModel` (extends `MapEditViewModel` for layers), `PolygonRegionsScreen` (local sheets `ProvinceNameSheet`, `ProvinceFactionSheet`). |
| `text/` | WLD-61. `TextMapState` / `CondState` (saveable), `TextMapFlow` (pure: reading with the model stub then the keyword reader, `함께 읽기` sources, author conditions kept over a new reading, `위치`, candidates, result rows and header), `TextMapViewModel` (worlds off the main thread, `이 지도로 시작`), `TextMapScreen` (local sheets `CondEditSheet`, `CondPlaceSheet`, `CondPositionSheet`; the editor draft lives in the screen's saveable state). |
| `track/` | WLD-62. `TrackState` (pure over `domain:world` `CharacterTracks`: chips, rows, places, stop marks, route-following or straight `추정` lines, position at the lens, map choice, ancestor-pin placement, chip taps), `CharacterTrackScreen` + `TrackViewModel` (scrubber from `reveal/` with play). |
| `gen/` | WLD-53 … 55. `GenFlowState` (saveable JSON: page, kind, land ratio, format, style, contours, steps, history with each entry's kind and ratio, focus, last batch, chosen), `GenFlow` (pure reducer and `GenUi`), `FormatCard`, `MapGenerateViewModel` (previews off the main thread with an LRU by `GenInput`; `start` runs `MapGenerator.start` off the main thread and commits the new map as one step `새 지도`), `MapFormatPickerScreen` / `MapGenerateScreen` (both pages in one back-stack entry), `FormatPage`, `StepsPage` (cover and inner), `CandidateHistorySheet`. |

**Conventions for later batches**
- **Screen state that must survive fold or process death** is `rememberSaveable` in the screen: viewport arrays, selection, layer filter, the editor state as JSON, the sketch photo as `Blob` JSON. View models hold only the document and the writes.
- **New tools**: add an `EditTool` set per format, or a parallel reducer (hex `한 칸 · 줄 · 채우기 · 고르기 · 되돌리기`, polygon, detail, node). Feed it through `MapToolbar(tools = …)`. Use `MapCanvas.extraDraw` / `marks` for its drafts. Add a `PropsUi` case for its panel.
- **Drawing**: anything new drawn over a map goes into `MapScene` (built through `MapReveal`) and is drawn in both `OverlayCanvas` and `MapExport.drawOverlays`; base pictures go through `GenRaster` / `baseRaster` / `baseLines`, which both renderers use.
- **Map menu**: anything a map screen offers beyond its tools is a `MapMenuAction` row in `view/MapMenu.rows` handled by `common/MapMenuHost` (one list for every map screen).
- **Lens edits of map values** (WLD-65): go through `domain:story` `LensEdit.choice` and show `ShapeChoiceBar`; never write a `◆` record by hand.
- **Writes**: every write is `commit` / `commitValue` in a `MapScreenViewModel`; a domain refusal returns null and nothing happens. Things that keep a travel mode key store the modes first (`TravelModeEdits.ensureStored`), because `Travel.modes` makes fresh default keys on every call while a project stores none.
- **Rule 19 masking**: every list, property panel and pick reads the `MapScene` (built through `MapReveal.at`), never `map.pins` / `map.regions` directly, so hidden places and factions stay out while locked.
- **`PickOnMap` contract**: the caller picks a request key and pushes `PickOnMap(p, requestKey, sceneKey?)`. It then collects `deps.results.results(requestKey)` and calls `consume(requestKey)`. The value is a `model.story.PlaceRef`: `PLACE` from a pin, `REGION` from a region in `지역` mode. The screen pops after `고르기`.
  - The first map shown is the one holding the scene's current place or region, else the first top-level map.
  - The mode starts as `지역` when the scene's place is a region.

## Done

- **WLD-66 거리 계산** (pure logic, no screen of its own): `domain:world` `map/Distances.distanceKm(from, to)` returns km, `ZERO · ROUTE · STRAIGHT` and `NO_SCALE · NO_LOCATION`, with ancestor pins on the nearest common map and regions by nearest points; `Travel.requiredMinutes` and `formatDuration` (no detour factor). Tests: `domain:world` `MapSpecTest` WLD-T53, T55; `domain:actions` `DistanceConsistencyTest` (DECISIONS §1). The map lane uses the same ancestor-pin rule to place track stops (`TrackState.pointOn`). Nothing was missing against the spec signature (the optional `mode` is the caller's `requiredMinutes(km, mode)`).

Every UI feature has a screen and none has been seen on a device yet, so they are listed under Partial with their pure parts and tests.

Cross-lane requests handled in batch 2:
- **Story lane, `PickOnMap.sceneCount`** (core pass added the argument): the confirm bar reads `씬 4개 → 잿더미 시장` for a bulk pick (`PickState.build(…, sceneCount)`, `PickCountTest`).
- **Recorder lane, "map lanes push `Recording(projectKey = p)`"**: the node map's command bar `/ 장소 · 노드 찾기` has the `라이프 레코더` button, which pushes `Recording(projectKey = p)`. Map views have no command bar on the boards, so no other map screen pushes it.

## Partial

UI built and compiled, logic tested on the JVM, not seen running:

- **WLD-56 시작 뒤 단계 다시 만들기** (`common/MapMenuHost`, board `MapReroll`):
  - `⋯ › 단계 다시 만들기` lists `해안선 · 산 · 강 · 숲 · 기후 · 마을 · 길` as `<단계> 다시 만들기`. Without author edits the step re-rolls at once; with them the guard sheet shows `강 다시 만들기`, `직접 고친 강 2 · 강에 붙은 도장 1(다리)`, the radios `고친 강은 남기기 · 나머지만 새로` (default) and `모두 새로 · 다리 도장 → 위치 확인 목록`, the note and `취소 · 다시 만들기` (all domain `StepReroll.sheet`).
  - The generator runs off the main thread; the result is one undo step `강 다시 만들기` (toast `되돌리기`); the previous layer goes into the map's history (`StepReroll`). After `모두 새로`, `위치 확인` lists the stamps of the old layer with `원래 자리 · 지금 자리` (`지금 자리` moves the stamp to the nearest point of the new line).
  - Stamps attach to a river or road when a built stamp is placed or moved onto it (`DetailEdits`), so the guard can count them.
  - `위치 확인` closes itself once every row is answered.
  - Tests: `MapMenuTest`, `Batch3RulesTest` (`RerollMerge`), `domain:world` `GenSpecTest` WLD-T62 (world lane), `MapFormatEditsTest`.
  - Left: the changed-layer motion (old 160 ms out, new 400 ms in); a history list (restoring goes through `되돌리기`); `StepReroll` keeps POLY cells and DETAIL generated stamps from the old world (domain-world Partial, world lane's `gen/`).
- **WLD-57 그림 · 양식 바꾸기** (`canvas/GenRaster`, `view/`):
  - Generated bases draw in the stored style: `색면` (soft province fields in the theme's `map-art` colours, blurred inside the land and clipped at the coast, no blue on land, faint coast · ripple · ridge · river · road lines; follows dark and light), `먹선`, `색` (fixed `ter-*`), `양피지`; hex and polygon cells keep `ter-*` / province colours; `등고선` lines on detailed terrain.
  - 지도 보기 shows `그림 · 색면` for generated detailed-terrain and polygon maps; the sheet `그림` sets the style and `등고선` (one step `그림 · 먹선`). The export draws the same picture.
  - The format is chosen until `이 지도로 시작` (`양식`, batch 2).
  - Tests: `GenRasterTest`, `MapMenuTest`.
  - Left: the 200 ms interior crossfade on a format switch; the pictures are not compared with the boards on a device.
- **WLD-58 육각 편집** (`hex/`, boards `MapHex`, `MMapHexFill`):
  - Header `세계 지도 · 육각` / `칸 칠하기`, chip `24 km · 뾰족 · 32×24`; card (cover) or side panel (inner, `[PROPOSED]`) with the tool bar `한 칸 · 줄 · 채우기 · 고르기 · 되돌리기`, the palette `바다 · 평원 · 숲 · 언덕 · 산 · 사막 · 늪 · 설원` (DS `TerrainSwatch`), the hint, the `고르기` card `O09 | 숲 · 하층 구역 | 칸 24 km · 장소 없음` with `장소 만들기` (name → place and pin at the cell centre), and the rows `칸 크기 | 24 km ›` (also sets the scale), `방향 | 뾰족 · 평평`, `좌표 표시 | 3칸마다 ›`.
  - Each paint, fill, line and setting is one undo step `칸 칠하기 · O09`. Mountain, hill and forest cells get their marks; coordinates are drawn every `좌표 표시` cells.
  - Rule 19: the `고르기` card leaves hidden places and regions of hidden factions out while locked.
  - Tests: `HexPaintTest` (WLD-T64 through the tool, WLD-T65), `MapFormatEditsTest`, `Batch3RulesTest`.
  - Left: the 120 ms paint fade and the fill spreading by distance (≤ 300 ms).
- **WLD-59 다각형 편집** (`poly/`, board `MapPoly`):
  - Header `세계 지도 › 지방` / `다각형 · 지역 나누기`, lens chip; side panel (inner) or bottom card (cover, `[PROPOSED]`) with `지역 · 레이어`: the selected province (`이름 ›`, `색`, `연결 세력 ›`), rows `<이름> | 칸 N`, the hint and `새 지역`; tool bar `고르기 · 칸 넣기 · 합치기 · 나누기 · 땅 · 바다 · 레이어`.
  - `칸 넣기` shows `칸 1 → 하층 구역`; `합치기` merges the tapped province into the selected one; `나누기` picks cells (bar `칸 N · 나누기`) for a new `지역 N`; `땅 · 바다` flips a cell. Each is one undo step `지역 · <name>`. Polygon bases draw provinces in their colours with darker borders.
  - A province linked to a hidden faction reads `숨긴 항목` in `연결 세력` while locked (domain `RefLabel.Hidden`).
  - Tests: `PolyEditorTest` (on a POLY map made by the generator; the fixture has none).
  - Left: provinces are not pickable as a scene `지역` (Q-17 default).
- **WLD-60 상세 지형 편집** (`edit/`, boards `MapDetail`, `MMapStamp`):
  - `DetailStamps`: header `세계 지도` / `상세 지형 · 편집`, lens chip, `⋯`, `되돌리기`, `완료`; tool bar `이동 · 도장 · 강 · 길 · 라벨 · 영역 · 레이어`; panel `도장 · 범례 · 레이어` (inner) or bottom card (cover, `[PROPOSED]`); scale bar.
  - `도장`: the 12 stamps, the hint, `크기 | 1 · 2 · 3`; a tap places the chosen stamp (one step `도장 · 성`). A selected stamp shows the chip `성 · 크기 2 · 0° · 장소 ‘결사의 문’`, a top handle (rotation, snapped to 15°) and a corner handle (size 1 · 2 · 3), both tap-then-place, plus `크기` buttons, `장소 연결 ›`, `보이는 시점`, `레이어` and `이동`.
  - `강 · 길`: taps add points (`지점 N · <km> km`, `되돌리기 · 완료`); the line is author-made. In `이동`, a selected line shows its points; a moved point flags the line as author-edited (WLD-56).
  - `라벨`, `영역`, `레이어` are the editor's own (curved labels WLD-44). `범례` lists `산 · 3 … 강 · 2 · 길 · 1`.
  - Tests: `DetailAndShapeTest`, `MapFormatEditsTest`, `GenRasterTest`.
  - Left: the placing motion (14 px drop, 0.96 press, 240 ms) and the haptic tick on each 15°; rotation and size are tap-to-place, not a drag.
- **WLD-61 글로 설명 → 조건 → 지형** (`text/`, boards `MapText`, `MapTextResult`; needsExternal):
  - Input (`새 지도 · 글로 설명` / `지형 만들기`): text field; `함께 읽기` chips `장소 N` and `페이지 ‘…’` (each toggles); `기기 안 · 클라우드`; `읽어 낸 조건 · N | 키워드`; DS `ConditionRow`s (3 × 3 cells, `⋯` edits); the hint; `조건 · 이 조건으로 만들기`. The text is read 0.5 s after the last change.
  - **External call stub**: `기기 안` tries `domain:world` `LanguageModelConditionReader` (a stub: no model bundled), then the on-device keyword reader; `클라우드` is the same stub and sends nothing (the line `TODO_COPY_MAP_TEXT_UNREADABLE` shows). Names link only to live places (`TextConditions.resolve`); an unknown name is dropped.
  - Conditions are a visible list: the editor `조건` (무엇 with kind chips, 어디 as a 3 × 3 grid of buttons, 특징, 연결 장소 ›, `지우기 · 완료`); the author's conditions survive a new reading.
  - Result (`글로 설명 · 결과`): the map, `조건 4 · 3 반영 · 닫기`, rows with `반영` / `확인 필요` and `위치` or `고치기` (`… → 위치 고르기`; a 3 × 3 picker), a tapped row highlights its area; `다른 후보 3` (three seed chips to choose) and `이 지도로 시작` (one step `새 지도`, stores the condition list).
  - A reading is skipped when text, engine and sources are what was last read (`readOf`), so a fold or restore keeps the list; a condition given a `위치` or edited becomes the author's own. `특징` is typed freely and split at `완료`.
  - Tests: `TextMapFlowTest` (the board's four conditions, WLD-T63), `Batch3RulesTest`.
  - Left: the real model or cloud reader (needsExternal), the phrase underline motion, and the settings row `지형 만들기 · 글로 설명` (library lane's 설정).
- **WLD-62 인물 동선** (`track/`, board `MapTrack`):
  - Header breadcrumb / `인물 동선`, lens chip; character chips (multi-select; the focused one is the last chosen, `acc` lines; others dashed `ink2`); tabs `동선 · 장소 · 레이어`; `리엔 · 씬 순서` rows `1-2 | 하층 거리 | 씬 1 · 1일째 새벽` (a row moves the lens to its scene; rows off this map are dotted); the note; stop chips (`1-2`, DS `RouteStop`), lines along a route or straight `추정`, and the position chip (DS `PersonChip`) at the lens; the episode scrubber with play.
  - Stops come from `domain:world` `CharacterTracks` with `hiddenUnlocked = hiddenOpen`: time order, flashbacks and scenes without place left out, hidden characters out while locked. Inner: side panel + bottom scrubber card; cover (Q-21): one bottom card.
  - Rule 19: while locked, stops at hidden places (or regions of hidden places or factions) and their legs stay out.
  - Tests: `TrackStateTest` (rows, legs, WLD-T58 position at 1-5, ancestor pins, chips), `Batch3RulesTest`.
  - Left: the play button's label is `TODO_COPY_MAP_TRACK_PLAY`; the empty list line is `TODO_COPY_MAP_TRACK_EMPTY`.
- **WLD-65 세력 영역의 시점별 모양** (`edit/ShapeEdits`, `MapEditScreen`):
  - Region handles are the shape in force at the lens. A vertex move under a past lens, or on a shape with `◆` history, shows `이 값 고치기` (with its line, e.g. `기본값을 고침`) and `여기서부터 바뀜 · 1-7` (`새 ◆ · …`) plus the banner `1-5 시점 값 고치는 중 | 고치면 1-5부터 적용` and `취소` (`domain:story` `LensEdit`). Without history at 최신 the base shape moves at once.
  - `이 값 고치기` writes the record in force, or the base shape when none is (`LensEdit.overwrite` has no base for region shapes); `여기서부터 바뀜` adds a `regionShape` record at `LensEdit.newAnchor`.
  - The region card lists `모양 | 1-9 ◆` and `1-9 ◆ · 비교` chips: the other shape dashed, the part outside the current shape dotted.
  - Tests: `DetailAndShapeTest` (WLD-T57, both buttons), `EditStateTest`, `MapScenesTest`.

- **WLD-36 지도 목록 · 새 지도** (`list/`):
  - Header `<project> · 세계` / `지도` / `+`, section `지도 · N`, a row of 200 × 150 thumbnails (live `MapCanvas`) with name and `생성 · 핀 9 · 영역 4` / `세계 지도 › 핀 · 생성`.
  - Empty state, and the `새 지도` sheet with its four rows and the `상위` line when opened with a parent.
  - `지형 만들기` pushes `MapFormatPicker(p, parentMapKey, parentPinKey)`; `노드 지도` creates a NODE map and pushes `NodeMap`; `스케치 촬영` pushes `SketchStraighten(p, photoHash, parent…)`. All three keep the `상위` pin (WLD-48).
  - Tests: `MapListStateTest`.
  - Batch 3: `⋯ › 이름` renames a map; `⋯ › 지우기` (지도 보기, 노드 지도) puts it in the trash through `domain:trash` `TrashOps.delete` with the toast `지도 <name> 지움 · 되돌리기`, then pops (test: `Batch3RulesTest`, the list leaves it out).
  - Left: overlays (pins, regions, routes, labels, stamps, lines) cannot be deleted yet; restoring a map is the trash screen's (`RestoreOps`).
- **WLD-37 지도 보기** (`view/`):
  - The map under glass bars, with small line `세계 · 지도 · <style>`, map name, lens chip, breadcrumb (tap opens that map), zoom label `120%`, two-finger pan/zoom, and saved centre and zoom.
  - The map starts at the fill zoom (no empty bands, as the boards draw it); 100 % (whole base visible) is the minimum.
  - Tapping a pin selects it (`acc`). On cover it opens the place sheet (`<name> · 닫기`, `장소 · <parent>`, description, `등장`, `배경 에셋`, `핀 옮기기 · 장소 카드 열기`). On inner it opens a 270 dp side card (`장소`, `지역`, `장소 카드 · 핀 옮기기`). Both are one saved selection, so a fold keeps it.
  - A pin with a child map shows a `하위 지도 | <name> ›` row (Q-02 default).
  - Bottom: scale bar, layer chips `전체 핀 · <layers>`, `지도 이미지 바꾸기` (image and sketch bases) and `편집`.
  - `focusPinKey` centres on the pin at 200 % and selects it.
  - Tests: `MapViewStateTest`, `MapScenesTest`, `ViewportMathTest`, `MapHitTestTest`.
  - Left: the fold motion and "selected pin keeps its screen position"; the pin-to-child-map zoom transition (WLD-48).
- **WLD-38 이미지 가져오기 · 큰 이미지**:
  - A failed read shows the toast `TODO_COPY_MAP_IMPORT_FAILED` (see Requests).
  - Gallery (`PickVisualMedia`) and file (`OpenDocument image/*`) import into the blob store, uncropped and stored once by hash. A new IMAGE map gets base, size and blob row in one step.
  - Large images (> 4096 px long edge) show a subsampled overview (≤ 2048), plus `BitmapRegionDecoder` tiles of the visible area at the zoom's sample size (LRU of 48).
  - `지도 이미지 바꾸기` keeps overlays.
  - Tests: `TilePlanTest`, `MapListStateTest`.
  - Left: tiling not measured on a device; no progress shown while a large image decodes.
- **WLD-39 스케치 촬영**:
  - `촬영` uses the system camera (`TakePicture` into a private cache file through `MapSketchFileProvider`; the file is deleted after import). `갤러리` picks a photo.
  - `펴기` is the `SketchStraighten` route (a back-stack entry, keeping the `상위` pin): four corner handles; each can be dragged, or tapped and then placed by tapping.
  - Straightening uses a perspective matrix, capped at 4096 px. The original photo and its corners are kept (`MapSketch`). The new SKETCH map opens in `편집` for `덧그리기`.
  - Tests: `StraightenTest`, `MapListStateTest`.
  - Left: on-device check of the camera hand-off (merged manifest, see Requests).
- **WLD-40 편집 모드 · 도구 줄** (`edit/`):
  - Header `<지도> · 편집`, breadcrumb small line, lens chip, `되돌리기` (icon) and `완료`.
  - `MapToolbar` `이동 · 핀 · 영역 · 경로 · 라벨 · 레이어` sits bottom-left on inner and bottom centre on cover, with the full bar on both (Q-16 default). Only the chosen tool acts.
  - The inner side panel has tabs `속성 · 레이어 · 목록`; cover has a bottom card.
  - Every change is one undo step with a `되돌리기` toast. Drafts survive fold and process death. The screen refuses writes in a read-only project (`읽기 전용` banner and toast); `편집` is disabled there in 지도 보기, so this only shows when the screen is reached some other way.
  - The inner vertex bar sits in the side panel's `속성`, not floating over the map as the board `MapEdit` draws it.
  - Tests: `EditorTest`, `EditStateTest`.
  - The format tool sets are their own screens (WLD-58 `HexPaint`, WLD-59 `PolygonRegions`, WLD-60 `DetailStamps`, reached from `⋯`); the node tool set is the node map's (WLD-50). The header has `⋯` (map menu).
- **WLD-41 핀**:
  - The `핀` tool: a tap opens `장소 고르기` (`초성 검색`, `잿더미 시장 · 씬 4개`, `새 장소` → name → place and pin in one step, `숨긴 장소 N` lock row). Places already pinned on this map are left out (one pin per place per map).
  - Pin mark: DS `MapPin`, selected in `acc`, with a 44 dp hit test.
  - `핀 옮기기` (view and edit): the next tap sets the spot, `완료` commits, `취소` leaves it.
  - Properties: `보이는 시점` (unit picker, `1-7부터`, `없음`) and `레이어`.
  - Tests: `EditorTest`, `EditStateTest` (pin picker rows).
  - `하위 지도` property: the pin's child maps (tap opens one) and `만들기 · 고르기` (WLD-48).
- **WLD-42 영역**:
  - Taps add vertices (DS `MapVertex`, the last one filled; dashed draft line). The bar reads `꼭짓점 N` with `되돌리기 · 닫기`; `닫기` is a button only and needs 3 vertices.
  - The panel `새 영역 · 그리는 중` sets name, `종류 | 세력 영역 · 지역 · 기타`, `연결 | 세력 · …`, `보이는 시점`, `레이어` and `색` (`map-art-1…5` swatches) while drawing. `닫기` commits it all in one step; an empty name becomes `영역 N`.
  - A selected region shows its vertex handles: tap a handle, then tap the map to move that vertex (one step). Its properties can be edited.
  - Tests: `EditorTest`, `EditStateTest`.
  - Left: the pop and haptic motion and the 200 ms fill. Editing a `◆` shape is WLD-65 (batch 3).
- **WLD-43 경로**:
  - Taps add points; a tap near a pin (0.02) snaps to it. The bar reads `지점 N · <km> km` with `되돌리기 · 완료`.
  - The route sheet shows title `결사의 문 → 상층 관문`, duration `2일 4시간`, `38 km · 지점 3` or `축척 없음` with `축척 맞추기` (pushes `MapScale`, WLD-46), mode chips `도보 15 km · 8시간 · 말 · 배 · 직접`, `보이는 시점` and `레이어`.
  - Choosing a mode stores `shownModeKey` (the modes are stored first); `직접` opens the feature-local `이동 수단` sheet (WLD-47), and a mode made there becomes the route's mode.
  - The map chip reads `결사의 문 → 상층 관문 · 도보 2일 4시간 · 38 km`. `쓰인 곳` is not shown (Q-18).
  - Tests: `EditStateTest`, `MapScenesTest`, `EditorTest`.
  - Left: the segment-by-segment draw, counter and haptic motion.
- **WLD-44 라벨**:
  - A tap asks for the text (`놓기`); the text can be edited (`글자 ›`). Labels have `보이는 시점` and `레이어`.
  - On detailed-terrain maps, `곡선 라벨` turns on a 3-point curve. Glyphs are set along it and its handles are moved by tap-to-place.
  - Tests: `CurveGeometryTest`, `EditorTest`, `EditStateTest`.
- **WLD-52 장소 고르기 · 지도에서 고르기** (`pick/`):
  - The map in pick mode, with the `장소 | 지역` toggle and a map switcher (node maps left out).
  - A pin picks its place; in `지역` mode, a tap inside a region at the lens picks the region. Only what the pick map draws can be picked: hidden places stay out while locked, and items not yet revealed at the lens are not pickable unless the map dims them (`숨은 것 흐리게`).
  - The bar reads `씬 1개 → 잿더미 시장` (or the name alone without a scene) with `고르기`, which sends the `PlaceRef` and pops.
  - In `장소` mode, a tap on an empty spot offers `새 장소 · 지도에 핀을 꽂으며 만들기` → name → place and pin, then picked.
  - The list half is core:ui `PlacePickerSheet`.
  - Tests: `PickStateTest`.
  - `PickOnMap.sceneCount` > 1 reads `씬 4개 → 잿더미 시장` (`PickCountTest`).
  - Left: the bulk table's `장소별 씬 수` overlay on the map (inner `BulkWide`); it needs the selected scenes per place from the story lane, which the route does not carry.
- **WLD-45 레이어** (`edit/`, `node/`):
  - The `레이어` tool (cover: a card above the tool bar) and tab (inner side panel): per layer a shown switch, `이름 ›` (rename sheet), `보이는 시점 ›` and up / down buttons (order by visible buttons, never drag, `LayerOrder`); `+ 레이어` adds one (name sheet). Each is one undo step `레이어 · <name>`.
  - A switched-off layer drops its overlays from every screen (`MapReveal.at`). Items take a layer through their `레이어 ›` row.
  - Tests: `LayerAndChildMapTest` (rows, order, a switched-off layer), `domain:world` `MapAuthoringTest.WLD-45 …`.
  - Left: the up / down buttons have `TODO_COPY_MAP_LAYER_UP` / `_DOWN` as accessibility labels (see Requests).
- **WLD-46 축척 맞추기** (`scale/`, board `MapScale`):
  - The map with `A` and `B` marks; a card `축척 맞추기 · 닫기` with `아는 거리 하나 → 지도 전체`, the number field, unit chips `일 · km · 리 · 직접`, `하루 이동 | 도보 15 km ›` for `일` (opens the mode list; a pick returns the mode), the rows `A | 결사의 문` and `B | 잿더미 시장` (the chosen row is placed by the next map tap; a tap near a pin snaps to it) and `축척 저장` (one step `축척 · <map>`).
  - Cover: card at the bottom; inner: 320 dp card at the top right (`[PROPOSED]`).
  - Entry: `⋯ › 축척 맞추기` on 지도 보기, and `축척 맞추기` on a route sheet without a scale. Node maps have no scale (no entry).
  - The view's and editor's scale bar reads `<n>일 · <km> km` with n = 1 or 2, whichever day length is closer to the bar's 96 dp at the current zoom (`ScaleBars`).
  - Tests: `ScaleEditTest` (WLD-T54 and the sheet's reducer), `ScaleBars`.
  - Left: the scale bar is DS `ScaleBar` at its fixed 96 dp, so its length is not exactly to scale (see Requests).
- **WLD-47 이동 수단** (`travel/`):
  - The feature-local sheet `이동 수단`: rows `도보 15 km · 8시간` (the current one marked), `직접` for a new mode, and a form `이름 · 하루 이동 [km] · 하루 [시간]` with `만들기` / `완료`. Bad values keep the button off; the domain refuses them too (`TravelModeEdits`). Each save is one step `이동 수단 · <name>`.
  - Opened from the route sheet's `직접` and the scale card's `하루 이동`.
  - Tests: `TravelModeRowsTest`, `domain:world` `MapAuthoringTest.WLD-47 …`.
  - Left: the project setting row `이동 수단 · … · 바꾸기 ›` lives in the library lane's 프로젝트 설정, and its `TravelModes` route is owned by the world lane (not registered yet). Neither can use this module's sheet (see Requests).
- **WLD-48 하위 지도** (`edit/`, `view/`, `list/`, `gen/`):
  - Pin properties: the child maps (tap opens), `하위 지도 · 만들기` (the `새 지도` sheet with `상위 | 세계 지도 · 핀 ‘상층’ | → 하위 지도`, kept through 양식, 펴기 and 노드 지도) and `고르기` (`MapNesting.candidates`: never this map or a map above it).
  - The place sheet and side card list `하위 지도 | <name> ›`. The breadcrumb `세계 지도 › 하층 구역 지도` goes back to a map already on the back stack, else opens it (`MapNav.crumb`).
  - Tests: `LayerAndChildMapTest`, `MapViewStateTest`, `domain:world` `MapAuthoringTest.WLD-48 …`.
  - Left: the zoom into the pin (decelerate 360 ms, haptic) and out (accelerate 240 ms); the breadcrumb swaps all its text at once.
- **WLD-49 시점별 공개** (`reveal/`, boards `MapReveal`, `MMapReveal`):
  - Header `<breadcrumb>` / `시점별 공개` with the lens chip; a card `1-5 시점 · 보이는 것만` with the `숨은 것 흐리게` switch (stored per map, one undo step), the point scrubber (a 44 dp stop per episode, the lens stop in `acc`; a stop moves the project lens) and the chips `보임 · 핀 4 · 영역 1` · `아직 공개 전 · 1`.
  - Hidden items are left out, or dimmed with their point in the tag (`상층 관문 · 1-7`). 지도 보기 and the pick map apply the same rule.
  - Entry: `⋯ › 시점별 공개` on 지도 보기 and 노드 지도.
  - Tests: `RevealStateTest` (WLD-T56 on the fixture, conflict 19), `MapScenesTest`.
  - Left: the unblur motion (6 px → 0, 320 ms, 60 ms stagger).
- **WLD-50 노드 지도** (`node/`, board `MapNode`):
  - Header `<부모> › <이름>` / `노드 지도`, lens chip, `⋯` (`시점별 공개 · 지도 내보내기`); node cards (DS `NodeCard`, sub line) with connection lines and their labels on the base-less surface.
  - Tool bar `이동 · 노드 · 연결 · 라벨 · 레이어`: `노드` on an empty spot asks `이름` and the sub line → node; `연결` is two node taps (the bar `연결 · <first>` with `취소`; an existing pair is refused); `라벨` places a text; `이동` selects. A node card has `이름 ›`, sub line, `장소 ›` (place link, places only), `보이는 시점 ›`, `이동` (the next tap places it) · `연결` · `장소 카드`; a connection has `라벨 ›` and `보이는 시점 ›`.
  - Command bar `/ 장소 · 노드 찾기` (name, sub line, place, 초성; a row selects the node and centres it) and the `라이프 레코더` button (`Recording(projectKey)`).
  - Inner: side panel `속성 · 레이어 · 목록`; cover: bottom card. Read-only projects (the example) only select.
  - Tests: `NodeEditorTest`, `NodeMapStateTest` (the example's 잿더미 시장), `domain:world` `MapAuthoringTest.WLD-50 …`.
  - Left: the sub-line field label is `TODO_COPY_MAP_NODE_SUB` (see Requests); nodes are not in layers (the model gives them none).
- **WLD-51 지도 내보내기 4K · 8K** (`export/`):
  - `⋯ › 지도 내보내기` with `4K · 8K` (starts at the app setting `큰 지도 내보내기`): the base plus what the screen draws at the lens (rule 19 masking and `보이는 시점` hold) into one PNG with a 3840 / 7680 px long edge, then the system share sheet. Large images are read in subsampled bands; an 8K bitmap that does not fit falls back to 16-bit colour, else the toast `TODO_COPY_MAP_EXPORT_FAILED`.
  - Tests: `ExportPlanTest`.
  - Left: not run on a device (memory at 8K, share targets).
- **WLD-53 생성기** (`gen/` over `domain:world` `gen/`):
  - Previews and `이 지도로 시작` run the domain generator (`TerrainGenerator`, `MapGenerator.start`) off the main thread. The stored map keeps version, seeds, choices, history and the result; format layers (hex cells, provinces, rivers, roads, stamps) come from `GenFormats`.
  - Tests: `domain:world` `GenSpecTest` (WLD-T61 determinism), `FixtureGenResultsTest`.
  - Left: the ≈ 1.2 s target on a device.
- **WLD-54 지형 만들기 흐름** (`gen/`, boards `MapFormat`, `MapGen`, `MapGenWide`):
  - `양식`: small `새 지도 · 지형 만들기 1/2`, the note, a 2-column grid of cards with a live preview of the same world per format (`육각 · 다각형 · 상세 지형 · 윤곽 · 노드`, plus `글로 설명` → `TextMap`), `이전 · 단계별로 만들기` (`노드` makes the node map at once).
  - `지형 만들기` 2/2: cover = preview, step tabs, the step card, `기본` chips, style chips and `등고선`, bottom `후보 기록 N · 이 지도로 시작`; inner = preview left, side panel with `기본`, `단계 · 고정 · 다시` rows and the chosen step's chips, `상세 지형 그림`, candidate strip with `새 후보`, and `이 지도로 시작` in the header.
  - `새 후보` makes 3 candidates (new seeds for unlocked steps), all kept in `후보 기록 N` (a sheet of thumbnails on cover); a candidate chosen again brings back its own `종류` and `땅 비율`. `이 지도로 시작` makes the map (one step `새 지도`) and replaces the flow with the map view.
  - Tests: `GenFlowTest`.
  - Left: the step-by-step drawing that doubles as progress (coastline → land → mountains → rivers → forests → towns · roads) and its reduced-motion fade; previews crossfade when ready instead.
- **WLD-55 단계 조작 · 시드 · 고정 · 다시** (`gen/`):
  - Per step: name, seed (mono), `고정` and `다시`; header `상세 지형 · 색면 · 시드 4827 · 해안선 고정`; chips per question (`산맥 수`, `높이`; `개수`, `굽이`, `호수 · 막힌 곳에 호수`), none for the undrawn steps (Q-20); cover card `강 · 시드 0031` with `강만 다시 · 강 고정`. A locked step never changes.
  - Tests: `GenFlowTest`.
  - Left: only the changed layer animates (old 160 ms out, new 400 ms in).

## Defaults used

- **Zoom 100 %** means the whole base fits the view (contain); zoom runs 100–800 %. Every map screen starts at the fill zoom. Map tap targets are 44 dp in screen pixels at every zoom. Pins, nodes and labels are hit first, then routes (within a third of the target), then regions.
- **Edit mode draws not-yet-revealed items dimmed**, so they can be edited.
- **View mode** at a past lens leaves those items out, or dims them when the map has `숨은 것 흐리게` (WLD-49).
- **Layer chips** `전체 핀 · <layer names>`: a layer chip shows only that layer's overlays (view-only filter, not saved). The boards' `하층 구역 · 상층` chips are read as these scope chips.
- **Generated bases** (WLD-57, batch 3): one pixel per generator cell, drawn scaled and smoothed.
  - `색면`: each land cell takes the field of its nearest town (thirds of the map without towns) in `map-art-2 … 4`, blurred over land cells only; sea `map-art-1`, deep sea `map-art-5`, both mixed with `bg`. Lines: coast (`ink3` 55 % plus a 5 dp 18 % ripple), ridges, rivers (`map-art-1`), dashed roads.
  - `먹선`: paper (`bg`, land 6 % `ink`), coast in `ink` with a wide faint second stroke, ridges `ink2`, rivers `ink2`, dashed roads `ink3`.
  - `색`: the fixed `ter-*` table. `양피지`: `ter-*` toned sepia, lines sepia-toned `ink2` / `ink3`.
  - `등고선`: marching squares at heights 150, 300, 450, 600 and 750, `ink3` 35 %.
  - Hex grids draw cells in `ter-*` with automatic marks (`산 · 봉우리` peak, `언덕` hump, `숲` dots; when a cell is ≥ 10 dp) and `좌표 표시` labels (≥ 14 dp). Polygon maps draw cells by nearest site at 160 × 120 in their province colour (unassigned land: the terrain under the site), province borders 45 % `ink`, cell borders 10 %.
  - Detailed-terrain maps draw their rivers and roads as scene lines (editable), not from the stored world; a kind with no drawn line (the fixture's 하층 구역 지도 has one road and no river) falls back to the stored world's lines of that kind, which are not editable.
- **Stamps** (WLD-60): stroked glyphs; radius 0.016 / 0.024 / 0.032 of the map width for size 1 / 2 / 3, at least 7 / 10 / 13 dp on screen; `dim` at 40 %. Hit by the 44 dp target after pins, nodes and labels. Built stamps (`도시 · 마을 · 성 · 폐허 · 탑 · 다리 · 항구 · 표지석`) placed or moved within 0.015 of a river or road attach to it (`DetailEdits`); terrain stamps never do. New stamps default to `성`, size 2.
- **Stamp handles**: a top handle (rotation: the direction from the stamp to the tapped spot, 0° up, snapped to 15°) and a corner handle (size: the step whose corner is nearest the tapped spot); tap the handle, then the map, as for vertices.
- **`줄`** (Q-23): two taps paint the straight hex line between the cells (cube-coordinate line, both ends included), one step.
- **`좌표 표시`** choices: `없음 · 2칸마다 · 3칸마다 · 5칸마다`. **`방향`** keeps rows and columns and resets the scale from `칸 크기`.
- **Polygon provinces** without a name read `지역 N` (their place in the list), as maps read `지도 N` and regions `영역 N`. A new province (`새 지역`, `나누기`) is named `지역 N` in the next `map-art` colour. `합치기`: the tapped province joins the selected one. `나누기`: chosen cells of the selected province form the new one; it needs at least one cell left behind.
- **WLD-56 seeds**: a re-rolled step gets a random 4-digit seed. A step without author edits re-rolls at once (no sheet). `지금 자리` in `위치 확인` moves the stamp to the nearest point of the new line; `원래 자리` keeps it.
- **WLD-57 entry**: the chip `그림 · <style>` (DS `LayerFlagChip`) on 지도 보기 for generated `상세 지형` and `다각형` maps only (hex and outline maps have fixed colours); `등고선` only on `상세 지형`.
- **WLD-61**: the text is read 0.5 s after the last change, and again when the engine or a source changes. `함께 읽기` offers `장소 N` (live, unhidden places) and one chip per live, unhidden page with text, all on; a page's text is read with the typed text (its conditions carry no underline). Factions are not offered: conditions link places only. The engine line reads `키워드` (domain `TextConditions.readLine`) while no model exists. The result map is a `상세 지형` · `색면` world; `다른 후보 3` shows three seed chips (`시드 0031`). The condition editor's 어디 grid is nine buttons `1 … 9` (north row first).
- **WLD-62**: the shown map is the route's, else the map where most of the focused character's stops have their own pin or region (ancestor pins break ties), else the first top-level map. A stop whose place is on a child map stands at the ancestor pin on the shown map; a region at the middle of its shape. Chip taps: add and focus; tap the focused one to remove it; tap another chosen one to focus it. Play steps one episode every 0.9 s.
- **WLD-65**: the two buttons appear when `LensEdit.choice` gives them (a past lens, or `◆` history at 최신); otherwise the base shape moves at once. `비교` compares the shape in force with one `◆` shape of the history.
- **Map delete**: `⋯ › 지우기` on 지도 보기 and 노드 지도 (not in the editors), no confirmation (the trash toast has `되돌리기`), as other deletes to the trash.
- **Region colours**: a token `map-art-N` maps to DS `mapArt[N-1]`, `#RRGGBB` is used as given, and any other token (`map-art-ash`, `map-art-red` in the fixture) gets a stable palette slot from its text.
- **Undo**: the toast text is the step label with its subject, built from source nouns only (`핀 · 결사의 문`, `핀 옮기기 · 결사의 문`, `영역 · <name>`, `경로 그리기`, `경로 · <title>`, `라벨 · <text>`, `레이어 · <name>`, `보이는 시점 · 1-7부터`, `새 지도`, `지도 이미지 바꾸기`). The edit header also has a persistent `되돌리기` icon.
- **Picker search** placeholder: the shared `초성 검색` (core:ui `PickerCopy`). Sheet title `장소 고르기` (the WLD-52 name). Label text field: `라벨`. `없음` for an unset `보이는 시점`, `레이어` or `연결`.
- **Pin tap with a child map** (Q-02): it opens the place sheet, which lists `하위 지도 | <name> ›`.
- **Place parent label** (Q-03): `지역` on the inner card. The cover sheet shows the domain eyebrow `장소 · <parent>`.
- **Region kind** (Q-17): `세력 영역 · 지역 · 기타`, default `세력 영역`. Default colour `map-art-1`. An empty name becomes `영역 N`.
- **`보이는 시점`** is stored as `PointRef.unitStart(unit)` and shown as `<code>부터`. `없음` clears it.
- **New-map sources**: `이미지 가져오기` opens `갤러리 · 파일` buttons under the row; `스케치 촬영` opens `촬영 · 갤러리`. These are fragments of the row's own small line.
- **Large image** (WLD-38 proposed threshold): > 4096 px long edge → overview ≤ 2048 px plus 512 px tiles. A straightened sketch is capped at 4096 px on its long edge. Camera photos are decoded upright (EXIF).
- **Sketch corners** start 8 % inside the photo.
- **`곡선 라벨`** is offered on DETAIL maps only; it starts as an arch 0.16 wide around the label.
- **Pick-mode map order**: the scene's map, else the first top-level map; node maps are excluded.

- **Map menu `⋯`** (WLD-46, 49, 51): one sheet `지도 메뉴` with `축척 맞추기 · 시점별 공개 · 지도 내보내기`. The spec names only `⋯ › 지도 내보내기` (`[PROPOSED]`); the other two need a visible way in.
- **Scale units** (Q-19): `리` = 0.4 km and `직접` has no km (domain:world defaults). The scale bar shows 1 or 2 days, whichever is closer to the 96 dp bar at the zoom.
- **Scale card position on inner** (`[PROPOSED]`): a 320 dp glass card at the top right.
- **Travel mode form**: `이름`, `하루 이동` (km) and `하루` (시간), built from `하루 이동 | 도보 15 km` (WLD-46) and rule 18's `도보 15 km · 하루 8시간`. Hours must be > 0 and ≤ 24; names are unique.
- **Reveal scrubber** (WLD-49): one stop per live episode (scenes in a one-level structure). A stop sets the **project lens** (`LensController.set`), so the lens chip, 지도 보기 and every other lens screen follow; `최신으로` in the lens sheet goes back.
- **`숨은 것 흐리게`** is stored per map (`WorldMap.revealViewDim`) and is one undo step.
- **Node maps**: a connection between the same two nodes is refused (either direction); nodes have no layer (the model has none); the `장소` link takes places only. Read-only projects only select on a node map, with the toast `읽기 전용`.
- **`지도 내보내기`**: the image is what the screen draws at the lens, including 지도 보기's layer chip filter. The share sheet has no title (none is given in the sources).
- **Generator flow**: the first style is the app setting `새 지도 그림`; the seeds are 4-digit (`0031`); the `노드` card's `단계별로 만들기` makes the node map at once; a candidate keeps the `종류` and `땅 비율` it was made with; `이 지도로 시작` replaces the flow on the back stack with the new map.

## Spec conflicts

- **WLD-61 board vs stub engine**: board `MapText` reads `읽어 낸 조건 · 4 | 기기 안 모델`; with no model bundled the line reads the domain's `키워드` (the keyword reader is the working on-device reader).
- **WLD-60 rotation**: board `MMapStamp` describes a drag on the top handle (`손을 떼면 가까운 15°로`); the editor uses the same tap-then-place pattern as every other handle (no drag anywhere in the editor, WLD-40).

- **Route duration**: boards `MapEdit`, `MapRouteC` and `TourMap` show `2.5일` and `도보 15 km/일`. The spec (rule 18 §3, WLD-43) retires decimal days: `2일 4시간` and `도보 15 km · 8시간`. The spec is followed.
- **`MapEdit` is not a `LensScreen`**, yet WLD-40 and the board `MapEdit` draw the lens chip. The screen shows the chip anyway; it opens `LensSheet`.
- **Cover editing tools** (Q-16): the planning doc limits cover editing; the cover boards show the full bar. The full bar is used.
- **Inner place card** (board `Map`) shows `소속 | 하층 구역`, while `MapSoft` shows `지역 | 하층 구역`. `지역` is used (Q-03 default).
- **`NodeMap` is not a `LensScreen`**, yet the board `MapNode` draws `최신 · 1-7`. The screen shows the chip (same as `MapEdit`).
- **Board `MapReveal` vs spec WLD-49 wording**: the spec calls `보이는 것만 | 숨은 것 흐리게` mode chips; the board draws the text `1-5 시점 · 보이는 것만` and a `숨은 것 흐리게` switch. The board is followed.
- **WLD-T56 vs fixture** (fixture conflict 19): 송전탑 폐허's pin is on 세계 지도, so 하층 구역 지도 at 1-5 reads `아직 공개 전 · 1`, not 2. `RevealStateTest` asserts the fixture value.
- **Map opens fitted vs boards "fill"**: the boards draw maps filling the view; QA 9 saw 296 %. `Viewport.UNSET` resolves to zoom 1 (fitted). Spec owner decides.
- **Toast colours in dark mode vs QA 9**: DS-27 gives the toast `ink` fill and `bg` text, so it is light on dark; QA calls it wrong. Not a map matter (DS is frozen).

## Requests

0.1.0 motion wave:
- **core:model `AppSettings.PaneSplit.Modules`**: a map id, if the orchestrator wants the inner `지형 만들기` steps panel (now a fixed 260 dp) on the shared split handle.
- **core:designsystem `FormatTile`**: a width parameter (it is a fixed 166 dp), so the format grid can stretch instead of spreading the leftover width between columns.
- **core:designsystem tokens**: a name for the `인물 동선` play step (900 ms; the module uses `MapMotion.PLAY_STEP_MS`), and for the 60 ms mark stagger (the module uses `CivilightMotion.searchBoxStaggerMs`).
- **core:designsystem toast (QA 9)**: decide whether the toast stays `ink` on `bg` (DS-27) in dark mode.
- **core:designsystem**: an indeterminate progress bar for `DelayedProgress` (map export and base replace have no fraction).
- **domain:world `gen` / `canvas` raster**: per-layer rasters of a generated world, for the 1.2 s step-order draw and the per-layer re-roll flow (`mapGenerate`, `mapReroll`).
- **Copy to confirm** (picked from the nearest existing words, replacing the `TODO_COPY_MAP_*` constants): `REROLL_OTHER_VERSION` `다른 버전 지도 · 다시 만들기 안 됨`, `TRACK_PLAY` `재생`, `TRACK_PAUSE` `멈춤`, `TEXT_UNREADABLE` `읽어 낼 수 없음 · 기기 안 모델 없음 · 클라우드 꺼짐`, `TRACK_EMPTY` `동선 없음 · 인물 고르기`, `IMPORT_FAILED` `이미지 가져오기 · 안 됨`, `LAYER_UP` `위로`, `LAYER_DOWN` `아래로`, `NODE_SUB` `부제`, `EXPORT_FAILED` `이미지 내보내기 · 안 됨`. The old "Copy needed (TODO_COPY)" lists below are answered by this.

Batch 3:
- **Copy needed (TODO_COPY)**, each a constant whose value is its own name:
  - `TODO_COPY_MAP_REROLL_OTHER_VERSION`: why `단계 다시 만들기` is off for a map from another generator version (WLD-53 never changes a stored map silently).
  - `TODO_COPY_MAP_TRACK_PLAY`: accessibility label of the 인물 동선 play button (WLD-62 `can play`).
  - `TODO_COPY_MAP_TRACK_EMPTY`: 인물 동선 with no character chosen or no stop.
  - `TODO_COPY_MAP_TEXT_UNREADABLE`: 글로 설명 when no reader can read the text (cloud off, no on-device model).
- **Copy to confirm**: `지역 N` (unnamed provinces), `칸 N` (split bar), `도장 · <kind>` and `칸 칠하기 · O09` undo labels, `원래 자리 · 지금 자리` (spec `[PROPOSED]` row), the `좌표 표시` choices, `지우기` for a condition and a map, `1-9 ◆ · 비교` chips, `모양` (region card row).
- **Cast lane (CST profile) / story lane**: push `CharacterTrack(p, characterKey = <key>)` where the profile or a scene offers `인물 동선`; the screen starts with that character.
- **World lane (`domain:world` `gen/`, not editable here)**: `StepReroll.apply` replaces hex cells, result layers and drawn lines only; a POLY map's `poly` cells and a DETAIL map's generated stamps stay from the old world, and text-condition statuses are not re-evaluated after a re-roll. `track/CharacterTracks.scrubber()` exists, but this module uses `RevealState.episodes` for the scrubber so it matches 시점별 공개.
- **Library lane (설정 › 지도 · 이미지 · 연표)**: the row `지형 만들기 · 글로 설명 | 기기 안 · 클라우드` (WLD-61); this screen keeps its own engine choice per session until the setting exists.
- **FEATURES.md**: WLD-56 … 62 and 65 partial, WLD-66 done.

Earlier batches:

- **Copy needed (TODO_COPY)**, each a constant whose value is its own name:
  - `TODO_COPY_MAP_IMPORT_FAILED`: toast when an image or photo cannot be read or stored (new map, `지도 이미지 바꾸기`, sketch, `펴기`). Proposed: `이미지 가져오기 · 안 됨`.
  - `TODO_COPY_MAP_EXPORT_FAILED`: toast when a map image cannot be made or shared (WLD-51).
  - `TODO_COPY_MAP_NODE_SUB`: field and row label of a node's sub line (`세라 · 1-3`); the board shows the line, not its name.
  - `TODO_COPY_MAP_LAYER_UP` / `TODO_COPY_MAP_LAYER_DOWN`: accessibility labels of the layer order buttons (WLD-45 order is `[PROPOSED]`).
- **Copy to confirm** (composed from source fragments, not given verbatim): source buttons `갤러리 · 파일` and `촬영 · 갤러리` under the new-map rows; `지점 N · <km> km` on the route draft bar; `영역 N` as the default region name; `하위 지도 | <name> ›` on the place sheet; `하위 지도 · 만들기 · 고르기` on the pin card; the mode form labels `하루 이동` (km) and `하루` (시간); `지도 메뉴` (the `⋯` sheet title); `연결 · <node>` and `이동 · <node>` step bars; undo labels `축척 · <map>`, `이동 수단 · <name>`, `노드 · <name>`, `연결 · <a> → <b>`.
- **core:designsystem `ScaleBar`**: a width (or a fraction) parameter, so the bar can be drawn to scale at every zoom (WLD-46). Today it is a fixed 96 dp and the label picks the closer of 1 or 2 days.
- **World lane (`TravelModes` route) and library lane (프로젝트 설정 `이동 수단 · 바꾸기 ›`, WLD-47)**: feature modules cannot depend on each other, so this module's `이동 수단` sheet cannot be reused there. The writes are shared: `domain:world` `TravelModeEdits` (`ensureStored`, `add`, `update`, `parseNumber`), rows `Travel.modeLabel`. Either register `TravelModes` in feature:world over them, or core moves the sheet into core:ui.
- **World lane, "place summary and 16:9 map crop around the pin (mapKey, pinKey)"**: not possible as a feature:map composable, because feature:world cannot depend on feature:map. Options for core: a shared sheet owned by MAP (e.g. `MapPinPreviewSheet(p, mapKey, pinKey)`), or moving `canvas/` (pure `MapCanvas`, `MapScenes`, `MapBases`) into a core module both lanes can use.
- **Story lane (bulk 장소 정하기, WLD-52)**: the inner `장소별 씬 수` overlay needs the count per place of the selected scenes; `PickOnMap` carries only one `sceneCount`. If the bulk table needs it, add the scene keys (or a request key the map can read them from) to the route.
- **app / merged manifest check**: `feature/map/src/main/AndroidManifest.xml` declares `MapSketchFileProvider` (authority `${applicationId}.map.sketch`, not exported; cache paths `map_sketch/` for the WLD-39 camera and `map_export/` for WLD-51 shares). Please confirm it in the merged manifest when APK builds resume. If any module ever declares the `CAMERA` permission, `ACTION_IMAGE_CAPTURE` will need it granted at run time.
- **FEATURES.md**: mark WLD-36 … 55 as partial (this lane may not edit it).

## 0.2.0 integration (wave 2, orchestrator)

Applied: NodeMap command-bar mic removed (S-8); `지도 만들기` tour no longer offered (LIB-96), MAP tour copy has no `경로` step.
