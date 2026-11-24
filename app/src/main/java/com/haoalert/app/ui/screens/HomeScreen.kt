package com.haoalert.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.haoalert.app.data.TaskEntity
import com.haoalert.app.domain.ScheduleCalculator
import com.haoalert.app.notifications.PermissionHelper
import com.haoalert.app.ui.HaoViewModel
import com.haoalert.app.ui.components.CompletedTaskCard
import com.haoalert.app.ui.components.ConfirmDialog
import com.haoalert.app.ui.components.EmptyState
import com.haoalert.app.ui.components.HaoLogo
import com.haoalert.app.ui.components.PendingTaskCard
import com.haoalert.app.ui.components.SectionTitle
import com.haoalert.app.ui.components.StatCard
import com.haoalert.app.ui.components.rememberNow
import com.haoalert.app.ui.util.Fmt

@Composable
fun HomeScreen(
    vm: HaoViewModel,
    onNewTask: () -> Unit,
    onOpenTask: (Long) -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onRequestNotifications: () -> Unit,
) {
    val context = LocalContext.current
    val pending by vm.pending.collectAsState()
    val completed by vm.completed.collectAsState()
    val now = rememberNow()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var selection by remember { mutableStateOf(setOf<Long>()) }
    var confirmDeleteSelected by remember { mutableStateOf(false) }
    var confirmClearAll by remember { mutableStateOf(false) }

    // Revalida permissões toda vez que o usuário volta das configurações do Android.
    var permissionTick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { permissionTick++ }
    val permissions = remember(permissionTick) { PermissionHelper.status(context) }

    val pendingList = pending.orEmpty()
    val completedList = completed.orEmpty()
    val selecting = selection.isNotEmpty()

    // Limpa seleção de itens que deixaram de existir ou ao trocar de aba.
    LaunchedEffect(completedList, tab) {
        if (tab != 1) selection = emptySet()
        else selection = selection.intersect(completedList.map { it.id }.toSet())
    }

    val upcoming = remember(pendingList, now / 60_000) {
        pendingList
            .filter { it.enabled }
            .mapNotNull { task -> ScheduleCalculator.next(task, now)?.let { task to it } }
            .sortedBy { it.second }
            .take(3)
    }
    val awaitingCount = remember(pendingList, now / 60_000) {
        pendingList.count { ScheduleCalculator.isAwaitingConfirmation(it, now) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            AnimatedVisibility(visible = !selecting) {
                ExtendedFloatingActionButton(
                    onClick = onNewTask,
                    icon = { Icon(Icons.Rounded.Add, null) },
                    text = { Text("Nova tarefa", fontWeight = FontWeight.SemiBold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 104.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Cabeçalho
            item {
                if (selecting) {
                    SelectionBar(
                        count = selection.size,
                        onClose = { selection = emptySet() },
                        onSelectAll = { selection = completedList.map { it.id }.toSet() },
                        onDelete = { confirmDeleteSelected = true },
                    )
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        HaoLogo()
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = onHistory) { Icon(Icons.Rounded.History, "Histórico") }
                        IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, "Configurações") }
                    }
                }
            }

            item {
                Column(Modifier.padding(top = 8.dp)) {
                    Text(
                        Fmt.greeting(now),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        Fmt.headerDate(now),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (!permissions.allCriticalGranted) {
                item {
                    PermissionBanner(
                        notificationsMissing = !permissions.notifications,
                        exactAlarmsMissing = !permissions.exactAlarms,
                        onFixNotifications = onRequestNotifications,
                        onFixExactAlarms = { PermissionHelper.openExactAlarmSettings(context) },
                    )
                }
            }

            // Resumo
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Pendentes", pendingList.size, Modifier.weight(1f), highlight = awaitingCount > 0)
                    StatCard("Concluídas", completedList.size, Modifier.weight(1f))
                }
            }

            // Próximas notificações
            item { SectionTitle("PRÓXIMAS NOTIFICAÇÕES", Modifier.padding(top = 6.dp)) }
            item {
                if (upcoming.isEmpty()) {
                    Text(
                        "Nenhuma notificação agendada.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    UpcomingList(upcoming, now, onOpenTask)
                }
            }

            // Abas
            item {
                Spacer(Modifier.height(4.dp))
                CategoryTabs(
                    selected = tab,
                    pendingCount = pendingList.size,
                    completedCount = completedList.size,
                    onSelect = { tab = it },
                )
            }

            if (tab == 0) {
                if (pending != null && pendingList.isEmpty()) {
                    item {
                        EmptyState(
                            Icons.Rounded.NotificationsNone,
                            "Nenhuma tarefa pendente",
                            "Toque em “Nova tarefa”, escolha os dias e o horário e deixe o Hao Alert lembrar você.",
                        )
                    }
                }
                items(pendingList, key = { "p${it.id}" }) { task ->
                    PendingTaskCard(
                        task = task,
                        now = now,
                        onClick = { onOpenTask(task.id) },
                        onToggle = { vm.setEnabled(task.id, it) },
                    )
                }
            } else {
                if (completedList.isNotEmpty() && !selecting) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { confirmClearAll = true }) {
                                Icon(Icons.Rounded.DeleteSweep, null, modifier = Modifier.padding(end = 6.dp))
                                Text("Limpar histórico")
                            }
                        }
                    }
                }
                if (completed != null && completedList.isEmpty()) {
                    item {
                        EmptyState(
                            Icons.Rounded.Checklist,
                            "Nada concluído ainda",
                            "As tarefas aparecem aqui depois que você abre uma notificação e toca em “Marcar como concluída”.",
                        )
                    }
                }
                items(completedList, key = { "c${it.id}" }) { task ->
                    CompletedTaskCard(
                        task = task,
                        selecting = selecting,
                        selected = task.id in selection,
                        onClick = {
                            if (selecting) selection = selection.toggle(task.id) else onOpenTask(task.id)
                        },
                        onLongClick = { selection = selection.toggle(task.id) },
                    )
                }
            }
        }
    }

    if (confirmDeleteSelected) {
        ConfirmDialog(
            title = "Excluir ${selection.size} tarefa(s)?",
            text = "As tarefas selecionadas serão apagadas permanentemente. Esta ação não pode ser desfeita.",
            confirmLabel = "Excluir",
            destructive = true,
            onConfirm = {
                vm.deleteCompleted(selection.toList())
                selection = emptySet()
                confirmDeleteSelected = false
            },
            onDismiss = { confirmDeleteSelected = false },
        )
    }
    if (confirmClearAll) {
        ConfirmDialog(
            title = "Limpar todo o histórico?",
            text = "Todas as ${completedList.size} tarefas concluídas serão apagadas permanentemente. Esta ação não pode ser desfeita.",
            confirmLabel = "Limpar tudo",
            destructive = true,
            onConfirm = {
                vm.clearCompleted()
                confirmClearAll = false
            },
            onDismiss = { confirmClearAll = false },
        )
    }
}

