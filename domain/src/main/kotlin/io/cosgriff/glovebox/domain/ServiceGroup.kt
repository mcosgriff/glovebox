package io.cosgriff.glovebox.domain

/**
 * The few groups History's timeline and the PDF report sort work into, each drawn in its own colour and always
 * named beside it. They come from the service catalog's categories: a line through the catalog line it was picked
 * from, else through the schedule item it resets ([of]), and a line with neither (Custom) is [OTHER]. Seven, and no
 * more: past that, small colour marks stop being told apart (#410).
 */
enum class ServiceGroup {
    OIL_ENGINE, TIRES, BRAKES, FLUIDS_4WD, SUSPENSION, FUEL_EXHAUST, OTHER;

    companion object {
        private val byCatalogCategory = mapOf(
            "engine" to OIL_ENGINE,
            "tires" to TIRES,
            "brakes" to BRAKES,
            "fluids" to FLUIDS_4WD,
            "cooling" to FLUIDS_4WD,
            "driveline" to FLUIDS_4WD,
            "suspension" to SUSPENSION,
            "fuel" to FUEL_EXHAUST,
            "exhaust" to FUEL_EXHAUST,
        )

        private val byCatalogItem: Map<String, ServiceGroup> = ServiceCatalog.categories
            .flatMap { c -> c.items.map { it.id to (byCatalogCategory[c.id] ?: OTHER) } }
            .toMap()

        private val byTemplateKey: Map<String, ServiceGroup> = ServiceCatalog.categories
            .flatMap { c -> c.items.mapNotNull { item -> item.resets?.let { it to (byCatalogCategory[c.id] ?: OTHER) } } }
            .toMap()

        /** A catalog line's group. */
        fun ofLine(catalogId: String?): ServiceGroup = catalogId?.let(byCatalogItem::get) ?: OTHER

        /**
         * A logged line's group, the one answer the timeline, the PDF report and the Markdown all read (#579): its
         * catalog line's, or else, for a line logged against a schedule item with no catalog line (the owner's own
         * items save none), that item's group in [itemGroups] ([ofItem], by item id). A line with neither is [OTHER].
         */
        fun of(line: RecordLine, itemGroups: Map<Long, ServiceGroup>): ServiceGroup =
            line.catalogId?.let(::ofLine) ?: line.scheduleItemId?.let(itemGroups::get) ?: OTHER

        /** A schedule item's group: a typical item through its [templateKey], the owner's own through its [service]. */
        fun ofItem(templateKey: String?, service: String?): ServiceGroup =
            templateKey?.let(byTemplateKey::get) ?: service?.let(byCatalogItem::get) ?: OTHER
    }
}
