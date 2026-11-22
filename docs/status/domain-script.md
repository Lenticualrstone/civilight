# domain:script

Pure Kotlin rules of the script editor (`docs/spec/10-script.md`), package
`io.github.lenticualrstone.civilight.domain.script`, over the `core:model` types. Every operation is a
pure function `ProjectDoc -> result`; undo = the caller keeps the previous doc (one compound
operation = one result). Resolved 때 comes in through `SceneTimeSource`; `StoryTimes.source(doc)` is
the adapter to the `domain:story` time engine. It is the only main source file that reads that engine; tests also call `domain:story` directly (see Requests).

`tools/gw :domain:script:test` passes: 146 tests, 143 passed, 3 ignored (spec conflicts below), 0 failed.

## Done

Main sources are in `domain/script/src/main/kotlin/io/github/lenticualrstone/civilight/domain/script/`. Tests are in the matching `src/test` folder.

**Spec §4 cases** (each test is named after its case id):

| Cases | Test class | Notes |
| --- | --- | --- |
| T-12-01 … T-12-25 (rule 12) | `Rule12BlocksTest` | All cases. T-12-07 and T-12-15 check resolved times and the `◇ 순서와 어긋남` count with the story engine. T-12-17 board row split is ignored (conflict 2). |
| T-11-01 … T-11-03 (rule 11) | `Rule11PositionTest` | |
| T-22-01 … T-22-26 (rule 22) | `Rule22ScenesTest` | Every "resolved time" assertion is recomputed by the story engine on the result doc. F4(c) is ignored (Q-22). The exact time in T-22-24 is ignored (conflict 4). |
| T-20-01 … T-20-04, T-19-01 … T-19-04, T-21-01 … T-21-03, T-23-01, T-23-02, T-18-01 | `Rules18To23Test` | Only the script-visible half of each case (see Partial). |

**Features**, with their files and tests:

- **SCR-21 block numbers** (`Numbering.kt`, `Labels.kt`). Each numbering unit has a counter that only grows. A number is never reused. Display is `#043`, and `#1043` from 1000 up. Tests: T-12-02, T-12-12, `RunsAndLabelsTest.number labels and particles`.
- **SCR-22 renumbering** (`Numbering.renumberPreview` / `renumber`). The preview sheet shows the three figures, rows, a `외 n개` line and a footer. Applying it gives 1…N in manuscript order, keeps fixed IDs, and leaves the counter unchanged. Test: T-12-17.
- **SCR-23/24/25 voice IDs** (`VoiceIds.kt`, `ExportScope.kt` `ExportVoices`).
  - Pattern tokens `{code}{chapter2}{episode2}{number3}`, with an `_n` suffix for an option.
  - `자동` IDs are computed. `고정` is set on the first export and records `lastExport`.
  - `버린 ID` carries a reason: deleted, kindChanged, merged, replaced or formatRefixed.
  - `새 ID로`, `고정 ID도 새 형식으로`, the `고정 ID n개는 그대로` notice, manual mode, and the `voiceIdEnabled` toggle.
  - Tests: T-12-13, T-12-18, T-12-23, `VoiceIdsTest` (9 tests).
- **SCR-12/13/15/18/09 block operations** (`BlockOps.kt`).
  - Insert with the inherited speaker. Enter at the end. Split at the caret, which keeps records, key, number and voice on the front block and trims the seam.
  - Backspace: merge with the previous block (records move without a flag, a fixed ID is discarded, `아님` and acknowledgements follow), remove an empty block, trash an emptied block that anchors records, and never cross a scene head.
  - Kind change, speaker change, stage position, choice options (at least 2).
  - Tests: T-12-08/09/16/20, T-22-07, `BlockOpsEdgeTest`.
