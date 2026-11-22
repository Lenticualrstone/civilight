# Civilight · 기능 목록 (master feature checklist)

One row per feature of the area specs in `docs/spec/`. The spec file is the source for each feature's behaviour; `docs/spec/00-domain-model.md` is the single data model every feature uses, and where it changes an area spec the row's notes name the resolution (`C-xx`).

## Statuses

| Status | Meaning |
| --- | --- |
| `done` | Implemented as the spec says, with its rule test cases passing, on cover and inner layouts. |
| `partial` | Part of the behaviour works; the notes say what is missing. |
| `stub` | The entry point, data and screens exist, but the external part is replaced by a placeholder that says so (no fake results). |
| `not started` | Nothing built yet. |

**needsExternal.** Features that depend on something outside the app's own process (Google Drive, speech-to-text, an on-device or cloud language model, home-screen or lock-screen surfaces) ship as `stub` first: their data model, settings rows and in-app screens are built, and the external service is wired in later. Their notes start with `needsExternal`. Every other feature runs fully on the device.

**Priority.** P0 = core, the app is useless without it · P1 = planned for v1 · P2 = later or only if needed.

## Summary

| Area | Spec | Features | P0 | P1 | P2 | needsExternal |
| --- | --- | --- | --- | --- | --- | --- |
| SCR · 스크립트 (script editor) | `spec/10-script.md` | 69 | 15 | 53 | 1 | 0 |
| STO · 스토리 · 시점 · 때 · 점검 (story, lens, scene time, checks) | `spec/20-story-time.md` | 43 | 21 | 22 | 0 | 0 |
| CST · 인물 (cast) | `spec/30-cast.md` | 52 | 18 | 30 | 4 | 0 |
| WLD · 세계 (terms, places, factions, timeline, maps) | `spec/40-world.md` | 69 | 24 | 44 | 1 | 1 |
| CNV · 캔버스 · 페이지 · 카드와 이미지 · 펼친 화면 · 모션 | `spec/50-canvas-pages.md` | 47 | 2 | 43 | 2 | 0 |
| GAL · 갤러리 (image galleries of characters and projects) | `spec/55-gallery.md` | 25 | 0 | 22 | 3 | 0 |
| LIB · 서재 · 보호 · 파일 (library, protection, files, backup, settings) | `spec/60-library-security-files.md` | 72 | 35 | 36 | 1 | 6 |
| LRC · 라이프 레코더 (life recorder) | `spec/70-life-recorder.md` | 47 | 18 | 29 | 0 | 9 |
| DS · 디자인 시스템 (design system) | `spec/80-design-system.md` | 64 | 35 | 29 | 0 | 0 |
| **All** | | **488** | 168 | 308 | 12 | 16 |

## SCR · 스크립트 (script editor)

Spec: `docs/spec/10-script.md`

| id | feature | priority | status | notes |
| --- | --- | --- | --- | --- |
| SCR-01 | Script screen (cover layout) | P0 | not started |  |
| SCR-02 | Header position line and position label (`이 씬 · 1-7`) | P1 | not started |  |
| SCR-03 | Inner (unfolded) layout: manuscript + side panel | P1 | not started |  |
| SCR-04 | Block model and common rendering | P0 | not started | domain model C-05 link runs carry `linkKey`; C-13 trash fields |
| SCR-05 | 대사 (dialogue) block | P0 | not started |  |
| SCR-06 | 나레이션 (narration) block | P0 | not started |  |
| SCR-07 | 괄호 지문 (parenthetical stage directions) | P1 | not started | domain model C-05 `paren` run kind |
| SCR-08 | 연출 지시 (direction) block: 배경 · BGM · 효과음 · CG · 화면 효과 | P1 | not started | domain model C-11 cues point at `ScriptAsset` |
| SCR-09 | 선택지 (choice) block | P1 | not started | domain model C-21 rejections key choice options by `optionKey` |
| SCR-10 | 시스템 텍스트 (system text) block | P1 | not started |  |
| SCR-11 | 작가 메모 (author memo) block `⌁` | P1 | not started |  |
| SCR-12 | Block add sheet and slash shortcuts | P0 | not started |  |
| SCR-13 | Change a block's kind | P1 | not started |  |
| SCR-14 | Editing state, autosave and the fixed writing line | P0 | not started | domain model C-18 one `ProjectActivity` row for the last writing position |
| SCR-15 | Enter, split, backspace merge, scene-head barrier, `줄바꿈` | P0 | not started |  |
| SCR-16 | Keyboard toolbar | P0 | not started |  |
| SCR-17 | Undo | P0 | not started |  |
| SCR-18 | Speaker inheritance (`직전 화자`) | P0 | not started |  |
| SCR-19 | Speaker picker sheet (`화자`) | P0 | not started |  |
| SCR-20 | Speaker label, display name and `화자 없음` | P0 | not started |  |
| SCR-21 | Permanent key and per-episode block number (`#043`) | P0 | not started | domain model C-17 voice state per block; C-32 counter `StoryUnit.nextBlockNumber` in `story/index.json` (§4.1) |
| SCR-22 | `번호 다시 매기기` (renumber an episode) | P1 | not started |  |
| SCR-23 | Voice ID: `자동` → `고정`, `버린 ID` | P1 | not started | domain model C-17 `VoiceState.lastExport` replaces the app-only export ledger |
| SCR-24 | Voice ID format (project setting) | P1 | not started |  |
| SCR-25 | Optional script fields: `무대 위치`, `보이스 ID` | P1 | not started |  |
| SCR-26 | `표시` sheet (display layers) | P1 | not started |  |
| SCR-27 | Preview modes `쓰기` and `대사만` | P1 | not started |  |
| SCR-28 | Preview mode `낭독` | P1 | not started |  |
| SCR-29 | `호칭 힌트` (address-form hint) | P1 | not started |  |
| SCR-30 | Swipe-left tab buttons (`시간 · 참고 · 속성`) | P1 | not started |  |
| SCR-31 | Panel / sheet container and selection following | P1 | not started |  |
| SCR-32 | `시간` tab | P1 | not started |  |
| SCR-33 | `참고` tab | P1 | not started |  |
| SCR-34 | `속성` tab | P1 | not started |  |
| SCR-35 | `모듈` tab and `씬 메모` | P1 | not started |  |
| SCR-36 | Block select mode (`블록 고르기`) | P1 | not started |  |
| SCR-37 | Move blocks | P1 | not started |  |
| SCR-38 | Delete blocks | P0 | not started | domain model C-13 `deletedAt` + `trashEntryKey`; empty blocks never trashed |
| SCR-39 | Block trash and restore | P1 | not started | domain model C-13/C-14 trashed rows go to `trash/` only |
| SCR-40 | Copy, cut, paste | P1 | not started |  |
| SCR-41 | `◆` change-record sheet from a block (`변화 기록`) | P1 | not started | domain model C-04 unified `Anchor`; C-06 term reveal is a `ChangeRecord` |
| SCR-42 | Markers and point-anchored record lines | P1 | not started | domain model C-04 `unitStart` anchors at any level |
| SCR-43 | Anchor deleted: `근거 지워짐` | P1 | not started | domain model C-04 `anchorState` + `originalAnchor`; `unplaced` anchor |
| SCR-44 | Consequences of block operations (records · number · voice ID) | P1 | not started |  |
| SCR-45 | First reveal `◇` anchored to a block | P1 | not started | domain model C-06 term reveal = `ChangeRecord(termReveal)` |
| SCR-46 | `씬 머리` (scene head) | P0 | not started |  |
| SCR-47 | Scene-head ⋯ menu | P1 | not started |  |
| SCR-48 | `전환 문구` (transition text) | P1 | not started | domain model C-07 transition text stored as runs |
| SCR-49 | `/씬` routing and `다음 씬 만들기` | P1 | not started |  |
| SCR-50 | `씬 나누기` (split a scene) | P1 | not started |  |
| SCR-51 | `씬 합치기` (merge with the previous scene) | P1 | not started |  |
| SCR-52 | `씬 지우기` (delete a scene from the manuscript) | P1 | not started |  |
| SCR-53 | `앞에 새 씬` (new scene before this one) | P1 | not started |  |
| SCR-54 | Scene-move preview (`씬 옮기기`) | P1 | not started |  |
| SCR-55 | `@` links in block text | P1 | not started | domain model C-05 link target kinds per surface |
| SCR-56 | Term candidates and pre-reveal `▲` in the manuscript | P1 | not started | domain model C-21 `TermRejection` lives in the term |
| SCR-57 | Deleted speakers and deleted record subjects (rule 20) | P1 | not started |  |
| SCR-58 | Hidden items and hidden scenes (rule 19) | P1 | not started |  |
| SCR-59 | Memo blocks inserted from the Life Recorder (rule 23) | P1 | not started |  |
| SCR-60 | Read-only mode (example project) | P1 | not started |  |
| SCR-61 | Asset list (`에셋`) and asset picker | P1 | not started | domain model C-11 `ScriptAsset` with `fileHash` |
| SCR-62 | Check items produced from script data | P1 | not started | domain model C-16 acknowledgements in `checks.json` |
| SCR-63 | Game-engine JSON: content contract (provisional shape) | P1 | not started | domain model C-27 engine envelope from LIB-45, content from SCR-63 |
| SCR-64 | Dialogue CSV (`대사 표 CSV`): rows from script data | P1 | not started | domain model C-17 CSV `바뀜` from `VoiceState.lastExport` |
| SCR-65 | Script PDF (`대본 PDF`): content rules | P1 | not started |  |
| SCR-66 | Fold and unfold continuity | P1 | not started |  |
| SCR-67 | Script motion | P1 | not started |  |
| SCR-68 | Script commands and navigation in the command field | P1 | not started |  |
| SCR-69 | Tabletop posture layout | P2 | not started | P2 · later |

