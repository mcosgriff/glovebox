package io.cosgriff.glovebox.ui.reminders

import io.cosgriff.glovebox.ui.theme.PlexMono
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.db.OdometerDao
import io.cosgriff.glovebox.data.db.ScheduleDao
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.DecidedBy
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.Interval
import io.cosgriff.glovebox.domain.LastDone
import io.cosgriff.glovebox.domain.LeadTime
import io.cosgriff.glovebox.domain.LeadTimeInput
import io.cosgriff.glovebox.domain.OdometerReading
import io.cosgriff.glovebox.domain.ReminderEngine
import io.cosgriff.glovebox.domain.RulePreview
import io.cosgriff.glovebox.domain.RulePreviews
import io.cosgriff.glovebox.domain.ServiceLog
import io.cosgriff.glovebox.domain.Snoozing
import io.cosgriff.glovebox.ui.service.asMiles
import io.cosgriff.glovebox.ui.service.longDate
import io.cosgriff.glovebox.ui.service.ruleLabel
import io.cosgriff.glovebox.ui.service.toEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/** A real, logged service with both intervals, used as the worked example. */
data class PreviewSource(
    val title: String,
    val interval: Interval,
    val last: LastDone,
    val odometer: Int,
    val milesPerDay: Double?,
)

/** A tracked reminder as the overrides list shows it. [rule] null = uses the default. */
data class RuleItem(val id: Long, val label: String, val rule: DueRule?)

data class ReminderRules(
    val defaultRule: DueRule,
    val lead: LeadTime,
    val preview: PreviewSource?,
    val items: List<RuleItem>,
)

