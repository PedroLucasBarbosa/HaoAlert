package com.haoalert.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.haoalert.app.data.TaskEntity
import com.haoalert.app.data.TaskEventEntity
import com.haoalert.app.haoApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HaoViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application.haoApp
    private val repo = app.repository

    val settingsStore = app.settings
    val settings = app.settings.state

    /** null enquanto o banco ainda não respondeu (evita piscar o estado "vazio"). */
    val pending: StateFlow<List<TaskEntity>?> =
        repo.pending.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val completed: StateFlow<List<TaskEntity>?> =
        repo.completed.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val events: StateFlow<List<TaskEventEntity>> =
        repo.events.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Par (tarefa, já carregou?). */
    fun observeTask(id: Long): Flow<Pair<TaskEntity?, Boolean>> = repo.observe(id).map { it to true }

    suspend fun getTask(id: Long): TaskEntity? = repo.get(id)

    fun save(task: TaskEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch { repo.save(task); onDone() }
    }

    fun restore(task: TaskEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch { repo.restore(task); onDone() }
    }

    fun complete(id: Long) = viewModelScope.launch { repo.complete(id) }

    fun setEnabled(id: Long, enabled: Boolean) = viewModelScope.launch { repo.setEnabled(id, enabled) }

    fun duplicate(id: Long, onDone: (Long?) -> Unit = {}) {
        viewModelScope.launch { onDone(repo.duplicate(id)) }
    }

    fun delete(id: Long) = viewModelScope.launch { repo.delete(id) }

    fun updateCompletedInfo(id: Long, title: String, description: String) =
        viewModelScope.launch { repo.updateCompletedInfo(id, title, description) }

    fun deleteCompleted(ids: List<Long>) = viewModelScope.launch { repo.deleteCompleted(ids) }

    fun clearCompleted() = viewModelScope.launch { repo.clearCompleted() }
}
