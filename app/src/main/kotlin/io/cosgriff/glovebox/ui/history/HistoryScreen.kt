package io.cosgriff.glovebox.ui.history

import io.cosgriff.glovebox.domain.enumOrNull
import io.cosgriff.glovebox.ui.screenState
import io.cosgriff.glovebox.ui.service.shortName
import io.cosgriff.glovebox.domain.CustomLines
import io.cosgriff.glovebox.domain.CustomCount
import io.cosgriff.glovebox.data.db.RecallDao
import io.cosgriff.glovebox.ui.components.InkOutlinedButton
import io.cosgriff.glovebox.ui.components.InkChip
import io.cosgriff.glovebox.ui.service.text
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.R
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import io.cosgriff.glovebox.ui.components.BackToTop
import io.cosgriff.glovebox.ui.components.BackToTopRoom
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import io.cosgriff.glovebox.domain.HistoryOrder
import io.cosgriff.glovebox.domain.HistoryOrdering
import io.cosgriff.glovebox.domain.HistorySort
import io.cosgriff.glovebox.data.db.jobPrices
import io.cosgriff.glovebox.domain.Spending
import io.cosgriff.glovebox.domain.HistorySearch
import io.cosgriff.glovebox.domain.SearchableRecord
import io.cosgriff.glovebox.ui.service.catalogLabel
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import io.cosgriff.glovebox.domain.SpendingSplit
import io.cosgriff.glovebox.domain.SpendingSummary
import io.cosgriff.glovebox.ui.service.monthYear
import java.time.LocalDate
import io.cosgriff.glovebox.domain.toDash
import io.cosgriff.glovebox.data.log.clusterSwap
import io.cosgriff.glovebox.ui.ArchivedHistoryRoute
import io.cosgriff.glovebox.ui.routeOrNull
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.ui.service.label
import io.cosgriff.glovebox.ui.service.unit
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.RecordMark
import io.cosgriff.glovebox.ui.service.HistoryFocus
import kotlinx.coroutines.launch
import io.cosgriff.glovebox.ui.service.RecordActions
import io.cosgriff.glovebox.domain.LogEdits
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.BorderStroke
import io.cosgriff.glovebox.ui.theme.PlexMono
import io.cosgriff.glovebox.ui.export.ExportSheet
import androidx.compose.ui.Alignment
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.db.ServiceRecordWithItems
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.domain.CatalogCategory
import io.cosgriff.glovebox.domain.CatalogItem
import io.cosgriff.glovebox.domain.jobsIn
import io.cosgriff.glovebox.domain.Money
import io.cosgriff.glovebox.domain.ServiceCatalog
import io.cosgriff.glovebox.domain.categoryOf
import io.cosgriff.glovebox.domain.format
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.service.longDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import androidx.compose.foundation.clickable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.semantics.Role
import io.cosgriff.glovebox.domain.Visits
import io.cosgriff.glovebox.domain.ServiceGroup
import io.cosgriff.glovebox.ui.components.Chevron
import io.cosgriff.glovebox.domain.ItemStatus
import io.cosgriff.glovebox.domain.ReminderProgress
import io.cosgriff.glovebox.ui.garage.GarageOverview
import io.cosgriff.glovebox.data.log.VehicleLogs

