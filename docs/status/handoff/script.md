# feature:script handoff

## batch 1

Module: feature/script. Status file docs/status/feature-script.md (Architecture section is the map).

Files:
- ScriptEntries.kt: registers only entry<Script>. Add ScriptAssets / ScriptFieldSettings / TransitionTextSettings entries and deps.sheets.register<SceneSplitSheet/SceneMergeSheet> here.
- ScriptScreen.kt: GlassScaffold + SheetHostLayout. Owns bars, the toast host, focus, the fixed writing line and command routing. Feature-local sheets are dispatched in scriptSheet(...).
- ScriptViewModel.kt (ProjectViewModel subclass): content flow (rows built on Default), EditorState, a serial op queue (enqueue → runOp(label){ScriptOps…} → apply(Outcome)), the text field (fieldValue sentinel + editRuns, committed 500 ms after typing with mergeKey "type:{block}"), canUndo, entered, scrollTo.

Pure state (JVM-tested):
- state/ScriptOps (Outcome: doc, caret, select, toast, inherited, created)
- state/ScriptRows (+ ScriptRowKeys)
- state/EditorState (+ EditorRules, caretRowKey)
- state/HeaderModel, state/ScriptTarget, state/CommandRouting
- editor/FieldDiff, editor/RunEdit, editor/RunLayout, editor/WritingLine (WritingLineTracker, CaretEvent)

UI: ui/Manuscript (LazyColumn, ManuscriptActions callbacks), ui/BlockViews, ui/EditableBlock, ui/SpeakerLineView, ui/ScriptBars, ui/ScriptSheets (BlockAddSheetKey, SpeakerSheetKey, LinkSheetKey, PositionSheetKey).

Conventions:
- Block edits go only through domain:script BlockOps (DECISIONS §1); character restore through domain:actions.
- Copy lives in ScriptCopy (linted by ScriptCopyLintTest, TODO_COPY_ skipped); domain strings (ScriptCommands, SceneText, Manuscript) are used as given.
- Field focus-loss is ignored unless that row still holds the caret, and while a sheet is open.
- Sheets opened while typing go through openOverKeyboard (hides the keyboard).
- Rows can lag the editor state; scroll and focus wait until the row exists.

Hooks left for later batches:
- onLayers is null → layers button disabled (SCR-26).
- HeadView onMenu is null → no ⋯ (SCR-47).
- The KeyboardToolbar ◆ is disabled (SCR-41).
- There is no side panel; the inner screen is only a 560 dp column at x 40 (SCR-03/31).
- `새 씬` already routes to SceneSplitSheet or NewUnitSheet (SCR-49); SceneSplitSheet content is SCR-50.
- Q-01 default: the header title is the project name.

Example-fixture note: #043 carries 3 ◆ in the example, so the merge toast is `◆ 3개 #042로`.

## batch 2

Built: the caret-on-open fix, SCR-02 (test), 03, 07, 08, 09, 10, 11, 13, 21, 22, 38, 46. Status file Architecture has the new map; read "Caret targets", "Opening" and the new screen rules first.

New files:
- layout/ScriptLayout.kt: width → cover/inner, area, column, panel. The screen measures with BoxWithConstraints; never use the window class here (two-pane detail).
- panel/PanelModels.kt + panel/ScriptPanel.kt: `PanelTab.BUILT` drives the tab strip (only `PROPS` today). Add SCR-32/33/35 tabs there and as branches of the `when (tab)` in `ScriptFrame`. Reuse the same tab composables in the cover panel sheet (SCR-31).
- renumber/: the shared `RenumberSheet` (registered in `scriptEntries`). It writes through `currentProjectStore` on `deps.appScope` because story opens it too.
- state/CueEditor.kt + ui/DirectionSheet.kt: the cue sheet; domain ops in `domain:script` `DirectionCues` (new, additive).
- ui/ScriptSheets.kt: `KindSheetKey` + `KindSheet` (SCR-13).

