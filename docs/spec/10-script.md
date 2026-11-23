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

**0.2.0 update (2026-11-24).** Sections marked `(0.2.0)` were rewritten for app version 0.2.0. Sources, in this order of precedence:
1. `docs/status/0.2.0-scope.md` (frozen scope; its defaults D1 … D34 apply here).
2. Planning `u14-script-chat-input.md` rows 1–21 (written below as `U-14 #n`) and `decisions-2026-11-23.md`.
3. Planning `icon-buttons.md` (icon class, icon name and accessibility name of each button). It wins over a board where they differ.
4. Board generator `design/night-manuscript-r2/gen/scr_u14_input.py`, boards `U14Input · U14Typing · U14Long · U14Sent · U14SwipeRight · U14Delete · U14SwipeLeft · U14After · U14Edit · U14Split · U14Merge · U14Kind · U14Cue · U14CueGame · U14Choice · U14Find · U14ReadOnly · U14Wide · U14SceneNext`.

Rules for the 0.2.0 parts:
- Korean copy in `code` comes from those sources. Copy that no source gives is a `TODO_COPY_SCR_…` constant in the `…Copy` object, never invented prose (scope §1). The spec names the constant, and a suggested wording where useful, as `TODO_COPY_SCR_X` (suggestion `…`). The suggestion is not shipped copy.
- `[DEFAULT]` marks a behaviour the sources leave open and this spec had to fix so the lane can build it. Each one is listed in section 6 (Q-37 onward), so the user can overrule it as a 0.2.x usability fix.
- `게임 제작 도구` is the per-project switch of scope G-1 … G-7 (owner: library and settings). "Switch on" and "switch off" below mean that switch. SCR-79 lists what it hides in the script.
- Text that says "Superseded (0.1.0)" describes behaviour that 0.2.0 removes. Lanes delete or stop calling the code that implements it. They do not keep it behind a flag.
- Every write goes through the path `DECISIONS.md` §1 names: block edits through `domain:script`, new scenes through `domain:actions` `SceneActions`. All motion goes through `CivilightMotion`.

---

## 1. Scope

### 1.1 What this area owns

1. The script screen on cover and inner layouts: header, manuscript, **(0.2.0)** the input bar with its tool row (replacing the command-bar row and keyboard toolbar), the `찾기` panel, side panel (inner) / bottom sheet (cover) with tabs `시간 · 참고 · 속성 · 모듈`.
2. Blocks: `대사` (dialogue), `나레이션` (narration), `연출 지시` (direction cues: 배경 · BGM · 효과음 · CG · 화면 효과), `선택지` (choice), `시스템 텍스트` (system text), `작가 메모` (⌁ author memo), and the inline `괄호 지문` (parenthetical stage directions) inside dialogue.
3. Speakers on blocks: inheritance, the speaker picker sheet, speaker label and display-name rendering in the manuscript.
4. Block identity: hidden permanent key, per-episode block number `#043`, renumbering, voice IDs (자동 · 고정 · 버린 ID).
5. Editing behaviour: **(0.2.0)** send, `고치기`, `나누기`, `합치기`, `이 뒤에`, swipe actions, Enter as a line break / undo / autosave / keyboard handling. Also 낭독 (SCR-28), find and replace in the manuscript (SCR-75), episode versions (SCR-76), foreshadowing marks on blocks (SCR-77) and the per-block display name (SCR-78).
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

### SCR-01 · Script screen (cover layout) (0.2.0)
**Priority** P0 · **needsExternal** no · **Boards** `U14Input`, `U14Typing`, `U14Sent`, `U14SceneNext`, `RSHead` · **Sources** U-14 #1, #4, #6, #12–#14, #16, #17, #20, #21; icon list (U-14 rows); DS GlassBars, ScriptBlock; DS-65, DS-69

The screen shows one episode's manuscript as a continuous scroll of scenes. Each scene starts with its scene head (SCR-46), followed by its blocks in order. The manuscript is always in the reading state: there is no editing state inside a block (SCR-14). Text is written only in the input bar at the bottom (SCR-70).

- **Entry.** Other areas open the script at a target `(sceneKey, blockKey?)`: project home `이어 쓰기`, episode or scene rows in story structure, search results, `찾기` results (SCR-68) and check-center `원고로 가기`.
  - With `blockKey`, that block is selected and scrolled so it sits directly above the input bar (SCR-71).
  - Without `blockKey`, the scene's last block sits directly above the input bar. An empty scene puts its head there instead.
  - Home `이어 쓰기` uses `lastWritingPosition` (SCR-59).
- **Scroll unit.** The whole episode scrolls as one list. Moving past the last scene does not load the next episode (unchanged).
- **Top glass bar, cover, keyboard down** (height 118, blur 36, faded bottom edge; the manuscript scrolls under it):
  - Line 1, mono 11 `ink3`: the position line (SCR-02), e.g. `1-7 · 씬 3 · 2일째 새벽`.
  - Line 2, serif bold 19: the title (Q-01 still open).
  - Right side, in this order:
    - the position chip `이 씬 · 1-7` (SCR-02);
    - the `찾기` icon button: icon `search` 20 dp, touch target 44, accessibility name `찾기 · 명령`; it opens SCR-68;
    - the `표시` icon button: icon `layers` 20 dp, touch target 44, accessibility name `연출 · 메모 표시`; it opens SCR-26.
  - The board `U14Input` still draws `찾기` with a text label. The icon list makes it icon only, and the icon list wins (scope ICON).
