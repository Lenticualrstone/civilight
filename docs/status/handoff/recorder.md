# feature:recorder handoff

## batch 1

Read docs/status/feature-recorder.md first; its Architecture section is the map.

The core is the process-wide CaptureHub (capture/CaptureHub.kt), obtained with CaptureHubs.of(deps):
- It sequences the domain:recorder reducers (Recording, TextMemo, LinkPicker, RecordHiding).
- It writes through two ports (capture/CapturePorts.kt): RecorderPort (over RecorderStore) and ProjectPort (over ProjectSessions).
- Tests drive it with the fakes in src/test/.../Fakes.kt (HubFixture, FakeEngine, FakeRecorder, FakeProjects, FakeEnv).
- Screens draw pure views from CaptureViews and keep composables thin.
- RecordingRoute.kt is the only registered route.
- TextMemoSheet (shared) and RecorderLinkPickerSheet are registered in RecorderEntries.kt.
- Copy comes from RecorderUiCopy or domain RecorderCopy. RecorderCopyLintTest checks both.

Not done:
- No audio is captured and no microphone is opened.
- Speech is the needsExternal stub: SpeechEngines.factory returns UnavailableSpeechEngine. Swap the factory to add a real engine; nothing else changes.
- For LRC-14, pass audio to Recording.stopAndSave inside CaptureHub.stopAndSave (RecorderStore.importAudio). The take path is files/recorder/takes/<recordKey>.m4a.
- For LRC-09, session.json is written on every change and at least every 5 s while recording, but nothing reads it at start-up. CaptureHub.focusLost() exists and nothing calls it yet. There is no notification or foreground service.

Route contract (EntryRoutes):
- Recording(surface, projectKey, targetKind, targetKey, caretBlockKey, startTyped).
- surface: any EntrySurface name in any case, or an alias (widget, tile, lock, cover, home, cast, profile, structure, world, checkCenter, canvas, settings). With the default commandBar the surface is inferred from the other arguments.
- targetKind: project, scene/episode/chapter/unit, character, place, term, faction or page, in any case.
- The inbox batch should push Recording(surface = "inbox", projectKey = filterProject, startTyped = ...) for 말로 기록 and 글로 쓰기.

Next batches:
- Add inbox/, record/ and settings/ packages, each with a view model and pure row builders, and register RecorderInbox, RecorderRecordDetail and RecorderSettings in RecorderEntries.kt.
- Already built ahead, do not rebuild: LRC-16 switches, LRC-22 picker masking, LRC-34 여기 넣기 with undo, LRC-42 숨기기 row, the LRC-10 glow modes.
- The shared memo sheet uses CaptureHub.showMemo and memoSheetGone, so a memo dismissed by the host opens fresh for the next entry.

## batch 2

Read the Architecture section of docs/status/feature-recorder.md; it now covers `record/`, `inbox/`, `put/` and `outside/`.

Core:
- `CaptureHubs.services(deps)` gives the process-wide `RecorderServices`: one `EnvSource` (one document cache), the ports, the action context, the hub and `RecordWriter`.
- All writes on saved records go through `RecordWriter` (`record/RecordWriter.kt`). Add methods there for new actions; never write the store from a composable.
- `RecordWriter.projectDocs` emits every project document it wrote. Any screen that judges placements must patch its env with `withDoc`; otherwise a fresh placement reads `정리 안 됨` until a reload. `InboxController` does this already.

Inbox:
- `InboxController` is a plain class tested on the JVM with `InboxFixture` (in `InboxTest.kt`), over `FakeRecorder` (now with a library flow and recorder trash rows) and `FakeEnv` (records `forget`).
- Screens are thin.
- Menu options of later batches are disabled in `InboxController.enabled`, and the row tags in `RowActions`. Turn them on there.

넣기:
- `PutSheet(recordKey, kind, projectKey?)` is the one sheet key. `PutFlows` holds the project step: initial project, `Destinations.choose`, prompt, `authResult`.
- For LRC-35 … 38, add a content per `PutKind` and reuse the step.
- Memo field and character need `RecorderActions` entries (requested from domain:actions).

Outside (needsExternal, nothing run on a device):
- The widget, tile and microphone foreground service are real and declared in this module's manifest.
- The stubs are the isolated "앱을 열지 않고" activity and the show-when-locked recording activity.
- `recorder.resume` recovers a cut-off take at start-up.

Not done:
- the record screen (LRC-28)
- settings (LRC-44)
- audio and player (LRC-14)
- the other 넣기 (LRC-35 … 38)
- deleted-target chip cards (LRC-23)
- target lines (LRC-24)
- unfolded layouts (LRC-31)
- the export hooks

Batch 2 also changed these:
- `CaptureHub.withExample` fills in a script entry's scene from its `caretBlockKey`.
- The 넣기 sheet keeps its step in `rememberSaveable` and closes only after a successful insert.
- Disabled tags are drawn at 38 % alpha, because DS `FilterChip` does not dim itself.

## batch 3

Read the Architecture section of docs/status/feature-recorder.md; it now covers `record/` (the record screen), `audio/` and the card / page 넣기.

Build:
- Run `tools/gw … --no-configuration-cache`. Without it, gw twice reused another lane's configuration-cache entry and ran its tasks under a recorder command, EXIT included.
- Check the log for `Task :feature:recorder:testDebugUnitTest` and fresh XML times.

