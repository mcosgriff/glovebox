package io.cosgriff.glovebox.ui.garage

import io.cosgriff.glovebox.ui.service.log
import io.cosgriff.glovebox.data.db.OdometerDao
import io.cosgriff.glovebox.data.db.RecallDao
import io.cosgriff.glovebox.data.db.ScheduleDao
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.nhtsa.toStored
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.ComingUp
import io.cosgriff.glovebox.domain.ItemStatus
import io.cosgriff.glovebox.domain.OpenRecalls
import io.cosgriff.glovebox.domain.Recalls
import io.cosgriff.glovebox.domain.ReminderPresentation
import io.cosgriff.glovebox.domain.Snoozing
import io.cosgriff.glovebox.domain.UrgentItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

data class GarageVehicle(
    val vehicle: VehicleEntity,
    val odometer: Int?,
    val comingUp: ComingUp,
    /** The oil change reminder, which others can be snoozed to; null if it isn't tracked. */
    val oil: ItemStatus?,
    /** Null until NHTSA has answered for this vehicle, so "No open recalls" is never a guess. */
    val recalls: OpenRecalls? = null,
)

data class Garage(val vehicles: List<GarageVehicle>) {
    val overdue: Int get() = vehicles.sumOf { it.comingUp.overdue }
    val dueSoon: Int get() = vehicles.sumOf { it.comingUp.dueSoon }
    val openRecalls: Int get() = vehicles.sumOf { it.recalls?.count ?: 0 }

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
    recalls: RecallDao,
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
                val log = v.log(itemsBy[v.id].orEmpty(), recordsBy[v.id].orEmpty(), readingsBy[v.id].orEmpty(), s)
                val statuses = log.statuses(today)
                val oilId = itemsBy[v.id].orEmpty().firstOrNull { it.templateKey == Snoozing.OIL_TEMPLATE_KEY }?.id
                GarageVehicle(
                    vehicle = v,
                    odometer = log.odometer,
                    comingUp = ReminderPresentation.comingUp(statuses),
                    oil = statuses.firstOrNull { it.item.id == oilId },
                )
            },
        )
    }.combine(recalls.observeAll()) { garage, all ->
        val recallsBy = all.groupBy { it.vehicleId }
        garage.copy(
            vehicles = garage.vehicles.map { g ->
                if (g.vehicle.recallsCheckedOn == null) g
                else g.copy(recalls = Recalls.open(recallsBy[g.vehicle.id].orEmpty().map { it.toStored() }))
            },
        )
    }
}