data class HistoryState(
    val vehicles: List<VehicleEntity>,
    /** The vehicle whose history is showing; null when the garage is empty. */
    val vehicle: VehicleEntity?,
    /** Newest first. */
    val records: List<ServiceRecordWithItems>,
    /** Lines under Custom that sorting could file (`CustomLines`); none for an archived vehicle, which is read only. */
    val customLines: CustomCount = CustomCount(0, 0),
    /**
     * The vehicle's next reminder for the timeline: `ComingUp.next`, from the same statuses as the Reminders
     * tab. Null when nothing is counting down, and for a sold vehicle, which has no reminders.
     */
    val next: Pair<ItemStatus, ReminderProgress>? = null,
    /** The vehicle's odometer now, in its own unit (`VehicleLog.odometer`); null before any reading. */
    val odometer: Int? = null,
    /** Each of the vehicle's schedule items' group (`VehicleLog.itemGroups`), for the timeline's record summaries. */
    val itemGroups: Map<Long, ServiceGroup> = emptyMap(),
) {
    /** Categories that actually appear in this vehicle's records, in catalog order. */
    val categories: List<CatalogCategory>
        get() {
            val used = records.flatMap { it.items }.mapNotNull { ServiceCatalog.categoryOf(it.catalogId)?.id }.toSet()
            return ServiceCatalog.categories.filter { it.id in used }
        }

    /** The jobs of [categoryId] that appear in this vehicle's records, in catalog order. */
    fun jobsIn(categoryId: String): List<CatalogItem> =
        ServiceCatalog.jobsIn(categoryId, records.flatMap { it.items }.mapNotNull { it.catalogId }.toSet())
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    vehicles: VehicleDao,
    recalls: RecallDao,
    private val actions: RecordActions,
    overview: GarageOverview,
    logs: VehicleLogs,
) : ViewModel() {
    /** Set when opened from Archived vehicles: that one vehicle's history, to read only. */
    private val archivedId: Long? = savedStateHandle.routeOrNull<ArchivedHistoryRoute>()?.vehicleId
    val archived: Boolean get() = archivedId != null

    // Both choices are kept in the saved state, so the tab comes back as it was left even if Android
    // had to restart the app in between.
    private val chosenVehicle: StateFlow<Long?> = savedStateHandle.getStateFlow(CHOSEN_VEHICLE, null)

    /** Category id to show, or null for all. */
    var filter by mutableStateOf(savedStateHandle.get<String>(FILTER))
        private set

    fun chooseFilter(categoryId: String?) {
        filter = categoryId
        savedStateHandle[FILTER] = categoryId
        chooseJob(null)
    }

    /** Catalog id of the one job to show within [filter], or null for the whole category. */
    var job by mutableStateOf(savedStateHandle.get<String>(JOB))
        private set

    fun chooseJob(catalogId: String?) {
        job = catalogId
        savedStateHandle[JOB] = catalogId
    }

    /** What is typed in the search field, as typed. Kept with the vehicle and category through a restart. */
    var search by mutableStateOf(savedStateHandle.get<String>(SEARCH).orEmpty())
        private set

    fun search(text: String) {
        search = text
        savedStateHandle[SEARCH] = text
    }

    /** Kept across vehicles: it is how the owner likes to read a history, not a choice about one vehicle. */
    var order by mutableStateOf(
        HistoryOrder(
            enumOrNull<HistorySort>(savedStateHandle.get<String>(SORT_BY)) ?: HistorySort.DATE,
            savedStateHandle.get<Boolean>(DESCENDING) ?: true,
        ),
    )
        private set

    fun chooseOrder(chosen: HistoryOrder) {
        order = chosen
        savedStateHandle[SORT_BY] = chosen.by.name
        savedStateHandle[DESCENDING] = chosen.descending
    }

    /** Timeline (the first choice) or list; kept like the order, across vehicles and through a restart. */
    var view by mutableStateOf(enumOrNull<HistoryView>(savedStateHandle.get<String>(VIEW)) ?: HistoryView.TIMELINE)
        private set

    fun chooseView(chosen: HistoryView) {
        view = chosen
        savedStateHandle[VIEW] = chosen.name
    }

    private val source: Flow<HistoryState> =
        if (archivedId != null) {
            logs.observe(archivedId).map { log ->
                HistoryState(listOfNotNull(log?.vehicle), log?.vehicle, log?.records.orEmpty(), odometer = log?.odometer, itemGroups = log?.itemGroups.orEmpty())
            }
        } else {
            combine(vehicles.observeActive(), chosenVehicle) { list, chosen ->
                list to (list.firstOrNull { it.id == chosen } ?: list.firstOrNull())
            }.flatMapLatest { (list, vehicle) ->
                if (vehicle == null) {
                    flowOf(HistoryState(list, null, emptyList()))
                } else {
                    combine(logs.observe(vehicle.id), recalls.observe(vehicle.id), overview.garage) { log, recallRows, garage ->
                        val records = log?.records.orEmpty()
                        val shown = garage.vehicles.firstOrNull { it.vehicle.id == vehicle.id }
                        HistoryState(
                            list, vehicle, records, CustomLines.count(records.savedLines(recallRows.recordIds())),
                            next = shown?.comingUp?.next,
                            odometer = shown?.odometer,
                            itemGroups = log?.itemGroups.orEmpty(),
                        )
                    }
                }
            }
        }

    val state: StateFlow<HistoryState?> = source.screenState(viewModelScope, null)

    /** The record to mark as just added or just restored, if it is in the list showing. */
    val fresh: StateFlow<HistoryFocus?> = actions.focus.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        // A save, delete or restore lands here: show that vehicle, with every category, so the record is in view.
        if (archivedId == null) {
            viewModelScope.launch {
                actions.focus.collect { focus ->
                    if (focus != null) {
                        chooseVehicle(focus.vehicleId)
                        search("")
                    }
                }
            }
        }
    }

    fun chooseVehicle(id: Long) {
        savedStateHandle[CHOSEN_VEHICLE] = id
        chooseFilter(null)
    }

    private companion object {
        const val CHOSEN_VEHICLE = "chosenVehicle"
        const val FILTER = "filter"
        const val JOB = "job"
        const val SEARCH = "search"
        const val SORT_BY = "sortBy"
        const val DESCENDING = "descending"
        const val VIEW = "view"
    }

    /** History is being left: the mark has been seen, and the next visit starts wherever the user left it. */
    fun focusSeen() = actions.clearFocus()
}

