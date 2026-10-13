package io.cosgriff.glovebox.ui.about

import io.cosgriff.glovebox.ui.theme.PlexMono
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.cosgriff.glovebox.ui.schedule.SectionLabel

const val SOURCE_URL = "https://github.com/mcosgriff/glovebox"

/** Opens a web page in the browser. Returns false if the phone has nothing that can. */
fun Context.openWebPage(url: String): Boolean = try {
    startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    true
} catch (_: ActivityNotFoundException) {
    false
}

/** The installed version, as Android reports it. */
private fun Context.versionName(): String =
    runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull().orEmpty()

/** A pushed screen's header: back arrow and title. */
@Composable
internal fun AboutHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.padding(start = 8.dp, top = 12.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
        Text(title, Modifier.padding(start = 8.dp).semantics { heading() }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun AboutCard(content: @Composable ColumnScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxWidth(), color = scheme.surface, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, scheme.outlineVariant)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), content = content)
    }
}

/** Design: 9c · About. */
@Composable
fun AboutScreen(onBack: () -> Unit, onOpenPrivacy: () -> Unit, onOpenLicences: () -> Unit, onMessage: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val version = remember { context.versionName() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AboutHeader("About", onBack)

        Column(
            Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(color = scheme.primary, shape = RoundedCornerShape(16.dp), modifier = Modifier.size(56.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Build, contentDescription = null, modifier = Modifier.size(30.dp), tint = scheme.onPrimary)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Glovebox", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        listOf(version, "updates via Obtainium").filter { it.isNotEmpty() }.joinToString(" \u00b7 "),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = PlexMono,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("WHERE THE DATA COMES FROM", Modifier.semantics { heading() })
                AboutCard {
                    Source("NHTSA vPIC", "VIN decoding, make and model lists")
                    HorizontalDivider(color = scheme.outlineVariant)
                    Source("NHTSA recalls", "Recalls by year, make and model")
                    HorizontalDivider(color = scheme.outlineVariant)
                    // No manufacturer data: the schedule is a general starter list and torque specs are typed in.
                    Source("Maintenance schedules & specs", "Typical intervals to start from, and your own entries")
                }
                Text(
                    "Intervals and torque specs are for reference. Check your owner's or service manual before relying on them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }

            AboutCard {
                LinkRow("Privacy", "No accounts, no tracking", onOpenPrivacy)
                HorizontalDivider(color = scheme.outlineVariant)
                LinkRow("Source code", "GitHub") {
                    if (!context.openWebPage(SOURCE_URL)) onMessage("No browser to open $SOURCE_URL")
                }
                HorizontalDivider(color = scheme.outlineVariant)
                LinkRow("Open-source licenses", "", onOpenLicences)
            }
        }
    }
}

@Composable
private fun Source(title: String, about: String) {
    Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(about, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun LinkRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button, onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        if (value.isNotEmpty()) Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(20.dp))
    }
}

/** What the app keeps, what it sends, and what it never does. Every line here has to stay true of the code. */
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AboutHeader("Privacy", onBack)
        Column(
            Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            listOf(
                "Everything stays on this phone" to listOf(
                    "There is no account and no Glovebox server. Your vehicles, service records, photos and settings exist only on this phone.",
                    "The database and photos are encrypted with keys held by Android Keystore.",
                    "Android's own cloud backup and device-to-device transfer are switched off for this app. The only copy that can leave is a backup file you make yourself.",
                ),
                "What is sent, and to whom" to listOf(
                    "The app talks to one organisation: NHTSA, the U.S. vehicle safety agency (vpic.nhtsa.dot.gov and api.nhtsa.gov).",
                    "Decoding a VIN sends that VIN.",
                    "Adding a vehicle without a VIN asks for NHTSA's list of makes, then for the models of the make and year you pick.",
                    "Checking for recalls sends the year, make and model. It does not send the VIN.",
                    "Like any website, NHTSA can see the internet address the request came from.",
                    "Nothing else is sent: no mileage, records, photos, names or location.",
                ),
                "What the app never does" to listOf(
                    "No analytics, crash reporting, advertising or tracking, and no third-party code that reports back to anyone.",
                ),
                "Files you save" to listOf(
                    "A backup file is encrypted with the password you choose. Glovebox does not keep the password and cannot recover it.",
                    "A PDF or Markdown export is not encrypted. Anyone who has the file can read it.",
                    "A schedule, milestone table or set of torque specs saved as an import file is not encrypted either. It holds what is on that screen (items and intervals, mileages and jobs, or fasteners with your values and notes) and nothing else about your vehicle or its history.",
                    "Deleting all data in Settings does not touch files you have already saved elsewhere.",
                ),
                "Files you import" to listOf(
                    "Import reads the one file you pick, on this phone, and adds what it holds to the vehicle you choose. The file is not copied or kept, and nothing is sent anywhere.",
                ),
                "When you leave the app" to listOf(
                    "A manufacturer's recall page, Find a dealer and the source code open in your browser or maps app. What those do is up to them.",
                    "Sharing a photo or saving a file hands it to the app you choose.",
                ),
                "Permissions" to listOf(
                    "Camera: to scan a VIN barcode and to photograph receipts and parts. Scanner frames are read and thrown away, never stored.",
                    "Notifications: for reminders, new recalls and the backup reminder.",
                    "Internet: for NHTSA only.",
                    "Fingerprint, face or screen lock: Android does the checking and tells the app only whether it passed.",
                ),
                "Updates" to listOf(
                    "Glovebox does not check for updates itself. If you installed it with Obtainium, Obtainium checks GitHub for new releases.",
                ),
            ).forEach { (title, lines) ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    AboutCard {
                        Column(Modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            lines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface) }
                        }
                    }
                }
            }
        }
    }
}
