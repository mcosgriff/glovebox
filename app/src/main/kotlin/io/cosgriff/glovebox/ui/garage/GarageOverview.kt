package io.cosgriff.glovebox.ui.garage

import io.cosgriff.glovebox.data.db.OdometerDao
import io.cosgriff.glovebox.data.db.ScheduleDao
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.ComingUp
import io.cosgriff.glovebox.domain.OdometerReading
import io.cosgriff.glovebox.domain.ReminderPresentation
import io.cosgriff.glovebox.domain.UrgentItem
import io.cosgriff.glovebox.domain.VehicleReminders
import io.cosgriff.glovebox.ui.service.toEntry
import io.cosgriff.glovebox.ui.service.toTracked
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

data class GarageVehicle(val vehicle: VehicleEntity, val odometer: Int?, val comingUp: ComingUp)

data class Garage(val vehicles: List<GarageVehicle>) {
    val overdue: Int get() = vehicles.sumOf { it.comingUp.overdue }
    val dueSoon: Int get() = vehicles.sumOf { it.comingUp.dueSoon }

    /** Only worth showing when there is more than one vehicle to look across. */
    val urgent: List<UrgentItem<VehicleEntity>>
        get() = if (vehicles.size < 2) emptyList() else ReminderPresentation.mostUrgent(vehicles.map { it.vehicle to it.comingUp })
}

/** Every active vehicle with its reminders, for the screens that look across the whole garage. */
class GarageOverview @Inject constructor(
    vehicles: VehicleDao,
    schedule: ScheduleDao,
    service: ServiceDao,
    odometer: OdometerDao,
    settings: SettingsRepository,
) {
    val garage: Flow<Garage> = combine(
        vehicles.observeActive(),
        schedule.observeAllTracked(),
        service.observeAllRecords(),
        odometer.observeAll(),
        settings.settings,
    ) { active, items, records, readings, s ->
        val itemsBy = items.groupBy { it.vehicleId }
        val recordsBy = records.groupBy { it.record.vehicleId }
        val readingsBy = readings.groupBy { it.vehicleId }
        val today = LocalDate.now()
        Garage(
            active.map { v ->
                val entries = recordsBy[v.id].orEmpty().map { it.toEntry() }
                val updates = readingsBy[v.id].orEmpty().map { OdometerReading(it.date, it.odometer) }
                val tracked = itemsBy[v.id].orEmpty().mapNotNull { it.toTracked(s.defaultRule) }
                GarageVehicle(
                    vehicle = v,
                    odometer = VehicleReminders.currentOdometer(entries, updates),
                    comingUp = ReminderPresentation.comingUp(VehicleReminders.statuses(tracked, entries, updates, today)),
                )
            },
        )
    }
}
