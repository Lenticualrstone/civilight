# feature:cast

Package `io.github.lenticualrstone.civilight.feature.cast` (module `feature/cast`). Batches 1 to 4 of 4 done (this lane is finished).
- Batch 1: CST-01 02 05 07 08 09 10 11 12 19 20 21 28.
- Batch 2: CST-29 30 40 41 43 03 04 06 13 14 15 16 17.
- Batch 3: CST-18 22 23 24 25 26 27 31 32 33 34 35 36.
- Batch 4: CST-37 38 39 42 44 45 46 47 48 49 50 51 52, plus the `TagSettings` route and the dotted `연결` fix buttons.

**Build and tests.** `tools/gw :feature:cast:compileDebugKotlin :feature:cast:testDebugUnitTest :domain:cast:test` exits 0 (batch 4 final run). feature:cast: 134 JVM tests in 19 classes, all passing (counts from the test-result XML): `CastCopyLintTest` 3, `CastListModelTest` 9, `ProfileModelTest` 12, `TimedEditModelTest` 12, `FieldDraftsTest` 9, `FieldSettingsTest` 8, `FixedFieldEditsTest` 4, `ItemTrashTest` 8, `RelationSheetModelTest` 6, `CastFilterModelTest` 5, `GameTextModelTest` 5, `CharacterImagesTest` 3, `FieldDefEditTest` 10, `SettingsBatch3Test` 7, `RelationMapModelTest` 10, and from batch 4 `MergeModelTest` 7, `DeletedItemModelTest` 7, `HidingAndRecorderTest` 6, `TagSettingsTest` 3. `domain:cast` was changed additively (batch 2: one new file; batch 3: three new files and one test class; batch 4: `script/SpeakerData.kt` and `SpeakerDataTest`): 142 tests, 137 green, 5 ignored as before (spec conflicts in domain-cast.md), 0 failures. Nothing was run on a device or emulator, and no APK was built.

## Architecture

**Entries** (`CastEntries.kt`, `castEntries`): registers routes and shared sheets only. Every route of the `cast` row and every CAST sheet is registered.
- Routes: `CastList`, `CharacterProfile`, `FieldSettings` (imported as `FieldSettingsRoute`, because `domain:cast` also has a `FieldSettings`), `ArchiveDocEdit`, `FieldDefEdit`, `RelationKindSettings`, `RelationMap`, and since batch 4 `TagSettings`.
- Shared sheets: `FieldValueEditSheet`, `FieldHistorySheet`, `AddFieldSheet`, `RelationEditSheet`, `DeleteImpactSheet`, and since batch 4 `MergeItemsSheet`.
- `RelationChangeContent` (CST-30) still waits for a core:navigation key that carries a block (Requests).

**Packages** (one folder per screen or flow):
- `CastUiCopy.kt`: the only UI copy object (DECISIONS §3).
  - It holds only copy that the `domain:cast` builders (`CastList`, `Profile`, `TimedValues`, `RelationViews`, `RelationDrafts`, `FieldSettings`, `CastCopy`) and the `domain:trash` `Messages` do not already produce. Domain strings are shown as they come.
  - `BTN_…` constants are buttons (linted as buttons). Formatted copy is functions (`since`, `emptyRow`, `recordSub`, `menuLabel` …).
  - `TODO_COPY_…` constants are missing copy (see Requests).
  - `CastCopyLintTest` lints every constant and list by reflection, plus each function with example-project arguments. A new function needs a line in `formatted()`. Pure models that need copy (`CharacterImages`, `CastFilterModel`) point at `CastUiCopy` constants, so the lint covers them.
- `common/`:
  - `CastViewModel`: a `ProjectViewModel` with `apply(label) { doc, ctx -> doc' }` (suspend) and `act(label, then) { doc, ctx -> doc' }` (fire and forget). Each write gets a fresh `EditContext` from `FeatureDeps.editContext()` (shell clock, zone and device id) and is one undo step named by its button's copy. A read-only project returns `EditResult.READ_ONLY`; it never throws.
  - `CastWrites` (batch 2): writes that must outlive their screen, on `deps.appScope` with their own project session (`launch(label, compute, mergeKey, then)`, `revert(label, DocChange)`). Use it whenever the screen may be gone when the write finishes or when a toast's `되돌리기` is pressed: deletes (the record pops after its own delete), restores, the archive editor's save-on-leave. `EditContext.toAction()` / `FeatureDeps.actionContext()` give the `domain:actions` `ActionContext`.
  - `CastEditorViewModel`: the editor behind shared sheets. Sheets get it with `rememberCastEditor(projectKey)` (`field/ValueSheetParts.kt`), which keys it inside whichever entry draws the sheet.
  - `castLens` (`ProjectDoc.castLens`): the stored lens through `LensRules.effective`. Every cast read uses it.
  - `RecordFrame`: the record's frame. A floating glass back button, lens pill, the tab's `+` (`add`) and the record `⋯` (`menu`) sit over the head; the glass top bar (name and record number, same actions) appears once the name scrolls away; tabs sit above the command bar. It also draws toasts and sheets.
  - `HiddenMark`: the `숨김` tag.
  - `SavingTextField` (batch 3): a settings text field that writes after a typing pause (600 ms) and once more when it leaves; it never re-seeds from the document while typing. Pass a `CastWrites` write with a `mergeKey`, so a typing run is one undo step.
- `list/` (CST-01 … CST-05):
  - `CastListModel` (pure; `CastListUi` holds the chip, query, typing state, `cards` and `filters`; `CastListState` adds `cardGroups`, `filters`, `activeFilters`, `canFilter`), `CastListViewModel` (`create`), `NewCharacter` (pure).
  - `CastFilterModel` (pure, CST-03): `FieldFilterState` (picked options per field, `숫자` ranges, sort field; saved with the screen as JSON through `FilterStateCodec`), `sheet`, `clean`, `apply` (filters rows; a sort makes one section named `<항목>순`), `cards`, `activeFilters`.
  - `CastListScreen`; `NewCharacterSheet` (feature-local); `CastFilterSheet` + `CastFilterContent` (feature-local); `CastCards.kt` (`CardRow`, card sizes 196 / 132).
- `profile/` (CST-07, CST-08, CST-11 … CST-17, CST-28, CST-40 entry):
  - `ProfileModel` (pure; `ProfileUi` holds the tab, `보기` and `항목 더 보기`; it returns null for a trashed, missing or locked character, and the screen then pops). `ProfileState` also carries `deletedPartnerRelations` and `keptPartners` for the dotted rows.
  - `ProfileScreen` + `ProfileViewModel`. `ProfileTabs.kt` has one composable per tab: `OverviewTab`, `RelationsTab`, `SpeechTab`, `GameTextTab`, `SecretTab`. `RecordRows.kt` has `RecordFieldRow` and `PlannedRow`. `RecordArt.kt` draws the background art. `RecordMenu.kt` is the record `⋯` (feature-local `RecordMenuSheet`).
  - `FixedFieldEdits` (pure; `이름`, `한 줄 소개`, tags) and `FixedFieldSheets.kt` (feature-local `NameEditSheet`, `OneLinerEditSheet`, `TagsEditSheet`). `FixedField` now also has `PORTRAIT` and `BACKGROUND` (CST-13 slot rows).
  - `BodySheets.kt`: feature-local `BodyEditSheet` (`말투` / `비밀` prose in the shared rich editor; `SPEECH` from `말투`, `SECRET` from `비밀` since batch 3). The batch-2 `AddressEditSheet` is gone: 호칭 chips open `relation/RelationSlotSheet` (below).
  - `RecordActions` carries every row callback. Later batches add theirs there.
