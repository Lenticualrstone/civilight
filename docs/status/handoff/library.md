# feature:library handoff

## batch 1

Read docs/status/feature-library.md first. Its Architecture section lists every package and how to extend it.

**Structure**
- LibraryEntries.kt registers Library, NewProject(step), SettingsRating, RatingDefaultsApply, ProjectSettings and the shared RatingChangeSheet. It creates one LibraryServices per activity, holding deps, writer, protection, facts and draft.
- Conventions:
  - one package per screen group (shelf, newproject, rating, projectsettings, protection, common)
  - a pure …State builder with JUnit tests
  - thin composables
  - view models via entryViewModel; project screens subclass ProjectViewModel
  - one …Copy object per package, added to LibraryCopyLintTest's objects list
- Test fixtures are in LibFx (spec §4 sample projects made with ProjectCreator.build, keys unique per run).

**Write paths**
- All protection and rating writes go through protection/ProtectionActions. It holds the authentication rule, and becoming hidden forgets recents.
- Writes to a project other than the current screen's go through common/ProjectWriter.
- Whole-project trash is LibraryStore.moveToTrash.
- New projects go through newproject/ProjectCreator: NewProjectFlow, then FieldBundles, then RecorderHooks.linkRecords.
- The 서재 gates sheets and dialogs on ShelfUi.shows, so hidden projects never leak after a re-lock or a restart.

**Unfinished threads**
1. The shell has no kept-open set. A project hidden in this session goes behind the lock. Never call unlockHiddenGroup as a workaround.
2. ProtectionFacts is NoBackupYet. Swap it when LIB-57 and LIB-58 land; the rating sheet's 지우기 › row and the R-18 ◇ line depend on it.
3. LIB-13 is this lane's. It needs a slot in the story lane's ProjectHome. Pass CreatedProject.protections on from ProjectCreator.create for 숨김 · 끔.
4. ExportSheet, RecoveryPassword and RestoreBackup are still shell placeholders. SettingsRating needs an entry row in 설정 (LIB-63).
5. LIB-04 card view: add it to ShelfUi plus a ViewToggle. Domain has cardSectionLabel and HiddenGroupRow.cardSub.
6. LIB-14: the wide frame (NewWide) is already built in NewProjectScreen and needs only a check against the board.
7. LIB-69: add sections to ProjectSettingsUi and ProjectSettingsScreen.

**Hazards**
- moveToTrash closes a store that ProjectSessions may cache for 15 s. Check this before building restore and reopen.
- A RatingChangeSheet reopened with the same key during the DS sheet's exit animation reuses a closed CloseOnce.

## batch 2

Read the Architecture section of docs/status/feature-library.md first; it now has `trash/` and `export/` sections.

**What was added**
- `trash/`: the 휴지통 (`Trash(filter)` route). `TrashState` builds rows only through `domain:trash` `TrashListing`; `TrashDecisions` dispatches restore and purge to the owners (`SceneActions`, `CharacterActions`, `RestoreOps`, `PurgeOps` + `PurgeActions`); `TrashActions` over `TrashPorts` holds the prompt rule (restore of `restoreNeedsAuth`, purge of masked rows, silent cancel) and talks to the stores. An authorization lives for one flow (write, dismissed scene choice, or any unlock-state change ends it). Tests: `TrashTest`.
- `export/`: the shared `ExportSheet`. `ExportForm` / `ExportFormRules` (draft reducers, `canCreate`), `ExportRanges`, `ExportRunner` (gate → bytes off the main thread → `fixVoiceIds` after the save), `SaveDialog` (activity result registry without a lifecycle), drafts in `LibraryServices.exports` (passwords blanked when the sheet leaves). The gate and build run in the sheet's scope; only the write after the dialog runs in `appScope`. Tests: `ExportTest`.
- Protection: `ProtectionPorts.keepOpen` / `deviceSecure`, outcome `NO_DEVICE_LOCK`, `LibraryServices.report(outcome)`. `ProjectCreator` keeps hidden projects open and refuses hiding without a device lock (`hidingRefused`), `stackAfter(result)` for the stack after `만들기`.
- `ProjectWriter.purge` (write, then `clearUndo`). `LibraryDocs` takes a row selector (`{ it.trashed }` for the 휴지통).
- `MUnlock` unblur on the open group; `NewWide` frame padded for the keyboard; DS icons replace `LibraryIcons`.

