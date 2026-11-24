package com.haoalert.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AppSettings(
    val notificationsEnabled: Boolean = true,
    val sound: Boolean = true,
    val vibration: Boolean = true,
    val defaultInterval: Int = 10,
    val darkTheme: Boolean = true,
)

/** Preferências locais simples (SharedPreferences), observáveis pela interface. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("hao_settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppSettings> = _state

    private fun load() = AppSettings(
        notificationsEnabled = prefs.getBoolean(KEY_NOTIF, true),
        sound = prefs.getBoolean(KEY_SOUND, true),
        vibration = prefs.getBoolean(KEY_VIBRATION, true),
        defaultInterval = prefs.getInt(KEY_INTERVAL, 10),
        darkTheme = prefs.getBoolean(KEY_DARK, true),
    )

    fun setNotificationsEnabled(v: Boolean) = update { prefs.edit().putBoolean(KEY_NOTIF, v).apply() }
    fun setSound(v: Boolean) = update { prefs.edit().putBoolean(KEY_SOUND, v).apply() }
    fun setVibration(v: Boolean) = update { prefs.edit().putBoolean(KEY_VIBRATION, v).apply() }
    fun setDefaultInterval(v: Int) = update { prefs.edit().putInt(KEY_INTERVAL, v).apply() }
    fun setDarkTheme(v: Boolean) = update { prefs.edit().putBoolean(KEY_DARK, v).apply() }

    /** Controle simples para saber se o pedido de permissão de notificações já foi feito. */
    var askedNotificationPermission: Boolean
        get() = prefs.getBoolean(KEY_ASKED_NOTIF, false)
        set(value) = prefs.edit().putBoolean(KEY_ASKED_NOTIF, value).apply()

    private inline fun update(write: () -> Unit) {
        write()
        _state.value = load()
    }

    private companion object {
        const val KEY_NOTIF = "notifications_enabled"
        const val KEY_SOUND = "sound"
        const val KEY_VIBRATION = "vibration"
        const val KEY_INTERVAL = "default_interval"
        const val KEY_DARK = "dark_theme"
        const val KEY_ASKED_NOTIF = "asked_notif_permission"
    }
}
