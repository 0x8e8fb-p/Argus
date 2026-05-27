package com.nexusblock.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.nexusblock.Constants
import com.nexusblock.data.repository.BlocklistSources
import com.nexusblock.data.worker.BlocklistUpdateWorker
import com.nexusblock.data.repository.BlocklistRepository
import com.nexusblock.engine.DnsFilterEngine
import com.nexusblock.service.NexusVpnService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlocklistUpdateResult(
    val status: WorkInfo.State? = null,
    val updated: String = "",
    val skipped: String = "",
    val failed: String = "",
    val totalRules: Int = 0,
    val isIdle: Boolean = true
)

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

    private val _lastUpdateResult = MutableStateFlow(BlocklistUpdateResult())
    val lastUpdateResult: StateFlow<BlocklistUpdateResult> = _lastUpdateResult.asStateFlow()

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

        // Observe the one-time update work to provide live feedback
        viewModelScope.launch {
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkFlow("${Constants.WORK_TAG_BLOCKLIST}_initial")
                .collect { infos ->
                    val info = infos.firstOrNull() ?: return@collect
                    val state = info.state
                    _isUpdating.value = state == WorkInfo.State.RUNNING || state == WorkInfo.State.ENQUEUED

                    if (state == WorkInfo.State.SUCCEEDED || state == WorkInfo.State.FAILED) {
                        val output = info.outputData
                        _lastUpdateResult.value = BlocklistUpdateResult(
                            status = state,
                            updated = output.getString(BlocklistUpdateWorker.KEY_UPDATED) ?: "",
                            skipped = output.getString(BlocklistUpdateWorker.KEY_SKIPPED) ?: "",
                            failed = output.getString(BlocklistUpdateWorker.KEY_FAILED) ?: "",
                            totalRules = output.getInt(BlocklistUpdateWorker.KEY_TOTAL_RULES, 0),
                            isIdle = false
                        )
                    } else if (state == WorkInfo.State.RUNNING) {
                        _lastUpdateResult.value = BlocklistUpdateResult(
                            status = state,
                            isIdle = false
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
                dnsEngine.reloadRules()
            }
        }
    }

    fun updateNow() {
        viewModelScope.launch {
            _isUpdating.value = true
            _lastUpdateResult.value = BlocklistUpdateResult(status = WorkInfo.State.ENQUEUED, isIdle = false)
            BlocklistUpdateWorker.runNow(context)
        }
    }
}

data class BlocklistSource(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val count: Int
)
