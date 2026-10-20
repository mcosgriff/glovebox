package io.cosgriff.glovebox.ui.about

import io.cosgriff.glovebox.ui.components.ScreenHeader
import io.cosgriff.glovebox.ui.components.RowsCard
import androidx.compose.ui.platform.LocalResources
import io.cosgriff.glovebox.ui.settings.licencesSummary
import io.cosgriff.glovebox.ui.settings.title
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.ui.theme.PlexMono
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.cosgriff.glovebox.ui.route
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.domain.LicenceGroup
import io.cosgriff.glovebox.domain.Licences
import io.cosgriff.glovebox.ui.LicenceTextRoute
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LicencesViewModel @Inject constructor(catalog: LicenceCatalog) : ViewModel() {
    /** Null while the list is read. */
    var groups by mutableStateOf<List<LicenceGroup>?>(null)
        private set

    init {
        viewModelScope.launch { groups = catalog.groups() }
    }
}

/** Everything built into the app, under the licence it comes with. */
@Composable
fun LicencesScreen(onBack: () -> Unit, onOpenLicence: (id: String) -> Unit, vm: LicencesViewModel = hiltViewModel()) {
    val scheme = MaterialTheme.colorScheme
    val groups = vm.groups

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(stringResource(R.string.licences_title), onBack)
        if (groups == null) return@Column // first frame while the list is read

        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.licences_free_software, Licences.APP_LICENCE_NAME),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    RowsCard { LinkRow(stringResource(R.string.app_name), stringResource(R.string.licences_gpl)) { onOpenLicence(Licences.APP_LICENCE_ID) } }
                    Text(
                        stringResource(R.string.licences_built_with, licencesSummary(groups)),
                        Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
            groups.forEach { group ->
                item(key = "licence:${group.licence.id}") {
                    Column(Modifier.padding(top = 14.dp)) {
                        RowsCard { LinkRow(group.title, stringResource(R.string.licences_read), onClick = { onOpenLicence(group.licence.id) }) }
                    }
                }
                items(group.libraries, key = { "${group.licence.id}:${it.id}" }) { library ->
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Text(Licences.displayName(library), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            listOfNotNull(library.id, library.version).joinToString(" \u00b7 "),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = PlexMono,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@HiltViewModel
class LicenceTextViewModel @Inject constructor(savedStateHandle: SavedStateHandle, catalog: LicenceCatalog) : ViewModel() {
    var loading by mutableStateOf(true)
        private set
    var document by mutableStateOf<LicenceDocument?>(null)
        private set

    init {
        viewModelScope.launch {
            document = catalog.document(savedStateHandle.route<LicenceTextRoute>().id)
            loading = false
        }
    }
}

/** The wording of one licence. */
@Composable
fun LicenceTextScreen(onBack: () -> Unit, onMessage: (String) -> Unit, vm: LicenceTextViewModel = hiltViewModel()) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val res = LocalResources.current
    val document = vm.document

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(document?.name ?: stringResource(R.string.licences_licence), onBack)
        if (vm.loading) return@Column

        // A long licence is drawn a paragraph at a time.
        val paragraphs = remember(document) { document?.text?.let(Licences::paragraphs).orEmpty() }
        SelectionContainer {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                document?.url?.let { url ->
                    item {
                        Column {
                            Text(
                                url,
                                Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button) {
                                    if (!context.openWebPage(url)) onMessage(res.getString(R.string.licences_no_browser, url))
                                }.padding(vertical = 12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.primary,
                            )
                            HorizontalDivider(color = scheme.outlineVariant)
                        }
                    }
                }
                if (paragraphs.isEmpty()) {
                    item {
                        Text(
                            // Said plainly rather than showing an empty page.
                            stringResource(if (document?.url != null) R.string.licences_no_text_url else R.string.licences_no_text),
                            Modifier.semantics { heading() },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                items(paragraphs) { paragraph ->
                    Text(paragraph, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
