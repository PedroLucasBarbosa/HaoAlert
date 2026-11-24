package com.haoalert.app.domain

import com.haoalert.app.data.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Conversão entre o texto salvo no banco e o conjunto de datas escolhidas no calendário. */
object TaskDates {
    fun parse(text: String): Set<LocalDate> =
        text.split(',')
            .mapNotNull { runCatching { LocalDate.parse(it.trim()) }.getOrNull() }
            .toSet()

    fun encode(dates: Collection<LocalDate>): String = dates.sorted().joinToString(",")
}

fun TaskEntity.dateSet(): Set<LocalDate> = TaskDates.parse(dates)

/**
 * Calcula os horários das notificações.
 *
 * Em cada data escolhida existe um "ciclo": a primeira notificação sai no horário inicial e as
 * seguintes a cada intervalo, até o fim do dia (ou até o usuário concluir a tarefa). Quando o dia
 * seguinte escolhido chega, um novo ciclo começa sozinho.
 *
 * Os horários seguem sempre a mesma grade (início + n × intervalo), então recalcular após um
 * reinício do aparelho ou mudança de fuso nunca gera horários duplicados.
 */
object ScheduleCalculator {

    /** Próximo disparo estritamente depois de [after], ou null se não houver datas pela frente. */
    fun next(task: TaskEntity, after: Long, zone: ZoneId = ZoneId.systemDefault()): Long? {
        val dates = task.dateSet()
        if (dates.isEmpty()) return null
        val interval = task.intervalSeconds.coerceAtLeast(1) * 1_000L
        val today = Instant.ofEpochMilli(after).atZone(zone).toLocalDate()

        for (date in dates.filter { !it.isBefore(today) }.sorted()) {
            val start = date.atTime(task.hour, task.minute).atZone(zone).toInstant().toEpochMilli()
            if (start > after) return start

            // O ciclo deste dia já começou: procura o próximo degrau da grade ainda dentro do dia.
            val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val steps = (after - start) / interval + 1
            val candidate = start + steps * interval
            if (candidate < dayEnd) return candidate
        }
        return null
    }

    /** Verdadeiro quando o ciclo de hoje já começou e a tarefa segue pendente de confirmação. */
    fun isAwaitingConfirmation(task: TaskEntity, now: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        if (task.completed || !task.enabled) return false
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        if (today !in task.dateSet()) return false
        val start = today.atTime(task.hour, task.minute).atZone(zone).toInstant().toEpochMilli()
        return now >= start
    }
}
