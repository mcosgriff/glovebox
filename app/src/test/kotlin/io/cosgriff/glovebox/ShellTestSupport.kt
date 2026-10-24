package io.cosgriff.glovebox

import io.cosgriff.glovebox.data.log.VehicleLogs
import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.internal.builders.ActivityComponentBuilder
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory
import dagger.hilt.android.internal.lifecycle.HiltViewModelFactory
import dagger.hilt.android.internal.lifecycle.HiltWrapper_HiltViewModelFactory_ActivityCreatorEntryPoint
import dagger.hilt.android.internal.lifecycle.RetainedLifecycleImpl
import dagger.hilt.android.internal.managers.ActivityComponentManager
import dagger.hilt.android.internal.managers.HiltWrapper_ActivityRetainedComponentManager_ActivityRetainedComponentBuilderEntryPoint
import dagger.hilt.android.internal.managers.HiltWrapper_ActivityRetainedComponentManager_ActivityRetainedLifecycleEntryPoint
import dagger.hilt.android.internal.managers.SavedStateHandleHolder
import dagger.hilt.android.ActivityRetainedLifecycle
import dagger.hilt.android.ViewModelLifecycle
import dagger.hilt.internal.GeneratedComponent
import dagger.hilt.internal.GeneratedComponentManager
import io.cosgriff.glovebox.data.db.GloveboxDatabase
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.ServiceRecordItemEntity
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.di.DataModule
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.notify.NotifyEntryPoint
import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.widget.WidgetEntryPoint
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.lang.reflect.Constructor
import java.lang.reflect.ParameterizedType
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Provider

/**
 * The application the shell tests run in, in place of `GloveboxApp` (whose Hilt graph would open the
 * SQLCipher database with a Keystore key, neither of which exists on the JVM). It answers Hilt's entry
 * points itself, from [ShellGraph]: the workers' and the widget's, and the chain `MainActivity`'s
 * injection and `hiltViewModel()` go through. Nothing in it reaches the network.
 */
class ShellTestApp : Application(), GeneratedComponentManager<Any> {
    lateinit var graph: ShellGraph
        private set

    override fun onCreate() {
        super.onCreate()
        AppText.install(this)
        graph = ShellGraph(this)
    }

    override fun generatedComponent(): Any = graph.singleton

    companion object {
        val current: ShellTestApp get() = ApplicationProvider.getApplicationContext()
        val graph: ShellGraph get() = current.graph
    }
}

/**
 * A small stand-in for the app's Dagger graph: an in-memory Room database (through Android's own driver, as
 * `:data`'s tests do), `DataModule`'s own providers for everything else it provides, and every other class
 * built through its `@Inject` constructor, once. ViewModels are made fresh, each with its own SavedStateHandle.
 */
class ShellGraph(private val app: Application) {
    val db: GloveboxDatabase = Room.inMemoryDatabaseBuilder(app, GloveboxDatabase::class.java)
        .setDriver(androidSqliteDriver())
        .build()

    /** As production's `@ApplicationScope`: on the main thread, one failure doesn't cancel the rest. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val made = mutableMapOf<Class<*>, Any>(
        Context::class.java to app,
        Application::class.java to app,
        GloveboxDatabase::class.java to db,
        CoroutineScope::class.java to scope,
        HttpClient::class.java to HttpClient(OfflineEngine()),
    )

    init {
        // DataStore keeps one store per file for the life of the JVM, so a test would see the last one's settings.
        clearStore("io.cosgriff.glovebox.data.settings.SettingsRepositoryKt", "getDataStore")
        clearStore("io.cosgriff.glovebox.data.settings.NoticeLogStoreKt", "getNoticeLog")
    }

    @Suppress("UNCHECKED_CAST")
    private fun clearStore(file: String, getter: String) {
        val method = Class.forName(file).declaredMethods.first { it.name == getter }
        method.isAccessible = true
        val store = method.invoke(null, app) as DataStore<Preferences>
        runBlocking { store.edit { it.clear() } }
    }

    inline fun <reified T : Any> get(): T = get(T::class.java)

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(type: Class<T>): T {
        made[type]?.let { return it as T }
        val built = provided(type) ?: construct(type, handle = null)
        made[type] = built
        return built
    }

    /** A ViewModel as `hiltViewModel()` would make it, never shared. */
    fun <T : ViewModel> viewModel(type: Class<T>, handle: SavedStateHandle): T = construct(type, handle)

