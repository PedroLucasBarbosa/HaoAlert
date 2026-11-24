package com.haoalert.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.haoalert.app.data.TaskDao
import com.haoalert.app.data.TaskEntity
import com.haoalert.app.domain.ScheduleCalculator

/**
 * Agenda UM alarme por tarefa. O PendingIntent é identificado pelo id da tarefa (requestCode +
 * data URI), então reagendar substitui o alarme anterior e nunca gera alarmes duplicados.
 */
class AlarmScheduler(
    private val context: Context,
    private val dao: TaskDao,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    private fun intentFor(taskId: Long, triggerAt: Long = 0L): Intent =
        Intent(context, AlarmReceiver::class.java)
            .setAction(ACTION_FIRE)
            .setData(Uri.parse("hao://alarm/$taskId"))
            .putExtra(EXTRA_TASK_ID, taskId)
            .putExtra(EXTRA_TRIGGER_AT, triggerAt)

    private fun pendingIntent(taskId: Long, triggerAt: Long = 0L, flags: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intentFor(taskId, triggerAt),
            flags or PendingIntent.FLAG_IMMUTABLE,
        )

    fun hasAlarm(taskId: Long): Boolean =
        pendingIntent(taskId, flags = PendingIntent.FLAG_NO_CREATE) != null

    /** Calcula o próximo horário da tarefa e agenda. Cancela se estiver concluída ou pausada. */
    fun schedule(task: TaskEntity, now: Long = System.currentTimeMillis()) {
        if (task.completed || !task.enabled) {
            cancel(task.id)
            return
        }
        val next = ScheduleCalculator.next(task, now)
        if (next == null) cancel(task.id) else setAt(task.id, next)
    }

    /** Agenda um disparo em um horário específico (usado também pelo "adiar"). */
    fun setAt(taskId: Long, triggerAt: Long) {
        val pi = pendingIntent(taskId, triggerAt, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            // Sem a permissão de alarmes exatos o Android pode atrasar o disparo.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun cancel(taskId: Long) {
        val pi = pendingIntent(taskId, flags = PendingIntent.FLAG_NO_CREATE) ?: return
        alarmManager.cancel(pi)
        pi.cancel()
    }

    /**
     * Reconstrói os agendamentos a partir do estado real salvo no banco.
     * @param force true após reinício/mudança de hora: recalcula todos.
     *              false: só cria o alarme das tarefas que estiverem sem alarme.
     */
    suspend fun rescheduleAll(force: Boolean) {
        val now = System.currentTimeMillis()
        dao.getActive().forEach { task ->
            if (force || !hasAlarm(task.id)) schedule(task, now)
        }
    }

    companion object {
        const val ACTION_FIRE = "com.haoalert.app.ACTION_FIRE"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TRIGGER_AT = "trigger_at"
    }
}
