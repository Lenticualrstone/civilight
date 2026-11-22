package io.github.lenticualrstone.civilight.domain.story.search

/** Screens with a command bar (STO-36). */
enum class CommandScreen { SCRIPT, HOME, STORY, CHECKS, PROFILE, TIMELINE, TIMELINE_ANCHOR, PAGE }

/** Placeholders of the bottom command bar per screen (STO-36), verbatim from the boards. */
object CommandBar {
    fun placeholder(screen: CommandScreen): String = when (screen) {
        CommandScreen.SCRIPT -> "명령 또는 검색 · 인물 · 블록 · 씬 이동"
        CommandScreen.HOME -> "명령 또는 검색 · 인물 · 용어 · 대사"
        CommandScreen.STORY -> "명령 또는 검색 · 씬 이동"
        CommandScreen.CHECKS, CommandScreen.PROFILE -> "명령 또는 검색"
        CommandScreen.TIMELINE -> "사건 · 연도 찾기"
        CommandScreen.TIMELINE_ANCHOR -> "사건 · 때 찾기"
        CommandScreen.PAGE -> "@로 인물 · 용어 연결, /로 블록 추가"
    }
}
