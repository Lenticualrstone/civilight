# domain:trash

Rule 20 and 00-domain-model §3 / §5 as pure functions over `ProjectDoc` (package `io.github.lenticualrstone.civilight.domain.trash`), plus the trash host's data side (LIB-38/39) for project, project-item and recorder rows. No I/O, no clock: every operation takes a `TrashContext` (now, zone, device id, key source).

`tools/gw :domain:trash:test`: 104 tests, 102 pass, 2 `@Ignore` (spec conflicts below), 0 failures.

Feature status in `docs/FEATURES.md` terms: the domain slice of CST-40 … CST-48 and LIB-38 / LIB-39 / LIB-40 is complete with its rule tests; their screens are not built here, so those features stay `partial` until the feature modules draw them.

## Done

Sources: `domain/trash/src/main/kotlin/io/github/lenticualrstone/civilight/domain/trash/`. Tests: `domain/trash/src/test/kotlin/io/github/lenticualrstone/civilight/domain/trash/`.

| Feature / section | What | Files | Tests |
| --- | --- | --- | --- |
| §5.1 direct marks, derived liveness | Only the deleted row gets `deletedAt` + `trashEntryKey`; owned rows are found as "live before the mark, not live after it" (no second ownership table) | `DocRows.kt` (`setMark`, `rowOf`, `isItemLive`), `LiveRows.kt` (`LiveRows`, `OwnedRows`) | `EntityKindTrashTest` (values stay on rows), TC-20-08, TC-20-09, TC-20-24 |
| CST-40 impact sheet | `Impact.of(doc, items, recorder)`: owned counts (relations + partner names, `◆`, `◇ 첫 공개`, game text, pins, field values, images, blocks, overlays, canvas parts) and staying references by kind; held `▲` for terms; locations hidden for hidden items; works for live and trashed items. `Messages.impactSheet`, `Messages.deleteToast` | `Impact.kt`, `Messages.kt` | TC-20-01, -02, -03, -10, -11, -12, -15; `EntityKindTrashTest` |
| CST-41 / CST-47 references | `ReferenceScanner.referencesTo(item)`: speakers (incl. spoken options), silent cast, `@` links on every surface incl. recorder text, scene place / region, field `연결` (base and `◆` values), term `관련`, canvas nodes, region / stamp / node / province / script-asset links, route ends, sub-maps, child places, event place / term / scene, flashbacks, world-event anchors, base scenes, recorder chips. Only live containers are scanned | `ReferenceScanner.kt` | TC-20-01, -12, -13 (variant), -14, -22; `EntityKindTrashTest` |
| §3 resolution | `Resolution.resolve` in the order `HIDDEN_LOCKED` (hiding wins, also for trashed and tagged) → `LIVE` → `TRASHED` → `TAGGED` (purged / notExported) → `MISSING`; labels, dotted flag, `되살리기`, export name, voice code from row or tag | `Resolution.kt` | `ResolutionTest` (10), TC-20-02, -06, -10, -21, export walk-through |
| §5.2 delete, every kind | `TrashOps.delete(doc, items, ctx)`: character, place, term, faction, field definition, change record, map, event, page, canvas, unit, block. One entry per item; blocks one `blockGroup` per scene per local day (extended on the same day); empty blocks hard-deleted (no entry, logged); `trashPlacement`; fixed voice IDs → `DiscardedVoiceId(deleted)`; anchors relocate; acks of non-live subjects dropped (not `deleted.item`); `hiddenAtDelete` | `DeleteOps.kt`, `Anchors.kt` | T-02, T-03, T-04, T-12-01, T-12-07 (variant), `EntityKindTrashTest`, `BlockRestoreTest` |
| Undo (`되돌리기`) | Row-level revert of the before/after pair; later edits of other rows survive | `DocChange.kt` | `EntityKindTrashTest.undo …`, T-12-03, TC-20-04, TC-20-17 |
| CST-43 restore | `RestoreOps.restore`: clears the mark, deletes the entry, re-attaches orphaned records, deletes `deleted.item` acks; report `다시 이어짐 · 그대로 둔 것` from the reference snapshot taken at delete time; merged items rebuilt from `mergedOriginal`; block restore by `prevKey` / `nextKey` / ordinal, `씬도 되살리기` · `다른 씬 끝에` (new number in another numbering unit); restored records are settled against the manuscript as it is now | `RestoreOps.kt`, `Anchors.kt`, `Gates.kt` | TC-20-05, -07, -08, -09, -14, -15, -19, -23, -24; T-14, T-15, T-12-24; `BlockRestoreTest`; `WalkThroughTest` |
| CST-44 / LIB-39 purge | `PurgeOps.purge / purgeEntries / purgeDue / emptyTrash`: removes the row and everything owned (relations, records, images, voice lines, archive docs, pins, overlays, blocks, units, rejections, travel overrides); `NameTag(purged)` with voice code and hidden flag for character · place · term · faction; world-event anchors orphaned on event purge; `deleted.item` acks kept while references remain; entries whose targets are gone dropped; deletion log kept 30 days; unreferenced blobs collected | `PurgeOps.kt` | TC-20-06, -10, -23, -25; T-08, T-12, T-13; `EntityKindTrashTest`, `TrashHostTest` |
| 30-day schedule | `TrashContext.daysLeft` (`30 − local calendar days`), `isDue` (`today ≥ purgeOn`), `TrashEntry.purgeDateFor` | `TrashContext.kt` | T-01, T-05, T-06, T-07, T-08; `TrashHostTest.countdown …`, `purge date …`, `daily purge …` |
| CST-48 / LIB-38 trash rows and filters | `TrashListing.projectItemRows / projectRows / recorderRows / list / chips`; filters `전체 · 프로젝트 · 씬 · 블록 · 인물 · 세계 · 변화 · 레코더` (`세계` = 장소 · 용어 · 세력 · 지도 · 사건; field definitions under `인물`; pages and canvases under `전체`); masked rows (hidden item, hidden project); one auth predicate (`TrashGates.needsAuth`) for rows, restore and `비우기` | `TrashListing.kt`, `TrashCopy.kt`, `Codes.kt`, `Gates.kt` | T-01 … T-11, TC-20-20, I-14, S-08, `TrashHostTest` |
| CST-42 check rows | `DeletedItems.rows`: one row per deleted item (trashed or purged tag), per-kind counts, held `▲`, bulk buttons, `이대로 두기` state | `DeletedItems.kt` | TC-20-04, -06, -13, -15, -28 |
| Fix paths | `Fixes.replaceSpeaker` (all or some lines, one undo), `unlink` (`글로 풀기`), `keep` (`이대로 두기`, item or one reference), `sameNameNotice` (exact NFC), `deletedSpeakerPickerRow` | `Fixes.kt`, `RunRewrite.kt` | TC-20-04, -05, -07, -16, -22, -23, -28; `ResolutionTest.same name …` |
| CST-45 / CST-46 merge | `MergeOps.preview / merge / undo`: references rewritten to the target (`MovedRef`s), target-empty values filled, chosen values taken, mutual relation deleted, overlapping relations folded into the target's (both `◆` sets kept, slots mapped by side), pins and route ends moved, term reveal chosen, merged item trashed `합침 → <target>`; `applyToRecorder`. Since 2026-11-22 (the one merge engine, DECISIONS §4): a field whose value lives only in `◆` records is not an empty cell, and a taken field brings the merged item's `◆` records (a target record at the same point gives way) | `MergeOps.kt` | TC-20-17, -18, -19 (variant), -26, -27, T-11; `MergeFieldHistoryTest` (3) |
| §5.4 walk-throughs | TC-20-08, -09, -19, -23, -24, SCR-39 (both halves), `기록 지우기` on an orphaned record, export without trash (`NameTag(notExported)` via `DerivedMarks.exportNameTags`) | `DerivedMarks.kt` | `WalkThroughTest` (10), Rule20 tests |
| Derived marks | `대상 지워짐` records per block, pin status `◇ 지도 없음`, top-level map after the parent pin is purged | `DerivedMarks.kt` | TC-20-15, -23, -24, -25 |
| Project trash | `ProjectTrash.delete / restore / purge / purgeDue / emptyTrash`; `replaceByOverwrite` (§6: re-key to K′, `replacedByOverwrite`) and `rekey(doc, newKey)`; example projects never trashed | `ProjectTrash.kt` | T-01, F-12, `TrashHostTest.project trash rows …` |
| Recorder trash (LRC-43) | `RecorderTrash.delete / restore / purge / purgeDue / emptyTrash`; `applyProjectPurge` (links get `purgedAt` + `lastLabel`), `applyProjectGone` | `RecorderTrash.kt` | T-07, `TrashHostTest.recorder …` |
| Seams for marks set elsewhere | `TrashOps.settleDelete(before, marked, ctx)` and `RestoreOps.settleRestore(before, unmarked, ctx)`: the anchor and ack steps of delete / restore, for domain:story's `SceneTrash`; `Messages.sceneDeleteToast(before, after, sceneKey, relocations)` | `DeleteOps.kt`, `RestoreOps.kt`, `Messages.kt` | `WalkThroughTest.scene marks set elsewhere …` |
| Toast copy (LIB-40 messages) | delete, restore, merge, block (`블록 #043 지움 · ◆ 3개는 #044 자리로`), scene (`씬 3 지움 · ◆ 3 · ◇ 1은 …`) | `Messages.kt` | TC-20-01 … -05, -17, T-12-01, T-12-07, `BlockRestoreTest` |

