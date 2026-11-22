# core/designsystem

Package root `io.github.lenticualrstone.civilight.designsystem` (sub-packages `theme`, `glass`, `foundation`, `icon`, `text`, `component`, `preview`). Build: `tools/gw :core:designsystem:compileDebugKotlin :core:designsystem:testDebugUnitTest` exits 0; 52 JVM tests in 6 classes, all passing (final run with `--rerun-tasks`). Every visual component has a dark and a light preview in `preview/*Previews.kt`. The hosts `ScrimDialog`, `Popover`, `DelayedProgress` and `SheetScrollBody` are previewed through their cards or content. Nothing was run on a device or emulator.

## Done

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

## Partial

- **DS-22 glass material**: `glass/GlassStyle.kt` (`GlassMode.resolve`, `glassStyle`, `SCRIM_ALPHA`) and `glass/GlassMaterial.kt` (`GlassSource`, `Modifier.glassSource`, `GlassBackground` with RenderEffect blur and saturation 1.4 on API 31+, DstIn edge masks, OPAQUE borders, `GlassBudget` for BLUR_COSTLY, `OnGlass` so glass never samples through glass). Test `T-DS-34 glass rendering by mode`. Missing: the blur path has never run, because no device or emulator was allowed. Rendering and GPU cost on API 31–33 are unverified.
- **DS-11 touch targets**: `foundation/HitArea.kt` (`hitArea`, `Modifier.expandedHitArea`) widens the touch area without changing layout; used by every tappable component. Missing: the midpoint split between chips 6–8 dp apart (overlaps go to the later-drawn sibling), and a clipping ancestor cuts the extra area. Test `T-DS-17`.
- **DS-26 popover**: `component/Popover.kt` (`Popover` in a `Popup`, `PopoverCard`, `AnchorBelowPositionProvider`). A `Popup` is its own window and cannot sample the backdrop, so the card uses the SCRIM fill (`raise` 92 %) instead of blurred glass.
- **DS-54 holo**: `component/Holo.kt` `Modifier.holoFoil(tilt, theme, strength)`, `HoloStrength`, `HoloDefaultTilt`. Blend strengths and parallax factors are approximations of the CSS reference; tilt input and sweep are CNV-30's.
- **DS-62 copy lint**: `text/CopyLint.kt` (`lintString`, `CopyLint.lint`, `CopyLint.lintStringsXml`). Tests `CopyLintTest` T-DS-24 … T-DS-28 plus `strings xml lint`. Missing: no test runs it over every module's `values/strings.xml`. Running that from this module's tests would tie this build to other agents' work in progress (see Requests).
- **DS-63 vocabulary**: covered by the mechanical rules in `CopyLint` (삭제, 장면, 복원, 백업에서 가져오기, 미지정, 선택, 변경, 수정, 넘기기, 미리보기, 접힌 화면, 지문으로만, 붙일 곳, 씬 전환 블록), with the allowlist. The review-only rules are not linted: project `제목`, and `받기` used for "apply".
- Not checked on a device: the receding sheet background (DS-23), the expanded hit areas (DS-11) and TalkBack output (DS-64). They have compile checks and previews only.

## Defaults used

