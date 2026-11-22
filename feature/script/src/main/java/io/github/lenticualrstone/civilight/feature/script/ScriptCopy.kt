package io.github.lenticualrstone.civilight.feature.script

/**
 * UI copy of the script screen that the domain module does not already produce (DECISIONS §3).
 * Every value is linted by `ScriptCopyLintTest`. Strings the domain builds (`ScriptCommands`,
 * `SceneText`, `Toasts`, `Manuscript`) are used as they come and linted there too.
 */
object ScriptCopy {
    // --- Header (SCR-01, SCR-02, SCR-14) ---

    /** `완료`: leaves editing, keeps the block selected (SCR-14). */
    const val DONE = "완료"

    /** Accessibility label of the layers icon (header and command row, SCR-01). */
    const val LAYERS_LABEL = "연출 · 메모 표시"

    /** The one row of the position-label menu (SCR-02). */
    const val SET_LENS_TO_SCENE = "렌즈를 이 씬으로 맞추기"

    // --- Bottom bar (SCR-01, SCR-15) ---

    /** Primary pill of the command row (SCR-15, Q-03 default: a soft line break). */
    const val LINE_BREAK = "줄바꿈"

    /** Life Recorder mic (owned by the recorder area). */
    const val MIC_LABEL = "라이프 레코더"

    // --- Manuscript states (SCR-01, SCR-14, SCR-18) ---

    /** Small label after the speaker of the block being typed (SCR-14). */
    const val TYPING = "입력 중"

    /** Small label after an inherited speaker until the first character (SCR-18). */
    const val INHERITED_SPEAKER = "직전 화자"

    /** Empty episode (SCR-01 `[PROPOSED]`). */
    const val NO_SCENES = "씬 없음"

    /** Button of the empty episode (SCR-01 `[PROPOSED]`). */
    const val NEW_SCENE = "새 씬"

    /** Speaker-slot words of a choice block (SCR-09, board `Blocks`). */
    const val CHOICE_LABEL = "선택지"
    const val CHOICE_REJOIN = "다음 블록에서 합류"

    /** Speaker-slot words of a system-text block (SCR-10, board `Blocks`). */
    const val SYSTEM_LABEL = "시스템"
    const val SYSTEM_SUB = "화면 문구"

    /** Speaker pill without a speaker (vocabulary: never `미지정`). */
    const val NO_SPEAKER = "화자 없음"

    /** Picker sub-line prefix when the display name differs at the block: `표시 이름 ???` (board `Picker`). */
    const val DISPLAY_NAME = "표시 이름"

    /** Scene-head `⋯` label: `씬 4 메뉴` (SCR-46). */
    const val SCENE_MENU_SUFFIX = "메뉴"

    fun sceneMenuLabel(ordinal: Int): String = "씬 $ordinal $SCENE_MENU_SUFFIX"

    /** `블록 추가` subline without an insert position (the first part of `지금 블록 아래 · 새 대사 화자: 리엔(직전)`). */
    const val ADD_BELOW = "지금 블록 아래"

    // --- Keyboard toolbar (SCR-16) ---

    /** Kind tool: `대사 ▾` (current kind + the sheet chevron). */
    const val KIND_CHEVRON = " ▾"

    const val PAREN_TOOL = "( )"
    const val PAREN_LABEL = "괄호 지문"
    const val LINK_TOOL = "@"
    const val CHANGE_TOOL = "◆"
    const val CHANGE_LABEL = "변화 기록"
    const val UNDO_LABEL = "되돌리기"

    /** Link picker title (SCR-55 names none). */
    const val LINK_PICKER_TITLE = "TODO_COPY_SCRIPT_LINK_PICKER_TITLE"

    // --- Undo step names (shown by undo history; spec words where one exists) ---

