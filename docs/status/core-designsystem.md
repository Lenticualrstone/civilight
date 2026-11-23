# core/designsystem

Package root `io.github.lenticualrstone.civilight.designsystem` (sub-packages `theme`, `glass`, `foundation`, `icon`, `text`, `component`, `preview`). Build: `tools/gw :core:designsystem:compileDebugKotlin :core:designsystem:testDebugUnitTest :app:compileDebugKotlin` exits 0 (2026-11-23, after the motion wave 0); 72 JVM tests in 8 classes, all passing. Every visual component has a dark and a light preview in `preview/*Previews.kt`. The hosts `ScrimDialog`, `Popover`, `DelayedProgress` and `SheetScrollBody` are previewed through their cards or content. Nothing was run on a device or emulator.

## Done

- **Integration check (2026-11-23):** `CivilightMotion` gained the holo spring values that lived in feature:canvas, unchanged: `holoSwayStiffness` 20, `holoSwayDamping` 0.25 (released drag sway), `holoTiltStiffness` = `Spring.StiffnessMediumLow`, `holoTiltDamping` = `Spring.DampingRatioNoBouncy` (sensor tilt follow). No spring with a literal stiffness is left outside `CivilightMotion`.
- **DS-01 theme**: `theme/CivilightTheme.kt` (`CivilightTheme(themeMode, textScale, glassMode, reducedMotion, applyToWindow)`, locals, `object CivilightTheme` accessors, `materialBridge` (no dynamic colour), window `bg` + system-bar icon appearance), `theme/ThemeMode.kt` (`resolveTheme`), `component/ThemeSwatch.kt`, `res/values{,-night}/civilight_colors.xml` for splash and window. Test `TokensTest.T-DS-04 theme resolution`.
- **DS-02 colour tokens**: `theme/CivilightColors.kt` (`ColorRole`, `ColorPalettes` with the declared rgba, `CivilightColors.dark/light`, `colorsFor`). Tests `T-DS-01 dark tokens`, `T-DS-02 light tokens`, `T-DS-03 alpha rounding`.
- **DS-03 fixed colours**: `theme/FixedColors.kt` (on-image, shades, crop, `mapArt(theme)`, `terrain`, `recorderGlow(theme)`, holo stops). Test `TokensTest.DS-03 fixed colours`.
- **DS-04 contrast**: `theme/Rgba.kt` (`relativeLuminance`, `contrast`, `Rgba.over`), `tokenContrast`. Tests `ContrastTest` T-DS-05 … T-DS-11 plus the ink3-on-tint table row.
- **DS-05 fonts**: `theme/CivilightType.kt` `Pretendard` (4 bundled weights, `FontSynthesis.None`); OFL text shipped as `src/main/assets/licenses/Pretendard-OFL.txt`.
- **DS-06 type roles**: `CivilightType` (all roles of §3.1 plus `emptyTitle`, `itemTitleCompact`, `leadCompact`, `numberHeader`, `wordmarkWelcome`), `TypeRole`, `defaultColorRole`; `tnum` on the 숫자 group; `LineHeightStyle(Center, None)`. Test `T-DS-18`, `DS-07 scaling formulas`.
- **DS-07 text-size scaling**: `theme/TextScale.kt` (`line`, `body`, `paren`, `field`, `recorderLive` formulas, `StepsSp`, `forStep`). Fixed-height components (buttons, chips, pills, lens chip, badges, segmented controls, command bar, fields) use `heightIn(min = …)`, so a larger system font scale grows them instead of clipping. Tests `T-DS-18 default text size`, `DS-07 scaling formulas`, `DS-34 slider snaps…`.
- **DS-08 Korean typesetting**: `CivilightType.KoreanLineBreak` (HighQuality · Strict · Phrase), tracking per role, `CivilightText` single-line ellipsis, `foundation/Hairlines.kt` `Modifier.readingMeasure()` (560).
- **DS-09 spacing / layout**: `theme/Tokens.kt` `CivilightSpacing`, `LayoutMetrics` (bar heights, insets, z order).
- **DS-10 shape / elevation / blur / size**: `CivilightShapes`, `CivilightSizes`, `CivilightBlur`, `CivilightElevation`, `foundation/Shadows.kt` `Modifier.tokenShadow`.
- **DS-12 icons**: `icon/CivilightIcons.kt` (`IconId` with every DS-12 path incl. `StepOpen`, `Exclaim`; stroke 1.6, round caps; `checkBold`), `icon/CivilightIcon.kt`. Tests `T-DS-23 icon stroke scales`, `DS-12 every icon path parses`.
- **DS-13 app mark**: `icon/AppMark.kt` (`MarkGeometry.Mark96/Static64`, `AppMark(barProgress)`, `Wordmark`, `BiometricButton`), drawables `civilight_mark.xml`, `civilight_launcher_foreground.xml`, `civilight_launcher_monochrome.xml`. Test `DS-13 mark geometry`.
- **DS-14 motion**: `theme/CivilightMotion.kt` (`Bezier`, `MotionToken`, all tokens, `spec(token, reduced)`). Test `T-DS-33 motion tokens`.
- **DS-15 haptics**: `theme/Haptics.kt` (`HapticToken`, `hapticConstant`, `rememberHaptics()`). Test `DS-15 haptic mapping`.
- **DS-16 / DS-17 markers**: `text/Markers.kt` (`MarkerKind`, `MarkerLine`, `markerStyle`, `markerSpokenText`), `text/MarkerGlyph.kt` (vector glyphs for all six incl. `▸ ⌁`, inline placeholders keeping the Unicode characters). Tests `T-DS-12`, `T-DS-14`, `DS-16 spoken markers`.
- **DS-18 dotted**: dotted underline (`InlineDecoration.DELETED_REF`), dashed marker outline (`MarkerLine.DOTTED`, label `근거 지워짐` / `대상 지워짐`), dashed row divider (`RowDivider.DASHED`), `ListRow(titleDotted)`, `SpeakerLabel(deleted)`. Tests `T-DS-13`, `T-DS-14`.
- **DS-19 inline decorations**: `text/Decorations.kt`, `text/DecoratedText.kt` (per-line underlines and boxes from the text layout, paren spans, caret with 1 s blink), `text/ManuscriptText.kt` (`ScriptRun`, `manuscriptText`). Tests `DS-19 link and candidate decorations`, `DS-19 manuscript runs…`.
- **DS-20 rating badge**: `component/RatingBadge.kt`, `badgeStyle`. Test `T-DS-15`.
- **DS-21 glass bars**: `component/GlassBars.kt` (`TopGlassBar`, `BottomGlassBar`, `GlassScaffold`). Test `T-DS-34` (masks / borders). The blur itself is not verified on a device (see DS-22).
- **DS-23 sheet**: `component/Sheet.kt` (`CivilightSheetLayout` with scrim, receding background 0.965 + 2 dp blur, glass, grab handle, drag-to-dismiss, max top, centred inner variant; `SheetHeader`, `SheetTabsHeader`, `SheetActions`, `SheetScrollBody`).
- **DS-24 side panel**: `component/SidePanel.kt` (`SidePanel`, `PanelCard`).
- **DS-25 dialogs**: `component/Dialogs.kt` (`ScrimDialog`, `ConfirmDialog`, `DialogCard`, `FloatingDialog`), `confirmButtonVariant`. Test `T-DS-29`.
- **DS-27 toast**: `component/Toast.kt` (`CivilightToast`, `ToastHost`), `toastStyle`. Test `T-DS-32`.
- **DS-28 banners / image fade**: `component/Banners.kt` (`LensEditBanner`, `ReadOnlyBanner`, `FadingHeaderArt`).
- **DS-29 command bar**: `component/CommandBar.kt` (`CommandBar` with mic / typed / close states), `MicButton`.
- **DS-30 buttons**: `component/Buttons.kt` (`CivilightButton` for every `ButtonVariant`, `ButtonRow`, `TextAction`), `buttonStyle`, `hasAtMostOnePrimary`. Test `T-DS-29`.
- **DS-31 icon buttons / pills / keyboard tools**: `component/IconButtons.kt` (`CivilightIconButton`, `GlassRoundButton`, `GlassPill`, `Pill`, `SpeakerPill`, `MicButton`, `KeyboardTool`, `KeyboardToolsRow`).
- **DS-32 controls**: `component/Controls.kt` (`CivilightSwitch`, `RadioMark`, `CheckboxMark`, `RadioRow`, `CheckboxRow`, `SwitchRow`, `ChoiceCard`, `TypeGridCell`), `switchStyle`, `radioStyle`, `checkboxStyle`. Test `T-DS-30`.
- **DS-33 segmented**: `component/Segmented.kt` (`SegmentedControl`, `OutlinedSegmented`, `SegmentItem`), `segmentStyle`. Test `T-DS-31`.
- **DS-34 slider**: `component/StepSlider.kt` (`StepSlider`, `sliderStepFor`). Test `DS-34 slider snaps…`.
- **DS-35 text fields**: `component/TextFields.kt` (`CivilightTextField` ×2 overloads, `PasswordField`, `SearchField`, `fieldOutline`), `ValueUnderEditRow`. Test `DS-35 field outline`.
- **DS-36 chips**: `component/Chips.kt` (`FilterChip`, `ChipFlow`, `RemovableChip`, `AddChip`, `MapValueChip`, `LayerFlagChip`, `ImageStatusChip`).
- **DS-37 tabs**: `component/Tabs.kt` (`CivilightTabs` with sliding underline, `TabSets`, `TabGap`), `tabStyle`. Test `T-DS-31`.
- **DS-38 lens chip**: `component/LensChip.kt` (text swap + `acc` sweep), `lensChipStyle`. Test `T-DS-16`.
- **DS-39 view toggle**: `ViewToggle` in `component/Segmented.kt`.
- **DS-40 rows**: `component/ListRows.kt` (`ListRow`, `SettingsRow`, `PickerRow`, `FieldTableRow`, `KeyValueLine`, `FieldActionRow`, `SectionLabel`, `Modifier.selectedRowBackground`), `selectionStyle`. Test `T-DS-35`.
- **DS-41 lock row**: `LockRow` in `component/States.kt`.
- **DS-42 script block**: `component/ScriptBlock.kt` (`ScriptBlock`, `SpeakerLine`, `SpeakerLabel`, `DirectionLine`), `text/SpeakerParts.kt` `speakerLabelParts`, `directionLineText`. Tests `T-DS-19`, `DS-42 direction line spacing`.
- **DS-43 scene head**: `component/SceneHead.kt`.
- **DS-44 swipe tabs**: `component/SwipeTabs.kt` (`SwipeTabsRow`, `SwipeRevealBox`).
- **DS-45 search hit**: `component/SearchHit.kt`, `text/SearchHitRuns.kt` (`searchHitRuns`, `normalizeMatches`, `searchHitDecorations`), staggered box reveal in `DecoratedText(revealBoxes)`. Tests `T-DS-20`, `search runs clamp and merge`.
- **DS-46 avatar**: `component/Avatar.kt`, `avatarGlyph`, `avatarGlyphSp`. Test `T-DS-22`.
- **DS-47 empty / error**: `component/States.kt` (`EmptyBlock`, `NoResults`, `ErrorBanner`, `FullScreenError`, `DelayedProgress`).
- **DS-48 progress**: `component/Progress.kt` (`StepBars`, `StepBarsRow`, `TourProgress`, `PagerMarks`, `ProgressBar`, `StepList`, `stepState`). Test `DS-48 step states`.
- **DS-49 tooltip**: `component/Tooltip.kt` (`TourCard` with arrow, `TourDim` rounded cut-out + `acc` frame).
- **DS-50 node card**: `component/NodeCard.kt`.
- **DS-51 cards**: `component/ImageCards.kt` `ImageCard`, `ShardArt`; `ImageStatusChip`.
- **DS-52 image tile**: `ImageTile` (double ring).
- **DS-53 crop frame**: `CropFrame`, `CropRatioRow`.
- **DS-55 map tools**: `component/MapTools.kt` (`MapToolbar`, `MapToolDefaults`, `LockToggle`, `RerollButton`, `GeneratorStepRow`, `StepChip`, `TerrainSwatch`, `FormatTile`).
- **DS-56 map marks**: `component/MapMarks.kt` (`MapPin`, `MapVertex`, `RouteStop`, `PersonChip`, `Breadcrumb`, `ScaleBar`, `TimelineLabel`).
- **DS-57 map style colours**: `FixedColors.mapArt`, `FixedColors.terrain` (rendering is WLD's).
- **DS-58 when picker**: `component/WhenPicker.kt` (`WhenPicker`, `WhenPickerModel`).
- **DS-59 condition row**: `component/ConditionRow.kt`.
- **DS-60 travel bar**: `component/TravelBar.kt`, `travelBarFraction`, `warningSpan`. Tests `T-DS-21`, `DS-02 warning text…`.
- **DS-61 recorder glow**: `component/RecorderGlow.kt` (`RecorderGlowBand`, `glowBandScale`, `RecordingMark`). Test `DS-61 glow bands stay between 0_72 and 1_08`.
- **DS-64 accessibility**: roles (Button / Tab / RadioButton / Checkbox / Switch), selected / toggle state through `pressable`, Korean labels on icon buttons, marker words via `contentDescription`, decorative layers `clearAndSetSemantics`, `Modifier.focusRing` (2 dp `acc`, 2 dp outside), progress semantics, toast live region.

- **Core pass (2026-11-22, for the feature lanes; details in shell.md "Changes for the feature lanes")**:
  - `BottomGlassBar` and `CivilightSheetLayout` sit above the keyboard via the public `KeyboardLift` insets (consumption-aware); `BottomGlassBar(aboveKeyboard = true)`.
  - `IconId.Pencil · Shield · Download · Minus`.
  - `NodeCard(deleted = …)` (dotted `ink3` outline + `◇`, rule 20 / CNV-11) with the pure `nodeCardLook`; `insetOutline(dotted = …)`.
  - Test: `NodeCardLookTest` (4). `:core:designsystem:testDebugUnitTest` 56 passing.

- **Motion wave 0 (2026-11-23, `docs/status/motion-audit.md` §3)**, details and API in "Motion helpers (for feature lanes)" below. UI motion stays **partial** until the orchestrator's emulator pass (with and without 항상 줄이기, and animator scale 0).
  - §3.1 / §3.2: `theme/Motion.kt` (`Motion`, `LocalMotion`, `CivilightTheme.motion`, `MotionRules`, builders, `animatedColor` / `animatedFraction` / `animatedAlpha`, `civilightItem`, `ValueSwap`, `ContentSwap`, `FoldReveal` / `FoldRevealValue`, `rememberUnhide` + `Modifier.unhide`, `rememberLastNonNull`, `rememberAnimatorScale`), `theme/MotionLint.kt`, D1–D3 tokens and `press` in `CivilightMotion`, `CivilightMotion.spec(token, reduced, delayMs)`. `CivilightTheme` reads the system animator scale itself (live) and folds scale 0 into reduced motion.
  - Promoted copies (lanes delete theirs in wave 1): `card/CardRules.kt`, `card/SnapCardRow.kt`, `card/CardViewMotion.kt` (canvas), `Motion.panelEnter` / `panelExit` (script `FoldMotion`), `rememberUnhide` / `UnhideMotion` (recorder `RevealMotion`), `Motion.valueSwap` (LensChip).
  - §3.3 DS rows: **S5** sheet back-screen origin 50 % / 30 %, centred sheet .96 → 1, scrim and back scale / blur follow the drag (`sheetOpenAmount`); **S4 (DS part)** `animateContentSize(motion.move(standard))` on the sheet surface; **S6** shared `SlidingIndicator` (tabs underline, `SegmentedControl` pill, `OutlinedSegmented` fill; reduced = crossfade in place, also fixes the reduced underline snap), animated colours / fills / outlines / enabled alpha in `ListRow` and every `selectedRowBackground` user (now a `Modifier.Node` that fades; signature unchanged, still not composable), `FilterChip`, switch (track, thumb colour; thumb snaps when reduced), radio (ring colour, dot scales 0 → 1 on `enter`, fades when reduced), checkbox (fill, ring, tick fades + scales on `enter`), `ChoiceCard`, `TypeGridCell`, `KeyboardTool`, text-field outline + width, error / hint lines via `FoldRevealValue`, `StepBars`, `TourProgress`, `PagerMarks` (width on `move`), `ProgressBar` (fill width on `move`, drawn, no relayout), `StepList`, `ImageTile` ring, map toolbar cells, `LockToggle` (+ icon crossfade), `StepChip`, `TerrainSwatch`, `FormatTile`, `NodeCard`, `CivilightButton` enabled alpha, `LensChip` outline and text; **S7** press scale on the `standard` curve (`CivilightMotion.press`), pressed `ink` 6 % overlay where nothing scales (`scaleOnPress = false` or reduced), ghost layer animated, new trailing `pressOverlay` param; **S8 (DS parts)** `Popover` rises 6 dp on `enter` and plays its exit before `onDismissRequest`; `ScrimDialog` one progress for scrim + 12 dp rise, animated dismiss for back / scrim / `ConfirmDialog` 취소 (`LocalDialogDismiss`), window animations off; `TourDim` hole moves on `move(standard)` and fades; `TourCard` step body `ContentSwap`; new `SidePanelHost(visible)`; **S10** `ToastHost(id = message)` swaps by id + `animateContentSize`; **S11** `DelayedProgress` follows reduced motion; **S12** search boxes grow from the left edge on `standard`, 60 ms stagger, 120 ms fade when reduced (`searchBoxReveal`), caret step blink stops (solid) when loops are off; **S13** glow clock is a frame clock that runs only when needed, mode changes blend level / sweep amounts on `standard`, durations are tokens (`glowSweep`, `glowAmounts`); **S18a** command bar hint ↔ typed and mic ↔ 닫기 crossfade, top bar back button folds in with the start padding on `move`; **S18b** slider tracks the finger, only the step snap animates (snaps when reduced); **S18c** `TabContent(selectedIndex)`; **S18e** glass mode change crossfades blur, fill and opaque borders; **S18f** `FieldTableRow` value through `ValueSwap`; **S18g** `MapPin` dim and colour cross over, `MapVertex(popOnAppear = true)` pops .6 → 1.3 → 1 on `enter` (fade when reduced), `RouteStop` colours.
  - Gallery (spec 55 §3.5): `ViewToggle(mode: ViewToggleMode, onChange)` three-cell overload `목록 · 격자 · 카드` with `IconId.GridTall` (board `scr_gallery.py` `I_GRID9`, six tall cells); the two-cell `ViewToggle(showCards, onChange)` is unchanged.
  - Tests: `MotionHelpersTest` (16): reduce rule, loops, scaled waits, spec / move / placement, D1–D3 tokens, unhide values, card rules, MotionLint rules and suppression, DS sources lint-clean, search box reveal, glow amounts and sweep, sheet drag amount, vertex pop.

## 0.2.0 shared writing components (2026-11-24, not yet wired into features)

- **DS-71 icon set**: `IconId` took the r2 paths of `Check`, `Search`, `X`, `List`, `File`, `Split`, `Merge` and gained `Send`, `NextScene`, `Edit`, `InsertAfter`, `Kind`, `SceneCut`, `Copy`, `Cut`, `MoveBlock`, `Graph`, `Prev`, `Up`, `Down`, `ToTop`, `ToBottom`, `SelectBox`, `Caption`, `Expand`, `CardFlip`, `Redeal`, `OpenOut`, `Fan`, `Gather`, `Lift`, `Drop`. `IconId.forKey(r2Key)` covers all 51 keys (`mirror` → `Flip`, `flip` → `CardFlip`, `move` → `MoveBlock`, `close` → `X`).
- **DS-70 icon buttons**: `icon/IconButtonRules.kt` (`IconClass`, `IconButtonSpec` per table button, `IconButtons.classOf` · `nameOf` · `row` · `offName`, `IconButtonCopy`). Composables `IconOnlyButton` (a), `IconLabelButton` (b, name may differ from the label, toggle state), `CivilightIconButton(spec, …)`. `icon/IconButtonLint.kt` flags icon-only calls with a blank, null or missing `contentDescription`; `Writing020Test` runs it over every module's sources. Left out of the registry: B25 `조건` and B26 (hidden map generator, no r2 key). B4 `◆ 시간` is text `시간` with the `◆` glyph, name `시간`.
- **U-14 input bar** (SCR-70, SCR-16, SCR-72, SCR-73): `component/ScriptInputState.kt` (`InputKind`, `InputSpeaker`, `InputMark`, `ScriptInputState`, `ScriptInputEvent`, `ScriptInput.reduce`, `ScriptInputRules`, `ScriptInputCopy`), `component/ScriptInputBar.kt` (`ScriptInputBar`, `ScriptInputActions`, `ScriptInputReadOnlyRow`). The script owns `SceneInView`, `MergeCheck`, cues and undo and passes them in (`InputContext`, `mergeOffReason`). Chip changes during `고치기` stay in the draft until the send (SCR-72 default).
- **SCR-30 swipe**: `SwipeSide`, `SwipeReveal` (settle past half, clamp, rest offsets 192 / 300), `TwoSidedSwipeRow`, `EditSwipeButtonsRow` (5 × 60, 60 ms stagger) in `component/SwipeTabs.kt`. `SwipeRevealBox` and `SwipeTabsRow` are unchanged.
- **DS-72 create card** (U-15): `component/CreateCard.kt` (`CreateCardMode`, `CreateButton`, `CreateKind` with the field sets of U-15.5 / .7 / .9, `CreateCardState`, `CreateCard.buttons` · `canCreate` · `title` · `rowLabel` · `fit` · `dismissOnScrim`, `CreateCardCopy`; `CreateCardOverlay`, `CreateCardBody`, `CreateCardFields`, `CreateRow`, `ListCreateRow`, `PickerCreateRow`). core:ui `ItemPickerSheet` takes optional `createKind` + `onCreate` (default off) and `PickerCreate.matched`.
- **Motion tokens**: `CivilightMotion.placeFlash` (600 ms, the `씬 n 끝에` flash) and `createdRowHighlightMs` (1 200).
- **Not verified on a device**: keyboard insets inside the create card's dialog window, the input bar's nested caret follow at 6 → 7 lines, swipe feel.
- **Not built here**: DS-73 search chips, DS-74 tilt primitives.

## Partial

- **DS-22 glass material**: `glass/GlassStyle.kt` (`GlassMode.resolve`, `glassStyle`, `SCRIM_ALPHA`) and `glass/GlassMaterial.kt` (`GlassSource`, `Modifier.glassSource`, `GlassBackground` with RenderEffect blur and saturation 1.4 on API 31+, DstIn edge masks, OPAQUE borders, `GlassBudget` for BLUR_COSTLY, `OnGlass` so glass never samples through glass). Test `T-DS-34 glass rendering by mode`. Missing: the blur path has never run, because no device or emulator was allowed. Rendering and GPU cost on API 31–33 are unverified.
- **DS-11 touch targets**: `foundation/HitArea.kt` (`hitArea`, `Modifier.expandedHitArea`) widens the touch area without changing layout; used by every tappable component. Missing: the midpoint split between chips 6–8 dp apart (overlaps go to the later-drawn sibling), and a clipping ancestor cuts the extra area. Test `T-DS-17`.
- **DS-26 popover**: `component/Popover.kt` (`Popover` in a `Popup`, `PopoverCard`, `AnchorBelowPositionProvider`). A `Popup` is its own window and cannot sample the backdrop, so the card uses the SCRIM fill (`raise` 92 %) instead of blurred glass.
- **DS-54 holo**: `component/Holo.kt` `Modifier.holoFoil(tilt, theme, strength)`, `HoloStrength`, `HoloDefaultTilt`. Blend strengths and parallax factors are approximations of the CSS reference; tilt input and sweep are CNV-30's.
- **DS-62 copy lint**: `text/CopyLint.kt` (`lintString`, `CopyLint.lint`, `CopyLint.lintStringsXml`). Tests `CopyLintTest` T-DS-24 … T-DS-28 plus `strings xml lint`. Missing: no test runs it over every module's `values/strings.xml`. Running that from this module's tests would tie this build to other agents' work in progress (see Requests).
- **DS-63 vocabulary**: covered by the mechanical rules in `CopyLint` (삭제, 장면, 복원, 백업에서 가져오기, 미지정, 선택, 변경, 수정, 넘기기, 미리보기, 접힌 화면, 지문으로만, 붙일 곳, 씬 전환 블록), with the allowlist. The review-only rules are not linted: project `제목`, and `받기` used for "apply".
- **Motion wave 0, emulator checks** (nothing above ran on a device): card parallax origin (`LazyListItemInfo.offset` taken as 0 at rest; the audit says offset − inset); sheet `animateContentSize` with the IME inset changing; the 6 % press overlay on large `scaleOnPress = false` surfaces (rows); a recycled lazy row fading its `selectedRowBackground`; animator scale 0 reaching Compose animations and stopping the glow / caret; popover and dialog exit before dismiss; glass mode crossfade on API 31–33.
- Not checked on a device: the receding sheet background (DS-23), the expanded hit areas (DS-11) and TalkBack output (DS-64). They have compile checks and previews only.

## Defaults used

- **Glass without Haze.** Haze is not in the version catalog. `GlassSource` records the content into a `GraphicsLayer`. `GlassBackground`, drawn as a sibling above that content, redraws the layer with `BlurEffect(36 dp)` and a 1.4 saturation `ColorMatrix`, then the fill, then a `DstIn` gradient mask. On API < 31, or when no `LocalGlassSource` is present, it draws the SCRIM look. A glass surface must never be a descendant of its own source.
- SCRIM fill = `raise` at alpha `0xEB` (Q-DS-14). In `BLUR_COSTLY`, only the first open sheet or panel blurs; later ones draw SCRIM.
- `TopGlassBar` height = `barHeight` + status-bar inset, with contents 16 / 18 dp below the inset. `BottomGlassBar` min height = 130 / 150 + navigation inset, contents 18 dp above the inset. `GlassScaffold` passes the full bar heights to its content as padding.
- System back for in-composition sheets and panels goes through `LocalBackHandler` (default no-op), because there is no activity-compose here. Dialogs use `Dialog` with the platform dim turned off and our `shade` scrim. Popovers use `Popup`.
- Pressed state: content scale 0.94 over 100 ms on the `standard` curve; where nothing scales (`scaleOnPress = false`, or reduced motion) an `ink` 6 % overlay fades in and out instead (S7); ghost buttons add an `ink` 8 % state layer that fades the same way. Disabled: 38 %. Focus: 2 dp `acc` ring 2 dp outside. Error field: 1.5 dp `r18` with a `small` `r18` line.
- Weight 800 roles request `FontWeight.ExtraBold`; until ExtraBold ships, the font matcher renders Bold 700. Korean phrase line breaking is set on every style (Compose applies it on API 33+).
- `TextScale` steps 16 / 17 / 18 / 20 / 22 (LIB-65 ED), default index 2. `paren` line height scales as `round(S × 20 / 18)`. Interface roles do not follow the step.
- Marker vector shapes: diamond as in `DM`, triangle, `▸` filled triangle, `♪` eighth note, `⌁` zig-zag. Inline size 0.75 em in a 1 em advance, centred; speaker-line markers 11 dp. Spoken words: `◆` 변화, `◇` 첫 공개, `▲` 경고. `▸ ♪ ⌁` are dropped from speech.
- Dotted underline dash 2 / 2 dp. Dashed outlines (memo node, deleted row divider) 4 / 3 dp.
- Search and candidate boxes are sized from the Pretendard ascent / descent (0.92 / 0.24 em), not from the line box. Box reveal (CNV-44, S12): 60 ms stagger, each box grows from its left edge over `standard`; reduced: each fades in over 120 ms.
- Sheet: drag dismiss past 40 % of its height; the centred inner sheet has radius 28 on all corners and max width 560 (DS-23 proposed); top limit status bar + 56 dp.
- Lens chip value change: the old text slides up 8 dp and the new one comes from below (200 ms), with a 1.5 dp `acc` underline sweeping from the left 7 dp above the bottom.
- Toast: rises 8 dp + fade with `Enter`, fades out with `Exit` (CNV-37 lists no toast motion); a new toast id crossfades the card in place (S10).
- Caret (DS-14): 1 s step blink while loops run; solid when reduced or with animator scale 0.
- Animated dismiss (S8): `ScrimDialog` and `Popover` fade out before calling `onDismissRequest`; if the caller keeps them composed (a no-op dismiss), they fade back in after two frames instead of leaving an invisible window.
- Haptics: TICK = `SEGMENT_TICK` on API ≥ 34, else `CLOCK_TICK`. CONFIRM = `CONFIRM` on API ≥ 30, else `VIRTUAL_KEY`.
- Recorder glow: band scale = 0.72 + 0.36 × (½ oscillation + ½ level). Saturation × 1.2 is applied per colour rather than as a group filter.
- Holo: silver layer at 55 % overlay, tint at the confirmed strength in overlay blend, sheen band 24 % of the diagonal at 35 % white. Tint shifts by 50 % of tilt, silver by 30 %.
- Theme swatch: 72 dp tall, radius 14, two `ink` lines and an `acc` caret; selected 1.5 dp `acc` inset.
- Launcher icon (Q-DS-13): the dark-theme 96-unit mark scaled 0.6875 into the 66 dp safe zone, on `#0B0C0E`, plus a white monochrome layer. Drawables only; the app decides.
- OFL licence placed at `assets/licenses/Pretendard-OFL.txt`.
- Copy lint: keys starting `btn_` or `action_` are buttons; keys starting `sample_` or `content_` hold story content and are skipped. Emoji detection uses an Emoji_Presentation range table (plane-1 emoji blocks taken whole) plus VS16.
- Kept the drawn values for the open questions: ink3 on tinted surfaces (Q-DS-04), error colour `r18` as an overridable parameter (Q-DS-05), lens chip `line` outline (Q-DS-09), choice card `edge` (Q-DS-10), 44 dp targets (Q-DS-11), `shadow-float` token for dialogs (Q-DS-18).
- Small glass chips (map pins, person chips, glass round buttons) blur only when a `LocalGlassSource` is provided. Otherwise they draw the SCRIM fill. Contents of bars, sheets and side panels are wrapped in `OnGlass`, so nested glass there falls back to SCRIM.
- No material icons-core glyph matches DS-12: those are filled, while the spec draws outlines at stroke 1.6. All icons are therefore `ImageVector`s built from the DS-12 path data.
- Where the API differs from the spec's notation: the §4 style functions take the theme as an argument (`markerStyle(kind, line, colors)`, `badgeStyle(rating, surface, colors)`, `lensChipStyle(state, colors)`, `buttonStyle(variant, colors)` …). `speakerLabelParts` takes `List<SpeakerMarker>` (kind + solid / dotted line), not `MarkerKind`, and returns `ColorRole` / `TypeRole` per part. `Rating {ALL, R15, R18}`, `Terrain` and `ThemeMode` are types local to this module (it has no core:model dependency), and `resolveTheme` returns `ResolvedTheme`. Search ranges are `IntRange` (`1 until 4`).
- Icon-only actions take an `IconAction(label, onClick)` (`TopGlassBar.back`, `SceneHead.menu`, `ConditionRow.more`, `ImageCard.more`), so they cannot be unlabelled. The labels are owner copy.

## Spec conflicts
- **Caret blink (motion audit S12 vs DS-14).** The audit asks for an "alpha blink, linear". DS-14 says "1 s period, step (on 50 % / off 50 %), linear". The spec wins: the caret keeps the step blink; it is now solid when reduced or with animator scale 0.
- **`stampDrop` easing (D1 vs CNV-37).** The orchestrator's D1 says standard easing; the CNV-37 registry row `도장` says `240 ms ENTER`. The token uses `standardCurve` per D1; map can pass `MotionToken(CivilightMotion.enterCurve, CivilightMotion.stampDrop.durationMs)` if the spec owner prefers ENTER.
- **Card row location (audit §3.2 vs wave-0 task).** The audit puts `SnapCardRow` / `CardViewMotion` in `core:ui`; the wave-0 task (and gallery §3.5 for the toggle) put the motion pieces in DS. They live in `designsystem/card/`; core:ui can re-export or wrap them.
- **Unhide on old APIs.** The audit's table reads as blur + alpha `.55 → 1` for every look. Kept the recorder's tested values: on API < 31 and reduced, opacity runs 0 → 1 (no blur to soften a .55 start).

- **DS-04 compositing vs the contrast table.** The Units convention says alpha tokens are composited with the rounded alpha byte (`floor(a × 255 + 0.5)`). The DS-04 table and T-DS-08 were computed with the declared alpha. With the byte alpha, light `edge` composites to `#868685` (spec `#868686`) and dark `ink3` on `hl` is 4.09 (spec 4.08). Every other value matches either way. The contrast functions composite with the declared alpha, so T-DS-08 and the table pass unchanged. Rendered colours use the byte alpha.
- **DS-62 em-dash regex vs T-DS-24.** The stated regex `\p{L} ?— ?\p{L}` does not match `R-15 — 기본`, because the character before the dash is a digit, yet T-DS-24 expects `EM_DASH_SEPARATOR`. The lint uses `[\p{L}\p{N}] ?— ?[\p{L}\p{N}]`.
- **DS-22 names the Haze library**, which is not in `gradle/libs.versions.toml`. The material is implemented natively (see Defaults).
- **T-DS-19 leaves one case open.** It does not say how the block number shows when the display name equals the real name and the block is selected. `speakerLabelParts` emits `· #043` as the second part.

## Motion helpers (for feature lanes)

Wave 0 of `docs/status/motion-audit.md` (§3.1 / §3.2), built 2026-11-23 by core agent A. Everything is in `core:designsystem`; import from `designsystem.theme` (helpers) and `designsystem.card` (card rows). Feature code never branches on `reduced` for motion: the helpers apply the one reduce rule (§3.1).

**Reduce rule (inside the helpers).** Appear / disappear / swap: opacity only, 120 ms linear. Placement, size, offset, viewport, width: no animation (`null` placement, `snap()`). Colour, fill, alpha: kept, 120 ms linear. Decorative loops (glow, caret blink, parallax, holo sway) stop. Direct manipulation always tracks the finger. "Reduced" = the app setting (CNV-46) **or** the system animator duration scale at 0; `CivilightTheme` reads the scale itself (`rememberAnimatorScale()`, a `ContentObserver`, live) and folds it into both `LocalReducedMotion` and `LocalMotion`. Compose's own animations already follow the system animator scale; hand-rolled waits use `motion.delayMs(ms)`.

**Settings and builders** (`theme/Motion.kt`):

| API | What | Reduced |
| --- | --- | --- |
| `LocalMotion` / `CivilightTheme.motion` → `Motion(reduced, animatorScale, density)` | provided by `CivilightTheme`; constructible outside composition (pass a `Density`) for Nav3 metadata lambdas | – |
| `motion.loops` | may decorative loops run | false |
| `motion.delayMs(ms)` | coroutine `delay` scaled by the animator scale | – |
| `motion.spec<T>(token = standard, delayMs = 0)` | `CivilightMotion.spec` (+ new delay overload `spec(token, reduced, delayMs)`) | 120 ms linear |
| `motion.move<T>(token = standard, delayMs = 0)` | size / offset / width / rect / rotation / viewport (`animate*AsState`, `animateContentSize`, `Animatable.animateTo`) | `snap()` |
| `motion.placement(token = standard)` | `animateItem` placement spec | `null` |
| `motion.enter(token = enter, rise: Dp? = null, expand = false, horizontal = false, delayMs = 0)` | fade + optional slide from `rise` (positive = from below) and expand (from top, or from start when `horizontal`) | fade only |
| `motion.exit(token = exit, rise: Dp? = null, shrink = false, horizontal = false)` | mirror of `enter` | fade only |
| `motion.swap(enter = standard, exit = exit, clip = true)` | `ContentTransform` for `AnimatedContent`, size follows on `standard` | crossfade, size snaps |
| `motion.valueSwap(distance = 8.dp)` | CNV-42 lens value swap: old up 8 dp, new from below, 200 ms | 120 ms crossfade |
| `motion.push(forward = true)` | in-route page push / pop shaped like the shell S1 (slide 1/8 width on `enter`, old fades on `exit`) | crossfade |
| `motion.panelEnter()` / `motion.panelExit()` | CNV-34 second pane / side panel (promoted script `FoldMotion`): 48 dp from the right on `foldOpen`; out fades on `foldClose` | fades |
| `animatedColor(target, token = standard): State<Color>` | `animateColorAsState`; read `.value` in draw lambdas | 120 ms |
| `animatedFraction(on, token = standard): State<Float>` | 0 ↔ 1 (fill amount, overlay, alpha) | 120 ms |
| `animatedAlpha(target, token = standard): State<Float>` | any alpha target (e.g. `.45 ↔ 1` dim) | 120 ms |
| `LazyItemScope.civilightItem(fadeIn = enter, placement = standard, fadeOut = exit): Modifier` (also `LazyGridItemScope`) | `animateItem` with tokens; folds pass `foldOpen` / `foldClose`; null leaves a part out | fades 120, placement `null` |
| `ValueSwap(text, style, modifier, color, maxLines = 1, textAlign, contentAlignment)` | `AnimatedContent(text, valueSwap)`; unchanged text does not animate | crossfade |
| `ContentSwap(state, modifier, contentAlignment, contentKey) { s -> }` | loading / empty / filled / error and any `when` body swap | crossfade |
| `FoldReveal(visible, modifier, horizontal = false) { }` | expand + fade on `foldOpen`, shrink + fade on `foldClose`; keeps content during exit | fade only |
| `FoldRevealValue(value: T?) { t -> }` | `FoldReveal` for nullable data; the last value stays drawn while it folds away | fade only |
| `rememberUnhide(revealed): Unhide` (a `State<Float>` with `look`, `isRunning`) + `Modifier.unhide(unhide)` | CNV-43, promoted recorder `RevealMotion`: blur 7 dp → 0 + alpha .55 → 1 over `unhideMs` on the `enter` curve (API 31+), fade 0 → 1 on older APIs; plays in reverse to re-lock; already revealed at first composition = clear (restore is a cut); wait on `isRunning` instead of `delay` | alpha only, 120 ms |
| `UnhideMotion` (pure: `look`, `durationMs`, `blurDp`, `alpha`) | JVM-testable values | – |
| `rememberLastNonNull(value)` | keeps exiting content composed (banner, toast, panel) | – |
| `MotionRules` (pure) | `effectiveReduced`, `loopsRun`, `scaledMs` | – |

**Tokens added to `CivilightMotion`** (orchestrator D1–D3): `mapReroll` 400, `rowIn` 250, `mapGenerate` 1200, `stampDrop` 240 (all `standardCurve`); `cardParallax = 0.08f`, `cardImageScale = 1.1f`; `recorderGlowSweep` (2400 ms, `easeInOutCurve`), `recorderLevelPeriodMs` 1600, `recorderBandPhaseMs` 230; plus `easeInOutCurve`, `foldPaneShift` 48 dp, `valueSwapShift` 8 dp. D4: `unhideBlur` stays 7 dp everywhere.

**Card rows** (`designsystem/card/`, shared copies of canvas `card/`; the audit named `core:ui`, the wave-0 task put them in DS so cast, library, world, map and the gallery can share them without `core:ui` work):
- `CardRules.kt`: `CardKind`, `CardSize`, `CardMetrics`, `CardSnap`, `SnapTicker`, `CardParallax` (`FACTOR = cardParallax`, `imageScale(reduced)`). `ViewChoice` stays in the lanes (needs `core:model`).
- `SnapCardRow(items, key, kind, inner, modifier, title, showPager, state) { item, imageModifier -> }`: snap fling on `enter` (320 ms, kept when reduced; it continues the finger), one haptic tick per settle (kept when reduced), `civilightItem()` on every card, inset and gap follow a fold on `foldOpen` / `foldClose`. **Signature change from the canvas copy:** the card lambda gets an `imageModifier` instead of `imageShiftDp: Float`; put it on the image layer inside the card's clip. It applies `−offset × 8 %` and the constant 1.1 scale inside `graphicsLayer` (no recomposition while scrolling; none when reduced). `Modifier.cardParallax(state, index, reduced)` is public for custom rows.
- `CardViewMotion` (`plan(reduced)`, `growSpec(motion)`, `transform(motion)`): CNV-45 list ↔ card; size transform is an explicit `SizeTransform(clip = false) { snap() }`.

**MotionLint** (`theme/MotionLint.kt`, runs on the JVM like `CopyLint`). Rules: `TWEEN_LITERAL` (`tween(320)`), `DURATION_LITERAL` (`durationMillis = …` / `delayMillis = …`, non-zero), `CROSSFADE_WITHOUT_SPEC`, `ANIMATE_ITEM_DEFAULT` (`animateItem()`), `DEFAULT_SNAP_FLING` (`rememberSnapFlingBehavior(`), `CUBIC_BEZIER_OUTSIDE_DS`. Comments and strings are ignored; a line with `// motion-lint: allow` (or the line above it) is skipped. Add to each feature's test:

```kotlin
@Test fun `motion lint`() {
    val issues = MotionLint.lintModule(File("."))
    assertEquals(MotionLint.report(issues), 0, issues.size)
}
```

**Component APIs added or changed (all source-compatible: new params are trailing with defaults).**
- `SidePanelHost(visible) { SidePanel(...) }`: second pane / side panel in and out on the fold tokens (S8).
- `TabContent(selectedIndex) { i -> }`: tab bodies crossfade (S18c); use under `SheetTabsHeader` and `SidePanel` tabs.
- `ToastHost(..., id = message)`: a new id swaps the toast (S10).
- `Modifier.pressable(..., pressOverlay = true)`: set false only when the caller draws its own pressed layer.
- `MapVertex(..., popOnAppear = true)`: pass false for vertices that already existed when the screen opened.
- `ViewToggle(mode: ViewToggleMode, onChange)`: gallery three-cell toggle; `IconId.GridTall`.
- `LocalDialogDismiss`: inside a `ScrimDialog`, the animated dismiss (use it for a `취소` in custom dialog bodies).
- Pure helpers for lanes' own drawing: `sheetOpenAmount`, `searchBoxReveal`, `glowAmounts` / `glowSweep`, `vertexPopScale`, `MAP_DIM_ALPHA`.
- Selection visuals (`selectedRowBackground`, chips, segmented, tabs, radio, checkbox, switch, choice cards, step chips, map tools, text-field outlines) now animate inside DS; lanes get S6 for free and must not wrap them in their own colour animations.

**How to migrate (wave 1).**
1. Replace `CivilightTheme.reducedMotion` branches around motion with `val motion = CivilightTheme.motion` and the builders above (`motion.enter(...)`, `motion.swap()`, `motion.move(...)`). Keep `reducedMotion` only for non-motion choices.
2. P1 items: `items(list, key = …) { Row(civilightItem().fillMaxWidth()) … }`; folds pass `civilightItem(fadeIn = CivilightMotion.foldOpen, fadeOut = CivilightMotion.foldClose)`.
3. P2 reveal: `if (x) Block()` → `FoldReveal(x) { Block() }`; nullable data → `FoldRevealValue(data) { Block(it) }`; non-folding blocks → `AnimatedVisibility(v, enter = motion.enter(rise = 8.dp), exit = motion.exit())`.
4. P3 swap: `when (state)` bodies → `ContentSwap(state) { s -> when (s) … }`; wizard steps → `AnimatedContent(step, transitionSpec = { motion.push(targetState > initialState) })`.
5. P4 value: lens- or edit-dependent text → `ValueSwap(text, style, color = …)`.
6. P5 unhide: `val u = rememberUnhide(open)`, then `Modifier.unhide(u)` on the revealed rows; replace `delay(UNBLUR_MS)` with `snapshotFlow { u.isRunning }`.
7. Colours and alpha: `val fill by animatedColor(if (on) colors.sel else colors.tint)`; for fills that appear, draw `fill.copy(alpha = fill.alpha * a)` with `val a by animatedFraction(on)` (a lerp to `Transparent` passes through grey).
8. Delete the local copies: canvas `card/SnapCardRow.kt`, `CardViewMotion.kt` and the model-free part of `CardRules.kt` → `designsystem.card`; script `fold/FoldMotion.kt` → `motion.panelEnter()` / `panelExit()`; recorder `inbox/RevealMotion.kt` → `rememberUnhide` / `Modifier.unhide`; world `ListCardMotion` → `CardViewMotion`.
9. Add the `motion lint` test above and fix what it reports.

## Requests
- **core:ui (wave-0 core agent B)**: `ToastController` should pass a toast id to `ToastHost(id = …)` so two toasts with the same text still swap (S10). `ThemeBridge` may drop its own animator-scale read (S16), since `CivilightTheme` now reads it live; keeping both is harmless. Picker rows (S11) and the lens sheet (S18d) can use `civilightItem()` / `FoldReveal`. S1–S3, S4 `SheetHost`, S8 banners (`CivilightScreen`, `LensEditBanner` call sites), S9, S14–S16 are core:ui / app rows, not built here.
- **Not built in DS (left)**: S17 theme / text-size crossfade (low priority); `ErrorBanner` / `LockRow` enter via `FoldReveal` is a call-site wrap (S11), not a component change.

- **app**: call `enableEdgeToEdge()`. Inside `CivilightTheme`, provide the back hook: `CompositionLocalProvider(LocalBackHandler provides { enabled, onBack -> BackHandler(enabled, onBack) })` (activity-compose). Pass `themeMode` (AppSettings.theme), `textScale = TextScale.forStep(textSizeStep)`, `glassMode = GlassMode.resolve(Build.VERSION.SDK_INT, reduceTransparency)` and `reducedMotion` (CNV-46 motion mode).
- **app**: splash `windowSplashScreenBackground = @color/civilight_bg`, `windowSplashScreenAnimatedIcon = @drawable/civilight_mark`, and window background `@color/civilight_bg` in the app themes (values and values-night). The adaptive launcher icon in `mipmap-anydpi-v26` should use `@color/civilight_launcher_background`, `@drawable/civilight_launcher_foreground` and `@drawable/civilight_launcher_monochrome`, pending Q-DS-13.
- **LIB (설정 › 도움말)**: show `assets/licenses/Pretendard-OFL.txt` (OFL requires the licence to ship with the font).
- **DS-62 repo-wide lint**: an app-level or CI test that runs `CopyLint.lintStringsXml` over every module's `src/main/res/values/strings.xml`. Alternatively, permission for this module's tests to scan the repository.
- **Optional catalog additions**: `pretendard_extrabold.otf` (Q-DS-01; no code change needed beyond a `Font` entry in `Pretendard`), and Haze if the native glass shows problems on devices.
- **FEATURES.md**: update the DS rows from this file (not this module's file to edit).
