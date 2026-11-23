# 40 · 세계 (World: terms, places, factions, timeline, maps) — feature prefix `WLD`

Implementation spec for the world area of Civilight. Engineers implement from this file alone; every Korean string in `code` is UI copy and must be used verbatim.

**Sources read (2026-11-22).** Planning doc main tab (live, rev 71: sections 세계 · 연표 · 지도, 지도 만들기, 지도 생성 · 양식, 지도 ↔ 연표, 카드 보기 · 이미지, 범위, 남은 확인) · rule tabs 21 (live rev 2, confirmed 2026-11-22), 11 (rev 8), 12 (rev 8), 18 (rev 6), 19 (rev 2), 20 (rev 2), 22 (rev 2), 인물 항목 (rev 6), 저장 파일 양식 (rev 8) · local draft `rules/21.md` (fuller text of rule 21; used where the live tab is condensed) · memory files (concept, vocabulary, rules-time/blocks/relations/travel/19-23, character-fields, feedback-*) · visible text of every generated board of 2026-11-22 01:05 (`design/night-manuscript/project/*.dc.html`, re-extracted) · generator source in part (`gen/scr_world.py` term/world/timeline functions, `scr_map.py` first half, `scr_link.py` timeline-anchor and place-time functions, `scr_new_time.py` candidate and link-all functions, `mapgeo.py` in full; `scr_mapgen.py`, `scr_new_*.py` board lists grepped) · design system `README.md`, `tokens.json`, `components/*/README.md` · research `2026-11-20-design-research.md`.

**Precedence.** Confirmed rule tabs and memory > planning main tab > board generator output of 2026-11-22 > older board text > design system README > research. Inside rule 21 the live tab wins on conflict and the local draft fills what the tab condenses; each use of the draft is cited as `21 draft`. Counts printed in rules 12 and 20 (`쓰인 곳 · 4`, `오리…`, `▲ 새로 2`) predate rule 21 and are recomputed here under "only links count". Where a newer source settles an older one this spec follows the newer one and says so; only unresolved conflicts go to section 6.

**Conventions.**
- `[PROPOSED]` marks behaviour or copy that no source states and that this spec had to choose so the feature is implementable. Unmarked text is from a source. No `[PROPOSED]` item adds a capability that the sources do not name.
- "cover" = cover screen (390×844 boards, Compact width < 600dp). "inner" = unfolded inner screen (800×800 boards, Medium 600–840dp / Expanded ≥ 840dp). Unfolded portrait is usually Medium, so two-pane layouts must be enabled explicitly for Medium.
- Board names drop the `NmDark`/`NmLight` prefix and `.dc.html` (e.g. `Term`, `MapEdit`, `RTLinkAll`). Every board exists in dark and light.
- Separators in copy are ` · ` (space, middle dot, space). No em dash in UI copy.
- Inside quoted sentences in `code` copy, `[…]` marks a linked or boxed span (rendered as the link shape or the `SearchHit` box); the brackets are not characters of the text. `<…>` marks a placeholder filled at runtime.
- Markers: `◆` change (`acc`), `◇` first reveal / soft notice (`ink3`), `▲` warning (`warn`, amber; tapping a `▲` opens the check center). Dotted underline / dotted outline = attached to something deleted (rules 12, 20).
- Points in story order use the episode form `1-7`, scene form `1-7 씬 3`, block form `#044`; chapters `2장`. "Lens" = 시점 렌즈. "집필 선" = end of the furthest scene that has content (rule 11-1).
- Words: `씬` (never 장면 in UI), `지우기` (to trash) vs `삭제` (permanent), `되살리기`, `고르기`, `바꾸기`, `고치기`, `건너뛰기`, `넣기` (apply), `제안 받기`. A scene's location is `장소` (a pin's place) or `지역` (a map region, called `영역` inside the map editor). Ordinary phrases `쓰인 곳`, `이곳의 시간` stay.
- **0.2.0 (frozen scope `docs/status/0.2.0-scope.md`, 2026-11-24).** Sections that change for app version 0.2.0 carry `(0.2.0)` in their heading or on the changed line. Existing IDs keep their numbers. New features are WLD-70 … WLD-76, placed at the end of their section, and new tests are WLD-T73 onward. Old behaviour that 0.2.0 replaces stays readable, marked `Superseded (0.2.0):`; hidden features keep their text because their data and domain code stay (hiding is never deleting, scope §8).
  - What changes:
    - create in place, U-15: WLD-70 (places, terms, factions, place picker) and WLD-74 (연표 events);
    - lens chip default `최신`, S-1: WLD-71;
    - automatic first reveal, S-5: WLD-72;
    - candidate setting, list, underline and the `참고` row, S-5: WLD-73;
    - simplified map and the travel check off by default, S-3 and S-2: WLD-75;
    - what the `게임 제작 도구` switch hides, G-2: WLD-76.
  - U-16 does not touch this area: the world lists keep their `목록 | 카드` view (WLD-05, D2).
  - Copy that no source gives is a `TODO_COPY_WORLD_<NAME>` constant, listed in WLD-76's copy table. It is never invented here.