@Composable
fun HistoryScreen(
    onOpenRecord: (recordId: Long) -> Unit,
    /** Opens the sort screen for a vehicle's Custom lines; absent where History is read only. */
    onSortCustom: ((vehicleId: Long) -> Unit)? = null,
    /** Shown as a back arrow; the History tab itself has none. */
    onBack: (() -> Unit)? = null,
    /** Log service with that job ticked, from the timeline's next reminder. */
    onLogIt: ((vehicleId: Long, scheduleItemId: Long) -> Unit)? = null,
    vm: HistoryViewModel = hiltViewModel(),
) {
    val state = vm.state.collectAsStateWithLifecycle().value ?: return // first frame while loading
    val fresh by vm.fresh.collectAsStateWithLifecycle()
    DisposableEffect(Unit) { onDispose { vm.focusSeen() } }

    var exporting by rememberSaveable { mutableStateOf(false) }
    if (exporting) ExportSheet(vehicleId = state.vehicle?.id, onDismiss = { exporting = false })

    HistoryContent(
        state, vm.filter, vm.order, fresh, vm.archived, remember { LocalDate.now() }, onOpenRecord, onBack,
        onExport = { exporting = true }, onChooseVehicle = vm::chooseVehicle, onChooseFilter = vm::chooseFilter,
        onChooseOrder = vm::chooseOrder, chosenJob = vm.job, onChooseJob = vm::chooseJob, search = vm.search, onSearch = vm::search,
        onSortCustom = onSortCustom?.let { open -> state.vehicle?.let { v -> { open(v.id) } } },
        view = vm.view, onChooseView = vm::chooseView, onLogIt = onLogIt,
    )
}