- **SCR-37/40 move, cut, copy and paste** (`BlockMove.kt`). Moving to another episode gives that episode's next numbers. Toasts: `◆ n개 씬 3 → 씬 4`, `◆ n개 1-7 → 1-8`, `◇ 첫 공개 … · ▲ 새로 k`. A copy re-mints the keys of blocks, cues, options and links, and drops records (`◆는 복사되지 않음`). Cut and paste works as a move. Plain-text paste makes narration blocks. Tests: T-12-10/11/12/14/21/22, T-22-19, `BlockOpsEdgeTest`.
- **SCR-38/39 delete, block trash, restore and undo** (`BlockTrash.kt`). A non-empty block goes to the trash with a `TrashPlacement` and a per-scene, per-day `blockGroup` entry. An empty block is removed with no toast. Every toast shape is covered. Undo re-attaches records. Restore places a block between its neighbours or at its ordinal, offers `씬도 되살리기` · `다른 씬 끝에`, and removes the deletion's `버린 ID`. The trash rows read `1-7 씬 3 · 블록 4개` · `오늘 지움` · `30일 남음`. Tests: T-12-01/03/04/05/19/24, `RunsAndLabelsTest.toast copy…`.
- **SCR-43/44 anchor relocation** (`Anchors.kt`).
  - Relocation goes to the next surviving block: same scene, then next scene, then next episode. If none is left the record is `unplaced` (예정). The record is flagged `orphaned` and keeps its original anchor.
  - Records re-attach on undo or restore.
  - `다른 블록에 잇기` candidates, `이대로 두기` (state `kept` plus a keep acknowledgement), `기록 지우기` (trash entry of kind change).
  - Tests: T-12-01…07, T-12-25, `ManuscriptTest.point lines…`.
- **SCR-41/42/45 records from a block, marks and point lines** (`Records.kt`). `createAtBlock` creates block-seat records. A term reveal re-seats the existing record instead of adding a second one. Marks are `◆`/`◇` and dotted when orphaned, kept, or when the target is deleted. Point-seat lines read `◆ 1-7부터 · 근거 블록 없음` + `블록에 잇기`. World-event records never show. Tests: T-12-16, T-12-25, `ManuscriptTest`.
- **SCR-20/26/27/29/57/58 display layers and labels** (`Manuscript.kt`).
  - Rows per episode under `ScriptLayers`: direction, memo, system and transition line, marks, parens, address hints.
  - `대사만` preview.
  - Speaker labels: `??? 하르트`, `화자 없음`, `숨긴 인물`, deleted `노아`, `영구 삭제 · 노아`.
  - Hidden scene row `숨긴 씬 · 블록 n · 지문으로 보기`. Address hint `리엔을 ‘꼬마’`.
  - Tests: T-19-01/02, T-20-01…04, T-22-23, `ManuscriptTest`.
- **SCR-46/48/02 scene heads, transition text and position line** (`SceneText.kt`). Head text and time part. `자동` text for both templates, `직접` text with a snapshot, `없음` keeps the last direct text. Transition ID is `tr-{sceneKey}`. Also the header line, the position label, `렌즈를 이 씬으로 맞추기`, and `저장됨`. Tests: T-22-01…06, T-11-03, T-18-01.
- **SCR-49/53 `/씬` routing, `다음 씬 만들기` and `앞에 새 씬`** (`SceneCreate.kt`). Tests: T-22-08, T-22-12.
- **SCR-50 split** (`SceneSplit.kt`). Preview: part cards with the excerpt rule, place and time rows, silent cast chips, flashback and hidden rows, and the `바뀌는 것` rows. Applying it changes no number, voice ID or record. Tests: T-22-09/10/11, `StoryCrossCheckTest` (split, merge and delete give the same 때 and block order as `domain:story`).
- **SCR-51 merge** (`SceneMerge.kt`, `SceneRefs.kt`).
  - Blocked cases. A place or time choice is asked only when both scenes differ. A memo line holds the unchosen values. B's direct text becomes a system block or is dropped.
  - Dependents, live or in the trash, are re-based with their absolute 때 kept.
  - Every in-project reference to B moves to A: record anchors (and remembered originals), reveal and unlock points (map overlays, events, archive documents), the lens, story events, travel overrides and `rebasedFromKey`. Acknowledgements about B are dropped.
  - Tests: T-22-13…17, T-22-24/25/26, `Rule22ScenesTest.merge moves every reference…`, `StoryCrossCheckTest.merge…`.
