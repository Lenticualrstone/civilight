# feature:canvas

All three batches are done: batch 1 built CNV-01 … CNV-10, batch 2 built CNV-11 … CNV-16 and CNV-21 … CNV-24, batch 3 built CNV-25 … CNV-31 and CNV-45 and closed the line-removal loose end.

**0.1.0 wave, step 1 of 3 (motion audit §4 canvas, QA 2, 3, 4, 10):** done and described under "Motion wave" below. `tools/gw :feature:canvas:compileDebugKotlin :feature:canvas:testDebugUnitTest --no-configuration-cache` exits 0 with 114 JVM tests in 13 classes, all passing. No domain module was changed. Nothing was run on a device or emulator, so every UI item stays "partial" until the orchestrator's emulator pass. Steps 2 and 3 (the `Gallery` route, its three sheets and `GalleryCard`) are not started: this step only removed the local card copies the gallery will use from the DS.

**0.1.0 wave, step 2 of 3 (gallery part 1: GAL-04 … GAL-09, GAL-23 … GAL-25):** built and described under "Gallery part 1" below. `tools/gw :feature:canvas:compileDebugKotlin :feature:canvas:testDebugUnitTest` exits 0 with 139 JVM tests in 14 classes, all passing (25 new in `GalleryScreenRulesTest`). No domain module was changed. Every gallery screen item is **partial** until the orchestrator's emulator pass.

**0.1.0 wave, step 3 of 3 (gallery part 2: GAL-10 … GAL-22, the three shared sheets, `사진`):** built and described under "Gallery part 2" below. `tools/gw :feature:canvas:compileDebugKotlin :feature:canvas:testDebugUnitTest` exits 0 with 159 JVM tests in 15 classes, all passing (20 new in `GalleryEditRulesTest`). No domain module or shared module was changed. Every gallery screen item stays **partial** until the orchestrator's emulator pass.

**0.2.0 wave 2 (canvas lane):** `tools/gw :feature:canvas:compileDebugKotlin :feature:canvas:testDebugUnitTest` exits 0 with 211 JVM tests in 22 classes, all passing (14 new: `CanvasListTest` 9, `HoloDefaultTest` 3, `CanvasIconButtonsTest` 2). No shared or domain module was changed. Nothing was run on an emulator, so every item below is **partial**.

- **N-58 several canvases:** the canvas name `▾` in the head opens the list sheet (`캔버스`: `＋ 새 캔버스` row, live canvases with `노드 N`, the open one marked, row `⋯`). The entry is the same with one canvas. The row `⋯` has `이름 바꾸기 · 지우기`, drawn like the page menu (S-6, D14). `지우기` shows only while another live canvas remains. Every write goes through `CanvasActions` (`create` · `rename` · `delete` · `restore` · `opened` · `openTarget`). A delete shows the trash toast, and its `되돌리기` restores that trash entry. The restore runs on the app scope with its own project session (`CanvasWrites`), so it still works after the user leaves the canvas screen while the toast is up (toasts outlive a popped screen, DS-67); the toast belongs to the project (`showFor`) and leaves with it. Home `캔버스` (`CanvasBoard(p)`, no key) opens `CanvasActions.openTarget`: the last opened canvas while it is live, else the first one. Read-only projects show the rows only.
- **U-15 pages and canvases:** the page list's head `+` is replaced by the top row `＋ 새 페이지` (shown with 0 pages too, left out when read-only). It opens the shared create card (`이름`) with `취소 · 만들고 열기 · 만들기`. `만들기` stays on the list, scrolls the new row (list) or card (card view) into view and flashes it with `hl` for `createdRowHighlightMs` (feature-local `createdFlash`, not a selection). `만들고 열기` opens the page straight into editing (feature-local `JustCreatedPages` mark read once by the page screen, `PageAutoEdit`), as the head `+` did in 0.1.0 (CNV-14). Canvases use the same card from the list sheet; the new canvas row flashes the same way and only the open canvas is drawn as `current`.
- **S-7 holo:** a new image has holo off. `AppSettings.holoDefaultForNewImages` defaults to false, and `GalleryActions.add` defaults to false. The `홀로그램 효과 기본` setting row still exists, and a user who turned it on in 0.1.0 keeps it (scope §8). Images that had holo keep it, and holo is set per image.
- **U-03 / U-11:** checked on the JVM and in code only. The foil draws only where `item.holo` is on. Under reduced motion the tilt is `STILL`, with no sweep and no sensor. The shelf card fits between the bars (`CardFit`, tested in `core:designsystem` `CardFitTest`), and the tilt moves the foil only, never the card's geometry, clipped to the card. Not checked on an emulator.
- **ICON:** gallery viewer `이전` · `다음` (a, off at an end), `설명` (b toggle with state), `고치기` (a). Picked-card panel `보기` · `고치기` (a, next to `⋯`); `설명 쓰기` stays text. `여러 장 고르기` (b `고르기`), `다시 잠그기` (b), `맨 앞` · `맨 뒤` (b icons on the existing buttons), `지문으로 보기` on the locked card and the node sheet (b). Viewer close uses `IconButtonSpec.CLOSE`. `CanvasIconButtonsTest` runs `IconButtonLint` over the module's main sources.

## Architecture

The package is `io.github.lenticualrstone.civilight.feature.canvas` (`src/main/java/...`). Composables stay thin. The rules are pure Kotlin objects, tested on `ExampleProject.build()` with fixtures S0 and S0-noRel in `src/test/.../CanvasFx.kt`.

- **`CanvasEntries.kt`** (`canvasEntries`, shell.md §1)
  - Routes: `CanvasBoard`, `PageList`, `PageDoc`, `ImageCrop`, `ImageSpecs`, `PresetImages`, `HoloExperiment`, `Gallery` (every route of the canvas row of shell.md §3).
  - Shared sheets: `LineLabelSheet`, `ImagePickSheet` (both CANVAS-owned sheets of shell.md §5).
  - Feature-local `LineStartSheet`. It is registered because it is stacked over `LineLabelSheet` on other areas' screens.
- **Copy.** There is one object per package: `CanvasCopy` (canvas), `page/PageCopy` (pages), `card/CardCopy` (cards), `image/ImageCopy` (images), `holo/HoloCopy` (holo experiment; `SAMPLE_` = board sample content) and `gallery/GalleryUiCopy` (gallery screen and viewer).
  - `BTN_` keys are buttons. Copy no source gives is the nearest existing wording, listed under Requests (no `TODO_COPY_*` string may remain; a test checks).
  - `CanvasCopyLintTest` lints every value of all five objects by reflection, plus the formatted helpers. A new constant in one of them is checked automatically; **a new copy object must be added to its `objects` list**.
- **`common/`**
  - `CanvasViewModel`: the base on `ProjectViewModel`.
    - `apply(label, mergeKey) { doc, ctx -> doc' }` (suspend) and `act(label, mergeKey, then) { … }` make one undo step named by the button or tool copy. They answer `EditResult.READ_ONLY` in the example.
    - `actApp { }` changes `_app` state.
    - `FeatureDeps.canvasEditContext()` builds the `EditContext`.
  - `RefFixes`: the buttons of a reference to a deleted item, shared by canvas item nodes (CNV-11) and page links (CNV-16).
    - `restore` goes through `CharacterActions.restore` for characters and `RestoreOps.restore` by trash entry for everything else. It returns `RestoreResult(doc, toast, needsAuth)`.
    - `keep` uses `Fixes.keep`. Also `isKept` and `inTrash`.
- **Graph engine.** It lives in core:ui `ui.graph` since the core pass. This module only imports it, and the old `graph/` package is deleted. A `NodeLook.DOTTED` node draws DS `NodeCard(deleted = true)` (dotted, `◇`).
- **`edit/CanvasEdits`**: pure canvas writes.
  - `resolve` (route canvas, else the first).
  - `resolveForRoute`: `CanvasBoard(canvasKey = null)` opens `_app/view.json` `lastCanvasKey`, else the first live canvas.
  - `createCanvas`, `addItemNode`, `addMemo`, `addGroup`.
  - `moveNode`: a 묶음 carries its members, and a dropped card joins or leaves a frame.
  - `setText`, `removeNode`.
  - `addLine` (`Created · Existing · Refused`), `removeLine`.
