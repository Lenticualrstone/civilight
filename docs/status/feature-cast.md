# feature:cast

Package `io.github.lenticualrstone.civilight.feature.cast` (module `feature/cast`). Batch 1 of 4 done: CST-01 02 05 07 08 09 10 11 12 19 20 21 28.

**Build and tests.** `tools/gw :feature:cast:compileDebugKotlin :feature:cast:testDebugUnitTest` exits 0. 55 JVM tests in 7 classes, all passing (counts from the test-result XML): `CastCopyLintTest` 3, `CastListModelTest` 9, `ProfileModelTest` 11, `TimedEditModelTest` 12, `FieldDraftsTest` 8, `FieldSettingsTest` 8, `FixedFieldEditsTest` 4. No domain module was changed. Nothing was run on a device or emulator, and no APK was built.

## Architecture

**Entries** (`CastEntries.kt`, `castEntries`): registers routes and shared sheets only.
- Routes: `CastList`, `CharacterProfile`, `FieldSettings` (imported as `FieldSettingsRoute`, because `domain:cast` also has a `FieldSettings`).
- Shared sheets: `FieldValueEditSheet`, `FieldHistorySheet`, `AddFieldSheet`.
- Not registered yet, so they show the shell's placeholder: `RelationMap`, `FieldDefEdit`, `RelationKindSettings`, `TagSettings`, `ArchiveDocEdit`, and the sheets `RelationEditSheet`, `DeleteImpactSheet`, `MergeItemsSheet`. Register each one in the batch that builds it.

**Packages** (one folder per screen):
- `CastUiCopy.kt`: the only UI copy object (DECISIONS §3).
  - It holds only copy that the `domain:cast` builders (`CastList`, `Profile`, `TimedValues`, `RelationViews`, `FieldSettings`, `CastCopy`) do not already produce. Domain strings are shown as they come.
  - `BTN_…` constants are buttons (linted as buttons). Formatted copy is functions (`since`, `emptyRow`, `recordSub` …).
  - `TODO_COPY_…` constants are missing copy (see Requests).
  - `CastCopyLintTest` lints every constant and list by reflection, plus each function with example-project arguments. A new function needs a line in `formatted()`.
- `common/`:
  - `CastViewModel`: a `ProjectViewModel` with `apply(label) { doc, ctx -> doc' }` (suspend) and `act(label, then) { doc, ctx -> doc' }` (fire and forget). Each write gets a fresh `EditContext` from `FeatureDeps.editContext()` (shell clock, zone and device id) and is one undo step named by its button's copy. A read-only project returns `EditResult.READ_ONLY`; it never throws.
  - `CastEditorViewModel`: the editor behind shared sheets. Sheets get it with `rememberCastEditor(projectKey)` (`field/ValueSheetParts.kt`), which keys it inside whichever entry draws the sheet.
  - `castLens` (`ProjectDoc.castLens`): the stored lens through `LensRules.effective`. Every cast read uses it.
  - `RecordFrame`: the record's frame. A floating glass back button and lens pill sit over the head; the glass top bar (name and record number) appears once the name scrolls away; tabs sit above the command bar. It also draws toasts and sheets.
  - `HiddenMark`: the `숨김` tag.
- `list/` (CST-01, CST-02, CST-05):
  - `CastListModel` (pure; `CastListUi` holds the chip, query and typing state, and is the place to add view modes), `CastListViewModel` (`create`), `NewCharacter` (pure).
  - `CastListScreen`; `NewCharacterSheet` (feature-local key and content).
- `profile/` (CST-07, CST-08, CST-11, CST-12, CST-28):
  - `ProfileModel` (pure; `ProfileUi` holds the tab, `보기` and `항목 더 보기`; it returns null for a trashed, missing or locked character, and the screen then pops).
  - `ProfileScreen` + `ProfileViewModel`. `ProfileTabs.kt` has one composable per tab: `OverviewTab`, `RelationsTab`, `SpeechTab`, `GameTextTab`, `SecretTab`. `RecordRows.kt` has `RecordFieldRow` and `PlannedRow`.
  - `FixedFieldEdits` (pure; `이름`, `한 줄 소개`, tags) and `FixedFieldSheets.kt` (feature-local `NameEditSheet`, `OneLinerEditSheet`, `TagsEditSheet`).
  - `RecordActions` carries every row callback. Later batches add theirs there, for example the record `⋯` menu, voice-line edits and `+ 관계`.