/** History as given, with no ViewModel: what [HistoryScreen] shows, and what the screenshot tests draw. */
@Composable
internal fun HistoryContent(
    state: HistoryState,
    /** The category chosen, or null for all. */
    chosenFilter: String?,
    order: HistoryOrder,
    fresh: HistoryFocus?,
    archived: Boolean,
    today: LocalDate,
    onOpenRecord: (recordId: Long) -> Unit,
    onBack: (() -> Unit)?,
    onExport: () -> Unit,
    onChooseVehicle: (Long) -> Unit,
    onChooseFilter: (String?) -> Unit,
    onChooseOrder: (HistoryOrder) -> Unit,
    /** The one job chosen within the category, as a catalog id, or null for the whole category. */
    chosenJob: String? = null,
    onChooseJob: (String?) -> Unit = {},
    /** What is typed in the search field. */
    search: String = "",
    onSearch: (String) -> Unit = {},
    onSortCustom: (() -> Unit)? = null,
    view: HistoryView = HistoryView.TIMELINE,
    onChooseView: (HistoryView) -> Unit = {},
    onLogIt: ((vehicleId: Long, scheduleItemId: Long) -> Unit)? = null,
    /** Where each view is scrolled to; given only by tests that start part way down. */
    listState: LazyListState = rememberLazyListState(),
    timelineState: LazyListState = rememberLazyListState(),
) {
    val categories = state.categories
    // A filter can outlive the last record that matched it (e.g. after a delete).
    val filter = chosenFilter?.takeIf { f -> categories.any { it.id == f } }
    // A category with one job in it has nothing to narrow, so its row of jobs only shows from two.
    val jobs = filter?.let(state::jobsIn).orEmpty().takeIf { it.size > 1 }.orEmpty()
    val job = chosenJob?.takeIf { j -> jobs.any { it.id == j } }
    val swap = state.vehicle?.clusterSwap()
    val sorted = HistoryOrdering.sorted(
        state.records,
        order,
        date = { it.record.date },
        dash = { swap.toDash(it.record.odometer, it.record.date) },
        id = { it.record.id },
    )
    fun matches(r: ServiceRecordWithItems) =
        (filter == null || r.items.any { if (job != null) it.catalogId == job else ServiceCatalog.categoryOf(it.catalogId)?.id == filter }) &&
            HistorySearch.matches(r.searchable(), search)
    // A visit (#307) is one card where its first record sorts, and shows whole when any of its records matches.
    val groups = Visits.grouped(sorted) { it.record.visitId }.filter { g -> g.any(::matches) }
    val shown = groups.flatten()
    // A record's cost covers all of its lines, so it can't be split by category: no line while a filter is on.
    val searching = HistorySearch.query(search) != null
    // The switch only shows with records to lay out; the choice is kept for when there are some.
    val timeline = view == HistoryView.TIMELINE && state.records.isNotEmpty()
    // The spending line is the list's; the timeline shows time, not money.
    val spending = if (filter == null && !searching && !timeline) Spending.summary(state.records, today, { it.record.date }, { it.record.costCents }) else null
    // Labor and parts under it (#485), over the same records, and only while it shows.
    val split = spending?.let { Spending.split(state.records) { r -> r.jobPrices } }

    // A new order starts at the top. Without this the list would hold on to the record that was first and
    // follow it to wherever it now sorts; coming back from a record keeps the place, since that is a new composition.
    var lastOrder by remember { mutableStateOf(order) }
    LaunchedEffect(order) {
        if (order != lastOrder) {
            lastOrder = order
            listState.scrollToItem(0)
        }
    }

    // The header and the filters are the first items of the list showing, so they scroll away with it
    // and the records get the screen; the Top button brings them back.
    val header: LazyListScope.() -> Unit = {
        if (onBack != null) {
            item(key = "back") {
                Box(Modifier.fillMaxWidth()) {
                    IconButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp, top = 12.dp)) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            }
        }
        item(key = "header") {
            Column(
                Modifier.padding(start = 20.dp, top = if (onBack != null) 0.dp else 16.dp, end = 20.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.history_title), Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
                    // The title gives way first: the toggle and Export keep their 48dp.
                    if (state.vehicle != null && state.records.isNotEmpty()) HistoryViewToggle(view, onChooseView)
                    if (state.vehicle != null) {
                        InkOutlinedButton(onClick = onExport, modifier = Modifier.heightIn(min = 48.dp), quiet = true) { Text(stringResource(R.string.history_export)) }
                    }
                }
                state.vehicle?.let { v ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(if (archived) R.string.history_subtitle_archived else R.string.history_subtitle, v.shortName(), pluralStringResource(R.plurals.records, shown.size, shown.size.asNumber())),
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        // The timeline always runs newest first, so there is nothing to sort.
                        if (state.records.size > 1 && !timeline) SortControls(order, onChooseOrder)
                    }
                    state.odometer?.let { miles ->
                        val reading = miles.asDistance(v.unit)
                        val spoken = stringResource(R.string.history_odometer_description, reading)
                        Text(
                            reading,
                            Modifier.semantics { contentDescription = spoken },
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = PlexMono,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                spending?.let { SpendingLine(it) }
                split?.let { SpendingSplitLine(it) }
                val custom = state.customLines
                if (!archived && custom.lines > 0 && onSortCustom != null) {
                    TextButton(onClick = onSortCustom, modifier = Modifier.heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 0.dp)) {
                        // The sort page has a row per wording, so a count of lines alone reads as if the two disagree.
                        Text(
                            if (custom.wordings == custom.lines) pluralStringResource(R.plurals.history_custom_lines, custom.lines, custom.lines.asNumber())
                            else pluralStringResource(R.plurals.history_custom_ways, custom.wordings, custom.lines.asNumber(), custom.wordings.asNumber()),
                        )
                    }
                }
            }
        }
        if (state.vehicles.size > 1) {
            item(key = "vehicles") {
                ChipRow {
                    state.vehicles.forEach { v ->
                        InkChip(v.shortName(), selected = v.id == state.vehicle?.id) { onChooseVehicle(v.id) }
                    }
                }
            }
        }
        if (jobs.isNotEmpty()) {
            // Inside a category with two jobs or more, its jobs take the categories' place: the category itself for
            // all of it, a job to show it alone (a second tap goes back to the whole category), and back to All.
            // A key of its own, so the row starts at its beginning rather than where the categories were scrolled.
            item(key = "jobs") {
                ChipRow {
                    IconButton(onClick = { onChooseFilter(null) }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.history_filter_back))
                    }
                    categories.firstOrNull { it.id == filter }?.let { c ->
                        InkChip(c.label, selected = job == null) { onChooseJob(null) }
                    }
                    jobs.forEach { j ->
                        InkChip(j.label, selected = job == j.id) { onChooseJob(if (job == j.id) null else j.id) }
                    }
                }
            }
        } else if (categories.isNotEmpty()) {
            item(key = "categories") {
                ChipRow {
                    InkChip(stringResource(R.string.history_all), selected = filter == null) { onChooseFilter(null) }
                    categories.forEach { c ->
                        InkChip(c.label, selected = filter == c.id) { onChooseFilter(c.id) }
                    }
                }
            }
        }
        // A focused field is kept composed by the list while it is scrolled off, so typing goes on.
        if (state.records.isNotEmpty()) item(key = "search") { SearchField(search, onSearch) }
    }

    Box(Modifier.fillMaxSize()) {
        val showing: LazyListState
        when {
            timeline && state.vehicle != null && !(shown.isEmpty() && searching) -> {
                showing = timelineState
                // Always by date, newest first, whatever the list's order: a visit is one entry, where its newest record is.
                val byDate = HistoryOrdering.sorted(
                    state.records, HistoryOrder(HistorySort.DATE, descending = true),
                    date = { it.record.date }, dash = { swap.toDash(it.record.odometer, it.record.date) }, id = { it.record.id },
                )
                HistoryTimelineList(
                    vehicle = state.vehicle,
                    groups = Visits.grouped(byDate) { it.record.visitId }.filter { g -> g.any(::matches) },
                    itemGroups = state.itemGroups,
                    // Like the spending line, the next reminder is about the whole vehicle, so it goes while a filter or search is on.
                    next = state.next.takeIf { filter == null && !searching },
                    since = state.records.minOfOrNull { it.record.date },
                    fresh = fresh,
                    listState = timelineState,
                    onOpenRecord = onOpenRecord,
                    onLogIt = onLogIt.takeIf { !archived },
                    header = header,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            else -> {
                showing = listState
                LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 16.dp + BackToTopRoom)) {
                    header()
                    when {
                        state.vehicle == null -> item(key = "empty") { EmptyNote(stringResource(R.string.history_no_vehicle)) }
                        state.records.isEmpty() && archived -> item(key = "empty") { EmptyNote(stringResource(R.string.history_none_archived)) }
                        state.records.isEmpty() -> item(key = "empty") { EmptyNote(stringResource(R.string.history_none)) }
                        shown.isEmpty() && searching -> item(key = "no-match") {
                            Column {
                                EmptyNote(stringResource(R.string.history_no_match, search.trim()))
                                TextButton(onClick = { onSearch("") }, modifier = Modifier.padding(horizontal = 8.dp).heightIn(min = 48.dp)) {
                                    Text(stringResource(R.string.history_search_show_all))
                                }
                            }
                        }
                        else -> itemsIndexed(groups, key = { _, g -> g.first().record.id }) { i, g ->
                            Box(Modifier.padding(start = 20.dp, top = if (i == 0) 8.dp else 10.dp, end = 20.dp)) {
                                if (g.size == 1) {
                                    val r = g.single()
                                    RecordCard(r, state.vehicle.unit, mark = fresh?.mark?.takeIf { r.record.id == fresh.recordId }, onClick = { onOpenRecord(r.record.id) })
                                } else {
                                    VisitCard(g, state.vehicle.unit, fresh, onOpenRecord)
                                }
                            }
                        }
                    }
                }
            }
        }
        BackToTop(showing, Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp))
    }
}

