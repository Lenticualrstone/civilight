# 70 · 라이프 레코더 (Life Recorder) — feature prefix `LRC`

Implementation spec for the Life Recorder area of Civilight (Android, Jetpack Compose). Feature ids `LRC-01` … `LRC-47` and `LRC-48` … `LRC-50` (§7, 0.2.0). Engineers implement from this file alone; every Korean string in `code` style is UI copy and must be used verbatim.

## 0. Conventions

**Sources read (2026-11-22).** Planning doc live tabs: rule 23 `레코더 연결` (rev 2, header `2026-11-22 추천안대로 확정`), 저장 파일 양식 (rev 8, confirmed 2026-11-21); local snapshot of the main planning text (`main_live.txt`, section 라이프 레코더 and 서재 · 숨기기 · 수위 등급, 온보딩, 저장 · 백업 · 내보내기, 범위, 남은 확인); confirmed rule drafts `rules/19.md`, `20.md`, `21.md`, `23.md` (23.md is the full text; the live tab is a condensed copy of it with the same decisions); memory files (concept, vocabulary, rules-19-23, rules-blocks, character-fields + file format, android-app, feedback-*); board generators `scr_lr.py`, `scr_decide.py`, `scr_states.py`, `scr_core.py`, `scr_inner.py`, `scr_onb.py`, `scr_onb2.py`, `scr_motion.py`, `scr_rating.py`, `scr_new_safe.py`, `scr_new_cast.py`, `scr_lib.py`, `scr_tools.py`; `boards_text.txt`; design system README and component READMEs; sibling specs `10-script.md`, `30-cast.md`, `40-world.md`, `50-canvas-pages.md`, `60-library-security-files.md` (read for contracts only, not edited).

**Source precedence** (newest authority first). When two sources disagree, the higher one wins; only real conflicts go to §6.