- `field/` (CST-09, CST-10, CST-11, CST-20):
  - `FieldDraft` / `FieldDrafts` (pure): one draft shape for all nine types. It is saved as JSON with `FieldDraftSaver`.
  - `TimedEditModel` (pure): value and history views and saves for a `TimedSubject`. A subject is a `Field` of any kind (character, place, term, faction) or a character's `DisplayName`. Relation slots can join as a third subject in CST-29.
  - `LongTextEdit` (pure): plain-paragraph edits that keep block keys.
  - Composables: `FieldValueEditor` (inputs by type, `알 수 없음`, `연결` pickers from core:ui), `FieldValueSheet.kt` (shared `FieldValueEditSheet`; feature-local `DisplayNameEditSheet`), `HistorySheet.kt` (shared `FieldHistorySheet`; feature-local `DisplayNameHistorySheet`; `시점 추가` = unit picker, then the value), and `ValueSheetParts.kt` (`TwoButtonHead`, `SaveButtons`, `HistoryRow`).
  - The world lane can open `FieldValueEditSheet` / `FieldHistorySheet` with `kind = PLACE · TERM · FACTION`; the same content serves them.
- `settings/` (CST-19, CST-21, CST-22 sheet side):
  - `FieldSettingsModel` (pure: `screen`, `reorder`, `dropIndex`, `move`), `FieldSettingsScreen` + `FieldSettingsViewModel` (grip drag on a visible 44 dp handle).
  - `AddFieldModel` + `AddFieldDraft` (pure; `withType`, `check`, `add`), and `AddFieldSheetContent` (shared `AddFieldSheet`). `entityKey == null` gives the settings layout (`RCFieldAdd`); otherwise the record layout (`RCProfileAdd`, with the `어디에` cards).
- Tests: `src/test/.../feature/cast/`. `CastFx` provides the example doc, a deterministic `EditContext`, and `atLens(...)`, which stores a lens in `_app`.

**How to extend.**
- Add a screen in its own package: a pure `…Model` object with a test, a `CastViewModel` subclass whose writes go through `act`, and a thin composable. Register it in `castEntries`.
- A shared sheet's content takes `SheetScope` as receiver and gets its editor from `rememberCastEditor`.
- Cross-domain writes (character delete, restore, merge) must go through `domain:actions` (`CharacterActions`, `MergeActions`), never through `domain:cast` `refs/*` or `merge/*`.

## Done

Pure logic with tests (screens on top of it are under Partial):
- **CST-02 filter and search state**: `CastListModel` (tag chip, typing state, 초성, fallback to `전체` when a tag is gone). Tests: `CastListModelTest`.
- **CST-05 create rule**: `NewCharacter` (name only, next record number 013, no fields). Test: `CST-05 …`.
- **CST-09 / CST-20 value drafts**: `FieldDrafts` (three states, nine types, validity, untouched value kept) and `LongTextEdit`. Tests: `FieldDraftsTest`.
- **CST-10 / CST-11 sheet models**: `TimedEditModel`. Tests: TC-11-01 … TC-11-05, `시점 추가`, Plain mode, `계산` read-only, blank display name.
- **CST-19 / CST-21 settings models**: `FieldSettingsModel`, `AddFieldModel`. Tests: TC-F-14, TC-F-20, TC-13-16, `계산` source, same-name notice, options.
- **CST-12 tag edits**: `FixedFieldEdits`. Tests: `FixedFieldEditsTest`.

## Partial

All UI. Built and compiling, but not yet seen on a device.
- **CST-01 인물 목록**:
  - Built:
    - glass head (project name, `인물`, lens chip, `+`)
    - `목록 | 관계도` segmented row (hidden on `EmptyCast`, as drawn)
    - chips `전체 N` + tags
    - grouped rows: avatar letter, name, subtitle, first appearance, `숨김` mark
    - `LockRow`
    - `EmptyCast`
    - command bar with mic
    - inner `TwoCast` (shell list/detail with the selected row) and `ColCast` (`CenterColumn`)
  - Left:
    - `관계도` pushes the `RelationMap` route (placeholder until CST-33), instead of switching inside the list.
    - The header `목록 | 카드` toggle is CST-04.
    - `라이프 레코더에서` opens the unfiltered inbox (see Requests).
    - Portrait avatars wait for CST-13.