**Conventions kept**: one package per screen group, pure builder + JUnit test, thin composables, copy objects in `LibraryCopyLintTest` (now seven). Feature modules compile to JVM 11 while domain modules are JVM 21: domain `inline reified` helpers (`ExportSheet.row<T>()`) cannot be called from this module; use `rows.filterIsInstance<T>()`.

**Unfinished threads**
1. `PDF 만들기` stays off: LIB-43 adds a `PdfDocument` writer in `ExportRunner.build`'s PDF branch (the plan comes from `ExportPlanner`; fixation does not apply to PDF).
2. The 휴지통 has no entry until LIB-63 (설정 row, §6 Q27).
3. The reverse `MUnlock` blur on re-lock is not built (the group leaves composition at once).
4. `preview.webp` is not passed to `ProjectFileExport` (no cover renderer).
5. Requests open: core:store write without undo (fixation, purge).

**Hazards**
- Restore and purge of a whole project call `ProjectSessions.closeIdle()` first (15 s cache of a closed store). Any new path that reopens a just-trashed project must too.
- `ProjectWriter.purge` drops the project's undo history.

## batch 3

Read the Architecture section of docs/status/feature-library.md first; it now has `settings/`, `openfile/`, the full `projectsettings/` and the 서재 card view and recents under `shelf/`.

**What was added**
- `settings/`: 설정 (LIB-63), 설정 · 숨기기 (LIB-64), 글자 크기 (LIB-65). One `SettingsViewModel`, pure `SettingsState` and `AppLockRules`, shared `SettingsFrame` / rows in `SettingsParts`. Rows of later batches (LIB-57, 58, 62, 66, 67, 68, help) already push their routes. Tests: `SettingsTest`.
- `openfile/`: 파일에서 열기 (LIB-52 … 54, LIB-55 failure state). `OpenFileFlow` (pure stages), `OpenFileViewModel` (uri read, decrypt off main), `ProjectImporter` over `ImportPorts` (`StoreImportPorts`). Tests: `OpenFileTest`, including a round trip from this module's own `.civilight` export, plain and locked (LIB-50/51 evidence).
- `projectsettings/`: every LIB-69 section and row with live values, `routeOf` to the owners' screens, the script switches, `StructureLevelsScreen`. Tests: `ProjectSettingsTest`.
- `shelf/`: card view (`ShelfCards`, `CardShelf.kt`, `CoverArt.kt`), 최근 연 파일 (`RecentsModel`, `RecentsRecorder` run by `LibraryViewModel` over the back stack), selected row in `목록 + 상세`, the 설정 button (`ED`). Tests: `ShelfCardsTest` (also LIB-07 scope).

**Conventions kept**: one package per screen group, pure builder + JUnit test, thin composables, copy objects in `LibraryCopyLintTest` (now nine).

**Unfinished threads**
1. Next batches build the placeholder routes this batch already pushes: `SettingsLayout` (LIB-66), `SettingsMotion` (LIB-67), `SettingsMore` (LIB-68), `SettingsBackup` (LIB-62), `RecoveryPassword` (LIB-58), `HelpReplay` (LIB-21), `RestoreBackup` / `Restoring` (LIB-59, 60), onboarding routes (LIB-15 … 18). `TextSizeControl` and `ThemeSegmented` are ready for `OnbLook`.
2. `ProtectionFacts` is still `NoBackupYet`; LIB-57/58 replace it, and 설정's Drive row then gets `마지막 백업 <time>` (`SettingsState.root(lastBackupAt = …)`).
3. LIB-55 partial import: `OpenFileFlow` maps `PackageOpen.Damaged` to a failure; partial parts already pass `failedNote` / `ImportOutcome.failedMessage` through.
4. LIB-07: story must register `LibrarySearch` with `CrossSearchScope` (Request filed).
5. Card motion (`MCardView`, `MCarousel` parallax, haptic tick) is not built; snap only.

