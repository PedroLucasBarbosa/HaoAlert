package com.haoalert.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.haoalert.app.haoApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Ação rápida "Adiar 10 min". Não conclui a tarefa: apenas reagenda o próximo aviso. */
class ActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SNOOZE) return
        val taskId = intent.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, -1L)
        if (taskId < 0) return
        val app = context.haoApp
        val pending = goAsync()
        scope.launch {
            try {
                app.repository.snooze(taskId, SNOOZE_MINUTES)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_SNOOZE = "com.haoalert.app.ACTION_SNOOZE"
        const val SNOOZE_MINUTES = 10
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
