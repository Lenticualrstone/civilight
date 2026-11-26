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
- Sample project: 「잿빛 새벽의 등불」 (R-15; the rating is stored but no longer shown from 0.2.0, S-4). Characters 리엔 (인물 기록 007), 하르트 (shown as `???` until 1-9), 세라, 오웬, 마르타, 노아 (인물 기록 012; in the trash in the rule-20 examples), 시장 꼬마 (rule 20 merge example; drawn as `노아(시장)`, 인물 기록 014). Tags 주연 · 등불 결사 · 하층 구역 · 상층. 집필 선 (writing line) = end of 1-7 씬 3. Lens chip reads `최신 · 1-7`.

**0.2.0 (frozen scope `docs/status/0.2.0-scope.md`, 2026-11-24).** Sections that change for app version 0.2.0 carry `(0.2.0)` in their heading or on the changed line. Existing IDs keep their numbers; new features are CST-66 … CST-76, placed at the end of the section they belong to. Old behaviour that 0.2.0 replaces is kept in the text and marked `Superseded (0.2.0):`, so the 0.1.0 meaning stays readable.
- What 0.2.0 changes here:
  - create in place, U-15: CST-66 (characters), CST-75 (tags), CST-76 (relation kinds);
  - 3D card views, U-16: CST-67;
  - several tags per character and tag delete, N-44: CST-68, CST-75;
  - `오래 안 나옴` sort and appearance bars, N-47: CST-69, CST-71;
  - lens chip default `최신`, S-1: CST-70;
  - `말투` one-line summary and secret-tab lock, N-43: CST-72, CST-73;
  - what the `게임 제작 도구` switch hides, G-2: CST-74;
  - the U-09 templates reconciled with 0.2.0: §7.9.
- Test cases are in §4.6, screens S39 … S48, and copy and build plan in §8.
- **Data rule.** Every 0.2.0 data change is additive: new optional fields with defaults, nothing removed or renamed. Hiding is never deleting (scope §8).
- **Notation.** In this spec `＋` before a label stands for the icon `plus` (DS-72 create row, ICON class b). The visible text is the label alone: `새 인물`, or in a picker with an unmatched query `‘세라’ 새 인물`, with quote marks in the app's `‘…’` style (DS-72). Tests assert the label text.
- **Copy rule.** Copy in `code` comes from a planning source or a board. Copy that no source gives is named as a `TODO_COPY_CAST_<NAME>` constant and is listed in §8.1; it is never invented here.

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

#### CST-01 · 인물 목록 (character list) — P0 (0.2.0)
- **Header (glass top bar).** Small line: project name (`잿빛 새벽의 등불`). Title: `인물`. Right: `LensChip` (`최신 · 1-7`; `acc` outline when not 최신), `ViewToggle` `목록 | 카드` (CST-04), `+` icon button (new character, CST-05).
  - **(0.2.0)** Right side of the head: the small `LensChip` (CST-70) and the U-16 view toggle (CST-67). The `+` icon button is removed. Creating moves to the top row `＋ 새 인물` (CST-66).
