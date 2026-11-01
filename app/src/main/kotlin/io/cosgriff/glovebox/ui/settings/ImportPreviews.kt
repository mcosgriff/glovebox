package io.cosgriff.glovebox.ui.settings

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import io.cosgriff.glovebox.domain.ImportPlan
import io.cosgriff.glovebox.ui.service.pluralWord
import io.cosgriff.glovebox.ui.service.ruleInSentence
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.ui.torque.text
import io.cosgriff.glovebox.ui.schedule.text
import io.cosgriff.glovebox.ui.service.label
import io.cosgriff.glovebox.ui.service.orText
import io.cosgriff.glovebox.domain.FileDistances
import io.cosgriff.glovebox.ui.service.unit
import io.cosgriff.glovebox.domain.DistanceUnit
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.HistoryImport
import io.cosgriff.glovebox.domain.ImportedRecord
import io.cosgriff.glovebox.domain.TableJob
import io.cosgriff.glovebox.domain.LogEdits
import io.cosgriff.glovebox.domain.Money
import io.cosgriff.glovebox.domain.SavedRecord
import io.cosgriff.glovebox.domain.format
import io.cosgriff.glovebox.domain.ImportKind
import io.cosgriff.glovebox.domain.ImportMode
import io.cosgriff.glovebox.domain.ImportedScheduleItem
import io.cosgriff.glovebox.domain.ImportedTorqueSpec
import io.cosgriff.glovebox.domain.Interval
import io.cosgriff.glovebox.domain.SavedScheduleItem
import io.cosgriff.glovebox.domain.SavedTorqueSpec
import io.cosgriff.glovebox.domain.Torque
import io.cosgriff.glovebox.domain.TorqueSource
import io.cosgriff.glovebox.domain.TorqueUnit
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.service.text
import kotlinx.coroutines.flow.map

/**
 * What a preview shows of [this] plan, the same way for every kind: each new item and each one it fills by [name]
 * and [describe], each that differs by [differName] (the file's name, unless the saved one is the one to recognise)
 * with both sides. The rest is particular to a kind, and passed through.
 */
internal fun <I, E> ImportPlan<I, E>.toView(
    kind: ImportKind,
    noun: PlanNoun,
    @StringRes caution: Int,
    name: (I) -> String,
    describe: (I) -> String,
    describeSaved: (E) -> String,
    differName: (I, E) -> String = { item, _ -> name(item) },
    note: String? = null,
    doubles: List<String> = emptyList(),
    moves: Map<ImportMode, List<String>> = emptyMap(),
    doubts: List<DoubtLine> = emptyList(),
    done: Map<ImportMode, List<DoneLine>> = emptyMap(),
): PlanView = PlanView(
    kind = kind,
    noun = noun,
    total = total,
    new = new.map { PlanLine(name(it), describe(it)) },
    fills = fills.map { (item, _) -> PlanLine(name(item), describe(item)) },
    differs = differs.map { (item, saved) -> PlanLine(differName(item, saved), describe(item), describeSaved(saved)) },
    same = same.size,
    caution = AppText.res.getString(caution),
    doubles = doubles,
    moves = moves,
    doubts = doubts,
    done = done,
    note = note,
)

/** "100 ft-lb · 21 mm · Star pattern", or what to say for a row that is only a name. */
internal fun describe(torque: Torque?, socket: String?, note: String?, unit: TorqueUnit): String =
    listOfNotNull(torque?.text(unit), socket, note).joinToString(" · ").ifEmpty { AppText.res.getString(R.string.import_name_only) }

internal fun ImportedTorqueSpec.describe(unit: TorqueUnit) = listOfNotNull(describe(torque, socket, note, unit), factoryManual.takeIf { source == TorqueSource.FACTORY }).joinToString(" · ")
internal fun SavedTorqueSpec.describe(unit: TorqueUnit) = listOfNotNull(describe(torque, socket, note, unit), factoryManual.takeIf { source == TorqueSource.FACTORY && torque != null }).joinToString(" · ")

