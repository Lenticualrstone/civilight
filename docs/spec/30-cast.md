# 30 · 인물 (Cast) — feature prefix `CST`

Implementation spec for the cast area of Civilight: the character list and its views, the character record (profile), per-project custom fields (`인물 항목`), relationships (rule 13), item hiding for characters (rule 19 §6), and deleting / restoring / merging items (rule 20). Engineers implement from this file alone; every Korean string in `code` is UI copy and must be used verbatim.

**Sources read (2026-11-22).** Planning doc main tab (live, rev 71; local snapshot `doc.txt` rev 41 is older) · rule tabs 11 (rev 8), 12 (rev 8), 13 (rev 6), 19 (rev 2), 20 (rev 2), `인물 항목` (rev 6), `저장 파일 양식` (rev 8) · local confirmed drafts `rules/21.md`–`23.md` · memory files (concept, vocabulary, character-fields, rules-time/blocks/relations/travel/19-23, feedback-*) · board generators `design/night-manuscript/gen/*.py` (newest copy: `scr_cast.py`, `scr_new_cast.py`, `scr_new_time.py`, `scr_world.py`, `scr_cards.py`, `scr_core.py`, `scr_decide.py`, `scr_states.py`, `scr_lib.py`, `scr_tools.py`, `gen_nm.py`) · `boards_text.txt` (older dump; used only where no generator has newer text) · design system `README.md` + `components/*/README.md`.

**Precedence.** Confirmed rule tabs and memory > planning main tab > board generator source > `boards_text.txt` > design system README. When a newer source settles an older one, this spec follows the newer one and says so in a *Source note*; only unresolved conflicts go to section 6.

**Conventions.**
- `[PROPOSED]` marks behaviour or copy that no source states and this spec had to choose to make the feature implementable. Everything not marked is from a source.
- "cover" = cover screen (390×844 boards, Compact width). "inner" = unfolded inner screen (800×800 boards, Medium/Expanded width).
- Board names drop the `NmDark`/`NmLight` prefix and `.dc.html` (e.g. `Cast`, `Relations`, `RCMerge`). Every board exists in dark and light.
- Separators in copy are ` · ` (space, middle dot, space). No em dash in UI copy.
- Markers: `◆` change record (color `acc`), `◇` soft notice / first reveal (color `ink3`), `▲` warning (color `warn`). **Dotted = attached to something deleted**: names get a dotted underline, `◆`/`◇` marks get a dotted outline (design system README).
- Notation: episode `1-7`, scene `1-7 씬 3`, block `#043`, chapter `2장`.
- Sample project: 「잿빛 새벽의 등불」 (R-15). Characters 리엔 (인물 기록 007), 하르트 (shown as `???` until 1-9), 세라, 오웬, 마르타, 노아 (인물 기록 012; in the trash in the rule-20 examples), 시장 꼬마 (rule 20 merge example; drawn as `노아(시장)`, 인물 기록 014). Tags 주연 · 등불 결사 · 하층 구역 · 상층. 집필 선 (writing line) = end of 1-7 씬 3. Lens chip reads `최신 · 1-7`.

---

## 1. Scope

### 1.1 What this area owns

1. **Character list** (`인물`) on cover and inner layouts, its tag filter and search, field filters and sorting, the `목록 | 카드` card view, and the `목록 | 관계도` relationship-map view mode.
2. **Character record** (`인물 기록`, the profile) in its book-page "document" layout: header (record number, name, one-liner, field table, images), the five fixed tabs `개요 · 말투 · 관계 · 게임 텍스트 · 비밀`, and the in-game text section (voice lines, archive documents).
3. **Fixed fields** `이름 · 표시 이름 · 한 줄 소개 · 이미지 · 태그 · 첫 등장 · 보이스 진행`, including the display-name override (`???` until reveal).
4. **The custom field engine** (`인물 항목`): 9 field types, value states (empty · `??` · value), visibility (`머리 · 목록 · 필터`), secret fields, start bundles, definition changes, age calculation, export names. The engine is shared by 장소 · 용어 · 세력 cards; this spec defines the engine and the settings screen with its `인물 · 장소 · 용어 · 세력` tabs, the World area renders it on its cards.
5. **Value history on the profile**: the field history sheet and the two edit buttons `이 값 고치기 · 여기서부터 바뀜` (rule 11 §1, applied to fields, display name and relation slots).
6. **Relationships** (rule 13): one relation per pair with `사이` + each side's `마음` + description + `호칭`, the viewer-side presentation, relation kinds (`관계 종류` settings), change over story time, the full relationship map, turning canvas lines into relations.
7. **Character hiding** (rule 19 §6) as seen in cast screens and the cast data contract for other screens.
8. **Deleting items** (rule 20) for 인물 · 장소 · 용어 · 세력: owned vs referencing data, the impact sheet, dotted references and their fix buttons, the check-center rows' data and bulk fixes, restore, permanent delete (name tags), merge, same-name notice. Screens for places, terms, maps belong to World; this spec owns the rules and test cases.
9. Cast-side ends of rule 23 (recorder `연결` line on the profile, `카드에 넣기` into character tabs, `⋯ › 인물로 만들기`).
10. What cast data goes into `.civilight`, game-engine JSON `characters[]`, and the open `인물 표 CSV`.

### 1.2 Hand-offs to other areas

No other `docs/spec/` file except `10-script.md` existed when this was written (checked 2026-11-22); areas are named by subject.

| Area | What it owns that this spec only references |
| --- | --- |
| 스크립트 (script editor, `10-script.md`) | Speaker picker sheet UI, `참고 · 속성` panel layout, `호칭 힌트` display layer, the `◆ 변화 기록` sheet container and its `필드 · 용어 공개` tabs, dotted speaker rendering in blocks, voice IDs. This spec supplies the cast data (CST-30, CST-37, CST-38, CST-41) |
| 시점 렌즈 · 변화 기록 (lens, change-record engine; rules 11, 12) | Lens state and chip, lens sheet, 집필 선 computation, `예정`, ChangeRecord storage, anchors (`블록 · 시점 · 세계 사건`), `근거 지워짐` relocation. This spec defines what a cast change record *contains* and how cast screens show it |
| 세계 (places, terms, factions, maps, timeline; rules 18, 21) | Cards, pins, regions, routes, sub-maps, term `쓰인 곳`, travel check. They reuse CST-19..CST-27 for card fields and follow CST-47 for deletion |
| 캔버스 (canvas) | The shared graph engine (nodes, lines, pan/zoom, line drawing). CST-33 (map) and CST-34 (line → relation) use it |
| 점검 센터 (check center) | Check list UI, filters, `맞음` / `맞다고 표시한 것` storage. CST-14, CST-39, CST-42 define cast rows |
| 저장 · 휴지통 · 내보내기 (storage, trash, export) | App DB, `.civilight` packaging, trash screen, export sheet and writers. CST-26, CST-35, CST-48 and §3.4 define cast content |
| 숨기기 · 등급 (hiding & rating; rule 19) | Hidden flag semantics at project level, system biometric prompt, re-lock timing, `LockRow`. CST-39 uses its unlock API |
| 이미지 (image slots, presets, crop, holo) | `이미지 고르기` sheet, crop editor, preset images, holo effect. CST-13 defines the cast slots |
| 라이프 레코더 (recorder; rule 23) | Recording, inbox, `연결`, `카드에 넣기` sheet. CST-49 defines what lands in a character |
| 새 프로젝트 · 온보딩 (new project, onboarding, tours) | New-project step 4 screen and the `인물 안내` tour (`TourCast`). CST-24 defines the bundle row's behaviour |
| 명령 칸 · 검색 (command bar & search) | Command palette, result list, 초성 matching. CST-27 defines field-value hits |
| 화면 · 접기 · 모션 (layout, fold, motion) | Window size classes, fold save/restore, global motion tokens. CST-06 gives cast specifics |
| 설정 · 프로젝트 설정 (settings) | Settings screens. CST-19 and CST-32 define the `인물 항목` and `관계 종류` rows and screens |

### 1.3 needsExternal rule

A feature is `needsExternal: yes` only if it needs a network service, cloud, an ML model, or OS integration beyond a normal app (Drive, speech-to-text, widgets, quick settings tile). The system photo picker, the biometric prompt (owned by 숨기기 · 등급) and the gyro holo effect (owned by 이미지) are referenced, not owned, so **no cast feature is needsExternal**.

---

## 2. Features

Priority: **P0** core (the app is useless without it) · **P1** planned for v1 · **P2** later / only if needed. Every feature here is `needsExternal: no` (see 1.3).

### A. List and views

