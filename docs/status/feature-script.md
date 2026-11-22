# feature:script

Package `io.github.lenticualrstone.civilight.feature.script`. Spec: `docs/spec/10-script.md` (SCR). Batch 1 of 6 (2026-11-22): SCR-01, 04, 05, 06, 12, 14, 15, 16, 17, 18, 19 (feature side), 20.

**Build and tests.** `tools/gw :feature:script:compileDebugKotlin :feature:script:testDebugUnitTest` exits 0: 65 JVM tests in 11 classes, all passing (`CommandRoutingTest` 3, `EditorRulesTest` 3, `FieldDiffTest` 8, `RunEditTest` 9, `RunLayoutTest` 6, `ScriptCopyLintTest` 3, `ScriptEntriesContractTest` 1, `ScriptOpsTest` 15, `ScriptRowsTest` 8, `ScriptTargetTest` 3, `WritingLineTest` 6). No domain module was changed. Nothing was run on a device or emulator, and no APK was built.

## Architecture

```
feature/script/src/main/java/…/feature/script/
  ScriptEntries.kt      entries function (shell.md §1): entry<Script> only
  ScriptScreen.kt       the screen: GlassScaffold + SheetHostLayout, bars, sheets, focus, writing line
  ScriptViewModel.kt    ProjectViewModel subclass: content flow, editor state, op queue, text field
  ScriptCopy.kt         UI copy not produced by domain:script (DECISIONS §3)
  state/                pure Kotlin, JVM-tested
    ScriptTarget.kt     route → OpenTarget (episode, scene, block, caret)
    ScriptRows.kt       domain Manuscript rows → ScriptRow (HeadRow, HiddenSceneRow, EmptySceneRow, BlockRow), ScriptRowKeys
    EditorState.kt      selection, caret, virtual line, paren mode, held speaker; EditorRules reducers
    HeaderModel.kt      header line and position label (rule 22 §6)
    ScriptOps.kt        edit operations → Outcome(doc, caret, select, toast, inherited, created)
    CommandRouting.kt   submitted command-field text → CommandAction
  editor/               text-field mechanics, JVM-tested
    FieldDiff.kt        sentinel field, classify a change (text, Enter, Backspace at start, parens)
    RunEdit.kt          range replace on runs that keeps untouched runs (links keep keys)
    RunLayout.kt        runs → drawn text + spans, raw ↔ display offsets
    WritingLine.kt      fixed writing line: WritingLine.adjust, WritingLineTracker, CaretEvent
  ui/                   thin composables
    Manuscript.kt       LazyColumn of rows, empty-episode state, new-block enter motion
    BlockViews.kt       BlockView, BlockFrame, SlotLine, HeadView, HiddenSceneView, EmptySceneView
    EditableBlock.kt    the block with the caret (BasicTextField + RunsTransformation)
    SpeakerLineView.kt  ScriptSpeakerLine (DS speakerLabelParts)
    ScriptBars.kt       ScriptTopBar, EditingTopBar, ScriptBottomBar, ScriptCommandField, KeyboardToolbar
    ScriptSheets.kt     feature-local sheet keys + content: BlockAddSheetKey, SpeakerSheetKey, LinkSheetKey, PositionSheetKey
```

**Data flow.**
- `ScriptViewModel.content` combines the store's `Ready` doc, the resolved `OpenTarget` and the hidden-item unlock state. It builds `ScriptRows` on `Dispatchers.Default`.
- Every write goes through one op queue (`enqueue`). Each op runs inside one `ProjectViewModel.update`, so one user action is one undo step (SCR-17).
- Typing: while a block holds the caret, the text field (`fieldValue`, sentinel + raw text) is the source of truth. `editRuns` follow it through `FieldDiff` + `RunEdit`, and are written to the store 500 ms after the last keystroke with `mergeKey = "type:{block}"`. Pending text is also written before every structural op, on `ON_STOP` and in `onCleared`.
- Block operations call `domain:script` (`BlockOps`) through `ScriptOps`, per DECISIONS §1. The speaker restore in the picker calls `domain:actions` `CharacterActions.restore`.
- After an op, `apply(Outcome)` moves the caret or selection, emits `scrollTo`, marks `entered` (enter motion) and shows the op's toast with `되돌리기`. The toast undoes only that step: it checks `undoCount`.

