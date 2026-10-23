package io.cosgriff.glovebox.ui.reminders

import io.cosgriff.glovebox.ui.service.shortName
import io.cosgriff.glovebox.data.db.rule
import io.cosgriff.glovebox.ui.components.ChoiceDialog
import io.cosgriff.glovebox.ui.components.SectionLabel
import io.cosgriff.glovebox.ui.service.typedNumber
import io.cosgriff.glovebox.ui.service.ruleInSentence
import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.ui.service.titleWord
import io.cosgriff.glovebox.ui.service.monthYear
import io.cosgriff.glovebox.ui.service.unit
import io.cosgriff.glovebox.domain.DistanceUnit
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
import io.cosgriff.glovebox.domain.asReadOn
import io.cosgriff.glovebox.domain.ReminderEngine
import io.cosgriff.glovebox.domain.RulePreview
import io.cosgriff.glovebox.domain.RulePreviews
import io.cosgriff.glovebox.domain.ServiceLog
import io.cosgriff.glovebox.domain.Snoozing
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.service.longDate
import io.cosgriff.glovebox.ui.service.ruleLabel
import io.cosgriff.glovebox.ui.service.clusterSwap
import io.cosgriff.glovebox.ui.service.toEntry
import io.cosgriff.glovebox.ui.service.toReadings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import io.cosgriff.glovebox.ui.service.answering
import javax.inject.Inject

/** A real, logged service with both intervals, used as the worked example. */
data class PreviewSource(
    val title: String,
    val interval: Interval,
    val last: LastDone,
    /** [last] as the odometer read that day; differs only for a service before a cluster replacement. */
    val lastRead: LastDone,
    val odometer: Int,
    val milesPerDay: Double?,
    /** What that vehicle's odometer counts in. */
    val unit: DistanceUnit,
)

/** A tracked reminder as the overrides list shows it. [rule] null = uses the default. */
data class RuleItem(val id: Long, val label: String, val rule: DueRule?)