- Sample project: 「잿빛 새벽의 등불」 (R-15; stored, not shown from 0.2.0, S-4). People 리엔 (출생 1183, 등불 결사), 하르트 (표시 이름 ??? until 1-9, 나이 알 수 없음), 세라 (기록관), 오웬 (밀수상), 노아, 마르타. Terms 오리지늄 등불 (별칭 오리지늄 · 빛돌, 첫 공개 1-7 씬 3 #044), 대정전 (첫 공개 1-4), 등불 결사 (첫 공개 1-1), 잿불의 서약 (공개 예정 2-5), 상층 송전망 (공개 예정 2장). Places 하층 거리 · 결사의 문 · 잿더미 시장 · 상층 관문 · 송전탑 폐허 · 하층 구역 (sub-map). Factions 등불 결사 · 상층 경비대. World events 대정전 (1195), 상층 송전망 사고 (1201). Story starts 1200; 집필 선 = 1-7 씬 3.

---

## 1. Scope

### 1.1 What this area owns

1. The `세계` module: tabs `장소 · 용어 · 세력 · 지도`, the term / place / faction lists and cards (cover and both inner layouts), creation, editing, hiding, deleting, restoring and merging of terms, places and factions (world-specific parts of rules 19 and 20).
2. Card fields for places, terms and factions: fixed fields named in the sources plus project fields defined through the shared field mechanism (인물 항목 rule 5: same settings screen, tabs `인물 · 장소 · 용어 · 세력`).
3. Term reveal and usage (rule 21 entire): aliases, the `◇ 첫 공개` record (its value and lifecycle; the block-anchoring engine is consumed), the term-link semantics of `@` links, `쓰인 곳` groups, `▲ 공개 전 사용`, text candidates (matcher, particles, `끊김`, overlap), `잇기 · 아님 · 맞음`, `모두 잇기 · 모두 아님`, `글자 찾기` on/off, the candidate index and its refresh schedule, and the term rows the check center lists under `용어 공개`.
4. World time and the `연표` module: project world-time mode (`연도만 · 세계 달력 · 시간 없이`), the world calendar, world events and story events, the timeline list, event detail, the three timeline views (`에피소드 기준 · 이야기 시작부터 · 기준 사건`), automatic ages on events.
5. Map making (`지도 만들기`): map list, new map, bases (generated terrain, imported image, sketch photo, node map), overlay data in 0–1 coordinates (pins, regions, routes, labels, layers, stamps), the visible tool bar, scale calibration, travel modes (project setting), nested maps, per-item reveal points, large-image tiling, map image export.
6. Map generation (`지도 생성 · 양식`): the single seeded generator, formats (`육각 · 다각형 · 상세 지형 · 윤곽 · 노드`), styles (`색면 · 먹선 · 색 · 양피지`), step controls (seed · 고정 · 다시), candidate history, re-rolling after start, the hex / polygon / detailed-terrain editors, and `글로 설명` (text → conditions → generator input).
7. Map ↔ timeline (`지도 ↔ 연표`) outputs: `인물 동선`, `이곳의 시간`, `연표 + 지도`, faction regions that change over story time, and the distance functions (route / straight line / ancestor pins / nearest region point) that the travel check consumes.
8. The place / region picker that other areas open (`지도에서 고르기 ›`, list picker, `새 장소 · 지도에 핀을 꽂으며 만들기`).
9. App settings rows `설정 › 지도 · 이미지 · 연표` that concern maps (`새 지도 그림`, `지형 만들기 · 글로 설명`, `큰 지도 내보내기`) and the project settings rows `세계 시간` and `이동 수단`.
10. What this area writes into `.civilight` (`world.json`, `maps/<지도 키>.json`, map base images in `assets/`, world-related records in `changes.json`).

### 1.2 Hand-offs to other areas

Sibling specs are being written in parallel; `docs/spec/10-script.md` existed when this file was written and is referenced as `SCR`. Other areas are named by subject.

| Area | What it owns that this spec only consumes or references | Contract this spec relies on |
| --- | --- | --- |
| 스크립트 (`10-script.md`, SCR) | Text runs and `@` link-run storage and rendering in the manuscript, the `@` picker UI, display-layer sheet (`표시`), anchoring `◆` and `◇` to blocks (rule 12: `근거 지워짐`, move, split, merge), the 시간 tab rows `◇ #044 · 용어 첫 공개`, scene head and `전환 문구` storage (rule 22) | (a) every link run carries a stable `linkId` (ULID) so `맞음` can attach to it; (b) the text-run model exposes run kinds `plain · link · paren` per block / option line / 전환 문구; (c) the anchor engine accepts a `◇ 첫 공개` record exactly like a `◆` record; (d) SCR renders candidates and pre-reveal links from WLD-27's query API |
| 장소와 때 (scene place & time; rules 11, 18, 22) | Scene `장소/지역` and `때` storage (base + interval + band/exact + precision + 회상), resolved story time, `N일째` labels, time bands, `씬 · 장소와 때` sheet (`SceneWhere`), cascade preview, flashback sheet, travel check verdicts and chips, `장소 · 때 한꺼번에` (`Bulk*`) | Provides per scene: place ref or region ref, resolved time as a range `[earliest, latest]` in minutes since story start (or a world date for 회상), flag 회상, cast list (speakers + silent). Consumes from WLD: place picker (WLD-52), distance function (WLD-66), travel modes (WLD-47), map reveal points of places (rule 22 `◇ 공개 전 장소 · 전환 문구`) |
| 인물 (cast; rules 13, 20, 인물 항목) | Characters, the shared field-definition mechanism and field types, `◆` value semantics and the two edit buttons `이 값 고치기 · 여기서부터 바뀜`, relationship storage shape (also used later for faction-to-faction), delete impact sheet and merge sheet shells | Character `출생` (날짜) and `알 수 없음` state for ages (WLD-35); `연결` fields pointing to 세력 · 장소 · 용어 (shown on WLD cards as "이 항목으로 잇는 인물") |
| 시점 렌즈 (lens) | Lens state, lens chip, `최신 · 1-7` label, `예정` rows, past-lens edit banner | WLD screens with a lens chip: 세계 lists and cards, 지도, 연표, 이곳의 시간, 인물 동선 |
| 점검 센터 (check center) | Check list shell, filters, `전체` count, `맞음` / `맞다고 표시한 것` storage and undo, `지운 항목` rows, `이동` rows | WLD-28 defines the term rows (kinds, copy, buttons, ordering); `맞음` store must key by `linkId` |
| 저장 · 휴지통 · 내보내기 | App DB, `.civilight` packaging, Drive backup, trash screen and filter `세계`, export sheet, image hashing | WLD section 3.3 defines the world payload |
| 숨기기 · 등급 (rule 19) | Hidden flags, biometric unlock, re-lock timing, `LockRow` | WLD-13 defines how hidden terms/places/factions show inside the world area and on maps |
| 이미지 (카드 보기 · 이미지) | Preset images (8), crop editor, holo option, image picker | Place/term/faction slot ratio 16:9, holo selectable |
| 캔버스 | Canvas nodes `세력 · 사건 · 용어` (project items placed as-is) | Reads Faction, TimelineEvent, Term by key |
| 명령 칸 · 검색 | Search results (`용어` rows such as `오리지늄 등불 · 첫 공개 1-7 #044`), 초성 matching, commands | Search over term names and aliases, place and faction names, event titles; placeholders per screen are defined here |
| 온보딩 | Tours `세계 안내 1/3` (`공개 시점 · 앞에서 쓰이면 ▲ 점검 센터`), `지도 안내 1/3` (`도구 줄 · 이동 · 핀 · 영역 · 경로 · 라벨 · 레이어 · 고른 것만 동작`), `장소와 때 안내`, help replay rows `세계 · 연표`, `지도 만들기`, `인물 동선 · 장소와 때` | Anchors for the tour highlights: list section header, map tool bar |
| 화면 · 접기 · 모션 | Window classes, fold state save/restore, global motion tokens, reduce motion/transparency | WLD-68 gives map/card motion specifics; WLD-37 gives fold behaviour of maps |
| 설정 · 프로젝트 설정 | Setting screens | WLD-29, WLD-47, WLD-67 define the world rows and their effects |

**Out of scope (sources say later or never).** City and building generation (`도시 · 건물 생성 · 나중`, v1 uses node maps). Worlds whose day is not 24 hours (rule 18: `하루 길이가 다른 세계는 나중에`). Pen / sketch layer (`만들지 않음`). Freehand drawing on maps (`숨은 제스처나 손으로 그리기는 없다`). Automatic place names (`지명은 자동으로 만들지 않는다`). Morphological analysis or similarity guessing for terms (rule 21 §3 rejected). Server or cloud term index (rule 21 §7 rejected).

---

## 2. Features

Each feature: priority (P0 core · P1 planned for v1 · P2 later), `needsExternal` (network service, cloud, ML model, or OS integration beyond a normal app), behaviour, copy, sources. The scope table of the planning doc puts every module in v1, so P2 is used only where a source says later.

### A. 세계 모듈 · 목록

#### WLD-01 · 세계 화면 틀 — P0 · needsExternal: no (0.2.0)

- Entry: project home shortcut `세계` (home row `스토리 · 인물 · 세계 · 연표 · 캔버스 · 설정`), the command bar, links from cards. The timeline is a separate module entered from `연표` (WLD-31).
- Header (glass bar): small line `잿빛 새벽의 등불` (project name), title `세계`, right: lens chip (e.g. `최신 · 1-7`; `acc` outline when not 최신) and `+` button. `+` opens the create sheet for the current tab (WLD-11; on `지도` it opens `새 지도`, WLD-36).
  - **Superseded (0.2.0):** the `+` button is removed. Each tab's list starts with its create row: `＋ 새 장소 · ＋ 새 용어 · ＋ 새 세력` (WLD-70), and on `지도`, `＋ 새 지도` (WLD-75). The lens chip is the small chip of WLD-71, and the project opens at `최신`.
- Segmented tabs under the header: `장소 · 용어 · 세력 · 지도`. The chosen tab is remembered per project `[PROPOSED]`.
- Hint line under tabs on the `용어` tab: `공개 시점 전 사용 → 점검 센터`. Tapping it opens the check center filtered to `용어 공개` `[PROPOSED]`.
- Command bar at the bottom with placeholder `장소 · 용어 · 세력 찾기` (search area owns results). On a node map the placeholder is `장소 · 노드 찾기`. **(0.2.0)** At most one command bar per frame (U-01, DS-65), with no mic (S-8).
- Lens: list sections and card values follow the lens (rule 11-4: lens chip only on screens whose content changes with it; the world screens are on that list).
- Inner layouts (list modules rule): A `목록 + 상세` (default; list pane 390 | detail pane 409; the selected item opens on the right, e.g. term list | term card) and B `가운데 한 칸` (single 560 reading column). Chosen in `설정 › 화면 › 펼친 화면 배치` (owned by settings). On unfold the selected item opens in the right pane; on fold the last-touched pane stays on the cover.
- `ViewToggle` `목록 | 카드` in the header right for the place, term and faction lists (WLD-05).
- Sources: planning 세계 · 연표 · 지도, 프로젝트 홈; rule 11 §3; boards `World`, `TwoWorld`, `ColWorld`, `TourWorld`, `Home`; DS `ViewToggle`, `LensChip`.

#### WLD-02 · 용어 목록 — P0 · needsExternal: no (0.2.0)

- Two sections, in this order:
  1. `공개됨 · <lens label>` (e.g. `공개됨 · 최신 · 1-7`): terms whose `첫 공개` position is at or before the lens point (at 최신: at or before the 집필 선).
  2. `아직 공개 전`: terms whose `첫 공개` is after the lens point, including point-anchored reveals beyond the 집필 선 (`예정`).
- Row (`ListRow .li`): title = term name (Myeongjo 16), fragment = first line of the description (e.g. `꺼지지 않는 빛을 내는 광석 등불`), right column (small, right-aligned):
  - line 1: `첫 공개 1-7` (episode of the reveal) for revealed terms; `공개 예정 2-5` or `공개 예정 2장` for point reveals beyond the 집필 선. If pre-reveal candidates exist, append ` · ◇ 후보 1` (count of `◇ 공개 전 후보`; rule 21 §8).
  - line 2 (only if ≥ 1): `▲ 앞에서 N회 사용` in `warn`, N = number of pre-reveal links (rule 21 §2 fixes this as the same number as the check badge `공개 전 사용 N`); links marked `맞음` are excluded `[PROPOSED, Q-07]`.
- Sort inside a section: manuscript order of `첫 공개` (earliest first); `아직 공개 전` by reveal point `[PROPOSED]`.
- Terms without any `첫 공개`: listed in `아직 공개 전` with right label `첫 공개 없음` `[PROPOSED]` (Q-08).
- Hidden terms while locked: one `LockRow` `숨긴 용어 N` with `지문으로 보기` (WLD-13).
- Tap row → term card (WLD-08); inner layout A shows it in the right pane.
- Empty state (no terms) `[PROPOSED]`: section headers hidden; one line `용어 없음` and primary button `용어 만들기` (opens WLD-11). Copy follows the empty-state pattern of `EmptyCast`.
- **(0.2.0)**
  - The first row is `＋ 새 용어` (WLD-70), present with 0 terms and with many. **Superseded:** the empty-state button `용어 만들기` (the empty line `용어 없음` stays under the create row).
  - `첫 공개` means the **effective** reveal of WLD-72: the hand-set record, else the first `@` link in manuscript order. A term with no hand-set record and no manuscript link is `첫 공개 없음` in `아직 공개 전` (Q-08 default kept).
  - ` · ◇ 후보 N` is appended only while the project's `용어 후보` setting is on (WLD-73).
  - The candidate list of S-5 is the last section of this tab (WLD-73).
- Sample (lens 최신 · 1-7): 공개됨 → 오리지늄 등불 (`첫 공개 1-7 · ◇ 후보 1`, `▲ 앞에서 1회 사용`), 대정전 (`첫 공개 1-4`), 등불 결사 (`첫 공개 1-1`); 아직 공개 전 → 잿불의 서약 (`공개 예정 2-5`), 상층 송전망 (`공개 예정 2장`).
- Sources: planning 용어 공개 시점; rule 21 §2, §8; rule 11 §1; boards `World`, `TwoWorld`, `ColWorld`, `TourWorld`.

#### WLD-03 · 장소 목록 — P0 · needsExternal: no (0.2.0)

- Not drawn. `[PROPOSED]` minimal list by analogy with the term list: rows title = place name, fragment = first line of description, right = `등장 1-2 · 1-7 외 4` (same derived value as the place card), grouped by `지역` (parent place, WLD-09) with an ungrouped section first. Hidden places while locked collapse into `숨긴 장소 N` (WLD-13).
- Tap row → place card (WLD-09). Long lists must work with 100+ entries (virtualized list).
- **(0.2.0)** The first row is `＋ 새 장소` (WLD-70).
- Sources: planning 세계 모듈 (장소 카드); rule 19 §6 (place hiding); boards `World` (tab exists); Q-09.

#### WLD-04 · 세력 목록 — P0 · needsExternal: no (0.2.0)

- Not drawn. `[PROPOSED]` rows: title = faction name, fragment = first line of description, right = number of characters whose `연결` field points to the faction (e.g. `인물 4`).
- Hidden factions while locked collapse into `숨긴 세력 N` (WLD-13).
- **(0.2.0)** The first row is `＋ 새 세력` (WLD-70).
- Sources: planning 세계 모듈; 인물 항목 §2 (`연결` · 상대 카드에 ‘이 항목으로 잇는 인물’); rule 19; Q-09.

#### WLD-05 · 목록 | 카드 보기 — P1 · needsExternal: no (0.2.0: unchanged by U-16)

- Header `ViewToggle` `목록 | 카드` on place, term and faction lists; the chosen view is remembered per screen.
- Card view: horizontal rows of cards with the item image in its fixed 16:9 crop (preset image or initial-letter card when none), name and the right-column label of the list row. One row per list section (용어: `공개됨` row and `아직 공개 전` row) `[PROPOSED]`.
- Motion: thumbnails grow into cards in place (decelerate 360 ms), list text exits first (accelerate 160 ms); carousel snaps to the nearest card (320 ms) with 8 % image parallax and a haptic tick; reduce motion → 120 ms fade, no parallax.
- **(0.2.0)** Unchanged. U-16's 3D card views are for the character list only (D2, CST-67). The board `scr_u16_cards_b.py` draws 타로 펼침 with world cards, but 0.2.0 does not build it here. In card view the `＋ 새 <kind>` row of WLD-70 stays above the card rows.
- Sources: planning 카드 보기 · 이미지; DS `ViewToggle`, `ImageTile`; boards `MCardView`, `MCarousel`, `ImageSlots`.

#### WLD-70 · 그 자리에서 만들기 · 장소 · 용어 · 세력 (create in place) — P0 (0.2.0, U-15)
Replaces WLD-11's `+` sheet. The overlay is DS-72's shared create card (D17): shape, focus, keyboard, buttons, highlight, dismiss and motion are defined there. This feature gives the world field sets and rows. `＋` before a label stands for the icon `plus`; the visible text is the label (CST conventions, DS-72).
- **List rows (U-15.1).** The first row of each tab's list, under the tabs and the `용어` hint line and above the first section and any `LockRow`, is icon `plus` + `새 장소` / `새 용어` / `새 세력` (ICON class b; accessibility name the same text). It is present with 0 items and with many.
- **Card title** is the row text (`새 장소` …). The screen stays on the list (U-15.2).
- **Field sets (U-15.7).** Only `이름` is required (trimmed, NFC).

| Kind | Fields, in order | Stored as |
| --- | --- | --- |
| 장소 | `이미지 · 이름 · 상위 지역 · 한 줄 설명` | `image` (16:9 slot, WLD-07) · `name` · `parentPlaceKey` (a live place picked from the place list picker; the field's own picker has no create row, so no nested card) · `description` (one line) |
| 용어 | `이름 · 다른 이름 · 한 줄 설명` | `name` · `aliases` (`다른 이름` is the card's word for `별칭`. It is one optional alias, validated as in WLD-16, and the text is never split. More aliases are added on the card with WLD-16's list editor) · `description` |
| 세력 | `이미지 · 이름 · 한 줄 설명` | `image` · `name` · `description` |

- **Buttons (U-15.6):** `취소 · 만들고 열기 · 만들기`, disabled while `이름` is blank.
  - `만들기` closes the card, stays on the list and briefly highlights the new row (through `CivilightMotion`). The list scrolls only if the row is out of view.
  - `만들고 열기` opens the new card (cover: full screen; inner layout A: right pane).
  - `취소` and back write nothing. A tap on the dimmed back closes the card only while every field is empty (DS-72).
- **Write.** One edit, one undo step, through `domain:world` `WorldItems.createTerm` / `createPlace` / `createFaction`. They gain the additive arguments `image`, `aliases` (terms) and `parentPlaceKey` (places already have it).
  - A new term or alias triggers the WLD-26 rescan of that term, if `용어 후보` is on.
  - Same-name items are allowed (WLD-11 rule kept).
- **Pickers (U-15.3).** Every sheet that picks one of these kinds has the same top row:
  - the place picker of WLD-52 (scene `장소와 때`, bulk `장소 정하기`, event place). Its row `새 장소 · 지도에 핀을 꽂으며 만들기` is **superseded** by `＋ 새 장소`; a pin for the new place is placed later with the map `핀` tool;
  - the `@` link picker (terms, places, factions; script area);
  - the term card's `관련` picker;
  - the `연결` field picker for 세력 · 장소 · 용어 (cast field engine).
  - The row label is `새 장소`. When the query matches nothing it is `‘<query>’ 새 장소` (likewise `새 용어`, `새 세력`; quote style DS-72), and the query fills `이름`.
  - In a picker the card shows `취소 · 만들기`, and the created item is returned as the pick.
  - The `@` picker, which mixes kinds, shows one create row, for the kind in view (SCR-55, the owner).
  - The merge target picker (WLD-15) has no create row.
- **Read-only (U-15.10).** Every create row and the empty-state button are hidden in a read-only project (the example). A hidden project, once unlocked, shows them.
- Other creation paths of WLD-11 are unchanged: the map `핀` tool's `새 장소`, and the recorder (creates nothing). Hex `장소 만들기` and stamp `장소 연결` are unreachable in 0.2.0 (WLD-75).
- Sources: `u15-create-in-place.md` rows 1–3, 6–8, 10 (planning); scope U-15.1 … U-15.3, U-15.6 … U-15.8, U-15.10, D17; `icon-buttons.md` row 60 (planning).

#### WLD-71 · 시점 렌즈 칩 · 세계 머리 (lens chip on world heads) — P0 (0.2.0, S-1)
- The small lens chip (CST-70) is shown only on these world heads:
  - the `세계` list head (all four tabs);
  - the term, place and faction card heads, including the place card's `이곳의 시간` tab.
- A project opens at `최신` (D9). A point picked here is the same lens state as on the cast heads and lasts until the project is closed.
- **Superseded (0.2.0)**, with the chip removed from these heads (each reads at `최신`):
  - 연표 (WLD-31), 연표 + 지도 (WLD-64);
  - map view and map edit (WLD-37, WLD-40);
  - node map (WLD-50), detailed terrain edit (WLD-60), 인물 동선 (WLD-62).
  - The `시점별 공개` view (WLD-49) keeps its own point scrubber; it is a view of reveal points, not the lens.
- WLD-12 is otherwise unchanged: cards and lists read their values at the head's lens.
- Sources: decisions row `단순화 · 시점 렌즈` (planning); scope S-1, D9.

### B. 카드

#### WLD-06 · 카드 공통 · 고정 칸과 프로젝트 항목 — P0 · needsExternal: no (0.2.0)

- Places, terms and factions use the same project-field mechanism as characters (인물 항목 rule 5, confirmed): `프로젝트 설정 › 인물 항목` has tabs `인물 · 장소 · 용어 · 세력`; each tab lists `고정 항목` (greyed, not deletable) and `프로젝트 항목 · N` with drag handles (`끌어서 순서`), `+ 항목` sheet (`항목 넣기`: 이름 → 종류 nine radios `짧은 글 · 긴 글 · 숫자 · 고르기 · 여러 개 고르기 · 날짜 · 기념일 · 연결 · 계산` → per-type options → `보이는 곳 · 머리 · 목록 · 필터` → `비밀 항목` toggle → `넣기`). The field engine (types, conversion rules, `알 수 없음(??)`, `빈 항목 N · 보기`, rename keeps values, `+ 항목` on a card with `모든 … 에 / 이 … 만`) is owned by the cast area and reused unchanged.
- Fixed fields per type (from the sources; anything else is a project field):
  - 용어: 이름 · 설명 · 참고 이미지 · 별칭 · 첫 공개 · 관련 · 글자 찾기 (on/off). `쓰인 곳` is derived.
  - 장소: 이름 · 설명 · 이미지 · 지역 (parent place). `등장`, `배경 에셋`, `이곳의 시간` are derived. **(0.2.0)** `배경 에셋` is shown only with `게임 제작 도구` on (WLD-76). The create card (WLD-70) calls 지역 `상위 지역` and the term's 별칭 `다른 이름`, as U-15 decision 7 words them; they are the same fields.
  - 세력: 이름 · 설명 · 이미지 `[PROPOSED]` (no source lists faction fixed fields; Q-10).
- Examples of project fields the sources give: 장소 `기후 · 인구`, `상태` (with ◆, e.g. 하층 구역 · 상태 `빛 있음 → 대정전 이후`); 세력 `수장 · 규모`.
- Every non-derived field may carry `◆` history (rule 11-2 two buttons `이 값 고치기 · 여기서부터 바뀜`; past-lens edit banner), engine owned by cast/lens areas.
- Empty fields are hidden in view; one line `빈 항목 N · 보기`; editing shows all as inputs.
- Sources: 인물 항목 §1–§7 and decision 5; memory character-fields; boards `RCFields`, `RCFieldAdd`, `TwoTimeline` (place field change), `ProjSettings` (`인물 항목 · … · 장소 · 용어 · 세력도`).

#### WLD-07 · 카드 이미지 — P1 · needsExternal: no

- Slot `장소 · 용어 · 세력`, fixed ratio 16:9, label `참고 이미지` on the term card. Picker: 8 preset images (`잿빛 새벽 · 등불 · 심해 · 안개 숲 · 사막 유적 · 성운 · 붉은 성채 · 설원`) or gallery/file; crop with fixed ratio, zoom, 90° rotate, mirror, `처음대로`; original kept; re-crop any time. Holo option per image (`홀로그램 효과 · 은빛 · 기울이면 흐름`, default from `설정 › 지도 · 이미지 · 연표 › 홀로그램 효과 기본`). No image → initial-letter card + `이미지 고르기`.
- Engine owned by the image area; this area only hosts the slot.
- Sources: planning 카드 보기 · 이미지 (table row `장소 · 용어 · 세력 · 16:9 · 선택`); boards `ImageSlots`, `ImagePick`, `ImageCrop`, `SettingsMore`.

#### WLD-08 · 용어 카드 — P0 · needsExternal: no (0.2.0)

- Header: small `세계 · 용어`, title = term name, lens chip. Command bar placeholder `명령 또는 검색`.
- Body, top to bottom:
  1. `참고 이미지` (16:9; WLD-07).
  2. Eyebrow `용어`, name (Myeongjo 800 30/38), description (Myeongjo body, may contain `@` links).
  3. Field table (`.row`): `첫 공개 | 1-7 씬 3 · #044` (or `공개 예정 2-5`, or `첫 공개 없음`); `별칭 | 오리지늄 · 빛돌` (WLD-16; after an alias change the value line shows `후보 N 새로 찾음` in `acc` until the card is left); `관련 | 등불 결사 · 대정전` (links to terms/places/factions); then project fields.
  4. `쓰인 곳` section (WLD-19): header `쓰인 곳 · 2 · 후보 1` (the `· 후보 N` part only when N ≥ 1); groups `쓰인 곳 · N`, `후보 · N · 확인 안 함` with right action `모두 잇기 · 모두 아님`, `원고 밖 · 메모 · N`. Each row: location (mono small, e.g. `1-4 씬 2`) + the whole sentence in Myeongjo with the linked or matched span boxed inline (`SearchHit`, never split onto its own line). Marker lines under a row: `▲ 공개(1-7) 전 사용` (warn), `◇ 첫 공개`, `◇ 후보 · 끊김 · 잇기 · 아님`, tag `선택지`, tag `게임 텍스트`, location label for out-of-manuscript rows (`인물 · 세라`, `페이지 · 하층 구역의 빛`).
- Tap a row → opens the manuscript at that block (or the page/card) `[PROPOSED]` (same as the check center's `원고로 가기`).
- Special states: 1-syllable name and aliases → under the header `1음절 · 글자 찾기 안 함` (only links counted); `글자 찾기` off → `글자 찾기 꺼짐 · 켜기`; more than 10 `아님` → chip `글자 찾기 끄기` (WLD-25); no reveal → `첫 공개 없음 · 쓰인 곳 3` plus chip `1-2 씬 1을 첫 공개로` (WLD-18).
- **(0.2.0)**
  - The `첫 공개` row shows the effective reveal (WLD-72). **Superseded:** the `첫 공개 없음 · 쓰인 곳 3` state and its chip `… 첫 공개로`. With automatic reveal, a term that has manuscript links always has a reveal.
  - The `· 후보 N` part of the header, the `후보` group, the `글자 찾기` states and chip, and `⋯ › 아님으로 둔 것` are shown only while the project's `용어 후보` setting is on (WLD-73).
  - Rows tagged `게임 텍스트` are shown and counted only with `게임 제작 도구` on (WLD-76).
- Lens: when the lens is in the past (e.g. 1-5), rows located after the lens point are dimmed; numbers do not change (rule 21 §5).
- `⋯` menu: `아님으로 둔 것` (list of rejections with `되돌리기` each), `지우기`, `합치기` (rule 20 §7), `숨기기` (WLD-13) `[PROPOSED order]`.
- Edit: tapping a field row edits it (fixed fields inline; project fields through the field engine).
- Sources: planning 용어 공개 시점 (카드 항목: 설명, 참고 이미지, 관련 항목, 쓰인 곳); rule 21 §2, §3, §5, §6, §8; rule 20 §7 (⋯ › 지우기 · 합치기); boards `Term` (2026-11-22), `TwoWorld`, `RTLinkAll`; Q-01 (groups vs single list).

#### WLD-09 · 장소 카드 — P0 · needsExternal: no (0.2.0)

- Two shapes of the same element (fold rule: `장소 시트 ↔ 장소 카드`, same element changes shape, keeps tab and scroll):
  - Summary on the map: cover = bottom sheet with eyebrow `장소 · 하층 구역`, name + `닫기`, description, rows `등장 | 1-2 · 1-7 외 4`, `배경 에셋 | 2`, buttons `핀 옮기기 · 장소 카드 열기`. Inner = side card (270 wide, top right over the map) with eyebrow `장소`, name, description, rows `등장`, `지역 | 하층 구역`, `배경 에셋 | 2`, buttons `장소 카드 · 핀 옮기기`.
  - Full card: header small `세계 · 장소`, lens chip; top 16:9 area shows the map around the place's pin with the pin highlighted (`PlaceTime`), eyebrow `장소 · <지역>`, name (Myeongjo 800 30/38); tabs `개요 · 이곳의 시간 · 지도`.
    - `개요`: description, rows `등장`, `지역`, `배경 에셋`, project fields, image.
    - `이곳의 시간`: WLD-63.
    - `지도`: the maps containing pins of this place, each with a thumbnail; tap → map view centred on the pin `[PROPOSED]`.
- Derived rows: `등장` = episodes whose scenes use this place (or a region that is this place's region `[PROPOSED]`), shown as first two episodes + `외 N` (e.g. `1-2 · 1-7 외 4`); `배경 에셋` = count of background assets linked to the place (asset → place link owned by the script asset list).
- `지역` (parent place): authored; prefilled from map nesting when the place is created by a pin on a sub-map whose parent pin belongs to place P `[PROPOSED]`. Label conflict `소속` vs `지역` → Q-03.
- `핀 옮기기`: WLD-41.
- **(0.2.0)** The `배경 에셋` row (sheet, side card, card) is shown only with `게임 제작 도구` on (WLD-76). The card's `지도` tab lists every map holding a pin of the place, including 0.1.0 node, generated and sketch maps, which open for viewing (WLD-75). `핀 옮기기` is offered only on image maps.
- Sources: planning 지도 (핀 → 장소 카드 설명 · 등장 에피소드 · 소속 · 배경 에셋), 접기 · 펼치기; boards `Map`, `MapC`, `MapSoft`, `MapSoftC`, `PlaceTime`, `MFoldMap`.

#### WLD-10 · 세력 카드 — P0 · needsExternal: no

- Not drawn. `[PROPOSED]` same layout as the term card without reveal/usage: image, eyebrow `세력`, name, description, project fields (e.g. `수장 · 규모`), then derived sections:
  - characters whose `연결` field points here (인물 항목 §2: `이 항목으로 잇는 인물`), each row = character name + field name (e.g. `리엔 · 소속`), following the lens;
  - map regions and polygon provinces linked to this faction (`연결 세력`), each with map name; tap → map centred on it;
  - timeline events at which a ◆ of this faction changes.
- Faction-to-faction relationships: WLD-69 (P2).
- Sources: planning 세계 모듈 (세력 카드); 인물 항목 §2, decision 5; rule 13 memory (faction relations later); boards `MapEdit` (`연결 | 세력 · 상층 경비대`), `MapPoly` (`연결 세력`), `TimelineMap`; Q-10.

#### WLD-11 · 항목 만들기 · 이름 바꾸기 — P0 · needsExternal: no (0.2.0: `+` sheet superseded by WLD-70)

- `+` in the world header opens a sheet for the current tab `[PROPOSED]`: `이름` input (required, NFC-normalised, trimmed), `설명` (optional), primary button `만들기`; the new card opens.
  - **Superseded (0.2.0):** this sheet is replaced by the create rows and card of WLD-70.
- Other creation paths from the sources:
  - Map `핀` tool → tap the map → pick an existing place or create a new one by name (WLD-41).
  - Hex cell `장소 만들기` (WLD-58); stamp `장소 연결` (WLD-60) may create a place.
  - Place picker row `새 장소 · 지도에 핀을 꽂으며 만들기` (WLD-52), used by scene `장소와 때` and bulk entry; suggestions that name an unknown place are flagged `새 장소` by the bulk area and create through this path only when the author confirms.
  - Life recorder `카드에 넣기` creates nothing (recorder area).
- Name collision: allowed; the merge rule shows `◇ 같은 이름 2 · <이름>` with a merge button only when names are identical (rule 20 §5–§6).
- Renaming a term or alias triggers a candidate rescan of that term (WLD-26). Renaming never rewrites manuscript text: link runs keep their written text (rule 21 §2).
- Sources: rule 20 §6 (`같은 이름 2`), rule 21 §2–§3, §7; boards `MapHex` (`장소 만들기`), `BulkPlace` (`새 장소`), `MapDetail` (`장소 연결`).

#### WLD-12 · 시점 렌즈와 세계 값의 변화 — P0 · needsExternal: no (0.2.0)

- All world cards, lists, maps and the timeline read values at the lens point (rule 11: 최신 = up to 집필 선; past lens = state after that unit ends).
- Values with `◆` history show `◆` on the row; editing at 최신 or a past lens offers `이 값 고치기 · 여기서부터 바뀜` (engine owned by lens/cast).
- What follows the lens in this area: term reveal sections (WLD-02), dimmed usage rows (WLD-08), map item visibility (WLD-49), region shapes (WLD-65), place-time rows (WLD-63), track position (WLD-62), timeline rows (WLD-31).
- **(0.2.0, S-1)** Only screens with the chip follow a picked point (WLD-71): the world lists and cards, including `이곳의 시간`. Map view, 연표 and 연표 + 지도 read at `최신`. Track position is unreachable (WLD-75).
- Sources: rule 11 §1–§4; planning 공통 개념 (시점 렌즈); boards `Lens`, `RTEditTwo`, `RTPlanned`.

#### WLD-13 · 숨기기 (용어 · 장소 · 세력) — P1 · needsExternal: no

- Item hiding covers terms, places and factions (rule 19 extended hiding to 장소 · 세력; confirmed 2026-11-22). Unlock = system biometric (fallback device PIN); re-lock timing shared with project hiding.
- While locked:
  - Lists: hidden items collapse into one `LockRow` `숨긴 용어 N` / `숨긴 장소 N` / `숨긴 세력 N` with `지문으로 보기` (`[PROPOSED]` labels by analogy with `숨긴 인물 1 · 지문으로 보기`).
  - Linked mentions (`@`) render `숨긴 항목`; plain manuscript text is never altered.
  - Terms: candidates are searched only while unlocked; while locked no candidate counts are shown (21 draft §3). `▲` and `◇` rows involving a hidden term are computed but shown in the check center as one locked line `▲ 숨긴 항목 관련 N · 지문으로 보기`.
  - Maps: pins and regions of hidden places/factions are not drawn (rule 19 §6 edge row). Routes ending at a hidden place `[PROPOSED]` are drawn up to the last visible point.
  - `인물 동선`: segments through hidden scenes are dotted `숨긴 씬`.
  - Search: card results excluded, ends with `숨긴 항목 제외`.
  - `@` picker: hidden items are not offered; only `숨긴 … 1 · 지문으로 보기` opens them.
- Hidden + deleted: hiding wins (rule 20 edge row): trash row `숨긴 장소 · 지문으로 보기` `[PROPOSED label]`.
- Sources: rule 19 §1 decision 5, §6 tables; rule 21 §3 (숨긴 용어); rule 20 edge rows; memory rules-19-23; board `ItemHide`.

#### WLD-14 · 지우기 · 되살리기 · 영구 삭제 (세계 항목) — P1 · needsExternal: no

- Entry: card `⋯ › 지우기`. Shell (impact sheet, toast, trash, restore, permanent delete, check center `지운 항목` filter) is shared with the cast area; this feature defines world-specific content.
- Impact sheet (shown if any reference or owned thing exists; otherwise no sheet, toast only). Sheet layout and its fixed copy come from the cast area's shell (board `RCDeleteImpact`). World-specific content this area supplies: owned things that go to trash with the item (field values, the item's `◆`, image; places: pins and nodes; terms: the `◇ 첫 공개`); reference counts by kind (scene places, `@` links, character `연결` values, region / stamp / asset links, routes touching pins, sub-maps); for a term, the `▲` that become held, e.g. `▲ 공개 전 사용 1` (rule 20 §4).
- References that stay in place (dotted + `◇`; wide places say `지운 장소 · 잿더미 시장`, `지운 용어 · 오리지늄 등불`, `지운 세력 · 상층 경비대`):
  - `@` links (manuscript, pages, recorder, memos): dotted chip; actions `다른 항목에 잇기 · 글로 풀기 · 되살리기 · 이대로 두기`.
  - Term usage and `▲`: text untouched, linked span dotted; `▲` kept as `▲ N 보류` inside the item's `◇` check line; restored to their places on restore; deleted together with the term on permanent delete.
  - Scene `장소 · 지역`: `지운 장소 · <이름>` `◇`; excluded from `인물 동선` and the travel check (same handling as `◇ 점검 못 함`); never checked with the trashed pin's coordinates. Actions `장소 고르기 · 되살리기 · 이대로 두기`.
  - Map pins/nodes of a deleted place go to trash with it; routes touching that pin remain with a dotted end; actions `다른 핀에 잇기 · 경로 지우기 · 되살리기 · 이대로 두기`.
  - Regions, stamps and background assets linked to a deleted place or faction: shape and art stay on the map, only the link field turns dotted `◇`; actions `다른 항목 고르기 · 비우기 · 되살리기 · 이대로 두기`.
  - Sub-map attached to a deleted place's pin: map stays; breadcrumb shows `지운 장소 · 잿더미 시장`; actions `다른 핀에 달기 · 되살리기 · 이대로 두기`; after permanent delete the sub-map becomes a top-level map.
  - Character `연결` values pointing to a deleted faction/place/term: `지운 세력 · 상층 경비대` dotted `◇`; actions `다른 항목 고르기 · 비우기 · 되살리기 · 이대로 두기`. Term `관련` likewise.
  - Faction-to-faction relations go to trash with the faction.
- Toast after delete: `<종류> <이름> 지움 · 참조 N · 점검 센터 · 되돌리기` (e.g. `장소 잿더미 시장 지움 · 참조 9 · 점검 센터 · 되돌리기` `[PROPOSED example]`).
- Restore (trash filter `세계`): item key unchanged, so every reference re-attaches; references the author changed meanwhile stay changed; toast lists counts. Pins return at the same 0–1 coordinates; if the pin's map was deleted meanwhile, the place returns and the pin shows `◇ 지도 없음` with `지도 되살리기 · 핀 다시 놓기` `[PROPOSED button copy]`.
- After 30 days or `영구 삭제`: a name tag (이름 · 종류) remains; references show `영구 삭제 · <이름>`; `되살리기` disappears from the actions.
- Lens: deleted items are absent from lists, maps, timeline and canvas at every lens point; dotted references show regardless of lens.
- Sources: rule 20 §2–§5 tables, §7; memory rules-19-23; boards `RCDeleteImpact` (shell), `Trash`, `Check`.

#### WLD-15 · 합치기 (세계 항목) — P1 · needsExternal: no

- Entry: card `⋯ › 합치기`, the impact sheet's `다른 <종류>와 합치기 ›`, and `◇ 같은 이름 2` rows. Pick target with the list picker of that type. Shell (comparison sheet `<A> → <B> · 기본은 <B> 값`, `옮길 참조 · N`, `다른 칸 · N`, `빈 칸 채움`, `취소 · 합치기`, toast `<A> → <B> 합침 · 참조 N · 되돌리기`, merged side to trash as `합침 → <B>`) is shared with the cast area.
- World-specific rows:
  - Places with two pins on the same map: the target's pin stays; the merged place's pin goes to trash with it; route ends attached to the merged pin move to the target pin; the sheet shows the count `[PROPOSED copy: 경로 끝 N · <B> 핀으로]`.
  - Terms with two `첫 공개`: comparison row, default target; after merge `▲` is recomputed. Aliases: union `[PROPOSED]` (names of the merged term become aliases of the target `[PROPOSED]`; Q-11). Candidates rescan.
  - Factions: `연결` values pointing to the merged faction move to the target; regions' `연결 세력` move; faction relations follow the cast relationship merge rule.
- Restoring the merged item from trash: it returns with its own original field values; moved references stay with the target.
- Sources: rule 20 §6; board `RCMerge` (shell).

### C. 용어 공개와 쓰인 곳 (rule 21)

Rule 21 in one paragraph: only `@` links are usage (fact). Text matches are `후보`, shown separately and never counted until the author presses `잇기`, which wraps exactly the matched span in a link. The app never creates links by itself. Matching uses the name and registered aliases only, at word start, followed by nothing, punctuation or a fixed particle list; cut-off words (2+ syllables + `…`) are `끊김` candidates; 1-syllable names are not searched; the longer name wins on overlap. Only game-output text yields `▲` and candidates. Comparison is by manuscript order, the reveal block itself counts as after. `아님` and `맞음` are author records stored in the project file; the candidate list is derived and never stored.

#### WLD-16 · 별칭 — P1 · needsExternal: no

- Fixed field `별칭` on the term card: zero or more strings, shown joined by ` · ` (e.g. `오리지늄 · 빛돌`). Editing: tap the row → list editor with `+` and per-alias remove `[PROPOSED]`.
- Each alias is NFC-normalised, trimmed, must be non-empty, must differ from the name and from the term's other aliases (case-insensitive for Latin) `[PROPOSED validation]`.
- Adding or changing an alias rebuilds the matcher and rescans the whole manuscript for that term only (WLD-26); the card shows next to the row `후보 N 새로 찾음` (N = candidates of this term that did not exist before). Existing text is never converted to links.
- `@` picker: aliases are searchable; an alias hit row reads `<alias> · <name>의 별칭` (e.g. `오리지늄 · 오리지늄 등불의 별칭`); choosing it inserts the alias text as the link text.
- Sources: rule 21 §2, §3 (별칭), §7, §8 (@ 고르기 창); board `RTLinkAll` (`별칭 | 오리지늄 · 빛돌 | 후보 2 새로 찾음`), `Term`.

#### WLD-17 · `@` 연결 (용어 대상) — P0 · needsExternal: no

- A term link = a text run of kind `link` with `{linkId, targetKind: TERM, targetKey, text}`. Storage and rendering are SCR's; this feature defines the term semantics.
- The run's text is what the author wrote (name, alias, or any span wrapped by `잇기`, e.g. `오리지`). Particles stay outside the run: `[오리지늄]은`.
- Links may exist in: dialogue line text, narration, system text, choice option lines, author memos, direction blocks, parenthetical runs (`괄호 지문`), pages, character profiles (incl. game text), place/term/faction card text fields, recorder records, and — pending Q-05 — direct `전환 문구`.
- A link's identity (`linkId`) survives edits of surrounding text, block moves and cut/paste-as-move; copy/paste creates new `linkId`s `[PROPOSED]` (rule 12 treats copy as new content).
- Unlinking (removing the link, text stays) deletes any `맞음` on that link.
- Sources: rule 21 §2, §4, §6; file format §4 (`@리엔은 연결 조각(키)`); planning 공통 개념 (@ 링크).

#### WLD-18 · 첫 공개 기록 — P0 · needsExternal: no (0.2.0: now the hand-set reveal; see WLD-72)

- **(0.2.0)** Everything below now describes the **hand-set** reveal: a stored `◇ 첫 공개` record, from 0.1.0 or set in 0.2.0 with `공개 시점 옮기기`. When a term has no stored record, its reveal is the automatic one of WLD-72, which is derived and never stored.
  - **Superseded:** "the app never infers it from the earliest link" and the suggestion chip `<first link location>을 첫 공개로`.
  - A stored record always wins over the automatic one (D12).
- One `◇ 첫 공개` record per term at most. It is an author record; the app never infers it from the earliest link.
- Anchor kinds (rule 12 §1 `자리`):
  - `BLOCK(blockKey)` — "from this block, inclusive". Displayed `1-7 씬 3 · #044`. Created from a manuscript block (SCR's block panel) or from the term card.
  - `POINT(unitKey)` — "from the start of that chapter / episode / scene". Used for `공개 예정 2-5`, `공개 예정 2장`. The manuscript panel shows a line above the unit's first block `◇ 2-5부터 · 근거 블록 없음` with `블록에 잇기` `[PROPOSED, by analogy with rule 12's ◆ 1-5부터 · 근거 블록 없음]`.
- Not allowed on a choice option line (`#045-1`): the option is disabled with the reason `선택지 보기 줄에는 첫 공개를 걸 수 없음` `[PROPOSED copy]`. Whether the whole choice block may carry it → Q-06.
- Setting / moving from the term card: tap the `첫 공개` row → sheet `공개 시점 옮기기` `[PROPOSED title, reusing the check-center button]` listing (a) the term's manuscript links in order (`1-2 씬 1 · 리엔 · “…”`), (b) `원고에서 고르기` (opens the script in block-pick mode), (c) `에피소드 · 장으로` (point picker). Choosing applies immediately with undo toast.
  - **(0.2.0)** While a stored record exists, the sheet has one more row, `TODO_COPY_WORLD_REVEAL_AUTO`. It deletes the stored record (undo toast), and the term returns to the automatic reveal (WLD-72).
  - Choosing (a), (b) or (c) on a term that had no stored record writes one: from then on the term is hand-set.
  - The sheet header shows the automatic location when there is no stored record, e.g. `첫 공개 | 1-4 씬 2 · #012` followed by `TODO_COPY_WORLD_REVEAL_AUTO_MARK` (the mark that says the value is automatic).
- Suggestion when there is none: chip `<first link location>을 첫 공개로` (e.g. `1-2 씬 1을 첫 공개로`), placed on the card and in the check center row; nothing changes until tapped.
- Lifecycle (engine owned by SCR / rule 12): deleting the anchor block moves the record to the next surviving block (same scene → next scene → next episode → `예정`) with dotted `◇` `근거 지워짐` and actions `다른 블록에 잇기 · 이대로 두기 · 기록 지우기`; restoring the block re-attaches. Moving the block moves the record; the toast adds the `▲` delta: `◇ 첫 공개 <old> → <new> · ▲ 새로 N` (N recomputed under links-only). Scene split keeps the record on its block (`첫 공개 | 오리지늄 등불 · 씬 3 #044 그대로`). Deleting the term trashes the record with it (WLD-14).
- Sources: rule 21 §5; rule 12 §1–§3 (◇ 용어 첫 공개 · 저장 · 같이 옮겨짐); rule 22 split sheet; boards `RSOrphan`, `RSSplit`, `Check`, `CSheetTime`.

#### WLD-19 · 쓰인 곳 판정 · 세 무리 — P0 · needsExternal: no (0.2.0)

- Usage of term T = every link run targeting T that is not inside a trashed block / trashed scene. Grouped:

| Text | Game output | Candidates searched | Link makes ▲ | Card group |
| --- | --- | --- | --- | --- |
| Dialogue line text, narration, system text | yes | yes | yes | `쓰인 곳` |
| Choice option line | yes | yes | yes | `쓰인 곳`, tag `선택지` |
| Speaker display name | yes | no (cast rules) | no | none |
| Author memo, direction block | no | no | no | `원고 밖 · 메모` |
| 괄호 지문 run inside dialogue | undecided (rule 21) | no | no | `원고 밖 · 메모` |
| Scene name, episode name | no | no | no | none |
| Character profile, page, place/term/faction card, life recorder | no | no | no | `원고 밖 · 메모` + location label (`인물 · 세라`, `페이지 · 하층 구역의 빛`) |
| Character profile game text (voice, archive docs) | yes | no | undecided → no | `원고 밖`, tag `게임 텍스트` |
| Block in trash | no | no | no | none (returns when restored) |
| Direct `전환 문구` (rule 22) | yes | yes `[PROPOSED]` | yes if it holds a link | `쓰인 곳` `[PROPOSED]`; Q-05 |

- **(0.2.0)**
  - "Game output" in this table is a property of the text kind, not of the `게임 제작 도구` switch. Dialogue, narration, system text and choice lines yield usage, `▲` and candidates whether the switch is on or off.
  - Only the row "Character profile game text" depends on the switch: while it is off, those links are not listed under `원고 밖` and not counted (WLD-76, CST-74).
  - Direction blocks are plain text with the switch off (U-14.11); they stay `원고 밖 · 메모`.
- Counts: `쓰인 곳 · N` = links in game-output manuscript text = the term's usage count. `후보 · N · 확인 안 함` = current candidates of T (WLD-21) after `아님` filtering. `원고 밖 · 메모 · N` = links elsewhere.
- Card header string: `쓰인 곳 · <usage>` + (` · 후보 <candidates>` if > 0).
- Row order inside a group: manuscript order (chapter › episode › scene › position; a scene's `전환 문구` sits before its first block); out-of-manuscript rows by kind then name `[PROPOSED]`.
- Each row shows the whole sentence with the span boxed (search-highlight rule); the location label is small mono.
- Sources: rule 21 §2, §4 (table), §8; feedback search-highlight.

#### WLD-20 · ▲ 공개 전 사용 — P0 · needsExternal: no (0.2.0)

- **(0.2.0)** R below is the **effective** reveal (WLD-72).
  - With the automatic reveal, R is the block of T's first link, so no link of T comes before R and T has no `▲`.
  - `▲ 공개 전 사용`, `맞음` and the `잇기 시 ▲ …` lines therefore arise only for a term with a stored (hand-set) reveal: a 0.1.0 record, a point reveal beyond the 집필 선, or one set with `공개 시점 옮기기`.
  - The rules below are unchanged for that case.
- For each link L of T in game-output manuscript text, with reveal R of T:
  - R = `BLOCK(b)`: L is `▲ 공개 전 사용` iff position(L) < position(b) in manuscript order. A link inside block b itself is after (rule 11-2 `X부터` includes the block). A link in an option line of the choice block b counts as inside b.
  - R = `POINT(u)`: L is `▲` iff position(L) < position(first block of u). If u lies beyond the 집필 선, every manuscript link is `▲`.
  - No R: no `▲`.
- Story time is never consulted (flashback scenes are compared by manuscript order).
- If R's anchor is `근거 지워짐`, compare against the block it moved to and show dotted `◇ 근거 지워짐` on each `▲` row.
- Card row marker: `▲ 공개(<reveal episode>) 전 사용` (e.g. `▲ 공개(1-7) 전 사용`). List: `▲ 앞에서 N회 사용`. Script module card `점검 | ▲ 공개 전 사용 N · …` counts only `▲` (never `◇` candidates). Tour copy `▲ 앞에서 1회 사용` means the same number (rule 21 §2). Links marked `맞음` are excluded from these counts `[PROPOSED, Q-07]`.
- Usage counts and `▲` come from the link index directly and are never delayed by candidate scanning (rule 21 §7).
- Manuscript marker: a pre-reveal link renders the link shape + amber `▲` in the margin, always visible (SCR renders).
- Sources: rule 21 §5, §2, §8; rule 11 §2; boards `Term`, `World`, `CSheetMods`, `Check`.

#### WLD-21 · 글자 후보 찾기 (matcher) — P1 · needsExternal: no

Determined by the rule (implement exactly):

1. **Patterns.** For every term with `글자 찾기` on (and, if hidden, only while unlocked): its name and each alias. A pattern with fewer than 2 syllables is not searched; the card says `1음절 · 글자 찾기 안 함` when the name and all aliases are 1 syllable.
2. **Normalisation.** Text and patterns are NFC; Latin letters compare case-insensitively; formatting boundaries (bold etc.) are ignored (21 draft). 초성 matching is never used for candidates.
3. **Multi-word names.** Spaces in a name are ignored when matching (`오리지늄 등불` also matches `오리지늄등불`); particles are checked only after the last word.
4. **Word start.** The character before the match must be: start of the scanned segment, whitespace, punctuation, a quote, or a bracket. `하층오리지늄` does not match.
5. **What follows (full match).** Accepted if the match is followed by the end of the segment, whitespace, or punctuation; or by an entry of the particle list; or by two list entries chained (`에서는`, `으로도`, `이라는`). Built-in list (project may append entries; Q-12): `은 · 는 · 이 · 가 · 을 · 를 · 의 · 에 · 에서 · 에게 · 한테 · 로 · 으로 · 와 · 과 · 도 · 만 · 까지 · 부터 · 처럼 · 보다 · 이나 · 나 · 이랑 · 랑 · 이야 · 야 · 이다 · 이었 · 였 · 이라 · 라`. Any other continuation (`오리지늄스러운`) is not a match; other spellings (`오리지니움`) are not matched (register an alias instead).
6. **Cut-off (`끊김`).** A prefix of a name or alias with ≥ 2 syllables, at word start, immediately followed by `…` (U+2026), `⋯` (U+22EF), `—` (U+2014) or `-` (U+002D) → candidate tagged `끊김`. A 1-syllable cut-off (`오… 그거?`) is not searched.
7. **Overlap.** When matches of different terms overlap, the longest match takes the span (`오리지늄 등불이` is a candidate for `오리지늄 등불` only, never for a term `등불`).
8. **Already linked text** is never a candidate (the scan skips link runs).
9. **Scope.** Only the text kinds marked "candidates searched: yes" in WLD-19.
10. **Homonyms** are ordinary candidates (`등불을 들고` for a term `등불`); the author answers `아님`.

Pending decisions (implement the proposed default behind a single matcher option so it can change; listed in Q-04):
- (a) What may follow the particle(s) inside the same word: proposed default = nothing (the word must end there). Consequence: `오리지늄이었다` and `오리지늄이라서` do not match although `이었` and `이라` are in the list.
- (b) A full name or alias followed by `…`: proposed = ordinary candidate (not `끊김`, since it is not cut off).
- (c) ASCII `...` (three periods) as a cut-off mark: proposed = no (not in the list).
- (d) A cut-off prefix shared by two terms (e.g. two names starting `오리지`): proposed = candidate for each term whose pattern has that prefix, unless one term's full pattern matches the same span.
- (e) Two terms whose patterns match exactly the same span with the same length: proposed = candidate for each.
- (f) Link runs and `괄호 지문` runs inside a segment: proposed = they split the segment; the position right after or before them counts as a word boundary.
- (g) Syllable count for non-Hangul names: proposed = each Hangul syllable, Latin letter run character, digit or CJK character counts 1; spaces do not count.
- (h) Spaces "ignored" in multi-word names: proposed = optional whitespace only where the name itself has a space (so `오리 지늄` does not match `오리지늄`).

- Output per candidate: `{termKey, containerKey (blockKey or sceneKey for 전환 문구), optionIndex?, start, end (offsets in the original segment), matchedText (NFC), kind: FULL | CUT_OFF, occurrence (0-based index among candidates of the same term with the same matchedText in the same container)}`.
- Sources: rule 21 §3 (live tab and draft), §4, §7; board `RTCandidate`, `RTLinkAll`.

#### WLD-22 · 후보 확인 · 잇기 · 아님 — P1 · needsExternal: no (0.2.0)

- Where candidates appear: the card's `후보` group (`◇ 후보 · 끊김 · 잇기 · 아님` under the sentence), the check center (`◇ 공개 전 후보` rows only), and the manuscript when the display toggle `용어 후보` is on (dotted underline; tap → small popover).
- **(0.2.0)** Every place above exists only while the project setting `용어 후보` is on (WLD-73). The manuscript part changes:
  - the underline is always drawn while the setting is on (no display toggle);
  - a block tap only selects (U-14.4), so the underline is not tappable;
  - the actions are in the block's `참고` tab, reached by a left swipe, as the row `용어 후보 · 등불 · 잇기 · 아님` (WLD-73).
  - **Superseded:** the manuscript popover below and its tap-to-open.
  - `잇기` and `아님` keep the meaning and records given here.
- Manuscript popover (cover and inner): title = term name (Myeongjo 16) + small `후보 · 끊김` (or `후보`), line `첫 공개 1-7 · 잇기 시 ▲ 공개 전 사용` (the `▲` clause only when linking would create a `▲`), buttons `잇기` (primary) · `아님` (ghost). No other dismissal than tapping outside; nothing happens on swipe.
- `잇기`: wraps exactly the candidate span in a new link run to the term (even when it differs from the name, e.g. `오리지`); usage and `▲` update immediately; undo available (toast `되돌리기` `[PROPOSED]`). In the check center the same row turns into `▲ 공개 전 사용` with `원고로 가기 · 공개 시점 옮기기 · 맞음` when pre-reveal.
- `아님`: stores a rejection `{containerKey, termKey, matchedText, occurrence}`; the candidate disappears everywhere. Undo via toast and via the card `⋯ › 아님으로 둔 것` list (`되돌리기` per row).
- Rejection lifetime: kept while the container exists and the same `(termKey, matchedText, occurrence)` is found again after any edit of that container; editing other text of the block keeps it. If the matched text changes (or the occurrence no longer exists), the record is released (deleted) and the new text is judged afresh. Block number changes and moves to another episode keep it (the key is the hidden permanent key).
- `맞음` is never offered on a candidate.
- Sources: rule 21 §2 (manuscript table), §6 (table), §5 (check-center row); board `RTCandidate`.

#### WLD-23 · 모두 잇기 · 모두 아님 — P1 · needsExternal: no

- Entry: card `후보` group header action `모두 잇기 · 모두 아님`.
- Sheet (cover: bottom sheet over the card; inner: same sheet over the detail pane `[PROPOSED]`): title `후보 N · <용어>` + `취소`; segmented `모두 잇기 | 모두 아님` (initial = the tapped word); rows = each candidate with a checkbox (default all checked), location line (`1-2 씬 1 · 리엔 · 끊김`, `1-3 씬 2 · 세라 · 별칭 빛돌`), and the whole sentence with the span boxed; footer `고른 N개 · 넣으면 ▲ 공개 전 사용 A → B` (only in `모두 잇기` mode and only when B ≠ A); primary `넣기 · N개`; note `넣은 뒤 · 한 단계로 되돌리기`.
- `넣기` applies all checked rows in one transaction; one undo step reverts all.
- Sources: rule 21 §6 (한꺼번에); board `RTLinkAll`.

#### WLD-24 · 맞음 (의도한 공개 전 사용) — P1 · needsExternal: no

- Offered only on `▲ 공개 전 사용` rows (check center and the term card `▲` row `[PROPOSED for card]`). Meaning: the author knows and keeps this pre-reveal use (same word as rule 18's `맞음`).
- Record attaches to the link (`linkId`). It disappears when the link is removed; it survives moving the `첫 공개`. Collected in the check center `맞다고 표시한 것` with undo. Stored in `.civilight` (store owned by the check center).
- Counting: see Q-07 (proposed: a `맞음` link is excluded from `▲` counts, `▲ 앞에서 N회 사용`, and the module badge; the card still lists the link under `쓰인 곳` with a small `맞음` tag).
- Sources: rule 21 §6; rule 18 §5; planning 점검 센터.

#### WLD-25 · 글자 찾기 켬 · 끔 — P1 · needsExternal: no (0.2.0)

- Per-term fixed field, default on. Turning off clears the term's candidate index; `아님` records are kept; the card shows `글자 찾기 꺼짐 · 켜기` (tap `켜기` to re-enable; candidates are recomputed and old `아님` apply again).
- When a term has more than 10 `아님` records, the card shows chip `글자 찾기 끄기`; nothing changes until tapped.
- **(0.2.0)** This per-term switch sits under the project setting `용어 후보` (WLD-73).
  - While the setting is off, no term is scanned and this row and its chip are hidden. The stored per-term values are kept.
  - Turning the setting on again restores each term's own value.
- Sources: rule 21 §3 (동음이의 row), §7 (table).

#### WLD-26 · 후보 색인과 다시 훑는 때 — P1 · needsExternal: no (0.2.0)

- One multi-pattern automaton (Aho–Corasick trie, 21 draft) over all patterns of all searchable terms; plus prefix detection for `끊김` (walk the same trie from each word start until a cut-off mark).
- Index lives in the app DB (derived, never written to `.civilight`).

| When | Work | Visible |
| --- | --- | --- |
| About 0.5 s after typing stops in a block | rescan that block only (background) | nothing; underlines update if `용어 후보` display is on |
| Term or alias added / changed | rebuild trie; rescan the whole manuscript for that term only | card `후보 N 새로 찾음` |
| Project opened or restored from file | full scan in background | check center `점검 중`; numbers dim until done |
| `글자 찾기` turned off | drop that term's candidates; keep `아님` | card `글자 찾기 꺼짐 · 켜기` |

- **(0.2.0)** While the project setting `용어 후보` is off, the index is neither built nor updated, and the scheduler does nothing. Turning it on runs the "project opened" full scan.
- Expected scale: 150 terms × 3 aliases ≈ 450 patterns, 20,000 blocks; a full scan is linear in text length and should finish within a few seconds (real-device check item). Usage and `▲` never wait for this.
- Sources: rule 21 §7.

#### WLD-27 · 원고 표시 계약 (연결 · 후보 · 공개 전 연결) — P1 · needsExternal: no (0.2.0: superseded in part, WLD-73)

- SCR renders; WLD provides per container: link spans with `isPreReveal`, candidate spans with `{termName, kind}`, and the popover actions.

| What | In the manuscript | Default |
| --- | --- | --- |
| Link | current link shape (1px `ink3` underline) | always |
| Candidate | dotted underline; tap → popover `<용어> · 잇기 · 아님` | display menu toggle `용어 후보`, default off |
| Pre-reveal link | link shape + amber `▲` in the margin | always |

- When `용어 후보` is on, the script header shows a chip `표시 · 용어 후보 켬`.
- **Superseded (0.2.0, S-5):** the `Candidate` row and the header chip. The contract is now:
  - **Candidate:** a faint dotted underline, drawn whenever the project setting `용어 후보` is on (default on). It is not tappable; the actions are in `참고` (WLD-73). The `표시` sheet has no `용어 후보` toggle and the head has no chip.
  - **Link** and **pre-reveal link:** unchanged.
  - The underline style (board `scr_u14_input.py` `CAND_ST`): dotted, colour `ink3` at 75 %, thickness 1.5 px, offset 6 px, the text keeping its own colour. It must look lighter than the dotted underline for "attached to something deleted" (DS-18) and differ from the link's solid 1 px `ink3` line (D13).
  - WLD gives the script, per block, the candidate spans `{termKey, termName, matchedText, kind, start, end, optionIndex?}` in text order.
- Sources: rule 21 §2 (원고 표시 table), §8; board `RTCandidate`.

#### WLD-28 · 점검 센터 ‘용어 공개’ 줄 — P0 · needsExternal: no (0.2.0)

Rows this area emits under the check-center filter `용어 공개` (shell, filters, `전체`, `맞음` store and the `지운 항목` filter are the check center's). Ordering inside the filter: `▲` rows first (manuscript order), then `◇ 공개 전 후보`, then `◇ 첫 공개 없음` `[PROPOSED order for the last two]`.

| Row | Copy | Buttons |
| --- | --- | --- |
| Pre-reveal link | `▲ 공개 전 사용 · 1-4 씬 2` / sentence `세라: “[오리지늄]은 타지 않아. 빛만 남지.”` / `‘오리지늄 등불’ 첫 공개 1-7 · 사용 1-4` | `원고로 가기 · 공개 시점 옮기기 · 맞음` |
| Pre-reveal links of a term revealed beyond the 집필 선 | one grouped row `<용어> · ▲ N` (e.g. `잿불의 서약 · ▲ 3`), expands to the per-link rows | per expanded row as above |
| Pre-reveal candidate | `◇ 공개 전 후보 · 1-2 씬 1 · 리엔: “등불 속 그 돌, [오리지]… 뭐라고 했죠?” · ‘오리지늄 등불’ 끊김 · 첫 공개 1-7` | `원고로 가기 · 잇기 · 아님` |
| Term without reveal | `◇ 첫 공개 없음 · <용어> · 쓰인 곳 N` `[PROPOSED full copy]` | chip `<location>을 첫 공개로`, `원고로 가기` `[PROPOSED]` |
| Reveal anchor deleted | on each `▲` row: dotted `◇ 근거 지워짐` | as `▲` |
| Hidden term (locked) | `▲ 숨긴 항목 관련 N · 지문으로 보기` | `지문으로 보기` |

- Source note: the board `Check` draws only `잇기 · 아님` on the `◇ 공개 전 후보` row; rule 21 §5 gives `원고로 가기 · 잇기 · 아님` and wins.
- **(0.2.0, D33)** The three term kinds stay in the check center and are compared with the effective reveal (WLD-72).
  - `▲` rows come only from terms with a stored reveal (WLD-20).
  - `◇ 공개 전 후보` rows appear only while `용어 후보` is on.
  - `◇ 첫 공개 없음` keeps its trigger, a term with usage N ≥ 1 and no reveal. The automatic reveal makes that impossible, so the row no longer shows. No new trigger is added.
  - **Superseded:** the chip `<location>을 첫 공개로` on that row.
- Post-reveal candidates never appear in the check center (card only).
- `전체` count includes `◇` rows (rule 21 §8); the script module badge counts only `▲`.
- After `잇기` on a `◇` row the row is replaced in place by the `▲` row.
- `공개 시점 옮기기` opens the WLD-18 sheet.
- The check center has no lens (rule 11-4).
- Sources: rule 21 §5, §8; rule 11 §3; planning 점검 센터 table; boards `Check`, `TwoCheck`, `ColCheck`.

#### WLD-72 · 첫 공개 자동 (automatic first reveal) — P0 · needsExternal: no (0.2.0, S-5)

- **Effective reveal.** For term T: `effectiveReveal(T) = stored(T) ?: auto(T)`.
  - `stored(T)` is the live `◇ 첫 공개` record of WLD-18: from 0.1.0, or written in 0.2.0 by `공개 시점 옮기기`. A stored record always wins (D12).
  - `auto(T)` is the position of T's first live link in manuscript order (chapter › episode › scene › block position; a scene's `전환 문구` sits before its first block, Q-05) among the `쓰인 곳` text kinds of WLD-19: dialogue, narration, system text, choice option lines, and direct `전환 문구` if it holds links.
    - Links in memos, direction blocks, pages, cards, profiles and the recorder never count.
    - Links in trashed blocks or scenes do not count.
    - Links in hidden scenes count, but nothing shows them while locked (WLD-13).
    - A link in a choice option line makes the reveal that **choice block**. The option-line ban of WLD-18 is for stored records only.
    - With no such link, `auto(T)` is none.
- **Derived, never stored.**
  - `auto(T)` is recomputed from the link index whenever links, blocks or their order change. It is never written to `changes.json` or anywhere else, so a stored `firstReveal` record always means hand-set and 0.2.0 adds no data (scope §8).
  - Deleting, moving or splitting the block of an automatic reveal just recomputes it: there is no `근거 지워짐`, no `◇ 첫 공개 … → … · ▲ 새로 N` toast and no `기록 지우기`. Those stay for stored records (WLD-18).
- **Display.**
  - The term card row `첫 공개 | 1-4 씬 2 · #012` gets the automatic mark `TODO_COPY_WORLD_REVEAL_AUTO_MARK` after it.
  - The list's `첫 공개 1-4` (WLD-02) and the script `시간` tab row `◇ #012 · 용어 첫 공개` show the effective reveal with no mark.
  - With no effective reveal: `첫 공개 없음` (WLD-02, WLD-08).
- **Hidden scenes.** `auto(T)` counts links in hidden scenes whatever the lock state, so the reveal, `▲` and candidate comparisons never change with the lock.
  - While the hidden lock is closed and the effective reveal's block (automatic or stored) is in a locked hidden scene, the card row reads `첫 공개 | <episode> · 숨긴 씬`. The scene and block are not named; `숨긴 씬` is WLD-13's word.
  - The list label keeps the episode only (`첫 공개 1-4`), as it already does.
  - The script `시간` row is inside the hidden scene and is not drawn while locked.
- **Consequences.**
  - Without a stored record a term has no `▲ 공개 전 사용`.
  - `◇ 공개 전 후보` still compares candidates with the effective reveal, so a cut-off word before the first link is a pre-reveal candidate (TC WLD-T73).
- **Hand-set in 0.2.0.** The term card's `첫 공개` row and the check center's `공개 시점 옮기기` open the WLD-18 sheet. Picking writes a stored record, and `TODO_COPY_WORLD_REVEAL_AUTO` removes it.
- **Domain.** `domain:world` `TermReveals.effective(doc, termKey, isLive): EffectiveReveal(kind: STORED | AUTO, anchor)` is additive. The existing `TermReveals.record` stays the stored record. Every reader of "the reveal" (lists, card, `▲`, candidates, check rows, the 시간 tab) uses `effective`.
- **Upgrade.** A 0.1.0 project keeps every stored record and its `▲` rows. Its terms without a record get the automatic reveal at once.
- Sources: decisions rows `단순화 · 용어 첫 공개 · 후보` (planning); scope S-5, D12, D33.

#### WLD-73 · 용어 후보 설정 · 목록 · 밑줄 · 참고 줄 (candidate check) — P1 · needsExternal: no (0.2.0, S-5)

- **Setting.** Project setting `용어 후보` (switch, default **on**), with sub `TODO_COPY_WORLD_CANDIDATES_SUB`.
  - It sits in `프로젝트 설정` in the world group and belongs to the `feature:library` settings screen.
  - It is stored as the additive project field `termCandidates: Boolean = true` (with the other project settings; it travels in `.civilight`).
  - It is one switch for the whole candidate check (rule 21 §3–§7): the matcher, the index, the per-term `글자 찾기` and every candidate surface.
- **While on:**
  1. **List in the 용어 tab.** The last section of the 용어 list (after `아직 공개 전`) is `.sec` `용어 후보 · N`, where N is the number of terms that have at least one candidate.
     - One row per such term, in term list order: title = term name; fragment `후보 3` (all of that term's candidates after `아님`, pre- and post-reveal); right column `◇ 공개 전 후보 1` when some come before the reveal.
     - Tapping a row opens the term card scrolled to its `후보` group, where `잇기 · 아님` per row and `모두 잇기 · 모두 아님` (WLD-22, WLD-23) do the work.
     - No candidate in the project hides the section. Locked hidden terms are left out (WLD-13).
  2. **Underline in the manuscript:** WLD-27 (0.2.0).
  3. **`참고` row.** For a selected block (left swipe → `참고`, script area), the top of the tab shows one row per candidate in that block, in text order: `용어 후보 · <matchedText> · 잇기 · 아님`, e.g. `용어 후보 · 등불 · 잇기 · 아님` (board `scr_core.py` `cand_row`).
     - `잇기` and `아님` stay text, 44 px targets.
     - `잇기` and `아님` act exactly as in WLD-22 (one undo step each); the row then leaves.
     - In a read-only project the `참고` tab opens for reading (U-14.15), and the row shows without `잇기 · 아님`.
  4. The card's `후보` group, the list's `· ◇ 후보 N` and the check center's `◇ 공개 전 후보` rows, as before.
- **While off:**
  - no underline, no `참고` row, no `용어 후보` section, no `· ◇ 후보 N`, no card `후보` group, no `◇ 공개 전 후보` check rows (D33);
  - no scan (WLD-26);
  - the per-term `글자 찾기` row is hidden.
  - Stored `아님` records and per-term `글자 찾기` values are kept.
  - Links, usage, `▲` and the automatic reveal do not depend on this setting.
- Sources: decisions rows `단순화 · 용어 첫 공개 · 후보` and `단순화 · 용어 후보 표시 (2026-11-24)` (planning); scope S-5, D13, D33; boards `scr_core.py` (`cand_row`), `scr_u14_input.py` (`CAND_ST`).

### D. 연표

#### WLD-29 · 세계 시간 설정 — P0 · needsExternal: no

- Project setting `세계 시간` (row in `프로젝트 설정 › 구조`, value e.g. `연도만 · 시작 1200년`, button `연도만 ›`), also chosen in new-project step 3/4 `시간` with options `연도만 · 예: 1200년` (default), `세계 달력 만들기 · 달 이름 · 한 달의 날 수 · 요일을 직접`, `시간 없이 · 연표 끔`, field `이야기가 시작되는 해` (e.g. `1200`), note `생일이 있으면 나이 자동 계산`.
- Effects:
  - `연도만`: world events carry a year; ages = year difference (not 만 나이; 인물 항목 §6); scene days display as `N일째`.
  - `세계 달력`: world events may carry year · month · day in the world's month names; scenes display calendar dates (and ranges for 대략); `N일째` remains as a secondary label (rule 11 §6).
  - `시간 없이`: the `연표` module is off (home shortcut hidden `[PROPOSED]`); ages only as number fields; scene `때` still works for travel checks (owned by 장소와 때).
- Changing the mode never deletes stored dates: values that the new mode cannot show are kept and shown as `◇` text `[PROPOSED]`.
- A day is always 24 hours; day boundary and time bands are owned by 장소와 때 (rule 18).
- Sources: planning 연표, 온보딩 (새 프로젝트 3/4); 인물 항목 §6; rule 18 §2; boards `NewTime`, `ProjSettings`.

#### WLD-30 · 세계 달력 — P1 · needsExternal: no

- Editor (not drawn) `[PROPOSED]` reached from `세계 시간 › 세계 달력`: ordered list of months, each `이름` + `날 수` (integer ≥ 1); optional ordered list of weekday names (empty = no weekdays); story start date (month, day) in the start year; buttons `+ 달 · + 요일 · 저장`.
- Day arithmetic: year length = sum of month lengths; date ↔ day index conversion by cumulative month lengths; weekday = (weekday of story start + day offset) mod weekday count.
- Birthdays (`기념일` fields) use this calendar's months; ages in calendar mode use 만 나이 when the scene/event date is known (cast owns ages; WLD-35 supplies dates).
- Sources: planning 연표; 인물 항목 §6; rule 11 §6; Q-13.

#### WLD-31 · 연표 화면 — P0 · needsExternal: no (0.2.0)

- Header: small `세계 시간과 이야기 시간`, title `연표`, lens chip, `+` (new event, WLD-32). Command bar placeholder `사건 · 연도 찾기` (in the anchor view `사건 · 때 찾기`).
  - **Superseded (0.2.0):** no lens chip (S-1, WLD-71); the timeline reads at `최신`, so the current-lens dot is the 집필 선. No `+`: the first row of the list is `＋ 새 사건` (WLD-74).
- Info line: `달력: 연도만 · 프로젝트 설정에서 월 · 일 달력 추가` (calendar mode shows the calendar name/months instead `[PROPOSED]`) and right `나이 자동 계산`.
- View chips `에피소드 기준 · 이야기 시작부터 · 기준 사건` (WLD-34).
- A vertical line list in time order with diamond dots (current-lens row dot in `acc`):
  - world events before the story;
  - section divider `이야기 시작 · 1장` (first chapter name);
  - story spans (chapters: `1200 · 1장 · 등불지기 · 1-1 ~ 1-10 · 리엔 17세 · 하르트 ??세`; planned chapters `계획`);
  - story events (anchored to scenes, e.g. `리엔, 결사 합류 · 1-3 · 1일째 아침 · 리엔 17세`);
  - world events during/after the story (e.g. `1201 · 상층 송전망 사고 · 세계 사건 · 2장에서 공개 예정`).
- Row: time label (mono, width 64), title (Myeongjo 700 16; world events in `ink2`, story rows in `ink`), sub line: kind + place + ages + manuscript relation (`1-4에서 회상` derived from flashback scenes whose base event is this event; `<point>에서 공개 예정` when the event's reveal point lies beyond the 집필 선).
- Lens: rows whose reveal point or scene lies after the lens point are dimmed `[PROPOSED]` (same rule as 이곳의 시간).
- Tap → event detail (WLD-33). Inner layout A: list | detail; layout B: one 560 column.
- Mode `시간 없이`: module hidden.
- Sources: planning 연표; boards `Timeline`, `TwoTimeline`, `ColTimeline`, `TimelineAnchor`; Q-14 (which characters' ages appear), Q-15 (story events vs scenes).

#### WLD-32 · 사건 만들기 · 고치기 — P0 · needsExternal: no (0.2.0: create superseded by WLD-74)

- **Superseded (0.2.0):** creation is the create card of WLD-74 (`이름 · 때 · 관련 인물`). The fuller sheet below is no longer a create flow. Its other fields (`종류`, `설명`, `장소`, `관련 용어`, `공개 시점`) are set afterwards in the event detail (WLD-33), each by tapping its row, and `종류` follows `때` (WLD-74).
- `+` opens the event sheet `[PROPOSED layout; fields from boards]`: `종류` (`세계 사건 · 이야기 사건`), `이름`, `설명`, time (world event: year, plus month/day in calendar mode; story event: `씬 고르기` — time comes from the scene), `장소` (place or region via WLD-52; e.g. `하층 구역` region), `관련 용어` (multi-pick), `공개 시점` (optional point, e.g. `2장`), primary `넣기`.
- Editing: tap a field in the detail.
- Delete: `⋯ › 지우기` (to trash, undo toast; flashback scenes based on this event keep their stored time and show `◇` base deleted `[PROPOSED, by analogy with rule 20]`).
- Sources: planning 연표, 지도 ↔ 연표 (사건에도 장소); boards `TwoTimeline`, `TimelineMap`, `RTFlashback` (기준 사건 대정전 1195).

#### WLD-33 · 사건 상세 — P1 · needsExternal: no

- Pane title `연표 · 세계 사건` (or `연표 · 이야기 사건` `[PROPOSED]`), name (e.g. `대정전`), time (`1195`), description, rows `장소 | 하층 구역`, `그때 나이 | 리엔 12세 · 세라 16세`, `원고 | 1-4 씬 2 · 회상`, `관련 용어 | 오리지늄 등불`.
- Section `이 사건에서 바뀐 것`: `◆` records anchored on this event (rule 12 `세계 사건` anchor: applies from before the story start), each `<item> · <field> | <old> → <new>` (e.g. `리엔 · 소속 | 없음 → 하층 구역의 고아`, `하층 구역 · 상태 | 빛 있음 → 대정전 이후`). `+ 변화` `[PROPOSED]` opens the shared ◆ creation flow with anchor = this event.
- Cover: full-screen detail; inner A: right pane.
- **(0.2.0)** Adds the row `관련 인물 | 리엔 · 세라` (WLD-74), placed after `장소`. The detail is where `종류`, `설명`, `장소`, `관련 용어` and `공개 시점` are set after an event is created in place.
- Sources: board `TwoTimeline`; rule 12 §1 (자리 · 세계 사건).

#### WLD-34 · 연표 보기 셋 — P1 · needsExternal: no

- `에피소드 기준` (default): story rows labelled by episode and `N일째` (`1-3 · 1일째 아침`).
- `이야기 시작부터`: story rows labelled by day since story start (`3일째`; 대략 → `약 13일째 (11~15)`).
- `기준 사건 · <name>`: tap the chip → pick any event (world or story); rows labelled by offset from the anchor: `기준` on the anchor; days for story rows (`−1일`, `+약 7일`); years for world events at year precision (`−5년`); sub line keeps the original location (`1-6 씬 1 · ‘며칠 뒤’ 대략 · 5~9일`). Footer hint `대략인 때는 범위(5~9일)로 · 점검도 범위로`.
- Ranges: an approximate offset [a, b] in days is labelled `약 m` where m = round((a + b) / 2) (derived from the rule 11 §6 examples: 5~9 → `+약 7일`, 11~15 → `약 13일째`), with the range in the sub line; exact offsets have no `약`.
- The default view for new projects comes from `설정 › 지도 · 이미지 · 연표 › 때 표기 기본`; per project `프로젝트 설정 › 때 표기` (owned by 장소와 때).
- All three views read the same resolved values (rule 11: store base + interval; every display and check uses the resolved value).
- Sources: rule 11 §4–§6; planning 지도 ↔ 연표; boards `Timeline`, `TimelineAnchor`, `SettingsMore`, `ProjSettings`.

#### WLD-35 · 그때 나이 — P1 · needsExternal: no

- For an event or story span at time t, for characters with a `출생` value: `연도만` → age = year(t) − birth year; `세계 달력` → 만 나이 when t has month/day; `출생` = `알 수 없음` → `??세` (e.g. `하르트 ??세`); no `출생` → omitted.
- Shown in timeline sub lines and in event detail `그때 나이`.
- Sources: planning 연표 (리엔 12세); 인물 항목 §6; boards `Timeline`, `TwoTimeline`; Q-14.

#### WLD-74 · 사건 만들기 · 그 자리에서 (create an event in place) — P0 · needsExternal: no (0.2.0, U-15)

- **Row.** The first row of the 연표 list (above the first world event, in every view of WLD-34) is icon `plus` + `새 사건`. It opens the shared create card titled `새 사건`, with CST-66's look, buttons and behaviour (`취소 · 만들고 열기 · 만들기`; `만들고 열기` opens the event detail, WLD-33). Hidden in a read-only project, and absent while world time is `시간 없이` (the module is off).
- **Fields (U-15.7):** `이름 · 때 · 관련 인물`. Only `이름` is required.
  - `때` is optional. It has two visible choices:
    - a year (plus month and day in `세계 달력` mode, WLD-30), which makes a `WORLD` event;
    - a scene picked with `씬 고르기` (as WLD-32), which makes a `STORY` event whose time is the scene's.
    - Left empty, the event is `WORLD` with no date, and the 연표 lists it after the dated world events before the story. `종류` is never asked: it follows from `때`.
  - `관련 인물` is optional: several live characters, picked with the character picker (its `＋ 새 인물` row is not offered inside this card; no nested card).
- **Data (additive).** `TimelineEvent` gains `relatedCharacterKeys: List<ULID> = []`.
  - The event detail (WLD-33) shows the row `관련 인물 | 리엔 · 세라`, tap → the same picker.
  - Deleted characters in it follow CST-41's dotted rule for `연결` values. Locked hidden characters show as `숨긴 인물` (CST-39).
- **Pickers.** The event pickers (`기준 사건` chip, WLD-34; flashback `기준 사건`, rule 11) show the row `＋ 새 사건` / `＋ ‘<query>’ 새 사건` (U-15.3). In a picker the card has `취소 · 만들기` and the new event is picked.
- **Write.** One edit through the event write path of `domain:world` `time` (`Events.kt`), extended additively with `when` and `relatedCharacterKeys`. One undo step.
- Sources: `u15-create-in-place.md` rows 4, 7 (planning); scope U-15.4, U-15.7.

### E. 지도 만들기

#### WLD-36 · 지도 목록 · 새 지도 — P0 · needsExternal: no (0.2.0: image maps only, WLD-75)

- `세계 › 지도` tab: header small `잿빛 새벽의 등불 · 세계`, title `지도`, `+`. Section `지도 · N`; horizontal row of map cards (200 × 150 thumbnail, name in Myeongjo 16, small line e.g. `생성 · 핀 9 · 영역 4` or `세계 지도 › 핀 · 이미지` for a sub-map).
- `+` (or `+` on a pin's sub-map row, WLD-48) opens the sheet `새 지도 · 닫기`; when started from a pin it shows `상위 | 세계 지도 · 핀 ‘상층’ | → 하위 지도`. Options (one radio-like row each):
  - `지형 만들기 · 종류 · 땅 비율 · 그림 → 후보 3` → WLD-54;
  - `이미지 가져오기 · 갤러리 · 파일 · 큰 이미지도 그대로` → WLD-38;
  - `스케치 촬영 · 종이에 그린 지도 → 펴기 · 덧그리기` → WLD-39;
  - `노드 지도 · 바탕 없이 장소와 연결만` → WLD-50.
- Map names: authored; default `[PROPOSED]` `지도 N`. A map list view for accessibility is not designed (Q-21).
- Sources: planning 지도 만들기 (바탕 table); board `MapNew`.

#### WLD-37 · 지도 보기 — P0 · needsExternal: no (0.2.0: WLD-75)

- Cover (`MapC`, `MapSoftC`): full-screen map under glass bars; header small `세계 · 지도` (or `세계 · 지도 · 색면`), title = map name (e.g. `하층 구역 지도`), lens chip, zoom label (e.g. `120%`); breadcrumb when nested (`세계 지도 › 하층 구역`). Tapping a pin selects it (`acc`) and opens the place bottom sheet (WLD-09) with `핀 옮기기 · 장소 카드 열기`.
- Inner (`Map`, `MapSoft`): map fills 800×800; side card at right (WLD-09) with `장소 카드 · 핀 옮기기`; bottom-left chips: layer/scope filters (`전체 핀 · 하층 구역 · 상층`; on generated maps `전체 핀 · 지역 · 그림 · 색면 · 편집`) and `지도 이미지 바꾸기`; scale bar bottom-left (`1일 · 15 km`).
- Gestures: two-finger pan/zoom (standard map interaction; not a hidden command). Everything else is buttons.
- `지도 이미지 바꾸기`: pick a new base image; all overlay data keeps its 0–1 coordinates (data is never baked into the base).
- `편집` → edit mode (WLD-40).
- Fold/unfold: centre and zoom stay; the sides reveal more; the selected pin keeps its screen position; place sheet ↔ side card is the same element; in edit mode the tool bar and in-progress vertices stay.
- Sources: planning 지도, 접기 · 펼치기; boards `Map`, `MapC`, `MapSoft`, `MapSoftC`, `MFoldMap`; Q-02 (pin tap with child map).

#### WLD-38 · 바탕 · 이미지 가져오기 · 큰 이미지 — P0 · needsExternal: no (0.2.0: kept, WLD-75)

- Import from gallery or file picker; the image is kept uncropped at full resolution, stored once by content hash.
- Large images load as tiles: a small overview first, then sharp tiles only for the zoomed area (subsampled region decoding; reference: telephoto). Loading the whole bitmap freezes around 8000 px, so full decoding is forbidden above a threshold `[PROPOSED: > 4096 px on the long edge]`.
- Sources: planning 지도 만들기 (성능); Q-21.

#### WLD-39 · 바탕 · 스케치 촬영 — P1 · needsExternal: no (0.2.0: hidden, data kept, WLD-75)

- Take a photo of a paper map with the system camera (or pick a photo), straighten it (`펴기`) and trace over it with the normal tools (`덧그리기`).
- `[PROPOSED]` 펴기 = four draggable corner handles + `펴기` button producing a perspective-corrected image; the original photo is kept. No ML or cloud service.
- Sources: planning 지도 만들기 (바탕 · 스케치 촬영); board `MapNew`.

#### WLD-40 · 편집 모드 · 도구 줄 — P0 · needsExternal: no (0.2.0: image maps only, WLD-75)

- Edit header: breadcrumb, title `<지도 이름> · 편집`, lens chip, `완료`.
- Visible tool bar (`MapToolbar`, 52px items, selected = `pri` fill, `role=toolbar`, label `지도 도구`): base set `이동 · 핀 · 영역 · 경로 · 라벨 · 레이어`; detailed terrain `이동 · 도장 · 강 · 길 · 라벨 · 영역 · 레이어`; hex `한 칸 · 줄 · 채우기 · 고르기 · 되돌리기`; polygon `고르기 · 칸 넣기 · 합치기 · 나누기 · 땅 · 바다 · 레이어`; node `이동 · 노드 · 연결 · 라벨 · 레이어`. Only the selected tool acts on taps (tour copy `고른 것만 동작`).
- Inner side panel tabs `속성 · 레이어 · 목록` (detailed: `도장 · 범례 · 레이어`; polygon: `지역 · 레이어`).
- Cover: the planning doc limits cover editing to `보기 · 핀 · 경로 · 축척` with region editing mainly on the inner screen, while cover boards show the full bar (Q-16). Implement the full bar on both; region editing must remain usable on cover.
- No hidden gestures, no freehand drawing; every change is undoable.
- Sources: planning 지도 만들기 (편집); DS `MapToolbar`; boards `MapEdit`, `MapRouteC`, `MapDetail`, `MapHex`, `MapPoly`, `MapNode`, `TourMap`.

#### WLD-41 · 핀 — P0 · needsExternal: no (0.2.0: kept, WLD-75)

- `핀` tool: tap a point → sheet to choose an existing place (list with search) or `새 장소` (name input) `[PROPOSED sheet]`; the pin stores the place key and 0–1 coordinates. One pin per place per map `[PROPOSED]`.
- Pin mark: icon + glass name tag; selected pin in `acc`; hit area ≥ 44px.
- `핀 옮기기` (visible button on the place sheet/card): enters move mode for that pin; the next tap on the map moves it; `완료` commits, undo available `[PROPOSED mechanics]`.
- Pin properties (inner `속성` tab, cover sheet): place, `보이는 시점`, layer, `하위 지도` (WLD-48).
- Sources: planning 지도 (핀 옮기기 · 지도 이미지 바꾸기는 눈에 보이는 버튼으로); DS `MapMarks`; boards `Map`, `MapC`.

#### WLD-42 · 영역 — P1 · needsExternal: no (0.2.0: kept, WLD-75)

- `영역` tool: tap to add vertices; floating bar `꼭짓점 N · 되돌리기 · 닫기`; `되돌리기` removes the last vertex; `닫기` (button only; no auto-close near the first point) closes the polygon (min 3 vertices). Vertex: 14px rounded square, last one filled; hit area 44px; pop 1.3× → rest with a haptic tick; closing fills in 200 ms.
- Panel `새 영역 · 그리는 중`: name (e.g. `상층 관문 구역`), `종류 | 세력 영역 ›` (kind list `[PROPOSED: 세력 영역 · 지역 · 기타]`, Q-17), `연결 | 세력 · 상층 경비대`, `보이는 시점 | 1-7부터`, `레이어 | 정치`, `색` (swatches from `map-art-*`).
- After creation vertices can be moved (select the region → vertex handles; drag a handle or tap-to-place in move mode).
- A region can be used as a scene `지역` (WLD-52) and as an event place.
- Shape can change over story time (WLD-65).
- Sources: planning 지도 만들기 (영역); boards `MapEdit`, `MMapRegion`.

#### WLD-43 · 경로 — P1 · needsExternal: no (0.2.0: hidden, data kept, WLD-75)

- `경로` tool: tap points (each tap draws that segment, distance counter updates per segment); endpoints may snap to pins. Sheet `경로 · 닫기`: title `결사의 문 → 상층 관문`, big duration `2일 4시간`, small `38 km · 지점 3`, mode chips `도보 15 km · 8시간 · 말 · 배 · 직접` (selected mode shows its parameters), `보이는 시점 | 1-7부터`, `쓰인 곳 | 1-7 씬 2 · “이틀 반이면 닿아.”`.
- On the map a chip label `결사의 문 → 상층 관문 · 도보 2일 4시간 · 38 km`.
- Distance = sum of segment lengths in 0–1 space × map scale (WLD-46); without scale the sheet shows `축척 없음` and `축척 맞추기` `[PROPOSED reuse of rule-18 copy]`.
- Required time per mode (rule 18 §3): with daily distance D and daily moving hours H, distance d: full = floor(d / D); rest = d − full·D; minutes = full·24·60 + rest / D · H · 60. Display: < 24 h → `약 H시간 M분` (minutes rounded to 10) `[PROPOSED rounding]`; ≥ 24 h → `N일 H시간` (hours rounded to nearest) `[PROPOSED rounding]`; never decimal days (`2.5일` is retired).
- `쓰인 곳` of a route is a derived list (rule 12) but the sources do not define how a passage refers to a route (Q-18). The `쓰인 곳` row is not shown until Q-18 is decided.
- Motion: segment-by-segment draw, distance counter, duration enters from below (decelerate 320 ms); haptic tick per point.
- Sources: planning 지도 만들기 (경로); rule 18 §3; boards `MapRouteC`, `MapEdit`, `MMapRoute`, `TourMap`.

#### WLD-44 · 라벨 — P1 · needsExternal: no (0.2.0: kept, WLD-75)

- `라벨` tool: tap → text input → placed at that point; on detailed-terrain maps labels can follow a curve (`곡선 라벨`, control points editable by handles `[PROPOSED]`). Labels are overlay data (not baked), have layer and `보이는 시점`.
- Sources: planning 지도 만들기, 지도 생성 · 양식 (곡선 라벨); board `MapDetail`.

#### WLD-45 · 레이어 — P1 · needsExternal: no (0.2.0: kept, WLD-75)

- Each map has named layers (e.g. `정치`); pins, regions, routes, labels, stamps belong to at most one layer. `레이어` tool / tab: list with visibility toggles, rename, add, order `[PROPOSED]`; each layer has `보이는 시점`.
- Sources: planning 지도 만들기 (레이어 · 시점별 공개); boards `MapEdit`, `MapDetail`, `MapPoly`.

#### WLD-46 · 축척 맞추기 — P1 · needsExternal: no (0.2.0: hidden, data kept, WLD-75)

- Sheet `축척 맞추기 · 닫기`, hint `아는 거리 하나 → 지도 전체`: tap point `A` and point `B` on the map (shown as `A — B`, labels name the nearest place when on a pin, e.g. `A | 결사의 문`, `B | 잿더미 시장`), enter a number (e.g. `2`) and a unit chip `일 · km · 리 · 직접`; for `일` the row `하루 이동 | 도보 15 km ›` picks the travel mode used for conversion; primary `축척 저장`.
- Stored: A, B (0–1), value, unit, mode key. Derived: km per normalised unit = value_km / |AB|. Scale bar shows `<n>일 · <km> km` with n = 1 or 2 by zoom `[PROPOSED]`.
- Unit `리` conversion and the meaning of `직접`: Q-19.
- Sub-maps have their own scale.
- Sources: planning 지도 만들기 (축척); board `MapScale`.

#### WLD-47 · 이동 수단 — P1 · needsExternal: no (0.2.0: hidden, data kept, WLD-75)

- Project setting `이동 수단` (row `도보 15 km · 8시간 · 말 40 km · 8시간 · 배 30 km · 24시간`, button `바꾸기 ›`). Each mode: name, daily distance (km), daily moving hours. Defaults 도보 15/8, 말 40/8, 배 30/24. Users can add modes (`직접`) and edit values `[PROPOSED editor]`.
- Used by route durations (WLD-43), scale conversion (WLD-46), and the travel check (consumer: 장소와 때). Project default mode = 도보; a character field `기본 이동 수단` may override for that character (rule 18 §3; consumer side).
- Sources: rule 18 §3, §7; boards `ProjSettings`, `MapRouteC`, `MapScale`.

#### WLD-48 · 하위 지도 (중첩) — P1 · needsExternal: no (0.2.0: kept, image child maps only, WLD-75)

- A pin can link a child map (`세계 › 지역 › 장소`). Create: pin properties `하위 지도 · 만들기 · 고르기` `[PROPOSED]` → `새 지도` sheet with `상위` line (WLD-36).
- Entering: the trigger is open (Q-02); default = a visible control in the pin's place sheet `[PROPOSED]`. The transition zooms centred on the pin into the child map (decelerate 360 ms, haptic `확인`); breadcrumb `세계 지도 › 하층 구역` at the top; tapping a breadcrumb part zooms out to it (accelerate 240 ms); breadcrumb text swaps only the changed part.
- Distances across maps: see WLD-66.
- Sources: planning 지도 만들기 (중첩); boards `MMapZoom`, `MapEdit`, `MapNew`.

#### WLD-49 · 시점별 공개 — P1 · needsExternal: no (0.2.0: kept, WLD-75)

- Pins, regions, routes, labels and layers have `보이는 시점` (optional point, `X부터`). At lens L an item is visible iff its point ≤ L.
- Reveal view (`시점별 공개`, header `<map> › 시점별 공개`, chip `시점 · 1-5`): mode chips `<L> 시점 · 보이는 것만 | 숨은 것 흐리게`; a point scrubber (`1-1 · 1-3 · 1-5 · 1-7 · 1-9 · 2-1 · 2-3`); summary `보임 · 핀 N · 영역 M | 아직 공개 전 · K`. Hidden items show their point in the tag (`상층 관문 · 1-7`).
- Motion: items becoming visible unblur 6px → 0 over 320 ms, staggered 60 ms in the order passed (same motion as unhide).
- The normal map view at a past lens applies the same visibility (hide by default; dim when the author chose `숨은 것 흐리게` `[PROPOSED persistence per map]`).
- Rule 22 consumer: a scene whose place's pin has a later `보이는 시점` raises `◇ 공개 전 장소 · 전환 문구` (owned by 장소와 때 / SCR).
- Sources: planning 지도 만들기 (시점별 공개); rule 12 §1 (지도의 보이는 시점 · 씬 · 에피소드를 가리킴); boards `MapReveal`, `MMapReveal`.

#### WLD-50 · 노드 지도 — P1 · needsExternal: no (0.2.0: hidden, data kept, WLD-75)

- Base-less map of places (nodes) and connections, for buildings, dungeons, episode routes. Header `<부모> › <이름>` (e.g. `하층 구역 › 결사 본부`), title `노드 지도`, lens chip; tools `이동 · 노드 · 연결 · 라벨 · 레이어`; search `장소 · 노드 찾기`.
- Node: name + optional sub line (e.g. `기록실 · 세라 · 1-3`, `송전탑 쪽 출구 · 2장 공개`), optional place link, `보이는 시점`. Connection: between two nodes, optional label (`계단 · 잠김`, `정문 통과`), `보이는 시점`. Node maps have no scale (travel check shows `◇ 축척 없음 · 점검 못 함`).
- Sources: planning 지도 만들기 (노드 지도); board `MapNode`; rule 18 §6.

#### WLD-51 · 지도 내보내기 4K · 8K — P1 · needsExternal: no (0.2.0: kept, WLD-75)

- Export the current map (base + visible overlay at the current lens) as an image; long edge 3840 px (`4K`, default) or 7680 px (`8K`), default from `설정 › 지도 · 이미지 · 연표 › 큰 지도 내보내기`. Entry `[PROPOSED]`: map `⋯ › 지도 내보내기`, then the system share/save sheet.
- Sources: planning 지도 만들기 (내보내기는 4K 기본, 8K 선택); board `SettingsMore`.

#### WLD-52 · 장소 고르기 (다른 영역에 주는 것) — P0 · needsExternal: no (0.2.0: U-15 row, WLD-70 · WLD-75)

- Opened by scene `장소와 때` (`장소 · 지역으로 바꾸기`, `지도에서 고르기 ›`), bulk `장소 정하기` (`모두 같은 장소 · 씬마다 차례로 · 장소 · 지역`), event place (WLD-32).
- List mode: places (or regions when `지역` is chosen) with the count of scenes using each (`잿더미 시장 · 씬 4개`), search, and the row `새 장소 · 지도에 핀을 꽂으며 만들기`.
- Map mode (`지도에서 고르기 ›`): the map opens in pick mode; tapping a pin picks the place; in `지역` mode tapping inside a region picks it; a bottom bar confirms (`<씬 4개> → <잿더미 시장>`); inner bulk table shows `장소별 씬 수` on the map and the instruction `핀으로 끌어다 놓기 대신 · 고른 씬 → 핀 탭 → 장소 정하기`.
- Returns `PlaceRef(placeKey)` or `RegionRef(mapKey, regionKey)`.
- **(0.2.0)**
  - The list mode's top row is `＋ 새 장소` / `＋ ‘<query>’ 새 장소` (WLD-70). **Superseded:** `새 장소 · 지도에 핀을 꽂으며 만들기`.
  - Map mode works on every listed map with pins, including 0.1.0 node, generated and sketch maps viewed read-only. On a node map, tapping a node that has a place picks that place.
- Sources: planning 지도 ↔ 연표 (장소 정하기); boards `SceneWhere`, `BulkPlace`, `BulkWide`.

#### WLD-75 · 지도 단순화 · 그림 한 장 + 장소 핀 (simplified maps) — P0 · needsExternal: no (0.2.0, S-3, S-2)

- **Rule (S-3).** A new map is one image plus place pins. Hidden, never deleted:
  - creating node, generated (terrain) and sketch maps, and the editing tools of those maps;
  - distance calculation and its surfaces (scale, routes, travel modes);
  - character routes (`인물 동선`).
- **Every map a 0.1.0 project has stays listed and opens for viewing with its base and pins** (D11), so no map looks lost. Stored data is never changed (scope §8).
- **New map.**
  - The `지도` tab's first row is icon `plus` + `새 지도`. A pin's `하위 지도` row in WLD-48 makes a child map the same way.
  - The row opens the system image picker (gallery or file, WLD-38) directly.
  - Picking an image creates an `IMAGE` map named `지도 N` (WLD-36 default), with the parent set when started from a pin, and opens it in map view. N is the smallest number ≥ 1 for which no live map of the project is named `지도 N`.
  - Cancelling the picker creates nothing.
  - **Superseded:** the `새 지도` sheet with `지형 만들기 · 이미지 가져오기 · 스케치 촬영 · 노드 지도`.
- **Status of every map feature in 0.2.0:**

| Feature | 0.2.0 | Stored data |
| --- | --- | --- |
| WLD-36 list | All maps listed with their thumbnails and sub lines (`생성 · 핀 9 · 영역 4` stays for a generated map). New maps: image only (above) | kept |
| WLD-37 view | `IMAGE` maps as before, except: no lens chip (reads at `최신`, WLD-71), no scale bar, no route chips. `GENERATED`, `SKETCH` and `NODE` maps open **view-only**: base (stored result, corrected sketch image, or nodes and connections), pins, regions, labels, layers, at `최신`. On them `편집`, `지도 이미지 바꾸기` and the style chips (`그림 · 색면`) are hidden | kept |
| WLD-38 import, large images | kept | kept |
| WLD-39 sketch | creating hidden; existing sketch maps view-only | photo, corners kept |
| WLD-40 edit, tool bar | `IMAGE` maps only. Tool bar `이동 · 핀 · 영역 · 라벨 · 레이어` (`경로` removed). The detailed-terrain, hex, polygon and node bars are unreachable. Edit head without the lens chip | — |
| WLD-41 pins | kept on `IMAGE` maps; read-only on the others (`핀 옮기기` hidden there) | kept |
| WLD-42 regions | kept on `IMAGE` maps (S-3 does not name them; scenes may use a region as `지역`) | kept |
| WLD-43 routes | tool, sheet and map chips hidden; existing routes are not drawn | kept |
| WLD-44 labels | kept on `IMAGE` maps; curved labels of detailed maps are view-only | kept |
| WLD-45 layers | kept | kept |
| WLD-46 scale | sheet, scale bar and every `축척 맞추기` button hidden | kept |
| WLD-47 travel modes | the project settings row `이동 수단` hidden | kept, incl. `defaultTravelModeKey` |
| WLD-48 sub-maps | kept; a new child map is image-only | kept |
| WLD-49 reveal by point | kept (its own scrubber); the normal map view applies visibility at `최신` | kept |
| WLD-50 node maps | creating and editing hidden; existing node maps view-only | kept |
| WLD-51 export 4K · 8K | kept for every map (base + overlays visible at `최신`, routes not drawn) | — |
| WLD-52 place picker | kept; U-15 row (WLD-70) | — |
| WLD-53 … WLD-61 generation, styles, hex, polygon, detailed terrain, text → terrain | hidden (no entry anywhere); generated maps view-only from their stored result | seeds, history, conditions, overrides kept |
| WLD-62 인물 동선 | hidden: no entry from the map, a character, a place or help | kept (track data is derived) |
| WLD-63 이곳의 시간 | kept (no distance) | — |
| WLD-64 연표 + 지도 | kept, without its lens chip | — |
| WLD-65 region shape over time | kept | kept |
| WLD-66 distance | no UI. The domain functions stay for the travel check (below) | — |
| WLD-67 app settings | `새 지도 그림` and `지형 만들기 · 글로 설명` hidden; `큰 지도 내보내기` kept | values kept |
| WLD-68 motion | rows of hidden features are unreachable; the rest unchanged | — |
| WLD-14 delete, WLD-15 merge (route parts) | the impact and merge sheets leave out route counts (`경로 끝 N · …`), and the fix actions `다른 핀에 잇기 · 경로 지우기` are not offered. The data effects still happen out of sight: a route end stays dotted on delete, and moves to the target pin on merge | kept |

- **Search and commands.** Map-related `/` commands and search rows for hidden tools (`지형 만들기`, `동선`, `축척`, `경로`) are not offered (S-9).
- **Tours and help.** The `지도 안내` tour runs on an `IMAGE` map and leaves out steps whose target is hidden: `경로`, the terrain editor, and `인물 동선 · 장소와 때`'s route step (S-9, D15). It never points at a hidden tool. The tour content is the library area's; this row says which targets are gone.
- **Travel check (S-2, D10).**
  - Owned by 장소와 때 and the check center: off by default, turned on in settings (`이동 점검`).
  - While on, it uses WLD-66 on the distance data a 0.1.0 project already holds (pins, routes, scales, travel modes). 0.2.0 offers no way to add any.
  - While off, its five check kinds are hidden (D33), and their acknowledgements are kept.
  - Storage note for that lane: 0.1.0 has `AppSettings.checkTravel` with default `true`, so "off by default" needs a new additive field rather than rewriting the stored value (scope §8 rule 1). See Q-31.
- **Rule 22 consumer.** `◇ 공개 전 장소 · 전환 문구` still reads pin reveal points (WLD-49), unchanged.
- Sources: decisions rows `단순화 · 지도`, `단순화 · 점검 센터 · 이동 점검` (planning); scope S-2, S-3, S-9, D10, D11, D15, D33; `icon-buttons.md` (`새 지도` menu, the "그 뒤 (지도 단순화)" notes) (planning).

### F. 지도 생성 · 양식

#### WLD-53 · 생성기 — P1 · needsExternal: no (0.2.0: hidden, data kept, WLD-75)

- One generator, on device, deterministic: same generator version + same seeds + same choices → identical result. Pipeline: seed → height and moisture fields on [0,1]² → sea level from `땅 비율` (quantile) → steps in order `해안선 → 산 → 강 → 숲 · 기후 → 마을 · 길` → the same world drawn per format.
- Reference algorithm (mock code, informative only; no parity required with it or with board images): value noise with octaves (3, 6, 12, 24) weighted (.5, .27, .15, .08); kind-specific falloff (`대륙`: one central ellipse; `두 대륙`: two ellipses; `섬 무리`: tiled noise minus centre distance; `한 지역`: no falloff, edges continue off-map); terrain thresholds by quantiles of land height (hill .72, mountain .88, peak .96) and moisture (dry .18, wet .62, swamp .9); rivers descend from high cells over a depression-filled height field (priority flood) to the sea; towns on low dry land near rivers/coast with spacing; roads as a minimum spanning tree between towns; polygon provinces from Lloyd-relaxed Voronoi cells grown from spaced seeds.
- Generation is a pure function in its own module (JVM-testable, no Android types). Target ≈ 1.2 s on device including first draw (real-device check).
- After `이 지도로 시작` the result is fixed as data (base image + coastline + hex cells + provinces + rivers + towns + roads + stamps) together with seeds, choices and generator version. The stored result is the baseline; seeds are used only for `다시`. A newer generator version must never silently change a stored map.
- No place names are generated.
- Sources: planning 지도 만들기 (생성기 규칙), 지도 생성 · 양식; file format §4 (생성 지도); memory map generation; `mapgeo.py` (reference only).

#### WLD-54 · 지형 만들기 흐름 — P1 · needsExternal: no (0.2.0: hidden, data kept, WLD-75)

1. `새 지도 › 지형 만들기` → `양식` (`새 지도 · 지형 만들기 1/2`): note `같은 세계 · 시드 4827 · 양식만 다름 · 시작 전까지 바꾸기 가능`; cards `육각 · 칸마다 지형 · 전략 지도`, `다각형 · 지방 · 국경 나누기`, `상세 지형 · 색면 · 산 · 강 · 길 · 마을`, `윤곽 · 해안선 · 등고선만`, `노드 · 장소와 연결만`, and `글로 설명 · 문장 → 조건 → 지형` (WLD-61); buttons `이전 · 단계별로 만들기`.
2. `지형 만들기` (2/2): base chips `종류 · 대륙 · 두 대륙 · 섬 무리 · 한 지역`, `땅 비율 · 바다 많음 · 반반 · 땅 많음`, style chips (WLD-57), step rows (WLD-55), candidate thumbnails + `새 후보` (inner) / `후보 기록 N` (cover), primary `이 지도로 시작`.
3. `새 후보` generates 3 new candidates using new seeds for unlocked steps; every candidate is kept in the history (`후보 기록 9`) and can be chosen again.
4. `이 지도로 시작` creates the map (WLD-53) and opens map view. Regenerating the whole map later creates a new map; the old map stays.
- Generation draws in step order and doubles as the progress indicator (coastline → land fill → mountains rise → rivers top to bottom → forests → towns · roads, ≈ 1.2 s, no spinner); reduce motion → one 120 ms fade of the finished map.
- Sources: planning 지도 만들기, 지도 생성 · 양식; boards `MapFormat`, `MapGen`, `MapGenWide`, `MMapGen`.

#### WLD-55 · 단계 조작 · 시드 · 고정 · 다시 — P1 · needsExternal: no (0.2.0: hidden, data kept, WLD-75)

- Per step row: name, seed (mono, e.g. `4827`), `고정` toggle (lock icon, label `고정`), `다시` (dice, label `다시`). Header shows locks, e.g. `상세 지형 · 색면 · 시드 4827 · 해안선 고정`.
- Per step 2–3 chips per question, no sliders. Drawn: `산` → `산맥 수 · 1 · 2 · 3 · 흩어짐`, `높이 · 낮게 · 보통 · 높게`; `강` → `개수 · 적게 · 보통 · 많이`, `굽이 · 곧게 · 보통 · 많이`, toggle `호수 · 막힌 곳에 호수`. Chips for `해안선`, `숲 · 기후`, `마을 · 길` are not drawn (Q-20; `[PROPOSED]` none until defined).
- Cover step card: e.g. `강 · 시드 0031`, chips, buttons `강만 다시 · 강 고정`.
- `다시` on a step re-seeds that step only; only the changed layer animates (old fades accelerate 160 ms, new 400 ms); locked steps never change. Re-rolling an upstream step while a downstream step is locked → Q-22.
- Sources: planning 지도 생성 · 양식 (단계 조작); boards `MapGen`, `MapGenWide`, `MMapReroll`.

#### WLD-56 · 시작 뒤 단계 다시 만들기 — P1 · needsExternal: no (0.2.0: hidden, data kept, WLD-75)

- After start, any step can be re-generated from map edit (`<단계> 다시 만들기` `[PROPOSED entry: edit ⋯]`). If the author edited that layer, a sheet asks first: title e.g. `강 다시 만들기`, line `직접 고친 강 2 · 강에 붙은 도장 1(다리)`, options `고친 강은 남기기 · 나머지만 새로` (default) and `모두 새로 · 다리 도장 → 위치 확인 목록`, note `이전 강 · 기록에 보관 · 되돌리기 가능`, buttons `취소 · 다시 만들기`.
- The previous layer result is stored in history and can be restored (undo and history list).
- Overlay data (pins, regions, routes, labels) is never touched by re-generation.
- `모두 새로` produces a `위치 확인` list of stamps that were attached to the old layer `[PROPOSED: list rows = stamp + 원래 자리 · 지금 자리]`.
- Sources: planning 생성기 규칙, 단계 조작; board `MapReroll`.

#### WLD-57 · 그림 · 양식 바꾸기 — P1 · needsExternal: no (0.2.0: hidden, data kept, WLD-75)

- Styles for detailed terrain and polygon: `색면` (default) · `먹선` · `색` · `양피지`. `색면`: province fields in the theme's `map-art-*` colours as soft blurred fields clipped to the coastline, faint coast/ripple/mountain/river/road lines on top; follows dark/light theme; no blue hues on land. Hex and polygon cells use fixed `ter-*` colours (same in both themes): deep #24394A, sea #33505F, plain #8E9A6A, forest #5E7352, hill #A39466, mount #857E73, peak #D3D7DB, desert #C4AB7A, swamp #66705A, snow #C6CED4. `등고선` toggle on detailed terrain.
- Default style for new maps: `설정 › 지도 · 이미지 · 연표 › 새 지도 그림`.
- Format can change until `이 지도로 시작`; switching keeps the coastline fixed on top and crossfades the interior (200 ms).
- Sources: planning 지도 생성 · 양식 (그림); DS `MapStyle`, tokens; boards `MapFormat`, `MapSoft`, `MapGenWide`, `MMapFormat`, `SettingsMore`.

#### WLD-58 · 육각 편집 — P1 · needsExternal: no (0.2.0: hidden, data kept, WLD-75)

- Header `세계 지도 · 육각`, title `칸 칠하기`, info `24 km · 뾰족 · 32×24`. Tools `한 칸 · 줄 · 채우기 · 고르기 · 되돌리기`; terrain palette `바다 · 평원 · 숲 · 언덕 · 산 · 사막 · 늪 · 설원`; hint `칸 탭 → 고른 지형 · 표식(산 · 숲)은 지형 따라 자동`.
- `한 칸`: paint the tapped cell (120 ms fade). `채우기`: flood-fill the connected cells of the same terrain from the tapped cell, spreading by distance (≤ 300 ms total). `고르기`: select a cell → info `O09 | 숲 · 하층 구역 | 칸 24 km · 장소 없음` and button `장소 만들기` (creates a place with a pin at the cell centre). `줄`: Q-23.
- Settings rows: `칸 크기 | 24 km ›` (also defines the map scale), `방향 | 뾰족 · 평평`, `좌표 표시 | 3칸마다`. Coordinates = row letter (A…) + two-digit column (`O09`).
- Mountain and forest marks are drawn automatically from the terrain.
- Sources: planning 지도 생성 · 양식 (육각); boards `MapHex`, `MMapHexFill`.

#### WLD-59 · 다각형 편집 — P1 · needsExternal: no (0.2.0: hidden, data kept, WLD-75)

- Header breadcrumb `세계 지도 › 지방`, title `다각형 · 지역 나누기`; tabs `지역 · 레이어`; region list rows `<이름> | 칸 N` (e.g. `등불 결사 영역 | 칸 37`), hint `이름 · 색 · 연결 세력 → 지역 탭`, `새 지역`.
- Tools `고르기 · 칸 넣기 · 합치기 · 나누기 · 땅 · 바다 · 레이어`: `칸 넣기` assigns tapped cells to the selected region (floating `칸 1 → 하층 구역`); `합치기` merges two regions; `나누기` splits a region by assigning chosen cells to a new region `[PROPOSED]`; `땅 · 바다` toggles a cell between land and sea.
- Region = 이름 · 색 · 연결 세력. Whether a province can be a scene `지역` → Q-17.
- Sources: planning 지도 생성 · 양식 (다각형); board `MapPoly`.

#### WLD-60 · 상세 지형 편집 — P1 · needsExternal: no (0.2.0: hidden, data kept, WLD-75)

- Header `세계 지도`, title `상세 지형 · 편집`, lens chip, `완료`; tabs `도장 · 범례 · 레이어`; tools `이동 · 도장 · 강 · 길 · 라벨 · 영역 · 레이어`; scale bar.
- Stamps (12): `산 · 언덕 · 숲 · 도시 · 마을 · 성 · 폐허 · 탑 · 다리 · 항구 · 동굴 · 표지석`. Hint `탭 → 놓기 · 모서리 → 크기 · 위 손잡이 → 회전`. Size steps `1 · 2 · 3` only; rotation snaps to 15° with a haptic tick (release → nearest 15°); placing drops 14 px with a 0.96 press (decelerate 240 ms). Row `장소 연결 | 결사의 문 ›`. Selected chip `성 · 크기 2 · 0° · 장소 ‘결사의 문’`.
- `강` and `길`: tap points to draw a river or road; edited rivers are flagged as author-edited (used by WLD-56).
- Curved labels (WLD-44).
- Sources: planning 지도 생성 · 양식 (상세 지형); boards `MapDetail`, `MMapStamp`.

#### WLD-61 · 글로 설명 → 조건 → 지형 — P1 · needsExternal: yes (on-device small language model; optional cloud) (0.2.0: hidden, data kept, WLD-75)

- Input screen (`새 지도 · 글로 설명`, title `지형 만들기`): text area (e.g. `북쪽은 눈 덮인 산맥이 가로막고, 남쪽 해안에 항구 도시가 있다. 동쪽은 메마른 사막, 서쪽 숲속에 결사의 문.`), line `함께 읽기 | 장소 6 | 세력 3 | 페이지 ‘하층 구역의 빛’` (project records read along; chips toggle each source `[PROPOSED]`), engine toggle `기기 안 · 클라우드`, result `읽어 낸 조건 · N | 기기 안 모델`, condition rows (`ConditionRow`: 3×3 grid showing 어디, name = 무엇, small = 특징 · 연결 장소, e.g. `산맥 · 북쪽 · 눈 덮임`, `항구 도시 · 남쪽 해안 · 장소 ‘잿더미 시장’`, `사막 · 동쪽 · 메마름`, `숲 · 서쪽 · 장소 ‘결사의 문’`), hint `조건 → 생성기 입력 · 그림은 생성기가 · 이름은 프로젝트 기록에서만`, buttons `조건 · 이 조건으로 만들기`.
- Conditions are a visible, editable list: edit, delete, add (`조건` opens the editor `[PROPOSED]`). Each condition = 무엇 (kind such as 산맥 · 항구 도시 · 사막 · 숲) · 어디 (subset of 3×3 cells) · 특징 · 연결 장소 (existing place only).
- The model never draws: conditions bias the generator's height and moisture inputs (e.g. ridge in the north band, coast in the south, dry east, wet west), so results stay seed-reproducible and step lock/re-roll works unchanged.
- Names come only from project records; the model must not invent names (unknown names are dropped from conditions `[PROPOSED]`).
- Result screen (`글로 설명 · 결과`): header `조건 4 · 3 반영 · 닫기`; each condition row with `반영` or `확인 필요` and `위치` (fix location); unmet example `항구 도시 · 남쪽 해안 · 만이 얕음 → 위치 고르기` with `고치기`; buttons `다른 후보 3 · 이 지도로 시작`. Tapping a condition briefly highlights its area on the map.
- Fallback: when no on-device model exists or the text is hard to read, offer cloud (like dictation); setting `설정 › 지도 · 이미지 · 연표 › 지형 만들기 · 글로 설명 | 기기 안 · 클라우드(정확도↑ · 전송)`. Only the text reading may go to the cloud; terrain generation always runs on device (Q-25). Device support is a real-device check item.
- Stored: the condition list only (model name/version as a note); the map does not change when the model changes.
- Motion: conditions appear one by one; the source phrase is underlined, then the condition row appears 250 ms later; tapping a condition re-underlines its phrase.
- Sources: planning 글로 설명, 범위 (글로 지도 만들기 · v1 포함, 기기 안 모델 지원 기종 확인 후 · 없으면 클라우드), 남은 확인; file format §4; boards `MapText`, `MapTextResult`, `MMapText`, `SettingsMore`; DS `ConditionRow`.

### G. 지도 ↔ 연표

#### WLD-62 · 인물 동선 — P1 · needsExternal: no (0.2.0: hidden, data kept, WLD-75)

- Inner board `MapTrack` (cover version not drawn, Q-21): breadcrumb, title `인물 동선`, chip `시점 · 1-7`; side tabs `동선 · 장소 · 레이어`; character chips (`리엔 · 하르트 · 세라`, multi-select; the focused character in `acc`, others outlined, dashed line).
- Side list `리엔 · 씬 순서`: rows `<episode> | <place> | <scene> · <day label>` (e.g. `1-2 | 하층 거리 | 씬 1 · 1일째 새벽`). Note `장소 · 때는 씬 속성에서 · 선은 지도 경로를 따라 · 경로가 없으면 직선 ‘추정’`.
- Stops = scenes where the character is in the cast (speakers + silent cast, owned by 장소와 때) and that have a place or region; ordered by resolved time (rule 11 §5: by time order, not manuscript order). Flashback scenes, scenes without place (`◇`), scenes whose place was deleted, hidden scenes (dotted `숨긴 씬`) and hidden characters are excluded.
- Line between consecutive stops follows a route that connects the two places if one exists, else a straight line labelled `추정`. Stop chips show the episode number.
- Bottom scrubber of episodes (`1-1 … 1-9`) moves the point and can play; the character position at the point = place of the latest stop at or before it, drawn as an initial + name glass chip.
- Sources: planning 지도 ↔ 연표 (인물 동선); rule 11 §5; rule 19 §6; rule 20 §2; boards `MapTrack`; DS `MapMarks`.

#### WLD-63 · 이곳의 시간 — P1 · needsExternal: no (0.2.0: kept, WLD-75)

- Place card tab `이곳의 시간`: world events whose place is this place (or its region), and scenes at this place, in time order. Rows: time label (`1195`, `1-2 씬 1`), title (event name or scene title), sub (`세계 사건 · 빛이 꺼진 첫 거리`, `1일째 새벽 · 리엔`, `대정전 그날 · 세라`, `계획 · 하르트`).
- Flashback scenes are placed at their own era (rule 11 §4), not at their manuscript position (Q-24: the board `PlaceTime` differs).
- A divider at the lens point `시점 1-7 · 이 아래는 아직` (`acc`, dashed); rows after it are dimmed (50 %).
- Sources: planning 지도 ↔ 연표 (이곳의 시간); rule 11 §4; board `PlaceTime`.

#### WLD-64 · 연표 + 지도 — P1 · needsExternal: no (0.2.0: kept without lens chip, WLD-75)

- Inner screen (header small `세계 · 연표 + 지도`, title `연표`, lens chip): left the event list (`1200 · 1-3 | 리엔, 결사 합류 | 씬 2 · 1일째 아침 | 결사의 문`), right the map. Hint `사건마다 장소 · 고르면 지도에서 강조`. Selecting an event highlights its place pin or region on the map.
- Faction region changes: an event row can show `세력 영역 변화 ◆`; chips `1-7 · 1-9 ◆ · 비교` switch the map between the two points or overlay them; caption `상층 경비대 | 영역 · 1-9 ◆ 넓어짐` and `점선 = 1-9부터 더해진 곳`.
- Cover version not drawn (Q-21).
- Sources: planning 지도 ↔ 연표 (연표 + 지도); board `TimelineMap`.

#### WLD-65 · 세력 영역의 시점별 모양 — P1 · needsExternal: no (0.2.0: kept, WLD-75)

- A region's shape (vertex list) is a value with `◆` history like any field: editing vertices at a lens point offers `이 값 고치기 · 여기서부터 바뀜` (rule 11-2); the map at lens L draws the shape in effect at L; `예정` shapes (beyond the 집필 선) are not applied at 최신.
- Comparison of two points: union drawn with the added area dotted.
- Sources: planning 지도 ↔ 연표 (세력 영역이 시점에 따라 바뀌면 ◆); rule 11 §1–§2; board `TimelineMap`.

#### WLD-66 · 거리 계산 (동선 · 이동 점검에 넘김) — P1 · needsExternal: no (0.2.0: domain kept, no UI, WLD-75)

- `distanceKm(from: PlaceOrRegionRef, to: PlaceOrRegionRef, mode?)` returns `{km, kind: ROUTE | STRAIGHT | ZERO, unavailable: NO_SCALE | NO_LOCATION | null}`:
  - same place → 0 (`ZERO`);
  - both pins on one map with a route connecting them → route length (`ROUTE`; shortest if several `[PROPOSED]`);
  - else straight-line distance between the two points (`STRAIGHT`; a lower bound, so the consumer may still raise `▲` with `경로 없음`);
  - a region involved → the nearest points between the two shapes;
  - pins on different maps → use their ancestor pins on the nearest common ancestor map (`하위 지도의 핀 → 상위 지도에 있는 조상 핀끼리`);
  - map without scale or node map → `NO_SCALE` (consumer shows `◇ 축척 없음 · 점검 못 함` with `축척 맞추기`);
  - deleted or trashed place → `NO_LOCATION` (never computed from trashed coordinates).
- `requiredMinutes(km, mode)` as in WLD-43; formatting helper `formatDuration`.
- No detour multiplier.
- Sources: rule 18 §3, §6; rule 20 (씬 장소 row); planning 지도 ↔ 연표 (이동 점검).

### H. 설정 · 모션 · 나중

#### WLD-67 · 설정 · 지도 · 이미지 · 연표 (앱 설정) — P1 · needsExternal: no (0.2.0: rows changed, WLD-75 · WLD-73)

- Screen `설정 › 지도 · 이미지 · 연표` (row in `설정`: `지도 · 이미지 · 연표 | 색면 · 홀로 · 생성 방식 · 이동 거리 ›`). Rows owned here: `지도 · 새 지도 그림 | 색면 · 먹선 · 색 · 양피지 · 색면 ›`; `지형 만들기 · 글로 설명 | 기기 안 · 클라우드(정확도↑ · 전송) · 기기 안 ›`; `큰 지도 내보내기 | 4K · 8K · 4K ›`. Rows shown on the same screen but owned elsewhere: `이미지` group (`홀로그램 효과 기본`, `기본 이미지`), `연표 · 때` group (`때 표기 기본`, `이동 점검`, `장소 없는 씬 알림`).
- **(0.2.0)**
  - `지도 · 새 지도 그림` and `지형 만들기 · 글로 설명` are hidden (WLD-75), and the row sub in `설정` drops `생성 방식`. The row sub is the library area's, which picks its new text.
  - `이동 점검` stays and is off by default (S-2, WLD-75).
  - The project setting `용어 후보` is WLD-73's (project settings, not this app screen).
- Sources: boards `SettingsMore`, `Settings`.

#### WLD-68 · 지도 · 카드 모션 — P1 · needsExternal: no

| Action | Motion | Haptic |
| --- | --- | --- |
| Map generation | coastline → land fill → mountains → rivers → forests → towns, ≈ 1.2 s, doubles as progress | — |
| Step re-roll | only the changed layer: old accelerate 160 ms → new 400 ms | — |
| Format switch | coastline fixed, interior crossfade 200 ms | — |
| Child map | zoom on pin decelerate 360 ms; back zoom-out accelerate 240 ms | 확인 |
| Reveal by point | blur 6 px → 0, 320 ms, 60 ms stagger | — |
| Region vertex / close | 1.3× pop → rest; close fills 200 ms | tick |
| Route | per segment draw, distance update, days decelerate 320 ms | tick |
| Stamp | drop with 0.96 press 240 ms, rotate in 15° detents | tick |
| Hex fill | spreads by distance, ≤ 300 ms | — |
| Text → conditions | one by one, phrase underline → row 250 ms | — |
| List ↔ card | thumbnail grows into card 360 ms, text exits first | — |
| Card carousel | snap 320 ms, image parallax 8 % | tick |

- Reduce motion: no movement, zoom, parallax or sweep; 120 ms fades only. Fold: centre/zoom kept, unfold decelerate 360 ms, fold accelerate 240 ms, reduce-motion crossfade 120 ms.
- Sources: planning 지도 · 카드 · 이미지 모션, 접기 · 펼치기; boards `MotionSpec2`, `MMap*`, `MFoldMap`.

#### WLD-69 · 세력 사이 관계 — P2 · needsExternal: no

- Faction-to-faction relations use the same storage shape as character relationships (사이 + each side's 마음); screens later. v1 stores nothing new; data model reserved.
- Sources: memory rules-relations (Faction-to-faction relations: same storage shape, screens later); rule 20 (세력끼리 관계 · 화면은 나중).

#### WLD-76 · 게임 제작 도구가 꺼져 있을 때 · 세계 · 0.2.0 글 (switch surfaces and copy) — P0 · needsExternal: no (0.2.0, G-2)

- The project switch `게임 제작 도구` (G-1) is owned by the library area. World reads it and never writes it.
- **World surfaces while it is off.** Each is hidden and its data kept; turning the switch on shows them as in 0.1.0.

| Surface | While off | Stored data |
| --- | --- | --- |
| Place `배경 에셋` row: map sheet, side card, card `개요` (WLD-09) | hidden | asset → place links kept |
| Impact sheet counts of background-asset links (WLD-14) | left out; the links still turn dotted as before, out of sight | kept |
| Term card rows tagged `게임 텍스트` (profile voice lines and archive documents, WLD-19) and their count in `원고 밖 · 메모 · N` | not listed and not counted | links kept |
| Asset creation in U-15 (the `연출` asset picker) | not offered (script area, U-15.4) | — |

- **Not affected** by the switch: the `쓰인 곳` text kinds, `▲`, candidates, the automatic reveal, maps and the timeline. "Game output" in WLD-19 names the text kinds, not the switch.
- **Copy (Kotlin constants; `WorldCopy` unless noted).** Sourced copy is verbatim; `TODO_COPY_…` rows have no source and go to the lane's Requests (DECISIONS §3).

| Constant (suggested) | Copy | Where | Source |
| --- | --- | --- | --- |
| `NEW_PLACE` · `NEW_TERM` · `NEW_FACTION` | `새 장소` · `새 용어` · `새 세력` (with icon `plus`) | list rows, card titles | u15 row 1; icon list row 60 |
| `newFor(kind, q)` | `‘<q>’ 새 장소` (likewise per kind) | picker rows | u15 row 3; quote style DS-72 |
| field labels | `이미지 · 이름 · 상위 지역 · 한 줄 설명` · `이름 · 다른 이름 · 한 줄 설명` · `이미지 · 이름 · 한 줄 설명` | create cards | u15 row 7 |
| `NEW_EVENT` · event fields | `새 사건` · `이름 · 때 · 관련 인물` | 연표 row, card | u15 rows 4, 7 |
| `RELATED_CHARACTERS` | `관련 인물` | event detail row | u15 row 7 |
| `NEW_MAP` (`MapCopy`) | `새 지도` (with icon `plus`) | 지도 tab row | WLD-36 sheet title; icon list row 60 rule |
| `TERM_CANDIDATES` | `용어 후보` | project setting, 용어 tab section `용어 후보 · N` | decisions rows `단순화 · 용어` |
| `candRow(word)` | `용어 후보 · 등불` + actions `잇기` · `아님` | `참고` row (rendered by the script area) | board `scr_core.py` `cand_row` |
| `TODO_COPY_WORLD_CANDIDATES_SUB` | none | setting sub | no source |
| `TODO_COPY_WORLD_REVEAL_AUTO` | none | `공개 시점 옮기기` row that returns to automatic | no source |
| `TODO_COPY_WORLD_REVEAL_AUTO_MARK` | none | mark after an automatic `첫 공개` value | no source |

- Sources: scope G-2, G-6, D33; decisions row `방향 · 게임 제작 기능` (planning).

---

## 3. Data

Common to every entity below (file format §4, §7): `key: ULID` (permanent, generated on device, never reused), `orderKey: String` (fractional index for manual order, where ordering applies), `updatedAt: Instant`, `updatedByDevice: String`, `ext: Map<String, Json>?` (extension slot). All text NFC. Coordinates on maps are `Float` in [0, 1] relative to the base (x right, y down). References are always by key, never by name, number or position. Derived values (counts, usage, candidates, ages, `N일째`, distances, appearances) are never stored.

### 3.1 Entities owned here

**Term** (`world.json › terms[]`)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| key, orderKey | ULID, String | no | |
| name | String | no | 이름; ≥ 1 char |
| aliases | List<String> | no (may be empty) | 별칭, in author order |
| description | RichText (runs) | no (may be empty) | 설명; may hold `@` link runs |
| image | ImageRef | yes | 16:9 slot (preset name like `preset:ash-dawn` or content hash + crop numbers + holo flag) |
| related | List<ItemRef> | no | 관련: refs to Term / Place / Faction |
| textSearch | Boolean | no (default true) | 글자 찾기 |
| hidden | Boolean | no | item hiding (rule 19) |
| fieldValues | Map<FieldKey, FieldValue> | no | project fields (engine owned by cast) |
| rejections | List<TermRejection> | no | `아님` records (see below) |

**TermRejection** (`아님`, inside its Term)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| key | ULID | no | |
| containerKind | enum `BLOCK · SCENE_TRANSITION` | no | where the candidate was |
| containerKey | ULID | no | block permanent key, or scene key for `전환 문구` |
| optionIndex | Int | yes | choice option line index inside the block |
| matchedText | String | no | NFC text of the candidate span |
| occurrence | Int | no | 0-based index among this term's candidates with the same matchedText in the same container |
| createdAt | Instant | no | |

**FirstReveal** (`◇ 첫 공개`; proposed in `changes.json` as kind `firstReveal`, Q-26)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| key | ULID | no | |
| termKey | ULID | no | one per term |
| anchor | Anchor | no | `BLOCK(blockKey)` or `POINT(unitKey, unitKind: CHAPTER · EPISODE · SCENE)`; engine owned by SCR / rule 12 |
| originalBlockKey | ULID | yes | reference only: mirrors SCR's anchor model; set while `근거 지워짐` (anchor moved), cleared on re-attach |
| state | enum `NORMAL · ORPHANED · LEFT_AS_IS` | no | reference only: mirrors SCR's anchor states (`근거 지워짐`, `이대로 두기`); use SCR's type if it defines one |

**(0.2.0)** A `FirstReveal` record is always hand-set. The automatic reveal (WLD-72) is derived from the link index and never stored, so no record is ever written for it.

**Place** (`world.json › places[]`)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| key, orderKey | | no | |
| name | String | no | |
| description | RichText | no | |
| image | ImageRef | yes | 16:9 |
| parentPlaceKey | ULID | yes | 지역 / 소속 (Q-03) |
| hidden | Boolean | no | |
| fieldValues | Map | no | project fields (e.g. 기후, 인구, 상태 with ◆) |

**Faction** (`world.json › factions[]`)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| key, orderKey | | no | |
| name | String | no | |
| description | RichText | no | |
| image | ImageRef | yes | |
| hidden | Boolean | no | |
| fieldValues | Map | no | project fields (e.g. 수장, 규모) |

**WorldTime** (`project.json › worldTime`)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| mode | enum `YEAR_ONLY · CALENDAR · NONE` | no | `연도만 · 세계 달력 · 시간 없이` |
| storyStartYear | Int | yes (null when NONE) | 이야기가 시작되는 해 (e.g. 1200) |
| calendar | WorldCalendar | yes | present when mode = CALENDAR (file format: calendar definition is stored in the file) |

**WorldCalendar**

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| months | List<{name: String, days: Int}> | no (≥ 1) | 달 이름 · 한 달의 날 수 |
| weekdays | List<String> | no (may be empty) | 요일 |
| storyStart | {month: Int, day: Int} | yes | Q-13 |
| storyStartWeekday | Int | yes | Q-13 |

**TimelineEvent** (`world.json › events[]`)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| key, orderKey | | no | orderKey breaks ties at equal time |
| kind | enum `WORLD · STORY` | no | 세계 사건 · 이야기 사건 (Q-15) |
| title | String | no | |
| description | RichText | no | |
| worldDate | {year: Int, month: Int?, day: Int?} | yes | WORLD only |
| sceneKey | ULID | yes | STORY only; time = scene's resolved time |
| place | PlaceOrRegionRef | yes | 장소 |
| relatedTerms | List<ULID> | no | 관련 용어 |
| revealPoint | PointRef | yes | 공개 시점 (`2장에서 공개 예정`) |
| hidden | Boolean | no | `[PROPOSED]` follows item hiding of linked items only |
| relatedCharacterKeys | List<ULID> | no (default empty) | **(0.2.0, additive)** 관련 인물 (WLD-74) |

**WorldMap** (`maps/<mapKey>.json`)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| key, orderKey | | no | |
| name | String | no | |
| parent | {mapKey, pinKey} | yes | nesting |
| baseKind | enum `GENERATED · IMAGE · SKETCH · NODE` | no | |
| baseImage | AssetHash | yes | IMAGE, SKETCH (corrected image), GENERATED (rendered base) |
| baseSize | {w: Int, h: Int} | yes | pixel size of the base |
| sketch | {photo: AssetHash, corners: 4 × Point} | yes | SKETCH only |
| format | enum `HEX · POLY · DETAIL · OUTLINE · NODE` | yes | GENERATED / NODE |
| style | enum `SOFT · INK · COLOR · PARCHMENT` | yes | 색면 · 먹선 · 색 · 양피지 |
| contours | Boolean | no | 등고선 |
| scale | MapScale | yes | |
| layers | List<MapLayer> | no | |
| pins, regions, routes, labels, stamps, drawnLines | Lists below | no | overlay data |
| hex | HexGrid | yes | HEX format |
| poly | PolyMap | yes | POLY format |
| nodes, nodeLinks | Lists | no | NODE format |
| generation | GeneratedMapRecord | yes | GENERATED |
| revealViewDim | Boolean | no | `숨은 것 흐리게` chosen `[PROPOSED]` |

**MapScale**: `a: Point`, `b: Point`, `value: Double`, `unit: enum DAY · KM · LI · CUSTOM`, `travelModeKey: ULID?` (for DAY). Derived km per unit.

**MapLayer**: `key`, `name: String`, `visible: Boolean`, `revealPoint: PointRef?`, `orderKey`.

**Pin**: `key`, `placeKey: ULID` (not null), `x, y: Float`, `layerKey?`, `revealPoint?`, `childMapKey?`. Unique (mapKey, placeKey) `[PROPOSED]`.

**Region**: `key`, `name: String`, `kind: String` (Q-17), `factionKey?`, `placeKey?`, `color: String` (token name or hex), `layerKey?`, `revealPoint?`, `vertices: List<Point>` (≥ 3; this value carries ◆ history through the shared change engine, field id `shape`).

**Route**: `key`, `points: List<RoutePoint>` (each `{x, y}` or `{pinKey}`), `layerKey?`, `revealPoint?`, `shownModeKey: ULID?`.

**MapLabel**: `key`, `text`, `x, y`, `curve: List<Point>?`, `layerKey?`, `revealPoint?`.

**Stamp**: `key`, `kind: enum MOUNTAIN · HILL · FOREST · CITY · TOWN · CASTLE · RUIN · TOWER · BRIDGE · PORT · CAVE · MARKER` (산 · 언덕 · 숲 · 도시 · 마을 · 성 · 폐허 · 탑 · 다리 · 항구 · 동굴 · 표지석), `x, y`, `size: 1 | 2 | 3`, `rotation: Int` (multiple of 15), `placeKey?`, `layerKey?`, `revealPoint?`, `attachedLayer: enum RIVER · ROAD · NONE` (for re-roll guard).

**DrawnLine** (detailed terrain rivers and roads): `key`, `kind: RIVER · ROAD`, `points: List<Point>`, `origin: GENERATED · AUTHOR`, `editedByAuthor: Boolean`.

**HexGrid**: `cellKm: Double` (24), `orientation: POINTY · FLAT`, `cols: Int`, `rows: Int` (32 × 24), `coordEvery: Int` (3), `cells: terrain per cell` (generated result), `overrides: Map<CellId, Terrain>` (author paints). Terrain enum `SEA · PLAIN · FOREST · HILL · MOUNTAIN · DESERT · SWAMP · SNOW` (+ `DEEP`, `PEAK` from generation).

**PolyMap**: `cells: List<{site: Point, land: Boolean}>`, `cellRegion: Map<CellIndex, ULID>`, `provinces: List<{key, name, color, factionKey?}>`.

**NodeMap node / link**: node `{key, name, sub: String?, placeKey?, x, y, revealPoint?}`; link `{key, aNodeKey, bNodeKey, label: String?, revealPoint?}`.

**GeneratedMapRecord**

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| generatorVersion | String | no | e.g. `gen-1` |
| kind | enum `CONTINENT · TWIN · ISLANDS · REGION` | no | 대륙 · 두 대륙 · 섬 무리 · 한 지역 |
| landRatio | enum `SEA_MORE · HALF · LAND_MORE` | no | 바다 많음 · 반반 · 땅 많음 |
| steps | List<{step: COAST · MOUNTAIN · RIVER · FOREST_CLIMATE · TOWN_ROAD, seed: Int, locked: Boolean, choices: Map<String, String>}> | no | 5 entries in order |
| result | {coastline: polygons, heights?: grid, hexCells?, provinces?, rivers, towns, roads, stamps} | no | the baseline |
| history | List<{createdAt, steps, result snapshot ref}> | no | candidates and pre-reroll layers |
| conditions | List<TextCondition> | yes | 글로 설명 |
| textModel | {name, version, onDevice: Boolean} | yes | record only |

**TextCondition**: `key`, `what: String`, `where: Set<Int>` (0..8 of the 3×3 grid), `traits: List<String>`, `placeKey: ULID?`, `status: APPLIED · NEEDS_CHECK`, `userPosition: Set<Int>?`, `sourceSpan: {start, end}?` (phrase in the input text).

**TravelMode** (`project.json › travelModes[]`): `key`, `name: String` (도보 · 말 · 배 · custom), `kmPerDay: Double`, `movingHoursPerDay: Double`, `orderKey`. Defaults: 도보 15/8, 말 40/8, 배 30/24.

**TermMatching** (`project.json › termMatching`): `extraParticles: List<String>` (project additions to the built-in list).

**(0.2.0) Project setting `termCandidates`**: Boolean, default `true` (WLD-73), additive. It lives with the other project settings in `core:model` `ProjectSettings` (owned by the library lane, a Request), so it travels in `.civilight`. A 0.1.0 file without it reads `true`. The 0.1.0 script display toggle `용어 후보` (WLD-27) is no longer read; its stored value is kept.

**(0.2.0) Additions to the term and place create paths** need no new field: the create card writes `aliases`, `image`, `description` and `parentPlaceKey`, which already exist.

**App settings (device-local, not in `.civilight`)**: `defaultMapStyle` (SOFT), `textMapEngine` (ON_DEVICE · CLOUD), `largeMapExport` (K4 · K8), remembered world tab and list/card view per screen, last map viewport.

### 3.2 Relations to entities owned by other areas

| From | To (owner) | Notes |
| --- | --- | --- |
| Term link run (`linkId`, targetKey) | Block / page / card text runs (SCR and others) | WLD indexes them; `맞음` keys on `linkId` |
| FirstReveal.anchor | Block, Scene, Episode, Chapter (SCR, story) | anchor engine of rule 12 |
| TermRejection.containerKey | Block / Scene (SCR) | permanent keys; survives renumbering |
| `맞음` record (check center) | linkId | removed when the link is removed |
| Scene place / region (장소와 때) | Place.key / (WorldMap.key, Region.key) | deleted place → dotted reference (rule 20) |
| Character `연결` field (cast) | Faction / Place / Term | reverse list on WLD cards |
| `◆` change records (cast / lens engine) | Term / Place / Faction fields, Region.shape | anchor may be a block, a point, or `EVENT(eventKey)` |
| Asset (SCR asset list) → `placeKey` | Place | count `배경 에셋` |
| Canvas nodes (canvas) | Term / Faction / TimelineEvent | |
| Recorder `연결` / `카드에 넣기` (recorder) | Term / Place / Faction | |
| Character `기본 이동 수단` field (cast) | TravelMode | travel check |
| Travel segment mode / `맞음` (장소와 때) | TravelMode | |
| Trash entries (storage) | Term / Place / Faction / TimelineEvent / pins / FirstReveal | filter `세계` |

### 3.3 What goes into `.civilight`

| File | Content from this area |
| --- | --- |
| `project.json` | `worldTime` (with the calendar definition), `travelModes`, `termMatching.extraParticles`, field definitions for 장소 · 용어 · 세력 (with the cast area's definitions) |
| `world.json` | `places[]`, `terms[]` (with `aliases`, `textSearch`, `rejections[]`), `factions[]`, `events[]` |
| `changes.json` | `◆` records on world fields and region shapes (incl. `EVENT` anchors); `firstReveal` records (proposed, Q-26) |
| `maps/<지도 키>.json` | one map: base info, layers, pins, regions, routes, labels, stamps, drawn lines, hex / poly / node data, generation record with result, seeds, choices, generator version, history, text conditions |
| `assets/<hash>.<ext>` | map base images, sketch photos, card images (originals; crops as numbers) |
| manifest `features` | `maps.gen` (optional), `fields.custom` (optional); a reader that lacks `maps.gen` still shows the stored base image and overlays |

Not stored: usage counts, candidate lists and the candidate index, `▲` lists, ages, `등장`, `배경 에셋` counts, distances, durations, track and place-time lists, check results other than `맞음` and `아님`.

**(0.2.0)**
- The automatic first reveal is never stored (WLD-72).
- `events[]` entries may carry `relatedCharacterKeys`, and `project.json` carries `termCandidates`.
- Hidden map data (node, generated and sketch maps, routes, scales, travel modes, text conditions) and the `게임 제작 도구` value are always written, on or off (scope §8, G-4).
- A 0.1.0 file opens unchanged.

Trash is not exported unless the export option `휴지통도 포함` is chosen (storage area). Deleted items keep a tombstone `{key, deletedAt}` for 30 days and a name tag after permanent delete (`{key, name, kind}`) so dotted references can print `영구 삭제 · <이름>`.

---

## 4. Rules as test cases

All cases are JVM unit tests against pure functions (matcher, usage/reveal evaluator, rejection store, duration/distance, timeline formatter, generator properties). Text offsets are 0-based UTF-16 indices. `[PROPOSED]` in a case means the expected value follows a proposed default and must be updated if the open question is decided otherwise.

### 4.1 Fixture F (「잿빛 새벽의 등불」, rule 21)

Manuscript order (chapter › episode › scene › block). 집필 선 = end of 1-7 씬 3. Lens = `최신 · 1-7`.

| Id | Place in order | Kind · speaker | Text (`[…]` = link run → T1) |
| --- | --- | --- | --- |
| b12 | 1-2 씬 1 | dialogue · 리엔 | `등불 속 그 돌, 오리지… 뭐라고 했죠?` (all plain) |
| b32 | 1-3 씬 2 | dialogue · 세라 | `빛돌 하나면 한 달은 버텨.` |
| b42 | 1-4 씬 2 (회상 · 1195) | dialogue · 세라 | `[오리지늄]은 타지 않아. 빛만 남지.` (linkId l42) |
| b61 | 1-6 씬 1 | dialogue · 오웬 | `하층에서 빛돌을 판다고?` |
| b7342 | 1-7 씬 3 #042 | dialogue · 리엔 | `저기… 불빛이 보여요. 누가 아직 남아 있는 걸까요?` |
| b7343 | 1-7 씬 3 #043 | dialogue · 하르트 | `멈추지 마. 저건 등불이 아니야.` |
| b7344 | 1-7 씬 3 #044 | narration | `바람이 잿더미를 쓸고 지나갔다. 꺼진 줄 알았던 [오리지늄 등불]이 한 번 깜박였다.` (linkId l44) |
| b7345 | 1-7 씬 3 #045 | dialogue · 리엔 | `(숨을 삼키며)` paren run + `하르트 씨, 방금…` |
| pg1 | page `하층 구역의 빛` (outside manuscript) | page text | `대정전 이후 하층 구역에서 빛은 곧 권력이다. [오리지늄 등불]을 가진 쪽이 거리를 가진다.` (linkId lpg) |

Term T1 = `오리지늄 등불`, `글자 찾기` on, `첫 공개` = BLOCK(b7344).
- **State A**: aliases = [`오리지늄`].
- **State B**: aliases = [`오리지늄`, `빛돌`].

No other term exists in Fixture F unless a case adds one.

### 4.2 Usage, candidates and `▲` (WLD-19 … WLD-23, WLD-28)

**WLD-T01 · Card in state A.** Given Fixture F, state A. When the term card of T1 is built. Then: header `쓰인 곳 · 2 · 후보 1`; group `쓰인 곳 · 2` = [b42 with marker `▲ 공개(1-7) 전 사용`, b7344 with marker `◇ 첫 공개`]; group `후보 · 1 · 확인 안 함` = [b12, kind `끊김`, matchedText `오리지`, span [10, 13)]; group `원고 밖 · 메모 · 1` = [pg1, label `페이지 · 하층 구역의 빛`] with no `▲` (a page is not game output, even though no reveal precedes it); b32 and b61 are not candidates (`빛돌` is not a pattern yet).

**WLD-T02 · List row in state A.** Given state A. When the term list is built at lens 최신 · 1-7. Then T1 is in section `공개됨 · 최신 · 1-7` with right column line 1 `첫 공개 1-7 · ◇ 후보 1` and line 2 `▲ 앞에서 1회 사용`.

**WLD-T03 · Check rows in state A.** Given state A. When the check center collects `용어 공개`. Then exactly two rows: (1) `▲ 공개 전 사용 · 1-4 씬 2` with `‘오리지늄 등불’ 첫 공개 1-7 · 사용 1-4` and buttons `원고로 가기 · 공개 시점 옮기기 · 맞음`; (2) `◇ 공개 전 후보 · 1-2 씬 1 · 리엔: “등불 속 그 돌, 오리지… 뭐라고 했죠?” · ‘오리지늄 등불’ 끊김 · 첫 공개 1-7` with buttons `원고로 가기 · 잇기 · 아님`. The script module badge counts `▲ 공개 전 사용 1` (the `◇` is not in it); the check center `전체` count includes both rows.

**WLD-T04 · 잇기 on the cut-off candidate.** Given state A. When `잇기` is applied to the b12 candidate. Then b12 holds a new link run over [10, 13) with text `오리지` → T1 (new linkId); the card header becomes `쓰인 곳 · 3`; the list shows `첫 공개 1-7` and `▲ 앞에서 2회 사용` (no `◇ 후보`); the check row for b12 becomes `▲ 공개 전 사용 · 1-2 씬 1` with `원고로 가기 · 공개 시점 옮기기 · 맞음`; the module badge counts 2.

**WLD-T05 · 맞음 after 잇기.** Given the result of WLD-T04. When `맞음` is chosen on the b12 `▲` row. Then a `맞음` record keyed by b12's new linkId exists and the row is listed under `맞다고 표시한 것`. `[PROPOSED, Q-07]` The active `▲` count returns to 1 and the list shows `▲ 앞에서 1회 사용`.

**WLD-T06 · 아님 on the cut-off candidate.** Given state A. When `아님` is applied to the b12 candidate. Then a rejection `{containerKind: BLOCK, containerKey: b12, termKey: T1, matchedText: "오리지", occurrence: 0}` is stored; card header `쓰인 곳 · 2`; list line 1 `첫 공개 1-7` without `◇ 후보`; the `◇ 공개 전 후보` check row is gone; card `⋯ › 아님으로 둔 것` lists one row.

**WLD-T07 · Rejection survives other edits.** Given WLD-T06. When b12's text becomes `등불 속 그 돌, 오리지… 뭐였죠?`. Then no candidate is shown for b12 and the rejection still exists.

**WLD-T08 · Rejection released when the matched text changes.** Given WLD-T06. When b12's text becomes `등불 속 그 돌, 오리… 뭐라고 했죠?`. Then a new candidate appears (kind `끊김`, matchedText `오리`, span [10, 12)) and the old rejection record is deleted.

**WLD-T09 · Rejection survives block renumbering and move to another episode.** Given WLD-T06. When b12 is moved to episode 1-3 (its number changes). Then the candidate is still suppressed (the record uses the permanent key).

**WLD-T10 · Adding an alias (state A → B).** Given state A. When alias `빛돌` is added. Then the matcher is rebuilt and only T1 is rescanned; the card shows `후보 2 새로 찾음` next to `별칭`; header `쓰인 곳 · 2 · 후보 3`; candidates = [b12 `오리지` 끊김, b32 `빛돌` (followed by a space), b61 `빛돌` (followed by particle `을`)]; no link is created.

**WLD-T11 · 모두 잇기.** Given state B. When the sheet `모두 잇기` opens. Then title `후보 3 · 오리지늄 등불`, rows (all checked) `1-2 씬 1 · 리엔 · 끊김`, `1-3 씬 2 · 세라 · 별칭 빛돌`, `1-6 씬 1 · 오웬 · 별칭 빛돌`, footer `고른 3개 · 넣으면 ▲ 공개 전 사용 1 → 4`, button `넣기 · 3개`. When `넣기 · 3개` is pressed. Then three link runs exist; header `쓰인 곳 · 5`; `▲` count 4. When undo is pressed once. Then all three links are removed and the state equals state B before the sheet.

**WLD-T12 · 모두 잇기 with one row unchecked.** Given the sheet of WLD-T11. When the b61 row is unchecked. Then footer `고른 2개 · 넣으면 ▲ 공개 전 사용 1 → 3` and button `넣기 · 2개`.

**WLD-T13 · 모두 아님.** Given state B. When `모두 아님` → `넣기 · 3개`. Then three rejections exist (b12 `오리지` 0, b32 `빛돌` 0, b61 `빛돌` 0) and the header is `쓰인 곳 · 2`.

**WLD-T14 · 링크 안 글자는 후보가 아님.** Given state A. Then b42's linked `오리지늄` is not a candidate and b7344's linked span is not a candidate.

### 4.3 Matcher (WLD-21)

Unless stated, term T1 (`오리지늄 등불`, alias `오리지늄`) and each text is one narration block placed after b7344.

| Id | Text | Expected candidates for T1 |
| --- | --- | --- |
| WLD-T15 | `오리지늄은 타지 않아.` | 1 · FULL · `오리지늄` [0, 4) |
| WLD-T16 | `오리지늄을 쥐었다.` / `오리지늄이 빛났다.` | 1 · FULL · `오리지늄` each |
| WLD-T17 | `꺼진 오리지늄등불이 깜박였다.` | 1 · FULL · `오리지늄등불` [3, 9) (name wins over alias) |
| WLD-T18 | `꺼진 오리지늄 등불이 깜박였다.` | 1 · FULL · `오리지늄 등불` [3, 10) |
| WLD-T19 | `하층오리지늄은 비싸다.` | none (not at word start) |
| WLD-T20 | `오리지니움은` / `오리지늄스러운 빛` | none |
| WLD-T21 | `“오리지늄은 타지 않아.”` / `(오리지늄)` | 1 each (quote / bracket boundary; punctuation after) |
| WLD-T22 | `오리지늄에서는` / `오리지늄으로도` / `오리지늄이라는` | 1 each (two chained particles) |
| WLD-T23 | `오… 그거?` | none (1-syllable cut-off) |
| WLD-T24 | `오리지늄의 빛.` | 1 (particle `의`, then space) |
| WLD-T25 | text in NFD (decomposed jamo) of `오리지늄은` | 1 · matchedText stored in NFC `오리지늄` |

**WLD-T26 · 1-syllable term.** Given a term `빛` (no aliases). When text `빛은 꺼졌다.` is scanned. Then no candidate; the card shows `1음절 · 글자 찾기 안 함`; links to `빛` are still counted.

**WLD-T27 · Overlap, longest wins.** Given terms T1 and T7 = `등불`. When text `꺼진 오리지늄 등불이` is scanned. Then one candidate for T1 [3, 10) and none for T7. When text `등불을 들고 나갔다.` is scanned. Then one candidate for T7 [0, 2).

**WLD-T28 · Homonym threshold.** Given T7 `등불` with 10 rejections. Then no chip. When an 11th rejection is added. Then the card shows chip `글자 찾기 끄기`; `글자 찾기` stays on until the chip is tapped.

**WLD-T29 · Text kinds.** Given T1. Then: author memo `⌁ 오리지늄은 나중에` → none; direction block text containing `오리지늄` → none; choice option line `오리지늄을 건넨다` → 1 candidate tagged `선택지`; speaker display name `오리지늄` → none; paren run `(오리지늄을 보며)` inside dialogue → none; character profile or page text → none (links there go to `원고 밖 · 메모`).

**WLD-T30 · Hidden term.** Given T1 hidden and the hidden lock closed. Then no candidates are computed and the card/list show no candidate counts. When unlocked. Then WLD-T01's candidates appear.

**WLD-T31 · 글자 찾기 off and on.** Given WLD-T06 (one rejection). When `글자 찾기` is turned off. Then candidates of T1 = none, card shows `글자 찾기 꺼짐 · 켜기`, the rejection remains. When turned on. Then candidates are recomputed and b12 is still suppressed.

**WLD-T32 · Pending defaults `[PROPOSED, Q-04]`.** `오리지늄이었다` → none (a); `오리지늄…` → 1 · FULL (b); `오리지...` (ASCII) → none (c).

### 4.4 Reveal comparison (WLD-18, WLD-20)

**WLD-T33 · Same block counts as after.** Given state A. Then l44 inside b7344 is not `▲`.

**WLD-T34 · Flashback compared by manuscript order.** Given term T2 `대정전` with `첫 공개` = BLOCK(b41), b41 placed in 1-4 씬 2 before b42, and links to T2 in b42 and in a block of 1-3 씬 2. Then the b42 link is not `▲` (after the reveal in manuscript order although the scene is a 1195 flashback) and the 1-3 씬 2 link is `▲`.

**WLD-T35 · No first reveal.** **Superseded (0.2.0):** with WLD-72, T8 has the automatic reveal BLOCK(the 1-2 씬 1 block), no `◇ 첫 공개 없음` row and no chip; see WLD-T73 … T78. The 0.1.0 expectation follows for reference. Given term T8 without `첫 공개` and links in 1-2 씬 1, 1-4 씬 2, 1-6 씬 1. Then no `▲`; the card shows `첫 공개 없음 · 쓰인 곳 3` and chip `1-2 씬 1을 첫 공개로`; the check center has `◇ 첫 공개 없음` for T8. When the chip is tapped. Then `첫 공개` = BLOCK(the 1-2 씬 1 block holding the first link) and no link is `▲`.

**WLD-T36 · Reveal planned beyond the 집필 선.** Given term T4 `잿불의 서약` with `첫 공개` = POINT(episode 2-5) and links in 1-5, 1-6 and 1-7. Then all three are `▲`; the check center shows one grouped row `잿불의 서약 · ▲ 3` that expands into three rows; the list shows T4 in `아직 공개 전` with `공개 예정 2-5`.

**WLD-T37 · Reveal block deleted.** Given state A. When b7344 is moved to trash. Then the reveal record's anchor becomes b7345, state ORPHANED, originalBlockKey = b7344; l44 is no longer counted (block in trash), so the header is `쓰인 곳 · 1 · 후보 1`; the b42 `▲` row shows dotted `◇ 근거 지워짐`. When b7344 is restored. Then the anchor returns to b7344, state NORMAL, and WLD-T01 holds again.

**WLD-T38 · Choice option line.** Given a choice block c15 in 1-5 with option lines `#045-1`, `#045-2`. When the author tries to set `첫 공개` of T1 on `#045-1`. Then it is refused (no record). Given a link to T1 in `#045-2`. Then it is usage, tagged `선택지`, and `▲` (before 1-7).

**WLD-T39 · Moving the reveal block.** Given state A plus link l47 → T1 in a block b7347 after b7344 in 1-7 씬 3. When b7344 is moved to the start of 1-8 씬 1. Then l44 is still not `▲` (same block), l47 becomes `▲`, and the toast reads `◇ 첫 공개 1-7 → 1-8 · ▲ 새로 1`.

**WLD-T40 · Lens dims, counts stay.** Given state A. When the lens is `1-5`. Then on the card the b12 and b42 rows are normal and the b7344 row is dimmed; the header stays `쓰인 곳 · 2 · 후보 1`; the term list puts T1 in `아직 공개 전` `[PROPOSED: right label stays 첫 공개 1-7]`.

**WLD-T41 · 맞음 lifecycle.** Given a `맞음` on l42. When the reveal is moved to 1-8. Then the `맞음` stays. When l42 is unlinked (text kept). Then the `맞음` record is deleted and b42's `오리지늄` becomes a candidate again.

**WLD-T42 · Particle for the suggestion chip.** **Superseded (0.2.0):** the chip is gone (WLD-18). The particle helper may stay for other copy; this case is no longer a product rule. Given first-link locations `1-2 씬 1`, `1-2 씬 2`, `1-6 씬 4`. Then chips read `1-2 씬 1을 첫 공개로`, `1-2 씬 2를 첫 공개로`, `1-6 씬 4를 첫 공개로` (particle by the Sino-Korean reading of the last digit: 0 3 6 7 8 1 → `을`; 2 4 5 9 → `를`).

### 4.5 Delete, hide, persist (WLD-13, WLD-14, section 3.3)

**WLD-T43 · Deleting a term.** Given state A. When T1 goes to trash. Then l42, l44 and lpg render dotted; the reveal record is in trash with T1; the check center shows one `◇ 지운 용어 · 오리지늄 등불` row containing `▲ 1 보류`. When T1 is restored. Then the `▲` returns to the b42 row and the reveal is active. When T1 is permanently deleted instead. Then links read `영구 삭제 · 오리지늄 등불`, the `▲` record is gone, and a name tag `{T1, 오리지늄 등불, 용어}` remains.

**WLD-T44 · Deleting a place.** Given place `잿더미 시장` with a pin on `하층 구역 지도`, a route `결사의 문 → 잿더미 시장`, and scene 1-6 씬 1 using it. When the place goes to trash. Then the pin is in trash; the route stays with a dotted end; scene 1-6 씬 1 shows `지운 장소 · 잿더미 시장` `◇`; `distanceKm(결사의 문, 잿더미 시장)` returns `NO_LOCATION`; `인물 동선` skips 1-6 씬 1. When restored. Then the pin is back at the same 0–1 coordinates and the route end re-attaches.

**WLD-T45 · Hidden place on a map.** Given place `기록실` hidden and the lock closed. Then its pin is not drawn and the place list shows one locked row `숨긴 장소 1` `[PROPOSED label]`.

**WLD-T46 · File round trip.** Given state B after WLD-T06-like rejection on b12. When the project is written to `.civilight` and read back. Then `world.json` T1 has `aliases ["오리지늄","빛돌"]`, `textSearch true`, one rejection; no candidate list or count is in any file; after opening, the check center shows `점검 중` until the full scan ends, then candidates = [b32, b61] (b12 suppressed).

**WLD-T47 · Rescan scope.** Given the index is built. When text in b12 changes and 500 ms pass without input. Then exactly one block (b12) is rescanned. When an alias of T1 changes. Then the trie is rebuilt and only T1's patterns are scanned across the manuscript.

**WLD-T72 · Full scan on open (rule 21 §7).** Given state B and the project closed (the candidate index is app-only, WLD-26). When the project is opened, or restored from a `.civilight` file. Then a full scan runs in the background; until it ends the check center shows `점검 중` and its candidate numbers (`◇ 공개 전 후보`) are dimmed [PROPOSED: the card's `후보 N` is dimmed the same way]; the usage count and the `▲ 공개 전 사용` count are final at once (they come from links, not candidates). When the scan ends. Then the dimming is gone and every candidate count equals the count before the project was closed; `아님` records are unchanged.

### 4.6 Time, timeline, ages (WLD-31 … WLD-35)

**WLD-T48 · Ages.** Given 리엔 출생 1183, 세라 출생 1179, 하르트 출생 `알 수 없음`, mode `연도만`. Then at 1195: `리엔 12세 · 세라 16세`, 하르트 `??세`; at 1200: `리엔 17세`.

**WLD-T49 · Story-start view labels.** Given resolved story days (from 장소와 때; day numbers since story start): 1-2 씬 1 = 2, 1-3 씬 2 = 3, 1-6 씬 1 = 8~12, 1-7 씬 1 = 10~14, 1-7 씬 3 = 11~15. When the view is `이야기 시작부터`. Then labels `2일째`, `3일째`, `약 10일째 (8~12)`, `약 12일째 (10~14)`, `약 13일째 (11~15)`.

**WLD-T50 · Anchor view.** Given WLD-T49 and story event `리엔, 결사 합류` on 1-3 씬 2, world event `대정전` 1195, story start 1200. When the view is `기준 사건 · 결사 합류`. Then `대정전` → `−5년`, 1-2 씬 1 → `−1일`, 1-3 씬 2 → `기준`, 1-6 씬 1 → `+약 7일` with `5~9일`, 1-7 씬 1 → `+약 9일`, 1-7 씬 3 → `+약 10일`.

**WLD-T51 · Mode 시간 없이.** Given world time mode `NONE`. Then the `연표` module is not offered and events cannot be dated `[PROPOSED: module hidden]`.

**WLD-T52 · Calendar arithmetic `[PROPOSED]`.** Given months `[가달 30, 나달 31, 다달 30, 라달 29]` (year = 120 days) and weekdays `[해, 달, 별]` with the story start = 가달 1일 on `해`. Then day offset 45 → 나달 16일, weekday `해` (45 mod 3 = 0); day offset 120 → next year 가달 1일.

### 4.7 Maps (WLD-43 … WLD-66)

**WLD-T53 · Route durations.** Given a route of 38 km. Then 도보 (15 km · 8시간) = 3,136 minutes → `2일 4시간`; 말 (40 km · 8시간) = 456 minutes → `약 7시간 40분`; 배 (30 km · 24시간) = 1,824 minutes → `1일 6시간`. The label never shows decimal days.

**WLD-T54 · Scale from days.** Given A at (0.20, 0.40) on `결사의 문`, B at (0.45, 0.40) on `잿더미 시장` (|AB| = 0.25), value `2`, unit `일`, mode 도보 15 km. Then the scale is 30 km for 0.25, i.e. 120 km per unit; a route of length 0.316667 measures 38 km.

**WLD-T55 · Distance kinds.** Given 세계 지도 with pins `상층` (parent of sub-map `하층 구역`) and `송전탑 폐허`, sub-map pins `하층 거리`, `결사의 문` and `상층 관문`, a route `결사의 문 → 상층 관문` on the sub-map (no route touches `하층 거리`), and scales on both maps. Then `distanceKm(하층 거리, 하층 거리)` = 0 `ZERO`; `(결사의 문, 상층 관문)` = route length `ROUTE`; `(하층 거리, 결사의 문)` = straight `STRAIGHT`; `(하층 거리, 송전탑 폐허)` = straight distance between `상층` and `송전탑 폐허` on 세계 지도; any pair on a node map or a map without scale → `NO_SCALE`.

**WLD-T56 · Reveal by point.** Given pins `결사의 문` (no point), `하층 거리` (1-1), `잿더미 시장` (1-3), `기록실` (1-5), `상층 관문` (1-7), `송전탑 폐허` (2-3) and one region (1-1). When the lens is `1-5`. Then visible = 4 pins and 1 region; the reveal summary reads `보임 · 핀 4 · 영역 1 | 아직 공개 전 · 2`; hidden tags `상층 관문 · 1-7`, `송전탑 폐허 · 2-3`.

**WLD-T57 · Region shape over time.** Given region `상층 경비대` with shape S1 and a `◆` shape S2 from 1-9 (beyond the 집필 선 1-7). Then at `최신 · 1-7` the map draws S1; comparing `1-7 · 1-9 ◆` draws S1 and S2 with S2∖S1 dotted.

**WLD-T58 · 인물 동선.** Given 리엔 in scenes 1-2 씬 1 (하층 거리, day 2), 1-3 씬 2 (결사의 문, day 3), 1-4 씬 2 (하층 거리, 회상 1195), 1-6 씬 1 (잿더미 시장, day 8~12), 1-7 씬 3 (결사의 문, day 11~15), 1-8 씬 1 (no place), and no routes among these places. Then stops = 1-2 → 1-3 → 1-6 → 1-7 (flashback and no-place scenes excluded); all three lines are straight and labelled `추정`; at lens point 1-5 the position chip is at `결사의 문`.

**WLD-T59 · Track order follows time.** Given 1-6 씬 2 with time earlier than 1-6 씬 1 (out of order). Then the track lists 1-6 씬 2 before 1-6 씬 1.

**WLD-T60 · 이곳의 시간.** Given place `하층 거리` with world event `대정전` (1195, place 하층 거리), scenes 1-2 씬 1 (day 2, 리엔), 1-4 씬 2 (회상, 대정전 그날, 세라), 1-7 씬 1 (day 10~14, 리엔 · 하르트), 2-3 씬 2 (planned, 하르트), lens `1-7`. Then rows in order `1195 · 대정전`, `1-4 씬 2 · 세라의 회상`, `1-2 씬 1 · 리엔의 새벽 순찰`, `1-7 씬 1 · 첫 번째 깜박임`, divider `시점 1-7 · 이 아래는 아직`, `2-3 씬 2 · 경비대 검문` dimmed. `[PROPOSED tie rule: at equal time a world event precedes a scene]`

**WLD-T61 · Generator determinism.** Given generator version V, kind `대륙`, `반반`, step seeds (4827, 0112, 0031, 0907, 0215) and fixed chips. When generated twice. Then both results are byte-identical. When only `강` is re-rolled with `해안선` and `산` locked. Then coastline and mountain layers are identical to before and only the river layer differs (property test over many seeds).

**WLD-T62 · Re-roll guard.** Given a started map with 2 author-edited rivers and a `다리` stamp attached to rivers. When `강 다시 만들기` is requested. Then the sheet shows `직접 고친 강 2 · 강에 붙은 도장 1(다리)` with default `고친 강은 남기기 · 나머지만 새로`; after `다시 만들기` the 2 edited rivers are unchanged, the previous river layer is in history, and undo restores it.

**WLD-T63 · Text conditions never invent names.** Given places `결사의 문`, `잿더미 시장` and a reading that yields `숲 · 서쪽 · 결사의 문` and `항구 도시 · 남쪽 해안 · 은빛 항구`. Then the first condition links `결사의 문`; the second keeps 무엇/어디/특징 and has no place link. After generation with 4 conditions of which 3 are met, the result header reads `조건 4 · 3 반영`.

**WLD-T64 · Hex flood fill.** Given a hex grid where 5 connected cells are `숲` and touch other forest only through `평원`. When `채우기` with `평원` is tapped on one of them. Then exactly those 5 cells become `평원`.

**WLD-T65 · Hex coordinates.** Given a 32 × 24 grid. Then cell (row 14, column 9) is labelled `O09`; labels are drawn every 3 cells.

### 4.8 More worked examples (rules 20, 21)

**WLD-T66 · Post-reveal candidate stays on the card.** Given state A plus block b7346 (1-7 씬 3 #046, narration) `꺼진 오리지늄은 다시 빛나지 않았다.` (plain text). Then the card header is `쓰인 곳 · 2 · 후보 2` (candidates: b12 `오리지` 끊김, b7346 FULL `오리지늄` [3, 7)); the list still shows `첫 공개 1-7 · ◇ 후보 1` (only pre-reveal candidates); the check center still has exactly the two rows of WLD-T03 (post-reveal candidates are card-only).

**WLD-T67 · No free continuation.** Given term T7 `등불`. When `등불지기는 밤에 일한다.` is scanned. Then no candidate for T7 (`지기는` is not a particle; rule 21 §3 rejected "any continuation").

**WLD-T68 · Other cut-off marks.** Given T1. When `오리지⋯ 그거`, `오리지— 그거`, `오리지- 그거` are scanned. Then each yields one candidate, kind `끊김`, matchedText `오리지`.

**WLD-T69 · Merging places with pins.** Given places `잿더미 시장` (target) and `시장 골목` (merged), each with a pin on `하층 구역 지도`, and routes `결사의 문 → 시장 골목` and `하층 거리 → 시장 골목`. When `시장 골목` is merged into `잿더미 시장`. Then the target pin stays at its coordinates; the merged pin goes to trash with `시장 골목` (`합침 → 잿더미 시장`); both route ends re-attach to the target pin; the comparison sheet shows the route-end count 2 `[PROPOSED copy, WLD-15]`.

**WLD-T70 · Merging terms with two first reveals.** Given term T9 `빛돌` with `첫 공개` = BLOCK(b32) and links to T9 in b32 and b61, and T1 in state A. When T9 is merged into T1 with the default choice (target's reveal b7344). Then the links in b32 and b61 target T1, both are `▲`, and T1's `▲` count is 3 (b42, b32, b61). When the author picks T9's reveal (b32) in the comparison row instead. Then no link of T1 is `▲` (b32 is the reveal block; b42 and b61 come after). `[PROPOSED, Q-11]` `빛돌` becomes an alias of T1.

**WLD-T71 · Restoring a place whose map was deleted.** Given place `기록실` with a pin on `하층 구역 지도`. When `기록실` goes to trash, then `하층 구역 지도` goes to trash, then `기록실` is restored. Then the place returns; its pin is flagged `◇ 지도 없음` with actions `지도 되살리기 · 핀 다시 놓기`.

### 4.9 0.2.0 cases (WLD-70 … WLD-76)

Fixture F as in §4.1, with one change: **state A0** = state A **without** the stored `첫 공개` record of T1. Every earlier case that uses Fixture F keeps its stored record and so tests the hand-set path. WLD-T35 and WLD-T42, which rely on a term with no record, are superseded. Cases marked "state model" run on the screen's JVM state builder; cases marked "emulator" are checked on the emulator. The project setting `용어 후보` is on unless a case says otherwise.

**WLD-T73 · Automatic reveal.** Given state A0. Then:
- `TermReveals.effective(T1)` = `AUTO`, anchor BLOCK(b42) (1-4 씬 2, the first manuscript link l42; the page link lpg never counts).
- The card row reads `첫 공개 | 1-4 씬 2 · <b42 number>` followed by `TODO_COPY_WORLD_REVEAL_AUTO_MARK`.
- No link of T1 is `▲` (l42 is in the reveal block; l44 comes after it), and the card header is `쓰인 곳 · 2 · 후보 1`.
- The list shows T1 in `공개됨 · 최신 · 1-7` with `첫 공개 1-4 · ◇ 후보 1` and no second line.
- The check center has exactly one `용어 공개` row: `◇ 공개 전 후보 · 1-2 씬 1 · …` ending `첫 공개 1-4` (b12 comes before b42).
- `changes.json` holds no `firstReveal` record for T1.

**WLD-T74 · The automatic reveal follows the links.** Given state A0.
- When l42 is unlinked (text kept) → Then the reveal is BLOCK(b7344) `AUTO`, and b42's `오리지늄` becomes a FULL candidate before the reveal. The list shows `첫 공개 1-7 · ◇ 후보 2`. No toast and no `◇ 근거 지워짐` appear.
- When b42 is moved to the trash instead → Then the reveal is BLOCK(b7344). When b42 is restored → Then BLOCK(b42) again.
- Still no stored record in either case.

**WLD-T75 · A stored record wins, and `자동` removes it.** Given state A (stored BLOCK(b7344)). Then WLD-T01 … WLD-T03 hold (`▲` on b42).
- When the `공개 시점 옮기기` row `TODO_COPY_WORLD_REVEAL_AUTO` is chosen → Then the record is deleted, the state equals WLD-T73, and the `▲` row is gone.
- When undo → Then the record and the `▲` return.

**WLD-T76 · Picking in the sheet makes it hand-set.** Given state A0. When `공개 시점 옮기기` → `에피소드 · 장으로` → `2-5` → Then a stored POINT(2-5) record exists, l42 and l44 are both `▲`, and the check center groups them as `오리지늄 등불 · ▲ 2`.

**WLD-T77 · First link in a choice line.** Given term T10 whose only link is in option line `#045-2` of choice block c15 (1-5). Then the effective reveal is BLOCK(c15), not refused (the option-line ban is for stored records), and the link is not `▲`.

**WLD-T78 · Text kinds that never reveal.** Given term T11 with links only in an author memo, a direction block and a page. Then T11 has no effective reveal and is in `아직 공개 전` with `첫 공개 없음`. No `◇ 첫 공개 없음` check row appears, because the usage is 0.

**WLD-T79 · Candidates off.** Given state A, and `용어 후보` turned off. Then:
- the card header is `쓰인 곳 · 2`, and the card has no `후보` group, no `글자 찾기` row and no `⋯ › 아님으로 둔 것`;
- the list shows `첫 공개 1-7` and `▲ 앞에서 1회 사용` (no `◇ 후보`);
- the check center has only the `▲` row;
- the script gets no candidate spans;
- the 용어 tab has no `용어 후보` section;
- the index is not built;
- T1's `textSearch` and every `아님` record are unchanged.
- When it is turned back on → Then a full scan runs, and WLD-T01 holds again.

**WLD-T80 · Candidate section in the 용어 tab.** Given state B. Then the last section of the 용어 list is `용어 후보 · 1`, with one row `오리지늄 등불` · `후보 3` · `◇ 공개 전 후보 3`. Tapping it opens T1's card at its `후보` group.

**WLD-T81 · `참고` row data.** Given state B and block b61 `하층에서 빛돌을 판다고?`.
- Then WLD gives the script one span `{T1, 오리지늄 등불, matchedText 빛돌, FULL, [5, 7)}`, and the script draws `용어 후보 · 빛돌 · 잇기 · 아님`.
- When `아님` → Then the rejection of WLD-T13's shape is stored for b61, and the span list for b61 is empty.

**WLD-T82 · Create a term in place.** When the `＋ 새 용어` card is filled with `이름 반딧돌 · 다른 이름 반디 · 한 줄 설명 작은 빛을 내는 돌` and `만들기` → Then:
- one term exists with aliases `[반디]` and that description;
- one undo step removes it;
- only that term is rescanned.

The same card with `다른 이름` = `반디, 개똥` stores the single alias `반디, 개똥`, which is never split (WLD-70).

**WLD-T83 · Create from a picker.**
- Given the scene place picker with the query `은빛 항구` and no match → Then the top row's label is `‘은빛 항구’ 새 장소` (icon `plus`). When `만들기` → Then a place `은빛 항구` exists with no pin and is returned as `PlaceRef`.
- Given the merge target picker → Then it has no create row.

**WLD-T84 · Create an event in place.**
- `이름 결사 창립 · 때 1190` makes a `WORLD` event in 1190.
- `때` = scene `1-3 씬 2` makes a `STORY` event on that scene.
- An empty `때` makes a `WORLD` event with no date, listed after the dated world events before the story.
- `관련 인물 리엔 · 세라` is stored as two keys, and the detail row reads `관련 인물 | 리엔 · 세라`.
- With world time `시간 없이` → Then the 연표 is off and no `＋ 새 사건` exists.

**WLD-T85 · Maps (state model; the picker step on the emulator).**
- Given a project from 0.1.0 with a node map, a generated map, a sketch map and an image map with a route and a scale.
- Then the 지도 list shows all four.
- The node, generated and sketch maps open with no `편집`.
- The image map's tool bar is `이동 · 핀 · 영역 · 라벨 · 레이어`, no route or scale bar is drawn, and no `축척 맞추기` is reachable.
- When `＋ 새 지도` → an image is picked → Then an `IMAGE` map named `지도 1` exists and opens. None of the four 0.1.0 maps is named `지도 N`; see the naming rule in WLD-75. When the picker is cancelled instead → Then nothing is created.
- The domain cases WLD-T53 … WLD-T55 still pass (the functions are kept).
- Saving and reloading the project leaves the four 0.1.0 map files unchanged.

**WLD-T86 · Lens on world heads (state model).** Given the lens `1-5` picked on the world head.
- Then the term list puts T1 (state A) in `아직 공개 전`, as WLD-T40.
- The 연표, the map view and 연표 + 지도 read at `최신` and show no lens chip.
- When the project is reopened → Then the world head reads `최신 · 1-7`.

**WLD-T87 · Switch off.** Given `게임 제작 도구` off and a link to T1 in 세라's archive document. Then:
- T1's card has no `게임 텍스트` row, and `원고 밖 · 메모` counts only lpg;
- the place card of `하층 거리` has no `배경 에셋` row.
- When the switch is on → Then both appear, with the same counts as 0.1.0.

**WLD-T88 · Read-only (state model).** Given the example project (read-only). Then no `＋ 새 장소 · ＋ 새 용어 · ＋ 새 세력 · ＋ 새 사건 · ＋ 새 지도` row exists, and no picker shows a create row.

**WLD-T89 · Automatic reveal in a hidden scene.** Given state A0 with scene 1-4 씬 2 hidden and the lock closed. Then:
- the effective reveal is still BLOCK(b42) (no `▲`, same candidates as WLD-T73);
- the card row reads `첫 공개 | 1-4 · 숨긴 씬`, and the list reads `첫 공개 1-4`.
- When unlocked → Then the card row names the scene and block as in WLD-T73.

**WLD-T90 · Copy lint.** Every constant of WLD-76's copy table passes `CopyLint`. Each `TODO_COPY_WORLD_…` is listed under the lane's Requests.

---

## 5. Screens

Components are from the design system (`.nm` scope): `GlassBars` (top/bottom glass, blur 36px; `투명도 줄이기` → opaque `raise` bars), `CommandBar`, `Tabs` (segmented), `LensChip`, `ListRow` (`.li` title + fragment, `.row` field table with `◆`), `Tag` (chips, hit area 44px), `Buttons`, `Controls` (radio round = one choice, checkbox rounded square = many), `Sheet` (cover bottom sheet; background recedes 2px blur), `Toast` (undo), `LockRow`, `SearchHit` (inline box), `ViewToggle`, `ImageTile`, `CropFrame`, `MapToolbar`, `MapMarks`, `MapStyle`, `ConditionRow`, `WhenPicker` and `TravelBar` (consumed from 장소와 때).

| Screen / state | Boards | Cover (390) | Inner (800) | Features |
| --- | --- | --- | --- | --- |
| 세계 · 용어 목록 | `World`, `TourWorld` | full-screen list, tabs, lens, `+`, command bar | layout A `TwoWorld` (list 390 · term card 409); layout B `ColWorld` (one 560 column) | WLD-01, 02 |
| 세계 · 장소 목록 / 세력 목록 | not drawn | as term list `[PROPOSED]` | as term list | WLD-03, 04 |
| 카드 보기 (world lists) | not drawn for world; pattern `MCardView`, `MCarousel` | horizontal card rows | same, wider rows | WLD-05 |
| 용어 카드 | `Term`, right pane of `TwoWorld`, `RTLinkAll` (behind the sheet) | full screen | right pane (layout A) or column (B) | WLD-08, 16, 19–25 |
| 후보 모두 잇기 시트 | `RTLinkAll` | bottom sheet over the card | sheet over the detail pane `[PROPOSED]` | WLD-23 |
| 용어 후보 작은 창 (manuscript) | `RTCandidate` | popover under the tapped span; header chip `표시 · 용어 후보 켬` | same in the manuscript column | WLD-22, 27 |
| 장소 시트 / 장소 카드 | `MapC`, `MapSoftC`, `Map`, `MapSoft`, `PlaceTime`, `MFoldMap` | bottom sheet on the map; full card with tabs `개요 · 이곳의 시간 · 지도` | side card (270) over the map; full card in list/detail | WLD-09, 63 |
| 세력 카드 | not drawn | full screen `[PROPOSED]` | right pane | WLD-10 |
| 항목 숨김 (lists) | `ItemHide` (cast pattern) | `LockRow` line | same | WLD-13 |
| 지우기 영향 · 합치기 비교 | `RCDeleteImpact`, `RCMerge` (cast shells) | bottom sheet | centred sheet | WLD-14, 15 |
| 점검 센터 · 용어 공개 rows | `Check`, `TwoCheck`, `ColCheck` | list rows with button rows | list + manuscript location (A) / column (B) | WLD-28 |
| 연표 | `Timeline`, `TimelineAnchor`, `TwoTimeline`, `ColTimeline` | full-screen line list | A: list · event detail; B: one column | WLD-31, 34, 35 |
| 사건 상세 | right pane of `TwoTimeline` | full screen | right pane | WLD-33 |
| 사건 만들기 sheet | not drawn | bottom sheet `[PROPOSED]` | centred sheet | WLD-32 |
| 연표 + 지도 | `TimelineMap` | not drawn (Q-21) | list left · map right | WLD-64, 65 |
| 지도 목록 · 새 지도 | `MapNew` | card row + bottom sheet `새 지도` | same, wider | WLD-36 |
| 지도 보기 | `MapC`, `MapSoftC`, `Map`, `MapSoft` | full-screen map + bottom sheet; zoom label | map + side card + bottom chips + scale bar | WLD-37, 38, 41 |
| 지도 편집 | `MapEdit`, `MapRouteC`, `TourMap` | tool bar bottom, sheets for properties | tool bar bottom-left, side panel `속성 · 레이어 · 목록`, floating vertex bar | WLD-40–45 |
| 경로 sheet | `MapRouteC`, `MMapRoute` | bottom sheet | side panel | WLD-43 |
| 축척 맞추기 | `MapScale` | bottom sheet + A/B markers | side panel `[PROPOSED]` | WLD-46 |
| 시점별 공개 | `MapReveal`, `MMapReveal` | map + mode chips + point scrubber + summary | same | WLD-49 |
| 하위 지도 | `MMapZoom` | breadcrumb at top | same | WLD-48 |
| 노드 지도 | `MapNode` | full screen, node tool bar | same, wider `[PROPOSED]` | WLD-50 |
| 양식 고르기 | `MapFormat` | 2-column card grid | same, wider | WLD-54, 57 |
| 단계별 만들기 | `MapGen` (cover), `MapGenWide` (inner) | map preview top, step card sheet (`강 · 시드 0031`, chips, `강만 다시 · 강 고정`), `후보 기록 9 · 이 지도로 시작` | map full, side panel with base chips, 5 step rows (seed · 고정 · 다시), step chips, style chips; candidate strip + `새 후보` | WLD-54, 55 |
| 단계 다시 만들기 | `MapReroll`, `MMapReroll` | bottom sheet with two radios | centred sheet | WLD-56 |
| 생성 지형 그림 | `MapSoft`, `MapSoftC`, `MMapFormat`, `MMapGen` | map | map | WLD-57, 68 |
| 육각 칸 칠하기 | `MapHex`, `MMapHexFill` | map + bottom sheet (tools, palette, settings) | not drawn; side panel `[PROPOSED]` | WLD-58 |
| 다각형 지역 | `MapPoly` | not drawn; bottom sheet `[PROPOSED]` | map + side panel `지역 · 레이어` + tool bar | WLD-59 |
| 상세 지형 도장 | `MapDetail`, `MMapStamp` | not drawn; bottom sheet `[PROPOSED]` | map + side panel `도장 · 범례 · 레이어` + tool bar | WLD-60 |
| 글로 설명 · 결과 | `MapText`, `MapTextResult`, `MMapText` | input sheet with condition list; result map + condition sheet | same, side panel `[PROPOSED]` | WLD-61 |
| 인물 동선 | `MapTrack` | not drawn (Q-21) | map + side panel `동선 · 장소 · 레이어` + episode scrubber | WLD-62 |
| 이곳의 시간 | `PlaceTime` | place card tab | right pane `[PROPOSED]` | WLD-63 |
| 장소 고르기 | `SceneWhere`, `BulkPlace`, `BulkWide` | sheet list + `지도에서 고르기 ›` | table + map with scene counts | WLD-52 |
| 설정 · 지도 · 이미지 · 연표 | `SettingsMore`, `Settings` | settings list | settings in column | WLD-67 |
| 프로젝트 설정 (세계 시간 · 이동 수단 · 인물 항목 tabs) | `ProjSettings`, `RCFields`, `RCFieldAdd`, `NewTime` | settings list | same | WLD-06, 29, 30, 47 |
| 모션 규칙 · 지도 · 카드 | `MotionSpec2`, `MMap*`, `MCardView`, `MCarousel`, `MFoldMap` | — | — | WLD-68 |
| 이미지 규격 | `ImageSlots`, `ImagePick`, `ImageCrop` | — | — | WLD-07 |

**(0.2.0) Screens.**
- 세계 lists: no `+`, a create row on top, the small lens chip.
- 용어 list: the `용어 후보 · N` section last.
- 용어 후보 작은 창: superseded by the `참고` row (script area).
- 연표 and map screens: no lens chip.
- Hidden map screens (경로, 축척, 노드 편집, 양식, 단계별 만들기, 단계 다시 만들기, 육각, 다각형, 상세 지형, 글로 설명, 인물 동선) are unreachable (WLD-75).

| Screen / state (0.2.0) | Boards | Cover (390) | Inner (800) | Features |
| --- | --- | --- | --- | --- |
| 만들기 카드 · 장소 · 용어 · 세력 · 사건 (list and picker modes) | `scr_icon_variants.py` (row only) | centred floating card over a dimmed list, above the IME | centred card | WLD-70, 74 |
| 용어 후보 section | none | last list section | same in the list pane | WLD-73 |
| 참고 · 용어 후보 row (data only; the script renders it) | `scr_core.py` `cand_row` | top of `참고` | same | WLD-73 |
| 새 지도 (image picker) | none | system picker | same | WLD-75 |
| 지도 보기 · 보기 전용 (node, generated, sketch) | `MapNode`, `MapSoft`, `Map` minus the edit controls | full-screen map, no `편집` | map with side card | WLD-75 |
| 사건 상세 · 관련 인물 row | right pane of `TwoTimeline` + one row | full screen | right pane | WLD-33, 74 |

**Layout notes.**
- Glass header heights: lists 110–118 px content offset; map screens overlay the map. Map tool bar sits bottom-left (inner) / bottom centre (cover), never near the fold line. Hit areas ≥ 44px for pins, vertices, chips, toggles.
- Typography: names and sentences in Nanum Myeongjo; labels and buttons in Noto Sans KR; numbers (years, `1-7`, `#044`, km, seeds, `O09`) in IBM Plex Mono.
- Colours: `▲` uses `warn` (dark #F2B66B, light #8A5A00); `◆` `acc` (dark #8FB8E6, light #1F5590); `◇` `ink3`. `map-art-1…5` (dark #3B4A66 #A0674C #D9A27F #C98A62 #2B3242; light #9DB0CC #D9A788 #EBC7AA #C9A07E #B8C3D6) for `색면`; `ter-*` fixed (WLD-57).
- Fold: the map keeps centre and zoom; place sheet ↔ side card is one element; list modules open the selected item on the right when unfolding; on fold the last-touched pane stays. State must be saved and restored for devices that close apps on fold.

---

## 6. Open questions

Each item names the conflicting or silent sources and the default this spec uses.

- **Q-01 · Term card layout.** Rule 21 §2 and §8 split `쓰인 곳` into three groups (`쓰인 곳 · 2`, `후보 · 1 · 확인 안 함`, `원고 밖 · 메모 · N`); the redrawn board `Term` (2026-11-22) shows one manuscript-order list with the candidate row inline (`◇ 후보 · 끊김 · 잇기 · 아님`) and no out-of-manuscript group. Default: groups (rule is the authority), header string as the board.
- **Q-02 · Pin tap with a child map.** Planning `지도` and boards `Map` / `MapC`: tapping a pin opens the place card. Board `MMapZoom`: `핀 탭 → 그 점을 중심으로 확대하며 하위 지도로`. Default: tap opens the place sheet; entering the child map needs a visible control in that sheet (copy not drawn).
- **Q-03 · Place parent label and source.** Planning `지도` and board `Map` say `소속 | 하층 구역`; boards `MapSoft`, `PlaceTime` say `지역 | 하층 구역` / `장소 · 하층 구역`; vocabulary memory lists `지역 / 영역 / 구역` as unresolved. Also unclear whether it is authored or derived from map nesting. Default: authored `지역`, prefilled from nesting.
- **Q-04 · Matcher details rule 21 leaves open.** (a) continuation after particles (`이었 · 였 · 이라 · 이다` are stems that need further endings, e.g. `오리지늄이었다`); (b) full name + `…`; (c) ASCII `...`; (d) cut-off prefix shared by two terms; (e) identical spans of two terms; (f) link and `괄호 지문` runs as boundaries; (g) syllable count outside Hangul; (h) how far "spaces ignored" goes. Defaults in WLD-21 and test WLD-T32.
- **Q-05 · Direct `전환 문구` and terms.** Rule 22 §3 table: `직접 문구 속 용어는 ▲ 공개 전 사용 점검 대상`; rule 21 §4's text-kind table does not list `전환 문구`, and `▲` comes only from links while rule 22 says the app does not read direct transition text. Unclear whether `@` links are allowed in it. Default: scanned for candidates as game-output text; links allowed and counted if SCR supports them.
- **Q-06 · First reveal on a whole choice block.** Rule 21 §5 forbids option lines and §8 leaves to the lead whether rule 12's "◆ only on the whole choice block" also applies to `◇`. Default: allowed on the whole choice block.
- **Q-07 · Effect of `맞음`.** Rule 21 §6 stores `맞음` on the link and collects it under `맞다고 표시한 것`, but does not say whether it lowers `▲ 앞에서 N회 사용`, the module badge, or the card marker; §8 also leaves the unified storage of `맞음` across the check center open. Default: excluded from all `▲` counts, row keeps a small `맞음` tag on the card.
- **Q-08 · Terms without first reveal in the list.** Rule 21 §5 defines card and check copy but not the list section. Default: `아직 공개 전` with right label `첫 공개 없음`.
- **Q-09 · Place and faction lists.** Tabs exist (`World`), rows are not drawn anywhere. Defaults in WLD-03/04.
- **Q-10 · Faction card and fixed fields.** 인물 항목 decision 5 extends project fields to 장소 · 용어 · 세력 but defines fixed fields only for 인물; no faction card board exists. Defaults in WLD-06/10.
- **Q-11 · Merging terms.** Rule 20 §6 covers two first reveals but not names and aliases. Default: the merged term's name and aliases become aliases of the target.
- **Q-12 · Project particle additions.** Rule 21 §3: list `프로젝트에서 덧붙임 가능`; no screen. Default: a row in `프로젝트 설정` (not drawn).
- **Q-13 · World calendar details.** Planning names `달 이름 · 한 달의 날 수 · 요일`; no editor board; the story start date inside the calendar and the weekday of day 1 are not defined; in `연도만` mode the year length for placing story days into years (chapter rows `1200`) is not defined. Default: editor in WLD-30, story start month/day stored, 365-day years in `연도만` `[PROPOSED]`.
- **Q-14 · Whose ages appear on timeline rows.** Boards show `리엔 12세` on the 대정전 row but `리엔 12세 · 세라 16세` in its detail, and `리엔 17세 · 하르트 ??세` on the chapter row. Default: detail = every character with a computable age; rows = characters tagged 주연 `[PROPOSED]`.
- **Q-15 · Story rows on the timeline.** Boards mix chapter spans, authored story events (`리엔, 결사 합류`, `하르트와 마주침`) and rows titled like scenes (`리엔의 새벽 순찰` = scene `새벽 순찰`). Default: authored `STORY` events anchored to scenes plus derived chapter spans; scenes are not auto-listed.
- **Q-16 · Cover editing tools.** Planning `지도 만들기`: `커버 화면은 보기 · 핀 · 경로 · 축척, 영역 편집은 펼친 화면 중심`; cover boards `MapRouteC`, `TourMap` show the full bar `이동 · 핀 · 영역 · 경로 · 라벨 · 레이어`. Default: full bar on both.
- **Q-17 · Regions vs provinces vs scene `지역`.** Overlay regions (`영역`, kind `세력 영역 ›`) and polygon provinces (`지역`) both exist; the kinds list is not defined; it is unclear whether a province can be a scene `지역`; rule 20 does not cover deleting a region that scenes use as `지역`. Default: only overlay regions are pickable; deleting such a region shows `지운 지역 · <이름>` on scenes `[PROPOSED]`.
- **Q-18 · Route `쓰인 곳`.** Planning: `원고에서 그 경로를 언급한 곳도 보인다`; rule 12 calls it computed; board `MapRouteC` shows `쓰인 곳 | 1-7 씬 2 · “이틀 반이면 닿아.”` with no link marker. No source defines how a sentence refers to a route (`@` targets are only 인물 · 용어 · 장소). Default: the route `쓰인 곳` row is not shown until decided.
- **Q-19 · Scale units.** `일 · km · 리 · 직접` — the km value of `리` and the meaning of `직접` are not given.
- **Q-20 · Step chips not drawn.** Chips for `해안선`, `숲 · 기후`, `마을 · 길` are absent from `MapGen`, `MapGenWide`.
- **Q-21 · Missing drawings.** Cover versions of `인물 동선` and `연표 + 지도` (planning: every module has a cover version, maps included); inner versions of hex/node; event sheet; calendar editor; map list view for accessibility (memory: remaining); 4K/8K export entry; the pixel threshold for tiled loading (planning only says ~8000 px freezes).
- **Q-22 · Re-rolling an upstream step while a downstream step is locked.** Planning gives per-step lock and re-roll but not dependencies (rivers depend on height; towns on rivers). Default `[PROPOSED]`: downstream locked steps keep their seed and choices and are recomputed from them on the new upstream result; author edits on them are kept as in WLD-56.
- **Q-23 · Hex `줄` tool.** Named on `MapHex` without behaviour.
- **Q-24 · Flashback position in `이곳의 시간`.** Rule 11 §4: flashbacks appear `그 시절 자리`; board `PlaceTime` lists `1-4 씬 2 · 세라의 회상 · 대정전 그날` after `1-2 씬 1`. Default: rule 11.
- **Q-25 · Cloud and generation.** Planning: generation runs on device; the settings row `지형 만들기 · 글로 설명 | 기기 안 · 클라우드` could be read as covering terrain generation too. Default: only text reading may use cloud.
- **Q-26 · Where `◇ 첫 공개`, `아님`, `맞음` live in `.civilight`.** The file-format tab lists `changes.json` for ◆ only and is silent on these three (it only says the `맞음` mark is stored). Default: `firstReveal` in `changes.json`, `아님` inside the term in `world.json`, `맞음` with the check-center owner keyed by `linkId`.
- **Q-27 · A place with pins on several maps.** Rule 22's `◇ 공개 전 장소 · 전환 문구` reads "the place's visible point (map)"; with pins on both a parent and a child map the two points may differ. Default: the earliest point among the place's pins.
- **Q-28 · Cross-area contracts this spec assumes.** (a) SCR gives every link run a stable `linkId` (ULID) so `맞음` can attach to a link (rule 21 §6 says `맞음` attaches to "그 연결"; no source defines link identity). (b) The travel verdict and its chips, the scene `장소와 때` sheet, `장소 · 때 한꺼번에`, and the `맞음` store are owned by other areas and are not specified here; this spec supplies only the place picker, distances, travel modes and term rows. (c) The `.civilight` placement of `첫 공개` and `아님` (Q-26) needs the storage owner's agreement.

**0.2.0 questions (each with the default used above)**
- **Q-29 · Which map tools S-3 hides.** S-3 and scope §8 name only node, terrain and sketch maps, distances, routes, travel modes and tracks. The headline "one image plus pins" could also mean hiding regions, labels, layers, sub-maps and reveal points. Default: those five stay on image maps, because scenes may use a region as `지역` and nothing names them. Scale goes with distances, and `연표 + 지도` stays without its lens chip.
- **Q-30 · Hand-set reveals in 0.2.0.** Scope D12 keeps 0.1.0 hand-set reveals but does not say whether a new one can be set. Default: `공개 시점 옮기기` stays (the check center's `▲` row still needs it), and one new row returns a term to automatic (WLD-72). The `◇ 첫 공개 없음` row keeps its trigger and in practice no longer shows.
- **Q-31 · Travel check default and storage.** 0.1.0 `AppSettings.checkTravel` defaults to `true`. Default proposed to the owning lane: a new additive field read in its place, default off, with the old value kept. Rewriting the stored value would break scope §8 rule 1.
- **Q-32 · Where the candidate setting lives.** "설정에서 끌 수 있다" does not say app or project. Default: a project setting `용어 후보`, default on, which travels in `.civilight`.
- **Q-33 · The candidate list in the 용어 tab.** Default: one row per term with candidates, as the last section, opening the term card's `후보` group, instead of a separate screen listing every candidate.
- **Q-34 · `다른 이름` and `상위 지역` in the create card.** U-15 words them so. Default: they are the existing `별칭` (one alias, never split) and `지역` fields, and the card keeps U-15's words.
- **Q-35 · Lens chip on world heads.** Default: the list head and the term, place and faction card heads only. 연표, map view, map edit and 연표 + 지도 read at `최신` with no chip.
- **Q-36 · New map entry.** U-15 lists no map kind. Default: the 지도 tab gets the same create row, `＋ 새 지도` (icon list rule "목록마다"), which opens the image picker directly, because image is the only base left.
- **Q-37 · `때` in the event card.** Default: one optional field with two visible choices (year, or `씬 고르기`); the event kind follows. Empty means an undated world event.