**Hazards**
- `RecentsRecorder` runs only once the 서재 has been composed in this process and while its entry stays on the stack (its view model lives with that entry). After a process-death restore onto a project screen nothing is recorded until the user returns to the 서재.
- A file opened with 파일에서 열기 that lands hidden is not checked against `deviceSecure` (§6 Q19): without a device lock it cannot be opened.
- `StoreImportPorts.releaseProject` resets the stack to 서재 when the overwritten project is open and waits for its cached store to close (up to 2 s) before re-keying it.

## batch 4

Read the Architecture section of docs/status/feature-library.md first; it now has `onboarding/`, `help/`, `tour/`, `notices/` and `common/SessionRelease`.

**What was added**
- `onboarding/`: routes `Onboarding(page)`, `Walkthrough(page)`, `ExampleTour(p)`.
  - `OnboardingFlow` (pure order, targets, and the resume ids the shell's `Startup.startRoutes` reads).
  - `OnboardingActions` (in `LibraryServices.onboarding`).
  - `OnboardingFinisher` over `FinishPorts` (LIB-18 order: state → 서재 → `SessionRelease.await` → `deleteExampleProject`; plus leftover clean-up and the `예시 프로젝트 삭제됨` use-up, both run by the 서재 view model).
  - Screens: `OnboardingScreen` (4 pages), `WalkthroughScreen` + `WalkDemos`, `ExampleTourScreen` + `ExampleTourState`.
  - Platform helpers `DeviceChoices` (mic, widget pin through the app's own provider) and the labelled `DriveSignInStub`.
- `help/`: `HelpReplay` (LIB-21).
- `tour/`: `TourHost` / `TourStepCard` / `TourPlacement` (data-only, meant for core:ui), `ModuleTourHost(module, deps)`, `showFirstUseTip`. The 서재 is wrapped in the dormant `LIBRARY_HIDING` tour.
- `notices/LibraryNotices` (LIB-30 texts).
- `domain:library` `onboarding/` (`TourModule`, `ModuleTours`, `TourRun`, `FirstUseTips`) and `StartBlock.ofProject` (LIB-13 `숨김 · 끔`); tests `OnboardingGuideTest`.
- LIB-14: `NewWide` follows the board (full-width progress, 800 frame, aside stretching, buttons bottom end, read-only name box, tree indent 18).

**Conventions kept**: pure builders with JUnit tests (`OnboardingTest`), thin composables, copy objects in `LibraryCopyLintTest` (now eleven, plus the tour copy). Story content quoted on onboarding pages lives in `OnboardingSample` (not linted: content).

**Unfinished threads**
1. LIB-59 (`백업에서 되살리기` from 환영 and the empty 서재): on success during the first run, call `OnboardingActions.finish()` (marks onboarding done and deletes the installed example).
2. LIB-57: replace `DriveSignInStub`; post `LibraryNotices.backupDone` (needs `POST_NOTIFICATIONS`, Request). LIB-60 posts `restoreDone`.
3. LIB-19: no tour runs yet (every module has `TODO_COPY_LIB19_*` steps). When core:ui hosts `TourHost`, other lanes wrap their screens and mark targets; the 서재 · 숨기기 tour needs copy and anchors (§6 Q15).
4. Requests filed: story `StartBlock.ofProject`, script `OnbExampleScript` (banner + `ExampleTourContract.FINISH_REQUEST`), core:ui tour host and tip presenter, DS `TourCard` `이전`, app `POST_NOTIFICATIONS`, core:model example one-liner.
5. Carried over: LIB-43 PDF, reverse `MUnlock` blur, `preview.webp`, card motion, `ProtectionFacts` swap at LIB-57/58, core:store write without undo.

**Hazards**
- `온보딩 끝내기` resets the stack to 서재 before deleting the example; other areas must never delete it themselves.
- The resume page is written only while `firstRunCompleted` is false; the walkthrough replay from 도움말 writes nothing.
- `TourHost` targets are root coordinates captured on layout; a target scrolled off screen keeps its last hole.

## batch 5

Read the Architecture section of docs/status/feature-library.md first; it now has `backup/`, `export/pdf/`, `export/MultiExport*` and `openfile/RecorderFiles`.

**What was added**
- `backup/` (LIB-57 … 60): `BackupBook` in `files/app/backup.json` (`BackupBookStore` over core:store `JsonFile`; port `BackupBooks`), `BackupFacts` (the real `ProtectionFacts`), `DriveGateway` + the labelled stub `UnavailableDrive` (nothing uploads in this build), `BackupRunner` + `StoreBackupSources`, `BackupSchedule` (start-up hook `library.backup`), `RecoveryKeyring` + `AndroidKeystoreWrap`, `RestoreModel` / `RestoreRunner` / `RestoreJob`, screens `BackupSettingsScreen` (`SettingsBackup`), `RecoveryPasswordScreen`, `RestoreBackupScreen`, `RestoringScreen`. Tests: `BackupTest` (in-memory Drive; full backup → new-device restore round trip).
- `export/pdf/` (LIB-43): `ScriptPdf` (blocks from the `ExportPlan`), `PdfLayout` (pure pagination over `TextMeasure`), `AndroidPdfWriter` (`PdfDocument`); `ExportRunner.build(…, pdf, coverRating, authenticate)`; `PDF 암호` = `domain:library` `export/PdfEncryption` (AES-256 R6, opened by PDFKit in a manual check). Tests: `PdfExportTest`, domain `PdfEncryptionTest`.
- `export/MultiExport` + `MultiExportSheet` + `FolderDialog` (LIB-72): the 서재 header `내보내기` button, checklist, per-file `암호`, folder picker. Tests: `MultiExportTest`.
- `openfile/RecorderFiles` (LIB-48): `.civilight-recorder` open, password, merge. Tests: `RecorderFileTest`.
- LIB-42 / 44 / 46 / 55 / 36 checks: `ExportRulesBatch5Test`, `MultiExportTest`.
- `SettingsFrame(bottomButton, sheets, localSheets)`; `LibraryServices.backups`, `attach(context)`, `writtenBy()`; `ShelfUi.exportable`; the LIB-24 `지우기 ›` row works.

**Conventions kept**: pure builders with JUnit tests, thin composables, copy objects in `LibraryCopyLintTest` (now 14: + `BackupCopy`, `RecoveryCopy`, `RestoreCopy`). Passwords: `remember` only (never saveable), `CharArray` zeroed after use, PBKDF2 off the main thread.

**Unfinished threads (batch 6: LIB-61 62 66 67 68 70 71, CNV-18 19 20, WLD-67)**
1. LIB-61: the failure banner = `domain:library` `BackupFailureBanner.of(book.state, now, zone)` at the top of `BackupSettingsScreen` (`다시 로그인` = `backups.connect()`, `지금 다시 시도` = `BackupRunner.run` now); the Drive row then reads `마지막 성공 …` from the banner.
2. LIB-62: add `기기 저장 공간` to `BackupSettingsState` / the screen (the other rows exist).
3. LIB-66 … 68: `SettingsLayout`, `SettingsMotion`, `SettingsMore` are still shell placeholders; requests waiting there: feature:story STO-42 rows in `SettingsMore`, feature:canvas `기본 이미지 · 보기 ›` → `PresetImages`; feature:cast CST-12 `태그` row → `TagSettings(p)` fits the CNV-18 … 20 row map in 프로젝트 설정.
4. Requests filed: core:store backup book, WorkManager + Drive client, app foreground service / notifications / recorder intent filter, PDF fonts, DS open-eye icon, recorder LRC-46 delegation.
5. Carried over: reverse `MUnlock` blur, `preview.webp`, card motion, core:store write-without-undo.

**Hazards**
- `UnavailableDrive` means the switch never turns on and the restore list is empty on a device; everything behind the port is tested in memory only.
- `PdfEncryption` accepts classic-xref PDFs without object streams; a device whose `PdfDocument` writes otherwise gets the save-failed toast for locked PDFs (never a plain file). Check on a device.
- `RestoreJob` lives in the app scope (no foreground service): closing the app process stops it.
- `BackupBookStore` shares `files/app/` with `AppStateStore` (different file names).

## batch 6 (last batch of this lane)

Read the Architecture section of docs/status/feature-library.md first: `settings/` (batch 6 screens, `SettingsRoutes`), `backup/` (LIB-61, LIB-62), `export/CoverPreview`, `shelf/` (motion) and `projectsettings/` (CNV-20) are updated there.

**What was added**
- `settings/`: `LayoutSettingsScreen` (LIB-66), `MotionSettingsScreen` (LIB-67), `MoreSettingsScreen` + local `MoreValueSheet` (LIB-68 / CNV-19 / WLD-67), pure `DisplaySettingsState` (rows, picker options, one `choose` reducer), `SettingsRoutes.routeOf(SettingsRootRow)` (CNV-18 row map, used by `SettingsScreen`), copy `LayoutSettingsCopy`, `MotionSettingsCopy`, `MoreSettingsCopy` (+ unlinted `MotionPreviewSample`). `SettingsViewModel` gained the three flows and setters.
- `backup/`: `BackupSettingsUi.banner` + `storage`; `DeviceStorage` / `StoreDeviceStorage` / `StorageFacts`; `BackupServices.runNow`, `running`, one run `Mutex`; `BackupSettingsViewModel.relogin` / `retry`.
- `export/CoverPreview` (`preview.webp`): `CoverPreview.source`, `PreviewRenderer`, `AndroidCoverPreview`; passed to `ExportRunner.build`, `MultiExport.build`, `BackupRunner`.
- `projectsettings/`: `ProjectSettingsTarget.TAGS` → cast `TagSettings(p)`; `ProjectLinkRow.sub` / `value` nullable.
- `newproject/ProjectCreator.build(…, notation)`: `때 표기 기본` for new projects.
- `shelf/`: reverse `MUnlock` (armed by the header tap, `relocking` in `LibraryScreen`), `MCarousel` tick and parallax (`ShelfCards.parallaxShift`, `parallaxScale`).
- Export sheet password fields use the shared `PasswordInput` (eye button). 휴지통 empty state uses the DS `EmptyBlock`.
- Tests: `SettingsBatch6Test` 10, `CoverPreviewTest` 2 (incl. a read-back through `OpenFileFlow`), `ShelfCardsTest` +1; total 158, all passing. `domain:library` unchanged.

**Left for whoever continues (all blocked outside this module)**
- needsExternal / build / app: Drive client + WorkManager, notifications (`POST_NOTIFICATIONS`), restore foreground service, recorder intent filter, PDF fonts.
- core: write-without-undo (core:store), tour host in core:ui, DS `ChoiceCard` leading slot, open-eye and gear icons.
- Copy: every `TODO_COPY_*` listed under Requests.
- Not built: `MCardView` list ↔ card morph, `Licenses` screen (no board or copy), a 서재 entry to pick the project cover.

**Hazards (new)**
- The reverse `MUnlock` keeps the just-locked rows on screen for up to 320 ms. It is armed only by the 서재 header `다시 잠그기` tap (never by a policy re-lock, unhide or trash), and the rows stay under `ProtectWindow`, inert and hidden from accessibility. Keep those guards if it is changed.
- `지금 다시 시도` / `다시 로그인` run in `appScope` under the run `Mutex`; the start-up run waits on the same lock.
