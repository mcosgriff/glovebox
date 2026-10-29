package io.cosgriff.glovebox.ui.service

import androidx.compose.foundation.BorderStroke
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
class ReadInvoice(val fill: InvoiceFill, val lines: List<InvoiceLine>, val roles: List<List<LineRole>>)

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

/**
 * "Read a shop's invoice" at the top of a new Log service (#484), and once read, what came of it: a reminder to check
 * it against the invoice, the VIN question, what couldn't be filled in, keeping the PDF, and "What was read".
 */
@Composable
internal fun InvoiceCard(form: LogServiceForm, vehicleName: String, onRead: () -> Unit) {
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
                    SwitchRow(
                        title = stringResource(R.string.invoice_keep_title),
                        about = stringResource(R.string.invoice_keep_about, vehicleName),
                        checked = form.keepInvoice,
                        onChange = { form.keepInvoice = it },
                    )
                    TextButton(onClick = { showing = true }, modifier = Modifier.heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 0.dp)) {
                        Text(stringResource(R.string.invoice_what_was_read), color = scheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            form.readingInvoice -> Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                Text(stringResource(R.string.invoice_reading), style = MaterialTheme.typography.bodyMedium)
            }
            else -> Surface(
                onClick = onRead,
                shape = MaterialTheme.shapes.medium,
                color = scheme.surface,
                border = BorderStroke(1.dp, scheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Surface(color = scheme.primaryContainer, shape = MaterialTheme.shapes.extraSmall, modifier = Modifier.size(32.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Description, contentDescription = null, tint = scheme.onPrimaryContainer, modifier = Modifier.size(18.dp)) }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.invoice_read_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.invoice_read_hint), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    }
                }
            }
        }
        form.invoiceProblem?.let {
            Text(
                stringResource(
                    when (it) {
                        InvoiceProblem.NOT_A_PDF -> R.string.invoice_problem_not_pdf
                        InvoiceProblem.UNREADABLE -> R.string.invoice_problem_unreadable
                        InvoiceProblem.NO_TEXT -> R.string.invoice_problem_no_text
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.error,
            )
        }
    }
    if (showing && read != null) WhatWasRead(read, onDismiss = { showing = false })
}

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
