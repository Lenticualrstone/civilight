# feature:world

Batch 1 of 3 done (WLD-01 02 03 04 06 08 09 10 11 12 17 18 19). `tools/gw :feature:world:compileDebugKotlin :feature:world:testDebugUnitTest` exits 0: 71 JVM tests in 10 classes, all passing. No domain module was changed. Nothing was run on a device or emulator; every screen is checked only by compiling and by the tests of its state builder.

## Architecture

Package `io.github.lenticualrstone.civilight.feature.world` (`src/main/java/...`). Composables stay thin: each screen has a pure `…State.build(doc, …)` (tested on `ExampleProject.build()`), a view model that combines the document, the hidden-item unlock and the candidate index into that state, and a screen that draws it.

- **`WorldEntries.kt`** (`worldEntries`, shell.md §1): registers `World`, `TermCard`, `PlaceCard`, `FactionCard`. Not registered yet: `Timeline`, `EventDetail`, `TimelineMap`, `WorldCalendar`, `TravelModes`, and the WORLD shared sheets `LinkCandidatesSheet` (WLD-23) and `EventCreateSheet` (WLD-32). They keep the shell placeholders. Register each one here as its batch builds it.
- **`WorldCopy`**: all UI copy this module writes. Strings the domain already builds (`공개됨 · 최신 · 1-7`, `쓰인 곳 · 2 · 후보 1`, `숨긴 장소 N`, `등장 1-2 · 1-7 외 4` …) are taken from `domain:world` and not repeated. Missing copy = `TODO_COPY_WORLD_*`. `WorldCopyLintTest` runs `CopyLint` over every constant and formatted function. Add new formatted functions to its `formatted()` list.
- **`common/`**: shared by every world screen.
  - `WorldViewModel`: base on `ProjectViewModel`, holding `readOnly`, `hiddenOpen` (rule 19, `UnlockController.itemsOpen`) and `editContext()`. Edits go through `apply(label) { doc -> doc' }`, one undo step each. Also `rename`, `setDescription` / `setDescriptionDoc`, `backToLatest`, `undoLast`.
  - `WorldEnv`: the app-scoped parts of `FeatureDeps` a view model may keep.
  - `TermIndexes`: one `CandidateIndex` per open project, shared by list and card in two panes, ref-counted with `acquire` / `release`. It rescans the whole manuscript when the text, terms or particle list change (`ScanInput.sameText`). WLD-26 (`RescanScheduler`) replaces `Holder.run` without touching callers.
  - `CardParts` (art slot, head, fixed row, field section, locked / gone card) and `CardModels` (`CardAccess`: `Open · Locked · Gone`, `FixedRowUi`, `ItemRefsText`).
  - `FieldRows`: project fields at the lens, with `◆` and the `빈 항목 N · 보기` split.
  - `RichText`: `CardText` draws card text with link decorations. `RichTextEdit` edits text and keeps runs: `apply` diffs one change, `insertLink` handles `@`.
  - `ItemEdits` and `RelatedOptions`: name, description, `관련`.
  - `ItemSheets`: `⋯` menu, `이름 바꾸기`, `설명 고치기` with the `@` tool. `DescriptionDraft` is the editor draft.
  - `LensBanner`: the past-lens banner.
  - `WorldTargets` / `openFromWorld`: where rows lead. World cards open with `openDetail`, everything else with `push`.
  - `ScreenParts`: back button per pane role, command bar, selected card key.
- **`list/`**: `WorldScreen`, `WorldListViewModel`, `WorldListState` (`WorldListItem`: Section · Row · Lock · Empty), `WorldTabs` / `WorldSegment`, and `CreateItemSheet` (feature-local key). The `지도` segment pushes feature:map's `MapList`. A tab is remembered per project in `_app/view.json › worldTab`.
- **`term/`**: `TermCardScreen` / `TermCardViewModel` / `TermCardState` (`UsageRowUi`, `UsageGroupUi`), `UsageRows` (row view with inline box, place line and candidate `잇기 · 아님`), `RevealOptions` + `TermSheets` (`공개 시점 옮기기`: link rows, `원고에서 고르기` scene → block, `에피소드 · 장으로`; `관련` sheet).
- **`place/`**: `PlaceCardScreen` (tabs `개요 · 이곳의 시간 · 지도`), `PlaceCardState`, `PlaceCardViewModel` (`지역`), `ParentChoices`.
- **`faction/`**: `FactionCardScreen` with its view model, and `FactionCardState`.
- **How to extend:**
  - A new route: a screen package with `…State` + view model + screen, then an `entry<Route>` in `WorldEntries`.
  - A card action: a menu row in `ItemMenuContent` (feature-local sheet keys in `ItemSheets`) and a view-model method calling the domain or `domain:actions`. Deletes go through `domain:trash` and merges through `MergeActions` (DECISIONS §1).
  - Shared sheets: `deps.sheets.register<LinkCandidatesSheet> { … }` in `WorldEntries`.

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