- **Top bar while the keyboard is up** (the input field has focus): one 76 dp line holding the position line (mono 11, one line, ellipsis), the chip `이 씬 · 1-7` and the `찾기` icon button. There is no `표시`, no `완료` and no `저장됨`. The system back gesture or the IME's hide key closes the keyboard; nothing else does.
- **Manuscript.** Blocks are typeset directly on `bg`: block gap 26 dp, side padding 24 dp. The speaker name sits above the line (SCR-20). Text is serif 18/1.7 by default. Block numbers show only on the blocks SCR-21 lists.
- **Bottom: the input bar (SCR-70).** It replaces every row of the 0.1.0 bottom bar. The script screen declares **no** command bar (DS-65): its single bottom bar is the input bar. There is **no recorder mic** anywhere on the script screen (U-14 #14, scope S-8).
- **Bottom padding** of the manuscript = the input bar's height, plus the keyboard when it is up. How the manuscript moves when that changes is SCR-71.
- **States.**
  - Empty episode (no scenes): the manuscript shows the single line `씬 없음`. The input bar's place label reads `씬 없음`, `보내기` is off and `다음 씬` is on (it creates 씬 1, SCR-74).
  - Empty scene: only its head is drawn. A send appends the scene's first block. **Superseded (0.1.0):** the automatic empty dialogue block with a caret.
- **Autosave.** Every committed operation (send, `고치기`, `나누기`, `합치기`, swipe actions) is written to the app DB at once. The unsent text in the input bar is view state (SCR-70); it is never part of the project file.
- **Superseded (0.1.0):**
  - the bottom bar's speaker pill, layers icon and `줄바꿈` pill;
  - the command field `명령 또는 검색 · 인물 · 블록 · 씬 이동` and the Life Recorder mic;
  - the editing header with `저장됨` and `완료`.

### SCR-02 · Header position line and position label (`이 씬 · 1-7`) (0.2.0)
**Priority** P1 · **needsExternal** no · **Boards** `U14Input`, `U14SceneNext`, `RSHead`, `CSheet*` · **Sources** rule 11 §3; rule 22 §6 (머리말); U-14 #20, #21; scope S-1, D9

- **Position line (머리말).** A short form of the scene's place and time, drawn from scene properties only: `{episode} · 씬 {ordinal} · {day label}`. The day label is the short form of rule 11 §6 (`2일째 새벽`, `약 13일째`, `회상 · 1195`). A scene with no time ends after `씬 {ordinal}` (unchanged).
- **Which scene it shows (0.2.0, U-14 #21).** The scene of the manuscript item directly above the input bar, computed by `SceneInView.target` (SCR-71). The input bar's place label `씬 {n} 끝에` uses the same function, so the two always name the same scene.
  - A selected block in another scene does not change it.
  - **Superseded (0.1.0):** "the scene of the caret or selected block, else the block under the top bar", and the `· 저장됨` form of the line.
- **Position label.** A chip without border reading `이 씬 · {episode}` (e.g. `이 씬 · 1-7`), accessibility name `이 씬 · 1-7 · 렌즈를 이 씬으로 맞추기`. It is not a lens; the script has no lens (rule 11 §3, scope S-1).
  - Tapping it opens a one-row menu with the action `렌즈를 이 씬으로 맞추기` (U-14 #20 keeps this path).
  - Choosing the action sets the app lens to the end of the scene in view (SCR-71). By D9 that point lasts until the project is closed; the next open starts at `최신`.
  - The toast `렌즈 · 1-7 씬 3` stays `[PROPOSED]` from 0.1.0. Whether S-1 meant to remove this action is Q-37.
- *Source note.* Boards `WideRef`, `WideMods` and `WideProps` still draw `최신 · 1-7`; rule 11 §3 replaces it with `이 씬 · 1-7` (unchanged).

### SCR-03 · Inner (unfolded) layout: manuscript + side panel (0.2.0)
**Priority** P1 · **needsExternal** no · **Boards** `U14Wide`, `Wide`, `WideRef`, `WideProps`, `WideMods`, `MFoldScript` · **Sources** planning 화면 · 디자인; U-14 #12; DS Sheet; DS-65

- Width class Medium or Expanded (≥ 600 dp): a manuscript column of fixed reading width 560 dp starting 40 dp from the left, and a side panel 290 dp wide on the right edge. The panel is full height, glass, with a left hairline (`line`).
- The header spans the manuscript area only (right edge 312 dp from the screen's right). It holds the position line, the title (serif 21) and, on the right:
  - the chip `이 씬 · 1-7`;
  - `찾기`: icon only, `search`, accessibility name `찾기 · 명령`;
  - `표시`: icon only, `layers`, accessibility name `연출 · 메모 표시`.
  - The board `U14Wide` draws both with text labels. The icon list wins.
- **Input bar (0.2.0, U-14 #12).** It sits under the manuscript column only:
  - with the panel open, left 40 dp and right 314 dp, and its glass ends at the panel's edge;
  - with the panel closed, left 40 dp and right 40 dp `[DEFAULT]`.
  - The side panel keeps its full height and is never covered by the bar.
  - **Superseded (0.1.0):** the bottom glass and command bar spanning the manuscript area.
- The panel holds the same content as the cover bottom sheet (SCR-31): tabs `시간 · 참고 · 속성 · 모듈` and `닫기`. Closing the panel gives the manuscript the full width but keeps the 560 dp reading column.
- The script ignores the `펼친 화면 배치` setting. Tabletop posture is SCR-69.

### 2.2 Blocks

### SCR-04 · Block model and common rendering (0.2.0)
**Priority** P0 · **needsExternal** no · **Sources** planning 스크립트 편집기; rule 12 §1, §5; rule 22 §3; 저장 파일 양식 §4; DS ScriptBlock; U-14 #4, #8, #11, #20; boards `U14Edit`, `U14Sent`, `U14Split`, `U14Merge`

- The manuscript of a scene is an ordered list of blocks. Kinds: `대사`, `나레이션`, `연출 지시`, `선택지`, `시스템 텍스트`, `작가 메모`. The kind menu (SCR-12) shows them as `대사 · 나레이션 · 연출 · 선택지 · 시스템 · 메모`. Scene boundaries are scene heads (SCR-46), not blocks.
- Every block has a hidden permanent key and a per-episode number `#043` (SCR-21). Order is an insertable order key.
- Text is a list of runs (plain, paren, link, break). **(0.2.0)** A line break typed with Enter in the input bar is a `TextRun.Break` (SCR-15).
- **(0.2.0)** A `연출 지시` block now also has text runs (its text line, SCR-08), besides its optional cues.
- **Looks** (selection, editing and new blocks):

| State | Look | Number shown |
| --- | --- | --- |
| Selected (tap, SCR-14) | `sel` background, 2 dp `acc` bar on the left edge, 12 dp vertical padding | yes |
| `고치는 중` (SCR-72) | `sel` background, 3 dp `acc` left bar, 1.5 dp dashed `acc` outline inset 1.5 dp, text `ink2`, tag `고치는 중` (Noto Sans KR 600 11/16 `acc`) after the speaker name or label | yes |
| `이 뒤에` target (SCR-73) | number shown; a 2 dp `acc` insertion line with an 8 dp dot at its left end, drawn under the block across the 24 dp side padding | yes |
| New (from a send, a `나누기` or a `합치기`) | `sel` background (no bar); tag `새로` after a send, `나눔` on the back block of a `나누기`, `합침` on the merged block (same tag style) | yes |

  - The `새로 · 나눔 · 합침` look lasts until the next send, tap, swipe, `고치기`, or the start of a manual scroll `[DEFAULT]`.
  - There is no caret in a block any more. **Superseded (0.1.0):** the 2×20 caret bar and the fixed writing line inside blocks.
- **Empty (0.2.0).** A block is empty when every text run is blank after trimming (Break runs count as blank), **and**:
  - for `연출 지시`, it has no cue;
  - for `선택지`, every option is blank;
  - **and** no `◆`, `◇` or foreshadowing mark (SCR-77) is anchored on it.

  A `연출 지시` block with text and no cue is **not** empty. `Block.hasContent()` changes to match, for the 집필 선 too: `DIRECTION → cues.isNotEmpty() || !runs.isBlankText()`.
- **Game output.** `대사 · 나레이션 · 시스템 텍스트 · 선택지` and `연출 지시` go to game exports; `작가 메모` never does (`export:false`). Game exports exist only with the switch on (SCR-79).

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

### SCR-07 · 괄호 지문 (parenthetical stage directions) (0.2.0)
**Priority** P1 · **needsExternal** no · **Boards** `Main`, `EditKeyboard`, `Layers` · **Sources** concept memory (NO EXPRESSION TAGS); DS ScriptBlock `.paren`; 저장 파일 양식 §4

- Expressions and actions are written inside the dialogue line in parentheses, e.g. `(숨을 삼키며) 하르트 씨, 방금…`. They are stored as a parenthetical run (not plain text).
- Render: Noto Sans KR 14, `ink3`, inline before/within the line.
- Created in the input bar by typing `(`…`)` or by the tool row `( )` button (**(0.2.0)** SCR-16, enabled for `대사` only), which inserts an empty parenthetical run and puts the caret inside it. [PROPOSED] Typing a `(` in plain text starts a parenthetical run that ends at the matching `)`.
- Hidden by the `괄호 지문` layer (SCR-26). Not a target for term candidates or `▲` (rule 21 §4). Whether parentheticals go into game exports is undecided (Q-10).
- There is no expression picker, expression tag, long-press expression, or `표정` field anywhere.

### SCR-08 · 연출 지시 (direction) block: text line, and cues with `게임 제작 도구` (0.2.0)
**Priority** P1 · **needsExternal** no · **Boards** `U14Cue`, `U14CueGame`, `Main`, `RSSelect`, `Assets` · **Sources** U-14 #11; decisions 2026-11-23 (방향: 게임 제작 기능; 다시 본 결정); scope G-2, §8; rule 22 §2

- **(0.2.0) Text line.** A direction block is written as one plain text line in the input bar, e.g. `배경 하층 거리, 잔불 BGM` or `효과음 바람, 화면 잠깐 암전`. It is stored as the block's `runs`. It has no speaker and no voice ID.
  - `domain:script` must carry runs for `DIRECTION` on every path: `BlockOps.TEXT_KINDS` (or its replacement) includes `DIRECTION` for `insert`, `setRuns`, `split`, merge and `changeKind`. Today `insert` and `changeKind` empty a direction block's runs and `split` refuses it; that is a data-loss bug under 0.2.0 (T-U14-26).
  - Enter still inserts a line break (SCR-15). The kind menu's subline `글 한 줄` describes the usual shape and is not a limit.
- **Cues (switch on only).** A block may also hold cues in order, with kinds `배경 · BGM · 효과음 · CG · 화면 효과`. `배경 · BGM · 효과음 · CG` reference an asset (SCR-61); `화면 효과` carries an effect name.
  - With the switch off, cues are not shown, not editable and **never removed** (scope §8 rule 1). A block's stored cues survive every edit made while the switch is off (SCR-72, T-U14-12).
- **Render** (mono 12/16 `ink3`, 24 dp side padding, no speaker line):

| Stored | Switch off | Switch on |
| --- | --- | --- |
| Text only | `▸ {text}` | `▸ {text}` |
| Cues only (a 0.1.0 block) | `▸ {cue summary}` | `▸ {cue summary}` |
| Text and cues | `▸ {text}` | `▸ {cue summary}` + two spaces + `{text}` |

  - The cue summary is the 0.1.0 form: `배경 {asset name}`, `♪ {asset name}` for BGM, `효과음 {name}`, `CG {name}`, `화면 효과 {name}`, joined with two spaces.
  - With the switch off, a cue-only 0.1.0 block still shows its summary as read-only text, so no 0.1.0 content looks lost (release gate: data loss).
  - The summary never names a locked hidden item.
  - In select mode the parts are joined with ` · `.
- **Editing (0.2.0).** Through `고치기` (SCR-72). With the switch on, the input bar shows the block's cues as chips (below) above its text line.
  - **Superseded (0.1.0):** the `[PROPOSED]` cue sheet that opened on tapping a direction block.
- **Cue chips in the input bar (switch on only, board `U14CueGame`).** They sit inside the field container, above the text, wrapping, with 6 dp gaps.
  - Each cue chip is 36 dp tall, radius 10, `tint` background with a 1 dp `line` inset, mono 12 `ink2`, and reads `{kind} {name}` (e.g. `효과음 바람`, `화면 효과 암전`). A trailing `✕` (icon `close` 14, 30×36 dp, accessibility name `큐 빼기`) removes the cue from the draft.
  - A final round `＋` (icon `plus` 16, 36 dp, `acc` outline, accessibility name `큐 넣기`) opens a menu. Its rows are `배경`, `BGM`, `효과음`, `CG` (each with subline `에셋 고르기`) and `화면 효과` (subline `페이드 · 암전`).
  - Choosing one of the first four rows opens the asset picker for that kind (SCR-61), which has the U-15 create row. `화면 효과` opens a two-row list, `페이드` (`fade`) and `암전` (`blackout`).
  - A 0.1.0 cue with another effect, such as `shake`, is kept and shown by its stored name.
- Hidden by the `연출 지시` layer. A `◆` may be anchored on a direction block.
- The link between a background asset and a place is unchanged (rule 22 §2).

### SCR-09 · 선택지 (choice) block: text only (0.2.0)
**Priority** P1 · **needsExternal** no · **Boards** `U14Choice`, `Blocks` · **Sources** decisions N-23 (선택지는 문구만); U-14 #11, #20; rule 12 §4; rule 21 §5; scope N-23, §8

- The story is linear. A choice block holds 2 or more option lines; only the option text differs, and the story always rejoins at the **next block** (computed, never stored).
- **(0.2.0, N-23) Text only.** An option line has no speaker and gets no voice ID and no suffix (`_1`). To make a line spoken, write a dialogue block after the choice. Choice blocks are not in the dialogue CSV (SCR-64).
  - A 0.1.0 option's stored `speakerKey` or `voice` (fixed ID) is kept unchanged and never shown, exported or regenerated (scope §8).
  - Closes Q-05. **Superseded (0.1.0):** spoken options, `HRT_0107_045_1`.
- Option lines display as `#045-1`, `#045-2` wherever numbers show.
- Render (`Blocks`): the speaker-line slot reads `선택지` (weight 600) + `다음 블록에서 합류` (`ink3`). Each option follows as `{index}` (mono 12 `ink3`) + text (serif 16/26).
- **(0.2.0) Writing.** With the kind chip on `선택지` the input takes one option per line; Enter starts the next option (board `U14Choice`).
  - Each line in the field is preceded by its index, mono 12 `ink3`, 8 dp gap. The index is drawn, not typed.
  - `보내기` is on only when the draft has **at least 2 non-blank lines** (SCR-70).
  - On send, blank lines are dropped and each line is trimmed.
- **Line ↔ option mapping on `고치기`** (`ChoiceLines.apply(old options, lines)`, JVM):
  - Line *i* (1-based, after dropping blank lines) updates the option at order position *i* and keeps its key and any stored speaker or voice.
  - Extra lines become new options with new keys, appended in order.
  - When there are fewer lines than options, the options past the last line are removed. A removed option's fixed voice ID becomes a `DiscardedVoiceId` (reason `DELETED`, with its `optionKey`).
  - The block always keeps at least 2 options, enforced by the send rule.
- `◆` and `◇` anchor on the whole choice block, never on an option line (unchanged).
- Term links in option text count as uses (rule 21 §4, unchanged).

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

### SCR-12 · Kind chip menu (replaces the block add sheet and slash shortcuts) (0.2.0)
**Priority** P0 · **needsExternal** no · **Boards** `U14Kind` · **Sources** U-14 #2, #11, #20; board `U14Kind` (menu rows); icon list (chips are class c)

- **Opened by** the kind chip of the input bar (SCR-70), or by the right-swipe button `종류` (SCR-30, then SCR-13). It is a popover menu (DS Popover; DS-66 gaps) above the chip row: width 230 dp, radius 16, rows 44 dp tall with 14 dp side padding.
- **Rows**, in this order. Name is 500 14/20 `ink`; the subline is 400 12/16 `ink3`, right of the name; the current kind's row has `sel` background and a check icon (`check` 16).

| Row | Subline | Kind |
| --- | --- | --- |
| `대사` | (none) | `DIALOGUE` |
| `나레이션` | `화자 없음` | `NARRATION` |
| `연출` | `글 한 줄` | `DIRECTION` |
| `선택지` | `화자 없음 · 2개 이상` | `CHOICE` |
| `시스템` | `화면 문구` | `SYSTEM` |
| `메모` | `게임 출력 제외` | `MEMO` |

- **From the input bar:** choosing a row sets the kind chip. It inserts nothing. The draft text stays as typed; only its validation and drawing change (e.g. lines become numbered options under `선택지`).
  - Choosing `나레이션 · 연출 · 선택지 · 시스템 · 메모` dims the speaker chip (SCR-18).
  - In `고치기` the chosen kind is applied to the block on the `고치기` send (SCR-72). Rows the block cannot convert to are dimmed with their reason (SCR-13).
- **From the right swipe `종류`:** choosing a row converts that block in place at once (SCR-13).
- Read-only projects have no input bar and no right swipe, so the menu cannot open.
- **Superseded (0.1.0):**
  - the `블록 추가` sheet and its subline `지금 블록 아래 · 새 대사 화자: …(직전)`;
  - inserting below the current block;
  - the slash shortcuts `/대 /나 /연 /선 /씬 /시 /메`;
  - the sheet's `새 씬` row. New scenes come from `다음 씬` (SCR-74) and the swipe `씬 나누기` (SCR-49).

### SCR-13 · Change a block's kind (0.2.0)
**Priority** P1 · **needsExternal** no · **Boards** `U14SwipeRight`, `U14Kind`, `U14Wide` (`속성 · 종류 · 바꾸기`) · **Sources** U-14 #3, #8, #18; rule 12 §4, §6; rule 23 §4

- **Where (0.2.0).** Three entry points, each through the kind menu (SCR-12):
  - the right-swipe button `종류`, which converts at once;
  - the `속성` tab row `종류` · `바꾸기` (board `U14Wide`), which converts at once;
  - the kind chip during `고치기`, applied on the `고치기` send.
  - Closes Q-12.
- **Conversion check** `KindChange.check(block, to): Allowed | Blocked(reason)` (pure, JVM):

| From → To | Result |
| --- | --- |
| Same kind | No-op; the row is the checked one |
| Any of `대사 · 나레이션 · 연출 · 시스템 · 메모` → another of them | Allowed. Runs carry over unchanged (paren runs stay paren runs) |
| `연출` holding ≥ 1 cue → any other kind | Blocked, reason `TODO_COPY_SCR_KIND_HAS_CUES` (suggestion `큐 있음`). This holds with the switch off too, because the cues would otherwise be lost while hidden |
| Text kind → `선택지` | Allowed when the text splits at `Break` runs into ≥ 2 non-blank lines; each line becomes an option (new keys). Otherwise blocked, reason `TODO_COPY_SCR_KIND_CHOICE_LINES` (suggestion `2줄 이상`). Swipe and `속성` paths only; in `고치기` the send rule (SCR-70) checks the lines instead |
| `선택지` → text kind | Allowed. Option texts are joined in order with one `Break` between them. Option keys are dropped; an option's fixed voice ID becomes a `DiscardedVoiceId` (reason `KIND_CHANGED`) |
| Unknown kind (newer file) → anything | Blocked (no right swipe and no menu on such a block) |

- A dimmed row shows its reason as a subline in `ink3` instead of the normal subline. Its accessibility name is `{row} · 꺼짐 · {reason}`.
- Conversion keeps the block's key and number. Records stay ("기록은 내용에 붙지 종류에 붙지 않는다").
- **Speaker and voice.**
  - Leaving `대사`: the automatic ID disappears; a fixed ID becomes a `버린 ID` (unchanged).
  - To `대사` from the swipe or `속성`: speaker none, so the block shows `화자 없음` and `▲` until a speaker is picked (unchanged).
  - To `대사` in `고치기`: the speaker chip's value.
- One conversion is one undo step. The check-center action `나레이션으로 바꾸기` runs this conversion (unchanged).
- **Superseded (0.1.0):** "conversion to or from `연출 지시` and `선택지` only for an empty block".

### 2.3 Writing and keyboard

### SCR-14 · Reading state only: block tap selects (0.2.0)
**Priority** P0 · **needsExternal** no · **Boards** `U14Input`, `U14Typing` · **Sources** U-14 #4; decisions N-27 (읽기 보기); 저장 파일 양식 §1 (자동 저장)

- **A block tap only selects.** Tapping anywhere on a block selects it: its text, a link, a candidate underline, the speaker label, a margin `▲`. No caret appears and the keyboard does not open (U-14.4).
  - Tapping another block moves the selection.
  - Tapping the selected block again, or empty manuscript space, clears the selection `[DEFAULT]`.
  - Tapping a scene head still opens `씬 · 장소와 때` (SCR-46). Tapping a hidden-scene row still starts the unlock (SCR-58).
- **Selection** drives the side panel or sheet content (SCR-31) and the tool row's `◆` target (SCR-16). It never changes where a send goes (SCR-70, SCR-71).
- **Writing** happens only in the input bar (SCR-70). Changing a block's text, speaker or kind goes through `고치기` (SCR-72), the `속성` tab (SCR-34) or the swipe `종류` (SCR-13).
- **Autosave.** Every committed operation is written to the app DB at once. The `.civilight` file is never written by these operations.
- **Superseded (0.1.0):**
  - the editing state: caret in a block, the 76 dp header with `{episode} · 씬 {n} · 저장됨 / 저장 중` and `완료`, and the `입력 중` label;
  - the fixed writing line inside the manuscript (SCR-71 gives the 0.2.0 anchor rule);
  - opening the speaker picker by tapping a speaker label (SCR-19);
  - opening a link's card by tapping it (SCR-55, Q-38);
  - the candidate popover on tap (SCR-56).
- Swipes (SCR-30) are not taps; they only reveal buttons.

### SCR-15 · Enter, `나누기`, `합치기`, scene-head barrier (0.2.0)
**Priority** P0 · **needsExternal** no · **Boards** `U14Typing`, `U14Edit`, `U14Split`, `U14Merge` · **Sources** U-14 #7, #9, #19; rule 12 §4; rule 22 §3

| Input | Where | Result |
| --- | --- | --- |
| Enter (soft keyboard or hardware) | input field (SCR-70) | Inserts a line break (`TextRun.Break`) at the caret in the draft. It never sends, never splits and never creates a block (U-14.7). Under `선택지` the next line is the next option |
| Backspace | input field | Edits the draft only. It never merges, deletes or changes a block |
| `나누기` | the `고치기` mark row (SCR-72) | **Split** at the input caret: the text before the caret stays in the block (key, number, voice ID and records unchanged). The text after it becomes a new block of the same kind and speaker, with a new key, the counter's next number and a new automatic voice ID |
| `합치기` | the `고치기` mark row (SCR-72) | **Merge** into the previous block. Only when the previous block of the same scene has the same kind **and** speaker (U-14 #19); otherwise it is dimmed with the reason `종류 다름` or `화자 다름`. Never across a scene head: on a scene's first block it is dimmed with `TODO_COPY_SCR_MERGE_SCENE_START` (suggestion `씬 처음`) |

- Record, number and voice consequences of split and merge are unchanged from 0.1.0 (SCR-44): records stay on the front block when splitting; when merging, the later block's records move to the merged block unflagged and its fixed voice ID becomes `버린 ID` (`MERGED`).
- `합치기` between blocks of different kinds or speakers is not possible. This closes Q-13.
- `줄바꿈` is gone (Q-03 closed).
- **Superseded (0.1.0):**
  - Enter = new block (same speaker) and Enter mid-line = split;
  - Backspace at a block's start = merge; Backspace in an empty block = delete;
  - the `줄바꿈` pill.
- Blocks are deleted only with `지우기` (SCR-38).

### SCR-16 · Tool row `( ) · @ · ◆ · ↶` (0.2.0)
**Priority** P0 · **needsExternal** no · **Boards** `U14Typing`, `U14Long`, `U14Edit` · **Sources** U-14 #10; board `TOOLS` (accessibility names); decisions 2026-11-23 (◆ 변화 기록: U-14 도구 줄의 ◆로 쓴다); icon list (`undo`)

- **When.** Only while the keyboard is up **and** the input field has focus (U-14.10). It is not shown for the `찾기` field (SCR-68) or for fields in sheets.
- **Where.** The first row of the input bar, above the chip row (SCR-70). Items are 44×44 dp, radius 12, 4 dp apart, left-aligned, Noto Sans KR 600 14/16 `ink2`.

| Item | Accessibility name | Action | Enabled |
| --- | --- | --- | --- |
| `( )` | `괄호 지문` | Inserts an empty paren run at the input caret and puts the caret inside it (SCR-07) | Kind chip = `대사` |
| `@` | `잇기` | Opens the link picker (SCR-55); the chosen item is inserted as a link run at the input caret | Always |
| `◆` (`acc`) | `변화 기록` | Opens the change-record sheet (SCR-41) for the target block below | When a target block exists |
| icon `undo` 18 | `되돌리기` | Project undo (SCR-17) of the last document operation | When there is something to undo |

- **`◆` target** `[DEFAULT]`:
  - during `고치기`, the block being edited;
  - otherwise the selected block;
  - otherwise none, and `◆` is dimmed.
  - It never targets the unsent draft, which is not a block yet. The sheet's subline names the block (`#043 · 1-7 씬 3`), so the target is visible before anything is recorded.
- **`↶` undoes the document, not the draft** `[DEFAULT]`. Text typed in the input field is not in the project undo history; the IME's own text undo applies to it.
- **Superseded (0.1.0):** the 56 dp toolbar on the keyboard with the kind item `대사 ▾` and the speaker item. Both are chips now (SCR-70).

### SCR-17 · Undo (0.2.0)
**Priority** P0 · **needsExternal** no · **Boards** `U14Delete`, `U14Split`, `U14Merge`, `UndoToast` · **Sources** rule 12; rule 11 §4; rule 22 §5; rule 23 §4; U-14 #10

- One undo history per open project `[PROPOSED scope]`. It covers: send, `고치기` send, `나누기`, `합치기`, swipe `지우기`, swipe and `속성` kind change, speaker change from `속성`, record creation, relocation and resolution, `다음 씬` and every scene operation, renumbering, recorder memo insertion, `이 판 글로` (SCR-76), a find-and-replace run (SCR-75) and foreshadowing marks (SCR-77).
- Each of these is **one** undo step, including compound ones: a merge with its record moves; a split; a replace run over many blocks.
- A toast's `되돌리기` undoes exactly the operation the toast reports. The button is icon `undo` + text `되돌리기` (icon class b).
- Undoing a delete restores the blocks at their original positions and re-attaches relocated records (SCR-43).
- Undo does not touch the input bar's draft. When an undo removes the block an `이 뒤에` mark or a `고치기` points at, SCR-73 and SCR-72 say what happens.
- **Superseded (0.1.0):** coalescing typing in a block into steps; the draft is not in the history (SCR-16). There is still no redo.

### SCR-70 · Input bar (0.2.0)
**Priority** P0 · **needsExternal** no · **Boards** `U14Input`, `U14Typing`, `U14Long`, `U14Sent`, `U14After`, `U14Edit`, `U14Kind`, `U14Cue`, `U14CueGame`, `U14Choice`, `U14Wide` · **Sources** U-14 #1, #2, #5–#8, #10, #11, #15–#18, #20; icon list (U-14 rows); DS-65, DS-69

The single place where script text is written. It replaces the 0.1.0 bottom bar on the script screen.

**Layout (cover).** A bottom glass bar. Its content column has 12 dp side insets and sits 16 dp above the navigation inset while the keyboard is down. Rows top to bottom are 6 dp apart (4 dp while the keyboard is up):

1. **Tool row** (SCR-16), only while the keyboard is up and the field has focus.
2. **Chip row** (min height 36):
   - **Speaker chip** (SCR-18) and **kind chip** (SCR-12): 36 dp tall, radius 18, `chip` background, Noto Sans KR 600 13/16 `ink`, padding 0 8 0 11, a trailing chevron-down icon 14. The text is the value: `리엔 ▾` · `대사 ▾`. The accessibility names are `화자` and `종류`. In the icon list the chips are class c: text, no icon.
   - A dimmed chip has `tint` background, a 1 dp `line` inset, weight 500 and `ink3`. It is not tappable and keeps its value.
   - While a chip's menu or sheet is open, the chip has a 1.5 dp `acc` inset.
   - **Place area**: flex, right-aligned, one line, Noto Sans KR 500 12/16 `ink3`; bold parts are 600 `ink`. It shows exactly one of the states in the table below.
3. **Field row.** A field container (radius 24, `bg`, 1 dp `edge` inset, padding 6 6 6 12, items bottom-aligned, 6 dp gap) holds:
   - **The text field**: serif (Nanum Myeongjo) 15/24 `ink`, min height 36, placeholder `쓰기…` in `ink3`. Under `선택지` each line is drawn with its index (SCR-09). Under `연출` with the switch on, the cue chips sit above the text (SCR-08).
   - **The send button**: 36 dp tall, radius 18, `pri` background, `onpri` content, 44 dp touch target.
     - Normally it is icon only: icon `send` 20 dp, accessibility name `보내기` (icon class a, scope ICON).
     - During `고치기` it shows the text `고치기` (Noto Sans KR 600 13/16, padding 0 13), accessibility name `고치기`. The icon list leaves this button as an open question, so it keeps its text (scope ICON, D28).
     - Off: `tint` background, `ink3` content, not tappable.

**Place area states** (one at a time):

| State | Shows | Notes |
| --- | --- | --- |
| Default | `씬 {n} 끝에` (`씬 {n}` bold) · `다음 씬` button | n = ordinal of `SceneInView.target` (SCR-71). `다음 씬` is SCR-74: icon `next_scene` 16 + text `다음 씬`, `acc` 600, accessibility name `다음 씬 만들기` |
| `이 뒤에` mark (SCR-73) | `#{n} 뒤에` (bold) · `✕` | `✕` = icon `close` 16, 28 dp drawn, 44 dp target, accessibility name `닫기`. No `다음 씬` in this state |
| `고치기` (SCR-72) | `#{n} 고치기` (bold) · `나누기` · `합치기` · `✕` | `나누기` and `합치기` are icon + text (`split`, `merge`; class b), `acc` 600. When off they are `ink3` 500; a dimmed `합치기` shows its reason in 500 11/13 `ink3` under the word (board `U14Edit`), accessibility name `합치기 · 꺼짐 · {reason}`. A dimmed `나누기` has accessibility name `나누기 · 꺼짐` |
| No scene (empty episode) | `씬 없음` · `다음 씬` | `보내기` off |

**Validation** `DraftRules.canSend(draft, mode, gameTools): Boolean` (pure, JVM). `보내기` and the `고치기` button are on only when all of these hold:
- the project is not read-only and a target exists (a scene for a send, the block for `고치기`);
- the content rule for the kind chip:
  - `대사 · 나레이션 · 시스템 · 메모`: at least one non-whitespace character outside `Break` runs (a paren or link run with text counts);
  - `연출`: non-blank text, or (switch on) at least one cue;
  - `선택지`: at least 2 non-blank lines (lines = text split at `Break`).
- So a blank `고치기` send is impossible. Emptying a block is not a way to delete it; use `지우기` (SCR-38).
- A `대사` send with the speaker chip at `화자 없음` is allowed and produces `▲ 화자 없음` (SCR-62).

**Send** (no `고치기` active), through `domain:script` in one undo step:
1. Normalize the draft: trim leading and trailing whitespace and `Break` runs. Under `선택지`, split into lines, drop blank lines and trim each line.
2. Insert position: the `이 뒤에` mark's block (`BlockPosition.After`) if set, else `BlockPosition.SceneEnd(target scene)` (U-14.1, U-14.6).
3. `BlockOps.insert` is called with:
   - kind = kind chip;
   - `speakerKey` = speaker chip for `대사`, else null, with `inheritSpeaker = false`;
   - the runs, cues (switch on, `연출`) or options.
   - The block gets the counter's next number (U-14.20; it may read out of order, e.g. `#058` after `#044`) and an automatic voice ID.
4. Clear the draft's text and cues; the chips keep their values (U-14.2). Clear the `이 뒤에` mark.
5. Scroll to the new block (SCR-71) and give it the `새로` look (SCR-04). It enters with the new-block motion (SCR-67).
6. Update `lastWritingPosition` (SCR-59).
- A send shows no toast; the new block is visible above the bar.

**Height (U-14 #16).**
- The field grows upward from 1 to 6 lines; its text area tops out at 6 × 24 = 144 dp (156 dp with padding). The manuscript moves up with it (SCR-71).
- From the 7th line on, the field scrolls inside itself. It shows a 3 dp scrollbar (`line` track, `ink3` thumb) and an 18 dp fade at its top edge.
- The caret line stays visible by DS-69's nested-scroll rule, including the moment the field first overflows (U-14.16). `CaretInView.delta` is applied to the field's own viewport first, then to the window band.

**Inner screen** (SCR-03): the same bar under the manuscript column only.

**Keyboard.**
- Tapping the field focuses it and opens the IME. While the IME is up, the bar sits directly on the keyboard. DS-69's "bottom glass bar while typing" rule treats the script's input bar as its keyboard toolbar, so it stays drawn.
- The manuscript stacks above the bar (board `U14Typing`).
- The IME action key is Enter, which inserts a line break (SCR-15).

**Draft** (view state, SCR §3.2 `draft`).
- The draft holds the field's runs, the draft cues, both chip values, the mark (none, `이 뒤에`, or `고치기` with the set-aside draft), and the input caret.
- It is stored per project in the app DB and never in `.civilight`. It survives leaving and reopening the script, fold and unfold (SCR-66) and process death.
- With the switch off, draft cues are kept but not drawn.

**Read-only:** the bar is replaced (SCR-60).

**Superseded (0.1.0):** the bottom bar of SCR-01 0.1.0, SCR-12's add sheet and SCR-16's toolbar.

### SCR-71 · Scene in view, bottom anchoring, scroll to the new block (0.2.0)
**Priority** P0 · **needsExternal** no · **Boards** `U14SceneNext`, `U14Sent`, `U14Long` · **Sources** U-14 #6, #16, #17, #21; DS-69; scope U-14.21 (motion through `CivilightMotion`)

**Scene in view** `SceneInView.target(items, inputTop): SceneKey?` (pure, JVM; `feature:script` layout logic).
- `items` are the laid-out manuscript items in manuscript order. Each item has `(sceneKey, kind, top, bottom)`, with kind one of `HEAD`, `BLOCK` or `HIDDEN_ROW` (a locked hidden scene, SCR-58). The `씬 없음` line of an empty episode is not an item. `inputTop` is the y of the input bar's top edge, keyboard included.
- Rule:
  1. The candidates are items with `top < inputTop` whose kind is not `HIDDEN_ROW`. The result is the scene of the **last** candidate in manuscript order: the item directly above the input bar, or the one the bar's edge cuts through.
  2. If there is no candidate, the result is the scene of the **first** item at or below `inputTop` that is not `HIDDEN_ROW`.
  3. If there is none (no scenes, or only locked hidden ones), the result is null. The place area then reads `씬 없음` and `보내기` is off.
- **A locked hidden scene is never a target.** Sending into it would place text in a scene the writer cannot see.
- An empty scene's `HEAD` item counts, so a scene made by `다음 씬` becomes the target at once.
- The position line (SCR-02) and the place area use this one result, so they always name the same scene (U-14.21).
- It is recomputed on scroll, layout change, keyboard change and document change.

**Highlight on change** (U-14 #21).
- When the result changes from one non-null scene to another, the `씬 {n} 끝에` part flashes once (board `U14SceneNext`): a `sel` fill (radius 8, height 26, padding 0 8) and a 2 dp `acc` bar along its bottom edge that runs out left to right.
- Timing: `CivilightMotion` token `standard`, total 600 ms `[DEFAULT]`. With reduced motion, a 120 ms fade in and out of the fill only.
- No flash on first composition. The position line changes text without motion.

**Bottom anchoring** (U-14 #16, #17). Applies when the input bar's height changes (field growth, tool row, mark row) or the keyboard opens, closes or resizes:
- The bottom edge of the item directly above the input bar keeps its distance to the bar's top edge, so the manuscript moves up with the bar ("그만큼 원고가 올라간다"). Implementation: scroll by the height delta in the same frame (snap, no animation, as DS-69).
- **At bottom**: the last item's bottom is within 1 dp of the end of the content. While at bottom, new content, the bar growing, or the manuscript first overflowing the screen keeps the last item directly above the bar (bottom-anchored list).
- A user drag or fling suspends anchoring until it ends, as in DS-69.

**Scroll to the new block** (U-14.17). After every send (and after `나누기` and `합치기`):
- scroll so the new (or merged) block's bottom sits 12 dp above the input bar's top edge;
- if the block is taller than the visible band, its top aligns with the band top instead (DS-69 band);
- this happens whether the view was at the bottom, scrolled up, or the block landed mid-episode (an `이 뒤에` send, or a send to a scene that is not the last one).
- Motion: `CivilightMotion` scroll, `decelerate` 320 ms. With reduced motion it snaps.

**Replaces** the 0.1.0 fixed writing line (SCR-14 0.1.0) and its `[PROPOSED]` minimum scroll.

### SCR-72 · `고치기`, `나누기`, `합치기` (0.2.0)
**Priority** P0 · **needsExternal** no · **Boards** `U14SwipeRight`, `U14Edit`, `U14Split`, `U14Merge` · **Sources** U-14 #8, #9, #19, #20; board toasts; rule 12 §4

**Start.** Right swipe (SCR-30) › `고치기` on block B.
1. If the draft has text, cues or a mark, it is set aside whole (text, cues, chip values, mark). It comes back when `고치기` ends.
2. The input field gets B's runs exactly (paren runs and link runs with their link keys; `Break` runs as lines). Under `선택지`, B's option lines are one per line. Under `연출` with the switch on, B's cues become cue chips (SCR-08).
3. The speaker chip gets B's speaker (or `화자 없음`), and the kind chip gets B's kind.
4. B gets the `고치는 중` look (SCR-04). The place area shows `#{n} 고치기 · 나누기 · 합치기 · ✕`, and the send button shows the text `고치기`.
5. The field takes focus with the caret at the end of the text. The manuscript scrolls so B sits directly above the bar (SCR-71 rule for a new block).

While `고치기` is active:
- The right swipe is not available on any block. The left swipe and its panels still work.
- `다음 씬` is not shown.
- Chip changes do **not** touch B until the `고치기` send `[DEFAULT]`. U-14 #8 "칩을 바꾸면 그 블록의 화자 · 종류도 함께 바뀐다" is met on the send, and `✕` can still undo everything. See Q-39.

**`고치기` send** (enabled by SCR-70 validation), in one undo step, `ScriptEdit.apply(doc, B, draft)` in `domain:script`:
- Text: B's runs become the normalized draft runs. Link runs keep their link keys.
- `선택지`: the options follow the line ↔ option mapping of SCR-09.
- Kind: if the kind chip differs from B's kind, B is converted (SCR-13). Rows blocked by SCR-13 cannot be chosen in the menu.
- Speaker: for `대사`, the speaker chip; for other kinds, none.
- Cues:
  - switch on: replaced by the draft's cue chips;
  - **switch off: left exactly as stored** (scope §8 rule 1).
- B keeps its key and number. Records, `◇` and foreshadowing anchors stay on B. Voice consequences follow SCR-23 (speaker change: automatic ID recomputed, fixed ID flagged `◇ 화자와 다름`).
- If nothing changed, the send ends `고치기` and makes no undo step.
- Afterwards `고치기` ends and B stays selected. `lastWritingPosition` = end of B.

**`✕`.** Ends `고치기` and discards the edits; B is unchanged.

**On end** (send, `나누기`, `합치기` or `✕`): the set-aside draft comes back whole, including the chip values from before `고치기` `[DEFAULT]` (Q-40). If nothing was set aside, the field is empty and the chips return to their values from before `고치기`.

**B is no longer live and visible while editing** (an undo removes it, it is deleted in select mode, or its scene or speaker becomes hidden and locked): `고치기` ends.
- If B was removed, the edited text stays in the field as a normal draft with no mark, so nothing typed is lost `[DEFAULT]`.
- If B became hidden and locked, the field is **cleared** and the set-aside draft comes back. Text from a locked item never stays on screen (T-U14-25).

**`나누기`** (U-14 #9).
- **On when:**
  - the kind chip is not `선택지`;
  - the text before the input caret, trimmed, is non-blank;
  - the text after it, trimmed, is non-blank.
- Otherwise it is dimmed.
- **Apply**, one undo step:
  1. B is first updated exactly as a `고치기` send with the text **before** the caret (chips applied).
  2. A new block N is inserted directly after B with the text **after** the caret (whitespace at the cut trimmed on both sides, as `BlockOps.split`).
  3. N has the same kind and speaker as the updated B, the counter's next number, an automatic voice ID, no records and no cues (cues stay on B).
  4. `아님` decisions split as `Containers.splitRejections`.
- After: `고치기` ends. B is selected, and N has the `나눔` look. The toast reads `#{B} 나눔 · 뒤 글 → #{N}` (e.g. `#044 나눔 · 뒤 글 → #058`) with `되돌리기`.

**`합치기`** (U-14 #9, #19).
- **Check** `MergeCheck.reason(prev, chipKind, chipSpeaker): Reason?` (pure, JVM). P is the live block directly before B in the same scene. The checks run in this order:
  1. There is no P (B is the scene's first block) → `TODO_COPY_SCR_MERGE_SCENE_START`.
  2. P's kind ≠ the kind chip → `종류 다름`.
  3. The kind is `대사` and P's speaker ≠ the speaker chip (two "none" values count as equal) → `화자 다름`.
  4. Otherwise null, and `합치기` is on.
  - Kind is checked first, so a dialogue block after a narration block shows `종류 다름` (board `U14Edit`).
  - The check uses the chip values, so it updates live as the chips change.
- **Apply**, one undo step:
  - P's runs become `Runs.join(P.runs, draft runs)` (one space between). For `선택지`, P's options are followed by the draft lines as new options. For `연출`, P's cues are followed by B's cues.
  - P keeps its key, number, kind, speaker and voice. B is removed (not trashed).
  - B's records move to P unflagged; B's fixed voice ID becomes `버린 ID` (`MERGED`). Acknowledgements and `아님` decisions follow (as `mergeIntoPrevious`).
- After: `고치기` ends. P has the `합침` look.
  - The toast reads `#{B}{을|를} #{P}에 합침` (board: `#058을 #045에 합침`). When records moved, the 0.1.0 record part is appended: ` · ◆ {n}개 #{P}로` (`Toasts.recordsTo`). Both parts carry `되돌리기`.
  - **Object particle after a number** `Particles.objectAfterNumber(n)`, by the last digit as read in Korean (영 일 이 삼 사 오 육 칠 팔 구): last digit 0, 1, 3, 6, 7, 8 → `을`; 2, 4, 5, 9 → `를`.

### SCR-73 · `이 뒤에` (insert after a block) (0.2.0)
**Priority** P0 · **needsExternal** no · **Boards** `U14After` · **Sources** U-14 #5

- Right swipe (SCR-30) › `이 뒤에` on block X sets the mark `이 뒤에 X`.
  - The place area shows `#{X} 뒤에 · ✕`.
  - X shows its number, and the insertion line is drawn under X (SCR-04).
  - The manuscript scrolls so X sits directly above the bar.
  - The draft text and chips are kept.
- The next send inserts directly after X (`BlockPosition.After(X)`). Then the mark clears and sends go to the scene end again (U-14.5).
- `✕` clears the mark without sending.
- **The mark clears silently** when X is deleted, moved to another scene, hidden and locked, or removed by an undo; the place area returns to `씬 {n} 끝에` `[DEFAULT]`.
- Setting `이 뒤에` on another block moves the mark. It cannot be set during `고치기` (no right swipe).
- `다음 씬` is not offered while the mark is set; close the mark first (board `U14After`).

### 2.4 Speakers

### SCR-18 · Speaker chip value (replaces speaker inheritance per block) (0.2.0)
**Priority** P0 · **needsExternal** no · **Boards** `U14Input`, `U14Kind`, `U14Choice` · **Sources** U-14 #2, #20; decisions N-23; concept memory; feedback-no-guessing

- The speaker chip holds the speaker of the **next send**. It keeps its value after a send and never changes on its own; there is no automatic alternation between speakers (U-14.2).
- **Initial value** `[DEFAULT]`:
  - the chip values saved in the project's `draft` view state;
  - if there are none (first open in this project on this device): kind `대사`, and speaker = the 0.1.0 `직전 화자` at the end of the target scene (the nearest dialogue block with a speaker above that position, inside the episode);
  - if there is no such block: none, and the chip reads `화자 없음 ▾`.
- **Dimmed** (not tappable, value kept) while the kind chip is `나레이션 · 연출 · 선택지 · 시스템 · 메모`.
  - U-14 #2 names `나레이션 · 연출 · 메모` and #20 adds `선택지`. `시스템` is added here because a system-text block has no speaker (SCR-10) `[DEFAULT]`.
  - Switching back to `대사` shows the kept value.
- A non-dialogue send stores no speaker.
- If the chip's character is moved to the trash, or becomes hidden and locked, the chip resets to `화자 없음` `[DEFAULT]`. A locked hidden name never shows in the chip.
- During `고치기` the chip holds the edited block's speaker; afterwards it returns to its previous value (SCR-72).
- **Superseded (0.1.0):**
  - each new dialogue block taking the nearest speaker above it;
  - the `직전 화자` label after a new block's name (`MType`);
  - the sheet subline `새 대사 화자: 리엔(직전)`.

### SCR-19 · Speaker picker sheet (`화자`) (0.2.0)
**Priority** P0 · **needsExternal** no · **Boards** `Picker`, `MSheet` · **Sources** planning 화자; feedback-no-guessing; rule 19 §6; rule 20 §2; U-14 #2; scope U-15.3

- **Opened from (0.2.0):**

| Entry | Effect of a tap on a row |
| --- | --- |
| Speaker chip (SCR-18) | Sets the chip (during `고치기`: the chip, applied on the send) |
| `속성` tab `화자` · `바꾸기` | Sets the selected block's speaker at once (one undo step) |
| Check center `화자 고르기` | Sets that block's speaker at once |

  - **Superseded (0.1.0):** opening it from a block's speaker label, the bottom-bar speaker pill, or the keyboard toolbar.
- Sheet: title `화자`, `닫기` on the right (icon `close`, accessibility name `닫기`, icon list). A search field (placeholder `초성 검색`) and tag chips `전체` + the project's tags, single-select.
- **Create row (0.2.0, U-15.3).** The first row is `＋ 새 인물`. When the query matches nothing it becomes `＋ '{query}' 새 인물` and the query fills the name. It opens the shared create card in its picker form (only `만들기`; spec 30 owns the card). The created character is picked at once. Hidden in read-only projects (U-15.10).
- Sections in this order:
  - `이 씬 등장`: characters who speak in, or are silent cast of, the target scene (from the chip) or the block's scene (from `속성` or the check center);
  - `최근` (up to 5, `[PROPOSED]`);
  - `전체`.
  - Row: 36 dp avatar initial, name (serif bold 16), and a subline of tags, or `표시 이름 ???` when the display name differs at this point.
- Hidden characters (locked) are not listed; the last row reads `숨긴 인물 {n} · 지문으로 보기`. A deleted current speaker gets the first row `지운 인물 · {name} · 되살리기` (only from `속성` or the check center). Both unchanged.
- It must stay fast with 100+ characters. The sheet motion is unchanged (SCR-67).

### SCR-20 · Speaker label, display name and `화자 없음` (0.2.0)
**Priority** P0 · **needsExternal** no · **Boards** `Main`, `Wide`, `CSheetProps`, `Check` · **Sources** planning 표시 이름 덮어쓰기; rule 11 §2; decisions N-43; DS ScriptBlock

- The label shows the character name. When the character's `표시 이름` in effect at this block differs from the name, the label is `{표시 이름}` (weight 600) followed by the real name in `ink3`: `??? 하르트`. With a number: `??? 하르트 · #043`.
- **(0.2.0) Per-block display name (N-43, SCR-78).** When the block has its own display name, the label is `{block display name} ({name})`, e.g. `경비병 (하르트)`. With a number: `경비병 (하르트) · #043`. It wins over the 표시 이름 history at that block. Closes Q-04.
- **(0.2.0)** Tapping the label only selects the block (SCR-14).
- **화자 없음.** A non-empty dialogue block without a speaker shows `화자 없음` in `warn` with `▲` on the right and produces `▲ 화자 없음` (SCR-62). With a block display name: `{block display name} (화자 없음)` `[DEFAULT]`.
- Deleted speaker: SCR-57. Hidden speaker: SCR-58. A locked hidden speaker shows `숨긴 인물` even when the block has its own display name `[DEFAULT]` (never leak through the override).

### SCR-78 · Per-block display name (`이 블록만 · 표시 이름`) (0.2.0)
**Priority** P1 · **needsExternal** no · **Boards** none (decision only) · **Sources** decisions N-43 (블록 하나만 다른 화자 이름); scope N-43, G-2, G-6, D24

- **Where.** The `속성` tab of a `대사` block has a row `이 블록만 · 표시 이름`, a text field. Empty means none. The value is saved when the field loses focus or its IME action runs (one undo step). It is trimmed, and blank is stored as null.
- **Manuscript and panels:** `{value} ({speaker name})` (SCR-20). 낭독 (SCR-28) shows `{value}`.
- **Game output (switch on only, SCR-63).**
  - JSON `displayName` = the value.
  - The voice ID is the original speaker's (`HRT_…`, unchanged by the override).
  - The CSV `화자` column stays the character name.
  - With the switch off, nothing about game output is shown (G-2). The manuscript label and 낭독 still use it (G-6).
- Changing the block's kind away from `대사` keeps the stored value, which is not drawn; it comes back if the block returns to `대사` `[DEFAULT]`.
- Not a character field: it never changes the character's 표시 이름 history (spec 30 owns that).

### 2.5 Block numbers and voice IDs

### SCR-21 · Permanent key and per-episode block number (`#043`) (0.2.0)
**Priority** P0 · **needsExternal** no · **Boards** `Wide`, `RSSelect`, `RSOrphan`, `UndoToast` · **Sources** rule 12 §5 (confirmed; replaces "블록마다 영구 ID(#043)"); planning 스크립트 편집기; 저장 파일 양식 §4

- **Key.** ULID created on the device when the block is created. Never shown, never changes, never reused. Used by record anchors, check `맞음` records, `아님` records, JSON `id`, CSV `블록 ID`, recorder links.
- **Number.** Unique within the episode. Issued from the episode's counter in creation order; the counter starts at 1 and only grows; numbers of deleted blocks are never reissued; numbers are never re-sorted automatically, so inserted blocks read out of order (`#042 리엔 · #058 하르트 · #044 나레이션`).
- Display `#` + 3 digits zero-padded (`#001`, `#043`). [PROPOSED] From 1000 on, unpadded (`#1043`).
- Changes only when the block moves to another episode (gets that episode's next number) or on explicit renumbering (SCR-22). Restored blocks keep their number.
- Choice option lines are `#{number}-{index}` (`#045-1`).
- **(0.2.0, U-14 #20)** In the manuscript the number shows only on these blocks: selected, `고치는 중`, the `이 뒤에` target, new blocks (`새로 · 나눔 · 합침`) and a block whose swipe row is open. It also shows in panels (`고른 블록 · #043`), select mode, toasts, check items and as the last part of the voice ID. It is never shown on other blocks in normal reading.
- **(0.2.0)** A block made by a send or a `나누기` takes the counter's next number, even when that reads out of order (`#044` then `#058`).

### SCR-22 · `번호 다시 매기기` (renumber an episode) (0.2.0)
**Priority** P1 · **needsExternal** no · **Boards** `RTRenumber` · **Sources** rule 12 §5 (다시 매기기), §6; `scr_new_time.py renumber`

- Entry: story-structure episode ⋯ → `번호 다시 매기기`, or the command `번호 다시 매기기 · 1-7` (SCR-68).
- Preview sheet (nothing changes before the button):
  - Title `번호 다시 매기기 · {episode}`; subtitle `원고 순서대로 #001부터`.
  - Three figures: `{N}` `블록` · `{k}` `바뀌는 번호` · `{N−k}` `그대로`.
  - Section `바뀌는 번호 · {k}`: one row per changed block `#{old} → #{new}` + `씬 {ordinal} · {speaker or kind}`; when that block's voice ID is fixed, a sub-row `보이스 {ID} · 고정 · 그대로`. After the first rows, the rest in one line `외 {m}개 · #055 → #038 · #052 → #046 고정 · …`.
  - Footer lines: `고정 보이스 ID {f}개는 그대로` and `번호만 다름 · ◇ 없음 · 변화 기록 · 블록 ID 그대로`.
  - Buttons `취소` · `다시 매기기` (primary).
  - **(0.2.0)** With the switch off, the `보이스 …` sub-rows, the `고정` markers in the `외 {m}개` line and the footer `고정 보이스 ID {f}개는 그대로` are not shown (SCR-79). Fixed IDs still stay unchanged.
- Apply: live blocks of the episode get numbers 1…N in manuscript order (scene order, then block order). Keys, records, `맞음`/`아님` records are untouched. Automatic voice IDs recompute; fixed IDs stay and raise no `◇` (the number part is never compared). One undo step.
- Trash entries and the counter after renumbering: Q-07.

### SCR-23 · Voice ID: `자동` → `고정`, `버린 ID` (0.2.0)
**Priority** P1 · **needsExternal** no · **Boards** `CSheetProps`, `WideProps`, `Check` · **Sources** rule 12 §6 (confirmed), §3–§4 tables; memory civilight-rules-blocks; 저장 파일 양식 §6 (voice state stored)

- Scope: dialogue blocks of the script only. Profile game-text voices are a later rule.
- **(0.2.0)** Choice options never get a voice ID (N-23, SCR-09); stored 0.1.0 option IDs are kept and not shown. Voice IDs (automatic, fixed, discarded), the `◇` voice checks and the format setting are shown only with the switch on (SCR-79). They are still computed and stored with the switch off: fixing happens only on a CSV or JSON export, which is hidden while the switch is off (scope §8).
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
| Split (`나누기`, SCR-72) | Front keeps; back gets a new automatic ID | Front keeps; back gets a new automatic ID |
| Merge (`합치기`, SCR-72) | Front keeps | Front keeps; later block's fixed ID → `버린 ID` |
| Copy · paste | New ID | — (copies never inherit a fixed ID) |
| Cut · paste | As move | As move |
| Speaker character deleted (rule 20) | Keeps computing with the deleted character's code | Unchanged |
| Speaker character merged into another (rule 20) | Recomputed with the target's code | Unchanged · `◇` (speaker part differs) |
| Block restored from trash | State as before deletion | State as before deletion |

- **◇ rule.** A fixed ID is flagged only when its **speaker part or episode part** differs from what the format would produce now. All such blocks gather in the check center group `보이스` as one item `형식과 다른 ID {n}` with buttons `새 ID로` · `그대로`. `새 ID로` gives the block a new ID and the old string becomes a `버린 ID` (rule 12 §6); [PROPOSED] the block returns to `자동` until its next export. `그대로` marks the item `맞음`.
- **버린 ID.** Stored with the block key and the date it was discarded; exported as CSV rows with 상태 `버림` (SCR-64). Automatic IDs never collide with discarded IDs because numbers are never reissued (see Q-07 for renumbering).
- **Manual format `직접`.** When the format is set to manual entry, the 속성 tab shows an editable ID field; duplicate IDs inside the project raise `▲` (SCR-62).

### SCR-24 · Voice ID format (project setting) (0.2.0)
**Priority** P1 · **needsExternal** no · **Boards** `ProjSettings` · **Sources** `scr_decide.py proj_settings`; rule 12 §6; planning 범위 (`형식 HRT_0107_043(인물 · 에피소드 · 블록)`)

- Row in 프로젝트 설정 › `스크립트 추가 칸`: `ID 형식` · `HRT_0107_043 · 인물 · 에피소드 · 블록 · 내보내면 고정` · `바꾸기 ›`. **(0.2.0)** The row is hidden while the switch is off (scope G-2).
- Default format = `{character voice code}_{episode code}_{block number}`; sample `HRT_0107_043` = 하르트 (`HRT`) · 1장 7번째 에피소드 (`0107`) · block `#043`. **(0.2.0)** Choice options no longer append `_{option index}` (N-23, SCR-09); 0.1.0 option IDs stay stored and unused.
- The character voice code comes from the cast area. How the episode code is built for other structures, and the editor for the format, are not specified (Q-06). A `직접` (manual) mode exists (rule 12 §6).
- Changing the format: automatic IDs follow at once; fixed IDs stay; the setting shows the count `고정 ID {n}개는 그대로` and a button `고정 ID도 새 형식으로` ([PROPOSED] which re-fixes them in the new format and records each old string as `버린 ID`, one undo step).

### SCR-25 · Optional script fields: `무대 위치`, `보이스 ID` (0.2.0)
**Priority** P1 · **needsExternal** no · **Boards** `ProjSettings`, `WideProps` · **Sources** concept memory SETTLED QUESTIONS; planning 범위; `scr_decide.py`

- **(0.2.0)** Both rows and their `속성` fields are hidden while the switch is off (scope G-2). Stored values are kept (scope §8).
- 프로젝트 설정 › `스크립트 추가 칸`:
  - `무대 위치` · `왼쪽 · 가운데 · 오른쪽` · toggle, default **off**.
  - `보이스 ID` · `대사마다 녹음용 ID` · toggle, default **on**.
  - `ID 형식` (SCR-24) and `전환 문구` (SCR-48).
- 무대 위치 on: the 속성 tab shows a segmented control `왼쪽 | 가운데 | 오른쪽` for dialogue blocks; [PROPOSED] no value until chosen; exported as `stage`.
- 보이스 ID off: the 속성 row and CSV/JSON voice values are hidden. [PROPOSED] Fixed IDs and discarded IDs stay in the data and come back when the toggle is turned on again.

### 2.6 Display layers and preview

### SCR-26 · `표시` sheet (display layers) (0.2.0)
**Priority** P1 · **needsExternal** no · **Boards** `Layers` · **Sources** planning 공통 개념 표시 레이어; `scr_script.py layers` (newest copy, includes `전환 문구`); rule 21 §2 (`용어 후보` toggle, default off); rule 22 §3 (씬 머리 늘 보임)

- **(0.2.0)** Opened only by the header `표시` icon button (icon `layers`, accessibility name `연출 · 메모 표시`, SCR-01). The bottom bar no longer has one.
- **(0.2.0)** The row `용어 후보` is removed. Candidate underlines follow the project setting `용어 후보` instead (WLD-73; scope S-5; SCR-56). The stored `layers.termCandidates` value is ignored and kept.
- **(0.2.0)** The `낭독` chip is enabled and opens the player (SCR-28).
- Sheet: title `표시`, right `닫기`; subline `보기에만 적용 · 파일 내용은 그대로`. Content is never removed from the file; layers only change what is drawn.

| Row | Description | Default | Hides |
| --- | --- | --- | --- |
| `연출 지시` | `배경 · BGM · 효과음 줄` | on | direction blocks |
| `작가 메모` | `⌁ 로 시작하는 메모` | on [PROPOSED; the `Layers` board draws it off, while `Main` shows memos] | memo blocks |
| `시스템 텍스트` | `[기록] 같은 화면 문구 · 전환 문구` | on | system-text blocks and the transition line under each scene head (the scene head itself always stays) |
| `변화 표식` | `◆ 변화 · ◇ 첫 공개` | on | `◆` `◇` glyphs in the manuscript, including dotted ones (panel rows stay) |
| `호칭 힌트` | `대사 위 호칭 표시` | off | SCR-29 |
| `괄호 지문` | `대사 속 (…) 표정 · 행동` | on | parenthetical runs |
| ~~`용어 후보`~~ | **(0.2.0) Removed.** Underlines follow the project setting `용어 후보` (WLD-73) | n/a | n/a |

- Section `미리 보기` with chips `쓰기` (default) · `대사만` · `낭독` (SCR-27, SCR-28).
- `▲` margin marks are never hidden by a layer [PROPOSED].
- [PROPOSED] Inserting a block of a hidden kind turns that layer on.
- Where layer choices are stored: Q-14 (they are view state, not file content; app settings are not written into `.civilight`).

### SCR-27 · Preview modes `쓰기` and `대사만`
**Priority** P1 · **needsExternal** no · **Boards** `Layers` · **Sources** planning 공통 개념 (`미리 보기: 쓰기 / 대사만 / 낭독`)

- `쓰기`: the normal editable manuscript.
- `대사만`: [PROPOSED] a read-only view of the same episode showing only dialogue blocks (speaker label + line) and scene heads; tapping a line returns to `쓰기` at that block.

### SCR-28 · `낭독` (line-by-line playback) (0.2.0)
**Priority** P1 · **needsExternal** no · **Boards** none (decision only) · **Sources** decisions N-27 (낭독의 뜻; 다음 줄로 넘기기; 읽기 보기); scope N-27, G-5, G-6, D18

- **Meaning (closes Q-08).** Full-screen playback of the script one step at a time. It does not use the device's speech synthesis (no TTS, needsExternal no). There is no separate reading view: U-14's tap-to-select removed the reason for one.
- **Entry (D18).** The `낭독` chip in the `표시` sheet's `미리 보기` section. It opens the player and does not stay selected: after closing, the chips read `쓰기` again.
  - Start step: the selected block if any, else the first step of the scene in view (SCR-71).
  - The chip is dimmed when the episode has no step.
- **Steps** `ReadAloud.steps(doc, episodeKey, gameTools): List<Step>` (pure, JVM), built from the live blocks of the episode in manuscript order:

| Block | Step |
| --- | --- |
| `대사` | `Line(speaker, runs)`. `speaker` is the block display name (SCR-78), else the display name in effect at the block (`???` respected), else the name. A deleted speaker uses the name; a locked hidden speaker reads `숨긴 인물`; none reads `화자 없음` |
| `나레이션`, `시스템 텍스트` | `Line(null, runs)` |
| `연출 지시` | `Caption("▸ " + render)`, where render follows SCR-08's table for the current switch. With the switch on, a `배경` cue whose asset has an image sets the background from this step on |

  - **Every step carries its resolved background**, `background: AssetKey?`: the last such `배경` image at or before it in the episode (null with the switch off). `이전`, `처음부터` and a mid-episode start therefore show the right background.
| `선택지` | `Choices(lines)`: every option line, in order, in one step |
| `작가 메모` | skipped |
| Empty blocks, blocks in a locked hidden scene, unknown kinds | skipped |

  - Paren runs show as `(text)` and link runs as their text.
  - Scene heads are not steps `[DEFAULT]`.
- **Screen.** Full screen on cover and inner; system bars stay.
  - **Background:** the current background image, filling the screen (switch on and an image exists), else solid `bg` (G-5).
  - **Dialogue box:** at the bottom, 16 dp side margin, `raise` background, radius 16, padding 16.
    - speaker line: Noto Sans KR 600 13/18 `ink2`, omitted for `Line(null, …)`;
    - text: serif 17/1.7 `ink`;
    - caption: mono 13/20 `ink3`;
    - choices: one row per line, `{index}` mono 12 `ink3` + text serif 16/26.
  - **Buttons** under the box, in this order: `이전` · `처음부터` · `닫기` · `다음` (text buttons, 44 dp tall; the icon list does not cover this screen, so text stays, D28).
- **Navigation.**
  - `다음` and a tap anywhere on the dialogue box go to the next step.
  - `이전` goes back one step.
  - `처음부터` goes to the episode's first step (D18).
  - `닫기` and system back return to the script with the current step's block selected and placed directly above the input bar (SCR-71).
  - On the first step `이전` is dimmed. On the last step `다음` is dimmed and a box tap does nothing.
- **Motion.** The step change is a text crossfade, `CivilightMotion` `standard` 200 ms; with reduced motion, a 120 ms fade.
- **Shown with the switch off** (G-6). Only the asset background needs the switch on.
- **Superseded (0.1.0):** the disabled `[PROPOSED]` chip.

### SCR-29 · `호칭 힌트` (address-form hint)
**Priority** P1 · **needsExternal** no · **Boards** `Layers` · **Sources** planning 인물 말투 (`상대별 호칭`, `호칭 힌트는 스크립트에서 켜고 끄는 옵션`); scope table (v1 포함); rule 13 (호칭 is a per-side field of the relation with `◆` history)

- Layer toggle `호칭 힌트` · `대사 위 호칭 표시`, default off.
- When on, a small line above a dialogue block shows how the speaker addresses others, read from the speaker's side of each relation at that block (rule 13 §4), e.g. `리엔을 ‘꼬마’` above a 하르트 line [PROPOSED format, mirrors the 참고 tab row].
- Which addressees are shown is not specified (Q-09). [PROPOSED] the other characters of the scene who have a 호칭 value.

### 2.7 Side panel and cover sheets

### SCR-30 · Swipe buttons: left `◆ 시간 · 참고 · 속성`, right `고치기 · 이 뒤에 · 종류 · 씬 나누기 · 지우기` (0.2.0)
**Priority** P0 · **needsExternal** no · **Boards** `U14SwipeLeft`, `U14SwipeRight`, `U14Delete`, `MSwipe`, `Tour2`, `Walk1` · **Sources** U-14 #3, #5, #15, #18; feedback-no-guessing; DS SwipeTabs; icon list (both swipe rows class b)

- **Both directions only reveal buttons.** Nothing runs until a button is tapped, even when a row is swiped to the end (U-14.3). Content is never shown inside the swipe.
- **Left swipe** (unchanged buttons, SCR-31): the block slides 192 dp left and reveals three 64 dp buttons on `raise`:
  - `◆ 시간`: the `◆` glyph, `acc` when the block carries a `◆`;
  - `참고`: icon `note_ref` 18;
  - `속성`: icon `props` 18.
  - Each is icon over label, class b, accessibility name = label. Tapping one selects the block and opens the panel or sheet at that tab.
- **Right swipe (0.2.0):** the block slides 300 dp right and reveals five 60 dp buttons on `raise`, each icon 18 over its label (class b, accessibility name = label). On cover 90 dp of the block stays visible.

| Button | Icon | Action |
| --- | --- | --- |
| `고치기` | `edit` | SCR-72 |
| `이 뒤에` | `insert_after` | SCR-73 |
| `종류` | `kind` | Kind menu, converts at once (SCR-12, SCR-13) |
| `씬 나누기` | `scene_cut` | SCR-49 routing for this block |
| `지우기` | `trash` | Deletes this block at once (SCR-38) |

- **Open and close.**
  - Released past half of the reveal width (96 or 150 dp), the row stays open; otherwise it returns. This closes Q-02 (`MSwipe` `놓으면 제자리` applies to short swipes).
  - One row is open at a time. Opening another row, tapping another block, or starting a scroll closes it.
  - Tapping a button runs it and closes the row.
  - The open row's block shows its number (SCR-21).
- **Where.**
  - Every block of a known kind, both directions. Not scene heads, hidden-scene rows or unknown-kind blocks (unknown kinds get the left swipe only).
  - The right swipe is unavailable during `고치기` (SCR-72) and in read-only projects. The left swipe opens there for reading (U-14 #15).
  - The swipe works with the keyboard up.
- **Motion:** slide 260 ms; buttons appear 60 ms apart (both via `CivilightMotion`, SCR-67).
- **Superseded (0.1.0):** "right swipe does nothing", "off for the block being edited", and the tap that opens the row (Q-02). A tap now selects only (SCR-14).

### SCR-31 · Panel / sheet container and selection following
**Priority** P1 · **needsExternal** no · **Boards** `CSheetTime`, `CSheetRef`, `CSheetProps`, `CSheetMods`, `Wide`, `WideRef`, `WideProps`, `WideMods` · **Sources** concept memory (side panel tabs 시간 · 참고 · 속성 · 모듈); DS Sheet, Tabs; planning 옆 패널

- Cover: bottom sheet whose top sits at y = 400 on an 844-high screen; top radius 28; grab handle 36×4; tabs row `시간 · 참고 · 속성 · 모듈` (text tabs, active tab underlined 2 px `acc`) and `닫기` on the right; shade over the manuscript; the selected block stays visible above the sheet.
- Inner: the 290 px side panel (SCR-03) with the same tabs and `닫기`.
- The content always describes the **selected block** (and its scene). Selecting another block updates the content in place on the same tab.
- [PROPOSED] Last tab and per-tab scroll position are remembered per project; folding/unfolding keeps tab and scroll (SCR-66).
- [PROPOSED] No block selected: tabs `시간 · 모듈` show scene content of **(0.2.0)** the scene in view (SCR-71); `참고 · 속성` show `고른 블록 없음`.

### SCR-32 · `시간` tab
**Priority** P1 · **needsExternal** no · **Boards** `CSheetTime`, `Wide`, `RSOrphan` · **Sources** `scr_core.py p_time` (newest copy); gen_nm `Wide`; rule 22 §7 (transition state word); rule 11 §3

1. Section `고른 블록 · #{n}` (inner: `고른 블록 · #043 하르트`). One card per record anchored on this block:
   - `◆` card: title (`acc`, 14/600) `◆ {kind} · {subject}` e.g. `◆ 관계 · 리엔의 마음 → 하르트`; subline `{before} → {after} · {episode} #{n}부터` e.g. `만나기 전 → 경계 · 1-7 #043부터`. Inner adds buttons `고치기` (opens the record's editor in the cast area) · `관계도에서 보기` (relation records only; opens the cast relation map).
   - Orphaned record card: dotted `◆`, subline `근거 #{old} 지워짐 · #{n}로 옮겨 옴`, buttons `다른 블록에 잇기 · 이대로 두기 · 기록 지우기` (SCR-43).
   - `◇` card: `◇ 첫 공개 · {term}` + `이 블록 · {episode} #{n}`.
   - No record: [PROPOSED] `변화 없음`.
2. Section `이 씬 · 장소와 때`, right side `{전환 문구 상태} · 바꾸기 ›`, where the state reads `전환 문구 없음`, `전환 문구 자동`, or `전환 문구 직접` [PROPOSED wording for the latter two; board shows `전환 문구 없음`]. `바꾸기 ›` opens the place & time area's `씬 · 장소와 때` sheet. Below: `{episode} · {N일째} · {band}` (serif bold 15) + ` · {place}` (small), e.g. `1-7 · 2일째 · 새벽 · 결사의 문`; line `{base text} · {precision} · 이야기 약 {d}일째`, e.g. `직전 씬 +1일 · 정확 · 이야기 약 13일째`; if the travel check has a warning for this scene, a `warn` line `▲ {character} 이동 시간 · 점검 센터` (tap → check center, travel filter).
3. Section `이 씬 안`: in manuscript order, background cues and blocks with their record state: `05:40 배경 · 하층 거리` (scene clock or band + first background cue), `#042 · 변화 없음`, `◆ #043 · 관계`, `◇ #044 · 용어 첫 공개` (inner: `◇ #044 용어 첫 공개 · 오리지늄 등불`). The selected block's row is highlighted. Point-seat records appear here as a line `◆ 1-5부터 · 근거 블록 없음` + `블록에 잇기` above the row of the unit's first block (SCR-42). [PROPOSED] Tapping a row selects that block and scrolls the manuscript to it.

### SCR-33 · `참고` tab (0.2.0)
**Priority** P1 · **needsExternal** no · **Boards** `CSheetRef`, `WideRef`, `MFoldScript` · **Sources** `scr_core.py p_ref`; rule 11 §2–§3 (block-level, `여기서` row); rule 13 §3 (viewed from the speaker's side; `만나기 전`)

- **(0.2.0) Term candidates first (scope S-5, decision 2026-11-24 용어 후보 표시; WLD-73 item 3).** When the setting `용어 후보` is on and the selected block (any kind SCR-56 scans) has candidates, the tab starts with one row per candidate occurrence, in text order (`scr_core.cand_row`):
  - The row is a `tint` card, radius 14, min height 48, with `용어 후보 ·` (small `ink3`), then the matched text (serif 600 16/24 drawn with the candidate underline), then the buttons `잇기` (600 `ink`) · `아님` (500 `ink3`), 44 dp targets, class c.
  - Example: `용어 후보 · 등불 · 잇기 · 아님`.
  - `잇기` and `아님` work as SCR-56's 0.1.0 popover buttons did.
  - When linking would make a pre-reveal use (a first reveal set by hand that lies after this block, D12), a second line under the row reads `첫 공개 {episode} · 잇기 시` + `▲ 공개 전 사용`.
- Header `화자 · {speaker} · 이 블록 기준` (e.g. `화자 · 하르트 · 이 블록 기준`).
- Rows: `말투`, one `{other}을/를` row per address term (e.g. `리엔을 · ‘꼬마’`), `표시 이름` (`??? (1-9 공개)`). **(0.2.0, N-43)** `말투` shows only the character's `한 줄 요약` field (e.g. `반말 · 짧게 끊음`), never the prose below it (spec 30 owns the field). With no summary the row is left out `[DEFAULT]`.
- `최근 대사`: the speaker's previous dialogue lines before this block, newest first, each `“{line}”` + location `1-7 씬 2` ([PROPOSED] 2 lines).
- `관계 · 지금`: one row per counterpart, from the speaker's side: `리엔 · → 보호 · ← 경계` (`→` = speaker's 마음, `←` = the other's 마음), `세라 · 사이 동료` (사이 only).
- **Block-level time rule.** Every value is the state just **before** the selected block; a change made by the selected block itself is shown as its own row `{before} → {after} ◆ 여기서`. Example (rule 11 §2 with rule 13 wording): selecting `#042` → `만나기 전`; `#043` → `만나기 전 → 경계 ◆ 여기서`; `#044` → `경계`.
- Hidden counterparts collapse into `숨긴 인물과의 관계 {n}`; deleted counterparts appear as one dotted row at the end (cast area rules 19/20).
- Non-dialogue block: Q-15.
- *Source note.* Rule tabs 11 and 12 say `모르는 사이`; rule 13 (confirmed later) replaced it with `만나기 전` (not a relation kind). The board `CSheetRef` shows the end state (`← 경계`) for `#043`; the rule-11 behaviour above wins.

### SCR-34 · `속성` tab (0.2.0)
**Priority** P1 · **needsExternal** no · **Boards** `CSheetProps`, `WideProps` · **Sources** `scr_core.py p_props`, `scr_script.py wideprops`; planning 옆 패널 속성; rule 12 §6; rule 20 §2

- **(0.2.0)** Read-only projects show the tab for reading: no `바꾸기`, no editable field.
- Inner shows the section `고른 블록 · #{n}`. Fields (label 12 `ink3` above a 44–46 px field box with `edge` outline):
  - **(0.2.0)** `종류`: current kind name (SCR-12 names) + `바꾸기`, which opens the kind menu and converts at once (SCR-13; board `U14Wide`).
  - `화자`: avatar + name + `바꾸기` (→ SCR-19). Deleted speaker: dotted `지운 인물 · {name}` + buttons `화자 바꾸기 · 되살리기 · 이대로 두기` (SCR-57).
  - `표시 이름`: resolved display name + right note, e.g. `???` · `1-9에서 공개` (inner board: `1-9에서 이름 공개`). [PROPOSED] Tap opens the character's 표시 이름 history in the cast area.
  - **(0.2.0)** `이 블록만 · 표시 이름` (dialogue only): SCR-78.
  - `무대 위치` (only if enabled, SCR-25, and **(0.2.0)** only with the switch on): `왼쪽 | 가운데 | 오른쪽`.
  - `보이스 ID` (only if enabled, and **(0.2.0)** only with the switch on): mono ID + state `자동` / `고정 · 11월 20일 내보냄`; manual format → editable.
  - `변화 연결`: `◆ 관계 변화 · 리엔 → 하르트` (`acc`), one line per record on this block; [PROPOSED] tap → `시간` tab.
  - **(0.2.0)** `복선 심기` and `다시 언급 · 거두기` rows, and `할 일` on memo blocks: SCR-77.
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

### SCR-36 · Block select mode (`블록 고르기`) (0.2.0)
**Priority** P1 · **needsExternal** no · **Boards** `RSSelect` · **Sources** rule 12 §3 (옮기는 방법), §8 (화면 not drawn then); `scr_new_scene.py rs_select`

- Entry is not drawn; Q-17. [PROPOSED] the command `블록 고르기` (SCR-68). No long-press or other hidden gesture.
- Header: position line, title `블록 고르기`, right `모두` (selects every block of the current scene [PROPOSED scope]) and primary `완료` (leave the mode).
- Each block becomes a row: left 52×44 checkbox (rounded square, check mark when on; `aria-label="고르기"`), body = label line `{label} #{n}` + markers (label: speaker name, `연출`, `나레이션`, `선택지`, `시스템`, `작가 메모`) and the block text, right a grip handle (`aria-label="끌어서 옮기기"`). Checked rows have `sel` background. Scene heads stay visible as dividers and cannot be checked [PROPOSED].
- Bottom glass bar (124 px): `고른 블록 {n}개` and right `#{first} ~ #{last} · ◆ {a} · ◇ {b} 같이 감` (first/last in manuscript order; counts of records riding along), then four buttons `옮기기 · 지우기 · 잘라내기 · 복사`.
- **Superseded (0.1.0):** `/씬` while blocks are checked. Select mode has no scene split in 0.2.0 (SCR-49).

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

### SCR-38 · Delete blocks (0.2.0)
**Priority** P0 · **needsExternal** no · **Boards** `UndoToast`, `RSSelect` · **Sources** rule 12 §2, §4, §7 (confirmed); vocabulary (`지우기` = trash; toast `블록 #043 지움`)

- **(0.2.0)** Ways to delete: the right-swipe button `지우기` (SCR-30; closes Q-33) and select mode `지우기`. **Superseded (0.1.0):** Backspace inside an empty block, and erasing a block's content then Backspace. An emptied `고치기` cannot be sent (SCR-70).
- Never asks for confirmation.
- Non-empty block → trash for 30 days (SCR-39). Empty block → removed with no toast and no trash entry.
- Records anchored on the block are relocated, never deleted (SCR-43). Fixed voice ID → `버린 ID`.
- **(0.2.0)** After a swipe `지우기`, records relocated to the next block show their dotted mark there (board `U14Delete`: `블록 #044 지움 · ◆ 1개는 #045 자리로`). If the deleted block was the `이 뒤에` target, the mark clears (SCR-73).
- Toasts (ink surface, `bg` text, one action `되돌리기` with undo icon, shown above the input bar):
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

- `복사`: puts the checked blocks on the app clipboard. **(0.2.0)** Pasting blocks (select mode, the `[PROPOSED]` paste of 0.1.0) inserts them after the selected block, else at the end of the scene in view (SCR-71) `[DEFAULT]`; plain text pasted into the input field goes into the draft only. Pasting inserts copies: new keys, the destination episode's next numbers, automatic voice IDs; records are **not** copied; toast `◆는 복사되지 않음` when any copied block had records.
- `잘라내기`: treated as a move (keys kept, records ride along, numbers/voice per SCR-37 when pasted). Behaviour when cut blocks are never pasted: Q-19.
- Pasting plain text from outside the app: Q-19. A pasted line such as `— 씬 2 · 기록실 —` becomes a narration block and never splits the scene (rule 22 §4).

### 2.9 Change records `◆` and first reveals `◇` on blocks (rule 12)

### SCR-41 · `◆` change-record sheet from a block (`변화 기록`) (0.2.0)
**Priority** P1 · **needsExternal** no · **Boards** `ChangeCreate` · **Sources** `scr_core.py change_create`; rule 12 §1 (seat "블록"); planning 공통 개념 변화 기록; rule 13 (사이 · 마음 · 호칭)

- **(0.2.0)** Opened from the tool row `◆` (SCR-16) for its target block: the `고치기` block, else the selected block. [PROPOSED] Also from a `◆ 변화 기록` row at the end of the `시간` tab's block section.
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

### SCR-44 · Consequences of block operations (records · number · voice ID) (0.2.0)
**Priority** P1 · **needsExternal** no · **Sources** rule 12 §3, §4, §6 (confirmed)

Single reference table; the features above implement it.

| Operation | `◆` / `◇` records | Block number | Voice ID |
| --- | --- | --- | --- |
| Edit text inside the block | Unchanged | Unchanged | Unchanged |
| Change speaker | Unchanged | Unchanged | 자동: new · 고정: `◇ 화자와 다름` |
| Change kind (dialogue → narration) | Unchanged (records attach to content, not kind) | Unchanged | 자동: gone · 고정: `버린 ID` |
| Split (`나누기`, SCR-72) | Stay on the front block | Back block: next number | Front keeps; back: new 자동 |
| Merge (`합치기`, SCR-72; same kind and speaker only) | Later block's records → merged block, not flagged; toast part `◆ 1개 #042로` | Front number | Front keeps; later 고정 → `버린 ID` |
| `고치기` send (SCR-72) | Unchanged | Unchanged | As speaker change or kind change when those changed |
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

### SCR-46 · `씬 머리` (scene head) (0.2.0)
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
- **(0.2.0)** `합치기` never crosses a head (SCR-15, SCR-72). A sent line that looks like a head is an ordinary block of the kind chip's kind and never makes a scene.

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

### SCR-49 · Scene routing (`씬 나누기` from a block) and `다음 씬 만들기` (0.2.0)
**Priority** P1 · **needsExternal** no · **Boards** `U14SwipeRight`, `Blocks` (row `새 씬`, 0.1.0) · **Sources** rule 22 §4; U-14 #18

**(0.2.0) Entry points.**
- The right-swipe button `씬 나누기` on block X (SCR-30). Routing by X's position is below.
- The input bar's `다음 씬` (SCR-74), which creates at once without a sheet.
- The scene-head ⋯ › `앞에 새 씬` (SCR-53).
- **Superseded (0.1.0):**
  - `/씬` and the add sheet's `새 씬` row;
  - routing by caret;
  - `/씬` in select mode. Select mode has no scene split in 0.2.0.

The swipe `씬 나누기` never changes anything by itself; it opens a sheet:

| X | Sheet |
| --- | --- |
| Not the last live block of its scene | `씬 나누기` (SCR-50), split below X, sub `{episode} 씬 {n} · #{X} 아래` |
| The last live block of its scene | `다음 씬 만들기` after X's scene |
| Read-only project | Not reachable (no right swipe) |

`다음 씬 만들기` sheet (unchanged from 0.1.0):
- title `다음 씬 만들기`, sub `{episode} · 씬 {n} 뒤`, `닫기`;
- fields: `제목` (`미정` · `쓰기 ›`), `장소` (the previous scene's place, `· 씬 {n}과 같음`, `비우기` · `바꾸기 ›`), `때` (`씬 {n} 기준 · 같은 때`, `비우기` · `바꾸기 ›`), `전환 문구` (project default chips);
- primary `만들기` `[PROPOSED]`.

It runs `SceneActions` like story structure's `새 씬`. Afterwards the new scene's head sits directly above the input bar, so it is the target scene (SCR-71).
- **Superseded (0.1.0):** the empty dialogue block with a caret after creation.

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

### SCR-53 · `앞에 새 씬` (new scene before this one) (0.2.0)
**Priority** P1 · **needsExternal** no · **Sources** rule 22 §4 (case table); rule 11 §4 (inserted scene starts without time)

- From head ⋯ › `앞에 새 씬` (**(0.2.0)** `/씬` is gone): opens a sheet that creates an empty scene directly before this scene. [PROPOSED] Same fields as `다음 씬 만들기` but nothing prefilled (`장소 없음`, `때 없음`, rule 11 §4), primary `만들기`.
- Other scenes' absolute times do not change; the next scene's ordinal increases.

### SCR-54 · Scene-move preview (`씬 옮기기`) (0.2.0)
**Priority** P1 · **needsExternal** no · **Boards** `RSSceneMove` · **Sources** rule 12 §3 (씬 옮기기 미리 보기, confirmed); `scr_new_scene.py rs_scene_move`

- Opened by story structure when the user moves a scene (the drag/target UI is the story-structure area's). Nothing changes before `옮기기`.
- Sheet: title `씬 옮기기`, sub `미리 보기 · 옮기기 전에는 그대로`; a card `{episode} · 씬 {n}` / `{title}` › `{destination episode}` / `{position} · 씬 {m}` (e.g. `1-7 · 씬 3 · 꺼진 등불` › `1-8 · 맨 앞 · 씬 1`).
- `바뀌는 것` rows:
  - `기록` · `◆ {a} · ◇ {b} · 시점 {from} → {to}` (e.g. `◆ 2 · ◇ 1 · 시점 1-7 → 1-8`)
  - `블록 번호` · `블록 {k}개 · {to} 새 번호` (planning text adds the first new number: `새 번호 1-8 #061~`); same episode → `그대로`
  - `보이스 ID` · `고정 {f}개 그대로 · ◇ 에피소드 부분 다름`; same episode → `그대로`. **(0.2.0)** Shown only with the switch on (scope G-2, SCR-79)
  - `때` · `그대로 · ◇ {o} 순서와 어긋남` (o = scenes whose time order no longer matches manuscript order)
- Buttons `취소` · `옮기기`. Apply per SCR-44; the scene keeps its key and its time.

### SCR-74 · `다음 씬` in the input bar (0.2.0)
**Priority** P0 · **needsExternal** no · **Boards** `U14Input`, `U14SceneNext` · **Sources** U-14 #18; icon list (`다음 씬`: class b, icon `next_scene`, accessibility name `다음 씬 만들기`); scope U-14.18

- **Where:** right of `씬 {n} 끝에` in the place area (SCR-70), and right of `씬 없음` in an empty episode.
  - It is drawn as icon `next_scene` 16 + text `다음 씬`, `acc` 600 12/16, with a 44 dp target and accessibility name `다음 씬 만들기`.
  - It is not shown while an `이 뒤에` mark or `고치기` is active.
- **Action**, one undo step: creates a new scene directly **after the target scene** (SCR-71) at once, through `domain:actions` `SceneActions`, with no sheet `[DEFAULT]`.
  - U-14 #18 "누르면 다음 씬이 생기고 `씬 4 끝에`로 바뀐다" describes an immediate create.
  - The new scene takes the `다음 씬 만들기` defaults (SCR-49) with nothing changed: title none, the target scene's place, time `씬 {n} 기준 · 같은 때` (or none when the target scene has none), and the project's default transition mode.
  - Its head shows these values, and tapping the head changes them (SCR-46).
  - If the target scene is not the last of the episode, the new scene goes between it and the next one; following ordinals increase by one.
  - In an empty episode it creates 씬 1 with no place and no time.
- **After:** the manuscript scrolls so the new head sits directly above the input bar. The target becomes the new scene (`씬 {n+1} 끝에`) and flashes (SCR-71). The draft and chips are kept.
- **Repeated taps:** `다음 씬` is dimmed while the target scene has no live block `[DEFAULT]`, so a double tap cannot leave empty scenes behind (accessibility name `다음 씬 만들기 · 꺼짐`). An empty episode is the exception: there `다음 씬` makes its first scene.
- Hidden flags, flashback flags and silent cast are not copied to the new scene.

### 2.11 Other areas' states inside the script

### SCR-55 · `@` links in block text (0.2.0)
**Priority** P1 · **needsExternal** no · **Boards** `Main` (linked `오리지늄 등불`), `EditKeyboard` (`@`) · **Sources** planning 공통 개념 @ 링크; rule 21 §2 (alias insertion, particles outside), §8 (@ picker finds aliases); rule 19 §6; rule 20 §2; 저장 파일 양식 §4

- Typing `@` in the input field, or the tool row `@` (SCR-16), opens the link picker (people, terms, places; search by name, 초성 and aliases). An alias result reads `오리지늄 · 오리지늄 등불의 별칭`; choosing it inserts the alias text into the draft as a link run.
- **(0.2.0)** The picker's first row is the U-15 create row for the kind in view (`＋ 새 인물` and so on; spec 30 and spec 40 own the cards). It is hidden in read-only projects.
- A link is a run holding the target key and the visible text; particles stay outside (`[오리지늄]은`).
- Render: link text in `ink` with a 1 px `ink3` underline. **(0.2.0)** Tapping a link only selects its block (SCR-14). **Superseded (0.1.0):** the `[PROPOSED]` tap that opened the target's card (Q-38).
- Locked hidden target: the link text is drawn as `숨긴 인물` / `숨긴 항목` and term emphasis is removed; unlinked plain text is never masked. The picker omits locked hidden items and ends with `숨긴 인물 {n} · 지문으로 보기`.
- Deleted target: dotted underline + `◇`; tapping shows the card `지운 인물 · 노아` (or `지운 용어 · …`, `지운 장소 · …`) with `다른 항목에 잇기 · 글로 풀기 · 되살리기 · 이대로 두기` (`글로 풀기` keeps the text and removes the link). After permanent deletion the chip reads `영구 삭제 · 노아` and `되살리기` is gone.
- Links in memos and direction cues are listed by the term card under `원고 밖 · 메모` and never create `▲`.

### SCR-56 · Term candidates and pre-reveal `▲` in the manuscript (0.2.0)
**Priority** P1 · **needsExternal** no · **Boards** `U14Input` (#044 `등불`), `RTCandidate` (0.1.0), `Check` · **Sources** rule 21 §2–§5; decision 2026-11-24 (용어 후보 표시); scope S-5, D12, D13; board note `CAND_ST`

- Candidate detection (names and aliases, word start, particle list, `끊김`, longest match, a re-scan ~0.5 s after a block changes) is done by the world area (`TermCandidates`); this feature renders the results.
- Only game-output text is scanned: dialogue, narration, system text, choice options. Not memos, direction text or cues, parentheticals, display names or scene titles.
- **(0.2.0) Underline.** When the project setting `용어 후보` is on (WLD-73, default on; scope S-5), every candidate range in the manuscript gets a faint dotted underline: dotted, 1.5 dp thick, offset 6 dp, `ink3` at 75 % opacity, ink skipping off.
  - The text keeps its line's own colour.
  - It must look lighter than the "attached to something deleted" dotted underline (DS-18) and different from a link (`ink` text, 1 dp solid `ink3`, DS-19) (D13).
  - With the check off there is no underline and no `참고` row.
  - **Superseded (0.1.0):** the `용어 후보` display layer (default off) and the header chip `표시 · 용어 후보 · 켬`.
- **(0.2.0) Not tappable.** A tap on an underline selects the block (SCR-14). `잇기` and `아님` live in the `참고` tab, reached with a left swipe (SCR-33). They work as before:
  - `잇기` wraps exactly that text range into a link;
  - `아님` stores a dismissal (block key + term key + matched text + occurrence index in the block).
  - **Superseded (0.1.0):** the popover on tap.
- **First reveal (S-5, D12).** A term's first reveal is set automatically to the first block in manuscript order that links the term with `@`; a first reveal set by hand in 0.1.0 wins (world area). `잇기` can therefore move an automatic first reveal earlier instead of raising `▲`.
- A link before its term's first reveal (manuscript order) shows the link look plus an amber `▲` in the line's margin, always visible regardless of layers. **(0.2.0)** A tap on it selects the block; the item is in the check center.
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

### SCR-59 · Memo blocks inserted from the Life Recorder (rule 23) (0.2.0)
**Priority** P1 · **needsExternal** no · **Sources** rule 23 §4 (confirmed); vocabulary memory (`넣기`)

- The recorder's `원고에 넣기` creates exactly one `⌁` author-memo block at the chosen position with a copy of the record's text; it never guesses another block kind. The block gets the episode's next number (inserted after `#044` it may be `#062`). Toast: `1-7 씬 3 · #062 작가 메모 넣음 · 되돌리기 · 가 보기` (`가 보기` opens the script at that block).
- The block stores its source (record key + record time). Its memo prefix reads `⌁ 작가 메모 · 레코더 11월 22일 08:41`; tapping the source opens the record in the recorder (audio plays there; audio is never copied into the manuscript).
- Editing the block never changes the record and vice versa.
- Changing it to dialogue/narration is ordinary kind change (SCR-13).
- Deleting it follows SCR-38/39; it is not relocated like `◆` records.
- **Last writing position (0.2.0).** The script persists, per project, `lastWritingPosition` = the block most recently made or changed by a send, a `고치기` send, `나누기` (the back block) or `합치기` (the merged block), with offset = end of its text. There is no caret in blocks any more. The recorder's location sheet shows it as `마지막으로 쓰던 곳 · 1-7 씬 3 · #044 뒤`.
- **(0.2.0)** The recorder opens only from home (scope S-8, U-14 #14), so a recording is never started from the script. The 0.1.0 `여기 넣기 · #044 뒤` path for a recording started in the script is gone (spec 70 owns the recorder side).

### SCR-60 · Read-only mode (example project) (0.2.0)
**Priority** P1 · **needsExternal** no · **Boards** `U14ReadOnly`, `OnbExampleScript`, `Tour1`, `Tour2`, `Tour4` · **Sources** concept memory ONBOARDING; U-14 #15; board note (`읽기 전용 is said once`); scope U-14.15, U-15.10

- The example project 「잿빛 새벽의 등불」 opened during onboarding is read-only.
- **(0.2.0) Header line:** `읽기 전용 · 편집 불가` replaces the position line (board `U14ReadOnly`). The chip `이 씬 · 1-7`, `찾기` and `표시` stay.
- **(0.2.0) Bottom.** The input bar is replaced by one row in a 96 dp glass bar, 12 dp side insets, 16 dp above the navigation inset:
  - left: a lock icon 16 (`acc`) + `예시 프로젝트` (Noto Sans KR 600 13/18 `ink`, one line);
  - right: `예시 홈으로` (ghost button) and `온보딩 끝내기` (primary). The onboarding area owns both actions.
  - Read-only is said once, in the header line, as the board note says. Scope U-14.15 writes the row as "`읽기 전용` and `예시 홈으로 · 온보딩 끝내기`"; the board's wording is used (Q-41).
  - **Superseded (0.1.0):** the banner `예시 프로젝트 · 읽기 전용` · `온보딩 후 삭제`.
- No input bar, no tool row, no `고치기 · 이 뒤에 · 종류 · 씬 나누기 · 지우기` (the right swipe does not open), no `다음 씬`, and no create rows (U-15.10).
- The left swipe opens `시간 · 참고 · 속성` for reading (U-14 #15): no `바꾸기`, no editable field, no `잇기 · 아님`, no record buttons.
- `낭독` (SCR-28) works.
- *Source note.* `Tour4` (`시점 렌즈`) teaches a lens chip on the script. The onboarding area must explain the position label instead, or drop the step (S-9).

### SCR-61 · Asset list (`에셋`) and asset picker (0.2.0)
**Priority** P1 · **needsExternal** no · **Boards** `Assets` · **Sources** planning 에셋; `scr_script.py assets`; rule 22 §2 (background ↔ place link)

- Screen: header small line `잿빛 새벽의 등불`, title `에셋`, right `+`. Filter chips `전체 · 배경 · BGM · 효과음 · CG · 안 쓰는 것`. Sections with counts `배경 · 4`, `BGM · 3`, `효과음 · 12`, `CG · 1`.
- Row: 56×40 thumbnail (image for 배경/CG; waveform icon for BGM/효과음), name (serif bold 16), subline `{file name} · {uses}` (`bg_lower_dawn · 이 에피소드 3회`, `bg_guild_gate · 6회`), audio subline `{file name} · {length}[ · 반복]` (`bgm_embers · 2분 41초 · 반복`, `se_lamp_flicker · 0.8초`), right the first episode that uses it in mono (`1-7`). A CG with no image: subline `cg_lamp_out · 아직 이미지 없음`, right `비어 있음` (`warn`).
- `안 쓰는 것` = assets no direction cue references.
- Command-field hint on this screen: `에셋 이름이나 파일 이름으로 찾기`.
- The same list, filtered to one kind, is the picker used when adding a direction cue (SCR-08). **(0.2.0)** The list, its command and the picker exist only with the switch on (scope G-2). The picker's first row is the U-15 create row `TODO_COPY_SCR_ASSET_CREATE` (suggestion `＋ 새 에셋`, the U-15 pattern). It opens the shared create card (`core:designsystem`, U-15) with this spec's field set `종류 · 이름 · 파일` (U-15.9). Only `이름` is required. In the picker only `만들기` shows, and the new asset is picked at once (U-15.6).
- Not specified: entry point, the `+` add/edit form, whether files are stored, deleting a used asset (Q-29). [PROPOSED] The form has kind, name, file name, optional image (배경 · CG), length and `반복` (BGM · 효과음), and a place link (배경).

### SCR-79 · `게임 제작 도구` in the script (0.2.0)
**Priority** P0 · **needsExternal** no · **Sources** decisions 2026-11-23 (방향: 게임 제작 기능); scope G-1 … G-7, §8

The switch is owned by project settings (G-1). In the script:

| Surface | Switch off | Switch on |
| --- | --- | --- |
| Voice ID rows in `속성`, voice-ID format and `보이스 ID` setting rows | hidden | as 0.1.0 |
| `무대 위치` setting row and `속성` field | hidden | as 0.1.0 |
| Cue chips in the input bar, the cue-summary part of a block with text, the asset list, asset picker and `에셋` command | hidden; the text line shows (SCR-08) | shown |
| Voice checks (`◇ 보이스 ID가 화자 · 에피소드와 다름`, `▲ 보이스 ID 겹침`) | not emitted to the check center | emitted |
| Game-output side of a block display name (SCR-78) | not mentioned anywhere | JSON `displayName` |
| JSON and CSV export | not offered (G-7: PDF and `.civilight` only) | offered |
| `낭독` background asset | solid background | asset image |
| `번호 다시 매기기` sheet (SCR-22): `보이스 … 고정 · 그대로` sub-rows and the footer `고정 보이스 ID {f}개는 그대로` | hidden | shown |
| `씬 옮기기` preview (SCR-54): the `보이스 ID` row | hidden | shown |

- **Always shown, either way (G-6):** `연출` and `선택지` as text blocks, 낭독, and the block display name in the manuscript.
- **Never deleted (scope §8).** Switching off and on loses nothing. Stored cues, assets, voice states, discarded IDs and stage positions are untouched by every 0.2.0 edit path while hidden (SCR-08, SCR-72).
- Switching while the script is open redraws at once. Draft cue chips are kept and hidden while off.

### 2.12 Checks

### SCR-62 · Check items produced from script data (0.2.0)
**Priority** P1 · **needsExternal** no · **Boards** `Check` · **Sources** planning 점검 센터 table; rule 12 §2, §6; rule 22 §2, §7; rule 19 §5; `scr_tools.py check`

The check center (other area) lists, filters and stores `맞음`; this area computes these items. Every item references blocks/scenes by key.

| Item (copy) | Mark | When | Filter | Buttons |
| --- | --- | --- | --- | --- |
| `▲ 화자 없음` · the line (e.g. `“…누구야?”`) · `대사 블록 {n}개` · location `1-7 씬 2` | `▲` | A non-empty dialogue block has no speaker | `화자` | `화자 고르기` · `나레이션으로 바꾸기` |
| `◇ 근거 지워진 변화 {n}` | `◇` | Records flagged `근거 지워짐` (SCR-43) | `변화` | `다른 블록에 잇기` · `이대로 두기` · `기록 지우기`; `모두 지우기` |
| `◇ 보이스 ID가 화자 · 에피소드와 다름`, item text `형식과 다른 ID {n}` (**(0.2.0)** switch on only) | `◇` | A fixed voice ID's speaker or episode part differs from the current format result | `보이스` | `새 ID로` · `그대로` |
| [PROPOSED] `▲ 보이스 ID 겹침 {n}` (**(0.2.0)** switch on only) | `▲` | Manual (`직접`) format and two blocks share an ID | `보이스` | [PROPOSED] `원고로 가기` |
| `◇ 전환 문구 · 쓴 뒤 때 바뀜 · 1-7 씬 4 · 06:10 → 07:00` (or `쓴 뒤 장소 바뀜`) | `◇` | Direct transition text and the scene's place/time differ from the snapshot taken when the text was saved | undecided (`이동` or new `씬`, Q-24) | `자동으로` · `문구 고치기` · `맞음` |
| `◇ 공개 전 장소 · 전환 문구` | `◇` | Automatic transition text names a place whose map visible point is after this scene | undecided | `직접 쓰기` · `맞음` |
| `▲ 숨긴 씬으로 가는 선택지 {n}` | `▲` | Export with hidden scenes excluded and a choice that leads into one (Q-28) | export sheet | — |

- `자동으로` switches the scene's transition to `자동`; `문구 고치기` opens the transition editor; `직접 쓰기` switches to `직접` prefilled with the automatic text [PROPOSED].
- `맞음` and `이대로 두기` are stored by the check area against the block/scene key, so the same warning does not come back after reopening a file.

### 2.13 Export content (script data)

### SCR-63 · Game-engine JSON: content contract (provisional shape) (0.2.0)
**Priority** P1 · **needsExternal** no · **Boards** `Export` · **Sources** rule 22 §6 (confirmed: content decided, format 미정); planning 내보내기 JSON 골격 (제안); rule 19 §5; rule 12 §5–§6; 저장 파일 양식 §1; research §3 (superseded parts dropped)

The engine format is not decided (planning 남은 확인); field names below are the planning skeleton's and are **provisional**. What each scene and block must carry is decided.

**(0.2.0)** Offered only with the switch on (G-7). Changes:
- A dialogue block's `displayName` is the block display name when set (SCR-78), else the display name in effect; `voice` stays the original speaker's ID.
- Choice `options` carry `text` only: no `voice`, never a `_1` suffix (N-23). Stored 0.1.0 option IDs are not written.
- A direction block carries `text` (its text line, plain) beside `cues` `[DEFAULT]`.

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
      { "id": "<block key>", "type": "choice", "options": [{ "text": "따라간다" }, { "text": "불빛 쪽을 한 번 더 본다" }], "rejoin": "<next block key>" },
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

### SCR-64 · Dialogue CSV (`대사 표 CSV`): rows from script data (0.2.0)
**Priority** P1 · **needsExternal** no · **Boards** `Export` · **Sources** rule 12 §6 (상태 column); rule 22 §6 (transition rows); rule 19 §5; planning 내보내기

- Columns: `화자 · 대사 · 보이스 ID · 블록 ID · 상태`.
- One row per dialogue block in manuscript order within the chosen range. **(0.2.0, N-23)** Choice blocks and their options are never rows. `블록 ID` = permanent key. `화자` = [PROPOSED] the character name (rule 19: hidden speakers' names are included); a block display name (SCR-78) does not change it.
- **(0.2.0)** Offered only with the switch on (G-7).
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

### SCR-66 · Fold and unfold continuity (0.2.0)
**Priority** P1 · **needsExternal** no · **Boards** `MFoldScript`, `FoldSpec` · **Sources** concept memory FOLD/UNFOLD MOTION; planning 접기 · 펼치기

- Cover bottom sheet ↔ inner side panel are the **same element**: the shape changes, the tab and scroll position stay.
- The anchor keeps its screen height; blocks above it re-wrap and grow upward. **(0.2.0)** The anchor is the item directly above the input bar (SCR-71), or the selected block while the panel or sheet is open.
- Unfold: decelerate 360 ms; fold: accelerate 240 ms; reduce motion: 120 ms crossfade. Glass bars keep their blur during the transition.
- A closed panel stays closed. **(0.2.0)** While typing in the input bar, its draft, caret, keyboard and mark (`이 뒤에`, `고치기`) are kept. The item directly above the input bar stays the anchor, so the target scene does not change (SCR-71).
- If the device closes apps on fold, the script saves state (episode, selected block, open tab, scroll, and **(0.2.0)** the `draft` with its mark and input caret) and restores it on reopen.
- No controls near the hinge.

### SCR-67 · Script motion (0.2.0)
**Priority** P1 · **needsExternal** no · **Boards** `MType`, `MSwipe`, `MSheet`, `MotionSpec`, `U14SceneNext` · **Sources** motion spec boards; DS README 재질과 움직임; U-14 #16, #17, #21; `DECISIONS.md` (motion via `CivilightMotion`, checked by `MotionLint`)

Every value below goes through `CivilightMotion`; `MotionLint` fails on a literal duration or easing in `feature:script`.

| Motion | Spec |
| --- | --- |
| New block (send, `나누기`, `합치기`) | Enters from 8 dp below, 320 ms decelerate `(.05,.7,.1,1)` |
| Swipe buttons (both directions) | Slide 260 ms; buttons 60 ms apart |
| Scroll to the new block (SCR-71) | 320 ms decelerate |
| Input bar growth, keyboard, mark row appearing (SCR-71) | No animation: snap in the same frame (DS-69) |
| Place-label flash on a scene change (SCR-71) | `standard`, 600 ms total `[DEFAULT]` |
| 낭독 step change (SCR-28) | Crossfade 200 ms standard |
| Sheet and menu open | 320 ms decelerate; background scales to 96.5 % with 2 dp blur (sheets only) |
| Sheet close, delete | 160 ms accelerate `(.3,0,.8,.15)` |
| Tab or value change | 200 ms standard `(.2,0,0,1)` |
| Never moves | The input field's caret line, glass bars while scrolling, the item directly above the input bar while the bar resizes |
| Reduce motion (system · always · never) | All movement becomes a 120 ms fade; scrolls snap |
| Reduce transparency | Bars become opaque `raise` with a `line` border |

- **Superseded (0.1.0):** "new block speaker = previous speaker" (SCR-18), and "the line being typed never moves", which now applies to the input field (SCR-70).

### SCR-68 · `찾기` (find and commands from the header) (0.2.0)
**Priority** P1 · **needsExternal** no · **Boards** `U14Find`, `Search` · **Sources** U-14 #13; icon list (`찾기`, `닫기`); rule 11 §3; rule 12 §5; scope N-13, N-28

- **Opened by** the header's `찾기` icon button (icon `search`, accessibility name `찾기 · 명령`; SCR-01, SCR-03). The bottom of the script holds only the input bar (U-14.13).
  - **Superseded (0.1.0):** the bottom command field and its hint `명령 또는 검색 · 인물 · 블록 · 씬 이동`.
- **Panel** (board `U14Find`): a glass panel from the top of the screen to the keyboard top, padding 16 20.
  - **Field row:** a `search` icon 18, then the query field (focused, keyboard up), then on the right `바꾸기` (text, class c; opens SCR-75) and `닫기` (icon `close`, accessibility name `찾기 닫기`, class a).
  - **Hint line** (small `ink3`): `명령 · 블록 · 인물 · 씬 이동`.
  - The manuscript behind is blurred 6 dp at 50 % opacity. The input bar and the tool row are hidden while the panel is open; the draft is kept.
- **Results**, grouped, with section heads in `sec` style:
  - `이 에피소드 · 블록 {n}`: rows `{episode} 씬 {n} · #{num} {label}` (small) over the whole sentence with the match boxed (memory feedback-search-highlight);
  - other areas' groups from the search area (e.g. `용어 · 1`), with N-13's chips owned by spec 20;
  - `명령`.
  - Tapping a block result closes the panel and opens that block as an entry target (SCR-01: selected and placed directly above the input bar).
- **Script commands:**
  - `렌즈를 이 씬으로 맞추기`, `점검 센터 열기`, `번호 다시 매기기 · {episode}`;
  - `[PROPOSED]` `블록 고르기` (SCR-36);
  - `에셋` (SCR-61, switch on only).
  - **Superseded (0.1.0):** `/대 /나 /연 /선 /씬 /시 /메` (the kind chip and `다음 씬` replace them).
- **Navigation** `[PROPOSED]`, unchanged: `#043` jumps to that block of the current episode; `씬 3` or a scene title jumps to that scene head.
- `닫기` or system back closes the panel and gives focus back to nothing (the keyboard closes).

### SCR-69 · Tabletop posture layout (0.2.0)
**Priority** P2 · **needsExternal** no · **Boards** `FoldSpec` (rules only) · **Sources** planning 반쯤 접기 (rule only); scope table (`규칙만 정함, 시안은 나중에`)

- Tabletop: the manuscript on the upper half; **(0.2.0)** the input bar (with its tool row), panel tabs and keyboard on the lower half; nothing tappable near the fold. Book posture: the two panes as in SCR-03, with the fold as the pane border.

### SCR-75 · Find and replace in the manuscript (`바꾸기`) (0.2.0)
**Priority** P1 · **needsExternal** no · **Boards** none (decision only) · **Sources** decisions N-28; scope N-28, D19; memory feedback-search-highlight

The search panel's layout and the name-field entry belong to other specs:
- spec 20 owns the search screen;
- spec 30 and spec 40 own the `원고 속 n곳 · 바꾸기` line under character, term and place name fields, which opens this mode with `찾을 말` filled in.

This feature owns the script side: matching, the result rows, applying the change and undo.

- **Entry:** `바꾸기` in the `찾기` field row (SCR-68). The panel switches to replace mode:
  - two fields, labelled `찾을 말` (prefilled with the current query) and `바꿀 말` (labels `TODO_COPY_SCR_REPLACE_FIND` and `TODO_COPY_SCR_REPLACE_WITH`; the words come from the N-28 sketch, which is not frozen);
  - then the result rows;
  - then a bottom row with `모두 고르기` and the primary `바꾸기 · {k}`. `k` = the number of checked rows, and the button is off at 0.
- **Range (D19):** manuscript blocks of the open project only: runs of every block kind, choice option text and direction text lines. Not profiles, game text, scene titles, transition text, cues or asset names.
  - Blocks in a locked hidden scene are left out; their count never shows.
  - The matching rule is `Replace.find(doc, query): List<Hit>` (pure, JVM): exact substring match after NFC, case-sensitive, inside a **single run's** text (plain, paren or link). A match across run boundaries is not a hit.
- **Rows:** one per hit, in manuscript order: a checkbox (empty at first, 44 dp), then a meta line `{episode} 씬 {n} · #{num} {label}`, then the whole sentence with the match boxed (search highlight rule). `{label}` follows SCR-20: a locked hidden speaker reads `숨긴 인물` (T-U14-19).
- **`조사 확인` mark** `Particles.needsCheck(old, new, following)` (pure, JVM). A row gets the mark (`◇ 조사 확인`, `ink3`) when all of these hold:
  - the hit is directly followed by one of the particles `이 · 가 · 은 · 는 · 을 · 를 · 과 · 와 · 으로 · 로 · 이나 · 나 · 이랑 · 랑 · 아 · 야`;
  - the last syllables of `찾을 말` and `바꿀 말` are both Hangul;
  - their final-consonant (받침) presence differs.
  - For `으로 · 로` a final ㄹ counts as no 받침.
  - The particle text is **never** changed.
- **Apply** `바꾸기 · {k}`: replaces exactly the checked hits in one undo step (SCR-17).
  - A link run keeps its target and link key; only its text changes.
  - Keys, numbers, records, voice IDs and speakers are untouched.
  - Then a toast `TODO_COPY_SCR_REPLACED` (suggestion `{k}곳 바꿈`) with `되돌리기`. One `되돌리기` restores every changed block.
  - The rows of the replaced hits stay in the list as changed, with the `조사 확인` mark still on.
- Read-only projects: `바꾸기` is not shown.

### 2.15 Versions and foreshadowing (0.2.0)

### SCR-76 · Episode versions: compare and `이 판 글로` (0.2.0)
**Priority** P1 · **needsExternal** no · **Boards** none (decision only) · **Sources** decisions N-29; scope N-29, D20, D21

Spec 20 (story structure) owns the episode ⋯ rows `판 남기기` and `지난 판` and the version list. The scope gives the compare screen to `feature:script`; this spec owns it and the data (D20).

- **Leave a version** (`판 남기기`): stores an `EpisodeVersion` (§3.2) of the episode's live blocks.
  - The default name is `{M}월 {D}일 · 초고` (e.g. `11월 23일 · 초고`) and can be edited before saving.
  - Locked hidden scenes are stored too; the data is the project's.
- **Automatic version on export (D21):** every PDF, JSON or CSV export (switch rules apply) adds one version per exported episode, named `{M}월 {D}일 · 내보낸 판`. A `.civilight` export and the automatic backup add **no** version, or versions would grow with every backup.
- **Compare screen.** Opened from a row of `지난 판`, on both cover and inner. It is one column; the two-column sketch is not frozen (scope §1).
  - Header: the version's name over `{episode}`. The rows follow.
  - **Rows**, from `VersionDiff.rows(version, current)` (pure, JVM): blocks are matched by block key, in current manuscript order. A removed block is placed after the nearest earlier matched block of the version.

| Case | Shown | Button |
| --- | --- | --- |
| Same text | Text as in the manuscript | none |
| Changed text | Current text; every inserted or changed character is boxed (1 dp `acc` outline, radius 3), from a character diff (LCS over code points). A deleted span is shown struck through, in `ink3`, in place | `이 판 글로` (text, class c) |
| In the version, not live now (deleted) | The version's text struck through, `ink3` | none |
| Live now, not in the version (added) | Current text, fully boxed | none |
| In a locked hidden scene (by the **version** block's `sceneKey` or the current one; either is enough) | One row `숨긴 씬 · 블록 {n} · 지문으로 보기` (SCR-58) per hidden scene; no text of it, struck or not | none |

  - Text compared = the block's shown text: runs joined, `Break` as a line break; for choices, option lines joined with line breaks.
- **`이 판 글로`**, one undo step:
  - Sets that block's runs (choices: option lines by the SCR-09 mapping) to the version's.
  - Never touches `◆`, `◇`, number, voice ID, speaker, kind, cues or other blocks (N-29).
  - Offered only when the version's kind and the current kind are both text kinds, or both `선택지` `[DEFAULT]`.
  - There is no whole-episode revert in 0.2.0 (D20).
- Versions are kept until the user deletes one from the list (spec 20). They travel in `.civilight` and backups as an additive field (D20, D29).

### SCR-77 · Foreshadowing and to-dos on blocks (`복선 심기` · `다시 언급 · 거두기` · `할 일`) (0.2.0)
**Priority** P1 · **needsExternal** no · **Boards** none (decision only) · **Sources** decisions N-41; scope N-41, D22; rule 12 §2 (relocation)

Spec 20 owns the home `작업` › `할 일 · 복선` list with its filters `열림 · 거둠 · 끝냄`. This spec owns the block side and the data.

- **Where (D22):** the `속성` tab of the selected block. U-14 has no block ⋯, and the right swipe's five buttons are fixed. Rows:
  - `복선 심기`, on any block kind except `작가 메모`.
    - It opens a sheet: `이름` (required; one line) and `거둘 곳` (optional; an episode picker that shows the chosen episode as `2-5`).
    - The primary button is `TODO_COPY_SCR_PLANT_CONFIRM` (suggestion `심기`).
    - It creates a `Foreshadow` planted on this block.
  - `다시 언급 · 거두기`, on any block kind.
    - It opens a list of the project's foreshadowings (open ones first), with the U-15 create row hidden; hidden locked items are left out.
    - Choosing one shows the two actions `다시 언급` (adds this block as a mention) and `거두기` (sets this block as the payoff, and the state becomes `거둠`).
  - Blocks that carry a mark list them under the rows as `복선 · {name} · {열림|거둠}`, one line each. A tap opens the same list at that item.
  - `할 일`, a switch shown only on `작가 메모` blocks. On makes the memo a to-do (`열림`); checking it done makes `끝냄`.
- **State** `Marks.state(item): 열림 · 거둠 · 끝냄` (pure, JVM; spec 20's filters count with it): a `Foreshadow` is `거둠` when `payoff` is set, else `열림`. A memo to-do is `끝냄` when `todo.done`, else `열림`. Deleted (trashed) items are not counted.
- **No manuscript glyph** for foreshadowing in 0.2.0. The scope names none, and the mark's shape was an open sketch question.
- **No grid view** (scope N-41). There is no `◇` when a `거둘 곳` episode passes (D22).
- **Block deleted (rule 12 §2):** a plant, mention or payoff anchored on a deleted block moves to the next surviving block (same order as SCR-43). It keeps the original block key, is flagged, and is drawn dotted in the `속성` rows. Undo or restoring the block re-attaches it. An empty block that carries a mark is not empty (SCR-04).
- `할 일` on a memo goes with the memo: deleting the memo trashes the to-do with it, and restoring brings it back.
- Each create, mention, payoff and to-do change is one undo step.
- Read-only: the rows are shown without actions.

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
| `runs` | List<TextRun> | no | Text of `DIALOGUE · NARRATION · SYSTEM · MEMO` and **(0.2.0)** the text line of `DIRECTION`; empty for `CHOICE` |
| `stage` | enum `LEFT · CENTER · RIGHT` | yes | `무대 위치`; `DIALOGUE` only; only used when the setting is on |
| `cues` | List<DirectionCue> | no | `DIRECTION` only; empty otherwise |
| `options` | List<ChoiceOption> | no | `CHOICE` only; ≥ 2 options; empty otherwise |
| `voice` | VoiceState | yes | `DIALOGUE` only |
| `recorderSource` | RecorderSource | yes | `MEMO` inserted by the recorder |
| `displayNameOverride` | String | yes | **(0.2.0, additive)** `이 블록만 · 표시 이름` (SCR-78); kept when the kind changes |
| `todo` | `{ done: Boolean }` | yes | **(0.2.0, additive)** `MEMO` only; non-null = `할 일` on (SCR-77) |
| `createdAt` | Instant | no | Creation time |
| `trashedAt` | Instant | yes | Set while the block is in the trash |
| `trashPlacement` | TrashPlacement | yes | Set while in the trash |

**`TextRun`** (sealed; new kinds may be added, older readers show their text)

| Variant | Fields | Meaning |
| --- | --- | --- |
| `Plain` | `text: String` | Ordinary text |
| `Paren` | `text: String` | `괄호 지문`; [PROPOSED] stored without the parentheses, rendered as `(text)` |
| `Link` | `targetKind: CHARACTER · TERM · PLACE`, `targetKey: Ulid`, `text: String` | `@` link; `text` is exactly what the manuscript shows (name, alias or candidate range) |
| `Break` | — | Line break. **(0.2.0)** Made by Enter in the input bar (SCR-15); `줄바꿈` is gone |

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
| `speakerKey` | Ulid | yes | **(0.2.0)** Kept from 0.1.0 files only; never written, shown or exported (N-23) |
| `voice` | VoiceState | yes | **(0.2.0)** Kept from 0.1.0 files only; never generated or exported (N-23) |

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

**`Block.hasContent()` (0.2.0):** `DIRECTION → cues.isNotEmpty() || !runs.isBlankText()`; the other kinds are unchanged (SCR-04).

**`EpisodeVersion`** (0.2.0, additive, stored in the project document; D20)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `key` | Ulid | no | |
| `episodeKey` | Ulid | no | Episode it was taken of |
| `name` | String | no | `11월 23일 · 초고`, `11월 23일 · 내보낸 판` |
| `createdAt` | Instant | no | |
| `auto` | Boolean | no | true for the export version (D21) |
| `blocks` | List<`{ blockKey, sceneKey, order, kind, speakerKey?, runs, optionLines: List<List<TextRun>> }`> | no | Snapshot of the live blocks in manuscript order |
| `deletedAt` | Instant | yes | Deleted from the version list (spec 20) |

**`Foreshadow`** (0.2.0, additive, stored in the project document)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `key` | Ulid | no | |
| `name` | String | no | `복선 심기` name |
| `plant` | `MarkAnchor` | no | Block it was planted on |
| `payoffEpisodeKey` | Ulid | yes | `거둘 곳` (e.g. `2-5`) |
| `mentions` | List<`MarkAnchor`> | no | `다시 언급` blocks |
| `payoff` | `MarkAnchor` | yes | `거두기` block; non-null = `거둠` |
| `deletedAt`, `trashEntryKey` | | yes | Trash (spec 20 list) |

`MarkAnchor` = `{ blockKey: Ulid, originalBlockKey: Ulid? }`. It follows the `근거 지워짐` relocation of SCR-43 (`originalBlockKey` non-null while flagged).

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
| `layers` | `{direction, memo, system, marks, addressHint, paren, termCandidates}`: Boolean | `표시` sheet (Q-14). **(0.2.0)** `termCandidates` is ignored and kept |
| `preview` | enum `WRITE · DIALOGUE_ONLY · READ_ALOUD` | `쓰기 · 대사만 · 낭독`. **(0.2.0)** `READ_ALOUD` is never stored (낭독 is a player, SCR-28); a stored one reads as `WRITE` |
| `draft` | `{ runs: List<TextRun>, cues: List<DirectionCue>, speakerKey: Ulid?, kind: BlockKind, mark: None · After(blockKey) · Edit(blockKey, aside: Draft?), caret: Int }` | **(0.2.0)** The input bar (SCR-70, SCR-72, SCR-73); per project |
| `panel` | `{ open: Boolean, tab: TIME · REF · PROPS · MODULES, scroll: Map<Tab, Int> }` | Panel state |
| `moduleCardOrder` | List<String> | `모듈` card order |
| `lastWritingPosition` | `{ blockKey: Ulid, offset: Int }` | Rule 23 `마지막으로 쓰던 곳`. **(0.2.0)** Set by sends and edits (SCR-59) |
| `restore` | `{ episodeKey, selectedBlockKey?, caretOffset?, scrollAnchor }` | Fold / process-death restore. **(0.2.0)** `caretOffset` is unused (the input caret is in `draft`) |
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
| `story/<episode key>.json` | All live blocks of the episode with every field of 3.2 (runs, cues, options, voice states, **(0.2.0)** `displayNameOverride`, `todo`), the episode's `nextBlockNumber`, and the episode's `DiscardedVoiceId` list [PROPOSED placement] |
| **(0.2.0)** project document (additive optional fields; D20, D29) | `EpisodeVersion` list and `Foreshadow` list. A 0.1.0 file without them opens unchanged |
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

**F3 · Choice fixture.** Episode 1-7, a choice block `#045` with options 1 `따라간다` and 2 `불빛 쪽을 한 번 더 본다` (**(0.2.0)** no option speaker, N-23; T-12-16 adds a 0.1.0 variant), followed by a block `#046`.

**F4 · Scene-head variants** (one scene each, ordinal as shown): (a) 1-7 S4 of F1; (b) same as (a) with band `아침` and no clock; (c) 1-6 씬 1, place 잿더미 시장, approximate `며칠 뒤`, no band, no clock, first timed scene of 1-6; (d) 1-2 씬 1, region 하층 구역 *(fixture)*, `1일째 새벽` band only; (e) 씬 2 with no place and no time; (f) 1-4 씬 2, 회상, region 하층 구역, year 1195.

### 4.2 Rule 12 · blocks and change records

**T-12-01 · Deleting an anchor block relocates the record** (§2 예시 1)
- Given F1.
- When `#043` is deleted.
- Then `#043` is in the trash with `prevKey = #042`, `nextKey = #044`, `ordinal = 7`; R1.anchor = `{seat: BLOCK, blockKey: #044, originalBlockKey: #043}`; `#044` shows a dotted `◆`; the toast reads `블록 #043 지움 · ◆ 1개는 #044 자리로` with `되돌리기`; the check center has `◇ 근거 지워진 변화 1`; no `DiscardedVoiceId` is created (voice was `자동`).

**T-12-02 · Re-anchoring to a rewritten line takes two taps** (§2 예시 1)
- Given T-12-01's result.
- When **(0.2.0)** `이 뒤에` is set on `#042`, the speaker chip is 하르트, the kind chip is `대사`, a line is sent, and `다른 블록에 잇기` is opened on R1.
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

**T-12-08 · `합치기` moves records without flagging** (§4) (0.2.0)
- Given F1 with R10 *(fixture)* on `#039`. When `고치기` is opened on `#039` (나레이션 after the 나레이션 `#038`) and `합치기` is tapped without other changes.
- Then:
  - `#038` holds both texts joined with one space, and keeps its key and number;
  - `#039` no longer exists and is not in the trash;
  - R10.anchor = `{blockKey: #038, originalBlockKey: null}`;
  - the toast reads `#039를 #038에 합침 · ◆ 1개 #038로` with `되돌리기`;
  - `고치기` has ended and `#038` has the `합침` look.
  - One undo restores `#038`, `#039` and R10 exactly.
- Variant *(fixture)*: `#048`'s speaker is changed to 하르트 and `#048` = `고정 HRT_0107_048`. When `고치기` on `#048` › `합치기` (into `#047`, 하르트), then `DiscardedVoiceId("HRT_0107_048", reason MERGED)` exists.
- 0.1.0 pair: `고치기` on `#043` (하르트 after 리엔's `#042`) shows `합치기` dimmed with `화자 다름` (T-U14-11).

**T-12-09 · `나누기` keeps records on the front block** (§4) (0.2.0)
- Given F1 with R6 *(fixture)* on `#047`. When `고치기` is opened on `#047`, the input caret is placed after `뒤돌아보지 말고`, and `나누기` is tapped.
- Then:
  - `#047` reads `뒤돌아보지 말고` and keeps R6;
  - a new block `#058`, speaker 하르트, reads `따라와.` directly after it, with voice `HRT_0107_058 · 자동`;
  - `nextBlockNumber = 59`;
  - the toast reads `#047 나눔 · 뒤 글 → #058`;
  - `고치기` has ended. One undo removes `#058` and restores `#047`'s text.

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

**T-12-16 · Choice blocks** (§4 선택지; N-23) (0.2.0)
- Given F3 with no option speaker. Then:
  - the options display `#045-1` and `#045-2`;
  - no option has a voice ID or a `_1` suffix;
  - the JSON `options` are `[{text: "따라간다"}, {text: "불빛 쪽을 한 번 더 본다"}]` and `rejoin` = `#046`'s key;
  - the CSV has no row for `#045`.
- When a `◆` is created from the choice block, its anchor is `{seat: BLOCK, blockKey: #045}`; no option key can be chosen.
- Variant (a 0.1.0 file): option 1 stores speaker 하르트 and `고정 HRT_0107_045_1`. Then:
  - both stay stored after load, save and reload;
  - neither is shown;
  - JSON and CSV contain neither;
  - no new option suffix is generated.

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

**T-12-20 · Emptying is not deleting; `지우기` relocates** (§2, §4) (0.2.0)
- Given F1. When `고치기` is opened on `#043` and all text is erased. Then the `고치기` button is off (SCR-70) and nothing can be sent.
- When `✕`, then the right swipe › `지우기` on `#043`. Then `#043` goes to the trash, R1 moves to `#044` flagged `근거 지워짐`, and the toast reads `블록 #043 지움 · ◆ 1개는 #044 자리로`.

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

**T-11-03 · Position line and label** (§3, rule 22 §6; U-14 #21) (0.2.0)
- Given F1 with `#045` directly above the input bar. Then the header line reads `1-7 · 씬 3 · 2일째 새벽`, the label `이 씬 · 1-7`, and the place area `씬 3 끝에`.
- Given `#050` directly above the input bar, with `#043` selected. Then the header line reads `1-7 · 씬 4 · 2일째 아침` and the place area `씬 4 끝에`; the selection does not change either.
- When `렌즈를 이 씬으로 맞추기` is chosen with S3 in view. Then the app lens = point (1-7, end of S3).

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

**T-22-07 · `합치기` never crosses a head** (§3) (0.2.0)
- Given F1. When `고치기` is opened on `#049` (first block of S4). Then `합치기` is dimmed with `TODO_COPY_SCR_MERGE_SCENE_START`, and `MergeCheck.reason` returns the scene-start reason. Backspace in the input field changes only the draft.

**T-22-08 · `씬 나누기` routing** (§4) (0.2.0)
- Given F1:
  - right swipe › `씬 나누기` on `#044` → the `씬 나누기` sheet with sub `1-7 씬 3 · #044 아래`;
  - on `#048` (last of S3) → `다음 씬 만들기`;
  - read-only project → the right swipe does not open.
- Nothing changes until a sheet button is tapped.
- `/씬` no longer exists, in the input field or in `찾기`.

**T-22-09 · Split** (§4 예, §5)
- Given F1. When **(0.2.0)** the right swipe › `씬 나누기` is tapped on `#044`.
- Then the sheet shows `씬 3` · `블록 9개` · `‘세라 · 불은 꺼졌어…’ ~ ‘…한 번 깜박였다.’` and `새 씬 4` · `블록 4개` · `‘리엔 · 하르트 씨, 방금…’ ~ ‘…결사의 신호였어요.’` (excerpt rule below); `장소 · 결사의 문 · 씬 3과 같음`; `때 · 씬 3 기준 · 같은 때`; `제목 · 미정`; `말 없는 등장 · 마르타` with `둘 다` selected; `바뀌는 것` rows `순번 · 씬 4 · 미정 → 씬 5`, `순번 · 씬 5 · 미정 → 씬 6`, `첫 공개 · 오리지늄 등불 · 씬 3 #044 그대로`, `◆ 관계 · 리엔 → 하르트 · #043 그대로`, `씬 5 때 · 기준 씬 3 · 2일째 아침 06:10 그대로`.
- When `나누기` is tapped. Then S3 holds `#036`–`#044`; a new scene `씬 4` holds `#045`–`#048` with place 결사의 문, time `S3 기준 · 간격 0 · 정확` (resolved `2일째 새벽 05:40`), transition `없음`, no title, silent cast 마르타; the old S4 is `씬 5` with base S3 and resolved `2일째 아침 06:10`; S5 is `씬 6`; all block numbers, voice IDs and anchors are unchanged. One undo restores the previous state.
- Excerpt rule [PROPOSED, chosen to reproduce the rule's strings]: parentheticals removed; leading excerpt = the longest run of whole words from the start whose length (with spaces) is ≤ 10 characters, trailing `.,?!…⋯` removed, then `…`; trailing excerpt = `…` + the longest run of whole words at the end with length ≤ 10. The leading excerpt is prefixed with `{speaker} · ` for dialogue.

**T-22-10 · Split above records** (§4)
- Given F1. When **(0.2.0)** the right swipe › `씬 나누기` is tapped on `#042`. Then `바뀌는 것` includes `◆ 관계 · 1-7 씬 3 #043 → 1-7 씬 4 #043` and `첫 공개 · 1-7 씬 3 #044 → 1-7 씬 4 #044`.

**T-22-11 · Split inheritance** (§5 table)
- A has no time → B has no time. A is `직접` (fixed point) → B is `A 기준 · 같은 때` and A is the only fixed point. A is a flashback → B shows the flashback row switched on. A is hidden → B shows the hidden row switched on. A is the first timed scene of its episode → A stays `1일째`.

**T-22-12 · `다음 씬 만들기` prefill** (§4)
- Given F1. When **(0.2.0)** the right swipe › `씬 나누기` is tapped on `#048`. Then the sheet prefills `장소 · 결사의 문 · 씬 3과 같음` and `때 · 씬 3 기준 · 같은 때`, each with `비우기`. Tapping `비우기` on both and creating gives a scene with no place and no time; S4 and S5 keep their resolved times.

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
- Given F1 with S3 as target. When **(0.2.0)** the text `— 씬 2 · 기록실 —` is pasted into the input field with the kind chip on `나레이션` and sent. Then a narration block `#058` after `#048` holds that text and the scene count of 1-7 is unchanged.

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

**T-21-01 · Candidate row in `참고`** (rule 21 §2, §5; S-5) (0.2.0)
- Given the block 1-2 씬 1 리엔 `등불 속 그 돌, 오리지… 뭐라고 했죠?`, the candidate check on, and 오리지늄 등불's first reveal set by hand on 1-7 `#044`.
- Then `오리지` has the faint dotted underline, and tapping it only selects the block.
- The left swipe › `참고` starts with the row `용어 후보 · 오리지` + `잇기 · 아님`, with the line `첫 공개 1-7 · 잇기 시` · `▲ 공개 전 사용` under it.
- When `잇기`: the text stays `오리지…`, `오리지` becomes a link run to the term, and the line shows `▲` in the margin.
- When `아님` instead: a dismissal `(block key, term key, "오리지", occurrence 0)` is stored and the underline disappears.

**T-21-02 · Candidate setting and non-output blocks** (rule 21 §2, §4; S-5) (0.2.0)
- With the candidate check off, no candidate underline is drawn and `참고` has no candidate row. A memo `오리지늄 등불 확인` never gets an underline, a row or `▲`, even with the check on.

**T-21-03 · Alias through `@`** (rule 21 §2, §8)
- Typing `@오리` lists `오리지늄 · 오리지늄 등불의 별칭`; choosing it inserts a link run with text `오리지늄`; typing `은` afterwards adds a plain run (`[오리지늄]은`).

**T-23-01 · Recorder memo insertion** (rule 23 §4)
- Given F1 and a recorder record made 2026-11-22 08:41. When it is inserted at `마지막으로 쓰던 곳` = after `#044`. Then a memo block `#058` follows `#044`, its prefix reads `⌁ 작가 메모 · 레코더 11월 22일 08:41`, and the toast reads `1-7 씬 3 · #058 작가 메모 넣음 · 되돌리기 · 가 보기`. (The rule's own example number `#062` comes from a different counter.)

**T-23-02 · Last writing position** (0.2.0)
- Given F1 and a `고치기` send on `#044` as the last edit. Then the recorder's location row reads `마지막으로 쓰던 곳 · 1-7 씬 3 · #044 뒤`.
- After a send that makes `#058`, it reads `… · #058 뒤`.

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
| U-14 #1, #2, #5, #6, #7, #11, #20 (0.2.0) | T-U14-01 … T-U14-05, T-12-02 |
| U-14 #3, #8, #9, #19 (0.2.0) | T-U14-06 … T-U14-11, T-12-08, T-12-09, T-12-20, T-22-07 |
| U-14 #11 with `게임 제작 도구` · scope §8 (0.2.0) | T-U14-12, T-U14-13, T-12-16 |
| U-14 #18, #21 (0.2.0) | T-U14-15, T-U14-17, T-11-03, T-22-08 |
| N-23, N-27, N-28, N-29, N-41, N-43 (0.2.0) | T-12-16, T-U14-18 … T-U14-24 |
| Hidden content in 0.2.0 surfaces · direction text (0.2.0) | T-U14-19, T-U14-25, T-U14-26 |

### 4.7 U-14 and other 0.2.0 features (JVM)

All cases use F1 unless they say otherwise; the switch is off unless they say on. "Send" means `보내기` in the input bar. Expected strings are exact.

**T-U14-01 · Send goes to the scene end; chips stay** (U-14 #1, #2, #20)
- Given `#048` directly above the input bar (target S3), chips 리엔 · `대사`, draft `알아요.`. When sent.
- Then a block `#058` (리엔, `알아요.`, voice `RIE_0107_058 · 자동`) is the last block of S3, after `#048`. `nextBlockNumber = 59`, the draft is empty and the chips read 리엔 · `대사`.
- When two more lines are sent. Then they are `#059` and `#060` at the end of S3, and the chips are still 리엔 · `대사`.

**T-U14-02 · Enter is a line break** (U-14 #7)
- When Enter is pressed between `하나` and `둘` in the draft. Then the draft runs are `[Plain 하나, Break, Plain 둘]` and the document is unchanged. Sending makes one block with those runs.

**T-U14-03 · `DraftRules.canSend`**

| Kind chip | Draft | Switch | Result |
| --- | --- | --- | --- |
| `대사` | `"  "` | off | false |
| `대사` | `[Break, Break]` | off | false |
| `대사` | `[Paren 작게]` | off | true |
| `선택지` | `따라간다` | off | false |
| `선택지` | `따라간다` Break `남는다` | off | true; sent options `["따라간다", "남는다"]` |
| `선택지` | `따라간다` Break Break `남는다` | off | true; the blank line is dropped |
| `연출` | `""` + 1 draft cue | on | true |
| `연출` | `""` + 1 draft cue | off | false (cues hidden) |
| `대사`, speaker chip `화자 없음` | `누구야?` | off | true; the new block raises `▲ 화자 없음` |

**T-U14-04 · `이 뒤에` sends once** (U-14 #5)
- When `이 뒤에` is set on `#044` and `저기.` is sent. Then `#058` sits between `#044` and `#045`, and the mark is gone.
- When `그래.` is sent next. Then `#059` is the last block of S3.

**T-U14-05 · `이 뒤에` mark clears with its block**
- When `이 뒤에` is set on `#044` and `#044` is then deleted with a swipe `지우기`. Then the mark is None and the place area reads `씬 3 끝에`.

**T-U14-06 · A swipe alone changes nothing** (U-14 #3)
- When a right swipe is released open on `#044` and then closed by a scroll, with no button tapped. Then the document equals the one before (deep equality), and no undo step exists.

**T-U14-07 · `고치기` send keeps identity** (U-14 #8)
- When `고치기` is opened on `#043`, the text is changed to `멈춰. 저건 등불이 아니야.`, the speaker chip is set to 리엔, and `고치기` is sent.
- Then `#043` keeps its key and number, R1 still anchors it, its runs are the new text, its speaker is 리엔 and its voice is `RIE_0107_043 · 자동`. One undo restores text and speaker.
- Variant: `#043` = `고정 HRT_0107_043`. Then the voice stays `HRT_0107_043` and `형식과 다른 ID 1` appears when the switch is on.

**T-U14-08 · `고치기` sets the draft aside and gives it back**
- Given the draft `준비` with chips 리엔 · `대사`. When `고치기` is opened on `#047` (하르트).
- Then the field holds `뒤돌아보지 말고 따라와.` and the chips read 하르트 · `대사`.
- When the text is changed and `✕` is tapped. Then `#047` is unchanged, the field holds `준비` and the chips read 리엔 · `대사`.
- When `고치기` is opened on `#047` and sent unchanged. Then no undo step is added.

**T-U14-09 · `고치기` block removed while editing**
- Given `#058` made by a send, with `고치기` open on it and its text changed to `새 글`. When the undo of the send removes `#058`. Then `고치기` ends and the field holds `새 글` with no mark.

**T-U14-10 · `나누기` availability**
- In `고치기` on `#047`: caret at 0 → off; caret at the end → off; caret after `뒤돌아보지 말고` → on.
- In `고치기` on F3 `#045` (`선택지`) → off at any caret.

**T-U14-11 · `MergeCheck.reason`** (U-14 #19)

| `고치기` on | Chips | Result |
| --- | --- | --- |
| `#036` (first of S3) | 세라 · `대사` | scene start |
| `#047` (after the memo `#046`) | 하르트 · `대사` | `종류 다름` |
| `#043` (after 리엔's `#042`) | 하르트 · `대사` | `화자 다름` |
| `#043` | 리엔 · `대사` (chip changed) | on |
| `#039` (after `#038`) | (none) · `나레이션` | on (both speakers none) |
| `#044` (나레이션, after 하르트's `#043`) | (none) · `나레이션` | `종류 다름` (kind first) |

- Accessibility name of the dimmed button in row 2: `합치기 · 꺼짐 · 종류 다름`.

**T-U14-12 · Hidden cues survive editing** (scope §8)
- Given the switch off. `#041` (cues 배경 `하층 거리(새벽)`, BGM `잔불`, no text) renders `▸ 배경 하층 거리(새벽)  ♪ 잔불`.
- When `고치기` is opened on `#041`. Then the field is empty and `고치기` is off.
- When `새벽 거리` is typed and sent. Then `#041`'s runs are `새벽 거리` and its cues are the same two cues (same keys). It renders `▸ 새벽 거리`, and with the switch on `▸ 배경 하층 거리(새벽)  ♪ 잔불  새벽 거리`.
- `KindChange.check(#041, NARRATION)` is blocked with `TODO_COPY_SCR_KIND_HAS_CUES`.

**T-U14-13 · Choice line ↔ option mapping** (SCR-09)
- Given F3, with option keys o1 and o2, and o1 storing speaker 하르트 and `고정 HRT_0107_045_1` (0.1.0).
- When `고치기` lines are `따라간다 · 남는다 · 돌아간다` (one per line). Then the options are o1 `따라간다` (with its stored speaker and voice), o2 `남는다`, and a new key `돌아간다`.
- When `고치기` then sends `따라간다 · 남는다`. Then the third option is removed and no `DiscardedVoiceId` is made, because it had no voice.
- When instead the lines `남는다 · 돌아간다` replace three options whose third, o3, held `고정 X_3` *(fixture)*. Then `DiscardedVoiceId("X_3", DELETED, optionKey o3)` exists, and o1 keeps its key with the text `남는다`.

**T-U14-14 · `KindChange.check`**
- `#043` → `NARRATION`: allowed.
- `#044` → `CHOICE`: blocked with `TODO_COPY_SCR_KIND_CHOICE_LINES` (one line).
- A 나레이션 block `가` Break `나` → `CHOICE`: allowed, options `가`, `나`.
- F3 `#045` → `DIALOGUE`: allowed, runs `따라간다` Break `불빛 쪽을 한 번 더 본다`, speaker none.

**T-U14-15 · `SceneInView.target`**
Items (top, bottom in dp): H3 (S3 head 100–140), #047 (150–200), #048 (210–260), H4 (S4 head 270–310), #049 (320–360); `inputTop` as given.

| `inputTop` | Result |
| --- | --- |
| 265 | S3 (`#048` is the last item with top < 265) |
| 300 | S4 (H4 top 270 < 300) |
| 120 | S3 (H3 is cut by the bar) |
| 90 | S3 (no candidate; the first item below is H3) |

- With H4 and `#049` replaced by one `HIDDEN_ROW` of S4 (270–320) and `inputTop` 330 → S3.
- With only a `HIDDEN_ROW` → null.
- With an empty list → null.

**T-U14-16 · `Particles.objectAfterNumber`**
- 58 → `을`; 39 → `를`; 44 → `를`; 43 → `을`; 45 → `를`; 40 → `을`; 1000 → `을`; 52 → `를`.
- So the toasts read `#058을 #045에 합침` and `#039를 #038에 합침`.

**T-U14-17 · `다음 씬`** (U-14 #18)
- Given S3 as target, draft `준비`. When `다음 씬` is tapped. Then, through `SceneActions` in one undo step:
  - a new scene follows S3 as `씬 4`, with place 결사의 문 and time `S3 기준 · 같은 때` (resolved `2일째 새벽 05:40`), title none and transition `없음`;
  - the old S4 and S5 become `씬 5` and `씬 6` with unchanged resolved times;
  - the target is the new scene (`씬 4 끝에`), and the draft is still `준비`;
  - `다음 씬` is now dimmed (the target scene has no block).
- When `준비` is sent. Then the new scene holds `#058`, and `다음 씬` is on again.
- Given an empty episode. Then the place area reads `씬 없음`, `보내기` is off and `다음 씬` is on. Tapping it makes `씬 1` with no place and no time.

**T-U14-18 · `ReadAloud.steps`** (N-27)
- For S3 the steps are, in order:
  - `Line(세라)`, `Line(오웬)`;
  - three `Line(null)` (`#038`–`#040`);
  - `Caption("▸ 배경 하층 거리(새벽)  ♪ 잔불")`;
  - `Line(리엔)` (with the paren `작게`), `Line(???)` (`#043`, display name in effect), `Line(null)` (`#044`), `Line(리엔)`, `Line(???)` (`#047`), `Line(리엔)`.
  - The memo `#046` is skipped: 12 steps.
- With `#047`'s block display name `경비병`, step 11 is `Line(경비병)`.
- F3: `#045` is one `Choices(["따라간다", "불빛 쪽을 한 번 더 본다"])`, followed by `#046`'s step.
- With S4 hidden and locked, no step comes from `#049`–`#052`.
- With the switch on and `#041`'s 배경 asset having an image: steps 1–5 have `background = null`, and steps 6–12 have that asset. Starting at `#045` (step 10) gives that asset, with no need to pass step 6. With the switch off every step has `null`.

**T-U14-19 · Find and replace** (N-28)
- Given five plain hits of `등불` *(fixture blocks)*. When rows 1, 3 and 5 are checked and `바꾸기 · 3` is tapped. Then only those three occurrences change, and one undo restores all three.
- A hit inside a link run changes the link's text and keeps its target and link key.
- `Particles.needsCheck`:
  - `하르트` → `리엔` before `가` → true; before `는` → true; before `의` → false;
  - `서울` → `부산` before `로` → true (ㄹ counts as no 받침 for `로`);
  - `서울` → `대전` before `을` → false (both have 받침);
  - `R2` → `리엔` before `가` → false (not Hangul).
- With 노아 hidden and locked and a hit in a 노아 line, the row's label reads `숨긴 인물`, never `노아`. A hit inside a locked hidden scene gives no row.

**T-U14-20 · Version diff and `이 판 글로`** (N-29)
- Given a version V of 1-7 taken from F1. Then `#043` is changed to `멈춰. 저건 등불이 아니야.`, `#046` is deleted, and `#058` is added after `#048`. Then `VersionDiff.rows(V, now)` gives:
  - `#043` changed (`춰` boxed, `추지 마` struck);
  - `#046` deleted, placed after `#045`;
  - `#058` added (fully boxed);
  - every other row unchanged.
- `이 판 글로` on `#043` restores `멈추지 마. 저건 등불이 아니야.`. R1, the number and the voice are unchanged, and it is one undo step.
- A round trip of V through `.civilight` export and import gives an equal V.
- An export of 1-7 adds a version named `{M}월 {D}일 · 내보낸 판` with `auto = true`.

**T-U14-21 · Foreshadowing relocation** (N-41)
- Given a `Foreshadow` planted on `#044`. When `#044` is deleted. Then `plant = {blockKey: #045, originalBlockKey: #044}` and `#044` is not empty (it went to the trash). Undo gives `plant = {blockKey: #044, originalBlockKey: null}`.

**T-U14-22 · Block display name** (N-43)
- Given `#047` with `displayNameOverride = "경비병"`. Then:
  - the label reads `경비병 (하르트)`;
  - JSON (switch on) gives `displayName: "경비병"` and `voice: "HRT_0107_047"`;
  - the CSV `화자` is `하르트`;
  - 낭독 shows `경비병`.
- Changing `#047` to 나레이션 and back to 대사 keeps `경비병`.

**T-U14-23 · Draft persistence**
- A `draft` with the mark `Edit(#043, aside = {runs 준비, chips 리엔 · 대사})` and caret 4 survives encode and decode of the script view state unchanged.
- It is not present in a `.civilight` export.

**T-U14-24 · Foreshadowing and to-do states** (N-41)
- A `Foreshadow` with no payoff → `열림`; with a payoff → `거둠`. A memo with `todo = {done: false}` → `열림`; `{done: true}` → `끝냄`. Counts for two open, one paid-off and one done item: `열림 2 · 거둠 1 · 끝냄 1`. A trashed item is not counted.

**T-U14-25 · No hidden text in new surfaces** (rule 19; release gate)
- Version: V holds S4 (`#049`–`#052`); then S4 is hidden and locked and `#050` is deleted. `VersionDiff.rows` gives one hidden-scene row for S4, and no row carries `#050`'s or any S4 text, struck or not.
- `고치기`: open `고치기` on `#050` and change its text; then S4 becomes hidden and locked. `고치기` ends, the field is empty (or holds the set-aside draft), and no `#050` text remains in the draft.

**T-U14-26 · Direction text is kept on every path** (SCR-08)
- With the kind chip `연출` and draft `배경 하층 거리, 잔불 BGM` sent: the new block's runs equal `[Plain 배경 하층 거리, 잔불 BGM]`.
- `changeKind` of that block to `나레이션` and back to `연출` keeps the runs.
- `나누기` on it after `배경 하층 거리,` gives two direction blocks `배경 하층 거리,` and `잔불 BGM`.
- `합치기` of two adjacent direction blocks `가` and `나` gives `가 나`.

---

## 5. Screens

Cover = 390×844 boards (Compact). Inner = 800×800 boards (Medium/Expanded). DS components in parentheses.

| # | Screen / sheet / state | Boards | Cover | Inner | Components | Features |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | Script, reading (0.2.0) | `U14Input`, `U14SceneNext`, `U14Delete`, `RSHead` | Full-width manuscript; header 118 dp with position line, title, `이 씬 · 1-7`, `찾기` and `표시` icons; input bar (chip row with place area and `다음 씬`, field, send) | `U14Wide`: 560 dp column at x 40, panel 290 dp; input bar under the column only | GlassBars, ScriptBlock, LensChip (as position label), chips, IconButton, Toast | SCR-01, 02, 03, 05–11, 46, 70, 71, 74 |
| 2 | Writing in the input bar (0.2.0) | `U14Typing`, `U14Long`, `U14Sent`, `U14Choice`, `U14Cue`, `U14CueGame` | Header 76 dp (position line, `이 씬 · 1-7`, `찾기`); input bar on the keyboard with tool row `( ) · @ · ◆ · ↶`, chip row, field up to 6 lines | Same, under the column | GlassBars, TextField, chips, Buttons | SCR-15, 16, 70, 71 |
| 3 | Kind chip menu (0.2.0) | `U14Kind` | Popover above the chip row, 230 dp wide | Same | Popover, ListRow | SCR-12, 13 |
| 4 | Speaker picker | `Picker`, `MSheet` | Bottom sheet | [PROPOSED] same sheet over the column | Sheet, Tag, ListRow | SCR-19 |
| 5 | `표시` sheet | `Layers` | Bottom sheet from y 250 | [PROPOSED] same sheet | Sheet, Controls (toggle), Tag | SCR-26–29 |
| 6 | Swipe buttons (0.2.0) | `U14SwipeLeft`, `U14SwipeRight`, `MSwipe`, `Tour2`, `Walk1` | Left: row slides 192 dp, 3 buttons; right: row slides 300 dp, 5 buttons | Same | SwipeTabs | SCR-30 |
| 6a | `이 뒤에` and `고치기` states (0.2.0) | `U14After`, `U14Edit`, `U14Split`, `U14Merge` | Mark row in the place area; `고치는 중` block; `고치기` send button; toasts `나눔` · `합침` | Same | ScriptBlock, Buttons, Toast | SCR-72, 73 |
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
| 19 | Term candidate underline and `참고` row (0.2.0) | `U14Input` (#044), `scr_core.cand_row` | Faint dotted underline; row `용어 후보 · 등불 · 잇기 · 아님` at the top of `참고` | Same, in the side panel | ScriptBlock, Buttons | SCR-33, 56 |
| 20 | Read-only example (0.2.0) | `U14ReadOnly`, `OnbExampleScript`, `Tour1`, `Tour2`, `Tour4` | Header line `읽기 전용 · 편집 불가`; 96 dp bottom row `예시 프로젝트` · `예시 홈으로` · `온보딩 끝내기` | Same row under the column `[DEFAULT]` | GlassBars, Buttons | SCR-60 |
| 21 | Asset list | `Assets` | Full screen list with chips | Not drawn. [PROPOSED] list modules' 목록 + 상세 layout | ListRow, Tag, ImageTile | SCR-61 |
| 22 | Fold transition | `MFoldScript`, `FoldSpec` | Sheet ↔ panel morph | — | Sheet | SCR-66 |
| 23 | Motion references | `MType`, `MSwipe`, `MSheet`, `MotionSpec` | — | — | — | SCR-67 |
| 24 | Script rows in other areas' screens | `ProjSettings` (스크립트 추가 칸), `Export`, `Trash`, `Check`, `SceneWhere` (`전환 문구` chips), `RTFlashback`, `RTCascade` | — | — | ListRow, Tag | SCR-24, 25, 48, 39, 62–65 |
| 25 | `찾기` panel (0.2.0) | `U14Find` | Glass panel from the top to the keyboard; field row with `바꾸기` and `닫기` | Same over the column | GlassBars, TextField, ListRow | SCR-68, 75 |
| 26 | 낭독 player (0.2.0) | not drawn | Full screen: background, dialogue box, `이전 · 처음부터 · 닫기 · 다음` | Same | Buttons | SCR-28 |
| 27 | Version compare (0.2.0) | not drawn | One column of rows with boxes, strikes and `이 판 글로` | Same (one column) | ListRow, Buttons | SCR-76 |
| 28 | Foreshadowing rows and sheets (0.2.0) | not drawn | `속성` rows; `복선 심기` sheet; `다시 언급 · 거두기` list | Same in the side panel | Sheet, ListRow, TextField | SCR-77, 78 |

**(0.2.0) Superseded rows:** 2 (`EditKeyboard`), 3 (`Blocks` add sheet) and 19's popover describe 0.1.0 only and are replaced as above.

**Not drawn (behaviour specified, layout [PROPOSED]):** `다음 씬 만들기` sheet, `앞에 새 씬` sheet, transition-text editor, direction-cue editor, asset add/edit form, `옮기기` destination picker, `다른 블록에 잇기` / `블록에 잇기` candidate list, select-mode entry, `필드` and `용어 공개` tabs of the `◆` sheet, `대사만` / `낭독` previews, `호칭 힌트` line, hidden-scene row, deleted-speaker 속성 variant, point-seat line in the `시간` tab, empty episode / empty scene states, inner keyboard toolbar.

---

## 6. Open questions

Only gaps and contradictions that block an exact implementation. Each names the sources on each side. Defaults chosen in this spec are marked [PROPOSED] where they are used.

| # | Question | Sources |
| --- | --- | --- |
| Q-01 | The script header title `잿빛 새벽의 등불` is both the project name and the title of episode 1-7. Which one does the header show? | `gen_nm.py script_head`; Home board (`1-7 · 잿빛 새벽의 등불`); `Assets` header (project name as small line) |
| Q-02 | **Closed (0.2.0, U-14 #3, #4).** A tap only selects; swipes only reveal buttons. A row released past half its reveal width stays open, a shorter swipe returns (SCR-30). | Planning 스와이프; DS SwipeTabs; `MSwipe`; U-14 |
| Q-03 | **Closed (0.2.0, U-14 #7).** Enter in the input bar is a line break; `줄바꿈` is gone (SCR-15). | Planning 입력 중; U-14 |
| Q-04 | **Closed (0.2.0, N-43).** A per-block display name in `속성` (SCR-78). | Decisions N-43 |
| Q-05 | **Closed (0.2.0, N-23).** Choices are text only: no speaker, no voice suffix, no CSV rows (SCR-09). | Decisions N-23 |
| Q-06 | Voice ID format: episode code for `막 › 장면`, 4-level and episode-less structures; numbers above 99/999; the `ID 형식 · 바꾸기 ›` editor and the `직접` mode UI | Planning 범위 (`HRT_0107_043`); rule 12 §6; `ProjSettings` |
| Q-07 | Renumbering vs "numbers are never reused": after `다시 매기기` a block can take a number whose discarded voice ID exists (auto ID equals a `버린 ID`). What is `nextBlockNumber` after renumbering, and do trashed blocks keep their old numbers (collision on restore)? [PROPOSED: counter unchanged; trashed blocks keep numbers; an automatic ID equal to a discarded ID raises the `▲` duplicate check] | Rule 12 §5 (다시 매기기; 지운 번호는 다시 쓰지 않고), §6 (겹치지 않는 이유) |
| Q-08 | **Closed (0.2.0, N-27).** Line-by-line playback without TTS (SCR-28). The word stays `낭독` (D18). | Decisions N-27 |
| Q-09 | `호칭 힌트`: which addressees are shown above a line, and in what format? | Planning 인물 말투; `Layers` (`대사 위 호칭 표시`); rule 13 §4 |
| Q-10 | Do `괄호 지문` go into game exports (JSON text, CSV 대사)? Are link runs flattened to plain text? | Rule 21 §4, §8 (`미정`); planning PDF option only |
| Q-11 | Direction cues: the full `화면 효과` list (rule 22: 페이드 · 암전; planning JSON: `shake`; research: overlay, delay, caption, dialog.hide), whether BGM has a stop cue, and the cue editor | Planning 블록 종류 and JSON 골격; rule 22 §2; research §3 |
| Q-12 | **Closed (0.2.0).** Right swipe `종류`, `속성 · 종류 · 바꾸기`, and the kind chip in `고치기`; the conversion table is SCR-13 (its two block reasons are `TODO_COPY`). | U-14 #3, #8; board `U14Wide` |
| Q-13 | **Closed (0.2.0, U-14 #19).** `합치기` only with the same kind and speaker, else dimmed with `종류 다름` or `화자 다름` (SCR-72). | U-14 |
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
| Q-33 | **Closed (0.2.0, U-14 #3).** The right-swipe button `지우기` (SCR-30, SCR-38). | U-14 |
| Q-34 | **Closed (0.2.0).** The speaker chip decides (SCR-18); an empty block cannot be sent (SCR-70). | U-14 #2 |
| Q-35 | Time default for a new scene: rule 22 prefills `다음 씬 만들기` (and story structure's `새 씬`, "같은 동작") from the previous scene; rule 11 §4 says a scene inserted in the middle starts with no time. Which applies to `새 씬` between two scenes and to `앞에 새 씬`? | Rule 22 §4; rule 11 §4 |
| Q-36 | Rule 12 §3 says records moved behind the writing line become 예정, but with rule 11 §1's definition (end of the furthest scene with content) a moved block always gives its destination content and extends the writing line, so a block-anchored record can never become 예정 by a move. Is a different writing-line definition meant (e.g. memo-only blocks do not count as content)? | Rule 12 §3; rule 11 §1 |

**0.2.0 defaults (`[DEFAULT]` in the text).** Each one was fixed so the lane can build. The user may overrule any of them as a 0.2.x usability fix.

| # | Default taken | Where | Sources |
| --- | --- | --- | --- |
| Q-37 | The chip `이 씬 · 1-7` keeps `렌즈를 이 씬으로 맞추기` (U-14 #20), although scope S-1 says the chip "only names the scene in view". The point lasts until the project closes (D9) | SCR-02 | U-14 #20; scope S-1, D9 |
| Q-38 | A link tap only selects (U-14 #4). There is no in-script way to open a link's card in 0.2.0; it is reached through `찾기` | SCR-14, SCR-55 | U-14 #4; SCR-55 0.1.0 |
| Q-39 | In `고치기`, chip changes are applied to the block on the `고치기` send, not at once ("칩을 바꾸면 그 블록의 화자 · 종류도 함께 바뀐다"), so `✕` can undo everything | SCR-72 | U-14 #8 |
| Q-40 | When `고치기` ends, the chips return to their values from before it, and any draft set aside comes back | SCR-18, SCR-72 | U-14 #2, #8 |
| Q-41 | Read-only bottom row uses the board's `예시 프로젝트` with the header line `읽기 전용 · 편집 불가`, not a `읽기 전용` line in the bar | SCR-60 | U-14 #15; scope U-14.15; board note |
| Q-42 | `다음 씬` creates at once with the `다음 씬 만들기` defaults (no sheet), after the scene in view, and is dimmed while that scene is empty. For a scene in view that is not the last, taking `같은 때` picks one side of open Q-35 (rule 11 §4 says a scene inserted in the middle starts with no time) | SCR-74 | U-14 #18; Q-35 |
| Q-52 | Scope acceptance U-14.6 says `씬 n 끝에` shows in every state except read-only, but the boards `U14After` and `U14Edit` replace it with `#044 뒤에` and `#044 고치기 · …`. This spec follows the boards | SCR-70 | U-14 #5, #6, #8; scope U-14.6 |
| Q-43 | The speaker chip also dims for `시스템` | SCR-18 | U-14 #2, #20; SCR-10 |
| Q-44 | The tool row `◆` targets the `고치기` block or the selected block, never the draft; `↶` is project undo, never draft undo | SCR-16 | U-14 #10 |
| Q-45 | `새로 · 나눔 · 합침` looks last until the next user action; the place-label flash is 600 ms | SCR-04, SCR-71 | U-14 #17, #21 |
| Q-46 | Initial chip values: saved per project; else `대사` + the 0.1.0 inherited speaker | SCR-18 | U-14 #2 |
| Q-47 | Copy with no source: `TODO_COPY_SCR_MERGE_SCENE_START`, `TODO_COPY_SCR_KIND_HAS_CUES`, `TODO_COPY_SCR_KIND_CHOICE_LINES`, `TODO_COPY_SCR_REPLACE_FIND`, `TODO_COPY_SCR_REPLACE_WITH`, `TODO_COPY_SCR_REPLACED`, `TODO_COPY_SCR_PLANT_CONFIRM`, `TODO_COPY_SCR_ASSET_CREATE` | SCR-13, 15, 61, 72, 75, 77 | scope §1 |
| Q-48 | A cue-only 0.1.0 direction block shows its cue summary as read-only text with the switch off. `고치기` on it starts with an empty field and never touches the cues | SCR-08, SCR-72 | scope G-2, §8 |
| Q-49 | Scene heads are not 낭독 steps; paren runs are shown in 낭독 | SCR-28 | decisions N-27 |
| Q-50 | `이 판 글로` is offered only when the version's kind and the current kind are both text kinds or both `선택지`; the compare screen is one column on both widths | SCR-76 | decisions N-29; scope §1 |
| Q-51 | Foreshadowing has no glyph in the manuscript; `복선 심기` is not offered on memos (memos use `할 일`) | SCR-77 | decisions N-41; scope N-41 |

