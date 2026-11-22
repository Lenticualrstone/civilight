# domain:story

Package `io.github.lenticualrstone.civilight.domain.story`. Pure Kotlin over `core:model`: functions take a `ProjectDoc` and return a preview plus the new `ProjectDoc`. One result doc is one undo step (the caller keeps the previous doc). Build and tests: `tools/gw :domain:story:test` passes, 163 tests: 159 passed, 0 failed, 4 ignored as spec conflicts. Every rule case in 20-story-time §4 is a JUnit test named after its id. Tests use `ExampleProject.build()` plus the deltas each case's Given states. Travel cases TRV-09 … 13 use a small `MiniProject` fixture.

## Done

| Feature | Files (`src/main/.../domain/story/`) | Tests |
| --- | --- | --- |
| STO-03 unit codes | `units/UnitCodes.kt` (`1장`, `1-7`, `씬 4`, `1-7 씬 3 · 꺼진 등불`, `Ⅰ 1장 · 등불지기`, custom hierarchies) | `StructureTest` STR-01, STR-04; used everywhere |
| STO-01, STO-04, STO-06 structure rows, statuses, progress, char counts, `오늘 쓴 글자`, `이어 쓰기` | `structure/StoryOverview.kt`, `structure/StructureEdit.kt`, `text/BlockText.kt` | `StructureTest` STR-01 … STR-05, `homeStatsAndResume`, `hiddenSceneRows`, `charCountRules`, `todayChars`, `lens09_episode18StaysUndecided` |
| STO-02 rename levels, level-count lock | `structure/StructureEdit.kt` | `StructureTest` STR-03 |
| STO-05 new scene prefill and creation | `scene/NewScene.kt` | `TimeEditTest` INS-01, `SceneOpsTest` TIME-12 prefill |
| STO-07 scene-head ⋯ rows | `scene/SceneHeads.kt` (`HeadMenu`) | `TransitionTest` `headMenuRows` |
| STO-08 scene move preview, unit reorder | `scene/SceneMove.kt` | `TimeEditTest` MOVE-01, `SceneOpsTest` T-12-15 |
| STO-09 split and merge values | `scene/SceneSplit.kt`, `scene/SceneMerge.kt` | `SceneOpsTest` SPL-01, SPL-02, MRG-01 … MRG-04, T-22-17/24/25/26 |
| STO-10 delete and restore (때 rebasing, `rebasedFromKey`) | `scene/SceneTrash.kt` | `TimeEditTest` DEL-01, DEL-02; `SceneOpsTest` T-22-18; `LensTest` LENS-12 |
| STO-12 … STO-16 lens: 집필 선, chip, sheet rows, `valueAt`, 예정, two edit buttons, banner, script lens | `lens/LensView.kt`, `lens/LensEdit.kt`, `lens/ValueText.kt` (on core's `ManuscriptIndex`) | `LensTest` LENS-01 … LENS-14, EDIT-01 … EDIT-04, `editSheetLines` |
| STO-17 … STO-19, STO-21, STO-22 때 engine: base + interval, shared `대략` variables, bands, 04:00 day, `N일째` per episode, `약 N일째 (a~b)`, anchor-relative labels, calendar label, time order | `time/SceneTimes.kt`, `time/Span.kt` (`LinExpr`), `time/ResolvedTime.kt`, `time/Bands.kt`, `time/TimeLabels.kt`, `time/Durations.kt` | `TimeResolutionTest` TIME-01 … TIME-13 (+ notation, cycles, flashback base, calendar, midpoint), `BandTest` BAND-01 … BAND-05 |
| STO-20 `장소와 때` sheet values | `scene/SceneWhereSheet.kt`, `time/BaseRules.kt` | `SceneWhereTest` (travel block: only open ▲, `sceneWhereTravelBlockForS9`); `TimeEditTest` BASE-01 … BASE-03 |
| STO-23 cascade preview (`뒤도 같이` · `이 씬만`) | `time/TimeChange.kt`, `time/Rebase.kt` | `TimeEditTest` CASC-01 … CASC-05 |
| STO-24 회상 (labels, result line, base skipping, split/merge rules) | `time/TimeLabels.kt`, `time/SceneTimes.kt` | TIME-12, SPL-02, MRG-03, `SceneWhereTest.flashbackSheet` |
| STO-25 `순서와 때가 다름`, `시각과 시간대가 다름` | `time/OrderChecks.kt` | ORD-01 … ORD-03, CASC-02/03, MOVE-01, `AckTest.clockBandNoticeForBand04` |
| STO-26 등장 | `scene/Appearance.kt` | `SceneWhereTest`, `TravelTest` |
| STO-27 (+ SCR-46/47/48 text) scene head, 전환 문구 auto/direct/none, templates, snapshot, stale and pre-reveal checks | `scene/SceneHeads.kt`, `transition/Transitions.kt`, `transition/TransitionChecks.kt` | `TransitionTest` TRN-01 … TRN-05, T-22-01, T-22-02, T-22-04 (T-22-03 = TRN-01/02, T-22-05 = TRN-05, T-22-06 = TRN-04; T-22-11/12 = `SceneOpsTest.spl02_inheritance`, `TimeEditTest.ins01`) |
| STO-28 … STO-30 travel: modes, distance, required time, gap, verdicts, chips, `맞음` fingerprint | `travel/TravelModes.kt`, `travel/Distances.kt`, `travel/TravelCheck.kt` | `TravelTest` TRV-01 … TRV-16 (TRV-07 also with `Acks`) |
| STO-31 … STO-35 bulk list, place, time, suggestions, inner table cells | `bulk/BulkList.kt`, `bulk/BulkPlace.kt`, `bulk/BulkWhen.kt`, `bulk/Suggestions.kt`, `text/WordMatcher.kt`, `text/Sentences.kt` | `BulkTest` BLK-01 … BLK-03, SUG-01 … SUG-05 |
| STO-36 command bar placeholders | `search/CommandBar.kt` | `SearchTest.commandBarPlaceholders` |
| STO-37, STO-38 search and commands (NFC, Latin case, 초성, whole-sentence hits, ▲ flag, hidden/deleted exclusions, similar names, commands, `#043`) | `search/TextQuery.kt`, `search/ProjectSearch.kt` | `SearchTest` SRCH-01 … SRCH-07 (+ scenes, filters, sentences) |
| STO-40, STO-41 registry markers/filters, open vs acknowledged, `맞음`/`이대로 두기`/`되돌리기`, subject deletion, counts, ▲-before-◇ order, locked hidden collapse, empty state | `checks/Acks.kt`, `checks/CheckCenter.kt`, `checks/Fingerprints.kt` | `AckTest` ORD-03, CHK-04, CHK-05, CHK-07; `CheckCenterTest` CHK-01 … CHK-03; `TravelTest` CHK-06 |
| STO-42 check switches | `travel/TravelCheck.kt` (`TravelSettings`) | `TravelTest` CHK-06 |

## Partial

Features whose engine part is listed under Done but whose screen, sheet or storage lives elsewhere (rules and tests here, no UI):

- STO-01 / STO-04 screens, the lens sheet UI, `씬 단위로 고르기`, motion: UI is the feature modules'. This module gives the row values.
- STO-43 (inner layouts) is not started here: it is layout only, with no domain logic.
- STO-07: the scene-head ⋯ rows are done. The story-list scene ⋯ and the episode ⋯ items have no domain logic beyond the existing actions. Copy for new episode/chapter is not drawn.
- STO-11: hidden scene masking (`숨긴 씬`, `숨김` tag) and deleted places (`지운 장소 · …`) are done. Rendering of deleted silent-cast members (`빼기 · 되살리기 · 이대로 두기`) is cast UI.
- STO-24: the flashback `전 · 후` amount is stored as `FlashbackAmount.days` but not resolved into a year (Q20 open).
- STO-39 / STO-40: the semantics are here. The items of rows 1–9 and 19 are emitted by world, SCR and cast. `domain:checks` should collect them as `CheckEntry`s and use `CheckCenter`. The scanning state (`점검 중`) is UI.
- STO-13 / STO-16: lens persistence (`LensState` in `_app/view.json`) is the store's. `ScriptLens` gives the enter / set / leave transitions.

## Defaults used

- **Base fallback:** a `기준` whose base is missing, untimed, 회상 or in a cycle resolves against the nearest earlier live timed non-flashback scene. A base in the trash still resolves through its own stored 때 until the delete rebases its dependents.
- **Time order (Q18):** lower bound of the absolute minute, then manuscript order. Untimed scenes sort last.
- **`대략` (Q17):** a chip `+n` with `대략` gives `[n−1, n+1]` clipped at 0, and `같은 날` gives `[0, 1]`. The bulk preview shows the nominal chip day plus `· 대략`.
- **`약 N일째` (Q30):** N is the midpoint, with .5 rounding up. The order line `직전 씬보다 앞 · 약 …` uses the midpoint (Q29).
- **Cascade:**
  - A change that does not move the story day (time-of-day only) moves no dependent, so it is stored without a preview.
  - A scene leaving the chain (때 cleared) rebases its direct dependents onto the previous timed scene, absolute time kept.
  - `이 씬만` with an inexact delta widens the dependents' interval and marks it `대략`.
- **Durations:** ≥ 24 h reads `D일 H시간`; < 24 h reads `약 H시간 M분` (10-minute rounding); gaps are exact `D일 H시간 M분`.
- **Unit codes (Q2):** `<n><name>` for the top level, `c-e` (all ancestor ordinals) for the episode level, `<name> <n>` for the scene level. Roman glyphs `Ⅰ … Ⅻ`, then Latin letters.
- **Lens:**
  - Sheet rows (Q11): 최신, episodes where a record of the viewed targets starts, the viewed point, and the first unwritten episode. The sub line is the episode title.
  - A unit-end anchor is labelled `1-7 씬 3 뒤`.
  - The `예정 · 2-5` point is the anchor's episode code.
  - `??? (1-8까지)`: the previous value plus the episode before a unit-start anchor (or the block before a block anchor).
- **Flashback result line:** `이야기 시작 해` when the year equals the start year, `이야기 시작 n년 뒤` when it is later.
- **Calendar label (Q14):** `<month> <d>일`; a range reads `<month> <a>일 ~ <b>일`.
- **Scene head:** a deleted place reads `지운 장소 · <name>`. The ⋯ row `앞에 새 씬 | 빈 씬 · 씬 4 앞` is `[PROPOSED]`. In a read-only project every row is disabled with `읽기 전용`.
- **전환 문구:**
  - `대략` (auto text = place only) is the scene's own precision, not inherited uncertainty.
  - A 회상 auto text with no clock or band uses the year (`하층 거리 · 1195`).
  - Place + no time gives the place only.
  - A direct text without a snapshot is never stale.
  - The snapshot is the story-day range plus minutes after 04:00, as in the fixture.
  - A stale place change wins over a time change in the title.
  - Stale time text shows only the part that changed: `약 N일째` and/or `06:10`, a band word, or `하루`.
- **Pre-reveal place:** checked for `자동` mode only. A place counts as visible when any of its pins on a live map, or its region, is visible at the scene's transition position. A place without pins is never flagged.
- **Bulk:**
  - Scope is the chapter (Q23).
  - Empty means no place or no 때; 회상 counts as a 때.
  - Place options are sorted by scene count in scope (desc), then place order.
  - `모두 같은 때`: every other selected scene gets `<first> 기준 · 같은 때`.
  - Band `그대로` keeps the band and clock. A picked band keeps the clock only when it lies inside that band. `비워 둠` clears both.
  - With no earlier timed scene, the result is `직접` from story day 1.
  - Overwriting a scene with dependents shows no per-scene cascade (Q24).
  - Base notes: `직전 X · <short label>` when X was timed before the sheet, `직전 X` when it gets its 때 in the same sheet, `X 기준 · 직전 씬 때 없음` / `직전 씬 회상` otherwise.
- **Suggestions:**
  - Proposals only for scenes lacking a place or 때, and only when they would fill one.
  - Sources in order:
    1. A background cue whose asset links to a place. The band is the parenthesised word of the asset name.
    2. The 직접 transition text, only in 직접 mode. Its first ` · ` fragment becomes `새 장소` when it is no known place.
    3. The first body sentence that holds a time phrase (narrowing logged under Spec conflicts).
  - Lexicon: `같은 날`, `다음 날`/`이튿날`, `이틀 뒤`, `사흘 뒤`, `며칠 뒤` (+5~9), the band words.
  - Matching follows the rule 21 particle list (one or two chained). Places need ≥ 2 syllables; band words may be 1 syllable.
  - `넣기` only fills empty values, with `대략` by default. A new place is a card without a pin.
- **Search:**
  - Section order follows the chips; 장소 · 세력 · 사건 appear under `전체` only.
  - The header count is shown only when a section has more than one item.
  - One line per block: the sentence of the first hit, with every hit inside it boxed. Blocks of ≤ 40 characters are one sentence.
  - Speaker label is the character name; `화자 없음` when there is none.
  - `숨긴 항목 제외` shows while locked whenever the project has any hidden item, whether or not one matched, so nothing leaks.
  - Similar names: syllable edit distance ≤ 1, or the same first two syllables.
  - The term sub's `장소:` part lists the term's `관련` places.
  - `#043` lists that block of the current episode.
- **Char count (Q4):** grapheme clusters of game-output text. 괄호 지문 text counts without its brackets; breaks, memos and direction cues do not count. `오늘 쓴 글자` = Σ max(0, now − day baseline). A block created today without a baseline counts in full; others count 0.
- **Unwritten episode row:** meta `—` in `ink3`; the title stays when the episode has one, `미정` only when untitled (STO-03 keeps `미정` for untitled units; STO-04's board shows only untitled ones).
- **Status:** a scene is `완료` when it or its episode carries `doneMarked` (the fixture marks episodes). An episode is `완료` when it is marked, or when every scene with content is.
- **Home:** the current chapter is the chapter of the last edit, else the first. `인물` counts non-deleted characters. `이어 쓰기` in a locked hidden scene falls back to the last visible block with content.
- **Level count (Q1):** locked once any live unit below the top level exists.
- **Acknowledgements:**
  - One per (check type, subject keys). Acknowledging again replaces a void one.
  - The kind follows `CheckType.ACKABLE_AS`, else `맞음`.
  - `dropForSubjects` removes acknowledgements whose subject was deleted (CHK-05).
  - The order notice fingerprint is the time difference, so a cascade that moves both scenes keeps the `맞음`.
- **점검 센터:**
  - Time and transition rows use the filter `이동` (Q25).
  - Chips show counts only when non-zero, except `전체`.
  - Locked hidden items collapse into one row at the top of every filter.
  - `마지막 확인` uses `방금` unless the caller passes another label.

## Spec conflicts

- **T-22-04(f) flashback head word order:** 10-script T-22-04 wants `씬 2 · 회상 · 하층 구역 · 1195` (rule 22 table); STO-24 / `RTFlashback` want `씬 2 · 하층 거리 · 회상 · 1195` (SCR Q-22(b)). Implemented STO-24's order. `TransitionTest.t2204f_flashbackHeadRule22Order` is `@Ignore`. F4(c) (`약 7일째` vs `1일째`) is blocked in the spec; the engine gives `씬 1 · 잿더미 시장 · 1일째 낮` (rule 11 §6).
- **BLK-01 `장소 · 때 빈 것 12`:** the fixture's 1장 has 31 scenes, 22 with a place and 19 with 때 (as BLK-01 states). 1-5 씬 3 and 1-10 씬 1 have 때 but no place, so 14 scenes lack a place or a 때 (12 lack 때). `BulkTest.blk01_emptyChipAsDrawn` is `@Ignore`; `blk01_emptyCountOfTheFixture` asserts 14.
- **SUG-02 place alias (resolved by the model field):** the case gives place 송전탑 폐허 the alias 송전탑. `core:model` now has `Place.aliases` (written only when not empty), and `Suggestions` matches each live place's name, its `aliases` and any `placeAliases` passed in (merged, duplicates dropped). `PlaceAliasSuggestionTest.sug02_bodySentenceAsDrawnWithTheAliasInTheModel` runs SUG-02 as drawn with the alias on the place; `modelAliasesAndPassedAliasesMerge` covers the merge. The example fixture's 송전탑 폐허 still has no alias, so `BulkTest.sug02_bodySentenceAsDrawn` stays `@Ignore` (left in place, unedited) and `sug02_bodySentenceWithAnAlias` still passes the alias through `Suggestions(placeAliases = …)`.
- **SRCH-01 term sub `첫 공개 1-7 #044 · 장소: 하층 구역`:** the fixture's 오리지늄 등불 relates to no place (관련 = 등불 결사, 대정전). 하층 구역 is a map region, not a `Place`. The engine lists related places: `termSubListsRelatedPlaces`. `SearchTest.srch01_termSubAsDrawn` is `@Ignore`.
- **Fixture R vs the example fixture:** 20-story-time Fixture R gives R3 = `등불지기장`; `ExampleProject` has `등불 결사 · 등불지기장`. `LensTest` builds Fixture R as the example plus that one value (`Fx.fixtureR()`). R5 (마음 + 호칭) is split into R5 and R5A in the fixture; the tests read both slots.
- **Suggestions from body text (open question):** STO-34 source (3) reads "the scene's game-output body text"; this module only takes a body sentence that holds a time phrase, and a place found in that same sentence. With the literal reading (a place mention alone is a proposal), 1-3 씬 1's narration `대신 결사의 문으로 가는 길을 알려 주었다` would add a fourth scene and `제안 있음` would read 4, not BLK-01's 3. With this rule the fixture's three are 1-4 씬 1 (배경 결사의 문(아침)), 1-6 씬 2 (배경 잿더미 시장(밤)) and 1-8 씬 2 (직접 `기록실 · 같은 날 밤`); the spec's illustrative three are 1-6 씬 2, 1-8 씬 1 and 1-8 씬 2. Needs a product decision.
- **Given deltas used (not conflicts):**
  - BLK-02 / SUG-02 clear 때 on 1-7 씬 4 and 1-8 씬 1, and put the narration into 1-8 #001.
  - SUG-03 trashes the 기록실 card.
  - SRCH-05 restores 노아 and 1-6 씬 5 from the trash and hides them.
  - STR-02 titles 1-7 씬 1 `새벽 순찰` and gives 씬 1 and 씬 2 one narration block of 1,820 and 2,104 characters.

## Requests

- ~~**core:model:** add `aliases: List<String>` to `Place`~~ Done (2026-11-22): `Place.aliases` exists and `Suggestions` reads it by default. 00-domain-model §2.6 does not list the field yet (spec owner).
- **domain:checks:** build `CheckEntry`s from every area's items and use `CheckCenter` / `Acks` for counts, order, collapse and acknowledgements. This module emits `TravelWarning`, `NoPlaceItem`, `NoScaleItem`, `OrderNotice`, `ClockBandNotice`, `StaleTransition` and `PlaceBeforeReveal`; each has `checkType`, `subjectKeys` and `fingerprint` (`AckSubject`).
- **Fixture:** 하르트's `ACK_TRAVEL_HART` stores the real `TravelCheck(doc).warningFor(HART, 1-7 씬 2, 1-7 씬 3)!!.fingerprint`, so the ▲ is acknowledged and the `장소와 때` sheet of 1-7 씬 3 shows no travel block. `SceneWhereTest.sceneWhereTravelBlockForS9` checks the block after reverting that acknowledgement. If `Fingerprints` parts change, the fixture's stored value must be recomputed.