private fun Set<Long>.toggle(id: Long): Set<Long> = if (id in this) this - id else this + id

@Composable
private fun SelectionBar(count: Int, onClose: () -> Unit, onSelectAll: () -> Unit, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Cancelar seleção") }
        Text(
            "$count selecionada(s)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onSelectAll) { Icon(Icons.Rounded.DoneAll, "Selecionar todas") }
        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.Delete, "Excluir selecionadas", tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun UpcomingList(items: List<Pair<TaskEntity, Long>>, now: Long, onOpen: (Long) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(scheme.surface)
            .border(1.dp, scheme.outline.copy(alpha = .6f), shape),
    ) {
        items.forEachIndexed { index, (task, at) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(task.id) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    Fmt.nextLabel(at, now),
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(110.dp),
                )
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            if (index < items.lastIndex) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(scheme.outline.copy(alpha = .4f)))
            }
        }
    }
}

@Composable
private fun CategoryTabs(selected: Int, pendingCount: Int, completedCount: Int, onSelect: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(scheme.surface)
            .border(1.dp, scheme.outline.copy(alpha = .6f), shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf("Pendentes" to pendingCount, "Concluídas" to completedCount).forEachIndexed { index, (label, count) ->
            val active = selected == index
            val bg by animateColorAsState(if (active) scheme.primary else scheme.surface, label = "tabBg")
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .clickable { onSelect(index) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$label · $count",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (active) scheme.onPrimary else scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PermissionBanner(
    notificationsMissing: Boolean,
    exactAlarmsMissing: Boolean,
    onFixNotifications: () -> Unit,
    onFixExactAlarms: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(scheme.primaryContainer.copy(alpha = .5f).compositeOver(scheme.surface))
            .border(1.dp, scheme.primary.copy(alpha = .7f), shape)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.WarningAmber, null, tint = scheme.primary)
            Spacer(Modifier.width(10.dp))
            Text("Ajuste necessário", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        if (notificationsMissing) {
            Text(
                "As notificações estão desativadas. Sem elas o Hao Alert não consegue avisar você.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onFixNotifications, modifier = Modifier.padding(top = 8.dp)) { Text("Ativar notificações") }
        }
        if (exactAlarmsMissing) {
            if (notificationsMissing) Spacer(Modifier.height(12.dp))
            Text(
                "A permissão de alarmes e lembretes está desativada. Os avisos podem atrasar vários minutos.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onFixExactAlarms, modifier = Modifier.padding(top = 8.dp)) { Text("Permitir alarmes exatos") }
        }
    }
}
