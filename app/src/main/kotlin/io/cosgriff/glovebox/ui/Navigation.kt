package io.cosgriff.glovebox.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.toMutableStateMap
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.savedstate.SavedState
import androidx.savedstate.savedState
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState

/** Where a screen's ViewModels find the route their screen was opened with (see [route]). */
private const val ROUTE = "io.cosgriff.glovebox.route"

/**
 * Hands [key] to the ViewModels of [content] through their `SavedStateHandle`, as Navigation Compose
 * did. Navigation 3 scopes ViewModels to an entry but leaves telling them its key to the app.
 */
@Composable
internal fun ProvideRoute(key: NavKey, content: @Composable () -> Unit) {
    val owner = checkNotNull(LocalViewModelStoreOwner.current) { "No ViewModelStoreOwner for $key" }
    val withRoute = remember(owner, key) { RouteOwner(owner, key) }
    CompositionLocalProvider(LocalViewModelStoreOwner provides withRoute, content = content)
}

/** The entry's own store and factory, with the route added to the arguments a `SavedStateHandle` starts from. */
private class RouteOwner(owner: ViewModelStoreOwner, key: NavKey) :
    ViewModelStoreOwner by owner, HasDefaultViewModelProviderFactory {
    private val defaults = owner as HasDefaultViewModelProviderFactory
    private val args = savedState { putSavedState(ROUTE, encodeToSavedState(NavKeySerializer(), key)) }

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = defaults.defaultViewModelProviderFactory

    override val defaultViewModelCreationExtras: CreationExtras
        get() = MutableCreationExtras(defaults.defaultViewModelCreationExtras).apply { set(DEFAULT_ARGS_KEY, args) }
}

/** The route this ViewModel's screen was opened with. */
inline fun <reified T : NavKey> SavedStateHandle.route(): T =
    checkNotNull(routeOrNull<T>()) { "Not opened with a ${T::class.simpleName}" }

/** The route, if the screen was opened with a [T]; for a ViewModel that serves more than one route. */
inline fun <reified T : NavKey> SavedStateHandle.routeOrNull(): T? = routeKey() as? T

@PublishedApi
internal fun SavedStateHandle.routeKey(): NavKey? =
    get<SavedState>(ROUTE)?.let { decodeFromSavedState(NavKeySerializer<NavKey>(), it) }

/**
 * What a screen hands back to the one under it (a scanned VIN, a dash reading), by name. Kept with the
 * back stacks, so a result survives Android restarting the app. Values must be ones a Bundle holds.
 */
internal val ResultsSaver = Saver<SnapshotStateMap<String, Any>, List<Any>>(
    save = { map -> map.flatMap { (name, value) -> listOf(name, value) } },
    restore = { saved -> saved.chunked(2) { (name, value) -> name as String to value }.toMutableStateMap() },
)

/** Drops everything above the first [keep] entries. */
internal fun MutableList<NavKey>.trimTo(keep: Int) {
    while (size > keep) removeAt(lastIndex)
}
