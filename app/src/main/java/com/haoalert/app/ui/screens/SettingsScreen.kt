package com.haoalert.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.haoalert.app.notifications.PermissionHelper
import com.haoalert.app.ui.HaoViewModel
import com.haoalert.app.ui.components.HaoLogo
import com.haoalert.app.ui.components.HaoTopBar
import com.haoalert.app.ui.components.SectionTitle

private val INTERVALS = listOf(5, 10, 15, 20, 30, 60)

@Composable
fun SettingsScreen(vm: HaoViewModel, onBack: () -> Unit, onRequestNotifications: () -> Unit) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsState()
    val store = vm.settingsStore

    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    val permissions = remember(tick) { PermissionHelper.status(context) }

    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "—"
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HaoTopBar("Configurações", onBack = onBack) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionTitle("NOTIFICAÇÕES")
            Group {
                SwitchRow("Notificações", "Desligue para silenciar todos os lembretes", settings.notificationsEnabled) {
                    store.setNotificationsEnabled(it)
                }
                SwitchRow("Som", "Tocar o som padrão de notificação", settings.sound) { store.setSound(it) }
                SwitchRow("Vibração", "Vibrar ao receber lembretes", settings.vibration) { store.setVibration(it) }
            }

            SectionTitle("INTERVALO PADRÃO", Modifier.padding(top = 8.dp))
            Text(
                "Usado ao criar novas tarefas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                INTERVALS.forEach { minutes ->
                    FilterChip(
                        selected = settings.defaultInterval == minutes,
                        onClick = { store.setDefaultInterval(minutes) },
                        label = { Text("$minutes min") },
                    )
                }
            }

            SectionTitle("APARÊNCIA", Modifier.padding(top = 8.dp))
            Group {
                SwitchRow("Tema escuro", "Visual grafite com destaque vermelho", settings.darkTheme) { store.setDarkTheme(it) }
            }

            SectionTitle("PERMISSÕES", Modifier.padding(top = 8.dp))
            Group {
                PermissionRow(
                    title = "Notificações",
                    granted = permissions.notifications,
                    description = "Necessária para exibir os lembretes.",
                    actionLabel = "Ativar",
                    onAction = onRequestNotifications,
                )
                PermissionRow(
                    title = "Alarmes e lembretes",
                    granted = permissions.exactAlarms,
                    description = "Garante avisos no minuto certo, mesmo com o app fechado.",
                    actionLabel = "Permitir",
                    onAction = { PermissionHelper.openExactAlarmSettings(context) },
                )
                PermissionRow(
                    title = "Sem restrição de bateria",
                    granted = permissions.batteryUnrestricted,
                    description = "Alguns fabricantes (Xiaomi, Samsung, Huawei, Oppo…) encerram apps em segundo plano. " +
                        "Libere o Hao Alert da economia de bateria para não perder avisos.",
                    actionLabel = "Ajustar",
                    onAction = { PermissionHelper.openBatterySettings(context) },
                )
            }

            SectionTitle("SOBRE", Modifier.padding(top = 8.dp))
            Group {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    HaoLogo(size = 48.dp, showName = false)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Hao Alert", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Versão $versionName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    "Lembretes recorrentes que só param quando você conclui a tarefa. " +
                        "Funciona 100% offline: tudo fica salvo apenas neste aparelho.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Group(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .6f), shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) { content() }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun PermissionRow(
    title: String,
    granted: Boolean,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    if (granted) "Concedida" else "Desativada",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (granted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                )
            }
            if (!granted) {
                OutlinedButton(onClick = onAction, shape = RoundedCornerShape(14.dp)) { Text(actionLabel) }
            }
        }
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
