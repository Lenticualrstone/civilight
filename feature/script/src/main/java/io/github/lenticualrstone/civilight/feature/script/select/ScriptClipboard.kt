package io.github.lenticualrstone.civilight.feature.script.select

import io.github.lenticualrstone.civilight.domain.script.BlockMove
import io.github.lenticualrstone.civilight.model.Key
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The app clipboard of blocks (SCR-40). It outlives the script screen (each back-stack entry has its
 * own view model), so cutting in 1-7 and pasting in 1-8 works. It belongs to one project: blocks
 * name that project's characters, terms and records, so another project never pastes them.
 */
object ScriptClipboard {
    data class Entry(val projectKey: Key, val clipboard: BlockMove.Clipboard) {
        val count: Int get() = clipboard.blocks.size
    }

    private val _entry = MutableStateFlow<Entry?>(null)
    val entry: StateFlow<Entry?> = _entry.asStateFlow()

    fun put(projectKey: Key, clipboard: BlockMove.Clipboard) {
        _entry.value = Entry(projectKey, clipboard).takeIf { clipboard.blocks.isNotEmpty() }
    }

    /** The clipboard of [projectKey], if it holds blocks of that project. */
    fun of(projectKey: Key): Entry? = _entry.value?.takeIf { it.projectKey == projectKey }

    /** A cut is pasted once (its blocks moved); a copy can be pasted again. */
    fun afterPaste(entry: Entry) {
        if (entry.clipboard.cut) _entry.compareAndSet(entry, null)
    }
}