- **CST-02**: typing in the command bar swaps it for a search field. No results shows `결과 없음`.
- **CST-05**: `+` and `인물 추가` open the feature-local sheet `인물 추가` (`이름`, `넣기`), and the new record opens on `개요` (detail pane on inner). Other entry points (new-project step 4, recorder `인물로 만들기`) belong to other lanes.
- **CST-07 record head**:
  - Built:
    - `인물 기록 007 · 최신 1-7`, name, one-liner (both tap to edit)
    - field table: `머리` fields, `첫 등장`, `보이스`; `1-3부터 ◆`; dimmed `예정` rows and footnote; cover limit 6 with `항목 더 보기`
    - floating glass back button and lens pill; collapsed glass bar after scroll
    - tabs above the command bar
  - Left:
    - background art (CST-13)
    - record `⋯` menu (CST-39, CST-40, CST-45)
    - inner 4:3 art
- **CST-08 tabs**:
  - `개요`: fixed rows, `항목` list, `빈 항목 N · 보기`, `+ 항목` (opens `AddFieldSheet` in record mode), numbered `긴 글` sections that tap to edit.
  - `말투` (prose and 호칭 chips, read-only until CST-15), `게임 텍스트` (read-only until CST-16 and CST-17), `비밀` (secret rows editable, prose read-only until CST-18).
  - The `레코더 · 정리 안 됨 N ›` link is CST-49.
- **CST-09**: the shared value sheet has inputs per type, `알 수 없음`, `◇` fallback notice, `연결` pickers drawn in place, and a read-only `계산` result.
  - A `긴 글` holding `@` links or paren runs shows read-only text with `TODO_COPY_CAST_LONG_TEXT_RICH`: there is no rich editor in core yet.
- **CST-10**:
  - Built: `◆` (its own 44 dp target) opens the history sheet: rows in story order, in-effect highlight, `지금 시점보다 뒤 · 예정`, `시점 추가` (unit picker → value), and `<point> 값 고치기`, which stacks the value sheet. The value sheet has `지금 값`, the history line, two stacked buttons with sub-lines, and the past-lens banner with `최신으로`.
  - Left:
    - The past-lens banner is inside the sheet, not under the record head (see Defaults).
    - At a past lens, `LensEdit` draws the field as `1-5부터 · 다음 변화 2-5` above the typed value. The sheet shows `지금 값 | 등불 결사 · 1-3부터` (domain `TimedValues.editSheet`) instead; the `다음 변화` preview is not built.
- **CST-11**: the `표시 이름` row has its own value and history sheets with the same two buttons. A blank value means show `이름`.
- **CST-12**: the feature-local `태그` sheet: project tags as chips, a `이름` line with `넣기` that adds an existing or new tag, and `이 값 고치기`. Tag ordering and renaming live on the `TagSettings` route (not built; no source draws it).
- **CST-19**:
  - Built: `FieldSettings` screen with kind tabs `인물 · 장소 · 용어 · 세력`, the fixed-field box, project rows (`<종류>` in `ink2`, then the detail), a grip handle (drag; label `순서 바꾸기`), and `+ 항목`.
  - Left:
    - Tapping a row pushes `FieldDefEdit` (CST-23, placeholder).
    - The bundle row `항목 더하기 ›` is not tappable yet (CST-24).
    - The entry row in 프로젝트 설정 is library's (see Requests).
    - The grip has no keyboard or TalkBack alternative to dragging.
- **CST-20**: nine types in the `종류` grid and in the value inputs.
- **CST-21**: `AddFieldSheet` from settings: `이름` (with `◇ 같은 이름 항목 있음`), `내보내기 이름`, `종류` grid, type inputs (단위 + `값은 바꿔 계산 안 함`, 보기 list, `직접 쓰기`, 연결 target, 날짜 precision, 계산 source, `여러 개`), `보이는 곳` (필터 disabled off the three types), `비밀 항목`, and `넣기`. On inner screens it is the shell's centred sheet, not a side popover.
- **CST-28 관계 tab**:
  - Built:
    - `관계 · 최신 1-7` header with `관계도로 보기`
    - one block per partner: avatar, `세라 · 선배`, viewer-side summary, `◆`
    - steps with `◆ 예정` or `◆ 지금 시점보다 뒤`
    - dotted deleted-partner row
    - `숨긴 인물과의 관계 N` row
  - Left:
    - `+ 관계` opens `RelationEditSheet` (CST-29, placeholder).
    - `관계도로 보기` opens `RelationMap` (CST-33, placeholder).
    - The deleted row's `되살리기 · 이대로 두기` buttons come with CST-41 and CST-43.

