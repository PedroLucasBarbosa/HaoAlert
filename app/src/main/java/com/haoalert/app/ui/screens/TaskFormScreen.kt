package com.haoalert.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haoalert.app.data.TaskEntity
import com.haoalert.app.ui.HaoViewModel
import com.haoalert.app.ui.components.HaoTopBar
import com.haoalert.app.ui.components.SectionTitle
import com.haoalert.app.ui.util.Fmt
import java.time.LocalDate

private val INTERVALS = listOf(5, 10, 15, 20, 30, 60)

/**
 * Criação, edição e restauração de tarefas.
 * [taskId] < 0 cria uma nova tarefa; [restore] devolve uma concluída para as pendentes.
 */
@Composable
fun TaskFormScreen(
    vm: HaoViewModel,
    taskId: Long,
    restore: Boolean,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    val settings by vm.settings.collectAsState()
    val isNew = taskId < 0

    var original by remember { mutableStateOf<TaskEntity?>(null) }
    var initialized by rememberSaveable { mutableStateOf(false) }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var daysMask by rememberSaveable { mutableIntStateOf(1 shl (LocalDate.now().dayOfWeek.value - 1)) }
    var hour by rememberSaveable { mutableIntStateOf(Fmt.defaultStartHour()) }
    var minute by rememberSaveable { mutableIntStateOf(0) }
    var interval by rememberSaveable { mutableIntStateOf(settings.defaultInterval) }
    var enabled by rememberSaveable { mutableStateOf(true) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }

    LaunchedEffect(taskId) {
        if (!isNew) {
            val task = vm.getTask(taskId)
            if (task == null) {
                onBack()
                return@LaunchedEffect
            }
            original = task
            if (!initialized) {
                title = task.title
                description = task.description
                daysMask = task.daysMask
                hour = task.hour
                minute = task.minute
                interval = task.intervalMinutes
                enabled = task.enabled
            }
        }
        initialized = true
    }

    val screenTitle = when {
        restore -> "Restaurar tarefa"
        isNew -> "Nova tarefa"
        else -> "Editar tarefa"
    }
    val titleError = showErrors && title.isBlank()
    val daysError = showErrors && daysMask == 0

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HaoTopBar(screenTitle, onBack = onBack) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (restore) {
                Text(
                    "Revise os dias e o horário. Ao restaurar, os lembretes voltam a ser agendados.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título") },
                singleLine = true,
                isError = titleError,
                supportingText = if (titleError) ({ Text("Informe um título") }) else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descrição") },
                minLines = 3,
                maxLines = 6,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))
            SectionTitle("DIAS DA SEMANA")
            DaySelector(daysMask) { daysMask = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { daysMask = Fmt.WEEKDAYS }, label = { Text("Dias úteis") })
                AssistChip(onClick = { daysMask = Fmt.ALL_DAYS }, label = { Text("Todos") })
                AssistChip(onClick = { daysMask = Fmt.WEEKEND }, label = { Text("Fim de semana") })
            }
            if (daysError) {
                Text(
                    "Selecione pelo menos um dia",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(8.dp))
            SectionTitle("HORÁRIO INICIAL")
            TimeField(Fmt.time(hour, minute)) { showTimePicker = true }

            Spacer(Modifier.height(8.dp))
            SectionTitle("REPETIR A CADA")
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                INTERVALS.forEach { minutes ->
                    FilterChip(
                        selected = interval == minutes,
                        onClick = { interval = minutes },
                        label = { Text(if (minutes == 60) "60 min" else "$minutes min") },
                    )
                }
            }

            if (!restore) {
                Spacer(Modifier.height(8.dp))
                val shape = RoundedCornerShape(16.dp)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .6f), shape)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Tarefa ativa", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (enabled) "Os lembretes serão enviados" else "Pausada: nenhum lembrete será enviado",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                "Primeiro aviso às ${Fmt.time(hour, minute)}, depois ${Fmt.intervalLabel(interval)} " +
                    "até você abrir a tarefa e marcar como concluída.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (title.isBlank() || daysMask == 0) {
                        showErrors = true
                        return@Button
                    }
                    val base = original ?: TaskEntity(title = "")
                    val task = base.copy(
                        title = title.trim(),
                        description = description.trim(),
                        daysMask = daysMask,
                        hour = hour,
                        minute = minute,
                        intervalMinutes = interval,
                        enabled = if (restore) true else enabled,
                    )
                    if (restore) vm.restore(task, onDone) else vm.save(task, onDone)
                },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                Icon(if (restore) Icons.Rounded.Restore else Icons.Rounded.Check, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (restore) "Restaurar e reativar lembretes" else "Salvar",
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showTimePicker) {
        HaoTimePickerDialog(
            initialHour = hour,
            initialMinute = minute,
            onConfirm = { h, m ->
                hour = h
                minute = m
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

@Composable
private fun DaySelector(mask: Int, onChange: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Fmt.dayShort.forEachIndexed { index, label ->
            val selected = (mask and (1 shl index)) != 0
            val bg by animateColorAsState(if (selected) scheme.primary else scheme.surfaceVariant, label = "dayBg")
            Box(
                Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(bg)
                    .clickable { onChange(mask xor (1 shl index)) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    fontSize = 11.sp,
                    maxLines = 1,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TimeField(value: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .6f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Schedule, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Text("Alterar", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HaoTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        text = { TimePicker(state = state) },
    )
}