## STO · 스토리 · 시점 · 때 · 점검 (story, lens, scene time, checks)

Spec: `docs/spec/20-story-time.md`

| id | feature | priority | status | notes |
| --- | --- | --- | --- | --- |
| STO-01 | 프로젝트 홈 | P0 | not started |  |
| STO-02 | 이야기 단계 (customisable hierarchy) | P0 | not started | domain model C-08 `structure.levels` list (no `StructureLevel` rows) |
| STO-03 | 단위 표기 (unit codes and labels) | P0 | not started |  |
| STO-04 | 스토리 구조 화면 | P0 | not started |  |
| STO-05 | 단위 만들기 | P0 | not started |  |
| STO-06 | 단위 정보 · 상태 · 진행 · 글자 수 | P0 | not started | domain model C-19 one `DayBaseline` model for 오늘 쓴 글자 |
| STO-07 | 구조 메뉴 (⋯) | P0 | not started |  |
| STO-08 | 씬 옮기기 · 순서 바꾸기 | P1 | not started |  |
| STO-09 | 나누기 · 합치기의 장소와 때 (engine for SCR sheets) | P1 | not started |  |
| STO-10 | 씬 지우기 · 되살리기 (때 and 집필 선 effects) | P0 | not started | domain model C-13 liveness by containment |
| STO-11 | 숨김 · 지움 상태 (structure, home) | P1 | not started |  |
| STO-12 | 이야기 위치와 집필 선 (engine) | P0 | not started | domain model C-04 unified `Anchor`; §4 position order |
| STO-13 | 시점 렌즈 칩과 시트 | P0 | not started | domain model C-04 lens point `UnitEnd` |
| STO-14 | 예정 (records beyond the 집필 선) | P1 | not started |  |
| STO-15 | 렌즈에서 값 고치기 (two buttons, banner) | P1 | not started | domain model C-04 `여기서부터 바뀜` at 최신 = `unitEnd` of the 집필 선 scene |
| STO-16 | 렌즈와 스크립트 | P1 | not started |  |
| STO-17 | 때 표기 · 시간 설정 | P0 | not started | domain model C-09 `worldTime` embedded in project |
| STO-18 | 세계 달력으로 보는 때 | P1 | not started |  |
| STO-19 | 하루와 시간대 | P0 | not started |  |
| STO-20 | 씬 · 장소와 때 시트 | P0 | not started |  |
| STO-21 | 때 저장: 기준 + 간격 | P0 | not started | domain model §4 base + interval; JSON shape §7.4 |
| STO-22 | 때 계산과 표기 (engine) | P0 | not started |  |
| STO-23 | 뒤 씬 밀림 미리 보기 (cascade) | P0 | not started |  |
| STO-24 | 회상 | P1 | not started |  |
| STO-25 | 순서와 때가 다름 | P1 | not started |  |
| STO-26 | 등장 | P1 | not started |  |
| STO-27 | 전환 문구 row and inputs (for SCR-48) | P1 | not started |  |
| STO-28 | 이동 수단 고르기 (mode per pair) | P1 | not started | domain model C-16 travel overrides in `checks.json` |
| STO-29 | 이동 점검 계산 (engine) | P1 | not started |  |
| STO-30 | 이동 점검 카드 · 제안 칩 · 맞음 | P1 | not started |  |
| STO-31 | 씬 고르기 (bulk list) | P1 | not started |  |
| STO-32 | 장소 정하기 | P1 | not started |  |
| STO-33 | 때 정하기 | P1 | not started |  |
| STO-34 | 제안 받기 | P1 | not started |  |
| STO-35 | 표로 한꺼번에 (inner) | P1 | not started |  |
| STO-36 | 명령 칸 | P0 | not started |  |
| STO-37 | 검색 | P0 | not started |  |
| STO-38 | 명령 | P1 | not started |  |
| STO-39 | 점검 센터 화면 (shared with CNV-17) | P0 | not started | domain model C-24 one check-center shell; STO-40 registry wins |
| STO-40 | 점검 종류 (registry) | P0 | not started | domain model C-24 check type ids (§2.9) |
| STO-41 | 맞음 · 이대로 두기 (acknowledgements) | P0 | not started | domain model C-16 one `Acknowledgement` entity |
| STO-42 | 점검 설정 | P1 | not started |  |
| STO-43 | 펼친 화면 배치 (home, check) | P1 | not started |  |

## CST · 인물 (cast)

Spec: `docs/spec/30-cast.md`

| id | feature | priority | status | notes |
| --- | --- | --- | --- | --- |
| CST-01 | 인물 목록 (character list) | P0 | not started |  |
| CST-02 | 태그 필터 · 찾기 (tag filter and search) | P0 | not started |  |
| CST-03 | 항목 필터 · 정렬 (filter and sort by custom fields) | P1 | not started |  |
| CST-04 | 카드 보기 (card view) | P1 | not started |  |
| CST-05 | 인물 만들기 (create a character) | P0 | not started |  |
| CST-06 | 펼친 화면 배치 (unfolded layouts) | P1 | not started |  |
| CST-07 | 인물 기록 머리 · 문서형 (record header, document layout) | P0 | not started | domain model C-02 `key` not `id`; C-10 images via `ImageUse` |
| CST-08 | 탭 다섯 · 개요 본문 (five tabs, overview body) | P0 | not started |  |
| CST-09 | 항목 값 넣기 · 세 상태 (entering values, three states) | P0 | not started | domain model C-03 `order`; FieldValue shape §2.5 |
| CST-10 | 시점 렌즈 · 변화 기록 · 고치기 두 버튼 (lens, history, two edit buttons) | P0 | not started | domain model C-04 `unitEnd` anchor; later blocks of that scene do not see it |
| CST-11 | 표시 이름 (display-name override) | P0 | not started |  |
| CST-12 | 태그 (tags) | P0 | not started |  |
| CST-13 | 이미지 · 초상과 배경 (portrait and background) | P1 | not started | domain model C-10 portrait and background are `ImageUse` rows |
| CST-14 | 첫 등장 · 보이스 진행 (computed fixed fields) | P1 | not started |  |
| CST-15 | 말투 탭 (speech tab) | P1 | not started | domain model C-07 bodies are `RichDoc` |
| CST-16 | 게임 텍스트 · 보이스 대사 (in-game voice lines) | P1 | not started |  |
| CST-17 | 게임 텍스트 · 아카이브 문서 (archive documents) | P1 | not started | domain model C-07 `RichDoc`; unlock point is a `PointRef` |
| CST-18 | 비밀 탭 · 비밀 항목 (secret tab and secret fields) | P1 | not started |  |
| CST-19 | 인물 항목 설정 화면 (field settings screen) | P0 | not started | domain model C-23 `FieldDef.definitionHidden` (not item hiding) |
| CST-20 | 항목 종류 아홉 가지 (nine field types) | P0 | not started |  |
| CST-21 | + 항목 시트 (add-field sheet from settings) | P0 | not started |  |
| CST-22 | 인물 기록에서 + 항목 (add a field from a record) | P1 | not started |  |
| CST-23 | 항목 정의 바꾸기 (changing field definitions) | P1 | not started |  |
| CST-24 | 시작 묶음 (start bundles) | P1 | not started |  |
| CST-25 | 나이 계산 · 출생과 생일 (age, birth and birthday) | P1 | not started |  |
| CST-26 | 항목 내보내기 (field export) | P1 | not started |  |
| CST-27 | 항목 값 검색 (searching field values) | P1 | not started |  |
| CST-28 | 관계의 모양 · 관계 탭 (relation shape and the 관계 tab) | P0 | not started |  |
| CST-29 | 관계 만들기 · 고치기 (create and edit a relation) | P0 | not started |  |
| CST-30 | 원고에서 관계 변화 기록 (relation change from a block) | P0 | not started |  |
| CST-31 | 호칭 (forms of address) | P1 | not started |  |
| CST-32 | 관계 종류 설정 (relation kinds) | P1 | not started |  |
| CST-33 | 전체 관계도 (full relationship map) | P1 | not started |  |
| CST-34 | 캔버스 · 관계도 선 → 관계 (lines become relations) | P1 | not started |  |
| CST-35 | 관계 내보내기 (relation export) | P1 | not started |  |
| CST-36 | 세력 사이 관계 (faction-to-faction relations) | P2 | not started | P2 · later |
| CST-37 | 화자 고르기 연동 (speaker picker data) | P1 | not started |  |
| CST-38 | 참고 · 속성 패널 연동 (reference and properties panel data) | P1 | not started |  |
| CST-39 | 인물 숨기기 (hiding a character) | P1 | not started |  |
| CST-40 | 지우기 · 영향 시트 (delete with impact sheet) | P0 | not started | domain model C-13 owned data trashed by derivation, not copied marks |
| CST-41 | 지운 항목 참조 (references to a deleted item) | P0 | not started |  |
| CST-42 | 점검 센터 · 지운 항목 (check-center rows for deleted items) | P1 | not started |  |
| CST-43 | 되살리기 (restore) | P0 | not started | domain model C-13 restore by key; walk-throughs §5.4 |
| CST-44 | 영구 삭제 · 이름표 (permanent delete, name tag) | P1 | not started | domain model C-15 `NameTag` in `tombstones.json` |
| CST-45 | 합치기 (merge duplicates) | P1 | not started | domain model C-15 `MergeRecord` in `tombstones.json` |
| CST-46 | 같은 이름 (same-name notice) | P1 | not started |  |
| CST-47 | 장소 · 용어 · 세력 지우기 (deleting world items) | P1 | not started |  |
| CST-48 | 휴지통 줄 (trash rows) | P1 | not started | domain model C-13 reason and target live on `TrashEntry` |
| CST-49 | 라이프 레코더 연결 (recorder links on characters) | P1 | not started |  |
| CST-50 | 태그별 항목 묶음 (per-tag field sets) | P2 | not started | P2 · later |
| CST-51 | 인물 표 CSV (character table CSV) | P2 | not started | P2 · later |
| CST-52 | 게임 텍스트 보이스 ID (voice IDs for profile voice lines) | P2 | not started | P2 · later |

