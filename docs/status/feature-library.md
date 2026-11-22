# feature:library

Batch 1 of 6 done (2026-11-22): LIB-01 02 03 09 10 11 12 22 23 24 25 26 27.

**Build and tests.** `tools/gw :feature:library:compileDebugKotlin :feature:library:testDebugUnitTest` exits 0: 37 tests, all passing (`LibraryCopyLintTest` 4, `ShelfStateTest` 9, `NewProjectTest` 13, `ProtectionTest` 11). No domain module was changed. Nothing was run on a device or emulator, and no APK was built.

## Architecture

Package root `io.github.lenticualrstone.civilight.feature.library` (`src/main/java/...`). Tests are in `src/test/java/...`, with fixtures in `LibFx`: the spec §4 sample projects made through the real `만들기` path, and the example document as an ordinary row.

**Entry and shared services**
- `LibraryEntries.kt`: `libraryEntries(deps)` registers this lane's routes and its shared sheet. Routes: `Library`, `NewProject(step)`, `SettingsRating`, `RatingDefaultsApply(rating)`, `ProjectSettings(p)`. Shared sheet: `RatingChangeSheet`. It creates one `LibraryServices` per activity, in memory only.
- `LibraryServices.kt` holds what every screen shares:
  - `deps`
  - `writer` (`ProjectWriter`)
  - `protection` (`ProtectionActions`)
  - `facts` (`ProtectionFacts`)
  - `draft` (`NewProjectDraftHolder`)
- **How to add a screen:** add an `entry<Route>` line, a `…Screen(route, services)` composable, a view model, and a pure `…State` builder with a JUnit test. View models are created with `entryViewModel { … }`. Project screens subclass `ProjectViewModel`, as `ProjectSettingsViewModel` does.

**`common/`**
- `LibraryDocs`: documents of all live projects, read with `LibraryStore.readDoc`. A document is read again only when its index `updatedAt` moves, and no store is opened.
- `ProjectWriter` / `SessionProjectWriter`: for writes to a project that is not the current screen's. It acquires the store, applies one labelled undo step, then releases.
- `rememberProjectDoc`: a sheet's live document.
- `ProtectionFacts`: currently `NoBackupYet`. Replace it when LIB-57 and LIB-58 land.
- `LibraryIcons`: pencil, shield, download.
- Helpers: `sheetBody`, `actionContext()`, `toAccess()` / `toSession()`.

**`protection/`**
- `ProtectionActions` over `ProtectionPorts` is **the one place** where a project's protections or rating change: hide, `숨김 풀기` in the open group, one switch, rating change, and `같이 바꾸기`.
- It applies the rule that turning hiding off needs a prompt, and that a cancelled prompt changes nothing.
- Becoming hidden drops 최근 연 파일.
- LIB-31 (⋯ `프로젝트 숨기기`, `숨김 풀기`) is already wired through it. A later batch adds only the remaining LIB-31 details.

**`shelf/` (서재, LIB-01 … LIB-03)**
- `LibraryScreen`, `LibraryViewModel`, `ProjectRow`, `ShelfSheets` (the ⋯ sheet `ShelfMenuSheet` and `RenameSheet`), `DeleteProjectDialog`.
- `ShelfState` is the pure builder of `ShelfUi`. It produces:
  - the rows and section label
  - the locked `LockRow` group, or the open `LibUnlocked` group with `숨김 풀기` and the header `다시 잠그기`
  - the `OnbEmpty` empty block with `예시 프로젝트 삭제됨` (LIB-70 and LIB-18 already draw from it)
  - the 라이프 레코더 row sub
- `ShelfUi.shows(key)` and `ShelfState.sheetsToClose` keep a hidden project's sheets and delete dialog from outliving the open group after a re-lock or a restart.
- The hidden-group unlock (LIB-32 `지문으로 보기`) and `다시 잠그기` (LIB-33) are wired to the shell's `UnlockController`.
- **To extend:**
  - LIB-04 card view: a `ShelfUi` field plus a composable switched by `ViewToggle`. `LibraryListing.cardSectionLabel` and `HiddenGroupRow.cardSub` exist in domain:library.
  - LIB-06: the shell's list-detail pairs already open `ProjectHome` beside the list through `openDetail`.

