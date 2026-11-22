# Motion audit (smooth transitions)

Date 2026-11-23. Read-only audit by nine module auditors, then a verification pass that re-read a sample of the "static" claims. No build or device run. Paths are module-relative: `DS/` = `core/designsystem/.../designsystem/`, `UI/` = `core/ui/.../ui/`, `APP/` = `app/.../civilight/`, a feature path starts at its `feature/<name>/.../feature/<name>/` package.

Bottom line: smooth motion is **not** in place. Screen push/pop, sheet open/close, toast, lens chip, tab underline, switch thumb, progress bar, swipe row, block add, script side panel, canvas list/card and holo sweep animate (some with literals or a wrong reduce path). Almost every other state change is a one-frame jump. There are no `animateColorAsState` calls anywhere in the repo. Only script, world, canvas and two spots in library (`WalkDemos`) and map (`GenerateScreen`, default `Crossfade`) use `animateItem`, `AnimatedVisibility`, `AnimatedContent` or `Crossfade`. The `unhide*` tokens are unused.

## 1. Summary

Verdicts: **ok** = animated with `CivilightMotion` tokens and correct reduce-motion handling. **hard-coded** = animated, but with literal durations or curves outside the tokens. **partial** = some of the change animates, or reduce-motion handling is wrong. **static** = a jump cut.

Each shared component is counted **once**, under core. Feature rows that only inherit a shared component are folded into core and listed in the "inherits" column. Feature counts cover changes the module itself owns.

| Module | ok | hard-coded | partial | static | total owned | inherits (folded) |
| --- | --: | --: | --: | --: | --: | --: |
| core:designsystem + core:ui + app | 5 | 5 | 16 | 25 | 51 | – |
| feature:library | 1 | 3 | 9 | 23 | 36 | 26 |
| feature:story | 0 | 0 | 1 | 25 | 26 | 16 |
| feature:script | 2 | 0 | 8 | 13 | 23 | 8 |
| feature:cast | 0 | 0 | 6 | 13 | 19 | 7 |
| feature:world | 0 | 1 | 8 | 21 | 30 | 9 |
| feature:map | 0 | 2 | 1 | 20 | 23 | 7 |
| feature:canvas | 3 | 1 | 4 | 13 | 21 | 10 |
| feature:recorder | 0 | 2 | 2 | 19 | 23 | 8 |
| **Total** | **11** | **14** | **55** | **172** | **252** | 91 |

The auditors reported 346 rows. 91 shared duplicates were folded into core and 3 meta rows were dropped, leaving 252 distinct changes. About 68 % are static. Fixing the shared items in §3 alone clears the 91 folded rows plus the 46 non-ok core rows. The rest needs module work, mostly the same few patterns (§4).

## 2. Verification and corrections

Re-read to confirm (claim holds):

- **core.** `ListRows.kt:40-45` `selectedRowBackground` draws or skips. `Segmented.kt:59-77` uses a plain `background(style.fill)`. `Popover.kt:77-85` is a bare `Popup`. `Dialogs.kt:57-80` scrim is a plain `background(shade)`. `SheetHost.kt:92-118` swaps `key(id)` while `sheetVisible` stays true. `ProjectEntryFrame.kt:81-118` switches with `when` / `return`. `CivilightScreen.kt:106-108` uses `if (banner != null)`. `States.kt:241-242` passes `reduced = true` hard-coded. `Pressable.kt:69-73` has no easing and gives no reduced substitute. `Controls.kt:59-63` animates the thumb only. `Tabs.kt:70-72` snaps when reduced. `Sheet.kt:110-125` has no `transformOrigin`. `CivilightRoot.kt:144-158` puts lock blur on, then off, in one frame, and `:229-249` holds literal 320/160/120. `RouteMetadata.kt:38,43-44` hard-codes 220 ms with no reduced path. `GlassOverlayScene` (`SceneStrategies.kt:79-91`) applies blur 14 dp and alpha .6 from the first frame. `CivilightMotion.kt` defines `unhide*`, and a repo grep finds no caller.
- **library.** `shelf/LibraryScreen.kt:259` has `items(...)` with no `animateItem`. `:371-372,401` use `UNBLUR_MS = 320`, a literal 7 dp and .55, and the default easing. `trash/TrashScreen.kt:187` uses `Modifier.rotate(if (expanded) 90f else 0f)`, and `:193` uses `if (expanded)`. `shelf/CardShelf.kt:113` uses a default `rememberSnapFlingBehavior(state)`. `NewProjectSteps.kt:87` uses `r18Block(...)?.let`.
- **story.** `StructureScreen.kt:140` has keyed items with no `animateItem`. `CheckCenterScreen.kt:247` uses `if (open) members.forEach` inside one item. `BulkScreen.kt:88` swaps with an early return. `BulkLists.kt:89` uses `fillMaxWidth(fraction)`. `where/SceneWhereContent.kt:85-93` swaps the body with a `return`. `search/SearchScreen.kt:224-227` uses `boxed()` `SpanStyle`.
- **script.** `ui/Manuscript.kt:136` has `fadeInSpec = null`. `motion/ScriptMotion.kt:60-65` returns `editing -> null`. `select/SelectList.kt:104` has no `animateItem`. `panel/ScriptPanel.kt:82,119` and `TabletopPanel.kt:49` use `tween(tabMs)` with the default easing. `ScriptScreen.kt:699` uses `.width(layout.areaWidth.dp)` with no animation. `motion/SwipeRow.kt:71,112` uses the same offset `Animatable` when reduced.
- **cast.** `list/CastListScreen.kt:242-253` swaps list and card with `if`. `CastCards.kt:58` `LazyRow` has no fling behaviour. `ProfileScreen.kt:227` re-keys `tab-${ui.tab}`. `RecordFrame.kt:77-98` uses `if (collapsed)`. `FieldSettingsScreen.kt:180-185` resets `offset = 0f` on drop.
- **world.** `list/WorldScreen.kt:229,235` uses `placementSpec = null`. `list/ListCardMotion.kt:53-56` and `WorldScreen.kt:178-183` show `GrowIn`. `WorldCardView.kt:118` uses a default snap fling. `time/TimelineScreen.kt:196` has no `animateItem`.
- **map.** `view/MapViewScreen.kt:118-124` assigns the viewport instantly, and `:194-204` swaps panels with a plain `when`. `gen/GenerateScreen.kt:82,112` uses `Crossfade` with no spec. `MapListScreen.kt:80-87` has no `animateItem` and no snap.
- **canvas.** `board/CanvasBoardScreen.kt:271-272` changes zoom in one step. `UI/graph/GraphSurface.kt:100-102` draws nodes in an unkeyed loop with no motion. `page/PageListScreen.kt:152` has no `animateItem`. `image/crop/CropStage.kt:78` uses `snap()` when reduced. `image/SlotImage.kt:44-49` applies 1.1 scale only when `shift != 0`.
- **recorder.** `RecordingRoute.kt:139-140` uses a plain `when (state.stage)`. `capture/CapturePanes.kt:114` uses `animateScrollTo` with the default spec, and `:134-137` draws the caret as a static `Box`. `inbox/InboxScreen.kt:269` has no `animateItem`, and `:135-148` uses `tween` with no easing. `inbox/RevealMotion.kt:28-31` duplicates the token values.

