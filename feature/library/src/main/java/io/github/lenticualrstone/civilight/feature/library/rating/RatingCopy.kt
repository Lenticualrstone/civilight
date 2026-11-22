package io.github.lenticualrstone.civilight.feature.library.rating

/**
 * Copy of 수위 등급 screens and sheets (LIB-22 … LIB-24; boards `RatingSettings`, `RHRatingSheet`,
 * `RHRatingDown`, `LibMenu`). Sheet rows, notes and ◇ rows come from `domain:library` `RatingSheet`.
 */
object RatingCopy {
    const val RATING = "수위 등급"
    const val CLOSE = "닫기"
    const val ARROW = "→"
    const val CANCEL = "취소"

    // 설정 · 수위 등급 (LIB-23)
    const val SETTINGS_SMALL = "설정"
    const val SETTINGS_INTRO = "등급별 기본값 · 프로젝트마다 따로 바꾸기 가능"
    const val R18_HIDE = "만들 때 숨기기"
    const val R18_HIDE_SUB = "지문 · PIN으로 열림"
    const val CAPTURE = "화면 캡처 가리기"
    const val R18_CAPTURE_SUB = "스크린샷 · 화면 녹화 · 최근 앱 미리 보기"
    const val NAME_MASK = "이름 가리기"
    const val R18_NAME_MASK_SUB = "위젯 · 알림 · 최근 항목에 ‘R-18 프로젝트’"
    const val RECORDER_MASK = "라이프 레코더 연결 가리기"
    const val R18_RECORDER_MASK_SUB = "기록의 연결에 등급만 표시"
    const val ALL_LABEL_ONLY = "라벨만"
    const val ALL_RATINGS = "모든 등급"
    const val EXPORT_COVER = "내보내기 표지에 등급 표시"
    const val EXPORT_COVER_SUB = "파일 · 인쇄"

    /** `이미 있는 <rating> 프로젝트 <N>`: the title of the `같이 바꾸기` list, from the tag's own words. */
    const val EXISTING_PREFIX = "이미 있는"
    const val PROJECT = "프로젝트"
    const val APPLY_TOGETHER = "같이 바꾸기"
}
