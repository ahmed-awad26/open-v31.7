package com.opencontacts.feature.vaults

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opencontacts.core.model.VaultSummary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VaultsRoute(onBack: () -> Unit, viewModel: VaultsViewModel = hiltViewModel()) {
    val vaults by viewModel.vaults.collectAsStateWithLifecycle()
    val editing by viewModel.editingVault.collectAsStateWithLifecycle()
    val quickAdd by viewModel.quickAddContact.collectAsStateWithLifecycle()
    val activeVaultId by viewModel.activeVaultId.collectAsStateWithLifecycle()
    var selectedVaultId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDeleteVaultId by rememberSaveable { mutableStateOf<String?>(null) }

    val selectedVault = vaults.firstOrNull { it.id == selectedVaultId }
    val confirmDeleteVault = vaults.firstOrNull { it.id == confirmDeleteVaultId }
    val defaultVault = vaults.firstOrNull { it.isDefault }
    val activeVault = vaults.firstOrNull { it.id == activeVaultId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vault manager") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::startCreate,
                text = { Text("New vault") },
                icon = { Icon(Icons.Default.Add, null) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text("Vault control center", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Choose one default vault, open any vault without losing your place, and add contacts directly into any vault from here.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            VaultStat("Vaults", vaults.size.toString())
                            VaultStat("Default", defaultVault?.displayName ?: "—")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            VaultStat("Active", activeVault?.displayName ?: "—")
                            VaultStat("Locked", vaults.count { it.isLocked }.toString())
                        }
                    }
                }
            }

            if (selectedVault != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Selected vault", fontWeight = FontWeight.Bold)
                                Text(
                                    "${selectedVault.displayName} is selected. Delete requires one more explicit confirmation.",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                            FilledTonalButton(onClick = { confirmDeleteVaultId = selectedVault.id }) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                                Text("Delete")
                            }
                        }
                    }
                }
            }

            items(vaults, key = { it.id }) { vault ->
                VaultCard(
                    vault = vault,
                    isActive = vault.id == activeVaultId,
                    isSelected = vault.id == selectedVaultId,
                    onUse = { viewModel.activate(vault.id) },
                    onSetDefault = { viewModel.setDefaultVault(vault.id) },
                    onEdit = { viewModel.startRename(vault) },
                    onQuickAdd = { viewModel.startQuickAdd(vault) },
                    onLock = { viewModel.lockVault(vault.id) },
                    onLongSelect = {
                        selectedVaultId = if (selectedVaultId == vault.id) null else vault.id
                    },
                )
            }
        }

        editing?.let { editor ->
            VaultEditorDialog(
                state = editor,
                onStateChange = viewModel::updateEditor,
                onDismiss = viewModel::dismissEditor,
                onConfirm = viewModel::saveEditor,
            )
        }

        quickAdd?.let { state ->
            QuickAddContactDialog(
                state = state,
                onStateChange = viewModel::updateQuickAdd,
                onDismiss = viewModel::dismissQuickAdd,
                onConfirm = viewModel::saveQuickAdd,
            )
        }

        confirmDeleteVault?.let { vault ->
            AlertDialog(
                onDismissRequest = { confirmDeleteVaultId = null },
                title = { Text("Delete vault") },
                text = {
                    Text(
                        "Delete ${vault.displayName}? This removes its isolated data. If it was the last or default vault, a safe fallback vault will be recreated automatically.",
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteVault(vault.id)
                            if (selectedVaultId == vault.id) selectedVaultId = null
                            confirmDeleteVaultId = null
                        },
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDeleteVaultId = null }) { Text("Cancel") }
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VaultCard(
    vault: VaultSummary,
    isActive: Boolean,
    isSelected: Boolean,
    onUse: () -> Unit,
    onSetDefault: () -> Unit,
    onEdit: () -> Unit,
    onQuickAdd: () -> Unit,
    onLock: () -> Unit,
    onLongSelect: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onUse, onLongClick = onLongSelect)
            .padding(0.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    Text(vault.displayName, style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (vault.isLocked) "Locked vault • requires unlock when opened" else "Ready vault • can be used immediately",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (vault.isDefault) StatusChip("Default", Icons.Default.Star)
                        if (isActive) StatusChip("Active", Icons.Default.RadioButtonChecked)
                        StatusChip(if (vault.isLocked) "Locked" else "Unlocked", if (vault.isLocked) Icons.Default.Lock else Icons.Default.LockOpen)
                    }
                }
                Box(modifier = Modifier.padding(start = 12.dp)) {
                    Icon(
                        imageVector = if (vault.isLocked) Icons.Default.Lock else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (vault.isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniActionButton("Add contact", Icons.Default.PersonAdd, onQuickAdd)
                MiniActionButton("Edit", Icons.Default.Edit, onEdit)
                MiniActionButton("Default", Icons.Default.Star, onSetDefault)
                MiniActionButton("Lock", Icons.Default.Lock, onLock)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onUse) {
                    Icon(Icons.Default.RadioButtonChecked, contentDescription = null)
                    Text(if (isActive) "Opened" else "Use")
                }
                OutlinedButton(onClick = onSetDefault) {
                    Icon(Icons.Default.Star, contentDescription = null)
                    Text(if (vault.isDefault) "Default" else "Make default")
                }
            }

            Text(
                "Long press this card to select it for deletion, then confirm from the red panel above.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    AssistChip(
        onClick = {},
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
    )
}

@Composable
private fun MiniActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
        TextButton(onClick = onClick) {
            Icon(icon, contentDescription = null)
            Text(label)
        }
    }
}

@Composable
private fun VaultStat(label: String, value: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun VaultEditorDialog(
    state: VaultEditorState,
    onStateChange: (VaultEditorState) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (state.id == null) "Create vault" else "Edit vault") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.displayName,
                    onValueChange = { onStateChange(state.copy(displayName = it)) },
                    label = { Text("Vault name") },
                    singleLine = true,
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Set as default")
                        Text("Use this vault automatically on app start.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = state.makeDefault, onCheckedChange = { onStateChange(state.copy(makeDefault = it)) })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Use now")
                        Text("Open this vault right after saving.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = state.useAfterSave, onCheckedChange = { onStateChange(state.copy(useAfterSave = it)) })
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun QuickAddContactDialog(
    state: QuickAddContactState,
    onStateChange: (QuickAddContactState) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add contact to ${state.vaultName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.displayName,
                    onValueChange = { onStateChange(state.copy(displayName = it)) },
                    label = { Text("Name") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.primaryPhone,
                    onValueChange = { onStateChange(state.copy(primaryPhone = it)) },
                    label = { Text("Primary phone") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.additionalPhones,
                    onValueChange = { onStateChange(state.copy(additionalPhones = it)) },
                    label = { Text("More phones") },
                    supportingText = { Text("One phone per line") },
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Favorite")
                    Switch(checked = state.isFavorite, onCheckedChange = { onStateChange(state.copy(isFavorite = it)) })
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
