# domain:library

Library, protection and file rules of `docs/spec/60-library-security-files.md` and `00-domain-model.md` §6–§8 as pure Kotlin over core:model (package `io.github.lenticualrstone.civilight.domain.library`, depends on core:model only). No I/O, no device clock: every function takes `now` and the zone; crypto and ZIP use only `javax.crypto` and `java.util.zip`.

`tools/gw :domain:library:test`: 183 tests, 183 pass, 0 ignored, 0 failures. Every §4 case id has a test named after it: R19-01…15, S-01…12, N-01…02, L-01…06, RL-01…07, I-01…18, E-01…20, F-01…19, B-01…10, T-01…15, P-01…12.

Sources: `domain/library/src/main/kotlin/io/github/lenticualrstone/civilight/domain/library/<package>/`. Tests: `domain/library/src/test/kotlin/io/github/lenticualrstone/civilight/domain/library/`.

## Done

Domain side done and tested; the screens are drawn by feature:library, so in FEATURES.md terms these stay `partial` until the UI exists, except the pure file-format features (LIB-47…51, 55, 56), which have no screen of their own.

| Feature | What | Files | Tests |
| --- | --- | --- | --- |
| LIB-49 package layout | ZIP with `mimetype` first and stored, all else deflated; manifest with `sha256-` hash of every entry; file units of §7.2 via `ProjectFileSet`; only blobs referenced by an exported row; recorder audio only with `원본 음성 포함`; `preview.webp` never for hidden projects; unknown files written back; features computed from content; unsafe entry names and zip bombs rejected | `format/ZipCodec.kt`, `PackageWriter.kt`, `ProjectPackageWriter.kt`, `Sha256.kt`, `JsonScan.kt` | `FormatTest` F-01, F-08, F-16, `features follow the content`, `without trash no non-live row is written`; `PackageEdgeTest` unsafe containers, referenced blobs |
| LIB-47 `.civilight` export | Latest format; without `휴지통도 포함` only live rows plus `NameTag(notExported)` for trashed things still referenced (§5.6); with it the `trash/` mirror; never fixes voice IDs; encryption with the `암호` | `format/ProjectPackageWriter.kt`, `export/ProjectFileExport.kt` | `ExampleRoundTripTest` (3), E-11, E-12, E-17, `encrypted project file export opens with its password`, `the trash travels only with 휴지통도 포함`, T-12, T-15 |
| LIB-50 versions | `FormatVersion` (`major.minor`, numeric), `UpgradeChain` (one step at a time, minor gaps walked, unknown major refused, input never modified), newer-file check by `minReaderVersion` and `features.required` (features beat the number), unknown optional features skipped | `format/FormatVersion.kt`, `UpgradeChain.kt`, `NewerFile.kt`, `FeatureNames.kt` | F-03, F-04, F-05, F-06; `PackageEdgeTest` versions, chain |
| LIB-51 encryption | Outer ZIP `mimetype` + `encryption.json` + `payload.bin`; AES-256-GCM with the exact header bytes as associated data; PBKDF2-HMAC-SHA256, 600 000 iterations, 16-byte salt, 12-byte nonce; lock types `export` (key from the password) and `recovery` (random data key wrapped by the 복구 암호, `wrappedDataKey` in the header); outer `mimetype` follows the inner package | `crypto/PackageCrypto.kt`, `RecoveryKeys.kt` | F-13, F-14, F-17; `PackageEdgeTest` wrong password, altered header/payload, unknown method, default header, recovery key by password and by data key |
| LIB-52 open from file | Reader gates (zip → mimetype → encryption header → manifest → newer check → hashes → upgrade → decode); same project = same **project.json** key (manifest block corrected to the content); `새 프로젝트로` (new key, `<name> (n)` next free, item keys kept), `덮어쓰기` (old copy re-keyed and trashed `replacedByOverwrite`, 30 days), same key only in the trash re-keyed silently; recorder records merge by key and only gain links; the `RHOpenFile` sheet with `씬 <s> · 인물 <c> · <size> · <date>` | `format/ProjectPackageReader.kt`, `importing/OpenDecision.kt`, `OpenFileSheet.kt`, `ProjectImports.kt`, `ProjectRekey.kt`, `RecorderMerge.kt` | F-09 (2), F-10, F-11, F-12 (2), F-18, F-19, `manifest and project json disagreeing on the key`, `not a package`, `re-keying a project …` |
| LIB-53 newer file | `RHNewerFile` rows `양식 <v>` · `<max>까지` · `필요한 기능` (Korean names), note, buttons; nothing imported | `format/NewerFile.kt` | F-04, F-06 |
| LIB-54 encrypted file | `RHEncryptedFile` sheet, field label by lock type (`암호` + `내보낼 때 정한 암호`, or `복구 암호`); decrypt then the same checks; decrypted imports land hidden | `crypto/EncryptedFile.kt`, `importing/ProjectImports.kt` | F-13, F-14, F-15 |
| LIB-55 damaged file | Per-file hash check; a damaged episode file is left out (`열지 못한 부분 · 1-7`), the block counter survives in `story/index.json`; damaged `manifest.json` / `project.json` / `story/index.json` import nothing | `format/ProjectPackageReader.kt` | F-07 (2) |
| LIB-56 regression file | `src/test/resources/format/1.0/잿빛 새벽의 등불.civilight` (example project, trash and linked records, written by this writer: `ProjectPackageWriter.write(ExampleProject.build(), ExampleRecorder.library(), options = ProjectFileOptions(includeTrash = true), writtenBy = "Civilight 1.0.0 (Android)", writtenAt = ExampleClock.NOW)`; regenerated 2026-11-22 when the fixture's two acknowledgements got real fingerprints, only `checks.json` and its manifest hash changed; regenerated again 2026-11-22 by the domain:actions agent when the two example maps got a real `gen-1` `generation.result`, only `maps/01JZ000000MAPS000000000001.json`, `maps/01JZ000000MAPS000000000002.json` and `manifest.json` changed); every version file opens, has no failed parts, and reads → writes back to identical file units; the current-version file equals `ExampleProject.build()` | test resource | `FormatRegressionTest` (3) |
| LIB-48 `.civilight-recorder` | Library file write/read, hidden records only when locked with the 복구 암호, audio optional, encrypted container recognised | `format/RecorderPackage.kt`, `RecorderSlice.kt` | `PackageEdgeTest` recorder library file round trip |
| LIB-22…25 rating and protections | Badges and glosses; rating defaults table (fixed R-15/ALL values); the `바뀌는 것` sheet for raising (current ∨ target default) and lowering (current, right label `R-15 기본 · 끔`), extra ◇ rows, notes, auth when hiding goes off, nothing auto-released; new-project protections and step-4 switch; project settings rows with `<rating> 기본과 다름`; `이미 있는 … 같이 바꾸기` | `rating/*.kt` | R19-01…15 + 4 more in `RatingSheetTest` |
| LIB-26…30 masking | `projectSurface` per `Surface`, `displayProjectName`, `recorderLinkLabel`, capture guard (masked-only labels do not trigger), recents preview and task label, backup notification text | `masking/*.kt` | S-01…12, N-01…02, `recorder link label`, `card section counts …`, `backup notification counts hidden projects` |
| LIB-31…35, 37 hiding and locks | Hidden access (group unlock, kept-open-until-relock), re-lock policy events, lock screen sequence (app lock and hidden lock separate), no blur snapshot for hidden or capture-blocked projects, cancel → 서재 locked | `masking/ProjectProfile.kt`, `lock/*.kt` | L-01…06, RL-01…07, `events before the unlock do not count` |
| LIB-36 item hiding | `itemSurface` contract for every R19 §6 surface, hidden-speaker-in-visible-scene finding, map pins/regions | `hiding/*.kt` | I-01…18 |
| LIB-38, 39 trash host | Filters (`전체 · 프로젝트 · 씬 · 블록 · 인물 · 세계 · 변화 · 레코더`), rows with computed reference counts, `<d>일 남음` on local calendar days, purge due at `d ≤ 0`, block group key (scene + local day), empty blocks never enter, restore choices for a block of a trashed scene, name-tag labels | `trash/TrashHost.kt` | T-01…15, `hidden items in the trash are masked while locked` |
| LIB-41…46 export decisions | Sheet rows per format and defaults (hidden scenes, file name, PDF password, `암호`, recorder row), auth gate (one prompt for `넣기`), `ExportPlan` (range, written scenes, left-out scenes, speakers with profile depth, field defs, discarded IDs, layers, warnings), CSV row states `새 · 바뀜 · 버림`, voice-ID fixation of exactly the written lines, `ExportRun` orchestration with an `ExportSerializer` the script area implements | `export/*.kt` | E-01…20, `layers row and range label`, `export run …` |
| LIB-57…61 backup | Backup plan (hidden: recovery lock or excluded; hidden items: recovery lock or unencrypted with ◇; R-18 not hidden unencrypted; example/trashed skipped; unchanged reused; a lock change re-uploads), prune after 30 days, restore run row, failure banner (`<n>일째` 1-based), restoring steps, per-project backup file | `backup/*.kt`, `export/ProjectFileExport.kt` | B-01…10 + 4 more in `BackupTest` |
| LIB-58 복구 암호 | Data key creation, password unlock, material for storage (never the password), `정하기` rule | `crypto/RecoveryKeys.kt` | `PackageEdgeTest` recovery key, `복구 암호 정하기 …` |
| LIB-01, 02, 03, 05, 07 서재 | Row meta (`1장 7/10 · 인물 12 · 오늘 08:12`), relative time, listing and hidden group row, card section label, cross-search scope, recents cards, ⋯ sheet rows and sub line, delete dialog (NFC name match) | `newproject/LibraryRows.kt`, `ProjectMenu.kt`, `masking/LibraryViews.kt`, `common/TimeLabels.kt` | P-09…12, S-01…03, `project menu and delete confirm` |
| LIB-09…13, 17, 18 new project and onboarding | Draft, name rule, structure levels and preview tree, protections, `만들기` (units, numbering unit, counters, travel modes, protagonist, record links with origin `startMaterial`), `시작해 볼 것` block, example install read-only, `온보딩 끝내기` purge, empty 서재, read-only guard | `newproject/*.kt`, `importing/ProjectImports.kt` | P-01…08, `step 2 preview trees`, `a 2-level and a 1-level structure`, `summary line …`, `checklist items complete by fact`, `the bundled example installs read-only …` |

## Partial

- **Screens and platform calls are not here.** Sheets, toasts, BiometricPrompt, `FLAG_SECURE`, task labels, SAF save, WorkManager, Drive REST, the Keystore wrap of the backup data key: this module returns data and copy; feature:library and the app call the platform.
- **PDF / CSV / engine JSON bytes** are the script area's (`ExportSerializer`); this module decides what goes in (`ExportPlan`, `CsvRows`) and fixes voice IDs afterwards.
- **Trash operations** (delete, restore, purge, merge) are domain:trash's; `trash/TrashHost.kt` covers the host data only. T-12 and T-15 here check what the file keeps (name tag, fixed IDs, number and voice state of a trashed block); the purge and restore themselves are tested in domain:trash `LibraryTrashTest`.
- **Blobs whose bytes the `BlobReader` cannot supply** are still listed in `assets/index.json` and reported in `ProjectPackageContent.missingBlobs`; the caller decides whether to block the export.
- **Newer-file sheet summary** has size and date but no scene/character counts (the file is not decoded).
- **E-19 resolved time**: the plan carries the scene's stored time (relative to its base, clock `06:10`, precision 정확), place, flashback flag and transition; the resolved `day 2` and band `아침` come from the story area's resolver when the script area serialises the engine JSON.
- **Not started** (no domain logic needed here or UI-only): LIB-04 motion, LIB-06, LIB-08, LIB-14, LIB-15, LIB-16, LIB-19, LIB-20, LIB-21, LIB-63, LIB-64 screen, LIB-65…69, LIB-71, LIB-72 (multi-project export: call `ProjectFileExport.export` once per project).

## Defaults used

- First scene with a blank title stores no title; its shown label is the derived `씬 1` (LIB-12 placeholder `비워 두면 ‘씬 1’`), so the scene label never reads `1-1 씬 1 · 씬 1`.
- Same project = the key in the imported `project.json`; when the manifest's `project` block disagrees, the content wins and the manifest is corrected on read.
- Imported names: `<name> (n)`, smallest n ≥ 2 not used by a live project (trashed names do not block).
- Files locked with an export `암호` land hidden on this device whatever their own flag (LIB-51, LIB-54 note); recovery-locked backups keep their own flag (see Spec conflicts).
- Unknown lock type in a header asks with the `암호` wording; wrong password = field error, retry, no lockout.
- Encryption: PBKDF2-HMAC-SHA256, 600 000 iterations, 16-byte salt, AES-256-GCM, 12-byte nonce, 128-bit tag, `encryption.json` bytes as AAD; recovery data key wrapped with AES-GCM under the password key (`nonce || ciphertext`, AAD `civilight-recovery-key`).
- 복구 암호 minimum 8 characters; changing it makes a new data key (old backups keep the old password, §6 Q24).
- Upgrade chain: a minor version without a step is passed over (minors only add); an unknown major refuses as damaged.
- Damaged-part label: episode files by episode code (`1-7`), other files by path.
- Open-file `<date>` = manifest `writtenAt`; size binary units with one decimal below 10 (`1.2 MB`, `340 MB`).
- Backup: whole project file locked with the 복구 암호 when it has hidden items (§6 Q5); R-18 not hidden unencrypted (§6 Q29); a version locked differently from the plan is uploaded again; retention 30 days by run start time.
- Backup failure `<n>일째` = calendar days since the first failed run + 1; reasons other than login expired use `TODO_COPY_LIB61_*`.
- Trash: `<d>일 남음` on calendar days in the device zone; purge due at `d ≤ 0`; rows newest first; `전체 <n>` only on the first chip.
- CSV `상태`: unchanged exported line empty; transition rows carry no state; `바뀜` = text hash or speaker differs from `VoiceState.lastExport`.
- Voice IDs in hidden scenes left out by `빼기` are not fixed (§6 Q20).
- Export file name extension appended unless typed; MIME types per LIB-41 `ED`.
- `.civilight-recorder` hidden records only in a recovery-locked file (LRC Q16 default); the encrypted container keeps the recorder `mimetype`.
- Example project: installed from the current-version file with `isExample = readOnly = true`, ⋯ menu only `열기`; the flag is never in a file.
- Delete confirm: trimmed, NFC-equal, inner spaces must match. Rename: trimmed NFC, blank rejected.
- No `voice.json` (C-17: voice state lives on blocks, discarded IDs in episode files).
- `TODO_COPY_*` strings left for copy not in sources: `LIB09_RECORDER_LINE`, `LIB13_PROTAGONIST_SUB`, `LIB24_DEFAULT_ON`, `LIB32_RELOCK_FIVE_MINUTES`, `LIB32_RELOCK_SCREEN_OFF`, `LIB38_CHANGE`, `LIB38_FIELD_DEF`, `LIB38_HIDDEN_RECORD`, `LIB38_REPLACED`, `LIB38_TEXT_RECORD`, `LIB41_CREATE_CSV`, `LIB41_CREATE_JSON`, `LIB41_CREATE_FILE`, `LIB41_PASSWORD_WARNING`, `LIB61_NETWORK`, `LIB61_NO_SPACE`, `LIB61_UNKNOWN`.

## Spec conflicts

- **P-09 `인물 12`**: the base fixture has 12 character rows but 노아 is in the trash (11 live; fixture.md conflict 7). The test builds its Given with fixture recipe 22 (노아 live) and keeps the expected `1장 7/10 · 인물 12 · 오늘 08:12`; `RowMeta` counts live characters.
- **P-01 `scene titled 씬 1`** vs LIB-12 placeholder: implemented as an empty stored title shown as `씬 1` (see Defaults); the test asserts the shown label.
- **LIB-56 path**: spec `ED` path `app/src/test/resources/format/<version>/`; this module cannot write under `app/`, so the file and its test live in `domain/library/src/test/resources/format/1.0/` (see Requests).
- **LIB-49 `voice.json`** vs 00 C-17: followed C-17 (no `voice.json`).
- **Trash row copy differs from domain:trash** for rows the sources leave open: world items here `<kind> · <project>`, there `<kind> · <project> · 참조 N`; change records here `TODO_COPY_LIB38_CHANGE`, there `◆ <entity> · <field>` / `변화 · <project>`; field definitions here `인물 · <project>`, there `항목 · <project>`. Both modules implement the T-cases; one row builder should be chosen.
- **Fixture variants** (the Then is kept as the spec states it; only the Given is built from the example with a recipe, so no test is `@Ignore`d):
  - R19-01/02: only R1 linked (`기록 1`); the example links more records to 잿빛.
  - E-11/E-12: R2 made hidden (`숨긴 기록 1`); the example has no hidden record (fixture.md 26).
  - E-02…E-06, I-02, I-03: 1-7 씬 4 hidden; I-03 adds 12 blocks to it (the example's 씬 4 is empty, fixture.md 1).
  - I-04 and the other 노아 cases: 1-6 씬 5 and 노아 brought back from the trash, 노아 hidden (fixture.md 21, 22).
  - F-09 open-file sample `씬 31 · 인물 11`: the example has 34 live scenes (fixture.md 6); the test checks 34 on the example and the sample string on a variant without 2-3 and 2-5.
  - B-04/B-05: 노아, 1-7 씬 4 and the term 대정전 hidden (three hidden items).
- **LIB-54 "hidden on this device" vs §6 Q5 backups**: a 복구 암호 backup of a visible project with hidden items would otherwise come back as a hidden project. Chosen: files locked with an export `암호` (or an unknown lock) land hidden; recovery-locked backups keep the project's own flag (`ImportedPackage.lockType`). Tests: F-15, B-05.
- **Lock rules in two places**: shell's `LockRules` (core:ui/app) implements the same L-xx / RL-xx table as `lock/LockScreens.kt` and `lock/Relock.kt` here; both run the same cases and must stay equal.

## Requests

- **app**: ship `domain/library/src/test/resources/format/1.0/잿빛 새벽의 등불.civilight` as the LIB-17 onboarding asset (install with `ProjectImports.installExample`), or move the regression files to `app/src/test/resources/format/` and point `FormatRegressionTest` there. Never regenerate a released version's file; while 1.0 is unreleased, regenerate it if `ExampleProject` changes (`FormatRegressionTest` then fails on purpose).
- ~~**domain:script**: implement `ExportSerializer`~~ Done for CSV and engine JSON (2026-11-22) in `domain:actions` `ExportActions` (`CSV`, `ENGINE_JSON`, `run` = `ExportRun.run` with the script's voice IDs); PDF stays in feature:library. Original request: implement `ExportSerializer` (PDF, CSV, engine JSON) over `domain.library.export.ExportPlan` / `CsvRows`, or map its own `ExportScope` onto the plan, so range, hidden-scene and voice-fixation rules are decided once. Its `GameJsonExport.FORMAT` already matches LIB-45 (`civilight-engine`); `characters[]` profiles can come from domain:cast `CharacterExport` (NAME_ONLY speakers in the plan get id · 이름 · 표시 이름 only).
- **domain:trash / feature:library**: pick one trash row builder (`domain.trash.TrashListing` is the fuller one: reference scanner, merge records); `domain.library.trash.TrashHost` can then be reduced to filters and countdown.
- **feature:library / app**: wrap the backup data key with the Android Keystore (`RecoveryKey.toMaterial(dataKeyWrappedByKeystore, alias, setAt)`), and run PBKDF2 off the main thread (600 000 iterations).
