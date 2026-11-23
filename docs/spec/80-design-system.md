# 80 · 디자인 시스템 (Design system: tokens, type, components, copy) — feature prefix `DS`

Implementation spec for the **design system** of Civilight ("야간 원고" · Night Manuscript) in Jetpack Compose: theme modes and colour tokens, typography and font files, spacing / shape / blur / size tokens, touch targets, icons and the app mark, markers and text decorations, the glass material, and every shared component with its states. It also fixes the UI copy rules and the vocabulary that every other area's strings must follow. Engineers implement from this file alone; every Korean string in `code` is UI copy and must be used verbatim.

**Sources read (2026-11-22).** Design system `design/design-system/project/README.md` (2026-11-22 01:05), `tokens.json` (v2), `components/*/README.md` and `components/*/preview.html`, `components/bundle.css` · generator CSS `design/night-manuscript/gen/gen_nm.py` (`MODES`, `CSS`, `cmdbar`, `lines`, `script_head`, `art`) and `nmlib.py` (`EXTRA`, `head`, `foot`, `sheet`, `li`, `sw`, `tags`, `av`, `rating`, icon paths) · newer generators `scr_new_scene.py`, `scr_new_cast.py`, `scr_new_safe.py`, `scr_new_time.py`, and `scr_core.py`, `scr_states.py`, `scr_decide.py`, `scr_onb.py`, `scr_tools.py`, `scr_cards.py`, `scr_link.py`, `scr_map.py`, `scr_lr.py`, `scr_motion.py`, `scr_inner.py`, `gen_holo2.py` · `boards_text.txt` + `board_index.tsv` · planning doc main tab (live, rev 71; sections 공통 개념, 화면 · 디자인, 카드 보기 · 이미지, 남은 확인) and its local snapshot · confirmed rule drafts `rules/19.md`–`23.md` (19 §3–§4 lock-screen mark, 20 dotted references, 21 candidates) · memory files (`civilight-android-app.md` **font decision 2026-11-22**, `civilight-concept.md`, `civilight-vocabulary.md`, `civilight-rules-19-23.md`, `civilight-rules-blocks.md`, `feedback-*.md`, `civilight-artifacts.md`) · research `2026-11-20-design-research.md` §1 (glass), §4 (foldables), §5 (Korean type) · the bundled font files in `core/designsystem/src/main/res/font/` (inspected with fontTools: version 1.309, `tnum` present, glyph coverage below) and `licenses/Pretendard-OFL.txt` · sibling specs present at writing time: `10-script.md` (SCR), `20-story-time.md` (STO), `30-cast.md` (CST), `40-world.md` (WLD), `50-canvas-pages.md` (CNV), `60-library-security-files.md` (LIB).

**Precedence.** Memory and confirmed rule tabs (newest) > planning main tab (live) > newer board generators (`scr_new_*.py`, then other `scr_*.py`) > `gen_nm.py` / `nmlib.py` / `bundle.css` > `boards_text.txt` > design-system README and component READMEs > `tokens.json` usage notes > research. When a newer source settles an older one this spec follows the newer one and says so in a *Source note*; only unresolved conflicts and real gaps go to section 6.

**Conventions.**
- `[PROPOSED]` marks behaviour or a value that no source states and that this spec had to choose to make the feature implementable. `[PROPOSED]` never introduces Korean copy. Section 4 contains no test for `[PROPOSED]` behaviour.
- **Units.** Mockup boards are CSS px. Read **1 px = 1 dp** for geometry and **1 px = 1 sp** for text size and line height. Board sizes (390 × 844 cover, 800 × 800 inner) are *references*; real layout follows window-size classes (CNV-32).
- **Colours with alpha.** Tokens given as `rgba(r,g,b,a)` are implemented with the listed ARGB literal, e.g. `Color(0x94101114)` for dark `glass`. The alpha byte is `floor(a × 255 + 0.5)` (round half up); every alpha token in this spec lists its resulting ARGB.
- "cover" = cover screen (390 × 844 boards, Compact width). "inner" = unfolded inner screen (800 × 800 boards, Medium/Expanded width).
- Board names drop the `NmDark`/`NmLight` prefix and `.dc.html` (`Script` = `Main` in dark). Every board exists in dark and light.
- Separators in copy are ` · ` (space, U+00B7, space). Markers: `◆` change (`acc`), `◇` first reveal / soft notice (`ink3`), `▲` warning (`warn`), `▸` direction, `♪` BGM, `⌁` author memo. **Dotted = attached to something deleted.**
- Sample project: 「잿빛 새벽의 등불」 (R-15). People 리엔 (인물 기록 007), 하르트 (shown `???` until 1-9), 세라, 오웬, 마르타, 노아 (in the trash in rule-20 examples). Script position 1-7 씬 3 · 2일째 새벽; blocks `#042` 리엔, `#043` 하르트 (◆ 관계 · 리엔의 마음 · 경계), `#044` narration (◇ 오리지늄 등불 첫 공개). Second project 「유리 정원」 (ALL). Hidden R-18 project 「검은 서약」.

**Superseded statements (resolved, not open).**
- **Fonts.** The design system README, `tokens.json`, every board and the sibling specs name *Nanum Myeongjo* (manuscript), *Noto Sans KR* (interface) and *IBM Plex Mono* (numbers). On 2026-11-22 the user chose **Pretendard for everything** ("프리텐다드를 설치해서 써", then "전부 Pretendard": UI, manuscript body and numbers; numbers use tabular figures instead of a mono font). Pretendard v1.3.9 (OFL) Regular · Medium · SemiBold · Bold are bundled. This spec keeps the three *roles* (원고 · 인터페이스 · 숫자) with their sizes, weights and line heights and maps every mockup family to Pretendard (DS-05, DS-06). Any sibling-spec text such as "Nanum Myeongjo 800 22/28" means "Pretendard, role weight 800, 22/28" through the table in DS-06.
- **Checkbox on-state.** `bundle.css` `.check.on` draws an `acc` outline with an `acc` tick; every newer board (`scr_bulk.py`, `scr_new_time.py`, `scr_new_cast.py`, `scr_new_scene.py` `.ck`) draws a **filled `acc` box with a `bg`-coloured tick**. This spec follows the newer boards (DS-32).
- **Component preview copy older than the rules.** `Toast` preview `블록 #043 삭제` → vocabulary `블록 #043 지움`. `LensChip` README `1-5 시점 값 수정 중` → `1-5 시점 값 고치는 중` (README, main tab, board `LensEdit`). `LensChip` preview `시점 · 최신` → rule 11: chip text is `최신 · 1-7` (label text owned by STO). `TravelBar` preview `2.5일` · `약 11시간` → rule 18 never shows `2.5일`; durations read `2일 4시간` / `9시간 40분` (copy owned by STO). `붙일 곳` → `연결` (rule 23).
- **R-18 colour on buttons.** Memory 2026-11-22: the R-18 rating's raise button stays `pri`; `r18` is only for the badge, the R-18 choice cell, and hard-to-undo confirm buttons.
- **Dotted rule.** Rule 20 and the DS README (2026-11-22): names get a dotted underline, `◆`/`◇` marks get a dotted outline, colour unchanged. Same meaning as rule 12's `근거 지워짐`.
- **Shared layout rules (DS-65 … DS-69, user findings 2026-11-23).** They replace: the `Two*` boards' two bottom bars side by side and DS-21 / DS-29 "every screen draws its bar" in `목록 + 상세` (→ DS-65, one bar per frame); DS-09's toast row and DS-27's position "6 dp above the top edge of the bottom glass bar" (→ DS-67 window slot `systemBottom + max(156, chrome + 6)`); DS-23's sheet bottom padding `navigation inset + 16` (→ DS-66 `systemBottom + 24`, 16 above the keyboard); DS-25's floating dialog `bottom 16` without the keyboard (→ DS-66); the popover's 12 dp window margin that ignores the system bars (→ DS-66); LIB-40's `ED` 6 s for every toast (→ DS-67: 2.5 s info, 4 s with action or error); DS-30's 8 % ghost pressed layer (→ DS-68: 6 %, as built); DS-40's settings-row ` ›` text and DS-23's plain-text `닫기` (→ DS-68 cues). BottomGlassBar riding on the keyboard while a content field is typed in (shell core pass, IME item 1) is replaced by DS-69: the bar is not drawn while typing outside the command field.

---

## 1. Scope

### 1.1 What this area owns

1. **Theme**: the two themes (다크 · 라이트), resolution of the theme setting, providing tokens to every composable, system-bar appearance, Material 3 bridging (DS-01).
2. **Colour tokens** for both themes and the theme-independent colours (on-image text, map art, terrain, recorder glow palette, holo reference), and the **contrast rules** with their checks (DS-02 … DS-04).
3. **Typography**: font files and licences, the type roles, text-size scaling hook, Korean typesetting rules (DS-05 … DS-08).
4. **Spacing, layout metrics, shapes, elevation, blur and size tokens**, the 44 dp touch-target rule (DS-09 … DS-11).
5. **Icons** and the **app mark** (manuscript lines + caret), including the lock-screen mark and the launcher icon (DS-12, DS-13).
6. **Motion and haptic tokens** as code constants (DS-14, DS-15). The *registry* of which motion plays where, and every motion's behaviour, is CNV-37 … CNV-46.
7. **Marks and inline decorations**: `◆ ◇ ▲ ▸ ♪ ⌁`, the dotted rule, link / candidate / hidden / search-hit / parenthetical decorations, rating badges (DS-16 … DS-20).
8. **Surfaces**: glass bars, the glass material and its fallbacks, bottom sheets, side panels, dialogs, popovers, toasts, banners, image fade (DS-21 … DS-28).
9. **Controls and navigation pieces**: command bar, buttons, icon buttons and pills, switch / radio / checkbox, segmented controls, slider, text fields, chips, tabs, lens chip, view toggle (DS-29 … DS-39).
10. **Content pieces**: list rows and section labels, lock row, script block typesetting, scene head, swipe button row, search hit, avatar, empty / error / progress blocks, tooltip card, node card (DS-40 … DS-50).
11. **Card, image and map / time components'** visual anatomy: cards, image tile, crop frame, holo material, map tool bar and map marks, map style colours, when picker, condition row, travel bar, recorder glow band (DS-51 … DS-61).
12. **UI copy rules and vocabulary**, enforced by a string lint over all string resources (DS-62, DS-63), and **accessibility semantics** shared by all components (DS-64).
13. **Shared layout rules** from the user's 2026-11-23 findings: one command bar per frame (DS-65), overlay gaps (DS-66), toast timing, anchoring and motion (DS-67, which overrides LIB-40's timing and position), tappable-element cues (DS-68), and keeping the caret line in view (DS-69).
14. **0.2.0 rules** (2026-11-24, section K): icon-button classes and the r2 icon set (DS-70, DS-71), the shared create card (DS-72), the search chip row (DS-73), 3D view primitives and tilt (DS-74), and notes on earlier blocks (DS-75).

### 1.2 Hand-offs to other areas

| Area | What it owns that this spec only references |
| --- | --- |
| 서재 · 보안 · 파일 (`60-library-security-files.md`, LIB) | Settings screens and stored values: `theme`, `textSizeStep` (LIB-65), AppSettings storage; splash → 서재 routing (LIB-08); the 되돌리기 toast host and its messages (LIB-40); rating / hiding / lock flows and their copy (rule 19), lock screens; onboarding screens incl. `OnbLook`; empty / error screen content; export. |
| 캔버스 · 페이지 · 카드 · 펼친 화면 · 모션 (`50-canvas-pages.md`, CNV) | Window-size classes and adaptive frame (CNV-32), unfolded list layouts (CNV-33), fold / unfold (CNV-34), state save (CNV-35), half-fold (CNV-36); motion registry, reduce-motion rules and every motion's behaviour (CNV-37 … CNV-45); `설정 · 움직임 · 투명도` effects and the **glass-mode decision** `glassMode(...)` (CNV-46, tests T-CNV-50, T-CNV-56); card view mechanics, card content and sizes (CNV-21 … CNV-24), image slots, presets, picker, crop editor, holo behaviour (CNV-25 … CNV-31); graph engine and canvas. |
| 스크립트 (`10-script.md`, SCR) | Block kinds and their content, speaker logic and display names in the manuscript, swipe and keyboard behaviour, display layers, block numbers, choice / system-text block rendering details, change-record anchoring. |
| 스토리 · 시점 · 때 · 점검 (`20-story-time.md`, STO) | Lens chip **text** and lens state, the past-lens banner behaviour, the command bar's behaviour and hints, search engine and match ranges, 때 notation strings, travel-check values and suggestion chips, check-center rows. |
| 인물 (`30-cast.md`, CST) | Display-name rule (`???` until 1-9), deleted-reference fix buttons and their copy (rule 20), relationship map content. |
| 세계 (`40-world.md`, WLD) | Term candidates and `잇기 · 아님` logic (rule 21), map editor tools and their behaviour, map generator, places and pins, text → map conditions. |
| 라이프 레코더 (spec by subject) | Recording, processing, `연결`, recorder settings; this spec owns only the glow band's look (DS-61). |

### 1.3 Not built / not shipped

- `components/Cover/` is the design-system **artboard** (a 960 × 300 title card: `Civilight` · `야간 원고 · 원고가 유리 밑으로 흐른다` with four token blocks). It is not an app component and is not shipped.
- The holo **component experiments** (`HoloKit`, `HoloMoment`) are CNV-31 (P2); only the shared holo material is described here (DS-54).
- No Material You dynamic colour, no wallpaper-derived palettes, no ring / donut / conic-gradient decorations (feedback 2026-11-20), no emoji.

### 1.4 needsExternal

No DS feature needs a network service, cloud, ML model, or OS integration beyond a normal app. The Haze blur library, Android SplashScreen and HapticFeedbackConstants are ordinary app dependencies / APIs. **Every DS feature is `needsExternal: no`.**

---

## 2. Features

### A. Theme, colour, contrast

#### DS-01 · 테마 (theme modes and token provider) — P0 · needsExternal: no

"두 테마: 다크 · 라이트. 기본은 기기 설정을 따른다. 토큰 이름은 같고 값만 바뀐다." (DS README 색)

- **Input**: `AppSettings.theme` (owned by LIB-65): `SYSTEM` (`기기 설정`, default) · `LIGHT` (`라이트`) · `DARK` (`다크`).
- **Resolution**: `isDark = theme == DARK || (theme == SYSTEM && systemIsDark)`, where `systemIsDark` is `isSystemInDarkTheme()` (UI mode night). Re-evaluated on every configuration change; a change of the setting applies **live** (LIB-65: "applied live") without restarting the activity `[PROPOSED]`: the root `CivilightTheme` recomposes with the other token set.
- **Provider**: one root composable `CivilightTheme(isDark, content)` provides, through `CompositionLocal`s, `CivilightColors` (DS-02/03), `CivilightType` (DS-06), `CivilightSpacing` (DS-09), `CivilightShapes`, `CivilightSizes`, `CivilightBlur` (DS-10), `CivilightMotion` (DS-14), and the resolved `GlassMode` (computed by CNV-46). Components read tokens only from these locals, never from literals.
- **Material 3 bridge** `[PROPOSED]`, for any M3 component used under the hood (text selection handles, ripple, date pickers): `primary = pri`, `onPrimary = onpri`, `background = bg`, `surface = bg`, `surfaceContainer = raise`, `onBackground = onSurface = ink`, `onSurfaceVariant = ink2`, `outline = edge`, `outlineVariant = line`, `error = r18`, `onError = onr18`, `secondary = tertiary = acc`. Dynamic colour is **off** on every API level (one accent only).
- **Window and system bars** `[PROPOSED]`: edge-to-edge (required at targetSdk ≥ 35); window background = `bg` (no flash between splash and 서재); status-bar and navigation-bar backgrounds transparent; system-bar icons dark in light theme and light in dark theme. The bottom glass bar (DS-21) extends under the navigation bar; its contents sit above the navigation inset.
- **Splash**: SplashScreen background = `bg` of the resolved theme (values / values-night), icon = the mark without plate (DS-13; motion CNV-38).
- Theme swatches in onboarding (`OnbLook`) preview each theme with that theme's own `bg`, `ink` and `acc`; the `기기 설정` swatch is split diagonally light | dark (screen: LIB).

*Sources:* DS README 색; tokens.json `themes`; planning 온보딩 (테마 기기 설정 · 라이트 · 다크); boards `Settings`, `OnbLook`; memory concept (one accent).

#### DS-02 · 색 토큰 (theme colour tokens) — P0 · needsExternal: no

Token names are identical in both themes; only values change. Values are from `gen_nm.py` `MODES` (identical to `tokens.json`).

| Token | Role (Korean name) | Dark | Light | Use |
| --- | --- | --- | --- | --- |
| `bg` | 바탕 | `#0B0C0E` | `#F6F5F1` | page background; manuscript is typeset directly on it |
| `raise` | 올린 면 · 올림 | `#16181C` | `#ECEAE4` | one step up: dialogs, swipe button row, opaque bars when transparency is reduced |
| `ink` | 글자 | `#ECECEF` | `#16171A` | body text, titles |
| `ink2` | 보조 | `#B4B6BD` | `#4A4C52` | secondary text: narration, descriptions, inactive values, `닫기` |
| `ink3` | 약한 글자 | `#8E9099` | `#62646B` | labels, times, meta; the faintest allowed text colour |
| `acc` | 강조 | `#8FB8E6` | `#1F5590` | marks only: caret, `◆`, selection bar, tab underline, current position, focus ring, switch-on track |
| `pri` | 주 버튼 | `#8FB8E6` | `#031C35` | primary button fill, selected segment / chip fill |
| `onpri` | 주 버튼 글자 | `#031C35` | `#F6F5F1` | text and icons on `pri` |
| `sel` | 선택 | `rgba(143,184,230,.12)` = `0x1F8FB8E6` | `rgba(3,28,53,.07)` = `0x12031C35` | selected row / block fill (with a 2 dp `acc` bar at the left) |
| `hl` | 하이라이트 | `rgba(143,184,230,.22)` = `0x388FB8E6` | `rgba(31,85,144,.13)` = `0x211F5590` | search-hit box fill (with 1 dp `acc` outline) |
| `tint` | 옅은 바탕 | `rgba(255,255,255,.07)` = `0x12FFFFFF` | `rgba(22,23,26,.05)` = `0x0D16171A` | input fields, secondary buttons, command bar, lock row, cards |
| `line` | 구분선 | `rgba(255,255,255,.09)` = `0x17FFFFFF` | `rgba(22,23,26,.10)` = `0x1A16171A` | decorative dividers between rows; never a meaningful boundary |
| `edge` | 테두리 | `rgba(255,255,255,.38)` = `0x61FFFFFF` | `rgba(22,23,26,.5)` = `0x8016171A` | meaningful boundaries: input, chip, choice cell, command bar (≥ 3:1) |
| `chip` | 칩 · 표지 바탕 | `#2A2D33` | `#E2E0D9` | avatar fill, small markers' plates, selected filter chip plate |
| `glass` | 유리 | `rgba(16,17,20,.58)` = `0x94101114` | `rgba(246,245,241,.66)` = `0xA8F6F5F1` | glass bars, sheets, side panels (with blur 36) |
| `shade` | 어둠 | `rgba(0,0,0,.45)` = `0x73000000` | `rgba(30,30,28,.18)` = `0x2E1E1E1C` | scrim behind sheets and dialogs |
| `warn` | 경고 | `#F2B66B` | `#8A5A00` | `▲` check warnings only; never `◆` |
| `r18` | R-18 · 지우기 | `#FF6B5E` | `#B3261E` | R-18 badge and choice cell; fill of hard-to-undo confirm buttons |
| `onr18` | r18 위 글자 | `#0B0C0E` | `#FFFFFF` | text on `r18` fill |

**Navy split** (DS README "네이비 나누기"): brand navy `#031C35` (`navy-core`, DS-03) is used only as the light theme's **fill** (`pri`). It is never a mark colour: on light text it would be 1.04:1 and on the dark background 1.14:1. Marks use `acc` (light `#1F5590`, dark `#8FB8E6`); in dark mode both fill and mark are `#8FB8E6` with `#031C35` on top.

**Usage rules** (DS README 원칙 · 대비 규칙):
- Text colours are only `ink`, `ink2`, `ink3`, `acc` (plus `onpri`, `onr18`, `bg` on toasts, and the fixed on-image colours of DS-03). Nothing fainter than `ink3`.
- `acc` is never painted as a large area. Allowed: caret, `◆`, 2 dp selection bar, 2 dp tab underline, 1–1.5 dp focus / selected outlines, switch-on track, small filled checkbox, step-bar current segment, current-position dot / pin.
- `warn` only for `▲` and the `▲` row text; `▲` is always `warn` wherever it appears, and tapping a `▲` opens the 점검 센터 (behaviour: STO).
- `r18` only for: the R-18 badge, the R-18 option in the rating picker, and the fill of confirm buttons of hard-to-undo actions (`휴지통으로` in the delete-project dialog). See §6 Q-DS-05 for the error states drawn in `r18`.
- Meaningful borders (input, chip, choice cell, command bar, lock row, empty-state block) use `edge`; dividers between rows use `line`.

*Sources:* `gen_nm.py` `MODES`; tokens.json; DS README 색, 네이비 나누기, 대비 규칙; board `Accent`; memory (accent change 2026-11-21, R-18 colour 2026-11-22).

#### DS-03 · 고정 색 (theme-independent colours) — P1 · needsExternal: no

These do not follow the theme (or follow it with their own table).

| Name | Value | Use |
| --- | --- | --- |
| `navy-core` | `#031C35` (both) | brand navy; source of light `pri` and dark `onpri`; never a mark |
| `onImage` | `#F4F4F6` | primary text and icons drawn over images (cards, `⋯` on cards) |
| `onImage2` | `rgba(244,244,246,.82)` = `0xD1F4F4F6` | secondary text over images |
| `onImageBadge` | `rgba(244,244,246,.75)` = `0xBFF4F4F6` | ALL / R-15 badge outline + text over images |
| `onImageR18` | `#FF8A80` | R-18 badge outline + text over images |
| `imageShadeTop` / `imageShadeBottom` | `rgba(8,9,12,0)` → `rgba(8,9,12,.86)` = `0xDB08090C` | bottom gradient on cards (62 % of the card height) |
| `imageChipFill` | `rgba(10,11,14,.5)` = `0x800A0B0E` + 12 dp blur | status chip over images (`홀로`, `쓰는 중 · 홀로`) |
| `cropOutline` · `cropGrid` · `cropDim` | `rgba(255,255,255,.9)` · `rgba(255,255,255,.4)` · `rgba(5,6,8,.55)` | crop frame (DS-53) |
| `map-art-1 … 5` | dark `#3B4A66` `#A0674C` `#D9A27F` `#C98A62` `#2B3242`; light `#9DB0CC` `#D9A788` `#EBC7AA` `#C9A07E` `#B8C3D6` | 색면 map fields and profile art placeholder layers; follow the theme (DS-57) |
| `ter-deep` · `ter-sea` · `ter-plain` · `ter-forest` · `ter-hill` · `ter-mount` · `ter-peak` · `ter-desert` · `ter-swamp` · `ter-snow` | `#24394A` · `#33505F` · `#8E9A6A` · `#5E7352` · `#A39466` · `#857E73` · `#D3D7DB` · `#C4AB7A` · `#66705A` · `#C6CED4` (both themes) | hex / polygon terrain (DS-57) |
| recorder glow, dark | `#3E6FA8` `#8FB8E6` `#1F5590` `#B5D3F5` `#5B8CC8` `#2B4C7E` `#9CC3EE` | DS-61 band colours, in this order |
| recorder glow, light | `#5B8CC8` `#1F5590` `#8FB8E6` `#2B4C7E` `#A9CCF2` `#3E6FA8` `#1F5590` | DS-61 |
| holo | silver and tint stops as CNV-30 | DS-54 |
| preset images | 8 palettes of CNV-26 (`scr_cards.py` `PRE`) | CNV-26 |

*Sources:* tokens.json (`navy-core`, `map-art-*`, `ter-*`); `scr_cards.py` (`.cv`, `.shadeb`, `.ct`, `.hchip`, `badge`); DS `CropFrame` preview; `scr_lr.py` `GLOW`; `gen_holo2.py`.

#### DS-04 · 대비 규칙 (contrast rules) — P0 · needsExternal: no

"글자는 바탕 대비 5.4:1 이상, 의미 있는 테두리 3:1 이상." (planning 접근성)