## Defaults used

- **Fixed rows in `개요`** [PROPOSED here]: `표시 이름` is shown while it is set or has history, and `태그` while there are tags. With `보기` on, all four (`이름 · 표시 이름 · 한 줄 소개 · 태그`) appear so each can be edited. `빈 항목 N · 보기` is shown even at 0.
- **Fixed fields without history** (`이름`, `한 줄 소개`, `태그`): one primary `이 값 고치기`, as CST-09 proposes for fields without history.
- **Past-lens banner** (S11, Q3): shown at the top of the value sheet, where the edit happens, with `최신으로` resetting the lens. The record screen itself shows only the lens chip.
- **Relation step status** is drawn `◆ 예정` / `◆ 지금 시점보다 뒤` in `acc`, as the `Relations` board writes it.
- **Empty value row** reads `비어 있음` (`ink3`).
- **`이 인물만` mark**: `◇` after the label.
- **`인물 추가` sheet** (CST-05 [PROPOSED]): title `인물 추가`, `이름`, `넣기`.
- **Tag sheet** (CST-12 [PROPOSED]): typing a name equal to an existing tag picks that tag (names unique per project). New tags are created on save, in pick order.
- **`목록 | 관계도`**: `관계도` pushes `RelationMap(p)` until CST-33 puts the map inside the list screen.
- **Record frame**: no glass top bar while the head is visible (DS-28). The bar appears after 160 px of scroll or past the first item.
- **Value sheet of a `연결` field**: the item picker replaces the inputs in the same sheet, so the draft is kept.
- **`긴 글` plain editing**: one paragraph per line; unchanged paragraphs keep their block (key, memo kind, recorder source).
- **Undo labels**: the pressed button's copy (`이 값 고치기`, `여기서부터 바뀜 · 1-7`, `시점 추가`, `항목 넣기`, `순서 바꾸기`, `인물 추가`).

## Spec conflicts

- **TC-11-01 planned row**: the fixture's R3 holds `등불 결사 · 등불지기장`, so the row reads `예정 · 2-5 | 등불 결사 · 등불지기장`, not `| 등불지기장` (same as domain-cast.md). `ProfileModelTest` asserts the fixture.
- **CST-19 `출생 · 날짜 · 연도 · 머리`**: fixture conflict #13 makes 출생 not `머리`, so the row sub reads `날짜 · 연도`. `FieldSettingsTest` asserts the fixture.
- **CST-02 `ㄹㅇ`**: the domain matches 초성 anywhere in the name, so `ㄹㅇ` finds both 리엔 and 엘리오 (`ㅇㄹㅇ`). The spec gives only 리엔 as an example.

## Requests

- **core:navigation `RecorderInbox`**: the route takes only `filter: String?` (a chip id). It cannot express "this project + `정리 안 됨`", which empty-cast `라이프 레코더에서` needs (`domain:recorder` `InboxOpening.FromProject`), nor "this character" for CST-49. Please add a project/target argument, or document a `filter` encoding. Until then the button opens the unfiltered inbox.
- **feature:library (`ProjectSettings`)**: the `인물` section row `인물 항목` should push `FieldSettings(p)`, with sub `FieldSettings.entryRow(doc)` from `domain:cast` (`게임 프로필 · 고정 7 · 프로젝트 11 · 장소 · 용어 · 세력도`) and `고치기 ›`.
- **core:ui or core:designsystem: a rich text editor** for `긴 글` values, the 말투 and 비밀 prose, and archive documents (`@` links, paren runs, `⌁` memo paragraphs). Until then those values are editable only when they hold plain paragraphs.
- **Copy needed**: `TODO_COPY_CAST_LONG_TEXT_RICH`, the note shown under a `긴 글` value that holds `@` links and cannot be edited as plain text yet.
- **FEATURES.md**: update CST-01 02 05 07 08 09 10 11 12 19 20 21 28 from this file (partial with notes as above).
