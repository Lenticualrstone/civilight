# feature:story

Package `io.github.lenticualrstone.civilight.feature.story`. Build and tests: `tools/gw :feature:story:compileDebugKotlin :feature:story:testDebugUnitTest` exits 0, with 59 JVM tests in 9 classes, all passing (batch 1, 2026-11-22). No domain module was changed. Nothing was run on a device or emulator, and no APK was built.

## Architecture

**Entries.** `StoryEntries.kt` (`@file:JvmName("StoryEntries")`, `storyEntries(deps)`, shell.md §1) registers the following.
- Routes: `ProjectHome`, `StoryStructure` and `SceneNotationSettings`.
- Shared sheets: `UnitMenuSheet` and `NewUnitSheet`.
- The feature-local `RenumberSheet`. It is registered in the `SheetRegistry` so that it also opens over the script, where `UnitMenuSheet` is hosted.

The other story routes (`CheckCenter`, `ProjectSearch`, `LibrarySearch`, `BulkPlaceTime`, `DayBandSettings`) and sheets (`SceneMoveSheet`, `SceneWhereSheet`) still show the shell placeholder. Each later batch adds one `entry<…>` / `register<…>` line here.

**Packages** (one per screen or sheet family; each has a `…Copy` object, a pure model and thin composables):

| Package | Files | What |
| --- | --- | --- |
| `common` | `StoryViewModel.kt`, `StoryActions.kt`, `StoryCopy.kt` | `StoryServices` holds the app-scoped services a view model may keep: projects, clock, device id, toasts, appScope, unlock and the recorder store. Build it with `deps.storyServices()`. Never keep `FeatureDeps` or the navigator in a view model. `StoryViewModel` is the base of every story view model: a `ProjectViewModel` plus `apply(label) { doc, ctx -> doc' }`, where one call is one undo step and the `ActionContext` comes from `services.actionContext()`. It also carries the shared edits `deleteScene` (with toast and `되돌리기`), `newUnitAfter`, `newTopUnit`, `setDone` and `setNotation`. `StoryActions` holds those edits as pure `ProjectDoc` functions over `domain:actions` / `domain:story`. `StoryActionsViewModel` is the view model the story sheets use inside any host entry: `storyActions(projectKey, deps)` returns `entryViewModel(key = "story.actions")`. |
| `home` | `HomeModel.kt`, `HomeStart.kt`, `HomeViewModel.kt`, `HomeScreen.kt`, `HomeCopy.kt` | 프로젝트 홈 (STO-01). `HomeModel.build` returns `HomeUi`: stats, `이어 쓰기`, current-chapter rows, shortcuts and the lens chip label. `HomeStartModel` builds the 만든 직후 block (LIB-13 content, board `NewReady`). `HomeViewModel` combines the project, the hidden-item lock and the recorder library. |
| `structure` | `StructureModel.kt`, `StructureViewModel.kt`, `StructureScreen.kt`, `StructureCopy.kt` | 스토리 구조 (STO-04). `StructureModel.build(doc, expanded, locked, readOnly)` returns a flat `StructureItem` list (`GroupItem` · `EpisodeItem` · `SceneItem`) for 1 to 4 levels, plus the `새 씬` target. The expanded keys are `rememberSaveable` screen state, with the newest last. |
| `menu` | `MenuModels.kt`, `MenuSheets.kt`, `RenumberModel.kt`, `RenumberSheet.kt`, `MenuCopy.kt` | The ⋯ menus (STO-07) as data: `UnitMenuModel` (scene, episode, chapter; `MenuSurface.STORY_LIST` or `SCENE_HEAD`) and `StructureMenuModel`. Each `MenuRow` carries a `MenuAction`, a label, a value and `enabled`; in read-only projects every mutating row shows `읽기 전용`. `UnitMenuContent` is the shared `UnitMenuSheet` content. `StructureMenuSheet` is a feature-local key. `RenumberSheet` shows the `번호 다시 매기기 · 1-7` preview over `domain:script` `Numbering`. |
| `newunit` | `NewSceneForm.kt`, `NewUnitSheetContent.kt`, `NewUnitCopy.kt` | `NewUnitSheet` content: `다음 씬 만들기` (`afterKey`, or the end of the episode) and `앞에 새 씬` (`beforeKey`). `NewSceneForms` handles setup and prefill (`SceneActions.prefillAfter` / `prefillBefore`), value lines, `비우기`, the place picker swap, the transition chips, and `create` through `SceneActions.create`. |
| `settings` | `NotationModel.kt`, `NotationScreen.kt`, `NotationCopy.kt` | 프로젝트 설정 › `때 표기` (STO-17), opened from LIB-69 as `SceneNotationSettings(p)`. Two radio rows, each showing the sample scene's label under it. |

