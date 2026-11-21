# 20 · 스토리 · 시점 · 때 · 점검 (STO)

Implementation spec for the **story / story-time / scene-time / check** area of Civilight (Android, Jetpack Compose). Feature ids `STO-01` … `STO-43`. Engineers implement from this file alone; every Korean string in `code` style is UI copy and must be used verbatim.

## 0. Conventions

**Sources read (2026-11-22).** Planning doc rule tabs 11 시점과 때 (rev 8, confirmed 2026-11-21), 12 블록과 변화 기록 (rev 8), 13 관계 (rev 6), 18 이동 점검 (rev 6, confirmed 2026-11-22), 22 씬 전환과 씬 속성 (rev 2, confirmed 2026-11-22), 규칙 index (rev 25, incl. the 2026-11-22 follow-up decisions), 저장 파일 양식 (rev 8); local confirmed drafts `rules/19.md` … `23.md`; planning main tab (live rev 71); memory files (concept, vocabulary, rules-time, rules-blocks, rules-relations, rules-travel, rules-19-23, character-fields, feedback-*); board text extracted fresh from `design/night-manuscript/project/*.dc.html` (generators `scr_story.py`, `scr_tools.py`, `scr_link.py`, `scr_bulk.py`, `scr_inner.py`, `scr_decide.py`, `scr_new_time.py`, `scr_new_scene.py`); design system `README.md` + `components/*/README.md`; research `2026-11-20-design-research.md`.

**Precedence** (newest authority first). Confirmed rule tabs + memory + 규칙 index follow-ups > current board generator copy > planning main tab > design-system component READMEs (`LensChip`, `WhenPicker`, `TravelBar` READMEs and the main tab still carry pre-rule copy such as `+3일로 · 말 1일`, `씬 전환 블록`, `받기 · 넘기기`, "저장은 이야기 시작부터의 시간 하나"; this spec silently follows the newer side). Only conflicts between confirmed sources, and true gaps, go to §6.

**Markers used in this spec**

- `ED` = **engineering default, not in sources.** A deterministic choice the sources do not make. Implement it unless the product owner says otherwise. Every `ED` that affects stored data or a test outcome has a §6 entry; layout-only `ED`s do not.
- `copy: not drawn` = a state needs a label no source gives. Do not invent Korean copy; use a string resource `TODO_COPY_STO_<id>` and raise it.
- Board names drop the `NmDark`/`NmLight` prefix (e.g. `Story`, `RTCascade`). Every board exists in dark and light. "cover" = 390×844 boards (Compact width); "inner" = 800×800 boards (Medium/Expanded width).
- Rule references: `R11 §4` = rule tab 11, section 4. `MAIN` = planning main tab. `FMT` = 저장 파일 양식 tab. `IDX` = 규칙 index follow-up list (2026-11-22).
- Markers in UI: `◆` change (`acc`), `◇` first reveal / soft notice (`ink3`), `▲` warning (`warn`, amber). Dotted underline / dotted outline = attached to something deleted.
- Separators in copy are ` · ` (space, middle dot, space). Ranges in durations use ` – ` (space, en dash, space) as on the boards. Episode notation `1-7`, chapter `2장`, block `#043`.
- **needsExternal**: every feature in this area runs on-device with app-internal data. None needs network, cloud, ML models or OS surfaces, so all are `no`.
- **Clock**: date logic (오늘 쓴 글자, `방금`) takes an injected `Clock`; tests use `now = 2026-11-22T09:00+09:00` (Asia/Seoul).

---

## 1. Scope

### 1.1 What this area owns

1. **Project home** (`홈`) on cover and inner layouts, including the episode list, `이어 쓰기`, the work shortcuts and the lens chip. It hosts the `시작해 볼 것` block owned by LIB-13.
2. **Story structure**: the customisable hierarchy (1–4 levels, default `장 › 에피소드 › 씬`), unit codes and labels (`1-7`, `씬 4`, `2장`), the `스토리` screen (chapters, episodes, scenes, statuses, counts), creating, ordering, moving and deleting units, and the menus on the list. The new-project wizard steps that pick the structure and time mode are owned by LIB-10 / LIB-11; this spec owns the data they write and the later editing rules.
3. **Story position model and the ChangeRecord engine**: the total order of blocks and units, point semantics (`X부터` includes X; a point means "after that unit ends"), the **집필 선**, and the engine other areas write `◆` records into: record envelope, anchors (`Block` · `UnitStart` · `SceneEnd` · `WorldEvent`), `valueAt(target, point)`, `예정` classification, storage in `changes.json`. Cast/world define what a record targets and holds (CST, WLD); SCR moves anchors when blocks move or are deleted (`근거 지워짐`, SCR-43/44).
4. **시점 렌즈** (lens): lens state per project, the lens chip, the lens sheet, `최신` vs `예정` classification, the past-lens edit banner, the two edit buttons' rules (`이 값 고치기` · `여기서부터 바뀜`), and the lens save/restore around the script. The field editors that show the two buttons are cast/world UI; they call this area's rules.
5. **Scene 때** (rule 11): storage as base + interval, `직접` fixed points, precision `정확 · 대략`, `회상`, resolution to absolute time with ranges, `N일째` notation (episode-relative, story-relative, anchor-event-relative), the cascade preview (`뒤도 같이 · 이 씬만`), and order/time mismatch notices.
6. **Time settings for scene 때**: `때 표기`, day boundary, time bands (rows in 프로젝트 설정, container LIB-69). World-time mode, start year, the world calendar (WLD-30) and travel modes (WLD-47) are WLD's; this area consumes them.
7. **Scene properties**: 장소 / 지역, 때, 회상, 등장 (speakers + silent cast); the `씬 · 장소와 때` sheet, which also hosts the `전환 문구` row of SCR-48. 전환 문구 and 씬 메모 storage, auto text and their checks are SCR's (SCR-35, SCR-48, SCR-62).
8. **이동 점검** (rule 18): pairs, gap between two scenes, verdicts, suggestion chips, mode choice per pair, `맞음`. Distance and required minutes come from WLD-66.
9. **장소 · 때 한꺼번에**: bulk list, `장소 정하기`, `때 정하기`, `제안 받기`, unfolded table.
10. **명령 칸 and search** inside a project: command bar on every screen, search sheet, 초성 matching, hit boxes, commands, empty state.
11. **점검 센터 semantics**: the registry of every check type with trigger, subject and fingerprint, the time/travel rows, `맞음` / `이대로 두기` invalidation rules, the check-related settings toggles. The screen shell (layout, row anatomy, filters, acknowledgement storage) is co-specified in CNV-17 (`50-canvas-pages.md`); STO-39 restates it and must stay identical (differences: §6 Q35).
12. The **engine contract** other areas consume: resolved scene time and labels, scene ordering by time, lens resolution of `◆` records, distance/time comparison for travel.

### 1.2 Hand-offs to other areas

Sibling files present when this was finished: `10-script.md` (SCR), `30-cast.md` (CST), `40-world.md` (WLD), `50-canvas-pages.md` (CNV), `60-library-security-files.md` (LIB).

| Area | Owns (this spec only references it) |
| --- | --- |
| 10 · 스크립트 (SCR) | Blocks, block numbers, voice IDs, renumbering (`RTRenumber`), the script header position line and `이 씬 · 1-7` label (SCR-02), `씬 머리` (SCR-46) and its ⋯ menu (SCR-47), `전환 문구` storage, auto text and checks (SCR-48), 씬 메모 (SCR-35), `/씬` routing and `다음 씬 만들기` (SCR-49), `씬 나누기` (SCR-50), `씬 합치기` (SCR-51), `씬 지우기` action and toast (SCR-52), `앞에 새 씬` (SCR-53), the scene-move preview sheet (SCR-54), block select mode, `근거 지워짐` and anchor relocation (SCR-43/44), the check items script data emits (SCR-62), script commands (SCR-68), the panel `시간` tab that shows `이 씬 · 장소와 때` |
| 60 · 서재 · 보호 · 파일 (LIB) | 서재, new-project wizard steps 1–4 incl. 구조 (LIB-10) and 시간 (LIB-11) screens, `시작해 볼 것` checklist (LIB-13), cross-project search scope (LIB-07), hiding/unlock/re-lock, trash host and toast host, `.civilight` packaging and versioning, export writers, app settings screens and the 프로젝트 설정 container (LIB-69), onboarding tours (incl. `장소와 때 안내`, `TourWhen`), read-only example (LIB-17) |
| 30 · 인물 (CST) | Characters and fields (incl. a project field `기본 이동 수단` if defined), relations (사이 · 마음 · 호칭), what a `◆` record targets and holds, field-history UI and the two edit buttons on the profile (`FieldHistory`, `RTEditTwo`, `RTPlanned` rows), display names, delete/merge |
| 40 · 세계 (WLD) | Places, regions, routes, scale, child maps, pins' `보이는 시점`, terms and aliases, first reveal records, candidate matching and `아님` (rule 21), world-time mode and start year, world calendar incl. its story start date (WLD-30), the 연표 screen and its views (WLD-31…35: `Timeline`, `TimelineAnchor`, `TimelineMap`), travel modes (WLD-47), 인물 동선 (WLD-62, `MapTrack`), 이곳의 시간 (WLD-63, `PlaceTime`), distance and required minutes (WLD-66), place picker `지도에서 고르기 ›`, `축척 맞추기` (WLD-46), world events (e.g. 대정전 1195) |
| 50 · 캔버스 · 페이지 (CNV) | 점검 센터 shell (CNV-17), list-module inner layouts and the layout setting (CNV-33), fold/unfold and state restore (CNV-34/35), motion tokens incl. lens value swap and search highlight (CNV-37…46), page editor |
| 라이프 레코더 | Recording entry (the mic on the command bar opens it), 기록함 |
| Foundation / design system | Glass bars, sheets, toasts, motion tokens, fold/unfold preservation, window-size classes |

### 1.3 Shared items, ownership decided here

| Item | Owner | Note |
| --- | --- | --- |
| Scene place/time **storage and computation** | here | SCR, world, export read it through STO-22 |
| ChangeRecord envelope, anchors, `valueAt`, `예정` | here (STO-12, §3) | targets/values: CST, WLD; anchor relocation: SCR-43/44 |
| `씬 지우기` · `앞에 새 씬` · `다음 씬 만들기` actions and toasts | SCR-49/52/53 | 때 rebasing, pre-fill values and 집필 선 effects: here (STO-05, STO-10) |
| 전환 문구 and 씬 메모 storage, auto text, transition checks | SCR-35/48/62 | the row in the `장소와 때` sheet and the resolved place/time they use: here (STO-27) |
| 점검 센터 shell | CNV-17 | registry semantics, subjects, fingerprints, time/travel rows: here (STO-39…42) |
| `씬 · 장소와 때` sheet (`SceneWhere`, `RTFlashback`) | here | Opened from SCR's scene head, SCR's `시간` tab, the structure ⋯ and the bulk table |
| `씬 머리` / `전환 문구` **rendering** in manuscript | SCR | Place/time labels come from STO-22 |
| Split / merge / move **sheets** | SCR | Place/time effects are computed by STO-09 / STO-08 and shown in those sheets |
| Split / merge / move **entry from the story list** | here | STO-07 |
| `맞음` / `이대로 두기` invalidation rules | here (STO-41) | storage mechanism also in CNV-17; rule 21 `아님` stays with world |
| 인물 동선, 연표 views | WLD-31…35, WLD-62/63 | Use STO-22 ordering and labels |
| Wizard steps 2–3 UI | LIB-10/11 | Write `StoryStructure` (§3) and WLD's world-time settings |
| Block renumbering | SCR | Entry in the episode ⋯ menu here (STO-07) |

---

## 2. Features

### A. Project home and story structure

#### STO-01 · 프로젝트 홈
Priority **P0** · needsExternal **no**

Layout (cover, `Home`), top to bottom:
- Glass head: left `‹ 서재  ·  프로젝트` (tap `‹ 서재` → 서재), rating badge (`R-15` etc., outlined mono; R-18 red — LIB), right lens chip (STO-13), e.g. `최신 · 1-7`.
- Project name in large Nanum Myeongjo (`잿빛 새벽의` / `등불` wraps naturally), one-line description (`꺼져 가는 하층 구역, 마지막 등불을 지키는 결사의 이야기.`).
- Three stats, mono numbers with small labels: `7/10` · `1장 에피소드` (progress of the current chapter, STO-06), `1,240` · `오늘 쓴 글자`, `12` · `인물`.
- `이어 쓰기` block: label `이어 쓰기`, location `1-7 씬 3 · 꺼진 등불`, quote of the last-edited block's first line `“하르트 씨, 방금…”`. Tap → opens the script at that block with the caret at its end.
- Current chapter list: header `Ⅰ 1장 · 등불지기`, rows `1-5 | 첫 번째 신호 | 완료`, `1-6 | 잿더미 시장 | 완료`, `1-7 | 잿빛 새벽의 등불 | 쓰는 중 · 씬 3`, `1-8 | — | 미정`, `1-9 | — | 미정`. Untitled episodes show `—` as title. Tap a row → story structure (STO-04) with that episode expanded (inner: shown in the right pane, see STO-43).
- `작업` shortcuts: `스토리 · 인물 · 세계 · 연표 · 캔버스 · 설정` (설정 = 프로젝트 설정, LIB container).
- Bottom command bar (STO-36) with placeholder `명령 또는 검색 · 인물 · 용어 · 대사`.
- Right after project creation, the `시작해 볼 것` block (LIB-13, board `NewReady`) is shown on this screen until closed with `목록 닫기`.

Rules:
- "Current chapter" = the chapter containing the last-edited block; if none, the first chapter. `ED`
- `오늘 쓴 글자`: see STO-06. `인물`: count of non-deleted characters of the project, hidden ones included (count only). `ED`
- `이어 쓰기` target = the last block the user edited in this project (app DB, not in `.civilight`). Empty project: block hidden. `ED`
- Hidden project: home is reachable only while unlocked (LIB). Locked hidden scenes never appear in `이어 쓰기`; if the last edit is in a now-locked scene, fall back to the most recent visible edited block. `ED`

Sources: MAIN 프로젝트 홈; boards `Home`, `TwoHome`, `ColHome`, `NewReady`.

#### STO-02 · 이야기 단계 (customisable hierarchy)
Priority **P0** · needsExternal **no**

- A project's structure is an ordered list of **1 to 4 level names**. Presets: `장 › 에피소드 › 씬` (default), `막 › 장면`, `직접 정하기` (`단계 수와 이름을 직접 (최대 4단계)`). The lowest level is the **scene level** (the unit that holds blocks and has 장소 · 때). With 2+ levels, the level directly above the scene level is the **episode level** (used for `N일째`, block numbers, voice IDs, lens chips). With 1 level there is no episode level (R11 §6 "에피소드 단계 없이 씬만").
- Picked in the wizard (LIB-10). Shown in 프로젝트 설정 as `단계 이름 | 장 › 에피소드 › 씬 | 바꾸기 ›` and in the story structure header `단계 이름: 장 › 에피소드 › 씬`.
- `바꾸기 ›` opens the level-name editor: rename any level (`단계 이름은 나중에 바꾸기 가능`). Renaming changes labels only; keys, order and data stay.
- Changing the **number** of levels after units exist is not specified by any source. `ED`: v1 allows renaming only; the add/remove level controls are disabled once any unit below the top level exists. Copy for the disabled state: not drawn. (§6 Q1)
- Notation note: the vocabulary rule `씬` (never `장면`) applies to the default preset; `막 › 장면` is a user-visible level name and is shown as written.

Sources: MAIN 스토리 구조, 새 프로젝트; memory vocabulary; boards `NewStructure`, `NewWide`, `ProjSettings`, `Story`.

#### STO-03 · 단위 표기 (unit codes and labels)
Priority **P0** · needsExternal **no**

- Default preset: chapter `N장` (header form `Ⅰ 1장 · 등불지기`, roman numeral + `N장` + title), episode code `c-e` (`1-7` = chapter ordinal - episode ordinal within the chapter), scene `씬 n` (ordinal within its episode). Block `#043` (SCR).
- Ordinals are derived from order, never stored. Scenes in trash do not count; hidden scenes do count (they keep their place and number, R19 §6).
- Codes are always mono (IBM Plex Mono). The full scene reference is `1-7 씬 3`; with title `1-7 씬 3 · 꺼진 등불`.
- Custom hierarchies: `ED` top level `<n><level name>` (`1장`, `1막`), episode level `c-e`, scene level `<level name> <n>` (`씬 4`, `장면 4`); 1-level projects use `<level name> <n>`; 4-level projects prefix the episode code with all ancestor ordinals (`1-2-3`). (§6 Q2)
- Untitled units show `미정` in the story list and `—` in compact rows (home, scene time column).

Sources: MAIN 공통 개념 표기 규칙; DS README; boards `Story`, `Home`.

#### STO-04 · 스토리 구조 화면
Priority **P0** · needsExternal **no**

Cover (`Story`), no lens chip (R11 §3):
- Glass head: small `잿빛 새벽의 등불`, title `스토리`, right ⋯ (STO-07).
- Line `단계 이름: 장 › 에피소드 › 씬`.
- Per chapter: header `Ⅰ 1장 · 등불지기` with right mono `7/10` (progress, STO-06). A chapter with no written episode shows `0/8 · 계획` in `ink3`, title in `ink2`, and its synopsis paragraph (`결사가 상층 구역으로 올라가며 대정전의 진실과 마주한다.`) instead of rows.
- Episode rows: grip icon (visible drag handle, STO-08), mono code `1-5`, title (Nanum Myeongjo 16; bold when expanded or current), right meta: `씬 3개`, or when expanded `씬 4개 · 5,210자`. Unwritten episodes: title `미정`, meta `—`, whole row `ink3`.
- Scene rows (under an expanded episode, indented 56 px): mono time column (short 때 label, STO-22: `1일째 새벽`, `2일째 아침`; `—` when no 때), `씬 1 · 새벽 순찰`, right meta = char count `1,820자`, or `쓰는 중` (`acc`) for the scene being written, or `—` for an empty scene. The scene being written gets the selection style (`sel` fill + 2 px `acc` left bar).
- Bottom: command bar placeholder `명령 또는 검색 · 씬 이동`, and a primary button `새 씬` above it (STO-05).