**Screen rules worth knowing.**
- The screen uses `GlassScaffold` directly, not `CivilightScreen`, because it swaps the 118 dp header for the 76 dp editing header and the command bar for the keyboard toolbar. It draws the toast host itself, the same way `CivilightScreen` does.
- Rows are rebuilt off the main thread, so the editor state can run ahead of them. Scroll requests (`scrollTo`) wait in state until their row exists, and the focus effect is keyed on "caret row is in `rows`".
- Focus: one `FocusRequester` follows `EditorState.caretRowKey`. When the caret row exists and no sheet is open, the row is scrolled into view, focus is requested (retried over a few frames until the field attaches) and the keyboard is shown.
- Sheets opened while typing (`대사 ▾`, speaker, `@`) hide the keyboard first, because DS sheets do not move above it. The caret stays, and the keyboard comes back when the sheet closes.
- A field that loses focus calls `onFieldFocusLost(rowKey)`. This is ignored when another row already holds the caret, and while a sheet is open, so the toolbar's sheets keep the caret.
- The fixed writing line: `EditableBlock` reports the caret line in root coordinates with a `CaretEvent`. `WritingLineTracker` returns the scroll delta, and the screen applies it with `listState.scrollBy`.
- The IME inset is applied once: the keyboard toolbar's spacer while editing, `ime − navigationBars` padding under the reading bottom bar.
- Inner screen: a 560 dp column at x 40, with the header in `wide` mode. There is no side panel yet.

**How to extend (next batches).**
- New sheets:
  - Feature-local: add a `@Serializable` key in `ui/ScriptSheets.kt` (or a new file per sheet) and a branch in `scriptSheet(...)` in `ScriptScreen.kt`.
  - Shared (`SceneSplitSheet`, `SceneMergeSheet`): register in `scriptEntries` with `deps.sheets.register<…>`.
- New routes (`ScriptAssets`, `ScriptFieldSettings`, `TransitionTextSettings`): add `entry<…>` in `ScriptEntries.kt`, with a screen file per route.
- New block ops (select mode, move, delete, kind change): add a pure function to `state/ScriptOps.kt` returning `Outcome`, then a VM method that runs it with `runOp(label) { … }` + `apply(...)`.
- Panel tabs (SCR-30…35): the selected block is `EditorState.selected`, and the header scene is `HeaderModel.sceneKey`. The 290 dp panel goes right of the 560 column in `ScriptFrame`.
- Layers sheet (SCR-26): `ScriptTopBar` / `ScriptBottomBar` take `onLayers` (null = disabled). Layer state is `doc.app.view.script.layers`, already read by `ScriptRows` and `Manuscript`.
- Scene head ⋯ (SCR-47): pass `onMenu` to `HeadView` (null today).
- `◆` toolbar item (SCR-41): enable it in `KeyboardToolbar`.

## Done

No feature id is done: every id of this batch is a UI feature and stays `partial` until it is seen on a device. The logic slices below are complete and tested:
- **SCR-15 field mechanics.** `FieldDiff`: Enter becomes a block op, Backspace at the start goes through the sentinel, `(`/`)` toggle paren mode, the caret-anchored diff handles repeated letters and Hangul recomposition. `RunEdit`: edits keep link keys and paren runs, a deleted whole link becomes plain text, multi-line paste makes `Break` runs. `RunLayout`: `(…)` drawn outside the raw text, raw ↔ display round-trip, paren layer off, pending `()`, hidden-link mask. Tests: `FieldDiffTest`, `RunEditTest`, `RunLayoutTest`.
- **SCR-14 fixed writing line math.** `WritingLine.adjust` and `WritingLineTracker`. Tests: `WritingLineTest`.
- **Entries contract.** `ScriptEntries.scriptEntries(EntryProviderScope, FeatureDeps)` resolves by the names in `FeatureContract.bindings`. Test: `ScriptEntriesContractTest`.
- **SCR-12 command routing.** `/` → sheet, `/대 … /메` → add, `/씬` → new-scene routing, `#043` / `씬 3` / title → jump, `렌즈를 이 씬으로 맞추기`, else search. Test: `CommandRoutingTest`.