Spec cases covered: 30-cast TC-20-01 … TC-20-28 (all 28); 60-library T-01 … T-15, F-12, I-14, S-08 (trash location only); 00 §5.4 every row; 10-script T-12-01 (fixture variant), -03, -04, -05, -07 (fixture variant), -24 (they exercise this module). LIB P-07 (example project purged with no trash entry) is domain:library's onboarding flow.

## Partial

- **Scene delete 때 rebasing (STO-10)**: composed in `domain:actions` `SceneActions.delete / restore / restoreBlocks` since 2026-11-22. It is domain:story's (`SceneTrash.delete / restore`); this module cannot depend on story. `TrashOps.delete(unit)` marks, relocates anchors and drops acks but does not rebase. The app composes `SceneTrash.delete` + `TrashOps.settleDelete` (and `SceneTrash.restore` + `RestoreOps.settleRestore`); see Requests.
- **UI**: sheets, toasts and rows are data and copy only; the feature modules draw them.
- **Blob store**: `PurgeResult.unreferencedBlobs` drops blobs from this project's `assets/index.json`; deleting files from the global blob store (only when no project references them) is core:store's.
- **Export**: `DerivedMarks.exportNameTags` computes the `tombstones.json` name tags of an export without `휴지통도 포함`; writing the package is the exporter's.
- **Orphaned-record fix paths** (`다른 블록에 잇기`, `이대로 두기` → `change.anchorDeleted`, `기록 지우기` from the check row) are domain:script's (`Anchors.anchorToBlock / keepHere / trashRecord`); this module only relocates and re-attaches, and restores a `change` entry.
- **Character merge**: `MergeOps` is the engine for every kind (DECISIONS §4, through `domain:actions` `MergeActions`). It also exists in domain:cast (`CharacterMerge`). Both write the same `MergeRecord` shape (`mergedOriginal`, `ext.summary` with `refs · relations · fields`) and a `merged` trash entry, so `RestoreOps.restore` and `Messages.mergedRestoreLine` work for either. `MergeOps` here covers all four kinds (places, terms, factions too).
- **Cross-project trash list**: the caller concatenates `projectRows` + `projectItemRows` (per project) + `recorderRows` and passes them to `TrashListing.list` / `chips`.

