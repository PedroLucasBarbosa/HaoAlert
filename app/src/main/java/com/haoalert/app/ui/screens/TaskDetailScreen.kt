package com.haoalert.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.haoalert.app.data.TaskEntity
import com.haoalert.app.domain.ScheduleCalculator
import com.haoalert.app.ui.HaoViewModel
import com.haoalert.app.ui.components.ConfirmDialog
import com.haoalert.app.ui.components.HaoTopBar
import com.haoalert.app.ui.components.InfoLine
import com.haoalert.app.ui.components.rememberNow
import com.haoalert.app.ui.util.Fmt

@Composable
fun TaskDetailScreen(
    vm: HaoViewModel,
    taskId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onRestore: (Long) -> Unit,
) {
    val state by remember(taskId) { vm.observeTask(taskId) }.collectAsState(initial = null to false)
    val (task, loaded) = state
    val now = rememberNow()

    // Se a tarefa não existir mais (por exemplo, notificação antiga), volta sem travar a tela.
    var everFound by remember { mutableStateOf(false) }
    LaunchedEffect(task, loaded) {
        if (task != null) everFound = true else if (loaded && !everFound) onBack()
    }

    var confirmComplete by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HaoTopBar(if (task?.completed == true) "Tarefa concluída" else "Tarefa", onBack = onBack) },
    ) { padding ->
        if (task == null) return@Scaffold
        val awaiting = ScheduleCalculator.isAwaitingConfirmation(task, now)

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (awaiting) {
                Text(
                    "Esta tarefa está aguardando a sua confirmação. Os avisos continuam até você marcá-la como concluída.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = .6f))
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp))
                        .padding(14.dp),
                )
            }

            Text(task.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            if (task.completed) {
                CompletedInfo(task)
            }

            Card {
                Text(
                    task.description.ifBlank { "Sem descrição." },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (task.description.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                )
            }

            Card {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoLine(Icons.Rounded.CalendarMonth, Fmt.daysLabel(task.daysMask))
                    InfoLine(Icons.Rounded.Schedule, "${Fmt.time(task.hour, task.minute)} · ${Fmt.intervalLabel(task.intervalMinutes)}")
                    if (!task.completed) {
                        if (task.enabled) {
                            val next = ScheduleCalculator.next(task, now)
                            InfoLine(
                                Icons.Rounded.NotificationsActive,
                                if (next != null) "Próxima notificação: ${Fmt.nextLabel(next, now)}" else "Sem dias selecionados",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            InfoLine(Icons.Rounded.NotificationsOff, "Pausada")
                        }
                    }
                }
            }

            if (!task.completed) {
                Button(
                    onClick = { confirmComplete = true },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                ) {
                    Icon(Icons.Rounded.CheckCircle, null)
                    Spacer(Modifier.width(10.dp))
                    Text("Marcar como concluída", fontWeight = FontWeight.Bold)
                }
                ActionRow(
                    left = ActionSpec("Editar", Icons.Rounded.Edit) { onEdit(task.id) },
                    right = if (task.enabled) ActionSpec("Pausar", Icons.Rounded.Pause) { vm.setEnabled(task.id, false) }
                    else ActionSpec("Reativar", Icons.Rounded.PlayArrow) { vm.setEnabled(task.id, true) },
                )
                ActionRow(
                    left = ActionSpec("Duplicar", Icons.Rounded.ContentCopy) {
                        vm.duplicate(task.id) { newId -> if (newId != null) onEdit(newId) }
                    },
                    right = ActionSpec("Excluir", Icons.Rounded.Delete, destructive = true) { confirmDelete = true },
                )
            } else {
                Button(
                    onClick = { onRestore(task.id) },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Icon(Icons.Rounded.Restore, null)
                    Spacer(Modifier.width(10.dp))
                    Text("Restaurar para pendentes", fontWeight = FontWeight.Bold)
                }
                ActionRow(
                    left = ActionSpec("Renomear / editar", Icons.Rounded.Edit) { showRename = true },
                    right = ActionSpec("Excluir", Icons.Rounded.Delete, destructive = true) { confirmDelete = true },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmComplete && task != null) {
        ConfirmDialog(
            title = "Marcar como concluída?",
            text = "“${task.title}” vai para a lista de concluídas e os lembretes repetidos serão interrompidos agora.",
            confirmLabel = "Concluir",
            onConfirm = {
                vm.complete(task.id)
                confirmComplete = false
                onBack()
            },
            onDismiss = { confirmComplete = false },
        )
    }
    if (confirmDelete && task != null) {
        ConfirmDialog(
            title = "Excluir tarefa?",
            text = "“${task.title}” será apagada permanentemente" +
                (if (!task.completed) " e todos os seus alarmes serão cancelados." else ".") +
                " Esta ação não pode ser desfeita.",
            confirmLabel = "Excluir",
            destructive = true,
            onConfirm = {
                vm.delete(task.id)
                confirmDelete = false
                onBack()
            },
            onDismiss = { confirmDelete = false },
        )
    }
    if (showRename && task != null) {
        RenameDialog(
            task = task,
            onSave = { title, description ->
                vm.updateCompletedInfo(task.id, title, description)
                showRename = false
            },
            onDismiss = { showRename = false },
        )
    }
}

@Composable
private fun CompletedInfo(task: TaskEntity) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.CheckCircle, null, tint = scheme.secondary)
        Spacer(Modifier.width(8.dp))
        Column {
            task.completedAt?.let {
                Text("Concluída em ${Fmt.dateTime(it)}", style = MaterialTheme.typography.bodyMedium, color = scheme.secondary)
            }
            if (task.restoredCount > 0) {
                task.lastRestoredAt?.let {
                    Text(
                        "Já restaurada ${task.restoredCount}× · última em ${Fmt.dateTime(it)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .6f), shape)
            .padding(16.dp),
    ) { content() }
}

private data class ActionSpec(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
private fun ActionRow(left: ActionSpec, right: ActionSpec) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(left, right).forEach { spec ->
            val color = if (spec.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            OutlinedButton(
                onClick = spec.onClick,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f).height(50.dp),
            ) {
                Icon(spec.icon, null, tint = color)
                Spacer(Modifier.width(8.dp))
                Text(spec.label, color = color, maxLines = 1)
            }
        }
    }
}

@Composable
private fun RenameDialog(task: TaskEntity, onSave: (String, String) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renomear / editar") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição") },
                    minLines = 3,
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                Text(
                    "Editar uma tarefa concluída não reativa os lembretes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title.trim(), description.trim()) }, enabled = title.isNotBlank()) {
                Text("Salvar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