data class ReminderRules(
    val defaultRule: DueRule,
    val lead: LeadTime,
    /** The unit the lead distance is typed in: the one new vehicles get. */
    val unit: DistanceUnit,
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
        val names = active.associate { it.id to it.shortName() }
        val mine = items.filter { it.vehicleId in names }

        // The oil change if it qualifies, otherwise the first reminder that does.
        val preview = mine.sortedBy { it.templateKey != Snoozing.OIL_TEMPLATE_KEY }.firstNotNullOfOrNull { item ->
            val miles = (if (item.useSevere) item.severeMiles ?: item.intervalMiles else item.intervalMiles) ?: return@firstNotNullOfOrNull null
            val months = (if (item.useSevere) item.severeMonths ?: item.intervalMonths else item.intervalMonths) ?: return@firstNotNullOfOrNull null
            val swap = active.firstOrNull { it.id == item.vehicleId }?.clusterSwap()
            val entries = records.filter { it.record.vehicleId == item.vehicleId }.map { it.toEntry(swap, mine.filter { m -> m.vehicleId == item.vehicleId }.answering()) }
            val last = ServiceLog.lastDone(item.id, entries) ?: return@firstNotNullOfOrNull null
            val all = ServiceLog.readings(
                entries,
                readings.filter { it.vehicleId == item.vehicleId }.toReadings(swap),
            )
            PreviewSource(
                title = if (several) "${item.name} · ${names[item.vehicleId]}" else item.name,
                interval = Interval(miles, months),
                last = last,
                lastRead = swap.asReadOn(last),
                odometer = all.maxOf { it.odometer },
                milesPerDay = ReminderEngine.milesPerDay(all),
                unit = active.firstOrNull { it.id == item.vehicleId }?.unit ?: s.distanceUnit,
            )
        }

        ReminderRules(
            defaultRule = s.defaultRule,
            lead = s.lead,
            unit = s.distanceUnit,
            preview = preview,
            items = mine.map { item ->
                RuleItem(
                    id = item.id,
                    label = if (several) "${item.name} · ${names[item.vehicleId]}" else item.name,
                    rule = item.rule,
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setDefaultRule(rule: DueRule) = viewModelScope.launch { settings.setDefaultRule(rule) }
    fun setLead(lead: LeadTime) = viewModelScope.launch { settings.setLeadTime(lead) }

    /** Null goes back to the default. */
    fun setOverride(scheduleItemId: Long, rule: DueRule?) = viewModelScope.launch { schedule.setRuleOverride(scheduleItemId, rule?.name) }
}


/** Which interval decides when a service is due, and how early to say so. Design: 7 · Reminder rules. */
@Composable
fun ReminderRulesScreen(onBack: () -> Unit, vm: ReminderRulesViewModel = hiltViewModel()) {
    val rules = vm.rules.collectAsStateWithLifecycle().value ?: return // first frame while loading
    val scheme = MaterialTheme.colorScheme
    // 0 = closed; -1 = choosing which service to override; otherwise that item's rule is being chosen.
    var editing by rememberSaveable { mutableLongStateOf(0L) }

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.padding(start = 8.dp, top = 12.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back)) }
            Text(stringResource(R.string.reminder_rules_title), Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
        }

        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                stringResource(R.string.rules_intro),
                color = scheme.onSurfaceVariant,
            )

            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DueRule.entries.forEach { rule ->
                    val selected = rule == rules.defaultRule
                    Surface(
                        color = scheme.surface,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(2.dp, if (selected) scheme.primary else scheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
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

            LeadTimeFields(rules.lead, rules.unit, onChange = vm::setLead)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(stringResource(R.string.rules_overrides), bold = true)
                val overridden = rules.items.filter { it.rule != null }
                Card(
                    shape = MaterialTheme.shapes.medium,
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
                                Text(stringResource(R.string.rules_override_value, item.rule!!.ruleLabel()), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = scheme.outlineVariant)
                        }
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(stringResource(if (overridden.isEmpty()) R.string.rules_every_service else R.string.rules_everything_else), Modifier.weight(1f))
                            Text(
                                stringResource(R.string.rules_uses_default),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (rules.items.any { it.rule == null }) {
                    TextButton(onClick = { editing = -1 }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.rules_add_override)) }
                }
            }
        }
    }

    when {
        editing == -1L -> {
            val labels = rules.items.filter { it.rule == null }.associate { it.id to it.label }
            ChoiceDialog(
                title = stringResource(R.string.rules_override_which),
                options = labels.keys.toList(),
                selected = null,
                label = { labels.getValue(it) },
                onPick = { editing = it },
                onDismiss = { editing = 0 },
            )
        }
        editing > 0L -> rules.items.firstOrNull { it.id == editing }?.let { item ->
            // Key 0 stands for "use the default".
            val labels = mapOf(0L to stringResource(R.string.rules_use_default, rules.defaultRule.ruleInSentence())) +
                DueRule.entries.associate { (it.ordinal + 1).toLong() to it.ruleLabel() }
            ChoiceDialog(
                title = item.label,
                options = labels.keys.toList(),
                selected = item.rule?.let { (it.ordinal + 1).toLong() } ?: 0L,
                label = { labels.getValue(it) },
                onPick = { key -> vm.setOverride(item.id, DueRule.entries.getOrNull(key.toInt() - 1)) },
                onDismiss = { editing = 0 },
            )
        }
    }
}

@Composable
private fun DueRule.description(): String = stringResource(
    when (this) {
        DueRule.WHICHEVER_FIRST -> R.string.rules_first_about
        DueRule.WHICHEVER_LAST -> R.string.rules_last_about
        DueRule.MILEAGE_ONLY -> R.string.rules_mileage_about
        DueRule.TIME_ONLY -> R.string.rules_time_about
    },
)

