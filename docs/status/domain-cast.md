# domain:cast

Package root `io.github.lenticualrstone.civilight.domain.cast` (pure Kotlin over `core:model`, using `domain:story` for unit codes, the lens view and scene times). `tools/gw :domain:cast:test` passes (exit 0): 125 tests, 120 run green, 5 ignored as spec conflicts (listed below), 0 failures.

Edits take `EditContext` = `domain:story`'s `scene.EditContext` (typealias), so one context serves story, script and cast edits.

## Done

Tests are in `domain/cast/src/test/kotlin/io/github/lenticualrstone/civilight/domain/cast/`. Every case of 30-cast.md §4 that has a cast-owned outcome is a test named after its id.

**Relations (rule 13): CST-28 … CST-35**
- `relation/RelationReader.kt` (state at a read point, `만나기 전`, pair roles), `relation/RelationViews.kt` (viewer side, `관계` tab, steps, `예정` rows, 호칭 chips), `relation/RelationEdits.kt` (one relation per pair, pair kinds fill the other side, minds never filled, 호칭 creates an empty relation, records from a block, the two edit buttons), `relation/RelationKinds.kt` (default list, settings screen, usage, add, rename, pair rename, delete with fallback text, switch preview), `relation/RelationPanel.kt` (script `참고` rows, `◆ 여기서`, `리엔을 | ‘꼬마’`), `relation/CanvasRelations.kt` (line-label sheet, `그냥 선으로 두기`, `관계로 만들기`).
- `map/RelationMap.kt` (lines at the lens, solid/dashed, middle and end labels, filters, node set, `N명 중 M명 · 숨김 K`, `고른 관계` card), `map/MapLayout.kt` (deterministic layout, saved positions, lines clipped to cards, label anchors).
- Tests: `Rule13RelationsTest` TC-13-01 … TC-13-21 (incl. a pair-kind creation variant of TC-13-02, the `참고` 호칭 rows in TC-13-07, applying a kind switch, and both edit buttons on a relation slot with a record in effect); `MapAndKindsTest` (map nodes and header, `고른 관계` steps, tag and kind filters, layout geometry, kind settings, new kinds, seed list).

