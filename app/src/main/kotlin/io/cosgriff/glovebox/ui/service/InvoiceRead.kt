package io.cosgriff.glovebox.ui.service

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import io.cosgriff.glovebox.domain.PhotoPage
import io.cosgriff.glovebox.domain.PhotoPages
import io.cosgriff.glovebox.domain.RecordPhotos
import io.cosgriff.glovebox.ui.photo.AttachMenu
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.InvoiceFigure
import io.cosgriff.glovebox.domain.InvoiceFill
import io.cosgriff.glovebox.domain.InvoiceLine
import io.cosgriff.glovebox.domain.InvoiceSection
import io.cosgriff.glovebox.domain.LineRole
import io.cosgriff.glovebox.domain.Money
import io.cosgriff.glovebox.domain.OdometerRole
import io.cosgriff.glovebox.domain.format
import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.ui.components.GloveboxSheet
import io.cosgriff.glovebox.ui.components.SwitchRow

/** What an invoice was read as (#484), for "What was read": its lines and what each became. Held in memory only. */
class ReadInvoice(val fill: InvoiceFill, val lines: List<InvoiceLine>, val roles: List<List<LineRole>>, val photos: Boolean = false)

/** Why a picked invoice filled nothing. */
enum class InvoiceProblem { NOT_A_PDF, UNREADABLE, NO_TEXT }

/** The recommended work and odometer in, for the notes; empty when the invoice had neither. */
internal fun invoiceNotes(fill: InvoiceFill, unit: DistanceUnit): String {
    val res = AppText.res
    val recommended = fill.recommendations.takeIf { it.isNotEmpty() }?.joinToString(res.getString(R.string.invoice_notes_separator)) { r ->
        r.cents?.let { res.getString(R.string.invoice_notes_item_priced, r.text, Money.format(it)) } ?: r.text
    }?.let { res.getString(R.string.invoice_notes_recommended, it) }
    val odometerIn = fill.odometerIn?.let { res.getString(R.string.invoice_notes_odometer_in, it.asDistance(unit)) }
    return listOfNotNull(recommended, odometerIn).joinToString("\n")
}

/** A paper invoice's pages on the form (#513), and what can be done with them; the ViewModel's, or a test's. */
class InvoicePages(
    val pages: PhotoPages = PhotoPages(),
    /** Why the last pages added fewer than were asked for. */
    val problem: String? = null,
    val onTake: () -> Unit = {},
    val onChoose: () -> Unit = {},
    val onRetake: (file: String) -> Unit = {},
    val onMove: (file: String, by: Int) -> Unit = { _, _ -> },
    val onRemove: (file: String) -> Unit = {},
    val onFill: () -> Unit = {},
    val onPutAside: () -> Unit = {},
)

/** Draws one page's picture: [PhotoThumb][io.cosgriff.glovebox.ui.photo.PhotoThumb] in the app, a plain tile in the screenshot tests. */
typealias PageThumb = @Composable (file: String, description: String, modifier: Modifier) -> Unit

/**
 * The invoice card at the top of a new Log service: "Read a shop's invoice" (#484) and "Photograph a paper invoice"
 * (#513); while pages are being photographed, their strip; and once read, what came of it: a reminder to check it
 * against the invoice, the VIN question, what couldn't be filled in, keeping the PDF or the pages, and "What was read".
 */
@Composable
internal fun InvoiceCard(form: LogServiceForm, vehicleName: String, onRead: () -> Unit, pages: InvoicePages, thumb: PageThumb) {
    val scheme = MaterialTheme.colorScheme
    val read = form.invoice
    var showing by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            read != null -> Surface(color = scheme.primaryContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.invoice_filled_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = scheme.onPrimaryContainer)
                    Text(stringResource(R.string.invoice_filled_body), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
                    read.fill.vinMismatch?.let {
                        Text(stringResource(R.string.invoice_vin_mismatch, it.invoiceEnd, it.vehicleEnd), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                    }
                    if (read.fill.jobs.isEmpty()) Text(stringResource(R.string.invoice_no_work), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
                    form.invoiceLeftOut.forEach { Text(stringResource(R.string.invoice_left_out, it), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface) }
                    KeepSwitch(form, vehicleName, photos = read.photos)
                    TextButton(onClick = { showing = true }, modifier = Modifier.heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 0.dp)) {
                        Text(stringResource(R.string.invoice_what_was_read), color = scheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            form.readingInvoice -> Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                Text(stringResource(R.string.invoice_reading), style = MaterialTheme.typography.bodyMedium)
            }
            pages.pages.pages.isNotEmpty() -> PagesCard(form, vehicleName, pages, thumb)
            else -> {
                ReadRow(Icons.Outlined.Description, stringResource(R.string.invoice_read_title), stringResource(R.string.invoice_read_hint), onRead)
                AttachMenu(onTake = pages.onTake, onChoose = pages.onChoose, modifier = Modifier.fillMaxWidth()) { open ->
                    ReadRow(Icons.Outlined.PhotoCamera, stringResource(R.string.invoice_photos_title), stringResource(R.string.invoice_photos_hint), open)
                }
            }
        }
        val problem = form.invoiceProblem?.let {
            stringResource(
                when (it) {
                    InvoiceProblem.NOT_A_PDF -> R.string.invoice_problem_not_pdf
                    InvoiceProblem.UNREADABLE -> R.string.invoice_problem_unreadable
                    InvoiceProblem.NO_TEXT -> R.string.invoice_problem_no_text
                },
            )
        } ?: pages.problem.takeIf { read == null }
        problem?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = scheme.error) }
    }
    if (showing && read != null) WhatWasRead(read, onDismiss = { showing = false })
}

