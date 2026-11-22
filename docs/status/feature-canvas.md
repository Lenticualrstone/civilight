# feature:canvas

Batch 1 of 3 (CNV-01 … CNV-10). `tools/gw :feature:canvas:compileDebugKotlin :feature:canvas:testDebugUnitTest` exits 0: 51 JVM tests in 6 classes, all passing. No domain module was changed (DECISIONS §2: canvas logic lives in this module). Nothing was run on a device or emulator; every screen is checked only by compiling, so every UI feature below is at most partial.

## Architecture

Package `io.github.lenticualrstone.civilight.feature.canvas` (`src/main/java/...`). Composables stay thin: rules are pure Kotlin objects tested on `ExampleProject.build()` (fixture S0 and S0-noRel in `src/test/.../CanvasFx.kt`).

- **`CanvasEntries.kt`** (`canvasEntries`, shell.md §1): registers the route `CanvasBoard`, the shared sheet `LineLabelSheet`, and the feature-local `LineStartSheet` (registered too, because it is stacked over `LineLabelSheet` on other areas' screens). Not registered yet (shell placeholder): `PageList`, `PageDoc`, `ImageCrop`, `ImageSpecs`, `PresetImages`, `HoloExperiment`, shared `ImagePickSheet`.
- **`CanvasCopy`**: every UI string of this module; `BTN_` keys are buttons; missing copy = `TODO_COPY_CANVAS_*` (listed under Requests). `CanvasCopyLintTest` lints every value and the formatted helpers.
- **`common/CanvasViewModel`**: base on `ProjectViewModel`. `apply(label, mergeKey) { doc, ctx -> doc' }` (suspend) and `act(label, mergeKey, then) { … }` (one undo step named by the button or tool copy, `EditResult.READ_ONLY` in the example), `actApp { }` for `_app` state. `FeatureDeps.canvasEditContext()` builds the `EditContext` (shell clock, zone, device id, ULID keys).
- **`graph/`: the shared graph engine (CNV-01)**. No `ProjectDoc` or canvas types, so the cast area's 전체 관계도 can use it once it moves to a shared module (Requests).
  - `GraphModel.kt`: `Pt`, `Sz`, `Rect`, `GraphNode` (`NodeLook` CARD · MEMO · DOTTED · FRAME), `GraphLine` (`LineStroke`, `MiddleWord`, `EndWord`), `GraphSelection`, `GraphScene`.
  - `GraphGeometry`: node rects from measured sizes, the visible segment between two cards, label anchors (t 0.5 / 0.28 / 0.72), hit tests (cards, then lines within 22 dp on screen, then frames; card targets grow to 44 dp on screen), stroke spec (`ink3` 1.2 / `acc` 2.2 cover · 2 inner, dash 6 / 5), label texts with arrows taken from the drawn positions (`경계 →` / `← 보호`, `후배 ↔ 선배` left to right).
  - `GraphViewport`: `screen = content × zoom + offset` (dp), pan, zoom around a point, `+` / `−` steps, `fit`.
  - `GraphSurface` + `GraphGestures`: the composable. Content drawn in a `graphicsLayer` (frames, lines, cards, label pills), node sizes measured into a `SnapshotStateMap`; two fingers pan and pinch, one finger drags a node, a press without movement is a tap, a mouse wheel zooms; nodes and labels have click semantics for accessibility services. Callbacks in `GraphSurfaceListener`.
- **`edit/CanvasEdits`**: pure canvas writes: `resolve` (route canvas, else first), `createCanvas`, `addItemNode`, `addMemo`, `addGroup`, `moveNode` (a 묶음 carries members; a dropped card joins or leaves a frame), `setText`, `removeNode` (takes its lines, frees members), `addLine` (`LineResult` Created · Existing · Refused), `removeLine`. Relation writes are not here (see `label/`).
- **`board/`: the canvas screen (CNV-02 … CNV-07, CNV-09)**.
  - `NodeLabels`: one function for every node title, by key at read time: `ItemRefs.resolve` for 인물 · 세력 · 용어 (locked hidden → `숨긴 인물` / `숨긴 항목`, no kind line; trashed → `지운 인물 · 노아` dotted), events from the index, memo text, 묶음 title.
  - `BoardScene`: canvas + lens → `GraphScene`. Relation lines read `domain:cast` `RelationMap.build(doc, lens, hiddenOpen)` by relation key (`relationLines`, computed once per document and lens, not per drag frame); no map line → plain line with its label. `positionOf` applies on-screen drag positions (a dragged frame carries its members).
  - `BoardReducer` + `BoardUi` + `CanvasTool` + `BoardAction`: tool bar, selection, the first end of a line; taps become actions (`EditNode`, `EditLine`, `PickItem`, `AddMemo`, `AddGroup`, `DrawLine`, `ReadOnly`). `BoardUi.save/restore` for `rememberSaveable`.
  - `CanvasBoardViewModel`: one undo step per action; drags and pan/zoom are written once at their end (`move`, `saveViewport` into `_app/view.json` `canvasViewports`).
  - `CanvasBoardScreen`: `CivilightScreen` (back, `캔버스`, canvas name, lens chip → `LensSheet`) over `GraphSurface`; `ZoomColumn`, cover `CanvasHint`, `CanvasToolBar` (`CanvasChrome.kt`); local sheets `NodeEditSheet` and `PlaceItemSheet`; inner popovers (`CanvasPopover`, `InnerPopover`) for the selected line or node.
  - `PlaceItemSheet`: `인물` → shell `CharacterPickerSheet`; `노드` → segmented `세력 · 사건 · 용어` over `FactionPickerSheet`, `TermPickerSheet` and a local event list (`eventRows`, 초성 search).
- **`label/`: `선 라벨` (CNV-08)**. `LineLabelModel` is pair-based (`LineEnd` from, to + lens) so a relationship-map line can reuse it: `ends`, `initialDraft` (label text, `<drawn-from>의 마음`, start = `CanvasRelations.startUnit`), `withSlot` (kind list follows `사이` / 마음), `build` → `LineLabelView`, `keep` / `make` (the only writes, through `domain:cast` `CanvasRelations.keepAsLine` / `makeRelation`). `LineLabelEditor` (entry view model, key `canvas-line-label-<project>`) holds drafts by line so the `시작 시점` picker can stack over the sheet. `LineLabelContent.kt`: `LineLabelRows` (shared by the cover sheet and the inner popover), `LineLabelSheetContent` (shared sheet), `LineStartSheetContent` (shell `UnitPickerSheet`, episode and scene levels).
- **`node/`: node edit (CNV-10)**. `NodeEditModel` → `NodeEditView` (memo text or 묶음 title, open route `인물 기록` · `용어 카드` · `세력 카드` · `사건 상세`, remove); `NodeEditRows` shared by the cover `NodeEditSheet` and the inner popover.
- **How to extend (batches 2 and 3):**
  - CNV-11 hidden/deleted: `NodeLabels` already resolves both; add the `◇` mark and the deleted-item buttons in `NodeEditModel`/`NodeEditRows`.
  - CNV-12: the canvas file shape is the model's; `CanvasEdits` already keeps keys, z order keys, `updatedAt/By` and NFC text.
  - Pages (CNV-13 … CNV-16): a `page/` package with `PageDoc`/`PageList` entries in `CanvasEntries`.
  - Images (CNV-21 … CNV-31): an `image/` package; register `ImagePickSheet` in `CanvasEntries`; `ImageCrop`, `ImageSpecs`, `PresetImages`, `HoloExperiment` entries.
  - A new canvas action: a `CanvasEdits` function + test, a `CanvasBoardViewModel` method, a `BoardAction` if a tap starts it.

## Done

Pure logic with passing tests (the screens on top are listed under Partial):

- **CNV-01 engine logic** (`GraphAndToolsTest`): viewport round trip and zoom around a point, zoom range, `fit`, hit order and 44 dp targets at 25 %, visible segment and label anchors, pair words and arrows from positions.
- **CNV-09 relation lines** (`RelationLinesTest`): T-CNV-01, T-CNV-02, T-CNV-03, T-CNV-05, T-CNV-06, T-CNV-07, T-CNV-08, T-CNV-58, T-CNV-16 and the canvas part of T-CNV-15; the canvas variant of T-CNV-04 (plain line at 1-6); arrows follow positions; a locked hidden end draws no relation labels.
- **CNV-08 label model** (`LineLabelModelTest`): T-CNV-09 (both directions), T-CNV-10 (four non-character lines), T-CNV-11, T-CNV-12, T-CNV-13, T-CNV-14; `사이` switches the kind list; `관계로 만들기` waits for a kind and is off when read-only; an already converted line reuses its relation; a locked hidden character cannot be named or related; draft save/restore.
- **CNV-02/04/05/06/07/10 edits** (`CanvasEditsTest`): route resolution and first canvas, item nodes store only kind + key and follow renames, the six sample nodes read as the board, memo text, 묶음 membership and carrying, tap–tap lines (no self-line, one per pair, none to a frame), removing a converted line keeps the relation, removing a node takes its lines but never the item, node edit targets and read-only, event search with 초성.
- **Sheet keys** (`SheetKeysTest`): `NodeEditSheet`, `PlaceItemSheet`, `LineStartSheet`, `LineLabelSheet` round-trip through `RouteCodec` (saved sheet stack, CNV-35); nodes are measured with unbounded constraints.
- **CNV-03 tool rules** (`GraphAndToolsTest`): one current tool (`노드` first), only it acts on empty space, read-only answers `읽기 전용`, `선` tap–tap with cancel, tool change drops a started line, saved-state round trip.

## Partial

All UI features below compile and are wired to the store; none has been seen on a device.

- **CNV-01 그래프 엔진**: surface, gestures and drawing built. Not checked on a device: gesture feel, text sharpness at 200 % (content is scaled as a layer), glass of the tool bar and zoom column (they draw the SCRIM fill, see Defaults). The 전체 관계도 cannot use it until the package moves (Requests).
- **CNV-02 캔버스 문서**: header, lens chip, viewport saved per canvas, `CanvasBoard(canvasKey)` resolution, first canvas made when a writable project has none. Left: listing, switching, renaming and deleting canvases (Q-CV-01, nothing drawn); the home shortcut opening the most recently opened canvas (needs a per-canvas `lastOpenedAt`, Requests).
- **CNV-03 도구 줄**: cover five columns and inner pill, `pri` current tool. Done in logic; not seen on a device.
- **CNV-04 항목 노드**: placing 인물 (shared picker) and 세력 · 사건 · 용어 (segmented picker), current names, node edit on tap. Left: Q-CV-04 (장소), Q-CV-05 (lens on item nodes; plain names are shown).
- **CNV-05 메모 노드**: create with `메모`, edit text in node edit (one undo step per node while typing).
- **CNV-06 묶음**: [PROPOSED] data shape only: a fixed 260 × 180 frame with a title, members join by dropping a card inside, the frame carries them. Left: resizing, any drawn design (Q-CV-06).
- **CNV-07 선 긋기**: tap–tap with the `선` tool; the label surface opens on the new or existing line.
- **CNV-08 선 라벨**: cover sheet (shared `LineLabelSheet`) and inner popover with the same rows; `시작 시점` picker; `+ 새 종류` opens cast's `RelationKindSettings`, which feature:cast has not registered yet, so today it lands on the shell placeholder. Left: `canvasKey == null` (relationship-map lines) shows only the title row until the key contract is settled (Requests); a line cannot be removed from the canvas in the UI yet (copy missing, `removeLine` exists and is tested).
- **CNV-09 관계 선과 시점 렌즈**: done in logic; drawing not seen on a device.
- **CNV-10 노드 편집**: cover sheet and inner popover; open the item, edit memo/묶음 text, remove (label is `TODO_COPY_CANVAS_REMOVE_NODE`).

## Defaults used

- **`노드` tool** (Q-CV-02): places a 세력 · 사건 · 용어 item (segmented `세력 · 사건 · 용어` above the picker). Taps on nodes and lines select and open their surface with every tool except `선`. Default tool `노드` (cover board idle state).
- **Empty-space taps**: the first tap away from a selection only clears it; otherwise the current tool acts. One finger on empty space never pans (CNV-01 gestures are two-finger); a mouse wheel zooms.
- **Zoom** (Q-CV-07): 25 % … 200 %, ×1.25 per button, around the view centre; the zoom column is shown on cover and inner (the inner board draws none, but the buttons are the visible alternative to pinching). First open fits the canvas between the bars, never past 100 %.
- **Lines** (Q-CV-08): tap A then tap B; no self-line; one line per unordered pair (the existing one opens instead); no line to a 묶음 frame; the label surface opens right after.
- **Label surface** (Q-CV-09 … Q-CV-11): no kind is preselected and `관계로 만들기` is off until one is picked; the start is the lens episode (`최신` = the 집필 선 episode) and can be changed to any episode or scene; the label text stays on the line and is not stored as the cell's 설명; `닫기` drops the draft (typed text is not kept). A draft closed by tapping the scrim is kept for this screen visit. An already converted line opens the same rows and adds a record to its relation. Pair 사이 kinds show as `선배 ↔ 후배` chips; the direction follows `domain:cast` (the drawn-to end holds the first name).
- **Converted line without a value at the lens** (Q-CV-12, CNV-09 [PROPOSED]): plain grey line with its label text. **Deleting a converted line** (Q-CV-13): removes only the canvas line.
- **Hidden and deleted items** (CNV-11 [PROPOSED], ahead of batch 2): a locked hidden item reads `숨긴 인물` / `숨긴 항목` with no kind line, cannot be opened, and its relation lines are plain; a trashed item reads `지운 인물 · 노아` with the dotted (dashed outline) look.
- **No canvas** (CNV-02 [PROPOSED]): a writable project gets one named `캔버스` (no source names it); a read-only project shows `TODO_COPY_CANVAS_NONE`.
- **Read-only (the example)**: tools stay visible; creating, dragging and the sheet buttons do nothing and say `읽기 전용` (toast or line in the sheet); pan, zoom and the lens still work (app state).
- **Glass over the surface**: the tool bar and zoom column use the SCRIM fill (`GlassBackground(source = null)`), because they are drawn inside the scaffold's content, which has no glass source of its own. Inner popovers use the SCRIM fill too (DS-26: a popup cannot sample the backdrop).
- **Item nodes and the lens** (Q-CV-05): plain current names (board `하르트` at `최신 · 1-7`).
- **Undo step names**: the tool or button copy (`노드` also for removing a node, `메모`, `묶음`, `선`, `그냥 선으로 두기`, `관계로 만들기`, `노드는 끌어서 이동` for a move).
- **Text fields**: memo / 묶음 text is held in the surface while it is open and written through with one merged undo step per node; line-label drafts are snapshot state in `LineLabelEditor`, so typing never waits for the store. Cards wrap their text at 240 dp.
- **Canvas viewport**: `_app/view.json` `canvasViewports` (offset in dp + zoom), saved at the end of each pan, pinch or button press; never exported.

## Spec conflicts

- None found between the spec cases and the fixture for CNV-01 … CNV-10. T-CNV-04 and T-CNV-07 are written for the relationship map (cast-owned); here they are tested as their canvas counterparts (plain line, CNV-09 [PROPOSED]).

## Requests

- **Move `feature/canvas/.../graph/` to a shared module** (core:ui or a new `core:graph`): it is the CNV-01 engine the cast area's 전체 관계도 must share, and feature:cast cannot depend on feature:canvas. It has no project types, only DS and Compose.
- **core:navigation `LineLabelSheet`**: for relationship-map lines (`canvasKey == null`) the sheet needs the two character keys (e.g. `fromCharacterKey`, `toCharacterKey`) instead of an `edgeKey`; `LineLabelModel` is already pair-based. Until then that case shows only the title row.
- **core:model**: a per-canvas `lastOpenedAt` (or `ProjectViewFile.lastCanvasKey`) for CNV-02's "open the most recently opened canvas".
- **core:ui picker**: an event kind in `PickerModels` / `ItemPickerSheet` (`PickKind.EVENT`), so `사건 고르기` can use the shared picker; a local list is used now.
- **core:designsystem**: `IconId.Minus` (`M5 12h14`) for the zoom column; a local vector is used now. A dotted-outline variant of `NodeCard` with the `◇` mark for deleted references (CNV-11).
- **Copy needed (TODO_COPY)**: `TODO_COPY_CANVAS_ZOOM_OUT` (accessibility label of `−`), `TODO_COPY_CANVAS_REMOVE_NODE` (node edit: take the node off this canvas; also the undo step), `TODO_COPY_CANVAS_REMOVE_LINE` (not shown yet: removing a line from the canvas), `TODO_COPY_CANVAS_NONE` (read-only project without a canvas, Q-EM-01). The name of a canvas made automatically (now `캔버스`).
- **FEATURES.md**: rows CNV-01 … CNV-10 → partial (this module may not edit it).
