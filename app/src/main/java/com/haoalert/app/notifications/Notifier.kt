package com.haoalert.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.haoalert.app.MainActivity
import com.haoalert.app.R
import com.haoalert.app.data.SettingsStore
import com.haoalert.app.data.TaskEntity

class Notifier(
    private val context: Context,
    private val settings: SettingsStore,
) {
    /** Os canais do Android são imutáveis, então há um por combinação de som/vibração. */
    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        for (sound in listOf(true, false)) {
            for (vibration in listOf(true, false)) {
                val label = when {
                    sound && vibration -> "som e vibração"
                    sound -> "somente som"
                    vibration -> "somente vibração"
                    else -> "silencioso"
                }
                val channel = NotificationChannel(
                    channelId(sound, vibration),
                    "Lembretes ($label)",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Lembretes repetidos de tarefas do Hao Alert"
                    if (sound) {
                        setSound(
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build(),
                        )
                    } else {
                        setSound(null, null)
                    }
                    enableVibration(vibration)
                    if (vibration) vibrationPattern = longArrayOf(0, 350, 200, 350)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun channelId(sound: Boolean, vibration: Boolean) =
        "task_alerts_s${if (sound) 1 else 0}_v${if (vibration) 1 else 0}"

    fun show(task: TaskEntity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val s = settings.state.value
        if (!s.notificationsEnabled) return

        val notificationId = task.id.toInt()

        // Abrir a tarefa leva aos detalhes; a conclusão só acontece lá, com confirmação.
        val openIntent = PendingIntent.getActivity(
            context,
            notificationId * 2,
            Intent(context, MainActivity::class.java)
                .setData(Uri.parse("hao://open/${task.id}"))
                .putExtra(EXTRA_OPEN_TASK, task.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val snoozeIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 2 + 1,
            Intent(context, ActionReceiver::class.java)
                .setAction(ActionReceiver.ACTION_SNOOZE)
                .setData(Uri.parse("hao://snooze/${task.id}"))
                .putExtra(AlarmScheduler.EXTRA_TASK_ID, task.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val body = task.description.ifBlank { "Toque para abrir a tarefa e marcar como concluída." }

        val notification = NotificationCompat.Builder(context, channelId(s.sound, s.vibration))
            .setSmallIcon(R.drawable.ic_stat_hao)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.hao_logo))
            .setColor(0xFFE5383B.toInt())
            .setContentTitle(task.title)
            .setContentText(body)
            .setSubText("Hao Alert")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(false)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .addAction(0, "Abrir tarefa", openIntent)
            .addAction(0, "Adiar 10 min", snoozeIntent)
            .build()

        // Mesmo id por tarefa: cada nova notificação substitui a anterior, sem acumular duplicadas.
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    fun cancel(taskId: Long) {
        NotificationManagerCompat.from(context).cancel(taskId.toInt())
    }

    companion object {
        const val EXTRA_OPEN_TASK = "open_task_id"
    }
}