## Defaults used

- Ordinal of `trashPlacement` = 0-based index among the scene's live blocks at delete time; restore inserts at that index when both neighbours are gone (§5.3; T-12-24 agrees).
- An empty block that anchors a record is trashed, not hard-deleted (T-12-20).
- Restore report (`그대로 둔 것`): the reference sites at delete time are kept in `TrashEntry.ext["refSites"]` (no model field).
- Merge restore sheet counts (`옮긴 참조 · 관계 · 칸`) are kept in `MergeRecord.ext["summary"]`; `칸` = values the target took (filled + chosen).
- A record restored from its own mark (`기록 지우기` undone from the trash) is settled against the manuscript as it is then: re-attaches if its original referent is live, relocates if its anchor referent is gone, else stays as it was (§5.4 row holds: still `orphaned` while the original is gone).
- One auth predicate for trash rows (`TrashGates.needsAuth`): hidden at delete time, hidden now (a block follows its scene) or the project hidden. Used for `되살리기`, the row's `되살리기는 지문 확인` and `비우기`.
- `비우기` leaves locked hidden entries (LIB-39 `ED`), for project items, projects and recorder records alike.
- Locked hidden reference that is also trashed: `숨긴 인물` / `숨긴 항목`, not dotted, no `되살리기`; export name stays the real name.
- A scene `지역` whose map is in the trash resolves `TRASHED` with the kind word `장소` (`지운 장소 · 하층 구역`).
- World item trash row sub: `<kind> · <project> · 참조 N` (LIB-38: pattern of the 인물 row).
- `변화` row: title `◆ <entity> · <field>` (relations `◆ 리엔 · 하르트 · 마음`, reveals `◇ 첫 공개 · <term>`), sub `변화 · <project>`.
- Field definition row: title = definition name, sub `항목 · <project>`, filter `인물` for every entity kind.
- Block group day words: `오늘 지움` · `어제 지움` · `M월 D일 지움`.
- Recorder rows: text records `글`; hidden records `숨긴 기록`.
- Pin action label `핀 다시 놓기` next to `지도 되살리기` (TC-20-24 has no copy).
- Check-row bulk buttons besides `화자 한꺼번에 바꾸기`: `장소 한꺼번에 바꾸기` · `@ 한꺼번에 바꾸기` · `연결 한꺼번에 바꾸기`; count words for world kinds (`장소 · 연결 · 관련 · 지도 · 경로 끝 · 하위 지도 · 하위 장소 · 사건 · 캔버스`).
- Impact sheet rows not on the board: owned `◇ 첫 공개 · 핀 · 블록 · 게임 텍스트`; staying `씬 장소 · 연결 값 · 관련 · 지도 연결 · 경로 끝 · 하위 지도 · 하위 장소 · 사건 장소 · 사건 용어 · 사건 기록 · 회상 씬 · 캔버스`; several items: title `<kind> <n> 지우기`, sub `<kind> <n> · 대사 N · …`.
- Toast `… · ◆ N개는 예정으로` when relocation finds no later block.
- `deleted.item` acknowledgement fingerprint: `deleted.item:<subject keys>`.
- Merge: a lone merged pin on a map where the target has no pin moves to the target place; the merged term's name and aliases become aliases of the target (WLD Q-11 `[PROPOSED]`); recorder links of the merged item move to the target, duplicates dropped.
- Unit purge: recorder links to it keep `<code> · <title>` as `lastLabel` (`1-6 씬 5 · 시장의 밤`); units and pages leave no name tag (§5.3 names character · place · term · faction only).
- An episode file whose scenes were all purged is kept while it holds discarded voice IDs.
- Event purge: `worldEvent` anchors become `orphaned` with `originalAnchor` = that anchor (§5.5 `ED`).
- `덮어쓰기`: the existing project is re-keyed to K′ and trashed; already-trashed rows are only re-keyed (§6 `ED`).
- Trash list order: deletion time, newest first (LIB-38 `ED`).