    /** What `DataModule` provides, made by its own method. */
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> provided(type: Class<T>): T? {
        val method = DataModule::class.java.methods.firstOrNull { it.returnType == type && it.declaringClass == DataModule::class.java }
            ?: return null
        val args = method.genericParameterTypes.map { arg(it, handle = null) }.toTypedArray()
        return method.invoke(DataModule, *args) as T
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> construct(type: Class<T>, handle: SavedStateHandle?): T {
        val constructor = type.declaredConstructors.firstOrNull { it.isAnnotationPresent(Inject::class.java) } as Constructor<T>?
            ?: error("Nothing in the shell graph makes ${type.name}")
        constructor.isAccessible = true
        val args = constructor.genericParameterTypes.map { arg(it, handle) }.toTypedArray()
        return constructor.newInstance(*args)
    }

    private fun arg(type: java.lang.reflect.Type, handle: SavedStateHandle?): Any = when {
        type == SavedStateHandle::class.java -> checkNotNull(handle) { "A SavedStateHandle outside a ViewModel" }
        type is ParameterizedType && type.rawType == dagger.Lazy::class.java ->
            dagger.Lazy { get(type.actualTypeArguments[0] as Class<*>) }
        type is ParameterizedType && type.rawType == Provider::class.java ->
            Provider { get(type.actualTypeArguments[0] as Class<*>) }
        else -> get(type as Class<*>)
    }

    /** Hilt's singleton component, as the application hands it out. */
    val singleton: Any = Singleton()

    // ---- The parts of Hilt's generated components that the app's code reaches. ----

    private inner class Singleton :
        GeneratedComponent,
        HiltWrapper_ActivityRetainedComponentManager_ActivityRetainedComponentBuilderEntryPoint,
        NotifyEntryPoint,
        WidgetEntryPoint {
        override fun retainedComponentBuilder(): ActivityRetainedComponentBuilder = object : ActivityRetainedComponentBuilder {
            override fun savedStateHandleHolder(savedStateHandleHolder: SavedStateHandleHolder) = this
            override fun build(): ActivityRetainedComponent = Retained()
        }

        override fun vehicles() = db.vehicles()
        override fun logs() = get<VehicleLogs>()
        override fun settings() = get<SettingsRepository>()
        override fun noticeLog() = get<io.cosgriff.glovebox.data.settings.NoticeLogStore>()
        override fun recalls() = get<io.cosgriff.glovebox.data.nhtsa.RecallRepository>()
        override fun notifier() = get<io.cosgriff.glovebox.notify.Notifier>()
        override fun widgets() = get<io.cosgriff.glovebox.widget.GarageWidgets>()
        override fun garage() = get<io.cosgriff.glovebox.ui.garage.GarageOverview>()
    }

    private inner class Retained :
        ActivityRetainedComponent,
        GeneratedComponent,
        ActivityComponentManager.ActivityComponentBuilderEntryPoint,
        HiltWrapper_ActivityRetainedComponentManager_ActivityRetainedLifecycleEntryPoint {
        private val lifecycle = RetainedLifecycleImpl()
        override fun getActivityRetainedLifecycle(): ActivityRetainedLifecycle = lifecycle
        override fun activityComponentBuilder(): ActivityComponentBuilder = object : ActivityComponentBuilder {
            override fun activity(activity: Activity) = this
            override fun build(): ActivityComponent = ActivityScoped()
        }
    }

    private inner class ActivityScoped :
        ActivityComponent,
        GeneratedComponent,
        MainActivity_GeneratedInjector,
        DefaultViewModelFactories.ActivityEntryPoint,
        HiltWrapper_HiltViewModelFactory_ActivityCreatorEntryPoint {
        override fun injectMainActivity(mainActivity: MainActivity) = inject(mainActivity)

        override fun getHiltInternalFactoryFactory(): DefaultViewModelFactories.InternalFactoryFactory =
            DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(ViewModelKeys, ViewModels())

        override fun getViewModelKeys(): Map<Class<*>, Boolean> = ViewModelKeys
        override fun getViewModelComponentBuilder(): ViewModelComponentBuilder = ViewModels()
    }

    /** Field injection, as Dagger does it for `@Inject lateinit var`. */
    fun inject(target: Any) {
        var type: Class<*>? = target.javaClass
        while (type != null && type != Any::class.java) {
            type.declaredFields.filter { it.isAnnotationPresent(Inject::class.java) }.forEach { field ->
                field.isAccessible = true
                field.set(target, arg(field.genericType, handle = null))
            }
            type = type.superclass
        }
    }

    /** Every ViewModel with an `@Inject` constructor counts as a Hilt one. */
    private object ViewModelKeys : AbstractMap<Class<*>, Boolean>() {
        override val entries: Set<Map.Entry<Class<*>, Boolean>> = emptySet()
        override fun containsKey(key: Class<*>) =
            ViewModel::class.java.isAssignableFrom(key) && key.declaredConstructors.any { it.isAnnotationPresent(Inject::class.java) }
        override fun get(key: Class<*>) = if (containsKey(key)) true else null
    }

    private inner class ViewModels : ViewModelComponentBuilder {
        private var handle: SavedStateHandle? = null
        override fun savedStateHandle(handle: SavedStateHandle) = apply { this.handle = handle }
        override fun viewModelLifecycle(viewModelLifecycle: ViewModelLifecycle) = this
        override fun build(): ViewModelComponent = ViewModelScoped(checkNotNull(handle))
    }

    private inner class ViewModelScoped(private val handle: SavedStateHandle) :
        ViewModelComponent,
        GeneratedComponent,
        HiltViewModelFactory.ViewModelFactoriesEntryPoint {
        override fun getHiltViewModelMap(): Map<Class<*>, Provider<ViewModel>> = object : AbstractMap<Class<*>, Provider<ViewModel>>() {
            override val entries: Set<Map.Entry<Class<*>, Provider<ViewModel>>> = emptySet()
            @Suppress("UNCHECKED_CAST")
            override fun get(key: Class<*>): Provider<ViewModel> = Provider { viewModel(key as Class<ViewModel>, handle) }
        }

        override fun getHiltViewModelAssistedMap(): Map<Class<*>, Any> = emptyMap()
    }

    // ---- Test data. Made up: no real VINs, records or people. ----

    /** Settings as a test wants them, whatever an earlier test in this JVM left (DataStore files are per process). */
    fun settings(block: suspend SettingsRepository.() -> Unit) = runBlocking { get<SettingsRepository>().block() }

    fun addVehicle(
        make: String = "Toyota",
        model: String = "4Runner",
        year: Int = 2019,
        nickname: String? = null,
        reading: Int? = 61_000,
        on: LocalDate = LocalDate.now().minusDays(30),
    ): Long = runBlocking {
        db.vehicles().add(VehicleEntity(vin = null, year = year, make = make, model = model, nickname = nickname, createdOn = on), firstReading = reading)
    }

    /** One of the user's own reminders, tracked. */
    fun addItem(vehicleId: Long, name: String = "Oil change", everyMiles: Int? = 5_000, everyMonths: Int? = 6): Long = runBlocking {
        db.schedule().insertAll(listOf(ScheduleItemEntity(vehicleId = vehicleId, name = name, intervalMiles = everyMiles, intervalMonths = everyMonths)))
        db.schedule().allFor(vehicleId).last().id
    }

    /** A service record with one line done against [itemId]. */
    fun logService(vehicleId: Long, itemId: Long?, on: LocalDate, odometer: Int, label: String = "Oil change", shop: String? = null): Long = runBlocking {
        db.service().insertWithItems(
            ServiceRecordEntity(vehicleId = vehicleId, date = on, odometer = odometer, diy = shop == null, shopName = shop),
            listOf(ServiceRecordItemEntity(recordId = 0, scheduleItemId = itemId, label = label)),
        )
    }

    /** A vehicle in the garage with an oil change logged long enough ago to be overdue. */
    fun overdueVehicle(make: String = "Toyota", model: String = "Tacoma"): Pair<Long, Long> {
        val id = addVehicle(make = make, model = model, reading = 61_000)
        val item = addItem(id)
        logService(id, item, LocalDate.now().minusYears(1), 50_000)
        return id to item
    }

    fun close() {
        scope.cancel()
        db.close()
        // So the next test, whichever application it runs in, starts from the defaults.
        clearStore("io.cosgriff.glovebox.data.settings.SettingsRepositoryKt", "getDataStore")
        clearStore("io.cosgriff.glovebox.data.settings.NoticeLogStoreKt", "getNoticeLog")
    }
}

/** A tall phone, so a whole screen fits without scrolling. */
const val SHELL_PHONE = "w411dp-h1100dp-xhdpi"

/** Waits for [text] to be on screen: the app's screens read DataStore and Room off the main thread. */
fun ComposeTestRule.waitForText(text: String, substring: Boolean = false, timeoutMs: Long = 10_000) =
    waitUntil(timeoutMs) { onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty() }

/** As [waitForText], by a node's content description. */
fun ComposeTestRule.waitForDescription(description: String, timeoutMs: Long = 10_000) =
    waitUntil(timeoutMs) { onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty() }

fun string(id: Int, vararg args: Any): String = ShellTestApp.current.getString(id, *args)

/** Runs the main thread's queue until [done], for work that arrives there from another thread (DataStore, Room). */
fun idleMainUntil(timeoutMs: Long = 10_000, done: () -> Boolean) {
    val end = System.currentTimeMillis() + timeoutMs
    while (true) {
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        if (done()) return
        check(System.currentTimeMillis() < end) { "Still waiting after $timeoutMs ms" }
        Thread.sleep(5)
    }
}

/** Android's SQLite driver, as `:data`'s tests use; `:data` keeps the sqlite artifact to itself, so it is found by name. */
private fun androidSqliteDriver(): androidx.sqlite.SQLiteDriver =
    Class.forName("androidx.sqlite.driver.AndroidSQLiteDriver").getDeclaredConstructor().newInstance() as androidx.sqlite.SQLiteDriver

/** Every request fails as if the phone were offline: the shell tests never reach NHTSA. */
private class OfflineEngine : HttpClientEngineBase("offline") {
    override val config = HttpClientEngineConfig()