**`newproject/` (LIB-09 … LIB-12, and the LIB-14 frame)**
- `NewProjectForm` is the draft, as `@Serializable` data. `NewProjectRules` holds the pure step rules.
- `NewProjectDraftHolder` shares the draft across the four `NewProject(step)` entries. Each step entry saves a JSON snapshot in `rememberSaveable` and restores it once after process death.
- `NewProjectViewModel` supplies step 4's lists and runs `만들기`.
- `ProjectCreator` builds the project:
  1. `domain:library` `NewProjectFlow.create`
  2. `domain:cast` `FieldBundles` (the `다른 프로젝트에서` field set)
  3. `domain:recorder` `RecorderHooks.linkRecords` (links with origin `시작 재료`)
- `NewProjectScreen` draws the cover frame (4-bar progress, glass bottom bar). On inner screens it draws the `NewWide` frame: a 380 form column with `취소` beside the progress and a 300 `미리 보기` aside. The wide frame is LIB-14's layout, already built; LIB-14 needs only checking against the board.
- `NewProjectSteps` holds the step bodies and `BundlePickerSheet` the bundle picker.
- `startNewProject()` resets the draft and pushes step 1. It is used by `+` and the empty 서재.
- After `만들기` the stack is reset to `서재 › 프로젝트 홈`, plus `› 세계 달력` when `세계 달력 만들기` was picked. LIB-13 (`시작해 볼 것`) is this lane's, in a later batch. Notes for it:
  - It is drawn on the new project's 프로젝트 홈, which is the story lane's route (`ProjectHome`), so that screen needs a slot for the block.
  - The data comes from `domain:library` `StartBlock.of(doc, recorder, protections)`.
  - `ProjectCreator.create` currently keeps only `hidden` in `CreateResult`. Pass `CreatedProject.protections` on for the `숨김 · 끔` summary (R19-11).

**`rating/` (LIB-22 … LIB-24)**
- `RatingCells`: the 3-cell picker.
- `RatingPickSheet`: a feature-local picker over a project, used from 서재 ⋯ and 프로젝트 설정. A different choice opens the shared sheet.
- `RatingChangeContent`: the shared `바뀌는 것` sheet.
  - It is built once when it opens and closes exactly once (`RatingSheetModel.CloseOnce`).
  - It closes when its hidden project's group locks.
  - It stays open on a cancelled prompt.
- `RatingSettingsScreen` and `RatingSettingsState`: 설정 · 수위 등급.
- `RatingDefaultsApplyScreen`: the `같이 바꾸기` list.

**`projectsettings/` (LIB-25, the start of LIB-69)**
- `ProjectSettingsScreen`, `ProjectSettingsViewModel` (`ProjectViewModel`), `ProjectSettingsState`.
- **To extend for LIB-69:** add the sections `구조`, `인물` and `스크립트 추가 칸` as fields of `ProjectSettingsUi` and as composables in the screen's column.

**Copy:** `ShelfCopy`, `NewProjectCopy`, `RatingCopy`, `ProtectionCopy`, `ProjectSettingsCopy`. Most composed copy comes from domain:library: `RowMeta`, `ProjectMenus`, `ProjectDeleteDialog`, `RatingSheets`, `R18DefaultsBlock`, `HiddenGroupRow`, `EmptyLibrary`, `ProjectProtectionSettings`. `LibraryCopyLintTest` lints every constant of the five objects plus the formatted copy. A new copy object must be added to its `objects` list.

## Done

Pure logic, tested:
- **LIB-23:** `RatingSettingsState`, R19-10 (`이미 있는 R-18 프로젝트 2 · 같이 바꾸기`, existing projects untouched, changed values only).
- **LIB-24:** `ProtectionActions.changeRating`, R19-02, R19-05, R19-06, R19-09.
- **LIB-25:** `ProtectionActions.switch`, `ProjectSettingsState`, R19-14, R19-15.
- **New project:** P-01 … P-04, P-06, R19-11 … R19-13.
- **서재:** S-01, P-07, P-09 … P-11.

## Partial

Every UI feature in this batch is partial: none has been seen on a device.