Record screen (LRC-28):
- `RecordController` is plain and JVM-tested (`RecordFixture` in `RecordScreenTest.kt`); `RecordViewModel` is thin; `RecordDetailScreen` is registered for `RecorderRecordDetail`.
- Edits autosave after 600 ms. A started write always finishes. `flush()` runs from the app scope on pause and when the entry is cleared.
- Deleted-target cards go `DeletedTargetSheet` → `RecordController.onCard` → `CardOutcome`. The picker key `RecordLinkSheet` carries add / replace / relink (`RecordLinkPicks`).
- LRC-39 marks already show on this screen.

Audio (LRC-14):
- `MediaRecorderTakes` keeps one ADTS AAC take per record. `CaptureHub.keptAudio` imports it into the blob store when `원본 음성 보관` is on.
- `RecordPlayer` (one per process, in `RecorderServices`) plays it.
- Tests use `FakeTakes` and `FakePlayback` (`Fakes.kt`). `HubFixture` takes `takes`, `online` and `engines`.

Speech (LRC-11/12):
- `EngineChoice` picks cloud only when it is set, online and available. Both engines are labelled stubs.
- A real on-device engine must take the app's microphone audio, since `MediaRecorderTakes` holds the mic.

넣기:
- `PutCardContent` and `PutPageContent` reuse `rememberProjectStep` / `PutTitle`.
- For LRC-37/38: add the `PutKind` branches in `PutSheetContent`, add the kinds to `PutAvailability.BUILT`, and enable them in `RecordMenus.enabled`. They need `RecorderActions.putInMemoField` / `makeCharacter` (requested).

LRC-24: `domain:recorder` `hooks/TargetLines.kt` (additive) gives the line from one open project. The script and cast lanes draw it (Requests).

LRC-31:
- The inbox and record screen sit in `CenterColumn`. The open row is marked, and the newest row is auto-opened once in two panes (`InboxSelection`).
- Recording in the right pane needs the shell (Requests).

Left for batch 4:
- LRC-37, 38 and 39 (the copy side).
- LRC-40/41 blur reveal.
- LRC-44 settings: it also gives LRC-12 its radio and LRC-14 its switch.
- LRC-45 … 47 hooks.

## batch 4 (last batch of this lane)

Read the Architecture section of docs/status/feature-recorder.md; it now covers `settings/`, `files/`, `inbox/RevealMotion.kt`, the memo-field and character sheets and `record/PendingRecorderActions.kt`.

Build:
- `tools/gw :feature:recorder:compileDebugKotlin :feature:recorder:testDebugUnitTest --no-configuration-cache` exits 0, 134 tests. Check the log for `Task :feature:recorder:testDebugUnitTest` and fresh XML times.
- `domain:recorder` was not changed in batch 4.

넣기 (LRC-37, 38):
- `PutKind.MEMO_FIELD` and `CHARACTER` run: `PutMemoFieldContent` (project → structure tree from `core:ui` `PickerModels.units`; a masked hidden scene asks for the items prompt) and `PutCharacterContent` (`이름` required).
- Writes go through `record/PendingRecorderActions.kt`, local stand-ins for the requested `RecorderActions.putInMemoField` / `makeCharacter` (same signatures). When `domain:actions` has them, switch the two calls in `RecordWriter` and delete the file.

After inserting (LRC-39): nothing new in code; feature tests cover copy trashed / restored / purged and undo. `@` runs in the editable text now mirror their chips: a dotted line in the trash, `◇` for a deleted card (`AtMarks` `dotted` / `diamond`).

Hiding (LRC-40 … 43):
- `inbox/RevealMotion.kt`: blur 7 → 0 dp, opacity 0.55 → 1, 320 ms on the hidden rows' text; fade below API 31; 120 ms fade with reduced motion. `InboxScreen` hoists one `Animatable`; `다시 잠그기` animates back, then calls `relockNow()`.
- The player stops when its record is trashed or gone (`RecordPlayer.follow` / `playable`, wired in `CaptureHubs`).
- The reveal plays only after this inbox showed the locked row; a group already open on arrival is clear.

Settings (LRC-44): `settings/` registered for `RecorderSettings`. Rows come from `domain:recorder` `RecorderSettingsScreen.rows` with `RecorderSettingId`; writes go through `deps.stores.settings.update`. The hub reads settings per take (tested after the hub exists).

Files (LRC-45, 46):
- LRC-45 is checked against `domain:library` `ExportSheets` / `ProjectPackageWriter` (tests only; the library lane draws the row).
- `files/RecorderLibraryFiles.kt` writes and restores `.civilight-recorder`: encrypted with the recovery key when it carries hidden records; restore merges by key and stores audio. Both load the library first.
- Nothing calls it yet. The LIB-57 backup is not built and cannot depend on this module (Request: move it to a shared module or add a registry).

Hooks (LRC-47): an audit of every hook, its caller and its state is in the status file's Partial section. The only hook still missing a caller is the cast profile line (CST-49).

Still open for whoever continues: the device run (nothing was seen on an emulator), a real speech engine, the requests in the status file (copy ids, `RecorderActions` additions, the toast's second action, the right-pane recording, glow modes, the backup registry).