## Partial

Every UI feature below is built and wired to the store, but nobody has seen it on a device yet. Boards compared in this batch (HTML text and layout): `EditKeyboard`, `Blocks`, `MType`. `Main`, `Picker` and `Wide` were followed through the spec's descriptions, not opened.

- **SCR-01 script screen (cover).**
  - Built: header, manuscript, bottom bars, scrolling once to the route target (block or scene head), the empty-episode state (`씬 없음` + `새 씬` → `NewUnitSheet`), and the empty scene's virtual dialogue line. Head tap opens `SceneWhereSheet`; the mic pushes `Recording(surface = "commandBar", projectKey, caretBlockKey)`.
  - Built: the header title (project name, Q-01), the position line, `이 씬 · 1-7` with its menu `렌즈를 이 씬으로 맞추기` (SCR-02 rows reused from `SceneText`), the speaker pill, `줄바꿈`, the command field with the mic.
  - Missing: the layers button is drawn disabled (SCR-26 is a later batch). The head `⋯` is not drawn (SCR-47). The read-only banner is not drawn (SCR-60).
  - Inner (SCR-03): only the 560 dp column at x 40; no side panel.
- **SCR-04/05/06 block rendering.**
  - Built: dialogue (speaker line, serif line, `(…)` paren runs, marks), narration (no speaker line, `◇` at the end), plus system text, memo `⌁ 작가 메모`, choice and direction rows, read-only.
  - Built: the selection look (`sel` + 2 dp `acc` bar), `#043` on the selected block, link decorations (live, hidden mask, deleted dotted). Tests: `ScriptRowsTest`.
  - Missing: on-device check of DS text metrics; choice/direction editing is a later batch (SCR-08/09).
