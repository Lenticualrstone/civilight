# domain:recorder

Package root `io.github.lenticualrstone.civilight.domain.recorder`. Pure Kotlin over `core:model` only (no other domain module is on the classpath). Rule 23 and 70-life-recorder.md as functions over `RecorderLibrary`, `ProjectDoc` and the model's runtime types (`EntryContext`, `RecordingSession`, `TextMemoDraft`, `InboxFilterState`, `RecorderSettings`, `UnlockSession`).

Build and tests: `tools/gw :domain:recorder:test` exits 0: 95 tests, 87 run green, 8 `@Ignore`d as spec conflicts (listed below), 0 failures. Every §4 case LRC-T01 … LRC-T50 (with T38b, T38c) is a test named after its id. Where a case conflicts with the fixture there is a spec-literal `@Ignore`d test plus a running `_fixture` twin; both share one test body with the conflicting value as a parameter. Test fixture: `src/test/.../Fx.kt` (P1 = `ExampleProject.build()` with a plain 서재 row, P2 「유리 정원」 (with a 세라 only for LRC-T05), P3 「검은 서약 (가제)」, F-open = R1 … R4 of `ExampleRecorder` + synthetic R5 … R12, F-hidden).

Inputs used everywhere: `RecorderEnv` (projects of the 서재 as `ProjectSnapshot(LibraryEntry, ProjectDoc)`, `UnlockSession`, zone, most recent project; a project not listed = permanently deleted) and `RecorderEditContext` (key source, now, device, zone).

## Done

