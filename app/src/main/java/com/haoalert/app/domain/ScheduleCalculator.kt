package com.haoalert.app.domain

import com.haoalert.app.data.TaskEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

private const val MINUTE_MS = 60_000L

fun TaskEntity.runsOn(day: DayOfWeek): Boolean = (daysMask and (1 shl (day.value - 1))) != 0

/**
 * Calcula os horários das notificações.
 *
 * Em cada dia selecionado existe um "ciclo": a primeira notificação sai no horário inicial e as
 * seguintes a cada intervalo, até o fim do dia (ou até o usuário concluir a tarefa). No próximo
 * dia selecionado um novo ciclo começa sozinho.
 *
 * Os horários seguem sempre a mesma grade (início + n × intervalo), então recalcular após um
 * reinício do aparelho ou mudança de fuso nunca gera horários duplicados.
 */
object ScheduleCalculator {

    /** Próximo disparo estritamente depois de [after], ou null se não houver dias selecionados. */
    fun next(task: TaskEntity, after: Long, zone: ZoneId = ZoneId.systemDefault()): Long? {
        if (task.daysMask == 0) return null
        val interval = task.intervalMinutes.coerceAtLeast(1) * MINUTE_MS
        val today = Instant.ofEpochMilli(after).atZone(zone).toLocalDate()

        for (offset in 0..7) {
            val date = today.plusDays(offset.toLong())
            if (!task.runsOn(date.dayOfWeek)) continue

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
        if (task.completed || !task.enabled || task.daysMask == 0) return false
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        if (!task.runsOn(today.dayOfWeek)) return false
        val start = today.atTime(task.hour, task.minute).atZone(zone).toInstant().toEpochMilli()
        return now >= start
    }
}