- **SCR-52/54 scene delete, restore and move** (`SceneLifecycle.kt`).
  - Delete relocates records to the next scene and re-bases dependents, with toast `씬 3 지움 · ◆ 3 · ◇ 1은 씬 4 처음으로`.
  - Restore returns rebased scenes to this scene.
  - The move preview takes its mismatch count from the engine. A move to another episode renumbers the blocks.
  - Tests: T-12-07, T-12-15, T-22-18, `StoryCrossCheckTest`.
- **SCR-62 checks** (`ScriptChecks.kt`). Checks: `▲ 화자 없음`, `◇ 근거 지워진 변화 n`, `형식과 다른 ID n`, `▲ 보이스 ID 겹침 n`, `◇ 전환 문구 · 쓴 뒤 때/장소 바뀜`, `◇ 공개 전 장소 · 전환 문구`, `▲ 숨긴 씬으로 가는 선택지 n`. An acknowledgement with a matching fingerprint silences its item. Tests: T-12-13/18/23, T-22-02/06, `ManuscriptTest`, `VoiceIdsTest`, `ExportsTest`.
- **SCR-63 engine JSON** (`GameJsonExport.kt`).
  - Envelope `civilight-engine` v1, per 00-domain-model C-27.
  - Each scene carries its resolved time, place and transition. The head is never in `blocks[]`.
  - Every block kind is written. Choices carry `rejoin`. `stage` and `voice` are written only when enabled. Characters are speakers only.
  - Writing the file fixes the written IDs.
  - Tests: T-22-21, T-12-18, T-19-04, `ExportsTest`.
- **SCR-64 dialogue CSV** (`DialogueCsvExport.kt`). Columns `화자 · 대사 · 보이스 ID · 블록 ID · 상태`. Rows: the transition row per scene, dialogue blocks and spoken options, then `버림` rows. `새` / `바뀜` / empty come from `lastExport`. Output is RFC 4180 with a BOM. Tests: T-22-22, T-20-04, `ExportsTest`.
- **SCR-65 PDF content** (`ScriptPdfContent.kt`). The head is printed as a title line. Direction blocks and parens follow the `연출 · 괄호 지문 포함` choice. Hidden scenes default to `빼기`. Test: `CommandsLinksPdfTest`.
- **SCR-61 assets** (`ScriptAssets.kt`). Use counts per episode, first-use episode, `안 쓰는 것`. Sublines: `이 에피소드 n회` / `n회`, `2분 34초 · 반복`, `아직 이미지 없음` / `비어 있음`. Also sections and the cue picker filter. Test: `AssetsAndCountsTest`.
- **SCR-35 counts** (`WritingCounts.kt`).
  - Grapheme counts of game-output text.
  - `오늘` = Σ max(0, now − day baseline), using `DayBaseline`. The card shows `1,240자` with `목표 2,000자`.
  - `이 씬 분량` reads `리엔 3 · 하르트 2 · 세라 1 · 오웬 1 · 나레이션 4`.
  - Dialogue reference count for a character.
  - Test: `AssetsAndCountsTest`.