Interactions:
- Tap episode row → expand/collapse its scenes. Exactly the current episode is expanded on entry. `ED`
- Tap scene row → open the script at that scene's head.
- Long-press does nothing (no hidden gestures). Each scene/episode row has a visible ⋯ (STO-07). `ED` for the ⋯ placement (copy not drawn beyond the menu items).
- Inner: in `목록 + 상세` layout the structure is the right pane of home (`TwoHome`), see STO-43.

Sources: MAIN 스토리 구조; R11 §3; boards `Story`, `TwoHome`, `RSSceneMove` (shows `2일째 아침 | 씬 4 · 미정 | —`).

#### STO-05 · 단위 만들기
Priority **P0** · needsExternal **no**

- `새 씬` (story structure bottom button) = the same action as SCR's `다음 씬 만들기`: creates a scene **after the current scene** (the scene being written, else the last scene of the expanded episode) in the same episode. `ED` for the insertion point.
- It opens the `다음 씬 만들기` sheet (SCR-49 owns the sheet): fields 제목 · 장소 · 때 · 전환 문구, all skippable. 장소 · 때 are **pre-filled** and shown with `비우기` next to them (R22 §4); this area computes the pre-fill:
  - 장소 = the previous scene's place/region (none → empty).
  - 때 = `<P> 기준 · 같은 때` (interval 0, P's band/time and precision), where P = the previous scene if it has non-flashback 때; else the nearest earlier timed non-flashback scene (`ED`, follows R11 §4 base rule); no such scene → 때 empty.
  - Previous scene is 회상: P skips it as above (R11 §4 "뒤 씬의 기준은 회상을 건너뛴 앞 씬"); 회상 itself is not copied `ED`.
  - Nothing is created until the sheet's create button. `비우기` on 때 → new scene has no 때 (R11 §4 "가운데에 씬을 넣음 → 새 씬은 때 없음으로 시작"; §6 Q33).
- `앞에 새 씬` (SCR-53) creates an empty scene before the current one; pre-fill as above from the scene before it. `ED`
- Next episode / chapter: created only from the story structure (`다음 씬 만들기` never crosses an episode, R22 §4). Entry: structure ⋯ and episode ⋯ (STO-07). Copy for "새 에피소드 / 새 장" items: not drawn.
- New units get a ULID key and an order key placed between neighbours (FMT §4).
- Inserting a scene never changes another scene's 때 (R11 §4): later scenes keep their base reference.

Sources: R22 §4; R11 §4; boards `Story`, `Blocks` (`새 씬 | 다음 씬 만들기 · 여기서 나누기 | /씬`).

#### STO-06 · 단위 정보 · 상태 · 진행 · 글자 수
Priority **P0** · needsExternal **no**

Stored per unit: title (nullable → `미정`), chapter synopsis (nullable; shown for `계획` chapters), scene 씬 메모 (text; used by merge, R22 §5), author-set **완료** flag.

Derived (never stored, FMT §4):
- **Scene status**: `미정` = no block with non-empty content; `완료` = author marked done; otherwise `쓰는 중`. The story list shows `쓰는 중` only for the scene containing the last edit; other in-progress scenes show their char count. `ED` (§6 Q3: the mark-done control is not drawn).
- **Episode status** (home rows): `미정` if no scene has content; `완료` if every scene with content is 완료 `ED`; else `쓰는 중 · 씬 n` with n = scene of the last edit in that episode (`ED`: if the last edit is elsewhere, the highest-numbered scene with content).
- **Chapter progress** `k/n`: n = episodes in the chapter (not in trash), k = episodes with content. k = 0 → label `0/n · 계획`.
- **Char counts** (`1,820자`, `5,210자`): `ED` count of Unicode grapheme clusters in game-output text (대사 incl. 괄호 지문, 나레이션, 선택지, 시스템 텍스트), spaces included, author memos and 연출 지시 excluded. Format with thousands separator `,` + `자`.
- **오늘 쓴 글자**: `ED` sum over blocks of max(0, length now − length at local-day start), counted with the same rule as char counts; resets at local midnight; stored as a daily snapshot in the app DB. (§6 Q4)

Sources: MAIN 스토리 구조, 프로젝트 홈; boards `Story`, `Home`.

#### STO-07 · 구조 메뉴 (⋯)
Priority **P0** · needsExternal **no**

- **Structure ⋯** (story structure head): `장소 · 때 한꺼번에` (→ STO-31). Other items: copy not drawn (new episode / new chapter, STO-05).
- **Scene ⋯** (on a scene row in the story list), items from R22 §7 plus the scene-head menu: `장소와 때` (→ STO-20), `전환 문구` (→ STO-20 transition row), `앞 씬과 합치기` (→ SCR merge sheet; disabled for the first scene of an episode), `여기서 나누기` (`ED`: opens the scene in the script; the split happens there via `/씬`, because a split point is a block — §6 Q5), `옮기기` (→ STO-08; copy from R12 §3 `옮기기`), `씬 지우기` · `휴지통 · 30일` (→ STO-10).
- **Scene-head ⋯** in the manuscript (`RSHeadMenu`, SCR-47): title `씬 4 · 1-7`; rows `장소와 때 | 결사의 문 · 2일째 아침 06:10`, `전환 문구 | 직접 · 결사의 문 앞 · 06:10`, `앞 씬과 합치기 | 씬 3 · 꺼진 등불`, `씬 지우기 | 휴지통 · 30일`. The right-hand values come from STO-22/27.
- **Episode ⋯**: `번호 다시 매기기 · 1-7` (→ SCR renumber sheet `RTRenumber`). Other items: copy not drawn.
- Read-only example project: all mutating items disabled with `읽기 전용` (R22 §4 table; LIB-17).

Sources: R22 §3, §4, §7; R12 §5; MAIN 지도 ↔ 연표 (들어가는 곳); boards `RSHeadMenu`, `RSHead`.

#### STO-08 · 씬 옮기기 · 순서 바꾸기
Priority **P1** · needsExternal **no**

- Ways to move (no hidden gestures, R12 §3): drag the visible grip handle on a row, or ⋯ › `옮기기` then pick a destination in a structure picker (copy for the picker: not drawn).
- **Scene move** always shows the preview sheet **before** moving (R12 §3; sheet `RSSceneMove`, content owned by SCR-54): title `씬 옮기기`, sub `미리 보기 · 옮기기 전에는 그대로`, line `1-7 · 씬 3 | 꺼진 등불 | 1-8 | 맨 앞 · 씬 1`, section `바뀌는 것` with rows `기록 | ◆ 2 · ◇ 1 · 시점 1-7 → 1-8`, `블록 번호 | 블록 13개 · 1-8 새 번호`, `보이스 ID | 고정 4개 그대로 · ◇ 에피소드 부분 다름`, `때 | 그대로 · ◇ 1 순서와 어긋남`; buttons `취소` · `옮기기`.
- This area supplies the **때 row**: a moved scene keeps its own base + interval and absolute time; no other scene's 때 changes (R11 §4). The row reads `그대로`, plus `· ◇ n 순서와 어긋남` when n scenes would get an order/time mismatch (STO-25) after the move.
- If the moved scene was the base of later scenes, they keep that base (by key) and their absolute time. `ED` (follows "옮기면 다른 씬의 때는 그대로").
- **Episode / chapter reorder** (drag): codes renumber (`1-7` may become `1-6`); no 때 changes; fixed voice IDs keep their old episode part and raise SCR's `◇ 에피소드 부분 다름`. No preview drawn. `ED` (§6 Q6)
- One undo step restores the previous order.

Sources: R12 §3; R11 §4; boards `RSSceneMove`, `Story`.

#### STO-09 · 나누기 · 합치기의 장소와 때 (engine for SCR sheets)
Priority **P1** · needsExternal **no**

The sheets are SCR's (`RSSplit`, `RSMerge`). This area computes the place/time values they pre-fill and the `바뀌는 것` time rows. Rule: **split and merge never change any other scene's absolute 때** (R22 §5).

Split A → A + new B (below the current block):
- B.장소 = A.장소; B.때 = `A 기준 · 같은 때` (interval 0, A's band/time, A's precision). A without 때 → B without 때. A is 회상 → B is 회상 (shown, can be turned off). A hidden → B hidden (shown).
- Scenes whose base is A keep base A; absolute unchanged. Row `씬 5 때 | 기준 씬 3 · 2일째 아침 06:10 그대로`.
- A stays the episode's first timed scene if it was (B does not change `1일째`).
- B: 전환 문구 `없음`, 제목 `미정`. Silent cast: sheet asks `씬 3 · 새 씬 4 · 둘 다` (default `둘 다`). Speakers are recounted from blocks.

Merge neighbours A (earlier) + B → A (same episode only):
- 장소, 때: if only one side has a value, use it without asking. If both have values and differ, the sheet offers `씬 3 것 | 2일째 새벽 05:40` and `씬 4 것 | 2일째 아침 06:10` (default A's); the unchosen one is appended to A's 씬 메모 as one line `합치기 전 · 씬 4 · 결사의 문 · 2일째 아침 06:10`.
- Choosing B's 때: A's 때 is rewritten so that A's absolute equals B's old absolute; scenes based on A get their interval recomputed so their absolute stays.
- Scenes based on B: base → merged A, interval = their absolute − A's absolute (range arithmetic, STO-22). If A has no 때, base = nearest earlier timed scene (R11 §4).
- B is `직접` (fixed point): the sheet shows that the fixed point disappears; choosing `씬 4 것` keeps 직접.
- Blocked: one side 회상 only → `먼저 회상 끄기`; one side hidden only → `먼저 숨김 풀기`; different episodes → not offered (move first).
- Title = A's; B's title becomes the first line of A's 씬 메모. 씬 메모 concatenated.
- One undo step restores both scenes with all values.

Sources: R22 §5 (live rev 2 + local draft tables); boards `RSSplit`, `RSMerge`.

#### STO-10 · 씬 지우기 · 되살리기 (때 and 집필 선 effects)
Priority **P0** · needsExternal **no**

- `씬 지우기` (story-list scene ⋯ here; scene-head ⋯ and the action/toast itself: SCR-52) moves the scene with all blocks to trash for 30 days, **without asking** (R12 §2 "씬을 지울 때도 같다. 묻지 않는다"). Toast (LIB host): `씬 3 지움 · ◆ 3 · ◇ 1은 씬 4 처음으로` + `되돌리기` (counts from SCR's record relocation).
- 때 effects: no other scene's absolute time changes. Every scene whose base was the deleted scene is rebased to the deleted scene's own base (or, if that is unavailable, the nearest earlier timed scene) with the interval recomputed so its absolute is unchanged (R11 §4).
- Restore (undo or trash): the scene returns with its own stored 때 (re-resolved against its base; if its base is gone, rebase as above). Scenes that were rebased away from it return to it as base with interval recomputed, absolute unchanged, unless the user edited their 때 in between. `ED` for the return of dependents (§6 Q7).
- If the deleted scene was beyond the 집필 선 or was the 집필 선 scene, the 집필 선 moves (STO-12); records in between become `예정` again, never deleted (R11 §1).
- Episode / chapter deletion: not specified by any source (trash filters have no episode entry). `ED`: not offered in v1; empty episodes can be left `미정`. (§6 Q8)

Sources: R11 §1, §4; R12 §2, §7; R22 §3; board `UndoToast`.

#### STO-11 · 숨김 · 지움 상태 (structure, home)
Priority **P1** · needsExternal **no**

- Hidden scene while locked: the row keeps place and number, title masked: `1-7 씬 4 · 숨긴 씬`; unlocked: title + small `숨김` tag (R19 §6).
- A scene whose place was deleted shows the place as dotted `지운 장소 · 잿더미 시장` in wide spots and dotted name + `◇` in narrow spots (R20 §2); `장소 고르기 · 되살리기 · 이대로 두기` appear in the `장소와 때` sheet (STO-20).
- Silent-cast member deleted: dotted name in the 등장 row with `빼기 · 되살리기 · 이대로 두기` (R20 §2).
- Scenes in trash are not shown and not counted.

Sources: R19 §6; R20 §2.

### B. Story position and the lens

#### STO-12 · 이야기 위치와 집필 선 (engine)
Priority **P0** · needsExternal **no**

**Story order.** Every block has a position `(chapter order, episode order, scene order, block order)` from order keys; this is "원고 순서". Positions between blocks are addressable (see §3 `StoryPoint`).

**Point semantics** (R11 §2, R12 §1):
- A **lens point** means "after that unit ends": lens `1-7` = state after the last block of 1-7; lens `1-7 씬 3` = after the last block of that scene.
- A **record anchor** "X부터" includes X: a block anchor applies from that block on (the block itself included); a unit anchor (`1-5 #—`, "그 에피소드나 씬의 처음부터") applies from before the unit's first block; a world-event anchor applies from before the story.
- A record applies at lens point P iff `anchor ≤ P` in the total order. Inside the script (SCR) the reference panel for a selected block B shows the state **just before B** (anchors < B) and lists B's own changes as a `여기서` row.

**집필 선** (R11 §1): the end of the furthest scene in story order that has content (≥1 block with non-empty content of any kind `ED`). Gaps are ignored: if 1-9 has content and 1-8 is empty, the 집필 선 is the end of 1-9 and 1-8 stays `미정`. No content anywhere → no 집필 선.

**최신**: lens point = 집필 선. Records with anchor ≤ 집필 선 are applied; records with anchor > 집필 선 are `예정` (STO-14). No 집필 선 → 최신 applies every record and there is no 예정 split.

**Recompute** the 집필 선 whenever a block gains/loses content or a scene is moved, deleted or restored. When it moves earlier (scenes deleted), records in between return to 예정; nothing is deleted. When a scene beyond it gets its first block, records up to the new 집필 선 move to applied automatically.

Hidden scenes: whether they count toward the 집필 선 is open (R19 hands it to R11). `ED`: they count (the author wrote them). (§6 Q9)

**ChangeRecord engine** (§3.1 `ChangeRecord`, `Anchor`, `StoryPoint`). API:
- `valueAt(target, point)`: the value of the record with the greatest anchor ≤ point (ties: later `createdAt` wins `ED`); no record → the target's base value, or for relation slots `만나기 전` (R13 §6).
- `recordsInEffect(target, point)`, `planned(target)` (anchor > 집필 선), `stateBefore(blockKey)` for the script panel (anchors < that block) and `changesAt(blockKey)` (anchors = that block, the `여기서` rows).
- `firstRevealBefore(term)` and ordering helpers for rule 21 use the same order.
- Records are stored in `changes.json` (FMT §2). Who may create them: CST (fields, relation slots, display name), WLD (term reveal, faction areas), SCR (from a block, SCR-41). Relocation on block delete/move/split/merge is SCR's (SCR-43/44) and only rewrites `anchor`.

Sources: R11 §1, §2; R12 §1; R13 §6; FMT §2.

#### STO-13 · 시점 렌즈 칩과 시트
Priority **P0** · needsExternal **no**

**Chip** (`LensChip`) in the glass head of screens whose content changes with the lens: 홈, 인물 목록 · 인물 기록 · 관계도, 세계 (용어 · 장소 · 세력), 지도, 연표, 캔버스. **No chip** on 스토리 구조, 점검 센터 and the script (R11 §3).
- Label at 최신: `최신 · 1-7` (episode code of the 집필 선 scene; scene-level-only projects use the scene label `ED`); no manuscript: `최신 · 전체`.
- Label at a past or future point: `시점 · 1-5` (episode point) or `시점 · 1-7 씬 2` (scene point, `ED` form), with `acc` 1 px border (DS).
- Tap → lens sheet.

**Lens sheet** (`Lens`, cover bottom sheet starting at y≈300; inner: same sheet `ED`):
- Title `시점 렌즈`, right `닫기` (closes without change).
- Sub line: board copy `앱 전체 적용 · 이 시점까지의 필드 · 관계 · 용어 공개`. (Conflicts with R11 §3 "스크립트 제외" — §6 Q10.)
- Rows (radio-like, the viewed one has `sel` + `acc` bar and right label `보는 중`): `최신` · `기본값 · 지금까지 쓴 모든 내용`; then episode points, each with code + sub (e.g. `1-3` · `결사 합류 · 하르트 만나기 전`, `1-5` · `첫 신호 · 하르트 만나기 전`); episodes beyond the 집필 선 are `ink3` with sub `아직 쓰지 않음` (e.g. `1-8`) but remain selectable (R11 §2: lens 1-9 shows 1-9 values).
- Which episodes get a row: `ED` 최신 + episodes where the record in view has a `◆` + the viewed point + the first unwritten episode; the sub line = episode title (the `· 하르트 만나기 전` relation hint is not specified, §6 Q11).
- `더 자세히`: chips of the neighbouring episodes (`1-4 · 1-5 · 1-6`, viewed one filled) and `씬 단위로 고르기` (opens a scene list of the chosen episode; copy not drawn beyond the chip).
- Buttons `최신으로 돌아가기` (ghost) · `이 시점으로 보기` (primary). Tapping a row only highlights; `이 시점으로 보기` applies.

**State**: one lens value per project, shared by all lens screens, persisted in the app DB (not in `.civilight`). `ED`: survives app restart.

**Motion** (`MLens`): on lens change, rows whose value changes slide the old value up and the new one in from below, 200 ms; unchanged rows do not move. Reduce motion → 120 ms fade.

Sources: R11 §1–§3; DS README 시점 렌즈; boards `Lens`, `Home`, `MLens`, `PlaceTime`, `Timeline`.

#### STO-14 · 예정 (records beyond the 집필 선)
Priority **P1** · needsExternal **no**

- Classification API: for any `◆` record (fields, relations incl. 사이 · 마음 · 호칭, display name) and any first-reveal record, return `APPLIED` or `PLANNED` relative to 최신 (STO-12).
- At 최신, cast/world screens render planned records as separate rows under the current value (`RTPlanned`): `예정 · 2-5 | 등불지기장` under 소속; `예정 · 1-9 | 하르트 → 신뢰` under the relation row; footnote `예정 · 집필 선(1-7 씬 3) 뒤 기록`. Rendering is cast/world's; the label parts (`예정 · <point>`) and the footnote point come from here.
- A term with first reveal beyond the 집필 선 is `공개 예정 2-5` in lists (world).
- At a past lens there is no 예정 split; records after the lens point are simply not applied.

Sources: R11 §1; R13 §6; boards `RTPlanned`, `RTEditTwo`.

#### STO-15 · 렌즈에서 값 고치기 (two buttons, banner)
Priority **P1** · needsExternal **no**

Rules this area defines; the field editors (cast profile, world cards) render them.
- When the user edits a field **that has change history** at 최신, the editor shows two visible buttons, never guessing (R11 §1):
  - `이 값 고치기` (primary, default) · sub `1-3 기록을 고침 · 1-3부터 새 값`: overwrites the value of the record in effect at the lens point. No new record.
  - `여기서부터 바뀜 · 1-7` (ghost) · sub `새 ◆ · 집필 선 1-7 씬 3 뒤`: creates a new `◆` record anchored **right after the 집필 선** (IDX) = `SceneEnd(1-7 씬 3)` (same anchor CST proposes). `ED`: lens 최신 includes `SceneEnd` of the 집필 선 scene, so the edited value shows immediately at 최신, while no block of 1-7 씬 3 sees it. (§6 Q12)
  - Fields without history at 최신: plain edit, no buttons.
- At a **past lens** (e.g. 1-5) the same two buttons are shown for any field (R11 §1 "과거 시점 렌즈에서 고칠 때도 같은 두 버튼"). The banner above the record (`LensEdit`): `1-5 시점 값 고치는 중 | 고치면 1-5부터 적용` with `최신으로` (→ lens 최신). `여기서부터 바뀜` there creates a unit-point record `1-5 #—`; the field footnote reads `저장 시 ◆ 변화 기록 · 1-5 #— · 이전 값 ‘등불 결사’`.
- Sheet layout (`RTEditTwo`): title = field name (`소속`), `닫기`; `지금 값 | 등불 결사 · 1-3부터`; history line `1-1 없음 · 1-3 등불 결사 · 예정 2-5 등불지기장`; the text field with the new value; the two buttons stacked.

Sources: R11 §1; IDX (`여기서부터 바뀜`의 새 ◆는 집필 선 바로 뒤); MAIN 공통 개념; boards `RTEditTwo`, `LensEdit`.

#### STO-16 · 렌즈와 스크립트
Priority **P1** · needsExternal **no**

- The script has no lens (R11 §3). Entering the script stores the current lens value; leaving the script restores it (R11 §3 table "보관했다가 스크립트를 나가면 되돌림").
- API `setLensToScene(sceneKey)`: sets the stored lens to the scene point (end of that scene) so that, when the user leaves the script, lens screens show that point. SCR-02 calls it from `렌즈를 이 씬으로 맞추기` (the only action on the `이 씬 · 1-7` label).
- In the command palette inside the script, `시점 바꾸기` is replaced by `렌즈를 이 씬으로 맞추기` (STO-38).

Sources: R11 §3; boards `Main`, `Search`.

### C. Scene 때 and project time settings

#### STO-17 · 때 표기 · 시간 설정
Priority **P0** · needsExternal **no**

- `세계 시간` mode (`연도만` default · `세계 달력` · `시간 없이`) and `이야기가 시작되는 해` (`1200`) are WLD's settings (row `세계 시간 | 연도만 · 시작 1200년 | 연도만 ›`), picked first in LIB-11. This area reads them: the start year drives `이야기 시작 5년 전` (STO-24); the calendar drives date labels (STO-18).
- `때 표기` (owned here, project-level, in `.civilight`): row `때 표기 | 1-7 · 2일째 · 새벽 · 연표는 기준 사건도 | 에피소드 기준 ›`. Options: `에피소드 기준` (default) · `이야기 시작부터` (the 연표 also offers `기준 사건`). New projects take the app default from 설정 › 지도 · 이미지 · 연표 › `때 표기 기본 | 새 프로젝트 · 프로젝트마다 바꾸기 가능 | 에피소드 기준 ›`.
- `시간 없이`: `ED` scenes still have 때 with `N일째` labels; only year/calendar features (연표 years, ages) are off. (§6 Q13)

Sources: MAIN 연표, 새 프로젝트; R11 §6; boards `ProjSettings`, `SettingsMore`, `NewTime`.

#### STO-18 · 세계 달력으로 보는 때
Priority **P1** · needsExternal **no**

- The calendar (months, weekdays, story start date) and its editor are WLD-30. A day is always 24 hours (R18 §2).
- With the calendar on, scene 때 labels show a calendar date for `day(X)` converted with WLD-30's date ↔ day functions; `대략` shows a date range; `N일째` stays as a secondary label (R11 §6). Stored 때 never changes when the calendar is turned on or off.
- Date label format is not drawn; `ED` `<month name> <d>일`, range `<month> <a>일 ~ <b>일`. (§6 Q14)
- Year-only mode needs no day → year conversion for scene labels; flashback years are entered as years.

Sources: MAIN 연표; R11 §6; R18 §2; WLD-30.

#### STO-19 · 하루와 시간대
Priority **P0** · needsExternal **no**

- Five bands partition the day (R18 §2): `새벽` 04:00–06:00 · `아침` 06:00–10:00 · `낮` 10:00–17:00 · `저녁` 17:00–20:00 · `밤` 20:00–다음 날 04:00. Ranges are half-open `[start, end)`.
- **The story day changes at 04:00** (`하루는 새벽 4시부터`); 밤 belongs to the day it started (01:30 is that day's 밤). Option `자정` (midnight). Changing the option never changes stored 때, only labels.
- Project settings row: `하루 · 시간대 | 하루는 새벽 4시부터 · 저녁 17–20`. Editing: only boundary times can change; bands stay contiguous and cover 24 h (moving one boundary moves the neighbour's). Editor copy: not drawn.
- `ED`: the day boundary is either 04:00 or 00:00 and does not follow edits of the 새벽 boundary. (§6 Q15)
- Exact time wins over band; a time outside its stored band (e.g. `새벽` + `07:00`) raises `◇ 시각과 시간대가 다름` (STO-40); the label keeps the stored band.

Sources: R18 §2; boards `ProjSettings`.

#### STO-20 · 씬 · 장소와 때 시트
Priority **P0** · needsExternal **no**

Opened from: the scene head tap (SCR), SCR panel `이 씬 · 장소와 때 | 바꾸기 ›`, structure scene ⋯ `장소와 때`, bulk list (`+ 장소` / `+ 때` cells). Cover = bottom sheet over the script (`SceneWhere`); inner = same content as a sheet over the side panel `ED`.

Content, top to bottom:
- Title `씬 3 · 장소와 때`, `닫기`.
- `장소 · ` + toggle link `지역으로 바꾸기` (switches between a pin place and a region). Value `결사의 문` (Nanum Myeongjo). `지도에서 고르기 ›` opens the world/map place picker. Deleted place: `지운 장소 · 잿더미 시장` dotted + `장소 고르기 · 되살리기 · 이대로 두기` (R20).
- `회상` toggle row (`RTFlashback`): `회상 | 연쇄 · 이동 점검에서 빠짐`. On → flashback form (STO-24).
- `때 · 직전 씬 기준 | 씬 2 · 1-7 · 1일째 저녁` (base label: `<base scene> · <its full label>`; when the base is not the directly previous scene: `1-7 씬 3 기준`).
- Day chips `같은 날 · +1일 · +2일 · 직접`. Larger relative intervals: input not drawn (§6 Q16); data supports any integer.
- Band chips `새벽 · 아침 · 낮 · 저녁 · 밤` + exact-time field (`05:40`, mono). Picking a time selects its band automatically for display.
- `정밀도 | 정확 · 대략`. `대략` stores a day range (STO-21).
- Result line: `1-7 · 2일째 · 새벽 | 이야기 약 13일째 · 연표에서` (full label + story-relative label + link to the 연표 at this scene).
- When the base is not the directly previous timed scene: button `기준을 직전 씬으로` (STO-21).
- When the result is earlier than the previous scene: line `직전 씬보다 앞 · 약 9시간 전` (STO-25).
- `등장 | 리엔 · 하르트 | (대사) | + 세라` (STO-26).
- `전환 문구 | 자동 · 직접 · 없음` (STO-27); `직접` reveals the text field.
- Travel warning block when a ▲ involves this scene (STO-30): `▲ 이동 시간 | 하르트 · 씬 2 상층 관문에서`, `9시간 40분 – 12시간 40분 · 도보 2일 4시간`, chips `+2일로 · 말 · 약 7시간 40분 · 맞음`.
- Changes apply when a control is changed; if the change alters the absolute time of a scene that has dependents, the cascade preview (STO-23) opens first and nothing changes until `바꾸기`.

Sources: R11 §4–§6; R18; R22 §7 (전환 문구 row); boards `SceneWhere`, `RTFlashback`, `TourWhen`, `CSheetTime`.

#### STO-21 · 때 저장: 기준 + 간격
Priority **P0** · needsExternal **no**

Each scene stores one `SceneTime` (§3), never an absolute value (R11 §4, replaces "값 하나로 저장"):
- `없음` — no 때.
- `기준` — base scene key + day interval (`같은 날` = 0, `+1일`, `+2일`, any integer incl. negative) + time-of-day (band, exact time, both, or none) + precision. 정확: one day value. 대략: a day range `[min, max]` (e.g. `며칠 뒤` = +5~9일).
- `직접` — a **fixed point**: absolute story day (`이야기 12일째`) or calendar date (STO-18), + time-of-day. Fixed points stop cascades.
- `회상` — flashback (STO-24), outside the chain.

Base selection rules:
- Default base = the nearest earlier (manuscript order) scene that has 때 and is not 회상. Its name is shown (`1-7 씬 3 기준 +2일`).
- The base never changes by itself when an in-between scene later gets 때. In that case the sheet shows `기준을 직전 씬으로`: sets base = the nearest earlier timed scene and recomputes the interval so the absolute stays.
- No earlier timed scene → only `직접` is enabled (first scene of the story).
- `대략` without an explicit range (chip picked with precision 대략): range input is not drawn. `ED`: chip value n → range `[n−1, n+1]` clipped at 0 for `+n`, `[0, 1]` for `같은 날`. (§6 Q17)
- Day intervals are in story days (STO-19 boundary). Entering a time under the `자정` option converts to the stored 04:00-based form (stored values never depend on the option).

Sources: R11 §4, §5; FMT §4 (때 row); board `SceneWhere`.

#### STO-22 · 때 계산과 표기 (engine)
Priority **P0** · needsExternal **no**

**Computation model** (makes R11 §5 hold: two scenes behind the same 대략 have an exact difference):
- Story day of scene X is a linear expression `day(X) = c_X + Σ_{i∈U(X)} u_i`, where `c_X` is the sum of minimum day intervals along X's base chain down to its fixed point, and `U(X)` is the set of 대략 links on that chain; each `u_i ∈ [0, max_i − min_i]` is a **named variable shared by every scene whose chain passes link i**.
- Time-of-day of X is its own range `t_X ∈ [t_min, t_max)` in minutes after the day start (04:00): exact time → a point; band only → the band range; nothing → the whole day `[0, 1440)`. Time-of-day is never inherited along the chain.
- Absolute minute `A(X) = (day(X) − 1)·1440 + t_X`.
- **Difference** `A(Y) − A(X)`: cancel shared variables, then take interval bounds over the remaining variables and both `t` ranges. Never subtract two independently computed ranges (that would give `−3~5일` for 1-7 씬 2 → 씬 3, R11 §5).
- Scenes with no 때 or 회상 have no absolute time and are skipped by checks.

**Labels** (R11 §6, R22 §2):
- Episode-relative day: `N = day(X) − day(F) + 1`, F = the first scene of X's episode (manuscript order) that has non-flashback 때. Exact → `2일째`; uncertain (variables not shared with F) → `약 N일째`.
- Story-relative: `이야기 N일째`; uncertain → `약 N일째 (a~b)` in full contexts, `약 N일째` in compact ones. `N` = midpoint of `[a, b]`, `ED` .5 rounds up.
- Anchor-relative (연표 `기준 사건` view; world owns the view): `기준`, `−1일`, `+약 7일` (range shown as `5~9일` in the sub line).
- Band word: the stored band, else the band containing the exact time.
- **Short form** (lists, script header, story time column): `2일째 새벽`. **Scene head** form: short + exact time when present: `2일째 아침 06:10`. **Full form** (one scene referenced): `1-7 · 2일째 · 새벽`. No 때: `—` in the story list, `때 없음` in the bulk list, `장소 없음 · 때 없음` in a scene head with neither. 회상: `회상 · 1195`. 1-level projects use the story-relative form (R11 §6).
- `시간 없이`/year-only → `N일째`; calendar on → date label (STO-18), N일째 secondary.

Ordering API: `compareByTime(X, Y)` by `A_min`, ties and overlaps broken by manuscript order. `ED` (§6 Q18). Used by 동선, `동시에 두 장소`, `같은 때 두 장소` (R11 §5: those use time order, not manuscript order).

Sources: R11 §4–§6; R18 §2, §4; R22 §2; FMT §4.

#### STO-23 · 뒤 씬 밀림 미리 보기 (cascade)
Priority **P0** · needsExternal **no**

- Trigger: a change to scene S's 때 that changes `A(S)` while some scene depends on S (transitively, through base links). No dependents → apply directly.
- Preview sheet (`RTCascade`):
  - Title `씬 1 · 때 바꾸기`; line `1-7 씬 1 · 첫 번째 깜박임 · 1-6 씬 3 기준`.
  - Before → after: `약 12일째 새벽 →` `약 14일째 새벽`, sub `+2일 · 이야기 날`.
  - Summary `뒤 씬 12개 +2일 · 1-9 씬 2(직접)에서 멈춤` (count = affected dependents; the stop names the next fixed point after them in manuscript order).
  - Up to 4 rows `1-7 씬 2 | 약 12일째 저녁 | → | 약 14일째 저녁`, then `외 7개 · 1-8 씬 2부터 1-9 씬 1까지 · 모두 +2일`, then the fixed point row `1-9 씬 2 | 직접 · 그대로 | 고정점`.
  - Radio `뒤도 같이` · `기본 · 뒤 씬 12개 +2일` (default) / `이 씬만` · `뒤 씬 때 그대로 · ◇ 순서와 때가 다름 1`.
  - Buttons `취소` · `바꾸기`.
- `뒤도 같이`: store S's new 때; dependents keep base + interval, so they shift by the same delta (range-aware). Fixed points do not move; a fixed point now earlier than a shifted scene before it gets the order/time notice (STO-25).
- `이 씬만`: store S's new 때; for each **direct** dependent D, recompute D's interval so `A(D)` is unchanged (may become negative). The `◇ n` count = scenes that would raise STO-25 notices. IDX: after `이 씬만` the result ends as `순서와 때가 다름` rows in 점검 센터 and nothing else. The option is always offered (IDX supersedes R11 §4 "간격이 0보다 작아지면 버튼을 내지 않음", §6 Q19).
- Moving, inserting, deleting or restoring scenes never cascades (STO-05, 08, 10).
- The whole cascade is one undo step.

Sources: R11 §4; IDX; board `RTCascade`.

#### STO-24 · 회상
Priority **P1** · needsExternal **no**

- Toggle `회상 | 연쇄 · 이동 점검에서 빠짐` in the 장소와 때 sheet (`RTFlashback`). On: the scene leaves the chain.
- Form: line `때 · 기준 | 회상은 직전 씬 기준 없음`; segmented `기준 사건 | 직접`.
  - `기준 사건`: row `기준 사건 | 대정전 | 1195 | 바꾸기 ›` (picks a world event); segmented `전 | 그날 | 후`. Amount for 전/후: copy not drawn (§6 Q20).
  - `직접`: year (and date when the calendar is on).
- Result: `연도 1195 · 대정전 그날 | 이야기 시작 5년 전 · 연표에서` (years = start year − flashback year).
- Line `씬 3 때 기준 | 씬 1 | · 회상 건너뜀`: scenes after a flashback take the nearest earlier non-flashback timed scene as base.
- Labels: scene head `씬 2 · 하층 거리 · 회상 · 1195`; script header `1-4 · 씬 2 · 회상 · 1195`. Not counted for `N일째`. Excluded from travel checks and 동선; 이곳의 시간 places it in its era (world).
- Split of a flashback scene keeps 회상 on the new scene (STO-09); merge of mixed flashback is blocked.

Sources: R11 §4, §6; R22 §2; board `RTFlashback`.

#### STO-25 · 순서와 때가 다름
Priority **P1** · needsExternal **no**

- Out-of-order time is allowed (cross-cutting); it is never a ▲ (R11 §5).
- Definition: let P be the nearest earlier (manuscript order) non-flashback timed scene of X. If `max(A(X) − A(P)) < 0`, X is **definitely earlier** → notice. If the range straddles 0 → no notice.
- In the 장소와 때 sheet, at the moment of picking: `직전 씬보다 앞 · 약 9시간 전` (duration = midpoint of `A(P) − A(X)` formatted with STO-29 rules; `약` when either side is a range) `ED` for the midpoint.
- 점검 센터 row `◇ 순서와 때가 다름` (filter STO-40), closed with `맞음`.
- The same condition after moving a scene, after a cascade pushes scenes past a fixed point, or after `이 씬만` is reported by the same row. Rule 11 calls the per-scene marker `◇ 때 확인`; R12/RSSceneMove say `순서와 어긋남`. `ED`: one check type; row title `순서와 때가 다름`, count fragment in previews `◇ n 순서와 어긋남` / `◇ 순서와 때가 다름 n` exactly as drawn. (§6 Q21)

Sources: R11 §4, §5; IDX; boards `RTCascade`, `RSSceneMove`.

#### STO-26 · 등장
Priority **P1** · needsExternal **no**

- 등장 of a scene = speakers of its dialogue blocks (automatic, a fact) ∪ silent cast (added by hand).
- Sheet row: `등장 | 리엔 · 하르트 | (대사) | + 세라`: automatic names followed by `(대사)`, then silent members, then `+` opens the speaker picker (SCR's picker, search + tags). Silent members can be removed; automatic ones cannot (they follow the blocks). Removal control copy: not drawn.
- Used by travel checks and 동선. Deleted / hidden members: R20 / R19 rendering (STO-11).

Sources: MAIN 지도 ↔ 연표; R22 §5; board `SceneWhere`.

#### STO-27 · 전환 문구 row and inputs (for SCR-48)
Priority **P1** · needsExternal **no**

SCR-48 owns 전환 문구 storage, the settings row `전환 문구 | 새 씬 기본 없음 · 틀 장소 · 시각 | 바꾸기 ›`, auto text, the direct-text snapshot and the checks it raises (SCR-62). This area owns the row `전환 문구 | 자동 · 직접 · 없음` inside the `장소와 때` sheet (choosing `직접` reveals SCR-48's text field) and supplies the inputs: resolved place name and resolved time (`ResolvedTime`, STO-22) used by auto text and by the snapshot comparison. The table below restates R22 §2 / SCR-48 so the sheet can preview the result; SCR-48 is authoritative.
- State per scene: `자동` · `직접` · `없음`; new scenes take the project default (factory `없음`).
- Auto text (template `장소 · 시각`):

  | Scene properties | Auto text |
  | --- | --- |
  | place + exact time | `결사의 문 · 06:10` |
  | place + band only | `결사의 문 · 아침` |
  | 대략 (with or without band) | `잿더미 시장` (place only, SCR-48) |
  | region + band | `하층 구역 · 새벽` |
  | no place | none; `자동` not selectable |
  | 회상 | from the flashback's own place/time; the word `회상` is never added |

  Template `장소만`: place name only. Scene ordinals and `N일째` never appear in auto text.
- `직접`: author's text, never parsed. SCR-48 snapshots place + resolved time when it is written. Switching away from 직접 keeps the text until it is overwritten (switching back restores it).
- Checks (emitted by SCR-62, listed in STO-40): `◇ 전환 문구 · 쓴 뒤 때 바뀜 · 1-7 씬 4 · 06:10 → 07:00` (or `장소` for a place change) when the fingerprint differs, buttons `자동으로 · 문구 고치기 · 맞음`; `◇ 공개 전 장소 · 전환 문구` when the place's map `보이는 시점` is after this scene (auto text is still produced), buttons `직접 쓰기 · 맞음`. Terms inside a 직접 text take part in the term reveal check (world).
- Background direction cues (`▸ 배경`) are not a source of truth and are never compared with the scene place (R22 §2).

Sources: R22 §2, §3, §6, §7; boards `RSHead`, `RSHeadMenu`, `SceneWhere`, `ProjSettings`, `CSheetTime` (`전환 문구 없음 ·`).

### D. 이동 점검 (travel check)

#### STO-28 · 이동 수단 고르기 (mode per pair)
Priority **P1** · needsExternal **no**

- The mode list is WLD-47: name + **daily distance (km)** + **daily moving hours** (R18 §3); factory `도보` 15 km · 8시간, `말` 40 km · 8시간, `배` 30 km · 24시간 (`밤에도 감`), plus author-defined `직접` modes; row `이동 수단 | 도보 15 km · 8시간 · 말 40 km · 8시간 · 배 30 km · 24시간 | 바꾸기 ›`.
- Mode used for a pair (in this order): the per-segment override (STO-30, stored on character + two scenes) → the character's `기본 이동 수단` field if the project defines one (cast) → project default `도보`. The app never picks a faster mode by itself.

Sources: R18 §3; WLD-47; board `ProjSettings`.

#### STO-29 · 이동 점검 계산 (engine)
Priority **P1** · needsExternal **no**

**Pairs.** For each character, take the scenes where the character is in 등장 (STO-26), excluding 회상, scenes with no 때, scenes with no place (they raise `◇ 장소 없음` instead), and scenes whose place is deleted (R20). Order them with `compareByTime` (STO-22). Each consecutive pair (X → Y) with different places is checked.

**Distance** (`distanceKm`, WLD-66): route length if a route connects the two places; else straight-line distance (a lower bound) with flag `경로 없음`; region involved → nearest points of the two areas; child-map pins → their ancestor pins on the common parent map; same place → 0 (no check); map without scale or node map → `◇ 축척 없음 · 점검 못 함` + `축척 맞추기` (→ map). No detour multiplier.

**Required time** (must equal WLD-66 `requiredMinutes`) for distance d with mode (km/day k, hours/day h):
`full = floor(d / k)`; `rest = d − full·k`; `required = full·24 h + rest / k · h`.

**Gap** between X and Y (STO-22 difference): `gapMin`, `gapMax`. Manuscript order is stronger: if `gapMin < 0 ≤ gapMax`, set `gapMin = 0`.

**Verdict** (R18 §5): no warning if `required ≤ gapMax`. Otherwise ▲, named:
1. `동시에 두 장소` if both scenes have exact times and `gapMin = gapMax = 0`;
2. else `같은 때 두 장소` if both scenes are on the same story day and have the same band;
3. else `이동 시간 부족` (when the distance is a straight line, add `직선으로도 부족 · 경로 없음`).

**Suggestions** (shown only when ▲, computed, nothing changes until tapped):
- Time chip `<later scene> · +k일로`, `k = ceil((required − gapMin) / 24 h)`: delays the later scene by k days. Label uses `씬 3` inside the same episode, `1-8 씬 1` across episodes `ED`.
- Mode chips: every mode m (other than the current one) with `required_m ≤ gapMin`, label `<mode> · <required_m>` (e.g. `말 · 약 7시간 40분`). None fits → no mode chip.
- `맞음`.

**Duration format** (R18 §3 examples; general rules `ED`; WLD's `formatDuration` must produce exactly these strings):
- ≥ 24 h: `D일 H시간` (H rounded to the nearest hour; `H = 0` → `D일`). No `약`.
- < 24 h, required time: `약 H시간 M분` with M rounded to the nearest 10 minutes (`M = 0` → `약 H시간`).
- Gaps are exact: `H시간 M분` (`M = 0` → `H시간`); a range is `<min> – <max>`; ≥ 24 h gap → `D일 H시간 M분`. Decimal days (`2.5일`) are never shown.

**Recompute** on any change of 때, place, route, scale, 등장, mode or band settings.

Sources: R18 §2–§6; R11 §5; R20 §2; boards `TravelCheck`, `SceneWhere`.

#### STO-30 · 이동 점검 카드 · 제안 칩 · 맞음
Priority **P1** · needsExternal **no**

Card in 점검 센터 filter `이동` (`TravelCheck`):
- `▲ 이동 시간 부족 | 하르트` (warn color, character name right-aligned in `ink2`).
- Route line `1-7 씬 2 상층 관문 → 씬 3 결사의 문`.
- `TravelBar`: rows `필요 | <bar warn 100%> | 도보 2일 4시간` and `있음 | <bar acc, width = gapMax / required> | 9시간 40분 – 12시간 40분`; both bars on the same hour scale.
- Detail `경로 38 km · 도보 15 km · 하루 8시간 · 1일째 저녁(17–20) → 2일째 05:40`.
- Chips `씬 3 · +2일로` · `말 · 약 7시간 40분` · `맞음`.
- Compact rows for further items: `▲ | 같은 때 두 장소 · 세라 | 1-7 씬 2 기록실 · 씬 4 잿더미 시장 · 둘 다 2일째 아침`; a range-driven shortage is a compact `이동 시간 부족` row whose detail starts `대략인데도 모자람 · <name>` and ends `가장 길게 잡아도 부족` (board row; §6 Q22); `◇ | 장소 없음 · 씬 3개 | 1-8 씬 1 · 1-8 씬 2 · 1-9 씬 1 · 동선 · 점검에서 빠짐` with `한꺼번에 ›` (→ STO-31 with those scenes pre-selected).

Chip actions:
- Time chip → opens the cascade preview (STO-23) for the later scene with its 때 delayed by k days (interval + k for 기준 scenes, day + k for 직접). Nothing changes until `바꾸기`.
- Mode chip → stores the per-segment override (character · X · Y · mode) immediately; the check recomputes and the ▲ disappears. Undo toast `ED` (copy not drawn).
- `맞음` → acknowledgement (STO-41) on (check type, character, X, Y) with a fingerprint of both scenes' 때 and places, the route and the mode. Any change of those re-opens the ▲ (R18 §5).

The same block appears in the 장소와 때 sheet of either scene (STO-20) and as a one-line link in SCR's `시간` tab (`▲ 하르트 이동 시간 · 점검 센터`).

Sources: R18 §5, §7; boards `TravelCheck`, `SceneWhere`, `CSheetTime`.

### E. 장소 · 때 한꺼번에

#### STO-31 · 씬 고르기 (bulk list)
Priority **P1** · needsExternal **no**

Entry: structure ⋯ `장소 · 때 한꺼번에`; 점검 센터 `◇ 장소 없음 › 한꺼번에 ›`. Cover board `BulkList`:
- Head: `잿빛 새벽의 등불 · 1장` / title `장소 · 때 한꺼번에`, right `모두 고르기`.
- Progress: `장소 | 22/31` and `때 | 19/31` with flat bars (no rings) — filled scenes / scenes in scope.
- Filter chips `전체 31` · `장소 · 때 빈 것 12` · `제안 있음 3` (single choice).
- List grouped by episode code (`1-6`, `1-7` …): rows `씬 1 · 잿더미 시장` + values `잿더미 시장 | 1일째 낮`; empty values `장소 없음 | 때 없음` in `ink3`; partial `장소 없음 | 저녁 · 대략`. Each row has a rounded-square checkbox.
- Footer: `고른 씬 4개 · 1-6 씬 2 · 1-8 씬 1 · 1-8 씬 2 · 1-9 씬 1` and buttons `장소 정하기` · `때 정하기` · `제안 받기 3` (count = scenes in the selection with suggestions; disabled when 0 `ED`).
- Scope: `ED` the chapter shown in the head (sources show `· 1장`); a chapter switcher is not drawn (§6 Q23).

Sources: MAIN 장소 · 때 한꺼번에; board `BulkList`.

#### STO-32 · 장소 정하기
Priority **P1** · needsExternal **no**

Sheet `장소 정하기 · 씬 4개` (`BulkPlace`), `닫기`:
- Segmented `모두 같은 장소 | 씬마다 차례로` and `장소 | 지역`.
- Place list with scene counts: `잿더미 시장 | 씬 4개` (selected shows `고름`), `결사의 문 | 씬 6개`, `하층 거리 | 씬 5개`, `상층 관문 | 씬 3개`; ordered by scene count desc `ED`.
- `새 장소 | 지도에 핀을 꽂으며 만들기` (→ map pin creation, world).
- Footer result line + action: `씬 4개 → 잿더미 시장`.
- `씬마다 차례로`: steps through the selected scenes one at a time with the same list (step UI copy not drawn).
- Applying is one undo step.

Sources: MAIN; board `BulkPlace`.

#### STO-33 · 때 정하기
Priority **P1** · needsExternal **no**

Sheet `때 정하기 · 씬 4개` (`BulkWhen`), `닫기`:
- Segmented `직전 씬 기준 차례로 | 모두 같은 때`.
- `씬 사이`: chips `같은 날 · +1일 · +2일 · 씬마다 고르기`.
- `시간대`: chips `그대로 · 비워 둠 · 새벽 · 아침 · 낮 · 저녁 · 밤` (`그대로` keeps each scene's band; `비워 둠` clears it).
- `정밀도 | 정확 · 대략`, default **대략** (`대략으로 넣으면 점검은 넓게 · 나중에 씬에서 정확히`).
- `미리 보기` rows, one per selected scene: `<scene> | <full label> · 대략 | <base note>`, e.g. `1-6 씬 2 | 1-6 · 2일째 · 대략 | 직전 1-6 씬 1 · 1일째 낮`, `1-8 씬 1 | 1-8 · 1일째 · 대략 | 1-7 씬 3 기준 · 직전 씬 때 없음`, `1-8 씬 2 | 1-8 · 2일째 · 대략 | 직전 1-8 씬 1`, `1-9 씬 1 | 1-9 · 1일째 · 대략 | 직전 1-8 씬 2`.
- Button `씬 4개에 넣기`.

Rules:
- `직전 씬 기준 차례로`: each selected scene gets base = its nearest earlier timed non-flashback scene **after earlier selected scenes have been given 때** (so a chain forms through the selection), interval = the chosen chip, band per `시간대`, precision per toggle (`대략` + chip: range per STO-21 `ED`).
- `씬마다 고르기`: per-row chip choice inside the preview (copy not drawn).
- `모두 같은 때`: `ED` the first selected scene gets the chosen relation to its base; every other selected scene gets `<first selected> 기준 · 같은 때`.
- Scenes that already have 때 are overwritten only if selected; overwriting a scene with dependents does not cascade-preview per scene; the bulk preview is the confirmation and dependents keep base + interval (they shift) `ED` (§6 Q24).
- One undo step for the whole application.

Sources: MAIN; R11 §4; R18 §7 (band chips 5); board `BulkWhen`.

#### STO-34 · 제안 받기
Priority **P1** · needsExternal **no**

Sheet (`BulkSuggest`): head `장소 · 때 한꺼번에 · 고른 씬 4개` / title `제안 받기`; line `원고의 배경 연출 · 전환 문구 · 본문에서 읽음 · 넣기 전엔 바뀌지 않음`.

Per selected scene, one card:
- Header `<scene> · <title> | <source>` where source ∈ `배경 연출` · `전환 문구` · `본문`.
- Evidence: the source sentence in full, matched parts boxed (`mark.hit`), e.g. `▸ 배경 | 잿더미 시장 | ( | 밤 | )`, `“이틀 뒤, 송전탑 그림자가 길게 누운 새벽.”` with `이틀 뒤`, `송전탑`, `새벽` boxed, `기록실 · 같은 날 밤` (direct transition text).
- Proposal line: `잿더미 시장 | 같은 날 · 밤`; `송전탑 폐허 | +2일 · 새벽`; `기록실 | 같은 날 · 밤 | · 장소 카드 없음 → 새 장소`.
- Buttons `넣기` · `고치기` (opens STO-20 pre-filled) · `건너뛰기`.
- No result: `1-9 씬 1 · 정체 | 제안 없음` / `배경 · 전환 문구 · 본문에서 찾지 못함 → 직접`.
- Footer `닫기` · `제안 3개 모두 넣기`.

Reading rules (this is the only place the app reads prose for place/time, R22 §6):
- Sources, in priority order `ED`: (1) a `▸ 배경` direction block in the scene whose asset is linked to a place (asset ↔ place link, world/SCR assets) — place = that place, band = the parenthesised band word in the cue; (2) the scene's **직접** transition text (auto text is never a source); (3) the scene's game-output body text.
- Place match: place names and aliases, matched like rule 21 (word start, fixed particle list, longest wins, 1-syllable names skipped). Unknown place-like word: only from a 직접 transition text's first fragment before ` · ` (`ED`), flagged `새 장소`.
- Time phrases `ED` lexicon (extendable): `같은 날` → 같은 날; `다음 날`, `이튿날` → +1일; `이틀 뒤` → +2일; `사흘 뒤` → +3일; `며칠 뒤` → 대략 +5~9일 (R11 sample); band words `새벽 · 아침 · 낮 · 저녁 · 밤`. Band only → `같은 날 · <band>`.
- `넣기` stores the proposal (precision `대략` default as in STO-33 `ED`); for `새 장소` it first creates a place card with that name and no pin `ED`. The 직접 transition text is left unchanged. Every 넣기 is undoable; `모두 넣기` is one undo step.

Sources: MAIN 장소 · 때 한꺼번에; R22 §6; R21 §3 (matcher); board `BulkSuggest`.

#### STO-35 · 표로 한꺼번에 (inner)
Priority **P1** · needsExternal **no**

Inner layout (`BulkWide`, 800×800): head `잿빛 새벽의 등불 · 1장 · 펼친 화면` / `장소 · 때 한꺼번에` / right `장소 · 때 빈 것 12`; progress `장소 22/31 · 때 19/31`.
- Table columns `씬 | 장소 | 때 | 제안`. Rows `1-6 씬 2` / `오웬의 거래 | + 장소 | + 때 | 배경`; filled cells show values; empty cells are `+ 장소` / `+ 때` buttons (open STO-20 for that scene); the `제안` column shows the source word (`배경` · `본문` · `전환 문구`) or `—`.
- Right: `장소별 씬 수` map with per-place counts (`6 · 5 · 4 · 3`). Note `핀으로 끌어다 놓기 대신 · 고른 씬 → 핀 탭 → 장소 정하기`: with scenes selected, tapping a pin assigns that place (no drag).
- Footer `고른 씬 4개 | 장소 정하기 | 때 정하기 | 제안 받기 3`.

Sources: MAIN; board `BulkWide`.

### F. 명령 칸 and search

#### STO-36 · 명령 칸
Priority **P0** · needsExternal **no**

- One command bar at the bottom of every project screen (no tab bar). Layout: `/` glyph (mono), placeholder, right mic button (`pri` fill) → Life Recorder (recorder area). Height 52, border `edge`, fill `tint` (`CommandBar`).
- Placeholders per screen: script `명령 또는 검색 · 인물 · 블록 · 씬 이동`; home `명령 또는 검색 · 인물 · 용어 · 대사`; story `명령 또는 검색 · 씬 이동`; check centre and profile-like screens `명령 또는 검색`; 연표 `사건 · 연도 찾기` / `사건 · 때 찾기` (anchor view); page `@로 인물 · 용어 연결, /로 블록 추가` (page editor, world).
- Tap → search sheet (STO-37). Typing `/` + shortcut in the script inserts blocks (SCR-12).

Sources: MAIN 공통 개념 명령 칸; DS `CommandBar`; boards `Home`, `Story`, `Check`, `Timeline`, `TimelineAnchor`, `Main`.

#### STO-37 · 검색
Priority **P0** · needsExternal **no**

Sheet (`Search`): full-height glass over the blurred screen. Query field `/ <query>` with caret and `닫기`; filter chips `전체 · 인물 · 용어 · 대사 · 씬 · 명령` (single choice, default `전체`).
- Sections in chip order, each with header (`용어`, `대사 · 3`, `명령` …); empty sections omitted.
- Under `전체` only, names of 장소, 세력 and 연표 사건 are also searched (WLD hand-off), in sections headed `장소`, `세력`, `사건` `ED`; tap opens the card / event.
- Item rows: 인물 (name, one-line intro `ED`), 용어 (name with hit box, sub `첫 공개 1-7 #044 · 장소: 하층 구역`, right `카드`), 씬 (`1-7 씬 3 · 꺼진 등불` `ED` sub = short 때 label), 대사 (all game-output blocks incl. narration and system text): location label `1-7 씬 3 · 나레이션` / `1-4 씬 2 · 세라` (small), then the **whole sentence** containing the hit as one serif line with the hit boxed inline (`mark.hit`: `hl` fill + 1 px `acc`), never broken onto its own line; flag `▲ 공개 전 사용` right of the label when that block holds a term link before its reveal (R21: search itself is plain text).
- Sentence = the text between sentence terminators (`. ? ! … ⋯` followed by space, closing quote or end) around the hit; quotes kept; short blocks show whole `ED`.
- Order inside 대사: latest story position first (board order 1-7, 1-4, 1-2) `ED`.
- Matching: NFC-normalised, Latin case-insensitive substring over names, aliases, titles and block text. **초성**: a query of only Hangul compatibility consonants (`ㄱ…ㅎ`) matches the initial-consonant string of names, aliases, scene titles and block text `ED` (e.g. `ㅇㄹㅈ` → 오리지늄 등불).
- Tap: 용어 → term card; 인물 → profile; 씬 → script at the scene head; 대사 → script with that block selected; 명령 → run (STO-38).
- Exclusions: deleted items (R20); locked hidden items: card results removed, text in visible scenes still found, a final line `숨긴 항목 제외` (R19 §6); hidden scenes' text not searched while locked. Cross-project search (서재) scope: LIB-07.
- Empty (`EmptySearch`): `결과 없음`, `‘오리지누’ · 숨긴 프로젝트 제외` (`숨긴 항목 제외` inside a project `ED`), `비슷한 이름` with rows like `오리지늄 | 등불 | 용어 · 첫 공개 1-7`, action `‘오리지누’ 용어로 만들기` (→ world term creation with that name). Similar names `ED`: names/aliases whose syllable edit distance to the query ≤ 1 or that start with the query's first two syllables.
- Motion (`MSearch`): on results update rows stay; the hit box draws from the left, 60 ms stagger. Reduce motion → 120 ms fade.

Sources: MAIN 검색; feedback search-highlight; R19 §6; R20 §2; R21 §6/§7; boards `Search`, `EmptySearch`, `MSearch`.

#### STO-38 · 명령
Priority **P1** · needsExternal **no**

Commands shown in the `명령` section (rows `› <command> | <sub>`), matched by text:
- `› 시점 바꾸기 | 지금: 최신 · 1-7` → lens sheet (STO-13). Not offered in the script.
- `› 렌즈를 이 씬으로 맞추기` (script only; sub copy not drawn) → STO-16.
- `› 점검 센터 열기 | 확인할 항목 3` (count = open ▲ + ◇ items) → STO-39.
- `번호 다시 매기기 · 1-7` → SCR renumber sheet (R12 §5).
- Navigation: typing a scene code or title (`1-7 씬 3`) lists the scene; typing `#043` inside an episode lists that block `ED`.
- Script block shortcuts (`/대 /나 /연 /선 /씬 /시 /메`) are SCR-12/SCR-68.

Sources: MAIN 공통 개념; R11 §3; R12 §5; board `Search`.

### G. 점검 센터

#### STO-39 · 점검 센터 화면 (shared with CNV-17)
Priority **P0** · needsExternal **no**

CNV-17 specifies the same shell; implement it once. This restatement adds the scanning state, ordering and the travel card; any difference is listed in §6 Q35.

Cover (`Check`), no lens chip:
- Head `잿빛 새벽의 등불` / `점검 센터`; sub line `기록과 원고가 어긋난 곳`.
- Filter chips (single choice): `전체 N` · `용어 공개` · `화자` · `이동 N` · `보이스` · `변화` · `지운 항목` · `맞다고 표시한 것`. `전체` counts open ▲ and ◇ (R21 §5). Chips with a non-zero count may show it (`이동 4`).
- Item row: marker + kind in `warn` (▲) or `ink3` (◇) and the location right-aligned (`1-4 씬 2`, `인물 · 리엔`, `휴지통`); body in Nanum Myeongjo (quoted sentence with links/hits underlined); small detail line; action buttons in one row (first = primary text color, then `ink2`, last `ink3`). Exact rows: STO-40.
- Order: ▲ before ◇ within a filter; then story order of location `ED`.
- Tap the location → open the script at that block/scene (inner: right pane, STO-43).
- Scanning (project open or file restore, R21 §7): chip counts dimmed and a `점검 중` label until the scan ends.
- Hidden items (locked): computed normally but collapsed into one row `▲ 숨긴 항목 관련 2 · 지문으로 보기` (biometric unlock, LIB); unlocked: original rows.
- Empty (`EmptyCheck`): chips `전체 0 · 용어 공개 · 화자 · 보이스 · 변화`, `어긋난 곳 없음`, `마지막 확인 · 방금 · 1-1 ~ 1-7` (range = first to last episode with content), buttons `원고로` · `맞다고 표시한 것 2`.
- `맞다고 표시한 것` filter lists acknowledged items with `되돌리기` per row (R21 §6 table) — row layout copy otherwise not drawn.

Sources: MAIN 점검 센터; R19 §6; R20 §3; R21 §5–§7; boards `Check`, `TravelCheck`, `EmptyCheck`, `TwoCheck`, `ColCheck`.

#### STO-40 · 점검 종류 (registry)
Priority **P0** · needsExternal **no**

The registry lists every check type, its filter, row copy and buttons. "Emitted by" = the area that computes it; this area renders, counts and stores acknowledgements.

| # | Marker · title | Filter | Row content (example) | Buttons | Acknowledged on | Emitted by · source |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | `▲ 공개 전 사용` | 용어 공개 | `1-4 씬 2`; `세라: “[오리지늄]은 타지 않아. 빛만 남지.”`; `‘오리지늄 등불’ 첫 공개 1-7 · 사용 1-4`. Several for one term collapse to `잿불의 서약 · ▲ 3` (expand) | `원고로 가기` · `공개 시점 옮기기` · `맞음` | the link (survives reveal move; gone with the link) | world · R21 §5, §6 |
| 2 | `◇ 공개 전 후보` | 용어 공개 (under ▲) | `◇ 공개 전 후보 · 1-2 씬 1 · 리엔: “등불 속 그 돌, [오리지]… 뭐라고 했죠?” · ‘오리지늄 등불’ 끊김 · 첫 공개 1-7` | `원고로 가기` · `잇기` · `아님` | `아님` stored by world | world · R21 §5 |
| 3 | `◇ 첫 공개 없음` | 용어 공개 | `첫 공개 없음 · 쓰인 곳 3`; chip `1-2 씬 1을 첫 공개로` | chip | — | world · R21 §5 |
| 4 | `▲ 화자 없음` | 화자 | `1-7 씬 2`; `“…누구야?”`; `대사 블록 1개` | `화자 고르기` · `나레이션으로 바꾸기` | block | SCR-62 · MAIN |
| 5 | `▲ 보이스 비어 있음` | 보이스 | `인물 · 리엔`; `리엔 · 신뢰 상승 후`; `보이스 6/24 비어 있음` | `보이스 쓰기` | — | cast · MAIN |
| 6 | `◇ 형식과 다른 ID 3` | 보이스 | voice ID whose character or episode part differs | `새 ID로` · `그대로` | block | SCR-62 · R12 §6 |
| 7 | `▲` duplicate voice ID (format `직접` only) | 보이스 | copy not drawn | copy not drawn | block | SCR-62 · R12 §6 |
| 8 | `◇ 근거 지워진 변화 1` | 변화 | dotted ◆; `◆ 관계 리엔 → 하르트 · 근거 #043 지워짐`; several → `모두 지우기` | `다른 블록에 잇기` · `이대로 두기` · `기록 지우기` | record | SCR-62 · R12 §2 |
| 9 | `◇ 지운 인물 · 노아` (also `지운 장소 · …`, `지운 용어 · …`, `지운 세력 · …`) | 지운 항목 | `휴지통`; `3일 남음`; `대사 14 · 씬 등장 2 · @ 3 · 레코더 1`; for terms an inner `▲ 1 보류`; after permanent delete `영구 삭제 · 노아` | per reference kind, e.g. `화자 한꺼번에 바꾸기` · `되살리기` · `이대로 두기` (no `되살리기` after permanent delete) | item | cast/world · R20 §3 |
| 10 | `▲ 이동 시간 부족` | 이동 | STO-30 card | `씬 3 · +2일로` · `말 · 약 7시간 40분` · `맞음` | character + two scenes (fingerprint) | here · R18 §5 |
| 11 | `▲ 같은 때 두 장소` | 이동 | `세라 · 1-7 씬 2 기록실 · 씬 4 잿더미 시장 · 둘 다 2일째 아침` | as 10 | as 10 | here · R18 §5 |
| 12 | `▲ 동시에 두 장소` | 이동 | as 11 with the exact time | as 10 | as 10 | here · R18 §5 |
| 13 | `◇ 장소 없음 · 씬 3개` | 이동 | `1-8 씬 1 · 1-8 씬 2 · 1-9 씬 1 · 동선 · 점검에서 빠짐` (one row for all) | `한꺼번에 ›` | — | here · R18, MAIN |
| 14 | `◇ 축척 없음 · 점검 못 함` | 이동 | pair that could not be checked | `축척 맞추기` | — | here · R18 §6 |
| 15 | `◇ 시각과 시간대가 다름` | 이동 `ED` | `<scene> · 새벽 07:00` `ED` | copy not drawn (`ED`: `맞음`) | scene | here · R18 §2 |
| 16 | `◇ 순서와 때가 다름` | 이동 `ED` | `<scene> · 직전 씬보다 앞 · 약 9시간 전` `ED` | `맞음` (+ `원고로 가기` `ED`) | scene + previous scene (fingerprint) | here · R11 §5, IDX |
| 17 | `◇ 전환 문구 · 쓴 뒤 때 바뀜` (or `장소 바뀜`) | 이동 `ED` | `1-7 씬 4 · 06:10 → 07:00` | `자동으로` · `문구 고치기` · `맞음` | scene (fingerprint) | here · R22 §2 |
| 18 | `◇ 공개 전 장소 · 전환 문구` | 이동 `ED` | scene + place | `직접 쓰기` · `맞음` | scene | here · R22 §2 |
| 19 | `◇ 숨긴 인물이 보이는 씬에 나옴 · 씬 3` | 화자 `ED` | | `씬도 숨기기` · `그대로` | scene + character | LIB/cast · R19 §6 |
| 20 | `▲ 숨긴 항목 관련 N · 지문으로 보기` | all | locked aggregate | `지문으로 보기` | — | here · R19 §6 |

Filter for 15–18: R22 leaves the name open (`이동` or a new `씬`); `ED` `이동` (§6 Q25). Rows 17–18 are emitted by SCR-62 (transition data is SCR's). CNV-17's registry lists the same kinds; for rows 11–12 it keeps MAIN's older buttons `때 고치기 · 장소 고치기 · 맞음`, this spec uses R18 §5 (same calculation, same chips) — §6 Q35.

Not routed to 점검 센터 (shown in place only): `◇ 넣은 뒤 고침` (recorder, R23 explicit); `▲ 숨긴 씬으로 가는 선택지 1` (export sheet, R19 §5); `◇ 같은 이름 2 · 노아` (restore, R20 §5); relation kind deleted ◇ (R13 §3); unconvertible field value ◇ (인물 항목). (§6 Q26)

Settings (STO-42) can switch off rows 10–12 and row 13.

Sources: MAIN 점검 센터; R11; R12; R18; R19; R20; R21; R22; boards `Check`, `TravelCheck`.

#### STO-41 · 맞음 · 이대로 두기 (acknowledgements)
Priority **P0** · needsExternal **no**

- `맞음` (accept a ▲/◇ as intended) and `이대로 두기` (R12/R20: keep a dangling reference) both create an `Acknowledgement` (§3) and move the item to `맞다고 표시한 것` (R20 §3 table: 이대로 두기 → 맞다고 표시한 것).
- Each check type defines its **subject** (keys) and a **fingerprint** of the inputs that made it fire (table above). If the fingerprint changes, the acknowledgement is void and the item returns open (R18 §5 "때나 장소, 경로가 바뀌면 표시가 풀려"). If the subject is deleted, the acknowledgement goes with it (R12 §1: 맞음 on a block disappears with the block, moves with it); undoing that delete restores the acknowledgement; restoring the subject later from trash re-evaluates the item fresh `ED`.
- `되돌리기` on a row in `맞다고 표시한 것` removes the acknowledgement.
- Stored in `.civilight` (FMT §6 `점검 ‘맞음’ 표시 · 항상`), so reopening a file does not re-raise accepted items. Check results themselves are never stored.
- `아님` for term candidates is stored by world (R21 §6) and not handled here.

Sources: FMT §4, §6; R12 §1; R18 §5; R20 §3; R21 §6.

#### STO-42 · 점검 설정
Priority **P1** · needsExternal **no**

App settings rows in `설정 › 지도 · 이미지 · 연표` (`SettingsMore`, screen container LIB), section `연표 · 때`:
- `때 표기 기본 | 새 프로젝트 · 프로젝트마다 바꾸기 가능 | 에피소드 기준 ›` (STO-17).
- `이동 점검 | 이동 시간 부족 · 같은 때 · 동시에 두 장소` with a switch (default on): off → registry rows 10–12 are not computed.
- `장소 없는 씬 알림 | 점검 센터 ◇` with a switch (default on): off → row 13 is hidden.

Sources: board `SettingsMore`.

### H. Layout

#### STO-43 · 펼친 화면 배치 (home, check)
Priority **P1** · needsExternal **no**

Mechanics, setting and pane sizes are CNV-33 (`설정 › 화면 › 펼친 화면 배치`: `목록 + 상세` default or `가운데 한 칸`). Home and check specifics:
- **Home, 목록 + 상세** (`TwoHome`): left pane 390 = home (STO-01), 1 px `line` divider, right pane 409 = story structure (STO-04) of the selected/current episode. Tapping an episode on the left updates the right pane.
- **Home, 가운데 한 칸** (`ColHome`): home in a 560 px reading column centred on `raise` at 50% opacity.
- **Check, 목록 + 상세** (`TwoCheck`): left = check list, right = the script at the selected item's position (manuscript with the block selected).
- **Check, 가운데 한 칸** (`ColCheck`): list in the 560 column.
- Fold/unfold keeps the selected item and scroll (foundation rule "만지던 것은 제자리").
- Bulk has its own inner layout (STO-35). Search, lens and 장소와 때 sheets on inner: same sheets, width `ED` 560.

Sources: memory concept (UNFOLDED LAYOUT); boards `TwoHome`, `ColHome`, `TwoCheck`, `ColCheck`, `BulkWide`.

---

## 3. Data

Types are Kotlin-flavoured. `Key` = ULID string (FMT §4). `OrderKey` = fractional-index string. Every persisted entity also carries `updatedAt: Instant`, `updatedByDevice: String`, `ext: JsonObject?` (FMT §4, §7). Field names in `.civilight` JSON are `ED` (camelCase of the names below).

### 3.1 Entities owned here

**`StoryStructure`** (part of the project record)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `preset` | enum `CHAPTER_EPISODE_SCENE` · `ACT_SCENE` · `CUSTOM` | no | which wizard option was picked |
| `levels` | `List<String>` size 1..4 | no | level names top → bottom, e.g. `["장","에피소드","씬"]`; last = scene level |

**`StoryUnit`** (chapter, episode, scene, or any custom level)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `key` | Key | no | permanent id |
| `projectKey` | Key | no | |
| `parentKey` | Key | yes | null for top-level units |
| `level` | Int | no | index into `levels` |
| `order` | OrderKey | no | order among siblings |
| `title` | String | yes | null → `미정` |
| `synopsis` | String | yes | chapter/episode plan text (`계획` chapters show it) |
| `doneMarked` | Boolean | no | author-set 완료 |
| `hidden` | Boolean | no | item hiding (model: LIB; stored here) |
| `deletedAt` | Instant | yes | non-null = in trash (30 days, LIB purge) |

**`SceneProps`** (1:1 with a scene-level `StoryUnit`)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `sceneKey` | Key | no | |
| `place` | `PlaceRef { kind: PLACE·REGION, key: Key }` | yes | 장소 (pin) or 지역 (region); key into world/map |
| `time` | `SceneTime` | no | see below; `None` when no 때 |
| `silentCast` | `List<Key>` (characters) | no | 말 없는 등장; speakers are derived |

The scene also carries SCR-owned fields (`memo` = 씬 메모, `transitionMode`, `transitionText`, `transitionSnapshot`; `10-script.md` §3 "Scene fields owned by SCR").

**`SceneTime`** (sealed)

| Variant | Fields | Meaning |
| --- | --- | --- |
| `None` | — | 때 없음 |
| `Relative` | `baseSceneKey: Key`, `dayMin: Int`, `dayMax: Int` (`dayMin ≤ dayMax`; equal when 정확), `precision: EXACT·APPROX`, `tod: TimeOfDay`, `rebasedFrom: Key?` | base + interval in story days; `rebasedFrom` = the deleted scene this one was rebased away from (STO-10 restore) |
| `Direct` | `storyDay: Int` **or** `calendarDate: {month: Int, day: Int, yearOffset: Int}`, `tod: TimeOfDay` | fixed point (고정점) |
| `Flashback` | `mode: ANCHOR·DIRECT`, `eventKey: Key?`, `relation: BEFORE·ON·AFTER?`, `amount: Duration?`, `year: Int?`, `calendarDate?`, `tod: TimeOfDay` | 회상; out of the chain |

`TimeOfDay = { band: Band?, clock: LocalTime? }` with `Band = 새벽·아침·낮·저녁·밤`; both null = whole day. `clock` is stored in the canonical 04:00-day form (a 01:30 scene on story day 1 stores `storyDay`/interval of day 1 and `01:30`).

**`SceneTimeSettings`** (part of the project record)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `notation` | enum `EPISODE` · `STORY_START` | no | `때 표기`, default `EPISODE` |
| `dayBoundary` | enum `DAWN_0400` · `MIDNIGHT` | no | default `DAWN_0400` |
| `bandStarts` | `Map<Band, LocalTime>` | no | default 새벽 04:00 · 아침 06:00 · 낮 10:00 · 저녁 17:00 · 밤 20:00; each band ends where the next starts |

Referenced, not owned: world-time mode, start year and `WorldCalendar` (WLD-30), `TravelMode` list and project default mode (WLD-47), transition settings (SCR-48).

**`TravelSegmentOverride`**: `key`, `characterKey`, `fromSceneKey`, `toSceneKey`, `modeKey`. Void (deleted) when either scene is deleted permanently.

**`ChangeRecord`** (envelope owned here; stored in `changes.json`)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `key` | Key | no | |
| `target` | `TargetRef` (opaque, defined by the creating area: CST `Field(entity, fieldDef)` · `DisplayName(character)` · `RelationSlot(relation, slot)`; WLD term reveal, faction area) | no | what changes |
| `value` | JSON payload (schema by the target's owner) | no | the value from the anchor on |
| `note` | String | yes | history note (e.g. `처음 만남`) |
| `anchor` | `Anchor` | no | where it starts |
| `orphaned` | Boolean | no | `근거 지워짐` (set by SCR-43) |
| `createdAt` | Instant | no | tie-break |

**`Anchor`** (sealed): `Block(blockKey)` — from that block, inclusive; `UnitStart(unitKey)` — from before the first block of that episode/scene (`1-5 #—`); `SceneEnd(sceneKey)` — after the last block of that scene (used by `여기서부터 바뀜` at 최신); `WorldEvent(eventKey)` — before the story.

**`StoryPoint`** (derived, not stored) and the order: every anchor and lens point maps to a sort key `[o_1, …, o_k, tail]`, where `o_i` are the order keys of the unit path (chapter, episode, scene, …) and `tail` is: `−∞` for `UnitStart` of that unit (shorter paths sort before longer ones at `−∞`), the block's order key for `Block`, `+∞` for `SceneEnd` / end-of-unit. `WorldEvent` sorts before everything. Lens points: `LATEST` = end of the 집필 선 scene (`+∞` on its path; no 집필 선 → `+∞` globally); `EpisodeEnd(e)` = `+∞` on e's path; `SceneEnd(s)` = `+∞` on s's path. A record applies at point P iff `sortKey(anchor) ≤ sortKey(P)`; the script panel for block B uses `< sortKey(Block(B))`.

**`Acknowledgement`**: `key`, `checkType: String` (registry id, e.g. `travel.shortage`, `time.order`, `transition.stale`, `term.beforeReveal`), `subjectKeys: List<Key>`, `fingerprint: String`, `kind: MATCH (맞음) · KEEP (이대로 두기)`, `createdAt: Instant`.

### 3.2 App-DB-only state (not in `.civilight`)

| Entity | Fields | Meaning |
| --- | --- | --- |
| `LensState` | `projectKey`, `point: LATEST · EpisodeEnd(key) · SceneEnd(key)`, `savedForScript: point?` | lens value; saved while the script is open (STO-16) |
| `ProjectActivity` | `projectKey`, `lastEditedBlockKey`, `lastEditedAt` | `이어 쓰기`, current chapter, `쓰는 중` |
| `DayBaseline` | `projectKey`, `localDate`, `blockKey`, `lengthAtDayStart` | 오늘 쓴 글자 (STO-06) |
| `UndoStep` | opaque | cascade, bulk, merge, move, mode override (one step each) |
| app settings (LIB container) | `defaultNotation`, `checkTravel: Boolean`, `checkNoPlace: Boolean` | STO-42 |

### 3.3 Derived values (never stored, FMT §4)

`StoryPoint` positions, 집필 선, `ResolvedTime { dayExpr: (c, vars), tRange }`, all labels (`N일째`, `약 N일째 (a~b)`, short/full forms), ordinals and codes (`1-7`, `씬 4`), statuses, progress, char counts, check items and counts, search index, suggestion lists, travel distances and required times.

### 3.4 Relations to other areas' entities

| Here | → Other area entity | Rule |
| --- | --- | --- |
| `SceneProps.place` | world/map `Place` / `Region` (key) | deleted target → dotted `지운 장소`, excluded from travel (R20) |
| `SceneProps.silentCast`, derived speakers | cast `Character` | speakers derived from SCR blocks |
| `SceneTime.Flashback.eventKey` | world `WorldEvent` (year) | |
| `TravelSegmentOverride.characterKey` | cast `Character` | |
| `StoryUnit` (scene) | SCR `Block.sceneKey` | blocks belong to scenes; block order keys inside the scene |
| Lens resolution | cast/world `ChangeRecord` (anchor = block key, unit key + start, or world event) | STO-12 order |
| Travel distance | map `Route`, `MapScale`, pin hierarchy | STO-29 |
| `Acknowledgement.subjectKeys` | any key (block, link, scene, character, record) | deleted subject → ack deleted |
| `StoryUnit.hidden` | LIB hiding/unlock | rendering per R19 §6 |
| `ChangeRecord.target`, `.value` | CST / WLD schemas | `Anchor.Block` keys are SCR blocks |
| `SceneTimeSettings` ↔ world time | WLD-30 calendar, WLD start year | labels only |

### 3.5 What goes into `.civilight`

| Data | File (FMT §2) | Note |
| --- | --- | --- |
| `StoryStructure`, `SceneTimeSettings` | `project.json` | "구조 · 프로젝트 설정" |
| `StoryUnit` tree, `SceneProps` (place, time, cast, done, hidden) | `story/index.json` | "장 · 에피소드 · 씬 순서와 씬의 장소 · 때" (SCR adds its scene fields to the same entries) |
| `ChangeRecord` | `changes.json` | "◆ 변화 기록 (자리 · 근거 블록 키)" |
| `Acknowledgement`, `TravelSegmentOverride` | `ED` `checks.json` (optional feature `checks.ack`) | FMT says 맞음 is always stored but names no file (§6 Q27) |
| Units in trash | not stored (FMT §6), unless export option `휴지통도 포함` | |
| Lens, activity, baselines, derived values | not stored | |

Manifest: the required feature list includes `time.base-interval` (FMT §3 example). Example `story/index.json` scene entry (`ED` shape):

```json
{
  "key": "01JD...S9", "parent": "01JD...E7", "level": 2, "order": "a3", "title": "꺼진 등불",
  "done": false, "hidden": false,
  "place": { "kind": "PLACE", "key": "01JD...P_GATE" },
  "time": { "type": "relative", "base": "01JD...S8", "dayMin": 1, "dayMax": 1,
            "precision": "exact", "band": null, "clock": "05:40" },
  "silentCast": []
}
```

---

## 4. Rules as test cases

All cases use the sample project 「잿빛 새벽의 등불」 with the fixtures below. Times are in minutes; `d` = story day. Default settings unless a case says otherwise: 연도만 · 시작 1200년, `때 표기` 에피소드 기준, bands 새벽 04–06 · 아침 06–10 · 낮 10–17 · 저녁 17–20 · 밤 20–04, day boundary 04:00, modes 도보 15 km · 8 h, 말 40 km · 8 h, 배 30 km · 24 h. Where boards disagree with each other on sample values, the rule tables win (user: ignore sample-data issues).

### 4.1 Fixtures

**Fixture T (time chain)** — manuscript order top to bottom.

| Id | Scene | Title | Place | 때 input |
| --- | --- | --- | --- | --- |
| S1 | 1-1 씬 1 | 첫 순찰 (fixture) | 하층 거리 | 직접 · 이야기 1일째 · no band |
| S2 | 1-2 씬 1 | 새벽 순찰 | 하층 거리 | 기준 S1 · +1일 · 새벽 · 정확 |
| S3 | 1-3 씬 1 | (fixture) | — | 없음 |
| S4 | 1-3 씬 2 | 결사 합류 | 결사의 문 | 기준 S2 · +1일 · 아침 · 정확 |
| S5 | 1-4 씬 2 | 세라의 회상 | 하층 거리 | 회상 · 기준 사건 대정전 (1195) · 그날 |
| S6 | 1-6 씬 1 | 잿더미 시장 | 잿더미 시장 | 기준 S4 · 대략 +5~9일 · 낮 |
| S7 | 1-7 씬 1 | 첫 번째 깜박임 | 하층 거리 | 기준 S6 · +2일 · 새벽 · 정확 |
| S8 | 1-7 씬 2 | 상층 관문 | 상층 관문 | 기준 S7 · 같은 날 · 저녁 · 정확 |
| S9 | 1-7 씬 3 | 꺼진 등불 | 결사의 문 | 기준 S8 · +1일 · 05:40 · 정확 |
| S10 | 1-7 씬 4 | 미정 | 결사의 문 | 기준 S9 · 같은 날 · 06:10 · 정확 |
| S11 | 1-8 씬 1 | 송전탑 아래 | 송전탑 폐허 | 기준 S10 · +2일 · 새벽 · 정확 |
| S12 | 1-9 씬 2 | (fixture) | 상층 관문 | 직접 · 이야기 20일째 · 낮 |

Cast: 하르트 speaks in S8 and S9. Route 상층 관문 ↔ 결사의 문 = 38 km. Anchor event `결사 합류` = S4.

**Fixture R (records, rule 11/13)** — content exists in every scene up to and including 1-7 씬 3; 1-7 씬 4, 1-8, 1-9, 2-5 are empty. 1-7 씬 3 blocks in order: #042 리엔 “저기… 불빛이 보여요.”, #043 하르트 “멈추지 마. 저건 등불이 아니야.”, #044 나레이션 “…오리지늄 등불이 한 번 깜박였다.”, #045 리엔 “하르트 씨, 방금…”.

| Record | Anchor | Value |
| --- | --- | --- |
| R1 리엔 · 소속 | unit 1-1 start | 없음 |
| R2 리엔 · 소속 | unit 1-3 start | 등불 결사 |
| R3 리엔 · 소속 | unit 2-5 start | 등불지기장 |
| R4 relation 리엔·하르트 · 리엔의 마음 | block #043 | 경계 |
| R5 relation 리엔·하르트 · 하르트의 마음 + 호칭 | block #043 | 보호 · ‘꼬마’ |
| R6 relation 리엔·하르트 · 사이 | unit 1-9 start | 동료 |
| R7 relation 리엔·하르트 · 리엔의 마음 + 호칭 | unit 1-9 start | 신뢰 · ‘하르트’ |
| R8 하르트 · 표시 이름 | unit 1-9 start | 하르트 (before: `???`) |
| R9 term 오리지늄 등불 · 첫 공개 | block #044 | — |

### 4.2 Lens and 집필 선 (R11 §1–§3, R13 §6)

- **LENS-01** Given Fixture R. When computing the 집필 선. Then it is the end of 1-7 씬 3 (after #045) and the lens chip at 최신 reads `최신 · 1-7`.
- **LENS-02** Given Fixture R, lens 최신. Then 리엔 → 하르트 shows `→ 경계 · ← 보호` (1-7부터 ◆), 하르트's display name is `???`, R3 is `예정 · 2-5 | 등불지기장`, R6/R7 are `예정 · 1-9 | 하르트 → 신뢰` (and 사이 동료), footnote `예정 · 집필 선(1-7 씬 3) 뒤 기록`.
- **LENS-03** Given Fixture R. When lens = 1-6. Then the relation shows `만나기 전` (no relation value) and the name is `???`.
- **LENS-04** When lens = 1-7. Then 리엔 → 하르트 `경계`, 하르트 → 리엔 `보호 · ‘꼬마’`, name `???`.
- **LENS-05** When lens = 1-8. Then same as LENS-04 (no record in 1-8).
- **LENS-06** When lens = 1-9 (unwritten, still selectable, row `ink3` with `아직 쓰지 않음`). Then 사이 `동료`, 리엔 → 하르트 `신뢰`, 호칭 `하르트`, display name `하르트`. The cast list label for 하르트 is `??? (1-8까지)`.
- **LENS-07** Given Fixture R, script panel (block-level state just before the block). When #042 is selected → relation `만나기 전`. When #043 is selected → `만나기 전 → 경계 · ◆ 여기서`. When #044 is selected → `경계`.
- **LENS-08** Given a term link to 오리지늄 등불 inside #042 (same scene, before #044). Then it is `▲ 공개 전 사용` (block order, not scene).
- **LENS-09** Given Fixture R plus content in a 1-9 scene while 1-8 stays empty. Then 집필 선 = end of that 1-9 scene, chip `최신 · 1-9`, R6–R8 are applied at 최신, 1-8 shows `미정` in the structure.
- **LENS-10** Given a project with no block content at all and records R1–R3. Then chip `최신 · 전체`, all records applied (소속 = 등불지기장), no 예정 rows.
- **LENS-11** Given Fixture R. When the first block is written in a 2-5 scene. Then R3 becomes applied at 최신 (chip `최신 · 2-5`) without any user action.
- **LENS-12** Given LENS-09 state. When the 1-9 scene is moved to trash. Then 집필 선 returns to the end of 1-7 씬 3 and R6–R8 are `예정` again; none is deleted.
- **LENS-13** Given lens 1-5 on 리엔. When the lens chip is rendered. Then it reads `시점 · 1-5` with an `acc` border; lens sheet row `1-5` shows `보는 중`.
- **LENS-14** Given the script is opened with lens 1-5 and the user taps `렌즈를 이 씬으로 맞추기` in 1-7 씬 3, then leaves the script. Then the lens is `시점 · 1-7 씬 3` `ED` label. Without tapping, leaving restores `시점 · 1-5`.

### 4.3 Editing under the lens (R11 §1, IDX)

- **EDIT-01** Given Fixture R, lens 최신, 리엔 소속 has history. When the user edits to `등불 결사 · 견습` and taps `이 값 고치기`. Then R2's value becomes `등불 결사 · 견습`; no new record; R3 unchanged.
- **EDIT-02** Same, but `여기서부터 바뀜 · 1-7`. Then a new record with anchor `SceneEnd(1-7 씬 3)` holds `등불 결사 · 견습`; R2 unchanged; at 최신 the profile shows `등불 결사 · 견습` (`ED`); the script panel at #045 (state before it) still shows `등불 결사`; a block appended later in 1-8 sees `등불 결사 · 견습`.
- **EDIT-03** Given lens 1-5. When editing 소속. Then the banner `1-5 시점 값 고치는 중 | 고치면 1-5부터 적용` + `최신으로` is shown; `여기서부터 바뀜` creates a record anchored at unit 1-5 start; footnote `저장 시 ◆ 변화 기록 · 1-5 #— · 이전 값 ‘등불 결사’`.
- **EDIT-04** Given a field with no history at 최신. Then editing shows no two-button choice.

### 4.4 Resolution and labels (R11 §5, §6)

| Case | Scene | Episode label (full) | Story-relative | vs anchor S4 |
| --- | --- | --- | --- | --- |
| **TIME-01** | S2 | `1-2 · 1일째 · 새벽` | `이야기 2일째` | `−1일` |
| **TIME-02** | S4 | `1-3 · 1일째 · 아침` | `이야기 3일째` | `기준` |
| **TIME-03** | S6 | `1-6 · 1일째 · 낮` | `약 10일째 (8~12)` | `+약 7일` (5~9) |
| **TIME-04** | S7 | `1-7 · 1일째 · 새벽` | `약 12일째 (10~14)` | `+약 9일` (7~11) |
| **TIME-05** | S8 | `1-7 · 1일째 · 저녁` | `약 12일째 (10~14)` | `+약 9일` |
| **TIME-06** | S9 | `1-7 · 2일째 · 새벽` | `약 13일째 (11~15)` | `+약 10일` (8~12) |
| **TIME-07** | S10 | `1-7 · 2일째 · 아침` (scene head `씬 4 · 결사의 문 · 2일째 아침 06:10`) | `약 13일째 (11~15)` | `+약 10일` |

- **TIME-08** (shared range) Given Fixture T. When computing `A(S9) − A(S8)`. Then the result is `[580, 760]` min = `9시간 40분 – 12시간 40분`, exact although both scenes are `약 N일째`. Negative test: an implementation that subtracts the independent day ranges (11~15 − 10~14) gets `−3~5일`; that must not happen.
- **TIME-09** Short forms: S9 in the story list `2일째 새벽`; script header `1-7 · 씬 3 · 2일째 새벽`; S7 story list `1일째 새벽`; a scene with no 때 `—`; bulk list `때 없음`.
- **TIME-10** (episode first scene untimed) Variant of Fixture T: S7 has no 때 and S8 = `S6 기준 · +2일 · 저녁`. Then S8 (first timed scene of 1-7) is `1-7 · 1일째 · 저녁` and S9 `1-7 · 2일째 · 새벽`. When S7 later gets `S6 기준 · +1일 · 새벽`, then S7 is `1-7 · 1일째 · 새벽`, S8 becomes `1-7 · 2일째 · 저녁`, S9 `1-7 · 3일째 · 새벽`; the stored values of S8 and S9 did not change (R11 §6: 값은 그대로, 표기만 다시 센다).
- **TIME-11** (approx inside an episode) Variant of Fixture T: S9 = `S8 기준 · 대략 +1~3일 · 05:40`. Then S7 `1-7 · 1일째 · 새벽` and S8 `1-7 · 1일째 · 저녁` stay exact; S9 is `1-7 · 약 3일째 · 새벽` (episode days 2~4, midpoint 3); S10 (`S9 기준 · 같은 날 · 06:10`) is `1-7 · 약 3일째 · 아침`; `A(S10) − A(S9)` is exactly 30 min (shared variable).
- **TIME-12** (flashback) S5 shows `회상 · 1195`, script header `1-4 · 씬 2 · 회상 · 1195`, result line `연도 1195 · 대정전 그날 | 이야기 시작 5년 전`. S5 has no `N일째`. A new scene N created right after S5 by `다음 씬 만들기` is pre-filled with 장소 `하층 거리` and 때 `S4 기준 · 같은 때` (base label `1-3 씬 2 기준`), not S5; N is not 회상; N is the first timed scene of 1-4, so its label is `1-4 · 1일째 · 아침`, story `이야기 3일째`.
- **TIME-13** (no episode level, R11 §6) Given a 1-level project (`levels = ["씬"]`) with 씬 1 = `직접 · 이야기 1일째 · 새벽` and 씬 2 = `씬 1 기준 · +1일 · 낮`. Then labels are story-relative: short `1일째 새벽`, `2일째 낮`; full form without an episode code `ED`: `2일째 · 낮`.
- (Calendar date labels are not testable until §6 Q14 is answered.)

### 4.5 Bands and day boundary (R18 §2)

- **BAND-01** Band ranges: 새벽 `[04:00, 06:00)`, 아침 `[06:00, 10:00)`, 낮 `[10:00, 17:00)`, 저녁 `[17:00, 20:00)`, 밤 `[20:00, 04:00)`. 05:40 → 새벽; 06:10 → 아침; 17:00 → 저녁; 03:59 → 밤.
- **BAND-02** Given a scene `S8 기준 · 같은 날 · 01:30`. Then it is story day 1 of 1-7: label `1일째 밤 01:30`, not `2일째`.
- **BAND-03** Same scene, project day boundary switched to `자정`. Then the label is `2일째 밤 01:30`; the stored `SceneTime` is byte-identical before and after.
- **BAND-04** Given band `새벽` + exact `07:00`. Then the time 07:00 is used for computation, the label keeps `새벽 07:00`, and `◇ 시각과 시간대가 다름` is raised.
- **BAND-05** Given exact time only (05:40, no band). Then the label band is `새벽` (derived).

### 4.6 Storage, base and cascade (R11 §4, IDX)

- **BASE-01** Given S3 has no 때. When S4's sheet is opened. Then the base label reads `1-2 씬 1 기준` (nearest earlier timed scene S2), not 1-3 씬 1.
- **BASE-02** Given BASE-01. When S3 later gets `S2 기준 · 같은 날 · 낮`. Then S4's base stays S2 and its absolute is unchanged; S4's sheet shows `기준을 직전 씬으로`. When tapped, S4 = `S3 기준 · +1일 · 아침` (interval recomputed: d(S3) = 2, d(S4) = 3), absolute unchanged.
- **BASE-03** Given a new project whose first scene is being given 때. Then only `직접` is enabled.
- **CASC-01** Given Fixture T. When S7's interval changes from +2일 to +4일. Then the preview shows `약 12일째 새벽 → 약 14일째 새벽 · +2일`, `뒤 씬 4개 +2일 · 1-9 씬 2(직접)에서 멈춤`, rows S8 `약 12일째 저녁 → 약 14일째 저녁`, S9 `약 13일째 새벽 → 약 15일째 새벽`, S10 `약 13일째 아침 → 약 15일째 아침`, S11 `약 15일째 새벽 → 약 17일째 새벽`, and `1-9 씬 2 | 직접 · 그대로 | 고정점`. With `뒤도 같이` + `바꾸기`, only S7's stored value changes; S8–S11 resolve +2 days; S12 stays day 20.
- **CASC-02** Same edit with `이 씬만`. Then S7 resolves to 약 14일째; S8 (direct dependent) is rewritten to `S7 기준 · −2일 · 저녁` so it stays 약 12일째; S9–S11 unchanged; exactly one `◇ 순서와 때가 다름` (S8 is now definitely earlier than S7); the preview line read `이 씬만 · 뒤 씬 때 그대로 · ◇ 순서와 때가 다름 1`.
- **CASC-03** Given Fixture T. When S7 changes by +8 days with `뒤도 같이`. Then S11 resolves to `약 23일째 (21~25)`, S12 stays `20일째 낮`, and S12 gets `◇ 순서와 때가 다름` (max of A(S12) − A(S11) < 0).
- **CASC-04** A cascade is one undo step: undo restores S7's stored value and nothing else needs restoring.
- **CASC-05** Given S11 has no dependents. When S11's 때 changes. Then no preview appears; the value is stored directly.
- **INS-01** Given Fixture T. When `다음 씬 만들기` creates N after S8 and the pre-filled 때 is kept. Then N = `S8 기준 · 같은 때` (same day, 저녁) and S9 keeps base S8 (absolute unchanged). With `비우기`, N has `때 없음` and S9 is unchanged.
- **DEL-01** Given Fixture T. When S8 is moved to trash. Then S9 becomes `S7 기준 · +1일 · 05:40` with `rebasedFrom = S8`; A(S9) is unchanged (`1-7 · 2일째 · 새벽`).
- **DEL-02** Given DEL-01. When S8 is restored. Then S8 resolves to `1-7 · 1일째 · 저녁` again and S9 returns to `S8 기준 · +1일 · 05:40` (`ED`), absolute unchanged.
- **MOVE-01** Given Fixture T. When S9 is moved after S11 (into 1-8). Then S9 keeps its stored and absolute time (약 13일째); S10 keeps base S9; the move preview `때` row reads `그대로 · ◇ 1 순서와 어긋남` (S9 is now before S11 in time but after it in the manuscript: A(S9) − A(S11) max < 0 because both share S6's variable).

### 4.7 Order/time mismatch (R11 §5)

- **ORD-01** Given previous scene P = `같은 날 · 20:00` and X = `같은 날 · 11:00` after it. Then X's sheet shows `직전 씬보다 앞 · 9시간 전` and `◇ 순서와 때가 다름` is raised; never ▲.
- **ORD-02** Given P = `2일째 아침` and X = `2일째 아침` (bands only). Then no notice (range straddles 0).
- **ORD-03** Given ORD-01 and `맞음`. Then the item moves to `맞다고 표시한 것`. When X's time changes to 10:00, the acknowledgement is void and the notice returns.

### 4.8 Travel (R18)

- **TRV-01** Required times for 38 km: 도보 → 3136 min (52 h 16 m), label `2일 4시간`; 말 → 456 min (7 h 36 m), label `약 7시간 40분`; 배 → 1824 min (30 h 24 m), label `1일 6시간`.
- **TRV-02** 30 km 도보 → 2 full days, rest 0 → 2880 min, label `2일`.
- **TRV-03** Gap S8 → S9: S8 = d1 저녁 `[17:00, 20:00)`, S9 = d2 05:40. gapMin = 580 min (`9시간 40분`), gapMax = 760 min (`12시간 40분`), shown `9시간 40분 – 12시간 40분`.
- **TRV-04** Verdict for 하르트 S8 → S9, mode 도보: 3136 > 760 → `▲ 이동 시간 부족 | 하르트`; card rows `필요 | 도보 2일 4시간`, `있음 | 9시간 40분 – 12시간 40분`; detail `경로 38 km · 도보 15 km · 하루 8시간 · 1일째 저녁(17–20) → 2일째 05:40`.
- **TRV-05** Time chip: deficit = 3136 − 580 = 2556 min (42 h 36 m) → k = ceil(2556 / 1440) = 2 → chip `씬 3 · +2일로`. Tapping opens the cascade preview with S9 = `S8 기준 · +3일 · 05:40` (was +1일). After `바꾸기`, gapMin = 580 + 2880 = 3460 min (57 h 40 m) ≥ 3136 and the ▲ is gone. Negative: setting the interval to +2일 (one extra day) gives gapMin 2020 min (33 h 40 m) and the ▲ stays.
- **TRV-06** Mode chips for TRV-04: 말 456 ≤ 580 → chip `말 · 약 7시간 40분`; 배 1824 > 580 → no chip. Tapping `말` stores an override (하르트, S8, S9, 말); the ▲ is gone; other characters' pairs still use 도보.
- **TRV-07** `맞음` on TRV-04 stores an acknowledgement; the ▲ leaves the open list. When S9's place, S8's band, the route length or the pair's mode changes, it returns.
- **TRV-08** Fits: a 4 km route between S8 and S9 places, 도보: full 0, rest 4 → 4/15 × 8 h = 128 min ≤ 760 → no item.
- **TRV-09** Same band, same day (separate fixture, board row `1-7 씬 2 기록실 · 씬 4 잿더미 시장 · 둘 다 2일째 아침`): 세라 in scene X (기록실, `2일째 아침`) then scene Y (잿더미 시장, `2일째 아침`), distance 12 km 도보 → 12/15 × 8 h = 384 min. gap = `[06:00,10:00) − [06:00,10:00)` = −240…240 → gapMin clamps to 0, gapMax = 240 → 384 > 240 → `▲ 같은 때 두 장소 · 세라`.
- **TRV-10** Same exact time 06:10 on the same day, different places → gap 0 → `▲ 동시에 두 장소`.
- **TRV-11** No route, straight line 20 km, 도보, gapMax 300 min: required = 1 day + 5 km → 1440 + 160 = 1600 > 300 → `▲ 이동 시간 부족` with `직선으로도 부족 · 경로 없음`. With straight line 2 km (64 min ≤ 300) → no item.
- **TRV-12** Places on a map without scale (or a node map) → `◇ 축척 없음 · 점검 못 함` with `축척 맞추기`; no ▲.
- **TRV-13** Same place in both scenes → no check.
- **TRV-14** S5 (회상) is never part of a pair; 1-8 씬 1 without a place is skipped and counted in `◇ 장소 없음 · 씬 n`.
- **TRV-15** A pair whose earlier scene's place is deleted → skipped (no ▲), the scene shows `지운 장소 · …`.
- **TRV-16** (mode precedence) Given the project defines a character field `기본 이동 수단` and 하르트's value is `말`. Then TRV-04's pair uses 말 (456 ≤ 760 → no ▲). With a per-segment override (하르트, S8, S9, 도보) the ▲ of TRV-04 returns. Without the field and override, 도보 is used.
- Distance cases (region nearest points, child-map ancestor pins, no scale) are WLD-66's tests; this area tests only how their results are used (TRV-11, TRV-12, TRV-15).

### 4.9 Split / merge (R22 §5)

- **SPL-01** Given Fixture T with 1-7 씬 3 (S9) holding 13 blocks. When split below #044. Then new scene B: place 결사의 문, 때 `S9 기준 · 같은 때` (d2 05:40, 정확), 전환 문구 없음, title 미정; old 씬 4 (S10) becomes `씬 5` and keeps base S9 and `2일째 아침 06:10`; records stay on their blocks (`첫 공개 오리지늄 등불 · 씬 3 #044 그대로`).
- **SPL-02** Split of an untimed scene → B untimed. Split of S5 (회상) → B is 회상 (toggle shown on). Split of a hidden scene → B hidden.
- **MRG-01** Given S9 and S10 with direct transition text on S10 `결사의 문 앞 · 06:10`. When merged with defaults. Then place 결사의 문 (same, no question), 때 = S9's (`2일째 새벽 05:40`), S9's memo gains `합치기 전 · 씬 4 · 결사의 문 · 2일째 아침 06:10`, the transition text becomes a system-text block (`시스템 텍스트로 남기기`), S11 (base S10) becomes `S9 기준 · +2일 · 새벽` with absolute unchanged (d4 of 1-7 = `약 15일째`).
- **MRG-02** Same merge choosing `씬 4 것`. Then S9's stored 때 becomes d2 06:10 (`S8 기준 · +1일 · 06:10`); S9's other dependents keep their absolute time.
- **MRG-03** Merge with one side 회상 → blocked with `먼저 회상 끄기`; one side hidden → `먼저 숨김 풀기`; scenes in different episodes → not offered.
- **MRG-04** Only one side has a place → that place is used without asking.

### 4.10 Transition text (R22 §2)

- **TRN-01** (SCR-48 authoritative) Template `장소 · 시각`: S10 auto → `결사의 문 · 06:10`; place + band only (`아침`) → `결사의 문 · 아침`; S6 (대략, band 낮) → `잿더미 시장` (place only); region 하층 구역 + 새벽 → `하층 구역 · 새벽`; scene without place → no text and `자동` disabled.
- **TRN-02** Template `장소만`: S10 → `결사의 문`.
- **TRN-03** Given S10 직접 `결사의 문 앞 · 06:10` written while S10 was 06:10. When S10's time becomes 07:00. Then the text is unchanged and `◇ 전환 문구 · 쓴 뒤 때 바뀜 · 1-7 씬 4 · 06:10 → 07:00` appears with `자동으로 · 문구 고치기 · 맞음`. `자동으로` → mode AUTO, text `결사의 문 · 07:00`, item closes.
- **TRN-04** Given 결사의 문's map `보이는 시점` = 1-8 and S10 in 1-7 with `자동`. Then the auto text is still produced and `◇ 공개 전 장소 · 전환 문구` appears with `직접 쓰기 · 맞음`.
- **TRN-05** Switching S10 from 직접 to 없음 and back to 직접 restores `결사의 문 앞 · 06:10`.

### 4.11 Bulk (MAIN, boards)

- **BLK-01** Given the bulk scope 1장 with 31 scenes, 22 with place and 19 with 때. Then the progress reads `장소 | 22/31`, `때 | 19/31`; chips `전체 31 · 장소 · 때 빈 것 12 · 제안 있음 3` (fixture counts).
- **BLK-02** Given selected 1-6 씬 2, 1-8 씬 1, 1-8 씬 2, 1-9 씬 1 (all untimed), 1-6 씬 1 = `1-6 · 1일째 · 낮`, 1-7 씬 3 timed, 1-7 씬 4 untimed. When `때 정하기` with `직전 씬 기준 차례로`, `+1일`, `그대로`, `대략`. Then preview rows: `1-6 씬 2 | 1-6 · 2일째 · 대략 | 직전 1-6 씬 1 · 1일째 낮`; `1-8 씬 1 | 1-8 · 1일째 · 대략 | 1-7 씬 3 기준 · 직전 씬 때 없음`; `1-8 씬 2 | 1-8 · 2일째 · 대략 | 직전 1-8 씬 1`; `1-9 씬 1 | 1-9 · 1일째 · 대략 | 직전 1-8 씬 2`; button `씬 4개에 넣기`. Preview labels show the nominal chip value + `· 대략` (board copy). Nothing is stored before the button; after it, one undo restores all four.
- **BLK-03** `장소 정하기` · `모두 같은 장소` · `잿더미 시장` for the same 4 → footer `씬 4개 → 잿더미 시장`; applying sets all four places; one undo.
- **SUG-01** Given 1-6 씬 2 contains `▸ 배경 잿더미 시장(밤)` whose asset links to place 잿더미 시장. Then the card source is `배경 연출`, proposal `잿더미 시장 | 같은 날 · 밤`.
- **SUG-02** Given 1-8 씬 1 narration `“이틀 뒤, 송전탑 그림자가 길게 누운 새벽.”` and place 송전탑 폐허 with alias 송전탑. Then source `본문`, boxed `이틀 뒤`, `송전탑`, `새벽`, proposal `송전탑 폐허 | +2일 · 새벽`.
- **SUG-03** Given 1-8 씬 2 with no place/때 and 직접 transition text `기록실 · 같은 날 밤`, no place card 기록실. Then source `전환 문구`, proposal `기록실 | 같은 날 · 밤 | · 장소 카드 없음 → 새 장소`.
- **SUG-04** Given 1-9 씬 1 with none of the above. Then `제안 없음` and `배경 · 전환 문구 · 본문에서 찾지 못함 → 직접`; the sheet footer reads `제안 3개 모두 넣기`.
- **SUG-05** No scene changes until `넣기` / `모두 넣기`; a scene's auto transition text is never a source.

### 4.12 Search (MAIN, R19, R21, feedback)

- **SRCH-01** Query `오리지` with Fixture R + 1-4 씬 2 세라 “[오리지늄]은 타지 않아. 빛만 남지.” and 1-2 씬 1 리엔 “등불 속 그 돌, 오리지… 뭐라고 했죠?”. Then section `용어` has `오리지늄 등불` (hit `오리지` boxed) with sub `첫 공개 1-7 #044 · 장소: 하층 구역`; section `대사 · 3` lists 1-7 씬 3 · 나레이션, 1-4 씬 2 · 세라 (flag `▲ 공개 전 사용`), 1-2 씬 1 · 리엔, each as one sentence with `오리지` boxed inline.
- **SRCH-02** Query `ㅇㄹㅈ` (consonants only) → matches term 오리지늄 등불 (initials `ㅇㄹㅈㄴ ㄷㅂ`).
- **SRCH-03** Query typed in NFD form of `오리지` → same results as SRCH-01.
- **SRCH-04** Query `오리지누` → `결과 없음`, `비슷한 이름` contains `오리지늄 등불 · 용어 · 첫 공개 1-7`, action `‘오리지누’ 용어로 만들기`.
- **SRCH-05** Given character 노아 hidden and locked and a hidden locked scene 1-6 씬 5. Then no 노아 card result, no text from 1-6 씬 5, last line `숨긴 항목 제외`; 노아's dialogue in a visible scene is still found by its text.
- **SRCH-06** Given 노아 in trash. Then no 노아 result.
- **SRCH-07** Command rows: query `점검` → `› 점검 센터 열기 | 확인할 항목 3`; query `시점` → `› 시점 바꾸기 | 지금: 최신 · 1-7` (outside the script).

### 4.13 Check centre (MAIN, R12, R18–R22)

- **CHK-01** Given open items: ▲ 공개 전 사용 (1-4 씬 2), ◇ 공개 전 후보 (1-2 씬 1), ▲ 화자 없음 (1-7 씬 2), ◇ 지운 인물 · 노아, ▲ 보이스 비어 있음. Then `전체 5` (◇ counted).
- **CHK-02** Given no open items and episodes 1-1 … 1-7 with content. Then `어긋난 곳 없음`, `마지막 확인 · 방금 · 1-1 ~ 1-7`, buttons `원고로` · `맞다고 표시한 것 2` (2 acknowledgements stored).
- **CHK-03** Given a locked hidden scene with 2 check items. Then one row `▲ 숨긴 항목 관련 2 · 지문으로 보기`.
- **CHK-04** `이대로 두기` on `◇ 근거 지워진 변화` → acknowledgement kind KEEP; the item appears under `맞다고 표시한 것`; `되돌리기` there reopens it.
- **CHK-05** (`ED`) Given an acknowledgement on a block-subject item. When the block is deleted, the acknowledgement goes with it; undo of that delete restores it; restoring the block later from trash evaluates the item fresh (open).
- **CHK-06** With `이동 점검` switched off in settings, TRV-04's ▲ is not computed; with `장소 없는 씬 알림` off, `◇ 장소 없음` is hidden.
- **CHK-07** Acknowledgements survive export → import of `.civilight` (stored), while check results are recomputed (not stored).

### 4.14 Structure and labels

- **STR-01** Default preset: header `Ⅰ 1장 · 등불지기 | 7/10` when 1장 has 10 episodes and 1-1 … 1-7 have content; 2장 with 8 episodes and none written → `Ⅱ 2장 · 잿불 | 0/8 · 계획` + synopsis.
- **STR-02** Episode 1-7 expanded with scenes of 1,820자, 2,104자, current scene 3, empty scene 4 → rows `1일째 새벽 | 씬 1 · 새벽 순찰 | 1,820자`, `1일째 저녁 | 씬 2 · … | 2,104자`, `2일째 새벽 | 씬 3 · 꺼진 등불 | 쓰는 중`, `2일째 아침 | 씬 4 · 미정 | —` (scene 4 has 때 but no content).
- **STR-03** Renaming level `에피소드` to `화` changes the header to `단계 이름: 장 › 화 › 씬`; keys and codes (`1-7`) are unchanged.
- **STR-04** A scene in trash is not counted: 1-7 with scenes 1, 2, (3 in trash), 4 shows `씬 3개` and the old 씬 4 is labelled `씬 3`.
- **STR-05** Home row for 1-7 with the last edit in scene 3 → `1-7 | 잿빛 새벽의 등불 | 쓰는 중 · 씬 3`; 1-8 without content and title → `1-8 | — | 미정`.

---

## 5. Screens

Components are design-system names (`GlassBars`, `CommandBar`, `LensChip`, `Sheet`, `Tabs`, `Tag`, `Buttons`, `Controls`, `ListRow`, `LockRow`, `Toast`, `SearchHit`, `WhenPicker`, `TravelBar`, `ViewToggle`). On the cover screen every panel/popover is a bottom sheet (top radius `radius-lg`, 36×4 handle, title + `닫기`, background `shade` with the screen behind at 96.5 % and `blur-back`); on the inner screen the same content is a side panel or a centred sheet. Text never sits inside cards; lists are typeset on `bg` with `line` separators.

| # | Screen / sheet | Boards | Cover (390) | Inner (800) | Components | Features |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | 프로젝트 홈 | `Home`, `NewReady` (checklist block, LIB) | single column under glass head; stats row; `이어 쓰기`; chapter list; `작업` shortcuts; command bar | `TwoHome` (home 390 \| story 409) or `ColHome` (560 column) | GlassBars, Tag (rating), LensChip, ListRow, Buttons, CommandBar | STO-01, 43 |
| 2 | 스토리 구조 | `Story`, `RSSceneMove` (background) | head `스토리`, level line, chapter headers, episode rows with grip, indented scene rows with time column; `새 씬` primary above command bar; no lens chip | right pane of `TwoHome` | GlassBars, ListRow, Buttons (`새 씬`), CommandBar | STO-04, 05, 06, 11 |
| 3 | 구조 메뉴 (⋯) | `RSHeadMenu` (scene head ⋯, drawn by SCR) | bottom sheet list of `title` + `value` rows | same, anchored to the row `ED` | Sheet, ListRow | STO-07 |
| 4 | 씬 옮기기 미리 보기 | `RSSceneMove` | bottom sheet over the structure (`미리 보기 · 옮기기 전에는 그대로`) | centred sheet `ED` | Sheet, ListRow, Buttons | STO-08 (content: SCR-54) |
| 5 | 씬 나누기 · 씬 합치기 | `RSSplit`, `RSMerge` | bottom sheets over the script (SCR) | side panel / sheet (SCR) | Sheet, Controls (radio/segmented), Buttons | STO-09 values |
| 6 | 다음 씬 만들기 | not drawn (fields from R22 §4) | bottom sheet (SCR) | sheet (SCR) | Sheet, WhenPicker | STO-05 |
| 7 | 시점 렌즈 시트 | `Lens`, `MLens` (motion) | bottom sheet starting y≈300 over a lens screen; rows, chips, two buttons | centred sheet `ED` | Sheet, LensChip, Tag, Buttons | STO-13 |
| 8 | 과거 시점 띠 · 두 버튼 · 예정 줄 | `LensEdit`, `RTEditTwo`, `RTPlanned` | banner under the head; field sheet with two stacked buttons (`btn2`: primary `pri`, ghost `tint` + `edge`, each with a small second line); 예정 rows `ink3` under values | same inside the profile pane | Buttons, ListRow | STO-14, 15 (rendered by cast/world) |
| 9 | 씬 · 장소와 때 | `SceneWhere`, `RTFlashback`, `TourWhen` (tour overlay, LIB) | bottom sheet over the script; place row, 회상 toggle, base line, day chips, band chips + time field, precision segmented, result line, 등장, 전환 문구, travel block | sheet over the side panel `ED` | Sheet, WhenPicker, Tag (chips), Controls (switch, segmented), TravelBar | STO-20, 21, 24, 26, 27 |
| 10 | 때 바꾸기 미리 보기 | `RTCascade` | bottom sheet: before→after, summary, ≤4 rows + `외 n개`, fixed-point row, two radios, `취소`/`바꾸기` | centred sheet `ED` | Sheet, Controls (radio), Buttons | STO-23 |
| 11 | 점검 센터 | `Check`, `TravelCheck`, `EmptyCheck` | head `점검 센터`, sub line, filter chips (wrap), item rows with action text buttons; travel card with `TravelBar`; empty state | `TwoCheck` (list \| script at position) or `ColCheck` | GlassBars, Tag (filters), TravelBar, LockRow (hidden aggregate), Buttons, CommandBar | STO-30, 39–41 |
| 12 | 검색 · 명령 | `Search`, `EmptySearch`, `MSearch` (motion) | full-height glass over the blurred screen; query field + `닫기`; chips; sections; hit boxes | same sheet at 560 width `ED` | SearchHit, Tag, ListRow | STO-36–38 |
| 13 | 장소 · 때 한꺼번에 | `BulkList`, `BulkPlace`, `BulkWhen`, `BulkSuggest` | list with progress bars, filters, checkboxes, footer actions; three bottom sheets | `BulkWide` table + 장소별 씬 수 map | Controls (checkbox, segmented), Tag, Sheet, SearchHit (evidence boxes), Buttons | STO-31–35 |
| 14 | 프로젝트 설정 rows (container LIB-69) | `ProjSettings` | rows owned here: `단계 이름`, `때 표기`, `하루 · 시간대`; referenced: `세계 시간`, `이동 수단` (WLD), `전환 문구` (SCR-48); each `바꾸기 ›` / value › | same in the right pane (LIB) | ListRow | STO-02, 17–19, 27, 28 |
| 15 | 설정 · 지도 · 이미지 · 연표 rows (container LIB) | `SettingsMore` | section `연표 · 때`: `때 표기 기본`, `이동 점검` switch, `장소 없는 씬 알림` switch | LIB | ListRow, Controls (switch) | STO-17, 42 |
| 16 | 새 프로젝트 · 구조 / 시간 (UI LIB) | `NewStructure`, `NewTime`, `NewWide` | LIB-10 / LIB-11 | `NewWide` | — | data: STO-02, 17 |
| 17 | Script-side entry points (UI SCR) | `Blocks` (`새 씬` row), `RSHead`, `CSheetTime`, `Wide`, `Main` | scene head line + transition line; `시간` tab `이 씬 · 장소와 때` + `전환 문구 없음 ·` + `바꾸기 ›`, `1-7 · 2일째 · 새벽 · 결사의 문`, `직전 씬 +1일 · 정확 · 이야기 약 13일째`, `▲ 하르트 이동 시간 · 점검 센터`; header `이 씬 · 1-7` | side panel `시간` tab `이 씬 · 2일째 새벽 · 결사의 문` | — | labels from STO-22, actions to STO-16/20/39 |
| 18 | Toasts | `UndoToast` (pattern) | bottom toast `ink` fill, one action `되돌리기` | same | Toast | STO-08, 10, 30, 32–34 |

Referenced consumer screens (owned by world · 연표 · 지도): `Timeline`, `TimelineAnchor` (labels `−1일`, `기준`, `+약 7일`, sub `1-6 씬 1 · ‘며칠 뒤’ 대략 · 5~9일`, note `대략인 때는 범위(5~9일)로 · 점검도 범위로`), `TimelineMap`, `MapTrack` (`장소 · 때는 씬 속성에서 · 선은 지도 경로를 따라 · 경로가 없으면 직선 ‘추정’`), `PlaceTime` (`시점 1-7 · 이 아래는 아직`). They call STO-22 for labels and ordering and STO-13 for the lens.

Board copy this spec deliberately does not follow (newer confirmed source wins): `Lens` sub line is kept but see §6 Q10; `TravelCheck` `있음 | 9시간 40분 – 12:40` → `9시간 40분 – 12시간 40분` (R18 §5); `Tour4` `시점 렌즈` step on the script (R11 §3 says to drop or reword it; tours are LIB); DS `LensChip` README `수정 중` → `고치는 중`; DS `WhenPicker` README "저장은 이야기 시작부터의 시간 하나" → base + interval (R11 §4); MAIN `받기 · 고치기 · 넘기기` → `넣기 · 고치기 · 건너뛰기`; MAIN `+3일로 · 말 1일` → STO-29 chips.

---

## 6. Open questions

Each item names the sources on each side and the `ED` this spec uses until decided.

| Q | Question | Sources | `ED` used |
| --- | --- | --- | --- |
| Q1 | Can the **number** of structure levels change after units exist? | MAIN / `NewStructure` say only "단계 이름은 나중에 바꾸기 가능"; nothing on depth | rename only; depth locked once a non-top unit exists |
| Q2 | Codes and labels for custom hierarchies (`막 › 장면`, 4 levels, 1 level) | MAIN 표기 규칙 covers 장/에피소드/씬 only | `<n><top name>`, `c-e`, `<scene name> <n>`, `a-b-c` for 4 levels |
| Q3 | How does a scene/episode become `완료`? No control is drawn | MAIN "상태(완료 · 쓰는 중 · 미정)"; `Home`, `Story` | author flag `doneMarked` (control copy not drawn); 쓰는 중/미정 derived |
| Q4 | Definitions of `오늘 쓴 글자` and `N자` (which blocks, spaces, deletions) | `Home` `1,240 · 오늘 쓴 글자`, `Story` `1,820자` | game-output text incl. spaces; daily positive growth per block |
| Q5 | `여기서 나누기` in the story-list scene ⋯ needs a block | R22 §7 row 스토리 구조 vs R22 §4 (split happens below the current block) | opens the scene in the script; split via `/씬` |
| Q6 | Episode / chapter reorder: preview? effects on fixed voice IDs' episode part | R12 §3 covers blocks and scenes only | no preview; codes renumber; SCR's `◇ 에피소드 부분 다름` |
| Q7 | After restoring a deleted scene, do scenes that were rebased away return to it? | R11 §4 row "되살리면 원래 때로 돌아와 같은 계산" (ambiguous) | yes, via `rebasedFrom`, absolute unchanged, unless edited meanwhile |
| Q8 | Deleting an episode or chapter | Trash filters (R12 §7, R20 §5) list 씬 · 블록, no episode | not offered in v1 |
| Q9 | Hidden scenes: do they count for the 집필 선 and the `최신 · 1-7` chip? How is a locked hidden scene named when it is another scene's base? | R19 §7 hands both to R11; R11 silent | they count; locked base label: copy not drawn |
| Q10 | Lens sheet sub line `앱 전체 적용 · …` contradicts "the script has no lens" | `Lens` board vs R11 §3 (confirmed) | board copy kept; needs a new line from the user |
| Q11 | Which episodes get rows in the lens sheet, and where do sub hints like `하르트 만나기 전` come from? | `Lens` board only | rows: 최신, episodes with a ◆ for the viewed record, viewed point, first unwritten; sub = episode title |
| Q12 | `여기서부터 바뀜` at 최신: is the new ◆ (anchored "right after the 집필 선") applied at 최신 or `예정` until the next block? | R11 §1 "집필 선 위치(1-7 씬 3)에" and IDX "집필 선 바로 뒤" vs R11 §1 edge row "아직 안 쓴 시점에 변화를 더함 → 예정" | anchor `SceneEnd(집필 선 scene)` (as CST proposes), applied at 최신, unseen by that scene's blocks (EDIT-02) |
| Q13 | Effect of `시간 없이 · 연표 끔` on scene 때 | `NewTime`; MAIN 연표 | scene 때 and `N일째` stay; only years/calendar/ages off |
| Q14 | World-calendar date label format for scene 때 (single date and 대략 range) | MAIN 연표; R11 §6; WLD-30 (start date proposed there) | `<month name> <d>일`, range `<month> <a>일 ~ <b>일` |
| Q15 | Do edits of the `새벽` boundary move the day boundary? | R18 §2 "새벽 4시"; settings row `하루는 새벽 4시부터` | no; boundary is 04:00 or 자정 only |
| Q16 | How are relative intervals above `+2일` (and negative ones) entered? | `SceneWhere` chips `같은 날 · +1일 · +2일 · 직접` only; R11 examples need `+5~9일` | data allows any integer; input UI copy not drawn |
| Q17 | What does `대략` mean when picked with a day chip, and how is it labelled? `BulkWhen` shows `1-6 · 2일째 · 대략`, `BulkList` shows `저녁 · 대략`, R11 uses `약 N일째`. How is a `며칠 뒤` range entered? | R11 §5–§6; MAIN 장소와 때; `BulkWhen`, `BulkList` | chip n → `[n−1, n+1]` (`같은 날` → `[0,1]`); general labels `약 N일째`; bulk preview shows nominal + `· 대략` |
| Q18 | Time order when ranges overlap (동선, `같은 때 두 장소` pairs) | R11 §5 "때 순서로" | sort by earliest start, ties/overlaps by manuscript order |
| Q19 | `이 씬만` when the next interval would become negative | R11 §4 table "간격이 0보다 작아지면 버튼을 내지 않음" vs IDX + `RTCascade` (`이 씬만 · ◇ 순서와 때가 다름 1`) | IDX (newer) followed: always offered, result is a ◇ |
| Q20 | Flashback `전 · 후`: how much before/after? | `RTFlashback` segmented `전` · `그날` · `후` only | amount field, copy not drawn |
| Q21 | One check or several: `◇ 때 확인` (R11 §4), `순서와 어긋남` (R12 §3, `RSSceneMove`), `순서와 때가 다름` (R11 §5, `RTCascade`, IDX) | R11, R12, IDX | one check type `순서와 때가 다름`; preview fragments as drawn |
| Q22 | `대략인데도 모자람` is a fourth label on `TravelCheck` but R18 has three | `TravelCheck` vs R18 §5 | treated as `이동 시간 부족` with that detail prefix |
| Q23 | Bulk scope (`잿빛 새벽의 등불 · 1장`): whole project or one chapter? Switching chapters? | `BulkList` head; MAIN | current chapter; switcher not drawn |
| Q24 | Bulk `때 정하기` on scenes that already have 때 and dependents: cascade preview? | MAIN; R11 §4 | no per-scene preview; dependents shift |
| Q25 | Filter for time/transition checks (`순서와 때가 다름`, `시각과 시간대가 다름`, `전환 문구 …`, `공개 전 장소`) | R22 §7 "필터 이름은 미정(‘이동’ 또는 새 ‘씬’)" | `이동` |
| Q26 | Should these ◇ reach 점검 센터: `같은 이름 2 · 노아` (R20 §5), deleted relation kind (R13 §3), unconvertible field value (인물 항목)? | each rule says only "◇" | not routed; shown in place |
| Q27 | Which `.civilight` file holds acknowledgements and travel-mode overrides? | FMT §6 "점검 ‘맞음’ 표시 · 항상" but §2 layout has no file for it | `checks.json`, optional feature `checks.ack` |
| Q28 | Auto transition text for a 대략 scene that has a band | R22 §2 table row shows only "대략(시각 없음) → 잿더미 시장"; SCR-48 decided "with or without band → place only" | follow SCR-48 (place only) |
| Q29 | `직전 씬보다 앞 · 약 9시간 전` cannot be derived from band ranges (저녁 vs 아침 gives 7–14 h) | R11 §5 example | midpoint of the difference, `약` when ranged; exact times give exact hours (ORD-01) |
| Q30 | `약 N일째` with an odd-width range (e.g. 5~6) | R11 §6 examples all even-width | midpoint, .5 rounds up |
| Q31 | Search: 장소 · 세력 · 사건 have no chip (WLD expects them searchable); pages? 초성 on body text? Result order? Similar-name rule? | `Search` chips `전체 · 인물 · 용어 · 대사 · 씬 · 명령`; MAIN 검색; WLD hand-off table | 장소 · 세력 · 사건 under `전체` only, pages not searched; 초성 on names/titles/text; latest position first; syllable edit distance ≤ 1 or 2-syllable prefix |
| Q32 | Duration format rules beyond the three R18 examples (rounding, `약`, multi-day gaps) | R18 §3 (`2일 4시간`, `약 7시간 40분`, `1일 6시간`) | STO-29 format rules |
| Q33 | Inserting a scene: R11 §4 "새 씬은 때 없음으로 시작" vs R22 §4 "앞 씬 값으로 미리 채워 보이고 비우기" | R11 (2026-11-21) vs R22 (2026-11-22, newer) | R22 followed: pre-filled with `비우기`; `비우기` gives R11's result |
| Q34 | Undo copy for a travel mode chip, structure ⋯ items for new episode/chapter, level-count lock state, 씬 단위로 고르기 list, mark-done control, 등장 removal | not drawn | `TODO_COPY_STO_<id>` |
| Q35 | Two specs describe the 점검 센터 shell (CNV-17 and STO-39). Differences: CNV-17 keeps MAIN's `때 고치기 · 장소 고치기 · 맞음` for `같은 때 두 장소` / `동시에 두 장소` (this spec: R18 §5 chips); CNV-17 leaves the filter of `순서와 때가 다름` and the un-mark action open (this spec: `이동` `ED`, `되돌리기` per R21 §6); CNV-17 gives `점검 못 함` no button (this spec: `축척 맞추기`, R18 §6) | CNV-17; R18 §5–§6; R21 §6 | implement one shell; use this spec's row semantics |