- **`board/`: the canvas screen**
  - `NodePlacement` (pure, JVM-tested): where a new node goes, centred on the middle of the view and moved to the nearest free spot (QA #10).
  - `NodeLabels`: the only place that builds node titles. Characters, factions and terms resolve through `ItemRefs.resolve`; events through the index; memos and 묶음 from their text.
    - A locked hidden item reads `숨긴 인물` / `숨긴 항목`.
    - A trashed item reads `지운 인물 · 노아` with `NodeLook.DOTTED`.
    - A name tag reads `영구 삭제 · 노아`.
  - `BoardScene`: canvas plus lens gives a `GraphScene`. Relation lines read `domain:cast` `RelationMap` by relation key, so a relation in the trash draws a plain line.
  - `BoardReducer`, `BoardUi`, `CanvasTool` and `BoardAction`: tools and selection.
  - `CanvasBoardViewModel` (`markOpened`, `restoreItem` and `keepItem` are batch 2), `CanvasBoardScreen`, `PlaceItemSheet` (the shell's `CharacterPickerSheet`, `FactionPickerSheet`, `TermPickerSheet`, `EventPickerSheet`), `CanvasChrome` (`IconId.Minus`) and `CanvasPopover`.
- **`label/`: `선 라벨` (CNV-08).** `LineLabelModel` (pair-based), `LineLabelEditor` and `LineLabelContent`. `LineLabelSheet.forCharacters` (`canvasKey == null`) shows only the title row (see Partial).
- **`node/`: node edit (CNV-10, CNV-11)**
  - `NodeEditModel` builds a `NodeEditView`. Besides batch 1's fields it has `deleted`, `item`, `restoreLabel` (only while the item is in the trash), `keepLabel` (until `이대로 두기` was chosen) and `unlockLabel`.
  - `NodeEditRows` (shared by the cover sheet and the inner popover) draws `지문으로 보기`, `되살리기`, `이대로 두기`, the open button and remove.
- **`page/`: pages (CNV-13 … CNV-16)**
  - `PageEdits`: pure writes.
    - `create`, `rename`, `setBlocks`, `setHidden`.
    - `addBlock`, `setKind`, `slash`: the `/` sheet turns an empty caret block, or adds after the caret.
    - `relink`; `unlink` (`Fixes.unlink`); `keep` and `restore` (`RefFixes`).
    - `delete` goes to the trash through `TrashOps` and returns the `Messages.deleteToast`.
  - `PageLinks`: resolves a link run to `PageLink(state LIVE · HIDDEN · TRASHED · GONE · OTHER, inline text, card title, item, route)`. It uses `ItemRefs` for 인물 · 용어 · 장소 · 세력 and its own rule for page targets (hidden before trashed).
  - `PageViews`: builds `PageView` / `PageParagraph`.
    - Shown text carries link and paren spans, and memo labels carry the recorder source.
    - `meta` is `마지막 수정 M월 D일 · 연결 N`. `linkCount` counts distinct outgoing targets plus live pages linking here.
    - The source mark uses `domain:recorder` `SourceMarks`.
    - `PageAccess OPEN · HIDDEN_LOCKED · GONE`: a locked or trashed page shows no name, body or header image.
    - `PageAccessRules.canEdit`: open, writable, and no locked hidden link, because the editor shows stored link text.
  - `PageListModel`: rows, plus one locked row `숨긴 페이지 N` while locked, or a `숨김` tag while open.
  - View models in `PageViewModels.kt`:
    - `PageDocViewModel`: name and body are written through with a merged undo step per field; one step per menu or card button.
    - `PageListViewModel`: `create`.
  - Screens:
    - `PageDocScreen` (board `Page`). Composition:
      - `CivilightScreen` with an empty title, small line `페이지` and `⋯`.
      - `CenterColumn`, which is the 560 column on inner screens.
      - The 21:9 header image.
      - Body: section line, name 30/38, meta, source mark.
      - Read mode draws blocks with `PageBlockText` (`PageBody.kt`). Edit mode uses the shell `RichTextEditor`, with `@` through `RichLinkPickerSheet`.
      - The command bar shows the hint, opens the `/` sheet and pushes `Recording(surface = page, targetKind = page, targetKey)` from its mic.
    - Local sheets (`PageSheets.kt`): `PageMenuSheet`, `BlockAddSheet`, `DeletedLinkSheet`, and `RelinkSheet` (shell `LinkPickerSheet`, `DOCUMENT_LINK_KINDS`).
    - `PageListScreen`: `목록 | 카드` (`ViewToggle`, `AppSettings.viewByScreen["pages"]`), `+` to make a page, list rows or one `SnapCardRow` of `RecentFileCard`s, and the locked row.
- **`card/`: card parts (CNV-21 … CNV-24)**
  - Card kinds, metrics, snap, ticker, parallax, `SnapCardRow` and `CardViewMotion` are the shared ones in `core:designsystem` `designsystem.card` since the motion wave (the local `CardRules.kt`, `SnapCardRow.kt` and `CardViewMotion.kt` are deleted). The row's card lambda gets `(item, imageModifier)`: put the modifier on the image layer inside the card's clip (`SlotImage(imageModifier = …)`).
  - `CardChoice.kt` keeps what needs `core:model`: `ViewChoice` (per-screen `목록 | 카드`, default `목록`) and `CardInitial`.
  - `CardParts.kt`: `ImagePlaceholder` (CNV-24), `ProjectCard`, `LockedCard`, `NewItemCard`, `RecentFileCard` (new `imageRadius` for the `목록 ↔ 카드` morph) and `PortraitCard`. The gallery card is core:ui's `GalleryCard`, not a copy here.
- **`image/`: images (CNV-24 … CNV-30)**. Pure parts first:
  - `ImageSlots` (T-CNV-32): `SlotSpec` per `ImageSlot` (name, used-in, ratios, chip labels, `holoAllowed`, holo note, picker title, short name, owner kinds); `ratios`, `holoAllowed`, `chip`, `parts(Ratio)` → `RatioParts(w, h)`.
  - `CropMath` (T-CNV-30, T-CNV-31): rotate → mirror → rectangle, as `SlotImages.decode`. `largestFrame`, `cropSize` (rounded), `crop(source, ratio, CropEdit)`, **`cropReadout`**, `editOf(crop)` (re-crop), `clamp`, `pan`, `rotate` (90° clockwise, centre turns with the picture), `flip`, `frameIn` (same frame as DS `CropFrame`), zoom steps 1.0 … 4.0 by 0.1.
  - `CropSession` (`@Serializable`, saved as JSON in `rememberSaveable`): one `CropEdit` per ratio, `current`, `touched`, `holo`; `select · zoom · zoomStep · pan · rotate · flip · reset · holo`, `crops()` = the edited ratios only, `readout()`; `start(slot, size, existing, hash, holo)` resumes the slot's crops when it already shows this original.
  - `ImageEdits` (T-CNV-33 … T-CNV-36): `ImageOwner(kind, key, slot)`; `use`, `addBlob` (once per hash), `placePreset`, `placeBlob` (same original keeps the other ratios' crops; a new one drops them), `setHolo`. One `ImageUse` per owner × slot (replaced in place, key kept); holo is forced off where the slot does not allow it. `ImageOwners.name` and `myImages` (this project's GALLERY / FILE image blobs, newest first).
  - `PresetImages` (CNV-26): the 8 presets (ids, names, colours, shard sets `i mod 4`), `byId`, `ofSource`. Ids are the same as feature:cast's `CharacterImages`.
  - `HoloRules` (T-CNV-34, T-CNV-55): `showToggle`, `initial`, `shows`, `sweepOnTurnOn` (null when reduced), `sweepTilt`, `sweepEdge` (the sweep fires on the toggle edge only), `blend` (the sweep's end settles into the live tilt), `fromDevice` (±25°), `fromDrag`, and the sway spring constants `SWAY_DAMPING` / `SWAY_STIFFNESS` (the old closed-form `sway` is gone). `HoloSensorGate` over a `TiltSensorPort`: registered only while ≥ 1 holo surface is composed and motion is not reduced and a sensor exists.
  - `CropHandOff`: in-memory, taken once: the picker's tapped tile bounds, its decoded thumbnail (`tileBitmap`) and the holo toggle for the crop editor (route arguments are frozen). `CropTransition` (pure): the start transform of a crop step (turn, mirror, reset, zoom step).
  - Composables: `PresetArt` / `DrawScope.drawPreset`; `HoloLayer.kt` (`ProvideHoloTilt` = one shared tilt and sensor per screen, `rememberHoloTilt`, `Modifier.holoDrag`, `holoFoilAt`, and the one-call `Modifier.holoSurface(on, sweepTrigger)` (raise `sweepTrigger` when the user switches holo on; the foil fades in and out with `on`), and `rememberSpringTilt`); `SlotImage` (preset art, decoded original with its crop, holo foil when `HoloRules.shows`, placeholder with `onPick`); `MineThumb`.
  - `pick/`: `ImagePickModel` (pure view: title, subtitle, tabs, tiles, holo row, initial selection and tab; `action(source)` → `PlacePreset` / `Crop`), `ImagePickViewModel` (keyed per owner × slot; `placePreset`, `import` from a content URI through `BlobStore.import`, refusing read-only before importing), `ImagePickSheetContent` (the shared sheet; `PickVisualMedia` and `OpenDocument`, no permission).
  - `crop/`: `ImageCropViewModel.apply` (one undo step `적용`), `CropStage` (drawing, gestures, frame, corner marks, readout, ratio aspect 200 ms, tile → stage growth), `ImageCropScreen` (own top bar and control panel through `CivilightScreen(topBar, bottomBar)`).
  - `specs/`: `ImageSpecsScreen` (board `ImageSlots`) and `PresetImagesScreen`.
- **`holo/`**: `HoloExperimentScreen` (CNV-31): tabs `은빛 홀로 카드 · 홀로 조각 · 1장 완결` with one shared tilt.
- **How to extend**
  - A new slot: a `SlotSpec` in `ImageSlots` (and its copy); everything else reads the registry.
  - A new holo surface: `Modifier.holoSurface(on)`, or `rememberHoloTilt()` + `holoDrag` + `holoFoilAt` when the tilt also drives something else (3D card). Wrap a screen with several in `ProvideHoloTilt`.
  - A new canvas action: a `CanvasEdits` function and test, a `CanvasBoardViewModel` method, and a `BoardAction` if a tap starts it.
  - A new page action: a `PageEdits` function and test, a `PageDocViewModel` method, and a `SheetAction` in `pageSheet`.

## Done

- **Integration check (2026-11-23):** `HoloRules.SWAY_STIFFNESS` / `SWAY_DAMPING` are now aliases of `CivilightMotion.holoSwayStiffness` / `holoSwayDamping`, and `rememberSpringTilt` uses `CivilightMotion.holoTiltStiffness` / `holoTiltDamping` (same values; looks unchanged). Still open from this file: the page menu `숨기기` calls `deps.unlock.unlockItems(projectKey)` (`PageDocScreen.kt`), which opens every hidden item of the project without 지문 · PIN.
Pure logic with passing tests. The screens on top are listed under Partial.

Batch 1:
- **CNV-01 engine logic** (`GraphAndToolsTest`, plus core:ui `GraphEngineTest` since the engine moved): viewport round trip, zoom around a point, zoom range, `fit`, hit order, 44 dp targets at 25 %, visible segment and label anchors, pair words and arrows from positions.
- **CNV-09 relation lines** (`RelationLinesTest`):
  - T-CNV-01, T-CNV-02, T-CNV-03, T-CNV-05, T-CNV-06, T-CNV-07, T-CNV-08, T-CNV-58, T-CNV-16 and the canvas part of T-CNV-15;
  - the canvas variant of T-CNV-04 (a plain line at 1-6);
  - arrows follow positions;
  - a locked hidden end draws no relation labels.
- **CNV-08 label model** (`LineLabelModelTest`):
  - T-CNV-09 (both directions), T-CNV-10 (four non-character lines), T-CNV-11, T-CNV-12, T-CNV-13, T-CNV-14;
  - `사이` switches the kind list;
  - `관계로 만들기` waits for a kind and is off when read-only;
  - an already converted line reuses its relation;
  - a locked hidden character cannot be named or related;
  - draft save/restore.
- **CNV-02/04/05/06/07/10 edits** (`CanvasEditsTest`):
  - route resolution and the first canvas;
  - item nodes store only kind + key and follow renames;
  - the six sample nodes read as the board;
  - memo text;
  - 묶음 membership and carrying;
  - tap–tap lines (no self-line, one per pair, none to a frame);
  - removing a converted line keeps the relation;
  - removing a node takes its lines but never the item;
  - node edit targets and read-only;
  - event search with 초성.
- **CNV-03 tool rules** (`GraphAndToolsTest`):
  - one current tool (`노드` first), and only it acts on empty space;
  - read-only answers `읽기 전용`;
  - `선` tap–tap with cancel; a tool change drops a started line;
  - saved-state round trip.

Batch 2:
- **CNV-12 캔버스 저장** (`CanvasRefsAndFileTest`). The canvas file `canvas/<key>.json` holds:
  - node keys, kinds, `refKey`, x/y, z order keys and `updatedAt/By`;
  - lines with `relationKey` only, never 사이 · 마음 · 종류 · 시작 (those live in the cast files);
  - no viewport, which is `_app/view.json`.

  Text is stored NFC. Every write stamps time and device. New nodes get ULID keys and draw on top.
- **CNV-11 logic** (`CanvasRefsAndFileTest`):
  - A trashed character node reads dotted `지운 인물 · 노아` with `되살리기` and `이대로 두기`.
  - `되살리기` re-attaches it by key.
  - `이대로 두기` is stored once and its button goes, while the dots stay.
  - After a permanent delete the node reads `영구 삭제 · 노아` with no `되살리기`.
  - A trashed event node restores through its trash entry.
  - A converted line whose relation went to the trash with its character draws plain, and the line keeps its relation key.
  - A hidden character while locked offers only `지문으로 보기`.
- **CNV-02 last opened canvas**: `resolveForRoute` with `lastCanvasKey`, falling back when that canvas is trashed.
- **CNV-13 … CNV-16 logic** (`PageTest`, 12 tests):
  - The page reads as the board.
  - T-CNV-17: page links raise no term warnings.
  - T-CNV-18: plain words are not references.
  - T-CNV-19: deleted link card, `글로 풀기`, restore and the permanent-delete branch.
  - `이대로 두기` and `다른 항목에 잇기`.
  - T-CNV-20: a page made from a record through `RecorderActions.makePage`.
  - T-CNV-21: the source mark through record trash and purge.
  - T-CNV-22: a hidden page while locked shows no name (header image nulled too; the example page has none), and a hidden, trashed page stays locked.
  - Links to hidden items keep the editor closed.
  - The page list, a new page, and delete to the trash.
  - `/` adds or turns a block.
  - Page copy.
- **CNV-21 … CNV-24 rules** (`CardRulesTest`): per-screen view choice with `목록` as default, card pitch per kind and width, T-CNV-41 (snapping, one tick per snap, 320 ms motion), T-CNV-42 (parallax and reduce), T-CNV-38 (initial letter), and card copy.
- **Sheet keys** (`SheetKeysTest`): `NodeEditSheet`, `PlaceItemSheet`, `LineStartSheet`, `LineLabelSheet`, the page sheets and (batch 3) `ImagePickSheet` round-trip through `RouteCodec` (the saved sheet stack, CNV-35). Nodes are measured with unbounded constraints.

Batch 3:
- **CNV-25 slot registry** (`ImageRulesTest` T-CNV-32): ratios, holo, chips, titles of every slot.
- **CNV-28 crop logic** (`ImageRulesTest`): T-CNV-30 (`2160 × 2880 · 원본 3024 × 4032`), T-CNV-31 (zoom 1.0 = largest frame), session edits (rotate, mirror, reset per ratio, ratio switch, holo refused for the background, saved-state round trip), panning clamps to the image and `editOf` reproduces a stored crop, zoom steps, the frame rectangle.
- **CNV-29 storage** (`ImageRulesTest`): T-CNV-33 (separate crops per ratio, original untouched, one use per owner × slot), T-CNV-35 (one blob row, two uses with its hash, checked through `ProjectFileSet` `assets/index.json`), T-CNV-36 (`preset:ash-dawn`, no blob), `내 이미지` filtering and order.
- **CNV-26 preset registry**: the eight names, ids, colours and shard sets.
- **CNV-30 holo rules**: T-CNV-34 (defaults, background never holo, also in the write), T-CNV-55 (sensor registered only while a holo surface is visible and motion is not reduced; no sweep when reduced; no gyro → drag), tilt from device, drag and sway.
- **CNV-27 picker model** (`ImagePickAndMotionTest`): the cover sheet reads as the board (`표지 이미지`, `잿빛 새벽의 등불 · 카드 3:4 · 목록 1:1`, `기본 이미지 8 · 내 이미지 0`, the current preset selected); an original in the slot opens on `내 이미지` and `고르기` leads to the crop editor; the hand-off is taken once.
- **CNV-45 plans** (`ImagePickAndMotionTest`): list ↔ card timings and reduce.

## Partial

All UI features below compile and are wired to the store. None has been seen on a device.

- **CNV-01 그래프 엔진**: drawing and gestures are now core:ui's. Not checked on a device: gesture feel, text sharpness at 200 %, the glass of the tool bar and zoom column.
- **CNV-02 캔버스 문서**: the header, lens chip, per-canvas viewport, last-opened canvas, and a first canvas when a writable project has none are built. Left: listing, switching, renaming and deleting canvases (Q-CV-01, nothing drawn).
- **CNV-03 도구 줄**: cover five columns and the inner pill, with `pri` on the current tool. Done in logic; not seen on a device.
- **CNV-04 항목 노드**: placing 인물 (shared `CharacterPickerSheet`) and 세력 · 사건 · 용어 (segmented picker over the shell's `FactionPickerSheet`, `EventPickerSheet` and `TermPickerSheet`), current names, node edit on tap. Left: Q-CV-04 (장소), Q-CV-05 (lens on item nodes; plain names are shown).
- **CNV-05 메모 노드**: create with `메모`; edit text in node edit (one undo step per node while typing).
- **CNV-06 묶음**: [PROPOSED] data shape only:
  - a fixed 260 × 180 frame with a title;
  - members join by dropping a card inside, and the frame carries them.

  Left: resizing, and any drawn design (Q-CV-06).
- **CNV-07 선 긋기**: tap–tap with the `선` tool. The label surface opens on the new or existing line. Batch 3: the label surface (cover sheet and inner popover) has a text button that takes the line off the canvas (`LineLabelEditor.remove` → `CanvasEdits.removeLine`, one undo step; a converted line's relation stays, Q-CV-13). Its label is `선 빼기` (proposed).
- **CNV-09 관계 선과 시점 렌즈**: done in logic; drawing not seen on a device.
- **CNV-10 노드 편집**: cover sheet and inner popover. Open the item, edit memo or 묶음 text, remove (`캔버스에서 빼기`, proposed copy, see Requests).
- **CNV-08 선 라벨**: the cover sheet (shared `LineLabelSheet`) and the inner popover show the same rows, with the `시작 시점` picker. `+ 새 종류` opens cast's `RelationKindSettings`; feature:cast has not registered it yet, so it lands on the shell placeholder. `LineLabelSheet.forCharacters` (relationship-map line, `canvasKey == null`) shows only the title row:
  - no lane opens it yet;
  - a pair with no canvas edge needs a cast write path other than `CanvasRelations.keepAsLine` / `makeRelation`, which both take a canvas edge.

  The model is pair-based, so only the state holder and the write would change.
- **CNV-11 숨긴 항목 · 지운 항목**: the board draws a dotted `◇` node (DS `NodeCard(deleted)` through core:ui). The node surface (sheet or popover) offers:
  - `되살리기`: the toast offers `되돌리기`; a hidden item asks `지문으로 보기` first;
  - `이대로 두기`;
  - `지문으로 보기`.

  Not seen on a device.
- **CNV-13 페이지**: the board `Page` layout is built, with read and edit modes:
  - shared rich editor with `@` links, `( )` and `⌁` memo paragraphs;
  - `/` sheet (`본문 · 소제목 · ⌁ 작가 메모`);
  - `⋯` menu (`고치기 · 페이지 머리 이미지 · 숨기기/숨김 풀기 · 지우기`);
  - header image 21:9 at the top;
  - mic → `Recording` for this page;
  - inner screens read in the 560 column.

  Left:
  - Q-PG-01 … Q-PG-04 stay as defaults. (`페이지 머리 이미지` now opens the registered picker.)
  - Not seen on a device: the editor inside a `LazyColumn` item, the keyboard, and tapping links.
- **CNV-14 페이지 목록**: the list and card view, `+` new page (opens straight into editing), and the locked row are built. **No lane pushes `PageList` yet** (Q-PG-05, no home shortcut), so the list and `+` are unreachable in the app (Requests: feature:story). `PageDoc` is reached from 최근 연 파일 (library `RecentsModel`), the recorder's 넣은 곳 (`PlacementRoutes`) and page links.
- **CNV-15 레코더 기록으로 만든 페이지**: the receiving side is built. The page shows its source mark (`레코더 M월 D일 HH:MM` → record screen; `레코더 · 휴지통` → recorder trash; `지워진 기록` as plain text), and memo blocks carry `⌁ 작가 메모 · 레코더 …`. The making is the recorder lane's sheet over `RecorderActions.makePage`. Not seen on a device.
- **CNV-16 숨기기 · 지우기 in pages**: built:
  - locked hidden page (lock row, `숨긴 항목` links, list row);
  - links to hidden items show `숨긴 인물` / `숨긴 항목`, and editing waits for `지문으로 보기`;
  - deleted links are dotted with `◇`;
  - card `지운 인물 · 노아` with `다른 항목에 잇기 · 글로 풀기 · 되살리기 · 이대로 두기`;
  - `영구 삭제 · 노아` without `되살리기`.

  Left: search and 최근 연 파일 exclusion belong to their owners (story search, library recents).
- **CNV-21 목록 | 카드**: wired on the page list only (`AppSettings.viewByScreen["pages"]`). 서재 and 인물 are the library and cast lanes', which use the DS `ViewToggle` themselves. The switch animates with CNV-45 (below).
- **CNV-22 가로 카드 줄**:
  - `SnapCardRow`: start inset 20/32, the next card peeks, snap to the nearest left edge with the DS 320 ms decelerate curve, a haptic tick per snap, 8 % parallax off under reduced motion, pager marks.
  - Compose's snap provider chooses the target item from the fling velocity; `CardSnap.target` is the rule it matches for a slow release.
  - Only the page list uses it (Requests: move to a shared module).
- **CNV-23 카드 부품**: `ProjectCard` (DS `ImageCard`), `LockedCard`, `NewItemCard`, `RecentFileCard` and `PortraitCard` are built to the `scr_cards.py` sizes. Only `RecentFileCard` is used (page cards), because library and cast cannot depend on this module (Requests). Holo layers are CNV-30 (batch 3).
- **CNV-24 이미지 없음**: `ImagePlaceholder` (tint, inset edge, first grapheme 64/80 `ink3` at 16/8, the `이미지 고르기` tag with a 44 dp target) is built, and `SlotImage(onPick)` draws the tag. In this module no slot shows an empty placeholder with a pick action (a page without a header image shows none, board `Page`; page cards open the page), so the tag appears only where other lanes use the same pattern (they keep their own copies until `card/` / `image/` move, Requests).
- **CNV-25 이미지 규격**: `ImageSpecsScreen` (board `ImageSlots`) is registered. No lane pushes `ImageSpecs` (Q-IM-07, Requests).
- **CNV-26 기본 이미지 8종**: drawn from the spec table everywhere this module shows an image (`SlotImage`, picker tiles, `PresetImagesScreen`). `PresetImages` is registered; no lane pushes it yet (library `기본 이미지 · 보기 ›`, Requests). feature:cast and feature:world still draw their own copy or a neutral shard (they cannot depend on this module).
- **CNV-27 이미지 고르기**: the shared sheet is registered and works over any area's screen (cast portrait/background, world card reference, the page header). Tabs, three-column grid with the DS `ImageTile`, holo row only for holo slots, `갤러리` (system photo picker) and `파일` (document picker) import into the blob store and `내 이미지`, then open the crop editor; a preset is placed with `고르기`; a `내 이미지` tile opens the crop editor. Read-only: the buttons say `읽기 전용` and nothing is imported. Not seen on a device: the system pickers, import of large files, the sheet height with the grid.
- **CNV-28 자르기**: full-screen editor: `취소 · <owner> · <slot> / 자르기 · 적용`; stage with the picture under the fixed frame (drag to move, pinch to zoom), outside darkened, thirds, corner marks, readout; ratio chips with `규격 고정` (each chip edits its own crop, the frame's aspect animates 200 ms); `확대` DS step slider with `1.4×`; `90° 회전 · 좌우 반전 · 처음대로`; the holo row with a live foil on the stage. `적용` = one undo step; `취소` / back drop the session; the session survives process death. Not seen on a device: gesture feel, EXIF orientation (photos whose orientation lives only in EXIF are shown and cropped as stored, the same as the cast and world decoders), memory with very large originals (decoded at most 2048 px).
- **CNV-29 이미지 저장**: one blob per content hash, one use per owner × slot, crops per ratio, presets by name. Trash with the owner is `domain:trash`'s (liveness of image uses). Caches are not written (decoding is on demand). Gap: core:store `BlobReferences` counts only rows that use a hash, so an original in `내 이미지` that no slot uses is deleted by the blob garbage collection after its grace period while its `assets.blobs` row stays; the picker hides such rows (`fileExists`). See Requests.
- **CNV-30 홀로그램 효과**: the DS foil driven by the rotation sensor (game rotation vector, relative to how the device was held), drag-to-tilt with a slow sway when there is no sensor, one 480 ms sweep when switched on in the picker or crop editor, a still foil under reduced motion, the sensor registered only while a holo surface is composed. Drawn in `SlotImage` (holo-capable slots), on the picker's selected tile while the holo row is on, on the crop stage and in the experiment. Not seen on a device ("자이로 홀로그램 느낌" is an open check).
- **CNV-31 홀로 실험** (P2): `HoloExperimentScreen` with the three boards' content (board sample text). `카드로 저장` and `2장 시작` are disabled (Q-IM-11). No lane pushes `HoloExperiment`.
- **CNV-45 카드 · 이미지 모션**: `목록 ↔ 카드` on the page list (shared thumbnail → card image, text out then in, reduced crossfade); 가로 카드 줄 snap, tick and parallax (batch 2); `고르기 → 자르기`: the tapped tile grows into the stage (ENTER 360 ms) and the frame fades in, ratio chips animate the frame only; `홀로 켜기` sweep. Left: the picker sheet closes before the route is pushed, so the shell's push transition plays at the same time as the growth; after process death the growth is skipped.

**Loose ends still open after the last batch, and why:**
- Canvas list, switch, rename, delete (Q-CV-01): no board or copy draws any of it; one canvas per project works (last-opened canvas, first canvas made automatically).
- 묶음 resize and drawn design (Q-CV-06): no board or behaviour in any source.
- `LineLabelSheet.forCharacters`: no lane opens it, and a pair without a canvas edge needs a `domain:cast` write path that does not take a canvas edge (this lane may not edit domain:cast).
- `PageList` is not pushed by any lane (request to feature:story); `ImageSpecs`, `PresetImages`, `HoloExperiment` likewise (request to feature:library).
- `card/` and `image/` cannot be used by other lanes until they move to core:ui (request).

## Motion wave (0.1.0, step 1 of 3)

Rows of `docs/status/motion-audit.md` §4 "feature:canvas", in the order of that list. All use the shared helpers only (`CivilightTheme.motion`, `civilightItem`, `ContentSwap`, `FoldReveal`, `ValueSwap`, `rememberUnhide`, `animatedColor` / `animatedFraction`, `motion.move` / `spec` / `swap`); `MotionLint` runs in `CanvasCopyLintTest` and finds nothing. No `CivilightTheme.reducedMotion` is left; the holo code uses `motion.loops`. Everything is unverified on screen.

| # | Row | What changed | Check on the emulator |
| --- | --- | --- | --- |
| 1 | Zoom buttons and initial fit | The board latches its view once the node sizes are measured (a saved view is used as is, else the fit) and never refits by itself again. The surface sits at opacity 0 until then and fades in on `enter`; it is re-created at the latch so the fitted view is its first view (no glide). A 600 ms safety net shows it anyway. `+` / `−` set the target viewport and core:ui's `GraphSurface` moves it on `standard` (snap when reduced). The percentage swaps with `ValueSwap` after a button press and is shown as is while pinching | open with and without a saved view, `+` / `−`, pinch, 항상 줄이기 |
| 2 | Picker → crop hand-off | `CropHandOff` also carries the tile's decoded thumbnail. The `ImageCrop` screen composes `CropStage` from its first frame: the thumbnail is drawn in the frame and grows from the tile on `enterLong` while the original decodes; the stage then takes over in place. Without a hand-off (gallery or file import, process restore) the stage fades in when ready. Controls keep their place from frame 0 and fade in on `enter` (blocked until the session exists). The fade-only route transition is a Request | 고르기 → 자르기 on a `내 이미지` tile, import from 갤러리 |
| 3 | Page list rows, card row items | `civilightItem()` on every list row, the section label, the card row and the locked row (the DS `SnapCardRow` does it per card). Loading, empty and list crossfade (`ContentSwap`). Pages that appear when hidden items open come in through `rememberUnhide`. `+` folds in beside the toggle | page list, 지문으로 보기, new page |
| 4 | List \| card nudge and radius | The second card starts 24 dp left and moves to its place on `enterLong` when the card view is switched on (none at first show, none when reduced; `PageListMotion.nudgeDp`). The thumbnail ↔ card radius (10 → 14 dp) morphs on the transition. The size transform is the DS `CardViewMotion.transform`'s explicit snap with `clip = false` | 목록 ↔ 카드 both ways |
| 5 | Parallax scale | `SlotImage(imageModifier)` replaces `shiftDp`; the DS row applies the constant 1.1 scale (1.0 when reduced) and `−offset × 8 %` inside `graphicsLayer` | scroll the card row, no edge shows |
| 6 | Crop | Ratio change: the frame's aspect on `standard`; reduced: the old frame crossfades into the new one (no snap). Turn, mirror, reset and zoom steps from the controls ease the new picture from its old look (`CropTransition`, about the frame centre, a turn also carries the size change; a drag or pinch follows the finger, told apart by a step counter the controls raise); reduced: snap | 90° 회전, 좌우 반전, 처음대로, the zoom slider |
| 7 | Holo | The sweep fires on the toggle's edge only (`holoSurface(sweepTrigger)`): not on restore, not when another tile becomes the shown one, not on a crop session restore. After the sweep the tilt settles into the live tilt on `standard`. The drag sway is a damped spring `Animatable` (follows the animator scale). The foil fades in and out with the toggle. Loops stop when `motion.loops` is false | holo on in the picker and the editor, rotate the device, restore |
| 8 | Page read ↔ edit and access | `civilightItem()` on header, lock, deleted, head, editor and paragraphs; title shown ↔ field and loading ↔ page crossfade (`ContentSwap`), the typed line is never animated; `⋯` and the source mark fold; a hidden page that was just unlocked uses `rememberUnhide` | 고치기 on and off, unlock a hidden page |
| 9 | Sheets | `이미지 고르기`: tab grids crossfade, the holo row folds. `선 라벨`: the relation rows fold with the line's ends, the kind chips swap with `무엇으로 적을지`, the start value swaps with `ValueSwap`, `읽기 전용` folds. Node edit: `읽기 전용` folds (the buttons stay plain equal-weight rows: a weight does not combine with a fold, and the sheet closes after each action). `노드` picker: the three pickers crossfade | each sheet, with the keyboard up too |
| 10 | Tool bar and loading | Cell fill and label colour use `animatedFraction` / `animatedColor`. Loading, no canvas and the board crossfade as whole screens (`ContentSwap`). The bar, its cells and the cover hint follow a fold or unfold on `foldOpen` / `foldClose` (`animateDpAsState`, `animateContentSize`, `FoldReveal`) | fold and unfold on the canvas, first open |
| 11 | Holo experiment | The tab bodies crossfade. The card follows a sensor's tilt through a spring (`rememberSpringTilt`); a dragging finger is tracked as it is | the three tabs |

**QA 2026-11-23**
- #2, #3, #4 (canvas part): the three `TODO_COPY_CANVAS_*` strings are replaced by `캔버스에서 빼기`, `선 빼기` and `축소` (proposed, see Requests). All other `TODO_COPY_CANVAS_*` constants in the module (page list, picker, crop) are replaced too, and `CanvasCopyLintTest` fails if a `"TODO_COPY` string returns to main sources. `TODO_COPY_LIB02_MORE_LABEL` is the library lane's.
- #10: new nodes (item, memo, 묶음) land on the middle of the visible canvas (between the top bar and the tool bar) and step aside from any node already there (`NodePlacement`, tested). The tapped point is now only the trigger, not the position. The item picker carries the centre in its sheet key and resolves overlap when the item is picked. The view no longer shifts when a node is added or a sheet opens, because the fitted view is latched (before, an unsaved view was recomputed from the node bounds and the layout size on every recomposition).

**Removed local copies:** `card/CardRules.kt` (model-free part), `card/SnapCardRow.kt`, `card/CardViewMotion.kt` (DS `designsystem.card`), the conditional 1.1 scale and `shiftDp` in `SlotImage`, `HoloRules.sway`. The graph engine was already core:ui's (no copy left here; `GraphAndToolsTest` holds the canvas half of the tests).

**Pane split (CNV-47):** this module draws no two-pane screen of its own (the board, page screens and the crop editor are single-pane), so there is nothing to switch to `SplitPanes` / `rememberPaneSplitState` yet. The gallery's list + detail (GAL-23, step 2) will use `SplitPanes(PaneSplit.Modules.GALLERY, …)`.

**Left for steps 2 and 3:** step 2 is done (see "Gallery part 1"); step 3 registers the three gallery sheets.

## Gallery part 1 (0.1.0 wave, step 2 of 3)

Package `feature.canvas.gallery`. Pure parts first, all JVM-tested on fixture G0 (`ExampleGallery.build()`) in `GalleryScreenRulesTest`; composables stay thin.

**Pure layer**
- `GalleryModel.kt`
  - `GalleryRows.build(doc, ownerKind, ownerKey, locked, filter, sort)` → `GalleryView`. Masking comes only from domain:library `ItemSurfaces.gallery` / `projectGallery`, so the gallery shows what the hiding contract allows.
  - The view holds: chips (`전체 8 · 삽화 4 …`, plus `프로젝트 · 인물` for the project gallery), sections (one per owner group for GAL-10), entries with 1-based positions among the visible items, the locked row, the hidden-characters row, the unlocked bar, `countLine` (`8장 · 숨김 2`), `orderEditable`, `isEmpty`, `filteredEmpty`.
  - `GalleryAccess`: a hidden character while locked, or a trashed one, is decided before anything is drawn, so no owner name or add card shows.
  - `GalleryViewChoice`: `viewByScreen` keys `characterGallery` / `projectGallery`, default `카드` (not `ViewChoice`, whose default is `목록`).
  - `GallerySort`: `직접 순서` (stored), `넣은 날` (newest first), `씬 순서` (manuscript order; a trashed scene, or a hidden one while locked, counts as unlinked). Storage is never reordered.
  - `GalleryFilter`: saveable ids.
  - `EntryLabels`: main-image chips derived from `ImageUse` (`초상 · 홀로`), the card and list meta line (scene code, else tags), scene states. Scene codes come from domain:story `UnitCodes`, which also gives a trashed scene the code it would have; a purged scene falls back to its `NameTag`.
- `GalleryPanel.kt`
  - `GalleryPanels.panel(...)` → `PanelModel` with title or `제목 없음`, `3/8`, the scene line, tags, caption spans, and buttons `보기 · 고치기 · ⋯` or `설명 쓰기 · 보기 · ⋯`.
  - Scene line: `1-7 씬 3 · 2일째 새벽` (tappable), `숨긴 씬`, or dotted `지운 씬 · 1-6 씬 1`.
  - `@` links whose target is trashed, or hidden while locked, are drawn as plain text (the typed text stays as typed).
  - `clampLines`: `카드` 3, `격자` 2, `목록` none.
- `GalleryPick.kt`
  - `GalleryPick`: `tap` (picks, or unpicks on re-tap), `menu` (`⋯` picks), `arrive` (only a visible key), `reconcile` (drops a pick that a filter, relock or delete hid), `toggleExpanded`, `save` (keeps the scroll fields).
  - `GalleryLayout` and `GalleryPlacement`: where the panel goes (under the row, row expands, right pane), where the hint goes (only `카드`; inner list + detail shows `이미지 누르기 · 설명 보기` on the right; grouped rows put it in the order line), and the order-line slot (pager up to 12, `41 · 128` readout, `8장 · 숨김 2`).
  - `GalleryGrid.lines`: rows of three; the panel goes after the row holding the pick; the lock tile goes last.
  - `CardRowRules`: `readout`, `arrowX` (null = the picked card is away, which is what shows `고른 카드로`).
  - `GalleryColumn.indexOfItem`: the LazyColumn index of a list row or grid line, for scrolling the pick into view.
  - `ViewerNav`: no wrap, ends disabled; `settle` when the shown item leaves the list.
  - `ViewerZoom`: scale 1…4, pan clamp.
  - `GalleryFacts`: `4.2 MB` and `MM-DD`.

**Screen** (`GalleryScreen`, registered as `entry<Gallery>` in `canvasEntries`; view model `GalleryViewModel`)
- Header: back, small line = owner name, title `갤러리`, the DS three-cell `ViewToggle(mode)` and `+` (`이미지 넣기`).
- Under the header, scrolling with the content: filter chips (one choice), the order line (`직접 순서 ⌄` opens a local radio sheet with the three sort modes; accessibility `정렬`; right slot per `GalleryPlacement.orderSlot`), and the unlocked bar `숨긴 이미지 2 · 열림 · 앱을 떠나면 잠김` with `다시 잠그기` (`UnlockController.relockNow`).
- `카드` (GAL-07, GAL-08): one DS `SnapCardRow` per section (`showPager = false`; the pager or readout sits in the order line, or in the group header for GAL-10) of core:ui `GalleryCard` (picked = lift and ring). The 설명 판 folds open under the row with an arrow on the picked card. The arrow slides when the pick moves, and the panel text crossfades. When the card scrolls away, `고른 카드로` takes the title row's right slot and the position moves to the meta line. While nothing is picked, the hint `카드 누르기 · 설명 보기` shows under the row. The locked card ends the row.
- `격자` (GAL-06): rows of three 3:4 tiles with the selection ring (no check badge). The panel (2-line clamp) is inserted under the picked row. The lock tile is the whole-tile `지문으로 보기`.
- `목록` (GAL-05): rows with a 64 thumbnail (the `목록 1:1` crop), title, two-line description, mono meta and `⋯`. The picked row turns `sel` with the 2 dp `acc` bar and folds open with the full description, meta, position and buttons. A `LockRow` ends the list.
- Panel buttons: `더 보기` · `접기` appear only when the text was clamped; the overflow is latched per item, so `접기` stays.
- The sheet host (`SheetHostLayout`) wraps both the screen and the viewer, so sheets opened from `보기` show over it.
- `보기` (GAL-09) is in-entry state, not a route.
  - The card image grows into the viewer image as a shared element (`CardViewMotion.growSpec`; fade when reduced).
  - Top: `닫기`, `<owner> · 갤러리` over `i/N`, `⋯`.
  - The whole original is drawn fitted (decoded at 2048 px max) with holo.
  - Pinch zoom and two-finger pan for viewing only; one finger does nothing.
  - `이전` · `설명` · `다음`: an end button is disabled (the DS disabled alpha .38, animated); images slide with `motion.push` and fade when reduced.
  - When the shown item leaves the list (deleted, relocked), the viewer moves to the nearest item by its last index, or closes.
  - `설명` (switch semantics) folds the text panel below the image (fade at the bottom edge) and is stored in `AppSettings.galleryTextOn`.
  - Inner: image left, a 300 dp column right with `원본 W × H · size` (`기본 이미지` for a preset) and `넣은 날 MM-DD`.
  - Back or `닫기` returns with the last viewed item picked and scrolled into view.
- GAL-23
  - Inner `목록 + 상세` with `목록` / `격자`: `SplitPanes(PaneSplit.Modules.GALLERY)`. Left: the list without the inline panel. Right: `고른 이미지 · i/N`, title, `⋯`, the 261 × 348 card crop with chip and holo, meta, full description, buttons. Nothing picked: the first item dimmed with `이미지 누르기 · 설명 보기` (§6 Q8).
  - `가운데 한 칸`: the cover layout in `CenterColumn`.
  - Inner `카드`: full width, 236 × 315 cards, two-column panel.
- GAL-25
  - Empty: `갤러리 · 0`, the outlined 222 × 296 card (`+`, `이미지 넣기`), the line `기본 이미지 8 · 사진 · 파일 · 원본 그대로`.
  - The main-image offer (`초상 이미지도 넣기` / `표지 이미지도 넣기`, `지금 초상 · <preset name or 사진> · 원본 하나`, `넣기`) is fully working through `GalleryActions.add` (one undo step, toast with `되돌리기`, holo default from settings).
  - A chosen tag with no images left shows `<tag> 0` and `이 태그의 이미지 없음` with `전체 보기`.
  - More than 12 cards show the readout. Lists are lazy.
- State
  - The pick, `더 보기`, filter, sort and the open viewer are `rememberSaveable`. The pick and panel are also written through to `_app/view.json` (`GalleryViewState`) only when they change. The scroll of the main row or list is saved when scrolling settles.
  - On first entry the pick is seeded from `_app`, so returning restores it.
  - The route's `pickKey` is applied once per entry, and only when visible; it scrolls the item into view.
  - Reconcile runs only on a loaded document, so a cold start does not wipe the saved pick.
- Images
  - The blob branch of `SlotImage` is factored into `image/BlobImage` (hash, crop, max side, content scale).
  - `GalleryImage` draws an item's crop or whole original. Decode sizes: list 192, tile 360, card 720, viewer 2048.
- Motion (GAL-24) uses only the shared helpers:
  - `civilightItem` on every lazy row, chip and card.
  - `FoldReveal` / `FoldRevealValue` for the panel, hint, expanded row and viewer text.
  - `motion.swap` for the panel text, `ContentSwap` for body states and the detail pane, `motion.push` for prev / next.
  - `animatedFraction` / `animatedColor` for rings, row fill and bar; `motion.move(standard)` for the arrow and panel height.
  - `MotionLint` and `CopyLint` pass (`GalleryUiCopy` is in the lint list).

**Not built in this step (left)**
- Step 3: done (see "Gallery part 2"): the three sheets are registered, and crop, order, reuse, holo, `여러 장 고르기`, hide and delete are built.
- GAL-04 search: the gallery's own command bar (`갤러리에서 찾기 · 제목 · 설명 · 태그`) is not drawn. The project search hits exist in domain:story (`GallerySearch`); feature:story draws them and opens `Gallery(…, pickKey)`, which works now.
- GAL-24 rows not done:
  - The `목록 ↔ 카드 ↔ 격자` thumbnail morph: the view switch just redraws; only the viewer has a shared image.
  - The 8 dp rise and the 40 ms stagger of newly added cards: they fade in through `civilightItem`.
  - The `이 뒤로` row slide (GAL-15, step 3).
- Project gallery (GAL-10): the three defects below are fixed in step 3 (see "Gallery part 2"). They were:
  - With a tag or `프로젝트` chip, the model returns several header-less sections, so `카드` draws several rows, each with its own hint.
  - With `전체` or `인물` (grouped), the `숨긴 이미지 N` lock is drawn only in `목록`; `카드` and `격자` leave it out.
  - Card sizes per group (162 × 216, 132 × 176) are not applied: every row uses the 222 × 296 card.
- Emulator checks:
  - The arrow position on a fling and at row ends.
  - `고른 카드로` when the picked card is half off-screen (it counts as away only when its centre leaves the row).
  - The shared image from a card inside the row's parallax layer into the viewer.
  - The panel height animation with `더 보기` in the two-column inner panel.
  - Pinch inside `AnimatedContent`.
  - The settle-time scroll save.
  - The split handle on the gallery.
  - Reduced motion and animator scale 0 throughout.
  - The card row's panel recomposes on every scroll frame while the picked card is visible (the arrow position is read in composition, and a new `GalleryHandlers` is built each pass); move the read into the draw phase if it shows.
  - The arrow slides in from x = 0 when the picked card comes back into view.

**Defaults used (proposed copy, to confirm)**
- `지금 표지`: the project counterpart of `지금 초상` in the empty gallery's cover offer.
- `갤러리 없음`: the gallery of a character in the trash (pattern `캔버스 없음`).
- `정렬`: the sort sheet's title (the spec gives it only as the accessibility label).
- The viewer's preset fact line reads only `기본 이미지`: presets have no registry pixel size in this module.
- `지운 씬 · <code>` is drawn with a plain underline; the DS has no dotted text underline.
- A hidden character's gallery (locked) shows one `LockRow` `숨긴 인물 1` with `지문으로 보기`.

## Gallery part 2 (0.1.0 wave, step 3 of 3)

Package `feature.canvas.gallery` unless named. Pure parts first, JVM-tested on G0 in `GalleryEditRulesTest`; composables stay thin. Every write goes through `domain:actions` `GalleryActions` (DECISIONS §1), one undo step per action; an action that changes nothing (a move at an end) leaves the document as it is, so the store records no step.

**Pure layer** (`GalleryEditModels.kt`)
- `MyImages.visible(doc, locked)`: `내 이미지` without originals that only hidden things use while locked (hidden gallery images, images of hidden characters, places, terms, factions, pages). An original nothing uses yet stays listed. The CNV-27 picker uses it too (`ImagePickModel.build(…, locked)`), so 55 §3.4's last row and T-GAL-20 hold for both pickers.
- `GalleryAddModel`: tabs `기본 이미지 8` · `내 이미지 <N>`, tiles with caption (`4.2 MB` or the preset name), mark (`이미 있음` · `휴지통에 있음` from `GalleryActions.sourceMark`, never for a hidden holder while locked), numbered badge in tap order, `넣기 · <k>`, `toggle`, `sources`.
- `GalleryMenus`: the menu model (title, `리엔 · 갤러리 3/8`, main rows, crop row, order row, holo, hidden); `mainRows` derives `지금 초상` from `ImageUse` (no stored flag, T-GAL-07); `mainPlan` = place a preset at once, or open the slot's crop editor (nothing written before `적용`, T-GAL-08) with the note `숨긴 이미지 · 초상에서는 보임` for a hidden item (T-GAL-23); `orderRow` = `3/8 · 직접 순서` with `맨 앞` · `앞으로` · `뒤로`, or disabled with `넣은 날` · `씬 순서` · `전체에서만` (T-GAL-11, T-GAL-12, T-GAL-24).
- `GalleryInfo.head`: `3024 × 4032 · 4.2 MB · 사진` (preset `기본 이미지`, no size), `리엔 · 갤러리 3/8 · 원본 그대로`.
- `GalleryOrder` + `OrderState`: rows of the owner's visible items in `직접 순서`, tap picks, `옮기기` / `취소` / `이 뒤로` (T-GAL-25).
- `MultiPick`: `여러 장 고르기`, `<k>장 고름`, reconcile with the visible keys.
- `GalleryCopyTargets`: the project, then live characters in cast order (hidden ones left out while locked), `이미 있음` for a single item's source, search by name (초성 too).
- `GalleryCardSizes` (`GalleryModel.kt`): 222 × 296 ungrouped, 162 × 216 project row and 132 × 176 character rows when grouped (GAL-10).
- `CropSession` (`image/`) gained `target = SLOT | GALLERY` (saved, default `SLOT`), `chip(ratio)` and `startGallery(size, item)`: ratios `갤러리 카드 3:4` · `목록 1:1`, holo allowed, the item's stored crops as start.

**Sheets** (`GallerySheets.kt`, registered in `canvasEntries`; shared keys from `core:navigation`, three feature-local pickers)
- `GalleryAddSheet` (GAL-11, board `GalAdd`): title `갤러리에 넣기`, `<owner> · 여러 장 · 고른 순서대로`, tabs, three-column tiles with ring, number badge and dimmed marked tiles, note `원본 그대로 · 같은 원본은 한 번만 저장`, buttons `사진` (`PickMultipleVisualMedia`) · `파일` (`OpenMultipleDocuments`) · `넣기 · <k>`. `사진` and `파일` copy the originals into the blob store and add them at once, in the order the system returns them, as one `GalleryActions.add` (the bytes are imported first; no separate `내 이미지` undo step). Toast `<k>장 넣음` (`· 이미 있음 <m>`) with `되돌리기`, which also puts the previous pick back. The sheet closes and the first new item is picked once the view holds it. Holo starts as `홀로그램 효과 기본`.
- `GalleryInfoSheet` (GAL-13, board `GalInfo`): head, `제목` (written as typed, merged undo per item), `설명` (core:ui `RichTextEditor` with `@` links, written as typed, merged undo; `focusDescription` focuses it), `태그` (project tags as chips; `+ 태그` opens a `이름` line with `넣기` that creates the tag through `CharacterEdits.addTag` and puts it on the item in one step), `연결` (`씬` · `1-7 씬 3 · 2일째 새벽` with `빼기` · `바꾸기`; empty: `씬 고르기`).
- `GalleryMenuSheet` (GAL-14, board `GalMore`): `보기` · `고치기`, then `초상으로` · `배경으로` (character) or `표지로` (project) with `지금 …` in place of the chevron, `자르기` (`갤러리 카드 3:4 · 목록 1:1 · 원본 그대로`; preset items: dimmed `기본 이미지 · 자르기 없음`), `다른 곳에도 넣기`, `순서` with three buttons, `순서 바꾸기`, `홀로그램 효과` and `숨기기` switches, `지우기` (`휴지통 · 30일 뒤 삭제`, toast `이미지 지움 · <title>` with `되돌리기`, no confirm sheet).
- Feature-local: `GalleryCopySheet` (GAL-16 owner picker with search; marked owners dimmed), `GallerySceneSheet` (core:ui `UnitPickerSheet` limited to the scene level), `GalleryTagsSheet` (GAL-18 tags on several: a chip is on when every item has it).
- The sheets read the screen through `LocalGalleryHost` (positions, sort and filter, `보기` / `자르기` / `순서 바꾸기`, the pick after an add). Opened from another screen they use the owner's default view (`전체`, `직접 순서`) and open `Gallery(…, pickKey)` for the in-screen modes.

**Screen** (`GalleryScreen.kt`, `GalleryOrderScreen.kt`, `GalleryBodies.kt`)
- The header `+`, `⋯`, `고치기`, `설명 쓰기` and the empty card now appear (the registry has the sheets). In the read-only example `+`, `고치기` and `설명 쓰기` stay hidden; the menu opens (for `보기`), its switches are disabled and its write rows say `읽기 전용`.
- In-screen modes over the gallery, one `AnimatedContent` (`enterLong` in, `exit` out), back leaves each: `보기` (step 2), `자르기` and `순서 바꾸기`.
  - `자르기` (GAL-12, board `GalCrop`): the CNV-28 editor, factored into `image/crop/CropEditor` (the `ImageCrop` route uses the same code), header `<owner> · 갤러리`. `적용` writes the edited ratios and holo as one step (`적용`); `취소` writes nothing.
  - `순서 바꾸기` (GAL-15, boards `GalOrder`, `GalOrderMove`): header `<owner> · 갤러리` / `순서 바꾸기`, section `직접 순서 · <N>`, rows (thumbnail 44, title or `제목 없음`, position, `앞으로` · `뒤로`, disabled at the ends), the picked row on `sel` with the bottom bar `고른 이미지 · <title> · <i>/<N>` and `맨 앞` · `옮기기` · `맨 뒤`. `옮기기`: the bar `옮기는 중 · <title>` over `<i>/<N>` with `취소`, `맨 앞으로` above the first row, `이 뒤로` on every row (the moving row reads `옮기는 중`). Rows slide on `standard` (`civilightItem`), the list keeps the moved row in view. Leaving picks the last picked row in the gallery.
- `초상으로` · `배경으로` · `표지로` (GAL-14): an original pushes the existing `ImageCrop` route for that slot with a `CropHandOff` that carries the item's holo flag and, for a hidden item, the note shown above the crop controls (kept with the screen across process death). `취소` there cancels the whole action (T-GAL-08). A preset item is placed at once (CNV Q-IM-03 default: presets are not cropped). The background slot never carries holo (CNV-25).
- `숨기기` (GAL-19): hiding writes `hidden` and nothing else. While locked the image folds into the `숨긴 이미지 <H>` tile at once (it is not opened again without 지문 · PIN); while unlocked it keeps its place with the `숨김` chip. Unhiding asks for 지문 · PIN first (`AuthCopy.HIDDEN_PROJECTS` prompt, LIB-32).
- `여러 장 고르기` (GAL-18): the text button in the order line (every view; only where the menu exists). Cards, tiles and rows get a checkbox (cards and rows hide `⋯`), a tap toggles instead of picking. The bottom bar: `<k>장 고름` (count swaps) and `닫기`, then `태그` · `숨기기` (`숨김 풀기` when all chosen are hidden) · `지우기` · `다른 곳에도 넣기`, one undo step each. Back leaves the mode; the pick is untouched.
- GAL-10 defects from step 2 fixed: an ungrouped project filter (`프로젝트`, a tag) is one header-less run over every owner sorted as a whole; grouped `카드` and `격자` end with the `숨긴 이미지 <H>` row like `목록`; grouped card rows use 162 × 216 / 132 × 176 (a leading size modifier wins over `GalleryCard`'s own size, so no core:ui change was needed).
- Image picker (CNV-27): the system photo button reads `사진` (55 §6 Q1 default, `ImageCopy.BTN_PHOTO`, also the import undo label); `내 이미지` is masked while locked.
- `ImageSpecs` (CNV-25 board): a display row `갤러리` · `갤러리 카드 · 목록 썸네일 · 보기는 원본` · `홀로 선택` · `3:4` `1:1` after the slot rows.
- Capture blocking (LIB-26): the shell's `ProjectEntryFrame` already protects the `Gallery` entry, its sheets and modes; nothing added here.
- Motion of the new parts: sheets and bars fold (`FoldReveal`, `FoldRevealValue`), the tab grids, the `연결` box, the menu's `지금 초상` ↔ chevron and the order row's `앞으로 · 뒤로` ↔ `이 뒤로` / `옮기는 중` crossfade (`ContentSwap`), the add badge fades in and out (`animatedFraction`, kept composed with its last number), the multi count swaps (`ValueSwap`), rows slide (`civilightItem`), the modes cross on `enterLong` / `exit`. `MotionLint` and `CopyLint` pass (`GalleryCropChips` added to the lint list).

**T-GAL cases and where they are tested**
- This module: 01 … 09, 11 … 14, 16, 19 (search hit opens the item: `GalleryPick.arrive`), 20 … 26, 30, 32 … 34, 36, 37, 39 (`GalleryScreenRulesTest`, `GalleryEditRulesTest`).
- Elsewhere: 10, 17, 18 (`domain:library` `GalleryPackageTest`), 15, 28 (`domain:actions` `GalleryActionsTest`, `domain:trash` `GalleryTrashTest`), 19 (`domain:story` `GallerySearchTest`), 27 (`GalleryActionsTest`, core:store `GalleryStoreTest`, core:model `GalleryModelTest`), 29, 35 (`GalleryTrashTest`), 31 (`domain:cast` `CastGalleryTest`, `GalleryActionsTest`; the sheet uses `CharacterEdits.addTag`), 37 (`GalleryModelTest`, `GalleryStoreTest`), 38 (`GalleryActionsTest`).

**Not built (left)**
- The order screen's grip and drag (`끌어서 옮기기`): not built. The buttons and `옮기기` are the visible way to move, also for 100+ items; a drag would need a reorderable lazy list.
- GAL-04: the gallery's own command bar search (`갤러리에서 찾기 · 제목 · 설명 · 태그`). Project search hits open `Gallery(…, pickKey)` (feature:story draws them).
- GAL-16: the owner picker has search but no tag chips.
- GAL-19 `이름 가리기` (LIB-27): in this codebase the protection covers widgets, notifications and recents (`domain:library` `ProjectSurfaces`, `ProtectionScreens`: `위젯 · 알림 · 최근 항목`), and the shell's `ProtectWindow` sets the recents label for the gallery entry. Names inside the project's own screens are not masked anywhere, so the gallery does not mask owner names either (see Spec conflicts).
- GAL-17: switching holo on from the menu does not play the CNV-30 sweep on the card (`GalleryImage` draws `holoSurface(item.holo)` without a sweep trigger); the foil fades in.
- GAL-21: the viewer decodes once at 2048 px and does not tile when zoomed.
- GAL-22: nothing in this module (export is `domain:library`).
- GAL-24 rows: the `목록 ↔ 카드 ↔ 격자` thumbnail morph; the 8 dp rise with a 40 ms stagger for added cards (they fade in through `civilightItem`); the card collapse on delete is the lazy row's fade and placement only.
- Inner screen: the menu is the shell's centred sheet, not a popover; the info sheet is not a side panel.
- Release fixes 2026-11-23 (partial until the next emulator pass): inner `목록 + 상세` now lifts a picked row or tile line out from under the bottom bar (`GalleryReveal.applies` is true for `목록` · `격자` in every layout); the column's end padding was already `scaffold bottom + 24 dp` in every view and layout and is unchanged. `이미지 정보` and `태그` sheet fields keep their whole block (label and box) in the sheet's scroll on focus and while the keyboard rises (`FieldInView`, `GalleryFieldInViewTest`); the writers' focus handling is untouched.
- Emulator checks: the add sheet's photo and file pickers with several images (order kept, toast, pick after add, `되돌리기` restoring the pick); the info sheet's typing (one merged undo step per field, Hangul composition, `@` picker stacked over the sheet, `설명 쓰기` focus); the menu's order buttons while the menu is open; `초상으로` from a hidden item (note above the crop controls) and after process death; `자르기` back and `적용`; the order screen at 128 items (`이 뒤로`, row slide, scroll to the moved row); `여러 장 고르기` checkboxes on cards (top right, `⋯` gone), on tiles and rows; grouped project gallery with 162 / 132 cards, the arrow on small cards and the lock row; hiding an item while locked (it stays until relock); reduced motion and animator scale 0 for every new fold, swap and mode change.

**Defaults used (proposed copy, to confirm)**
- `사진` / `파일` add at once on return (55 GAL-11 "appended in the order the system returns them", T-GAL-21) rather than joining the numbered choice.
- `+ 태그` opens a `이름` line with `넣기` (the cast tag sheet's pattern); `지금 배경` (pattern of `지금 초상`); `숨김 풀기` in the `여러 장 고르기` bar when every chosen image is hidden; `항목 고르기` as the `@` picker title of `설명`; the copy and tag pickers are titled `다른 곳에도 넣기` and `태그`.
- Preset items cannot be cropped (CNV Q-IM-03 default in this module; 55 §6 Q14's other branch): the menu row is dimmed with `기본 이미지 · 자르기 없음`, and `초상으로` with a preset places it at once.
- Hiding an item while locked folds it into the lock tile at once (GAL-19); unlike a page just hidden, nothing is opened, because opening would show every hidden image without 지문 · PIN.
- The menu's order row shows `맨 앞` · `앞으로` · `뒤로` (board); `맨 뒤` is on the order screen's bottom bar.

## Defaults used

Batch 1:
- **`노드` tool** (Q-CV-02): places a 세력 · 사건 · 용어 item (segmented `세력 · 사건 · 용어` above the picker). Taps on nodes and lines select them and open their surface with every tool except `선`. The default tool is `노드`.
- **Empty-space taps**: the first tap away from a selection only clears it. Otherwise the current tool acts. One finger on empty space never pans. A mouse wheel zooms.
- **Zoom** (Q-CV-07): 25 % … 200 %, ×1.25 per button, around the view centre.
  - The zoom column shows on cover and inner screens. The inner board draws none, but the buttons are the visible alternative to pinching.
  - The first open fits the canvas between the bars, never past 100 %.
- **Lines** (Q-CV-08): tap A then tap B. No self-line. One line per unordered pair (the existing one opens instead). No line to a 묶음 frame. The label surface opens right after.
- **Label surface** (Q-CV-09 … Q-CV-11):
  - No kind is preselected, and `관계로 만들기` is off until one is picked.
  - The start is the lens episode (`최신` = the 집필 선 episode), and it can be changed to any episode or scene.
  - The label text stays on the line and is not stored as the cell's 설명.
  - `닫기` drops the draft (typed text is not kept). A draft closed by tapping the scrim is kept for the screen visit.
  - An already converted line opens the same rows and adds a record to its relation.
  - Pair 사이 kinds show as `선배 ↔ 후배` chips. The direction follows `domain:cast`: the drawn-to end holds the first name.
- **Converted line without a value at the lens** (Q-CV-12): a plain grey line with its label text. **Deleting a converted line** (Q-CV-13): removes only the canvas line.
- **No canvas** (CNV-02 [PROPOSED]): a writable project gets one named `캔버스` (no source names it). A read-only project shows `캔버스 없음` (proposed).
- **Read-only (the example)**: tools stay visible. Creating, dragging and the sheet buttons do nothing and say `읽기 전용` (a toast, or a line in the sheet). Pan, zoom, lens and the last-opened canvas still work, because they are app state.
- **Glass over the surface**: the tool bar and zoom column use the SCRIM fill (`GlassBackground(source = null)`), because they are drawn inside the scaffold's content, which has no glass source of its own. Inner popovers use the SCRIM fill too (DS-26: a popup cannot sample the backdrop).
- **Item nodes and the lens** (Q-CV-05): plain current names (the board shows `하르트` at `최신 · 1-7`).
- **Undo step names**: the tool or button copy: `노드` (also for removing a node), `메모`, `묶음`, `선`, `그냥 선으로 두기`, `관계로 만들기`, and `노드는 끌어서 이동` for a move. Batch 3: `고르기` (preset), `갤러리` / `파일` (an original added to `내 이미지`), `적용` (crop), and `선 빼기` (line off the canvas, proposed).
- **Text fields**: memo and 묶음 text is held in the surface while it is open and written through with one merged undo step per node. Line-label drafts are snapshot state in `LineLabelEditor`, so typing never waits for the store. Cards wrap their text at 240 dp.
- **Canvas viewport**: `_app/view.json` `canvasViewports` (offset in dp + zoom), saved at the end of each pan, pinch or button press. **Last opened canvas**: `_app/view.json` `lastCanvasKey`, set when a canvas opens. Neither is ever exported.

Batch 2:
- **Hidden and deleted items on the canvas** (CNV-11 [PROPOSED], Q-CV-14):
  - A node is a reference, so a trashed item's node stays, dotted with `◇`.
  - `되살리기` shows while the item is in the trash. `이대로 두기` shows until it is chosen (stored with `domain:trash` `Fixes.keep`).
  - The node can still be removed from the canvas.
  - A relation in the trash leaves a plain line that keeps its relation key, so restoring the character brings the labels back.
  - A locked hidden item offers `지문으로 보기` and cannot be opened.
- **Page section line** (Q-PG-03): `Page.sectionLabel` is free text shown as `페이지 · <label>`. A page without one reads `페이지`.
- **`연결 N`** (Q-PG-04): distinct items and pages this page links to, plus other live pages linking to it. Computed, never stored.
- **Page block kinds** (Q-PG-01): `본문 · 소제목 · ⌁ 작가 메모` (spec §3.1 PageBlock). The `/` sheet title is `블록 추가`, from the hint. `/` on an empty block turns it; otherwise it adds after the caret block.
- **Page `⋯` menu** (Q-PG-02): `고치기 · 페이지 머리 이미지 · 숨기기 / 숨김 풀기 · 지우기` (vocabulary words). Its accessibility label is `페이지 메뉴` (pattern `<name> 메뉴`).
- **Hiding a page**: like a project just hidden, the project's hidden items stay open (`unlockItems`) until the next re-lock, so the page does not vanish from under the author.
- **Deleting a page** (Q-PG-08): to the trash through `domain:trash` `TrashOps`, with its delete toast and `되돌리기`. The trash lists it under `전체`. The screen pops.
- **Unnamed page**: `미정` (the trash area's word for an unnamed thing) until it has a name. A page link with nothing left reads `지운 페이지`.
- **Editing a page**: tap into the text, `고치기` in the menu, or the command bar. The name and body write through as typed, with one merged undo step per field. Back ends editing the user started (tap, `고치기`, command bar) first; a blank page that opened straight into editing leaves on the first back (`PageBackRules`, 2026-11-23: ending such a session changed nothing visible, so that back read as ignored). The editor writes its last change as it leaves. Each editing session gets a fresh editor, seeded from the stored page, so changes made in read mode are never overwritten by an old draft. A page with a link to a locked hidden item asks `지문으로 보기` before editing (the editor shows stored link text).
- **Page from a record** (Q-PG-06): the copied text, the name prefill and the source-mark text are `domain:recorder`'s (`PageInsert`, `SourceMarks`). The page shows the mark under its meta line.
- **Page list** (Q-PG-05): live pages in their order. The section line is `페이지 · N`. The row sub line is `<section label> · M월 D일`. Locked hidden pages collapse into `숨긴 페이지 N` (pattern `숨긴 프로젝트 N`). The card view is one row of 최근 연 파일 cards, with the 21:9 header image or the initial letter.
- **`목록 | 카드`** (Q-IM-02): `목록` first, remembered per screen in `AppSettings.viewByScreen` (app preference, never in `.civilight`).
- **Card snap**: the nearest card's left edge at the row start, with the DS `enter` curve over 320 ms (the decelerate token). Reduced motion keeps the snap and drops the parallax.

Batch 3:
- **Picker titles and chips** (Q-IM-05): `표지 이미지` and `카드 3:4 · 목록 1:1` as drawn. Other slots: title = slot name (`인물 초상`, `인물 배경`, `페이지 머리 이미지`; the card slot uses `카드 참고 이미지` because its slot name is `장소 · 용어 · 세력`); chips from the main tab (`카드 4:5 · 아바타 1:1`, `커버 9:16 · 펼친 화면 4:3`) and the plain ratio for one-ratio slots (`16:9`, `21:9`). Crop header short word: `표지`, else the same title.
- **`내 이미지`** (Q-IM-03): this project's originals picked from the gallery or files (map bases, sketches and recorder audio are not listed), newest first. The tile caption is the pixel size (`3024 × 4032`). Presets are not cropped: `고르기` places them at once. A `내 이미지` tile always opens the crop editor (new crop, or re-crop starting from the slot's crops when it is the same original); this is also the re-crop path ("언제든 다시 자르기").
- **Crop controls** (Q-IM-04): zoom 1.0× … 4.0× in 0.1 steps on the DS step slider (no free value); drag moves the picture, pinch zooms; corner marks do not resize the frame; `90° 회전` is clockwise; the readout's `원본` is the file's own size (not rotated). Each ratio has its own rotation and mirror, and `처음대로` resets only the current ratio. `적용` saves the ratios that were edited or opened (the first ratio always); other ratios keep their crops for the same original, or have none (drawn centre-cropped) for a new one.
- **Picker and crop on the inner screen** (Q-IM-01): the picker is the shell's centred sheet; the crop stage fills the window and the controls sit in the 560 column.
- **Holo**: initial toggle = the slot's current value, else `홀로그램 효과 기본`. Device tilt ±25° = full foil travel, measured from how the device was held when the first holo surface appeared. Without a sensor: drag, then a damped sway (period 1.4 s, 4 s to rest). The sweep plays only when holo is switched on in the picker or editor, not every time a holo image scrolls into view.
- **Copy of the experiment** (CNV-31): tab labels are the three board titles; screen title `홀로 실험` (the feature name) under `실험`.
- **Line removal**: a text button under the label surface's two buttons; a converted line keeps its relation (Q-CV-13 default).

## Spec conflicts

- 55 GAL-11 / T-GAL-21 read as "`사진` · `파일` add on return", T-GAL-20 as "chosen through `사진` … and `넣기` is tapped". This module adds on return (one `add` call either way); the toast and counts of both cases hold.
- 55 GAL-19 asks `이름 가리기` (LIB-27) for owner names in gallery headers, menus and trash rows; LIB-27 here covers widgets, notifications and recents only, and no project screen masks names inside the project. Not applied (record for the spec owner).
- 55 §6 Q14 defaults to cropping presets in registry pixels, but this module's CNV Q-IM-03 default is "presets are not cropped" and presets have no registry pixel size here, so the spec's other branch is used (`자르기` disabled, `기본 이미지 · 자르기 없음`).
- None found between the spec cases and the fixture for CNV-01 … CNV-16, CNV-21 … CNV-31 and CNV-45.
- The crop frame's dim: the spec says `rgba(5,6,8,.62)`, DS `FixedColors.cropDim` is `.55`. The DS component is used as is.
- T-CNV-04 and T-CNV-07 are written for the relationship map (cast-owned), so they are tested here as their canvas counterparts.
- T-CNV-20 leaves open whether the 연결 goes to the page or the project. `RecorderActions.makePage` covers the project, and the test accepts that.

## Requests

- **0.2.0 (canvas lane):**
  - **feature:script (U-15.4 에셋):** the asset list and the asset picker in the 연출 input are `feature:script` (`AssetsScreen`, `CueEditor`). `feature:canvas` has no asset surface, so the `＋ 새 에셋` row and card (`종류 · 이름 · 파일`, `CreateKind.ASSET`, only with `게임 제작 도구` on via `GameTools.on`) were not built here.
  - **core:designsystem (ICON A6):** `SheetHeader` still draws its close as the text `닫기` (`TextAction`). The list makes it an icon-only `close` (`IconButtonSpec.CLOSE`). One change there covers every sheet head in this module (about 15).
  - **core:designsystem (ICON B21):** `LockRow`'s action (`지문으로 보기` on the page list and page doc) is text only; the list wants the `fingerprint` icon before it.
  - **Copy to confirm:** `캔버스 목록` (accessibility name of the head name `▾`), `노드 N` (list row sub-line), list sheet title `캔버스`, rename card `이름 바꾸기` with `이름` and `바꾸기`. These are existing vocabulary words; no `TODO_COPY_*` is left.
  - **core:designsystem / core:ui (feature-local workarounds):** `CanvasTopBar` is a local copy of the glass top bar because `TopGlassBar` takes a plain title (an `onTitle` on the shared bar would remove it). The new-row highlight after `만들기` is a feature-local `hl` flash (`common/CreatedFlash.kt`) drawn behind the row, or over a page card; a DS-72 highlight state on `ListRow` (and on cards) would replace it.
  - **Deferred (not in 0.2.0):** canvas reorder. 00 §13.6 names it, the N-58 scope row does not; `CanvasActions` has no reorder and no control is drawn (nothing visible). Request for the next wave: `CanvasActions.reorder` (domain:actions), then a list-sheet control. `lastCanvasKey` lives in `_app/view.json` (DECISIONS, code); 00 §13.6 says `_app/activity.json`, and the spec should follow.
  - **`PageCopy.MENU`** is `페이지 메뉴` (board label kept); the icon list's ⋯ default is `메뉴`.
  - **Trash:** a deleted canvas is listed in the project trash under `전체` (kind word `캔버스`, `domain:trash` `TrashListing`), so it stays restorable after the toast.
  - **S-6 menu shape:** the page ⋯ (`고치기 · 숨기기 · 지우기`) is on the page head; the canvas has no head ⋯, and its actions (`이름 바꾸기 · 지우기`) are on the list row ⋯, drawn with the same `ActionRows`. If S-6 means a head ⋯ on the canvas too, it needs a spec line.


- **Gallery part 2 (step 3):**
  - **core:model / CNV-25:** an `ImageSlot` (or slot-registry entry) for gallery items. The crop editor now takes `CropTarget.GALLERY` with the project-cover slot as a stand-in for its first frame, and the `ImageSpecs` gallery row is drawn by hand.
  - **core:ui (optional):** a size parameter on `GalleryCard` for the grouped rows (162 × 216, 132 × 176). Today a leading size modifier wins over the card's own size; the title type stays the large card's.
  - **feature:library (trash, LIB-38):** `되살리기` and `영구 삭제` of `galleryItem` trash rows through `GalleryActions.restore` (`authenticated` for a hidden image) and `GalleryActions.purge`, with the row title (`제목 없음`) and sub `<owner> · 갤러리` from `domain:trash` `TrashListing`.
  - **feature:world / feature:map:** the system photo button of the map base import is still `갤러리` there; 55 §6 Q1 renames it to `사진` everywhere (this module's picker is done).
  - **feature:cast (GAL-01):** the profile section's `넣기` can open `GalleryAddSheet(p, CHARACTER, c)` now; the sheet picks nothing outside the gallery screen. A card tap opens `Gallery.ofCharacter(p, c, pickKey)`.
  - **Shared (LIB-27):** if `이름 가리기` should mask owner names inside a project (55 GAL-19), a shared helper for every area; none exists today.
  - **feature:canvas pages (own lane, next wave):** the page menu's `숨기기` calls `unlockItems` after hiding, which opens every hidden item of the project without 지문 · PIN when it was locked. The gallery no longer does this; the page should follow.
  - **Spec 55 (to confirm):** the copy under "Gallery part 2 › Defaults used".

- **Gallery part 1 (step 2):**
  - **DS:** a dotted text underline for `지운 씬 · <code>` (GAL-08). The `NodeCard` / `ListRow` dotted look exists only for outlines and titles.
  - **feature:story:** draw `SearchResult.gallery` hits and open `Gallery(p, kind, owner, pickKey = hit.itemKey)`. The gallery screen applies `pickKey` once and scrolls the item into view.
  - **feature:cast** (GAL-01, GAL-02) and **feature:story** (GAL-03): the route is registered now, so `Gallery.ofCharacter` / `Gallery.ofProject` open the real screen.
  - **core:ui (optional):** a shared `GalleryImage` / `BlobImage` (now in `feature:canvas` `image/`), so cast's profile section can draw gallery crops without copying canvas code. This joins the earlier `image/` move request.
  - **Spec 55 (to confirm):** the copy listed under "Gallery part 1 › Defaults used".

- **feature:story (CNV-14, Q-PG-05):** add an entry to the page list, `navigator.push(PageList(p))`, from the project home (a `페이지` shortcut or row) once the spec owner places it. Until then the list and its `+` are unreachable. `PageDoc` is reachable from recents, recorder placements and links.
- **core:store `BlobReferences` (CNV-27, CNV-29):** count the `assets.blobs` rows of image originals picked from the gallery or files (`BlobSource.GALLERY` / `FILE`, `image/*`) as references, so `내 이미지` keeps an original that no slot uses yet. Today they are metadata only and the file is collected after the grace period.
- **Move `image/` to a shared module (core:ui) as well** (batch 3): `PresetImages` + `PresetArt` (cast's `PresetPainter` and world's neutral shard can go), `ImageSlots`, `HoloRules` / `HoloLayer` (`holoSurface`), `SlotImage`, `MineThumb`. Only `pick/` and `crop/` need the project (view models). Until then other lanes draw presets and holo with their own copies.
- **feature:library: entry points** (Q-IM-07): `설정 · 지도 · 이미지 · 연표 › 기본 이미지 · 보기 ›` → `navigator.push(PresetImages)`; an entry for `ImageSpecs` and `HoloExperiment` once the spec owner places them (nothing pushes them now). Also: the project cover can now be picked with `ImagePickSheet(p, PROJECT, p, PROJECT_COVER)`.
- **Move `card/` and `image/SlotImage` to a shared module (core:ui):** like the graph engine, so the 서재 (LIB-04/05), 인물 (CST-04) and world cards can use one `SnapCardRow` (snap, tick, parallax), `ProjectCard` / `LockedCard` / `NewItemCard` / `RecentFileCard` / `PortraitCard`, `ImagePlaceholder` and `SlotImage`. They have no project types beyond `ImageUse` / `Ratio` (core:model).
- **feature:cast (info, their request to core):** a remembered `목록 | 카드` per screen already exists as `AppSettings.viewByScreen` with `stores.settings.setViewMode(screen, ViewMode)`. The page list uses it with the screen id `pages`.
- **Copy proposed in the motion wave (to confirm).** No spec or board gives these. The `TODO_COPY_CANVAS_*` constants are gone (a test fails if one comes back); each now holds the nearest existing wording:
  - `축소`: accessibility label of `−`, the pair of `확대`.
  - `캔버스에서 빼기`: node edit, takes the node off this canvas (CNV-10 minimum function; the project item stays). Also the task's example.
  - `선 빼기`: takes a line off the canvas (CNV-07 loose end; a plain line is removed outright with an undo toast, no trash entry, so not `지우기`, which the vocabulary keeps for "to trash"). The task's example was `선 지우기`; use it if the spec owner wants the line to go to a trash.
  - `캔버스 없음`: a read-only project without a canvas (Q-EM-01).
  - `새 페이지`: the page list's `+` label and its undo step.
  - the empty name field of a new page now shows `미정` (the existing unnamed-page word).
  - `페이지 없음`: a project without pages (Q-EM-01).
  - `항목 고르기`: title of the `@` picker and `다른 항목에 잇기` picker in pages.
  - `내 이미지 없음`: the picker's `내 이미지` tab with no images (Q-EM-01).
  - `이미지 가져오기 실패`: toast when the gallery or file pick is unreadable (Q-EM-01).
  - `이미지 열 수 없음`: the crop stage when the original is missing or cannot be decoded (same pattern as the shell's `프로젝트 열 수 없음`).
  - The name of a canvas made automatically is still `캔버스`.
- **Copy to confirm (batch 3):** the picker titles and ratio chips of non-cover slots, `홀로 실험`, the tile caption `W × H` (see Defaults).
- **DS (optional):** `CropFrame` draws no corner marks; the editor draws them itself. `ImageTile` has no check badge (board: 24 × 24 `acc` with a `bg` check).
- **Copy to confirm:** `페이지 메뉴`, `블록 추가`, `미정` (unnamed page), `숨긴 페이지 N`, `지운 페이지`, and the menu words `고치기 · 숨기기 · 숨김 풀기 · 지우기` on a page (see Defaults).
- **Done since batch 1 (core pass):**
  - The graph engine is in core:ui.
  - The `LineLabelSheet` character keys exist; the pair case is still unused, see Partial.
  - `lastCanvasKey` is used.
  - `PickKind.EVENT` / `EventPickerSheet` is used.
  - `IconId.Minus` is used.
  - The dotted `NodeCard` is drawn by core:ui.
- **Cross-lane requests handled in batch 2:**
  - recorder "canvas lane: push `Recording` with `targetKind = page`": done from the page's command bar mic. The canvas board draws no command bar (Q-CV-15), so it pushes nothing.
  - cast and world "register `ImagePickSheet` / `ImageCrop`, share the preset painter": **done in batch 3** — both are registered (cast's portrait and background, world's `CARD_REFERENCE` sheets now open the real picker and editor). The preset painter cannot be shared from a feature module; see the move request above.
- **core:ui / core:navigation (motion wave):** give the `ImageCrop` route a fade-only transition (S2-style route metadata), so the screen fades while the thumbnail grows (audit §4 canvas 2). The route is a full-screen push today, so the shell's push slide runs under the growing thumbnail.
- **core:ui `GraphSurface` (S14 left):** the draw-progress of new lines and the fade of removed lines (a removed node fades, its lines vanish at once). Nothing to change here.
- **DS `Popover`:** the inner popover animates in and on an outside tap, but a programmatic close (selecting another node, the tool bar) removes it from composition and so closes at once. If that reads badly on the emulator, the DS popover would need a `visible` input.
- **Emulator pass items specific to this module:** the second-card nudge may move only the card text (the shared image is drawn by the transition overlay, outside the card's layer); the tool bar's `animateContentSize` sits after the caller's `fillMaxWidth`, so inner → cover may snap; an unsaved view does not refit after a fold or unfold (the fit is latched);  the whole-screen crossfade of loading, no canvas and board inside `CanvasBoardScreen` (`CivilightScreen` nested in `AnimatedContent`); the cropped-frame match between the picker thumbnail and the stage (a re-crop of an image with an existing crop starts from a centred thumbnail); the flip animation squashes the picture through zero width.
- **FEATURES.md:** rows CNV-01 … CNV-11, CNV-13 … CNV-16, CNV-21 … CNV-31 and CNV-45 → partial; CNV-12 → done. This module may not edit that file.

## 0.2.0 integration (wave 2, orchestrator)

Applied: page and gallery command-bar mics removed (S-8); `SheetHeader` close is the A6 icon. Not applied: `ListRow` highlight state (local `createdFlash` kept), tappable `TopGlassBar` title, `CanvasActions.reorder`, asset create in place (feature:script; asset create rows stay unbuilt).


## 0.2.0 integration (wave 2 requests applied)

Applied in shared modules: `core:ui` pickers take `creator: PickerCreator?` (U-15.3 create row, picker-mode card, `상위 지역` picked in place, the new item returned as the pick; written through `domain:actions` `CreateActions.inPicker`; no row in read-only projects); trashed tags make no picker chip, subtitle or match (N-44). `core:navigation`: SCRIPT-owned `EpisodeVersionsSheet` · `VersionCompareSheet` · `ManuscriptReplaceSheet`; routes `MarksList`, `SettingsLibraryView`, `SettingsCardView`. `core:model` `TimelineEvent.relatedCharacterKeys` (additive; merges remap it). DS: `TagColor` palette, `TagDot`, `TagColorRow`, `FilterChip(tagColor)`, `LensChip(small)` (the `CivilightScreen` head chip is small), `ListRow(highlighted)` and `Modifier.createdHighlight`.

Canvas: item pickers (인물 · 세력 · 용어 nodes) have create rows. Local `createdFlash` kept (card shapes and outsets the DS highlight does not draw).
