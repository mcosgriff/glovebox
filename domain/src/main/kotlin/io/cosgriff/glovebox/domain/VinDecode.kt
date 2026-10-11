package io.cosgriff.glovebox.domain

/** How much of a VIN NHTSA's vPIC decoder could make sense of. */
enum class DecodeQuality {
    /** Error code 0 and a year, make and model. */
    CLEAN,

    /** Year, make and model came back, but vPIC flagged something: worth a look before saving. */
    PARTIAL,

    /** No usable year, make or model. The user has to fix the VIN or fill them in. */
    UNIDENTIFIED,
}

object VinDecode {
    /** The first model year a 17-character VIN can describe. */
    const val FIRST_YEAR = 1981

    /** [errorCode] is vPIC's, which may list several codes ("1,5"); "0" alone means a clean decode. */
    fun quality(errorCode: String?, year: Int?, make: String?, model: String?): DecodeQuality {
        if (year == null || make.isNullOrBlank() || model.isNullOrBlank()) return DecodeQuality.UNIDENTIFIED
        val codes = errorCode.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }
        return if (codes.all { it == "0" }) DecodeQuality.CLEAN else DecodeQuality.PARTIAL
    }

    /**
     * vPIC sometimes can't tell trims apart from the VIN and lists them together ("SR5/SR5 Premium",
     * "LX, EX"). More than one entry means the user should pick.
     */
    fun trimChoices(trim: String?): List<String> =
        trim.orEmpty().split('/', ',').map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    /** vPIC drive types read like "4WD/4-Wheel Drive/4x4"; the first part is enough for a chip. */
    fun shortDrive(driveType: String?): String? = driveType?.substringBefore('/')?.trim()?.takeIf { it.isNotEmpty() }

    /** A model year typed by hand; null unless it is one a road vehicle could have. */
    fun parseYear(text: String, thisYear: Int): Int? =
        text.trim().toIntOrNull()?.takeIf { it in FIRST_YEAR..thisYear + 1 }
}