@Composable
private fun SearchField(search: String, onSearch: (String) -> Unit) {
    val focus = LocalFocusManager.current
    OutlinedTextField(
        value = search,
        onValueChange = onSearch,
        placeholder = { Text(stringResource(R.string.history_search)) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            if (search.isNotEmpty()) {
                IconButton(onClick = { onSearch("") }) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.history_search_clear))
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            // Filled and borderless like the record cards; the outline only while typing.
            unfocusedBorderColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 8.dp, end = 20.dp),
    )
}

/** What the search reads of a record: each line as typed and, for a catalog line, its name as shown now. */
private fun ServiceRecordWithItems.searchable() = SearchableRecord(
    labels = items.flatMap { listOfNotNull(it.label, it.catalogId?.let(::catalogLabel)) },
    notes = record.notes,
    shop = record.shopName,
)

/** "$1,240.00 this year · $6,830.00 since Mar 2021", and how many records it counted when some have no cost. */
@Composable
private fun SpendingLine(s: SpendingSummary) {
    val since = s.since.format(monthYear)
    val line = s.thisYearCents?.let { stringResource(R.string.history_spending_year, Money.format(it), Money.format(s.totalCents), since) }
        ?: stringResource(R.string.history_spending_since, Money.format(s.totalCents), since)
    Text(
        if (s.counted == s.records) line
        else stringResource(R.string.history_spending_partial, line, pluralStringResource(R.plurals.history_spending_counted, s.records, s.counted.asNumber(), s.records.asNumber())),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * "Line prices on 3 of 12 records: labor $420.00, parts $610.00" (#485). Worded as its own figures, not a share of
 * the total above; labor or parts no record entered is left out rather than shown as $0.00.
 */
@Composable
private fun SpendingSplitLine(s: SpendingSplit) {
    val priced = s.priced.asNumber()
    val records = s.records.asNumber()
    val labor = s.laborCents
    val parts = s.partsCents
    val text = when {
        labor != null && parts != null -> pluralStringResource(R.plurals.history_spending_split, s.records, priced, records, Money.format(labor), Money.format(parts))
        labor != null -> pluralStringResource(R.plurals.history_spending_split_labor, s.records, priced, records, Money.format(labor))
        parts != null -> pluralStringResource(R.plurals.history_spending_split_parts, s.records, priced, records, Money.format(parts))
        else -> return
    }
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun RecordCard(r: ServiceRecordWithItems, unit: DistanceUnit, mark: RecordMark?, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val fresh = mark != null
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        // Canvas: the record just added, or just restored, is outlined in the accent.
        border = BorderStroke(if (fresh) 1.5.dp else 1.dp, if (fresh) scheme.primary else scheme.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(r.record.date.format(longDate), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(r.record.odometer.asDistance(unit), style = MaterialTheme.typography.bodyMedium, fontFamily = PlexMono, fontWeight = FontWeight.SemiBold)
            }
            Text(r.items.joinToString(", ") { it.label })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    LogEdits.who(r.record.diy, r.record.shopName, mark, r.record.enteredAsLastDone, r.record.imported).text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                r.record.costCents?.let {
                    Text(Money.format(it), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * Records linked as one visit (#307), as one card: the date and, when its records agree, the reading; then
 * each record with its own work, tag and cost, opening that record; then the visit's total. A record whose
 * date or reading differs from the first's says its own, as entered: a disagreement is shown, never settled.
 */
@Composable
private fun VisitCard(records: List<ServiceRecordWithItems>, unit: DistanceUnit, fresh: HistoryFocus?, onOpenRecord: (Long) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val first = records.first().record
    val readings = Visits.readings(records.map { it.record.odometer })
    val oneDay = records.all { it.record.date == first.date }
    val marked = records.any { it.record.id == fresh?.recordId }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        border = BorderStroke(if (marked) 1.5.dp else 1.dp, if (marked) scheme.primary else scheme.outlineVariant),
    ) {
        Column(Modifier.padding(start = 16.dp, top = 14.dp, end = 8.dp, bottom = 6.dp)) {
            Row(Modifier.fillMaxWidth().padding(end = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(first.date.format(longDate), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (readings.size == 1) {
                    Text(readings.single().asDistance(unit), style = MaterialTheme.typography.bodyMedium, fontFamily = PlexMono, fontWeight = FontWeight.SemiBold)
                }
            }
            Text(
                pluralStringResource(R.plurals.history_visit, records.size, records.size.asNumber()),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
            )
            records.forEach { r ->
                HorizontalDivider(Modifier.padding(top = 8.dp, end = 8.dp), color = scheme.outlineVariant)
                val own = listOfNotNull(
                    r.record.date.takeIf { !oneDay }?.format(longDate),
                    r.record.odometer.takeIf { readings.size > 1 }?.asDistance(unit),
                    LogEdits.who(r.record.diy, r.record.shopName, fresh?.mark?.takeIf { r.record.id == fresh.recordId }, r.record.enteredAsLastDone, r.record.imported).text,
                ).joinToString(" · ")
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button) { onOpenRecord(r.record.id) }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(r.items.joinToString(", ") { it.label })
                        Text(own, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    }
                    r.record.costCents?.let { Text(Money.format(it), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold) }
                    Chevron()
                }
            }
            // Only a sum says anything: with one cost, the total would repeat it.
            Visits.total(records.map { it.record.costCents })?.takeIf { records.count { it.record.costCents != null } > 1 }?.let { total ->
                HorizontalDivider(Modifier.padding(end = 8.dp), color = scheme.outlineVariant)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp, end = 34.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.history_visit_total), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    Text(Money.format(total), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Material's sort icon opens what to sort by; the arrow beside it turns the order round (down is newest or highest first). */
@Composable
private fun SortControls(order: HistoryOrder, onChoose: (HistoryOrder) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = stringResource(R.string.history_sort_by), tint = scheme.onSurfaceVariant)
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = MaterialTheme.shapes.small,
            containerColor = scheme.surface,
            border = BorderStroke(1.dp, scheme.outlineVariant),
        ) {
            Text(
                stringResource(R.string.history_sort_by),
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
            )
            HistorySort.entries.forEach { by ->
                val chosen = by == order.by
                DropdownMenuItem(
                    text = { Text(stringResource(by.label), fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal) },
                    onClick = {
                        open = false
                        if (!chosen) onChoose(order.copy(by = by))
                    },
                    // The space stays when it isn't chosen, so the two names line up.
                    leadingIcon = { Icon(Icons.Outlined.Check, contentDescription = null, tint = if (chosen) scheme.primary else Color.Transparent) },
                    modifier = Modifier.semantics { selected = chosen },
                )
            }
        }
    }
    val now = stringResource(order.label)
    IconButton(
        onClick = { onChoose(order.copy(descending = !order.descending)) },
        modifier = Modifier.semantics { stateDescription = now },
    ) {
        Icon(
            if (order.descending) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
            contentDescription = stringResource(R.string.history_reverse),
            tint = scheme.onSurfaceVariant,
        )
    }
}

private val HistorySort.label: Int
    @StringRes get() = when (this) {
        HistorySort.DATE -> R.string.history_sort_date
        HistorySort.MILEAGE -> R.string.history_sort_mileage
    }

/** The order as a reader would say it, which depends on what is sorted. */
private val HistoryOrder.label: Int
    @StringRes get() = when (by) {
        HistorySort.DATE -> if (descending) R.string.history_order_newest else R.string.history_order_oldest
        HistorySort.MILEAGE -> if (descending) R.string.history_order_highest else R.string.history_order_lowest
    }

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

@Composable
private fun EmptyNote(text: String) {
    Text(
        text,
        Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