- **SCR-19/36 speaker picker and select mode** (`SpeakerPicker.kt`). Picker sections `이 씬 등장 · 최근 · 전체`, the deleted-speaker row, and `숨긴 인물 n · 지문으로 보기`. The select-mode bar reads `고른 블록 n개 · #a ~ #b · ◆ · ◇ 같이 감`. Tests: T-19-03, T-20-02, `ManuscriptTest`.
- **SCR-55/56 links and term candidates** (`Links.kt`, `TermCandidates.kt`). Link states: live, `숨긴 인물/항목`, `지운 인물 · 노아` with 4 buttons, `영구 삭제 · 노아`. Also `글로 풀기` and `다른 항목에 잇기`. For candidates: underline visibility (layer off by default, game-output kinds only), the popover with `후보 · 끊김`, `첫 공개 1-7 · 잇기 시`, `▲ 공개 전 사용`, `잇기` linking exactly the range, and the alias row. The pre-reveal margin `▲` is `Panels.preRevealLinks`. Tests: T-21-01…03, T-11-02, `CommandsLinksPdfTest`.
- **SCR-59 recorder memo** (`RecorderMemo.kt`). Inserts one `⌁` memo with its source and the next number. Toast `1-7 씬 3 · #058 작가 메모 넣음` with `되돌리기 · 가 보기`. Also `마지막으로 쓰던 곳 · 1-7 씬 3 · #044 뒤`. Tests: T-23-01/02.
- **SCR-12/68 sheet and commands** (`ScriptCommands.kt`). Add-sheet rows with shortcuts, the subline `새 대사 화자: 리엔(직전)`, `/x` parsing, the command list, and jumps by `#043`, `씬 3` or a title. Test: `CommandsLinksPdfTest`.
- **Panel rules** (`Panels.kt`). Block-level relation state `만나기 전 → 경계 ◆ 여기서`, the orphan card, the deleted-speaker field, and the voice field. Tests: T-11-01, `ManuscriptTest`.
- **Time adapter** (`StoryTimes.kt`). Checked against the F1 table, the `exact` precision of T-22-21 and a stale-free fixture. Test: `StoryTimesTest`.

## Partial

- **SCR-17 undo.** There is no undo stack here: callers keep the previous doc. `BlockTrash.undoDelete` covers undo after a doc was replaced.
- **SCR-28 `낭독`.** It draws like `쓰기` (Q-08).
- **SCR-32/33/34 panel tabs.** Only the content rules above exist. `말투`, `최근 대사` and the full `관계 · 지금` list are left to the UI with the cast domain.
- **SCR-35 cards.** The `점검` card counts are not built, and the goal is a parameter.
- **SCR-47 head ⋯ value lines.** Only the title exists. The value lines reuse `SceneText`.
- **SCR-60 read-only mode.** Only the `/씬` routing is dimmed. Editing gates belong to the UI.
- **T-20-03 and T-20-04.** Restoring and purging the character belong to the cast and trash areas. The tests simulate them and check the script's labels, picker, exports and voice IDs.
- **T-21-01 and T-21-03.** Candidate matching, `아님` storage and the `@` picker search belong to the world and search areas. The script renders and links.
- **T-19-04.** Biometric confirmation belongs to the export area. The script gives the hidden row, the `넣기` / `빼기` defaults and the export content.
- **T-22-20.** "No travel check changes" is asserted with `TravelCheck(doc).warnings` from `domain:story`.
- **Recorder links to a merged-away scene.** Recorder links that name a scene removed by a merge live in the recorder library, outside the project, and are not retargeted here.
- **UI-only features (not in this module).** SCR-01, 03, 05/06/10/11 rendering, 14, 16, 30, 31, 66, 67, 69.

## Defaults used

**Voice IDs and numbering**
- **Q-06 episode code.** `{chapter}` is the ordinal of the numbering unit's parent and `{episode}` the ordinal of the numbering unit, both among live siblings. A 1-level project gives `00`.
- **Q-07 renumbering.**
  - The counter does not change after renumbering.
  - Trashed blocks keep their numbers.
  - An automatic ID equal to a `버린 ID` raises `▲ 보이스 ID 겹침`.

**Exports**
- **Q-10.** Parens are exported as `(…)`. Links are flattened to their text. A break is `\n`.
- **Q-25.** The transition ID is `tr-{sceneKey}`.
- **Q-26.** A direction block is `{type: direction, cues: [{op, asset}]}`. A screen effect's `op` is the effect name.
- **Q-27 CSV.**
  - `바뀜` means the text (plain-text hash) or the speaker changed since `lastExport`.
  - Transition rows take a caller-supplied ledger and are `새` without one.
  - Spoken options get their own rows.
  - `화자` is the character name (the name tag after a purge).
