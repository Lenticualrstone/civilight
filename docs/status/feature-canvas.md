# feature:canvas

All three batches are done: batch 1 built CNV-01 … CNV-10, batch 2 built CNV-11 … CNV-16 and CNV-21 … CNV-24, batch 3 built CNV-25 … CNV-31 and CNV-45 and closed the line-removal loose end. `tools/gw :feature:canvas:compileDebugKotlin :feature:canvas:testDebugUnitTest --no-configuration-cache` exits 0 with 95 JVM tests in 11 classes, all passing. No domain module was changed (DECISIONS §2: canvas logic lives in this module). Nothing was run on a device or emulator. Every screen has only been compiled, so every UI feature below is at most partial.

## Architecture

The package is `io.github.lenticualrstone.civilight.feature.canvas` (`src/main/java/...`). Composables stay thin. The rules are pure Kotlin objects, tested on `ExampleProject.build()` with fixtures S0 and S0-noRel in `src/test/.../CanvasFx.kt`.

- **`CanvasEntries.kt`** (`canvasEntries`, shell.md §1)
  - Routes: `CanvasBoard`, `PageList`, `PageDoc`, `ImageCrop`, `ImageSpecs`, `PresetImages`, `HoloExperiment` (every route of the canvas row of shell.md §3).
  - Shared sheets: `LineLabelSheet`, `ImagePickSheet` (both CANVAS-owned sheets of shell.md §5).
  - Feature-local `LineStartSheet`. It is registered because it is stacked over `LineLabelSheet` on other areas' screens.
- **Copy.** There is one object per package: `CanvasCopy` (canvas), `page/PageCopy` (pages), `card/CardCopy` (cards), `image/ImageCopy` (images) and `holo/HoloCopy` (holo experiment; `SAMPLE_` = board sample content).
  - `BTN_` keys are buttons. Missing copy is a `TODO_COPY_CANVAS_*` constant, listed under Requests.
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
- **`card/`: card view mechanics (CNV-21 … CNV-24)**
  - `CardRules.kt`:
    - `CardKind` / `CardMetrics`: sizes, gap, pitch, inset, radius.
    - `CardSnap`: `target`, `indexAt`, and `MOTION` = DS `enter` (320 ms decelerate).
    - `SnapTicker`: one haptic tick per snap.
    - `CardParallax` (8 %, none when reduced), `ViewChoice` (per-screen `목록 | 카드`, default `목록`) and `CardInitial`.
  - `SnapCardRow`: a `LazyRow` with `snapFlingBehavior(SnapLayoutInfoProvider(Start), decay, CardSnap.MOTION)`, the pager marks, a haptic tick and the parallax shift.
  - `CardParts.kt`: `ImagePlaceholder` (CNV-24), `ProjectCard`, `LockedCard`, `NewItemCard`, `RecentFileCard` and `PortraitCard`. Other lanes cannot use these until they move (Requests).
  - `CardViewMotion` (CNV-45 `목록 ↔ 카드`): the plan (grow 360 ms ENTER, list text out 160 ms EXIT, card text in after 160 ms; reduced = 120 ms crossfade, no shared images), `growSpec`, `transform`. `PageListScreen` runs it with `SharedTransitionLayout` + `AnimatedContent`: list rows now have a 48 × 48 (radius 10) header-image thumbnail that is the same shared element as the card image (`RecentFileCard(imageModifier = …)`).