## WLD · 세계 (terms, places, factions, timeline, maps)

Spec: `docs/spec/40-world.md`

| id | feature | priority | status | notes |
| --- | --- | --- | --- | --- |
| WLD-01 | 세계 화면 틀 | P0 | not started |  |
| WLD-02 | 용어 목록 | P0 | not started |  |
| WLD-03 | 장소 목록 | P0 | not started |  |
| WLD-04 | 세력 목록 | P0 | not started |  |
| WLD-05 | 목록 \| 카드 보기 | P1 | not started |  |
| WLD-06 | 카드 공통 · 고정 칸과 프로젝트 항목 | P0 | not started | domain model C-07 descriptions are `RichDoc`; C-10 card image via `ImageUse` |
| WLD-07 | 카드 이미지 | P1 | not started |  |
| WLD-08 | 용어 카드 | P0 | not started | domain model C-21 `rejections` keyed by `optionKey` |
| WLD-09 | 장소 카드 | P0 | not started |  |
| WLD-10 | 세력 카드 | P0 | not started |  |
| WLD-11 | 항목 만들기 · 이름 바꾸기 | P0 | not started |  |
| WLD-12 | 시점 렌즈와 세계 값의 변화 | P0 | not started |  |
| WLD-13 | 숨기기 (용어 · 장소 · 세력) | P1 | not started | domain model C-22 timeline events have no own hidden flag |
| WLD-14 | 지우기 · 되살리기 · 영구 삭제 (세계 항목) | P1 | not started | domain model C-13 pins and regions follow the place by derivation |
| WLD-15 | 합치기 (세계 항목) | P1 | not started |  |
| WLD-16 | 별칭 | P1 | not started |  |
| WLD-17 | `@` 연결 (용어 대상) | P0 | not started | domain model C-05 `linkKey` on every link run |
| WLD-18 | 첫 공개 기록 | P0 | not started | domain model C-06 first reveal is a `ChangeRecord(termReveal)`; C-04 anchors |
| WLD-19 | 쓰인 곳 판정 · 세 무리 | P0 | not started |  |
| WLD-20 | ▲ 공개 전 사용 | P0 | not started |  |
| WLD-21 | 글자 후보 찾기 (matcher) | P1 | not started |  |
| WLD-22 | 후보 확인 · 잇기 · 아님 | P1 | not started | domain model C-21 `아님` stays in `world.json` |
| WLD-23 | 모두 잇기 · 모두 아님 | P1 | not started |  |
| WLD-24 | 맞음 (의도한 공개 전 사용) | P1 | not started | domain model C-16 `맞음` = `Acknowledgement(term.beforeReveal, [linkKey])` |
| WLD-25 | 글자 찾기 켬 · 끔 | P1 | not started |  |
| WLD-26 | 후보 색인과 다시 훑는 때 | P1 | not started |  |
| WLD-27 | 원고 표시 계약 (연결 · 후보 · 공개 전 연결) | P1 | not started |  |
| WLD-28 | 점검 센터 ‘용어 공개’ 줄 | P0 | not started |  |
| WLD-29 | 세계 시간 설정 | P0 | not started | domain model C-09 `worldTime` embedded in project |
| WLD-30 | 세계 달력 | P1 | not started |  |
| WLD-31 | 연표 화면 | P0 | not started | domain model C-22 events: no stored hidden flag |
| WLD-32 | 사건 만들기 · 고치기 | P0 | not started |  |
| WLD-33 | 사건 상세 | P1 | not started |  |
| WLD-34 | 연표 보기 셋 | P1 | not started |  |
| WLD-35 | 그때 나이 | P1 | not started |  |
| WLD-36 | 지도 목록 · 새 지도 | P0 | not started |  |
| WLD-37 | 지도 보기 | P0 | not started |  |
| WLD-38 | 바탕 · 이미지 가져오기 · 큰 이미지 | P0 | not started |  |
| WLD-39 | 바탕 · 스케치 촬영 | P1 | not started |  |
| WLD-40 | 편집 모드 · 도구 줄 | P0 | not started |  |
| WLD-41 | 핀 | P0 | not started |  |
| WLD-42 | 영역 | P1 | not started |  |
| WLD-43 | 경로 | P1 | not started |  |
| WLD-44 | 라벨 | P1 | not started |  |
| WLD-45 | 레이어 | P1 | not started |  |
| WLD-46 | 축척 맞추기 | P1 | not started |  |
| WLD-47 | 이동 수단 | P1 | not started |  |
| WLD-48 | 하위 지도 (중첩) | P1 | not started | domain model C-26 child map link stored once (`WorldMap.parent`) |
| WLD-49 | 시점별 공개 | P1 | not started | domain model §4.6 `PointRef` reveal points |
| WLD-50 | 노드 지도 | P1 | not started |  |
| WLD-51 | 지도 내보내기 4K · 8K | P1 | not started |  |
| WLD-52 | 장소 고르기 (다른 영역에 주는 것) | P0 | not started | domain model C-25 `PlaceRef {kind, key}` for scene places |
| WLD-53 | 생성기 | P1 | not started |  |
| WLD-54 | 지형 만들기 흐름 | P1 | not started |  |
| WLD-55 | 단계 조작 · 시드 · 고정 · 다시 | P1 | not started |  |
| WLD-56 | 시작 뒤 단계 다시 만들기 | P1 | not started |  |
| WLD-57 | 그림 · 양식 바꾸기 | P1 | not started |  |
| WLD-58 | 육각 편집 | P1 | not started |  |
| WLD-59 | 다각형 편집 | P1 | not started |  |
| WLD-60 | 상세 지형 편집 | P1 | not started |  |
| WLD-61 | 글로 설명 → 조건 → 지형 | P1 | not started | needsExternal: on-device language model, optional cloud → stub first |
| WLD-62 | 인물 동선 | P1 | not started |  |
| WLD-63 | 이곳의 시간 | P1 | not started |  |
| WLD-64 | 연표 + 지도 | P1 | not started |  |
| WLD-65 | 세력 영역의 시점별 모양 | P1 | not started |  |
| WLD-66 | 거리 계산 (동선 · 이동 점검에 넘김) | P1 | not started |  |
| WLD-67 | 설정 · 지도 · 이미지 · 연표 (앱 설정) | P1 | not started |  |
| WLD-68 | 지도 · 카드 모션 | P1 | not started |  |
| WLD-69 | 세력 사이 관계 | P2 | not started | P2 · later |

## CNV · 캔버스 · 페이지 · 카드와 이미지 · 펼친 화면 · 모션

Spec: `docs/spec/50-canvas-pages.md`