Corrections made:

| Row | Change | Decided by |
| --- | --- | --- |
| story, two-pane detail swap | partial → **static** | `UI/nav/SceneStrategies.kt:47` sets the scene key to `list.contentKey`, and `UI/window/AdaptiveFrames.kt:57` calls `detail?.invoke()` in a plain `Box`, so NavDisplay runs no transition |
| story, row press feedback | ok → **partial** | Rows pass `scaleOnPress = false`, and `Pressable.kt:78` sets `indication = null`, so they get no feedback |
| map, press | ok → **partial** (folded into core) | `Pressable.kt:72` `tween(pressMs)` uses the default easing, and there is no reduced substitute |
| map, screen push / child map | ok → **partial** (folded into core) | `CivilightRoot.kt:229-249` holds literals, and the WLD-68 child-map zoom is not implemented |
| core: tab underline, swipe reveal, keyboard; script: swipe reveal, swipe auto-close | ok → **partial** | Wrong reduce path: `Tabs.kt:70` snaps, `SwipeRow.kt:71,112` still slides 192 dp in 120 ms, IME is system-driven and jumps on API 29 |
| library, holo foil on cards | partial → **ok** | Static foil is correct here; the sweep belongs to canvas |
| cast, "no snap fling anywhere in the repo" | evidence wrong, row stands | `CardShelf.kt:113`, `WorldCardView.kt:118` and `SnapCardRow.kt:64-71` use snap fling. The fix is to reuse `SnapCardRow` |
| story, "LensChip is the only AnimatedContent" | evidence wrong | Toast, States, script, world and canvas also use animation APIs. Rows unaffected |
| dropped: story "module has no motion" and "unused tokens", recorder "글로 쓰기 visibility" | meta or not a runtime change | – |

**Verify on a device**, because these were inferred from source and not run:

- The `GrowIn` card grow never fires. `remember`/`LaunchedEffect(Unit)` see `switched = false` at first composition (`ListCardMotion.kt:53-56`).
- The screen under a glass overlay blanks during the 220 ms fade (Nav3 `SceneSetupNavEntryDecorator`).
- The sheet glass backdrop lags during the rise.
- The fold scene switch dips to `bg`.
- The IME resize jumps on API 29.

## 3. Shared fixes, done first by one core agent (wave 0)

DECISIONS §2 freezes `core:designsystem`, `core:ui` and `app` while feature lanes run. One core agent does everything in this section before the feature wave. Feature agents only call these helpers. If a feature needs something new in a shared module, it adds it under "## Requests" in its status file.

### 3.1 Reduce-motion rule (one rule, encoded in the helpers)

`CivilightMotion.spec(token, true)` is still a 120 ms tween. Passed to a placement, size or offset animation, it would produce a 120 ms move. The helpers therefore apply this rule, and feature code never branches on `reduced` itself:

- **Appear, disappear, swap.** Opacity only: `fadeIn`/`fadeOut(spec(token, true))`, 120 ms linear. No slide, scale, expand or blur.
- **Placement, size, offset, viewport, width.** No animation: `null` placement, `snap()` size, `snapTo` position.
- **Colour, fill and alpha changes.** Keep them at 120 ms linear. These are not movement.
- **Decorative loops** stop: the glow goes STILL, the caret stays solid and parallax is 0. Direct manipulation (drag, pinch, swipe follow) always tracks the finger.

### 3.2 Helper set, `DS/theme/Motion.kt` (new)

| Helper | Shape | Reduced |
| --- | --- | --- |
| `LocalMotion` / `CivilightTheme.motion` | `class Motion(val reduced: Boolean)`, provided by `CivilightTheme` next to `LocalReducedMotion` | – |
| `motion.spec<T>(token, delayMs = 0)` | Wraps `CivilightMotion.spec`; also adds a delay overload to `CivilightMotion.spec` | 120 ms linear |
| `motion.move<T>(token): FiniteAnimationSpec<T>` | For size, offset, width, rect, rotation and viewport (`animate*AsState`, `animateContentSize`, `Animatable.animateTo`, none of which take null) | `snap()` |
| `motion.enter(token = enter, rise: Dp? = null, expand = false)` / `motion.exit(token = exit, ...)` | `EnterTransition` / `ExitTransition`: fade plus optional `slideInVertically` or `expandVertically` | Fade only |
| `motion.swap(enter = standard, exit = exit)` | `ContentTransform` for `AnimatedContent`, with `SizeTransform(spec(standard))` | Fade, `SizeTransform` snap |
| `motion.valueSwap(distance = 8.dp)` | Lifted from `LensChip.kt:79-87`: the old value leaves up 8 dp, the new one enters from below, 200 ms (CNV-42) | 120 ms crossfade |
| `LazyItemScope.civilightItem(fadeIn = enter, placement = standard, fadeOut = exit): Modifier` | `animateItem(...)` with those tokens (folds pass `foldOpen`/`foldClose`) | Fades 120 ms, placement `null` (`animateItem` accepts null) |
| `animatedColor(target, token = standard)` / `animatedFraction(on)` | `animateColorAsState` / `animateFloatAsState(if (on) 1f else 0f)` | 120 ms |
| `ValueSwap(text, style, color)` | `AnimatedContent(text, motion.valueSwap())`; unchanged text does not animate | Crossfade |
| `ContentSwap(state) { }` | Loading / empty / filled / error swap | Crossfade |
| `FoldReveal(visible) { }` | `AnimatedVisibility(expandVertically(foldOpen) + fade, shrinkVertically(foldClose) + fade)`; keeps the last content during exit | Fade only |
| `rememberUnhide(revealed): State<Float>` + `Modifier.unhide(progress)` | Promoted from recorder `inbox/RevealMotion.kt` (BLUR on API 31+ / FADE on older APIs / QUICK_FADE): blur `unhideBlur * (1 - p)`, alpha `lerp(unhideStartAlpha, 1, p)`, `tween(unhideMs, enterCurve)`; plays in reverse to re-lock | Alpha only, 120 ms |
| `rememberLastNonNull(value)` | Keeps exiting content composed while it leaves (banner, toast, panel) | – |
| `MotionLint` (JVM, called from each feature test like `CopyLint`) | Flags `tween(<literal>)`, `Crossfade(` without a spec, `animateItem()` without arguments, a default `rememberSnapFlingBehavior(state)`, and `CubicBezierEasing(` outside DS | – |

