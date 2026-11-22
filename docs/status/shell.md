# shell (core:navigation · core:ui · app)

Packages: `io.github.lenticualrstone.civilight.navigation` (core:navigation), `io.github.lenticualrstone.civilight.ui.*` (core:ui), `io.github.lenticualrstone.civilight` + `.shell` (app).

**Build and tests.** `tools/gw :core:navigation:testDebugUnitTest :core:ui:testDebugUnitTest` exits 0: 12 tests in core:navigation (`NavigatorTest` 6, `RouteSerializationTest` 6) and 42 in core:ui (`FeatureContractTest` 3, `KoreanSearchTest` 3, `LabelsAndLensTest` 7, `LockRulesTest` 8, `PickerModelsTest` 9, `ProjectViewModelTest` 2, `SessionsAndToastsTest` 3, `ShellRulesTest` 7), all passing. `tools/gw :app:processDebugResources :app:compileDebugKotlin` exits 0, so the app's code, manifest, themes and resources build.

**No APK was built.** The user asked to hold release and APK builds, so `:app:assembleDebug`, `bundle*`, `install*` and every release task were not run. Any APK under `app/build/outputs` is from an earlier run, not from this code. Nothing was run on a device or emulator.

---

## Feature entry contract (all feature agents follow this)

### 1. The entries function: one per feature module, with a fixed JVM name

Each feature module has exactly one file declaring its entries function. The app finds it by reflection, by the class and method names below. Copy the annotation and the signature exactly.

| Module | File (in `src/main/java/io/github/lenticualrstone/civilight/feature/<x>/`) | First lines |
| --- | --- | --- |
| `:feature:library` | `LibraryEntries.kt` | `@file:JvmName("LibraryEntries")` · `package io.github.lenticualrstone.civilight.feature.library` · `fun EntryProviderScope<NavKey>.libraryEntries(deps: FeatureDeps)` |
| `:feature:story` | `StoryEntries.kt` | `@file:JvmName("StoryEntries")` · `…feature.story` · `fun EntryProviderScope<NavKey>.storyEntries(deps: FeatureDeps)` |
| `:feature:script` | `ScriptEntries.kt` | `@file:JvmName("ScriptEntries")` · `…feature.script` · `fun EntryProviderScope<NavKey>.scriptEntries(deps: FeatureDeps)` |
| `:feature:cast` | `CastEntries.kt` | `@file:JvmName("CastEntries")` · `…feature.cast` · `fun EntryProviderScope<NavKey>.castEntries(deps: FeatureDeps)` |
| `:feature:world` | `WorldEntries.kt` | `@file:JvmName("WorldEntries")` · `…feature.world` · `fun EntryProviderScope<NavKey>.worldEntries(deps: FeatureDeps)` |
| `:feature:map` | `MapEntries.kt` | `@file:JvmName("MapEntries")` · `…feature.map` · `fun EntryProviderScope<NavKey>.mapEntries(deps: FeatureDeps)` |
| `:feature:canvas` | `CanvasEntries.kt` | `@file:JvmName("CanvasEntries")` · `…feature.canvas` · `fun EntryProviderScope<NavKey>.canvasEntries(deps: FeatureDeps)` |
| `:feature:recorder` | `RecorderEntries.kt` | `@file:JvmName("RecorderEntries")` · `…feature.recorder` · `fun EntryProviderScope<NavKey>.recorderEntries(deps: FeatureDeps)` |

Imports: `androidx.navigation3.runtime.EntryProviderScope`, `androidx.navigation3.runtime.NavKey`, `io.github.lenticualrstone.civilight.ui.feature.FeatureDeps`. The names are also in code as `FeatureContract.bindings` (core:ui).

A complete example that compiles and is loaded by the real loader in a unit test is `core/ui/src/test/java/io/github/lenticualrstone/civilight/ui/fakefeature/FakeEntries.kt`. It shows a route entry, a screen with a local sheet that uses `CharacterPickerSheet`, and a shared sheet registration (`FeatureContractTest`).

```kotlin
@file:JvmName("ScriptEntries")

package io.github.lenticualrstone.civilight.feature.script

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.navigation.SceneSplitSheet
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps

fun EntryProviderScope<NavKey>.scriptEntries(deps: FeatureDeps) {
    entry<Script> { route -> ScriptScreen(route, deps) }
    entry<ScriptAssets> { route -> AssetsScreen(route, deps) }
    deps.sheets.register<SceneSplitSheet> { sheet -> SceneSplitSheetContent(sheet, this) }
}
```

