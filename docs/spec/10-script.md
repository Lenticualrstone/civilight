# 10 · 스크립트 (Script editor) — feature prefix `SCR`

Implementation spec for the script editor of Civilight. Engineers implement from this file alone; every Korean string in `code` is UI copy and must be used verbatim.

**Sources read (2026-11-22).** Planning doc main tab (live, rev 71) · 규칙 index (rev 25) · rule tabs 11 (rev 8), 12 (rev 8), 22 (rev 2, confirmed 2026-11-22) · 저장 파일 양식 (rev 8) · local confirmed drafts `rules/19.md`–`23.md` · memory files (concept, vocabulary, rules-time/blocks/relations/travel/19-23, character-fields, feedback-*) · board generators `design/night-manuscript/gen/*.py` (newest copy) · `boards_text.txt` (older copy, used only where a generator has no newer text) · design system `README.md` + `components/*/README.md` · research `2026-11-20-design-research.md`.

**Precedence.** Confirmed rule tabs and memory > planning main tab > board generator source > `boards_text.txt` > design system README > research. When a newer source settles an older one, this spec follows the newer one and says so in a *Source note*; only unresolved conflicts go to section 6.

**Conventions.**
- `[PROPOSED]` marks behaviour or copy that no source states and this spec had to choose to make the feature implementable. Everything not marked is from a source.
- "cover" = cover screen (390×844 boards, Compact width). "inner" = unfolded inner screen (800×800 boards, Medium/Expanded width).
- Board names drop the `NmDark`/`NmLight` prefix and `.dc.html` (e.g. `Main`, `CSheetTime`, `RSSplit`). Every board exists in dark and light.
- Separators in copy are ` · ` (space, middle dot, space). No em dash in UI copy.
- Markers: `◆` change (color `acc`), `◇` first reveal / soft notice (color `ink3`), `▲` warning (color `warn`), `▸` direction, `♪` BGM, `⌁` author memo. Dotted outline / dotted underline = "attached to something deleted".
- Block number notation is `#043`; episode notation is `1-7`; chapter is `2장`.

---

## 1. Scope

### 1.1 What this area owns

1. The script screen on cover and inner layouts: header, manuscript, command-bar row, keyboard toolbar, side panel (inner) / bottom sheet (cover) with tabs `시간 · 참고 · 속성 · 모듈`.
2. Blocks: `대사` (dialogue), `나레이션` (narration), `연출 지시` (direction cues: 배경 · BGM · 효과음 · CG · 화면 효과), `선택지` (choice), `시스템 텍스트` (system text), `작가 메모` (⌁ author memo), and the inline `괄호 지문` (parenthetical stage directions) inside dialogue.
3. Speakers on blocks: inheritance, the speaker picker sheet, speaker label and display-name rendering in the manuscript.
4. Block identity: hidden permanent key, per-episode block number `#043`, renumbering, voice IDs (자동 · 고정 · 버린 ID).
5. Editing behaviour: Enter / split / backspace merge / undo / autosave / keyboard handling.
6. Display layers (`표시` sheet) and preview modes.
7. Block select mode, move, delete, block trash entries, copy/cut/paste.
8. Anchoring of `◆` change records and `◇` first reveals to blocks (rule 12): creation from a block, relocation when the anchor is deleted (`근거 지워짐`), behaviour on move/split/merge/copy.
9. Scenes as units inside the manuscript (rule 22): `씬 머리` (scene head), `전환 문구` (transition text), `/씬` routing, `다음 씬 만들기`, `씬 나누기`, `씬 합치기`, `씬 지우기`, `앞에 새 씬`, and the content of the scene-move preview.
10. Script-side rendering of other areas' states: hidden items/scenes, deleted speakers and link targets, `@` links, term candidates, recorder-inserted memos, read-only example project.
11. The asset list (`에셋`) used by direction cues.
12. Check items computed from script data (they are listed by the check center).
13. The content contract of script data in exports: game-engine JSON `scenes[]`, dialogue CSV rows, script PDF content rules; and which script data goes into `.civilight`.

### 1.2 Hand-offs to other areas

Sibling spec files did not exist in `docs/spec/` when this file was written (checked 2026-11-22), so areas are named by subject.