    const val UNDO_ADD_BLOCK = "블록 추가"
    const val UNDO_SPLIT = "나누기"
    const val UNDO_MERGE = "합치기"
    const val UNDO_DELETE = "지우기"
    const val UNDO_SPEAKER = "화자"
    const val UNDO_RESTORE = "되살리기"
    const val UNDO_LINE_BREAK = LINE_BREAK
    const val UNDO_TYPING = "TODO_COPY_SCRIPT_UNDO_TYPING"
    const val UNDO_ADD_OPTION = "선택지"
    const val UNDO_KIND = "종류 바꾸기"
    const val UNDO_CUE = "연출 지시"

    // --- Panel (SCR-03, SCR-31, SCR-34) ---

    /** Panel tabs `시간 · 참고 · 속성 · 모듈` (SCR-31). */
    const val TAB_TIME = "시간"
    const val TAB_REF = "참고"
    const val TAB_PROPS = "속성"
    const val TAB_MODS = "모듈"

    /** No selected block (SCR-31 `[PROPOSED]`). */
    const val NO_SELECTED_BLOCK = "고른 블록 없음"

    /** `속성` field labels (SCR-34; `종류` is SCR-13 `[PROPOSED]`). */
    const val KIND_FIELD = "종류"
    const val SPEAKER_FIELD = "화자"

    /** `바꾸기` (SCR-13, SCR-19, SCR-34, SCR-08). */
    const val CHANGE = "바꾸기"

    // --- Kind change (SCR-13) ---

    /** `종류 바꾸기` sheet title (SCR-13 `[PROPOSED]`). */
    const val KIND_CHANGE_TITLE = "종류 바꾸기"

    // --- Direction cues (SCR-08 `[PROPOSED]` editor) ---

    /** Cue sheet title: the add sheet's row name. */
    const val DIRECTION_TITLE = "연출 지시"

    /** `+ 연출` with the cue-kind chips. */
    const val ADD_CUE = "+ 연출"

    /** Takes one cue out. */
    const val REMOVE_CUE = "빼기"

    /** A cue kind without assets of that kind (the asset form is SCR-61). */
    const val NO_ASSETS = "TODO_COPY_SCRIPT_NO_ASSETS"

    // --- Commands (SCR-68) ---

    /** `번호 다시 매기기` (SCR-22): the command word and the sheet's fallback title. */
    const val RENUMBER = "번호 다시 매기기"

    /** `점검 센터 열기` (SCR-68). */
    const val OPEN_CHECK_CENTER = "점검 센터 열기"

    /** `닫기` (the command field while typing; the DS sheet header's default label). */
    const val CLOSE = "닫기"

    // --- 표시 sheet (SCR-26 … SCR-29, board `Layers`) ---

    const val LAYERS_TITLE = "표시"
    const val LAYERS_SUB = "보기에만 적용 · 파일 내용은 그대로"
    const val LAYER_DIRECTION = "연출 지시"
    const val LAYER_DIRECTION_SUB = "배경 · BGM · 효과음 줄"
    const val LAYER_MEMO = "작가 메모"
    const val LAYER_MEMO_SUB = "⌁ 로 시작하는 메모"
    const val LAYER_SYSTEM = "시스템 텍스트"
    const val LAYER_SYSTEM_SUB = "[기록] 같은 화면 문구 · 전환 문구"
    const val LAYER_MARKS = "변화 표식"
    const val LAYER_MARKS_SUB = "◆ 변화 · ◇ 첫 공개"
    const val LAYER_ADDRESS = "호칭 힌트"
    const val LAYER_ADDRESS_SUB = "대사 위 호칭 표시"
    const val LAYER_PAREN = "괄호 지문"
    const val LAYER_PAREN_SUB = "대사 속 (…) 표정 · 행동"

    /** Section and chips `쓰기 · 대사만 · 낭독` (SCR-27, SCR-28). */
    const val PREVIEW_SECTION = "미리 보기"
    const val PREVIEW_WRITE = "쓰기"
    const val PREVIEW_DIALOGUE = "대사만"
    const val PREVIEW_READ_ALOUD = "낭독"

    /** Chip under the header while a preview other than `쓰기` is on (`ED`, spec words only). */
    fun previewChip(mode: String): String = "$PREVIEW_SECTION · $mode"