- **`image/`: images (CNV-24 … CNV-30)**. Pure parts first:
  - `ImageSlots` (T-CNV-32): `SlotSpec` per `ImageSlot` (name, used-in, ratios, chip labels, `holoAllowed`, holo note, picker title, short name, owner kinds); `ratios`, `holoAllowed`, `chip`, `parts(Ratio)` → `RatioParts(w, h)`.
  - `CropMath` (T-CNV-30, T-CNV-31): rotate → mirror → rectangle, as `SlotImages.decode`. `largestFrame`, `cropSize` (rounded), `crop(source, ratio, CropEdit)`, **`cropReadout`**, `editOf(crop)` (re-crop), `clamp`, `pan`, `rotate` (90° clockwise, centre turns with the picture), `flip`, `frameIn` (same frame as DS `CropFrame`), zoom steps 1.0 … 4.0 by 0.1.
  - `CropSession` (`@Serializable`, saved as JSON in `rememberSaveable`): one `CropEdit` per ratio, `current`, `touched`, `holo`; `select · zoom · zoomStep · pan · rotate · flip · reset · holo`, `crops()` = the edited ratios only, `readout()`; `start(slot, size, existing, hash, holo)` resumes the slot's crops when it already shows this original.
  - `ImageEdits` (T-CNV-33 … T-CNV-36): `ImageOwner(kind, key, slot)`; `use`, `addBlob` (once per hash), `placePreset`, `placeBlob` (same original keeps the other ratios' crops; a new one drops them), `setHolo`. One `ImageUse` per owner × slot (replaced in place, key kept); holo is forced off where the slot does not allow it. `ImageOwners.name` and `myImages` (this project's GALLERY / FILE image blobs, newest first).
  - `PresetImages` (CNV-26): the 8 presets (ids, names, colours, shard sets `i mod 4`), `byId`, `ofSource`. Ids are the same as feature:cast's `CharacterImages`.
  - `HoloRules` (T-CNV-34, T-CNV-55): `showToggle`, `initial`, `shows`, `sweepOnTurnOn` (null when reduced), `sweepTilt`, `fromDevice` (±25°), `fromDrag`, `sway` (damped, 4 s). `HoloSensorGate` over a `TiltSensorPort`: registered only while ≥ 1 holo surface is composed and motion is not reduced and a sensor exists.
  - `CropHandOff`: in-memory, taken once — the picker's tapped tile bounds and holo toggle for the crop editor (route arguments are frozen).
  - Composables: `PresetArt` / `DrawScope.drawPreset`; `HoloLayer.kt` (`ProvideHoloTilt` = one shared tilt and sensor per screen, `rememberHoloTilt`, `Modifier.holoDrag`, `holoFoilAt`, and the one-call `Modifier.holoSurface(on, sweepOnStart)`); `SlotImage` (preset art, decoded original with its crop, holo foil when `HoloRules.shows`, placeholder with `onPick`); `MineThumb`.
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
- **CNV-07 선 긋기**: tap–tap with the `선` tool. The label surface opens on the new or existing line. Batch 3: the label surface (cover sheet and inner popover) has a text button that takes the line off the canvas (`LineLabelEditor.remove` → `CanvasEdits.removeLine`, one undo step; a converted line's relation stays, Q-CV-13). Its label is still `TODO_COPY_CANVAS_REMOVE_LINE`.
- **CNV-09 관계 선과 시점 렌즈**: done in logic; drawing not seen on a device.
- **CNV-10 노드 편집**: cover sheet and inner popover. Open the item, edit memo or 묶음 text, remove (the label is `TODO_COPY_CANVAS_REMOVE_NODE`).
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
- **No canvas** (CNV-02 [PROPOSED]): a writable project gets one named `캔버스` (no source names it). A read-only project shows `TODO_COPY_CANVAS_NONE`.
- **Read-only (the example)**: tools stay visible. Creating, dragging and the sheet buttons do nothing and say `읽기 전용` (a toast, or a line in the sheet). Pan, zoom, lens and the last-opened canvas still work, because they are app state.
- **Glass over the surface**: the tool bar and zoom column use the SCRIM fill (`GlassBackground(source = null)`), because they are drawn inside the scaffold's content, which has no glass source of its own. Inner popovers use the SCRIM fill too (DS-26: a popup cannot sample the backdrop).
- **Item nodes and the lens** (Q-CV-05): plain current names (the board shows `하르트` at `최신 · 1-7`).
- **Undo step names**: the tool or button copy: `노드` (also for removing a node), `메모`, `묶음`, `선`, `그냥 선으로 두기`, `관계로 만들기`, and `노드는 끌어서 이동` for a move. Batch 3: `고르기` (preset), `갤러리` / `파일` (an original added to `내 이미지`), `적용` (crop), and `TODO_COPY_CANVAS_REMOVE_LINE` (line off the canvas).
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
- **Editing a page**: tap into the text, `고치기` in the menu, or the command bar. The name and body write through as typed, with one merged undo step per field. Back leaves editing first. Each editing session gets a fresh editor, seeded from the stored page, so changes made in read mode are never overwritten by an old draft. A page with a link to a locked hidden item asks `지문으로 보기` before editing (the editor shows stored link text).
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

- None found between the spec cases and the fixture for CNV-01 … CNV-16, CNV-21 … CNV-31 and CNV-45.
- The crop frame's dim: the spec says `rgba(5,6,8,.62)`, DS `FixedColors.cropDim` is `.55`. The DS component is used as is.
- T-CNV-04 and T-CNV-07 are written for the relationship map (cast-owned), so they are tested here as their canvas counterparts.
- T-CNV-20 leaves open whether the 연결 goes to the page or the project. `RecorderActions.makePage` covers the project, and the test accepts that.

## Requests

- **feature:story (CNV-14, Q-PG-05):** add an entry to the page list, `navigator.push(PageList(p))`, from the project home (a `페이지` shortcut or row) once the spec owner places it. Until then the list and its `+` are unreachable. `PageDoc` is reachable from recents, recorder placements and links.
- **core:store `BlobReferences` (CNV-27, CNV-29):** count the `assets.blobs` rows of image originals picked from the gallery or files (`BlobSource.GALLERY` / `FILE`, `image/*`) as references, so `내 이미지` keeps an original that no slot uses yet. Today they are metadata only and the file is collected after the grace period.
- **Move `image/` to a shared module (core:ui) as well** (batch 3): `PresetImages` + `PresetArt` (cast's `PresetPainter` and world's neutral shard can go), `ImageSlots`, `HoloRules` / `HoloLayer` (`holoSurface`), `SlotImage`, `MineThumb`. Only `pick/` and `crop/` need the project (view models). Until then other lanes draw presets and holo with their own copies.
- **feature:library: entry points** (Q-IM-07): `설정 · 지도 · 이미지 · 연표 › 기본 이미지 · 보기 ›` → `navigator.push(PresetImages)`; an entry for `ImageSpecs` and `HoloExperiment` once the spec owner places them (nothing pushes them now). Also: the project cover can now be picked with `ImagePickSheet(p, PROJECT, p, PROJECT_COVER)`.
- **Move `card/` and `image/SlotImage` to a shared module (core:ui):** like the graph engine, so the 서재 (LIB-04/05), 인물 (CST-04) and world cards can use one `SnapCardRow` (snap, tick, parallax), `ProjectCard` / `LockedCard` / `NewItemCard` / `RecentFileCard` / `PortraitCard`, `ImagePlaceholder` and `SlotImage`. They have no project types beyond `ImageUse` / `Ratio` (core:model).
- **feature:cast (info, their request to core):** a remembered `목록 | 카드` per screen already exists as `AppSettings.viewByScreen` with `stores.settings.setViewMode(screen, ViewMode)`. The page list uses it with the screen id `pages`.
- **Copy needed (TODO_COPY):**
  - `TODO_COPY_CANVAS_ZOOM_OUT`: accessibility label of `−`.
  - `TODO_COPY_CANVAS_REMOVE_NODE`: node edit, take the node off this canvas; also the undo step.
  - `TODO_COPY_CANVAS_REMOVE_LINE`: not shown yet; removing a line from the canvas.
  - `TODO_COPY_CANVAS_NONE`: a read-only project without a canvas, Q-EM-01.
  - `TODO_COPY_CANVAS_NEW_PAGE`: the page list's `+` and its undo step.
  - `TODO_COPY_CANVAS_PAGE_NAME_PLACEHOLDER`: the empty name field of a new page.
  - `TODO_COPY_CANVAS_PAGE_LIST_EMPTY`: a project without pages, Q-EM-01.
  - `TODO_COPY_CANVAS_LINK_PICKER_TITLE`: the `@` picker and `다른 항목에 잇기` picker title in pages.
  - The name of a canvas made automatically (now `캔버스`).
  - `TODO_COPY_CANVAS_MY_IMAGES_EMPTY`: the picker's `내 이미지` tab with no images (Q-EM-01).
  - `TODO_COPY_CANVAS_IMAGE_IMPORT_FAILED`: toast when the gallery / file pick is unreadable or not an image (Q-EM-01).
  - `TODO_COPY_CANVAS_IMAGE_UNREADABLE`: the crop stage when the original file is missing on this device or cannot be decoded.
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
- **FEATURES.md:** rows CNV-01 … CNV-11, CNV-13 … CNV-16, CNV-21 … CNV-31 and CNV-45 → partial; CNV-12 → done. This module may not edit that file.
