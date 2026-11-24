package com.haoalert.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.haoalert.app.haoApp

/**
 * Tarefa auxiliar (sem horário exato): de tempos em tempos confere se toda tarefa ativa ainda
 * tem um alarme registrado e recria os que estiverem faltando. Os avisos em si são sempre
 * disparados pelo AlarmManager.
 */
class SafetyNetWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        applicationContext.haoApp.scheduler.rescheduleAll(force = false)
        return Result.success()
    }
}