- **View mode control.** First content row: segmented control `목록 | 관계도` (selected = `pri` fill). `관계도` switches the screen to the relationship map (CST-33) without leaving the cast list. **(0.2.0)** Each segment is an icon plus its text (`list` · `graph`, icon list class b). It stays above every U-16 view, and `관계도` overrides the list or 3D choice while it is selected (§6 Q23 default kept).
- **Filter chips.** `전체 N` + one chip per project tag in tag order (`주연 · 등불 결사 · 하층 구역 · 상층`), single-select, `전체` default (CST-02). N counts characters that are not in the trash and not locked-hidden (`전체 11` both when 노아 is in the trash and when 노아 is hidden). **(0.2.0)** N counts each character once, even when it is listed under several tags (CST-68). Trashed tags have no chip (CST-75).
- **Grouped rows.** Section header per tag group (`.sec` style: `주연`, `등불 결사`, `하층 구역`). Row (`ListRow .li`): 40 px avatar (portrait 1:1 crop, else the name's first letter), name (Myeongjo 16 bold), one-line subtitle (small), right: first appearance episode (mono small, CST-14). **(0.2.0)** A character appears in the section of **each** of its live tags (CST-68), not only under its first tag. The first row of the list is `＋ 새 인물` (CST-66).
- **Subtitle composition.** `list-visible field values (max 2, in field order, empty skipped)` + `한 줄 소개` + display-name note, joined by ` · `. Examples at `최신 · 1-7`: 리엔 `17세 · 등불 결사 · 막내 등불지기` / `1-2`; 하르트 `결사의 검사 · 표시 이름 ??? (1-8까지)` / `1-7`; 세라 `기록관 · 리엔의 선배` / `1-1`; 마르타 `결사장 · 아직 등장 전` / `2-1`; 오웬 `밀수상 · 잿더미 시장` / `1-6`. Unknown values print as `??` with their unit (`??세`). `아직 등장 전` is appended when the first appearance is after the lens point (board-derived).
- **Tap a row** → character record (cover: full screen; inner list+detail: right pane, CST-06).
- **Command bar** hint: `이름 · 초성 · 태그로 찾기` (CST-02). Mic on the right opens the recorder. **Superseded (0.2.0):** no mic (S-8: the recorder opens only from home). The frame draws one command bar at most (U-01, DS-65); the hint is unchanged.
- **Values follow the lens** (ages, 소속 …). Characters in the trash never appear. Locked hidden characters collapse into one `LockRow` (CST-39).
- **Empty state** (`EmptyCast`): chip `전체 0`, title `인물 없음`, sub `이름만 적어도 시작 · 나머지는 나중에`, buttons `인물 추가` (primary → CST-05) and `라이프 레코더에서` (opens the recorder inbox filtered to this project and `정리 안 됨`, where each record has `⋯ › 인물로 만들기`, CST-49).
  - **(0.2.0)** The `＋ 새 인물` row stays on top with 0 items (U-15.1). `인물 추가` opens the same create card (CST-66), not a sheet.
  - `라이프 레코더에서` stays: it opens 기록함 and records nothing. S-8 removes only the mic entry points.
  - In a read-only project (the example) both the `＋ 새 인물` row and `인물 추가` are hidden (U-15.10).
- Sources: planning `인물` (인물 목록); `인물 항목` §4; rule 19 §6; rule 20 §2, §7; boards `Cast`, `ItemHide`, `EmptyCast`, `TwoCast`, `ColCast`, `MFoldList`.

#### CST-02 · 태그 필터 · 찾기 (tag filter and search) — P0
- Tapping a tag chip shows only that tag's characters (other sections hidden); `전체` shows all. One chip active at a time.
- Typing in the command bar on this screen filters the list live by name, 초성 (Korean initial consonants: `ㄹㅇ` matches 리엔) and tag name. Matching is NFC-normalised. Characters in the trash and locked-hidden characters never match.
- No results: use the shared empty-search state from 명령 칸 · 검색 (title `결과 없음`).
- **(0.2.0)** While a query is typed, the `＋ 새 인물` row stays on top. It does not take the query (only picker sheets prefill, U-15.3). A character with several tags still shows once per matching tag section (CST-68).
- Sources: planning `인물` (태그 필터, 이름 · 초성 · 태그 검색); boards `Cast`, `Picker` (`ㄹㅇ` · `초성 검색`).

#### CST-03 · 항목 필터 · 정렬 (filter and sort by custom fields) — P1
- Fields whose `필터` box is on (only types `고르기 · 여러 개 고르기 · 연결`) add filter chips to the list (example: `성별 · 종족 · 소속`). Picking a chip opens its options (or linked records) and filters to characters whose value matches. `숫자` fields allow a range filter.
- Sortable types `숫자 · 날짜 · 기념일 · 계산` give sort orders named `<항목 이름>순` (`신장순 · 나이순 · 생일순`). `기념일` sorts by month order. `??` and empty values sort last [PROPOSED].
- `짧은 글 · 긴 글` are search-only (CST-27).
- No board draws these controls ("Not drawn yet: list filter/sort by fields"); placement is an open question (§6 Q13).
- **(0.2.0)** The sort list also has `오래 안 나옴` (CST-69).
- Sources: `인물 항목` §2 table, §4 table; memory character-fields.

#### CST-04 · 카드 보기 (card view) — P1 (0.2.0: superseded by CST-67)
**Superseded (0.2.0):** the `목록 | 카드` toggle and the per-tag horizontal card rows below are replaced by the U-16 view options (CST-67). In 0.2.0 the character list has no 2D card-row view. The card content rules (title = display name at the lens, `???` with `하르트 · 이름 공개 1-9`, `홀로` badge) carry over to the CST-67 cards. The 0.1.0 text follows for reference.
- `ViewToggle` `목록 | 카드` in the header; the chosen view is remembered per screen (local preference, not in `.civilight`).
- Card view = one horizontally scrolling row per tag group, header `<tag> · <count>` (`주연 · 3`, `하층 구역 · 3`); tag chips stay on top (`전체 11 · 주연 · 등불 결사 · 하층 구역`).
- Card: portrait 4:5 crop (or the first letter + `이미지 고르기`), title, right mono first appearance, subtitle line. Title is the display name in effect at the lens when one is set: `???` with subtitle `하르트 · 이름 공개 1-9`; otherwise the name with one-liner/list fields (`리엔` · `등불 결사 · 막내 등불지기`; `마르타` · `결사장`). A `홀로` badge marks a portrait with the holo effect on.
- Board sizes: first group 196×245 cards, later groups 132×165 cards.
- Motion: list → card morphs each thumbnail into its card in place, decel 360 ms, list text exits first (accel 160 ms); the row snaps to the nearest card (320 ms) with 8 % image parallax and a tick haptic. Reduce motion → 120 ms fade.
- Sources: planning `카드 보기 · 이미지`; boards `CastCards`, `MCardView`, `MCarousel`; motion spec `MotionSpec2`.

#### CST-05 · 인물 만들기 (create a character) — P0 (0.2.0)
- Entry points: header `+` on the list; `인물 추가` in the empty state; new-project step 4 `주인공 (선택)` (creates one character with that name); recorder `⋯ › 인물로 만들기` (CST-49).
- Only `이름` is required (`이름만 적어도 시작 · 나머지는 나중에`). The new character gets a new ULID key, the next record number (§6 Q11), empty project fields, and no relations.
- After creation the record opens on `개요`. No create sheet is drawn; [PROPOSED] a bottom sheet titled `인물 추가` with one `이름` field and primary `넣기`.
- **Superseded (0.2.0):** the header `+`, the `인물 추가` bottom sheet and "after creation the record opens" are replaced by CST-66: the `＋ 새 인물` row and picker rows open the create card, `만들기` stays on the list, and `만들고 열기` opens the record on `개요`. New-project step 4 and `인물로 만들기` keep their own flows. Every path still writes through `CharacterEdits` with the key and number rules above.
- Sources: boards `EmptyCast`, `NewStart`; rule 23 §4 table.

#### CST-06 · 펼친 화면 배치 (unfolded layouts) — P1
- Setting `설정 › 화면 › 펼친 화면 배치` (owned by Settings) picks one of two layouts for list modules:
  - **A `목록 + 상세`** (default, `TwoCast`): list 390 left | record 409 right. Tapping a row opens it in the right pane.
  - **B `가운데 한 칸`** (`ColCast`): the list in one 560 px reading column; a record opens full width at the same column.
- Fold continuity (`MFoldList`): unfolding keeps the list where it is and opens the selected character (e.g. 리엔) in the right pane, sliding in from 48 px right, decel 360 ms. Folding shows the pane touched last; if the record was being viewed it stays the record. With B the list just widens to 560. State is saved for devices that close apps on fold.
- Sources: planning `화면 · 디자인` (커버 화면과 펼친 화면, 접기 · 펼치기); boards `TwoCast`, `ColCast`, `MFoldList`, `LayoutSettings`.

#### CST-66 · 그 자리에서 만들기 · 인물 (create in place) — P0 (0.2.0, U-15)
Replaces CST-05's entry points. The overlay is the shared create card owned by `core:designsystem` (D17). This feature gives the 인물 field set and the cast behaviour.
- **List row (U-15.1).**
  - The first row of the character list is a button row: icon `plus` + text `새 인물` (ICON class b; accessibility name `새 인물`; touch target at least 44).
  - It sits directly under the filter chips (and under the `목록 | 관계도` segment), above the first tag section and above the `LockRow`.
  - It is present with 0 characters and with many, and stays put while a query is typed.
  - In the 3D views (CST-67), which have no rows, the same action is a head button with icon `plus` + `새 인물` (D31).
- **Overlay (U-15.2, U-15.8).**
  - Tapping the row opens the create card. The screen stays on the list.
  - The card is a centred floating card on cover and inner screens, and the back dims. It rises above the keyboard so that no field is hidden at cover width with the IME up.
  - Its look matches the character card of the card views (portrait area on top, then the fields).
  - Title: `새 인물`.
  - Shape, focus, keyboard, motion and accessibility are DS-72's; this feature does not restate them.
- **Fields (U-15.5),** in this order: `이미지 · 이름 · 표시 이름 · 한 줄 소개 · 태그`.
  - `이미지`: the portrait slot (CST-13) through the image area's `이미지 고르기` and crop. Empty shows the first-letter placeholder. The pick is held by the card and imported only on `만들기` or `만들고 열기`; `취소` imports nothing.
  - `이름`: required. It is focused when the card opens (the IME comes up), and the value is trimmed and NFC.
  - `표시 이름`: optional. It is stored as the base display name (`displayNameBase`), with no change record.
  - `한 줄 소개`: optional, one line.
  - `태그`: chips of the live project tags, several may be checked. No tag is created from inside this card (no nested create card), so the tag picker's `＋ 새 태그` (CST-75) is not offered here.
  - Every other field is filled later on the record (U-15 decision 5).
- **Buttons (U-15.6).** `취소 · 만들고 열기 · 만들기`.
  - `만들기` and `만들고 열기` are disabled while `이름` is blank (only whitespace counts as blank).
  - `만들기` creates the character, closes the card and stays on the list. The scroll position is kept when the new row is already in view; otherwise the list scrolls just enough to show it. The new row (every copy of it, CST-68) is highlighted as DS-72 gives it (1 200 ms).
    - If a tag chip filter is active and the new character does not carry that tag, the filter returns to `전체` so the new row can be seen. Nothing is pre-checked from the filter (no guessing).
  - `만들고 열기` creates the character and opens its record on `개요` (cover: full screen; inner layout A: right pane).
  - `취소` and the system back close the card and write nothing. A tap on the dimmed back closes it only while every field is empty; with typed content it does nothing (DS-72).
- **Write.** One edit through `CharacterEdits.create`, which gains the optional arguments `displayNameBase`, `tagKeys` and `portrait` (additive). It is one undo step. The key, record number and empty project fields follow CST-05.
- **Pickers (U-15.3, U-15.6).** Every sheet that picks a character has the same row on top:
  - the pickers are the speaker picker (CST-37, SCR's sheet), the `@` link picker (SCR-55: one create row for the kind in view), the relation partner picker (CST-29), the scene `등장` picker, and the speaker picker of `화자 한꺼번에 바꾸기` (CST-42). U-15.3 names three of them as examples; the others follow the same rule (§8.3 Q45);
  - the merge target picker (CST-45) has **no** create row, because a merge needs an existing item.
  - With an empty query the row reads `＋ 새 인물`. When the query matches no listed character, by the picker's own matching (name, 초성, tag; trashed and locked hidden characters are never listed), the row becomes `＋ ‘세라’ 새 인물`. Opening the card then fills `이름` with the trimmed query.
  - In a picker the card shows only `취소 · 만들기` (no `만들고 열기`). `만들기` creates the character, closes the card and returns it to the picker as the picked value: the speaker is set, the link is inserted, or the partner is chosen. The picker then closes as after any pick.
  - The row is above every other row, including CST-37's `지운 인물 · 노아 · 되살리기` row, which comes second.
- **Read-only (U-15.10).** In a read-only project (the example) the list row, the 3D head button, the empty-state `인물 추가` and every picker create row are hidden. A hidden project, once unlocked, shows them like any project.
- **The other U-15 kinds** use the same card and the same rules, and are specified by their owners:
  - 장소 · 용어 · 세력 (WLD-70) and 사건 (WLD-74), in `40-world.md`;
  - 태그 (CST-75) and 관계 종류 (CST-76), here;
  - 에셋, `종류 · 이름 · 파일`, only with `게임 제작 도구` on: the script area;
  - 페이지 · 캔버스, `이름`: the canvas area.
- Sources: `u15-create-in-place.md` rows 1–3, 5, 6, 8, 10 (planning); scope U-15.1 … U-15.3, U-15.5, U-15.6, U-15.8, U-15.10, D17, D31; `icon-buttons.md` row `목록 머리 ＋` (planning); board `scr_icon_variants.py` (`＋ 새 인물` row before the first section).

#### CST-67 · 카드 보기 · 3D 보기 (card view options) — P1 (0.2.0, U-16)
Replaces CST-04. It covers the character card list only (D2). The 서재 views are the library area's (LIB-93). The settings, the stored fields and the toggle rule belong to `00-domain-model.md` §13.4 and LIB-94; this feature follows them and adds the cast specifics.
- **Setting (LIB-94).** `설정` section `화면`, row `카드 보기` › opens a screen with one radio list of the seven values of `AppSettings.castCardStyle` (00 §13.4), in this order: `부채꼴 덱 · 회전 목마 · 겹친 더미 · 타로 펼침 · 기울어진 벽 · 롤로덱스 · 목록`.
  - The default is `부채꼴 덱` (`FAN_DECK`; D1, which the user marked 확인 필요).
  - The tilt switch is LIB-94's shared `AppSettings.viewTilt` row; one switch serves 서재 and cast.
  - The board `U16SetCardView` also draws sub lines, a `움직임` section and a `머리 토글` line. LIB-94 does not adopt them, and this spec follows LIB-94 (§8.3 Q41).
- **Head toggle (DS-39, DS-74).** In the character list head there are two cells: `목록` (icon `list`) and the chosen 3D view (icon `Grid`, accessibility name = the style's label, e.g. `부채꼴 덱`).
  - The view shown is `ShownView.of(castCardStyle, castToggle)` (00 §13.4). An absent `castToggle` shows the 3D view, and the last position is remembered across restarts.
  - When the setting is `목록`, no toggle is drawn.
  - The 0.1.0 `viewByScreen["cast"]` value is kept and not read.
  - The `서재 보기 바꾸기` sheet that the board `U16SetHeaderToggle` draws for 서재 is not adopted by LIB-93, so the cast head has no such sheet either. The style is changed in 설정.
- **Failure.** A view that cannot draw shows the list for that session, never a blank screen (as LIB-93).
- **What a 3D view shows.**
  - The characters of the current chip filter, in list order (the active sort, CST-69, applies). Each character is shown once (no tag sections), and the filter chips stay above the view.
  - Each card: the portrait 4:5 crop or the first letter, the title and the subtitle by CST-04's rules (display name at the lens, `???` with `하르트 · 이름 공개 1-9`, `홀로` badge).
  - Trashed characters never appear. Locked hidden characters are never drawn as cards. One lock item sits at the end of the view, as LIB-93's lock card, with `숨긴 인물 <n>` and the class-b button `지문으로 보기` (icon `fingerprint`). Once unlocked, hidden characters are cards like the rest.
  - With 0 characters the CST-01 empty state replaces the view.
- **Controls.** Every gesture has a visible button, and there are no hidden gestures.
  - `이전 · 다음` are icon buttons (`prev` · `next`, accessibility names `이전 카드` · `다음 카드`).
  - The card's main button is `열기`, or `뒤집기` where a view flips a card first (ICON class c).
  - 롤로덱스 has vertical previous and next buttons plus the 초성 index row.
  - Per-view behaviour follows the boards: `U16CastDeck`, `U16CastRing` (`scr_u16_cards_a.py`), 겹친 더미 and 타로 펼침 (`scr_u16_cards_b.py`, whose 타로 board draws world cards; in 0.2.0 it draws characters, D2), 기울어진 벽 and 롤로덱스 (`scr_u16_cards_c.py`).
  - `열기` opens the record: full screen on cover, the right pane in inner layout A.
- **Tilt and motion.** Tilting the device moves the view slightly (parallax and a light band).
  - It follows `Tilt.follows(viewTilt, motion)` (00 §13.4, DS-74): it stops when the LIB-94 tilt switch is off or motion is reduced. With reduced motion there is no 3D motion at all, only 120 ms fades.
  - All motion goes through `CivilightMotion`, and `MotionLint` passes.
  - Cards far from the focus are not drawn, so a 200-character project stays responsive.
- **Inner screen.** The view lives in the list pane (layout A) or the 560 column (layout B). `관계도` overrides the view while it is selected (CST-01).
- Sources: scope U-16, D1, D2, D31; decisions row `U-16 서재 · 카드 보기 (2026-11-24)` (planning); `00-domain-model.md` §13.4; LIB-93, LIB-94; DS-39, DS-74; boards `U16SetCardView`, `scr_u16_cards_a.py`, `scr_u16_cards_b.py`, `scr_u16_cards_c.py`; `icon-buttons.md` rows 167, 171, 176 (planning).

#### CST-68 · 여러 태그 · 묶음마다 (several tags, every group) — P1 (0.2.0, N-44)
- A character is listed in the section of **each** of its live tags. Sections follow the tag order; inside a section the rows follow the list order (or the active sort). A character with no live tag is in the untagged section, which comes last, as built. Closes the first part of §6 Q13.
- `전체 N` counts each character once (`전체 11` when 리엔 sits under both 주연 and 등불 결사).
- A tag chip shows only that tag's section, as before.
- Selection (inner layout A): the selected character is highlighted in every section where it appears.
- Trashed tags make no section (CST-75). A character whose tags are all trashed is listed in the untagged section.
- Unchanged: the relationship map counts (`N명 중 M명`) and the tag settings usage line (`인물 3`), which already count distinct characters.
- Sources: decisions row `N-44` (planning); scope N-44.

#### CST-69 · 오래 안 나옴 정렬 (sort: longest absent) — P1 (0.2.0, N-47)
- The list sort control (CST-03, built as `CastFilterSheet`) gains the order `오래 안 나옴`.
- Order:
  1. Characters with an appearance, by the manuscript position of their last appearance (CST-71), earliest first. The character absent for the longest comes first.
  2. Ties (the same last scene) keep list order (`orderKey`).
  3. Characters with no appearance come after all others, in list order.
- It applies inside each tag section, in a chip-filtered list and in the 3D views. It ignores the lens (D23) and never changes the stored order.
- `대사 많은 순` from the N-47 sketch is not frozen in and is not built.
- Sources: decisions row `N-47` (planning); scope N-47, D23.

### B. Character record (profile)

#### CST-07 · 인물 기록 머리 · 문서형 (record header, document layout) — P0
- **Cover layout** (`Profile`): the background image (인물 배경, 9:16 crop) fills the top ~430 px and fades and blurs progressively into `bg` (blur 22 px masked from 25 % to 60 %). Glass round back button (44 px) top-left; lens chip top-right (`시점 · 1-5` / `최신 · 1-7`). Content starts at y≈300:
  1. `.sec` line `인물 기록 007 · 시점 1-5` (record number · lens).
  2. Name, Myeongjo 40/48 weight 800.
  3. `한 줄 소개`, Myeongjo 16/27 `ink2`.
  4. Field table (`ListRow .row`: label `ink3` | value). Values with history show the start of the record in effect in `ink3` and `◆` in `acc`: `소속 | 등불 결사 1-3부터 ◆`. Numbers in mono (`154 cm`, `1-2`, `18 / 24`).
  5. Sections of the active tab (CST-08).
- **Field table rows** = fields marked `머리` in field order, then the fixed computed fields `첫 등장` and `보이스` unless hidden from the header (CST-14). Cover shows at most 6 rows; more → last row `항목 더 보기` expands the table. Example (리엔, game-profile bundle): `나이 17세 · 소속 등불 결사 ◆ · 신장 154 cm · 종족 인간 · 첫 등장 1-2` (+ `보이스 18 / 24`).
- **(0.2.0)** The `보이스` row (`보이스 18 / 24`) is left out of the field table while `게임 제작 도구` is off (CST-74). The `LensChip` top right is the small chip of CST-70.
- **Bottom glass bar**: `Tabs` row `개요 · 말투 · 관계 · 게임 텍스트 · 비밀` above the `CommandBar` (`명령 또는 검색`, mic). **(0.2.0)** No mic (S-8). With the switch off the tabs row is `개요 · 말투 · 관계 · 비밀` (CST-74).
- **Inner**: same structure inside the 409 px right pane (layout A) or the 560 column (layout B); the background image uses its 4:3 crop.
- **Record ⋯ menu** [PROPOSED placement; no board draws it]: `숨기기` (CST-39), `지우기` (CST-40), `합치기` (CST-45); `모든 인물에 넣기` appears on single-character fields (CST-22).
- Sources: planning `인물` ("책 한 쪽처럼 읽힌다: 번호 · 큰 이름 · 한 줄 소개 · 항목 표 · 본문"); `인물 항목` §1, §4; boards `Profile`, `TwoCast`, `TourCast`, `RCProfileAdd`, `RTEditTwo`, `RTPlanned`; memory concept (document layout, portrait hero).

#### CST-08 · 탭 다섯 · 개요 본문 (five tabs, overview body) — P0
- Tabs are fixed and cannot be renamed or removed: `개요 · 말투 · 관계 · 게임 텍스트 · 비밀`. Selected tab = `acc` 2 px underline. Tapping a tab shows its content; the selected tab is kept on fold/unfold.
- **(0.2.0)** With `게임 제작 도구` off the tabs are `개요 · 말투 · 관계 · 비밀` (4), and `게임 텍스트` is hidden with all its data kept (CST-74). If the remembered tab is `게임 텍스트` while the switch is off, the record opens on `개요`. Opening `비밀` while `이 탭 잠그기` is on asks for the device lock first (CST-73).
- **`개요`**:
  1. Field table (CST-07).
  1a. **(0.2.0)** Appearance bars and `마지막 등장` (CST-71), directly under the field table.
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
  - **Superseded (0.2.0, U-10, D16):** the single button of a sheet with no history reads `저장`. `이 값 고치기` is used only for a value with history edited at a lens point (rule 11 §2). The same goes for the tag name, the relation kind name, the fixed-field sheets and the CST-72 `한 줄 요약` sheet.
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
- **(0.2.0, S-1)** A project always opens at `최신` (D9). Another point is picked only from the small lens chip on the cast and world heads (CST-70). Everything above is unchanged for a point picked there, including the past-lens banner and the two buttons.
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
- **(0.2.0)**
  - Tags gain an optional `색` (CST-75).
  - The text line that added a tag is **superseded** by the picker row `＋ 새 태그` and the create card (CST-75).
  - A character with several tags is listed under each tag (CST-68).
  - A tag can be deleted to the trash (CST-75).
  - Trashed tags are not shown anywhere: chips, sections, field sheet, speaker picker, gallery. They stay in each character's `tagKeys`, so restoring re-attaches them.
- Sources: `인물 항목` §1 (태그 → 목록 묶음 · 화자 고르기 필터); planning `인물` (태그 필터 주연 · 세력 · 구역); boards `Cast`, `Picker`, `CastCards`.

#### CST-13 · 이미지 · 초상과 배경 (portrait and background) — P1
- Two image slots per character:
  - **인물 초상** (portrait): crops `카드 4:5` and `아바타 1:1`; holo effect optional (default off).
  - **인물 배경** (background): crops `커버 9:16` (cover) and `펼친 화면 4:3` (inner); no holo ("글자 뒤"); shown at the top of the record, fading downward.
- Each slot takes one of the 8 built-in presets (`잿빛 새벽 · 등불 · 심해 · 안개 숲 · 사막 유적 · 성운 · 붉은 성채 · 설원`) or a gallery/file image. The original is kept and is cut once: one picture, one crop per fixed ratio. For the portrait, `카드 4:5` is set first and `아바타 1:1` follows it automatically (the largest square inside that crop, top-aligned so the avatar keeps the face; rotation and mirror inherited) and is saved on `적용`. The crop screen always shows the avatar as a preview (`아바타 1:1 · 자동`); tapping it opens it to adjust its own crop (`· 직접`), and `처음대로` there returns it to `자동`. A portrait with only the 4:5 crop stored draws the avatar derived the same way. The background's two ratios are cropped separately (a 4:3 cut from the 9:16 would be a thin band). Any ratio can be re-cropped any time. No image → first-letter card + `이미지 고르기`.
- Picking, cropping, presets and holo belong to the image area (`ImagePick`, `ImageCrop`, `ImageSlots`). This feature defines the slots and where each crop is used.
- Sources: planning `카드 보기 · 이미지`; boards `ImageSlots`, `CastCards`, `Profile`.

#### CST-14 · 첫 등장 · 보이스 진행 (computed fixed fields) — P1
- **첫 등장** (first appearance): the earliest scene in story order where the character is the speaker of a block or is in the scene's `등장` list (speakers are added automatically; silent characters are added by hand in the scene sheet). Shown as the episode (`1-2`). Scenes in the trash do not count. Never stored.
- **보이스 진행** (voice progress): `<filled> / <total>` over the character's voice lines (CST-16), shown as `18 / 24` in the header and `보이스 · 18 / 24` on the `게임 텍스트` tab with a flat progress bar. `filled` = lines whose state is not `비어 있음` (consistent with the check row `보이스 6/24 비어 있음`; see §6 Q5).
- Both are fixed fields: they cannot be deleted but can be hidden from the record header (`첫 등장 · 보이스`) [PROPOSED control: a switch per fixed field in the `고정 항목` box of CST-19].
- **Check row** (rendered by the check center, filter `보이스`): `▲ 보이스 비어 있음` · location `인물 · 리엔` · body `리엔 · 신뢰 상승 후` / `보이스 6/24 비어 있음` · action `보이스 쓰기` (opens `게임 텍스트` at the first empty line). The board draws this row with an `ink3` label.
- **(0.2.0)** With `게임 제작 도구` off, `보이스 진행` is hidden wherever it appears: the header row, the `보이스` header switch in CST-19, the `고정 항목` box and the check row above with its `보이스` chip (G-2, D33). The voice lines and their states are kept. `첫 등장` is unchanged and always shown.
- Sources: `인물 항목` §1 (첫 등장 · 보이스 진행 계산, 감출 수는 있다); `저장 파일 양식` §4 (계산값 적지 않음); planning `지도 ↔ 연표` (등장 인물); boards `Profile`, `Voice`, `Check`, `Cast`.

#### CST-15 · 말투 탭 (speech tab) — P1
- Content: the character's way of speaking (1인칭 · 어미 · 말버릇) as Myeongjo prose under `.sec` `말투`, e.g. `1인칭은 ‘저’. 누구에게나 존댓말을 쓰고, 확신이 없으면 ‘…요’로 말끝을 흐린다.`; then the collected 호칭 chips (`Tag` style), one per partner, `<partner> → <호칭>`: `하르트 → 하르트 씨 · 세라 → 선배 · 오웬 → 아저씨`.
- The 호칭 chips are **not stored here**: they are read from the viewer's `호칭` slot of each relation at the lens (CST-31). Tapping a chip edits that relation slot; edits from either place are the same value.
- The prose body accepts `⌁` memo paragraphs inserted by the recorder (CST-49).
- See §6 Q9 for whether 1인칭 · 어미 · 말버릇 are separate fields and where the panel's one-line `말투` comes from.
- **(0.2.0, N-43)** Q9 is decided. The tab starts with the `한 줄 요약` row (CST-72), then the prose under `.sec` `말투`, then the 호칭 chips. The reference panel shows only the summary (CST-38).
- Sources: planning `인물` (말투); rule 13 §5; boards `Relations`; rule 23 §4.

#### CST-16 · 게임 텍스트 · 보이스 대사 (in-game voice lines) — P1 (0.2.0: only with `게임 제작 도구` on)
- **(0.2.0)** The whole `게임 텍스트` tab (this feature and CST-17) is shown only while the project's `게임 제작 도구` is on (G-2). Off, it is hidden with its data kept (CST-74). N-45 (a project list of voice slots) is dropped, so voice lines stay per character exactly as below.
- Tab header: `.sec` `보이스 · 18 / 24` with right `게임 순서`; flat progress bar (4 px, `ink` fill on `tint`, width = filled/total). Header right of the record: `+` adds a voice line at the end.
- Each line in game order: number (mono 11, `01`), slot name (Noto Sans 13 semibold: `임명 · 대화 1 · 대화 2 · 신뢰 상승 후`), right state label, then the text (Myeongjo 15/24; `—` in `ink3` when empty).
- States and colours: `녹음 대본 완료` (`ink2`), `쓰는 중` (`acc`), `비어 있음` (`ink3`). `비어 있음` whenever the text is empty [PROPOSED: derived]; otherwise the author picks `쓰는 중` or `녹음 대본 완료` [PROPOSED control: tap the state label].
- Sample: `01 임명 녹음 대본 완료 “저… 정말 제가 맡아도 되는 걸까요?”` · `02 대화 1 녹음 대본 완료 “등불은요, 꺼지기 직전에 제일 밝아져요.”` · `03 대화 2 쓰는 중 “하르트 씨는… 아니에요, 아무것도.”` · `04 신뢰 상승 후 비어 있음 —`.
- Rule 21 §4 table: profile game text (voice lines and archive documents) **goes to the game**, gets **no term candidates**, and whether its `@` links create `▲ 공개 전 사용` is **undecided** ("미정, 위치 없음"); its links appear on term cards under `원고 밖` with the tag `게임 텍스트`. Until decided, no `▲` is raised from profile game text (§6 Q28). Profile voice lines have no voice ID in v1 (CST-52).
- Sources: planning `인물` (게임 텍스트); rule 21 §4 table (rows "인물 프로필 게임 텍스트"); rule 12 §6 (범위); board `Voice`.

#### CST-17 · 게임 텍스트 · 아카이브 문서 (archive documents) — P1 (0.2.0: only with `게임 제작 도구` on)
- **(0.2.0)** Hidden with the tab while the switch is off (CST-74). Archive documents, unlock points and their `@` links are kept.
- Section `.sec` `아카이브 문서 · <count>` below the voice lines. Row per document: `기록 <n> · <title>` with sub `<글자 수>자 · <unlock point> 이후 열림`, or `비어 있음 · <unlock point> 이후 열림` when empty. Samples: `기록 1 · 등불지기의 일지` / `1,120자 · 1-7 이후 열림`; `기록 2 · 대정전 그날` / `비어 있음 · 2장 이후 열림`.
- The unlock point is an episode or chapter. Character count is derived (no spaces rule given; [PROPOSED] count all characters except line breaks, formatted with thousands separators).
- Tap → full-width Myeongjo document editor with `@` links [PROPOSED; not drawn].
- Sources: planning `인물` (아카이브 문서 · 글자 수 · 열리는 시점); board `Voice`.

#### CST-18 · 비밀 탭 · 비밀 항목 (secret tab and secret fields) — P1
- The `비밀` tab holds settings not yet told to players: (1) every field whose `비밀 항목` switch is on (CST-21), shown only here, never in the header, list, filters, speaker picker or panels; (2) a free prose body that accepts recorder memo paragraphs (CST-49).
- Export: the export sheet's display-layer list gets `비밀 항목 포함`, default off (CST-26).
- The 비밀 tab is called "항목 숨기기 대상" (planning; rule 19 §5 버린 안); whether it can be locked separately is §6 Q10.
- **(0.2.0, N-43)** Q10 is decided. The tab head has `이 탭 잠그기`; when it is on, every opening of the tab asks for the fingerprint or the screen lock (CST-73).
- Sources: planning `인물` (비밀); `인물 항목` §1, §8; rule 19 §5; board `RCFieldAdd` (`비밀 항목 · 비밀 탭에만 보임`).

#### CST-70 · 시점 렌즈 칩 · 기본 최신 (lens chip on cast heads) — P0 (0.2.0, S-1)
- **Where the chip is.** The `LensChip` is shown, small, only on these cast heads:
  - the character list head, in `목록`, in the 3D views and in `관계도` (whose `시점 · 1-7` chip is the same chip);
  - the character record head (cover top right; inner pane head).
  - The world heads are in WLD-71.
  - The small size is a `core:designsystem` `LensChip` variant. If none exists, it is a Request to that module, not a local copy.
- **Default.** A project always opens at `최신` (D9).
  - A point picked on a cast or world head is one lens state for the open project, shared by the cast and world heads. It lasts until the project is closed.
  - 0.2.0 neither reads nor writes the lens state that 0.1.0 stored (kept, not applied; scope §8).
- **Screens without a chip** always read at `최신`, whatever point is picked on a cast or world head: home, script, 연표, check center, search, and every other screen.
  - The script's `이 씬 · 1-7` chip is not a lens and is unchanged (U-14.20).
- **Unchanged:** the chip copy (`최신 · 1-7`, `시점 · 1-5`), its `acc` outline when not at 최신, the lens sheet it opens, the past-lens banner and the two edit buttons (CST-10), and every `◆` record.
- Sources: decisions row `단순화 · 시점 렌즈` (planning); scope S-1, D9.

#### CST-71 · 등장 막대 · 마지막 등장 (appearance bars, last appearance) — P1 (0.2.0, N-47)
- **Place.** On the `개요` tab directly under the field table (CST-08 item 1a). Characters only; places and terms do not get it.
- **Appeared (D23).** A character appeared in a scene when a live block of that scene either:
  - has the character as its speaker (a per-block display name, N-43, keeps the original speaker, so it counts); or
  - holds a live `@` link to the character in game-output text: dialogue, narration, system text or a choice option line.
  - Memos, direction blocks, pages and profiles do not count.
  - The scene `등장` list (silent cast) does **not** count, unlike `첫 등장` (CST-14 is unchanged).
  - Trashed scenes and blocks do not count. While the hidden lock is closed, hidden scenes are not counted, so no number reveals them. Unlocked, they count.
- **Bars.**
  - One bar per live episode of the project, in manuscript order across chapters. Its value is the number of scenes of that episode in which the character appeared.
  - Bar height is proportional to the value divided by this character's largest value. With all values 0, every bar is an empty slot.
  - Episodes after the 집필 선 have no content, so their bars are empty.
  - The bars ignore the lens (D23).
  - The row scrolls horizontally inside its section when it is wider than the column; the page never scrolls sideways.
  - Bars are not tappable in 0.2.0 (the sketch's tap-to-scene list is not frozen in).
- **`마지막 등장`.** A `.row` line under the bars: `마지막 등장 | 1-4`, the episode of the last scene in manuscript order where the character appeared. With no appearance it reads `마지막 등장 | —` (`—` in `ink3`, as an empty voice line is drawn).
- **Section label:** `TODO_COPY_CAST_APPEARANCE_HEAD`. The sketch's `등장 · 씬 14 · 대사 212 ›` is not frozen in.
- **Accessibility:** the bar row reads one item per episode, `TODO_COPY_CAST_APPEARANCE_BAR(episode, n)`.
- A project with no episode hides the section.
- **Domain.** `domain:cast` `Appearances` (pure, derived, never stored):
  - `of(doc, characterKey, hiddenUnlocked): AppearanceBars(bars: List<EpisodeBar(episodeKey, label, scenes)>, last: LastAppearance?(sceneKey, episodeLabel))`;
  - `lastPositions(doc, hiddenUnlocked): Map<Key, ManuscriptPosition?>`, used by CST-69.
- Sources: decisions row `N-47` (planning); scope N-47, D23.

#### CST-72 · 말투 한 줄 요약 (speech one-line summary) — P1 (0.2.0, N-43)
- `Character` gains `speechSummary: String?` (additive, default null). It is one line: NFC and trimmed, line breaks are removed on save, and empty is stored as null.
- **말투 tab.** The first row is `.row` `한 줄 요약 | 반말 · 짧게 끊음`; when empty it reads `한 줄 요약 | —` (`ink3`). The prose under `.sec` `말투` and the 호칭 chips follow, unchanged (CST-15).
- **Edit.** Tapping the row opens a sheet titled `한 줄 요약`: one-line input, `닫기`, primary `저장` (U-10: no history, so not `이 값 고치기`). It has no `◆` history. Saved through an additive `CharacterEdits.setSpeechSummary`; undo as other saves.
- **Reference panel.** CST-38's `말투` line shows only the summary. When the summary is empty the `말투` line is left out. The prose is never shown in the panel.
- Recorder `카드에 넣기` → `말투` still appends to the prose (CST-49).
- Closes §6 Q9.
- Sources: decisions row `N-43 · 말투 형식` (planning); scope N-43.

#### CST-73 · 비밀 탭 잠그기 (lock the secret tab) — P1 (0.2.0, N-43)
- `Character` gains `secretTabLocked: Boolean = false` (additive). The lock is per character.
- **Switch.** The `비밀` tab head has the switch row `이 탭 잠그기`, sub `TODO_COPY_CAST_SECRET_LOCK_SUB`.
  - Turning it on asks nothing.
  - Turning it off first asks for the device lock, as an opening does; a cancel leaves it on.
  - If the device has neither a biometric nor a screen lock, the switch is disabled with the sub `TODO_COPY_CAST_SECRET_LOCK_NO_DEVICE`.
  - In a read-only project the switch is disabled.
- **Gate.** While the switch is on, **every** opening of the `비밀` tab asks for the fingerprint or the screen lock (system biometric prompt with the device-credential fallback, through `core:ui` `LockRules`, the same prompt the hidden items use). An opening is any of:
  - selecting the tab from another tab;
  - opening a record whose remembered tab is `비밀`;
  - the app coming back to the foreground with the tab shown.
- **Before confirmation** the tab's content is not composed, so no secret text is drawn behind the prompt.
  - Cancel or failure keeps the previously selected tab. If the record opened on `비밀`, it shows `개요`.
  - Leaving the tab, leaving the record or the app going to the background locks it again.
  - Fold and unfold (a configuration change) keep a confirmed tab open.
- **Every surface outside the tab that could show secret content, while the switch is on.** Prompts are never shown outside the tab; each surface follows its rule:

| Surface | Rule while `secretTabLocked` |
| --- | --- |
| Search (CST-27) | no hits from that character's secret fields or `secretBody` |
| Term, place and faction cards, `쓰인 곳 · 원고 밖 · 메모` (WLD-19): sentences from that character's `secretBody` or secret fields | not listed and not counted |
| Reverse `연결` lists (`이 항목으로 잇는 인물` on cards, the faction row count `인물 4`, WLD-04, WLD-10) | the character's secret `연결` fields are left out |
| Merge comparison sheet (CST-45), which shows secret field values | opening it for this character first asks for the device lock; a cancel does not open it |
| Field history sheet of a secret field | opened only from inside the tab, so it is already behind the gate |
| Delete impact sheet, trash row | counts only, as before (no secret text) |
| Recorder `카드에 넣기` into `비밀` | allowed (it appends and shows nothing) |
| Export `비밀 항목 포함` (CST-26, JSON with `게임 제작 도구` on) | unchanged: a deliberate export by the author (§8.3 Q40) |
- **Pure gate:** `feature:cast` `SecretTabGate`, with the states `LOCKED → PROMPTING → OPEN` and the events `select`, `confirm`, `cancel`, `leave`, `background`. It is JVM-tested (TC-N43-04).
- Closes §6 Q10.
- Sources: decisions row `N-43 · 비밀 탭 잠금` (planning); scope N-43 (`core:ui` `LockRules`).

#### CST-74 · 게임 제작 도구가 꺼져 있을 때 · 인물 (cast surfaces of the switch) — P0 (0.2.0, G-2)
- **The switch.** `게임 제작 도구` is `Project.gameTools`, owned by the library area (LIB-91; 00 §13.2). It is off for new projects. A 0.1.0 project with game data opens with it on (G-3, D6). Cast reads it through `GameTools.on(project)` and never writes it.
- **Cast surfaces while it is off.** Every row below is hidden; its data is kept. Turning the switch on shows everything as in 0.1.0, with nothing recomputed or lost.

| Surface | While off | Stored data |
| --- | --- | --- |
| `게임 텍스트` tab: voice lines (CST-16), their states, archive documents and unlock points (CST-17), `+` for a voice line | hidden; the tabs are `개요 · 말투 · 관계 · 비밀` | kept |
| Header row `보이스 18 / 24` and the `보이스` header switch on `인물 항목` (CST-19) | hidden | `showVoiceProgressInHeader` kept |
| `고정 항목` box on `인물 항목` | reads `고정 항목 · 6`, without `보이스 진행` | — |
| Check `▲ 보이스 비어 있음` and the check-center chip `보이스` (CST-14) | hidden (check center, D33) | acknowledgements kept |
| Voice code and voice IDs: the impact sheet's `게임 텍스트 n`, the merge sheet's row `보이스 ID \| 고정 2 · 그대로 · ◇`, the `Voice ID` reference row (CST-41), the name tag's voice code display | hidden | kept; automatic IDs are derived as in 0.1.0 |
| Game-engine JSON options: CST-26 `fields`, `비밀 항목 포함` for JSON, CST-35 `호칭` option | hidden with the JSON export (G-7) | settings kept |
| `@` links inside voice lines and archive documents, on term cards (`원고 밖`, tag `게임 텍스트`, WLD-19) | not listed and not counted | kept |
| Search hits inside voice lines and archive documents | not returned | — |
| Tour steps and help rows that point at `게임 텍스트` | left out (S-9) | — |
| Not cast, listed for completeness: the script's extra fields `보이스 ID` · `무대 위치` (SCR-25, LIB-91) | hidden by the script area | kept; a 무대 위치 value is game data for G-3 (D6) |

- **Always shown, on or off:** the display name with its history (CST-11), the per-block display name in the manuscript (N-43, G-6), `첫 등장`, relations, every field, and the bundle name `게임 프로필` (a set of profile fields, not game data).
- **G-3 input.** No cast function. `GameData.present(doc, exportRecords)` (`core:model`, 00 §13.2) already counts every `VoiceLine` and `ArchiveDoc`, live or in the trash (item 5). It also counts a 무대 위치 value (item 6), which is the script's extra field (SCR-25) and is hidden with the switch off by the script area. Cast never flips the switch.
- `.civilight` and backups always carry all of it (G-4).
- Sources: scope G-1 … G-4, G-6, G-7, D6, D33; decisions row `방향 · 게임 제작 기능` (planning); `scope-table-r2.md` §4 (4 tabs while off).

#### CST-75 · 태그 만들기 · 색 · 지우기 (tags: create, colour, delete) — P1 (0.2.0, U-15, N-44)
- `Tag` gains `color: String?` (a token name from the `core:designsystem` tag palette; null = no colour) and `deletedAt: Timestamp?` (trash). Both are additive. If no tag palette exists, it is a Request to `core:designsystem`.
- **Create (U-15.4, U-15.9).**
  - The tag settings screen (`TagSettingsScreen`, as built) has the top row icon `plus` + `새 태그`.
  - The character `태그` field sheet (CST-12) has the same top row in picker mode: `＋ 새 태그`, or `＋ ‘<query>’ 새 태그` when its search matches nothing.
  - Both open the shared create card (DS-72) titled `새 태그`, with the fields `이름 · 색`.
    - `이름` is required and must not equal a **live** tag's name (NFC, trimmed; `TagSettingsModel.canName` limited to live tags).
    - `색` is a row of palette swatches plus `없음` (the default; no colour).
  - Buttons:
    - from the settings list: `취소 · 만들고 열기 · 만들기`. `만들고 열기` opens that tag's edit sheet.
    - from the field sheet: `취소 · 만들기`. The new tag is checked on the character at once, in the same undo step as its creation.
- **Colour.** A colour dot (DS size) sits before the tag name on the list filter chips, the tag section headers, the tag field chips and the tag settings rows. With no colour there is no dot. The colour is also set or cleared in the tag's edit sheet (primary `저장`, U-10).
- **Delete (N-44).**
  - The tag edit sheet has the ghost button `지우기`. It opens a `ConfirmDialog`:
    - title `‘주연’ 지우기`;
    - lines `쓰는 인물 3` (live characters holding the tag, counted like the settings row, so locked hidden characters are not counted), then `이미지 1` only when gallery images hold it (the usage word of 55 §3.4);
    - the footnote `휴지통 · 30일 · 되살리면 다시 이어짐`.
  - Buttons: `취소` · `휴지통으로`. The confirm also shows with `쓰는 인물 0`.
  - The effect is one write, the new `CharacterEdits.trashTag(doc, tagKey, ctx)`, which sets `deletedAt`. Characters and gallery images **keep** the key in `tagKeys`; nothing else changes.
  - Toast: `태그 주연 지움 · 되돌리기` (the `<kind> <name> 지움` pattern of CST-40).
  - **One write path:** any tag delete in the app, including one the gallery offers, goes through `trashTag`. The existing `CharacterEdits.removeTag` (removes the key everywhere) is used only for the permanent delete below.
- **While trashed,** the tag appears nowhere:
  - not as a chip, a section, a field chip, a speaker picker chip, a gallery tag or filter, or a search-by-tag hit;
  - characters are listed under their other tags or in the untagged section (CST-68).
- **Trash (CST-48).**
  - Filter `인물`. Row: the name, right `<n>일 남음`, sub `TODO_COPY_CAST_TAG_TRASH_SUB`, chips `되살리기 · 영구 삭제`.
  - `되살리기` clears `deletedAt`, and every character and image that held the key shows the tag again.
  - If a live tag now has the same name, `되살리기` is disabled with the reason `TODO_COPY_CAST_TAG_NAME_TAKEN` (names stay unique among live tags; nothing is renamed by guess).
  - `영구 삭제`, or 30 days (`PurgeActions.runDue`), runs `removeTag`.
- In a read-only project the create rows and `지우기` are hidden.
- Sources: `u15-create-in-place.md` rows 4, 9 (planning); decisions row `N-44` (planning); scope U-15.4, U-15.9, N-44.

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
- **(0.2.0)** With `게임 제작 도구` off, the box reads `고정 항목 · 6` without `보이스 진행`, and the `보이스` header switch is hidden. The row sub's real count follows: `고정 6` (CST-74). `첫 등장` and its switch stay.
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
- **(0.2.0)** The game-engine JSON and everything above that belongs to it are offered only while `게임 제작 도구` is on (G-2, G-7). Export names stay stored either way.
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
- **(0.2.0, U-15)**
  - The partner picker has the `＋ 새 인물` row (CST-66). A partner created there is picked at once.
  - The `+ 새 종류` chip opens the relation-kind create card (CST-76) and picks the new kind into the slot being edited.
  - The `시작 시점` default is the cast head's lens, which is `최신` unless a point was picked (CST-70).
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
- **(0.2.0)** `+ 새 종류` opens the create card of CST-76 in picker mode. Where the script opens this sheet under U-14 is the script area's (U-14.10 tool row `◆`). The sheet's content is unchanged.
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
  - **Superseded (0.2.0):** creating a kind uses the create card of CST-76 (`이름 · 양쪽에서 부르는 이름`). The section the `+ 새 종류` chip sits in gives `사이 / 마음`. A filled `양쪽에서 부르는 이름` makes a 짝 kind; an empty one makes a 대칭 kind.
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
- **(0.2.0)** The `+ 새 종류` chip opens CST-76 in picker mode.
- Sources: rule 13 §7; boards `Canvas`, `CanvasC`, `CanvasCLabel`.

#### CST-35 · 관계 내보내기 (relation export) — P1
- `.civilight`: per relation the two character keys, the `사이` slot, and each side's mind · description · 호칭, each with its `◆` records.
- Game-engine JSON: option to export 호칭 only, default off. Relations normally do not go to the game.
- **(0.2.0)** The JSON option is shown only while `게임 제작 도구` is on (CST-74). `.civilight` is unchanged.
- Sources: rule 13 §9 (내보내기).

#### CST-36 · 세력 사이 관계 (faction-to-faction relations) — P2
- Storage uses the same shape (사이 + each side's stance) with faction endpoints; screens are decided later. For now the map's faction-region changes cover it. Deleting a faction sends its faction relations to the trash with it (rule 20).
- Sources: rule 13 §8; rule 20 §2 (세력을 지움).

#### CST-76 · 관계 종류 만들기 카드 (create a relation kind in place) — P1 (0.2.0, U-15)
- **Entry.** Every `+ 새 종류` chip: in the relation sheet (CST-29), the change-record sheet's `관계` segment (CST-30), the line-label sheet (CST-34) and the `관계 종류` settings sections (CST-32).
  - The chip keeps its place at the end of the kind chips. Kind lists are chips, not a list sheet, so no top row is added.
  - It opens the shared create card (DS-72) titled `새 관계 종류` (DS-72's title pattern; open there as Q-DS-30).
- **Category from context.**
  - A `사이` slot, or the `사이` section of CST-32, makes a `사이` kind.
  - A `<A>의 마음` slot, or the `마음` section, makes a `마음` kind.
  - The card never asks for the category (no guessing, nothing hidden).
- **Fields (U-15.9).**
  - `이름` is required, trimmed and NFC.
  - `양쪽에서 부르는 이름` is shown only for a `사이` kind. It is drawn as `<이름> ↔ [ ]`: the left part mirrors `이름` and the input is the other side's name.
    - Empty makes a 대칭 kind (`symmetry = SYMMETRIC`).
    - Filled makes a 짝 kind (`symmetry = PAIR`, `pairName` = the input, e.g. `선배 ↔ 후배`).
  - A `마음` kind has `이름` only, because minds are one-sided (CST-32).
- **Buttons.**
  - From a relation, change-record or line-label sheet: `취소 · 만들기`. The new kind is created and picked into the slot being edited. For a 짝 kind the holder rule of CST-29 applies. The partner the kind was chosen for holds `이름` and the record owner holds `pairName`, as 세라 holds `선배` when it is chosen in 리엔's sheet.
  - From `관계 종류` settings: `취소 · 만들고 열기 · 만들기`. `만들고 열기` opens the kind's edit view of CST-32.
- **Write.** Through the existing `domain:cast` `RelationKinds.add`. Creating and picking is one undo step.
- Hidden in a read-only project (U-15.10).
- Sources: `u15-create-in-place.md` rows 4, 9 (planning); scope U-15.4, U-15.9; rule 13 §3.

### E. Data for the script editor

#### CST-37 · 화자 고르기 연동 (speaker picker data) — P1
- The picker (script area) gets from cast: search (name, 초성), tag chips (`전체 · 주연 · 등불 결사 · 하층 구역`), chips for fields marked `필터` (e.g. `소속 · 등불 결사`), ordering groups `이 씬 등장` and `최근`, row sub = tags/one-liner or `표시 이름 ???`.
- Excluded: characters in the trash; locked hidden characters (bottom row `숨긴 인물 1 · 지문으로 보기`). When the current block's speaker is a deleted character, the top row is `지운 인물 · 노아 · 되살리기`.
- **(0.2.0, U-15.3)**
  - The first row is the create row of CST-66, `＋ 새 인물` or `＋ ‘세라’ 새 인물`. The deleted-speaker row comes second.
  - Chips show only live tags; a character with several tags matches each of its tag chips (CST-68).
  - With N-23, a choice block has no speaker, so the picker is not opened for it (script area).
- Sources: `인물 항목` §8 (화자 고르기); rule 19 §6; rule 20 §2 (찾기 · 화자 고르기); board `Picker`.

#### CST-38 · 참고 · 속성 패널 연동 (reference and properties panel data) — P1
- For a selected block the script panel asks cast for the speaker's state **just before that block**, plus that block's own changes as `여기서` rows (rule 11 §2):
  - `말투` one line (`반말 · 짧게 끊음`), the 호칭 toward partners (`리엔을 | ‘꼬마’`), `표시 이름 | ??? (1-9 공개)`, the first two `머리` fields (`화자 · 리엔 · 17세 · 등불 결사`).
  - **(0.2.0, N-43)** The `말투` line is the character's `speechSummary` (CST-72). It is left out when the summary is empty and never falls back to the prose. A block with its own display name (`이 블록만 · 표시 이름`, script area) shows the panel for its original speaker.
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
| 인물 동선 · map | no route for the hidden character | drawn (0.2.0: `인물 동선` is hidden for everyone, S-3) |
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
- **(0.2.0)** While `게임 제작 도구` is off, the sheet leaves out the game-text count, and the game text still goes to the trash with the character. The `인물 동선` effects are not mentioned anywhere (S-3).
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
- **(0.2.0)** While `게임 제작 도구` is off, the row `보이스 ID | 고정 2 · 그대로 · ◇` is left out; the IDs are handled exactly as in the table. The merge target picker has no `＋ 새 인물` row (CST-66).
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
- **(0.2.0, S-3)** Routes are hidden (WLD-75).
  - The impact and merge sheets leave out route counts.
  - The fix actions `다른 핀에 잇기 · 경로 지우기` are not offered.
  - The data effects in the table still happen out of sight: a route end stays dotted, and on a merge it moves to the target pin.
  - The node rows of the `Map pin / node` line keep their data and are view-only.
- Sources: rule 20 §2 tables.

#### CST-48 · 휴지통 줄 (trash rows) — P1
- Trash filters (rule 12 §5 changed by rule 20): `전체 · 프로젝트 · 씬 · 블록 · 인물 · 세계 · 변화 · 레코더` (`세계` = 장소 · 용어 · 세력).
- Character row: name (Myeongjo 15 bold), right `<n>일 남음`, sub `인물 · <프로젝트> · 대사 14 · 관계 1`, chips `되살리기 · 영구 삭제`. Hidden: name `숨긴 인물`, sub `인물 · 잿빛 새벽의 등불 · 되살리기는 지문 확인`. Merged: `합침 → 노아` [PROPOSED placement in the sub line].
- Deleted field definitions appear under `인물` (CST-23).
- **(0.2.0, N-44)** Deleted tags appear under `인물` too (CST-75). The filter list is unchanged.
- Sources: rule 20 §5, §7; board `Trash`.

### H. Recorder (rule 23)

#### CST-49 · 라이프 레코더 연결 (recorder links on characters) — P1
- A character is a valid `연결` target. Starting a recording or text memo from a character record pre-fills the `연결` chip with that character.
  - **Superseded (0.2.0, S-8):** the recorder opens only from home, so nothing starts a recording from a character record. The `연결` chip is set in the recorder itself.
  - The `레코더 · 정리 안 됨 n ›` link, which opens 기록함 and records nothing, and `카드에 넣기` and `인물로 만들기` are kept.
- `개요` shows `레코더 · 정리 안 됨 <n> ›` (`acc`, right of the first section header) when linked records are still `정리 안 됨`; tap → recorder inbox filtered to this character. Locked hidden records are not counted.
- `카드에 넣기` (from the inbox) can target a character tab `개요 · 말투 · 비밀 · <긴 글 항목>`: the text is copied to the end of that tab/field as a `⌁ 작가 메모` paragraph with a source mark; the record stays in the library.
- `⋯ › 인물로 만들기`: creates a new character (sheet: project · `이름`); `한 줄 소개` = the record's first sentence; a `⌁` memo paragraph with the text at the end of `개요`.
- Sources: rule 23 §3, §4; board `Profile` (`레코더 · 정리 안 됨 1 ›`).

### I. Later

#### CST-50 · 태그별 항목 묶음 (per-tag field sets) — P2
- Rejected for v1 (a 주연 having 20 fields and an NPC 5): changing a tag would make fields appear/disappear. Hiding empty fields gives the same effect; add only "if needed later".
- Sources: `인물 항목` §4 (버린 안).

#### CST-51 · 인물 표 CSV (character table CSV) — P2 (0.2.0: 안 함)
- **(0.2.0)** Not built. Game exports that 0.1.0 did not build are `안 함` (scope §7).
- Not in the current export formats; "useful for moving to a setting book or wiki, must decide whether to add". §6 Q6.
- Sources: `인물 항목` §8.

#### CST-52 · 게임 텍스트 보이스 ID (voice IDs for profile voice lines) — P2 (0.2.0: 안 함)
- **(0.2.0)** Not built (scope §7). N-45 (voice slot list) is dropped as well.
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

**0.2.0 additions (all additive: new optional fields with defaults; a 0.1.0 file reads with the defaults, and nothing is removed or renamed)**

| Entity | New field | Type · default | Feature |
| --- | --- | --- | --- |
| Character | `speechSummary` | String? · null | CST-72 |
| Character | `secretTabLocked` | Boolean · false | CST-73 |
| Tag | `color` | String? · null (DS tag palette token) | CST-75 |
| Tag | `deletedAt` | Timestamp? · null (trash; purge after 30 days runs `removeTag`) | CST-75 |
| `AppSettings` (device-local) | `castCardStyle`, `castToggle`, `viewTilt` | as defined in `00-domain-model.md` §13.4 (owner LIB-94); cast only reads them | CST-67 |
- Not cast data, read only: the project switch `Project.gameTools` (00 §13.2; read through `GameTools.on(project)`; owner LIB-91).
- Unchanged and still stored: `voiceCode`, VoiceLine, ArchiveDoc, `showVoiceProgressInHeader`, `relationExportAddress`, and the lens state 0.1.0 stored (not applied in 0.2.0, CST-70).

### 3.2 Records owned elsewhere that cast writes

**ChangeRecord** (engine owned by 시점 렌즈 · 변화 기록). Cast needs: `id`, `target` = `Field(entityId, fieldDefId)` | `DisplayName(characterId)` | `RelationSlot(relationId, slot)`, `value` (FieldValue payload / SlotValue / String for 호칭 and 표시 이름), `note: String?` (history note `세라의 안내로 합류`; `처음 만남`), `anchor` = `Block(blockKey)` (from that block, inclusive) | `Point(unitKey)` (from the start of that episode/scene; `1-5 #—`) | `WorldEvent(eventKey)` (before the story) | `SceneEnd(sceneKey)` ([PROPOSED] for `여기서부터 바뀜` at `최신`, CST-10, §6 Q27), `orphaned: Boolean` (`근거 지워짐`), `targetDeleted` (derived: `대상 지워짐`).

**Trash entries**, **check `맞음` marks**, **recorder 연결** and **scene `등장` lists** are owned by their areas; cast reads them for counts.

### 3.3 Derived, never stored

Appearance bars and `마지막 등장` (CST-71) and the `오래 안 나옴` order (CST-69) (0.2.0), age (`나이`), `첫 등장`, `보이스 진행`, `아직 등장 전`, display-name ranges (`(1-8까지)`, `1-9 공개`), `예정` / `지금 시점보다 뒤` flags, archive character counts, kind usage counts, reference counts on the impact sheet / trash row / check row, `N명 중 M명`, 호칭 chips on the 말투 tab, the target card's `이 항목으로 잇는 인물`.

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
- **(0.2.0)**
  - `cast.json` characters carry `speechSummary` and `secretTabLocked`.
  - `project.json` tags carry `color` and `deletedAt`. A trashed tag is written like any tag, since it is in the project, not in the trash export.
  - Voice lines, archive documents and voice codes are always written, whatever the `게임 제작 도구` value (G-4).
  - `AppSettings` fields are never in `.civilight`.

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

### 4.6 0.2.0 rules (JVM unless marked "emulator")

**Fixture P (appearances, CST-69, CST-71).**
- Episodes in manuscript order: `1-1`, `1-2`, `1-3` (written) and `2-1` (no content; after the 집필 선). Scenes: `1-1 씬 1`, `1-1 씬 2`, `1-2 씬 1`, `1-2 씬 2` (a hidden scene), `1-3 씬 1`.
- Characters in list order, all untagged: 리엔, 하르트, 세라, 오웬.
- 리엔 speaks in `1-1 씬 1` and `1-1 씬 2`. A narration block in `1-2 씬 1` holds `[리엔]은 웃었다` (an `@` link). 리엔 is on the `등장` list of `1-3 씬 1` without speaking.
- 하르트 speaks in `1-3 씬 1`. A memo block in `1-1 씬 1` holds an `@하르트` link.
- 세라 speaks only in a block of `1-2 씬 1` that is in the trash.
- 오웬 speaks only in `1-2 씬 2` (the hidden scene).

**TC-N47-01 Bars and last appearance, hidden lock closed**
- Then 리엔's bars are `1-1 2 · 1-2 1 · 1-3 0 · 2-1 0` and `마지막 등장 | 1-2`. The silent `등장` in `1-3` does not count.
- Then 하르트's bars are `0 · 0 · 1 · 0` and `마지막 등장 | 1-3`. The memo link does not count.
- Then 세라's bars are all 0 and `마지막 등장 | —` (the trashed block does not count).
- Then 오웬's bars are all 0 and `마지막 등장 | —` (the hidden scene is not counted while locked).
- When the lens is set to `1-1` on the cast head → Then every value is the same (D23).

**TC-N47-02 Hidden lock open**
- When the hidden group is unlocked → Then 오웬's bars are `0 · 1 · 0 · 0` and `마지막 등장 | 1-2`.

**TC-N47-03 Sort `오래 안 나옴`**
- Locked → Then the order is 리엔 (last `1-2 씬 1`), 하르트 (`1-3 씬 1`), then 세라 and 오웬 (no appearance, in list order).
- Unlocked → Then 리엔, 오웬 (`1-2 씬 2`), 하르트, 세라.
- Then the stored `orderKey`s are unchanged.

**TC-N47-04 No episodes**
- Given a project with no episode → Then `Appearances.of` returns no bars, and the profile model hides the section.

**TC-N44-01 Several tags**
- Given 리엔 with tags `주연 · 등불 결사`, 세라 with `등불 결사`, and 9 more characters, 11 in all.
- Then the list has 리엔 in both the `주연` and the `등불 결사` sections, and the chip reads `전체 11`.
- When the chip `등불 결사` is picked → Then only that section shows, with 리엔 and 세라.

**TC-N44-02 Tag to the trash and back**
- Given tag `주연` on 리엔, 하르트 and 마르타, and on one gallery image.
- When `지우기` → Then the confirm lines are `쓰는 인물 3` · `이미지 1` and the footnote `휴지통 · 30일 · 되살리면 다시 이어짐`.
- When `휴지통으로` →
  - Then `주연` has `deletedAt` set, and all three characters and the image still hold its key;
  - the list has no `주연` chip or section;
  - 리엔 shows only under `등불 결사`, and 하르트 and 마르타 under their other tags or untagged;
  - the toast reads `태그 주연 지움 · 되돌리기`;
  - the trash filter `인물` lists `주연`.
- When `되살리기` → Then the section and chip return with the same three characters, and the image shows the tag again.

**TC-N44-03 Restore with the name taken**
- Given `주연` in the trash and a new live tag named `주연` → Then `되살리기` on the trashed one is disabled with `TODO_COPY_CAST_TAG_NAME_TAKEN`.
- Then creating a live tag named like a trashed one is allowed (`canName` looks at live tags only).

**TC-N44-04 Purge**
- Given `주연` trashed 31 days ago → When `PurgeActions.runDue` runs → Then `removeTag` has removed the key from the project, every character and every gallery image.

**TC-N44-05 Zero users still confirm**
- Given an unused tag → When `지우기` → Then the confirm shows `쓰는 인물 0` (and no `이미지` line).

**TC-U15-01 Name required**
- Given the 인물 create card → Then `만들기` and `만들고 열기` are disabled with `이름` = `""` and with `"   "`, and enabled with `세라`.

**TC-U15-02 만들기 from the list**
- Given the chip `주연` is active.
- When the card is filled with `이름 세라 · 표시 이름 ??? · 한 줄 소개 기록관 · 태그 등불 결사` and `만들기` is pressed → Then:
  - one character exists with those values (`displayNameBase = "???"`, no change record) and the next record number;
  - one undo step reverts it;
  - the chip returns to `전체`;
  - the list model marks the new key for the brief highlight;
  - the card is closed and no record is opened.

**TC-U15-03 Picker prefill and pick**
- Given the speaker picker with the query `세라` and no character matching it → Then the first row's label is `‘세라’ 새 인물` (icon `plus`).
- When it is tapped → Then the card opens with `이름` = `세라` and only `취소 · 만들기`.
- When `만들기` → Then the character exists and the picker returns its key as the picked speaker.

**TC-U15-04 Query that matches**
- Given the query `ㄹㅇ` (matches 리엔 by 초성) → Then the first row's label is `새 인물` (no quoted query).

**TC-U15-05 Order of top rows**
- Given the current block's speaker is 노아 (in the trash) → Then the picker rows start with the create row `새 인물`, then `지운 인물 · 노아 · 되살리기`.

**TC-U15-06 Read-only and merge**
- Given the example project (read-only) → Then the list model and every picker model have no create row, and `인물 추가` is absent.
- Given the merge target picker in any project → Then it has no create row.

**TC-U15-07 Tag from the field sheet**
- Given 리엔's `태그` sheet with the query `상층 귀족`, no tag of that name, and the card filled with `색` = a palette token → When `만들기` →
  - Then tag `상층 귀족` exists with that colour and is checked on 리엔;
  - one undo step removes both the tag and the check.

**TC-U15-08 Relation kind card**
- Given the relation sheet 리엔 · 세라 on the slot `사이`, when `+ 새 종류` → `이름` = `사형`, `양쪽에서 부르는 이름` = `사제`, `만들기` →
  - Then a `BETWEEN · PAIR` kind `사형 ↔ 사제` exists and is picked into the slot, with 세라 holding `사형` (CST-29).
- Given the same with `양쪽에서 부르는 이름` empty → Then the kind is `BETWEEN · SYMMETRIC`.
- Given the slot `리엔의 마음` → Then the card has no `양쪽에서 부르는 이름` row, and the kind is `MIND`.

**TC-S1-01 Lens default and scope**
- Given a 0.1.0 project whose stored lens state is `1-5` → When it opens → Then the cast and world heads read `최신 · 1-7`.
- When `1-5` is picked on the cast list head → Then the record head and the world head read `시점 · 1-5`, while home, script and 연표 still read values at 최신.
- When the project is closed and reopened → Then it is `최신` again, and the stored 0.1.0 lens state is unchanged (never written).

**TC-G-01 Tabs while off**
- Given a project with `게임 제작 도구` off and 리엔 with 24 voice lines → Then the profile tabs are `개요 · 말투 · 관계 · 비밀`, and the header table has no `보이스` row.
- Given the remembered tab is `게임 텍스트` → Then the record opens on `개요`.
- When the switch is turned on → Then 5 tabs, `보이스 18 / 24`, and the 24 lines equal to before.

**TC-G-02 Field settings while off**
- Then the box reads `고정 항목 · 6`, there is no `보이스` header switch, and the row sub has `고정 6`.
- When the switch is on → Then `고정 항목 · 7`.

**TC-G-03 Game data for G-3 (D6)**
- Covered by `00-domain-model.md` DM-0.2-01 (`GameData.present`). The cast cases to keep in that test: one voice line with empty text → true; one archive document in the trash only → true.

**TC-G-04 Search while off**
- Given the text `등불지기의 일지` only in an archive document title and body → Then search returns no hit while the switch is off, and one hit while it is on.

**TC-N43-01 Speech summary**
- When the summary is saved as `  반말 ·\n짧게 끊음 ` → Then the stored value is `반말 · 짧게 끊음` (trimmed, no line break), and the reference panel line reads `말투 | 반말 · 짧게 끊음`.
- When it is cleared → Then the stored value is null, the 말투 tab row reads `한 줄 요약 | —`, and the panel has no `말투` line (the prose is not shown).

**TC-N43-02 Plain save word**
- Then the `한 줄 요약` sheet's primary button is `저장`. A copy test fails if any sheet without history uses `이 값 고치기` (U-10).

**TC-N43-03 Secret tab gate**
- Given `secretTabLocked = true` and the gate `LOCKED` →
  - When `select(비밀)` → Then the gate is `PROMPTING`, and the tab content is not composed.
  - When `cancel` → Then the gate is `LOCKED`, and the selected tab is the previous one.
  - When `select(비밀)` and `confirm` → Then `OPEN`.
  - When `select(개요)` and `select(비밀)` again → Then `PROMPTING` again.
  - When `background` while `OPEN` → Then `LOCKED`.
  - A configuration change while `OPEN` keeps `OPEN`.

**TC-N43-04 Turning the lock off**
- Given `secretTabLocked = true` → When the switch is turned off and the prompt is cancelled → Then it stays true. When confirmed → Then false.
- Given a device with no screen lock → Then the switch is disabled with `TODO_COPY_CAST_SECRET_LOCK_NO_DEVICE`.

**TC-N43-05 Search with the lock on**
- Given 하르트's secret field `정체 | 상층 경비대 첩자` and `secretTabLocked = true` → Then searching `첩자` returns no hit for 하르트. With the lock off it behaves as before (CST-27).

**TC-U16-01 Card view settings**
- Given fresh `AppSettings` → Then `castCardStyle = FAN_DECK`, `castToggle` is absent, and the cast list shows `부채꼴 덱` with the toggle cells `목록` · `부채꼴 덱`.
- When the toggle is set to `목록` and the app restarts → Then the list shows.
- When `목록` is picked in settings → Then no toggle is drawn and the list shows (`ShownView.of`, DM-0.2-05).

**TC-U16-02 What a 3D view holds**
- Given TC-N44-01 and the chip `전체` → Then the 3D view model lists 11 cards, 리엔 once.
- Given 0 characters → Then the screen model is the empty state, not a 3D view.

**TC-U16-03 Reduce motion (emulator, plus a JVM check of the motion spec)**
- With the system `애니메이션 줄이기` → Then the tilt input is ignored, and the view uses fades only. `MotionLint` passes.

**TC-020-COPY Copy lint**
- Every constant of §8.1 passes `CopyLint`: no 해요체, no em dash, ` · ` separators. Each `TODO_COPY_…` constant is listed under the lane's Requests (DECISIONS §3).

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

**(0.2.0) Changes to the rows above.**
- S1: no `+` in the head, `＋ 새 인물` first row, a small lens chip, the U-16 toggle and `보기`, no mic.
- S3: superseded by S42.
- S5: no mic, and the `보이스` row only with the switch on.
- S5 and S6: 4 tabs while the switch is off.
- S7: only with `게임 제작 도구` on.
- S8: gains the lock row (S47).
- S25: the `보이스` rows only with the switch on.

| # | Screen / sheet / state (0.2.0) | Boards | Cover (390) | Inner (800) | Components |
| --- | --- | --- | --- | --- | --- |
| S39 | 만들기 카드 · 인물 (list and picker modes) | `scr_icon_variants.py` (row only) | centred floating card over a dimmed list, above the IME | centred card over both panes | shared create card (`core:designsystem`), image slot, text fields, tag chips, `Buttons` |
| S40 | 만들기 카드 · 태그 | none | same | same | create card, text field, swatch row |
| S41 | 만들기 카드 · 관계 종류 | none | same | same | create card, text field, `<이름> ↔ [ ]` row |
| S42 | 카드 보기 · 3D (7 options) | `U16CastDeck`, `U16CastRing`, `scr_u16_cards_b.py`, `scr_u16_cards_c.py` | full list area under the chips | list pane (A) or 560 column (B) | 3D primitives through `CivilightMotion`, icon buttons `이전 · 다음`, `열기` / `뒤집기` |
| S43 | 카드 보기 바꾸기 sheet | `U16SetHeaderToggle` (서재 version) | bottom sheet | popover | `RadioRow` ×7, footer line |
| S44 | 설정 › 보기 › 카드 보기 | `U16SetCardView` | settings screen (`feature:library`) | same | radio rows with live thumbnails, `SwitchRow` |
| S45 | 개요 · 등장 막대 | none | section under the field table | same in the pane | bar row (horizontal scroll inside), `.row` |
| S46 | 한 줄 요약 sheet | none | bottom sheet | popover | text field, primary `저장` |
| S47 | 비밀 · 이 탭 잠그기 + prompt | none | switch row at the tab head; system prompt | same | `SwitchRow`, system biometric prompt |
| S48 | 태그 지우기 confirm · 휴지통 줄 | none (`RCDeleteImpact` style lines) | `ConfirmDialog`; trash row | same | `ConfirmDialog`, trash row |

**Required but not drawn** (build from this spec; layout open): S8 비밀 tab; S16 relation create/edit sheet; record ⋯ menu (`숨기기 · 지우기 · 합치기`); character create sheet; tag editing; field filter chips and sort control (CST-03); bundle picker sheet (`기본 · 게임 프로필 · 비어 있음 · 다른 프로젝트에서`); field edit sheet for existing fields (option editor, type change preview, hide, delete confirm `값이 있는 인물 12명`); kind edit / switch preview; multi-select delete; locked item-hide states for the speaker field, relationship map and other characters' relation rows; item toast `인물 노아 지움 · 참조 20 · 점검 센터 · 되돌리기`; restore report toast; same-name `◇` row; archive document editor; voice line state control; `고른 관계` sheet on inner when opened from a record.

---

## 6. Open questions

Only real contradictions or gaps between sources. Each lists both sides and the default this spec uses until decided.

1. **Default relation kinds.** Rule 13 §3: 대칭 `동료 · 형제자매 · 배우자 · 연인 · 친구 · 거래 상대 · 적` (7), 짝 `부모 ↔ 자식 · 스승 ↔ 제자 · 상관 ↔ 부하 · 선배 ↔ 후배` (4), 마음 `신뢰 · 의지 · 동경 · 호감 · 보호 · 경계 · 불신 · 질투 · 증오 · 죄책감 · 무관심` (11). Board `RCRelTypes` and `ProjSettings` (`사이 12 · 마음 11`): 대칭 `동료 · 형제자매 · 친구 · 거래 상대 · 연인 · 소꿉친구`, 짝 adds `보호자 ↔ 피보호자 · 주인 ↔ 하인`, 마음 has `존경 · 연모` instead of `호감 · 불신`. `ChangeCreate`/`CanvasCLabel` still offer `불신`. Default here: the rule 13 list.
2. **Merge sample.** Rule 20 §6: `시장 꼬마` → 노아, overlap `오웬 거래 | 보호`, `빈 칸 채움 · 출생 · 성격 · 2칸`. Board `RCMerge`: `노아(시장)`, overlap row `오웬 · 사이 · 거래 상대 | 보호자`, `다른 칸 · 3` (나이 · 소개 · 소속), `같은 항목 6`. Behaviour is the same; sample data only (user said sample issues can wait).
3. **Editing at a past lens.** Rule 11 §1: the same two buttons (`이 값 고치기 · 여기서부터 바뀜`) at a past lens; the banner `고치면 1-5부터 적용` covers only the second meaning. Board `LensEdit` still shows only the banner and `저장 시 ◆ 변화 기록 · 1-5 #—`; `FieldHistory` buttons are `시점 추가 · 1-3 값 고치기`. Default: rule 11 (two buttons everywhere), keep the banner.
4. **(0.2.0: closed by N-43)** The per-block display name is a script `속성` row `이 블록만 · 표시 이름` (manuscript `경비병 (하르트)`, game output `경비병` with the original speaker's voice ID). The character-level history of CST-11 is unchanged. **Display-name model.** Character-level `표시 이름` with history (rule 12 §1, rule 11 table) vs the script `속성` panel's per-block `표시 이름` field (`??? · 1-9에서 공개`). Is there a per-block override independent of the character history? Also the list shows the real name as title with a note while the card view shows `???` as title. Default: one character-level history; the 속성 panel shows it read-only with a link to the record; list/card as drawn.
5. **보이스 진행.** Header `18 / 24` and check `보이스 6/24 비어 있음` imply filled = not empty (쓰는 중 counts); "진행" could mean `녹음 대본 완료` only. Default: not-empty count.
6. **인물 표 CSV.** `인물 항목` §8 leaves it undecided. Default: not built (CST-51, P2).
7. **Auto-filled 생일.** "출생 날짜에 월 · 일까지 있으면 생일은 자동으로 채워진다": stored copy (editable, can diverge) or derived (read-only while 출생 has m/d)? Default: derived and read-only while 출생 has month and day.
8. **Tabs vs one document.** DS/planning: five tabs. Board `Relations` (tab `관계` selected) shows `Ⅱ 말투` and `Ⅲ 관계` numbered sections on one scroll, while `인물 항목` §4 numbers 긴 글 sections in 개요 as `Ⅰ 개요 · Ⅱ 성격 · Ⅲ 과거`. Default: separate tab pages; Roman numerals only for 개요's long-text sections.
9. **(0.2.0: closed, CST-72)** Prose body plus a separate one-line `한 줄 요약`; the panel shows only the summary. **말투 structure.** Planning: `1인칭 · 어미 · 말버릇` (could be three fields); board: one prose paragraph; `참고` panel shows a one-line `말투 | 반말 · 짧게 끊음` with no stated source. Default: prose body + a separate optional one-line summary used by the panel [needs a decision and a board].
10. **(0.2.0: closed, CST-73)** `이 탭 잠그기` on the tab head asks for the device lock on every opening. **비밀 tab hiding.** Planning calls 비밀 "항목 숨기기 대상" and rule 19 §5 says hiding also covers unpublished settings, but hiding is defined per item (character), not per tab. Can the 비밀 tab be locked on its own? Default: no separate lock; hide the character.
11. **Record number.** `인물 기록 007 / 012 / 014` is shown everywhere but no rule says how it is assigned (creation order? reused? renumbered?). Default: creation order per project, never reused, never renumbered (like block numbers, rule 12 §5).
12. **Voice code per character.** Voice ID format `HRT_0107_043` needs a speaker code and the name tag stores it, but no source says where a character's code is set or how it is generated. Default: a `보이스 코드` row in the record's `속성`-like area [needs a board]; auto-suggest is a guess and is not used.
13. **(0.2.0: multi-tag part closed, CST-68)** A character appears under each of its tags; `전체` counts it once. The rest of this question is unchanged. **List order, multi-tag grouping, filter/sort UI.** Grouping is by tag, but a character can have several tags (리엔 is 주연 and in 등불 결사; the boards list her only under 주연; 세라 appears under 등불 결사 in the list and under 하층 구역 in cards). Default order inside a group is not stated; field filter/sort controls are not drawn. Default: a character appears under its first tag; order = `orderKey` (creation order); sort/filter controls in a header `⋯` sheet [PROPOSED].
14. **Relationship map nodes and layout.** Which characters appear (`11명 중 5명` on inner, `11명 중 4명` on cover) and how M is counted is not defined; node positions are not defined (auto layout? saved like canvas nodes?). Default: nodes = characters with at least one drawn line at the lens after filters; positions saved per project like canvas nodes, auto-placed on first view.
15. **Middle label for pair kinds on the map.** An undirected line with 사이 `선배 ↔ 후배` is labelled `선배` (리엔–세라) while 하르트–마르타 is labelled `부하`; which side's name the middle label shows is not defined. Default: `선배 ↔ 후배` with the role names placed toward their holders.
16. **Deleting a kind when a description exists.** Rule 13 §3: "지우면 그 종류를 고른 관계는 설명으로 남고 ◇". If the slot already has a description (리엔 → 하르트 `경계 · ‘불빛을 아는 낯선 사람’`), how are the two texts combined? Default: keep `kindFallbackText` separately and show `경계 · 불빛을 아는 낯선 사람 ◇`.
17. **‘꼬마’: description or 호칭.** Rule 13 §2 example table puts `보호 · ‘꼬마’` in the mind column (like a description); §4, §6, memory and `CSheetRef` (`리엔을 | ‘꼬마’`) treat `꼬마` as 하르트's 호칭 for 리엔. Default: 호칭.
18. **Reference panel layout and block basis.** Rule 13 §4: the partner's mind goes on a smaller line below (`← 경계`); boards `CSheetRef`/`WideRef` put `→ 보호 · ← 경계` on one line. The same boards select #043 yet show the post-#043 state, while rule 11 §2 says #043 shows `만나기 전 → … ◆ 여기서`. Default: rule text (script area renders).
19. **`예정` vs `지금 시점보다 뒤`.** Older boards (`Relations`, `FieldHistory`, `CastMap`, `CastMapC`) label 1-9 / 2-5 records at lens 1-7 or 1-5 as `지금 시점보다 뒤`; rule 11 and `RTPlanned` call anything past the 집필 선 `예정`. `RTPlanned` also shows `예정 · 1-9 · 하르트 → 신뢰` in 리엔's record where rule 13 has 리엔 → 신뢰 (should read `→ 신뢰`). Default: `예정` past the 집필 선; `지금 시점보다 뒤` for written records after a past lens.
20. **Year at a lens in `연도만` mode.** Age = lens year − birth year, but how a scene's 때 (days since story start, possibly ranges) maps to a year when only years exist (365-day years? year changes only by explicit events?) is not defined. Default: story start year + floor(days ÷ 365); a range uses its earliest day.
21. **`항목 더하기` matching.** "없는 항목만 더한다": matched by field name, or by the bundle field's identity (a renamed `신장` → `키` would be re-added under name matching)? Default: by `bundleOrigin` when present, else by name.
22. **Name tags, merge records and trashed items in `.civilight`.** Rule 20 defers name-tag and merge-record storage to `저장 파일 양식`, which does not list them. Also the file excludes the trash by default, so an exported project with 노아 in the trash has dotted references to a key that is not in the file. Default: always write name tags for trashed and permanently deleted items referenced by live data, plus merge records, in `cast.json` (optional feature `cast.tombstones` [PROPOSED]).
23. **(0.2.0)** The card side is now the U-16 views (CST-67). The default below is kept: `관계도` overrides whichever view is chosen. **Card view vs `목록 | 관계도`.** `Cast` has the segmented `목록 | 관계도`; `CastCards` has the header `목록 | 카드` toggle and no segmented control. How the two controls combine (is the map reachable from card view?) is not drawn. Default: both controls on both views; `관계도` overrides the list/card choice while selected.
24. **Entry points not drawn.** The record ⋯ menu (숨기기 · 지우기 · 합치기) and the multi-select needed for "여러 개를 한꺼번에 지움" have no board; long-press is not allowed (no hidden gestures). Default: record ⋯ menu; a visible `고르기` action in the list header ⋯ for multi-select [PROPOSED].
25. **Header table vs `머리` flag.** `RCFields` marks 종족 `목록 · 필터` (not `머리`), but `RCProfileAdd` and `인물 항목` §9 show 종족 in the record's header table; the `항목` list for fields without `머리` (§4 "개요 탭의 ‘항목’ 목록") is not drawn. Default: header = `머리` fields only; others in the `항목` list.
26. **`연결` overloaded.** Rule 23 renamed the recorder's `붙일 곳` to `연결`, which is also the field type name `연결`. Vocabulary memo lists this as open. Default: keep both as specified.
27. **Where `여기서부터 바뀜` at 최신 puts the record.** Rule 11 §1: "집필 선 위치(1-7 씬 3)에 ◆". Board `RTEditTwo` and the confirmed 2026-11-22 note: "집필 선 1-7 씬 3 뒤" / "right after the 집필 선". Rule 12 §1's anchor table lists only the past-lens case as a `시점` anchor, which applies from the *start* of the unit (that would back-date the change to the start of 1-7 씬 3), and anything *after* the 집필 선 is `예정` (would not show at 최신). Default [PROPOSED]: new anchor `SceneEnd(1-7 씬 3)`, applied at 최신, not before the end of that scene (CST-10, TC-11-03). Needs the lens/change-record area to adopt it.
28. **`▲` from profile game text.** Rule 21 §4 table: profile voice lines and archive documents go to the game but whether their `@` links raise `▲ 공개 전 사용` is "미정, 위치 없음" (no story position). Default: no `▲`, links listed under `원고 밖` with tag `게임 텍스트`; an archive document's `… 이후 열림` point could serve as its position if decided later.
29. **Kind switch and 호칭-only export are under-specified.** Rule 13 §3 says the `사이 / 마음` and `대칭 / 짝 이름` switches may change while in use and show their effect first, but not what happens to slots already holding that kind (a 사이 value has no side; a mind has one). Rule 13 §9 offers "호칭만 내보낼지 선택 (기본 끔)" for game-engine JSON, but LIB-45's JSON shape has no key for it. Default until decided: the preview is shown and nothing is applied without confirmation (TC-13-20); the JSON option is listed but its shape is left to LIB-45 (TC-13-21).

---

## 7. 인물 항목 편집 보강 · 인물 템플릿 (U-09, added 2026-11-23)

**User report (0.1.0 QA, U-09).** "프로젝트에서 캐릭터 설정으로 사용할 수 있는 템플릿 제작 옵션이 없음 (사실상 기존의 것을 강제로 활용해야함)". Today a project can only start from the four built-in choices (`기본 · 게임 프로필 · 비어 있음 · 다른 프로젝트에서`). The user cannot keep a field set of their own and reuse it.

**Sources read (2026-11-23).** This file §2 C (CST-19 … CST-27), §3.1, §4.3 · `40-world.md` WLD-06 · `60-library-security-files.md` LIB-12, LIB-57, LIB-63 · `55-gallery.md` GAL order controls (§3.4 lines `앞으로 · 뒤로 · 맨 앞`) · `80-design-system.md` DS-62, DS-63 and `CopyLint` · `docs/status/DECISIONS.md` · memory character-fields, vocabulary, rules-19-23, feedback-* · code `feature/cast/settings/*`, `domain/cast/field/*`, `feature/library/newproject/*`, `core/model` `FieldDef`, `AppSettings`, `core/store` `SettingsStore`, `AppStateStore`, `feature/library/backup/BackupRunner`.

**Confirmed rules this section builds on (memory character-fields, 2026-11-21).**
- Fixed fields are only `이름 · 표시 이름 · 한 줄 소개 · 이미지 · 태그 · 첫 등장 · 보이스 진행`. Every other field is a project field of one of 9 types.
- Fields are defined in `프로젝트 설정 › 인물 항목` and from a profile's `+ 항목` (`모든 인물에` · `이 인물만`).
- Start bundles are `기본 · 게임 프로필 · 비어 있음 · 다른 프로젝트에서`, chosen in new-project step 4.
- The same mechanism serves 장소 · 용어 · 세력.
- Empty fields are hidden in view.
- Renames and type changes keep values (anything that cannot convert stays as text with `◇`).

The template rules in §7 are new: none of the sources states them, so every template rule is `[PROPOSED]` unless it repeats a rule above.

### 7.1 What is built today (code audit, 2026-11-23)

| Part of the `인물 항목` editor | State in code | Where |
| --- | --- | --- |
| List screen with `인물 · 장소 · 용어 · 세력` tabs, `고정 항목` box, `첫 등장` · `보이스` header switches, `프로젝트 항목 · N` rows, `+ 항목`, `시작 묶음` row | built | `feature:cast` `settings/FieldSettingsScreen.kt`, `domain:cast` `field/FieldSettings.kt` |
| `+ 항목` sheet (9 types, type options, `보이는 곳`, `비밀 항목`, `내보내기 이름`, same-name `◇`) | built, settings and record versions | `settings/AddFieldSheetContent.kt`, `AddFieldModel.kt` |
| Rename, export name, unit (`환산하지 않음`), options (rename in place, `빼기` listing holders first, `+ 보기`), `직접 쓰기` | built | `settings/FieldDefEditScreen.kt`, `FieldDefEditModel.kt` |
| Type change limited to the CST-23 pairs, with a `◇ 바꾸지 못한 값 N` preview before anything changes | built | `FieldDefEditModel.preview` / `convert`, `domain:cast` `FieldConversion` |
| Reorder | **drag only**: a vertical drag on the grip (`detectVerticalDragGestures`). No button and no accessibility action, so a TalkBack user or anyone who cannot drag cannot reorder | `FieldSettingsScreen.kt` `FieldReorder` |
| `숨기기` (definition hidden, values kept, `숨김` mark on the row) | built | `FieldDefinitions.setHidden` |
| `지우기` to the trash | built, but the confirm shows only `값이 있는 인물 12명`. The count word is `명` for every kind (`값이 있는 장소 4명`), and nothing warns that a `계산` field depends on the field being deleted | `FieldDefinitions.deleteSummary`, `ItemTrash.deleteFieldDef` |
| `계산` whose source field is in the trash | **probable bug**: `FieldReader.readAge` looks the source up with `doc.index.fieldDef(...)`, which also returns trashed definitions. `storedAt` then reads the base value from the entity's `fields` map with no liveness check (only change records go through `liveness.isLive`), so `나이` most likely keeps calculating from a trashed `출생`. TC-F-31 pins the rule either way | `domain:cast` `field/FieldReader.kt` |
| `모든 인물에 넣기` for an `이 인물만` field | built | `FieldDefinitions.promoteToAll` |
| Empty fields hidden in view (`빈 항목 N · 보기`) | built (TC-F-13) | `Profile.kt` |
| Default value per field (pre-filled for new characters) | **not built and not in any rule**. See CST-65 and §7.8 Q30 | — |
| `항목 더하기` sheet (`기본 · 게임 프로필 · 비어 있음 · 다른 프로젝트에서`, adds missing only, preview of added names) | built | `settings/BundleSheet.kt`, `BundleModel.kt`, `domain:cast` `FieldBundles` |
| New-project step 4 bundle row and picker | built, character fields only (`ProjectCreator` applies `FieldBundles` with the default kind) | `feature:library` `newproject/BundlePickerSheet.kt`, `ProjectCreator.kt` |
| A user-made field set that lives outside projects (template) | **missing**. This is U-09 | — |

### 7.2 Features

#### CST-53 · 인물 항목 편집기 · 보강 범위 (editor deltas) — P0
- CST-19 … CST-24 stay as written. This feature adds only what §7.1 shows missing: visible reorder controls (CST-54), a fuller delete confirm (CST-55), and the `계산` source fix (CST-55).
- One editor serves all four kinds. Every change in §7 applies to the `인물 · 장소 · 용어 · 세력` tabs alike, and to the template editor (CST-59).
- Default values are out of scope (CST-65).

#### CST-54 · 순서 · 앞으로 · 뒤로 (visible reorder controls) — P0 `[PROPOSED]`
- The trailing text of the section label `프로젝트 항목 · N` becomes a text button `순서 바꾸기`. It replaces the static note `끌어서 순서`; the button is hidden in read-only projects.
- Tapping it turns on **order mode**:
  - Each row shows the grip (44 hit), the name, a mono position `3/7`, and two buttons `앞으로` · `뒤로` (44 hit each).
  - `앞으로` is disabled on the first row and `뒤로` on the last.
  - The chevron, `+ 항목` and the `시작 묶음` row are hidden.
  - A note under the section label reads `끌기 · 앞으로 · 뒤로`.
  - The section label's trailing button reads `완료`; tapping it ends order mode.
- Order mode also ends when the screen is left or the tab is switched.
- Dragging the grip keeps working, both in and out of order mode (board `RCFields`).
- Each button tap moves the field one place and is one undo step, labelled `순서 바꾸기` (`CastUiCopy.REORDER`).
- Positions count every live definition of the kind, including hidden ones (`숨김` rows are listed and movable). `이 인물만` fields are not listed and keep their order key.
- Accessibility: the grip also exposes the custom actions `앞으로` and `뒤로` (same effect as the buttons), and its content description stays `row.gripLabel`.
- Motion: neighbours glide with the existing `civilightItem(placement = CivilightMotion.standard)`, so no new motion is added (MotionLint).
- Writes go through `FieldSettingsModel.move` (`FieldDefinitions.move`, one order key between the new neighbours). In `domain:cast` only the copy changes: `FieldSettings.screen` gains `reorderAction = "순서 바꾸기"` and keeps `reorderNote` for the order-mode note (additive). This replaces board copy (`RCFields` `끌어서 순서`), see §7.8 Q35.

#### CST-55 · 지우기 · 영향 줄 (field delete confirm) — P0
- The confirm (`FieldDefEditScreen` `Pending.Delete`, DS `ConfirmDialog`) lists one line per fact. Lines whose count is 0 are left out, except the first:
  1. `값이 있는 <kind> <n><unit>`. The unit is `명` for 인물 and `개` for 장소 · 용어 · 세력 `[PROPOSED unit]`. Examples: `값이 있는 인물 12명`, `값이 있는 세력 3개`.
  2. `◆ 변화 기록 <n>`: live change records whose target is this field.
  3. `계산 항목 <name> · 직접 넣기로`: one line per live `계산` field whose source is this field (`계산 항목 나이 · 직접 넣기로`).
  4. `필터에서 빠짐`: shown when `inFilter` is on.
  5. The footnote `휴지통 · 30일 · 되살리면 다시 이어짐` (`TrashCopy.SHEET_FOOTNOTE`).
- Buttons are unchanged: `취소` · `휴지통으로`.
- **`계산` source rule** (fixes the §7.1 bug): a `계산` field whose source definition is trashed, purged or missing behaves exactly as if the source were empty.
  - `나이` is read from its own typed value (CST-25) and shows no `계산` derivation.
  - Restoring the source brings the calculation back.
  - No value is written while the source is in the trash.
- Domain: DECISIONS §1 gives delete-impact counts to `domain:trash`, but a field definition has no references there, only values. This extends the existing `domain:cast` `deleteSummary` (already the source of the line) rather than adding a second engine: an additive `FieldDefinitions.deleteImpact(doc, defKey): FieldDeleteImpact` returns `holders`, `records`, `calcDependents: List<String>` and `inFilter`, plus `lines()` for the copy. `deleteSummary` remains for compatibility, with the per-kind unit fixed. `FieldReader.readAge` checks `deletedAt == null` on the source.

#### CST-56 · 인물 템플릿 · 무엇이 담기나 (what a template holds) — P0 `[PROPOSED]`
- A **template** (`템플릿`) is a named, app-level field set. It lives outside every project, so every project can use it. It holds field **definitions** of any of the four kinds and never holds values, characters, change records, relation kinds, tags or project settings.
- Per field, a template keeps:
  - name, kind, type, export name;
  - `보이는 곳` (`머리 · 목록 · 필터`) and `비밀 항목`;
  - the type settings: `여러 개` (짧은 글), unit, option labels in order, `직접 쓰기`, date precision, `연결` target kind, and the `계산` source as a key local to the template;
  - an **origin id**.
- It never keeps the definition's `숨기기` state. A hidden field that is taken into a template becomes a normal field there.
- Never included:
  - `이 인물만` fields (`scopeCharacterKey != null`);
  - trashed fields;
  - the fixed fields, which every project has anyway.
- Origin id: a field copied from a built-in bundle keeps its bundle id (`basic.birth`). Any other field gets `tpl:<template field key>` the first time it enters a template, and keeps it from then on.
- Per field, a template also keeps `sourceDefKeys`: the opaque keys of the project definitions it was saved from. They are used only for matching (CST-57, CST-60), so the source project needs no write and a rename there still matches.
- A template stores no project name and no project key.
- Built-in bundles (`기본 · 게임 프로필 · 비어 있음`) are not templates. They stay read-only in code (`FieldBundles`), and the words for them stay `시작 묶음` (CST-24).

#### CST-57 · 템플릿으로 저장 (save the project's fields as a template) — P0 `[PROPOSED]`
- **Entry.** `프로젝트 설정 › 인물 항목`, a new row under the `시작 묶음` row: title `템플릿으로 저장`, sub `프로젝트 밖에 보관 · 새 프로젝트 · 항목 더하기에서 사용`, chevron. The row is hidden in the read-only example project (its fields are the 게임 프로필 bundle anyway).
- **Sheet** `템플릿으로 저장` (cover: bottom sheet; inner: side popover), with `닫기`:
  - `이름` input, focused and empty. `저장` stays disabled while the name is blank. If a live template already has the same name (NFC, trimmed), `◇ 같은 이름 템플릿 있음` shows under the input; saving is still allowed.
  - `어디에`, two radio cards:
    - `새 템플릿` (default);
    - `있는 템플릿 바꾸기` with sub `그 템플릿 항목을 이 목록으로`. Choosing it shows the visible templates as radio rows; the picked one's name fills `이름`.
  - Section `담을 항목 · <checked n>`, grouped by kind in tab order. Each kind has a group label `인물 · 11`, and kinds with no live project fields are left out. Rows are checkboxes `<name>` with sub `<종류> · <detail>` (the settings row sub without the places).
    - All rows start checked, except definitions with `숨기기` on: they start unchecked and carry the mark `숨김`.
    - Unchecking a `계산` field's source shows `◇ 나이 · 출생 없이 · 직접 넣기` under the `계산` row. That template's `계산` then has no source.
  - Line `이 인물만 항목 <n> · 빠짐` when such fields exist.
  - Line `항목 정의만 · 값 없음`.
  - **Hidden project** (`protections.hidden`, which R-18 projects have by default; **(0.2.0, S-4, D34)** ratings are no longer shown or picked, and the rule keys only on `protections.hidden`, which a 0.1.0 R-18 project keeps): a switch `숨긴 템플릿` with sub `지문으로 열 때만 보임` starts **on**. Turning it off shows `◇ 잠가도 보임`. In other projects the switch starts off and has no notice. See CST-62.
  - Primary `저장`, enabled when the name is not blank and at least one field is checked.
- **Effect.**
  - `새 템플릿` adds a template.
  - `있는 템플릿 바꾸기` replaces that template's field list with the checked fields. A checked definition whose key is in a template field's `sourceDefKeys`, or whose `bundleOrigin` equals a template field's origin, keeps that template field's key and origin, so projects that took it earlier still match it. Other checked definitions become new template fields.
  - Either way the toast reads `템플릿 <name> 저장`.
  - Nothing in the project changes and nothing enters the project's undo stack.
- Saving writes nothing to the project: neither `bundleOrigin` nor anything else. Matching back to the source project goes through `sourceDefKeys`.

#### CST-58 · 설정 › 인물 템플릿 (template list) — P0 `[PROPOSED]`
- **Entry.** App `설정` (LIB-63): a new section `인물` with the row `인물 템플릿`.
  - Sub: `템플릿 <n>`, where n counts visible live templates only (CST-62), or `없음`.
  - The row has a chevron.
- **Screen.** Head `설정`, title `인물 템플릿`.
  - Section `템플릿 · <n>`. Rows are ordered by the template's order key (newest at the end). Each row:
    - title: the name;
    - sub: per-kind counts of non-empty kinds, `인물 11 · 장소 2 · 세력 1`, or `항목 없음`;
    - right: `고치기 ›`;
    - tap → CST-59.
  - Row `+ 템플릿` (plus icon) → sheet `템플릿 만들기`:
    - `이름` input;
    - `시작` radios: `비어 있음` (default) · `기본` · `게임 프로필` · `템플릿에서` (expands to the visible templates) · `프로젝트에서` (expands to the live projects in 서재, hidden ones only while the hidden group is open);
    - when `시작` is `프로젝트에서` and the project is hidden, the `숨긴 템플릿` switch of CST-57 appears, on;
    - primary `만들기` creates the template and opens CST-59.
  - When the hidden group is locked and hidden templates exist: one `LockRow` `숨긴 템플릿` · `지문으로 보기`, with **no count**. Unlocking uses the same biometric flow as 서재's hidden group (rule 19). While it is open, hidden templates are listed in place with a small lock mark.
  - Row `지운 템플릿 · <n>`, only when n > 0 (n counts visible trashed templates). It leads to a list whose rows read `<name>` · `<M월 D일> 지움 · 30일 뒤 삭제`, each with `되살리기`.
  - Empty state (no visible live templates, no lock row): outlined card `템플릿 없음`, sub `프로젝트 설정 › 인물 항목 › 템플릿으로 저장`, button `템플릿 만들기`.
- Reorder of templates: not in v1 (rows follow creation order).

#### CST-59 · 템플릿 고치기 (edit a template outside any project) — P1 `[PROPOSED]`
- Screen head `인물 템플릿`, title = the template name.
- Top: the `이름` field. It is the template's rename (`이름 바꾸기`), saved as you type, and a blank name is not saved.
- Then the same list as CST-19, drawn by the same composable:
  - tabs `인물 · 장소 · 용어 · 세력`;
  - `고정 항목 · 7` box (reference only); **(0.2.0)** in the template editor it reads `고정 항목 · 6` without `보이스 진행`, because a template is not tied to a project's `게임 제작 도구` and holds no game data (§7.9);
  - no `첫 등장` · `보이스` header switches (project settings);
  - `템플릿 항목 · <n>` rows with grip and CST-54 order mode;
  - `+ 항목` (same sheet as CST-21, `어디에` fixed to all items of the kind);
  - no `시작 묶음` row and no `템플릿으로 저장` row.
- Row tap opens the same field screen as CST-23, with these differences, since a template has no values:
  - **Type** may change to any of the 9 types, because there is nothing to convert. Choosing `연결` asks for the target kind; choosing `계산` asks for the source (a `날짜` field of the template, or `없음`). No `◇` preview is shown.
  - Options: `빼기` removes at once, because no one holds an option.
  - No `숨기기` and no `모든 인물에 넣기`.
  - The bottom ghost button is `빼기`, not `지우기`. It removes the field from the template after a confirm: title = field name, line `이 템플릿에서만 · 프로젝트 그대로`, buttons `취소` · `빼기`. Any `계산` that used the removed field as its source loses its source.
- `숨긴 템플릿` switch (sub `지문으로 열 때만 보임`). Turning it off shows `◇ 잠가도 보임`.
- Bottom ghost button `지우기`. Confirm: title = name, lines `항목 <n>` · `프로젝트에 넣은 항목은 그대로` · `지운 템플릿 · 30일 뒤 삭제`, buttons `취소` · `지우기`. Toast `템플릿 <name> 지움 · 되돌리기`.
- Every change writes to the template store at once. There is no project undo stack here; the only undo is the toast after `지우기`.
- Changing a template never changes projects that took fields from it earlier.

#### CST-60 · 항목 더하기 · 템플릿 (apply a template to a project) — P0 `[PROPOSED]`
- **Entry**: the existing `항목 더하기` sheet (CST-24, `BundleSheet`, opened per kind tab from the `시작 묶음` row). One verb is used for one meaning: applying a template is `항목 더하기`, like applying a bundle. There is no separate `불러오기`.
- The sheet keeps the four built-in rows in their confirmed order. Under them, a section `템플릿 · <n>` lists the visible live templates as radio rows (sub: per-kind counts). Hidden templates are listed only while the hidden group is open; there is no lock row in this sheet.
- **Preview** for a chosen template, computed on a copy before anything is written:
  - Section `더할 항목 · <n>`: names of the fields that will be added for the current tab's kind, in template order.
  - Section `이미 있음 · <n>`: template fields already matched in the project (names joined with ` · `).
  - Section `◇ 같은 이름 · 다른 종류 · <n>`: rows `<name> · <template type> · 건너뜀`. These are never added and never change the existing field.
  - If the template also has fields of other kinds: a checkbox `<other kinds>도` with sub `장소 2 · 세력 1` (only kinds that would add something), **off** by default. When it is checked, the preview adds one `더할 항목` group per kind.
- **Matching**, for each template field in order:
  1. A live project field whose key is in the template field's `sourceDefKeys`, or of the same kind with `bundleOrigin` equal to the template field's origin → `이미 있음`.
  2. Else a live project field of the same kind with the same name (NFC):
     - same type → `이미 있음`;
     - different type → `◇ 같은 이름 · 다른 종류`.
  3. Else → `더할 항목`.
- **Apply** (primary `항목 더하기`, enabled when something would be added):
  - Missing fields are added at the end of each kind's list, in template order. They get new keys and new option keys, and `bundleOrigin` = the template field's origin id.
  - A `계산` field takes as its source the matched or newly added field of the template's source. If the template has no source, so does the new field (typed age).
  - Existing fields, their options and every value stay untouched. Option lists are never merged.
  - It is one undo step, labelled `항목 더하기`.
  - `시작 묶음 · <name>` (the `bundleName` of each kind that got fields) becomes the template name, or `숨긴 템플릿` for a hidden template (CST-62).
- Applying the same template twice adds nothing the second time. A renamed field still matches by its origin id.

#### CST-61 · 새 프로젝트 4단계 · 템플릿 (template at project creation) — P1 `[PROPOSED]`
- The bundle picker of step 4 (LIB-12, `BundlePickerSheet`) keeps `기본 · 게임 프로필 · 비어 있음 · 다른 프로젝트에서` in that order, then adds a section `템플릿 · <n>` with the visible templates. Each row's sub gives per-kind counts (`인물 11 · 장소 2`). Hidden templates appear only while the hidden group is open.
- The default stays `기본`. The app never preselects a template.
- After picking, the step-4 row reads `인물 항목 | <counts> | <template name> ›`.
- If the hidden group locks while a hidden template is picked, the row reads `인물 항목 | 숨긴 템플릿 ›` with no counts. The pick is kept and applied on `만들기`.
- On `만들기` the template is applied to **all four kinds** at once, because the new project has no fields yet, so nothing is skipped. This is unlike `다른 프로젝트에서`, which stays character-only.
- `fieldBundleAtCreation` = `TEMPLATE`, and `bundleName` = the template name (`숨긴 템플릿` for a hidden one).
- If the template was deleted between picking and `만들기`, the project is created with `기본`, and the creation toast gains `◇ 템플릿 없음 · 기본으로`.

#### CST-62 · 숨긴 템플릿 (hidden templates never leak) — P0 `[PROPOSED]`
Templates live at app level. Field and option names copied from a hidden or R-18 project (0.2.0: "R-18" means a 0.1.0 project whose stored rating is R-18, D34; its protections decide, not a badge) would otherwise show in 설정 and in other projects' pickers while that project is locked. Rules:
- A template carries `hidden: Boolean`. CST-57, CST-58 and CST-59 set it through the visible switch. It defaults on when the source is a hidden project and off otherwise. Nothing else ever sets or clears it.
- While the hidden group is locked, a hidden template appears nowhere by name, field name or count:
  - `설정 › 인물 템플릿` shows one `LockRow` `숨긴 템플릿` without a count, and the row sub `템플릿 <n>` counts only visible ones;
  - it is not offered in `항목 더하기`, in new-project step 4 or in `템플릿 만들기 › 템플릿에서`;
  - the `지운 템플릿` list and its count leave it out;
  - search, widgets and notifications never show templates at all.
- While the group is open, hidden templates are listed normally with a lock mark. They re-lock under the same rule as hidden projects (설정 · 숨기기 `다시 잠그는 때`).
- A non-hidden project that took fields from a hidden template shows `시작 묶음 · 숨긴 템플릿`, never the template's name. The added fields themselves are ordinary project fields.
- No template stores its source project's name or key, so deleting or hiding a project later reveals nothing.
- A template hidden **after** a non-hidden project took fields from it: that project's `시작 묶음 · <name>` keeps the name already copied (project content, not a live link). See §7.8 Q36.
- R-18 capture blocking (`FLAG_SECURE`) applies to `템플릿 고치기` while a hidden template is open, as it does for hidden projects (`ProtectWindow`). **(0.2.0, S-4, D34)** Read this as the `화면 캡처 가리기` protection: it applies while a hidden template is open, keyed on the protection switch and not on a rating.

#### CST-63 · 템플릿 보관 · 백업 (storage and backup) — P1 `[PROPOSED]`
- **Store.** One small JSON file `files/app/fieldTemplates.json` (`FieldTemplatesFile`, see §7.3), through `core:store` `JsonFile`, in a new `FieldTemplateStore`.
  - Not `AppSettings`. That record is re-emitted to every settings reader on each change, and templates are content, not preferences.
  - Not `.civilight`, because templates belong to no project.
- **Trash.** `지우기` sets `deletedAt`. Templates deleted more than 30 days ago are dropped when the store loads and by `PurgeActions.runDue` (an additive call). `되살리기` clears `deletedAt`.
- **Drive backup (LIB-57).** The backup run uploads `fieldTemplates.json` next to the settings JSON as one more app-level unit:
  - Non-hidden templates are in plain form.
  - Hidden templates go in only when a 복구 암호 exists and `숨긴 것도 백업` is on, encrypted with the 복구 암호 like hidden projects. Otherwise they are left out, and the backup warnings gain `숨긴 템플릿 · 백업 안 됨` (no count).
- **Restore (LIB-59).** Templates merge by key: a newer `updatedAt` wins, and nothing local is deleted.
- While Drive is a stub (`UnavailableDrive`), this rule only shapes `BackupPlanner` and its tests.
- **Export or import a template as a file**: not in v1 (CST-65).

#### CST-64 · 장소 · 용어 · 세력 템플릿 (same mechanism) — P1 `[PROPOSED]`
- **Cheap, included.** The engine is already kind-generic: `FieldDef.entityKind`, the four settings tabs, `AddFieldSheet(kind)` on place, term and faction cards, and `FieldBundles.apply(..., kind)`. A template therefore holds fields of any kind (CST-56), and saving, editing and applying cover all four tabs with no extra screens.
- There are still no built-in bundles for 장소 · 용어 · 세력. They start `비어 있음` (CST-24).
- `다른 프로젝트에서` at step 4 stays character-only (as built). A template at step 4 applies all kinds (CST-61).

#### CST-65 · 나중에 · 기본값 · 템플릿 파일 (later) — P2
- **Default value per field** (a value pre-filled for new characters): no confirmed rule asks for it. It would also clash with "empty = hidden in view" and with `??`. Not built; it needs the user's decision (§7.8 Q30).
- **Template as a file** (share or import a `.civilight-template`): not in v1. It needs a file type, a picker and the import rules of LIB-52.
- **Reordering templates** in `설정 › 인물 템플릿`: not in v1.

### 7.3 Data

**FieldTemplate** (`core:model` `model/app/FieldTemplate.kt`; app level; never in `.civilight`)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `key` | Key (ULID) | no | permanent key |
| `name` | String | no | NFC, trimmed, non-empty; not unique |
| `hidden` | Boolean | no | CST-62 |
| `fields` | List<TemplateField> | no | may be empty |
| `order` | OrderKey | no | list order (creation order in v1) |
| `createdAt`, `updatedAt` | Timestamp | no | `updatedAt` decides the restore merge |
| `deletedAt` | Timestamp | yes | `지운 템플릿` (30 days) |
| `ext` | JsonObject | yes | forward compatibility |

**TemplateField**

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `key` | Key | no | template-local key |
| `origin` | String | no | `basic.birth`, `gameProfile.height`, or `tpl:<key>` (CST-56) |
| `entityKind` | OpenEnum<EntityKind> | no | |
| `name`, `exportName` | String, String? | no, yes | |
| `type` | OpenEnum<FieldType> | no | |
| `order` | OrderKey | no | order inside its kind |
| `inHeader`, `inList`, `inFilter`, `isSecret` | Boolean | no | `inFilter` only for 고르기 · 여러 개 고르기 · 연결 |
| `shortTextMultiple`, `unit`, `allowFreeText` | as `FieldDef` | yes | by type |
| `options` | List<String> | no | labels in order; new option keys on apply |
| `datePrecision`, `linkTargetKind` | OpenEnum | yes | by type |
| `calcSourceKey` | Key | yes | another `TemplateField.key`; null = typed age |
| `sourceDefKeys` | List<Key> | no | keys of the project definitions this field was saved from (opaque; matching only) |

**FieldTemplatesFile** `{ formatVersion: 1, templates: List<FieldTemplate> }` in `files/app/fieldTemplates.json`. A missing or unreadable file reads as empty. Unknown fields are kept in `ext`.

**Model changes elsewhere**
- `FieldBundle` gains `TEMPLATE`. `Project.fieldBundleAtCreation` is stored as `OpenEnum<FieldBundle>` (checked in `Project.kt`), so an older reader keeps the unknown value; the field is informational only. Exhaustive `when`s to update: `FieldBundles.label` and `FieldBundles.fields` (`TEMPLATE` → no built-in fields, like `FROM_PROJECT`); `FieldBundles.PICKER` stays the four built-ins; `BundleSheet` and `BundlePickerSheet` keep templates in their own section, not in `PICKER`; `ProjectCreator` gets a `TEMPLATE` branch.
- `NewProjectForm` gains `bundleTemplateKey: Key?`.

### 7.4 Rules as test cases

**TC-F-27 Order buttons**
- Given 인물 fields `출생 · 나이 · 생일 · 소속 · 성격` and order mode on.
- When `성격` `앞으로` → Then the order is `출생 · 나이 · 생일 · 성격 · 소속`, `성격` reads `4/5`, and one undo step `순서 바꾸기` exists.
- Then `출생` has `앞으로` disabled, and the last row has `뒤로` disabled.

**TC-F-28 Order mode is explicit**
- Given the 인물 항목 screen → Then the section label's trailing control is the button `순서 바꾸기`, and rows show no `앞으로 · 뒤로`.
- When `순서 바꾸기` → Then the rows show `앞으로 · 뒤로`, the chevrons and `+ 항목` are hidden, and the button reads `완료`.
- When switching to the `장소` tab → Then order mode is off.

**TC-F-29 Hidden definitions in the order**
- Given `신장` hidden at position 3 of 7.
- Then in order mode it is listed with `숨김` and reads `3/7`, and its neighbours' `앞으로` · `뒤로` count it.

**TC-F-30 Delete confirm per kind**
- Given 세력 field `규모` with values on 3 factions, no records, `inFilter` off → Then the confirm lines are `값이 있는 세력 3개` and the footnote.
- Given 인물 `출생` with values on 12 characters, 5 `◆`, and `나이` calculating from it → Then the lines are `값이 있는 인물 12명` · `◆ 변화 기록 5` · `계산 항목 나이 · 직접 넣기로` and the footnote.

**TC-F-31 Calc source in the trash**
- Given 리엔 출생 1183 (calculated 나이 `17세` at lens 1-4) and no typed 나이.
- When `출생` is deleted → Then 리엔's 나이 is empty (shown in `빈 항목 N`), not `17세`.
- When `출생` is restored → Then 나이 is `17세` again.

**TC-F-32 Save picks**
- Given the example-like project: 인물 fields of 게임 프로필 (11), `신장` hidden, an `이 인물만` field `좋아하는 것` on 리엔, and 장소 field `기후`.
- When the save sheet opens → Then:
  - group `인물 · 11` has 10 checked rows and `신장` unchecked with `숨김`;
  - group `장소 · 1` has `기후` checked;
  - `이 인물만 항목 1 · 빠짐` is shown.
- When saved as `내 기본` → Then the template has 11 fields (10 인물 + `기후`) and no values. Its `출생` has origin `basic.birth` and `기후` has `tpl:<key>`. The project is unchanged and its undo stack has no new step.

**TC-F-33 Calc without its source**
- When the save sheet unchecks `출생` and keeps `나이` → Then `◇ 나이 · 출생 없이 · 직접 넣기` shows, and the saved `나이` has `calcSourceKey = null`.

**TC-F-34 Apply preview and matching**
- Given project B with 인물 `출생` (날짜, `bundleOrigin = basic.birth`), `소속` (짧은 글), and `키` (숫자, origin `tpl:h1`, renamed from `신장`).
- And template T with `출생` (`basic.birth`), `소속` (연결), `신장` (origin `tpl:h1`), and `특기`.
- Then the preview reads `더할 항목 · 1` `특기`; `이미 있음 · 2` `출생 · 신장`; `◇ 같은 이름 · 다른 종류 · 1` `소속 · 연결 · 건너뜀`.
- When `항목 더하기` → Then only `특기` is added (at the end, with `bundleOrigin = <T's 특기 origin>`). `소속` stays 짧은 글 with its values, and `키` keeps its name.

**TC-F-35 Apply never changes existing**
- Given B's 고르기 `종족` with options `인간 · 수인` and template T's `종족` (same origin) with `인간 · 수인 · 용족`.
- When T is applied → Then B's `종족` options are still `인간 · 수인`, and every character value is unchanged.

**TC-F-36 Apply twice**
- When T is applied to B twice → Then the second preview has `더할 항목 · 0`, `항목 더하기` is disabled, and no undo step is added.

**TC-F-37 Calc source on apply**
- Given B has `출생` (origin `basic.birth`) and T has `나이` (계산, source T's `출생`).
- When applied → Then B's new `나이` calculates from B's existing `출생`.

**TC-F-38 Other kinds are opt-in**
- Given T has 인물 `특기` and 장소 `기후`, applied from the `인물` tab.
- Then the checkbox `장소 · 용어 · 세력도` (sub `장소 1`) is off, and applying adds only `특기`.
- When the checkbox is checked before applying → Then `기후` is added to 장소 too, still in one undo step.

**TC-F-39 Step 4 with a template**
- Given template `내 기본` (인물 10, 장소 1).
- When it is picked in step 4 → Then the row reads `인물 항목 | 인물 10 · 장소 1 | 내 기본 ›`.
- When created → Then the project has those 10 인물 fields and 1 장소 field, `fieldBundleAtCreation = TEMPLATE`, and the 인물 항목 row reads `내 기본 · 고정 7 · 프로젝트 10 · …`. **(0.2.0)** A new project has `게임 제작 도구` off (G-1), so the row reads `내 기본 · 고정 6 · 프로젝트 10 · …` (CST-19, TC-G-02).
- Given no choice is made → Then `기본` is applied (TC-F-15 unchanged).

**TC-F-40 Hidden template stays hidden**
- Given hidden, locked project 「유리 정원」 and a template `유리 정원 인물` saved from it with `숨긴 템플릿` on, plus one visible template.
- Then:
  - `설정` shows `인물 템플릿 · 템플릿 1`;
  - the template screen shows the visible row and `숨긴 템플릿 · 지문으로 보기` with no number;
  - neither step 4, `항목 더하기` nor `템플릿 만들기 › 템플릿에서` lists it;
  - no string on these screens contains `유리 정원` or any of its field names.
- When the hidden group is unlocked → Then it is listed with a lock mark and can be applied.
- When a non-hidden project applies it → Then that project's row reads `시작 묶음 · 숨긴 템플릿`.

**TC-F-41 Save defaults in a hidden project**
- Given a hidden project → Then the save sheet's `숨긴 템플릿` switch starts on.
- When it is turned off → Then `◇ 잠가도 보임` shows.
- Given a normal project → Then the switch starts off with no notice.

**TC-F-42 Step 4 re-lock**
- Given a hidden template picked in step 4, when the hidden group locks → Then the row reads `인물 항목 | 숨긴 템플릿 ›`, and `만들기` still applies it.

**TC-F-43 Template trash**
- When `지우기` on `내 기본` → Then it leaves the list, the toast reads `템플릿 내 기본 지움 · 되돌리기`, and `지운 템플릿 · 1` appears.
- When `되돌리기` (or `되살리기` in the list) → Then it is back with its fields.
- Given `deletedAt` 31 days ago → Then it is gone after the next store load.
- Projects that took its fields are unchanged.

**TC-F-44 Template editor has no values to guard**
- Given template field `소속` (연결 · 세력) → Then every one of the 9 types is offered, and changing to 짧은 글 shows no `◇` preview.
- When `빼기` on `출생` while `나이` uses it → Then after the confirm `이 템플릿에서만 · 프로젝트 그대로`, `나이` has no source.

**TC-F-45 Replace an existing template**
- Given project A saved template `내 기본`, whose `특기` has origin `tpl:s1` and `sourceDefKeys = [A's 특기]`, and A then renamed `특기` to `장기` and added 장소 `기후`.
- When A saves with `있는 템플릿 바꾸기` → `내 기본`, keeping `장기` and checking `기후` → Then the template field for `장기` keeps its key and origin `tpl:s1` (name now `장기`), and `기후` is a new template field with a new origin.

**TC-F-48 Round trip to the source project**
- Given project A saved `내 기본` and then renamed `신장` to `키`.
- When `내 기본` is applied back to A → Then `신장` is under `이미 있음` (matched by `sourceDefKeys`), nothing is added, and A was never written by the save.

**TC-F-46 Backup of templates**
- Given one visible and one hidden template, and no 복구 암호 → Then the backup plan uploads the visible one, leaves the hidden one out, and the warnings include `숨긴 템플릿 · 백업 안 됨`.
- Given a 복구 암호 and `숨긴 것도 백업` on → Then the hidden one is included, encrypted.

**TC-F-47 Copy lint**
- Then every constant in §7.6 passes `CopyLint`: no 해요체, no em dash, ` · ` separators, none of `선택 · 수정 · 변경 · 미리보기`, and `삭제` only in its allowlisted forms (`30일 뒤 삭제`).

### 7.5 Screens

| # | Screen / sheet | Boards | Cover (390) | Inner (800) | Components |
| --- | --- | --- | --- | --- | --- |
| S29 | 인물 항목 · 순서 바꾸기 mode | none (from `RCFields`) | rows with grip, `3/7`, `앞으로` · `뒤로` | same in the 560 column | `SectionLabel` (trailing text button), grip, `TextAction` ×2 |
| S30 | 지우기 confirm (field) | none | `ConfirmDialog` with fact lines | same | `ConfirmDialog` |
| S31 | 템플릿으로 저장 sheet | none | bottom sheet | side popover | `Sheet`, text field, radio cards, checkbox rows by kind, `SwitchRow`, primary button |
| S32 | 설정 › 인물 템플릿 | none | full screen list | `목록 + 상세`: list left, `템플릿 고치기` right `[PROPOSED]` | `ListRow`, `LockRow`, empty card (LIB-70 pattern), plus row |
| S33 | 템플릿 만들기 sheet | none | bottom sheet | popover | text field, `RadioRow` (expanding sources) |
| S34 | 템플릿 고치기 | none (same layout as `RCFields`) | full screen | right pane or 560 column | the extracted field-list composable, `SwitchRow`, ghost button |
| S35 | 템플릿 항목 고치기 | none (same layout as `FieldDefEdit`) | full screen | same | the extracted field-form composable |
| S36 | 항목 더하기 + 템플릿 section and preview | none (extends CST-24 sheet) | bottom sheet | popover | `RadioRow`, section labels, `CheckboxRow`, primary button |
| S37 | 새 프로젝트 4/4 · 템플릿 rows | `NewStart` (row only) | picker sheet section | same | `RadioRow` |
| S38 | 지운 템플릿 list | none | full screen | same | `ListRow` + `되살리기` |

None of these is drawn. Build them from this section with the existing components; no new design-system component is needed.

### 7.6 Copy (Kotlin constants; `CastUiCopy` unless noted)

| Constant (suggested) | Copy | Where |
| --- | --- | --- |
| `REORDER` (exists) | `순서 바꾸기` | section trailing button · undo label |
| `BTN_DONE` | `완료` | order mode end |
| `BTN_FORWARD` · `BTN_BACKWARD` | `앞으로` · `뒤로` | order mode buttons, grip accessibility actions |
| `ORDER_NOTE` | `끌기 · 앞으로 · 뒤로` | order mode note |
| `position(i, n)` | `3/7` | order mode, mono |
| `deleteHolders(kind, n)` (domain) | `값이 있는 인물 12명` · `값이 있는 세력 3개` | delete confirm |
| `deleteRecords(n)` (domain) | `◆ 변화 기록 5` | delete confirm |
| `deleteCalc(name)` (domain) | `계산 항목 나이 · 직접 넣기로` | delete confirm |
| `DELETE_FILTER` (domain) | `필터에서 빠짐` | delete confirm |
| `TEMPLATE_SAVE_ROW` | `템플릿으로 저장` | 인물 항목 row · sheet title |
| `TEMPLATE_SAVE_SUB` | `프로젝트 밖에 보관 · 새 프로젝트 · 항목 더하기에서 사용` | row sub |
| `TEMPLATE_SAME_NAME` | `◇ 같은 이름 템플릿 있음` | name input |
| `TEMPLATE_WHERE` | `어디에` | save sheet |
| `TEMPLATE_NEW` | `새 템플릿` | radio card |
| `TEMPLATE_REPLACE` · `TEMPLATE_REPLACE_SUB` | `있는 템플릿 바꾸기` · `그 템플릿 항목을 이 목록으로` | radio card |
| `templatePicks(n)` | `담을 항목 · 11` | section |
| `kindCount(kind, n)` | `인물 · 11` | group label |
| `calcWithoutSource(calc, source)` | `◇ 나이 · 출생 없이 · 직접 넣기` | save sheet |
| `scopedLeftOut(n)` | `이 인물만 항목 1 · 빠짐` | save sheet |
| `TEMPLATE_DEFS_ONLY` | `항목 정의만 · 값 없음` | save sheet |
| `TEMPLATE_HIDDEN` · `TEMPLATE_HIDDEN_SUB` | `숨긴 템플릿` · `지문으로 열 때만 보임` | switch; also the lock row title and `시작 묶음 · 숨긴 템플릿` |
| `TEMPLATE_VISIBLE_NOTICE` | `◇ 잠가도 보임` | switch off in a hidden context |
| `BTN_SAVE` | `저장` | save sheet primary |
| `templateSaved(name)` | `템플릿 내 기본 저장` | toast |
| `TEMPLATES_TITLE` (library `SettingsCopy`) | `인물 템플릿` | 설정 row, screen title |
| `SETTINGS_SECTION_CAST` (library) | `인물` | 설정 section |
| `templatesSub(n)` (library) | `템플릿 3` · `없음` | 설정 row sub |
| `templateKinds(…)` | `인물 11 · 장소 2 · 세력 1` · `항목 없음` | list row sub, picker subs |
| `BTN_EDIT_CHEVRON` (exists as `고치기 ›` in domain) | `고치기 ›` | list row right |
| `TEMPLATE_ADD_ROW` | `템플릿` (with plus icon: `+ 템플릿`) | list |
| `TEMPLATE_MAKE` | `템플릿 만들기` | sheet title, primary `만들기`, empty-state button |
| `TEMPLATE_START` | `시작` | make sheet section |
| `FROM_TEMPLATE` · `FROM_PROJECT_SHORT` | `템플릿에서` · `프로젝트에서` | make sheet radios |
| `LOCK_TEMPLATES` · `LOCK_OPEN` | `숨긴 템플릿` · `지문으로 보기` | lock row |
| `deletedTemplates(n)` | `지운 템플릿 · 2` | list row |
| `deletedTemplateSub(date)` | `11월 23일 지움 · 30일 뒤 삭제` | trash list |
| `BTN_RESTORE` (exists) | `되살리기` | trash list |
| `TEMPLATES_EMPTY` · `TEMPLATES_EMPTY_SUB` | `템플릿 없음` · `프로젝트 설정 › 인물 항목 › 템플릿으로 저장` | empty state |
| `TEMPLATE_FIELDS` | `템플릿 항목 · 7` | template editor section |
| `TEMPLATE_REMOVE_ONLY` | `이 템플릿에서만 · 프로젝트 그대로` | field `빼기` confirm |
| `templateDeleteLines(n)` | `항목 7` · `프로젝트에 넣은 항목은 그대로` · `지운 템플릿 · 30일 뒤 삭제` | template `지우기` confirm |
| `templateDeleted(name)` | `템플릿 내 기본 지움` (+ action `되돌리기`) | toast |
| `TEMPLATE_SECTION` | `템플릿 · 3` | `항목 더하기` and step-4 pickers |
| `previewAdd(n)` · `previewHave(n)` · `previewClash(n)` | `더할 항목 · 1` · `이미 있음 · 2` · `◇ 같은 이름 · 다른 종류 · 1` | apply preview |
| `clashRow(name, type)` | `소속 · 연결 · 건너뜀` | apply preview |
| `otherKinds` · `otherKindsSub(…)` | `장소 · 용어 · 세력도` · `장소 1` | apply checkbox |
| `TEMPLATE_GONE` (library `NewProjectCopy`) | `◇ 템플릿 없음 · 기본으로` | creation toast addition |
| `BACKUP_HIDDEN_TEMPLATES` (library `BackupCopy`) | `숨긴 템플릿 · 백업 안 됨` | backup warnings |

Existing words reused unchanged: `항목 더하기`, `시작 묶음`, `빼기`, `지우기`, `되돌리기`, `취소`, `닫기`, `휴지통으로`, `숨김`. The new noun `템플릿` should be added to DS-63 as the one word for a user-made field set; built-in sets stay `시작 묶음`.

### 7.7 Build plan

**Shared modules (frozen; Requests for the orchestrator, DECISIONS §2)**
1. `core:model`:
   - `model/app/FieldTemplate.kt` (`FieldTemplate`, `TemplateField`, `FieldTemplatesFile`), with a serialisation round-trip test;
   - `FieldBundle.TEMPLATE`;
   - `NewProjectForm.bundleTemplateKey` (it lives in `feature:library`, so that part is not frozen).
2. `core:store`:
   - `templates/FieldTemplateStore.kt` (`JsonFile` at `files/app/fieldTemplates.json`; `state`, `update`, `ensureLoaded`, `purgeDeleted(now)`), registered in `StoreContainer`;
   - a test for load, unreadable → empty, and purge after 30 days.
3. `core:navigation`:
   - routes `TemplateList`, `TemplateEdit(templateKey)` and `TemplateFieldEdit(templateKey, fieldKey)`; they are not `ProjectRoute`s, owner `CAST`;
   - added to `RouteSamples`.
4. `domain:actions`: `PurgeActions.runDue` also purges templates trashed for 30 days (additive overload taking the templates file), with a test.

**`domain:cast` (cast lane, additive)**
- `field/FieldTemplates.kt` (pure):
  - `fromProject(doc, picks, name, hidden, ctx)`;
  - `replaceFields(template, doc, picks, ctx)`, which keeps template keys by `sourceDefKeys` or origin;
  - `preview(doc, template, kinds)` → `TemplatePreview(added, present, clashes)` per kind;
  - `apply(doc, template, kinds, ctx)` → `BundleResult` per kind;
  - `forProjectCreation(doc, template, ctx)` for all kinds;
  - `visible(templates, hiddenUnlocked)`;
  - `kindCounts`;
  - `toScratchDoc(template)` and `fromScratchDoc(doc, template)`, so the editor runs over a field-only `ProjectDoc`.
- `FieldBundles`: a public `addFields(doc, fields: List<BundleField>, kind, ctx)` that wraps the private `addMissing`, so templates use the same matching (origin, then name). Clash detection (same name, other type) goes in `FieldTemplates.preview`; `addMissing` already skips same-name fields.
- `FieldDefinitions.deleteImpact` and the per-kind unit (CST-55). `FieldReader.readAge` ignores a trashed source.
- Tests: `domain/cast/.../FieldTemplatesTest.kt` (TC-F-32 … 38, 44, 45, 48), additions to `CustomFieldsTest.kt` (TC-F-30, 31).

**`feature:cast`**
- `settings/`:
  - extract the stateless `FieldListBody` from `FieldSettingsScreen` (tabs, fixed box, rows, order mode, `+ 항목` slot, trailing rows slot);
  - extract the stateless `FieldDefForm` from `FieldDefEditScreen`, driven by a `FieldDefHost` with `hasValues: Boolean` (template = false) and write callbacks;
  - the project screens become thin wrappers;
  - add `OrderMode` to `FieldSettingsModel` (`forward`, `backward`, `positionLabel`, end checks);
  - add the delete-confirm lines.
- `templates/` (new package):
  - `TemplateListModel` + `TemplateListScreen` (S32, S38);
  - `NewTemplateSheet` (S33, feature-local);
  - `TemplateEditViewModel` (holds the scratch doc, writes `fromScratchDoc` to `FieldTemplateStore` on each change) + `TemplateEditScreen` (S34) + `TemplateFieldEditScreen` (S35);
  - `TemplateAddFieldSheet` (feature-local, reuses the `AddFieldSheetContent` form with `entityKey = null`);
  - `SaveTemplateModel` + `SaveTemplateSheet` (S31, feature-local, opened from `FieldSettingsScreen`).
- `settings/BundleModel.kt` + `BundleSheet.kt`: the template section, the 3-part preview and the other-kinds checkbox (S36).
- `CastEntries.kt`: register the three routes.
- `CastUiCopy`: §7.6 constants, with new formatters added to `CastCopyLintTest.formatted()`.
- Tests:
  - `FieldSettingsTest` (+ TC-F-27, 28, 29);
  - `FieldDefEditTest` (+ TC-F-30 lines);
  - new `TemplateModelsTest` (list visibility and lock row TC-F-40, save defaults TC-F-41, trash TC-F-43);
  - `CastCopyLintTest` (TC-F-47);
  - motion lint passes with no new motion.

**`feature:library`**
- `settings/SettingsScreen.kt` + `SettingsState.kt` + `SettingsCopy`: section `인물`, row `인물 템플릿` → `TemplateList`; the sub counts visible templates (needs `FieldTemplateStore` in deps).
- `newproject/BundlePickerSheet.kt`, `NewProjectForm.kt`, `NewProjectSteps.kt`, `ProjectCreator.kt`, `NewProjectViewModel.kt`:
  - the template section and row label (CST-61, TC-F-39, TC-F-42);
  - creation through `FieldTemplates.forProjectCreation`;
  - the missing-template fallback and toast.
  - Tests in `NewProjectTest`.
- `backup/BackupRunner.kt` + `domain:library` `BackupPlanner`: the templates unit and the hidden-template rule (TC-F-46); restore merges by key (`RestoreRunner`).

**Order of work**
1. CST-54 and CST-55 (no shared-module change, ships alone; fixes a usability gap and a data bug).
2. Requests 1 to 3.
3. `FieldTemplates` domain with its tests.
4. Save sheet, list and apply.
5. Template editor (extraction).
6. Step 4.
7. Backup.

Commands: `tools/gw :domain:cast:test :feature:cast:compileDebugKotlin :feature:cast:testDebugUnitTest :feature:library:compileDebugKotlin :feature:library:testDebugUnitTest`. No APK.

`docs/FEATURES.md` needs rows CST-53 … CST-65 when this is scheduled. Not edited here.

### 7.8 Open questions (each with the default this section uses)

30. **Default value per field.** Not in any rule. It conflicts with "empty fields hidden" and `??`. Default: not built (CST-65); ask the user.
31. **Hidden templates.** Default: a visible `숨긴 템플릿` switch, on when saving from a hidden project. The alternative is to forbid saving templates from hidden projects altogether. Ask the user.
32. **Name of the feature.** `인물 템플릿` (the user's word, mirroring `인물 항목`, which also holds 장소 · 용어 · 세력) vs. renaming built-in bundles to templates too (`기본 템플릿`). Default: user sets are `템플릿`, built-ins stay `시작 묶음`, and applying either is `항목 더하기`.
33. **Applying other kinds from a tab.** Default: only the tab's kind, with an opt-in checkbox for the rest (TC-F-38). At step 4 all kinds are applied.
34. **Same name, same type, different options or unit.** Default: counted as `이미 있음` and never merged (TC-F-35), because applying never changes existing fields.
35. **`순서 바꾸기` replaces the board note `끌어서 순서`** (`RCFields`). Default: the button replaces the note; the note survives as `끌기 · 앞으로 · 뒤로` in order mode.
36. **Template hidden after use.** A project that already shows `시작 묶음 · <name>` keeps it. Alternative: store the template key and re-mask the name while it is hidden. Default: keep (the name is a snapshot).

### 7.9 0.2.0 reconciliation (U-09 in the frozen scope)
U-09 is built as §7.1 … §7.8 specify (scope U-09, D26: CST-56 … CST-64 are built as written and wait for the user's confirmation one by one). CST-65 is not built. In 0.2.0:
- **No voice slots.** N-45 (a project list of voice-line slots) is dropped, so a template never holds voice lines, voice-slot names, archive documents or any `게임 텍스트` content. CST-56's list already holds field definitions only; this line states that it stays so.
- **The switch does not touch templates.** A template is app-level and has no `게임 제작 도구` value.
  - Saving, applying and step 4 behave the same whether the source or target project has the switch on or off.
  - The template editor's `고정 항목` box reads `고정 항목 · 6`, without `보이스 진행` (CST-59).
  - The bundle `게임 프로필` stays offered: its fields (코드네임, 신장, 종족 …) are profile fields, not game data.
- **Ratings (S-4, D34).** No template screen shows or picks a rating.
  - "Hidden project" in CST-57 and CST-62 means `protections.hidden`.
  - Capture blocking means the `화면 캡처 가리기` protection.
  - A 0.1.0 R-18 project keeps its stored rating and protections, so its templates default to hidden exactly as before.
  - CST-61's step 4 does not move: the rating sat in the name step, which loses its picker (D34).
- **U-15.** Templates and field definitions are not U-15 kinds. `+ 항목` and `+ 템플릿` stay rows with an icon and text (icon list: `＋ 항목` is already class b).
- **U-10.** The `템플릿으로 저장` sheet already uses `저장`. No template sheet uses `이 값 고치기`.
- **Data (scope §8).** `fieldTemplates.json` is unchanged. 0.2.0 adds no template field.

---

## 8. 0.2.0 copy, build plan and open questions

### 8.1 Copy (Kotlin constants; `CastUiCopy` unless noted)
Sourced copy is used verbatim. "Analogy" rows take a board's copy with only the noun swapped and need the user's eye. `TODO_COPY_…` rows have no source and go to the lane's Requests.

| Constant (suggested) | Copy | Where | Source |
| --- | --- | --- | --- |
| `NEW_CHARACTER` | `새 인물` (with icon `plus`) | list row, 3D head button, card title, accessibility name | u15 row 1; icon list row 60 |
| `newCharacterFor(q)` | `‘세라’ 새 인물` (with icon `plus`) | picker row when nothing matches | u15 row 3; quote style DS-72 |
| `BTN_CANCEL` · `BTN_CREATE_OPEN` · `BTN_CREATE` | `취소` · `만들고 열기` · `만들기` | create card | u15 row 6 |
| field labels (exist) | `이미지` · `이름` · `표시 이름` · `한 줄 소개` · `태그` | create card | u15 row 5 |
| `NEW_TAG` · `newTagFor(q)` | `새 태그` · `‘<q>’ 새 태그` | tag settings row, tag sheet row, card title | u15 rows 3, 4 (pattern) |
| `FIELD_COLOR` · `COLOR_NONE` | `색` · `없음` | tag card and tag edit sheet | u15 row 9; `없음` as in `templatesSub` |
| `NEW_KIND` | `새 관계 종류` | relation kind card title | DS-72 title pattern (its Q-DS-30) |
| `FIELD_PAIR_NAME` | `양쪽에서 부르는 이름` | relation kind card (사이 only) | u15 row 9 |
| `tagDeleteTitle(name)` | `‘주연’ 지우기` | tag delete confirm | `RCDeleteImpact` pattern |
| `tagUsers(n)` | `쓰는 인물 3` | tag delete confirm | decisions row N-44 |
| `tagImages(n)` | `이미지 1` | tag delete confirm | 55 §3.4 usage line |
| `tagDeleted(name)` | `태그 주연 지움` (+ action `되돌리기`) | toast | CST-40 `<kind> <name> 지움` pattern |
| `SPEECH_SUMMARY` | `한 줄 요약` | 말투 row, sheet title | decisions row N-43 |
| `BTN_SAVE` (exists) | `저장` | summary sheet, tag edit sheet | U-10, D16 |
| `SECRET_LOCK` | `이 탭 잠그기` | 비밀 tab switch | decisions row N-43 |
| `LAST_SEEN` | `마지막 등장` | appearance section row | decisions row N-47 |
| `SORT_ABSENT` | `오래 안 나옴` | sort control | decisions row N-47 |
| `TABS_NO_GAME` | `개요 · 말투 · 관계 · 비밀` | tabs while the switch is off | r2 §4 |
| `fixedCount(n)` (exists as format) | `고정 항목 · 6` | 인물 항목 box while off | CST-19 format |
| card view labels (library `SettingsCopy`, LIB-94; 00 §13.4) | `카드 보기` and `부채꼴 덱 · 회전 목마 · 겹친 더미 · 타로 펼침 · 기울어진 벽 · 롤로덱스 · 목록` | settings row and screen, toggle cell accessibility names | `scr_u16_settings.py`; 00 §13.4 |
| `PREV_CARD` · `NEXT_CARD` | `이전 카드` · `다음 카드` | icon-button accessibility names | icon list (existing aria-labels) |
| `OPEN` · `FLIP` | `열기` · `뒤집기` | card main button | icon list row 167 |
| `TODO_COPY_CAST_APPEARANCE_HEAD` | none | appearance section label | no source (the sketch is not frozen) |
| `TODO_COPY_CAST_APPEARANCE_BAR` | none (formatter of episode and count) | bar accessibility | no source |
| `TODO_COPY_CAST_SECRET_LOCK_SUB` | none | `이 탭 잠그기` sub | no source |
| `TODO_COPY_CAST_SECRET_LOCK_NO_DEVICE` | none | disabled switch reason | no source |
| `TODO_COPY_CAST_TAG_TRASH_SUB` | none | trash row sub for a tag | no source |
| `TODO_COPY_CAST_TAG_NAME_TAKEN` | none | disabled `되살리기` reason | no source |

The undo label of every create is `만들기`; of a tag delete, `지우기`. Both words already exist.

### 8.2 Build plan
Shared modules are frozen. Items 1 to 4 are Requests for the orchestrator (DECISIONS §2).
1. `core:model` (additive):
   - `Character.speechSummary`, `Character.secretTabLocked`;
   - `Tag.color`, `Tag.deletedAt`;
   - the U-16 `AppSettings` fields are 00 §13.4's (`castCardStyle`, `castToggle`, `viewTilt`; LIB-94's Request), not new here;
   - serialisation round-trip tests that read a 0.1.0 JSON with none of these fields.
2. `core:store`: nothing cast-specific beyond item 1 (00 §13.4 covers the settings codec).
3. `core:designsystem`:
   - the shared create card is DS-72 (`CreateCard`, `CreateCardMode`). Its field-set description must be reachable from `feature:cast`, `feature:world` and `feature:script`, because the speaker picker lives in the script area and features do not depend on each other;
   - the small `LensChip` variant (not in spec 80 yet: Request);
   - the tag colour palette (not in spec 80 yet: Request);
   - the 3D and tilt primitives are DS-74.
4. `domain:actions` `PurgeActions.runDue`: purge tags trashed for 30 days through `CharacterEdits.removeTag`.
5. `domain:cast` (cast lane, additive, pure, with tests):
   - `CharacterEdits.create(…, displayNameBase, tagKeys, portrait)`, `setSpeechSummary`, `setSecretTabLocked`, `trashTag`, `restoreTag`;
   - `CreateCards` for the field sets and validation of 인물 · 태그 · 관계 종류, and the picker row label (`newCharacterFor`, used by the script area's pickers too);
   - `CastList`: `groupTag` becomes every live tag (CST-68), trashed tags are skipped, and the `오래 안 나옴` order;
   - `Appearances`;
   - `RelationKinds.add` used by the kind card;
   - tests: TC-N44-01 … 05, TC-N47-01 … 04, TC-U15-01 … 08 (the domain parts), TC-N43-01.
6. `feature:cast`:
   - the list row and the 3D head button;
   - the card hosts and picker rows of the cast-owned pickers (relation partner, tag field sheet, `화자 한꺼번에 바꾸기`);
   - the tag settings create row and the delete confirm;
   - the profile's 4-tab rule, the appearance section, the `한 줄 요약` row and sheet, the `이 탭 잠그기` row and `SecretTabGate`;
   - the U-16 views, the head toggle and the `카드 보기 바꾸기` sheet;
   - the `CastUiCopy` constants of §8.1;
   - tests: TC-S1-01 (model part), TC-G-01, TC-G-02, TC-N43-02 … 05, TC-U16-01, TC-U16-02, TC-020-COPY.
7. Other lanes this spec depends on:
   - `feature:script` (speaker and `@` pickers use CST-66's row and card; the panel `말투` line uses CST-72);
   - `feature:library` (G-1 switch and G-3 upgrade step; the `카드 보기` settings screen; tours under S-9);
   - `feature:story` and `domain:checks` (voice checks hidden, D33);
   - `feature:canvas` (any gallery tag delete goes through `trashTag`).

Commands: `tools/gw :domain:cast:test :feature:cast:compileDebugKotlin :feature:cast:testDebugUnitTest`. No APK.

### 8.3 Open questions (each with the default this section uses)
37. **Which heads keep the lens chip (S-1).** Default:
    - the character list head (all modes including `관계도`) and the character record head;
    - on the world side, the list head and the term, place and faction card heads (WLD-71).
    - Every other screen reads at `최신`, even after a point is picked on a cast or world head.
38. **Picker rows versus kind chips (U-15.3).** Relation kinds are picked from chips, not a list sheet. Default: the existing `+ 새 종류` chip opens the card (CST-76); no top row is added.
39. **What counts as appearing (D23 versus CST-14).** Default: speaking a block, or an `@` link in game-output text (dialogue, narration, system, choice lines). Memo and direction links and the silent `등장` list do not count, so a silent character can have `첫 등장 1-3` and empty bars. Hidden scenes count only while unlocked.
40. **Secret-tab lock scope.** Default: per character, asked on every opening, and turning it off asks too. While it is on, search skips the character's secret fields, and the `비밀 항목 포함` export is unchanged.
41. **U-16 settings and head.** Default: follow 00 §13.4 and LIB-93/LIB-94 (owners).
    - `castCardStyle`, `castToggle` and one shared `viewTilt`;
    - no sub lines, no `머리 토글` line and no `카드 보기 바꾸기` sheet, although the boards draw them;
    - when the style is `목록`, no toggle is drawn.
42. **Tag restore when the name is taken.** Default: `되살리기` is disabled with a reason; nothing is renamed by guess.
43. **No create from inside the create card.** Default: the 인물 card's tag chips list live tags only, with no nested `새 태그` card.
44. **Recorder entries on the profile under S-8.** Default: only starting a recording from a record is removed. The `레코더 · 정리 안 됨 n ›` link and the empty state's `라이프 레코더에서`, which open 기록함 and record nothing, stay.
45. **Pickers beyond U-15.3's examples.** U-15.3 names the speaker, `@` link and scene place pickers. Default: every picker of a creatable kind gets the row, except the merge target picker:
    - the relation partner picker;
    - the scene `등장` picker;
    - the bulk speaker picker;
    - the tag field sheet.
46. **After `만들기` with a chip filter that hides the new row.** Default: the filter returns to `전체`, so the new row is seen. Nothing is pre-checked from the filter.
47. **Image picked in the create card.** Default: the image is held by the card and imported only on `만들기` or `만들고 열기`, so `취소` leaves no asset.
48. **`고정 항목` while the switch is off.** Default: `고정 항목 · 6` and `고정 6`, so the count matches what is shown. This changes TC-F-39's row text for new projects.
49. **N-47 acceptance fixture.** The scope's acceptance says "bar values on the example project". Default: the JVM tests use the small Fixture P, so the values are written in the test. The example project is checked on the emulator.


---

## 9. 0.6.0 changes (2026-11-26)

- **Immersive character profile (`CST-07`, `CST-13`)**: `ProfileScreen` adopts `ImmersiveDetailScaffold` with `RecordImmersiveHero` (background/ambient portrait + holo card), glass lens pill, `ReportBottomChrome` toast clearance, and 2-pane keyed state (`characterKey` on `rememberSaveable`, `TabBody` speech/secret caches, and ViewModel).
- **Scoped tags (`CST-12`, `CST-75`)**: `Tag.scope` (`CHARACTER`, `TERM`, `PLACE`, `FACTION`, `ALL`; one-way widening only), shared `TagSheet`, and unified `TagSettingsScreen` managing all scopes with hidden-safe usage counts and delete confirmation.
- **Appearance trace & secret tab fixes**: `AppearanceTrace` reuses `Appearances` (parent episode/chapter hidden respected, hidden `@` mentions masked, 44 dp row targets); `SecretTabGate` relocks on back/leave.
- **Relation kind permanent delete (`CST-32`)**: Confirm dialog and button use `영구 삭제` (`danger`, no trash entry).