| Area | What it owns that this spec only references |
| --- | --- |
| 스토리 구조 · 홈 (story structure, project home) | Chapter/episode/scene list, scene titles and status (`완료 · 쓰는 중 · 미정`), `새 씬` button, scene ⋯ in the list (`앞 씬과 합치기 · 여기서 나누기` call into SCR-50/51), scene drag/move UI that opens the preview of SCR-54, episode ⋯ `번호 다시 매기기` entry, `이어 쓰기`, per-scene character counts (`5,210자`, computed from this area's block text) |
| 장소와 때 (scene place & time; rules 11, 18) | The writing line (집필 선, "end of the furthest scene with content"), which reads block existence from this area; storage of a scene's place/region and time (base + interval + band/exact + precision + 회상), resolved time computation, `N일째` labels, band hours, the `씬 · 장소와 때` sheet (`SceneWhere`), cascade preview (`RTCascade`), flashback sheet (`RTFlashback`), travel checks, `장소 · 때 한꺼번에` |
| 인물 (cast; rules 13, 20, 인물 항목) | Characters, voice code (the `HRT` part), 표시 이름 history, relations (사이 · 마음 · 호칭), field histories, the value semantics of `◆` records (`이 값 고치기 · 여기서부터 바뀜`), character delete impact sheet, merge |
| 세계 (terms, places, factions; rule 21) | Terms, aliases, the `◇ 첫 공개` record's ownership, candidate matching (trie, particles, `끊김`), `아님` records, term card `쓰인 곳`, `모두 잇기` |
| 시점 렌즈 (lens) | Lens state and lens chip; the script has no lens and only sets it through `렌즈를 이 씬으로 맞추기` (SCR-02) |
| 점검 센터 (check center) | Check list UI, filters, `맞음` / `맞다고 표시한 것` storage, bulk fixes; SCR-62 defines the items the script emits |
| 저장 · 휴지통 · 내보내기 (storage, trash, export) | App DB, `.civilight` packaging, Drive backup, trash screen and its filters, export sheet, file writing (PDF renderer, CSV writer, JSON writer); SCR-63..65 define script content |
| 숨기기 · 등급 (hiding & rating; rule 19) | Hidden flags, biometric unlock, re-lock timing, capture blocking |
| 라이프 레코더 (recorder; rule 23) | Recording, inbox, `원고에 넣기` location sheet, `여기 넣기 · #044 뒤` button; SCR-59 renders the inserted memo |
| 명령 칸 · 검색 (command bar & search) | Command palette, search results, 초성 matching; SCR-68 lists script-specific commands |
| 화면 · 접기 · 모션 (layout, fold, motion, design system) | Window size classes, fold state save/restore, global motion tokens, reduce motion/transparency; SCR-66/67 give script specifics |
| 온보딩 (onboarding) | Example project lifecycle, walkthrough and tours drawn over the script |
| 설정 · 프로젝트 설정 (settings) | Setting screens; SCR-24/25/48 define the script settings' effects |

---

## 2. Features

Each feature lists **Priority** (P0 core · P1 planned for v1 · P2 later, only where a source defers it), **needsExternal** (network, cloud, ML model or OS integration beyond a normal app), behaviour, Korean copy, and sources.

### 2.1 Screen and layout

### SCR-01 · Script screen (cover layout)
**Priority** P0 · **needsExternal** no · **Boards** `Main`, `Blocks`, `UndoToast`, `RSHead` · **Sources** planning 스크립트 편집기 · 화면 · 디자인; DS GlassBars, CommandBar, ScriptBlock

The screen shows one episode's manuscript as a continuous scroll of scenes. Each scene starts with its scene head (SCR-46), followed by its blocks in order.

- **Entry.** Opened by other areas at a target `(sceneKey, blockKey?)`: project home `이어 쓰기` (last scene + last block written), episode/scene rows in story structure, search results, command-bar jumps (SCR-68), check-center `원고로 가기`. The target block is scrolled to and becomes the selected block. If `blockKey` is absent, the scene head of the target scene is scrolled to the top edge under the header.
- **Scroll unit.** The manuscript of the whole episode scrolls as one list. [PROPOSED] Moving past the last scene of an episode does not load the next episode; the next episode opens through story structure or a command.
- **Top glass bar** (height 118 on cover, blur 36, faded bottom edge; manuscript scrolls under it):
  - Line 1, mono 11 `ink3`: the header position line (머리말, SCR-02), e.g. `1-7 · 씬 3 · 2일째 새벽`.
  - Line 2, serif bold 19: `잿빛 새벽의 등불` (see open question Q-01 on whether this is the project or the episode name).
  - Right: position label `이 씬 · 1-7` (SCR-02), then a 44×44 layers icon button (`aria-label="연출 · 메모 표시"`) that opens the `표시` sheet (SCR-26).
- **Manuscript.** Blocks are typeset directly on `bg` with no cards, bubbles or avatars. Block gap 26 px (`space-7`); side padding 24 px. Speaker name above the line (SCR-20). Text serif (Nanum Myeongjo) 18/1.7 by default (app text-size setting).
- **Bottom glass bar** (height 150, faded top edge) holds, top to bottom:
  1. While a writing position exists (a caret or a selected block): a row with the **speaker pill** (32 px avatar initial + name, e.g. `리 리엔`; tap → speaker picker SCR-19 for the current dialogue block, or for the next dialogue block when the current block is not dialogue), the layers icon (same as header), a flexible gap, and the primary pill `줄바꿈` (SCR-15).
  2. The command field: `/` glyph + hint `명령 또는 검색 · 인물 · 블록 · 씬 이동`, and on the right the Life Recorder mic (32 px circle, `pri` fill, `aria-label="라이프 레코더"`; starts recording immediately, owned by the recorder area).
- **States.** Empty episode (no scenes): [PROPOSED] the manuscript shows the single line `씬 없음` and a primary button `새 씬` that runs SCR-49's `다음 씬 만들기` for an empty episode. Empty scene (no blocks): the scene head followed by one empty dialogue block with the inherited speaker (SCR-18) and the caret in it [PROPOSED].
- **Autosave.** Every change is saved to the app DB immediately (SCR-14); there is no save button.

### SCR-02 · Header position line and position label (`이 씬 · 1-7`)
**Priority** P1 · **needsExternal** no · **Boards** `Main`, `RSHead`, `CSheet*` · **Sources** rule 11 §3; rule 22 §6 (머리말); DS README 시점 렌즈

- **Position line (머리말).** Short form of the scene's place/time, drawn from scene properties only: `{episode} · 씬 {ordinal} · {day label}` where the day label is the short form of rule 11 §6 (`2일째 새벽`; `약 13일째`; `회상 · 1195`; nothing when the scene has no time → `1-7 · 씬 2`). [PROPOSED] When the scene has no time, the line ends after `씬 {ordinal}`.
- **Which scene it shows** (rule 22 §6): if there is a caret or a selected block, the scene of that block; otherwise the scene of the block that sits directly under the bottom edge of the top glass bar. While editing, the line becomes `{episode} · 씬 {ordinal} · 저장됨` (SCR-14). The position line stays even when a scene head is visible on screen (one is position, the other is a boundary).
- **Position label.** A lens-shaped chip without border reading `이 씬 · {episode}` (e.g. `이 씬 · 1-7`). It is not a lens: the script has no lens (rule 11 §3). Tapping it opens a one-row menu with the single action `렌즈를 이 씬으로 맞추기`; choosing it sets the app lens to "end of this scene" (rule 11 §2: point `1-7 씬 3` = after the last block of 씬 3) and shows [PROPOSED] the toast `렌즈 · 1-7 씬 3`. The lens value is then used by lens screens (인물, 세계, 지도, 연표, 캔버스, 홈).
- *Source note.* Boards `WideRef`, `WideMods`, `WideProps` still draw the lens chip `최신 · 1-7` in the script header; rule 11 §3 (confirmed) replaces it with `이 씬 · 1-7` on every script board.

### SCR-03 · Inner (unfolded) layout: manuscript + side panel
**Priority** P1 · **needsExternal** no · **Boards** `Wide`, `WideRef`, `WideProps`, `WideMods`, `MFoldScript` · **Sources** planning 화면 · 디자인 (스크립트는 펼친 화면에서 원고 + 옆 패널); DS Sheet; layout settings board (`스크립트는 옆 패널 방식 유지`)

- Width class Medium/Expanded (≥ 600 dp): manuscript column of fixed reading width 560 px starting 40 px from the left; side panel 290 px wide on the right edge, full height, glass, left hairline (`line`).
- Header spans the manuscript area only (right edge 312 px from the screen's right): position line, title (serif 21), position label. Bottom glass and command bar span the manuscript area only (`right: 314`).
- The panel holds the same content as the cover bottom sheet (SCR-31): tabs `시간 · 참고 · 속성 · 모듈` and `닫기`. Closing the panel gives the manuscript the full width but keeps the 560 reading column.
- The script ignores the `펼친 화면 배치` setting (목록 + 상세 / 가운데 한 칸); that setting applies to list modules only.
- Tabletop posture is SCR-69 (P2).

### 2.2 Blocks

### SCR-04 · Block model and common rendering
**Priority** P0 · **needsExternal** no · **Sources** planning 스크립트 편집기; rule 12 §1, §5; rule 22 §3; 저장 파일 양식 §4; DS ScriptBlock

- The manuscript of a scene is an ordered list of blocks. Kinds (v1): `대사`, `나레이션`, `연출 지시`, `선택지`, `시스템 텍스트`, `작가 메모`. There is no scene-transition block, no battle-line block, no voice-direction block, no expression tag (planning 범위). Scene boundaries are scene heads (SCR-46), not blocks.
- Every block has a hidden permanent key (ULID, never shown, never changes) and a per-episode number `#043` (SCR-21). Order is stored as an insertable order key, not an array index.
- Text is stored as a list of runs: plain text, parenthetical (`괄호 지문`), link (`@` target key), so new run kinds can be added later (저장 파일 양식 §4).
- Selection appearance: selected block gets `sel` background plus a 2 px `acc` bar on the left edge, 12 px vertical padding. Editing caret is a 2×20 `acc` bar; the line being typed never moves (SCR-14).
- A block is **empty** when every text run is blank after trimming and, for `연출 지시`, it has no cue, and for `선택지`, every option is blank, **and** no `◆`/`◇` record is anchored on it (rule 12 §4: an empty block has nothing attached, so it is deleted with no toast and no trash). [PROPOSED] definition of blank. A block whose text was erased but which still anchors a record is **not** empty: deleting it follows SCR-38 for non-empty blocks (trash, relocation, toast), because records always survive (rule 12 §2).
- Game output: `대사 · 나레이션 · 시스템 텍스트 · 선택지` and `연출 지시` go to game exports; `작가 메모` never does (`export:false`).

### SCR-05 · 대사 (dialogue) block
**Priority** P0 · **needsExternal** no · **Boards** `Main`, `EditKeyboard` · **Sources** planning 블록 종류 (`화자 · 대사 · (괄호 지문)` · `/대`); concept memory

- Fields: speaker (character key or none), text runs, optional stage position (SCR-25), voice ID state (SCR-23).
- Render: speaker line (Noto Sans KR 12/16, `ink3`, letter-spacing .04em; name in weight 600 `ink2`) above the text line (serif 18/1.7 `ink`). Right end of the speaker line carries markers (`◆` in `acc`, dotted outline variants, `▲`). See SCR-20 for display names.
- No length limit on the line (planning: 대사창 글자 수 제한 없음).
- Speaker none → see SCR-20 `화자 없음`.

### SCR-06 · 나레이션 (narration) block
**Priority** P0 · **needsExternal** no · **Boards** `Main` · **Sources** planning (`화자 없는 서술` · `/나`)

- Fields: text runs. No speaker, no voice ID.
- Render: no speaker line; text serif 16/28 `ink2`. In select mode and panels it is labelled `나레이션`.
- A `◇` first-reveal mark is drawn at the end of the text line in `ink3` (e.g. `…한 번 깜박였다. ◇`).

### SCR-07 · 괄호 지문 (parenthetical stage directions)
**Priority** P1 · **needsExternal** no · **Boards** `Main`, `EditKeyboard`, `Layers` · **Sources** concept memory (NO EXPRESSION TAGS); DS ScriptBlock `.paren`; 저장 파일 양식 §4

- Expressions and actions are written inside the dialogue line in parentheses, e.g. `(숨을 삼키며) 하르트 씨, 방금…`. They are stored as a parenthetical run (not plain text).
- Render: Noto Sans KR 14, `ink3`, inline before/within the line.
- Created by typing `(`…`)` or by the keyboard toolbar `( )` button, which inserts an empty parenthetical run and puts the caret inside it. [PROPOSED] Typing a `(` in plain text starts a parenthetical run that ends at the matching `)`.
- Hidden by the `괄호 지문` layer (SCR-26). Not a target for term candidates or `▲` (rule 21 §4). Whether parentheticals go into game exports is undecided (Q-10).
- There is no expression picker, expression tag, long-press expression, or `표정` field anywhere.

### SCR-08 · 연출 지시 (direction) block: 배경 · BGM · 효과음 · CG · 화면 효과
**Priority** P1 · **needsExternal** no · **Boards** `Main`, `RSSelect`, `Blocks`, `Assets` · **Sources** planning 블록 종류 (`배경 · BGM · 효과음 · CG · 화면 효과` · `/연`), 에셋; rule 22 §2 (화면 효과 = 페이드 · 암전, no new direction kinds); research §3

- One direction block holds one or more **cues** in order (board `RSSelect`: block `#041` = `▸ 배경 하층 거리(새벽) · ♪ 잔불`). Cue kinds: `배경`, `BGM`, `효과음`, `CG`, `화면 효과`.
- `배경 · BGM · 효과음 · CG` cues reference an asset from the asset list (SCR-61). `화면 효과` cues carry an effect name; the sources name `페이드` and `암전` (rule 22) and the JSON op `shake` (planning JSON 골격). The full effect list is open (Q-11).
- Render (mono 12/16 `ink3`, 24 px side padding, no speaker line): cues joined with spaces: `▸ 배경 {asset name}` for 배경, `♪ {asset name}` for BGM; [PROPOSED] `효과음 {name}`, `CG {name}`, `화면 효과 {name}` for the others. In select mode the cues are joined with ` · `.
- Editing [PROPOSED]: tapping a direction block opens a sheet listing its cues with `+ 연출` (cue kind chips `배경 · BGM · 효과음 · CG · 화면 효과` → asset picker filtered to that kind) and per-cue `바꾸기 · 빼기`. The sources do not draw this sheet (Q-11).
- Hidden by the `연출 지시` layer. No voice ID. No source forbids a `◆` on a direction block, so it anchors like any block (SCR-42).
- A background asset may be linked to a place (asset ↔ 장소); this link is not a source of the scene's place and no check compares them (rule 22 §2).

### SCR-09 · 선택지 (choice) block
**Priority** P1 · **needsExternal** no · **Boards** `Blocks` · **Sources** planning (`대사만 달라지고 다음 블록에서 합류` · `/선`); rule 12 §4 선택지; rule 21 §5; scope table (v1 포함)

- The story is linear. A choice block holds 2+ option lines; only the option text differs, and the story always rejoins at the **next block** (the rejoin point is computed, never stored).
- Option lines are sub-lines of the block, displayed `#045-1`, `#045-2` (block number + `-` + option index from 1).
- Render (`Blocks`): speaker-line slot reads `선택지` (weight 600) + `다음 블록에서 합류` (`ink3`); options below, each `{index}` (mono 12 `ink3`) + text (serif 16/26).
- `◆` and `◇` cannot be anchored on an option line, only on the whole choice block (rule 12 §4, rule 21 §5).
- An option line that is spoken gets a voice ID with a sub-number: `HRT_0107_045_1` (rule 12 §4). How an option gets a speaker is not specified (Q-05).
- Term links inside option text count as uses (rule 21 §4, tag `선택지`).
- Editing [PROPOSED]: Enter at the end of an option adds the next option; backspace in an empty option removes it; the block keeps at least 2 options.

### SCR-10 · 시스템 텍스트 (system text) block
**Priority** P1 · **needsExternal** no · **Boards** `Blocks` · **Sources** planning (`획득 · 안내 같은 화면 문구` · `/시`); rule 22 §4 (time skips)

- Fields: text runs. Render: speaker-line slot `시스템` + `화면 문구`; text mono 13/22 `ink2`, e.g. `[기록] ‘꺼진 등불’을 얻었다.`
- Used for in-scene time skips such as `30분 뒤`; these never change the scene's time and are not read by travel checks (rule 22 §4).
- Hidden by the `시스템 텍스트` layer. No voice ID.

### SCR-11 · 작가 메모 (author memo) block `⌁`
**Priority** P1 · **needsExternal** no · **Boards** `Main`, `RSSelect` · **Sources** planning (`⌁로 시작, 게임 출력 제외` · `/메`); scope table (battle lines / voice direction go in memos); rule 23 §4

- Fields: text runs; optional recorder source (SCR-59).
- Render: mono 12/16 `ink3`, `⌁ 작가 메모` + two spaces + text, e.g. `⌁ 작가 메모  여기서 등불이 두 번째로 깜박이게 할지`. In select mode: label `작가 메모`, text `⌁ {text}`.
- Excluded from all game output (`export:false`), never a term-candidate target, never raises `▲` (rule 21 §4). Hidden by the `작가 메모` layer.
- `#043` typed inside a memo is plain text and does not follow renumbering (rule 12 §5).

### SCR-12 · Block add sheet and slash shortcuts
**Priority** P0 · **needsExternal** no · **Boards** `Blocks`, `MType` · **Sources** planning 블록 종류, 공통 개념 명령 칸; rule 22 §4; `scr_script.py` (newest copy)

- Opened by: the keyboard toolbar's block-kind button `대사 ▾` (planning: `키보드 위 줄에 … 블록 추가`, SCR-16) and by typing `/` in the command field (planning 명령 칸: `블록 추가(/대, /나, /연…)`).
- Sheet (cover; [PROPOSED] inner uses the same sheet over the manuscript column): title `블록 추가`, right `닫기`; subline `지금 블록 아래 · 새 대사 화자: {name}(직전)`, e.g. `지금 블록 아래 · 새 대사 화자: 리엔(직전)`. Rows (name serif bold 16, description 12 `ink3`, shortcut mono 11 right):

| Row | Description | Shortcut |
| --- | --- | --- |
| `대사` | `화자 · 대사 · (괄호 지문)` | `/대` |
| `나레이션` | `화자 없는 서술` | `/나` |
| `연출 지시` | `배경 · BGM · 효과음 · CG · 화면 효과` | `/연` |
| `선택지` | `대사만 달라지고 다시 합류` | `/선` |
| `새 씬` | `다음 씬 만들기 · 여기서 나누기` | `/씬` |
| `시스템 텍스트` | `획득 · 안내 같은 화면 문구` | `/시` |
| `작가 메모` | `게임 출력 제외` | `/메` |

- Choosing a block kind inserts a new block of that kind **directly below the current block** (or at the end of the scene if there is no current block) and puts the caret in it. The newly typed block slides in from 8 px below over 320 ms (decelerate). A new dialogue block takes the inherited speaker (SCR-18).
- Choosing `새 씬` inserts nothing and runs SCR-49 routing.
- Slash shortcuts typed in the command field (`/대` …) do the same as the rows. Typing `/` alone opens the sheet.
- Read-only project: the sheet rows are dimmed and [PROPOSED] carry `읽기 전용`; the `/씬` row is dimmed with `읽기 전용` (rule 22 §4).
- *Source note.* `boards_text.txt` still shows `씬 전환 · 다음 씬 · 이야기 속 시각`; rule 22 replaces it with `새 씬 · 다음 씬 만들기 · 여기서 나누기 · /씬` (as in `scr_script.py`). The design system README's block-kind list still names `씬 전환`; rule 22 supersedes it.

### SCR-13 · Change a block's kind
**Priority** P1 · **needsExternal** no · **Boards** none (no kind-change control is drawn) · **Sources** rule 12 §4 (종류 바꾸기), §6; check center (`나레이션으로 바꾸기`); rule 23 §4 (memo → dialogue by normal editing)

- No board draws a kind-change control (the toolbar's `대사 ▾` opens the add sheet, SCR-12). [PROPOSED] The `속성` tab gets a first row `종류` with the current kind and `바꾸기`, which opens the add-sheet list (without `새 씬`) titled `종류 바꾸기`; choosing a kind converts the selected block in place.
- Conversion keeps the block's key and number. Records (`◆`, `◇`) stay: "기록은 내용에 붙지 종류에 붙지 않는다".
- Voice ID: dialogue → anything else: an automatic ID disappears; a fixed ID becomes a discarded ID (`버린 ID`). Anything → dialogue: gets an automatic ID (if voice IDs are on).
- Text runs carry over. [PROPOSED] Conversion between the text kinds (`대사 · 나레이션 · 시스템 텍스트 · 작가 메모`) is always allowed; conversion to or from `연출 지시` and `선택지` is offered only for an empty block (Q-12).
- Check-center action `나레이션으로 바꾸기` on `▲ 화자 없음` runs this conversion.

### 2.3 Writing and keyboard

### SCR-14 · Editing state, autosave and the fixed writing line
**Priority** P0 · **needsExternal** no · **Boards** `EditKeyboard`, `Main` · **Sources** planning 입력 중; DS ScriptBlock; motion spec (`움직이지 않는 것 · 입력 중인 줄 · 커서 위치`); 저장 파일 양식 §1 (자동 저장)

- Tapping the text of a block places the caret there and enters editing (see Q-02 for the tap that reveals swipe buttons).
- Editing state (cover): the top bar shrinks to 76 px and shows the mono line `{episode} · 씬 {ordinal} · 저장됨` (e.g. `1-7 · 씬 3 · 저장됨`) and on the right the text button `완료`. `완료` hides the keyboard and leaves the block selected.
- The block being edited has the selection look and a small `입력 중` label after the speaker name (11 px `ink3`).
- **Fixed writing line.** When the keyboard opens or closes, and while typing, the line holding the caret keeps its screen position; content above moves instead. [PROPOSED] If the keyboard plus toolbar would cover the caret line, the manuscript scrolls by the minimum amount that puts the caret line just above the toolbar.
- **Autosave.** Every edit is written to the app DB; `저장됨` means the latest edit is stored. [PROPOSED] While a write is pending the word reads `저장 중`. The `.civilight` file is never written while editing.
- Swipe on the block being edited is off (SCR-30).

### SCR-15 · Enter, split, backspace merge, scene-head barrier, `줄바꿈`
**Priority** P0 · **needsExternal** no · **Boards** `EditKeyboard`, `Main` · **Sources** `EditKeyboard` (`엔터 = 새 블록(같은 화자)`); rule 12 §4 (나누기 · 합치기 · 빈 블록); rule 22 §3 (백스페이스)

| Input | Where | Result |
| --- | --- | --- |
| Enter | end of a `대사` block | New `대사` block directly below with the same speaker; caret moves into it |
| Enter | end of a `나레이션 · 시스템 텍스트 · 작가 메모` block | [PROPOSED] New block of the same kind below |
| Enter | end of a `선택지` option | New option below it (SCR-09) |
| Enter | middle of the text | **Split**: text before the caret stays in the block (key, number, voice ID, records unchanged); text after the caret becomes a new block of the same kind and speaker with a new key, the episode's next number and a new automatic voice ID. Records stay on the front block; if the sentence they rely on moved to the back block, the author re-anchors with `다른 블록에 잇기` (SCR-43) |
| Backspace | start of a block that is not the first block of its scene | **Merge** into the previous block: its text is appended to the previous block; the previous block keeps key, number and voice ID; the later block's records move to the merged block and are **not** flagged `근거 지워짐`; toast `◆ {n}개 #{number}로` (e.g. `◆ 1개 #042로`) when n ≥ 1; a fixed voice ID of the later block becomes a discarded ID. See Q-13 for blocks of different kinds |
| Backspace | start of the first block of a scene | Nothing. The caret never crosses the scene head; there is no hidden scene merge |
| Backspace | inside an empty block | The empty block is deleted with no toast and no trash entry; caret goes to the end of the previous block [PROPOSED caret target] |
| `줄바꿈` pill | anywhere in a text block | [PROPOSED] Inserts a line break inside the current block (Enter makes a new block). See Q-03 |

### SCR-16 · Keyboard toolbar
**Priority** P0 · **needsExternal** no · **Boards** `EditKeyboard` · **Sources** `scr_core.py edit_keyboard`; planning (`키보드 위 줄에 화자 · 줄바꿈 · 블록 추가`)

A glass strip 56 px high sits directly on top of the system keyboard while editing. Items left to right, each ≥ 44×44:

| Item | Shows | Action |
| --- | --- | --- |
| Block kind | `대사 ▾` (current kind; `sel` background) | Block-add sheet (SCR-12); kind change is SCR-13 |
| Speaker | 24 px avatar initial + name, e.g. `리 리엔` (dialogue only) | Speaker picker (SCR-19) |
| `( )` | | Insert a parenthetical run, caret inside (SCR-07) |
| `@` | | Link picker (SCR-55) |
| `◆` | | Change-record sheet for this block (SCR-41) |
| Undo icon | | Undo (SCR-17) |

- Non-dialogue blocks: [PROPOSED] the speaker item is hidden.
- `줄바꿈` is not on the drawn toolbar; see Q-03.

### SCR-17 · Undo
**Priority** P0 · **needsExternal** no · **Boards** `EditKeyboard`, `UndoToast` · **Sources** rule 12 (되돌리기 on every toast); rule 11 §4 (모든 연쇄는 한 번에 되돌린다); rule 22 §5 (합치기 · 되돌리기 한 번으로 둘 다 돌아옴); rule 23 §4

- One undo history per open project [PROPOSED scope] covering text edits, block insert/delete/move/split/merge/kind change, speaker change, record creation/relocation/resolution, scene split/merge/delete/create, renumber, and recorder memo insertion.
- Each compound operation is one undo step (e.g. a merge with its time rewrite and memo line; a split; a renumber; a multi-block delete).
- Toast `되돌리기` buttons undo exactly the operation the toast reports.
- Undoing a delete restores the block(s) at their original positions and re-attaches relocated records (SCR-43).
- [PROPOSED] Consecutive typing in one block coalesces into one step per pause of ≥ 1 s. There is no redo in the sources.

### 2.4 Speakers

### SCR-18 · Speaker inheritance (`직전 화자`)
**Priority** P0 · **needsExternal** no · **Boards** `MType`, `Blocks` · **Sources** planning 화자; concept memory; feedback-no-guessing

- A new dialogue block takes the speaker of the nearest dialogue block above the insertion point. [PROPOSED] The search stays inside the episode; if none exists the new block has no speaker.
- There is never automatic alternation between speakers and no swipe to change speaker.
- The block-add sheet states the inherited speaker: `새 대사 화자: 리엔(직전)`. The newly inserted block shows the small label `직전 화자` after the name until the first character is typed (`MType`).

### SCR-19 · Speaker picker sheet (`화자`)
**Priority** P0 · **needsExternal** no · **Boards** `Picker`, `MSheet` · **Sources** planning 화자; feedback-no-guessing; rule 19 §6; rule 20 §2

- Opened from: the speaker label of a dialogue block, the speaker pill in the command row, the keyboard toolbar speaker item, `바꾸기` in the 속성 tab, check-center `화자 고르기`.
- Sheet: title `화자`, right `닫기`. Search field (placeholder `초성 검색`; matches name, 초성 of the name, and tags; [PROPOSED] also display name). Tag chips: `전체` + the project's tags (sample `주연 · 등불 결사 · 하층 구역`), single-select.
- Sections in this order: `이 씬 등장` (characters who speak in this scene or are added as silent cast), `최근` (speakers chosen most recently in this project, [PROPOSED] up to 5, excluding those already listed), then [PROPOSED] `전체` (all other characters by name). Row: 36 px avatar initial, name (serif bold 16), subline: tags (`주연 · 등불 결사`) or `표시 이름 ???` when the display name differs at this block.
- Tapping a row sets the block's speaker and closes the sheet. Result ordering only speeds things up; nothing is preselected.
- Must stay fast with 100+ characters (lazy list, search over an index).
- Hidden characters (locked) are not listed; the last row reads `숨긴 인물 {n} · 지문으로 보기` and opens the hiding area's unlock (rule 19).
- When the current block's speaker is a deleted character, the first row reads `지운 인물 · {name} · 되살리기` (rule 20); deleted characters are otherwise not listed.
- Motion: sheet enters with decelerate 320 ms; screen behind scales to 96.5 % with 2 px blur.

### SCR-20 · Speaker label, display name and `화자 없음`
**Priority** P0 · **needsExternal** no · **Boards** `Main`, `Wide`, `CSheetProps`, `Check` · **Sources** planning 표시 이름 덮어쓰기; concept memory (speaker labels show the name only plus real name for overrides); rule 11 §2 (`??? (1-8까지)`); DS ScriptBlock

- The label shows the character name. When the character's `표시 이름` in effect at this block differs from the name, the label is `{표시 이름}` (weight 600) followed by the real name in `ink3`: `??? 하르트`. When numbers are shown (selected block, panels): `??? 하르트 · #043`.
- `표시 이름` in effect = the cast area's display-name history evaluated at this block (a `◆` on this block applies from this block). Sample: 하르트 is `???` through 1-8 and `하르트` from 1-9.
- Tapping the label opens the picker (SCR-19).
- **화자 없음.** A non-empty dialogue block without a speaker shows the label `화자 없음` in `warn` with `▲` on the right [PROPOSED rendering] and produces the check `▲ 화자 없음` (SCR-62). The word is always `화자 없음` (vocabulary rule), never `미지정`.
- Deleted speaker: SCR-57. Hidden speaker: SCR-58. Per-block display-name override: Q-04.

### 2.5 Block numbers and voice IDs

### SCR-21 · Permanent key and per-episode block number (`#043`)
**Priority** P0 · **needsExternal** no · **Boards** `Wide`, `RSSelect`, `RSOrphan`, `UndoToast` · **Sources** rule 12 §5 (confirmed; replaces "블록마다 영구 ID(#043)"); planning 스크립트 편집기; 저장 파일 양식 §4

- **Key.** ULID created on the device when the block is created. Never shown, never changes, never reused. Used by record anchors, check `맞음` records, `아님` records, JSON `id`, CSV `블록 ID`, recorder links.
- **Number.** Unique within the episode. Issued from the episode's counter in creation order; the counter starts at 1 and only grows; numbers of deleted blocks are never reissued; numbers are never re-sorted automatically, so inserted blocks read out of order (`#042 리엔 · #058 하르트 · #044 나레이션`).
- Display `#` + 3 digits zero-padded (`#001`, `#043`). [PROPOSED] From 1000 on, unpadded (`#1043`).
- Changes only when the block moves to another episode (gets that episode's next number) or on explicit renumbering (SCR-22). Restored blocks keep their number.
- Choice option lines are `#{number}-{index}` (`#045-1`).
- Shown on the selected block's speaker line, in panels (`고른 블록 · #043`), select mode, toasts, check items and as the last part of the voice ID; not shown on unselected blocks in normal reading.

### SCR-22 · `번호 다시 매기기` (renumber an episode)
**Priority** P1 · **needsExternal** no · **Boards** `RTRenumber` · **Sources** rule 12 §5 (다시 매기기), §6; `scr_new_time.py renumber`

- Entry: story-structure episode ⋯ → `번호 다시 매기기`, or the command `번호 다시 매기기 · 1-7` (SCR-68).
- Preview sheet (nothing changes before the button):
  - Title `번호 다시 매기기 · {episode}`; subtitle `원고 순서대로 #001부터`.
  - Three figures: `{N}` `블록` · `{k}` `바뀌는 번호` · `{N−k}` `그대로`.
  - Section `바뀌는 번호 · {k}`: one row per changed block `#{old} → #{new}` + `씬 {ordinal} · {speaker or kind}`; when that block's voice ID is fixed, a sub-row `보이스 {ID} · 고정 · 그대로`. After the first rows, the rest in one line `외 {m}개 · #055 → #038 · #052 → #046 고정 · …`.
  - Footer lines: `고정 보이스 ID {f}개는 그대로` and `번호만 다름 · ◇ 없음 · 변화 기록 · 블록 ID 그대로`.
  - Buttons `취소` · `다시 매기기` (primary).
- Apply: live blocks of the episode get numbers 1…N in manuscript order (scene order, then block order). Keys, records, `맞음`/`아님` records are untouched. Automatic voice IDs recompute; fixed IDs stay and raise no `◇` (the number part is never compared). One undo step.
- Trash entries and the counter after renumbering: Q-07.

### SCR-23 · Voice ID: `자동` → `고정`, `버린 ID`
**Priority** P1 · **needsExternal** no · **Boards** `CSheetProps`, `WideProps`, `Check` · **Sources** rule 12 §6 (confirmed), §3–§4 tables; memory civilight-rules-blocks; 저장 파일 양식 §6 (voice state stored)

- Scope: dialogue blocks of the script only (and spoken choice options, SCR-09). Profile game-text voices are a later rule.
- **자동**: computed from the format (SCR-24) every time it is shown; nothing is stored except "auto".
- **고정**: the first time a block is written to a dialogue CSV or a game-engine JSON export, its current ID string and the export date are stored; from then on the app never changes it by itself (recordings and localization files use it).
- 속성 tab shows `HRT_0107_043 · 자동` or `HRT_0107_043 · 고정 · 11월 20일 내보냄` (date = first export, Korean month/day).

| Event | 자동 | 고정 |
| --- | --- | --- |
| Speaker changed | New ID | Unchanged · `◇ 화자와 다름` |
| Moved to another episode | New ID | Unchanged · `◇ 에피소드와 다름` |
| Block number changed (renumber) | New ID | Unchanged · no check (number part ignored) |
| Block deleted | Disappears | Becomes `버린 ID`, never reused |
| Dialogue → narration (kind change) | Disappears | `버린 ID` |
| Format changed in settings | New format | Unchanged · settings show `고정 ID {n}개는 그대로` and `고정 ID도 새 형식으로` |
| Split (Enter mid-line) | Front keeps; back gets a new automatic ID | Front keeps; back gets a new automatic ID |
| Merge (Backspace at start) | Front keeps | Front keeps; later block's fixed ID → `버린 ID` |
| Copy · paste | New ID | — (copies never inherit a fixed ID) |
| Cut · paste | As move | As move |
| Speaker character deleted (rule 20) | Keeps computing with the deleted character's code | Unchanged |
| Speaker character merged into another (rule 20) | Recomputed with the target's code | Unchanged · `◇` (speaker part differs) |
| Block restored from trash | State as before deletion | State as before deletion |

- **◇ rule.** A fixed ID is flagged only when its **speaker part or episode part** differs from what the format would produce now. All such blocks gather in the check center group `보이스` as one item `형식과 다른 ID {n}` with buttons `새 ID로` · `그대로`. `새 ID로` gives the block a new ID and the old string becomes a `버린 ID` (rule 12 §6); [PROPOSED] the block returns to `자동` until its next export. `그대로` marks the item `맞음`.
- **버린 ID.** Stored with the block key and the date it was discarded; exported as CSV rows with 상태 `버림` (SCR-64). Automatic IDs never collide with discarded IDs because numbers are never reissued (see Q-07 for renumbering).
- **Manual format `직접`.** When the format is set to manual entry, the 속성 tab shows an editable ID field; duplicate IDs inside the project raise `▲` (SCR-62).

### SCR-24 · Voice ID format (project setting)
**Priority** P1 · **needsExternal** no · **Boards** `ProjSettings` · **Sources** `scr_decide.py proj_settings`; rule 12 §6; planning 범위 (`형식 HRT_0107_043(인물 · 에피소드 · 블록)`)

- Row in 프로젝트 설정 › `스크립트 추가 칸`: `ID 형식` · `HRT_0107_043 · 인물 · 에피소드 · 블록 · 내보내면 고정` · `바꾸기 ›`.
- Default format = `{character voice code}_{episode code}_{block number}`; sample `HRT_0107_043` = 하르트 (`HRT`) · 1장 7번째 에피소드 (`0107`) · block `#043`. Choice options append `_{option index}` (`HRT_0107_045_1`).
- The character voice code comes from the cast area. How the episode code is built for other structures, and the editor for the format, are not specified (Q-06). A `직접` (manual) mode exists (rule 12 §6).
- Changing the format: automatic IDs follow at once; fixed IDs stay; the setting shows the count `고정 ID {n}개는 그대로` and a button `고정 ID도 새 형식으로` ([PROPOSED] which re-fixes them in the new format and records each old string as `버린 ID`, one undo step).

### SCR-25 · Optional script fields: `무대 위치`, `보이스 ID`
**Priority** P1 · **needsExternal** no · **Boards** `ProjSettings`, `WideProps` · **Sources** concept memory SETTLED QUESTIONS; planning 범위; `scr_decide.py`

- 프로젝트 설정 › `스크립트 추가 칸`:
  - `무대 위치` · `왼쪽 · 가운데 · 오른쪽` · toggle, default **off**.
  - `보이스 ID` · `대사마다 녹음용 ID` · toggle, default **on**.
  - `ID 형식` (SCR-24) and `전환 문구` (SCR-48).
- 무대 위치 on: the 속성 tab shows a segmented control `왼쪽 | 가운데 | 오른쪽` for dialogue blocks; [PROPOSED] no value until chosen; exported as `stage`.
- 보이스 ID off: the 속성 row and CSV/JSON voice values are hidden. [PROPOSED] Fixed IDs and discarded IDs stay in the data and come back when the toggle is turned on again.

### 2.6 Display layers and preview

### SCR-26 · `표시` sheet (display layers)
**Priority** P1 · **needsExternal** no · **Boards** `Layers` · **Sources** planning 공통 개념 표시 레이어; `scr_script.py layers` (newest copy, includes `전환 문구`); rule 21 §2 (`용어 후보` toggle, default off); rule 22 §3 (씬 머리 늘 보임)

- Opened by the layers icon (header or command row, `aria-label="연출 · 메모 표시"`).
- Sheet: title `표시`, right `닫기`; subline `보기에만 적용 · 파일 내용은 그대로`. Content is never removed from the file; layers only change what is drawn.

| Row | Description | Default | Hides |
| --- | --- | --- | --- |
| `연출 지시` | `배경 · BGM · 효과음 줄` | on | direction blocks |
| `작가 메모` | `⌁ 로 시작하는 메모` | on [PROPOSED; the `Layers` board draws it off, while `Main` shows memos] | memo blocks |
| `시스템 텍스트` | `[기록] 같은 화면 문구 · 전환 문구` | on | system-text blocks and the transition line under each scene head (the scene head itself always stays) |
| `변화 표식` | `◆ 변화 · ◇ 첫 공개` | on | `◆` `◇` glyphs in the manuscript, including dotted ones (panel rows stay) |
| `호칭 힌트` | `대사 위 호칭 표시` | off | SCR-29 |
| `괄호 지문` | `대사 속 (…) 표정 · 행동` | on | parenthetical runs |
| `용어 후보` | [PROPOSED] `점선 밑줄 · 잇기 · 아님` | off | candidate underlines (SCR-56) |

- Section `미리 보기` with chips `쓰기` (default) · `대사만` · `낭독` (SCR-27, SCR-28).
- `▲` margin marks are never hidden by a layer [PROPOSED].
- [PROPOSED] Inserting a block of a hidden kind turns that layer on.
- Where layer choices are stored: Q-14 (they are view state, not file content; app settings are not written into `.civilight`).

### SCR-27 · Preview modes `쓰기` and `대사만`
**Priority** P1 · **needsExternal** no · **Boards** `Layers` · **Sources** planning 공통 개념 (`미리 보기: 쓰기 / 대사만 / 낭독`)

- `쓰기`: the normal editable manuscript.
- `대사만`: [PROPOSED] a read-only view of the same episode showing only dialogue blocks (speaker label + line) and scene heads; tapping a line returns to `쓰기` at that block.

### SCR-28 · Preview mode `낭독`
**Priority** P1 · **needsExternal** no · **Boards** `Layers` · **Sources** planning 공통 개념; vocabulary memory (`낭독` is an open naming question)

- The chip exists in v1. No source says what it does (read-aloud with speech synthesis, or a reading layout). Behaviour is blocked on Q-08; until then the chip is shown disabled [PROPOSED]. If it turns out to be speech synthesis, set needsExternal = yes (Android TTS).

### SCR-29 · `호칭 힌트` (address-form hint)
**Priority** P1 · **needsExternal** no · **Boards** `Layers` · **Sources** planning 인물 말투 (`상대별 호칭`, `호칭 힌트는 스크립트에서 켜고 끄는 옵션`); scope table (v1 포함); rule 13 (호칭 is a per-side field of the relation with `◆` history)

- Layer toggle `호칭 힌트` · `대사 위 호칭 표시`, default off.
- When on, a small line above a dialogue block shows how the speaker addresses others, read from the speaker's side of each relation at that block (rule 13 §4), e.g. `리엔을 ‘꼬마’` above a 하르트 line [PROPOSED format, mirrors the 참고 tab row].
- Which addressees are shown is not specified (Q-09). [PROPOSED] the other characters of the scene who have a 호칭 value.

### 2.7 Side panel and cover sheets

### SCR-30 · Swipe-left tab buttons (`시간 · 참고 · 속성`)
**Priority** P1 · **needsExternal** no · **Boards** `Main`, `MSwipe`, `Tour2`, `Walk1` · **Sources** feedback-no-guessing (swipe exception, corrected); planning 스와이프; DS SwipeTabs; motion spec

- Swiping a block **left** slides it 192 px and reveals three buttons on `raise`: `◆ 시간` (the `◆` and label are `acc` when the block carries a `◆`), `참고` (speech-bubble icon), `속성` (list icon). Right swipe does nothing.
- The swipe only reveals buttons; nothing executes, even when swiped to the end. Content is never shown inside the swipe.
- Tapping a button selects the block and opens the side panel (inner) or bottom sheet (cover) at that tab.
- Off for the block being edited.
- Motion: slide 260 ms; buttons appear 60 ms apart; [PROPOSED] released past half of 192 px the row stays open until a button, another block, or scrolling closes it; otherwise it returns ("놓으면 제자리", see Q-02).
- Tap alternative: planning says a tap opens the same row; how this coexists with tap-to-edit is Q-02.
- Applies to every block kind [PROPOSED]; not to scene heads.

### SCR-31 · Panel / sheet container and selection following
**Priority** P1 · **needsExternal** no · **Boards** `CSheetTime`, `CSheetRef`, `CSheetProps`, `CSheetMods`, `Wide`, `WideRef`, `WideProps`, `WideMods` · **Sources** concept memory (side panel tabs 시간 · 참고 · 속성 · 모듈); DS Sheet, Tabs; planning 옆 패널

- Cover: bottom sheet whose top sits at y = 400 on an 844-high screen; top radius 28; grab handle 36×4; tabs row `시간 · 참고 · 속성 · 모듈` (text tabs, active tab underlined 2 px `acc`) and `닫기` on the right; shade over the manuscript; the selected block stays visible above the sheet.
- Inner: the 290 px side panel (SCR-03) with the same tabs and `닫기`.
- The content always describes the **selected block** (and its scene). Selecting another block updates the content in place on the same tab.
- [PROPOSED] Last tab and per-tab scroll position are remembered per project; folding/unfolding keeps tab and scroll (SCR-66).
- [PROPOSED] No block selected: tabs `시간 · 모듈` show scene content of the scene under the header; `참고 · 속성` show `고른 블록 없음`.

### SCR-32 · `시간` tab
**Priority** P1 · **needsExternal** no · **Boards** `CSheetTime`, `Wide`, `RSOrphan` · **Sources** `scr_core.py p_time` (newest copy); gen_nm `Wide`; rule 22 §7 (transition state word); rule 11 §3

1. Section `고른 블록 · #{n}` (inner: `고른 블록 · #043 하르트`). One card per record anchored on this block:
   - `◆` card: title (`acc`, 14/600) `◆ {kind} · {subject}` e.g. `◆ 관계 · 리엔의 마음 → 하르트`; subline `{before} → {after} · {episode} #{n}부터` e.g. `만나기 전 → 경계 · 1-7 #043부터`. Inner adds buttons `고치기` (opens the record's editor in the cast area) · `관계도에서 보기` (relation records only; opens the cast relation map).
   - Orphaned record card: dotted `◆`, subline `근거 #{old} 지워짐 · #{n}로 옮겨 옴`, buttons `다른 블록에 잇기 · 이대로 두기 · 기록 지우기` (SCR-43).
   - `◇` card: `◇ 첫 공개 · {term}` + `이 블록 · {episode} #{n}`.
   - No record: [PROPOSED] `변화 없음`.
2. Section `이 씬 · 장소와 때`, right side `{전환 문구 상태} · 바꾸기 ›`, where the state reads `전환 문구 없음`, `전환 문구 자동`, or `전환 문구 직접` [PROPOSED wording for the latter two; board shows `전환 문구 없음`]. `바꾸기 ›` opens the place & time area's `씬 · 장소와 때` sheet. Below: `{episode} · {N일째} · {band}` (serif bold 15) + ` · {place}` (small), e.g. `1-7 · 2일째 · 새벽 · 결사의 문`; line `{base text} · {precision} · 이야기 약 {d}일째`, e.g. `직전 씬 +1일 · 정확 · 이야기 약 13일째`; if the travel check has a warning for this scene, a `warn` line `▲ {character} 이동 시간 · 점검 센터` (tap → check center, travel filter).
3. Section `이 씬 안`: in manuscript order, background cues and blocks with their record state: `05:40 배경 · 하층 거리` (scene clock or band + first background cue), `#042 · 변화 없음`, `◆ #043 · 관계`, `◇ #044 · 용어 첫 공개` (inner: `◇ #044 용어 첫 공개 · 오리지늄 등불`). The selected block's row is highlighted. Point-seat records appear here as a line `◆ 1-5부터 · 근거 블록 없음` + `블록에 잇기` above the row of the unit's first block (SCR-42). [PROPOSED] Tapping a row selects that block and scrolls the manuscript to it.

### SCR-33 · `참고` tab
**Priority** P1 · **needsExternal** no · **Boards** `CSheetRef`, `WideRef`, `MFoldScript` · **Sources** `scr_core.py p_ref`; rule 11 §2–§3 (block-level, `여기서` row); rule 13 §3 (viewed from the speaker's side; `만나기 전`)

- Header `화자 · {speaker} · 이 블록 기준` (e.g. `화자 · 하르트 · 이 블록 기준`).
- Rows: `말투` (summary from the cast 말투 tab, e.g. `반말 · 짧게 끊음`), one `{other}을/를` row per address term (e.g. `리엔을 · ‘꼬마’`), `표시 이름` (`??? (1-9 공개)`).
- `최근 대사`: the speaker's previous dialogue lines before this block, newest first, each `“{line}”` + location `1-7 씬 2` ([PROPOSED] 2 lines).
- `관계 · 지금`: one row per counterpart, from the speaker's side: `리엔 · → 보호 · ← 경계` (`→` = speaker's 마음, `←` = the other's 마음), `세라 · 사이 동료` (사이 only).
- **Block-level time rule.** Every value is the state just **before** the selected block; a change made by the selected block itself is shown as its own row `{before} → {after} ◆ 여기서`. Example (rule 11 §2 with rule 13 wording): selecting `#042` → `만나기 전`; `#043` → `만나기 전 → 경계 ◆ 여기서`; `#044` → `경계`.
- Hidden counterparts collapse into `숨긴 인물과의 관계 {n}`; deleted counterparts appear as one dotted row at the end (cast area rules 19/20).
- Non-dialogue block: Q-15.
- *Source note.* Rule tabs 11 and 12 say `모르는 사이`; rule 13 (confirmed later) replaced it with `만나기 전` (not a relation kind). The board `CSheetRef` shows the end state (`← 경계`) for `#043`; the rule-11 behaviour above wins.

### SCR-34 · `속성` tab
**Priority** P1 · **needsExternal** no · **Boards** `CSheetProps`, `WideProps` · **Sources** `scr_core.py p_props`, `scr_script.py wideprops`; planning 옆 패널 속성; rule 12 §6; rule 20 §2

- Inner shows the section `고른 블록 · #{n}`. Fields (label 12 `ink3` above a 44–46 px field box with `edge` outline):
  - [PROPOSED] `종류`: current kind + `바꾸기` (SCR-13). Not on the boards.
  - `화자`: avatar + name + `바꾸기` (→ SCR-19). Deleted speaker: dotted `지운 인물 · {name}` + buttons `화자 바꾸기 · 되살리기 · 이대로 두기` (SCR-57).
  - `표시 이름`: resolved display name + right note, e.g. `???` · `1-9에서 공개` (inner board: `1-9에서 이름 공개`). [PROPOSED] Tap opens the character's 표시 이름 history in the cast area.
  - `무대 위치` (only if enabled, SCR-25): `왼쪽 | 가운데 | 오른쪽`.
  - `보이스 ID` (only if enabled): mono ID + state `자동` / `고정 · 11월 20일 내보냄`; manual format → editable.
  - `변화 연결`: `◆ 관계 변화 · 리엔 → 하르트` (`acc`), one line per record on this block; [PROPOSED] tap → `시간` tab.
- Non-dialogue blocks show only the fields that apply [PROPOSED]: memo with a recorder source shows `출처 · 레코더 {date time}` (SCR-59).

### SCR-35 · `모듈` tab and `씬 메모`
**Priority** P1 · **needsExternal** no · **Boards** `CSheetMods`, `WideMods` · **Sources** `scr_core.py p_mods` (newest copy incl. recorder link); planning 모듈; rule 21 §8 (◇ 후보 not counted in ▲ badge); rule 22 §5; rule 23 §4

Cards (`tint`, radius 14), in user order:
- `오늘`: today's written characters `1,240자`, a 4 px progress bar (`acc` on `line`), `목표 2,000자`. Counting rule and where the goal is set: Q-16.
- `이 씬 분량`: per-speaker counts in this scene, e.g. `리엔 12 · 하르트 7 · 나레이션 4` (planning: 화자별 줄 수). [PROPOSED] counts dialogue blocks per speaker and narration blocks.
- `점검`: `▲ 공개 전 사용 1 · 화자 없음 1` (warn). [PROPOSED] counts of this scene's `▲` items by kind; `◇ 공개 전 후보` is not included; tap → check center.
- `씬 메모`: the scene's free-text memo (serif 14), editable in place [PROPOSED]; right link `레코더 · 정리 안 됨 {n} ›` when recorder records linked to this scene are unsorted (→ recorder inbox). Merging scenes appends lines (SCR-51); the recorder's `⋯ › 메모 칸에 넣기` appends one line.
- Footer `카드 순서 · 추가는 ⋯`: cards can be reordered; more cards are added from ⋯. The set of other cards is not specified (Q-16).

### 2.8 Select, move, delete, trash, clipboard

### SCR-36 · Block select mode (`블록 고르기`)
**Priority** P1 · **needsExternal** no · **Boards** `RSSelect` · **Sources** rule 12 §3 (옮기는 방법), §8 (화면 not drawn then); `scr_new_scene.py rs_select`

- Entry is not drawn; Q-17. [PROPOSED] the command `블록 고르기` (SCR-68). No long-press or other hidden gesture.
- Header: position line, title `블록 고르기`, right `모두` (selects every block of the current scene [PROPOSED scope]) and primary `완료` (leave the mode).
- Each block becomes a row: left 52×44 checkbox (rounded square, check mark when on; `aria-label="고르기"`), body = label line `{label} #{n}` + markers (label: speaker name, `연출`, `나레이션`, `선택지`, `시스템`, `작가 메모`) and the block text, right a grip handle (`aria-label="끌어서 옮기기"`). Checked rows have `sel` background. Scene heads stay visible as dividers and cannot be checked [PROPOSED].
- Bottom glass bar (124 px): `고른 블록 {n}개` and right `#{first} ~ #{last} · ◆ {a} · ◇ {b} 같이 감` (first/last in manuscript order; counts of records riding along), then four buttons `옮기기 · 지우기 · 잘라내기 · 복사`.
- `/씬` while blocks are checked splits below the last checked block (SCR-49).

### SCR-37 · Move blocks
**Priority** P1 · **needsExternal** no · **Boards** `RSSelect`, `UndoToast` · **Sources** rule 12 §3 (confirmed)

- **Handle drag**: dragging a row's grip moves the checked blocks (or that one block when unchecked) [PROPOSED]; drop between any two blocks of the episode, including across scene heads; the list auto-scrolls near the edges.
- **`옮기기`**: opens a destination picker built from the scene list (chapters › episodes › scenes) and a position inside the chosen scene; not drawn (Q-17).
- Records anchored to moved blocks move with them; their story point changes accordingly. Block moves show a toast **after** the move (scenes get a preview before, SCR-54).

| Destination | Records | Number | Voice ID | Toast |
| --- | --- | --- | --- | --- |
| Same scene | Move with block | Unchanged | Unchanged | [PROPOSED] none |
| Other scene, same episode | Move with block | Unchanged | Unchanged | `◆ {n}개 씬 3 → 씬 4` (only if n ≥ 1) |
| Other episode | Move with block | Destination episode's next numbers, in manuscript order of the moved blocks | 자동: new · 고정: unchanged + `◇ 에피소드와 다름` | `◆ {n}개 1-7 → 1-8` |

- A first reveal moved behind uses of the term: the toast adds `◇ 첫 공개 1-7 → 1-8 · ▲ 새로 {k}` (k = links that become pre-reveal uses).
- Moving past the writing line (집필 선) makes the records 예정; moving them back before it applies them again (rule 11 §1). Nothing needs re-linking.
- Undo via the toast or SCR-17.

### SCR-38 · Delete blocks
**Priority** P0 · **needsExternal** no · **Boards** `UndoToast`, `RSSelect` · **Sources** rule 12 §2, §4, §7 (confirmed); vocabulary (`지우기` = trash; toast `블록 #043 지움`)

- Ways to delete: select mode `지우기`; backspace inside an empty block; erasing a block's content then backspace.
- Never asks for confirmation.
- Non-empty block → trash for 30 days (SCR-39). Empty block → removed with no toast and no trash entry.
- Records anchored on the block are relocated, never deleted (SCR-43). Fixed voice ID → `버린 ID`.
- Toasts (ink surface, `bg` text, one action `되돌리기` with undo icon, shown above the bottom bar):
  - One block with records: `블록 #043 지움 · ◆ 1개는 #044 자리로`.
  - One block without records: [PROPOSED] `블록 #043 지움`.
  - Several blocks: `블록 5개 지움 · ◆ 2개 다음 자리로`.
  - [PROPOSED] Only first reveals: `블록 #044 지움 · ◇ 1개는 #045 자리로`; both kinds: `블록 #043 지움 · ◆ 1 · ◇ 1은 #045 자리로`; no surviving block anywhere after: `… · ◆ 1개는 예정으로`.

### SCR-39 · Block trash and restore
**Priority** P1 · **needsExternal** no · **Boards** `Trash`, `RSOrphan` · **Sources** rule 12 §7 (confirmed); rule 20 §5 (trash filters incl. `세계`); `scr_states.py trash`

- Each non-empty deleted block is kept 30 days with: scene key, episode key, previous and next neighbour keys, its ordinal position in the scene, number, voice state, deletion time.
- The trash screen (trash area) shows blocks grouped **per scene per day**: title `1-7 씬 3 · 블록 4개`, sub `블록 · 잿빛 새벽의 등불 · 오늘 지움`, right `30일 남음`, buttons `되살리기 · 영구 삭제`. Expanding a group lists each block's first line with its own `되살리기`. Filter chip `블록`.
- Blocks that were inside a deleted scene belong to that scene's trash entry, not to block groups [PROPOSED].
- **Restore position**: between the remembered neighbours; if both neighbours are gone, at the same ordinal position in its scene; if its scene is in the trash, the user picks `씬도 되살리기` or `다른 씬 끝에`. When something else now sits between the two neighbours: Q-18.
- A restored block keeps its number and voice state (`자동`/`고정`); for a fixed ID, the `버린 ID` entry created by the deletion is removed again [PROPOSED consequence of "상태는 지우기 전 그대로"]. Records relocated because of its deletion re-attach to it unless the user chose `다른 블록에 잇기` or `이대로 두기` in the meantime.
- After 30 days or `영구 삭제`, relocated records stay where they are; [PROPOSED] the panel note `#043 · 휴지통 · 30일 · 되살리면 다시 이어짐` disappears and the record keeps its dotted mark until resolved.

### SCR-40 · Copy, cut, paste
**Priority** P1 · **needsExternal** no · **Boards** `RSSelect` · **Sources** rule 12 §3–§4; rule 22 §4 (pasted transition-like lines)

- `복사`: puts the checked blocks on the app clipboard. Pasting inserts copies below the current block: new keys, the destination episode's next numbers, automatic voice IDs; records are **not** copied; toast `◆는 복사되지 않음` when any copied block had records.
- `잘라내기`: treated as a move (keys kept, records ride along, numbers/voice per SCR-37 when pasted). Behaviour when cut blocks are never pasted: Q-19.
- Pasting plain text from outside the app: Q-19. A pasted line such as `— 씬 2 · 기록실 —` becomes a narration block and never splits the scene (rule 22 §4).

### 2.9 Change records `◆` and first reveals `◇` on blocks (rule 12)

### SCR-41 · `◆` change-record sheet from a block (`변화 기록`)
**Priority** P1 · **needsExternal** no · **Boards** `ChangeCreate` · **Sources** `scr_core.py change_create`; rule 12 §1 (seat "블록"); planning 공통 개념 변화 기록; rule 13 (사이 · 마음 · 호칭)

- Opened from the keyboard toolbar `◆` (SCR-16) for the current block. [PROPOSED] Also from a `◆ 변화 기록` row at the end of the `시간` tab's block section.
- Sheet: title `변화 기록`, right `취소`; subline `#{n} · {episode} 씬 {ordinal}` (e.g. `#043 · 1-7 씬 3`); segmented `관계 | 필드 | 용어 공개`.
- **관계** (drawn): two character chips `리 리엔 · 하 하르트` (tap a chip → speaker picker to choose that side; [PROPOSED] the block's speaker fills one side, the other starts as `인물 고르기`); `바꾸는 것` chips `{A}의 마음 · {B}의 마음 · 사이 · 호칭`; `값 · {current value} →` (e.g. `값 · 만나기 전 →`) with chips from the project's relation kinds (마음 kinds for 마음, 사이 kinds for 사이; sample `경계 · 신뢰 · 의지 · 동경 · 불신 · + 새 종류`) [PROPOSED: a text field for 호칭]; rows `시작 시점 · 이 블록 · 1-7 #043` and `반영 · 인물 · 관계도 · 시점 렌즈`; primary `기록`.
- **필드** and **용어 공개** tabs are not drawn [PROPOSED]: 필드 = character chip → field (project fields and `표시 이름`) → new value with the field type's editor; 용어 공개 = term picker → sets that term's `◇ 첫 공개` to this block (if it already has one elsewhere, the sheet shows `첫 공개 {old} → {episode} #{n}` before `기록`).
- `기록` creates the record with seat **block** = this block's key. It applies from this block inclusive (rule 11 §2). The `◆` appears on the block at once. Value semantics, history lists and lens effects belong to the cast/world areas.
- A choice block's option lines cannot be chosen; the record anchors to the whole choice block. Scene heads cannot hold records.

### SCR-42 · Markers and point-anchored record lines
**Priority** P1 · **needsExternal** no · **Boards** `Main`, `Wide`, `RSOrphan` · **Sources** rule 12 §1 (three seats); DS README 대비 규칙 (dotted = attached to something deleted); 규칙 index decisions 2026-11-22

- `◆` (`acc`) sits at the right end of a dialogue's speaker line, or at the end of a narration/system line. `◇` (`ink3`) sits at the end of the text line. A record whose anchor block was deleted is drawn with a **dotted outline** (`◆` outline with dash 2/1.8, same color); the same for `◇`.
- A record on a block applies from that block, inclusive (`1-7 #043부터`).
- **Point seat** (records created by `시점 추가` in a profile, `여기서부터 바뀜` at a past lens, a canvas line's start point, a term's `공개 예정 2-5`): shown in the **script panel** (rule 12 §1 column `스크립트 패널에서`), not in the manuscript: in the `시간` tab's `이 씬 안` list, one line above the row of the unit's first block: `◆ 1-5부터 · 근거 블록 없음` with a button `블록에 잇기` (opens the same candidate list as SCR-43 `다른 블록에 잇기`; choosing a block converts the seat to block). [PROPOSED] For an episode point the line appears in the `이 씬 안` list of that episode's first scene; for a unit with no blocks, at the top of its scene's list.
- **Block seat** records appear in the panel as that block's `◆` (`시간` tab cards and `이 씬 안` rows) and as the mark on the block in the manuscript.
- **World-event seat** records are never shown in the script.
- All marks and point lines are hidden by the `변화 표식` layer (SCR-26).

### SCR-43 · Anchor deleted: `근거 지워짐`
**Priority** P1 · **needsExternal** no · **Boards** `RSOrphan`, `UndoToast`, `Check` · **Sources** rule 12 §2 (confirmed), §7; rule 21 §5 (근거 지워짐 for first reveal); planning 점검 센터 table

- Deleting a block never asks and never deletes its records. Each record anchored to it moves to the **next surviving block**, searched in this order: (1) next block of the same scene, (2) first block of the next scene, (3) first block of the next episode, (4) none → the record becomes **예정** (beyond the writing line, rule 11 §1). Records keep the original block key and are flagged `근거 지워짐`.
- Never moved to an earlier block or "to the start of the scene" (that would hide warnings).
- Display: dotted `◆`/`◇` at the new block. The `시간` tab card reads e.g. `관계 · 리엔의 마음 → 하르트` / `근거 #043 지워짐 · #044로 옮겨 옴` with buttons `다른 블록에 잇기` · `이대로 두기` · `기록 지우기`, and under the cards `#043 · 휴지통 · 30일 · 되살리면 다시 이어짐`.
- `다른 블록에 잇기`: a candidate list of blocks; the block immediately before the current anchor is first (so re-anchoring to a just-rewritten line takes two taps). [PROPOSED] The other blocks of the scene follow in manuscript order (Q-21). Choosing one anchors the record there; the flag and the remembered original key are cleared.
- `이대로 두기`: the record stays on the current block; the remembered original key is cleared (restoring the old block no longer re-attaches). Whether the dotted mark stays: Q-20.
- `기록 지우기`: the record goes to the trash (filter `변화`, 30 days).
- Doing nothing: the check center shows `◇ 근거 지워진 변화 {n}` (filter `변화`) with the same three buttons; with several, `모두 지우기`.
- Undo of the deletion, or restoring the block from the trash, re-attaches every still-flagged record to the original block and clears the flag.
- A deleted first-reveal anchor keeps working for `▲` comparisons from its new block; `▲` rows show a dotted `◇ 근거 지워짐` (rule 21 §5).
- Deleting a scene relocates all its records to the first block of the next scene (then next episode, then 예정); toast `씬 3 지움 · ◆ 3 · ◇ 1은 씬 4 처음으로` (SCR-52).
- Recorder links and recorder-inserted memos do not relocate (rule 23).

### SCR-44 · Consequences of block operations (records · number · voice ID)
**Priority** P1 · **needsExternal** no · **Sources** rule 12 §3, §4, §6 (confirmed)

Single reference table; the features above implement it.

| Operation | `◆` / `◇` records | Block number | Voice ID |
| --- | --- | --- | --- |
| Edit text inside the block | Unchanged | Unchanged | Unchanged |
| Change speaker | Unchanged | Unchanged | 자동: new · 고정: `◇ 화자와 다름` |
| Change kind (dialogue → narration) | Unchanged (records attach to content, not kind) | Unchanged | 자동: gone · 고정: `버린 ID` |
| Split (Enter mid-line) | Stay on the front block | Back block: next number | Front keeps; back: new 자동 |
| Merge (Backspace at start) | Later block's records → merged block, not flagged; toast `◆ 1개 #042로` | Front number | Front keeps; later 고정 → `버린 ID` |
| Delete empty block | None to move; no toast, no trash | — | — |
| Delete non-empty block | Relocated + flagged (SCR-43) | Never reused | 고정 → `버린 ID` |
| Delete several blocks | Per block; one toast `블록 5개 지움 · ◆ 2개 다음 자리로` | — | Per block |
| Copy · paste | Not copied; toast `◆는 복사되지 않음` | New numbers | New 자동 |
| Cut · paste | As move | As move | As move |
| Move within scene | Move along (point shifts a few lines) | Unchanged | Unchanged |
| Move to other scene (same episode) | Move along; toast `◆ 1개 씬 3 → 씬 4` | Unchanged | Unchanged |
| Move to other episode | Move along; toast `◆ 1개 1-7 → 1-8` | Destination's next number | 자동: new · 고정: `◇ 에피소드와 다름` |
| Move a scene within the episode | Move along | Unchanged | Unchanged |
| Move a scene to another episode | Move along | All blocks get new numbers | 자동: new · 고정: `◇` |
| Renumber episode | Unchanged | 1…N in manuscript order | 자동: new · 고정: unchanged, no `◇` |
| Split scene | Stay on their blocks; only location names change | Unchanged | Unchanged |
| Merge scenes | Stay on their blocks; location names change (`1-7 씬 4 #046 → 1-7 씬 3 #046`) | Unchanged | Unchanged |

Scene time (rule 11 §4): moving a block has no time effect. Moving a scene keeps its time; out-of-order results raise `◇` `때 확인` (time area).

Other things attached to blocks (rule 12 §1 table):
- Check `맞음` marks stored on a block: deleted together with the block; moved together with it.
- Term/route `쓰인 곳`: computed; a deleted block drops out of the list, a moved block shows at its new place.
- Choice rejoin: computed; always the block that now follows the choice.
- Timeline events and map items' `보이는 시점` point at scenes/episodes, not blocks; only scene moves affect them.

### SCR-45 · First reveal `◇` anchored to a block
**Priority** P1 · **needsExternal** no · **Boards** `Main`, `RSOrphan`, `RSSplit` · **Sources** rule 12 §1 table; rule 21 §5; rule 20 §2 (the first reveal belongs to the term)

- A term's `◇ 첫 공개` is the term's own record (world area) whose anchor follows this spec's block-seat rules (SCR-42…44).
- Set from the `용어 공개` tab of SCR-41 or from the term card (world area).
- Manuscript order decides `▲`: a link to the term in a block **before** the `◇` block is a pre-reveal use (`▲ 공개 전 사용`); a link in the `◇` block itself counts as after the reveal. Time of scenes is never used.
- Cannot be placed on a choice option line (rule 21 §5; see Q-30).
- Moving the `◇` block behind existing uses: toast `◇ 첫 공개 1-7 → 1-8 · ▲ 새로 {k}` (SCR-37).

### 2.10 Scenes in the manuscript (rule 22)

### SCR-46 · `씬 머리` (scene head)
**Priority** P0 · **needsExternal** no · **Boards** `RSHead`, `RSHeadMenu`, `RTCandidate`, `RTRenumber`, `RTFlashback` · **Sources** rule 22 §2–§3 (confirmed 2026-11-22); `scr_new_scene.py scene_head`; rule 11 §6; rule 18 §1–§2

- Every scene, including the first of an episode, begins with one head line. Scene boundaries come from the story-structure scene list; the head is how the boundary looks in the manuscript.
- The head is **not a block**: no `#` number, no voice ID, cannot be deleted on its own, cannot hold `◆`/`◇`, never part of `blocks[]` in exports.
- Render: 8 px top margin, top hairline (`line`), 10 px padding; left 24 px. Line 1 serif bold 15/22 `ink`: the head text. Line 2 mono 11/16 `ink3`: `전환 문구 · {text} · {자동|직접}` or `전환 문구 · 없음`. Right: a 44 px ⋯ button (`aria-label="씬 {ordinal} 메뉴"`).
- **Head text**, drawn only from scene properties (place/region and resolved time from the place & time area):
  `씬 {ordinal} · {place, region, or 장소 없음} · {time part}`

| Scene properties | Head text |
| --- | --- |
| Place, exact clock (06:10 → band 아침 by rule 18) | `씬 4 · 결사의 문 · 2일째 아침 06:10` |
| Place, band only | `씬 4 · 결사의 문 · 2일째 아침` |
| Approximate time, no clock | `씬 1 · 잿더미 시장 · 약 7일째` (see Q-22) |
| Region | `씬 1 · 하층 구역 · 1일째 새벽` |
| No place, no time | `씬 2 · 장소 없음 · 때 없음` |
| Flashback | `씬 2 · 회상 · 하층 구역 · 1195` (see Q-22 for word order) |

- Time part: `{N}일째` (or `약 {N}일째` when the resolved day is a range) + ` {band}` + ` {HH:MM}` when the time is exact; `때 없음` when the scene has no time. `N일째` counts from the episode's first timed scene (rule 11 §6). The time band of an exact clock follows rule 18 (새벽 04–06 · 아침 06–10 · 낮 10–17 · 저녁 17–20 · 밤 20–04; the day changes at 04:00).
- Tapping the head opens the place & time area's `씬 · 장소와 때` sheet for that scene. The ⋯ opens SCR-47. While the ⋯ sheet is open the head has `sel` background.
- The head is always drawn; only its transition line follows the `시스템 텍스트` layer.
- The caret never crosses a head (SCR-15). A pasted line that looks like a head becomes narration.

### SCR-47 · Scene-head ⋯ menu
**Priority** P1 · **needsExternal** no · **Boards** `RSHeadMenu` · **Sources** rule 22 §3, §4 (`앞에 새 씬`); `scr_new_scene.py rs_head_menu`

- Sheet title `씬 {ordinal} · {episode}` (e.g. `씬 4 · 1-7`). Rows, each with a label and a value line:

| Row | Value line | Action |
| --- | --- | --- |
| `장소와 때` | `결사의 문 · 2일째 아침 06:10` | Open `씬 · 장소와 때` (place & time area) |
| `전환 문구` | `직접 · 결사의 문 앞 · 06:10` | Transition editor (SCR-48) |
| `앞 씬과 합치기` | `씬 3 · 꺼진 등불` (previous scene's ordinal and title) | SCR-51. [PROPOSED] Absent for the first scene of an episode |
| `씬 지우기` | `휴지통 · 30일` | SCR-52 |
| `앞에 새 씬` | [PROPOSED] `빈 씬 · 씬 {ordinal} 앞` | SCR-53 (rule 22 §4; not on the board, Q-31) |

### SCR-48 · `전환 문구` (transition text)
**Priority** P1 · **needsExternal** no · **Boards** `RSHead`, `RSHeadMenu`, `SceneWhere`, `ProjSettings`, `Layers` · **Sources** rule 22 §2 (confirmed); `scr_link.py scene_where` (chip row); `scr_decide.py` (setting row)

- Each scene has one transition text: game text shown to the player at the scene change. It is data separate from the scene's place/time and never their source.
- Modes: `자동` (generated from scene properties; follows them), `직접` (author's text; the app never reads or parses it), `없음` (nothing shown in game). Default for new scenes is `없음`; the project setting can change the default.
- Project setting row (프로젝트 설정 › 스크립트 추가 칸): `전환 문구` · `새 씬 기본 없음 · 틀 장소 · 시각` · `바꾸기 ›`. Template choices: `장소 · 시각` (default) and `장소만`.
- **자동 text** (template `장소 · 시각`; P = place or region name):

| Scene properties | 자동 text |
| --- | --- |
| Place + exact clock | `결사의 문 · 06:10` |
| Place + band only | `결사의 문 · 아침` |
| Approximate time (with or without band) | `잿더미 시장` (place only) |
| Region + band | `하층 구역 · 새벽` |
| No place | not generated; `자동` cannot be chosen |
| Flashback | generated from the flashback scene's own place/time; the word `회상` is never added ([PROPOSED] `하층 구역 · 1195`) |
| Place not yet visible at this scene (map item's visible point is later) | still generated; raises `◇ 공개 전 장소 · 전환 문구` (SCR-62) |

  Template `장소만`: P only. The ordinal (`씬 4`) and `N일째` are never in the text (author-only info). Screen effects (fade, blackout) are direction cues, not transition text. [PROPOSED] Place + no time → P only.
- **직접 text**: free text, e.g. `결사의 문 앞 · 06:10`. When saved, the app stores a snapshot of the scene's place and resolved time; if they later differ, SCR-62 raises `◇ 전환 문구 · 쓴 뒤 때 바뀜` (or `장소`). Switching to `없음` keeps the last direct text; switching back to `직접` restores it until it is overwritten.
- **Editor**: the `씬 · 장소와 때` sheet has a row `전환 문구` with chips `자동 · 직접 · 없음`; head ⋯ › `전환 문구` opens [PROPOSED] a sheet with the same chips, a read-only preview for `자동`, a text field for `직접`, and `닫기`.
- Shown as the second line of the scene head (SCR-46); the `시스템 텍스트` layer hides it.
- Terms in direct text are subject to the pre-reveal check (rule 22 §3); see Q-32 for how links/candidates work inside it.
- Export: JSON scene value, one CSV row per scene with text (SCR-63/64), PDF never prints it [PROPOSED] (PDF prints the head as a title line).

### SCR-49 · `/씬` routing and `다음 씬 만들기`
**Priority** P1 · **needsExternal** no · **Boards** `Blocks` (row `새 씬`) · **Sources** rule 22 §4 (confirmed)

`/씬` (or the `새 씬` row) never inserts a block and never changes anything by itself; it opens a sheet:

| Situation | Sheet |
| --- | --- |
| Current block is the last block of its scene (or the scene has no blocks) | `다음 씬 만들기` |
| Current block is not the last | `여기서 씬 나누기` (SCR-50), split below the current block |
| Blocks are checked in select mode | `여기서 씬 나누기` below the last checked block (the checked range does not become the new scene) |
| Position is on the scene head / above the first block | Only `앞에 새 씬` (SCR-53) |
| Last scene of an episode | `다음 씬 만들기` creates the scene inside the same episode; a new episode is made in story structure |
| Read-only project | Row dimmed with `읽기 전용` |

`다음 씬 만들기` sheet (fields from rule 22, layout [PROPOSED] like `RSSplit`): title `다음 씬 만들기`, sub `{episode} · 씬 {n} 뒤`, `닫기`; fields `제목` (`미정` · `쓰기 ›`), `장소` (previous scene's place, `· 씬 {n}과 같음`, buttons `비우기` · `바꾸기 ›`), `때` (`씬 {n} 기준 · 같은 때`, buttons `비우기` · `바꾸기 ›`), `전환 문구` (project default chips `자동 · 직접 · 없음`); all fields may be skipped; primary [PROPOSED] `만들기`. It performs the same action as story structure's `새 씬`. After creation the new scene's head appears below the current scene and [PROPOSED] an empty dialogue block with the inherited speaker receives the caret. Other scenes' absolute times do not change. See Q-35 for the time default when inserting a scene in the middle.

### SCR-50 · `씬 나누기` (split a scene)
**Priority** P1 · **needsExternal** no · **Boards** `RSSplit` · **Sources** rule 22 §4–§5 (confirmed); `scr_new_scene.py rs_split`

- Sheet: title `씬 나누기`, sub `{episode} 씬 {n} · #{num} 아래`, right `닫기`.
- Two part cards: `씬 {n}` · `블록 {a}개` with the range `‘{label} · {first words}…’ ~ ‘…{last words}’`, and `새 씬 {n+1}` · `블록 {b}개` with its range (sample `‘세라 · 불은 꺼졌어…’ ~ ‘…한 번 깜박였다.’`). [PROPOSED] excerpt = label + first ~8 characters, last ~8 characters.
- Fields for the new scene B (prefilled, editable):
  - `장소` · `{A's place}` · `· 씬 {n}과 같음` · `바꾸기 ›`
  - `때` · `씬 {n} 기준 · 같은 때` · `바꾸기 ›` (interval 0, same precision as A; if A has no time, B has none)
  - `제목` · `미정` · `쓰기 ›`
  - `말 없는 등장 · {name}` · chips `씬 {n} | 새 씬 {n+1} | 둘 다` (default `둘 다`), one row per silent cast member of A
  - If A is a flashback: row [PROPOSED] `회상` with a toggle, on; if A is hidden: row [PROPOSED] `숨김` with a toggle, on.
- `바뀌는 것` rows: one row per following scene of the episode whose ordinal shifts (`순번 · 씬 4 · 미정 → 씬 5`, ordinal only; `미정` = no title); records on blocks that stay in place (`첫 공개 · 오리지늄 등불 · 씬 3 #044 그대로`, `◆ 관계 · 리엔 → 하르트 · #043 그대로`) or that change location (`◆ 관계 · 1-7 씬 3 #043 → 1-7 씬 4 #043`); scenes based on A (`씬 5 때 · 기준 씬 3 · 2일째 아침 06:10 그대로`).
- Primary `나누기`. Nothing changes before it.
- Apply: blocks after the split point move to the new scene B inserted directly after A. Block keys, numbers, voice IDs and records are unchanged (the voice ID has no scene part). Speakers of each scene are recounted from blocks. B: transition `없음`, title none. If A is a fixed point (`직접`), B is `A 기준 · 같은 때` and A stays the only fixed point. If A is the first timed scene of the episode, day 1 stays with A. Scenes whose base was A keep base A and their absolute time. One undo step.

### SCR-51 · `씬 합치기` (merge with the previous scene)
**Priority** P1 · **needsExternal** no · **Boards** `RSMerge` · **Sources** rule 22 §5 (confirmed); `scr_new_scene.py rs_merge`

- Entry: head ⋯ › `앞 씬과 합치기` on scene B, or story-structure scene ⋯ › `앞 씬과 합치기`. Merges B into the previous scene A of the **same episode** only (other episodes: move first).
- Blocked cases (the sheet shows the reason and no merge button [PROPOSED]): only one is a flashback → `회상이 섞임 · 먼저 회상 끄기`; only one is hidden → `숨긴 씬이 섞임 · 먼저 숨김 풀기`.
- Sheet: title `씬 합치기`, right `닫기`, sub `{episode} 씬 {a} + 씬 {b} → 씬 {a}`.
  - `장소`: same → `{place} · 씬 {a} · 씬 {b} 같음`; one empty → the existing value is used without asking; both set and different → radio `씬 {a} 것` / `씬 {b} 것` (A preselected).
  - `때 · 하나 고르기` (when both set and different): radio `씬 {a} 것 · {time}` (preselected) / `씬 {b} 것 · {time}`; note `고르지 않은 때 → 씬 메모 한 줄`. One empty → existing value without asking.
  - If B is a fixed point: [PROPOSED] note `씬 {b} 고정점 사라짐 · 씬 {b} 것을 고르면 직접 유지`.
  - If B has a direct transition text: `전환 문구 · 씬 {b} 직접` + `‘{text}’` + segmented `시스템 텍스트로 남기기 | 지우기` (first preselected).
  - `바뀌는 것`: `씬 {c} 때 · 기준 씬 {b} → 씬 {a} · 간격 다시 적음 · 절대 때 그대로` for each scene based on B; `제목 · {A title} · 씬 {b} 제목 없음`, or when B has a title [PROPOSED] `제목 · {A title} · 씬 {b} 제목 {B title} → 씬 메모`; `기록 · 블록에 그대로 · 위치 이름만 씬 {b} → 씬 {a}`.
  - Primary `합치기`.
- Apply (one undo step restores both scenes):
  - B's blocks follow A's blocks. A keeps its key, title, flags.
  - Unchosen differing values become one memo line in A's 씬 메모: `합치기 전 · 씬 {b} · {place} · {time}` (sample `합치기 전 · 씬 4 · 결사의 문 · 2일째 아침 06:10`).
  - B's title becomes the first line of A's memo; B's memo is appended after A's.
  - B's direct transition text: `시스템 텍스트로 남기기` inserts a system-text block with that text at the start of B's former blocks (new number from the counter); `지우기` sends the text to the trash [PROPOSED as a trashed system-text block].
  - Choosing `씬 {b} 것` for time rewrites A's base/interval so A's absolute time equals B's; scenes based on A keep their absolute times (their intervals are rewritten). Choosing B's time keeps B's fixed point as `직접`.
  - Scenes based on B switch their base to A and get interval = their absolute − A's absolute (a range when approximate). If A has no time, the base becomes the nearest earlier timed scene (rule 11 §4).
  - Records stay on their blocks; only location names change. Travel checks recompute; warnings that disappear simply leave the check center.
  - [PROPOSED] B's scene record is removed (not trashed); undo recreates it with the same key.

### SCR-52 · `씬 지우기` (delete a scene from the manuscript)
**Priority** P1 · **needsExternal** no · **Boards** `RSHeadMenu`, `Trash` · **Sources** rule 22 §3; rule 12 §2 (씬을 지울 때도 같다), §7; rule 11 §4

- Head ⋯ › `씬 지우기` (`휴지통 · 30일`). No confirmation.
- The whole scene (head and blocks) goes to the trash for 30 days (filter `씬`). Following scenes' ordinals decrease by one.
- Records in the scene move to the first block of the next scene (then next episode, then 예정) and are flagged `근거 지워짐`. Toast: `씬 {n} 지움 · ◆ {a} · ◇ {b}은 씬 {n+1} 처음으로` + `되돌리기` ([PROPOSED] `씬 {n} 지움` when there are no records). Ordinals in the toast are the ones before deletion.
- Other scenes' absolute times do not change; a scene that used the deleted scene as base switches to the previous scene and its interval is recomputed. Restoring returns the scene with its own time and recomputes the same way.
- Deleting blocks never merges or deletes scenes; an emptied scene stays as a scene with its head.

### SCR-53 · `앞에 새 씬` (new scene before this one)
**Priority** P1 · **needsExternal** no · **Sources** rule 22 §4 (case table); rule 11 §4 (inserted scene starts without time)

- From head ⋯ › `앞에 새 씬` or `/씬` at the top of a scene: opens a sheet that creates an empty scene directly before this scene. [PROPOSED] Same fields as `다음 씬 만들기` but nothing prefilled (`장소 없음`, `때 없음`, rule 11 §4), primary `만들기`.
- Other scenes' absolute times do not change; the next scene's ordinal increases.

### SCR-54 · Scene-move preview (`씬 옮기기`)
**Priority** P1 · **needsExternal** no · **Boards** `RSSceneMove` · **Sources** rule 12 §3 (씬 옮기기 미리 보기, confirmed); `scr_new_scene.py rs_scene_move`

- Opened by story structure when the user moves a scene (the drag/target UI is the story-structure area's). Nothing changes before `옮기기`.
- Sheet: title `씬 옮기기`, sub `미리 보기 · 옮기기 전에는 그대로`; a card `{episode} · 씬 {n}` / `{title}` › `{destination episode}` / `{position} · 씬 {m}` (e.g. `1-7 · 씬 3 · 꺼진 등불` › `1-8 · 맨 앞 · 씬 1`).
- `바뀌는 것` rows:
  - `기록` · `◆ {a} · ◇ {b} · 시점 {from} → {to}` (e.g. `◆ 2 · ◇ 1 · 시점 1-7 → 1-8`)
  - `블록 번호` · `블록 {k}개 · {to} 새 번호` (planning text adds the first new number: `새 번호 1-8 #061~`); same episode → `그대로`
  - `보이스 ID` · `고정 {f}개 그대로 · ◇ 에피소드 부분 다름`; same episode → `그대로`
  - `때` · `그대로 · ◇ {o} 순서와 어긋남` (o = scenes whose time order no longer matches manuscript order)
- Buttons `취소` · `옮기기`. Apply per SCR-44; the scene keeps its key and its time.

### 2.11 Other areas' states inside the script

### SCR-55 · `@` links in block text
**Priority** P1 · **needsExternal** no · **Boards** `Main` (linked `오리지늄 등불`), `EditKeyboard` (`@`) · **Sources** planning 공통 개념 @ 링크; rule 21 §2 (alias insertion, particles outside), §8 (@ picker finds aliases); rule 19 §6; rule 20 §2; 저장 파일 양식 §4

- Typing `@` or the toolbar `@` opens the link picker (people, terms, places; search by name, 초성 and aliases). An alias result reads `오리지늄 · 오리지늄 등불의 별칭`; choosing it inserts the alias text.
- A link is a run holding the target key and the visible text; particles stay outside (`[오리지늄]은`).
- Render: link text in `ink` with a 1 px `ink3` underline. [PROPOSED] Tapping a link when not editing opens the target's card from its area.
- Locked hidden target: the link text is drawn as `숨긴 인물` / `숨긴 항목` and term emphasis is removed; unlinked plain text is never masked. The picker omits locked hidden items and ends with `숨긴 인물 {n} · 지문으로 보기`.
- Deleted target: dotted underline + `◇`; tapping shows the card `지운 인물 · 노아` (or `지운 용어 · …`, `지운 장소 · …`) with `다른 항목에 잇기 · 글로 풀기 · 되살리기 · 이대로 두기` (`글로 풀기` keeps the text and removes the link). After permanent deletion the chip reads `영구 삭제 · 노아` and `되살리기` is gone.
- Links in memos and direction cues are listed by the term card under `원고 밖 · 메모` and never create `▲`.

### SCR-56 · Term candidates and pre-reveal `▲` in the manuscript
**Priority** P1 · **needsExternal** no · **Boards** `RTCandidate`, `Check` · **Sources** rule 21 §2–§5 (confirmed); `scr_new_time.py candidate`

- Candidate detection (names + aliases, word start, particle list, `끊김`, longest match, re-scan of the edited block ~0.5 s after typing stops) is done by the world area; this feature renders results.
- Only game-output text is scanned: dialogue, narration, system text, choice options. Not memos, direction cues, parentheticals, display names, scene titles.
- With the `용어 후보` layer on (default off): candidate ranges get a dotted underline, and a small chip under the header reads `표시 · 용어 후보` · `켬` [PROPOSED placement]. Tapping an underline opens a small popover:
  - line 1: `{term}` + tag `후보 · 끊김` (or `후보`)
  - line 2 when linking would create a pre-reveal use: `첫 공개 {episode} · 잇기 시` + `▲ 공개 전 사용`
  - buttons `잇기` (wraps exactly that text range into a link) · `아님` (stores a dismissal: block key + term key + matched text + occurrence index in the block)
- A link that sits before its term's first reveal (manuscript order) shows the link look plus an amber `▲` in the line's margin, always visible regardless of layers; tapping `▲` opens the check-center item.
- Hidden terms are only searched while unlocked.

### SCR-57 · Deleted speakers and deleted record subjects (rule 20)
**Priority** P1 · **needsExternal** no · **Boards** `CSheetProps`/`WideProps` variant (described in rule 20 §7), `Check`, `RCDeleteImpact` · **Sources** rule 20 §2–§5 (confirmed)

- A dialogue block whose speaker character is in the trash keeps the speaker: the label shows the name with a dotted underline and `◇` (not `▲ 화자 없음`). Exports still use the name.
- 속성 tab: `지운 인물 · 노아` (dotted) and buttons `화자 바꾸기` (speaker picker) · `되살리기` (restores the character; all references reconnect) · `이대로 두기` (moves the check item to `맞다고 표시한 것`; the dotted look stays).
- Speaker picker shows `지운 인물 · 노아 · 되살리기` as its first row only for such blocks.
- Voice ID: unchanged; automatic IDs keep using the deleted character's code (from its nameplate).
- Blocks inside trashed scenes are not counted in the character's reference counts.
- After permanent deletion the label reads `영구 삭제 · 노아` with `◇`; only `되살리기` disappears from the actions.
- Records whose **subject** is a deleted item (e.g. a `◆` about 노아's 소속, the `◇` first reveal of a deleted term) go to the trash with the item; the block keeps a dotted mark labelled `대상 지워짐` with `되살리기 · 이대로 두기 · 기록 지우기`.
- Restoring the character reconnects every block except blocks whose speaker the user changed in the meantime (they keep the new speaker).

### SCR-58 · Hidden items and hidden scenes (rule 19)
**Priority** P1 · **needsExternal** no · **Boards** `ItemHide` (list form; script form described in rule 19 §6) · **Sources** rule 19 §5–§6 (confirmed)

- Locked hidden speaker: label `숨긴 인물`; the line text stays as written; plain text that mentions the name is never masked.
- Locked hidden scene: instead of its blocks the manuscript shows one row `숨긴 씬 · 블록 {n} · 지문으로 보기` (e.g. `숨긴 씬 · 블록 12 · 지문으로 보기`); tapping starts the hiding area's unlock (system biometric, fallback device PIN). [PROPOSED] The scene head is replaced by that row as well (story structure shows `1-7 씬 4 · 숨긴 씬` with the title masked).
- Unlocked: the scene renders normally; [PROPOSED] its head shows a small `숨김` tag.
- Hidden status is inherited by a split-off scene and blocks merging (SCR-50/51).
- Check results that involve hidden items collapse into one locked row (check area); a hidden speaker speaking in a visible scene raises the check `숨긴 인물이 보이는 씬에 나옴 · 씬 3` with `씬도 숨기기 · 그대로` (the app never hides the scene itself).

### SCR-59 · Memo blocks inserted from the Life Recorder (rule 23)
**Priority** P1 · **needsExternal** no · **Sources** rule 23 §4 (confirmed); vocabulary memory (`넣기`)

- The recorder's `원고에 넣기` creates exactly one `⌁` author-memo block at the chosen position with a copy of the record's text; it never guesses another block kind. The block gets the episode's next number (inserted after `#044` it may be `#062`). Toast: `1-7 씬 3 · #062 작가 메모 넣음 · 되돌리기 · 가 보기` (`가 보기` opens the script at that block).
- The block stores its source (record key + record time). Its memo prefix reads `⌁ 작가 메모 · 레코더 11월 22일 08:41`; tapping the source opens the record in the recorder (audio plays there; audio is never copied into the manuscript).
- Editing the block never changes the record and vice versa.
- Changing it to dialogue/narration is ordinary kind change (SCR-13).
- Deleting it follows SCR-38/39; it is not relocated like `◆` records.
- **Last writing position.** The script persists, per project, the last caret position (block key + text offset); the recorder's location sheet shows it as `마지막으로 쓰던 곳 · 1-7 씬 3 · #044 뒤`.
- When a recording was started from the script, the recorder's processing screen offers `여기 넣기 · #044 뒤` (recorder area), which performs the same insertion.

### SCR-60 · Read-only mode (example project)
**Priority** P1 · **needsExternal** no · **Boards** `OnbExampleScript`, `Tour1`, `Tour2`, `Tour4` · **Sources** concept memory ONBOARDING; `scr_onb.py a_example_script`; rule 22 §4 (`/씬` dimmed)

- The example project 「잿빛 새벽의 등불」 opened during onboarding is read-only. A banner under the header reads `예시 프로젝트 · 읽기 전용` with `온보딩 후 삭제` on the right; the header line shows `읽기 전용 · 편집 불가`; the bottom shows `예시 홈으로` · `온보딩 끝내기` instead of the command row (onboarding area owns these actions).
- No caret, no keyboard, no insert/edit/move/delete/split/merge/record creation; the block-add rows (including `/씬`) are dimmed with `읽기 전용`. [PROPOSED] Swipe buttons and panels still open for reading.
- *Source note.* `Tour4` (`시점 렌즈`) teaches a lens chip on the script; rule 11 §3 removes the lens from the script, so that tour step must explain the position label instead or be dropped (onboarding area).

### SCR-61 · Asset list (`에셋`) and asset picker
**Priority** P1 · **needsExternal** no · **Boards** `Assets` · **Sources** planning 에셋; `scr_script.py assets`; rule 22 §2 (background ↔ place link)

- Screen: header small line `잿빛 새벽의 등불`, title `에셋`, right `+`. Filter chips `전체 · 배경 · BGM · 효과음 · CG · 안 쓰는 것`. Sections with counts `배경 · 4`, `BGM · 3`, `효과음 · 12`, `CG · 1`.
- Row: 56×40 thumbnail (image for 배경/CG; waveform icon for BGM/효과음), name (serif bold 16), subline `{file name} · {uses}` (`bg_lower_dawn · 이 에피소드 3회`, `bg_guild_gate · 6회`), audio subline `{file name} · {length}[ · 반복]` (`bgm_embers · 2분 41초 · 반복`, `se_lamp_flicker · 0.8초`), right the first episode that uses it in mono (`1-7`). A CG with no image: subline `cg_lamp_out · 아직 이미지 없음`, right `비어 있음` (`warn`).
- `안 쓰는 것` = assets no direction cue references.
- Command-field hint on this screen: `에셋 이름이나 파일 이름으로 찾기`.
- The same list, filtered to one kind, is the picker used when adding a direction cue (SCR-08).
- Not specified: entry point, the `+` add/edit form, whether files are stored, deleting a used asset (Q-29). [PROPOSED] The form has kind, name, file name, optional image (배경 · CG), length and `반복` (BGM · 효과음), and a place link (배경).

### 2.12 Checks

### SCR-62 · Check items produced from script data
**Priority** P1 · **needsExternal** no · **Boards** `Check` · **Sources** planning 점검 센터 table; rule 12 §2, §6; rule 22 §2, §7; rule 19 §5; `scr_tools.py check`

The check center (other area) lists, filters and stores `맞음`; this area computes these items. Every item references blocks/scenes by key.

| Item (copy) | Mark | When | Filter | Buttons |
| --- | --- | --- | --- | --- |
| `▲ 화자 없음` · the line (e.g. `“…누구야?”`) · `대사 블록 {n}개` · location `1-7 씬 2` | `▲` | A non-empty dialogue block has no speaker | `화자` | `화자 고르기` · `나레이션으로 바꾸기` |
| `◇ 근거 지워진 변화 {n}` | `◇` | Records flagged `근거 지워짐` (SCR-43) | `변화` | `다른 블록에 잇기` · `이대로 두기` · `기록 지우기`; `모두 지우기` |
| `◇ 보이스 ID가 화자 · 에피소드와 다름`, item text `형식과 다른 ID {n}` | `◇` | A fixed voice ID's speaker or episode part differs from the current format result | `보이스` | `새 ID로` · `그대로` |
| [PROPOSED] `▲ 보이스 ID 겹침 {n}` | `▲` | Manual (`직접`) format and two blocks share an ID | `보이스` | [PROPOSED] `원고로 가기` |
| `◇ 전환 문구 · 쓴 뒤 때 바뀜 · 1-7 씬 4 · 06:10 → 07:00` (or `쓴 뒤 장소 바뀜`) | `◇` | Direct transition text and the scene's place/time differ from the snapshot taken when the text was saved | undecided (`이동` or new `씬`, Q-24) | `자동으로` · `문구 고치기` · `맞음` |
| `◇ 공개 전 장소 · 전환 문구` | `◇` | Automatic transition text names a place whose map visible point is after this scene | undecided | `직접 쓰기` · `맞음` |
| `▲ 숨긴 씬으로 가는 선택지 {n}` | `▲` | Export with hidden scenes excluded and a choice that leads into one (Q-28) | export sheet | — |

- `자동으로` switches the scene's transition to `자동`; `문구 고치기` opens the transition editor; `직접 쓰기` switches to `직접` prefilled with the automatic text [PROPOSED].
- `맞음` and `이대로 두기` are stored by the check area against the block/scene key, so the same warning does not come back after reopening a file.

### 2.13 Export content (script data)

### SCR-63 · Game-engine JSON: content contract (provisional shape)
**Priority** P1 · **needsExternal** no · **Boards** `Export` · **Sources** rule 22 §6 (confirmed: content decided, format 미정); planning 내보내기 JSON 골격 (제안); rule 19 §5; rule 12 §5–§6; 저장 파일 양식 §1; research §3 (superseded parts dropped)

The engine format is not decided (planning 남은 확인); field names below are the planning skeleton's and are **provisional**. What each scene and block must carry is decided.

```json
{
  "format": "civilight-script",
  "version": "0.1",
  "assets": [{ "id": "<asset key>", "kind": "background|bgm|sfx|cg", "name": "하층 거리(새벽)", "file": "bg_lower_dawn" }],
  "characters": [{ "id": "<character key>", "name": "하르트", "displayName": "???" }],
  "scenes": [{
    "id": "<scene key>",
    "index": 4,
    "title": null,
    "place": { "id": "<place key>", "name": "결사의 문", "kind": "place|region" },
    "time": { "storyDay": { "min": 11, "max": 15 }, "episodeDay": 2, "band": "아침", "clock": "06:10", "precision": "exact|approx" },
    "flashback": false,
    "transition": { "id": "<transition id>", "mode": "direct|auto", "text": "결사의 문 앞 · 06:10" },
    "blocks": [
      { "id": "<block key>", "type": "dialogue", "speaker": "<character key>", "displayName": "마르타", "stage": "left", "text": "문은 해가 뜨면 닫혀. 서둘러.", "voice": "MRT_0107_050" },
      { "id": "<block key>", "type": "narration", "text": "…" },
      { "id": "<block key>", "type": "direction", "cues": [{ "op": "background", "asset": "<asset key>" }, { "op": "bgm.play", "asset": "<asset key>" }] },
      { "id": "<block key>", "type": "choice", "options": [{ "text": "따라간다", "voice": "HRT_0107_045_1" }, { "text": "불빛 쪽을 한 번 더 본다" }], "rejoin": "<next block key>" },
      { "id": "<block key>", "type": "system", "text": "[기록] ‘꺼진 등불’을 얻었다." },
      { "id": "<block key>", "type": "memo", "text": "…", "export": false }
    ]
  }]
}
```

The scene values are those of F1's 1-7 씬 4 (section 4); the `blocks` array shows one block of each kind for illustration only.

Decided content (rule 22 §6 and others):
- Scene: permanent key, ordinal, title; place or region (key + name); **resolved** time (the engine cannot resolve base + interval): days from story start, episode day, band, clock, precision; for approximate time the earliest and latest values; flashback flag; transition text (text, 자동/직접, transition ID derived from the scene key; empty when `없음`); `blocks[]`. The scene head is not a block and never appears in `blocks[]`; there is no `scene.transition` block.
- Block `id` = the hidden permanent key (never the `#` number).
- Dialogue: speaker key, display name in effect at that block, stage position (only when enabled), text, voice ID (only when enabled). Choice: options + `rejoin` = id of the next block (computed). Direction: cues as `op` values (`background · cg · bgm.play · sfx · shake …`). Memo: `export:false`.
- Characters: only speakers that appear, with id, name and display name; no profile data (rule 19 §5). Hidden speakers are exported with id and name (game builds must not break).
- Hidden scenes in range: the export sheet asks `숨긴 씬 {n} · 넣기 · 빼기`; JSON defaults to `넣기`, which requires one biometric confirmation.
- Exporting fixes every exported voice ID (SCR-23).
- Dropped from the research skeleton by later decisions: `expression`, `stage{slots, focus}`, `character.enter/exit/move`.
- Open: units and names of time fields, transition ID string, direction-block shape, parentheticals in `text`, link runs flattening (Q-10, Q-25, Q-26).

### SCR-64 · Dialogue CSV (`대사 표 CSV`): rows from script data
**Priority** P1 · **needsExternal** no · **Boards** `Export` · **Sources** rule 12 §6 (상태 column); rule 22 §6 (transition rows); rule 19 §5; planning 내보내기

- Columns: `화자 · 대사 · 보이스 ID · 블록 ID · 상태`.
- One row per dialogue block in manuscript order within the chosen range; [PROPOSED] spoken choice options as their own rows. `블록 ID` = permanent key. `화자` = [PROPOSED] the character name (rule 19: hidden speakers' names are included).
- One row per scene that has a transition text: `화자` = `전환 문구`, `대사` = the text, `보이스 ID` empty, `블록 ID` = transition ID, `상태` by the same rules.
- `상태` values: `새` · `바뀜` · `버림`. Every discarded ID is exported as a row with `버림` so the recording team knows which files are obsolete. Rows left out because a hidden scene was excluded are not `버림`. Exact meaning of `바뀜`: Q-27.
- Hidden scenes: default `넣기`.
- Exporting fixes the voice IDs written.

### SCR-65 · Script PDF (`대본 PDF`): content rules
**Priority** P1 · **needsExternal** no · **Boards** `Export` · **Sources** planning 내보내기 (`표시 레이어(연출 · 괄호 지문) 포함 여부 선택`); rule 22 §6 (씬 머리 = 제목 줄); rule 19 §5; rule 23 §5

- Each scene starts with its head text as a title line (e.g. `씬 4 · 결사의 문 · 2일째 아침 06:10`).
- Export-sheet row `표시 레이어` · `연출 · 괄호 지문 포함` chooses whether direction blocks and parentheticals are printed; [PROPOSED] memo blocks (including recorder-inserted ones) follow the same layer choices as the `표시` sheet.
- Hidden scenes default `빼기`; rating printed on the cover (export area).

### 2.14 Fold, motion, commands

### SCR-66 · Fold and unfold continuity
**Priority** P1 · **needsExternal** no · **Boards** `MFoldScript`, `FoldSpec` · **Sources** concept memory FOLD/UNFOLD MOTION; planning 접기 · 펼치기

- Cover bottom sheet ↔ inner side panel are the **same element**: the shape changes, the tab and scroll position stay.
- The anchor (selected block, or the caret line) keeps its screen height; blocks above it re-wrap and grow upward.
- Unfold: decelerate 360 ms; fold: accelerate 240 ms; reduce motion: 120 ms crossfade. Glass bars keep their blur during the transition.
- A closed panel stays closed. While typing, caret, keyboard and the typed line position are kept.
- If the device closes apps on fold, the script saves state (episode, selected block, caret offset, open tab, scroll) and restores it on reopen.
- No controls near the hinge.

### SCR-67 · Script motion
**Priority** P1 · **needsExternal** no · **Boards** `MType`, `MSwipe`, `MSheet`, `MotionSpec` · **Sources** motion spec boards; DS README 재질과 움직임

| Motion | Spec |
| --- | --- |
| New block | Enters from 8 px below, 320 ms decelerate `(.05,.7,.1,1)`; speaker = previous speaker |
| Swipe buttons | Slide 260 ms; buttons 60 ms apart |
| Sheet open | 320 ms decelerate; background scales to 96.5 % with 2 px blur |
| Sheet close, delete | 160 ms accelerate `(.3,0,.8,.15)` |
| Tab or value change | 200 ms standard `(.2,0,0,1)` |
| Never moves | The line being typed, the caret position, glass bars while scrolling |
| Reduce motion (system · always · never) | All movement becomes a 120 ms fade |
| Reduce transparency | Bars become opaque `raise` with a `line` border |

### SCR-68 · Script commands and navigation in the command field
**Priority** P1 · **needsExternal** no · **Boards** `Main`, `Search` · **Sources** planning 공통 개념 명령 칸; rule 11 §3 (검색 · 명령의 ‘시점 바꾸기’ → ‘렌즈를 이 씬으로 맞추기’ in the script); rule 12 §5

- Hint in the script: `명령 또는 검색 · 인물 · 블록 · 씬 이동`.
- Script commands: the block shortcuts `/대 /나 /연 /선 /씬 /시 /메`; `렌즈를 이 씬으로 맞추기` (replaces `시점 바꾸기` while in the script); `점검 센터 열기`; `번호 다시 매기기 · {episode}`; [PROPOSED] `블록 고르기` (SCR-36) and `에셋` (SCR-61).
- Navigation: [PROPOSED] typing `#043` jumps to that block of the current episode; `씬 3` or a scene title jumps to that scene head. Other results (people, terms, lines) come from the search area.

### SCR-69 · Tabletop posture layout
**Priority** P2 · **needsExternal** no · **Boards** `FoldSpec` (rules only) · **Sources** planning 반쯤 접기 (rule only); scope table (`규칙만 정함, 시안은 나중에`)

- Tabletop: manuscript on the upper half; command field, panel tabs and keyboard on the lower half; nothing tappable near the fold. Book posture: the two panes as in SCR-03, with the fold as the pane border.

---

## 3. Data

Types: `Ulid` = 26-char ULID string created on the device; `OrderKey` = insertable string order value (fractional index); `Instant` = UTC timestamp. All text is UTF-8 and stored NFC-normalized (저장 파일 양식 §2). Every persisted entity also carries `modifiedAt: Instant`, `modifiedBy: String` (device id) and an optional `ext: JsonObject` (저장 파일 양식 §4, §7); they are not repeated below.

### 3.1 Ownership

| Entity / field | Owner | This area |
| --- | --- | --- |
| `Block`, `TextRun`, `DirectionCue`, `ChoiceOption`, `VoiceState`, `DiscardedVoiceId`, episode block counter | SCR | owns |
| `Asset` (script assets) | SCR | owns |
| `Scene.memo`, `Scene.transition*` | SCR | owns |
| `Scene` key, episode, order, title, status | 스토리 구조 | reads, creates/splits/merges/deletes through its API |
| `Scene` place/region, time (base, interval, band, clock, precision, 회상), silent cast | 장소와 때 | reads; writes only the split/merge results defined in SCR-50/51 |
| `Scene.hidden`, item hidden flags | 숨기기 · 등급 | reads |
| `ChangeRecord` content (subject, before/after values) | 인물 · 세계 | reads |
| `ChangeRecord.anchor` (seat, block key, original block key) | SCR | owns the anchor rules; the record is stored in `changes.json` |
| Term `◇ 첫 공개` record | 세계 | anchor follows SCR rules |
| Check `맞음` / `이대로 두기` records | 점검 센터 | emits items (SCR-62) |
| Candidate `아님` records | 세계 | keyed by block key |
| Character (name, voice code, display-name history, relations, 호칭) | 인물 | reads |
| Trash list, retention, permanent delete | 휴지통 | supplies block placement data |
| Script view state | SCR | owns (app DB only) |

### 3.2 Entities owned by this area

**`Block`**

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `key` | Ulid | no | Hidden permanent key; never changes, never reused |
| `sceneKey` | Ulid | no | Scene that contains the block (the episode follows from the scene) |
| `order` | OrderKey | no | Position inside the scene |
| `number` | Int ≥ 1 | no | Per-episode number shown as `#043` |
| `kind` | enum `DIALOGUE · NARRATION · DIRECTION · CHOICE · SYSTEM · MEMO` | no | Block kind |
| `speakerKey` | Ulid | yes | Character key; `DIALOGUE` only; null = `화자 없음` |
| `runs` | List<TextRun> | no | Text of `DIALOGUE · NARRATION · SYSTEM · MEMO`; empty for other kinds |
| `stage` | enum `LEFT · CENTER · RIGHT` | yes | `무대 위치`; `DIALOGUE` only; only used when the setting is on |
| `cues` | List<DirectionCue> | no | `DIRECTION` only; empty otherwise |
| `options` | List<ChoiceOption> | no | `CHOICE` only; ≥ 2 options; empty otherwise |
| `voice` | VoiceState | yes | `DIALOGUE` only |
| `recorderSource` | RecorderSource | yes | `MEMO` inserted by the recorder |
| `createdAt` | Instant | no | Creation time |
| `trashedAt` | Instant | yes | Set while the block is in the trash |
| `trashPlacement` | TrashPlacement | yes | Set while in the trash |

**`TextRun`** (sealed; new kinds may be added, older readers show their text)

| Variant | Fields | Meaning |
| --- | --- | --- |
| `Plain` | `text: String` | Ordinary text |
| `Paren` | `text: String` | `괄호 지문`; [PROPOSED] stored without the parentheses, rendered as `(text)` |
| `Link` | `targetKind: CHARACTER · TERM · PLACE`, `targetKey: Ulid`, `text: String` | `@` link; `text` is exactly what the manuscript shows (name, alias or candidate range) |
| `Break` | — | [PROPOSED] soft line break from `줄바꿈` (Q-03) |

**`DirectionCue`**

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `key` | Ulid | no | [PROPOSED] stable id of the cue |
| `kind` | enum `BACKGROUND · BGM · SFX · CG · SCREEN_EFFECT` | no | `배경 · BGM · 효과음 · CG · 화면 효과` |
| `assetKey` | Ulid | yes | Required for every kind except `SCREEN_EFFECT` |
| `effect` | String | yes | `SCREEN_EFFECT` only; e.g. `fade`, `blackout`, `shake` (Q-11) |

**`ChoiceOption`**

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `key` | Ulid | no | [PROPOSED] permanent option key (localization, voice) |
| `order` | OrderKey | no | Position in the block; the display index `-1`, `-2` is derived |
| `runs` | List<TextRun> | no | Option text |
| `speakerKey` | Ulid | yes | Speaker when the option is spoken (Q-05) |
| `voice` | VoiceState | yes | Voice ID with option suffix (`_1`) when spoken |

**`VoiceState`** (embedded)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `mode` | enum `AUTO · FIXED` | no | `자동` until first export, then `고정` |
| `fixedId` | String | yes | The exported ID; required when `FIXED` |
| `fixedAt` | Instant | yes | First export time (shown `11월 20일 내보냄`) |
| `manualId` | String | yes | ID typed by the author when the format is `직접` |

**`DiscardedVoiceId`** (`버린 ID`)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `id` | String | no | The discarded ID string; never reissued |
| `blockKey` | Ulid | no | Block it belonged to |
| `optionKey` | Ulid | yes | Option it belonged to |
| `discardedAt` | Instant | no | When it became discarded |
| `reason` | enum `DELETED · KIND_CHANGED · MERGED · REPLACED · FORMAT_REFIXED` | no | [PROPOSED] for the CSV and support |

**Episode block counter** (field on the story area's `Episode`, owned by SCR)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `nextBlockNumber` | Int ≥ 1 | no | Next number to issue in this episode; only grows (Q-07 for renumbering) |

**`TrashPlacement`** (embedded in a trashed block)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `prevKey` | Ulid | yes | Block that was directly before |
| `nextKey` | Ulid | yes | Block that was directly after |
| `ordinal` | Int | no | 0-based position in the scene at deletion time |

**`RecorderSource`** (embedded in a memo block)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `recordKey` | Ulid | no | Life Recorder record |
| `recordedAt` | Instant | no | Shown as `레코더 11월 22일 08:41` |

**Scene fields owned by SCR**

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `memo` | String | no (may be empty) | `씬 메모`; multi-line |
| `transitionMode` | enum `AUTO · DIRECT · NONE` | no | Default from project setting (initially `NONE`) |
| `transitionText` | String | yes | Last direct text; kept while the mode is `AUTO`/`NONE` |
| `transitionSnapshot` | `{ placeKey: Ulid?, regionKey: Ulid?, resolvedTime: ResolvedTime? }` | yes | Place/time when the direct text was last saved (for SCR-62) [PROPOSED shape] |

Derived scene values (never stored): ordinal `씬 N`, head text, auto transition text, transition ID (from the scene key, Q-25).

**`Asset`**

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `key` | Ulid | no | |
| `kind` | enum `BACKGROUND · BGM · SFX · CG` | no | |
| `name` | String | no | Display name, e.g. `하층 거리(새벽)` |
| `fileName` | String | no | Engine file name, e.g. `bg_lower_dawn` |
| `imageHash` | String | yes | Content hash of an attached image (`BACKGROUND · CG`); null for a CG shows `비어 있음` |
| `durationMs` | Long | yes | `BGM · SFX` length (`2분 41초`, `0.8초`) |
| `loop` | Boolean | no | `반복` (BGM) |
| `placeKey` | Ulid | yes | `BACKGROUND` ↔ place link (used only by bulk suggestions) |

Derived: use count, first-use episode, `안 쓰는 것`.

**Project script settings** (stored with project settings; screens in the settings area)

| Field | Type | Default | Meaning |
| --- | --- | --- | --- |
| `stagePositionEnabled` | Boolean | false | `무대 위치` |
| `voiceIdEnabled` | Boolean | true | `보이스 ID` |
| `voiceIdFormat` | `{ mode: PATTERN · MANUAL, pattern: String }` | `PATTERN`, `{code}_{chapter2}{episode2}_{number3}` [PROPOSED token syntax] | `ID 형식` |
| `transitionDefault` | enum `AUTO · DIRECT · NONE` | `NONE` | `새 씬 기본` |
| `transitionTemplate` | enum `PLACE_TIME · PLACE_ONLY` | `PLACE_TIME` | `틀 장소 · 시각` / `장소만` |

**`ChangeRecord.anchor`** (shared shape for `◆` records and the term's `◇` first reveal)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `seat` | enum `BLOCK · POINT · WORLD_EVENT` | no | Rule 12 §1 three seats |
| `blockKey` | Ulid | yes | Current anchor block (`BLOCK`) |
| `originalBlockKey` | Ulid | yes | Non-null while flagged `근거 지워짐`: the deleted anchor to re-attach to |
| `pointUnit` | `{ kind: EPISODE · SCENE, key: Ulid }` | yes | `POINT` seat (`◆ 1-5부터`) |
| `eventKey` | Ulid | yes | `WORLD_EVENT` seat |
| `trashedAt` | Instant | yes | Set by `기록 지우기` |

Derived: `planned` (after the writing line or no surviving block), display location `1-7 씬 3 #043`.

**Script view state** (app DB only; never in `.civilight`)

| Field | Type | Meaning |
| --- | --- | --- |
| `layers` | `{direction, memo, system, marks, addressHint, paren, termCandidates}`: Boolean | `표시` sheet (Q-14) |
| `preview` | enum `WRITE · DIALOGUE_ONLY · READ_ALOUD` | `쓰기 · 대사만 · 낭독` |
| `panel` | `{ open: Boolean, tab: TIME · REF · PROPS · MODULES, scroll: Map<Tab, Int> }` | Panel state |
| `moduleCardOrder` | List<String> | `모듈` card order |
| `lastWritingPosition` | `{ blockKey: Ulid, offset: Int }` | Rule 23 `마지막으로 쓰던 곳` |
| `restore` | `{ episodeKey, selectedBlockKey?, caretOffset?, scrollAnchor }` | Fold / process-death restore |
| `recentSpeakers` | List<Ulid> | Picker `최근` |
| `dailyTally` | Map<LocalDate, Int> | `오늘` card (Q-16) |

### 3.3 Relations to other areas

- `Block.speakerKey`, `TextRun.Link(CHARACTER)`, `ChoiceOption.speakerKey` → 인물 `Character` (name, voice code, 표시 이름 history, hidden, trashed/nameplate).
- `TextRun.Link(TERM)` → 세계 `Term`; `TextRun.Link(PLACE)` and `Asset.placeKey` → 세계 `Place`.
- `Block.sceneKey` → 스토리 구조 `Scene` → `Episode` (block counter).
- `ChangeRecord.anchor.blockKey` / `originalBlockKey` → `Block.key`; term first reveal likewise.
- `CandidateDismissal (아님)` (세계) → `Block.key` + term key + matched text + occurrence index.
- `CheckAcceptance (맞음)` (점검 센터) → block key or scene key + check kind.
- `RecorderRecord` (라이프 레코더) ↔ `Block.recorderSource`; the record lists the block as a `넣은 곳`.

### 3.4 What goes into `.civilight`

| File (저장 파일 양식 §2) | Script content |
| --- | --- |
| `project.json` | Project script settings (3.2) |
| `story/index.json` | Scenes with `memo`, `transitionMode`, `transitionText`, `transitionSnapshot` (alongside the story and time areas' scene fields) |
| `story/<episode key>.json` | All live blocks of the episode with every field of 3.2 (runs, cues, options, voice states), the episode's `nextBlockNumber`, and the episode's `DiscardedVoiceId` list [PROPOSED placement] |
| `changes.json` | `◆` records including `anchor` (seat, block key, original block key) |
| `world.json` | Term first reveals including their anchor (world area) |
| `assets/index.json`, `assets/<hash>.<ext>` | Script assets' metadata and attached images [PROPOSED: script assets live in `assets/index.json`; whether audio files are packaged is Q-29] |

- Always stored: voice ID states and discarded IDs (so another device keeps IDs fixed), check `맞음` records, `아님` records, links.
- Not stored: anything derived — scene ordinals, head text, resolved time, automatic voice IDs, automatic transition text, candidate lists, check results, counts — and view state.
- The trash is not stored unless the export option `휴지통도 포함` is chosen; then trashed blocks keep `trashedAt` and `trashPlacement`.
- Manifest required features include `story.blocks`, `changes`, `time.base-interval` (sample manifest).

---

## 4. Rules as test cases

All cases use the fixtures below. Fixture values not taken from a source are marked *(fixture)*; the user asked to ignore sample-data inconsistencies between boards, so the fixtures are made self-consistent instead of copying every illustrative count. Expected strings are exact. Cases marked **blocked** depend on an open question.

### 4.1 Fixtures

**F1 · Project 「잿빛 새벽의 등불」** (R-15; structure `장 › 에피소드 › 씬`; voice IDs on; 무대 위치 off; transition default `없음`, template `장소 · 시각`; voice format `{code}_{chapter2}{episode2}_{number3}`).

- Characters and voice codes: 리엔 `RIE`*(fixture)*, 하르트 `HRT`, 세라 `SRA`*(fixture)*, 오웬 `OWN`*(fixture)*, 마르타 `MRT`*(fixture)*, 노아 `NOA`*(fixture)*. 하르트's 표시 이름 is `???` through 1-8 and `하르트` from 1-9.
- Term `오리지늄 등불`, aliases `오리지늄`, `빛돌`; its `◇ 첫 공개` is anchored on 1-7 `#044`.
- Episode **1-7** (chapter 1, episode 7), `nextBlockNumber = 58`. Episode **1-8**, `nextBlockNumber = 61`; its 씬 1 starts with block `#001`.
- Scenes of 1-7 (times as resolved by the place & time area):

| Scene | Title | Place | Time input | Resolved |
| --- | --- | --- | --- | --- |
| S1 씬 1 | 새벽 순찰 | 하층 거리 *(fixture)* | 1-6 씬 3 기준 +2일 · 새벽 | 1일째 새벽 · story day 12 (10~14) |
| S2 씬 2 | 결사의 문 | 상층 관문 | same day · 저녁 | 1일째 저녁 |
| S3 씬 3 | 꺼진 등불 | 결사의 문 | S2 기준 +1일 · 05:40 · 정확 | 2일째 새벽 05:40 · story day 13 (11~15) |
| S4 씬 4 | (none) | 결사의 문 | S3 기준 같은 날 · 06:10 · 정확 | 2일째 아침 06:10 |
| S5 씬 5 *(fixture)* | (none) | 결사의 문 | S4 기준 같은 날 · 08:00 · 정확 | 2일째 아침 08:00 |

- S3 silent cast: 마르타 *(fixture; the rule example uses 세라, who speaks in S3 in the boards)*.
- S4 transition: `직접`, text `결사의 문 앞 · 06:10`, snapshot (결사의 문, 2일째 06:10). Other scenes: `없음`.
- Blocks of S3 (13 blocks, all voice IDs `자동`):

| # | Kind | Speaker | Text | Anchored |
| --- | --- | --- | --- | --- |
| #036 | 대사 | 세라 | 불은 꺼졌어. 기록도 거기서 끝나. | — |
| #037 | 대사 | 오웬 | 기록 따위로 배가 부르진 않지. | — |
| #038–#040 | 나레이션 | — | *(fixture text)* | — |
| #041 | 연출 | — | cues: 배경 `하층 거리(새벽)`, BGM `잔불` | — |
| #042 | 대사 | 리엔 | `(작게)` 저기… 불빛이 보여요. 누가 아직 남아 있는 걸까요? | R4 `◆` *(fixture)* |
| #043 | 대사 | 하르트 | 멈추지 마. 저건 등불이 아니야. | R1 `◆` 관계 · 리엔의 마음 → 하르트 · 만나기 전 → 경계 |
| #044 | 나레이션 | — | 바람이 잿더미를 쓸고 지나갔다. 꺼진 줄 알았던 [오리지늄 등불]이 한 번 깜박였다. | `◇` 첫 공개 오리지늄 등불 |
| #045 | 대사 | 리엔 | `(숨을 삼키며)` 하르트 씨, 방금… | R5 `◆` *(fixture)* |
| #046 | 작가 메모 | — | 여기서 등불이 두 번째로 깜박이게 할지 | — |
| #047 | 대사 | 하르트 | 뒤돌아보지 말고 따라와. | — |
| #048 | 대사 | 리엔 | 하지만 저 불빛은 결사의 신호였어요. | — |

- Blocks of S4: `#049` 연출 (배경 `결사의 문(아침)`), `#050` 마르타 `문은 해가 뜨면 닫혀. 서둘러.`, `#051` 리엔 `(숨을 고르며) 하르트 씨는요?`, `#052` 세라 `먼저 들어갔어. 너도 들어와.` Block of S5: `#053` 나레이션 *(fixture)*. Numbers 054–057 were issued earlier and permanently deleted *(fixture)*.

**F2 · Renumber fixture.** Episode 1-7 of another project with 48 live blocks in manuscript order at positions p = 1…48; the block at position p has number p, except: p20 `#056` (씬 1 · 나레이션), p31 `#057` (씬 2 · 나레이션), p38 `#055` (씬 3), p45 `#051` (씬 4 · 하르트, `고정` `HRT_0107_051`), p46 `#052` (씬 4, `고정`), p47 `#053` (씬 4, `고정`), p48 `#058` (씬 4 · 하르트, `고정` `HRT_0107_058`). Numbers 20, 31, 38, 45–48 are not in use. `nextBlockNumber = 59`.

**F3 · Choice fixture.** Episode 1-7, a choice block `#045` with options 1 `따라간다` (spoken by 하르트 *(fixture)*) and 2 `불빛 쪽을 한 번 더 본다`, followed by a block `#046`.

**F4 · Scene-head variants** (one scene each, ordinal as shown): (a) 1-7 S4 of F1; (b) same as (a) with band `아침` and no clock; (c) 1-6 씬 1, place 잿더미 시장, approximate `며칠 뒤`, no band, no clock, first timed scene of 1-6; (d) 1-2 씬 1, region 하층 구역 *(fixture)*, `1일째 새벽` band only; (e) 씬 2 with no place and no time; (f) 1-4 씬 2, 회상, region 하층 구역, year 1195.

### 4.2 Rule 12 · blocks and change records

**T-12-01 · Deleting an anchor block relocates the record** (§2 예시 1)
- Given F1.
- When `#043` is deleted.
- Then `#043` is in the trash with `prevKey = #042`, `nextKey = #044`, `ordinal = 7`; R1.anchor = `{seat: BLOCK, blockKey: #044, originalBlockKey: #043}`; `#044` shows a dotted `◆`; the toast reads `블록 #043 지움 · ◆ 1개는 #044 자리로` with `되돌리기`; the check center has `◇ 근거 지워진 변화 1`; no `DiscardedVoiceId` is created (voice was `자동`).

**T-12-02 · Re-anchoring to a rewritten line takes two taps** (§2 예시 1)
- Given T-12-01's result.
- When a dialogue block is inserted after `#042` with speaker 하르트, and `다른 블록에 잇기` is opened on R1.
- Then the new block is `#058` and `nextBlockNumber = 59`; the manuscript order reads `#042 · #058 · #044`; the first candidate is `#058`. Choosing it sets R1.anchor = `{blockKey: #058, originalBlockKey: null}` and the mark on `#058` is solid.

**T-12-03 · Undo re-attaches**
- Given T-12-01's result. When the toast's `되돌리기` is tapped. Then `#043` is back between `#042` and `#044`, R1.anchor.blockKey = `#043`, `originalBlockKey = null`, and the check item is gone.

**T-12-04 · Restoring keeps a choice the user made**
- Given T-12-02's result. When `#043` is restored from the trash. Then R1 stays on `#058` (position of `#043`: Q-18).

**T-12-05 · `기록 지우기`** (§2 예시 2)
- Given T-12-01's result. When `기록 지우기` is tapped on R1. Then R1 has `trashedAt` set and is listed under trash filter `변화`; `#044` has no `◆`; the check item is gone.

**T-12-06 · Relocation order** (§2)
- (a) Given F1 with R6 *(fixture)* on `#048`. When `#048` is deleted. Then R6 → `#049` (first block of S4).
- (b) Given F1 with R7 *(fixture)* on `#053` (last block of 1-7). When `#053` is deleted. Then R7 → 1-8 `#001`.
- (c) Given a project whose last episode ends with block `#010` holding R8. When `#010` is deleted. Then R8 has no block, is `planned` (예정), and keeps `originalBlockKey = #010`.

**T-12-07 · Deleting a scene** (§2 씬을 지울 때)
- Given F1 (S3 holds R1, R4, R5 and the `◇` on `#044`).
- When S3 is deleted from its head ⋯.
- Then no confirmation is shown; S3 is in the trash (filter `씬`); R1, R4, R5 and the first reveal are anchored on `#049` with `originalBlockKey` set; the toast reads `씬 3 지움 · ◆ 3 · ◇ 1은 씬 4 처음으로`; S4 becomes `씬 3` and its base becomes S2 with its resolved time unchanged (`2일째 아침 06:10`).

**T-12-08 · Backspace merge moves records without flagging** (§4)
- Given F1. When the caret is at the start of `#043` and Backspace is pressed.
- Then `#042` holds both texts ([PROPOSED] joined with one space), keeps key, number and speaker 리엔; `#043` no longer exists and is not in the trash; R1.anchor.blockKey = `#042`, `originalBlockKey = null`; toast `◆ 1개 #042로`.
- Variant: `#043` voice `고정 HRT_0107_043` → `DiscardedVoiceId("HRT_0107_043", reason MERGED)`.

**T-12-09 · Split keeps records on the front block** (§4)
- Given F1 with R6 *(fixture)* on `#047`. When Enter is pressed after `뒤돌아보지 말고`.
- Then `#047` reads `뒤돌아보지 말고` and keeps R6; a new block `#058`, speaker 하르트, reads `따라와.` directly after it with voice `HRT_0107_058 · 자동`.

**T-12-10 · Copy does not copy records** (§4)
- Given F1. When `#043` is copied and pasted below `#048`. Then a new block `#058` (하르트, same text, `HRT_0107_058 · 자동`) has no record; R1 stays on `#043`; toast `◆는 복사되지 않음`.

**T-12-11 · Move to another scene** (§3)
- Given F1. When `#043` is moved after `#050`. Then its number stays `#043`, voice `HRT_0107_043`, R1 moves with it, toast `◆ 1개 씬 3 → 씬 4`.

**T-12-12 · Move to another episode, automatic voice** (§3, §6)
- Given F1. When `#043` is moved to the start of 1-8 씬 1. Then its number becomes `#061`, 1-8's `nextBlockNumber = 62`, voice `HRT_0108_061 · 자동`, toast `◆ 1개 1-7 → 1-8`.

**T-12-13 · Move to another episode, fixed voice** (§6)
- Given F1 with `#043` voice `고정 HRT_0107_043`. When moved as in T-12-12. Then its number is `#061`, voice stays `HRT_0107_043`, and the check center has `형식과 다른 ID 1` under `보이스`.

**T-12-14 · Moving the first reveal behind its uses** (§3)
- Given F1 with links to 오리지늄 등불 in `#047` and `#048` *(fixture)*. When `#044` is moved to 1-8 씬 1. Then the toast contains `◇ 첫 공개 1-7 → 1-8 · ▲ 새로 2`.

**T-12-15 · Scene-move preview** (§3)
- Given F1 with `#042, #043, #045, #047` voice `고정` (exported 2026-11-20).
- When story structure asks to move S3 to the front of 1-8.
- Then the sheet shows `1-7 · 씬 3` · `꺼진 등불` › `1-8` · `맨 앞 · 씬 1`; `기록 · ◆ 3 · ◇ 1 · 시점 1-7 → 1-8` (R1, R4, R5 and the first reveal on `#044`); `블록 번호 · 블록 13개 · 1-8 새 번호`; `보이스 ID · 고정 4개 그대로 · ◇ 에피소드 부분 다름`; `때 · 그대로 · ◇ 1 순서와 어긋남`; buttons `취소` · `옮기기`. Nothing changes before `옮기기`. After `옮기기`, S3's blocks are `#061`…`#073` in manuscript order.
- Note: the board's `◆ 2` and `블록 18개` are sample data. The `◇ 1 순서와 어긋남` comes from the place & time area (S4 at 06:10 now precedes S3 at 05:40 in manuscript order).

**T-12-16 · Choice blocks** (§4 선택지)
- Given F3. Then the options display `#045-1`, `#045-2`; option 1's voice is `HRT_0107_045_1`; the JSON `rejoin` of `#045` is `#046`'s key. When a `◆` is created from the choice block, its anchor is `{seat: BLOCK, blockKey: #045}`; no option key can be chosen.

**T-12-17 · Renumber** (§5; board `RTRenumber`)
- Given F2. When `번호 다시 매기기 · 1-7` is opened.
- Then the sheet shows `48` `블록` · `7` `바뀌는 번호` · `41` `그대로`; rows `#056 → #020 · 씬 1 · 나레이션`, `#057 → #031 · 씬 2 · 나레이션`, `#051 → #045 · 씬 4 · 하르트` with `보이스 HRT_0107_051 · 고정 · 그대로`, `#058 → #048 · 씬 4 · 하르트` with `보이스 HRT_0107_058 · 고정 · 그대로`, and `외 3개 · #055 → #038 · #052 → #046 고정 · #053 → #047 고정`; footer `고정 보이스 ID 4개는 그대로` and `번호만 다름 · ◇ 없음 · 변화 기록 · 블록 ID 그대로`.
- When `다시 매기기` is tapped. Then every block at position p has number p; keys and records are unchanged; the four fixed IDs are unchanged and no `◇` is raised; automatic IDs follow the new numbers. (Counter value afterwards: Q-07.)

**T-12-18 · Voice ID events** (§6 table)
- Given F1. When `#043`'s speaker changes to 리엔. Then voice = `RIE_0107_043 · 자동`.
- Given `#043` = `고정 HRT_0107_043`. When its speaker changes to 리엔. Then voice stays `HRT_0107_043` and the check `형식과 다른 ID 1` appears; when `새 ID로` is tapped, voice = `RIE_0107_043 · 자동` and `DiscardedVoiceId("HRT_0107_043", REPLACED)` exists.
- Given `#043` = `고정`. When `#043` is deleted. Then `DiscardedVoiceId("HRT_0107_043", DELETED)` exists.
- Given `#043` = `고정`. When `#043` is changed to 나레이션. Then `DiscardedVoiceId("HRT_0107_043", KIND_CHANGED)` exists and R1 stays on `#043`.
- Given 214 fixed IDs in the project *(rule example)*. When the ID format changes. Then the setting shows `고정 ID 214개는 그대로` and the button `고정 ID도 새 형식으로`.
- Given `#043` = `자동`. When a JSON export containing `#043` is written on 2026-11-20. Then `#043` = `고정 HRT_0107_043`, `fixedAt` 2026-11-20, and the 속성 tab reads `HRT_0107_043 · 고정 · 11월 20일 내보냄`. Before the export it read `HRT_0107_043 · 자동`.

**T-12-19 · Deletion toasts and trash grouping** (§4, §7)
- Given F1. When `#038`–`#042` (5 blocks; R4 on `#042` and R9 *(fixture)* on `#040`) are deleted together. Then the toast reads `블록 5개 지움 · ◆ 2개 다음 자리로`, and both records sit on `#043`.
- Given 4 non-empty blocks of 1-7 S3 deleted today. Then the trash shows one row `1-7 씬 3 · 블록 4개` · `블록 · 잿빛 새벽의 등불 · 오늘 지움` · `30일 남음`.
- Given an empty dialogue block. When it is deleted. Then no toast, no trash entry, and the counter is unchanged.

**T-12-20 · An emptied block that anchors a record is not "empty"** (§2, §4)
- Given F1. When all text of `#043` is erased and Backspace is pressed inside it. Then `#043` goes to the trash, R1 moves to `#044` flagged `근거 지워짐`, and the toast reads `블록 #043 지움 · ◆ 1개는 #044 자리로`.

**T-12-21 · Moving across the writing line needs no re-linking** (§3 집필 선을 넘을 때)
- Given F1 plus an episode 1-9 with one scene and no blocks *(fixture; 1-9 counter 1)*. When `#043` is moved to 1-9 씬 1. Then `#043` becomes `#001` of 1-9, R1 still anchors that block (same key), and nothing asks to re-link. Because the block gives 1-9 씬 1 content, the writing line moves to the end of 1-9 씬 1 and R1 is applied, not 예정 (see Q-36).

**T-12-22 · Cut and paste is a move** (§3, §4)
- Given F1. When `#043` is cut in select mode and pasted after `#050`. Then the pasted block has `#043`'s key and number, R1 still anchors it, the toast reads `◆ 1개 씬 3 → 씬 4`, and no `◆는 복사되지 않음` toast appears.

**T-12-23 · Duplicate manual voice IDs** (§6 겹치지 않는 이유)
- Given F1 with the voice format set to `직접`, `#043` = `HRT_A_01` and `#047` = `HRT_A_01` *(fixture)*. Then the check center has one `▲` duplicate item covering both blocks ([PROPOSED] label `▲ 보이스 ID 겹침 1`). With automatic IDs no duplicate can arise.

**T-12-24 · Restore placement and preserved state** (§7)
- (a) Given F1 with `#043` = `고정 HRT_0107_043`. When `#043`, then `#042` and `#044` are deleted, and `#043` alone is restored. Then, both neighbours being gone, `#043` returns at its stored ordinal 7 of S3 (between `#045` and `#046` in the remaining list `#036 … #041, #045, #046, #047, #048`); its number is `#043`; its voice is `고정 HRT_0107_043`; the `버린 ID` entry created by its deletion is gone.
- (b) Given `#043` deleted and then S3 deleted. When `#043` is restored from its block group. Then the sheet offers `씬도 되살리기` · `다른 씬 끝에`; choosing `다른 씬 끝에` and S4 appends `#043` at the end of S4 with number `#043`.

**T-12-25 · Point seat shown in the panel, not the manuscript** (§1 세 자리)
- Given F1 plus a record P1 *(fixture)* `리엔 · 소속 → 등불 결사` anchored at the point `1-7` (unit start, made by `시점 추가` in the profile), and a world-event record W1 *(fixture)* from the timeline's `이 사건에서 바뀐 것`.
- When S1 of 1-7 is open and the `시간` tab is shown. Then the `이 씬 안` list has one line `◆ 1-7부터 · 근거 블록 없음` with `블록에 잇기`, placed above the row of S1's first block *(fixture blocks)* [PROPOSED placement for an episode point, SCR-42]; no `◆` mark is drawn on any block in the manuscript for P1; W1 appears nowhere in the script.
- When S3 is open. Then no P1 line appears [PROPOSED: the line belongs only to the first scene of the unit, SCR-42].
- When the `변화 표식` layer is off (SCR-26). Then the P1 line is hidden too.
- When `블록에 잇기` is tapped and S1's second block *(fixture)* is chosen from the candidate list (same list as SCR-43). Then P1 becomes a block-seat record on that block: the panel line is gone, the block shows `◆`, and the record applies from that block, inclusive.

### 4.3 Rule 11 · position and block-level time in the script

**T-11-01 · Reference state per block** (§2, with rule 13 wording)
- Given F1. Then the resolver for `리엔의 마음 → 하르트` gives: before `#042` = `만나기 전`; at `#043` = before `만나기 전`, here `경계` (row `만나기 전 → 경계 ◆ 여기서`); before `#044` = `경계`.

**T-11-02 · Pre-reveal by block order inside one scene** (§2)
- Given F1 plus a link to 오리지늄 등불 in `#042`. Then `#042` shows `▲` (pre-reveal use). A link inside `#044` and one inside `#045` raise no `▲`.

**T-11-03 · Position line and label** (§3, rule 22 §6)
- Given F1 with the caret in `#045`. Then the header line reads `1-7 · 씬 3 · 2일째 새벽` and the label `이 씬 · 1-7`.
- Given no caret and no selection, and `#050` directly under the top bar. Then the header line reads `1-7 · 씬 4 · 2일째 아침`.
- When `렌즈를 이 씬으로 맞추기` is chosen with the header on S3. Then the app lens = point (1-7, end of S3).

### 4.4 Rule 22 · scene head, transition text, split, merge, export

**T-22-01 · Head and direct transition** (§2–§3)
- Given F1. Then S4's head reads `씬 4 · 결사의 문 · 2일째 아침 06:10` and its second line `전환 문구 · 결사의 문 앞 · 06:10 · 직접`. S3's second line reads `전환 문구 · 없음`.

**T-22-02 · Direct text survives a time change and raises ◇** (§2 예)
- Given F1. When S4's time is changed to 07:00. Then the head reads `씬 4 · 결사의 문 · 2일째 아침 07:00`; the transition text is still `결사의 문 앞 · 06:10`; the check center has `◇ 전환 문구 · 쓴 뒤 때 바뀜 · 1-7 씬 4 · 06:10 → 07:00` with `자동으로` · `문구 고치기` · `맞음`.
- When `자동으로` is tapped. Then S4's mode is `자동`, the line reads `전환 문구 · 결사의 문 · 07:00 · 자동`, `transitionText` still holds `결사의 문 앞 · 06:10`, and the check item is gone.

**T-22-03 · Automatic text per properties** (§2 table; template `장소 · 시각`)
- F4(a) → `결사의 문 · 06:10`; F4(b) → `결사의 문 · 아침`; F4(c) → `잿더미 시장`; F4(d) → `하층 구역 · 새벽`; F4(e) → none, and `자동` cannot be selected.
- Template `장소만`: F4(a) → `결사의 문`.

**T-22-04 · Head per properties** (§2 table; rule 11 §6)
- F4(a) → `씬 4 · 결사의 문 · 2일째 아침 06:10`; F4(b) → `씬 4 · 결사의 문 · 2일째 아침`; F4(d) → `씬 1 · 하층 구역 · 1일째 새벽`; F4(e) → `씬 2 · 장소 없음 · 때 없음`; F4(f) → `씬 2 · 회상 · 하층 구역 · 1195`.
- **blocked (Q-22)**: F4(c). Rule 22's table gives `씬 1 · 잿더미 시장 · 약 7일째`; rule 11 §6 gives `1일째` for an episode's first scene even when approximate.

**T-22-05 · Direct text is kept while not in use** (§3)
- Given F1. When S4 is set to `없음` and then back to `직접`. Then the text is `결사의 문 앞 · 06:10` again.

**T-22-06 · Pre-reveal place** (§2 table)
- Given F1 with S4 set to `자동` and the map item 결사의 문 visible from 1-8. Then the text is still `결사의 문 · 06:10` and the check center has `◇ 공개 전 장소 · 전환 문구` with `직접 쓰기` · `맞음`.

**T-22-07 · Backspace never crosses a head** (§3)
- Given F1 with the caret at the start of `#049`. When Backspace is pressed. Then nothing changes.

**T-22-08 · `/씬` routing** (§4)
- Given F1. Caret in `#044` → `씬 나누기` sheet with sub `1-7 씬 3 · #044 아래`. Caret in `#048` → `다음 씬 만들기`. `#042`–`#044` checked → split below `#044`. Read-only project → the row is dimmed with `읽기 전용`. Nothing changes until a sheet button is tapped.

**T-22-09 · Split** (§4 예, §5)
- Given F1 with the caret in `#044`. When `/씬` is chosen.
- Then the sheet shows `씬 3` · `블록 9개` · `‘세라 · 불은 꺼졌어…’ ~ ‘…한 번 깜박였다.’` and `새 씬 4` · `블록 4개` · `‘리엔 · 하르트 씨, 방금…’ ~ ‘…결사의 신호였어요.’` (excerpt rule below); `장소 · 결사의 문 · 씬 3과 같음`; `때 · 씬 3 기준 · 같은 때`; `제목 · 미정`; `말 없는 등장 · 마르타` with `둘 다` selected; `바뀌는 것` rows `순번 · 씬 4 · 미정 → 씬 5`, `순번 · 씬 5 · 미정 → 씬 6`, `첫 공개 · 오리지늄 등불 · 씬 3 #044 그대로`, `◆ 관계 · 리엔 → 하르트 · #043 그대로`, `씬 5 때 · 기준 씬 3 · 2일째 아침 06:10 그대로`.
- When `나누기` is tapped. Then S3 holds `#036`–`#044`; a new scene `씬 4` holds `#045`–`#048` with place 결사의 문, time `S3 기준 · 간격 0 · 정확` (resolved `2일째 새벽 05:40`), transition `없음`, no title, silent cast 마르타; the old S4 is `씬 5` with base S3 and resolved `2일째 아침 06:10`; S5 is `씬 6`; all block numbers, voice IDs and anchors are unchanged. One undo restores the previous state.
- Excerpt rule [PROPOSED, chosen to reproduce the rule's strings]: parentheticals removed; leading excerpt = the longest run of whole words from the start whose length (with spaces) is ≤ 10 characters, trailing `.,?!…⋯` removed, then `…`; trailing excerpt = `…` + the longest run of whole words at the end with length ≤ 10. The leading excerpt is prefixed with `{speaker} · ` for dialogue.

**T-22-10 · Split above records** (§4)
- Given F1 with the caret in `#042`. When `/씬` is chosen. Then `바뀌는 것` includes `◆ 관계 · 1-7 씬 3 #043 → 1-7 씬 4 #043` and `첫 공개 · 1-7 씬 3 #044 → 1-7 씬 4 #044`.

**T-22-11 · Split inheritance** (§5 table)
- A has no time → B has no time. A is `직접` (fixed point) → B is `A 기준 · 같은 때` and A is the only fixed point. A is a flashback → B shows the flashback row switched on. A is hidden → B shows the hidden row switched on. A is the first timed scene of its episode → A stays `1일째`.

**T-22-12 · `다음 씬 만들기` prefill** (§4)
- Given F1 with the caret in `#048`. When `/씬` is chosen. Then the sheet prefills `장소 · 결사의 문 · 씬 3과 같음` and `때 · 씬 3 기준 · 같은 때`, each with `비우기`. Tapping `비우기` on both and creating gives a scene with no place and no time; S4 and S5 keep their resolved times.

**T-22-13 · Merge, default choices** (§5 예)
- Given F1. When `앞 씬과 합치기` is chosen on S4.
- Then the sheet shows `1-7 씬 3 + 씬 4 → 씬 3`; `장소 · 결사의 문 · 씬 3 · 씬 4 같음`; `때 · 하나 고르기` with `씬 3 것 · 2일째 새벽 05:40` selected and `씬 4 것 · 2일째 아침 06:10`; `고르지 않은 때 → 씬 메모 한 줄`; `전환 문구 · 씬 4 직접` · `‘결사의 문 앞 · 06:10’` with `시스템 텍스트로 남기기` selected; `바뀌는 것`: `씬 5 때 · 기준 씬 4 → 씬 3 · 간격 다시 적음 · 절대 때 그대로`, `제목 · 꺼진 등불 · 씬 4 제목 없음`, `기록 · 블록에 그대로 · 위치 이름만 씬 4 → 씬 3`.
- When `합치기` is tapped. Then S3 holds `#036`–`#048`, a new system-text block `#058` reading `결사의 문 앞 · 06:10`, then `#049`–`#052`; S3's memo contains the line `합치기 전 · 씬 4 · 결사의 문 · 2일째 아침 06:10`; S5 (now `씬 4`) has base S3 and resolved time `2일째 아침 08:00`; S4 no longer exists. One undo restores S3 and S4 exactly as before.

**T-22-14 · Merge choosing the later scene's time** (§5)
- Given F1. When merging S4 into S3 with `씬 4 것` chosen. Then S3's resolved time is `2일째 아침 06:10`; S5 is still `2일째 아침 08:00`; the memo line is `합치기 전 · 씬 3 · 결사의 문 · 2일째 새벽 05:40` [PROPOSED by symmetry].

**T-22-15 · Merge with one side empty** (§5)
- Given F1 with S3's place cleared. When S4 is merged into S3. Then the merged place is 결사의 문 and the sheet asks nothing about place.

**T-22-16 · Merge blocked** (§5)
- S4 is a flashback and S3 is not → `회상이 섞임 · 먼저 회상 끄기`, no merge. S4 hidden and S3 not → `숨긴 씬이 섞임 · 먼저 숨김 풀기`. 1-8 씬 1 → `앞 씬과 합치기` is not offered (no previous scene in 1-8).

**T-22-17 · Merge with `지우기` for the transition** (§5)
- As T-22-13 with `지우기`. Then no system-text block is created and `nextBlockNumber` stays 58.

**T-22-18 · Scene delete keeps other times** (§5; rule 11 §4)
- Given F1. When S4 is deleted. Then S5's base is S3 and its resolved time is still `2일째 아침 08:00`; when S4 is restored from the trash, S4 is `2일째 아침 06:10` and S5's base is recomputed with the same absolute time.

**T-22-19 · Text that looks like a head** (§4)
- Given F1. When the text `— 씬 2 · 기록실 —` is pasted below `#048`. Then a narration block `#058` holds that text and the scene count of 1-7 is unchanged.

**T-22-20 · In-scene time skip** (§4)
- Given F1. When a system-text block `30분 뒤` is inserted after `#044`. Then S3's resolved time is still `2일째 새벽 05:40` and no travel check changes.

**T-22-21 · JSON scene value** (§6 table)
- Given F1. When 1-7 is exported as JSON. Then S4's scene object has `id` = S4 key, `index` 4, `title` null, `place` = 결사의 문 (`kind` place), `time` = `{storyDay: {min 11, max 15}, episodeDay 2, band 아침, clock "06:10", precision exact}`, `flashback` false, `transition` = `{mode direct, text "결사의 문 앞 · 06:10", id derived from S4's key}`, and `blocks` = `#049`–`#052` only (no head entry, no transition block).

**T-22-22 · CSV transition row** (§6)
- Given F1, first export. Then the CSV contains a row `전환 문구 | 결사의 문 앞 · 06:10 | (empty) | {S4 transition id} | 새`, and no transition rows for S1–S3 and S5.

**T-22-23 · The head ignores layers** (§3 표시 레이어)
- Given F1 with the `시스템 텍스트` layer off. Then S4's head `씬 4 · 결사의 문 · 2일째 아침 06:10` is drawn and its line `전환 문구 · 결사의 문 앞 · 06:10 · 직접` is not.

**T-22-24 · Merge where B is a fixed point** (§5)
- Given F1 with S4's time entered as `직접 · 2일째 아침 06:10` (fixed point). When `앞 씬과 합치기` is chosen on S4. Then the sheet shows [PROPOSED] `씬 4 고정점 사라짐 · 씬 4 것을 고르면 직접 유지`. With `씬 3 것`: the merged S3 keeps `S2 기준 +1일 · 05:40`, S5's base becomes S3 with resolved time `2일째 아침 08:00`, and no fixed point remains. With `씬 4 것`: S3's time becomes `직접 · 2일째 아침 06:10`.

**T-22-25 · Merge where B has a title** (§5 제목 · 씬 메모)
- Given F1 with S4 titled `문 앞` *(fixture)*, S3's memo `등불이 두 번째로 깜박이는 타이밍` and S4's memo empty. When merged with defaults. Then the title stays `꺼진 등불`; the memo's first line is `문 앞`, followed by `등불이 두 번째로 깜박이는 타이밍` and `합치기 전 · 씬 4 · 결사의 문 · 2일째 아침 06:10` [PROPOSED order after the first line].

**T-22-26 · Merge with time on one side only** (§5)
- Given F1 with S3's time cleared *(fixture)* and S4 = `S2 기준 +1일 · 06:10`. When S4 is merged into S3. Then the sheet asks nothing about time; S3's time becomes `2일째 아침 06:10`; S5's base becomes S3 with resolved time `2일째 아침 08:00`.

### 4.5 Rules 19, 20, 21, 23, 18 as seen in the script

**T-20-01 · Deleted speaker stays** (rule 20 §2)
- Given F1 plus 1-6 씬 2 and 씬 4 with 14 lines by 노아 and the trashed 1-6 씬 5 with more 노아 lines. When 노아 is deleted. Then the 14 lines show `노아` with a dotted underline and `◇` (not `▲ 화자 없음`); 노아's reference count for dialogue is 14; the 속성 tab of such a line shows `지운 인물 · 노아` with `화자 바꾸기` · `되살리기` · `이대로 두기`; automatic voice IDs keep the `NOA` code.

**T-20-02 · Picker row for a deleted speaker** (rule 20 §2 모서리)
- The speaker picker opened on a 노아 line starts with `지운 인물 · 노아 · 되살리기`; opened on a 리엔 line it has no such row.

**T-20-03 · Restore keeps user changes** (rule 20 §5)
- Given T-20-01 and 3 of the 14 lines changed to 오웬. When 노아 is restored. Then 11 lines are 노아 again (solid) and 3 stay 오웬.

**T-20-04 · Permanent delete** (rule 20 §5)
- Given T-20-01. When 노아 is permanently deleted. Then the labels read `영구 삭제 · 노아` with `◇`; `되살리기` is gone; exports still write the name 노아; fixed voice IDs are unchanged.

**T-19-01 · Hidden speaker and plain text** (rule 19 §6)
- Given 노아 hidden and locked. Then 노아's lines in 1-6 씬 5 show the label `숨긴 인물` with the line text unchanged, and a narration `노아가 웃었다` is unchanged.

**T-19-02 · Hidden scene row** (rule 19 §6)
- Given 1-7 S4 hidden, locked, with 12 blocks *(fixture count)*. Then the manuscript shows `숨긴 씬 · 블록 12 · 지문으로 보기` in place of S4.

**T-19-03 · Picker hides hidden characters** (rule 19 §6)
- Given 노아 hidden and locked. Then the picker does not list 노아 and ends with `숨긴 인물 1 · 지문으로 보기`.

**T-19-04 · Export of hidden content** (rule 19 §5)
- Given T-19-02. When 1장 is exported as JSON. Then the sheet shows `숨긴 씬 1 · 1-7 씬 4` with `넣기` preselected and requires one biometric confirmation; for PDF `빼기` is preselected. Given 노아 hidden, 노아's lines are exported with 노아's speaker id and the name `노아`.

**T-21-01 · Candidate popup** (rule 21 §2, §5)
- Given the block 1-2 씬 1 리엔 `등불 속 그 돌, 오리지… 뭐라고 했죠?` and the `용어 후보` layer on. Then `오리지` has a dotted underline; tapping it shows `오리지늄 등불` · `후보 · 끊김`, `첫 공개 1-7 · 잇기 시` · `▲ 공개 전 사용`, and `잇기` · `아님`.
- When `잇기`: the text stays `오리지…`, `오리지` becomes a link run to the term, and the line shows `▲` in the margin.
- When `아님` instead: a dismissal `(block key, term key, "오리지", occurrence 0)` is stored and the underline disappears.

**T-21-02 · Layer default and non-output blocks** (rule 21 §2, §4)
- With default layers no candidate underline is drawn. A memo `오리지늄 등불 확인` never gets an underline or `▲`, even with the layer on.

**T-21-03 · Alias through `@`** (rule 21 §2, §8)
- Typing `@오리` lists `오리지늄 · 오리지늄 등불의 별칭`; choosing it inserts a link run with text `오리지늄`; typing `은` afterwards adds a plain run (`[오리지늄]은`).

**T-23-01 · Recorder memo insertion** (rule 23 §4)
- Given F1 and a recorder record made 2026-11-22 08:41. When it is inserted at `마지막으로 쓰던 곳` = after `#044`. Then a memo block `#058` follows `#044`, its prefix reads `⌁ 작가 메모 · 레코더 11월 22일 08:41`, and the toast reads `1-7 씬 3 · #058 작가 메모 넣음 · 되돌리기 · 가 보기`. (The rule's own example number `#062` comes from a different counter.)

**T-23-02 · Last writing position**
- Given the last caret position was the end of `#044` in F1. Then the recorder's location row reads `마지막으로 쓰던 곳 · 1-7 씬 3 · #044 뒤`.

**T-18-01 · Bands in the head** (rule 18 §1–§2)
- 05:40 → `새벽`; 06:10 → `아침`; a scene at story time 01:30 after a `2일째` evening belongs to `2일째` → head `… · 2일째 밤 01:30`.

### 4.6 Coverage

| Rule § | Cases |
| --- | --- |
| 11 §2 (block-level point, X부터 includes the block) | T-11-01, T-11-02 |
| 11 §3 (script has no lens) | T-11-03 |
| 11 §4 (move/delete keep other times) | T-12-07, T-22-18 |
| 11 §6 (N일째) | T-22-04 (partly blocked) |
| 12 §1 (seats) | T-12-16 (block seat), T-12-25 (point seat, world-event seat) |
| 12 §2 (anchor deleted) | T-12-01 … T-12-07, T-12-19, T-12-20 |
| 12 §3 (move) | T-12-11 … T-12-15, T-12-21, T-12-22 |
| 12 §4 (edit ops, choice) | T-12-08 … T-12-10, T-12-16 |
| 12 §5 (numbers) | T-12-02, T-12-12, T-12-17 |
| 12 §6 (voice ID) | T-12-13, T-12-17, T-12-18, T-12-23 |
| 12 §7 (trash) | T-12-04, T-12-05, T-12-19, T-12-24 |
| 18 §1–§2 (bands, day start) | T-18-01 |
| 19 §5–§6 | T-19-01 … T-19-04 |
| 20 §2, §5 | T-20-01 … T-20-04 |
| 21 §2, §4, §5 | T-21-01 … T-21-03, T-11-02 |
| 22 §2 | T-22-01 … T-22-06 |
| 22 §3 | T-22-01, T-22-07, T-22-23 |
| 22 §4 | T-22-08 … T-22-12, T-22-19, T-22-20 |
| 22 §5 | T-22-09, T-22-11, T-22-13 … T-22-18, T-22-24 … T-22-26 |
| 22 §6 | T-11-03, T-22-21, T-22-22 |
| 23 §4 | T-23-01, T-23-02 |

---

## 5. Screens

Cover = 390×844 boards (Compact). Inner = 800×800 boards (Medium/Expanded). DS components in parentheses.

| # | Screen / sheet / state | Boards | Cover | Inner | Components | Features |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | Script, reading/writing | `Main`, `UndoToast`, `RSHead` | Full-width manuscript; header 118 px with position line, title, `이 씬 · 1-7`, layers icon; bottom bar with speaker pill, layers icon, `줄바꿈`, command field + mic | `Wide`: 560 px column at x 40, panel 290 px on the right, header and bottom bar cover the column only | GlassBars, CommandBar, ScriptBlock, LensChip (as position label), Toast | SCR-01, 02, 03, 05–11, 46 |
| 2 | Editing with keyboard | `EditKeyboard` | Header 76 px `1-7 · 씬 3 · 저장됨` + `완료`; toolbar 56 px over the keyboard | Not drawn. [PROPOSED] toolbar spans the manuscript column; panel stays | GlassBars, Buttons | SCR-14–17 |
| 3 | Block add sheet | `Blocks`, `MType` | Bottom sheet from y 296 | [PROPOSED] same sheet over the manuscript column | Sheet, ListRow | SCR-12 |
| 4 | Speaker picker | `Picker`, `MSheet` | Bottom sheet | [PROPOSED] same sheet over the column | Sheet, Tag, ListRow | SCR-19 |
| 5 | `표시` sheet | `Layers` | Bottom sheet from y 250 | [PROPOSED] same sheet | Sheet, Controls (toggle), Tag | SCR-26–29 |
| 6 | Swipe buttons | `Main` (swiped row), `MSwipe`, `Tour2`, `Walk1` | Row slides 192 px | Same | SwipeTabs | SCR-30 |
| 7 | Panel `시간` | `CSheetTime`, `Wide`, `RSOrphan` | Sheet from y 400 (RSOrphan from 470) | Side panel; adds `고치기 · 관계도에서 보기` on the record card | Sheet, Tabs, ListRow (`.row`), Buttons | SCR-31, 32, 43 |
| 8 | Panel `참고` | `CSheetRef`, `WideRef`, `MFoldScript` | Sheet | Side panel | Sheet, Tabs, ListRow | SCR-33 |
| 9 | Panel `속성` | `CSheetProps`, `WideProps` | Sheet (화자 · 표시 이름 · 보이스 ID) | Side panel with `고른 블록 · #043`, `무대 위치`, `변화 연결` | Sheet, Tabs, segmented control, Buttons | SCR-34, 25 |
| 10 | Panel `모듈` | `CSheetMods`, `WideMods` | Sheet | Side panel | Sheet, Tabs | SCR-35 |
| 11 | `◆` change-record sheet | `ChangeCreate` | Sheet from y 250 | [PROPOSED] same sheet | Sheet, Tag, segmented control, Buttons | SCR-41 |
| 12 | Anchor deleted state | `RSOrphan`, `UndoToast` | Dotted `◆`, sheet card with three buttons | Same card in the side panel | ScriptBlock, Buttons, Toast | SCR-43 |
| 13 | Block select mode | `RSSelect` | Header `블록 고르기` + `모두` + `완료`; checkbox and grip per row; bottom bar 124 px | Not drawn. [PROPOSED] same in the column; panel hidden | Controls (check), GlassBars, Buttons | SCR-36–40 |
| 14 | Scene head and ⋯ | `RSHead`, `RSHeadMenu` | Head line + ⋯; ⋯ opens a bottom sheet | [PROPOSED] same sheet | ScriptBlock, Sheet, ListRow | SCR-46, 47 |
| 15 | Split sheet | `RSSplit` | Sheet from top region (manuscript shows `#044` above) | [PROPOSED] same sheet | Sheet, Tag, Buttons | SCR-50 |
| 16 | Merge sheet | `RSMerge` | Sheet from y 132; head highlighted | [PROPOSED] same sheet | Sheet, Controls (radio), segmented control, Buttons | SCR-51 |
| 17 | Scene-move preview | `RSSceneMove` (over 스토리 구조) | Sheet | [PROPOSED] same sheet | Sheet, Buttons | SCR-54 |
| 18 | Renumber preview | `RTRenumber` | Sheet over the script | [PROPOSED] same sheet | Sheet, Buttons | SCR-22 |
| 19 | Term candidate popover | `RTCandidate` | Small popover under the underlined text; chip `표시 · 용어 후보 · 켬` under the header | Same | ScriptBlock, Buttons | SCR-56 |
| 20 | Read-only example | `OnbExampleScript`, `Tour1`, `Tour2`, `Tour4` | Banner `예시 프로젝트 · 읽기 전용` · `온보딩 후 삭제`; bottom `예시 홈으로 · 온보딩 끝내기` | Not drawn | GlassBars, Buttons | SCR-60 |
| 21 | Asset list | `Assets` | Full screen list with chips | Not drawn. [PROPOSED] list modules' 목록 + 상세 layout | ListRow, Tag, ImageTile | SCR-61 |
| 22 | Fold transition | `MFoldScript`, `FoldSpec` | Sheet ↔ panel morph | — | Sheet | SCR-66 |
| 23 | Motion references | `MType`, `MSwipe`, `MSheet`, `MotionSpec` | — | — | — | SCR-67 |
| 24 | Script rows in other areas' screens | `ProjSettings` (스크립트 추가 칸), `Export`, `Trash`, `Check`, `SceneWhere` (`전환 문구` chips), `RTFlashback`, `RTCascade` | — | — | ListRow, Tag | SCR-24, 25, 48, 39, 62–65 |

**Not drawn (behaviour specified, layout [PROPOSED]):** `다음 씬 만들기` sheet, `앞에 새 씬` sheet, transition-text editor, direction-cue editor, asset add/edit form, `옮기기` destination picker, `다른 블록에 잇기` / `블록에 잇기` candidate list, select-mode entry, `필드` and `용어 공개` tabs of the `◆` sheet, `대사만` / `낭독` previews, `호칭 힌트` line, hidden-scene row, deleted-speaker 속성 variant, point-seat line in the `시간` tab, empty episode / empty scene states, inner keyboard toolbar.

---

## 6. Open questions

Only gaps and contradictions that block an exact implementation. Each names the sources on each side. Defaults chosen in this spec are marked [PROPOSED] where they are used.

| # | Question | Sources |
| --- | --- | --- |
| Q-01 | The script header title `잿빛 새벽의 등불` is both the project name and the title of episode 1-7. Which one does the header show? | `gen_nm.py script_head`; Home board (`1-7 · 잿빛 새벽의 등불`); `Assets` header (project name as small line) |
| Q-02 | How does "a tap opens the same button row" coexist with tapping a block to edit it? And does the swiped row stay open after release (`MSwipe` says `놓으면 제자리`)? | Planning 스와이프 (`탭해도 같은 줄이 열린다`); DS SwipeTabs; `EditKeyboard`; `MSwipe` |
| Q-03 | What does `줄바꿈` do and where is it while typing? Enter already makes a new block. The planning puts `화자 · 줄바꿈 · 블록 추가` on the keyboard row; the drawn toolbar has `대사 ▾ · 화자 · ( ) · @ · ◆ · 되돌리기` and the command row has a `줄바꿈` pill | Planning 입력 중; `EditKeyboard` (`엔터 = 새 블록(같은 화자)`); `Main` command row |
| Q-04 | Is there a per-block display-name override (a line shown as `경비병`), or only the character's 표시 이름 history? | Planning `표시 이름 덮어쓰기`; research `displayName(덮어쓰기)`; 인물 항목 (표시 이름 fixed field); rule 12 §1 (◆ 표시 이름) |
| Q-05 | Choice options: how does an option get a speaker (rule 12 gives spoken options a voice ID `HRT_0107_045_1`)? Is an option only a label, or a label plus a spoken line? | Rule 12 §4; planning `대사만 달라지고 다음 블록에서 합류`; board `Blocks` |
| Q-06 | Voice ID format: episode code for `막 › 장면`, 4-level and episode-less structures; numbers above 99/999; the `ID 형식 · 바꾸기 ›` editor and the `직접` mode UI | Planning 범위 (`HRT_0107_043`); rule 12 §6; `ProjSettings` |
| Q-07 | Renumbering vs "numbers are never reused": after `다시 매기기` a block can take a number whose discarded voice ID exists (auto ID equals a `버린 ID`). What is `nextBlockNumber` after renumbering, and do trashed blocks keep their old numbers (collision on restore)? [PROPOSED: counter unchanged; trashed blocks keep numbers; an automatic ID equal to a discarded ID raises the `▲` duplicate check] | Rule 12 §5 (다시 매기기; 지운 번호는 다시 쓰지 않고), §6 (겹치지 않는 이유) |
| Q-08 | What is the `낭독` preview (speech synthesis, or a reading layout)? | Planning 공통 개념; `Layers`; vocabulary memory (open word) |
| Q-09 | `호칭 힌트`: which addressees are shown above a line, and in what format? | Planning 인물 말투; `Layers` (`대사 위 호칭 표시`); rule 13 §4 |
| Q-10 | Do `괄호 지문` go into game exports (JSON text, CSV 대사)? Are link runs flattened to plain text? | Rule 21 §4, §8 (`미정`); planning PDF option only |
| Q-11 | Direction cues: the full `화면 효과` list (rule 22: 페이드 · 암전; planning JSON: `shake`; research: overlay, delay, caption, dialog.hide), whether BGM has a stop cue, and the cue editor | Planning 블록 종류 and JSON 골격; rule 22 §2; research §3 |
| Q-12 | Where is kind change offered (no board draws it), and which conversions are allowed, especially to/from `연출 지시` and `선택지`? | Rule 12 §4 (대사 → 나레이션 is the only example); check center `나레이션으로 바꾸기` |
| Q-13 | Backspace merge between blocks of different kinds or speakers. The rule's own toast `◆ 1개 #042로` merges 하르트's `#043` into 리엔's `#042`. | Rule 12 §4 |
| Q-14 | Are display-layer choices per project, per device, or app-wide? | Planning 공통 개념 (보기에서만 감춘다); 저장 파일 양식 §6 (app settings not in the file) |
| Q-15 | What does the `참고` tab show for narration, direction, system text and memo blocks? | Rule 11 §2–§3; `CSheetRef` (dialogue only) |
| Q-16 | `모듈` tab: how `오늘` characters are counted (net? which kinds? also on Home), where `목표 2,000자` is set, which cards ⋯ can add, and the scope of the `점검` card (scene, episode or project) | `CSheetMods`; Home (`1,240 오늘 쓴 글자`); planning 모듈 |
| Q-17 | Entry to block select mode; the `옮기기` destination picker; the scope of `모두` | Rule 12 §3 (`이 화면들은 아직 시안이 없다`), §8; `RSSelect` |
| Q-18 | Restore position when new blocks were inserted between the remembered neighbours | Rule 12 §7 (앞뒤 블록 사이) |
| Q-19 | Cut blocks that are never pasted; plain text pasted from outside (one block per line or paragraph? which kind?) | Rule 12 §3 (잘라내기 = 옮기기); rule 22 §4 (pasted head-like line → narration) |
| Q-20 | After `이대로 두기` on a `근거 지워짐` record, does the dotted mark stay? | Rule 12 §2 (choice survives restore); rule 20 §3 (`이대로 두기` keeps the dotted look, moves to `맞다고 표시한 것`) |
| Q-21 | The candidate list of `다른 블록에 잇기` / `블록에 잇기` beyond its first entry (scene only? search?) | Rule 12 §1, §2 |
| Q-22 | Scene-head format conflicts: (a) rule 22 shows `씬 1 · 잿더미 시장 · 약 7일째`, but rule 11 §6 makes an episode's first scene `1일째` even when approximate (1-6 씬 1 = `1-6 · 1일째 낮`), and drops the band; (b) flashback order `씬 2 · 회상 · 하층 구역 · 1195` (rule 22) vs `씬 2 · 하층 거리 · 회상 · 1195` (`RTFlashback`); (c) whether approximate times show a band in the head | Rule 22 §2 table; rule 11 §6 table and 경계 사례; `RTFlashback` |
| Q-23 | `RTCascade` shows `전환 문구 · 이틀 뒤 새벽 · 자동`, which neither template (`장소 · 시각`, `장소만`) can produce. Is there a relative-time template, or is the board wrong? | `RTCascade`; rule 22 §2 |
| Q-24 | `◇ 전환 문구 · 쓴 뒤 … 바뀜`: compare the scene's own inputs or its resolved values (a cascade from an earlier scene changes the resolved day)? Which check-center filter holds the two transition checks (`이동` or a new `씬`)? | Rule 22 §2, §7; rule 11 §4 |
| Q-25 | Engine JSON: final format and versioning, units of the time fields, the transition ID string (`씬 키에서 나온 영구 ID`), whether block numbers are exported | Planning 남은 확인; rule 22 §6; 저장 파일 양식 §1 |
| Q-26 | JSON shape of a direction block with several cues (`#041` holds 배경 + BGM) vs "연출은 한 타입 아래 op" | `RSSelect`; planning JSON 골격 |
| Q-27 | CSV `상태 · 바뀜`: changed ID or changed line text since the last export? This needs a per-block export ledger no source defines. Also: rows for spoken choice options; `화자` as name or display name | Rule 12 §6; rule 22 §6; rule 19 §5 |
| Q-28 | `▲ 숨긴 씬으로 가는 선택지` assumes a choice leads somewhere, but options always rejoin at the next block. Which choices count — only a choice that is the last block before a hidden scene? | Rule 19 §5; rule 12 §4 |
| Q-29 | Assets: entry point, add/edit form, whether audio and image files are packaged (`assets/index.json` is described for images), deleting a used asset, and the two counts `이 에피소드 3회` / `6회` | `Assets`; planning 에셋; 저장 파일 양식 §2 |
| Q-30 | Whether "a first reveal cannot sit on a choice option" is formally added to rule 12 (rule 21 leaves it to the lead); this spec applies it | Rule 21 §5, §8; rule 12 §4 |
| Q-31 | `앞에 새 씬`: rule 22 puts it in the scene-head ⋯, but `RSHeadMenu` shows four rows only; and the caret can never be above a scene's first block | Rule 22 §4; `RSHeadMenu` |
| Q-32 | Rule 22 makes terms in direct transition text subject to the pre-reveal `▲`, but rule 21 counts only `@` links and does not list transition text among scanned texts. Can direct text hold `@` links, and is it scanned for candidates? | Rule 22 §3; rule 21 §2, §4 |
| Q-33 | Is there a direct way to delete one non-empty block outside select mode (e.g. in `속성` or a ⋯)? | Rule 12 §2 (writers delete while typing); `RSSelect` |
| Q-34 | Speaker of a new dialogue block when no earlier dialogue exists; does an empty new dialogue block without a speaker count for `▲ 화자 없음`? [PROPOSED: no speaker; only non-empty blocks count] | Planning 화자; check center table |
| Q-35 | Time default for a new scene: rule 22 prefills `다음 씬 만들기` (and story structure's `새 씬`, "같은 동작") from the previous scene; rule 11 §4 says a scene inserted in the middle starts with no time. Which applies to `새 씬` between two scenes and to `앞에 새 씬`? | Rule 22 §4; rule 11 §4 |
| Q-36 | Rule 12 §3 says records moved behind the writing line become 예정, but with rule 11 §1's definition (end of the furthest scene with content) a moved block always gives its destination content and extends the writing line, so a block-anchored record can never become 예정 by a move. Is a different writing-line definition meant (e.g. memo-only blocks do not count as content)? | Rule 12 §3; rule 11 §1 |

