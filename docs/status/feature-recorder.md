# feature:recorder

Batch 1 of 4 done: LRC-01, 06, 07, 08, 11, 13, 15, 17, 18, 19, 20, 21 (the capture: mic, permission, `녹음 중`, `정리 중`, `글 메모`, links and the link picker).

**Build and tests.** `tools/gw :feature:recorder:compileDebugKotlin :feature:recorder:testDebugUnitTest` exits 0: 39 JVM tests, all passing (`CaptureHubTest` 16, `MemoHubTest` 10, `LinksAndRoutesTest` 6, `MemoEditingTest` 4, `RecorderCopyLintTest` 3). `domain:recorder` was not changed. Nothing was run on a device or emulator, and no APK was built.

## Architecture

Package `io.github.lenticualrstone.civilight.feature.recorder`.

| Package | Files | What |
| --- | --- | --- |
| (root) | `RecorderEntries.kt` | `recorderEntries(deps)`: registers the route `Recording`, the shared sheet `TextMemoSheet` and the feature sheet `RecorderLinkPickerSheet`. `RecorderInbox`, `RecorderRecordDetail` and `RecorderSettings` still show the shell placeholder. |
| (root) | `RecorderUiCopy.kt`, `RecorderSheets.kt` | Feature copy that `domain:recorder` `RecorderCopy` does not hold. `RecorderLinkPickerSheet` is the picker stacked over the shared memo. |
| `capture` | `CaptureHub.kt` | **The one capture of the process.** It holds the recording, `정리 중`, the memo and the picker request, and calls the `domain:recorder` reducers (`Recording`, `TextMemo`, `LinkPicker`, `RecordHiding`). It runs the timer and the speech engine, writes the library, and writes `여기 넣기` through `domain:actions` `RecorderActions`. It lives as long as the process, not a screen, because back only minimises a recording (LRC-07) and a second mic tap reopens it (LRC-01). |
| `capture` | `CaptureHubs.kt` | `CaptureHubs.of(deps)`: the process-wide hub, built on first use from a screen or sheet, never from the entries function. |
| `capture` | `CapturePorts.kt` | Ports the hub writes through: `RecorderPort` (core:store `RecorderStore`: library, `session.json`, `draft.json`) and `ProjectPort` (one undo step in a project through `ProjectSessions`). Tests use fakes (`src/test/.../Fakes.kt`). |
| `capture` | `CaptureState.kt` | `CaptureStage` (`IDLE · MIC_REASON · MIC_OFF · RECORDING · PROCESSING · TYPING`), `CaptureState`, `PickerRequest` (add, or replace a chip's links), `CancelOutcome`, `PutHereOutcome`. |
| `capture` | `CaptureViews.kt` | Pure view builders: `recording` (domain `RecordingScreen` + no-engine line), `processing` (`여기 넣기` only where it can run), `memo` (sheet view + open `@` candidates + `@` marks). |
| `capture` | `RecordingRoute.kt` | The `Recording` overlay route. It draws `녹음 중`, `정리 중`, `마이크 꺼짐`, the reason sheet, the memo sheet and the picker sheet from the hub state. It asks for the permission, handles back and closes when the hub goes idle. |
| `capture` | `CapturePanes.kt` | Thin composables: `CaptureFrame` (glass + DS `RecorderGlowBand` + 560 dp column), `RecordingPane`, `ProcessingPane`, `MicOffPane`, `MicReasonContent`. |
| `capture` | `EntryRoutes.kt` | Turns a `Recording` route into the model `EntryContext` (보던 곳, LRC-19), and back (`routeOf`). The parser accepts several spellings; see the route contract below. |
| `capture` | `CaptureProtection.kt` | Window protection while a capture is open: the strongest protection among the entry project and every linked project (rule 19 §3, LIB-26). |
| `memo` | `TextMemoSheet.kt` | The memo sheet content (`LrText`) and `MemoField`: plain-text editing over runs, with a leading `@` and a 2 dp `acc` underline on link runs through a `VisualTransformation`. |
| `memo` | `MemoEditing.kt` | `MemoEditing` (one `Runs.replace` per field change, Hangul-safe) and `AtMarks` (offset mapping for the shown `@`). |
| `memo` | `SharedTextMemo.kt` | The shared `TextMemoSheet` (other lanes open it over their own screens) and `SharedLinkPicker` (stacked picker). |
| `link` | `LinkChipRow.kt` | Chips of the capture screens (`연결 · …` on the first one, ×, `@` mark, dotted for trashed targets, `+ 연결`). |
| `link` | `LinkPickerRows.kt`, `LinkPickerSheet.kt` | Picker rows from `LinkPicker.search`: `글에 나온 이름` group first, then the ranked rest. Linked rows are checked. Search by name · 초성 · alias. |
| `speech` | `SpeechEngine.kt` | `SpeechEngine` / `SpeechListener` (partial, final, level, finished). `SpeechEngines.factory` currently gives `UnavailableSpeechEngine`, the **needsExternal stub** (LRC-11/12). |
| `env` | `RecorderEnvSource.kt` | `EnvSource`: builds the domain `RecorderEnv` (every 서재 row with its document, unlock state, most recently opened project). `quick()` uses only open documents and does no I/O; `load()` reads closed ones and caches them by row time. |

**Route contract for other lanes** (core:navigation `Recording`):
- `surface`: any `EntrySurface` name in any case (`script`, `SCRIPT`, `projectHome`, `project_home`). Also accepted: `widget`, `tile`, `lock`, `cover`, `home`, `cast`, `profile`, `structure`, `world`, `checkCenter`, `canvas`, `settings`.
- With the default `commandBar` (or an unknown value), the surface is inferred from the other arguments:
  - `caretBlockKey`, or a unit target → script
  - character, place, term, faction or page target → that card
  - only a `projectKey` → a screen inside the project (project link)
  - no project → 서재 (no pre-fill)
- `targetKind`: `project`, `scene` / `episode` / `chapter` / `unit`, `character`, `place`, `term`, `faction`, `page`, in any case.
- **Script lane:** push `Recording(surface = "script", projectKey, targetKind = "scene", targetKey = sceneKey, caretBlockKey = caretBlock)`. Without `caretBlockKey`, `정리 중` has no `여기 넣기 · #044 뒤`.
- `startTyped = true` opens the memo with the same entry context.
- `TextMemoSheet(projectKey)` opens the memo as a sheet over the caller's screen. It can pre-fill only the project (see Requests).

**How to extend (later batches):**
- **Inbox, record screen, settings:** add `inbox/`, `record/` and `settings/` packages, each with a view model and pure row builders, and register their entries in `RecorderEntries.kt`. Record writes go through `RecorderPort.update` (or the `RecorderStore` directly) with the domain reducers. Copy goes in `RecorderUiCopy`, or comes from `RecorderCopy`.
- **Starting a capture from the inbox (`말로 기록` / `글로 쓰기`):** push `Recording(surface = "inbox", projectKey = <filter project>, startTyped = …)`.
- **Real engine (LRC-11/12):** set `SpeechEngines.factory` (for example in a recorder-owned initializer) to an engine that never ends on silence and reports `onLevel`. Nothing else changes.
- **Audio (LRC-14):** pass `audio` to `Recording.stopAndSave` in `CaptureHub.stopAndSave` (import the take via `RecorderStore.importAudio`). `audioTempPath` already names `files/recorder/takes/<recordKey>.m4a`.
- **Crash recovery (LRC-09):** read `RecorderStore` `session.json` at start-up and resume into `정리 중`. The hub already writes the session at every change, at least every 5 s.
- **Notification (LRC-09):** a foreground service that observes `CaptureHubs.of(deps).state`.

## Done

Pure logic with passing tests (the rules themselves are `domain:recorder`, tested there):
- **Capture sequencing** (`CaptureHub`):
  - **LRC-01:** a mic tap starts at once; a second tap reopens the running recording; a memo opened from another screen never drops a minimised recording. Tests: `LRC_01_aSecondMicTapShowsTheRunningRecording`, `LRC_01_aMemoOpenedElsewhereNeverDropsAMinimisedRecording`.
  - **LRC-06:** reason line → system prompt → `마이크 꺼짐`. Test: `LRC_06_reasonFirstThenSystemPromptThenMicOff`.
  - **LRC-07 / LRC-08:** paused time not counted; no auto-stop; the record is written at `멈추고 저장`; `취소` with undo; `취소` during a continued take drops only the new take. Tests: `LRC_T47_noAutoStop`, `pausedTimeIsNotCountedAndTheTailTurnsFinalAtTheStop`, `cancelFirstTakeDiscardsAndUndoBringsItBackIntoProcessing`, `cancelDuringAContinuedTakeDropsOnlyTheNewTake`, `theSessionIsWrittenForCrashRecovery`.
  - **LRC-11 (no engine):** the recording runs and saves empty text. Test: `LRC_11_withoutAnEngineTheRecordingRunsAndSavesEmptyText`.
  - **LRC-13:** `이어서 말하기` continues the same record; a late final result refines the tail. Tests: `LRC_T02_rule23Example`, `LRC_T48_resumeContinuesTheSameRecord`, `aLateFinalResultRefinesTheTailWhileFinalising`.
  - **`여기 넣기` and `숨기기`** (shown on `정리 중`; LRC-34 and LRC-42 are later batches). Tests: `LRC_34_putHereInsertsAfterTheCaretBlockAndUndoRemovesIt`, `putHereIsNotOfferedInAReadOnlyProjectNorFromAnotherEntry`, `LRC_42_hideRowIsOnForAnR18TargetAndHidesTheRecord`.
- **Memo** (LRC-15, 16, 17). Tests: `LRC_T04_atChipLine`, `LRC_T06_crossProjectAtImpliesThatProject`, `LRC_15_saveIsOffForBlankTextAndADraftIsKeptOnClose`, `LRC_16_toTextKeepsTheRecordingAsAVoiceRecord`, `LRC_16_toVoiceStartsWithTheTypedTextAfterThePermission`, `aMemoInAnotherScreensSheetHandsOverToTheRecordingRoute`. Shared-sheet lifecycle: `LRC_19_aMemoDismissedByItsHostOpensFreshForTheNextEntry`, `LRC_15_aDismissedMemoWithTextComesBackAsItsDraft`, `LRC_20_theStackedPickerKeepsTheMemo`, `LRC_19_aMemoInsideTheExampleProjectIsMarkedAsExample`. Field editing: `MemoEditingTest` (4).
- **Links** (LRC-18 … 21):
  - Entry pre-fill. Test: `LRC_T01_prefilledLinkByEntry`.
  - Route parsing. Tests: `theRouteParserInfersTheSurfaceFromTheDefaultCommandBar`, `theRouteRoundTripsAnEntry`.
  - Chip × and replace. Test: `LRC_18_chipRemovalAndReplaceOnTheCaptureScreens`.
  - Picker rows and the `글에 나온 이름` group. Test: `LRC_T07_fixture_pickerRowsStartWithNamesInTheText`.
  - Masked project rows. Test: `LRC_22_maskedProjectRowsPrintTheRating`.
  - Environment order. Test: `theEnvListsLiveProjectsByLastEditThenTrashedOnes`.
  - LRC-T05 (search order) and LRC-T08 (locked hidden targets) are tested in `domain:recorder`; the picker shows that order unchanged.
- **Copy:** `RecorderCopyLintTest` runs DS `CopyLint` over `RecorderUiCopy` and `RecorderCopy`, and checks that missing copy stays a `TODO_COPY_LRC_nn` id.

## Partial

Every UI feature below is built and wired to the store, but nobody has seen it on a device yet.
- **LRC-01:** the `Recording` route and the hub are done. The cast, world, library and story lanes push `Recording` from their command bars. The script, map and canvas lanes do not yet (see the route contract).
- **LRC-06:** reason sheet, system prompt, `마이크 꺼짐`, `시스템 설정 열기` (app details) and `글로 쓰기` are built. Coming back from settings with the permission granted closes the screen without starting. `RECORD_AUDIO` is declared in this module's manifest.
- **LRC-07:**
  - Built:
    - header (live mark, `라이프 레코더`, tabular timer, `취소`)
    - chips
    - transcript 25/40 (`ink` confirmed, `ink3` tail, 2×26 `acc` caret; scrolls to the end)
    - `글로 쓰기` · `멈추고 저장` · pause
    - the `취소` undo toast
    - back minimises
    - 560 dp column on inner screens
  - Left:
    - **No audio is captured.** No microphone is opened while the engine is the stub: the timer runs and the record is a voice record with empty text and no audio (`record.audio = null`, so `정리 중` shows the `TODO_COPY_LRC_06` note).
    - The glow level stays 0, so the bands sit at 0.72 × ref.
    - A recording left with back has no visible indicator other than the mic reopening it (notification is LRC-09).
    - The `TwoLr` right-pane placement belongs to the inbox batch.
- **LRC-08:** the hub never stops on silence (tested). The real engine must run segmented sessions; that is not built (stub).
- **LRC-11:** `SpeechEngine` seam plus the `UnavailableSpeechEngine` stub (**needsExternal**). The no-engine line `TODO_COPY_LRC_05` shows. A real on-device `SpeechRecognizer` engine is left.
- **LRC-13:**
  - Built:
    - header with duration
    - `받아쓰는 중 · 문장 나누기` while finalising, with the sweep glow
    - final text
    - chips
    - `숨기기` row
    - note
    - `여기 넣기 · #044 뒤` (only from the script, with a writable project and non-blank text) with its toast and undo
    - `이어서 말하기` · `기록함에 두기`
    - back = `기록함에 두기`
  - Left: playback and audio (LRC-14).
- **LRC-15:**
  - Built:
    - the sheet (`라이프 레코더 · 글` · `닫기`, field 18/30 on `tint`, chips, `숨기기` row, mic, hint, `저장` disabled on blank)
    - draft kept on close or scrim, saved while typing
    - the shared `TextMemoSheet`
  - On inner screens the memo is the DS centred sheet.
- **LRC-17:** typing `@` lists up to 6 candidates under the field. Picking one makes a link run shown as `@이름`, underlined, with the chip `이름 @` (no ×). Deleting the run removes the link.
- **LRC-18:** links are added, removed, replaced and de-duplicated through the domain. Masked chips of one project collapse, and × removes all their links.
- **LRC-19:** the pre-fill follows the setting `보던 곳 연결` and the entry table. Example-project chip: the domain chip text is used.
- **LRC-20:**
  - Built:
    - the picker sheet (title `TODO_COPY_LRC_07`, search, `글에 나온 이름`, ranked rest, linked rows checked)
    - add, and replace by chip tap
    - stacked over the shared memo
  - Left: the lock-screen unlock request (LRC-05).
- **LRC-21:** capture-screen chips are done. Inbox tags and the locked-row tags come with the inbox batch.
- **Built ahead of their batch** (keep them; finish the rest there):
  - LRC-16: both switches, tested.
  - LRC-22: masked project rows in the picker and collapsed masked chips.
  - LRC-34: `여기 넣기` with toast and undo, tested.
  - LRC-42: the `숨기기` row on `정리 중` and on the memo, tested.
  - LRC-09:
    - `focusLost()` pauses (nothing calls it yet; no audio focus is requested)
    - the session is written for crash recovery
    - back never discards
  - LRC-10: the DS `RecorderGlowBand` is drawn with recording, still and sweep modes, the `recorderGlow` setting and reduced motion. The level input is always 0 (no audio).
- **Not started in this module:** LRC-02 … 05, LRC-12, LRC-14, LRC-23 … 33, LRC-35 … 41, LRC-43 … 47, the rest of LRC-09 (notification, foreground service, recovery at start-up), and the rest of LRC-10. `RecorderInbox`, `RecorderRecordDetail` and `RecorderSettings` show the placeholder. Nothing reads `session.json` at start-up yet.

## Defaults used

- **Mic reason sheet** (LRC-06 `[PROPOSED]`): the `OnbVoice` row (`마이크 허용` · `녹음할 때만 사용`) as a small sheet titled `라이프 레코더`.
- **`취소`:**
  - On a first take it discards the take and shows the toast `TODO_COPY_LRC_03` · `되돌리기`. Undo writes the record and reopens `Recording` in `정리 중`.
  - On a continued take it drops only the new take and returns to `정리 중`.
- **Back:**
  - on `녹음 중`: leaves the screen; the recording keeps running
  - on `정리 중`: `기록함에 두기`
  - on the memo: `닫기` (the draft is kept)
  - on `마이크 꺼짐`: `닫기`
- **Restored back stack:** a `Recording` route restored after process death never starts the mic by itself. The start is consumed once with `rememberSaveable`, and the route closes when nothing is running.
- **`여기 넣기` hidden** (`[PROPOSED]` in LRC-33) when:
  - the entry project is read-only (the example) or in the trash
  - the text is blank
  - the engine is still finalising
- **Picker:**
  - Rows already linked are checked and cannot be chosen again.
  - Tapping a collapsed masked chip replaces all its links with the chosen target.
- **Memo `@` list:** at most 6 candidates, drawn as rows under the field so the keyboard stays up.
- **Persistence:**
  - The session is written 300 ms after a change and at least every 5 s while recording.
  - The memo draft is written 300 ms after a change.
- **Window protection:**
  - Capture screens take the strongest protection among the entry project and every linked project.
  - The picker takes that of every project it lists.
- **The shared memo's entry context** is `OTHER_IN_PROJECT` with the sheet's project, or 서재 without one.
- **A memo opened while a recording runs** closes and shows the running recording instead (one capture at a time).
- **Shared memo dismissed by its host** (scrim, drag, back, or the host screen leaving): `CaptureHub.memoSheetGone()` marks the memo detached. The next `showMemo` closes it as `닫기` would: a non-empty draft is kept and returns with its own links, while an empty memo is dropped. The new memo then opens for the new entry. The stacked picker and the switch to voice do not detach the memo.

## Spec conflicts

- **LRC-T07, 대정전:** the fixture makes 대정전 a term, so it is in the `글에 나온 이름` group (see `domain-recorder.md`). The feature test is the `_fixture` twin and expects 하르트 · 대정전 · 세라.
- **`취소` toast duration:** LRC-07 `ED` says 4 s; the shell's toast host is fixed at 6 s (LIB-40), so this toast shows for 6 s.

## Requests

- **Copy needed (TODO_COPY), shown raw on these screens today:**
  - `TODO_COPY_LRC_02`: resume label of the pause button
  - `TODO_COPY_LRC_03`: `취소` toast
  - `TODO_COPY_LRC_05`: no speech engine line
  - `TODO_COPY_LRC_06`: `정리 중` note when no audio is kept (always, until audio capture lands)
  - `TODO_COPY_LRC_07`: link picker title
- **core:navigation, `TextMemoSheet`:** add `targetKind: String? = null, targetKey: Key? = null` (same strings as `Recording`), so a memo opened as a sheet from a card or a scene can pre-fill that card or scene (LRC-19). Until then, lanes that want the card pre-filled push `Recording(…, startTyped = true)` instead.
- **core:navigation, optional:** make `RecorderLinkPickerSheet` a `SharedSheet` (owner RECORDER). It works today as a feature key looked up through the shared registry, and it restores by class name.
- **Script lane:** push `Recording(surface = "script", projectKey, targetKind = "scene", targetKey = sceneKey, caretBlockKey = caretBlockKey)` from the script command bar.
- **Map and canvas lanes:** push `Recording(projectKey = p)`, with `targetKind = "page"` and `targetKey` on a page.
- **app, later (LRC-09):** a start-up hook that lets this module resume a saved `session.json` into `정리 중`. It can also be done from the recorder's own initializer; this will be decided in the LRC-09 batch.
- **FEATURES.md:** LRC-01, 06, 07, 08, 11, 13, 15, 17, 18, 19, 20, 21 → partial (this module may not edit that file).