Promote existing token-based code instead of writing new code. The core agent cannot edit feature modules, so wave 0 **copies** these into core and each feature agent deletes its local copy in wave 1:

- canvas `card/SnapCardRow.kt` + `card/CardViewMotion.kt` → `core:ui` `card/`, the shared snap row and list/card transform. **Prototype first**: `SnapCardRow` depends on canvas-local `SnapTicker`, `CardSnap` and `CardMetrics` (`SnapCardRow.kt:55,69,72,83`), which must move with it; `CardViewMotion` only needs DS.
- script `fold/FoldMotion.kt` panelEnter/panelExit → DS (DS-only imports, lifts cleanly)
- recorder `RevealMotion` → `unhide` (above)
- the `LensChip` transition → `valueSwap`

### 3.3 Shared component fixes, in order of how visible the jump is

| # | Where | Exact fix | Reduced |
| --- | --- | --- | --- |
| S1 | Shell push/pop/predictive, `APP/shell/CivilightRoot.kt:229-249` | Delete `Decelerate`/`ENTER_MS`/`EXIT_MS`/`REDUCED_MS`. Enter: `slideInHorizontally(spec(enter)){it/8} + fadeIn(spec(enter))`. Exit: `fadeOut(spec(exit))`, which sets the exit curve that is missing today. Pop: `slideOutHorizontally(spec(exit)){it/8} + fadeOut(spec(exit))` | Fades only, via `motion.enter`/`exit` |
| S2 | Overlay routes, `UI/nav/RouteMetadata.kt:38-48`, `SceneStrategies.kt:79-91` | Remove `OVERLAY_FADE_MS`. Use a fade-only transition built from `spec(enter)`/`spec(exit)`, and read `reduced` through a `() -> Boolean` passed into `RouteMetadata.of`, because the metadata lambda is not composable. Make `GlassOverlayScene` a Nav3 `OverlayScene` (`overlaidEntries = listOf(below)`) so the base stays composed, then drive blur 0→14 dp and alpha 1→.6 from an `Animatable` with `spec(enter/exit)`. **Prototype first**: check the `OverlayScene.onRemove` exit | Fade 120, no blur ramp |
| S3 | Two-pane detail, `UI/nav/SceneStrategies.kt:47-60`, `UI/window/AdaptiveFrames.kt:46-57` | Swap the detail slot by key with `AnimatedContent(detailKey, motion.swap())`; on first open, slide in 48 dp from the right on `foldOpen` (CNV-34). **Prototype first**: the outgoing `NavEntry` is no longer in `scene.entries`, so check Nav3 1.1.2 `SceneSetupNavEntryDecorator` to see whether its `Content()` still renders. Fallback: keep the previous detail as a frozen snapshot layer and fade it out. On a fold or unfold, give `ListDetailScene` metadata with a fade-only transition on `foldOpen`/`foldClose` instead of the generic push slide | Crossfade 120, no slide |
| S4 | Sheet stack / sheet-to-sheet, `UI/scaffold/SheetHost.kt:92-118`, `DS/component/Sheet.kt:165,197-213` | Add `Modifier.animateContentSize(motion.move(standard))` on the sheet surface Column; this fixes height jumps for every sheet. Crossfade the `shown` swap with `AnimatedContent(shown, motion.swap(), contentKey = RouteCodec::encode)`. **Prototype first**: `SheetBody` needs the `ColumnScope` receiver, and the `removeState` side effect assumes a single composed sheet | Fade, size snap |
| S5 | Sheet polish, `Sheet.kt:92-192` | `transformOrigin = TransformOrigin(.5f, .3f)` (CNV-41). Centred sheet: scale .96→1 from `progress`. Drag: scrim and back scale use `progress * (1 - drag/height)` | Alpha only (unchanged) |
| S6 | Selection and value colour in DS: `ListRows.kt:40` `selectedRowBackground`, `Segmented.kt` (both controls), `Chips.kt:56-67` FilterChip, `Controls.kt:94-141` Radio/Checkbox, `:263-309` ChoiceCard/TypeGridCell, `:57-87` switch track, `Tabs.kt:92,100` label colour, `TextFields.kt:78-118` outline and error line, `Progress.kt:49-151` StepBars/PagerMarks/StepList, `ImageCards.kt:155-167` ring, `MapTools.kt:92-271`, `IconButtons.kt:172-180` toggle, `NodeCard.kt:45-90`, `Buttons.kt:67` enabled alpha, `LensChip.kt:48-96` outline | Fill, outline, text colour and alpha go through `animatedColor`/`animatedFraction` on `standard`. Fill and bar draw with `alpha * a`. The segment pill slides with an `Animatable` like `Tabs.kt:65-75`. The radio dot scales 0→1 and the tick fades and scales in on `enter`. Error and hint lines use `FoldReveal`. PagerMarks width uses `animateDpAsState(motion.move(standard))`. LensChip passes `SizeTransform` with `spec(standard)` | Colours 120 ms; segment pill, radio dot, tick and underline do not move: they crossfade or snap. Also fix the reduced underline snap at `Tabs.kt:70` |
| S7 | Press, `DS/foundation/Pressable.kt:69-80`, `Buttons.kt:79-82` | Scale: `tween(pressMs, easing = standard.easing)`. For `scaleOnPress = false` and for reduced motion, add a pressed overlay of `ink @ .06` driven by `animatedFraction(pressed)` on `pressMs`. Animate the ghost layer alpha the same way | Overlay alpha only, no scale |
| S8 | Overlays and popups: `Popover.kt:77-85`, `Dialogs.kt:57-80`, `Tooltip.kt:46-129` (TourDim/TourCard), `SidePanel.kt:37-70`, `CivilightScreen.kt:106-108` banner, `LensEditBanner.kt:40-42` | Popover: `MutableTransitionState` + `AnimatedVisibility(motion.enter(rise = 6.dp)/exit)`, with dismiss delayed until the exit finishes. Dialog: one `progress` `Animatable` for scrim alpha and a 12 dp card rise; close via a `visible` flag, then call the callback; `window.setWindowAnimations(0)`. Tour: `animateRectAsState(motion.move(standard))` for the hole, `ContentSwap` for the card. Expose `SidePanelHost(visible)` with FoldMotion enter/exit. Banner: `rememberLastNonNull` + `AnimatedVisibility(motion.enter(rise = -8.dp))` | Fades only, hole and card snap |
| S9 | Locks: `CivilightRoot.kt:140-158` app lock, `UI/security/LockScreens.kt:71`, `UI/project/ProjectEntryFrame.kt:81-118` | App lock: show instantly (protected content must never linger in a fade); on unlock, fade the overlay out on `exit` and ramp the content blur 14→0 and alpha .6→1. Hidden project: lock→content uses `rememberUnhide` + lock-icon crossfade (CNV-43). `ProjectEntryFrame` Loading/Ready/Failed: `ContentSwap` | Unlock fades only |
| S10 | Toast, `Toast.kt:86-97`, `UI/scaffold/ToastController.kt` | Give the toast an id. On a new id, run `AnimatedContent(id, motion.swap())` (LIB-40: replace). Add `animateContentSize(motion.move(standard))` | Crossfade |
| S11 | States and pickers: `States.kt:238-243`, `States.kt:47-222`, `UI/picker/ItemPickerSheet.kt:68-104`, `Pickers.kt` | `DelayedProgress`: pass `CivilightTheme.reducedMotion` instead of `true`. ErrorBanner/LockRow enter via `FoldReveal`. Picker rows use `civilightItem()`, with stable keys that are kind+key without the title (`:80`) | – |
| S12 | Search hit and caret, `DS/text/DecoratedText.kt:104-159` | Full motion: each box grows from its left edge, `clipRect` width `* progress` on the `standard` easing (CNV-44); keep the 60 ms stagger. Caret: alpha blink, linear `caretPeriodMs` | Box: alpha 120. Caret: solid |
| S13 | Recorder glow, `DS/component/RecorderGlow.kt:79-120` | Create the infinite transitions only when `mode != STILL`. Intensity (`standard`) blends level scale and sweep amount between modes. Name 2400 ms (Open decision D3) | STILL, no clock |
| S14 | Graph surface, `UI/graph/GraphSurface.kt:89-193`, `GraphViewport.kt` | `rememberAnimatedViewport()`: an `Animatable` over (x, y, zoom); buttons, fit, focus and wheel call `animateTo(spec(standard))`, gestures call `snapTo`. Nodes: `key(node.key)`, enter with alpha plus 8 dp rise on `enter`; removed nodes kept for an `exit` fade. Lines: draw progress on `enter`, fade on `exit`. FrameNode/LabelPill/line colour use `animatedColor` | Viewport snaps; nodes and lines fade |
| S15 | Splash, `APP/MainActivity.kt:99-139`, `res/drawable/splash_mark.xml` | Curve and duration from tokens: `PathInterpolator` from `enterCurve`. Bars drawn by an AVD (CNV-38). The first NavDisplay rises 14 dp and fades in on `enter`, signalled from `animateSplashExit` | 120 ms fade (already) |
| S16 | Reduce-motion source, `UI/theme/ThemeBridge.kt:43-55`, `CivilightRoot.kt:89` | Observe `ANIMATOR_DURATION_SCALE` with a `ContentObserver` (`produceState`) so "기기 설정 따름" updates live | – |
| S17 | Theme or text-size switch, `DS/theme/CivilightTheme.kt:65-71` (low priority) | Crossfade the content keyed on the resolved theme, `spec(standard)`; or animated colour tokens | 120 ms |
| S18a | Bar content, `DS/component/CommandBar.kt:60-77`, `GlassBars.kt:71-80` | Hint ↔ typed text and mic ↔ 닫기 via `AnimatedContent(motion.swap())`; back-button appearance animates `padding(start)` with `motion.move(standard)` | Fade |
| S18b | `DS/component/StepSlider.kt:58-63` | While dragging, show `dragFraction` directly; tween only the release snap (`spec(standard)`) | Release snaps |
| S18c | Tab content, `Sheet.kt:246-260` `SheetTabsHeader`, `SidePanel.kt:58-66` | Add `TabContent(selectedIndex) = AnimatedContent(motion.swap())` and use it for sheet tabs and side panel bodies | Crossfade |
| S18d | Lens sheet, `UI/lens/LensSheet.kt:85-121` | Neighbour chips and 씬 단위로 고르기 list via `FoldReveal`; row highlight from S6 | Fade |
| S18e | Glass mode switch, `DS/glass/GlassMaterial.kt` | Crossfade glass ↔ opaque layer alpha on `spec(standard)` when `glassMode` changes | 120 ms |
| S18f | Record values, `ListRows.kt:163-196` `FieldTableRow` | Value text through `ValueSwap` (CNV-42); used by world and cast | Crossfade |
| S18g | Map marks, `DS/component/MapMarks.kt` `MapPin`/`MapVertex` | `MapPin` dim alpha .45 ↔ 1 and selected colour via `animatedFraction`/`animatedColor`; `MapVertex` first-composition pop .6→1.3→1 on `enter` | Alpha only, no pop |
| S18 | Keyboard, `GlassBars.kt:111,187`, `Sheet.kt:161`, `CivilightScreen.kt:86-112` | Nothing on API 30 and up, where the system animates the inset. API 29 jump is a platform limit; optionally use `animateDpAsState(imeBottom, motion.move(standard))` on API < 30 | – |

