package com.haoalert.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")
private val MONTH_FMT = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", PT_BR)
private val WEEK_HEADERS = listOf("D", "S", "T", "Q", "Q", "S", "S") // domingo primeiro

/**
 * Calendário mensal: toque em um dia para marcar/desmarcar. Dias anteriores a hoje ficam bloqueados.
 * [selected] é o conjunto de datas em que a tarefa deve notificar.
 */
@Composable
fun DateCalendar(
    selected: Set<LocalDate>,
    onToggle: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val today = LocalDate.now()

    // O mês exibido começa na primeira data marcada (ou no mês atual) e sobrevive à rotação da tela.
    var monthText by rememberSaveable {
        mutableStateOf(
            YearMonth.from(selected.filter { !it.isBefore(today) }.minOrNull() ?: today).toString()
        )
    }
    val month = YearMonth.parse(monthText)
    val shape = RoundedCornerShape(22.dp)

    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(scheme.surface)
            .border(1.dp, scheme.outline.copy(alpha = .6f), shape)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { monthText = month.minusMonths(1).toString() },
                enabled = month.isAfter(YearMonth.from(today)),
            ) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Mês anterior") }
            Text(
                MONTH_FMT.format(month.atDay(1)).replaceFirstChar { it.titlecase(PT_BR) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { monthText = month.plusMonths(1).toString() }) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Próximo mês")
            }
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            WEEK_HEADERS.forEach {
                Text(
                    it,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }

        // Domingo = coluna 0.
        val leadingBlanks = month.atDay(1).dayOfWeek.value % 7
        val cells: List<LocalDate?> =
            List(leadingBlanks) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
        val rows = cells.chunked(7)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            rows.forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (col in 0 until 7) {
                        val date = week.getOrNull(col)
                        Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                            if (date != null) DayCell(date, date in selected, date.isBefore(today), date == today, onToggle)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    selected: Boolean,
    past: Boolean,
    isToday: Boolean,
    onToggle: (LocalDate) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (selected) scheme.primary else androidx.compose.ui.graphics.Color.Transparent, label = "dayBg")
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(bg)
            .then(if (isToday && !selected) Modifier.border(1.5.dp, scheme.primary, CircleShape) else Modifier)
            .clickable(enabled = !past || selected) { onToggle(date) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            date.dayOfMonth.toString(),
            fontSize = 14.sp,
            fontWeight = if (selected || isToday) FontWeight.Bold else FontWeight.Normal,
            color = when {
                selected -> scheme.onPrimary
                past -> scheme.onSurfaceVariant.copy(alpha = .35f)
                else -> scheme.onSurface
            },
        )
    }
}
