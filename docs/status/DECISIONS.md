# Decisions for the feature phase (orchestrator, 2026-11-22)

Every feature agent follows this file. It settles the "pick one" requests from the foundation reports, so all eight feature lanes call the same code for the same user action. When this file and a status file disagree, this file wins.

## 1. One write path per action

| Action | Use | Do not use |
| --- | --- | --- |
| New scene, split, merge, move, delete, restore (structure and time) | `domain:story` `scene.*` (`NewScene`, `SceneSplit`, `SceneMerge`, `SceneMove`, `SceneTrash`, `Rebase`, `EditContext`), through `domain:actions` `SceneActions` | `domain:script` `SceneSplit`, `SceneMerge`, `SceneLifecycle`, `SceneCreate` (kept only for its cross-check tests) |
| Block edits inside a scene (insert, split at caret, merge, kind, move, copy, delete to trash, restore position, numbering, voice IDs) | `domain:script` | — |
| Scene delete or restore with trash entry, 때 rebase, reference settle and the toast | `domain:actions` `SceneActions.delete` / `restore` (composes `SceneTrash` + `TrashOps.settleDelete` / `RestoreOps.settleRestore` + `Messages.sceneDeleteToast`) | calling those pieces by hand in a feature |
| Character delete, restore, purge | `domain:actions` `CharacterActions` (writes the `DeletedItems` snapshot into the trash entry, then `domain:trash`) | `domain:cast` `refs/*` write helpers |
| Character merge and world-item merge | `domain:actions` `MergeActions` (one engine; `domain:actions` agent picks the engine and records it below in §4). Preview and per-field choices: `domain:cast` `MergePreview` | the other engine |
| Every other delete / restore / purge inside a project (world items, fields, relations, pages, canvases, maps, changes) | `domain:trash` (`TrashOps`, `RestoreOps`, `PurgeOps`) | per-area copies |
| Trash list rows inside a project (filters incl. 세계, 변화) | `domain:trash` `TrashListing` | `domain:library` `trash.TrashHost` |
| Whole-project trash (서재 휴지통) | `domain:library` `ProjectTrash` + `core:store` `LibraryStore` | — |
| Daily purge and start-up maintenance | `domain:actions` `PurgeActions.runDue(...)` (project rows, whole projects, recorder records, blob pruning list) | — |
| Delete impact sheet counts (rule 20) | `domain:trash` impact computation, shown by the shared `DeleteImpactSheet` (owner: cast) | — |
| Term usage, candidates, 잇기 · 아님 · 모두 잇기, rescan, `@` alias search | `domain:world` (`TermQueries`, `TermCandidates`) | — |
| Lens (집필 선, values at a point) and the two edit buttons (`이 값 고치기` · `여기서부터 바뀜`) | `domain:story` `LensEdit` (anchors). `domain:cast` `TimedValues` delegates to it | separate anchor logic |
| Scene time, N일째, bands, transitions, travel check, check-center rows and acknowledgements | `domain:story` (+ `Fingerprints`), `domain:checks` for the 점검 센터 list | `domain:script` transition checks |
| Distances on maps (WLD-66) | `domain:world` `map/Distances` | — (`domain:story` travel keeps its own until the review round; `domain:actions` has a test that both agree on the example project) |
| Recorder 넣기 (원고에 넣기 · 카드에 넣기 · 페이지로 만들기) | `domain:recorder` `put/*` (copies runs, LRC-32), through `domain:actions` `RecorderActions` | `domain:script` `RecorderMemo`, `domain:cast` `createFromRecord` / `appendMemo` |
| Recorder record delete / restore / purge | `domain:trash` `RecorderTrash` | `domain:recorder` `trash/RecordTrash` |
| Recorder records inside a .civilight (export slice, merge on import) | `domain:library` `RecorderSlice`, `RecorderMerge` | `domain:recorder` `export/*` |
| .civilight read/write, upgrade chain, encryption, import decisions | `domain:library` | — |
| Export range, hidden scenes, voice-ID fixation (PDF, CSV, engine JSON) | `domain:library` `ExportPlan` / `ExportRun`; CSV and engine JSON serialisers from `domain:script` adapted in `domain:actions` `ExportActions`; PDF drawn in `feature:library` (Android `PdfDocument`) from the same plan | `domain:script` `ExportScope` deciding ranges on its own |
| App lock and hidden-project lock rules | `core:ui` `LockRules` (the app already uses it) | `domain:library` `lock/*` |
| Unit labels (`1-7 씬 3`) | `core:ui` `UnitLabels` in UI, `domain:story` `UnitCodes` in domain code | `domain:recorder` `link/UnitLabels` for new code |
| Rating badge text | `domain:library` `RatingLabels` / `core:ui` `ThemeBridge.rating` | `domain:recorder` copy |
| Korean initial search (초성) | `core:ui` `KoreanSearch` | per-area copies |

## 2. Who owns what during the feature phase

- **Shared modules are frozen** while feature lanes run: `core:model`, `core:store`, `core:designsystem`, `core:navigation`, `core:ui`, `app`, `domain:actions` and every build file. A lane that needs something there writes it under "## Requests" in its status file and works around it inside its own module (a feature-local sheet key, a local helper). The orchestrator applies requests between waves.
- **Domain gaps.** A lane may make **additive** edits to its own domain module(s) below: new files, new functions, bug fixes with a test. It must not rename, remove or change the signature of anything public, because other lanes compile against it. It runs that domain module's tests after each change.