    // --- Panel `시간` (SCR-32) ---

    const val NO_CHANGE = "변화 없음"
    const val SCENE_TIME_SECTION = "이 씬 · 장소와 때"
    const val CHANGE_MORE = "바꾸기 ›"
    const val IN_SCENE = "이 씬 안"
    const val BACKGROUND = "배경"
    const val REVEAL_ROW = "용어 첫 공개"
    const val EDIT_RECORD = "고치기"
    const val SHOW_IN_MAP = "관계도에서 보기"
    const val PRECISION_EXACT = "정확"
    const val PRECISION_APPROX = "대략"
    const val PREVIOUS_SCENE = "직전 씬"
    const val SAME_DAY = "같은 날"
    const val DIRECT_TIME = "직접"

    /** `▲ 하르트 이동 시간 · 점검 센터` (SCR-32). */
    fun travelLine(name: String): String = "▲ $name 이동 시간 · 점검 센터"

    /** `◇` card line `이 블록 · 1-7 #044` (SCR-32). */
    fun revealHere(location: String): String = "이 블록 · $location"

    /** Inner section title `고른 블록 · #043 하르트` (SCR-32). */
    fun selectedBlockWide(title: String, speaker: String?): String = if (speaker == null) title else "$title $speaker"

    // --- Panel `참고` (SCR-33) ---

    fun refHeader(speaker: String): String = "화자 · $speaker · 이 블록 기준"
    const val SPEECH_ROW = "말투"
    const val RECENT_LINES = "최근 대사"
    const val RELATIONS_NOW = "관계 · 지금"
    const val NOT_MET = "만나기 전"
    const val HERE = "◆ 여기서"
    fun hiddenRelations(n: Int): String = "숨긴 인물과의 관계 $n"

    /** Deleted partners, one dotted row: `지운 인물 · 노아` (rule 20 wording). */
    fun deletedPartners(names: String): String = "지운 인물 · $names"

    /** `참고` for a block that is not a dialogue line with a speaker (Q-15 has no answer). */
    const val REF_NOT_DIALOGUE = "TODO_COPY_SCRIPT_REF_NOT_DIALOGUE"

    // --- Panel `속성` (SCR-34, SCR-25, SCR-23) ---

    const val STAGE_FIELD = "무대 위치"
    const val STAGE_LEFT = "왼쪽"
    const val STAGE_CENTER = "가운데"
    const val STAGE_RIGHT = "오른쪽"
    const val VOICE_FIELD = "보이스 ID"
    const val VOICE_AUTO = "자동"
    const val VOICE_FIXED = "고정"
    const val CHANGES_FIELD = "변화 연결"
    const val SOURCE_FIELD = "출처"
    const val CHANGE_SPEAKER = "화자 바꾸기"
    const val RESTORE = "되살리기"
    const val KEEP = "이대로 두기"
    const val UNDO_KEEP = "이대로 두기"

    /** Suffix of the cover note `1-9에서 공개` (domain:cast `DisplayNames.propertiesValue`). */
    const val REVEAL_SUFFIX = "에서 공개"

    /** Inner `표시 이름` note `1-9에서 이름 공개` (board `WideProps`). */
    fun nameRevealWide(point: String): String = "${point}에서 이름 공개"

    /** `보이스 ID · #006-1` for a spoken choice option (SCR-23). */
    fun optionVoiceLabel(number: String): String = "$VOICE_FIELD · $number"

    /** `고정 · 11월 20일 내보냄`. */
    fun voiceFixed(date: String?): String = if (date == null) VOICE_FIXED else "$VOICE_FIXED · $date 내보냄"

    /** `◆ 관계 변화 · 리엔 → 하르트` (board `WideProps`). */
    fun changeLink(kind: String, subject: String): String = "◆ $kind 변화 · $subject"

    /** `레코더 11월 22일 08:41` (SCR-59). */
    fun recorderSource(at: String): String = "레코더 $at"

