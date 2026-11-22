# feature:canvas handoff

## batch 1

Status file: docs/status/feature-canvas.md. Its Architecture section is the map.

Module feature/canvas, package io.github.lenticualrstone.civilight.feature.canvas.
- CanvasEntries.kt registers CanvasBoard, the shared LineLabelSheet and the feature-local LineStartSheet. Add PageList, PageDoc, ImageCrop, ImageSpecs, PresetImages, HoloExperiment and the shared ImagePickSheet there.
- CanvasCopy holds all strings (BTN_ = buttons, TODO_COPY_CANVAS_* = missing). CanvasCopyLintTest lints them by reflection, so new constants are checked automatically.
- common/CanvasViewModel: ProjectViewModel with apply/act(label, mergeKey) { doc, ctx -> } plus actApp. FeatureDeps.canvasEditContext() builds the context.
- graph/: the generic engine (no project types). GraphScene, GraphNode, GraphLine; GraphGeometry for hits, strokes, labels and arrows; GraphViewport; the GraphSurface composable with GraphSurfaceListener.
- edit/CanvasEdits: pure doc writes.
- board/: NodeLabels (the only place for node titles; it respects hidden-locked and trashed items) and BoardScene (canvas plus lens to a GraphScene, relation look from domain:cast RelationMap by relation key). BoardReducer, BoardUi and CanvasTool handle tools and selection. Also CanvasBoardViewModel, CanvasBoardScreen (cover sheets, inner popovers), PlaceItemSheet and CanvasChrome (tool bar, zoom, hint).
- label/: LineLabelModel (pair-based; writes only through domain:cast CanvasRelations.keepAsLine and makeRelation), LineLabelEditor (snapshot-state drafts per edge), and LineLabelContent (rows shared by the sheet and the popover, plus the start picker).
- node/: NodeEditModel and NodeEditRows (text held locally while open).
- Tests use CanvasFx (S0 = ExampleProject, S0-noRel).

Conventions: one undo step per action, named by its copy. Drags and pan/zoom are written once at their end. Read-only answers with the toast 읽기 전용. Cover screens use bottom sheets; inner screens use CanvasPopover anchored at screen points.

Unfinished threads:
- CNV-11: NodeLabels already resolves hidden and trashed items. Still missing: the ◇ mark and the deleted-item buttons.
- Canvas list and rename (Q-CV-01).
- No UI to remove a line (copy missing).
- canvasKey == null on LineLabelSheet.
- Group frame resize.

## batch 2

Built CNV-11 … CNV-16 and CNV-21 … CNV-24 on batch 1's structure. 76 JVM tests in 9 classes pass (`tools/gw :feature:canvas:compileDebugKotlin :feature:canvas:testDebugUnitTest`, exit 0). The status file's Architecture section is the map. It was rewritten for batch 2.

- The graph engine now lives in core:ui `ui.graph`. The module's own `graph/` package is gone, so import from core:ui.
- `common/RefFixes`: the one place for `되살리기` (`CharacterActions` / `RestoreOps`) and `이대로 두기` (`Fixes.keep`) on a reference to a deleted item. Canvas nodes (CNV-11) and page links (CNV-16) both use it.
- `page/`:
  - pure parts: `PageEdits` (writes), `PageLinks` (link resolution with hidden and deleted states), `PageViews` (row builder, source mark, `연결 N`, access), `PageListModel`;
  - screens: `PageDocScreen` (board `Page`; read mode is `PageBlockText`, edit mode is the shell `RichTextEditor`) and `PageListScreen`;
  - local sheets in `PageSheets.kt`. Copy is in `PageCopy`.
- `card/`: `CardRules` (metrics, snap, tick, parallax, `ViewChoice`, `CardInitial`), `SnapCardRow`, and the card composables in `CardParts`. `image/SlotImage` decodes an original with its crop; presets draw neutral `ShardArt`.

Conventions added: copy objects per package (`CanvasCopy`, `PageCopy`, `CardCopy`), all linted by `CanvasCopyLintTest`. A locked or trashed page shows nothing of itself: no name, body or header image.

Unfinished threads for batch 3 (CNV-25 … CNV-31, CNV-45):
- Register the shared `ImagePickSheet` in `CanvasEntries`; cast and world already open it.
- Pass `onPick` to `SlotImage` / `ImagePlaceholder` so `이미지 고르기` shows.
- Draw the presets in `SlotImage`; cast and world want a shared preset painter.
- Add the slot registry and `cropReadout` with T-CNV-30 … T-CNV-36.
- Add the ImageCrop / ImageSpecs / PresetImages / HoloExperiment entries, and the list ↔ card motion.

Older threads still open:
- Canvas list and rename (Q-CV-01).
- Removing a line in the UI (copy missing).
- `LineLabelSheet.forCharacters` (needs a cast write path without a canvas edge; unused so far).
- 묶음 resize.
- Nothing pushes `PageList` (request to feature:story).
- `card/` should move to core:ui for library and cast (request).

## batch 3 (last batch of the lane)

Built CNV-25 … CNV-31 and CNV-45, plus the line-removal button (CNV-07 loose end). 95 JVM tests in 11 classes pass (`tools/gw :feature:canvas:compileDebugKotlin :feature:canvas:testDebugUnitTest --no-configuration-cache`, exit 0). The status file's Architecture section is the map; its "Loose ends still open" list under Partial says what is left and why.

- `image/` pure parts: `ImageSlots` (slot registry), `CropMath` (`cropReadout`, rotate → mirror → rectangle like `SlotImages.decode`), `CropSession` (serializable editor state, one edit per ratio), `ImageEdits` / `ImageOwners` (one use per owner × slot, blobs once per hash, holo forced off where not allowed), `PresetImages` (ids shared with feature:cast), `HoloRules` + `HoloSensorGate`, `CropHandOff` (in-memory tile bounds and holo for the crop editor).
- `image/` composables: `PresetArt`, `HoloLayer` (`ProvideHoloTilt`, `rememberHoloTilt`, `holoDrag`, `holoFoilAt`, `holoSurface`), `SlotImage` now draws presets and holo, `MineThumb`; `pick/` (shared `ImagePickSheet` content, view model keyed per owner × slot), `crop/` (`ImageCrop` screen), `specs/` (`ImageSpecs`, `PresetImages`).
- `holo/HoloExperimentScreen` (P2, board sample content, buttons disabled for Q-IM-11).
- `card/CardViewMotion` + `PageListScreen` (`SharedTransitionLayout` list ↔ card; list rows got 48 dp thumbnails).
- Conventions added: `ImageCopy` and `HoloCopy` (in `CanvasCopyLintTest.objects`; a new copy object must be added there). Build with `--no-configuration-cache` (recorder lane's finding).

Open: Q-CV-01 canvas list, 묶음 resize, `forCharacters`, nobody pushes `PageList` / `ImageSpecs` / `PresetImages` / `HoloExperiment`, and `card/` + `image/` should move to core:ui so cast, world and library can share presets, holo and cards.
