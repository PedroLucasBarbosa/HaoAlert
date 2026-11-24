package com.haoalert.app

import android.app.Application
import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.haoalert.app.data.HaoDatabase
import com.haoalert.app.data.SettingsStore
import com.haoalert.app.data.TaskRepository
import com.haoalert.app.notifications.AlarmScheduler
import com.haoalert.app.notifications.Notifier
import com.haoalert.app.notifications.SafetyNetWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class HaoApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: HaoDatabase by lazy { HaoDatabase.create(this) }
    val settings: SettingsStore by lazy { SettingsStore(this) }
    val notifier: Notifier by lazy { Notifier(this, settings) }
    val scheduler: AlarmScheduler by lazy { AlarmScheduler(this, database.taskDao()) }
    val repository: TaskRepository by lazy { TaskRepository(database.taskDao(), scheduler, notifier) }

    override fun onCreate() {
        super.onCreate()
        notifier.ensureChannels()

        // Garante que toda tarefa ativa tenha alarme (sem sobrescrever adiamentos em andamento).
        appScope.launch { scheduler.rescheduleAll(force = false) }

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "alarm_safety_net",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SafetyNetWorker>(12, TimeUnit.HOURS).build(),
        )
    }
}

val Context.haoApp: HaoApp get() = applicationContext as HaoApp