- **Q-28.** A choice "leads into" a hidden scene when it is the last live block of a visible scene and the next scene of the same episode is hidden.

**Block editing**
- **Q-12 kind change.** Conversion between text kinds is free. Direction and choice convert only when the block is empty. A block converted to dialogue starts with no speaker.
- **Q-13 merge.** Any two text kinds merge. The previous block keeps its kind and speaker, and the texts join with one space. Direction and choice blocks never merge.
- **Empty first block.** Backspace in an empty first block of a scene does nothing.
- **Q-18 restore.**
  - The block goes after its live previous neighbour, else before its live next neighbour, else at its stored ordinal.
  - `다른 씬 끝에` in another episode issues that episode's next number.
- **Q-19 paste.** Cut blocks stay until pasted. Plain text becomes one narration block per non-empty line.

**Anchors and records**
- **Q-20.** `이대로 두기` sets state `kept` and stores a keep acknowledgement. The dotted mark stays.
- **Q-21 relink candidates.**
  - For an orphaned block record: the block before the anchor, then the scene's other blocks in order.
  - For a point record: the blocks of the unit's first live scene.
  - For an unplaced record: the last live block.
- **SCR-42 `unitEnd` points.** They show at the end of the unit's last live scene as `◆ {unit} 끝부터 · 근거 블록 없음`.

**Scenes, heads and transitions**
- **Q-22 heads.**
  - An episode's first scene is `1일째` (rule 11).
  - A flashback reads `씬 2 · 회상 · {place} · 1195`.
  - An approximate time shows no clock in the head.
- **Q-24 stale transition.** The check compares the resolved story-day range and time-of-day range with the snapshot. Its filter is `씬`.
- **SCR-49 `/씬`.**
  - On the last block it routes to `다음 씬 만들기`.
  - A new scene receives one empty dialogue block with the inherited speaker.
  - On the head it offers `앞에 새 씬`.
- **SCR-50/51 point anchors.**
  - Split: `unitEnd(A)` records move to `unitEnd(B)`.
  - Merge: `unitStart(B)` records go to B's first former block, and `unitEnd(B)` records become `unitEnd(A)`.
  - B's row is removed, not trashed.
- **T-22-09 excerpt rule.** As the spec states it: at most 10 characters of whole words, parens removed, trailing punctuation dropped.

**Display and counts**
- **Q-09 address hints.** Hints cover the other characters of the scene (speakers and silent cast), seen from the speaker's side.
- **Q-16 counts.**
  - Grapheme clusters of dialogue (with parens), narration, system text and options. Spaces are included.
  - A block created today without a baseline counts from 0.
  - The goal defaults to 2000.
- **Q-34.** An empty dialogue block without a speaker raises nothing.
- **SCR-65 PDF.** The transition line is never printed. Memos follow the memo layer.
- **Q-03.** `줄바꿈` is a `Break` run inserted with `Runs.insertRun`.

## Spec conflicts

