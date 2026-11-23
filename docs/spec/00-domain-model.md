# 00 · 도메인 모델 (Domain model) — one model for the whole app

The single data model of Civilight. Every area spec (`10-script.md` … `80-design-system.md`) describes behaviour; this file decides **what is stored, under which name and type, who owns it, how it is deleted, how story positions compare, and how it is written into `.civilight`**. Where an area spec's §3 disagrees with this file, this file wins and §10 names the spec text that must change. Questions that are still the product owner's (an `ED` / `[PROPOSED]` default in some §6) are listed in §11 and are not decided here.

## 0. Precedence and scope

- **Tie-break used for every conflict:** confirmed rule tabs and the 저장 파일 양식 tab (FMT) and memory notes > the area that owns the entity (per the ownership column below) > other areas' assumptions > board samples. When two area specs each made an engineering default (`ED`, `[PROPOSED]`) for the same thing, the one that keeps references by key, keeps the file forward-compatible and needs the fewest special cases wins; the reason is given in §10.
- **Storage is two-layered** (FMT §1): the **app DB** (Room) is the working store and may change schema freely; the **`.civilight` package** is the move/backup unit, written only on export/backup and read only on import/restore. This file defines both and the mapping (§7, §9).
- `ED` marks an engineering default made **in this file** where no source decides; like the area specs' `ED`/`[PROPOSED]`, it stands until the product owner says otherwise.
- Feature ids (`SCR-21`, `STO-12` …) refer to the area specs. Rule references (`R11 §4`) refer to the planning doc rule tabs, as in the area specs.

## 1. Conventions (apply to every entity)

### 1.1 Keys (rule 12 hidden permanent key)

- Every stored item has a `key`: a 26-character **ULID** string (Crockford base 32, upper case) made on the device when the item is created. It is never shown in the UI, never changes, never reused, and survives rename, move, trash, restore, renumbering and export/import. It is the "hidden key" of rule 12: block numbers (`#043`), record numbers (`인물 기록 007`), codes (`1-7`), ordinals (`씬 4`) and names are **labels**, never identity.
- **References are always by key** (FMT §4), never by name, number, label or array position. A reference field is named `<role>Key` (`sceneKey`, `speakerKey`, `parentKey`, `fromNodeKey`); a list of references `<role>Keys`.
- **Scope.** Item keys are unique inside one project (R12 §5 "프로젝트 전체에서 하나"). The same item key may exist in two projects (a `새 프로젝트로` import keeps item keys, LIB-52). Therefore:
  - the portable identity of an item is `(projectKey, itemKey)`;
  - recorder records (outside projects) and recorder links are keyed globally by their own ULID and point into projects with `(projectKey, targetKey)`;
  - in the app DB every project-scoped table is keyed by `(projectId, key)`, where `projectId` is a **local surrogate integer** of the project row that never leaves the device (§6 explains why the portable `Project.key` alone is not enough).
- Keys are compared as plain strings; ULIDs sort by creation time, which is used only as a last tie-break (e.g. `createdAt` ties) and never as manuscript order.

### 1.2 OrderKey (manual order)

Manual order (blocks in a scene, units among siblings, characters, fields, options, canvases, map layers …) is stored as an **OrderKey** string on the item, field name `order`; never as array position (FMT §4).

- **Alphabet:** the 62 ASCII characters `0-9`, `A-Z`, `a-z`, ranked in ASCII order (`'0' < … < '9' < 'A' < … < 'Z' < 'a' < … < 'z'`).
- **Valid key:** non-empty, only alphabet characters, never ends in `'0'`.
- **Comparison:** byte-wise (ASCII code units), shorter-is-prefix sorts first. In Kotlin `String.compareTo` is correct because keys are ASCII; **never** a `Collator` or locale-aware compare. SQLite columns holding order keys use `COLLATE BINARY`.
- **Generation:** `between(a, b)` returns a key strictly between `a` (exclusive; `null` = before everything) and `b` (exclusive; `null` = after everything). Reference algorithm (base-62 midpoint, as in the *fractional-indexing* scheme), `D = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"`:

  ```kotlin
  // a: lower bound, "" = before everything; b: upper bound, null = after everything; requires a < b
  fun mid(a: String, b: String?): String {
      if (b != null) {
          var n = 0
          while (n < b.length && (a.getOrElse(n) { '0' }) == b[n]) n++
          if (n > 0) return b.substring(0, n) + mid(a.drop(n), b.drop(n))
      }
      val da = if (a.isEmpty()) 0 else D.indexOf(a[0])
      val db = if (b == null) D.length else D.indexOf(b[0])
      if (db - da > 1) return D[(da + db) / 2].toString()
      return if (b != null && b.length > 1) b.substring(0, 1)
             else D[da] + mid(a.drop(1), null)
  }
  fun between(a: String?, b: String?) = mid(a ?: "", b)
  ```

  The first key of an empty list is `between(null, null)` = `"V"`. Generation is deterministic (no random jitter) in v1.
- **Rebalancing:** when a generated key would exceed 40 characters, the app rewrites the order keys of that one sibling list with evenly spaced keys in one transaction. This is safe because nothing references order keys.
- Code labels derived from order (`1-7`, `씬 3`, `기록 2`) are recomputed on read and never stored.

### 1.3 Bookkeeping fields

Every persisted entity (row in the app DB and object in a file) carries:

| Field | Type | Meaning |
| --- | --- | --- |
| `updatedAt` | Instant (UTC) | last change (FMT §7) |
| `updatedBy` | String | device id of the last change (FMT §7); a random id made once per installation |
| `ext` | JSON object? | extension slot (FMT §4); unknown keys read from a file are kept here and written back (§8.3) |

Entities that show a creation time also carry `createdAt: Instant`. These fields are not repeated in the tables below. (Replaces `modifiedAt`/`modifiedBy`/`modifiedByDevice`/`updatedByDevice`/`updatedDevice` in the area specs; §10 C-01.)

### 1.4 Text

All text is UTF-8 and **NFC-normalised** on write and on read (FMT §2, LIB-49). Plain short strings (names, titles, memo lines) are `String`. Text that can hold `@` links or typed markup is stored as **runs**.

**`TextRun`** (sealed; unknown kinds read from a newer file are kept verbatim and rendered as their `t` text, FMT §4):

| Kind (JSON `k`) | Fields | Used in | Meaning |
| --- | --- | --- | --- |
| `plain` (default, `k` omitted) | `t: String` | everywhere | ordinary text |
| `paren` | `t: String` | dialogue blocks | `괄호 지문`; stored without the parentheses, drawn `(t)` (SCR-07) |
| `link` | `id: Key` (linkKey), `to: LinkTarget`, `t: String` | everywhere text runs exist | an `@` link (**LinkRun**); `t` = exactly the text shown (name, alias, or the linked candidate range) |
| `break` | — | blocks | soft line break (`줄바꿈`, SCR Q-03) |

**`LinkTarget`** = `{ kind, key, projectKey? }` with `kind ∈ character · term · place · faction · page · unit · project`. `projectKey` is present **only** in recorder text (records live outside projects); everywhere else the target is in the same project. Allowed kinds per surface (the data allows the union; the picker offers only these):

| Surface | Allowed `to.kind` |
| --- | --- |
| Script blocks, choice options, transition runs | `character · term · place` (SCR-55) |
| Character bodies, field long text, voice lines, archive docs, world descriptions, pages, event descriptions | `character · term · place · faction · page` (CNV-13, WLD-17) |
| Recorder record text | all, incl. `unit` and `project` (LRC-17, LRC Q20) |

**Link identity.** `linkKey` is minted when the link is created (picker, `잇기`, `모두 잇기`). It **survives** edits of surrounding text, block split (the run stays with whichever block holds it), block merge, move, cut/paste-as-move and scene split/merge. It is **re-minted** on copy/paste, on recorder `넣기` copies (LRC-32), and on any other copy, because a copy is new content (rule 12). Acknowledgements of `▲ 공개 전 사용` attach to the `linkKey` (WLD-24).

**`RichDoc`** — keyed paragraphs, used by every long text that the recorder can append to or that has headings: `List<DocBlock>`.

| `DocBlock` field | Type | Meaning |
| --- | --- | --- |
| `key` | Key | paragraph identity; target of a recorder `cardParagraph` placement |
| `order` | OrderKey | position in the document |
| `kind` | `paragraph · heading · memo` (open list, Q-PG-01) | 본문 · 소제목 · `⌁ 작가 메모` |
| `runs` | List<TextRun> | content |
| `recorderSource` | RecorderSource? | set on a `⌁` paragraph made by `카드에 넣기` / `인물로 만들기` / `페이지로 만들기` |

`RecorderSource` = `{ recordKey: Key, recordedAt: Instant }` (shown `레코더 11월 22일 08:41`; the mark text when the record is trashed or purged is derived, LRC-32).

### 1.5 JSON conventions (all files of §7)

- Property names: lowerCamelCase; references end in `Key`/`Keys` (`parentKey`, `fromNodeKey`, `targetKey`), never bare `parent`, `from`, `target`.
- Enum values: lowerCamelCase strings of the Kotlin enum name: `DIALOGUE` → `"dialogue"`, `SCENE_TRANSITION` → `"sceneTransition"`, `CHAPTER_EPISODE_SCENE` → `"chapterEpisodeScene"`, rating `R15` → `"r15"`. Display words (`"R-15"`, `"아침"`) are never stored as enum values. Unknown enum values are kept as the raw string (§8.3).
- `Instant` → ISO-8601 UTC with `Z` (`"2026-11-21T23:41:00Z"`); `LocalDate` → `"2026-11-22"`; `LocalTime` (clock in a story day) → `"HH:mm"`; durations → integer milliseconds named `…Ms`.
- Decimal values typed by the author (field `숫자`) are strings (`"154"`, `"1.5"`) so no precision is lost.
- Absent optional values are omitted (not `null`), except where `null` carries meaning (stated in the schema).
- Arrays of keyed items are unordered sets; their order comes from `order` keys.

### 1.6 Storage classes

| Class | Where | Leaves the device as |
| --- | --- | --- |
| **P** project data | app DB, project-scoped tables | `.civilight` (§7), Drive backup |
| **P·trash** | app DB rows with `deletedAt` | only `trash/` with `휴지통도 포함` (§7.9) |
| **R** recorder library | app DB, global tables | `.civilight-recorder` (§7.11); the slice linked to a project also in that project's `recorder.json` |
| **A** app DB only | app DB | never (not even Drive) unless stated |
| **S** app settings | DataStore | Drive app-settings backup file only (LIB-57) |
| **RT** runtime | memory / crash-recovery file | never |
| **C** code-only | constants in code (`core/designsystem`, preset registry) | never |

### 1.7 Canonical names (glossary)

One name per concept in code, DB and files. Area spec names on the right are aliases that must be renamed (§10).

