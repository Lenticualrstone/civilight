package io.github.lenticualrstone.civilight.feature.library.backup

/** 설정 › 보관 (LIB-57, LIB-61, LIB-62; boards `ErrBackup`, `Settings`). */
object BackupCopy {
    const val SMALL = "설정"
    const val TITLE = "보관"
    const val SECTION = "보관"
    const val DRIVE = "Google Drive 자동 백업"
    const val DRIVE_SUB = "하루 한 번 · 와이파이 · 원본은 기기에"
    const val LAST_BACKUP = "마지막 백업"
    const val HISTORY = "백업 기록"
    const val HISTORY_SUB = "30일 보관"
    const val RECOVERY = "복구 암호"
    const val RECOVERY_SUB = "숨긴 프로젝트 백업용"
    const val RECOVERY_SET = "정해 둠"
    const val RECOVERY_DEFINE = "정하기"
    const val RECOVERY_CHANGE = "바꾸기"

    // LIB-62 `기기 저장 공간` (board `ErrBackup`)
    const val STORAGE = "기기 저장 공간"
    const val STORAGE_FREE = "남은 공간"
    const val STORAGE_PROJECTS = "프로젝트"

    /** Turning the switch off or on could not reach Google (needsExternal stub; copy not in sources). */
    const val TODO_COPY_LIB57_DRIVE_NOT_READY = "TODO_COPY_LIB57_DRIVE_NOT_READY"
}

/** 복구 암호 정하기 (LIB-58, board `RHRecoveryPw`). */
object RecoveryCopy {
    const val SMALL = "설정 › 보관"
    const val TITLE = "복구 암호 정하기"
    const val HIDDEN_PROJECTS = "숨긴 프로젝트"
    const val CARD_SUB = "이 암호로 잠금 · Google Drive 자동 백업"
    const val FIELD = "복구 암호"
    const val REPEAT = "한 번 더"
    const val STRENGTH = "강도"
    const val CHARS = "자"
    const val SHOW = "보이기"
    const val WARNING = "잊으면 숨긴 프로젝트 백업을 열 수 없음"
    const val WARNING_SUB = "앱 · 서버에 저장 안 함 · 찾아 줄 수 없음"
    const val LOCKED_TOGETHER = "함께 잠금"
    const val HIDDEN_ITEMS = "숨긴 항목"
    const val RESTORE_WHEN = "되살릴 때"
    const val RESTORE_WHEN_VALUE = "이 암호 · 그 뒤 기기 지문"
    const val SET = "정하기"
    const val STRENGTH_GOOD = "좋음"

    /** Strength words other than `좋음` (LIB-58: not in sources). */
    const val TODO_COPY_LIB58_STRENGTH_WEAK = "TODO_COPY_LIB58_STRENGTH_WEAK"
    const val TODO_COPY_LIB58_STRENGTH_FAIR = "TODO_COPY_LIB58_STRENGTH_FAIR"
    const val TODO_COPY_LIB58_STRENGTH_STRONG = "TODO_COPY_LIB58_STRENGTH_STRONG"

    /** The eye button while the password shows (the board only labels `보이기`). */
    const val TODO_COPY_LIB58_HIDE = "TODO_COPY_LIB58_HIDE"

    /** `정하기` failed (Keystore or disk; copy not in sources). */
    const val TODO_COPY_LIB58_SET_FAILED = "TODO_COPY_LIB58_SET_FAILED"

    /** Under `한 번 더` when the two fields differ (copy not in sources). */
    const val TODO_COPY_LIB58_MISMATCH = "TODO_COPY_LIB58_MISMATCH"
}

/** 백업에서 되살리기 and 되살리는 중 (LIB-59, LIB-60; boards `Restore`, `Restoring`). */
object RestoreCopy {
    const val SMALL = "설정 › 보관"
    const val TITLE = "백업에서 되살리기"
    const val CONNECTED = "Google Drive · 계정 연결됨"
    const val RUNS = "백업 · 30일"
    const val SAME_NAME = "같은 이름이 이미 있을 때"
    const val AS_NEW = "새 프로젝트로"
    const val OVERWRITE = "덮어쓰기"
    const val NOTE = "숨긴 프로젝트 · 복구 암호로 열고 이 기기 지문으로 다시 잠금"
    const val HIDDEN_PROJECTS = "숨긴 프로젝트"
    const val CONNECT = "연결"
    const val PASSWORD_FIELD = "복구 암호"
    const val CANCEL = "취소"
    const val OPEN = "열기"

    const val RESTORING_SMALL = "되살리기"
    const val DRIVE = "Google Drive"
    const val BACKUP = "백업"
    const val ABOUT = "약"
    const val SECONDS = "초"

    /** Not connected: sign-in comes first (LIB-59: copy not in sources). */
    const val TODO_COPY_LIB59_NOT_CONNECTED = "TODO_COPY_LIB59_NOT_CONNECTED"

    /** No run within 30 days (copy not in sources). */
    const val TODO_COPY_LIB59_NO_RUNS = "TODO_COPY_LIB59_NO_RUNS"

    /** The per-project checklist's label (§6 Q25 `ED`: undrawn). */
    const val TODO_COPY_LIB59_PROJECTS = "TODO_COPY_LIB59_PROJECTS"

    /** A visible project whose name the run index does not hold (an index written elsewhere). */
    const val TODO_COPY_LIB59_UNNAMED_PROJECT = "TODO_COPY_LIB59_UNNAMED_PROJECT"

    /** The 복구 암호 sheet's title (LIB-54 sheet reused; `암호 파일` names one file). */
    const val PASSWORD_TITLE = "복구 암호"

    /** Wrong 복구 암호 (LIB-54 `ED`: field error, retry; copy not in sources). */
    const val TODO_COPY_LIB59_WRONG_PASSWORD = "TODO_COPY_LIB59_WRONG_PASSWORD"

    /** Some files could not be restored (gone, damaged, newer; copy not in sources). */
    const val TODO_COPY_LIB60_PARTLY_FAILED = "TODO_COPY_LIB60_PARTLY_FAILED"

    /** 되살리는 중 reached with no restore running (after a process death; copy not in sources). */
    const val TODO_COPY_LIB60_NOT_RUNNING = "TODO_COPY_LIB60_NOT_RUNNING"
}
