package com.haoalert.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.haoalert.app.haoApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Reconstrói os alarmes a partir do banco após reinicialização, atualização do app, mudança de
 * data/hora/fuso ou alteração da permissão de alarmes exatos. Como cada tarefa tem um único
 * alarme identificado pelo seu id, reagendar substitui o anterior e não duplica notificações.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.haoApp
        val pending = goAsync()
        scope.launch {
            try {
                app.scheduler.rescheduleAll(force = true)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
