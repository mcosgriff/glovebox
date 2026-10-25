package io.cosgriff.glovebox.domain

/**
 * Looking a saved vehicle's VIN up again (#462): it only ever fills what is empty, so nothing the owner
 * typed or corrected is replaced.
 */
object VinRefresh {
    /** The decoded values for fields that are empty in [saved]. */
    fun <K> additions(saved: Map<K, String?>, found: Map<K, String?>): Map<K, String> =
        found.mapNotNull { (field, value) ->
            value?.trim()?.takeIf { it.isNotEmpty() && saved[field].isNullOrBlank() }?.let { field to it }
        }.toMap()

    /**
     * Whether the decode describes the saved vehicle: the same year and make. The model isn't compared, since
     * it may have been corrected when the vehicle was added; a VIN typed in later could be anyone's.
     */
    fun sameVehicle(savedYear: Int, savedMake: String, decodedYear: Int?, decodedMake: String?): Boolean =
        decodedYear == savedYear && decodedMake != null && decodedMake.trim().equals(savedMake.trim(), ignoreCase = true)
}
