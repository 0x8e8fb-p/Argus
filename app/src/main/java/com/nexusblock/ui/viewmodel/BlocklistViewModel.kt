package com.nexusblock.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexusblock.data.repository.BlocklistSources
import com.nexusblock.data.worker.BlocklistUpdateWorker
import com.nexusblock.data.repository.BlocklistRepository
import com.nexusblock.engine.DnsFilterEngine
import com.nexusblock.service.NexusVpnService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

@HiltViewModel
class BlocklistViewModel @Inject constructor(
    application: Application,
    private val blocklistRepo: BlocklistRepository,
    private val dnsEngine: DnsFilterEngine
) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val _sources = MutableStateFlow<List<BlocklistSource>>(emptyList())
    val sources: StateFlow<List<BlocklistSource>> = _sources.asStateFlow()

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    init {
        _sources.value = BlocklistSources.all.map {
            BlocklistSource(it.id, it.name, it.defaultEnabled, 0)
        }

        viewModelScope.launch {
            blocklistRepo.observeSourceStates().collect { states ->
                val byId = states.associateBy { it.source }
                _sources.value = BlocklistSources.all.map { definition ->
                    val state = byId[definition.id]
                    BlocklistSource(
                        id = definition.id,
                        name = definition.name,
                        enabled = state?.enabled ?: definition.defaultEnabled,
                        count = state?.count ?: 0
                    )
                }
            }
        }
    }

    fun toggleSource(id: String, enabled: Boolean) {
        viewModelScope.launch {
            blocklistRepo.setSourceEnabled(id, enabled)
            _sources.value = _sources.value.map {
                if (it.id == id) it.copy(enabled = enabled) else it
            }
            if (NexusVpnService.isRunning) {
                dnsEngine.reloadBlocklists()
            }
        }
    }

    fun updateNow() {
        viewModelScope.launch {
            _isUpdating.value = true
            try {
                val request = OneTimeWorkRequestBuilder<BlocklistUpdateWorker>().build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    "blocklist_update_now",
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            } finally {
                _isUpdating.value = false
            }
        }
    }
}

data class BlocklistSource(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val count: Int
)
