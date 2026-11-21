# 60 · 서재, 보호, 파일 (LIB)

Implementation spec for the **library / protection / files** area of Civilight (Android, Jetpack Compose). Feature ids `LIB-01` … `LIB-72`. Engineers implement from this file alone; every Korean string in `code` style is UI copy and must be used verbatim.

## 0. Conventions

**Source precedence** (newest authority first). When two sources disagree, the higher one wins and the conflict is listed in §6 if it matters.

1. Confirmed rule tabs in the planning doc: 19 숨기기와 등급 (confirmed 2026-11-22), 20 항목을 지웠을 때 (2026-11-22), 12 블록과 변화 기록 (2026-11-21), 22, 23, 21, 인물 항목, 저장 파일 양식 (2026-11-21). Memory notes of 2026-11-22 (follow-up decisions on the new boards).
2. Board generator source `design/night-manuscript/gen/*.py` (newest board copy; `scr_new_safe.py` holds the RH* boards).
3. `boards_text.txt` dump (00:08, older than several generators).
4. Main planning text (live rev 71).

**Markers used in this spec**

- `ED` = **engineering default, not from sources.** An implementation choice the sources do not make. Use it unless the product owner says otherwise; it is never user-visible copy.
- `copy: not in sources` = a state needs a label that no source gives. Do not invent Korean copy; leave a TODO string resource `TODO_COPY_<id>` and raise it.
- Board names are the generator names without the `NmDark`/`NmLight` prefix (e.g. `Library`, `RHRatingSheet`). Every board exists in dark and light.
- Rule references: `R19 §3` = rule tab 19, section 3. `FMT §5` = 저장 파일 양식 tab, section 5. `MAIN` = main planning text. `CF §5` = 인물 항목 tab.
- **needsExternal rule**: `yes` when the feature depends on something outside the app's own process: a network/cloud service (Google Drive), home-screen surfaces (widgets, quick settings tile, launcher pinning), speech-to-text or an ML model. `no` for in-app platform APIs (BiometricPrompt, `FLAG_SECURE`, task description label, intent filters, app-posted notifications, Storage Access Framework).
- **Clock**: all date logic takes an injected `Clock`. Test cases use `now = 2026-11-22T09:00+09:00` (Asia/Seoul) unless stated.
- **Separators**: UI fragments are joined with ` · ` (space, middle dot, space). No em dash, no `/`, no comma lists in UI copy.
- **Vocabulary** (fixed): 씬 (never 장면 in UI), 지우기 = to trash, 삭제 only for permanent (`영구 삭제`, `30일 뒤 삭제`), 되살리기 (never 복원 in labels, except the board title `불러오는 중 · 복원`), 고르기, 바꾸기, 고치기, 건너뛰기, 연결 (recorder link; replaces old ‘붙일 곳’), 넣기.

## 1. Scope

**This area owns**

- 서재 (project list) in all layouts, the project ⋯ menu, project delete, card view, 최근 연 파일, cross-project search scope rules, splash → 서재 start routing.
- 새 프로젝트 four-step flow and the 만든 직후 checklist.
- 온보딩: first-run setup, walkthrough, read-only example project 「잿빛 새벽의 등불」 and its deletion, the module tooltip-tour framework, one-line first-use tips, 도움말 · 안내 다시 보기.
- 수위 등급 (ALL · R-15 · R-18) and the four per-project protections; the rating-change `바뀌는 것` sheet; 설정 · 수위 등급 defaults.
- Hiding: hidden projects (group, unlock, re-lock, lock screens), the common hidden-item model and its masking contract, app lock, capture blocking, recents preview masking, name masking in notifications/widgets/recents.
- 휴지통 as a host (list, filters, countdown, 영구 삭제, 비우기, 30-day purge, name tags) and the 되돌리기 toast host.
- 내보내기 sheet and the four outputs (대본 PDF, 대사 표 CSV, 게임 엔진 JSON, 프로젝트 파일 `.civilight`), the `.civilight-recorder` library file.
- The `.civilight` file format: package layout, versioning, unknown-content preservation, encryption, open-from-file, newer-file refusal, partial import of damaged files.
- Google Drive auto backup, 복구 암호, 백업에서 되살리기, restore progress, backup failure.
- App settings screens (설정, 설정 · 숨기기, 설정 · 수위 등급, 설정 › 보관, 설정 · 펼친 화면 배치, 설정 · 움직임 · 투명도, the container of 설정 · 지도 · 이미지 · 연표) and the 프로젝트 설정 screen container.
- Shared empty / error / loading state patterns (boards on page 상태 · 보관 · 잠금).

**Handed off to other areas** (named by module; this spec only states the contract they must honour)

- **Script editor**: blocks, block numbers, voice-ID generation and its 자동 · 고정 display, scene head, rendering of hidden scenes inside the script, 근거 지워짐. This spec triggers voice-ID fixation on export (LIB-46).
- **Cast**: character fields and bundles (definitions), delete-impact sheet (`RCDeleteImpact`), merge (`RCMerge`), relationships, speaker picker, cast-list rendering of hidden characters. Restore semantics of a character from trash.
- **World / Map**: places, terms, factions, maps, pins; their hidden/deleted rendering; image picker and crop (`ImagePick`, `ImageCrop`), preset images.
- **Timeline / place-time**: scene 때 resolution (needed by JSON export), world-calendar editor (opened from LIB-11), 때 표기, travel modes, day/time bands rows in 프로젝트 설정.
- **Life Recorder**: 기록함, recording, records' own hiding (`LrHide`/`LrLocked`…), recorder settings screen (`LrSettings`), widget and quick-settings tile, link (`연결`) model. This spec owns only: the 서재 library row, the start-material link step, the `.civilight-recorder` file, and the recorder-link masking value.
- **Project home / story / check / search**: 프로젝트 홈 (`Home`), 스토리 구조, 점검 센터, the search engine. This spec owns the cross-project scope rule and the `시작해 볼 것` checklist block on the new project's home.
- **Foundation / design system**: theme tokens, glass bars, sheet/toast components, motion tokens, fold/unfold state preservation, window-size classes.

**Explicitly not built** (sources rejected or never drew them): coach marks (onboarding comparison style A; settled: walkthrough + tooltip tours), the 처음 설정 4/4 "무엇부터 할까요" screen (`a_ready` exists in `scr_onb.py` but is not a board), Notion/Word import, own-server sync. (Multi-project export is stated in FMT §6 but has no drawn entry point: LIB-72, P2.)

## 2. Features

### 2.1 서재

#### LIB-01 · 서재 목록 (커버 화면)
Priority **P0** · needsExternal **no**

The app's first screen after splash (and after the app-lock screen if on). One scroll column under the glass bars.

- **Header** (`GlassBars` top): small label `Civilight`, title `서재`, right: `+` icon button → LIB-09 (새 프로젝트). While the hidden group is open (LIB-32) the right slot shows `다시 잠그기` (lock icon + label) instead of `+` (board `LibUnlocked`).
- **Header right** also holds the `ViewToggle` 목록 | 카드 (drawn in `LibCards`; MAIN 카드 보기 puts it in the header of every list screen, so it is shown in list view too, left of `+`). See LIB-04.
- **Section** `프로젝트 · N` (N = visible, non-hidden, non-trashed projects). In card view the label is `프로젝트 · N · 숨김 M` (M = hidden projects, shown only when M ≥ 1).
- **Project row** (one per visible project, top border `line`), sorted by last modified descending (`ED`):
  - title (Nanum Myeongjo 800 20px), then a small line: rating badge (LIB-22) + meta.
  - meta by format: 연재 장편 `<n>장 <e>/<E> · 인물 <c> · <time>` (e.g. `1장 7/10 · 인물 12 · 오늘 08:12`); 단편 `단편 · 씬 <s>개 · <time>` (`단편 · 씬 6개 · 3일 전`); 설정만 `설정만 · 인물 <c> · <time>` (`설정만 · 인물 5 · 11월 2일`). `<n>` = chapter number of the furthest-written episode, `<e>` = that episode's number within the chapter, `<E>` = episode count of that chapter (`1장 7/10`: 1-7 is the furthest episode with content, chapter 1 has 10 episodes). `<time>` = last modified, relative (LIB-01 time format below).
  - right of the text: status `쓰는 중` (accent, 12px) when the project is "being written" (see §6 Q12 for the definition; `ED`: shown on the project that has an episode in status 쓰는 중), then the ⋯ icon → LIB-02.
  - tap row → opens 프로젝트 홈 (handed off) at the last position.
- **Hidden group** (`LockRow`), shown when ≥1 hidden project exists and the group is locked: lock icon, `숨긴 프로젝트 <M>`, sub `이름 가림 · 검색 · 위젯 · 알림 제외`, button `지문으로 보기` → LIB-32. Never shows names, covers, counts of scenes or ratings.
- **Library section**: section label `라이브러리 · 프로젝트 밖`, one row: mic icon tile, `라이프 레코더`, sub `기록 <n> · 정리 안 됨 <u>`, chevron → 기록함 (Life Recorder area). With zero records the sub is `기록 0` (board `OnbEmpty`).
- **Footer** `CommandBar` placeholder `모든 프로젝트에서 찾기` + mic (mic → Life Recorder recording, handed off). Search behaviour: LIB-07.
- **Empty**: no projects → LIB-70 (서재 empty card).
- **Time format** (`ED`, derived from boards): same day `오늘 HH:mm`; previous day `어제`; 2–6 days `N일 전`; 7+ days `M월 D일`; a different year `YYYY년 M월 D일`.
- Masking: hidden projects never appear as rows; R-18 projects appear with their real name and red badge (R19 §3 row 서재 행).

Sources: MAIN 서재 · 숨기기 · 수위 등급; R19 §3; boards `Library`, `LibUnlocked`, `OnbEmpty`; `scr_lib.py`.

#### LIB-02 · 프로젝트 ⋯ 메뉴
Priority **P0** · needsExternal **no**

Bottom sheet (`Sheet`) from a row's ⋯ (or a card's ⋯). Header: project name (h2) + `닫기`; sub: rating badge + `<format> · 씬 <s>개 · 인물 <c> · 페이지 <p>` (e.g. `단편 · 씬 6개 · 인물 4 · 페이지 3`). Rows (icon + title + sub), in this order:

| Row | Sub | Action |
|---|---|---|
| `열기` | `마지막 위치` | Open the project at its last position (last block/screen). |
| `이름 바꾸기` | — | Inline rename sheet; name required, NFC-normalized, trimmed (`ED`). copy for the rename field: not in sources (reuse field label `이름` from LIB-09). |
| `수위 등급` | `ALL(R-12) · R-15 · R-18 · 지금 <rating>` | Opens a 3-cell rating picker (same component as LIB-09) and, on a different choice, the `바뀌는 것` sheet (LIB-24). Same rating → nothing. |
| `프로젝트 숨기기` | `프로젝트 전체 · 지문 · PIN으로 열림` | Hides the project (LIB-31). For a hidden project (only reachable while the group is open) this action is `숨김 풀기` (inline on the row, LIB-31). |
| `백업 · 내보내기` | `Google Drive · 파일` | Opens 내보내기 (LIB-41) for this project. (What "Google Drive" means here is §6 Q16.) |
| `지우기` | `휴지통 · 30일 뒤 삭제` | Project delete confirm (LIB-03). |

The example project (LIB-17) shows only `열기` (read-only) — `ED`; it is deleted by LIB-18, never by this menu.

Sources: boards `LibMenu`; R19 §2 (rating change entry), MAIN 수위 등급.

#### LIB-03 · 프로젝트 지우기 확인
Priority **P0** · needsExternal **no**

Centered dialog over the 서재 (shade behind).

- Title `‘<name>’ 지우기` (e.g. `‘유리 정원’ 지우기`).
- Rows: `씬 · 인물 · 페이지` → `<s> · <c> · <p>` (`6 · 4 · 3`); `이동` → `휴지통 · 30일 보관`.
- `확인: 프로젝트 이름 입력` + text field. The confirm button is enabled only when the typed text equals the project name after NFC normalization (`ED`: exact match, no trimming of inner spaces, leading/trailing whitespace trimmed).
- Buttons `취소` (ghost) · `휴지통으로` (fill `r18` / `onr18` — destructive confirm is the only place `r18` fills a button).
- On confirm: project moves to 휴지통 as one entry (LIB-38), disappears from 서재, 최근 연 파일, search, widgets; Drive backup keeps its existing versions (no upload of a trashed project, `ED`).
- Hidden project delete (only possible while the group is open): same dialog; the trash row later shows `숨긴 프로젝트` (LIB-38).
- Rule 20 §4 explicitly keeps this name-typing confirm only for projects; items inside a project use the impact sheet (cast/world areas).

Sources: MAIN 저장 · 백업 · 내보내기 (지우기); R20 §4 table row 프로젝트 지우기; board `ConfirmDelete`.

#### LIB-04 · 목록 | 카드 보기 (서재)
Priority **P1** · needsExternal **no**

