package io.github.lenticualrstone.civilight.feature.library.settings

/** 설정 · 펼친 화면 배치 (LIB-66, board `LayoutSettings`). */
object LayoutSettingsCopy {
    const val SMALL = "설정 › 화면"
    const val TITLE = "펼친 화면"
    const val SECTION = "펼친 화면 배치"
    const val LIST_DETAIL = "목록 + 상세"
    const val LIST_DETAIL_SUB = "왼쪽 목록 · 오른쪽 고른 항목"
    const val CENTER = "가운데 한 칸"
    const val CENTER_SUB = "읽기 폭 유지 · 양옆 여백"
    const val NOTE = "서재 · 홈 · 인물 · 세계 · 연표 · 기록함 · 점검에 적용 · 커버 화면은 그대로 · 스크립트는 옆 패널 방식 유지"
}

/** 설정 · 움직임 · 투명도 (LIB-67, boards `ReduceSettings`, `ReduceTransparency`). */
object MotionSettingsCopy {
    const val SMALL = "설정"
    const val TITLE = "움직임 · 투명도"
    const val SECTION_ANIMATION = "애니메이션"
    const val FOLLOW = "기기 설정 따름"
    const val FOLLOW_SUB = "‘애니메이션 삭제’가 켜져 있으면 줄임"
    const val ALWAYS = "항상 줄이기"
    const val ALWAYS_SUB = "이동 없이 페이드 120 ms"
    const val NEVER = "줄이지 않음"
    const val SECTION_TRANSPARENCY = "투명도"
    const val REDUCE_TRANSPARENCY = "투명도 줄이기"
    const val REDUCE_TRANSPARENCY_SUB = "유리 대신 불투명 막대 · 흐림 없음"
    const val PREVIEW_GLASS = "유리"
    const val PREVIEW_OPAQUE = "불투명"
    const val RECORDER_GLOW = "라이프 레코더 빛"
    const val RECORDER_GLOW_SUB = "녹음 중 아래 빛 띠"
}

/**
 * The manuscript line inside the two `투명도` previews (board `ReduceSettings`). Story content of the
 * example, not UI copy: not linted.
 */
object MotionPreviewSample {
    const val LINE = "멈추지 마. 저건 등불이 아니야. 바람이 잿더미를 쓸고 지나갔다."
}

/** 설정 · 지도 · 이미지 · 연표 (LIB-68 container; rows CNV-19, WLD-67, STO-42; board `SettingsMore`). */
object MoreSettingsCopy {
    const val SMALL = "설정"
    const val TITLE = "지도 · 이미지 · 연표"

    // 이미지 (CNV-19: CNV-30 holo default, CNV-26 presets)
    const val SECTION_IMAGE = "이미지"
    const val HOLO = "홀로그램 효과 기본"
    const val HOLO_SUB = "새 이미지에 · 이미지마다 바꾸기 가능"
    const val PRESETS = "기본 이미지"
    const val PRESETS_SUB = "8종 · 앱에 포함 · 오프라인"
    const val PRESETS_VALUE = "보기"

    // 지도 (WLD-67)
    const val SECTION_MAP = "지도"
    const val MAP_STYLE = "새 지도 그림"
    const val MAP_STYLE_SUB = "색면 · 먹선 · 색 · 양피지"
    const val STYLE_SOFT = "색면"
    const val STYLE_INK = "먹선"
    const val STYLE_COLOR = "색"
    const val STYLE_PARCHMENT = "양피지"
    const val TERRAIN = "지형 만들기 · 글로 설명"
    const val TERRAIN_SUB = "기기 안 · 클라우드(정확도↑ · 전송)"
    const val ENGINE_DEVICE = "기기 안"
    const val ENGINE_CLOUD = "클라우드"
    const val ENGINE_CLOUD_SUB = "정확도↑ · 전송"
    const val LARGE_EXPORT = "큰 지도 내보내기"
    const val LARGE_EXPORT_SUB = "4K · 8K"
    const val EXPORT_4K = "4K"
    const val EXPORT_8K = "8K"

    // 연표 · 때 (STO-17, STO-42)
    const val SECTION_TIME = "연표 · 때"
    const val NOTATION = "때 표기 기본"
    const val NOTATION_SUB = "새 프로젝트 · 프로젝트마다 바꾸기 가능"
    const val NOTATION_EPISODE = "에피소드 기준"
    const val NOTATION_STORY_START = "이야기 시작부터"
    const val CHECK_TRAVEL = "이동 점검"
    const val CHECK_TRAVEL_SUB = "이동 시간 부족 · 같은 때 · 동시에 두 장소"
    const val CHECK_NO_PLACE = "장소 없는 씬 알림"
    const val CHECK_NO_PLACE_SUB = "점검 센터 ◇"

    /** The value pickers' close word (DS sheet head). */
    const val CLOSE = "닫기"
}
