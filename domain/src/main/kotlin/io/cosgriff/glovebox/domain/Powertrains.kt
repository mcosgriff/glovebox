package io.cosgriff.glovebox.domain

/**
 * What moves a vehicle, as far as a maker's schedule tells them apart: a hybrid and the petrol car with the
 * same engine size have different guides, and so do a flex-fuel truck and its petrol twin. Stored by name
 * (`vehicles.powertrain`); null is "not known", never [GAS].
 */
enum class Powertrain(val fileName: String) {
    GAS("gas"),
    HYBRID("hybrid"),
    PLUG_IN("plug-in"),
    FLEX_FUEL("flex-fuel"),
    ELECTRIC("electric"),
}

object Powertrains {
    /** A stored name, or null for nothing on record (or a name a later version dropped). */
    fun of(stored: String?): Powertrain? = Powertrain.entries.firstOrNull { it.name == stored }

    /** As a bundled list writes it ("plug-in"). */
    fun fromFile(name: String): Powertrain? = Powertrain.entries.firstOrNull { it.fileName == name }

    /**
     * What vPIC's decode says, where it says it plainly: its electrification level ("Strong HEV (Hybrid Electric
     * Vehicle)", "PHEV (Plug-in Hybrid Electric Vehicle)", "BEV (Battery Electric Vehicle)") and its fuels
     * ("Ethanol (E85)", "Flexible Fuel Vehicle (FFV)"). **Never [Powertrain.GAS]:** vPIC leaves these fields blank
     * for many hybrids, so a blank is "can't tell". A mild hybrid, or a hybrid of unknown level, is also "can't
     * tell", since a maker's hybrid schedule is for a full hybrid. Only the owner says a vehicle is petrol.
     */
    fun fromVpic(electrification: String?, primaryFuel: String?, secondaryFuel: String?): Powertrain? {
        val e = electrification?.uppercase().orEmpty()
        val fuels = listOfNotNull(primaryFuel, secondaryFuel).joinToString(" ").uppercase()
        return when {
            "PHEV" in e || "PLUG-IN" in e -> Powertrain.PLUG_IN
            e.startsWith("BEV") || "BATTERY ELECTRIC" in e -> Powertrain.ELECTRIC
            "STRONG HEV" in e -> Powertrain.HYBRID
            e.isNotBlank() -> null
            "ETHANOL" in fuels || "E85" in fuels || "FLEXIBLE FUEL" in fuels -> Powertrain.FLEX_FUEL
            else -> null
        }
    }
}
