package com.nexusblock.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexusblock.data.model.BlockedEvent
import com.nexusblock.data.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val statsRepo: StatsRepository
) : ViewModel() {

    private val events: StateFlow<List<BlockedEvent>> = statsRepo.observeRecentEvents(500)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _filter = MutableStateFlow("")

    val filteredEvents: StateFlow<List<BlockedEvent>> = combine(events, _filter) { list, query ->
        if (query.isBlank()) list
        else list.filter { it.host.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearLogs() {
        viewModelScope.launch {
            statsRepo.clearLogs()
        }
    }

    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
