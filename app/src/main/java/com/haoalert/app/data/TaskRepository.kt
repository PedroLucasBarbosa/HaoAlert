package com.haoalert.app.data

import com.haoalert.app.notifications.AlarmScheduler
import com.haoalert.app.notifications.Notifier
import kotlinx.coroutines.flow.Flow

/**
 * Único ponto de escrita das tarefas: toda alteração no banco já atualiza (ou cancela) os
 * alarmes correspondentes, então os dois nunca ficam fora de sincronia.
 */
class TaskRepository(
    private val dao: TaskDao,
    private val scheduler: AlarmScheduler,
    private val notifier: Notifier,
) {
    val pending: Flow<List<TaskEntity>> = dao.observePending()
    val completed: Flow<List<TaskEntity>> = dao.observeCompleted()
    val events: Flow<List<TaskEventEntity>> = dao.observeEvents()

    fun observe(id: Long): Flow<TaskEntity?> = dao.observe(id)
    suspend fun get(id: Long): TaskEntity? = dao.get(id)

    /** Cria ou atualiza uma tarefa pendente e (re)agenda seus alarmes. */
    suspend fun save(task: TaskEntity): Long {
        val id = if (task.id == 0L) {
            dao.insert(task)
        } else {
            dao.update(task)
            task.id
        }
        scheduler.schedule(task.copy(id = id))
        return id
    }

    suspend fun duplicate(id: Long): Long? {
        val original = dao.get(id) ?: return null
        val copy = original.copy(
            id = 0,
            title = "${original.title} (cópia)",
            enabled = true,
            completed = false,
            completedAt = null,
            createdAt = System.currentTimeMillis(),
            restoredCount = 0,
            lastRestoredAt = null,
        )
        return save(copy)
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) {
        val task = dao.get(id) ?: return
        if (task.completed) return
        val updated = task.copy(enabled = enabled)
        dao.update(updated)
        if (!enabled) notifier.cancel(id)
        scheduler.schedule(updated)
    }

    /** Conclusão explícita: encerra imediatamente o ciclo de notificações. */
    suspend fun complete(id: Long) {
        val task = dao.get(id) ?: return
        if (task.completed) return
        val now = System.currentTimeMillis()
        // Banco primeiro: se um alarme disparar agora, o receiver já enxerga a tarefa concluída.
        dao.update(task.copy(completed = true, completedAt = now))
        dao.insertEvent(TaskEventEntity(taskId = id, title = task.title, type = TaskEventEntity.TYPE_COMPLETED, at = now))
        scheduler.cancel(id)
        notifier.cancel(id)
    }

    /** Devolve uma tarefa concluída para as pendentes, já com os dias/horários revisados. */
    suspend fun restore(edited: TaskEntity) {
        val now = System.currentTimeMillis()
        val restored = edited.copy(
            completed = false,
            completedAt = null,
            enabled = true,
            restoredCount = edited.restoredCount + 1,
            lastRestoredAt = now,
        )
        dao.update(restored)
        dao.insertEvent(TaskEventEntity(taskId = restored.id, title = restored.title, type = TaskEventEntity.TYPE_RESTORED, at = now))
        scheduler.schedule(restored)
    }

    /** Editar uma tarefa concluída nunca reativa notificações. */
    suspend fun updateCompletedInfo(id: Long, title: String, description: String) {
        val task = dao.get(id) ?: return
        if (!task.completed) return
        dao.update(task.copy(title = title, description = description))
    }

    suspend fun delete(id: Long) {
        scheduler.cancel(id)
        notifier.cancel(id)
        dao.removeTask(id)
    }

    suspend fun deleteCompleted(ids: List<Long>) {
        if (ids.isEmpty()) return
        dao.removeCompleted(ids)
    }

    suspend fun clearCompleted() = dao.removeAllCompleted()

    /** Adia: remove o aviso atual e agenda o próximo para daqui a [minutes] minutos. */
    suspend fun snooze(id: Long, minutes: Int) {
        val task = dao.get(id) ?: return
        notifier.cancel(id)
        if (task.completed) return
        scheduler.setAt(id, System.currentTimeMillis() + minutes * 60_000L)
    }
}
