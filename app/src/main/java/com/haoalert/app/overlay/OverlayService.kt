package com.haoalert.app.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.haoalert.app.MainActivity
import com.haoalert.app.R
import com.haoalert.app.notifications.Notifier

/**
 * Alerta Visual: sobreposição em tela cheia (TYPE_APPLICATION_OVERLAY) com o GIF e o título da tarefa.
 *
 * - Só existe UMA sobreposição por vez. Um novo disparo substitui a anterior (nada de duplicadas
 *   nem de vários GIFs tocando ao mesmo tempo).
 * - Tocar abre a tarefa; o "✕" apenas fecha o alerta, e a tarefa continua pendente com as
 *   repetições ativas (o próximo alarme já está agendado pelo AlarmReceiver).
 * - É um serviço em primeiro plano de curta duração: termina assim que a sobreposição some.
 */
class OverlayService : Service() {

    private val main = Handler(Looper.getMainLooper())
    private var root: View? = null
    private var gif: GifHandle? = null
    private var currentTaskId: Long = -1L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Todo startForegroundService() precisa ser seguido de startForeground() imediatamente.
        enterForeground()

        val taskId = intent?.getLongExtra(EXTRA_TASK_ID, -1L) ?: -1L
        val title = intent?.getStringExtra(EXTRA_TITLE).orEmpty()
        if (taskId < 0 || !canDrawOverlays(this)) {
            removeOverlay()
            return START_NOT_STICKY
        }
        showOverlay(taskId, title)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        removeOverlayView()
        if (instance === this) instance = null
        super.onDestroy()
    }

    // ---------------------------------------------------------------- notificação do serviço

    private fun enterForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(SERVICE_CHANNEL, "Alerta visual", NotificationManager.IMPORTANCE_MIN).apply {
                    description = "Exibida apenas enquanto o alerta visual está na tela"
                    setShowBadge(false)
                }
            )
        }
        val notification: Notification = NotificationCompat.Builder(this, SERVICE_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_hao)
            .setContentTitle("Hao Alert")
            .setContentText("Alerta visual na tela")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else 0
        ServiceCompat.startForeground(this, SERVICE_NOTIFICATION_ID, notification, type)
    }

    // ---------------------------------------------------------------- sobreposição

    private fun showOverlay(taskId: Long, title: String) {
        removeOverlayView() // garante uma única sobreposição / um único GIF
        currentTaskId = taskId

        val view = buildView(taskId, title)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START }

        try {
            getSystemService(WindowManager::class.java).addView(view, params)
        } catch (_: Exception) {
            gif?.release(); gif = null
            stopSelfCleanly()
            return
        }
        root = view

        // Entrada suave
        view.alpha = 0f
        view.scaleX = 0.92f
        view.scaleY = 0.92f
        view.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(280)
            .setInterpolator(DecelerateInterpolator()).start()
    }

    private fun buildView(taskId: Long, title: String): View {
        val ctx = this
        val dp = { v: Int -> (v * resources.displayMetrics.density).toInt() }

        val gifHandle = GifPlayer.create(ctx)
        gif = gifHandle

        val column = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(64), dp(20), dp(44))
        }

        // GIF: ocupa o espaço restante (principal elemento visual), proporção preservada.
        if (gifHandle != null) {
            column.addView(
                gifHandle.view,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f),
            )
        } else {
            column.addView(View(ctx), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }

        column.addView(
            TextView(ctx).apply {
                text = title
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                maxLines = 4
                ellipsize = TextUtils.TruncateAt.END
                setShadowLayer(8f, 0f, 2f, Color.BLACK)
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(20) },
        )

        column.addView(
            TextView(ctx).apply {
                text = "TOQUE PARA ABRIR"
                setTextColor(0x99FFFFFF.toInt())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                letterSpacing = 0.18f
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(12) },
        )

        val close = TextView(ctx).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            gravity = Gravity.CENTER
            contentDescription = "Dispensar alerta"
            setOnClickListener { removeOverlay() } // só dispensa: a tarefa segue pendente
        }

        return FrameLayout(ctx).apply {
            setBackgroundColor(0xF2090405.toInt())
            addView(column, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            addView(
                close,
                FrameLayout.LayoutParams(dp(52), dp(52), Gravity.TOP or Gravity.END)
                    .apply { topMargin = dp(36); rightMargin = dp(12) },
            )
            setOnClickListener { openTask(taskId) }
        }
    }

    private fun openTask(taskId: Long) {
        val intent = Intent(this, MainActivity::class.java)
            .setData(Uri.parse("hao://open/$taskId"))
            .putExtra(Notifier.EXTRA_OPEN_TASK, taskId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        try {
            startActivity(intent)
        } catch (_: Exception) {
            // A notificação do lembrete continua disponível como caminho alternativo.
        }
        removeOverlay()
    }

    /** Remove só a view e libera o GIF (sem encerrar o serviço). */
    private fun removeOverlayView() {
        root?.let {
            it.animate().cancel()
            try {
                getSystemService(WindowManager::class.java).removeViewImmediate(it)
            } catch (_: Exception) {
            }
        }
        root = null
        gif?.release()
        gif = null
        currentTaskId = -1L
    }

    private fun removeOverlay() {
        removeOverlayView()
        stopSelfCleanly()
    }

    private fun stopSelfCleanly() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun dismissFor(taskId: Long?) {
        if (taskId == null || taskId == currentTaskId) removeOverlay()
    }

    companion object {
        private const val EXTRA_TASK_ID = "task_id"
        private const val EXTRA_TITLE = "title"
        private const val SERVICE_CHANNEL = "visual_alert_service"
        private const val SERVICE_NOTIFICATION_ID = 7001

        @Volatile
        private var instance: OverlayService? = null

        fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

        /** Chamado pelo AlarmReceiver quando o lembrete dispara. Nunca lança exceção. */
        fun show(context: Context, taskId: Long, title: String) {
            if (!canDrawOverlays(context)) return
            val intent = Intent(context, OverlayService::class.java)
                .putExtra(EXTRA_TASK_ID, taskId)
                .putExtra(EXTRA_TITLE, title)
            try {
                context.startForegroundService(intent)
            } catch (_: Exception) {
                // O sistema pode negar o início em segundo plano; a notificação normal já foi enviada.
            }
        }

        /** Fecha a sobreposição dessa tarefa (conclusão, exclusão, pausa, adiar). Não inicia o serviço. */
        fun dismiss(taskId: Long) {
            val service = instance ?: return
            service.main.post { service.dismissFor(taskId) }
        }

        /** Fecha qualquer sobreposição (usado quando o usuário abre o app por uma notificação). */
        fun dismissAny() {
            val service = instance ?: return
            service.main.post { service.dismissFor(null) }
        }
    }
}