- **SCR-12 block add sheet.**
  - Built: the rows and shortcuts from `ScriptCommands`, the subline `지금 블록 아래 · 새 대사 화자: 리엔(직전)`, read-only dimming with `읽기 전용`.
  - Built: the new block goes below the current block (else at the end of the header's scene) with the caret, a non-text kind is selected, the 8 dp / 320 ms enter motion, and a hidden layer of that kind is turned on.
  - `새 씬` opens `SceneSplitSheet` mid-scene or `NewUnitSheet` at a scene's end (SCR-49 routing). `SceneSplitSheet` is this lane's and shows the shell placeholder until SCR-50.
  - Slash shortcuts run when the command field is submitted.
- **SCR-14 editing state.**
  - Built: tap places the caret at the tapped offset, the 76 dp header `1-7 · 씬 3 · 저장됨` / `저장 중` with `완료`, `입력 중` after the speaker, autosave (500 ms) and last writing position (`ProjectActivity`).
  - Built: the fixed writing line, and system back leaves editing.
  - Unverified on a device: IME behaviour, Hangul composition across a reseed of the field, and the switch from the virtual line to the created block.
- **SCR-15 Enter / split / Backspace / `줄바꿈`.**
  - Built: all rows of the table through `BlockOps`, with the merge toast `◆ n개 #042로` (with `되돌리기`), empty-block removal with the caret at the end of the previous block, the scene-head barrier, and `줄바꿈` as a `Break` run (Q-03).
  - Tests: `ScriptOpsTest` (`T-12-08`, `T-12-09`, `T-22-07`).
- **SCR-16 keyboard toolbar.**
  - Built: the 56 dp glass strip over the IME with `대사 ▾` → add sheet, speaker (dialogue only), `( )` paren mode, `@` → `LinkPickerSheet` (the pick becomes a link run at the remembered caret), and undo (enabled by `canUndo`).
  - Missing: `◆` is drawn disabled until SCR-41.
- **SCR-17 undo.**
  - Built: one store step per op, typing coalesced per block and pause, toast `되돌리기` undoes only its own step, and the toolbar undo.
  - Missing: redo is not offered (the spec has none). Undo of later-batch ops comes with them.
- **SCR-18 speaker inheritance.**
  - Built: new dialogue (add sheet, Enter at the end, the empty scene's line) takes the nearest dialogue speaker above, with the label `직전 화자` until the first character.
  - Built: a speaker picked while the current block is not dialogue is held for the next dialogue insert.
  - Tests: `ScriptOpsTest`, `ScriptRowsTest`.
- **SCR-19 speaker picker (feature side).**
  - The shell's `CharacterPickerSheet` is fed with this scene's cast and silent cast, `recentSpeakers` (stored in `_app/view.json`, newest first), the current speaker, and `표시 이름 ???` when the display name at the block differs.
  - `지운 인물 · {name} · 되살리기` runs `CharacterActions.restore`, with its toast.
  - It opens from the speaker label, the pill and the toolbar. Opening it from `속성` and from the check center comes with those batches.
- **SCR-20 speaker label.**
  - Built: `??? 하르트`, `· #043` when selected, `화자 없음` in `warn` with `▲` for non-empty lines, deleted and hidden speakers from `Manuscript`. Tapping the label opens the picker. Tests: `ScriptRowsTest`.
- **Routes and shared sheets not registered yet** (the shell placeholder shows): `ScriptAssets` (SCR-61), `ScriptFieldSettings` (SCR-24/25), `TransitionTextSettings` (SCR-48), `SceneSplitSheet` (SCR-50), `SceneMergeSheet` (SCR-51).

## Defaults used

- **Q-01 header title:** the project name (`잿빛 새벽의 등불`).
- **Q-02 tap:** a tap on a block's text places the caret (SCR-14). Swipe buttons (SCR-30) are a later batch.
- **Q-03 `줄바꿈`:** a `Break` run at the caret. It is reachable from the command row, not from the keyboard toolbar (as drawn).
- **Empty scene:** shown as a virtual dialogue line with the inherited speaker. The block is created at the first typed character, so opening an empty scene uses no block number and adds no undo step. SCR-01 `[PROPOSED]` says "one empty dialogue block".
- **Autosave:** typing is written 500 ms after the last keystroke. The store merges steps with the same `mergeKey` within its 2 s window, so one pause gives one undo step (SCR-17 `[PROPOSED]` says 1 s).
- **Command field:** commands run only on submit (keyboard action), never while typing. `/` alone opens the sheet. Unmatched text opens `ProjectSearch(p, query)`.
- **Jump (`#043`):** selects the block without a caret and scrolls to it.
- **`◆` toolbar item and layers button:** drawn disabled (38 %) until SCR-41 / SCR-26.
- **Picker `최근`:** stored newest first, up to 10 (the picker shows 5).
- **Sheets over the keyboard:** a sheet opened while typing hides the keyboard and shows it again on close (see Requests).
- **Recorder mic:** `Recording(surface = "commandBar", projectKey, caretBlockKey = selected block)`.

## Spec conflicts

- **Merge toast count on the example project:** spec F1 has one `◆` on `#043` (`◆ 1개 #042로`). The example project anchors three, so `ScriptOpsTest` expects `◆ 3개 #042로`. This is the same fixture difference `domain-script.md` conflict 1 describes.
- **Scaffold:** shell.md §6 says screens use `CivilightScreen`. The script needs the 118 / 76 dp header swap and the keyboard toolbar, which `CivilightScreen` cannot take, so it uses `GlassScaffold` + `SheetHostLayout` and re-creates the toast host.

## Requests

- **core:ui `CivilightScreen`:** optional `topBar` / `bottomBar` slot overrides (or a public `ToastOverlay`), so the script does not re-create the toast host.
- **core:designsystem `CivilightSheetLayout`:** lift the bottom sheet above the IME (`WindowInsets.ime`). The `@` link picker's search field opens the keyboard over the lower part of the sheet; the script hides the keyboard when it opens a sheet as a workaround.
- **Copy needed (TODO_COPY):**
  - `TODO_COPY_SCRIPT_LINK_PICKER_TITLE`: the title of the `@` link picker from the keyboard toolbar (SCR-16 / SCR-55 name none).
  - `TODO_COPY_SCRIPT_UNDO_TYPING`: the undo-step label for typed text (`UndoState.undoLabel`; not shown on screen today).
- **FEATURES.md:** update rows SCR-01, 04, 05, 06, 12, 14, 15, 16, 17, 18, 19, 20 from this file (this lane may not edit it).
