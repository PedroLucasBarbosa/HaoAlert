package com.haoalert.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.haoalert.app.data.TaskEventEntity
import com.haoalert.app.ui.HaoViewModel
import com.haoalert.app.ui.components.EmptyState
import com.haoalert.app.ui.components.HaoTopBar
import com.haoalert.app.ui.components.SectionTitle
import com.haoalert.app.ui.util.Fmt
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private enum class StatusFilter(val label: String) {
    ALL("Todas"), COMPLETED("Concluídas"), RESTORED("Restauradas")
}

private enum class PeriodFilter(val label: String, val days: Long?) {
    ALL("Tudo", null), TODAY("Hoje", 0), WEEK("7 dias", 6), MONTH("30 dias", 29)
}

@Composable
fun HistoryScreen(vm: HaoViewModel, onBack: () -> Unit, onOpenTask: (Long) -> Unit) {
    val events by vm.events.collectAsState()
    var status by rememberSaveable { mutableStateOf(StatusFilter.ALL) }
    var period by rememberSaveable { mutableStateOf(PeriodFilter.ALL) }

    val filtered = remember(events, status, period) {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        events.filter { event ->
            val statusOk = when (status) {
                StatusFilter.ALL -> true
                StatusFilter.COMPLETED -> event.type == TaskEventEntity.TYPE_COMPLETED
                StatusFilter.RESTORED -> event.type == TaskEventEntity.TYPE_RESTORED
            }
            val periodOk = period.days?.let { days ->
                val date = Instant.ofEpochMilli(event.at).atZone(zone).toLocalDate()
                !date.isBefore(today.minusDays(days))
            } ?: true
            statusOk && periodOk
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HaoTopBar("Histórico", onBack = onBack) },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("SITUAÇÃO")
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusFilter.entries.forEach {
                            FilterChip(selected = status == it, onClick = { status = it }, label = { Text(it.label) })
                        }
                    }
                    SectionTitle("PERÍODO", Modifier.padding(top = 4.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PeriodFilter.entries.forEach {
                            FilterChip(selected = period == it, onClick = { period = it }, label = { Text(it.label) })
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Rounded.History,
                        if (events.isEmpty()) "Histórico vazio" else "Nada neste filtro",
                        if (events.isEmpty()) "Conclusões e restaurações de tarefas aparecem aqui."
                        else "Tente outro período ou outra situação.",
                    )
                }
            }

            items(filtered, key = { it.id }) { event ->
                HistoryRow(event) { onOpenTask(event.taskId) }
            }
        }
    }
}

@Composable
private fun HistoryRow(event: TaskEventEntity, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    val restored = event.type == TaskEventEntity.TYPE_RESTORED
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(scheme.surface)
            .border(1.dp, scheme.outline.copy(alpha = .6f), shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (restored) Icons.Rounded.Restore else Icons.Rounded.CheckCircle,
            null,
            tint = if (restored) scheme.primary else scheme.secondary,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                event.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                (if (restored) "Restaurada em " else "Concluída em ") + Fmt.dateTime(event.at),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}