- `ViewToggle` in the header: segmented `목록` | `카드`; selected cell `pri` fill. The chosen view is remembered **per screen** (persist in AppSettings `viewByScreen["library"]`).
- **Card view** (board `LibCards`, cover): section `프로젝트 · N · 숨김 M` with a page indicator (pager dots, one per card); one horizontal snapping row of `ProjectCard`s 3:4 (cover 236×296 on cover screen): cover image (or preset/initial-letter card), bottom dark gradient, rating badge, title, one meta line, ⋯ at the top-right; optional status chip (`쓰는 중`, and `홀로` when the cover's holo is on, joined: `쓰는 중 · 홀로`).
- Hidden projects: one **lock card** at the end of the row: lock icon, `숨긴 프로젝트 <M>`, `이름 · 표지 가림`, button `지문으로 보기` (→ LIB-32). Never a per-project card while locked (R19 §1 row 가로 카드 줄 fixes `MCarousel`, which wrongly shows `R-18 · 검은 서약`).
- Second row `최근 연 파일` (LIB-05) of smaller cards (cover 150×100): image, kind line `<kind> · <project>` (e.g. `씬 · 잿빛 새벽의 등불`), title (`1-7 씬 3 · 꺼진 등불`), time (`오늘 08:12`).
- Then the library section (`라이프 레코더` row) as in LIB-01.
- Inner screen card view (`LibCardsWide`): bigger cards (236×336), a trailing `새 프로젝트` card (outlined, `+` and label `새 프로젝트`) → LIB-09, and up to 4 recent cards (adds e.g. `용어 · 잿빛 새벽의 등불 · 오리지늄 등불 · 4일 전`).
- Motion (handed to foundation, values from MAIN 모션 table): list → card thumbnails grow in place 360 ms, text leaves first (accelerate 160 ms); carousel snaps to nearest card (decelerate 320 ms), image parallax 8 %, haptic tick on snap; reduce motion → no parallax, snap only.

Sources: MAIN 카드 보기 · 이미지; boards `LibCards`, `LibCardsWide`, `MCardView`, `MCarousel`; R19 §1/§7 (가로 카드 줄); DS `ProjectCard`, `ViewToggle`.

#### LIB-05 · 최근 연 파일
Priority **P1** · needsExternal **no**

- Records every open of a scene, character, page or term (and place/faction cards, `ED`) as a `RecentItem` (§3). Shown newest first in the card view (3 on cover, 4 on inner screen).
- Masking (R19 §3 + §6):
  - item in a **hidden project** → never listed (removed immediately when the project becomes hidden, even while unlocked).
  - **hidden item** (character/term/page/scene/place/faction) → never listed, locked or open (R19 §6 row 최근 연 파일: `빠짐` in both columns).
  - project with **이름 가리기** on (R-18 default) → the project name in the kind line is replaced by `R-18 프로젝트`; the item title line is omitted (`ED`; the title would reveal content; replacement copy: not in sources).
  - trashed project/item → not listed.
- Raising a project to R-18 updates existing entries immediately (presentation is computed, not stored — R19 §2 "보이는 방식").

Sources: MAIN 카드 보기; R19 §2 table row "R-18로 올리기 전에 만들어진 것", §3, §6; boards `LibCards`, `LibCardsWide`.

#### LIB-06 · 펼친 화면 서재
Priority **P1** · needsExternal **no**

Window-size class Medium/Expanded (foundation). Layout chosen in 설정 · 펼친 화면 배치 (LIB-66):

- **목록 + 상세** (default, board `TwoLibrary`): left pane = 서재 list exactly as LIB-01 (390 wide); right pane = 프로젝트 홈 of the selected project (handed off). Selecting a row updates the right pane; the hidden group row opens LIB-32 in place.
- **가운데 한 칸** (board `ColLibrary`): the LIB-01 column centered at reading width 560, side margins empty.
- Card view on the inner screen: `LibCardsWide` (LIB-04).
- Fold/unfold keeps the selected project and scroll (foundation rule "만지던 것은 제자리").

Sources: MAIN 커버 화면과 펼친 화면; boards `TwoLibrary`, `ColLibrary`, `LibCardsWide`, `LayoutSettings`.

#### LIB-07 · 모든 프로젝트에서 찾기 (범위 규칙)
Priority **P1** · needsExternal **no**

The 서재 command bar searches across all projects (engine and result rendering: search area). This area defines **which projects/items are in scope**:

- Hidden project, group **locked** → excluded; results end with `숨긴 프로젝트 제외`. Group **open** → included only if 설정 · 숨기기 `검색 · 명령 결과` is on (LIB-64; default on).
- R-18 project not hidden → included, result rows carry the R-18 badge (R19 §3).
- Hidden items in any project: locked → card results excluded, text of visible scenes still searchable, footer `숨긴 항목 제외`; open → all (R19 §6 row 검색 · 명령).
- Trashed projects/items → excluded (R20 모서리 찾기).
- Empty result: LIB-70 (`결과 없음` · `‘<query>’ · 숨긴 프로젝트 제외`).
- Capture blocking applies while any result row from a capture-blocked project is visible (LIB-26).

Sources: R19 §3, §6; R20 §2 모서리; boards `EmptySearch`, `Library` footer.

#### LIB-08 · 스플래시와 시작 흐름
Priority **P1** · needsExternal **no**

- Android 12+ SplashScreen API: background = theme `bg`, no icon plate, the manuscript-lines mark (three bars + accent caret); app exit animation moves the mark up while 서재 rises; total ≤ 1 s; reduce motion → mark → 서재 fade 120 ms (board `SplashStrip`: 0 ms 표식만, 250 ms 줄이 그어짐, 700 ms 표식 위로 · 서재가 아래에서, 1 s 서재 · 입력 가능).
- Routing after splash: first run (OnboardingState.firstRunCompleted = false) → LIB-15 환영; app lock on → LIB-35 lock screen; else → 서재 (LIB-01). A pending `.civilight` open intent (LIB-52) is handled after unlock.
- API 29–30 (no SplashScreen API): `ED` use the androidx core-splashscreen compat library.

Sources: MAIN 움직임 (스플래시); boards `Splash`, `SplashStrip`.

### 2.2 새 프로젝트

Four steps, cover screen full-screen pages with a 4-segment progress bar at the top (current segment `acc`, done `ink2`, todo `line`), small label `새 프로젝트 · k/4`, title, body, bottom glass bar with buttons. Values persist while moving back and forth; `취소` on step 1 discards everything (no confirm, `ED`). Nothing is created until `만들기` on step 4.

#### LIB-09 · 1/4 이름과 형식
Priority **P0** · needsExternal **no**

- Title `이름과 형식`.
- Field `이름` (required; `다음` disabled while blank after trim). Field `한 줄 소개 (선택)` (placeholder `—`).
- `형식` radio cards (title + sub): `연재 장편` · `장 · 에피소드 · 씬으로 길게` (default selected); `단편` · `씬만으로 짧게`; `설정만` · `원고 없이 세계와 인물만`.
- Row `수위 등급` with right note `나중에 바꾸기 가능`; 3-cell picker: `ALL` / `12세 이하`, `R-15` / `15세 이상`, `R-18` / `성인` (mono label + small gloss). Selected ALL/R-15 cell = `pri` fill; selected R-18 = `r18` fill; unselected R-18 cell has an `r18` outline and red text. Default selection: §6 Q11 (`ED`: `ALL`).
- When **R-18** is selected, a tinted block appears under the picker: header `R-18 기본 동작 · 설정에서 바꾸기`, then one checked line per R-18 default that is **on** in 설정 · 수위 등급 (factory: `프로젝트 숨기기 켬`, `화면 캡처 · 최근 앱 미리 보기 가림`, `위젯 · 알림에 이름 대신 ‘R-18 프로젝트’`; the recorder-link default line: copy not in sources). If the R-18 hide default is on and no 복구 암호 is set, add the ◇ line `자동 백업에서 빠짐` (R19 §7 row 새 프로젝트 · R-18 선택; full line copy beyond these words: not in sources).
- Buttons `취소` · `다음`.
- The selected rating fills the step-4 hide toggle (LIB-12) and all protections from 설정 · 수위 등급 (LIB-23).

Sources: MAIN 새 프로젝트; R19 §2 table row 새 프로젝트, §7; boards `NewName`, `NewNameR18`; `scr_onb.py b_name`, `scr_rating.py new_r18`.

#### LIB-10 · 2/4 구조
Priority **P0** · needsExternal **no**

- Title `구조`, description `단계 이름은 나중에 바꾸기 가능`.
- Radio cards: `장 › 에피소드 › 씬` · `서브컬처 게임 메인 스토리처럼` (default); `막 › 장면` · `연극 · 짧은 이벤트`; `직접 정하기` · `단계 수와 이름을 직접 (최대 4단계)`.
- `직접 정하기` expands an editor: 1–4 level-name fields with add/remove (`ED` layout; copy: not in sources). Level names required, ≤4 levels.
- `미리 보기` tree (tinted box): for 3 levels `장 1` › `에피소드 1-1` › `씬 1`, `씬 2`, `에피소드 1-2`; for 2 levels `<L1> 1` › `<L2> 1`, `<L2> 2`. Updates live with the choice.
- Buttons `이전` · `다음`.
- For 형식 단편 / 설정만 see §6 Q10 (`ED`: step shown unchanged).
- The chosen structure becomes `Project.structure` (editable later in 프로젝트 설정 › 단계 이름, LIB-69).

Sources: MAIN 새 프로젝트, 스토리 구조; boards `NewStructure`, `NewWide`.

#### LIB-11 · 3/4 시간
Priority **P0** · needsExternal **no**

- Title `시간`. Radio cards: `연도만` · `예: 1200년` (default); `세계 달력 만들기` · `달 이름 · 한 달의 날 수 · 요일을 직접` (opens the world-calendar editor of the Timeline area, then returns); `시간 없이` · `연표 끔`.
- Field `이야기가 시작되는 해` (mono, integer, e.g. `1200`), hint `생일이 있으면 나이 자동 계산`. Hidden when `시간 없이` (`ED`).
- Buttons `이전` · `다음`.

Sources: MAIN 새 프로젝트, 연표; CF §6 (age calc per time mode); board `NewTime`.

#### LIB-12 · 4/4 시작 재료
Priority **P0** · needsExternal **no**

- Title `시작 재료`.
- Field `주인공 (선택)` → on create, a character with that name is created (fixed field 이름; Cast area). Blank → none.
- Field `첫 씬 제목 (선택)`, placeholder `비워 두면 ‘씬 1’` → title of the first scene. 형식 설정만 creates no scene (`ED`).
- `라이프 레코더 기록 연결 · <total>개 중 <k>개 고름`: checkbox list (rounded-square checkboxes) of 기록함 records in state 정리 안 됨, newest first (`ED` for the filter), each row `음성 0:24 · 오늘` / first line of text. Checked records get a **link** (`연결`) to the new project on create; their content is not inserted anywhere (R23 table "시작 재료").
- Row `인물 항목` · `출생 · 나이 · 생일 · 소속 · 성격` · `기본 ›` → bundle picker: `기본` (default, always; never guessed from 형식), `게임 프로필`, `비어 있음`, `다른 프로젝트에서` (lists other projects; hidden projects only while the group is open; copies the field set only, no values). Field definitions themselves: Cast area (CF §5).
- Row `이 프로젝트 숨기기` · `지문 · PIN으로 열림` + switch. Initial value = the protections derived from the step-1 rating (R-18 factory → on; else off). If the user turns it off, that is final; the other three protections stay as derived (R19 §2 table).
- Buttons `이전` · `만들기`.
- `만들기`: creates the project (keys, structure with chapter 1 › episode 1-1 › first scene when the format has a manuscript, time mode, field bundle, protections, links), then opens LIB-13. If hidden, the project goes into the hidden group but stays unlocked for this session until the next re-lock (same as R19 §2 "지금은 열린 채").

Sources: MAIN 새 프로젝트; R19 §2 table row 새 프로젝트; R23 §4 table (시작 재료); CF §5; board `NewStart` (`scr_onb.py b_start`).

#### LIB-13 · 만든 직후 · 시작해 볼 것
Priority **P1** · needsExternal **no**

The new project's home (Project home area) shows, on top, this block:

- Small `‹ 서재  ·  새 프로젝트`, title (project name), line: rating badge + `<형식> · <구조> · <시작 해>년` (e.g. `연재 장편 · 장 › 에피소드 › 씬 · 1200년`). When the project was created hidden-off from an R-18 default, the summary includes `숨김 · 끔` (R19 §2; exact placement `ED`: appended to this line).
- `바로 쓰기` (accent micro-label) + `1장 · 에피소드 1-1 · 씬 1` + button `쓰기` → opens the script at the first scene (uses the actual level names and first scene title).
- `시작해 볼 것 · <done>/4` + `목록 닫기` (dismisses the checklist for good). Items (checkbox square, title, sub, chevron):
  1. `주인공 만들기` · `<name> · 완료` when a protagonist was given (struck through, accent box) else sub copy: not in sources; tap → Cast.
  2. `연결된 기록 <k>개 정리하기` · `기록함` → 기록함 filtered to this project (R23). Hidden when k = 0 (`ED`).
  3. `세계 용어 하나 적기` · `공개 시점 포함` → World › new term.
  4. `첫 대사 쓰기` · `대사 블록 · 화자 고르기` → script.
- Items complete automatically by fact (a character exists; all linked records are 넣음/끝냄; ≥1 term exists; ≥1 dialogue block exists).

Sources: MAIN 새 프로젝트 ("시작해 볼 것"); R23 §4 table (만든 직후); board `NewReady`.

#### LIB-14 · 새 프로젝트 펼친 화면
Priority **P1** · needsExternal **no**

Inner screen (board `NewWide`): left column (380) with progress bar + `취소`, small `새 프로젝트 · k/4`, title, the step's fields (step 2 also shows field `이름` read-only at top and the label `나누는 방법` above the radio cards); right `aside` (300, `raise` panel): `미리 보기`, project name, format, live tree, note `실시간 미리 보기`. Buttons `이전` · `다음` bottom-right under the left column. Same data and validation as LIB-09…12.

Sources: board `NewWide`.

### 2.3 온보딩

Three layers (MAIN 온보딩): first-run walkthrough, module tooltip tours on first entry, one-line first-use tips. All skippable; all can be replayed. Coach marks are not used.

#### LIB-15 · 첫 실행 설정
Priority **P1** · needsExternal **yes** (Drive sign-in, home-screen widget)

Shown once (OnboardingState). Pages:

1. **환영** (`OnbWelcome`): the sample manuscript blurred behind glass (masked top-transparent); mono `CIVILIGHT`; title `문명의 빛`; text `세계를 만들고, 인물을 세우고,` / `대사를 쓰는 곳.` (content, kept as written); small `설정 3단계 + 예시 · 모두 나중에 바꾸기 가능`; buttons `시작하기` (primary) → page 2; `백업에서 되살리기` (ghost) → LIB-59 (on success, onboarding is marked complete and the example is never installed, `ED`).
2. **화면 · 1/4** (`OnbLook`): `처음 설정 · 1/4`, title `화면`; three theme swatches `기기 설정` · `라이트` · `다크` (selection = AppSettings.theme, applied live); `글자 크기` slider between small and large `가`, preview line `“멈추지 마. 저건 등불이 아니야.”`; top-right `건너뛰기`; button `다음`.
3. **백업과 잠금 · 2/4** (`OnbSafe`): row `Google Drive 자동 백업` · `하루 한 번 · 와이파이 · 원본은 기기에` + button `연결` (starts Google sign-in + Drive authorization, LIB-57); row `앱 잠금` · `열 때 지문` + switch (board shows it on; `ED` default off until the user turns it on, see §6 Q13); note `나중에 연결: 설정 › 보관`; buttons `나중에` · `다음`; `건너뛰기`.
4. **라이프 레코더 · 3/4** (`OnbVoice`): card `라이프 레코더` · `프로젝트 밖의 따로 된 기록함`, text `말로 기록 → 글로 변환 → 원고 · 카드에 넣기`; choices `마이크 허용` · `녹음할 때만 사용` (tapping it explains first, then fires the system permission request — the reason is shown before the system prompt) and `홈 화면 위젯 추가` · `앱을 열지 않고 바로 녹음` (requests widget pinning; widget itself: Life Recorder area); buttons `나중에` · `다음`; `건너뛰기`.
5. Then the walkthrough (LIB-16), then the example (LIB-17), step `4/4`.

Button semantics (`ED`, §6 Q14): `나중에` = leave this step's choice unset and go to the next page; `건너뛰기` (top-right) = skip the rest of 처음 설정 and go straight to the walkthrough. Every choice is also in 설정.

Sources: MAIN 온보딩 첫 실행; boards `OnbWelcome`, `OnbLook`, `OnbSafe`, `OnbVoice`; `scr_onb.py`.

#### LIB-16 · 워크스루 3장
Priority **P1** · needsExternal **no**

Full-screen pages with a live demo card (radius 24, 420 high) and a 3-dot progress (mono `k/3`), top-right `건너뛰기`; banner `예시 · 읽기 전용 · 온보딩 후 삭제` under the demo.

| Page | Title | Sub | Demo | Buttons |
|---|---|---|---|---|
| 1/3 (`Walk1`) | `밀어서 탭 고르기` | `블록 왼쪽으로 → 시간 · 참고 · 속성` | 하르트 block slides left revealing `◆ 시간` · `참고` · `속성` | `다음` |
| 2/3 (`Walk2`) | `시점 렌즈` | `고른 시점까지만 보기 · 앱 전체` | 리엔 with chip toggling `시점 · 1-7` ↔ `시점 · 1-2`; rows 나이 `17세`; 소속 `등불 결사 ◆` ↔ `없음`; 하르트와 `경계` ↔ `만나기 전` | `이전` · `다음` |
| 3/3 (`Walk3`) | `떠오르면 말로` | `명령 칸 마이크 · 라이프 레코더` | recording glow + `듣는 중` + `2장 첫 장면은 비 오는 상층 관문에서` | `이전` · `예시 둘러보기` |

`건너뛰기` or `예시 둘러보기` → LIB-17. Demos are looping animations (reduce motion: static end state, `ED`).

Sources: MAIN 온보딩 (워크스루 3장); concept memory (walkthrough chosen, coach marks not used); boards `Walk1`–`Walk3`.

#### LIB-17 · 예시 프로젝트 (읽기 전용)
Priority **P1** · needsExternal **no**

- 「잿빛 새벽의 등불」 ships inside the app as a `.civilight` asset (`ED`; the format tab already keeps this file per format version, LIB-56). Installed on first run with `isExample = true`, `readOnly = true`; uses the 게임 프로필 field bundle so 리엔 shows 신장 · 종족 (CF §5).
- **예시 둘러보기 · 4/4** (`OnbExample`): floating glass banner at the top with the 4-segment bar (segment 4 current), lock icon, `예시 프로젝트 · 읽기 전용`, right `온보딩 후 삭제`. Content: title, one-liner `꺼져 가는 하층 구역, 마지막 등불을 지키는 결사의 이야기.`; section `둘러볼 곳` with three rows → `스크립트 · 1-7 씬 3` · `대사 · 나레이션 · 연출 지시 · 변화 표식`; `인물 · 리엔` · `시점에 따라 바뀌는 필드와 관계`; `세계 · 오리지늄 등불` · `용어 공개 시점과 점검`; section `Ⅰ 1장 · 등불지기` with episode rows (`1-6 잿더미 시장 완료`, `1-7 잿빛 새벽의 등불 씬 4개`, `1-8 — 미정`). Bottom primary `온보딩 끝내기` (LIB-18).
- **예시 스크립트** (`OnbExampleScript`): the normal script screen of 1-7 씬 3 with the banner, header label `1-7 · 씬 3 · 2일째 새벽`, no command bar; bottom: `읽기 전용 · 편집 불가`, buttons `예시 홈으로` · `온보딩 끝내기`.
- Read-only enforcement: every edit entry point (typing, block add, swipe-to-panel actions that change data, ⋯ menus that write, drag) is disabled in a read-only project; navigation, lens, panels and search work. Module tours (LIB-19) run on it.
- Not listed in 서재 as a normal project (`ED`); not in Drive backup; not exportable.

Sources: MAIN 온보딩 (예시 둘러보기); CF §5 예시 프로젝트; boards `OnbExample`, `OnbExampleScript`, `CoachScript`/`Tour*` banners.

#### LIB-18 · 온보딩 끝내기 · 예시 삭제
Priority **P1** · needsExternal **no**

- `온보딩 끝내기` (from LIB-17 screens) permanently deletes the example project (not to 휴지통 — the board uses `삭제`), sets `firstRunCompleted = true`, and opens the empty 서재 (`OnbEmpty`): section `프로젝트 · 0`; outlined card `프로젝트 없음`, text `프로젝트 = 원고 · 인물 · 세계 · 페이지 한 묶음`, buttons `새 프로젝트` (→ LIB-09) · `백업에서 되살리기` (→ LIB-59); below it a small trash icon + `예시 프로젝트 삭제됨` (shown once, `ED`: until the first project exists); library row `라이프 레코더` · `기록 0`.
- If the user leaves the app mid-onboarding, the next launch resumes at the same page (`ED`).

Sources: MAIN 온보딩 ("‘온보딩 끝내기’를 누르면 예시 프로젝트를 지우고 빈 서재로 간다"); board `OnbEmpty`.

#### LIB-19 · 모듈 안내 (툴팁 투어)
Priority **P1** · needsExternal **no**

Framework this area owns; each module's step content is given below from the boards (module areas supply the target anchors).

- Trigger: first entry into a module (per module flag in OnboardingState). Shade over the screen with one cut-out hole around the target (6 px padding, 12 px radius, 1.5 px `acc` frame); a tooltip card (`raise`, radius 16) with a 3- or 4-segment bar, mono `<label> · k/n`, title, sub, `건너뛰기` (left, text) and `이전` (from step 2) · `다음` / `마치기` (last step).
- `건너뛰기` marks the whole module tour as seen.
- Steps drawn in the boards:

| Module | Steps | Drawn steps (title · sub) |
|---|---|---|
| 스크립트 | 4 | 1/4 `명령 칸` · `이동 · 검색 · 블록 추가 · 마이크` (`Tour1`); 2/4 `블록을 왼쪽으로` · `시간 · 참고 · 속성 → 옆 패널 탭` (`Tour2`); 4/4 `시점 렌즈` · `고른 시점까지의 인물 · 관계 · 용어`, button `마치기` (`Tour4`); 3/4 not drawn |
| 인물 | 3 | 1/3 label `인물 안내`: `시점 렌즈` · `이 시점까지의 필드 · 관계만` (`TourCast`) |
| 세계 · 연표 | 3 | 1/3 label `세계 안내`: `공개 시점` · `앞에서 쓰이면 ▲ 점검 센터` (`TourWorld`) |
| 라이프 레코더 | 3 | 2/3 label `레코더 안내`: `연결` · `보던 곳 · 넣기 전까지 내용은 그대로` (`TourLr`, `scr_decide.py`) |
| 지도 만들기 | 3 | 1/3 label `지도 안내`: `도구 줄` · `이동 · 핀 · 영역 · 경로 · 라벨 · 레이어 · 고른 것만 동작` (`TourMap`) |
| 인물 동선 · 장소와 때 | 3 | 1/3 label `장소와 때 안내`: `장소와 때` · `직전 씬 기준 · 시간대 · 정확/대략 → 이동 점검` (`TourWhen`) |
| 서재 · 숨기기 | 3 | none drawn (§6 Q15) |

Undrawn steps: copy not in sources.

Sources: MAIN 모듈 안내; concept memory (settled 2026-11-21); boards `Tour1`, `Tour2`, `Tour4`, `TourCast`, `TourWorld`, `TourLr`, `TourMap`, `TourWhen`.

#### LIB-20 · 기능 첫 사용 한 줄 안내
Priority **P1** · needsExternal **no**

- A one-line tip shown the first time a feature is used, once per tip id, never again. Global switch `기능 첫 사용 짧은 안내` · `한 줄 · 한 번만` (LIB-21; default on).
- Presentation `ED`: the `Toast` component without an action, 1 line. The list of tips and their copy: not in sources (each module area may register tips).

Sources: MAIN 온보딩 (세 층); board `HelpReplay`.

#### LIB-21 · 도움말 · 안내 다시 보기
Priority **P1** · needsExternal **no**

Screen (head `설정` / `도움말 · 안내 다시 보기`):

- Section `처음 안내`: row `워크스루` · `3단계 · 봤음|아직 안 봄` + tag `다시 보기` (replays LIB-16).
- Section `모듈 안내 · 처음 들어갈 때`: rows `스크립트` (4단계), `인물`, `세계 · 연표`, `라이프 레코더`, `서재 · 숨기기`, `지도 만들기`, `인물 동선 · 장소와 때` (3단계 each), each with `<n>단계 · 봤음` or `<n>단계 · 아직 안 봄` and tag `다시 보기` (resets that module's flag; the tour runs on next entry, `ED`).
- Row `기능 첫 사용 짧은 안내` · `한 줄 · 한 번만` + switch.
- Button `모든 안내 처음부터` (resets every tour flag and every tip id; does not reinstall the example project).
- Replays that need the example project: see §6 Q9.

Sources: MAIN 다시 보기; board `HelpReplay` (`scr_decide.py help_replay`).

### 2.4 수위 등급과 보호

#### LIB-22 · 수위 등급 (값 · 배지 · 표기)
Priority **P0** · needsExternal **no**

- Values: `ALL` (gloss `12세 이하`; long form in menus `ALL(R-12)`), `R-15` (`15세 이상`), `R-18` (`성인`). Chosen at creation (LIB-09), changed later from 서재 ⋯ › 수위 등급 (LIB-02) or 프로젝트 설정 (LIB-25); both open the same sheet (LIB-24).
- Badge (`Tag` rating badge): mono 10 px, small radius; ALL and R-15 = `ink2` outline; R-18 = `r18` red. Shown on 서재 rows/cards, project ⋯ sheet, project home line, new-project summary, export sheet subtitle.
- Rating itself changes no behaviour. Behaviour comes from the four stored protection values (LIB-23). Exception: `내보내기 표지에 등급 표시` (all ratings, default on) prints the rating on the PDF cover (LIB-43).

Sources: MAIN 수위 등급 table; R19 §2 rule; DS README 숨기기 · 등급, Tag.

#### LIB-23 · 보호 값과 등급별 기본값 (설정 · 수위 등급)
Priority **P0** · needsExternal **no**

**Per-project protections** (stored on the project, §3 `Protections`):

| Protection | Effect when on | Kind |
|---|---|---|
| `프로젝트 숨기기` (hidden) | LIB-31…34 | **state** — changes only by an explicit choice |
| `화면 캡처 가리기` (captureBlock) | LIB-26 | presentation — applied immediately everywhere |
| `이름 가리기` (nameMask) | LIB-27 | presentation |
| `라이프 레코더 연결 가리기` (recorderLinkMask) | LIB-28 | presentation |

Presentation protections are read live: changing a value immediately changes how existing things are drawn (R19 §2 "보이는 방식").

**설정 · 수위 등급** screen (head `설정` / `수위 등급`, board `RatingSettings`):

- Intro `등급별 기본값 · 프로젝트마다 따로 바꾸기 가능`.
- Group R-18 badge + `성인`: switches `만들 때 숨기기` · `지문 · PIN으로 열림` (on); `화면 캡처 가리기` · `스크린샷 · 화면 녹화 · 최근 앱 미리 보기` (on); `이름 가리기` · `위젯 · 알림 · 최근 항목에 ‘R-18 프로젝트’` (on); `라이프 레코더 연결 가리기` · `기록의 연결에 등급만 표시` (on).
- Group R-15 + `15세 이상`: switch `화면 캡처 가리기` (off).
- Group ALL + `12세 이하`: text `라벨만` (no switches).
- Section `모든 등급`: switch `내보내기 표지에 등급 표시` · `파일 · 인쇄` (on).
- Changing a default affects **only new projects and future rating changes**. After any change in a rating group, a tag appears under that group: `이미 있는 <rating> 프로젝트 <N> · 같이 바꾸기` (drawn for R-18: `이미 있는 R-18 프로젝트 2 · 같이 바꾸기`). Tap → a list of those projects with checkboxes; applying sets the changed value on the checked projects (hide changes go through LIB-31 rules: turning hide off needs authentication). List screen copy: not in sources. Hidden projects appear in that list only while the hidden group is open (`ED`).
- Fixed (not editable) defaults: R-15 hide/name/recorder = off; ALL all = off.

Sources: MAIN 수위 등급 table; R19 §2 rule + table; boards `RatingSettings`, `NewNameR18`; `scr_rating.py`.

#### LIB-24 · 등급 바꾸기 · `바뀌는 것` 시트
Priority **P0** · needsExternal **no**

Opened whenever the rating of an existing project is changed to a different value. Never applied silently. Built fresh every time (no memory of earlier choices).

Layout (`RHRatingSheet`, `RHRatingDown`):

- Top: small `<project name> · 수위 등급` + `닫기`.
- Title: `<from badge> → <to badge>` + `바뀌는 것`.
- Small line: raising → `체크 · <to> 기본값 · 설정 › 수위 등급`; lowering → `체크 · 지금 켜진 보호 · 체크를 풀어야 꺼짐`.
- Four checkbox rows (whole row is the target, rounded-square check):

| Row | Sub (raising) | Sub (lowering) |
|---|---|---|
| `프로젝트 숨기기` | `지문 · PIN으로 열림` | `풀면 바꿀 때 지문 확인` |
| `화면 캡처 가리기` | `스크린샷 · 녹화 · 최근 앱` | `스크린샷 · 녹화 · 최근 앱` |
| `이름 가리기` | `위젯 · 알림 · 최근 항목 ‘R-18 프로젝트’` | `위젯 · 알림 · 최근 항목` |
| `레코더 연결 가리기` | `연결에 등급만 · 기록 <n> 바로 적용` (n = recorder records linked to this project; drawn `기록 1`) | `연결에 등급만` |

- Initial checks: **raising** (to a higher rating) → checked = current value **or** the target rating's default (never pre-uncheck a protection that is on; see §6 Q7). **Lowering** → checked = current values; each row shows a grey right label stating the target rating's default for that row; drawn only as `R-15 기본 · 끔` (all four R-15 defaults are off). The "on" variant (e.g. R-18 → R-15 never needs it, but a custom R-15 capture default could): copy not in sources (`TODO_COPY_LIB24_DEFAULT_ON`).
- Extra ◇ rows (only when the condition holds):
  - `복구 암호 없음` · `숨기면 자동 백업에서 빠짐` · action `정하기 ›` (→ LIB-58) — when `프로젝트 숨기기` is checked and no 복구 암호 is set.
  - `이전 백업 <n> · 암호 없음` · `30일 백업 목록에 그대로` · action `지우기 ›` — when the result will be hidden and Drive holds n > 0 unencrypted versions of this project. `지우기 ›` deletes those Drive versions after a confirm (copy: not in sources). Without the tap they stay (R19 §2 table).
- Note line: raising with hide checked → `바꾸면 ‘숨긴 프로젝트’ 묶음으로 · 지금은 열린 채`; lowering → `내려도 저절로 풀리는 것 없음`.
- Buttons `취소` · `바꾸기`. `바꾸기` is the `pri` button even when raising to R-18 (`r18` is only for the badge and hard-to-undo confirms).
- Apply: rating := to; protections := checked rows. If `프로젝트 숨기기` goes from on to off, `바꾸기` first runs authentication (LIB-32 prompt); cancel/fail → nothing changes and the sheet stays open. If it goes off → on, the project joins the hidden group and stays unlocked until the next re-lock (LIB-33).
- Effects visible at once (examples in §4): 최근 연 파일 entries removed/masked, recorder links show `R-18`, capture block starts on the current screen.

Sources: R19 §2 (rule, example, table, 버린 안); boards `RHRatingSheet`, `RHRatingDown`; memory 2026-11-22 (R-18 raise button stays `pri`).

#### LIB-25 · 프로젝트 설정 › 등급 · 숨기기
Priority **P0** · needsExternal **no**

Section `등급 · 숨기기` of 프로젝트 설정 (board `ProjSettings`):

- `수위 등급` · `ALL · R-15 · R-18` · value `<rating> ›` → picker → LIB-24.
- `이 프로젝트 숨기기` · `지문 · PIN으로 열림` + switch. On: immediate (LIB-31). Off: authentication first.
- `화면 캡처 가리기` · `스크린샷 · 최근 앱 미리 보기` + switch.
- `이름 가리기` · `위젯 · 알림 · 최근 항목` + switch.
- `라이프 레코더 연결 가리기` · `기록의 연결에 등급만 표시` + switch.
- When a value differs from the current rating's default, append to its sub, in `acc`: `<rating> 기본과 다름` (drawn: `R-15 기본과 다름` on 화면 캡처 가리기). Changing a single switch never changes the rating.

Sources: R19 §2 table row "프로젝트 설정에서 동작 하나만 바꾸기", §7; board `ProjSettings` (`scr_decide.py`).

#### LIB-26 · 화면 캡처 가리기
Priority **P0** · needsExternal **no**

- When on for a project, screenshots, screen recording and the system recents preview are blocked **whenever any content of that project is on screen** — not only inside the project: the open hidden group's row for it, cross-project search results, 기록함 rows showing records linked to it, 최근 연 파일 cards of it, the export sheet (R19 §3).
- Screenshot/recording result: black (system).
- Masked-only presentations (e.g. `R-18 프로젝트` label without content) do not trigger it (`ED`).
- `ED` implementation: single-activity app; a `CaptureGuard` state computed from the visible composition; set/clear `WindowManager.LayoutParams.FLAG_SECURE` on the window when the guard changes. Set it before the content is first drawn (no one-frame leak).

Sources: MAIN 수위 등급 (FLAG_SECURE in concept memory); R19 §3; board `Recents`.

#### LIB-27 · 이름 가리기
Priority **P0** · needsExternal **no** (the contract also binds widget code, which is external — Life Recorder area)

- Single function `displayProjectName(project, surface)` used by every surface outside the project itself:

| Surface | hidden project (any rating) | not hidden, nameMask on | otherwise |
|---|---|---|---|
| 서재 row/card | not shown individually (group row/lock card only) | real name + red badge | real name |
| 최근 연 파일, home widget lists | omitted | `R-18 프로젝트` | real name |
| 모든 프로젝트에서 찾기 | omitted while locked (`숨긴 프로젝트 제외`) | real name + badge | real name |
| notifications (backup · restore · export done) | counted as `숨긴 프로젝트 N` | `R-18 프로젝트` | real name |
| system recents card name (task label) | `Civilight` | `R-18 프로젝트` | `Civilight` |
| 휴지통 / 기록함 location text | `숨긴 프로젝트` (restore needs auth) | real name | real name |

- The masked label is the literal `R-18 프로젝트` even if the masked project's rating was lowered with name masking kept on (`ED`; §6 Q8).

Sources: MAIN 수위 등급; R19 §3 table; board `Recents`.

#### LIB-28 · 라이프 레코더 연결 가리기
Priority **P1** · needsExternal **no**

When on, every place in the Life Recorder (chips, 기록함 rows, 정리 중 screen) that shows a link to this project shows only the rating, `R-18` (drawn value), instead of the project/scene name — also while the project is open (R23 §6). Rendering is the Life Recorder area's job; this area provides `recorderLinkLabel(project)`.

Sources: MAIN 수위 등급; R19 §2 example ("‘하르트가 등불을 싫어하는 이유’의 … 바로 ‘R-18’로 보인다"); R23 §6 table.

#### LIB-29 · 시스템 최근 앱 미리 보기와 카드 이름
Priority **P0** · needsExternal **no**

- Capture-blocked project content on screen → preview masked (FLAG_SECURE, LIB-26).
- Hidden project content on screen (any rating, capture block off) → **recents preview only** is masked; screenshots follow that project's capture setting.
- Task label (recents card name): `Civilight` while hidden-project content is on screen; `R-18 프로젝트` while content of a non-hidden name-masked project is on screen; default app label otherwise.
- Board `Recents` caption: `R-18 프로젝트를 연 채로 나가면 · 스크린샷도 검은 화면` / `숨긴 프로젝트는 등급과 상관없이 가림 · 카드 이름 ‘Civilight’`.
- `ED` implementation (verify API levels against current platform docs): API 33+ `Activity.setRecentsScreenshotEnabled(false)` for recents-only masking; API 29–32 have no recents-only API (see §6 Q6; fallback `ED`: FLAG_SECURE while hidden content is visible). Task label via `setTaskDescription(TaskDescription(label))`.
- The board draws `R-18 프로젝트 · 미리 보기 가림` inside the recents card; with FLAG_SECURE the system draws a blank card, so this in-card text cannot be guaranteed (§6 Q6).

Sources: R19 §3 table rows 시스템 최근 앱; board `Recents` (`scr_rating.py recents`).

#### LIB-30 · 알림 문구 가림
Priority **P1** · needsExternal **no**

Notifications posted by this area (backup done, restore done, export done) never contain a hidden project's name; they count hidden projects as `숨긴 프로젝트 N` (number only); name-masked projects appear as `R-18 프로젝트`.

- Backup done, `ED` rule reproducing both R19 §3 examples: if the run included ≥1 hidden project → `백업 끝 · 프로젝트 <n> · 숨긴 프로젝트 <h>` (n = non-hidden projects in the run); else → `백업 끝 · ` + project names joined by ` · ` with name-masked ones as `R-18 프로젝트`. (§6 Q17.)
- Restore done / export done: same masking; copy beyond the masked tokens: not in sources (restoring board: `앱을 닫아도 계속 · 끝나면 알림`).

Sources: R19 §3 table + example; board `Restoring`.

### 2.5 숨기기와 잠금

#### LIB-31 · 프로젝트 숨기기 · 숨김 풀기
Priority **P0** · needsExternal **no**

- **Hide** (⋯ › `프로젝트 숨기기`, 프로젝트 설정 switch, rating sheet, new-project step 4): sets `protections.hidden = true`. Hides everything of the project together: manuscript, cast, world, pages, assets. Effects: removed from 서재 rows (counted in the hidden group), 최근 연 파일, widgets, cross-project search (while locked); notifications count only; recents preview masked; trash/기록함 locations show `숨긴 프로젝트`; Drive backup encrypted with 복구 암호 or excluded (LIB-57).
- Hiding needs no authentication. If done from the locked state it applies immediately; if the hidden group is open, the project appears inside the open group.
- No 복구 암호 set when hiding: the rating sheet shows the ◇ row (LIB-24); for the other entry points see §6 Q18 (copy: not in sources).
- **Unhide**: inline `숨김 풀기` on a row in the open group (`LibUnlocked`), the 프로젝트 설정 switch, or unchecking in the rating sheet. Requires the group to be open and (for the switch/sheet) a fresh authentication. Then the project returns to the normal list.
- Device without a secure lock screen: hiding cannot be protected by PIN; behaviour §6 Q19.

Sources: MAIN 숨기기; concept memory HIDING MODEL; R19 §2–§3; boards `LibMenu`, `LibUnlocked`, `ProjSettings`.

#### LIB-32 · 숨긴 프로젝트 열기 (지문 · PIN)
Priority **P0** · needsExternal **no**

- `지문으로 보기` (group row or lock card) → system biometric prompt titled `숨긴 프로젝트 보기` (board: small `시스템 생체 인증`, title `숨긴 프로젝트 보기`, `지문 센서 터치`, buttons `취소` · `PIN 사용`). Device PIN/pattern/password is the fallback.
- Success → group expands in place (`LibUnlocked`): tinted box, unlock icon, `숨긴 프로젝트 <M> · 열림`, right text by re-lock policy (`앱을 떠나면 잠김` for the default; other policies: copy not in sources), then the hidden projects as normal rows (rating badge, meta) each with `숨김 풀기`. Header right becomes `다시 잠그기`. Motion: blur 7 px → 0, 320 ms; reverse on re-lock (`MUnlock`).
- Cancel → nothing changes. Failure lockout (`ErrBio`): the system shows `지문 인식 5회 실패` · `30초 뒤 다시 · 지금은 기기 PIN` with `취소` · `PIN 사용` (system text; the app does not draw it). After 취소 the user stays on 서재, group locked.
- One authentication opens all hidden projects (`ED`: the group unit). Items hidden inside a project are a separate unlock per project (LIB-36).
- `ED` implementation (verify the authenticator combinations against the androidx.biometric docs for minSdk 29): `androidx.biometric.BiometricPrompt`; API 30+: `BIOMETRIC_STRONG or DEVICE_CREDENTIAL`; API 29: `BIOMETRIC_WEAK or DEVICE_CREDENTIAL` (STRONG + credential is unsupported on API 28–29). With `DEVICE_CREDENTIAL` allowed the system supplies the PIN option itself (no negative button from the app).

Sources: MAIN 숨기기 (열기); boards `LibUnlock`, `LibUnlocked`, `ErrBio`, `MUnlock`.

#### LIB-33 · 다시 잠그는 때 · 다시 잠그기
Priority **P0** · needsExternal **no**

- Policy (설정 · 숨기기, LIB-64), radio: `앱을 떠날 때` · `다른 앱 · 화면 꺼짐` (default); `화면이 꺼질 때만` · `다른 앱 다녀와도 유지`; `연 지 5분 뒤`.
- Re-lock events: ON_LEAVE → app goes to background (other app, home, recents) or screen off; SCREEN_OFF_ONLY → screen off only; FIVE_MIN → 5 minutes after the unlock time, measured on the clock even in foreground (`ED`: checked on resume and by a timer).
- `다시 잠그기` button (header while open) → immediate re-lock.
- Re-lock closes the hidden group, re-locks hidden items of every project (same setting, R19 §6 edge), and if a hidden project was on screen, shows the hidden-project lock screen (LIB-34) instead of its content.
- Item-level re-lock uses the same policy (R19 §6 edge "다시 잠그는 때").

Sources: MAIN 숨기기 ("기본: 앱을 떠날 때"); R19 §6 edge; board `HideSettings`, `LibUnlocked`.

#### LIB-34 · 잠금 화면 (흐린 원고 · 마크만)
Priority **P0** · needsExternal **no**

Lock screens drawn by the app (not the system keyguard):

| Situation | Screen |
|---|---|
| Normal project, app lock due | `AppLock`: last screen blurred (14 px, 0.6 opacity) under glass; mark; `Civilight`; `잠김 · 지문으로 열기`; fingerprint button (`pri`); `기기 PIN 사용` |
| Hidden project on screen, no app lock, re-lock due | `RHLockMark`: plain `bg`, mark (72), `숨긴 프로젝트`, `잠김 · 지문으로 열기`, fingerprint button, `PIN 사용` · `서재로` |
| Hidden project, app lock also on | first the app-lock screen in mark-only form, then after it the hidden-project lock screen asks again (app lock and hidden lock stay separate) |
| R-18 not hidden, capture block on | mark only, no name (app-lock layout without the blurred content; title `Civilight`, `ED`) |
| Auth failed / cancelled | go to 서재; hidden group locked |
| Pre-captured screen for the blur | never taken for hidden or capture-blocked projects |

`서재로` → 서재 with the group locked. Example from R19 §4: leaving 「잿빛 새벽의 등불」 1-7 씬 3 and returning shows the blurred manuscript under the lock; leaving 「검은 서약」 shows only the mark and `숨긴 프로젝트 · 잠김`.

Sources: R19 §4 (rule, table, example, decision 6 recommendation confirmed); boards `AppLock`, `RHLockMark`.

#### LIB-35 · 앱 잠금
Priority **P1** · needsExternal **no**

- Setting 설정 › 보안 `앱 잠금` · `지문으로 열기` (switch); also offered in onboarding (`앱 잠금` · `열 때 지문`).
- Locks on app start and every return from background (R19 §4 example "쓰다 나갔다 오면 … 잠금 창이 뜬다"; exact timing §6 Q13).
- Unlock: the fingerprint button opens the system prompt; `기기 PIN 사용` opens device-credential auth. Biometric lockout → PIN (MAIN "생체 인증이 잠기면 PIN으로").
- Separate from the hidden-project lock: opening the app lock never opens hidden projects.
- Turning app lock off requires authentication (`ED`).

Sources: MAIN 저장 · 백업 (앱 잠금); R19 decision 6 (confirmed separate); boards `AppLock`, `OnbSafe`, `Settings`.

#### LIB-36 · 항목 숨기기 (공통 규칙)
Priority **P1** · needsExternal **no**

Hideable items inside a project: 인물 · 용어 · 페이지 · 씬 · 장소 · 세력 (장소 · 세력 added in v1, MAIN 범위). This area owns the flag, the per-project unlock and the masking contract; each module renders it.

- Hide/unhide from the item's ⋯ menu (module areas). Unlock: per project, `지문으로 보기` on any `숨긴 <kind> N` row → same prompt as LIB-32; re-lock per LIB-33.
- Hiding applies to the item's own records (card, profile, relationships, changes). App-drawn links show a mask while locked; text the user typed stays as typed. Warnings and results are never deleted; they collapse into one locked counted row.
- Masking contract (R19 §6) — implement as `itemSurface(surface, locked)`:

| Surface | Locked | Open |
|---|---|---|
| 인물 · 용어 · 페이지 list | one row `숨긴 인물 1` (kind word varies) · `지문 · PIN으로 열림` · `지문으로 보기` | in place + small `숨김` tag |
| 스토리 구조 · hidden scene | keeps position and number `1-7 씬 4 · 숨긴 씬`, title masked | title + `숨김` tag |
| Script · hidden scene | one line instead of blocks `숨긴 씬 · 블록 12 · 지문으로 보기` | as is |
| Speaker cell · @ link · term highlight | `숨긴 인물` · `숨긴 항목` · no term highlight | names |
| Speaker picker | not listed; last row `숨긴 인물 1 · 지문으로 보기` | listed |
| Search · command | card results excluded; visible scenes' text found; footer `숨긴 항목 제외` | all |
| Other characters' relations · 호칭 · 말투 | one row `숨긴 인물과의 관계 1` | as is |
| Whole relationship map | node and lines not drawn; `12명 중 5명 · 숨김 1` | drawn |
| Lens · ◆ records | grouped `숨긴 항목 변화 2` | as is |
| 점검 센터 | all checks computed; results `▲ 숨긴 항목 관련 2 · 지문으로 보기` | original rows |
| Character route · map | hidden-scene stretch dashed `숨긴 씬`; no route for a hidden character | as is |
| Recorder link | `숨긴 항목` | name |
| 휴지통 | `숨긴 인물 · 3일 남음` · restore needs auth | name |
| 최근 연 파일 | omitted | omitted |

- Edges: hidden character speaks in a visible scene → 점검 ◇ `숨긴 인물이 보이는 씬에 나옴 · 씬 3` with `씬도 숨기기` · `그대로` (the app never hides the scene itself); locked hidden character is not an @ candidate, only reachable via `숨긴 인물 1 · 지문으로 보기`; 장소 · 세력 follow the same table and their map pins/regions are not drawn while locked; re-lock timing = LIB-33.
- Board `ItemHide`: cast list with filter chips, note `인물 · 용어 · 페이지 · 씬 단위 · @연결은 ‘숨긴 항목’으로 표시`, lock row `숨긴 인물 1` · `지문 · PIN으로 열림` · `지문으로 보기`.

Sources: MAIN 숨기기; R19 §6 (rule, example, tables); MAIN 범위 (장소 · 세력 숨기기 v1); board `ItemHide`.

#### LIB-37 · 인증 실패와 잠김
Priority **P0** · needsExternal **no**

Common handling for every auth gate (hidden group, hidden items, unhide, export `넣기` of hidden scenes, trash restore of hidden things, app lock):

- Cancel → action aborted, state unchanged, no toast.
- Biometric lockout → the system prompt offers device PIN (`ErrBio`); if the user cancels, behave as cancel.
- No enrolled biometrics but a device credential exists → device credential prompt directly.
- No secure lock screen → §6 Q19.

Sources: boards `ErrBio`, `LibUnlock`; MAIN 앱 잠금.

### 2.6 휴지통과 되돌리기

#### LIB-38 · 휴지통
Priority **P0** · needsExternal **no**

App-wide screen (head small `30일 뒤 자동 삭제`, title `휴지통`, right action `비우기`). This area is the **host**: list, filters, countdown, permanent delete and the auth gate. What "되살리기" restores is defined by the owning area.

- Filter chips (single choice): `전체 <n>` · `프로젝트` · `씬` · `블록` · `인물` · `세계` · `변화` · `레코더` (세계 = 장소 · 용어 · 세력; R20 §5 replaces R12 §7's list).
- Row: title (Nanum Myeongjo 700 15px), right `<d>일 남음`; sub line; two tags `되살리기` · `영구 삭제`.
- Entry kinds and their row copy:

| Kind (filter) | Title | Sub | Source |
|---|---|---|---|
| project (프로젝트) | `<name>` | `프로젝트 · 씬 <s>개 · 인물 <c>명` | `Trash` |
| project replaced by `덮어쓰기` (프로젝트) | `<name>` | copy: not in sources | FMT §6 |
| scene (씬) | `<ep> 씬 <n> · <title>` (`1-6 씬 5 · 시장의 밤`) | `씬 · <project>` | `Trash` |
| block group per scene per day (블록) | `<ep> 씬 <n> · 블록 <k>개` | `블록 · <project> · 오늘 지움` (day word by time format) | R12 §7 |
| character (인물) | `<name>` | `인물 · <project> · 대사 <x> · 관계 <y>` (reference counts, computed) | R20 §5 |
| place / term / faction (세계) | `<name>` | copy: not in sources (pattern of 인물 row, `ED`) | R20 §5 |
| merged item (kind's filter) | `<name>` | contains `합침 → <target>` (R20 §6) | R20 §6 |
| change record removed with `기록 지우기` (변화) | copy: not in sources | — | R12 §7 |
| deleted field definition (인물) | copy: not in sources | — | CF §7 |
| recorder record (레코더) | `음성 0:12` (kind + length) | `라이프 레코더` | `Trash`, R23 §6 |
| hidden item (its kind) | `숨긴 인물` (kind word varies) | `인물 · <project> · 되살리기는 지문 확인` | `Trash`, R19 §6 |
| item of a hidden project | `숨긴 프로젝트` as location | restore needs auth | R19 §3 |

- Block groups expand (tap row) to list each block's first line with its own `되살리기` (R12 §7). Empty blocks never enter the trash.
- Sorting: by deletion time descending (`ED`).
- Counting `<d>일 남음`: `d = 30 − (calendar days between deletion date and today)`, local time zone (`ED` for the exact boundary). Deleted today → `30일 남음`.
- `되살리기`: hidden things → authentication first. Then calls the owner's restore. Block restore positions: original place (by key between neighbours); if both neighbours are gone, same order position in the scene; if the scene is in the trash, choose `씬도 되살리기` · `다른 씬 끝에` (R12 §7). Restored blocks keep number and voice-ID state. Records waiting as 근거 지워짐 re-attach.
- Locks: while hidden things are locked their rows show only the masked form; `되살리기` triggers auth.
- Reference counts on rows are computed at display time (never stored, FMT §4).

Sources: R12 §7; R20 §2/§5/§6; R19 §3, §6; R23 §6; CF §7; FMT §6; board `Trash` (`scr_states.py trash`).

#### LIB-39 · 영구 삭제 · 비우기 · 30일 자동 삭제 · 이름표
Priority **P0** · needsExternal **no**

- `영구 삭제` on a row → confirm (copy: not in sources) → removes the entry and its content permanently.
- `비우기` (header action) → confirm (copy: not in sources) → permanently deletes every entry regardless of the selected filter (`ED`). Entries of hidden projects/items are deleted only if they are currently unlocked; locked ones stay (`ED`).
- Automatic purge when `d ≤ 0` (`ED`: daily WorkManager job + check on app start).
- After permanent delete of a 인물 · 장소 · 용어 · 세력, a **name tag** remains: name, kind, voice code (speaker part, e.g. `HRT`), and the hidden flag. References elsewhere show `영구 삭제 · <name>` with ◇ (hidden: `영구 삭제 · 숨긴 항목`); dialogue never becomes 화자 없음; fixed voice IDs are unchanged (R20 §5). Storage: §3 `NameTag`, file §3 (open question §6 Q4 for the file layout).
- The deleted key and time are kept 30 days in a deletion log (FMT §7) for future sync.

Sources: R20 §5; FMT §7; board `Trash`.

#### LIB-40 · 되돌리기 알림 (Toast host)
Priority **P0** · needsExternal **no**

- `Toast` component: `ink` fill, `bg` text, one action. Position: above the bottom glass bar (156 px from the bottom on the script screen). One toast at a time; a new one replaces the old (`ED`); duration `ED` 6 s; tapping the action performs the undo and dismisses.
- Messages come from owning areas; drawn/sourced examples: `블록 #043 지움 · ◆ 1개는 #044 자리로` + `되돌리기` (board `UndoToast`); `블록 5개 지움 · ◆ 2개 다음 자리로`; `씬 3 지움 · ◆ 3 · ◇ 1은 씬 4 처음으로`; `인물 노아 지움 · 참조 20 · 점검 센터 · 되돌리기`; `인물 노아 지움 · 되돌리기`; `시장 꼬마 → 노아 합침 · 참조 5 · 되돌리기`; `노아 되살림 · 다시 이어짐 · 대사 11 · @ 3 · 씬 등장 2 · 그대로 둔 것 · 대사 3`.
- After the toast is gone, deleted content is still in 휴지통 (R12 §7: "되돌리기 알림, 휴지통 30일").

Sources: R12 §2, §7; R20 §4–§6; DS `Toast`; board `UndoToast`.

### 2.7 내보내기

#### LIB-41 · 내보내기 시트
Priority **P0** · needsExternal **no**

Bottom sheet over the project (board `Export`, `scr_states.py export`), opened from 서재 ⋯ › `백업 · 내보내기` or the project's menus.

- Header `내보내기` + `닫기`; sub `<project name> · <rating badge>`.
- Section `형식` (radio cards, one selected; selected = `sel` fill + `acc` outline):
  - `대본 PDF` · `읽기용 · 표지에 등급`
  - `대사 표 CSV` · `화자 · 대사 · 보이스 ID · 블록 ID · 상태`
  - `게임 엔진 JSON` · `씬 · 블록 · 화자 · 연출 · 선택지`
  - `프로젝트 파일 .civilight` · `옮기기 · 보관 · 숨김 · R-18 · 숨긴 항목 있으면 암호`
- Option rows (label left, value right), shown per format:

| Row | Formats | Value / control |
|---|---|---|
| `범위` | PDF · CSV · JSON | chapter / episode picker; drawn `1장 전체`. Units = the project's levels above 씬 (MAIN "범위는 장 · 에피소드 단위로") |
| `숨긴 씬 <n> · <ep 씬 list>` | PDF · CSV · JSON, only if the range contains hidden scenes | `넣기` · `빼기` segmented; default PDF `빼기`, CSV `넣기`, JSON `넣기`; `넣기` requires one authentication when pressing the create button (R19 §5) |
| `숨긴 인물 <n> · <names>` | PDF · CSV · JSON, if hidden characters speak in range | info `화자 이름만 들어감` (no control) |
| `표시 레이어` | PDF | drawn `연출 · 괄호 지문 포함` → layer picker (연출 지시 · 괄호 지문 · 작가 메모 …, same list as the script's 표시 레이어) |
| `비밀 항목 포함` | JSON (and PDF if it ever prints cards) | switch, default off (CF §8); copy only the label is sourced |
| `파일 이름` | all | text; default = project name; hidden or R-18 project → `civilight-내보내기-<MMDD>` (e.g. `civilight-내보내기-1122`); editable (§6 Q1) |
| `PDF 암호` | PDF, when project is hidden or R-18 | switch default on + password field (copy beyond the label: not in sources) |
| `암호` | .civilight | switch default on when the project is hidden, R-18, or has any hidden item; else off; password + repeat fields (reuse `pw_field` of `RHRecoveryPw`); one warning line that a forgotten password cannot be recovered by the app (FMT §5; exact copy: not in sources — `RHRecoveryPw`'s `잊으면 … 열 수 없음` pattern) |
| `레코더 기록 <n>` | .civilight | `원본 음성 포함` checkbox (default off) · `숨긴 기록 <h> 제외` (drawn: `레코더 기록 4` · `원본 음성 포함 · 숨긴 기록 1 제외`) |
| `휴지통도 포함` | .civilight | switch default off (FMT §6; label sourced, placement `ED`) |

- Primary button: `PDF 만들기` (drawn, for PDF). Labels for CSV, JSON and `.civilight`: copy not in sources (`TODO_COPY_LIB41_CREATE_CSV`, `_JSON`, `_FILE`; the obvious candidates follow the same "<형식> 만들기" pattern but are not confirmed).
- On create: auth if needed → build file → `ED` save via Storage Access Framework `ACTION_CREATE_DOCUMENT` with the file name and MIME type (PDF `application/pdf`, CSV `text/csv`, JSON `application/json`, `.civilight` `application/vnd.civilight.project`); then a done notification (LIB-30) or toast (copy: not in sources).
- Example project and trashed projects cannot be exported.
- Capture block applies while this sheet is visible for a capture-blocked project.

Sources: MAIN 내보내기; R19 §5; R23 §6 (내보내기); CF §8; FMT §5–§6; board `Export`.

#### LIB-42 · 숨긴 것과 내보내기
Priority **P1** · needsExternal **no**

| Format | Hidden scenes default | Hidden characters · terms · pages | Other |
|---|---|---|---|
| 대본 PDF | `빼기` | PDF has no cards; speaker names follow the scene | hidden or R-18 project → `PDF 암호` row default on |
| 대사 표 CSV | `넣기` | speaker names included | rows left out by `빼기` are **not** `버림` (R12 status) |
| 게임 엔진 JSON | `넣기` | used speakers exported with id · 이름 · 표시 이름 only, no profile | with `빼기`, a choice leading into a left-out scene raises ▲ `숨긴 씬으로 가는 선택지 <n>` (shown in the sheet before creating, `ED` placement) |
| 프로젝트 파일 .civilight | never asked · everything · hidden flags kept | everything | encryption default on if hidden · R-18 · has hidden items |

- Hidden Life Recorder records are left out of `.civilight` by default; they go in only when the file is encrypted because the project is hidden (R23 §6, new default confirmed).
- No fake encryption for formats that cannot carry it (CSV, JSON): nothing is added (R19 §5 rule).

Sources: R19 §5; R23 §6.

#### LIB-43 · 대본 PDF
Priority **P1** · needsExternal **no**

- Reading copy of the manuscript for the chosen range. Cover page with project name and rating (when `내보내기 표지에 등급 표시` is on — default on for all ratings).
- Scene heads appear as title lines (R22 §6). Blocks rendered in manuscript order; speaker name above line (Nanum Myeongjo body, Noto Sans KR labels, embedded fonts).
- Display layers: the `표시 레이어` choice decides whether 연출 지시, 괄호 지문 and 작가 메모 appear (memos inserted from the recorder follow the layer too, R23 §6).
- Hidden scenes: per LIB-42. Optional password (`PDF 암호`) using standard PDF encryption (`ED`: AES-256 via the PDF library).
- Page size and typography: `ED` (A4, body 11 pt).

Sources: MAIN 내보내기 (대본 PDF); R22 §6; R23 §6; R19 §5.

#### LIB-44 · 대사 표 CSV
Priority **P1** · needsExternal **no**

- Columns, in order: `화자` · `대사` · `보이스 ID` · `블록 ID` · `상태` (MAIN; R12 §6). `블록 ID` = the block's permanent key (ULID), not `#043`.
- Rows: every dialogue block in range in manuscript order (including dialogue lines inside 선택지 options, whose voice IDs carry a sub-number e.g. `HRT_0107_045_1`); one row per scene that has a 전환 문구 (화자 = `전환 문구`, 블록 ID = the phrase ID derived from the scene key, 보이스 ID empty, same 상태 rules; R22 §6); one row per discarded voice ID (`버림`). Whether narration / system text rows belong in the table is §6 Q22.
- `상태`: `새` (first export of this voice ID), `바뀜` (`ED`: the line text or speaker differs from what was last exported for this ID), `버림` (a fixed ID discarded since; still exported so the recording team knows). Rows left out by hidden-scene `빼기` are not `버림`.
- Hidden speakers: real name in `화자` (R19 §5). Deleted (trashed or permanently deleted) speakers: name kept (R20 §2 table).
- Exporting fixes voice IDs (LIB-46).
- `ED`: UTF-8 with BOM, CRLF, RFC 4180 quoting.

Sources: MAIN 내보내기; R12 §6; R22 §6; R19 §5; R20 §2.

#### LIB-45 · 게임 엔진 JSON
Priority **P0** · needsExternal **no**

The engine contract is separate from `.civilight` and versioned on its own; it changes rarely ("거의 안 바뀜. 바꾸면 버전을 올림", FMT §1). Final shape awaits the target engine (MAIN 남은 확인); implement this proposed skeleton:

```
{
  "format": "civilight-engine",          // ED name
  "version": 1,
  "assets": [ ... ],                     // backgrounds, BGM, SFX, CG used in range
  "characters": [ { "id", "name", "displayName", "fields": { "<exportName>": value | [ {from, value} ] } } ],
  "scenes": [ {
      "id", "order", "title",
      "place": { "id", "name", "kind": "pin|region" } | null,
      "time": { "fromStart", "day", "band", "clock", "precision", "earliest", "latest" },
      "flashback": false,
      "transition": { "text", "mode": "auto|manual", "id" } | null,
      "blocks": [ ... ]
  } ]
}
```

- Blocks: every block has its permanent `id`. Dialogue: `speaker` · `displayName` · `stage` (무대 위치, if enabled) · `text` · `voice`. Directions: one type with `op` (`background` · `cg` · `bgm.play` · `sfx` · `shake` …). Choices: `options[]` + `rejoin` (rejoin is always the next block). Author memos: `export:false` (excluded from the engine output). System text blocks included. The scene head is **not** a block (R22 §6).
- `scenes[]` carry resolved time (the engine cannot resolve base + interval): time since story start, episode day, band, clock, precision; for 대략 the earliest and latest values; flashback flag; place key + name; transition text, auto/manual, phrase ID; empty when 없음 (R22 §6 table).
- `characters[]`: per used character `fields` keyed by each field's export name (default = field name); fields with ◆ history export as a per-point list. Secret fields only with `비밀 항목 포함` (CF §8). Hidden characters: id · 이름 · 표시 이름 only.
- Hidden scenes per LIB-42 (`넣기` default, auth). 괄호 지문 handling: §6 Q21.
- Exporting fixes voice IDs (LIB-46).

Sources: MAIN 내보내기 (JSON 골격); R22 §6; CF §8; R19 §5; FMT §1.

#### LIB-46 · 보이스 ID 고정 (내보낼 때)
Priority **P1** · needsExternal **no**

- A dialogue block's voice ID is `자동` until it is first written into a CSV or JSON export; at that moment it becomes `고정` with the export date, and the app never changes it afterwards (R12 §6). The property panel (Script area) shows `HRT_0107_043 · 자동` / `HRT_0107_043 · 고정 · 11월 20일 내보냄`.
- Only blocks actually written into the file are fixed (range and hidden-scene `빼기` respected). Whether hidden-but-excluded scenes get fixed: §6 Q20 (`ED`: not fixed, since they were not written).
- PDF and `.civilight` exports never fix voice IDs.
- Fixed IDs and discarded IDs are stored in `.civilight` always (FMT §6).

Sources: R12 §6; FMT §6; R19 §7.

#### LIB-47 · 프로젝트 파일 내보내기 (.civilight)
Priority **P0** · needsExternal **no**

- Writes one project into one `.civilight` package (LIB-49) in the **latest** format version. Contents per FMT §6: manuscript, cast, world, maps, canvases, pages, change records — always; project settings and field definitions — always (app settings such as theme/layout never); original images and map bases — always; voice-ID states and discarded IDs — always; Life Recorder records linked to this project — text always, original audio optional (default off), hidden records only as in LIB-42; trash — not included unless `휴지통도 포함`; check `맞음` marks and term `아님`/`맞음` — always; derived values, search index, preview images — never, except one cover `preview.webp` (omitted for hidden projects).
- Encryption per LIB-51 when the `암호` switch is on.
- File name per LIB-41 (`.civilight` extension appended).

Sources: FMT §2, §5, §6; R21 (저장); R19 §5; R23 §6.

#### LIB-48 · 레코더 라이브러리 파일 (.civilight-recorder)
Priority **P1** · needsExternal **no**

- The Life Recorder library (outside projects) exports and backs up as `.civilight-recorder`, same package structure and version rules as `.civilight` (mimetype `application/vnd.civilight.recorder`, `ED` name). Entry point for manual export: Life Recorder area (not drawn).
- On restore/open, records with the same permanent key merge into one and only gain links (R23 §6).

Sources: FMT §6; R23 §6.

#### LIB-72 · 여러 프로젝트 한꺼번에 내보내기
Priority **P2** · needsExternal **no**

- FMT §6: exporting several projects at once produces several files, one `.civilight` per project (never a combined file). Each file follows LIB-47 (own encryption default, own file name).
- No entry point or selection UI is drawn (§6 Q23). `ED`: a multi-select mode on 서재 offering only `프로젝트 파일 .civilight`; hidden projects selectable only while the group is open; one password prompt per encrypted file. Copy not in sources.

Sources: FMT §6 (내보내기 시트의 변화).

### 2.8 프로젝트 파일 .civilight

#### LIB-49 · 묶음 구조와 적는 법
Priority **P0** · needsExternal **no**

The app DB is the working store; `.civilight` is the move/backup unit and is never edited in place (FMT §1). Conversion to/from the format happens only at export/import, so the DB schema may change freely.

- **Container**: ZIP. First entry `mimetype`, **stored (no compression)**, content `application/vnd.civilight.project` (EPUB/ODF style).
- **Layout** (FMT §2):

```
<name>.civilight
├ mimetype                application/vnd.civilight.project
├ manifest.json           format version · features · file list with hashes
├ project.json            name · rating · structure · project settings · field definitions
├ story/
│  ├ index.json           chapters · episodes · scenes order, each scene's place · time
│  └ <episode key>.json   blocks (one file per episode)
├ cast.json               characters, field values, relationships
├ world.json              places · terms · factions · timeline events
├ changes.json            ◆ change records (position · anchor block key)
├ maps/<map key>.json     base info + pins · regions · routes · labels
├ canvas/<canvas key>.json
├ pages/<page key>.json
├ assets/index.json       crop data · holo · usage
├ assets/<hash>.<ext>     original images · map bases · (optional) audio
├ recorder.json           recorder records linked to this project
└ preview.webp            library card cover (absent for hidden projects)
```

- Files this area adds for content the tree does not place (`ED`, §6 Q4): `checks.json` (점검 `맞음`, term `아님`/`맞음`), `voice.json` (voice-ID states and discarded IDs if not kept on blocks), `tombstones.json` (name tags, merge records), `deletions.json` (deleted keys + time, 30 days), `trash/` (only with `휴지통도 포함`).
- **Writing rules** (FMT §4): every item has a device-generated permanent ULID key; references are always by key (never name, `#043` or position); ordering by an order-key string per item (insertable, `ED`: fractional index), never by array position; text as a list of runs (plain, paren stage-direction, link-by-key …) so unknown run kinds degrade to plain text; time stored as rule 11 (base scene key or 직접 + interval + band + precision; world calendar definition included); derived values never stored (age, N일째, first appearance, usage lists, check results) except check `맞음` marks; generated maps store seed + step choices + generator version **and** the result (base image, coastline, hex cells, region data); text→map stores the condition list only (model name/version for the record); images stored once by content hash with crops as numbers (area, rotation, flip); built-in images by name only (`preset:ash-dawn`; preset names are never removed from the app); every item has an optional `ext` object.
- Per item: last-modified time and device (future sync).
- All text UTF-8, Korean normalized to **NFC** on write and on read.
- Engine JSON is separate (LIB-45).

Sources: FMT §1, §2, §4, §7.

#### LIB-50 · 버전 규칙
Priority **P0** · needsExternal **no**

Five promises (FMT §3):

1. Old files always open. The app reads every earlier format; upgrades chain 1→2→3 one step at a time; a written upgrade step is never deleted; the source file is never modified; exports always write the latest format.
2. `formatVersion` is `major.minor` (e.g. `1.4`). Adding items/files → minor; changing the meaning or shape of existing ones → major (avoid; add under a new name).
3. Unknown items/files are skipped, not errors. Unknown content must survive: the app keeps unknown `ext` objects and unknown files it read and writes them back unchanged when re-exporting the same project (`ED`; FMT says "모르는 것을 버리지 않는다").
4. A newer file is not imported if it needs something this app does not know (LIB-53). Decision uses `minReaderVersion` and `features.required`, not just the version number.
5. Growth is named by features: `features.required` (unknown → cannot open) and `features.optional` (unknown → that part is skipped).

Manifest (FMT §3 example):

```json
{
  "format": "civilight-project",
  "formatVersion": "1.0",
  "minReaderVersion": "1.0",
  "features": {
    "required": ["story.blocks", "changes", "time.base-interval"],
    "optional": ["maps.gen", "fields.custom"]
  },
  "writtenBy": "Civilight 1.0.0 (Android)",
  "writtenAt": "2026-11-21T08:12:00Z",
  "project": { "key": "01JD…", "title": "잿빛 새벽의 등불", "rating": "R-15" },
  "encrypted": false,
  "files": { "story/01JD….json": "sha256-…" }
}
```

Sources: FMT §3.

#### LIB-51 · 암호화
Priority **P0** · needsExternal **no**

- Whole-file encryption with a passphrase (never a biometric: biometrics exist only on one device). Used by: `.civilight` export with `암호` on (default on for hidden · R-18 · has hidden items); Drive auto backups of hidden projects with the 복구 암호 (LIB-57).
- Outside an encrypted file only `mimetype` and the encryption info are visible; name, cover and rating are inside (FMT §5). The KDF and cipher names are written so stronger methods can be added later and old files still open with the recorded method.
- After opening, the imported project is re-locked with this device's biometrics (hidden on this device).
- `ED` container: outer ZIP = `mimetype` (stored) + `encryption.json` + `payload.bin`; `encryption.json` = `{ "format":"civilight-encrypted", "lockType":"export|recovery", "kdf":"PBKDF2-HMAC-SHA256", "iterations":600000, "salt":b64, "cipher":"AES-256-GCM", "nonce":b64 }`; `payload.bin` = the complete inner `.civilight` ZIP encrypted. `lockType` drives the password field label on open (LIB-54). See §6 Q2 (manifest wording). Algorithms are an engineering choice the sources leave open (they only require that the names are recorded); Argon2id is an acceptable alternative.

Sources: FMT §5; R19 §5; memory 2026-11-22 (label follows lock type).

#### LIB-52 · 파일에서 열기
Priority **P0** · needsExternal **no**

- Tapping a `.civilight` file in a file manager or mail opens Civilight (`ED`: intent filters for `ACTION_VIEW` with the MIME type and `*.civilight` path pattern). Also reachable for Drive auto-backup files (they open with the 복구 암호).
- Flow (cover: bottom sheet over the source app, boards `RHOpenFile`…): small `Civilight · 파일에서 열기` + `닫기`; title = project name; summary line: rating badge + `씬 <s> · 인물 <c> · <size> · <date>` (e.g. `씬 31 · 인물 11 · 1.2 MB · 11월 22일`).
- If a project with the **same project key** already exists: ◇ `같은 프로젝트가 이미 있음` · `서재 · 씬 <s> · <time> 수정`; radio `새 프로젝트로` · `‘<name> (2)’로 추가` (default) / `덮어쓰기` · `지금 것은 휴지통 · 30일`. Buttons `취소` · `열기`.
  - 새 프로젝트로 → import under a **new project key** and the name suffixed ` (2)` (next free number). Item keys are unique per project (R12 §5 "프로젝트 전체에서 하나"), so items keep their keys inside the new project (`ED`: store item rows keyed by project key + item key; regenerate item keys only if the DB design requires globally unique keys). **Recorder records are the exception either way**: they live outside projects and merge by their permanent key, so a record already in the library is not duplicated; it only gains a link to the new project (R23 §6).
  - 덮어쓰기 → the existing project goes to 휴지통 (LIB-38), the file's project is imported with its keys.
- No same key → `열기` imports directly (buttons `취소` · `열기`; the same-project block is absent).
- App lock / hidden lock apply first. Example/read-only state is never imported from files.
- Hidden flags inside the file are kept; hidden projects land in the hidden group.

Sources: FMT §6 (파일에서 열기); memory 2026-11-22 (same project = project ID); board `RHOpenFile`.

#### LIB-53 · 새 버전 파일 안내
Priority **P0** · needsExternal **no**

When `minReaderVersion` > the app's max supported version **or** `features.required` contains a feature the app does not know:

- Sheet (`RHNewerFile`): title + summary as LIB-52; heading `새 버전 앱에서 만든 파일`; rows `이 파일` · `양식 <formatVersion>`, `지금 앱` · `<max>까지`, `필요한 기능` · `<unknown required features, human names>` (drawn: `양식 1.4`, `1.2까지`, `지도 생성 2판`); note `가져오지 않음 · 파일은 그대로 · 업데이트 뒤 다시 열기`; buttons `닫기` · `Civilight 업데이트` (opens the store listing / release page, `ED`).
- Nothing is imported, not even read-only. Unknown **optional** features never block: those parts are skipped silently on import (`ED`: list them in a ◇ line after import; copy: not in sources).
- Feature id → Korean name table is part of the app (`ED`), e.g. `maps.gen.v2` → `지도 생성 2판`.

Sources: FMT §3 promise 4; board `RHNewerFile`.

#### LIB-54 · 암호 파일 열기
Priority **P0** · needsExternal **no**

- Sheet (`RHEncryptedFile`): lock tile + `암호 파일` + file name (mono, ellipsized); note `이름 · 등급 · 내용 · 열면 보임`; password field labelled `암호` with hint `내보낼 때 정한 암호` for export files, or `복구 암호` for auto-backup files (label follows `lockType`); note `열면 ‘숨긴 프로젝트’ 묶음으로 · 지문으로 다시 잠금`; buttons `취소` · `열기`.
- Wrong password: copy not in sources (`ED`: field error state, retry allowed, no lockout).
- After decrypting, continue with LIB-52 (same-key check) and LIB-53 (newer check) on the inner package; the imported project is hidden on this device (biometric re-lock).

Sources: FMT §5; board `RHEncryptedFile`; memory 2026-11-22.

#### LIB-55 · 깨진 파일 부분 가져오기
Priority **P1** · needsExternal **no**

- On open, every file's hash is checked against `manifest.files`. If one episode file is damaged, everything else is imported and the user sees `열지 못한 부분 · <episode>` (e.g. `열지 못한 부분 · 1-7`). Episodes are separate files for exactly this reason.
- If `manifest.json`, `project.json` or `story/index.json` is damaged → nothing imported (`ED`; copy: not in sources).

Sources: FMT §7 (깨진 파일).

#### LIB-56 · 양식 회귀 예시 파일
Priority **P1** · needsExternal **no**

For every released format version, keep a `잿빛 새벽의 등불` example file in the repo (`ED` path `app/src/test/resources/format/<version>/잿빛 새벽의 등불.civilight`); a JVM test opens every one with the current app and asserts the same content (FMT §3 지키는 방법). The format description lives in the same repository (`ED` `docs/format/`). The onboarding example (LIB-17) is the current-version file.

Sources: FMT §3.

### 2.9 백업과 되살리기

#### LIB-57 · Google Drive 자동 백업
Priority **P1** · needsExternal **yes**

- Toggle 설정 › 보관 `Google Drive 자동 백업` (and onboarding `연결`). Schedule: once a day, Wi-Fi only (`하루 한 번 · 와이파이 · 원본은 기기에`). Originals stay on the device.
- Units: one `.civilight` per project, one `.civilight-recorder`, one app-settings file. Only projects changed since their last uploaded version are uploaded. 30 days of versions kept.
- Hidden projects: encrypted with the 복구 암호 (`lockType: recovery`); if no 복구 암호 is set, or 설정 · 숨기기 `숨긴 것도 백업` is off, hidden projects are **excluded**, and 설정 and the backup screen show `숨긴 프로젝트 <n> · 백업 안 됨`.
- Normal projects with hidden items: if a 복구 암호 exists, the hidden items' content is locked with it (`ED`: the whole project file is encrypted with the 복구 암호; §6 Q5); if not, the project is backed up unencrypted and 설정 › 보관 shows ◇ `숨긴 항목 <n> · 암호 없이 백업 · 복구 암호 정하기 ›`. Never excluded (a restored project would have holes).
- Example project and trashed projects are not uploaded.
- `ED` implementation: WorkManager periodic work (24 h, `NetworkType.UNMETERED`, battery not low); Drive REST v3 in the app-data folder (`drive.appdata` scope); a per-run index file listing project key → Drive file/version, size, encrypted flag, hidden flag; versions not referenced by any run in the last 30 days are deleted. Drive folder and scope are not specified by the sources (MAIN only notes a Drive permission review before release).
- Notification on completion per LIB-30; failures per LIB-61.

Sources: MAIN 저장 · 백업; FMT §5–§6; R19 §5; boards `OnbSafe`, `Settings`, `ErrBackup`.

#### LIB-58 · 복구 암호
Priority **P1** · needsExternal **no**

- Screen (`RHRecoveryPw`, head `설정 › 보관` / `복구 암호 정하기`): card lock tile + `숨긴 프로젝트 <n>` · `이 암호로 잠금 · Google Drive 자동 백업`; field `복구 암호` with a 4-bar strength meter + `강도 · <좋음…> · <len>자` (drawn `강도 · 좋음 · 14자`); field `한 번 더`; each field has a show/hide eye button; ◇ `잊으면 숨긴 프로젝트 백업을 열 수 없음` · `앱 · 서버에 저장 안 함 · 찾아 줄 수 없음`; section `함께 잠금`: rows `숨긴 항목 <n>` · `<project names that have hidden items>`, `되살릴 때` · `이 암호 · 그 뒤 기기 지문`; bottom primary `정하기` (enabled when both fields match and strength ≥ minimum, `ED` minimum 8 chars).
- Set once; 설정 shows `복구 암호` · `숨긴 프로젝트 백업용 · 정해 둠` · `바꾸기 ›` (unset: `정하기 ›`, sub copy: not in sources). Changing re-locks future backups (old versions keep the old password, §6 Q24).
- The password itself is never stored. `ED`: derive a key-encryption key (PBKDF2, salt) and wrap a random backup data key with it; keep the data key also wrapped by an Android Keystore key so daily background backups need no typing; the password-wrapped data key travels inside each encrypted backup header.
- Strength levels and their copy beyond `좋음`: not in sources.

Sources: FMT §5 (자동 백업의 암호); board `RHRecoveryPw`; memory (복구 암호 confirmed).

#### LIB-59 · 백업에서 되살리기
Priority **P1** · needsExternal **yes**

Screen (`Restore`, head `설정 › 보관` / `백업에서 되살리기`), also from onboarding `백업에서 되살리기` and the empty 서재:

- `Google Drive · 계정 연결됨` (cloud icon). Not connected → sign-in first (copy: not in sources).
- Section `백업 · 30일`: one row per backup run, newest first: `<M월 D일 HH:mm>` · `프로젝트 <n> · 숨김 <h> 포함 · <size>` (or `프로젝트 <n> · <size>` without hidden); selected row `sel` + left accent bar.
- Per-project choice inside a run is required by FMT §6 but not drawn (§6 Q25; `ED`: after choosing a run, a checklist of its projects, hidden ones as `숨긴 프로젝트 <h>` until the 복구 암호 is entered).
- Section `같은 이름이 이미 있을 때` (match is by **project key**, §6 Q26): segmented `새 프로젝트로` (default) · `덮어쓰기`; same semantics as LIB-52 (overwritten project → 휴지통).
- Note `숨긴 프로젝트 · 복구 암호로 열고 이 기기 지문으로 다시 잠금`. Hidden projects ask for the 복구 암호 once (LIB-54 sheet with label `복구 암호`).
- Bottom primary `<M월 D일> 백업 되살리기` → LIB-60.
- Recorder records restored by key merge (R23 §6).

Sources: MAIN 저장 · 백업 (백업에서 되살리기); FMT §5–§6; board `Restore`.

#### LIB-60 · 되살리는 중
Priority **P1** · needsExternal **yes**

Screen (`Restoring`, board title `불러오는 중 · 복원`; head `되살리기`): section `Google Drive · <date> 백업`; title `되살리는 중`; progress bar (bytes) with `<done> / <total> MB` and `약 <n>초`; steps (square markers: done = filled `acc`, current = `acc` outline, todo = `ink3` outline): `원고 · 인물 · 세계` · `<n>개 프로젝트`; `에셋 · 음성 기록` · `이미지 <i> · 음성 <a>`; `숨긴 프로젝트 <h>` · `지문 확인 후 풀림`; note `앱을 닫아도 계속 · 끝나면 알림`. Runs as a foreground service / expedited work (`ED`) so it survives leaving the app; completion notification per LIB-30.

Sources: board `Restoring`; MAIN 상태 화면 (복원 진행).

#### LIB-61 · 백업 실패 · Drive 로그인 만료
Priority **P1** · needsExternal **yes**

- 설정 › 보관 shows a banner (tint, `r18` outline, warning icon): `백업 실패 · <n>일째` · `<reason> · 원고는 기기에 안전` (drawn reason `Google 로그인 만료`); buttons `다시 로그인` (primary; re-auth) · `지금 다시 시도` (runs the backup now). The Drive row then reads `마지막 성공 <date time>`.
- `<n>일째` is 1-based like rule 11's N일째: n = (calendar days from the first failed run of the current streak to today) + 1 (`ED`). Other failure reasons (no space on Drive, network): copy not in sources.
- Errors never block writing; data stays local.

Sources: MAIN 상태 화면 (백업 실패 · Drive 로그인 만료); board `ErrBackup`.

#### LIB-62 · 설정 › 보관
Priority **P1** · needsExternal **yes**

Screen head `설정` / `보관` (board `ErrBackup` shows it with the failure banner):

- `Google Drive 자동 백업` · `마지막 백업 <time>` (or `마지막 성공 <date time>` after failures) + switch.
- `기기 저장 공간` · `남은 공간 <x> GB · 프로젝트 <y> MB` + chevron (detail screen not drawn, `ED` P2).
- `백업 기록` · `30일 보관` + chevron → LIB-59.
- `복구 암호` row (LIB-58).
- Warnings when applicable: `숨긴 프로젝트 <n> · 백업 안 됨`; ◇ `숨긴 항목 <n> · 암호 없이 백업 · 복구 암호 정하기 ›`.

Sources: board `ErrBackup`, `Settings`; FMT §5; R19 §5.

### 2.10 설정 화면

#### LIB-63 · 설정 (앱)
Priority **P0** · needsExternal **no**

Root app-settings screen (head `Civilight` / `설정`, board `Settings`). App settings are separate from project settings.

- Section `화면`: segmented `기기 설정` · `라이트` · `다크` (theme, applied live); `글자 크기` · `본문 18 · 행간 1.7` · `보통 ›` (LIB-65); `펼친 화면 배치` · `서재 · 인물 · 세계 · 연표 …` · `목록 + 상세 ›` (LIB-66); `움직임 · 투명도` · `애니메이션 줄이기 · 투명도 줄이기` · `기기 설정 ›` (LIB-67); `지도 · 이미지 · 연표` · `색면 · 홀로 · 생성 방식 · 이동 거리` · `›` (LIB-68).
- Section `보관`: `Google Drive 자동 백업` · `마지막 백업 오늘 08:12` + switch; `복구 암호` · `숨긴 프로젝트 백업용 · 정해 둠` · `바꾸기 ›`.
- Section `보안`: `앱 잠금` · `지문으로 열기` + switch (LIB-35); `숨기기` · `숨긴 프로젝트 <n> · 다시 잠그는 때` · `<policy> ›` (LIB-64).
- Entry rows that other boards imply but this board does not draw (휴지통, 도움말 · 안내 다시 보기, 라이프 레코더 settings, 설정 › 보관 detail): §6 Q27. `ED`: add rows `휴지통`, `라이프 레코더`, `도움말 · 안내 다시 보기` using those screens' titles, and make the 보관 section rows open 설정 › 보관.

Sources: board `Settings` (`scr_tools.py settings`); concept memory ("app settings vs project settings now split").

#### LIB-64 · 설정 · 숨기기
Priority **P0** · needsExternal **no**

Screen (head `설정` / `숨기기`, board `HideSettings`):

- Intro `프로젝트 단위 기본 · 앱 잠금과 별도`.
- Section `다시 잠그는 때`: radios `앱을 떠날 때` · `다른 앱 · 화면 꺼짐` (default); `화면이 꺼질 때만` · `다른 앱 다녀와도 유지`; `연 지 5분 뒤`.
- Section `가리는 곳`: `검색 · 명령 결과` · `열려 있을 때만 결과에 포함` (switch, on); `위젯 · 최근 항목 · 알림` · `위젯 · 최근 항목 · 검색에서 빠짐 · 알림은 ‘숨긴 프로젝트’` (switch, on). Meaning when off: §6 Q28 (`ED`: off can never reveal a hidden name; for `검색 · 명령 결과` off = hidden content stays out of results even while open; for the second switch off = no change, keep on and disabled until decided).
- Section `인증`: `지문` · `실패하면 기기 PIN` · status `사용 중`; `숨긴 것도 백업` · `Google Drive · 복구 암호로 잠금` (switch, on); `수위 등급별 동작` · `R-18 · 숨기기 · 캡처 가림 · 이름 가림` · `›` (→ LIB-23).

Sources: board `HideSettings` (`scr_lib.py hidesettings`); R19 §7 row 설정 · 숨기기.

#### LIB-65 · 화면 · 글자 크기
Priority **P0** · needsExternal **no**

- Theme: `기기 설정` (default) · `라이트` · `다크`.
- 글자 크기: default `보통` = body 18 sp, line height 1.7. Other steps and their names: not in sources (`ED`: 5 steps 16/17/18/20/22 sp, labels from the size slider only). Onboarding uses a slider between a small and a large `가` with a live preview line.
- Both stored in AppSettings; applied app-wide via the design-system typography.

Sources: boards `Settings`, `OnbLook`; MAIN 남은 확인 (18 · 1.7 default).

#### LIB-66 · 설정 · 펼친 화면 배치
Priority **P1** · needsExternal **no**

Screen (head `설정 › 화면` / `펼친 화면`, board `LayoutSettings`): section `펼친 화면 배치`; radios `목록 + 상세` · `왼쪽 목록 · 오른쪽 고른 항목` (default) / `가운데 한 칸` · `읽기 폭 유지 · 양옆 여백`; note `서재 · 홈 · 인물 · 세계 · 연표 · 기록함 · 점검에 적용 · 커버 화면은 그대로 · 스크립트는 옆 패널 방식 유지`.

Sources: MAIN 커버 화면과 펼친 화면; board `LayoutSettings`.

#### LIB-67 · 설정 · 움직임 · 투명도
Priority **P1** · needsExternal **no**

Screen (head `설정` / `움직임 · 투명도`, board `ReduceSettings`): section `애니메이션`: radios `기기 설정 따름` · `‘애니메이션 삭제’가 켜져 있으면 줄임` (default) / `항상 줄이기` · `이동 없이 페이드 120 ms` / `줄이지 않음`; section `투명도`: `투명도 줄이기` · `유리 대신 불투명 막대 · 흐림 없음` (switch, off) with two previews `유리` / `불투명`; `라이프 레코더 빛` · `녹음 중 아래 빛 띠` (switch, on; same value as the recorder settings' `녹음 중 빛 띠`). Rendering effects: foundation (`ReduceTransparency` board shows opaque bars).

Sources: MAIN 접근성; boards `ReduceSettings`, `ReduceTransparency`.

#### LIB-68 · 설정 · 지도 · 이미지 · 연표 (container)
Priority **P1** · needsExternal **no**

Screen (head `설정` / `지도 · 이미지 · 연표`, board `SettingsMore`). This area builds the screen; each value's meaning belongs to World/Map and Timeline:

- `이미지`: `홀로그램 효과 기본` · `새 이미지에 · 이미지마다 바꾸기 가능` (switch off); `기본 이미지` · `8종 · 앱에 포함 · 오프라인` · `보기 ›`.
- `지도`: `새 지도 그림` · `색면 · 먹선 · 색 · 양피지` · `색면 ›`; `지형 만들기 · 글로 설명` · `기기 안 · 클라우드(정확도↑ · 전송)` · `기기 안 ›`; `큰 지도 내보내기` · `4K · 8K` · `4K ›`.
- `연표 · 때`: `때 표기 기본` · `새 프로젝트 · 프로젝트마다 바꾸기 가능` · `에피소드 기준 ›`; `이동 점검` · `이동 시간 부족 · 같은 때 · 동시에 두 장소` (switch on); `장소 없는 씬 알림` · `점검 센터 ◇` (switch on).

Sources: board `SettingsMore` (`scr_consist.py`).

#### LIB-69 · 프로젝트 설정 (container)
Priority **P0** · needsExternal **no**

Screen (head `<project name>` / `프로젝트 설정`, board `ProjSettings`). This area owns the screen, section `구조` › `단계 이름` and `세계 시간`, and section `등급 · 숨기기` (LIB-25). Other rows are rendered here but specified by their areas:

- `구조`: `단계 이름` · `장 › 에피소드 › 씬` · `바꾸기 ›` (edits Project.structure, same editor as LIB-10 직접 정하기; renaming keeps all data); `세계 시간` · `연도만 · 시작 1200년` · `연도만 ›` (time mode + start year; calendar editor: Timeline); `때 표기`, `이동 수단`, `하루 · 시간대` (Timeline / travel).
- `인물`: `인물 항목` · `게임 프로필 · 고정 7 · 프로젝트 7 · 장소 · 용어 · 세력도` · `고치기 ›`; `관계 종류` · `사이 12 · 마음 11` · `바꾸기 ›` (Cast).
- `등급 · 숨기기`: LIB-25.
- `스크립트 추가 칸`: `무대 위치`, `보이스 ID`, `ID 형식`, `전환 문구` (Script).
- Read-only example project: every row disabled.

Sources: board `ProjSettings` (`scr_decide.py proj_settings`); concept memory (project settings split).

### 2.11 상태 화면

#### LIB-70 · 빈 상태
Priority **P1** · needsExternal **no**

Shared pattern (`empty_block`): outlined card (radius 20), title (Nanum Myeongjo 800 22px), one sub line, a primary and an optional ghost button. Instances (hosts in brackets):

| Board | Title | Sub | Buttons |
|---|---|---|---|
| `OnbEmpty` (서재, this area) | `프로젝트 없음` | `프로젝트 = 원고 · 인물 · 세계 · 페이지 한 묶음` | `새 프로젝트` · `백업에서 되살리기` |
| `EmptyCast` (Cast) | `인물 없음` | `이름만 적어도 시작 · 나머지는 나중에` | `인물 추가` · `라이프 레코더에서` |
| `EmptySearch` (Search) | `결과 없음` | `‘<query>’ · 숨긴 프로젝트 제외` | section `비슷한 이름` + one suggestion row, ghost `‘<query>’ 용어로 만들기` |
| `EmptyCheck` (Check) | `어긋난 곳 없음` | `마지막 확인 · 방금 · 1-1 ~ 1-7` | `원고로` · `맞다고 표시한 것 2` |
| `EmptyLr` (Life Recorder) | `기록 없음` | `명령 칸 마이크 · 홈 위젯 · 빠른 설정` | `말로 기록` · `글로` |

Chips above the card show `전체 0`.

Sources: MAIN 상태 화면; boards listed.

#### LIB-71 · 오류 · 불러오는 중 공통 꼴
Priority **P1** · needsExternal **no**

- Errors use a full-screen or banner form with a coloured icon, a Nanum Myeongjo title, one sub line and verb buttons: `ErrBackup` (LIB-61), `ErrBio` (LIB-37, system UI), `ErrMic` (Life Recorder: `마이크 꺼짐` · `시스템 설정에서 허용 필요 · 녹음할 때만 사용` · `시스템 설정 열기` · `글로 쓰기`).
- Loading uses progress bars and stepped lists (`Restoring`, LIB-60); spinners are not drawn anywhere (`ED`: no spinner under 100 ms).

Sources: MAIN 상태 화면; boards `ErrBackup`, `ErrBio`, `ErrMic`, `Restoring`.

## 3. Data

Types: `Ulid` = 26-char ULID string generated on device; `Instant` = UTC instant; `LocalDate` in the device time zone. "File" column = how the value is stored in `.civilight` (path from LIB-49) or `—` (app-only, never in the file).

### 3.1 Enums

| Enum | Values (Korean label) |
|---|---|
| `Rating` | `ALL` (`ALL`, gloss `12세 이하`, long `ALL(R-12)`), `R15` (`R-15`, `15세 이상`), `R18` (`R-18`, `성인`) |
| `ProjectFormat` | `SERIAL` (`연재 장편`), `SHORT` (`단편`), `SETTING_ONLY` (`설정만`) |
| `StructurePreset` | `CHAPTER_EPISODE_SCENE` (`장 › 에피소드 › 씬`), `ACT_SCENE` (`막 › 장면`), `CUSTOM` (`직접 정하기`) |
| `TimeMode` | `YEAR_ONLY` (`연도만`), `WORLD_CALENDAR` (`세계 달력`), `NONE` (`시간 없이`) |
| `FieldBundle` | `BASIC` (`기본`), `GAME_PROFILE` (`게임 프로필`), `EMPTY` (`비어 있음`), `FROM_PROJECT` (`다른 프로젝트에서`) |
| `RelockPolicy` | `ON_LEAVE` (`앱을 떠날 때`), `SCREEN_OFF_ONLY` (`화면이 꺼질 때만`), `FIVE_MINUTES` (`연 지 5분 뒤`) |
| `ThemeMode` | `SYSTEM` (`기기 설정`), `LIGHT` (`라이트`), `DARK` (`다크`) |
| `UnfoldedLayout` | `LIST_DETAIL` (`목록 + 상세`), `CENTER_COLUMN` (`가운데 한 칸`) |
| `AnimationPref` | `FOLLOW_SYSTEM` (`기기 설정 따름`), `ALWAYS_REDUCE` (`항상 줄이기`), `NEVER_REDUCE` (`줄이지 않음`) |
| `LibraryView` | `LIST` (`목록`), `CARD` (`카드`) |
| `TrashKind` | `PROJECT`, `SCENE`, `BLOCK_GROUP`, `CHARACTER`, `PLACE`, `TERM`, `FACTION`, `CHANGE`, `RECORDER`, `FIELD_DEF` |
| `TrashFilter` | `ALL` (`전체`), `PROJECT` (`프로젝트`), `SCENE` (`씬`), `BLOCK` (`블록`), `CHARACTER` (`인물`), `WORLD` (`세계`), `CHANGE` (`변화`), `RECORDER` (`레코더`) |
| `TrashReason` | `DELETED`, `MERGED`, `REPLACED_BY_OVERWRITE` |
| `ExportFormat` | `PDF` (`대본 PDF`), `CSV` (`대사 표 CSV`), `ENGINE_JSON` (`게임 엔진 JSON`), `PROJECT_FILE` (`프로젝트 파일 .civilight`) |
| `HiddenSceneChoice` | `INCLUDE` (`넣기`), `EXCLUDE` (`빼기`) |
| `LockType` | `EXPORT` (field `암호`), `RECOVERY` (field `복구 암호`) |
| `VoiceIdState` (Script-owned, read here) | `AUTO` (`자동`), `FIXED` (`고정`), `DISCARDED` (`버린 ID`) |
| `CsvRowState` | `NEW` (`새`), `CHANGED` (`바뀜`), `DISCARDED` (`버림`) |
| `HideableKind` | `CHARACTER`, `TERM`, `PAGE`, `SCENE`, `PLACE`, `FACTION` |
| `Surface` | `LIBRARY_ROW`, `RECENTS_LIST`, `WIDGET_LIST`, `CROSS_SEARCH`, `NOTIFICATION`, `SYSTEM_RECENTS_PREVIEW`, `SYSTEM_RECENTS_LABEL`, `SCREENSHOT`, `TRASH_LOCATION`, `RECORDER_LOCATION` |

### 3.2 Entities owned by this area

**Project**

| Field | Type | Null | Meaning | File |
|---|---|---|---|---|
| key | Ulid | no | permanent project key; same key = same project (open/restore conflict) | manifest `project.key`, project.json |
| name | String (NFC) | no | `이름` | project.json; manifest `project.title` |
| oneLiner | String | yes | `한 줄 소개` | project.json |
| format | ProjectFormat | no | `형식` | project.json |
| rating | Rating | no | `수위 등급` | project.json; manifest `project.rating` |
| protections | Protections | no | embedded, below | project.json |
| structurePreset | StructurePreset | no | step 2 choice | project.json |
| structureLevels | List<StructureLevel> (1..4) | no | level names, top → bottom | project.json |
| timeMode | TimeMode | no | step 3 | project.json |
| startYear | Int | yes | `이야기가 시작되는 해`; null when NONE | project.json |
| calendarKey | Ulid | yes | world calendar (Timeline-owned) | project.json (definition included, FMT §4) |
| fieldBundleAtCreation | FieldBundle | no | step 4 bundle (field defs themselves: Cast) | project.json (informational) |
| coverImage | ImageRef | yes | 프로젝트 표지 (Images area) | assets |
| createdAt / updatedAt | Instant | no | timestamps | project.json |
| updatedByDevice | String | no | last device id (future sync, FMT §7) | project.json |
| lastOpenedAt | Instant | yes | for `열기 · 마지막 위치`, sorting | — |
| lastPosition | PositionRef | yes | last screen + block key | — |
| isExample / readOnly | Boolean | no | LIB-17 | — (example asset is a normal file) |
| deletedAt | Instant | yes | in 휴지통 when set | — |
| checklist | StartChecklist | yes | LIB-13 | — |
| unknownExt | JsonObject | yes | preserved `ext` + unknown keys (LIB-50) | written back |

**Protections** (embedded in Project)

| Field | Type | Meaning |
|---|---|---|
| hidden | Boolean | `프로젝트 숨기기` (state) |
| captureBlock | Boolean | `화면 캡처 가리기` |
| nameMask | Boolean | `이름 가리기` |
| recorderLinkMask | Boolean | `라이프 레코더 연결 가리기` |

**StructureLevel**: `key: Ulid`, `name: String` (e.g. `장`), `order: Int` (0 = top).

**RatingDefaults** (app-level, singleton)

| Field | Type | Factory |
|---|---|---|
| r18Hide | Boolean | true |
| r18Capture | Boolean | true |
| r18NameMask | Boolean | true |
| r18RecorderMask | Boolean | true |
| r15Capture | Boolean | false |
| exportCoverRating | Boolean | true (`내보내기 표지에 등급 표시`, all ratings) |
| changedGroups | Set<Rating> | empty; drives `이미 있는 … 같이 바꾸기` |

Fixed: R15 hide/name/recorder = false; ALL all = false.

**AppSettings** (singleton, app-only; included in the Drive app-settings backup file, never in `.civilight`)

| Field | Type | Default |
|---|---|---|
| theme | ThemeMode | SYSTEM |
| textSizeStep | Int | `보통` (18 sp · 1.7) |
| unfoldedLayout | UnfoldedLayout | LIST_DETAIL |
| animation | AnimationPref | FOLLOW_SYSTEM |
| reduceTransparency | Boolean | false |
| recorderGlow | Boolean | true (shared with Life Recorder) |
| appLock | Boolean | false (`ED`, §6 Q13) |
| relockPolicy | RelockPolicy | ON_LEAVE |
| hiddenInSearchWhileOpen | Boolean | true (`검색 · 명령 결과`) |
| hiddenOutOfWidgetsRecents | Boolean | true |
| backupHidden | Boolean | true (`숨긴 것도 백업`) |
| driveBackup | Boolean | false until connected |
| driveAccountId | String | null |
| firstUseTips | Boolean | true |
| viewByScreen | Map<String, LibraryView> | `library` → LIST |
| (map/image/timeline values) | — | owned by World/Timeline, stored here |

**OnboardingState** (app-only)

| Field | Type | Meaning |
|---|---|---|
| firstRunCompleted | Boolean | LIB-18 sets true |
| resumePage | String | page id to resume (`ED`) |
| walkthroughSeen | Boolean | `워크스루 · 봤음` |
| exampleState | enum NOT_INSTALLED · INSTALLED · DELETED | LIB-17/18 |
| moduleTourSeen | Map<ModuleId, Boolean> | modules: SCRIPT, CAST, WORLD_TIMELINE, RECORDER, LIBRARY_HIDING, MAP, ROUTE_PLACE_TIME |
| tipsShown | Set<String> | LIB-20 |
| emptyLibraryNoticeShown | Boolean | `예시 프로젝트 삭제됨` once |

**StartChecklist** (per project, app-only): `items: List<{ id: PROTAGONIST|LINKED_RECORDS|TERM|FIRST_LINE, done: Boolean }>`, `linkedRecordCount: Int`, `dismissed: Boolean`.

**UnlockSession** (runtime only, never persisted)

| Field | Type | Meaning |
|---|---|---|
| appUnlocked | Boolean | app lock passed |
| hiddenGroupUnlockedAt | Instant? | null = locked |
| itemsUnlocked | Map<ProjectKey, Instant> | per-project hidden items |
| lastBackgroundAt / lastScreenOffAt | Instant? | for RelockPolicy |

**RecentItem** (app-only): `key: Ulid`, `projectKey: Ulid`, `kind: SCENE|CHARACTER|PAGE|TERM|PLACE|FACTION`, `targetKey: Ulid`, `openedAt: Instant`. Max 20 kept (`ED`). Presentation always recomputed (masking).

**TrashEntry** (index; content stays in its owner's tables with `deletedAt`)

| Field | Type | Null | Meaning |
|---|---|---|---|
| key | Ulid | no | entry id |
| kind | TrashKind | no | |
| projectKey | Ulid | yes | null for RECORDER |
| targetKeys | List<Ulid> | no | the trashed item(s); BLOCK_GROUP = blocks of one scene deleted on one day |
| groupSceneKey / groupDate | Ulid / LocalDate | yes | BLOCK_GROUP grouping |
| reason | TrashReason | no | |
| mergedIntoKey | Ulid | yes | `합침 → <target>` |
| hiddenAtDelete | Boolean | no | render masked + auth to restore (also true if the project is hidden) |
| deletedAt | Instant | no | |
| purgeOn | LocalDate | no | deletedDate + 30 days |

**NameTag** (after permanent delete; R20 §5)

| Field | Type | Meaning |
|---|---|---|
| key | Ulid | the deleted item's key (references keep pointing at it) |
| kind | CHARACTER · PLACE · TERM · FACTION | |
| name | String | shown as `영구 삭제 · <name>` |
| voiceCode | String? | speaker part of voice IDs (e.g. `HRT`) |
| hidden | Boolean | shown as `영구 삭제 · 숨긴 항목` |
| purgedAt | Instant | |

File: `tombstones.json` (`ED`, §6 Q4).

**DeletionLogEntry**: `key: Ulid`, `kind: String`, `deletedAt: Instant`; kept 30 days (FMT §7). File: `deletions.json` (`ED`).

**RecoveryKeyMaterial** (app-only, `ED` design LIB-58): `kdf: String`, `iterations: Int`, `salt: ByteArray`, `dataKeyWrappedByPassword: ByteArray`, `dataKeyWrappedByKeystore: ByteArray`, `keystoreAlias: String`, `setAt: Instant`. Never the password.

**BackupRun** (app-only cache of the Drive run index)

| Field | Type | Meaning |
|---|---|---|
| runId | String | |
| startedAt / finishedAt | Instant | row `11월 20일 08:12` |
| status | OK · FAILED(reason) | LIB-61 |
| entries | List<BackupEntry> | |
| totalBytes | Long | `340 MB` |
| hiddenIncluded | Int | `숨김 2 포함` |
| hiddenExcluded | Int | `숨긴 프로젝트 n · 백업 안 됨` |

**BackupEntry**: `kind: PROJECT|RECORDER|SETTINGS`, `projectKey: Ulid?`, `driveFileId: String`, `versionId: String`, `sizeBytes: Long`, `encrypted: Boolean`, `lockType: LockType?`, `hidden: Boolean`, `hasHiddenItems: Boolean`.

**BackupState** (app-only): `lastSuccessAt: Instant?`, `failureStreakStartedAt: Instant?`, `lastFailureReason: String?`.

**ExportOptions** (transient sheet state)

| Field | Type | Default |
|---|---|---|
| format | ExportFormat | PDF |
| range | List<StructureNodeKey> | whole first chapter (`1장 전체`) |
| hiddenScenes | HiddenSceneChoice | PDF EXCLUDE · CSV/JSON INCLUDE |
| layers | Set<Layer> | 연출 · 괄호 지문 |
| includeSecretFields | Boolean | false |
| fileName | String | project name, or `civilight-내보내기-MMDD` for hidden/R-18 |
| pdfPassword | String? | on (required) for hidden/R-18 |
| encrypt | Boolean | hidden ∨ R-18 ∨ hasHiddenItems |
| password | String? | |
| includeRecorderAudio | Boolean | false |
| includeTrash | Boolean | false |

**ExportRecord** (per project, app-only + used for CSV state): `exportedAt: Instant`, `format`, `rangeKeys`, and per voice ID `lastExportedTextHash`, `lastExportedSpeakerKey` (`ED`, for `바뀜`).

**FileManifest** (LIB-50 JSON): `format`, `formatVersion`, `minReaderVersion`, `features.required[]`, `features.optional[]`, `writtenBy`, `writtenAt`, `project{key,title,rating}`, `encrypted`, `files{path → "sha256-…"}`.

**EncryptionHeader** (`ED`, LIB-51): `format`, `lockType`, `kdf`, `iterations`, `salt`, `cipher`, `nonce`, optional `wrappedDataKey` (recovery lock).

### 3.3 Contracts on entities owned by other areas

| Owner | Entity | Field this area needs | Meaning |
|---|---|---|---|
| Cast / World / Pages / Script | Character, Term, Page, Scene, Place, Faction | `hidden: Boolean` | item hiding (LIB-36); stored in `.civilight` |
| all owners | every trashable entity | `deletedAt: Instant?`, `trashEntryKey: Ulid?` | trash host (LIB-38) |
| Script | Block | `voiceIdState`, `voiceId`, `fixedAt: LocalDate?` | fixed by LIB-46; stored in file |
| Script | Scene | resolved time/place/transition (computed) | JSON export |
| Life Recorder | LrRecord | `links: List<Link>`, `hidden: Boolean`, `state` | start-material links, export inclusion, masking |
| Cast | FieldDefinition | `exportName`, `secret: Boolean` | JSON `fields` |
| Images | ImageRef | content hash + crops + holo | assets/ |

### 3.4 What `.civilight` must contain (summary)

Always: project (with rating, protections, structure, time settings, field definitions), story index + per-episode blocks (with permanent keys, numbers, voice-ID state, fixed/discarded IDs), cast + relationships, world + timeline events, changes, maps (with generated results), canvases, pages, assets (originals by hash), linked recorder records (text; audio optional; hidden records only when the project is hidden and the file encrypted), hidden flags of everything, check `맞음` marks, term `아님`/`맞음`, name tags, deletion log, per-item modified time/device, `ext`. Optional: trash (`휴지통도 포함`), recorder audio, `preview.webp` (not for hidden projects). Never: app settings, derived values, search index, recents, unlock state, Drive state, the example flag.

## 4. Rules as test cases

All cases are JVM unit tests against pure functions; signatures are `ED` suggestions. Injected clock `now = 2026-11-22T09:00+09:00`. Sample data: projects 「잿빛 새벽의 등불」 (R-15, visible, 2 chapters planned, 1-7 쓰는 중), 「유리 정원」 (ALL, 단편, 씬 6), 「검은 서약 (가제)」 (R-18, hidden, 설정만, 인물 5), 「외전 · 오웬의 시장」 (R-15, hidden, 씬 2). Factory RatingDefaults unless stated.

### 4.1 Rating change sheet — `ratingSheet(from, to, current: Protections, defaults, recoverySet: Boolean, unencryptedBackups: Int, linkedRecords: Int): RatingSheet` and `applyRatingSheet(...)`

| # | Given | When | Then |
|---|---|---|---|
| R19-01 | 잿빛 R-15, current all false, no 복구 암호, Drive holds 3 unencrypted versions, 1 linked record | ratingSheet(R15 → R18) | direction RAISE; rows [hidden ✓, capture ✓, nameMask ✓, recorderMask ✓]; no right labels; recorder sub `연결에 등급만 · 기록 1 바로 적용`; extra rows [`복구 암호 없음`, `이전 백업 3 · 암호 없음`]; note `바꾸면 ‘숨긴 프로젝트’ 묶음으로 · 지금은 열린 채`; small line `체크 · R-18 기본값 · 설정 › 수위 등급` |
| R19-02 | R19-01 sheet, all checked | apply | rating R18; protections all true; requiresAuth false; project in hidden group but `UnlockSession` keeps it open until re-lock; `recorderLinkLabel(잿빛)` = `R-18` for record ‘하르트가 등불을 싫어하는 이유’; recent items of 잿빛 removed from `recentsFor(LIST)` immediately |
| R19-03 | same as R19-01 but 복구 암호 set and 0 unencrypted backups | ratingSheet(R15 → R18) | no extra rows |
| R19-04 | 검은 서약 R-18, current all true | ratingSheet(R18 → R15) | direction LOWER; all four rows checked; right label `R-15 기본 · 끔` on each; hidden row sub `풀면 바꿀 때 지문 확인`; note `내려도 저절로 풀리는 것 없음`; small line `체크 · 지금 켜진 보호 · 체크를 풀어야 꺼짐` |
| R19-05 | R19-04 sheet, nothing unchecked | apply | rating R15; protections unchanged (all true); requiresAuth false |
| R19-06 | R19-04 sheet, hidden unchecked | apply | requiresAuth true; on auth success → hidden false, others true; on auth cancel → rating stays R18, protections unchanged |
| R19-07 | 잿빛 lowered to R-15 earlier with nameMask kept on, then | ratingSheet(R15 → R18) again | built fresh: checks = current ∨ R-18 defaults (all ✓); no memory of the earlier sheet |
| R19-08 | 유리 정원 ALL, current capture = true (set by hand), R-15 default capture false | ratingSheet(ALL → R15) | capture row checked (current ∨ default; §6 Q7); others unchecked |
| R19-09 | any project | ratingSheet(X → X) | null (no sheet, no change) |
| R19-10 | 2 existing R-18 projects | user turns `이름 가리기` off in 설정 · 수위 등급 | existing R-18 projects keep nameMask true; tag text `이미 있는 R-18 프로젝트 2 · 같이 바꾸기`; a new R-18 project gets nameMask false |
| R19-11 | `newProjectProtections(R18, defaults, step4Hide = false)` | create | hidden false; capture true; nameMask true; recorderMask true; summary contains `숨김 · 끔` |
| R19-12 | `newProjectProtections(R15, defaults, step4Hide = null)` | create | all false |
| R19-13 | `newProjectProtections(R18, defaults with r18Hide=false, step4Hide = null)` | create | hidden false; step-4 toggle initial value false |
| R19-14 | 잿빛 R-15, captureBlock turned on in 프로젝트 설정 (R-15 default off) | `projectSettingsRows(project, defaults)` | 화면 캡처 가리기 sub ends with `R-15 기본과 다름`; other rows have no marker; rating stays R15 |
| R19-15 | 검은 서약 R-18, all protections on (= R-18 defaults) | projectSettingsRows | no `기본과 다름` marker on any row |

### 4.2 Masking surfaces — `projectSurface(surface, project, unlock): Presentation` (R19 §3)

Fixtures: H = 검은 서약 (hidden, R-18), M = R-18 project not hidden with nameMask on (use 잿빛 after R19-02 with hidden unchecked), N = 유리 정원.

| # | Surface | H (group locked) | M | N |
|---|---|---|---|---|
| S-01 | LIBRARY_ROW | not a row; counted in `숨긴 프로젝트 2` | row with real name + red badge | real name, grey badge |
| S-02 | RECENTS_LIST / WIDGET_LIST | omitted | `R-18 프로젝트` | real name |
| S-03 | CROSS_SEARCH | omitted, footer `숨긴 프로젝트 제외` | included, badge | included |
| S-04 | NOTIFICATION | counted `숨긴 프로젝트 1` | `R-18 프로젝트` | real name |
| S-05 | SYSTEM_RECENTS_PREVIEW | masked (recents only) | masked (capture block) | shown |
| S-06 | SYSTEM_RECENTS_LABEL | `Civilight` | `R-18 프로젝트` | `Civilight` |
| S-07 | SCREENSHOT | follows H.captureBlock (true → black) | black | allowed |
| S-08 | TRASH_LOCATION / RECORDER_LOCATION | `숨긴 프로젝트`, restore needs auth | real name | real name |
| S-09 | CROSS_SEARCH, H with group **open** and `hiddenInSearchWhileOpen` = true | included | — | — |
| S-10 | captureGuard: visible = open hidden group row of H (captureBlock true) | FLAG_SECURE on | — | — |
| S-11 | captureGuard: visible = 기록함 row linked to M (M.captureBlock true) | — | FLAG_SECURE on | — |
| S-12 | captureGuard: visible = only the masked label `R-18 프로젝트` of M (최근 연 파일) | — | FLAG_SECURE off (`ED`) | — |

Notification text — `backupDoneText(entries)` (`ED` rule, §6 Q17):

| # | Run contents | Text |
|---|---|---|
| N-01 | three non-hidden projects + 검은 서약 (hidden) | `백업 끝 · 프로젝트 3 · 숨긴 프로젝트 1` |
| N-02 | 잿빛 (N), 유리 정원 (N), 검은 서약 not hidden but nameMask on | `백업 끝 · 잿빛 새벽의 등불 · 유리 정원 · R-18 프로젝트` |

### 4.3 Lock screen — `lockScreenMode(lastProject, appLock, relockDue, authResult)` (R19 §4)

| # | Given | Then |
|---|---|---|
| L-01 | last = 잿빛 1-7 씬 3 (normal), appLock on, returning from background | BLURRED_CONTENT; title `Civilight`; `잠김 · 지문으로 열기`; `기기 PIN 사용` |
| L-02 | last = 검은 서약 (hidden), appLock off, relock due | MARK_ONLY_HIDDEN; `숨긴 프로젝트`; `잠김 · 지문으로 열기`; buttons `PIN 사용` · `서재로` |
| L-03 | last = 검은 서약, appLock on | first MARK_ONLY app lock; after success → MARK_ONLY_HIDDEN asks again (two separate auths) |
| L-04 | last = R-18 not hidden, captureBlock on, appLock on | MARK_ONLY, no project name |
| L-05 | L-02 and auth cancelled | navigate to 서재; hidden group locked |
| L-06 | last = 검은 서약 or capture-blocked project | `takeBlurSnapshot` returns false (no pre-captured screen) |

Re-lock — `relockDue(policy, unlockedAt, events, now)`:

| # | Policy | Events since unlock | Due? |
|---|---|---|---|
| RL-01 | ON_LEAVE | app backgrounded once | yes |
| RL-02 | ON_LEAVE | screen off | yes |
| RL-03 | SCREEN_OFF_ONLY | app backgrounded, returned, no screen off | no |
| RL-04 | SCREEN_OFF_ONLY | screen off | yes |
| RL-05 | FIVE_MINUTES | unlocked 08:55, now 09:00 | yes (≥ 5 min) |
| RL-06 | FIVE_MINUTES | unlocked 08:56, now 09:00, app backgrounded | no |
| RL-07 | any | `다시 잠그기` pressed | yes, immediately |

### 4.4 Item hiding — `itemSurface(surface, locked)` (R19 §6)

Fixture: 노아 hidden in 잿빛; 1-7 씬 4 hidden.

| # | Surface | Locked | Open |
|---|---|---|---|
| I-01 | cast list | single row `숨긴 인물 1` with `지문으로 보기`; list total excludes 노아 | 노아 in place + `숨김` tag |
| I-02 | story structure | `1-7 씬 4 · 숨긴 씬`, title masked | title + `숨김` |
| I-03 | script, 1-7 씬 4 with 12 blocks | one line `숨긴 씬 · 블록 12 · 지문으로 보기` | blocks |
| I-04 | speaker cell of 노아's line in 1-6 씬 5 | `숨긴 인물`; line text unchanged | `노아` |
| I-05 | narration text "노아가 웃었다" (plain text) | unchanged | unchanged |
| I-06 | speaker picker | 노아 absent; last row `숨긴 인물 1 · 지문으로 보기` | listed |
| I-07 | search "노아" | no card result; visible scenes' text hits remain; footer `숨긴 항목 제외` | all |
| I-08 | 오웬's relations | row `숨긴 인물과의 관계 1` | relation shown |
| I-09 | whole relationship map, 12 characters, 5 shown at lens | `12명 중 5명 · 숨김 1`; 노아 node/lines not drawn | drawn |
| I-10 | lens ◆ list with 2 changes of 노아 | `숨긴 항목 변화 2` | rows |
| I-11 | 점검 센터 with 2 warnings about 노아 | `▲ 숨긴 항목 관련 2 · 지문으로 보기` (checks still computed) | original rows |
| I-12 | route map | 1-7 씬 4 stretch dashed `숨긴 씬`; no route for 노아 | as is |
| I-13 | recorder link to 노아 | `숨긴 항목` | `노아` |
| I-14 | 휴지통 entry of hidden 노아, deleted 2026-10-26 | `숨긴 인물 · 3일 남음`; restore requires auth | `노아` |
| I-15 | recents | omitted | omitted |
| I-16 | edge: hidden 노아 speaks in visible 씬 3 | 점검 ◇ `숨긴 인물이 보이는 씬에 나옴 · 씬 3`, actions `씬도 숨기기` · `그대로`; scene NOT auto-hidden | — |
| I-17 | edge: @ candidates while locked, query "노" | 노아 absent; only `숨긴 인물 1 · 지문으로 보기` | present |
| I-18 | edge: 결사의 문 (place) hidden, map | pin and region not drawn while locked | drawn |

### 4.5 Export decisions

| # | Given | When | Then |
|---|---|---|---|
| E-01 | `hiddenSceneDefault(format)` | PDF / CSV / ENGINE_JSON | EXCLUDE / INCLUDE / INCLUDE; PROJECT_FILE → not asked (everything) |
| E-02 | 잿빛 1장 range contains hidden 1-7 씬 4 | JSON sheet opened | row `숨긴 씬 1 · 1-7 씬 4` with `넣기` selected |
| E-03 | E-02, user presses create | — | exactly one auth prompt before writing; cancel → nothing written, no voice IDs fixed |
| E-04 | E-02 with `빼기` | — | no auth; scene absent from JSON |
| E-05 | 노아 hidden, JSON export of 1-6 씬 5 | write | 노아's lines keep `speaker` = 노아's id and name `노아`; characters[] entry for 노아 has only id · name · displayName |
| E-06 | JSON `빼기`, a choice in 1-7 씬 3 leads into hidden 1-7 씬 4 | build sheet | ▲ `숨긴 씬으로 가는 선택지 1` shown |
| E-07 | CSV `빼기` excluding a scene with fixed IDs | write | those IDs not in file and not marked `버림` |
| E-08 | `encryptDefault(project)` | 잿빛 R-15, no hidden items / 잿빛 with 노아 hidden / 검은 서약 / R-18 not hidden | false / true / true / true |
| E-09 | `defaultFileName(project, now)` | 잿빛 R-15 / 검은 서약 hidden / R-18 not hidden | `잿빛 새벽의 등불` / `civilight-내보내기-1122` / `civilight-내보내기-1122` |
| E-10 | PDF of 검은 서약 | sheet | `PDF 암호` row present, on |
| E-11 | `.civilight` of 잿빛, 4 linked records, 1 hidden, project not hidden | sheet / write | row `레코더 기록 4 · 원본 음성 포함 · 숨긴 기록 1 제외`; file contains 3 records; audio absent |
| E-12 | `.civilight` of 검은 서약 (hidden, encrypted) with a hidden linked record | write | hidden record included |
| E-13 | PDF / CSV / JSON of any project with linked records | write | no recorder records in the file |
| E-14 | voice ID HRT_0107_043 state AUTO, block in range | first CSV export on 2026-11-20 | state FIXED, fixedAt 2026-11-20; CSV row state `새` |
| E-15 | E-14 then speaker of #043 changed to 리엔, ID stays HRT_0107_043 | second CSV export | row state `바뀜` (`ED` definition) |
| E-16 | E-14 then block #043 deleted | CSV export | row HRT_0107_043 with state `버림` |
| E-17 | PDF and `.civilight` exports | write | no voice-ID state changes |
| E-18 | 1-7 씬 4 has transition phrase `결사의 문 앞 · 06:10` (manual) | CSV | one row: 화자 `전환 문구`, 대사 `결사의 문 앞 · 06:10`, 블록 ID = phrase ID, 보이스 ID empty |
| E-19 | JSON of 1-7 씬 4 (결사의 문, 2일째 아침 06:10, 정확, not flashback) | write | scene.place.name `결사의 문`; time.day 2, band 아침, clock 06:10, precision 정확; flashback false; transition {text `결사의 문 앞 · 06:10`, mode manual, id}; no scene-head block in blocks[] |
| E-20 | JSON characters with field 신장 (export name `height`) and a secret field | export, `비밀 항목 포함` off | `fields.height` present; secret field absent |

### 4.6 `.civilight` format

| # | Given | When | Then |
|---|---|---|---|
| F-01 | export of 잿빛 | write | ZIP entry 0 is `mimetype`, method STORED, bytes `application/vnd.civilight.project`; manifest lists every other file with `sha256-` hash |
| F-02 | name typed as NFD `잿빛` (decomposed jamo) | write then read | stored and read as NFC; search by NFC `잿빛` matches |
| F-03 | file formatVersion 1.0 written by 1.0 app | read by app supporting up to 1.2 | opens; upgrade steps 1.0→1.1→1.2 applied in order; source file bytes unchanged |
| F-04 | manifest minReaderVersion 1.4, app max 1.2, required [`maps.gen.v2`] | `openDecision` | REFUSE_NEWER; sheet rows `양식 1.4`, `1.2까지`, `지도 생성 2판`; nothing imported |
| F-05 | manifest formatVersion 1.3, minReaderVersion 1.0, optional [`maps.gen.v2`] unknown, app max 1.2 | openDecision | IMPORT (optional part skipped) |
| F-06 | required contains unknown `story.blocks.v3` while minReaderVersion 1.0 | openDecision | REFUSE_NEWER (feature list beats version number) |
| F-07 | hash of `story/<1-7 key>.json` mismatches | import | all other files imported; result message `열지 못한 부분 · 1-7` |
| F-08 | unknown `ext` object on a character and unknown file `labs/x.json` | import then re-export same project | both written back unchanged (`ED` preservation) |
| F-09 | existing project with key K; file with key K | openDecision | ASK_SAME_PROJECT; default `새 프로젝트로` labelled `‘잿빛 새벽의 등불 (2)’로 추가` |
| F-10 | F-09, choose 새 프로젝트로 | import | new project key ≠ K; name `잿빛 새벽의 등불 (2)`; item keys kept inside the new project (`ED`); original project untouched |
| F-11 | F-09, a project named `잿빛 새벽의 등불 (2)` already exists | import as new | name suffix (3) (`ED` next free number) |
| F-18 | library already has record R (key KR) linked to 잿빛; file of 잿빛 imported as new project contains R | import | still one record KR; links = [잿빛, 잿빛 (2)] (R23 §6) |
| F-19 | backup contains 잿빛 and 유리 정원, both linked to record KR; library is empty | restore both | one record KR with two links (R23 §6 "같은 영구 키면 하나로 합쳐 연결만 더한다") |
| F-12 | F-09, choose 덮어쓰기 | import | old project → TrashEntry(kind PROJECT, reason REPLACED_BY_OVERWRITE, purgeOn 2026-12-22); imported project keeps key K |
| F-13 | encrypted export file, lockType EXPORT | open | password field label `암호`, hint `내보낼 때 정한 암호`; manifest not readable before decrypt |
| F-14 | Drive backup file of hidden project, lockType RECOVERY | open from file | field label `복구 암호` |
| F-15 | decrypt success of a hidden project file | import | project hidden on this device (biometric re-lock) |
| F-16 | hidden project export | write | no `preview.webp` |
| F-17 | encrypted container | inspect outer ZIP | entries exactly [`mimetype`, `encryption.json`, `payload.bin`] (`ED` layout) |

### 4.7 Backup

| # | Given | When | Then |
|---|---|---|---|
| B-01 | 2 hidden projects, no 복구 암호 | `backupPlan` | hidden projects excluded; 설정 shows `숨긴 프로젝트 2 · 백업 안 됨` |
| B-02 | 2 hidden projects, 복구 암호 set, `숨긴 것도 백업` on | backupPlan | both included, encrypted lockType RECOVERY |
| B-03 | B-02 with `숨긴 것도 백업` off | backupPlan | both excluded |
| B-04 | 잿빛 with 3 hidden items, no 복구 암호 | backupPlan | included unencrypted; ◇ `숨긴 항목 3 · 암호 없이 백업 · 복구 암호 정하기 ›` |
| B-05 | 잿빛 with hidden items, 복구 암호 set | backupPlan | included, encrypted with recovery key (`ED`) |
| B-06 | 유리 정원 unchanged since last upload | backupPlan | not uploaded; run index references previous version |
| B-07 | versions older than 30 days not referenced by any run within 30 days | prune | deleted |
| B-08 | restore list run 11월 20일 08:12: 3 projects incl. 2 hidden, 340 MB | row text | `11월 20일 08:12` · `프로젝트 3 · 숨김 2 포함 · 340 MB`; button `11월 20일 백업 되살리기` |
| B-09 | last success 2026-11-18 08:12; first failed run 2026-11-20 (Google login expired); now 11-22 | banner | `백업 실패 · 3일째` · `Google 로그인 만료 · 원고는 기기에 안전`; Drive row `마지막 성공 11월 18일 08:12` (`ED` 1-based counting) |
| B-10 | first failed run today | banner | `백업 실패 · 1일째` (`ED`) |

### 4.8 Trash

`daysLeft(deletedAt, now) = 30 − daysBetween(localDate(deletedAt), localDate(now))`.

| # | Given | Then |
|---|---|---|
| T-01 | 유리 정원 deleted 2026-11-21 | `29일 남음`; sub `프로젝트 · 씬 6개 · 인물 4명` |
| T-02 | 4 blocks of 1-7 씬 3 deleted today | one row `1-7 씬 3 · 블록 4개` · `블록 · 잿빛 새벽의 등불 · 오늘 지움` · `30일 남음` |
| T-03 | blocks of 1-7 씬 3 deleted today and 2 more yesterday | two rows (grouped per scene per day) |
| T-04 | an empty block deleted | no trash entry, no toast |
| T-05 | 1-6 씬 5 · 시장의 밤 deleted 2026-11-04 | `12일 남음`; sub `씬 · 잿빛 새벽의 등불` |
| T-06 | 노아 deleted 2026-10-26 with 14 dialogue references, 1 relationship | `3일 남음`; sub `인물 · 잿빛 새벽의 등불 · 대사 14 · 관계 1` |
| T-07 | recorder record 0:12 deleted 2026-10-24 | `음성 0:12` · `라이프 레코더` · `1일 남음` |
| T-08 | entry deleted 2026-10-23 | purged on 2026-11-22 run (daysLeft 0) |
| T-09 | filter WORLD with a trashed term 오리지늄 등불 and place 잿더미 시장 | both listed; filter CHARACTER excludes them |
| T-10 | filter chips | exactly `전체 · 프로젝트 · 씬 · 블록 · 인물 · 세계 · 변화 · 레코더` |
| T-11 | merged 시장 꼬마 → 노아 | entry kind CHARACTER, reason MERGED, label contains `합침 → 노아` |
| T-12 | 영구 삭제 of 노아 (voice code `NOA`, `ED` sample code) | NameTag {name 노아, kind CHARACTER, voiceCode NOA}; references render `영구 삭제 · 노아`; fixed IDs unchanged |
| T-13 | 영구 삭제 of a hidden character | NameTag.hidden true; references render `영구 삭제 · 숨긴 항목` |
| T-14 | restore a block whose scene is in the trash | choices `씬도 되살리기` · `다른 씬 끝에` |
| T-15 | restore block #043 (voice FIXED) | number #043 and FIXED state unchanged; ◆ waiting as 근거 지워짐 re-attaches |

### 4.9 New project and onboarding

| # | Given | When | Then |
|---|---|---|---|
| P-01 | step 4 첫 씬 제목 blank, format SERIAL, structure 장 › 에피소드 › 씬 | create | chapter `장 1`, episode `1-1`, scene titled `씬 1`; LIB-13 shows `1장 · 에피소드 1-1 · 씬 1` |
| P-02 | step 4 주인공 `리엔` | create | one character 리엔; checklist item 1 done, sub `리엔 · 완료`; header `시작해 볼 것 · 1/4` |
| P-03 | 3 unsorted records, 2 checked | create | the 2 records gain a link to the new project; their text not inserted anywhere; checklist `연결된 기록 2개 정리하기` |
| P-04 | format SETTING_ONLY | bundle default | `기본` (never guessed) |
| P-05 | bundle `다른 프로젝트에서`, hidden group locked | picker | hidden projects not listed |
| P-06 | step 1 name `  ` (blank) | `다음` | disabled |
| P-07 | onboarding finished via `온보딩 끝내기` | state | example project permanently deleted (no TrashEntry); exampleState DELETED; 서재 shows `프로젝트 · 0`, `프로젝트 없음`, `예시 프로젝트 삭제됨` |
| P-08 | example project | any write API | rejected (readOnly) |
| P-09 | `rowMeta(잿빛)` with 1-7 furthest episode, chapter 1 has 10 episodes, 12 characters, modified today 08:12 | — | `1장 7/10 · 인물 12 · 오늘 08:12` |
| P-10 | `rowMeta(유리 정원)` SHORT, 6 scenes, modified 2026-11-19 | — | `단편 · 씬 6개 · 3일 전` (`ED` time format) |
| P-11 | `rowMeta(검은 서약)` SETTING_ONLY, 5 characters, modified 2026-11-02 | — | `설정만 · 인물 5 · 11월 2일` |
| P-12 | `relativeTime` (`ED`) | 2026-11-21 / 2026-11-18 / 2026-11-15 | `어제` / `4일 전` / `11월 15일` |

## 5. Screens

Cover = 390×844 board (Compact width). Inner = 800×800 board (Medium/Expanded). Components are design-system names (`GlassBars`, `CommandBar`, `Sheet`, `ListRow` (`.li`, `.row`), `LockRow`, `Tag` (chips, rating badge), `Buttons`, `Controls` (switch, radio, checkbox), `Toast`, `ViewToggle`, `ProjectCard`, `ImageTile`, `Tabs`). "Sheet on cover / centered sheet or side panel on inner" is the general rule (DS Sheet; R20 §4: cover bottom sheet, unfolded centered sheet).

| Screen / state | Boards | Cover | Inner | Components | Features |
|---|---|---|---|---|---|
| Splash | `Splash`, `SplashStrip` | system splash → mark → 서재 | same | mark | LIB-08 |
| 서재 (list) | `Library` | single column, header `+`, footer command bar | `TwoLibrary` list + project home; `ColLibrary` 560 column | GlassBars, CommandBar, ListRow, LockRow, Tag | LIB-01, 06 |
| 서재 card view | `LibCards` | 3:4 card row + recents row + library row | `LibCardsWide` bigger cards + `새 프로젝트` card + 4 recents | ViewToggle, ProjectCard, pager | LIB-04, 05 |
| 서재 motion | `MCardView`, `MCarousel`, `MUnlock` | list ↔ card morph; carousel snap; unhide blur | same | — | LIB-04, 32 |
| Project ⋯ sheet | `LibMenu` | bottom sheet | centered sheet (`ED`) | Sheet, ListRow | LIB-02 |
| Hidden group unlock | `LibUnlock`, `ErrBio` | system biometric prompt | same | system | LIB-32, 37 |
| Hidden group open | `LibUnlocked` | expanded tinted group, header `다시 잠그기` | in left list pane | ListRow, Buttons | LIB-31–33 |
| Project delete confirm | `ConfirmDelete` | centered dialog | centered dialog | Buttons (r18 fill) | LIB-03 |
| Rating picker + `바뀌는 것` | `RHRatingSheet`, `RHRatingDown` | bottom sheet over 서재 | centered sheet | Sheet, Controls (checkbox), Tag | LIB-24 |
| New project 1/4 | `NewName`, `NewNameR18` | full page, progress bar, bottom glass buttons | `NewWide` pattern: left form + right preview | Controls (radio cards), fields, rating picker | LIB-09 |
| New project 2/4 | `NewStructure`, `NewWide` | radio cards + tree preview below | form left, live tree in right aside | radio cards, tree | LIB-10, 14 |
| New project 3/4 | `NewTime` | radio cards + year field | `NewWide` pattern | radio cards, field | LIB-11 |
| New project 4/4 | `NewStart` | fields, checkbox list, bundle row, hide switch | `NewWide` pattern | checkbox, switch | LIB-12 |
| 만든 직후 | `NewReady` | project home top block + checklist | right pane of TwoLibrary (`ED`) | ListRow, Buttons | LIB-13 |
| 첫 실행 | `OnbWelcome`, `OnbLook`, `OnbSafe`, `OnbVoice` | full pages | centered 560 column (`ED`) | Buttons, switches, swatches, slider | LIB-15 |
| Walkthrough | `Walk1`, `Walk2`, `Walk3` | demo card + text + buttons | centered (`ED`) | demo animations | LIB-16 |
| Example project | `OnbExample`, `OnbExampleScript` | read-only banner floating at top | same with inner layouts | glass banner | LIB-17 |
| Empty 서재 | `OnbEmpty` | empty card | same in list pane | empty block | LIB-18, 70 |
| Tooltip tours | `Tour1`, `Tour2`, `Tour4`, `TourCast`, `TourWorld`, `TourLr`, `TourMap`, `TourWhen` | shade + hole + tooltip card | same, anchored to target | tooltip | LIB-19 |
| Onboarding comparison (rejected style) | `CoachScript` | not built | — | — | Scope |
| Help replay | `HelpReplay` | settings list | settings in list+detail (`ED`) | ListRow, Tag | LIB-21 |
| 설정 | `Settings` | list | list + detail (`ED`) | ListRow, switches, segmented | LIB-63 |
| 설정 · 숨기기 | `HideSettings` | list | detail pane | radio, switch | LIB-64 |
| 설정 · 수위 등급 | `RatingSettings` | list | detail pane | switch, Tag | LIB-23 |
| 설정 › 보관 | `ErrBackup` | banner + rows | detail pane | banner, switch | LIB-61, 62 |
| 복구 암호 정하기 | `RHRecoveryPw` | full page, bottom primary | detail pane | password fields, meter | LIB-58 |
| 설정 · 펼친 화면 배치 | `LayoutSettings` | list | detail pane | radio | LIB-66 |
| 설정 · 움직임 · 투명도 | `ReduceSettings`, `ReduceTransparency` | list with previews | detail pane | radio, switch | LIB-67 |
| 설정 · 지도 · 이미지 · 연표 | `SettingsMore` | list | detail pane | ListRow | LIB-68 |
| 프로젝트 설정 | `ProjSettings` | list | detail pane | ListRow, switch | LIB-25, 69 |
| 최근 앱 가림 | `Recents` | system recents | system | — | LIB-29 |
| App lock | `AppLock` | blurred content + mark | same | fingerprint button | LIB-34, 35 |
| Hidden lock (mark only) | `RHLockMark` | mark only + `PIN 사용` · `서재로` | same | — | LIB-34 |
| 항목 숨기기 (cast list example) | `ItemHide` | lock row in list | list pane | LockRow | LIB-36 |
| 휴지통 | `Trash` | filter chips + rows | list + detail (`ED`) | Tag chips, ListRow | LIB-38, 39 |
| Undo toast | `UndoToast` | toast above bottom bar | same | Toast | LIB-40 |
| 내보내기 | `Export` | bottom sheet over project | centered sheet / side panel (`ED`) | radio cards, rows, segmented | LIB-41–48, 72 |
| 파일에서 열기 | `RHOpenFile` | sheet over the system file app | centered sheet | radio, Buttons | LIB-52 |
| 새 버전 파일 | `RHNewerFile` | sheet | centered sheet | rows | LIB-53 |
| 암호 파일 | `RHEncryptedFile` | sheet | centered sheet | password field | LIB-54 |
| 백업에서 되살리기 | `Restore` | list + bottom primary | detail pane | ListRow, segmented | LIB-59 |
| 되살리는 중 | `Restoring` | progress page | same | progress bar, steps | LIB-60 |
| Empty / error states | `EmptyCast`, `EmptySearch`, `EmptyCheck`, `EmptyLr`, `ErrMic` | in host screens | in host panes | empty block | LIB-70, 71 |

Not covered here (other areas): `LrSettings` (Life Recorder), `ImagePick` / `ImageCrop` / `ImageSlots` (images), `RCDeleteImpact` / `RCMerge` (Cast), `Home` (project home).

## 6. Open questions

Only real contradictions or gaps. Each states both sides and the default this spec took.

1. **Default file name for hidden / R-18 exports.** FMT §5: `숨긴 프로젝트 2026-11-21.civilight`. R19 §5 and board `RHEncryptedFile`: `civilight-내보내기-1122`. Default taken: R19 (newer, confirmed 2026-11-22).
2. **What is visible outside an encrypted file.** FMT §5: only `mimetype` and encryption info; title, cover, rating inside. FMT §3 manifest example carries `project.title` and `rating` in clear and says KDF/cipher names go "in the manifest". Default: outer `encryption.json` holds KDF/cipher; the real manifest is inside the payload (LIB-51 `ED`).
3. **인물 표 CSV.** CF §8: a character-table CSV "지금 내보내기 형식에는 없다 … 더할지 정해야 한다"; the export sheet has four formats only. Default: not built; decide whether to add a fifth format.
4. **Where name tags, merge records, check `맞음` / term `아님` marks, voice-ID states/discarded IDs and the deletion log live in the file.** R20 §5 defers name-tag storage to 저장 파일 양식, which does not define it; FMT §6 requires voice states and `맞음` "always" but the file tree has no file for them. Default `ED`: `tombstones.json`, `checks.json`, `voice.json` (or per block), `deletions.json`.
5. **Hidden items in auto backup.** R19 §5: "숨긴 항목 내용을 그 암호로 잠근다" (item-level). The format only defines whole-file encryption. Default `ED`: encrypt the whole project file with the 복구 암호 when it has hidden items and a 복구 암호 exists.
6. **Recents preview masking feasibility.** R19 §3: hidden projects mask the recents preview only (screenshots follow capture setting). Android 29–32 has no recents-only API; FLAG_SECURE blocks screenshots too. Board `Recents` draws `R-18 프로젝트 · 미리 보기 가림` inside the card, which the system does not render under FLAG_SECURE. Default: API 33+ `setRecentsScreenshotEnabled(false)`, API 29–32 FLAG_SECURE; card name via task label. Needs a real-device check.
7. **Raising checks when a protection is already on but the new default is off.** R19 §2: "올릴 때는 새 등급 기본값이 체크된 채로"; R19 rule and memory: "nothing auto-released". Default: checked = current ∨ new default.
8. **`R-18 프로젝트` label for a project whose rating is no longer R-18** (lowered with 이름 가리기 kept on). Sources only give the R-18 label. Default: keep the literal `R-18 프로젝트`. Alternative: `<rating> 프로젝트`.
9. **Replaying the walkthrough / tours after the example is deleted.** Onboarding deletes the example (MAIN), but the walkthrough and the script tour boards are drawn on the example with the banner `예시 · 읽기 전용 · 온보딩 후 삭제`, and HelpReplay offers `다시 보기`. Default: walkthrough demos are self-contained; module tours replay on the user's own project; the example is not reinstalled.
10. **Step 2 구조 for 형식 단편 / 설정만.** 단편 = `씬만으로 짧게`, 설정만 = no manuscript, yet step 2 always offers 장 › 에피소드 › 씬. Default: step shown unchanged; 단편 users can pick `직접 정하기` with one level. Confirm whether to skip or preset the step.
11. **Default rating in step 1.** `NewName` shows R-15 selected (the sample project); no rule names a default. Default `ED`: ALL.
12. **Meaning of `쓰는 중` on a 서재 row/card.** Shown only on 잿빛 in boards; not defined. Default `ED`: the project has an episode in status 쓰는 중. Alternative: the most recently edited project.
13. **App lock default and timing.** `OnbSafe` draws `앱 잠금` switched on; Settings draws it on; no source states the default or the exact re-lock timing (R19 §4 example implies every return from background). Default: off until chosen; lock on every return from background.
14. **`건너뛰기` vs `나중에` in first-run pages.** Both appear on the same pages without definitions. Default: `나중에` = next page without the action; `건너뛰기` = skip the remaining setup pages to the walkthrough.
15. **서재 · 숨기기 module tour has no board** (HelpReplay lists it with 3 steps). Copy needed.
16. **`백업 · 내보내기 · Google Drive · 파일` in the project menu.** Opens the export sheet; whether it also offers "back up this project now" to Drive is not drawn. Default: export sheet only.
17. **Backup-done notification wording.** R19 §3 gives `백업 끝 · 프로젝트 3 · 숨긴 프로젝트 1` (counts) and `백업 끝 · 잿빛 새벽의 등불 · 유리 정원 · R-18 프로젝트` (names). Default rule: counts when any hidden project is in the run, names otherwise. Restore/export done copy not given.
18. **Hiding a project when no 복구 암호 is set** from ⋯ › `프로젝트 숨기기`, the 프로젝트 설정 switch or step 4: only the rating sheet and step 1 (R-18) show the ◇ "자동 백업에서 빠짐" warning. Default: no extra sheet; 설정 › 보관 shows `숨긴 프로젝트 n · 백업 안 됨`.
19. **Device without a secure lock screen (no PIN).** Hidden projects and app lock rely on 지문 · PIN. Not addressed. Default `ED`: hiding and app lock require a device credential; prompt to set one (copy not in sources).
20. **Voice IDs in hidden scenes left out of an export** (R19 §7 defers to rule 12, which does not say). Default: not fixed (not written).
21. **괄호 지문 in game output** (R21 open note): whether JSON `text` keeps `(숨을 삼키며)` runs. Default `ED`: keep as a separate `stage direction` run so the engine may drop it.
22. **Narration / system text rows in 대사 표 CSV.** The CSV is "녹음 · 현지화용"; sources list only dialogue columns. Default: dialogue (+ choice dialogue), transition rows and 버림 rows only.
23. **Multi-project export entry point.** FMT §6: "여러 프로젝트를 한꺼번에 내보내면 파일이 여러 개 나온다"; no board shows how to pick several projects. Default: LIB-72 (P2) with a multi-select on 서재; copy not in sources.
24. **Changing the 복구 암호.** Old backup versions remain locked with the old password; not addressed. Default: keep old versions; restoring them asks for the password they were made with.
25. **Per-project restore.** FMT §6: "되살리기 화면에서는 프로젝트 단위로 고를 수 있게 된다"; board `Restore` only selects a whole backup. Default: add a project checklist after choosing a run (copy not in sources).
26. **Restore conflict label.** Board `Restore`: `같은 이름이 이미 있을 때`; FMT §6 and memory 2026-11-22: same project = same project key. Behaviour by key; the board label then reads wrong. Proposed (not confirmed copy): reuse the wording of `RHOpenFile`'s `같은 프로젝트가 이미 있음`. Needs a copy decision.
27. **Settings entry points not drawn.** No board shows how to reach 휴지통, 도움말 · 안내 다시 보기, 설정 · 라이프 레코더 or the 설정 › 보관 screen from 설정. Default `ED`: rows on 설정 (LIB-63).
28. **설정 · 숨기기 `가리는 곳` switches.** R19 §3 makes hidden projects always excluded from widgets/recents/search while locked and always masked in notifications, so turning these switches off has no defined meaning. Default: off never reveals hidden names; `검색 · 명령 결과` off = hidden content excluded even while open; the second switch is shown on and disabled until decided.
29. **R-18 non-hidden auto backups.** Export encrypts R-18 files by default (FMT §5), but auto-backup encryption is defined only for hidden projects. Default: R-18 not hidden → unencrypted backup.
30. **Library `+` while the hidden group is open.** `LibUnlocked` replaces `+` with `다시 잠그기`. Default: follow the board (new project via `+` after re-lock); confirm.