## Partial

All of these compile and their state is tested. None has been seen on a device.

- **WLD-01 세계 화면 틀**: header (project name, `세계`, lens chip, `+`), segments `장소 · 용어 · 세력 · 지도` (the last pushes `MapList`), hint `공개 시점 전 사용 → 점검 센터` → `CheckCenter(filter = CheckFilter.TERM.name)`, command bar `장소 · 용어 · 세력 찾기` → `ProjectSearch` (mic → `Recording`), the remembered tab, list | card through `openDetail` with the selected row marked, and the 560 column through `CenterColumn`. Left: the `목록 | 카드` toggle (WLD-05, batch 2), the node-map placeholder `장소 · 노드 찾기` (map lane), and fold motion (shell CNV-34).
- **WLD-02 용어 목록**: two sections at the lens, right column `첫 공개 1-7 · ◇ 후보 1` / `▲ 앞에서 1회 사용` (warn), candidate numbers dimmed while the first scan runs, `숨긴 용어 N · 지문으로 보기`, and `용어 없음` with `용어 만들기`.
- **WLD-03 장소 목록 / WLD-04 세력 목록**: domain rows (`등장 …`, `지역` sections, `인물 N`), lock rows, empty state. The list is a `LazyColumn`, so 100+ rows stay virtualized.
- **WLD-06 카드 공통**:
  - Fixed rows per kind. Project fields at the lens with `◆`, and `빈 항목 N · 보기` reveals the empty ones.
  - Each field row opens cast's `FieldValueEditSheet`, and `+ 항목` opens `AddFieldSheet`. The cast lane owns both, so `이 값 고치기 · 여기서부터 바뀜` work once cast registers them.
  - `설명 고치기` keeps links and paragraph keys.
  - Left: the image slot draws the initial-letter card only (WLD-07, batch 2).
- **WLD-08 용어 카드**:
  - Header, art slot labelled `참고 이미지`, eyebrow, name, and the description with links.
  - Rows `첫 공개` (tap → `공개 시점 옮기기`), `별칭` (read-only), `관련` (tap → tick sheet with 초성 search and a hidden-item row), then project fields.
  - Notices `1음절 · 글자 찾기 안 함` and `첫 공개 없음 · 쓰인 곳 N`, `글자 찾기 꺼짐 · 켜기`, and chips `<위치>을 첫 공개로` and `글자 찾기 끄기`.
  - `쓰인 곳` groups with inline boxes, markers and tags. Out-of-manuscript rows put `페이지 · 하층 구역의 빛` on a line under the sentence. Rows are dimmed after the lens, and a tap opens the manuscript, page, profile or card.
  - Candidate rows draw `◇ 후보 · 끊김` + `잇기 · 아님` as buttons (`TermEdits.link` / `reject`, one undo step and a toast). This is the card part of WLD-22.
  - `⋯` has `이름 바꾸기 · 설명 고치기`.
  - Left (later batches): `⋯ › 아님으로 둔 것 · 지우기 · 합치기 · 숨기기` (WLD-13/14/15/22), the group action `모두 잇기 · 모두 아님` (WLD-23, `LinkCandidatesSheet`), the alias editor with `후보 N 새로 찾음` (WLD-16), the `맞음` tag on rows (WLD-24), and the image (WLD-07).
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

## Defaults used

- **Empty value `—`**: shown for an empty `관련` or `지역` and for empty fields in the `보기` state (the boards use `—` for empty table cells, e.g. `BulkPlace`, `Lens`).
- **`⋯` menu**: `이름 바꾸기 · 설명 고치기` until the batch 2 rows arrive.
  - `설명 고치기` and its button `고치기` are not on any board (`설명` is a fixed field the boards never show editing). Words from the vocabulary.
  - The menu's accessibility label is `<이름> 메뉴`, after the scene head's `씬 4 메뉴`.
  - Read-only projects (the example) disable edit rows with `읽기 전용`.