Rules:
- Register only the routes your module owns. `Route.owner` in core:navigation says which module that is. Navigation3 rejects a second registration of the same route class, and the app fails at startup.
- If the class is missing, the app shows its placeholder screen for your routes. If the class exists but the function is missing or has another signature, the app throws at startup on purpose.
- The function runs once per activity, inside `entryProvider { }`. Only register entries and sheets there. Do no I/O and start no work.
- Routes you have not registered yet keep the placeholder (`PlaceholderScreen`: the route name, the owning module and its arguments).
- The shell wraps every entry. Do not do any of the following yourself: add the route to the entry metadata, wrap project routes in `ProjectEntryFrame`, run the script lens hook. Your `entry<…>` content only draws your screen.

### 2. FeatureDeps (core:ui, `ui.feature.FeatureDeps`)

You receive it as the entries function's parameter. In composition it is also `LocalFeatureDeps.current`.

| Member | What |
| --- | --- |
| `stores: StoreContainer` | library, recorder, blobs, settings, appState, clock, deviceId (core:store API) |
| `navigator: Navigator` | `push`, `replace`, `pop`, `popTo(inclusive) { }`, `popTo(route)`, `popToType<R>()`, `resetTo(…)`, `backStack`, `current` |
| `settings: StateFlow<AppSettings>` | always current; write through `stores.settings.update { }` / typed setters |
| `biometric: BiometricGate` | `suspend authenticate(AuthRequest(title, subtitle, credentialOnly)): AuthResult` (`SUCCESS · CANCELLED · FAILED · UNAVAILABLE`), `availability()`. On cancel or fail do nothing and show no toast (LIB-37) |
| `clock: AppClock` | the injected clock for all date logic |
| `windowLayout: State<WindowLayout>` | also `LocalWindowLayout.current`: `widthDp`, `widthClass` (`COMPACT · MEDIUM · EXPANDED`), `isInner`, `twoPanes`, `centerColumn`, `posture` (always `UNKNOWN` for now) |
| `projects: ProjectSessions` | shared open project stores: `acquire`/`release`, `peek`, `peekDoc`. Normally use `ProjectViewModel` or `currentProjectStore` instead |
| `unlock: UnlockController` | `state: StateFlow<UnlockState>` (`appLocked`, `hiddenGroupOpen`, `itemsOpen(projectKey)`, `recordsOpen`); `unlockHiddenGroup()`, `unlockItems(projectKey)`, `unlockRecords()`, `relockNow()` (`다시 잠그기`), `lockHiddenGroup()`, `appLockDisabled()` |
| `capture: CaptureGuard` | window protection requests. Prefer the `ProtectWindow(WindowProtection)` composable |
| `toasts: ToastController` | `show(text, actionLabel = "되돌리기", onAction)`; one at a time; 6 s (LIB-40) |
| `results: NavResults` | `send(requestKey, value)` / `consume(requestKey)` / `results(requestKey)` between screens (e.g. `PickOnMap`) |
| `sheets: SheetRegistry` | `register<S : SheetKey> { sheet -> … }`; content of shared sheets |
| `appScope: CoroutineScope` | main-thread scope that outlives screens |
| `lens(store): LensController` | the project's lens: `state`/`point` flows, `current()`, `set`, `backToLatest`, `setLensToScene(sceneKey)`, `enterScript`/`leaveScript` (the shell calls the last two) |

### 3. Routes (core:navigation)

`Route : NavKey` is sealed and `@Serializable`. Every route belongs to one area interface: `LibraryRoute · StoryRoute · ScriptRoute · CastRoute · WorldRoute · MapRoute · CanvasRoute · RecorderRoute · ShellRoute`. Project screens also implement `ProjectRoute` (`projectKey`). Markers: `LensScreen` (shows the lens chip; never the script, 스토리 구조 or 점검 센터), `ListPaneRoute` / `DetailPaneRoute` (CNV-33), `OverlayRoute` (full-window glass over the previous screen).

