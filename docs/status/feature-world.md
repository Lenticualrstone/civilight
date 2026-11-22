# feature:world

All three batches done. Batch 1: WLD-01 02 03 04 06 08 09 10 11 12 17 18 19. Batch 2: WLD-05 07 13 14 15 16 20 21 22 28 29 31 32 (33, 34 partly). Batch 3 (2026-11-22): WLD-23 24 25 26 27 30 33 34 35 63 64 68 69, the `TimelineMap` and `TravelModes` routes (WLD-64, WLD-47) and the shared sheet `LinkCandidatesSheet`. Every world route and both WORLD shared sheets are registered now.

`tools/gw :feature:world:compileDebugKotlin :feature:world:testDebugUnitTest` exits 0: 135 JVM tests in 22 classes, all passing. `tools/gw :domain:world:test` exits 0: 123 tests, 1 ignored (pre-existing spec conflict WLD-T60), 0 failures (the count includes the map lane's tests in the `map` package). Nothing was run on a device or emulator; every screen is checked only by compiling and by the tests of its state builder.

## Architecture

Package `io.github.lenticualrstone.civilight.feature.world` (`src/main/java/...`). Composables stay thin: each screen has a pure `…State.build(doc, …)` (tested on `ExampleProject.build()`), a view model that combines the document, the hidden-item unlock and the candidate index into that state, and a screen that draws it.

- **`WorldEntries.kt`** (`worldEntries`, shell.md §1): registers every world route (`World`, `TermCard`, `PlaceCard`, `FactionCard`, `Timeline`, `EventDetail`, `WorldCalendar`, `TimelineMap`, `TravelModes`), both WORLD shared sheets (`EventCreateSheet`, `LinkCandidatesSheet`), and the feature-local reveal sheets (`registerRevealSheets`, so `공개 시점 옮기기` also shows over other areas' screens).
- **`WorldCopy`**: all UI copy this module writes. Strings the domain already builds (`공개됨 · 최신 · 1-7`, `쓰인 곳 · 2 · 후보 1`, `숨긴 장소 N`, `등장 1-2 · 1-7 외 4` …) are taken from `domain:world` and not repeated. Missing copy = `TODO_COPY_WORLD_*` (none left since the motion pass: see below). `WorldCopyLintTest` runs `CopyLint` over every constant and formatted function. Add new formatted functions to its `formatted()` list.
- **`common/`**: shared by every world screen.
  - `WorldViewModel`: base on `ProjectViewModel`, holding `readOnly`, `hiddenOpen` (rule 19, `UnlockController.itemsOpen`), `editContext()` and `actionContext()` (`domain:actions` `ActionContext`, same keys and clock). Edits go through `apply(label) { doc -> doc' }`, one undo step each. Also `rename`, `setDescription` / `setDescriptionDoc`, `backToLatest`, `undoLast`, and (batch 2) `setHidden`, `impact`, `deleteItem`, `restoreItem`, `merge` (returns `Merged(toast, undo)`; the undo also reverts the Life Recorder links).
  - `WorldEnv`: the app-scoped parts of `FeatureDeps` a view model may keep, now with the `RecorderStore` (merge moves recorder links) and the zone.
  - `ItemLifecycle` (pure): hide flag, impact, delete (`TrashOps`), restore of the item's own entry (`RestoreOps`, hidden needs auth), `restorable`, `sameName` (`Fixes.sameNameNotice`) for places, terms, factions and events.
  - `MergePlan` (pure): `deleteSheet(impact)` from `Messages.impactSheet`; `canMerge`, `mergeSheet(doc, kind, merged, target, choices)` from `MergeActions.itemPreview` plus `MergeRows.routeEndRow` and the term `첫 공개` comparison row; `toggle`; `refParts`.
  - `LifecycleSheets`: feature-local `DeleteSheet`, `MergePickSheet`, `MergeSheet` and `rememberDeleteAction` (no sheet when nothing would be affected). `ItemSheets.itemSheet` dispatches them.
  - `CardExtras`: `CardImages` (pure slot reading + off-main-thread decode with the 16:9 crop), `CardImageSlot` (image or initial letter, `이미지 고르기` → canvas's `ImagePickSheet`), `WorldGoneCard` (dotted label + `되살리기`), `SameNameLine` (`◇ 같은 이름 2 · …` + `합치기`).
  - `TermIndexes`: one `CandidateIndex` per open project, shared by list, card and the bulk sheet, ref-counted with `acquire` / `release`. Batch 3 (WLD-26): each new document is diffed against the last by `RescanPlan` / `RescanPlans.of` (pure: `Full` on the first input or an access / particle change, `Incremental(terms, spots)`, `None`) and handed to `domain:world` `RescanScheduler` (typed block rescanned alone after 0.5 s, changed term swept at once, full scan pending only the first time). All scans of one project run on `dispatcher.limitedParallelism(1)`, so results never race. It never writes the document: stale `아님` records are hidden at read time (`StaleRejections.stale`) and released inside the world's own term writes (`TermCardViewModel.termWrite`, `LinkCandidatesViewModel.apply`).
  - `CardParts` (art slot, head, fixed row, field section, locked / gone card) and `CardModels` (`CardAccess`: `Open · Locked · Gone`, `FixedRowUi`, `ItemRefsText`).
  - `FieldRows`: project fields at the lens, with `◆` and the `빈 항목 N · 보기` split.
  - `RichText`: `CardText` draws card text with link decorations. `RichTextEdit` edits text and keeps runs: `apply` diffs one change, `insertLink` handles `@`.
  - `ItemEdits` and `RelatedOptions`: name, description, `관련`.
  - `ItemSheets`: `⋯` menu (`이름 바꾸기 · 설명 고치기 · (용어) 아님으로 둔 것 · 숨기기 / 숨김 풀기 · 합치기 · 지우기`), `이름 바꾸기`, `설명 고치기` with the `@` tool. `DescriptionDraft` is the editor draft.
  - `LensBanner`: the past-lens banner.
  - `WorldTargets` / `openFromWorld`: where rows lead. World cards open with `openDetail`, everything else with `push`.
  - `ScreenParts`: back button per pane role, command bar, selected card key.
- **`list/`**: `WorldScreen`, `WorldListViewModel`, `WorldListState` (`WorldListItem`: Section · Row · Lock · Empty), `WorldTabs` / `WorldSegment`, and `CreateItemSheet` (feature-local key). The `지도` segment pushes feature:map's `MapList`. A tab is remembered per project in `_app/view.json › worldTab`. `WorldCardView`: `WorldCardRows` (pure: one card row per list section; the view per screen in `AppSettings.viewByScreen` under `world.terms · world.places · world.factions`) and `WorldCardRow` (snapping `LazyRow`, 8 % parallax, tick on snap, none under reduced motion). `World.createTerm` opens `용어 만들기` once (saveable `createHandled`).
- **`term/` (batch 3)**: `LinkCandidatesState` (pure `BulkSheetState` `Ready · Pending · Empty`, `BulkSheetUi` from `domain:world` `BulkSheet`; saved state = mode + ids of unchecked rows, so rows are rebuilt from the current index; `apply` → `BulkApply` `Done · Stale · Nothing`), `LinkCandidatesSheet` (`LinkCandidatesViewModel` keyed `world.linkCandidates.<p>`, holds its own `TermIndexes` handle; `LinkCandidatesContent`; the card opens the feature-local `BulkCandidatesSheet(termKey, mode)`, other areas the shared `LinkCandidatesSheet` which starts in `모두 잇기`). `TermCardUi.bulkActions` draws `모두 잇기 · 모두 아님` on the 후보 group header; `UsageRowUi.linkKey` / `canMarkIntended` draw `맞음` next to the `▲` marker (`TermCardViewModel.markIntended`).
- **`term/`**: `TermCardScreen` / `TermCardViewModel` / `TermCardState` (`UsageRowUi`, `UsageGroupUi`, `MarkerUi` with `warn` and `dotted`), `UsageRows` (row view with inline box, place line, candidate `잇기 · 아님`, `▲` as a button to the check center, dotted `◇ 근거 지워짐`), `RevealOptions` (pure), `RevealSheets` (self-contained `공개 시점 옮기기` flow that reads `currentProjectStore` and its own keyed `RevealEditorViewModel`; each step replaces the one before; `revealSheet(key)` for local use and `registerRevealSheets(deps)`), `TermSheets` (`관련`, `별칭` (`AliasSheet`), `아님으로 둔 것` (`RejectionsSheet`)), `AliasesAndRejections` (pure `AliasEditor`: add / remove with `후보 N 새로 찾음` from `CandidateIndex.rescanTerm` against the index the card shows, null while pending; `RejectionRows`).
- **`place/`**: `PlaceCardScreen` (tabs `개요 · 이곳의 시간 · 지도`), `PlaceCardState`, `PlaceCardViewModel` (`지역`), `ParentChoices`.
- **`faction/`**: `FactionCardScreen` with its view model, and `FactionCardState`.
- **`time/`** (batch 2): `TimelineState` (pure, over `domain:world` `TimelineBuilder` with the new lock flag; rows, current = last undimmed chapter, view chips, anchor fallback), `TimelineScreen` (+ feature-local `AnchorPickSheet`), `EventDraft` / `EventForms` (pure form rules and save: create world / story event, edit keeps runs through `RichTextEdit`, `시간 없이` keeps stored dates), `EventSheets` (shared `EventCreateSheet` content and feature-local `EventEditSheet`; pickers replace the form inside the sheet; draft is saveable JSON), `EventDetailState` / `EventDetailScreen` (+ `EventMenuSheet`: `고치기 · 지우기`; trashed event with `되살리기`), `WorldTimeState` / `WorldTimeScreen` (route `WorldCalendar`), `TimeViewModels` (`TimelineViewModel`, `EventEditorViewModel` + `rememberEventEditor` for the shared sheet, `EventDetailViewModel`, `WorldTimeViewModel`).
- **`time/` (batch 3)**: `EventChanges` (pure `+ 변화`: `ChangeDraft`, `ChangeStep`, `ValueInput` per field type, `add` = `domain:cast` `TimedValues.addRecord` with `Anchor.WorldEvent`) and `EventChangeSheet` (feature-local, steps replace each other: item picker → field → value → link target); `CalendarDraft` (pure editor state, `problem()`, `toCalendar()`) and `CalendarEditor` (under `세계 달력 만들기` in `WorldTimeScreen`, `저장` → `WorldTimeViewModel.saveCalendar` → `WorldTimeEdits.setCalendar`); `TimelineMapState` / `TimelineMapScreen` (`TimelineMapViewModel`; list | schematic map on inner screens, map above the list on the cover; `CompareMode` `NOW · CHANGE · BOTH` for the region chips); `TimelineState.defaultView` (the project's `때 표기`). The 연표 header has a pin button → `TimelineMap`.
- **`travel/` (batch 3)**: `TravelModesState` (rows, `TravelModeDraft`, `save` over `domain:world` `TravelModeEdits`, which it only calls) and `TravelModesScreen` (`TravelModesViewModel`, feature-local `TravelModeSheet(index?)`).
- **`list/ListCardMotion`** (batch 3, WLD-68): timings of `목록 ↔ 카드` and `GrowIn`; the list's `LazyColumn` items use `animateItem` with them.
- **How to extend:**
  - A new route: a screen package with `…State` + view model + screen, then an `entry<Route>` in `WorldEntries`.
  - A card action: a menu row in `ItemMenuContent` (feature-local sheet keys in `ItemSheets`) and a view-model method calling the domain or `domain:actions`. Deletes go through `domain:trash` and merges through `MergeActions` (DECISIONS §1).
  - Shared sheets: `deps.sheets.register<LinkCandidatesSheet> { … }` in `WorldEntries`. Content of a shared sheet reads `currentProjectStore` and a keyed `entryViewModel(key = "world.<x>.$projectKey")` (see `rememberEventEditor`), never a world screen's view model.
  - (Batch 3 slots, all filled now) WLD-23 group action `모두 잇기 · 모두 아님` goes in `TermCardScreen.termBody` above the `후보` group (domain `card.candidates.action`, `BulkSheet`); WLD-24 `맞음` on the card `▲` row (`UsageRowView` markers); WLD-26 `RescanScheduler` in `TermIndexes.Holder.run`; WLD-30 calendar editor under the `세계 달력 만들기` row of `WorldTimeScreen`; WLD-33 `+ 변화` under `이 사건에서 바뀐 것` in `EventDetailScreen`; WLD-34 already switches views (verify on device); WLD-35 ages come from the domain rows; WLD-63/64 `PlaceCard` tab and `TimelineMap`.

## Done

Pure logic with passing tests (the screens on top are listed under Partial):

- `WorldListState`: WLD-T02, the list half of WLD-T40, the WLD-02 sample sections, lock row, empty state, places with `지역` sections, factions, and tab choice.
- `TermCardState`: WLD-T01, T04, T06, T10, T26, T35, T40, plus fixed rows, `글자 찾기` off, and locked and trashed access.
- `RevealOptions`: link and block rows, WLD-T35 apply and toast, T38 refusal, and a move to 2-5 with the T39 toast pattern (`◇ 첫 공개 1-7 → 2-5 · ▲ 새로 1`).
- `RichTextEdit` / `DescriptionDraft`: links and paragraph keys survive edits, and `@` insertion works.
- `ItemEdits` / `RelatedOptions`: create, rename, description, `관련`.
- `PlaceCardState`, `FieldRows`, `ParentChoices`, `FactionCardState`.
- `LensBanner` and `TermIndexes`.
- `StaleIndexTest`: card and list build without error from a newer document and the previous index (b12 shortened, b12 trashed, right after `잇기`). The view models do this while a rescan runs.

- Batch 2 pure logic (all with tests):
  - `TimelineState` (`TimeStatesTest`): WLD-31 rows on the example (`대정전 · 이야기 시작 · 1장 · 1장 · 등불지기 · 리엔, 결사 합류 · 상층 송전망 사고 · 2장 · 잿불`), lens chapter, dimming, `2장에서 공개 예정`; WLD-34 labels (`3일째`, `기준`, chip `기준 사건 · 리엔, 결사 합류`, footer) and the anchor fallback; WLD-T51; WLD-13 masking.
  - `EventForms` / `EventDraft`: create world and story events, calendar month/day checks, edit keeps the kind, links and a date `시간 없이` cannot show.
  - `EventDetailState`: board `TwoTimeline` rows and `이 사건에서 바뀐 것`; delete to the trash and `되살리기`.
  - `WorldTimeState`: the three modes, start year, calendar line, calendar mode before WLD-30.
  - `ItemLifecycle` / `MergePlan` (`LifecycleTest`): WLD-13 hide and lock row; WLD-T43 impact sheet, toast with `▲ 공개 전 사용 1`, dotted card, restore; hidden restore needs auth; TC-20-03 no sheet; WLD-15 same-name notice, place merge sheet and the engine's `합침` entry; WLD-T70 reveal comparison row.
  - `AliasEditor` / `RejectionRows` (`AliasRejectionTest`): WLD-T10 (`후보 2 새로 찾음`, header `쓰인 곳 · 2 · 후보 3`), pending index, refusals, the row's `—`; WLD-T06 list and undo; WLD-T37 dotted `◇ 근거 지워짐`.
  - `WorldCardRows` / `CardImages` (`WorldCardRowsTest`).
- `domain:world` additive (batch 2, `LockedTimelineTest`, `LockedTimelineUnitsTest`): `TimelineBuilder.build(lens, view, anchor, hiddenUnlocked)` (while locked: hidden places and regions `숨긴 항목`, hidden characters' ages left out, a hidden chapter keeps only its code, a story event on a hidden scene reads `숨긴 씬`), `EventDetails.detail(eventKey, hiddenUnlocked)`, `EventMasks`, `WorldTimeEdits` (`setMode`, `setStartYear`, `parseYear`). The old signatures are unchanged and behave as before.
- Cross-lane: shell core pass item 3, `World.createTerm` (feature:story LIB-13 request): the 세계 screen opens `용어 만들기` once on arrival.
- WLD-20 and WLD-21 rules are `domain:world`'s (`TermQueries`, `TermMatcher`, spec cases WLD-T15 … T42 there); this module draws them and its card and list tests cover T01, T02, T04, T10, T37, T40.

- Batch 3 pure logic (all with tests):
  - WLD-26 `RescanPlans` (`RescanPlanTest`): WLD-T72 full scan first, WLD-T47 a typed block rescans only that block and an alias change sweeps only T1, lens / rejections change nothing, unlock is a full scan, `글자 찾기` off is a term change; the shared index rescans b12 only after the 0.5 s pause and sweeps a new alias at once (coroutine test).
  - WLD-23 `LinkCandidatesState` (`BulkAndIntendedTest`): WLD-T11 (title, rows, boxed sentences, footer `고른 3개 · 넣으면 ▲ 공개 전 사용 1 → 4`, `넣기 · 3개`, note, three links in one step, `쓰인 곳 · 5`), WLD-T12, WLD-T13, pending and empty states, a stale row is dropped.
  - WLD-24 (`BulkAndIntendedTest`): WLD-T05 on the card (`맞음` tag, `▲` line no longer a warning, list loses `▲ 앞에서`), never on candidates.
  - WLD-25 (`BulkAndIntendedTest`): WLD-T28 chip after the 11th `아님`, WLD-T31 off and on keep the rejection.
  - WLD-22 lifetime: `아님으로 둔 것` hides stale records; term writes release them (domain `StaleRejections`, WLD-T07 / T08).
  - WLD-30 `CalendarDraft` (`CalendarDraftTest`): WLD-T52 through the editor (`나달 16일`, `해`), problems that stop `저장`, removing rows keeps the start, calendar mode shows the editor before any month exists.
  - WLD-33 `EventChanges` (`EventChangesTest`): offered on world events only, calculated fields left out, `넣기` adds a record anchored on the event and the detail lists `리엔 · 소속 | … → …`, empty / invalid values write nothing.
  - WLD-34 / WLD-35 (`TimelineViewTest`): default view from `때 표기`, WLD-T48 ages on rows (`리엔 12세`, `리엔 17세 · 하르트 ??세`).
  - WLD-64 `TimelineMapState` (`TimelineMapStateTest`): rows in time order, the selected event lights `결사의 문`, WLD-65 chips `1-7 · <point> ◆ · 비교` with the dotted note and added shape, WLD-13 masking of a hidden place.
  - WLD-47 `TravelModesState` (`TravelModesStateTest`): defaults `도보 15 km · 8시간`, editing a default stores the modes once, `직접` adds, refusals.
  - WLD-68 `ListCardMotion` (`ListCardMotionTest`): 160 ms exit, 360 ms grow after it, reduced motion 120 ms fades.
- `domain:world` additive (batch 3, `ManuscriptContractTest`): `term/ManuscriptMarks` (WLD-27 contract: `LinkMark` with `preReveal` / `intended` / `masked`, `CandidateMark` with term name and `후보 · 끊김`, `CandidatePopover` `첫 공개 1-7 · 잇기 시` + `▲ 공개 전 사용`, `SpotMarks.marginWarning`), `term/StaleRejections` (`release(doc, access, spots)` on a fresh scan of the rejection spots, `stale(doc)`), `WorldTimeEdits.setCalendar`, `track/PlaceTimes.rows(placeKey, lens, hiddenUnlocked)` (the old overload = unlocked). Tests WLD-27 ×4, WLD-T07, T08, T52, WLD-63 lock.
- WLD-27 (manuscript display contract) is done as the domain query API above; the script lane renders it (see Requests).
- WLD-69 (세력 사이 관계, P2): v1 stores nothing new and has no screen (spec). The storage shape is reserved in core:model (`Relation` ends `RelationEndKind.FACTION`); nothing to build here.

## Partial

All of these compile and their state is tested. None has been seen on a device.

- **WLD-01 세계 화면 틀**: header (project name, `세계`, lens chip, `+`), segments `장소 · 용어 · 세력 · 지도` (the last pushes `MapList`), hint `공개 시점 전 사용 → 점검 센터` → `CheckCenter(filter = CheckFilter.TERM.name)`, command bar `장소 · 용어 · 세력 찾기` → `ProjectSearch` (mic → `Recording`), the remembered tab, list | card through `openDetail` with the selected row marked, and the 560 column through `CenterColumn`. `목록 | 카드` is WLD-05 (batch 2, below). Left: the node-map placeholder `장소 · 노드 찾기` (map lane), and fold motion (shell CNV-34).
- **WLD-02 용어 목록**: two sections at the lens, right column `첫 공개 1-7 · ◇ 후보 1` / `▲ 앞에서 1회 사용` (warn), candidate numbers dimmed while the first scan runs, `숨긴 용어 N · 지문으로 보기`, and `용어 없음` with `용어 만들기`.
- **WLD-03 장소 목록 / WLD-04 세력 목록**: domain rows (`등장 …`, `지역` sections, `인물 N`), lock rows, empty state. The list is a `LazyColumn`, so 100+ rows stay virtualized.
- **WLD-06 카드 공통**:
  - Fixed rows per kind. Project fields at the lens with `◆`, and `빈 항목 N · 보기` reveals the empty ones.
  - Each field row opens cast's `FieldValueEditSheet`, and `+ 항목` opens `AddFieldSheet`. The cast lane owns both, so `이 값 고치기 · 여기서부터 바뀜` work once cast registers them.
  - `설명 고치기` keeps links and paragraph keys.
  - The image slot is WLD-07 (batch 2, below).
- **WLD-08 용어 카드**:
  - Header, art slot labelled `참고 이미지`, eyebrow, name, and the description with links.
  - Rows `첫 공개` (tap → `공개 시점 옮기기`), `별칭` (read-only), `관련` (tap → tick sheet with 초성 search and a hidden-item row), then project fields.
  - Notices `1음절 · 글자 찾기 안 함` and `첫 공개 없음 · 쓰인 곳 N`, `글자 찾기 꺼짐 · 켜기`, and chips `<위치>을 첫 공개로` and `글자 찾기 끄기`.
  - `쓰인 곳` groups with inline boxes, markers and tags. Out-of-manuscript rows put `페이지 · 하층 구역의 빛` on a line under the sentence. Rows are dimmed after the lens, and a tap opens the manuscript, page, profile or card.
  - Candidate rows draw `◇ 후보 · 끊김` + `잇기 · 아님` as buttons (`TermEdits.link` / `reject`, one undo step and a toast). This is the card part of WLD-22.
  - `⋯` has `이름 바꾸기 · 설명 고치기`.
  - Batch 2 added `⋯ › 아님으로 둔 것 · 숨기기 · 합치기 · 지우기`, the alias editor with `후보 N 새로 찾음`, the image slot and the `◇ 같은 이름` line. Batch 3 added the group action `모두 잇기 · 모두 아님` (WLD-23) and `맞음` on `▲` rows (WLD-24).
- **WLD-09 장소 카드 (full shape)**:
  - Eyebrow `장소 · <지역>`, tabs `개요 · 이곳의 시간 · 지도`.
  - `개요`: description, `등장`, `지역` (picker that excludes the place and its descendants, plus `비우기`), `배경 에셋`, and project fields.
  - `이곳의 시간`: the domain rows with the lens divider. Events open `EventDetail` and scenes open `Script`. Hidden scenes read `숨긴 씬`.
  - `지도`: one row per map holding a pin → `MapView(focusPinKey)`, or `◇ 지도 없음`.
  - Left: the 16:9 map crop around the pin at the top (initial-letter card for now). The map summary sheet and side card (`핀 옮기기 · 장소 카드 열기`) belong to feature:map.
- **WLD-10 세력 카드**: head, description, project fields (`수장`), `이 항목으로 잇는 인물` (→ profile), linked regions and provinces (→ `MapView`), and change events (→ `EventDetail`). Two section titles are TODO_COPY (see Requests). The fixture's `소속` is `짧은 글`, so the example shows no linked characters.
- **WLD-11 만들기 · 이름 바꾸기**:
  - `+` opens `<종류> 만들기` with `이름` (required) and `설명`, then `만들기` opens the new card beside the list. The empty-state button does the same.
  - `⋯ › 이름 바꾸기` never rewrites link text, and the term index rescans on the new name.
  - Left: `◇ 같은 이름 2` (WLD-15) and creation from map pins (feature:map, WLD-41).
- **WLD-12 시점 렌즈**:
  - Lists, cards, fields, usage dimming and `이곳의 시간` all read the lens. The lens chip opens `LensSheet`.
  - At a past lens an editable card shows the `LensEdit` banner `1-5 시점 값 고치는 중 · 고치면 1-5부터 적용` with `최신으로`, which sets the lens to 최신 in `_app/` with no undo step.
  - Left: map, timeline and track lens parts (later batches and the map lane).
- **WLD-17 `@` 연결**:
  - Card text draws links (`숨긴 항목` while locked, dotted for trashed or purged targets). Usage, `▲` and groups come from link runs only.
  - `설명 고치기` has a visible `@` keyboard tool that opens `LinkPickerSheet` (인물 · 용어 · 장소, aliases, hidden row). The pick is inserted at the caret as a new link run. The draft keeps runs while typing, so later edits keep the link and its key.
  - Left: the picker title is TODO_COPY. Links are not drawn inside the editor field (plain text there; they show on the card).
- **WLD-18 첫 공개 기록**:
  - `공개 시점 옮기기` sheet with three ways to set the reveal:
    - (a) the term's manuscript links, the current one selected. Option lines are disabled with `선택지 보기 줄에는 첫 공개를 걸 수 없음`.
    - (b) `원고에서 고르기`: scene picker, then that scene's dialogue, narration, system and choice blocks.
    - (c) `에피소드 · 장으로`: a chapter or episode start, for `공개 예정`.
  - Choosing applies at once with toast `◇ 첫 공개 1-7 → 2-5 · ▲ 새로 1` or `◇ 첫 공개 1-2` and `되돌리기`. The chip `<위치>을 첫 공개로` does the same.
  - Left: the `근거 지워짐` actions `다른 블록에 잇기 · 이대로 두기 · 기록 지우기` (SCR anchor engine; the card shows the domain's `◇ 근거 지워짐` marker), and a block-pick mode inside the script.
- **WLD-19 쓰인 곳 · 세 무리**: header `쓰인 곳 · N · 후보 M`; groups `쓰인 곳 · N`, `후보 · N · 확인 안 함`, `원고 밖 · 메모 · N` in the domain's order; tags `선택지` and `게임 텍스트`; whole sentence with the inline `SEARCH_HIT` box. Dialogue is quoted as on board `Term`.

- **WLD-05 목록 | 카드**: `ViewToggle` `목록 | 카드` on the hint line of each tab, remembered per screen in `설정 › viewByScreen`; card view = one snapping row of 16:9 cards per list section with the list row's label; parallax and the snap tick off under reduced motion. Left: the in-place grow / list-text-exit transition between the two views (the content swaps at once).
- **WLD-07 카드 이미지**: the slot reads the item's `CARD_REFERENCE` image use: a picked blob (decoded off the main thread with its 16:9 crop, rotation and mirror) or the initial-letter card; `이미지 고르기` opens canvas's shared `ImagePickSheet` (placeholder until the canvas lane registers it). Left: presets draw a neutral `ShardArt` (the preset images live in the image area, see Requests); the holo effect is not drawn on cards.
- **WLD-13 숨기기**: `⋯ › 숨기기 / 숨김 풀기` on every card; lists fold hidden items into `숨긴 용어 N` (batch 1); the 연표 and event detail now mask hidden places, characters' ages, related terms and `◆` rows while locked. Left: hidden items on maps (map lane), search (story lane).
- **WLD-14 지우기 · 되살리기**: `⋯ › 지우기` → impact sheet (feature-local, from `Messages.impactSheet`, see Requests) or, with nothing affected, at once; toast `용어 오리지늄 등불 지움 · 참조 … · 점검 센터 · 되돌리기`; the card of a trashed item shows its dotted label and `되살리기` (hidden ones after `지문으로 보기`). Left: `영구 삭제` and the 30-day purge are the trash screen's (`Trash` route, library lane); the per-reference fix actions (`다른 항목에 잇기 · 글로 풀기 …`) are the check center's.
- **WLD-15 합치기**: from `⋯ › 합치기`, the impact sheet's `다른 용어와 합치기 ›`, and the `◇ 같은 이름 2 · …` line on every card: kind picker → comparison sheet (`옮길 참조 · N`, `다른 칸 · N` with a choice per row, term `첫 공개` row, `경로 끝 N · <B> 핀으로`, `빈 칸 채움 · N`, footer) → `MergeActions.mergeItems`, recorder links follow, toast with `되돌리기`, the target card opens. Left: relation-overlap rows (factions have no relations yet, WLD-69).
- **WLD-16 별칭**: the `별칭` row is editable (`—` when empty): sheet with removable chips and a field with `+`; `후보 N 새로 찾음` next to the row after a change (from a rescan of that term against the index the card shows). Since batch 3 an alias change sweeps only that term (WLD-26).
- **WLD-20 ▲**: card markers `▲ 공개(1-7) 전 사용` are buttons to `CheckCenter(TERM)`; dotted `◇ 근거 지워짐` under them. List `▲ 앞에서 N회 사용` (batch 1). Left: the manuscript margin `▲` (script lane).
- **WLD-21 matcher**: rules in `domain:world`; the card and list read the shared index, rescanned incrementally since batch 3.
- **WLD-22 잇기 · 아님**: card buttons (batch 1) and `⋯ › 아님으로 둔 것` with `되돌리기` per row. Batch 3: stale `아님` records are hidden from that list at once and released in the next world term write (same undo step). The release inside the script's own block edit is the script lane's (Requests). Toast texts are TODO_COPY.
- **WLD-28 점검 센터 줄**: rows and counts are `domain:world` / `domain:checks`'s and the check center screen is feature:story's. World's part: the `공개 시점 옮기기` flow is self-contained and registered with the shell, so it opens over the check center as soon as it can be named (see Requests); the card `▲` opens the check center with `용어 공개`.
- **WLD-29 세계 시간**: route `WorldCalendar`: radios `연도만 · 예: 1200년 / 세계 달력 만들기 · 달 이름 · 한 달의 날 수 · 요일을 직접 / 시간 없이 · 연표 끔`, `이야기가 시작되는 해` (keystrokes merge into one undo step), note `생일이 있으면 나이 자동 계산`; mode changes keep stored dates and the calendar. In calendar mode without months the project still reads like `연도만` until the editor (WLD-30) is saved.
- **WLD-31 연표**: header, info line with `나이 자동 계산`, chips, line list with diamonds (lens chapter in `acc`, larger), world-event titles in `ink2`, dimmed rows after the lens, tap → `EventDetail` beside the list; `시간 없이` shows `시간 없이 · 연표 끔` with `세계 시간`. The chips switch views (WLD-34) and `기준 사건` picks the anchor with `EventPickerSheet`.
- **WLD-32 사건 만들기 · 고치기**: shared `EventCreateSheet` (`종류 · 이름 · 설명 · 연도 (월 · 일) / 씬 · 장소 · 관련 용어 · 공개 시점`, `넣기`), `EventEditSheet` from any field of the detail or `⋯ › 고치기`, `⋯ › 지우기` to the trash with `되돌리기`. Left: the `◇` on flashback scenes whose base event was deleted (story lane draws scene times).
- **WLD-33 사건 상세**: pane title, name, time, description with links, rows, `이 사건에서 바뀐 것`, and (batch 3) `+ 변화` on live world events: item picker (인물 · 용어 · 장소 · 세력, 초성, hidden row) → field (value now) → value in the field's input (짧은 글, 긴 글 as plain paragraphs, 숫자, 고르기 chips, 여러 개 고르기, 날짜 with 월 · 일 for YMD fields, 기념일, 연결 through the place / term / faction picker) → `넣기`, one undo step. Left: device check; 긴 글 values have no `@` links in this form; the record's history note (`처음 만남`) is not asked.
- **WLD-23 모두 잇기 · 모두 아님** (batch 3): header action on the 후보 group (only when the index is final), sheet `후보 N · <용어>` + `취소`, segmented `모두 잇기 | 모두 아님` starting on the tapped word, rows with checkboxes and the boxed sentence, footer with the `▲ …` part in `warn`, `넣기 · N개`, note; one undo step and a toast with `되돌리기`; `점검 중` while the first scan runs. The shared `LinkCandidatesSheet` serves other areas and starts in `모두 잇기`. Left: device check; toast texts TODO_COPY.
- **WLD-24 맞음** (batch 3): `맞음` text button next to the card's `▲` marker; marked rows keep the `▲` line in `ink3` with the `맞음` tag; undo through the toast. The check center's `맞음` and `맞다고 표시한 것` are feature:story's. Left: device check; toast text TODO_COPY.
- **WLD-25 글자 찾기**: `글자 찾기 꺼짐 · 켜기` and the chip `글자 찾기 끄기` (batch 1); since batch 3 switching sweeps only that term. Left: device check.
- **WLD-26 후보 색인** (batch 3): see Architecture (`TermIndexes`). Left: the device timing check (150 terms × 3 aliases, 20,000 blocks), including `RescanPlans.changedSpots`, which walks the manuscript twice per document change; the index lives in memory per open project (no app DB), so a full scan runs whenever a world screen first opens the project.
- **WLD-30 세계 달력** (batch 3): editor under the calendar option (months `달 이름` + `한 달의 날 수` with remove buttons and `+ 달`, weekdays with `+ 요일`, story start month chips + day, start weekday chips, `저장` with the blocking line). Left: device check; labels of the start date and weekday are TODO_COPY.
- **WLD-34 연표 보기 셋**: chips and labels (batch 2); batch 3 opens the 연표 in the project's `때 표기`. Left: device check.
- **WLD-35 그때 나이**: ages on rows (주연) and in the detail come from `domain:world`; tested at the state level. Left: device check.
- **WLD-63 이곳의 시간**: the place card tab (batch 1) with rows, the dashed `acc` divider and dimming; rows open the event or the scene. Batch 3: while locked, hidden characters leave the scene sub lines (new `domain:world` `PlaceTimes.rows(placeKey, lens, hiddenUnlocked)`; tests in `PlaceCardStateTest` and `ManuscriptContractTest`). Left: device check.
- **WLD-64 연표 + 지도** (batch 3): route `TimelineMap` (entry: pin button on the 연표 header): list with hint, rows `time | title / sub | place` plus `세력 영역 변화 ◆`, tap selects and lights the pin or region, `›` opens the event; map chips when several maps; region chips `1-7 · 1-9 ◆ · 비교` with caption and `점선 = …`. Left: the map is a schematic (region outlines and pins on a plain ground), not feature:map's rendered base (Requests); device check.
- **WLD-47 이동 수단 / route `TravelModes`** (batch 3): rows `도보 | 15 km · 8시간`, tap → form `이름 · 하루 거리 km · <hours> 시간` with `저장`, `직접` adds a mode. Left: deleting a mode and choosing the project default mode (no spec copy), device check.
- **WLD-68 지도 · 카드 모션** (world part): `목록 ↔ 카드` (list text out 160 ms, cards grow in place 360 ms after it, only right after the switch on the same tab), card carousel snap and parallax (batch 2); reduced motion = 120 ms fades. The map rows of the table (generation, re-roll, child map zoom, reveal blur, vertex, route, stamp, hex fill, conditions) belong to feature:map. Left: device check.

## Defaults used

- **Empty value `—`**: shown for an empty `관련` or `지역` and for empty fields in the `보기` state (the boards use `—` for empty table cells, e.g. `BulkPlace`, `Lens`).
- **`⋯` menu**: `이름 바꾸기 · 설명 고치기`, then (batch 2) `아님으로 둔 것` (terms) · `숨기기` / `숨김 풀기` · `합치기` · `지우기` (cast spec record menu order `숨기기 · 지우기 · 합치기` is `[PROPOSED]`; the destructive row is last here).
  - `설명 고치기` and its button `고치기` are not on any board (`설명` is a fixed field the boards never show editing). Words from the vocabulary.
  - The menu's accessibility label is `<이름> 메뉴`, after the scene head's `씬 4 메뉴`.
  - Read-only projects (the example) disable edit rows with `읽기 전용`.
- **`공개 시점 옮기기` sheet**: the title is the check center's button (WLD-28). Its sub-actions `원고에서 고르기` and `에피소드 · 장으로` are `[PROPOSED]` labels; no board draws a reveal picker. The first-set toast `◇ 첫 공개 1-2` follows the move toast's pattern.
- **Reveal on a whole choice block** is allowed (Q-06, as the domain does).
- **Past-lens banner** (`LensEdit` copy from the cast spec) on world cards only, and only when the project is editable. The lists show no banner.
- **Out-of-manuscript usage rows**: the mono location column stays empty and the place (`페이지 · 하층 구역의 빛`, `인물 · 세라`) is a line under the sentence, as WLD-08 lists it among the lines under a row. Author memos inside the manuscript keep their scene code in the column.
- **Card candidate `잇기 · 아님`** act on the card at once (one undo step, toast with `되돌리기`). `맞음` is never offered there.
- **`@` in card descriptions**: the picker offers 인물 · 용어 · 장소, as in the script (`PickerModels.links`). The draft is saved across process death as JSON. Links inserted in the editor show as plain text in the field until saved.
- **Term index**: one per project, released with the last world screen, rescanned incrementally (WLD-26). A full scan after access (unlock) or particle changes keeps showing the old numbers; only the very first scan is `점검 중`.
- **Stale `아님`** (batch 3): no write without a user action (no undo-free content write exists in core:store). The list hides stale records at once; the next world term write (link, reject, aliases, `글자 찾기`, reveal, bulk sheet) releases them in the same undo step.
- **Bulk sheet** (batch 3): the group action shows only when the index is final; `넣기` with a row that went stale rebuilds the rows instead of closing; the shared key starts in `모두 잇기`.
- **`맞음` on the card** (batch 3, Q-07): a marked row keeps its `▲` line in `ink3` plus the `맞음` tag; no unmark on the card (the check center's `맞다고 표시한 것` has it); undo via the toast.
- **`+ 변화`** (batch 3): offered on live world events only (a story event's changes sit on its scene); one record per `넣기`; no history note; title `변화`, field step shows `없음` for an empty value; the header's close word is `뒤로` on the field and value steps (back to the step before).
- **세계 달력 editor** (batch 3): one empty month row to start; a month row can be removed while more than one exists; the start date defaults to month 1 day 1 and the start weekday to the first; `저장` is disabled with the blocking line under it; nothing changes the mode.
- **연표 default view** (batch 3): the project's `때 표기` (`에피소드 기준` or `이야기 시작부터`); `기준 사건` always needs a pick.
- **연표 + 지도** (batch 3): no event selected at first; the shown map = the selected event's map, else the map chip, else the first top-level map; a region `◆` anchored at a world event compares against 최신; the cover shows the map above the list (not drawn, Q-21); pins of other places dim while one is lit.
- **이동 수단** (batch 3): title `이동 수단`, field labels `이름 · 하루 거리 (km)`, hours label TODO_COPY; editing a default mode stores all three defaults first (`TravelModeEdits.ensureStored`).
- **Secret project fields** (`isSecret`) show on world cards like other fields; world cards have no `비밀` tab. Definitions hidden with `definitionHidden` are left out.
- **Fixed fields are base values**: `이름 · 설명 · 관련 · 지역 · 별칭` have no `ChangeTarget` in the model, so they carry no `◆` and an edit at a past lens changes them for every point. The banner's `고치면 1-5부터 적용` applies to project fields only (through cast's sheet). WLD-06 says every non-derived field may carry `◆`; see Requests.
- **Layout values** (paddings, the 64 dp location column, type roles) come from the board HTML through the earlier attempt. This batch re-checked the `Term` location column (64 px, mono 11) and the `World` hint line; other measures were not re-checked.
- **Place `지역` picker**: places only (no regions), the place itself and its descendants excluded. A loop is refused by `WorldItems.setParentPlace`.

- **Event sheet**: one form for create and edit; the kind is fixed after creation; the `공개 시점` picker offers chapters and episodes (`에피소드 · 장으로` levels), the scene picker scenes only. Labels `연도 · 월 · 일 · 씬 · 장소 고르기 · 씬 고르기` are vocabulary words (the sheet is not drawn). After `넣기` the sheet closes (no toast; the row appears).
- **Event detail**: every field and the head open the edit sheet; `⋯` has `고치기 · 지우기`. Title of the detail = event name, small line = `연표 · 세계 사건`.
- **Timeline current row**: the last undimmed chapter row carries the `acc` diamond (board `Timeline` marks 1장 at lens 1-7). Chapter rows open nothing.
- **Delete / merge sheets** are feature-local copies of cast's shared shells (copy from `domain:trash`); the merged item = the card's item, the picked item stays (board `RCMerge`: the open record is merged into the picked one). The merge choice rows are radio pairs labelled `<A> · 합쳐짐` / `<B> · 남음`.
- **Locked time screens**: a story event on a hidden scene shows `숨긴 씬` on the 연표 and its detail is a lock row with `지문으로 보기`; a hidden chapter row keeps only its code.
- **Merge undo** runs in the app scope on the project's open store (the merged card closes when the target card opens), and reverts the Life Recorder links too.
- **Event dates without a calendar**: month and day fields are not shown and a stored month and day are kept on edit.
- **Hiding** shows no toast (the card turns into its lock row unless items are open); it is one undo step.
- **Reveal flow**: `원고에서 고르기` and `에피소드 · 장으로` replace the `공개 시점 옮기기` sheet instead of stacking, so choosing closes the whole flow from any host screen.
- **Card view**: card width 220 dp, gap 12 dp, label = the list row's right column (`첫 공개 1-7 · ◇ 후보 1`).
- **Restore from the card**: `되살리기` (vocabulary) on the gone card, toast `<이름> 되살림 · …` from `Messages.restoreToast`.

## Spec conflicts

- **`세력 영역 변화 ◆` on the example** (batch 3): the fixture's region-shape `◆` (상층 경비대, 1-9) is anchored at an episode start, and `domain:world` `TimelineMaps` marks an event only when the record sits on the event, its scene or a block of its scene; no example event carries the tag, while board `TimelineMap` shows it on `상층 경비대 하층 진입`. Domain reading kept; the comparison chips still show on 세계 지도.
- **`아직 공개 전` order**: WLD-02 sorts by reveal point, which puts `상층 송전망` (`공개 예정 2장`, start of chapter 2) before `잿불의 서약` (`공개 예정 2-5`). The WLD-02 sample and board `World` list them the other way. The rule is followed (domain order). Test: `WLD-02 sample - revealed and planned terms`.
- **Past-lens section title**: the domain titles the first section `공개됨 · 1-5` at a past lens. The pattern `공개됨 · <lens label>` would read `공개됨 · 시점 · 1-5` if the chip label is meant. Domain output kept (its `RevealSpecTest` asserts it); no board draws it.
- **Term description sample**: the spec's list sample fragment `꺼지지 않는 빛을 내는 광석 등불` differs from the fixture's description. The list shows the fixture's first line.

## Motion pass (0.1.0 wave, 2026-11-23)

Rows of `motion-audit.md` §4 feature:world, built with the shared helpers only (`CivilightTheme.motion`, `civilightItem`, `ContentSwap`, `ValueSwap`, `FoldReveal`, `rememberUnhide`, `animatedColor` / `animatedFraction` / `animatedAlpha`, DS `SnapCardRow`, `CardViewMotion`, core:ui `SplitPanes`). `tools/gw :feature:world:compileDebugKotlin :feature:world:testDebugUnitTest` exits 0 (142 tests, 0 failures; `WorldCopyLintTest` now also runs `MotionLint.lintModule`). Nothing ran on a device: **every row below is `partial` until the orchestrator's emulator pass** (with and without `항상 줄이기`, animator scale 0).

| # | Row | What changed | Check on the emulator |
| --- | --- | --- | --- |
| 1 | List ↔ card | `list/ListCardMotion.kt` and `GrowIn` deleted. `WorldList` pins segments + hint + `ViewToggle` and swaps the body with one `AnimatedContent` keyed by `WorldView(tab, cards)`: a flip on one tab uses `CardViewMotion.transform` (text out on `exit`, cards in after it), a tab change uses `motion.swap()`. **GrowIn fix:** it never ran because `switched` was set in a `LaunchedEffect`, one frame after the card rows composed (`remember` saw false). Now the start scale is decided in the composition that brings the cards in (`WorldViewSwitch.growsIn(currentState, targetState)`), the `Animatable` starts at 0.92 and runs `enterLong` after the `exit` delay, then rows composed later (scrolling) draw at rest. Pure decisions in `list/WorldViewSwitch.kt` (JUnit) | flip `목록 → 카드` and back on 용어 / 장소 / 세력; the cards must grow from the top-left after the list text has left; reduced = 120 ms fades |
| 2 | Rows and cards insert, remove, reorder | `civilightItem()` on every lazy item (list rows, card rows, lock, empty). The 160 ms delay is gone except on the list ↔ card swap (it is `CardViewMotion`'s). Card rows are keyed by section (`cards:<title>`, repeats get `#2`), not by index; list lines have unique keys without the index (`WorldViewSwitch.uniqueKeys`, tested on every tab of the example project) | lens change, hidden unlock, create / delete an item |
| 3 | Card snap and parallax | `WorldCardRow` is the DS `SnapCardRow` (snap on `enter`, tick on settle, `rememberUpdatedState(reduced)`, 8 % parallax and constant 1.1 scale read in `graphicsLayer`). `imageModifier` goes on the image layer inside the clip (`CardImageContent`). Local snap / parallax / tick code deleted | fling a row; reduced = snap kept, no parallax |
| 4 | Tab switch | The body crossfades (`motion.swap()`, no delay). The `용어` hint line is a `FoldReveal` | switch 장소 / 용어 / 세력 |
| 5 | Locked / gone / open | `CardBodySwap` (new, `common/WorldMotion.kt`) wraps the body of the term, place, faction and event screens: loading / locked / gone / open crossfade; unlock plays `rememberUnhide` (hoisted above the swap so only a real unlock plays) on the open body. Items inside use `civilightItem()`. The `되살리기` button and the same-name line fold in (`FoldReveal`) | unlock a hidden card, restore a gone card |
| 6 | Lens value swap, dimmed rows | `CardHead` eyebrow, name and description swap with `ValueSwap` / `motion.valueSwap()`; field values already swap in the DS `FieldTableRow`. Dimmed rows (`쓰인 곳`, 연표, 이곳의 시간) fade through `Modifier.dimmed` (`animatedAlpha`) | move the lens across a change |
| 7 | Card bodies, usage groups, timeline, event changes, settings lists | Place tabs are one `ContentSwap(tab)` item; term notices (`searchOff`, chips) fold; `FieldSection` empty fields and the `빈 항목 N · 보기` line fold; timeline rows / chips / footer, event rows and changes, 세계 시간 and 이동 수단 lists use `civilightItem()`; the timeline body (none / `시간 없이` / list) is a `ContentSwap`; the calendar editor folds the weekday block and the problem line and eases its height | open each screen, toggle `시간 없이`, add / remove a calendar month |
| 8 | Timeline + map | Regions: per-key `animatedFraction` drives fill alpha, stroke width and tone; another map crossfades on `standard` (`ContentSwap` keyed by `mapKey`); compare chips and the dotted note fold (`FoldRevealValue`); wide ↔ narrow is an `AnimatedContent(wide)` on `foldOpen` / `foldClose`; selected row highlight and title colour animate. **Pane split:** the wide event list | map frame is now `SplitPanes(PaneSplit.Modules.TIMELINE, …)` (390 dp list by default), so it gets the CNV-47 handle. `PaneSplit.Modules` has no id for this screen, so it shares the 연표 ratio (Requests) | fold / unfold, pick a map, drag the handle |
| 9 | Field section and sheets | Step swaps with `motion.push` through `SheetSteps` (new): `+ 변화` (item → field → value → target, `ChangeStep.isAhead`), the event form's pickers, the description editor's link picker; `ContentSwap` for the bulk sheet's scanning / empty / rows; `FoldReveal` for the event form's kind, year, month-day, scene and reveal rows, the alias chips, the link tool, merge sheet lines. Left: `TermTicks`, the delete impact sheet and the reveal sheets still draw static lists (their content does not change while open) | step through `+ 변화`, create an event |
| 10 | Images | One `CardImageContent` for the card slot and the card row tile: picked image `Crossfade(spec(standard))`, selected tile fill `animatedColor` | open a card with a picked image |

**Deleted copies:** `ListCardMotion`, `GrowIn`, the local `rememberSnapFlingBehavior` row, the local parallax, the hand-rolled `delay`.

**QA 8 (empty 지도 list, heading `지도 · 0` twice, empty card without side margin): not in this module.** The 지도 list is `feature/map/.../list/MapListScreen.kt`: it draws `SectionLabel(list.section)` and then `EmptyBlock(title = list.section, Modifier.padding(top = 48.dp))` with no horizontal padding. World's own empty lists already sit inside the 24 dp content padding and show no heading twice. Not edited (DECISIONS §2: the map lane owns it); the fix is `EmptyBlock(title = <empty line, not the section>, Modifier.padding(horizontal = 24.dp, vertical = 48.dp))` there. The world card view now pads its lock and empty lines the same way (24 dp).

**Decisions to confirm**
- The list rows have no 48 dp thumbnail (the audit says "shared thumbnail"; spec WLD-02..05 draw none and D6 added the thumbnail for the library only). So the list ↔ card flip fades text and grows the card row in place, without a shared image. Adding a `leading` thumbnail to `ListRow` in `RowsBody` plus `sharedElement` would complete it; I did not widen the design.
- Segments, hint and `ViewToggle` are now pinned above the scrolling list (they scrolled with it before), because the body swap must not fade them.
- Copy: no `TODO_COPY_WORLD_*` is left. Toasts reuse the action's own word (`잇기`, `아님`, `맞음`, `넣기`, `저장`; bulk `모두 잇기 · 3`); `영역` / `세력 영역 변화 ◆` (faction sections), `연결` (picker title), `하루 이동 시간` (rule 18), `월 · 일` (calendar start label), `이미 있음` (alias duplicate and travel name taken, the gallery's word) and the `… 없음` lines (`후보 없음`, `항목 없음`, `아님으로 둔 것 없음`) are the nearest existing words. The refusal lines under the calendar, travel, alias and date fields are the label of the field to fix (`+ 달` for no month, `달 이름`, `한 달의 날 수`, `요일`, `월 · 일`, `이름`, `하루 거리`, `연도` …). The toasts read as the action (`잇기`, `넣기`, `저장`), not as a result (`지움`, `되살림`) as the vocabulary prefers; a result wording needs the spec owner. All of these want the spec owner's confirmation.

## Requests

- **Copy (TODO_COPY; replaced in the motion pass by the nearest existing words, to confirm)**:
  - `TODO_COPY_WORLD_FACTION_AREAS`: title of the faction card's section of linked map regions and provinces.
  - `TODO_COPY_WORLD_FACTION_EVENTS`: title of its section of events where a `◆` of the faction changes.
  - `TODO_COPY_WORLD_LINK_PICKER_TITLE`: the `@` picker title. feature:script asks the same (`TODO_COPY_SCRIPT_LINK_PICKER_TITLE`); one word for both would be best.
  - `TODO_COPY_WORLD_LINKED_TOAST` and `TODO_COPY_WORLD_REJECTED_TOAST`: toast text after `잇기` / `아님` on a card candidate (WLD-22 gives only `되돌리기`).
- **Copy to confirm**: `설명 고치기` · `고치기`, `원고에서 고르기`, `에피소드 · 장으로`, `<이름> 메뉴`, `◇ 첫 공개 1-2` (see Defaults). Batch 2: `연도 · 월 · 일 · 씬 · 씬 고르기 · 장소 고르기` (event sheet, not drawn), `<별칭> 빼기` (chip remove label), `시간 없이 · 연표 끔` as the timeline's off state with the button `세계 시간`, `숨김 풀기` (cast spec `[PROPOSED]`), `보기 · 목록 · 카드` (DS `ViewToggle` defaults), the merge sheet heads `<A> · 합쳐짐` / `<B> · 남음` (board `RCMerge`) used as radio sub-lines.
- **feature:cast**: register `FieldValueEditSheet` and `AddFieldSheet` for `EntityKind.PLACE · TERM · FACTION`. The world cards open them for project fields.
- **feature:story**: `CheckCenter(filter = CheckFilter.TERM.name)` opens the `용어 공개` filter. Settled: the core pass made the `CheckFilter` name the route contract.
- **feature:script (SCR anchor engine)**: a block-pick mode in the script for `원고에서 고르기` (now a scene list, then block rows in a sheet), and the `근거 지워짐` actions for `◇ 첫 공개`.
- **feature:map**: the place summary sheet and side card on the map (WLD-09 first shape), and the 16:9 map crop around the pin for the top of the place card. A small composable taking `(mapKey, pinKey)` would let this card show it.
- **core:model / spec owner**: `◆` history on fixed world fields (`설명`, `지역`, `관련`) needs a `ChangeTarget` kind. Until then they are base values (see Defaults).
- **core:ui (optional)**: a shared `LensEditBanner` helper. Delivered by the core pass as `ProjectLensEditBanner`; world has not switched yet (its `common/LensBanner` stays, same copy).
- **FEATURES.md**: update rows WLD-01 02 03 04 06 08 09 10 11 12 17 18 19 and (batch 2) WLD-05 07 13 14 15 16 20 21 22 28 29 31 32 33 to partial (this module may not edit that file).
- **Copy (TODO_COPY, batch 2; replaced in the motion pass, to confirm)**: `TODO_COPY_WORLD_ALIAS_EMPTY`, `TODO_COPY_WORLD_ALIAS_SAME_AS_NAME`, `TODO_COPY_WORLD_ALIAS_DUPLICATE` (alias refusals, WLD-16 `[PROPOSED validation]` has no lines), `TODO_COPY_WORLD_NO_REJECTIONS` (empty `아님으로 둔 것`), `TODO_COPY_WORLD_EVENT_DATE_INVALID` (a year, month or day outside the calendar).
- **core:navigation (WLD-28)**: a shared WORLD sheet `RevealMoveSheet(projectKey, termKey)` for the check center's `공개 시점 옮기기`. World would register it with `RevealContent` (already self-contained, `term/RevealSheets.kt`); until then feature:story can push `TermCard(p, termKey)`.
- **feature:cast**: cast registers `DeleteImpactSheet` for every `EntityKind`; `MergeItemsSheet` is still not registered (checked in batch 3). World keeps its feature-local copies (`common/LifecycleSheets.kt`, writing through `TrashOps` and `MergeActions.mergeItems`); switch `⋯ › 지우기` to cast's sheet once a `MergeItemsSheet` serves world kinds.
- **feature:canvas / image area**: register `ImagePickSheet` (world opens it for `CARD_REFERENCE` of places, terms, factions), and expose the preset images (`preset:<id>`) in a shared place (core:ui or core:designsystem) so cards can draw them; world draws a neutral shard for presets now.
- **feature:story**: the 프로젝트 홈 `연표` shortcut should hide in `시간 없이` (`WorldTimeMode.NONE`, WLD-29 `[PROPOSED]`); the timeline itself shows `시간 없이 · 연표 끔`.
- **feature:library**: `프로젝트 설정 › 세계 시간` row → `WorldCalendar(p)` (built).
- **Batch 3 requests**:
  - **feature:script (WLD-27, SCR-56)**: render term marks from `domain:world` `term/ManuscriptMarks` (`at(spot)` → link spans with `preReveal` for the margin `▲`, candidate spans for the `용어 후보` layer; `popover(candidate)` for the small window; writes `TermEdits.link` / `reject`). The shared candidate index lives in feature:world (`TermIndexes`) and cannot be reached from the script: build one with `CandidateIndex.build` / `RescanScheduler` there, or ask core to host it.
  - **feature:script (WLD-22)**: inside the block-edit transform call `StaleRejections.release(doc, TermAccess(hiddenOpen), spots = <edited spots>)`, so a stale `아님` goes in the same undo step as the text change (WLD-T08).
  - **core:navigation**: `LinkCandidatesSheet(projectKey, termKey, mode)` (WLD-23 says the sheet opens on the tapped word; world uses a feature-local key from its card meanwhile); `FieldValueEditSheet(…, anchorEventKey)` so cast's value sheet can serve `+ 변화` (world has its own flow meanwhile).
  - **core:store**: a content write without an undo step for derived maintenance (stale `아님` release), refused on read-only projects.
  - **feature:map**: a small read-only map composable in a shared place (`mapKey`, highlights, region shapes at a point) so `연표 + 지도` can draw the real base instead of its schematic.
  - **feature:cast**: `MergeItemsSheet` is still not registered, so world keeps its local delete and merge sheets (rechecked at the end of batch 3).
  - **Copy (TODO_COPY, batch 3; replaced in the motion pass, to confirm)**: `TODO_COPY_WORLD_NO_CANDIDATES`, `TODO_COPY_WORLD_BULK_LINKED_TOAST`, `TODO_COPY_WORLD_BULK_REJECTED_TOAST`, `TODO_COPY_WORLD_INTENDED_TOAST`, `TODO_COPY_WORLD_CHANGE_ADDED_TOAST`, `TODO_COPY_WORLD_NO_FIELDS`, `TODO_COPY_WORLD_CALENDAR_START_LABEL`, `TODO_COPY_WORLD_CALENDAR_START_WEEKDAY_LABEL`, `TODO_COPY_WORLD_CALENDAR_NO_MONTH`, `…_MONTH_NAME`, `…_MONTH_DAYS`, `…_WEEKDAY_NAME`, `…_CALENDAR_START`, `TODO_COPY_WORLD_CALENDAR_SAVED_TOAST`, `TODO_COPY_WORLD_TRAVEL_HOURS`, `TODO_COPY_WORLD_TRAVEL_NAME_EMPTY`, `…_DISTANCE_BAD`, `…_HOURS_BAD`, `…_NAME_TAKEN`.
  - **Copy to confirm (batch 3)**: `변화` (title of `+ 변화`), `달 이름 · 한 달의 날 수 · 요일 · + 달 · + 요일 · 저장`, `이동 수단 · 직접 · 하루 거리`, `연표 + 지도` (pin button label), `용어 고르기 · 세력 고르기`, `<이름> 빼기`.
  - **FEATURES.md**: batch 3 rows WLD-23 24 25 26 30 33 34 35 63 64 68 → partial; WLD-27 and WLD-69 → done (contract API / reserved storage); WLD-47 editor part → partial.
- **Motion pass (2026-11-23)**
  - `core:model` `PaneSplit.Modules`: an id for `연표 + 지도` (the screen uses `TIMELINE` for now, so it shares the 연표 list | detail ratio).
  - `feature:map` (QA 8): `MapListScreen` empty state, see the Motion pass section.
  - Spec owner: the copy listed under "Decisions to confirm"; the 48 dp thumbnail of world list rows (WLD-02..05).
