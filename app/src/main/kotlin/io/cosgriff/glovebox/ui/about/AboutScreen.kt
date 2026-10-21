package io.cosgriff.glovebox.ui.about

import io.cosgriff.glovebox.ui.components.ScreenHeader
import io.cosgriff.glovebox.ui.components.RowsCard
import androidx.compose.ui.platform.LocalResources
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import javax.inject.Inject
import io.cosgriff.glovebox.ui.torque.KnownSpecCatalog
import io.cosgriff.glovebox.domain.KnownSpecList
import io.cosgriff.glovebox.domain.KnownTakesList
import io.cosgriff.glovebox.ui.vehicle.KnownTakesCatalog
import io.cosgriff.glovebox.domain.KnownScheduleList
import io.cosgriff.glovebox.ui.schedule.KnownScheduleCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.runtime.getValue
import io.cosgriff.glovebox.ui.theme.PlexMono
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import io.cosgriff.glovebox.ui.components.SectionLabel

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

/** Design: 9c · About. */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenLicences: () -> Unit,
    onMessage: (String) -> Unit,
    vm: KnownListsViewModel = hiltViewModel(),
) {
    // The torque lists that ship with the app, each credited to where it came from.
    val known by vm.lists.collectAsStateWithLifecycle()
    val schedules by vm.schedules.collectAsStateWithLifecycle()
    val takes by vm.takes.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val res = LocalResources.current
    val version = remember { context.versionName() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader(stringResource(R.string.about), onBack)

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
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        listOf(version, stringResource(R.string.about_updates_via)).filter { it.isNotEmpty() }.joinToString(" \u00b7 "),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = PlexMono,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(stringResource(R.string.about_sources))
                RowsCard {
                    Source(stringResource(R.string.about_vpic), stringResource(R.string.about_vpic_note))
                    HorizontalDivider(color = scheme.outlineVariant)
                    Source(stringResource(R.string.about_recalls), stringResource(R.string.about_recalls_note))
                    HorizontalDivider(color = scheme.outlineVariant)
                    // Nothing of Glovebox's own: the schedule is a general starter list and torque specs are typed in.
                    // What else ships is a named list (a maker's guide or manual), offered for its model, never
                    // applied unasked, and marked to check.
                    Source(stringResource(R.string.about_schedules), stringResource(R.string.about_schedules_note))
                    known.forEach { list ->
                        HorizontalDivider(color = scheme.outlineVariant)
                        Source(list.source.name, stringResource(R.string.about_known_specs, list.title, list.source.note))
                    }
                    schedules.forEach { list ->
                        HorizontalDivider(color = scheme.outlineVariant)
                        Source(list.source.name, stringResource(R.string.about_known_schedule, list.title, list.source.note))
                    }
                    takes.forEach { list ->
                        HorizontalDivider(color = scheme.outlineVariant)
                        Source(list.source.name, stringResource(R.string.about_known_takes, list.title, list.source.note))
                    }
                }
                Text(
                    stringResource(R.string.about_reference),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }

            RowsCard {
                LinkRow(stringResource(R.string.privacy), stringResource(R.string.about_privacy_note), onOpenPrivacy)
                HorizontalDivider(color = scheme.outlineVariant)
                LinkRow(stringResource(R.string.about_source), stringResource(R.string.about_github)) {
                    if (!context.openWebPage(SOURCE_URL)) onMessage(res.getString(R.string.licences_no_browser, SOURCE_URL))
                }
                HorizontalDivider(color = scheme.outlineVariant)
                LinkRow(stringResource(R.string.licences_title), "", onOpenLicences)
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
        ScreenHeader(stringResource(R.string.privacy), onBack)
        Column(
            Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            listOf(
                stringResource(R.string.privacy_stays) to stringArrayResource(R.array.privacy_stays).toList(),
                stringResource(R.string.privacy_sent) to stringArrayResource(R.array.privacy_sent).toList(),
                stringResource(R.string.privacy_never) to stringArrayResource(R.array.privacy_never).toList(),
                stringResource(R.string.privacy_saved) to stringArrayResource(R.array.privacy_saved).toList(),
                stringResource(R.string.privacy_imported) to stringArrayResource(R.array.privacy_imported).toList(),
                stringResource(R.string.privacy_leaving) to stringArrayResource(R.array.privacy_leaving).toList(),
                stringResource(R.string.privacy_permissions) to stringArrayResource(R.array.privacy_permissions).toList(),
                stringResource(R.string.privacy_updates) to stringArrayResource(R.array.privacy_updates).toList(),
            ).forEach { (title, lines) ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    RowsCard {
                        Column(Modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            lines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface) }
                        }
                    }
                }
            }
        }
    }
}

/** The bundled torque lists and maintenance schedules, for crediting their sources on the About screen. */
@HiltViewModel
class KnownListsViewModel @Inject constructor(catalog: KnownSpecCatalog, scheduleCatalog: KnownScheduleCatalog, takesCatalog: KnownTakesCatalog) : ViewModel() {
    val takes: StateFlow<List<KnownTakesList>> = flow { emit(takesCatalog.lists()) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val lists: StateFlow<List<KnownSpecList>> = flow { emit(catalog.lists()) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val schedules: StateFlow<List<KnownScheduleList>> = flow { emit(scheduleCatalog.lists()) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
