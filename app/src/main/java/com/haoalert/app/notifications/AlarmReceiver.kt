package com.haoalert.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.haoalert.app.haoApp
import com.haoalert.app.overlay.OverlayService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Recebe o alarme de uma tarefa (inclusive com o app fechado), agenda o próximo e notifica. */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, -1L)
        if (taskId < 0) return
        val firedFor = intent.getLongExtra(AlarmScheduler.EXTRA_TRIGGER_AT, 0L)
        val app = context.haoApp
        val pending = goAsync()

        scope.launch {
            try {
                val task = app.database.taskDao().get(taskId)
                if (task == null || task.completed || !task.enabled) {
                    app.scheduler.cancel(taskId)
                    return@launch
                }
                // Agenda o próximo ANTES de notificar: a repetição continua mesmo que a
                // notificação seja ignorada ou dispensada. Usar o maior entre "agora" e o
                // horário previsto evita repetir o mesmo degrau se o alarme chegar adiantado.
                val base = maxOf(System.currentTimeMillis(), firedFor)
                app.scheduler.schedule(task, base)
                app.notifier.show(task)

                // Alerta visual: chave geral + opção da tarefa + permissão "Exibir sobre outros apps".
                val settings = app.settings.state.value
                if (settings.notificationsEnabled && settings.visualAlertEnabled && task.visualAlert) {
                    OverlayService.show(context.applicationContext, task.id, task.title)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
