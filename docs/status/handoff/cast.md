# feature:cast handoff

## batch 1

Read docs/status/feature-cast.md §Architecture first.

**Entries.** CastEntries.kt registers the CastList, CharacterProfile and FieldSettings routes (FieldSettings is imported as FieldSettingsRoute because domain:cast has a FieldSettings too) and the shared sheets FieldValueEditSheet, FieldHistorySheet and AddFieldSheet. Register new routes and sheets there as you build them: RelationMap, FieldDefEdit, RelationKindSettings, TagSettings, ArchiveDocEdit, RelationEditSheet, DeleteImpactSheet, MergeItemsSheet.

**Conventions:**
- **Copy**: all UI copy goes in CastUiCopy. BTN_ prefix = button, functions for formatted copy, TODO_COPY_ for missing copy. Add every new function to formatted() in CastCopyLintTest. Domain builders (CastList, Profile, TimedValues, RelationViews, FieldSettings, CastCopy) already produce most strings; use theirs.
- **View models**: subclass common/CastViewModel and write through act(label) { doc, ctx -> doc' }. One undo step per write, labelled with the button copy. Read-only returns READ_ONLY.
- **Shared sheets** get their editor with rememberCastEditor(projectKey) (field/ValueSheetParts.kt).
- **Lens**: every read uses doc.castLens.
- **Pure models**: CastListModel, ProfileModel, TimedEditModel, FieldDrafts, LongTextEdit, FixedFieldEdits, FieldSettingsModel and AddFieldModel are pure objects with JVM tests over CastFx (the example doc, a deterministic ctx, atLens).
- **Sheet keys**: feature-local ones are @Serializable data classes implementing SheetKey (NewCharacterSheet, NameEditSheet, OneLinerEditSheet, TagsEditSheet, DisplayNameEditSheet, DisplayNameHistorySheet).

**Record screen.** profile/ProfileScreen uses common/RecordFrame (floating glass buttons, a collapsing top bar, tabs above the command bar). Add a row's callbacks to RecordActions, and put tab bodies in profile/ProfileTabs.kt.
- 말투, 게임 텍스트 and 비밀 are read-only so far (CST-15 to CST-18).
- The 관계 tab is wired to RelationViews.tab. + 관계 opens the unregistered RelationEditSheet (CST-29), so start there. TimedSubject (field/TimedEditModel.kt) can take a relation slot as a third subject so the two edit buttons are reused.
- The record ⋯ menu (숨기기 · 지우기 · 합치기) does not exist yet. Put it in RecordFrame's top row. Character delete, restore and merge must go through domain:actions CharacterActions and MergeActions, never through domain:cast refs/* or merge/*.

**List screen.** 목록 | 관계도 pushes RelationMap for now. CST-33 should add a view mode to CastListUi and draw the map inside the list. CST-04 adds a card mode.

**Field settings.** Rows push FieldDefEdit (CST-23). The bundle row needs CST-24.

**Gaps that need no new feature:**
- The past-lens banner sits inside the value sheet, not under the record head (spec S11).
- The board's 1-5부터 · 다음 변화 2-5 preview line is not built.
- The settings grip has no keyboard alternative.
- No domain module was changed this batch.

## batch 2

Read docs/status/feature-cast.md §Architecture first; it now lists the batch-2 packages.

**Built this batch:** relation sheet (CST-29), relation change content (CST-30, unreachable), delete, restore and keep (CST-40, 41, 43), field filters and sort (CST-03), card view (CST-04), images (CST-13), unfolded art (CST-06), header switches (CST-14), 말투 (CST-15), voice lines (CST-16), archive documents (CST-17). `긴 글` values also use the shared rich editor now.

**New conventions:**
- **`common/CastWrites`**: any write whose screen may leave before it finishes, or that a toast's `되돌리기` reverts, runs on the app scope with its own session. A record pops right after its own delete, so a view-model write would be cancelled. Use `launch(label, compute, mergeKey, then)` and `revert(label, change)`.
- **`trash/ItemTrash` + `ItemTrashFlow` (`rememberItemTrash`)**: the only delete, restore and keep path in cast.
  - Characters go through `domain:actions` `CharacterActions`; places, terms and factions through `domain:trash`.
  - The shared `DeleteImpactSheet` serves all four kinds. The world lane can open it now.
- **Relation logic shared with script**: it lives in `domain:cast` `relation/RelationDrafts.kt` (additive). `relation/RelationSheetModel` adds the CST-29 save modes on top.
- **Record `⋯`**: `profile/RecordMenu.kt` (`RecordMenuSheet`) holds only `지우기`.
  - CST-39 adds `숨기기`, through `CharacterEdits.setHidden`, plus the lock rules.
  - CST-45 adds `합치기` (`MergeActions`, the `MergeItemsSheet` to register) and the impact sheet's `다른 인물과 합치기 ›` in its `중복이면` row (`trash/DeleteImpactContent.kt`).
- **`RecordFrame`** takes `add` (the tab's `+`) and `menu` (`⋯`). Both show in the floating row and in the collapsed bar.
- **Images**:
  - `image/CharacterImages` maps each place to its crop ratio. `rememberSlotPainter` draws a preset (`PresetPainter`, a copy of the board generator's shards) or a blob, decoded off the main thread.
  - Picking and cropping are canvas's `ImagePickSheet` / `ImageCrop`. They still show the shell placeholder.
- **List**: `CastListUi.cards` and `CastListUi.filters`. `CastFilterModel` is pure; `FilterStateCodec` saves the state with the screen. CST-33 should add the map mode to `CastListUi` and draw it in place of the rows.

**Left for later batches, beyond their own features:**
- `RelationChangeContent` needs a core:navigation key with a block (Requests). Register it under that key in `castEntries` once it exists.
- `비밀` prose is still read-only. `BodyEditSheet(…, BodyPart.SECRET)` already works, so CST-18 only needs to open it from `SecretTab`.
- No visible control yet for reordering voice lines (`VoiceLines.move` exists).
- Dotted `연결` values have no `되살리기 · 이대로 두기` in the value sheet. `ItemTrash.restore` / `keep` cover them.
- `보이스 쓰기` cannot scroll to the first empty line; that needs a route argument.
- Card-view motion (morph, snapping, parallax, haptic) and fold continuity (shell CNV-34) are not built.
- Removing a voice line or an archive document is not offered: no source draws it, and the domain's `remove` would bypass the trash. If a later spec adds it, route it through `CastWrites` with a `되돌리기` toast.
- `RelationSheetModel` keeps `FromStart` only for an empty slot with no history. Everything else follows `LensEdit.choice`.

## batch 3

Read docs/status/feature-cast.md §Architecture first; it lists the batch-3 packages (`kinds/`, `map/`, the new files in `settings/`, `common/SavingTextField`, `relation/RelationSlotSheet`).

**Built this batch:** secret tab (CST-18), `+ 항목` from a record with the `종류 ›` row (CST-22), the field edit screen (CST-23, `FieldDefEdit`), the start-bundle picker (CST-24), age read-only / typed (CST-25), export names (CST-26), field-value hits in the list search (CST-27), 호칭 chips through the relation slot sheet (CST-31), `관계 종류` (CST-32, `RelationKindSettings`), the full relationship map (CST-33, `RelationMap`) with its line tool and line-label sheet (CST-34), faction relation storage (CST-36). CST-35 has no cast screen (library export).

**New conventions:**
- Settings texts write as you type through `common/SavingTextField` + `CastWrites` with a `mergeKey`; switches and chips go through the view model's `act`. Confirms are DS `ConfirmDialog`s whose pending choice is saved as a string with the screen.
- Field-definition delete: `ItemTrash.deleteFieldDef` (`TrashOps`, `ItemId.fieldDef`) via `ItemTrashFlow.deleteFieldDef(key) { pop }`.
- The map is its own route, not a list mode (`CastList` is a 390 dp list pane on inner screens). Its state is `map/MapUi` (JSON in `rememberSaveable`); everything visible comes from `RelationMapModel.build`. Node positions: `domain:cast` `MapPositions` in `project.cast.ext["relationMap"]`; dragging in a read-only project stays on screen only.
- Map lines without a canvas edge go through `domain:cast` `PairLines` (also offered to the canvas lane for `LineLabelSheet.forCharacters`).
- `RelationEditContent` now wraps `RelationEditBody(projectKey, viewer, partner?, initialSlot)`; open one slot directly with the feature-local `RelationSlotSheet`.

**Next batch (CST-37 … CST-52):**
- CST-39 `숨기기` in `RecordMenuContent`; the map already hides locked characters (domain).
- CST-45 `합치기`: register `MergeItemsSheet`, add the impact sheet's `다른 인물과 합치기 ›` and a `◇ 같은 이름` row (CST-46).
- CST-42 check rows: feature:story asks for a way to run `화자 한꺼번에 바꾸기` / `@ 한꺼번에 바꾸기` by item key (see feature-story.md Requests).
- CST-48 trash rows are library's screen; field definitions restore through `RestoreOps` there.
- CST-49 recorder: `RecorderInbox(projectKey, "character", c)` for `레코더 · 정리 안 됨 n ›`.

**Open gaps (beyond the features):**
- The map shows only characters with a drawn line (Q14 default), so the `선` tool can only join characters already on the map.
- `ProjectSearch` (story) does not call `FieldSearch` yet; the 호칭-only export option has no place in `ExportPlan`.
- Earlier gaps still open: voice-line and archive reorder/remove, dotted `연결` fix buttons in the value sheet, `보이스 쓰기` scroll target, card-view motion.

## batch 4 (last batch of this lane)

Read docs/status/feature-cast.md §Architecture; it lists the batch-4 packages (`merge/`, the new files in `trash/`, `profile/RecorderLink.kt`, `field/LinkFixes.kt`, `settings/TagSettings*`) and `domain:cast` `script/SpeakerData.kt`.

**Built this batch:** record `⋯` `숨기기 / 숨김 풀기 · 지우기 · 합치기` (CST-39, CST-45); the shared `MergeItemsSheet` for all four kinds (CST-45, WLD-15) with its picker step, `RCMerge` rows and a revertible write that also moves recorder links; `◇ 같은 이름` row (CST-46); the deleted item's check row with `화자 · @ 한꺼번에 바꾸기`, `되살리기`, `이대로 두기` inside `DeleteImpactSheet` (CST-42, CST-44); dotted `연결` fix buttons in the value sheet (CST-41, CST-47); `레코더 · 정리 안 됨 n ›` (CST-49); `TagSettings` (CST-12); the script panel and picker-chip data in `domain:cast` (CST-37, CST-38). CST-48 is library's trash screen; CST-50 … CST-52 are not built by spec decision.

**Conventions set:**
- `MergeItemsSheet(p, kind, keepKey, otherKey)`: `otherKey` is merged into `keepKey`; `keepKey == otherKey` starts at the picker. Writes go through `merge/MergeFlow` (doc via `CastWrites`, recorder via `stores.recorder.updateLibrary`).
- `DeleteImpactSheet` on an item that is not live (trash or name tag) shows its check row; an item that leaves while the impact sheet is open closes it (`openedLive` saved with the sheet).
- Bulk changes are `trash/DeletedItemModel.apply` (one `DocChange`, toast with `되돌리기`).

**Unfinished threads (all recorded under Requests / Partial):**
- `RelationChangeContent` (CST-30) still needs a core:navigation key with a block.
- `필터` chips in the core picker (CST-37); script should render `SpeakerData.panel` (CST-38).
- `장소 · 연결 한꺼번에 바꾸기` need `domain:trash` write functions; multi-select delete (§6 Q24) has no board.
- Nothing pushes `TagSettings` yet (library `프로젝트 설정`).
- Older gaps: voice-line and archive reorder / remove (no source draws them), `보이스 쓰기` scroll target (route argument), card-view motion, map nodes only for characters with a drawn line (Q14).