## 4. Per-module fixes (feature wave, one agent per module, in parallel after wave 0)

Repeating patterns, so rows below just name them:

- **P1 items.** Add `civilightItem()` to every keyed lazy item.
- **P2 reveal.** Wrap conditional blocks in `FoldReveal`, or `AnimatedVisibility(motion.enter/exit)` for non-folding blocks.
- **P3 swap.** Use `ContentSwap` / `AnimatedContent(motion.swap())` for `when`/`if` body swaps and for `ui == null → content`.
- **P4 value.** Use `ValueSwap` for lens- or edit-dependent text.
- **P5 unhide.** Use `rememberUnhide` + `Modifier.unhide`.

Reduced behaviour always follows §3.1, inside the helpers.

### feature:library

1. **List | card** (`shelf/LibraryScreen.kt:239-257`, `ProjectRow.kt:67-70`). Reuse the promoted `SnapCardRow` + `CardViewMotion` (`SharedTransitionLayout`, `sharedBounds` thumbnail on `enterLong`, text out on `exit`, text in after a 160 ms delay). `ProjectRow` needs a 48 dp thumbnail (spec gap: request it).
2. **Rows, cards, trash rows, empty, lock card** (`LibraryScreen.kt:259,262-282`, `CardShelf.kt:115,142-143`, `trash/TrashScreen.kt:110-117`): P1.
3. **Hidden group unlock / relock** (`LibraryScreen.kt:115-129,284-301,354-401`). Replace `UNBLUR_MS`, 7 dp and .55 with P5. Crossfade LockRow→OpenGroup with P3. Replace `delay(UNBLUR_MS)` with the animation's completion. Auto re-lock plays the reverse from a kept snapshot; process restore stays a cut. Card view: apply P5 to the `hiddenOpen` cards and P1 to `lockCard`.
4. **Wizard and onboarding pages** (`NewProjectScreen.kt:84-97`, `OnboardingActions.kt:74-80`, `OnboardingScreen.kt:63-68`). Keep the step inside one route with `AnimatedContent(step, motion.swap())` and a slide on `enter` (it/8). Keep StepBars/WalkDots outside it, so S6 animates them.
5. **Sheet inner steps** (`openfile/OpenFileScreen.kt:114-121`, `MultiExportSheet.kt:104-144`, `export/ExportSheetContent.kt:157-173,229-293`, `BundlePickerSheet.kt:60-73`, `RatingSheets.kt:147-167`): P3 for stages, P2 for options.
6. **Conditional form blocks** (`NewProjectSteps.kt:87-108,162-190,206-215,262-280`, `RatingSettingsScreen.kt:79-81`, `BackupSettingsScreen.kt:132-142`, `RestoreScreens.kt:159-190`, `RecoveryPasswordScreen.kt:120,175-185`, `ProjectSettingsScreen.kt:183-189`). P2. Changed text uses P4. Strength bars use S6 colours.
7. **Loading → content** (`LibraryScreen.kt` `ui.loaded`, `ExampleTourScreen.kt:102`, `ProjectSettingsScreen.kt:90`, `StructureLevelsScreen.kt:86`, `RestoringScreen.kt:66-77`): P3.
8. **Header actions** (`LibraryScreen.kt:190-195`): P3 for `+` ↔ `다시 잠그기`, P2 (horizontal) for export.
9. **Card shelf** (`CardShelf.kt:113`): use the promoted `SnapCardRow`, which snaps on `spec(enter)`. Cover image (`shelf/CoverArt.kt:65-70`): `Crossfade(bitmap, spec(standard))`.
10. **Trash expand** (`TrashScreen.kt:187-204`). Chevron uses `animateFloatAsState(motion.move(standard))` → `graphicsLayer { rotationZ }`; lines use P2. Reduced: chevron snaps, lines fade.
11. **Tour host** (`tour/TourHost.kt:83,107-152`): `AnimatedVisibility(running)`, card offset via `animateIntOffsetAsState(motion.move(standard))`, step body P3 (S8 animates the hole).
12. **Progress while creating or exporting** (`NewProjectScreen.kt:103`, `ExportSheetContent.kt:104-180`): `DelayedProgress(working)` (DS-47).
13. **Open file route** (`OpenFileScreen.kt:78-91`). Open the sheet after the push settles, and pop only after the sheet's exit has finished; or make the route an `OverlayRoute`.
14. **Walk demos** (`onboarding/WalkDemos.kt:86-228`). Swipe demo: easings from tokens, `swipeMs`/`swipeStaggerMs` keyframes, and no infinite transition when reduced. Lens demo: `motion.valueSwap()` instead of 400/320. Voice demo caret: blink as in S12.
15. **Settings value text** (`MoreSettingsScreen.kt:85-88` → `SettingsParts.kt:90`): P4.

