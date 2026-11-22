package io.github.lenticualrstone.civilight.ui.feature

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import io.github.lenticualrstone.civilight.navigation.SheetKey
import kotlin.reflect.KClass

/**
 * What sheet content can do with the host it is shown in. It is also the sheet's [ColumnScope], so
 * `ColumnScope` content (pickers, `SheetScrollBody`, `SheetActions` weights) can be called directly.
 */
interface SheetScope : ColumnScope {
    /** Closes this sheet (with its exit motion). */
    fun close()

    /** Opens another sheet over this one (e.g. 때 바꾸기 미리 보기 from 씬 · 장소와 때). */
    fun open(sheet: SheetKey)

    /** True on inner screens, where the sheet is drawn centred (DS-23) instead of from the bottom. */
    val inner: Boolean
}

/** Draws one sheet's content (title row, body, actions) inside the shell's sheet surface. */
typealias SheetContent<S> = @Composable SheetScope.(S) -> Unit

/**
 * Content of [io.github.lenticualrstone.civilight.navigation.SharedSheet]s, registered by the owning
 * feature from its entries function, looked up by every screen's sheet host. Feature-local sheets
 * may be registered too; the host also accepts local content directly.
 */
class SheetRegistry {
    private val contents = HashMap<KClass<out SheetKey>, Registered<*>>()

    fun <S : SheetKey> register(type: KClass<S>, content: SheetContent<S>) {
        require(type !in contents) { "sheet ${type.simpleName} is already registered" }
        contents[type] = Registered(content)
    }

    inline fun <reified S : SheetKey> register(noinline content: SheetContent<S>) = register(S::class, content)

    /** The registered content for [sheet]'s class, or null. */
    fun contentFor(sheet: SheetKey): SheetContent<SheetKey>? {
        val registered = contents[sheet::class] ?: return null
        return { key -> registered.Render(this, key) }
    }

    fun isRegistered(type: KClass<out SheetKey>): Boolean = type in contents

    /**
     * Keeps the typed content and casts only the key when drawing. Casting the composable lambda itself
     * (`as SheetContent<SheetKey>`) fails at run time: Kotlin checks a function's arity on such casts,
     * and a composable lambda reports the arity of its first interface.
     */
    private class Registered<S : SheetKey>(private val content: SheetContent<S>) {
        @Composable
        fun Render(scope: SheetScope, sheet: SheetKey) {
            @Suppress("UNCHECKED_CAST")
            content(scope, sheet as S)
        }
    }
}