1. **F1 overlay instead of `@Ignore` for fixture conflicts.** The example project differs from SCR F1 (fixture.md #1, #2, #17, #21, #22). So the tests build F1 on top of `ExampleProject.build()` (`SpecFixtures.kt` `F1`) and run the cases as the spec states them.
   - **What the overlay changes.**
     - It adds 1-7 씬 4 blocks #049–#052, plus 1-7 씬 5 with #053.
     - Spec R1 is example R4. Spec R4 is example R5, moved to #042. Spec R5 is example R5A, moved to #045. The ◇ is R9.
     - The 1-7 counter stays 58.
   - **Cases that depend on the overlay:** T-12-06a/b, T-12-07, T-12-11, T-12-15, T-12-19, T-12-22, T-12-24b, T-11-03, T-22-07, T-22-09, T-22-13/14/17/18/21/24/25/26, T-19-02.
   - **Other test changes:**
     - F4(d)/(f) put region 하층 구역 on 1-2 씬 1 and 1-4 씬 2 (fixture #17).
     - T-19-01/03/04 and T-20-03 bring 노아 back from the trash (#22). T-19-01 also restores 1-6 씬 5 (#21).
     - T-20-01, T-20-02 and T-20-04 start from 노아 already in the trash instead of deleting him in the test (#22).
     - T-19-02 adds 8 narration blocks to reach the fixture count of 12.
   - **Extra merge row.** The example bases 1-8 씬 1 on 1-7 씬 4, so the T-22-13 merge preview has an extra row `1-8 씬 1 때 · 기준 씬 4 → 씬 3 · …`. It is asserted.
2. **T-12-17 board rows.** The case lists `#056, #057, #051, #058` one by one and `#055, #052, #053` in the `외` line. No stated rule produces that split. The implementation lists the first 4 changed rows in manuscript order. The case as stated is `@Ignore`d, and a companion test asserts the implemented rule.
3. **T-22-04 F4(c), blocked by Q-22.** Rule 22 says `약 7일째`, but rule 11 says `1일째`. The rule 22 case is `@Ignore`d. Rule 11 is implemented and tested.
4. **T-22-24.** The case wants S5 at exactly `2일째 아침 08:00` after re-basing on S3. But S3's day is a range (11–15, from 1-6 씬 1's `대략` link), and SCR-51 says the new interval is "a range when approximate". The engine therefore gives `약 2일째 아침`. The exact-time assertion is `@Ignore`d. The note, the bases, the fixed-point removal and the `씬 4 것` path are tested.
5. **T-22-09 vs T-22-10 row order.** T-22-09 lists `첫 공개 …` before `◆ 관계 …`, and T-22-10 lists them the other way round. The order of record rows is not asserted. The implementation lists records in block order.

## Requests

- **Orchestrator and `domain:story` (duplication).** Both modules implement the same scene operations under the same class names in different packages:

  | `domain:script` | `domain:story` |
  | --- | --- |
  | `SceneSplit` | `SceneSplit` |
  | `SceneMerge` | `SceneMerge` |
  | `SceneLifecycle` (delete, restore, move) | `SceneTrash`, `SceneMove` |
  | `SceneCreate` | `NewScene` |
  | `TimeChains` (interval rewrite) | `Rebase` |
  | `EditContext` | `EditContext` |

  `StoryCrossCheckTest` pins split and delete to the same resulting times. Please decide which module owns SCR-50…54 / STO-08…10, and have the other call it. Please also give one `EditContext` to both, for example in `core:model`.
- **`domain:story`.** Please keep these stable, or tell me when they change.
  - Used by main code, only in `StoryTimes.kt`:
    - `SceneTimes(doc).resolve / labelDay / storyDay`
    - `ResolvedTime.Timed.{t, band, clock, ownApprox}`
    - `OrderChecks.notices / newNotices`
  - Used by tests only:
    - `TravelCheck(doc).warnings` (T-22-20)
    - `scene.SceneSplit.plan`, `scene.SceneMerge.plan` with `MergeOptions` / `MergeSide` / `MergePlan.Ready`, `scene.SceneTrash.delete` and `scene.EditContext` (`StoryCrossCheckTest`)
- **`core:model`.** There is no stored export ledger for transition text, so CSV `바뀜` on transition rows (Q-27) needs the caller to pass `transitionLedger`. Please consider a scene-level `lastExport` (like `VoiceState.lastExport`) or an `ExportRecord` extension.
- **`domain:world`.** Candidate matching, `아님` storage and `@` picker alias search, for T-21-01 and T-21-03. The script calls `TermCandidates.link` / `popover` with the matched range.
- **`domain:cast` and `domain:trash`.** Character restore and purge (T-20-03/04). The script reads the result.
