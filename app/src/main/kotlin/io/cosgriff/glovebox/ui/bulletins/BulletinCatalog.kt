package io.cosgriff.glovebox.ui.bulletins

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.domain.BulletinFile
import io.cosgriff.glovebox.domain.BulletinList
import io.cosgriff.glovebox.domain.Bulletins
import io.cosgriff.glovebox.domain.VehicleBulletins
import io.cosgriff.glovebox.ui.components.readBundled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** The maker's bulletins that ship with the app (#459): every file in `assets/bulletins`, read once. */
@Singleton
class BulletinCatalog @Inject constructor(@ApplicationContext private val context: Context) {
    private val loaded: List<BulletinList> by lazy {
        context.readBundled(Bulletins.ASSET_DIR) { (Bulletins.parse(it) as? BulletinFile.Ok)?.list }
    }

    suspend fun lists(): List<BulletinList> = withContext(Dispatchers.IO) { loaded }

    /** [vehicle]'s bulletins for its year, make and model; null for a vehicle no list covers, or none at all. */
    fun forVehicle(vehicle: Flow<VehicleEntity?>): Flow<VehicleBulletins?> =
        combine(vehicle, flow { emit(lists()) }) { v, lists -> v?.let { Bulletins.forVehicle(lists, it.year, it.make, it.model) } }
}
