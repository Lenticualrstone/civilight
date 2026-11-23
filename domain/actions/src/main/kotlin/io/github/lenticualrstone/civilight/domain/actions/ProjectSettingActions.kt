package io.github.lenticualrstone.civilight.domain.actions

import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/**
 * (0.2.0) Project settings added in 0.2.0. Pure `ProjectDoc -> ProjectDoc`; like the other 프로젝트
 * 설정 switches the caller writes them without a toast (LIB-91 `ED`). Nothing else changes: turning
 * a switch off hides, it never deletes (scope §8 rule 1).
 */
object ProjectSettingActions {
    /** `게임 제작 도구` (G-1, LIB-91). Always stores `true` or `false`; game data is untouched either way. */
    fun setGameTools(doc: ProjectDoc, on: Boolean, ctx: ActionContext): ProjectDoc {
        if (doc.project.gameTools == on) return doc
        return doc.copy(project = doc.project.copy(gameTools = on, updatedAt = ctx.now, updatedBy = ctx.device))
    }

    /** `용어 후보` (WLD-73, S-5). Stored `잇기 · 아님` decisions are kept either way. */
    fun setTermCandidates(doc: ProjectDoc, on: Boolean, ctx: ActionContext): ProjectDoc {
        if (doc.project.termCandidates == on) return doc
        return doc.copy(project = doc.project.copy(termCandidates = on, updatedAt = ctx.now, updatedBy = ctx.device))
    }
}