## Spec conflicts

| Case | Conflict | Test |
| --- | --- | --- |
| TC-20-13 | Fixture conflict #10 makes 소속 a `짧은 글` field, so no character links 상층 경비대 as 소속. | `Rule20CastTest.TC-20-13 faction delete` is `@Ignore`; the synthetic `연결 · 세력` field variant passes. |
| TC-20-19 | The restore sheet reads `칸 3`, but TC-20-17 (its Given) takes only 출생 and 성격 (`빈 칸 채움 · 2`; 나이 stays 노아's). The board's `다른 칸 · 3` is sample data (CST §6 Q2). | `Rule20MergeTest.TC-20-19 restore the merged item` is `@Ignore`; the fixture-count variant reads `칸 2`. |
| TC-20-11 | `대사 40` is board sample data; 노아, 오웬 and 고든 speak a different number of lines in the fixture. | Test asserts `인물 3 · 대사 <their line count>`. |
| TC-20-01 / -05 / -08 / -23 | 노아 is in the trash in the fixture (fixture conflict #22). | Tests start from `Fx.noahLive()` (mark cleared, `TRASH_NOAH` dropped). |
| T-12-01 / T-12-07 | 10-script F1 has one record (R1) on #043 and blocks in 1-7 씬 4; the fixture has R4, R5, R5A on #043 and no blocks in 씬 4 (fixture conflict #1). | Tests assert `◆ 3개는 #044 자리로` and relocation to 1-8 #001 (`… 1-8 씬 1 처음으로`). |

## Requests

- ~~**App / orchestrator (scene delete)**~~ Done (2026-11-22): `domain:actions` `SceneActions`. Original: compose `SceneTrash.delete` (domain:story; mark, entry, 때 rebase) with `TrashOps.settleDelete(before, marked, ctx)` and build the toast with `Messages.sceneDeleteToast(before, after, sceneKey, relocations)`; for restore, `SceneTrash.restore` then `RestoreOps.settleRestore`. `TrashOps.delete(ItemId.unit(…))`, `RestoreOps.restore` of a `scene` entry and `SceneChoice.RestoreScene` in `RestoreOps.restoreBlocks` clear or set the mark without the 때 rebase: for a scene, run `SceneTrash.restore` first and then `RestoreOps.restoreBlocks` (the scene is live, so no choice is asked).
- ~~**App / orchestrator (merge)**~~ Done (2026-11-22): `MergeOps`, DECISIONS §4. Original: pick one owner for character merge (domain:cast `CharacterMerge` or `MergeOps` here); both are compatible on restore.
- **core:store:** (the pure part is `domain:actions` `PurgeActions.runDue`, 2026-11-22; the store writes its result) run `PurgeOps.purgeDue`, `ProjectTrash.purgeDue` and `RecorderTrash.purgeDue` daily and on app start; after each project purge apply `RecorderTrash.applyProjectPurge` to the recorder library; delete blob files listed in `PurgeResult.unreferencedBlobs` when no other project references them; after `ProjectTrash.replaceByOverwrite`, store the old project under `ProjectTrash.rekey(doc, K′)`.
- **core:model:** consider real fields for what this module keeps in `ext`: `TrashEntry.ext["refSites"]` (reference sites at delete time, for the restore toast) and `MergeRecord.ext["summary"]` (`옮긴 참조 · 관계 · 칸` counts). Until then they round-trip as extension data.
- **domain:checks:** the `지운 항목` filter rows are `DeletedItems.rows(doc, ctx, recorder, locks)`; `deleted.item` acks use subjects `[itemKey]` or `[itemKey, referencingKey]` and fingerprint `Fixes.deletedItemFingerprint`.
