package io.github.lenticualrstone.civilight.feature.library.openfile

/**
 * Copy of 파일에서 열기 (LIB-52 … LIB-54; boards `RHOpenFile`, `RHNewerFile`, `RHEncryptedFile`). The
 * sheet words themselves come from `domain:library` (`OpenFileSheet`, `NewerFileSheet`,
 * `EncryptedFileSheet`); this object holds the screen frame and the states the sources leave open.
 */
object OpenFileCopy {
    const val SMALL = "Civilight"
    const val TITLE = "파일에서 열기"

    /** The file could not be read, or it is not a `.civilight` project (copy not in sources). */
    const val NOT_A_PROJECT = "프로젝트 파일 아님"

    /** `manifest.json`, `project.json` or `story/index.json` damaged: nothing imported (LIB-55, copy not in sources). */
    const val DAMAGED = "파일 손상 · 열 수 없음"

    /** Wrong password on `RHEncryptedFile` (LIB-54 `ED`: field error, retry, no lockout; copy not in sources). */
    const val WRONG_PASSWORD = "암호가 맞지 않음"

    /** The file names a key derivation or cipher this app cannot use (copy not in sources). */
    const val UNSUPPORTED = "지원하지 않는 암호 방식"

    /** The import itself failed after `열기` (disk full, a key clash; copy not in sources). */
    const val IMPORT_FAILED = "열지 못함"

    /** `.civilight-recorder` sheet (LIB-48 `ED`: undrawn; the 서재 row's words). */
    const val RECORDER_TITLE = "라이프 레코더"
    const val RECORDS = "기록"

    /** A recorder library file from a newer app (LIB-48 + LIB-53; copy not in sources). */
    const val NEWER = "새 버전 앱에서 만든 파일 · 업데이트 뒤 다시 열기"
}
