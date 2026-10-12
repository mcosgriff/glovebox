package io.cosgriff.glovebox.ui.about

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
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
        AboutHeader("Open-source licenses", onBack)
        if (groups == null) return@Column // first frame while the list is read

        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Glovebox is free software. You can read, change and share its source code under the ${Licences.APP_LICENCE_NAME}. " +
                            "It comes with no warranty.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    AboutCard { LinkRow("Glovebox", "GPL-3.0") { onOpenLicence(Licences.APP_LICENCE_ID) } }
                    Text(
                        "It is built with the libraries below. ${Licences.summary(groups)}, as built into this version.",
                        Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
            groups.forEach { group ->
                item(key = "licence:${group.licence.id}") {
                    Column(Modifier.padding(top = 14.dp)) {
                        AboutCard { LinkRow(group.name, "Read", onClick = { onOpenLicence(group.licence.id) }) }
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
            document = catalog.document(savedStateHandle.toRoute<LicenceTextRoute>().id)
            loading = false
        }
    }
}

/** The wording of one licence. */
@Composable
fun LicenceTextScreen(onBack: () -> Unit, onMessage: (String) -> Unit, vm: LicenceTextViewModel = hiltViewModel()) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val document = vm.document

    Column(Modifier.fillMaxSize()) {
        AboutHeader(document?.name ?: "License", onBack)
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
                                    if (!context.openWebPage(url)) onMessage("No browser to open $url")
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
                            if (document?.url != null) "This version doesn't carry the wording of this licence. It is published at the address above." else "This version doesn't carry the wording of this licence.",
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
