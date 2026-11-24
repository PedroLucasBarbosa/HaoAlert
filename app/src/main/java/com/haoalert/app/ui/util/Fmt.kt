package com.haoalert.app.ui.util

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object Fmt {
    private val locale: Locale = Locale.forLanguageTag("pt-BR")

    val dayShort = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")

    const val WEEKDAYS = 31      // seg a sex
    const val WEEKEND = 96       // sáb e dom
    const val ALL_DAYS = 127

    fun time(hour: Int, minute: Int): String = String.format(locale, "%02d:%02d", hour, minute)

    fun daysLabel(mask: Int): String = when (mask) {
        ALL_DAYS -> "Todos os dias"
        WEEKDAYS -> "Segunda a sexta"
        WEEKEND -> "Fim de semana"
        0 -> "Nenhum dia selecionado"
        else -> dayShort.filterIndexed { i, _ -> (mask and (1 shl i)) != 0 }.joinToString(", ")
    }

    fun intervalLabel(minutes: Int): String = if (minutes == 60) "a cada 1 h" else "a cada $minutes min"

    private val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy", locale)
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", locale)
    private val headerDateFmt = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", locale)

    private fun zoned(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())

    fun date(ms: Long): String = dateFmt.format(zoned(ms))
    fun clock(ms: Long): String = timeFmt.format(zoned(ms))
    fun dateTime(ms: Long): String = "${date(ms)} às ${clock(ms)}"

    fun headerDate(ms: Long): String =
        headerDateFmt.format(zoned(ms)).replaceFirstChar { it.titlecase(locale) }

    fun greeting(ms: Long): String {
        val hour = zoned(ms).hour
        return when {
            hour < 5 -> "Boa madrugada"
            hour < 12 -> "Bom dia"
            hour < 18 -> "Boa tarde"
            else -> "Boa noite"
        }
    }

    /** "Hoje 09:10", "Amanhã 09:00" ou "Qua 09:00". */
    fun nextLabel(triggerAt: Long, now: Long): String {
        val trigger = zoned(triggerAt)
        val today = zoned(now).toLocalDate()
        val clock = time(trigger.hour, trigger.minute)
        return when (ChronoUnit.DAYS.between(today, trigger.toLocalDate())) {
            0L -> "Hoje $clock"
            1L -> "Amanhã $clock"
            else -> "${dayShort[trigger.dayOfWeek.value - 1]} $clock"
        }
    }

    fun defaultStartHour(): Int = LocalTime.now().plusHours(1).hour
}