1. Confirmed rule tabs and memory notes of 2026-11-22: rule 23 (this area's rule), rule 19, rule 20, rule 21, 저장 파일 양식, rule 12 (via memory).
2. Board generator source `design/night-manuscript/gen/*.py` (the Lr boards were regenerated after rule 23 at 00:35).
3. Main planning text (live snapshot, 01:16).
4. `boards_text.txt` dump (00:08) and `new_safe_text.txt` — **older than the generators for every Lr board**.
5. Design system README, research.

*Source note — stale copy that this spec does NOT use.* `boards_text.txt` still shows the pre-rule-23 copy on `LrInbox`, `LrHide`, `LrLocked`, `LrUnlock`, `LrUnlocked`, `LrRecord`, `LrProcess`, `LrText`, `LrEntry`, `LrSettings`, `ColLr`, `TwoLr`: `붙일 곳 · 1-7 씬 3`, `+ 인물 · 용어`, filter `연결됨`, `1-7 씬 3에서`, `인물에 붙이기`, `인물 · 용어에 붙이기 · 고른 카드의 메모로`, `커서 위치 · 고른 씬`, `2장 메모로`, `붙일 곳 제안 · 보던 곳을 칩으로 · 지우면 기록함`. `new_safe_text.txt` (`RHRatingSheet`, `RHRatingDown`) shows `‘붙일 곳’에 등급만`; the generator `scr_new_safe.py` says `연결에 등급만`. The DS README §라이프 레코더 still says `붙일 곳 칩으로 제안`; the planning text's 설정 line still lists `붙일 곳 제안`. All of these are superseded by rule 23 §7 and the regenerated boards; the copy in this spec is the rule-23 copy.

**Markers**

- `[PROPOSED]` = behaviour no source states that this spec had to choose to make the feature implementable. Everything not marked comes from a source.
- `copy: not in sources` = a state needs a label no source gives. Do not invent Korean copy; use a string resource `TODO_COPY_LRC_<n>` and raise it (listed in §6).
- "cover" = cover screen (390×844 boards, Compact width). "inner" = unfolded inner screen (800×800 boards, Medium/Expanded width).
- Board names drop the `NmDark`/`NmLight` prefix and `.dc.html` (e.g. `LrInbox`). Every board exists in dark and light.
- Separators in copy are ` · ` (space, middle dot, space). No em dash in UI copy.
- Markers in UI: `◆` change (`acc`), `◇` soft notice (`ink3`), `▲` warning (`warn`), `⌁` author memo. Dotted outline / dotted underline = attached to something deleted (DS README).
- Font: the app uses **Pretendard everywhere** (memory `civilight-android-app`, user decision 2026-11-22). Mockups show Nanum Myeongjo for the transcript and record bodies; implement those as the **manuscript body style** (Pretendard) at the sizes given here. Numbers (timer, durations) use tabular figures (`tnum`), not a mono font.
- Sample project: 「잿빛 새벽의 등불」 (R-15, 장 › 에피소드 › 씬, scene `1-7 씬 3`). Second project 「유리 정원」 (ALL). Hidden R-18 project 「검은 서약 (가제)」.
- Fixed clock for examples and tests: **2026-11-22**, local time zone (from `레코더 11월 22일 08:41`, `오늘 08:41`, `어제 23:10`).

---

## 1. Scope

### 1.1 What this area owns

The Life Recorder (`라이프 레코더`) is a capture tool for sudden ideas, by voice (default) or text. Its records live in a **library outside every project** (`라이브러리 · 프로젝트 밖`), the 기록함 (inbox). This area owns:

1. **Entry points** that start a capture: the mic on every `CommandBar`, the home-screen widget, the quick-settings tile, the cover-screen widget, lock-screen recording, and the inbox's own `말로 기록` / `글로 쓰기` (LRC-01 … LRC-06).
2. **Recording** (`녹음 중`): immediate start, live transcript, pause, cancel, switch to text, no silence auto-stop, voice glow, background continuity (LRC-07 … LRC-10).
3. **Transcription** (`받아쓰기`): on-device by default, cloud optional (LRC-11, LRC-12).
4. **Processing** (`정리 중`): final transcript, links, conditional `숨기기` row, `여기 넣기`, `이어서 말하기`, `기록함에 두기` (LRC-13); original audio keeping and playback (LRC-14).
5. **Text memo** (`글 메모`) with `@` links (LRC-15, LRC-16, LRC-17).
6. **Links** (`연결`, rule 23 §2): the fact that a record relates to a project, chapter, episode, scene, character, place, term, faction or page; pre-filled link from 보던 곳; the link picker; link labels, masking and deleted-target states (LRC-18 … LRC-24).
7. **The 기록함**: list, states `정리 안 됨 · 넣음 · 끝냄`, filters incl. `프로젝트 ▾`, record screen, ⋯ menu, empty state, unfolded layouts (LRC-25 … LRC-31).
8. **넣기** (copy out of a record): `원고에 넣기` with the position sheet, `여기 넣기`, `카드에 넣기`, `페이지로 만들기`, `⋯ › 메모 칸에 넣기`, `⋯ › 인물로 만들기`, and the two-way pointers between record and copy (LRC-32 … LRC-39).
9. **Hiding records** with biometric open/re-lock, the hidden default for hidden/R-18 targets, deleting records (LRC-40 … LRC-43).
10. **설정 · 라이프 레코더** screen and the recorder library file / export selection, and the hooks other areas call (LRC-44 … LRC-47).

### 1.2 Hand-offs to other areas

| Area (spec) | What it owns that this spec only references |
| --- | --- |
| 스크립트 (`10-script.md`, SCR) | Rendering of the `⌁` memo block created by `원고에 넣기` / `여기 넣기` (SCR-59), `Block.recorderSource`, block numbers `#043` and the "next number" rule (SCR-21), the **last writing position** per project (SCR-59 "Last writing position"), the scene memo field on the `모듈` tab that shows `레코더 · 정리 안 됨 n ›` (CSheetMods / WideMods), undo history inside the script. |
| 인물 (`30-cast.md`, CST) | The profile line `레코더 · 정리 안 됨 n ›` (CST-49), the character tabs `개요 · 말투 · 비밀 · <긴 글 항목>` that receive `카드에 넣기` paragraphs, creating the character for `⋯ › 인물로 만들기`, the delete impact sheet row `레코더 칩 1` (rule 20), character merge. |
| 세계 (`40-world.md`, WLD) | Place · term · faction cards as link targets and as `카드에 넣기` targets (their long-text tabs/fields), `@` link picker content for terms (aliases, rule 21). Rule 21: Life Recorder text is never scanned for `▲` / `후보`. |
| 캔버스 · 페이지 (`50-canvas-pages.md`, CNV) | The page created by `페이지로 만들기` (CNV-15) and its source mark. |
| 스토리 구조 · 홈 · 장소와 때 (`20-story-time.md`) | Chapter / episode description line that receives `⋯ › 메모 칸에 넣기` for 장 · 에피소드, scene moves (link follows the key), project home as an entry context. |
| 서재 · 보호 · 파일 (`60-library-security-files.md`, LIB) | 서재 library row `라이프 레코더 · 기록 n · 정리 안 됨 u` (LIB-01), new-project step `라이프 레코더 기록 연결` (LIB-12) and checklist `연결된 기록 2개 정리하기` (LIB-13), onboarding screens incl. `OnbVoice`, `Walk3`, the module tour host incl. `TourLr` (LIB-19), the protection value `recorderLinkMask` and `recorderLinkLabel(project)` (LIB-28), `CaptureGuard` / FLAG_SECURE (LIB-26), biometric prompt helper and auth failure handling (LIB-32, LIB-37), re-lock policy (LIB-33), the trash host with filter `레코더` (LIB-38/39), the toast host (LIB-40), the export sheet and `.civilight` writer (LIB-41, LIB-47), the `.civilight-recorder` package format (LIB-48), Drive backup (LIB-57), restore (LIB-59), app setting `recorderGlow` (AppSettings, shared with `설정 · 움직임 · 투명도`). |
| 점검 센터 | Rule 20's `지운 항목` row counts `레코더 1` (this area supplies the count). `◇ 넣은 뒤 고침` and `◇ 넣은 곳 지워짐` are **never** sent to the check center (rule 23 §4, §6). |
| 명령 칸 · 검색 | The `CommandBar` component itself; this area owns only what the mic does. |
| 온보딩 | Example project lifecycle (`온보딩 끝내기` deletes it); this area defines what happens to records linked to it. |

---

## 2. Features

### 2.1 Entry points

#### LRC-01 · 명령 칸 마이크 (command-bar mic)
**Priority** P0 · **needsExternal** no · **Boards** `Main`, `LrEntry`, `LrRecord`; DS `CommandBar` · **Sources** MAIN 라이프 레코더 (들어가는 곳: 모든 명령 칸의 마이크; 녹음: 누르면 바로 시작); DS CommandBar README; rule 23 §2 (보던 곳 table)

**(0.2.0)** mic only on 프로젝트 홈; every other command bar has none (LRC-48).

- Every `CommandBar` (every screen that has one: script, project home, cast, world, timeline, map, 서재, inbox excepted — the inbox has its own bottom buttons) has on its right a 32 px circular mic button, fill `pri`, icon `onpri`, `aria-label="라이프 레코더"`.
- Tap → **recording starts at once** (no intermediate screen, no confirmation) and the recording screen (LRC-07) opens over the current screen. The current screen becomes the **entry context** (보던 곳) for the pre-filled link (LRC-19).
- If the microphone permission is not granted → LRC-06 instead of recording.
- If a recording is already running (e.g. started from the tile) → the mic opens that running recording screen instead of starting a second one [PROPOSED].
- Labels on boards: `LrEntry` panel `명령 칸의 마이크` · `모든 화면 · 명령 칸 오른쪽`; walkthrough `Walk3` `떠오르면 말로` · `명령 칸 마이크 · 라이프 레코더` (owned by onboarding).

#### LRC-02 · 홈 화면 위젯 (home-screen widget)
**Priority** P1 · **needsExternal** yes (Android app widget) · **Boards** `LrEntry`, `OnbVoice`, `EmptyLr` · **Sources** MAIN 들어가는 곳; `LrEntry` panel; `OnbVoice` choice; rule 23 §2 table

**(0.2.0)** Not offered in 0.2.0 (LRC-48, N-73 `안 함`).

- A home-screen widget showing the mic in a `pri` circle and two lines: `떠오른 생각` (title) / `눌러서 바로 녹음` (sub). Panel label on `LrEntry`: `홈 화면 위젯` · `앱을 열지 않고 바로 녹음`.
- Tap → recording starts immediately in a recording surface that does not open the app's normal screens (`앱을 열지 않고`): `ED` a translucent, task-isolated activity that hosts only LRC-07/LRC-13 and finishes back to the launcher after `기록함에 두기` or `취소`.
- Pre-filled link: none; only `+ 연결` (rule 23 §2 table).
- The widget never shows project names, record text or counts (rule 19: hidden projects leave widgets; the widget has no content to mask) [PROPOSED: the widget is static].
- Added from onboarding (`OnbVoice` choice `홈 화면 위젯 추가` · `앱을 열지 않고 바로 녹음`, owned by LIB-15) via `AppWidgetManager.requestPinAppWidget` `ED`, or from the launcher's widget picker.

#### LRC-03 · 빠른 설정 타일 (quick-settings tile)
**Priority** P1 · **needsExternal** yes (TileService) · **Boards** `LrEntry` · **Sources** MAIN 들어가는 곳; `LrEntry` panel `빠른 설정 타일` · `알림창에서 한 번 · 잠금 화면에선 녹음만`; rule 23 §2

**(0.2.0)** Not offered in 0.2.0 (LRC-48).

- A quick-settings tile with the mic icon. Tile label: copy: not in sources (`TODO_COPY_LRC_01`; `ED` use the app-wide name `라이프 레코더`, which is existing copy, until decided).
- One tap starts recording immediately (`알림창에서 한 번`).
- From the lock screen it records only (LRC-05): `잠금 화면에선 녹음만`.
- Pre-filled link: none.

#### LRC-04 · 커버 화면 위젯 (cover-screen widget) and unfold continuation
**Priority** P1 · **needsExternal** yes (launcher/cover-screen widget host of the foldable) · **Boards** `LrEntry` · **Sources** MAIN 들어가는 곳 (커버 화면); `LrEntry` panel `커버 화면` · `접은 채 위젯으로 · 펼치면 이어서`; rule 23 §2 table (`커버 화면 위젯` → 없음)

**(0.2.0)** Not offered in 0.2.0 (LRC-48).

- The same widget as LRC-02 placed on the cover screen. Tap while folded → recording starts on the cover screen.
- **Unfolding during recording continues the same recording** on the inner screen without a gap: same timer, same transcript, same links, same audio file (`펼치면 이어서`). The fold rule "만지던 것은 제자리" applies: the transcript's last line keeps its screen height.
- `ED` On devices whose cover screen is a normal launcher (Fold class) this is the LRC-02 widget; devices with a vendor-only cover widget host are out of scope until the main device model is decided (planning 남은 확인 "주 기기 모델").
- Pre-filled link: none.

#### LRC-05 · 잠금 화면 녹음 (lock-screen recording)
**Priority** P1 · **needsExternal** yes (show-when-locked activity, keyguard) · **Boards** `LrSettings`, `LrEntry` · **Sources** MAIN 들어가는 곳 (잠금 화면(새 녹음만)); concept memory (lock-screen recording allowed, new recording only, inbox needs unlock); `LrSettings` row `잠금 화면에서 녹음` · `새 녹음만 · 기록함은 잠금 해제 후`; rule 23 §2 (잠금 화면 → 없음 · `+ 연결`만), §2 버린 안 (잠금 화면 녹음이 원고를 바꾸면 안 됨)

**(0.2.0)** Not offered in 0.2.0; the setting row is hidden, its value kept (LRC-48).

- When setting `잠금 화면에서 녹음` is on (default on), the tile (and any lock-screen shortcut the OS offers) starts a recording **without unlocking the device**.
- On the lock screen only these are available: LRC-07 (record, pause, `취소`, `멈추고 저장`), LRC-13 with `이어서 말하기` and `기록함에 두기`. Not available: `글로 쓰기` [PROPOSED: typing on the keyguard is allowed by the OS, but the text sheet's `@` search would list project items], `여기 넣기`, the inbox, any 넣기.
- `+ 연결` is shown (rule 23 table) but tapping it requests device unlock first (`ED` `KeyguardManager.requestDismissKeyguard`), because the link picker lists project and item names [PROPOSED; see §6 Q7]. Cancelling the unlock keeps the recording screen.
- After `기록함에 두기` the recording surface finishes and the lock screen returns. The record is in the inbox with no link.
- Setting off → the tile on the lock screen asks for device unlock first, then records.

#### LRC-06 · 마이크 권한과 `마이크 꺼짐` (mic permission and error)
**Priority** P0 · **needsExternal** no · **Boards** `ErrMic`, `OnbVoice` · **Sources** MAIN 온보딩 (마이크 권한: 시스템 요청 전에 이유 표시); `OnbVoice` choice `마이크 허용` · `녹음할 때만 사용`; `ErrMic`; MAIN 상태 화면 (오류: 마이크 꺼짐)

- `RECORD_AUDIO` is requested only with a reason shown first (onboarding `마이크 허용` · `녹음할 때만 사용`, owned by LIB-15). If onboarding was skipped, the first mic tap shows the same reason line before the system prompt [PROPOSED: reuse the `OnbVoice` row as a small sheet].
- Permission denied or revoked → full-screen glass `ErrMic` instead of recording:
  - Header left `라이프 레코더`, right text button `닫기`.
  - Mic icon 28 px in colour `r18`; title `마이크 꺼짐` (headline 26/34, weight 800); sub `시스템 설정에서 허용 필요 · 녹음할 때만 사용` (13 px, `ink2`).
  - Buttons stacked: primary `시스템 설정 열기` (52 high, radius 26) → app details settings (`ACTION_APPLICATION_DETAILS_SETTINGS`); ghost `글로 쓰기` → text memo (LRC-15) with the same entry context.
  - `닫기` → back to where the user was.
- When the user returns from system settings with permission granted, the error screen closes and recording does **not** start by itself [PROPOSED: no action without a tap]; the user taps the mic again.

### 2.2 Recording

#### LRC-07 · 녹음 중 (recording screen)
**Priority** P0 · **needsExternal** yes (speech-to-text, LRC-11/12) · **Boards** `LrRecord`, `TourLr` (owned by LIB-19), `Walk3` · **Sources** MAIN 녹음; concept memory (live transcript, unconfirmed tail dimmed, band glow, no rings); rule 23 §7 row 녹음 중 (`연결 · 1-7 씬 3 · + 연결`); `scr_lr.py record()`

**(0.2.0)** Pause releases the mic and resume opens a new segment; back saves the take as `정리 안 됨` (LRC-49 replaces the pause line and the `[PROPOSED]` back rule below).

Layout (cover, 390×844; full window):
- Background: the screen the user was on, blurred 3 px, under a full-window glass layer (`.glass`).
- Header row (top 18, left 22, right 16): an 8 px square rotated 45° in `acc` (live mark), `라이프 레코더` (13/18, weight 600), timer `00:12` (13 px, `ink2`, tabular), flexible space, text button `취소` (`.lab`).
- Link row (top 64): chips, 32 high, gap 6, wrapping: one chip per link `연결 · <label>` with a link icon (14) and `×` (see LRC-18 … LRC-21 for labels and the × rule), then the chip `+ 연결` (no ×). Example: `연결 · 1-7 씬 3` · `+ 연결`. Only the **first** link chip carries the word `연결 · `; further chips show only the label [PROPOSED from `LrText`: `연결 · 잿빛 새벽의 등불` · `오웬` · `세라 @`].
- Transcript (top 150): section label `듣는 중` (`.sec`); paragraph in manuscript body style 25/40, colour `ink`. The part already confirmed by the recognizer is `ink`; the unconfirmed tail is `ink3`; a 2×26 px caret in `acc` follows the last character. Example: `하르트가 등불을 싫어하는 이유. 대정전 때 누군가를 잃었다는 설정은 어떨까,` (`ink`) + `그게 세라의` (`ink3`) + caret. The transcript scrolls so the caret line stays visible; earlier lines move up.
- Glow (LRC-10) behind the bottom edge.
- Bottom row (left/right 16, bottom 26, gap 10): ghost button with text icon `글로 쓰기`; primary button `멈추고 저장` (flex 1, height 56, radius 28, 15 px); ghost circular button 56×56 with pause icon, `aria-label="일시정지"`.

Behaviour:
- Timer counts recorded time `mm:ss` from `00:00` (pauses excluded).
- `일시정지` → audio capture and recognition pause; timer stops; the live mark stops pulsing; the button's icon becomes a record/resume icon. Tap again → resumes into the same record [PROPOSED: paused state not drawn; label `aria-label` for resume: copy: not in sources `TODO_COPY_LRC_02`].
- `멈추고 저장` → stops capture, **creates and persists the record** (state `정리 안 됨`, kind voice, duration = timer), then shows `정리 중` (LRC-13). The record exists from this moment even if the app is killed before the user taps anything on LRC-13 [PROPOSED].
- `취소` → stops and discards the audio and transcript; no record is created; the screen closes. `ED` show the toast host's undo toast for 4 s with copy: not in sources (`TODO_COPY_LRC_03`) and `되돌리기`, which restores the discarded recording into `정리 중`. (No source defines 취소; §6 Q8.)
- `글로 쓰기` → LRC-16 (switch to text).
- `+ 연결`, chip tap, chip × → LRC-20 / LRC-18. Links can be changed while recording.
- Back gesture / system back = same as `취소`? **No** [PROPOSED]: back minimises to the ongoing-recording notification (LRC-09); it never discards.
- Module tour (owned by LIB-19): `TourLr` 2/3 highlights the link chip row with tooltip `연결` · `보던 곳 · 넣기 전까지 내용은 그대로`, label `레코더 안내 · 2/3`, buttons `건너뛰기` · `다음`.

Inner (unfolded): same content full-window over the blurred current screen; the text column keeps a maximum width of 560 centred [PROPOSED: not drawn at 800].

#### LRC-08 · 침묵 자동 멈춤 없음 (no auto-stop)
**Priority** P0 · **needsExternal** yes (continuous recognition) · **Boards** `LrRecord` · **Sources** MAIN 녹음 (침묵 자동 멈춤 없음: ‘멈추고 저장’을 눌러야만 끝난다); concept memory (user decision 2026-11-21); DS README (직접 멈출 때까지)

- A recording ends **only** when the user taps `멈추고 저장` (or `취소`). Silence of any length never stops, pauses or saves it.
- `ED` engineering consequence: platform recognizers end a session on silence. The engine (LRC-11) must run continuous/segmented sessions (API 33+ `EXTRA_SEGMENTED_SESSION`, or restart a new session on every end-of-speech) and append results to the same transcript; the audio file is recorded by the app itself (one continuous file), independent of recognizer sessions.
- No maximum length is defined [PROPOSED: none; storage permitting].

#### LRC-09 · 녹음 계속 (background continuity, interruptions)
**Priority** P1 · **needsExternal** yes (foreground service `microphone` type, notification) · **Boards** — · **Sources** LRC-08 rule; rule 19 §3 (notifications never show hidden names); `LrEntry` (`펼치면 이어서`)

**(0.2.0)** In-app leave saves and releases (LRC-49); the background case below stays (D32).

- Leaving the recording screen (home, other app, screen off, fold/unfold) does not stop the recording (consequence of LRC-08). `ED` run capture in a foreground service with `foregroundServiceType="microphone"` started while the recording screen is visible.
- Ongoing notification: copy: not in sources (`TODO_COPY_LRC_04`). It must contain no transcript text and no link label (a link may point to a hidden or R-18 project; rule 19 §3 notifications show only `숨긴 프로젝트 N` / `R-18 프로젝트`). Tapping it returns to LRC-07.
- Audio focus loss (incoming call) → the recording pauses (same as `일시정지`) and stays paused until the user resumes [PROPOSED; §6 Q8].
- Process death while recording: the audio written so far is kept; on next launch the record is created with what was captured and opens in `정리 중` [PROPOSED].
- Fold/unfold: state is saved and restored (planning: "접을 때 앱이 닫힐 수 있어 상태 저장 · 복원이 필수").

#### LRC-10 · 빛 띠 (voice glow)
**Priority** P1 · **needsExternal** no · **Boards** `LrRecord`, `LrProcess`, `Walk3`, `LrSettings`, `ReduceSettings`, `MotionSpec` · **Sources** MAIN 녹음 (네이비 빛 띠), 처리 중 (아래 빛이 좌우로 흐른다); concept memory (bands, not rings; reference libraries.dev/voice); feedback memory (no ring/donut effects); `scr_lr.py` (`GLOW`, `LR_CSS`); `MotionSpec` (`선형 · 커서 · 빛 띠만`, `줄이기 · 이동 없이 페이드 120 ms · 빛 띠 정지`)

- Seven vertical **bands** (never rings or circles) rising from the bottom edge, spanning the full width + 40 px overflow each side, positioned 60 px below the bottom edge, blurred 34 px, saturation 1.2, rounded tops 40 px, each a vertical gradient from its colour (bottom) to transparent (top). Non-interactive.
- Colours, band i uses `colors[i % 7]`:
  - dark: `#3E6FA8`, `#8FB8E6`, `#1F5590`, `#B5D3F5`, `#5B8CC8`, `#2B4C7E`, `#9CC3EE`
  - light: `#5B8CC8`, `#1F5590`, `#8FB8E6`, `#2B4C7E`, `#A9CCF2`, `#3E6FA8`, `#1F5590`
- **Recording:** reference heights `[170, 260, 330, 240, 300, 210, 150]` px, opacity 0.95. In the app each band's height follows the input level: `ED` height_i = ref_i × (0.72 + 0.36 × level_i), level_i = smoothed RMS of the last ~100 ms (0…1), with per-band phase offsets of 0.23 s so neighbouring bands differ; easing **linear** (MotionSpec: linear only for caret and glow). Silent input settles at 0.72 × ref.
- **Processing** (`정리 중`): heights `[90, 140, 120, 160, 110, 80, 60]`, opacity 0.8, no per-band motion; the whole band group sweeps horizontally −90 px ↔ +90 px, 2.4 s, ease-in-out, alternating, until processing ends.
- Reduce motion (`설정 · 움직임 · 투명도` → 줄이기, or system `애니메이션 삭제` when following the system): glow shown **static** (no level motion, no sweep) — `빛 띠 정지`.
- Setting off (`LrSettings` `녹음 중 빛 띠` · `말소리 크기 표시`, same value as `ReduceSettings` `라이프 레코더 빛` · `녹음 중 아래 빛 띠`; stored once as AppSettings `recorderGlow`, default on) → no glow at all.
- Reduce transparency does not affect the glow (it is not a glass bar) [PROPOSED].

### 2.3 Transcription

#### LRC-11 · 받아쓰기 · 기기 안 (on-device transcription, default)
**Priority** P0 · **needsExternal** yes (speech-to-text model) · **Boards** `LrSettings`, `LrRecord`, `LrProcess` · **Sources** MAIN 받아쓰기 (기기 안이 기본, 오프라인, 음성이 기기 밖으로 나가지 않음); concept memory (settled 2026-11-21); MAIN 남은 확인 (기기 안 받아쓰기 엔진 · 결정할 것)

- Default engine: on-device, offline; audio never leaves the device.
- Live partial results feed the transcript tail (`ink3`); final results turn `ink`.
- Language: Korean (`ko-KR`) [PROPOSED: single language; no picker in sources].
- `ED` engine: `SpeechRecognizer.createOnDeviceSpeechRecognizer` (API 31+) with audio supplied from the app's own capture (`EXTRA_AUDIO_SOURCE`, API 33+) so the same audio goes to the file and the recognizer; on API 29–30 or when no on-device Korean model is installed, see §6 Q1 (engine undecided in sources).
- If no on-device recognizer/model is available: the recording still records audio; the transcript area shows copy: not in sources (`TODO_COPY_LRC_05`); the record is saved with empty text and can be typed in later (LRC-28) [PROPOSED].
- Text is stored NFC-normalised (file format §2).

#### LRC-12 · 받아쓰기 · 클라우드 (cloud transcription, optional)
**Priority** P1 · **needsExternal** yes (network STT provider) · **Boards** `LrSettings` · **Sources** `LrSettings` radio `클라우드` · `정확도↑ · 음성 전송 · 인터넷 필요`; MAIN 범위 (클라우드 받아쓰기 v1 포함, 선택 · 기본은 기기 안); MAIN 남은 확인 (클라우드 받아쓰기 제공자 · 결정할 것)

- Chosen only in `설정 · 라이프 레코더` › `받아쓰기` (radio). Default `기기 안`.
- When `클라우드`: audio is sent to the provider; results replace on-device results in the same UI.
- No network at recording time → transcribe on-device for that recording [PROPOSED]; no automatic upload later.
- Records of a hidden project: no special rule in sources; the choice is global [§6 Q2].
- Provider: undecided (§6 Q1).

### 2.4 After recording

#### LRC-13 · 정리 중 (processing screen)
**Priority** P0 · **needsExternal** no · **Boards** `LrProcess`, `TwoLr` (right pane) · **Sources** MAIN 처리 중; rule 23 §4 (`여기 넣기 · #044 뒤`), §6 (숨기기 줄 기본값), §7 row 정리 중; `scr_lr.py process()`

Layout (cover, full window glass over the blurred previous screen):
- Header: `라이프 레코더` (13/18, 600) and duration `00:24` (`ink2`, tabular). No `취소`.
- Glow in **processing** mode (LRC-10) while sentence finalisation runs.
- Section label `받아쓰는 중 · 문장 나누기` while the final pass (punctuation, sentence split) runs; then the final transcript in manuscript body style 25/40 `ink`. Example: `하르트가 등불을 싫어하는 이유. 대정전 때 누군가를 잃었다는 설정은 어떨까. 그게 세라의 오빠였다면?`. When the pass ends the label is removed and the glow stops [PROPOSED: end state not drawn].
- Link row (margin-top 22): same chips as LRC-07 (`연결 · 1-7 씬 3` · `+ 연결`).
- Conditional `숨기기` row (LRC-42): only when any link target is a hidden project or an R-18 project; a switch row, **on** by default; can be turned off before leaving. Row copy: `숨기기` + sub `지문 · PIN으로 열림` [PROPOSED: reuse the ⋯ menu copy of `LrHide`].
- Small note (13/20, `small`): `원본 음성 함께 보관 · 글은 나중에 고치기 가능` when audio keeping is on (LRC-14); when off, copy: not in sources (`TODO_COPY_LRC_06`).
- Buttons (bottom 26, column, gap 10):
  1. `여기 넣기 · #044 뒤` (ghost, full width) — **only** when the recording was started from the script (entry context = script with a caret); `#044` is the block number of the caret block at the moment recording started (LRC-34).
  2. Row: `이어서 말하기` (ghost, flex 1) · `기록함에 두기` (primary, flex 1).

Behaviour:
- `이어서 말하기` → back to LRC-07 continuing the **same record** (timer resumes from `00:24`, new audio appended, new text appended after a space).
- `기록함에 두기` → saves link and hidden changes and closes the recorder surface; the user is back where they started (script, home, launcher, lock screen). Toast: none [PROPOSED].
- `여기 넣기 · #044 뒤` → performs `원고에 넣기` at that exact position (LRC-34); nothing is inserted before the tap (rule 23 §4).
- Leaving by system back = `기록함에 두기` [PROPOSED].
- The text on this screen is read-only; editing happens in the record screen (LRC-28) (`글은 나중에 고치기 가능`).

#### LRC-14 · 원본 음성 보관과 재생 (original audio)
**Priority** P1 · **needsExternal** no · **Boards** `LrSettings`, `LrInbox`, `LrProcess` · **Sources** MAIN 설정 (원본 음성 보관 · 30일 뒤 정리 가능); `LrSettings` row `원본 음성 보관` · `글과 함께 · 30일 뒤 정리 가능`; `LrProcess` note; rule 23 §4 (음성 파일은 원고로 옮기지 않는다; 출처 표시를 누르면 기록이 열리고 음성은 거기서 재생); file format §6 (레코더 원본 음성은 선택 · 기본 끔)

- Setting `원본 음성 보관` (default on): the recorded audio is stored with the record (`ED` AAC/M4A mono 32 kbps in app-private storage, named by content hash).
- Off: audio is discarded after the transcript is final; the record keeps its duration (`음성 0:24`) but has no player [PROPOSED].
- `30일 뒤 정리 가능`: meaning not specified (manual cleanup offer vs automatic removal) — §6 Q3. `ED` nothing is deleted automatically (rule 12/23 principle "아무것도 조용히 지우지 않음").
- Playback: inbox voice rows and the record screen show a 32 px round play button (`tint`) and a 20-bar waveform (3 px bars, gap 2, heights from the audio envelope; played part in `acc`, rest `ink3`). Tap play ↔ pause. Only one record plays at a time.
- Audio is **never** copied into the manuscript, cards or pages; copies link back to the record, where it plays.
- Export: audio goes into `.civilight` only with `원본 음성 포함` checked (LRC-45).

### 2.5 Text memo

#### LRC-15 · 글 메모 (text memo sheet)
**Priority** P0 · **needsExternal** no · **Boards** `LrText`, `EmptyLr`, `LrInbox` · **Sources** MAIN 글 메모 (아래 시트, @로 인물 · 용어 연결); rule 23 §5, §7 row 글 메모; `scr_lr.py textmemo()`

- Opened by: `글로 쓰기` in the inbox bottom bar, `글로` in the empty inbox, `글로 쓰기` on `ErrMic`, `글로 쓰기` during recording (LRC-16). Entry context = the screen underneath (`LrText` shows it over the project home of 「잿빛 새벽의 등불」).
- Bottom sheet (DS `Sheet`, top radius `radius-lg`, handle 36×4), height to the keyboard:
  - Header row: `라이프 레코더 · 글` (13/18, 600) · right text button `닫기`.
  - Text field (min height 120, padding 14, radius 16, `tint` background, inset 1 px `line`), manuscript body style 18/30, caret `acc`. `@` links render with a 2 px `acc` underline (`@세라`).
  - Link row (margin-top 10): chips per LRC-18; example `연결 · 잿빛 새벽의 등불` · `오웬` · `세라 @` · `+ 연결`.
  - Bottom row: ghost round 48 px mic button `aria-label="말로 기록으로 바꾸기"` (LRC-16); hint `@로 인물 · 용어 · 씬 연결` (`small`, flex 1); primary `저장`.
  - Keyboard below.
- `저장` → creates the record (kind 글, state `정리 안 됨`, time = now) with its links; sheet closes; no toast [PROPOSED]. Disabled while the text is empty or whitespace [PROPOSED].
- `닫기` with non-empty unsaved text → keep the draft and close [PROPOSED: the draft returns next time the sheet opens from any entry; no discard dialog in sources]; with empty text → close.
- The `숨기기` row (LRC-42) for hidden/R-18 link targets: rule 23 names only the `정리 중` screen; `ED` the text sheet shows the same row above the bottom row when the condition holds (§6 Q9).

#### LRC-16 · 말 ↔ 글 바꾸기 (switch voice ↔ text)
**Priority** P1 · **needsExternal** no · **Boards** `LrRecord` (`글로 쓰기`), `LrText` (`말로 기록으로 바꾸기`) · **Sources** MAIN 녹음 (녹음 중 ‘글로 쓰기’로 전환 가능); `LrText` mic button

- During recording, `글로 쓰기` stops capture and opens the text memo sheet over the same background with the **transcript so far** in the text field and the same links; the audio captured so far stays attached to the record, so the saved record is a voice record whose text the user typed on [PROPOSED; §6 Q10].
- In the text sheet, `말로 기록으로 바꾸기` starts recording (LRC-07); the typed text so far becomes the start of the transcript and the links carry over [PROPOSED].
- Either switch keeps the original entry context (for `여기 넣기` and the pre-filled link).

#### LRC-17 · 글 메모의 `@` (links inside text)
**Priority** P0 · **needsExternal** no · **Boards** `LrText` · **Sources** rule 23 §5; rule 19 §6 (locked hidden items are not @ candidates); rule 20 §2 (@ to a deleted item); rule 21 (records are not scanned)

- Typing `@` in the text memo opens the shared link picker (same component as the script's `@`, SCR-55) with candidates of kinds 인물 · 용어 · 씬 (board hint `@로 인물 · 용어 · 씬 연결`) and the other link units of LRC-18 (장 · 에피소드 · 장소 · 세력 · 페이지) [PROPOSED: rule 23 §2 makes every unit a link unit; the hint names three].
- **Search order** (rule 23 §5): (1) items of projects this record is already linked to, (2) items of the most recently opened project, (3) everything else. Within a group: name / 초성 / alias match as SCR-55.
- Choosing an item inserts an `@` link run (text = chosen name or alias, key = item key) and adds a link with origin `@` (LRC-18). The chip for it shows `<name> @` (small `@` in `ink3`, 11 px) **without ×**: it can only be removed by deleting the `@` text. Deleting the run removes the link and the chip.
- Choosing an item of another project also links that project (implied by the sub-link; LRC-18).
- Never offered: items of locked hidden projects, locked hidden items (rule 19 §6 "검색 제외"), items in trash.
- Dictated text (voice) **never** creates `@` runs or links by itself (rule 23 §5). See LRC-20 for `글에 나온 이름`.
- Life Recorder text is not part of rule 21: no `▲ 공개 전 사용`, no `후보` (rule 21 §5 table row 라이프 레코더 · 아니오).

### 2.6 Links (`연결`)

#### LRC-18 · 연결 모델 (link model)
**Priority** P0 · **needsExternal** no · **Boards** `LrRecord`, `LrProcess`, `LrText`, `LrInbox`, `LrHide` · **Sources** rule 23 §2 (규칙, 연결 단위, 하위 연결, 영구 키, 연결이 하는 일 셋), §3, §5; vocabulary memo (`붙일 곳` → `연결`)

- A **link** (`연결`) is a fact: "this record is about X". It **never** moves or copies content anywhere; the record always stays in the library.
- Units: 프로젝트 · 장 · 에피소드 · 씬 (named by the project's level names) · 인물 · 장소 · 용어 · 세력 · 페이지. **No block-level links** (the position is chosen when inserting).
- A sub-link implies the project link: a link to `1-7 씬 3` also means 「잿빛 새벽의 등불」 for filtering and export. The implied project link is **derived**, not stored as a chip [PROPOSED].
- Links point at permanent keys (ULID). The label shown (`1-7 씬 3`) is computed at display time from the current structure (scene number in its episode).
- Labels: project → project name; chapter → `<n>장`; episode → `1-7`; scene → `1-7 씬 3`; card → item name (`하르트`); page → page name [PROPOSED for chapter/page format; scene and episode per rule 23 examples]. Masking and deleted states override labels (LRC-22, LRC-23).
- A link records its **origin**:
  - `보던 곳` — pre-filled from the entry context (LRC-19); has ×; leaving it in place counts as the user's choice.
  - `직접` — added with `+ 연결`; has ×.
  - `@` — from an `@` run in the text; no ×; removed only by editing the text.
  - `넣기` — added when content was inserted into a target not yet linked (LRC-32); has × [PROPOSED].
  - `시작 재료` — added by new-project step 4 (LIB-12); has × [PROPOSED].
- Duplicates: the same target is linked at most once; a second add is a no-op; an `@` to an already-linked target turns that chip into the `@` form while the run exists [PROPOSED].
- What a link does (rule 23 §2, exactly three things):
  1. puts the record under that project in the inbox's `프로젝트 ▾` filter (and under the target in target filters, LRC-27);
  2. includes the record in that project's `.civilight` export (LRC-45);
  3. makes the record visible from the target: `레코더 · 정리 안 됨 n ›` (LRC-24).
- Links and states are **separate axes**: a linked record that was never inserted is `정리 안 됨`; the old copy `… 에서` (origin) is not used.
- Chip × removes a non-`@` link immediately, with no confirmation [PROPOSED: a link is a fact the user can re-add].

#### LRC-19 · 보던 곳 연결 (pre-filled link from the entry context)
**Priority** P0 · **needsExternal** no · **Boards** `LrRecord`, `LrText`, `LrSettings` · **Sources** rule 23 §2 (보던 곳 · 보던 곳이 없는 입구 table), §7 row 설정; `LrSettings` row `보던 곳 연결` · `시작한 곳을 칩으로 · 언제든 빼기`

**(0.2.0)** Only the 프로젝트 홈 mic and the inbox buttons remain as entries (LRC-48).

- When setting `보던 곳 연결` is on (default on), a new recording or text memo starts with **one** link chip for the entry context:

| Entry | Pre-filled link |
| --- | --- |
| Script command bar | that scene |
| Character card, world card (place · term · faction) | that card |
| Page | that page |
| Project home · 스토리 구조 · 연표 · 지도 | that project |
| 서재 · home-screen widget · quick-settings tile · lock screen · cover-screen widget | none · only `+ 연결` |
| Inbox `말로 기록` / `글로 쓰기` | the inbox's project filter if one is set (`프로젝트 ▾` = a project), else none |

- Screens not in the table (e.g. 점검 센터, 설정, canvas) → the project if the screen is inside a project, else none [PROPOSED].
- The chip is visible, removable (×) and replaceable (tap → picker, LRC-20). Nothing is filed silently.
- Entry context inside a **locked** hidden item or project cannot occur (they are not on screen). Entry context inside an open hidden project → the chip is pre-filled as usual and LRC-42's `숨기기` default applies.
- Entry inside the onboarding example project → chip `예시 · 끝나면 연결 없어짐` (rule 23 §6) instead of the normal label; it behaves as a project/scene link until `온보딩 끝내기` (LRC-23).
- Setting off → no pre-filled chip from any entry.

#### LRC-20 · 연결 고르기 (link picker: `+ 연결` and chip tap)
**Priority** P0 · **needsExternal** no · **Boards** — (picker not drawn; chips on `LrRecord`, `LrProcess`, `LrText`, `LrInbox`) · **Sources** rule 23 §2 (탭하면 고르기 시트가 열린다; 추가 버튼은 어디서나 ‘+ 연결’), §5 (글에 나온 이름 묶음), rule 19 §6 (검색 제외); DS `Sheet`

- `+ 연결` opens a bottom sheet (cover) / popover-sheet (inner, DS: side panel or popover becomes a sheet on cover) to **add** a link. Tapping an existing non-`@` chip opens the same sheet to **replace** that link's target [PROPOSED: replace = remove old + add new in one step].
- Sheet content [PROPOSED layout, no board]: title `+ 연결` is the button label; sheet title copy: not in sources (`TODO_COPY_LRC_07`); a search field (name, 초성, alias); a project level row list; then results grouped by kind with the unit names used in LRC-18.
- **`글에 나온 이름` group** (rule 23 §5): for voice records (and any record) the first group lists characters and terms whose **name is contained verbatim** in the record text (exact character match, no particles logic, no fuzzy matching). It is only an ordering inside the picker the user opened; nothing appears by itself elsewhere. Group label `글에 나온 이름`.
- Order after that group: items of already-linked projects → most recently opened project → the rest (same as `@`).
- Excluded: locked hidden projects and their contents, locked hidden items, trashed targets, the read-only example project after `온보딩 끝내기`.
- On the lock screen the sheet requires device unlock first (LRC-05).
- Choosing a target in another project adds that link; the project is implied.

#### LRC-21 · 연결 칩 표시 (chip rendering rules)
**Priority** P0 · **needsExternal** no · **Boards** `LrRecord`, `LrProcess`, `LrText`, `LrInbox`, `LrHide`, `LrLocked`, `LrUnlocked` · **Sources** `scr_lr.py` (chip(), inbox item conn row), rule 23 §2, §5, §7

- Capture screens (LRC-07, LRC-13, LRC-15): DS chip 32 high, gap 6, icon link 14, `ink` text, `×` 14 for removable links; `+ 연결` chip last. First link chip prefixed `연결 · `.
- Inbox rows: a link row below the text: link icon (14, `ink2`) followed by selected-style tags (`tag on`) with only the labels, e.g. `1-7 씬 3` · `하르트`; no ×, no `연결 · ` prefix. A **project** link is not shown as a tag on the inbox row when the record also has a link to something inside that project (the R2 row reads `오웬` · `세라` although R2 is linked to 「잿빛 새벽의 등불」 too: rule 23 §3 table and `LrInbox`) [PROPOSED rule; §6 Q24]. Capture screens and the record screen show every link chip.
- Locked-inbox visible rows (`LrLocked`, `LrUnlocked`): the link tags sit at the right of the row header (`오웬` `세라`).
- `LrHide` sheet's record row shows links in the meta line: `오늘 08:41 · 1-7 씬 3 · 하르트`.
- Order of chips: creation order of the links [PROPOSED].

#### LRC-22 · 연결 가림 (masking: R-18, hidden project, hidden item)
**Priority** P1 · **needsExternal** no · **Boards** `RHRatingSheet`, `RHRatingDown`, `RatingSettings`, `ProjSettings`, `LrInbox` · **Sources** rule 23 §6 (가림 table, R-18 확정, 열어 둔 동안에도 같게); rule 19 §2 (example: 바로 ‘R-18’), §3 (휴지통 · 기록함 위치: 숨긴 프로젝트), §6 table (기록함 연결 · 숨긴 항목); LIB-28 `recorderLinkLabel(project)`; LIB-26 capture guard

**(0.2.0)** The `R-18` label becomes the masked-name constant (LRC-50).

Every place in this area that shows a link label (chips, inbox link rows, LrHide meta, picker, `넣은 곳` lists, project filter) uses:

| Target | Label shown |
| --- | --- |
| Project with `라이프 레코더 연결 가리기` on (R-18 default) — any link into it (project, scene, card, page…) | `R-18` (the project's rating badge text) only. Same while the project is open. |
| Hidden project, locked | `숨긴 프로젝트`; after unlocking (group open) the real label |
| Hidden item (character, scene, term, place, faction, page) in a visible project, locked | `숨긴 항목`; after unlocking that project's items, the real label |
| The record itself hidden and locked | the whole row is collapsed into the locked group; links not shown at all (LRC-40) |

- Several masked links of the same project collapse into one chip `R-18` / `숨긴 프로젝트` [PROPOSED].
- The rating label comes from `recorderLinkLabel(project)` (LIB-28), not a literal.
- **Hiding wins over deletion** (rule 20 edge row, rule 19 §3): a link into a hidden project or to a hidden item keeps showing `숨긴 프로젝트` / `숨긴 항목` while the target is in the trash, and `영구 삭제 · 숨긴 항목` after purge; the real name is never printed (evaluation order in §3.3).
- Masking is a **presentation** value: it applies immediately to existing records when the value changes (rule 19 §2: raising 「잿빛 새벽의 등불」 to R-18 with the row checked makes the record `하르트가 등불을 싫어하는 이유` show `R-18` at once).
- Capture guard: whenever a visible inbox row, chip or record screen shows content of a record linked to a project whose `화면 캡처 가리기` is on, report that project to `CaptureGuard` (LIB-26) so FLAG_SECURE is set (rule 19 §3 "그 프로젝트에 붙은 기록이 보이는 기록함"). A masked label alone (`R-18`) does not trigger it (LIB-26 `ED`), but the record **text** next to it does.
- Hidden records and hidden-project records are excluded from search results (LRC-40).

#### LRC-23 · 연결 대상이 지워졌을 때 (deleted, moved and example targets)
**Priority** P1 · **needsExternal** no · **Boards** `RCDeleteImpact` (`레코더 칩 1`), `Trash` · **Sources** rule 23 §6 (지움 table; 다음 블록으로 옮기기 안 빌림; 씬 옮기기; 예시 프로젝트; 휴지통 레코더 필터); rule 20 §2 table row `라이프 레코더 붙일 곳 칩`, §3 (지운 항목 row `레코더 1`), §5 (되살리기 counts)

Links are never deleted because their target was deleted, and they never move to a "next" target.

| Target | While in trash | Restored | After permanent delete (30 days) |
| --- | --- | --- | --- |
| Scene (`1-7 씬 3`) | dotted chip `1-7 씬 3 · 휴지통` | as before | plain text (not a chip) `지워진 씬 · 1-7 씬 3`; the **project link stays** |
| Project (「잿빛 새벽의 등불」) | `잿빛 새벽의 등불 · 휴지통` | as before | plain text name; in `프로젝트 ▾` the record falls under `연결 없음` |
| Character / place / term / faction (rule 20) | dotted chip (name + dotted underline + `◇`); tap → card `지운 인물 · 노아` with buttons `칩 다시 고르기` · `칩 빼기` · `이대로 두기` | re-linked | chip `영구 삭제 · 노아` |
| Chapter / episode / page | `<label> · 휴지통` dotted [PROPOSED: same as scene] | as before | plain text `<label>` [PROPOSED: copy for 장/에피소드/페이지 not in sources, `TODO_COPY_LRC_08`] |

- `칩 다시 고르기` → link picker (LRC-20) replacing the target; `칩 빼기` → removes the link; `이대로 두기` → keeps it dotted.
- **`@` runs in record text** (rule 20 §2 row `@연결 (원고 · 페이지 · 레코더 · 글 메모)`): a run pointing at a deleted character / place / term / faction shows dotted + `◇`; tap → card `지운 인물 · 노아` with `다른 항목에 잇기` · `글로 풀기` (text stays, link removed) · `되살리기` · `이대로 두기`; restored → re-linked; after purge the run shows as chip `영구 삭제 · 노아` and `글로 풀기` remains possible. The matching AT-origin chip mirrors the run's state and offers **no** `칩 빼기` (rule 23 §5: an `@` link leaves only by editing the text); `글로 풀기` removes both the link run and the chip.
- Hidden targets keep their mask while deleted (LRC-22; rule 20 edge row).
- Counts for other areas: deleting a character with linked records shows `레코더 칩 <n>` in the impact sheet (rule 20 §4; `RCDeleteImpact` `레코더 칩 1`), and the check center row `◇ 지운 인물 · 노아 · 3일 남음 · 대사 14 · 씬 등장 2 · @ 3 · 레코더 1`. This area provides `recorderLinkCount(targetKey)` = number of non-trashed records with a **non-`@`** link to the target (`레코더 칩`), and `recorderAtLinkCount(targetKey)` = number of `@` runs in non-trashed records pointing at it, which the cast area adds to `@연결` (rule 20 lists recorder text under `@연결`; 30-cast's `참조 20 = 14 + 2 + 3 + 1`) [PROPOSED split; hidden records counted, since a count reveals no content].
- **Scene moved** to another episode: the link follows the key; label recomputes (`1-7 씬 3` → `1-8 씬 1`).
- **Onboarding example project** (deleted by `온보딩 끝내기`, LIB-18): records created inside it stay in the library; their links to the example are removed (the chip `예시 · 끝나면 연결 없어짐` warned about this).
- Links never go to the trash; the trash `레코더` filter shows records only (LRC-43).
- Rule 20 restore toast counts (`노아 되살림 · 다시 이어짐 · …`) are owned by cast; recorder links re-attach by key.

#### LRC-24 · 대상 쪽에서 보이는 줄 (`레코더 · 정리 안 됨 n ›`)
**Priority** P1 · **needsExternal** no · **Boards** `CSheetMods`, `WideMods`, `Profile` · **Sources** rule 23 §2 (연결이 하는 일 (3)), §7 last row; `scr_core.py p_mods` (`씬 메모` card right `레코더 · 정리 안 됨 1 ›`); `gen_nm.py` profile (`Ⅰ 개요` right `레코더 · 정리 안 됨 1 ›`)

- This area provides `unsortedLinkedCount(targetKey)` = number of records in state `정리 안 됨`, not in trash, with a link (any origin) to exactly that target, **excluding locked hidden records** (rule 23 §2 "잠긴 숨긴 기록은 이 숫자에 넣지 않는다"). Records linked only to the project do not count for a scene or character.
- The script `모듈` tab card `씬 메모` (SCR) and the character profile `개요` header (CST-49) show `레코더 · 정리 안 됨 <n> ›` in `acc` 12 px when n ≥ 1, nothing when 0.
- Tap → the inbox opens filtered to that target (LRC-27 target filter).
- Same for place/term/faction cards and pages: rule 23 names only the scene memo and profile; `ED` not shown elsewhere until drawn.

### 2.7 기록함 (inbox)

#### LRC-25 · 기록함 목록 (inbox list)
**Priority** P0 · **needsExternal** no · **Boards** `LrInbox`, `LrHide` (background), `TwoLr`, `ColLr`, `Library` (entry row) · **Sources** MAIN 라이프 레코더 (기록은 프로젝트 밖의 별도 라이브러리(기록함)에 쌓인다); rule 23 §3 (정렬은 최근 순 하나), §4 (기록함 행은 연결과 상관없이 같은 버튼 셋), §7 row 기록함; `scr_lr.py inbox()`

Entry: 서재 row `라이프 레코더` (LIB-01), target lines (LRC-24), checklist / empty-cast links (LRC-47), the copy source marks (LRC-32), the trash (restored record).

Layout (cover):
- Header (DS `GlassBars` top): small line `라이브러리 · 프로젝트 밖`, title `라이프 레코더`, back icon on the left, right a filter icon button (purpose §6 Q11; `ED` opens a text search over the visible records, see LRC-40 for exclusion).
- Filter row (DS `Tag` chips, height 30, selected = `pri` fill): `전체 12` · `정리 안 됨 3` · `넣음` · `끝냄` · `음성` · `글` · `프로젝트 ▾` (LRC-27). When hidden records exist, the locked group row (LRC-40) sits under the filter row.
- List, **newest first only** (by `createdAt`, descending). No other sort; `정리 안 됨` is never floated up automatically.
- Row (top border `line`, padding 14 0):
  1. Header line: kind label 12/16 weight 600 (`음성 0:24` for voice with duration, `글` for text); meta `small` (`오늘 08:41`); right-aligned state label 12/16 600 in `acc`: `정리 안 됨` (drawn) / `넣음` / `끝냄` [PROPOSED for the last two].
  2. Body: record text, manuscript body style 16/27, clamped to 3 lines with `…` [PROPOSED].
  3. Voice records: player row (LRC-14), margin-top 8.
  4. Link row (only when the record has links): link icon 14 `ink2` + selected tags with link labels (`1-7 씬 3` · `하르트`), margin-top 8 (LRC-21, LRC-22).
  5. Action row (margin-top 10, tags, gap 6): `원고에 넣기` · `카드에 넣기` · `페이지로 만들기` on **every** row; rows with **no link** also show `+ 연결` after them (as drawn; §6 Q6); then a `⋯` icon button opening LRC-29 (`aria-label` copy: not in sources, `TODO_COPY_LRC_09`; the button is required because the ⋯ menu must be reachable without hidden gestures).
- Bottom bar (glass, 140 high): ghost button with text icon `글로 쓰기` → LRC-15; primary `말로 기록` with mic icon (flex 1, 56 high, radius 28, 15 px) → LRC-07. Entry context = inbox (LRC-19 last row).
- Tap on a row's body → the record screen (LRC-28) on cover; selects it in the right pane on inner (LRC-31).
- Labels:
  - Date meta: same local day → `오늘 HH:mm`; previous day → `어제 HH:mm`; earlier → `M월 d일` (`11월 18일`); other year → copy: not in sources (`TODO_COPY_LRC_10`; `ED` `yyyy년 M월 d일`).
  - Duration: `음성 m:ss` (`음성 0:24`, `음성 1:02`); ≥ 1 h `음성 h:mm:ss` [PROPOSED].
- Rule 21: record text never shows term highlights, `▲` or `후보`.
- Example rows (board, rule 23 §3 table):

```
음성 0:24 · 오늘 08:41                                    정리 안 됨
하르트가 등불을 싫어하는 이유. 대정전 때 누군가를 잃었다는 설정은 어떨까. 그게 세라의 오빠였다면?
▶ ▮▮▮▮▮▮▮▮▮▮▮▮▯▯▯▯▯▯▯▯
[link] 1-7 씬 3 · 하르트
원고에 넣기 · 카드에 넣기 · 페이지로 만들기 · ⋯

글 · 어제 23:10                                           정리 안 됨
오웬은 사실 결사 출신? 탈퇴 이유가 세라와 관련
[link] 오웬 · 세라
원고에 넣기 · 카드에 넣기 · 페이지로 만들기 · ⋯

음성 1:02 · 어제 08:15                                    정리 안 됨
2장 첫 장면은 비 오는 상층 관문에서 시작. 리엔이 처음으로 위를 올려다보는…
▶ ▮▮▮▮▮▮▮▮▮▮▮▮▯▯▯▯▯▯▯▯
원고에 넣기 · 카드에 넣기 · 페이지로 만들기 · + 연결 · ⋯
```
(`[link]` stands for the 14 px link icon; `▶ ▮▯` for the player; `⋯` is the [PROPOSED] menu button.) No row ever shows a content-guessed button such as the removed `2장 메모로`. The 오웬 record shows `정리 안 됨` (rule 23 §3 corrected the board, which had no state).

#### LRC-26 · 상태 (`정리 안 됨 · 넣음 · 끝냄`)
**Priority** P0 · **needsExternal** no · **Boards** `LrInbox`, `LrLocked`, `LrUnlocked` · **Sources** rule 23 §3 (상태 table, 규칙: 상태는 ‘넣었는가’ 하나만 본다), §6 (넣은 곳 모두 영구 삭제 → 정리 안 됨)

| State | Meaning | Becomes this when |
| --- | --- | --- |
| `정리 안 됨` | not put anywhere yet | new record; all 넣은 곳 permanently deleted; `⋯ › 다시 정리 안 됨` from `끝냄`; undo of the first insertion |
| `넣음` | put into a manuscript, card, page (or memo field / new character) at least once; has a 넣은 곳 list | the moment an insertion completes (from `정리 안 됨` or `끝냄`) |
| `끝냄` | finished without inserting | `⋯ › 끝냄으로 표시` (only offered in `정리 안 됨`), with `되돌리기` in the toast |

- Links never change the state.
- Undoing an insertion (toast `되돌리기`) returns the record to the state it had before that insertion.
- A 넣은 곳 whose target is in the trash still counts: the record stays `넣음`.
- `ED` implementation (§3): state is **derived** = `넣음` if any placement is not permanently gone; else `끝냄` if `doneMarked`; else `정리 안 됨`. Completing an insertion clears `doneMarked` [PROPOSED].
- Toast after `끝냄으로 표시`: copy: not in sources (`TODO_COPY_LRC_11`) + `되돌리기`.

#### LRC-27 · 필터와 `프로젝트 ▾` (filters)
**Priority** P0 · **needsExternal** no · **Boards** `LrInbox`, `LrLocked`, `LrUnlocked`, `LrHide` · **Sources** rule 23 §3 (기록함 필터, 숫자는 상태 칩에만, 서재 줄 그대로), §2 (탭하면 기록함이 그 대상으로 걸러져 열린다), §4 table (만든 직후 · 빈 상태 · 인물)

- **Status chips**, single choice: `전체` · `정리 안 됨` · `넣음` · `끝냄`; default `전체`. Counts are shown on `전체 <n>` and `정리 안 됨 <u>` as drawn; `넣음` and `끝냄` carry no count (§6 Q15). Counts are over records **not in trash**, after the kind/project/target filters [PROPOSED], and **exclude locked hidden records** (board `LrLocked`: `전체 9 · 정리 안 됨 2`; `LrUnlocked`: `전체 12 · 정리 안 됨 3`).
- **Kind chips** `음성` · `글`: independent toggles, at most one on at a time [PROPOSED]; none on = both kinds. Never carry numbers.
- **`프로젝트 ▾`** (right end): menu `모든 프로젝트` (default) · each project in 서재 order · `연결 없음`. Choosing a project shows records linked to it or to anything inside it (implied project link). `연결 없음` shows records with no live project link (incl. records whose only project was permanently deleted). The chip then reads `<project label> ▾` [PROPOSED]. Masking (LRC-22) applies to project labels in the menu: locked hidden projects are **not listed**; projects with `라이프 레코더 연결 가리기` show `R-18` (§6 Q17).
- **Target filter** [PROPOSED chip form]: when the inbox opens from a target (`레코더 · 정리 안 됨 n ›` on a scene or character), the list is limited to records linked to that target and status `정리 안 됨` is preselected; a removable chip `<target label> ×` appears first in the filter row; × clears the target filter (project filter returns to `모든 프로젝트`).
- Preset openings:

| Opened from | Preset |
| --- | --- |
| 서재 row `라이프 레코더` | no filter (`전체`) |
| `레코더 · 정리 안 됨 n ›` (scene memo card, profile) | target = that scene / character, status `정리 안 됨` |
| checklist `연결된 기록 2개 정리하기` (LIB-13) | project = that project (rule 23 §4 "이 프로젝트로 거름"); status `정리 안 됨` [PROPOSED] |
| empty cast `라이프 레코더에서` | project = that project, status `정리 안 됨` (rule 23 §4) |

- Filter state lives for the app session (`ED`), is not stored in files, and is reset when the inbox is opened from 서재.
- Filtered-empty list: copy: not in sources (`TODO_COPY_LRC_12`); the filter row stays visible. `EmptyLr` (LRC-30) is only for a library with zero records.

#### LRC-28 · 기록 화면 (record screen)
**Priority** P1 · **needsExternal** no · **Boards** `TwoLr` (right pane reuses `LrProcess`); otherwise not drawn · **Sources** rule 23 §4 (◇ 넣은 뒤 고침 · 기록 화면에만; 출처 표시를 누르면 기록이 열리고 음성은 거기서 재생), §6 (◇ 넣은 곳 지워짐 · 기록 화면에만); `LrProcess` note `글은 나중에 고치기 가능`

The screen of one record (§6 Q12: referenced by rule 23, drawn only as the `LrProcess` layout in `TwoLr`). Layout [PROPOSED, built from `LrProcess` parts]:
- Header: kind + duration (`음성 0:24`), date (`오늘 08:41`), back (cover), `⋯` (LRC-29).
- Text: manuscript body style; **editable in place** (tap to edit, autosave on every change, `@` works as in LRC-17 for typed text). Every committed edit increments the record's text revision (used by LRC-39).
- Voice: player (LRC-14).
- Link row with `+ 연결` (LRC-18 … LRC-23).
- `넣은 곳` list (rule 23 vocabulary `넣은 곳 목록`; section label copy: `넣은 곳` [PROPOSED reuse of rule wording]): one row per placement, newest first, label per kind:
  - block → `<scene label> · #<number>` (`1-7 씬 3 · #062`), tap → script at that block;
  - card paragraph → `<card name> · <tab>` [PROPOSED];
  - page → `<page name>` [PROPOSED];
  - memo field → `<target label> · 씬 메모` / description line [PROPOSED];
  - new character → `<character name>` [PROPOSED].
  - In trash: label + ` · 휴지통` (rule 23 table "넣은 곳 목록에 휴지통"); permanently deleted: plain text in `ink3`, not tappable.
- `◇ 넣은 뒤 고침` next to every placement whose copy was made from an older text revision (LRC-39).
- `◇ 넣은 곳 지워짐` under the list when every placement is permanently deleted (state is back to `정리 안 됨`).
- Actions: `원고에 넣기` · `카드에 넣기` · `페이지로 만들기` (same as the row).
- Hidden record: reachable only while hidden records are open (LRC-41); otherwise any deep link (source mark tap) first runs the biometric prompt `숨긴 기록 보기` [PROPOSED].
- Inner: shown in the right pane (LRC-31).

#### LRC-29 · 기록 ⋯ 메뉴 (record menu)
**Priority** P0 · **needsExternal** no · **Boards** `LrHide` · **Sources** `scr_lr.py hidemenu()`; MAIN 숨기기 (기록마다 ⋯ → 숨기기); rule 23 §3 (⋯ › 끝냄 · ⋯ › 다시 정리 안 됨), §4 (⋯ › 메모 칸에 넣기 · ⋯ › 인물로 만들기), §7 row 숨기기

Bottom sheet (cover) / popover (inner):
- Preview card (margin-top 14, padding 12 14, radius 14, `tint`): `small` line `음성 0:24 · 오늘 08:41`; text 15/24 manuscript body style, one-to-two-line excerpt `하르트가 등불을 싫어하는 이유. 대정전 때 누군가를 잃었다는…`.
- Options (each: icon 20 `ink2`, title 16/22 weight 700, sub `small`; rows separated by `line`):

| Option | Sub (copy) | Shown when | Action |
| --- | --- | --- | --- |
| `원고에 넣기` | `위치 고르기 · 연결된 씬 · 마지막으로 쓰던 곳` | always | LRC-33 |
| `카드에 넣기` | `고른 탭 끝에 작가 메모` | always | LRC-35 |
| `페이지로 만들기` | — (copy: not in sources) | always | LRC-36 |
| `메모 칸에 넣기` | — | always | LRC-37 |
| `인물로 만들기` | — | always | LRC-38 |
| `끝냄으로 표시` | — | state `정리 안 됨` | LRC-26 |
| `다시 정리 안 됨` | — | state `끝냄` | LRC-26 |
| `숨기기` | `지문 · PIN으로 열림` | record not hidden | LRC-40 |
| `숨김 풀기` | — | record hidden (group open) | LRC-41 |
| `지우기` | `휴지통 · 30일 뒤 삭제` | always | LRC-43 |

- Row order: as in the table [PROPOSED; the board draws 원고에 넣기 · 카드에 넣기 · 숨기기 · 지우기 in this relative order]. The board's highlighted `숨기기` row (`sel` background + 2 px `acc` left bar) illustrates the action being chosen; nothing is preselected in the app.
- The menu text says `넣기`, never `붙이기` (rule 23 §7 row 숨기기: old `인물 · 용어에 붙이기 · 고른 카드의 메모로` replaced).

#### LRC-30 · 빈 기록함 (empty inbox)
**Priority** P1 · **needsExternal** no · **Boards** `EmptyLr`, `OnbEmpty` (서재 row `기록 0`) · **Sources** MAIN 상태 화면 (기록함 비어 있음); `scr_states.py e_lr()`

**(0.2.0)** Sub becomes `TODO_COPY_LRC_EMPTY_SUB` (LRC-48).

- When the library has zero records (trash not counted): header as LRC-25 without the right icon; chip `전체 0`; outlined block (margin-top 18, padding 26 20, radius 20, inset `edge`): title `기록 없음` (22/30, 800), sub `명령 칸 마이크 · 홈 위젯 · 빠른 설정` (13 px), buttons `말로 기록` (primary, flex 1) · `글로` (ghost, flex 1). No bottom bar.
- `말로 기록` → LRC-07; `글로` → LRC-15 (entry context inbox, no filter → no pre-filled link).
- 서재 row then reads `기록 0` (LIB-01).

#### LRC-31 · 펼친 화면 배치 (unfolded layouts)
**Priority** P1 · **needsExternal** no · **Boards** `TwoLr`, `ColLr`, `LayoutSettings` · **Sources** MAIN 화면 · 디자인 (목록 모듈 … 기록함 … 두 배치); concept memory (unfolded layout); `scr_inner.py` PAIRS `('Lr', 'LrInbox', 'LrProcess', '기록함 | 기록')`

- Chosen in 설정 › 화면 › 펼친 화면 배치 (LIB-66); applies to the inbox like every list module; the cover layout is unchanged.
- **목록 + 상세** (default, `TwoLr`): left pane 390 = the inbox (LRC-25) incl. its bottom bar; 1 px `line` divider; right pane = the selected record (LRC-28) — the board shows the just-recorded record in the `정리 중` layout (LRC-13). On opening with nothing selected, the newest visible record is selected [PROPOSED]. A new recording started from the inbox shows `녹음 중` / `정리 중` in the right pane [PROPOSED].
- **가운데 한 칸** (`ColLr`): the inbox in a 560-wide centred column on `bg`, side margins `raise` at 50 % opacity, 1 px `line` edges; a tapped record opens in the same column.
- Fold/unfold (MFoldList rule): the selected record opens in the right pane on unfold; on fold the last-touched pane stays; recording continues (LRC-04, LRC-09).

### 2.8 넣기 (copy a record out)

#### LRC-32 · 넣기 공통 규칙 (common rules for every insertion)
**Priority** P0 · **needsExternal** no · **Boards** `LrInbox`, `LrHide` · **Sources** rule 23 §4 (규칙, 같이 다루는 경우, 버린 안), §5 (@ in copies), §6 (지움 table); 10-script SCR-59; 50-canvas CNV-15; 30-cast CST-49

- **Copy, not move.** Every 넣기 creates new content holding a copy of the record's **current** text; the record stays in the library unchanged.
- **Two-way pointers.** The created content stores its source (record key + record `createdAt`); the record gains a placement (`넣은 곳`). Both by permanent key.
- **Link added.** If the target is not yet among the record's links, one link (origin `넣기`) is added (rule 23 §4: 넣은 대상이 아직 연결에 없으면 연결이 하나 더해진다): block → its scene; card paragraph → the card; page → the new page; memo field → that scene / episode / chapter; new character → that character.
- **State** becomes `넣음` when the insertion completes (LRC-26).
- **`@` runs in the copy:** a link run whose target is in the destination project stays an `@` link; a run pointing into another project becomes plain text with the same characters (`@세라` → `세라`).
- **Never guess the kind.** Manuscript copies are always one `⌁ 작가 메모` block (game output excluded); cards get a `⌁ 작가 메모` paragraph; nothing is turned into dialogue or narration by the app. Changing kind afterwards is normal editing in the target area.
- **No sync.** Later edits on either side do not propagate (LRC-39).
- **Repeatable.** The same record may be inserted any number of times, also into several projects; each insertion adds a placement (and a link when needed).
- **Protected targets.** Choosing a **locked hidden** project or an **R-18** project as the destination runs the system biometric prompt at that moment (rule 23 §4 "프로젝트를 고르는 순간 시스템 생체 인증"); cancel → the choice is not made. The read-only example project and projects in the trash are not offered [PROPOSED].
- **Undo.** Every insertion shows a toast with `되돌리기` (LIB-40 host); undo deletes the created content (no trash entry), removes the placement and the link this insertion added, and restores the previous state.
- **Source mark** on copies (rendered by the owning area): `⌁ 작가 메모 · 레코더 11월 22일 08:41` (script block; date `M월 d일 HH:mm` of the record's `createdAt`); record in trash → `레코더 · 휴지통`; record permanently deleted → `지워진 기록`. Tapping the mark opens the record screen (LRC-28), where audio plays; for a trashed record it opens the trash filtered to `레코더` [PROPOSED].
- The audio file is never copied.

#### LRC-33 · 원고에 넣기 · 위치 시트 (insert into the manuscript)
**Priority** P0 · **needsExternal** no · **Boards** `LrInbox`, `LrHide` (sub `위치 고르기 · 연결된 씬 · 마지막으로 쓰던 곳`); sheet itself not drawn · **Sources** rule 23 §4 (원고에 넣기의 위치 시트 code block and bullets); 10-script SCR-59; rule 12 (block numbers)

- Sheet title line: `원고에 넣기 · <project> ▾`. The `▾` switches the destination project (LRC-32 protected targets). Initial project: the record's only linked project if it has exactly one (explicit or implied); otherwise no project, and the project menu opens first [PROPOSED].
- Rows are **facts only**; **no row is preselected**:

```
원고에 넣기 · 잿빛 새벽의 등불 ▾
연결된 씬 · 1-7 씬 3 · 맨 끝
마지막으로 쓰던 곳 · 1-7 씬 3 · #044 뒤
씬 고르기 ›
이미 넣음 · 1-7 씬 3 · #062        (only from the second time)
```

  - `연결된 씬 · <scene> · 맨 끝` — one row per scene of this project linked to the record (not in trash, not a locked hidden scene) [PROPOSED: one row each when several].
  - `마지막으로 쓰던 곳 · <scene> · #<n> 뒤` — the project's last caret position (block key + offset, stored by the script, SCR-59). The number is shown only; the position is stored by key. Omitted when the project has none or that block no longer exists [PROPOSED].
  - `씬 고르기 ›` — opens the project's structure (장 › 에피소드 › 씬); choosing a scene inserts at its end [PROPOSED].
  - `이미 넣음 · <scene> · #<n>` — one row per existing block placement of this record in this project, shown only when there is at least one; informational; tap → opens the script at that block [PROPOSED].
- Tapping a target row inserts at once: **one `⌁ 작가 메모` block** with the copied text, at the end of the scene (after its last block, before the next 씬 머리) or directly after block `#<n>`. Its number is the episode's next number in creation order (rule 12): inserted after `#044` it gets `#062` when `#061` is the highest number ever issued in 1-7.
- Toast: `1-7 씬 3 · #062 작가 메모 넣음 · 되돌리기 · 가 보기` (`가 보기` → the script scrolled to the new block; §6 Q4 on one vs two actions).
- Projects with format `설정만` (no manuscript) are not offered [PROPOSED].
- The record's text is copied as runs (LRC-32 `@` rule); an empty-text record cannot be inserted (`원고에 넣기` disabled) [PROPOSED].

#### LRC-34 · 여기 넣기 (insert at the recording's start position)
**Priority** P1 · **needsExternal** no · **Boards** `LrProcess` (`여기 넣기 · #044 뒤`) · **Sources** rule 23 §4 bullet (스크립트에서 녹음을 시작했으면 …), §7 row 정리 중; 10-script SCR-59

- Shown on `정리 중` (LRC-13) **only** when the recording was started from the script's command bar; label `여기 넣기 · #<n> 뒤` where `#<n>` is the block holding the caret when the mic was tapped.
- Nothing is inserted until it is tapped. Tap → LRC-33 insertion directly after that block (by key), same toast, record state `넣음`, recorder surface closes and the script shows the new block.
- If that block was deleted in the meantime, the button is not shown [PROPOSED].
- Not shown for recordings started anywhere else, nor after `이어서 말하기` from a non-script entry.

#### LRC-35 · 카드에 넣기 (insert into a card)
**Priority** P1 · **needsExternal** no · **Boards** `LrInbox`, `LrHide` (`카드에 넣기` · `고른 탭 끝에 작가 메모`) · **Sources** rule 23 §4 table row 카드에 넣기; 30-cast CST-49; character fields memo (no fixed `메모` field); rule 23 §7 (old `인물에 붙이기` replaced)

- Sheet steps: project (LRC-32 protected targets) → card (인물 · 장소 · 용어 · 세력; search by name / 초성; locked hidden items not listed) → tab (character: `개요` · `말투` · `비밀` · each `긴 글` field; place / term / faction: the card's long-text sections as defined by the world area, §6 Q19).
- Result: a `⌁ 작가 메모` paragraph with the copied text appended at the **end** of the chosen tab/field, with source mark. The record gains a placement; the card gains a link if not linked.
- Toast: copy: not in sources (`TODO_COPY_LRC_13`) + `되돌리기`.
- There is no "card memo" fixed field; never invent one (rule 23 §1 audit).

#### LRC-36 · 페이지로 만들기 (make a page)
**Priority** P1 · **needsExternal** no · **Boards** `LrInbox` · **Sources** rule 23 §4 table row 페이지로 만들기 (새 페이지 하나 · 이름은 첫 문장 앞부분(시트에서 고치기) · 프로젝트 · 이름); 50-canvas CNV-15

- Sheet: project (LRC-32) and `이름` field prefilled with the start of the record's first sentence, editable. `ED` prefill = first sentence (up to the first `.`, `?`, `!` or line break), trimmed, cut to 30 characters; e.g. R1 → `하르트가 등불을 싫어하는 이유`. Confirm button copy: not in sources (`TODO_COPY_LRC_14`).
- Result: one new page in that project whose body is the copied text (CNV-15 decides paragraph vs memo block). Placement + link to the new page.
- Toast: copy: not in sources (`TODO_COPY_LRC_15`) + `되돌리기`.

#### LRC-37 · ⋯ › 메모 칸에 넣기 (append to a memo field)
**Priority** P1 · **needsExternal** no · **Boards** — · **Sources** rule 23 §4 table row ⋯ › 메모 칸에 넣기 (씬 메모 칸 끝 한 줄, 장 · 에피소드는 설명 줄 끝; 프로젝트 → 장 · 에피소드 · 씬), bullet (2장에 쓰려면 `+ 연결`로 2장을 걸거나 ⋯ › 메모 칸에 넣기 → 2장)

- Sheet: project → structure tree (장 · 에피소드 · 씬).
- Scene → the copied text is appended as **one line** at the end of the scene's `씬 메모` (the memo card on the script `모듈` tab). Chapter / episode → appended at the end of its description line (story-structure area).
- Placement kind memo field; link to that scene / episode / chapter if missing; state `넣음`.
- Toast: copy: not in sources (`TODO_COPY_LRC_16`) + `되돌리기`.
- Multi-line record text is joined into one line with spaces [PROPOSED].

#### LRC-38 · ⋯ › 인물로 만들기 (make a character)
**Priority** P1 · **needsExternal** no · **Boards** `EmptyCast` (`라이프 레코더에서`) · **Sources** rule 23 §4 table row ⋯ › 인물로 만들기 (새 인물 · 한 줄 소개 = 첫 문장 · 개요 끝에 ⌁ 메모; 프로젝트 · 이름), 같이 다루는 경우 (빈 상태 · 인물); 30-cast CST-49; rule 20 §6 (example: 시장 꼬마 merged into 노아)

- Sheet: project and `이름` (required, empty at start [PROPOSED]).
- Result (created by the cast area): a new character with fixed field `한 줄 소개` = the record's first sentence, and a `⌁ 작가 메모` paragraph with the full copied text at the end of `개요`. Placement (kind character) + link to the new character; state `넣음`.
- A duplicate character produced this way is fixed by the cast area's merge (rule 20 §6), not by the recorder.
- Toast: copy: not in sources (`TODO_COPY_LRC_17`) + `되돌리기`.

#### LRC-39 · 넣은 뒤 (after inserting: edits, deleted copies)
**Priority** P1 · **needsExternal** no · **Boards** — · **Sources** rule 23 §4 같이 다루는 경우 (넣은 뒤 기록을 고침 · 복사본을 고침), §6 지움 table rows 넣은 블록 · 카드 문단 · 페이지 and 기록 자체

| Event | Copy | Record |
| --- | --- | --- |
| Record text edited after an insertion | unchanged | record screen shows `◇ 넣은 뒤 고침` beside that 넣은 곳 (never in 점검 센터, never in the inbox row) |
| Copy edited | — | unchanged, no mark |
| Copy (block / card paragraph / page) moved to trash | — | its 넣은 곳 row shows `휴지통`; state stays `넣음` |
| Copy restored | — | row back to normal |
| Copy permanently deleted | — | its row stays as plain text; when **all** placements are gone → state `정리 안 됨` + `◇ 넣은 곳 지워짐` (record screen only) |
| Record moved to trash | source mark `레코더 · 휴지통`, text unchanged | — |
| Record restored | source mark normal | as before |
| Record permanently deleted | source mark `지워진 기록`, text unchanged | — |

- `◇ 넣은 뒤 고침` is computed per placement: record text revision > the revision copied.
- Undo of an insertion (LRC-32) is not a deletion: no trash entry, no `◇`.

### 2.9 Hiding and deleting records

#### LRC-40 · 기록 숨기기 · 잠긴 묶음 (hide records, locked group)
**Priority** P1 · **needsExternal** no · **Boards** `LrHide`, `LrLocked`, DS `LockRow` · **Sources** MAIN 숨기기 (기록마다 ⋯ → 숨기기 · 잠긴 한 줄 · 내용 · 날짜 · 길이 가림 · 검색에서 빠진다); concept memory (LIFE RECORDER HIDE); DS README (항목 · 레코더 기록도 가능); `scr_lr.py locked_group()`

- `⋯ › 숨기기` hides the record at once (hiding needs no authentication [PROPOSED]). The row leaves the list and the locked group count grows.
- **Locked group row** (under the filter row; DS `LockRow`): margin-top 14, padding 14 16, radius 16, `tint`, inset 1 px `line`; lock icon in a 40×40 `raise` tile (radius 12); title `숨긴 기록 <n>` (16/22, 700); sub `내용 · 날짜 · 길이 가림 · 검색 제외`; button `지문으로 보기` (primary, 40 high, 12 px). `n` = all hidden records not in trash, regardless of filters [PROPOSED].
- While locked, hidden records: are not listed; are not in the status counts; show no content, date, length or links anywhere; are excluded from search and command results and from the inbox text search; are excluded from `레코더 · 정리 안 됨 n` lines (LRC-24); are not offered in the new-project `라이프 레코더 기록 연결` list [PROPOSED]; are not exported by default (LRC-45).
- Hidden records keep their links and placements; copies made from them keep their text and source mark (the mark does not reveal the record).

#### LRC-41 · 지문으로 열기 · 다시 잠그기 · 숨김 풀기 (open, re-lock, unhide)
**Priority** P1 · **needsExternal** no (system BiometricPrompt, as LIB-32) · **Boards** `LrUnlock`, `LrUnlocked`, `MUnlock`, `ErrBio` · **Sources** concept memory (system biometric prompt, fallback device PIN; visible only while open, re-locks on leaving the app or `다시 잠그기`); `scr_lr.py unlock()`, `unlocked()`; `MotionSpec` (`숨김 풀기 · 흐림 7 px → 0 · 320 ms`); `MUnlock` (`다시 잠글 때 반대로`)

- `지문으로 보기` → system biometric prompt (drawn on `LrUnlock`: small `시스템 생체 인증`, title `숨긴 기록 보기`, `지문 센서 터치`, fingerprint glyph, buttons `취소` · `PIN 사용`). Use the shared helper of LIB-32 (device credential fallback); failures per LIB-37 (`ErrBio`).
- Success → the group opens in place (`LrUnlocked`): a box (`sel` background, inset `line`, radius 16) with a header line: unlock icon (`acc`) · `숨긴 기록 <n> · 열림` (13/18, 600) · right `앱을 떠나면 잠김` (`small`); inside, the hidden records as rows with kind, date, text and right action `숨김 풀기`; header right of the screen becomes `다시 잠그기` (ghost, 36 high, lock icon). Status counts now include hidden records (`전체 12 · 정리 안 됨 3`). Motion: blur 7 px → 0 over 320 ms on the revealed rows; reverse on re-lock; reduce motion → 120 ms fade.
- `숨김 풀기` → the record becomes normal and moves into the main list.
- Tapping a hidden row while open → record screen with all actions.
- **Re-lock**: when the app goes to the background (right text `앱을 떠나면 잠김`) and on `다시 잠그기`. `ED` use the global re-lock policy of LIB-33 for records too; the right text for the other policies is copy: not in sources (`TODO_COPY_LRC_18`; §6 Q13).
- One authentication opens all hidden records; it does not open hidden projects or hidden items, and opening those does not open records [PROPOSED: separate gates, like app lock vs hidden projects].
- Cancel → nothing changes.

#### LRC-42 · 숨기기 줄 기본값 (hide by default for hidden / R-18 targets)
**Priority** P1 · **needsExternal** no · **Boards** — (row not drawn) · **Sources** rule 23 §6 (새 기본값: 연결 대상이 숨긴 프로젝트나 R-18이면 정리 중 화면에 `숨기기` 줄이 켜진 채로 보인다 · 끄고 저장할 수 있다); rule 23 tab header (confirmed); memory rules-19-23 ("Hidden/R-18 targets → 숨기기 row pre-on")

- On `정리 중` (LRC-13), if any link (explicit or implied) points into a **hidden** project or an **R-18** project, a `숨기기` row with a switch appears, **on**. Turning it off and leaving saves the record unhidden. It is a visible toggle, not a silent decision.
- The row appears or disappears live as links are added/removed; when it first appears it is on; if the triggering link is removed the row disappears and the record will not be hidden [PROPOSED].
- On `기록함에 두기` (or `여기 넣기`) with the switch on → the record is saved hidden (LRC-40).
- Text memo: same row (`ED`, §6 Q9).

#### LRC-43 · 기록 지우기 (delete records)
**Priority** P1 · **needsExternal** no · **Boards** `LrHide` (`지우기` · `휴지통 · 30일 뒤 삭제`), `Trash` (`음성 0:12` · `라이프 레코더` · `1일 남음`) · **Sources** rule 23 §6 (지움: 휴지통 30일, 되살리면 다시 붙음, 아무것도 조용히 지우지 않음; 휴지통 ‘레코더’ 필터는 기록 자체만), 버린 안 (대상과 함께 기록도 휴지통으로: 버림); vocabulary (지우기 = to trash, 삭제 = permanent); LIB-38/39

- `⋯ › 지우기` moves the record to the trash at once (no confirmation sheet: a record has no references to show) and shows a toast: copy: not in sources (`TODO_COPY_LRC_19`; vocabulary pattern `… 지움`) + `되돌리기`.
- Trash row (LIB-38 host, filter `레코더`): title = kind + length for voice (`음성 0:12`) or `글` for text [PROPOSED for text]; sub `라이프 레코더`; right `<d>일 남음`; tags `되살리기` · `영구 삭제`. Hidden record → title `숨긴 기록` and `되살리기` needs the biometric prompt [PROPOSED, by analogy with rule 19 `숨긴 인물`].
- While in trash: not in the inbox, counts, targets' lines, exports, search. Copies show `레코더 · 휴지통`.
- `되살리기` → back with all links, placements, hidden flag and state.
- Permanent delete (`영구 삭제`, `비우기`, or 30 days after `deletedAt`) → record, audio file, links and placements are erased; copies keep their text and show `지워진 기록`.
- Deleting a project or a target never moves records to the trash (rule 23 §6 버린 안); see LRC-23 for the link side.
- Only records go to the trash; links never do.

### 2.10 Settings and files

#### LRC-44 · 설정 · 라이프 레코더 (settings screen)
**Priority** P1 · **needsExternal** no · **Boards** `LrSettings`, `ReduceSettings` (shared glow row) · **Sources** MAIN 설정 (받아쓰기 방식, 잠금 화면 녹음, 원본 음성 보관, 녹음 중 빛 띠, 연결); rule 23 §7 row 설정 (`보던 곳 연결 · 시작한 곳을 칩으로 · 언제든 빼기`); `scr_decide.py lr_settings()`

**(0.2.0)** Row `잠금 화면에서 녹음` is hidden; its value is kept (LRC-48).

Screen: header small `설정`, title `라이프 레코더`; reached from 설정 (LIB-63 row, `ED`).

| Section | Row | Sub | Control | Default |
| --- | --- | --- | --- | --- |
| `받아쓰기` | `기기 안` | `오프라인 · 음성이 기기 밖으로 나가지 않음` | radio | selected |
| | `클라우드` | `정확도↑ · 음성 전송 · 인터넷 필요` | radio | — |
| `녹음` | `잠금 화면에서 녹음` | `새 녹음만 · 기록함은 잠금 해제 후` | switch | on |
| | `원본 음성 보관` | `글과 함께 · 30일 뒤 정리 가능` | switch | on |
| | `녹음 중 빛 띠` | `말소리 크기 표시` | switch | on (AppSettings `recorderGlow`, same value as `ReduceSettings` `라이프 레코더 빛`) |
| | `보던 곳 연결` | `시작한 곳을 칩으로 · 언제든 빼기` | switch | on |

- Effects: LRC-11/12, LRC-05, LRC-14, LRC-10, LRC-19.
- Turning `원본 음성 보관` off does not delete audio already kept [PROPOSED].

#### LRC-45 · 내보내기와 레코더 기록 (export contents)
**Priority** P1 · **needsExternal** no · **Boards** `Export` (row `레코더 기록 4` · `원본 음성 포함 · 숨긴 기록 1 제외`) · **Sources** rule 23 §6 내보내기 (확정: 연결된 기록만; 숨긴 기록 기본 제외 · 숨긴 프로젝트 암호 파일일 때만 포함; PDF · CSV · JSON 제외; 여러 프로젝트 · 되살릴 때 합침); 저장 파일 양식 §2 (`recorder.json`), §6 (레코더 기록 · 이 프로젝트에 붙인 것만 · 글은 항상, 원본 음성은 선택(기본 끔)); LIB-41/42/47

- `.civilight` of a project contains exactly the records **linked to that project** (explicit or implied) that are not in the trash, minus hidden records unless the project itself is hidden and the file is encrypted.
- Export-sheet row (LIB-41 draws it; this area supplies the numbers): `레코더 기록 <n>` · `원본 음성 포함 ▢` (checkbox, default off) · `숨긴 기록 <h> 제외`, where n = linked records not in trash (hidden included), h = hidden ones among them. With h = 0 the `숨긴 기록` part is omitted [PROPOSED]; when hidden records are included (hidden project, encrypted file) the part reads copy: not in sources (`TODO_COPY_LRC_20`).
- Text is always included; audio only with `원본 음성 포함`.
- `대본 PDF`, `대사 표 CSV`, `게임 엔진 JSON` never contain records. Inserted `⌁` memo blocks follow the display layers / `export:false` rules of their owners.
- A record linked to several projects goes into each project's file.
- On import / restore (LIB-52/59): a record whose key already exists is merged — the existing record is kept, missing links and placements from the file are added, nothing is removed or overwritten [PROPOSED: text conflicts keep the local text].
- `recorder.json` content per record: see §3.4.

#### LRC-46 · 레코더 라이브러리 파일 · 백업 (`.civilight-recorder`)
**Priority** P1 · **needsExternal** no (the Drive transport is LIB-57) · **Boards** — · **Sources** 저장 파일 양식 (이름 `.civilight-recorder`; 라이프 레코더 라이브러리: 프로젝트 밖이라 따로 파일로 내보내고 백업, 같은 묶음 구조와 버전 규칙; Drive 자동 백업: 레코더 파일 하나); LIB-48

- The whole library (all records not in trash, with all links and placements, audio per `원본 음성 보관`) is written as one `.civilight-recorder` package (mimetype `application/vnd.civilight.recorder`, LIB-48) for Drive backup and manual export.
- Restore merges by record key (LRC-45 rule).
- Hidden records in this file: `ED` encrypted with the 복구 암호 like hidden projects; without a 복구 암호 they are left out of the backup (§6 Q16).
- Manual export entry point: not drawn (§6 Q16); `ED` none in v1 besides backup.

#### LRC-47 · 다른 화면의 레코더 고리 (hooks used by other areas)
**Priority** P1 · **needsExternal** no · **Boards** `Library`, `OnbEmpty`, `NewStart`, `NewReady`, `EmptyCast`, `OnbVoice`, `HelpReplay`, `TourLr`, `RCDeleteImpact`, `CSheetMods`, `WideMods`, `Profile` · **Sources** rule 23 §4 table (시작 재료 · 만든 직후 · 빈 상태 · 인물), §7; LIB-01/12/13/15/19/21; CST-49

**(0.2.0)** `OnbVoice` `홈 화면 위젯 추가` is hidden; the rating-sheet hook has no caller (LRC-48, LIB-92).

This area exposes these functions/screens; the named areas draw them:

| Hook | Contract |
| --- | --- |
| 서재 row `라이프 레코더` · `기록 <n> · 정리 안 됨 <u>` (LIB-01) | `libraryCounts()` → n = records not in trash, u = those `정리 안 됨`. Board values `기록 12 · 정리 안 됨 3`, equal to the unlocked inbox; `ED` include hidden records (§6 Q14). Tap → inbox (no filter). |
| New project `라이프 레코더 기록 연결 · <t>개 중 <k>개 고름` (LIB-12) | `startMaterialCandidates()` → records `정리 안 됨`, not hidden-locked, newest first, rows `음성 0:24 · 오늘` / first text line; on `만들기`, `linkRecords(keys, newProject)` adds a project link (origin `시작 재료`) and nothing else — no content is inserted. |
| Checklist `연결된 기록 2개 정리하기` · `기록함` (LIB-13) | n = records linked to the project in `정리 안 됨`; tap → inbox preset (LRC-27). The old `프로젝트 메모` wording is gone. |
| `EmptyCast` `라이프 레코더에서` (CST) | opens the inbox with project + `정리 안 됨` (LRC-27); each record's `⋯ › 인물로 만들기` (LRC-38). |
| Scene memo card / profile `레코더 · 정리 안 됨 n ›` | `unsortedLinkedCount(targetKey)` (LRC-24). |
| Character delete impact `레코더 칩 <n>`; check row `레코더 <n>` (rule 20) | `recorderLinkCount(targetKey)` (non-`@` links) and `recorderAtLinkCount(targetKey)` (added to `@연결`) (LRC-23). |
| Onboarding `OnbVoice` | copy `라이프 레코더` · `프로젝트 밖의 따로 된 기록함`, `말로 기록 → 글로 변환 → 원고 · 카드에 넣기`; `마이크 허용` · `녹음할 때만 사용` (LRC-06); `홈 화면 위젯 추가` · `앱을 열지 않고 바로 녹음` (LRC-02). |
| Module tour `레코더 안내` 3 steps (LIB-19); `HelpReplay` row `라이프 레코더` · `3단계` | step 2/3 drawn (`TourLr`: target = link chip row on `LrRecord`, `연결` · `보던 곳 · 넣기 전까지 내용은 그대로`); steps 1 and 3 not drawn (LIB-19 owns). Tour starts on first entry to the inbox or first recording [PROPOSED]. |
| Rule 19 rating sheet row `레코더 연결 가리기` · `연결에 등급만 · 기록 <n> 바로 적용` (LIB-24) | n = records linked to that project (`linkedRecordCount(projectKey)`, hidden included). |
| Example project | `exampleProjectDeleted(projectKey)` removes links to it (LRC-23). |

---

## 3. Data

Naming: this area owns **`LrRecord`** (60-library calls it `LrRecord`; 10-script's relation table calls it `RecorderRecord` — same entity, `RecorderRecord` is an alias). All keys are device-made ULIDs (file format §4). All text is NFC. Times are stored as UTC `Instant` and shown in the device time zone.

### 3.1 Enums

| Enum | Values (UI word) |
| --- | --- |
| `RecordKind` | `VOICE` (`음성`), `TEXT` (`글`) |
| `RecordState` (derived, §3.3) | `UNSORTED` (`정리 안 됨`), `PUT` (`넣음`), `DONE` (`끝냄`) |
| `LinkTargetKind` | `PROJECT`, `CHAPTER`, `EPISODE`, `SCENE`, `CHARACTER`, `PLACE`, `TERM`, `FACTION`, `PAGE` (levels are named by the project's structure names) |
| `LinkOrigin` | `ENTRY` (보던 곳), `MANUAL` (`+ 연결`), `AT` (`@` in text), `INSERT` (added by 넣기), `START_MATERIAL` (new-project step 4) |
| `PlacementKind` | `SCRIPT_BLOCK`, `CARD_PARAGRAPH`, `PAGE`, `MEMO_FIELD`, `CHARACTER` |
| `PlacementStatus` (derived) | `ACTIVE`, `IN_TRASH` (`휴지통`), `GONE` (permanently deleted) |
| `LinkDisplay` (derived for rendering) | `NORMAL`, `MASK_RATING` (`R-18`), `HIDDEN_PROJECT` (`숨긴 프로젝트`), `HIDDEN_ITEM` (`숨긴 항목`), `TRASHED` (`<label> · 휴지통`, dotted), `DELETED_ITEM_TRASH` (rule 20 dotted chip + `◇`), `GONE` (plain text / `영구 삭제 · <name>`), `EXAMPLE` (`예시 · 끝나면 연결 없어짐`) |
| `TranscriptionMode` | `ON_DEVICE` (`기기 안`), `CLOUD` (`클라우드`) |
| `EntrySurface` | `SCRIPT`, `CHARACTER`, `PLACE`, `TERM`, `FACTION`, `PAGE`, `PROJECT_HOME`, `STORY`, `TIMELINE`, `MAP`, `OTHER_IN_PROJECT`, `LIBRARY`, `HOME_WIDGET`, `QS_TILE`, `LOCK_SCREEN`, `COVER_WIDGET`, `INBOX` |
| `StatusFilter` | `ALL` (`전체`), `UNSORTED` (`정리 안 됨`), `PUT` (`넣음`), `DONE` (`끝냄`) |
| `KindFilter` | `NONE`, `VOICE` (`음성`), `TEXT` (`글`) |
| `ProjectFilter` | `All` (`모든 프로젝트`), `Project(key)`, `NoLink` (`연결 없음`) |

### 3.2 Entities owned by this area

**`LrRecord`** (one row per record; library, outside projects)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `key` | Ulid | no | permanent key; never reused |
| `kind` | RecordKind | no | voice or text (a voice record stays voice even if typed on later) |
| `createdAt` | Instant | no | creation (= recording start) time; shown as `오늘 08:41`; copied into source marks (`레코더 11월 22일 08:41`) |
| `modifiedAt` | Instant | no | last change (file format §7) |
| `modifiedByDevice` | String | no | device id of the last change (file format §7) |
| `text` | List<TextRun> | no | runs as in the script (`Plain(text)`, `Link(itemKey, itemKind, projectKey, text)`); may be empty for an untranscribed voice record |
| `textRevision` | Int | no | starts at 1; +1 on every committed user edit after creation (dictation appends from `이어서 말하기` also +1) |
| `durationMs` | Long | yes | voice only; recorded time excluding pauses |
| `audio` | LrAudio | yes | null when not kept (LRC-14) |
| `transcribedBy` | TranscriptionMode | yes | engine that produced the text (null for text records) [PROPOSED] |
| `doneMarked` | Boolean | no | `끝냄` mark (LRC-26); cleared when an insertion completes |
| `hidden` | Boolean | no | hidden record (LRC-40) |
| `deletedAt` | Instant | yes | in trash since (LIB-38); purge at +30 days |
| `trashEntryKey` | Ulid | yes | LIB-38 index entry |

**`LrAudio`** (embedded in `LrRecord`)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `contentHash` | String | no | sha256 of the file; file name in app storage and in `assets/` |
| `mimeType` | String | no | `ED` `audio/mp4` (AAC) |
| `sizeBytes` | Long | no | |
| `durationMs` | Long | no | |
| `envelope` | ByteArray | no | downsampled loudness for the 20-bar waveform and replay [PROPOSED] |

**`LrLink`** (`연결`)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `key` | Ulid | no | |
| `recordKey` | Ulid | no | → `LrRecord` |
| `targetKind` | LinkTargetKind | no | |
| `projectKey` | Ulid | no | project of the target (= `targetKey` for `PROJECT`) |
| `targetKey` | Ulid | no | permanent key of the target |
| `origin` | LinkOrigin | no | decides × vs `@` chip (LRC-18) |
| `createdAt` | Instant | no | chip order |
| `nameTag` | String | yes | last known label, written when the target is permanently deleted (e.g. `1-7 씬 3`, `잿빛 새벽의 등불`, `노아`) so the link can show `지워진 씬 · 1-7 씬 3` / `영구 삭제 · 노아` |
| `purgedAt` | Instant | yes | target permanently deleted |
| `nameTagHidden` | Boolean | no | the target was hidden when it was purged → show `영구 삭제 · 숨긴 항목`, never the name (rule 20 edge row) |

Constraint: unique (`recordKey`, `targetKind`, `targetKey`). Implied project links are not rows.

**`LrPlacement`** (`넣은 곳`)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `key` | Ulid | no | |
| `recordKey` | Ulid | no | → `LrRecord` |
| `projectKey` | Ulid | no | destination project |
| `kind` | PlacementKind | no | |
| `targetKey` | Ulid | no | created block / card paragraph / page / the scene-episode-chapter whose memo got the line / the new character |
| `containerKey` | Ulid | yes | scene key for a block; card key for a paragraph (+ `tabId`) |
| `tabId` | String | yes | card tab or field id for `CARD_PARAGRAPH` |
| `copiedRevision` | Int | no | `LrRecord.textRevision` at copy time (`◇ 넣은 뒤 고침` when lower than the current revision) |
| `createdAt` | Instant | no | |
| `nameTag` | String | no | label at creation (`1-7 씬 3 · #062`), refreshed while the target exists, kept after purge |
| `purgedAt` | Instant | yes | target permanently deleted → `GONE` |

**`RecorderSettings`** (app settings group; not in project files; in the app-settings backup)

| Field | Type | Default | UI |
| --- | --- | --- | --- |
| `transcription` | TranscriptionMode | `ON_DEVICE` | `받아쓰기` |
| `lockScreenRecording` | Boolean | true | `잠금 화면에서 녹음` |
| `keepAudio` | Boolean | true | `원본 음성 보관` |
| `presetLink` | Boolean | true | `보던 곳 연결` |
| (glow) | — | — | uses `AppSettings.recorderGlow` (60-library, default true; `녹음 중 빛 띠` = `라이프 레코더 빛`) |

**`EntryContext`** (runtime, carried by a capture)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `surface` | EntrySurface | no | where the capture started |
| `projectKey` | Ulid | yes | project on screen |
| `targetKind` / `targetKey` | LinkTargetKind / Ulid | yes | pre-filled link target per LRC-19 |
| `caretBlockKey` | Ulid | yes | script only: block holding the caret (for `여기 넣기`) |
| `isExample` | Boolean | no | inside the onboarding example project |
| `onLockScreen` | Boolean | no | started while the keyguard was showing |

**`RecordingSession`** (runtime; persisted to app-private storage for crash recovery, never exported)

| Field | Type | Meaning |
| --- | --- | --- |
| `recordKey` | Ulid | key reserved at start; the record row is written at `멈추고 저장` |
| `entry` | EntryContext | |
| `phase` | `RECORDING` / `PAUSED` / `PROCESSING` | screen state |
| `elapsedMs` | Long | timer |
| `confirmedRuns` / `partialText` | List<TextRun> / String | transcript (`ink` / `ink3`) |
| `pendingLinks` | List<LrLink> | links edited on the capture screens |
| `hideSwitch` | Boolean? | LRC-42 row value; null when the row is not shown |
| `audioTempPath` | String | growing audio file |

**`TextMemoDraft`** (app-only, one at a time) [PROPOSED]: `runs`, `pendingLinks`, `entry`, `updatedAt`.

**`InboxFilterState`** (app session only): `status: StatusFilter = ALL`, `kind: KindFilter = NONE`, `project: ProjectFilter = All`, `target: (LinkTargetKind, Ulid)?`.

**Unlock state**: add `recordsUnlockedAt: Instant?` to 60-library's runtime `UnlockSession` (null = locked). Re-locked by the LIB-33 events and `다시 잠그기`.

### 3.3 Derived values

- `state(record)` = `PUT` if any placement has status ≠ `GONE`; else `DONE` if `doneMarked`; else `UNSORTED`.
- `placementsAllGone(record)` = placements non-empty and all `GONE` → `◇ 넣은 곳 지워짐`.
- `editedAfter(placement)` = `record.textRevision > placement.copiedRevision` → `◇ 넣은 뒤 고침`.
- `placementStatus` = `GONE` if `purgedAt` set; `IN_TRASH` if the target is in the trash; else `ACTIVE`.
- `impliedProjects(record)` = distinct `projectKey` over links whose project is not permanently deleted.
- `linkDisplay(link)` evaluated in this order (**hiding wins**, rule 20 edge row "숨김이 이긴다", rule 19 §3): (1) record hidden & locked → not shown; (2) project hidden & locked → `HIDDEN_PROJECT` (also when the project or target is in the trash or purged); (3) project `recorderLinkMask` on → `MASK_RATING`, label = `recorderLinkLabel(project)` (LIB-28; `R-18` for an R-18 project); (4) item hidden & its project's items locked → `HIDDEN_ITEM` (also when in trash; after purge `영구 삭제 · 숨긴 항목` if `nameTagHidden`); (5) target purged → `GONE`; (6) target in trash → `TRASHED` (scene, project, chapter, episode, page) / `DELETED_ITEM_TRASH` (character, place, term, faction); (7) project is the onboarding example → `EXAMPLE`; (8) else `NORMAL` with the computed label. A purged target whose `nameTagHidden` is true never prints its name.
- Counts exposed to other areas (LRC-47): `libraryCounts()`, `unsortedLinkedCount(targetKey)`, `recorderLinkCount(targetKey)`, `linkedRecordCount(projectKey)`, `startMaterialCandidates()`. None of them is stored.

### 3.4 Relations to entities owned by other areas

| Other area | Entity / field | Relation |
| --- | --- | --- |
| Script (SCR) | `Block.recorderSource {recordKey, recordedAt}` (memo blocks) | ↔ `LrPlacement(kind = SCRIPT_BLOCK, targetKey = block key, containerKey = scene key)` |
| Script (SCR) | per-project last writing position (block key + offset) | read by LRC-33 `마지막으로 쓰던 곳` |
| Script / story | Scene, Episode, Chapter (keys, numbers, trash state, `씬 메모`, description line) | link targets; `MEMO_FIELD` placements |
| Cast (CST) | Character; character tab paragraphs with a recorder source | link targets; `CARD_PARAGRAPH` and `CHARACTER` placements |
| World (WLD) | Place, Term, Faction; their long-text paragraphs with a recorder source | link targets; `CARD_PARAGRAPH` placements |
| Pages (CNV) | Page with `source {recordKey, recordedAt}` (CNV-15) | link target; `PAGE` placements |
| Library (LIB) | Project (`hidden`, `rating`, `protections.recorderLinkMask`, `captureBlock`, `isExample`, format) | masking, capture guard, protected targets, example handling |
| Library (LIB) | `TrashEntry` (filter `RECORDER`) | records in trash |
| Library (LIB) | `UnlockSession.recordsUnlockedAt` (added here) | hidden-record gate |
| Library (LIB) | `AppSettings.recorderGlow` | glow switch |

### 3.5 What is stored in files

**Project file `.civilight` → `recorder.json`** (file format §2, §6; rule 23 §6):
- Records linked to the project (explicit or implied), not in trash; hidden records only when the project is hidden and the file is encrypted.
- Per record: `key`, `kind`, `createdAt`, `modifiedAt`, `modifiedByDevice`, `text` (runs; `@` runs into other projects written as plain text [PROPOSED]), `textRevision`, `durationMs`, `doneMarked`, `hidden`, the record's links **into this project** and its placements **in this project** (with `nameTag`, `copiedRevision`, `purgedAt`), `audio` reference `assets/<hash>.<ext>` only when `원본 음성 포함` is checked, `ext` (optional extension object, file format §4).
- Never: other projects' links/placements [PROPOSED], `RecordingSession`, drafts, filter state, unlock state, settings.

```json
{
  "records": [
    {
      "key": "01JDR1…",
      "kind": "voice",
      "createdAt": "2026-11-21T23:41:00Z",
      "modifiedAt": "2026-11-21T23:41:30Z",
      "modifiedByDevice": "…",
      "text": [{ "t": "하르트가 등불을 싫어하는 이유. 대정전 때 누군가를 잃었다는 설정은 어떨까. 그게 세라의 오빠였다면?" }],
      "textRevision": 1,
      "durationMs": 24000,
      "doneMarked": false,
      "hidden": false,
      "links": [
        { "key": "…", "kind": "scene", "target": "<1-7 씬 3 key>", "origin": "entry", "createdAt": "…" },
        { "key": "…", "kind": "character", "target": "<하르트 key>", "origin": "manual", "createdAt": "…" }
      ],
      "placements": [],
      "audio": null
    }
  ]
}
```
(2026-11-22 08:41 in Asia/Seoul = `2026-11-21T23:41:00Z`.)

**Library file `.civilight-recorder`** (LRC-46): same record objects for **all** records not in trash, with all links and placements of every project, audio per `keepAudio`; `ED` hidden records encrypted with the 복구 암호 or left out (§6 Q16). Mimetype `application/vnd.civilight.recorder`; manifest and version rules as `.civilight` (LIB-48/50).

**Import/restore merge** (rule 23 §6): same record key → one record; union of links and placements by key; local text kept.

---

## 4. Rules as test cases

### 4.1 Fixtures

- **Clock**: 2026-11-22 09:00, zone Asia/Seoul.
- **P1** 「잿빛 새벽의 등불」: R-15, not hidden, protections all off (`recorderLinkMask = false`, capture off). Structure 장 › 에피소드 › 씬. Scene **S** = `1-7 씬 3`. Episode 1-7: highest block number ever issued **#061** (given). Last writing position: caret in block **#044** of S. Chapter **C2** = `2장`. Characters 리엔, 하르트, 세라, 오웬, 노아. Term 오리지늄 등불 (alias 빛돌). Timeline event 대정전 (not a term).
- **P2** 「유리 정원」: ALL, 단편, not hidden.
- **P3** 「검은 서약 (가제)」: R-18, hidden, locked, `recorderLinkMask = true`.
- **R1**: VOICE, `createdAt` 2026-11-22 08:41, 24 s, text `하르트가 등불을 싫어하는 이유. 대정전 때 누군가를 잃었다는 설정은 어떨까. 그게 세라의 오빠였다면?`, links [S (ENTRY), 하르트 (MANUAL)], no placements, not hidden.
- **R2**: TEXT, 2026-11-21 23:10, text `오웬은 사실 결사 출신? 탈퇴 이유가 ` + `@세라` (link run → 세라) + `와 관련`, links [P1 (ENTRY), 오웬 (MANUAL), 세라 (AT)].
- **R3**: VOICE, 2026-11-21 08:15, 62 s, text `2장 첫 장면은 비 오는 상층 관문에서 시작. 리엔이 처음으로 위를 올려다보는…`, no links.
- **R4**: TEXT, 2026-11-18, `결말: 리엔이 마지막 등불을 스스로 끈다.`, `doneMarked = true`.
- **R5 … R12**: 8 records created 2026-11-01 … 2026-11-08 (one per day, 10:00), each with one ACTIVE placement (state 넣음) in P2 and one INSERT link to that placement's page in P2.
- Inbox **F-open**: R1 … R12, none hidden. Inbox **F-hidden**: same but R1, R4, R5 hidden.

### 4.2 Links and entry

**LRC-T01 · Pre-filled link by entry** (rule 23 §2 table). Given `presetLink = true`. When a capture starts from each entry, then the pending links are:

| Entry | Expected pending links |
| --- | --- |
| script command bar in S | [S (ENTRY)] → chip `연결 · 1-7 씬 3` |
| profile of 하르트 | [하르트 (ENTRY)] |
| a page `하층 구역 메모` of P1 | [that page] |
| P1 project home / 스토리 구조 / 연표 / 지도 | [P1 (ENTRY)] → chip `연결 · 잿빛 새벽의 등불` |
| 서재, home widget, QS tile, lock screen, cover widget | [] → only `+ 연결` |
| inbox `말로 기록` with `ProjectFilter = Project(P1)` | [P1 (ENTRY)] |
| inbox `말로 기록` with `ProjectFilter = All` | [] |

And with `presetLink = false`, every entry gives [].

**LRC-T02 · Rule 23 §2 example.** Given the script open at S. When the mic is tapped, the user speaks, taps `멈추고 저장`, then on `정리 중` taps `+ 연결` and chooses 하르트, then `기록함에 두기`. Then a record exists with kind VOICE, duration 24 s, links [S (ENTRY), 하르트 (MANUAL)], state `정리 안 됨`; its inbox row reads `음성 0:24` · `오늘 08:41` · `정리 안 됨` / the text / link row `1-7 씬 3` · `하르트` / actions `원고에 넣기` · `카드에 넣기` · `페이지로 만들기`. No row text contains `에서`.

**LRC-T03 · Sub-link implies the project.** Given F-open. When `ProjectFilter = Project(P1)`, then R1 (scene link only) and R2 are listed and R3 is not. When `ProjectFilter = Project(P2)`, R1, R2, R3 are not listed. When `ProjectFilter = NoLink`, R3 is listed and R1, R2 are not.

**LRC-T04 · @ chip line** (rule 23 §5). Given a text memo opened from P1 project home, text `오웬은 사실 결사 출신? 탈퇴 이유가 ` and the user adds 오웬 with `+ 연결`, then types `@` and picks 세라, then `와 관련`. Then the chip row is `연결 · 잿빛 새벽의 등불` (×) · `오웬` (×) · `세라 @` (no ×) · `+ 연결`. When the user deletes the text `@세라`, then the 세라 link is removed and the row is `연결 · 잿빛 새벽의 등불` · `오웬` · `+ 연결`. When the user taps × on `오웬`, the 오웬 link is removed.

**LRC-T05 · @ search order.** Given a record linked to P2 only, the most recently opened project is P1, and both P1 and P2 have a character named `세라`. When the user types `@세라`, then the P2 세라 is listed first, the P1 세라 second. Given locked P3 has a character `세라`, it is not listed at all.

**LRC-T06 · Cross-project @ implies that project.** Given a text memo with no links. When the user picks `@하르트` (P1). Then links = [하르트 (AT)], implied projects = {P1}, and `ProjectFilter = Project(P1)` lists the record.

**LRC-T07 · Dictation creates no links; `글에 나온 이름`.** Given R1 was dictated. Then R1 has no AT links and no text run is a link. When the user opens `+ 연결` on R1, then the first group `글에 나온 이름` lists 하르트 (already linked, shown checked [PROPOSED]) and 세라, in order of first occurrence [PROPOSED]; 대정전 (an event) and 오리지늄 등불 (text has only `등불`) are not in the group; 리엔, 오웬, 노아 are not in the group.

**LRC-T08 · Locked hidden targets are not offered.** Given P3 locked and P1 character 노아 hidden with P1 items locked. When `+ 연결` or `@` searches `노아` or any P3 item, then no result is shown. After unlocking P1's items, 노아 is listed.

### 4.3 States and inbox

**LRC-T09 · Three board records** (rule 23 §3 table). Given F-open. Then state(R1) = state(R2) = state(R3) = `정리 안 됨`; R1 link row `1-7 씬 3` · `하르트`; R2 link row `오웬` · `세라` (the P1 project tag is omitted because 오웬/세라 are inside P1 — [PROPOSED] LRC-21 rule; a record linked only to P1 shows `잿빛 새벽의 등불`); R3 has no link row and its action row is `원고에 넣기` · `카드에 넣기` · `페이지로 만들기` · `+ 연결`; no row shows `2장 메모로`.

**LRC-T10 · Filter counts.** Given F-open. Then the filter row reads `전체 12` · `정리 안 됨 3` · `넣음` · `끝냄` · `음성` · `글` · `프로젝트 ▾`. Given F-hidden, locked: `전체 9` · `정리 안 됨 2`, and the locked row reads `숨긴 기록 3` · `내용 · 날짜 · 길이 가림 · 검색 제외` · `지문으로 보기`. After a successful `지문으로 보기`: `전체 12` · `정리 안 됨 3`, group header `숨긴 기록 3 · 열림` · `앱을 떠나면 잠김`, rows R1, R4 (and R5) each with `숨김 풀기`, screen header button `다시 잠그기`.

**LRC-T11 · Sort.** Given F-open. Then the list order starts R1 (11-22 08:41), R2 (11-21 23:10), R3 (11-21 08:15), R4 (11-18).

**LRC-T12 · Labels.** Then R1 meta `오늘 08:41`, R2 `어제 23:10`, R3 `어제 08:15`, R4 `11월 18일`; R1 kind `음성 0:24`, R3 `음성 1:02`; the recording timer at 12 s reads `00:12`.

**LRC-T13 · 끝냄 and back.** Given R3 `정리 안 됨`. When `⋯ › 끝냄으로 표시`, then state `끝냄`, `전체 12` · `정리 안 됨 2`. When the toast's `되돌리기` is tapped, state `정리 안 됨`. When marked again and later `⋯ › 다시 정리 안 됨`, state `정리 안 됨`. `끝냄으로 표시` is not in the menu of R5 (state 넣음).

**LRC-T14 · Insert into a 끝냄 record.** Given R4 (`끝냄`). When it is inserted anywhere, then state `넣음` and `doneMarked = false`. When that insertion is undone, then state `끝냄` again (rule 23 §3 "되돌리기로 취소하면 앞 상태로"; [PROPOSED] mechanics: undo restores `doneMarked`).

**LRC-T15 · Target line count** (rule 23 §2). Given F-open. Then `unsortedLinkedCount(S)` = 1 (R1) and the scene memo card shows `레코더 · 정리 안 됨 1 ›`; `unsortedLinkedCount(하르트)` = 1; `unsortedLinkedCount(세라)` = 1 (R2's AT link); `unsortedLinkedCount(P1-only targets such as 리엔)` = 0 → no line. Given F-hidden locked, `unsortedLinkedCount(S)` = 0 → no line. Tapping the line on S opens the inbox with target filter S and status `정리 안 됨`, listing R1 only.

### 4.4 넣기

**LRC-T16 · Position sheet rows** (rule 23 §4). Given R1 and P1 with last writing position after #044 in S. When `원고에 넣기`. Then the title is `원고에 넣기 · 잿빛 새벽의 등불 ▾` and the rows are exactly, in order: `연결된 씬 · 1-7 씬 3 · 맨 끝`; `마지막으로 쓰던 곳 · 1-7 씬 3 · #044 뒤`; `씬 고르기 ›`. No row is selected and nothing is inserted until a row is tapped.

**LRC-T17 · Block number and toast.** Given LRC-T16. When `마지막으로 쓰던 곳 · 1-7 씬 3 · #044 뒤` is tapped. Then one block of kind `작가 메모` is created directly after #044 in S with number **#062**, export excluded, text = R1's text, `recorderSource = {R1.key, 2026-11-22 08:41}`, memo prefix `⌁ 작가 메모 · 레코더 11월 22일 08:41`; R1 gets one placement (SCRIPT_BLOCK, nameTag `1-7 씬 3 · #062`, copiedRevision 1); R1's links are unchanged (S already linked); state(R1) = `넣음`; toast `1-7 씬 3 · #062 작가 메모 넣음 · 되돌리기 · 가 보기`.

**LRC-T18 · 이미 넣음 row.** Given LRC-T17 done. When `원고에 넣기` is opened again for R1. Then rows are `연결된 씬 · 1-7 씬 3 · 맨 끝`; `마지막으로 쓰던 곳 · 1-7 씬 3 · #044 뒤`; `씬 고르기 ›`; `이미 넣음 · 1-7 씬 3 · #062`. Before the first insertion the `이미 넣음` row is absent.

**LRC-T19 · Undo.** Given LRC-T17 done. When the toast's `되돌리기` is tapped. Then block #062 no longer exists and is not in the trash, R1 has no placement, state(R1) = `정리 안 됨`. The episode's next number is #063 for the following new block (the counter only grows and numbers are never reissued: rule 12 §3, SCR-21).

**LRC-T20 · 여기 넣기 only from the script.** Given a recording started from the script command bar with the caret in #044 of S. Then `정리 중` shows `여기 넣기 · #044 뒤` above `이어서 말하기` · `기록함에 두기`, and the manuscript is unchanged. When it is tapped, the result equals LRC-T17. Given a recording started from P1 project home (or the inbox, widget, tile), then `여기 넣기` is absent.

**LRC-T21 · Insert adds a link when missing.** Given R3 (no links). When `페이지로 만들기` into P1 with the prefilled name kept (`ED` prefill = `2장 첫 장면은 비 오는 상층 관문에서 시작`). Then a page exists in P1 with R3's text, R3 gets a PAGE placement and a link (INSERT) to the page; implied projects = {P1}; state `넣음`.

**LRC-T22 · 메모 칸에 넣기 → 2장** (rule 23 §4 bullet). Given R3. When `⋯ › 메모 칸에 넣기` → P1 → `2장`. Then the text is appended as one line at the end of 2장's description line; R3 gains a MEMO_FIELD placement and a link (INSERT) to C2; state `넣음`.

**LRC-T23 · Two projects.** Given R1 inserted into P1 (LRC-T17). When R1 is also made into a page in P2. Then R1 has two placements (P1 block #062, P2 page) and links S, 하르트, plus the P2 page (INSERT); implied projects {P1, P2}; state `넣음`.

**LRC-T24 · @ runs in copies.** Given R2 (`@세라` → P1 세라). When R2 is made into a page in P1, the page text contains a link run to 세라. When R2 is made into a page in P2, the page text contains the plain text `세라` with no link.

**LRC-T25 · Edits after insertion.** Given LRC-T17 done. When R1's text is edited (revision 2). Then block #062 text is unchanged and R1's record screen shows `◇ 넣은 뒤 고침` beside `1-7 씬 3 · #062`; the check center has no new item; the inbox row shows no mark. When instead block #062 is edited, R1 is unchanged and shows no mark.

**LRC-T26 · Protected destination.** Given R3. When P3 (hidden, locked) is chosen as the destination in any 넣기 sheet, then the biometric prompt runs before any row appears; on cancel nothing changes and the sheet stays on the project choice. When an unhidden R-18 project is chosen, the prompt also runs.

**LRC-T27 · 인물로 만들기.** Given R3 and P1. When `⋯ › 인물로 만들기` with name `시장 꼬마`. Then a character `시장 꼬마` exists in P1 with `한 줄 소개` = `2장 첫 장면은 비 오는 상층 관문에서 시작.` and a `⌁ 작가 메모` paragraph with R3's full text at the end of `개요`; R3 gains a CHARACTER placement and a link (INSERT) to 시장 꼬마; state `넣음`.

**LRC-T28 · Start material and checklist** (rule 23 §4 table). Given F-open records R1, R3 checked in new-project step 4 (`라이프 레코더 기록 연결 · 3개 중 2개 고름`, candidates R1, R2, R3). When `만들기` creates project P4. Then R1 and R3 each gain a link (START_MATERIAL) to P4, nothing is inserted, states stay `정리 안 됨`, and the checklist shows `연결된 기록 2개 정리하기` · `기록함`; tapping it opens the inbox with `ProjectFilter = Project(P4)` listing R1 and R3.

### 4.5 Masking, hiding, capture

**LRC-T29 · Rule 19 example: mask applies at once.** Given P1 (R-15) whose only linked record is R1 (links S, 하르트) — R2 is not in this fixture. When P1 is raised to R-18 in the `바뀌는 것` sheet with `레코더 연결 가리기` checked (sub `연결에 등급만 · 기록 1 바로 적용`). Then, without reopening anything, R1's link row shows one tag `R-18` (S and 하르트 both collapse into it [PROPOSED collapse]), and the record screen chip shows `연결 · R-18`; the same while P1 is open.

**LRC-T30 · Masking table** (rule 23 §6). Given a record linked to a scene of P3 (hidden, locked) → label `숨긴 프로젝트`; after unlocking the hidden group → the scene label. Given a record linked to 노아 (hidden item, P1 items locked) → `숨긴 항목`; after unlocking P1 items → `노아`. Given the record itself hidden and locked → it is not listed and no link is shown.

**LRC-T31 · 숨기기 row default** (rule 23 §6). Given a recording started from a scene of P3 while the hidden group is open. Then `정리 중` shows the `숨기기` row **on**. When the user saves with `기록함에 두기`, the record is hidden. Given the same with the row turned off, the record is not hidden. Given a recording linked only to P1 (R-15, not hidden), no `숨기기` row is shown. Given a recording linked to an R-18 project that is not hidden, the row is shown on.

**LRC-T32 · Capture guard.** Given P1 with `화면 캡처 가리기` on and R1 linked to S. When the inbox shows R1's row on screen, the window is FLAG_SECURE; when R1 is scrolled out and no other record of a capture-blocked project is visible, it is cleared.

**LRC-T33 · Search exclusion.** Given R4 hidden and locked. When the inbox search (or command search) looks for `마지막 등불`, then R4 is not found. [PROPOSED] After unlocking, it is found only while unlocked (follows `검색 · 명령 결과 · 열려 있을 때만 결과에 포함`).

### 4.6 Deletion

**LRC-T34 · Linked scene deleted** (rule 23 §6 row 1). Given R1 linked to S and 하르트. When S goes to the trash, R1's link row shows a dotted `1-7 씬 3 · 휴지통`. When S is restored, `1-7 씬 3` again. When S is permanently deleted, the link shows plain text `지워진 씬 · 1-7 씬 3`, `ProjectFilter = Project(P1)` still lists R1.

**LRC-T35 · Linked project deleted** (row 2). Given R2 linked to P1 (+ 오웬, 세라). When P1 goes to the trash, the project link shows `잿빛 새벽의 등불 · 휴지통`. When P1 is permanently deleted, the link shows the plain name `잿빛 새벽의 등불` and `ProjectFilter = NoLink` lists R2.

**LRC-T36 · Placements deleted** (row 3). Given LRC-T17 done (#062). When #062 goes to the trash, R1's 넣은 곳 row reads `1-7 씬 3 · #062 · 휴지통` and state stays `넣음`. When #062 is permanently deleted, the row stays as plain text and, it being R1's only placement, state(R1) = `정리 안 됨` and the record screen shows `◇ 넣은 곳 지워짐`; no check-center item.

**LRC-T37 · Record deleted** (row 4). Given LRC-T17 done. When R1 goes to the trash, block #062's source mark reads `레코더 · 휴지통` and its text is unchanged; R1 is absent from the inbox and from `unsortedLinkedCount`. When R1 is restored, the mark is normal again and R1 is back with its links, placement and state `넣음`. When R1 is permanently deleted, the mark reads `지워진 기록` and #062's text is unchanged.

**LRC-T38 · Deleted character** (rule 20). Given R1 linked to 노아 (MANUAL) and no other record referring to 노아. When 노아 is deleted, `recorderLinkCount(노아)` = 1, the impact sheet counts `레코더 칩 1` and the check row includes `레코더 1`; R1's chip is dotted `노아` + `◇`; tapping it shows `지운 인물 · 노아` with `칩 다시 고르기` · `칩 빼기` · `이대로 두기`. When 노아 is restored, the chip is normal. When 노아 is permanently deleted, the chip reads `영구 삭제 · 노아`.

**LRC-T38b · Deleted character in an `@` run** (rule 20 §2). Given R2 with the run `@세라` and its AT chip. When 세라 is deleted, `recorderLinkCount(세라)` = 0 and `recorderAtLinkCount(세라)` = 1 (counted under `@연결`); the run and the chip `세라 @` are dotted with `◇`; tapping shows `다른 항목에 잇기` · `글로 풀기` · `되살리기` · `이대로 두기` and no `칩 빼기`. When `글로 풀기` is tapped, the text still reads `…탈퇴 이유가 세라와 관련`, the run is plain text and the 세라 link is gone.

**LRC-T38c · Hidden wins over deletion.** Given P1's character 노아 hidden, P1 items locked, and R1 linked to 노아. When 노아 goes to the trash, R1's chip reads `숨긴 항목` (not dotted `노아`). When 노아 is purged, the chip reads `영구 삭제 · 숨긴 항목`.

**LRC-T39 · Scene moved** (rule 23 §6). Given R1 linked to S (`1-7 씬 3`). When S is moved to the start of episode 1-8, the link still points to S's key and its label is `1-8 씬 1`.

**LRC-T40 · Example project** (rule 23 §6). Given the onboarding example project and a recording started inside it. Then the chip reads `예시 · 끝나면 연결 없어짐`. When `온보딩 끝내기` deletes the example, the record remains in the library with no links (`ProjectFilter = NoLink` lists it).

**LRC-T41 · Trash row and filter.** Given a voice record of 12 s deleted on 2026-10-24. Then the trash with filter `레코더` shows the row `음성 0:12` · `라이프 레코더` · `1일 남음` with `되살리기` · `영구 삭제`; no link or placement appears in the trash under any filter. On 2026-11-23 the record is purged.

**LRC-T42 · Deleting a project never trashes records.** Given R2 linked to P1. When P1 goes to the trash, R2 stays in the inbox (not in the trash).

### 4.7 Export

**LRC-T43 · Export row** (rule 23 §6). Given P1 not hidden with 4 linked records not in trash, one of them hidden. Then the `.civilight` export sheet row reads `레코더 기록 4` · `원본 음성 포함` (unchecked) · `숨긴 기록 1 제외`, and the written `recorder.json` contains 3 records and no audio assets. With `원본 음성 포함` checked, the voice records' audio files are in `assets/`.

**LRC-T44 · Hidden project file.** Given P3 hidden with 2 linked records, one hidden. When `.civilight` of P3 is exported (encrypted), both records are in the file.

**LRC-T45 · Other formats.** Given P1 with linked records. When `대본 PDF`, `대사 표 CSV` or `게임 엔진 JSON` is exported, no record text appears in the file except through inserted memo blocks, which follow their display/export rules.

**LRC-T46 · Multi-project and merge.** Given R1 linked to P1 and to a page of P2. Then R1 is in both P1's and P2's `.civilight`; P1's file holds only R1's P1 links/placements. When P2's file is imported on a device that already has R1, there is still one R1, now also holding the P2 links/placements; R1's local text is kept.

### 4.8 Recording rules

**LRC-T47 · No auto-stop.** Given a recording in progress. When the input is silent for 10 minutes, the phase is still `RECORDING`, the timer keeps counting and no record is saved. Only `멈추고 저장` creates the record.

**LRC-T48 · Resume continues the same record.** Given `정리 중` after 24 s. When `이어서 말하기`, then speaking 10 s and `멈추고 저장`. Then there is one record with duration 34 s and the new text appended after the old text.

**LRC-T49 · Lock screen.** Given the device locked and `잠금 화면에서 녹음 = true`. When the tile is tapped, a recording starts with no links; `정리 중` offers `이어서 말하기` · `기록함에 두기` only (no `여기 넣기`); tapping `+ 연결` requests device unlock [PROPOSED, §6 Q7]. Given `잠금 화면에서 녹음 = false`, tapping the tile on the lock screen requests unlock before recording.

**LRC-T50 · Rule 21 does not apply.** Given R1's text contains `등불` and a record text containing `오리지늄 등불` without `@`. Then no `후보` or `▲ 공개 전 사용` is produced for any record, and term `쓰인 곳` lists no recorder record unless it is an `@` link (rule 21: only `@` links count).

---

## 5. Screens

| Screen / sheet / state | Boards | Cover (390) | Inner (800) | Components |
| --- | --- | --- | --- | --- |
| 녹음 중 (LRC-07) | `LrRecord`, `Walk3`, `TourLr` | full-window glass over the blurred previous screen; header, chips, transcript 25/40, bottom row `글로 쓰기` · `멈추고 저장` · pause | same full-window layout; text column max 560 centred [PROPOSED]; in `TwoLr` the right pane when started from the inbox [PROPOSED] | `.glass`, `Tag` chips, `Buttons`, glow bands |
| 정리 중 (LRC-13) | `LrProcess`, `TwoLr` | full window; `여기 넣기` / `이어서 말하기` · `기록함에 두기` | right pane of `TwoLr` (409 wide) | `.glass`, `Tag`, `Buttons`, glow sweep |
| 마이크 꺼짐 (LRC-06) | `ErrMic` | full-window glass | same, centred | `Buttons` |
| 글 메모 (LRC-15) | `LrText` | bottom sheet with keyboard | `ED` the same bottom sheet, width ≤ 560 centred (DS: popovers on inner) | `Sheet`, `Tag`, `Buttons` |
| 기록함 (LRC-25) | `LrInbox` | list + bottom bar | `TwoLr` (목록 + 상세, default) or `ColLr` (가운데 한 칸) | `GlassBars`, `Tag`, `ListRow`-like rows, `Buttons` |
| 숨긴 기록 잠김 (LRC-40) | `LrLocked` | locked group row under filters | same in left pane / column | `LockRow` |
| 지문으로 열기 (LRC-41) | `LrUnlock`, `ErrBio` | system biometric prompt | system | system UI |
| 숨긴 기록 열림 (LRC-41) | `LrUnlocked`, `MUnlock` | open group box, `다시 잠그기` in header | same | `LockRow` open form |
| 기록 ⋯ (LRC-29) | `LrHide` | bottom sheet with preview card | popover/sheet anchored to the ⋯ button [PROPOSED] | `Sheet`, option rows |
| 빈 기록함 (LRC-30) | `EmptyLr` | empty block | same | empty block, `Buttons` |
| 기록 화면 (LRC-28) | `TwoLr` right pane (`LrProcess` layout) | full screen [PROPOSED] | right pane / column | as 정리 중 + `넣은 곳` list |
| 연결 고르기 (LRC-20) | not drawn | bottom sheet | sheet/popover | `Sheet`, search field, list |
| 원고에 넣기 위치 시트 (LRC-33) | not drawn (copy in rule 23 §4) | bottom sheet | sheet/popover | `Sheet`, rows |
| 카드에 넣기 · 페이지로 만들기 · 메모 칸에 넣기 · 인물로 만들기 (LRC-35…38) | not drawn | bottom sheets | sheet/popover | `Sheet` |
| 설정 · 라이프 레코더 (LRC-44) | `LrSettings` | settings list | settings list (centred column, LIB settings layout) | radio, switch rows |
| Entry points overview (LRC-01…05) | `LrEntry` (800×800 design board, not an app screen) | — | — | `CommandBar`, widget, tile, cover widget mocks |
| Home widget / cover widget (LRC-02, LRC-04) | `LrEntry` panel mocks | launcher | launcher | mic `pri` circle + `떠오른 생각` / `눌러서 바로 녹음` |
| Toasts | rule 23 §4 copy | bottom toast (LIB-40) | same | `Toast` |

Boards owned by this spec: `LrEntry`, `LrHide`, `LrInbox`, `LrLocked`, `LrProcess`, `LrRecord`, `LrSettings`, `LrText`, `LrUnlock`, `LrUnlocked`, `EmptyLr`, `ErrMic`, `TwoLr`, `ColLr`, `MUnlock` (recorder rows).
Boards only referenced (owned elsewhere): `Main` and DS `CommandBar` (mic), `TourLr` / `HelpReplay` / `OnbVoice` / `Walk3` / `NewStart` / `NewReady` / `Library` / `OnbEmpty` (LIB onboarding & 서재), `CSheetMods` / `WideMods` (SCR), `Profile` / `EmptyCast` / `RCDeleteImpact` (CST), `Trash` / `Export` / `RatingSettings` / `RHRatingSheet` / `RHRatingDown` / `ProjSettings` / `ReduceSettings` / `LayoutSettings` / `ErrBio` (LIB), `MotionSpec`.

Motion summary for this area: sheets decelerate 320 ms with the back screen at 96.5 % + 2 px blur; glow linear; processing sweep 2.4 s; unhide blur 7 px → 0 in 320 ms (reverse on re-lock); reduce motion → 120 ms fades, glow static.

---

## 6. Open questions

Each item names both sides. `ED` is the default this spec uses until decided.

1. **Speech-to-text engine and cloud provider.** MAIN 남은 확인 lists "기기 안 받아쓰기 엔진과 클라우드 받아쓰기 제공자" as undecided, and "어느 기기에서 기기 안 모델을 쓸 수 있는지는 확인 필요" (MAIN, 지도 글로 만들기 line). LRC-11/12 need an engine for API 29–30 and for devices without an on-device Korean model. `ED` platform `SpeechRecognizer` on-device where available; otherwise record audio only.
2. **Cloud transcription for hidden or R-18 material.** `LrSettings` makes `클라우드` a global choice; rule 19/23 hide such records but say nothing about sending their audio out. `ED` global choice applies.
3. **`원본 음성 보관 · 30일 뒤 정리 가능`.** `LrSettings` and MAIN 설정 say "30일 뒤 정리 가능" without saying whether audio is removed automatically after 30 days or a cleanup is offered; rules 12/23 say nothing is deleted silently. `ED` no automatic removal; cleanup UI not defined.
4. **Toast with two actions.** `rules/23.md` §4: `1-7 씬 3 · #062 작가 메모 넣음 · 되돌리기 · 가 보기`; the live rule-23 tab: `… 작가 메모 넣음 · 되돌리기` only; DS `Toast` README: "동작 하나". `ED` follow 23.md (10-script SCR-59 does the same).
5. **Deleted-target chip wording.** Rule 20 §2 (recorder row): dotted chip while in trash, `영구 삭제 · 노아` after purge; rule 23 §6: `1-7 씬 3 · 휴지통` while in trash, `지워진 씬 · 1-7 씬 3` (plain text) after purge. `ED` rule 20 wording for characters/places/terms/factions, rule 23 wording for scenes and projects; chapters/episodes/pages have no copy (`TODO_COPY_LRC_08`).
6. **`+ 연결` on inbox rows and the ⋯ entry.** Rule 23 §4: "기록함 행은 연결과 상관없이 같은 버튼 셋"; board `LrInbox` adds `+ 연결` only on the linkless row. No board shows how ⋯ is opened from a row. `ED` `+ 연결` on linkless rows only (as drawn), a visible `⋯` button on every row.
7. **`+ 연결` on the lock screen.** Rule 23 §2: lock screen → "없음 · `+ 연결`만"; `LrSettings`: "새 녹음만 · 기록함은 잠금 해제 후". The link picker lists project and item names. `ED` the chip is shown but opening it requires device unlock.
8. **Undefined recording controls.** `LrRecord` draws `취소` and a pause button, but no source defines what `취소` discards, the paused state's look, or behaviour on calls / audio focus loss / process death. `ED` LRC-07, LRC-09 (discard with undo toast, `TODO_COPY_LRC_02/03`; pause on focus loss; recover after process death).
9. **`숨기기` row on the text memo.** Rule 23 §6 names only the `정리 중` screen. `ED` same row on the text sheet.
10. **`글로 쓰기` during recording.** MAIN: "녹음 중 ‘글로 쓰기’로 전환 가능" — what happens to the audio and transcript so far is not stated. `ED` transcript moves into the text field; audio stays attached; record stays VOICE.
11. **Inbox header icon and searching records.** `LrInbox` draws a filter icon in the header; the filter chips are already in the body. MAIN/LrLocked say hidden records are "검색 제외", implying records are searchable, but the search categories (MAIN 검색: 인물 · 용어 · 대사 · 장면 · 명령) have no recorder group. `ED` the icon opens a text search over visible records; records are not in the command-bar search.
12. **Record screen not drawn.** Rule 23 places `넣은 곳 목록`, `◇ 넣은 뒤 고침` and `◇ 넣은 곳 지워짐` on "기록 화면"; no board draws it (`TwoLr` reuses `LrProcess`). `ED` LRC-28 layout; `넣은 곳` label from rule wording.
13. **Re-lock policy for hidden records.** `LrUnlocked` prints `앱을 떠나면 잠김`; LIB-33 has three policies for hidden projects/items. `ED` the same global policy; copy for the other two policies on this row is missing (`TODO_COPY_LRC_18`).
14. **서재 count with hidden records.** 서재 row `기록 12 · 정리 안 됨 3` (`Library`, `RHRatingSheet`) equals the unlocked inbox; the locked inbox shows `전체 9 · 정리 안 됨 2` (`LrLocked`). `ED` 서재 counts include hidden records.
15. **Counts on `넣음` / `끝냄`.** Rule 23 §3: "숫자는 상태 칩에만" (all four are status chips) but the example and board show numbers only on `전체` and `정리 안 됨`. `ED` as drawn.
16. **`.civilight-recorder` details.** File format tab defines the file and its backup but no manual export entry point (LIB-48: "not drawn"), and no rule says how hidden records are protected in it (hidden projects use the 복구 암호, file format §5). `ED` no manual entry in v1; hidden records encrypted with the 복구 암호, or left out of the backup when none is set.
17. **Masked projects in `프로젝트 ▾`.** Rule 23 §6 masks links to `R-18` "칩 · 기록함 표시"; with two masked projects the filter menu would show two identical `R-18` entries. `ED` masked label in menu order.
18. **`연결` used twice.** Rule 23 renamed `붙일 곳` to `연결`, which is also the character-field type `연결` (세력 · 장소 · 용어). Vocabulary memo and rule 23 §7 list this as an open naming question. `ED` keep both.
19. **`카드에 넣기` tabs for world cards.** Rule 23 §4 lists tabs `개요 · 말투 · 비밀 · 긴 글 항목` (character tabs); place / term / faction cards have other sections, and rule 23 §7 notes "카드 본문 블록 규칙과 맞춰야 한다". `ED` world cards offer their long-text sections as defined by the world area.
20. **`@` target kinds.** MAIN 글 메모: "@로 인물 · 용어 연결"; `LrText` hint `@로 인물 · 용어 · 씬 연결`; rule 23 §2 makes 장 · 에피소드 · 장소 · 세력 · 페이지 link units too. `ED` all link units are `@` candidates; hint copy as drawn.
21. **Missing system copy.** QS tile label (`TODO_COPY_LRC_01`), recording notification (`TODO_COPY_LRC_04`), STT-unavailable text (`TODO_COPY_LRC_05`), note without kept audio (`TODO_COPY_LRC_06`), link picker title (`TODO_COPY_LRC_07`), ⋯ aria label (`TODO_COPY_LRC_09`), other-year date (`TODO_COPY_LRC_10`), toasts for `끝냄`, `카드에 넣기`, `페이지로 만들기`, `메모 칸에 넣기`, `인물로 만들기`, record deleted (`TODO_COPY_LRC_11`, `13`, `15`, `16`, `17`, `19`), filtered-empty list (`TODO_COPY_LRC_12`), page sheet button (`TODO_COPY_LRC_14`), export row when hidden records are included (`TODO_COPY_LRC_20`), ⋯ subs for options other than the four drawn on `LrHide`.
22. **Recorder module tour.** MAIN and `HelpReplay` give the recorder tour 3 steps; only step 2/3 (`TourLr`) is drawn. Steps 1 and 3 are undefined (owned by LIB-19, listed here because they describe this area).
23. **Where the record is created.** No source says whether a record exists before `기록함에 두기`. `ED` created at `멈추고 저장` (the button says 저장), so leaving `정리 중` any way keeps it.
24. **Project tag on inbox rows.** Rule 23 §5 shows R2's chips as `연결 · 잿빛 새벽의 등불 · 오웬 · 세라 @`; rule 23 §3 table and `LrInbox` show the same record's link row as `오웬 · 세라`. `ED` (LRC-21): the inbox row omits a project tag when a link inside that project exists; capture and record screens show all chips.

---

## 7. 0.2.0 changes (2026-11-24) (0.2.0)

Feature ids `LRC-48` … `LRC-50`. Source: the frozen scope `docs/status/0.2.0-scope.md` (S-8, U-13, N-13, S-4, D8, D32, D34; §7 row N-73 `안 함`) and the decision row `레코더 입구` (2026-11-24): "앱 안에서는 홈에서만 연다. 앱 밖 입구(홈 화면 위젯 · 빠른 설정 타일 · 잠금 화면 녹음)는 `그 뒤`로 미루고, 자리만 있던 설정 항목도 감춘다. 명세 70의 진입점을 이렇게 고친다." Where this section and §2 disagree, this section wins; each superseded block carries a **(0.2.0)** line. Stored values are never removed (scope §8).

#### LRC-48 · 입구는 홈에서만 (entry points in 0.2.0)
**Priority** P0 · **needsExternal** no · scope S-8, D4

| Entry (0.1.0) | 0.2.0 |
| --- | --- |
| `CommandBar` mic on 프로젝트 홈 | **kept**: the only in-app mic. Entry context = that project (LRC-19 row `Project home`) |
| `CommandBar` mic on every other screen (script, cast, world, timeline, map, 서재, canvas, pages, gallery, check center, structure, assets, track screen) | **removed** (the bar draws no mic, DS-75) |
| 기록함 `말로 기록` · `글로 쓰기` (LRC-25, LRC-30) | kept: they are inside the recorder. Entry context = the inbox's project filter (LRC-19) |
| 서재 row `라이브러리 · 프로젝트 밖` › `라이프 레코더` (LIB-01) | kept: it opens the 기록함 list, not a recording (`ED`, LIB §8.8 Q-02-3) |
| Home-screen widget (LRC-02), quick-settings tile (LRC-03), cover-screen widget (LRC-04), lock-screen recording (LRC-05) | **not offered** (N-73 `안 함`). 0.1.0 declared `RecorderTileService` and `RecorderWidgetProvider` in `feature:recorder`'s manifest; 0.2.0 removes both declarations, so the system drops any placed tile or widget on upgrade (`ED`). No show-when-locked activity is declared. Records made through them in 0.1.0 stay in the 기록함 |
| `OnbVoice` `홈 화면 위젯 추가` (LRC-47) | hidden (LIB-95) |
| 설정 · 라이프 레코더 row `잠금 화면에서 녹음` (LRC-44) and any widget or tile row | hidden; the stored value is kept and not read (00 §13.7) |
| `EmptyLr` sub `명령 칸 마이크 · 홈 위젯 · 빠른 설정` (LRC-30) | `TODO_COPY_LRC_EMPTY_SUB` (none drawn until given) |
| Recorder records in search (`/` and the search sheet) | not searched (N-13; DS-73 has no recorder chip). The inbox's own search stays (LRC-25) |

- Rows of LRC-19's entry table for removed entries no longer occur; the rule for the remaining entries is unchanged.
- Tours and help rows that name removed entries are left out (LIB-96).

#### LRC-49 · 일시정지 · 나가기 (pause and leaving, U-13)
**Priority** P0 · **needsExternal** no · scope U-13, D8, D32 · **supersedes the `[PROPOSED]` back rule of LRC-07 and the pause line of LRC-07**

Owner of the behaviour: lane 1 (`feature:recorder`, `domain:recorder`); this block records the rule so the spec matches it.

- `일시정지` releases the microphone, audio focus and the speech engine's input. The timer stops. Resume starts a **new segment** of the same take (same record key, same links). `멈추고 저장` joins the segments in order into one audio and one transcript.
- **Leaving the recorder inside the app** (system back, or moving to another screen) while recording or paused: the take is saved as `정리 안 됨` (no data loss) and every mic holder is released once; toast `기록함에 둠` (DS-67 info). Nothing is discarded; `취소` stays the only discard.
- **App to the background** (home button, another app, screen off) while recording: the take keeps recording with its visible notification (LRC-09, D32). Removing the task from recents saves the take and releases the mic.
- Tests (`MicReleaseTest`, scope U-13): a paused take holds no recorder and no audio focus; leave → one save and one release per holder; two segments joined in order.

#### LRC-50 · 연결 가림 without ratings (S-4)
**Priority** P1 · **needsExternal** no · scope S-4, D34

- A link into a project whose `라이프 레코더 연결 가리기` is on shows `TODO_COPY_LIB_MASKED_NAME` (interim `이름 가림`, LIB-92) instead of `R-18`. Several masked links of one project collapse into one chip with that label.
- The switch stays in 프로젝트 설정 › `숨기기` (LIB-25 note). The LRC-42 `숨기기` default and the cloud-transcription exclusion keep keying on a stored `r18` rating (D34); a new 0.2.0 project is `all`, so they never fire for it.
- §6 Q17 (two identical `R-18` entries in `프로젝트 ▾`) now reads with the masked label; default unchanged.

### 7.1 Notes on §2 blocks

| Block | **(0.2.0)** |
| --- | --- |
| LRC-01 | mic only on 프로젝트 홈 (LRC-48) |
| LRC-02 … LRC-05 | not offered (LRC-48, N-73 `안 함`) |
| LRC-07 | pause and back rules replaced by LRC-49 |
| LRC-09 | background keeps recording (D32); in-app leave saves (LRC-49) |
| LRC-19 | only home and inbox entries remain |
| LRC-22, LRC-28 hook | masked label per LRC-50 |
| LRC-30 | `EmptyLr` sub `TODO_COPY_LRC_EMPTY_SUB` |
| LRC-44 | `잠금 화면에서 녹음` row hidden |
| LRC-47 | `OnbVoice` widget choice hidden; rating-sheet hook unused |

### 7.2 Tests (JVM)

- **LRC-0.2-01** `entryPoints(screen)`: `HOME` → mic present; `SCRIPT`, `CAST`, `WORLD`, `LIBRARY`, `CANVAS`, `PAGE`, `GALLERY`, `CHECKS`, `MAP`, `TIMELINE` → no mic.
- **LRC-0.2-02** `recorderSettingsRows()` → no `잠금 화면에서 녹음` row; `RecorderSettings.lockScreenRecording` keeps its stored value after a settings write.
- **LRC-0.2-03** a project with `recorderLinkMask = true` → `recorderLinkLabel(project)` equals the masked-name constant and contains no `R-18`.
- **LRC-0.2-04** `MicReleaseTest` as LRC-49.