/** "Every 7,500 mi or 12 mo · severe 5,000 mi or 6 mo · whichever comes first" */
internal fun describe(normal: Interval?, severe: Interval?, rule: DueRule?, service: String?, unit: DistanceUnit, firstAt: Int? = null): String = listOfNotNull(
    firstAt?.let { AppText.res.getString(R.string.import_first_at, it.asDistance(unit)) },
    normal?.let { AppText.res.getString(if (firstAt != null) R.string.import_then_every else R.string.interval_every, it.orText(unit)) }
        ?: AppText.res.getString(R.string.import_no_interval),
    severe?.let { AppText.res.getString(R.string.import_severe, it.orText(unit)) },
    rule?.ruleInSentence(),
    HistoryImport.services[service]?.let { AppText.res.getString(R.string.import_reset_by, it.label) },
).joinToString(" · ")

/** "Oil & filter change, Tire rotation · Corner Garage · $62.50" */
internal fun describe(labels: List<String>, who: String, costCents: Long?): String =
    listOfNotNull(labels.joinToString(", "), who, costCents?.let(Money::format)).joinToString(" · ")

internal fun ImportedRecord.describe() = describe(work.map { it.label }, LogEdits.who(diy, shopName, null).text, costCents)
internal fun SavedRecord.describe() = describe(labels, LogEdits.who(diy, shopName, null, enteredAsLastDone).text, costCents)

/** "Change oil, Rotate tires · or 12 months" */
internal fun describe(work: List<TableJob>, orMonths: Int?): String =
    listOfNotNull(work.joinToString(", ") { it.name }, orMonths?.let { AppText.res.getQuantityString(R.plurals.milestones_or_months, it, it.asNumber()) }).joinToString(" · ")

internal fun ImportedScheduleItem.describe(unit: DistanceUnit) = listOfNotNull(describe(normal, severe, rule, service, unit, firstAt), severeOnlyWords.takeIf { severeOnly }).joinToString(" · ")
internal fun SavedScheduleItem.describe(unit: DistanceUnit) = listOfNotNull(describe(normal, severe, rule, service, unit, firstAt), severeOnlyWords.takeIf { severeOnly }).joinToString(" · ")

/** What a plan counts: the words for it, by kind of file. */
enum class PlanNoun(@PluralsRes private val counted: Int, @StringRes val differ: Int, @StringRes val replaceOnly: Int, @PluralsRes val fills: Int) {
    ROW(R.plurals.plan_rows, R.string.plan_rows_differ, R.string.plan_rows_replace_only, R.plurals.plan_rows_fill),
    ITEM(R.plurals.plan_items, R.string.plan_items_differ, R.string.plan_items_replace_only, R.plurals.plan_items_fill),
    MILESTONE(R.plurals.milestones, R.string.plan_milestones_differ, R.string.plan_milestones_replace_only, R.plurals.plan_milestones_fill),
    RECORD(R.plurals.records, R.string.plan_records_differ, R.string.plan_records_replace_only, R.plurals.plan_records_fill),
    ;

    /** "3 rows" */
    fun count(n: Int): String = AppText.res.getQuantityString(counted, n, n.asNumber())
}

val ImportKind.label: String
    get() = AppText.res.getString(
        when (this) {
            ImportKind.SERVICE_HISTORY -> R.string.import_kind_history
            ImportKind.INTERVAL_SCHEDULE -> R.string.details_schedule
            ImportKind.MILESTONE_SCHEDULE -> R.string.milestones_title
            ImportKind.TORQUE_SPECS -> R.string.import_kind_torque
        },
    )

/** Said under a preview when the file's unit isn't the vehicle's: what the page lists is what will be saved, not what the file says. */
internal fun convertedNote(from: DistanceUnit, to: DistanceUnit): String? =
    AppText.res.getString(R.string.import_converted, from.pluralWord, to.pluralWord).takeIf { FileDistances.converts(from, to) }

internal val factoryManual: String get() = AppText.res.getString(R.string.import_factory_manual)

internal val severeOnlyWords: String get() = AppText.res.getString(R.string.import_severe_only)