Conventions set in batch 2:
- One text field engine for blocks and choice options: `EditorState.option` / `caretOption`, `PendingText.optionKey`, `Caret.optionKey`, `LinkSheetKey.optionKey`. Do not add a second field engine.
- `ScriptRows.slotOf` decides the line above a non-dialogue block (number only while selected); `EditableBlock` takes the same `slot`, so the line does not jump when editing starts. The memo prefix is drawn inside the field's transformation for the same reason.
- `ScriptOps.takesParens(kind)`: paren runs only in dialogue and options.
- Ops that need the screen to open a sheet emit on `vm.sheetRequests`.
- `vm.deleteBlocks(keys)` is the delete entry for select mode (SCR-36); it already shows the domain toast with `되돌리기`.

Open threads:
- Panel reopen needs SCR-30 (no other drawn control; back does not close the panel).
- Story lane must switch to `navigation.RenumberSheet` and must not register it (double registration throws).
- `SceneMergeSheet` / `SceneSplitSheet` (SCR-50/51) are opened by story's head menu and by `/씬`; they still show the shell placeholder.
- Optional clean-up: the core pass added `CivilightScreen(topBar, bottomBar)`, `ToastOverlay` and sheets above the IME; the script still uses its own toast host and hides the keyboard before sheets.

## batch 3

Built: SCR-23 … SCR-34 (voice ID field and settings, `표시` layers and previews, 호칭 힌트, swipe buttons, panel container and the `시간 · 참고 · 속성` tabs). Status file Architecture has a new "Panel and layers (batch 3)" section; read it first.

New files and where things are:
- `layers/LayersModel.kt`, `layers/LayersSheet.kt`: `표시` sheet; layers and preview are `_app/view.json` view state written with `updateApp`. `LayersModel.effective` draws a stored `낭독` as `쓰기`; `isEditable` gates typing (`대사만` is read-only; taps call `vm.leavePreviewAt`).
- `panel/`: pure models `TimeTab`, `RefTab`, `PropsModel` (in `PanelModels.kt`), `PanelRecords`, `SpeakerKeep`; `PanelState.kt` has `PanelRequest`, `PanelContent`, `PanelBuild.of` (only the shown tab is computed) and `PanelActions`; views in `PanelViews.kt` / `ScriptPanel.kt`. `PanelTabBody(content, wide, actions)` serves the inner side panel and the cover `PanelSheetKey`.
- `ScriptPanelWiring.kt`: `scriptPanelActions(...)` maps panel buttons to sheets, routes and VM calls.
- `settings/`: route `ScriptFieldSettings` (registered in `scriptEntries`).
- `state/CommandRow.kt`: what the reading command row shows. The panel buttons there are the visible tap path to the swipe buttons (hard rule: no hidden-gesture-only path) and the way back to a closed inner panel.