#### CST-01 · 인물 목록 (character list) — P0
- **Header (glass top bar).** Small line: project name (`잿빛 새벽의 등불`). Title: `인물`. Right: `LensChip` (`최신 · 1-7`; `acc` outline when not 최신), `ViewToggle` `목록 | 카드` (CST-04), `+` icon button (new character, CST-05).
- **View mode control.** First content row: segmented control `목록 | 관계도` (selected = `pri` fill). `관계도` switches the screen to the relationship map (CST-33) without leaving the cast list.
- **Filter chips.** `전체 N` + one chip per project tag in tag order (`주연 · 등불 결사 · 하층 구역 · 상층`), single-select, `전체` default (CST-02). N counts characters that are not in the trash and not locked-hidden (`전체 11` both when 노아 is in the trash and when 노아 is hidden).
- **Grouped rows.** Section header per tag group (`.sec` style: `주연`, `등불 결사`, `하층 구역`). Row (`ListRow .li`): 40 px avatar (portrait 1:1 crop, else the name's first letter), name (Myeongjo 16 bold), one-line subtitle (small), right: first appearance episode (mono small, CST-14).
- **Subtitle composition.** `list-visible field values (max 2, in field order, empty skipped)` + `한 줄 소개` + display-name note, joined by ` · `. Examples at `최신 · 1-7`: 리엔 `17세 · 등불 결사 · 막내 등불지기` / `1-2`; 하르트 `결사의 검사 · 표시 이름 ??? (1-8까지)` / `1-7`; 세라 `기록관 · 리엔의 선배` / `1-1`; 마르타 `결사장 · 아직 등장 전` / `2-1`; 오웬 `밀수상 · 잿더미 시장` / `1-6`. Unknown values print as `??` with their unit (`??세`). `아직 등장 전` is appended when the first appearance is after the lens point (board-derived).
- **Tap a row** → character record (cover: full screen; inner list+detail: right pane, CST-06).
- **Command bar** hint: `이름 · 초성 · 태그로 찾기` (CST-02). Mic on the right opens the recorder.
- **Values follow the lens** (ages, 소속 …). Characters in the trash never appear. Locked hidden characters collapse into one `LockRow` (CST-39).
- **Empty state** (`EmptyCast`): chip `전체 0`, title `인물 없음`, sub `이름만 적어도 시작 · 나머지는 나중에`, buttons `인물 추가` (primary → CST-05) and `라이프 레코더에서` (opens the recorder inbox filtered to this project and `정리 안 됨`, where each record has `⋯ › 인물로 만들기`, CST-49).
- Sources: planning `인물` (인물 목록); `인물 항목` §4; rule 19 §6; rule 20 §2, §7; boards `Cast`, `ItemHide`, `EmptyCast`, `TwoCast`, `ColCast`, `MFoldList`.

#### CST-02 · 태그 필터 · 찾기 (tag filter and search) — P0
- Tapping a tag chip shows only that tag's characters (other sections hidden); `전체` shows all. One chip active at a time.
- Typing in the command bar on this screen filters the list live by name, 초성 (Korean initial consonants: `ㄹㅇ` matches 리엔) and tag name. Matching is NFC-normalised. Characters in the trash and locked-hidden characters never match.
- No results: use the shared empty-search state from 명령 칸 · 검색 (title `결과 없음`).
- Sources: planning `인물` (태그 필터, 이름 · 초성 · 태그 검색); boards `Cast`, `Picker` (`ㄹㅇ` · `초성 검색`).

#### CST-03 · 항목 필터 · 정렬 (filter and sort by custom fields) — P1
- Fields whose `필터` box is on (only types `고르기 · 여러 개 고르기 · 연결`) add filter chips to the list (example: `성별 · 종족 · 소속`). Picking a chip opens its options (or linked records) and filters to characters whose value matches. `숫자` fields allow a range filter.
- Sortable types `숫자 · 날짜 · 기념일 · 계산` give sort orders named `<항목 이름>순` (`신장순 · 나이순 · 생일순`). `기념일` sorts by month order. `??` and empty values sort last [PROPOSED].
- `짧은 글 · 긴 글` are search-only (CST-27).
- No board draws these controls ("Not drawn yet: list filter/sort by fields"); placement is an open question (§6 Q13).
- Sources: `인물 항목` §2 table, §4 table; memory character-fields.

#### CST-04 · 카드 보기 (card view) — P1
- `ViewToggle` `목록 | 카드` in the header; the chosen view is remembered per screen (local preference, not in `.civilight`).
- Card view = one horizontally scrolling row per tag group, header `<tag> · <count>` (`주연 · 3`, `하층 구역 · 3`); tag chips stay on top (`전체 11 · 주연 · 등불 결사 · 하층 구역`).
- Card: portrait 4:5 crop (or the first letter + `이미지 고르기`), title, right mono first appearance, subtitle line. Title is the display name in effect at the lens when one is set: `???` with subtitle `하르트 · 이름 공개 1-9`; otherwise the name with one-liner/list fields (`리엔` · `등불 결사 · 막내 등불지기`; `마르타` · `결사장`). A `홀로` badge marks a portrait with the holo effect on.
- Board sizes: first group 196×245 cards, later groups 132×165 cards.
- Motion: list → card morphs each thumbnail into its card in place, decel 360 ms, list text exits first (accel 160 ms); the row snaps to the nearest card (320 ms) with 8 % image parallax and a tick haptic. Reduce motion → 120 ms fade.
- Sources: planning `카드 보기 · 이미지`; boards `CastCards`, `MCardView`, `MCarousel`; motion spec `MotionSpec2`.

#### CST-05 · 인물 만들기 (create a character) — P0
- Entry points: header `+` on the list; `인물 추가` in the empty state; new-project step 4 `주인공 (선택)` (creates one character with that name); recorder `⋯ › 인물로 만들기` (CST-49).
- Only `이름` is required (`이름만 적어도 시작 · 나머지는 나중에`). The new character gets a new ULID key, the next record number (§6 Q11), empty project fields, and no relations.
- After creation the record opens on `개요`. No create sheet is drawn; [PROPOSED] a bottom sheet titled `인물 추가` with one `이름` field and primary `넣기`.
- Sources: boards `EmptyCast`, `NewStart`; rule 23 §4 table.

#### CST-06 · 펼친 화면 배치 (unfolded layouts) — P1
- Setting `설정 › 화면 › 펼친 화면 배치` (owned by Settings) picks one of two layouts for list modules:
  - **A `목록 + 상세`** (default, `TwoCast`): list 390 left | record 409 right. Tapping a row opens it in the right pane.
  - **B `가운데 한 칸`** (`ColCast`): the list in one 560 px reading column; a record opens full width at the same column.
- Fold continuity (`MFoldList`): unfolding keeps the list where it is and opens the selected character (e.g. 리엔) in the right pane, sliding in from 48 px right, decel 360 ms. Folding shows the pane touched last; if the record was being viewed it stays the record. With B the list just widens to 560. State is saved for devices that close apps on fold.
- Sources: planning `화면 · 디자인` (커버 화면과 펼친 화면, 접기 · 펼치기); boards `TwoCast`, `ColCast`, `MFoldList`, `LayoutSettings`.

### B. Character record (profile)

#### CST-07 · 인물 기록 머리 · 문서형 (record header, document layout) — P0
- **Cover layout** (`Profile`): the background image (인물 배경, 9:16 crop) fills the top ~430 px and fades and blurs progressively into `bg` (blur 22 px masked from 25 % to 60 %). Glass round back button (44 px) top-left; lens chip top-right (`시점 · 1-5` / `최신 · 1-7`). Content starts at y≈300:
  1. `.sec` line `인물 기록 007 · 시점 1-5` (record number · lens).
  2. Name, Myeongjo 40/48 weight 800.
  3. `한 줄 소개`, Myeongjo 16/27 `ink2`.
  4. Field table (`ListRow .row`: label `ink3` | value). Values with history show the start of the record in effect in `ink3` and `◆` in `acc`: `소속 | 등불 결사 1-3부터 ◆`. Numbers in mono (`154 cm`, `1-2`, `18 / 24`).
  5. Sections of the active tab (CST-08).
- **Field table rows** = fields marked `머리` in field order, then the fixed computed fields `첫 등장` and `보이스` unless hidden from the header (CST-14). Cover shows at most 6 rows; more → last row `항목 더 보기` expands the table. Example (리엔, game-profile bundle): `나이 17세 · 소속 등불 결사 ◆ · 신장 154 cm · 종족 인간 · 첫 등장 1-2` (+ `보이스 18 / 24`).
- **Bottom glass bar**: `Tabs` row `개요 · 말투 · 관계 · 게임 텍스트 · 비밀` above the `CommandBar` (`명령 또는 검색`, mic).
- **Inner**: same structure inside the 409 px right pane (layout A) or the 560 column (layout B); the background image uses its 4:3 crop.
- **Record ⋯ menu** [PROPOSED placement; no board draws it]: `숨기기` (CST-39), `지우기` (CST-40), `합치기` (CST-45); `모든 인물에 넣기` appears on single-character fields (CST-22).
- Sources: planning `인물` ("책 한 쪽처럼 읽힌다: 번호 · 큰 이름 · 한 줄 소개 · 항목 표 · 본문"); `인물 항목` §1, §4; boards `Profile`, `TwoCast`, `TourCast`, `RCProfileAdd`, `RTEditTwo`, `RTPlanned`; memory concept (document layout, portrait hero).

#### CST-08 · 탭 다섯 · 개요 본문 (five tabs, overview body) — P0
- Tabs are fixed and cannot be renamed or removed: `개요 · 말투 · 관계 · 게임 텍스트 · 비밀`. Selected tab = `acc` 2 px underline. Tapping a tab shows its content; the selected tab is kept on fold/unfold.
- **`개요`**:
  1. Field table (CST-07).
  2. `항목` list: fields not marked `머리` and not `긴 글`, same row style [PROPOSED order: field order].
  3. Row `빈 항목 N · 보기` (left, small) and `+ 항목` (right, CST-22). Empty fields are hidden in view mode; `보기` expands them; editing shows every field as an input.
  4. Each `긴 글` field becomes a numbered section in field order: `Ⅰ 개요 · Ⅱ 성격 · Ⅲ 과거` (`.sec` header + Myeongjo 16/28 body with `@` links). Empty long-text sections are hidden like other empty fields.
  5. If recorder records are linked to this character and not yet put anywhere, the first section header carries the right-aligned link `레코더 · 정리 안 됨 1 ›` (CST-49).
- `말투` → CST-15. `관계` → CST-28. `게임 텍스트` → CST-16, CST-17. `비밀` → CST-18.
- Sources: planning `인물` 탭 5개; `인물 항목` §1 (탭은 그대로), §4; DS `Tabs`; boards `Profile`, `RCProfileAdd`, `Relations`, `Voice`.

#### CST-09 · 항목 값 넣기 · 세 상태 (entering values, three states) — P0
- Every field value is in one of three states: **비어 있음** (empty), **알 수 없음 `??`** (unknown: the author has not decided, or the game profile shows `??`), **값** (a value). Unknown is not counted as an empty field by checks.
- Tapping a field row opens the field sheet (title = field name, `닫기`). Inputs per type (CST-20): short text line(s); long text in place in its section (Myeongjo, `@` links); number + unit suffix; radio list (`고르기`, plus a free-text line when `직접 쓰기` is allowed); checkboxes (`여러 개 고르기`); year or year·month·day (`날짜`, world-calendar month names when the project uses one); month·day (`기념일`); record picker with search (`연결`); read-only result (`계산`). Each sheet has an `알 수 없음` toggle that stores `??` [PROPOSED control; the state itself is from the source].
- Fields **without** history: the sheet's single primary button saves [PROPOSED label `이 값 고치기`]. Fields **with** history: CST-10's two buttons.
- Saves are immediate (autosave to the app DB); undo is a toast where other areas use one.
- Sources: `인물 항목` §2 (값의 세 상태), §3; rule 11 §1; boards `RTEditTwo`, `LensEdit`.

#### CST-10 · 시점 렌즈 · 변화 기록 · 고치기 두 버튼 (lens, history, two edit buttons) — P0
- **All values follow the lens.** At `최신 · 1-7` 최신 means "written so far" (up to the 집필 선, end of 1-7 씬 3). Changes recorded after the 집필 선 are `예정` and are not applied. With no manuscript the chip reads `최신 · 전체` and every change applies.
- **Point semantics.** A point means "after that unit ends"; a record "from X" includes X. Records anchored to a point (`시점` anchor) apply from the beginning of that episode/scene; records anchored to a block apply from that block inclusive (rule 12 §1).
- **`예정` rows** (`RTPlanned`): under a field row whose next record is past the 집필 선, a dimmed right-aligned row `예정 · 2-5 | 등불지기장` (mono label, 75 % opacity, `ink3`). Same for relations: `예정 · 1-9 | → 신뢰`. Footnote under the table: `예정 · 집필 선(1-7 씬 3) 뒤 기록`.
- **Field history sheet** (`FieldHistory`): opened by tapping `◆` or the value row's history link. Title `<항목> · 변화 기록`, `닫기`; sub `시점별 값 · 프로필에는 1-5 값`. One row per record in story order: point (mono 12, `ink3`) | value (Myeongjo 16 bold) + note (small, e.g. `세라의 안내로 합류`). The record in effect at the lens is highlighted (`sel` fill + 2 px `acc` left bar). Records after the lens are `ink3` with note `지금 시점보다 뒤`; records after the 집필 선 say `예정`. Buttons: `시점 추가` (ghost; adds a point-anchored record: point picker → value) and `<point> 값 고치기` (primary; e.g. `1-3 값 고치기` edits the record in effect).
- **Two edit buttons** (rule 11 §1; `RTEditTwo`). Editing a value that has history shows, under the input: `지금 값 | 등불 결사 · 1-3부터`, the history line `1-1 없음 · 1-3 등불 결사 · 예정 2-5 등불지기장`, and two stacked buttons:
  - `이 값 고치기` (default, primary) with sub `1-3 기록을 고침 · 1-3부터 새 값` → overwrites the value of the record in effect.
  - `여기서부터 바뀜 · 1-7` (ghost) with sub `새 ◆ · 집필 선 1-7 씬 3 뒤` → creates a new `◆` at the 집필 선. [PROPOSED anchor, §6 Q27] The record is anchored `SceneEnd(1-7 씬 3)`: it sits after the last block of the scene that holds the 집필 선, **is applied at `최신`** (the lens includes the 집필 선 position) and is not applied at any lens before the end of that scene. Blocks written later in that scene come after it.
- **At a past lens** (`LensEdit`): a glass banner under the header with `acc` outline: `1-5 시점 값 고치는 중` / `고치면 1-5부터 적용` and ghost `최신으로` (sets the lens back to 최신). The field shows `1-5부터 · 다음 변화 2-5`. The same two buttons are offered (rule 11 §1): `이 값 고치기` corrects the record in effect at 1-5; `여기서부터 바뀜 · 1-5` creates a point record `1-5 #—` whose previous value is shown (`저장 시 ◆ 변화 기록 · 1-5 #— · 이전 값 ‘등불 결사’`). See §6 Q3 for the board/rule mismatch.
- A change added at a point that has no manuscript yet is `예정`; when that scene gets its first block it becomes applied automatically. When the 집필 선 moves back (scene deleted), records in between return to `예정`; nothing is deleted.
- Calculated fields (`계산`) never have history. Every other field (including `숫자`, e.g. 신장 growing in 2장) may; there is no per-field switch.
- Sources: rule 11 §1–2; rule 12 §1 (anchors); `인물 항목` §6 (시점에 따라 바뀜); planning `공통 개념` (시점 렌즈, 변화 기록); boards `FieldHistory`, `RTEditTwo`, `RTPlanned`, `LensEdit`, `Lens`.

#### CST-11 · 표시 이름 (display-name override) — P0
- Fixed field. Holds the name shown to players instead of the real name (`???`, or any free string such as a title). It is time-varying: it has `◆` records like other fields (rule 12 §1 lists "필드 · 관계 · 표시 이름"). Revealing the name = a record at the reveal point that clears the override [PROPOSED storage: value `null` = show `이름`].
- Sample: 하르트 `표시 이름 = ???` until a record at 1-9 clears it. Lens 1-6 / 1-7 / 1-8 → `???`; lens 1-9 → `하르트`. At `최신 · 1-7` the 1-9 record is `예정`, so `???` is shown.
- Presentation (all derived from the history):
  - List subtitle: `표시 이름 ??? (1-8까지)` (last episode before the reveal episode; "(1-8까지)" ≡ "1-9에서 공개").
  - Card title `???`, subtitle `하르트 · 이름 공개 1-9`.
  - Speaker picker row sub `표시 이름 ???`.
  - Script `참고` panel row `표시 이름 | ??? (1-9 공개)`; `속성` panel field `???` with `1-9에서 공개` (script area renders).
  - Script speaker label shows the display name with the real name beside it (`??? 하르트`).
- If no reveal record exists, the list note is `표시 이름 ???` [PROPOSED].
- Sources: planning `스크립트 편집기` (표시 이름 덮어쓰기); `인물 항목` §1; rule 11 §1–2; rule 12 §1; memory vocabulary (`??? (1-8까지)`); boards `Cast`, `CastCards`, `Picker`, `CSheetRef`, `CSheetProps`, `WideRef`.

#### CST-12 · 태그 (tags) — P0
- Fixed field: an ordered set of project tags per character (`주연 · 등불 결사 · 하층 구역 · 상층`). Tags drive list grouping (CST-01), list filter chips (CST-02), card rows (CST-04) and the speaker picker's filter chips (`전체 · 주연 · 등불 결사 · 하층 구역`).
- Tags are project-wide records (name, order). Editing UI is not drawn; [PROPOSED] the field sheet for `태그` shows the project tags as checkable chips plus a text line to add a new tag.
- Sources: `인물 항목` §1 (태그 → 목록 묶음 · 화자 고르기 필터); planning `인물` (태그 필터 주연 · 세력 · 구역); boards `Cast`, `Picker`, `CastCards`.

#### CST-13 · 이미지 · 초상과 배경 (portrait and background) — P1
- Two image slots per character:
  - **인물 초상** (portrait): crops `카드 4:5` and `아바타 1:1`; holo effect optional (default off).
  - **인물 배경** (background): crops `커버 9:16` (cover) and `펼친 화면 4:3` (inner); no holo ("글자 뒤"); shown at the top of the record, fading downward.
- Each slot takes one of the 8 built-in presets (`잿빛 새벽 · 등불 · 심해 · 안개 숲 · 사막 유적 · 성운 · 붉은 성채 · 설원`) or a gallery/file image. The original is kept; each ratio is cropped separately with fixed ratios and can be re-cropped any time. No image → first-letter card + `이미지 고르기`.
- Picking, cropping, presets and holo belong to the image area (`ImagePick`, `ImageCrop`, `ImageSlots`). This feature defines the slots and where each crop is used.
- Sources: planning `카드 보기 · 이미지`; boards `ImageSlots`, `CastCards`, `Profile`.

#### CST-14 · 첫 등장 · 보이스 진행 (computed fixed fields) — P1
- **첫 등장** (first appearance): the earliest scene in story order where the character is the speaker of a block or is in the scene's `등장` list (speakers are added automatically; silent characters are added by hand in the scene sheet). Shown as the episode (`1-2`). Scenes in the trash do not count. Never stored.
- **보이스 진행** (voice progress): `<filled> / <total>` over the character's voice lines (CST-16), shown as `18 / 24` in the header and `보이스 · 18 / 24` on the `게임 텍스트` tab with a flat progress bar. `filled` = lines whose state is not `비어 있음` (consistent with the check row `보이스 6/24 비어 있음`; see §6 Q5).
- Both are fixed fields: they cannot be deleted but can be hidden from the record header (`첫 등장 · 보이스`) [PROPOSED control: a switch per fixed field in the `고정 항목` box of CST-19].
- **Check row** (rendered by the check center, filter `보이스`): `▲ 보이스 비어 있음` · location `인물 · 리엔` · body `리엔 · 신뢰 상승 후` / `보이스 6/24 비어 있음` · action `보이스 쓰기` (opens `게임 텍스트` at the first empty line). The board draws this row with an `ink3` label.
- Sources: `인물 항목` §1 (첫 등장 · 보이스 진행 계산, 감출 수는 있다); `저장 파일 양식` §4 (계산값 적지 않음); planning `지도 ↔ 연표` (등장 인물); boards `Profile`, `Voice`, `Check`, `Cast`.

#### CST-15 · 말투 탭 (speech tab) — P1
- Content: the character's way of speaking (1인칭 · 어미 · 말버릇) as Myeongjo prose under `.sec` `말투`, e.g. `1인칭은 ‘저’. 누구에게나 존댓말을 쓰고, 확신이 없으면 ‘…요’로 말끝을 흐린다.`; then the collected 호칭 chips (`Tag` style), one per partner, `<partner> → <호칭>`: `하르트 → 하르트 씨 · 세라 → 선배 · 오웬 → 아저씨`.
- The 호칭 chips are **not stored here**: they are read from the viewer's `호칭` slot of each relation at the lens (CST-31). Tapping a chip edits that relation slot; edits from either place are the same value.
- The prose body accepts `⌁` memo paragraphs inserted by the recorder (CST-49).
- See §6 Q9 for whether 1인칭 · 어미 · 말버릇 are separate fields and where the panel's one-line `말투` comes from.
- Sources: planning `인물` (말투); rule 13 §5; boards `Relations`; rule 23 §4.

#### CST-16 · 게임 텍스트 · 보이스 대사 (in-game voice lines) — P1
- Tab header: `.sec` `보이스 · 18 / 24` with right `게임 순서`; flat progress bar (4 px, `ink` fill on `tint`, width = filled/total). Header right of the record: `+` adds a voice line at the end.
- Each line in game order: number (mono 11, `01`), slot name (Noto Sans 13 semibold: `임명 · 대화 1 · 대화 2 · 신뢰 상승 후`), right state label, then the text (Myeongjo 15/24; `—` in `ink3` when empty).
- States and colours: `녹음 대본 완료` (`ink2`), `쓰는 중` (`acc`), `비어 있음` (`ink3`). `비어 있음` whenever the text is empty [PROPOSED: derived]; otherwise the author picks `쓰는 중` or `녹음 대본 완료` [PROPOSED control: tap the state label].
- Sample: `01 임명 녹음 대본 완료 “저… 정말 제가 맡아도 되는 걸까요?”` · `02 대화 1 녹음 대본 완료 “등불은요, 꺼지기 직전에 제일 밝아져요.”` · `03 대화 2 쓰는 중 “하르트 씨는… 아니에요, 아무것도.”` · `04 신뢰 상승 후 비어 있음 —`.
- Rule 21 §4 table: profile game text (voice lines and archive documents) **goes to the game**, gets **no term candidates**, and whether its `@` links create `▲ 공개 전 사용` is **undecided** ("미정, 위치 없음"); its links appear on term cards under `원고 밖` with the tag `게임 텍스트`. Until decided, no `▲` is raised from profile game text (§6 Q28). Profile voice lines have no voice ID in v1 (CST-52).
- Sources: planning `인물` (게임 텍스트); rule 21 §4 table (rows "인물 프로필 게임 텍스트"); rule 12 §6 (범위); board `Voice`.

#### CST-17 · 게임 텍스트 · 아카이브 문서 (archive documents) — P1
- Section `.sec` `아카이브 문서 · <count>` below the voice lines. Row per document: `기록 <n> · <title>` with sub `<글자 수>자 · <unlock point> 이후 열림`, or `비어 있음 · <unlock point> 이후 열림` when empty. Samples: `기록 1 · 등불지기의 일지` / `1,120자 · 1-7 이후 열림`; `기록 2 · 대정전 그날` / `비어 있음 · 2장 이후 열림`.
- The unlock point is an episode or chapter. Character count is derived (no spaces rule given; [PROPOSED] count all characters except line breaks, formatted with thousands separators).
- Tap → full-width Myeongjo document editor with `@` links [PROPOSED; not drawn].
- Sources: planning `인물` (아카이브 문서 · 글자 수 · 열리는 시점); board `Voice`.

#### CST-18 · 비밀 탭 · 비밀 항목 (secret tab and secret fields) — P1
- The `비밀` tab holds settings not yet told to players: (1) every field whose `비밀 항목` switch is on (CST-21), shown only here, never in the header, list, filters, speaker picker or panels; (2) a free prose body that accepts recorder memo paragraphs (CST-49).
- Export: the export sheet's display-layer list gets `비밀 항목 포함`, default off (CST-26).
- The 비밀 tab is called "항목 숨기기 대상" (planning; rule 19 §5 버린 안); whether it can be locked separately is §6 Q10.
- Sources: planning `인물` (비밀); `인물 항목` §1, §8; rule 19 §5; board `RCFieldAdd` (`비밀 항목 · 비밀 탭에만 보임`).

### C. Custom fields (`인물 항목`)

#### CST-19 · 인물 항목 설정 화면 (field settings screen) — P0
- Entry: `프로젝트 설정` › section `인물` › row `인물 항목` with sub showing the bundle and real counts, `게임 프로필 · 고정 7 · 프로젝트 7 · 장소 · 용어 · 세력도`, and right `고치기 ›`.
- Screen (`RCFields`, cover): head small `프로젝트 설정`, title `인물 항목`. Tabs `인물 · 장소 · 용어 · 세력` (same engine for World cards; each tab lists that kind's fields).
- `.sec` `고정 항목 · 7` + a `tint` box (dimmed): `이름 · 표시 이름 · 한 줄 소개 · 이미지 · 태그 · 첫 등장 · 보이스 진행`.
- `.sec` `프로젝트 항목 · <n>` with right `끌어서 순서`. Rows: visible grip handle (aria `순서 바꾸기`; drag reorders; no hidden gesture), name (15 bold), sub `<종류> · <detail> · <places>`, chevron → field edit sheet (CST-21 layout, CST-23 rules). Sample rows (game-profile project as drawn):
  `출생 · 날짜 · 연도 · 머리` · `나이 · 계산 · 출생에서 · 머리 · 목록` · `소속 · 연결 · 세력 · 머리 · 목록 · 필터` · `신장 · 숫자 · cm · 머리` · `성별 · 고르기 · 보기 3 · 필터` · `종족 · 고르기 · 보기 4 · 목록 · 필터` · `성격 · 긴 글 · 개요`.
- Row `+ 항목` (plus icon) → CST-21.
- Bottom row: title `시작 묶음 · 게임 프로필`, sub `빈 항목은 보기에서 숨김 · 값은 ?? 가능`, right `항목 더하기 ›` → bundle picker (CST-24).
- The header-table order on records follows this order.
- Sources: `인물 항목` §3 (프로젝트 설정 › 인물 항목 sketch), §5; memory rules-19-23 (real counts `고정 7 · 프로젝트 7`, per-field `머리 · 목록 · 필터`); boards `RCFields`, `ProjSettings`.

#### CST-20 · 항목 종류 아홉 가지 (nine field types) — P0

| 종류 | Example | Input | Filter · sort | Type-specific settings |
| --- | --- | --- | --- | --- |
| `짧은 글` | 코드네임, 별명 | one line; with `여러 개` on, several lines (aliases) | search only | `여러 개` on/off |
| `긴 글` | 성격, 과거 | Myeongjo body, `@` links | search only; becomes a section of `개요` | — |
| `숫자` | 신장 154 cm, 체중 | number + unit text | sort · range filter | unit text (never converted) |
| `고르기` | 성별, 종족, 혈액형 | one of the defined options | filter chips | options; `직접 쓰기` allowed or not |
| `여러 개 고르기` | 특기, 무기 | several options | filter chips | options |
| `날짜` | 출생 | year, or year · month · day | sort; source of age | precision `연도` / `연 · 월 · 일` |
| `기념일` | 생일 | month · day (no year) | sort by month order | — |
| `연결` | 소속(세력), 출신지(장소), 능력(용어) | pick from project records | filter chips; the target card shows `이 항목으로 잇는 인물` | target kind: `세력 · 장소 · 용어` only (never 인물; character-to-character goes through relations) |
| `계산` | 나이 | none (from another field) | sort | formula + source (`출생에서`) |

- Values: empty · `??` · value (CST-09). Display of `??` keeps the unit (`??세`, `?? cm`).
- Rejected: everything as one short text (no gender filter, no height sort, no age calc).
- Sources: `인물 항목` §2; rule 13 §8 (연결 → 세력 · 장소 · 용어 only, replaces the earlier 스승(인물) example).

#### CST-21 · + 항목 시트 (add-field sheet from settings) — P0
- Sheet (`RCFieldAdd`, cover; side popover on inner): title `항목 넣기`, `닫기`; sub `인물 · 모든 인물에`.
- Rows, top to bottom: `이름` input (focused) → `종류` radio grid (2 columns, 9 types in table order; selected = `sel` fill) → type-specific inputs (number: `단위` input + note `값은 바꿔 계산 안 함`; choice types: option list editor; `고르기`: `직접 쓰기` switch; `연결`: target kind `세력 · 장소 · 용어`; `짧은 글`: `여러 개` switch; `날짜`: `연도 · 연 · 월 · 일`; `계산`: source field) → `보이는 곳` checkboxes `머리 · 목록 · 필터` (`필터` only enabled for `고르기 · 여러 개 고르기 · 연결`) → `비밀 항목` switch with sub `비밀 탭에만 보임` → primary `넣기`.
- Optional `내보내기 이름` (export key, e.g. `height`) [PROPOSED position: under `이름`]; empty = the field name (CST-26).
- `넣기` with an empty name is disabled. A name equal to an existing field of the same kind is allowed but [PROPOSED] shows `◇ 같은 이름 항목 있음` under the input.
- Sources: `인물 항목` §3 (`+ 항목` 시트 순서), §8 (내보내기 이름); board `RCFieldAdd`.

#### CST-22 · 인물 기록에서 + 항목 (add a field from a record) — P1
- `개요` row `빈 항목 N · 보기 | + 항목` (`RCProfileAdd`). Sheet: title `항목 넣기`, sub `<인물> · 인물 기록 <번호>` (`리엔 · 인물 기록 007`); `이름` input (`좋아하는 것`); row `종류 | 짧은 글 ›` (opens the type list); `어디에` with two radio cards:
  - `모든 인물에` / `인물 항목에 추가 · 다른 인물은 빈 칸` (**default**): becomes a project field; other characters are empty.
  - `이 인물만` / `<인물>에게만` (`리엔에게만`): a field only this character has; a small mark next to its name in the record; record ⋯ (or the field's sheet) offers `모든 인물에 넣기` to promote it later.
  - primary `넣기`.
- The app never guesses which one; the choice is always visible.
- Sources: `인물 항목` §3 (인물 기록에서 바로); board `RCProfileAdd`.

#### CST-23 · 항목 정의 바꾸기 (changing field definitions) — P1
Fields have a hidden permanent key; values, `◆` history and export follow the key.

| Change | Behaviour | Copy |
| --- | --- | --- |
| Rename (`키 → 신장`) | values kept | — |
| Reorder | by the grip handle; record header order follows | `끌어서 순서` |
| Unit (`cm → 척`) | only the label changes; numbers are never converted | sheet note `환산하지 않음` |
| Option rename (`여 → 여성`) | every character with that option shows the new label | — |
| Option delete | characters with it keep the text as a fallback value with `◇ 보기에 없는 값`; before deleting, the sheet lists them (`리엔 외 3명`) | `◇ 보기에 없는 값` |
| Type change | only the pairs below; values that cannot convert stay as their original text with `◇` and a count | `◇ 바꾸지 못한 값 3` |
| Hide (숨기기) | values stay, shown nowhere; unhiding restores them | — |
| Delete (지우기) | sheet shows how many characters have a value (`값이 있는 인물 12명`); the field goes to the trash (filter `인물`) for 30 days; restoring brings back values and `◆` | `값이 있는 인물 12명` |

Allowed type conversions:

| From | To | Values |
| --- | --- | --- |
| 짧은 글 | 고르기 · 여러 개 고르기 | the same text becomes an option; options are built from existing values |
| 짧은 글 | 숫자 | only text that reads as a number (`163cm` → `163`); others `◇` |
| 짧은 글 | 연결 | only names equal to a project record of the target kind; others `◇` |
| 고르기 | 여러 개 고르기 | all |
| 숫자 · 고르기 · 연결 · 날짜 | 짧은 글 | all (the visible text) |
| 긴 글 | any other | not allowed: make a new field and copy by hand |

Any pair not in the table is not offered. Rejected: blocking type changes once values exist.
- Sources: `인물 항목` §7.

#### CST-24 · 시작 묶음 (start bundles) — P1

| Bundle | Fields |
| --- | --- |
| `기본` (default) | 출생 (날짜 · 연도) · 나이 (계산) · 생일 (기념일) · 소속 (연결 · 세력) · 성격 (긴 글) |
| `게임 프로필` | 기본 + 코드네임 (짧은 글) · 성별 (고르기) · 종족 (고르기) · 출신지 (연결 · 장소) · 신장 (숫자 · cm) · 특기 (여러 개 고르기) |
| `비어 있음` | fixed fields only |
| `다른 프로젝트에서` | that project's field definitions only, no values; hidden projects appear only while unlocked |

- Chosen in new-project step 4 `시작 재료` as one row `인물 항목 | 출생 · 나이 · 생일 · 소속 · 성격 | 기본 ›` (no extra step). Default is always `기본`; the app never picks by project format (no "설정만 → 게임 프로필" guessing).
- Changing later: `프로젝트 설정 › 인물 항목` › `항목 더하기 ›` (= `시작 묶음`), which **adds missing fields only** and never deletes or changes existing fields or values.
- A bundle is only a starting point; fields can be added and removed freely afterwards.
- The example project uses `게임 프로필`; onboarding shows 신장 · 종족 on 리엔.
- Same mechanism for 장소 · 용어 · 세력 (bundles for those kinds are not defined in the sources; they start `비어 있음` [PROPOSED]).
- Sources: `인물 항목` §5; memory rules-19-23 ("시작 묶음 = '항목 더하기' (adds missing fields only)"); boards `NewStart`, `RCFields`.

#### CST-25 · 나이 계산 · 출생과 생일 (age, birth and birthday) — P1
- `출생` (날짜, "when born") and `생일` (기념일, "which day every year") are separate. `출생` (date) and `출신지` (place link) are separate fields in bundles.
- If `출생` has month and day, `생일` is filled automatically (§6 Q7: stored copy or derived).
- `나이` (계산, source `출생`) is computed at the point being viewed (the lens; in a flashback scene, that scene's year).

| Project time setting | 출생 | 생일 | 나이 |
| --- | --- | --- | --- |
| `연도만` (default) | year | 1월–12월 · 일 | lens year − birth year; not 만 나이 (birthday unknown in the year) |
| `세계 달력` | y · m · d (world month names) | world month · day | 만 나이 when the scene's 때 has a date (birthday passed or not) |
| `시간 없이` | none | 1월–12월 · 일 | entered directly as a number |

- If `출생` is empty or `??`, `나이` is entered directly and may have `◆` records like any other field. Unknown age prints `??세`.
- Example: 리엔 is 17세 at lens 1-4 (1장, year 1200) and 12세 in the 대정전 flashback (1195); hence 출생 = 1183 (derived). Timeline events show the age at that event (`대정전 1195 · 리엔 12세`); 하르트 shows `??세`.
- How a lens point maps to a year in `연도만` mode is §6 Q20.
- Sources: `인물 항목` §6; planning `세계 · 연표 · 지도` (연표: 사건마다 인물 나이); rule 11 §4 (회상).

#### CST-26 · 항목 내보내기 (field export) — P1
- Game-engine JSON: each `characters[]` entry gets a `fields` object keyed by the field's export name (`height`); empty export name = the field name. Renaming a field keeps the export name, so engine code does not break. Fields with `◆` records export as a list of values per point.
- Secret fields: export sheet display-layer switch `비밀 항목 포함`, default off.
- Hidden characters in JSON: only `id · 이름 · 표시 이름`, no profile (rule 19 §5).
- Relation export: CST-35. `인물 표 CSV`: CST-51 (undecided).
- Sources: `인물 항목` §8; rule 19 §5; planning `저장 · 백업 · 내보내기` (JSON 골격: `characters`).

#### CST-27 · 항목 값 검색 (searching field values) — P1
- `짧은 글 · 고르기 · 여러 개 고르기 · 연결` values are hit by the command-bar search: `성직자` finds characters whose 특기 is 성직자. The result row names the field and value, e.g. `종족 · 인간`, `특기 · 성직자`. `긴 글` text is searched like body text. Search results show the whole sentence with the match boxed inline (`mark.hit`).
- Hidden characters: card results excluded while locked, footer `숨긴 항목 제외`. Trashed characters excluded.
- Sources: `인물 항목` §8 (검색); rule 19 §6; memory feedback-search-highlight.

### D. Relationships (rule 13)

#### CST-28 · 관계의 모양 · 관계 탭 (relation shape and the 관계 tab) — P0
- **Shape.** Between two characters there is **at most one relation**. It has:
  - `사이` (between): a fact true for both sides (선배 ↔ 후배, 동료, 거래 상대). Kind + one-line description.
  - `<A>의 마음` and `<B>의 마음` (each side's mind): one-directional (경계, 신뢰, 의지, 동경). Each kind + one-line description.
  - `호칭` per side: what A calls B (CST-31).
  All parts may be empty. Kinds feed the map, filters and checks; descriptions are for people.
- **Before the first change** the relation shows `만나기 전` and draws no line. `만나기 전` is not a kind and is never stored (it replaces the earlier "모르는 사이"). Two people who know each other without a particular feeling get the mind kind `무관심`.
- **Viewer side.** Every screen shows the relation from the character being viewed (record owner; speaker in the script panel): own mind first with `→`, the other's mind after with `←`; `사이` as a label next to the partner's name (`세라 · 선배` = 세라 is 리엔's 선배). List rows and cards show no relations.
- **관계 tab** (`Relations`): `.sec` `관계 · 시점 1-7` with right link `관계도로 보기` (opens CST-33 with this character focused). One block per partner:
  - avatar 36, partner name (+ ` · <사이 label>`), sub line = viewer-side summary at the lens: `→ 경계 · ← 보호`, `→ 의지 · 의지하는 선배` (mind + description), or `마음 적지 않음` when neither side has a mind; `◆` at the right when the relation has records.
  - history steps (mono point + text, `ink3`; the step in effect is `ink`): `1-7 · 처음 만남 · → 경계 · ← 보호`; `1-9 · 사이 동료 · → 신뢰` marked `예정` (past the 집필 선) or `지금 시점보다 뒤` (after the lens but written).
  - Sample at `시점 · 1-7`: 하르트 `→ 경계 · ← 보호` ◆ (steps 1-7, 1-9 예정); 세라 · 선배 `→ 의지 · 의지하는 선배` (steps `1-1 · 사이 선배 · 결사 안내`, `1-3 · → 의지`); 오웬 · 거래 상대 `마음 적지 않음` (step `1-6 · 사이 거래 상대`).
  - Bottom rows (dashed top border): deleted partner `노아` (dotted underline) `◇ 지운 인물 · 관계 1` with `되살리기 · 이대로 두기` (CST-41); locked hidden partner `숨긴 인물과의 관계 1` (CST-39).
  - `+ 관계` → CST-29.
- Many-person relations (삼각관계, "결사 사람들") are written pairwise; groups belong to tags and 소속.
- Rejected: two directed lines only; one undirected line only.
- Sources: rule 13 §2, §4, §6, §8; boards `Relations`, `RTPlanned`; source note: `Relations` labels the 1-9 step `지금 시점보다 뒤`, rule 11 + `RTPlanned` call steps past the 집필 선 `예정`; this spec uses both meanings as defined above (§6 Q19).

#### CST-29 · 관계 만들기 · 고치기 (create and edit a relation) — P0
- Entry: `관계` tab `+ 관계`; map card `관계 고치기`; a relation row tap.
- `+ 관계`: partner picker (same sheet as the speaker picker: search, 초성, tag chips) → relation sheet. If the pair already has a relation, the sheet opens that relation (one per pair).
- Relation sheet [PROPOSED layout; copy from the change-record and line-label sheets]: title `<A> · <B>`; `무엇으로 적을지` chips `사이 · <A>의 마음 · <B>의 마음 · 호칭`; per selected slot: kind chips from the project list + `+ 새 종류`, one-line `설명 (선택)`; `시작 시점` row (default = the lens position, written as a point-anchored record, rule 12 §1); primary `관계로 만들기` (new) or the two buttons of CST-10 (existing slot with history).
- **Pair kinds fill the other side**: choosing `선배` for 세라 in 리엔's sheet stores 사이 = 선배 ↔ 후배 with 세라 holding `선배`; 세라's view shows `리엔 · 후배`. This is a fact, not a guess.
- **Minds are never filled for the other side**: writing 리엔 → 경계 leaves 하르트's mind empty.
- Sources: rule 13 §3, §7 (관계 탭 `+ 관계` · 상대 고르기 · 시점은 렌즈 위치); boards `CastMap`, `CastMapC` (`관계 고치기`).

#### CST-30 · 원고에서 관계 변화 기록 (relation change from a block) — P0
- The script's `◆ 변화 기록` sheet (`ChangeCreate`, container owned by the script area) has segment `관계 · 필드 · 용어 공개`. The `관계` segment content is defined here:
  - title `변화 기록`, right `취소`; sub `#043 · 1-7 씬 3`.
  - pair chips with avatars `리엔 · 하르트` (tap to change either person).
  - `바꾸는 것` chips `리엔의 마음 · 하르트의 마음 · 사이 · 호칭` (multi-select). Turning on two chips asks for two values and creates two `◆`.
  - `값 · <current> →` + kind chips `경계 · 신뢰 · 의지 · 동경 · 불신 · + 새 종류`; for the first record the current value reads `만나기 전`. Optional description.
  - `시작 시점 | 이 블록 · 1-7 #043` (block anchor: applies from this block inclusive).
  - `반영 | 인물 · 관계도 · 시점 렌즈` (informational).
  - primary `기록`.
- The old "누가 누구를 · 한쪽" control and "모르는 사이" value are gone.
- Relation `◆` records follow rule 12 anchoring (deleting #043 moves the record to the next surviving block, `근거 지워짐`).
- Sources: rule 13 §6 (변화 기록 시트가 바뀐다), §7; rule 12 §1–2; board `ChangeCreate`.

#### CST-31 · 호칭 (forms of address) — P1
- A per-side slot of the relation: "what A calls B", beside A's mind and description. The 말투 tab only collects it (CST-15); editing from either place changes the same value.
- 호칭 has `◆` history: if 리엔 calls 하르트 `하르트` instead of `하르트 씨` from 1-9, a `◆` is made at that block.
- The script `호칭 힌트` layer shows the 호칭 in effect at that block. Other words used inside the dialogue are not checked (the app cannot tell which word is an address).
- Writing a 호칭 for a partner with no relation creates the relation with empty 사이 and minds; the map draws no line for it.
- Sources: rule 13 §5; planning `인물` (말투); board `Relations`, `CSheetRef` (`리엔을 | ‘꼬마’`).

#### CST-32 · 관계 종류 설정 (relation kinds) — P1
- Entry: `프로젝트 설정` › `인물` › `관계 종류` with counts (`사이 12 · 마음 11`) and `바꾸기 ›`. Same screen family as `인물 항목`.
- Screen (`RCRelTypes`): head small `프로젝트 설정`, title `관계 종류`.
  - `.sec` `사이 · <n>` with right `둘에게 같은 사실`; `대칭 · <n>` as chips; `짝 · <n>` with right `한쪽 적으면 반대쪽도`, rows `<a> ↔ <b>` (Myeongjo 15 bold) + `쓰임 <n>` + chevron; chip `+ 새 종류`.
  - `.sec` `마음 · <n>` with right `한쪽만 · 반대쪽은 비워 둠`; chips + `+ 새 종류`.
- **Default list (rule 13 §3):** 사이 · 대칭 `동료 · 형제자매 · 배우자 · 연인 · 친구 · 거래 상대 · 적`; 사이 · 짝 `부모 ↔ 자식 · 스승 ↔ 제자 · 상관 ↔ 부하 · 선배 ↔ 후배`; 마음 `신뢰 · 의지 · 동경 · 호감 · 보호 · 경계 · 불신 · 질투 · 증오 · 죄책감 · 무관심`. The board shows a different list; §6 Q1.
- New kind: choose `사이 / 마음`; for 사이 choose `대칭 / 짝 이름` (짝 asks for the other side's name). `+ 새 종류` also appears in every kind picker.
- Editing (same as field options):
  - rename → every relation shows the new name;
  - delete → relations using it keep the kind's text as description and show `◇` (merging with an existing description: §6 Q16);
  - changing a pair's other name later renames the already written other side;
  - the `사이 / 마음` and `대칭 / 짝` switches can be changed while in use; a preview shows the effect on existing relations first [PROPOSED copy `바뀌는 관계 N`].
- Sources: rule 13 §3; boards `RCRelTypes`, `ProjSettings`.

#### CST-33 · 전체 관계도 (full relationship map) — P1
- A view mode of the character list (`목록 | 관계도`), same engine as the canvas. Header: small `인물 · <N>명 중 <M>명` (cover `인물 · 11명 중 4명`; inner `인물 · 관계도로 보기 · 11명 중 5명`; with locked hidden: `12명 중 5명 · 숨김 1`), title `관계도` (cover) / `전체 관계도` (inner), lens chip `시점 · 1-7`, filter icon (inner).
- Nodes: rectangular cards (name + subtitle: `주연`, `결사 · 검사`, `기록관`, `밀수상`, `결사장 · 2장`). No circles.
- Lines (at the lens): **solid** when the pair has a `사이`, with the 사이 label in the middle; **dashed** when only minds exist. Each mind is a small label near its holder's end with an arrow: `경계 →` near 리엔, `← 보호` near 하르트. No line for `만나기 전` or a 호칭-only relation. **Never colour by kind**; kinds are shown by line style and labels. The selected relation's line and labels use `acc` (2.2 px); others `ink3` 1.2 px.
- Filters: `사이 · 마음 · 둘 다` (default `둘 다`); kind filter chips; group chips by tag `전체 · 등불 결사 · 하층 구역` (bottom-left).
- Tap a line → `고른 관계` (inner: glass card bottom-right, 260 px; cover: bottom sheet): title `리엔 · 하르트`, per-point steps `1-7 | 리엔 → 경계 · 하르트 → 보호`, `1-9 | 사이 동료 · 리엔 → 신뢰 · 지금 시점보다 뒤` (cover abbreviates to `· 뒤`; past the 집필 선 it is `예정`), buttons `인물 기록` (ghost) · `관계 고치기` (primary → CST-29). Cover sheet has `닫기`.
- Tap a node → its record [PROPOSED]. Drawing a line between two character nodes opens the line-label sheet (CST-34).
- Deleted characters are absent at every lens; locked hidden characters draw no node and no lines.
- Which nodes appear and node positions: §6 Q14.
- Sources: rule 13 §4 (전체 관계도, 관계도 필터); planning `인물` (전체 관계도); rule 19 §6; rule 20 §2, §7; boards `CastMap`, `CastMapC`.

#### CST-34 · 캔버스 · 관계도 선 → 관계 (lines become relations) — P1
- On the canvas and on the relationship map, lines are plain lines. A line becomes a relation **only** by the `관계로 만들기` button, on every screen size. Picking chips alone records nothing.
- Line-label sheet (cover `CanvasCLabel`; inner popover in `Canvas`): title `선 라벨`, `닫기`; sub `리엔 · 하르트`; label text input (optional; `경계하는 사이`); `.sec` `무엇으로 적을지` chips `사이 · 리엔의 마음 · 하르트의 마음`; `종류` chips `경계 · 신뢰 · 의지 · 동경 · 불신 · + 새 종류`; row `시작 시점 | 1-7`; buttons `그냥 선으로 두기` (ghost) · `관계로 만들기` (primary).
- The `방향 · 한쪽` field is removed. Choosing whose mind sets the direction; 사이 has none. The direction the line was drawn only preselects `<drawn-from>의 마음`.
- A line made into a relation is drawn like the map (solid/dashed, end labels); a plain line stays a thin grey line.
- Only character–character lines can become relations; lines to 세력 · 용어 · 메모 have no `관계로 만들기`.
- Sources: rule 13 §7; boards `Canvas`, `CanvasC`, `CanvasCLabel`.

#### CST-35 · 관계 내보내기 (relation export) — P1
- `.civilight`: per relation the two character keys, the `사이` slot, and each side's mind · description · 호칭, each with its `◆` records.
- Game-engine JSON: option to export 호칭 only, default off. Relations normally do not go to the game.
- Sources: rule 13 §9 (내보내기).

#### CST-36 · 세력 사이 관계 (faction-to-faction relations) — P2
- Storage uses the same shape (사이 + each side's stance) with faction endpoints; screens are decided later. For now the map's faction-region changes cover it. Deleting a faction sends its faction relations to the trash with it (rule 20).
- Sources: rule 13 §8; rule 20 §2 (세력을 지움).

### E. Data for the script editor

#### CST-37 · 화자 고르기 연동 (speaker picker data) — P1
- The picker (script area) gets from cast: search (name, 초성), tag chips (`전체 · 주연 · 등불 결사 · 하층 구역`), chips for fields marked `필터` (e.g. `소속 · 등불 결사`), ordering groups `이 씬 등장` and `최근`, row sub = tags/one-liner or `표시 이름 ???`.
- Excluded: characters in the trash; locked hidden characters (bottom row `숨긴 인물 1 · 지문으로 보기`). When the current block's speaker is a deleted character, the top row is `지운 인물 · 노아 · 되살리기`.
- Sources: `인물 항목` §8 (화자 고르기); rule 19 §6; rule 20 §2 (찾기 · 화자 고르기); board `Picker`.

#### CST-38 · 참고 · 속성 패널 연동 (reference and properties panel data) — P1
- For a selected block the script panel asks cast for the speaker's state **just before that block**, plus that block's own changes as `여기서` rows (rule 11 §2):
  - `말투` one line (`반말 · 짧게 끊음`), the 호칭 toward partners (`리엔을 | ‘꼬마’`), `표시 이름 | ??? (1-9 공개)`, the first two `머리` fields (`화자 · 리엔 · 17세 · 등불 결사`).
  - `관계 · 지금` from the speaker's side: partner · `→ <own mind>` and the partner's mind `← <mind>` (rule 13: on a smaller line below; board: same line, §6 Q18); `사이` as `사이 동료`.
- Example (speaker 하르트): `#042` selected → 리엔: `만나기 전`; `#043` selected → `만나기 전 → 보호 ◆ 여기서` (하르트's mind) and the partner line `만나기 전 → 경계 ◆ 여기서`; `#044` selected → `리엔 · → 보호 · 호칭 ‘꼬마’` and `← 경계`.
- Sources: rule 11 §2; rule 13 §4; `인물 항목` §4 (스크립트 참고 패널: 머리 앞의 2개); boards `CSheetRef`, `WideRef`, `CSheetProps`.

### F. Hiding (rule 19 §6)

#### CST-39 · 인물 숨기기 (hiding a character) — P1
- From the record ⋯ `숨기기` (unhide: `숨김 풀기` [PROPOSED]). The hide applies to the character's record (profile, relations, `◆` records). Unlocking uses the system biometric prompt (fallback device PIN) via the hiding area; it re-locks with the project-hiding re-lock setting.

| Place | Locked | Unlocked |
| --- | --- | --- |
| Character list | one `LockRow`: `숨긴 인물 1 · 지문 · PIN으로 열림 · 지문으로 보기` | in place with a small `숨김` tag |
| Speaker field, `@` links | `숨긴 인물` · `숨긴 항목`; plain text untouched | name |
| Speaker picker | not listed; bottom `숨긴 인물 1 · 지문으로 보기` | listed |
| Search | card results excluded; visible scenes' text still found; footer `숨긴 항목 제외` | all |
| Other characters' relations · 호칭 · 말투 | one row `숨긴 인물과의 관계 1` | as normal |
| Relationship map | no node, no lines; header `12명 중 5명 · 숨김 1` | drawn |
| Lens · `◆` lists | hidden item's records grouped `숨긴 항목 변화 2` | as normal |
| Check center | computed as usual; results grouped `▲ 숨긴 항목 관련 2 · 지문으로 보기` | normal rows |
| 인물 동선 · map | no route for the hidden character | drawn |
| Recorder `연결` | `숨긴 항목` | name |
| Trash | `숨긴 인물 · 3일 남음`; restore needs biometric | name |
| 최근 연 파일 | excluded | excluded |

- Edge cases: a hidden character speaking in a visible scene → check `◇ 숨긴 인물이 보이는 씬에 나옴 · 씬 3` with `씬도 숨기기` · `그대로` (the app never hides the scene itself). Linking a locked hidden character with `@`: not in candidates; only through `숨긴 인물 1 · 지문으로 보기`.
- Export: PDF/CSV/JSON keep the speaker name (`숨긴 인물 1 · 노아` row `화자 이름만 들어감`); JSON includes only `id · 이름 · 표시 이름`. `.civilight` contains everything and turns encryption on by default when any item is hidden.
- The same rules apply to 장소 · 세력 (rule 19 extension, World area; pins and regions are not drawn while locked).
- Rejected: masking the hidden character's name inside free text.
- Sources: rule 19 §5–6; planning `서재 · 숨기기`; memory rules-19-23; boards `ItemHide`, `Trash`.

### G. Deleting, restoring and merging items (rule 20)

#### CST-40 · 지우기 · 영향 시트 (delete with impact sheet) — P0
- Entry: record ⋯ `지우기` (characters); card ⋯ `지우기 · 합치기` (places, terms, factions; World area).
- **If the item has at least one reference or owned record**: one impact sheet (cover: bottom sheet; inner: centred sheet). Counts only; no name typing; nothing to fix first; nothing blocked.
  - Board copy (`RCDeleteImpact`): title `‘노아’ 지우기`, sub `인물 · 참조 20`; `.sec` `같이 휴지통으로` rows `관계 1` (sub `오웬`), `◆ 변화 기록 2` (rule text also lists `게임 텍스트 0`); `.sec` `제자리에 남음 · 지운 인물로 보임` with the dotted sample `노아 ◇` at the right, rows `대사 화자 14` (sub `1-6 씬 2 · 씬 4`), `씬 등장 2`, `@연결 3`, `레코더 칩 1`; row `점검 센터` / `◇ 1줄로 모임 · 한꺼번에 바꾸기`; row `중복이면` / `지우지 않고 합치기` with right `다른 인물과 합치기 ›` (→ CST-45); footnote `휴지통 · 30일 · 되살리면 다시 이어짐`; buttons `취소` (ghost) · `휴지통으로` (primary).
- **If there are no references and no owned records**: no sheet; delete at once and show the toast `인물 노아 지움 · 되돌리기`.
- After deleting: toast `인물 노아 지움 · 참조 20 · 점검 센터 · 되돌리기`. For terms the toast also counts warnings that will be held, e.g. `▲ 공개 전 사용 1`.
- Hidden item: counts only, no locations (no scene names).
- Several items at once: one sheet with totals `인물 3 · 대사 40 · …` (multi-select entry: §6 Q24).
- Deleting a whole project keeps its own confirm (type the name); this rule is for items inside a project.
- Rejected: toast only; typing the name; a red warning-style confirm (deleting is chosen, so `◇`, not `▲`).
- Sources: rule 20 §4; board `RCDeleteImpact`, `UndoToast` (block version only).

#### CST-41 · 지운 항목 참조 (references to a deleted item) — P0
- **Owned** data goes to the trash with the item: its field values, `◆` records, images, game text; its relations (rule 13; meaningless without both ends); a place's pins and nodes; a term's first-reveal record. **Referencing** data (anything that calls the item by key elsewhere) stays in place and shows dotted + `◇`. Plain text is never touched (page memo `노아 시점으로?` stays).
- References inside scenes or blocks that are themselves in the trash are not counted and not checked; they are judged when that scene returns.
- Labels: narrow places (speaker field, `@` chip, 등장 line) = name + dotted underline + `◇`; wide places (panels, cards, check center) = `지운 인물 · 노아` · `지운 장소 · 잿더미 시장` · `지운 용어 · 오리지늄 등불` · `지운 세력 · 상층 경비대`.

| Reference | While deleted | Fix paths | On restore | After permanent delete |
| --- | --- | --- | --- | --- |
| Dialogue speaker | dotted name + `◇`; **not** `▲ 화자 없음`; export keeps the name | `화자 바꾸기` (speaker picker) · `되살리기` · `이대로 두기` | re-linked | `영구 삭제 · 노아` `◇`; export keeps the name |
| Voice ID | unchanged; 자동 keeps computing with 노아's code | changing the speaker: 고정 ID stays + `◇` (rule 12 §6) | no change | the name tag carries the code |
| Scene `등장` (silent) | dotted name in the 등장 line | `빼기` · `되살리기` · `이대로 두기` | re-linked | `영구 삭제 · 노아` |
| `@` link (manuscript, page, recorder, text memo) | dotted chip + `◇`; tap → card `지운 인물 · 노아` | `다른 항목에 잇기` · `글로 풀기` (text stays, link removed) · `되살리기` · `이대로 두기` | re-linked | `영구 삭제 · 노아` chip; `글로 풀기` still possible |
| Relation (rule 13) | goes to the trash with 노아; the partner's list shows a dotted row; gone from the map | `되살리기` · `이대로 두기` (moving to another person = merge) | returns as it was | deleted with it (counted on the sheet) |
| `◆`/`◇` records whose target is the deleted item (노아 소속 `◆` 1-6; term first reveal `◇` #044) | kept with the item; the block shows a dotted `대상 지워짐` | `되살리기` · `이대로 두기` · `기록 지우기` | solid again | deleted |
| Field `연결` values, term `관련` (세력 · 장소 · 용어) | `지운 세력 · 상층 경비대` dotted + `◇` | `다른 항목 고르기` · `비우기` · `되살리기` · `이대로 두기` | re-linked | `영구 삭제 · 상층 경비대` |
| Recorder `연결` chip | dotted chip | `칩 다시 고르기` · `칩 빼기` · `이대로 두기` | re-linked | `영구 삭제 · 노아` chip |

- Scene place, pins, regions, sub-maps, term usage: CST-47.
- Lens: deleted items are absent from lists, map, timeline and relationship map at every lens; dotted references show regardless of the lens.
- Search and picker: deleted items are excluded; the picker's top row exception is in CST-37.
- Script `속성` panel for a block whose speaker is deleted: `지운 인물 · 노아` with `화자 바꾸기 · 되살리기 · 이대로 두기` (rendered by the script area).
- Rejected: deleting references too; forcing fixes before deleting; checking travel with trashed pin coordinates.
- Sources: rule 20 §2; DS README (dotted); boards `Relations` (dotted 노아 row), `RCDeleteImpact`.

#### CST-42 · 점검 센터 · 지운 항목 (check-center rows for deleted items) — P1
- New check filter `지운 항목`. **One row per deleted item** (not per reference): label `◇ 지운 인물 · 노아`, location `휴지통`, body `3일 남음` / `대사 14 · 씬 등장 2 · @ 3 · 레코더 1`, actions `화자 한꺼번에 바꾸기` · `되살리기` · `이대로 두기`.
- With several reference kinds the row has one bulk button per kind (화자 · 장소 · @ · 연결); each kind is changed separately.
- `화자 한꺼번에 바꾸기` → speaker picker (search · tags) → preview `대사 14 · 노아 → 오웬` → `바꾸기`. One-step undo. Changing only some lines (from the expanded list) leaves the remaining count (`대사 14` → `대사 3`).
- Expanding the row lists the lines; each goes to the manuscript.
- `이대로 두기` moves the row to `맞다고 표시한 것`; the dotted marks stay in the manuscript.
- After permanent delete the row reads `영구 삭제 · 노아` and loses `되살리기`.
- Rejected: one row per reference; auto-replacing with a similar character (a guess).
- Sources: rule 20 §3; board `Check`.

#### CST-43 · 되살리기 (restore) — P0
- Restoring brings back all owned data; the item key (ULID) is unchanged, so every reference re-attaches (a fact, not a guess). References the user changed in the meantime stay as changed. The result is reported as counts.
- Toast example: `노아 되살림 · 다시 이어짐 · 대사 11 · @ 3 · 씬 등장 2 · 그대로 둔 것 · 대사 3` (3 lines had been changed to 오웬 and stay 오웬; to undo, pick the speaker again on those lines).

| Case | Behaviour |
| --- | --- |
| A new item with the same name exists | keep both; `◇ 같은 이름 2 · 노아` + `합치기` (CST-46) |
| The map that held its pin was deleted meanwhile | the place returns; its pin shows `◇` `지도 없음`; `지도 되살리기` or place the pin again |
| The partner is also in the trash | the relation returns only when both are back; restoring one shows the partner row dotted |
| A scene with its lines is in the trash (`1-6 씬 5 · 시장의 밤`) | restoring 노아 does not show those lines until the scene returns; restoring the scene re-links them |
| Hidden item | restore needs biometric confirmation |

- Sources: rule 20 §5; rule 12 §2 (keys).

#### CST-44 · 영구 삭제 · 이름표 (permanent delete, name tag) — P1
- After 30 days, or `영구 삭제` in the trash, only a **name tag** remains: name · kind (`인물 · 장소 · 용어 · 세력`) · voice code (the speaker part, e.g. `HRT`). No field values, images, relations or records.
- References show `영구 삭제 · 노아` + `◇`; their fix paths lose `되살리기`. Dialogue never becomes `화자 없음`. Fixed voice IDs (rule 12 §6) are exported unchanged.
- A hidden item's name tag keeps the hidden mark: references show `영구 삭제 · 숨긴 항목`.
- Storage of name tags: §6 Q22.
- Rejected: converting references to `화자 없음` / plain text at permanent delete.
- Sources: rule 20 §5, edge table (숨긴 항목을 지움).

#### CST-45 · 합치기 (merge duplicates) — P1
- Two items of the same kind. Entry: item ⋯ `합치기`; impact sheet `다른 인물과 합치기 ›`; same-name `◇` row `합치기`. Flow: pick the target (same window as the speaker picker for characters) → comparison sheet → `합치기`.
- Comparison sheet (`RCMerge`): title `인물 합치기`, sub `<merged> → <target> · 기본은 <target> 값`; `.sec` `옮길 참조 · 5` / `대사 3 · @연결 1 · 씬 등장 1`; `.sec` `다른 칸 · 3` with column heads `<merged> · 합쳐짐` | `<target> · 남음` and rows of two radio cells (default = target's value): `나이 ?? | 11세`, `소개 …`, `소속 …`; `.sec` `관계 겹침 · 1` rows per slot (`오웬` · note `사이` · `거래 상대 | 보호자`); rows `같은 항목 | 6 · 펼치기`, `빈 칸 채움 | 출생 · 성격 · 2`, `보이스 ID | 고정 2 · 그대로 · ◇`; footnote `<merged> · ‘합침 → <target>’로 휴지통`; buttons `취소` · `합치기`.
- Rules: all references of the merged item move to the target. Default is always the target's value; change per row. Target-empty fields take the merged value without asking and are counted under `빈 칸 채움`. After merging: toast `시장 꼬마 → 노아 합침 · 참조 5 · 되돌리기`; undo restores everything. The merged item goes to the trash as `합침 → 노아`.

| Conflict | Handling |
| --- | --- |
| Both have a relation with the same partner (one per pair) | pick 사이 · each mind · 호칭 per row (default target); `◆` records of both are kept; same slot and same point → a comparison row |
| A relation between the two items | deleted; sheet row `서로 관계 1 · 지워짐` |
| Fixed voice IDs | unchanged; their speaker part now differs → rule 12 §6 `◇` rows; automatic IDs recompute with the target's code |
| Two place pins | the target's pin stays; the merged pin goes to the trash; route ends move to the target pin (count on the sheet) |
| Two term first reveals | comparison row (default target); `▲ 공개 전 사용` recomputed after choosing |
| Restoring the merged item from the trash | it returns with its own original field values as a separate item; moved references, relations and taken values stay with the target; sheet `옮긴 참조 5 · 관계 1 · 칸 3 · 노아에 그대로`; the restored item's relations are empty |

- Rejected: references-only in v1; suggesting merges by similar names (a guess; only exact same names, CST-46); reverting references when restoring the merged item.
- Sources: rule 20 §6; board `RCMerge` (sample differs, §6 Q2).

#### CST-46 · 같은 이름 (same-name notice) — P1
- When two items of the same kind have exactly the same name (after restore, or created), show `◇ 같은 이름 2 · 노아` with `합치기`. Only exact equality (NFC); never similarity.
- Sources: rule 20 §5, §6 (버린 안 ②).

#### CST-47 · 장소 · 용어 · 세력 지우기 (deleting world items) — P1
Same model as characters (CST-40..46). Additional reference rows (rendered by the World area):

| Reference | While deleted | Fix paths | On restore | After permanent delete |
| --- | --- | --- | --- | --- |
| Scene place / region | `지운 장소 · 잿더미 시장` `◇`; excluded from 인물 동선 and travel checks (like rule 18 `◇ 점검 못 함`); never checked with trashed pin coordinates | `장소 고르기` · `되살리기` · `이대로 두기` | re-linked, checks rerun | stays excluded |
| Map pin / node | goes to the trash with the place; routes that touched it stay with a dotted end | `다른 핀에 잇기` · `경로 지우기` · `되살리기` · `이대로 두기` | returns at the same 0–1 coordinates | deleted; route end stays dotted |
| Regions · stamps · background assets linked to a place/faction | shape and image stay on the map; only the link is dotted `◇` (regions belong to the map) | `다른 항목 고르기` · `비우기` · `되살리기` · `이대로 두기` | re-linked | `영구 삭제 · <이름>` |
| Sub-map on a deleted place's pin | the map stays; its breadcrumb shows `지운 장소 · 잿더미 시장` | `다른 핀에 달기` · `되살리기` · `이대로 두기` | re-linked | moves to the top map |
| Term usage · `▲ 공개 전 사용` | text untouched; only linked spans dotted; `▲` is not removed but held inside the check row as `▲ 1 보류` | `글로 풀기` · `되살리기` · `이대로 두기` | `▲` returns to its place | `▲` records deleted too |

- Deleting a faction: characters' `소속` (연결) values follow the table in CST-41; faction-to-faction relations go to the trash with it.
- Sources: rule 20 §2 tables.

#### CST-48 · 휴지통 줄 (trash rows) — P1
- Trash filters (rule 12 §5 changed by rule 20): `전체 · 프로젝트 · 씬 · 블록 · 인물 · 세계 · 변화 · 레코더` (`세계` = 장소 · 용어 · 세력).
- Character row: name (Myeongjo 15 bold), right `<n>일 남음`, sub `인물 · <프로젝트> · 대사 14 · 관계 1`, chips `되살리기 · 영구 삭제`. Hidden: name `숨긴 인물`, sub `인물 · 잿빛 새벽의 등불 · 되살리기는 지문 확인`. Merged: `합침 → 노아` [PROPOSED placement in the sub line].
- Deleted field definitions appear under `인물` (CST-23).
- Sources: rule 20 §5, §7; board `Trash`.

### H. Recorder (rule 23)

#### CST-49 · 라이프 레코더 연결 (recorder links on characters) — P1
- A character is a valid `연결` target. Starting a recording or text memo from a character record pre-fills the `연결` chip with that character.
- `개요` shows `레코더 · 정리 안 됨 <n> ›` (`acc`, right of the first section header) when linked records are still `정리 안 됨`; tap → recorder inbox filtered to this character. Locked hidden records are not counted.
- `카드에 넣기` (from the inbox) can target a character tab `개요 · 말투 · 비밀 · <긴 글 항목>`: the text is copied to the end of that tab/field as a `⌁ 작가 메모` paragraph with a source mark; the record stays in the library.
- `⋯ › 인물로 만들기`: creates a new character (sheet: project · `이름`); `한 줄 소개` = the record's first sentence; a `⌁` memo paragraph with the text at the end of `개요`.
- Sources: rule 23 §3, §4; board `Profile` (`레코더 · 정리 안 됨 1 ›`).

### I. Later

#### CST-50 · 태그별 항목 묶음 (per-tag field sets) — P2
- Rejected for v1 (a 주연 having 20 fields and an NPC 5): changing a tag would make fields appear/disappear. Hiding empty fields gives the same effect; add only "if needed later".
- Sources: `인물 항목` §4 (버린 안).

#### CST-51 · 인물 표 CSV (character table CSV) — P2
- Not in the current export formats; "useful for moving to a setting book or wiki, must decide whether to add". §6 Q6.
- Sources: `인물 항목` §8.

#### CST-52 · 게임 텍스트 보이스 ID (voice IDs for profile voice lines) — P2
- Rule 12 §6 covers script dialogue only; IDs for profile game voice lines are decided later.
- Sources: rule 12 §6 (범위).

---

## 3. Data

All keys are ULIDs made on the device. Everything refers by key, never by name, number or position. Text is UTF-8, Korean NFC-normalised on input. Every persisted entity carries `updatedAt: Instant` and `updatedDevice: String` and an optional `ext: Map<String, Json>` (file format §4, §7). Ordered collections use an `orderKey: String` (fractional index that allows inserting between two items), never array position.

### 3.1 Entities owned by this area

**Character**

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `id` | Ulid | no | permanent key; survives rename, trash, restore |
| `projectId` | Ulid | no | owning project |
| `recordNo` | Int | no | shown as 3 digits `인물 기록 007`; numbering rule §6 Q11 [PROPOSED: next number in creation order, never reused] |
| `name` | String | no | `이름` (fixed); non-empty |
| `displayNameBase` | String | yes | `표시 이름` value before any record; `null` = show `name`. Time-varying via ChangeRecord (target `DISPLAY_NAME`) |
| `oneLiner` | String | yes | `한 줄 소개` |
| `portrait` | ImageSlotRef | yes | 인물 초상: asset or preset, crops `CARD_4_5`, `AVATAR_1_1`, `holo: Boolean` (default false) |
| `background` | ImageSlotRef | yes | 인물 배경: crops `COVER_9_16`, `WIDE_4_3`; no holo |
| `tagIds` | List<Ulid> | no (may be empty) | ordered tags |
| `speechBody` | RichText | yes | `말투` tab prose (runs incl. `@` links and `⌁` memo paragraphs) |
| `secretBody` | RichText | yes | `비밀` tab prose |
| `voiceCode` | String | yes | speaker part of voice IDs (`HRT`); where it is edited: §6 Q12 |
| `hidden` | Boolean | no | item hiding (rule 19) |
| `trash` | TrashState | yes | `{ deletedAt: Instant, reason: DELETED | MERGED, mergedInto: Ulid? }`; null = live |
| `orderKey` | String | no | stable order inside the project (default list order inside a group, §6 Q13) |

**Tag**: `id`, `projectId`, `name: String` (non-empty, unique per project [PROPOSED]), `orderKey`.

**FieldDef** (the engine; one per field, any entity kind)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `id` | Ulid | no | permanent key (values, history, export follow it) |
| `projectId` | Ulid | no | |
| `entityKind` | enum `CHARACTER · PLACE · TERM · FACTION` | no | which settings tab |
| `name` | String | no | display name, e.g. `신장` |
| `exportName` | String | yes | JSON key (`height`); null = `name` |
| `type` | enum `SHORT_TEXT · LONG_TEXT · NUMBER · CHOICE · MULTI_CHOICE · DATE · ANNIVERSARY · LINK · CALC` | no | 9 types (`짧은 글 · 긴 글 · 숫자 · 고르기 · 여러 개 고르기 · 날짜 · 기념일 · 연결 · 계산`) |
| `orderKey` | String | no | order in settings and record header |
| `inHeader` / `inList` / `inFilter` | Boolean ×3 | no | `머리 · 목록 · 필터`; `inFilter` only allowed for CHOICE · MULTI_CHOICE · LINK |
| `isSecret` | Boolean | no | `비밀 항목` (only on the `비밀` tab) |
| `hidden` | Boolean | no | definition hidden (values kept) |
| `scopeCharacterId` | Ulid | yes | set for `이 인물만` fields; null = all characters of the kind |
| `shortTextMultiple` | Boolean | yes | SHORT_TEXT `여러 개` |
| `unit` | String | yes | NUMBER unit text (`cm`); never used for conversion |
| `options` | List<FieldOption> | yes | CHOICE / MULTI_CHOICE |
| `allowFreeText` | Boolean | yes | CHOICE `직접 쓰기` |
| `datePrecision` | enum `YEAR · YMD` | yes | DATE |
| `linkTargetKind` | enum `FACTION · PLACE · TERM` | yes | LINK (never CHARACTER) |
| `calc` | `{ formula: AGE, sourceFieldId: Ulid }` | yes | CALC (`나이` from `출생`) |
| `bundleOrigin` | String | yes | bundle field id this came from (`basic.birth`…), for `항목 더하기` matching (§6 Q21) |
| `trash` | TrashState | yes | deleted field definition (trash filter `인물`) |

**FieldOption**: `id`, `label: String`, `orderKey`.

**FieldValue** (base value, i.e. before any change record; one per entity × field, absent = empty)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `entityId` | Ulid | no | character (or place/term/faction) |
| `fieldDefId` | Ulid | no | |
| `state` | enum `EMPTY · UNKNOWN · VALUE · FALLBACK_TEXT` | no | `UNKNOWN` = `??`; `FALLBACK_TEXT` = kept text after option delete / failed conversion (`◇`) |
| `payload` | sealed: `Texts(List<String>)` · `Rich(RichText)` · `Number(decimal String)` · `Choice(optionIds: List<Ulid>, freeText: String?)` · `Date(year: Int, month: Int?, day: Int?)` · `Anniversary(month: Int, day: Int)` · `Link(targetIds: List<Ulid>)` | yes | by type; null unless `VALUE` |
| `fallbackText` | String | yes | for `FALLBACK_TEXT` |
| `fallbackReason` | enum `OPTION_REMOVED · CONVERSION_FAILED` | yes | drives `◇ 보기에 없는 값` / `◇ 바꾸지 못한 값` |

CALC fields have no FieldValue unless the age is entered directly because the source is empty/unknown (then `payload = Number`).

**Relation**

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `id` | Ulid | no | |
| `projectId` | Ulid | no | |
| `endA`, `endB` | EntityRef `{kind: CHARACTER · FACTION, id}` | no | unordered pair; unique per project (`{A,B}` = `{B,A}`); FACTION ends are storage-only in v1 (CST-36) |
| `between` | SlotValue | yes | base `사이` |
| `mindA`, `mindB` | SlotValue | yes | base minds |
| `addressA`, `addressB` | String | yes | base 호칭 (A → B, B → A) |
| `trash` | TrashState | yes | follows its ends (rule 20) |

`SlotValue = { kindId: Ulid?, description: String?, pairFirstHolder: A | B ? (between + PAIR kind only: which end holds the kind's first name, e.g. 세라 holds 선배), kindFallbackText: String? (kind deleted) }`. Slots for change records: `BETWEEN · MIND_A · MIND_B · ADDRESS_A · ADDRESS_B`. Most real values live in change records (relations usually start from a point/block record); before the first record of any slot the relation is `만나기 전`.

**RelationKind**: `id`, `projectId`, `category: BETWEEN · MIND`, `symmetry: SYMMETRIC · PAIR` (BETWEEN only), `name: String`, `pairName: String?` (PAIR: the other side's name, e.g. `후배` for `선배`), `orderKey`. Usage count (`쓰임 1`) is derived.

**VoiceLine**: `id`, `characterId`, `orderKey` (game order), `title: String` (`임명`), `text: RichText?`, `status: WRITING · DONE` (stored; `비어 있음` is derived from empty text [PROPOSED]).

**ArchiveDoc**: `id`, `characterId`, `orderKey`, `title: String` (`등불지기의 일지`; displayed as `기록 <n> · <title>`, n = position), `body: RichText?`, `unlockPoint: UnitRef?` (episode or chapter key, `1-7 이후 열림`, `2장 이후 열림`).

**ProjectCastSettings**: `bundleName: Map<EntityKind, String>` (last applied bundle, shown `게임 프로필`), `showFirstAppearanceInHeader: Boolean = true`, `showVoiceProgressInHeader: Boolean = true`, `relationExportAddress: Boolean = false` (JSON 호칭 option).

**NameTag** (after permanent delete): `key: Ulid` (the deleted item's key), `kind: CHARACTER · PLACE · TERM · FACTION`, `name: String`, `voiceCode: String?`, `wasHidden: Boolean`, `deletedAt: Instant`.

**MergeRecord**: `mergedId`, `targetId`, `at: Instant`, `movedRefs: List<RefKey>`, `filledFieldIds: List<Ulid>`, `chosenFromMerged: List<{fieldDefId | relationSlot}>`, `deletedMutualRelationId: Ulid?`, `mergedOriginalValues` (snapshot for restoring the merged item, CST-45). Used for one-step undo and for the restore rule.

**RefAck** (`이대로 두기` on deleted-item references): `targetKey: Ulid`, `scope: ITEM | REF(refKey)`, `at: Instant`. Shares storage with check-center `맞음` marks (owned by 점검 센터).

### 3.2 Records owned elsewhere that cast writes

**ChangeRecord** (engine owned by 시점 렌즈 · 변화 기록). Cast needs: `id`, `target` = `Field(entityId, fieldDefId)` | `DisplayName(characterId)` | `RelationSlot(relationId, slot)`, `value` (FieldValue payload / SlotValue / String for 호칭 and 표시 이름), `note: String?` (history note `세라의 안내로 합류`; `처음 만남`), `anchor` = `Block(blockKey)` (from that block, inclusive) | `Point(unitKey)` (from the start of that episode/scene; `1-5 #—`) | `WorldEvent(eventKey)` (before the story) | `SceneEnd(sceneKey)` ([PROPOSED] for `여기서부터 바뀜` at `최신`, CST-10, §6 Q27), `orphaned: Boolean` (`근거 지워짐`), `targetDeleted` (derived: `대상 지워짐`).

**Trash entries**, **check `맞음` marks**, **recorder 연결** and **scene `등장` lists** are owned by their areas; cast reads them for counts.

### 3.3 Derived, never stored

Age (`나이`), `첫 등장`, `보이스 진행`, `아직 등장 전`, display-name ranges (`(1-8까지)`, `1-9 공개`), `예정` / `지금 시점보다 뒤` flags, archive character counts, kind usage counts, reference counts on the impact sheet / trash row / check row, `N명 중 M명`, 호칭 chips on the 말투 tab, the target card's `이 항목으로 잇는 인물`.

### 3.4 What goes into `.civilight`

| File | Cast content |
| --- | --- |
| `project.json` | FieldDefs for all four kinds (incl. options, export names, visibility, secret, hidden, scope), RelationKinds, Tags, ProjectCastSettings |
| `cast.json` | Characters (all fields above), FieldValues of characters, Relations (base slots), VoiceLines, ArchiveDocs |
| `world.json` | FieldValues of places/terms/factions (World area) |
| `changes.json` | ChangeRecords incl. relation slots and display name, with anchors |
| `assets/` | portrait/background originals by content hash; crops as numbers (rect · rotation · flip); presets by name (`preset:ash-dawn`) |
| check marks | `이대로 두기` acks are stored like `맞음` marks |
| tombstones | keys and deletion times of deleted items (30 days) |
| not stored | derived values (3.3); the trash unless `휴지통도 포함` is chosen; app preferences (view toggles) |

- Manifest feature names: custom fields are the optional feature `fields.custom` (file format §3 example).
- Name tags and merge records: the file-format tab does not list them (§6 Q22).
- Hidden characters: everything is included with the hidden mark; a project with hidden items gets encryption on by default (rule 19 §5).

---

## 4. Rules as test cases

Values are from the rule tabs and the sample project. Lens notation: `최신` = up to the 집필 선 (end of 1-7 씬 3). "Derived" marks a value computed from the sources rather than written in them.

### 4.1 Rule 13 · 관계

**TC-13-01 Relation shape (rule 13 §2 table, 최신 · 1-7)**
- Given relation 리엔·하르트 with 사이 empty, 리엔's mind `경계` (description `불빛을 아는 낯선 사람`), 하르트's mind `보호`, 하르트's 호칭 for 리엔 `꼬마`
- When reading it from 리엔's side at `최신 · 1-7`
- Then 사이 = none, own mind `→ 경계`, other mind `← 보호`; summary `→ 경계 · ← 보호`.

**TC-13-02 Pair kind fills the other side**
- Given kind `선배 ↔ 후배` (PAIR) and relation 리엔·세라 with 사이 = 선배↔후배 where 세라 holds `선배`
- When viewing from 리엔 / from 세라
- Then 리엔's row reads `세라 · 선배`; 세라's row reads `리엔 · 후배`. No mind is set on 세라's side (`(적지 않음)`).

**TC-13-03 Symmetric kind, no minds**
- Given 리엔·오웬 사이 = `거래 상대` (SYMMETRIC), both minds empty
- Then 리엔's row reads `오웬 · 거래 상대` with sub `마음 적지 않음`; 오웬's row reads `리엔 · 거래 상대`.

**TC-13-04 Minds are never filled for the other side**
- Given a fresh relation 리엔·하르트
- When the user sets 리엔's mind to `경계`
- Then 하르트's mind stays empty; no `◆` is created for 하르트.

**TC-13-05 Viewer side on both records**
- Given TC-13-01
- Then 리엔's 관계 tab: `하르트 — → 경계 · ← 보호`; 하르트's 관계 tab: `리엔 — → 보호 · ← 경계`.

**TC-13-06 Change over time (rule 13 §6 table)**
- Given records: `1-7 #043` (block) 리엔's mind `◆ 경계`; `1-7 #043` 하르트's mind `◆ 보호` and 하르트's 호칭 `◆ 꼬마`; `1-9` (point, 예정) 사이 `◆ 동료`, 리엔's mind `◆ 신뢰`, 리엔's 호칭 `◆ 하르트`
- When lens = 1-6 → Then the relation reads `만나기 전`, no map line.
- When lens = 1-7 → Then `→ 경계 · ← 보호`, 사이 none, dashed map line.
- When lens = `최신 · 1-7` → Then same as 1-7; the 1-9 values are shown only as `예정` rows (`예정 · 1-9 · 사이 동료 · → 신뢰`), not applied.
- When lens = 1-9 (assume 1-9 written) → Then 사이 `동료` (solid line, middle label `동료`), `→ 신뢰 · ← 보호`, 리엔's 호칭 for 하르트 `하르트`.

**TC-13-07 Per-block panel (rule 11 §2 with rule 13 words)**
- Given 1-7 씬 3 blocks `#042` 리엔, `#043` 하르트 (records as TC-13-06), `#044` narration
- When the script selects #042 → Then 리엔 → 하르트 = `만나기 전`.
- When #043 is selected → Then `만나기 전 → 경계 ◆ 여기서` (리엔's mind) and, with speaker 하르트, `만나기 전 → 보호 ◆ 여기서`.
- When #044 is selected (speaker panel for 하르트) → Then `리엔 · → 보호 · 호칭 ‘꼬마’` and `← 경계`.

**TC-13-08 Two minds in one block → two ◆**
- Given the change sheet on #043 with chips `리엔의 마음` and `하르트의 마음` both on, values `경계` and `보호`
- When `기록`
- Then exactly two ChangeRecords exist, both anchored `Block(#043)`, targets `MIND_리엔` and `MIND_하르트`.

**TC-13-09 호칭 only creates an empty relation**
- Given 세라 and 마르타 without a relation
- When 세라's 호칭 for 마르타 is set to `결사장님` [value derived for the test]
- Then a relation 세라·마르타 exists with 사이/minds empty; the map draws no line between them; 세라's 말투 tab shows chip `마르타 → 결사장님`.

**TC-13-10 One relation per pair**
- Given 리엔·하르트 exists
- When `+ 관계` on 리엔 picks 하르트
- Then no second relation is created; the existing relation's sheet opens.

**TC-13-11 Kind rename**
- Given kind `경계` used by 리엔 → 하르트
- When renamed to `경계심`
- Then every screen shows `→ 경계심`; the ChangeRecord still points to the same kind id.

**TC-13-12 Kind delete keeps text**
- Given 하르트 → 리엔 mind `보호` with no description
- When kind `보호` is deleted
- Then that slot shows description `보호` with `◇`; `kindId` = null, `kindFallbackText` = `보호`.

**TC-13-13 Pair rename updates the written other side**
- Given `선배 ↔ 후배`, 세라 holds 선배 for 리엔
- When the pair's second name is changed to `막내` [value derived for the test]
- Then 세라's view of 리엔 reads `리엔 · 막내`; 리엔's view of 세라 still `세라 · 선배`.

**TC-13-14 Map line style and labels (lens 1-7)**
- Given exactly four relations at lens 1-7: 리엔·하르트 (minds only: 리엔 `경계`, 하르트 `보호`), 리엔·세라 (사이 선배↔후배 + 리엔's mind `의지`), 리엔·오웬 (사이 `거래 상대`, no minds), 세라·하르트 (사이 `동료`, no minds)
- When filter `둘 다` (default) → Then all four lines are drawn; 리엔·하르트 dashed with `경계 →` near 리엔 and `← 보호` near 하르트; 리엔·세라 solid with a middle 사이 label and `의지 →` near 리엔; 리엔·오웬 solid `거래 상대`; 세라·하르트 solid `동료`. All lines share one colour except the selected one (`acc`).
- When filter `사이` → Then drawn: 리엔·세라, 리엔·오웬, 세라·하르트; not drawn: 리엔·하르트.
- When filter `마음` → Then drawn: 리엔·하르트, 리엔·세라; not drawn: 리엔·오웬, 세라·하르트.

**TC-13-17 No relations on rows and cards**
- Given TC-13-14
- Then 리엔's list row and card contain no relation text (`경계`, `선배` …); relations appear only on the 관계 tab, the map and the script panel.

**TC-13-18 호칭 is one value**
- Given 리엔's 호칭 for 하르트 `하르트 씨` (relation slot `ADDRESS_리엔`)
- When the user edits the chip `하르트 → 하르트 씨` on 리엔's 말투 tab to `하르트 님` [value derived for the test]
- Then the relation's 리엔-side 호칭 reads `하르트 님` in the relation sheet and in the script panel; no separate 말투 value exists.

**TC-13-19 무관심 vs 만나기 전**
- Given 리엔·마르타 with no records → Then the pair shows `만나기 전` and no line.
- When 리엔's mind for 마르타 is set to `무관심` at 1-7 [values derived for the test] → Then at lens 1-7 a dashed line with `무관심 →` near 리엔 is drawn; at lens 1-6 it is still `만나기 전` with no line.

**TC-13-15 Canvas line needs the button**
- Given a line drawn from node 리엔 to node 하르트 on the canvas
- When the label sheet opens → Then `무엇으로 적을지` preselects `리엔의 마음`.
- When the user picks `경계` and closes with `그냥 선으로 두기` → Then no relation and no ChangeRecord exist; the line stays thin grey.
- When the user picks `경계`, `시작 시점 1-7` and taps `관계로 만들기` → Then 리엔's mind `경계` is recorded with a point anchor at 1-7 and the line is drawn dashed with end labels.
- Given a line between 리엔 and the faction node 등불 결사 → Then the sheet has no `관계로 만들기`.

**TC-13-16 연결 field cannot target characters**
- When creating a `연결` field → Then the target choices are exactly `세력 · 장소 · 용어`.

**TC-13-20 Switching a kind's category shows the effect first (rule 13 §3 분류 스위치)**
- Given kind `거래 상대` (사이 · 대칭) used by exactly one relation, 리엔·오웬 (TC-13-03)
- When the user switches `거래 상대` to `마음` in `프로젝트 설정 › 관계 종류`
- Then before anything changes a preview lists the affected relation (`리엔 · 오웬`) with the count [PROPOSED copy `바뀌는 관계 1`, CST-32]
- When the preview is cancelled → Then the kind is still `사이 · 대칭` and 리엔·오웬's 사이 slot still reads `거래 상대`.
- The same preview appears when a 사이 kind is switched between `대칭` and `짝 이름`. What applying does to an existing slot value is not stated by rule 13 (§6 Q29).

**TC-13-21 Relation export (rule 13 §9 내보내기, CST-35)**
- Given the relations of TC-13-06 (리엔·하르트 with block and point records)
- When the project is written to `.civilight` and read back
- Then `cast.json › relations` has one entry for the pair with both character keys, the 사이 slot, each side's mind · description · 호칭, and every `◆` record of each slot (in `changes.json`) comes back with the same anchors.
- When 1장 is exported as game-engine JSON with default options → Then no relation data (사이, minds, descriptions, 호칭) is in the file.
- When the option to export 호칭 only is turned on → Then only 호칭 values are added (no 사이 or mind values); the JSON key and placement are not defined yet (LIB-45, §6 Q29).

### 4.2 Rule 11 applied to cast fields

**TC-11-01 Field history at a lens**
- Given 리엔 소속 records `1-1 없음` (note `하층 구역의 고아`), `1-3 등불 결사` (note `세라의 안내로 합류`), `2-5 등불 결사 · 등불지기장`
- When lens = 1-5 → Then the header reads `소속 | 등불 결사 1-3부터 ◆`; the history sheet highlights `1-3` and marks `2-5` `지금 시점보다 뒤` (and `예정`, since 2-5 is past the 집필 선); sub `시점별 값 · 프로필에는 1-5 값`; primary button `1-3 값 고치기`.
- When lens = `최신 · 1-7` → Then the header shows `등불 결사 1-3부터 ◆` and a dimmed row `예정 · 2-5 | 등불지기장`.

**TC-11-02 이 값 고치기**
- Given TC-11-01 at `최신 · 1-7`
- When the user types `등불 결사 · 견습` and taps `이 값 고치기`
- Then the `1-3` record's value becomes `등불 결사 · 견습`; no new record; 1-1 and 2-5 unchanged.

**TC-11-03 여기서부터 바뀜**
- Given TC-11-01 at `최신 · 1-7`
- When `여기서부터 바뀜 · 1-7` with `등불 결사 · 견습`
- Then a new `◆` is created with anchor `SceneEnd(1-7 씬 3)` [PROPOSED, §6 Q27]; at lens 1-5 and at lens `1-7 씬 2` the value is still `등불 결사`; at `최신 · 1-7` it is `등불 결사 · 견습`; the 1-3 record is unchanged; the `2-5` record still follows as `예정`.

**TC-11-04 Past-lens edit**
- Given lens 1-5
- When the user edits 소속 to `등불 결사 · 견습` and chooses `여기서부터 바뀜 · 1-5`
- Then a point record `1-5 #—` is created (previous value shown as `이전 값 ‘등불 결사’`); lens 1-4 still shows `등불 결사`; the banner reads `1-5 시점 값 고치는 중` / `고치면 1-5부터 적용`.

**TC-11-05 Display name by lens**
- Given 하르트 `표시 이름 = ???` with a record at 1-9 clearing it
- Then lens 1-6 / 1-7 / 1-8 → `???`; lens 1-9 → `하르트`; at `최신 · 1-7` → `???` (1-9 is 예정); list note `표시 이름 ??? (1-8까지)`; card subtitle `하르트 · 이름 공개 1-9`; panel `??? (1-9 공개)`.

**TC-11-06 No manuscript**
- Given a `설정만` project with no blocks and a record `1-3 등불 결사`
- Then the lens chip reads `최신 · 전체` and 소속 shows `등불 결사` (all records apply; no `예정`).

**TC-11-07 Planned point becomes applied**
- Given a record at 1-9 (예정) and 1-9 씬 1 empty
- When 1-9 씬 1 gets its first block (the 집필 선 moves to 1-9 씬 1)
- Then the record applies at `최신` without user action.

### 4.3 Custom fields (`인물 항목`)

**TC-F-01 Age in 연도만 mode**
- Given time setting `연도만`, story start 1200, 리엔 출생 = 1183 (derived from 17세 in 1200 and 12세 in 1195)
- When lens = 1-4 (1장, year 1200) → Then 나이 = `17세`.
- When viewing the flashback scene `1-4 씬 2 · 회상 · 1195` → Then 나이 = `12세`; the timeline event `대정전 1195` shows `리엔 12세`.

**TC-F-02 Unknown age**
- Given 하르트 출생 = `??` and 나이 entered as `??`
- Then the header shows `나이 | ??세`; a check for empty fields does not count it.

**TC-F-03 Birth with month/day fills birthday**
- Given time setting `세계 달력` and 출생 = year · month · day
- Then 생일 shows that month · day without the user typing it.

**TC-F-04 Direct age with history**
- Given 출생 empty and 나이 entered `16` at 1-1 with a record `17` at 1-6 [values derived for the test]
- Then lens 1-5 → `16세`, lens 1-6 → `17세`.

**TC-F-05 Short text → number**
- Given 짧은 글 `키` with values `163cm`, `154`, `키 작음` on three characters
- When the type changes to 숫자
- Then values are `163`, `154`, and `키 작음` stays as fallback text with `◇`; the sheet reports `◇ 바꾸지 못한 값 1`.

**TC-F-06 Short text → choice**
- Given 짧은 글 `종족` values `인간`, `인간`, `수인`
- When changed to 고르기
- Then options are `인간 · 수인` (built from values); all three characters keep their value as a selected option.

**TC-F-07 Disallowed conversion**
- Given 긴 글 `과거`
- Then the type list offers no other type for it.

**TC-F-08 Option delete**
- Given 고르기 `성별` with option `여` chosen by 리엔 and 3 others
- When deleting `여`
- Then the confirm lists `리엔 외 3명`; afterwards those four show `여` as fallback text with `◇ 보기에 없는 값`.

**TC-F-09 Option rename**
- When `여` is renamed `여성` → Then all characters with it show `여성`.

**TC-F-10 Unit change never converts**
- Given 신장 154 `cm`
- When the unit becomes `척`
- Then 리엔 shows `154 척`; the sheet note reads `환산하지 않음`.

**TC-F-11 Rename keeps values and export name**
- Given `키` with exportName `height`, values and a `◆`
- When renamed `신장`
- Then values and records are unchanged; JSON still uses `height`.

**TC-F-12 Field delete and restore**
- Given a field with values on 12 characters
- When deleted → Then the confirm reads `값이 있는 인물 12명`; the field is in the trash (`인물`) for 30 days and invisible.
- When restored → Then all 12 values and their `◆` return.

**TC-F-13 Empty fields hidden**
- Given 리엔 with 3 empty project fields
- Then view mode shows the row `빈 항목 3 · 보기`; edit mode shows all fields as inputs.

**TC-F-14 + 항목 from the record**
- When on 리엔 the user adds `좋아하는 것` (짧은 글) with `모든 인물에` → Then it becomes a project field; 하르트 and others are empty.
- When with `이 인물만` → Then only 리엔 has it, with a small mark; `모든 인물에 넣기` later makes it a project field and keeps 리엔's value.

**TC-F-15 Bundle default and 항목 더하기**
- Given a new project → Then the step-4 row reads `인물 항목 | 출생 · 나이 · 생일 · 소속 · 성격 | 기본 ›` and `기본` is selected regardless of project format.
- Given a project on `기본` where 리엔 has values
- When `게임 프로필` is applied via `항목 더하기`
- Then exactly `코드네임 · 성별 · 종족 · 출신지 · 신장 · 특기` are added (11 fields total); no existing field or value changes.

**TC-F-16 Bundle from another project**
- Given project 「유리 정원」 hidden and locked
- Then `다른 프로젝트에서` does not list it; after unlocking it is listed; applying it copies definitions only, no values.

**TC-F-17 List row composition**
- Given 리엔 with list fields 나이 (`17세`) and 소속 (`등불 결사`) and one-liner `막내 등불지기`
- Then the row subtitle is `17세 · 등불 결사 · 막내 등불지기` and the right label `1-2`.

**TC-F-18 Header limit on cover**
- Given 8 fields marked `머리` with values
- Then the cover record shows 6 rows and `항목 더 보기`.

**TC-F-19 Field search**
- Given 특기 (여러 개 고르기) `성직자` on one character
- When searching `성직자` → Then that character appears with the label `특기 · 성직자`.

**TC-F-20 Filter only on allowed types**
- Then `필터` can be checked for 고르기 · 여러 개 고르기 · 연결 and is disabled for the other six types.

**TC-F-21 Hide and unhide a field definition**
- Given 신장 with 리엔 `154 cm`
- When the field is hidden → Then 신장 appears nowhere (header, list, filters, settings rows stay with a hidden mark, export) and the value is kept.
- When unhidden → Then 리엔 shows `154 cm` again.

**TC-F-22 Short text → link**
- Given 짧은 글 `소속` values `등불 결사` (a faction record exists with that exact name) and `결사` (no record named `결사`)
- When changed to 연결 with target kind 세력
- Then the first becomes a link to 등불 결사; `결사` stays as fallback text with `◇`; the sheet reports `◇ 바꾸지 못한 값 1`.

**TC-F-23 Choice → multi-choice**
- Given 고르기 `종족` with `인간` on 리엔
- When changed to 여러 개 고르기 → Then 리엔's value is the set `{인간}`; no `◇`.

**TC-F-24 Typed → short text**
- Given 숫자 `신장` 154 (`cm`), 연결 `소속` → 등불 결사, 날짜 `출생` 1183
- When each is changed to 짧은 글 → Then the texts are the visible strings `154 cm`, `등불 결사`, `1183` [derived: exactly as the value is displayed, unit included]; no `◇`.

**TC-F-25 World-calendar age (만 나이)**
- Given time setting `세계 달력`, 출생 = 1183 · month 5 · day 10 (world month 5) [values derived for the test]
- When the lens scene's 때 is 1200 · month 3 · day 1 → Then 나이 = `16세` (birthday not yet passed).
- When the scene's 때 is 1200 · month 6 · day 1 → Then 나이 = `17세`.
- When the scene has no date (year only) → Then 나이 = `17세` (year subtraction) [PROPOSED fallback].

**TC-F-26 Panel uses the first two 머리 fields**
- Given 리엔's 머리 fields in order 나이 `17세`, 소속 `등불 결사`, 신장 `154 cm`
- When the script panel shows 리엔 as speaker → Then the line reads `화자 · 리엔 · 17세 · 등불 결사`.

### 4.4 Rule 20 · 지웠을 때

**TC-20-01 Impact counts (노아)**
- Given 노아 with 1 relation (오웬), 2 `◆` records, 0 game text, 14 dialogue lines in 1-6 씬 2 and 씬 4, 2 scene appearances, 3 `@` links, 1 recorder chip; and more 노아 lines inside the trashed scene `1-6 씬 5 · 시장의 밤`
- When `지우기`
- Then the sheet shows `같이 휴지통으로: 관계 1 · ◆ 변화 기록 2` and `제자리에 남음: 대사 화자 14 · 씬 등장 2 · @연결 3 · 레코더 칩 1`, sub `인물 · 참조 20` (14 + 2 + 3 + 1); lines in 1-6 씬 5 are not counted.

**TC-20-02 After delete**
- When `휴지통으로`
- Then the toast reads `인물 노아 지움 · 참조 20 · 점검 센터 · 되돌리기`; the list shows `전체 11` without 노아; the map header counts 11; 오웬's 관계 tab ends with the dotted row `노아 ◇ 지운 인물 · 관계 1`; the 14 lines show `노아` dotted with `◇` and are not `▲ 화자 없음`; JSON/CSV export the speaker name `노아`.

**TC-20-03 Zero references**
- Given a character with no references and no owned records
- When `지우기` → Then no sheet; toast `인물 <이름> 지움 · 되돌리기`.

**TC-20-04 Check row and bulk change**
- Given TC-20-02
- Then the check center filter `지운 항목` has one row `◇ 지운 인물 · 노아 · 3일 남음 · 대사 14 · 씬 등장 2 · @ 3 · 레코더 1`.
- When `화자 한꺼번에 바꾸기` → 오웬 → preview `대사 14 · 노아 → 오웬` → `바꾸기` → Then all 14 lines have speaker 오웬; one undo restores them.
- When instead only 11 lines are changed one by one → Then the row reads `대사 3`.

**TC-20-05 Restore keeps user changes**
- Given 3 of the 14 lines were changed to 오웬 while 노아 was in the trash
- When 노아 is restored
- Then 11 lines re-link to 노아, 3 stay 오웬; toast `노아 되살림 · 다시 이어짐 · 대사 11 · @ 3 · 씬 등장 2 · 그대로 둔 것 · 대사 3`; the relation with 오웬 is back.

**TC-20-06 Permanent delete**
- Given 노아 in the trash with voice code `NOA` [derived] and a fixed voice ID on one line
- When `영구 삭제`
- Then a NameTag `{노아, 인물, NOA}` remains; lines show `영구 삭제 · 노아` `◇` (never `화자 없음`); the fixed voice ID is exported unchanged; the relation with 오웬 is gone; the check row reads `영구 삭제 · 노아` without `되살리기`.

**TC-20-07 Same name on restore**
- Given a new character `노아` created while the old 노아 is in the trash
- When the old 노아 is restored
- Then both exist; `◇ 같은 이름 2 · 노아` with `합치기` is shown.

**TC-20-08 Both partners in the trash**
- Given 노아 and 오웬 both deleted
- When only 노아 is restored → Then the relation stays in the trash; 노아's 관계 tab shows 오웬 as a dotted row.
- When 오웬 is restored too → Then the relation returns.

**TC-20-09 Restore before the scene**
- Given 노아 and scene `1-6 씬 5 · 시장의 밤` both in the trash
- When 노아 is restored → Then 1-6 씬 5's lines are still invisible.
- When the scene is restored → Then its 노아 lines re-link to 노아.

**TC-20-10 Hidden item delete**
- Given 노아 hidden (locked)
- When deleted → Then the sheet shows counts only (no `1-6 씬 2 · 씬 4`); the trash row reads `숨긴 인물` with sub `인물 · 잿빛 새벽의 등불 · 되살리기는 지문 확인`; after permanent delete references read `영구 삭제 · 숨긴 항목`.

**TC-20-11 Several at once**
- When three characters are deleted together → Then one sheet with totals (`인물 3 · 대사 40 · …`).

**TC-20-12 Plain text untouched**
- Given a page memo `노아 시점으로?` (plain text, no `@`)
- When 노아 is deleted → Then the memo text is unchanged and not counted.

**TC-20-13 Faction delete**
- Given 상층 경비대 linked as 소속 on two characters
- When deleted → Then both 소속 values show `지운 세력 · 상층 경비대` dotted `◇` with `다른 항목 고르기 · 비우기 · 되살리기 · 이대로 두기`.

**TC-20-14 Place delete**
- Given 잿더미 시장 used as a scene place, with a pin, a route ending on it and a sub-map on the pin
- When deleted → Then the scene shows `지운 장소 · 잿더미 시장` `◇` and leaves travel checks; the pin is in the trash; the route stays with a dotted end; the sub-map stays with breadcrumb `지운 장소 · 잿더미 시장`.
- When restored → Then the pin returns at the same 0–1 coordinates and checks rerun.

**TC-20-15 Term delete holds the warning**
- Given term 오리지늄 등불 with first reveal `◇ #044` and one `▲ 공개 전 사용` at 1-4 씬 2
- When deleted → Then the toast includes `▲ 공개 전 사용 1`; the `◇` record is kept with the term and #044 shows a dotted `대상 지워짐`; the check row holds `▲ 1 보류`.
- When restored → Then the `▲` returns to 1-4 씬 2.

**TC-20-16 Deleted item invisible at any lens**
- Given 노아 in the trash
- Then lens 1-6 and `최신 · 1-7` both omit 노아 from list, map and timeline; dotted references are visible at every lens; the speaker picker lists no 노아 except the top row `지운 인물 · 노아 · 되살리기` when the current block's speaker is 노아.

**TC-20-17 Merge**
- Given `시장 꼬마` (3 lines, 1 `@`, 1 scene appearance; 나이 `??`; relation with 오웬 `거래`; 출생 and 성격 filled) and target `노아` (나이 `11세`; relation with 오웬 `보호`; 출생 and 성격 empty; 2 fixed voice IDs on 시장 꼬마's lines)
- When merging 시장 꼬마 → 노아 with defaults
- Then `옮길 참조 5` move to 노아; 나이 stays `11세`; the 오웬 relation keeps 노아's slot values (both sets of `◆` are kept); 출생 and 성격 are filled from 시장 꼬마 without asking (`빈 칸 채움 · 2`); the 2 fixed voice IDs are unchanged and raise rule 12 `◇`; 시장 꼬마 goes to the trash as `합침 → 노아`; toast `시장 꼬마 → 노아 합침 · 참조 5 · 되돌리기`.
- When `되돌리기` → Then every change above is undone.

**TC-20-18 Mutual relation on merge**
- Given 시장 꼬마 and 노아 have a relation with each other
- When merged → Then that relation is deleted; the sheet shows `서로 관계 1 · 지워짐`.

**TC-20-19 Restore the merged item**
- Given TC-20-17 done
- When 시장 꼬마 is restored from the trash
- Then it returns as a separate character with its original values and no relations; the 5 references, the relation and the taken values stay with 노아; the sheet reads `옮긴 참조 5 · 관계 1 · 칸 3 · 노아에 그대로`.

**TC-20-20 Trash filter**
- Then the trash filters are `전체 · 프로젝트 · 씬 · 블록 · 인물 · 세계 · 변화 · 레코더`; a deleted place appears under `세계`.

**TC-20-21 Automatic voice ID while deleted**
- Given a 노아 line in 1-6 with an automatic voice ID and 노아's voice code `NOA` [derived]
- When 노아 is in the trash → Then the ID keeps being computed with `NOA` (e.g. `NOA_0106_<번호>`); it is not cleared and does not change to another code.

**TC-20-22 글로 풀기**
- Given a page with an `@노아` link and 노아 in the trash
- When the user taps the dotted chip and picks `글로 풀기` → Then the page shows plain text `노아`, the link is removed, the item's `@` count drops by 1.
- When 노아 is restored → Then that text stays plain (user choice kept).

**TC-20-23 Record whose target is deleted**
- Given 노아's 소속 `◆` anchored at a 1-6 block
- When 노아 is deleted → Then the record is kept with 노아 (in the trash), the 1-6 block shows a dotted `◆` with `대상 지워짐` and buttons `되살리기 · 이대로 두기 · 기록 지우기`.
- When 노아 is restored → Then the `◆` is solid again.
- When instead 노아 is permanently deleted → Then the record is deleted with it.

**TC-20-24 Restore a place whose map is gone**
- Given 잿더미 시장 (with a pin on map 「하층 구역 지도」) deleted, then the map deleted
- When 잿더미 시장 is restored → Then the place returns; its pin shows `◇` `지도 없음` with `지도 되살리기` and a way to place the pin again.

**TC-20-25 Sub-map after permanent delete**
- Given a sub-map attached to 잿더미 시장's pin
- When 잿더미 시장 is permanently deleted → Then the sub-map remains and moves under the top map.

**TC-20-26 Merge two places with pins**
- Given places A (target) and B each with a pin, and two routes ending on B's pin
- When B is merged into A → Then A's pin stays, B's pin goes to the trash, both route ends move to A's pin; the sheet shows the moved route-end count `2`.

**TC-20-27 Merge two terms with first reveals**
- Given terms A (target, first reveal 1-7 #044) and B (first reveal in 1-3 [derived]), with a linked use of B in 1-4 씬 2
- When merged with the default → Then A's first reveal `1-7 #044` is kept; `▲ 공개 전 사용` is recomputed for all of B's former uses, so the use in 1-4 씬 2 becomes `▲`.
- When the comparison row is switched to B's value → Then the first reveal is B's 1-3 record and that `▲` is not raised.

**TC-20-28 이대로 두기**
- Given TC-20-02
- When `이대로 두기` on the check row → Then the row moves to `맞다고 표시한 것`; the 14 lines still show the dotted `노아 ◇`; the ack is stored and survives a `.civilight` round trip.

### 4.5 Rule 19 · 인물 숨기기

**TC-19-01 List and speaker field**
- Given 노아 hidden and locked (12 characters)
- Then the list shows `전체 11` and `숨긴 인물 1 · 지문 · PIN으로 열림 · 지문으로 보기`; 1-6 씬 5 lines show speaker `숨긴 인물` with the dialogue text unchanged; narration `노아가 웃었다` stays as written.

**TC-19-02 Map and relations**
- Then the map header reads `12명 중 5명 · 숨김 1` and draws no 노아 node or line; 오웬's 관계 tab shows `숨긴 인물과의 관계 1`.

**TC-19-03 Picker and @**
- Then the speaker picker does not list 노아 and ends with `숨긴 인물 1 · 지문으로 보기`; typing `@노` offers no candidate.

**TC-19-04 Export keeps the speaker**
- When exporting 1장 as JSON with 노아 hidden
- Then 1-6 씬 5's 노아 lines keep 노아's speaker id and `노아`; `characters[]` has only id · name · display name for 노아; the export sheet row reads `숨긴 인물 1 · 노아 · 화자 이름만 들어감`.

**TC-19-05 Hidden character in a visible scene**
- Given 노아 hidden and speaking in visible 씬 3
- Then the check center shows `◇ 숨긴 인물이 보이는 씬에 나옴 · 씬 3` with `씬도 숨기기 · 그대로`; the scene is not hidden automatically.

**TC-19-06 Unlocked**
- When unlocked by biometric → Then 노아 is back in place with a small `숨김` tag; re-lock follows the project re-lock setting.

**TC-19-07 Search while locked**
- Given 노아 hidden and locked, 노아 speaking in visible scene 1-6 씬 2
- When searching `노아` → Then no character card result; the dialogue lines in visible scenes are still found; the list ends with `숨긴 항목 제외`.

**TC-19-08 Change lists**
- Given 노아 with 2 `◆` records, hidden and locked
- Then lists of change records (lens views, scene time panel) show one row `숨긴 항목 변화 2` instead of the two records.

**TC-19-09 Check results**
- Given two check results involving 노아 (e.g. two `▲` rows) while locked
- Then the check center shows `▲ 숨긴 항목 관련 2 · 지문으로 보기`; after unlocking, the two original rows appear.

---

## 5. Screens

| # | Screen / sheet / state | Boards | Cover (390) | Inner (800) | Components |
| --- | --- | --- | --- | --- | --- |
| S1 | 인물 목록 | `Cast`, `ItemHide`, `MFoldList` | full screen list, glass head + `CommandBar` | A: list 390 \| record 409 (`TwoCast`); B: 560 column (`ColCast`) | `GlassBars`, `LensChip`, `ViewToggle`, segmented control, `Tag` chips, `ListRow .li`, `LockRow`, `CommandBar` |
| S2 | 인물 없음 (empty) | `EmptyCast` | full | same in the list pane | `Tag`, `Buttons` |
| S3 | 카드 보기 | `CastCards`, `MCardView`, `MCarousel` | horizontal rows per tag | same, wider rows [PROPOSED] | `ViewToggle`, `ImageTile`/portrait cards |
| S4 | 관계도 | `CastMapC`, `CastMap` | graph + bottom sheet `고른 관계` | graph + glass aside 260 px bottom-right, chips bottom-left | canvas engine, `Sheet`, `Tag`, `Buttons`, `LensChip` |
| S5 | 인물 기록 · 개요 | `Profile`, `TourCast`, `RCProfileAdd`, `RTEditTwo`, `RTPlanned` | background art 9:16 top, document body, tabs above command bar | right pane 409 (A) / 560 column (B), art 4:3 | `GlassBars`, `LensChip`, `ListRow .row`, `Tabs`, `CommandBar` |
| S6 | 인물 기록 · 말투 · 관계 | `Relations` | same frame | same | `Tag` (호칭 chips), avatars, history steps, dotted row |
| S7 | 인물 기록 · 게임 텍스트 | `Voice` | same frame | same | progress bar, `ListRow` |
| S8 | 인물 기록 · 비밀 | none drawn | same frame | same | — |
| S9 | 항목 변화 기록 sheet | `FieldHistory` | bottom sheet over record | side popover/sheet [PROPOSED] | `Sheet`, `Buttons` |
| S10 | 값 고치기 sheet (two buttons) | `RTEditTwo` | bottom sheet | side sheet | `Sheet`, input, `Buttons` (two stacked with sub-labels) |
| S11 | 과거 시점에서 고치기 | `LensEdit` | banner under head | same | glass banner, `LensChip` (acc) |
| S12 | + 항목 (record) | `RCProfileAdd` | bottom sheet | popover | `Sheet`, radio cards (`Controls`) |
| S13 | 프로젝트 설정 › 인물 항목 | `RCFields`, `ProjSettings` | full screen with kind tabs | list + detail of a field [PROPOSED] | `Tabs`, grip rows, `ListRow` |
| S14 | + 항목 (settings) | `RCFieldAdd` | bottom sheet | popover | radio grid, checkboxes, switch |
| S15 | 프로젝트 설정 › 관계 종류 | `RCRelTypes`, `ProjSettings` | full screen | same | `Tag` chips, pair rows |
| S16 | 관계 만들기 · 고치기 sheet | none drawn (copy from `ChangeCreate`, `CanvasCLabel`) | bottom sheet | popover | chips, inputs |
| S17 | ◆ 변화 기록 · 관계 | `ChangeCreate` | bottom sheet over script | side panel | segmented, chips, `Buttons` |
| S18 | 선 라벨 sheet | `CanvasCLabel`, `Canvas` | bottom sheet | popover 300 px | chips, `Buttons` |
| S19 | 참고 · 속성 panel (cast data) | `CSheetRef`, `WideRef`, `CSheetProps` | bottom sheet | side panel 290 px | `ListRow .row` |
| S20 | 화자 고르기 (cast data) | `Picker` | bottom sheet | popover | search, `Tabs` chips, rows |
| S21 | 지우기 · 영향 sheet | `RCDeleteImpact` | bottom sheet | centred sheet | `Sheet`, count rows, `Buttons` |
| S22 | 합치기 · 비교 sheet | `RCMerge` | bottom sheet | centred sheet [PROPOSED] | radio cells, rows, `Buttons` |
| S23 | 되돌리기 알림 (item) | `UndoToast` (block version only) | toast above command bar | same | `Toast` |
| S24 | 휴지통 rows | `Trash` | list | list + detail | `Tag` chips, rows |
| S25 | 점검 센터 · 지운 항목 / 보이스 rows | `Check` | list | list + manuscript (`TwoCheck`) | rows with actions |
| S26 | 새 프로젝트 4단계 · 인물 항목 row | `NewStart` | row in step 4 | new-project area | `ListRow` |
| S27 | 이미지 slots | `ImageSlots`, `ImagePick`, `ImageCrop` | image area | image area | `ImageTile`, `CropFrame` |
| S28 | 시점 렌즈 sheet | `Lens` | lens area | lens area | `LensChip`, `Sheet` |

**Required but not drawn** (build from this spec; layout open): S8 비밀 tab; S16 relation create/edit sheet; record ⋯ menu (`숨기기 · 지우기 · 합치기`); character create sheet; tag editing; field filter chips and sort control (CST-03); bundle picker sheet (`기본 · 게임 프로필 · 비어 있음 · 다른 프로젝트에서`); field edit sheet for existing fields (option editor, type change preview, hide, delete confirm `값이 있는 인물 12명`); kind edit / switch preview; multi-select delete; locked item-hide states for the speaker field, relationship map and other characters' relation rows; item toast `인물 노아 지움 · 참조 20 · 점검 센터 · 되돌리기`; restore report toast; same-name `◇` row; archive document editor; voice line state control; `고른 관계` sheet on inner when opened from a record.

---

## 6. Open questions

Only real contradictions or gaps between sources. Each lists both sides and the default this spec uses until decided.

1. **Default relation kinds.** Rule 13 §3: 대칭 `동료 · 형제자매 · 배우자 · 연인 · 친구 · 거래 상대 · 적` (7), 짝 `부모 ↔ 자식 · 스승 ↔ 제자 · 상관 ↔ 부하 · 선배 ↔ 후배` (4), 마음 `신뢰 · 의지 · 동경 · 호감 · 보호 · 경계 · 불신 · 질투 · 증오 · 죄책감 · 무관심` (11). Board `RCRelTypes` and `ProjSettings` (`사이 12 · 마음 11`): 대칭 `동료 · 형제자매 · 친구 · 거래 상대 · 연인 · 소꿉친구`, 짝 adds `보호자 ↔ 피보호자 · 주인 ↔ 하인`, 마음 has `존경 · 연모` instead of `호감 · 불신`. `ChangeCreate`/`CanvasCLabel` still offer `불신`. Default here: the rule 13 list.
2. **Merge sample.** Rule 20 §6: `시장 꼬마` → 노아, overlap `오웬 거래 | 보호`, `빈 칸 채움 · 출생 · 성격 · 2칸`. Board `RCMerge`: `노아(시장)`, overlap row `오웬 · 사이 · 거래 상대 | 보호자`, `다른 칸 · 3` (나이 · 소개 · 소속), `같은 항목 6`. Behaviour is the same; sample data only (user said sample issues can wait).
3. **Editing at a past lens.** Rule 11 §1: the same two buttons (`이 값 고치기 · 여기서부터 바뀜`) at a past lens; the banner `고치면 1-5부터 적용` covers only the second meaning. Board `LensEdit` still shows only the banner and `저장 시 ◆ 변화 기록 · 1-5 #—`; `FieldHistory` buttons are `시점 추가 · 1-3 값 고치기`. Default: rule 11 (two buttons everywhere), keep the banner.
4. **Display-name model.** Character-level `표시 이름` with history (rule 12 §1, rule 11 table) vs the script `속성` panel's per-block `표시 이름` field (`??? · 1-9에서 공개`). Is there a per-block override independent of the character history? Also the list shows the real name as title with a note while the card view shows `???` as title. Default: one character-level history; the 속성 panel shows it read-only with a link to the record; list/card as drawn.
5. **보이스 진행.** Header `18 / 24` and check `보이스 6/24 비어 있음` imply filled = not empty (쓰는 중 counts); "진행" could mean `녹음 대본 완료` only. Default: not-empty count.
6. **인물 표 CSV.** `인물 항목` §8 leaves it undecided. Default: not built (CST-51, P2).
7. **Auto-filled 생일.** "출생 날짜에 월 · 일까지 있으면 생일은 자동으로 채워진다": stored copy (editable, can diverge) or derived (read-only while 출생 has m/d)? Default: derived and read-only while 출생 has month and day.
8. **Tabs vs one document.** DS/planning: five tabs. Board `Relations` (tab `관계` selected) shows `Ⅱ 말투` and `Ⅲ 관계` numbered sections on one scroll, while `인물 항목` §4 numbers 긴 글 sections in 개요 as `Ⅰ 개요 · Ⅱ 성격 · Ⅲ 과거`. Default: separate tab pages; Roman numerals only for 개요's long-text sections.
9. **말투 structure.** Planning: `1인칭 · 어미 · 말버릇` (could be three fields); board: one prose paragraph; `참고` panel shows a one-line `말투 | 반말 · 짧게 끊음` with no stated source. Default: prose body + a separate optional one-line summary used by the panel [needs a decision and a board].
10. **비밀 tab hiding.** Planning calls 비밀 "항목 숨기기 대상" and rule 19 §5 says hiding also covers unpublished settings, but hiding is defined per item (character), not per tab. Can the 비밀 tab be locked on its own? Default: no separate lock; hide the character.
11. **Record number.** `인물 기록 007 / 012 / 014` is shown everywhere but no rule says how it is assigned (creation order? reused? renumbered?). Default: creation order per project, never reused, never renumbered (like block numbers, rule 12 §5).
12. **Voice code per character.** Voice ID format `HRT_0107_043` needs a speaker code and the name tag stores it, but no source says where a character's code is set or how it is generated. Default: a `보이스 코드` row in the record's `속성`-like area [needs a board]; auto-suggest is a guess and is not used.
13. **List order, multi-tag grouping, filter/sort UI.** Grouping is by tag, but a character can have several tags (리엔 is 주연 and in 등불 결사; the boards list her only under 주연; 세라 appears under 등불 결사 in the list and under 하층 구역 in cards). Default order inside a group is not stated; field filter/sort controls are not drawn. Default: a character appears under its first tag; order = `orderKey` (creation order); sort/filter controls in a header `⋯` sheet [PROPOSED].
14. **Relationship map nodes and layout.** Which characters appear (`11명 중 5명` on inner, `11명 중 4명` on cover) and how M is counted is not defined; node positions are not defined (auto layout? saved like canvas nodes?). Default: nodes = characters with at least one drawn line at the lens after filters; positions saved per project like canvas nodes, auto-placed on first view.
15. **Middle label for pair kinds on the map.** An undirected line with 사이 `선배 ↔ 후배` is labelled `선배` (리엔–세라) while 하르트–마르타 is labelled `부하`; which side's name the middle label shows is not defined. Default: `선배 ↔ 후배` with the role names placed toward their holders.
16. **Deleting a kind when a description exists.** Rule 13 §3: "지우면 그 종류를 고른 관계는 설명으로 남고 ◇". If the slot already has a description (리엔 → 하르트 `경계 · ‘불빛을 아는 낯선 사람’`), how are the two texts combined? Default: keep `kindFallbackText` separately and show `경계 · 불빛을 아는 낯선 사람 ◇`.
17. **‘꼬마’: description or 호칭.** Rule 13 §2 example table puts `보호 · ‘꼬마’` in the mind column (like a description); §4, §6, memory and `CSheetRef` (`리엔을 | ‘꼬마’`) treat `꼬마` as 하르트's 호칭 for 리엔. Default: 호칭.
18. **Reference panel layout and block basis.** Rule 13 §4: the partner's mind goes on a smaller line below (`← 경계`); boards `CSheetRef`/`WideRef` put `→ 보호 · ← 경계` on one line. The same boards select #043 yet show the post-#043 state, while rule 11 §2 says #043 shows `만나기 전 → … ◆ 여기서`. Default: rule text (script area renders).
19. **`예정` vs `지금 시점보다 뒤`.** Older boards (`Relations`, `FieldHistory`, `CastMap`, `CastMapC`) label 1-9 / 2-5 records at lens 1-7 or 1-5 as `지금 시점보다 뒤`; rule 11 and `RTPlanned` call anything past the 집필 선 `예정`. `RTPlanned` also shows `예정 · 1-9 · 하르트 → 신뢰` in 리엔's record where rule 13 has 리엔 → 신뢰 (should read `→ 신뢰`). Default: `예정` past the 집필 선; `지금 시점보다 뒤` for written records after a past lens.
20. **Year at a lens in `연도만` mode.** Age = lens year − birth year, but how a scene's 때 (days since story start, possibly ranges) maps to a year when only years exist (365-day years? year changes only by explicit events?) is not defined. Default: story start year + floor(days ÷ 365); a range uses its earliest day.
21. **`항목 더하기` matching.** "없는 항목만 더한다": matched by field name, or by the bundle field's identity (a renamed `신장` → `키` would be re-added under name matching)? Default: by `bundleOrigin` when present, else by name.
22. **Name tags, merge records and trashed items in `.civilight`.** Rule 20 defers name-tag and merge-record storage to `저장 파일 양식`, which does not list them. Also the file excludes the trash by default, so an exported project with 노아 in the trash has dotted references to a key that is not in the file. Default: always write name tags for trashed and permanently deleted items referenced by live data, plus merge records, in `cast.json` (optional feature `cast.tombstones` [PROPOSED]).
23. **Card view vs `목록 | 관계도`.** `Cast` has the segmented `목록 | 관계도`; `CastCards` has the header `목록 | 카드` toggle and no segmented control. How the two controls combine (is the map reachable from card view?) is not drawn. Default: both controls on both views; `관계도` overrides the list/card choice while selected.
24. **Entry points not drawn.** The record ⋯ menu (숨기기 · 지우기 · 합치기) and the multi-select needed for "여러 개를 한꺼번에 지움" have no board; long-press is not allowed (no hidden gestures). Default: record ⋯ menu; a visible `고르기` action in the list header ⋯ for multi-select [PROPOSED].
25. **Header table vs `머리` flag.** `RCFields` marks 종족 `목록 · 필터` (not `머리`), but `RCProfileAdd` and `인물 항목` §9 show 종족 in the record's header table; the `항목` list for fields without `머리` (§4 "개요 탭의 ‘항목’ 목록") is not drawn. Default: header = `머리` fields only; others in the `항목` list.
26. **`연결` overloaded.** Rule 23 renamed the recorder's `붙일 곳` to `연결`, which is also the field type name `연결`. Vocabulary memo lists this as open. Default: keep both as specified.
27. **Where `여기서부터 바뀜` at 최신 puts the record.** Rule 11 §1: "집필 선 위치(1-7 씬 3)에 ◆". Board `RTEditTwo` and the confirmed 2026-11-22 note: "집필 선 1-7 씬 3 뒤" / "right after the 집필 선". Rule 12 §1's anchor table lists only the past-lens case as a `시점` anchor, which applies from the *start* of the unit (that would back-date the change to the start of 1-7 씬 3), and anything *after* the 집필 선 is `예정` (would not show at 최신). Default [PROPOSED]: new anchor `SceneEnd(1-7 씬 3)`, applied at 최신, not before the end of that scene (CST-10, TC-11-03). Needs the lens/change-record area to adopt it.
28. **`▲` from profile game text.** Rule 21 §4 table: profile voice lines and archive documents go to the game but whether their `@` links raise `▲ 공개 전 사용` is "미정, 위치 없음" (no story position). Default: no `▲`, links listed under `원고 밖` with tag `게임 텍스트`; an archive document's `… 이후 열림` point could serve as its position if decided later.
29. **Kind switch and 호칭-only export are under-specified.** Rule 13 §3 says the `사이 / 마음` and `대칭 / 짝 이름` switches may change while in use and show their effect first, but not what happens to slots already holding that kind (a 사이 value has no side; a mind has one). Rule 13 §9 offers "호칭만 내보낼지 선택 (기본 끔)" for game-engine JSON, but LIB-45's JSON shape has no key for it. Default until decided: the preview is shown and nothing is applied without confirmation (TC-13-20); the JSON option is listed but its shape is left to LIB-45 (TC-13-21).