| Module | Routes |
| --- | --- |
| library | `Library` · `Onboarding(page)` · `Walkthrough(page)` · `ExampleTour(projectKey)` · `NewProject(step)` · `Settings` · `SettingsTextSize` · `SettingsLayout` · `SettingsMotion` · `SettingsMore` · `SettingsHiding` · `SettingsRating` · `RatingDefaultsApply(rating)` · `SettingsBackup` · `RecoveryPassword` · `HelpReplay` · `Licenses` · `Trash(filter)` · `RestoreBackup` · `Restoring` · `OpenFile(uri)` · `ProjectSettings(p)` · `StructureLevels(p)` |
| story | `ProjectHome(p)` · `StoryStructure(p, focusUnitKey)` · `CheckCenter(p, filter)` · `ProjectSearch(p, query)` · `LibrarySearch(query)` · `BulkPlaceTime(p)` · `SceneNotationSettings(p)` · `DayBandSettings(p)` |
| script | `Script(p, sceneKey, blockKey, caretAtEnd)` · `ScriptAssets(p)` · `ScriptFieldSettings(p)` · `TransitionTextSettings(p)` |
| cast | `CastList(p)` · `CharacterProfile(p, characterKey, tab)` · `RelationMap(p, focusKey)` · `FieldSettings(p, kind)` · `FieldDefEdit(p, fieldDefKey)` · `RelationKindSettings(p)` · `TagSettings(p)` · `ArchiveDocEdit(p, characterKey, docKey)` |
| world | `World(p, tab)` · `TermCard(p, termKey)` · `PlaceCard(p, placeKey, tab)` · `FactionCard(p, factionKey)` · `Timeline(p)` · `EventDetail(p, eventKey)` · `TimelineMap(p)` · `WorldCalendar(p)` · `TravelModes(p)` |
| map | `MapList(p)` · `MapView(p, mapKey, focusPinKey)` · `MapEdit` · `MapScale` · `MapReveal` · `NodeMap` · `MapFormatPicker(p)` · `MapGenerate(p, format)` · `HexPaint` · `PolygonRegions` · `DetailStamps` · `TextMap(p, mapKey?)` · `CharacterTrack(p, characterKey?, mapKey?)` · `PickOnMap(p, requestKey, sceneKey?)` |
| canvas | `CanvasBoard(p, canvasKey?)` · `PageList(p)` · `PageDoc(p, pageKey)` · `ImageCrop(p, ownerKind, ownerKey, slot, blobHash)` · `ImageSpecs` · `PresetImages` · `HoloExperiment` |
| recorder | `RecorderInbox(filter)` · `RecorderRecordDetail(recordKey)` · `Recording(surface, projectKey, targetKind, targetKey, caretBlockKey, startTyped)` · `RecorderSettings` |
| shell | `AppLock` · `HiddenProjectLock(p)`. These are overlay keys. They are never pushed. |

Route arguments are keys, strings, booleans, ints and plain enums only, never model objects, so they save through `SavedState`. If you need a new route, list it under Requests here. Do not declare a `Route` in your module: the sealed hierarchy lives in core:navigation.

### 4. How to open things

- **Open a project:** `deps.navigator.push(ProjectHome(projectKey))`. From the 서재 list use `deps.navigator.openDetail(ProjectHome(key))` (core:ui `ui.nav.openDetail`). It replaces the detail while one is already beside its list, so the right pane swaps on inner screens.
- **Any list → detail** (인물 목록 → 인물 기록, 세계 → 용어 카드, 연표 → 사건, 기록함 → 기록, 점검 센터 → `Script`): `navigator.openDetail(detailRoute)`. On inner screens in `목록 + 상세`, the shell draws the pairs in `ListDetailPairs` side by side (390 | rest). A screen can read `LocalPaneRole.current` (`SINGLE · LIST · DETAIL`), for example to hide its back button in the detail pane.
- **Script:** `navigator.push(Script(p, sceneKey = …))`, or `Script(p, blockKey = …, caretAtEnd = true)` for `이어 쓰기`. The shell runs the STO-16 lens hook around the script. Inside the script, call only `deps.lens(store).setLensToScene(sceneKey)` for `렌즈를 이 씬으로 맞추기`.
- **Back to 서재:** `navigator.resetTo(Library)`. **Finish onboarding:** `navigator.resetTo(Library)` after updating `OnboardingState`.
- **Inside a project route:** `currentProjectStore` (or `LocalProjectStore.current`) is already open. The window protection and the hidden-project lock are already applied.
- **View models:** subclass `ProjectViewModel(projectKey, deps.projects)` and create it with `entryViewModel { MyVm(route.projectKey, deps.projects, …) }`.
  - Each back-stack entry has its own `ViewModelStore`, cleared on pop.
  - `state: StateFlow<ProjectLoad>` is `Loading · Ready(doc, readOnly) · Failed`, and `doc: StateFlow<ProjectDoc?>`.
  - `suspend update(label, mergeKey) { doc -> doc' }` returns `EditResult.APPLIED · READ_ONLY`. `edit(…)` is the fire-and-forget form. `updateApp { }` changes `_app` state with no undo step and also works in read-only projects. Also `undo()` / `redo()`.
  - View models get no `SavedStateHandle`. Restorable state goes into route arguments and `rememberSaveable`.
