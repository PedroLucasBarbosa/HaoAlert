package com.haoalert.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Uma tarefa/lembrete. Fica em "Pendentes" enquanto [completed] for false e vai para
 * "Concluídas" somente quando o usuário confirma explicitamente a conclusão.
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    /** Datas escolhidas no calendário, no formato ISO separado por vírgula ("2026-11-24,2026-11-25"). */
    val dates: String = "",
    val hour: Int = 9,
    val minute: Int = 0,
    /** Intervalo entre as notificações, em segundos (padrão: 600 = 10 min). */
    val intervalSeconds: Int = 600,
    val enabled: Boolean = true,
    /** Alerta visual com GIF sobreposto nesta tarefa. */
    val visualAlert: Boolean = true,
    val completed: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val restoredCount: Int = 0,
    val lastRestoredAt: Long? = null,
)

/** Registro do histórico: conclusões e restaurações. */
@Entity(tableName = "task_events")
data class TaskEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val title: String,
    /** [TYPE_COMPLETED] ou [TYPE_RESTORED]. */
    val type: String,
    val at: Long,
) {
    companion object {
        const val TYPE_COMPLETED = "COMPLETED"
        const val TYPE_RESTORED = "RESTORED"
    }
}
