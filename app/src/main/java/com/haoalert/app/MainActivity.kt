package com.haoalert.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.haoalert.app.notifications.Notifier
import com.haoalert.app.notifications.PermissionHelper
import com.haoalert.app.ui.AppNav
import com.haoalert.app.ui.HaoViewModel
import com.haoalert.app.ui.theme.HaoTheme

class MainActivity : ComponentActivity() {

    /** Id da tarefa pedida por uma notificação, até a navegação consumi-lo. */
    private val openTaskRequest = mutableStateOf<Long?>(null)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* o estado é relido ao voltar */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)

        setContent {
            val vm: HaoViewModel = viewModel()
            val settings by vm.settings.collectAsState()

            DisposableEffect(settings.darkTheme) {
                val style = if (settings.darkTheme) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }

            HaoTheme(darkTheme = settings.darkTheme) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNav(
                        vm = vm,
                        openTaskRequest = openTaskRequest,
                        onOpenTaskHandled = { openTaskRequest.value = null },
                        onRequestNotifications = ::requestNotifications,
                    )
                }
            }
        }

        // No primeiro uso, já pede a permissão de notificações (Android 13+).
        if (savedInstanceState == null &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !PermissionHelper.notificationsGranted(this) &&
            !haoApp.settings.askedNotificationPermission
        ) {
            requestNotifications()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val id = intent?.getLongExtra(Notifier.EXTRA_OPEN_TASK, -1L) ?: -1L
        if (id >= 0) {
            openTaskRequest.value = id
            intent?.removeExtra(Notifier.EXTRA_OPEN_TASK)
        }
    }

    /**
     * Pede a permissão de notificações. Se o Android não mostrar mais o diálogo (negada antes) ou se
     * as notificações foram desligadas nas configurações do sistema, abre a tela de ajustes do app.
     */
    private fun requestNotifications() {
        val store = haoApp.settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            if (store.askedNotificationPermission &&
                !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
            ) {
                PermissionHelper.openAppNotificationSettings(this)
            } else {
                store.askedNotificationPermission = true
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            PermissionHelper.openAppNotificationSettings(this)
        }
    }
}