- **Results:** push a picker route with your own `requestKey`, collect `deps.results.results(requestKey)`, then `consume(requestKey)`.
- **From outside the app** (widget, tile, notification): start `MainActivity` with the extra `MainActivity.EXTRA_ROUTE` = `RouteCodec.encode(route)`. Only classes in `io.github.lenticualrstone.civilight.navigation` are accepted. An `ACTION_VIEW` of a `.civilight` file arrives as `OpenFile(uri)`. Both are pushed after the app lock is passed.

### 5. How to show sheets

Sheets are not back-stack entries. They are saveable state of the screen that shows them, so an open sheet survives process death (CNV-35).

1. Declare the sheet key as a `@Serializable` class implementing `SheetKey`. Feature-local keys may live in your module. Sheets other areas open over their own screens are the `SharedSheet`s in core:navigation `Sheets.kt`, each naming its `owner`:
   - SHELL: `LensSheet`
   - STORY: `UnitMenuSheet`, `NewUnitSheet`, `SceneMoveSheet`, `SceneWhereSheet`
   - SCRIPT: `SceneSplitSheet`, `SceneMergeSheet`
   - CAST: `FieldHistorySheet`, `FieldValueEditSheet`, `AddFieldSheet`, `RelationEditSheet`, `DeleteImpactSheet`, `MergeItemsSheet`
   - WORLD: `LinkCandidatesSheet`, `EventCreateSheet`
   - CANVAS: `LineLabelSheet`, `ImagePickSheet`
   - LIBRARY: `ExportSheet`, `RatingChangeSheet`
   - RECORDER: `TextMemoSheet`
2. In the screen: `val sheets = rememberSheetHost()`, then pass it to `CivilightScreen(…, sheets = sheets, localSheets = { key -> when (key) { is MySheet -> { { MySheetContent(key, this) } } else -> null } })`, or wrap content in `SheetHostLayout(sheets, localSheets = …) { … }`.
3. Open with `sheets.open(key)`. Inside the sheet, `SheetScope` gives `close()`, `open(another)` (stacked: closing returns to the one below) and `inner`.
4. The owner of a shared sheet registers its content in its entries function: `deps.sheets.register<SceneWhereSheet> { sheet -> SceneWhereContent(sheet, this) }`. The content's receiver is a `SheetScope`, which is also the sheet's `ColumnScope`, so `ColumnScope` content such as pickers, `SheetScrollBody` and `SheetActions` can be called directly. Start it with the DS `SheetHeader(title, onClose = ::close)`. Use `SheetScrollBody` for long bodies and `SheetActions` for buttons. An unregistered shared sheet shows a placeholder sheet.
5. Cover screens get the DS bottom sheet. Inner screens get the centred sheet. The script's side panel and map/canvas popovers are drawn by the owning feature.
6. The lens chip opens `LensSheet(projectKey)`. The shell draws it (rows, `더 자세히`, `씬 단위로 고르기`, `최신으로 돌아가기` · `이 시점으로 보기`).

### 6. Shared UI in core:ui

- **`CivilightScreen`** (`ui.scaffold`): the screen frame with these parts:
  - glass top bar: `title`, `smallLine`, `back: IconAction`, `lens: LensChipLabel` + `onLens`, `actions`
  - `commandBar` (DS `CommandBar`) in the glass bottom bar, plus `bottomExtraRow`
  - `banner` under the top bar
  - the `되돌리기` toast above the bottom bar
  - `sheets` / `localSheets`
  - `content(PaddingValues)` scrolls under both bars
- **Lens** (`ui.lens`):
  - `ProjectLensChip(store, onClick)`
  - `lensChipLabel(doc)` (`최신 · 1-7`, `최신 · 전체`, `시점 · 1-5`, `시점 · 1-7 씬 2`)
  - `LensRules`, `LensRows`, `LensSheetContent`, `LensController`, `ScriptLensEffect`, `LatestAction`
  - The script never shows a lens chip.
- **Pickers** (`ui.picker`):
  - `CharacterPickerSheet`: `화자`, `초성 검색`, tag chips, `이 씬 등장 · 최근 · 전체`, deleted speaker row, hidden row
  - `TermPickerSheet` (aliases), `PlacePickerSheet` (places + regions → `PlaceRef`), `FactionPickerSheet`, `UnitPickerSheet`, `LinkPickerSheet` (`@`: 인물 · 용어 · 장소)
  - the generic `ItemPickerSheet` and the pure `PickerModels` (same data, testable)
  - `rememberHiddenGate(projectKey)` supplies `HiddenGate(open, unlock)`
  - Feature-specific data goes in as parameters: `sceneCast`, `recent`, `subtitleOf` (e.g. `표시 이름 ???` at a block).