/** One way to fill the form from an invoice, as a row to tap. */
@Composable
private fun ReadRow(icon: ImageVector, title: String, hint: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(color = scheme.primaryContainer, shape = MaterialTheme.shapes.extraSmall, modifier = Modifier.size(32.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = scheme.onPrimaryContainer, modifier = Modifier.size(18.dp)) }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(hint, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        }
    }
}

/** Keep the PDF among the vehicle's documents, or the pages as the record's photos: on unless turned off. */
@Composable
private fun KeepSwitch(form: LogServiceForm, vehicleName: String, photos: Boolean) {
    SwitchRow(
        title = stringResource(if (photos) R.string.invoice_keep_pages_title else R.string.invoice_keep_title),
        about = if (photos) stringResource(R.string.invoice_keep_pages_about) else stringResource(R.string.invoice_keep_about, vehicleName),
        checked = form.keepInvoice,
        onChange = { form.keepInvoice = it },
    )
}

/**
 * A paper invoice's pages while they are photographed and read (#513): the strip, in the order they will be read, each
 * page tapped to move it, take it again or remove it; how far reading has got; and, once every page is read, filling
 * the form from them.
 */
@Composable
private fun PagesCard(form: LogServiceForm, vehicleName: String, pages: InvoicePages, thumb: PageThumb) {
    val scheme = MaterialTheme.colorScheme
    val all = pages.pages
    Surface(shape = MaterialTheme.shapes.medium, color = scheme.surface, border = BorderStroke(1.dp, scheme.outlineVariant), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.invoice_pages_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.invoice_pages_about), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                all.pages.forEachIndexed { i, page -> PageTile(i, page, all.pages.size, pages, thumb) }
            }
            when {
                all.ready -> Button(onClick = pages.onFill, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(pluralStringResource(R.plurals.invoice_pages_fill, all.pages.size, all.pages.size.asNumber()))
                }
                all.nothingRead -> Text(stringResource(R.string.invoice_pages_nothing_read), style = MaterialTheme.typography.bodySmall, color = scheme.error)
                else -> Row(Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(
                        stringResource(R.string.invoice_pages_reading, (all.readCount + 1).asNumber(), all.pages.size.asNumber()),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (RecordPhotos.room(all.pages.size) > 0) {
                    AttachMenu(onTake = pages.onTake, onChoose = pages.onChoose) { open ->
                        TextButton(onClick = open, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(stringResource(R.string.invoice_pages_add), color = scheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                TextButton(onClick = pages.onPutAside, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.invoice_pages_put_aside), color = scheme.onSurfaceVariant)
                }
            }
            KeepSwitch(form, vehicleName, photos = true)
        }
    }
}

/** One page in the strip: its picture, whether it has been read, and its menu. */
@Composable
private fun PageTile(index: Int, page: PhotoPage, count: Int, pages: InvoicePages, thumb: PageThumb) {
    val scheme = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    val name = stringResource(R.string.invoice_page, (index + 1).asNumber())
    Column(Modifier.width(96.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box {
            Box(Modifier.size(96.dp, 128.dp).clickable(role = Role.Button, onClickLabel = stringResource(R.string.invoice_page_options, (index + 1).asNumber())) { menu = true }) {
                thumb(page.file, name, Modifier.fillMaxSize())
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                if (index > 0) MenuItem(stringResource(R.string.invoice_page_earlier)) { menu = false; pages.onMove(page.file, -1) }
                if (index < count - 1) MenuItem(stringResource(R.string.invoice_page_later)) { menu = false; pages.onMove(page.file, 1) }
                MenuItem(stringResource(R.string.invoice_page_retake)) { menu = false; pages.onRetake(page.file) }
                MenuItem(stringResource(R.string.invoice_page_remove)) { menu = false; pages.onRemove(page.file) }
            }
        }
        Text(
            when (page.words?.isEmpty()) {
                null -> stringResource(R.string.invoice_page_reading)
                true -> stringResource(R.string.invoice_page_blank)
                false -> stringResource(R.string.invoice_page_read)
            },
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MenuItem(text: String, onClick: () -> Unit) = DropdownMenuItem(text = { Text(text) }, onClick = onClick)

/** "What was read": each page's lines as read, and what each became. Only to look at; nothing here is saved. */
@Composable
internal fun WhatWasRead(read: ReadInvoice, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    GloveboxSheet(onDismiss = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.invoice_what_was_read), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.invoice_what_was_read_about), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            var page = -1
            read.lines.forEachIndexed { i, line ->
                if (line.page != page) {
                    page = line.page
                    Text(
                        stringResource(R.string.invoice_page, line.page.asNumber()),
                        Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurfaceVariant,
                    )
                    HorizontalDivider(color = scheme.outlineVariant)
                }
                val roles = read.roles.getOrNull(i).orEmpty()
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(line.text, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (roles.isEmpty()) stringResource(R.string.invoice_role_ignored) else roles.map { roleText(it) }.joinToString(stringResource(R.string.invoice_notes_separator)),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (roles.isEmpty()) scheme.onSurfaceVariant else scheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun roleText(role: LineRole): String = when (role) {
    LineRole.Repeated -> stringResource(R.string.invoice_role_repeated)
    is LineRole.Heading -> stringResource(
        when (role.section) {
            InvoiceSection.SERVICES -> R.string.invoice_section_services
            InvoiceSection.LINE_TABLE -> R.string.invoice_section_table
            InvoiceSection.TOTALS -> R.string.invoice_section_totals
            InvoiceSection.RECOMMENDED -> R.string.invoice_section_recommended
            InvoiceSection.TERMS -> R.string.invoice_section_terms
        },
    )
    is LineRole.Work -> stringResource(R.string.invoice_role_work)
    is LineRole.Figure -> {
        val name = stringResource(role.figure.label())
        val amount = Money.format(role.cents)
        stringResource(
            when (role.taken) {
                true -> R.string.invoice_role_figure_taken
                false -> R.string.invoice_role_figure_left_out
                null -> R.string.invoice_role_figure_check
            },
            name, amount,
        )
    }
    LineRole.Recommended -> stringResource(R.string.invoice_role_recommended)
    LineRole.Shop -> stringResource(R.string.invoice_role_shop)
    LineRole.Date -> stringResource(R.string.invoice_role_date)
    is LineRole.Vin -> stringResource(if (role.taken) R.string.invoice_role_vin else R.string.invoice_role_vin_left_out)
    is LineRole.Odometer -> {
        val value = role.value.asNumber()
        val said = stringResource(
            when (role.role) {
                OdometerRole.IN -> R.string.invoice_role_odometer_in
                OdometerRole.OUT -> R.string.invoice_role_odometer_out
                OdometerRole.UNLABELLED -> R.string.invoice_role_odometer
            },
            value,
        )
        if (role.taken) said else stringResource(R.string.invoice_role_left_out, said)
    }
}

private fun InvoiceFigure.label(): Int = when (this) {
    InvoiceFigure.SUBTOTAL -> R.string.invoice_figure_subtotal
    InvoiceFigure.LABOR -> R.string.invoice_figure_labor
    InvoiceFigure.PARTS -> R.string.invoice_figure_parts
    InvoiceFigure.FEES -> R.string.invoice_figure_fees
    InvoiceFigure.SUBLET -> R.string.invoice_figure_sublet
    InvoiceFigure.PART_EACH -> R.string.invoice_figure_part_each
    InvoiceFigure.PART_TOTAL -> R.string.invoice_figure_part_total
    InvoiceFigure.TOTAL_LABOR -> R.string.invoice_figure_total_labor
    InvoiceFigure.TOTAL_PARTS -> R.string.invoice_figure_total_parts
    InvoiceFigure.TOTAL_FEES -> R.string.invoice_figure_total_fees
    InvoiceFigure.TOTAL_SUBLETS -> R.string.invoice_figure_total_sublets
    InvoiceFigure.SUPPLIES -> R.string.invoice_figure_supplies
    InvoiceFigure.TAX -> R.string.invoice_figure_tax
    InvoiceFigure.OTHER_CHARGE -> R.string.invoice_figure_other
    InvoiceFigure.DISCOUNT -> R.string.invoice_figure_discount
    InvoiceFigure.GRAND_TOTAL -> R.string.invoice_figure_grand_total
    InvoiceFigure.BALANCE_DUE -> R.string.invoice_figure_balance_due
    InvoiceFigure.PAID -> R.string.invoice_figure_paid
}

/** Under a figure the invoice printed and no sum vouched for, while it is left blank. */
@Composable
internal fun UnreadNote() {
    Text(stringResource(R.string.invoice_unread), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
}

/** An outline in the accent for a field [UnreadNote] is under. */
@Composable
internal fun unreadFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    errorContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedBorderColor = MaterialTheme.colorScheme.primary,
)
