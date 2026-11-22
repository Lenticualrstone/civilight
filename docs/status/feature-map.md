# feature:map

Package `io.github.lenticualrstone.civilight.feature.map`. Build and tests: `tools/gw :feature:map:compileDebugKotlin :feature:map:testDebugUnitTest` exits 0. There are 72 JVM tests in 12 classes, all passing. No domain module was changed. Nothing was run on a device or emulator, and no APK was built (the APK hold).

Batch 1 of 3 (2026-11-22): WLD-36, 37, 38, 39, 40, 41, 42, 43, 44 and 52.

## Architecture

**Entries** (`MapEntries.kt`, `@file:JvmName("MapEntries")`, `mapEntries(deps)`):
- Registered: `MapList`, `MapView`, `MapEdit` and `PickOnMap`.
- Every other map route keeps the shell placeholder until a later batch registers it: `MapScale`, `MapReveal`, `NodeMap`, `MapFormatPicker`, `MapGenerate`, `HexPaint`, `PolygonRegions`, `DetailStamps`, `TextMap` and `CharacterTrack`.
- This module owns no shared sheet: `Sheets.kt` names none with owner MAP. Every sheet here is a feature-local `@Serializable SheetKey`.

**Packages**

| Package | What |
| --- | --- |
| `MapCopy.kt` | All UI copy (one object, DECISIONS §3), checked by `MapCopyLintTest`. `BTN_` constants are buttons. Undo labels (also the toast text) are `MapCopy.step(<source noun>, <subject>)`, e.g. `핀 · 결사의 문`, `핀 옮기기 · 결사의 문`, `영역 · 상층 관문 구역`. Missing copy is a `TODO_COPY_MAP_…` constant whose value is its own name (see Requests). |
| `common/` | **`MapEnv`**: the app-scoped deps a view model may keep (sessions, unlock, clock, device id, blobs, toasts, app scope).<br>**`MapScreenViewModel`**: the base of every view model, built on `ProjectViewModel`. It provides `readOnly`, `hiddenOpen`, `editContext()` and `lensOf(doc)`. Its `commit(label) { doc -> doc? }` and `commitValue(label) { doc -> (doc, value)? }` are the one write path: one `update` per user action, then the `되돌리기` toast; a null result means refused, and nothing happens. A read-only project shows the toast `읽기 전용`. `undoLast()` runs on the app scope, so the toast's `되돌리기` still works after the screen is popped.<br>**`MapCreation`**: pure new-map and base-swap edits over `MapEdits`. They also add the base `Blob` row to `doc.assets.blobs`, because the package writer only exports catalogued blobs.<br>**`ScreenParts`**: `GlassCard` (consumes taps, so a tap on a card never reaches the map), `TopBarTapGuard` (the strip under the glass top bar), `blockMapTaps`, `CardHeader`, `CardRow` and `mapBack`. |
| `canvas/` | **One map surface for every screen.**<br>`Viewport`: centre and zoom, saved as a float array. `Viewport.UNSET` starts a screen at the fill zoom.<br>`ViewportMath`: pure map↔screen conversion, pinch around the centroid, pan, clamp, visible rect, `fillZoom` and `resolve`.<br>`MapScene` / `MapScenes.build(doc, map, lens, hiddenOpen, mode, layerFilter)`: the domain readings as drawables. It uses `MapReveal.at` for liveness, masking and layers, `RegionShapes.shapeAt` and `Routes.sheet(...).mapLabel`.<br>`MapBase` / `MapBases.of(map)`: `Image`, `Generated` (stored `gen-1` world raster with rivers and roads, or the map's hex cells), `Node` or `Blank`. WLD-57 styles plug in at `MapBase.Generated`.<br>`TilePlan`: the large-image plan.<br>`MapHitTest`: tap targets in px (44 dp; a pin's target is centred on its icon and covers its name tag).<br>`CurveGeometry`: curved labels.<br>`MapColors`: region tokens and terrain mapping.<br>`MapCanvas`: the composable (base, overlays, two-finger pan/zoom, taps with hit results; `extraDraw` and `marks` slots for drafts and handles). `BaseLayers` draws the bases. |
| `list/` | `MapListScreen` / `MapListViewModel` / `MapListState`, and `NewMapSheet` (`parentMapKey`, `parentPinKey` for WLD-48). |
| `view/` | `MapViewScreen` / `MapViewViewModel` / `MapViewState`, and `MovePinBar` (shared with edit). |
| `edit/` | `EditorState` + `Editor`: a pure, saveable reducer (tools, region and route drafts, pending pin and label spots, `핀 옮기기`, chosen vertex or curve handle, panel tab).<br>`EditState`: the pure UI builder (`PropsUi` per selection or draft, layers, `목록` rows).<br>`MapEditViewModel`: one commit per action over `MapEdits`.<br>`MapEditScreen`, `EditPanels` and `EditSheets` (local sheet keys `PinPlaceSheet`, `LabelTextSheet`, `RevealSheet`, `LayerSheet`, `FactionSheet`, `KindSheet`, `RegionNameSheet`).<br>`PinPick`: the rows of `핀 · 장소 고르기`. |
| `sketch/` | `Straighten` (pure corners and output size), `SketchImages` (camera target through the module's own `MapSketchFileProvider`, capture import, EXIF-upright decode, `Matrix.setPolyToPoly` straightening into a blob), and `StraightenScreen`. |
| `pick/` | `PickState` (pure) and `PickOnMapScreen` / `PickOnMapViewModel`. |

**Conventions for later batches**
- **Screen state that must survive fold or process death** is `rememberSaveable` in the screen: viewport arrays, selection, layer filter, the editor state as JSON, the sketch photo as `Blob` JSON. View models hold only the document and the writes.
- **New tools**: add an `EditTool` set per format, or a parallel reducer (hex `한 칸 · 줄 · 채우기 · 고르기 · 되돌리기`, polygon, detail, node). Feed it through `MapToolbar(tools = …)`. Use `MapCanvas.extraDraw` / `marks` for its drafts. Add a `PropsUi` case for its panel.
- **Drawing what a later batch owns**:
  - WLD-49 reveal view: `SceneMode` and `MapScenes` already apply `보이는 시점`, dimming in edit or with `revealViewDim`.
  - WLD-50 node maps: `MapScene.nodes` and `links` are already drawn.
  - WLD-60 stamps and hand-drawn lines: not drawn yet. Add `SceneStamp` / `SceneLine` to `MapScene` and draw them in `OverlayCanvas`.
- **Rule 19 masking**: every list, property panel and pick reads the `MapScene` (built through `MapReveal.at`), never `map.pins` / `map.regions` directly, so hidden places and factions stay out while locked.
- **`PickOnMap` contract**: the caller picks a request key and pushes `PickOnMap(p, requestKey, sceneKey?)`. It then collects `deps.results.results(requestKey)` and calls `consume(requestKey)`. The value is a `model.story.PlaceRef`: `PLACE` from a pin, `REGION` from a region in `지역` mode. The screen pops after `고르기`.
  - The first map shown is the one holding the scene's current place or region, else the first top-level map.
  - The mode starts as `지역` when the scene's place is a region.

## Done

None. Every feature in this batch has a screen, and none has been seen on a device yet. The pure parts with tests are listed under Partial.

## Partial

UI built and compiled, logic tested on the JVM, not seen running:

- **WLD-36 지도 목록 · 새 지도** (`list/`):
  - Header `<project> · 세계` / `지도` / `+`, section `지도 · N`, a row of 200 × 150 thumbnails (live `MapCanvas`) with name and `생성 · 핀 9 · 영역 4` / `세계 지도 › 핀 · 생성`.
  - Empty state, and the `새 지도` sheet with its four rows and the `상위` line when opened with a parent.
  - `지형 만들기` pushes `MapFormatPicker` (placeholder until WLD-54). `노드 지도` creates a NODE map and pushes `NodeMap` (placeholder until WLD-50).
  - Tests: `MapListStateTest`.
  - Left: nothing opens the sheet with a parent yet (WLD-48 `하위 지도 · 만들기`). `MapFormatPicker` cannot carry a parent (see Requests).
  - Left: maps cannot be renamed yet (names are authored, WLD-36). Maps and overlays cannot be deleted yet; when built, deletes go through `domain:trash` (`TrashOps` / `RestoreOps`, DECISIONS §1), not by removing rows here.
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
  - `펴기` shows four corner handles; each can be dragged, or tapped and then placed by tapping.
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
  - Left: the format tool sets (hex, polygon, detail, node: WLD-50, 58, 59, 60).
  - Left: the `레이어` tab only toggles visibility (WLD-45 adds the rest).
- **WLD-41 핀**:
  - The `핀` tool: a tap opens `장소 고르기` (`초성 검색`, `잿더미 시장 · 씬 4개`, `새 장소` → name → place and pin in one step, `숨긴 장소 N` lock row). Places already pinned on this map are left out (one pin per place per map).
  - Pin mark: DS `MapPin`, selected in `acc`, with a 44 dp hit test.
  - `핀 옮기기` (view and edit): the next tap sets the spot, `완료` commits, `취소` leaves it.
  - Properties: `보이는 시점` (unit picker, `1-7부터`, `없음`) and `레이어`.
  - Tests: `EditorTest`, `EditStateTest` (pin picker rows).
  - Left: the `하위 지도` property (WLD-48).
- **WLD-42 영역**:
  - Taps add vertices (DS `MapVertex`, the last one filled; dashed draft line). The bar reads `꼭짓점 N` with `되돌리기 · 닫기`; `닫기` is a button only and needs 3 vertices.
  - The panel `새 영역 · 그리는 중` sets name, `종류 | 세력 영역 · 지역 · 기타`, `연결 | 세력 · …`, `보이는 시점`, `레이어` and `색` (`map-art-1…5` swatches) while drawing. `닫기` commits it all in one step; an empty name becomes `영역 N`.
  - A selected region shows its vertex handles: tap a handle, then tap the map to move that vertex (one step). Its properties can be edited.
  - Tests: `EditorTest`, `EditStateTest`.
  - Left: the pop and haptic motion, the 200 ms fill, and editing a `◆` shape (WLD-65).
- **WLD-43 경로**:
  - Taps add points; a tap near a pin (0.02) snaps to it. The bar reads `지점 N · <km> km` with `되돌리기 · 완료`.
  - The route sheet shows title `결사의 문 → 상층 관문`, duration `2일 4시간`, `38 km · 지점 3` or `축척 없음` with `축척 맞추기` (pushes `MapScale`, a placeholder until WLD-46), mode chips `도보 15 km · 8시간 · 말 · 배 · 직접`, `보이는 시점` and `레이어`.
  - Choosing a mode stores `shownModeKey`; `직접` pushes `TravelModes` (world lane).
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
  - Left: the bulk `장소별 씬 수` overlay and a multi-scene count (see Requests). No lane consumes the result yet.

## Defaults used

- **Zoom 100 %** means the whole base fits the view (contain); zoom runs 100–800 %. Every map screen starts at the fill zoom. Map tap targets are 44 dp in screen pixels at every zoom. Pins, nodes and labels are hit first, then routes (within a third of the target), then regions.
- **Edit mode draws not-yet-revealed items dimmed**, so they can be edited.
- **View mode** at a past lens leaves those items out, or dims them when the map has `숨은 것 흐리게` (WLD-49).
- **Layer chips** `전체 핀 · <layer names>`: a layer chip shows only that layer's overlays (view-only filter, not saved). The boards' `하층 구역 · 상층` chips are read as these scope chips.
- **Generated bases** before WLD-57:
  - One pixel per generator cell, coloured by the `ter-*` table and smoothed, with coastline (`ink3`), rivers (`ter-sea`) and roads (`ink2`).
  - Maps with hex cells draw the hex grid instead.
  - Stamps and drawn lines are not drawn yet.
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

## Spec conflicts

- **Route duration**: boards `MapEdit`, `MapRouteC` and `TourMap` show `2.5일` and `도보 15 km/일`. The spec (rule 18 §3, WLD-43) retires decimal days: `2일 4시간` and `도보 15 km · 8시간`. The spec is followed.
- **`MapEdit` is not a `LensScreen`**, yet WLD-40 and the board `MapEdit` draw the lens chip. The screen shows the chip anyway; it opens `LensSheet`.
- **Cover editing tools** (Q-16): the planning doc limits cover editing; the cover boards show the full bar. The full bar is used.
- **Inner place card** (board `Map`) shows `소속 | 하층 구역`, while `MapSoft` shows `지역 | 하층 구역`. `지역` is used (Q-03 default).

## Requests

- **core:navigation `MapFormatPicker`**: add `parentMapKey` / `parentPinKey`, so `지형 만들기` from a pin's `새 지도` sheet keeps the `상위` line (WLD-48). Today the parent is dropped for generated maps.
- **core:navigation, optional**: a `SketchStraighten(p, photoHash, parentMapKey?, parentPinKey?)` route. `펴기` is currently drawn in place of the map list from saved screen state, which works but is not a back-stack entry.
- **app / merged manifest check**: `feature/map/src/main/AndroidManifest.xml` declares `MapSketchFileProvider` (authority `${applicationId}.map.sketch`, not exported, cache path `map_sketch/` only) for the WLD-39 camera hand-off. Please confirm it in the merged manifest when APK builds resume. If any module ever declares the `CAMERA` permission, `ACTION_IMAGE_CAPTURE` will need it granted at run time.
- **Story lane (consumer of `PickOnMap`)**: the result is a `model.story.PlaceRef` sent under the request key. The route has no scene count for bulk `장소 정하기` (`씬 4개 → …`). Add `sceneCount: Int` to `PickOnMap`, or a bulk variant, if the bulk table needs it.
- **Copy needed (TODO_COPY)**: `TODO_COPY_MAP_IMPORT_FAILED`, the toast when an image or photo cannot be read or stored (new map, `지도 이미지 바꾸기`, sketch, `펴기`). Proposed: `이미지 가져오기 · 안 됨`.
- **Copy to confirm** (composed from source fragments, not given verbatim): source buttons `갤러리 · 파일` and `촬영 · 갤러리` under the new-map rows; `지점 N · <km> km` on the route draft bar; `영역 N` as the default region name; `하위 지도 | <name> ›` on the place sheet.
- **FEATURES.md**: mark WLD-36 … 44 and WLD-52 as partial (this lane may not edit it).
