# 50 · 캔버스 · 페이지 · 도구 · 카드와 이미지 · 펼친 화면 · 모션 — feature prefix `CNV`

Implementation spec for the canvas area of Civilight: the canvas editor and its graph engine (shared with the cast area's 전체 관계도), free pages, the check-center shell, the card-view and image system, unfolded (inner-screen) layouts, fold/unfold behaviour, the motion system and the 움직임 · 투명도 preferences. Engineers implement from this file alone; every Korean string in `code` is UI copy and must be used verbatim.

**Sources read (2026-11-22).** Planning doc main tab (live, rev 71; local snapshot rev 41 only for orientation) · 규칙 index (rev 25) · rule tabs 11 (rev 8), 12 (rev 8), 13 (rev 6), 저장 파일 양식 (rev 8) · confirmed rule drafts `rules/19.md`–`23.md` · memory files (concept, vocabulary, rules-time/blocks/relations/travel/19-23, character-fields, feedback-*) · board generators `design/night-manuscript/gen/` (`scr_world.py` canvas, `scr_cast.py` relationship map, `scr_tools.py` page/check/settings, `scr_cards.py`, `scr_inner.py`, `scr_fold.py`, `scr_motion.py`, `scr_motion2.py`, `scr_decide.py`, `scr_consist.py`, `scr_new_cast.py`, `nmlib.py`) · `boards_text.txt` + `board_index.tsv` · design system README, `tokens.json`, component READMEs · research `2026-11-20-design-research.md` §1, §4 · sibling specs present at writing time: `10-script.md` (SCR), `30-cast.md` (CST), `60-library-security-files.md` (LIB).

**Precedence.** Confirmed rule tabs and memory > planning main tab (live) > board generator source (`gen/*.py`) > `boards_text.txt` (an older text dump; where it differs from a generator, the generator wins) > design-system README > research. When a newer source settles an older one this spec follows the newer one and says so in a *Source note*; only unresolved conflicts or real gaps go to section 6.

**Conventions.**
- `[PROPOSED]` marks behaviour that no source states and this spec had to choose to make a feature implementable. `[PROPOSED]` never introduces Korean copy; where copy is missing it says *copy not specified* and the item is in section 6. Section 4 contains no test for `[PROPOSED]` behaviour.
- "cover" = cover screen (390×844 boards, Compact width). "inner" = unfolded inner screen (800×800 boards, Medium/Expanded width). Board sizes are mockup px; read 1 px ≈ 1 dp as a *reference only*. Real layout is driven by window-size class and proportions (CNV-32); real dp must be measured on the device (planning `남은 확인`).
- Board names drop the `NmDark`/`NmLight` prefix and `.dc.html` (e.g. `Canvas`, `CanvasCLabel`). Every board exists in dark and light.
- Separators in copy are ` · `. Markers: `◆` change (`acc`), `◇` notice / first reveal (`ink3`), `▲` warning (`warn`, always), `⌁` author memo. Dotted underline / dotted outline = attached to something deleted (rule 20 + DS).
- Colour / type / size tokens are the design system's (`bg raise ink ink2 ink3 acc pri onpri sel hl tint line edge glass shade warn r18`, fonts Nanum Myeongjo / Noto Sans KR / IBM Plex Mono, `radius-*`, `space-*`, `hit-min 44`, `measure 560`, `card-w 222`, `blur-glass 36`, `blur-back 2`).

**Superseded statements (resolved, not open).**
- Planning main tab, 캔버스: "선 라벨을 달 때 관계 종류(동료 · 경계 · 가족 · 스승 · 적대)와 시작 시점을 고르면 그 선이 인물 관계로 기록된다" → rule 13 §7: a line becomes a relation **only** through the `관계로 만들기` button, on every screen size; picking chips records nothing; the sheet asks `무엇으로 적을지` (사이 / A의 마음 / B의 마음); the kind list is the project's 관계 종류. The `방향 · 한쪽` field is removed.
- Inner `Canvas` board popover shows only 4 kind chips and no `+ 새 종류` → rule 13 §9: the inner popover has **the same content** as the cover sheet `CanvasCLabel`.
- `boards_text.txt` copy is older than the generators: 점검 센터 `전체 3` without `지운 항목` → `전체 5` + `지운 항목` + the `◇ 공개 전 후보` sub-row (`scr_tools.py`); 인물 카드 보기 `전체 12` with 노아 → `전체 11`, no 노아 (`scr_cards.py`, rule 20); 프로젝트 설정 / 설정 · 라이프 레코더 / 설정 · 지도 · 이미지 · 연표 rows → `scr_decide.py` / `scr_consist.py` versions; `붙일 곳` → `연결` (rule 23).
- `MFoldList` (in `scr_fold.py`) still shows `전체 12` and 노아 → rule 20 (노아 in trash: `전체 11`).
- Rule 11 §2 table's "모르는 사이" → rule 13 §6: not a value; shown as `만나기 전`.

---

## 1. Scope

### 1.1 What this area owns

1. **Graph engine** shared by the canvas and the cast area's 전체 관계도: node cards, plain and relation-backed lines, labels, selection, pan/zoom, node drag (CNV-01).
2. **Canvas editor** (`캔버스`): canvases of a project, tool bar, project-item nodes, memo nodes, 묶음, plain lines, the `선 라벨` sheet/popover and its `관계로 만들기` hand-off, relation-backed line rendering under the lens, node edit surface, cover vs inner adaptations, canvas persistence (CNV-02 … CNV-12).
3. **Pages** (`페이지`, free documents): page view/edit, header image, entry points, receiving side of recorder `페이지로 만들기`, hiding/deletion/term-usage effects on pages (CNV-13 … CNV-16).
4. **Check-center shell** (`점검 센터`): screen, filters, row anatomy, `맞음` / `맞다고 표시한 것` storage, locked hidden-item row, empty state, unfolded layouts, and the registry of check kinds other areas emit (CNV-17).
5. **Settings effects** of the boards on page `페이지 · 도구` and the motion/layout preferences: image section of `설정 · 지도 · 이미지 · 연표`, `설정 › 화면 › 펼친 화면 배치`, `설정 · 움직임 · 투명도`, and a row map of the settings containers owned elsewhere (CNV-18 … CNV-20, CNV-33, CNV-46).
6. **Card view and images**: the `목록 | 카드` toggle mechanics, card components and snapping rows (content of the 서재 and 인물 rows belongs to LIB and CST), image slots and ratios, the 8 preset images, image picker, crop editor, holo effect, holo experiments (CNV-21 … CNV-31).
7. **Inner screen and fold**: window-size classes and the adaptive frame, list-module two-pane / single-column layouts, fold/unfold transitions, state save/restore, half-fold postures (CNV-32 … CNV-36).
8. **Motion system**: tokens and easing, the motion registry (incl. reference values for map motions owned by the world/map area), splash, block add, swipe row, sheet, lens value swap, unhide, search highlight, card/image motions, reduce motion, reduce transparency and glass fallbacks (CNV-37 … CNV-46).

### 1.2 Hand-offs to other areas

| Area | What it owns that this spec only references |
| --- | --- |
| 인물 · 관계 (`30-cast.md`, CST) | Characters, relations (rule 13: 사이 · 마음 · 설명 · 호칭, one per pair, kind list `관계 종류`), the 전체 관계도 view (data, filters `사이 · 마음 · 둘 다`, counts, `고른 관계` sheet, `관계 고치기`), **the relation write semantics of a canvas line → relation (CST-34)**, 인물 card-view content (CST-04), cast unfolded layout specifics (CST-06), image slots of the profile (CST-13), deletion impact and merge (rule 20) |
| 스크립트 (`10-script.md`, SCR) | Blocks, speakers, panels, swipe buttons, command bar, keyboard; the behaviour behind block-add / swipe / sheet motions; script fold specifics (SCR-66/67); script check items (SCR-62); rendering of `◆ 1-7부터 · 근거 블록 없음` rows in the panel; tabletop for the script (SCR-69) |
| 서재 · 보안 · 파일 (`60-library-security-files.md`, LIB) | 서재 list/card content and 최근 연 파일 (LIB-04/05/06), splash → 서재 routing (LIB-08), onboarding and 도움말 · 안내 다시 보기 (LIB-21), rating/hiding/lock (rule 19), trash host and 되돌리기 toast host (LIB-38/40), export, `.civilight` packaging and Drive backup, the containers of 설정 / 프로젝트 설정 / 설정 · 지도 · 이미지 · 연표 |
| 시점 렌즈 · 변화 기록 (rules 11, 12) | Lens state, lens chip and sheet, 집필 선, `예정`, ChangeRecord storage and placements (`블록 · 시점 · 세계 사건`). This spec calls `valueAt(lens)` and creates `시점` placements through it |
| 세계 · 지도 · 연표 · 장소와 때 (rules 18, 21, 22) | Terms, places, factions, events, maps and **all map motions' behaviour**, term `쓰인 곳`, travel checks, place/time, check items they emit |
| 라이프 레코더 (rule 23) | Recording, inbox, `연결`, the `페이지로 만들기` sheet, recorder settings screen `LrSettings` and its behaviour |
| 홈 · 스토리 · 명령 칸 · 검색 | Project home (incl. 작업 shortcuts `스토리 · 인물 · 세계 · 연표 · 캔버스 · 설정`), story structure, the command palette and search engine (this spec owns only the search-highlight motion) |

### 1.3 needsExternal rule

A feature is `needsExternal: yes` only if it needs a network service, cloud, an ML model, or OS integration beyond a normal app. The system photo/document picker, device rotation sensor, haptics, the Android 12+ SplashScreen API and Jetpack WindowManager posture APIs are ordinary app APIs. Rows in settings screens that lead to Drive or cloud features belong to their own areas. **No CNV feature is needsExternal.**

---

## 2. Features

### A. Canvas and graph engine

#### CNV-01 · 그래프 엔진 (shared graph engine) — P1 · needsExternal: no
One component renders node-and-line graphs for the canvas (CNV-02…12) and the cast area's 전체 관계도 (boards `CastMap`, `CastMapC`). "노드 · 선 · 상호작용이 같고, 차이는 담는 것뿐이다."

- **Surface.** Unbounded 2-D plane; content coordinates in dp at zoom 1.0. Viewport = centre point + zoom factor. Content draws under the glass header (and tool bar on canvas); glass bars stay fixed while panning.
- **Node card** ("노드는 사각 카드"; no circles or rings anywhere): padding 10 dp × 14 dp, radius 14, fill `raise`, 1 dp inset `line`. Line 1 = name, Nanum Myeongjo 700 14/20 `ink`. Line 2 = kind or subtitle, Noto Sans KR 400 11/15 `ink3` (canvas: `세력`, `인물`, `사건`, `용어`, `메모`; relationship map: one-line descriptor such as `결사 · 검사`). Memo variant: transparent fill, 1 dp **dashed** `line` border.
- **Selected node**: 1.5 dp inset `acc` ring. **Selected line**: stroke `acc`, 2 dp (2.2 dp on cover).
- **Plain line**: straight, node-centre to node-centre, stroke `ink3` 1.2 dp, solid ("가는 회색 선").
- **Relation-backed line** (rule 13 §4 and §7; board `CastMap`): one line per pair.
  - 사이 present → **solid** line + middle label (t = 0.5) with the 사이 kind. For paired kinds (선배 ↔ 후배) which side's word is shown is not specified (Q-CV-11); board `CastMap` shows `선배` on the 리엔–세라 line.
  - Only 마음 present → **dashed** line (dash 6 dp, gap 5 dp).
  - Each side's 마음 → a small label near that side's end with an arrow: label of A near A (t ≈ 0.28) `<kind> →`, label of B near B (t ≈ 0.72) `← <kind>` (board: `경계 →`, `← 보호`).
  - Label pill: fill `bg`, 1 dp inset `line`, radius 10, padding 2 × 8, Noto Sans KR 500 11/16, `ink2` (`acc` while the line is selected).
  - **Never colour by kind** (rule 13 §4). Colour changes only for selection.
  - No value at the lens (`만나기 전`) or only 호칭 → the relationship map draws **no line** (rule 13 §5, §6). Canvas behaviour: CNV-09.
- **Gestures** (cover hint copy `두 손가락 이동 · 확대 · 노드는 끌어서 이동`): two-finger drag pans, pinch zooms, one-finger drag on a node moves it (canvas only; relationship-map layout is the cast area's). Tap selects. No hidden gestures beyond these; everything else is a visible button.
- **Zoom readout** (cover, board `CanvasC`): glass column at top-right (12 dp from edge, 128 dp from top, radius 16, 1 dp `line`): `+` button, percentage in IBM Plex Mono 12/16 `ink2` (e.g. `82%`), `−` button; buttons 44 × 44. Zoom range and step: not specified (Q-CV-07).
- Hit targets ≥ 44 dp for nodes, lines (tap tolerance around the stroke) and labels.

*Sources:* main tab 캔버스, 전체 관계도; rule 13 §4, §5, §6, §7; boards `Canvas`, `CanvasC`, `CanvasCLabel`, `CastMap`, `CastMapC`; DS principles.

#### CNV-02 · 캔버스 문서 (canvases of a project) — P1 · needsExternal: no
- A project holds **one or more** canvases (file format: `canvas/<캔버스 키>.json`). Each canvas has a name (sample `1장 구상`).
- Header (glass, 112 tall): back button, small mono line `캔버스`, title = canvas name (Nanum Myeongjo 700 19/26), right: lens chip (e.g. `최신 · 1-7`). Rule 11 §3 lists 캔버스 among screens **with** a lens chip.
- Entry: project home 작업 shortcut `캔버스`.
- Listing, creating, renaming, switching and deleting canvases: **not drawn and not specified** (Q-CV-01). `[PROPOSED]` until decided: the shortcut opens the most recently opened canvas of the project; if the project has none, an empty canvas is created; the name stays editable through the (not yet designed) canvas menu.
- Lens chip behaviour (tap → lens sheet, `acc` outline when not 최신) is the lens area's.

*Sources:* 저장 파일 양식 §2 tree; boards `Canvas`, `CanvasC`; rule 11 §3; home board 작업 row.

#### CNV-03 · 도구 줄 (canvas tool bar) — P1 · needsExternal: no
- Glass pill at the bottom, 1 dp `line`, padding 6, gap 4.
  - inner (`Canvas`): centred, 18 dp from bottom, items `노드` · `인물 카드` · `메모` · `선` · `묶음`, Noto Sans KR 500 13/36, each padding 0 × 14, radius 18.
  - cover (`CanvasC`): 12 dp side insets, 18 dp from bottom, 5 equal columns, items `노드` · `인물` · `메모` · `선` · `묶음`, 500 12/44, radius 18.
- The current tool has `pri` fill and `onpri` text. Exactly one tool is current. Only the current tool acts on taps on the surface ("고른 것만 동작", same principle as the map tool bar).
- Board states: cover idle shows `노드` current; inner shows `선` current while a line label is being edited.
- Tool meanings: `선` → CNV-07; `메모` → CNV-05; `인물 카드` / `인물` → place a character node (CNV-04); `묶음` → CNV-06; `노드` → **not specified** (Q-CV-02: "place a project item of kind 세력 · 사건 · 용어" or "select/move mode").
- There is no pen, pencil or sketch tool (펜 · 스케치 레이어 없음 — out of scope by decision).

*Sources:* boards `Canvas`, `CanvasC`; main tab 캔버스; 범위 table row 펜 · 스케치 레이어.

#### CNV-04 · 항목 노드 (project-item nodes) — P1 · needsExternal: no
- Node kinds that reference a project item **by key** ("프로젝트 항목을 그대로 올림"): `인물` (character), `세력` (faction), `사건` (timeline event), `용어` (term). Sample: `등불 결사 · 세력`, `리엔 · 인물`, `하르트 · 인물`, `대정전 · 사건`, `오리지늄 등불 · 용어`.
- The node shows the item's **current** name; renaming the item renames every node that references it. The canvas never copies item data.
- Choosing the item when placing a node: a visible picker (the app's picker pattern: search with 초성, ordering instead of auto-selection, works with 100+ items). The picker sheet for canvas is not drawn (Q-CV-03).
- Tapping a node selects it and opens node edit (CNV-10).
- `장소` is not in the source node list (Q-CV-04). Lens effects on item nodes (display name `???`, unrevealed terms, characters not yet appearing) are not specified; boards show the plain name `하르트` on canvas (Q-CV-05).

*Sources:* main tab 캔버스 노드; board `Canvas` nodes; feedback-no-guessing-interactions.

#### CNV-05 · 메모 노드 (memo nodes) — P1 · needsExternal: no
- A free-text node owned by the canvas (sample `2장 떡밥?`, kind line `메모`), drawn with the memo variant (transparent, dashed border).
- Created with the `메모` tool; text edited through node edit (CNV-10). Exists only in the canvas file. Plain text; `@` links in memo nodes are not specified.

*Sources:* main tab 캔버스 노드; boards `Canvas`, `CanvasC`.

#### CNV-06 · 묶음 (group) — P1 · needsExternal: no
- Listed as a node type and as a tool (`묶음`). No board shows a group and no source defines it (Q-CV-06). `[PROPOSED]` data shape only: a group has a title and a set of member nodes; moving the group moves its members. No UI or copy is defined here until decided.

*Sources:* main tab 캔버스 노드; tool bars in `Canvas`, `CanvasC`.

#### CNV-07 · 선 긋기 (plain lines) — P1 · needsExternal: no
- With the `선` tool current, the author connects node A to node B; a line A→B is created. The **drawing direction is stored** (rule 13 §7: it is used only as the default choice in the label sheet).
- New lines are plain lines ("그냥 선으로 긋는다"). A plain line exists only in the canvas and never touches cast data.
- Allowed between any two nodes (item nodes, memo nodes). Exact connect gesture (tap A then tap B, or drag A→B), self-lines, and several lines between the same two nodes: not specified (Q-CV-08). `[PROPOSED]`: tap A then tap B; no self-line; at most one line per unordered node pair.
- `[PROPOSED]`: right after a line is created its `선 라벨` surface (CNV-08) opens (inner board shows the popover open while `선` is current).

*Sources:* main tab 캔버스 선; rule 13 §7; board `Canvas`.

#### CNV-08 · 선 라벨 · 관계로 만들기 (line label sheet and the relation hand-off) — P1 · needsExternal: no
Opens when a line is tapped (or after creation, CNV-07). Same content on all screen sizes (rule 13 §9).

- **Surface.** Inner: glass popover (≈300 dp wide, radius 18, 1 dp `line`, padding 14 × 16) placed next to the line. Cover: bottom sheet (board top at 340 of 844; `Sheet` component: grab handle, shade behind) with title `선 라벨` (Nanum Myeongjo 800 22/28) and `닫기` at the right, subtitle line = the two node names joined, e.g. `리엔 · 하르트`. Inner popover header: section label `선 라벨`.
- **Rows, top to bottom.**
  1. Label text field (optional). Board value `경계하는 사이`. Height 46 (cover) / 40 (inner), radius 12/10, fill `tint`, caret `acc`.
  2. `무엇으로 적을지` chips — single choice: `사이` · `<A>의 마음` · `<B>의 마음` with A, B = the two character names (board: `사이` · `리엔의 마음` · `하르트의 마음`). **Default = the 마음 of the node the line was drawn from** (rule 13 §7; line drawn 리엔 → 하르트 ⇒ `리엔의 마음`).
  3. `종류` chips — single choice — the project's 관계 종류 list of the chosen category (사이 kinds when `사이` is chosen, 마음 kinds otherwise; list and its edit sheet are the cast area's), followed by `+ 새 종류`. Board (마음): `경계` · `신뢰` · `의지` · `동경` · `불신` · `+ 새 종류`, `경계` chosen. Do not hard-code the list (the rule-13 default list and the `RCRelTypes` board differ — Q-CV-16).
  4. Row `시작 시점` with the value in IBM Plex Mono (board `1-7`, with the lens at `최신 · 1-7`). Choosing another point: picker not drawn (Q-CV-10).
  5. Buttons, equal width: `그냥 선으로 두기` (secondary, `tint`) · `관계로 만들기` (primary, `pri`).
- **Which rows appear.** Rows 2–4 and `관계로 만들기` appear **only for a line between two `인물` nodes**. Any line touching a `세력`, `사건`, `용어` or `메모` node shows the label field and `그냥 선으로 두기` only (rule 13 §7).
- **Buttons.**
  - Picking chips records **nothing** (rule 13 §7).
  - `그냥 선으로 두기`: saves the label text on the canvas line; closes. No cast data changes.
  - `관계로 만들기` (all screen sizes): hands `(characterA, characterB, cell = 사이 | A의 마음 | B의 마음, kind, startPoint)` to the cast area's relation write (CST-34, normative). Per rule 13: one relationship per pair (create if missing, otherwise reuse); the chosen cell gets a `◆` change record; the other side's 마음 is **never** filled in; 사이 kinds are symmetric or paired and set both sides as a fact. Per rule 12 §1 the record from a canvas line is a **`시점` placement** (no anchor block) that applies **from the start** of the chosen episode/scene. The canvas line stores the relation key and from then on renders as a relation (CNV-09).
  - Closing (`닫기`, tapping outside the popover) without a button: nothing is recorded; whether typed label text is kept is not specified (Q-CV-09).
- **Not specified (Q-CV-09, Q-CV-10, Q-CV-11):** whether the label text becomes the cell's 설명; which direction a paired 사이 kind (선배 ↔ 후배) applies in this sheet; whether `관계로 만들기` is enabled before a kind is chosen; how an already converted line opens (current values, editing).
- **Script side:** the script panel shows such a record above the first block of that unit as `◆ 1-7부터 · 근거 블록 없음` with button `블록에 잇기` (rule 12 §1; SCR renders it).

*Copy:* `선 라벨` · `닫기` · `무엇으로 적을지` · `사이` · `<이름>의 마음` · `종류` · `+ 새 종류` · `시작 시점` · `그냥 선으로 두기` · `관계로 만들기`.
*Sources:* rule 13 §2, §3, §7, §9; rule 12 §1 (`시점` 자리: "캔버스 선의 시작 시점" · "그 에피소드나 씬의 처음부터"); boards `Canvas`, `CanvasCLabel`.

#### CNV-09 · 관계 선과 시점 렌즈 (relation-backed lines under the lens) — P1 · needsExternal: no
- A canvas line with a relation key renders from the relationship's state **at the canvas lens** using CNV-01 relation styling. Per cell the value is the record in effect at the lens point (rule 11 §1: `최신` applies records up to the 집필 선 only, later ones are `예정`; rule 11 §2: lens `1-7` = after the last block of 1-7; rule 12 §1: a `시점` record applies from the start of its unit; a block record from its block, inclusive).
- Plain lines and memo/item nodes do not change with the lens.
- When the relationship has **no** cell value at the lens (state `만나기 전`) or only 호칭: the relationship map draws no line (rule 13 §5, §6); for a canvas line the behaviour is not specified (Q-CV-12). `[PROPOSED]`: draw it as a plain line with its label text, without relation labels.
- Deleting a relation-backed line from the canvas: effect on the relationship not specified (Q-CV-13). `[PROPOSED]`: removes the canvas line only; the relationship stays (it belongs to the characters and is edited through `관계 고치기`).

*Sources:* rule 11 §1, §2, §3; rule 12 §1; rule 13 §4–§7.

#### CNV-10 · 노드 편집 (node edit surface) — P1 · needsExternal: no
- "커버 화면: 선 라벨 · 노드 편집은 옆 팝오버 대신 아래 시트." Inner = popover next to the node; cover = bottom sheet (`Sheet`).
- Contents are not drawn (Q-CV-03). Minimum functions the editor must reach `[PROPOSED]`, copy not specified: edit memo text; open the referenced item's record/card (the relationship map uses `인물 기록` for this); remove the node from this canvas (does not delete the project item).

*Sources:* main tab 캔버스 커버 화면; board `CastMap` (`인물 기록` button pattern).

#### CNV-11 · 숨긴 항목 · 지운 항목이 캔버스에 있을 때 (hidden and deleted items on canvas) — P1 · needsExternal: no
Rules 19 and 20 do not list the canvas. Facts that bind the implementation:
- Rule 19 §6: links the app draws to a hidden item show `숨긴 인물` / `숨긴 항목` while locked; the relationship map draws no node and no line for a hidden character (`12명 중 5명 · 숨김 1`); results are never dropped, only grouped.
- Rule 20 §2: data that *references* a deleted item stays in place, dotted, with `◇` and the label `지운 인물 · 노아` (`지운 장소 · …`, `지운 용어 · …`, `지운 세력 · …`) and the buttons of its kind; data *owned* by the item (its relations, ◆ records) goes to trash with it; restore re-attaches by key; after permanent delete a name tag remains (`영구 삭제 · 노아`).
- Which of these applies to canvas item nodes and relation-backed lines is not specified (Q-CV-14). `[PROPOSED]` (consistent with both rules): an item node is a *reference* → while the item is in trash the node stays, dotted, with `◇` and `지운 인물 · 노아`-style text; a relation-backed line whose relationship is in trash renders as a plain line; a node of a hidden, locked item shows `숨긴 인물` / `숨긴 항목` and no relation labels.

*Sources:* rule 19 §6 table; rule 20 §1–§3; DS 점선 rule.

#### CNV-12 · 캔버스 저장 (canvas persistence) — P1 · needsExternal: no
- App DB is the working store; each canvas is exported to `.civilight` as `canvas/<canvas key>.json` (file format §2, §6 "원고 · 인물 · 세계 · 지도 · 캔버스 · 페이지 · 변화 기록 · 항상").
- ULID keys for canvas, nodes and lines; order keys (insertable strings) for z-order; last-modified time + device on every item; optional `ext` on every item; text NFC-normalised (file format §4, §7).
- Item nodes store only the referenced key and kind; relation-backed lines store only the relation key. Relationship data (사이, 마음, 호칭, ◆) lives in the cast files, never in the canvas file.
- Canvas viewport (centre, zoom): `[PROPOSED]` app DB only (view state, like other UI state), not in the file.

*Sources:* 저장 파일 양식 §2, §4, §6, §7.

### B. Pages (페이지)

#### CNV-13 · 페이지 (free document: view and edit) — P1 · needsExternal: no
"페이지: 자유 문서(세계관 메모 등). @로 인물 · 용어 연결, /로 블록 추가, 작가 메모 블록, 연결 수 표시." Board `Page`.

- **Header** (glass, 80 tall): back button, small mono line `페이지`, no title in the bar, `⋯` button at the right. `⋯` menu contents: not specified (Q-PG-02).
- **Body** (from 110 dp, 24 dp side margins; text set directly on `bg`, no boxes except the memo block):
  - Section label (sample `페이지 · 세계관 메모`; what `세계관 메모` is — a category, a parent, free text — is not specified, Q-PG-03).
  - Page name: Nanum Myeongjo 800 30/38 (sample `하층 구역의 빛`). A page has an **이름** (vocabulary: projects and items have 이름, not 제목).
  - Meta line, small `ink3`: `마지막 수정 <날짜> · 연결 <N>` (sample `마지막 수정 11월 20일 · 연결 4`). What `연결 N` counts (outgoing `@` links, links to this page, or both) is not specified; the sample body shows 3 linked words (Q-PG-04).
  - Paragraphs: Nanum Myeongjo 400 16/28 `ink`.
  - Sub-headings: Nanum Myeongjo 700 17/24 (samples `빛의 값`, `결사의 규칙`).
  - `@` links: inline, same font, 1 dp `ink3` underline (inset bottom shadow). Tap opens the linked item's card/record. Samples: `오리지늄 등불`, `등불 결사`, `오웬`.
  - Author memo block: fill `tint`, radius 14, padding 12 × 14; label `⌁ 작가 메모` (small); text Noto Sans KR 400 14/22 `ink2` (sample `2장에서 값이 두 배로 뛰는 장면 넣기. 노아 시점으로?`).
- **Editing**: tap into text to edit. Bottom command bar hint `@로 인물 · 용어 연결, /로 블록 추가` (verbatim, including the comma — Q-PG-07). `@` opens the shared link picker (SCR owns the `@` picker); `/` adds a block (shared block-add sheet). Block kinds seen in pages: paragraph, sub-heading, author memo. The complete list of block kinds allowed in a page is not specified (Q-PG-01).
- **Header image**: slot `페이지 머리 이미지`, 21:9, no holo (CNV-25). The `Page` board shows a page without one; where it renders is the top of the page ("페이지 맨 위").
- **Not game output**: page text is never exported to game JSON/CSV/PDF, never raises `▲ 공개 전 사용` or `후보`; term links in pages are listed under the term's `쓰인 곳` in the group `원고 밖 · 메모` (rule 21 §3–4; world area renders).
- **Storage**: text stored as runs — plain text, link run (target key + kind), memo block; display names are resolved at render time (file format §4).
- **Last-modified date** shows `M월 D일` (sample `11월 20일`).

*Sources:* main tab 페이지; board `Page`; image-slot table; rule 21 §3–4 and the `하층 구역 메모` example; 저장 파일 양식 §4.

#### CNV-14 · 페이지 목록과 들어가는 곳 (page list and entry points) — P1 · needsExternal: no
Sources that reference a page list or page entries: DS `ViewToggle` ("서재 · 인물 · 세계 · 페이지 목록에 쓴다"); rule 19 §6 (`인물 · 용어 · 페이지 목록` with a locked row); 최근 연 파일 cards (`페이지 · 유리 정원` / `온실의 규칙` / `3일 전`); recorder `페이지로 만들기`; recorder `연결` units include `페이지` (rule 23 §2).
- No board draws a page list, and project home 작업 shortcuts (`스토리 · 인물 · 세계 · 연표 · 캔버스 · 설정`) contain no `페이지` (Q-PG-05). Search categories do not list pages either.
- Must exist regardless: open a page from 최근 연 파일 (LIB-05), from an `@` link to a page, from the recorder record's 넣은 곳 list, and from a page list once its entry point is decided.
- When a page list exists it follows rule 19 §6 (locked hidden pages collapse into one locked row with the count; open: in place with a small `숨김` tag) and supports `목록 | 카드` (CNV-21) per DS.

*Sources:* as listed.

#### CNV-15 · 레코더 기록으로 만든 페이지 (page made from a recorder record) — P1 · needsExternal: no
Receiving side of rule 23 §4 `페이지로 만들기` (the sheet and the record side belong to the recorder area).
- Creates **one new page** in the chosen project; its name is prefilled with the start of the record's first sentence and is editable in the sheet; the page body receives a **copy** of the record text. The record stays in the library.
- Both sides point to each other by permanent key: the page stores the source record key; the record's `넣은 곳` list gains the page; the record's state becomes `넣음`; if the page is not yet among the record's `연결`s, one `연결` is added (fact).
- Later edits are independent: editing the page leaves the record untouched (no mark); editing the record leaves the page untouched and shows `◇ 넣은 뒤 고침` on the record screen only (never in the check center).
- Page in trash: the record's 넣은 곳 list shows `휴지통` for it; restoring changes nothing else; after permanent delete the entry stays as plain text, and when **all** 넣은 곳 are gone the record returns to `정리 안 됨` with `◇ 넣은 곳 지워짐` (record screen only) (rule 23 §6 지움 table).
- Record trashed: the page's source mark reads `레코더 · 휴지통`; after the record's permanent delete it reads `지워진 기록`; the page text never changes (same table, row 기록 자체).
- Whether the copied text lands as a paragraph or as an author-memo block, the exact truncation of the prefilled name, and the page's normal (record alive) source-mark copy are not specified (Q-PG-06). For script blocks the source mark is `⌁ 작가 메모 · 레코더 11월 22일 08:41`.

*Sources:* rule 23 §2, §4 (table and 같이 다루는 경우), §6 table row 넣은 블록 · 카드 문단 · 페이지.

#### CNV-16 · 페이지에 걸리는 숨기기 · 지우기 (hiding and deleting as seen in pages) — P1 · needsExternal: no
- **Hidden page** (item hiding covers 인물 · 용어 · 페이지 · 씬, plus 장소 · 세력 by the 범위 table): while locked, lists show one locked row with the count; `@` links to it show `숨긴 항목`; it leaves search results (with `숨긴 항목 제외` at the end) and 최근 연 파일; unlocking uses the project's re-lock setting (rule 19 §6; LIB-36 owns the common model).
- **Links in a page to a hidden item**: rendered `숨긴 인물` / `숨긴 항목` while locked; plain words that happen to be a name are left untouched (rule 19 §6 버린 안).
- **Links in a page to a deleted item** (rule 20 §2 row `@연결`): dotted chip + `◇`; tapping shows a card `지운 인물 · 노아` with `다른 항목에 잇기` · `글로 풀기` · `되살리기` · `이대로 두기`. `글로 풀기` keeps the letters and removes only the link. Restoring the item re-attaches the link. After permanent delete the chip reads `영구 삭제 · 노아` and `글로 풀기` remains available. `이대로 두기` moves the check-center row to `맞다고 표시한 것`.
- **Plain text is never a reference**: the memo `노아 시점으로?` is untouched when 노아 is deleted and is not counted in the impact sheet (rule 20 §2).
- Deleting a page itself (trash or not; trash filters `전체 · 프로젝트 · 씬 · 블록 · 인물 · 변화 · 레코더 · 세계` have no `페이지`) is not specified (Q-PG-08).

*Sources:* rule 19 §6; rule 20 §2 (incl. the 페이지 example) and table row `@연결`; 범위 table row 장소 · 세력 숨기기.

### C. Tools on page `페이지 · 도구`

#### CNV-17 · 점검 센터 (check-center shell) — P1 · needsExternal: no
"점검 센터: 기록과 원고가 어긋난 곳을 모은다. 항목마다 고치는 버튼을 나란히 둔다." Other areas compute items; this feature owns the screen, filters, row layout, `맞음` storage and the registry below. Boards `Check`, `TravelCheck` (rows from the map/time area), `EmptyCheck`, `TwoCheck`, `ColCheck`.

- **Header**: back, small mono line = project name (`잿빛 새벽의 등불`), title `점검 센터`. **No lens chip** (rule 11 §3). Bottom command bar hint `명령 또는 검색`.
- **Intro line** (small, 13/20): `기록과 원고가 어긋난 곳`.
- **Filter chips** (single choice, `Tag` component; selected = `pri`): `전체 <N>` · `용어 공개` · `화자` · `이동 <N>` · `보이스` · `변화` · `지운 항목` · `맞다고 표시한 것`. (`Check` shows `전체 5 · 용어 공개 · 화자 · 보이스 · 변화 · 지운 항목 · 맞다고 표시한 것`; `TravelCheck` (`scr_link.py`) shows `전체 5 · 용어 공개 · 화자 · 이동 4 · 보이스 · 변화` with `이동 4` selected — Q-CK-04.) `전체 N` counts every open row **including `◇` rows and `◇` sub-rows** (rule 21 §(바뀌는 시안): "`전체` 수에 ◇ 포함"). Rows marked `맞음` / `이대로 두기` leave every filter except `맞다고 표시한 것`.
- **Row anatomy** (1 dp `line` top border, padding 14 dp vertical):
  - line 1: marker + kind (Noto Sans KR 600 12/16; `▲ …` in `warn`, `◇ …` in `ink3`), location at the right (small `ink3`, e.g. `1-4 씬 2`, `인물 · 리엔`, `휴지통`);
  - line 2: the evidence sentence in Nanum Myeongjo 400 16/26, whole sentence, with the matched span underlined (e.g. `세라: “오리지늄은 타지 않아. 빛만 남지.”` with `오리지늄` underlined);
  - line 3: small detail (e.g. `‘오리지늄 등불’ 첫 공개 1-7 · 사용 1-4`);
  - action row: text buttons Noto Sans KR 500 13/18, gap 14; first `ink`, second `ink2`, third `ink3` (e.g. `원고로 가기` · `공개 시점 옮기기` · `맞음`).
  - **Sub-row** under a row (dashed top border, 13/18): `◇ 공개 전 후보 · 1-2 씬 1` + `잇기` · `아님` (rule 21).
  - `▲` is always `warn` (DS rule "▲는 어디서나 warn 색"); the `Check` board draws the `▲ 보이스 비어 있음` label in `ink3` — follow the DS.
  - **Card variant** (`TravelCheck`, first row): a `tint` card (radius 18, 1 dp `edge`, padding 14) holding the kind line `▲ 이동 시간 부족` + `하르트`, the route line `1-7 씬 2 상층 관문 → 씬 3 결사의 문`, a 318 × 150 mini-map with the route, two bars `필요` (`warn`, label `도보 2일 4시간`) and `있음` (`acc`), a detail line (`경로 38 km · 도보 15 km · 하루 8시간 · 1일째 저녁(17–20) → 2일째 05:40`) and fix chips. Its content and numbers are the 장소와 때 area's (rule 18); the shell only hosts it. Further travel rows use the compact list form (`▲` + bold kind · person + one small line; `◇ 장소 없음` adds `한꺼번에 ›`).
- **Navigation**: buttons that go to a place (`원고로 가기`, `보이스 쓰기`, `한꺼번에 ›`) open the target screen at the item (script opens at `(sceneKey, blockKey)`, SCR entry). Tapping any `▲` mark elsewhere in the app opens 점검 센터 (DS markers rule).
- **`맞음` / `이대로 두기`**: move the item to `맞다고 표시한 것`; the mark is stored and exported in `.civilight` so reopening never raises it again (file format §4 계산값, §6). Marks are keyed by the permanent keys of what they concern: a mark on a block **disappears with the block and moves with the block** (rule 12 §1 table). Un-marking from `맞다고 표시한 것`: not specified (Q-CK-02). Where marks live in the file tree: not specified (Q-CK-01).
- **Hidden items while locked** (rule 19 §6): all checks are still computed; rows about locked hidden items collapse into one row `▲ 숨긴 항목 관련 <N> · 지문으로 보기` (unlock → original rows).
- **Empty state** (`EmptyCheck`): filter row with `전체 0`; title `어긋난 곳 없음`; small `마지막 확인 · 방금 · 1-1 ~ 1-7`; buttons `원고로` · `맞다고 표시한 것 <N>` (sample `맞다고 표시한 것 2`).
- **Unfolded**: 목록 + 상세 → left = the list, right = `원고 위치` (the script at the selected row's location, board `TwoCheck`); 가운데 한 칸 → `ColCheck` (CNV-33).
- **Computation timing** is the emitting areas' (computed values are never stored).

**Check-kind registry** (rows the shell must lay out; copy verbatim; owner computes):

| Kind label | Marker | Filter | Buttons | Owner · source |
| --- | --- | --- | --- | --- |
| `공개 전 사용` | ▲ | 용어 공개 | `원고로 가기` · `공개 시점 옮기기` · `맞음` | 세계 · rule 21, main tab |
| `공개 전 후보 · <위치>` (sub-row) | ◇ | 용어 공개 | `잇기` · `아님` | 세계 · rule 21 |
| `화자 없음` | ▲ | 화자 | `화자 고르기` · `나레이션으로 바꾸기` | 스크립트 · SCR-62 |
| `보이스 비어 있음` | ▲ | 보이스 | `보이스 쓰기` | 인물 |
| `형식과 다른 ID <N>` (보이스 ID가 화자 · 에피소드와 다름) | ◇ | 보이스 | `새 ID로` · `그대로` | 스크립트 · rule 12 §6 |
| `근거 지워진 변화 <N>` | ◇ | 변화 | `다른 블록에 잇기` · `이대로 두기` · `기록 지우기`; several → `모두 지우기` | 변화 기록 · rule 12 §2 |
| `지운 인물 · 노아` (also `지운 장소 · …`, `지운 용어 · …`, `지운 세력 · …`) | ◇ | 지운 항목 | `화자 한꺼번에 바꾸기` · `되살리기` · `이대로 두기` (board `Check`) | 인물 · rule 20 |
| `이동 시간 부족` | ▲ | 이동 | minimal-fix chips, e.g. `씬 3 · +2일로` · `말 · 약 7시간 40분`, then `맞음` (board `TravelCheck`) | 장소와 때 · rule 18 |
| `대략인데도 모자람 · <인물>` | ▲ | 이동 | not drawn (list form) | 장소와 때 · rule 18 |
| `같은 때 두 장소 · <인물>` / `동시에 두 장소` | ▲ | 이동 | main tab: `때 고치기` · `장소 고치기` · `맞음` | 장소와 때 · rule 18, main tab |
| `장소 없음 · 씬 <N>개` | ◇ | 이동 | `한꺼번에 ›` | 장소와 때 · main tab, `TravelCheck` |
| `점검 못 함` (no scale) | ◇ | 이동 | — | 장소와 때 · rule 18 |
| `순서와 때가 다름` | ◇ | not specified | `맞음` | 장소와 때 · rule 11 §5 |
| `전환 문구 · 쓴 뒤 장소 · 때 바뀜` | ◇ | `이동` or new `씬` (undecided) | `자동으로` · `문구 고치기` · `맞음` | 스크립트 · rule 22 |
| `공개 전 장소 · 전환 문구` | ◇ | same | `직접 쓰기` · `맞음` | 스크립트 · rule 22 |
| `숨긴 인물이 보이는 씬에 나옴 · 씬 <N>` | ◇ | not specified | `씬도 숨기기` · `그대로` | 숨기기 · rule 19 §6 |
| `숨긴 항목 관련 <N>` (locked group) | ▲ | all | `지문으로 보기` | 숨기기 · rule 19 §6 |

*Sources:* main tab 점검 센터 table (rev 71); boards `Check` (`scr_tools.py`), `TravelCheck`, `EmptyCheck`, `TwoCheck`, `ColCheck`; rules 11 §3 §5, 12 §1 §2 §6, 18, 19 §6, 20, 21, 22; 저장 파일 양식 §4, §6.

#### CNV-18 · 설정 (app settings root: row map) — P1 · needsExternal: no
**Implementation owner: LIB** (container of 설정). Listed here to map every row on the `Settings` board to the feature that implements it; do not implement twice.

| Section · row (copy) | Subtitle · value | Implemented by |
| --- | --- | --- |
| `화면` · theme segmented `기기 설정` · `라이트` · `다크` | default `기기 설정` (DS: "기본은 기기 설정을 따른다") | LIB (theme tokens: design system) |
| `글자 크기` | `본문 18 · 행간 1.7` · `보통 ›` | LIB / onboarding (other steps not specified, Q-ST-02) |
| `펼친 화면 배치` | `서재 · 인물 · 세계 · 연표 …` · `목록 + 상세 ›` | screen LIB; effect **CNV-33** |
| `움직임 · 투명도` | `애니메이션 줄이기 · 투명도 줄이기` · `기기 설정 ›` | screen LIB; effect **CNV-46** |
| `지도 · 이미지 · 연표` | `색면 · 홀로 · 생성 방식 · 이동 거리` · `›` | **CNV-19** |
| `보관` · `Google Drive 자동 백업` | `마지막 백업 오늘 08:12` · toggle | LIB |
| `복구 암호` | `숨긴 프로젝트 백업용 · 정해 둠` · `바꾸기 ›` | LIB |
| `보안` · `앱 잠금` | `지문으로 열기` · toggle | LIB |
| `숨기기` | `숨긴 프로젝트 2 · 다시 잠그는 때` · `앱을 떠날 때 ›` | LIB |

Paths referenced by other boards but **absent from the root board**: `설정 › 수위 등급` (reached from `설정 · 숨기기` row `수위 등급별 동작 ›`), `설정 · 라이프 레코더` (header `설정` / `라이프 레코더`), `설정 › 도움말 › 안내 다시 보기` (Q-ST-01).

*Sources:* board `Settings` (`scr_tools.py`); main tab 온보딩 다시 보기; boards `HideSettings`, `LrSettings`, `HelpReplay`.

#### CNV-19 · 설정 · 지도 · 이미지 · 연표 — P1 · needsExternal: no
Header small `설정`, title `지도 · 이미지 · 연표`. Container: LIB. Rows (board `SettingsMore`, `scr_consist.py`):

| Section | Row (copy) · subtitle | Control · default | Implemented by |
| --- | --- | --- | --- |
| `이미지` | `홀로그램 효과 기본` · `새 이미지에 · 이미지마다 바꾸기 가능` | toggle · off | **CNV-30** (initial holo value of newly placed images in holo-capable slots) |
| | `기본 이미지` · `8종 · 앱에 포함 · 오프라인` | value `보기 ›` | **CNV-26** (opens a view of the 8 presets; screen not drawn — Q-IM-07) |
| `지도` | `새 지도 그림` · `색면 · 먹선 · 색 · 양피지` | value `색면 ›` | 지도 area |
| | `지형 만들기 · 글로 설명` · `기기 안 · 클라우드(정확도↑ · 전송)` | value `기기 안 ›` | 지도 area |
| | `큰 지도 내보내기` · `4K · 8K` | value `4K ›` | 지도 area |
| `연표 · 때` | `때 표기 기본` · `새 프로젝트 · 프로젝트마다 바꾸기 가능` | value `에피소드 기준 ›` | 장소와 때 area |
| | `이동 점검` · `이동 시간 부족 · 같은 때 · 동시에 두 장소` | toggle · on | 장소와 때 area |
| | `장소 없는 씬 알림` · `점검 센터 ◇` | toggle · on | 장소와 때 area (row lands in CNV-17) |

*Sources:* board `SettingsMore`; main tab 카드 보기 · 이미지.

#### CNV-20 · 프로젝트 설정 · 도움말 · 레코더 설정 (row maps) — P1 · needsExternal: no
**Implementation owners: LIB (프로젝트 설정 container, 도움말 · 안내 다시 보기 LIB-21), recorder area (`LrSettings`).** Row maps so every board row on page `페이지 · 도구` has an owner.

*프로젝트 설정* (board `ProjSettings`, `scr_decide.py`; header small = project name, title `프로젝트 설정`):

| Section | Row · subtitle | Value | Owner |
| --- | --- | --- | --- |
| `구조` | `단계 이름` · `장 › 에피소드 › 씬` | `바꾸기 ›` | 스토리 |
| | `세계 시간` · `연도만 · 시작 1200년` | `연도만 ›` | 연표 |
| | `때 표기` · `1-7 · 2일째 · 새벽 · 연표는 기준 사건도` | `에피소드 기준 ›` | 장소와 때 |
| | `이동 수단` · `도보 15 km · 8시간 · 말 40 km · 8시간 · 배 30 km · 24시간` | `바꾸기 ›` | 장소와 때 (rule 18) |
| | `하루 · 시간대` · `하루는 새벽 4시부터 · 저녁 17–20` | `바꾸기 ›` | 장소와 때 (rule 18) |
| `인물` | `인물 항목` · `게임 프로필 · 고정 7 · 프로젝트 7 · 장소 · 용어 · 세력도` | `고치기 ›` | CST |
| | `관계 종류` · `사이 12 · 마음 11` | `바꾸기 ›` | CST |
| `등급 · 숨기기` | `수위 등급` · `ALL · R-15 · R-18` | `R-15 ›` | LIB |
| | `이 프로젝트 숨기기` · `지문 · PIN으로 열림` | toggle | LIB |
| | `화면 캡처 가리기` · `스크린샷 · 최근 앱 미리 보기 · R-15 기본과 다름` (last fragment in `acc`) | toggle | LIB |
| | `이름 가리기` · `위젯 · 알림 · 최근 항목` | toggle | LIB |
| | `라이프 레코더 연결 가리기` · `기록의 연결에 등급만 표시` | toggle | LIB |
| `스크립트 추가 칸` | `무대 위치` · `왼쪽 · 가운데 · 오른쪽` | toggle (off) | SCR |
| | `보이스 ID` · `대사마다 녹음용 ID` | toggle (on) | SCR |
| | `ID 형식` · `HRT_0107_043 · 인물 · 에피소드 · 블록 · 내보내면 고정` | `바꾸기 ›` | SCR |
| | `전환 문구` · `새 씬 기본 없음 · 틀 장소 · 시각` | `바꾸기 ›` | SCR (rule 22) |

Whether the home 작업 shortcut `설정` opens 프로젝트 설정 or app 설정 is not specified (Q-ST-03).

*도움말 · 안내 다시 보기* (board `HelpReplay`; header small `설정`, title `도움말 · 안내 다시 보기`): section `처음 안내` row `워크스루` · `3단계 · 봤음`; section `모듈 안내 · 처음 들어갈 때` rows `스크립트` (4단계), `인물`, `세계 · 연표`, `라이프 레코더`, `서재 · 숨기기`, `지도 만들기`, `인물 동선 · 장소와 때` (3단계 each), each with status `봤음` / `아직 안 봄` and a tag button `다시 보기`; toggle `기능 첫 사용 짧은 안내` · `한 줄 · 한 번만` (on); button `모든 안내 처음부터`. Behaviour: LIB-19/20/21.

*설정 · 라이프 레코더* (board `LrSettings`; header small `설정`, title `라이프 레코더`): `받아쓰기` radios `기기 안` · `오프라인 · 음성이 기기 밖으로 나가지 않음` (default) / `클라우드` · `정확도↑ · 음성 전송 · 인터넷 필요`; `녹음` toggles `잠금 화면에서 녹음` · `새 녹음만 · 기록함은 잠금 해제 후` (on), `원본 음성 보관` · `글과 함께 · 30일 뒤 정리 가능` (on), `녹음 중 빛 띠` · `말소리 크기 표시` (on), `보던 곳 연결` · `시작한 곳을 칩으로 · 언제든 빼기` (on). Behaviour: recorder area. The `녹음 중 빛 띠` toggle and `움직임 · 투명도` → `라이프 레코더 빛` (CNV-46) control the same thing on two screens (Q-ST-04).

*Sources:* boards `ProjSettings`, `HelpReplay`, `LrSettings`; rules 18, 19, 22, 23.

### D. Card view and images (카드 보기 · 이미지)

#### CNV-21 · 목록 | 카드 전환 (ViewToggle mechanics) — P1 · needsExternal: no
"목록 화면은 머리의 분할 버튼으로 목록 | 카드 보기를 고른다. 고른 보기는 화면마다 기억한다."
- Component `ViewToggle`: segmented control in the header's right slot, height 36, radius 12, 1 dp inset `edge`, two cells 44 wide with icons (list icon / grid icon, 18 dp), selected cell `pri` fill + `onpri` icon, other cells `ink3`. Accessibility labels: group `보기`, cells `목록`, `카드`. Hit area ≥ 44.
- Used on 서재 (LIB-04), 인물 (CST-04); DS also names 세계 and 페이지 lists, which have no card board (Q-IM-08).
- The choice is stored **per screen** in app preferences (not in `.civilight`). `[PROPOSED]` default `목록` (every list board shows the list first).
- Switching animates with CNV-45 "목록 ↔ 카드".

*Sources:* main tab 카드 보기 · 이미지; DS `ViewToggle`; boards `LibCards`, `CastCards`, `MCardView`.

#### CNV-22 · 가로 카드 줄 (snapping card rows) — P1 · needsExternal: no
"카드 보기: 가로로 넘기는 줄."
- A row is a horizontal scroller that starts at the content inset (20 dp cover, 32 dp inner) and runs past the right edge so the next card peeks out ("두 번째 카드는 오른쪽으로 비켜 서며 다음이 있다는 것을 보임").
- Fling/drag ends by **snapping the nearest card's left edge** to the row start: decelerate 320 ms, haptic tick on snap. The card's image moves less than the card (parallax 8 %). Reduce motion: snap only, no parallax (CNV-45).
- Card pitch (reference): project cards 222 + 14 gap = 236 (cover), 236 + 16 = 252 (inner); recent-file cards 150 + 14 = 164 (cover), 176 + 16 = 192 (inner); portrait cards 196 + 14 = 210, small portrait 132 + 14 = 146.
- Optional pager at the row's section header (right): one mark per card, current `acc` 18 × 4, others `line` 6 × 4, gap 4, radius 2.
- Rows used: 서재 프로젝트 row and `최근 연 파일` row (LIB-04/05), 인물 one row per tag group (CST-04).

*Sources:* main tab; boards `LibCards`, `LibCardsWide`, `CastCards`, `MCarousel`; DS `ProjectCard`; 모션 table row 가로 카드 줄.

#### CNV-23 · 카드 부품 (card components) — P1 · needsExternal: no
All text on images uses fixed light colours (independent of theme): primary `#F4F4F6`, secondary `rgba(244,244,246,.82)`.
- **ProjectCard** (3:4; cover 222 × 296 = `card-w`; inner 236 × 336): radius 20; image fills; bottom gradient over 62 % of the height from `rgba(8,9,12,0)` to `rgba(8,9,12,.86)`; text block 16 dp from left/right, 14 dp from bottom: rating badge (IBM Plex Mono 600 10/18, height 18, padding 0 × 6, radius 5, 1 dp outline; `R-18` in `#FF8A80`, others `rgba(244,244,246,.75)`), name Nanum Myeongjo 800 22/28, meta Noto Sans KR 400 12/17. Top-right `⋯` (44 hit, icon `#F4F4F6`). Optional status chip at top-left 12/12: height 24, padding 0 × 9, radius 12, Noto Sans KR 600 11/14, `#F4F4F6` on `rgba(10,11,14,.5)` + 12 dp blur (sample `쓰는 중 · 홀로`). Holo-on covers draw the holo layers (CNV-30) and include `홀로` in the chip. Samples: `R-15` · `잿빛 새벽의 등불` · `1장 7/10 · 인물 12 · 오늘 08:12`; `ALL` · `유리 정원` · `단편 · 씬 6개 · 3일 전`.
- **Locked card** (same size): fill `tint`, 1 dp inset `edge`, content 18 dp inset from bottom/sides: lock icon 22 `ink2`, `숨긴 프로젝트 <N>` (Nanum Myeongjo 800 20/26), small `이름 · 표지 가림`, primary button (height 40) `지문으로 보기`. One locked card stands for all hidden projects; a hidden project never gets its own card while locked (rule 19 §1/§7, `MCarousel` fix).
- **New-project card** (inner only, same size): transparent, 1.5 dp inset `edge`, centred `+` icon 28 and label `새 프로젝트` (`lab`).
- **Recent-file card**: image 150 × 100 (inner 176 × 110), radius 14; below (8 dp): kind line small (`씬 · 잿빛 새벽의 등불`), title Nanum Myeongjo 700 15/21 single line with ellipsis (`1-7 씬 3 · 꺼진 등불`), time small (`오늘 08:12`). Which image a recent item shows (project cover, portrait, page header…) is not specified (Q-IM-09).
- **Portrait card** (4:5; primary group 196 × 245, other groups 132 × 165, radius 20): below (10 dp) name Nanum Myeongjo 800 18/24 with first appearance in mono small at the right (`1-2`), then a small sub line (`등불 결사 · 막내 등불지기`). Holo-on portraits show the chip `홀로`. Names follow the lens (display-name rule of CST-11: at `최신 · 1-7` 하르트 shows `???` with sub `하르트 · 이름 공개 1-9`). Which group gets the large size is not specified (Q-IM-10).

*Sources:* `scr_cards.py` (`pcard`, `lockcard`, `fcard`, `ccard`), boards `LibCards`, `LibCardsWide`, `CastCards`; DS `ProjectCard`; rule 19.

#### CNV-24 · 이미지 없음 (initial-letter placeholder) — P1 · needsExternal: no
"아무것도 안 고르면 이름 첫 글자 카드 + ‘이미지 고르기’."
- When a slot has no image: fill `tint`, 1 dp inset `edge`, the first character of the owner's name in Nanum Myeongjo 800 64/80 `ink3` at the top-left (16, 8), and a tag at the bottom-left (12 dp) on `bg`: image icon 14 + `이미지 고르기`. Tapping the tag opens the picker for that slot (CNV-27).
- First character = first grapheme of the name (`노아` → `노`, `유리 정원` → `유`).

*Sources:* main tab; DS `ImageTile`; `scr_cards.py` `ccard(empty=…)`.

#### CNV-25 · 이미지 규격 (slots and fixed ratios) — P1 · needsExternal: no
"이미지가 들어가는 곳" — every image place is a slot with fixed ratios; ratios are chosen with buttons, never free.

| Slot (copy) | Used in (copy) | Ratios (crop chips) | Holo |
| --- | --- | --- | --- |
| `프로젝트 표지` | `서재 카드 · 목록 썸네일` | 3:4 `카드 3:4`, 1:1 `목록 1:1` | allowed (`홀로 선택`) |
| `인물 초상` | `인물 카드 · 아바타` | 4:5 (카드), 1:1 (아바타) | allowed |
| `인물 배경` | `프로필 위 · 아래로 흐려짐` | 9:16 (커버), 4:3 (펼친 화면) | never (`홀로 없음 · 글자 뒤`) |
| `장소 · 용어 · 세력` | `카드 참고 이미지` | 16:9 | allowed |
| `페이지 머리 이미지` | `페이지 맨 위` | 21:9 | never (`홀로 없음`) |

- One source image → **a separate crop per ratio**. Ratios are fixed per slot (`규격 고정`). Chip labels drawn only for 표지 (`카드 3:4` · `목록 1:1`); others follow the main-tab wording (`카드 4:5, 아바타 1:1`; `커버 9:16, 펼친 화면 4:3`) — exact chip copy for non-cover slots not drawn (Q-IM-05).
- Rules screen (board `ImageSlots`; header small `규격`, title `이미지 · 자르기 · 홀로`): section `이미지가 들어가는 곳` with the table above (ratio glyphs + mono labels), then section `규칙` with four lines: `원본 그대로 보관 · 자르기는 언제든 다시` · `한 이미지 → 규격마다 따로 자르기` · `기본 이미지 · 앱에 포함 · 오프라인` · `홀로 · 기본 끔 · 이미지마다 켜기`. Its entry point is not specified (Q-IM-07).
- Slot ownership: project cover → LIB; portrait/background → CST-13; place/term/faction → world area; page header → CNV-13. All use CNV-26…30.

*Sources:* main tab 카드 보기 · 이미지 table; board `ImageSlots`; DS `CropFrame`.

#### CNV-26 · 기본 이미지 8종 (preset images) — P1 · needsExternal: no
- Eight bundled abstract images, available in **every** slot, offline: `잿빛 새벽` · `등불` · `심해` · `안개 숲` · `사막 유적` · `성운` · `붉은 성채` · `설원`. Angular shards only — no figures, no circles.
- Stored by name, never as a file (`preset:ash-dawn` for 잿빛 새벽). The app never removes a preset name (file format §4). Ids for the other seven are not given (Q-IM-06); `[PROPOSED]` `preset:lantern`, `preset:deep-sea`, `preset:mist-forest`, `preset:desert-ruin`, `preset:nebula`, `preset:red-citadel`, `preset:snowfield` — must be fixed before the first export because they become part of the file format.
- Reference rendering (generator `scr_cards.py`; same in both themes): background `linear-gradient(165deg, c0, c1)`; three polygon layers s1 (opacity .9), s2 (1.0), s3 (.9) with shard set `i mod 4`.

| # | Name | c0 | c1 | s1 | s2 | s3 |
| --- | --- | --- | --- | --- | --- | --- |
| 0 | 잿빛 새벽 | #2B2F3A | #6B6F7C | #A3A7B3 | #4A4F5C | #D9C7A3 |
| 1 | 등불 | #1D1A17 | #4A3A28 | #C8913F | #6E5236 | #2C241C |
| 2 | 심해 | #06131F | #123B56 | #2F6F8F | #0B2536 | #7FB3C8 |
| 3 | 안개 숲 | #1E2A24 | #4E6656 | #8BA593 | #2E3E34 | #C9D4C3 |
| 4 | 사막 유적 | #3A2A1E | #A57A52 | #D8B98C | #6B4A30 | #F0DEC0 |
| 5 | 성운 | #120F24 | #3B2E5C | #7A64A8 | #241B3D | #C7B6E6 |
| 6 | 붉은 성채 | #240E0E | #6A2420 | #A9483C | #3D1715 | #E0A08A |
| 7 | 설원 | #8C98A4 | #E4E9EE | #B7C2CC | #6E7D8A | #F6F8FA |

Shard polygons (x y in % of the frame): set 0 — s1 `0 58, 55 30, 100 46, 100 72, 0 86`, s2 `0 74, 40 56, 100 68, 100 100, 0 100`, s3 `58 8, 78 0, 70 44, 52 52`; set 1 — `0 40, 100 18, 100 60, 0 70` / `0 66, 70 50, 100 80, 100 100, 0 100` / `12 10, 30 6, 26 34, 8 30`; set 2 — `20 0, 60 0, 90 100, 40 100` / `0 60, 100 42, 100 100, 0 100` / `62 20, 84 16, 80 40, 60 42`; set 3 — `0 30, 45 12, 100 36, 100 64, 50 54, 0 70` / `0 78, 100 58, 100 100, 0 100` / `40 60, 56 52, 60 72, 44 78`.
- `설정 · 지도 · 이미지 · 연표` → `기본 이미지` `보기 ›` shows the eight (view not drawn, Q-IM-07).

*Sources:* main tab; 저장 파일 양식 §4 row 기본 이미지 8종; `scr_cards.py` `PRE`, `SHARDS`; memory (angular shards, no circles).

#### CNV-27 · 이미지 고르기 (image picker sheet) — P1 · needsExternal: no
Board `ImagePick` (over 서재 card view; sheet top at 118 of 844).
- Sheet header: title = slot title (`표지 이미지`; titles for other slots not drawn, Q-IM-05) + `닫기`; subtitle `<owner name> · <ratio chips>` (`유리 정원 · 카드 3:4 · 목록 1:1`).
- Tabs (`Tabs`): `기본 이미지 8` · `내 이미지 <N>` (sample `내 이미지 3`). Scope of `내 이미지` (this project's imported images or every project's) is not specified (Q-IM-03).
- Grid: 3 columns of tiles (108 × 144, gap 9 → pitch 117; row pitch 178), radius 12, preset name below in small `ink3`. Selected tile (`ImageTile`): 2 dp `bg` gap + 2 dp `acc` ring, check badge top-right (24 × 24, radius 7, `acc` fill, `bg` check), name in `ink`.
- Holo row (only for holo-capable slots): `홀로그램 효과` · `은빛 · 기울이면 흐름` with a toggle; initial value = current image's holo or, for a new image, `홀로그램 효과 기본` (CNV-19).
- Buttons: `갤러리` (secondary, image icon) → system photo picker; `파일` (secondary, file icon) → system document picker; `고르기` (primary) → applies the selected tile.
- After `갤러리` / `파일` returns an image → the crop editor opens for that image with the slot's first ratio (CNV-28); the image is added to `내 이미지`.
- `고르기` with a preset sets the slot to the preset (presets fill the frame; whether presets can be cropped is not specified — Q-IM-03). With a `내 이미지` tile → crop editor if that ratio has no crop yet `[PROPOSED]`.
- Motion: the tapped tile grows into the crop stage (CNV-45).

*Sources:* board `ImagePick`, `MImageCrop`; DS `ImageTile`; main tab.

#### CNV-28 · 자르기 (crop editor) — P1 · needsExternal: no
Board `ImageCrop` (full screen).
- **Top bar** (`bg`, 70 tall): `취소` (left, text button) · centre small mono `<owner> · <slot short name>` (`유리 정원 · 표지`) over title `자르기` (Nanum Myeongjo 700 17/24) · `적용` (primary, height 40).
- **Stage** (between top bar and controls; reference 70 → 540): the image; crop frame with the current ratio; everything outside the frame darkened (`rgba(5,6,8,.62)`); frame outline 1.5 dp white (90 %); rule-of-thirds lines 1 dp white 40 %; four L-shaped corner marks (22 × 22, 4 dp white). Under the frame, mono 11 `#E9EAEE`: `<cropW> × <cropH> · 원본 <W> × <H>` in source pixels (sample `2160 × 2880 · 원본 3024 × 4032`).
- **Controls** (`bg` panel, 16 × 20 padding):
  - ratio chips of the slot (`카드 3:4` · `목록 1:1`) + small `규격 고정` at the right. Each chip edits **its own** crop; switching animates the frame's aspect (200 ms) while the image stays.
  - `확대` slider (track 4 dp `line`, fill `acc`, knob 24 `ink`) with mono value `1.4×`. 1.0× = the largest frame of that ratio inside the (rotated) image — *derived* from the board readout (3024 / 1.4 = 2160, 4032 / 1.4 = 2880), not stated in words. Maximum and step: not specified (Q-IM-04).
  - tool buttons with labels: `90° 회전` · `좌우 반전` · `처음대로` (resets zoom, position, rotation, flip of the current ratio). Rotation direction: not specified.
  - holo row (holo-capable slots only): `홀로그램 효과` · `은빛 · 기울이면 흐름 · 애니메이션 줄이기면 고정` + toggle.
- Dragging the image moves it under the fixed frame `[PROPOSED]`; whether the corner marks resize the frame is not specified (Q-IM-04).
- `적용`: saves for each edited ratio `{x, y, w, h}` in source pixels of the rotated image + rotation (0/90/180/270) + horizontal flip + zoom, and the holo flag; the original file is kept untouched (CNV-29). `취소`: discards all edits of this session.
- Re-crop any time from the slot ("원본은 보관, 언제든 다시 자르기").

*Sources:* board `ImageCrop`; DS `CropFrame`; main tab 자르기; 저장 파일 양식 §4 이미지.

#### CNV-29 · 이미지 저장 (originals, crops, reuse) — P1 · needsExternal: no
- An imported image is stored **once** as the original file, named by its content hash, in `assets/<hash>.<ext>` (file format §2, §4: "원본 하나를 내용 해시 이름으로. 자른 것은 숫자(영역 · 회전 · 반전)로만"). Large originals are kept as they are.
- Each use (owner × slot) stores: source (`preset:<id>` or the hash), one crop per ratio of the slot, holo flag. These go to `assets/index.json` ("자른 정보 · 홀로 · 쓰이는 곳").
- The same original used in several places is stored once. Cropped bitmaps, thumbnails and previews are caches, never exported — except `preview.webp`, the 서재 card cover of the project (none for hidden projects; LIB).
- Images belonging to a deleted item go to trash with it (rule 20 §1 "딸린 것 … 이미지").

*Sources:* 저장 파일 양식 §2, §4, §6; rule 20.

#### CNV-30 · 홀로그램 효과 (silver holo on images) — P1 · needsExternal: no
"이미지마다 켜는 선택지(기본 끔). 은빛 바탕 + 옅은 무지갯빛, 기기를 기울이면 흐른다. 애니메이션 줄이기면 고정. 글자 뒤 배경에는 쓰지 않는다." In scope for v1 (범위 table).
- **Where**: holo-capable slots only (표지, 초상, 장소 · 용어 · 세력). Never on `인물 배경` or `페이지 머리 이미지`.
- **Default**: off; new images take `홀로그램 효과 기본` (CNV-19, default off).
- **Look** (confirmed by the user 2026-11-21, "지금이 딱 좋네"): a silver metallic gradient (dark theme `#8E949C`→`#F4F5F7`; light theme pewter `#33373E`→`#666C74`) plus a faint iridescent tint layer (pink · sky · lilac) at 45 % (dark) / 32 % (light) / 50 % on card foils, overlay blend; the tint layer moves faster than the silver layer (parallax); a narrow white sheen band (≈ 24 % wide, 105°) slides with the tilt. Text drawn over holo keeps ≥ 4.5:1 (worst case measured 5.3:1). Reference CSS: `scr_cards.py` `.hfoil` / `.hsheen` (tint stops `rgba(255,150,200,.5)`, `rgba(140,210,255,.5)`, `rgba(190,160,255,.5)` at 6/12/18 %, silver stops `#7E848C`, `#E9ECEF`, `#A3A9B1`, `#F6F7F9` at 0/6/12/18/26 %, 115°, background-size 260 %, offset by tilt: tint 50 %, silver 30 %).
- **Input**: device tilt (rotation sensor). No gyro → drag on the image to tilt; on release it sways slowly back (experiment board `Holo`: `기기 자이로 → 없으면 끌어서 기울이기 → 손을 떼면 천천히 흔들림`).
- **Turning on**: one silver sweep left → right, decelerate 480 ms, then it follows tilt only.
- **Off-screen**: the sensor listener is unregistered when no holo image is visible.
- **Reduce motion**: static foil, no sweep, no tilt tracking (`애니메이션 줄이기 · 정지`).
- Real-device feel is an open check (planning 남은 확인 "자이로 홀로그램 느낌").

*Sources:* main tab 카드 보기 · 이미지, 홀로그램, 범위; memory (HOLO = SILVER, tint strengths confirmed); boards `ImageCrop`, `ImagePick`, `MHolo`, `Holo`; `scr_cards.py`.

#### CNV-31 · 홀로 실험 (holo on components, "rare moment" screen) — P2 · needsExternal: no
Experimental (`‘1장 완결’ 같은 드문 순간 화면은 아직 실험`). Not for daily editing.
- `HoloKit`: one shared tilt drives holo on title text (`1장 완결`), marks `◆ ◇`, a badge (`LEGEND`), a chip (`주연 · 리엔`), the primary button sheen (`쓰기 시작`), a progress fill (`1장 7/10`), a toggle track, a card border (`하르트 · 정체 공개` · `1-9 · 표시 이름 ??? → 하르트`); note `은빛 모든 단계 대비 4.5:1 이상`.
- `HoloMoment`: project name, `1장 완결`, `등불지기 · 1-1 ~ 1-10`, stats `48,210` `글자` · `31` `씬` · `12` `인물`, `가장 많이 말한 인물` · `리엔 · 214줄`, buttons `카드로 저장` · `2장 시작`.
- Trigger, what `카드로 저장` saves, and where `2장 시작` goes are not specified (Q-IM-11).

*Sources:* boards `Holo`, `HoloKit`, `HoloMoment`; main tab 홀로그램.

### E. Inner screen, fold and unfold

#### CNV-32 · 창 크기와 화면 틀 (window-size classes, adaptive frame) — P0 · needsExternal: no
- Classify by the **app window** width: Compact < 600 dp, Medium 600–840, Expanded ≥ 840 (Large 1200–1600, XL ≥ 1600 behave as Expanded). Cover screen ≈ Compact (390 reference). Unfolded portrait is usually Medium, landscape Expanded (800 reference; 1:1 test profile 802 × 802).
- Two panes on Medium must be **enabled explicitly** (`calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth`; material3-adaptive 1.3.0). Samsung recommends a 50:50 split on Z Fold (see Q-LY-01).
- Compact: side panels and popovers become **bottom sheets** (`Sheet`); every module has a cover version, including 지도, 캔버스, 관계도. Medium/Expanded: side panels, popovers, cards.
- Recompute the layout mode on every fold/unfold and configuration change; Android 16 (API 36) ignores orientation/size locks on sw ≥ 600 dp.
- No tappable control near the hinge / fold line (`FoldingFeature` bounds).
- Script keeps its own unfolded layout (manuscript + side panel, SCR); canvas and map use full-surface + popover/card; list modules use CNV-33.

*Sources:* main tab 커버 화면과 펼친 화면 (구현 기준); research §4; DS `Sheet`.

#### CNV-33 · 목록 모듈 펼친 화면 배치 (two-pane / single column + setting) — P1 · needsExternal: no
Applies to `서재 · 홈 · 인물 · 세계 · 연표 · 기록함 · 점검`. Cover screen unchanged; script keeps its side panel.
- **`목록 + 상세`** (default): left = the module's cover list (390 reference), 1 dp `line` divider, right = the selected item's detail (409 reference). Pairs (boards `Two*`): 서재 | 프로젝트 홈 · 홈 | 스토리 구조 · 인물 목록 | 인물 · 용어 목록 | 용어 카드 · 연표 | 사건 · 기록함 | 기록 · 점검 | 원고 위치. Selecting a row updates the right pane; the selected row shows `sel` fill + 2 dp `acc` bar at the left. What the right pane shows before anything is selected is not specified (Q-LY-02).
- **`가운데 한 칸`**: one column at reading width 560 (`measure`) centred; margins filled with `raise` at 50 % opacity; the column on `bg` with 1 dp `line` at both sides (boards `Col*`; on 800 the margins are 120 each).
- **Setting** `설정 › 화면 › 펼친 화면 배치` (board `LayoutSettings`; screen container LIB-66): header small `설정 › 화면`, title `펼친 화면`; section `펼친 화면 배치`; two radio cards (round radio, 22 dp; selected card `sel` fill + 1.5 dp `acc`, others 1 dp `edge`, radius 16, with a 72 × 72 diagram): `목록 + 상세` · `왼쪽 목록 · 오른쪽 고른 항목` (default) and `가운데 한 칸` · `읽기 폭 유지 · 양옆 여백`; footnote `서재 · 홈 · 인물 · 세계 · 연표 · 기록함 · 점검에 적용 · 커버 화면은 그대로 · 스크립트는 옆 패널 방식 유지`. The value shows on the 설정 root row (`목록 + 상세 ›`).
- Stored in app preferences (not in `.civilight`).
- Module specifics: LIB-06 (서재), CST-06 (인물); others by their owners.

*Sources:* main tab 커버 화면과 펼친 화면; 범위 table row 가운데 한 칸; boards `TwoLibrary`, `TwoHome`, `TwoCast`, `TwoWorld`, `TwoTimeline`, `TwoLr`, `TwoCheck`, `ColLibrary`, `ColHome`, `ColCast`, `ColWorld`, `ColTimeline`, `ColLr`, `ColCheck`, `LayoutSettings`.

#### CNV-34 · 접기 · 펼치기 (fold/unfold transitions) — P1 · needsExternal: no
Rule: `만지던 것은 제자리, 나머지가 비켜 선다`.
- **Anchor**: the selected block, the selected item, the map centre or the caret keeps its on-screen height. Blocks above it re-wrap and grow upward.
- **Same element, different shape**: bottom sheet ↔ side panel; place sheet ↔ place card. Never closed and reopened: same tab, same scroll offset.
- **Second pane**: on unfold it enters from 48 dp to the right (fade + translate); on fold, the **last-touched pane** stays on the cover (looking at the detail → the cover shows the detail screen).
- **Timing**: unfold decelerate 360 ms; fold accelerate 240 ms; glass bars keep their blur during the transition. Reduce motion: no move, no shape change — crossfade 120 ms.
- **Typing**: caret, keyboard and the written line's position are kept; an input inside the panel moves with the panel.
- **Script** (`MFoldScript`; SCR-66/67): same panel, same tab (`시간 · 참고 · 속성 · 모듈`), same scroll; selected `#043` keeps its height; a closed panel stays closed; typing keeps caret and keyboard. Reference geometry: cover sheet (y 470, h 330, top radius 28) ↔ side panel (x 496, y 112, w 288, h 672, radius 20); manuscript column 390 → 488; the command bar spans only the manuscript column when unfolded (right inset 12 → 324); the sheet's grab handle fades out as it becomes a panel.
- **List modules** (`MFoldList`): the list stays; the selected item (리엔) opens in the right pane; with `가운데 한 칸` the list column only widens to 560.
- **Map** (`MFoldMap`): centre and zoom unchanged — the sides simply show more; the selected pin (하층 거리) keeps its position; place sheet (y 520, h 280) ↔ place card at top-right (x 516, y 112, w 268, h 360), decelerate 360 ms, back to a sheet on fold; while editing, the tool bar and the vertices being drawn stay.

*Sources:* main tab 접기 · 펼치기; boards `FoldSpec`, `MFoldScript`, `MFoldList`, `MFoldMap`.

#### CNV-35 · 상태 저장과 복원 (state save/restore across fold and process death) — P0 · needsExternal: no
- Folding from inner to cover **closes the app by default** unless the user enabled continuation per app; process death can happen at any time. The app must save and restore so that reopening returns to **the same block · tab · scroll** ("앱이 접힐 때 닫히는 기기 설정이면, 상태를 저장해 두었다가 다시 열면 같은 블록 · 탭 · 스크롤로 돌아온다").
- Minimum saved state: current route and its arguments (project, scene, item keys), selected block/item key, panel open/closed + tab, scroll offsets of manuscript/list/panel, caret position, open sheet, map centre + zoom, canvas viewport. Unsaved text is never lost (editing auto-saves; SCR).
- Real-device check of state restoration is listed in planning 남은 확인.

*Sources:* main tab 구현 기준 and 접기 · 펼치기; `FoldSpec` row 앱 이어짐; research §4 연속성.

#### CNV-36 · 반쯤 접기 (half-fold postures) — P2 · needsExternal: no
"반쯤 접기(테이블 · 책 자세) 화면 — 규칙만 정함, 시안은 나중에."
- **Tabletop** (`HALF_OPENED` + horizontal hinge): top half = manuscript / map view; bottom half = command bar + panel tabs + keyboard. Nothing tappable near the fold line.
- **Book** (`HALF_OPENED` + vertical hinge): the two panes as they are; the fold line is the pane boundary.
- Script tabletop specifics: SCR-69.

*Sources:* main tab 접기 · 펼치기, 범위 table; `FoldSpec`; research §4 (자세 판단).

### F. Motion (모션)

#### CNV-37 · 모션 토큰 · 등록표 · 줄이기 (motion tokens, registry, reduce) — P1 · needsExternal: no
Principles: `움직임은 위치를 알려 줄 때만` (MotionSpec); `무엇이 바뀌었는지 · 어디로 갔는지` (MotionSpec2). Every motion has a job; nothing decorative loops.

**Easing tokens (M3).**

| Token | Curve | Duration | Used for |
| --- | --- | --- | --- |
| `STANDARD` (`표준`) | `cubic-bezier(.2,0,0,1)` | 200 ms | taps, value changes |
| `ENTER` (`감속 (등장)`) | `cubic-bezier(.05,.7,.1,1)` | 320 ms | sheets, block add, things appearing |
| `EXIT` (`가속 (퇴장)`) | `cubic-bezier(.3,0,.8,.15)` | 160 ms | closing, deleting |
| `LINEAR` (`선형`) | linear | — | caret blink and the recorder light band **only** |

**Never moves:** the line being typed, the caret position, glass bars while scrolling.

**Reduce motion** (CNV-46 decides when it is on): every movement, scale, parallax and sweep is replaced by a 120 ms opacity fade; the recorder light band stands still (`빛 띠 정지`); holo is static; fold/unfold crossfades 120 ms.

**Registry** (values are normative; "owner" implements the behaviour that triggers it):

| Motion (copy) | Movement | Timing | Haptic | Owner |
| --- | --- | --- | --- | --- |
| 스플래시 | CNV-38 | ≤ 1 s | — | CNV / LIB-08 routing |
| 블록 추가 | from 8 dp below, speaker = 직전 화자 | ENTER 320 | — | SCR · CNV-39 |
| 스와이프 버튼 줄 | block slides, buttons stagger 60 ms | 260 ms | — | SCR · CNV-40 |
| 시트 | back screen 96.5 % + blur 2 dp; sheet rises | ENTER 320 | — | all · CNV-41 |
| 시점 렌즈 | only changed values swap + underline sweep | 200 ms | — | lens · CNV-42 |
| 숨김 풀기 | blur 7 dp → 0 | 320 ms | — | hiding · CNV-43 |
| 검색 하이라이트 | rows still, boxes draw from left, 60 ms stagger | — | — | search · CNV-44 |
| 접기 · 펼치기 | CNV-34 | 360 / 240 | — | CNV-34 |
| 지도 생성 | drawn in step order 해안선 → 색면 → 산 → 강 → 숲 → 마을 · 길; doubles as the progress indicator, no loading spinner; reduce: finished map fades in 120 ms | ≈ 1.2 s total, each step decelerates | — | 지도 |
| 단계 다시 | only the changed layer: old leaves (EXIT 160) → new flows in | 160 → 400 ms | — | 지도 |
| 양식 바꾸기 | coastline fixed on top; inside crossfades (only before `이 지도로 시작`) | 200 ms | — | 지도 |
| 하위 지도 | zoom in centred on the tapped pin; back: zoom out from the breadcrumb; breadcrumb swaps only the changed segment | in ENTER 360, out EXIT 240 | `확인` | 지도 |
| 시점별 공개 | items becoming visible unblur 6 dp → 0, in the order the scrubber passes them | 320 ms, 60 ms apart | — | 지도 |
| 영역 꼭짓점 · 닫기 | vertex pops 1.3× → 1; line follows vertices; `닫기` (button only, no auto-close near the first point) fills the area | fill 200 ms | `틱` per vertex | 지도 |
| 경로 | each tapped segment draws; distance counter updates per segment; travel time enters from below | days ENTER 320 | `틱` | 지도 |
| 도장 | drops from 14 dp above, presses to 0.96× and settles; rotation detents every 15°, release snaps to nearest 15°; size steps 1 · 2 · 3 | 240 ms ENTER | `틱` per detent | 지도 |
| 육각 채우기 | spreads outward from the tapped cell by distance over contiguous same terrain; single-cell paint = 120 ms fade | ≤ 300 ms total | — | 지도 |
| 글로 설명 | conditions appear one by one; source phrase underlines, its condition row follows; tapping a condition re-underlines its phrase; on the result, tapping a condition briefly lights its map area | row 250 ms after its underline | — | 지도 |
| 목록 ↔ 카드 | CNV-45 | 360 ms | — | CNV-45 |
| 가로 카드 줄 | CNV-22/45 | 320 ms, parallax 8 % | `틱` | CNV-45 |
| 고르기 → 자르기 | CNV-45 | 360 ms, ratio 200 ms | — | CNV-45 |
| 홀로 켜기 | CNV-30/45 | sweep 480 ms | — | CNV-45 |

Haptics exist only where the table says `틱` or `확인`.

*Sources:* main tab 움직임, 지도 · 카드 · 이미지 모션 table, 접근성; boards `MotionSpec`, `MotionSpec2`, `M*` boards, `MMap*` boards; `scr_motion.py` (curves).

#### CNV-38 · 스플래시 (splash) — P1 · needsExternal: no
"아이콘 판 없이 원고 줄 로고, 1초 안에 서재로(Android 12 SplashScreen)."
- **System phase**: Android 12+ SplashScreen; window background = `bg` of the current theme; **no icon plate**; only the mark. Mark (96 dp box): three rounded bars height 6 (radius 3) in `ink` at y 22 / 44 / 66, widths 72 / 60 / 40, x 12; a caret 4 × 16 in `acc` at (58, 62).
- **App phase** (exit animation; total ≤ 1 s, curve `cubic-bezier(.05,.7,.1,1)`): 0 ms mark only (`시스템 스플래시 · 표식만`) → 250 ms the three bars have drawn left→right (scaleX 0 → 1, 80 ms apart) (`줄이 그어짐 · 감속`) → 700 ms the mark moves up and shrinks (towards translateY −300, scale .42, fading out) while 서재 rises from 14 dp below and fades in (`표식 위로 · 서재가 아래에서`) → 1 s 서재 accepts input (`서재 · 입력 가능`).
- During the splash the board shows the wordmark `CIVILIGHT` (IBM Plex Mono 11/16, letter-spacing .3em, `ink3`) 56 dp above the bottom.
- **Reduce motion**: mark → 서재 fade 120 ms.
- The destination and routing after the splash are LIB-08. The lock-screen "mark only" (rule 19 §4) uses this mark.

*Sources:* main tab 움직임; boards `Splash`, `SplashStrip`; `scr_motion.py` `mark`, `splash`.

#### CNV-39 · 블록 추가 모션 (block add) — P1 · needsExternal: no
- The new block appears **under the current block**, moving up from 8 dp below with opacity 0 → 1 (ENTER 320 ms); its speaker label shows the previous speaker with the small note `직전 화자` (block-add rule and copy: SCR). The caret jumps to the new block; the pressed key/button (`줄바꿈`, block pill) scales to 0.94 and back.
- Nothing else in the manuscript moves except the blocks below sliding down by the new block's height; the line being typed never moves.
- Reduce: the new block fades in 120 ms.

*Sources:* boards `MType`, `MotionSpec` row 블록 추가.

#### CNV-40 · 스와이프 버튼 줄 모션 (swipe button row) — P1 · needsExternal: no
- Swiping a block left reveals three buttons `시간` (with `◆` in `acc` when the block has a change) · `참고` · `속성` on `raise` (reference width 192 = 3 × 64). The block follows the finger and settles at −192 over 260 ms; each button fades in from 16 dp to the right, 60 ms apart (`밀기 260 ms · 놓으면 제자리 · 버튼 60 ms 간격`). Releasing returns the block to rest; **nothing executes on swipe**; off while typing (SCR).
- Reduce: buttons fade in 120 ms, no slide.

*Sources:* boards `MSwipe`, `MotionSpec`; feedback-no-guessing-interactions (swipe exception).

#### CNV-41 · 시트 모션 (sheets) — P1 · needsExternal: no
- Opening any bottom sheet: the screen behind scales to 96.5 % (origin 50 % / 30 %) and blurs 2 dp (`blur-back`); the shade (`shade`) fades in; the sheet rises from the bottom (ENTER 320 ms). Closing reverses with EXIT 160 ms. Sample sheet: `화자` · `닫기` · `검색 · 초성` · rows `리엔` `이 씬`, `하르트` `이 씬`, `세라` `최근`.
- Reduce: shade and sheet fade 120 ms; no scale, no blur change.

*Sources:* boards `MSheet`, `MotionSpec`; DS `Sheet`.

#### CNV-42 · 시점 렌즈 값 교체 (lens value swap) — P1 · needsExternal: no
- When the lens changes, **only values that differ** animate: the old value leaves upward 8 dp fading out, the new value enters from 8 dp below (200 ms); rows whose value is unchanged do not move (`바뀌는 값만 위로 빠지고 새 값이 아래에서 · 200 ms · 안 바뀌는 줄은 그대로`).
- The lens chip text swaps the same way and an `acc` underline sweeps under it from the left (`밑줄 쓸기`).
- Board example (리엔's record, lens `시점 · 1-7` → `시점 · 1-2`): `소속` `등불 결사 ◆` → `없음`; `하르트와` `경계` → `만나기 전`; `나이` `17세` and `첫 등장` `1-2` stay still.
- Reduce: changed values crossfade 120 ms.

*Sources:* boards `MLens`, `MotionSpec` row 시점 렌즈; rule 13 §6 (`만나기 전`).

#### CNV-43 · 숨김 풀기 모션 (unhide) — P1 · needsExternal: no
- When a hidden group unlocks, its content goes from blur 7 dp and opacity .55 to sharp and opaque in 320 ms; the lock icon crossfades to the unlock icon. Re-locking runs the reverse (`흐림 7 px → 0 · 320 ms · 다시 잠글 때 반대로`). Sample: `숨긴 기록 3` with two records.
- Reduce: fade only, no blur animation.

*Sources:* boards `MUnlock`, `MotionSpec`.

#### CNV-44 · 검색 하이라이트 모션 (search highlight) — P1 · needsExternal: no
- Results show the **whole sentence** as primary serif text with the match boxed **inline** (never split onto its own line); location is a small label above (`1-7 씬 3 · 나레이션`).
- Box: `mark.hit` — fill `hl`, 1 dp inset `acc`, radius 4, padding 1 × 3.
- When results update, rows stay where they are; only each box draws from its left edge (scaleX 0 → 1), 60 ms apart per result (`줄은 제자리 · 박스만 왼쪽에서 그려짐 · 60 ms 간격`). Sample query `오리지`, section `대사 · 3`.
- Reduce: boxes appear with a 120 ms fade.

*Sources:* boards `MSearch`, `MotionSpec` row 검색; feedback-search-highlight.

#### CNV-45 · 카드 · 이미지 모션 (list ↔ card, carousel, picker → crop, holo on) — P1 · needsExternal: no
- **목록 → 카드**: each list thumbnail (48 × 48, radius 10) grows **in place** into its card (222 × 296, radius 20), keeping order and position, ENTER 360 ms; list text leaves first (EXIT 160 ms), card text enters after; the second card moves aside to the right to show there is more. Card → list reverses.
- **가로 카드 줄**: snap to the nearest card's left edge, ENTER 320 ms; the image inside a card moves 8 % less than the card (parallax); haptic `틱` on snap. Reduce: no parallax, snap only.
- **고르기 → 자르기**: the tapped tile grows from its grid position into the crop stage (ENTER 360 ms) while the picker sheet fades; changing the ratio chip animates only the frame's aspect (200 ms), the image stays.
- **홀로 켜기**: one silver sweep left → right (ENTER 480 ms), then tilt-only; sensors off when off-screen; reduce: static, no sweep.

*Sources:* boards `MCardView`, `MCarousel`, `MImageCrop`, `MHolo`, `MotionSpec2`; main tab 모션 table.

#### CNV-46 · 움직임 · 투명도 (motion & transparency preferences, glass fallback) — P1 · needsExternal: no
Screen `설정 · 움직임 · 투명도` (board `ReduceSettings`; container LIB; header small `설정`, title `움직임 · 투명도`):
- Section `애니메이션`, radios (round):
  - `기기 설정 따름` · `‘애니메이션 삭제’가 켜져 있으면 줄임` — **default**.
  - `항상 줄이기` · `이동 없이 페이드 120 ms`.
  - `줄이지 않음`.
- Section `투명도`:
  - toggle `투명도 줄이기` · `유리 대신 불투명 막대 · 흐림 없음` — default off.
  - preview pair, each 120 tall, radius 14, sample text `멈추지 마. 저건 등불이 아니야. 바람이 잿더미를 쓸고 지나갔다.` under a bottom bar: left glass, label `유리`; right opaque, label `불투명`.
  - toggle `라이프 레코더 빛` · `녹음 중 아래 빛 띠` — default on (possibly the same preference as `LrSettings` `녹음 중 빛 띠` — Q-ST-04).
- Root row value shows the current animation choice (`기기 설정 ›`).

Effects:
- **Motion mode** = reduced if `항상 줄이기`, or if `기기 설정 따름` and the system's remove-animations setting is on (animator duration scale 0); full if `줄이지 않음` or the system setting is off. Reduced → CNV-37 reduce rules everywhere.
- **Glass**: bars, sheets and side panels are `glass` + 36 dp blur with masked fading edges. Haze tiers: API ≥ 33 full blur; API 31–32 blur works but re-rendering is costly; API ≤ 30 a translucent scrim replaces blur.
- **Reduce transparency on** (board `ReduceTransparency`): every glass surface draws opaque `raise`, no blur, no edge mask; the top bar gets a 1 dp `line` bottom edge, the bottom bar a 1 dp `line` top edge. Applies app-wide.
- Whether the system "reduce blur" setting can be read is a real-device check; if not, only the in-app toggle exists.
- Performance check: one blur source costs 25–33 MiB GPU memory (Pixel 8a); profile top bar + bottom bar + sheet at 120 Hz on the Fold.
- Preferences live in app settings (not in `.civilight`).

*Sources:* main tab 접근성, 남은 확인; boards `ReduceSettings`, `ReduceTransparency`, `Settings`; DS `GlassBars`; research §1.

---

## 3. Data

Common to every stored entity (file format §4, §7): `key` = ULID made on the device, never reused; references always by key (never by name, number or position); `order` = insertable order string; `updatedAt` (instant) + `updatedBy` (device id); optional `ext` JSON object; all text NFC-normalised UTF-8. Computed values are never stored.

### 3.1 Entities owned by this area

**Canvas** — one canvas document of a project.

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| key | ULID | no | canvas key; file name `canvas/<key>.json` |
| projectKey | ULID | no | owning project |
| name | String | no | header title (`1장 구상`) |
| order | OrderKey | no | order among the project's canvases |
| createdAt, updatedAt, updatedBy | Instant, Instant, DeviceId | no | bookkeeping |
| ext | Json | yes | extension slot |
| *viewport* | {cx, cy: Float, zoom: Float} | yes | **app DB only** `[PROPOSED]`; not exported |
| *lastOpenedAt* | Instant | yes | app DB only; drives `[PROPOSED]` "open the most recent canvas" |

**CanvasNode**

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| key | ULID | no | |
| canvasKey | ULID | no | |
| kind | enum `CHARACTER` · `FACTION` · `EVENT` · `TERM` · `MEMO` · `GROUP` | no | `인물` · `세력` · `사건` · `용어` · `메모` · `묶음` |
| refKey | ULID | yes | required for CHARACTER/FACTION/EVENT/TERM (Character, Faction, TimelineEvent, Term keys); null for MEMO/GROUP |
| text | String | yes | memo text (MEMO) or group title (GROUP) |
| x, y | Float | no | top-left in canvas coordinates (dp at zoom 1.0) |
| w, h | Float | yes | GROUP frame only `[PROPOSED]` |
| groupKey | ULID | yes | containing GROUP node `[PROPOSED]` |
| z | OrderKey | no | draw order |
| updatedAt, updatedBy, ext | | | |

**CanvasEdge** (a line)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| key | ULID | no | |
| canvasKey | ULID | no | |
| fromNodeKey, toNodeKey | ULID | no | drawing direction A → B (default `무엇으로 적을지` = A의 마음) |
| labelText | String | yes | plain label (`경계하는 사이`); kept when `그냥 선으로 두기` |
| relationKey | ULID | yes | set by `관계로 만들기`; points to the cast Relationship of the pair |
| updatedAt, updatedBy, ext | | | |

Invariants: both nodes on the same canvas; `relationKey` only when both nodes are `CHARACTER`; the relationship's pair = the two nodes' `refKey`s (unordered).

**Page** (free document)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| key | ULID | no | file `pages/<key>.json` |
| projectKey | ULID | no | |
| name | String | no | `하층 구역의 빛` |
| sectionLabel | String | yes | the small line above the name (`페이지 · 세계관 메모` shows `세계관 메모`); meaning open (Q-PG-03) |
| blocks | List<PageBlock> | no | body |
| headerImage | ImageUse key | yes | slot `PAGE_HEADER` |
| hidden | Boolean | no | item hiding (model: LIB-36) |
| sourceRecordKey | ULID | yes | recorder record this page was made from (rule 23) |
| order | OrderKey | no | order in the page list |
| createdAt, updatedAt, updatedBy, ext | | | `updatedAt` feeds `마지막 수정 M월 D일` |
| *linkCount* | Int | — | **computed**, never stored (`연결 N`, Q-PG-04) |

**PageBlock**

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| key | ULID | no | |
| type | enum `PARAGRAPH` · `HEADING` · `MEMO` (list open, Q-PG-01) | no | 본문 · 소제목 · ⌁ 작가 메모 |
| runs | List<TextRun> | no | shared run contract (SCR): plain text, link run `{targetKind, targetKey}`; display text resolved at render |
| order | OrderKey | no | |

**ImageAsset** (an imported original)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| hash | String | no | content hash; file `assets/<hash>.<ext>` |
| ext, mime | String | no | |
| width, height | Int | no | original pixels |
| source | enum `GALLERY` · `FILE` | no | where it came from |
| projectKey | ULID | yes | scope of `내 이미지` (Q-IM-03) |
| importedAt | Instant | no | |

**ImageUse** (an image placed in a slot)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| key | ULID | no | |
| ownerKind | enum `PROJECT` · `CHARACTER` · `PLACE` · `TERM` · `FACTION` · `PAGE` | no | |
| ownerKey | ULID | no | |
| slot | enum `PROJECT_COVER` · `CHARACTER_PORTRAIT` · `CHARACTER_BACKGROUND` · `CARD_REFERENCE` · `PAGE_HEADER` | no | CNV-25 |
| source | String | no | `preset:<id>` or an ImageAsset hash |
| crops | Map<Ratio, Crop> | no | one per ratio of the slot that has been cropped (empty for presets) |
| holo | Boolean | no | always false for `CHARACTER_BACKGROUND`, `PAGE_HEADER` |
| updatedAt, updatedBy | | no | |

**Crop**: `ratio` enum `R3_4` · `R1_1` · `R4_5` · `R9_16` · `R4_3` · `R16_9` · `R21_9`; `x, y, w, h` Int (source pixels after rotation); `rotation` Int ∈ {0, 90, 180, 270}; `flipH` Boolean; `zoom` Float (≥ 1.0).

**PresetImage** (static registry in the app, never stored): `id` (`ash-dawn` …), `name` (`잿빛 새벽` …), colours `c0 c1 s1 s2 s3`, `shardSet` 0–3 (CNV-26).

**CheckAck** (a `맞음` / `이대로 두기` mark)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| key | ULID | no | |
| kind | String | no | check-kind id from the registry (CNV-17) |
| subjectKeys | List<ULID> | no | permanent keys the item is about (block, term, scene, character …) |
| createdAt, createdBy | Instant, DeviceId | no | |

Lifecycle: removed together with a subject block; moves with it (rule 12 §1).

**AppPrefs** (this area's app preferences; app DB + the app-settings backup file owned by LIB; never in `.civilight`)

| Field | Type | Default | Meaning |
| --- | --- | --- | --- |
| viewByScreen | Map<ScreenId, `LIST` · `CARD`> | `LIST` `[PROPOSED]` | CNV-21 |
| unfoldedLayout | `LIST_DETAIL` · `CENTER_COLUMN` | `LIST_DETAIL` | CNV-33 |
| animation | `FOLLOW_SYSTEM` · `ALWAYS_REDUCE` · `NEVER_REDUCE` | `FOLLOW_SYSTEM` | CNV-46 |
| reduceTransparency | Boolean | false | CNV-46 |
| recorderGlow | Boolean | true | CNV-46 (relation to `LrSettings` `녹음 중 빛 띠`: Q-ST-04) |
| holoDefaultForNewImages | Boolean | false | CNV-19 / CNV-30 |

**SavedUiState** (saved instance state + app DB; never exported): route + arguments, selected block/item key, panel open + tab, scroll offsets, caret, open sheet, map centre/zoom, canvas viewport, last-touched pane (CNV-34/35).

### 3.2 Relations to other areas' entities

| From | To | Owner | Notes |
| --- | --- | --- | --- |
| CanvasNode.refKey | Character · Faction · TimelineEvent · Term | CST · 세계 · 연표 | reference only; deleted/hidden handling Q-CV-14 |
| CanvasEdge.relationKey | Relationship (pair, 사이, 마음 ×2, 설명, 호칭) | CST (rule 13) | write semantics CST-34 |
| `관계로 만들기` | ChangeRecord with placement `시점` (episode/scene key), no anchor block | 변화 기록 (rule 12 §1) | applies from the start of that unit |
| Page TextRun link | Character · Term · Place · Faction · Page … | SCR (run contract) | rule 19/20 rendering in CNV-16 |
| Page.sourceRecordKey ↔ RecorderRecord.insertedInto | RecorderRecord | 레코더 (rule 23) | both directions by permanent key |
| ImageUse.ownerKey | Project · Character · Place · Term · Faction · Page | LIB · CST · 세계 · CNV | trash with the owner (rule 20) |
| CheckAck.subjectKeys | Block · Scene · Term · Character … | SCR · 세계 · CST | |
| RecentItem (LIB-05) | Page, scene, character, term … | LIB | card visuals CNV-23 |

### 3.3 What goes into `.civilight`

| Path | Content | When |
| --- | --- | --- |
| `canvas/<canvasKey>.json` | Canvas (without viewport) + its nodes + its edges | always (file format §6) |
| `pages/<pageKey>.json` | Page + blocks (runs) | always |
| `assets/index.json` | every ImageAsset's metadata + every ImageUse (slot, source, crops, holo) — "자른 정보 · 홀로 · 쓰이는 곳" | always |
| `assets/<hash>.<ext>` | original image files | always |
| `preview.webp` | 서재 card cover of the project (from `PROJECT_COVER` 3:4 crop) | not for hidden projects (LIB) |
| CheckAck marks | `맞음` / `이대로 두기` | always ("다시 열어도 같은 경고가 다시 뜨지 않도록"); file location not specified (Q-CK-01) |
| — | AppPrefs, SavedUiState, canvas viewport, caches (cropped bitmaps, thumbnails), computed values (`연결 N`, check results) | never |

Presets are written as their name (`preset:ash-dawn`), never as files. Relationship data created from the canvas is written by the cast area (`cast.json`, `changes.json`), not into the canvas file.

Illustrative canvas file (field names are this spec's; the file-format owner may rename them while keeping the shape):

```json
{
  "key": "01JD…C1", "name": "1장 구상", "order": "a0",
  "nodes": [
    {"key": "01JD…N1", "kind": "FACTION",   "refKey": "01JD…F01", "x": 300, "y": 160, "z": "a0"},
    {"key": "01JD…N2", "kind": "CHARACTER", "refKey": "01JD…C07", "x": 150, "y": 330, "z": "a1"},
    {"key": "01JD…N3", "kind": "CHARACTER", "refKey": "01JD…C02", "x": 470, "y": 330, "z": "a2"},
    {"key": "01JD…N6", "kind": "MEMO", "text": "2장 떡밥?", "x": 90, "y": 640, "z": "a5"}
  ],
  "edges": [
    {"key": "01JD…E3", "from": "01JD…N2", "to": "01JD…N3", "labelText": "경계하는 사이", "relationKey": "01JD…R12"}
  ],
  "updatedAt": "2026-11-22T08:12:00Z", "updatedBy": "dev-1"
}
```

---

## 4. Rules as test cases

All cases use the sample project 「잿빛 새벽의 등불」 (R-15). **Fixture S0** (from rules 11, 12, 13 and the boards):
- Episodes 1-1 … 1-10 in 1장; 집필 선 = end of **1-7 씬 3**; lens chip at 최신 reads `최신 · 1-7`.
- 1-7 씬 3 blocks: `#042` 리엔 "저기… 불빛이 보여요." · `#043` 하르트 "멈추지 마. 저건 등불이 아니야." · `#044` narration "…오리지늄 등불이 한 번 깜박였다." (`◇` first reveal of 오리지늄 등불).
- Relationship **리엔 · 하르트**: block records at `1-7 #043` — 리엔 → 하르트 마음 `경계` (설명 `불빛을 아는 낯선 사람`), 하르트 → 리엔 마음 `보호` (호칭 `꼬마`); planned records at `1-9` — 사이 `동료`, 리엔 → 하르트 마음 `신뢰` (호칭 `하르트`).
- Relationship **리엔 · 세라**: 사이 `후배 ↔ 선배` (세라 is 리엔's 선배); 리엔 → 세라 마음 `의지` (설명 `의지하는 선배`); 세라 → 리엔 마음 not written.
- Relationship **리엔 · 오웬**: 사이 `거래 상대`; no 마음.
- 리엔: 소속 `등불 결사` from 1-3 (before: `없음`); 나이 `17세`; 첫 등장 `1-2`. 하르트: display name `???` until 1-8, `하르트` from 1-9.
- Canvas `1장 구상` with nodes 등불 결사 (세력), 리엔 (인물), 하르트 (인물), 대정전 (사건), 오리지늄 등불 (용어), 2장 떡밥? (메모).

Where a case needs a pair **without** a relationship it says so explicitly (fixture **S0-noRel**: S0 minus the 리엔 · 하르트 relationship and its records).

Functions named below are the suggested pure, JVM-testable seams: `relationLineStyle(rel, lens)`, `lineLabelModel(edge, nodes)`, `valueAt(cell, lens | blockPosition)`, `cropReadout(...)`, `motionMode(...)`, etc.

### 4.1 Graph engine and relation-backed lines (rules 11, 13)

**T-CNV-01 · relation line at 최신.** Given S0, lens `최신 · 1-7`. When `relationLineStyle(리엔·하르트, 최신)`. Then `dashed = true` (only 마음), no middle label, end label near 리엔 = `경계 →`, end label near 하르트 = `← 보호`.

**T-CNV-02 · relation line at 1-9.** Given S0, lens `1-9`. When `relationLineStyle(리엔·하르트, 1-9)`. Then `dashed = false` (사이 exists), middle label `동료`, end labels `신뢰 →` (리엔 side) and `← 보호` (하르트 side).

**T-CNV-03 · 1-8 equals 1-7.** Given S0, lens `1-8`. Then the style equals T-CNV-01 (rule 11 §2 table: 렌즈 1-8 → `경계`).

**T-CNV-04 · before the first change → no line on the relationship map.** Given S0, lens `1-6`. When the relationship map computes lines. Then there is **no** line between 리엔 and 하르트 and the state label is `만나기 전` (rule 13 §6; never "모르는 사이").

**T-CNV-05 · 사이 only.** Given S0, lens `최신 · 1-7`. When `relationLineStyle(리엔·오웬)`. Then solid, middle label `거래 상대`, no end labels.

**T-CNV-06 · one side's 마음 never mirrored.** Given S0. When `relationLineStyle(리엔·세라, 최신)`. Then solid (사이 exists), end label near 리엔 `의지 →`, **no** end label near 세라.

**T-CNV-07 · 호칭 only draws nothing.** Given S0 plus a new relationship 세라 · 오웬 whose only value is 세라 → 오웬 호칭 `오웬`. When the relationship map computes lines. Then no line between 세라 and 오웬 (rule 13 §5).

**T-CNV-08 · never colour by kind.** Given two unselected relation lines whose 마음 kinds are `경계` and `신뢰`. Then both stroke colours equal `ink3`; selecting one makes only that one `acc` (2 dp).

### 4.2 Line label sheet and `관계로 만들기` (rules 12, 13)

**T-CNV-09 · default by drawing direction.** Given canvas `1장 구상`. When a line is drawn 리엔 → 하르트 and its label model is built. Then `무엇으로 적을지` offers `사이`, `리엔의 마음`, `하르트의 마음` and preselects `리엔의 마음`. When the line is drawn 하르트 → 리엔 instead. Then `하르트의 마음` is preselected.

**T-CNV-10 · only 인물–인물 lines can become relations.** Given lines 리엔 → 등불 결사, 대정전 → 오리지늄 등불, 하르트 → 오리지늄 등불 and 2장 떡밥? → 리엔. When each label model is built. Then `showsRelationRows = false` and the buttons are exactly [`그냥 선으로 두기`] (no `관계로 만들기`, no `무엇으로 적을지`, no `종류`, no `시작 시점`).

**T-CNV-11 · chips alone record nothing.** Given S0-noRel and a line 리엔 → 하르트. When the author picks `리엔의 마음` and `경계` and closes the sheet with `닫기`. Then the cast repository has no relationship for the pair (리엔, 하르트), the change-record count is unchanged, and `edge.relationKey == null`.

**T-CNV-12 · `그냥 선으로 두기` stays on the canvas.** Given S0-noRel and the line 리엔 → 하르트 with label text `경계하는 사이`. When `그냥 선으로 두기`. Then `edge.labelText == "경계하는 사이"`, `edge.relationKey == null`, and no cast data changed.

**T-CNV-13 · `관계로 만들기` writes a `시점` record.** Given S0-noRel, line 리엔 → 하르트, sheet values `리엔의 마음` · `경계` · `시작 시점 1-7`. When `관계로 만들기`. Then exactly one relationship exists for {리엔, 하르트}; one change record exists with cell = 리엔's 마음 toward 하르트, kind `경계`, placement `시점` at episode 1-7, anchor block `null`; 하르트's 마음 toward 리엔 is still empty; 사이 is empty; `edge.relationKey` = that relationship's key.

**T-CNV-14 · one relationship per pair.** Given S0 plus a relationship 세라 · 하르트 with 사이 `동료`, and a canvas line 세라 → 하르트. When `세라의 마음` · `신뢰` · `1-7` → `관계로 만들기`. Then the pair {세라, 하르트} still has exactly one relationship; it now also holds 세라 → 하르트 마음 `신뢰` (placement `시점` 1-7); 사이 `동료` is unchanged.

**T-CNV-15 · a `시점` record applies from the start of its unit.** Given the state after T-CNV-13 (no block record). Then `valueAt(리엔→하르트, lens 1-6)` = none (`만나기 전`); `valueAt(…, lens 1-7)` = `경계`; in the script, the value just before block `#042` (1-7 씬 3) is already `경계`; the script panel shows `◆ 1-7부터 · 근거 블록 없음` with `블록에 잇기` above the first block of 1-7 씬 1. *Contrast (S0, block record at #043):* just before `#042` the value is `만나기 전`, at `#043` the panel shows the change "만나기 전 → 경계 ◆ 여기서", at `#044` `경계` (rule 11 §2; rule 13 wording).

**T-CNV-16 · a start point after the 집필 선 is 예정.** Given S0-noRel. When `관계로 만들기` with `리엔의 마음` · `경계` · `시작 시점 1-9`. Then at lens `최신 · 1-7` the value is not applied (listed as 예정); at lens `1-9` it is applied; when the first block of 1-9 is written (집필 선 moves into 1-9), it is applied at 최신 without any other action (rule 11 §1 경계 사례).

**T-CNV-58 · converted lines look like the relationship map; plain lines stay grey.** Given the canvas after T-CNV-13 (lens `최신 · 1-7`) and a plain line 대정전 → 오리지늄 등불. When both are styled on the canvas. Then the 리엔 → 하르트 line has the relation style of CNV-01 (dashed, end label `경계 →` near 리엔, no label near 하르트 because 하르트's 마음 is empty in S0-noRel) and the 대정전 → 오리지늄 등불 line is solid `ink3` 1.2 dp with no relation labels (rule 13 §7: "관계로 만든 선은 캔버스에서도 관계도와 같은 모양 … 그냥 선은 가는 회색 선으로 남아 구분된다").

### 4.3 Pages (rules 19, 20, 21, 23)

**T-CNV-17 · page links never raise term warnings.** Given the page `하층 구역의 빛` whose first paragraph contains a link run to 오리지늄 등불 ("…[오리지늄 등불]을 가진 쪽이 거리를 가진다"), the term's first reveal at 1-7 씬 3 `#044`, and the page created before any manuscript existed. When term usage is computed. Then the page mention is listed in the term's `쓰인 곳` under the group `원고 밖 · 메모` with the page as its location, and it produces **no** `▲ 공개 전 사용` and **no** `후보` (rule 21 §3–4 example "하층 구역 메모").

**T-CNV-18 · plain words are not references.** Given the page memo block `2장에서 값이 두 배로 뛰는 장면 넣기. 노아 시점으로?` (plain runs only). When 노아 is deleted (moved to trash). Then the memo text is byte-identical, has no dotted mark and no `◇`, and the page contributes no reference to 노아's delete-impact counts (rule 20 §2).

**T-CNV-19 · a link to a deleted item.** Given a page paragraph with a link run to 노아. When 노아 is moved to trash. Then the run renders as a dotted chip with `◇`; its card reads `지운 인물 · 노아` with buttons `다른 항목에 잇기` · `글로 풀기` · `되살리기` · `이대로 두기`. When `글로 풀기`. Then the run becomes plain text `노아`. (Alternative branch) When 노아 is restored instead. Then the link run is solid again with the same target key. (Alternative branch) When 노아 is permanently deleted. Then the chip reads `영구 삭제 · 노아`, `글로 풀기` is still offered and `되살리기` is not.

**T-CNV-20 · page made from a record.** Given the recorder record `음성 1:02` with text "2장 첫 장면은 비 오는 상층 관문에서 시작. 리엔이 처음으로 위를 올려다보는…", state `정리 안 됨`, no 연결. When `페이지로 만들기` into 「잿빛 새벽의 등불」 with the prefilled name accepted. Then a new page exists in that project whose name is a non-empty prefix of "2장 첫 장면은 비 오는 상층 관문에서 시작."; its body contains the record text; `page.sourceRecordKey` = the record key; the record still exists in the library with state `넣음`, its 넣은 곳 list contains the page, and its 연결 list now covers 「잿빛 새벽의 등불」 (a 연결 to the page or to the project — which one is not specified, Q-PG-06). When the record text is edited afterwards. Then the page is unchanged and the record screen shows `◇ 넣은 뒤 고침`; the check center shows nothing. When the page is edited instead. Then the record is unchanged.

**T-CNV-21 · source mark follows the record's deletion.** Given the page from T-CNV-20. When the record is moved to trash. Then the page's source mark reads `레코더 · 휴지통`. When the record is permanently deleted. Then it reads `지워진 기록` and the page text is unchanged.

**T-CNV-22 · hidden page while locked.** Given the page `하층 구역의 빛` hidden and the project's hidden items locked. Then a page list shows one locked row with count 1 instead of the page; a link run to it elsewhere renders `숨긴 항목`; it is absent from 최근 연 파일 and from search results (which end with `숨긴 항목 제외`) (rule 19 §6).

### 4.4 Check-center shell (rules 12, 19, 20, 21)

**T-CNV-23 · `전체` counts ◇ rows.** Given open items: `▲ 공개 전 사용` (1-4 씬 2), its sub-row `◇ 공개 전 후보 · 1-2 씬 1`, `▲ 화자 없음` (1-7 씬 2), `◇ 지운 인물 · 노아`, `▲ 보이스 비어 있음` (인물 · 리엔). Then the first filter chip reads `전체 5`.

**T-CNV-24 · `맞음` moves an item and persists.** Given open items `▲ 공개 전 사용` (1-4 씬 2, **without** a 후보 sub-row — what happens to a sub-row when its parent is marked is not specified), `▲ 화자 없음` (1-7 씬 2), `▲ 보이스 비어 있음` (인물 · 리엔) → `전체 3`. When `맞음` on `▲ 공개 전 사용 · 1-4 씬 2`. Then the chip reads `전체 2`, the item is listed under `맞다고 표시한 것`, a CheckAck exists for it. When the project is exported to `.civilight` and imported on another device. Then the item is still under `맞다고 표시한 것` and not in `전체`.

**T-CNV-25 · `이대로 두기` is a 맞음.** Given `◇ 지운 인물 · 노아`. When `이대로 두기`. Then the item moves to `맞다고 표시한 것`; the dotted speaker names in 1-6 씬 2 · 씬 4 stay dotted (rule 20 §3 table row 이대로 두기).

**T-CNV-26 · marks follow their block.** Given a CheckAck whose subject is block `#043`. When `#043` is moved to 1-7 씬 4. Then the mark still exists (same block key). When `#043` is deleted. Then the mark is removed (rule 12 §1 table row 점검 ‘맞음’ 표시).

**T-CNV-27 · locked hidden items collapse.** Given 노아 hidden and locked, and two open items about 노아. Then the list shows one row `▲ 숨긴 항목 관련 2 · 지문으로 보기` instead of the two (both items are still computed; how they count in `전체` is not specified — Q-CK-03). When unlocked. Then the two original rows are shown.

**T-CNV-28 · no lens chip, ▲ always warn.** Given the check-center screen. Then the header has no lens chip; every `▲` kind label uses `warn` (including `▲ 보이스 비어 있음`, which the `Check` board draws in `ink3`).

**T-CNV-29 · empty state.** Given no open items, two CheckAcks, last computation just now over 1-1 … 1-7. Then the screen shows `전체 0`, `어긋난 곳 없음`, `마지막 확인 · 방금 · 1-1 ~ 1-7`, and buttons `원고로` · `맞다고 표시한 것 2`.

### 4.5 Cards and images

**T-CNV-30 · crop readout.** Given an imported original 3024 × 4032, slot `프로젝트 표지`, ratio 3:4, zoom 1.4, no rotation. When `cropReadout`. Then crop = 2160 × 2880 and the label is `2160 × 2880 · 원본 3024 × 4032`.

**T-CNV-31 · zoom 1.0 = largest frame** (*derived* from the board readout, see CNV-28). Given the same original. When ratio 1:1 at zoom 1.0. Then crop = 3024 × 3024. When ratio 3:4 at zoom 1.0. Then crop = 3024 × 4032.

**T-CNV-32 · slot registry.** Then `ratios(PROJECT_COVER) = [3:4, 1:1]`, `ratios(CHARACTER_PORTRAIT) = [4:5, 1:1]`, `ratios(CHARACTER_BACKGROUND) = [9:16, 4:3]`, `ratios(CARD_REFERENCE) = [16:9]`, `ratios(PAGE_HEADER) = [21:9]`; `holoAllowed` is true for PROJECT_COVER, CHARACTER_PORTRAIT, CARD_REFERENCE and false for CHARACTER_BACKGROUND, PAGE_HEADER.

**T-CNV-33 · separate crops per ratio.** Given one original placed as 유리 정원's cover. When the 3:4 crop is changed. Then the 1:1 crop is unchanged and the original file is untouched.

**T-CNV-34 · holo defaults.** Given `holoDefaultForNewImages = false`. When an image is placed as 리엔's portrait. Then `holo = false`. Given `holoDefaultForNewImages = true`. When images are placed as 리엔's portrait and as 리엔's background. Then portrait `holo = true`, background `holo = false` (and the background's holo toggle is not shown).

**T-CNV-35 · originals stored once.** Given one gallery image used as 유리 정원's cover and as 세라's portrait. When exported. Then `assets/` contains one file `<hash>.<ext>` and `assets/index.json` lists two uses pointing to the same hash.

**T-CNV-36 · presets by name.** Given 잿빛 새벽의 등불's cover set to the preset 잿빛 새벽. When exported. Then the use's source is `preset:ash-dawn` and no image file is written for it.

**T-CNV-37 · hidden projects never get their own card.** Given visible projects 잿빛 새벽의 등불 (R-15) and 유리 정원 (ALL), and two hidden projects (one is 「검은 서약」, R-18), locked. When the 서재 card row is built. Then it is [잿빛 새벽의 등불, 유리 정원, locked card `숨긴 프로젝트 2`]; no card shows `검은 서약` or `R-18`; the section header reads `프로젝트 · 2 · 숨김 2`.

**T-CNV-38 · initial-letter placeholder.** Given a character `노아` and a project `유리 정원` with no image. Then their placeholders show `노` and `유` with the tag `이미지 고르기`.

**T-CNV-39 · portrait card name follows the lens.** Given S0. When the 인물 card row is built at lens `최신 · 1-7`. Then 하르트's card name is `???` with sub line `하르트 · 이름 공개 1-9`. At lens `1-9`. Then the card name is `하르트` (rule 11 §2).

**T-CNV-40 · trashed characters leave the cards.** Given S0 with 노아 in trash. Then the 인물 card view's first filter chip reads `전체 11` and no card shows 노아 (rule 20).

**T-CNV-41 · snapping.** Given a project row with pitch 236 (cover). When a fling ends at offset 300. Then it snaps to 236. Ends at 360. Then it snaps to 472. Ends at 100. Then it snaps to 0. A haptic tick fires once per snap.

**T-CNV-42 · parallax and reduce.** Given full motion. When a card is displaced by −236. Then its image is displaced by −236 × (1 − 0.08) relative to the screen (i.e. +18.88 relative to the card). Given reduced motion. Then the image displacement equals the card's (no parallax).

### 4.6 Layout and fold

**T-CNV-43 · window-size classes.** `sizeClass(390) = Compact`, `sizeClass(599) = Compact`, `sizeClass(600) = Medium`, `sizeClass(800) = Medium`, `sizeClass(839) = Medium`, `sizeClass(840) = Expanded`.

**T-CNV-44 · panes per layout.** Given a list module. When width 390. Then one pane (list or detail). When width 800 and `unfoldedLayout = LIST_DETAIL`. Then two panes [list, detail]. When width 800 and `CENTER_COLUMN`. Then one column of width 560 with 120 margins on both sides.

**T-CNV-45 · fold keeps the last-touched pane.** Given 인물 unfolded in `LIST_DETAIL`, 리엔 selected, last touch in the detail pane. When folded. Then the cover shows 리엔's record. Given the last touch in the list pane instead. Then the cover shows the list.

**T-CNV-46 · unfold keeps panel state.** Given the cover script with the bottom sheet open on tab `참고`, scrolled 120 dp, block `#043` selected at y = 300. When unfolded. Then the side panel is open on `참고` with scroll 120 dp and `#043` is at y = 300. Given the sheet closed instead. Then after unfolding the side panel is closed.

**T-CNV-47 · map keeps centre and zoom.** Given the map at centre (0.52, 0.41) and zoom 1.2 with 하층 거리 selected. When unfolded or folded. Then centre and zoom are unchanged and 하층 거리 stays selected.

**T-CNV-48 · fold timings.** `foldTransition(UNFOLD, full) = 360 ms, ENTER curve`; `foldTransition(FOLD, full) = 240 ms, EXIT curve`; `foldTransition(any, reduced) = crossfade 120 ms`.

**T-CNV-49 · restore after process death.** Given the script on 1-7 씬 3 with `#043` selected, panel tab `참고`, manuscript scroll 840 dp. When the process is killed and the app reopened. Then the same scene, selected block, tab and scroll are restored.

### 4.7 Motion and preferences

**T-CNV-50 · motion mode.** `motionMode(FOLLOW_SYSTEM, systemAnimationsRemoved = true) = REDUCED`; `(FOLLOW_SYSTEM, false) = FULL`; `(ALWAYS_REDUCE, false) = REDUCED`; `(NEVER_REDUCE, true) = FULL`.

**T-CNV-51 · tokens.** `STANDARD = 200 ms cubic-bezier(.2,0,0,1)`, `ENTER = 320 ms cubic-bezier(.05,.7,.1,1)`, `EXIT = 160 ms cubic-bezier(.3,0,.8,.15)`; under REDUCED every movement spec resolves to an opacity fade of 120 ms.

**T-CNV-52 · lens swap animates only changed rows.** Given 리엔's record at lens `1-7`. When the lens changes to `1-2`. Then the animated rows are `소속` (`등불 결사 ◆` → `없음`) and `하르트와` (`경계` → `만나기 전`); `나이` `17세` and `첫 등장` `1-2` are not animated.

**T-CNV-53 · search boxes stagger, rows stay.** Given query `오리지` with three results (1-7 씬 3 나레이션, 1-4 씬 2 세라, 1-2 씬 1 리엔). When results update. Then the boxes start drawing at 0, 60 and 120 ms; no result row changes position.

**T-CNV-54 · splash.** Under FULL the splash exit completes in ≤ 1000 ms; under REDUCED it is a single 120 ms fade.

**T-CNV-55 · holo sensor lifecycle.** Given a holo cover visible. Then the rotation sensor is registered. When it scrolls off-screen. Then the sensor is unregistered. Given REDUCED. Then no sensor is registered and no sweep runs when holo is switched on.

**T-CNV-56 · glass tiers.** `glassMode(api 30) = SCRIM`, `glassMode(31) = BLUR_COSTLY`, `glassMode(32) = BLUR_COSTLY`, `glassMode(33) = BLUR`; with `reduceTransparency = true`, `glassMode(any) = OPAQUE` (fill `raise`, no blur, no edge mask).

**T-CNV-57 · preference defaults.** A fresh install has `unfoldedLayout = LIST_DETAIL`, `animation = FOLLOW_SYSTEM`, `reduceTransparency = false`, `recorderGlow = true`, `holoDefaultForNewImages = false`.

---

## 5. Screens

Board names: see Conventions. "Cover" = Compact width, "inner" = Medium/Expanded (CNV-32). Components are DS components (`GlassBars`, `CommandBar`, `Sheet`, `Tabs`, `LensChip`, `Tag`, `Buttons`, `Controls`, `ListRow`, `LockRow`, `Toast`, `ViewToggle`, `ProjectCard`, `ImageTile`, `CropFrame`, `SearchHit`) plus this area's node card and line.

| Screen / state | Boards | Cover (390) | Inner (800) | Components |
| --- | --- | --- | --- | --- |
| 캔버스 (idle) | `CanvasC`, `Canvas` | full-surface graph; glass header (back · `캔버스` · name · lens chip); zoom column top-right (`+` · `82%` · `−`); hint `두 손가락 이동 · 확대 · 노드는 끌어서 이동` 92 dp above the bottom; 5-column tool bar `노드 · 인물 · 메모 · 선 · 묶음` | same surface; centred pill tool bar `노드 · 인물 카드 · 메모 · 선 · 묶음`; no zoom column drawn (Q-CV-07) | GlassBars (top), LensChip, node card, line, tool bar. No CommandBar drawn (Q-CV-15) |
| 선 라벨 | `CanvasCLabel` (sheet, top 340), `Canvas` (popover ≈300 wide next to the line) | bottom sheet with title, `닫기`, names line | popover, same rows (rule 13 §9) | Sheet, Tag chips, text field, Buttons |
| 노드 편집 | — (not drawn) | bottom sheet | popover | Sheet |
| 전체 관계도 (engine only; view owned by CST) | `CastMap`, `CastMapC` | graph + `목록 · 관계도` toggle + filters + sheet `리엔 · 하르트` | graph + glass card `고른 관계` | engine (CNV-01) |
| 페이지 | `Page` | header 80 (`페이지` · `⋯`), document body, memo block, command bar hint | not drawn; DS: unfolded manuscript reads in one column of `measure` 560 | GlassBars, CommandBar |
| 점검 센터 | `Check`, `TravelCheck`, `EmptyCheck`, `TwoCheck`, `ColCheck` | list with filter chips and rows | 목록 + 상세: list \| `원고 위치` (script at the item); 가운데 한 칸: 560 column | Tag, Buttons (text), ListRow |
| 설정 (container LIB) | `Settings` | rows (CNV-18) | via CNV-33 rules of the owner | ListRow, Controls, segmented theme |
| 설정 · 지도 · 이미지 · 연표 | `SettingsMore` | rows (CNV-19) | — | ListRow, Controls |
| 프로젝트 설정 (container LIB) | `ProjSettings` | rows (CNV-20) | — | ListRow, Controls |
| 도움말 · 안내 다시 보기 (LIB-21) | `HelpReplay` | rows + `다시 보기` tags + `모든 안내 처음부터` | — | ListRow, Tag, Buttons |
| 설정 · 라이프 레코더 (recorder) | `LrSettings` | radios + toggles | — | Controls |
| 펼친 화면 배치 | `LayoutSettings` | two radio cards with diagrams + footnote | — | Controls |
| 움직임 · 투명도 | `ReduceSettings`; applied: `ReduceTransparency` | radios, toggles, glass/opaque preview | — | Controls, GlassBars |
| 서재 카드 보기 (content LIB) | `LibCards`, `LibCardsWide` | project row 222 × 296 + 최근 연 파일 row 150 × 100 + library block | project row 236 × 336 + `새 프로젝트` card + 4 recent cards 176 × 110 | ViewToggle, ProjectCard, locked card |
| 인물 카드 보기 (content CST) | `CastCards` | tag chips; rows per tag group 196 × 245 / 132 × 165 | not drawn (CST-06) | ViewToggle, portrait card |
| 이미지 고르기 | `ImagePick` (sheet, top 118) | bottom sheet over the owner screen | not drawn (Q-IM-01) | Sheet, Tabs, ImageTile, Controls, Buttons |
| 자르기 | `ImageCrop` | full screen: top bar, stage, controls | not drawn (Q-IM-01) | CropFrame, Tag, slider, Controls |
| 이미지 규격 | `ImageSlots` | rules list | — | ListRow |
| 목록 모듈 펼친 화면 | `TwoLibrary`, `TwoHome`, `TwoCast`, `TwoWorld`, `TwoTimeline`, `TwoLr`, `TwoCheck`; `ColLibrary`, `ColHome`, `ColCast`, `ColWorld`, `ColTimeline`, `ColLr`, `ColCheck` | — | two panes 390 \| 409 (reference) or one 560 column | owner screens |
| 접기 · 펼치기 | `MFoldScript`, `MFoldList`, `MFoldMap`, `FoldSpec` | sheet / list / place sheet | side panel / list + detail / place card | Sheet ↔ side panel |
| 스플래시 | `Splash`, `SplashStrip` | mark → 서재 | same timing | — |
| 모션 | `MType`, `MSwipe`, `MSheet`, `MLens`, `MUnlock`, `MSearch`, `MotionSpec`, `MCardView`, `MCarousel`, `MImageCrop`, `MHolo`, `MotionSpec2`; map reference `MMapGen`, `MMapReroll`, `MMapFormat`, `MMapZoom`, `MMapReveal`, `MMapRegion`, `MMapRoute`, `MMapStamp`, `MMapHexFill`, `MMapText` | per CNV-37…45 | same | — |
| 홀로 실험 | `Holo`, `HoloKit`, `HoloMoment` | experiment screens | — | holo layers |

**Cover vs inner, general rule:** inner side panels and popovers become cover bottom sheets with the same content, tab and scroll (DS `Sheet`, CNV-34); unfolded documents read at `measure` 560; list modules follow CNV-33; canvas and map keep full-surface views and swap sheet ↔ popover/card.

---

## 6. Open questions

Only real gaps or contradictions between sources. Each item names the sources on each side.

### Canvas
- **Q-CV-01 · Canvas list.** File format allows many canvases (`canvas/<캔버스 키>.json`); boards show one (`1장 구상`); no list/create/rename/delete/switch UI; home shortcut `캔버스` target undefined.
- **Q-CV-02 · `노드` tool.** Tool bars list `노드` beside `인물 카드` (inner) / `인물` (cover); the main tab's node list is "인물 카드 · 세력 · 사건 · 용어, 메모, 묶음". Is `노드` "place a 세력 · 사건 · 용어 item" or a select/move mode? Also the label differs between sizes (`인물 카드` vs `인물`).
- **Q-CV-03 · Item picker and node edit.** Main tab: "노드 편집은 옆 팝오버 대신 아래 시트" — no board draws node editing or the picker used to place item nodes.
- **Q-CV-04 · 장소 nodes.** Places are not in the canvas node list (main tab) though they are project items like 세력 · 용어.
- **Q-CV-05 · Lens on item nodes.** Canvas board shows `하르트` while the lens is `최신 · 1-7` (display name `???` until 1-9 per rule 11 §2 and CastCards). Do item nodes follow display names, unrevealed terms, not-yet-appearing characters?
- **Q-CV-06 · 묶음.** Named as node type and tool; no behaviour or board.
- **Q-CV-07 · Zoom.** Range and step of zoom; zoom column only on the cover board.
- **Q-CV-08 · Line gesture.** Tap–tap or drag; self-lines; several lines per node pair; whether the label surface opens on creation.
- **Q-CV-09 · Label text.** Rule 13 §2 gives each cell a 설명; §7 the sheet a 라벨 글(선택). Does `관계로 만들기` store the label as the chosen cell's 설명? Is typed text kept when the sheet is closed without a button?
- **Q-CV-10 · 시작 시점.** Default (board `1-7` equals the lens; rule 13 §7 says lens position for `+ 관계`, not for canvas) and the picker (episode only or episode/scene; rule 12 §1: "그 에피소드나 씬의 처음부터").
- **Q-CV-11 · Paired 사이 and other sheet states.** Direction of a paired 사이 kind chosen in the sheet (선배 ↔ 후배) and which word the middle label shows; whether `관계로 만들기` is enabled before a kind is chosen; how an already converted line opens and is edited.
- **Q-CV-12 · Converted line without a value at the lens.** Relationship map draws nothing at `만나기 전` or with only 호칭 (rule 13 §5–6); a canvas line is a canvas object — hide it, or draw it plain?
- **Q-CV-13 · Deleting a converted line.** Does it delete or keep the relationship?
- **Q-CV-14 · Hidden/deleted items on canvas.** Rules 19 §6 and 20 do not list the canvas.
- **Q-CV-15 · Command bar.** Main tab 공통 개념: "명령 칸 · 모든 화면 아래 하나"; canvas boards (both sizes) and settings boards have no command bar.
- **Q-CV-16 · Relation kinds list (CST to settle).** Rule 13 §3 defaults (사이 대칭 7: 동료 · 형제자매 · 배우자 · 연인 · 친구 · 거래 상대 · 적; 짝 4; 마음 11 incl. 호감 · 불신) vs board `RCRelTypes` (`사이 12` = 대칭 6 incl. 소꿉친구 + 짝 6 incl. 보호자 ↔ 피보호자 · 주인 ↔ 하인; 마음 11 incl. 존경 · 연모, no 불신 · 호감) vs `CanvasCLabel` chips (`불신`). The canvas only reads the list.

### Pages
- **Q-PG-01 · Page block kinds.** Main tab: "/로 블록 추가, 작가 메모 블록"; board shows paragraph, sub-heading, memo. Full list (and whether script blocks like 대사 are allowed) not given.
- **Q-PG-02 · `⋯` menu** of a page: contents not drawn (hide, delete, header image, …).
- **Q-PG-03 · `페이지 · 세계관 메모`.** Is `세계관 메모` a category, a folder, or free text?
- **Q-PG-04 · `연결 4`.** Outgoing links, backlinks, or both? The sample body shows 3 linked words.
- **Q-PG-05 · Page list and entry.** DS `ViewToggle` and rule 19 §6 imply a page list; no board, no home shortcut (`스토리 · 인물 · 세계 · 연표 · 캔버스 · 설정`), search categories lack pages.
- **Q-PG-06 · Page from a record.** Paragraph vs memo block for the copied text; truncation of the prefilled name ("첫 문장 앞부분"); source-mark copy while the record is alive (only `레코더 · 휴지통` / `지워진 기록` are given).
- **Q-PG-07 · Hint copy.** `@로 인물 · 용어 연결, /로 블록 추가` (board) uses a comma; vocabulary rule: separators are ` · ` only.
- **Q-PG-08 · Deleting a page.** Trash or not; trash filters (`전체 · 프로젝트 · 씬 · 블록 · 인물 · 변화 · 레코더 · 세계`) have no 페이지.
- **Q-PG-09 · Sample name.** Board `하층 구역의 빛` vs rule 21 example `하층 구역 메모` (sample data; rule 10 pending).

### Check center
- **Q-CK-01 · Where `맞음` marks live in `.civilight`.** File format §6 says they are always stored; the §2 file tree has no place for them.
- **Q-CK-02 · Undoing / expiry.** Can a mark be removed from `맞다고 표시한 것`? Does a mark lapse when the underlying data changes (e.g., the sentence is edited)?
- **Q-CK-03 · Counting locked hidden items** in `전체` (rule 19: grouped into one row; count rule not given).
- **Q-CK-04 · Filter set.** `Check` shows `전체 5 · 용어 공개 · 화자 · 보이스 · 변화 · 지운 항목 · 맞다고 표시한 것` (no `이동`); `TravelCheck` shows `전체 5 · 용어 공개 · 화자 · 이동 4 · 보이스 · 변화` (no `지운 항목`, no `맞다고 표시한 것`), both counting 5 over different item sets. The union order used in CNV-17 is this spec's reading. Filters for `순서와 때가 다름` (rule 11), the rule-22 rows (`이동` or a new `씬`, undecided) and `숨긴 인물이 보이는 씬에 나옴` (rule 19) are not set. (`Check` also draws `▲ 보이스 비어 있음` in `ink3` against the DS rule; this spec follows the DS.)

### Empty and error states
- **Q-EM-01 · Not drawn for this area.** Sources give only the check center's empty state (`EmptyCheck`). Missing: a canvas with no nodes; an empty page (new page before any text); the picker's `내 이미지` tab with 0 images; image import failures (unsupported or unreadable file, photo-picker permission refused or cancelled); a crop source too small for a ratio. Copy for all of these is unspecified.

### Settings
- **Q-ST-01 · Root rows.** `설정 › 수위 등급`, `설정 · 라이프 레코더` and `설정 › 도움말 › 안내 다시 보기` are referenced by other boards and the main tab but the `Settings` board has no rows for them (LIB decides placement).
- **Q-ST-02 · 글자 크기** steps other than `보통` (`본문 18 · 행간 1.7`).
- **Q-ST-03 · Home `설정` shortcut** → app 설정 or 프로젝트 설정?
- **Q-ST-04 · Light band toggle twice.** `움직임 · 투명도` → `라이프 레코더 빛` and `설정 · 라이프 레코더` → `녹음 중 빛 띠`: one preference shown in two places, or two?

### Cards and images
- **Q-IM-01 · Picker and crop on the inner screen** are not drawn (research: large-screen quality tier asks not to stretch bottom sheets full width).
- **Q-IM-02 · Default view** per list screen (`목록` assumed).
- **Q-IM-03 · `내 이미지` scope** (this project or all projects) and whether presets can be cropped.
- **Q-IM-04 · Crop controls.** Zoom maximum and step; do the corner marks resize the frame; rotation direction; whether the `원본 W × H` readout shows rotated dimensions.
- **Q-IM-05 · Copy for non-cover slots.** Picker titles (only `표지 이미지` drawn) and ratio chip labels (only `카드 3:4` · `목록 1:1` drawn).
- **Q-IM-06 · Preset ids** other than `preset:ash-dawn` (they become part of the file format).
- **Q-IM-07 · Entry points** of `ImageSlots` and of the preset view behind `기본 이미지` `보기 ›`.
- **Q-IM-08 · Card view for 세계 and 페이지 lists** (DS names them; no boards).
- **Q-IM-09 · Image of a 최근 연 파일 card** per kind (scene, character, page, term).
- **Q-IM-10 · Portrait rows.** Which tag group gets the large cards; characters with several tags (one row each or once).
- **Q-IM-11 · Holo moment.** Trigger of `1장 완결`, what `카드로 저장` saves, where `2장 시작` goes (experiment).

### Layout and fold
- **Q-LY-01 · Two-pane split.** `Two*` boards 390 | 409; `MFoldList` 400 | 400; research: Samsung recommends 50:50; script `MFoldScript` 488 | 288 panel. Which is canonical in proportional terms?
- **Q-LY-02 · Right pane before a selection** in `목록 + 상세`.
- **Q-LY-03 · Hit target.** DS and main tab: 44 px; research (large-screen quality tier 2): 48 dp touch targets.

### Motion
- **Q-MO-01 · Blur radius.** `숨김 풀기` 7 px (`MotionSpec`, `MUnlock`) vs `시점별 공개` 6 px described as "숨김 풀기와 같은 모션" (`MMapReveal`).
- **Q-MO-02 · Sheet overshoot.** `MSheet` loop rises 6 px past rest before settling; spec text says only "시트 감속".
- **Q-MO-03 · `줄이지 않음` vs system "remove animations".** Compose follows the system animator scale; overriding it may not be possible (implementation check).
- **Q-MO-04 · Splash wordmark.** `CIVILIGHT` at the bottom of `Splash`: system branding image or app-phase drawing?
- **Q-MO-05 · System reduce-blur setting** readable by the app (planning 남은 확인) — if not, only the in-app toggle.