- **LIB-01 서재.** Built:
  - header `Civilight` / `서재`, with `+`, or `다시 잠그기` while the group is open
  - `프로젝트 · N` and rows newest first: title, badge, meta, `쓰는 중`, ⋯
  - the locked group row with `지문으로 보기`, and the open group with `숨김 풀기`
  - `라이브러리 · 프로젝트 밖` › `라이프 레코더` (`기록 n · 정리 안 됨 u`)
  - command bar `모든 프로젝트에서 찾기` + mic
  - the `OnbEmpty` empty card

  Left: the `ViewToggle` (LIB-04, later batch).
- **LIB-02 ⋯ sheet.** Rows from `ProjectMenus`: `열기` · `이름 바꾸기` · `수위 등급` · `프로젝트 숨기기`/`숨김 풀기` · `백업 · 내보내기` · `지우기`. The example shows only `열기`.

  `백업 · 내보내기` opens the shared `ExportSheet`, which is still the placeholder sheet until LIB-41 (later batch).
- **LIB-03 delete confirm.** Built: counts, NFC name match, `휴지통으로` in `r18`. Confirming pops the project's screens, moves the project to the trash through `LibraryStore.moveToTrash` and removes its recents.
- **LIB-09 … LIB-12 new project.** Every field, radio card, picker, the R-18 block, the level editor, the preview tree, the record checklist, the bundle picker and the hide switch are built. Left:
  - `TODO_COPY` labels for the level editor buttons
  - a device check of the wide frame (LIB-14 board)
- **LIB-22.** Badges via `ThemeBridge.rating` on rows, sheets and settings. The PDF cover rating belongs to LIB-43.
- **LIB-23 설정 · 수위 등급 and the `같이 바꾸기` list.** Not yet reachable: `SettingsRating` is opened from 설정 (LIB-63, later batch).
- **LIB-24 `바뀌는 것` sheet.** The ◇ row `복구 암호 없음 · 정하기 ›` pushes `RecoveryPassword` (placeholder until LIB-58). The row `이전 백업 n · 암호 없음 · 지우기 ›` is never built until Drive backups exist (LIB-57, `NoBackupYet`).
- **LIB-25.** Section `등급 · 숨기기` of 프로젝트 설정: rating row → picker → sheet, and four switches with `<rating> 기본과 다름` in `acc`. The other LIB-69 sections are later batches.
- **LIB-26 feature side.** Every surface of this module that shows a project's content outside its own routes calls `ProtectWindow(WindowProtection.forProject(…))`:
  - open-group rows
  - the ⋯ sheet
  - the delete dialog
  - the rating sheet
  - hidden projects in the bundle picker and the `같이 바꾸기` list

  Project routes are protected by the shell. Left: 최근 연 파일 cards (LIB-05) and the export sheet (LIB-41), in later batches.
- **LIB-27.** The rule is `domain:library` `ProjectSurfaces.displayProjectName`, the one function for every surface outside a project.
  - In this module, hidden projects never appear by name while the group is locked. This covers rows, sheets restored after a restart, the bundle picker and the `같이 바꾸기` list.
  - R-18 rows show the real name with the red badge (S-01).
  - Left: widget, notification and recents surfaces (other lanes and later batches).
- **Hidden projects created or hidden in this session** (LIB-12, LIB-24, LIB-25, ⋯ `프로젝트 숨기기`): they join the hidden group but do **not** stay open until the next re-lock, because the shell has no kept-open set (see Requests).
  - Opening such a project, or staying on its 프로젝트 설정, shows the hidden-project lock and needs a prompt.
  - This module never calls `unlockHiddenGroup()` as a workaround, because that would open every other hidden project without a prompt.

**Hazards for later batches**
- `LibraryStore.moveToTrash` closes the project's store while `ProjectSessions` may still cache it for 15 s. Check this before building restore and reopen (LIB-38 trash, LIB-40 undo toast).
- The DS sheet keeps the last sheet composed while it animates out. If another lane reopens `RatingChangeSheet` with the same key during that exit animation, the old composition (with `CloseOnce.closed = true`) is reused, and its close buttons stop working. Paths through `RatingPickSheet` never do this.
- `RHRatingSheet` was compared with the board text: header, `R-15 → R-18 바뀌는 것`, small line, four rows, ◇ rows, note and buttons all match. `RHRatingDown` was not compared; it follows the spec.

## Defaults used

