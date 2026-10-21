package io.cosgriff.glovebox.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.CatalogRuleDao
import io.cosgriff.glovebox.data.db.CatalogRuleEntity
import io.cosgriff.glovebox.data.db.toRule
import io.cosgriff.glovebox.domain.CatalogRules
import io.cosgriff.glovebox.ui.components.DangerTextButton
import io.cosgriff.glovebox.ui.components.EmptyState
import io.cosgriff.glovebox.ui.components.RowsCard
import io.cosgriff.glovebox.ui.components.ScreenHeader
import io.cosgriff.glovebox.ui.history.catalogChoices
import io.cosgriff.glovebox.ui.history.lineWithCategory
import io.cosgriff.glovebox.ui.service.FileUnderDialog
import io.cosgriff.glovebox.ui.service.asNumber
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryRulesViewModel @Inject constructor(private val dao: CatalogRuleDao) : ViewModel() {
    /** Null while loading. By wording, ignoring case. */
    val rules: StateFlow<List<CatalogRuleEntity>?> = dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setLine(id: Long, catalogId: String) = viewModelScope.launch { dao.setLine(id, catalogId) }

    /** Changes no record: a rule only ever filed lines as they came in. */
    fun delete(id: Long) = viewModelScope.launch { dao.delete(id) }
}

/**
 * The user's category rules (#310): wording that files a line under a catalog line on import, kept from
 * sorting Custom lines on History. Each can be pointed at another line or deleted; neither touches a record.
 */
@Composable
fun CategoryRulesScreen(onBack: () -> Unit, vm: CategoryRulesViewModel = hiltViewModel()) {
    val rules by vm.rules.collectAsStateWithLifecycle()
    CategoryRulesContent(rules ?: return, onSetLine = vm::setLine, onDelete = vm::delete, onBack = onBack)
}

/** The rules as given, with no ViewModel: what [CategoryRulesScreen] shows, and what the screenshot tests draw. */
@Composable
internal fun CategoryRulesContent(
    rules: List<CatalogRuleEntity>,
    onSetLine: (id: Long, catalogId: String) -> Unit,
    onDelete: (id: Long) -> Unit,
    onBack: () -> Unit,
) {
    var changing by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleting by rememberSaveable { mutableStateOf<Long?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ScreenHeader(stringResource(R.string.rules_title), onBack, subtitle = stringResource(R.string.rules_about)) }
        if (rules.isEmpty()) {
            item { EmptyState(stringResource(R.string.rules_empty)) }
        } else {
            item {
                RowsCard(Modifier.padding(horizontal = 20.dp)) {
                    rules.forEachIndexed { i, rule ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        RuleRow(rule, onChange = { changing = rule.id }, onDelete = { deleting = rule.id })
                    }
                }
            }
        }
    }
    rules.firstOrNull { it.id == changing }?.let { rule ->
        FileUnderDialog(
            label = rule.text,
            lines = catalogChoices,
            onPick = { choice ->
                onSetLine(rule.id, choice.option.id)
                changing = null
            },
            onDismiss = { changing = null },
        )
    }
    rules.firstOrNull { it.id == deleting }?.let { rule ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.rules_delete_title)) },
            text = { Text(stringResource(R.string.rules_delete_body, rule.text)) },
            confirmButton = {
                DangerTextButton(onClick = {
                    onDelete(rule.id)
                    deleting = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun RuleRow(rule: CatalogRuleEntity, onChange: () -> Unit, onDelete: () -> Unit) {
    val known = CatalogRules.known(rule.toRule())
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(
            Modifier.weight(1f).heightIn(min = 56.dp).clickable(role = Role.Button, onClickLabel = stringResource(R.string.rules_change), onClick = onChange)
                .padding(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(rule.text, style = MaterialTheme.typography.bodyLarge)
            // A line a later version dropped: kept, shown, never applied, until it is pointed elsewhere or deleted.
            Text(
                if (known) lineWithCategory(rule.catalogId) else stringResource(R.string.rules_unknown_line),
                style = MaterialTheme.typography.bodySmall,
                color = if (known) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.rules_delete_for, rule.text))
        }
    }
}

/** Settings' row for the rules page: shown only while there are rules, since a row that leads nowhere is left out. */
@Composable
internal fun CategoryRulesRow(count: Int, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().heightIn(min = 60.dp).clickable(role = Role.Button, onClick = onOpen).padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // As the Import row above it is drawn.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.settings_rules), style = MaterialTheme.typography.bodyLarge)
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(20.dp))
        }
        Text(
            pluralStringResource(R.plurals.settings_rules_note, count, count.asNumber()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