@HiltViewModel
class ReminderRulesViewModel @Inject constructor(
    vehicles: VehicleDao,
    private val schedule: ScheduleDao,
    service: ServiceDao,
    odometer: OdometerDao,
    private val settings: SettingsRepository,
) : ViewModel() {
    /** Null while loading. */
    val rules: StateFlow<ReminderRules?> = combine(
        vehicles.observeActive(),
        schedule.observeAllTracked(),
        service.observeAllRecords(),
        odometer.observeAll(),
        settings.settings,
    ) { active, items, records, readings, s ->
        val several = active.size > 1
        val names = active.associate { it.id to "${it.year} ${it.nickname ?: it.model}" }
        val mine = items.filter { it.vehicleId in names }

        // The oil change if it qualifies, otherwise the first reminder that does.
        val preview = mine.sortedBy { it.templateKey != Snoozing.OIL_TEMPLATE_KEY }.firstNotNullOfOrNull { item ->
            val miles = (if (item.useSevere) item.severeMiles ?: item.intervalMiles else item.intervalMiles) ?: return@firstNotNullOfOrNull null
            val months = (if (item.useSevere) item.severeMonths ?: item.intervalMonths else item.intervalMonths) ?: return@firstNotNullOfOrNull null
            val entries = records.filter { it.record.vehicleId == item.vehicleId }.map { it.toEntry() }
            val last = ServiceLog.lastDone(item.id, entries) ?: return@firstNotNullOfOrNull null
            val all = ServiceLog.readings(
                entries,
                readings.filter { it.vehicleId == item.vehicleId }.map { OdometerReading(it.date, it.odometer) },
            )
            PreviewSource(
                title = if (several) "${item.name} · ${names[item.vehicleId]}" else item.name,
                interval = Interval(miles, months),
                last = last,
                odometer = all.maxOf { it.odometer },
                milesPerDay = ReminderEngine.milesPerDay(all),
            )
        }

        ReminderRules(
            defaultRule = s.defaultRule,
            lead = s.lead,
            preview = preview,
            items = mine.map { item ->
                RuleItem(
                    id = item.id,
                    label = if (several) "${item.name} · ${names[item.vehicleId]}" else item.name,
                    rule = item.ruleOverride?.let { runCatching { DueRule.valueOf(it) }.getOrNull() },
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setDefaultRule(rule: DueRule) = viewModelScope.launch { settings.setDefaultRule(rule) }
    fun setLead(lead: LeadTime) = viewModelScope.launch { settings.setLeadTime(lead) }

    /** Null goes back to the default. */
    fun setOverride(scheduleItemId: Long, rule: DueRule?) = viewModelScope.launch { schedule.setRuleOverride(scheduleItemId, rule?.name) }
}

private val monthYear: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.US)

/** Which interval decides when a service is due, and how early to say so. Design: 7 · Reminder rules. */
@Composable
fun ReminderRulesScreen(onBack: () -> Unit, vm: ReminderRulesViewModel = hiltViewModel()) {
    val rules = vm.rules.collectAsStateWithLifecycle().value ?: return // first frame while loading
    val scheme = MaterialTheme.colorScheme
    // 0 = closed; -1 = choosing which service to override; otherwise that item's rule is being chosen.
    var editing by rememberSaveable { mutableLongStateOf(0L) }

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.padding(start = 8.dp, top = 12.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
            Text("Reminder rules", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
        }

        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                "Most services have a mileage interval and a time interval. Choose which one sets the due date.",
                color = scheme.onSurfaceVariant,
            )

            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DueRule.entries.forEach { rule ->
                    val selected = rule == rules.defaultRule
                    Surface(
                        color = scheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(2.dp, if (selected) scheme.primary else scheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .selectable(selected = selected, role = Role.RadioButton, onClick = { vm.setDefaultRule(rule) }),
                    ) {
                        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            RadioButton(selected = selected, onClick = null)
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(rule.ruleLabel(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(rule.description(), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            PreviewCard(rules.preview, rules.defaultRule)

            LeadTimeFields(rules.lead, onChange = vm::setLead)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("PER-SERVICE OVERRIDES")
                val overridden = rules.items.filter { it.rule != null }
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = scheme.surface),
                    border = BorderStroke(1.dp, scheme.outlineVariant),
                ) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        overridden.forEach { item ->
                            Row(
                                Modifier.fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .clickable(role = Role.Button, onClick = { editing = item.id })
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(item.label, Modifier.weight(1f))
                                Text("${item.rule!!.ruleLabel()} ›", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = scheme.outlineVariant)
                        }
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(if (overridden.isEmpty()) "Every service" else "Everything else", Modifier.weight(1f))
                            Text(
                                "Uses default",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (rules.items.any { it.rule == null }) {
                    TextButton(onClick = { editing = -1 }, modifier = Modifier.heightIn(min = 48.dp)) { Text("+ Add override") }
                }
            }
        }
    }

    when {
        editing == -1L -> PickDialog(
            title = "Override which service?",
            options = rules.items.filter { it.rule == null }.map { it.id to it.label },
            selected = null,
            onPick = { editing = it },
            onDismiss = { editing = 0 },
        )
        editing > 0L -> rules.items.firstOrNull { it.id == editing }?.let { item ->
            PickDialog(
                title = item.label,
                // Key 0 stands for "use the default".
                options = listOf(0L to "Use default (${rules.defaultRule.ruleLabel().lowercase(Locale.US)})") +
                    DueRule.entries.map { (it.ordinal + 1).toLong() to it.ruleLabel() },
                selected = item.rule?.let { (it.ordinal + 1).toLong() } ?: 0L,
                onPick = { key ->
                    vm.setOverride(item.id, DueRule.entries.getOrNull(key.toInt() - 1))
                    editing = 0
                },
                onDismiss = { editing = 0 },
            )
        }
    }
}

private fun DueRule.description(): String = when (this) {
    DueRule.WHICHEVER_FIRST -> "Recommended. Mileage or time, the earlier one — how most schedules are written."
    DueRule.WHICHEVER_LAST -> "Waits until both the mileage and the time interval have passed."
    DueRule.MILEAGE_ONLY -> "Ignore time. Good if you drive a lot."
    DueRule.TIME_ONLY -> "Ignore mileage. Good for a weekend vehicle."
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        Modifier.semantics { heading() },
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PreviewCard(source: PreviewSource?, rule: DueRule) {
    val scheme = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        border = BorderStroke(1.dp, scheme.outlineVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (source == null) {
                Text("PREVIEW", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = scheme.onSurfaceVariant)
                Text(
                    "Once you've logged a service that has both a mileage and a time interval, this shows how the rule works out for it.",
                    style = MaterialTheme.typography.bodySmall,
                )
                return@Column
            }
            val p = RulePreviews.preview(source.interval, source.last, rule, source.odometer, LocalDate.now(), source.milesPerDay)
            Text(
                "PREVIEW · ${source.title.uppercase(Locale.US)}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurfaceVariant,
            )
            Text(
                "Every ${source.interval.miles!!.asMiles()} or ${source.interval.months} months · " +
                    "last done ${source.last.date.format(longDate)} at ${source.last.odometer.asMiles()}",
                style = MaterialTheme.typography.bodySmall,
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LimitBar(
                    label = "Mileage · ${p.dueAtOdometer.asMiles()}",
                    date = when {
                        p.milesFraction >= 1.0 -> "reached"
                        else -> p.mileageDate?.let { "~${it.format(monthYear)}" } ?: "no estimate yet"
                    },
                    fraction = p.milesFraction,
                    deciding = p.decidedBy == DecidedBy.MILEAGE,
                )
                LimitBar(
                    label = "Time · ${source.interval.months} months",
                    date = p.dueByDate.format(longDate),
                    fraction = p.timeFraction,
                    deciding = p.decidedBy == DecidedBy.TIME,
                )
            }
            HorizontalDivider(color = scheme.outlineVariant)
            val (line, why) = p.nextReminder(rule, source.interval.months!!)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Next reminder", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                Text(line, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(why, style = MaterialTheme.typography.bodySmall)
            }
            if (p.canExceedInterval) {
                Surface(color = scheme.primaryContainer, shape = RoundedCornerShape(10.dp)) {
                    Text(
                        "This can push services past the recommended interval.",
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onPrimaryContainer,
                    )
                }
            }
            Text(
                source.milesPerDay?.let {
                    "Dates estimated from your average of ~${RulePreviews.milesPerMonth(it).asMiles()}/month"
                } ?: "Mileage dates need two odometer readings at least a week apart.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

/** The headline and the reason under it. */
private fun RulePreview.nextReminder(rule: DueRule, months: Int): Pair<String, String> {
    val odometer = dueAtOdometer.asMiles()
    val timeDate = dueByDate.format(longDate)
    // "~" marks a date worked out from the driving pace; a limit already reached needs no estimate.
    val mileageDate = mileageDate?.let { if (milesFraction >= 1.0) "Due now" else "~${it.format(longDate)}" }
    val afterLast = "$months months after the last service"
    return when (rule) {
        DueRule.MILEAGE_ONLY -> odometer to (mileageDate?.let { if (it == "Due now") "The mileage interval has passed" else "Est. around ${it.drop(1)}" } ?: "No date estimate yet")
        DueRule.TIME_ONLY -> timeDate to afterLast
        DueRule.WHICHEVER_FIRST -> when (decidedBy) {
            DecidedBy.MILEAGE -> mileageDate!! to "Mileage hits first, at $odometer"
            DecidedBy.TIME -> timeDate to "Time hits first, $afterLast"
            null -> "$odometer or $timeDate" to "Whichever comes first — there's no driving pace yet to say which"
        }
        DueRule.WHICHEVER_LAST -> when (decidedBy) {
            DecidedBy.MILEAGE -> mileageDate!! to "Mileage hits last, at $odometer"
            DecidedBy.TIME -> timeDate to (odometerAtTimeDue?.let { "Time hits last — est. ~${it.asMiles()} by then" } ?: "Time hits last")
            null -> "$odometer and $timeDate" to "Once both have passed — there's no driving pace yet to say which is later"
        }
    }
}

@Composable
private fun LimitBar(label: String, date: String, fraction: Double, deciding: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row {
            Text(
                label,
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (deciding) scheme.primary else scheme.onSurface,
            )
            Text(date, style = MaterialTheme.typography.bodySmall, fontFamily = PlexMono)
        }
        // Decorative: the label and date beside it carry the meaning.
        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(scheme.outlineVariant).clearAndSetSemantics { }) {
            Box(
                Modifier.fillMaxWidth(fraction.toFloat())
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(if (deciding) scheme.primary else scheme.outline),
            )
        }
    }
}

@Composable
private fun LeadTimeFields(lead: LeadTime, onChange: (LeadTime) -> Unit) {
    var miles by rememberSaveable { mutableStateOf(lead.miles.toString()) }
    var days by rememberSaveable { mutableStateOf(lead.days.toString()) }
    // Saved as soon as both boxes hold something valid; otherwise the last good values stay in force.
    val update = { m: String, d: String ->
        miles = m
        days = d
        LeadTimeInput.parse(m, d)?.let(onChange)
    }
    val valid = LeadTimeInput.parse(miles, days) != null
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("REMIND ME EARLY")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = miles,
                onValueChange = { update(it, days) },
                label = { Text("Miles before") },
                singleLine = true,
                isError = LeadTimeInput.parse(miles, "0") == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = days,
                onValueChange = { update(miles, it) },
                label = { Text("Days before") },
                singleLine = true,
                isError = LeadTimeInput.parse("0", days) == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            if (valid) {
                "A reminder shows as due soon this far ahead of its limit."
            } else {
                "Use whole numbers: up to ${LeadTimeInput.MAX_MILES.asMiles()} and up to ${LeadTimeInput.MAX_DAYS} days. Not saved yet."
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (valid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun PickDialog(
    title: String,
    options: List<Pair<Long, String>>,
    selected: Long?,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).selectableGroup()) {
                options.forEach { (key, label) ->
                    Row(
                        Modifier.fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(
                                selected = key == selected,
                                // Without a current choice it is a plain list of things to pick from.
                                role = if (selected != null) Role.RadioButton else Role.Button,
                                onClick = { onPick(key) },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (selected != null) RadioButton(selected = key == selected, onClick = null)
                        Text(label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