| Lane (feature module) | May also edit (additive) |
| --- | --- |
| `feature:library` | `domain:library` |
| `feature:story` | `domain:story`, `domain:checks` |
| `feature:script` | `domain:script` |
| `feature:cast` | `domain:cast` |
| `feature:world` | `domain:world` except its `map` package |
| `feature:map` | `domain:world` `map` package only |
| `feature:canvas` | — (canvas logic lives in `feature:canvas`, with JVM tests) |
| `feature:recorder` | `domain:recorder` |

`domain:trash` has no lane: requests only.

- **Shared sheets.** The owner named in `core:navigation` `Sheets.kt` (see `docs/status/shell.md` §5) builds and registers the content. Other lanes only open them.

## 3. Conventions

- **UI copy** is Kotlin constants in one `…Copy` object per feature package (as `core:ui` `LockScreens` does), never `strings.xml`. Each feature module has a JVM test that runs `core:designsystem` `CopyLint` over every value of its copy objects. Copy is exactly as the specs and boards give it: terse fragments joined with ` · `, no 해요체, no em dash. Missing copy is a `TODO_COPY_<AREA>_<NAME>` constant listed under Requests, never invented prose.
- **Screens** use `CivilightScreen`, the DS components and `ProjectViewModel` from the shell contract (`docs/status/shell.md`). Every screen works at cover width (≈390 dp, Compact) and on the inner screen (Medium/Expanded) as the spec and boards draw it.
- **Logic** that can be tested on the JVM lives in plain Kotlin (view-model state reducers, row builders) with JUnit tests; composables stay thin.
- **No APK.** The user holds release and APK builds. Feature agents run only `tools/gw :<module>:compileDebugKotlin :<module>:testDebugUnitTest` (plus the tests of a domain module they changed). No `assemble*`, `bundle*`, `install*`, release tasks, emulator or device.
- **Status.** A UI feature is at most `partial` until the orchestrator has seen it running on the emulator; say what was built and what is left.

## 4. Records from the glue step

Filled by the `domain:actions` agent (2026-11-22); details and tests in `docs/status/domain-actions.md`.

**Merge engine: `domain:trash` `MergeOps`.** `MergeActions.mergeCharacters` and `MergeActions.mergeItems` both run it; `domain:cast` `CharacterMerge` is not called by any action (kept for its own tests).
- Why: it is the only engine for all four named kinds (characters, places, terms, factions), so characters and world items share one write path. Its undo is the same row-level `DocChange.revert` every other trash operation uses. Restoring a merged item is wired to it (`RestoreOps.restore` → `MergeOps.restoreMerged`), and it has per-field choices (`MergeChoices.fieldsFromMerged`, relation slots, term reveal).
- `CharacterMerge` covers characters only, and its undo needs the in-memory `MergeSnapshot` and its own `movedRefs` strings.
- What the cast engine did and the chosen one lacked is now routed through it:
  - **Record-only values:** a field whose value lives only in `◆` records is not empty and is never filled over. Fixed in `MergeOps.preview`.
  - **Records of a taken field:** they move to the target, and a target record at the same point gives way. Fixed in `MergeOps.merge`.
  - Both fixes have tests in `domain:trash` `MergeFieldHistoryTest`.
  - **Character-only cells** (`소개 · 표시 이름` with its records · `태그 · 말투 · 비밀`) are applied by `MergeActions.mergeCharacters` after the engine, with the cast sheet's choices.
- The comparison sheet stays `domain:cast` `MergePlanner.preview` (`MergeActions.characterSheet`); `MergeActions.engineChoices` maps its `MergeChoices` onto the engine's.

**API names that differ from §1:**
- **Whole-project trash:** `ProjectTrash` lives in `domain:trash` (`ProjectTrash`, `ProjectTrashState`), not `domain:library`; `domain:library` only has `trash.TrashHost` (rows and countdown). `PurgeActions` uses the `domain:trash` one.
- **`SceneActions.restore`, `restoreEntry`, `restoreBlocks`:** they return `domain:trash` `RestoreOutcome`, the type `RestoreOps` returns, so trash rows handle one outcome type. Scene structure goes through `SceneActions.create` · `split` · `merge` · `move` · `reorderUnit` · `prefillAfter` · `prefillBefore`.
- **`CharacterActions`:** `impact`, `delete`, `undo`, `restore` (returns `CharacterRestoreResult(outcome, toast)`), `purge` (takes and returns the recorder library).
- **`MergeActions`:** `characterSheet`, `itemPreview`, `engineChoices`, `mergeCharacters`, `mergeItems`, `undo` (takes and returns the recorder library), `restoreLine`.
- **`PurgeActions.runDue`:** takes `(now, projects, projectTrash, recorderTrash, zone, device, newKey)`, or an `ActionContext` overload, and returns `PurgeRun` (projects, item purges, project trash, purged projects, recorder trash and log, `blobsToDelete`). `withPlainUnitLabels` stores the plain unit label (`1-7 씬 3`) as `RecorderLink.lastLabel`.
- **`RecorderActions`:** `putInManuscript`, `putInCard`, `makePage`, `undoPut`, `delete`, `restore`, `purge`.
- **`ExportActions`:** `run(doc, options, authenticated, now)`. The serialisers are `ExportActions.CSV` and `ExportActions.ENGINE_JSON` (`domain.library.export.ExportSerializer`), `serializerFor(format)` (null for PDF and `.civilight`), and `voiceIdOf(doc)`. The script's `DialogueCsvExport.export` and `GameJsonExport.export` are not used, because they would fix voice IDs on their own.
- **Contexts:** every action takes one `ActionContext(now, zone, device, newKey)`; `.edit`, `.trash` and `.recorder` give the rule modules' own context types.

**Distances (WLD-66):** `domain:story` travel and `domain:world` `map/Distances` agree within 1 m on the example project, for both routes and every pinned place pair (`DistanceConsistencyTest`). Nothing is ignored.
