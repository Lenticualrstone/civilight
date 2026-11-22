# fixture

The canonical example project 「잿빛 새벽의 등불」 (R-15) as Kotlin code in `core:model`, package `io.github.lenticualrstone.civilight.model.sample`.

- `ExampleProject.build(): ProjectDoc` builds the whole project. `ExampleProject.libraryEntry()` gives the 서재 row (`isExample`, `readOnly`).
- `ExampleKeys` holds fixed ULID-shaped keys: named constants, plus family functions `scene(c, e, s)`, `block(c, e, n)`, `character(recordNo)` and others.
- `ExampleClock` holds fixed timestamps. `NOW` = 2026-11-22 09:00 Asia/Seoul.
- `ExampleRecorder` holds the recorder library slice: `library()`, `projectFile()` (the project's `recorder.json`) and `trashEntries()`.
- Everything is deterministic, so `build() == build()`.
- Live and trashed rows sit together. In every list the non-live rows come last, so `ProjectFileSet.split(doc).toDoc(app = doc.app) == doc`.

## Done

Sources (`core/model/src/main/kotlin/io/github/lenticualrstone/civilight/model/sample/`):

| File | Holds |
| --- | --- |
| `ExampleKeys.kt` | key constants and families |
| `ExampleSupport.kt` | `ExampleClock` and text/doc builders |
| `ExampleSettings.kt` | `project.json`: settings, 13 field definitions, 4 tags, 22 relation kinds, travel modes |
| `ExampleStory.kt` | story tree and scene fields |
| `ExampleScript.kt` | blocks and voice IDs |
| `ExampleCast.kt` | `cast.json` |
| `ExampleWorld.kt` | `world.json` |
| `ExampleMaps.kt` | the three maps |
| `ExampleMapResults.kt` | the stored `generation.result` JSON of the two generated maps (real `gen-1` output, see Maps below) |
| `ExampleChanges.kt` | `changes.json` |
| `ExampleExtras.kt` | canvas, pages, assets, checks, trash, deletion log, `_app` state |
| `ExampleRecorder.kt` | recorder library slice |
| `ExampleProject.kt` | `build()` |

Tests (`core/model/src/test/kotlin/io/github/lenticualrstone/civilight/model/sample/`): 32 tests, all passing. `tools/gw :core:model:test` passes with 123 tests (87 model + 32 fixture + 4 `PlaceAliasesJsonTest`), 0 failures.

**`ExampleProjectInvariantsTest`**

| Test | Checks |
| --- | --- |
| `buildIsDeterministic` | two builds are equal |
| `ratingAndProjectSettings` | R-15, structure, start year 1200, protections off, 도보/말/배 with fixed keys |
| `everyKeyIsAValidUlidAndUnique` | every identity key (rows, options, cues, doc paragraphs, link keys, recorder rows) is a valid ULID and globally unique |
| `namedConstantsMatchTheirFamilies` | named constants equal what the family functions build |
| `orderKeysAreValid` | all order keys are valid and no two siblings share one |
| `treeLevelsAndNumberingUnits` | levels are consistent; every scene's numbering unit is its episode; every block sits in the right episode file; `regroupBlocks()` changes nothing |
| `blockNumbersAreUniquePerNumberingUnitAndBelowTheCounter` | numbers are unique per unit and below `nextBlockNumber`; block keys encode the number; discarded IDs point at purged blocks; purged numbers are not reused |
| `exactlyOneLiveTermRevealPerTerm` | one live reveal per term |
| `trashMarksMatchTrashEntries` | direct marks match `TrashEntry` rows, purge dates are +30 local days, recorder trash is consistent |
| `allReferencesResolve` | every reference resolves (see below) |

`allReferencesResolve` checks:

- **Story and script:** scene places and regions, time bases and flashback events, silent cast, block scenes, speakers, cue assets, choice options.
- **Link runs:** each target resolves, the kind is allowed on its surface (script, document, recorder), and `projectKey` appears only in recorder text.
- **Fields:** field-map keys match the field's entity kind, choice options exist, link targets exist, payload type matches the field type.
- **Cast:** relation ends, slot kinds per category, one relation per pair, voice lines, archive documents and their unlock points.
- **World:** term `관련`, place parents, event scenes, places, terms and reveal points.
- **Maps:** parent pins, pin places, one pin per place, region faction and layer, route pins, stamps, nodes and node links, scale travel mode.
- **Records:** targets, typed values (FieldValue, SlotValue with the right kind category, address String, RegionShapeValue) and anchors.
- **Canvas, pages, assets, acknowledgements:** canvas nodes and relation edges, page recorder sources, image uses, asset places, acknowledgement subjects.
- **Recorder:** link targets and placements.

**`ExampleProjectRoundTripTest`**

| Test | Checks |
| --- | --- |
| `wholeProjectSurvivesSplitEncodeDecode` | `ProjectFileSet.split` → `encode` → JSON text → `decode` (no failed or unknown paths) → `toDoc` equals the doc, unit by unit and as a whole |
| `liveFilesHoldOnlyLiveRowsAndTheTrashMirrorTheRest` | trashed rows sit only in the trash mirror, and the set of episode files is right |
| `eachFileUnitIsCanonicalJson` | re-encoding gives identical JSON |
| `recorderSliceRoundTrips` | library file and project `recorder.json` round-trip |

**`ExampleProjectFactsTest`**

Asserts the headline facts below: the 집필 선, structure counts, 1-7 씬 3 blocks, relation and name by lens, 소속 history, the 예정 set, script panel rows, term reveals and pre-reveal use, 노아's references, cast facts, Fixture T time inputs, and the 38 km route.

**`ExampleProjectContentTest`**

| Test | Checks |
| --- | --- |
| `relationValuesByKindName` | relation values by kind name: `경계` + description, `보호`, `동료`, `신뢰`, `꼬마`, `하르트`, 선배↔후배 held by 세라, `의지`, `거래 상대`; 사이 11 · 마음 11 |
| `voiceIdOfHartsLine` | `HRT_0107_043` and the discarded `RIE_0102_007` |
| `everySceneUpToTheWritingLineHasContentAndNoneAfter` | content exists in every live scene up to 1-7 씬 3 and in none after |
| `rienFirstAppearsIn12` | 리엔 does not appear in 1-1 |
| `termWordsAppearAsPlainTextOnlyInTheCandidateBlocks` | `오리지`, `빛돌`, `대정전`, `잿불`, `송전망` appear as plain manuscript text only in b12, b32 and b61 |
| `everyNamedKeyConstantIsUsed` | every named `ExampleKeys` constant is in the fixture (by reflection) |

### Facts for test writers

Clock: `ExampleClock.NOW` = 2026-11-22T00:00:00Z (09:00 KST).

**Project**
- Name 「잿빛 새벽의 등불」, rating R-15, `연재 장편`, protections all off.
- Structure `장 › 에피소드 › 씬`. World time `연도만 · 시작 1200년`. Scene-time settings default (episode notation, 04:00 day, bands 04 · 06 · 10 · 17 · 20).
- Script settings: voice IDs on, 무대 위치 off, transition default `없음`, template `장소 · 시각`, pattern `{code}_{chapter2}{episode2}_{number3}`.
- Travel modes `TRAVEL_WALK` 도보 15/8, `TRAVEL_HORSE` 말 40/8, `TRAVEL_SHIP` 배 30/24. Default 도보 (`defaultTravelModeKey` null).
- `fieldBundleAtCreation = gameProfile`, `cast.bundleName.character = "게임 프로필"`, `nextCharacterRecordNo = 13`.
- Cover: `IMAGE_PROJECT_COVER` = `preset:ash-dawn`.

**Structure.** 1장 `등불지기` has 1-1 … 1-10; 2장 `잿불` has 2-1 … 2-8 (scenes only in 2-3 and 2-5). 1장 has 31 live scenes: 22 with a place, 19 with 때. The project has 34 live scenes. 1-6 씬 5 `시장의 밤` is in the trash (2026-11-04, `12일 남음`).

Episode titles: 1-1 잿빛 거리, 1-2 불빛을 세는 아이, 1-3 결사의 문, 1-4 그날의 빛, 1-5 갈림길, 1-6 잿더미 시장, 1-7 잿빛 새벽의 등불. 1-8, 1-9 and 1-10 have no title. 1-9 and 2-5 have synopses.

**집필 선.** End of 1-7 씬 3 (`SCENE_1_7_3`). Its last block is #048. Lens chip at 최신 = `최신 · 1-7`. 1-7 씬 4 has no blocks. 1-8 씬 1 has one empty block #001. Every scene up to 1-7 씬 3 has content.

**Scenes** (Fixture T ids in brackets; `기준` = base scene, interval, band or clock):

| Scene | Title | Place | 때 | Blocks | Speakers · silent cast |
| --- | --- | --- | --- | --- | --- |
| 1-1 씬 1 [S1] | 첫 순찰 | 하층 거리 | 직접 · 1일째 | #001–#004 (#001 = `◇ 등불 결사`) | 세라, 테오 |
| 1-1 씬 2 | 오웬의 노점 | 잿더미 시장 | 1-1 씬 1 · 같은 날 · 저녁 | #005–#007 | 고든, 오웬 |
| 1-1 씬 3 | 결사의 문지기 | 결사의 문 | 1-1 씬 2 · 같은 날 · 밤 | #008–#011 | 마르타, 테오 |
| 1-2 씬 1 [S2] | 새벽 순찰 | 하층 거리 | 1-1 씬 1 · +1일 · 새벽 | #001–#004 (b12 = #003) | 세라, 리엔 |
| 1-2 씬 2 | 고아원의 지붕 | 하층 거리 | — | #005, #006, #008 (#007 purged) | 리엔 · 노아 |
| 1-2 씬 3 | 빛 한 줌 | 잿더미 시장 | — | #009–#011 | 고든, 리엔, 오웬 |
| 1-3 씬 1 [S3] | 등불 결사의 소문 | — | — | #001–#003 | 테오, 리엔 |
| 1-3 씬 2 [S4] | 결사 합류 | 결사의 문 | 1-2 씬 1 · +1일 · 아침 · transition 자동 | #004–#008 (b32 = #007) | 마르타, 리엔, 세라 |
| 1-3 씬 3 | 첫 등불 | 결사의 문 | — | #009–#011 | 마르타, 리엔 |
| 1-4 씬 1 | 결사의 아침 | 결사의 문 | — | #001–#003 (#001 ▸ 배경 결사의 문(아침)) | 리엔, 세라 |
| 1-4 씬 2 [S5] | 세라의 회상 | 하층 거리 | 회상 · 대정전 · 그날 | #004 b41 (`◇ 대정전`), #005 b42 (l42 `▲`), #006 | 세라 · 리엔 |
| 1-4 씬 3 | 기록실의 먼지 | 기록실 | 1-3 씬 2 · +1일 · 저녁 | #007–#009 | 세라, 리엔 |
| 1-5 씬 1 | 하층 구역 순찰 | 지역 하층 구역 | 1-4 씬 3 · +1일 · 새벽 06:20 | #001–#003 | 테오, 세라 |
| 1-5 씬 2 | 두 갈래 길 | 하층 거리 | 1-5 씬 1 · 같은 날 · 아침 | #004–#007 (#006 choice); #008, #009 in trash (2026-11-15) | 리엔, 세라 |
| 1-5 씬 3 | 등불 수리 | — | 1-5 씬 2 · +1일 · 낮 | #010–#013 | 도르, 리엔 · 노아 |
| 1-5 씬 4 | 기록실의 저녁 | 기록실 | 1-5 씬 3 · 같은 날 · 저녁 | #014 system (with `줄바꿈`), #015–#017 | 세라, 리엔 |
| 1-6 씬 1 [S6] | 잿더미 시장 | 잿더미 시장 | 1-3 씬 2 · 대략 +5~9일 · 낮 | #001–#004 (b61 = #003) | 리엔, 오웬 |
| 1-6 씬 2 | 노아의 심부름 | — | — | #005 ▸ 배경 잿더미 시장(밤), #006 `[노아]가…`, #007–#014 | 노아 ×6, 오웬 |
| 1-6 씬 3 | 시장의 저녁 | — | — | #015–#017 | 고든, 도르 |
| 1-6 씬 4 | 오웬의 장부 | 잿더미 시장 | — | #018–#029 | 노아 ×8, 오웬 |
| 1-6 씬 5 (trash) | 시장의 밤 | 잿더미 시장 | — | #030–#032 (incl. `노아가 웃었다.`) | 노아, 오웬 |
| 1-7 씬 1 [S7] | 첫 번째 깜박임 | 하층 거리 | 1-6 씬 1 · +2일 · 새벽 | #001–#017 (#001 fade + SFX 바람, #008 CG) | 리엔 |
| 1-7 씬 2 [S8] | 상층 관문 | 상층 관문 | 1-7 씬 1 · 같은 날 · 저녁 | #018–#035 (#027 no speaker) | 카일, 하르트, 이레나 · 엘리오 |
| 1-7 씬 3 [S9] | 꺼진 등불 | 결사의 문 | 1-7 씬 2 · +1일 · 05:40 | #036–#048 | 세라, 오웬, 리엔, 하르트 · 마르타 |
| 1-7 씬 4 [S10] | (미정) | 결사의 문 | 1-7 씬 3 · 같은 날 · 06:10 · 직접 `결사의 문 앞 · 06:10` | none | — |
| 1-8 씬 1 [S11] | 송전탑 아래 | 송전탑 폐허 | 1-7 씬 4 · +2일 · 새벽 | #001 (empty) | — |
| 1-8 씬 2 | (미정) | — | — · 직접 `기록실 · 같은 날 밤` | none | — |
| 1-9 씬 1 | (미정) | — | — | none | — |
| 1-9 씬 2 [S12] | 다시 관문 | 상층 관문 | 직접 · 20일째 · 낮 | none | — |
| 1-10 씬 1–3 | (미정) | — | 씬 1: 1-9 씬 2 · +3일 · 밤 | none | — |
| 2-3 씬 1, 2-3 씬 2 | —, 경비대 검문 | —, 하층 거리 | — | none | 2-3 씬 2: 하르트 (silent) |
| 2-5 씬 1 | — | — | — | none | — |

All Fixture T times hold with exact intervals except S6, which is `대략 +5~9`. S9 and S10 have a clock and no band.

**Block counters** (`StoryUnit.nextBlockNumber`): 1-1 12, 1-2 12, 1-3 12, 1-4 10, 1-5 18, 1-6 33, 1-7 58, 1-8 61; 1 for every other episode.

**1-7 씬 3** (SCR F1 text):

| Block | Kind | Content |
| --- | --- | --- |
| #036 | 세라 | `불은 꺼졌어. 기록도 거기서 끝나.` |
| #037 | 오웬 | `기록 따위로 배가 부르진 않지.` |
| #038–#040 | 나레이션 | — |
| #041 | 연출 | 배경 하층 거리(새벽), BGM 잔불 |
| #042 | 리엔 | `(작게)` + `저기… 불빛이 보여요. 누가 아직 남아 있는 걸까요?` |
| #043 | 하르트 | `멈추지 마. 저건 등불이 아니야.` (ordinal 7 in the scene) |
| #044 | 나레이션 | `바람이 잿더미를 쓸고 지나갔다. 꺼진 줄 알았던 [오리지늄 등불]이 한 번 깜박였다.` (link `LINK_L44`) |
| #045 | 리엔 | `(숨을 삼키며)` + `하르트 씨, 방금…` |
| #046 | 작가 메모 | `여기서 등불이 두 번째로 깜박이게 할지` |
| #047 | 하르트 | `뒤돌아보지 말고 따라와.` |
| #048 | 리엔 | `하지만 저 불빛은 결사의 신호였어요.` |

Scene memo `등불이 두 번째로 깜박이는 타이밍`. Silent cast 마르타. All voice IDs automatic (`HRT_0107_043`).

**Cast.** 12 rows, 11 live. Record numbers: 마르타 1 MRT, 세라 2 SRA, 오웬 3 OWN, 하르트 4 HRT, 카일 5 KYL, 이레나 6 IRN, 리엔 7 RIE, 노아 8 NOA (trash), 도르 9 DOR, 테오 10 TEO, 엘리오 11 ELI, 고든 12 GDN.

Tags: 주연 = 리엔, 하르트, 세라. 등불 결사 = 리엔, 세라, 마르타, 테오. 하층 구역 = 오웬, 도르, 고든 (+ 노아). 상층 = 카일, 이레나, 엘리오.

- **리엔:** one-liner `막내 등불지기`. 출생 1183, 생일 3/14, 신장 154 (cm, export name `height`), 성별 여, 종족 인간, 코드네임 작은 등, 출신지 → 하층 거리, 특기 등불 수리 · 길 찾기. No base 소속. First appearance 1-2.
- **하르트:** `displayNameBase = "???"`. 출생 `??` and 나이 `??` (state unknown). 신장 183.
- **세라:** 기록관, 출생 1179, 소속 등불 결사.
- **마르타:** 결사장, 출생 1158, base 소속 `상층 경비대`, then `등불 결사` from event 대정전 (W1). 특기 includes 성직자.
- **노아:** 나이 entered `11`. 출생 and 성격 empty.
- **Fields:** 성별 `여` is held by 리엔, 세라, 마르타, 이레나 (`리엔 외 3명`). Every character has 성별.
- **Field definitions:**
  - Character, in order: 출생 · 나이 (calc from 출생) · 생일 · 소속 (`짧은 글`) · 신장 · 성별 · 종족 · 성격 · 코드네임 · 출신지 (`연결` → 장소) · 특기.
  - Place field 상태: 하층 거리 base `빛 있음`, then `대정전 이후` from event 대정전 (W2).
  - Faction field 수장: 등불 결사 마르타, 상층 경비대 카일.

**Relations** (base; records below):
- `REL_RIEN_HART`: empty base.
- `REL_RIEN_SERA`: 사이 선배↔후배, 세라 holds 선배. 리엔's 마음 의지 `의지하는 선배`. 리엔's 호칭 `세라 씨`.
- `REL_RIEN_OWEN`: 사이 거래 상대.
- `REL_SERA_HART`: 사이 동료.
- `REL_OWEN_NOAH`: 노아's 호칭 `아저씨`. Not live while 노아 is in the trash.

At lens 1-7 four relations are live.

**Voice lines and archive documents**
- 리엔 has 5 voice lines; `신뢰 상승 후` and `전투 시작` are empty (`▲ 보이스 비어 있음`). 하르트 has 1.
- Archive: 리엔 `등불지기의 일지` (unitEnd 1-7), `대정전 그날` (empty, unitEnd 2장); 하르트 `하르트의 수첩` (unitEnd 1-9).

**Change records**

| Key | Target · value | Anchor | 예정 at 최신? |
| --- | --- | --- | --- |
| R1 | 리엔 소속 `없음` (note 하층 구역의 고아) | unitStart 1-1 | no |
| R2 | 리엔 소속 `등불 결사` (note 세라의 안내로 합류) | unitStart 1-3 | no |
| R3 | 리엔 소속 `등불 결사 · 등불지기장` | unitStart 2-5 | yes |
| R4 | 리엔의 마음 → 하르트 `경계` (desc `불빛을 아는 낯선 사람`, note 처음 만남) | block #043 | no |
| R5 | 하르트의 마음 `보호` | block #043 | no |
| R5A | 하르트's 호칭 for 리엔 `꼬마` | block #043 | no |
| R6 | 사이 `동료` | unitStart 1-9 | yes |
| R7 | 리엔의 마음 `신뢰` | unitStart 1-9 | yes |
| R7A | 리엔's 호칭 `하르트` | unitStart 1-9 | yes |
| R8 | 하르트 표시 이름 cleared (value absent = show 이름) | unitStart 1-9 | yes |
| R9 | `◇` 오리지늄 등불 | #044 | no |
| `REVEAL_BLACKOUT` | `◇` 대정전 | b41 | no |
| `REVEAL_LANTERN_SOCIETY` | `◇` 등불 결사 | 1-1 #001 | no |
| `REVEAL_EMBER_OATH` | `◇` 잿불의 서약 | unitStart 2-5 | yes |
| `REVEAL_UPPER_GRID` | `◇` 상층 송전망 | unitStart 2장 | yes |
| W1 | 마르타 소속 `등불 결사` | worldEvent 대정전 | no |
| W2 | 하층 거리 상태 `대정전 이후` | worldEvent 대정전 | no |
| `REGION_SHAPE_1_9` | 상층 경비대 shape S2 | unitStart 1-9 | yes |
| N1 | 노아 소속 `잿더미 시장 심부름꾼` | 1-6 #007 | not live (target trashed) |
| N2 | 오웬의 마음 → 노아 `보호` | 1-6 #025 | not live |
| `RECORD_TRASHED` | 세라 코드네임 `기록관` | unitStart 1-5 | in the trash since 2026-11-18 |

All anchors are `normal`.

Expected readings:
- 리엔→하르트 at lens 1-6 is `만나기 전`.
- At 1-7, 1-8 and 최신: `→ 경계 · ← 보호 · 꼬마`.
- At 1-9: 사이 동료, `→ 신뢰 · ← 보호`, 호칭 하르트, display name 하르트.
- Script panel: before #042 and before #043 the value is `만나기 전`. `여기서` at #043 shows R4, R5, R5A. Before #044 the value is `경계`.

**World**
- **Terms:** 오리지늄 등불 (aliases 오리지늄 · 빛돌; `관련` 등불 결사 (세력) · 대정전), 대정전, 등불 결사, 잿불의 서약, 상층 송전망. None is hidden or trashed; there are no rejections.
- **Places:** 하층 거리, 결사의 문, 잿더미 시장, 상층 관문 (parent 상층), 송전탑 폐허, 기록실 (parent 결사의 문), 상층.
- **Factions:** 등불 결사, 상층 경비대.
- **Events:** 대정전 (world, 1195, place 하층 거리, terms 오리지늄 등불 · 대정전); 상층 송전망 사고 (world, 1201, reveal 2장 start); `리엔, 결사 합류` (story, 1-3 씬 2).
- **Link runs to 오리지늄 등불:** l42 in b42 (`▲ 공개 전 사용`), l44 in #044, lpg on page `하층 구역의 빛` (`원고 밖`).
- **Candidates of 오리지늄 등불:** b12 `오리지` (끊김), b32 `빛돌`, b61 `빛돌`. All are before the reveal.

**Maps**

*Generated maps (both):* `generatorVersion = gen-1`, steps 해안선 (locked) · 산 · 강 · 숲·기후 · 마을·길 with no choices and no text conditions. Since 2026-11-22 `generation.result` is the real `gen-1` output of `domain:world` (`TerrainGenerator.generate` + `GenResults.encode`) for the stored kind, land ratio and seeds, stored as JSON text in `ExampleMapResults.kt`; before, it was a minimal stand-in (`towns` a number, `coastline` a flat point list) that `MapGenerator.storedWorld` read as null although `StepReroll` accepted it as the same version (domain-world request). `domain:world` `FixtureGenResultsTest` checks each result equals a fresh generation; regenerate the file if the generator or the stored inputs change. The maps' overlays (세계 지도's hand-made 6×4 hex cells, 하층 구역 지도's drawn road and stamps) are unchanged and are not generator output.

*세계 지도* (`MAP_WORLD`; hex 6×4, generated 대륙 · 반반 with seeds 4827 · 112 · 31 · 907 · 215 (6 towns, 4 rivers in the stored result), scale 400 km per 0.8)
- Pins: 상층 `PIN_WORLD_UPPER_CITY`; 송전탑 폐허, revealed from 2-3.
- Region 상층 경비대 (faction, layer 정치, from 1-7), with a `◆` shape at 1-9.

*하층 구역 지도* (`MAP_LOWER_DISTRICT`; child of pin 상층, detail, generated 한 지역 · 땅 많음 with seeds 5120 · 77 · 3021 · 18 · 640 (5 towns, 3 rivers in the stored result))
- Scale `2일` 도보 between 결사의 문 (0.20, 0.40) and 잿더미 시장 (0.45, 0.40), so 120 km per unit.
- Pins: 결사의 문 (no point), 하층 거리 (0.22, 0.43; from 1-1), 잿더미 시장 (from 1-3), 기록실 (0.26, 0.36; from 1-5), 상층 관문 (0.416667, 0.30; from 1-7).
- Region 하층 구역 (from 1-1; contains every pin except 상층 관문).
- Routes:
  - 결사의 문 → (0.20, 0.30) → 상층 관문: length 0.316667, 38 km.
  - 결사의 문 → (0.33, 0.44) → 잿더미 시장.

*잿더미 시장* (`MAP_ASH_MARKET`; node map, child of pin 잿더미 시장)
- 3 nodes, 2 links.

**Canvas and pages**
- Canvas `1장 구상` has nodes 등불 결사 (세력), 리엔, 하르트, 대정전 (사건), 오리지늄 등불 (용어) and `2장 떡밥?` (메모). Edges: 리엔—하르트 (`relationKey` `REL_RIEN_HART`) and 대정전 → 오리지늄 등불 (`그날 꺼진 빛`).
- Page `하층 구역의 빛` (설정 노트; created before the manuscript): lpg paragraph, a paragraph with an `@노아` link, and a memo `2장에서 값이 두 배로 뛰는 장면 넣기. 노아 시점으로?` (plain text).
- Page `등불 결사 규약` (세력): made from recorder R0 (`sourceRecord`, a `⌁` paragraph).

**노아's references** (TC-20-01 numbers)
- 14 live dialogue lines: 1-6 씬 2 ×6, 1-6 씬 4 ×8.
- 2 silent-cast appearances (1-2 씬 2, 1-5 씬 3).
- 3 live `@` links: 1-6 #006, 오웬's 비밀, page `하층 구역의 빛`.
- 1 recorder chip (`RECORDER_NOAH`).
- 1 relation (오웬) and 2 `◆` (N1, N2).
- More lines in trashed 1-6 씬 5.
- In the trash since 2026-10-26 (`3일 남음`).

**Trash**

| Entry | Kind | Holds | Deleted | Purge on |
| --- | --- | --- | --- | --- |
| `TRASH_NOAH` | character | 노아 | 2026-10-26 | 11-25 |
| `TRASH_SCENE_1_6_5` | scene | 1-6 씬 5 | 2026-11-04 | 12-04 |
| `TRASH_BLOCKS_1_5_2` | blockGroup | 1-5 #008, #009 (`trashPlacement` prev #007, ordinals 4/5) | 2026-11-15 | 12-15 |
| `TRASH_RECORD` | change | `RECORD_TRASHED` | 2026-11-18 | 12-18 |

Recorder trash: a voice record of 0:12, deleted 2026-10-24, purged 2026-11-23. The deletion log holds 10 purged blocks.

**Checks**

Acknowledged (`맞다고 표시한 것 2`; fingerprints are the real values of the `domain:story` `Fingerprints` scheme, computed by `domain:checks`, so both stay acknowledged):
- `ACK_TRAVEL_HART`: `travel.shortage` [하르트, 1-7 씬 2, 1-7 씬 3]. 도보 3,136 min needed > 760 available. Fingerprint `sha256-1f4bd89577870ca8310efdafc13f2015eb611d792c8d1bfe4310cc4f5e192f26`.
- `ACK_CLOCK_BAND`: `time.clockBandMismatch` [1-5 씬 1]. Fingerprint `sha256-abbd7af894cac54d25b10f6cd6059928464eec05286a01f9383afba6a3f65d11`.

Open items a correct engine should raise at 최신 with default settings:

| Item | Where |
| --- | --- |
| ▲ 공개 전 사용 | 1-4 씬 2 (l42) |
| ◇ 공개 전 후보 | 1-2 씬 1 (b12), 1-3 씬 2 (b32), 1-6 씬 1 (b61) |
| ▲ 화자 없음 | 1-7 씬 2 #027 |
| ◇ 지운 인물 · 노아 | — |
| ▲ 보이스 비어 있음 | 인물 · 리엔 |

Depending on the `장소 없는 씬 알림` setting, there may also be `◇ 장소 없음` for timed scenes without a place in a character's track (1-5 씬 3).

No other check is expected to fire: no other travel `▲`, no time-order mismatch, no stale transition, no hidden items. This rests on hand calculation of the travel pairs and times; `domain:checks` has not confirmed it.

**Recorder** (`ExampleRecorder`; link `projectKey` = the project)

| Record | Kind · time | Text / state | Links |
| --- | --- | --- | --- |
| R0 | text, 2026-11-05 | — | page 등불 결사 규약 (insert); PAGE placement |
| R1 | voice 0:24, 2026-11-22 08:41 KST | LRC text | 1-7 씬 3 (unit, entry), 하르트 (manual) |
| R2 | text, 11-21 23:10 KST | `@세라` run with `projectKey` | project (entry), 오웬 (manual), 세라 (at) |
| R3 | voice 1:02, 11-21 08:15 KST | — | none |
| R4 | text, 11-18 | 끝냄 | none |
| `RECORDER_NOAH` | text, 10-20 | — | 노아 (manual) |
| `RECORDER_TRASHED` | voice 0:12 | in trash | — |

`projectFile()` holds R0, R1, R2 and `RECORDER_NOAH`.

**App state.** Last edit at the end of #044 (`lastEditedBlockKey = BLOCK_044`, offset = its text length), at 2026-11-22 08:12 KST. Lens `최신`.

### Key constants

Families: `chapter(c)`, `episode(c, e)`, `scene(c, e, s)`, `block(c, e, n)`, `character(recordNo)`, `fieldDef(n)`, `fieldOption(def, n)`, `tag(n)`, `relationKind(n)`, `relation(n)`, `voiceLine(n)`, `archiveDoc(n)`, `term(n)`, `place(n)`, `faction(n)`, `event(n)`, `map(n)`, `pin(n)`, `region(n)`, `route(n)`, `mapLayer(n)`, `mapLabel(n)`, `stamp(n)`, `drawnLine(n)`, `mapNode(n)`, `mapNodeLink(n)`, `canvas(n)`, `canvasNode(n)`, `canvasEdge(n)`, `page(n)`, `docBlock(n)`, `record(n)`, `link(n)`, `scriptAsset(n)`, `directionCue(n)`, `choiceOption(n)`, `imageUse(n)`, `acknowledgement(n)`, `trashEntry(n)`, `travelMode(n)`, `recorderRecord(n)`, `recorderLink(n)`, `placement(n)`. Unnamed blocks: `ExampleKeys.block(1, 6, 19)` etc.

Link keys:

| Key | Link |
| --- | --- |
| `link(1)` | l44 |
| `link(2)` | l42 |
| `link(3)` | lpg |
| `link(4)` | 1-1 #001 → 등불 결사 |
| `link(5)` | b41 → 대정전 |
| `link(6)` | 1-6 #006 → 노아 |
| `link(7)` | page → 노아 |
| `link(8)` | 오웬 비밀 → 노아 |
| `link(9)` | R2 → 세라 |
| `link(10)` | 하르트 비밀 → 상층 송전망 |
| `link(11)` | 등불 결사 description → 마르타 |

Named constants (`ExampleKeys.<NAME>` = value):

**Project**

- `PROJECT` = `01JZ000000PRJT000000000001`

**Story structure**

- `CHAPTER_1` = `01JZ000000CHAP000000000001`
- `CHAPTER_2` = `01JZ000000CHAP000000000002`
- `EPISODE_1_1` = `01JZ000000EPSD000000000101`
- `EPISODE_1_2` = `01JZ000000EPSD000000000102`
- `EPISODE_1_3` = `01JZ000000EPSD000000000103`
- `EPISODE_1_4` = `01JZ000000EPSD000000000104`
- `EPISODE_1_5` = `01JZ000000EPSD000000000105`
- `EPISODE_1_6` = `01JZ000000EPSD000000000106`
- `EPISODE_1_7` = `01JZ000000EPSD000000000107`
- `EPISODE_1_8` = `01JZ000000EPSD000000000108`
- `EPISODE_1_9` = `01JZ000000EPSD000000000109`
- `EPISODE_1_10` = `01JZ000000EPSD000000000110`
- `EPISODE_2_3` = `01JZ000000EPSD000000000203`
- `EPISODE_2_5` = `01JZ000000EPSD000000000205`
- `SCENE_1_1_1` = `01JZ000000SCNE000000010101` — Fixture T S1: 1-1 씬 1 `첫 순찰`.
- `SCENE_1_1_2` = `01JZ000000SCNE000000010102`
- `SCENE_1_1_3` = `01JZ000000SCNE000000010103`
- `SCENE_1_2_1` = `01JZ000000SCNE000000010201` — Fixture T S2: 1-2 씬 1 `새벽 순찰` (holds b12).
- `SCENE_1_2_2` = `01JZ000000SCNE000000010202`
- `SCENE_1_2_3` = `01JZ000000SCNE000000010203`
- `SCENE_1_3_1` = `01JZ000000SCNE000000010301` — Fixture T S3: 1-3 씬 1, no place, no 때.
- `SCENE_1_3_2` = `01JZ000000SCNE000000010302` — Fixture T S4: 1-3 씬 2 `결사 합류` (holds b32; story event 결사 합류).
- `SCENE_1_3_3` = `01JZ000000SCNE000000010303`
- `SCENE_1_4_1` = `01JZ000000SCNE000000010401`
- `SCENE_1_4_2` = `01JZ000000SCNE000000010402` — Fixture T S5: 1-4 씬 2 `세라의 회상`, 회상 · 대정전 그날 (holds b41, b42).
- `SCENE_1_4_3` = `01JZ000000SCNE000000010403`
- `SCENE_1_5_1` = `01JZ000000SCNE000000010501`
- `SCENE_1_5_2` = `01JZ000000SCNE000000010502`
- `SCENE_1_5_3` = `01JZ000000SCNE000000010503`
- `SCENE_1_5_4` = `01JZ000000SCNE000000010504`
- `SCENE_1_6_1` = `01JZ000000SCNE000000010601` — Fixture T S6: 1-6 씬 1 `잿더미 시장` (holds b61).
- `SCENE_1_6_2` = `01JZ000000SCNE000000010602`
- `SCENE_1_6_3` = `01JZ000000SCNE000000010603`
- `SCENE_1_6_4` = `01JZ000000SCNE000000010604`
- `SCENE_1_6_5` = `01JZ000000SCNE000000010605` — 1-6 씬 5 `시장의 밤`, in the trash since 2026-11-04.
- `SCENE_1_7_1` = `01JZ000000SCNE000000010701` — Fixture T S7: 1-7 씬 1 `첫 번째 깜박임`.
- `SCENE_1_7_2` = `01JZ000000SCNE000000010702` — Fixture T S8: 1-7 씬 2 `상층 관문`.
- `SCENE_1_7_3` = `01JZ000000SCNE000000010703` — Fixture T S9: 1-7 씬 3 `꺼진 등불`, 결사의 문, 2일째 새벽 05:40; the 집필 선 is its end.
- `SCENE_1_7_4` = `01JZ000000SCNE000000010704` — Fixture T S10: 1-7 씬 4 (미정), 결사의 문, 06:10, 직접 전환 문구; no blocks.
- `SCENE_1_8_1` = `01JZ000000SCNE000000010801` — Fixture T S11: 1-8 씬 1 `송전탑 아래`; one empty block #001.
- `SCENE_1_8_2` = `01JZ000000SCNE000000010802`
- `SCENE_1_9_1` = `01JZ000000SCNE000000010901`
- `SCENE_1_9_2` = `01JZ000000SCNE000000010902` — Fixture T S12: 1-9 씬 2, 직접 · 이야기 20일째 · 낮.
- `SCENE_1_10_1` = `01JZ000000SCNE000000011001`
- `SCENE_1_10_2` = `01JZ000000SCNE000000011002`
- `SCENE_1_10_3` = `01JZ000000SCNE000000011003`
- `SCENE_2_3_1` = `01JZ000000SCNE000000020301`
- `SCENE_2_3_2` = `01JZ000000SCNE000000020302` — 2-3 씬 2 `경비대 검문` (planned, 하르트).
- `SCENE_2_5_1` = `01JZ000000SCNE000000020501`

**Blocks of 1-7 씬 3 (SCR fixture F1) and named blocks of WLD fixture F**

- `BLOCK_036` = `01JZ000000BKNM010700000036`
- `BLOCK_037` = `01JZ000000BKNM010700000037`
- `BLOCK_038` = `01JZ000000BKNM010700000038`
- `BLOCK_039` = `01JZ000000BKNM010700000039`
- `BLOCK_040` = `01JZ000000BKNM010700000040`
- `BLOCK_041` = `01JZ000000BKNM010700000041` — Direction: 배경 `하층 거리(새벽)`, BGM `잔불`.
- `BLOCK_042` = `01JZ000000BKNM010700000042` — 리엔 `(작게) 저기… 불빛이 보여요. …` (WLD b7342).
- `BLOCK_043` = `01JZ000000BKNM010700000043` — 하르트 `멈추지 마. 저건 등불이 아니야.` — R4, R5, R5a anchor here (WLD b7343).
- `BLOCK_044` = `01JZ000000BKNM010700000044` — Narration with link l44; `◇ 첫 공개` of 오리지늄 등불 (WLD b7344).
- `BLOCK_045` = `01JZ000000BKNM010700000045` — 리엔 `(숨을 삼키며) 하르트 씨, 방금…` (WLD b7345).
- `BLOCK_046` = `01JZ000000BKNM010700000046` — Author memo `여기서 등불이 두 번째로 깜박이게 할지`.
- `BLOCK_047` = `01JZ000000BKNM010700000047`
- `BLOCK_048` = `01JZ000000BKNM010700000048` — Last block before the 집필 선.
- `BLOCK_1_7_027` = `01JZ000000BKNM010700000027` — 1-7 씬 2 #027: dialogue with no speaker (`▲ 화자 없음`).
- `BLOCK_1_8_001` = `01JZ000000BKNM010800000001` — 1-8 씬 1 #001: an empty narration block (no content, so the 집필 선 does not move).
- `BLOCK_B12` = `01JZ000000BKNM010200000003` — WLD b12: 1-2 씬 1 리엔 `등불 속 그 돌, 오리지… 뭐라고 했죠?` (plain, cut-off candidate).
- `BLOCK_B32` = `01JZ000000BKNM010300000007` — WLD b32: 1-3 씬 2 세라 `빛돌 하나면 한 달은 버텨.` (alias candidate).
- `BLOCK_B41` = `01JZ000000BKNM010400000004` — 1-4 씬 2 #004: narration with a link to term 대정전; its `◇ 첫 공개` (WLD-T34 b41).
- `BLOCK_B42` = `01JZ000000BKNM010400000005` — WLD b42: 1-4 씬 2 세라 `[오리지늄]은 타지 않아. 빛만 남지.` (`▲ 공개 전 사용`).
- `BLOCK_B61` = `01JZ000000BKNM010600000003` — WLD b61: 1-6 씬 1 오웬 `하층에서 빛돌을 판다고?` (alias candidate).
- `BLOCK_1_1_001` = `01JZ000000BKNM010100000001` — 1-1 씬 1 #001: first block of the manuscript; `◇ 첫 공개` of term 등불 결사.
- `BLOCK_1_5_CHOICE` = `01JZ000000BKNM010500000006` — 1-5 씬 2 #006: the choice block (options `시장 쪽으로 간다` · `결사로 돌아간다`).
- `BLOCK_1_6_007` = `01JZ000000BKNM010600000007` — 1-6 씬 2 #007: 노아's first line, fixed voice ID `NOA_0106_007`; N1 anchors here.
- `BLOCK_1_6_025` = `01JZ000000BKNM010600000025` — 1-6 씬 4 #025: 오웬 `(장부를 덮으며) 백 번째…`; N2 anchors here.

**Characters (key encodes the 인물 기록 number)**

- `MARTHA` = `01JZ000000CHAR000000000001`
- `SERA` = `01JZ000000CHAR000000000002`
- `OWEN` = `01JZ000000CHAR000000000003`
- `HART` = `01JZ000000CHAR000000000004`
- `KAIL` = `01JZ000000CHAR000000000005`
- `IRENA` = `01JZ000000CHAR000000000006`
- `RIEN` = `01JZ000000CHAR000000000007`
- `NOAH` = `01JZ000000CHAR000000000008` — In the trash since 2026-10-26 (`3일 남음` on 2026-11-22).
- `DOR` = `01JZ000000CHAR000000000009`
- `TEO` = `01JZ000000CHAR000000000010`
- `ELIO` = `01JZ000000CHAR000000000011`
- `GORDON` = `01JZ000000CHAR000000000012`

**Field definitions**

- `FIELD_BIRTH` = `01JZ000000FDEF000000000001`
- `FIELD_AGE` = `01JZ000000FDEF000000000002`
- `FIELD_BIRTHDAY` = `01JZ000000FDEF000000000003`
- `FIELD_AFFILIATION` = `01JZ000000FDEF000000000004` — 소속, `짧은 글` (see fixture.md: not `연결`).
- `FIELD_HEIGHT` = `01JZ000000FDEF000000000005` — 신장, number in cm, export name `height`.
- `FIELD_GENDER` = `01JZ000000FDEF000000000006`
- `FIELD_SPECIES` = `01JZ000000FDEF000000000007`
- `FIELD_PERSONALITY` = `01JZ000000FDEF000000000008`
- `FIELD_CODENAME` = `01JZ000000FDEF000000000009`
- `FIELD_ORIGIN` = `01JZ000000FDEF000000000010`
- `FIELD_SPECIALTY` = `01JZ000000FDEF000000000011`
- `FIELD_PLACE_STATE` = `01JZ000000FDEF000000000012` — Place field 상태.
- `FIELD_FACTION_LEADER` = `01JZ000000FDEF000000000013` — Faction field 수장.
- `OPTION_GENDER_FEMALE` = `01JZ000000FCHC000006000001`
- `OPTION_GENDER_MALE` = `01JZ000000FCHC000006000002`
- `OPTION_GENDER_UNSAID` = `01JZ000000FCHC000006000003`
- `OPTION_SPECIES_HUMAN` = `01JZ000000FCHC000007000001`
- `OPTION_SPECIES_BEASTFOLK` = `01JZ000000FCHC000007000002`
- `OPTION_SPECIES_FAE` = `01JZ000000FCHC000007000003`
- `OPTION_SPECIES_AUTOMATON` = `01JZ000000FCHC000007000004`
- `OPTION_SPECIALTY_LAMP_REPAIR` = `01JZ000000FCHC000011000001`
- `OPTION_SPECIALTY_PATHFINDING` = `01JZ000000FCHC000011000002`
- `OPTION_SPECIALTY_SWORD` = `01JZ000000FCHC000011000003`
- `OPTION_SPECIALTY_RECORDS` = `01JZ000000FCHC000011000004`
- `OPTION_SPECIALTY_BARTER` = `01JZ000000FCHC000011000005`
- `OPTION_SPECIALTY_CLERIC` = `01JZ000000FCHC000011000006`
- `OPTION_SPECIALTY_HEALING` = `01JZ000000FCHC000011000007`

**Tags**

- `TAG_LEAD` = `01JZ000000TAGS000000000001`
- `TAG_LANTERN_SOCIETY` = `01JZ000000TAGS000000000002`
- `TAG_LOWER_DISTRICT` = `01JZ000000TAGS000000000003`
- `TAG_UPPER` = `01JZ000000TAGS000000000004`

**Relation kinds (rule 13 seed list)**

- `KIND_COLLEAGUE` = `01JZ000000RKND000000000001`
- `KIND_SIBLING` = `01JZ000000RKND000000000002`
- `KIND_SPOUSE` = `01JZ000000RKND000000000003`
- `KIND_LOVER` = `01JZ000000RKND000000000004`
- `KIND_FRIEND` = `01JZ000000RKND000000000005`
- `KIND_TRADE_PARTNER` = `01JZ000000RKND000000000006`
- `KIND_ENEMY` = `01JZ000000RKND000000000007`
- `KIND_PARENT_CHILD` = `01JZ000000RKND000000000008`
- `KIND_MASTER_PUPIL` = `01JZ000000RKND000000000009`
- `KIND_SUPERIOR_SUBORDINATE` = `01JZ000000RKND000000000010`
- `KIND_SENIOR_JUNIOR` = `01JZ000000RKND000000000011`
- `KIND_TRUST` = `01JZ000000RKND000000000012`
- `KIND_RELIANCE` = `01JZ000000RKND000000000013`
- `KIND_ADMIRATION` = `01JZ000000RKND000000000014`
- `KIND_FONDNESS` = `01JZ000000RKND000000000015`
- `KIND_PROTECTION` = `01JZ000000RKND000000000016`
- `KIND_WARINESS` = `01JZ000000RKND000000000017`
- `KIND_DISTRUST` = `01JZ000000RKND000000000018`
- `KIND_JEALOUSY` = `01JZ000000RKND000000000019`
- `KIND_HATRED` = `01JZ000000RKND000000000020`
- `KIND_GUILT` = `01JZ000000RKND000000000021`
- `KIND_INDIFFERENCE` = `01JZ000000RKND000000000022`

**Relations**

- `REL_RIEN_HART` = `01JZ000000RSHP000000000001` — 리엔 (A) · 하르트 (B): empty base; R4–R7a.
- `REL_RIEN_SERA` = `01JZ000000RSHP000000000002` — 리엔 (A) · 세라 (B): 사이 선배↔후배 (세라 holds 선배), 리엔's 마음 의지.
- `REL_RIEN_OWEN` = `01JZ000000RSHP000000000003` — 리엔 (A) · 오웬 (B): 사이 거래 상대.
- `REL_SERA_HART` = `01JZ000000RSHP000000000004` — 세라 (A) · 하르트 (B): 사이 동료.
- `REL_OWEN_NOAH` = `01JZ000000RSHP000000000005` — 오웬 (A) · 노아 (B): not live (노아 trashed); N2.

**Voice lines and archive documents**

- `VOICE_RIEN_AFTER_TRUST` = `01JZ000000VCES000000000004` — 리엔 `신뢰 상승 후`, empty (`▲ 보이스 비어 있음`).
- `ARCHIVE_RIEN_JOURNAL` = `01JZ000000ARCH000000000001`
- `ARCHIVE_RIEN_BLACKOUT` = `01JZ000000ARCH000000000002`
- `ARCHIVE_HART_NOTEBOOK` = `01JZ000000ARCH000000000003`

**World**

- `TERM_ORIGINIUM_LANTERN` = `01JZ000000TERM000000000001` — T1 `오리지늄 등불`, aliases 오리지늄 · 빛돌, `◇` on #044.
- `TERM_BLACKOUT` = `01JZ000000TERM000000000002` — `대정전`, `◇` on b41 (1-4 씬 2).
- `TERM_LANTERN_SOCIETY` = `01JZ000000TERM000000000003` — `등불 결사`, `◇` on 1-1 씬 1 #001.
- `TERM_EMBER_OATH` = `01JZ000000TERM000000000004` — `잿불의 서약`, `◇` planned at unit 2-5 start.
- `TERM_UPPER_GRID` = `01JZ000000TERM000000000005` — `상층 송전망`, `◇` planned at 2장 start.
- `PLACE_LOWER_STREET` = `01JZ000000P1CE000000000001`
- `PLACE_SOCIETY_GATE` = `01JZ000000P1CE000000000002`
- `PLACE_ASH_MARKET` = `01JZ000000P1CE000000000003`
- `PLACE_UPPER_GATE` = `01JZ000000P1CE000000000004`
- `PLACE_PYLON_RUINS` = `01JZ000000P1CE000000000005`
- `PLACE_ARCHIVE_ROOM` = `01JZ000000P1CE000000000006`
- `PLACE_UPPER_CITY` = `01JZ000000P1CE000000000007` — `상층`: its pin on 세계 지도 is the parent of 하층 구역 지도 (WLD-T55).
- `FACTION_LANTERN_SOCIETY` = `01JZ000000FCTN000000000001`
- `FACTION_UPPER_GUARD` = `01JZ000000FCTN000000000002`
- `EVENT_BLACKOUT` = `01JZ000000EVNT000000000001` — World event 대정전, 1195, at 하층 거리.
- `EVENT_GRID_ACCIDENT` = `01JZ000000EVNT000000000002` — World event 상층 송전망 사고, 1201, revealed from 2장.
- `EVENT_JOINS_SOCIETY` = `01JZ000000EVNT000000000003` — Story event `리엔, 결사 합류` on 1-3 씬 2 (anchor event of Fixture T).

**Maps**

- `MAP_WORLD` = `01JZ000000MAPS000000000001`
- `MAP_LOWER_DISTRICT` = `01JZ000000MAPS000000000002` — 하층 구역 지도, child of 세계 지도 via [PIN_WORLD_UPPER_CITY]; scale 2일 도보 per 0.25.
- `MAP_ASH_MARKET` = `01JZ000000MAPS000000000003` — Node map 잿더미 시장, child of 하층 구역 지도 via [PIN_ASH_MARKET].
- `PIN_WORLD_UPPER_CITY` = `01JZ000000P1NS000000000001`
- `PIN_WORLD_PYLON_RUINS` = `01JZ000000P1NS000000000002`
- `PIN_SOCIETY_GATE` = `01JZ000000P1NS000000000003`
- `PIN_LOWER_STREET` = `01JZ000000P1NS000000000004`
- `PIN_ASH_MARKET` = `01JZ000000P1NS000000000005`
- `PIN_ARCHIVE_ROOM` = `01JZ000000P1NS000000000006`
- `PIN_UPPER_GATE` = `01JZ000000P1NS000000000007`
- `REGION_LOWER_DISTRICT` = `01JZ000000RGNS000000000001` — Region 하층 구역 on 하층 구역 지도 (a scene `지역`, revealed from 1-1).
- `REGION_UPPER_GUARD` = `01JZ000000RGNS000000000002` — Region 상층 경비대 on 세계 지도; shape `◆` from 1-9.
- `ROUTE_GATE_TO_UPPER_GATE` = `01JZ000000RTES000000000001` — 결사의 문 → 상층 관문, 38 km.
- `ROUTE_GATE_TO_MARKET` = `01JZ000000RTES000000000002` — 결사의 문 → 잿더미 시장.

**Canvas and pages**

- `CANVAS_CHAPTER_1` = `01JZ000000CNVS000000000001` — Canvas `1장 구상`.
- `CANVAS_EDGE_RIEN_HART` = `01JZ000000CNED000000000001` — Edge 리엔 — 하르트 backed by [REL_RIEN_HART].
- `PAGE_LOWER_LIGHT` = `01JZ000000PAGE000000000001` — Page `하층 구역의 빛` (WLD pg1).
- `PAGE_SOCIETY_RULES` = `01JZ000000PAGE000000000002` — Page `등불 결사 규약`, made from recorder record [RECORDER_R0].

**Link runs (link keys)**

- `LINK_L44` = `01JZ000000HREF000000000001` — l44: #044 → 오리지늄 등불.
- `LINK_L42` = `01JZ000000HREF000000000002` — l42: b42 → 오리지늄 등불 (text `오리지늄`).
- `LINK_LPG` = `01JZ000000HREF000000000003` — lpg: page 하층 구역의 빛 → 오리지늄 등불.

**Change records**

- `R1` = `01JZ000000CHGR000000000001` — R1 리엔 · 소속 `없음` at unit 1-1 start (note `하층 구역의 고아`).
- `R2` = `01JZ000000CHGR000000000002` — R2 리엔 · 소속 `등불 결사` at unit 1-3 start (note `세라의 안내로 합류`).
- `R3` = `01JZ000000CHGR000000000003` — R3 리엔 · 소속 `등불 결사 · 등불지기장` at unit 2-5 start (예정).
- `R4` = `01JZ000000CHGR000000000004` — R4 리엔의 마음 → 하르트 `경계` (`불빛을 아는 낯선 사람`) at #043.
- `R5` = `01JZ000000CHGR000000000005` — R5 하르트의 마음 → 리엔 `보호` at #043.
- `R5A` = `01JZ000000CHGR000000000006` — R5a 하르트's 호칭 for 리엔 `꼬마` at #043.
- `R6` = `01JZ000000CHGR000000000007` — R6 사이 `동료` at unit 1-9 start (예정).
- `R7` = `01JZ000000CHGR000000000008` — R7 리엔의 마음 → 하르트 `신뢰` at unit 1-9 start (예정).
- `R7A` = `01JZ000000CHGR000000000009` — R7a 리엔's 호칭 for 하르트 `하르트` at unit 1-9 start (예정).
- `R8` = `01JZ000000CHGR000000000010` — R8 하르트 · 표시 이름 cleared (show 이름) at unit 1-9 start (예정).
- `R9` = `01JZ000000CHGR000000000011` — R9 `◇ 첫 공개` 오리지늄 등불 at #044.
- `REVEAL_BLACKOUT` = `01JZ000000CHGR000000000012`
- `REVEAL_LANTERN_SOCIETY` = `01JZ000000CHGR000000000013`
- `REVEAL_EMBER_OATH` = `01JZ000000CHGR000000000014`
- `REVEAL_UPPER_GRID` = `01JZ000000CHGR000000000015`
- `W1` = `01JZ000000CHGR000000000016` — W1 마르타 · 소속 `등불 결사` at world event 대정전 (base `상층 경비대`).
- `W2` = `01JZ000000CHGR000000000017` — W2 하층 거리 · 상태 `대정전 이후` at world event 대정전 (base `빛 있음`).
- `REGION_SHAPE_1_9` = `01JZ000000CHGR000000000018` — Region 상층 경비대 shape S2 at unit 1-9 start (예정).
- `N1` = `01JZ000000CHGR000000000019` — N1 노아 · 소속 `잿더미 시장 심부름꾼` at 1-6 #007 (target in the trash).
- `N2` = `01JZ000000CHGR000000000020` — N2 오웬의 마음 → 노아 `보호` at 1-6 #025 (relation not live).
- `RECORD_TRASHED` = `01JZ000000CHGR000000000021` — 세라 · 코드네임 record, removed by `기록 지우기` on 2026-11-18.

**Script assets**

- `ASSET_BG_LOWER_STREET_DAWN` = `01JZ000000ASST000000000001`
- `ASSET_BG_SOCIETY_GATE_MORNING` = `01JZ000000ASST000000000002`
- `ASSET_BG_ASH_MARKET_NIGHT` = `01JZ000000ASST000000000003`
- `ASSET_BGM_EMBERS` = `01JZ000000ASST000000000004`
- `ASSET_SFX_WIND` = `01JZ000000ASST000000000005`
- `ASSET_CG_FLICKER` = `01JZ000000ASST000000000006`
- `ASSET_BGM_UPPER_BELL` = `01JZ000000ASST000000000007` — Not used by any cue (`안 쓰는 것`).

**Checks, trash, travel**

- `ACK_TRAVEL_HART` = `01JZ000000ACKS000000000001` — `맞음` on `travel.shortage` 하르트 1-7 씬 2 → 씬 3.
- `ACK_CLOCK_BAND` = `01JZ000000ACKS000000000002` — `맞음` on `time.clockBandMismatch` 1-5 씬 1 (새벽 + 06:20).
- `TRASH_NOAH` = `01JZ000000TRSH000000000001`
- `TRASH_SCENE_1_6_5` = `01JZ000000TRSH000000000002`
- `TRASH_BLOCKS_1_5_2` = `01JZ000000TRSH000000000003`
- `TRASH_RECORD` = `01JZ000000TRSH000000000004`
- `TRAVEL_WALK` = `01JZ000000TRVM000000000001`
- `TRAVEL_HORSE` = `01JZ000000TRVM000000000002`
- `TRAVEL_SHIP` = `01JZ000000TRVM000000000003`
- `IMAGE_PROJECT_COVER` = `01JZ000000PCTR000000000001`

**Recorder (library outside the project)**

- `RECORDER_R0` = `01JZ000000RCRD000000000000` — Made into page 등불 결사 규약 (state 넣음).
- `RECORDER_R1` = `01JZ000000RCRD000000000001` — LRC R1: voice, 2026-11-22 08:41 KST, links [1-7 씬 3 (entry), 하르트 (manual)].
- `RECORDER_R2` = `01JZ000000RCRD000000000002` — LRC R2: text with `@세라`, links [project (entry), 오웬 (manual), 세라 (at)].
- `RECORDER_R3` = `01JZ000000RCRD000000000003` — LRC R3: voice 1:02, no links.
- `RECORDER_R4` = `01JZ000000RCRD000000000004` — LRC R4: text, `끝냄`.
- `RECORDER_NOAH` = `01JZ000000RCRD000000000005` — Linked to 노아 (manual): his `레코더 칩 1`.
- `RECORDER_TRASHED` = `01JZ000000RCRD000000000006` — Voice 0:12 in the recorder trash since 2026-10-24.

## Partial

- **Acknowledgement fingerprints** are stored values, not computed here: `core:model` cannot call the `domain:story` `Fingerprints` scheme. They are the values `domain:checks` computed for the example as built. If the story engine changes its fingerprint parts, or a fixture edit changes 하르트's 1-7 씬 2 → 씬 3 pair or 1-5 씬 1's time, both acknowledgements turn void; `domain:checks` `FixtureChecksTest.fixtureRaisesEightOpenItemsAndTwoAcknowledged` then fails, and the values must be recomputed.
- **No blob rows.** Nothing references image or audio bytes. The cover is `preset:ash-dawn`, script assets have no `fileHash`, and recorder records have no audio. An exporter test therefore needs no asset files.
- **Recorder records R5 … R12** of LRC fixture P2 (「유리 정원」) are not included. They belong to another project.
- **The bundled `.civilight` regression file** (§8.1, LIB-17) is not written here. Producing it needs the exporter (`app`/`core:store`).

## Defaults used

**Keys and clock**
- Keys are `01JZ000000` + a 4-letter family tag + 12 digits. Crockford base 32 has no I, L, O or U, hence tags such as `P1CE`, `P1NS`, `HREF`.
- Block keys encode episode and number: `BKNM` + `cc ee 0000 nnnn`. Character keys encode the `인물 기록` number.
- `updatedBy` is `example-device` everywhere.
- Project created 2026-03-01. Last edit 2026-11-22 08:12 KST (`LAST_EDIT`).

**Voice IDs**
- 1-1 … 1-3 dialogue IDs were fixed by a CSV export on 2026-11-10. Each `fixedId` equals the automatic format and `lastExport.textHash` is the SHA-256 of the text.
- 1-6 #007 (노아) was fixed on 2026-11-12 as `NOA_0106_007`.
- Every other block is automatic (`voice` absent).
- 1-2 #007 (`RIE_0102_007`) was deleted and purged on 2026-11-14, leaving a `DiscardedVoiceId(deleted)`.

**Records and log**
- 하르트's name reveal R8 stores `value` absent (show 이름), not the string `하르트`.
- `DeletionLogEntry.key` is the purged item's key: 1-2 #007, and 1-7 #049 … #057 purged 2026-11-19.

**Content choices**
- 1-1 … 1-6 are `doneMarked`.
- 1-8 씬 1 holds one empty narration block #001. It is relocation target "1-8 #001" (T-12-06b) without moving the 집필 선.
- 1-5 씬 1 is `새벽` with clock 06:20 on purpose. `time.clockBandMismatch` is acknowledged.
- 1-8 씬 2 has a 직접 transition `기록실 · 같은 날 밤` with an empty snapshot `{}` (no place, no 때).
- `대정전` is both a world event and a term. The term is revealed at 1-4 씬 2 #004 (b41, a link run).
- `등불 결사` is both a faction and a term. The term is revealed at 1-1 씬 1 #001, the first block.
- 하르트's secret body links term 상층 송전망. The faction 등불 결사's description links 마르타.
- The 기록실 place's parent is 결사의 문. 상층 관문's parent is 상층.
- Invented names: characters 카일, 이레나, 도르, 테오, 엘리오, 고든; the extra episode and scene titles; field values; map geometry. No spec names them.

## Spec conflicts

| # | Conflict | Chosen | Spec tests affected |
| --- | --- | --- | --- |
| 1 | **Content after 1-7 씬 3.** SCR F1 gives 1-7 씬 4 blocks #049–#052, a 1-7 씬 5 (#053) and content at 1-8 #001. STO Fixture R, CST §4, WLD Fixture F and CNV S0 put the 집필 선 at the end of 1-7 씬 3. | 집필 선 = end of 1-7 씬 3. 1-7 씬 4 exists (때 06:10, 직접 transition) with no blocks. No 씬 5. 1-8 #001 exists but is empty. #049–#057 are purged numbers; the counter stays 58. | SCR T-12-06(a), T-12-07 (relocation now goes to 1-8 #001, not #049), T-12-11, T-12-22, T-12-24(b), T-11-03 (#050), T-22-07, T-22-09 (`씬 5/6` rows), T-22-13/14/17/18/21/24/25/26, T-19-02 (`블록 12`), LIB E-19 (no blocks in S4) |
| 2 | **1-7 씬 1/2 titles and base.** SCR F1 has `새벽 순찰` based on 1-6 씬 3, and `결사의 문`. STO Fixture T and WLD-T60 have `첫 번째 깜박임` based on 1-6 씬 1, and `상층 관문`. STR-02 also says 씬 1 `새벽 순찰`. | Fixture T. 1-2 씬 1 keeps `새벽 순찰`. | SCR F1 table, STO STR-02 |
| 3 | **1-7 block counter.** SCR F1 has `nextBlockNumber = 58`. LRC fixture says highest issued #061 (next #062). | 58. A recorder insert after #044 becomes #058 (as SCR T-23-01 already notes). | LRC-T17, T18, T19, T36, T37 |
| 4 | **1-8 씬 1.** Fixture T S11 has 송전탑 폐허 and S10 +2일 새벽. WLD-T58 and TRV-14 have no place, BLK-02 has no 때, and SUG-02 has a narration plus a proposal of exactly S11's values. | Fixture T, with no narration (it would move the 집필 선 into 1-8). | WLD-T58, STO TRV-14, BLK-02, SUG-02 |
| 5 | **1-7 씬 4 때.** Fixture T S10 has 06:10. BLK-02 calls it untimed. | Timed. | BLK-02 |
| 6 | **Scene counts.** BLK-01 has 1장 = 31 scenes, 22 with place and 19 with 때 (matched). LIB RHOpenFile shows `씬 31 · 인물 11` for the whole project. | 34 live scenes: 1장 31 + 2-3 ×2 + 2-5 ×1. | LIB open-file summary sample |
| 7 | **Character count.** LIB P-09 shows `인물 12`; CST and CNV show `전체 11` with 노아 in the trash. | 12 rows, 11 live. | LIB P-09 (count rows vs live) |
| 8 | **Term aliases.** The orchestrator, SCR F1, LRC P1 and WLD §0 give 오리지늄 등불 the aliases `오리지늄 · 빛돌` (WLD state B). WLD Fixture F's default is state A. | State B. b32 and b61 (`빛돌`) are then pre-reveal candidates: `◇ 공개 전 후보` rows at 1-3 씬 2 and 1-6 씬 1. | WLD-T01 … T09, T14, T66 (remove alias 빛돌 for state A), STO CHK-01, CNV T-CNV-23 (`전체 5` becomes 7) |
| 9 | **Number of terms.** WLD §0 and the WLD-03 list sample give 5 terms: 오리지늄 등불, 대정전, 등불 결사, 잿불의 서약, 상층 송전망. WLD Fixture F says no other term exists. LRC P1 says 대정전 is not a term. | 5 terms. None matches plain manuscript text before its reveal. | WLD Fixture F remark, LRC-T07 (`글에 나온 이름` would list term 대정전) |
| 10 | **소속 field type.** CST-24 and CST-19 make it `연결 · 세력`. The rule-11 cases and Fixture R type free text (`없음`, `등불 결사 · 견습`, `… 등불지기장`), and TC-F-22 starts from a `짧은 글 소속`. | `짧은 글`. 출신지 is the bundle's `연결` field (→ 장소). | CST TC-F-24, TC-20-13, WLD-10 `이 항목으로 잇는 인물` (as 소속) |
| 11 | **R3 value.** STO Fixture R has `등불지기장`; CST TC-11-01 has `등불 결사 · 등불지기장`. | CST (owner of field values). | STO LENS-02 string |
| 12 | **Bundle size.** CST-19 and the LIB settings row show `게임 프로필 · 고정 7 · 프로젝트 7`. CST-24 and TC-F-15 give 기본 + 6 = 11. | 11 character fields. | CST-19 / LIB-1048 sub text |
| 13 | **Field flags.** CST-19 marks 출생 `머리` and 종족 `목록`. TC-F-26 needs the first two `머리` fields to be 나이 · 소속, and TC-F-17's list row shows only 나이 · 소속. | `머리` = 나이 · 소속 · 신장 · 종족 (CST-14 header example). `목록` = 나이 · 소속. | CST-19 sample rows |
| 14 | **Relation kinds.** Rule 13 has 사이 11 (대칭 7 + 짝 4) and 마음 11. Boards `RCRelTypes` and `ProjSettings` show `사이 12 · 마음 11` with different names. | Rule 13 list (CST Q1 default). | CST-32 / LIB settings count |
| 15 | **세라·하르트 relation.** CST TC-13-14 has it in the base (사이 동료). CNV S0 lacks it (T-CNV-14 adds it). | Present. T-CNV-14's "still exactly one relationship" holds. | CNV S0 description |
| 16 | **하르트 in 1-7 씬 1.** WLD-T60 lists `1-7 씬 1 · 리엔 · 하르트`. Fixture T says 하르트 speaks in S8 and S9. Putting him in S7 would add a second travel `▲` (하층 거리 → 상층 관문, 28 km in ≤ 16 h). | Not in S7. | WLD-T60 |
| 17 | **Places of 1-2 씬 1 and 1-4 씬 2.** SCR F4(d)/(f) give region 하층 구역; Fixture T gives place 하층 거리. | Fixture T. Region 하층 구역 is used by 1-5 씬 1. | SCR T-22-03/T-22-04 (d), (f) as base facts |
| 18 | **What 하층 구역 is.** WLD §0 lists `하층 구역 (sub-map)` with the places. WLD-T55 has pin `상층` as parent of sub-map 하층 구역. TC-20-24 names the map 「하층 구역 지도」. | Map `하층 구역 지도`, child of pin 상층 on 세계 지도, plus region `하층 구역` on it. No Place named 하층 구역. | WLD-33 `장소 | 하층 구역` for 대정전 (chosen: place 하층 거리, per WLD-T60) |
| 19 | **Pin reveal summary.** WLD-T56 counts all six pins on one map. Here 송전탑 폐허's pin is on 세계 지도 (WLD-T55). | 하층 구역 지도 at lens 1-5 reads `핀 4 · 영역 1 · 아직 공개 전 1` (상층 관문). | WLD-T56 |
| 20 | **기록실 place card.** SUG-03 says there is none. WLD-T45, T56 and T71 use the place 기록실. | Exists. | SUG-03 (`· 장소 카드 없음 → 새 장소`) |
| 21 | **State of 1-6 씬 5.** It is hidden (SRCH-05, T-19-01, LIB I-04) and also trashed (TC-20-01/09, LIB T-05). LIB E-08 says the example has no hidden items. | Trashed, not hidden. Nothing in the base is hidden. | SRCH-05, T-19-01, CST TC-19-*, LIB I-* (hidden variants) |
| 22 | **노아's state.** Live in TC-20-01 and T-20-01 (before deletion), hidden in TC-19-*, trashed in TC-20-02 (after), LIB T-06, STO CHK-01, CNV T-CNV-40 and the orchestrator. | Trashed 2026-10-26. To start live, clear `deletedAt`/`trashEntryKey` on `NOAH` and drop `TRASH_NOAH`. | CST TC-20-01, SCR T-20-01, TC-19-* |
| 23 | **Check centre count.** CHK-01, T-CNV-23 and CHK-02 count `전체 5` plus `맞다고 표시한 것 2`. | Base raises 7 open items (#8 above) and stores 2 acknowledgements (see Facts). | STO CHK-01, CNV T-CNV-23 |
| 24 | **Board samples not reproduced.** STR-02 character counts (1,820자 · 2,104자); CST-14 voice progress `18 / 24` (here 3 / 5); SPL-01 and SCR T-22-09 split counts follow 13 blocks. | — | STO STR-02, CST-14 |
| 25 | **대정전 event place.** WLD-T60 gives place 하층 거리; WLD-33 gives `하층 구역` (region). | 하층 거리. | WLD-33 |
| 26 | **Recorder records.** LRC F-open has P1 records R1–R4. CST TC-20-01 needs 노아's `레코더 칩 1`; R0 is my own addition for placement coverage. | `ExampleRecorder` = R0 (page placement), R1–R4, `RECORDER_NOAH`, and a trashed voice record. To get LRC F-open's P1 slice, drop R0 and `RECORDER_NOAH`. | LRC-T03 (`Project(P1)` filter also lists R0 and the 노아 record), LRC-T10 (`정리 안 됨 3` becomes 4), LRC-T15, LRC-T38 (another record refers to 노아), LRC-T29 and LIB R19-01 (one linked record), LRC-T43 and LIB E-11 (four linked records, none hidden here) |

## Requests

- **domain:library owner:** the fixture's encoded JSON changed on 2026-11-22 (`maps/01JZ000000MAPS000000000001.json` and `maps/01JZ000000MAPS000000000002.json`: both generated maps' `generation.result` now hold real `gen-1` output), so the §8.1 regression file `domain/library/src/test/resources/format/1.0/잿빛 새벽의 등불.civilight` must be regenerated from `ExampleProject.build()`; until then `FormatRegressionTest` `the current-version file holds the example project` fails. (`Place.aliases`, added the same day, is not written when empty, so it does not change the encoded fixture.)

- **domain:checks:** keep `FixtureChecksTest` guarding the fixture's two stored fingerprints (`travel.shortage`, `time.clockBandMismatch`), and report new values here when the story engine's fingerprint parts change.
- **App / exporter owner:** write `ExampleProject.build()` plus `ExampleRecorder.projectFile()` as the bundled example `.civilight` (LIB-17) and as the §8.1 regression file `app/src/test/resources/format/1.0/`.