- **Labels:** `UnitLabels(doc)` gives `code` (`1장`, `1-7`, `씬 3`), `reference` (`1-7 씬 3`), `referenceWithTitle`, `header` (`Ⅰ 1장 · 등불지기`), `episodeOf`, `ordinal`.
- **Search:** `KoreanSearch.matches/find/initials` (초성).
- **Window** (`ui.window`): `LocalWindowLayout`, `WindowLayout`, `CenterColumn { }` (`가운데 한 칸` 560 column), `ListDetailPanes`.
- **Security** (`ui.security`):
  - `ProtectWindow(WindowProtection.forProject(protections))` for any content of a protected project outside its own routes (hidden-group rows, search results, 기록함 rows, recents cards, the export sheet)
  - `AppLockScreen`, `HiddenProjectLockScreen`, `LockRules`, `UnlockController`, `AuthCopy.HIDDEN_PROJECTS` (`숨긴 프로젝트 보기`)
- **Theme:** `ThemeBridge` maps model `ThemeMode` / `Rating` / text step / animation preference to the design system's types. Use `ThemeBridge.rating(project.rating)` for `RatingBadge`.

---

## Done

- **Routes for every §5 screen**: `navigation/Route.kt` plus one file per area (`LibraryRoutes`, `StoryRoutes`, `ScriptRoutes`, `CastRoutes`, `WorldRoutes`, `MapRoutes`, `CanvasRoutes`, `RecorderRoutes`, `ShellRoutes`). Shared sheets are in `Sheets.kt`. `FeatureModule.kt` holds `Route.owner`, `projectKeyOrNull`, `hasLens`, `routeName`. Tests:
  - `RouteSerializationTest.the samples cover every route and every shared sheet`: the sealed descriptor is compared with `RouteSamples`, so a new route without a sample fails.
  - `every route survives the back-stack serializer used for process death` and `a whole back stack round-trips`: Navigation3's `NavKeySerializer` / `NavBackStackSerializer`, run through `Json`. On a device, Android restores through the SavedState encoder instead, which a JVM test cannot reach. That path is unverified (see Partial).
  - `every route has an owner and lens screens follow R11 §3`.
- **Navigator**: `navigation/Navigator.kt` (`Navigator`, `BackStackNavigator`, `popTo`, `popToType`, `resetTo`), `NavResults.kt`, `RouteCodec.kt`. The stack is never emptied, and pushing a route already in the stack returns to it. Tests: `NavigatorTest` (6) and `RouteSerializationTest.route codec …`, `nav results wait until consumed`.
- **FeatureDeps and the entry contract**:
  - core:ui: `ui/feature/FeatureDeps.kt` (`FeatureDeps`, `LocalFeatureDeps`, `entryViewModel`), `FeatureContract.kt`, `FeatureLoader.kt` (reflection, placeholder fallback, loud failure on a bad signature), `SheetRegistry.kt`, `ShellSheets.kt`.
  - App side: `shell/FeatureRegistry.kt` (calls `FeatureLoader` with the eight bindings) and `app/proguard-rules.pro` keep rules.
  - Tests, using the fake features in `src/test/.../fakefeature/`: `FeatureContractTest.a feature found by its JVM name registers entries and shared sheets, the rest falls back`, `a feature with the wrong signature fails loudly`, `a shared sheet cannot be registered twice and local sheet keys save with the host`, plus `ShellRulesTest.every feature module has one fixed entry binding`.
  - The first test found a crash, now fixed: casting a composable sheet lambda to a wider type fails Kotlin's function-arity check at run time. `SheetRegistry` now casts only the key.
- **Per-entry view models**: `ui/nav/EntryViewModels.kt` (`rememberEntryViewModelDecorator`). Each entry gets its own `ViewModelStore`, kept across configuration changes and cleared on pop.
- **CNV-32 window classes**: `ui/window/WindowLayout.kt` (window size, not screen, from `LocalWindowInfo`; Compact < 600, Medium 600–840, Expanded ≥ 840), `AdaptiveFrames.kt`. Test: `ShellRulesTest.window classes - CNV-32`.
- **CNV-33 two panes and the centred column**: `ui/nav/SceneStrategies.kt` (`ListDetailSceneStrategy`, `LocalPaneRole`), `RouteMetadata.kt` (`ListDetailPairs`), `ListDetailNavigation.kt` (`openDetail`), and `CenterColumn`. Tests: `ShellRulesTest.list and detail pairs - CNV-33`, `choosing another row swaps the detail instead of stacking it`.
- **Glass overlay routes**: `OverlaySceneStrategy` (search, recording over the blurred previous screen, fade transitions).
- **STO-13 lens chip and sheet; STO-16 lens and script**:
  - `ui/lens/LensRules.kt` (`chipLabel`, `enterScript`, `leaveScript`, `setLensToScene`, `LensRows`), `LensController.kt` (writes through `updateApp`), `LensSheet.kt`, `ScriptLensEffect.kt`.
  - The shell registers `LensSheet` and wraps `Script` in the lens hook.
  - Tests in `LabelsAndLensTest`: `lens chip reads 최신 · 1-7 at the writing line and 시점 at a point`, `the script has no lens - STO-16`, `lens sheet rows start with 최신 and show the first unwritten episode`, `a viewed scene point gets its own row and neighbours surround an episode`.
