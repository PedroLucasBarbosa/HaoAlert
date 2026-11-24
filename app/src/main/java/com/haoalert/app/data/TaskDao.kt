package com.haoalert.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TaskDao {
    @Query("SELECT * FROM tasks WHERE completed = 0 ORDER BY hour, minute, id")
    abstract fun observePending(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE completed = 1 ORDER BY completedAt DESC")
    abstract fun observeCompleted(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    abstract fun observe(id: Long): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id")
    abstract suspend fun get(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE completed = 0 AND enabled = 1")
    abstract suspend fun getActive(): List<TaskEntity>

    @Insert
    abstract suspend fun insert(task: TaskEntity): Long

    @Update
    abstract suspend fun update(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    abstract suspend fun deleteTask(id: Long)

    @Query("DELETE FROM tasks WHERE id IN (:ids) AND completed = 1")
    abstract suspend fun deleteCompletedIn(ids: List<Long>)

    @Query("DELETE FROM tasks WHERE completed = 1")
    abstract suspend fun deleteAllCompleted()

    // ---- Histórico ----

    @Insert
    abstract suspend fun insertEvent(event: TaskEventEntity)

    @Query("SELECT * FROM task_events ORDER BY at DESC")
    abstract fun observeEvents(): Flow<List<TaskEventEntity>>

    @Query("DELETE FROM task_events WHERE taskId = :taskId")
    abstract suspend fun deleteEventsFor(taskId: Long)

    @Query("DELETE FROM task_events WHERE taskId IN (:ids)")
    abstract suspend fun deleteEventsForMany(ids: List<Long>)

    @Query("DELETE FROM task_events WHERE taskId IN (SELECT id FROM tasks WHERE completed = 1)")
    abstract suspend fun deleteEventsOfCompleted()

    @Transaction
    open suspend fun removeTask(id: Long) {
        deleteEventsFor(id)
        deleteTask(id)
    }

    @Transaction
    open suspend fun removeCompleted(ids: List<Long>) {
        deleteEventsForMany(ids)
        deleteCompletedIn(ids)
    }

    @Transaction
    open suspend fun removeAllCompleted() {
        deleteEventsOfCompleted()
        deleteAllCompleted()
    }
}
