# domain:checks

Package `io.github.lenticualrstone.civilight.domain.checks` (pure Kotlin JVM over `core:model`, using `domain:story`, `domain:script`, `domain:cast`, `domain:world` and `domain:trash`). The module adds no check rules of its own except row 19. It collects the items each area computes, maps them to the STO-40 registry, and runs the 점검 센터 list and acknowledgements on top of `domain:story`'s `CheckCenter` / `Acks`.

`tools/gw :domain:checks:test` passes (exit 0): 47 tests, 47 passed, 0 failed, 0 ignored.

## Done

Sources are in `domain/checks/src/main/kotlin/io/github/lenticualrstone/civilight/domain/checks/`. Tests are in the matching `src/test` folder.

**Public API**

- `CheckInputs(now, zone, travel: TravelSettings, hiddenUnlocked, candidates: CandidateIndex?, recorder: RecorderLibrary?)`, with `CheckInputs.of(AppSettings, …)` for the STO-42 switches.
- `ProjectChecks.run(doc, inputs): CheckReport` runs every row. `ProjectChecks.check(doc, inputs, checkType)` and `CheckDefinition.compute(doc, inputs)` run one row's check function. Engines shared by several types (term queries, travel check, transitions) run once per run. The report holds `results` (open and acknowledged), `open`, `acknowledged`, `entries` (story `CheckEntry`s), `count(filter)`, `inFilter`, `ofType`, `warningCount` and `scanning` (`점검 중`).
- `CheckResult` is one item. It carries `checkType`, `subjectKeys`, `fingerprint` (each in the owning area's scheme), `title`, `tag`, `location`, `body` with `highlight`, `detail`, `flag`, `actions: List<FixAction(label, kind: FixKind, target, option, days, modeKey)>`, `storyOrder`, `lockedHidden`, `group`, `nestKey`/`nestUnder`, `cannotCheck` (`점검 못 함`) and `travel` (the STO-30 card data). Use `entry()` to get a story `CheckEntry`.
- `CheckRegistry.all` holds the 20 `CheckDefinition`s (number, id, title, buttons, area, setting, marker, filter, ack kind). `CheckRegistry.ackLabel` returns `맞음` · `그대로` · `이대로 두기`.
- `CheckCenterView.header(report)` gives the chips and the `점검 중` label. `rows(report, filter)` gives `Item` (with sub-rows), `Group` or `HiddenGroup`. `empty(report)` gives `EmptyCheck`.
- `CheckAcks` handles acknowledgements:
  - `acknowledge` (refuses types that cannot be acknowledged)
  - `revert`
  - `find`
  - `voided` and `prune`
  - `dropForSubjects`
- `CheckCopy` holds the UI strings.

**Features**

| Feature | What | Files | Tests |
| --- | --- | --- | --- |
| STO-40 registry | All 20 rows with id, number, marker (`▲`/`◇`, from story `CheckCenter.markerOf`), filter (`CheckCenter.filterOf`), ack kind (`CheckType.ACKABLE_AS`), buttons, emitting area, and a pure check function per row (`compute`) | `CheckRegistry.kt`, `CheckCopy.kt`, `sources/Emitters.kt` | `CheckCenterTest.registryListsEveryCheckTypeInOrder`, `FixtureChecksTest.everyRegistryRowAgainstTheFixture` (20-row expected-count table) |
| Rows 1–3 `용어 공개` (WLD-28) | Built from world `TermQueries.checks()` with hidden terms open; masking applied afterwards. Gives `▲ 공개 전 사용` (subject `[linkKey]`, `TermFingerprints`), the planned-term group `잿불의 서약 · ▲ n`, `◇ 공개 전 후보` sub-rows under their term's `▲`, and `◇ 첫 공개 없음` with the reveal chip | `sources/TermSource.kt` | `TermChecksTest` (7), `FixtureChecksTest` |
| Rows 4, 6, 7, 8 (SCR-62) | `▲ 화자 없음` and `▲ 보이스 ID 겹침` from `ScriptChecks`. `◇ 형식과 다른 ID` is one item per fixed ID, using script's `voiceFormatFingerprint`. `◇ 근거 지워진 변화` is one item per orphaned or kept record, using `Anchors.fingerprintOf`. Rows 6 and 8 are grouped (`◇ 근거 지워진 변화 3` + `모두 지우기`) | `sources/ScriptSource.kt` | `ScriptChecksTest` (6) incl. CHK-04 (real delete of #043), CHK-05 |
| Row 5 (CST-14) and row 19 (R19 §6) | `▲ 보이스 비어 있음` per live character, from cast `VoiceLines.emptyCheck`. `◇ 숨긴 인물이 보이는 씬에 나옴 · 씬 3` is computed here, with `씬도 숨기기 · 그대로` | `sources/CastSource.kt` | `CastChecksTest` TC-19-05, TC-19-09 (+2) |
| Row 9 (CST-42) | `◇ 지운 인물 · 노아 · 3일 남음 · 대사 14 · 씬 등장 2 · @ 3 · 레코더 1` and `영구 삭제 · 노아`, from trash `DeletedItems.rows`. It is the only source of deleted rows. Fingerprint `Fixes.deletedItemFingerprint` | `sources/TrashSource.kt` | `TrashChecksTest` TC-20-04, TC-20-15, TC-20-28, purge |
| Rows 10–14 (STO-29/30) | Travel `▲` from story `TravelCheck`: the `이동 시간 부족` card (route line, bars, detail) and the compact rows `같은 때 두 장소 · 세라 \| … · 둘 다 2일째 아침` / `동시에 두 장소`, with time and mode chips and `맞음`. `◇ 장소 없음 · 씬 n개` is one row. `◇ 축척 없음 · 점검 못 함` is one row per scene pair with `cannotCheck` | `sources/TravelSource.kt` | `TravelChecksTest` TRV-04/05/06/07, TRV-09/10 (rows 11–12 on 하르트's pair), TRV-12, CHK-06 |
| Rows 15–18 | `◇ 시각과 시간대가 다름` and `◇ 순서와 때가 다름` from story `OrderChecks`. The transition rows come from story `TransitionChecks` | `sources/TimeSource.kt` | `TimeChecksTest` ORD-01/02/03 (+3) |
| Row 20, STO-39 / CNV-17 list | `▲ 숨긴 항목 관련 N · 지문으로 보기` (story `CheckCenter` collapse). Chips `전체 N` · … (counts per filter, dimmed while `점검 중`). Order is ▲ before ◇, then story order. Also groups, sub-rows, `맞다고 표시한 것` rows with `되돌리기`, and the empty state `어긋난 곳 없음` | `CheckCenterView.kt`, `ProjectChecks.kt` | `CheckCenterTest` CHK-03, `FixtureChecksTest` CHK-01, CHK-02, `TermChecksTest` scanning / hidden |
| STO-41 acknowledgements | `맞음` / `그대로` go through story `Acks`. `이대로 두기` on a record goes through script `Anchors.keepHere` (record `kept`); on a deleted item it goes through trash `Fixes.keep`. `되돌리기` puts a kept record back to `근거 지워짐` with its pre-relocation anchor, so restoring the block later re-attaches it. Void acknowledgements (same subject, other fingerprint) are listed and pruned; `deleted.item` is exempt | `CheckAcks.kt` | T-CNV-24, ORD-03, TRV-07, CHK-04, CHK-05, CHK-07 (file round trip), `pruneDeletesVoidAcknowledgementsOnly` (one fixture fingerprint edited to a stale value: only that acknowledgement is voided and pruned) |
| STO-42 switches | `이동 점검` and `장소 없는 씬 알림` from `AppSettings` | `CheckInputs.kt` | CHK-06, `inputsFollowTheAppSettings` |

**Example project at 최신 (default settings)**

| State | Open | `맞다고 표시한 것` |
| --- | --- | --- |
| As built (the fixture's two acknowledgements carry the real fingerprints) | 8: ▲ 공개 전 사용 1-4 씬 2, ◇ 공개 전 후보 ×3 (1-2 씬 1, 1-3 씬 2, 1-6 씬 1), ▲ 화자 없음 1-7 씬 2, ▲ 보이스 비어 있음 리엔, ◇ 지운 인물 · 노아, ◇ 장소 없음 · 씬 2개 (1-5 씬 3 · 1-10 씬 1). Chips `전체 8 · 용어 공개 4 · 화자 1 · 이동 1 · 보이스 1 · 변화 · 지운 항목 1 · 맞다고 표시한 것 2` | 2: ▲ 이동 시간 부족 하르트 (`ACK_TRAVEL_HART`), ◇ 시각과 시간대가 다름 1-5 씬 1 (`ACK_CLOCK_BAND`) |
| `장소 없는 씬 알림` off | 7 (fixture.md conflict 23) | 2 |
| CHK-01 Givens (state A: no alias 빛돌; no `◇ 장소 없음`) | 5 (`전체 5`) | 2 |

No other check fires on the fixture: no other travel `▲`, no order mismatch, no stale transition, no place before reveal, no voice-ID row, no orphaned record. This confirms the fixture's hand calculation.

## Partial

- **Screens.** CNV-17 and STO-39 / STO-43 layouts, the travel card drawing and the unfolded panes are UI. This module gives the rows, chips, actions and copy.
- **Fix actions.** Rows carry typed `FixAction`s. Their effects are the owners' functions, routed by the UI:
  - script `Anchors.anchorToBlock` / `trashRecord`, `VoiceIds.replaceWithNew`, `BlockOps.changeSpeaker` / `changeKind`
  - world `TermEdits.link` / `reject` / reveal moves
  - trash `Fixes.replaceSpeaker` and `RestoreOps.restore`
  - story `TravelCheck.timeChipPreview` / `applyModeChip` and `Transitions.toAuto` / `writeDirect`
  - hiding a scene
- **`점검 중` timing.** The flag follows the `CandidateIndex` the caller passes (`CandidateIndex.pending()` while the world `RescanScheduler` runs). Scheduling the scan on project open or restore is the app's job.

## Defaults used

- **Counting.** `전체` and every chip count items, not drawn rows: a group `◇ 근거 지워진 변화 3` counts 3, and nested `◇ 공개 전 후보` sub-rows count each. This matches world's `totalCount`. Locked hidden items still count (CNV Q-CK-03), like story `CheckCenter.count`.
- **Sub-rows.** A `◇ 공개 전 후보` hangs under the first open `▲ 공개 전 사용` row of the same term. If no such row exists, the candidate is its own row. Members of a planned-term group are never parents. `맞다고 표시한 것` is a flat list with only `되돌리기` per row.
- **Groups.** These types are one item per subject, folded into one row at the first member's place:
  - `◇ 형식과 다른 ID n` (per fixed ID, so `그대로` works per block)
  - `◇ 근거 지워진 변화 n` (per record, plus `모두 지우기` when n > 1)
  - planned terms `<용어> · ▲ n`
- **`◇ 첫 공개 없음`.** Location `용어 · <name>`, body `<name> · 쓰인 곳 N`. Buttons: world's chip `<위치>을 첫 공개로`, then `원고로 가기`.
- **`◇ 형식과 다른 ID`.** Body `<fixed> → <automatic>`, detail `보이스 ID가 화자 · 에피소드와 다름`, location `1-1 씬 1 · #002`.
- **`◇ 근거 지워진 변화`.**
  - Body `◆ 관계 리엔 → 하르트 · 근거 #043 지워짐`, or `◇ 첫 공개 <용어> · …` for reveals. The location is where the record sits now (`1-7 씬 3 · #044`).
  - A `kept` record is listed with its keep acknowledgement. If that acknowledgement is missing or void, the record is open again.
  - `Anchors.keepHere` clears the pre-relocation anchor. The keep acknowledgement written here stores it in `ext.originalAnchor`. `되돌리기` sets the record back to `orphaned` with that anchor, so `originalAnchor` is non-null iff orphaned. If the acknowledgement was written elsewhere without it, the current anchor is used.
- **Row 19.**
  - It fires for every live hidden character who appears (speaker or silent cast, `Appearance.all`) in a live scene whose unit chain is not hidden.
  - Title `◇ 숨긴 인물이 보이는 씬에 나옴 · 씬 2`, location `1-6 씬 2`, body = the character name.
  - While locked it collapses with the other hidden rows.
- **Hidden.** A row is `lockedHidden` while the gate is closed and any of these is hidden:
  - its subject term, character or place
  - its scene, or any unit above it
  - the scene of its block
  - a record about a hidden item (`CastHiding.isAboutHidden`)
  - all scenes of `◇ 장소 없음`
- **`◇ 축척 없음`.** One row per scene pair, not per character. The names go in `tag`, the action `축척 맞추기` targets the map, and `cannotCheck = true`.
- **Travel rows.** `tag` = character, `location` = earlier scene, sorted by the later scene.
  - `이동 시간 부족`: `body` = route line, `detail` = the card detail (+ `직선으로도 부족 · 경로 없음`). A range-driven shortage has detail `대략인데도 모자람 · <name> · <route> · 가장 길게 잡아도 부족`.
  - `같은 때 두 장소` / `동시에 두 장소`: `body` = the compact line after the name (`1-7 씬 2 상층 관문 · 씬 3 결사의 문 · 둘 다 …`), `detail` = the card detail.
- **Time rows.**
  - `순서와 때가 다름` reads `<scene> · 직전 씬보다 앞 · 9시간 전` with `원고로 가기 · 맞음`.
  - `시각과 시간대가 다름` reads `<scene> · 새벽 06:20` with `맞음`.
- **Empty state.** It is not shown while `점검 중`, so `어긋난 곳 없음` never appears before the scan ends.
- **Void acknowledgements.** An acknowledgement is void only when an item with the same type and subjects fires now with another fingerprint. If the item does not fire (switched off, scan pending, fixed), nothing is voided. `deleted.item` acknowledgements are never voided here; the trash area deletes them on restore or when no reference remains.
- **Acknowledging.** Types not in `CheckType.ACKABLE_AS` are refused, so `▲ 화자 없음` and `▲ 보이스 비어 있음` cannot be hidden. `deleted.item` uses trash `Fixes.keep`, so the subject is `[itemKey]`.
- **Candidate index.** When `CheckInputs.candidates` is null, a full `CandidateIndex.build` runs with hidden terms searchable. Their rows are then masked, never shown while locked.

## Spec conflicts

- **CHK-01 / T-CNV-23 `전체 5` vs the fixture.** The fixture raises 8 open items as built (2 acknowledged), and 7 without `◇ 장소 없음`.
  - `chk01_allCountsBothMarkers` reaches 5 with the case's own world (WLD state A: alias `빛돌` removed, fixture.md #8) and `장소 없는 씬 알림` off.
  - `withoutNoPlaceNoticeTheFixtureRaisesSeven` asserts fixture.md #23's 7.
- **`◇ 장소 없음` scenes.** fixture.md expects it for 1-5 씬 3 ("in a character's track"). Story's `TravelCheck.noPlace` lists every timed scene without a place (its `ED`), so it also lists 1-10 씬 1, which has no cast: `◇ 장소 없음 · 씬 2개`. I kept the owner's rule.
- **Transition rows 17–18 exist twice.**
  - SCR `ScriptChecks.transitionStale` / `transitionPlaceBeforeReveal` use filter `씬` and fingerprint `transition:…`. They also leave out items acknowledged under their own fingerprint.
  - Story `TransitionChecks` uses filter `이동` (Q25) and a `sha256-…` fingerprint.
  - I used story's (C-24: STO-40 wins). A `맞음` stored from 점검 센터 is therefore not recognised by `ScriptChecks.transitionStale` (see Requests).
- **Row 6 title.** STO-40 has `◇ 형식과 다른 ID 3`; SCR-62 has `◇ 보이스 ID가 화자 · 에피소드와 다름` with item text `형식과 다른 ID {n}`. The group row uses STO-40's title and each item carries SCR's phrase as detail.
- **Rows 11–12 buttons.** CNV-17 keeps MAIN's `때 고치기 · 장소 고치기 · 맞음`; STO-40 uses the R18 §5 chips. I used STO-40 (C-24).
- **TC-20-04 / board `Check` buttons** `화자 한꺼번에 바꾸기 · 되살리기 · 이대로 두기`. Trash's row adds `@ 한꺼번에 바꾸기` for 노아's 3 `@` links (CST-42 per-kind buttons). I passed trash's list through; the test asserts the first and the last two.
- **`대사 블록 n개` on `▲ 화자 없음`.** SCR-62 counts all speakerless dialogue blocks of the project on every row. Each row is one block, so a per-scene count may have been meant. I kept script's value.

## Requests

- **Fixture owner (`core:model` `ExampleExtras`).** The fingerprint scheme is the owning engine's (`domain:story` `Fingerprints.of(...)` over resolved times, places and mode). It cannot be computed in `core:model`, so the fixture stores the values this module computed for the example project as built:
  - `ACK_TRAVEL_HART` (`travel.shortage` [하르트, 1-7 씬 2, 1-7 씬 3]): `sha256-1f4bd89577870ca8310efdafc13f2015eb611d792c8d1bfe4310cc4f5e192f26`
  - `ACK_CLOCK_BAND` (`time.clockBandMismatch` [1-5 씬 1]): `sha256-abbd7af894cac54d25b10f6cd6059928464eec05286a01f9383afba6a3f65d11`

  With them the fixture raises 8 open items and `맞다고 표시한 것 2`, and tests use `Fx.doc()` as is. If the story engine changes its fingerprint parts, or a fixture edit changes those inputs, these values change too: `FixtureChecksTest.fixtureRaisesEightOpenItemsAndTwoAcknowledged` (`voided` empty, 2 acknowledged) fails until the fixture is updated.
- **domain:script.**
  - `ScriptChecks.compute` drops acknowledged items. Its `speakerMissing`, `anchorDeleted` and `voiceDuplicates` items have empty fingerprints, and `anchorDeleted` / `voiceFormatDiffers` fold all subjects into one item. This module therefore rebuilds rows 6 and 8 per subject, from `VoiceIds.differsFromFormat`, `ScriptChecks.voiceFormatFingerprint` and `Anchors.fingerprintOf`.
  - Please keep those three public.
  - Consider dropping the script's own transition checks in favour of `domain:story` `TransitionChecks`. If you keep them, use its fingerprints so one `맞음` is read the same everywhere.
- **domain:story.** Please keep `CheckCenter` (`markerOf`, `filterOf`, `count`, `chips`, `rows`, `empty`), `Acks`, `CheckEntry` and the `AckSubject` items (`TravelWarning`, `OrderNotice`, `ClockBandNotice`, `StaleTransition`, `PlaceBeforeReveal`) stable. This module depends on them.
- **domain:world / domain:trash.** Same for `TermQueries.checks()` / `TermCheckRow` / `TermFingerprints` and `DeletedItems.rows` / `Fixes.keep` / `Fixes.deletedItemFingerprint`.
