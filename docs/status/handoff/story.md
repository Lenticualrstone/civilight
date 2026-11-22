# feature:story handoff

## batch 1

Read docs/status/feature-story.md §Architecture first.

Layout:
- Packages: common (StoryServices, StoryViewModel base with apply(label){doc,ctx->doc'}, StoryActions pure edits, StoryCopy), home, structure, menu (⋯ models, UnitMenuSheet content, local StructureMenuSheet and RenumberSheet), newunit (NewUnitSheet content), settings (때 표기).
- StoryEntries.kt registers ProjectHome, StoryStructure and SceneNotationSettings, the shared sheets UnitMenuSheet and NewUnitSheet, and the local RenumberSheet (via deps.sheets.register so it opens over the script too).
- Later batches add their routes and sheets there: CheckCenter, ProjectSearch, LibrarySearch, BulkPlaceTime, DayBandSettings, SceneMoveSheet, SceneWhereSheet.

Conventions:
- View models are created with entryViewModel { XViewModel(route.projectKey, deps.storyServices()) } and never keep FeatureDeps or the navigator.
- Sheets get their edits from storyActions(projectKey, deps) and the store from currentProjectStore.
- To swap sheets, call close() then open() (replaceWith in MenuSheets.kt).
- Pure models carry JUnit tests named after spec case ids.
- Every …Copy object goes into StoryCopyLintTest.copyObjects, and button constant names into its buttons set.

Unfinished threads:
- STO-08: add the 옮기기 row (MenuAction) plus drag on the grip. Open SceneMoveSheet; its content is STORY-owned.
- STO-20: SceneWhereSheet content. The 장소와 때 / 전환 문구 rows already open it. Wire the 때 edit into the new-scene sheet, which can only clear 때 today.
- STO-31: BulkPlaceTime, already pushed from the structure ⋯.
- STO-43: inner layouts.
- When the SCRIPT shared renumber key exists, switch MenuAction.RENUMBER to it and drop the local key.

Test fixtures are in src/test/.../Fixtures.kt: Fx.doc() is the example, Fx.ctx(), Fx.mini(levels, nodes), and withBlock for LENS-09-style content.

## batch 2 (in progress, 2026-11-22)

- Done so far: step 0 fixes (no `TODO_COPY_` row or sub line reaches the screen: `MenuModel.drawnOnly`, `HomeStartModel.shownSub`; `RecorderInbox(projectKey = p)`, `World(…, createTerm = true)`; `새 씬` in 1-level projects), and the `where` package: `SceneWhereSheet` content + cascade (`WhenEdit`, `WhereModel`, `CascadeBody`, local `CascadeSheet`).
- Next: CheckCenter, ProjectSearch, DayBandSettings, SceneMoveSheet (see the status file once updated).
- Also done: `check` package (CheckCenter screen, `CheckFixes` routing every `FixKind`, local `PickSpeakerSheet`), `search` package (ProjectSearch overlay, `SearchModel`, `SearchViewModel`), `domain:story` `UnitSettingsEdit.setDayBoundary / setBandStart / canSetBandStart / setHidden` (+ `DaySettingsEditTest`).

## batch 2 (finished, 2026-11-22)

Read docs/status/feature-story.md §Architecture first (packages `where`, `check`, `search`, `move`, `settings` were added).

- Built: SceneWhereSheet (STO-20 … 23 + rows of STO-27 / STO-30), CascadeSheet, CheckCenter (STO-39 … 41 UI, CNV-17 shell), ProjectSearch (STO-36 … 38), DayBandSettings (STO-19), 옮기기 (STO-08: MoveTargetSheet + SceneMoveSheet; episode/chapter reorder via ⋯).
- Conventions added: no `TODO_COPY_` string reaches the screen (`MenuModel.drawnOnly`, `isDrawnCopy`, `HomeStartModel.shownSub`); typed fields use `applyMerged(label, mergeKey)`; cross-module row actions are a tested sealed effect (`check/CheckFixes.kt`, exhaustive over `FixKind`); every edit is recomputed on the doc it applies to and is a no-op on a gone unit (sheets survive process death).
- domain:story additions (additive): `UnitSettingsEdit.setDayBoundary / setBandStart / canSetBandStart / movableBands / setHidden` + `DaySettingsEditTest`.
- Unfinished threads: STO-24 flashback form (under `WhereFlashbackRow`), STO-26 등장 edit (`WhereCastRow`), STO-30 TravelBar card (sheet + check rows), STO-31 … 35 `BulkPlaceTime` (cells open `SceneWhereSheet`), new-scene sheet 때 editing via `WhenEdit`, grip drag (STO-08), `LibrarySearch` route, STO-43 inner layouts, the three hidden check buttons (Requests), switch to the shared `RenumberSheet` once SCRIPT registers it.

## batch 3 (finished, 2026-11-22)

Read docs/status/feature-story.md §Architecture first (the `where` and `check` rows were extended).

- Built (feature side): STO-24 회상 form (`where/FlashbackModel`, `WhereFlashbackForm`; event picker swapped into the sheet; `직접` year, plus month chips and a day field when the world calendar is on), STO-26 editable 등장 row (`where/CastModel`, shared `CharacterPickerSheet`), STO-11 marks (스토리/홈 `숨긴 씬` + unlock on tap, deleted/hidden place via `where/PlaceFix`, deleted/hidden silent members), STO-18 date in the result box (`where/CalendarLabels`), STO-25 order line, STO-27 row, STO-28 mode chips; check centre `되살리기` (`FixEffect.Restore` → `StoryViewModel.restoreWithToast`).
- Logic-only ids (UI is another lane's): STO-09 (SCR-50/51 sheets use `SceneActions.split` / `merge` previews), STO-14/15 (cast/world render `LensEdit`), STO-16 (shell hook; story only has the search command `렌즈를 이 씬으로 맞추기`).
- Conventions added: a restore is a pure `(doc, ctx) -> RestoreStep?` (doc, change, toast) run through `restoreWithToast`, so the toast carries `되돌리기`. The local RenumberSheet is gone; open `navigation.RenumberSheet` (SCRIPT registers it).
- domain:story additions (additive, tested in `AppearanceEditTest`): `scene/AppearanceEdit.addSilent/removeSilent`, `time/FlashbackLines.skipLine`, `TimeLabels.dayPart`, and the `직접` 회상 result line no longer names a kept event.
- Unfinished threads: STO-30 TravelBar card (board: `tint` card + `edge`, replaces `WhereTravelBlock` and the generic travel rows in `CheckRows`), STO-31…35 `BulkPlaceTime` (cells open `SceneWhereSheet`; `PickOnMap(sceneCount)` exists), new-scene sheet 때 editing via `WhenEdit` (still keep/clear only), grip drag (STO-08), `LibrarySearch`, STO-43 inner layouts, the two undrawn check buttons (`다른 블록에 잇기`, `화자 한꺼번에 바꾸기`), board-exact `+ 세라` inline silent members, Q20 flashback amount.

## batch 4 (finished, 2026-11-22, last batch of this lane)

Read docs/status/feature-story.md §Architecture first (new package `bulk`; `check` gained `TravelLook` / `TravelCard`; `search` gained `LibrarySearch*`; `common` gained `MiniMapModel` / `MiniMapView`).

- Built: STO-30 travel card and compact rows (점검 센터 and the `장소와 때` sheet), STO-31 … STO-35 장소 · 때 한꺼번에 (`BulkPlaceTime`: cover list, local `BulkPlaceSheet` / `BulkWhenSheet`, `제안 받기` page, inner table + `장소별 씬 수`), STO-43 selection in `TwoHome` / `TwoCheck`, `LibrarySearch` (LIB-07 request), 때 chips in the new-scene sheet. STO-29 (engine) and STO-42 (switches) were already in the domain modules.
- Conventions added: a screen-to-screen hand-over without a route argument goes through `deps.results` under a fixed key and is consumed once into `rememberSaveable` (`BulkModel.preselectKey`); schematic maps come from pure `MiniMaps` and draw with `MiniMapView`; row selection beside a detail pane is a pure `selectedKey(backStack, p)`.
- No domain module changed. `tools/gw :feature:story:compileDebugKotlin :feature:story:testDebugUnitTest` exits 0: 140 tests in 20 classes, all passing.
- Unfinished threads (all listed under Partial / Requests): grip drag (STO-08), measured time/code columns on 스토리 구조, the two undrawn check buttons, board-exact inline `+ 세라`, Q20 flashback amount, `고치기` pre-fill (needs `SceneWhereSheet` arguments), a `BulkPlaceTime` route argument for the preselection, base images under the mini maps, LIB's STO-42 rows, device checks of every screen.