### feature:story

1. **Two-pane detail**: covered by S3.
2. **Structure and check groups expand** (`StructureScreen.kt:140-194`, `CheckCenterScreen.kt:240-260`, `CheckRows.kt:102`). Structure: P1 with `foldOpen` fade-in and `foldClose` fade-out. Check: move the members out of the single item into their own keyed items (P1), or use P2. Swap Plus/Minus with `Crossfade` on `standard`.
3. **Lists**: Home rows, start block, check rows, filters, empty and lock, bulk rows (`HomeScreen.kt:144-155`, `CheckCenterScreen.kt:179-193,266-270`, `BulkLists.kt:66,151`, `search/SearchScreen.kt:138-174`, `LibrarySearchScreen.kt:153-170`). P1. Give search rows stable keys (no index, `:173`).
4. **Sheet body swaps** (`where/SceneWhereContent.kt:66-223`, `WhereFlashbackForm.kt:48-125`, `WhereTimeSection.kt:66-81`, `WhereSections.kt:69-201`, `CascadeContent.kt`, `newunit/NewUnitSheetContent.kt:97-158`, `move/MoveSheets.kt:60-159`, `BulkPlaceSheet.kt:119-148`, `BulkWhenSheet.kt:75-127`). P3 on the body kind (FORM/CASCADE/PICK_*) and P2 on conditional rows. Row alpha uses `animatedFraction`.
5. **Bulk ↔ suggest page** (`BulkScreen.kt:88-111`). Prefer making it a real NavKey push. Otherwise use `AnimatedContent(suggesting)` with the shell push/pop transforms from S1.
6. **Lens value swap in the body** (`HomeScreen.kt:170-228`; WhereTimeSection `ResultBox`; footer counts `BulkLists.kt:231,245`; check chip counts): P4.
7. **Loading → content** (`HomeScreen.kt:95-111`, `StructureScreen.kt:109-119`, `DayBandScreen.kt:85`, `NotationScreen.kt:62`, `BulkScreen.kt:144-150`, `CheckCenterScreen.kt:117-121`): P3.
8. **Home newReady and dismiss** (`HomeScreen.kt:74-147`): P1 on conditional items, P2 for the top-bar chip group.
9. **Bulk progress** (`BulkLists.kt:88-89`): use the DS `ProgressBar`. Fold footer (`:239`): `animateDpAsState(motion.move(foldOpen/foldClose))`.
10. **Search name and command hits** (`SearchScreen.kt:224-227`): render through `DecoratedText(revealBoxes = true)` so every hit shares CNV-44.
11. **Travel card** (`TravelCard.kt`, `TravelBar`): bar fill uses `animateFloatAsState(motion.move(standard))` (low priority).
12. **CommitField focus** (`CommitField.kt:36-59`): outline colour via `animatedColor`.

### feature:script

