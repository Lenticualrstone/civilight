package io.github.lenticualrstone.civilight.feature.library.export

/**
 * Copy of the 내보내기 sheet (LIB-41; board `Export`). Format cards, option rows, the primary label and
 * warnings come from `domain:library` (`ExportDefaults`, `ExportSheets`, `ExportRow`).
 */
object ExportCopy {
    const val SECTION_FORMAT = "형식"

    /** Sheet head (`내보내기`), also the 서재's 여러 프로젝트 entry label (LIB-72 `ED`). */
    const val HEADER = "내보내기"

    /** The `.civilight` `암호` field (LIB-41). */
    const val PASSWORD = "암호"
    const val CANCEL = "취소"

    /** While the file is built (DS-47 `DelayedProgress`: shown only after a second). */
    const val WORKING = "만드는 중"
    const val SEP = " · "

    /** Second password field (`RHRecoveryPw` `한 번 더`, reused per LIB-41). */
    const val PASSWORD_REPEAT = "한 번 더"

    /** The `전체` choice of `범위` (the domain's label for an empty range). */
    const val RANGE_ALL = "전체"

    /** After the file was saved (LIB-41: "a done notification or toast (copy: not in sources)"). */
    const val DONE = "파일 저장함"

    /** The chosen place could not be written (no source copy). */
    const val SAVE_FAILED = "저장하지 못함"

    /** `한 번 더` differs from `암호` (no source copy). */
    const val PASSWORD_MISMATCH = "암호가 서로 다름"
}