| id | feature | priority | status | notes |
| --- | --- | --- | --- | --- |
| CNV-01 | 그래프 엔진 (shared graph engine) | P1 | not started |  |
| CNV-02 | 캔버스 문서 (canvases of a project) | P1 | not started |  |
| CNV-03 | 도구 줄 (canvas tool bar) | P1 | not started |  |
| CNV-04 | 항목 노드 (project-item nodes) | P1 | not started |  |
| CNV-05 | 메모 노드 (memo nodes) | P1 | not started |  |
| CNV-06 | 묶음 (group) | P1 | not started |  |
| CNV-07 | 선 긋기 (plain lines) | P1 | not started |  |
| CNV-08 | 선 라벨 · 관계로 만들기 (line label sheet and the relation hand-off) | P1 | not started |  |
| CNV-09 | 관계 선과 시점 렌즈 (relation-backed lines under the lens) | P1 | not started |  |
| CNV-10 | 노드 편집 (node edit surface) | P1 | not started |  |
| CNV-11 | 숨긴 항목 · 지운 항목이 캔버스에 있을 때 (hidden and deleted items on canvas) | P1 | not started |  |
| CNV-12 | 캔버스 저장 (canvas persistence) | P1 | not started | domain model C-01/C-02 bookkeeping names; JSON conventions §1.5 |
| CNV-13 | 페이지 (free document: view and edit) | P1 | not started | domain model C-07 page body is `RichDoc` |
| CNV-14 | 페이지 목록과 들어가는 곳 (page list and entry points) | P1 | not started |  |
| CNV-15 | 레코더 기록으로 만든 페이지 (page made from a recorder record) | P1 | not started |  |
| CNV-16 | 페이지에 걸리는 숨기기 · 지우기 (hiding and deleting as seen in pages) | P1 | not started | domain model C-20 pages go to trash (`PAGE` entry, filter 전체) |
| CNV-17 | 점검 센터 (check-center shell) | P1 | not started | domain model C-16 `Acknowledgement`; C-24 STO-40 registry wins |
| CNV-18 | 설정 (app settings root: row map) | P1 | not started |  |
| CNV-19 | 설정 · 지도 · 이미지 · 연표 | P1 | not started |  |
| CNV-20 | 프로젝트 설정 · 도움말 · 레코더 설정 (row maps) | P1 | not started |  |
| CNV-21 | 목록 \| 카드 전환 (ViewToggle mechanics) | P1 | not started |  |
| CNV-22 | 가로 카드 줄 (snapping card rows) | P1 | not started |  |
| CNV-23 | 카드 부품 (card components) | P1 | not started |  |
| CNV-24 | 이미지 없음 (initial-letter placeholder) | P1 | not started |  |
| CNV-25 | 이미지 규격 (slots and fixed ratios) | P1 | not started |  |
| CNV-26 | 기본 이미지 8종 (preset images) | P1 | not started |  |
| CNV-27 | 이미지 고르기 (image picker sheet) | P1 | not started |  |
| CNV-28 | 자르기 (crop editor) | P1 | not started |  |
| CNV-29 | 이미지 저장 (originals, crops, reuse) | P1 | not started | domain model C-10/C-11 `Blob` + `ImageUse` + `ScriptAsset` in `assets/index.json` |
| CNV-30 | 홀로그램 효과 (silver holo on images) | P1 | not started |  |
| CNV-31 | 홀로 실험 (holo on components, "rare moment" screen) | P2 | not started | P2 · later |
| CNV-32 | 창 크기와 화면 틀 (window-size classes, adaptive frame) | P0 | not started |  |
| CNV-33 | 목록 모듈 펼친 화면 배치 (two-pane / single column + setting) | P1 | not started |  |
| CNV-34 | 접기 · 펼치기 (fold/unfold transitions) | P1 | not started |  |
| CNV-35 | 상태 저장과 복원 (state save/restore across fold and process death) | P0 | not started |  |
| CNV-36 | 반쯤 접기 (half-fold postures) | P2 | not started | P2 · later |
| CNV-37 | 모션 토큰 · 등록표 · 줄이기 (motion tokens, registry, reduce) | P1 | not started |  |
| CNV-38 | 스플래시 (splash) | P1 | not started |  |
| CNV-39 | 블록 추가 모션 (block add) | P1 | not started |  |
| CNV-40 | 스와이프 버튼 줄 모션 (swipe button row) | P1 | not started |  |
| CNV-41 | 시트 모션 (sheets) | P1 | not started |  |
| CNV-42 | 시점 렌즈 값 교체 (lens value swap) | P1 | not started |  |
| CNV-43 | 숨김 풀기 모션 (unhide) | P1 | not started |  |
| CNV-44 | 검색 하이라이트 모션 (search highlight) | P1 | not started |  |
| CNV-45 | 카드 · 이미지 모션 (list ↔ card, carousel, picker → crop, holo on) | P1 | not started |  |
| CNV-46 | 움직임 · 투명도 (motion & transparency preferences, glass fallback) | P1 | not started | domain model C-12 one `AppSettings` record |
| CNV-47 | 펼친 화면 비율 (two-pane ratio handle: drag settles to 1:2 · 1:1 · 2:1, popover with 기본, per module) | P1 | partial | data done: `AppSettings.paneSplit` · `PaneSplit` · `SettingsStore.setPaneSplit`; core:ui `PaneSplitRules` (tested), `SplitPanes`, `rememberPaneSplitState` + `PaneSplitHandle`, wired into every `목록 + 상세` frame (2026-11-23); script side panel and gallery list + detail: lanes; not seen on the emulator |

## GAL · 갤러리 (image galleries of characters and projects)

Spec: `docs/spec/55-gallery.md`

| id | feature | priority | status | notes |
| --- | --- | --- | --- | --- |
| GAL-01 | 인물 개요의 갤러리 칸 (gallery section in the profile) | P1 | not started | domain model §12 `GalleryItem` |
| GAL-02 | 인물 카드의 갤러리 수 (count chip on character cards) | P2 | not started |  |
| GAL-03 | 프로젝트 갤러리 들어가는 곳 (project home `작업` entry) | P1 | not started |  |
| GAL-04 | 갤러리 화면 틀 (header, 3-way view toggle, filters, order line, search) | P1 | not started | `ViewMode.GRID` (domain model §12.4); route `Gallery` and sheets `GalleryAddSheet` · `GalleryInfoSheet` · `GalleryMenuSheet` in core:navigation, shared `GalleryCard` in core:ui, command-bar index `domain:story` `GallerySearch` (2026-11-23) |
| GAL-05 | 목록 (list view) | P1 | not started |  |
| GAL-06 | 격자 (grid view) | P1 | not started |  |
| GAL-07 | 카드 (snapping card row) | P1 | not started |  |
| GAL-08 | 고른 카드와 설명 판 (picking a card and showing its text) | P1 | not started | hint only in `카드` while nothing is picked |
| GAL-09 | 보기 (full-screen viewer, `이전 · 설명 · 다음`) | P1 | not started |  |
| GAL-10 | 프로젝트 갤러리 (project gallery and grouping by owner) | P1 | not started |  |
| GAL-11 | 이미지 넣기 (add several images) | P1 | not started |  |
| GAL-12 | 자르기 (gallery slot crop) | P1 | not started |  |
| GAL-13 | 이미지 정보 (title, description, tags, scene link) | P1 | not started | domain model §12 `GalleryItem.caption`, `tagKeys`, `sceneKey` |
| GAL-14 | 메뉴 · 초상으로 · 배경으로 · 표지로 (item menu and use as the main image) | P1 | not started | derived from `ImageUse`, no stored flag (domain model §12) |
| GAL-15 | 순서 (order buttons, `순서 바꾸기`, `옮기기`) | P1 | partial | data done: `GalleryActions.move` · `moveAfter` (visible items, one undo step); screens not built |
| GAL-16 | 다른 곳에도 넣기 (same original in several galleries) | P2 | not started | copies every field incl. `hidden` |
| GAL-17 | 홀로그램 효과 (holo per item) | P1 | not started |  |
| GAL-18 | 여러 장 고르기 (act on several) | P2 | not started |  |
| GAL-19 | 숨기기 · 등급 (hiding and rating) | P1 | partial | data done: `HideableKind.GALLERY_IMAGE`, `ItemSurfaces.gallery` · `projectGallery` · `searchScope`; screens not built |
| GAL-20 | 지우기 · 되살리기 (delete, restore, owner delete, purge, merge) | P1 | partial | data done: trash kind `galleryItem`, `OwnedKind.GALLERY_ITEM`, `MergeOps`, `GalleryActions.delete` · `restore` · `purge`; menu and toast UI not built |
| GAL-21 | 저장과 용량 (storage and size) | P1 | partial | data done: one blob reference function `BlobRefs`, `GalleryActions.galleryBytes`; size labels not built |
| GAL-22 | 내보내기 (export) | P1 | partial | data done: feature id `gallery`, format 1.0 (no 1.1), `gallery/` files and originals; export-sheet size row (Q13) not built |
| GAL-23 | 펼친 화면 (inner layouts) | P1 | not started |  |
| GAL-24 | 모션 (motion) | P1 | not started |  |
| GAL-25 | 빈 갤러리 · 많은 이미지 (empty and large) | P1 | not started |  |