@Composable
private fun PreviewCard(source: PreviewSource?, rule: DueRule) {
    val scheme = MaterialTheme.colorScheme
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        border = BorderStroke(1.dp, scheme.outlineVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (source == null) {
                Text(stringResource(R.string.rules_preview), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = scheme.onSurfaceVariant)
                Text(
                    stringResource(R.string.rules_preview_none),
                    style = MaterialTheme.typography.bodySmall,
                )
                return@Column
            }
            val unit = source.unit
            val p = RulePreviews.preview(source.interval, source.last, rule, source.odometer, LocalDate.now(), source.milesPerDay)
            Text(
                stringResource(R.string.rules_preview_of, source.title.uppercase(AppText.locale)),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurfaceVariant,
            )
            Text(
                stringResource(
                    R.string.rules_preview_source,
                    source.interval.miles!!.asDistance(unit),
                    pluralStringResource(R.plurals.months, source.interval.months!!, source.interval.months!!.asNumber()),
                    source.last.date.format(longDate),
                    source.lastRead.odometer.asDistance(unit),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LimitBar(
                    label = stringResource(R.string.rules_bar_mileage, p.dueAtOdometer.asDistance(unit)),
                    date = when {
                        p.milesFraction >= 1.0 -> stringResource(R.string.rules_reached)
                        else -> p.mileageDate?.let { stringResource(R.string.snooze_about, it.format(monthYear)) } ?: stringResource(R.string.rules_no_estimate)
                    },
                    fraction = p.milesFraction,
                    deciding = p.decidedBy == DecidedBy.MILEAGE,
                )
                LimitBar(
                    label = stringResource(R.string.rules_bar_time, pluralStringResource(R.plurals.months, source.interval.months!!, source.interval.months!!.asNumber())),
                    date = p.dueByDate.format(longDate),
                    fraction = p.timeFraction,
                    deciding = p.decidedBy == DecidedBy.TIME,
                )
            }
            HorizontalDivider(color = scheme.outlineVariant)
            val (line, why) = p.nextReminder(rule, source.interval.months!!, unit)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.rules_next_reminder), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                Text(line, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(why, style = MaterialTheme.typography.bodySmall)
            }
            if (p.canExceedInterval) {
                Surface(color = scheme.primaryContainer, shape = RoundedCornerShape(10.dp)) {
                    Text(
                        stringResource(R.string.rules_can_exceed),
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onPrimaryContainer,
                    )
                }
            }
            Text(
                source.milesPerDay?.let {
                    stringResource(R.string.rules_pace, RulePreviews.milesPerMonth(it).asDistance(unit))
                } ?: stringResource(R.string.rules_no_pace),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

/** The headline and the reason under it. */
@Composable
private fun RulePreview.nextReminder(rule: DueRule, months: Int, unit: DistanceUnit): Pair<String, String> {
    val odometer = dueAtOdometer.asDistance(unit)
    val timeDate = dueByDate.format(longDate)
    val reached = milesFraction >= 1.0
    val estimated = mileageDate?.format(longDate)
    // "~" marks a date worked out from the driving pace; a limit already reached needs no estimate.
    val mileageDate = estimated?.let { if (reached) stringResource(R.string.due_now) else stringResource(R.string.snooze_about, it) }
    val afterLast = pluralStringResource(R.plurals.rules_after_last, months, months.asNumber())
    return when (rule) {
        DueRule.MILEAGE_ONLY -> odometer to when {
            estimated == null -> stringResource(R.string.rules_no_date)
            reached -> stringResource(R.string.rules_mileage_passed)
            else -> stringResource(R.string.rules_est_around, estimated)
        }
        DueRule.TIME_ONLY -> timeDate to afterLast
        DueRule.WHICHEVER_FIRST -> when (decidedBy) {
            DecidedBy.MILEAGE -> mileageDate!! to stringResource(R.string.rules_mileage_first, odometer)
            DecidedBy.TIME -> timeDate to stringResource(R.string.rules_time_first, afterLast)
            null -> stringResource(R.string.either_or, odometer, timeDate) to stringResource(R.string.rules_first_no_pace)
        }
        DueRule.WHICHEVER_LAST -> when (decidedBy) {
            DecidedBy.MILEAGE -> mileageDate!! to stringResource(R.string.rules_mileage_last, odometer)
            DecidedBy.TIME -> timeDate to (odometerAtTimeDue?.let { stringResource(R.string.rules_time_last_est, it.asDistance(unit)) } ?: stringResource(R.string.rules_time_last))
            null -> stringResource(R.string.rules_both, odometer, timeDate) to stringResource(R.string.rules_last_no_pace)
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
private fun LeadTimeFields(lead: LeadTime, unit: DistanceUnit, onChange: (LeadTime) -> Unit) {
    // Keyed on the unit: changing the default unit converts the saved distance, and the box has to follow.
    var miles by rememberSaveable(unit) { mutableStateOf(lead.miles.toString()) }
    var days by rememberSaveable { mutableStateOf(lead.days.toString()) }
    // Saved as soon as both boxes hold something valid; otherwise the last good values stay in force.
    val update = { m: String, d: String ->
        miles = m
        days = d
        LeadTimeInput.parse(m.typedNumber(), d.typedNumber())?.let(onChange)
    }
    val valid = LeadTimeInput.parse(miles.typedNumber(), days.typedNumber()) != null
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(stringResource(R.string.rules_remind_early), bold = true)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = miles,
                onValueChange = { update(it, days) },
                label = { Text(stringResource(R.string.rules_distance_before, unit.titleWord)) },
                singleLine = true,
                isError = LeadTimeInput.parse(miles.typedNumber(), "0") == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = days,
                onValueChange = { update(miles, it) },
                label = { Text(stringResource(R.string.rules_days_before)) },
                singleLine = true,
                isError = LeadTimeInput.parse("0", days.typedNumber()) == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            if (valid) {
                stringResource(R.string.rules_lead_note)
            } else {
                stringResource(R.string.rules_lead_error, LeadTimeInput.MAX_MILES.asDistance(unit), LeadTimeInput.MAX_DAYS.asNumber())
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (valid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
        )
    }
}