Conventions set in batch 3:
- `vm.panel` follows `EditorState.selected`; the screen only sets `vm.setPanelRequest(headerScene, visible)` and `vm.setPanelTab`. `vm.openPanelAt(block, tab)` selects without a caret.
- `PanelTab.BUILT` = tabs with content (`시간 · 참고 · 속성`); `PanelTab.SWIPE` = the swipe/command-row buttons (always those three). Add `MODS` to BUILT only with SCR-35.
- `addBlock` turns the layer on before inserting (else `dropHiddenPosition` drops the new block's selection).
- Tests use the example project (`#043` has 3 ◆: 리엔의 마음, 하르트의 마음, 하르트의 호칭).

Open threads:
- `모듈` tab (SCR-35), per-tab scroll memory (`ScriptPanelState.scroll`), orphan card buttons (SCR-43), point line `블록에 잇기` (SCR-42), `용어 후보` layer row (SCR-56), `표시 이름` tap to cast history.
- `TODO_COPY_SCRIPT_REF_NOT_DIALOGUE` (Q-15) is drawn raw in `참고` for non-dialogue blocks.
- Library lane should push `ScriptFieldSettings(p)` from `프로젝트 설정` (request in the status file).
- Still open from batch 2: `SceneSplitSheet` / `SceneMergeSheet` placeholders (SCR-50/51), optional switch to `ToastOverlay`.

## batch 4

Built: SCR-35 (`모듈`), 36 (select mode), 37 (move picker + grip drag), 39 (trash note `되살리기`), 40 (clipboard), 41/45 (`◆ 변화 기록` sheet), 42/43 (orphan buttons, candidate sheet, point line `블록에 잇기`), 44 (consequences, tested), 47 (already wired to story's menu), 48 (setting route `TransitionTextSettings`). Status file Architecture has a "Batch 4 structure" part; read it first.

Where things are:
- New packages `records/`, `change/`, `select/`; `panel/ModsTab*.kt`; `settings/TransitionSettings*.kt`.
- VM queue helpers are `internal`; new ops live in extension files `ScriptRecordOps.kt`, `ScriptChangeOps.kt`, `ScriptSelectOps.kt`, `ScriptModuleOps.kt`, each over a pure function returning `ScriptOps.Outcome` (tested). Keep `ScriptViewModel.kt` from growing.
- `vm.changeDraft` holds the change sheet's draft (pickers stack over it). `vm.select` is select mode; `ScriptClipboard` is process-wide.
- `domain:script` `SceneMemo` (additive, tested) writes the `씬 메모`.

Conventions set:
- Buttons that change the project hide in read-only projects (`TimeTabModel.editable`, `PropsModel.readOnly`).
- Select mode acts only on drawn rows; checks are reconciled after every row rebuild.
- Write paths: relation records through cast `RelationDrafts.recordAtBlock`, field and display-name records through cast `TimedValues.addRecord`, reveals through world `TermEdits.setReveal`, orphan fixes through domain:script `Anchors`, block restore through domain:actions `SceneActions.restoreBlocks`.

Open threads:
- Shared keys requested (core:navigation): `RelinkBlockSheet` (story check center, world) and `BlockPickSheet` (world `원고에서 고르기`); move `RelinkSheet` content to the shared key once it exists.
- DS icons `Cut` / `Copy` requested; copy `TODO_COPY_SCRIPT_PASTE`, `…_MODS_NO_CHECKS`, `…_MODS_SCENE_EMPTY` open.
- Next batch: SCR-49 `/씬` with checked blocks (split below `SelectRules.ordered(...).last()`), SCR-50/51 sheets (still placeholders; story opens `SceneMergeSheet`), SCR-56 term-candidate layer row.
- Not seen on a device: the grip drag, the in-place memo field, the change sheet's pickers returning into the draft.

## batch 5

Built: SCR-49 (`/씬` routing incl. checked blocks and the caret at a scene's start, the new scene takes the caret), SCR-50/51 (shared `SceneSplitSheet` / `SceneMergeSheet`, registered), SCR-52/53/54 (story's sheets; script-side flush, reconcile, follow), SCR-55 (typed `@`, link taps, deleted-link card), SCR-56 (`용어 후보` layer, spans, popover, margin `▲`, WLD-22 release), SCR-57 (`대상 지워짐` card), SCR-58 (masking in the change sheet, `이 씬 분량`, split rows; `숨김` head tag), SCR-59 (recorder source tap), SCR-60 (read-only banner and footer). Read "Batch 5 structure" in the status file's Architecture first.

Where things are:
- `scenes/` (split / merge models and the shared sheets), `terms/` (TermMarksModel), `links/` (LinkFixes, LinkSheets), `readonly/` (ReadOnlyBars), `panel/SubjectDeleted.kt`, `ScriptSceneOps.kt`, `ScriptLinkOps.kt`.
- `domain:script` `WritingCounts.sceneSpeakers(doc, scene, unlocked)` (additive, tested).

Conventions set:
- Shared sheets keep only saveable choices and re-plan on the doc they write (`store.update { Model.apply(d, …) }`), returning it unchanged when stale.
- Story's `NewUnitSheet` over the script is followed (`vm.followNewScene`) while it is the top sheet.
- `BlockRow.termCandidates` are raw offsets; `RunsText` maps them with `RunLayout.toDisplay`. Taps outside typing go through `LinkFixes.tapAt` before the caret.
- `ScriptContent.terms` is the rows build's marks source (popover data).

Open threads:
- Candidate spans in choice options and direct `전환 문구`; the margin `▲` while typing.
- `TermQueries` per rows build scans the whole project's text: watch performance on big projects (a per-spot cache would help).
- Shared keys still requested: `RelinkBlockSheet`, `BlockPickSheet`.
- Batch 6: SCR-61 … SCR-69, CNV-39, CNV-40. Nothing has been seen on a device.

Build: `tools/gw :domain:script:test :feature:script:compileDebugKotlin :feature:script:testDebugUnitTest --no-configuration-cache` exits 0: feature 169 tests, `domain:script` 154 (3 ignored as before), 0 failed.

## batch 6

Built (last batch of the lane): SCR-61 (`에셋` route `ScriptAssets`, asset form, the cue picker as the filtered list with `+ 에셋`), SCR-66 (sheet ↔ side panel handover, per-tab scroll, anchor kept on resize, restore point on `ON_STOP`), SCR-67 / CNV-39 / CNV-40 (motion values in `motion/ScriptMotion`, `StaggeredSwipeRow`, `animateItem` placement where the typed row never moves), SCR-68 (`에셋` command, suggestion rows over the command field), SCR-69 (tabletop split, posture read from the window). SCR-62 … 65 have no script UI: their content is `domain:script` (checks listed by `domain:checks`, files by `ExportActions` / feature:library); `ExportContentTest` checks JSON / CSV / PDF lines on the shipping path; left: the CSV transition row's `상태` (domain:actions) and library's PDF not using `ScriptPdfContent` (Requests). Loose ends closed: per-tab scroll, the 60 ms stagger, `표시 이름` → the character's record, cue picker for empty kinds (`+ 에셋`), `ScriptAssets` registered. Read "Batch 6 structure" in the status file's Architecture first.

Where things are:
- `assets/` (AssetsModel, AssetForm, AssetFormSheet, AssetsScreen, AssetViews, AssetsViewModel), `motion/` (ScriptMotion, SwipeRow), `fold/` (FoldRules, FoldEffects, FoldMotion), `layout/TabletopLayout.kt` + `TabletopPosture.kt`, `panel/TabletopPanel.kt`, `ScriptFoldOps.kt`, `state/CommandSuggestions.kt`, `ui/CommandSuggestionRows.kt`.
- `domain:script` (additive): `ScriptAssetEdits`, `ScriptPdfContent.linesOf`; bug fix `ScriptAssets.duration` → `3분 05초` (board `Assets`).

Conventions set:
- `ScriptFrame`'s bottom bar is one lambda (`bottomContent`) used by the scaffold, or by the tabletop lower half.
- `PanelScrollMemory` is the only scroll state of panel tabs (side panel, sheet, tabletop share it).
- Forms that import a file keep the imported `Blob` in their saveable draft and list it in the project in the same update as the save.

Open threads (for a review round):
- Nothing has been seen on a device: the swipe drag vs list scroll, fold handover timing, tabletop on a real foldable, the gallery pick.
- Shared keys still not delivered: `RelinkBlockSheet`, `BlockPickSheet` (story's `다른 블록에 잇기`, world's `원고에서 고르기`).
- Q-blocked: 모듈 card reorder and goal (Q-16), paste from outside (Q-19), direct delete (Q-33), asset delete and audio files (Q-29), `참고` for non-dialogue (Q-15).
- Candidate spans in choice options and direct `전환 문구`; `TermQueries` per rows build on big projects.

Build: `tools/gw :domain:script:test :feature:script:compileDebugKotlin :feature:script:testDebugUnitTest --no-configuration-cache` exits 0: feature 190 tests, `domain:script` 159 (3 ignored as before), 0 failed.