- **STO-03 unit labels** (for the shell's pickers and lens): `ui/labels/UnitLabels.kt`. Tests: `unit codes follow STO-03`, `trashed units have no code and do not count`, `roman numerals`.
- **Pickers (SCR-19, SCR-55, CST-37, WLD-52 list part, R19 §6 I-06 / I-17, R20 deleted speaker)**: `ui/picker/PickerModels.kt`, `ItemPickerSheet.kt`, `Pickers.kt`, `ui/search/KoreanSearch.kt`. Tests: `PickerModelsTest` (9, incl. I-02 masked hidden scene and I-06 hidden count), `KoreanSearchTest` (3).
- **ProjectViewModel and sessions**: `ui/project/ProjectViewModel.kt`, `ProjectSessions.kt`, `RefCountedSessions.kt`, `ProjectEntryFrame.kt` (`LocalProjectStore`, `currentProjectStore`). Tests:
  - `ProjectViewModelTest.the example opens read-only, app state still changes, and the store closes after the last screen`
  - `a project that is gone reports a failure instead of loading forever`
  - `SessionsAndToastsTest.one open per key, closed after the last release and a delay`, `a failed open is retried by the next acquire`
- **LIB-26 / LIB-29 capture blocking, recents and task label**:
  - `ui/security/CaptureGuard.kt`: reference-counted `CaptureGuard`, applied synchronously; `ProtectWindow`.
  - `WindowProtection.forProject`: FLAG_SECURE for capture block. Hidden projects mask the recents preview, via `setRecentsScreenshotEnabled` on API 33+ and FLAG_SECURE below that.
  - `ActivityWindowProtector`: task label `Civilight` or `R-18 프로젝트`.
  - `ProjectEntryFrame` protects every project route before its content is first drawn.
  - Tests: `ShellRulesTest.window protection per project - LIB-26, LIB-29, S-05 to S-07`, `capture guard is reference counted …`.
- **LIB-32 … LIB-35, LIB-37 locks**:
  - Lock logic: `ui/security/LockRules.kt`, `UnlockController.kt` (memory only; process start = locked), `LockScreens.kt` (`AppLock`, `RHLockMark`), `BiometricGate.kt`.
  - App side:
    - `shell/AndroidBiometricGate.kt`: STRONG|CREDENTIAL on API 30+, WEAK|CREDENTIAL on API 29, no negative button, credential-only for `기기 PIN 사용`.
    - `CivilightRoot`: app lock overlay. The blurred last screen shows only for normal projects; mark-only modes compose nothing underneath.
    - `MainActivity` sends lifecycle events; `CivilightApp` registers a screen-off receiver; `CivilightRoot` runs a 30 s tick for `연 지 5분 뒤`.
    - `ProjectEntryFrame`: hidden-project lock in place of content; cancel → 서재 with the group locked.
  - Tests: `LockRulesTest` L-01 … L-06, `RL-01 to RL-07 re-lock table`, `the controller applies the policy to lifecycle events`.
- **LIB-40 toast host**: `ui/scaffold/ToastController.kt`, drawn by `CivilightScreen`. Test: `SessionsAndToastsTest.toasts replace each other and leave after their time`.
- **Common scaffold, sheets and placeholders**: `ui/scaffold/CivilightScreen.kt`, `SheetHost.kt` (saveable sheet stack via `RouteCodec`, registry lookup, placeholder sheet), `PlaceholderScreen.kt`.
- **Theme from settings (DS requests, CNV-46 effects)**:
  - `ui/theme/ThemeBridge.kt`: theme mode; text step, where model 0 is DS step 2 (18 sp); reduced motion from `AnimationPref` plus the system animator scale; glass tier.
  - `CivilightRoot` passes all of these to `CivilightTheme` and provides `LocalBackHandler` (activity-compose `BackHandler`).
  - `MainActivity` calls `enableEdgeToEdge()`.
  - Test: `ShellRulesTest.theme bridge - text step and motion`.
- **App shell**:
  - `CivilightApp`: Application with a `ServiceLocator` holding stores, settings state, sessions, unlock, toasts, results and the startup routes.
  - `MainActivity`: a `FragmentActivity` that uses `CivilightRoot`.
  - `CivilightRoot`: `NavDisplay` with `rememberNavBackStack` (restored after process death), saveable-state and view-model decorators, overlay and list-detail strategies, push/pop transitions (reduced motion → 120 ms fade), pending outside routes.
  - Stores are flushed in `onPause`. Maintenance (`runMaintenance`) runs at start.
- **LIB-08 / CNV-38 splash and start routing**:
  - `res/values/themes.xml` (`Theme.Civilight.Starting`: `bg`, mark only, no plate), `res/drawable/splash_mark.xml`.
  - The splash stays until the start routes are known.
  - Exit: the mark rises, shrinks to .42 and fades in 450 ms on `cubic-bezier(.05,.7,.1,1)`; with reduced motion, a 120 ms fade.
  - `shell/Startup.kt`: first run → onboarding at `resumePage`; else 서재.
- **LIB-17 seed**: `Startup.seedExampleIfNeeded`.
  - On the first run only, `ExampleProject.build()` is added with `isExample = true, readOnly = true`, then `exampleState = INSTALLED`.
  - Never after `firstRunCompleted`. Never again after `DELETED`. `ProjectKeyTakenException` counts as installed.
- **Launcher icon**: `res/mipmap-anydpi-v26/ic_launcher.xml` (DS drawables and colour).

## Partial

- **CNV-34 fold transitions**: the activity handles size and fold changes itself (`configChanges`), so screens keep their state and scroll. The 360 / 240 ms pane-entry motion and "last-touched pane stays" are not built. Panes switch at once when the width class changes.
- **CNV-35 state restore**: the back stack, open sheets (`rememberSheetHost`) and entry `rememberSaveable` state are saved for process death. Restore through Android's SavedState encoder has not been run on a device; the JVM tests cover the same serializers through `Json` only. Selection, scroll, caret, map and canvas viewports are each feature's to save (route arguments, `rememberSaveable`, `_app/view.json`).
- **CNV-36 postures**: `FoldPosture` is always `UNKNOWN`. Neither core:ui nor app depends on `androidx.window` (see Requests). Hinge avoidance is not possible yet.
- **CNV-38 splash**: the system phase and the exit motion are done. The 250 ms "three bars draw left → right" phase is not, because the splash icon is the static mark drawable.
- **LIB-29 on API 29–32**: hidden-project content uses FLAG_SECURE, because recents-only masking needs API 33. On those devices screenshots of hidden projects are blocked even with capture block off (library spec §6 Q6 `ED`).
- **LIB-34 blurred lock below API 31**: no blur exists there, so the app lock is always mark-only.
- **LIB-52 intent filter**: an `application/octet-stream` VIEW filter with `.civilight` path patterns. File apps that send another MIME type or no file name will not offer Civilight. This needs checking on devices.
- **Overlay scene on inner screens**: an `OverlayRoute` draws only the single entry below it, blurred, not the two panes.
- **WLD-52**: the list half of `장소 고르기` is in `PlacePickerSheet`. `지도에서 고르기 ›` is feature:map's `PickOnMap`.

## Defaults used

- **Feature wiring by reflection** (`ED`): the app cannot reference feature code at compile time while the feature modules are empty. Once all eight exist, `FeatureRegistry` can switch to direct calls without changing the contract.
- **Sheets are screen state, not routes**: the DS sheet wraps the screen it recedes. Shared sheets cross modules through `SheetRegistry`. Keys are saved with `RouteCodec` (Navigation3's `{type, value}` JSON).
- **Pushing a route already in the stack returns to it** (no duplicates). Navigation3 needs unique content keys.
- **List | detail pairs** are fixed to the CNV-33 `Two*` boards, plus `점검 센터 | Script` (`TwoCheck`). A list module alone on an inner screen shows an empty right pane (Q-LY-02). The list pane is 390 dp, or half the window on narrow Medium windows (Q-LY-01).
- **Project stores**: opened once per project and closed 15 s after the last screen or view model of that project leaves, keeping undo while you move around a project.
- **Window protection**: a project route whose document is not open yet counts as capture-blocked for the lock decision. The app lock prompt title is `Civilight` (LIB-35 gives none).
- **App lock with no secure lock screen** (library spec §6 Q19 open): the prompt returns `UNAVAILABLE` and the app lock lets the user in rather than locking them out for good. feature:library should refuse to turn `앱 잠금` on when `biometric.availability()` is `NONE`. The hidden-project lock stays locked in that case (cancel → 서재); hidden projects cannot be opened on such a device until Q19 is decided.
- **Prompts in flight**: while `AndroidBiometricGate` shows a prompt, `onStop` is not treated as leaving the app. The device-credential screen can be a separate activity, and that must not raise the app lock or re-lock what the prompt is opening.
- **Item unlock prompt**: LIB-36 says "same prompt as LIB-32", so `지문으로 보기` on hidden rows uses the title `숨긴 프로젝트 보기`.
- **Re-lock**: checked on background, screen off, return to the foreground and every 30 s. The app lock comes back after every background trip and every screen-off when it is on.
- **Toasts**: 6 s; a new one replaces the old.
- **Transitions**: push = slide in 1/8 width + fade over 320 ms emphasized decelerate; exit = fade 160 ms. Reduced motion = 120 ms crossfade. Overlay routes fade 220 ms.
- **Text size**: `AppSettings.textSizeStep` 0 (`보통`) = DS `TextScale.forStep(2)` (18 sp). Steps −2 … +2 map to 16 / 17 / 18 / 20 / 22.
- **Onboarding resume**: `OnboardingState.resumePage` values are an `OnboardingPage` name, `WALKTHROUGH_1` … `WALKTHROUGH_3`, `EXAMPLE` or `EXAMPLE_SCRIPT`. Anything else starts at the welcome page.
- **No system backup**: `allowBackup="false"` plus data-extraction rules excluding everything. Android's own backup would copy hidden projects unencrypted. The app's Drive backup (LIB-57) is the only backup.
- **Back icon label** `뒤로` (boards draw only `‹`). Placeholder screens and sheets show developer text (route name, module, arguments). They are not product UI and disappear once features register.
- **Lens rows** (STO-13 `ED`): 최신, then episodes holding a live change record, the viewed point, and the first episode beyond the 집필 선, in manuscript order. A viewed scene point gets its own row under its episode. A lens point naming a trashed or unknown unit reads as 최신.
- **Unit labels**: hidden scenes count and trashed ones do not. 2-level projects use `1막` for the top level and `장면 3` for scenes. A locked hidden scene in the unit picker reads `1-7 씬 4 · 숨긴 씬`.
- **Picker order**: `이 씬 등장` keeps the caller's order, `최근` keeps at most 5 not already listed, `전체` is by name. Sub line = tags joined with ` · `, else the one-liner. Link-picker sections are titled `인물 · 용어 · 장소`. The hidden row names the kind when only one kind is hidden, else `숨긴 항목 N`.

## Spec conflicts

- **Lens sheet sub line**: STO-13 keeps the board copy `앱 전체 적용 · 이 시점까지의 필드 · 관계 · 용어 공개`, while R11 §3 excludes the script (STO §6 Q10). The board copy is shown. The script has no chip, so the sheet is never opened there.
- **Text-size step origin**: the model's `textSizeStep` counts from `보통` = 0, while DS `TextScale.forStep` counts from the smallest step (default index 2). Bridged in `ThemeBridge`; no change requested.
- **Lock logic overlaps domain:library**: library spec §4.3 (`lockScreenMode`, `relockDue`) is implemented here in `LockRules` because core:ui and app may not depend on domain modules. If domain:library implements the same table, both must stay equal. The tests use the same cases.

## Requests

- **core/ui/build.gradle.kts and app/build.gradle.kts: add `libs.androidx.window`** (already in the catalog). It enables fold posture (`FoldingFeature`, CNV-36) and hinge-safe placement (CNV-32). `WindowLayout.posture` is ready for it.
- **core/ui/build.gradle.kts, optional: `libs.compose.material3.adaptive`** (catalog 1.3.0). Only needed if `calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth` is wanted (CNV-32). Classification currently uses the window width directly.
- **app/build.gradle.kts: `testImplementation(libs.junit)`**, so `Startup.startRoutes` and the reflection lookup in `FeatureRegistry` can get JVM tests.
- **Copy needed (TODO_COPY)**: `TODO_COPY_SHELL_PROJECT_OPEN_FAILED`, the title of the error shown when a project on the back stack can no longer be opened (deleted elsewhere, unreadable, newer store version). Its button reuses `서재로`.
- **Feature agents**: new routes or shared sheets go in core:navigation. Ask here, do not declare them in your module.
- **DS-62 repo-wide copy lint** (designsystem request): left for CI. A core:ui test that scans every module's `strings.xml` would tie this module's build to other agents' work in progress.
- **FEATURES.md**: update the rows listed under Done and Partial (CNV-32 … CNV-36, CNV-38, CNV-46 effects, LIB-08, LIB-17 seed, LIB-26, LIB-29, LIB-32 … LIB-35, LIB-37, LIB-40, STO-13, STO-16, SCR-19 / SCR-55 picker parts). This module may not edit that file.