    // --- 스크립트 추가 칸 (SCR-24, SCR-25, board `ProjSettings`) ---

    const val FIELDS_TITLE = "스크립트 추가 칸"
    const val STAGE_SETTING_SUB = "왼쪽 · 가운데 · 오른쪽"
    const val VOICE_SETTING_SUB = "대사마다 녹음용 ID"
    const val ID_FORMAT = "ID 형식"
    const val FORMAT_PATTERN = "인물 · 에피소드 · 블록"
    const val FORMAT_MANUAL = "직접"
    const val FIXED_ON_EXPORT = "내보내면 고정"

    /** `HRT_0107_043 · 인물 · 에피소드 · 블록 · 내보내면 고정`. */
    fun patternSub(sample: String): String = "$sample · $FORMAT_PATTERN · $FIXED_ON_EXPORT"

    /** `직접 · 내보내면 고정`. */
    const val MANUAL_SUB = "$FORMAT_MANUAL · $FIXED_ON_EXPORT"

    /** The spec's sample voice ID (SCR-24) when the project has no dialogue line to show. */
    const val SAMPLE_CODE = "HRT"

    // --- Records on blocks (SCR-41 … SCR-43, board `RSOrphan`) ---

    /** Orphan card buttons (SCR-43). */
    const val RELINK = "다른 블록에 잇기"
    const val TRASH_RECORD = "기록 지우기"

    /** Point-seat line button (SCR-42). */
    const val LINK_TO_BLOCK = "블록에 잇기"

    /** `◆ 변화 기록` row at the end of the `시간` tab's block section (SCR-41 `[PROPOSED]`). */
    const val RECORD_CHANGE_ROW = "◆ 변화 기록"

    /** Toast after `기록 지우기` (vocabulary `지움`, as `블록 #043 지움`): `◆ 관계 · 리엔 → 하르트 지움`. */
    fun recordTrashed(title: String): String = "$title 지움"

    // --- Panel `모듈` (SCR-35, boards `CSheetMods`, `WideMods`) ---

    const val MOD_TODAY = "오늘"
    const val MOD_SCENE = "이 씬 분량"
    const val MOD_CHECKS = "점검"
    const val MOD_MEMO = "씬 메모"

    /** Footer (board copy; the order is stored, the add list is Q-16). */
    const val MODS_FOOTER = "카드 순서 · 추가는 ⋯"

    /** `점검` card parts: `▲ 공개 전 사용 1 · 화자 없음 1`. */
    fun checkPreReveal(n: Int): String = "공개 전 사용 $n"
    fun checkNoSpeaker(n: Int): String = "화자 없음 $n"

    /** `점검` card of a scene with no `▲` (not drawn by the boards). */
    const val MODS_NO_CHECKS = "TODO_COPY_SCRIPT_MODS_NO_CHECKS"

    /** `이 씬 분량` of a scene without dialogue or narration. */
    const val MODS_SCENE_EMPTY = "TODO_COPY_SCRIPT_MODS_SCENE_EMPTY"

    /** Toast after `되살리기` of a block (as the trash area's `노아 되살림`): `블록 #043 되살림`. */
    fun blockRestored(number: String): String = "블록 $number 되살림"

    /** Undo step names of the record fixes. */
    const val UNDO_RELINK = RELINK
    const val UNDO_TRASH_RECORD = TRASH_RECORD