- **`공개 시점 옮기기` sheet**: the title is the check center's button (WLD-28). Its sub-actions `원고에서 고르기` and `에피소드 · 장으로` are `[PROPOSED]` labels; no board draws a reveal picker. The first-set toast `◇ 첫 공개 1-2` follows the move toast's pattern.
- **Reveal on a whole choice block** is allowed (Q-06, as the domain does).
- **Past-lens banner** (`LensEdit` copy from the cast spec) on world cards only, and only when the project is editable. The lists show no banner.
- **Out-of-manuscript usage rows**: the mono location column stays empty and the place (`페이지 · 하층 구역의 빛`, `인물 · 세라`) is a line under the sentence, as WLD-08 lists it among the lines under a row. Author memos inside the manuscript keep their scene code in the column.
- **Card candidate `잇기 · 아님`** act on the card at once (one undo step, toast with `되돌리기`). `맞음` is never offered there.
- **`@` in card descriptions**: the picker offers 인물 · 용어 · 장소, as in the script (`PickerModels.links`). The draft is saved across process death as JSON. Links inserted in the editor show as plain text in the field until saved.
- **Term index**: one per project, released with the last world screen. The full rescan on text change stands in for WLD-26's incremental rescans.
- **Secret project fields** (`isSecret`) show on world cards like other fields; world cards have no `비밀` tab. Definitions hidden with `definitionHidden` are left out.
- **Fixed fields are base values**: `이름 · 설명 · 관련 · 지역 · 별칭` have no `ChangeTarget` in the model, so they carry no `◆` and an edit at a past lens changes them for every point. The banner's `고치면 1-5부터 적용` applies to project fields only (through cast's sheet). WLD-06 says every non-derived field may carry `◆`; see Requests.
- **Layout values** (paddings, the 64 dp location column, type roles) come from the board HTML through the earlier attempt. This batch re-checked the `Term` location column (64 px, mono 11) and the `World` hint line; other measures were not re-checked.
- **Place `지역` picker**: places only (no regions), the place itself and its descendants excluded. A loop is refused by `WorldItems.setParentPlace`.

## Spec conflicts

- **`아직 공개 전` order**: WLD-02 sorts by reveal point, which puts `상층 송전망` (`공개 예정 2장`, start of chapter 2) before `잿불의 서약` (`공개 예정 2-5`). The WLD-02 sample and board `World` list them the other way. The rule is followed (domain order). Test: `WLD-02 sample - revealed and planned terms`.
- **Past-lens section title**: the domain titles the first section `공개됨 · 1-5` at a past lens. The pattern `공개됨 · <lens label>` would read `공개됨 · 시점 · 1-5` if the chip label is meant. Domain output kept (its `RevealSpecTest` asserts it); no board draws it.
- **Term description sample**: the spec's list sample fragment `꺼지지 않는 빛을 내는 광석 등불` differs from the fixture's description. The list shows the fixture's first line.

## Requests

- **Copy (TODO_COPY)**:
  - `TODO_COPY_WORLD_FACTION_AREAS`: title of the faction card's section of linked map regions and provinces.
  - `TODO_COPY_WORLD_FACTION_EVENTS`: title of its section of events where a `◆` of the faction changes.
  - `TODO_COPY_WORLD_LINK_PICKER_TITLE`: the `@` picker title. feature:script asks the same (`TODO_COPY_SCRIPT_LINK_PICKER_TITLE`); one word for both would be best.
  - `TODO_COPY_WORLD_LINKED_TOAST` and `TODO_COPY_WORLD_REJECTED_TOAST`: toast text after `잇기` / `아님` on a card candidate (WLD-22 gives only `되돌리기`).
- **Copy to confirm**: `설명 고치기` · `고치기`, `원고에서 고르기`, `에피소드 · 장으로`, `<이름> 메뉴`, `◇ 첫 공개 1-2` (see Defaults).
- **feature:cast**: register `FieldValueEditSheet` and `AddFieldSheet` for `EntityKind.PLACE · TERM · FACTION`. The world cards open them for project fields.
- **feature:story**: `CheckCenter(filter = CheckFilter.TERM.name)` is how the `용어` hint opens the `용어 공개` filter. Please accept the enum name as the filter string.
- **feature:script (SCR anchor engine)**: a block-pick mode in the script for `원고에서 고르기` (now a scene list, then block rows in a sheet), and the `근거 지워짐` actions for `◇ 첫 공개`.
- **feature:map**: the place summary sheet and side card on the map (WLD-09 first shape), and the 16:9 map crop around the pin for the top of the place card. A small composable taking `(mapKey, pinKey)` would let this card show it.
- **core:model / spec owner**: `◆` history on fixed world fields (`설명`, `지역`, `관련`) needs a `ChangeTarget` kind. Until then they are base values (see Defaults).
- **core:ui (optional)**: a shared `LensEditBanner` helper with the cast copy (`<code> 시점 값 고치는 중`, `고치면 <code>부터 적용`, `최신으로`), so cast and world do not each keep a copy.
- **FEATURES.md**: update rows WLD-01 02 03 04 06 08 09 10 11 12 17 18 19 to partial (this module may not edit that file).
