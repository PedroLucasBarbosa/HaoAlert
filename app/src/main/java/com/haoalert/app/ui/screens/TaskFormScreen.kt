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
import com.haoalert.app.domain.TaskDates
import com.haoalert.app.ui.components.DateCalendar
import com.haoalert.app.ui.HaoViewModel
import com.haoalert.app.ui.components.HaoTopBar
import com.haoalert.app.ui.components.SectionTitle
import com.haoalert.app.ui.util.Fmt
import java.time.LocalDate

/** Intervalos em segundos: 30 s, 1 min, 5, 10, 15, 20, 30 e 60 min. */
private val INTERVALS = listOf(30, 60, 300, 600, 900, 1200, 1800, 3600)

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
    var datesText by rememberSaveable { mutableStateOf(TaskDates.encode(listOf(LocalDate.now()))) }
    var hour by rememberSaveable { mutableIntStateOf(Fmt.defaultStartHour()) }
    var minute by rememberSaveable { mutableIntStateOf(0) }
    var interval by rememberSaveable { mutableIntStateOf(settings.defaultIntervalSeconds) }
    var enabled by rememberSaveable { mutableStateOf(true) }
    var visualAlert by rememberSaveable { mutableStateOf(true) }
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
                datesText = task.dates
                hour = task.hour
                minute = task.minute
                interval = task.intervalSeconds
                enabled = task.enabled
                visualAlert = task.visualAlert
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
    val selectedDates = TaskDates.parse(datesText)
    val daysError = showErrors && selectedDates.none { !it.isBefore(LocalDate.now()) }

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
            SectionTitle("DATAS")
            DateCalendar(
                selected = selectedDates,
                onToggle = { d ->
                    datesText = TaskDates.encode(if (d in selectedDates) selectedDates - d else selectedDates + d)
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { datesText = TaskDates.encode(listOf(LocalDate.now())) },
                    label = { Text("Só hoje") },
                )
                AssistChip(
                    onClick = {
                        val today = LocalDate.now()
                        datesText = TaskDates.encode((0L..6L).map { today.plusDays(it) })
                    },
                    label = { Text("Próximos 7 dias") },
                )
                AssistChip(onClick = { datesText = "" }, label = { Text("Limpar") })
            }
            Text(
                if (selectedDates.isEmpty()) "Toque nos dias do calendário para escolher."
                else "${selectedDates.size} dia(s) · ${Fmt.datesLabel(selectedDates)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (daysError) {
                Text(
                    "Selecione pelo menos uma data de hoje em diante",
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
                INTERVALS.forEach { seconds ->
                    FilterChip(
                        selected = interval == seconds,
                        onClick = { interval = seconds },
                        label = { Text(Fmt.intervalShort(seconds)) },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            VisualAlertRow(
                checked = visualAlert,
                globalEnabled = settings.visualAlertEnabled,
                onChange = { visualAlert = it },
            )

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
                    if (title.isBlank() || selectedDates.none { !it.isBefore(LocalDate.now()) }) {
                        showErrors = true
                        return@Button
                    }
                    val base = original ?: TaskEntity(title = "")
                    val task = base.copy(
                        title = title.trim(),
                        description = description.trim(),
                        dates = TaskDates.encode(selectedDates),
                        hour = hour,
                        minute = minute,
                        intervalSeconds = interval,
                        enabled = if (restore) true else enabled,
                        visualAlert = visualAlert,
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
private fun VisualAlertRow(checked: Boolean, globalEnabled: Boolean, onChange: (Boolean) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) { tick++ }
    val overlayGranted = remember(tick) { com.haoalert.app.notifications.PermissionHelper.overlayGranted(context) }

    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .6f), shape)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Alerta visual com GIF", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "Mostra o GIF em tela cheia, por cima de outros apps, a cada aviso",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = checked, onCheckedChange = onChange)
        }
        if (checked && !globalEnabled) {
            Text(
                "O alerta visual está desligado nas Configurações e não vai aparecer.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        if (checked && !overlayGranted) {
            Text(
                "Falta permitir \"Exibir sobre outros apps\". Sem isso, só a notificação aparece.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp),
            )
            TextButton(onClick = { com.haoalert.app.notifications.PermissionHelper.openOverlaySettings(context) }) {
                Text("Permitir agora")
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
