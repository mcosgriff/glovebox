package io.cosgriff.glovebox

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.cosgriff.glovebox.data.db.KnownScheduleLists
import io.cosgriff.glovebox.ui.schedule.KnownScheduleCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * The scope for work that has to outlive the screen that started it: an Undo tapped after the screen
 * closed, a file the picker hands back after the lock took the screens away. On the main thread, as
 * the screens are; one failure doesn't cancel the rest.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton @ApplicationScope
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** The bundled makers' schedules, from the app's assets, for `:data`'s `MakersSchedules`. */
    @Provides
    fun knownScheduleLists(catalog: KnownScheduleCatalog): KnownScheduleLists = catalog
}
