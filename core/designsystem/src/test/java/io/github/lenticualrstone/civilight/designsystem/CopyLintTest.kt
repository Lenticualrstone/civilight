package io.github.lenticualrstone.civilight.designsystem

import io.github.lenticualrstone.civilight.designsystem.text.CopyLint
import io.github.lenticualrstone.civilight.designsystem.text.LintIssue
import io.github.lenticualrstone.civilight.designsystem.text.lintString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CopyLintTest {

    private fun ok(s: String, button: Boolean = false) = assertEquals("'$s'", emptyList<LintIssue>(), lintString(s, button))

    private fun flags(s: String, issue: LintIssue, button: Boolean = false) =
        assertTrue("'$s' should give $issue but gave ${lintString(s, button)}", issue in lintString(s, button))

    @Test
    fun `T-DS-24 separators`() {
        ok("이름 가림 · 검색 · 위젯 · 알림 제외")
        flags("이름 가림·검색 제외", LintIssue.UNSPACED_DOT)
        flags("R-15 — 기본", LintIssue.EM_DASH_SEPARATOR)
        ok("—")
        ok("처음 설정 · 1/4")
        ok("18 / 24")
        flags("받기 / 고치기", LintIssue.SLASH_LIST)
    }

    @Test
    fun `T-DS-25 vocabulary`() {
        listOf(
            "블록 #043 삭제", "화자 미지정", "복원 중", "이미지 선택", "1-5 시점 값 수정 중", "미리보기", "장면 4",
            "넘기기", "지문으로만 열림", "변경",
        ).forEach { flags(it, LintIssue.VOCAB) }
        listOf(
            "블록 #043 지움 · ◆ 1개는 #044 자리로", "휴지통 · 30일 뒤 삭제", "30일 뒤 자동 삭제", "영구 삭제",
            "영구 삭제 · 노아", "온보딩 후 삭제", "예시 프로젝트 삭제됨", "‘애니메이션 삭제’가 켜져 있으면 줄임", "선택지",
            "주인공 (선택)", "마지막 수정 11월 20일 · 연결 4", "막 › 장면", "1-5 시점 값 고치는 중", "미리 보기", "건너뛰기",
            "지문 · PIN으로 열림",
        ).forEach { ok(it) }
    }

    @Test
    fun `T-DS-26 buttons`() {
        flags("확인", LintIssue.LONE_CONFIRM, button = true)
        ok("확인: 프로젝트 이름 입력")
        ok("확인 필요")
        listOf("휴지통으로", "되돌리기", "쓰기 시작", "바꾸기").forEach { ok(it, button = true) }
    }

    @Test
    fun `T-DS-27 symbols`() {
        listOf("◆ 변화", "◇ 첫 공개", "▲ 공개 전 사용", "▸ 배경", "♪ 잔불", "⌁ 작가 메모", "‹ 서재", "1-1 없음 → 1-3 등불 결사", "???")
            .forEach { ok(it) }
        flags("★ 주연", LintIssue.SYMBOL)
        flags("✓ 완료", LintIssue.SYMBOL)
        flags("🔥", LintIssue.SYMBOL)
    }

    @Test
    fun `T-DS-28 notation`() {
        ok("1-7 · 씬 3 · 2일째 새벽")
        ok("#043")
    }

    @Test
    fun `strings xml lint`() {
        val xml = """
            <resources>
                <string name="btn_confirm">확인</string>
                <string name="label_hidden">이름 가림 · 검색 제외</string>
                <string name="sample_line">장면은 바뀌지 않았다</string>
                <string name="label_bad">이름·검색</string>
            </resources>
        """.trimIndent()
        val findings = CopyLint.lintStringsXml(xml)
        assertEquals(listOf("btn_confirm" to LintIssue.LONE_CONFIRM, "label_bad" to LintIssue.UNSPACED_DOT), findings.map { it.key to it.issue })
    }
}