| Canonical | Korean UI word | Aliases in area specs | Not to be confused with |
| --- | --- | --- | --- |
| `StoryUnit` | 장 · 에피소드 · 씬 (level names) | Chapter, Episode, Scene, StructureNode; a level is an index into `structure.levels` (no `StructureLevel` row, C-08) | — |
| `Scene` | 씬 | scene-level `StoryUnit` + `SceneProps` + SCR scene fields | `장면` (only a level name in `막 › 장면`) |
| numbering unit | 에피소드 | Episode (SCR "episode block counter") | §4.1 |
| `Block` | 블록 | — | scene head (`씬 머리`, not stored) |
| `ChangeRecord` | `◆` 변화 기록, `◇` 첫 공개 | FirstReveal (WLD) | `RecorderRecord` |
| `Anchor` | 자리 · 근거 | seat, pointUnit, `Anchor.Point`, `POINT` | `PointRef` (reveal/unlock points, §4.6) |
| position · `LensPoint` | 시점 · 위치 · 집필 선 | StoryPoint, EpisodeEnd, SceneEnd | story time (때, §4.7) |
| `Relation` | 관계 | Relationship (CNV) | `RelationKind` |
| `FieldDef` / `FieldValue` | 항목 / 값 | FieldDefinition | fixed fields (columns of `Character`) |
| `LinkRun` | `@` 연결 | link run, `Link` run | `RecorderLink`, field type `LINK` |
| `RecorderLink` | 연결 (레코더) | LrLink | `LinkRun` |
| field type `LINK` | 항목 종류 `연결` | — | the two above |
| `RecorderRecord` | 기록 (레코더) | LrRecord, RecorderRecord, recorder entry | `ChangeRecord`, `Character.recordNo` (인물 기록 number) |
| `Placement` | 넣은 곳 | LrPlacement | — |
| `TermRejection` | 아님 | CandidateDismissal, the `아님` half of a term candidate decision | `Acknowledgement` (`맞음`, the other half) |
| `Acknowledgement` | 맞음 · 이대로 두기 | CheckAck, CheckAcceptance, RefAck | `TermRejection` |
| `ScriptAsset` | 에셋 (배경 · BGM · 효과음 · CG) | Asset (SCR) | `Blob` |
| `Blob` | (file) | ImageAsset (CNV), LrAudio file | `ImageUse` |
| `ImageUse` | 이미지 (slot) | ImageSlotRef, ImageRef, coverImage, headerImage | — |
| `DocBlock` / `RichDoc` | 문단 · 소제목 · `⌁` 메모 | PageBlock, RichText | `Block` (script) |
| `MapRegion` | 영역 (map editor) · 지역 (scene place kind) | Region | `PolyProvince` (polygon-map 지역 cells) |
| `NameTag` | 이름표 | tombstone name tag | `RecorderLink.lastLabel` / `Placement.lastLabel` (recorder's own last-known label strings, formerly `nameTag`) |
| `TrashEntry` | 휴지통 줄 | — | `DeletionLogEntry` |
| `hidden` | 숨기기 (rule 19 item hiding) | — | `FieldDef.definitionHidden`, `MapLayer.visible` |

**Not entities:** *Card* (카드) is a view of a `Character` / `Place` / `Term` / `Faction` (text = their `RichDoc` fields, image = an `ImageUse` with slot `cardReference` or `characterPortrait`). *Alias* (별칭) is a `String` in `Term.aliases`. *Scene head* (`씬 머리`) and *transition ID* are derived. *Speaker* is a `Character` referenced by `Block.speakerKey` / `ChoiceOption.speakerKey`. *Calendar* is embedded in `Project.worldTime`.

## 2. Entity catalogue

Columns: **Class** = storage class (§1.6) · **Owner** = area that defines the semantics and writes it (others read or call its API) · **File** = place in `.civilight` (§7). Types: `Key` (ULID), `OrderKey`, `Instant`, `LocalDate`, `LocalTime`, `Int`, `Long`, `Boolean`, `String`, `TextRun`, `RichDoc` (§1.4). `?` = nullable/optional. Bookkeeping fields (§1.3) are implicit. Fields marked **[changed]** differ from the owning area spec (see §10).

### 2.1 Project and project settings — owner LIB (settings groups: owners as named)

**`Project`** · P · `project.json` (+ manifest `project`)

| Field | Type | Meaning |
| --- | --- | --- |
| `key` | Key | portable project identity; same key = same project on open/restore (LIB-52) |
| `name` | String | `이름` |
| `oneLiner` | String? | `한 줄 소개` |
| `format` | `serial · short · settingOnly` | `연재 장편 · 단편 · 설정만` |
| `rating` | `all · r15 · r18` | 수위 등급 |
| `protections` | `Protections { hidden, captureBlock, nameMask, recorderLinkMask }: Boolean` | rule 19 per-project values (LIB-23) |
| `structure` | `StoryStructure` **[changed]** | `{ preset: chapterEpisodeScene · actScene · custom, levels: List<String> (1..4, top → scene level) }` (owner STO-02; replaces LIB `structurePreset` + `structureLevels: List<StructureLevel>`) |
| `worldTime` | `WorldTime` **[changed]** | owner WLD-29: `{ mode: yearOnly · calendar · none, storyStartYear: Int?, calendar: WorldCalendar? }` (replaces LIB `timeMode`/`startYear`/`calendarKey`) |
| `sceneTime` | `SceneTimeSettings` | owner STO-17/19: `{ notation: episode · storyStart, dayBoundary: dawn0400 · midnight, bandStarts: Map<Band, LocalTime> }`; `Band = dawn · morning · day · evening · night` (새벽 · 아침 · 낮 · 저녁 · 밤), defaults 04:00 · 06:00 · 10:00 · 17:00 · 20:00 |
| `script` | `ScriptSettings` (SCR's ProjectScriptSettings) | owner SCR-24/25/48: `{ stagePositionEnabled=false, voiceIdEnabled=true, voiceIdFormat: { mode: pattern · manual, pattern="{code}_{chapter2}{episode2}_{number3}" }, transitionDefault: auto · direct · none (=none), transitionTemplate: placeTime · placeOnly (=placeTime) }` |
| `cast` | `CastSettings` (CST's ProjectCastSettings) | owner CST: `{ bundleName: Map<EntityKind, String>, showFirstAppearanceInHeader=true, showVoiceProgressInHeader=true, relationExportAddress=false }` |
| `travelModes` | List<`TravelMode`> | owner WLD-47: `{ key, order, name, kmPerDay: Double, movingHoursPerDay: Double }`; defaults 도보 15/8, 말 40/8, 배 30/24 |
| `defaultTravelModeKey` | Key? | project default (도보 when null) |
| `termMatching` | `TermMatching { extraParticles: List<String> }` | owner WLD-21 |
| `fieldBundleAtCreation` | `basic · gameProfile · empty · fromProject` | informational (LIB-12) |
| `counters` | `{ nextCharacterRecordNo: Int ≥ 1, nextBlockNumber: Int? }` **[new, `ED`]** | issue-only counters: `인물 기록 NNN` never reused (CST Q11 default needs a stored counter); `nextBlockNumber` only for 1-level structures (§4.1) |
| `createdAt` | Instant | |
| `fieldDefs`, `tags`, `relationKinds` | lists | stored inside `project.json`, entities in §2.5 |

`WorldCalendar` = `{ months: List<{ name: String, days: Int }>, weekdays: List<String>, storyStart: { month: Int, day: Int }?, storyStartWeekday: Int? }` (WLD-30).

**(0.2.0)** `rating` is kept and written but no longer shown or set by the UI (§13.3). New optional property `gameTools: Boolean?` (absent ≠ false, §13.2).

App-DB-only project columns (A): `id: Long` (local surrogate, §6), `deletedAt`/`trashEntryKey` (trash), `isExample`, `readOnly` (last-opened time and position live in `ProjectActivity`, §2.13). They are never written to the file (`isExample` is a property of the bundled asset, LIB-17).

### 2.2 Story structure and scenes — owner STO (scene memo and transition fields: SCR)

**`StoryUnit`** · P · `story/index.json` — one row per chapter, episode, scene or custom-level unit.

| Field | Type | Meaning |
| --- | --- | --- |
| `key` | Key | |
| `parentKey` | Key? | null for top-level units |
| `level` | Int | index into `Project.structure.levels`; the last index is the **scene level** |
| `order` | OrderKey | among siblings |
| `title` | String? | null → `미정` |
| `synopsis` | String? | chapter/episode plan and description line; receives recorder `메모 칸에 넣기` lines for 장 · 에피소드 (LRC-37) |
| `doneMarked` | Boolean | author-set 완료 (STO-06) |
| `hidden` | Boolean | item hiding (rule 19); meaningful on scene-level units only (LIB `HideableKind.SCENE`) |
| `nextBlockNumber` | Int? | only on numbering units (§4.1), owner SCR-21; only grows; stored here, in `story/index.json`, not in the episode file **[changed, C-32]** |
| `deletedAt`, `trashEntryKey` | Instant?, Key? | trash (§5) |

**Scene-level fields** (columns of the same row when `level` = scene level; JSON: on the same object):

| Field | Type | Owner | Meaning |
| --- | --- | --- | --- |
| `place` | `PlaceRef? = { kind: place · region, key }` **[changed]** | STO | 장소 (a `Place`) or 지역 (a `MapRegion`, project-unique key); replaces WLD's `(mapKey, regionKey)` pair |
| `time` | `SceneTime` | STO | below |
| `silentCast` | List<Key> | STO | 말 없는 등장 (characters); speakers are derived from blocks |
| `memo` | String | SCR | `씬 메모`, multi-line plain text |
| `transitionMode` | `auto · direct · none` | SCR | |
| `transitionRuns` | List<TextRun>? **[changed]** | SCR | last 직접 text, kept while the mode is `auto`/`none` (was `transitionText: String`; runs so that rule 22 §3 terms can be links, Q-32 decides whether the editor offers `@`) |
| `transitionSnapshot` | `{ place: PlaceRef?, dayRange: [Int, Int]?, tRange: [Int, Int]? }?` | SCR | place and resolved time (story-day range, minutes-after-04:00 range) when the direct text was saved; input of `transition.stale`, whose row label (`06:10 → 07:00`) is derived from it (SCR's `[PROPOSED shape]` with `PlaceRef`) |

**`SceneTime`** (sealed; STO-21):

| Variant | Fields |
| --- | --- |
| `none` | — |
| `relative` | `baseKey: Key` (base scene), `dayMin: Int`, `dayMax: Int` (equal when exact; any integer, may be negative), `precision: exact · approx`, `band: Band?`, `clock: LocalTime?`, `rebasedFromKey: Key?` (scene this was rebased away from, STO-10) |
| `direct` | `storyDay: Int?` **or** `calendarDate: { month, day, yearOffset }?`, `band?`, `clock?` — a fixed point |
| `flashback` | `mode: anchor · direct`, `eventKey: Key?`, `relation: before · on · after ?`, `amount: { days: Int }?` (Q20 open), `year: Int?`, `calendarDate?`, `band?`, `clock?` |

`clock` is stored in the canonical 04:00-day form regardless of `dayBoundary` (STO-19). Resolved time, `N일째`, labels and time order are derived (§4.7).

**`TravelSegmentOverride`** · P · `checks.json` — `{ key, characterKey, fromSceneKey, toSceneKey, modeKey }` (STO-28). Void when either scene is purged.

### 2.3 Script — owner SCR

**`Block`** · P · `story/<numbering unit key>.json`

| Field | Type | Meaning |
| --- | --- | --- |
| `key` | Key | hidden permanent key (JSON `id`, CSV `블록 ID`, anchors, acks, rejections, placements) |
| `sceneKey` | Key | containing scene |
| `order` | OrderKey | position in the scene |
| `number` | Int ≥ 1 | per numbering unit, shown `#043` (§4.1) |
| `kind` | `dialogue · narration · direction · choice · system · memo` | unknown kinds from newer files are kept read-only (§8.3) |
| `speakerKey` | Key? | `dialogue` only; null = `화자 없음` |
| `runs` | List<TextRun> | `dialogue · narration · system · memo`; empty otherwise |
| `stage` | `left · center · right ?` | `dialogue` only, used when `stagePositionEnabled` |
| `cues` | List<`DirectionCue`> | `direction` only |
| `options` | List<`ChoiceOption`> | `choice` only, ≥ 2 |
| `voice` | `VoiceState?` | `dialogue` only |
| `recorderSource` | RecorderSource? | `memo` made by `원고에 넣기` / `여기 넣기` |
| `createdAt` | Instant | |
| `deletedAt`, `trashEntryKey` | Instant?, Key? | set while directly trashed (§5); **[changed]** from `trashedAt` |
| `trashPlacement` | `TrashPlacement { prevKey: Key?, nextKey: Key?, ordinal: Int }?` | restore position (SCR-39) |

- **`DirectionCue`** = `{ key, kind: background · bgm · sfx · cg · screenEffect, assetKey: Key? (→ ScriptAsset; required except screenEffect), effect: String? }`.
- **`ChoiceOption`** = `{ key, order, runs: List<TextRun>, speakerKey: Key?, voice: VoiceState? }`; display index `#045-1` is derived.
- **`VoiceState`** **[changed, `lastExport` is `ED`]** = `{ mode: auto · fixed, fixedId: String?, fixedAt: Instant? (first export), manualId: String?, lastExport: { at: Instant, textHash: String, speakerKey: Key? }? }`. `lastExport` replaces LIB's app-only `ExportRecord` ledger so the CSV `바뀜` state is the same on every device (SCR Q-27 stays open for the exact meaning; this only stores the inputs). There is no `DISCARDED` mode: a discarded ID is a `DiscardedVoiceId` row.
- **`DiscardedVoiceId`** (`버린 ID`) · P · in the numbering unit file = `{ id: String, blockKey, optionKey?, discardedAt: Instant, reason: deleted · kindChanged · merged · replaced · formatRefixed }`. Never reissued; exported as CSV `버림` rows.

**`ScriptAsset`** **[renamed]** (`에셋`) · P · `assets/index.json › scriptAssets` — `{ key, kind: background · bgm · sfx · cg, name, fileName (engine file name), fileHash: String? (→ Blob; image for background/cg, audio for bgm/sfx; whether audio is packaged is SCR Q-29), durationMs: Long?, loop: Boolean, placeKey: Key? }`. Use counts, first-use episode and `안 쓰는 것` are derived.

**`ScriptViewState`** · A · per project: `layers {direction, memo, system, marks, addressHint, paren, termCandidates}`, `preview: write · dialogueOnly · readAloud`, `panel {open, tab, scroll}`, `moduleCardOrder`, `recentSpeakers`, `restore {unitKey, selectedBlockKey?, caretOffset?, scrollAnchor}`. (Last writing position and daily counts moved to §2.13; §10 C-18, C-19.)

### 2.4 Change records (`◆` and `◇ 첫 공개`) — envelope and order: STO-12 · anchor relocation: SCR-43/44 · targets/values: CST, WLD

**`ChangeRecord`** · P · `changes.json`

| Field | Type | Meaning |
| --- | --- | --- |
| `key` | Key | |
| `target` | `ChangeTarget` | what changes (below) |
| `value` | JSON? | the value from the anchor on; schema by target kind (below) |
| `note` | String? | history note (`처음 만남`) |
| `anchor` | `Anchor` | where it starts (§4.3) |
| `anchorState` | `normal · orphaned · kept` **[changed]** | `orphaned` = `근거 지워짐` (moved because its referent was deleted, will re-attach on restore); `kept` = the author chose `이대로 두기` (no re-attach; the dotted look follows SCR Q-20) |
| `originalAnchor` | Anchor? | non-null **iff** `orphaned`: the anchor before relocation |
| `createdAt` | Instant | tie-break at equal positions |
| `deletedAt`, `trashEntryKey` | Instant?, Key? | `기록 지우기` (trash filter `변화`) |

**`ChangeTarget`** (sealed; JSON `kind`):

| Kind | Fields | `value` | Owner |
| --- | --- | --- | --- |
| `field` | `entityKind: character · place · term · faction`, `entityKey`, `fieldDefKey` | a `FieldValue` body (`{state, payload?}`, §2.5) | CST (engine), WLD (cards) |
| `displayName` | `characterKey` | String, or `null` = show `이름` (reveal) | CST-11 |
| `relationSlot` | `relationKey`, `slot: between · mindA · mindB · addressA · addressB` | `SlotValue` for between/mind, String for address | CST-29/31 |
| `regionShape` | `mapKey`, `regionKey` | `{ vertices: List<[x, y]> }` | WLD-65 |
| `termReveal` | `termKey` | `null` (the anchor is the reveal point) | WLD-18 **[changed]**: was a separate `FirstReveal` entity |

Invariant: at most one **live** `termReveal` record per term (WLD-18). A term reveal may anchor on a whole choice block but never on an option line (the model has no option-level anchor at all, so the rule holds by construction; whether the whole choice block is allowed for `◇` is WLD Q-06 / SCR Q-30).

**`Anchor`** (sealed; JSON `kind`) **[changed]** — one type for every record:

| Kind | Fields | Meaning | Created by |
| --- | --- | --- | --- |
| `block` | `blockKey` | from that block, inclusive (`1-7 #043부터`) | SCR-41, term card, `블록에 잇기` |
| `unitStart` | `unitKey` (any level: chapter, episode, scene, custom) | from before the unit's first block (`1-5 #—`, `공개 예정 2장`) | profile `시점 추가`, `여기서부터 바뀜` at a past lens, canvas `관계로 만들기`, term `공개 예정` |
| `unitEnd` | `unitKey` (a scene in v1) | after the unit's last block, **including blocks added to that unit later** | `여기서부터 바뀜` at 최신 = `unitEnd(집필 선 scene)` (STO-15) |
| `worldEvent` | `eventKey` | before the story (세계 사건 자리) | world/timeline |
| `unplaced` | — | no surviving block after a deletion (SCR-43 step 4 says the record becomes `예정`; representing that as an anchor is `ED`); sorts after everything | relocation only |

### 2.5 Cast — owner CST (field engine shared with WLD cards)

**`Character`** · P · `cast.json › characters`

| Field | Type | Meaning |
| --- | --- | --- |
| `key` | Key | **[changed]** was `id` |
| `recordNo` | Int | `인물 기록 007`; from `Project.counters.nextCharacterRecordNo`, never reused or renumbered |
| `name` | String | `이름`, non-empty |
| `displayNameBase` | String? | `표시 이름` before any record; null = show `name`; history via `ChangeRecord(displayName)` |
| `oneLiner` | String? | `한 줄 소개` |
| `tagKeys` | List<Key> | ordered tags |
| `speechBody` | RichDoc | `말투` tab prose |
| `secretBody` | RichDoc | `비밀` tab prose |
| `voiceCode` | String? | speaker part of voice IDs (`HRT`); where it is edited: CST Q12 |
| `hidden` | Boolean | item hiding |
| `order` | OrderKey | |
| `fields` | Map<fieldDefKey, FieldValue> | base values (before any record) of project fields |
| `deletedAt`, `trashEntryKey` | | trash (§5); merge reason/target live on the `TrashEntry` **[changed]** |

Portrait and background are `ImageUse` rows (slots `characterPortrait`, `characterBackground`), not columns **[changed]**. Derived, never stored: age, `첫 등장`, `보이스 진행`, display-name ranges, reference counts.

**`Tag`** · P · `project.json › tags` — `{ key, name (unique per project), order }`.

**`FieldDef`** · P · `project.json › fieldDefs` — `{ key, entityKind: character · place · term · faction, name, exportName?, type: shortText · longText · number · choice · multiChoice · date · anniversary · link · calc, order, inHeader, inList, inFilter, isSecret, definitionHidden **[renamed from hidden]**, scopeCharacterKey?, shortTextMultiple?, unit?, options?: List<FieldOption { key, label, order }>, allowFreeText?, datePrecision?: year · ymd, linkTargetKind?: faction · place · term, calc?: { formula: age, sourceFieldDefKey }, bundleOrigin?, deletedAt?, trashEntryKey? }`. `inFilter` only for choice · multiChoice · link.

**`FieldValue`** (embedded in its entity's `fields` map; DB table keyed by `(entityKey, fieldDefKey)`):

| Field | Type | Meaning |
| --- | --- | --- |
| `state` | `unknown · value · fallbackText` | `unknown` = `??`; *empty* = no entry (never stored) |
| `payload` | sealed `texts: List<String>` · `rich: RichDoc` · `number: String` · `choice: { optionKeys, freeText? }` · `date: { year, month?, day? }` · `anniversary: { month, day }` · `link: { targetKeys }` | present iff `state = value` |
| `fallbackText` | String? | iff `fallbackText` |
| `fallbackReason` | `optionRemoved · conversionFailed ?` | `◇ 보기에 없는 값` / `◇ 바꾸지 못한 값` |

`calc` fields have no value unless the age is typed directly (then `number`).

**`Relation`** · P · `cast.json › relations` — `{ key, endA: EntityRef, endB: EntityRef, between: SlotValue?, mindA: SlotValue?, mindB: SlotValue?, addressA: String?, addressB: String? }`; `EntityRef = { kind: character · faction, key }`. One relation per unordered pair: unique index on `(min(endA, endB), max(endA, endB))` compared as `kind + ":" + key`. `SlotValue = { kindKey: Key?, description: String?, pairFirstHolder: a · b ?, kindFallbackText: String? }`. Before the first record of any slot the relation reads `만나기 전` (derived). FACTION ends are storage-only in v1 (CST-36).

**`RelationKind`** · P · `project.json › relationKinds` — `{ key, category: between · mind, symmetry: symmetric · pair (between only), name, pairName?, order }`. Seed list = rule 13 §3 (CST Q1 default). Deleting a kind is permanent and leaves `kindFallbackText` on slots that used it (rule 13 §3), not a trash entry.

**`VoiceLine`** · P · `cast.json › voiceLines` — `{ key, characterKey, order, title, text: RichDoc, status: writing · done }` (`비어 있음` derived from empty text).

**`ArchiveDoc`** · P · `cast.json › archiveDocs` — `{ key, characterKey, order, title, body: RichDoc, unlockPoint: PointRef? }` (`1-7 이후 열림` = `unitEnd(1-7)`, §4.6).

### 2.6 World — owner WLD

**`Term`** · P · `world.json › terms` — `{ key, order, name, aliases: List<String>, description: RichDoc, related: List<ItemRef>, textSearch: Boolean = true, hidden, fields, rejections: List<TermRejection> }`; `ItemRef = { kind: term · place · faction, key }`. Its `◇ 첫 공개` is a `ChangeRecord(termReveal)` in `changes.json` **[changed]**.

**`TermRejection`** (`아님`, embedded in its term) — `{ key, containerKind: block · sceneTransition, containerKey (block key or scene key), optionKey: Key? **[changed from optionIndex]**, matchedText (NFC), occurrence: Int (0-based among same-text candidates of this term in the container), createdAt }`.

**`Place`** · P · `world.json › places` — `{ key, order, name, description: RichDoc, parentPlaceKey: Key? (지역/소속, WLD Q-03), hidden, fields }`.

**`Faction`** · P · `world.json › factions` — `{ key, order, name, description: RichDoc, hidden, fields }`.

Card images: `ImageUse` slot `cardReference` (16:9).

**`TimelineEvent`** · P · `world.json › events` — `{ key, order (tie-break at equal time), kind: world · story, title, description: RichDoc, worldDate: { year, month?, day? }? (world only), sceneKey? (story only), place: PlaceRef?, relatedTermKeys, revealPoint: PointRef? }`. **No stored `hidden`** **[changed]**: an event is shown masked when an item it links is hidden (WLD's `[PROPOSED]` meaning), which is derived.

### 2.7 Maps — owner WLD (`maps/<mapKey>.json`, one file per map)

**`WorldMap`** · P — `{ key, order, name, parent: { mapKey, pinKey }? **[only link]**, baseKind: generated · image · sketch · node, baseImageHash?, baseSize: { w, h }?, sketch: { photoHash, corners: [4 × Point] }?, format: hex · poly · detail · outline · node ?, style: soft · ink · color · parchment ?, contours: Boolean, scale: MapScale?, revealViewDim: Boolean, layers, pins, regions, routes, labels, stamps, drawnLines, hex?, poly?, nodes, nodeLinks, generation? }`. A pin's child map is derived from `WorldMap.parent` **[changed]** (WLD also stored `Pin.childMapKey`).

All coordinates are `Float` in [0, 1] of the base (x right, y down); `Point = [x, y]`. Overlay rows (each `{ key, …, layerKey?, revealPoint: PointRef? }`):

- `MapLayer { key, order, name, visible, revealPoint? }`
- `Pin { key, placeKey, x, y, layerKey?, revealPoint? }` — unique `(mapKey, placeKey)`
- `MapRegion { key (project-unique), name, kind: String (Q-17), factionKey?, placeKey?, color, layerKey?, revealPoint?, vertices: List<Point> (≥ 3, base shape; history via ChangeRecord(regionShape)) }`
- `Route { key, points: List<{x, y} | {pinKey}>, layerKey?, revealPoint?, shownModeKey? }`
- `MapLabel { key, text, x, y, curve: List<Point>?, layerKey?, revealPoint? }`
- `Stamp { key, kind: mountain · hill · forest · city · town · castle · ruin · tower · bridge · port · cave · marker, x, y, size: 1|2|3, rotation (multiple of 15), placeKey?, layerKey?, revealPoint?, attachedLayer: river · road · none }`
- `DrawnLine { key, kind: river · road, points, origin: generated · author, editedByAuthor }`
- NodeMap: `NodeMapNode { key, name, sub?, placeKey?, x, y, revealPoint? }`, `NodeMapLink { key, aNodeKey, bNodeKey, label?, revealPoint? }`
- `MapScale { a: Point, b: Point, value: Double, unit: day · km · li · custom, travelModeKey? }`
- `HexGrid { cellKm, orientation: pointy · flat, cols, rows, coordEvery, cells: List<Terrain>, overrides: Map<cellIndex, Terrain> }`, `Terrain = deep · sea · plain · forest · hill · mountain · peak · desert · swamp · snow`
- `PolyMap { cells: List<{ site: Point, land: Boolean }>, cellProvince: Map<cellIndex, Key>, provinces: List<PolyProvince { key, name, color, factionKey? }> }`
- `GeneratedMapRecord { generatorVersion, kind: continent · twin · islands · region, landRatio: seaMore · half · landMore, steps: List<{ step: coast · mountain · river · forestClimate · townRoad, seed: Int, locked, choices: Map<String, String> }>, result: { coastline, heights?, hexCells?, provinces?, rivers, towns, roads, stamps }, history: List<{ createdAt, steps, resultRef }>, conditions: List<TextCondition>?, textModel: { name, version, onDevice }? }` — the result is stored, not only the seed (FMT §4).
- `TextCondition { key, what, where: Set<Int 0..8>, traits: List<String>, placeKey?, status: applied · needsCheck, userPosition: Set<Int>?, sourceSpan: { start, end }? }`

### 2.8 Canvas and pages — owner CNV

**`Canvas`** · P · `canvas/<key>.json` — `{ key, order, name, createdAt, nodes, edges }`. Viewport and `lastOpenedAt`: A.

- `CanvasNode { key, kind: character · faction · event · term · memo · group, refKey? (required for item kinds), text? (memo text / group title), x, y: Float (dp at zoom 1), w?, h? (group), groupKey?, z: OrderKey }` — an item node is a **reference** (rule 20: stays, dotted, when the item is trashed).
- `CanvasEdge { key, fromNodeKey, toNodeKey, labelText?, relationKey? }` — `relationKey` only when both nodes are `character`; relation data lives in `cast.json`/`changes.json`, never in the canvas file.

**`Page`** · P · `pages/<key>.json` — `{ key, order, name, sectionLabel? (Q-PG-03), blocks: RichDoc **[changed]**, hidden, sourceRecord: RecorderSource?, createdAt, deletedAt?, trashEntryKey? }`. Header image = `ImageUse` slot `pageHeader`. `연결 N` is derived (Q-PG-04).

### 2.9 Checks — registry and semantics: STO-40/41 · shell: CNV-17

**`Acknowledgement`** **[merged]** · P · `checks.json › acknowledgements` — one entity for `맞음` and `이대로 두기` everywhere (replaces CNV `CheckAck`, CST `RefAck`, WLD's link-keyed `맞음`).

| Field | Type | Meaning |
| --- | --- | --- |
| `key` | Key | |
| `checkType` | String | registry id (below) |
| `subjectKeys` | List<Key> | keys the item is about (block, scene, character, record, linkKey …) |
| `fingerprint` | String | hash of the inputs that made the check fire; a different current fingerprint voids the ack (STO-41) |
| `kind` | `match · keep` | `맞음` · `이대로 두기` |
| `createdAt` | Instant | |

Lifecycle (STO-41, rule 12 §1): an ack follows its subjects by key, so it moves with a moved block. When any subject stops being live (direct or derived delete), the ack is deleted; that delete's undo restores it, but restoring the subject later from the trash does **not** (the item is re-evaluated fresh, STO-41 `ED`). **Exception:** acks of `deleted.item` (and the `keep` of a `대상 지워짐` record, §5.4) are *about* the deleted item, so they stay active while it is in the trash and after it is purged (the row then reads `영구 삭제 · <name>`, CST-42); they are deleted when the item is restored or when no reference to it remains. `되돌리기` in `맞다고 표시한 것` deletes an ack. A `fingerprint` that no longer matches voids it (deleted on next evaluation).

**Check type ids** (STO-40 is the registry; CNV-17 renders it):

| # | `checkType` | Ackable as | Subject |
| --- | --- | --- | --- |
| 1 | `term.beforeReveal` | match | `[linkKey]` |
| 2 | `term.candidateBeforeReveal` | — (`아님` = `TermRejection`) | — |
| 3 | `term.noReveal` | — | `[termKey]` |
| 4 | `speaker.missing` | — | `[blockKey]` |
| 5 | `voice.empty` | — | `[characterKey]` |
| 6 | `voice.formatDiffers` | match (`그대로`) | `[blockKey]` or `[blockKey, optionKey]` |
| 7 | `voice.duplicate` | — | `[blockKey…]` |
| 8 | `change.anchorDeleted` | keep (`이대로 두기`, also sets `anchorState = kept`) | `[recordKey]` |
| 9 | `deleted.item` | keep | `[itemKey]` (or `[itemKey, referencingKey]` for one reference, e.g. a `대상 지워짐` record) |
| 10 | `travel.shortage` | match | `[characterKey, fromSceneKey, toSceneKey]` |
| 11 | `travel.sameTimeTwoPlaces` | match | as 10 |
| 12 | `travel.simultaneous` | match | as 10 |
| 13 | `travel.noPlace` | — | — |
| 14 | `travel.noScale` | — | — |
| 15 | `time.clockBandMismatch` | match | `[sceneKey]` |
| 16 | `time.order` | match | `[sceneKey, previousSceneKey]` |
| 17 | `transition.stale` | match | `[sceneKey]` |
| 18 | `transition.placeBeforeReveal` | match | `[sceneKey]` |
| 19 | `hidden.characterInVisibleScene` | match (`그대로`) | `[sceneKey, characterKey]` |
| 20 | `hidden.lockedGroup` | — (aggregate row) | — |

Check results are never stored.

### 2.10 Images and files — owner CNV (script assets: SCR)

**`Blob`** **[renamed from ImageAsset]** · P · `assets/index.json › blobs` + `assets/<hash>.<ext>` — `{ hash (lowercase hex SHA-256 of the bytes), ext, mime, bytes: Long, width?, height?, durationMs?, source: gallery · file · sketch · generated · recorder, importedAt }`. Stored once per content (one global blob store on the device; the app DB records which projects imported it, `blob_owner`, §9, `ED`); garbage-collected when no live or trashed row references it (§5.3). In a file, `blobs` lists exactly the blobs the exported rows reference.

**`ImageUse`** · P · `assets/index.json › imageUses` — `{ key, ownerKind: project · character · place · term · faction · page, ownerKey, slot: projectCover · characterPortrait · characterBackground · cardReference · pageHeader, source: "preset:<id>" | blob hash, crops: Map<Ratio, Crop>, holo: Boolean }`; unique `(ownerKind, ownerKey, slot)`. `Ratio = r3x4 · r1x1 · r4x5 · r9x16 · r4x3 · r16x9 · r21x9`; `Crop = { x, y, w, h: Int (source px after rotation), rotation: 0 · 90 · 180 · 270, flipH: Boolean, zoom: Float ≥ 1 }`. Slot ratios and holo permission: CNV-25. Owners have **no image columns** **[changed]**.

`PresetImage` · C — static registry (`ash-dawn` …, CNV-26); stored only as the name `preset:<id>`, never removed from the app.

### 2.11 Deletion records — owner LIB (trash host), rules: CST-40…48, SCR-39/43

**`TrashEntry`** · A (and `trash/index.json` with `휴지통도 포함`) — `{ key, kind, projectKey?, targetKeys: List<Key>, groupSceneKey?, groupDate: LocalDate? (block groups per scene per day), reason: deleted · merged · replacedByOverwrite, mergedIntoKey?, hiddenAtDelete: Boolean, deletedAt, purgeOn: LocalDate }`. `kind = project · scene · blockGroup · character · place · term · faction · change · recorder · fieldDef · page · map · canvas · event` (**[changed]**: the last four are added, §10 C-20).

**`NameTag`** **[merged]** (`이름표`) · P · `tombstones.json › nameTags` — `{ key (the deleted item's key; references keep pointing at it), kind: character · place · term · faction · unit · page, name, voiceCode: String?, hidden: Boolean, state: purged · notExported, purgedAt: Instant? }`. `state = notExported` (`ED`) is written for items that are in the trash on the exporting device but referenced by exported live data (§5.6).

**`MergeRecord`** · P · `tombstones.json › mergeRecords` — `{ key, kind, mergedKey, targetKey, at, movedRefs: List<{ ownerKind, ownerKey, field }>, filledFieldDefKeys, chosenFromMerged: List<{ fieldDefKey } | { relationKey, slot }>, deletedMutualRelationKey?, mergedOriginal: JSON snapshot }` (CST-45). Used for undo and for restoring the merged item.

**`DeletionLogEntry`** · P · `deletions.json` — `{ key, kind, deletedAt }`, kept 30 days (FMT §7, future sync).

### 2.12 Recorder — owner LRC (library outside projects)

**`RecorderRecord`** **[renamed from LrRecord]** · R

| Field | Type | Meaning |
| --- | --- | --- |
| `key` | Key | global |
| `kind` | `voice · text` | |
| `createdAt` | Instant | recording start; copied into source marks |
| `runs` | List<TextRun> | text (link targets carry `projectKey`) |
| `textRevision` | Int ≥ 1 | +1 on every committed edit |
| `durationMs` | Long? | voice only, pauses excluded |
| `audio` | `{ hash (→ Blob), mime, bytes, durationMs, envelope: ByteArray }?` | null when not kept |
| `transcribedBy` | `onDevice · cloud ?` | |
| `doneMarked` | Boolean | `끝냄` |
| `hidden` | Boolean | hidden record (own gate, LRC-41) |
| `deletedAt`, `trashEntryKey` | | trash (filter `레코더`) |

State `정리 안 됨 · 넣음 · 끝냄` is derived (LRC §3.3).

**`RecorderLink`** (`연결`) · R — `{ key, recordKey, projectKey, targetKind: project · unit · character · place · term · faction · page **[changed: unit replaces chapter · episode · scene]**, targetKey, origin: entry · manual · at · insert · startMaterial, createdAt, lastLabel: String? **[renamed from nameTag]**, purgedAt?, lastLabelHidden: Boolean }`; unique `(recordKey, projectKey, targetKind, targetKey)`. Implied project links are derived, never stored.

**`Placement`** (`넣은 곳`) · R — `{ key, recordKey, projectKey, kind: scriptBlock · cardParagraph · page · memoField · character, targetKey (block key · DocBlock key · page key · unit key · character key), containerKey? (scene key for a block; entity key for a paragraph), tabId? (fieldDefKey or `speech` · `secret` · `overview` · `description`), copiedRevision: Int, createdAt, lastLabel: String, purgedAt? }`.

**`RecorderSettings`** · S — `{ transcription: onDevice · cloud, lockScreenRecording, keepAudio, presetLink }`; the glow switch is `AppSettings.recorderGlow`.

Runtime and drafts (RT/A): `EntryContext`, `RecordingSession` (crash-recovery file), `TextMemoDraft`, `InboxFilterState`, `UnlockSession.recordsUnlockedAt`.

### 2.13 App-level state — owners as named

| Entity | Class | Fields | Owner |
| --- | --- | --- | --- |
| `AppSettings` **[merged]** | S | `theme: system · light · dark`, `textSizeStep`, `unfoldedLayout: listDetail · centerColumn`, `animation: followSystem · alwaysReduce · neverReduce`, `reduceTransparency`, `recorderGlow`, `holoDefaultForNewImages`, `viewByScreen: Map<ScreenId, list · card>`, `appLock`, `relockPolicy: onLeave · screenOffOnly · fiveMinutes`, `hiddenInSearchWhileOpen`, `hiddenOutOfWidgetsRecents`, `backupHidden`, `driveBackup`, `driveAccountId?`, `firstUseTips`, `defaultSceneNotation`, `checkTravel`, `checkNoPlace`, `defaultMapStyle`, `textMapEngine: onDevice · cloud`, `largeMapExport: k4 · k8`, `recorder: RecorderSettings`, `ratingDefaults: RatingDefaults` | LIB container; groups from CNV-46, STO-42, WLD-67, LRC-44 |
| `RatingDefaults` | S | `r18Hide, r18Capture, r18NameMask, r18RecorderMask` (true), `r15Capture` (false), `exportCoverRating` (true), `changedGroups: Set<Rating>` | LIB-23 |
| `OnboardingState` | A | `firstRunCompleted, resumePage, walkthroughSeen, exampleState: notInstalled · installed · deleted, moduleTourSeen, tipsShown, emptyLibraryNoticeShown` | LIB |
| `StartChecklist` | A | per project `{ dismissed }`; items complete by fact (LIB-13) | LIB |
| `ProjectActivity` **[merged]** | A | per project `{ lastEditedBlockKey?, lastEditedOffset?, lastEditedAt?, lastOpenedAt?, lastRoute? }` — serves `이어 쓰기` (STO-01), `마지막으로 쓰던 곳` (SCR-59, LRC-33) and `열기 · 마지막 위치` (LIB-02) | STO |
| `DayBaseline` **[kept, replaces SCR dailyTally]** | A | `{ projectId, localDate, blockKey, lengthAtDayStart }` | STO-06 |
| `LensState` | A | per project `{ point: latest · unitEnd(unitKey), savedForScript? }` | STO-13/16 |
| `RecentItem` | A | `{ key, projectId, kind: scene · character · page · term · place · faction, targetKey, openedAt }`, max 20 | LIB-05 |
| `UndoStep` | A | opaque per area | each area |
| `ExportRecord` | A | `{ projectId, exportedAt, format, rangeKeys }` (UI history only; CSV inputs moved to `VoiceState.lastExport`) | LIB |
| `BackupRun`, `BackupEntry`, `BackupState` | A | LIB §3 | LIB-57…61 |
| `RecoveryKeyMaterial` | A | `kdf, iterations, salt, dataKeyWrappedByPassword, dataKeyWrappedByKeystore, keystoreAlias, setAt` (never the password) | LIB-58 |
| `UnknownFile` | A | `{ projectId, path, bytes, sha256 }` — files of an imported package this app did not understand, written back on re-export (§8.3) | LIB-50 |
| `SavedUiState` | A + saved instance state | route + args, selection, panel, scroll, caret, open sheet, map/canvas viewport | CNV-35 |
| `ExportOptions` | RT | the export sheet's transient choices (LIB §3.2) | LIB-41 |
| `UnlockSession` | RT | `appUnlocked, hiddenGroupUnlockedAt?, itemsUnlocked: Map<projectId, Instant>, recordsUnlockedAt?, lastBackgroundAt?, lastScreenOffAt?` | LIB-32/33, LRC-41 |
| Canvas viewport, map viewport, remembered world tab | A | — | CNV, WLD |

**(0.2.0)** `AppSettings` gains `libraryStyle`, `castCardStyle`, `libraryToggle`, `castToggle` and `viewTilt`; `viewByScreen` keys `library` and `cast` are kept but no longer read (§13.4). `ProjectActivity` gains `lastCanvasKey` (§13.6). `RatingDefaults` is kept but unused (§13.3).

### 2.14 Code-only values and derived values

- **C (code-only, `core/designsystem` and registries):** `CivilightColors`, `FixedColors`, `CivilightType`, `TextScale`, spacing/shape/size/blur/elevation tokens, `CivilightMotion`, `HapticToken`, `MarkerKind`, `MarkerLine`, `InlineDecoration`, `BadgeSurface`, `ButtonVariant`, `LensChipState`, `IconId` (DS §3), `PresetImage` registry, check-type registry, feature-id → Korean name table (LIB-53). Nothing from DS is stored.
- **Derived, never stored** (FMT §4): codes and ordinals (`1-7`, `씬 4`, `#045-1` index), scene head text, transition ID (from the scene key), automatic transition text, automatic voice IDs, `ResolvedTime` and every time label, time order, 집필 선, `예정`, lens values, statuses and progress, character counts, `첫 등장`, `보이스 진행`, ages, relation `만나기 전`, display-name ranges, usage lists (`쓰인 곳`), candidates and the candidate index, `▲` lists, check results, reference counts, `연결 N`, recorder states and counts, distances and durations, travel verdicts, liveness (§5.1), reference resolution (§3). Exceptions that are stored because they are author decisions or history: `Acknowledgement`, `TermRejection`, `VoiceState.fixedId`/`lastExport`, `transitionSnapshot`, generated map results.

## 3. Resolving a reference (rules 19 and 20 as data)

Every key reference (speaker, `@` link, scene place, field `연결` value, term `관련`, canvas item node, region/stamp/asset place link, recorder link, silent cast …) is resolved at read time, never rewritten by a deletion. `resolve(ref)` returns the first case that holds:

| # | Case | Condition | Presentation contract |
| --- | --- | --- | --- |
| 1 | `HIDDEN_LOCKED` | the target (or its project) is hidden and that gate is locked (LIB-36, LRC-22) | `숨긴 인물` / `숨긴 항목` / `숨긴 프로젝트`; never the name. **Hiding wins over deletion** (rule 20 edge row): this case is checked first, also for trashed and name-tagged targets (`NameTag.hidden`) |
| 2 | `LIVE` | the row exists and `isLive` (§5.1) | normal |
| 3 | `TRASHED` | the row exists, not live | dotted + `◇`, `지운 <kind> · <name>`; fix paths incl. `되살리기` (CST-41) |
| 4 | `TAGGED` | no row, a `NameTag` with that key | `state = purged`: `영구 삭제 · <name>`; `state = notExported`: `지운 <kind> · <name>` without `되살리기` (§5.6) |
| 5 | `MISSING` | neither | dotted, label copy not in sources (`TODO_COPY_DM_01`); never crashes, never becomes `화자 없음` |

- A dialogue block whose speaker resolves to 3–5 keeps the speaker (never `▲ 화자 없음`); exports use the name from the row or the tag (rule 20 §2). Automatic voice IDs use the voice code from the row or `NameTag.voiceCode`.
- Plain text is never a reference and is never changed or masked (rules 19 §6, 20 §2).
- References inside non-live containers (blocks of a trashed scene, a trashed page …) are not counted and not checked (CST-41).
- **`대상 지워짐`** (TC-20-23, SCR-57): a `ChangeRecord` whose *target* item is not live is itself not live (no effect on `valueAt`, lens or lists), but its anchor still resolves and the anchor block shows a dotted mark `대상 지워짐` with `되살리기 · 이대로 두기 · 기록 지우기`. `되살리기` restores the target item; `이대로 두기` stores `Acknowledgement(deleted.item, keep, [itemKey, recordKey])`; `기록 지우기` gives the record its own trash mark (`change` entry). This state is independent of `anchorState`, which is about the anchor's referent, not the target.

## 4. Story position (rule 11)

### 4.1 Levels, episode level, numbering unit

`Project.structure.levels` has n = 1…4 names. The **scene level** is `n − 1`. With n ≥ 2 the **episode level** is `n − 2` (STO-02). The **numbering unit** of a scene is its ancestor at the episode level; with n = 1 it is the project itself. The numbering unit:

- issues block numbers: `number` is unique among the unit's blocks, taken from `StoryUnit.nextBlockNumber` (or `Project.counters.nextBlockNumber` when n = 1), which only grows; numbers of deleted blocks are never reissued; a block moved into another numbering unit takes that unit's next number (SCR-21); renumbering (SCR-22) reassigns 1…N to live blocks in manuscript order and leaves the counter unchanged (SCR Q-07 default);
- keeps its counter in `story/index.json` (`StoryUnit.nextBlockNumber`) or `project.json` (`counters.nextBlockNumber`), **not** in the episode file (C-32): when LIB-55 imports around a damaged episode file the counter survives, so numbers of the lost blocks are never reissued. A numbering unit with no live blocks keeps its counter (1 when nothing was ever issued);
- is the file unit `story/<numbering unit key>.json` (key = the unit's key, or the project key when n = 1), written for every numbering unit that has a live scene, possibly with empty arrays, so a damaged file loses one episode only (LIB-55);
- is the episode of `N일째` (STO-22) and of voice-ID episode codes (format for other structures: SCR Q-06).

### 4.2 Manuscript order

The manuscript order (원고 순서) is a depth-first walk of the **live** units of a project, children in `order`, and inside each scene: the scene's transition text, then its live blocks in `order`. Hidden scenes are included (STO Q9 default); trashed units and blocks are not. Story time (때) never changes manuscript order.

### 4.3 Positions and comparison

A **position** is one of: `start(u)` (before everything under unit u), `transition(s)` (a scene's transition text), `block(b)`, `end(u)` (after everything under u, including blocks added to u later), `event(e)` (before the story), `unplaced` (after everything). Positions are compared through ranks of an Euler-tour index (`ManuscriptIndex`, derived, cached in memory per project, rebuilt when a unit or block is inserted, moved, trashed or restored):

```
rank = 0
visit(u):                       // units of the project in order, recursively
    start[u] = rank++
    if u is a scene:
        transition[u] = rank++
        for b in liveBlocks(u) by order: rank[b] = rank++
    for c in liveChildren(u) by order: visit(c)
    end[u] = rank++
eventRank(e) = −1 − (index of e in events sorted descending by (worldDate.year, month ?: 0, day ?: 0, order, key))
unplaced = +∞
```

Equivalent sort key (STO §3): `[o_1 … o_k, tail]` with the unit path's order keys and `tail ∈ {start < transition < block order key < end}`; a shorter path at `start` sorts before every position under it and at `end` after every position under it.

- **Anchor position:** `block(b)` → `rank[b]`; `unitStart(u)` → `start[u]`; `unitEnd(u)` → `end[u]`; `worldEvent(e)` → `eventRank(e)`; `unplaced` → `+∞`.
- **Granularity is the block** (WLD-20): a link's position is its block's position (a choice option line counts as its choice block; transition runs as `transition[s]`); character offsets are never compared. "From X" includes X: a link inside the reveal block counts as after the reveal.
- `unitEnd(u)` sorts after blocks added to u later (STO-15 reading; §10 C-04). Consequence: a `여기서부터 바뀜` record made at 최신 is applied at 최신 at once, and no block of that scene sees it, including blocks written into that scene afterwards; blocks of later scenes do.

### 4.4 집필 선, lens points, 예정

- **집필 선** = `end(S*)`, S* = the last scene in manuscript order that has ≥ 1 live block with non-empty content of any kind (STO-12; hidden scenes count). No such scene → no 집필 선.
- **Lens point** `LensPoint = latest | unitEnd(u)` (episode or scene; STO's `EpisodeEnd`/`SceneEnd` are `unitEnd`). `latest` = the 집필 선, or `+∞` (everything applies) when there is none.
- A live record **applies at P** iff `pos(anchor) ≤ pos(P)`. It is **예정** iff a 집필 선 exists and `pos(anchor) > pos(집필 선)` (so `unplaced` records are always 예정 while a 집필 선 exists).
- **Script panel** for block B (the script has no lens): state = records with `pos(anchor) < rank[B]`; the `여기서` rows = records with `pos(anchor) = rank[B]`.
- `렌즈를 이 씬으로 맞추기` stores `unitEnd(scene)` into `LensState.savedForScript` (STO-16).

### 4.5 `valueAt`

`valueAt(target, P)` = `value` of the live record of that target with the greatest `pos(anchor) ≤ pos(P)`; ties → later `createdAt`. No such record → the base value (`Character.fields`, `displayNameBase`, `Relation` base slots, `MapRegion.vertices`), or `만나기 전` for an empty relation slot (rule 13). Calculated fields have no history. Term reveal: `revealPos(term)` = `pos(anchor)` of its live `termReveal` record; a link L of that term is `▲ 공개 전 사용` iff `pos(L) < revealPos`; no reveal record → no `▲` (WLD-20).

### 4.6 `PointRef` (reveal and unlock points)

`PointRef = unitStart(unitKey) | unitEnd(unitKey)`, used by map `보이는 시점` (`X부터` = `unitStart`), `TimelineEvent.revealPoint` (`unitStart`) and `ArchiveDoc.unlockPoint` (`X 이후 열림` = `unitEnd`). An item is visible at lens P iff `pos(point) ≤ pos(P)` (WLD-49). Unlike anchors, a `PointRef` whose unit is not live is **not rewritten**: it resolves at read time to the start of the next live unit in manuscript order, or `+∞` if none, and goes back to normal when the unit is restored (`ED`; no source covers deleting a unit that a reveal point names).

### 4.7 Story time is a separate axis

`SceneTime` (§2.2) resolves to `ResolvedTime` (STO-22: shared-variable day expressions, `t` ranges, labels). `compareByTime` orders scenes for 동선, `같은 때 두 장소`, `동시에 두 장소` and the 연표; manuscript order (§4.3) is used for the lens, `◆`, `◇`, `▲`, 집필 선 and every rule-11/12/21 comparison. The two never substitute for each other ("Story time is never consulted", WLD-20).

## 5. Deletion, trash and cascade (rules 12 and 20)

### 5.1 Direct marks and derived liveness

- Only the row the user deleted gets `deletedAt` + `trashEntryKey` (LIB §3.3 contract) and one `TrashEntry`. Owned rows are **not copied or marked**: they stop being live because their owner is not live. This is what makes "goes to the trash with the item" and "comes back when both ends are back" exact.
- `isLive(row) = row.deletedAt == null && every owner of the row is live`. Owners:

| Row | Owners (all must be live) | Notes |
| --- | --- | --- |
| `Project` | — | |
| `StoryUnit` | parent unit | |
| `Block` | scene | |
| `ChangeRecord` | the target's entity: character / place / term / faction / relation / map region / field def | the **anchor never makes a record non-live** (it relocates, §5.5) |
| `FieldValue` | entity, `FieldDef` | |
| `Relation` | both ends | TC-20-08 |
| `VoiceLine`, `ArchiveDoc` | character | |
| `ImageUse` | owner | |
| `Pin` | place **and** map | TC-20-24 |
| other map overlays (`MapRegion`, `Route`, `MapLabel`, `Stamp`, `DrawnLine`, nodes, node links, layers) | map | their `placeKey`/`factionKey` are references |
| `WorldMap` | — | `parent.pinKey` is a reference (sub-map stays when the place goes, CST-47) |
| `CanvasNode`, `CanvasEdge` | canvas | `refKey`, `relationKey` are references |
| `TermRejection` | term | |
| `Acknowledgement` | — | deleted when a subject stops being live (undo restores it; trash restore re-evaluates fresh); `deleted.item` acks are exempt (§2.9) |
| `TravelSegmentOverride` | — | inactive while a scene is not live; deleted on purge |
| `TimelineEvent`, `Page`, `Canvas`, `ScriptAsset`, `Tag`, `RelationKind` | — | |
| `RecorderLink`, `Placement` | record | targets are references; links never go to the trash (LRC-43) |
| `DiscardedVoiceId`, `NameTag`, `MergeRecord`, `DeletionLogEntry` | — | history |

### 5.2 What each delete does

| Deleted thing | Direct mark · `TrashEntry.kind` | Not live by derivation | References left dotted (§3) | Also |
| --- | --- | --- | --- | --- |
| Non-empty block(s) | each block · one `blockGroup` per scene per local day | — | — | anchors relocate (§5.5); fixed voice ID → `DiscardedVoiceId(deleted)`; `trashPlacement` set |
| Empty block | none: hard-deleted, no entry, no toast (SCR-38) | — | — | — |
| Scene | scene · `scene` | its blocks | silent cast unaffected; recorder links show `휴지통` | anchors in/at the scene relocate to the next scene; scenes based on it rebase (STO-10) |
| Character · place · term · faction | item · own kind (filter `인물` / `세계`) | field values, `◆` records about it, images, relations (char/faction), voice lines, archive docs, pins (place), term reveal + rejections (term) | speakers, `@` links, scene place, silent cast, `연결` values, `관련`, canvas nodes, region/stamp/asset links, route ends, sub-map parent | `▲` of a deleted term are held, not removed (CST-47) |
| Field definition | def · `fieldDef` (filter `인물`) | its values and records | — | |
| Change record (`기록 지우기`) | record · `change` (filter `변화`) | — | — | |
| Map | map · `map` (filter `세계`) **[added]** | all its overlays incl. pins | child maps keep `parent` (breadcrumb dotted) | |
| Timeline event | event · `event` (filter `세계`) **[added]** | — | `worldEvent` anchors, flashback `eventKey`, canvas nodes | |
| Page | page · `page` (filter `전체` only) **[added; Q-PG-08 open]** | its `ImageUse` | `@` links to it, recorder links | |
| Canvas | canvas · `canvas` (filter `전체` only) **[added; Q-CV-01 open]** | nodes, edges | — | |
| Recorder record | record · `recorder` | links, placements | copies keep text, mark `레코더 · 휴지통` | |
| Project | project · `project` | everything in it | recorder links show `휴지통` | |

Merge (CST-45, WLD-15): references are rewritten to the target key (recorded in `MergeRecord.movedRefs`), chosen values copied, the mutual relation deleted, then the merged item is trashed with `reason = merged`, `mergedIntoKey = target`.

### 5.3 Restore, purge, blobs

- **Restore** (`되살리기`, undo): clear `deletedAt`/`trashEntryKey` on the direct row, delete the `TrashEntry`; everything derived comes back; references re-resolve by key, so a reference the author changed meanwhile simply holds another key and stays changed (CST-43). Hidden things need authentication first (LIB-38). Blocks return between `trashPlacement.prevKey`/`nextKey`, else at `ordinal`, else (scene not live) the user picks `씬도 되살리기` · `다른 씬 끝에` (SCR-39). A restored block's fixed voice ID: its `DiscardedVoiceId(deleted)` row is removed. Orphaned records whose `originalAnchor` points at the restored block or unit re-attach (§5.5). A merged item returns as a separate item from `MergeRecord.mergedOriginal`, with no relations and no moved references (TC-20-19).
- **Purge** (30 days via daily job, `영구 삭제`, `비우기`): delete the direct row and, recursively, every row whose liveness depends on it (§5.1 table); delete travel overrides whose scenes no longer exist (acknowledgements already went when their subject stopped being live, except `deleted.item` acks, which stay while references to the purged item remain, §2.9); write a `NameTag(state = purged)` for a character · place · term · faction (with `voiceCode`, `hidden`); recorder links/placements to it get `purgedAt` and keep `lastLabel`; add a `DeletionLogEntry`; then garbage-collect `Blob`s no row (live or trashed) references.
- Records left `orphaned` whose original block was purged stay where they are and stay dotted until the author resolves them (SCR-39).

### 5.4 Walk-throughs (must hold)

| Case | Data path |
| --- | --- |
| TC-20-08 · 노아 and 오웬 both deleted, then restored one by one | only 노아 and 오웬 carry marks. The relation is not live while either end is not live. Restore 노아 → relation still not live (오웬); 노아's tab shows 오웬 dotted (`TRASHED`). Restore 오웬 → relation live again. |
| TC-20-09 · 노아 and scene 1-6 씬 5 both deleted | the scene's blocks are not live by containment. Restore 노아 → blocks still not live, lines invisible. Restore the scene → blocks live, `speakerKey` resolves to live 노아. |
| TC-20-19 · restore the merged 시장 꼬마 | references were rewritten to 노아 at merge time and stay; 시장 꼬마 is rebuilt from `mergedOriginal` with its original field values and no relations. |
| TC-20-24 · place deleted, then its map deleted, then the place restored | pin not live (place and map). Restore place → pin still not live (map) → place card shows `◇ 지도 없음` + `지도 되살리기`. Restore map → pin live at the same 0–1 coordinates. |
| SCR-39 · block #043 deleted alone, later its scene deleted | #043 carries its own mark (`blockGroup` entry); the scene carries one (`scene` entry); the scene's other blocks carry none. Restoring the scene brings back the other blocks; #043 stays in its block group. Restoring #043 while the scene is in the trash asks `씬도 되살리기` · `다른 씬 끝에`. |
| TC-20-23 · 노아's 소속 `◆` anchored at a 1-6 block, then 노아 deleted | the record has no mark; it is not live because its target is not live; the 1-6 block draws a dotted `◆` `대상 지워짐` (§3). Restore 노아 → solid again. Purge 노아 → the record is deleted with him (§5.3). |
| `기록 지우기` on an orphaned record | the record gets its own mark (`change` entry); restoring it brings it back still `orphaned` with its `originalAnchor`. |
| Export without `휴지통도 포함` while 노아 is in the trash and still speaks in live blocks | the file has no 노아 row but `tombstones.json` has `NameTag{노아, character, NOA, state: notExported}`; on import the lines resolve `TAGGED` and show `지운 인물 · 노아` without `되살리기`; JSON/CSV exports from the imported copy still print `노아`. |

### 5.5 Anchor relocation (SCR-43) and other block operations

- When the referent of a live record's anchor stops being live — a `block` anchor's block, a `unitStart`/`unitEnd` anchor's unit (scene deleted) — the anchor becomes `block(first live block with rank > old position)`, i.e. same scene → next scene → next episode; none → `unplaced`. `anchorState := orphaned`; `originalAnchor :=` the old anchor (if already orphaned, the first original is kept). Never moved earlier.
- Restoring the referent (or undoing the delete) sets `anchor := originalAnchor`, `anchorState := normal`, `originalAnchor := null` for every record still `orphaned` towards it. `다른 블록에 잇기` sets a new `block` anchor, `normal`. `이대로 두기` sets `kept`, clears `originalAnchor`, and stores `Acknowledgement(change.anchorDeleted, keep)`.
- `worldEvent` anchors are not relocated: a trashed event still sorts by its stored date; after purge the anchor sorts first among events and the record becomes `orphaned` with `originalAnchor` = that anchor (`ED`, no source covers deleting an event that anchors records).
- Not relocation: **merge** (Backspace) rewrites the later block's anchors to the merged block, `normal` (toast `◆ 1개 #042로`); **split** keeps anchors on the front block; **move**/**cut-paste** keep keys so anchors ride along; **copy** never copies records. Acknowledgements and `TermRejection`s key on block keys and follow the same rules.
- Recorder links and recorder-inserted memo blocks never relocate (rule 23).

### 5.6 Trash in exported files

- Without `휴지통도 포함`, non-live rows are not written. For each trashed character · place · term · faction · page · unit that live exported data references, `tombstones.json` gets a `NameTag` with `state = notExported` (name, kind, voice code, hidden flag). Existing `purged` name tags are always written.
- With `휴지통도 포함`, non-live rows are written under `trash/` (§7.9) with their marks and `trash/index.json` holds the `TrashEntry` rows; the 30-day countdown continues from `deletedAt`.
## 6. Project identity, import and duplication

- `Project.key` (portable) is **unique among all project rows of the app DB, live or trashed**. Every project row also has a local `projectId` (integer, never exported); project-scoped tables are keyed `(projectId, key)`, so two projects may hold the same item keys.
- Recorder rows point into projects by the portable key (`RecorderLink.projectKey`, `Placement.projectKey`, recorder `LinkTarget.projectKey`); resolution = the project row with that key.

| Event | Result |
| --- | --- |
| Open a file whose key is not in the DB | new `projectId`, key from the file |
| Same key exists and is live → `새 프로젝트로` (LIB-52 default) | new `projectId`, **new** key, name `<name> (2)` (next free number); item keys unchanged; the file's recorder records merge by record key and gain links to the new key (F-18) |
| Same key exists and is live → `덮어쓰기` | (`ED`) the existing project is **re-keyed** to a fresh key K′ (its rows keep their `projectId`) and moved to the trash with `reason = replacedByOverwrite`; the file is imported with key K. Recorder links to K now resolve to the imported project (it is the newer version of the same project); restoring the replaced copy later gives a separate project K′ |
| Same key exists only in the trash | (`ED`) the trashed row is re-keyed to K′ first, then the file imports with K |
| Backup restore (LIB-59) | same rules per project; the recorder library merges by record key (LRC-45) |
| Encrypted file | after decrypting, the same rules; the project lands hidden (LIB-54) |
| Example project (LIB-17) | the bundled current-version `.civilight`, imported with `isExample = readOnly = true`; `온보딩 끝내기` purges it (no trash) and removes recorder links into it (LRC-23) |

## 7. The `.civilight` package

### 7.1 Container

ZIP; entry 0 = `mimetype`, **stored** (no compression), content `application/vnd.civilight.project` (LIB-49). All other entries deflated. Every JSON file is UTF-8, NFC, objects as in §1.5. Files are independent: a reader that fails one file keeps the others (LIB-55), except `manifest.json`, `project.json`, `story/index.json`, without which nothing is imported.

### 7.2 Layout

```
<name>.civilight
├ mimetype                     application/vnd.civilight.project
├ manifest.json                §7.3
├ project.json                 Project + settings groups + fieldDefs + tags + relationKinds      (§7.4)
├ story/
│  ├ index.json                StoryUnit tree incl. scene fields                                 (§7.4)
│  └ <numbering unit key>.json blocks, discardedVoiceIds (one file per episode)                (§7.4)
├ cast.json                    characters (+ field values), relations, voiceLines, archiveDocs  (§7.5)
├ world.json                   places, terms (+ aliases, rejections), factions, events          (§7.6)
├ changes.json                 ChangeRecord ◆ and ◇ termReveal                                  (§7.7)
├ maps/<map key>.json          one WorldMap with overlays and generation record                (§7.6)
├ canvas/<canvas key>.json     one Canvas with nodes and edges                                  (§7.8)
├ pages/<page key>.json        one Page                                                         (§7.8)
├ assets/index.json            blobs, imageUses, scriptAssets                                   (§7.8)
├ assets/<hash>.<ext>          originals: images, map bases, sketch photos, optional audio
├ recorder.json                recorder records linked to this project                          (§7.10)
├ checks.json                  acknowledgements, travelOverrides                 [feature checks.ack]   (§7.7)
├ tombstones.json              nameTags, mergeRecords                            [feature tombstones]   (§7.7)
├ deletions.json               deletion log, 30 days                             [feature deletions]    (§7.7)
├ trash/…                      only with 휴지통도 포함                           [feature trash]        (§7.9)
└ preview.webp                 서재 card cover (absent for hidden projects)
```

### 7.3 Manifest and feature ids

`manifest.json` is the `FileManifest` of LIB §3.2:

```ts
manifest.json = {
  format: "civilight-project",
  formatVersion: "1.0",            // major.minor (LIB-50)
  minReaderVersion: "1.0",
  features: { required: string[], optional: string[] },
  writtenBy: "Civilight 1.0.0 (Android)",
  writtenAt: Instant,
  project: { key: Key, title: string, rating: "all" | "r15" | "r18" },
  encrypted: false,                // describes the package it sits in; an encrypted file's inner manifest says false (ED, LIB §6 Q2)
  files: { [path: string]: "sha256-<hex>" }   // every entry except mimetype and manifest.json
}
```

Feature ids written by format 1.0 (a reader refuses a file whose `required` list has an id it does not know, and skips the files of an unknown `optional` id, LIB-50/53):

| Id | Class | Covers | Present when |
| --- | --- | --- | --- |
| `story.blocks` | required | `story/*.json` block model, runs, voice state | always |
| `changes` | required | `changes.json`, anchors, `termReveal` | always |
| `time.base-interval` | required | `SceneTime` base + interval | always |
| `fields.custom` | optional | `fieldDefs`, `fields` maps | project has any field def |
| `maps.gen` | optional | `generation` inside map files (a reader without it shows the stored base image and overlays) | any generated map |
| `checks.ack` | optional | `checks.json` | any acknowledgement or travel override |
| `tombstones` | optional | `tombstones.json` | any name tag or merge record |
| `deletions` | optional | `deletions.json` | any entry in the last 30 days |
| `trash` | optional | `trash/` | `휴지통도 포함` |
| `recorder` | optional | `recorder.json` | any linked record |
| `recorder.audio` | optional | audio blobs referenced by `recorder.json` | `원본 음성 포함` |

The app keeps a feature id → Korean name table for the newer-file sheet (LIB-53).

**(0.2.0)** New optional feature `versions` (`versions/<unit key>.json`, §13.5); the format stays 1.0 (§13.1).

### 7.4 `project.json` and `story/`

```ts
project.json = {
  key, name, oneLiner?, format, rating, protections: { hidden, captureBlock, nameMask, recorderLinkMask },
  structure: { preset, levels: string[] },
  worldTime: { mode: "yearOnly" | "calendar" | "none", storyStartYear?, calendar?: {
      months: { name, days }[], weekdays: string[], storyStart?: { month, day }, storyStartWeekday? } },
  sceneTime: { notation: "episode" | "storyStart", dayBoundary: "dawn0400" | "midnight",
               bandStarts: { dawn: "04:00", morning: "06:00", day: "10:00", evening: "17:00", night: "20:00" } },
  script: { stagePositionEnabled, voiceIdEnabled, voiceIdFormat: { mode: "pattern" | "manual", pattern },
            transitionDefault: "auto" | "direct" | "none", transitionTemplate: "placeTime" | "placeOnly" },
  cast: { bundleName: { character?, place?, term?, faction? }, showFirstAppearanceInHeader,
          showVoiceProgressInHeader, relationExportAddress },
  travelModes: { key, order, name, kmPerDay, movingHoursPerDay, updatedAt, updatedBy }[],
  defaultTravelModeKey?, termMatching: { extraParticles: string[] },
  fieldBundleAtCreation, counters: { nextCharacterRecordNo, nextBlockNumber? },
  fieldDefs: FieldDef[], tags: Tag[], relationKinds: RelationKind[],
  createdAt, updatedAt, updatedBy, ext?
}

story/index.json = { units: StoryUnit[] }
StoryUnit = {
  key, parentKey?, level, order, title?, synopsis?, doneMarked, hidden, nextBlockNumber?,
  // scene level only:
  place?: { kind: "place" | "region", key },
  time?: SceneTime,                       // omitted = { type: "none" }
  silentCast?: Key[], memo?: string,
  transitionMode?: "auto" | "direct" | "none", transitionRuns?: TextRun[],
  transitionSnapshot?: { place?: PlaceRef, dayRange?: [int, int], tRange?: [int, int] },
  updatedAt, updatedBy, ext?
}
SceneTime =
  | { type: "none" }
  | { type: "relative", baseKey, dayMin, dayMax, precision: "exact" | "approx", band?, clock?: "HH:mm", rebasedFromKey? }
  | { type: "direct", storyDay?, calendarDate?: { month, day, yearOffset }, band?, clock? }
  | { type: "flashback", mode: "anchor" | "direct", eventKey?, relation?: "before" | "on" | "after",
      amount?: { days }, year?, calendarDate?, band?, clock? }

story/<numbering unit key>.json = {
  unitKey,                                // the episode-level unit, or the project key for 1-level projects
                                          // (the counter is StoryUnit.nextBlockNumber / counters.nextBlockNumber, §4.1)
  blocks: Block[],                        // all live blocks of the unit's scenes; grouped by sceneKey, ordered by order
  discardedVoiceIds: { id, blockKey, optionKey?, discardedAt, reason }[]
}
Block = {
  key, sceneKey, order, number, kind, speakerKey?, runs: TextRun[], stage?,
  cues?: { key, kind, assetKey?, effect? }[],
  options?: { key, order, runs: TextRun[], speakerKey?, voice?: VoiceState }[],
  voice?: VoiceState, recorderSource?: { recordKey, recordedAt },
  createdAt, updatedAt, updatedBy, ext?
}
VoiceState = { mode: "auto" | "fixed", fixedId?, fixedAt?, manualId?,
               lastExport?: { at, textHash: "sha256-<hex>", speakerKey? } }
TextRun = { t } | { k: "paren", t } | { k: "link", id: Key, to: { kind, key, projectKey? }, t } | { k: "break" }
```

### 7.5 `cast.json`

```ts
cast.json = {
  characters: { key, recordNo, name, displayNameBase?, oneLiner?, tagKeys: Key[], speechBody: RichDoc,
                secretBody: RichDoc, voiceCode?, hidden, order, fields: { [fieldDefKey]: FieldValue },
                updatedAt, updatedBy, ext? }[],
  relations:  { key, endA: { kind, key }, endB: { kind, key }, between?: SlotValue, mindA?: SlotValue,
                mindB?: SlotValue, addressA?, addressB?, updatedAt, updatedBy, ext? }[],
  voiceLines: { key, characterKey, order, title, text: RichDoc, status: "writing" | "done", … }[],
  archiveDocs:{ key, characterKey, order, title, body: RichDoc, unlockPoint?: PointRef, … }[]
}
FieldValue = { state: "unknown" } | { state: "value", payload } | { state: "fallbackText", fallbackText, fallbackReason }
payload    = { texts: string[] } | { rich: RichDoc } | { number: string } | { choice: { optionKeys, freeText? } }
           | { date: { year, month?, day? } } | { anniversary: { month, day } } | { link: { targetKeys } }
SlotValue  = { kindKey?, description?, pairFirstHolder?: "a" | "b", kindFallbackText? }
RichDoc    = { key, order, kind: "paragraph" | "heading" | "memo", runs: TextRun[], recorderSource? }[]
PointRef   = { kind: "unitStart" | "unitEnd", unitKey }
```

### 7.6 `world.json` and `maps/`

```ts
world.json = {
  places:   { key, order, name, description: RichDoc, parentPlaceKey?, hidden, fields, … }[],
  terms:    { key, order, name, aliases: string[], description: RichDoc, related: { kind, key }[],
              textSearch, hidden, fields,
              rejections: { key, containerKind: "block" | "sceneTransition", containerKey, optionKey?,
                            matchedText, occurrence, createdAt }[], … }[],
  factions: { key, order, name, description: RichDoc, hidden, fields, … }[],
  events:   { key, order, kind: "world" | "story", title, description: RichDoc,
              worldDate?: { year, month?, day? }, sceneKey?, place?: PlaceRef, relatedTermKeys: Key[],
              revealPoint?: PointRef, … }[]
}
maps/<mapKey>.json = WorldMap (§2.7), overlays as arrays of objects, points as [x, y] numbers in 0..1,
                     each overlay object with key, updatedAt, updatedBy, ext?
```

### 7.7 `changes.json`, `checks.json`, `tombstones.json`, `deletions.json`

```ts
changes.json = { records: {
  key,
  target: { kind: "field", entityKind, entityKey, fieldDefKey } | { kind: "displayName", characterKey }
        | { kind: "relationSlot", relationKey, slot } | { kind: "regionShape", mapKey, regionKey }
        | { kind: "termReveal", termKey },
  value?: any, note?,
  anchor: { kind: "block", blockKey } | { kind: "unitStart", unitKey } | { kind: "unitEnd", unitKey }
        | { kind: "worldEvent", eventKey } | { kind: "unplaced" },
  anchorState: "normal" | "orphaned" | "kept", originalAnchor?: Anchor,
  createdAt, updatedAt, updatedBy, ext? }[] }

checks.json = {
  acknowledgements: { key, checkType, subjectKeys: Key[], fingerprint, kind: "match" | "keep", createdAt, … }[],
  travelOverrides:  { key, characterKey, fromSceneKey, toSceneKey, modeKey, … }[]
}
tombstones.json = {
  nameTags:     { key, kind, name, voiceCode?, hidden, state: "purged" | "notExported", purgedAt? }[],
  mergeRecords: { key, kind, mergedKey, targetKey, at, movedRefs, filledFieldDefKeys, chosenFromMerged,
                  deletedMutualRelationKey?, mergedOriginal }[]
}
deletions.json = { entries: { key, kind, deletedAt }[] }
```

### 7.8 `canvas/`, `pages/`, `assets/index.json`

```ts
canvas/<key>.json = { key, order, name, createdAt,
  nodes: { key, kind, refKey?, text?, x, y, w?, h?, groupKey?, z, … }[],
  edges: { key, fromNodeKey, toNodeKey, labelText?, relationKey?, … }[], updatedAt, updatedBy, ext? }
pages/<key>.json  = { key, order, name, sectionLabel?, blocks: RichDoc, hidden, sourceRecord?: { recordKey, recordedAt },
                      createdAt, updatedAt, updatedBy, ext? }
assets/index.json = {
  blobs:        { hash, ext, mime, bytes, width?, height?, durationMs?, source, importedAt }[],
  imageUses:    { key, ownerKind, ownerKey, slot, source: "preset:<id>" | "<hash>",
                  crops: { [ratio]: { x, y, w, h, rotation, flipH, zoom } }, holo, … }[],
  scriptAssets: { key, kind, name, fileName, fileHash?, durationMs?, loop, placeKey?, … }[]
}
```

Only blobs referenced by an exported row are written; audio blobs of recorder records only with `원본 음성 포함`.

### 7.9 `trash/` (feature `trash`)

`trash/index.json = { entries: TrashEntry[] }` plus mirrors of the normal files holding **only non-live rows** in the normal shapes with `deletedAt`/`trashEntryKey` on the directly deleted ones: `trash/story/index.json`, `trash/story/<unit>.json` (trashed blocks with `trashPlacement`, and the blocks of trashed scenes), `trash/cast.json`, `trash/world.json`, `trash/changes.json`, `trash/maps/<key>.json`, `trash/canvas/<key>.json`, `trash/pages/<key>.json`, `trash/assets/index.json`. Keeping trashed rows out of the normal files means an older reader that skips the optional `trash` feature never shows deleted content as live.

### 7.10 `recorder.json` (feature `recorder`)

```ts
recorder.json = { records: {
  key, kind: "voice" | "text", createdAt, updatedAt, updatedBy, runs: TextRun[], textRevision, durationMs?,
  doneMarked, hidden, audio?: { hash, mime, bytes, durationMs },     // audio only with 원본 음성 포함
  links:      { key, targetKind, targetKey, origin, createdAt, lastLabel?, purgedAt?, lastLabelHidden }[],
  placements: { key, kind, targetKey, containerKey?, tabId?, copiedRevision, createdAt, lastLabel, purgedAt? }[],
  ext? }[] }
```

Only records linked to this project (explicitly or implied), not in the trash; hidden records only when the project is hidden and the file is encrypted (LRC-45). Links and placements are those into **this** project (no `projectKey`: implied). `@` runs pointing into other projects are written as plain text (LRC §3.5). On import, records merge by key: union of links and placements, local text kept.

### 7.11 `.civilight-recorder` (library file)

Same container and version rules; `mimetype` = `application/vnd.civilight.recorder`; manifest `format: "civilight-recorder"`; `records.json` = all records not in the trash with **all** links and placements (each with `projectKey`); `assets/<hash>.<ext>` audio per `keepAudio`; hidden records encrypted with the 복구 암호 or left out (LRC Q16 default).

### 7.12 Encryption and preview

Encrypted files (LIB-51 `ED`): outer ZIP = `mimetype` (stored) + `encryption.json` (`EncryptionHeader`, LIB §3.2) `{ format: "civilight-encrypted", lockType: "export" | "recovery", kdf, iterations, salt, cipher, nonce, wrappedDataKey? }` + `payload.bin` (the complete inner package, encrypted). Nothing else is visible outside; the inner manifest holds name and rating. `preview.webp` (from the `projectCover` 3:4 crop) is omitted for hidden projects (LIB-47, F-16); in an encrypted file it sits inside the payload like everything else (FMT §5).

## 8. Versioning and migration

### 8.1 File format

- The five promises of LIB-50 apply: every earlier format opens; upgrades chain one step at a time (`1.0 → 1.1 → …`) as pure functions on the parsed JSON tree, run before mapping to the DB; an upgrade step is never deleted; the source file is never modified; exports always write the latest version.
- Adding a file, an optional property or an enum value → **minor**; changing the meaning or shape of an existing property → **major** (avoid; add under a new name instead).
- Each format version ships a regression file of 「잿빛 새벽의 등불」 under `app/src/test/resources/format/<version>/`, opened by a JVM test with the current app (LIB-56).

### 8.2 Working store (replaces the Room app database — decision 2026-11-22, lead engineer)

- The working store is a **per-project document store**, not Room/SQLite. FMT §1 only requires a working store separate from the `.civilight` package; it does not name a database engine. Reason: eight feature modules are built in parallel, and a shared DAO layer would be the one file every module must change; a document store matches the §7 layout one-to-one, keeps unknown content without `ext` columns, and lets every rule be a pure function over immutable data that is tested on the JVM.
- Layout in app storage: `files/projects/<projectKey>/` holds the same file units as §7.2 (`project.json`, `story/index.json`, one `story/<numberingUnitKey>.json` per numbering unit, `cast.json`, `world.json`, `maps/<mapKey>.json`, `changes.json`, `checks.json`, `tombstones.json`, `deletions.json`, `canvas/…`, `pages/…`, `assets/index.json`, `trash/…`) plus app-only files (`_app/activity.json`, `_app/view.json`, `_app/undo/…`). Blobs live in `files/blobs/<hash>.<ext>`. The recorder library is a separate store `files/recorder/` and the project index (서재 list, ratings, hidden flags, last opened) is a separate store `files/library.json`. App settings stay in DataStore.
- The on-device files carry their own `storeVersion` (independent of the file format version) and are **never handed out as a `.civilight`**: export always goes through the §7 writer (which applies format versioning, encryption and the derived-value rules), and import always goes through the §8.1 upgrade chain.
- Saving: in memory each project is one immutable `ProjectDoc`; edits are pure functions `ProjectDoc -> ProjectDoc`. Only the **file units whose objects changed** are written, debounced (about 600 ms) with temp-file + atomic rename, and flushed immediately on `onPause`/`onStop`. The whole project is never serialised per keystroke.
- `storeVersion` changes ship an explicit upgrade step on the stored JSON (the same chain style as §8.1); there is no destructive fallback.
- Where this file or an area spec says "app DB", "row", "table", "column" or "Room", read "working store", "object", "file unit", "property". `id: Long` local surrogates are not needed: the project directory is keyed by `Project.key`, and an import that must not collide (`새 프로젝트로`, LIB-52) re-keys the project (§6).

### 8.3 Unknown content is preserved (LIB-50 promise 3)

- Unknown properties of a known object are moved into that row's `ext` column (as `_unknown: {…}`), together with the object's own `ext`, and written back unchanged on the next export of the same project.
- Unknown enum values are stored as the raw string; the item is kept and shown read-only in the closest safe form (a block of unknown kind shows its runs as plain text; an unknown run kind shows `t`; an unknown anchor kind sorts as `unplaced` (`ED`); an unknown `ChangeTarget` kind is kept and ignored by `valueAt`).
- Unknown files (and files of unknown optional features) are stored as `UnknownFile` rows with their bytes and written back with the same path; their manifest hash is recomputed.

## 9. Working store mapping

| Store file (under `files/projects/<projectKey>/`) | Holds | Notes |
| --- | --- | --- |
| `project.json` | `Project` incl. settings groups, `fieldDefs`, `tags`, `relationKinds` | as §7.4 |
| `story/index.json` | `StoryUnit` (with scene fields), `DiscardedVoiceId` of all units | `nextBlockNumber` on numbering units |
| `story/<numberingUnitKey>.json` | `Block` of that numbering unit | written alone when only that episode changed |
| `cast.json`, `world.json`, `maps/<mapKey>.json` | cast, world, map objects | as §7.5–7.6 |
| `changes.json`, `checks.json`, `tombstones.json`, `deletions.json` | records, acks, overrides, name tags, merges, deletion log | as §7.7 |
| `canvas/<canvasKey>.json`, `pages/<pageKey>.json`, `assets/index.json` | canvas, pages, `ScriptAsset`, `ImageUse`, blob index | as §7.8 |
| `trash/…` | `TrashEntry` and directly trashed objects | live files hold only live objects (C-14) |
| `_app/*.json` | `ProjectActivity`, `ScriptViewState`, lens state, recents, start checklist, undo steps, export records | class A, never exported |

Global stores: `files/library.json` (project index: key, name, rating, hidden, isExample, readOnly, deletedAt, last opened), `files/recorder/records.json` + audio blobs (class R), `files/blobs/` (content-addressed), DataStore `settings` (class S). Derived values (§2.14: manuscript index, candidate index, resolved times, check results) are computed from the in-memory `ProjectDoc` and cached; none is stored.

## 10. Conflicts resolved here

Each row: what the area specs disagree on, the resolution in this file, and the spec text that must change to match. Ids `C-xx` are cited from `docs/FEATURES.md` notes.

| Id | Conflict | Sources on each side | Resolution | Must change |
| --- | --- | --- | --- | --- |
| C-01 | Bookkeeping field names | SCR §3 `modifiedAt`/`modifiedBy`; LRC §3.2 `modifiedAt`/`modifiedByDevice`; STO, WLD, LIB `updatedByDevice`; CST `updatedDevice`; CNV `updatedBy`/`createdBy` | `updatedAt` + `updatedBy` (+ `createdAt` where shown) on every entity (§1.3) | SCR, LRC, STO, WLD, CST, LIB, CNV §3 |
| C-02 | Identity field names | CST §3 uses `id`, `projectId`, `characterId`, `entityId`, `fieldDefId`, `kindId`, `tagIds`, `mergedInto`; every other area uses `key`; samples use bare `parent`, `base`, `from`, `target` | `key` and `<role>Key(s)` everywhere (§1.1); `projectId` exists only as the local DB surrogate (§6) | CST §3; STO §3.5, CNV §3.3, LRC §3.5 samples |
| C-03 | Manual order field and its definition | CST, WLD `orderKey`; SCR, STO, CNV `order`; no spec defines the alphabet or the comparison | `order: OrderKey` with the base-62 alphabet, binary comparison and generation of §1.2 | CST §3, WLD §3 |
| C-04 | Anchor model and the meaning of a scene-end anchor | SCR §3.2 `seat` + `pointUnit {EPISODE · SCENE}` + `originalBlockKey` + `trashedAt`; STO §3.1 `Block · UnitStart · SceneEnd · WorldEvent` + `orphaned: Boolean`; CST §3.2 `Point(unitKey)` + proposed `SceneEnd`; WLD §3.1 `POINT(unitKey, CHAPTER · EPISODE · SCENE)` + `NORMAL · ORPHANED · LEFT_AS_IS`. STO's `+∞` tail keeps later blocks of that scene **before** a `SceneEnd` record; CST-10's last sentence puts them **after** it | One sealed `Anchor { block, unitStart (any level), unitEnd, worldEvent, unplaced }` + `anchorState normal · orphaned · kept` + `originalAnchor` (§2.4, §5.5). A boolean cannot tell "will re-attach" from "kept by 이대로 두기"; SCR-43 step 4 needs `unplaced`; WLD needs chapter-level points. `unitEnd` follows STO: the 집필 선 is scene-granular, so "right after the 집필 선" (IDX) is after everything in that scene (§4.3) | SCR §3.2; STO §3.1 (`SceneEnd` → `unitEnd`, `orphaned` → `anchorState`); CST §3.2 and CST-10 last sentence; WLD §3.1 |
| C-05 | `@` link runs: identity, targets, field names | SCR §3.2 `Link(targetKind CHARACTER · TERM · PLACE, targetKey, text)` with no identity; WLD-17/§1.2 require `linkId`; LRC §3.2 `Link(itemKey, itemKind, projectKey, text)`; CNV §3.1 `{targetKind, targetKey}` and page links to factions and pages | `LinkRun { id (linkKey), to { kind, key, projectKey? }, t }`; one union of target kinds with per-surface subsets; identity kept through split/merge/move/cut, re-minted on copy (§1.4) | SCR §3.2 `TextRun`, SCR-55; LRC §3.2 (`Link` shape, record `text` → `runs`); CNV §3.1 |
| C-06 | Where `◇ 첫 공개` lives | WLD §3.1 separate `FirstReveal` entity with its own anchor and state, "proposed in `changes.json` as kind `firstReveal`"; STO-12 and SCR-45 say the anchor engine treats `◇` exactly like `◆` | A `ChangeRecord` with target `termReveal`; at most one live per term (§2.4) | WLD §3.1, §3.3 |
| C-07 | Long text and transition text shape | CST, WLD opaque `RichText`; CNV `PageBlock` list; LRC `cardParagraph` placements need a paragraph key; SCR `transitionText: String` while rule 22 §3 / WLD-19 need term links and candidates in direct transition text | `RichDoc` of keyed `DocBlock`s for every long text (§1.4); transition stored as `transitionRuns` | CST §3, WLD §3, CNV §3 (`PageBlock` → `DocBlock`), SCR §3.2 |
| C-08 | Story structure levels | LIB §3.2 `structurePreset` + `structureLevels: List<StructureLevel {key, name, order}>`; STO §3.1 `StoryStructure {preset, levels: List<String>}` with `StoryUnit.level` as index | STO's list (level identity = index; the count is locked once units exist, STO Q1) | LIB §3.1–§3.2 |
| C-09 | World time settings | LIB §3.2 `timeMode {YEAR_ONLY · WORLD_CALENDAR · NONE}`, `startYear`, `calendarKey`; WLD §3.1 `WorldTime {mode YEAR_ONLY · CALENDAR · NONE, storyStartYear, calendar}` embedded | WLD's embedded `worldTime` (owner WLD-29; one calendar per project) | LIB §3.1–§3.2 |
| C-10 | Where image choices are stored | CST `Character.portrait/background: ImageSlotRef`; WLD `image: ImageRef`; LIB `Project.coverImage`; CNV `ImageUse` rows in `assets/index.json` (FMT §2 "자른 정보 · 홀로 · 쓰이는 곳") and `Page.headerImage` as an `ImageUse` key | `ImageUse` rows only, unique per (owner, slot); owners have no image columns (§2.10) | CST §3.1, WLD §3.1, LIB §3.2, CNV `Page` |
| C-11 | "Asset" means two things | SCR §3.2 `Asset` (direction assets, `imageHash`, proposed in `assets/index.json`); CNV §3.1 `ImageAsset` (content-hashed originals in the same file); LRC audio files by hash | `ScriptAsset` (catalogue entry with `fileHash` for image or audio) and `Blob` (any content-hashed file); one `assets/index.json` with `blobs`, `imageUses`, `scriptAssets` (§2.3, §2.10) | SCR §3.2, §3.4; CNV §3.1, §3.3; LRC §3.2 |
| C-12 | App settings defined in five places | LIB `AppSettings`; CNV `AppPrefs` (repeats `unfoldedLayout`, `animation`, `reduceTransparency`, `recorderGlow`, `viewByScreen`); WLD device settings; STO §3.2 check settings; LRC `RecorderSettings` | One `AppSettings` DataStore record with named groups (§2.13) | CNV §3.1, STO §3.2, WLD §3.1, LRC §3.2 |
| C-13 | Trash state on rows | CST `trash: TrashState {deletedAt, reason, mergedInto}`; SCR `trashedAt` (blocks and anchors); STO `deletedAt`; LRC, LIB `deletedAt` + `trashEntryKey`; CST/WLD "owned data goes to the trash with the item" without saying how | `deletedAt` + `trashEntryKey` only on the directly deleted row; reason and merge target on `TrashEntry`; owned rows are non-live by derivation (§5.1), which makes TC-20-08/09/24 exact | CST §3.1, SCR §3.2, WLD §3 |
| C-14 | Trash inside the file | SCR §3.4: trashed blocks keep `trashedAt`/`trashPlacement` in their episode files; LIB-49: `trash/` | `trash/` mirror + `trash/index.json`; normal files never hold non-live rows, so a reader that skips the optional `trash` feature never shows deleted content as live (§7.9) | SCR §3.4 |
| C-15 | Name tags and merge records | LIB `NameTag {…, hidden, purgedAt}` in `tombstones.json`; CST `NameTag {…, wasHidden, deletedAt}` + `MergeRecord` in `cast.json` (`cast.tombstones`); WLD tombstone `{key, name, kind}`; LRC `nameTag` strings; no spec covers live references to trashed items in a trash-less export (CST Q22) | `tombstones.json` with `nameTags` (`state purged · notExported`) and `mergeRecords`, feature `tombstones`; recorder strings renamed `lastLabel` (§2.11, §5.6) | CST §3.1, §3.4; WLD §3.3; LRC §3.2; LIB §3.2 (`state`) |
| C-16 | Acknowledgement entity and file | STO `Acknowledgement {checkType, subjectKeys, fingerprint, kind}` in `checks.json` (ED); CNV `CheckAck {kind, subjectKeys, createdBy}`, file open; CST `RefAck {targetKey, scope}`; WLD term `맞음` keyed by `linkId`, "with the check-center owner"; LIB-49 puts term `아님` in `checks.json` too | One `Acknowledgement` (STO shape) in `checks.json` with `travelOverrides`; term `맞음` = subject `[linkKey]` (§2.9) | CNV §3.1, CST §3.1, WLD §3.3, LIB-49 |
| C-17 | Voice-ID storage | LIB-49 `voice.json`, LIB `VoiceIdState AUTO · FIXED · DISCARDED`, contract `fixedAt: LocalDate`, CSV `바뀜` ledger app-only (`ExportRecord`); SCR per-block `VoiceState` (`fixedAt: Instant`) + per-episode `DiscardedVoiceId` | SCR's model; no `voice.json`; `VoiceState.lastExport {at, textHash, speakerKey}` replaces the app-only ledger so every device computes the same CSV state (§2.3) | LIB §3.1, §3.2 (`ExportRecord`), §3.3, LIB-49 |
| C-18 | Last position, three copies | LIB `Project.lastPosition`; STO `ProjectActivity {lastEditedBlockKey, lastEditedAt}`; SCR `lastWritingPosition {blockKey, offset}` | One app-DB `ProjectActivity` (§2.13) | LIB §3.2, SCR §3.2 |
| C-19 | Daily character count, two models | SCR `ScriptViewState.dailyTally: Map<LocalDate, Int>`; STO `DayBaseline` | `DayBaseline` (STO-06 definition) | SCR §3.2 |
| C-20 | Trash kinds | LIB `TrashKind` has no page, map, canvas or event; CST-43/WLD-14 restore maps (`지도 되살리기`); WLD §3.2 lists events under trash filter `세계`; vocabulary makes every `지우기` a trash move | Add `page`, `map`, `canvas`, `event`; filters: map, event → `세계`; page, canvas → `전체` only. Whether pages and canvases get a delete action and where stays open (Q-PG-08, Q-CV-01) | LIB §3.1 |
| C-21 | Term `아님` location and option reference | LIB-49 ED: `checks.json`; WLD: inside the term with `optionIndex`; SCR gives options stable keys | Inside the term in `world.json`, `optionKey` instead of `optionIndex` (indexes shift when options move) | LIB-49; WLD §3.1; SCR-56 dismissal fields |
| C-22 | Hidden flag on timeline events | WLD §3.1 `TimelineEvent.hidden` `[PROPOSED]` "follows item hiding of linked items only"; LIB `HideableKind` has no event | No stored flag; masking derived from linked hidden items | WLD §3.1 |
| C-23 | `hidden` means two things | CST `FieldDef.hidden` (definition hidden, values kept) vs rule-19 item hiding | Rename to `FieldDef.definitionHidden` | CST §3.1 |
| C-24 | Two check-center registries | CNV-17 vs STO-40: buttons of `같은 때 두 장소` / `동시에 두 장소`, filters of time/transition rows, the `점검 못 함` button, ack naming (STO Q35) | STO-40's registry and semantics with the check-type ids of §2.9; CNV-17 implements the shell only | CNV-17 registry table |
| C-25 | Scene place reference | STO `PlaceRef {kind PLACE · REGION, key}`; WLD §3.2 `(WorldMap.key, Region.key)` | `{kind, key}` with project-unique region keys | WLD §3.2 |
| C-26 | Child-map link stored twice | WLD `WorldMap.parent {mapKey, pinKey}` and `Pin.childMapKey` | `WorldMap.parent` only; a pin's child map is derived | WLD §3.1 |
| C-27 | Engine JSON envelope and field names | LIB-45 `{format "civilight-engine", version 1, time {fromStart, day, …, earliest, latest}}`; SCR-63 `{format "civilight-script", version "0.1", time {storyDay {min, max}, episodeDay, …}}` | Envelope from LIB-45 (writer owner); scene and block content names from SCR-63 (content owner); still provisional until the target engine is chosen (SCR Q-25). Not part of `.civilight` | LIB-45 skeleton; SCR-63 header |
| C-28 | Project key scope | LIB-52 ED "rows keyed by project key + item key" breaks when `덮어쓰기` puts the old project (same key) in the trash and when that copy is restored | Local `projectId` surrogate; the portable key is unique among live and trashed rows; a replaced or trashed same-key project is re-keyed (§6) | LIB-52 ED note |
| C-29 | Recorder link units | LRC `LinkTargetKind CHAPTER · EPISODE · SCENE` assume the default structure; custom and 4-level structures have other levels | `unit` (label from the unit's level and code) | LRC §3.1, §3.2; LIB §3.3 contract |
| C-30 | Fonts | LIB (LIB-01, LIB-38, LIB-43 PDF), CNV (CNV-13, 17, 23, 24, 28, 38), STO (STO-01, 03, 04, 39) and CST name Nanum Myeongjo / Noto Sans KR / IBM Plex Mono; the user's decision (memory 2026-11-22), DS-05/DS-06 and LRC §0: Pretendard everywhere, numbers with `tnum` | Pretendard; those font names are read as DS-06 type roles (body serif → manuscript body role, mono → number role). Not a data matter, listed because it is cross-spec | the named typography lines |
| C-31 | JSON casing in samples | STO §3.5 mixes `"PLACE"` and `"relative"`; CNV `"FACTION"`, `"from"`; LRC `"voice"`, `"target"`; LIB-50 manifest `"rating": "R-15"` | §1.5: lowerCamelCase properties and enum values (`"place"`, `"faction"`, `"r15"`), `…Key` reference names | STO §3.5, CNV §3.3, LRC §3.5, LIB-50 samples |
| C-32 | Where the block counter is stored | SCR §3.4 `[PROPOSED placement]`: `nextBlockNumber` inside `story/<episode key>.json`; LIB-55 imports everything except a damaged episode file, and rule 12 §5 forbids reissuing numbers | `StoryUnit.nextBlockNumber` in `story/index.json` (`counters.nextBlockNumber` in `project.json` for 1-level projects), which LIB-55 requires to be intact (§4.1) | SCR §3.4 |

## 11. Open questions carried (not decided here)

These stay with the product owner; the area specs' defaults remain in force and the model supports either answer unless noted.

| Question | Where | Model impact |
| --- | --- | --- |
| Voice-ID episode code for `막 › 장면`, 1- and 4-level structures; numbers above 999 | SCR Q-06 | none (numbering unit defined, §4.1) |
| Renumbering vs trashed blocks' numbers | SCR Q-07 | counter unchanged (default); block-number index is not unique (§9) |
| Does the dotted mark stay after `이대로 두기` on a `근거 지워짐` record | SCR Q-20 | `anchorState = kept` lets the UI choose |
| Exact meaning of CSV `바뀜` | SCR Q-27, LIB-44 | inputs stored in `VoiceState.lastExport` |
| Are script-asset audio files packaged | SCR Q-29 | `ScriptAsset.fileHash` optional |
| `@` links and candidates inside direct transition text | SCR Q-32, WLD Q-05 | storage is runs, so both answers fit |
| `◇ 첫 공개` on a whole choice block | SCR Q-30, WLD Q-06 | allowed by the model; option lines impossible by construction |
| Changing the number of structure levels later | STO Q1 | levels locked once units exist (default) |
| Hidden scenes and the 집필 선 | STO Q9 | they count (default) |
| Flashback `전 · 후` amount | STO Q20 | `amount: { days }` placeholder |
| Character record numbers; where `voiceCode` is edited | CST Q11, Q12 | counter `nextCharacterRecordNo` added |
| Region kinds and provinces as scene `지역` | WLD Q-17 | only overlay `MapRegion`s are `PlaceRef(region)` |
| Page delete, page list; canvas list and delete | CNV Q-PG-05, Q-PG-08, Q-CV-01 | trash kinds `page`, `canvas` exist (C-20) |
| `내 이미지` scope | CNV Q-IM-03 | the app DB records which project imported a blob (`blob_owner`, §9) |
| Preset ids other than `ash-dawn` (they become file format) | CNV Q-IM-06 | must be fixed before the first export |
| Hidden items in Drive backup; recorder file hidden records | LIB Q5, LRC Q16 | whole-file encryption (default) |
| Voice IDs of hidden scenes left out of an export | LIB Q20 | not fixed (default) |
| Label for a reference whose target is absent and has no name tag | new | `TODO_COPY_DM_01` (§3) |
| `notExported` name tags shown as `지운 <kind> · <name>` without `되살리기` | new | confirm the wording (§5.6) |
| Engineering defaults made in this file: `unplaced` anchor, `worldEvent` anchors after an event purge, `PointRef` resolution for a non-live unit, re-keying a replaced or trashed same-key project, `NameTag.state = notExported`, `VoiceState.lastExport`, `Project.counters`, `blob_owner`, unknown anchor kinds sorting as `unplaced` | new | marked `ED` where defined; confirm or replace |

## 12. Addendum (2026-11-23) · Gallery — model owner: lane `feature:canvas` (renderers: CST, STO, LIB) · spec `55-gallery.md` (GAL)

Additive only: nothing above changes; §12.6 lists the rows to fold into §1.7, §2.11 and §10 at the next pass of this file. `ED` defaults here are listed in `55-gallery.md` §6.

### 12.1 `GalleryItem` · P · `gallery/<ownerKey>.json › items`

An ordered image of a character or of the project. It references a `Blob` (or a preset) exactly like `ImageUse` does, but it is a list per owner, so `ImageUse` keeps its one-row-per-(owner, slot) rule.

```ts
GalleryItem = {
  key: Key,
  ownerKind: "project" | "character",
  ownerKey: Key,                       // Project.key or Character.key
  source: "preset:<id>" | "<blob hash>",
  order: OrderKey,                     // 직접 순서 within (ownerKind, ownerKey), §1.2
  title: string,                       // "" = untitled (`제목 없음`)
  caption?: RichDoc,                   // description; LinkRun targets as §1.4 / C-05; names resolve at 최신 (ED)
  tagKeys: Key[],                      // project Tag keys (§2.5 `Tag`)
  sceneKey?: Key,                      // a scene (StoryUnit); label and 때 computed on read
  crops: { r3x4?: Crop, r1x1?: Crop }, // gallery slot ratios; missing = centred largest frame; presets in registry pixels
  holo: boolean,
  hidden: boolean,                     // item hiding (GAL-19, ED extension of LIB-36)
  createdAt: Instant, updatedAt: Instant, updatedBy: DeviceId,
  deletedAt?: Instant, trashEntryKey?: Key,
  ext?: object
}
GalleryFile = { ownerKind, ownerKey, items: GalleryItem[] }   // one file unit per owner
```

- **Uniqueness** (`ED`): at most one item per `(ownerKind, ownerKey, source)` among live **and trashed** items, so restore never creates a duplicate; purge frees the source.
- **Main image is derived**: an item is `초상` / `배경` / `표지` when the owner's `ImageUse` of that slot has the same `source`. No flag is stored. `초상으로` · `배경으로` · `표지로` write that `ImageUse` (same source, new crops).
- **Slot registry**: a new `SlotSpec GALLERY` (ratios `r3x4`, `r1x1`; holo allowed) is used by the crop editor only; `ImageSlot` in `ImageUse` does **not** gain a gallery value.
- **Scene link** is a reference: trashed or purged scene → dotted `지운 씬 · <code>` per §3 (purged: code from the `NameTag` of kind `unit`); hidden scene while locked → `숨긴 씬` (rule 19 §6). Either sorts as unlinked in `씬 순서`.
- **Hiding** (`ED`): while locked a hidden item is absent from every surface (lists, counts, positions, search, picker `내 이미지`, `이미 있음` marks) but still holds its source for uniqueness; adds and copies of that source are skipped without naming it (55 GAL-19).
- **Copy** (`다른 곳에도 넣기`): a new item with every field except `key`, owner, `order` and bookkeeping (`hidden` included).
- **Merge** (CST-45, `MergeOps`): the merged character's items move to the target, appended in their order; one whose source the target already holds stays with the merged character. Each move is a `MergeRecord.movedRefs` entry `{ ownerKind: "galleryItem", ownerKey: <item key>, field: "ownerKey" }`.
- **Naming**: the existing `Blob.source = gallery` (`BlobSource.GALLERY`) means "system photo picker" and is unrelated to this entity; its UI word is `사진`. The serial name stays.

### 12.2 Liveness, trash, blobs

| Row | Owners (all must be live) | Notes |
| --- | --- | --- |
| `GalleryItem` | owner (project or character) | `sceneKey`, `tagKeys` are references |

- Deleting one item: direct mark + `TrashEntry.kind = galleryItem` (filters `전체`, plus `인물` for character items, `ED`). Restore clears the mark; the item sorts by its kept `order`. A hidden item's trash row is masked and restore needs authentication.
- Deleting the owner character: items are not live by derivation and come back with it (§5.1); the impact sheet counts them (`OwnedKind.GALLERY_ITEM`, `이미지 <n>`). Purge of the owner deletes them (§5.3).
- **Blob references.** Three computations exist today: `BlobReferences.counts` (core:store), `PurgeOps.referencedBlobs` (domain:trash) and `PurgeActions.heldBlobs` (domain:actions). They must become one function, and it counts `GalleryItem.source` hashes of live and trashed items. Otherwise a purge of any unrelated item deletes originals only gallery items use. Done 2026-11-23: `core:model` `BlobRefs` is that function; the other three delegate to it.
- Undo: `DocChange.revert` gains a row list for gallery items (as for `imageUses`); `LiveRows`, `RestoreOps`, `TrashListing`, `PurgeOps.removeImages` and the `DeletedItems` snapshot each gain a gallery branch (55 §3.4).

### 12.3 Package (format 1.0)

- New file units: `gallery/<ownerKey>.json` (`GalleryFile`, live items) and `trash/gallery/<ownerKey>.json` (trashed items, `휴지통도 포함`, §7.9 pattern).
- Originals of gallery items: `gallery/files/<hash>.<ext>`, written for every blob a written gallery item references, also when `assets/<hash>.<ext>` holds the same bytes for another row (the package may hold such a file twice; the device stores it once). Every written blob keeps its metadata row in `assets/index.json › blobs`. The reader finds `gallery/` by path whether or not the manifest lists `gallery`, and a file without `gallery/` opens with empty galleries. `ED`, 55 §6 Q16.
- Manifest feature id (§7.3): `gallery` · optional · covers `gallery/` · present when any gallery file is written. `Features.GALLERY` joins `Features.OPTIONAL` (and so `KNOWN`).
- Version (correction 2026-11-23): the gallery is part of **format 1.0**, because 0.1.0 is not released yet and ships with it. `formatVersion "1.0"`, `minReaderVersion "1.0"`; no format 1.1 and no upgrade step. The 1.0 regression file `app/src/test/resources/format/1.0/` (LIB-56) gains G0.
- On read: `gallery/files/*` go into the one blob store; gallery files whose owner is missing from the file are dropped (a damaged or hand-edited file).
- Item hiding counts as "the project has hidden items" for the encryption default (rule 19 §5, LIB-51); the user can still turn encryption off.
- PDF, CSV and engine JSON exports carry no gallery content (`ED`).

### 12.4 App state (never exported)

- `AppSettings.viewByScreen` keys `characterGallery`, `projectGallery` with values `LIST` · `GRID` · `CARD` (`ViewMode.GRID` is new; screens without it never store it; the codec's fallback for unknown values stays `LIST`).
- `AppSettings.galleryTextOn: Boolean = true` (viewer `설명` toggle).
- Saved UI state (process death, CNV-35): picked gallery item key, panel expanded flag, row scroll per gallery screen.

### 12.5 Working store (§8.2)

- `ProjectDoc.gallery: Map<ownerKey, GalleryFile>`, stored as the file units `gallery/<ownerKey>.json` and `trash/gallery/<ownerKey>.json` under `files/projects/<projectKey>/`. An edit (a caption keystroke included) rewrites only that owner's unit, debounced as §8.2.
- `ProjectIndex` accessors: items by owner in `order`, items by `source` (uniqueness, `이미 있음`, `휴지통에 있음`, blob references).
- Liveness check (the existing live-row helpers, §5.1) for a gallery item = not directly marked and owner live.

### 12.6 Rows to fold into the body later

- §1.7 glossary: `GalleryItem` · 갤러리 이미지 · — · `ImageUse` (one image per slot), `BlobSource.GALLERY` (system photo picker).
- §2.11 `TrashEntry.kind` gains `galleryItem`.
- §7.3 feature table gains `gallery` · optional · `gallery/` · any gallery item.
- §10 conflict row C-33 · gallery: new trash kind `galleryItem`, feature id `gallery` (format 1.0), `ViewMode.GRID`, one blob reference function · resolution as §12.2–12.4.

## 13. Addendum (2026-11-24) · 0.2.0 (0.2.0)

Source: the frozen scope `docs/status/0.2.0-scope.md` (G-1 … G-7, S-4, S-8, U-16, N-29, N-58, §8 data safety, defaults D6, D20, D21, D29, D34). Additive only: no field above is removed, renamed or given a new meaning, and every 0.1.0 store and `.civilight` file opens unchanged (scope §8 rule 2). Rows to fold into the body later are in §13.9.

### 13.1 Format version (D29)

- `.civilight` stays **format 1.0** (`formatVersion "1.0"`, `minReaderVersion "1.0"`). This deviates from §8.1's "adding a property or a file → minor" on purpose (D29): every 0.2.0 addition is either an optional property, which a 1.0 reader keeps in `ext` and writes back (§8.3), or a file under a new **optional** feature id, which a 1.0 reader skips and keeps as an `UnknownFile` (§8.3). No upgrade step is needed, so the version does not move. `writtenBy` reads `Civilight 0.2.0 (Android)`.
- The working store's `storeVersion` (§8.2) also stays; the one-time G-3 step (§13.2) keys on an absent property, not on a version number.
- If a later 0.2.x change needs an upgrade step, the version moves to 1.1 then, and 0.1.0 files must still open (D29).

### 13.2 `Project.gameTools` · `게임 제작 도구` (G-1, G-3, G-4; owner LIB-91)

| Field | Type | Class · file | Meaning |
| --- | --- | --- | --- |
| `gameTools` | `Boolean?` | P · `project.json › gameTools` | `true` = 게임 제작 도구 on. **Absent and `false` differ**: absent means "written by 0.1.0, not decided yet". Never encoded as a default: a 0.2.0 writer always writes `true` or `false` |

- **New project** (`NewProject.create`): writes `gameTools: false` (G-1).
- **One-time upgrade** (G-3): when a project is loaded from the working store **or** read from a `.civilight` (import, open, restore) and `gameTools` is absent, set `gameTools = GameData.present(doc, exportRecords)` and save that unit at once. It runs only while the property is absent; after that the stored value is the user's (a project turned off in 0.2.0 stays off when reopened). The same pure function serves both paths.
- **`GameData.present(doc, exportRecords): Boolean`** (`core:model`, pure, D6) is `true` when any of these exists, live **or in the trash** (trash files included):
  1. a `Block` or `ChoiceOption` with `voice.mode == fixed`, a non-null `voice.manualId`, or a non-null `voice.lastExport`;
  2. a `DiscardedVoiceId` row;
  3. a `direction` block with a non-empty `cues` list;
  4. a `ScriptAsset` row;
  5. a `VoiceLine` or an `ArchiveDoc` row (`게임 텍스트`);
  6. a `dialogue` block with a non-null `stage` (무대 위치 value);
  7. an `ExportRecord` (§2.13, class A) with format CSV or engine JSON. This input exists only on the device that exported; an imported 0.1.0 file has no export records, but every block such an export wrote carries a fixed ID (item 1), so the result is the same.
  - Auto voice IDs alone never count (every 0.1.0 project has them). `voice.manualId` counts because a hand-typed ID is not automatic (`ED`, extends D6's "fixed or discarded").
- **Example project** (D7): same rule.
- **Export and backup** (G-4): `.civilight` and Drive backups always carry `gameTools` and every piece of game data, on or off. Nothing is stripped on write; nothing is reset on read.
- **Hiding never deletes** (scope §8 rule 1): turning the switch off changes only what is shown. No row listed above is removed, rewritten or regenerated. Auto voice IDs stay derived exactly as in 0.1.0, so turning it on shows what 0.1.0 would.
- Pure helper for every surface: `GameTools.on(project) = project.gameTools == true`. An absent value is never read by UI code; the load step above always resolves it first.

### 13.3 Rating without badges (S-4, D34; owner LIB-92)

- `Project.rating`, `Project.protections`, `AppSettings.ratingDefaults` and the manifest's `project.rating` are **kept and written as stored**. No UI shows or changes the rating in 0.2.0.
- A **new project** stores `rating: all` and the `전체` protections (all four `false`); the step-4 `이 프로젝트 숨기기` switch is the user's only protection choice at creation (LIB-09, LIB-12 notes).
- A 0.1.0 `r15` or `r18` project keeps its stored rating and switches. Rules keyed on a stored `r18` keep working from that value (D34): the recorder `숨기기` row default (LRC-42), the cloud-transcription exclusion, and the export defaults keyed on R-18 (file name, `PDF 암호`, `암호`, LIB-41).
- `RatingDefaults` is no longer read for new projects; its values are kept for a later version.

### 13.4 View settings (U-16; owner LIB-93, LIB-94, CST hand-off)

New `AppSettings` fields (class S, DataStore; never in `.civilight`):

| Field | Type | Default | Values (UI label) |
| --- | --- | --- | --- |
| `libraryStyle` | `LibraryStyle` | `BOOKSHELF` (D1) | `BOOKSHELF` `3D 책장` · `COVER_SPIN` `표지 회전` · `MANUSCRIPT_STACK` `원고 더미` · `ARCHIVE_HALL` `서고 회랑` · `INDEX_DRAWER` `색인 카드 서랍` · `CONSTELLATION` `별자리` · `LIST` `목록` |
| `castCardStyle` | `CardStyle` | `FAN_DECK` (D1) | `FAN_DECK` `부채꼴 덱` · `CAROUSEL` `회전 목마` · `STACK` `겹친 더미` · `TAROT` `타로 펼침` · `TILTED_WALL` `기울어진 벽` · `ROLODEX` `롤로덱스` · `LIST` `목록` |
| `libraryToggle` | `ViewMode?` | absent | head toggle on 서재: `LIST` = the list, `CARD` = the chosen 3D style; **absent = `CARD`** |
| `castToggle` | `ViewMode?` | absent | head toggle on the cast list, same meaning |
| `viewTilt` | `Boolean` | `true` | tilt follow switch (LIB-94) |

- Enum order is the settings list order above. Unknown stored values decode to the default (`BOOKSHELF`, `FAN_DECK`), never to `LIST`, so a newer value never hides the chosen 3D view silently.

- **Why new toggle fields** instead of `viewByScreen["library"]` / `["cast"]`: the settings codec writes with `encodeDefaults = false`, so 0.1.0 persisted the seeded `library → LIST` whenever any other `viewByScreen` entry changed; a stored `LIST` therefore does not mean the user chose the list. And 0.1.0's `CARD` meant the old card carousel, which 0.2.0 replaces. The old entries are kept and no longer read (scope §8: settings kept). Every install, upgraded or new, first shows the chosen 3D view (decision U-16: "화려한 보기가 기본"); from then on the last toggle position is remembered. `ViewMode.GRID` stays gallery-only (§12.4); a `GRID` read here decodes as `CARD`.
- **`ShownView.of(style, toggle)`** (pure, `core:model`): `style == LIST` → list, and the toggle is not drawn; `toggle == LIST` → list; otherwise (absent or `CARD`) → `style`.
- **Tilt** (parallax and light): `Tilt.follows(viewTilt, motion) = viewTilt && motion != REDUCED`, where `motion` is the resolved motion mode of CNV-46 (`설정 · 움직임 · 투명도` `애니메이션` with the system flag).

### 13.5 Episode versions `판` (N-29; owners STO and SCR for behaviour, LIB-97 for files) · **supersedes D20**

The computed task for this pass chose the working store plus a `.civilight` export option over scope D20 ("stored in the project document"). This section follows that choice. Because Drive backup (LIB-57) is still a `needsExternal` stub, the export option defaults to **on**, so moving a project by file keeps its versions unless the user turns the row off.

**`EpisodeVersion`** · P (optional in manual export) · working store `files/projects/<projectKey>/versions/<unitKey>.json` · package `versions/<unitKey>.json` (feature `versions`)

```ts
versions/<unitKey>.json = {
  unitKey: Key,                         // the numbering unit (episode level; the project key for 1-level projects, §4.1)
  versions: EpisodeVersion[]            // oldest first
}
EpisodeVersion = {
  key: Key,
  name: string,                         // `11월 23일 · 초고` (manual default) or `11월 24일 · 내보낸 판` (D21)
  origin: "manual" | "export",
  createdAt: Instant, createdBy: DeviceId,
  scenes: { key: Key, title?: string }[],          // scene order of the unit at that moment (scene keys, titles as shown)
  blocks: VersionBlock[],               // every live block of the unit's scenes, in manuscript order
  ext?: object
}
VersionBlock = {
  key: Key,                             // the block's permanent key; `이 판 글로` matches on it
  sceneKey: Key, number: Int, kind: BlockKind, speakerKey?: Key,
  runs: TextRun[],                      // text as it was (link runs keep their targets)
  options?: { key: Key, runs: TextRun[] }[]     // choice lines
}
```

- **What a version holds**: text, kind, speaker, number and order only. ◆ records, voice IDs, cues, stage values and acknowledgements are **not** copied and are never changed by a version (decision N-29: "◆ · 번호 · 보이스 ID는 건드리지 않습니다").
- **Writes**: `판 남기기` (manual), and one automatic version per exported numbering unit when a PDF, CSV or engine JSON export covers it (D21; `origin: export`). A `.civilight` export or a backup creates no version. The write path is one pure function in `domain:script` (`EpisodeVersions.capture(doc, unitKey, name, origin, ctx)`), called by `feature:story` (episode ⋯) and by `domain:actions` `ExportActions.run`.
- **`이 판 글로`** writes the version's `runs` (and choice `options[].runs` by option key) into the live block with the same key, as one undo step through `domain:script`. Nothing else of the block changes. A block that is no longer live (trashed or purged) shows no `이 판 글로` (`ED`; no whole-episode revert in 0.2.0, D20).
- **No cap** on the number of versions (`ED`). Each version is a few KB of text.
- **Hidden content**: a version holds the blocks of hidden scenes like the live file does. While the hidden scene is locked, its rows on the compare screen are masked exactly as the scene is in the script (rule 19 §6). The file's encryption default already counts hidden items (LIB-51).
- **Deletes**: trashing the unit hides its versions with it (not live by derivation, §5.1); restoring brings them back; purging the unit or the project deletes `versions/<unitKey>.json`. Deleting a single version is not in 0.2.0 scope (`ED`: no delete action).
- **Leaving the device**:

| Path | Versions written? |
| --- | --- |
| Drive automatic backup (LIB-57) | **always** (a restore must not lose them) |
| Manual `.civilight` export (LIB-41) | when the export row `TODO_COPY_LIB_EXPORT_VERSIONS` is on; **default on** (`ED`: Drive backup is still a `needsExternal` stub in this build, so a manual file is the only way versions leave the device; the row keeps it a choice as the computed task asks) |
| `.civilight-recorder` | never |
| PDF, CSV, engine JSON | never (they only trigger the automatic version) |

- **Reading**: restore and import read `versions/` whenever present (feature `versions` is optional, so a 1.0 reader skips it and keeps it as unknown files). Import as a new project (`새 프로젝트로`, re-keyed per §6) keeps the versions; their `key`s are kept and their block keys follow the re-keying map. A file without `versions/` opens with no versions.
- Manifest feature id (§7.3): `versions` · optional · covers `versions/` · present when the file holds any version.

### 13.6 Several canvases (N-58; owner CNV, D25)

- The model already stores canvases as a map (`canvas/<key>.json`, `ProjectDoc.canvases`), each with `order` and `name`; 0.2.0 adds no project data.
- New class A field `ProjectActivity.lastCanvasKey: Key?` (`_app/activity.json`): written when a canvas is opened. Home `캔버스` opens it when live, else the first live canvas by `order`, else creates one (`ED`, same as 0.1.0's first open).
- Create, rename, reorder, delete and restore a canvas: trash kind `canvas` (C-20) through `domain:trash`; a canvas's nodes and edges go and come back with it (§5.1). Node and edge rules are unchanged (D25).

### 13.7 Recorder settings that are hidden (S-8)

- `RecorderSettings.lockScreenRecording` (and any widget or tile value) stays stored with its 0.1.0 value; no 0.2.0 code reads it. The rows are hidden (LRC-48).

### 13.8 Data-safety tests (JVM)

- **DM-0.2-01** `GameData.present`: empty project → false; one block with `voice.mode = fixed` → true; the same block in the trash only → true; one `ScriptAsset` → true; one `VoiceLine` → true; one dialogue block with `stage = left` → true; one `DiscardedVoiceId` → true; a direction block with one cue → true; only auto voice IDs → false; only an `ExportRecord` with format CSV → true; with format PDF → false.
- **DM-0.2-02** Upgrade: a store unit without `gameTools` and with a fixed voice ID → loads `true` and the unit is rewritten with `gameTools: true`; without game data → `false`; a unit with `gameTools: false` and a fixed voice ID → stays `false`.
- **DM-0.2-03** Round trip: `gameTools: false` with voice IDs, cues, assets, voice lines and stage values → export `.civilight` → import → every row equal and `gameTools` still `false` (G-4).
- **DM-0.2-04** 0.1.0 fixture (written by the `v0.1.0` tag's code, scope §8): load, save through 0.2.0 `core:store`, reload → every 0.1.0 field equal; `rating` and `protections` unchanged; `RatingDefaults` unchanged.
- **DM-0.2-05** `ShownView.of`: `(BOOKSHELF, absent)` → BOOKSHELF; `(BOOKSHELF, LIST)` → list; `(LIST, CARD)` → list and no toggle; unknown stored style string → `BOOKSHELF`. A 0.1.0 settings record with `viewByScreen = {library: LIST, cast: CARD}` and no new fields → 서재 shows `BOOKSHELF`, the cast list `FAN_DECK`; the old map is written back unchanged.
- **DM-0.2-06** `Tilt.follows(viewTilt, motion)`: `(true, FOLLOW_SYSTEM + system reduce on)` → false; `(true, FOLLOW_SYSTEM + system reduce off)` → true; `(true, ALWAYS_REDUCE)` → false; `(true, NEVER_REDUCE + system reduce on)` → true (CNV-46 resolution); `(false, any)` → false.
- **DM-0.2-07** Versions: capture a unit of 5 blocks → 5 `VersionBlock`s in manuscript order; edit block 3 and trash block 4; `이 판 글로` on block 3 → its runs equal the version's and its key, number, `voice`, ◆ records are unchanged; block 4 offers no `이 판 글로`; one undo restores block 3's edited text.
- **DM-0.2-08** Versions and files: manual export with default options contains `versions/<unitKey>.json` and the manifest lists `versions`; with the row off neither is present; the backup writer's output always contains them; import of a file without `versions/` → no versions, no error.
- **DM-0.2-09** Export creates a version: a CSV export of `1장 전체` with episodes 1-1 … 1-3 adds exactly one `origin: export` version to each of those three units, named `<M>월 <D>일 · 내보낸 판`; a `.civilight` export adds none.

### 13.9 Rows to fold into the body later

- §1.7 glossary: `EpisodeVersion` · 판 · — · `UndoStep` (undo is session-only).
- §2.1 `Project` table gains `gameTools`; §7.4 `project.json` gains `gameTools?: boolean`.
- §2.13 `AppSettings` gains `libraryStyle`, `castCardStyle`, `libraryToggle`, `castToggle`, `viewTilt`; `ProjectActivity` gains `lastCanvasKey`.
- §7.2 layout gains `versions/<unit key>.json` (feature `versions`); §7.3 feature table gains `versions` · optional.
- §9 store mapping gains `versions/<unitKey>.json`.
- §10 conflict rows:
  - **C-34** N-29 storage: scope D20 (project document, travels in every `.civilight`) vs the computed task (working store, `.civilight` only when chosen). Resolution §13.5: working store, Drive backup always, manual export as a row that defaults on while Drive is a stub.
  - **C-35** `viewByScreen["library"]` seeded `LIST` (0.1.0 code, persisted by `encodeDefaults = false` whenever the map changed) vs U-16 (3D view default). Resolution §13.4: new fields `libraryToggle`, `castToggle` (absent = 3D); the old entries are kept and not read.
  - **C-36** Format version rule §8.1 (minor on any addition) vs D29 (move only when the upgrade chain needs it). Resolution §13.1: stay 1.0.