    /** Every value above, for the lint test. */
    val all: List<String> = listOf(
        DONE, LAYERS_LABEL, SET_LENS_TO_SCENE, LINE_BREAK, MIC_LABEL, TYPING, INHERITED_SPEAKER, NO_SCENES,
        NEW_SCENE, CHOICE_LABEL, CHOICE_REJOIN, SYSTEM_LABEL, SYSTEM_SUB, NO_SPEAKER, DISPLAY_NAME, sceneMenuLabel(4), KIND_CHEVRON.trim(),
        PAREN_TOOL, PAREN_LABEL, LINK_TOOL, CHANGE_TOOL, CHANGE_LABEL, UNDO_LABEL, LINK_PICKER_TITLE,
        UNDO_ADD_BLOCK, UNDO_SPLIT, UNDO_MERGE, UNDO_DELETE, UNDO_SPEAKER, UNDO_RESTORE, ADD_BELOW, UNDO_LINE_BREAK, UNDO_TYPING, CLOSE,
        RENUMBER, OPEN_CHECK_CENTER, UNDO_ADD_OPTION, UNDO_KIND, UNDO_CUE, TAB_TIME, TAB_REF, TAB_PROPS, TAB_MODS,
        NO_SELECTED_BLOCK, KIND_FIELD, SPEAKER_FIELD, CHANGE, KIND_CHANGE_TITLE, DIRECTION_TITLE, ADD_CUE, REMOVE_CUE, NO_ASSETS,
        LAYERS_TITLE, LAYERS_SUB, LAYER_DIRECTION, LAYER_DIRECTION_SUB, LAYER_MEMO, LAYER_MEMO_SUB, LAYER_SYSTEM, LAYER_SYSTEM_SUB,
        LAYER_MARKS, LAYER_MARKS_SUB, LAYER_ADDRESS, LAYER_ADDRESS_SUB, LAYER_PAREN, LAYER_PAREN_SUB, PREVIEW_SECTION, PREVIEW_WRITE,
        PREVIEW_DIALOGUE, PREVIEW_READ_ALOUD, previewChip(PREVIEW_DIALOGUE), NO_CHANGE, SCENE_TIME_SECTION, CHANGE_MORE, IN_SCENE,
        BACKGROUND, REVEAL_ROW, EDIT_RECORD, SHOW_IN_MAP, PRECISION_EXACT, PRECISION_APPROX, PREVIOUS_SCENE, SAME_DAY, DIRECT_TIME,
        travelLine("하르트"), revealHere("1-7 #044"), selectedBlockWide("고른 블록 · #043", "하르트"), refHeader("하르트"), SPEECH_ROW,
        RECENT_LINES, RELATIONS_NOW, NOT_MET, HERE, hiddenRelations(1), deletedPartners("노아"), REF_NOT_DIALOGUE, STAGE_FIELD, STAGE_LEFT, STAGE_CENTER,
        STAGE_RIGHT, VOICE_FIELD, VOICE_AUTO, VOICE_FIXED, CHANGES_FIELD, SOURCE_FIELD, CHANGE_SPEAKER, RESTORE, voiceFixed("11월 20일"),
        KEEP, UNDO_KEEP, REVEAL_SUFFIX, nameRevealWide("1-9"), optionVoiceLabel("#006-1"),
        changeLink("관계", "리엔 → 하르트"), recorderSource("11월 22일 08:41"), FIELDS_TITLE, STAGE_SETTING_SUB, VOICE_SETTING_SUB,
        ID_FORMAT, FORMAT_PATTERN, FORMAT_MANUAL, FIXED_ON_EXPORT, patternSub("HRT_0107_043"), MANUAL_SUB,
        RELINK, TRASH_RECORD, LINK_TO_BLOCK, RECORD_CHANGE_ROW, recordTrashed("◆ 관계 · 리엔 → 하르트"), blockRestored("#043"), UNDO_RELINK, MOD_TODAY, MOD_SCENE, MOD_CHECKS, MOD_MEMO, MODS_FOOTER,
        checkPreReveal(1), checkNoSpeaker(1), MODS_NO_CHECKS, MODS_SCENE_EMPTY, UNDO_TRASH_RECORD,
    )

    /** Buttons (linted with the button rules). */
    val buttons: List<String> = listOf(
        DONE, LINE_BREAK, NEW_SCENE, SET_LENS_TO_SCENE, CLOSE, CHANGE, REMOVE_CUE, CHANGE_MORE, EDIT_RECORD, SHOW_IN_MAP,
        CHANGE_SPEAKER, RESTORE, KEEP, RELINK, TRASH_RECORD, LINK_TO_BLOCK,
    )
}