- **Glass without Haze.** Haze is not in the version catalog. `GlassSource` records the content into a `GraphicsLayer`. `GlassBackground`, drawn as a sibling above that content, redraws the layer with `BlurEffect(36 dp)` and a 1.4 saturation `ColorMatrix`, then the fill, then a `DstIn` gradient mask. On API < 31, or when no `LocalGlassSource` is present, it draws the SCRIM look. A glass surface must never be a descendant of its own source.
- SCRIM fill = `raise` at alpha `0xEB` (Q-DS-14). In `BLUR_COSTLY`, only the first open sheet or panel blurs; later ones draw SCRIM.
- `TopGlassBar` height = `barHeight` + status-bar inset, with contents 16 / 18 dp below the inset. `BottomGlassBar` min height = 130 / 150 + navigation inset, contents 18 dp above the inset. `GlassScaffold` passes the full bar heights to its content as padding.
- System back for in-composition sheets and panels goes through `LocalBackHandler` (default no-op), because there is no activity-compose here. Dialogs use `Dialog` with the platform dim turned off and our `shade` scrim. Popovers use `Popup`.
- Pressed state: content scale 0.94 for 100 ms (none under reduced motion); ghost buttons add an `ink` 8 % state layer. Disabled: 38 %. Focus: 2 dp `acc` ring 2 dp outside. Error field: 1.5 dp `r18` with a `small` `r18` line.
- Weight 800 roles request `FontWeight.ExtraBold`; until ExtraBold ships, the font matcher renders Bold 700. Korean phrase line breaking is set on every style (Compose applies it on API 33+).
- `TextScale` steps 16 / 17 / 18 / 20 / 22 (LIB-65 ED), default index 2. `paren` line height scales as `round(S × 20 / 18)`. Interface roles do not follow the step.
- Marker vector shapes: diamond as in `DM`, triangle, `▸` filled triangle, `♪` eighth note, `⌁` zig-zag. Inline size 0.75 em in a 1 em advance, centred; speaker-line markers 11 dp. Spoken words: `◆` 변화, `◇` 첫 공개, `▲` 경고. `▸ ♪ ⌁` are dropped from speech.
- Dotted underline dash 2 / 2 dp. Dashed outlines (memo node, deleted row divider) 4 / 3 dp.
- Search and candidate boxes are sized from the Pretendard ascent / descent (0.92 / 0.24 em), not from the line box. Box reveal: 60 ms stagger, then a `Standard` fade.
- Sheet: drag dismiss past 40 % of its height; the centred inner sheet has radius 28 on all corners and max width 560 (DS-23 proposed); top limit status bar + 56 dp.
- Lens chip value change: the old text slides up 8 dp and the new one comes from below (200 ms), with a 1.5 dp `acc` underline sweeping from the left 7 dp above the bottom.
- Toast: rises 8 dp + fade with `Enter`, fades out with `Exit` (CNV-37 lists no toast motion).
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

- **DS-04 compositing vs the contrast table.** The Units convention says alpha tokens are composited with the rounded alpha byte (`floor(a × 255 + 0.5)`). The DS-04 table and T-DS-08 were computed with the declared alpha. With the byte alpha, light `edge` composites to `#868685` (spec `#868686`) and dark `ink3` on `hl` is 4.09 (spec 4.08). Every other value matches either way. The contrast functions composite with the declared alpha, so T-DS-08 and the table pass unchanged. Rendered colours use the byte alpha.
- **DS-62 em-dash regex vs T-DS-24.** The stated regex `\p{L} ?— ?\p{L}` does not match `R-15 — 기본`, because the character before the dash is a digit, yet T-DS-24 expects `EM_DASH_SEPARATOR`. The lint uses `[\p{L}\p{N}] ?— ?[\p{L}\p{N}]`.
- **DS-22 names the Haze library**, which is not in `gradle/libs.versions.toml`. The material is implemented natively (see Defaults).
- **T-DS-19 leaves one case open.** It does not say how the block number shows when the display name equals the real name and the block is selected. `speakerLabelParts` emits `· #043` as the second part.

## Requests

- **app**: call `enableEdgeToEdge()`. Inside `CivilightTheme`, provide the back hook: `CompositionLocalProvider(LocalBackHandler provides { enabled, onBack -> BackHandler(enabled, onBack) })` (activity-compose). Pass `themeMode` (AppSettings.theme), `textScale = TextScale.forStep(textSizeStep)`, `glassMode = GlassMode.resolve(Build.VERSION.SDK_INT, reduceTransparency)` and `reducedMotion` (CNV-46 motion mode).
- **app**: splash `windowSplashScreenBackground = @color/civilight_bg`, `windowSplashScreenAnimatedIcon = @drawable/civilight_mark`, and window background `@color/civilight_bg` in the app themes (values and values-night). The adaptive launcher icon in `mipmap-anydpi-v26` should use `@color/civilight_launcher_background`, `@drawable/civilight_launcher_foreground` and `@drawable/civilight_launcher_monochrome`, pending Q-DS-13.
- **LIB (설정 › 도움말)**: show `assets/licenses/Pretendard-OFL.txt` (OFL requires the licence to ship with the font).
- **DS-62 repo-wide lint**: an app-level or CI test that runs `CopyLint.lintStringsXml` over every module's `src/main/res/values/strings.xml`. Alternatively, permission for this module's tests to scan the repository.
- **Optional catalog additions**: `pretendard_extrabold.otf` (Q-DS-01; no code change needed beyond a `Font` entry in `Pretendard`), and Haze if the native glass shows problems on devices.
- **FEATURES.md**: update the DS rows from this file (not this module's file to edit).