| Feature | What (files under `src/main/.../domain/recorder/`) | Tests |
| --- | --- | --- |
| LRC-01, LRC-05, LRC-06 (decision side) | `Recording.micTap` (start / open running / `마이크 꺼짐` / unlock first), `linkPickerNeedsUnlock`; lock-screen `정리 중` buttons (`capture/Recording.kt`) | `LRC_T49_lockScreen`, `pausedTimeIsNotCountedAndBackNeverDiscards` |
| LRC-07, LRC-08, LRC-09 (state) | Session reducer: `start`, `pause`/`resume`/`focusLost`, `elapse`/`silence` (no auto-stop), `partial`/`final`, `cancel` + `undoCancel`, `back` (never discards), `recordingScreen` (timer `00:12`, live mark, chips, transcript, `일시정지`) | `LRC_T47_noAutoStop`, `cancelFirstTakeDiscardsAndUndoRestores`, `pausedTimeIsNotCountedAndBackNeverDiscards` |
| Transcript text model (LRC-07, LRC-11) | `capture/Transcript.kt`: `TranscriptView(confirmed = ink, tail = ink3)`, `Transcripts.appendFinal` (plain only, space-joined; dictation never makes links) | `LRC_T48_*`, `switchingBetweenVoiceAndText` |
| LRC-13 | `Recording.stopAndSave` (record written at `멈추고 저장`), `continueSpeaking` (same record, timer resumes, text appended, revision +1), `keepInInbox` (links + hide saved), `processingScreen` (`받아쓰는 중 · 문장 나누기`, note, hide row, `여기 넣기`, buttons) | `LRC_T02_rule23Example`, `LRC_T48_resumeContinuesTheSameRecord`, `LRC_T20_fixture_*` |
| LRC-15, LRC-16 | `capture/TextMemo.kt`: `MemoSheet`, `open` (kept draft returns), `type`/`delete`/`edit`, `addLink`/`removeLink`/`replaceLink`, `canSave`, `save`, `close`, `view`; `toVoice`; `Recording.switchToText` (record stays voice) | `LRC_T04_atChipLine`, `LRC_T06_*`, `textMemoDraftIsKeptOnClose`, `switchingBetweenVoiceAndText`, `switchToTextNeverDuplicatesALink` |
| LRC-17 `@` parsing | `text/AtMentions.kt` (`queryAt`: `@` at start or after a non-letter, ≤ 20 chars, no line break, not inside a link; `complete`: replaces `@query` with a link run whose text is the chosen name or alias); `text/Runs.kt` (run editing over shown text, links atomic); `LinkEdits.syncAt` (AT links follow runs) | `LRC_T04_*`, `LRC_T06_*`, `atQueryRules`, `runsReplaceKeepsLinksAtomic` |
| LRC-18, LRC-19 | `link/TargetRef.kt` (identity `(projectKey, kind, key)`), `link/LinkEdits.kt` (add idempotent, × except `@` form, replace keeps the chip's place, `@` form for an already linked target), `link/EntryLinks.kt` (preset table, builders) | `LRC_T01_prefilledLinkByEntry`, `linkAddIsIdempotentAndAtMarksAnExistingLink`, `replaceKeepsChipPlaceAndBecomesManual` |
| LRC-20 | `picker/LinkPicker.kt`: all link units, `글에 나온 이름` (characters and terms whose name is in the text, by first occurrence), linked projects → recent project → rest; name / prefix / contains / alias / 초성; locked hidden projects and items, trashed targets and trashed projects never offered | `LRC_T05_atSearchOrder`, `LRC_T07_fixture_*`, `LRC_T08_lockedHiddenTargetsAreNotOffered`, `hangulInitialsMatch`, `aliasMatchUsesTheAliasAsRunText` |
| LRC-21 | `link/Chips.kt`: capture/record-screen row (`연결 · ` on the first chip, `세라 @` without ×), inbox tags (labels only; project tag omitted when an inner link exists); masked links of one project collapse | `LRC_T04_*`, `LRC_T09_*`, `masksCollapseByProjectAndKeepOrder` |
| LRC-22 | `link/LinkViews.kt`: §3.3 order (record hidden → hidden project → `recorderLinkMask` rating badge → hidden item → purged → trash → example → normal); hiding wins over deletion | `LRC_T29_maskAppliesAtOnce`, `LRC_T30_fixture_maskingTable`, `LRC_T38c_*`, `maskedProjectsInTheProjectMenu` |
| LRC-23 | Deleted targets: `1-7 씬 3 · 휴지통`, `지워진 씬 · 1-7 씬 3`, dotted `노아` + `◇`, `영구 삭제 · 노아`, project `… · 휴지통` / plain name, `예시 · 끝나면 연결 없어짐`; `link/DeletedTargets.kt` (`지운 인물 · 노아` card and buttons, `글로 풀기`, `다른 항목에 잇기`); scene moves relabel by key | `LRC_T34` … `LRC_T35`, `LRC_T38_*`, `LRC_T38b_*`, `LRC_T39_sceneMoved`, `LRC_T40_exampleProject`, `relinkRunPointsTheRunElsewhere` |
| LRC-24, LRC-47 | `hooks/RecorderHooks.kt`: `libraryCounts`, `unsortedLinkedCount` + `targetLine`, `recorderLinkCount`, `recorderAtLinkCount`, `linkedRecordCount`, `checklistLine`, `startMaterialCandidates` + header, `linkRecords` (`시작 재료`), `exampleProjectDeleted` | `LRC_T15_targetLineCount`, `LRC_T28_startMaterialAndChecklist`, `LRC_T38_*`, `LRC_T38b_*`, `LRC_T29_*` |
| LRC-25, LRC-27, LRC-30 | `inbox/Inbox.kt` (newest first only, rows, filter chips with counts, `프로젝트 ▾` menu, target chip `1-7 씬 3 ×`, empty `기록 없음`, filtered-empty flag), `inbox/InboxFilters.kt` (presets, kind toggles, project/no-link/target filters; the target filter is `InboxTarget(projectKey, kind, key)`, so a same item key in another project does not match; `toInboxTarget` / `toTargetRef`) | `LRC_T03_*`, `LRC_T09` … `LRC_T12`, `LRC_T15_*`, `kindFilterAndProjectChip`, `emptyAndFilteredEmptyInbox`, `InboxTargetProjectTest` |
| LRC-26, LRC-39 | `state/RecordStates.kt` (state derived: 넣음 if any placement not gone, else 끝냄 if marked, else 정리 안 됨; placement status ACTIVE / IN_TRASH / GONE; `◇ 넣은 뒤 고침`; `◇ 넣은 곳 지워짐`; implied projects), `inbox/RecordMenu.kt` (`끝냄으로 표시` / undo / `다시 정리 안 됨`) | `LRC_T13_doneAndBack`, `LRC_T14_insertIntoDoneRecord`, `LRC_T25_editsAfterInsertion`, `LRC_T36_fixture_*` |
| LRC-28 | `inbox/RecordScreen.kt` (`RecordScreens.of`, `editText` with revision +1 and `@` sync), `state/PlacementLabels.kt` (`1-7 씬 3 · #058`, `하르트 · 말투`, page name, `1-7 씬 3 · 씬 메모`, character name; ` · 휴지통`; masked) | `LRC_T25_*`, `LRC_T36_fixture_*` |
| LRC-29 | `RecordMenu.options` (order and conditions of the table, subs as drawn), `preview` | `menuOptionsAndPreview`, `LRC_T13_*` |
| LRC-32 | `put/Insertion.kt`: copy not move, two-way pointers (`recorderSource` + placement), link added when missing (origin `넣기`), `doneMarked` cleared, `@` runs into the destination stay links (new link key, no project key, surface kinds), others plain; `put/InsertionUndo.kt` (`Undo.undo`: content removed without a trash entry, placement and added link removed, `doneMarked` restored); `put/Destinations.kt` (biometric step for locked hidden / R-18, not offered: trashed, example, `설정만` for the manuscript); `put/SourceMarks.kt` (`레코더 11월 22일 08:41` / `레코더 · 휴지통` / `지워진 기록`) | `LRC_T14`, `LRC_T19_fixture_undo`, `LRC_T21`, `LRC_T23`, `LRC_T24_atRunsInCopies`, `LRC_T26_protectedDestination`, `LRC_T37_fixture_*` |
| LRC-33, LRC-34 | `put/PositionSheet.kt` (title, `연결된 씬 · … · 맨 끝`, `마지막으로 쓰던 곳 · … · #044 뒤`, `씬 고르기 ›`, `이미 넣음 · …`, initial project, `여기 넣기` label/spot), `put/ManuscriptInsert.kt` (one `⌁` memo block, next episode number, toast `… 작가 메모 넣음` · `되돌리기` · `가 보기`) | `LRC_T16_positionSheetRows`, `LRC_T17/18/19/20_fixture_*` |
| LRC-35 … LRC-38 | `put/CardInsert.kt` + `put/CardDocs.kt` (cards, tabs `개요 · 말투 · 비밀 · <긴 글>` / `설명 · <긴 글>`), `put/PageInsert.kt`, `put/MemoFieldInsert.kt`, `put/CharacterInsert.kt` | `cardInsertIntoSpeechTabAndUndo`, `characterTabsAndWorldTabs`, `LRC_T21`, `LRC_T22`, `LRC_T27_makeCharacter`, `sceneMemoGetsANewLineAndUndoRestoresIt` |
| LRC-40 … LRC-42 | `hide/RecordHiding.kt` (hide/unhide, hide-row condition and live switch), locked group / open group in `Inbox.screen`, `RecordSearch` (hidden and locked-hidden-project records excluded), `RecordScreens.needsUnlock` | `LRC_T10_filterCounts`, `LRC_T31_hideRowDefault`, `hideRowFollowsTheLinksLive`, `LRC_T33_searchExclusion`, `recordsOfALockedHiddenProjectAreNotSearched` |
| LRC-43 | `trash/RecordTrash.kt` (delete with entry + toast, restore with auth for hidden, purge, `isDue`, trash row `음성 0:12 · 라이프 레코더 · 1일 남음`) | `LRC_T37_fixture_*`, `LRC_T41_trashRowAndFilter`, `LRC_T42_*`, `hiddenRecordInTrashIsMaskedAndNeedsAuthToRestore` |
| LRC-44 | `settings/RecorderSettingsScreen.kt` (rows, subs, defaults; glow = `AppSettings.recorderGlow`) | `settingsScreenRows` |
| LRC-45, LRC-46 | `export/RecorderExport.kt` (row `레코더 기록 4 · 원본 음성 포함 · 숨긴 기록 1 제외`, records selection, `recorder.json` via `RecorderFile.forProject`, audio hashes, library file, other formats excluded), `RecorderMerge` (merge by key, local text kept; re-key for `새 프로젝트로`) | `LRC_T43_exportRow`, `LRC_T44_hiddenProjectFile`, `LRC_T45_otherFormats`, `LRC_T46_multiProjectAndMerge` |
| Capture guard (LRC-22) | `inbox/RecordSearch.kt` › `CaptureGuard.projectsFor / isSecure` | `LRC_T32_captureGuard` |
| Rule 21 (LRC-17) | `hooks/RecorderTermUse.kt` (never scanned; only `@` links count as term uses) | `LRC_T50_rule21DoesNotApply` |
| Labels | `TimeLabels.kt` (`오늘 08:41`, `어제 23:10`, `11월 18일`, `2025년 3월 4일`, `음성 0:24`, timer), `RecorderCopy.kt` (all UI copy; missing copy as `TODO_COPY_LRC_<n>` ids) | `LRC_T12_labels`, `timeLabels` |

## Partial

- LRC-02, LRC-03, LRC-04, LRC-09 (service), LRC-10, LRC-11, LRC-12, LRC-14 (player), LRC-31: platform and UI work (widget, tile, foreground service, recognizer, glow, audio, layouts). This module only provides their decisions and data (`micTap`, session reducer, `RecordingSession` for crash recovery, `hasPlayer`).
- LRC-41: the biometric prompt and re-lock are LIB-32/LIB-33; this module reads `UnlockSession.recordsUnlockedAt` and gives the locked/open group views and the note (`앱을 떠나면 잠김`; other policies TODO_COPY_LRC_18).
- LRC-46: `RecorderExport.libraryFile` builds `records.json`; packaging, encryption and Drive transport are LIB-48/LIB-57.
- LRC-23 purge marks (`purgedAt`, `lastLabel`, `lastLabelHidden`) are written by `domain:trash` (`RecorderTrash.applyProjectPurge/applyProjectGone`); this module only reads them (`LinkViews.lastLabelFor` gives the label to store).

## Defaults used

- `ED` time labels: another year `yyyy년 M월 d일`; durations from one hour `h:mm:ss` (row and timer).
- `ED` the `@` query looks back at most 20 characters; `@` must start the text or follow a non-letter; typing inside a link run turns it into plain text (the link goes).
- [PROPOSED] chip order = link creation order; a replaced chip keeps its place and becomes `직접` (MANUAL).
- [PROPOSED] × on a collapsed masked chip removes all its non-`@` links; plain (purged) labels have no ×.
- Purged unit links: `지워진 씬 · <scene label>` when the stored `lastLabel` starts with a scene label (`1-7 씬 3` of `1-7 씬 3 · 꺼진 등불`, as `domain:trash` writes it); chapter / episode / page labels stay plain (`TODO_COPY_LRC_08` not invented). A purged link with `lastLabelHidden` shows `영구 삭제 · 숨긴 항목` (or `숨긴 프로젝트` for a project link).
- Items in a trashed project render as in the trash (`… · 휴지통` / dotted card chip).
- `글에 나온 이름`: names only (not aliases), characters and terms of every offerable project; equal names ordered linked projects → recent → 서재 order.
- Units are searched by their label only (`1-7 씬 3`, `1-7`, `2장`), not by title (LRC-20 names name · 초성 · alias).
- Link lists are deduplicated by target alone (a list is one record's or one draft's links), so carried draft links never duplicate.
- Destination menu lists locked hidden projects as `숨긴 프로젝트` (choosing them runs the prompt); R-18 projects always need the prompt; trashed, example/read-only and (manuscript) `설정만` projects are not offered.
- `이미 넣음` rows: active block placements only.
- Card `개요` = the first non-secret `긴 글` character field (same as `domain:cast`); without one, `인물로 만들기` writes no paragraph. World cards: `설명` (description) + their `긴 글` fields. Card toast, page toast, memo toast, character toast, delete toast: TODO ids.
- Page body = one `⌁` memo `DocBlock` with `recorderSource`, plus `Page.sourceRecord` (as the fixture's page made from R0).
- Memo field: scene memo gets a new line; chapter/episode `synopsis` gets the line after a space; multi-line text joined with spaces. Placement label `1-7 씬 3 · 씬 메모`; chapter/episode label only (`2장`).
- A record's creation time after `멈추고 저장` defaults to the ULID time of the key minted at start (`startedAt` may be passed).
- `취소` during a continued take drops only the new take and returns to `정리 중`; a first take is discarded with an undo toast (TODO_COPY_LRC_03).
- Text memo draft link rows carry an empty record key until `저장`.
- `hasPlayer` = voice record with kept audio.
- Inbox preview excerpt: 30 characters + `…`.
- Export: hidden records go into the file only when the project is hidden and the file encrypted; library file includes hidden records only when asked (`ED` Q16).
- Import merge: links deduplicated by key and by `(record, project, kind, target)`; placements by key; local record wins.

## Spec conflicts

- **1-7 block counter** (fixture.md conflict 3): LRC §4.1 says #061 was the highest issued (next #062); the example fixture's counter is 58. Spec-literal `LRC_T17`, `LRC_T18`, `LRC_T19`, `LRC_T20`, `LRC_T36`, `LRC_T37` are `@Ignore`d (#062, #063); `_fixture` twins run the same bodies with #058 (and #059 after undo).
- **대정전 is a term** (fixture.md conflict 9): LRC-T07 expects `글에 나온 이름` = 하르트, 세라; the fixture's term 대정전 is in R1's text, so the group is 하르트, 대정전, 세라. `LRC_T07_*` `@Ignore`d; twin runs.
- **LRC-T30 vs fixture P3**: P3 has `라이프 레코더 연결 가리기` on (§4.1), so after unlocking the hidden group §3.3 step 3 shows `R-18`, not the scene label T30 expects. `LRC_T30_*` `@Ignore`d; twin asserts `R-18` and shows the scene label for a hidden project without the mask.
- **Cross-module (not a test)**: `domain:library`'s `RecorderSlice.select` uses `RecorderLibrary.recordsLinkedTo`, which counts placements as links; this module's export row counts links only (LRC-45 "linked records"). They differ only when the user removed the `넣기` link of a placement.

## Requests

Logic duplicated here because this module may depend on `core:model` only (the orchestrator may want to dedupe or add module dependencies):
- `trash/RecordTrash.kt` repeats `domain:trash` `RecorderTrash.delete/restore/purge` (same rules, plus this area's trash row and toast).
- `export/RecorderExport.kt` (`linkedRecords`, `exportedRecords`, `projectFile`) and `RecorderMerge` repeat `domain:library` `RecorderSlice` and `RecorderMerge`.
- `put/CharacterInsert.kt` and `put/CardInsert.kt` repeat `domain:cast` `CharacterEdits.createFromRecord` / `appendMemo`. Differences: this module copies the record's **runs** (`@` links into the destination kept, others plain, LRC-32) while cast copies plain text; this module's first sentence ends at the first `.`, `?` or `!` of the first line; cast's ends at `. ? ! 。 …` followed by whitespace or the end.
- `put/ManuscriptInsert.kt` repeats `domain:script` `RecorderMemo` (SCR-59): one `⌁` memo block with the next number.
- `link/UnitLabels.kt` repeats `domain:story` `UnitCodes`; `LinkViews.ratingBadge` repeats `domain:library` `RatingLabels.badge`; `RecorderEditContext` repeats `domain:story` `EditContext`; `text/Hangul.kt` repeats `domain:cast` `Hangul` initials.

Other requests:

- ~~`domain:library` / `domain:trash`: plain unit label as `lastLabel`~~ Done (2026-11-22) in `domain:actions` `PurgeActions.withPlainUnitLabels` (`domain:story` `UnitCodes.unitLabel` before the purge: `1-7 씬 3`, `1-7`, `1장`), used by `PurgeActions.runDue` and `CharacterActions.purge`. Original request: when a unit is purged, store the unit's plain label (`1-7 씬 3`) as `RecorderLink.lastLabel` (or keep `<label> · <title>` as now; this module parses the scene label from its start). `LinkViews.lastLabelFor(env, target)` gives the label this area prints.
- `domain:library`: `RecorderSlice.select` should select by links only (LRC-45), or tell this area to count placements too, so the export row and the written file agree.
- `domain:script` (SCR-59) and this module both insert the recorder memo block; the script module may call `ManuscriptInsert.insert` once `domain:script` can depend on `domain:recorder`, or this module can switch to the script's insert when allowed to depend on it.
- ~~`core:model`: `InboxFilterState.target` is `(kind, key)` without the project key~~ Done (2026-11-22): `InboxFilterState.target` is `core:model` `InboxTarget(projectKey, kind, key)`; `InboxFilters` matches links with `pointsAt` (project key included) and the target chip shows only while some record links to that exact target (a project target always). `InboxTargetProjectTest` covers two projects sharing an item key.