- **Step 3 `세계 달력 만들기`:** the calendar editor opens after `만들기` (`WorldCalendar` is a project route, so it needs the project). See Spec conflicts.
- **`열기 · 마지막 위치`:** opens 프로젝트 홈, then the script at `lastEditedBlockKey` when that block is live. A row tap opens 프로젝트 홈 through `openDetail`.
- **`이름 바꾸기` sheet:** title `이름 바꾸기` (the menu row), field `이름` (LIB-09), buttons `취소` · `바꾸기`. Disabled while the name is blank or unchanged.
- **`같이 바꾸기`:**
  - The settings keep only which groups changed, so "the changed value" is derived. It is every editable protection whose default differs from the factory default. A value turned off and on again is not a change.
  - The list (title `이미 있는 <rating> 프로젝트 <N>`, from the tag's own words; list copy is not in sources) shows each project with what would change, all checked.
  - Turning hiding off asks once for all; a cancelled prompt changes none.
  - Hidden projects are listed only while the group is open.
- **The tag** appears only when at least one project of that rating differs from the changed values.
- **Rating sheet after a re-lock:** a hidden project's sheet closes as soon as the group locks. A 서재 sheet or delete dialog of a project that is no longer a visible row closes too, from the top of the stack down. These sheets wait for the index to load first, so a sheet restored after a restart is not lost before its row exists.
- **New-project draft:** shared in memory across the four step entries. Each step entry saves a JSON snapshot for process death, restored once. `취소` on step 1 discards the draft with no confirm.
- **`쓰는 중` (§6 Q12):** shown when an episode's status is 쓰는 중.
- **Example project (LIB-17):** not a 서재 row (`LibraryListing`).
- **Fixtures:** the example has 11 live characters (fixture.md conflict 7), so its row reads `1장 7/10 · 인물 11 · 오늘 08:12`. P-09's `인물 12` is covered in domain:library with recipe 22.

## Spec conflicts

- **LIB-11 `세계 달력 만들기`** says it "opens the world-calendar editor of the Timeline area, then returns" during step 3. That editor is the project route `WorldCalendar(p)`, which needs an existing project, and nothing is created before `만들기` (§2.2). Chosen: open it right after `만들기`, on top of 프로젝트 홈.
- **Board `NewStart`** reads `라이프 레코더에서 가져오기 · 3개 중 2개 고름`. The spec (`연결` vocabulary, R23) reads `라이프 레코더 기록 연결 · <total>개 중 <k>개 고름`. The spec wording is used (domain:recorder `startMaterialHeader`).
- **DECISIONS §1** names `ProjectTrash` for whole-project trash. The real move is `core:store` `LibraryStore.moveToTrash` (the same row rule as `domain:trash` `ProjectTrash.delete`), which this module calls. `ProjectTrash` is pure state with no I/O.

## Requests

- **core:ui (UnlockController / LockRules): a per-project kept-open set** ("지금은 열린 채", R19 §2, LIB-12, LIB-24, LIB-33). A project that becomes hidden in this session should stay open until the next re-lock. `domain:library` `HiddenAccess.keptOpen` already models it. Needed:
  - `UnlockController.keepOpen(projectKey)`, cleared by every re-lock
  - `ProjectEntryFrame` treating a kept-open project as unlocked

  This module would call it from `ProtectionActions` when hiding goes off → on. Until then, see Partial.
- **core:designsystem: three DS-12 icons** that `LibMenu` draws: pencil (`이름 바꾸기`), shield (`수위 등급`), download tray (`백업 · 내보내기`). They are drawn locally in `common/LibraryIcons.kt` in the DS-12 style.
- **Copy needed (TODO_COPY):**
  - `TODO_COPY_LIB02_MORE_LABEL`: the accessible label of a row's ⋯ button (boards draw only the icon).
  - `TODO_COPY_LIB10_ADD_LEVEL`, `TODO_COPY_LIB10_REMOVE_LEVEL`: the `직접 정하기` editor buttons.
  - Surfaced from domain:library:
    - `TODO_COPY_LIB09_RECORDER_LINE` (R-18 block)
    - `TODO_COPY_LIB24_DEFAULT_ON`
    - `TODO_COPY_LIB32_RELOCK_SCREEN_OFF`, `TODO_COPY_LIB32_RELOCK_FIVE_MINUTES` (open-group note)
- **FEATURES.md:** LIB-01 02 03 09 10 11 12 22 23 24 25 26 27 → partial (see Partial for what is left).
