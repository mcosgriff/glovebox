package io.cosgriff.glovebox.data.log

import io.cosgriff.glovebox.data.db.OdometerDao
import io.cosgriff.glovebox.data.db.ScheduleDao
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Where a [VehicleLog] is read from (#420): the vehicle, all its schedule items, its records, its readings and the
 * settings, put together the one way. Screens, the widget and the workers read a vehicle's reminders through
 * here rather than gathering the rows themselves.
 */
@Singleton
class VehicleLogs @Inject constructor(
    private val vehicles: VehicleDao,
    private val schedule: ScheduleDao,
    private val service: ServiceDao,
    private val odometer: OdometerDao,
    private val settings: SettingsRepository,
) {
    /** Null while the vehicle doesn't exist, as after it is deleted. */
    fun observe(vehicleId: Long): Flow<VehicleLog?> = combine(
        vehicles.observe(vehicleId),
        schedule.observeAll(vehicleId),
        service.observeRecords(vehicleId),
        odometer.observe(vehicleId),
        settings.settings,
    ) { vehicle, items, records, readings, s -> vehicle?.log(items, records, readings, s) }

    /** The log as it is now, for a worker or a one-off read. Null if the vehicle doesn't exist. */
    suspend fun load(vehicleId: Long): VehicleLog? {
        val vehicle = vehicles.byId(vehicleId) ?: return null
        return vehicle.log(schedule.allFor(vehicleId), service.recordsFor(vehicleId), odometer.readingsFor(vehicleId), settings.settings.first())
    }

    /** Every active vehicle's log, in [VehicleDao.observeActive]'s order. Sold vehicles get no reminders. */
    fun observeGarage(): Flow<List<VehicleLog>> = combine(
        vehicles.observeActive(),
        schedule.observeEvery(),
        service.observeAllRecords(),
        odometer.observeAll(),
        settings.settings,
    ) { active, items, records, readings, s ->
        val itemsBy = items.groupBy { it.vehicleId }
        val recordsBy = records.groupBy { it.record.vehicleId }
        val readingsBy = readings.groupBy { it.vehicleId }
        active.map { v -> v.log(itemsBy[v.id].orEmpty(), recordsBy[v.id].orEmpty(), readingsBy[v.id].orEmpty(), s) }
    }
}