**Rule 11 on cast values: CST-10, CST-11**
- `timed/ReadPoint.kt`, `timed/TimedValues.kt` (history rows, `예정` / `지금 시점보다 뒤`, `이 값 고치기`, `여기서부터 바뀜`, edit and history sheet copy), `DisplayNames.kt` (`???` until the reveal, list note, card, panels, speaker label). The `여기서부터 바뀜` anchor and record come from `domain:story` `LensEdit.newAnchor` / `LensEdit.changeFromHere` (DECISIONS §1); cast has no anchor logic of its own for the two edit buttons (other actions keep theirs: `DisplayNames.revealAt`, `FieldValueEdits.addPointRecord`, `CanvasRelations.startUnit`). `이 값 고치기` keeps cast's base writers (`setBase`).
- Tests: `Rule11FieldHistoryTest` TC-11-01 … TC-11-07, plus display-name edits (`이 값 고치기`, `여기서부터 바뀜`, `revealAt`); `LensEditDelegationTest` (the anchor equals `LensEdit.newAnchor` at 최신, a past lens, with nothing written, with no units and on a lens unit that no longer exists; with nothing written `여기서부터 바뀜` adds a record at the first top-level unit and the sheet's second button matches `LensEdit.choice`).

**Field engine: CST-07 … CST-09, CST-12, CST-18 … CST-27**
- `field/FieldTypes.kt` (9 types, filter/sort/search abilities, allowed conversions, `연결` targets), `field/FieldDefinitions.kt` (`항목 넣기`, same-name notice, `이 인물만` / `모든 인물에 넣기`, rename, reorder, unit, places, secret, hide, options, option delete with fallback, delete to the trash and restore), `field/FieldConversion.kt` (type changes with `◇ 바꾸지 못한 값 N`), `field/FieldValueEdits.kt` (three states, empty never stored, two edit buttons, `연결` fixes), `field/FieldReader.kt` (values over time, `계산` age, derived birthday), `field/StoryDates.kt` (world date at a read point over `SceneTimes`), `field/FieldText.kt` (`17세`, `??세`, `?? cm`, `154 cm`, labels, `◇` notices, dotted links), `field/FieldBundles.kt` (`기본 · 게임 프로필 · 비어 있음 · 다른 프로젝트에서`, add missing only), `field/FieldSettings.kt` (settings screen and entry row), `field/Profile.kt` (record header, cover limit, `개요`, `비밀` tab, list subtitle, script panel line), `field/FieldSearch.kt` (value hits, long text by sentence), `field/FieldFilters.kt` (filter chips, range, `<항목>순` sorting), `export/CharacterExport.kt` (`characters[]` fields by export name, per-point lists, secret switch, hidden characters, 호칭-only option).
- Tests: `CustomFieldsTest` TC-F-01 … TC-F-26 plus secret fields, name check, settings rows, filters and sort, sentence search, empty never stored.

**List, record, game text, recorder: CST-01, CST-02, CST-04, CST-05, CST-12, CST-14, CST-16, CST-17, CST-49**
- `list/CastList.kt` (chips `전체 N`, groups by first tag, `LockRow`, search by name, 초성 and tag, `EmptyCast`, card rows `<tag> · <n>`), `list/Hangul.kt`, `FirstAppearance.kt`, `CharacterEdits.kt` (create with the next record number, tags, voice code, hiding flag, `카드에 넣기` memos, `인물로 만들기`), `voice/VoiceLines.kt` (states, progress, check row, edits), `voice/ArchiveDocs.kt` (rows, character count, unlock).
- Tests: `CastViewsTest`.

**Deleting, restoring, merging (rule 20): CST-40 … CST-46, cast side**
- `ItemRefs.kt` (reference resolution: hidden, live, trashed, name tag, missing), `refs/References.kt` (every key reference with its container's liveness), `refs/DeleteImpact.kt` (impact sheet, toasts, several at once), `refs/DeletedItems.kt` (check row, trash row, bulk speaker change and undo, restore report, purge with name tag, same-name notice), `refs/RefRewriter.kt`, `merge/MergePreview.kt` + `merge/CharacterMerge.kt` (comparison sheet, per-row choices, references moved, relations folded with both sets of `◆`, mutual relation deleted, `합침 → <target>` trash entry, undo, restoring the merged item).
- Tests: `Rule20CastTest` TC-20-01 … TC-20-16, TC-20-21, TC-20-23; `Rule20MergeTest` TC-20-17 … TC-20-19, plus a merge into a target whose value lives only in `◆` records (리엔's 소속: never filled, history kept).

**Hiding (rule 19): CST-39, cast side**
- `CastHiding.kt` (`숨긴 항목 변화 N`), hiding handled in `ItemRefs`, `CastList`, `RelationViews`, `RelationPanel`, `RelationMap`, `CharacterExport`.
- Tests: `Rule19HidingTest` TC-19-01 … TC-19-04, TC-19-06 … TC-19-08.

## Partial

- **Cases with no cast-owned outcome** are not written as cast tests: TC-20-20 (trash filters; `domain:trash`), TC-20-22 (`글로 풀기`; `domain:trash` RunRewrite), TC-20-24, TC-20-25, TC-20-26 (pins, sub-maps, route ends; `domain:trash` / `domain:world`), TC-20-27 (term reveals on merge; `domain:trash`), TC-20-28 (`이대로 두기` ack; `domain:trash` / `domain:checks`), TC-19-05 and TC-19-09 (check results; `domain:checks`). Parts of other cases owned elsewhere are left to those modules: TC-20-02 JSON/CSV speaker names and TC-20-21 automatic voice IDs (`domain:script`), TC-20-14 pins, routes and sub-maps (`domain:world`), TC-20-15 held `▲` row (`domain:checks`), TC-20-16 timeline and speaker-picker top row (`domain:world`, `domain:script`), TC-19-03 picker bottom row and `@` candidates UI, TC-19-04 export sheet row, TC-19-07 dialogue-line search (`domain:script`).
- CST-03 filter/sort data and functions exist; the controls' placement is open (Q13), no UI here.
- CST-06, CST-13 (image slots, crops, holo) and CST-15 prose editing are UI / image-area work; cast gives the data (`CastCard.holo`, `speechBody`, 호칭 chips).
- CST-36 (faction relations, P2), CST-50 … CST-52 (P2) not built.
- CST-37 speaker picker: `domain:script` owns the picker; cast supplies `CastList.search`, `DisplayNames.pickerNote` and `ItemRefs`.
- `StoryDates` years: `연도만` maps story days to years by 365-day years (Q20 default); a lens on an episode reads its last timed non-flashback scene.

## Defaults used

- **Display name**: one character-level history (Q4); `displayNameBase` and `displayName` records with a null value = show `이름` (CST-11 [PROPOSED]); list note `표시 이름 ???` without a reveal.
- **Lens labels**: section headers `관계 · 시점 1-7` at a past lens and `관계 · 최신 1-7` at `최신`; record line `인물 기록 007 · 최신 1-7`.
- **Relation summary**: `→ own · ← other`; a description is added only when exactly one mind is set (`→ 의지 · 의지하는 선배`); `마음 적지 않음` with no mind; `만나기 전` before any value.
- **Steps**: records grouped by their point label; 호칭 records shown only when the point has nothing else; status `예정` past the 집필 선, else `지금 시점보다 뒤` after the lens (Q19).
- **Map**: nodes = characters with a drawn line at the lens after filters (Q14); pair kinds label both roles toward their holders `후배 ↔ 선배` (Q15); a tag group chip keeps lines whose both ends have the tag [PROPOSED]; layout is deterministic force-directed from a circle, saved positions win.
- **Kind delete** keeps `kindFallbackText` beside an existing description, shown `경계 · 불빛을 아는 낯선 사람 ◇` (Q16); a pair kind keeps `선배 ↔ 후배`.
- **Kind switch** (Q29): the preview lists the affected pairs and `바뀌는 관계 N`; applying changes only the kind, slots keep their kind key.
- **Canvas `관계로 만들기`** starts at `unitStart` of the lens episode (`최신` = the 집필 선 episode); `사이` from a canvas line makes the drawn-to end hold a pair kind's first name.
- **`여기서부터 바뀜`** follows `LensEdit` (2026-11-22): at `최신` with nothing written it adds a `◆` at the start of the first top-level unit (label `여기서부터 바뀜 · 전체`, sub `새 ◆`, LensEdit's copy) instead of falling back to `이 값 고치기`; with no units at all the anchor is `Anchor.Unplaced`, and a past lens whose unit no longer exists anchors at that unit's start (both LensEdit's behaviour, not decided here). The overwrite sub without a record is `처음 값을 고침`.
- **Age** (Q20): `연도만` = start year + ⌊(day − 1) ÷ 365⌋ with the earliest day of a range; `세계 달력` gives 만 나이 when the scene has a month and day, else year subtraction (TC-F-25 [PROPOSED]); a lens on a scene or a block reads that scene (a flashback reads its event's or its own year); untimed scenes take the nearest earlier timed scene; no 때 at all = the start year. `??` birth with nothing typed = `??세`.
- **Birthday** (Q7): derived and read-only while `출생` has month and day; bundle field `basic.birthday`, else a field named `생일`.
- **Bundle matching** (Q21): by `bundleOrigin`, else by name. Bundles for 장소 · 용어 · 세력 start empty. `다른 프로젝트에서` skips `이 인물만` fields.
- **Field text**: date `1183` (year) or `1183년 5월 10일` (world month names with a calendar); anniversary `3월 14일`; multiple values joined by ` · `.
- **Type changes**: options from short text in first-use order; multi-value short text cannot become a single `고르기`; fallback text converts to `짧은 글` as plain text.
- **Field filter/sort**: `??` and empty last; hidden, secret and `이 인물만` fields are not filters or sort orders; secret fields are not searched.
- **Field delete**: trash entry `fieldDef`, purge on local date + 30.
- **List**: a character appears under its first tag (Q13), untagged under `태그 없음`; a tag chip filters by any tag; 초성 matches anywhere in the name; card subtitle = up to two list fields + one-liner.
- **Archive count**: characters except line breaks, `1,120` grouping (CST-17 [PROPOSED]).
- **Voice lines**: `비어 있음` derived from empty text; tapping the state toggles `쓰는 중 ↔ 녹음 대본 완료`.
- **Export JSON** (LIB-45 skeleton): `fields` values are text, number, option label(s), `{year, month?, day?}`, `{month, day}`, `[{id, name}]`, `??`, fallback text; per-point lists `[{from, value}]` with `from` = `1-3`, `1-7 #043`, `2장` or null for the base; `계산` exported only when typed; 호칭-only option as `addresses: { <partner id>: value | [{from, value}] }` (Q29).
- **Merge**: a cell is `빈 칸 채움` only when the target has neither a base value nor live `◆` records; a target whose value lives in records gets a `다른 칸` row (target default, sheet text = value at `최신`). Taking the merged value moves the merged item's records to the target and keeps the target's own records, except one at the very same point. `MergeRecord.mergedOriginal` is the merged character row JSON and `ext.summary` holds `{refs, relations, fields}`; the undo snapshot lives in the in-memory `MergeResult`. Unknown age compares as `??세`.
- **Recorder**: `인물로 만들기` puts the memo into the first non-secret `긴 글` field (the model has no separate 개요 body).

## Spec conflicts

Each is kept as stated in an `@Ignore("spec conflict: …")` test; a runnable variant follows the fixture.

- **TC-11-01**: Given R3 = `등불 결사 · 등불지기장`, Then the planned row reads `예정 · 2-5 | 등불지기장`. Variant: the row shows the record's value `등불 결사 · 등불지기장`.
- **TC-F-24 / TC-20-13**: 소속 is `연결 · 세력` in the cases (CST-19, CST-24) but `짧은 글` in the fixture (fixture.md #10). Variants use a synthetic `연결` field named 소속.
- **TC-19-02**: expects `12명 중 5명`; the fixture's four live pairs give 4 nodes (`인물 · 12명 중 4명 · 숨김 1`), which matches the cover board's `11명 중 4명`.
- **TC-20-19**: expects `칸 3`; TC-20-17's defaults take exactly two cells (`빈 칸 채움 · 출생 · 성격 · 2`), so the line reads `칸 2`.
- **TC-20-17** (noted, test runs): the Given (시장 꼬마's 출생 filled, 나이 `??`; 노아's 출생 empty, 나이 `11세`) cannot display as stated after the merge under CST-25: 출생 1189 is filled into 노아, so 노아's 나이 is computed (1200 − 1189 = 11), not read from the typed 11. The test asserts the stored values.
- **Not a test conflict, noted**: TC-20-11's `대사 40` is sample data; the test checks the format with the fixture's sum. CST-14's `18 / 24` is board data; the fixture gives `3 / 5`. CST-19's `프로젝트 7` vs 11 fields (fixture.md #12): the entry row prints the real count.

## Requests

- (Decided 2026-11-22, DECISIONS §4: merges run on `domain:trash` `MergeOps` through `domain:actions` `MergeActions`; the cast comparison sheet `MergePlanner.preview` and `MergeChoices` stay the UI input, `CharacterMerge` is no longer on a write path. `MergeOps` now follows cast's record rules: a record-only value is not empty, taken fields bring their `◆` records.) **domain:trash** owns delete, restore, purge and a cross-kind merge (`MergeOps`, `Impact`, `ReferenceScanner`). Cast has its own character-side versions (`refs/*`, `merge/*`) because it cannot import trash. They agree on `TrashEntry` (reason `merged` + `mergedIntoKey`), `MergeRecord.mergedOriginal` (character JSON) and `ext.summary`, so either can restore the other's merge; `movedRefs` field strings differ (cast `target`, trash `target.relationKey`), so undo must go through the module that merged. The feature layer should pick one path per action (suggested: trash for writes, cast for the cast-specific sheet text such as `나이 ??세 | 11세` and viewer-side relation rows).
- **domain:world** has its own `time/Ages.kt`; ages should come from one place. Cast's `StoryDateSource` is a `fun interface`, so a shared resolver can replace `StoryDates.Default`.
- **domain:script**: the speaker picker should use `CastList.search` (hiding, trash, 초성) and `DisplayNames.pickerNote`; the 참고 panel can use `RelationPanel.rows/addressRows` and `Profile.panelLine`.
- **Exporter (LIB-45)**: `CharacterExport.entry/characters` gives the `characters[]` profile part; the 호칭-only key `addresses` is a placeholder until LIB-45 defines it.
- ~~**domain:story**: `timed/TimedValues` and story's `lens/LensEdit` both implement the two edit buttons~~ Done (2026-11-22): `TimedValues.changeFromHereAnchor` / `changeFromHere` delegate to `LensEdit` (public API unchanged; `changeFromHereAnchor` is never null now and `changeFromHere`'s `setBase` is unused). Still open for the spec owner: the sheet copy differs between the modules (cast `처음 값을 고침` vs LensEdit `기본값을 고침`; past-lens sub cast `새 ◆ · 1-5부터` vs LensEdit `새 ◆ · 1-5 #—`; cast's banner has title and sub, LensEdit's is one line), and `LensEdit.newAnchor` gives `Unplaced` with no units and a dangling `unitStart` for a lens on a missing unit (cast used to offer only `이 값 고치기` there).
- ~~**domain:trash**: snapshot at delete time~~ Done (2026-11-22) in `domain:actions` `CharacterActions.delete` (writes `DeletedItems.withSnapshot` into the entry) and `CharacterActions.restore` (toast from `DeletedItems.restoreReport`). Original request: `DeletedItems.restoreReport` needs the references at delete time (`DeletedItems.snapshot`, stored with `withSnapshot` in `TrashEntry.ext["castRefs"]`); trash's delete does not write it yet, so the restore toast's `그대로 둔 것` counts need trash to call `snapshot` when deleting a character.