- `field/` (CST-09, CST-10, CST-11, CST-20):
  - `FieldDraft` / `FieldDrafts` (pure): one draft shape for all nine types, saved as JSON with `FieldDraftSaver`. `richValue` saves a `긴 글` from the rich editor.
  - `TimedEditModel` (pure): value and history views and saves for a `TimedSubject` (`Field` of any item kind, a character's `DisplayName`).
  - `LongTextEdit` (pure): plain-paragraph edits that keep block keys (still used by `시점 추가`).
  - Composables: `FieldValueEditor` (inputs by type; `richLongText` slot), `FieldValueSheet.kt` (shared `FieldValueEditSheet`, `긴 글` in the shared `RichTextEditor`; feature-local `DisplayNameEditSheet`), `HistorySheet.kt`, `ValueSheetParts.kt` (`TwoButtonHead`, `SaveButtons`, `HistoryRow`).
- `relation/` (batch 2; CST-29, CST-30):
  - `RelationSheetModel` (pure): `view` (title `리엔 · 세라`, slot chips, `지금 값`, kind chips, `RelationSave.FromStart` (`관계로 만들기` / `기록` with `시작 시점`) or `RelationSave.Edit` (Plain or two buttons from `LensEdit.choice`)), `initialDraft`, `canSave`, `save` (creates the pair's relation inside the save).
  - `RelationEditContent`: the shared `RelationEditSheet`. Only `endAKey` → partner picker first; `relationKey` alone → that relation from end A. Batch 3 split its body into `RelationEditBody(projectKey, viewer, partner?, initialSlot)`; the feature-local `RelationSlotSheet(projectKey, viewerKey, partnerKey?, slot)` opens one slot directly (the `말투` 호칭 chips and `+ 호칭`, CST-31), with the same one-or-two buttons.
  - `RelationChangeContent(projectKey, blockKey, viewerKey, partnerKey)`: the `관계` segment of `◆ 변화 기록`. Not reachable yet (see Requests).
  - Shared logic lives in `domain:cast` `relation/RelationDrafts.kt` (`SheetSlot`, `SlotDraft`, `KindChip`, `kindChips`, `slotName`, `draftOf`, `input`, `valueText`, `recordText`, `addKind`, `changeView`, `recordAtBlock`), so feature:script can use it too.
- `trash/` (batch 2; CST-40, CST-41, CST-43):
  - `ItemTrash` (pure): `impact` (`CharacterActions.impact` / `domain:trash` `Impact.of`), `sheet` (`Messages.impactSheet` split into label | count, staying title with the right particle), `delete` (`CharacterActions.delete` / `TrashOps.delete` + `Messages.deleteToast`), `restore` (`CharacterActions.restore` / `RestoreOps.restore`), `keep` / `isKept` (`Fixes.keep` with the referencing relation).
  - `ItemTrashFlow` + `rememberItemTrash(projectKey)`: `requestDelete` (sheet or delete at once), `delete` (toast with `되돌리기` = row-level revert), `restore` (biometric prompt on `NeedsAuth`, then retry), `keep`. All through `CastWrites`.
  - `DeleteImpactContent`: the shared `DeleteImpactSheet` for all four kinds.
- `gametext/` (batch 2; CST-16, CST-17):
  - `GameTextModel` (pure): `addLine`, `saveLine`, `archiveView`, `saveArchive` (creates on the first save), `setUnlock`.
  - `VoiceLineSheet` + `VoiceLineContent` (feature-local): slot name + text in the rich editor; `넣기` / `이 값 고치기`.
  - `ArchiveDocScreen` (`ArchiveDocEdit` route): `이름`, `열리는 시점` (unit picker), rich body, character count; saves as you type (600 ms, one undo step per typing run via `mergeKey`) and once more when the screen leaves.
- `image/` (batch 2; CST-13):
  - `CharacterImages` (pure): `ImagePlace` (avatar 1:1, card 4:5, background 9:16 cover / 4:3 inner), `image`, `background`, `slotRows`, preset ids and names.
  - `PresetPainter` (the 8 presets as shard art, palettes from the board generator) and `rememberSlotPainter` (blob decoded off the main thread with the place's crop).
- `settings/` (CST-19, CST-21, CST-14 switches; batch 3 CST-22, CST-23, CST-24):
  - `FieldSettingsModel` (pure: `screen`, `reorder`, `dropIndex`, `move`, `showsInHeader`, `setShowInHeader`), `FieldSettingsScreen` + `FieldSettingsViewModel`. The `시작 묶음` row now opens `BundleSheet`.
  - `AddFieldModel` + `AddFieldDraft` (pure), and `AddFieldSheetContent` (shared `AddFieldSheet`). From a record it draws the `종류 | 짧은 글 ›` row with the type list in place (`RCProfileAdd`), from settings the radio grid (`RCFieldAdd`).
  - `FieldDefEditModel` (pure: `view`, rename, export name, unit, options, type change `preview` / `convert`) and `FieldDefEditScreen` (`FieldDefEdit` route). Confirms are DS `ConfirmDialog`s whose pending choice is saved as a string. Delete goes through `ItemTrash.deleteFieldDef` (`domain:trash` `TrashOps`).
  - `BundleModel` (pure: `rows`, `preview`, `previewFrom`, `apply`, `applyFrom`, `sources`) and `BundleSheet` (feature-local, from `항목 더하기 ›`). `다른 프로젝트에서` reads the source through `ProjectSessions.acquire` / `release`.
- `kinds/` (batch 3; CST-32): `RelationKindModel` (pure; `KindShape` = `사이 / 마음`, `대칭 / 짝` with names) and `RelationKindSettingsScreen` (`RelationKindSettings` route) with the feature-local `NewKindSheet` and `KindEditSheet`.
- `map/` (batch 3; CST-33, CST-34): `RelationMapModel` (pure: `MapUi` saved as JSON, `build` → `RelationMapState` with the `ui.graph` `GraphScene`, tag and kind chips, `고른 관계`; `tapNode` / `tapLine` / `tapEmpty`, line tool, `normalized` for saving), `RelationMapScreen` (`RelationMap` route; `GraphSurface`), and `MapSheets.kt` (feature-local `MapFilterSheet`, `MapLineSheet`, `SelectedRelationSheet`).
- `merge/` (batch 4; CST-45, CST-46, WLD-15): `MergeModel` (pure: `canMerge`, `view` → `MergeView` with `MergeCellView` rows for characters through `MergeActions.characterSheet` and for places, terms, factions through `MergeActions.itemPreview` + `domain:world` rows; picks are a saved set of cell ids `f:<field>`, `c:<cell>`, `r:<partner>:<slot>`, `reveal`; `characterChoices` / `itemChoices`; `merge` → `MergeActions.mergeCharacters` / `mergeItems`), `MergeFlow` (`rememberMergeFlow`: the write through `CastWrites`, the recorder library through `stores.recorder.updateLibrary` with `MergeOps.applyToRecorder`, toast `… 합침 · 참조 N` with `되돌리기` reverting both), `MergeSheet.kt` (`MergeItemsContent`, the shared `MergeItemsSheet`).
  - Sheet key contract: `MergeItemsSheet(p, kind, keepKey, otherKey)` merges `otherKey` into `keepKey` (the one that stays). With `keepKey == otherKey` the sheet starts with the picker of that kind (`CharacterPickerSheet` for characters, as CST-45 asks; place, term, faction pickers otherwise), so a caller that knows only the item opens it that way.
- `trash/` additions (batch 4; CST-42, CST-44): `DeletedItemModel` (pure: the item's `domain:trash` `DeletedItems` row, `DeletedItemView`, `BulkKind` `SPEAKER` (`Fixes.replaceSpeaker`) and `LINK` (a local workaround: `domain:cast` `RefRewriter.retargetLinks` on the in-project link keys `ReferenceScanner` finds; the button shows only while such links exist, since the row's `@` count also holds recorder text links), `preview` (`대사 14 · 노아 → 오웬`), `apply` → `BulkChanged` with its `DocChange`), `DeletedItemContent` (row → picker → preview → `바꾸기`), `ItemTrash.keepItem` / `ItemTrashFlow.keepItem` (item-level `이대로 두기`).
  - `DeleteImpactContent` now has two modes. An item live when the sheet opens shows the impact sheet (with `다른 인물과 합치기 ›`, which closes it and opens `MergeItemsSheet(p, kind, key, key)`); an item already in the trash or left as a name tag shows its check row (`DeletedItemContent`). Other lanes (the check center) can open `DeleteImpactSheet(p, kind, key)` for a deleted item to get the bulk change.
- `profile/` additions (batch 4): `RecordMenuContent(name, hidden, onHide, onDelete, onMerge)` (`숨기기` / `숨김 풀기`, `지우기`, `합치기`); `ProfileState.hidden` (the `숨김` tag beside the record number) and `.sameName` (`◇ 같은 이름 2 · 노아` row with `합치기` at the top of `개요`); `RecorderLink.kt` (`RecorderLinkModel` pure + `rememberRecorderLink`: `레코더 · 정리 안 됨 n ›` right of the first `긴 글` section header, or on its own line without one; it pushes `RecorderInbox(p, "character", c)`); the dotted partner name opens `DeleteImpactSheet` for that partner (its check row).
- `field/LinkFixes.kt` (batch 4; CST-41, CST-47): `DottedLinkRow` per dotted `연결` value in the value sheet (`지운 세력 · 상층 경비대` with `되살리기` while in the trash, `이대로 두기` until chosen; `영구 삭제 · …` without `되살리기`). `FieldValueEditor` takes `dottedLinks`, `onRestoreLink`, `onKeepLink`.
- `settings/` additions (batch 4; CST-12): `TagSettingsModel` (pure: `rows` `주연 · 3`, `canName`, `rename`, `add`) and `TagSettingsScreen` (`TagSettings` route; feature-local `TagNameSheet`).
- `domain:cast` `script/SpeakerData.kt` (batch 4; CST-37, CST-38, for the script lane): `fieldChips` (`소속 · 등불 결사` per `필터` option) and `filter`, and `panel(doc, speaker, block, hiddenUnlocked)` → `SpeakerPanel` (`화자 · …` head line, one-line `말투`, `??? (1-9 공개)`, 호칭 rows, `RelationPanel` rows, `숨긴 인물과의 관계 N`).
- Tests: `src/test/.../feature/cast/`. `CastFx` provides the example doc, a deterministic `EditContext` (`ctx`) and `ActionContext` (`action`), `atLens(...)`, `noahLive`, `addCharacter` and `key`.

**How to extend.**
- Add a screen in its own package: a pure `…Model` object with a test, a `CastViewModel` subclass whose writes go through `act`, and a thin composable. Register it in `castEntries`.
- A shared sheet's content takes `SheetScope` as receiver and gets its editor from `rememberCastEditor`.
- A write whose screen may leave first (deletes, merges, anything with a `되돌리기` toast) goes through `CastWrites`.
- Cross-domain writes (character delete, restore, merge) go through `domain:actions` (`CharacterActions`, `MergeActions`), never through `domain:cast` `refs/*` or `merge/*`. Use `ItemTrash` / `ItemTrashFlow` for delete and restore.
- Record `⋯` rows live in `RecordMenuContent`; merges go through `MergeItemsSheet` (picker first when `keepKey == otherKey`) and `MergeFlow`, never by calling `MergeActions` from a screen.
- Settings-like screens that write as you type use `SavingTextField` + `CastWrites` with a `mergeKey`; switches and chips write through the view model's `act` (one undo step each). Confirms use the DS `ConfirmDialog`.
- The relationship map is its own route (`RelationMap`), not a mode inside `CastList`: `CastList` is a 390 dp list pane on inner screens, while `CastMap` is drawn 800 wide. The cast list's `목록 | 관계도` pushes it; the map's `목록` returns to the list.

## Done

Pure logic with tests (screens on top of it are under Partial):
- **CST-02 filter and search state**: `CastListModel`. Tests: `CastListModelTest`.
- **CST-03 field filters and sort**: `CastFilterModel` (options of `필터` fields, `숫자` ranges, `<항목>순` with `??` and empty last, stale fields dropped). Tests: `CastFilterModelTest` (CST-03 ×3, stale state and codec).
- **CST-05 create rule**: `NewCharacter`. Test: `CST-05 …`.
- **CST-09 / CST-20 value drafts**: `FieldDrafts` (incl. `richValue`) and `LongTextEdit`. Tests: `FieldDraftsTest`.
- **CST-10 / CST-11 sheet models**: `TimedEditModel`. Tests: TC-11-01 … TC-11-05 and others.
- **CST-12 tag edits**: `FixedFieldEdits`. Tests: `FixedFieldEditsTest`.
- **CST-13 slots and crops**: `CharacterImages`. Tests: `CharacterImagesTest`.
- **CST-14 header switches**: `FieldSettingsModel.setShowInHeader` over `CastSettings.showFirstAppearanceInHeader` / `showVoiceProgressInHeader`. Test: `GameTextModelTest.CST-14 …`.
- **CST-16 / CST-17 edits**: `GameTextModel`. Tests: `GameTextModelTest`.
- **CST-19 / CST-21 settings models**: `FieldSettingsModel`, `AddFieldModel`. Tests: `FieldSettingsTest`.
- **CST-29 relation sheet**: `RelationSheetModel` + `domain:cast` `RelationDrafts`. Tests: `RelationSheetModelTest` (TC-13-10, TC-13-19, TC-13-04, pair kind, empty slot and a filled slot through `LensEdit` at two lenses, self pair) and `domain:cast` `RelationDraftsTest` (8: chips, TC-13-02, TC-13-04, clear and change, CST-30 view, TC-13-08, TC-13-19, new kind).
- **CST-30 change from a block, logic**: `RelationDrafts.changeView` / `recordAtBlock` (values read just before the block, `만나기 전` first, records at the block, relation created when missing). Tests above.
- **CST-40 / CST-41 / CST-43 logic**: `ItemTrash`. Tests: `ItemTrashTest` (노아's sheet, particle, gone item, TC-20-03, delete and revert, restore toast, hidden restore needs auth, `이대로 두기` on one row) and `ProfileModelTest.CST-41 …` (오웬's dotted 노아 row finds `REL_OWEN_NOAH`; keeping it hides only its buttons). The `[partner, relation]` acknowledgement matches no check-center result (those use `[item]`), so it adds no row to `맞다고 표시한 것`; restoring the partner removes it (`RestoreOps`).
- **domain:cast addition**: `relation/RelationDrafts.kt` (new file; nothing renamed or changed).
- **Batch 3, pure logic with tests**:
  - **CST-23 definition changes**: `FieldDefEditModel` over `FieldDefinitions` / `FieldConversion`; delete through `ItemTrash.deleteFieldDef` (`TrashOps`, toast `항목 신장 지움 · 되돌리기`). Tests: `FieldDefEditTest` (edit view, TC-F-05 preview before anything changes, TC-F-07, TC-F-08, TC-F-09, TC-F-10, TC-F-11, TC-F-12 delete and revert, TC-F-22).
  - **CST-22 `이 인물만` and `모든 인물에 넣기`**: `FieldDefEditTest.TC-F-14 …` (mark on the record row, value kept when promoted).
  - **CST-24 start bundles**: `BundleModel`. Tests: `SettingsBatch3Test` (picker rows, TC-F-15 adds only missing fields and changes nothing else, TC-F-16 hidden source only while unlocked and definitions only).
  - **CST-25 age**: computed `나이` read-only in the value sheet (`derived`), typed when `출생` is empty. Test `FieldDefEditTest.CST-25 …`; the rules are `domain:cast` (TC-F-01 … TC-F-04, TC-F-25).
  - **CST-26 export names**: rename keeps `height`, an empty export name falls back to the field name (`FieldDefEditTest.TC-F-10 TC-F-11 …` over `CharacterExport`). Secret fields stay out unless `비밀 항목 포함` (library's export sheet, `domain:library` `ExportSheet`).
  - **CST-27 field search in the cast list**: `CastListModel.fieldHits` over `FieldSearch`. Test `RelationMapModelTest.TC-F-19 …`.
  - **CST-31 호칭**: `RelationSheetModel` with `SheetSlot.ADDRESS`. Tests `RelationMapModelTest.TC-13-18 …` (two buttons, one value) and `TC-13-09 …` (empty relation, no map line, chip `마르타 → 결사장님`).
  - **CST-32 relation kinds**: `RelationKindModel`. Tests `SettingsBatch3Test` (new kinds incl. a pair needing both names, TC-13-11, TC-13-13, TC-13-20 preview before apply, TC-13-12).
  - **CST-33 / CST-34 map**: `RelationMapModel` + `domain:cast` `PairLines`, `MapPositions`. Tests `RelationMapModelTest` (TC-13-14 scene strokes and labels and the `사이` filter, inner header / cover card / chips, line tool and taps, saved and dragged positions) and `domain:cast` `PairLinesTest` (TC-13-15 map variant, pair-kind holder, positions in `ext`).
  - **CST-36 faction relations (P2, storage)**: `domain:cast` `FactionRelations`. Test `RelationMapModelTest.CST-36 …` (one per pair, deleted with the faction and back on revert, never on the map).
  - **CST-18 secret fields**: `FieldDefEditTest.CST-18 …` (only on `비밀`).
- **Batch 3 domain:cast additions** (new files only; nothing renamed or changed): `relation/PairLines.kt` (line-label sheet and `관계로 만들기` for a pair with no canvas edge), `relation/FactionRelations.kt`, `map/MapPositions.kt` (saved node positions in `project.cast.ext["relationMap"]`), test `PairLinesTest` (3).
- **Batch 4, pure logic with tests**:
  - **CST-38 panel data**: `domain:cast` `SpeakerData.panel` (the script lane renders it). Tests `SpeakerDataTest` (하르트 at #042 `리엔 · 만나기 전`, #043 `만나기 전 → 보호 ◆ 여기서` and `만나기 전 → 경계 ◆ 여기서`, #044 `리엔 · → 보호 · 호칭 ‘꼬마’` / `← 경계` / `리엔을 | ‘꼬마’`; TC-F-26 head line; one-line 말투; a locked hidden partner as `숨긴 인물과의 관계 1` and no panel for a locked hidden or deleted speaker).
  - **CST-37 field chips**: `SpeakerData.fieldChips` / `filter` (test above). The picker's other parts are `core:ui` `PickerModels` (shell Done).
  - **CST-45 / CST-46 merge**: `MergeModel` over `MergeActions`. Tests `MergeModelTest` (TC-20-17 sheet as `RCMerge` with target defaults, a picked cell maps to `MergeChoices`, TC-20-17 write and row-level revert, TC-20-18 `서로 관계 1 · 지워짐`, live / same-kind / not-itself guards, WLD-15 two places, CST-46 `◇ 같은 이름 2 · 리엔` on the record state).
  - **CST-42 / CST-44 check row and bulk change**: `DeletedItemModel`. Tests `DeletedItemModelTest` (TC-20-04 row and `대사 N · 노아 → 오웬` in one revertible step, `@` retarget, TC-20-28 `이대로 두기`, TC-20-06 `영구 삭제 · 노아` without `되살리기`).
  - **CST-41 / CST-47 dotted `연결` values**: `LinkFixes`. Test `DeletedItemModelTest.TC-20-13 variant …` (a deleted faction reads `지운 세력 · 상층 경비대` with `되살리기`, `이대로 두기` acknowledges that holder only).
  - **CST-39 hiding on cast screens**: `CharacterEdits.setHidden` from the record menu. Tests `HidingAndRecorderTest` (TC-19-01 lock row text and the record closing while locked, TC-19-06 `숨김` on the open record and unhide, TC-19-02 `숨긴 인물과의 관계` count).
  - **CST-49 recorder link count**: `RecorderLinkModel`. Tests `HidingAndRecorderTest` (하르트's line, none without links, locked hidden records not counted until records are open).
  - **CST-12 tag settings**: `TagSettingsModel`. Tests `TagSettingsTest` (order and counts, rename keeps keys and refuses taken / blank names, add at the end with unique names).
- **Batch 4 domain:cast addition** (new files only): `script/SpeakerData.kt`, test `SpeakerDataTest` (6).
- **Cross-lane requests done in batch 4**:
  - feature:world: `MergeItemsSheet` is registered and serves places, terms and factions (engine preview with `경로 끝` and the `첫 공개` row, `MergeActions.mergeItems`, recorder links moved). Open it as `MergeItemsSheet(p, kind, keepKey = target, otherKey = merged)`, or with `keepKey == otherKey` to start at the picker. `DeleteImpactSheet` now links `다른 장소와 합치기 ›` to it.
  - feature:story (CST-42 `화자 한꺼번에 바꾸기` / `@ 한꺼번에 바꾸기` by item key): open `DeleteImpactSheet(p, kind, itemKey)` for the deleted item; for an item not live it shows the check row with those bulk buttons, `되살리기` and `이대로 두기` (no new key needed). A dedicated key is still requested below.
  - feature:recorder (CST-49, LRC-24): `레코더 · 정리 안 됨 n ›` on `개요` pushes `RecorderInbox(projectKey = p, targetKind = "character", targetKey = c)`.
- **Cross-lane requests done in batch 3**:
  - feature:canvas: `+ 새 종류` in the line-label sheet pushes `RelationKindSettings`, which is now registered (CST-32).
  - feature:canvas (info to cast): the cast list's `목록 | 카드` is remembered in `AppSettings.viewByScreen["cast"]` (CST-04), so cast's request to core is withdrawn.
  - feature:canvas: a relationship-map line (`LineLabelSheet.forCharacters`) now has a write path without a canvas edge: `domain:cast` `PairLines.sheet` / `kindChips` / `makeRelation` (cast's own map uses it).
- **Cross-lane requests done**:
  - feature:world: `FieldValueEditSheet` and `AddFieldSheet` take `EntityKind.PLACE · TERM · FACTION` (since batch 1; the same content serves them). `DeleteImpactSheet` is now registered and works for places, terms and factions too (`TrashOps.delete`, `Messages.deleteToast`).
  - core pass: empty-cast `라이프 레코더에서` now pushes `RecorderInbox(projectKey = p)`.
  - core pass: the shared rich editor is used for `긴 글` values, `말투` prose, voice lines and archive documents; `TODO_COPY_CAST_LONG_TEXT_RICH` is gone.

## Partial

All UI. Built and compiling, but not yet seen on a device.
- **CST-01 인물 목록**:
  - Built: glass head (project name, `인물`, lens chip, `목록 | 카드`, filter button when the project has filters, `+`); `목록 | 관계도`; chips; active filter chips; grouped rows with the portrait avatar (or the first letter); `LockRow`; `EmptyCast` (`라이프 레코더에서` opens the recorder inbox filtered to this project); command bar with mic; inner `TwoCast` / `ColCast`.
  - `관계도` pushes the `RelationMap` route (CST-33, below).
- **CST-02**: typing in the command bar swaps it for a search field. No results shows `결과 없음`.
- **CST-03**: the header filter button (`항목 필터 · 정렬`, `acc` while active) opens a sheet with option chips per `필터` field, a min / max pair per `숫자` field, and `<항목>순` chips; changes apply at once. Active filters show as removable chips under the tag chips. Filters also narrow the card view.
- **CST-04 카드 보기**:
  - Built: header `ViewToggle`; one horizontal row per tag (`주연 · 3`), cards 196 × 245 in the first row and 132 × 165 after, portrait 4:5 or the first letter with `이미지 고르기`, title (`???` with `하르트 · 이름 공개 1-9`), first appearance, `홀로` chip.
  - The choice is remembered across launches in `AppSettings.viewByScreen["cast"]` (batch 3). Left: the list → card morph, snapping, parallax and tick haptic (motion spec).
- **CST-05**: as batch 1.
- **CST-06 펼친 화면 배치**:
  - Built: `TwoCast` (shell list | detail, the record in the right pane without its back button, the selected row highlighted) and `ColCast` (`CenterColumn`); the record's art uses the 4:3 crop on inner screens and 9:16 on the cover.
  - Left: fold continuity (`MFoldList` 48 px slide, last-touched pane) is the shell's CNV-34, not built there yet.
- **CST-07 record head**: as batch 1, plus the background art (CST-13) and the record `⋯`.
- **CST-08 tabs**: `개요` as batch 1 (plus image slot rows); `말투`, `게임 텍스트`, `비밀` editable. The `레코더 · 정리 안 됨 N ›` link is CST-49.
- **CST-09**: `긴 글` values now edit in the shared rich editor (`@` links, `( )`, `⌁` paragraphs kept). The `@` picker title is `TODO_COPY_CAST_LINK_PICKER_TITLE`.
- **CST-10, CST-11, CST-12, CST-19, CST-20, CST-21, CST-28**: as batch 1, except:
  - CST-28: partner blocks open the relation sheet; `+ 관계` works (CST-29); the dotted row has `되살리기 · 이대로 두기` (CST-41, CST-43).
- **CST-13 이미지**:
  - Built: slot rows `인물 초상 · 인물 배경` in `개요` (with `보기`, or when set) reading the preset name, `내 이미지` or `이미지 고르기`; the record's background art (cover 430 tall, text from 300; inner 4:3, fading); portrait avatars in the list and 4:5 cards; `홀로` chip. Blob images decode off the main thread with the place's crop (rotation, flip).
  - Picking, presets, cropping and holo are the image area's `ImagePickSheet` / `ImageCrop`, which the canvas lane registered in its batch 3; cast opens them. Left: the holo foil on cast cards and art is not drawn (DS `holoFoil` + tilt are CNV-30's), and the board's glass blur over the art's foot is not drawn. Cast still draws presets with its own `PresetPainter` copy (canvas asks core to move its painter to a shared module).
- **CST-14 첫 등장 · 보이스**: header rows from `Profile.header`; `보이스 · 3 / 5` and the bar on `게임 텍스트`; `고정 항목` switches `첫 등장 · 보이스` (sub `머리`) in `인물 항목` settings. Left: the check row's `보이스 쓰기` opens the tab but cannot scroll to the first empty line (no route argument).
- **CST-15 말투**: the prose opens `BodyEditSheet` (rich editor; an empty body shows a tappable `비어 있음`); each 호칭 chip opens that pair's 호칭 slot (CST-31, below).
- **CST-16 보이스 대사**: the record's `+` on `게임 텍스트` adds a line (sheet `보이스 대사 추가`: `이름` + text); tapping a line edits it (`이 값 고치기`); tapping its state switches `쓰는 중 ↔ 녹음 대본 완료` (an empty line stays `비어 있음`). Left: reordering lines (`VoiceLines.move` exists; no visible control yet); removing a line (no source draws it, and `VoiceLines.remove` bypasses the trash).
- **CST-17 아카이브 문서**: rows open `ArchiveDocEdit`; the section's `+` opens a new one (created on its first save; the saved key survives a state restore, so it is never created twice). `열리는 시점` takes any unit from the unit picker (`X 이후 열림`). Left: removing a document (no source draws it; `ArchiveDocs.remove` bypasses the trash).
- **CST-29 관계 만들기 · 고치기**: shared `RelationEditSheet`: partner picker (`상대 고르기`; picking the viewer is ignored), `무엇으로 적을지` chips, `지금 값` or the two-button head, kind chips (pair kinds as two role chips) with `+ 새 종류` (a new `사이` kind is `대칭`), `설명 (선택)` or the 호칭 text, `시작 시점` row (unit picker) and `관계로 만들기` / `기록`, or `이 값 고치기` / the two buttons. Left: the map card's `관계 고치기` (CST-33).
- **CST-30**: `RelationChangeContent` is built but cannot be opened: the script hosts `◆ 변화 기록` (SCR-41, not started) and no shared sheet key carries a block (see Requests).
- **CST-40 지우기**: record `⋯` → `지우기` → the impact sheet (`RCDeleteImpact`: counts, owned and staying rows, dotted `노아 ◇`, `점검 센터` and `중복이면` rows with `다른 인물과 합치기 ›` (batch 4), footnote, `취소 · 휴지통으로`) or, without references, delete at once; then the toast with `되돌리기`. The record leaves the screen after the delete. Left: several items at once (TC-20-11; §6 Q24's `고르기` in the list header has no board and is not built; the domain sums them).
- **CST-41**: the dotted partner row and the dotted `연결` chips (wide label `지운 세력 · 상층 경비대`) show; the partner row's `되살리기 · 이대로 두기` work; since batch 4 the value sheet has `되살리기 · 이대로 두기` under a dotted `연결` value (with `세력 고르기` and `빼기` for the other two fix paths), and the dotted partner name opens that partner's check row. Script, recorder and world references are those lanes'.
- **CST-18 비밀** (batch 3; S8 not drawn): secret fields (tap to edit, `◆` for history), then `비밀` with the prose in the shared rich editor (`⌁` memo paragraphs kept); an empty body is a tappable `비어 있음`. Left: export `비밀 항목 포함` is library's export sheet (exists in `domain:library`); no separate lock (§6 Q10 default).
- **CST-22 + 항목 from a record** (batch 3): `개요` `+ 항목` opens `항목 넣기` with `리엔 · 인물 기록 007`, `이름`, the `종류 | 짧은 글 ›` row (type list in place), `어디에` cards (`모든 인물에` default, `이 인물만` / `리엔에게만`), `넣기`. `이 인물만` fields carry the `◇` mark; their value sheet and their edit screen offer `모든 인물에 넣기`. Left: the record `⋯` has no `모든 인물에 넣기` row (it is on the field instead).
- **CST-23 항목 정의 바꾸기** (batch 3; not drawn): `FieldDefEdit` screen from each settings row: `이름`, `내보내기 이름` (placeholder = the name), `단위` with `환산하지 않음`, `보기` (rename in place with the holders under each, `빼기` asks first `리엔 외 3명 | ◇ 보기에 없는 값`, `+ 보기`), `직접 쓰기`, `종류` (current type, chips of the allowed targets, `연결` target chips; a dialog `짧은 글 → 숫자` with `◇ 바꾸지 못한 값 N` before `바꾸기`), `보이는 곳`, `비밀 항목`, `숨기기`, `모든 인물에 넣기`, `지우기` (dialog `값이 있는 인물 12명` · footnote · `휴지통으로`, then the toast with `되돌리기` and the screen leaves). Left: calc source and date precision cannot be changed (not in the CST-23 table); the inner list | detail layout of S13 [PROPOSED] is a full screen.
- **CST-24 시작 묶음** (batch 3): the settings row `시작 묶음 · 게임 프로필 · 항목 더하기 ›` opens the picker (`기본 · 게임 프로필 · 비어 있음 · 다른 프로젝트에서`, each with its fields), the fields that would be added, and `항목 더하기`. `다른 프로젝트에서` lists the other live projects (hidden ones while the hidden group is open). Left: new-project step 4 is the library lane's (`FieldBundles.newProjectRow`).
- **CST-25 나이 · 출생 · 생일**: logic in `domain:cast`; the value sheet shows a computed `나이` and a derived `생일` read-only with `계산 · 출생에서`, and a typed `나이` when `출생` is empty. Not seen on a device.
- **CST-26 항목 내보내기**: `내보내기 이름` in `항목 넣기` and the edit screen. The JSON writer and `비밀 항목 포함` are library / `domain:actions` (`ExportActions` with `CharacterExport`).
- **CST-27 항목 값 검색**: the cast list's search shows field hits under `항목` (`리엔` · `특기 · 성직자` with the match boxed, DS `SearchHit`), `결과 없음` only when neither names nor values match, and `숨긴 항목 제외` while hidden characters are locked. Left: the project command-bar search (`ProjectSearch`, story lane) does not call `FieldSearch` yet (Requests).
- **CST-31 호칭** (batch 3): a chip opens the pair's 호칭 slot (`RelationSlotSheet`): `지금 값`, then `이 값 고치기` / `여기서부터 바뀜 · 1-7` (a `◆` from here) like every timed value, or `시작 시점` + `기록` for a new one. `+ 호칭` picks a partner first; a pair without a relation gets an empty one (no map line). The script's `호칭 힌트` layer is the script lane's.
- **CST-32 관계 종류** (batch 3; `RCRelTypes`): `사이 · N` / `둘에게 같은 사실`, `대칭 · N` chips, `짝 · N` / `한쪽 적으면 반대쪽도` rows `선배 ↔ 후배 · 쓰임 1 ›`, `마음 · N` / `한쪽만 · 반대쪽은 비워 둠` chips, `+ 새 종류` in both (sheet: `사이 · 마음`, `대칭 · 짝 이름`, `이름`, `짝 이름`, `넣기`). A kind opens its sheet: names with `이 값 고치기`, the switches with `바뀌는 관계 N` and the pairs before `바꾸기` / `취소`, and `지우기` (dialog with `쓰임 N`). Left: the default list follows rule 13 (§6 Q1), not the board.
- **CST-33 전체 관계도** (batch 3; `CastMapC`, `CastMap`): `RelationMap` route on the shared graph engine: header `인물 · 11명 중 4명` / `관계도` (cover) or `인물 · 관계도로 보기 · …` / `전체 관계도` (inner), lens chip, filter button (`사이 · 마음 · 둘 다`, kind chips), `선` tool, cover `목록 | 관계도`, tag chips bottom left, cards with subtitles, solid `사이` lines with the middle label and dashed mind lines with end labels, the selected line in `acc`. A line opens `고른 관계` (inner: glass aside 260 bottom right; cover: bottom sheet with `닫기`), with `인물 기록` and `관계 고치기` (shared relation sheet). A node opens its record; from a record's `관계도로 보기` that character is highlighted. Nodes drag with one finger; positions are saved per project (screen-only in a read-only project). Left: nodes are only characters with a drawn line (§6 Q14), so a line can be drawn only between characters already on the map; zoom buttons; the selected line's labels in `acc` are drawn by the engine only for the label text.
- **CST-34 선 → 관계** (batch 3): on the map, the `선` tool → first character → second character opens `선 라벨` (`세라 · 마르타`, `무엇으로 적을지` with `<drawn-from>의 마음` preselected, that slot's kind chips incl. pair roles and `+ 새 종류`, `시작 시점`, `그냥 선으로 두기` · `관계로 만들기`). Only `관계로 만들기` writes. Left: the canvas sheet is the canvas lane's (it can use `PairLines` for `forCharacters`).
- **CST-35 관계 내보내기**: `.civilight` round trip and the 호칭-only JSON shape are `domain:cast` / `domain:library` (TC-13-21 in `domain:cast`). No cast screen: the export options are the library's export sheet; `ExportPlan` has no 호칭-only option yet (Requests).
- **CST-36 세력 사이 관계** (P2): storage only (`FactionRelations`), as the spec says; screens later.
- **CST-43 되살리기**: from the dotted partner row, the dotted `연결` value and the deleted item's check row, with the report toast (no `되돌리기`) and the biometric prompt for hidden items. Trash rows are CST-48 / library.
- **Batch 4** (built and compiling, not seen on a device):
  - **CST-37 화자 고르기 연동**: the picker is `core:ui` `CharacterPickerSheet` (search, 초성, tag chips, `이 씬 등장 · 최근`, deleted top row, hidden bottom row); cast uses the same window for its merge target and bulk speaker change. Left: the `필터` field chips (`소속 · 등불 결사`) are built as data (`SpeakerData.fieldChips`) but the core picker has no chip parameter for them (Requests).
  - **CST-39 인물 숨기기**: record `⋯` `숨기기` / `숨김 풀기` (one undo step each through `CastWrites`). While hidden items are locked the record closes and the list shows `숨긴 인물 1 · 지문 · PIN으로 열림 · 지문으로 보기`; unlocked, the record shows the `숨김` tag; the map, other records' `숨긴 인물과의 관계 1` and search exclusions follow the domain rules (batch 1 to 3 screens). The speaker field, picker, check center, trash and export rows of the table are the script, story, library lanes'. Hiding does not unlock (the gate opens only through `지문으로 보기`).
  - **CST-42 점검 센터 · 지운 항목**: the check row as a sheet (`DeleteImpactSheet` on a deleted item): `◇ 지운 인물 · 노아`, `휴지통 · 3일 남음 · 대사 14 · …`, `화자 한꺼번에 바꾸기` / `@ 한꺼번에 바꾸기` → picker → `대사 14 · 노아 → 오웬` → `바꾸기` (one undo step; toast with `되돌리기`), `되살리기`, `이대로 두기`. Reachable in cast from a dotted partner name; the check center list itself is story's. Left: `장소 · 연결 한꺼번에 바꾸기` (no write function in `domain:trash`; not drawn), changing only some lines from an expanded list.
  - **CST-44 영구 삭제 · 이름표**: purge is library's trash screen; cast shows the name tag state: `영구 삭제 · 노아` check row without `되살리기`, dotted `영구 삭제 · …` `연결` values without `되살리기`.
  - **CST-45 합치기**: record `⋯` `합치기`, the impact sheet's `다른 인물과 합치기 ›` and the `◇ 같은 이름` row open the shared `MergeItemsSheet`: target picker (speaker-picker window), then `RCMerge`: `인물 합치기`, `<merged> → <target> · 기본은 <target> 값`, `옮길 참조 · N` with the kinds, `다른 칸 · N` with the column heads and two radio cells per row (target selected), `관계 겹침 · N` rows per slot, `같은 항목 | 6 · 펼치기` (lists the equal cells), `빈 칸 채움`, `보이스 ID`, `서로 관계 1 · 지워짐`, footnote, `취소 · 합치기`; then the toast `시장 꼬마 → 노아 합침 · 참조 5` with `되돌리기` (doc rows and recorder links). Inner screens get the centred sheet from the shell.
  - **CST-46 같은 이름**: `◇ 같은 이름 2 · 노아` with `합치기` at the top of `개요` while another live character has exactly that name. Left: the cast list does not mark same-name rows (the spec names no list place).
  - **CST-47 장소 · 용어 · 세력 지우기**: the shared delete and merge sheets serve all three kinds (world opens them); dotted `연결` values of deleted world items have their fix buttons. The world-side reference rows (scene place, pins, sub-maps, term usage) are the world and map lanes'.
  - **CST-48 휴지통 줄**: library's `Trash` screen draws the rows from `domain:trash` `TrashListing` (character row, hidden row, `합침 → 노아`) and restores characters through `domain:actions` `CharacterActions.restore`, other items through `RestoreOps`, purges through `PurgeOps` (checked in `feature:library` `TrashDecisions`); cast has no trash screen. Field definitions restore there too (CST-23).
  - **CST-49 라이프 레코더 연결**: `레코더 · 정리 안 됨 n ›` (acc) right of the first `긴 글` section header of `개요` (or on its own line when there is none) → the recorder inbox for this character; the mic pre-fills `연결` with the character (`Recording(…, "CHARACTER", c)`). `카드에 넣기` and `인물로 만들기` are the recorder lane's sheets over `domain:recorder` (`CharacterInsert`).
  - **`TagSettings`** (CST-12, no board): the tags in order with counts (`주연 · 3`), rename (`이름`, `이 값 고치기`), `+ 태그` (`넣기`). No route pushes it yet (Requests); delete and reorder are not offered (no rule).
- **Not built by spec decision** (reported `not started`): CST-50 (`태그별 항목 묶음`, rejected for v1 in the spec), CST-51 (`인물 표 CSV`, §6 Q6 default: not built), CST-52 (profile voice-line IDs, decided later; rule 12 §6 covers script dialogue only).

## Defaults used

- **Fixed rows in `개요`** [PROPOSED here]: `표시 이름` while set or with history; image slots while they hold an image; `태그` while there are tags. With `보기` on, all six (`이름 · 표시 이름 · 한 줄 소개 · 인물 초상 · 인물 배경 · 태그`). `빈 항목 N · 보기` is shown even at 0.
- **Fixed fields without history** (`이름`, `한 줄 소개`, `태그`, `말투`, `비밀`): one primary `이 값 고치기`. Since batch 3 a 호칭 chip follows the relation slot's buttons (CST-31).
- **Past-lens banner** (S11, Q3): at the top of the value sheet, with `최신으로`.
- **Relation step status** `◆ 예정` / `◆ 지금 시점보다 뒤` in `acc`.
- **Empty value row** reads `비어 있음` (`ink3`).
- **`이 인물만` mark**: `◇` after the label.
- **`인물 추가` sheet** (CST-05 [PROPOSED]): `인물 추가`, `이름`, `넣기`.
- **Tag sheet** (CST-12 [PROPOSED]): an existing name picks that tag; new tags are created on save.
- **`목록 | 관계도`**: `관계도` pushes the `RelationMap` route (CST-33 is a route, see Architecture); the map's `목록` returns to the list.
- **Record frame**: no glass top bar while the head is visible; the bar appears after 160 px of scroll or past the first item. With background art the content starts at the art (no top inset).
- **Value sheet of a `연결` field**: the item picker replaces the inputs in the same sheet.
- **Undo labels**: the pressed button's copy (`이 값 고치기`, `여기서부터 바뀜 · 1-7`, `시점 추가`, `항목 넣기`, `순서 바꾸기`, `인물 추가`, `관계로 만들기`, `기록`, `넣기`, `휴지통으로`, `되돌리기`, `되살리기`, `이대로 두기`, the voice state word, `첫 등장` / `보이스` for the switches, `아카이브 문서` for archive typing).
- **Relation sheet** (S16 [PROPOSED layout]):
  - One slot at a time (`무엇으로 적을지` chips act as a selector); several slots at once only on the change sheet (CST-30).
  - `호칭` = what the viewer calls the partner.
  - Pair kinds show two chips named by the partner's role (`선배` = the partner is 선배).
  - A slot with nothing set and no history records from `시작 시점` (default: the lens episode, `CanvasRelations.startUnit`); the button is `관계로 만들기` for a new pair and `기록` for an empty slot of an existing one. Any other slot follows `LensEdit.choice` (`이 값 고치기`, or the two buttons), like every timed value.
  - `+ 새 종류` adds a `사이` kind as `대칭`; pair kinds come from `관계 종류` settings (CST-32). An existing name is reused.
  - Partner picker title `상대 고르기` (rule 13 §7 wording).
- **Record `⋯`** (§6 Q24): `숨기기` (or `숨김 풀기`) · `지우기` · `합치기`, in the spec's order. `이대로 두기` on a dotted partner row stores a `deleted.item` acknowledgement for `[partner, relation]` (one row), which hides that row's buttons; the item-level ack (check center) hides them too.
- **Restore toast** has no `되돌리기` (the item can be deleted again).
- **Field filters** (CST-03, Q13): a header filter button opens `항목 필터 · 정렬`; sort chips sit under `정렬` [PROPOSED]; a sort makes one section titled `<항목>순`; ranges read `신장 · 150 ~ 170` on the active chip. Options of one field widen, fields narrow.
- **Card view** (Q23): the card toggle and `목록 | 관계도` both show; tag chips, search and filters narrow the cards; card headers count the shown cards.
- **Images**: preset ids `ash-dawn` (fixture) and the spec 50 [PROPOSED] ids; preset art copied from the board generator (`scr_cards.py`). The background has no holo.
- **Voice line sheet**: title `보이스 대사 추가` for a new line [PROPOSED]; slot name labelled `이름`.
- **Archive editor**: saves as you type; `이름` for the title; `열리는 시점` accepts any unit (the spec says episode or chapter; the picker has no level filter here); character count counts spaces, not line breaks (domain rule).
- **Batch 3**:
  - **Field edit screen** (CST-23, not drawn): a full screen per field; texts save as you type (one undo step per typing run), switches and chips at once. Type-change and option-delete confirms are dialogs; a type change is previewed on a copy before anything is written. Its button is `바꾸기` (the verb of the spec's `관계 종류 · 바꾸기 ›` row) [PROPOSED reuse]; the dialog title is `짧은 글 → 숫자` (the spec's `키 → 신장` notation).
  - **Field delete**: a confirm dialog with `값이 있는 인물 N명` and the trash footnote, then `휴지통으로` and the toast with `되돌리기`; the edit screen leaves.
  - **Bundle picker** (CST-24, not drawn): title `시작 묶음`, radio rows with their fields, the fields to add under `항목 더하기`, button `항목 더하기` (disabled when nothing is missing).
  - **Kind sheet** (CST-32, not drawn): rename with `이 값 고치기`; the switch preview lists `바뀌는 관계 N` and the pairs, then `취소` · `바꾸기`; delete asks with `쓰임 N`. New kind: `사이 · 마음` and `대칭 · 짝 이름` segments, `이름`, `짝 이름`, `넣기`.
  - **Map** (CST-33): its own route; a finished drag saves every shown node's place (so the layout does not re-settle the others); the fitted view is latched on the first drag; the inner aside uses the SCRIM fill (it sits inside the screen's glass source); a cover line tap always selects and opens the sheet; filter and `선` buttons on both sizes (the cover board draws neither, so they are the only path to them); filter sheet title `필터` with `종류` kind chips; node drag undo label `노드는 끌어서 이동` (the canvas hint); positions in `project.cast.ext["relationMap"]` (no model field); the cover card shortens `지금 시점보다 뒤` to `뒤`; `인물 기록` opens the focused character when it is one of the pair, else end A.
  - **Map line sheet** (CST-34): no label text input (a map line has nowhere to keep it); `그냥 선으로 두기` closes without writing.
  - **List search** (CST-27): field hits under the section `항목`, after the name rows.
  - **호칭** (CST-31): `+ 호칭` chip at the end of the `말투` chips.
- **Batch 4**:
  - **Hiding** (CST-39): no confirm and no toast (no source gives one); the undo step is named `숨기기` / `숨김 풀기`. Hiding never opens the gate, so with hidden items locked the record closes at once.
  - **Merge entry** (CST-45): every entry starts at the target picker (the merge direction is always the author's pick; the record being viewed is the one merged away). The picker title is the sheet title (`인물 합치기`, `장소 합치기` …). `같은 항목 | 6 · 펼치기` lists the equal cells' names under the row; world items show `같은 항목 | N` without `펼치기` (the engine gives a count only). After merging, the merged item's record closes on its own; the target is not opened.
  - **Same-name row** (CST-46): at the top of `개요` as a dashed row with `합치기` (acc) on the right; its `합치기` opens the same picker (no guess about which of the two stays).
  - **Deleted item in the delete sheet** (CST-42): `DeleteImpactSheet` on an item that is not live shows its check row instead of closing; an item that leaves while the impact sheet is open still closes it. The bulk preview line for links is `@ 3 · 노아 → 오웬` [PROPOSED, after `대사 14 · 노아 → 오웬`]; the toast after `바꾸기` repeats the preview line with `되돌리기`. Bulk buttons are not offered on a masked (locked hidden) row.
  - **Hidden gate on batch-4 surfaces** (rule 19 §6): while hidden characters are locked the same-name row counts only visible characters, a merge overlap with a hidden partner reads `숨긴 인물` (no slot line), and tag counts leave hidden characters out (as the list chips do).
  - **Recorder link** (CST-49): placed right of the first `긴 글` section header; with no `긴 글` section, on its own right-aligned line under `빈 항목 · + 항목`. The count reads the documents of every project the linked records were put into (others count as gone, as the recorder's environment does).
  - **One-line 말투** (CST-38, §6 Q9): the first sentence of the 말투 prose (`CharacterEdits.firstSentence`); the model has no separate summary field.
  - **Tags** (CST-12): `TagSettings` title `태그` under `프로젝트 설정`; rows `주연 · 3` (the card-row pattern); rename keeps the key, so characters keep the tag.
- **Header switches** (CST-14 [PROPOSED control]): `첫 등장` and `보이스` switches with sub `머리` under the `고정 항목` box, character tab only.

## Spec conflicts

- **TC-11-01 planned row**: the fixture's R3 holds `등불 결사 · 등불지기장`, so the row reads `예정 · 2-5 | 등불 결사 · 등불지기장`.
- **CST-19 `출생 · 날짜 · 연도 · 머리`**: fixture conflict #13 makes 출생 not `머리`.
- **CST-02 `ㄹㅇ`**: matches both 리엔 and 엘리오 (초성 anywhere).
- **CST-40 staying title**: `domain:trash` `Messages.impactSheet` writes `지운 세력로 보임`; the sheet shows `지운 세력으로 보임` (see Requests).

## Requests

- **core:navigation (CST-30)**: a shared sheet that carries a block, owner CAST, for example `RelationEditSheet(…, blockKey: Key? = null)` or a new `RelationChangeSheet(projectKey, blockKey, viewerKey, partnerKey?)`. The content is ready: `relation/RelationChangeContent(projectKey, blockKey, viewerKey, partnerKey)`; cast registers it as soon as the key exists.
- **feature:script (SCR-41)**: the `관계` segment of `◆ 변화 기록` is cast's. Open the sheet above once it exists, or draw the segment from `domain:cast` `RelationDrafts.changeView` and save with `RelationDrafts.recordAtBlock` (both tested).
- **domain:trash**: `Messages.impactSheet` `stayingTitle` uses `로` for every kind (`지운 세력로 보임`); it should be `세력으로` (domain:cast `DeleteImpact.directionParticle`). Cast builds the title locally until then.
- **core (image area, from canvas's request)**: once `PresetImages` / `PresetArt` move to a shared module, cast drops its copy `image/PresetPainter.kt`. (`ImagePickSheet` and `ImageCrop` are registered by canvas since its batch 3.)
- **core:navigation (optional, CST-14)**: `CharacterProfile` has no way to open `게임 텍스트` at a line (`보이스 쓰기` → first empty line). A `focusKey: Key? = null` would do.
- **Copy needed**: `TODO_COPY_CAST_LINK_PICKER_TITLE`, the title of the `@` picker in cast text (voice lines, 말투, archive documents, `긴 글`).
- **Spec owner**: confirm the [PROPOSED] copy used here: `상대 고르기`, `보이스 대사 추가`, `정렬`, and the relation sheet's `기록` for an empty slot of an existing pair.
- **feature:story (`ProjectSearch`, CST-27)**: add character field-value hits from `domain:cast` `FieldSearch.search(doc, query, lens, hiddenUnlocked)` (rows `<항목> · <값>`, the match boxed; secret fields are never returned). The cast list already shows them.
- **feature:canvas (CST-34)**: `LineLabelSheet.forCharacters` can use `domain:cast` `PairLines.sheet` / `kindChips` / `makeRelation` (no canvas edge needed). Cast's own map draws its line sheet locally (`MapLineSheet`) until then.
- **domain:library (CST-35)**: `ExportPlan` / `ExportSheet` need the 호칭-only JSON option (`CastSettings.relationExportAddress`, `CharacterExport` `addresses`); still open from `domain-actions.md`.
- **core:model (CST-33, optional)**: a real field for the relationship map's node positions (now `project.cast.ext["relationMap"]`, `domain:cast` `MapPositions`).
- **Spec owner**: confirm `바꾸기` as the apply button of a previewed type change and kind switch, `필터` / `종류` in the map filter sheet, the bundle sheet copy (`시작 묶음`, `항목 더하기`), the `항목` section of list search hits, and `+ 호칭` on `말투`.
- **core:navigation (CST-42)**: a CAST-owned sheet for one bulk change of a deleted item, for example `DeletedItemSheet(projectKey, kind, itemKey, refKind?)`, so the check center can open the bulk change directly. Until then `DeleteImpactSheet(p, kind, itemKey)` on a deleted item shows the same check row with its bulk buttons (built and registered).
- **core:ui (CST-37)**: `CharacterPickerSheet` / `PickerModels.characters` need the `필터` field chips (`소속 · 등불 결사`) next to the tag chips: `domain:cast` `SpeakerData.fieldChips(doc)` and `SpeakerData.filter(doc, keys, chip, lens)` give the data.
- **feature:script (CST-38)**: the `참고 · 속성` panel can take everything cast supplies from `domain:cast` `SpeakerData.panel(doc, speaker, block, hiddenUnlocked)` (head line, one-line 말투, `??? (1-9 공개)`, 호칭 rows, relation rows with `◆ 여기서`, the hidden row).
- **feature:library (CST-12)**: `프로젝트 설정` can push `TagSettings(p)` (registered; a `태그` row). No cast screen links to it.
- **domain:trash (CST-42)**: write functions for `장소 한꺼번에 바꾸기`, `연결 한꺼번에 바꾸기` and `@ 한꺼번에 바꾸기` (like `Fixes.replaceSpeaker`; for `@` also over recorder text), so the check row can offer them. Cast draws `화자` and, as a local workaround over project text only (`RefRewriter.retargetLinks`), `@`.
- **Spec owner (batch 4)**: confirm `숨김 풀기` (spec [PROPOSED]), the `@ N · A → B` preview, the merge picker title `인물 합치기`, and the tag screen (`태그`, rename and add only); decide §6 Q9 (one-line 말투) and Q24 (multi-select delete entry).
- **FEATURES.md**: update CST-01 … CST-52 from this file (partial with notes as above; CST-36 storage only; CST-38 done as data; CST-50 … CST-52 not built by spec decision).