- Every text colour on `bg` ≥ 5.4:1 (the binding case is `ink3`). Meaningful borders (`edge`, composited over `bg`) ≥ 3:1. Glass text is checked against the glass colour composited over `bg` (worst case of the scrolling content is the bar's own fill because the blur averages it).
- **Computed values** (WCAG 2.x relative luminance; alpha tokens composited over `bg` with the rounding of the Units convention):

| Pair | Dark | Light |
| --- | --- | --- |
| `ink` on `bg` · on `raise` | 16.60 · 15.07 | 16.43 · 14.90 |
| `ink2` on `bg` · on `raise` | 9.66 · 8.77 | 7.87 · 7.13 |
| `ink3` on `bg` · on `raise` | 6.15 · 5.59 | 5.42 · 4.91 |
| `acc` on `bg` · on `raise` | 9.47 · 8.60 | 6.97 · 6.32 |
| `warn` on `bg` | 10.88 | 5.43 |
| `r18` on `bg` | 7.00 | 5.99 |
| `edge` (composited `#68686A` / `#868686`) vs `bg` | 3.52 | 3.34 |
| `onpri` on `pri` | 8.32 | 15.76 |
| `onr18` on `r18` | 7.00 | 6.54 |
| toast: `bg` on `ink` | 16.60 | 16.43 |
| `ink3` on `sel` · `hl` · `tint` (composited) | 5.10 · 4.08 · 5.30 | 4.72 · 4.43 · 4.91 |
| `navy-core #031C35` vs light `ink` · vs dark `bg` | — | 1.04 (light ink) · 1.14 (dark bg) |

- The board `Accent` prints these as `1.04:1`, `1.14:1`, `7.0:1` (light `acc`) and `9.5:1` (dark `acc`); the exact light value is 6.97 (see §6 Q-DS-03).
- A JVM test (T-DS-05 … T-DS-11) recomputes this table from the token objects so a token edit that breaks a rule fails the build.
- Pairs below 5.4 for `ink3` on `sel`, `hl`, `tint` and light `raise` are recorded as §6 Q-DS-04; until answered, components keep the drawn colours.

*Sources:* planning 접근성; DS README 대비 규칙; tokens.json usage notes; board `Accent`.

### B. Typography

#### DS-05 · 글꼴 파일과 라이선스 (font files and licences) — P0 · needsExternal: no

- **Shipped family: Pretendard** (user decision 2026-11-22, memory `civilight-android-app.md`). Files in `core/designsystem/src/main/res/font/`:

| File | Weight | Version | Licence |
| --- | --- | --- | --- |
| `pretendard_regular.otf` | 400 | 1.309 (Pretendard v1.3.9) | SIL Open Font License 1.1 |
| `pretendard_medium.otf` | 500 | 1.309 | OFL 1.1 |
| `pretendard_semibold.otf` | 600 | 1.309 | OFL 1.1 |
| `pretendard_bold.otf` | 700 | 1.309 | OFL 1.1 |

- Copyright line to show: `Copyright (c) 2021, Kil Hyung-jin (https://github.com/orioncactus/pretendard), with Reserved Font Name Pretendard.` The full licence text is `licenses/Pretendard-OFL.txt` and must ship inside the APK (e.g. as an asset shown under 설정 › 도움말) `[PROPOSED]` placement; OFL requires the licence to accompany the font. The font files may be bundled and embedded; they may not be sold alone, and a modified font may not use the name "Pretendard".
- **Weight 800** (all `원고` display / title roles in the mockups) has no bundled file. `[PROPOSED]` render 800 roles with Bold 700 until `pretendard_extrabold.otf` (same v1.3.9 OFL package) is added; adding it requires no code change beyond the `FontFamily` entry. §6 Q-DS-01.
- **Not bundled, design reference only**: Nanum Myeongjo (OFL), Noto Sans KR (OFL), IBM Plex Mono (OFL) — the families used by the mockups. They must not be added to the APK.
- **Glyph coverage** (checked in `pretendard_regular.otf`): present `◆ ◇ ▲ ♪ · › ‹ → ← ↔ … ⋯ ‘ ’ “ ” — − × ° •`; **missing `▸` (U+25B8) and `⌁` (U+2301)**. Markers are therefore drawn as vector inline glyphs (DS-16, DS-17), never left to font fallback.
- OpenType features present: `tnum` (tabular figures), `pnum`, `case`, `zero`, `ss01…ss16`, `cv01…cv13`, `frac`. Only `tnum` is used (DS-06).
- `FontFamily(Font(R.font.pretendard_regular, W400), Font(…medium, W500), Font(…semibold, W600), Font(…bold, W700))`; no synthetic bold / italic (`FontSynthesis.None` `[PROPOSED]`), no italics anywhere (no source uses italic).

*Sources:* memory `civilight-android-app.md`; repo `licenses/Pretendard-OFL.txt`; research §5 (Pretendard OFL, first choice for UI and body sans); planning 남은 확인 ("공개 시 글꼴 라이선스").

#### DS-06 · 글자 역할 (type roles) — P0 · needsExternal: no

Three groups from the DS: **원고** (manuscript: dialogue, body, names, titles), **인터페이스** (buttons, tabs, labels, speaker names, parentheticals), **숫자** (times, episode `1-7`, block `#043`, numbers). All three render in **Pretendard**; the 숫자 group adds `fontFeatureSettings = "tnum"`. Sizes in sp, line heights in sp.

**Mapping from mockup families (normative for reading boards and sibling specs):**

| Mockup | Implementation |
| --- | --- |
| Nanum Myeongjo 400 / 700 / 800 | Pretendard 400 / 700 / 800 (800 → 700 until ExtraBold ships, DS-05) |
| Noto Sans KR 400 / 500 / 600 / 700 | Pretendard 400 / 500 / 600 / 700 |
| IBM Plex Mono 400 / 500 / 600 | Pretendard 400 / 500 / 600 + `tnum` |

**Roles** (name → size/line height · weight · letter-spacing · default colour · mockup origin · used for):

| Role | Size / LH | Wt | Tracking | Colour | Origin | Used for |
| --- | --- | --- | --- | --- | --- | --- |
| `hero` | 44 / 54 | 800 | 0 | ink | NM | welcome title `문명의 빛` |
| `display` | 40 / 48 | 800 | −0.01 em | ink | NM (tokens `display`) | character and project name on a record (`리엔`) |
| `displayM` | 36 / 46 | 800 | −0.01 em | ink | NM | project name on 홈 |
| `pageTitle` | 28 / 38 | 800 | 0 | ink | NM | onboarding / new-project step titles |
| `stateTitle` | 26 / 34 | 800 | 0 | ink | NM | full-screen state titles (`마이크 꺼짐`, `되살리는 중`) |
| `title` | 22 / 28 | 800 | 0 | ink | NM (tokens `title`, `.h2`) | sheet and section titles (`화자`, `시점 렌즈`); empty-state titles use 22 / 30 |
| `dialogTitle` | 21 / 28 | 800 | 0 | ink | NM | dialog titles (`‘유리 정원’ 지우기`) |
| `headerTitle` | 19 / 26 (cover) · 21 / 28 (inner) | 700 | 0 | ink | NM | top glass bar title |
| `heading` | 17 / 24 | 700 | 0 | ink | NM (tokens `heading`, `.h3`) | list and card titles (`1-7 씬 3 · 꺼진 등불`) |
| `itemTitle` | 16 / 22 (settings rows 15 / 22) | 700 | 0 | ink | NM (`.li .t b`) | list-row titles, picker names |
| `sceneHead` | 15 / 22 | 700 | 0 | ink | NM | 씬 머리 title |
| `line` | 18 / 31 | 400 | 0 | ink | NM (tokens `line`, `.ln`) | dialogue; manuscript default |
| `body` | 16 / 28 | 400 | 0 | ink (narration: ink2) | NM (tokens `body`) | narration, documents, page text |
| `lead` | 16 / 27 (record) · 15 / 25 (home, onboarding) | 400 | 0 | ink2 | NM | one-line introductions under a title |
| `field` | 16 / 22 | 400 | 0 | ink | NM | text-field values |
| `recorderLive` | 25 / 40 | 400 | 0 | ink (unconfirmed tail ink3) | NM | live transcript |
| `stat` | 20 / 26 | 400 | 0 | ink | Mono | home stats (`7/10`, `1,240`, `12`), `tnum` |
| `command` | 14 / 20 | 400 | 0 | ink3 | Sans (`.cmd`, `.row`) | command-bar hint, field-table rows |
| `label` | 13 / 18 | 500 | 0 | ink2 | Sans (tokens `label`, `.lab`) | `닫기`, row labels, field labels |
| `tab` | 13 / 44 | 500 | 0 | ink3 (on: ink) | Sans (`.tabs`) | tab text (the 44 line height is the hit height) |
| `button` | 13 / 16 | 600 | 0 | onpri | Sans (`.btn`) | buttons; large buttons 15 / 16 |
| `chip` | 12 / 16 | 500 | 0 | ink2 | Sans (`.tag`, `.lens`) | chips, lens chip, swipe buttons |
| `speaker` | 12 / 16 | 600 (name) on 500 line | 0.04 em | name ink2, rest ink3 | Sans (tokens `speaker`, `.who`) | speaker line above a block |
| `small` | 12 / 17 | 400 | 0 | ink3 | Sans (tokens `small`) | meta and description fragments |
| `paren` | 14 / 20 (inline: the line's own line height) | 400 | 0 | ink3 | Sans (tokens `paren`) | `(숨을 삼키며)` inside a line |
| `micro` | 11 / 14 | 500 (status chip 600) | 0 | ink2 | Sans | map tool labels, image status chip, swipe-tab `◆` size |
| `sec` | 11 / 14 | 500 | 0.08 em | ink3 | Mono (tokens `sec`) | section heads (`인물 기록 007 · 시점 1-5`, `Ⅰ 개요`), `tnum` |
| `number` | 12 / 16 | 400 | 0 | ink3 | Mono (tokens `number`) | `05:40 · 1-7 · 18/24`, header small line (11 / 15), `tnum` |
| `dir` | 12 / 16 | 400 | 0 | ink3 | Mono (`.dir`) | direction and memo lines (`▸ 배경 …`, `⌁ 작가 메모 …`), `tnum` |
| `badge` | 10 / 18 | 600 | 0 | see DS-20 | Mono | rating badge, `tnum` |
| `wordmark` | 11 / 16 (splash) · 12 / 16 (welcome) | 400 | 0.3 em (splash) · 0.12 em (welcome) | ink3 | Mono | `CIVILIGHT` |

- Inline `paren` spans keep the surrounding line height (the CSS `line-height: 1` inside an 18 / 31 line); 14 / 20 applies when a parenthetical is shown on its own (tokens.json sample).
- Role names are code names; tokens.json names (`display`, `title`, `heading`, `line`, `body`, `label`, `speaker`, `small`, `paren`, `sec`, `number`) keep their values exactly.
- Line heights are set with `LineHeightStyle(alignment = Center, trim = None)` `[PROPOSED]` so the 44-sp tab line and 31-sp dialogue line match the boards.
- `includeFontPadding = false` `[PROPOSED]` (Compose default on current versions).

*Sources:* tokens.json `type.groups`; `gen_nm.py` CSS; `nmlib.py` `EXTRA`; board generators listed per role; board `Style` (`제목 나눔명조 800` · `대사·본문 나눔명조 18/31` · `화자 · 라벨 Noto Sans KR 12` · `1-7 · 05:40 · IBM Plex Mono`); memory font decision.

#### DS-07 · 글자 크기 적용 (text-size scaling) — P0 · needsExternal: no

- The setting `글자 크기` (default `보통` = `본문 18 · 행간 1.7`) and its steps are LIB-65.
- Effect owned here: the selected step sets the **manuscript base size** `S` (default 18 sp). The `line` role becomes `S / round(S × 31 / 18)`; `body` becomes `round(S × 16 / 18) / round(S × 28 / 18)`; `paren` `round(S × 14 / 18)`; `field` and `recorderLive` scale by `S / 18` `[PROPOSED]`. At `보통` all values equal DS-06 exactly.
- Interface roles (`label`, `chip`, `button`, `small`, `sec` …) do **not** follow the 글자 크기 step `[PROPOSED]`; they follow the system font scale through sp like every role. Fixed-height components (command bar 52, buttons 44, chips 30, tabs 44) grow their height when the system font scale makes the text taller, never clipping text `[PROPOSED]`.
- The onboarding slider (`OnbLook`) previews `“멈추지 마. 저건 등불이 아니야.”` in the `line` role at the chosen size (screen: LIB).

*Sources:* planning 남은 확인 ("글자 크기 18 · 행간 1.7 기본"); boards `Settings`, `OnbLook`; LIB-65.

#### DS-08 · 한글 조판 (Korean typesetting rules) — P1 · needsExternal: no

- **Line breaking**: Korean text breaks between words (어절), never inside a word: `LineBreak.Paragraph` with `LineBreakConfig` word style **phrase** on API ≥ 33 (`TextStyle.lineBreak = LineBreak(strategy = HighQuality, strictness = Strict, wordBreak = Phrase)`); on API 29–32 `wordBreak = Default` (system behaviour) `[PROPOSED]`.
- **Tracking**: 0 everywhere except the roles that state it (display −0.01 em, speaker 0.04 em, sec 0.08 em, wordmark 0.12–0.3 em). Research §5: "자간 0(큰 제목만 −1~−2%)".
- **Line height**: manuscript ≈ 1.7 (18 / 31, 16 / 28), UI 1.3–1.5.
- **Numbers**: all `숫자` roles use `tnum` so `05:40`, `18 / 24`, `#043` columns align; numbers inside manuscript prose keep proportional figures.
- **Reading width**: manuscript columns never exceed `measure` 560 dp (DS-09); on inner screens the reading column is 560 (research: about 30–40 Korean characters per line).
- **Hangul normalisation**: text is rendered as stored (NFC, owned by the file format); no case transforms.
- **Ellipsis**: single-line titles and hints truncate with `…` at the end (`TextOverflow.Ellipsis`), e.g. the command-bar hint and recent-file titles.
- **Quotes**: Korean curly quotes `“ ”` / `‘ ’` as typed in copy; the app never converts straight quotes in user text.

*Sources:* research §5 (조판, LineBreakConfig PHRASE); DS README 글꼴; `gen_nm.py` `cmdbar` (ellipsis); `scr_cards.py` recent-file title.

### C. Spacing, shape, size, touch, icons, mark, motion

#### DS-09 · 간격과 배치 (spacing and layout metrics) — P0 · needsExternal: no

**Spacing tokens** (tokens.json):

| Token | dp | Use |
| --- | --- | --- |
| `space-1` | 4 | icon ↔ text |
| `space-2` | 8 | between fragments, between stacked bars / buttons |
| `space-3` | 12 | inside bars, command-bar side insets |
| `space-4` | 16 | inside sheets and cards |
| `space-5` | 20 | screen side gutter for settings and lists; sheet side padding |
| `space-6` | 24 | manuscript side gutter |
| `space-7` | 26 | gap between manuscript blocks (the only separation between blocks) |

**Layout metrics** (references from the boards; real sizes follow CNV-32):

| Metric | Cover (390) | Inner (800) |
| --- | --- | --- |
| Top glass bar height | 112 (lists, `head()`); 118 (script) | 112 (script `Wide`); 96 (script with panel tabs `wide()`) |
| Top bar contents | left 20, right 12–16, top 16; back button 44 with −12 left offset | left 32, top 18 |
| Bottom glass bar height | 130 (`foot()`); 150 when a row sits above the command bar (script speaker row, profile tabs) | 150, spans only the manuscript column on the script |
| Command bar insets | left / right 12, bottom 18, rows 8 apart | script: left 24, right 314 (panel 290 + 24), bottom 20 |
| Content top | 104–110 below the top of the window | 96–112 |
| Manuscript column | full width, gutters 24 | x 40, width 560 (`measure`) |
| Side panel | — (bottom sheet) | width 290, padding 20 top · 22 sides, inset left 1 dp `line` |
| List + detail | — | list 390 · 1 dp `line` · detail 409 (CNV-33) |
| Single column | — | 560 centred, margins `raise` 50 % (CNV-33) |
| Bottom sheet padding | 10 top · 20 sides (picker 16) | — |
| Toast position | left / right 16, 6 dp above the bottom glass bar (script: bottom 156) | same, within the manuscript column `[PROPOSED]` |

- No tappable element within the fold / hinge area (CNV-32).
- Lists of rows have no outer card; rows separate with a 1 dp `line` top border (DS-40).

*Sources:* tokens.json `spacing`, `size.measure`; `nmlib.py` `head`, `foot`, `content`, `sheet`; `gen_nm.py` `cmdbar`, `script_head`, Wide; `scr_core.py` `wide`; `scr_inner.py`; `scr_states.py` `undo_toast`.

#### DS-10 · 모서리 · 그림자 · 흐림 · 크기 (shape, elevation, blur and size tokens) — P0 · needsExternal: no

**Radius** (tokens.json, plus radii the boards use for specific components):

| Token | dp | Use |
| --- | --- | --- |
| `radius-xs` | 5 | rating badge |
| `radius-sm` | 12 | inputs, segmented controls, keyboard tools, list-row selection |
| `radius-cmd` (observed) | 14 | command bar (`.cmd`), export option rows, panel cards, map tool cells |
| `radius-md` | 16 | cards in panels, lock row, dialogs' inner cards, toast, popover, banner, choice cards |
| `radius-card` (observed) | 20 | image cards, empty-state block, projectcards |
| `radius-dialog` (observed) | 26 | centred dialog (`ConfirmDelete`) |
| `radius-lg` | 28 | sheet top corners, bottom floating dialog (`ErrBio`) |
| `radius-full` | 9999 | pill buttons, chips, avatars, mic |

**Elevation**: glass and manuscript have **no shadow**. `shadow-float` (dialogs, tooltips): dark `0 20 50 rgba(0,0,0,.45)`, light `0 20 50 rgba(30,30,28,.18)` — in Compose a 50 dp-blur shadow offset 20 dp; implement with `Modifier.dropShadow` / `graphicsLayer` shadow of that colour `[PROPOSED]` mechanism. Popovers use `0 10 28 shade` (DS-26).

**Blur** (tokens.json `blur`): `blur-glass` 36 dp (bars, sheets, side panels; with saturation × 1.4); `blur-edge` 22 dp (art fading downward, DS-28); `blur-back` 2 dp (screen behind a sheet). Other blurs used by components: 12 dp (small glass chips on maps and images), 7 dp (hidden content before unlock, CNV-43), 6 dp (map reveal), 34 dp (recorder glow, DS-61).

**Sizes** (tokens.json `size`): `hit-min` 44 · `cmd-h` 52 · `measure` 560 · `card-w` 222 (project card width, 3:4) · `map-tool` 52 · `vertex` 14. Component sizes: icon 20 (also 14, 16, 18, 22, 24, 28, 30, 48 where stated), avatar 32 / 36 / 38, mic button 32, switch 46 × 28, radio / checkbox 22, grab handle 36 × 4.

**Layer order** (z): content 0 → glass bars 2 → bar contents 3 → scrim (`shade`) 3 → sheet / dialog / panel 4 → toast 5 → tour dim 7 → tooltip 9.

*Sources:* tokens.json `radius`, `shadow`, `blur`, `size`; `bundle.css`; `nmlib.py`; `scr_states.py`; `scr_decide.py` `tip`.

#### DS-11 · 누르는 범위 (touch targets ≥ 44 dp) — P0 · needsExternal: no

"모든 누르는 것은 실제 범위 44px 이상(`hit-min`). 작게 보이는 칩 · 토글 · 라디오는 `::after`로 넓힌다." (DS README 누르는 범위)

- Every tappable element has a touch area ≥ 44 × 44 dp; the drawn shape may be smaller.
- Expansion rules (from `bundle.css` `::after`):
  - chips (`.tag`), lens chip (`.lens`), switch (`.sw`), any small tappable (`.hit`, e.g. the 32 dp mic, inline search boxes): touch area `width = max(w + 8, 44)`, `height = max(h, 44)`, centred on the drawn shape.
  - radio, checkbox, map vertex (`.vx`): fixed 44 × 44 centred.
  - lock toggle `.lk` (36 × 36): expand by 4 dp on every side (44 × 44).
  - icon buttons (`.ib`) and buttons are already ≥ 44 tall.
- Compose: `Modifier.minimumInteractiveComponentSize()` is not enough for the `w + 8` rule; use a custom `Modifier.hitArea(minWidth = max(w + 8, 44), minHeight = 44)` that extends the pointer-input bounds without changing layout `[PROPOSED]` mechanism.
- Neighbouring touch areas must not overlap; where chips sit 6–8 dp apart the extra width is split at the midpoint `[PROPOSED]`.
- A whole row (checkbox row, radio row, settings row, choice card) is one touch target; the control inside is not a separate target (`scr_new_safe.py` `crow`: "whole row is the target").
- Research mentions a 48 dp Tier-2 target for large screens; the app keeps 44 (§6 Q-DS-11).

*Sources:* DS README 누르는 범위; tokens.json `hit-min`; `bundle.css`; memory audit fixes 2026-11-21; `scr_new_safe.py`.

#### DS-12 · 아이콘 (icon set) — P0 · needsExternal: no

**(0.2.0)** The r2 set (DS-71) adds ids and replaces the paths of `Check`, `Search`, `X`, `List`, `File`, `Split` and `Merge`; where a button shows only an icon or an icon with text is DS-70.

- Style: outline icons on a 24 × 24 grid, `stroke = currentColor`, stroke width **1.6** (in grid units, so it scales with the drawn size: 20 dp icon → 1.33 dp), round caps and joins, no fill. Default drawn size 20 dp; default colour `ink2` in icon buttons. No filled or two-tone icons; no circles as decoration.
- Implement each as an `ImageVector` from the SVG path data below. Names are code names; the Korean label is the accessibility label used on the boards where one exists.

| Id | Path data | Where |
| --- | --- | --- |
| `Back` | `M15 5l-7 7 7 7` | header back |
| `Next` (chevron) | `M9 5l7 7-7 7` | row chevron, `이어 쓰기` |
| `More` | `M5 12h.01M12 12h.01M19 12h.01` | `⋯` menus |
| `Plus` | `M12 5v14M5 12h14` | add |
| `X` | `M6 6l12 12M18 6L6 18` | remove chip |
| `Check` | `M5 12l4 4 10-10` | checkbox tick, done |
| `Grip` | `M9 6h.01M15 6h.01M9 12h.01M15 12h.01M9 18h.01M15 18h.01` | drag handle (`끌어서 순서`) |
| `Filter` | `M4 6h16M7 12h10M10 18h4` | filter |
| `Search` | `M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14zM20 20l-4-4` | search field (18 dp) |
| `Mic` | `M12 4a3 3 0 0 1 3 3v5a3 3 0 0 1-6 0V7a3 3 0 0 1 3-3zM6 11a6 6 0 0 0 12 0M12 17v3` | `라이프 레코더` (16 dp in the command bar) |
| `Text` | `M5 6h14M12 6v12M9 18h6` | `글로 쓰기` |
| `Pause` | `M9 6v12M15 6v12` | recorder |
| `Layers` | `M12 4l9 5-9 5-9-5zM3 14l9 5 9-5` | `연출 · 메모 표시` (display layers), map `레이어` |
| `Link` | `M10 14l4-4M8.5 15.5l-1 1a3 3 0 0 1-4-4l3-3a3 3 0 0 1 4 0M15.5 8.5l1-1a3 3 0 0 1 4 4l-3 3a3 3 0 0 1-4 0` | `연결` chips |
| `Lock` | `M7 11V8a5 5 0 0 1 10 0v3M6 11h12v9H6zM12 15v2` | hidden groups |
| `Unlock` | `M7 11V8a5 5 0 0 1 9.6-2M6 11h12v9H6zM12 15v2` | unlocked group |
| `Fingerprint` | `M7 18c1-2 1.5-4 1.5-6a3.5 3.5 0 0 1 7 0c0 1.2-.1 2.3-.3 3.4M12 12c0 3-.6 5.6-1.8 8M4.5 14c.3-.8.5-1.6.5-2.5a7 7 0 0 1 14 0c0 2-.2 3.9-.7 5.7M14.8 19.5c.3-.8.5-1.6.6-2.5` | biometric buttons (30 dp), error (48 dp) |
| `EyeOff` | `M3 3l18 18M10.6 6.1A9.8 9.8 0 0 1 12 6c6 0 9.5 6 9.5 6a16 16 0 0 1-2.7 3.4M6.4 7.6A16 16 0 0 0 2.5 12s3.5 6 9.5 6a9.6 9.6 0 0 0 4.2-1M10 10a3 3 0 0 0 4 4` | `숨기기` |
| `Warn` | `M12 4l9 16H3zM12 10v4M12 17h.01` | error banners |
| `Trash` | `M5 7h14M10 7V5h4v2M7 7l1 12h8l1-12` | 휴지통 |
| `Undo` | `M9 14L4 9l5-5M4 9h10a6 6 0 0 1 0 12h-3` | `되돌리기` (toast, 16 dp) |
| `UndoMap` | `M9 7L4 12l5 5M4 12h11a5 5 0 0 1 0 10h-2` | map `되돌리기` (variant kept as drawn) |
| `Cloud` | `M7 18h10a4 4 0 0 0 .5-8A6 6 0 0 0 6 11a3.5 3.5 0 0 0 1 7z` | backup |
| `Drive` | `M8 4h8l5 9-4 7H7l-4-7zM8 4l9 16M16 4L7 20M3 13h18` | Drive row |
| `File` | `M7 3h7l5 5v13H7zM14 3v5h5` | file rows (variant `M6 3h8l4 4v14H6zM14 3v4h4` in `scr_cards.py` is replaced by this one) |
| `Folder` | `M3 7h7l2 2h9v10H3z` | file picker |
| `Image` | `M4 5h16v14H4zM4 16l5-5 4 4 3-3 4 4` | `이미지 고르기` |
| `Grid` / `List` | `M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z` / `M4 6h16M4 12h16M4 18h16` | ViewToggle `카드` / `목록` |
| `Rotate` · `Flip` · `Reset` | `M4 12a8 8 0 1 0 3-6.2M4 4v4h4` · `M12 3v18M8 7l-4 5 4 5M16 7l4 5-4 5` · `M4 4v6h6M4.5 15a8 8 0 1 0 2-8.5L4 9` | crop tools |
| `Pin` | `M12 21s-6-5.5-6-10a6 6 0 0 1 12 0c0 4.5-6 10-6 10z` | map pin (24 dp), map tool `핀` |
| `Move` | `M12 3v18M3 12h18M8 7l4-4 4 4M8 17l4 4 4-4M7 8l-4 4 4 4M17 8l4 4-4 4` | map tool `이동` (the DS preview uses the simpler `M12 3v18M3 12h18`) |
| `Area` · `Route` · `Label` | `M5 7l7-3 7 4-1 9-8 3-6-6z` · `M5 19c4 0 3-7 7-7s3-7 7-7` · `M5 6h14M12 6v13` | map tools `영역` · `경로` · `라벨` |
| `Stamp` · `Brush` · `Fill` · `Select` · `Node` · `Line` | `M9 4h6l-1 7h4v4H6v-4h4zM5 20h14` · `M4 20c3 0 4-2 4-4l9-9 3 3-9 9c-2 0-4 1-4 4z` · `M5 12l7-7 7 7-7 7zM19 15c0 2 1 3 1 4a1 1 0 0 1-2 0c0-1 1-2 1-4` · `M4 4h6M14 4h6M4 20h6M14 20h6M4 4v6M4 14v6M20 4v6M20 14v6` · `M4 6h6v5H4zM14 13h6v5h-6zM10 8.5h2v7h2` · `M4 18L20 6` | map / canvas tools |
| `Dice` · `Camera` · `Wave` · `Book` | `M4 4h16v16H4zM9 9h.01M15 15h.01M15 9h.01M9 15h.01` · `M4 8h4l2-3h4l2 3h4v11H4zM12 17a4 4 0 1 0 0-8 4 4 0 0 0 0 8z` · `M3 12c3-4 6 4 9 0s6 4 9 0` · `M5 4h11a3 3 0 0 1 3 3v13H8a3 3 0 0 1-3-3zM5 17a3 3 0 0 1 3-3h11` | seed, sketch photo, voice record, pages |
| `Merge` · `Split` | `M6 6l6 6-6 6M18 6l-6 6 6 6` · `M12 4v16M6 8l-3 4 3 4M18 8l3 4-3 4` | merge / split |
| `Paper` (참고) · `Props` (속성) | `M5 5h14v10h-8l-4 3.5V15H5z` · `M4 7h16M4 12h16M4 17h10` | swipe buttons `참고` · `속성` (18 dp) |
| `StepLock` | `M6 11h12v9H6zM8 11V8a4 4 0 0 1 8 0v3` | map generator step `고정` (distinct from `Lock`) |

- `Check` has a second variant `M5 12l5 5 9-10` in `scr_new_cast.py`; the canonical one above replaces it. The search icon in `gen_nm.py` (`M10.5 17a6.5 6.5 0 1 0 0-13 …`) is replaced by `Search`.

*Sources:* `nmlib.py` icon constants and `.ico` CSS; icon constants of every `scr_*.py`; `gen_nm.py` (`LAYERS`, `BACK`, `SEARCH`, swipe icons); DS `MapToolbar` preview.

#### DS-13 · 원고 줄 표식 (app mark, lock mark, launcher icon) — P1 · needsExternal: no

"스플래시: 아이콘 판 없이 로고만" — the mark is three manuscript lines and a typing caret. No plate, no circle.

- **Mark, 96-unit box** (`scr_motion.py` `mark`): three bars, x 12, y 22 / 44 / 66, widths 72 / 60 / 40, height 6, corner radius 3, colour `ink`; caret x 58, y 62, 4 × 16, radius 2, colour `acc`. Scales uniformly.
- **Static mark, 64-unit box** (`scr_new_safe.py` `static_mark`, app lock `scr_states.py`): bars x 8, y 14 / 28 / 42, widths 48 / 40 / 26, height 5, radius 3, `ink`; caret x 38, y 40, 3 × 12, `acc`. Drawn at 64 dp (app lock) and 72 dp (lock-screen mark only, `RHLockMark`).
- **Uses**: splash icon (CNV-38: bars draw left → right, mark rises; reduce → fade), app lock screen (`AppLock`: mark + `Civilight` 22 / 30 800 + small `잠김 · 지문으로 열기`), mark-only lock screen for hidden / capture-blocked projects (rule 19 §4, `RHLockMark`: `bg` only + mark + `숨긴 프로젝트` + small `잠김 · 지문으로 열기`; screens and copy LIB), splash wordmark `CIVILIGHT` (DS-06 `wordmark`).
- **Launcher icon** `[PROPOSED]` (no source draws one): adaptive icon, background `#0B0C0E` (dark `bg`), foreground = the 96-unit mark with dark-theme colours (`#ECECEF` bars, `#8FB8E6` caret) scaled so the mark fits the 66 dp safe zone of the 108 dp canvas; monochrome (themed) icon = the three bars and caret in one colour. §6 Q-DS-13.
- Biometric button next to the mark: 64 × 64, radius 20, `pri` fill, `Fingerprint` 30 `onpri`; secondary text button `기기 PIN 사용` / `PIN 사용` (copy LIB).

*Sources:* planning 움직임 (스플래시); boards `Splash`, `SplashStrip`, `AppLock`, `RHLockMark`; rule 19 §4; `scr_motion.py`, `scr_states.py`, `scr_new_safe.py`.

#### DS-14 · 모션 토큰 (motion tokens) — P1 · needsExternal: no

Code constants in `CivilightMotion`; the registry of where each is used, reduce-motion behaviour and every motion's choreography are CNV-37 … CNV-46 (values below are identical to CNV-37).

| Token | Curve | Duration | Use |
| --- | --- | --- | --- |
| `Standard` (`표준`) | `CubicBezierEasing(0.2f, 0f, 0f, 1f)` | 200 ms | taps, value changes |
| `Enter` (`감속 (등장)`) | `CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)` | 320 ms | sheets, block add, appearing |
| `Exit` (`가속 (퇴장)`) | `CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)` | 160 ms | closing, deleting |
| `Linear` (`선형`) | `LinearEasing` | — | caret blink and recorder glow only |
| `Reduced` | linear opacity fade | 120 ms | replaces every movement when motion is reduced |
| `FoldOpen` / `FoldClose` | `Enter` / `Exit` curves | 360 / 240 ms | CNV-34 |
| `EnterLong` | `Enter` curve | 360 ms | list ↔ card, picker → crop, sub-map zoom |
| `Swipe` | `Standard` | 260 ms, buttons staggered 60 ms | CNV-40 |
| `HoloSweep` | `Enter` curve | 480 ms | CNV-45 |
| `SheetBack` | scale 0.965 + blur 2 dp | with `Enter` | CNV-41 |
| `Unhide` | blur 7 dp → 0, opacity .55 → 1 | 320 ms | CNV-43 |

- Caret blink: 1 s period, step (on 50 % / off 50 %), linear.
- What never moves: the line being typed, the caret position, glass bars during scroll (CNV-37).

*Sources:* DS README 재질과 움직임; board `MotionSpec`; `scr_motion.py` (`STD`, `DEC`, `ACC`, keyframes); CNV-37.

#### DS-15 · 햅틱 (haptic tokens) — P1 · needsExternal: no

- Two haptic tokens only, used exactly where CNV-37's registry says `틱` or `확인`:
  - `틱` (tick): region vertex placed, route segment, stamp rotation detent, card-row snap → `HapticFeedbackConstants.CLOCK_TICK` `[PROPOSED]` mapping (`SEGMENT_TICK` on API ≥ 34 `[PROPOSED]`).
  - `확인` (confirm): entering a sub-map → `HapticFeedbackConstants.CONFIRM` on API ≥ 30, `VIRTUAL_KEY` below `[PROPOSED]` mapping.
- Haptics respect the system haptic setting (`View.performHapticFeedback` default flags). No other haptics (research §5: "중요한 순간에만").

*Sources:* planning 지도 · 카드 · 이미지 모션 table (햅틱 column); board `MotionSpec2`; research §5 모션·햅틱.

### D. Marks and inline decorations

#### DS-16 · 표식 ◆ ◇ ▲ (change, first reveal, warning) — P0 · needsExternal: no

"표식 셋은 모양과 색을 함께 바꾼다: ◆ 변화 `acc` · ◇ 첫 공개 `ink3` · ▲ 경고 `warn`(호박색). ▲는 어디서나 `warn` 색이고, 누르면 점검 센터로 간다." (DS README)

| Marker | Kind | Shape | Colour | Meaning | Typical copy |
| --- | --- | --- | --- | --- | --- |
| `◆` | `CHANGE` | filled diamond | `acc` | a change record (any field, relation, display name) at this point | `◆ 변화`, `◆ 관계 · 리엔의 마음 · 1-7 #043부터`, `소속 · 등불 결사 · 1-3부터 ◆` |
| `◇` | `NOTICE` | outline diamond | `ink3` | first reveal of a term; soft notices (deleted reference, no place, order/time mismatch, candidate before reveal) | `◇ 첫 공개`, `◇ #044 용어 첫 공개 · 오리지늄 등불`, `◇ 장소 없음` |
| `▲` | `WARNING` | filled triangle | `warn` | a check warning; tapping opens the 점검 센터 | `▲ 공개 전 사용`, `▲ 화자 없음`, `▲ 이동 시간 부족` |

- Shape **and** colour change together; a marker is never told apart by colour alone.
- Rendering: vector glyphs (not font glyphs) so the solid and dotted variants match on every device `[PROPOSED]` (DS-05 glyph coverage). Size = 0.75 × the surrounding font size, baseline-aligned like a text glyph, advance = glyph width + 0.25 em `[PROPOSED]`. In the speaker line the marker sits at the far right (`margin-left: auto`); in rows it follows the value after one space; in swipe buttons it is 11 sp above the label.
- Copy strings keep the Unicode characters (`◆`, `◇`, `▲`) so text, search and accessibility see them; a `MarkedText` renderer replaces exactly these characters with the inline vectors.
- Accessibility names `[PROPOSED]` (spoken instead of the symbol): `◆` → `변화`, `◇` → `첫 공개` (or the following label), `▲` → `경고`. These words already exist in the board copy (`◆ 변화`, `◇ 첫 공개`).
- The `▲` tap target follows DS-11; tapping navigates to the check center filtered to that item (behaviour STO / CNV-17).
- Display layer `변화 표식` (SCR display layers) hides `◆` and `◇` in the script view only; `▲` is never hidden by display layers.

*Sources:* DS README 대비 규칙; planning 공통 개념 표식; boards `Script`, `Wide`, `Style`, `SearchHit` preview, `Profile`, `TravelCheck`; memory (warning colour 2026-11-21).

#### DS-17 · 연출 기호와 기호 목록 (▸ ♪ ⌁ and the allowed symbol set) — P0 · needsExternal: no

"이모지 없음. 기호는 정해진 것만: ◆ ◇ ▲ ▸ ♪ ⌁." (DS README 문구)

| Symbol | Meaning | Where | Colour |
| --- | --- | --- | --- |
| `▸` | direction cue (`▸ 배경 하층 거리(새벽)`) | direction lines (`dir` role) | `ink3` |
| `♪` | BGM (`♪ 잔불`) | direction lines | `ink3` |
| `⌁` | author memo, excluded from game output (`⌁ 작가 메모 여기서 등불이 두 번째로 깜박이게 할지`) | memo lines; recorder memo blocks (rule 23) | `ink3` |

- `▸` and `⌁` are **missing from Pretendard**; all six markers are vector glyphs (DS-16). Vector shapes `[PROPOSED]`: `▸` a right-pointing filled triangle in a 0.6 em box; `♪` an eighth note; `⌁` a horizontal zig-zag bolt.
- **Other punctuation allowed in UI copy** (all present in Pretendard): `·` separator, `›` (row value / breadcrumb / 다음), `‹` (`‹ 서재`), `→` `←` `↔` (value changes and directions: `1-1 없음 → 1-3 등불 결사`, `R-15 → R-18`), `…` and `⋯` (cut-off words, `⋯` menu label), `‘ ’ “ ”` quotes, `#` block numbers, `@` links, `/` commands and fractions (`/대`, `1/4`, `18 / 24`), `+` add (`+ 연결`, `+ 항목`), `−`, `~` ranges (`1-1 ~ 1-10`), `–` (spaced en dash in time ranges: `scr_link.py` draws `9시간 40분 – 12:40`, STO normalises it to `9시간 40분 – 12시간 40분` per rule 18), `%`, `°`, `×`, `?` (`???` display names), `( )` `[ ]` (`[기록]` system-text example), `:` `.` `,`, `—` only as a stand-alone "empty value" glyph (episode name `—`, block `#—`, table cells), and `•` only for password dots.
- Not allowed: emoji (any emoji-presentation code point), `✓ ★ ● ○ ■ □ ◎`, decorative bullets. Ticks are the `Check` icon, not a character.

*Sources:* DS README 문구, 원고 블록; `gen_nm.py` `lines`; symbol inventory of `boards_text.txt`; font coverage check.

#### DS-18 · 점선 = 지운 것에 걸림 (dotted = attached to something deleted) — P1 · needsExternal: no

"점선 = 지운 것에 걸린 표시. 이름 · 글자는 점선 밑줄(‘지운 인물 · 노아’), ◆ · ◇ 마크는 점선 테두리(근거 블록이 지워진 기록). 색은 원래 표식 색 그대로." (DS README; rule 20; rule 12 `근거 지워짐`)

- **Names and linked text** that point to a deleted item: same font, weight and colour as they would have (`ink2` in speaker lines and narrow slots, `ink3` in secondary lists), plus a **dotted underline**: dot 2 dp on / 2 dp off `[PROPOSED]` pattern, thickness 1 dp, offset 4 dp below the baseline, colour = the text colour. Followed by `◇` in `ink3` in narrow slots (speaker slot, @ chip, 등장 line): `노아 ◇`. Wide slots (panel, card, check center) spell it out: `지운 인물 · 노아`, `지운 장소 · 잿더미 시장`, `지운 용어 · 오리지늄 등불`, `지운 세력 · 상층 경비대` (copy CST / WLD).
- **Marks** whose anchor or target is deleted: the same marker drawn as an **outline with a dashed stroke**, in its own colour. Reference geometry (`scr_new_scene.py` `DM`): 14 × 14 box, diamond path `M7 1.5L12.5 7 7 12.5 1.5 7z`, no fill, stroke 1.5, dash 2 on / 1.8 off, colour `acc`; accessibility label `근거 지워짐`. A dotted `◇` uses the same dashed outline in `ink3`; rule 20 calls the target-deleted case `대상 지워짐`.
- **Rows** for a deleted counterpart (e.g. the last row in a relation list): a 1 dp **dashed** top border in `line` and the dotted name (`scr_cast.py`: `노아` · `◇ 지운 인물 · 관계 1` · `되살리기 · 이대로 두기`).
- **Map**: a route whose end pin is deleted keeps a dashed end segment (rule 20; drawing WLD).
- Restoring the item turns everything back to solid (rule 20 "실선으로"). Permanent deletion keeps the dotted look with `영구 삭제 · 노아` (copy CST).
- Dotted never means anything else. Other dashed lines in the boards are separate signs with their own meaning: the 집필 선 marker `시점 1-7 · 이 아래는 아직` (1 dp dashed `acc`), 고정점 rows in cascade previews (1 dp dashed `ink3`), memo nodes on canvas (dashed `line` outline), relation lines with only 마음 (dashed, CST). Their owners define them; none of them uses a dotted **underline** or a dotted **marker**.

*Sources:* DS README 대비 규칙; rule 20 §1–§4 (`rules/20.md`); memory rules-19-23 (2026-11-22 dotted decision), rules-blocks; boards `RSOrphan`, `RCDeleteImpact`, `Relations`; `scr_new_scene.py`, `scr_new_cast.py`, `scr_cast.py`.

#### DS-19 · 글 속 표시 (inline decorations: link, candidate, hidden, parenthetical, caret) — P0 · needsExternal: no

| Decoration | Look | Source | Behaviour owner |
| --- | --- | --- | --- |
| `@` link to an item (`오리지늄 등불`) | text `ink` + 1 dp solid underline `ink3` 2 dp below baseline (CSS `border-bottom: 1px solid var(--ink3)` / `.lk`) | `gen_nm.py` `lines`, `scr_new_time.py` `.lk` | SCR (runs), WLD (term usage) |
| Term candidate `후보` (display layer `용어 후보` on) | dotted underline `ink3`, thickness 2 dp, offset 6 dp | `scr_new_time.py` `.cand` | WLD rule 21 |
| Candidate tapped (`.cand.on`) | underline colour `acc`, `sel` fill, radius 4 around the matched range; opens the candidate popover (DS-26) | `.cand.on`, board `RTCandidate` | WLD |
| Link to a hidden item | the text `숨긴 항목` / `숨긴 인물` in place of the name (copy LIB rule 19) with the link underline; plain text is never altered | rule 19 §6 | LIB |
| Link to a deleted item | DS-18 dotted underline + `◇` | rule 20 | CST |
| Parenthetical `(숨을 삼키며)` | `paren` role inline: 14 sp, 400, `ink3`, keeps the line's line height; display layer `괄호 지문` hides it in the view | `.paren` | SCR |
| Caret while typing | 2 dp × 20 dp `acc` bar (18 dp in fields, 26 dp in the live transcript), vertical offset −3 dp; blinks per DS-14 | `CARET` | SCR |
| Unconfirmed transcript tail | `ink3` until confirmed | `scr_lr.py` | Recorder |

- Search-result boxes are DS-45. The past-lens "value being edited" field uses the text-field focus ring (DS-35).

*Sources:* `gen_nm.py`; `scr_new_time.py` XCSS; rule 21 §2 table (후보 점선 밑줄, 기본 끔); rule 19 §6; memory (no expression tags, `.paren`).

#### DS-20 · 등급 배지 (rating badge) — P1 · needsExternal: no

**(0.2.0)** Not drawn anywhere: the rating leaves the UI (S-4, LIB-92, DS-75). The text below describes 0.1.0.

"프로젝트 만들 때 수위 등급 하나: ALL(R-12) · R-15 · R-18. R-18은 빨간 배지." (DS README 숨기기 · 등급)

- Text `ALL`, `R-15` or `R-18` in the `badge` role (10 / 18, 600, `tnum`), height 18, padding 0 × 6, radius `radius-xs` 5, 1 dp inside outline, 6 dp right margin, raised 1 dp on the baseline (`vertical-align: 1px`).
- Colours on `bg` / `raise`: ALL and R-15 → outline `ink3`, text `ink2`; R-18 → outline `r18`, text `r18` (`nmlib.py` `rating`, `bundle.css` `.badge`). On images (cards): ALL / R-15 → outline + text `onImageBadge`; R-18 → `onImageR18` `#FF8A80` (DS-03).
- Never filled; never coloured by anything but the rating.
- Shown on 서재 rows and cards, the project home line, new-project summary, export cover (placement LIB).
- In the rating picker the R-18 choice cell uses `r18` for its selected outline (board `NewNameR18`; screen LIB); the rating-raise sheet's confirm button stays `pri` (memory 2026-11-22).

*Sources:* DS README; `nmlib.py` `rating`; `bundle.css` `.badge`; `scr_cards.py` `badge`; DS `Tag` preview; memory (content rating, R-18 colours).

### E. Surfaces

#### DS-21 · 유리 막대 (GlassBars: top title bar and bottom command bar) — P0 · needsExternal: no

"위 제목 막대와 아래 명령 막대(GlassBars)는 glass + 블러 36px, 가장자리는 마스크로 흐려진다. 본문 위에 유리를 깔지 않는다." (DS README 원칙)

- **Top bar** (`.glass.gT`): full width, from the window top (behind the status bar) down to the bar height (cover 112 / script 118; inner 112 / 96). Fill and blur per DS-22. Alpha mask: opaque from 0 to **72 %** of its height, then a linear fade to transparent at 100 %. Contents (z above): optional back `IconButton` (`Back`, 44, shifted −12 dp so the glyph aligns with the 20 dp gutter), a column with the small line (`number` role 11 / 15, `ink3`, e.g. `1-7 · 씬 3 · 2일째 새벽`, `인물 기록 007`, `설정`) and the title (`headerTitle` 19 / 26 700 cover, 21 / 28 inner), then right-side items (lens chip, `Layers` icon button `연출 · 메모 표시`, ViewToggle, `⋯`). Contents top 16 (cover) / 18 (inner).
- **Bottom bar** (`.glass.gB`): full width at the window bottom (behind the navigation bar), height 130 (or 150 with an extra row; 120 on onboarding steps). Mask: opaque from the bottom up to **70 %**, then fading to transparent at the top edge. Contents: a column 8 dp apart, left / right 12, bottom 18 above the navigation inset: optional row (script: speaker pill · `Layers` · spacer · primary pill `줄바꿈`; profile: record tabs) then the command bar (DS-29). On inner script screens the bottom bar spans only the manuscript column (right edge = side panel's left edge).
- The manuscript scrolls **under** both bars and dissolves into them; the bars never move while scrolling (CNV-37 "never moves").
- Bars carry no shadow and no hairline in glass mode.
- Board-specific variants: profile has no top bar, only floating **glass round buttons** (DS-31) over the art; onboarding has a progress row instead of a title.

*Sources:* DS `GlassBars` README / preview; `bundle.css` `.glass`, `.gT`, `.gB`; `nmlib.py` `head`, `foot`; `gen_nm.py` `cmdbar`, `script_head`, Profile; board `Style` (`유리 36px · 아래로 흐려짐`).

#### DS-22 · 유리 재질 (glass material and its fallbacks) — P0 · needsExternal: no

- **Material**: background blur `blur-glass` 36 dp of the content behind + saturation × 1.4, then the `glass` fill on top. Implemented with the Haze library (`hazeSource` on the scrolling content, `hazeEffect` on bars, sheets, side panels and popovers) `[PROPOSED]` library choice (planning: "블러는 Haze 라이브러리 기준"). Small glass chips (map pin labels, map person chips, image status chip) use 12 dp blur with the same fill.
- **Modes** (decided by CNV-46 `glassMode(api, reduceTransparency)`; tests T-CNV-56). Rendering per mode:

| Mode | When (CNV-46) | Rendering |
| --- | --- | --- |
| `BLUR` | API ≥ 33, transparency not reduced | blur 36 + saturation 1.4 + `glass` fill + edge masks (DS-21) |
| `BLUR_COSTLY` | API 31–32 | same look as `BLUR`; at most the top bar, bottom bar and one sheet blur at the same time `[PROPOSED]` (research: one blur source costs 25–33 MiB GPU memory) |
| `SCRIM` | API ≤ 30 (minSdk 29) | no blur; translucent fill. `[PROPOSED]` `raise` at 92 % alpha (`0xEB` + `raise` RGB) with the same edge masks. §6 Q-DS-14 |
| `OPAQUE` | `투명도 줄이기` on | no blur, fill `raise` (opaque), **no edge mask**; top bar gets a 1 dp `line` bottom border, bottom bar a 1 dp `line` top border; sheets and panels keep their `line` inset edge (board `ReduceTransparency`: `유리 대신 불투명 막대 · 흐림 없음`) |

- Glass is only for bars, sheets, side panels, popovers, banners and small map / image chips. Never behind manuscript blocks, never stacked glass-on-glass for text-heavy content (DS README; research §1).
- Text on glass uses the normal text tokens; the contrast check uses the fill composited on `bg` (DS-04).
- Whether the system "reduce blur" setting can be read is a device check owned by CNV-46.

*Sources:* DS README 재질과 움직임; tokens.json `blur`; `bundle.css` (incl. `prefers-reduced-transparency` rule); board `ReduceTransparency` (`scr_motion.py` `EXTRA_CSS`); planning 접근성, 남은 확인; research §1.

#### DS-23 · 시트 (bottom sheet) — P0 · needsExternal: no

"커버 화면에서 패널 · 선택 · 메뉴는 아래 시트가 된다. 펼친 화면에서는 같은 내용이 옆 패널이다." (DS `Sheet`)

- **Anatomy** (`.sheet`): attached to the bottom, full width; top corners `radius-lg` 28; padding 10 top, 20 sides (16 in the speaker picker), 0 bottom + navigation inset; glass material (DS-22); 1 dp `line` inset at the top edge. **Grab handle** 36 × 4, radius 2, `ink3` at 50 % opacity, centred 10 dp from the top. **Title row** 14 dp below the handle (12 in newer sheets): title in `title` role (22 / 28 800) on the left, `닫기` in `label` role (`ink2`) on the right, baseline-aligned; optional `small` sub-line 2 dp below the title. Some sheets put tabs in the title row instead (`시간 · 참고 · 속성 · 모듈` + `닫기`).
- **Scrim**: `shade` over the whole window behind the sheet; the screen behind scales to 96.5 % and blurs 2 dp (`blur-back`) while the sheet is open (motion CNV-41).
- **Height**: wraps its content; the top edge never rises above the bottom of the status bar + 56 dp `[PROPOSED]`; longer content scrolls inside the sheet with the title row pinned. Board tops for reference (844 window): speaker picker 236, lens 300, panel sheets 400, orphan record 470.
- **Dismiss**: `닫기`, tapping the scrim, system back, or dragging the handle down past 40 % of its height `[PROPOSED]` threshold. Dragging never confirms anything.
- **Actions** at the bottom of a sheet: a row 8 dp apart, `ghost` cancel flex 1 + primary flex 1 (or primary flex 2 when a cancel exists, `scr_new_scene.py` `actions`), margin-top 14–16.
- **Inner screens**: the same content becomes a side panel (DS-24) for script panels and map / place cards, or a centred sheet of max width 560 for confirmations and impact sheets (rule 20 §4: "커버 아래 시트, 펼친 화면 가운데 시트") `[PROPOSED]` width. Shape changes on fold keep the same element, tab and scroll (CNV-34).

*Sources:* DS `Sheet` README / preview; `nmlib.py` `sheet`, `EXTRA` (`.sheet`, `.grab`, `.h2`); `gen_nm.py` Picker; `scr_new_scene.py` `sh_title`, `actions`; board `MSheet`; rule 20 §4.

#### DS-24 · 옆 패널 (side panel, inner) — P0 · needsExternal: no

- Glass aside on the right edge, full window height, width 290 at the 800 reference (≈ 36 % of the window) `[PROPOSED]` proportion; padding 20 top, 22 sides; 1 dp `line` inset on its left edge; z above the bottom bar of the manuscript column.
- Header row: `Tabs` (DS-37, gap 16) on the left, `닫기` (`label`, 12 sp) on the right.
- Section heads inside use `sec`; cards inside use `tint` fill, radius 14, padding 14 (e.g. the `◆ 관계 · 리엔의 마음 · 1-7 #043부터` card with actions `고치기` · `관계도에서 보기`).
- Selected rows inside the panel use `sel` + 2 dp `acc` bar with −10 dp side margins (DS-40).
- The fold transition geometry (floating 288 × 672 radius 20 in `MFoldScript`) is CNV-34; see §6 Q-DS-21.

*Sources:* `gen_nm.py` Wide; `scr_core.py` `wide`; boards `Wide`, `WideRef`, `WideProps`, `WideMods`, `MFoldScript`.

#### DS-25 · 대화상자 (dialogs) — P0 · needsExternal: no

Two drawn forms; both sit above the `shade` scrim.

1. **Centred dialog** (`ConfirmDelete`): left / right 20 dp on cover (max width 420 dp on inner `[PROPOSED]`), radius 26, padding 22, fill `raise`, 1 dp `line` inset, shadow `shadow-float` (board draws `rgba(0,0,0,.25)`, §6 Q-DS-18). Title `dialogTitle` (`‘유리 정원’ 지우기`), then `.row` facts (`씬 · 인물 · 페이지` | `6 · 4 · 3`; `이동` | `휴지통 · 30일 보관`), optional `small` label + text field (`확인: 프로젝트 이름 입력`), then two buttons 8 dp apart, flex 1 each: `ghost` `취소` and the confirm button. A **hard-to-undo** confirm (moving a project to trash, permanent delete) uses the `danger` button (`r18` / `onr18`, `휴지통으로`); every other confirm uses `primary`.
2. **Bottom floating dialog** (`ErrBio`, system-auth style): left / right 12, bottom 16, radius 28, padding 26 · 22 · 18, `raise` fill, `line` inset, shadow; centred column, 8 dp gaps: `small` header (`시스템 생체 인증`), title 19 / 26 700 (`지문 인식 5회 실패`), `small` 13 sp (`30초 뒤 다시 · 지금은 기기 PIN`), icon 48 `ink3`, buttons `ghost` `취소` + primary `PIN 사용`.

- Dialog text follows the copy rules (DS-62); 해요체 sentences are allowed **only** in confirmation dialogs that truly need them (DS README 문구).
- The primary button's label is a verb; never `확인` alone.

*Sources:* boards `ConfirmDelete`, `ErrBio`; `scr_states.py` `confirm_delete`, `err_bio`; DS `Buttons`; DS README 문구.

#### DS-26 · 작은 창 (popover) — P1 · needsExternal: no

- Glass (DS-22) card anchored 6 dp below (or above, if no room) its target; radius 16; padding 12 · 12 · 12 · 14; 1 dp `edge` inset + shadow `0 10 28` in `shade`; width 268 on cover (reference) `[PROPOSED]` max 320 dp. Title row: name in `itemTitle` + `small` tag (`오리지늄 등불` · `후보 · 끊김`); `small` line (`첫 공개 1-7 · 잇기 시 ▲ 공개 전 사용`, `▲` part in `warn`); buttons row 8 dp apart, flex 1 each: primary + ghost (`잇기` · `아님`) — copy WLD.
- Dismiss by tapping outside or back. On cover screens a popover with more than one action row becomes a bottom sheet (CNV-32: "커버에서는 옆 패널 · 팝오버가 아래 시트가 된다").
- Canvas line-label popovers on inner screens use the same material (content CNV-08).

*Sources:* `scr_new_time.py` `.pop`, `candidate`; board `RTCandidate`; planning 커버 화면과 펼친 화면.

#### DS-27 · 알림 (Toast) — P0 · needsExternal: no

"지우기 · 옮기기 뒤 아래에 잠깐. `ink` 면 + `bg` 글자, 동작 하나." (DS `Toast`)

- Anatomy: radius 16; padding 6 · 6 · 6 · 16; fill `ink`; message in 14 / 20 500 `bg`, flex 1, up to two lines `[PROPOSED]`; one action button at the right: `Button` with **`bg` fill and `ink` text** (44 tall, pill), icon `Undo` 16 + label `되돌리기` (gap 6). Gap 10 between message and action.
- Position: left / right 16 dp, 6 dp above the top edge of the bottom glass bar (script reference: bottom 156 over a 150 bar). It does not cover the command bar.
- Behaviour (host, timing, replacement, messages) is LIB-40. Messages follow vocabulary: `블록 #043 지움 · ◆ 1개는 #044 자리로`.
- Enter: rises 8 dp + fade with `Enter`; exit: fade with `Exit` (reduced: fades 120 ms) `[PROPOSED]` (CNV-37 lists no toast motion).

*Sources:* DS `Toast`; board `UndoToast`; `scr_states.py` `undo_toast`; LIB-40.

#### DS-28 · 띠와 그림 가장자리 (banners and image fade) — P1 · needsExternal: no

- **Past-lens edit banner** (`LensEdit`): glass, left / right 12, top 64 (just below the record header), radius 16, padding 10 · 12 · 10 · 14, **1.5 dp `acc` inset**; text column: `1-5 시점 값 고치는 중` (600 13 / 18) over `small` `고치면 1-5부터 적용`; right: ghost button 36 tall, 12 sp, padding 0 × 12, `최신으로` (touch area 44). Behaviour STO.
- **Read-only example banner** (`OnbExample`): glass, left / right 12, top 16, radius 18, padding 10 · 14, 1 dp `line` inset; a 4-step progress bar (DS-48) above a row: `Lock` 16 in `acc`, `예시 프로젝트 · 읽기 전용` (600 13 / 18), `small` `온보딩 후 삭제` (copy LIB).
- **Image fade into the page** (profile header art, `art()`): the image (or placeholder art) is drawn full width from the top (height 430 on cover); a gradient from transparent (at its fade start, 230 dp) to `bg` at 85 % of the remaining height covers its lower part; on top, a backdrop blur `blur-edge` 22 dp masked from transparent at 25 % to opaque at 60 % of the image height, so the picture blurs progressively downward into the solid page that carries the record text. No glass bar on top; back button and lens chip float as glass round buttons (DS-31).

*Sources:* `scr_decide.py` `lens_edit`; `scr_onb.py` `ro_banner`; `gen_nm.py` `art`, Profile; tokens.json `blur-edge`; boards `LensEdit`, `OnbExample`, `Profile`.

### F. Controls and navigation

#### DS-29 · 명령 칸 (CommandBar) — P0 · needsExternal: no

**(0.2.0)** The mic is drawn only on 프로젝트 홈 (S-8, DS-75, LRC-48). The script has no bottom command bar (U-14).

"모든 화면 아래의 명령 칸. 탭 막대 대신 이 칸 하나로 인물 · 블록 · 장면 이동과 검색을 한다." (DS `CommandBar`; behaviour STO)

- Height `cmd-h` 52; radius 14 (§6 Q-DS-08); fill `tint`; 1 dp `edge` inset; row with gap 10, padding 0 × 14; text `command` role (14 / 20, `ink3`).
- Leading `/` in the `number` role 13 sp `ink2`; hint text flex 1, one line, ellipsis (`명령 또는 검색 · 인물 · 블록 · 씬 이동` on the script, `명령 또는 검색 · 인물 · 용어 · 대사` on 홈, `명령 또는 검색` elsewhere; per-screen hints STO / LIB, e.g. `모든 프로젝트에서 찾기` on 서재, `이름 · 초성 · 태그로 찾기` on 인물).
- Trailing **mic**: 32 × 32 circle, `pri` fill, `Mic` 16 in `onpri`, touch area 44 (DS-11); accessibility label `라이프 레코더`; one tap starts recording (Recorder).
- Active (typing) state: hint replaced by typed text in `ink` with a caret (DS-19); on the search sheet the right end shows `닫기` (`small`) instead of the mic (board `EmptySearch`).
- Always placed inside the bottom glass bar (DS-21); never floats over the manuscript without the bar.

*Sources:* DS `CommandBar`; `gen_nm.py` `cmdbar`, `.cmd`; `nmlib.py` `foot`; boards `Script`, `Home`, `Library`, `Search`, `EmptySearch`.

#### DS-30 · 버튼 (Buttons) — P0 · needsExternal: no

"높이 44 이상, `radius-full`. 동사로 끝나는 짧은 라벨." (DS `Buttons`)

| Variant | Fill / text | Size | Use |
| --- | --- | --- | --- |
| `primary` | `pri` / `onpri` | 44 tall, padding 0 × 18, radius 22, `button` role 600 13 / 16 | the one main action of a screen or sheet (`쓰기 시작`, `줄바꿈`, `이 시점으로 보기`) |
| `ghost` (보조) | `tint` / `ink` | same | secondary actions (`닫기`, `취소`, `최신으로 돌아가기`) |
| `danger` (지우기 확인) | `r18` / `onr18` | same | confirm of a hard-to-undo action only (`휴지통으로`) |
| `primaryLarge` | `pri` / `onpri` | 52 tall, radius 26, 15 sp (welcome `시작하기`: 54 / 27) | onboarding / wizard bottom action (`다음`) |
| `compact` | primary or ghost | 36–40 tall drawn, padding 0 × 12–14, 12 sp; touch area 44 | inside banners, tooltips, lock rows (`최신으로`, `다음`, `지문으로 보기`) |
| `twoLine` (`.btn2`) | `pri` / `onpri`; ghost: `tint` / `ink` + 1 dp `edge` inset | min height 56, padding 8 × 18, radius 16; title 600 14 / 19; sub-line 400 12 / 16 at 80 % opacity, left-aligned | choices that need a consequence line (`이 값 고치기` · `1-3 기록을 고침 · 1-3부터 새 값`; `여기서부터 바뀜 · 1-7` · `새 ◆ · 집필 선 1-7 씬 3 뒤`) |
| `onToast` | `bg` / `ink` | 44, pill | the toast action |
| `onImage` | none / `onImage` | 44 icon button | `⋯` on cards |

- One `primary` per screen or sheet (DS `Buttons`). Button rows: 8 dp apart; equal flex unless the source gives `flex: 2` to the primary.
- Labels are verbs or verb phrases; `확인` alone is forbidden (DS-62).
- States `[PROPOSED]` (no source draws them): **pressed** — content scales to 0.94 for 100 ms then back (the `MType` key press), plus an `ink` state layer at 8 % on ghost buttons; **disabled** — 38 % opacity of fill and text, no state layer, not focusable for actions but still announced; **focused** (keyboard) — 2 dp `acc` outline 2 dp outside the shape.
- Icon + text buttons: icon 16 dp, gap 6 (`되돌리기`, `글로 쓰기`).

*Sources:* DS `Buttons` README / preview; `bundle.css` `.btn`, `.btn.ghost`; `scr_new_time.py` `.btn2`; `scr_onb.py` `shell`, `a_welcome`; `scr_states.py`; `scr_decide.py` `tip`; DS README 문구; memory (R-18 raise button stays pri).

#### DS-31 · 아이콘 버튼 · 알약 · 키보드 도구 (icon buttons, pills, keyboard tools) — P0 · needsExternal: no

**(0.2.0)** Which buttons are icon only, icon + text or text: DS-70. The `줄바꿈` primary pill is gone with U-14.

- **Icon button** (`.ib`): 44 × 44, icon 20 `ink2` centred, no fill. Header back uses `Back`; display layers `Layers` (`연출 · 메모 표시`); `⋯` uses `More`.
- **Glass round button** (profile over art): 44 × 44, radius 22, glass; icon `Back`. **Glass pill** next to it: height 44, padding 0 × 16, radius 22, glass, 500 12 / 16 `ink` (`시점 · 1-5`).
- **Pill** (`.pill`): height 44, gap 8, padding 0 × 14, radius 22, 500 13 / 16. Speaker pill on the script: `tint` fill, padding-left 6, avatar 32 + name (`리 · 리엔`); primary pill `pri` / `onpri`, padding 0 × 18, 600 (`줄바꿈`).
- **Keyboard tool** (keyboard top row, `EditKeyboard`): height 44, min width 44 (padding 0 × 8 when it has text), radius 12, 600 13 / 16; off `ink2`, on `sel` fill + `ink`; gap 4 between icon and text.
- **Map tool cell**: DS-55.

*Sources:* `nmlib.py` `.ib`; `gen_nm.py` `.pill`, `cmdbar`, Profile; `scr_core.py` `edit_keyboard`.

#### DS-32 · 토글 · 라디오 · 체크 (Controls) — P0 · needsExternal: no

"토글(.sw): 켜짐은 acc 트랙. 라디오(둥근): 하나만 고르기. 체크(모서리 둥근 사각 + 체크 표시): 여러 개 고르기. 모두 누르는 범위 44px." (DS `Controls`)

- **Switch**: track 46 × 28, radius 14. Off: `tint` fill + 1 dp `line` inset; thumb 22 × 22 radius 11 at x 3, y 3, `ink3`. On: track `acc`, no inset; thumb at x 21, fill `bg`. Thumb moves with `Standard` 200 ms (reduced: jump) `[PROPOSED]`. Touch area 54 × 44.
- **Radio** (single choice): 22 × 22 circle, 1.5 dp `ink3` inset; on: 1.5 dp `acc` inset + 12 × 12 `acc` dot centred. Touch area 44 × 44.
- **Checkbox** (multiple choice): 22 × 22 (20 × 20 in dense sheets), radius 6; off: 1.5 dp `ink3` inset; on: **`acc` fill** with the `Check` icon 16 dp, stroke `bg`, stroke width 3 (in grid units), round caps. Touch area 44 × 44.
- **Rows**: the whole row toggles. Radio row (`.li` with radio at the right, title 15 / 22 700 + `small` sub-line); checkbox row (checkbox at the left, gap 12, padding 11 · 0, right-aligned `small` note, e.g. `R-15 기본 · 끔`); type grid cell (radio + 14 / 20 label; selected cell `sel` fill, radius 12, label 600).
- **Choice card** (radio card): padding 14 × 16 (export options 12 × 14), radius 16 (14), margin-bottom 8; selected `sel` fill + 1.5 dp `acc` inset; unselected 1 dp `edge` inset (§6 Q-DS-10); title `itemTitle`, sub `small`, optional diagram / right content, radio at the far right.
- Disabled `[PROPOSED]`: 38 % opacity, not toggleable.

*Sources:* DS `Controls` README / preview; `bundle.css` `.sw`, `.radio`, `.check`; `nmlib.py` `sw`; `scr_bulk.py` / `scr_new_time.py` / `scr_new_cast.py` `chk`, `dot`, `type_grid`; `scr_new_safe.py` `crow`; `scr_onb.py` `choice`; `scr_states.py` `export`.

#### DS-33 · 분할 버튼 (segmented controls) — P0 · needsExternal: no

- **Text segmented** (`.sg`, newer boards): a row of equal cells in a container with padding 3, radius 12, `tint` fill; cell text 500 13 sp centred, line height 38 (container height 44); cells radius 9; selected cell `pri` fill + `onpri` text; others `ink2`. Used for theme (`기기 설정 · 라이트 · 다크`) and 2–3 way choices in sheets (board examples `모두 잇기 · 모두 아님`, `기준 사건 · 직접`, `씬 3 · 새 씬 4 · 둘 다`, `시스템 텍스트로 남기기 · 지우기`; copy by owners).
  - The 설정 board draws the theme control at 12 / 34 (40 tall); this spec uses the 44-tall `.sg` geometry everywhere (§6 Q-DS-11).
- **Outlined segmented** (`.seg`): inline row, height 36, radius 12, 1 dp `edge` inset, clipped; cells 44 wide (icon 18) or auto width with padding 0 × 12 and 600 12 / 16 text (WhenPicker precision `정확 · 대략`, height 32); selected cell `pri` / `onpri`, others `ink3`; touch height 44.
- Accessibility: a group with role `RadioGroup` semantics; each cell is `selected` / not (DS-64).

*Sources:* `scr_new_time.py` `.sg`; `scr_new_scene.py` `seg`; `scr_tools.py` `settings`; `bundle.css` `.seg`; DS `ViewToggle`, `WhenPicker` previews.

#### DS-34 · 슬라이더 (slider) — P1 · needsExternal: no

- Track 4 dp tall, radius 2, `line`; filled part from the start to the thumb in `acc`; thumb **18 × 18 rounded square** (radius 5) in `ink` (no circle); end labels: a small `가` (13 sp) at the start and a large `가` (22 sp) at the end, 12 dp from the track. Touch height 44; the thumb's touch area 44 × 44.
- Discrete steps snap with `Standard` 200 ms; each step change updates the live preview line (owner LIB-65). `[PROPOSED]` tick marks are not drawn (none on the board).

*Sources:* `scr_onb.py` `a_look`; board `OnbLook`.

#### DS-35 · 입력칸 (text fields) — P0 · needsExternal: no

- **Text field**: label above in `label` role (13 / 18 500 `ink2`), 6 dp gap; field 46 tall, radius 12, `tint` fill, padding 0 × 12; value in `field` role (16 / 22, `ink`). Unfocused: 1 dp `edge` inset. Focused: 1.5 dp `acc` inset + caret (DS-19). Optional unit suffix at the right in `small` `ink3` (`cm`); never converted (CST). Optional hint under the field in `small`, 6 dp gap.
- **Value-under-edit** (a lens-dependent field being corrected): the same focused field placed inside a field-table row (`LensEdit`: `등불 결사 · 견습`), with the row's meta (`1-5부터 · 다음 변화 2-5`) right-aligned above.
- **Confirm-name field** (delete project): same field, focused ring `acc` 1 dp (board) → use the standard 1.5 dp `[PROPOSED]` for consistency.
- **Search field** (in sheets / search screen): the command-bar shape (DS-29) with `Search` 18 at the left, typed text in `ink` (e.g. `ㄹㅇ` with caret), and a right-aligned `small` 11 sp hint `초성 검색`.
- **Password field** (`복구 암호`): dots `•` in 18 / 20 with 0.18 em tracking (`tnum`), caret when focused, hint lines below (copy LIB).
- Error state `[PROPOSED]` (not drawn): 1.5 dp `r18` inset and a `small` line in `r18` below; message copy from the owning area.
- Placeholder text, if any, is a format hint only, in `ink3` (no label replacement).

*Sources:* `scr_new_cast.py` `field`; `scr_decide.py` `lens_edit`; `scr_states.py` `confirm_delete`; `gen_nm.py` Picker; `scr_new_safe.py` `pw_field`; `scr_onb.py` `field`.

#### DS-36 · 칩 (Tag: filter chips, removable chips, small chips) — P0 · needsExternal: no

"칩: 높이 30, 테두리 edge. 선택은 pri 면. 실제로 누르는 범위는 44px(::after)." (DS `Tag`)

- **Filter / choice chip** (`.tag`): height 30, padding 0 × 12, radius 15, 1 dp `edge` inset, `chip` role (500 12 / 16, `ink2`); selected: `pri` fill, `onpri` text, no outline. Counts inside the label (`전체 12`, `이동 4`). Rows of chips wrap with 6 dp gaps (8 in the DS preview). Touch area per DS-11.
- **Removable chip** (recorder `연결`): height 32, gap 6, text `ink`, optional leading icon 14 (`Link`), trailing `X` 14 = remove (`연결 · 1-7 씬 3 ✕`). An add chip without `X`: `+ 연결`.
- **Map value chip** (`.chipv`): height 32, padding 0 × 12, radius 16, 600 12 / 16, gap 6 (fill per owner).
- **Status chip on images**: DS-51.
- **Layer flag chip** (`표시 · 용어 후보 켬`): lens-chip shape at 28 tall with `ink` text and the value word in `acc` 600.
- Chips never use colour to encode kind (relations, tags); only selection uses `pri`.

*Sources:* DS `Tag`; `nmlib.py` `.tag`, `tags`; `scr_lr.py` `chip`; `bundle.css` `.chipv`; `scr_new_time.py` `candidate`.

#### DS-37 · 탭 (Tabs) — P0 · needsExternal: no

"글자만 있고 선택은 acc 2px 밑줄. 탭 줄은 한 줄을 넘기지 않는다." (DS `Tabs`)

- Row of text tabs, gap 18 (16 in panels, 14 in cover sheets, 22 in settings kind tabs); `tab` role (500 13 / 44 `ink3`); selected tab `ink` with a 2 dp `acc` underline at the bottom of its 44 dp box (inset shadow `0 -2px 0 acc`).
- One line only: if the tabs do not fit, the row scrolls horizontally `[PROPOSED]`; tab sets in the sources always fit 390 dp.
- Fixed sets: character record `개요 · 말투 · 관계 · 게임 텍스트 · 비밀` (5, fixed); script panel `시간 · 참고 · 속성 · 모듈`; others by owner (e.g. `전체 · 주연 · 등불 결사 · 하층 구역` filters in the speaker picker are drawn as tabs; settings kinds `인물 · 장소 · 용어 · 세력`).
- Switching tabs changes content with a 200 ms `Standard` crossfade `[PROPOSED]`; the underline moves to the new tab with `Standard`.

*Sources:* DS `Tabs`; `gen_nm.py` `.tabs`; `scr_core.py` `tabs4`; `scr_new_cast.py` `KIND_TABS`; board `Profile`, `Picker`.

#### DS-38 · 시점 렌즈 칩 (LensChip) — P0 · needsExternal: no

"앱 전체의 시점 렌즈. 최신이 아니면 acc 테두리." (DS `LensChip`; text and state STO)

- Shape (`.lens`): height 32, padding 0 × 12, radius 16, 1 dp `line` inset (§6 Q-DS-09), `chip` role 500 12 / 16, `ink2`. Touch area per DS-11 (44 tall).
- **At 최신**: as above (`최신 · 1-7`).
- **Not 최신**: text `acc` and 1 dp `acc` inset (`시점 · 1-5`).
- **Script position label**: same text style, **no outline**, no right padding (`이 씬 · 1-7`) — the script has no lens (rule 11 §4); its single action `렌즈를 이 씬으로 맞추기` is STO's.
- **Glass variant** on art headers: DS-31 glass pill.
- Value change animation: the text swaps (old up 8 dp, new from below 8 dp, 200 ms) and an `acc` underline sweeps from the left (CNV-42).
- Lens sheet rows (STO screen): `itemTitle` label + `small` sub; current row `sel` + 2 dp `acc` bar and a right label `보는 중` (600 12 `acc`); unwritten points (`1-8 · 아직 쓰지 않음`) in `ink3`.

*Sources:* DS `LensChip` README / preview; `bundle.css` `.lens`; `nmlib.py` `LENS`, `SCENE_AT`; `scr_story.py` `lens`; `scr_decide.py` `lens_edit`; DS README 시점 렌즈; memory rules-time.

#### DS-39 · 목록 | 카드 전환 (ViewToggle) — P1 · needsExternal: no

**(0.2.0)** On 서재 and the cast list the second cell is the chosen 3D view, and no toggle is drawn when the setting is `목록` (DS-74, LIB-93).

- The outlined segmented control (DS-33) in the header's right slot: 2 cells 44 wide, height 36, icons `List` and `Grid` 18 dp; selected cell `pri` / `onpri`; others `ink3`; accessibility group `보기`, cells `목록`, `카드`.
- Mechanics (per-screen memory, default, switching motion) are CNV-21 / CNV-45.

*Sources:* DS `ViewToggle`; `scr_cards.py` `seg`; CNV-21.

### G. Content pieces

#### DS-40 · 목록 행 · 항목 표 · 섹션 머리 (ListRow, field table, section label) — P0 · needsExternal: no

"`.li`: 제목(명조 16) + 한 줄 조각(`small`). 구분은 `line`. `.row`: 항목 표(라벨 `ink3` | 값). 시점마다 바뀐 값은 ◆." (DS `ListRow`)

- **List row** (`.li`): row, centre-aligned, gap 12, padding 12 · 0, 1 dp `line` top border; optional leading (avatar 36–38, checkbox, grip `Grip` 18 in a 32-wide icon button with −8 dp offset); text column (flex 1): title `itemTitle` (16 / 22 700 `ink`; settings and sheet rows 15 / 22) and sub-line `small` (12 / 17 `ink3`, one line, ellipsis); trailing: value + chevron or a control. Min height 44.
  - **Settings row**: trailing value `small` in `ink2` followed by ` ›` (`보통 ›`, `목록 + 상세 ›`, `기기 설정 ›`), or a switch.
  - **Picker row** (speaker picker): padding 8 × 10, radius 12, avatar 38, name `itemTitle`, sub `small`; current choice `sel` + 2 dp `acc` left bar.
- **Field table row** (`.row`): baseline row, space-between, padding 11 · 0, 1 dp `line` top border, `command` role text (14 / 20); label `ink3`, value `ink`; inline extras after the value: a `small` `ink3` point (`1-3부터`) and `◆` in `acc` (`소속 · 등불 결사 1-3부터 ◆`); numbers in `number` role (`1-2`, `18 / 24`).
- **Key–value line** (`.rk`, sheets): baseline row, gap 10, padding 7 · 0, 1 dp `line` top border, 400 13 / 19 `ink`; key column 64 wide, 12 sp `ink3`.
- **Field action row** (`fld`): min height 44, 1 dp `line` top, gap 10; key `small` 40 wide; value 14 / 20 flex 1; action `label` 12 sp + ` ›`.
- **Selected row** anywhere: `sel` fill + 2 dp `acc` bar at the left edge (inset), drawn edge-to-edge with −10 dp side margins and 10 dp inner padding so text does not move.
- **Section label** (`.sec`): `sec` role (11 / 14, 500, 0.08 em, `ink3`, `tnum`), 22–28 dp above its section, 6–10 dp above the first row (`프로젝트 · 2`, `인물 기록 007 · 시점 1-5`, `Ⅰ 1장 · 등불지기`, `작업`). A right-side `small` or `acc` link may share its baseline (`레코더 · 정리 안 됨 1 ›` in `acc`).
- Rows never sit in cards; the list is set directly on `bg` (DS README 원칙).

*Sources:* DS `ListRow`; `nmlib.py` `.li`, `li`; `gen_nm.py` `.row`, `.sec`, Picker, Profile; `scr_new_scene.py` `.rk`, `fld`; `scr_tools.py` `settings`; `scr_new_cast.py` `frow`.

#### DS-41 · 잠긴 줄 (LockRow) — P1 · needsExternal: no

"숨긴 프로젝트 · 항목 · 기록은 한 줄로 묶어 이름까지 가린다." (DS `LockRow`; behaviour LIB / Recorder)

- Box: padding 14 × 16, radius 16, `tint` fill, 1 dp `edge` inset, row gap 12. Optional leading `Lock` 18 `ink`. Text column: title `itemTitle` (`숨긴 프로젝트 2`, `숨긴 기록 3`) and `small` (`이름 가림 · 검색 · 위젯 · 알림 제외`, copy LIB). Trailing `compact` primary button 40 tall, 12 sp (`지문으로 보기`).
- Locked content behind it is never rendered (no blurred preview of hidden names); after unlock the group's rows appear with the unhide motion (CNV-43) and the `Unlock` icon.
- Locked **card** form in card view: CNV-23.

*Sources:* DS `LockRow`; `scr_states.py` `lib_masked`; boards `Library`, `LibUnlocked`, `LrLocked`, `MUnlock`.

#### DS-42 · 원고 블록 (ScriptBlock typesetting) — P0 · needsExternal: no

"원고 블록은 상자 없이 조판한다. 블록 사이는 space-7(26px), 화자 이름은 speaker 스타일, 대사는 line." (DS `ScriptBlock`; block content and behaviour SCR)

- **Block box** (`.b`): column, gap 6, side padding 24 (gutter), no fill, no border, no avatar. Blocks are separated **only** by 26 dp (`space-7`); some dense previews use 22–24.
- **Speaker line** (`.who`): `speaker` role row, gap 8, centre-aligned: name 600 `ink2` (`리엔`); when the display name differs from the real name, the display name first in the name style, then the real name in `ink3` (`???` `하르트`); optional `· #043` in `ink3` when the block is selected; markers at the far right (`◆` `acc`, dotted marks DS-18). **No expression tags** anywhere.
- **Dialogue** (`.ln`): `line` role 18 / 31 `ink`. **Narration**: `body` role 16 / 28 `ink2`, no speaker line (some boards label it `나레이션` when selected). **Parenthetical**: `paren` inline (DS-19).
- **Direction line** (`.dir`): `dir` role 12 / 16 `ink3`, side padding 24: `▸ 배경  하층 거리(새벽)   ♪ 잔불` (two spaces after the label, three before the next cue). **Author memo**: same style, `⌁ 작가 메모  …`.
- **Selected block**: `sel` fill + 2 dp `acc` bar at the left, 12 dp extra padding top and bottom (10 in compact previews), the speaker name turns `ink`.
- **Typing block**: speaker name `ink`, caret in `acc` at the insertion point; **the line being typed never moves**, also when the keyboard opens (DS README 원고 블록; CNV-37).
- **Links / candidates / hidden / deleted** inside text: DS-18, DS-19.
- Choice, system-text and other block kinds use these same roles; their exact layout is SCR's.
- Unfolded: the manuscript column is 560 wide at x 40 (reference), same typesetting.

*Sources:* DS `ScriptBlock` README / preview; `gen_nm.py` `.b`, `.who`, `.ln`, `.paren`, `.dir`, `lines`, Wide; DS README 원고 블록; memory (no expression tags).

#### DS-43 · 씬 머리 (scene head) — P0 · needsExternal: no

- Not a block (rule 22): a header line between scenes with a 1 dp `line` top border (inset). Newest geometry (`scr_new_scene.py` `scene_head`): margins 8 top, 24 left, 12 right; padding-top 10; a row (top-aligned, gap 4) of a text column (gap 2) and a `⋯` icon button (44, `More`, shifted −6 dp up; accessibility label `씬 4 메뉴`). Older boards without the menu use margins 0 × 24 and padding-top 12.
- Title in `sceneHead` (15 / 22 700 `ink`): `씬 4 · 결사의 문 · 2일째 아침 06:10`; sub-line in `number` role 11 / 16 `ink3`: `전환 문구 · 결사의 문 앞 · 06:10 · 직접` / `전환 문구 · 없음`. Copy and states (자동 · 직접 · 없음, the `⋯` menu items) are SCR / STO.
- Selected state (menu open, `RSHeadMenu`): `sel` fill over the head; the `⋯` icon turns `ink` (no `acc` bar on the head).

*Sources:* `gen_nm.py` `SCENE_HEAD`; `scr_new_time.py` `shead_scene`; `scr_new_scene.py` `scene_head(on)`; boards `RSHead`, `RSHeadMenu`; rule 22.

#### DS-44 · 스와이프 버튼 줄 (SwipeTabs) — P1 · needsExternal: no

"왼쪽으로 밀기만 있다. 버튼을 누르면 그 탭으로 옆 패널 · 시트가 열린다. 탭해도 같은 줄이 열린다. 입력 중인 블록은 꺼짐." (DS `SwipeTabs`; behaviour SCR, motion CNV-40)

- Revealed row: anchored to the right edge of the block's row, width 192 (3 × 64), height = the block's height, fill `raise`. Three buttons (`.tb`), each flex 1, column, centred, gap 4, `chip` role 500 12 / 16 `ink`, 1 dp `line` left border: `시간` (with `◆` 11 sp above it and label in `acc` when the block has a change), `참고` (icon `Paper` 18), `속성` (icon `Props` 18).
- The block slides left by 192 over `bg` (it keeps its own `bg` fill so the row does not show through).
- Nothing executes on swipe; only the buttons act.

*Sources:* DS `SwipeTabs`; `gen_nm.py` `lines(swiped=True)`, `.tb`; feedback (swipe exception).

#### DS-45 · 검색 결과 줄 (SearchHit) — P0 · needsExternal: no

"검색어 부분을 문장 안에서 박스로 표시한다(hl 바탕 + 1px acc). 검색어를 따로 떼어 줄을 바꾸지 않는다." (DS `SearchHit`; feedback-search-highlight)

- Result: location label in `small` above (`1-4 씬 2 · 세라`, `1-7 씬 3 · 나레이션`), 4 dp gap, then the **whole sentence** as one paragraph in 16 / 27 400 `ink`.
- Match box (`mark.hit`): `hl` fill, `ink` text, radius 4, padding 1 × 3, margin 0 × 1, 1 dp `acc` inset; same font as the sentence; it never breaks the line on its own — the box is an inline span, so the sentence wraps as normal text.
- Trailing state after the sentence: `▲ 공개 전 사용` in `warn` 600 12 / 16 (copy from the owner).
- Group headers `대사 · 3` in `sec`. Empty result: DS-47.
- Motion: rows stay; boxes draw left → right, 60 ms apart (CNV-44).

*Sources:* DS `SearchHit`; `nmlib.py` `mark.hit`; boards `Search`, `MSearch`, `EmptySearch`; memory feedback-search-highlight.

#### DS-46 · 아바타 · 첫 글자 (avatar and initials) — P1 · needsExternal: no

- Avatar (`.av`): circle (`radius-full`), `chip` fill, the **first grapheme of the name** centred in 700 `ink`, font size = `floor(size × 0.38)` sp (32 → 12, 36 → 13) except the picker's 38 dp avatar which uses 14 sp. Sizes: 32 (speaker pill), 36 (lists), 38 (speaker picker).
- Mini avatar inside a map person chip: 18 × 18, 10 sp (DS-56).
- With a portrait image: the image's 1:1 crop fills the circle (slots CNV-25).
- Initial-letter **card** for an image slot without an image: CNV-24.

*Sources:* `gen_nm.py` `.av`; `nmlib.py` `av`; `bundle.css` `.who2 i`.

#### DS-47 · 빈 화면 · 오류 블록 (empty and error states) — P0 · needsExternal: no

Screens and copy belong to their owners (LIB states page); the shared visual parts are:

- **Empty block**: margin-top 18, padding 26 × 20, radius 20, 1 dp `edge` inset; title 800 22 / 30 (`인물 없음`, `어긋난 곳 없음`, `기록 없음`); `small` 13 sp sub-line (`이름만 적어도 시작 · 나머지는 나중에`); buttons row 16 dp below: primary flex 1 + optional ghost flex 1 (`인물 추가` · `라이프 레코더에서`). The filter chips above stay (`전체 0`).
- **No results** (search): no box; title 800 22 / 30 `결과 없음` 30 dp below the chips, `small` 13 sp context (`‘오리지누’ · 숨긴 프로젝트 제외`), a `sec` `비슷한 이름` with list rows using search boxes, and a ghost action.
- **Inline error banner** (`ErrBackup`): padding 14 × 16, radius 16, `tint` fill, **1.5 dp inset in the error colour**, icon `Warn` 18 in the error colour, title 700 15 / 22, `small` sub-line; action buttons below, 10 dp gap. The boards draw the error colour as `r18` (§6 Q-DS-05).
- **Full-screen error** (`ErrMic`): glass full screen, icon 28 in the error colour, `stateTitle` (`마이크 꺼짐`), `small` 13 sp sub, buttons column: `primaryLarge` + ghost.
- **Loading**: no spinner for work under 1 s; longer work shows a progress bar or the work itself drawing (map generation doubles as its progress indicator, CNV-37) (DS-48).

*Sources:* `scr_states.py` `empty_block`, `e_cast`, `e_search`, `e_check`, `e_lr`, `err_mic`, `err_backup`; boards `EmptyCast`, `EmptySearch`, `EmptyCheck`, `EmptyLr`, `ErrMic`, `ErrBackup`.

#### DS-48 · 진행 표시 (progress) — P1 · needsExternal: no

- **Step bars** (onboarding / new project): a grid of N equal segments, gap 4, height 3, radius 2; current `acc`, done `ink2`, upcoming `line`. Sits in the top row with `건너뛰기` (`label`) at the right.
- **Tour progress** (tooltip): N bars 16 × 3, gap 3, radius 2; current `acc`, others `line`; with `<label> · n/N` in `number` 11 sp `ink3`.
- **Pager marks** (card rows): current `acc` 18 × 4, others `line` 6 × 4, gap 4, radius 2 (CNV-22).
- **Progress bar** (restore, export): track 6 tall, radius 3, `line`; fill `acc`; below: `number` 12 sp value (`218 / 340 MB`) left and `small` estimate (`약 40초`) right.
- **Step list markers** (restore steps): 20 × 20 radius 6 squares — done `acc` fill; current 2 dp `acc` inset; upcoming 1.5 dp `ink3` inset; upcoming step text `ink3`.
- No circular spinners or rings anywhere (feedback 2026-11-20).

*Sources:* `scr_onb.py` `bars`; `scr_decide.py` `tip`; `scr_states.py` `loading_restore`; CNV-22; board `Restoring`.

#### DS-49 · 안내 툴팁 (tooltip tour card and dim) — P1 · needsExternal: no

- Card: z 9, padding 14 · 16 · 12, radius 16, `raise` fill, 1 dp `line` inset + shadow (`0 14 30 rgba(0,0,0,.25)` on the board; `shadow-float` here `[PROPOSED]` consistency); arrow = a 12 × 12 square rotated 45° in `raise` with the matching `line` edge, 6 dp outside the card's top (or bottom) edge, x aligned to the target.
- Content: tour progress (DS-48) + `label · n/N`; title 700 16 / 22; `small` sub; action row: `건너뛰기` (`label` 12 sp, touch padding 10 × 6) at the left, `compact` primary `다음` (36 tall) at the right (copy LIB).
- **Dim**: `shade` over the screen (z 7) with a cut-out = the target's rectangle padded by 6 dp on every side (`scr_onb2.py` `holes_mask`, mask composite exclude); around the hole a **frame**: the same padded rectangle with radius 12 and a 1.5 dp `acc` ring (`frame`, z 8). The planning doc's "대상을 동그렇게 비우고" is drawn as this rounded rectangle, never a circle.
- Tours, steps and copy: LIB (onboarding).

*Sources:* `scr_decide.py` `tip`, `dim`; boards `Tour1`, `Tour2`, `Tour4`, `TourCast`, `TourWorld`, `TourLr`, `TourMap`, `TourWhen`; planning 온보딩 (모듈 안내).

#### DS-50 · 노드 카드 (canvas / relationship-map node) — P1 · needsExternal: no

- Square-cornered card (no circles, planning 캔버스): padding 10 × 14, radius 14, `raise` fill, 1 dp `line` inset; title 700 14 / 20 `ink`; sub-line 400 11 / 15 `ink3` (`세력`, `사건`).
- Memo node: transparent fill, **dashed** 1 dp `line` outline (its own sign, not the deleted-dotted rule).
- Selected node `[PROPOSED]`: 1.5 dp `acc` inset. Lines, labels and node behaviour: CNV-01 … CNV-12; relation line styles: CST.

*Sources:* `nmlib.py` `.node`; `scr_world.py` canvas nodes; planning 캔버스.

### H. Cards, images, map and time, recorder

#### DS-51 · 카드 (cards over images) — P1 · needsExternal: no

Visual tokens of the card family; sizes, content and rows are CNV-22 / CNV-23.

- Image card (`.cv`): radius 20, clipped; image fills; bottom shade gradient over the lower 62 % from `imageShadeTop` to `imageShadeBottom`; text block 16 dp from the sides and 14 dp from the bottom in fixed on-image colours: badge (DS-20 on-image), title 800 22 / 28 (`onImage`; 16 / 21 single-line in small previews), meta 400 12 / 17 (`onImage2`).
- Status chip (`.hchip`): top-left 12 / 12, height 24, padding 0 × 9, radius 12, 600 11 / 14 `onImage` on `imageChipFill` + 12 dp blur (`홀로`, `쓰는 중 · 홀로`).
- `⋯` at the top-right: 44 icon button, icon 18 `onImage`.
- Locked card / new-project card / recent-file card / portrait card: CNV-23.
- Selected image tile ring (DS-52) is the only selection style on images.

*Sources:* `bundle.css` / `scr_cards.py` `.cv`, `.shadeb`, `.ct`, `.hchip`, `pcard`; DS `ProjectCard`.

#### DS-52 · 이미지 칸 (ImageTile) — P1 · needsExternal: no

"고른 칸은 acc 고리 + 이름 ink." (DS `ImageTile`)

- Tile: the image in its slot ratio (presets drawn at 86 × 114 in the picker), radius 12; name below in `small` (6 dp gap) `ink3`.
- Selected: a double ring — 2 dp gap in `bg`, then 2 dp `acc` (`box-shadow: 0 0 0 2px bg, 0 0 0 4px acc`); name `ink`.
- Preset art (8 abstract angular-shard images: gradient 165° + three polygon layers) and their names: CNV-26. Picker sheet, tabs and flow: CNV-27.

*Sources:* DS `ImageTile` README / preview; `scr_cards.py` `img`, `PRE`, `SHARDS`.

#### DS-53 · 자르기 틀 (CropFrame) — P1 · needsExternal: no

"자르기 틀. 바깥은 어둡게, 안은 3분할 선. 비율은 쓰는 곳마다 고정이고 칩으로 고른다." (DS `CropFrame`)

- Frame: 1.5 dp outline `cropOutline`; outside dimmed with `cropDim`; inside, rule-of-thirds lines (two vertical at 33 % / 66 %, two horizontal) 1 dp `cropGrid`. Stage corners radius 12.
- Ratio chips (DS-36) under the stage (`카드 3:4` selected `pri`, `목록 1:1`), with a `small` note `규격 고정`.
- Tools (zoom, `Rotate` 90°, `Flip`, `Reset` `처음대로`) and behaviour: CNV-28.

*Sources:* DS `CropFrame` README / preview; `scr_cards.py` icons.

#### DS-54 · 은빛 홀로 재질 (holo material) — P1 · needsExternal: no

- One shared material for holo-enabled images: a silver layer + a faint iridescent tint layer in overlay blend + a narrow white sheen band, all offset by device tilt `(tx, ty)`. Exact colour stops, strengths (tint 45 % dark / 32 % light / 50 % on card foils, confirmed by the user), blend, parallax and inputs are CNV-30; this spec only fixes that it is implemented once as a reusable `Modifier.holoFoil(tilt)` and that text drawn over it keeps ≥ 4.5:1 (CNV-30 measured 5.3:1 worst case).
- Never on backgrounds behind text (`인물 배경`, `페이지 머리 이미지`).
- Reduced motion: static foil at the default tilt (`--tx .35`, `--ty −.2`), no sweep.

*Sources:* planning 카드 보기 · 이미지 (홀로그램); `scr_cards.py` `.hfoil`, `.hsheen`; `gen_holo2.py`; memory (HOLO = SILVER, strengths confirmed); CNV-30.

#### DS-55 · 지도 도구 줄과 생성 조각 (MapToolbar and generator controls) — P1 · needsExternal: no

"지도 편집의 눈에 보이는 도구 줄. 한 칸 map-tool 52px, 고른 도구는 pri 면. 숨은 제스처 대신 이것으로 모드를 바꾼다." (DS `MapToolbar`; tools and behaviour WLD)

- **Tool bar** (`.mbar`): glass (DS-22) container, padding 4, gap 2, radius 18, 1 dp `line` inset; cells 52 × 50, radius 14, column centred, gap 2: icon 18 + label 500 11 / 14 `ink2`; selected cell `pri` fill, `onpri`. Role `toolbar`, label `지도 도구`. Default set `이동 · 핀 · 영역 · 경로 · 라벨 · 레이어` (icons `Move`, `Pin`, `Area`, `Route`, `Label`, `Layers`).
- **Generator step row** (`.step`): height 46, padding 0 6 0 10, 1 dp `line` top, gap 8, 600 13 / 18 `ink`; seed in `number` 11 / 14 `ink3`; trailing lock toggle and re-roll (`.lk`). Selected step: `sel` + 2 dp `acc` bar.
- **Lock toggle** (`.lk`): 36 × 36, radius 10, `ink3`; on `acc` icon on `tint`; touch area 44 (expanded 4 dp each side); icons `StepLock` (고정) / `StepOpen` `M6 11h12v9H6zM8 11V8a4 4 0 0 1 7.5-2`, and `Dice` for `다시`; labels `고정`, `다시`.
- **Step chip** (`.stp`): column, centred, gap 4, padding 8 · 0, radius 12, 500 11 / 14 `ink2`; on: `sel` fill + 1.5 dp `acc` inset, `ink`. Used for steps, hex tools (`한 칸` …) and stamp palette.
- **Terrain swatch** (`.pal`): 52 wide column, hexagon 34 × 30 (clip polygon 25 % 0, 75 % 0, 100 % 50 %, 75 % 100 %, 25 % 100 %, 0 50 %) in the `ter-*` colour, label 500 11 / 14 `ink2`; selected: label `ink` and a 2 dp `acc` outline 2 dp off the hexagon.
- **Format tile** (`.fmt`): 166 wide, preview 166 × 118 radius 14 with 1 dp `line` inset; selected: double ring 2 dp `bg` + 2 dp `acc` (outer 4).

*Sources:* DS `MapToolbar` README / preview; `bundle.css` `.mbar`, `.step`, `.lk`, `.stp`, `.pal`, `.fmt`; `scr_map.py` `mbar`; `scr_mapgen.py`.

#### DS-56 · 지도 표시 (MapMarks) — P1 · needsExternal: no

"핀: 아이콘 + 유리 이름표. 고른 핀은 acc. 꼭짓점: 14px 둥근 사각, 마지막 점은 채움. 누르는 범위 44px. 동선 정류장: 에피소드 번호 칩(고른 인물 acc 면, 다른 인물 테두리). 인물 위치: 시점 렌즈 위치에 머리글자 + 이름 유리 칩." (DS `MapMarks`; data and behaviour WLD / STO)

- **Pin** (`.mpin`): `Pin` icon 24 + name label (gap 4): label padding 2 × 8, radius 10, glass fill + 12 dp blur, 600 12 / 16 `ink`. Default colour `ink`; selected pin icon and label text `acc`; not-yet-revealed under the lens (dim mode): 45 % opacity.
- **Vertex** (`.vx`): 14 × 14, radius 4, `bg` fill, 2 dp `acc` inset, centred on the point; the last placed vertex filled `acc`; touch area 44 × 44.
- **Route stop** (`.stop`): height 20, padding 0 × 6, radius 6, 600 10 / 12 `tnum`; the chosen character's stops `acc` fill + `bg` text; other characters' stops `raise` fill, `ink2` text, 1 dp `ink3` inset.
- **Person chip** (`.who2`): height 26, padding 0 9 0 4, radius 13, glass + 12 dp blur, 1 dp `line` inset, gap 4, 600 12 / 16 `ink`; mini avatar 18 (DS-46).
- **Breadcrumb** (`.crumb`): row gap 4, 500 12 / 16 `ink3`, separators `›`, current segment 600 `ink` (`세계 › 하층 구역`).
- **Scale bar**: 96 × 6 box with 1.5 dp `ink2` inset, left half filled `ink2`; label `number` 11 sp `ink2` 3 dp below (`10 km`).
- **Timeline scrubber labels** (`.ep`): 500 10 / 12 `tnum` `ink3`, current point `acc` 700.

*Sources:* DS `MapMarks` README / preview; `bundle.css` `.mpin`, `.vx`, `.stop`, `.who2`, `.crumb`, `.ep`; `scr_map.py` `mpin`, `crumb`, `scalebar`; `scr_link.py`.

#### DS-57 · 지도 그림 색 (MapStyle colours) — P1 · needsExternal: no

"생성 지도의 그림. 색면(기본): 지역마다 map-art-* 흐린 색면을 해안선 안에 깔고, 해안 · 산 · 강 · 길은 옅은 선으로 얹는다. 땅에 파란 계열 금지. 육각 · 다각형은 ter-* 지형 색. 지도 데이터는 그림에 굽지 않는다." (DS `MapStyle`)

- **색면 (default)**: land base `map-art-5`, regions `map-art-1 … 4` as soft fields (blurred, clipped to the coastline), coast / ripple / mountain / river / road lines faint on top; colours follow the theme (DS-03). No blue family on land (sea confusion). Rendering (blur radii, line styles) is WLD's map renderer, reference `mapgeo.py` `soft_svg`.
- **Hex / polygon** terrain colours `ter-*` (identical in both themes because the map is a picture): deep sea `#24394A`, sea `#33505F`, plain `#8E9A6A`, forest `#5E7352`, hill `#A39466`, mountain `#857E73`, peak `#D3D7DB`, desert `#C4AB7A`, swamp `#66705A`, snow `#C6CED4`.
- Other picture styles (`먹선 · 색 · 양피지`, `실루엣`) are WLD's.
- Map overlay data (pins, regions, routes, labels) is always drawn by DS-56 components on top, never baked into the picture.

*Sources:* DS `MapStyle`; tokens.json `map-art-*`, `ter-*`; planning 지도 생성 · 양식.

#### DS-58 · 때 고르기 (WhenPicker) — P1 · needsExternal: no

Visual anatomy of the scene-time picker; options, labels and results are STO (rule 11).

- Header line `small` (`때 · 직전 씬 기준` + base on the right, e.g. `씬 2 · 1-7 · 1일째 저녁`).
- Row 1: choice chips (DS-36) `같은 날 · +1일 · +2일 · 직접`, gap 6.
- Row 2: chips `새벽 · 아침 · 낮 · 저녁 · 밤` and an exact-time chip (`05:40`, `tnum`).
- Row 3: `small` `정밀도` left, outlined segmented (DS-33, 32 tall) `정확 · 대략` right.
- Result box: padding 10 × 12, radius 12, `sel` fill: 700 16 / 22 `1-7 · 2일째 · 새벽` + `small` `이야기 전체 13일째` / `이야기 약 13일째 · 연표에서`.

*Sources:* DS `WhenPicker` README / preview; board `SceneWhere`; rule 11 (memory).

#### DS-59 · 조건 줄 (ConditionRow) — P1 · needsExternal: no

"글로 설명에서 읽어 낸 조건 한 줄. 3×3 칸 = 어디, 이름 = 무엇, 작은 글 = 특징 · 연결 장소." (DS `ConditionRow`; logic WLD)

- Row (`.cond`): centre-aligned, gap 10, padding 10 · 0, 1 dp `line` top. Leading **3 × 3 grid** (`.g3`): 8 × 8 cells, gap 2, radius 2; cells in `line`, the cells the condition covers in `acc`. Text: name 700 15 / 20 (`산맥`, `항구 도시`), `small` (`북쪽 · 눈 덮임`, `남쪽 해안 · 장소 ‘잿더미 시장’`). Trailing `More` icon button 18 `ink3`.
- **Result state** (after generation): leading icon 18 instead of the grid — `Check` in `acc` for `반영`, an exclamation (`M12 5v9M12 18h.01`) in `warn` for `확인 필요`.

*Sources:* DS `ConditionRow` README / preview; `bundle.css` `.cond`, `.g3`; `scr_mapgen.py` (map text boards).

#### DS-60 · 이동 막대 (TravelBar) — P1 · needsExternal: no

"필요 시간(warn)과 두 씬 사이 시간(acc)." (DS `TravelBar`; values and copy STO, rule 18)

- Grid: label column 44, bar column flexible, value column 64 (`minmax` to fit longer values), gap 8, centre-aligned rows.
- Bars (`.bar`): 8 tall, radius 4. Row `필요`: `warn` bar spanning the full column. Row `있음`: `acc` bar, width = `min(1, available ÷ required)` of the column (both in minutes; the available value is the **longest** possible gap, rule 18).
- Values in `small` with `ink` text (`scr_link.py`: `도보 2일 4시간`, `9시간 40분 – 12:40`; STO's normalised form `9시간 40분 – 12시간 40분`; exact strings STO); never `2.5일` (rule 18).
- Below: `small` line starting with `▲ 이동 시간 부족` in `warn` then ` · ` facts (`경로 38 km · 도보 15 km/일`); suggestion chips row (DS-36) under it.

*Sources:* DS `TravelBar` README / preview; `scr_link.py` (bar rows); board `TravelCheck`; memory rules-travel.

#### DS-61 · 레코더 빛 띠 (recorder glow band) — P1 · needsExternal: no

"화면 아래에서 말소리 크기를 따라 올라오는 네이비 빛 띠 … 처리 중: 아래 빛이 좌우로 흐른다." (planning 라이프 레코더; behaviour Recorder)

- Seven vertical bands side by side across the full width (+40 dp overflow each side, −60 dp below the bottom edge), each a vertical gradient from its colour (DS-03 glow palette, in order) at the bottom to transparent at the top, top corners radius 40; the whole group blurred 34 dp with saturation × 1.2. **Bands, never rings or circles.**
- **Recording**: each band's height follows the input level (board heights 170 · 260 · 330 · 240 · 300 · 210 · 150 dp at a sample moment), scaling between 0.72 and 1.08 of its base, 1.6 s ease-in-out alternate, each band phase-shifted 0.23 s; opacity 0.95.
- **Processing** (`정리 중`): the group sweeps horizontally ±90 dp, 2.4 s ease-in-out alternate; band heights frozen.
- **Reduced motion** or `라이프 레코더 빛` off (CNV-46): bands static (`빛 띠 정지`) or hidden respectively.
- Recording header mark: an 8 × 8 `acc` square rotated 45° (a small diamond, not a circle) before `라이프 레코더` and the elapsed time (`00:12`, `tnum`).

*Sources:* `scr_lr.py` `LR_CSS`, `glow`, `record`; boards `LrRecord`, `LrProcess`; planning 라이프 레코더; MotionSpec (`줄이기 · 빛 띠 정지`).

### I. Copy, vocabulary, accessibility

#### DS-62 · 문구 규칙 (UI copy rules and lint) — P0 · needsExternal: no

"라벨과 조각으로. 문장형 안내 대신 명사 · 짧은 구 … 해요체 문장은 꼭 필요한 확인 대화상자에만. 버튼은 동사 … ‘확인’ 단독 금지. 조각 사이 구분은 가운뎃점 ` · `. 이모지 없음." (DS README 문구; feedback-ui-copy-terse)

Rules every string resource must follow (owners write the copy; this spec defines and enforces the rules):

1. **Labels and fragments**, not sentences. Headings are labels (`구조`, `시간`), not questions. Descriptions are fragments: `이름 가림 · 검색 제외`, `1-5부터 적용`, `휴지통 · 30일 뒤 삭제`. 해요체 (`~해요`, `~돼요`) only inside confirmation dialogs that need it.
2. **Buttons are verbs**: `쓰기 시작`, `휴지통으로`, `되돌리기`, `바꾸기`, `넣기`. A button whose whole label is `확인` is forbidden.
3. **Separator** between fragments: ` · ` (space, U+00B7, space). Forbidden as separators: em dash between words, comma lists, ` / ` between words, an unspaced `·`. (`/` stays for commands `/대` and numbers `1/4`, `18 / 24`; `—` stays as a stand-alone empty value.)
4. **Symbols**: only the set of DS-17; no emoji.
5. **Notation**: episodes always `1-7` (never `7장` for an episode); chapters `2장`; blocks `#043`; times `05:40`; rating `ALL · R-15 · R-18` (gloss `12세 이하` for ALL). Day labels per rule 11 (`2일째 새벽`, `1-7 · 2일째 · 새벽`) — formatting owned by STO.
6. **Durations** per rule 18: `2일 4시간`, `약 7시간 40분`; never decimal days.
7. Design notes never appear in UI strings (no "설계 메모" in header slots).
8. Story and sample content (dialogue, profile prose, example project text) is content, not UI copy; the rules do not apply to it.

**String lint** (a JVM test over `values/strings.xml` of every module, T-DS-24 … T-DS-28):
- flag `\S·` and `·\S` (unspaced middle dot);
- flag `\p{L} ?— ?\p{L}` (em dash between words);
- flag `(?<![\d/])\s/\s(?![\d])` between letters (slash list) and `,` used to list three or more fragments in a label `[PROPOSED]` heuristic: a resource of < 40 characters with two or more commas;
- flag any code point with Unicode `Emoji_Presentation=Yes`, and `✓ ★ ● ○ ■ □ ◎`;
- flag a resource used as a button label (key prefix `btn_` or `action_` `[PROPOSED]` naming) whose value is exactly `확인`;
- flag vocabulary violations (DS-63) with its allowlist.

*Sources:* DS README 문구; memory feedback-ui-copy-terse, civilight-vocabulary; planning 공통 개념 (표기 규칙); memory rules-travel.

#### DS-63 · 낱말 (vocabulary: one word per meaning) — P0 · needsExternal: no

From memory `civilight-vocabulary.md` (applied 2026-11-21; Claude's picks, user may overrule) and later confirmed rules.

| Use | Never (in UI) | Allowed exceptions |
| --- | --- | --- |
| `씬` (unit; counts `씬 4개`, ordinal `씬 4`) | `장면` | `막 › 장면` as a structure level name |
| `지우기` / `지움` (to trash) | `삭제` for trash | `삭제` means **permanent** deletion and stays there: `영구 삭제`, `영구 삭제 · 노아`, `N일 뒤 삭제` and `N일 뒤 자동 삭제` (`휴지통 · 30일 뒤 삭제`, `30일 뒤 자동 삭제`), the onboarding example's removal (`온보딩 후 삭제`, `예시 · 읽기 전용 · 온보딩 후 삭제`, `예시 프로젝트 삭제됨`), and the quoted system setting `‘애니메이션 삭제’`. The lint enforces this through the allowlist; a new permanent-delete string is added to it on review. |
| `되살리기` | `복원`, `백업에서 가져오기` | — |
| `화자 없음` | `미지정` | — |
| `고르기` / `고른` | `선택` (select) | `선택지` (block kind); `(선택)` as the "optional" marker in field labels (`주인공 (선택)`, `첫 씬 제목 (선택)`, `한 줄 소개 (선택)`, `설명 (선택)`) and `선택` / `홀로 선택` meaning "optional" in the image-slot table (§6 Q-DS-23) |
| `바꾸기` | `변경` | — |
| `고치기` / `고치는 중` | `수정` (edit) | the last-modified stamp `마지막 수정 <날짜>` and `<time> 수정` drawn on the newest boards (`scr_tools.py`, `scr_new_safe.py`), pending §6 Q-DS-23 |
| `건너뛰기` | `넘기기` | — |
| `미리 보기` (spaced) | `미리보기` | — |
| `글로 쓰기` · `말로 기록` | — | — |
| `글자 크기` · `게임 텍스트` · `커버 화면` | `접힌 화면` | — |
| project `이름` | project `제목` | (review rule, not linted: `제목` is valid for scenes and pages) |
| `때 표기` (setting name) | — | — |
| `제안 받기` (request) · `넣기` (apply) | `받기` for apply | — |
| `지문 · PIN으로 열림` | `지문으로만` | — |
| `??? (1-8까지)` (display name revealed at 1-9) | — | — |
| `연결` (recorder link; also a character-field type) | `붙일 곳` | ordinary `쓰인 곳`, `이곳의 시간` |
| `씬 머리` · `전환 문구` | `씬 전환 블록` | — |
| `장소` (pin) · `지역` (region; `영역` inside the map editor) | `곳` for a scene's place | `장소와 때`, `장소 없음`, `동시에 두 장소` |

- Open naming questions stay as they are until the user decides (§6 Q-DS-20): `서재` vs `라이브러리`, `지역 / 영역 / 구역`, `홀로`, `낭독`, map style `색`, overloaded `기록 / 모듈 / 이동`, `연결` with two meanings.

*Sources:* memory `civilight-vocabulary.md` (incl. 2026-11-22 update), `civilight-concept.md` (TERMINOLOGY 2026-11-21), `civilight-rules-19-23.md`.

#### DS-64 · 접근성 의미 (accessibility semantics) — P1 · needsExternal: no

- **Contrast** DS-04; **touch** DS-11; **motion** and **transparency** preferences CNV-46; **text size** DS-07.
- **Roles and labels** `[PROPOSED]` mapping of the boards' `aria-label`s: icon buttons carry their Korean label (`연출 · 메모 표시`, `라이프 레코더`, `순서 바꾸기`, `보기`, `목록`, `카드`, `지도 도구`, `고정`, `다시`, `근거 지워짐`); switches, radios, checkboxes and segmented cells expose toggle / selected state; tabs expose `Tab` role and selection; markers are spoken per DS-16; decorative layers (glass bars, art, glow, holo, scrim) are hidden from accessibility.
- **Focus**: keyboard / D-pad focus shows a 2 dp `acc` outline 2 dp outside the element `[PROPOSED]` (Tier 2 large-screen guidance in research §4: keyboard and mouse support).
- **Hidden content**: masked names are never put in content descriptions (LIB rule 19).
- **Dark and light** are equal citizens (research §5): every component is checked in both themes.

*Sources:* boards' `aria-label` attributes (`gen_nm.py`, `scr_lr.py`, `scr_new_cast.py`, `scr_map.py`, `scr_cards.py`, `scr_mapgen.py`, `scr_new_scene.py`); planning 접근성; research §4, §5.

### J. Shared layout rules (user findings 2026-11-23)

These rules come from the user's 0.1.0 emulator findings U-01, U-02, U-04, U-05 and U-07 (2026-11-23). The user's findings are the source; the values below were chosen to make them testable and are listed for confirmation in §6 (Q-DS-24 … Q-DS-29). Where they disagree with an earlier statement in this file or in LIB-40, these rules win (see *Superseded statements*). Every rule has JVM tests in §4 (T-DS-36 … T-DS-45) against pure helpers: `CommandBarHost.layout`, `CommandScope.next`, `OverlayGaps.*`, `NoticeRules.*`, `Affordance.of`, `CaretInView.delta`.

**Terms used below.**
- **Frame**: the area the shell gives one scene. Single screen: the whole window. `목록 + 상세`: the whole window (both panes). `가운데 한 칸`: the 560 dp column. A docked side panel (DS-24, script and map card) is not a pane; it is cut off the frame's right side.
- **`systemBottom`**: the bottom obstruction from the system, in dp from the window bottom: `max(navigationBars.bottom, mandatorySystemGestures.bottom, ime.bottom)`. It is the lowest line any app-drawn touch target or overlay may reach.
- **Bottom chrome**: what an app screen draws at the bottom of the frame: the bottom glass bar (DS-21), a pane's extra row, a keyboard toolbar, a wizard or settings bottom button. Its height is measured from the window bottom including the navigation inset padding it carries.
- **Compact / inner**: `WindowLayout.isInner` false / true.

#### DS-65 · 명령 칸은 창마다 하나 (one command bar per frame) — P0 · needsExternal: no · user finding U-01

**Rule.** A frame draws **exactly one** DS-29 command bar, in **one** bottom glass bar that spans the **full frame width**. In `목록 + 상세` neither pane draws its own bottom glass bar or command bar; the shell draws one for the frame. The command bar's target (hint, search scope, mic context) is the **scope pane**, chosen with a visible scope switch, never by guessing from touches.

1. **Who draws it.** A screen still declares its command bar through `CivilightScreen(commandBar = …, bottomExtraRow = …)`. When `LocalPaneRole` is `LIST` or `DETAIL`, `CivilightScreen` does not draw `BottomGlassBar`; it hands its declaration to the frame's `CommandBarHost`, which draws the single bar. In `SINGLE` the screen's own bar is the frame's bar (unchanged on cover screens).
2. **Geometry.**
   - Cover: unchanged (DS-21, DS-29): bar height 130 (150 with an extra row), command field 52 tall, 12 dp side insets, 18 dp above the navigation inset.
   - Inner, single screen and `목록 + 상세`: the bar spans the frame width. The **command row** (scope switch + command field) is centred, side insets 24 dp, max width 720 dp; command field 52 tall, flex 1; bottom 20 dp above the navigation inset (DS-09 inner script value).
   - Inner with a docked side panel: the bar spans the frame minus the panel (DS-21 script rule, now general).
3. **Scope switch** (inner, only when **both** panes declare a command bar): the outlined segmented control of DS-33 (`.seg`: 36 dp drawn, 44 dp touch, radius 12, 1 dp `edge` inset) at the start of the command row, 8 dp before the command field. Two cells, left pane then right pane; each cell label is **exactly the title that pane's top bar shows** (existing copy, e.g. `서재` · `잿빛 새벽의 등불`), `chip` role 600 12 / 16, padding 0 × 12, max cell width 160 dp with ellipsis. Selected cell `pri` / `onpri`, other `ink3`. Accessibility: a `RadioGroup` whose label is `TODO_COPY_DS_COMMAND_SCOPE` (no source gives one; listed under Requests). With only one pane declaring a command bar, no switch is drawn and the bar is that pane's.
4. **Which pane is the scope** (`CommandScope.next(current, event)`):
   - A detail opens or is replaced (a list row tapped, `openDetail`): scope = `DETAIL`.
   - The detail closes (back, `EmptyDetailPane` shown): scope = `LIST`.
   - The user taps a scope-switch cell: scope = that pane, until the next open / replace / close.
   - Touching, scrolling or typing inside a pane **never** changes the scope.
   - A fold to cover resets nothing; on cover only one screen shows and its own bar is used.
5. **Content of the bar** = the scope pane's declaration: its hint text (per-screen hints of STO / LIB, e.g. `모든 프로젝트에서 찾기` on 서재, `명령 또는 검색 · 인물 · 용어 · 대사` on 홈), its search target, and the mic's recorder context. When the scope changes the hint swaps with `ValueSwap` (CNV-42, `standard` 200 ms; reduced: 120 ms crossfade). The bar itself never moves, resizes or fades on a scope change.
6. **Extra rows** (`bottomExtraRow`: script speaker row and `줄바꿈`, record tabs) stay with their pane: they are drawn inside the frame's bar, 8 dp above the command row, horizontally confined to their pane's column with 12 dp insets from the pane edges and the divider. The bar is 150 tall when any pane has an extra row, else 130. Pane-local action bars that are not command bars (wizard `이전 · 다음`, settings bottom button, keyboard toolbar) stay inside their pane and sit on the frame's bar top (or on the keyboard, DS-69).
7. **Locked content.** A pane that shows a lock, or a hidden project / item that is locked, declares **no** command bar and contributes **no** scope cell; its masked title is never placed in the scope switch or in the bar's accessibility tree (LIB rule 19). If the scope pane locks, the scope moves to the other pane.
8. **Content padding.** Both panes receive the same bottom padding = the frame bar's height (or the keyboard, DS-69), so a scope change never reflows either pane.
9. **Toasts and banners.** Toasts are window-level (DS-67), never per pane. Banners (DS-28) stay with their pane.

*Why full width and not "under the focused pane":* there is no focus state the user can see today (shell: "last-touched pane stays" is not built), and a bar that jumps between panes on touch is a guessing interaction (memory `feedback-no-guessing-interactions`). A fixed full-width bar keeps one position for the bar, the toast slot (DS-67) and the overlay gaps (DS-66), and the scope switch makes the target visible and explicit.

#### DS-66 · 떠 있는 것의 아래 간격 (overlay gaps above bars, gesture bar and keyboard) — P0 · needsExternal: no · user finding U-04

**Tokens** (`OverlayGaps`, dp):

| Token | Compact | Inner | Between |
| --- | --- | --- | --- |
| `edge` | 16 | 24 | an overlay's bottom edge and `systemBottom` when no app chrome is below it |
| `aboveBar` | 6 | 6 | an overlay's bottom edge and the top edge of the bottom glass bar (incl. its fade) |
| `aboveKeyboard` | 8 | 8 | an overlay's bottom edge and the keyboard top when nothing rides on the keyboard |
| `sheetBottom` | 24 | 24 | a bottom sheet's last content and `systemBottom` (keyboard hidden) |
| `sheetBottomIme` | 16 | 16 | a bottom sheet's last content and the keyboard top (the sheet rides on the keyboard) |
| `side` | 12 | 24 | a popover / menu / tooltip and the frame's side edges |
| `top` | 8 | 8 | any overlay's top edge and the status-bar inset |
| `dialogBand` | 24 | 24 | a centred dialog's band to the status inset and to `systemBottom` |
| `toastLiftMin` | 156 | 156 | the toast slot above `systemBottom` (DS-67) |

**Placement per overlay** (`y` = distance of the overlay's bottom edge from the window bottom):

| Overlay | Keyboard hidden | Keyboard up |
| --- | --- | --- |
| Bottom sheet (DS-23) | attached to the window bottom; content bottom padding `systemBottom + sheetBottom` (cover 24 + 24 on a 24 dp gesture bar = 48) | sheet sits on the keyboard; content bottom padding `sheetBottomIme` 16; max height shrinks by the keyboard |
| Centred sheet (DS-23 inner) | centred in the band `[statusTop + 24, H − systemBottom − 24]`, 24 dp side margins, max width 560 | same band with the keyboard in `systemBottom` |
| Centred dialog (DS-25) | centred in the same band; max height = band height, body scrolls, buttons stay | same |
| Bottom floating dialog (`ErrBio`) | `y = systemBottom + edge` (cover 16, inner 24); sides 12 (cover) / centred max width 420 (inner) | `y = systemBottom + aboveKeyboard` |
| Popover, menu, tooltip card (DS-26, DS-49) | below its anchor with 6 dp gap if its bottom stays `≤ H − max(systemBottom + edge, barTop + aboveBar)`; otherwise above the anchor with 6 dp gap; never above `statusTop + top`; sides ≥ `side` | same limits with the keyboard in `systemBottom` |
| Toast (DS-27, DS-67) | the window slot of DS-67 | the slot follows the keyboard |

- `barTop` is the top edge of the frame's bottom glass bar (incl. its fade) when one is drawn; with no bar the `edge` term alone applies.
- No overlay's touch target lies below `H − systemBottom` (inside the gesture bar or 3-button navigation bar).
- Sheets, dialogs and popovers sit above the scrim; the toast is above sheets (z 5 > 4, DS-10) and is never covered by a scrim.

#### DS-67 · 알림의 시간 · 자리 · 움직임 (toast and notice rules) — P0 · needsExternal: no · user finding U-05

**Kinds and durations** (`NoticeRules.durationMs(kind)`; the shown time is then passed through Android's `AccessibilityManager.getRecommendedTimeoutMillis(base, FLAG_CONTENT_TEXT | FLAG_CONTENT_CONTROLS when it has an action)`, which only lengthens it):

| Kind | What | Base duration | Example |
| --- | --- | --- | --- |
| `INFO` | a fact, nothing to do (`actionLabel = null`) | **2 500 ms** | `Drive 연결 준비 중`, `내보냄` (owner copy), first-use tips (LIB-20) |
| `ACTION` | one action, normally `되돌리기` | **4 000 ms** | `블록 #043 지움 · ◆ 1개는 #044 자리로` + `되돌리기` |
| `ERROR` | something failed and needs nothing from the user now | **4 000 ms** | `IMPORT_FAILED`, `SAVE_FAILED` (owner copy) |

- **No toast stays until dismissed.** A failure that needs the user to act (backup failed, Drive login expired, mic off) is never a toast; it is a banner (DS-28) or an error block (DS-47) that stays until resolved (LIB-61 already does this). A sticky toast would need its own close control and would cover the slot above the command bar indefinitely.
- The timer **pauses** while a finger is down on the toast, while accessibility focus is on it, and while the app is not resumed (`ON_STOP`); it resumes with the remaining time.
- A new toast replaces the shown one at once (LIB-40); it starts its own full duration.

**Anchoring: the toast belongs to the window.** One `WindowToastHost` is drawn by the shell above `NavDisplay` and the sheet hosts and below the lock and the tour dim. Screens and panes draw no toast host of their own.
- *Why not "dismiss on leaving":* 되돌리기 toasts are often raised together with a navigation (page delete then `pop`, `PageDocScreen`), so dismissing on leaving would remove the undo the user needs. A window toast also cannot appear twice (today each pane of `목록 + 상세` draws the same toast) or jump between screens (today a settings screen without a bar puts it 6 dp above the window edge, and the next screen re-draws it 6 dp above a 130 dp bar).
- **Position.** Horizontally: compact 16 dp from both sides; inner centred on the window, width `min(W − 48, 560)`. Vertically: the toast's bottom edge sits at `systemBottom + lift`, with `lift = max(toastLiftMin 156, tallestBottomChrome + 6)`, where `tallestBottomChrome` is read **once when the toast appears** (above `systemBottom`, so 130 or 150 for glass bars) and then frozen for that toast. While shown, the toast moves only with the keyboard (`systemBottom` changes), never because the screen behind changes, a sheet opens, or a pane's scope changes.
- **Dismissed** by: its action (runs, then dismisses), the timer, a replacing toast, and **at once** (same frame, no exit motion) when the app lock or a hidden-project lock appears, when `다시 잠그기` / a relock runs, or when the project an `ACTION` toast acts on leaves the back stack (its session is released). A toast is never drawn over a lock screen, so no name in its text can show while locked.
- **Motion** (helpers in `theme/Motion.kt`, tokens `CivilightMotion`, MotionLint-clean): enter = `motion.enter(CivilightMotion.enter, rise = 8.dp)` (fade + 8 dp rise, 320 ms); exit = `motion.exit(CivilightMotion.exit)` (fade, 160 ms); replace = `motion.swap(enter = CivilightMotion.enter)` with `animateContentSize(motion.move(CivilightMotion.standard))`; following the keyboard uses the system inset animation (API ≥ 30) or `rememberKeyboardBottom()` on `standard` (API 29). Screen transitions do not animate the toast (it lives outside the scenes). Reduced motion: 120 ms fades, no rise, size snaps.

#### DS-68 · 누르는 것은 누르는 것처럼 (affordance: what is tappable) — P0 · needsExternal: no · user finding U-07

**Rule.** Every tappable element shows **at least one** of the cues below; plain text in `ink` / `ink2` / `ink3` with none of them is **never** tappable. Conversely the chevron (cue B) is **never** drawn on something that is not tappable.

| Cue | Look | Used by |
| --- | --- | --- |
| A · container | a fill (`pri`, `tint`, `bg` on toast) or a 1 dp `edge` / `line` outline around the touch shape | buttons (DS-30), chips (DS-36), segmented (DS-33), lens chip (DS-38), **link tiles** (below), sheet header `닫기` (below) |
| B · chevron | `IconId.Next` 16 dp, `ink3`, vertically centred, 4 dp after the last trailing text (value), 0 dp from the row's end padding | every tappable **row** that opens a screen, sheet or picker and has no control at its end: settings rows, list rows (서재, 인물, 세계, 연표, 기록함, 점검), home episode rows, `이어 쓰기` |
| C · control | a switch, radio or checkbox at the row end; the whole row toggles (DS-32) | settings switches, choice rows |
| D · icon button | a 44 dp icon button glyph (DS-31) with an accessibility label | header icons, mic, `⋯` |
| E · inline link | `acc` text + `IconId.Next` 14 dp in `acc`, 2 dp gap | a link sharing a section label's baseline (`레코더 · 정리 안 됨 1`), a link in an empty or error block |
| F · text decoration | DS-19 decorations inside prose (term link underline, `@` link) | manuscript and rich text |

- **Settings rows**: the trailing ` ›` text of DS-40 becomes cue B (`보통` + 4 dp + `Next` 16).
- **Sheet and side-panel header `닫기`** (DS-23, DS-24): the compact ghost button of DS-30 (36 dp drawn, `tint` fill, 12 sp 600, padding 0 × 12, touch 44) instead of plain `ink2` text.
- **Picker rows** inside a sheet whose title is a `… 고르기` picker (speaker picker, `@` picker, event picker) are exempt from cue B: the whole sheet is one choice list; the current choice shows `sel` + 2 dp `acc` bar (DS-40).
- **Link tile** (new; replaces the plain-text `작업` grid on 프로젝트 홈): height 48, radius 12 (`radius-sm`), fill `tint`, padding 0 × 14; label `itemTitle` at weight 500, 15 / 22, `ink`, one line, ellipsis; trailing `Next` 16 `ink3`, 8 dp gap. Grid: 3 columns when the content width ≥ 336 dp, else 2; gaps 8 dp both ways; tiles share the row width equally. A dimmed shortcut (`설정`) uses label `ink2` and the same tile.
- **Field-table rows, key–value lines, section labels, stats, sub-lines** (DS-40 `.row`, `.rk`, `.sec`) are not tappable and never carry cue B.

**States** (one table for every tappable; values from `foundation/Pressable.kt`, which this supersedes DS-30's 8 % with):

| State | Rows, tiles, inline links | Buttons, chips, segmented, icon buttons |
| --- | --- | --- |
| pressed | `ink` overlay at 6 % over the element's shape, no scale; `CivilightMotion.press` (100 ms, `standard` curve) in and out | scale 0.94 on `press`, no overlay |
| disabled | whole element (incl. chevron and label) at 38 % opacity; no press feedback; semantics `disabled`, still announced | same |
| focused (keyboard / D-pad) | 2 dp `acc` ring, 2 dp outside the shape (full-width rows: drawn on the row bounds, inset 0) | 2 dp `acc` ring, 2 dp outside |
| selected (rows) | `sel` fill + 2 dp `acc` start bar (DS-40); chevron unchanged | `pri` fill (DS-33, DS-36) |

Reduced motion: no scale; the overlay fades in 120 ms linear (colour change, kept).

#### DS-69 · 커서 줄은 늘 보인다 (caret line stays in view) — P0 · needsExternal: no · user finding U-02

**Rule.** While a text field or editor has focus, the **caret line** is always fully inside the **visible band**. This holds for single-line and multi-line fields, the rich editor (`core:ui` `RichTextEditor`), the script editor, page documents, and fields in sheets, side panels and dialogs.

- **Caret line box**: from `getLineTop(line)` to `getLineBottom(line)` of the line holding the caret (`selection.end`; while a selection handle is dragged, the dragged end), in window coordinates. An empty field uses its first line.
- **Visible band** of the field's nearest vertical scroll container:
  - top = bottom of the top chrome over that container (top glass bar, sheet title row if pinned, banner) **+ 8 dp**;
  - bottom = the highest of: keyboard top, keyboard toolbar top, the bottom glass bar's top edge when drawn, a sheet's pinned action row top, `H − systemBottom`; then **− 16 dp**.
- **Correction** (`CaretInView.delta(lineTop, lineBottom, bandTop, bandBottom)`):
  - line inside the band: no scroll;
  - line below the band: scroll the content up by `lineBottom − bandBottom`;
  - line above the band: scroll the content down by `bandTop − lineTop`;
  - line taller than the band: align `lineTop` to `bandTop`.
  - The scroll is the **minimum** and **snaps** in the same frame as the change (no animation), so the line being typed keeps its place (DS-14 "never moves", SCR-14 fixed writing line).
- **When it runs**: on focus, every text change, caret move, the field growing or shrinking, the keyboard opening / closing / resizing, a sheet changing height, a bar appearing or leaving, fold / unfold. It does **not** run while the user is dragging or flinging the container (the user's scroll wins until the next text change or caret move).
- **Nested scroll**: a field with its own scroll (fixed max height) first scrolls itself so the caret line is inside the field's viewport, then the outer container applies the band rule to the caret line box.
- **Single-line fields** scroll horizontally so the caret stays inside the field's text area with 4 dp to spare.
- **Bottom glass bar while typing**: while a field that is **not** the command field has focus and the keyboard is up, the bottom glass bar is not drawn; the screen's keyboard toolbar (script) or nothing takes its place, and the band bottom is the keyboard (or toolbar) top − 16. The bar comes back when the keyboard closes. (The script already does this; it becomes the rule for every screen.)
- **Sheets**: the sheet rides on the keyboard (DS-66); if the band inside the sheet is shorter than the caret line box + 24 dp, the sheet first grows to its maximum height, then the band rule applies.

### K. 0.2.0 rules (2026-11-24) (0.2.0)

Source: the frozen scope `docs/status/0.2.0-scope.md` (ICON, U-15, N-13, U-16, S-4, D17, D28, D31) and the planning icon list (`icon-buttons.md`, 89 rows: a 15 · b 33 · c 41, with its icon set `icons_set.py`). The list lives outside the repository, so every row a test needs is copied here. Where this section and an earlier block disagree, this section wins; each superseded block carries a **(0.2.0)** line. Pure helpers with JVM tests (T-DS-46 … T-DS-53): `IconButtons.classOf`, `IconButtons.nameOf`, `IconId.forKey`, `CreateCard.canCreate`, `CreateCard.buttons`, `SearchChips.row`, `Tilt.offset`.

#### DS-70 · 아이콘 버튼 규칙 (icon buttons: a · b · c) — P0 · needsExternal: no · scope ICON

**Rule** (user decision 2026-11-24: "텍스트가 꼭 필요한 곳에서는 텍스트를 남겨두도록 해").

| Class | Shape | When | Accessibility name |
| --- | --- | --- | --- |
| **a** · icon only | DS-31 icon button: 44 × 44 touch, icon 20, stroke 1.6 (grid units), `ink2` | a universal meaning (send, search, close, previous, next, edit, undo) | the board's existing label first; else the **full old text** |
| **b** · icon + short text | icon 16 … 18 before the label, gap 6; button or pill height ≥ 44 | the picture alone is not universal | the **full old text**, even when the visible label is shortened |
| **c** · text as now | unchanged | confirm and delete actions, chips that show a picked value, flow buttons, anything ambiguous | the text |

- **When unsure, keep the text** (c). A row the list marks as an open question keeps its text until the user answers (D28).
- **Scope override**: the input row's send slot while editing (`고치기`) keeps its **text** (c), although the list draws icon `check`.
- Every a-class button exposes `contentDescription` = its accessibility name and `Role.Button`; a toggle-like b button (`설명`, `뒤집기`) exposes its on/off state (`stateDescription`).
- Long press on an a-class button shows its accessibility name as a tooltip (`[PROPOSED]`, DS-49 tooltip card without buttons, 1.5 s).
- Not changed (already icon-only or icon + text, accessibility name checked only): `back`, `⋯` (`More`, name `더 보기` `[PROPOSED]` where no board name exists), the recorder mic (`라이프 레코더`), record pause (`일시정지`), the U-16 view toggle cells, the card prev / next round buttons, menu rows, settings rows, map tool bar cells.

**a · icon only** (15 rows):

| # | Screen · button | Old text | Icon key | Accessibility name |
| --- | --- | --- | --- | --- |
| A1 | Script input row · send | `보내기` | `send` | `보내기` |
| A2 | Script input row · send while editing | `고치기` | — | **kept as text (c)**, scope ICON |
| A3 | Head (cover and inner) · search | `찾기` | `search` | `찾기 · 명령` |
| A4 | Head (inner) · display layers | `표시` | `layers` | `연출 · 메모 표시` |
| A5 | Search field and inner panel · close | `닫기` | `close` | `찾기 닫기` |
| A6 | Sheet and panel heads, about 30 places (블록 추가 · 표시 · 패널 · 렌즈 · 선 라벨 · 장소 · 관계 · 서재 · 지도 · 한꺼번에 · 갤러리) | `닫기` | `close` | `닫기` |
| A7 | 서재 head `+` | (none) | `plus` | `새 프로젝트` |
| A8 | Gallery viewer bottom row | `이전` · `다음` | `prev` · `next` | `이전` · `다음` |
| A9 | Gallery viewer caption panel | `고치기` | `edit` | `고치기` |
| A10 | Picked card caption panel | `보기` · `고치기` | `expand` · `edit` | `보기` · `고치기` |
| A11 | Map drawing tool bar | `되돌리기` | `undo` | `되돌리기` |
| A12 | Motion area drawing | `되돌리기` | `undo` | `되돌리기` (surface hidden in 0.2.0, S-3) |
| A13 | 프로젝트 홈 head left | `‹ 서재` | `back` | `서재로` |
| A14 | 롤로덱스 · previous / next | `이전` · `다음` | `up` · `down` | `이전 카드` · `다음 카드` |
| A15 | 롤로덱스 · opened card | `닫기` | `close` | `닫기` |

**b · icon + text** (33 rows; "same" = the accessibility name equals the visible text):

| # | Screen · button | Visible text | Icon key | Accessibility name |
| --- | --- | --- | --- | --- |
| B1 | Editing mark · right of the insert place | `나누기` · `합치기` | `split` · `merge` | `나누기` · `합치기`; disabled: `합치기 · 꺼짐 · <reason>` (`화자 다름` · `종류 다름`) |
| B2 | Above the input · next to the insert place | `다음 씬` | `next_scene` | `다음 씬 만들기` |
| B3 | Right swipe, 5 buttons | `고치기` · `이 뒤에` · `종류` · `씬 나누기` · `지우기` | `edit` · `insert_after` · `kind` · `scene_cut` · `trash` | same |
| B4 | Left swipe | `◆ 시간` · `참고` · `속성` | `◆` glyph · `note_ref` · `props` | same |
| B5 | Toast after a delete | `되돌리기` | `undo` | same |
| B6 | Left swipe, older boards | `참고` · `속성` | `note_ref` · `props` | same |
| B7 | Block selection tool row | `복사` · `잘라내기` · `옮기기` · `지우기` | `copy` · `cut` · `move` · `trash` | same |
| B8 | List head create row (인물 · 세계 · 연표 · 에셋) | `새 인물` (per list: `새 장소` · `새 용어` · `새 세력` …) | `plus` | same (U-15) |
| B9 | Story bottom row | `새 씬` | `plus` | same |
| B10 | Cast list view switch | `목록` · `관계도` | `list` · `graph` | same |
| B11 | Membership record sheet | `시점` | `plus` | `시점 추가` |
| B12 | Character record · add field | `항목` | `plus` | `항목 넣기` |
| B13 | Gallery viewer centre | `설명` | `caption` | `설명` (with on/off state) |
| B14 | Gallery sort row | `고르기` | `select_box` | `여러 장 고르기` |
| B15 | Gallery reorder | `맨 앞` · `맨 뒤` | `to_top` · `to_bottom` | same |
| B16 | Add and hidden cells | `사진` · `파일` · `다시 잠그기` | `image` · `file` · `lock` | same |
| B17 | Image pick | `갤러리` · `파일` | `image` · `file` | same |
| B18 | Card off screen | `고른 카드로` | `prev` | same |
| B19 | Profile gallery cell | `넣기` | `plus` | same |
| B20 | Crop tools | `90° 회전` · `좌우 반전` · `처음대로` | `rotate` · `mirror` · `reset` | same |
| B21 | Hidden or locked cell | `지문으로 보기` · `잠금 풀기` | `fingerprint` | same (D31) |
| B22 | Life recorder | `말로 기록` · `글로 쓰기` | `mic` · `write` | same |
| B23 | Map generator | `다른 후보 3` | `dice` | same (surface hidden, S-3) |
| B24 | Empty inbox · mic off | `말로 기록` · `글로 쓰기` (`글로` becomes `글로 쓰기`) | `mic` · `write` | same |
| B25 | Map generator tools | `새 후보` · `강만 다시` · `강 고정` · `새 지역` · `조건` | `dice` · `lock` · `plus` | same (hidden, S-3) |
| B26 | Map generator step palette | `해안선` · `산` · `강` · `한 칸` · `채우기` · `되돌리기` … | terrain glyphs | same (hidden, S-3) |
| B27 | 원고 더미 tools | `들어 보기` · `내려놓기` · `모으기` · `펼치기` | `lift` · `drop` · `gather` · `fan` | same |
| B28 | New project cell in a 서재 view | `새 프로젝트` | `plus` | same |
| B29 | 부채꼴 덱 · under the card | `뒤집기` | `flip` | same (with on/off state) |
| B30 | Card open | `열기` | `open_out` · `expand` | same |
| B31 | 타로 펼침 tools | `모두 뒤집기` · `다시 펼치기` | `flip` · `redeal` | same |
| B32 | 롤로덱스 head `+` | `새 인물` | `plus` | same (D31: U-15 wins over the narrow head) |
| B33 | Card wall · new card cell | `새 카드` | `plus` | same |

**c · text as now** (41 rows, by kind): speaker and kind chips (`리엔 ▾` · `대사 ▾`), the scene chip `이 씬 · 1-7`, every filter chip, tab and segmented cell; confirm and delete actions (`취소`, `완료`, `합치기`, `바꾸기`, `휴지통으로`, `비우기`, `나누기` · `합치기` · `옮기기` · `기록` · `적용` · `고르기` · `정하기` · `넣기` on sheet bottoms, `기록 지우기` · `다른 블록에 잇기` · `이대로 두기`); flow buttons (`다음` · `이전` · `건너뛰기` · `완료`, `시작하기`, `예시 홈으로` · `온보딩 끝내기`); inline text links (`바꾸기 ›` · `쓰기 ›` · `기본 ›` · `전체 보기 ›` …); screen primary actions (`인물 추가`, `PDF 만들기`, `설명 쓰기`, `멈추고 저장` · `이어서 말하기`, `기록함에 두기`, `여기 넣기 · #044 뒤`); the U-16 picked-item buttons (`열기` · `만들기` · `꺼내기` · `앞으로` · `제자리` · `돌려놓기`) and card primary buttons (`인물 페이지` …); lock alternatives `PIN 사용` · `기기 PIN 사용`. The rating board's `바꾸기` is gone with the rating (S-4).

*Sources:* scope ICON, D28, D31; decision row `아이콘` (2026-11-24); planning `icon-buttons.md`.

#### DS-71 · 아이콘 세트 r2 (icon set additions) — P0 · needsExternal: no

- The r2 set (2026-11-24) is newer than DS-12 and **wins for every key it defines**. Same drawing rule as DS-12 (24 grid, stroke 1.6, round caps and joins, no fill). `IconId.forKey(key)` maps each r2 key to one `IconId`; existing ids are kept (none renamed or removed) and take the r2 path where the r2 set draws the same meaning.

| r2 key | `IconId` | Path data | Note |
| --- | --- | --- | --- |
| `send` | `Send` **new** | `M12 19V6M6.5 11.5L12 6l5.5 5.5` | |
| `check` | `Check` | `M5 12.5l4.5 4.5L19 7.5` | replaces DS-12 path |
| `search` | `Search` | `M10.5 17a6.5 6.5 0 1 0 0-13 6.5 6.5 0 0 0 0 13zM20 20l-4.5-4.5` | replaces DS-12 path (the r2 set restores the `gen_nm.py` shape) |
| `layers` | `Layers` | `M12 4l9 5-9 5-9-5zM3 14l9 5 9-5` | same |
| `close` | `X` | `M6.5 6.5l11 11M17.5 6.5l-11 11` | replaces DS-12 path; chip remove uses it too |
| `split` | `Split` | `M12 4v16M4 12h5M6.5 9.5L4 12l2.5 2.5M20 12h-5M17.5 9.5L20 12l-2.5 2.5` | replaces DS-12 path |
| `merge` | `Merge` | `M12 4v16M3 12h5M5.5 9.5L8 12l-2.5 2.5M21 12h-5M18.5 9.5L16 12l2.5 2.5` | replaces DS-12 path |
| `next_scene` | `NextScene` **new** | `M4 6h12M4 10h12M4 14h7M17 13v7M13.5 16.5h7` | |
| `edit` | `Edit` **new** | `M4 20h4L19 9l-4-4L4 16zM13.5 6.5l4 4` | |
| `insert_after` | `InsertAfter` **new** | `M5 6h14M5 11h14M12 14v6M9 17h6` | |
| `kind` | `Kind` **new** | `M4 7h10M4 12h16M4 17h7M17 4l3 3-3 3` | |
| `scene_cut` | `SceneCut` **new** | `M4 5h16M4 19h16M4 12h3M10.5 12h3M17 12h3` | |
| `trash` | `Trash` | `M5 7h14M10 7V5h4v2M7 7l1 12h8l1-12` | same |
| `note_ref` | `Paper` | `M5 5h14v10h-8l-4 3.5V15H5z` | same |
| `props` | `Props` | `M4 7h16M4 12h16M4 17h10` | same |
| `copy` | `Copy` **new** | `M8 8h11v11H8zM5 16V5h11` | |
| `cut` | `Cut` **new** | `M6 7a2.5 2.5 0 1 0 0 .01M6 17a2.5 2.5 0 1 0 0 .01M8 8.5L19 18M8 15.5L19 6` | |
| `move` | `MoveBlock` **new** | `M12 4v16M8 8l4-4 4 4M8 16l4 4 4-4` | DS-12 `Move` stays the map pan tool |
| `undo` | `Undo` | `M9 14L4 9l5-5M4 9h10a6 6 0 0 1 0 12h-3` | same |
| `plus` | `Plus` | `M12 5v14M5 12h14` | same |
| `list` | `List` | `M4 7h16M4 12h16M4 17h16` | replaces DS-12 path |
| `graph` | `Graph` **new** | `M8.5 6.5a2.5 2.5 0 1 1-5 0 2.5 2.5 0 0 1 5 0zM20.5 9a2.5 2.5 0 1 1-5 0 2.5 2.5 0 0 1 5 0zM12.5 18a2.5 2.5 0 1 1-5 0 2.5 2.5 0 0 1 5 0zM8.4 7.1l7.2 1.4M6.6 8.9l2.6 6.8M16.4 10.8l-4.6 5.4` | |
| `back` | `Back` | `M15 5l-7 7 7 7` | same; head left only |
| `prev` | `Prev` **new** | `M15 5l-7 7 7 7` | same shape as `back`, used in bottom rows and card controls (D31) |
| `next` | `Next` | `M9 5l7 7-7 7` | same |
| `up` · `down` | `Up` · `Down` **new** | `M5 15l7-7 7 7` · `M5 9l7 7 7-7` | |
| `to_top` · `to_bottom` | `ToTop` · `ToBottom` **new** | `M5 4.5h14M12 20V9M7 14l5-5 5 5` · `M5 19.5h14M12 4v11M7 10l5 5 5-5` | |
| `select_box` | `SelectBox` **new** | `M5 5h14v14H5zM8.5 12l2.5 2.5 4.5-5` | |
| `more` | `More` | `M5 12h.01M12 12h.01M19 12h.01` | same |
| `caption` | `Caption` **new** | `M5 6h14M5 11h14M5 16h9` | |
| `expand` | `Expand` **new** | `M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5` | |
| `image` | `Image` | `M4 5h16v14H4zM4 16l5-5 4 4 3-3 4 4` | same |
| `file` | `File` | `M6 3h8l4 4v14H6zM14 3v4h4` | replaces DS-12 path |
| `rotate` · `reset` | `Rotate` · `Reset` | as DS-12 | same |
| `mirror` | `Flip` | `M12 3v18M8 7l-4 5 4 5M16 7l4 5-4 5` | DS-12 `Flip` **is** the r2 `mirror` (crop `좌우 반전`) |
| `flip` | `CardFlip` **new** | `M4 12a8 8 0 0 1 14-5.3M20 12a8 8 0 0 1-14 5.3M18 3v4h-4M6 21v-4h4` | card `뒤집기`; not the crop mirror |
| `lock` · `fingerprint` · `mic` · `pause` · `dice` | `Lock` · `Fingerprint` · `Mic` · `Pause` · `Dice` | as DS-12 | same |
| `write` | `Text` | `M5 6h14M12 6v12M9 18h6` | same (`글로 쓰기`) |
| `redeal` | `Redeal` **new** | `M4 12a8 8 0 1 0 2.4-5.7M4 4.5v4h4` | |
| `open_out` | `OpenOut` **new** | `M14 5h5v5M19 5l-8 8M10 6H6a1 1 0 0 0-1 1v11a1 1 0 0 0 1 1h11a1 1 0 0 0 1-1v-4` | |
| `fan` · `gather` | `Fan` · `Gather` **new** | `M12 20L5 7M12 20l7-13M12 20V5` · `M5 9l7-4 7 4-7 4zM5 13l7 4 7-4M5 17l7 4 7-4` | |
| `lift` · `drop` | `Lift` · `Drop` **new** | `M4 17h16M4 20.5h16M7 6.5h10v6H7zM9.5 4L12 2l2.5 2` · `M4 17h16M4 20.5h16M7 8h10v5.5H7zM9.5 3L12 5l2.5-2` | |

- Look-alikes that stay apart because each sits next to its own text (b): `rotate` · `reset` · `redeal`; `props` · `caption`; `gather` · `layers`. No two a-class icons share a shape except `back` / `prev`, which are split by place (D31).

#### DS-72 · 만들기 카드 (create card overlay) — P0 · needsExternal: no · scope U-15, D17

One shared overlay in `core:designsystem`; each feature supplies its title, its fields and the write. Behaviour of each kind (field sets, what is created) is the feature's (U-15.5, U-15.7, U-15.9).

- **Where it opens**: from a list's create row (`＋ 새 인물` …) and from a picker sheet's create row. The screen under it does not change (U-15.2).
- **Shape** (cover and inner alike, U-15.8): a centred floating card over the `shade` scrim. Fill `raise`, radius `card` 20, 1 dp `line` inset, shadow `shadow-float`; width = window − 2 × 16 on cover, max 420 on inner; padding 20. It matches the character card view's look (same radius, fill and image slot as the profile card head).
- **Anatomy**, top to bottom: title (`dialogTitle` role), then the fields (each with a **visible label** above it, never a placeholder alone), then the button row. Title and button row are pinned; the field area scrolls when it does not fit.
- **Title**: `새 <kind word>` from the create row (`새 인물`, `새 장소`, `새 용어`, `새 세력`; and by the same pattern `새 사건`, `새 에셋`, `새 태그`, `새 관계 종류`, `새 페이지`, `새 캔버스`, listed to confirm in §6 Q-DS-30).
- **Keyboard** (U-15.8): the card's bottom stays ≥ 8 dp above the keyboard top (DS-66 `aboveKeyboard`), and ≥ `systemBottom + 16` without it. When the card is taller than the space, the field area shrinks and scrolls, and DS-69 keeps the caret line visible. No field is ever under the keyboard.
- **Focus**: on open the first text field (the name) has focus and the keyboard opens (`[PROPOSED]`); in a picker with a query the name is prefilled with the query (U-15.3) and the caret sits at its end.
- **Buttons** (`CreateCard.buttons(mode)`, c-class text): list mode `취소` (ghost) · `만들고 열기` (ghost) · `만들기` (primary); picker mode `취소` · `만들기`. `만들기` and `만들고 열기` are enabled only while the required field (the name) is non-blank after trim (`CreateCard.canCreate`).
- **After a create**: list mode `만들기` closes the card, keeps the list where it was, and highlights the new row for 1 200 ms (`hl` fill fading out through `CivilightMotion.standard`; reduced: no fade, removed after the same time). If the new row is outside the visible part of the list, the list scrolls the minimum to show it first (`[PROPOSED]`). `만들고 열기` closes the card and opens the new item. Picker mode `만들기` closes the card and the picker, with the new item picked (U-15.3, U-15.6).
- **Dismiss**: `취소` and system back close the card and discard what was typed (explicit acts). A scrim tap closes it only while every field is empty; with typed content a scrim tap does nothing (`[PROPOSED]`: no silent loss, no confirm dialog). The list's scroll position is unchanged (U-15.2).
- **Write**: one call to the feature's write (through its `DECISIONS.md` §1 path), one undo step; no spinner (writes are under 100 ms).
- **Motion**: enter `CivilightMotion.enter` (fade + scale 0.96 → 1), exit `CivilightMotion.exit` (fade); scrim fades with them; reduced: 120 ms fades. MotionLint applies.
- **Accessibility**: dialog semantics with the title as pane title; focus stays inside the card; on close focus returns to the create row that opened it.
- **Read-only**: create rows are not drawn in read-only projects (U-15.10); the overlay is never opened there.
- **Create row** (list top and picker top, U-15.1, U-15.3): a list row 48 tall under the search field and filter chips, b-class: icon `plus` 18 + label (`새 인물`, or in a picker with an unmatched query `‘세라’ 새 인물`; the quote marks follow the app's `‘…’` style). Label and icon `acc`; pressed state per DS-68; no chevron. Accessibility name = the label.

#### DS-73 · 찾기 칩 줄 (search chip row) — P1 · needsExternal: no · scope N-13

- Row (STO search sheet, behaviour STO): chips `전체 · 인물 · 용어 · 대사 · 씬 · 명령` (single choice) followed by the **dropdown chip** `그 밖 ▾`. When `대사` is selected a second dropdown chip `화자 ▾` appears directly after `대사`.
- **Layout**: the row **wraps** (flow row, chip gap 6, line gap 6); it never scrolls sideways (N-13 acceptance at cover width). DS-36 chip shape; dropdown chips add ` ▾` to their text.
- **`그 밖 ▾`**: tap opens a popover (DS-26; on cover a small sheet when it would cover the field) listing `장소 · 세력 · 사건 · 페이지`, single choice. Picking one selects that kind for the whole row (the other chips deselect) and the chip shows the picked value, `장소 ▾` (c: a chip that shows a picked value). Picking any of the six chips returns it to `그 밖 ▾`, unselected.
- **`화자 ▾`**: tap opens the speaker picker sheet (same rows as `화자 고르기`; hidden characters left out while locked) with a first row `전체`. Picking a character shows `<name> ▾` (display name rules of CST) and limits results to that speaker's lines; `전체` clears it. Leaving `대사` removes the chip and clears the speaker.
- `SearchChips.row(selected, extraKind, speaker)` returns the chip models in order with their text and selection. No recorder kind exists in the row (N-13: records are not searched).
- Accessibility: dropdown chips read their visible text and expose "opens a list" (`Role.DropdownList`).

#### DS-74 · 3D 보기의 바탕 (3D view primitives and tilt) — P1 · needsExternal: no · scope U-16

- Shared pieces for the six 서재 views (LIB-93) and the six character card views (CST): `rememberTilt(enabled)`, `Modifier.parallax(tilt, depth)`, `Modifier.sheen(tilt)`, and the view-toggle cell model.
- **Tilt source**: the rotation-vector sensor, registered only while `enabled` and the host is resumed and visible; unregistered on pause. Output `(tx, ty)` in −1 … 1, from device pitch and roll relative to the pose when the view appeared, clamped at ±12°, low-pass filtered (time constant 120 ms) `[PROPOSED]`. `enabled = Tilt.follows(viewTilt, motion)` (00 §13.4: the LIB-94 switch on and motion not reduced); when false no sensor is registered and the output is `(0, 0)`.
- **Parallax**: `Tilt.offset(tilt, depth) = (tx × depth, ty × depth)`, depth ≤ 8 dp (LIB-04's 8 % image parallax rule, `[PROPOSED]` value). **Light**: a sheen band offset by the same tilt (the holo material's rule, DS-54).
- **Frame**: every transform is clipped to the view's frame; nothing moves over the head, the bars or the list below (U-11 rule).
- **Motion**: transitions between views and items use `CivilightMotion` tokens only (MotionLint); reduced motion → 120 ms fades, no idle motion, no tilt; the LIB-94 tilt switch off → no tilt, other motion unchanged.
- **Toggle cell**: DS-39 with the second cell's icon `Grid` and accessibility name = the chosen view's label (`3D 책장` …).

#### DS-75 · 0.2.0 notes on earlier blocks — P1 · needsExternal: no

- **DS-20 rating badge**: not drawn anywhere in 0.2.0 (S-4, LIB-92). `RatingBadge` and `BadgeSurface` stay in code, unused. The `r18` colour keeps its other uses (hard-to-undo confirm buttons, error marks per Q-DS-05).
- **DS-29 command bar mic**: the mic is drawn only on 프로젝트 홈's command bar (S-8, LRC-48). Every other command bar omits it; the command field takes the freed width.
- **DS-39 ViewToggle**: on 서재 and the cast list it switches between the chosen 3D view and the list (DS-74).
- **DS-12 icons**: the r2 paths of DS-71 replace the listed DS-12 paths.

### Tests (0.2.0)

**T-DS-46 · icon classes (DS-70).** For each A-row (except A2) `IconButtons.classOf(id) = A`, `nameOf(id)` equals the table's accessibility name and is non-blank; A2 (`고치기` send) is `C`. For each B-row the accessibility name equals the table's (for B11 `시점 추가` although the text is `시점`; for B14 `여러 장 고르기` although the text is `고르기`).

**T-DS-47 · icon set (DS-71).** `IconId.forKey` is defined for all 51 r2 keys; `forKey("mirror") = Flip`; `forKey("flip") = CardFlip`; `forKey("move") = MoveBlock`; `forKey("close") = X`; `forKey("prev").pathData == forKey("back").pathData`; `Check.pathData = "M5 12.5l4.5 4.5L19 7.5"`; every `IconId` path parses with `PathParser`.

**T-DS-48 · touch targets (DS-70).** Every a-class button model has a touch area ≥ 44 × 44 and icon size 20; every b-class icon is 16 … 18.

**T-DS-49 · create card buttons (DS-72).** `CreateCard.buttons(LIST) = [취소, 만들고 열기, 만들기]`; `buttons(PICKER) = [취소, 만들기]`; `canCreate(name = "  ") = false`; `canCreate("세라") = true`.

**T-DS-50 · create card gap (DS-72).** Window 844, keyboard top 520: card bottom ≤ 512; card height 600 → field area height = available − title − buttons, and the title and button rows stay inside `[top + 8, 512]`.

**T-DS-51 · search chips (DS-73).** `row(selected = 전체)` → 7 chips, last `그 밖 ▾`; `row(selected = 대사)` → 8 chips, `화자 ▾` at index 4; `row(extraKind = 장소)` → last chip text `장소 ▾`, selected, and none of the six selected; `row(selected = 대사, speaker = 리엔)` → chip text `리엔 ▾`.

**T-DS-52 · tilt (DS-74).** `Tilt.offset((0.5, −1.0), depth = 8) = (4, −8)`; a 20° pitch is clamped to `ty = ±1`; with `enabled = false` the source registers no listener and returns `(0, 0)`; `Tilt.follows(false, NEVER_REDUCE) = false`, `Tilt.follows(true, ALWAYS_REDUCE) = false`, `Tilt.follows(true, NEVER_REDUCE) = true`.

**T-DS-53 · no badge (DS-75).** No 0.2.0 screen model (서재 row, card, home head, export sheet) carries a rating badge model.

---

## 3. Data

### 3.1 What this area owns

The design system **persists nothing**. It owns code-level value types (in `core/designsystem`) that other areas consume, and it reads a few app preferences owned by LIB / CNV.

| Type | Kind | Fields (type, nullability) | Meaning |
| --- | --- | --- | --- |
| `CivilightColors` | data class, one instance per theme | `bg, raise, ink, ink2, ink3, acc, pri, onpri, sel, hl, tint, line, edge, chip, glass, shade, warn, r18, onr18`: `Color`, non-null | DS-02 tokens |
| `FixedColors` | object + per-theme lists | `navyCore, onImage, onImage2, onImageBadge, onImageR18, imageShadeTop, imageShadeBottom, imageChipFill, cropOutline, cropGrid, cropDim`: `Color`; `mapArt: List<Color>` (size 5, per theme); `terrain: Map<Terrain, Color>` (10 entries: DEEP, SEA, PLAIN, FOREST, HILL, MOUNT, PEAK, DESERT, SWAMP, SNOW); `recorderGlow: List<Color>` (size 7, per theme) | DS-03 |
| `CivilightType` | data class | one `TextStyle` per role of DS-06 (`hero, display, displayM, pageTitle, stateTitle, title, dialogTitle, headerTitle, headerTitleInner, heading, itemTitle, sceneHead, line, body, lead, field, recorderLive, stat, command, label, tab, button, buttonLarge, chip, speaker, speakerName, small, paren, micro, sec, number, dir, badge, wordmark`), non-null | DS-06 |
| `TextScale` | value class | `manuscriptBaseSp: Float` (default 18) | DS-07; derived from `AppSettings.textSizeStep` |
| `CivilightSpacing` | data class | `s1 = 4, s2 = 8, s3 = 12, s4 = 16, s5 = 20, s6 = 24, s7 = 26` (`Dp`) | DS-09 |
| `CivilightShapes` | data class | `xs = 5, sm = 12, cmd = 14, md = 16, card = 20, dialog = 26, lg = 28, full` (`Dp` / `RoundedCornerShape`) | DS-10 |
| `CivilightSizes` | data class | `hitMin = 44, cmdH = 52, measure = 560, cardW = 222, mapTool = 52, vertex = 14, icon = 20, mic = 32, grab = 36 × 4` (`Dp`) | DS-10 |
| `CivilightBlur` | data class | `glass = 36, edge = 22, back = 2, chip = 12` (`Dp`), `glassSaturation = 1.4f` | DS-10, DS-22 |
| `CivilightElevation` | data class | `floatDark`, `floatLight`: shadow (offsetY 20 dp, blur 50 dp, colour) | DS-10 |
| `CivilightMotion` | object | `standard, enter, exit, linear`: (easing, durationMs); `reducedFadeMs = 120`, `foldOpenMs = 360`, `foldCloseMs = 240`, `enterLongMs = 360`, `swipeMs = 260`, `swipeStaggerMs = 60`, `holoSweepMs = 480`, `sheetBackScale = 0.965f`, `sheetBackBlur = 2.dp`, `unhideBlur = 7.dp`, `unhideMs = 320` | DS-14 |
| `HapticToken` | enum | `TICK`, `CONFIRM` | DS-15 |
| `MarkerKind` | enum | `CHANGE (◆), NOTICE (◇), WARNING (▲), DIRECTION (▸), BGM (♪), MEMO (⌁)` | DS-16, DS-17 |
| `MarkerLine` | enum | `SOLID`, `DOTTED` | DS-18 |
| `InlineDecoration` | enum | `LINK, CANDIDATE, CANDIDATE_SELECTED, HIDDEN_LINK, DELETED_REF, SEARCH_HIT, PAREN` | DS-18, DS-19, DS-45 |
| `BadgeSurface` | enum | `PLAIN`, `ON_IMAGE` | DS-20 |
| `ButtonVariant` | enum | `PRIMARY, GHOST, DANGER, PRIMARY_LARGE, COMPACT_PRIMARY, COMPACT_GHOST, TWO_LINE, TWO_LINE_GHOST, ON_TOAST, ON_IMAGE` | DS-30 |
| `LensChipState` | enum | `LATEST`, `PAST`, `SCRIPT_POSITION` | DS-38 |
| `IconId` | enum | every id of DS-12, plus the **new** ids of DS-71 (0.2.0); `forKey(r2Key)` | DS-12, DS-71 |
| `IconClass` | enum + `IconButtons.classOf` · `nameOf` (0.2.0) | `A`, `B`, `C` per DS-70 table rows | DS-70 |
| `CreateCardMode` | enum (0.2.0) | `LIST`, `PICKER`; `CreateCard.buttons(mode)`, `canCreate(name)` | DS-72 |
| `Tilt` | value + object (0.2.0) | `tx, ty: Float` in −1 … 1; `follows(viewTilt, motion)`, `offset(tilt, depth)` | DS-74 |
| `CommandScope` | enum + pure reducer | `LIST`, `DETAIL`; `next(current, event)` with events `DETAIL_OPENED`, `DETAIL_REPLACED`, `DETAIL_CLOSED`, `PICKED(scope)`, `PANE_LOCKED(scope)`, `TOUCHED_IN(scope)` | DS-65 |
| `CommandBarHost` | state holder (core:ui) | per pane: `declared: Boolean`, `title: String?` (masked as shown), `hasExtraRow: Boolean`, `locked: Boolean`; `layout(...)` → `barCount`, `showsSwitch`, `cells`, `barHeight` | DS-65 |
| `OverlayGaps` | object (`Dp`) | `edgeCompact = 16, edgeInner = 24, aboveBar = 6, aboveKeyboard = 8, sheetBottom = 24, sheetBottomIme = 16, sideCompact = 12, sideInner = 24, top = 8, dialogBand = 24, toastLiftMin = 156`; pure `toastBottom`, `sheetBottomPadding`, `popoverY`, `dialogBand` | DS-66, DS-67 |
| `NoticeKind` | enum | `INFO`, `ACTION`, `ERROR` | DS-67 |
| `NoticeRules` | object | `durationMs(kind)`: 2 500 / 4 000 / 4 000; `dismissOn(event)` | DS-67 |
| `Affordance` | enum + pure `of(element)` | `CONTAINER`, `CHEVRON`, `CONTROL`, `ICON_BUTTON`, `INLINE_LINK`, `DECORATION`, `NONE` | DS-68 |
| `CaretInView` | object | `delta(lineTop, lineBottom, bandTop, bandBottom): Float`, `MARGIN_TOP = 8`, `MARGIN_BOTTOM = 16` (dp) | DS-69 |

**Inputs read (owned elsewhere):** `AppSettings.theme: ThemeMode {SYSTEM, LIGHT, DARK}` and `AppSettings.textSizeStep` (LIB-65); `AppPrefs.animation`, `AppPrefs.reduceTransparency`, `AppPrefs.recorderGlow` and the resolved `GlassMode {BLUR, BLUR_COSTLY, SCRIM, OPAQUE}` / motion mode (CNV-46); the system dark-mode flag and API level.

### 3.2 Relations to other areas' entities

| From (other area) | Drives | Notes |
| --- | --- | --- |
| Project rating `ALL · R-15 · R-18` (LIB, rule 19) | DS-20 badge colours | R-18 → `r18` / `onImageR18` |
| Character display name and real name (CST display-name rule) | DS-42 speaker-line parts | `???` + real name in `ink3` |
| Change record anchor state: anchored · `근거 지워짐` · `대상 지워짐` (SCR rule 12, CST rule 20) | DS-16 / DS-18 marker line `SOLID` / `DOTTED` | colour stays the marker's |
| Reference to a trashed or permanently deleted item (CST rule 20) | DS-18 dotted name + `◇` | restore → solid |
| Term link / candidate / `아님` state (WLD rule 21) | DS-19 decoration | candidate underline only when the `용어 후보` layer is on |
| Hidden item / project state (LIB rule 19) | DS-19 hidden link label, DS-41 lock row | masked names never reach the UI tree |
| Check item severity (STO / CNV-17) | DS-16 `▲` vs `◇` | `▲` always `warn` |
| Search match ranges (STO search) | DS-45 boxes | ranges in UTF-16 indices of the displayed sentence |
| Travel-check values (STO rule 18) | DS-60 bar widths | longest gap ÷ required |
| Image, crop and holo flag (CNV-25 … CNV-30) | DS-51 … DS-54 | — |

### 3.3 `.civilight` file

Nothing from this area is stored in `.civilight`. Theme, text size, motion and transparency choices are app settings (LIB AppSettings, backed up only in the app-settings backup file, never exported). Preset image names (`preset:…`) belong to CNV-26 and the file format.

---

## 4. Rules as test cases

**(0.2.0)** Tests T-DS-46 … T-DS-53 for section K sit at the end of section K.

All tests are JVM unit tests against the token objects and pure helper functions (`resolveTheme`, `contrast`, `hitArea`, `markerStyle`, `badgeStyle`, `lensChipStyle`, `speakerLabelParts`, `searchHitRuns`, `travelBarFraction`, `avatarGlyphSp`, `iconStrokeDp`, `lintString`). Contrast uses WCAG 2.x relative luminance; alpha tokens are composited over `bg` with the alpha rounding of the Units convention; ratios are compared at two decimals unless stated.

**T-DS-01 · dark tokens.** Given `CivilightColors.dark`, then `bg = #0B0C0E`, `raise = #16181C`, `ink = #ECECEF`, `ink2 = #B4B6BD`, `ink3 = #8E9099`, `acc = #8FB8E6`, `pri = #8FB8E6`, `onpri = #031C35`, `warn = #F2B66B`, `r18 = #FF6B5E`, `onr18 = #0B0C0E`, `chip = #2A2D33`, and ARGB `sel = 0x1F8FB8E6`, `hl = 0x388FB8E6`, `tint = 0x12FFFFFF`, `line = 0x17FFFFFF`, `edge = 0x61FFFFFF`, `glass = 0x94101114`, `shade = 0x73000000`.

**T-DS-02 · light tokens.** Given `CivilightColors.light`, then `bg = #F6F5F1`, `raise = #ECEAE4`, `ink = #16171A`, `ink2 = #4A4C52`, `ink3 = #62646B`, `acc = #1F5590`, `pri = #031C35`, `onpri = #F6F5F1`, `warn = #8A5A00`, `r18 = #B3261E`, `onr18 = #FFFFFF`, `chip = #E2E0D9`, and ARGB `sel = 0x12031C35`, `hl = 0x211F5590`, `tint = 0x0D16171A`, `line = 0x1A16171A`, `edge = 0x8016171A`, `glass = 0xA8F6F5F1`, `shade = 0x2E1E1E1C`.

**T-DS-03 · alpha rounding.** Given `rgba(22,23,26,.5)`, when converted, then alpha byte = `floor(0.5 × 255 + 0.5) = 128` (`0x80`). Given `rgba(22,23,26,.10)`, then `26` (`0x1A`). Given `rgba(16,17,20,.58)`, then `148` (`0x94`).

**T-DS-04 · theme resolution.** `resolveTheme(SYSTEM, systemDark = true) = DARK`; `resolveTheme(SYSTEM, false) = LIGHT`; `resolveTheme(LIGHT, true) = LIGHT`; `resolveTheme(DARK, false) = DARK`. `colorsFor(DARK).bg = #0B0C0E`, `colorsFor(LIGHT).bg = #F6F5F1`.

**T-DS-05 · faintest text.** `contrast(ink3, bg)`: dark `6.15`, light `5.42`; both `≥ 5.40` (planning 접근성 "글자는 바탕 대비 5.4:1 이상").

**T-DS-06 · accent marks (board `Accent`).** `contrast(acc, bg)`: dark `9.47` → printed `9.5:1`; light `6.97` → printed `7.0:1` (assert `round1(6.97) == 7.0`, not `≥ 7.00`).

**T-DS-07 · navy-core is never a mark.** `contrast(#031C35, light ink #16171A) = 1.04`; `contrast(#031C35, dark bg #0B0C0E) = 1.14`. Then for both themes `acc != #031C35`, and `pri(light) == #031C35`, `onpri(dark) == #031C35`.

**T-DS-08 · meaningful borders.** `edge` composited on `bg`: dark `#68686A`, light `#868686`; `contrast(edge, bg)`: dark `3.52`, light `3.34`; both `≥ 3.00`.

**T-DS-09 · body text.** `contrast(ink, bg)`: dark `16.60`, light `16.43`; `contrast(ink, raise)`: dark `15.07`, light `14.90`. `contrast(ink2, bg)`: dark `9.66`, light `7.87` (`≥ 7.00`, tokens "bg 위 7:1 이상").

**T-DS-10 · fills with text.** `contrast(onpri, pri)`: dark `8.32`, light `15.76`. `contrast(onr18, r18)`: dark `7.00`, light `6.54`. Toast `contrast(bg, ink)`: dark `16.60`, light `16.43`.

**T-DS-11 · warn and r18 as text.** `contrast(warn, bg)`: dark `10.88`, light `5.43`; `contrast(r18, bg)`: dark `7.00`, light `5.99`; all `≥ 5.40`.

**T-DS-12 · marker colours.** For both themes: `markerStyle(CHANGE).color == acc`, `markerStyle(NOTICE).color == ink3`, `markerStyle(WARNING).color == warn`, `markerStyle(DIRECTION | BGM | MEMO).color == ink3`. `markerStyle(WARNING).color != acc` and `markerStyle(CHANGE).color != warn` (◆ and ▲ never share a colour).

**T-DS-13 · dotted reference (rule 20, 노아).** Given 노아 is in the trash and block 1-6 씬 2 has speaker 노아, when the speaker label is styled, then text `노아` keeps colour `ink2`, has `underline = DOTTED`, and is followed by `◇` in `ink3`. When 노아 is restored, then the underline is `NONE` and no `◇` follows.

**T-DS-14 · dotted mark (rule 12, #043).** Given the ◆ record `관계 · 리엔의 마음 → 하르트` anchored at `#043`, when `#043` is moved to the trash and the record moves to `#044` with state `근거 지워짐`, then `#044` shows `markerStyle(CHANGE, DOTTED)` = dashed outline diamond (stroke 1.5, dash 2 / 1.8 in the 14-unit box), colour `acc`, accessibility label `근거 지워짐`, followed by the solid `◇` of `#044`'s own first reveal (오리지늄 등불). When `#043` is restored, then the record re-attaches to `#043` with `SOLID`.

**T-DS-15 · rating badge.** `badgeStyle(ALL, PLAIN)` = outline `ink3`, text `ink2`; `badgeStyle(R15, PLAIN)` = outline `ink3`, text `ink2`; `badgeStyle(R18, PLAIN)` = outline `r18`, text `r18`. `badgeStyle(R15, ON_IMAGE)` = outline and text `rgba(244,244,246,.75)`; `badgeStyle(R18, ON_IMAGE)` = outline and text `#FF8A80`. Sample: 「잿빛 새벽의 등불」 shows `R-15` with outline `ink3`; 「검은 서약」 shows `R-18` with `r18`.

**T-DS-16 · lens chip.** `lensChipStyle(LATEST)` (`최신 · 1-7`) = outline `line`, text `ink2`. `lensChipStyle(PAST)` (`시점 · 1-5`) = outline `acc`, text `acc`. `lensChipStyle(SCRIPT_POSITION)` (`이 씬 · 1-7`) = no outline, no right padding, text `ink2`.

**T-DS-17 · hit areas.** `hitArea(chip 52 × 30) = 60 × 44`; `hitArea(chip 30 × 30) = 44 × 44`; `hitArea(lens 85 × 32) = 93 × 44`; `hitArea(switch 46 × 28) = 54 × 44`; `hitArea(mic 32 × 32) = 44 × 44`; `hitArea(radio 22 × 22) = 44 × 44` (fixed); `hitArea(vertex 14 × 14) = 44 × 44` (fixed); `hitArea(lockToggle 36 × 36) = 44 × 44` (+4 each side); `hitArea(iconButton 44 × 44) = 44 × 44`.

**T-DS-18 · default text size.** Given `textSizeStep = 보통` (18), then `line` = 18 sp / 31 sp, `body` = 16 sp / 28 sp, `paren` = 14 sp, and `line` line height ÷ size ≈ 1.72 (planning "본문 18 · 행간 1.7").

**T-DS-19 · speaker line parts (board `Wide`).** `speakerLabelParts(displayName = "???", realName = "하르트", selected = true, blockNo = "#043", markers = [CHANGE])` → `[("???", speakerName, ink), ("하르트 · #043", speaker, ink3), (◆, acc, alignEnd)]`. `speakerLabelParts("리엔", "리엔", selected = false, …, markers = [])` → `[("리엔", speakerName, ink2)]` (no duplicate real name).

**T-DS-20 · search hit stays inline.** Given sentence `“오리지늄은 타지 않아. 빛만 남지.”` and match range `[1, 4)` for `오리지`, when `searchHitRuns` builds the text, then runs are `[plain "“", box "오리지", plain "늄은 타지 않아. 빛만 남지.”"]`, the concatenated text equals the input exactly, no line break is inserted, and the box style is fill `hl`, text `ink`, 1 dp `acc` outline, radius 4.

**T-DS-21 · travel bar (rule 18 example).** Given 하르트 1-7 씬 2 (상층 관문, 1일째 저녁) → 씬 3 (결사의 문, 2일째 05:40), 38 km, 도보: required `2일 4시간` = 3 120 min, longest gap `12시간 40분` = 760 min, when `travelBarFraction(available = 760, required = 3120)`, then `0.2436` → the `있음` bar spans 24 % of the column (board 24 %) and the `필요` bar spans 100 % in `warn`. Given available ≥ required, then fraction = 1.0.

**T-DS-22 · avatar glyph.** `avatarGlyph("리엔") = "리"`; `avatarGlyphSp(32) = 12`, `avatarGlyphSp(36) = 13` (`floor(size × 0.38)`).

**T-DS-23 · icon stroke scales.** `iconStrokeDp(sizeDp = 24) = 1.6`; `(20) = 1.333…`; `(16) = 1.066…` (stroke 1.6 in 24-unit grid).

**T-DS-24 · copy lint: separators.** `lintString("이름 가림 · 검색 · 위젯 · 알림 제외")` → ok. `lintString("이름 가림·검색 제외")` → `UNSPACED_DOT`. `lintString("R-15 — 기본")` → `EM_DASH_SEPARATOR`. `lintString("—")` → ok (empty value). `lintString("처음 설정 · 1/4")` → ok. `lintString("18 / 24")` → ok. `lintString("받기 / 고치기")` → `SLASH_LIST`.

**T-DS-25 · copy lint: vocabulary.** → `VOCAB` for: `블록 #043 삭제`, `화자 미지정`, `복원 중`, `이미지 선택`, `1-5 시점 값 수정 중`, `미리보기`, `장면 4`, `넘기기`, `지문으로만 열림`, `변경`. → ok for: `블록 #043 지움 · ◆ 1개는 #044 자리로`, `휴지통 · 30일 뒤 삭제`, `30일 뒤 자동 삭제`, `영구 삭제`, `영구 삭제 · 노아`, `온보딩 후 삭제`, `예시 프로젝트 삭제됨`, `‘애니메이션 삭제’가 켜져 있으면 줄임`, `선택지`, `주인공 (선택)`, `마지막 수정 11월 20일 · 연결 4`, `막 › 장면`, `1-5 시점 값 고치는 중`, `미리 보기`, `건너뛰기`, `지문 · PIN으로 열림`. The lint runs over UI string resources only; story content (example project text, sample dialogue, recorder sample records such as `2장 첫 장면은 비 오는 상층 관문에서`) is not linted (DS-62 rule 8).

**T-DS-26 · copy lint: buttons.** A button resource `확인` → `LONE_CONFIRM`. Labels `확인: 프로젝트 이름 입력` (field label) and `확인 필요` (state) → ok. Button labels `휴지통으로`, `되돌리기`, `쓰기 시작`, `바꾸기` → ok.

**T-DS-27 · copy lint: symbols.** `◆ 변화`, `◇ 첫 공개`, `▲ 공개 전 사용`, `▸ 배경`, `♪ 잔불`, `⌁ 작가 메모`, `‹ 서재`, `1-1 없음 → 1-3 등불 결사`, `???` → ok. `★ 주연`, `✓ 완료`, any emoji (`🔥`) → `SYMBOL`.

**T-DS-28 · copy lint: notation.** `1-7 · 씬 3 · 2일째 새벽` → ok; `#043` → ok. (Episode-as-`7장` cannot be linted mechanically; review rule only.)

**T-DS-29 · button variants.** The `ConfirmDelete` confirm `휴지통으로` uses `DANGER` (`r18` / `onr18`); its cancel `취소` uses `GHOST` (`tint` / `ink`). The R-18 rating-raise sheet's confirm uses `PRIMARY` (`pri` / `onpri`), never `DANGER`. A sheet has at most one `PRIMARY`.

**T-DS-30 · controls.** Switch off: track `tint` + 1 dp `line`, thumb x = 3 dp, thumb `ink3`; on: track `acc`, thumb x = 21 dp, thumb `bg`. Radio on: 1.5 dp `acc` ring + 12 dp `acc` dot; off: 1.5 dp `ink3` ring. Checkbox on: `acc` fill + `Check` stroke `bg`; off: 1.5 dp `ink3` ring, radius 6.

**T-DS-31 · segmented and tabs.** Selected segment fill `pri`, text `onpri`; unselected text `ink2` (text segmented) / `ink3` (outlined). Selected tab text `ink` with a 2 dp `acc` underline; unselected `ink3`, no underline. Tab sets: record tabs are exactly `개요 · 말투 · 관계 · 게임 텍스트 · 비밀`; script panel tabs exactly `시간 · 참고 · 속성 · 모듈`.

**T-DS-32 · toast.** Toast fill `ink`, message colour `bg`, action button fill `bg` with text `ink`, label `되돌리기`; sample message `블록 #043 지움 · ◆ 1개는 #044 자리로`.

**T-DS-33 · motion tokens.** `standard` = (0.2, 0, 0, 1), 200 ms; `enter` = (0.05, 0.7, 0.1, 1), 320 ms; `exit` = (0.3, 0, 0.8, 0.15), 160 ms; `reducedFadeMs = 120`; `foldOpenMs = 360`; `foldCloseMs = 240`; `sheetBackScale = 0.965`; `sheetBackBlur = 2 dp`; `unhideBlur = 7 dp`; `swipeMs = 260`, `swipeStaggerMs = 60`; `holoSweepMs = 480`. (When each applies: T-CNV-50 and CNV-37.)

**T-DS-34 · glass rendering by mode.** For `GlassMode.OPAQUE`: fill `raise`, blur 0, no edge mask, top bar bottom border 1 dp `line`, bottom bar top border 1 dp `line`. For `GlassMode.BLUR`: fill `glass`, blur 36 dp, saturation 1.4, top-bar mask opaque to 0.72 then fading, bottom-bar mask opaque to 0.70 then fading. (Mode selection: T-CNV-56.)

**T-DS-35 · selection fill.** A selected row or block: background `sel`, left bar 2 dp `acc`; the speaker name of a selected block is `ink`. A search box uses `hl`, never `sel`.

**T-DS-36 · one command bar per frame (DS-65).** `CommandBarHost.layout` on an inner window 800 dp wide:
- 서재 alone (`LIST`, declared, title `서재`, detail = none): `barCount = 1`, `showsSwitch = false`, hint = 서재's `모든 프로젝트에서 찾기`, bar width 800, command row width `min(800 − 48, 720) = 720`.
- 서재 | 홈 of 「잿빛 새벽의 등불」 (both declared): `barCount = 1` (never 2), `showsSwitch = true`, `cells = ["서재", "잿빛 새벽의 등불"]`, selected = `DETAIL`, hint = 홈's `명령 또는 검색 · 인물 · 용어 · 대사`.
- 점검 센터 | Script (both declared; Script has an extra row): `barCount = 1`, `barHeight = 150`, the extra row lies inside the Script pane's column only.
- A pane whose `locked = true` (hidden project 「검은 서약」 locked): `cells` has one entry or none, and no string of `cells` equals `검은 서약`.
- Cover window (390): `barCount = 1`, `showsSwitch = false` in every case.

**T-DS-37 · scope changes only by explicit acts (DS-65).** `CommandScope.next`: `(LIST, DETAIL_OPENED) = DETAIL`; `(LIST, PICKED(LIST)) = LIST`; `(DETAIL, PICKED(LIST)) = LIST`; `(LIST, DETAIL_REPLACED) = DETAIL`; `(DETAIL, DETAIL_CLOSED) = LIST`; `(LIST, TOUCHED_IN(DETAIL)) = LIST` and `(DETAIL, TOUCHED_IN(LIST)) = DETAIL` (touch never moves it); `(DETAIL, PANE_LOCKED(DETAIL)) = LIST`.

**T-DS-38 · overlay gaps (DS-66).** With a cover window `H = 844`, gesture bar `systemBottom = 24`, keyboard hidden:
- `sheetBottomPadding(systemBottom = 24, ime = false) = 48`; with the keyboard up (`ime = true`): `16`.
- `floatingDialogY(compact, systemBottom = 24) = 40`; inner: `48`; keyboard up with `systemBottom = 300`: `308`.
- `dialogBand(statusTop = 24, H = 844, systemBottom = 24) = [48, 796]`.
- `popoverY`: anchor bottom 600, popover height 200, bar top at `844 − 154 = 690` → limit `min(844 − 24 − 16, 690 − 6) = 684`; `600 + 6 + 200 = 806 > 684` → placed above: top = anchor top − 6 − 200. Anchor bottom 300 → below at 306.
- No overlay rectangle returned by these helpers extends below `H − systemBottom`.

**T-DS-39 · toast slot (DS-67).** `toastBottom(systemBottom = 24, tallestChrome = 130) = 24 + 156 = 180`; `(24, 150) = 180`; `(24, 0)` (settings screen, no bar) `= 180`; `(24, 170) = 24 + 176 = 200`; keyboard up `(300, 130) = 456`. Moving from a screen with no bar to one with a 130 dp bar while the toast shows: its bottom stays 180. Horizontal: compact width `W − 32`; inner `W = 800` → width 560, left 120.

**T-DS-40 · toast durations and dismissal (DS-67).** `durationMs(INFO) = 2500`; `durationMs(ACTION) = 4000`; `durationMs(ERROR) = 4000`; no kind returns "until dismissed". `ToastController.show("Drive 연결 준비 중", actionLabel = null)` → kind `INFO`, gone after 2 500 ms of resumed time; a press held 1 000 ms in the middle → gone after 3 500 ms. `show("블록 #043 지움 · ◆ 1개는 #044 자리로", "되돌리기") {…}` → kind `ACTION`, 4 000 ms. While shown: app lock → `current == null` in the same frame; `relockNow()` → `null`; the project it acts on released → `null`; a `pop()` of the screen that raised it → still shown.

**T-DS-41 · one toast host (DS-67).** In `목록 + 상세` with a toast shown, exactly one toast node exists in the semantics tree (not one per pane), and it is above the sheet scrim (z 5 > 4).

**T-DS-42 · affordance cues (DS-68).** `Affordance.of`: `ListRow(onClick != null, trailing = none)` → `CHEVRON`; `ListRow(onClick = null)` → `NONE` and no `Next` icon drawn; field-table `.row` → `NONE`; settings row `보통` → `CHEVRON` (value + 4 dp + `Next` 16 `ink3`, no ` ›` text); switch row → `CONTROL`; `CivilightButton` → `CONTAINER`; link tile `인물` → `CONTAINER` (tile height 48, radius 12, fill `tint`, trailing `Next` 16); picker row in `화자 고르기` → exempt (no chevron, current `sel`); sheet header `닫기` → `CONTAINER` (compact ghost, 36 drawn, touch 44). For any element with `Affordance == NONE`, `onClick == null`.

**T-DS-43 · home 작업 grid (DS-68).** On 프로젝트 홈 with content width 342 (cover 390 − 2 × 24): 3 columns, tiles `(342 − 2 × 8) / 3 = 108.67` wide, 48 tall, 8 dp gaps; rows `스토리 · 인물 · 세계`, `연표 · 캔버스 · 갤러리`, `페이지 · 점검 센터`, `설정` (last row, label `ink2`). Content width 300 (a 1:2 list pane): 2 columns.

**T-DS-44 · pressed and disabled (DS-68).** A pressed row draws an `ink` overlay at alpha 0.06 and scale 1.0; a pressed button scales to 0.94 with no overlay; `press` = 100 ms. Disabled: alpha 0.38 for label, fill and chevron; no overlay or scale when pressed. Focus ring 2 dp `acc`.

**T-DS-45 · caret line in view (DS-69).** `CaretInView.delta` with `bandTop = 120`, `bandBottom = 484` (cover: top chrome ends 112, + 8; keyboard top 544, toolbar 44 → 500, − 16):
- line 300–328 → `0`;
- line 470–498 → `+14` (content scrolls up 14);
- line 100–128 → `−20`;
- line 100–600 (taller than the band) → `−20` (top aligned).
- A field growing by one 28 dp line whose new caret line is 480–508 → `+24`, applied in the same frame (no animation): after it the line's top is at 456 on screen.
- While a drag is in progress, the correction is not applied (`delta` is not called); the next text change applies it.
- While a content field has focus and the keyboard is up, the bottom glass bar is not drawn (`BottomGlassBar` absent from the tree); after the keyboard closes it is drawn again.

---

## 5. Screens

The design system has no app screens of its own; it supplies the parts every screen uses. These boards define or illustrate it (names without `NmDark`/`NmLight`):

| Board | What it fixes for the DS | Features |
| --- | --- | --- |
| `Style` (스타일, 800 × 800) | colour swatches (`바탕 · 올림 · 글자 · 보조 · 약한 글자 · 강조 · 주 버튼 · 경고` with hex), type samples, glass sample `유리 36px · 아래로 흐려짐`, pieces (speaker pill, `줄바꿈`, `최신 · 1-7`, `???` `하르트`, `◆ 변화`, `◇ 첫 공개`, tabs) | DS-02, DS-06, DS-16, DS-21 |
| `Accent` (강조색 비교) | why `#031C35` is a fill and not a mark; contrast figures | DS-02, DS-04 |
| `MotionSpec`, `MotionSpec2`, `FoldSpec` | motion token values (registry CNV) | DS-14, DS-15 |
| `Splash`, `SplashStrip` | the mark and wordmark | DS-13 |
| `AppLock`, `RHLockMark` | static mark, biometric button | DS-13 |
| `ReduceSettings`, `ReduceTransparency` | glass vs opaque rendering | DS-22 |
| `Holo`, `HoloKit` | holo material (experiments CNV-31) | DS-54 |

**Components and where they are drawn** (cover = 390 boards, inner = 800 boards):

| Component | Cover boards | Inner boards | Cover vs inner |
| --- | --- | --- | --- |
| GlassBars (DS-21) | `Script`, `Home`, `Library`, `Cast`, `World`, `Settings` … | `Wide`, `WideRef`, `WideProps`, `WideMods`, `Two*`, `Col*` | inner header title 21 / 28 and left 32; bottom bar spans only the manuscript column on the script |
| Sheet (DS-23) | `Picker`, `Lens`, `CSheetTime`, `CSheetRef`, `CSheetProps`, `CSheetMods`, `RSSplit`, `RSMerge`, `RSOrphan`, `RCDeleteImpact`, `RHRatingSheet`, `RTLinkAll`, `RTCascade`, `MSheet` | — | becomes a side panel (script, map card) or a centred sheet (confirmations) on inner |
| Side panel (DS-24) | — | `Wide`, `WideRef`, `WideProps`, `WideMods`, `MFoldScript` | inner only |
| Dialog (DS-25) | `ConfirmDelete`, `ErrBio` | — | centred dialog max width 420 on inner `[PROPOSED]` |
| Popover (DS-26) | `RTCandidate` | `Canvas` (line label) | multi-action popovers become sheets on cover |
| Toast (DS-27) | `UndoToast` | — | inner: inside the manuscript column `[PROPOSED]` |
| Banner (DS-28) | `LensEdit`, `OnbExample`, `OnbExampleScript` | — | — |
| CommandBar (DS-29) | every cover board with a bottom bar | `Wide` | inner script: left 24, right 314 |
| Buttons (DS-30) | `ConfirmDelete`, `OnbWelcome`, `OnbLook`, `RTEditTwo`, `EmptyCast`, `UndoToast` | — | same |
| Controls (DS-32) | `Settings`, `ReduceSettings`, `RHRatingSheet`, `BulkList`, `RCFieldAdd`, `Export`, `LayoutSettings` | — | same |
| Segmented (DS-33) | `Settings`, `RTLinkAll`, `RTFlashback`, `RSSplit`, `RSMerge`, `SceneWhere` | `LibCardsWide` | same |
| Slider (DS-34) | `OnbLook` | — | — |
| Text field (DS-35) | `RCFieldAdd`, `LensEdit`, `ConfirmDelete`, `Picker`, `RHRecoveryPw`, `NewName` | — | same |
| Tag (DS-36) | `Cast`, `Check`, `Lens`, `SceneWhere`, `LrRecord`, `TravelCheck` | — | same |
| Tabs (DS-37) | `Profile`, `Picker`, `CSheetTime`, `RCFields` | `Wide` | panel tab gap 16 |
| LensChip (DS-38) | `Cast`, `Profile`, `Lens`, `LensEdit`, `Script` (`이 씬 · 1-7`) | `Wide` | same |
| ViewToggle (DS-39) | `LibCards`, `CastCards` | `LibCardsWide` | same |
| ListRow (DS-40) | `Library`, `Cast`, `Settings`, `Profile`, `Home`, `Picker` | `Two*` | list pane keeps cover geometry |
| LockRow (DS-41) | `Library`, `LibUnlocked`, `LrLocked`, `MUnlock` | `TwoLibrary` | same |
| ScriptBlock (DS-42) | `Script`, `Layers`, `EditKeyboard`, `RSSelect` | `Wide` | column 560 at x 40 |
| Scene head (DS-43) | `RSHead`, `RTCandidate`, `RTFlashback` | — | same |
| SwipeTabs (DS-44) | `Script`, `MSwipe` | — | cover only (inner uses the panel) `[PROPOSED]` |
| SearchHit (DS-45) | `Search`, `MSearch`, `EmptySearch` | — | same |
| Empty / error (DS-47) | `EmptyCast`, `EmptySearch`, `EmptyCheck`, `EmptyLr`, `ErrMic`, `ErrBackup` | `TwoCheck` | same |
| Progress (DS-48) | `OnbLook` … `OnbExample`, `Restoring`, `LibCards`, `Tour*` | — | — |
| Tooltip (DS-49) | `Tour1`, `Tour2`, `Tour4`, `TourCast`, `TourWorld`, `TourLr`, `TourMap`, `TourWhen` | — | — |
| Node card (DS-50) | `CanvasC`, `CastMapC` | `Canvas`, `CastMap` | — |
| Cards (DS-51) | `LibCards`, `CastCards`, `MCarousel` | `LibCardsWide` | sizes CNV-23 |
| ImageTile / CropFrame (DS-52/53) | `ImagePick`, `ImageCrop`, `ImageSlots`, `MImageCrop` | — | — |
| MapToolbar (DS-55) | `MapRouteC`, `MapGen`, `MapHex` | `MapEdit`, `MapGenWide`, `MapPoly`, `MapDetail`, `MapFormat` | region editing mainly inner |
| MapMarks (DS-56) | `MapC`, `MapRouteC`, `MapScale` | `Map`, `MapEdit`, `MapTrack`, `TimelineMap` | same marks |
| MapStyle (DS-57) | `MapSoftC` | `MapSoft` | — |
| WhenPicker (DS-58) | `SceneWhere`, `BulkWhen` | `BulkWide` | — |
| ConditionRow (DS-59) | `MapText`, `MapTextResult` | — | — |
| TravelBar (DS-60) | `TravelCheck`, `SceneWhere` | — | — |
| Recorder glow (DS-61) | `LrRecord`, `LrProcess` | — | — |

---

## 6. Open questions

**Q-DS-01 · Fonts: Pretendard everywhere vs three families; weight 800.** Memory `civilight-android-app.md` (2026-11-22): "전부 Pretendard" — UI, manuscript and numbers, numbers with `tnum`. The design-system README, `tokens.json`, every board, the planning doc (`명조로 조판한 원고`, 남은 확인 "명조 본문의 장시간 가독성") and the computed task for this spec still name Nanum Myeongjo / Noto Sans KR / IBM Plex Mono, as do sibling specs CNV-23, CNV-24, CNV-38, LIB. This spec follows memory (newest, user's own words) and maps roles (DS-06). Open: (a) bundle `pretendard_extrabold.otf` for the 800 roles, or keep 800 → 700 (`[PROPOSED]` today); (b) update the DS README / boards so the mockups match the app.

**Q-DS-02 · Missing glyphs `▸` and `⌁`.** Pretendard has no U+25B8 or U+2301 (checked in the bundled OTF). DS-16/17 draw all six markers as vectors `[PROPOSED]`. The vector shapes for `▸ ♪ ⌁` need a design check.

**Q-DS-03 · Contrast claims vs exact values.** `tokens.json` says `acc` is "bg 위 7:1 이상"; light `acc` is 6.97:1 (board `Accent` prints `7.0:1`). `tokens.json` says `ink` is "bg · raise 위 15:1 이상"; light `ink` on `raise` is 14.90:1. Either accept the rounding or nudge the light values.

**Q-DS-04 · `ink3` below 5.4:1 on tinted surfaces.** The rule is stated for `bg` only. Light `ink3` on `raise` 4.91, on `sel` 4.72, on `hl` 4.43, on `tint` 4.91; dark `ink3` on `hl` 4.08, on `sel` 5.10, on `tint` 5.30. Dialogs (`raise`), selected rows (`sel`), lock rows and cards (`tint`) all carry `ink3` meta text on the boards. Keep, or use `ink2` for meta on those surfaces.

**Q-DS-05 · Error colour.** `tokens.json` lists `r18` for "R-18 등급 배지 · 선택 칸, 삭제 확인 버튼", the README says "R-18만 빨강", but boards `ErrMic` (mic icon) and `ErrBackup` (1.5 dp outline and warn icon) draw errors in `r18`. Decide: errors in `r18`, in `warn`, or a separate error token.

**Q-DS-06 · ALL / R-15 badge outline.** DS README: "ALL · R-15 배지는 `ink2` 테두리"; `bundle.css` `.badge` and `nmlib.py` `rating()` draw outline `ink3` with text `ink2`. This spec follows the CSS (DS-20).

**Q-DS-07 · Checkbox on-state in the design system.** `bundle.css` `.check.on` (outline + `acc` tick) vs every newer board (filled `acc` + `bg` tick). Resolved here to the newer boards; the DS `Controls` preview should be updated.

**Q-DS-08 · Command-bar radius.** `tokens.json` `radius-sm` 12 says "명령 칸 안쪽"; `.cmd` draws radius 14. This spec uses 14 (`radius-cmd`).

**Q-DS-09 · Lens chip border colour.** The rule "뜻을 가진 테두리 = `edge`" vs `.lens` drawn with a `line` outline (1.23:1) at 최신. The chip is tappable and meaningful; `edge` would make it read as a control.

**Q-DS-10 · Choice-card unselected border.** Onboarding `choice()` uses `line`; `export` options and `LayoutSettings` use `edge`. This spec uses `edge` (meaningful boundary).

**Q-DS-11 · Touch-target size.** DS: 44 dp (`hit-min`). Research §4: Android large-screen quality Tier 2 asks for 48 dp. Also the 설정 theme control is drawn 40 tall (`scr_tools.py`), below 44; this spec uses the 44-tall `.sg`.

**Q-DS-12 · Stale preview copy in the design-system project.** `Toast` preview `블록 #043 삭제`, `LensChip` README `수정 중` and preview `시점 · 최신`, `TravelBar` preview `2.5일` / `약 11시간` / `+N일로 · 이동 수단 · 맞음`, `ConditionRow` OK. Resolved in this spec by vocabulary and rules 11, 18; the DS project files should be regenerated.

**Q-DS-13 · Launcher icon.** No source draws an app icon (only the splash mark "아이콘 판 없이"). DS-13 proposes an adaptive icon from the mark on dark `bg`; needs the user's decision before release.

**Q-DS-14 · Glass on API 29–30.** Planning: "30 이하는 반투명 막으로 대체" without values; minSdk is 29. DS-22 proposes `raise` at 92 % with the same masks.

**Q-DS-15 · System reduce-blur setting.** Whether the app can read Android's "블러 줄이기" is a device check (planning 남은 확인; CNV-46). If it can, should it force `OPAQUE` like `투명도 줄이기`?

**Q-DS-16 · Text-size steps and scope.** Only `보통` (18 · 1.7) is in the sources; LIB-65 uses an engineering default of five steps. Whether the step scales interface text too, and how it combines with Android's font scale, is not stated (DS-07 proposes: manuscript only).

**Q-DS-17 · Pressed, disabled, focus and error states.** No board draws them for buttons, chips, rows or fields; DS-30 / DS-32 / DS-35 / DS-64 propose values.

**Q-DS-18 · Dialog shadow.** Boards draw dialogs and tooltips with `rgba(0,0,0,.25)` in both themes; `tokens.json` `shadow-float` is `.45` dark / `rgba(30,30,28,.18)` light. This spec uses the token.

**Q-DS-19 · Haptic constants.** The registry names `틱` and `확인`; the Android constants are a proposal (DS-15).

**Q-DS-20 · Open vocabulary.** `서재` vs `라이브러리`, `지역 / 영역 / 구역`, `홀로` (abbreviation), `낭독`, map style `색`, overloaded `기록 / 모듈 / 이동`, and `연결` used for both recorder links and a character-field type (memory vocabulary + rules-19-23). Not changed until the user decides.

**Q-DS-21 · Side-panel geometry.** Board `Wide` draws a flush full-height panel 290 wide without radius; `MFoldScript` animates to a floating panel 288 × 672 with radius 20 at x 496. Pick one resting shape.

**Q-DS-22 · Circles.** The user dislikes circle / ring decoration (memory 2026-11-20/21), yet avatars, the mic button, radio buttons and some glass buttons are circular on every board. They are functional, not decorative; confirm they stay round.

**Q-DS-23 · Vocabulary words the newest boards still use.** The vocabulary says `고치기` not `수정` and `고르기` not `선택`, yet the boards (and sibling specs CNV, LIB, CST) carry `마지막 수정 11월 20일 · 연결 4` (page list, `scr_tools.py`), `서재 · 씬 32 · 오늘 08:12 수정` (open-from-file, `scr_new_safe.py`, 2026-11-22), `(선택)` as an "optional" marker (`주인공 (선택)`, `첫 씬 제목 (선택)`, `한 줄 소개 (선택)`, `설명 (선택)`), and `선택` / `홀로 선택` meaning "optional" in the image-slot table. These are different meanings (last edited, optional) from the banned ones (edit, select), so DS-63 allowlists them; confirm or give replacement words.

**Q-DS-24 · Where the single command bar sits on the inner screen (DS-65).** Chosen: one bar across the full frame with a visible scope switch (pane titles), scope = detail when one opens, list when it closes, or the cell the user taps. Alternative: the bar sits only under the pane the user last touched (moves between panes, no switch). The scope switch also needs an accessibility label: `TODO_COPY_DS_COMMAND_SCOPE`.

**Q-DS-25 · Toast durations (DS-67).** Chosen: info 2.5 s, with action (`되돌리기`) 4 s, error 4 s, lengthened only by the system accessibility timeout; LIB-40's engineering default was 6 s for all. Deleted items stay in 휴지통 for 30 days after the undo toast is gone. Confirm 4 s for undo, or keep 6 s for `ACTION` only.

**Q-DS-26 · Toast slot height (DS-67).** Chosen: one window slot 156 dp above the system bottom on every screen (the tallest standard bottom bar 150 + 6), so a toast never jumps. On screens without a bar (settings) it now floats higher than today's 6 dp.

**Q-DS-27 · Bottom bar hidden while typing (DS-69).** Chosen: while a field other than the command field is being typed in, the bottom glass bar is not drawn (as the script already does), to give the caret line room above the keyboard. The command bar and mic come back when the keyboard closes.

**Q-DS-28 · Chevron on every tappable row (DS-68).** Chosen: every row that opens something shows `Next` 16 dp `ink3` at its end (서재, 인물, 세계, 연표, 기록함, 점검 lists, home episode rows, settings rows), and picker rows in `… 고르기` sheets are exempt. The boards draw most list rows without a chevron.

**Q-DS-29 · `작업` on 프로젝트 홈 as tiles (DS-68).** Chosen: `tint` link tiles 48 tall in a 3-column grid with a chevron each. Alternative: one list row per shortcut with a chevron (9 rows, longer page).

**Q-DS-30 · Create card titles (DS-72), 0.2.0.** Sourced: `새 인물`, `새 장소`, `새 용어`, `새 세력` (U-15.1). Built by the same pattern, to confirm: `새 사건`, `새 에셋`, `새 태그`, `새 관계 종류`, `새 페이지`, `새 캔버스`.

**Q-DS-31 · Create card dismiss with typed text (DS-72).** Chosen: `취소` and back discard; a scrim tap does nothing while any field has content. Alternative: a discard confirm.

**Q-DS-32 · Create card focus (DS-72).** Chosen: the name field has focus and the keyboard opens. Alternative: no focus until a tap (the character card starts with the image slot).

**Q-DS-33 · `⋯` accessibility name where no board gives one (DS-70).** Chosen: `더 보기` `[PROPOSED]`; confirm or give the word.

**Q-DS-34 · Tilt values (DS-74).** ±12° clamp, 120 ms low-pass and 8 dp depth are `[PROPOSED]`; check on a device. Tilt stops with the LIB-94 switch (`TODO_COPY_LIB_VIEW_TILT`, default on) or with reduced motion.

**Q-DS-35 · Open rows of the icon list kept as text (D28).** The input row's `고치기` send slot (scope keeps text); `지문으로 보기` · `잠금 풀기` stay b (D31).