## LIB · 서재 · 보호 · 파일 (library, protection, files, backup, settings)

Spec: `docs/spec/60-library-security-files.md`

| id | feature | priority | status | notes |
| --- | --- | --- | --- | --- |
| LIB-01 | 서재 목록 (커버 화면) | P0 | not started |  |
| LIB-02 | 프로젝트 ⋯ 메뉴 | P0 | not started |  |
| LIB-03 | 프로젝트 지우기 확인 | P0 | not started |  |
| LIB-04 | 목록 \| 카드 보기 (서재) | P1 | not started |  |
| LIB-05 | 최근 연 파일 | P1 | not started |  |
| LIB-06 | 펼친 화면 서재 | P1 | not started |  |
| LIB-07 | 모든 프로젝트에서 찾기 (범위 규칙) | P1 | not started |  |
| LIB-08 | 스플래시와 시작 흐름 | P1 | not started |  |
| LIB-09 | 1/4 이름과 형식 | P0 | not started |  |
| LIB-10 | 2/4 구조 | P0 | not started |  |
| LIB-11 | 3/4 시간 | P0 | not started |  |
| LIB-12 | 4/4 시작 재료 | P0 | not started | domain model C-08 structure list; C-09 world time |
| LIB-13 | 만든 직후 · 시작해 볼 것 | P1 | not started |  |
| LIB-14 | 새 프로젝트 펼친 화면 | P1 | not started |  |
| LIB-15 | 첫 실행 설정 | P1 | not started | needsExternal: Drive sign-in, widget → stub first |
| LIB-16 | 워크스루 3장 | P1 | not started |  |
| LIB-17 | 예시 프로젝트 (읽기 전용) | P1 | not started |  |
| LIB-18 | 온보딩 끝내기 · 예시 삭제 | P1 | not started |  |
| LIB-19 | 모듈 안내 (툴팁 투어) | P1 | not started |  |
| LIB-20 | 기능 첫 사용 한 줄 안내 | P1 | not started |  |
| LIB-21 | 도움말 · 안내 다시 보기 | P1 | not started |  |
| LIB-22 | 수위 등급 (값 · 배지 · 표기) | P0 | not started |  |
| LIB-23 | 보호 값과 등급별 기본값 (설정 · 수위 등급) | P0 | not started |  |
| LIB-24 | 등급 바꾸기 · `바뀌는 것` 시트 | P0 | not started |  |
| LIB-25 | 프로젝트 설정 › 등급 · 숨기기 | P0 | not started |  |
| LIB-26 | 화면 캡처 가리기 | P0 | not started |  |
| LIB-27 | 이름 가리기 | P0 | not started |  |
| LIB-28 | 라이프 레코더 연결 가리기 | P1 | not started |  |
| LIB-29 | 시스템 최근 앱 미리 보기와 카드 이름 | P0 | not started |  |
| LIB-30 | 알림 문구 가림 | P1 | not started |  |
| LIB-31 | 프로젝트 숨기기 · 숨김 풀기 | P0 | not started |  |
| LIB-32 | 숨긴 프로젝트 열기 (지문 · PIN) | P0 | not started |  |
| LIB-33 | 다시 잠그는 때 · 다시 잠그기 | P0 | not started |  |
| LIB-34 | 잠금 화면 (흐린 원고 · 마크만) | P0 | not started |  |
| LIB-35 | 앱 잠금 | P1 | not started |  |
| LIB-36 | 항목 숨기기 (공통 규칙) | P1 | not started |  |
| LIB-37 | 인증 실패와 잠김 | P0 | not started |  |
| LIB-38 | 휴지통 | P0 | not started | domain model C-20 trash kinds `PAGE · MAP · CANVAS · EVENT` added |
| LIB-39 | 영구 삭제 · 비우기 · 30일 자동 삭제 · 이름표 | P0 | not started | domain model C-15 `NameTag` shape |
| LIB-40 | 되돌리기 알림 (Toast host) | P0 | not started |  |
| LIB-41 | 내보내기 시트 | P0 | not started |  |
| LIB-42 | 숨긴 것과 내보내기 | P1 | not started |  |
| LIB-43 | 대본 PDF | P1 | not started |  |
| LIB-44 | 대사 표 CSV | P1 | not started |  |
| LIB-45 | 게임 엔진 JSON | P0 | not started |  |
| LIB-46 | 보이스 ID 고정 (내보낼 때) | P1 | not started | domain model C-17 voice state per block |
| LIB-47 | 프로젝트 파일 내보내기 (.civilight) | P0 | not started | domain model §7 package layout |
| LIB-48 | 레코더 라이브러리 파일 (.civilight-recorder) | P1 | not started |  |
| LIB-72 | 여러 프로젝트 한꺼번에 내보내기 | P2 | not started | P2 · later |
| LIB-49 | 묶음 구조와 적는 법 | P0 | not started | domain model §7 package layout; C-14 `trash/`; C-15/C-16 extra files |
| LIB-50 | 버전 규칙 | P0 | not started | domain model §8 versioning; feature ids §7.3 |
| LIB-51 | 암호화 | P0 | not started |  |
| LIB-52 | 파일에서 열기 | P0 | not started | domain model C-28 project key scope (§6) |
| LIB-53 | 새 버전 파일 안내 | P0 | not started |  |
| LIB-54 | 암호 파일 열기 | P0 | not started |  |
| LIB-55 | 깨진 파일 부분 가져오기 | P1 | not started | domain model C-32 block counter lives in `story/index.json`, so a damaged episode file never causes reissued numbers |
| LIB-56 | 양식 회귀 예시 파일 | P1 | not started |  |
| LIB-57 | Google Drive 자동 백업 | P1 | not started | needsExternal: Google Drive → stub first |
| LIB-58 | 복구 암호 | P1 | not started |  |
| LIB-59 | 백업에서 되살리기 | P1 | not started | needsExternal: Google Drive → stub first |
| LIB-60 | 되살리는 중 | P1 | not started | needsExternal: Google Drive → stub first |
| LIB-61 | 백업 실패 · Drive 로그인 만료 | P1 | not started | needsExternal: Google Drive → stub first |
| LIB-62 | 설정 › 보관 | P1 | not started | needsExternal: Google Drive → stub first |
| LIB-63 | 설정 (앱) | P0 | not started | domain model C-12 one `AppSettings` record |
| LIB-64 | 설정 · 숨기기 | P0 | not started |  |
| LIB-65 | 화면 · 글자 크기 | P0 | not started |  |
| LIB-66 | 설정 · 펼친 화면 배치 | P1 | not started |  |
| LIB-67 | 설정 · 움직임 · 투명도 | P1 | not started |  |
| LIB-68 | 설정 · 지도 · 이미지 · 연표 (container) | P1 | not started |  |
| LIB-69 | 프로젝트 설정 (container) | P0 | not started |  |
| LIB-70 | 빈 상태 | P1 | not started |  |
| LIB-71 | 오류 · 불러오는 중 공통 꼴 | P1 | not started |  |

## LRC · 라이프 레코더 (life recorder)

Spec: `docs/spec/70-life-recorder.md`