    @InternalAPI
    override suspend fun execute(data: HttpRequestData): HttpResponseData = throw IOException("Offline in tests: ${data.url.host}")
}

/** Taps the node showing [text] once it is on screen, scrolling to it where it sits in a scrolling page. */
fun ComposeTestRule.tap(text: String, substring: Boolean = false) {
    waitForText(text, substring)
    val node = onAllNodes(androidx.compose.ui.test.hasText(text, substring = substring)).onLast()
    scrollIntoView(node)
    node.performClick()
    waitForIdle()
}

/** Taps the node described as [description] once it is on screen. */
fun ComposeTestRule.tapDescription(description: String) {
    waitForDescription(description)
    val node = onAllNodes(androidx.compose.ui.test.hasContentDescription(description)).onLast()
    scrollIntoView(node)
    node.performClick()
    waitForIdle()
}

/** The screen's own back arrow. */
fun ComposeTestRule.back() = tapDescription(string(R.string.back))

/** Waits until nothing on screen shows [text]. */
fun ComposeTestRule.waitForNoText(text: String, timeoutMs: Long = 10_000) =
    waitUntil(timeoutMs) { onAllNodes(androidx.compose.ui.test.hasText(text)).fetchSemanticsNodes().isEmpty() }

private fun scrollIntoView(node: androidx.compose.ui.test.SemanticsNodeInteraction) {
    // Only nodes inside a scrolling page can be scrolled to; the rest are already in view.
    val inScroller = generateSequence(node.fetchSemanticsNode().parent) { it.parent }
        .any { it.config.contains(androidx.compose.ui.semantics.SemanticsActions.ScrollBy) }
    if (inScroller) node.performScrollTo()
}

/** Switches to the bottom bar's tab labelled [label]. */
fun ComposeTestRule.tab(label: String) {
    waitUntil(10_000) { onAllNodes(androidx.compose.ui.test.hasText(label) and androidx.compose.ui.test.isSelectable()).fetchSemanticsNodes().isNotEmpty() }
    onAllNodes(androidx.compose.ui.test.hasText(label) and androidx.compose.ui.test.isSelectable()).onLast().performClick()
    waitForIdle()
}
