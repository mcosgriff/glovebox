package io.cosgriff.glovebox.data.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.cosgriff.glovebox.data.DataWipe
import io.cosgriff.glovebox.data.crypto.DatabaseKeyManager
import io.cosgriff.glovebox.data.crypto.PhotoCrypto
import io.cosgriff.glovebox.data.db.GloveboxDatabase
import io.cosgriff.glovebox.data.nhtsa.NhtsaClient
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides @Singleton
    fun keyManager(@ApplicationContext c: Context) = DatabaseKeyManager(c)

    @Provides @Singleton
    fun photoCrypto(@ApplicationContext c: Context) = PhotoCrypto(c)

    @Provides @Singleton
    fun database(@ApplicationContext c: Context, keys: DatabaseKeyManager) = GloveboxDatabase.open(c, keys)

    @Provides fun vehicleDao(db: GloveboxDatabase) = db.vehicles()
    @Provides fun serviceDao(db: GloveboxDatabase) = db.service()
    @Provides fun scheduleDao(db: GloveboxDatabase) = db.schedule()
    @Provides fun odometerDao(db: GloveboxDatabase) = db.odometer()
    @Provides fun torqueDao(db: GloveboxDatabase) = db.torque()

    @Provides @Singleton
    fun settings(@ApplicationContext c: Context) = SettingsRepository(c)

    @Provides @Singleton
    fun dataWipe(
        @ApplicationContext c: Context,
        db: GloveboxDatabase,
        keys: DatabaseKeyManager,
        photos: PhotoCrypto,
    ) = DataWipe(c, db, keys, photos)

    @Provides @Singleton
    fun httpClient() = HttpClient(OkHttp) {
        expectSuccess = true
        install(HttpTimeout) { requestTimeoutMillis = 15_000 }
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    @Provides @Singleton
    fun nhtsa(http: HttpClient) = NhtsaClient(http)
}