**How to extend** (next batches):
- **New screen:** add a package with `XCopy`, `XModel` (pure, tested), `XViewModel : StoryViewModel(projectKey, services)` and `XScreen(route, deps)`. Create the view model with `entryViewModel { XViewModel(route.projectKey, deps.storyServices()) }`. Wrap the screen in `CenterColumn { CivilightScreen(…) }`. Hide the back button when `LocalPaneRole.current == PaneRole.DETAIL`.
- **New edit:** write a pure function (in `StoryActions` or the screen's model) that returns the new doc, or the same doc when its precondition no longer holds. Call it through `apply(label)`. Scene structure and time edits go through `domain:actions` `SceneActions`; never call `domain:script` `SceneSplit` / `SceneMerge` / `SceneLifecycle`.
- **Shared sheet (STORY):** register it in `storyEntries` with `deps.sheets.register<K> { sheet -> KContent(sheet, deps) }`. Inside the content, read the host's store via `currentProjectStore`, take edits from `storyActions(projectKey, deps)`, and start with `SheetHeader`. Swap to another sheet with `close()` + `open(next)` (`replaceWith` in `MenuSheets.kt`).
- **Menu rows:** add a `MenuAction`, a row in `UnitMenuModel` / `StructureMenuModel`, and its branch in `UnitMenuContent`. `SceneMoveSheet` (STO-08) belongs there as `옮기기`. `SceneWhereSheet` (STO-20) is already opened by `장소와 때` and `전환 문구`.
- **Copy:** add every new `…Copy` object to `StoryCopyLintTest.copyObjects`, and button constant names to its `buttons` set.

## Done

Pure logic with passing tests. The screens that use it are listed under Partial, because nobody has seen them on a device yet.
- **STO-03 labels in rows:** come from `domain:story` `UnitCodes` / `StoryOverview` (`StructureModelTest`, `HomeModelTest`).
- **STO-06 statuses, progress, char counts, the `완료` flag:**
  - Row values: `StructureModelTest.str01…`, `str02…`, `str04…`, `HomeModelTest.str05_currentChapterRows`.
  - The flag: `StoryActionsTest.doneFlagIsTheAuthorsAndRepeatsAreNoOps_STO06`.
- **STO-10 delete and undo through `SceneActions`:** `StoryActionsTest.del01_…`, `del02_…`, `lens12_…`, `deleteIsOnlyForLiveScenes`.
- **STO-17 `때 표기`:** `NotationModelTest` (samples `1-7 · 2일째 · 새벽` / `1-7 · 약 13일째 · 새벽`), `StoryActionsTest.notationChangesLabelsOnly_STO17`.
- **STO-05 new scene form:** `NewSceneFormTest` (INS-01 both ways, TIME-12 base skipping the flashback, `앞에 새 씬`, empty episode, picked place, direct text, `자동` needs a place, stale sheets). New units: `StoryActionsTest.newEpisode…`, `newChapter…`.
- **STO-07 menus:** `MenuModelTest` (story-list rows, first-scene merge disabled, the scene-head row set equal to `SceneHeads.menu`, read-only, the episode ⋯ with `번호 다시 매기기 · 1-7`, chapter, structure ⋯ per level count). Renumber: `RenumberModelTest`.
- **Copy lint:** `StoryCopyLintTest` covers `StoryCopy`, `HomeCopy`, `StructureCopy`, `MenuCopy`, `NewUnitCopy` and `NotationCopy`.

## Partial

UI features are built and wired to the store, but have not been seen on a device.
- **STO-01 프로젝트 홈:**
  - Done: the cover layout follows `Home`: head `‹ 서재 · 프로젝트`, rating badge, lens chip → `LensSheet`, name, one-liner, the three stats, `이어 쓰기` (→ `Script(blockKey, caretAtEnd = true)`), the current chapter's rows (→ `StoryStructure(focusUnitKey)` via `openDetail`, so the inner `TwoHome` puts the structure in the right pane), the `작업` shortcuts and the command bar (→ `ProjectSearch`, mic → `Recording`).
  - Done: the `시작해 볼 것` block (`NewReady`): summary line with rating, `바로 쓰기` + `쓰기`, `시작해 볼 것 · n/4` + `목록 닫기` (writes `startChecklist.dismissed` through `updateApp`), and four rows with their targets (`HomeStartModelTest`).
  - Done: while nothing has been written (`HomeUi.newReady`: block shown and no `이어 쓰기` target), home follows `NewReady`. Once there is something to resume, home is `Home` again (head chip, stats, `이어 쓰기`) with the block above the chapter list.
  - Left: STO-43 inner fine-tuning (`ColHome` 560 column comes from `CenterColumn`). The command-bar hint in `NewReady` mode keeps home's hint (the board shows `명령 또는 검색`).
  - Left: `오늘 쓴 글자` only grows once the script records day baselines (`domain:script` `WritingCounts`, script lane).
- **STO-02:** the level line `단계 이름: 장 › 에피소드 › 씬` and the structure ⋯ row `단계 이름 | 장 › 에피소드 › 씬` (→ `StructureLevels`, LIB-69 editor). Rename and level-count lock are `domain:story` `StructureEdit`. The level editor screen is LIB's.
- **STO-04 스토리 구조:**
  - Done: the cover `Story` layout: head with the project name and ⋯, level line, chapter headers with progress, planned chapters with synopsis (tap toggles their rows), episode rows (grip, code, title bold when expanded or current, meta, ⋯), scene rows (time column, label, meta, `sel` + `acc` bar for the scene being written, ⋯), `새 씬` primary above the command bar, and no lens chip.
  - Done: tapping an episode expands or collapses it; tapping a scene opens `Script(sceneKey)`.
  - Left: the grip is drawn but drag is STO-08 (batch 2+).
  - Left: the time and code columns are minimum widths, so a long label (`약 13일째 새벽` under `이야기 시작부터`, a 4-level code) pushes that row's title right and titles no longer line up. A measured column width (widest label in the list) would fix it.
- **STO-05:**
  - Done: `새 씬` opens `NewUnitSheet(parent, after)`. Episode ⋯ and structure ⋯ create the next episode and chapter (copy not drawn).
  - Left: 때 in the sheet can only be kept or cleared (`비우기`); editing it is the `WhenPicker` of STO-20/21.
  - Left: `새 씬` is disabled in 1-level projects (see Requests).
- **STO-07:**
  - Done: scene ⋯ `장소와 때 · 전환 문구 · 앞 씬과 합치기 · 여기서 나누기 · 씬 지우기` (+ `완료`). Episode ⋯ `번호 다시 매기기 · 1-7` (+ `새 에피소드`, `완료`). Chapter ⋯ (`새 장`). Structure ⋯ `장소 · 때 한꺼번에 · 단계 이름` (+ `새 장`).
  - Waiting on other screens: `장소와 때` / `전환 문구` open `SceneWhereSheet`, whose content is STO-20 (placeholder until a later batch). `앞 씬과 합치기` opens SCR's `SceneMergeSheet`. `장소 · 때 한꺼번에` pushes `BulkPlaceTime` (STO-31, placeholder until later).
  - Left: `옮기기` (STO-08) is not in the scene ⋯ yet.
- **STO-10:** `씬 지우기` from the scene ⋯ deletes without asking. The toast `씬 n 지움 · …` + `되돌리기` reverts on `appScope` with its own session, so it still works after the screen is gone. Restore from the 휴지통 is the library lane's trash screen, through `SceneActions.restore`.
- **STO-12 / STO-13:** the engine and the lens sheet are `domain:story` and the shell. This module shows the chip on home (`LensRules.chipLabel`) and opens `LensSheet`. Not shown on 스토리 구조 (R11 §3).
- **STO-17:** the `때 표기` screen is built. Its row in 프로젝트 설정 (`때 표기 | … | 에피소드 기준 ›`) is LIB-69's, and must push `SceneNotationSettings(p)`. The app-level default (`때 표기 기본`) is LIB's.

## Defaults used

- **Home by hierarchy:**
  - 3+ levels: the current chapter (the top-level unit of the last edit, else the first) and its episodes.
  - 2 levels: every top-level unit as an episode row, with no header.
  - 1 level: the scenes. The first stat is `k/n` with the episode or scene level name.
- **`시작해 볼 것` block:**
  - Shown while `startChecklist.dismissed` is false, never on a read-only project (the example). Imported projects show it too until closed, because `startChecklist` is app state and is not exported.
  - While shown and nothing is written yet (no `이어 쓰기` target), home follows `NewReady`: head `새 프로젝트`, no rating badge or lens chip in the head (the rating sits in the summary line), no stats. The chapter rows and `작업` stay below. Once there is something to resume, home is `Home` with the block between `이어 쓰기` and the chapter list.
  - Row targets: `주인공 만들기` → `CastList`; `연결된 기록 …` → `RecorderInbox()` (unfiltered); `세계 용어 하나 적기` → `World(TERMS)`; `첫 대사 쓰기` and `쓰기` → `Script` at the first scene.
- **Structure:**
  - On entry the episode of `focusUnitKey`, else the episode of the last edit, is expanded, plus any planned chapter above it.
  - `새 씬` goes into the most recently expanded episode (else the episode of the last edit): after the scene being written when it is in that episode, else after the last scene.
  - The ⋯ sits at the end of every episode and scene row.
- **Menus:**
  - `여기서 나누기` opens the scene in the script (Q5).
  - `앞 씬과 합치기` on an episode's first scene is drawn disabled.
  - `완료` is a row in the scene and episode ⋯ (copy not drawn).
  - Opening a sheet from a ⋯ replaces the menu sheet.
- **New scene sheet:** an empty episode starts with nothing pre-filled. `직접` with no text is created as `없음`. The undo label is `새 씬`.
- **Renumber:** `다시 매기기` is disabled when no number would change, and in read-only projects. The undo label is the sheet title.
- **`때 표기` sample:** the 집필 선 scene when it has 때, else the first timed scene; no sample when nothing is timed.
- **Scene-row columns:** the time and code columns have the board widths (64, 30) as minimums, so `약 13일째 새벽` and `1-2-3` are not cut.

## Spec conflicts

- **`NewReady` text extract vs HTML:** `docs/boards/boards_text.txt` lists row 2 as `가져온 기록 2개 정리하기 | 프로젝트 메모`, while the HTML board and LIB-13 say `연결된 기록 2개 정리하기 | 기록함`. The HTML and spec are followed (via `domain:library` `StartBlock`).
- **`시작해 볼 것 · n/4`** keeps `/4` when the `연결된 기록` row is hidden (k = 0, LIB-13 `ED`), so a fresh project reads `0/4` with three rows. This is `domain:library`'s header; it is flagged here for the library lane.

## Requests

- **core:navigation, shared sheet for SCR-22:** add `RenumberSheet(projectKey, episodeKey)` with owner SCRIPT. Until then feature:story draws a feature-local `RenumberSheet` from `domain:script` `Numbering.renumberPreview` / `renumber`. When the shared key exists, the script lane registers the content, and this module switches `MenuAction.RENUMBER` to it and drops its local key.
- **domain:actions `SceneActions.create` (and `domain:story` `NewScene.create`):** allow `parentKey = null` for 1-level structures, so `새 씬` works there. It is disabled in 1-level projects until then.
- **core:navigation, a route or argument for "new term"** (LIB-13 `세계 용어 하나 적기` → World › new term), for example `World(p, tab, createTerm = true)`. Until then it opens the 용어 tab.
- **feature:recorder, a `RecorderInbox(filter)` id for "records linked to project p"** (LIB-13 row 2). Until then it opens unfiltered.
- **feature:script:** record day baselines (`domain:script` `WritingCounts`) on block edits, so home's `오늘 쓴 글자` counts the day's growth. Also call `ProjectActivity.lastEditedBlockKey` updates, which `이어 쓰기` and `쓰는 중` read.
- **feature:library (LIB-69):** the 프로젝트 설정 row `때 표기` should push `SceneNotationSettings(p)`.
- **Copy needed (LIB-13, rendered on home):** `TODO_COPY_LIB13_PROTAGONIST_SUB` (`domain:library` `StartBlock`) is the `주인공 만들기` sub line while no character exists.
- **Copy needed (TODO_COPY, 20-story-time §6 Q34):**
  - `TODO_COPY_STO_NEW_EPISODE`: episode ⋯ / structure ⋯ (2 levels), "new episode after this one".
  - `TODO_COPY_STO_NEW_CHAPTER`: chapter ⋯ / structure ⋯, "new chapter at the end".
  - `TODO_COPY_STO_MARK_DONE` / `TODO_COPY_STO_UNMARK_DONE`: the `완료` toggle in the scene and episode ⋯.
- **FEATURES.md** (not this module's file to edit): STO-01, 02, 04, 05, 07, 10, 13, 17 → partial (feature side); STO-03, 06 row logic → done in `domain:story` plus this module's tests.