1. **Bar swap on starting and stopping typing** (`ScriptScreen.kt:601-728`, `ui/ScriptBars.kt:82-239`). Top and bottom bars use `AnimatedContent(barMode, motion.swap())`. The extra row, ClipboardStrip, command suggestions and header chips use P2. Content padding uses `animateDpAsState(motion.move(standard))` only if the caret-line compensation (`ScriptScreen.kt:506-523`) stays pixel-exact; otherwise keep the cut and say so.
2. **Row insert other than add** (`ui/Manuscript.kt:128-139`, `motion/ScriptMotion.kt:60-65`). Diff key sets each composition. Inserted rows get `fadeInSpec = spec(enter)` and ENTER placement. Removed rows close the gap on EXIT placement even while editing, so long as the row key set changed; a pure edit stays `null`. Make `entered` a set (`ScriptViewModel.kt:214-215,698`) so rapid adds do not cut each other.
3. **Manuscript width with the side panel, and the tabletop split** (`ScriptScreen.kt:698-699,918-931`, `fold/FoldEffects.kt:48-89`). Width uses `animateFloatAsState(motion.move(foldOpen|foldClose))`. Or, cheaper: clip and shift the width and re-wrap once at the end. Keep the anchor with a per-frame `scrollBy`. Split uses P2.
4. **Select mode list** (`select/SelectList.kt:74-128`): P1. The dropped row's offset uses `Animatable.animateTo(0f, motion.move(standard))`.
5. **Selection highlight padding** (`ui/BlockViews.kt:164-173`). Make the 12 dp padding constant, or `animateDpAsState(motion.move(standard))`. Add a selection case to `ScriptMotion.placement` using `standard`. The fill comes from S6.
6. **Sheet steps** (`scenes/SceneSheets.kt:88-135`, `ui/DirectionSheet.kt:74-146`, `change/ChangeSheet.kt:143-165`, `LinkSheets.kt:66`, `ui/SelectViews.kt:258-306`): P3 and P2.
7. **Swipe row under reduce** (`motion/SwipeRow.kt:71,112`). Reduced: snap the block, fade the buttons on their own 120 ms `Animatable`. Full motion: scale the settle time by the distance left, with a minimum of 120.
8. **Panel content and speaker label** (`panel/PanelViews.kt`, `ModsTabView.kt`, `SpeakerLineView.kt:83-105`): P4 / P2. Panel tab `Crossfade` (`ScriptPanel.kt:82,119`, `TabletopPanel.kt:49`): `animationSpec = spec(standard)`; delete `ScriptMotion.tabMs`.
9. **Hidden scene unlock** (`ui/BlockViews.kt:306-318`): P5 on the rows that appear because the gate opened.
10. **Programmatic scroll** (`ScriptScreen.kt:963-969`). `animateScrollBy(distance, spec(enter))`; when reduced, use `scrollToItem`.
11. **Tidy** (`Manuscript.kt:234,238`): use the `ENTER_SHIFT_DP` constant and `spec(enter, reduced)`.
12. **Small controls local to script** (`ScriptBars.kt:150,286`, `SelectViews.kt:240`): alpha via `animatedFraction`.

### feature:cast

1. **List | card** (`list/CastListScreen.kt:242-253`, `CastCards.kt:56-80`). Reuse the promoted `SnapCardRow` + `CardViewMotion`. That covers the shared thumbnail on `enterLong`, the snap on `spec(enter)`, 8 % parallax and the tick.
2. **List rows, filters, search hits, empty and lock** (`CastListScreen.kt:197-250`): P1. The search bar ↔ command bar swap (`:120-134`) uses P3.
3. **Profile tabs** (`ProfileScreen.kt:227,276-297`). One `item("tab")` holding `AnimatedContent(ui.tab, motion.swap())`.
4. **Header collapse** (`profile/RecordFrame.kt:76-98`): P3. The expanded `GlassPill` lens should render `LensChip` too, so the swap and sweep are the same in both states.
5. **Lens value swap in records** (`RecordRows.kt:33-100`, `ProfileTabs.kt:98-232`): P4 on the value and since, plus the relation steps and planned row.
6. **Expand, empty fields, art, hidden mark** (`ProfileScreen.kt:117-241`, `ProfileTabs.kt:127-142`): P2. Art top padding uses `animateDpAsState(motion.move(standard))`.
7. **Unhide** (`CastListScreen.kt:85-96,228-241`): P5 on revealed rows; LockRow icon via S6/S9 crossfade.
8. **Sheet steps** (`merge/MergeSheet.kt:82-232`, `relation/RelationEditContent.kt:97-153`, `RelationChangeContent.kt:114-116`): P3 and P2. Merge cells use `animatedColor`, keeping the font weight constant.
9. **Delete impact auto-delete flicker** (`DeleteImpactContent.kt:65-105`). Decide `needsSheet` before opening, and skip the sheet when nothing references the item.
10. **Relation map** (`relation/RelationMapScreen.kt:119-262`). Use S14's animated viewport for refit, wheel and fold. The dragged node settles via `Animatable<Offset>`. The aside uses `AnimatedVisibility(motion.enter(rise))`. The tool tint uses `animatedColor`.
11. **Field reorder** (`settings/FieldSettingsScreen.kt:156-200`). Keyed `LazyColumn` + P1. Release animates offset to the slot via `motion.move(standard)`, then commits. Neighbours shift with `animateFloatAsState`.
12. **Game text rows** (`ProfileTabs.kt:325-370`): state colour via `animatedColor`.
13. **Profile pop on null state** (`ProfileScreen.kt:119-123`): keep the last state while popping.

### feature:world

1. **List | card** (`list/ListCardMotion.kt`, `WorldScreen.kt:174-259`). Replace `ListCardMotion` with the promoted `CardViewMotion` (tokens, shared thumbnail). Delete `GrowIn` and the `delay` at `:183`; verify on a device first that it is dead.
2. **Rows and cards insert, remove, reorder** (`WorldScreen.kt:229,235`, `WorldCardView.kt:122`): P1 with placement. Drop the 160 ms delay except on the list ↔ card swap. Key card rows by section, not index (`:84`).
3. **Card snap and parallax** (`WorldCardView.kt:111-148`). Use the promoted `SnapCardRow` (`spec(enter)` snap, tick on settle, `rememberUpdatedState(reduced)`, tick kept when reduced). Name the parallax constant (D2), read it inside `graphicsLayer`, and keep a constant 1.1 scale.
4. **Tab switch** (`WorldScreen.kt:197-216`): P3 without the delay. The hint line uses P2.
5. **Locked / gone / open on cards** (`common/CardParts.kt:164-172`, `CardExtras.kt:166-185`, `TermCardScreen.kt:130`, `PlaceCardScreen.kt:57`, `FactionCardScreen.kt:56`, `time/EventDetailScreen.kt:129-153`): P5 for unlock, P3 for gone → open.
6. **Lens value swap** (`common/CardParts.kt:79-160`; FieldTableRow values come from S18f): P4. Dimmed rows (`term/UsageRows.kt:55`, `PlaceCardScreen.kt:236`, `TimelineScreen.kt:233`): alpha via `animatedFraction`.
7. **Card bodies, usage groups, timeline, event changes, settings lists** (`TermCardScreen.kt:128-262`, `PlaceCardScreen.kt:137-216`, `FactionCardScreen.kt:118-153`, `time/TimelineScreen.kt:143-246`, `EventDetailScreen.kt:175-176`, `WorldTimeScreen.kt:79-112`, `TravelModesScreen.kt:123-139`): P1 and P3. Place tabs use one `AnimatedContent(tab)`.
8. **Timeline + map** (`TimelineMapScreen.kt:122-272`). Regions get per-key `Animatable` alpha and stroke on `standard`. The map uses `AnimatedContent(mapKey)` with a 200 ms crossfade (WLD-68). Compare uses P2. Wide ↔ narrow uses `AnimatedContent(wide)` on fold tokens.
9. **Field section expand and sheets** (`CardParts.kt:121-160`, `CalendarEditor.kt:39-124`, `LinkCandidatesSheet.kt:150-196`, `EventSheets.kt`, `EventChangeSheet.kt`, `ItemSheets.kt`, `LifecycleSheets.kt`, `RevealSheets.kt`, `TermSheets.kt`): P2 and P3.
10. **Images** (`WorldCardView.kt:153-158`, `CardExtras.kt:132-138`): `Crossfade(bitmap, spec(standard))`. Selected card fill (`WorldCardView.kt:147`) uses `animatedColor`.

