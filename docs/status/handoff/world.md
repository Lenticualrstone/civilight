# feature:world handoff

## batch 1

Status file: docs/status/feature-world.md (Architecture section lists every file and how to extend). Module: feature/world/src/main/java/io/github/lenticualrstone/civilight/feature/world.

Pattern: each screen = pure `…State.build(doc, …)` (JVM-tested on ExampleProject.build(), fixture helper in src/test/.../WorldTestFixture.kt, which also derives state A by dropping the alias 빛돌) + a view model extending common/WorldViewModel. The view model gives readOnly, hiddenOpen and editContext(); its apply(label){} is one undo step; backToLatest sets the lens to 최신. The screen uses CivilightScreen, rememberSheetHost and feature-local @Serializable sheet keys.

Copy goes in WorldCopy only; WorldCopyLintTest lints it, and new formatted functions go into its formatted() list.

Entries: WorldEntries registers World, TermCard, PlaceCard and FactionCard. Batch 2/3 must register Timeline, EventDetail, TimelineMap, WorldCalendar and TravelModes, plus the WORLD shared sheets LinkCandidatesSheet (WLD-23) and EventCreateSheet (WLD-32).

Already wired, do not rebuild:
- Card-level candidate 잇기 · 아님: TermCardViewModel.linkCandidate / rejectCandidate and UsageRows.CandidateMarker.
- The 공개 시점 옮기기 sheet (term/RevealOptions, TermSheets). Reuse it for the check center's 공개 시점 옮기기.
- The @ link tool in the 설명 고치기 sheet: common/ItemSheets DescriptionContent, DescriptionDraft and RichTextEdit.insertLink.

WLD-26 hook: common/TermIndexes.Holder.run currently does a full CandidateIndex.build on every text change; swap in RescanScheduler there and callers stay unchanged. StaleIndexTest guards rendering a new doc against an old index.

Where batch-2 work slots in:
- The ⋯ menu (common/ItemSheets.ItemMenuContent) has only 이름 바꾸기 · 설명 고치기. Add 아님으로 둔 것 · 지우기 (domain:trash) · 합치기 (domain:actions MergeActions) · 숨기기 there.
- The term card group header action 모두 잇기 · 모두 아님 is not drawn yet. The domain gives card.candidates.action; add it in TermCardScreen.termBody.
- The alias row is read-only (FixedRowUi editable=false); WLD-16 makes it editable.

Spec conflicts recorded:
- The 아직 공개 전 order follows the rule (2장 before 2-5), not the spec sample.
- The past-lens section title is the domain's 공개됨 · 1-5.

No domain module was changed.

## batch 2

Built WLD-05 07 13 14 15 16 20 21 22 28 29 31 32, plus most of WLD-33 (event detail) and WLD-34 (view chips work). Status file Architecture lists every new file; read it first.

New packages and files: `time/` (Timeline, EventDetail, WorldCalendar route = 세계 시간, the shared EventCreateSheet and a local EventEditSheet), `common/ItemLifecycle` + `MergePlan` + `LifecycleSheets` (hide, delete with impact sheet, restore from the card, merge with comparison sheet), `common/CardExtras` (image slot, gone card with 되살리기, same-name line), `list/WorldCardView` (목록 | 카드), `term/RevealSheets` (self-contained 공개 시점 옮기기, registered with the shell), `term/AliasesAndRejections` (alias editor and 아님으로 둔 것).

Conventions set this batch:
- Shared or cross-screen sheet content reads `currentProjectStore` and a keyed `entryViewModel(key = "world.<x>.$projectKey")` (see `rememberEventEditor`, `RevealEditorViewModel`). Never pass a screen's view model into shared sheet content.
- Deletes go through `ItemLifecycle.delete` (TrashOps) and merges through `WorldViewModel.merge` (MergeActions.mergeItems, plus recorder links). Both are one undo step with a toast carrying `되돌리기`.
- Pickers inside a sheet replace its content (a saveable `pick` enum), so the draft survives under them.
- Lock masking on time screens comes from the new domain overloads `TimelineBuilder.build(..., hiddenUnlocked)` and `EventDetails.detail(eventKey, hiddenUnlocked)`.

domain:world changed (additive, tests in `LockedTimelineTest`): those two overloads, `EventMasks`, `WorldTimeEdits`.

Batch 3 slots (see the status file "How to extend"):
- WLD-23: group action above the 후보 group, plus register `LinkCandidatesSheet`.
- WLD-24: `맞음` on the card ▲ rows.
- WLD-26: `RescanScheduler` in `TermIndexes.Holder.run`. Also release stale 아님 records there, not as UI writes.
- WLD-30: calendar editor in `WorldTimeScreen`.
- WLD-33: `+ 변화`.
- WLD-35: ages are already in the rows.
- WLD-63 and WLD-64: TimelineMap and the place time tab.
- TravelModes route (map lane pushes it from the route sheet `직접`).

## batch 3

Last batch of this lane. Built WLD-23 24 25 26 27 30 33 34 35 63 64 68 69, the routes `TimelineMap` and `TravelModes`, and the shared sheet `LinkCandidatesSheet`; every world route and both WORLD shared sheets are registered. `tools/gw :feature:world:compileDebugKotlin :feature:world:testDebugUnitTest` exits 0 (135 tests, 22 classes); `tools/gw :domain:world:test` exits 0 (123, 1 pre-existing ignore). Nothing seen on a device.

New files (status file Architecture lists them with roles): `common/RescanPlan` (pure doc diff) and a rewritten `common/TermIndexes` on `RescanScheduler`; `term/LinkCandidatesState`, `term/LinkCandidatesSheet`; `time/EventChanges`, `time/EventChangeSheet`, `time/CalendarDraft`, `time/CalendarEditor`, `time/TimelineMapState`, `time/TimelineMapScreen`; `travel/TravelModesState`, `travel/TravelModesScreen`; `list/ListCardMotion`.

domain:world additive: `term/ManuscriptMarks` (WLD-27 contract for the script), `term/StaleRejections` (fresh-scan release of stale `아님`), `WorldTimeEdits.setCalendar`, a lock-aware `PlaceTimes.rows(placeKey, lens, hiddenUnlocked)`; test `ManuscriptContractTest`.

Conventions added:
- The term index never writes the document. Stale `아님` are hidden at read time (`StaleRejections.stale`) and released inside world term writes (`TermCardViewModel.termWrite`); the script should release them inside its block edit (Requests).
- Scans of one project run on one thread (`limitedParallelism(1)`); feed documents with `Handle.submit`, never call the scheduler directly.
- Sheets that need the index (bulk sheet) acquire their own `TermIndexes` handle in a keyed `entryViewModel`.

Open threads (Requests in the status file): `LinkCandidatesSheet` mode argument and an event anchor on `FieldValueEditSheet` (core:navigation); an undo-free maintenance write (core:store); a shared read-only map composable (feature:map) to replace the 연표 + 지도 schematic; cast `MergeItemsSheet` (then drop `common/LifecycleSheets` merge copies); many TODO_COPY lines; the optional switch of `common/LensBanner` to core:ui `ProjectLensEditBanner` and of the description editor to core:ui `RichTextEditor` (would draw links inside the editor) was not done.
