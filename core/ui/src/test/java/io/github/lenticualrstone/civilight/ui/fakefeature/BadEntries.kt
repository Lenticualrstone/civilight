@file:JvmName("BadEntries")

package io.github.lenticualrstone.civilight.ui.fakefeature

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

/** A feature that got the signature wrong (missing `deps`); the loader must refuse it loudly. */
@Suppress("UNUSED_PARAMETER")
fun EntryProviderScope<NavKey>.badEntries(count: Int) = Unit