### feature:map

1. **Place panel, bars, cover cards** (`view/MapViewScreen.kt:194-232`, `edit/MapEditScreen.kt:487-556`, `NodeMapScreen.kt:283-331`, `PolygonRegionsScreen.kt:171-208`, `HexPaintScreen.kt:209-212`, `PickOnMapScreen.kt:169-190`). Add a shared map helper `common/ScreenParts.kt` `MapBar(kind)` = `AnimatedContent(kind, motion.swap())` with a rise on `enter`. Keep the last `PlacePanelUi` during exit. The toolbar outside uses `animateContentSize(motion.move(standard))`.
2. **Programmatic viewport moves** (`MapViewScreen.kt:118-124`, `NodeMapScreen.kt:177`, `PickOnMapScreen.kt:163`). Hold the viewport in an `Animatable` (as S14) and `animateTo(spec(enter))`, which is 320 ms; WLD-68 says 360, so use `foldOpen`. Skip the first UNSET resolve.
3. **Pan / zoom settle** (`canvas/MapCanvas.kt:124-128`, `Viewport.kt:72-85`). Replace the gesture detector with an `awaitEachGesture` loop that has an end hook. On release, `animateTo(clamped, spec(standard))` and decay the pan. Reduced: snap, no decay.
4. **Fold layout** (every screen's `if (inner)`). `common/FoldSwitch.kt`: `AnimatedContent(inner)` on `foldOpen`/`foldClose`, overlay layer only. The canvas inset uses `animateDpAsState(motion.move(fold*))`.
5. **Generate** (`gen/GenerateScreen.kt:82,112`, `StepsPage.kt:135-258`). Page: `AnimatedContent(page)` with the S1 push/pop transforms. Preview: `Crossfade((world, format), spec(standard))`. Focused step chips use P2. Candidate tiles use P2 per tile. The ring uses `animatedColor`. Per-layer re-roll and the step-order draw need new tokens (D1).
6. **Map list** (`MapListScreen.kt:72-98`). P1, P3 for empty, the promoted `SnapCardRow`, and thumbnail fade-in.
7. **Overlay marks** (`MapCanvas.kt:164-267`). Pin alpha and colour come from S18g. Pins, labels and chips here: `key(p.key)`, label and chip alpha via `animatedFraction(!dim)` (.45 ↔ 1), position via `animateOffsetAsState(motion.move(standard))`. Shapes get per-key alpha and stroke. The layer filter fades.
8. **Reveal by point** (`reveal/MapRevealScreen.kt:110-144`). Fill uses `animateFloatAsState(motion.move(standard))`. Newly visible items use P5 with a 60 ms stagger (D4: 6 px vs 7 dp). Chip numbers use P4.
9. **Base image and tiles** (`canvas/BaseLayers.kt:84-128`): fade in on `standard`. Replace and export use `DelayedProgress`.
10. **Edit drafts, vertex, region close, route, stamp, hex fill** (`MapEditScreen.kt:400-463`, `hex/HexPaintScreen.kt:139-160`). Vertex pop comes from S18g; closing fills alpha over 200 ms `standard`, a route draws via `PathMeasure` on `enter`, and hex fills spread by distance in at most 300 ms. Stamp 240 ms is D1. Reduced: final state with a 120 ms alpha.
11. **Character track** (`track/CharacterTrackScreen.kt:100-148`). Person chips use `Animatable<Offset>` along the route on `enter`. Name the 900 ms play step. Focus stroke uses `animatedFraction`.
12. **Text to map** (`text/TextMapScreen.kt:128-285`). Pages use P3. The world uses P2. Rows appear one by one (D1: 250 ms). The highlight uses `animatedFraction`.
13. **Panel tabs and lists** (`edit/EditPanels.kt:244-262`, `EditSheets.kt:84-97`, `TravelModeSheet.kt:86-108`, `NewMapSheet.kt:99-114`, `MapMenuHost.kt:168`, `MapScaleScreen.kt:121-150`): P3 for tabs and views, P1 for lists (`LayersPanel` as a keyed `LazyColumn`), P2 for option expand.

### feature:canvas

1. **Zoom buttons and initial fit** (`board/CanvasBoardScreen.kt:195-197,271-272`). Use S14's `rememberAnimatedViewport`. Hold the surface at alpha 0 until node sizes are measured, then fade in on `enter`. The percent label uses P4.
2. **Picker → crop hand-off** (`ImagePickSheetContent.kt:94-98`, `image/crop/ImageCropScreen.kt:97-158`, `CropStage.kt:86-118`). Show the tile's in-memory bitmap from frame 0 and start the grow immediately. Give the `ImageCrop` route a fade-only transition (request, S2-style metadata). Controls fade in on `enter`.
3. **Page list rows and card row items** (`page/PageListScreen.kt:102-176`, `card/SnapCardRow.kt:86`): P1, P3 for empty/list, P5 for unlocked hidden pages.
4. **List | card nudge and radius** (`card/CardViewMotion.kt`, `PageListScreen.kt:162`, `CardParts.kt:187-189`). The second card moves aside: −24 dp→0 on `enterLong`. The radius morphs via `animateDpAsState`. Use an explicit `SizeTransform(clip = false) { _, _ -> snap() }`.
5. **Parallax scale** (`image/SlotImage.kt:44-49`, `SnapCardRow.kt:87-91`). Constant 1.1, or 1.0 when reduced; displacement is `offset - inset`; read it inside `graphicsLayer`.
6. **Crop** (`CropStage.kt:78`): reduced uses `spec(standard, true)` instead of `snap()` (alpha crossfade of the frame). Rotate, flip, reset and zoom steps (`ImageCropScreen.kt:229-243`): `Animatable` via `motion.move(standard)`; reduced snaps.
7. **Holo** (`image/HoloLayer.kt:106-191`, `HoloRules.kt:33-35`). Blend the sweep end into the live tilt on `standard`. Fire the sweep once, on the toggle edge, not on restore (`ImageCropScreen.kt:94,167`) or on a tile change (`ImagePickSheetContent.kt:140`). The sway becomes a damped spring `Animatable` so the animator scale applies. The foil fades off on `exit`.
8. **Page read ↔ edit and access** (`page/PageDocScreen.kt:169-240`): P1 on header, head and paragraphs; P3 on the title and body only, never moving the typed line; P5 for unlock.
9. **Sheets** (`LineLabelContent.kt:128-258`, `NodeEditContent.kt:62-86`, `PlaceItemSheet.kt:50-58`, `ImagePickSheetContent.kt:79-186`): P3, P2 and P4. The tile grid uses `AnimatedContent(tab)`.
10. **Tool bar and loading** (`CanvasChrome.kt:67-99`, `CanvasBoardScreen.kt:85-94`): `animatedColor` for cells, P3 for board loading and NoCanvas. Fold chrome (`CanvasChrome.kt:55-71`, `SnapCardRow.kt:72,83`) uses `animateDpAsState(motion.move(fold*))`.
11. **Holo experiment** (`HoloExperimentScreen.kt:79-103`): P3 on the tab. Tilt through a spring.

### feature:recorder

1. **Capture stage swap and glass fill** (`RecordingRoute.kt:136-182`, `capture/CapturePanes.kt:70-75`). Use `AnimatedContent(state.stage, motion.swap())` with no slide, because the typed line never moves. Glass fill alpha uses `animatedFraction(glass)` drawn behind.
2. **Glow visibility** (`CapturePanes.kt:75`). Keep the last mode and fade on `enter`/`exit`. The mode blend is S13.
3. **Inbox** (`inbox/InboxScreen.kt:163-271`). P1 on rows, search, lock, open group and filter items. Loading, empty and rows use P3. The bottom bar uses `AnimatedVisibility(motion.enter(rise))`. Header `다시 잠그기` uses P2 (horizontal).
4. **Reveal** (`InboxScreen.kt:135-148`, `inbox/RevealMotion.kt`). Becomes the shared P5 (S-promotion). Animate with the token easing, and fade the `OpenGroupBox` container with it.
5. **Processing rows and link chips** (`CapturePanes.kt:140-221`, `link/LinkChipRow.kt:50-79`, `InboxRowItem.kt`). Use P2, or chips in a keyed `LazyRow` with P1. Row content changes use `animateContentSize(motion.move(standard))`.
6. **Auto-scroll** (`CapturePanes.kt:114`). `animateScrollTo(max, spec(standard))`; reduced uses `scrollTo`.
7. **Caret** (`CapturePanes.kt:134-137`): blink as S12; solid when reduced.
8. **Memo and mic reason sheet** (`memo/TextMemoSheet.kt:84-140`): P3 on the stage, P2 for candidates and the hide row.
9. **Record detail and player** (`record/RecordScreen.kt:108-211`, `audio/PlayerRow.kt:48-87`). Status uses P3, the body `animateContentSize`. Play/pause uses `Crossfade(spec(standard))`. Played bars use `animatedColor`.
10. **Small states** (`CapturePanes.kt:100,170`, `InboxRowItem.kt` ActionTag alpha). The mark alpha and enabled alpha use `animatedFraction`; the pause icon uses `Crossfade`.
11. **Processing with the glow off or reduced** (`CapturePanes.kt:187-224`): `DelayedProgress(finalising)`.
12. **Link picker** (`link/LinkPickerSheet.kt:84-100`): P1.
13. **Nested sheet layouts** (`RecordingRoute.kt:119-136`). Stop the double back-scale (0.965² and 4 dp) when the picker opens over the memo sheet.

## 5. Open decisions (spec owner)

- **D1.** New tokens for DS-14 / CNV-37: map layer re-roll in 400 ms, text-to-condition row 250 ms, map generation about 1.2 s, stamp drop 240 ms. Until decided, feature code must not use literals.
- **D2.** Card parallax 8 % and image scale 1.1 as named constants: `CivilightMotion.cardParallax`?
- **D3.** Recorder glow sweep 2400 ms and level period: tokens, or the glow's "own durations"?
- **D4.** WLD-49 says unblur 6 px; CNV-43 and `unhideBlur` say 7 dp. Pick one.
- **D5.** App lock: confirm that showing the lock stays instant (security) and only unlocking animates.
- **D6.** `ProjectRow` 48 dp thumbnail (spec) is missing, and CNV-45 list ↔ card in the library needs it.


**Decided by the orchestrator (2026-11-23), so wave 0 and wave 1 can proceed:**
- D1: yes. Add `CivilightMotion` tokens `mapReroll` 400 ms, `rowIn` 250 ms, `mapGenerate` 1200 ms, `stampDrop` 240 ms (standard easing; reduce rule as §3.1).
- D2: yes. `CivilightMotion.cardParallax = 0.08f`, `CivilightMotion.cardImageScale = 1.1f`.
- D3: tokens. `recorderGlowSweep` 2400 ms and `recorderLevelPeriod`; reduced mode stops the sweep (static glow).
- D4: 7 dp everywhere (token `unhideBlur`); WLD-49's 6 px is read as the same value.
- D5: confirmed. The lock appears instantly; only unlocking animates.
- D6: add the 48 dp `ProjectRow` thumbnail from the spec; library owns it.

## 6. Order and ownership

1. **Wave 0, core agent** (`core:designsystem`, `core:ui`, `app`): §3.2 helpers and `MotionLint`, then S1–S18g in table order. Prototype S2, S3 and S4 first. Then run `tools/gw :core:designsystem:testDebugUnitTest :core:ui:testDebugUnitTest :app:compileDebugKotlin`.
2. **Wave 1, one agent per feature module, in parallel**: §4, in each module's order. Each runs only `tools/gw :<module>:compileDebugKotlin :<module>:testDebugUnitTest` and adds `MotionLint` to its copy test. Anything needed in a shared module goes under "## Requests".
3. **Orchestrator emulator pass**, both with and without "항상 줄이기" and with developer animator scale 0: overlay blanking, glass lag, two-pane detail, fold switch, the `GrowIn` replacement, API 29 IME. No module is `done` before this pass (DECISIONS §3).
