# feature:library

Batch 1 of 6 done (2026-11-22): LIB-01 02 03 09 10 11 12 22 23 24 25 26 27.
Batch 2 of 6 done (2026-11-22): LIB-29 31 32 33 34 37 38 39 40 41 45 47 49 (feature side; the shell built LIB-29 32 33 34 37 40).
Batch 3 of 6 done (2026-11-22): LIB-04 05 06 07 50 51 52 53 54 63 64 65 69.
Batch 4 of 6 done (2026-11-22): LIB-08 13 14 15 16 17 18 19 20 21 28 30 35 (feature side; the shell built LIB-08 splash and routing, the LIB-17 seed and the LIB-35 lock).
Batch 5 of 6 done (2026-11-22): LIB-36 42 43 44 46 48 72 55 56 57 58 59 60 (Drive itself is a labelled stub: nothing is uploaded or downloaded in this build).
Batch 6 of 6 done (2026-11-23): LIB-61 62 66 67 68 70 71, CNV-18 19 20, WLD-67, plus loose ends from Partial: the reverse `MUnlock` blur on re-lock, `MCarousel` parallax and tick, `preview.webp` in exports and backups, the eye button on export passwords, `때 표기 기본` applied to new projects, cast's `태그` row in 프로젝트 설정.

**Build and tests (batch 6).** `tools/gw :feature:library:compileDebugKotlin :feature:library:testDebugUnitTest` exits 0: 158 tests, all passing (batch 5's 145 plus `SettingsBatch6Test` 10, `CoverPreviewTest` 2, one `ShelfCardsTest` case for the parallax; `ProjectSettingsTest` now expects the `태그` row; `LibraryCopyLintTest` lints seventeen copy objects plus the storage line and the LIB-61 banner). `domain:library` was not changed in this batch. Nothing was run on a device or emulator, and no APK was built.

**Build and tests (batch 5).** `tools/gw :feature:library:compileDebugKotlin :feature:library:testDebugUnitTest` exits 0: 145 tests, all passing (batch 4's 114 plus `BackupTest` 13, `ExportRulesBatch5Test` 7, `PdfExportTest` 6, `MultiExportTest` 3, `RecorderFileTest` 2; `ExportTest` keeps 12 with the PDF case rewritten; `LibraryCopyLintTest` now lints fourteen copy objects). `domain:library` was changed additively (`export/PdfEncryption.kt`): `tools/gw :domain:library:test` exits 0, 192 tests (189 + `PdfEncryptionTest` 3). The encrypted PDF the domain test writes (`domain/library/build/pdf-encryption/sample.pdf`) was opened with macOS PDFKit (a Swift script): locked, wrong password refused, right password opens, text and info strings decrypt. Nothing was run on a device or emulator, and no APK was built.

**Build and tests (batch 4).** `tools/gw :feature:library:compileDebugKotlin :feature:library:testDebugUnitTest` exits 0: 114 tests, all passing (batch 3's 101 plus `OnboardingTest` 13; `LibraryCopyLintTest` now lints eleven copy objects and the LIB-19 tour copy). `domain:library` was changed additively (package `onboarding`, `StartBlock.ofProject`): `tools/gw :domain:library:test` exits 0, 189 tests (183 + `OnboardingGuideTest` 6). Nothing was run on a device or emulator, and no APK was built.

**Build and tests (batch 3).** `tools/gw :feature:library:compileDebugKotlin :feature:library:testDebugUnitTest` exits 0: 101 tests, all passing (`LibraryCopyLintTest` 4, `ShelfStateTest` 9, `ShelfCardsTest` 10, `NewProjectTest` 15, `ProtectionTest` 13, `TrashTest` 17, `ExportTest` 12, `SettingsTest` 5, `ProjectSettingsTest` 7, `OpenFileTest` 9). No domain module was changed. Nothing was run on a device or emulator, and no APK was built.

**Build and tests (batch 2, kept for the record).** `tools/gw :feature:library:compileDebugKotlin :feature:library:testDebugUnitTest` exited 0: 70 tests, all passing (`LibraryCopyLintTest` 4, `ShelfStateTest` 9, `NewProjectTest` 15, `ProtectionTest` 13, `TrashTest` 17, `ExportTest` 12). No domain module was changed. Nothing was run on a device or emulator, and no APK was built.

## Architecture

Package root `io.github.lenticualrstone.civilight.feature.library` (`src/main/java/...`). Tests are in `src/test/java/...`, with fixtures in `LibFx`: the spec §4 sample projects made through the real `만들기` path, and the example document as an ordinary row.

**Entry and shared services**
- `LibraryEntries.kt`: `libraryEntries(deps)` registers this lane's routes and its shared sheets. Routes: `Library`, `NewProject(step)`, `Settings`, `SettingsHiding`, `SettingsTextSize`, `SettingsRating`, `RatingDefaultsApply(rating)`, `ProjectSettings(p)`, `StructureLevels(p)`, `Trash(filter)`, `OpenFile(uri)`, and since batch 4 `Onboarding(page)`, `Walkthrough(page)`, `ExampleTour(p)`, `HelpReplay`. Since batch 5 also `SettingsBackup`, `RecoveryPassword`, `RestoreBackup`, `Restoring` and the start-up hook `library.backup`. Since batch 6 also `SettingsLayout`, `SettingsMotion`, `SettingsMore`. Still the shell placeholder: `Licenses` (no board or copy draws it). Shared sheets: `RatingChangeSheet`, `ExportSheet`. It creates one `LibraryServices` per activity, in memory only.
- `LibraryServices.kt` holds what every screen shares:
  - `deps`
  - `writer` (`ProjectWriter`)
  - `protection` (`ProtectionActions`)
  - `facts` (`ProtectionFacts`)
  - `draft` (`NewProjectDraftHolder`)
  - `exports` (`ExportDrafts`: the open 내보내기 sheets' drafts, passwords in memory only)
  - `onboarding` (`OnboardingActions`, batch 4: resume page, page navigation, walkthrough flag, `온보딩 끝내기`)
  - `backups` (`BackupServices`, batch 5: book, facts, Drive port, keyring, restore job) and `facts` = `backups.facts`
  - `attach(context)` / `writtenBy()` (batch 5): the 서재 hands over the application context so background writes can say `Civilight <version> (Android)`
  - `deviceSecure()`, `showNoDeviceLock()`, `report(outcome)`: the one place a protection outcome becomes visible (only `NO_DEVICE_LOCK` shows a toast; cancel and success stay silent, LIB-37).
- **How to add a screen:** add an `entry<Route>` line, a `…Screen(route, services)` composable, a view model, and a pure `…State` builder with a JUnit test. View models are created with `entryViewModel { … }`. Project screens subclass `ProjectViewModel`, as `ProjectSettingsViewModel` does.

**`common/`**
- `LibraryDocs`: documents of the selected index rows (live projects by default; the 휴지통 passes `{ it.trashed }`), read with `LibraryStore.readDoc`. A document is read again when its index `updatedAt` moves or its key re-enters the selection, and no store is opened.
- `ProjectWriter` / `SessionProjectWriter`: for writes to a project that is not the current screen's. It acquires the store, applies one labelled undo step, then releases. `purge(…)` is the permanent-delete form: one step, then `clearUndo()`, because the undo stack holds whole-document snapshots that still contain the purged rows.
- `rememberProjectDoc`: a sheet's live document.
- `ProtectionFacts`: since batch 5 `backup.BackupFacts` over the backup book (`NoBackupYet` stays only as a test double).
- Icons: the ⋯ sheet uses the DS `IconId.Pencil` / `Shield` / `Download` (core pass); the local `LibraryIcons` copy is gone.
- Helpers: `sheetBody`, `actionContext()`, `toAccess()` (now with `UnlockState.keptOpen`) / `toSession()`.

**`protection/`**
- `ProtectionActions` over `ProtectionPorts` is **the one place** where a project's protections or rating change: hide, `숨김 풀기` in the open group, one switch, rating change, and `같이 바꾸기`.
- It applies the rule that turning hiding off needs a prompt, and that a cancelled prompt changes nothing.
- Becoming hidden drops 최근 연 파일 and calls `ProtectionPorts.keepOpen` (`UnlockController.keepOpen`, "지금은 열린 채"), so the project stays open until the next re-lock.
- Turning hiding on needs a secure lock screen (`ProtectionPorts.deviceSecure`, §6 Q19 `ED`). Without one every path (⋯, switch, rating sheet, `같이 바꾸기`) returns `NO_DEVICE_LOCK` and changes nothing; `ProjectCreator` creates the project visible and reports `hidingRefused`.
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
- **Card view (LIB-04, batch 3):** `ShelfUi.showCards`, `cardSectionLabel`, `cards: List<ProjectCardUi>` (visible projects, then the open group's with `hiddenOpen = true`), `recents: List<RecentCardUi>`; the lock card is `ShelfUi.lockedGroup`. `ShelfCards` holds the pure parts: the view choice (`AppSettings.viewByScreen["library"]`, written with `SettingsStore.setViewMode`), board metrics, `isWide(widthDp)` (≥ 560), `pageCount`, `cover(doc)` (the `PROJECT_COVER` image use, 3:4 crop, preset, else the initial letter) and `chip` (`쓰는 중 · 홀로`). Composables: `CardShelf.kt` (`ProjectCardsBlock`: section + `PagerMarks`, snapping `LazyRow` of DS `ImageCard`s, `LockCard`, wide-only `NewProjectCard`; `RecentsBlock`), `CoverArt.kt` (`CoverArt`, `BlobImage`, `CoverBitmaps.decode`). `LibraryScreen` switches between the list and the card blocks; the header holds `ViewToggle`, the 설정 button and `+` / `다시 잠그기`.
- **최근 연 파일 (LIB-05, batch 3):** `RecentsModel` (`itemOf(route, doc, …)` = what an open records, `cards(…)` = `domain:library` `Recents.cards` plus time, image and window protection, `routeOf`). `RecentsRecorder` is run by `LibraryViewModel`: it watches the top of the back stack (`snapshotFlow`), waits up to 2 s for the opened project store, falls back to the 서재's stored copy, and calls `AppStateStore.recordRecent`. A card opens `ProjectHome` (`openDetail`), then the item's screen.
- **Motion (batch 6):** the header `다시 잠그기` tap arms the reverse `MUnlock` with the group as it was (`relocking` in `LibraryScreen`); once the 서재 shows the group locked, those rows blur and fade out for at most 320 ms in place of the lock row (whole group, inert, silent to accessibility, still under `ProtectWindow`). Every other re-lock (leaving the app, screen off, 5 minutes, unhide, trash) and reduced motion drop the rows at once. The card row (`ProjectCardsBlock`) ticks (`HapticToken.TICK`) when it settles on another card and slides each picture by `ShelfCards.parallaxShift` (8 % of the card width per stride) scaled by `parallaxScale` (1 at rest, so the author's crop shows whole; none with reduced motion).
- **LIB-06:** the shell draws `목록 + 상세` and `가운데 한 칸`; the 서재 marks the row whose project is in the right pane (`LocalPaneRole.LIST`, `selectedRowBackground`). `ProjectRow(selected = …)`.

**`newproject/` (LIB-09 … LIB-12, and the LIB-14 frame)**
- `NewProjectForm` is the draft, as `@Serializable` data. `NewProjectRules` holds the pure step rules.
- `NewProjectDraftHolder` shares the draft across the four `NewProject(step)` entries. Each step entry saves a JSON snapshot in `rememberSaveable` and restores it once after process death.
- `NewProjectViewModel` supplies step 4's lists and runs `만들기`.
- `ProjectCreator` builds the project:
  1. `domain:library` `NewProjectFlow.create`
  2. `domain:cast` `FieldBundles` (the `다른 프로젝트에서` field set)
  3. `domain:recorder` `RecorderHooks.linkRecords` (links with origin `시작 재료`)
- `NewProjectScreen` draws the cover frame (4-bar progress, glass bottom bar). On inner screens it draws the `NewWide` frame (LIB-14, checked against the board in batch 4): within an 800 frame, the progress row across both columns with `취소` at its end, the 380 form column at the start, the 300 `미리 보기` aside (padding 22) at the end stretching down to the buttons, `이전 · 다음` (380) at the bottom end. Step 2 shows the name read-only in a filled box and `나누는 방법`. The preview tree indents 18 per level with 4 dp line padding (boards `NewStructure`, `NewWide`).
- `NewProjectSteps` holds the step bodies and `BundlePickerSheet` the bundle picker.
- `startNewProject()` resets the draft and pushes step 1. It is used by `+` and the empty 서재.
- After `만들기` the stack is reset to `ProjectCreator.stackAfter(result)`: `서재 › 프로젝트 홈`, plus `› 세계 달력` when `세계 달력 만들기` was picked. No `NewProject` step stays below (test `after 만들기 back … goes to 서재`). A project created hidden is kept open (`CreatorPorts.keepOpen`).
- The `NewWide` frame pads its box with `navigationBars ∪ ime`, so `이전 · 다음` ride on the keyboard on inner screens; the cover frame gets the same from the DS `BottomGlassBar` (core pass). LIB-13 (`시작해 볼 것`): batch 4 settled it (see Partial, LIB-13). The original notes:
  - It is drawn on the new project's 프로젝트 홈, which is the story lane's route (`ProjectHome`), so that screen needs a slot for the block.
  - The data comes from `domain:library` `StartBlock.of(doc, recorder, protections)`.
  - `숨김 · 끔` (R19-11) is derived from the document by `domain:library` `StartBlock.ofProject` (batch 4), because the home has only the document; `CreateResult` is unchanged.

**`rating/` (LIB-22 … LIB-24)**
- `RatingCells`: the 3-cell picker.
- `RatingPickSheet`: a feature-local picker over a project, used from 서재 ⋯ and 프로젝트 설정. A different choice opens the shared sheet.
- `RatingChangeContent`: the shared `바뀌는 것` sheet.
  - It is built once when it opens and closes exactly once (`RatingSheetModel.CloseOnce`).
  - It closes when its hidden project's group locks.
  - It stays open on a cancelled prompt.
- `RatingSettingsScreen` and `RatingSettingsState`: 설정 · 수위 등급.
- `RatingDefaultsApplyScreen`: the `같이 바꾸기` list.

**`projectsettings/` (LIB-25, LIB-69)**
- `ProjectSettingsScreen`, `ProjectSettingsViewModel` (`ProjectViewModel`), `ProjectSettingsState`, `ProjectSettingsRows` (the rows other areas specify, rebuilt from their domain modules: `FieldSettings.entryRow`, `RelationKinds.summary`, `Travel.modes`, `BandTable.hoursLabel`, `SceneTimes`/`TimeLabels`, `VoiceIds.currentId`), `ScriptSettingsEdit` (the two script switches as one undo step each), `StructureLevelsScreen` + `StructureLevelsForm` (`단계 이름`, `StructureEdit.renameLevel`; the level count is never changed here).
- Each `… ›` row maps to its owner's route in `routeOf(target, p)`; an owner that has not built its screen shows the shell placeholder.
- **CNV-20 (batch 6):** the rows match `50-canvas-pages.md`'s row map, and every owner has registered its screen. One row beyond the board: `태그` (`ProjectSettingsTarget.TAGS` → cast's `TagSettings(p)`, sub = the project's tag names, no value word; cast's CST-12 request, Spec conflicts). `ProjectLinkRow.sub` / `value` are nullable (null = not drawn).
- **To extend:** a new row is a `ProjectSettingsTarget` + a builder in `ProjectSettingsRows` + a `routeOf` branch.

**`settings/` (설정, LIB-63 … LIB-68)**
- `SettingsScreen` (route `Settings`), `HidingSettingsScreen` (`SettingsHiding`), `TextSizeScreen` (`SettingsTextSize`), and since batch 6 `LayoutSettingsScreen`, `MotionSettingsScreen`, `MoreSettingsScreen`; one `SettingsViewModel` class serves them all (one instance per entry) (reads `deps.settings`, writes `SettingsStore`'s typed setters; values apply live through the shell's `ThemeBridge`).
- `SettingsState` is the pure builder (`root`, `hiding`, `textSize`); `AppLockRules` the 앱 잠금 switch rule (on needs a device lock; off asks once). `SettingsParts`: `SettingsFrame` (glass head + 560 column), `SettingsSection`, `LinkRow`, `SwitchSettingRow`, `SwitchLinkRow`, `ThemeSegmented`; `TextSizeControl` is reusable by onboarding `OnbLook` (LIB-15).
- **Reach:** the 서재 header's 설정 button (`ED`, no board draws it) → 설정 → `숨기기` → `수위 등급별 동작` (LIB-23), `휴지통` (LIB-38), `라이프 레코더` (recorder's `RecorderSettings`), `도움말 · 안내 다시 보기` (`HelpReplay`), `펼친 화면 배치` → `SettingsLayout`, `움직임 · 투명도` → `SettingsMotion`, `지도 · 이미지 · 연표` → `SettingsMore` (LIB-66 … 68, batch 6), `Google Drive 자동 백업` → `SettingsBackup` and `복구 암호` → `RecoveryPassword` (LIB-57, 58, batch 5; LIB-61 / 62 in batch 6). Every row's route is `SettingsRoutes.routeOf(SettingsRootRow)` (CNV-18 row map, tested).
- **Batch 6 screens (all on `SettingsFrame`, one shared `SettingsViewModel`; pure builders in `DisplaySettingsState`, copy `LayoutSettingsCopy`, `MotionSettingsCopy`, `MoreSettingsCopy`):**
  - `LayoutSettingsScreen` (LIB-66, board `LayoutSettings`): two radio cards with drawn 72 dp thumbnails (`LayoutCard`, local, since DS `ChoiceCard` has no leading slot) and the note; writes `AppSettings.unfoldedLayout` (the shell's `rememberWindowLayout` applies it, CNV-33).
  - `MotionSettingsScreen` (LIB-67, board `ReduceSettings`): `애니메이션` radios (`AnimationPref`), `투명도 줄이기` switch, the `유리` / `불투명` previews (each always draws its own look; the line is `MotionPreviewSample`, story content, unlinted), `라이프 레코더 빛` = `AppSettings.recorderGlow`, the same value the recorder's `녹음 중 빛 띠` writes (Q-ST-04).
  - `MoreSettingsScreen` (LIB-68 container for CNV-19, WLD-67, STO-42; board `SettingsMore`): switches `홀로그램 효과 기본`, `이동 점검`, `장소 없는 씬 알림`; `기본 이미지 · 보기 ›` → canvas `PresetImages`; value rows `새 지도 그림`, `지형 만들기 · 글로 설명`, `큰 지도 내보내기`, `때 표기 기본` open the local `MoreValueSheet(value)` (radio list; `DisplaySettingsState.options` / `choose`, one reducer for the four).
  - `때 표기 기본` is applied by `ProjectCreator.build(…, notation)` (`NewProjectViewModel` passes `defaultSceneNotation`).
- **To extend:** a new settings screen = a route entry, a `…Screen` using `SettingsFrame`, a builder in `SettingsState` (or its own `…State`) and a setter in `SettingsViewModel`.

**`openfile/` (파일에서 열기, LIB-50 … LIB-55)**
- `OpenFileScreen` (route `OpenFile(uri)`): an empty frame with one local sheet (`OpenFileSheetKey`) that shows the stage; closing the sheet pops the route.
- `OpenFileFlow` (pure): bytes → `OpenStage` (`Reading`, `Failed`, `Password` = `RHEncryptedFile`, `Newer` = `RHNewerFile`, `Ask` = `RHOpenFile` with the same-project choice). Reading, gates, upgrades, decryption, the newer check and sheets are all `domain:library` (`ProjectPackageReader`, `EncryptedFiles`, `OpenDecisions`, `OpenFileSheets`).
- `OpenFileViewModel` reads the `content://` uri and decrypts off the main thread; a hidden same-key project asks for the hidden-project prompt before the sheet shows it. `ProjectImporter` over `ImportPorts` (`StoreImportPorts` in the app) runs `열기`: the decision again on the current 서재, blobs, the old copy released (screens off the stack, cached store closed) and moved aside for `덮어쓰기`, the project added, recorder records merged by key. Then the stack is `서재` (+ `프로젝트 홈` for a visible project).

**`trash/` (휴지통, LIB-38, LIB-39)**
- `TrashScreen` (route `Trash(filter)`), `TrashViewModel`, `TrashSheets` (`SceneChoiceSheet`, `ScenePickSheet`), `TrashScreenCopy`.
- `TrashState.build(inputs, unlock, ctx)` is the pure builder of `TrashUi`. Every row comes from `domain:trash` `TrashListing` (`projectRows`, `projectItemRows`, `recorderRows`, `list`, `chips`): one row builder (DECISIONS §1). Each `TrashRowUi` says its `source` (`PROJECT` · `ITEM` · `RECORDER`), its project and the protections for `ProtectWindow` while it shows content. Masking comes from the shell's `UnlockState`: `hiddenGroupOpen` for project rows, `itemsOpen(p)` + `projectOpen(p)` (group or kept open) for item rows, `recordsOpen` for records. The read-only example is not listed.
- `TrashDecisions` is the one dispatch to the owners' write paths: scene and block group → `SceneActions.restoreEntry` / `restoreBlocks`, character → `CharacterActions.restore` (CST-43 toast), anything else → `RestoreOps.restore`; purges → `PurgeOps.purgeEntries` + `PurgeActions.withPlainUnitLabels` / `applyProjectPurge` (recorder links keep their label).
- `TrashActions` over `TrashPorts` runs them: one prompt before restoring a row with `restoreNeedsAuth` and before purging a masked row (silent cancel), `NeedsSceneChoice` for T-14, `비우기` = every unmasked row of every filter, `purgeDue` (T-08). Whole projects go to `LibraryStore.restore` / `StoreContainer.purgeProject` after `ProjectSessions.closeIdle()` (the 15 s cache hazard). Records go through `RecorderActions` on the recorder store. A prompt's authorization lives for one flow only: it ends on a write, on dismissing the scene choice, and on any change of the unlock state (`TrashViewModel` watches it).
- Opening the 휴지통 runs `StoreContainer.runMaintenance()` and the 30-day purge of every live project's own entries and of the records.
- **Reach:** 설정 › `휴지통` (batch 3, §6 Q27 `ED`); other lanes push `Trash(filter)`. A new trash kind needs nothing here unless its restore has an owner action in `domain:actions`; add that branch to `TrashDecisions.restoreItem`.

**`export/` (내보내기, LIB-41, LIB-45, LIB-47, LIB-49)**
- `ExportSheetContent` is the shared `ExportSheet` (registered here, opened by any lane over a project or from the 서재 ⋯). `ExportForm` is the draft; `ExportFormRules` the pure reducers and `canCreate`; `ExportRanges` the `범위` choices; `ExportRunner` builds the bytes; `SaveDialog` is the system save dialog; `ExportCopy`.
- Rows, defaults, the primary label and warnings come from `domain:library` (`ExportDefaults.initial`, `ExportSheets.build`, `ExportGate`). The draft lives in `LibraryServices.exports` (memory, per project), cleared on `닫기` or after a saved file; its passwords are blanked whenever the sheet leaves composition.
- Create: `ExportGate` (one prompt for `넣기` of hidden scenes) → CSV / engine JSON through `domain:actions` `ExportActions.run`, `.civilight` through `ProjectFileExport.export` with a `BlobReader` over `stores.blobs` (off the main thread; PBKDF2) → the sheet closes → `SaveDialog.open` (registered on the activity's result registry without a lifecycle, so the answer arrives even when a re-lock disposed the sheet) → the bytes are written → for CSV/JSON `ExportRunner.fixVoiceIds` is applied to the store's current document (one step `내보내기`). A cancelled dialog fixes nothing.
- `대본 PDF` (LIB-43) is written since batch 5 (`export/pdf/`, see below); `ExportFormRules.available` is true for every format.
- The sheet closes itself for the example, a trashed project, or a hidden project whose group is locked (`UnlockState.projectOpen`), and protects the window while open (LIB-26, LIB-29).

**`onboarding/` (첫 실행, 워크스루, 예시 둘러보기, LIB-08 feature side, LIB-15 … LIB-18; batch 4)**
- `OnboardingFlow` (pure): page order 환영 → 화면 1/4 → 백업과 잠금 2/4 → 라이프 레코더 3/4 → 워크스루 1 … 3 → 예시 둘러보기 4/4; `next`, `skip` (`건너뛰기` → `Walkthrough(1)`), `walkNext` / `walkSkip` → `OnbTarget` (`Go(route)`, `EndReplay`, `Finish`), labels, banner rule, and `resumeId(route)`: exactly the strings the shell's `Startup.startRoutes` reads (`WELCOME` … `VOICE`, `WALKTHROUGH_<n>`, `EXAMPLE`, `EXAMPLE_SCRIPT`).
- `OnboardingActions` (in `LibraryServices`): `recordResume(route)` / `recordResumeId(id)` (first run only), `go`, `back` (`이전`: return when on the stack, else replace), `follow(target)`, `markWalkthroughSeen`, `finish()`, `cleanUpLeftover()` and `useUpNoticeIfDue(library)` (the 서재 view model calls the last two). Writes run in `deps.appScope`.
- `OnboardingFinisher` over `FinishPorts` (one per activity; release + delete run under one `Mutex` and re-read the example, so the 서재's clean-up and a double tap never delete twice): `온보딩 끝내기` = state finished (`Onboarding.finish` state; `NOT_INSTALLED` kept when there never was an example) → `resetTo(서재)` → `SessionRelease.await` (closeIdle until the example's store is gone) → `StoreContainer.deleteExampleProject` (no trash, recorder links and recents dropped). `cleanUpLeftover` finishes a delete cut off by process death. `noticeUsedUp`: `예시 프로젝트 삭제됨` ends with the first live project (`emptyLibraryNoticeShown`).
- Screens: `OnboardingScreen` (route `Onboarding(page)`: `WelcomePage`, `LookPage` (three DS `ThemeSwatch`es + `TextSizeControl` without its size line), `SafePage` (Drive `연결` through `LibraryServices.backups.connect()` (batch 5; the Drive port is the `UnavailableDrive` stub), `앱 잠금` through `SettingsViewModel.setAppLock` = the 설정 rule), `VoicePage` (mic permission via `RequestPermission`, widget pin via `DeviceChoices.requestWidget`, which finds the app's own widget provider)); `WalkthroughScreen` + `WalkDemos` (looping demos; reduced motion = still end state); `ExampleTourScreen` + `ExampleTourViewModel` (`ProjectViewModel`) + `ExampleTourState` (pure rows from the document). `OnboardingParts`: `OnbFrame` (top row, scrolling 560 column, glass bottom bar, `ToastOverlay` above it; 예시 둘러보기 has the same overlay), `SetupTopRow`, `SetupHeading`, `LaterNextButtons`, `WalkDots`.
- `ExampleTourContract.FINISH_REQUEST` (`"library.onboarding.finish"`): another area ends onboarding by `deps.results.send(FINISH_REQUEST, true)` + `popToType<ExampleTour>()`; 예시 둘러보기 consumes it and runs LIB-18 (the script's `OnbExampleScript`, Request).
- Copy: `OnboardingCopy` (linted) and `OnboardingSample` (story content quoted from the boards: the manuscript lines, demo values; not linted, they are content).
- **To extend:** LIB-59 `백업에서 되살리기` from 환영 ends onboarding on success (batch 5: `RestoreJob` calls `OnboardingActions.finish()` during the first run). Drive `연결` becomes real by replacing `UnavailableDrive` in `BackupServices` (LIB-57; `DriveSignInStub` was removed in batch 5).

**`help/` (도움말 · 안내 다시 보기, LIB-21; batch 4)**
- `HelpReplayScreen` (route `HelpReplay`, `SettingsFrame`), `HelpReplayViewModel`, `HelpReplayState` + `HelpCopy`. Walkthrough `다시 보기` pushes `Walkthrough(1)` as a replay (onboarding finished: no resume writes, no example, `마치기` pops back); a module's `다시 보기` resets its flag (`ModuleTours.reset`, the tour runs on the next entry); `모든 안내 처음부터` = `FirstUseTips.resetAll`; the tips switch writes `AppSettings.firstUseTips`.

**`tour/` (LIB-19 framework, LIB-20 presenter; batch 4)**
- Rules live in `domain:library` `onboarding` so every lane can call them: `TourModule` (ids = `moduleTourSeen` keys, row titles, card labels), `ModuleTours` (`steps`, drawn copy, undrawn steps as `TODO_COPY_LIB19_*`, `copyComplete`, `shouldStart` (not seen and copy complete), `markSeen`, `reset`, card words), `TourRun` (pure step reducer), `FirstUseTips` (`shouldShow`, `markShown`, `resetAll`).
- `TourHost` (data-only API, ready to move to core:ui): `TourDim` hole around the step's `Modifier.tourTarget(step)`, `TourStepCard` (DS-49 card plus the `이전` button the boards draw), placed by the pure `TourPlacement` (below, above, else middle). The shade eats every touch; `건너뛰기`, `마치기` and back end the tour. `ModuleTourHost(module, deps)` binds it to `OnboardingState`.
- The 서재 is wrapped in `ModuleTourHost(LIBRARY_HIDING)`; it stays dormant until the tour's copy and anchors exist (§6 Q15).
- `FeatureDeps.showFirstUseTip(tipId, text)`: the LIB-20 toast presenter (once per id, switch respected).

**`notices/` (LIB-30; batch 4)**
- `LibraryNotices`: backup, restore and export done texts; hidden projects are counted (`숨긴 프로젝트 N`), name-masked ones read `R-18 프로젝트`. Posted by LIB-57 / LIB-60 work later (Request: `POST_NOTIFICATIONS`).

**`common/SessionRelease` (batch 4):** waits (≤ 2 s) until a project's cached store is closed; use it before deleting or re-keying a project that screens may still hold.

**`backup/` (보관, 복구 암호, 되살리기; LIB-57 … LIB-60, batch 5)**
- `BackupBook` (`@Serializable`: runs of the last 30 days, `BackupState` failure streak, `RecoveryKeyMaterial`, content time of each project's last uploaded version) in `files/app/backup.json` through core:store's own `JsonFile` (`BackupBookStore`, Request: move into `AppStateStore`). `BackupBooks` is the port (tests: in memory). `BackupFacts` is the real `ProtectionFacts` (replaces `NoBackupYet`: `recoverySet`, `unencryptedBackups`).
- `DriveGateway` is the only network port (account, connect, upload a version, download, delete, run index). **`UnavailableDrive` is the labelled needsExternal stub used in the app**: no account, `연결` → `DriveConnect.Unavailable`, transfers fail. `DriveRunIndex` (runs + names of non-hidden projects only) is what a new device lists.
- `BackupRunner` over `BackupSources` (`StoreBackupSources` = the stores; `projects()` gives `BackupProject(doc, updatedAt)` with the 서재 index time, which every content edit moves): `domain:library` `BackupPlanner` decides; `ProjectFileExport.backup` per upload (recovery lock when the plan says so); one `.civilight-recorder` (`RecorderPackage`, hidden records only when locked with the 복구 암호 and `숨긴 것도 백업` is on, audio per `원본 음성 보관`); one settings JSON (no account id); prune (`BackupPlanner.prune`); book + Drive index. A `DriveException` records a failed run and starts the streak (LIB-61 banner data).
- `BackupSchedule.due` (`ED` until WorkManager: a day after the last success, an hour after a failed try) runs from the start-up hook `library.backup` (`LibraryEntries`), launched in `appScope`.
- `RecoveryKeyring` over `DeviceKeyWrap` (`AndroidKeystoreWrap`: AES-256-GCM key `civilight.recovery` in the Android Keystore): `set(password)` = `RecoveryKeys.create` off the main thread, Keystore wrap, material stored and flushed, password zeroed; `current()` = the key for today's backup without typing.
- `RestoreModel` (pure: runs from book + Drive index, newest first, the chosen run, the `§6 Q25` checklist with one `숨긴 프로젝트 <h>` row, the request), `RestoreRunner` (download → open (`EncryptedFiles` with the 복구 암호) → `ProjectImporter` (same import as 파일에서 열기, `새 프로젝트로` / `덮어쓰기`) → recorder library merged by key; `checkPassword` tries the first locked file before anything is imported), `RestoreJob` (process-wide status, runs in `appScope`, first run → `OnboardingActions.finish()`, else 서재; restore-done toast from `LibraryNotices`).
- Screens: `BackupSettingsScreen` (`SettingsBackup`: Drive switch, `백업 기록 · 30일 보관 ›`, `복구 암호` row, the next run's warnings), `RecoveryPasswordScreen` (`RecoveryPassword`, board `RHRecoveryPw`), `RestoreBackupScreen` (`RestoreBackup`, board `Restore`, local `RestorePasswordSheet`), `RestoringScreen` (`Restoring`, board `Restoring`, `RestoringModel`). `PasswordInput` (field + eye button) is shared with the multi-export sheet. Pure builders: `BackupSettingsState`, `RecoveryPasswordState`, `RestoreModel`, `RestoringModel`. Copy: `BackupCopy`, `RecoveryCopy`, `RestoreCopy`.
- `SettingsFrame` gained `bottomButton` (the boards' full-width 52 dp primary) and `sheets` / `localSheets`.
- **LIB-61, LIB-62 (batch 6):** `BackupSettingsUi.banner` = `domain:library` `BackupFailureBanner.of(book.state, now, zone)`, drawn with the DS `ErrorBanner` and the two buttons under it (board `ErrBackup`); while it shows, the Drive row reads `마지막 성공 <M월 D일 HH:mm>`. `다시 로그인` = `backups.connect()` then a run; `지금 다시 시도` = `BackupServices.runNow` (ignores the schedule). Runs share one `Mutex` with the start-up run, run in `appScope`, and `BackupServices.running` disables both buttons meanwhile; `NotRunning` (always, with the stub) shows `TODO_COPY_LIB57_DRIVE_NOT_READY`. `기기 저장 공간` = `StorageFacts` from `DeviceStorage` (`StoreDeviceStorage`: usable bytes of `files/`, sizes of the projects directory and the blob store, read off the main thread), text `남은 공간 1.2 GB · 프로젝트 340 MB`; the row has no destination (no detail screen is drawn), so it shows no `›`.
- The real Drive client replaces `UnavailableDrive` in `BackupServices.drive`; nothing else changes.

**`export/pdf/` (대본 PDF, LIB-43, batch 5)**
- `ScriptPdf.build(doc, plan, rangeKeys, coverRating)` → `PdfBlock`s from the same `ExportPlan` (cover with name, rating when `내보내기 표지에 등급 표시`, range; scene heads `SceneText.headText`; dialogue with the speaker name above; narration, system, choices; 연출 / 작가 메모 / 괄호 지문 by layer).
- `PdfLayout` (pure, over a `TextMeasure`): A4, margins 64 / 72, body 11 pt × 1.7; the cover alone; scene heads and speaker names kept with what follows; page numbers. `AndroidPdfWriter` draws the pages with `PdfDocument` (body system serif, labels Pretendard).
- `ExportRunner.build(…, pdf, coverRating, authenticate)`: the gate (`넣기` asks once), the plan, the writer, then `domain:library` `PdfEncryption` when `PDF 암호` is on. No plan rides with the file, so nothing is fixed (LIB-46). A PDF that cannot be locked is never written plain (`Unavailable` + toast).

**`export/MultiExport` (LIB-72, batch 5)**
- `MultiExport` (pure: rows, toggle, unique file names, per-file `암호` default, `build` = `ProjectFileExport.export`) and `MultiExportSheet` (local to the 서재: checklist → one `암호` step per locked file → `FolderDialog` (`ACTION_OPEN_DOCUMENT_TREE`) → one `.civilight` per project through `DocumentFile`). Entry: the 서재 header's `IconId.Download` button (`내보내기`), listing `ShelfUi.exportable` (visible rows, then the open group's).

**`export/CoverPreview` (`preview.webp`, batch 6):** `CoverPreview.source(doc)` (pure: the project's picked `PROJECT_COVER` blob and its 3:4 crop; none for a hidden project, a preset or no cover), `PreviewRenderer` (port), `AndroidCoverPreview` (decode with `CoverBitmaps`, 360 px wide, WebP). Passed to `ExportRunner.build(…, preview)`, `MultiExport.build(…, preview)` and `BackupRunner(…, preview)`; the domain writer drops it again for hidden projects.

**`openfile/RecorderFiles` (LIB-48, batch 5):** `OpenFileFlow.open` recognises a `.civilight-recorder` by its mimetype and returns `OpenStage.Recorder`; a recovery-locked one goes through the same password stage (`RecorderFiles.unlock`); `열기` merges by key (`RecorderMerge.merge`) and stores the audio, then opens the 기록함.

**Copy:** `ShelfCopy`, `NewProjectCopy`, `RatingCopy`, `ProtectionCopy`, `ProjectSettingsCopy`, `TrashScreenCopy`, `ExportCopy`, `SettingsCopy`, `OpenFileCopy`, `OnboardingCopy`, `HelpCopy` (batch 4), `BackupCopy`, `RecoveryCopy`, `RestoreCopy` (batch 5), `LayoutSettingsCopy`, `MotionSettingsCopy`, `MoreSettingsCopy` (batch 6) (the sheets' own words come from domain:library `OpenFileSheet`, `NewerFileSheet`, `EncryptedFileSheet`). Most composed copy comes from domain:library: `RowMeta`, `ProjectMenus`, `ProjectDeleteDialog`, `RatingSheets`, `R18DefaultsBlock`, `HiddenGroupRow`, `EmptyLibrary`, `ProjectProtectionSettings`. `LibraryCopyLintTest` lints every constant of the seventeen objects plus the formatted copy (storage line, LIB-61 banner) and the LIB-19 tour copy drawn on the boards. A new copy object must be added to its `objects` list.

## Done

Pure logic, tested:
- **LIB-23:** `RatingSettingsState`, R19-10 (`이미 있는 R-18 프로젝트 2 · 같이 바꾸기`, existing projects untouched, changed values only).
- **LIB-24:** `ProtectionActions.changeRating`, R19-02, R19-05, R19-06, R19-09.
- **LIB-25:** `ProtectionActions.switch`, `ProjectSettingsState`, R19-14, R19-15.
- **New project:** P-01 … P-04, P-06, R19-11 … R19-13; the stack after `만들기` (`ProjectCreator.stackAfter`).
- **서재:** S-01, P-07, P-09 … P-11.
- **LIB-31 rules:** "지금은 열린 채" on every path that hides (`keepOpen`), and the §6 Q19 refusal without a device lock (ProtectionTest, NewProjectTest).
- **LIB-38 / LIB-39 rules:** `TrashState`, `TrashDecisions`, `TrashActions`: T-01, T-05 … T-10, T-12, T-14, S-08, the LIB-37 cancel, `비우기` leaving masked rows, one-block restore (TrashTest).
- **LIB-41 / LIB-45 / LIB-47 / LIB-49 rules:** `ExportFormRules`, `ExportRunner`, `ExportRanges`: E-01 … E-04, E-08 … E-11, LIB-46 fixation only after the save, the `.civilight` package (`mimetype` first, stored) and its encrypted container (ExportTest).
- **LIB-50 / LIB-51 (feature side, batch 3):** a `.civilight` written by this module's 내보내기 (plain and with `암호`) reads back through `OpenFileFlow`; a file needing an unknown required feature is refused (F-04, F-06). The version and crypto rules themselves are `domain:library`'s (its own tests). `OpenFileTest`.
- **LIB-52 … LIB-54 rules (batch 3):** `OpenFileFlow`, `OpenStage.Ask.choose`, `ProjectImporter`: F-09 (no same project), F-10 (`새 프로젝트로` default, `‘<name> (2)’로 추가`, new key), F-12 (`덮어쓰기` order: release, move aside, recents, add), the example never overwritten, the decision re-taken at `열기`, F-13 … F-15 (password sheet labels, wrong password keeps the sheet, a password-locked file lands hidden), not-a-package, recorder records merged without duplicates (`OpenFileTest`).
- **LIB-63 … LIB-65 rules (batch 3):** `SettingsState` (the `Settings` and `HideSettings` board rows, text steps 16 … 22), `AppLockRules` (`SettingsTest`).
- **LIB-69 rules (batch 3):** `ProjectSettingsRows` on the example against board `ProjSettings`, `routeOf`, `ScriptSettingsEdit`, `StructureLevelsForm` (`ProjectSettingsTest`).
- **LIB-04 / LIB-05 / LIB-07 rules (batch 3):** card row and lock card (never a hidden project's card while locked), open-group cards, `쓰는 중 · 홀로`, cover choice, view choice per screen; recents S-02 (hidden project, hidden or trashed item left out, locked or open), S-12 (masked `R-18 프로젝트` card, no title, no protection), what an open records, the recorder's wait; cross-search scope S-03, S-09 and the `검색 · 명령 결과` switch (`ShelfCardsTest`).
- **Requests of other lanes done (batch 3):** feature:cast (`인물 항목` → `FieldSettings(p)` with `FieldSettings.entryRow(doc)` and `고치기 ›`), feature:story (`때 표기` → `SceneNotationSettings(p)`, `하루 · 시간대` → `DayBandSettings(p)` with `BandTable.hoursLabel`), feature:world (`세계 시간` → `WorldCalendar(p)`), all in 프로젝트 설정.

- **Batch 4, pure logic tested (`OnboardingTest`, domain `OnboardingGuideTest`):**
  - LIB-08 feature side: resume ids equal the shell's `startRoutes` strings; never written once onboarding is finished.
  - LIB-15 / LIB-16: page order, `나중에` = next page, `건너뛰기` = walkthrough; walkthrough targets for first run, replay and a missing example; labels and banner.
  - LIB-17: 예시 둘러보기 rows from the example document (`스크립트 · 1-7 씬 3`, `인물 · 리엔`, `세계 · 오리지늄 등불`, `Ⅰ 1장 · 등불지기`, `1-6 잿더미 시장 완료` · `1-7 잿빛 새벽의 등불 씬 4개` · `1-8 — 미정`) and their routes.
  - LIB-18 (P-07): finish order (state, 서재, release, delete), no trash, no delete without an example, the leftover clean-up, exactly one delete when the new 서재's clean-up and a second tap race the finish (one lock, the example re-read inside it), the notice used up by the first project.
  - LIB-19: step counts and drawn copy, seen / reset, dormant while copy is missing, the card reducer, card placement.
  - LIB-20: once per id, never with the switch off. LIB-21: the `HelpReplay` rows, `모든 안내 처음부터`.
  - LIB-28: `recorderLinkLabel` and the recorder's `LinkViews.ratingBadge` agree for every rating; the 프로젝트 설정 switch is the flag the recorder reads.
  - LIB-30: backup, restore and export texts never name a hidden project.
  - LIB-13 / R19-11: `StartBlock.ofProject` adds `숨김 · 끔` when the rating hides by default and the project is visible.
- **Requests of other lanes (batch 4):** the open one naming this lane is feature:script's LIB-69 `스크립트 추가 칸` request; batch 3 already covers it (the two switches inline, `ID 형식 ›` → `ScriptFieldSettings(p)`). No other request names feature:library.

- **Batch 5, pure logic tested:**
  - LIB-42: `빼기` leaves the hidden scene's lines out of the 대사 표 and they are not `버림`; hidden recorder records stay out of a `.civilight` unless it is locked because the project is hidden (`숨긴 기록 1 제외`); hidden speakers by name only (`숨긴 인물 1 · 리엔`, `화자 이름만 들어감`) (`ExportRulesBatch5Test`, `MultiExportTest`).
  - LIB-44: the CSV bytes from the real path: BOM, CRLF, `화자,대사,보이스 ID,블록 ID,상태`, `새` on a first export, RFC 4180 quoting (`ExportRulesBatch5Test`).
  - LIB-46: CSV / JSON fix only after the save (batch 2 test); PDF and `.civilight` carry no plan, so nothing is fixed (`ExportRulesBatch5Test`, `PdfExportTest`).
  - LIB-55: a damaged episode file opens with `열지 못한 부분 · 1-7` shown on `RHOpenFile` before `열기` and in the toast after; damaged `project.json` or `story/index.json` imports nothing (`ExportRulesBatch5Test`). Rules are `domain:library`'s.
  - LIB-56: the regression file and `FormatRegressionTest` are `domain:library`'s (189 → 192 domain tests pass, including it).
  - LIB-48: a library file opens as `라이프 레코더 · 기록 n`, merges twice without duplicates, a recovery-locked one asks for the 복구 암호 (`RecorderFileTest`).
  - LIB-43: blocks, layers, `빼기`, pagination (cover alone, page numbers, no speaker at a page foot, text inside the margins), `PDF 암호` (V5 / R6, AES-256; domain `PdfEncryptionTest` decrypts with Algorithm 2.A and PDFKit opened the sample), the gate (`PdfExportTest`).
  - LIB-57: B-01 … B-04, B-07 on the runner (hidden left out without a 복구 암호 with `숨긴 프로젝트 1 · 백업 안 됨`, locked with one, never named in the index or readable in the file, unchanged reused, failure streak, prune), schedule, nothing runs while off or not connected, LIB-24 `지우기 ›` (`BackupTest`).
  - LIB-58: material without the password, the facts follow it, `강도 · 좋음 · 14자`, `정하기` needs 8 matching characters, `함께 잠금` rows (`BackupTest`, `MultiExportTest`).
  - LIB-57 (B-04): a block-only edit uploads a new version, because the 서재 index time (not `Project.updatedAt`, which the store does not stamp) decides `unchanged`.
  - LIB-59 / LIB-60: `RestoreEnd` (first run ends onboarding only with something restored); a full round trip onto a new device (no local material, Drive index only): wrong 복구 암호 refused before any import, hidden project stays hidden, visible stays visible, recorder merged once, progress ends at 100 %; unchecking the hidden row needs no password (`BackupTest`).
  - LIB-72: one file per project, own defaults, unique names, a locked file opens with its own password (`MultiExportTest`).

**Batch 6 (pure logic, tested):**
- **LIB-61 rules:** B-09 (`백업 실패 · 3일째` · `Google 로그인 만료 · 원고는 기기에 안전`, Drive row `마지막 성공 11월 18일 08:12`), B-10 (`1일째`; a success clears the banner and the row reads `마지막 백업 오늘 09:00`) through `BackupSettingsState` (`SettingsBatch6Test`; the banner text itself is domain:library's).
- **LIB-62 rule:** the `기기 저장 공간` line (`남은 공간 1.2 GB · 프로젝트 340 MB`) and its empty sub before the sizes are read.
- **LIB-66 … LIB-68, CNV-19, WLD-67 rows:** `DisplaySettingsState` builders against the boards' factory values, the four value pickers (choices in the sub's order, one selected) and the `choose` reducer (unknown id = no change).
- **CNV-18:** `SettingsRoutes.routeOf` maps every 설정 row to its screen (all registered: this lane's, recorder's `RecorderSettings`).
- **CNV-20:** the 프로젝트 설정 row map (LIB-69 tests) plus the `태그` row → `TagSettings`.
- **STO-17 (this lane's half):** a new project takes `때 표기 기본` (`ProjectCreator.build`).
- **`preview.webp`:** `CoverPreview.source` rules, the package carrying it only for a visible project, and such a package reading back through `OpenFileFlow` to the `열기` sheet (`CoverPreviewTest`).
- **`MCarousel`:** `ShelfCards.parallaxShift` (0 for the settled card, 8 % one stride away, capped).
- **Cross-lane, recorder LRC-43:** the 휴지통's `레코더` row of a hidden record already asks the `숨긴 기록 보기` prompt before `RecorderActions.restore(…, authenticated = true)` (`TrashActions.gate` on `restoreNeedsAuth`, batch 2). Nothing changed.
- **Cross-lane done in batch 6:** story STO-42 (`이동 점검`, `장소 없는 씬 알림`, `때 표기 기본` rows in `SettingsMore`), canvas (`기본 이미지 · 보기 ›` → `PresetImages`), cast CST-12 (`태그` row → `TagSettings(p)`), map WLD-61 (`지형 만들기 · 글로 설명` row stores `AppSettings.textMapEngine`; the map lane reads it).

## Partial

Every UI feature of batches 1 and 2 is partial: none has been seen on a device.

- **LIB-01 서재.** Built:
  - header `Civilight` / `서재`, with `+`, or `다시 잠그기` while the group is open
  - `프로젝트 · N` and rows newest first: title, badge, meta, `쓰는 중`, ⋯
  - the locked group row with `지문으로 보기`, and the open group with `숨김 풀기`
  - `라이브러리 · 프로젝트 밖` › `라이프 레코더` (`기록 n · 정리 안 됨 u`)
  - command bar `모든 프로젝트에서 찾기` + mic
  - the `OnbEmpty` empty card

  Batch 3: the `ViewToggle` and the 설정 button in the header.
- **LIB-02 ⋯ sheet.** Rows from `ProjectMenus`: `열기` · `이름 바꾸기` · `수위 등급` · `프로젝트 숨기기`/`숨김 풀기` · `백업 · 내보내기` · `지우기`. The example shows only `열기`.

  `백업 · 내보내기` opens the shared `ExportSheet` (LIB-41, batch 2).
- **LIB-03 delete confirm.** Built: counts, NFC name match, `휴지통으로` in `r18`. Confirming pops the project's screens, moves the project to the trash through `LibraryStore.moveToTrash` and removes its recents.
- **LIB-09 … LIB-12 new project.** Every field, radio card, picker, the R-18 block, the level editor, the preview tree, the record checklist, the bundle picker and the hide switch are built. Left:
  - `TODO_COPY` labels for the level editor buttons
  - a device check of the wide frame (reworked to the `NewWide` board in batch 4, LIB-14)
- **LIB-22.** Badges via `ThemeBridge.rating` on rows, sheets and settings. The PDF cover rating belongs to LIB-43.
- **LIB-23 설정 · 수위 등급 and the `같이 바꾸기` list.** Reachable since batch 3: 서재 › 설정 › `숨기기` › `수위 등급별 동작`.
- **LIB-24 `바뀌는 것` sheet.** The ◇ row `복구 암호 없음 · 정하기 ›` pushes `RecoveryPassword` (built in batch 5). The row `이전 백업 n · 암호 없음 · 지우기 ›` shows once unencrypted Drive versions exist; `지우기 ›` deletes them (`BackupServices.deleteUnencrypted`, batch 5).
- **LIB-25.** Section `등급 · 숨기기` of 프로젝트 설정: rating row → picker → sheet, and four switches with `<rating> 기본과 다름` in `acc`. The other LIB-69 sections came in batch 3 (see LIB-69).
- **LIB-26 feature side.** Every surface of this module that shows a project's content outside its own routes calls `ProtectWindow(WindowProtection.forProject(…))`:
  - open-group rows
  - the ⋯ sheet
  - the delete dialog
  - the rating sheet
  - hidden projects in the bundle picker and the `같이 바꾸기` list
  - the 휴지통 rows that show a project's content, and the export sheet (batch 2)

  Project routes are protected by the shell. Batch 3: open-group project cards and 최근 연 파일 cards that name content (masked cards do not, S-12).
- **LIB-27.** The rule is `domain:library` `ProjectSurfaces.displayProjectName`, the one function for every surface outside a project.
  - In this module, hidden projects never appear by name while the group is locked. This covers rows, sheets restored after a restart, the bundle picker and the `같이 바꾸기` list.
  - R-18 rows show the real name with the red badge (S-01).
  - 최근 연 파일 cards (batch 3) show `R-18 프로젝트` without the item title for a name-masked project, and leave hidden projects out. Left: widget and notification surfaces (other lanes and later batches).
- **Hidden projects created or hidden in this session** (LIB-12, LIB-24, LIB-25, ⋯ `프로젝트 숨기기`): they join the hidden group and stay open until the next re-lock through `UnlockController.keepOpen` (batch 2, core pass). Not seen on a device yet.
- **LIB-29 (feature side).** Shell-built (FLAG_SECURE, recents masking, task label). This module adds `ProtectWindow` on its batch-2 surfaces outside project routes (휴지통 rows, export sheet). Nothing else is feature-side.
- **LIB-31.** Hide (⋯, switch, rating sheet, step 4) and unhide (open group, switch, rating sheet) were built in batch 1. Batch 2 added kept-open and the Q19 refusal with `TODO_COPY_LIB31_NO_DEVICE_LOCK`. The Q18 extra warning for hiding without a 복구 암호 is not built (copy not in sources; default: none).
- **LIB-32.** Shell: the prompt. Feature: `지문으로 보기` (batch 1) and the `MUnlock` motion (batch 2): the opened rows come out of a 7 px blur in 320 ms, none with reduced motion. `Modifier.blur` draws only on API 31+. The reverse blur on re-lock came in batch 6 (see Architecture, shelf motion). Left: the re-lock note for the two non-default policies (`TODO_COPY_LIB32_*` from domain:library).
- **LIB-33.** Shell: the policy and events. Feature: `다시 잠그기` in the header (batch 1). Kept-open projects re-lock with the group. The policy screen is 설정 · 숨기기 (LIB-64, batch 3).
- **LIB-34.** Entirely shell-built (`AppLock`, `RHLockMark`, `서재로`). No feature-side part.
- **LIB-37.** Every gate in this module treats anything but `SUCCESS` as cancel with no toast: group unlock, unhide via switch or sheet, `같이 바꾸기`, trash restore and purge of masked rows, export `넣기`. Q19 (no secure lock screen): hiding is refused (see LIB-31).
- **LIB-38 휴지통.** Built: route `Trash(filter)`, header `30일 뒤 자동 삭제` / `휴지통` / `비우기`, filter chips, rows with `<d>일 남음`, `되살리기` · `영구 삭제` tags, block groups that open to their blocks with their own `되살리기`, the `씬도 되살리기` · `다른 씬 끝에` sheet with a scene picker, masked rows while locked, one prompt before restoring hidden things, restore toasts (CST-43; projects `<name> 되살림`, `ED`). Left:
  - entry: 설정 › `휴지통` (batch 3, §6 Q27 `ED`); other lanes may push `Trash(filter)`
  - inner-screen layout is the 560 centred column, not a list + detail (`ED`)
  - empty-state copy `TODO_COPY_LIB38_EMPTY` (batch 6: drawn in the DS `EmptyBlock`, LIB-70)
- **LIB-39.** Built: `영구 삭제` and `비우기` with confirms (`TODO_COPY_LIB39_*`), name tags through `PurgeOps`, recorder links keep labels, blob collection after purges, the 30-day purge on opening the 휴지통. Left: a daily background job (WorkManager is not a dependency; core-store's `runMaintenance` covers whole projects and records at start).
- **LIB-40.** Shell: the toast host. Feature: restore toasts in the 휴지통, the Q19 toast, the export done and failure toasts (`TODO_COPY_LIB41_*`).
- **LIB-41 내보내기.** Built as described in Architecture: header and `<name> · <badge>`, four format cards, every option row per format (`범위` and `표시 레이어` open inline checkbox lists, `숨긴 씬` `넣기 · 빼기`, `숨긴 인물` info, `비밀 항목 포함`, `파일 이름`, `PDF 암호`, `암호` with `한 번 더` and the warning line, `레코더 기록 n` with `원본 음성 포함`, `휴지통도 포함`), warnings, the primary button, the gate, the save dialog. Left:
  - `PDF 만들기` works since batch 5 (LIB-43)
  - primary labels for CSV, JSON and `.civilight` are `TODO_COPY_LIB41_CREATE_*` (from domain:library)
  - the `암호` and `PDF 암호` fields have the `보이기` eye button since batch 6 (shared `PasswordInput`; the hide label is `TODO_COPY_LIB58_HIDE`)
  - the save dialog answer is lost if the process dies while the dialog is open (nothing is written or fixed; the author exports again)
- **LIB-45 engine JSON / LIB-47 `.civilight` / LIB-49 package.** Bytes from `domain:actions` `ExportActions.ENGINE_JSON` and `domain:library` `ProjectFileExport` (latest format, `mimetype` first and stored, manifest hashes, optional encryption). The feature side is the sheet, the blob reader, `writtenBy` (`Civilight <versionName> (Android)`) and the save. `preview.webp` is written since batch 6 from the project's own cover picture (none for presets or hidden projects).

- **LIB-04 card view (batch 3).** Built: `ViewToggle` in the header in both views (remembered as `viewByScreen["library"]`), `프로젝트 · N · 숨김 M` with pager marks, one snapping row of 3:4 `ImageCard`s (cover image, preset art or the initial letter; holo foil; badge, title, meta, `쓰는 중 · 홀로` chip, ⋯ → the same ⋯ sheet), the lock card with `지문으로 보기`, the wide row (236 × 336, `새 프로젝트` card, 4 recents) when the 서재 is ≥ 560 wide. Batch 6: `MCarousel` parallax 8 % and the tick on settling. Left: the `MCardView` morph (list ↔ card); preset pictures are abstract shard art until CNV-26 provides the 8 presets; pager marks follow the first visible card.
- **LIB-05 최근 연 파일 (batch 3).** Built: recording of every scene, character, page, term, place and faction screen opened while the 서재 is on the stack; masking per R19 §3/§6 (computed each time, so raising to R-18 applies at once); cards with image, kind line, title, time; tap opens 프로젝트 홈 then the item. Left: opens made while the 서재 is not on the stack (a widget or notification start) are not recorded (see Requests); scene cards have no picture (`ED`: scenes have no image).
- **LIB-06 (batch 3).** The shell draws `TwoLibrary` (390 list | 프로젝트 홈) and `ColLibrary` (560 column, `CenterColumn`); the 서재 marks the selected row in the list pane. Left: fold/unfold keeping the selection and scroll is the shell's CNV-34; in `목록 + 상세` the card view keeps the cover row (see Spec conflicts).
- **LIB-07 (batch 3).** The 서재 command bar pushes `LibrarySearch`, a story route. The scope rule is `domain:library` `CrossSearchScope.of(profiles, access, settings)` (tested here with the 숨기기 switch). Left: the search screen itself (story lane, Requests).
- **LIB-52 … LIB-54 (batch 3).** Built: `RHOpenFile` (title, badge + `씬 · 인물 · 크기 · 날짜`, the same-project block with `새 프로젝트로` · `덮어쓰기` radios, `취소` · `열기`), `RHNewerFile` (rows, note, `닫기` · `Civilight 업데이트` → store listing), `RHEncryptedFile` (lock tile, file name, notes, `암호`/`복구 암호` field, error line), the import and the stack after it, window protection while a protected file's name shows. Left: copy for failures (`TODO_COPY_LIB52_*`, `LIB54_*`, `LIB55_DAMAGED`); the ◇ line for skipped optional features (copy not in sources); the intent filter's reach on real file apps (shell Partial).
- **LIB-63 설정 (batch 3).** Built: head `Civilight` / `설정`; `화면` (theme segmented applied live, 글자 크기, 펼친 화면 배치, 움직임 · 투명도, 지도 · 이미지 · 연표), `보관` (Drive row showing the switch, opens 설정 › 보관; 복구 암호 `정하기`/`바꾸기`), `보안` (앱 잠금 switch with the Q19 refusal and the off prompt; 숨기기 with `숨긴 프로젝트 n · 다시 잠그는 때` and the policy), then `휴지통`, `라이프 레코더`, `도움말 · 안내 다시 보기`. Every row opens a built screen since batch 6 (CNV-18). `마지막 백업 <time>` reads the backup book and shows once a Drive run succeeded.
- **LIB-64 설정 · 숨기기 (batch 3).** Built: intro, the three re-lock radios, `검색 · 명령 결과` switch, `위젯 · 최근 항목 · 알림` on and disabled (§6 Q28), `지문 · 실패하면 기기 PIN · 사용 중`, `숨긴 것도 백업` switch, `수위 등급별 동작 ›`.
- **LIB-65 글자 크기 (batch 3).** Built: theme segmented, the 5-step slider between small and large `가`, the preview line in the live body style, `본문 <sp> · 행간 1.7`.
- **LIB-69 프로젝트 설정 (batch 3).** Built: all four sections with live values, every `… ›` row wired to its owner's route, the two script switches, `단계 이름` editor, the example read-only. Every owner's target screen is registered now (checked in batch 6, CNV-20); batch 6 added the `태그` row.

**Batch 4 (none seen on a device):**
- **LIB-08 (feature side).** Built: every onboarding page records itself as the resume page during the first run; `Onboarding`, `Walkthrough`, `ExampleTour` are registered, so the shell's start routes land on real screens. Left: nothing feature-side; the 250 ms bar-drawing splash phase is the shell's (CNV-38).
- **LIB-13.** The block is drawn by feature:story's home (`HomeStartModel`). Feature side: `domain:library` `StartBlock.ofProject(doc, recorder, ratingDefaults)` derives `숨김 · 끔` from the document (Request to story to call it). Left: the `주인공 만들기` sub copy (`TODO_COPY_LIB13_PROTAGONIST_SUB`).
- **LIB-14.** `NewWide` reworked to the board (see Architecture). Left: a device check.
- **LIB-15.** Built: `OnbWelcome` (blurred manuscript, glass wash, wordmark, title, lines, `시작하기`, `백업에서 되살리기` → `RestoreBackup`), `OnbLook` (swatches applied live, slider, preview line), `OnbSafe` (Drive card + `연결`, app-lock switch with the 설정 rule, note), `OnbVoice` (recorder card, `마이크 허용` → system prompt, `홈 화면 위젯 추가` → launcher pin request; each card shows as chosen once the device reports it), `나중에` · `다음` · `건너뛰기`. Inner screens: the centred 560 column. Left: Google sign-in is a labelled stub (`UnavailableDrive`, needsExternal, LIB-57) and says so with `TODO_COPY_LIB15_DRIVE_NOT_READY`; `TODO_COPY_LIB15_WIDGET_UNSUPPORTED`. `백업에서 되살리기` opens the LIB-59 screen (batch 5).
- **LIB-16.** Built: three pages with the live demos (block swipe with the three tabs, lens chip and values swapping, recorder glow with the spoken line), 3-bar progress, `k/3`, banner, `이전` · `다음` / `예시 둘러보기`, `건너뛰기`; replay from 도움말. Left: device check of the demo timing.
- **LIB-17 (feature side).** Built: 예시 둘러보기 (`OnbExample`) with the floating read-only banner, rows from the document, `온보딩 끝내기`; resume `EXAMPLE` / `EXAMPLE_SCRIPT`. Left: `OnbExampleScript` (banner and `예시 홈으로 · 온보딩 끝내기` over the script) is the script lane's screen (Request); read-only enforcement is each area's.
- **LIB-18.** Built: `온보딩 끝내기` (permanent delete, `firstRunCompleted`, empty 서재), the trash mark before `예시 프로젝트 삭제됨`, the notice used up by the first project, resume after leaving mid-onboarding (shell + resume ids). Left: device check.
- **LIB-19.** Built: the framework (`TourHost`, `TourStepCard`, `TourPlacement`, `ModuleTourHost`) and the rules in `domain:library` for every module. No tour runs yet: every module has undrawn steps (`TODO_COPY_LIB19_*`), and other lanes cannot reach the host until it moves to core:ui (Requests). The 서재 · 숨기기 tour has no board (§6 Q15).
- **LIB-20.** Built: the rule (`FirstUseTips`), the switch (LIB-21) and the presenter `showFirstUseTip`. Left: tip ids and copy belong to each area (not in sources); a shared presenter in core:ui (Request).
- **LIB-21.** Built: the `HelpReplay` screen as drawn, wired to `OnboardingState` and the tips switch. Left: device check.
- **LIB-28.** Rendering is feature:recorder's (it reads `recorderLinkMask`); the switches are LIB-23 / LIB-25's. Feature side: the agreement test. Nothing left here.
- **LIB-30.** Built: `LibraryNotices` (masked texts). Left: posting them (LIB-57 backup, LIB-60 restore; export finishes in the foreground and shows a toast), `POST_NOTIFICATIONS` (Request), lead words `TODO_COPY_LIB30_*`.
- **`Licenses` route (not in this batch's ids):** not built. No board or copy draws it; the Pretendard OFL text already ships in the APK assets (`assets/licenses/Pretendard-OFL.txt`), which is what the licence requires. A later batch can show it from 도움말 once its row copy exists.
- **LIB-35 (feature side).** 설정 (batch 3) and `OnbSafe` (batch 4) share `SettingsViewModel.setAppLock` (`AppLockRules`): on needs a device lock, off asks once. The lock itself is the shell's.

**Batch 5 (none seen on a device):**
- **LIB-36.** The rule and every surface table are `domain:library` (`itemSurface`, I-01 … 18) and the shell (`unlockItems`); the lists are each module's. This module's own surfaces follow it: 휴지통 masked rows with one prompt (batch 2), 최근 연 파일 omits hidden items (batch 3), exports name hidden speakers only, `.civilight` and Drive backups lock projects with hidden items by default, 복구 암호 `함께 잠금` counts them. Left: nothing here; `ItemHide` lists are cast / world / canvas / story.
- **LIB-42.** Built: the sheet rows and defaults, the `넣기` gate for PDF, CSV and JSON, the JSON ▲ warning line, hidden records in `.civilight`. Left: device check.
- **LIB-43.** Built: the PDF (cover, heads, lines, layers, pages) through `PdfDocument`, `PDF 만들기` on, `PDF 암호` = AES-256 R6. Left: a device check of the real `PdfDocument` output through `PdfEncryption` (the parser expects a classic xref table and no object streams; anything else is refused, never written plain); fonts differ from the spec (Spec conflicts).
- **LIB-44 / LIB-46.** Bytes and fixation are `domain:actions` / `domain:library`; this module saves and fixes after the save. Left: the core:store write-without-undo request (fixation is still an undo step `내보내기`).
- **LIB-48.** Built: open and merge of `.civilight-recorder` (파일에서 열기) and the library file in every Drive backup. Left: the manual export entry is the recorder area's (undrawn); the app's intent filter does not list `application/vnd.civilight.recorder` or `*.civilight-recorder` (Request).
- **LIB-55.** Built: partial import with the line on the sheet and the toast. Left: copy for the fatal case (`TODO_COPY_LIB55_DAMAGED`).
- **LIB-56.** `domain:library` holds the file and the test; nothing feature-side.
- **LIB-57.** Built: everything but Google: the plan, files, recovery lock, recorder and settings files, prune, run index, failure streak, the daily run at start (`ED`), 설정 › 보관 with the switch, `백업 기록 ›`, `복구 암호` and the warnings, onboarding `연결`, the LIB-24 `지우기 ›`. Left (needsExternal): the Drive client (`UnavailableDrive` is a labelled stub, so the switch cannot turn on and no run happens), WorkManager periodic work with the Wi-Fi constraint, the done notification (`LibraryNotices.backupDone`, `POST_NOTIFICATIONS` Request).
- **LIB-58.** Built: the `RHRecoveryPw` screen (card, field + eye, 4-bar meter, `한 번 더`, ◇ warning, `함께 잠금`, bottom `정하기`), Keystore-wrapped data key, 설정 rows `정하기 ›` / `바꾸기 ›`. Left: strength words other than `좋음` (`TODO_COPY_LIB58_*`); device check of the Keystore.
- **LIB-59.** Built: the `Restore` screen (account line, `백업 · 30일` rows with the selected bar, the checklist, `같은 이름이 이미 있을 때`, note, `<M월 D일> 백업 되살리기`), the 복구 암호 sheet with retry, the first-run end (`OnboardingActions.finish()`). Left (needsExternal): with the stub, the list is empty and `연결` says `TODO_COPY_LIB57_DRIVE_NOT_READY`.
- **LIB-60.** Built: the `Restoring` screen and the job (bytes, `약 <n>초`, three steps, note), toast at the end. Left: a foreground service so it survives the app being closed (`ED` app scope; Request), the done notification.
- **LIB-72.** Built: the 서재 header `내보내기` button, checklist, per-file `암호`, folder picker, one file per project. Left: copy (`TODO_COPY_LIB41_CREATE_FILE` is the button; §6 Q23 entry undrawn); device check of `DocumentFile` writes.

**Batch 6 (none seen on a device):**
- **LIB-61.** Built: the `ErrBackup` banner (DS `ErrorBanner`, `r18` outline, warning icon), `다시 로그인` (sign-in, then a run) and `지금 다시 시도` (a run now), both off while a run goes; the Drive row's `마지막 성공 …`. Left (needsExternal): with `UnavailableDrive` no run ever fails or succeeds, so the banner cannot appear on a device; the buttons show `TODO_COPY_LIB57_DRIVE_NOT_READY`. Reasons other than the login (`TODO_COPY_LIB61_NO_SPACE`, `_NETWORK`, `_UNKNOWN`, from domain:library) have no copy.
- **LIB-62.** Built: 설정 › 보관 in the board's order (banner, `보관`: Drive switch, `기기 저장 공간`, `백업 기록 ›`, `복구 암호 ›`, warnings). Left: the storage detail screen (not drawn, `ED` P2), so the row opens nothing; the Drive part is needsExternal as in LIB-57.
- **LIB-66.** Built: the `LayoutSettings` screen; the choice applies live through the shell. Left: device check (inner screen).
- **LIB-67.** Built: the `ReduceSettings` screen with the previews; values apply live (`ThemeBridge`, `GlassMode.OPAQUE`). Left: device check of the glass preview (a plain `glass` fill, no backdrop blur inside the small sample).
- **LIB-68 / CNV-19 / WLD-67.** Built: every `SettingsMore` row with its control; value rows open a radio sheet (`ED`, no board). What each value does belongs to its owner (map lane: style, engine, export size; canvas: holo default, presets; story / checks: the two switches, read by `domain:checks` `CheckInputs.of`; this lane: `때 표기 기본` for new projects).
- **CNV-18.** The row map is built and tested; left: a device check of the 설정 screen.
- **CNV-20.** 프로젝트 설정 rows match the row map; `도움말 · 안내 다시 보기` (LIB-21) and the recorder's `LrSettings` are built by their owners. Nothing left here.
- **LIB-70.** The shared empty form is the DS `EmptyBlock`. This lane's instances: `OnbEmpty` (서재, `새 프로젝트` · `백업에서 되살리기` → `RestoreBackup`, checked) and the 휴지통 (title `TODO_COPY_LIB38_EMPTY`). `EmptyCast`, `EmptySearch`, `EmptyCheck`, `EmptyLr` are their hosts' (cast, story, story, recorder).
- **LIB-71.** The banner form is used by LIB-61; loading uses bars and stepped lists (`Restoring`). This module draws no spinner anywhere (checked); short waits show nothing. `ErrBio` is system UI, `ErrMic` the recorder's.
- **Loose ends closed:** reverse `MUnlock` blur (LIB-32), `MCarousel` parallax and tick (LIB-04), `preview.webp` (LIB-47, LIB-49, also in Drive backups), eye button on export passwords (LIB-41), `때 표기 기본` for new projects.
- **Still open, each blocked:** `MCardView` list ↔ card morph (a shared-element motion between the list and card blocks; not built, the switch is instant); LIB-19 tours (copy, core:ui host); LIB-30 notifications (app manifest `POST_NOTIFICATIONS`, channel); LIB-60 foreground service (app); LIB-57 / 59 Drive and WorkManager (build files, Google); LIB-39 daily job (WorkManager); the core:store write-without-undo; `Licenses` (no board, no copy); every `TODO_COPY_*` in Requests.

**Hazards for later batches**
- **Reverse `MUnlock` (batch 6):** the just-locked rows stay drawn for up to 320 ms after the header `다시 잠그기` tap. Keep its guards if it changes: armed only by that tap, never by a policy re-lock; inert and silent to accessibility; still under `ProtectWindow`; off with reduced motion.
- **Hidden imports without a device lock (batch 3):** see the Q19 default; the import does not check `deviceSecure`. The project is in the 서재's hidden group and cannot be opened there until the device has a lock screen.
- **최근 연 파일 recording (batch 3):** `RecentsRecorder` runs in the 서재's view model, which exists only after the 서재 has been composed in this process and while its entry stays on the stack. After a process-death restore onto a project screen, or a start from a widget or notification without the 서재 below, nothing is recorded until the user is back at the 서재.
- `LibraryStore.moveToTrash` closes the project's store while `ProjectSessions` may still cache it for 15 s; a cached closed store throws on the next edit. The 휴지통 calls `ProjectSessions.closeIdle()` before restoring or purging a project. Any other path that reopens a just-trashed project must do the same. Not checked on a device.
- `ProjectWriter.purge` clears the project's undo history (whole-document snapshots would bring purged rows back). An open screen of that project loses its undo steps after a 휴지통 purge.
- `SaveDialog` registers on the activity's result registry under one key without a lifecycle. Two export saves at once would share the key; the sheet prevents that (it closes before the dialog opens).
- The DS sheet keeps the last sheet composed while it animates out. If another lane reopens `RatingChangeSheet` with the same key during that exit animation, the old composition (with `CloseOnce.closed = true`) is reused, and its close buttons stop working. Paths through `RatingPickSheet` never do this.
- **Onboarding (batch 4):** `온보딩 끝내기` resets the stack to 서재 and deletes the example; any screen of the example still open elsewhere (script, cast, world) is cleared by that reset. Another area must never delete the example itself: send `ExampleTourContract.FINISH_REQUEST`.
- **Tour host (batch 4):** `TourHost` measures targets in root coordinates; a target inside a scrolled list moves with it, but one scrolled off screen leaves the hole where it was last placed.
- **Batch 5:** `RestoreJob` runs in the app scope; leaving the app keeps it running only while the process lives. A second restore cannot start while one runs. `BackupBookStore` and `AppStateStore` share `files/app/` (different files). The backup hook reads every live project document at start when a run is due (only with Drive connected). `PdfEncryption` refuses PDFs with object streams or incremental updates; if a device's `PdfDocument` ever writes those, `PDF 만들기` shows the save-failed toast for locked PDFs (never a plain file).
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
- **휴지통 (batch 2):**
  - The example project's own trash rows are not listed (it is read-only and not a 서재 row).
  - A masked row (`숨긴 …`) asks for the prompt before `영구 삭제` too, as its `되살리기` does; `비우기` never prompts and leaves masked rows (LIB-39 `ED`).
  - Restore toasts: items use `domain:trash` / `domain:actions` copy; a whole project `<name> 되살림` (the CST-43 pattern); block groups none. A masked row's restore shows no toast (it would name what the lock hides).
  - The 30-day purge also runs when the 휴지통 opens (`runMaintenance` for whole projects and records, `PurgeOps` per live project); a purge write is not an undo step.
  - Block groups open with a visible chevron button on the row (the board draws no control); each block line shows `#043` and its first line with `되살리기`.
  - `다른 씬 끝에` picks among live scenes with the shared `UnitPickerSheet` (scene level only).
- **내보내기 (batch 2):**
  - The sheet opens on `대본 PDF` (§3 `ExportOptions` default; board `Export`); switching cards re-seeds that format's defaults, keeps a typed file name and the range, and drops typed passwords.
  - `범위` and `표시 레이어` open inline checkbox lists under their rows (no sub-sheet: a shared sheet cannot open another lane's local keys); `범위` offers `전체` plus every unit above 씬, labelled by `ExportSheets.rangeLabel`.
  - `파일 이름` must not be blank; `암호` needs both fields equal and not empty; `PDF 암호` (when on) must not be empty.
  - Saving: the gate and the build run in the sheet's own scope (leaving the sheet cancels them); the sheet closes when the system dialog opens. The draft stays in memory until `닫기` or a saved file, so reopening after a cancelled dialog shows the same choices, but typed passwords are blanked whenever the sheet leaves composition. Voice IDs are fixed after the bytes reached the chosen file, on the project as it is then, as one undo step named `내보내기`.
  - `writtenBy` = `Civilight <versionName> (Android)`; `preview.webp` since batch 6 (see Batch 6 defaults).
- **Device without a secure lock screen (§6 Q19 `ED`):** hiding is refused on every path the author takes (⋯, switch, rating sheet, `같이 바꾸기`), with one toast (`TODO_COPY_LIB31_NO_DEVICE_LOCK`); a new project that would be created hidden (R-18 default or the step-4 switch) is created visible with the same toast. Exception (batch 3): a file opened with 파일에서 열기 keeps its hidden flag, and a file locked with an export `암호` always lands hidden (LIB-51, LIB-54). On such a device that project stays behind the hidden-project lock until Q19 is decided (see Hazards).
- **프로젝트 설정 values vs board `ProjSettings` (batch 3):** the rows show the project's real values. On the example: `프로젝트 11` (board 7, fixture.md #12), `사이 11 · 마음 11` (board 사이 12, fixture.md #14), and the `ID 형식` sample is the last live line's current ID (`RIE_0107_048`; the board shows 하르트's `#043`, `ED`).
- **서재 › 설정 entry (batch 3, `ED`):** no board draws it. A 44 dp icon button (`IconId.Props`, label `설정`) between the `ViewToggle` and `+` in the 서재 header.
- **Card view (batch 3):** the open hidden group's projects show as cards after the visible ones while open (undrawn; `숨김 풀기` is in their ⋯ sheet), each protecting the window. The wide row (`LibCardsWide`) is used when the 서재 is at least 560 dp wide. Pager marks: one per card of the row. The initial-letter card is the first character in `onImage2` on `chip`; a preset cover draws shard art.
- **최근 연 파일 recording (batch 3):** the 서재 view model records the top route while the 서재 is on the stack (no area records opens itself). It prefers the open store's document, waiting up to 2 s, then the stored copy. The example is never recorded.
- **파일에서 열기 (batch 3):** the flow is a sheet over an empty `파일에서 열기` frame (boards draw it over the file app). A hidden same-key project asks for the hidden-project prompt first; cancel closes the flow. After `열기` the stack is `서재`, plus `프로젝트 홈` for a visible project; a hidden one lands in the hidden group. Wrong password: field error, retry, no lockout (LIB-54 `ED`).
- **설정 (batch 3):** the Drive switch row opens 설정 › 보관 and does not toggle (it needs a Google account, LIB-57). `앱 잠금` off asks once first (`ED`); on is refused without a device lock (§6 Q19). Text steps 16/17/18/20/22, only `보통` named.
- **Orchestrator report 2 (top bar after `만들기`):** the stack is `서재 › 프로젝트 홈`; `‹ 서재 · 새 프로젝트` is board `NewReady`, drawn by feature:story's `HomeScreen` while the project is new (`newReady`). No `NewProject` route stays under it (NewProjectTest). No change made.


- **Onboarding (batch 4):**
  - Pages are pushed (`다음`), so system back walks back through them; a page resumed after process death has nothing below it, and `이전` then swaps in place.
  - `마이크 허용`: the card's own sub `녹음할 때만 사용` is the reason shown before the system prompt (no extra explainer; copy not in sources). The two `OnbVoice` cards read as chosen from the device's answer (permission granted, a recorder widget placed), not from a tap.
  - `OnbSafe` `앱 잠금` starts off (§6 Q13); the board draws it on.
  - Walkthrough demos with reduced motion: the swipe shows the tabs revealed, the lens shows `시점 · 1-2`, the glow is still.
  - Replay of the walkthrough from 도움말 (§6 Q9): demos only, no banner, last button `마치기` (the tours' word; the board's `예시 둘러보기` would open a deleted example), `건너뛰기` and `마치기` return to 도움말.
  - The walkthrough counts as seen once its last button or `건너뛰기` was used (first run or replay).
  - `모든 안내 처음부터` also sets the walkthrough to `아직 안 봄`.
  - A module's `다시 보기` resets its flag; the tour runs on the next entry (spec `ED`).
  - First run without an installed example (seed failed): `예시 둘러보기` and `건너뛰기` end onboarding at once, and the empty 서재 does not say `예시 프로젝트 삭제됨`.
  - 예시 둘러보기 episodes: the scene's episode with the one before and after it.
- **Tours (batch 4):** a tour whose copy is incomplete never starts (LIB-19 rows `TODO_COPY_*`); the card sits below its target, above when it does not fit, else mid-screen without an arrow; back ends the tour like `건너뛰기`.
- **LIB-13 `숨김 · 끔` (batch 4):** derived from the current 설정 · 수위 등급 defaults, since the creation-time choice is not stored.
- **LIB-30 (batch 4):** restore done uses the backup rule (counts with hidden projects, names otherwise) after its lead word.

- **Batch 5:**
  - Drive (`ED`, LIB-57): app-data folder; one file name per project (`<projectKey>.civilight`), `library.civilight-recorder`, `settings.json`; versions are Drive versions; a run index file holds the runs and the names of non-hidden projects. Settings travel without the account id; restoring does not apply the settings file (`ED`).
  - Daily run: at app start when due (a day after the last success, an hour after a failed try), until WorkManager exists.
  - 설정 › 보관 warnings show while the switch is on (they describe the next run). The ◇ line opens 복구 암호 정하기.
  - 복구 암호 strength: 1 bar under 8 characters, 2 for 8 … 11, 3 from 12, 4 from 16 with letters, digits and others; one kind only loses a bar. Changing it needs no old password (a new data key; old backups keep theirs, §6 Q24). The eye button uses `IconId.EyeOff` (no open-eye icon in the DS).
  - Restore end: during the first run, onboarding ends only when something came back; otherwise 되살리는 중 goes back to 백업에서 되살리기 and onboarding continues (`RestoreEnd`). Outside the first run, the 서재.
  - Restore: the newest run is preselected; the checklist starts all checked; locked files ask the 복구 암호 once, checked against the first locked file before anything is imported; hidden projects import last; a file that cannot be opened is skipped and counted (`TODO_COPY_LIB60_PARTLY_FAILED`).
  - 대본 PDF: A4, body 11 pt × 1.7, margins 64 / 72, cover page alone, page numbers centred at the foot, speaker = the real name (as the CSV), choices as `▸` lines, directions `▸`, memos `⌁`, header `%PDF-1.7` after encryption, all permissions with a random owner password.
  - `.civilight-recorder` sheet: `라이프 레코더` · `기록 <n>`, `취소` · `열기`; after `열기` the 기록함 opens over the 서재.
  - 여러 프로젝트 내보내기: entry = a `내보내기` icon button in the 서재 header (shown while there is something to list); files are written into one chosen folder; a name already used in the batch gets ` (2)`.

- **Batch 6:**
  - The `SettingsMore` value rows (`새 지도 그림`, `지형 만들기 · 글로 설명`, `큰 지도 내보내기`, `때 표기 기본`) open one radio sheet each, titled with the row title, listing the choices the sub names (`클라우드` keeps `정확도↑ · 전송` as its sub); a pick writes and closes. No board draws these pickers.
  - `기기 저장 공간`: free = usable space of the volume holding the app's files; projects = the live and the trashed projects' directories (with history) plus the blob store (images, recorded audio). No `›`, no tap (no detail screen is drawn, LIB-62 P2).
  - `다시 로그인` runs a backup right after a successful sign-in, so the banner clears when Drive answers. A cancelled sign-in changes nothing, silently.
  - The reverse `MUnlock` plays only after the 서재 header's `다시 잠그기` tap; every other re-lock removes the rows at once, so nothing hidden is drawn again on return.
  - `MCarousel`: the tick plays when the row comes to rest on a different first card; parallax shifts the picture against the scroll by up to 8 % of the card width, scaled just enough to cover the card while it moves (no scale at rest).
  - The LIB-61 banner shows only while `Google Drive 자동 백업` is on (with it off nothing runs, like the warnings).
  - `preview.webp`: only the project's own cover picture (3:4 crop, 360 px wide, WebP quality 80); none for a preset cover or no cover, never for a hidden project.
  - `태그` row: sub = tag names in order, no value word (`›` alone), no sub while there is no tag.

## Spec conflicts

- **LIB-11 `세계 달력 만들기`** says it "opens the world-calendar editor of the Timeline area, then returns" during step 3. That editor is the project route `WorldCalendar(p)`, which needs an existing project, and nothing is created before `만들기` (§2.2). Chosen: open it right after `만들기`, on top of 프로젝트 홈.
- **Board `NewStart`** reads `라이프 레코더에서 가져오기 · 3개 중 2개 고름`. The spec (`연결` vocabulary, R23) reads `라이프 레코더 기록 연결 · <total>개 중 <k>개 고름`. The spec wording is used (domain:recorder `startMaterialHeader`).
- **DECISIONS §1** names `ProjectTrash` for whole-project trash. The real move is `core:store` `LibraryStore.moveToTrash` (the same row rule as `domain:trash` `ProjectTrash.delete`), which this module calls. `ProjectTrash` is pure state with no I/O.
- **Export format cards:** board `Export` reads `게임 엔진 JSON · 블록 · 화자 · 연출 · 선택지` and `.civilight · 옮기기 · 보관 · 숨김이면 암호`; the spec (LIB-41, newer rule tabs) reads `씬 · 블록 · 화자 · 연출 · 선택지` and `옮기기 · 보관 · 숨김 · R-18 · 숨긴 항목 있으면 암호`. The spec wording is used (`domain:library` `ExportDefaults.formatCard`).
- **LIB-04 `LibCardsWide` vs CNV-33 `목록 + 상세`:** the spec calls `LibCardsWide` the inner-screen card view, but on inner screens the default layout puts the 서재 in the 390 list pane. Chosen: the wide row only when the 서재 itself is ≥ 560 wide (`가운데 한 칸`, or a single pane); in the list pane the cover row is used.
- **Board `LibCards` card size:** the board draws 222 × 296 cards; LIB-04 text says 236 × 296. The board is used (222 × 296; the wide board's 236 × 336 matches the text).
- **Trash row copy:** `domain:library` `trash.TrashHost` and `domain:trash` `TrashListing` both build rows; DECISIONS §1 names `TrashListing` for rows inside a project, so the 휴지통 uses `TrashListing` for every source (world rows read `<kind> · <project> · 참조 N`, `변화` rows `◆ <entity> · <field>`).


- **예시 둘러보기 one-liner (batch 4):** LIB-17 and board `OnbExample` read `꺼져 가는 하층 구역, 마지막 등불을 지키는 결사의 이야기.`; the example project's `oneLiner` (core:model `ExampleSettings.ONE_LINER`) is `꺼진 등불이 깜박인 새벽, 하층 거리의 견습 등불지기가 이름 없는 남자를 만난다.`. The screen shows the document's value (Request to core:model).
- **`OnbVoice` text extract vs HTML (batch 4):** `boards_text.txt` reads `원고 · 인물에 붙이기`; the HTML board and LIB-15 read `원고 · 카드에 넣기`. The HTML and spec are followed.
- **Recorder tour step 2 (batch 4):** board `TourLr` reads `붙일 곳` · `보던 곳 제안 · 지우면 기록함으로`; LIB-19 (newer `scr_decide.py`) reads `연결` · `보던 곳 · 넣기 전까지 내용은 그대로`. The spec is followed.
- **`NewWide` buttons (batch 4):** LIB-14 says `이전 · 다음` sit "bottom-right under the left column"; the board places them at the bottom right (right 40, 380 wide), under the aside. The board is followed.
- **`시작해 볼 것 · n/4` (batch 4, from the story lane):** the header keeps `/4` when the `연결된 기록` row is hidden (k = 0), as P-02 expects (`1/4` without records). Kept as the spec states it.

- **LIB-43 fonts (batch 5):** the spec asks for embedded Nanum Myeongjo (body) and Noto Sans KR (labels); the app ships only Pretendard. The body uses the system serif (Noto Serif CJK on most Android devices), labels Pretendard; `PdfDocument` embeds the glyphs used. Shipping the two fonts needs assets in the app (Request).
- **LIB-56 path (batch 5):** spec `ED` path `app/src/test/resources/format/<version>/`; the file lives in `domain/library/src/test/resources/format/1.0/` (domain-library.md), since lanes cannot write under `app/`.

- **`태그` row in 프로젝트 설정 (batch 6):** the `ProjSettings` board and the CNV-20 row map have no `태그` row; cast's CST-12 asked for one (its `TagSettings` screen has no other entry). Added at the end of `인물` with the screen's own title.
- **`SettingsMore` `이동 점검` sub (batch 6):** `boards_text.txt` reads `이동 시간 부족 · 동시에 두 장소`; the HTML board and the spec read `이동 시간 부족 · 같은 때 · 동시에 두 장소`. The HTML and spec are followed.

## Requests

- ~~**core:ui: a per-project kept-open set**~~ Done by the core pass (`UnlockController.keepOpen`); this module calls it from `ProtectionActions` and `ProjectCreator` and feeds `keptOpen` into `HiddenAccess` (batch 2).
- ~~**core:designsystem: three DS-12 icons**~~ Done by the core pass (`IconId.Pencil`, `Shield`, `Download`); the ⋯ sheet uses them and `common/LibraryIcons.kt` is deleted (batch 2).
- **core:store: a content write without an undo step** (`ProjectStore.update(…, recordUndo = false)` or similar). Two writes here should not be undoable: the voice-ID fixation after an export (LIB-46: "the app never changes it afterwards"; today it is an undo step named `내보내기`), and a 휴지통 purge (today written as a step, then `clearUndo()`, which drops the project's whole history).
- **core:ui / app (optional): keep a pending save-dialog answer across a re-lock.** `SaveDialog` registers on the activity's registry without a lifecycle, so the answer arrives after a re-lock; a process death while the dialog is open still loses it (nothing written, nothing fixed).
- **Copy needed (TODO_COPY):**
  - `TODO_COPY_LIB02_MORE_LABEL`: the accessible label of a row's ⋯ button (boards draw only the icon).
  - `TODO_COPY_LIB10_ADD_LEVEL`, `TODO_COPY_LIB10_REMOVE_LEVEL`: the `직접 정하기` editor buttons.
  - `TODO_COPY_LIB31_NO_DEVICE_LOCK`: toast when hiding is refused for want of a device lock (§6 Q19).
  - `TODO_COPY_LIB38_EMPTY`: an empty 휴지통; `TODO_COPY_LIB38_EXPAND_LABEL`: the block-group open/close button's label.
  - `TODO_COPY_LIB39_PURGE_CONFIRM`, `TODO_COPY_LIB39_EMPTY_CONFIRM`: the `영구 삭제` and `비우기` confirm titles (buttons use `영구 삭제` · `비우기` · `취소`).
  - `TODO_COPY_LIB41_DONE`, `TODO_COPY_LIB41_SAVE_FAILED`: after the save dialog; `TODO_COPY_LIB41_PASSWORD_MISMATCH`: under `한 번 더`.
  - Surfaced from domain:library:
    - `TODO_COPY_LIB09_RECORDER_LINE` (R-18 block)
    - `TODO_COPY_LIB24_DEFAULT_ON`
    - `TODO_COPY_LIB32_RELOCK_SCREEN_OFF`, `TODO_COPY_LIB32_RELOCK_FIVE_MINUTES` (open-group note)
    - `TODO_COPY_LIB41_CREATE_CSV`, `_JSON`, `_FILE` (primary buttons), `TODO_COPY_LIB41_PASSWORD_WARNING` (the `암호` warning line)
- **feature:story (LIB-07):** register `LibrarySearch(query)` and scope it with `domain:library` `CrossSearchScope.of(profiles, unlock → HiddenAccess, settings)` (hidden projects out while locked; while open only with `hiddenInSearchWhileOpen`; footer `숨긴 프로젝트 제외`; empty sub `CrossSearchScope.emptySub`). The 서재 already pushes it.
- **All lanes showing a scene, character, page, term, place or faction (LIB-05, optional):** opens are recorded by the 서재 while it is on the stack. A screen reached without the 서재 below (widget, notification) is not recorded; call `AppStateStore.recordRecent` with `feature.library` rules only if the orchestrator wants that (a core:ui helper would be the shared place).
- **core:designsystem:** a settings (gear) icon for the 서재's 설정 button; `IconId.Props` stands in.
- **Copy needed (batch 3):** `TODO_COPY_LIB35_NO_DEVICE_LOCK` (앱 잠금 refused without a device lock), `TODO_COPY_LIB52_NOT_A_PROJECT`, `TODO_COPY_LIB52_IMPORT_FAILED`, `TODO_COPY_LIB54_WRONG_PASSWORD`, `TODO_COPY_LIB54_UNSUPPORTED`, `TODO_COPY_LIB55_DAMAGED`.
- **Other lanes:** push `Trash(filter)` (e.g. `Trash(TrashFilter.SCENE)` after a scene delete) and open `ExportSheet(p)` from project menus; both are registered now.
- **FEATURES.md:** LIB-01 02 03 09 10 11 12 22 23 24 25 26 27 → partial (batch 1). LIB-29 31 32 33 34 37 38 39 40 41 45 47 49 → partial (batch 2; see Partial for what is left). LIB-04 05 06 07 52 53 54 63 64 65 69 → partial, LIB-50 51 → done (rules in domain:library, round trip tested here) (batch 3).
- **Batch 4 requests:**
  - **feature:story (LIB-13):** call `domain:library` `StartBlock.ofProject(doc, recorder, deps.settings.value.ratingDefaults)` instead of `StartBlock.of(doc, recorder)`, so the summary line ends with `숨김 · 끔` for an R-18 project created visible (R19-11).
  - **feature:script (LIB-17 `OnbExampleScript`):** while the project is the example (`LibraryEntry.isExample`) and onboarding is not finished (`OnboardingState.firstRunCompleted == false`), show the DS `ReadOnlyBanner("예시 프로젝트 · 읽기 전용", subtitle = "온보딩 후 삭제", stepCount = 4, currentStep = 3)` at the top, header `1-7 · 씬 3 · 2일째 새벽`, no command bar, and at the bottom `읽기 전용 · 편집 불가` with `예시 홈으로` (= `navigator.popToType<ExampleTour>()`) and `온보딩 끝내기` (= `deps.results.send("library.onboarding.finish", true)` then `popToType<ExampleTour>()`). Never delete the example yourself. This lane writes `resumePage = EXAMPLE_SCRIPT` when it opens the script from 예시 둘러보기.
  - **core:ui (LIB-19, LIB-20):** move `feature/library/.../tour/TourHost.kt`, `TourStepCard.kt` and `TourPlacement.kt` to `ui.tour` (they have no domain types), plus a `FirstUseTip` presenter like `tour/FirstUseTip.kt`, so every lane can run its module tour (`domain:library` `ModuleTours.shouldStart` / `markSeen`, targets with `Modifier.tourTarget(step)`) and its tips.
  - **core:designsystem (LIB-19):** `TourCard(…, previousLabel: String? = null, onPrevious: (() -> Unit)? = null)`; boards `Tour2`, `Tour4` draw `건너뛰기 | 이전 | 다음`. `TourStepCard` here stands in.
  - **app (LIB-30):** declare `android.permission.POST_NOTIFICATIONS` (API 33+) and a notification channel for backup, restore and export done; the texts are `LibraryNotices`.
  - **core:model (LIB-17):** the example's `ONE_LINER` differs from the line LIB-17 and the boards show (Spec conflicts); align the fixture or the spec.
  - **All lanes (LIB-19, LIB-20):** each module's tour targets and its undrawn step copy; tip ids and their copy. Module ids: `domain:library` `TourModule`.
  - **Copy needed (batch 4):** `TODO_COPY_LIB15_DRIVE_NOT_READY`, `TODO_COPY_LIB15_WIDGET_UNSUPPORTED`, `TODO_COPY_LIB19_*` (undrawn steps of every module, the whole 서재 · 숨기기 tour and its label, §6 Q15), `TODO_COPY_LIB30_RESTORE_DONE`, `TODO_COPY_LIB30_EXPORT_DONE`.
- **FEATURES.md (batch 4):** LIB-08 13 14 15 16 17 18 19 20 21 30 35 → partial; LIB-28 → done (rendering in feature:recorder, rule and switches here and in domain:library, agreement tested).
- **Batch 5 requests:**
  - **core:store:** own the backup book (`BackupRun` list, `BackupState`, `RecoveryKeyMaterial`, per-project uploaded content time) in `AppStateStore`; this lane writes `files/app/backup.json` through `JsonFile` meanwhile (`backup/BackupBook.kt`).
  - **build (feature/library):** `androidx.work:work-runtime-ktx` (LIB-57 daily periodic work, `NetworkType.UNMETERED`, battery not low) and the Google sign-in / Drive REST client (`play-services-auth` or Credential Manager + Drive API) to replace `UnavailableDrive`.
  - **app:** a foreground service type `dataSync` (or expedited work) for 되살리기 (LIB-60 `앱을 닫아도 계속`), `POST_NOTIFICATIONS` + channel (LIB-30, already filed), and `ACTION_VIEW` filters for `application/vnd.civilight.recorder` / `*.civilight-recorder` (LIB-48) so they arrive as `OpenFile(uri)`.
  - **app (assets, LIB-43):** Nanum Myeongjo and Noto Sans KR (OFL) for the PDF, if the spec's fonts are wanted.
  - **core:designsystem:** an open-eye icon for the password fields' `보이기` (`IconId.EyeOff` stands in).
  - **feature:recorder (LRC-46):** the library file in Drive backups is written here with `domain:library` `RecorderPackage` (same rule as `RecorderLibraryFiles`: hidden records only when locked with the 복구 암호 and `숨긴 것도 백업`); `RecorderLibraryFiles` may delegate to it, or keep it for a manual export.
  - **Copy needed (batch 5):** `TODO_COPY_LIB57_DRIVE_NOT_READY`, `TODO_COPY_LIB58_STRENGTH_WEAK` / `_FAIR` / `_STRONG`, `TODO_COPY_LIB58_HIDE`, `TODO_COPY_LIB58_MISMATCH`, `TODO_COPY_LIB59_NOT_CONNECTED`, `TODO_COPY_LIB59_NO_RUNS`, `TODO_COPY_LIB59_PROJECTS`, `TODO_COPY_LIB59_UNNAMED_PROJECT`, `TODO_COPY_LIB59_WRONG_PASSWORD`, `TODO_COPY_LIB60_PARTLY_FAILED`, `TODO_COPY_LIB60_NOT_RUNNING`, `TODO_COPY_LIB48_NEWER`.
  - **FEATURES.md (batch 5):** LIB-36 42 43 48 55 57 58 59 60 72 → partial; LIB-44 46 56 → done (rules in domain modules, the wiring tested here).
- **Batch 6 requests:**
  - **feature:map (WLD-67):** read `AppSettings.defaultMapStyle` (new map's 그림 style), `textMapEngine` (`지형 만들기 · 글로 설명` engine; replaces the per-session choice) and `largeMapExport` (default size of `큰 지도 내보내기`); 설정 › 지도 · 이미지 · 연표 writes them now.
  - **core:designsystem:** `ChoiceCard(…, leading: (@Composable () -> Unit)? = null)` for the `LayoutSettings` cards; `LayoutSettingsScreen.LayoutCard` stands in.
  - **feature:canvas (optional):** a 서재 entry to pick the project cover (`ImagePickSheet(p, PROJECT, p, PROJECT_COVER)`) has no board or row copy; `preview.webp` and the card view use the cover once one is set elsewhere.
  - **Copy needed (batch 6):** none new; `TODO_COPY_LIB38_EMPTY` (now the `EmptyBlock` title), `TODO_COPY_LIB57_DRIVE_NOT_READY` (the two banner buttons with the stub), and domain:library's `TODO_COPY_LIB61_NO_SPACE` / `_NETWORK` / `_UNKNOWN` still wait.
  - **FEATURES.md (batch 6):** LIB-61 62 66 67 68 70 71, CNV-18 19 20, WLD-67 → partial (screens and rows built and their builders tested; not seen on a device; the values' effects are their owners').