| id | feature | priority | status | notes |
| --- | --- | --- | --- | --- |
| LRC-01 | 명령 칸 마이크 (command-bar mic) | P0 | not started |  |
| LRC-02 | 홈 화면 위젯 (home-screen widget) | P1 | not started | needsExternal: home-screen widget → stub first |
| LRC-03 | 빠른 설정 타일 (quick-settings tile) | P1 | not started | needsExternal: quick-settings tile → stub first |
| LRC-04 | 커버 화면 위젯 (cover-screen widget) and unfold continuation | P1 | not started | needsExternal: cover-screen widget host → stub first |
| LRC-05 | 잠금 화면 녹음 (lock-screen recording) | P1 | not started | needsExternal: lock-screen activity → stub first |
| LRC-06 | 마이크 권한과 `마이크 꺼짐` (mic permission and error) | P0 | not started |  |
| LRC-07 | 녹음 중 (recording screen) | P0 | not started | needsExternal: speech-to-text → stub first |
| LRC-08 | 침묵 자동 멈춤 없음 (no auto-stop) | P0 | not started | needsExternal: continuous recognition → stub first |
| LRC-09 | 녹음 계속 (background continuity, interruptions) | P1 | not started | needsExternal: microphone foreground service → stub first |
| LRC-10 | 빛 띠 (voice glow) | P1 | not started |  |
| LRC-11 | 받아쓰기 · 기기 안 (on-device transcription, default) | P0 | not started | needsExternal: on-device speech model → stub first |
| LRC-12 | 받아쓰기 · 클라우드 (cloud transcription, optional) | P1 | not started | needsExternal: cloud speech provider → stub first |
| LRC-13 | 정리 중 (processing screen) | P0 | not started |  |
| LRC-14 | 원본 음성 보관과 재생 (original audio) | P1 | not started |  |
| LRC-15 | 글 메모 (text memo sheet) | P0 | not started |  |
| LRC-16 | 말 ↔ 글 바꾸기 (switch voice ↔ text) | P1 | not started |  |
| LRC-17 | 글 메모의 `@` (links inside text) | P0 | not started | domain model C-05 recorder link runs carry `projectKey` |
| LRC-18 | 연결 모델 (link model) | P0 | not started | domain model C-29 `RecorderLink.targetKind = unit` for chapter · episode · scene |
| LRC-19 | 보던 곳 연결 (pre-filled link from the entry context) | P0 | not started |  |
| LRC-20 | 연결 고르기 (link picker: `+ 연결` and chip tap) | P0 | not started |  |
| LRC-21 | 연결 칩 표시 (chip rendering rules) | P0 | not started |  |
| LRC-22 | 연결 가림 (masking: R-18, hidden project, hidden item) | P1 | not started |  |
| LRC-23 | 연결 대상이 지워졌을 때 (deleted, moved and example targets) | P1 | not started | domain model C-15 recorder `lastLabel` (not `nameTag`) |
| LRC-24 | 대상 쪽에서 보이는 줄 (`레코더 · 정리 안 됨 n ›`) | P1 | not started |  |
| LRC-25 | 기록함 목록 (inbox list) | P0 | not started |  |
| LRC-26 | 상태 (`정리 안 됨 · 넣음 · 끝냄`) | P0 | not started |  |
| LRC-27 | 필터와 `프로젝트 ▾` (filters) | P0 | not started |  |
| LRC-28 | 기록 화면 (record screen) | P1 | not started |  |
| LRC-29 | 기록 ⋯ 메뉴 (record menu) | P0 | not started |  |
| LRC-30 | 빈 기록함 (empty inbox) | P1 | not started |  |
| LRC-31 | 펼친 화면 배치 (unfolded layouts) | P1 | not started |  |
| LRC-32 | 넣기 공통 규칙 (common rules for every insertion) | P0 | not started | domain model C-05 copies re-mint `linkKey`; C-07 card paragraphs are keyed `DocBlock`s |
| LRC-33 | 원고에 넣기 · 위치 시트 (insert into the manuscript) | P0 | not started |  |
| LRC-34 | 여기 넣기 (insert at the recording's start position) | P1 | not started |  |
| LRC-35 | 카드에 넣기 (insert into a card) | P1 | not started |  |
| LRC-36 | 페이지로 만들기 (make a page) | P1 | not started |  |
| LRC-37 | ⋯ › 메모 칸에 넣기 (append to a memo field) | P1 | not started |  |
| LRC-38 | ⋯ › 인물로 만들기 (make a character) | P1 | not started |  |
| LRC-39 | 넣은 뒤 (after inserting: edits, deleted copies) | P1 | not started |  |
| LRC-40 | 기록 숨기기 · 잠긴 묶음 (hide records, locked group) | P1 | not started |  |
| LRC-41 | 지문으로 열기 · 다시 잠그기 · 숨김 풀기 (open, re-lock, unhide) | P1 | not started |  |
| LRC-42 | 숨기기 줄 기본값 (hide by default for hidden / R-18 targets) | P1 | not started |  |
| LRC-43 | 기록 지우기 (delete records) | P1 | not started |  |
| LRC-44 | 설정 · 라이프 레코더 (settings screen) | P1 | not started |  |
| LRC-45 | 내보내기와 레코더 기록 (export contents) | P1 | not started | domain model §7.10 recorder slice in `recorder.json` |
| LRC-46 | 레코더 라이브러리 파일 · 백업 (`.civilight-recorder`) | P1 | not started | domain model §7.11 `.civilight-recorder` |
| LRC-47 | 다른 화면의 레코더 고리 (hooks used by other areas) | P1 | not started |  |

## DS · 디자인 시스템 (design system)

Spec: `docs/spec/80-design-system.md`

| id | feature | priority | status | notes |
| --- | --- | --- | --- | --- |
| DS-01 | 테마 (theme modes and token provider) | P0 | not started |  |
| DS-02 | 색 토큰 (theme colour tokens) | P0 | not started |  |
| DS-03 | 고정 색 (theme-independent colours) | P1 | not started |  |
| DS-04 | 대비 규칙 (contrast rules) | P0 | not started |  |
| DS-05 | 글꼴 파일과 라이선스 (font files and licences) | P0 | not started | domain model C-30 Pretendard everywhere |
| DS-06 | 글자 역할 (type roles) | P0 | not started |  |
| DS-07 | 글자 크기 적용 (text-size scaling) | P0 | not started |  |
| DS-08 | 한글 조판 (Korean typesetting rules) | P1 | not started |  |
| DS-09 | 간격과 배치 (spacing and layout metrics) | P0 | not started |  |
| DS-10 | 모서리 · 그림자 · 흐림 · 크기 (shape, elevation, blur and size tokens) | P0 | not started |  |
| DS-11 | 누르는 범위 (touch targets ≥ 44 dp) | P0 | not started |  |
| DS-12 | 아이콘 (icon set) | P0 | not started |  |
| DS-13 | 원고 줄 표식 (app mark, lock mark, launcher icon) | P1 | not started |  |
| DS-14 | 모션 토큰 (motion tokens) | P1 | not started |  |
| DS-15 | 햅틱 (haptic tokens) | P1 | not started |  |
| DS-16 | 표식 ◆ ◇ ▲ (change, first reveal, warning) | P0 | not started |  |
| DS-17 | 연출 기호와 기호 목록 (▸ ♪ ⌁ and the allowed symbol set) | P0 | not started |  |
| DS-18 | 점선 = 지운 것에 걸림 (dotted = attached to something deleted) | P1 | not started |  |
| DS-19 | 글 속 표시 (inline decorations: link, candidate, hidden, parenthetical, caret) | P0 | not started |  |
| DS-20 | 등급 배지 (rating badge) | P1 | not started |  |
| DS-21 | 유리 막대 (GlassBars: top title bar and bottom command bar) | P0 | not started |  |
| DS-22 | 유리 재질 (glass material and its fallbacks) | P0 | not started |  |
| DS-23 | 시트 (bottom sheet) | P0 | not started |  |
| DS-24 | 옆 패널 (side panel, inner) | P0 | not started |  |
| DS-25 | 대화상자 (dialogs) | P0 | not started |  |
| DS-26 | 작은 창 (popover) | P1 | not started |  |
| DS-27 | 알림 (Toast) | P0 | not started |  |
| DS-28 | 띠와 그림 가장자리 (banners and image fade) | P1 | not started |  |
| DS-29 | 명령 칸 (CommandBar) | P0 | not started |  |
| DS-30 | 버튼 (Buttons) | P0 | not started |  |
| DS-31 | 아이콘 버튼 · 알약 · 키보드 도구 (icon buttons, pills, keyboard tools) | P0 | not started |  |
| DS-32 | 토글 · 라디오 · 체크 (Controls) | P0 | not started |  |
| DS-33 | 분할 버튼 (segmented controls) | P0 | not started |  |
| DS-34 | 슬라이더 (slider) | P1 | not started |  |
| DS-35 | 입력칸 (text fields) | P0 | not started |  |
| DS-36 | 칩 (Tag: filter chips, removable chips, small chips) | P0 | not started |  |
| DS-37 | 탭 (Tabs) | P0 | not started |  |
| DS-38 | 시점 렌즈 칩 (LensChip) | P0 | not started |  |
| DS-39 | 목록 \| 카드 전환 (ViewToggle) | P1 | not started |  |
| DS-40 | 목록 행 · 항목 표 · 섹션 머리 (ListRow, field table, section label) | P0 | not started |  |
| DS-41 | 잠긴 줄 (LockRow) | P1 | not started |  |
| DS-42 | 원고 블록 (ScriptBlock typesetting) | P0 | not started |  |
| DS-43 | 씬 머리 (scene head) | P0 | not started |  |
| DS-44 | 스와이프 버튼 줄 (SwipeTabs) | P1 | not started |  |
| DS-45 | 검색 결과 줄 (SearchHit) | P0 | not started |  |
| DS-46 | 아바타 · 첫 글자 (avatar and initials) | P1 | not started |  |
| DS-47 | 빈 화면 · 오류 블록 (empty and error states) | P0 | not started |  |
| DS-48 | 진행 표시 (progress) | P1 | not started |  |
| DS-49 | 안내 툴팁 (tooltip tour card and dim) | P1 | not started |  |
| DS-50 | 노드 카드 (canvas / relationship-map node) | P1 | not started |  |
| DS-51 | 카드 (cards over images) | P1 | not started |  |
| DS-52 | 이미지 칸 (ImageTile) | P1 | not started |  |
| DS-53 | 자르기 틀 (CropFrame) | P1 | not started |  |
| DS-54 | 은빛 홀로 재질 (holo material) | P1 | not started |  |
| DS-55 | 지도 도구 줄과 생성 조각 (MapToolbar and generator controls) | P1 | not started |  |
| DS-56 | 지도 표시 (MapMarks) | P1 | not started |  |
| DS-57 | 지도 그림 색 (MapStyle colours) | P1 | not started |  |
| DS-58 | 때 고르기 (WhenPicker) | P1 | not started |  |
| DS-59 | 조건 줄 (ConditionRow) | P1 | not started |  |
| DS-60 | 이동 막대 (TravelBar) | P1 | not started |  |
| DS-61 | 레코더 빛 띠 (recorder glow band) | P1 | not started |  |
| DS-62 | 문구 규칙 (UI copy rules and lint) | P0 | not started |  |
| DS-63 | 낱말 (vocabulary: one word per meaning) | P0 | not started |  |
| DS-64 | 접근성 의미 (accessibility semantics) | P1 | not started |  |

## Coverage

Checked 2026-11-22 against three sources: every board (the board index, 183 boards, plus the 27 newest rule boards in `design/night-manuscript/gen/scr_new_*.py`), the rule tabs 11, 12, 13, 18, 19, 20, 21, 22 and 23 (confirmed revisions), and the planning main tab text (the rev 41 copy). Result: every board, rule section and planning module maps to existing features, so **no feature was added**. Four rule sections had features but no test case; those tests were added to the specs (see *Tests added*).

This section is written by hand. The script that generates the tables above rewrites the whole file, so re-append this section after a rerun.

### Boards

- 193 of 210 boards appear in an area spec's §5 Screens and are cited by at least one feature block.
- `30-cast.md` and `50-canvas-pages.md` have no Features column in §5. Their boards are tied to features through the feature blocks instead: `CanvasCLabel` CNV-08 · CST-34; `ColCast`, `TwoCast` CST-06 · CNV-33; `MFoldList` CNV-34 · CST-06; `FieldHistory` CST-10; `Page` CNV-13; `Relations` CST-15 · CST-28 · CST-31; `Voice` CST-16 · CST-17; `RCProfileAdd` CST-22; `RCRelTypes` CST-32.
- **Excluded** (design explorations, not screens to build), 17 boards:
  - 온보딩 방식 비교 (12): `CoachScript`, `Tour1`, `Tour2`, `Tour4`, `TourCast`, `TourLr`, `TourMap`, `TourWhen`, `TourWorld`, `Walk1`, `Walk2`, `Walk3`. Some are still used as visual references by LIB-16 (walkthrough), LIB-19 (module tours) and SCR-60.
  - 스타일 (2): `Style`, `Accent` (the old accent comparison, `#031C35`).
  - 실험 · 홀로그램 (3): `Holo`, `HoloKit`, `HoloMoment`. CNV-30, CNV-31 and DS-54 use them only as references for the holo material.

### Rules → features → tests

Test ids are those of the spec named (10 = `10-script.md`, 20 = `20-story-time.md`, and so on). Each rule's `결정할 것` table and its §1 (what was missing in the boards) only summarise the sections below and carry no behaviour of their own, except rule 11 §1 and rule 12 §1, which are rules.

| Rule § | Features | Tests |
| --- | --- | --- |
| 11 §1 최신 = 집필 선 · 예정 · 고치기 두 버튼 | STO-12, STO-14, STO-15, CST-10 | 20 LENS-01…14, EDIT-01…04; 30 TC-11-xx |
| 11 §2 시점의 경계 | STO-12, STO-13, SCR-33 | 20 LENS-xx; 10 T-11-01, T-11-02; 30 TC-13-07 |
| 11 §3 스크립트와 렌즈 | STO-16, SCR-02, STO-13 | 20 LENS-xx; 10 T-11-03 |
| 11 §4 기준 + 간격 · 연쇄 · 회상 | STO-21, STO-23, STO-24, STO-10 | 20 BASE-xx, MOVE-01; 10 T-12-07, T-22-18 |
| 11 §5 대략 · 거꾸로 가는 때 | STO-22, STO-25 | 20 TIME-xx, ORD-01…03 |
| 11 §6 N일째 | STO-22, STO-17 | 20 TIME-01…13; 10 T-22-04 |
| 11 §7 다른 규칙과의 연결 | — (links only) | — |
| 12 §1 블록에 묶이는 것 · 세 자리 | SCR-41, SCR-42, STO-12 | 10 T-12-16, **T-12-25** |
| 12 §2 근거 블록을 지울 때 | SCR-38, SCR-43 | 10 T-12-01…07, T-12-19, T-12-20 |
| 12 §3 옮길 때 | SCR-37, SCR-44, SCR-54, STO-08 | 10 T-12-11…15, T-12-21, T-12-22 |
| 12 §4 바꾸기 · 나누기 · 합치기 · 복사 · 선택지 | SCR-09, SCR-13, SCR-15, SCR-40 | 10 T-12-08…10, T-12-16 |
| 12 §5 블록 번호와 영구 키 | SCR-21, SCR-22 | 10 T-12-02, T-12-12, T-12-17 |
| 12 §6 보이스 ID | SCR-23, SCR-24, SCR-64, LIB-46 | 10 T-12-13, T-12-17, T-12-18, T-12-23 |
| 12 §7 휴지통과 되돌리기 | SCR-39, LIB-38 | 10 T-12-04, T-12-05, T-12-19, T-12-24; 60 T-xx |
| 12 §8–§9 바뀌는 시안 · 연결 | copy applied in SCR, STO, LIB | — |
| 13 §2 사이와 마음 | CST-28 | 30 TC-13-01…05 |
| 13 §3 종류 목록 | CST-32 | 30 TC-13-02, TC-13-11…13, **TC-13-20** |
| 13 §4 누구 쪽에서 보나 | CST-28, CST-33, CST-38 | 30 TC-13-05, TC-13-07, TC-13-14, TC-13-17 |
| 13 §5 호칭 | CST-31, SCR-29 | 30 TC-13-09, TC-13-18 |
| 13 §6 시간에 따른 변화 | CST-30, SCR-41 | 30 TC-13-06, TC-13-08, TC-13-19 |
| 13 §7 만드는 곳 | CST-29, CST-34, CNV-08 | 30 TC-13-10, TC-13-15; 50 T-CNV-09… |
| 13 §8 경계 | CST-20, CST-36 (P2), WLD-69 (P2) | 30 TC-13-16 |
| 13 §9 바뀌는 시안 · 내보내기 | CST-35 (+ copy) | 30 **TC-13-21** |
| 18 §2 하루와 시간대 | STO-19 | 20 BAND-01…05; 10 T-18-01 |
| 18 §3 이동 수단과 필요 시간 | STO-28, WLD-47 | 20 TRV-01, TRV-02, TRV-16; 40 WLD-T53 |
| 18 §4 두 씬 사이의 시간 | STO-29 | 20 TRV-03, TRV-09 |
| 18 §5 판정과 제안 칩 | STO-30 | 20 TRV-04…07, TRV-10 |
| 18 §6 경로가 없을 때 | STO-29, WLD-66 | 20 TRV-11…13, TRV-15; 40 WLD-T55 |
| 18 §7 바뀌는 시안 | copy in STO-19, WLD-43 | — |
| 19 §2 등급을 바꿀 때 | LIB-23, LIB-24, LIB-25 | 60 R19-01…15 |
| 19 §3 가리는 범위와 이름 | LIB-05, LIB-07, LIB-26…30 | 60 §4.2 (S-xx … N-xx) |
| 19 §4 잠금 화면 | LIB-33, LIB-34, LIB-35 | 60 §4.3 (L-xx, RL-xx) |
| 19 §5 내보내기와 백업 | LIB-42, LIB-43, LIB-57, SCR-63…65 | 60 E-xx, B-xx; 10 T-19-xx |
| 19 §6 항목 숨기기 | LIB-36, SCR-58, CST-39, WLD-13, CNV-16, STO-11 | 60 I-01…18; 30 TC-19-01…09; 10 T-19-01…04 |
| 19 §7 바뀌는 시안 | copy | — |
| 20 §2 딸린 것과 가리키는 것 | CST-41, SCR-57, WLD-14, LRC-23 | 30 TC-20-02, TC-20-12…16, TC-20-21…25; 10 T-20-xx |
| 20 §3 점검 센터와 한꺼번에 고치기 | CST-42 | 30 TC-20-04, TC-20-28; 50 T-CNV-23… |
| 20 §4 지우기 확인 · 영향 수 | CST-40 | 30 TC-20-01, TC-20-03, TC-20-11 |
| 20 §5 되살리기와 영구 삭제 | CST-43, CST-44, CST-48, LIB-39 | 30 TC-20-05…09, TC-20-20 |
| 20 §6 합치기 | CST-45, CST-46, WLD-15 | 30 TC-20-17…19, TC-20-26, TC-20-27; 40 WLD-T69, WLD-T70 |
| 20 §7 바뀌는 시안 | copy | — |
| 21 §2 연결과 후보 | WLD-17, WLD-19, WLD-22 | 40 §4.2; 10 T-21-xx |
| 21 §3 글자 맞추기 | WLD-16, WLD-21 | 40 §4.3 |
| 21 §4 어느 글을 보나 | WLD-19, WLD-27 | 40 §4.2; 10 T-21-xx |
| 21 §5 첫 공개와 ▲ | WLD-18, WLD-20, WLD-28 | 40 §4.4; 10 T-21-xx |
| 21 §6 잇기 · 아님 · 맞음 | WLD-22…25 | 40 WLD-T05, T06, T08, T11…T13, T31 |
| 21 §7 찾는 시점과 비용 | WLD-26 | 40 WLD-T10, WLD-T47, **WLD-T72** |
| 21 §8 바뀌는 시안 | copy | — |
| 22 §2 원본은 씬 속성 하나 | SCR-48, STO-27 | 20 TRN-01…05; 10 T-22-01…06 |
| 22 §3 씬 머리 | SCR-46, SCR-47 | 10 T-22-01, T-22-07, T-22-23 |
| 22 §4 씬 중간의 /씬 | SCR-49, SCR-50, SCR-53 | 10 T-22-08…12, T-22-19, T-22-20 |
| 22 §5 나누기 · 합치기의 장소와 때 | STO-09, SCR-50, SCR-51 | 20 SPL-xx, MRG-01…04; 10 T-22-09…26 |
| 22 §6 내보내기 · 머리말 · 한꺼번에 | SCR-02, SCR-63…65, STO-34 | 10 T-11-03, T-22-21, T-22-22 |
| 22 §7 바뀌는 시안 | copy | — |
| 23 §2 연결 | LRC-18…21 | 70 §4.2 |
| 23 §3 상태와 필터 | LRC-26, LRC-27 | 70 §4.3 |
| 23 §4 넣기 | LRC-32…39, SCR-59, LIB-12, LIB-13 | 70 §4.4; 10 T-23-01, T-23-02 |
| 23 §5 글 메모의 @과 받아쓰기 | LRC-17, LRC-20 | 70 LRC-T05, LRC-T07 |
| 23 §6 숨김 · R-18 · 내보내기 · 지움 | LRC-22, LRC-23, LRC-40…43, LRC-45 | 70 §4.5–§4.7 |
| 23 §7 바뀌는 시안 | copy | — |

### Tests added

- `10-script.md` T-12-25: point seat shown in the panel, not the manuscript; world-event seat never shown (rule 12 §1). The §4.6 coverage row is updated.
- `30-cast.md` TC-13-20: switching a kind's category shows its effect first (rule 13 §3).
- `30-cast.md` TC-13-21: relation export to `.civilight` and game-engine JSON (rule 13 §9, CST-35). New open question §6 Q29.
- `40-world.md` WLD-T72: full scan on project open and `점검 중` (rule 21 §7).

### Planning main tab → features

| Planning section | Features |
| --- | --- |
| 목표 · 차별점 | STO-13, SCR-01…12, LIB-45, WLD-18…20, WLD-31, STO-37, CNV-32, LRC-01… |
| 앱 구조 | LIB-01, LIB-08, STO-01, STO-36, STO-38, CNV-35 |
| 공통 개념 | STO-13, CST-10, SCR-41, STO-17…22, DS-16, SCR-26…28, SCR-55, WLD-17, STO-36…38, DS-29, STO-03 |
| 스크립트 편집기 | SCR-01…69 |
| 인물 | CST-01…35 |
| 세계 · 연표 · 지도 | WLD-01…37 |
| 지도 만들기 | WLD-36…52 |
| 지도 생성 · 양식 | WLD-53…61 |
| 지도 ↔ 연표 | STO-17…35, WLD-62…66 |
| 캔버스 | CNV-01…12 |
| 프로젝트 홈 · 스토리 · 페이지 · 점검 · 검색 | STO-01…11, CNV-13…16, STO-39…42, CNV-17, STO-37 |
| 라이프 레코더 | LRC-01…47 |
| 서재 · 숨기기 · 수위 등급 | LIB-01…07, LIB-22…37 |
| 카드 보기 · 이미지 | CNV-21…31 |
| 온보딩 | LIB-08…21 |
| 화면 · 디자인 | CNV-32…46, DS-01…64 |
| 저장 · 백업 · 내보내기 | LIB-35, LIB-38…62, LIB-70, LIB-71 |
| 범위 | rows below |
| 남은 확인 | real-device checks and owner decisions, listed under *Still uncertain* |

**Lines in the rev 41 text that later rules replaced** (built as the rule says, not as the old line says):

- `씬 전환` block → `씬 머리` with `전환 문구` (rule 22 §3).
- `블록마다 영구 ID(#043)` → hidden permanent key + per-episode number (rule 12 §5).
- Time stored as one value → base + interval (rule 11 §4).
- Past-lens banner only → two buttons `이 값 고치기 · 여기서부터 바뀜` (rule 11 §1); the script has no lens (rule 11 §3).
- `+3일로` · `말 1일` → `+2일로` · `말 · 약 7시간 40분` (rule 18 §5).
- Canvas line kinds `동료 · 경계 · 가족 · 스승 · 적대` → 사이 and 마음 kinds (rule 13).
- Term `쓰인 곳` as manuscript sentences → links plus candidates (rule 21 §2).
- Recorder `붙일 곳`, filter `연결됨`, `인물에 붙이기` → `연결`, states `정리 안 됨 · 넣음 · 끝냄`, `카드에 넣기` (rule 23).
- `블록 · 항목 삭제는 되돌리기 알림` → trash for 30 days, impact sheet for items with references (rule 12 §7, rule 20 §4).
- Manuscript body set in 명조 → Pretendard everywhere (user decision, domain model C-30, DS-05).

**Not built, by decision** (범위 table): 분기 흐름도, 표정 태그 · 표정 고르기, 전투 대사 · 보이스 지시 전용 블록, 펜 · 스케치 레이어, Notion 가져오기, 자동 화자 번갈아 넣기 · 스와이프로 바꾸기, coach marks.

**Later, with no feature row:** 자체 서버 동기화 (the `.civilight` items already carry last-modified time and device for it, LIB-49), 도시 · 건물 생성 (node maps instead, WLD-50), worlds whose day is not 24 hours (rule 18 §2).

### Still uncertain

1. **App database.** The Room working store, its explicit migrations and autosave are defined in `00-domain-model.md` §8.2 and §9 and used by every feature, but no row of this file names them. Build them with the first P0 feature.
2. **Navigation.** No single row covers the app's route map and system back (서재 → 프로젝트 홈 → modules, no tab bar). It is spread over LIB-01, LIB-08, STO-01, STO-36, STO-38 and CNV-35.
3. **Example project content.** LIB-17 and LIB-56 ship 「잿빛 새벽의 등불」 as a `.civilight` asset, but no feature lists building its full content (cast, world, maps, manuscript). Rule tab 10 (sample data) was not part of this check.
4. **Planning text revision.** This check used the rev 41 copy of the main tab; the specs were written from rev 71. A rev 71 copy has the same 19 sections and folds in the rule decisions listed above; it was compared by section only, not line by line.
5. **Duplicate rows.** 세력 사이 관계 is both CST-36 and WLD-69; 설정 · 지도 · 이미지 · 연표 is CNV-19, WLD-67 and LIB-68 (content vs container). Build each once.
6. **Board vs rule.** The default relation kinds on `RCRelTypes` differ from rule 13 §3; the spec follows the rule (`30-cast.md` §6 Q1).
7. **Open flags in the confirmed rules:**
   - Rule 19 §6: hiding places and factions is a new rule that still needs the user's confirmation (WLD-13 builds it as the rule table says).
   - Rule 19 §7: whether hidden scenes count toward the 집필 선, voice-ID fixing when a hidden scene is left out of an export, hidden characters' relation lines on the other side, and travel checks through hidden scenes are left to rules 11, 12, 13 and 18.
   - Rule 21 §4: whether `괄호 지문` goes to game output, and whether profile game text raises `▲`, are 미정 (both `▲` 없음 for now).
   - Rule 21 §8: whether "a first reveal cannot sit on a choice option" is added to rule 12.
   - Rule 23 §6: two new defaults marked 확인 필요 (the `숨기기` row on for hidden or R-18 link targets; hidden records left out of `.civilight` unless the file is encrypted).
   - Rule 12 §5–§7: `#` links in memos, profile game-text voice IDs (CST-52, P2) and per-block text history are left for later.
8. **Real-device checks and owner decisions** (planning `남은 확인`): glass blur cost at 120 Hz, real dp of both screens and state restore on fold, whether the system "reduce blur" setting is readable, long-session readability of the manuscript body (size 18, line height 1.7; now set in Pretendard, C-30), gyro holo feel, map generation speed with tiles, blur and holo together, which devices run an on-device language model; the main Fold model, the speech-to-text engines, the final game-engine JSON format, and release (font licences, Drive permission review).
