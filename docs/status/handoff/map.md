# feature:map handoff

## batch 1

Module feature/map, package io.github.lenticualrstone.civilight.feature.map. Status file: docs/status/feature-map.md (Architecture section is authoritative).

MapEntries.kt registers MapList, MapView, MapEdit and PickOnMap. The other map routes still use shell placeholders. MAP owns no shared sheets.

Key pieces:
- common/MapViewModelBase.kt (MapScreenViewModel + MapEnv): commit/commitValue is the one write path, one update per user action plus the `되돌리기` toast. undoLast runs on appScope.
- common/MapCreation.kt: new maps and base swaps. Each also adds the Blob row to doc.assets.blobs.
- canvas/: one MapCanvas for every screen.
  - Viewport and ViewportMath are pure. Viewport.UNSET resolves to the fill zoom.
  - MapScenes.build(doc, map, lens, hiddenOpen, SceneMode, layerFilter) is the only source for drawing, lists and picks (rule 19 masking through MapReveal.at).
  - MapBases chooses the base: Image tiles, Generated raster/hex, Node or Blank. WLD-57 styles plug into MapBase.Generated.
  - MapHitTest gives 44 dp px targets.
  - CurveGeometry handles curved labels.
- edit/:
  - EditorState and Editor form a pure reducer saved as JSON in rememberSaveable. It covers tool, region and route drafts, pending pin and label spots, 핀 옮기기, the chosen vertex or handle, and the panel tab.
  - EditState builds the PropsUi per selection or draft.
  - MapEditViewModel wraps MapEdits.
  - EditSheets holds the feature-local sheet keys.
- To add the hex, polygon, detail or node tool sets: give each format its own tool enum or reducer, pass it to MapToolbar(tools=…), add a PropsUi case, and draw drafts through MapCanvas.extraDraw and marks.
- Stamps and DrawnLines are not drawn yet; add them to MapScene and OverlayCanvas for WLD-60.
- MapViewState already applies 보이는 시점 (with revealViewDim), so WLD-49's reveal view can reuse SceneMode.
- PickOnMap result contract: a model.story.PlaceRef sent with deps.results.send(requestKey, ref), then pop.
- Copy lives in MapCopy (CopyLint test). Undo labels are MapCopy.step(sourceNoun, subject).
- Build: tools/gw :feature:map:compileDebugKotlin :feature:map:testDebugUnitTest exits 0, 72 tests. No domain module was changed.

## batch 2

Built WLD-45 … 51 and 53 … 55 on batch 1's structure (status file Architecture is authoritative). An earlier cut-off attempt had left most of `scale/`, `reveal/`, `travel/`, `export/`, `gen/` and the node state; this batch reviewed and finished them, added the node map screen and all tests.

- Entries now also register `MapScale`, `MapReveal`, `NodeMap`, `MapFormatPicker`, `MapGenerate` and `SketchStraighten`. Left for batch 3: `HexPaint`, `PolygonRegions`, `DetailStamps`, `TextMap`, `CharacterTrack` (still placeholders).
- New packages: `scale/` (WLD-46), `travel/` (WLD-47 local sheet), `reveal/` (WLD-49), `node/` (WLD-50), `export/` (WLD-51, `rememberMapExporter`), `gen/` (WLD-53 … 55).
- `MapEditViewModel` is now `open`; `NodeMapViewModel` extends it. Format editors of batch 3 can do the same to reuse labels, layers and `보이는 시점` writes.
- `MapMoreSheet` (`view/`) is the one map menu (`축척 맞추기 · 시점별 공개 · 지도 내보내기`). Add WLD-56 `<단계> 다시 만들기` and WLD-57 `양식 바꾸기` entries there or in the edit header.
- `SceneLink` carries `aKey` / `bKey`; connection labels are drawn by `MapCanvas`.
- Rule 19: `ScaleEdit` snaps and names only pins of the drawn `MapScene` (like every pick); keep that for any new tap-to-pin logic.
- Domain (`domain:world` `map/`, additive): `TravelModeEdits`, `LayerOrder`, `MapNesting`, `NodeEdits`; tests `MapAuthoringTest`.
- `GenFlowState` keeps `historyKinds` / `historyRatios` parallel to the history, so a re-chosen candidate is the same world.
- Not built (motion): WLD-54 step-order drawing as progress, WLD-55 changed-layer animation, WLD-48 zoom in/out, WLD-49 unblur. The previews crossfade.
- Open requests: DS `ScaleBar` width; world/library reuse of travel modes; world's pin crop composable (cannot cross feature modules); TODO_COPY list in the status file.
- Build: `tools/gw :feature:map:compileDebugKotlin :feature:map:testDebugUnitTest :domain:world:test` exits 0; feature:map 120 tests (21 classes), domain:world 109.

## batch 3

Last batch of the lane: WLD-56 … 62, 65 and 66 built on batch 1–2's structure; the status file's Architecture section is authoritative.

- Entries now register every map route: `DetailStamps`, `HexPaint`, `PolygonRegions`, `TextMap` and `CharacterTrack` too. No placeholder is left.
- Ways in: the map menu `⋯` (pure `view/MapMenu.rows`, actions and sheets in `common/MapMenuHost`) on 지도 보기, the editors and 노드 지도: `이름`, the format editor, `단계 다시 만들기`, `인물 동선`, `지우기`. The `그림 · 색면` chip on 지도 보기 (WLD-57). Nobody outside this module pushes the batch-3 routes; the cast lane is asked to push `CharacterTrack(p, characterKey)`.
- Canvas: `GenRaster` (pure ARGB pictures for the four styles, polygon cells, contours, base lines) is shared by `MapCanvas` and `MapExport` through `baseRaster` / `baseLines`. `MapScene` has `lines` and `stamps`; `StampGlyphs` draws stamps in both renderers.
- Terrain hits (stamps, drawn lines) are opt-in (`MapCanvas(hitTerrain = true)`, only the detail editor), so they never shadow regions in other screens.
- Editor: `MapEditScreen` and `DetailStampsScreen` are one private `MapEditorScreen(detail)`; `EditTool.BASE` / `DETAIL`. WLD-65 vertex moves go through `edit/ShapeEdits` (`LensEdit.choice`) and `ShapeChoiceBar`.
- New packages: `hex/`, `poly/`, `text/`, `track/`, each a pure saveable reducer + UI builder + view model + screen.
- Domain (`domain:world` `map/`, additive): `HexEdits`, `DetailEdits`, tested by `MapFormatEditsTest`.
- Not built (motion): hex paint fade and fill spread, stamp drop and 15° haptic, changed-layer re-roll motion, format crossfade, text-condition underline. The language model and cloud readers of WLD-61 are labelled stubs (needsExternal).
- Open requests: TODO_COPY list (4 new), cast lane push for `CharacterTrack`, world lane `StepReroll` gaps (POLY cells, DETAIL stamps, condition statuses), library setting row for the 글로 설명 engine.
- Build: `tools/gw :feature:map:compileDebugKotlin :feature:map:testDebugUnitTest :domain:world:test` exits 0; feature:map 165 tests (29 classes), domain:world 123 (1 skipped by the world lane).
