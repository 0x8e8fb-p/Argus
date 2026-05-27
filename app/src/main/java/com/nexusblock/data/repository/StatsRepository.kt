package com.nexusblock.data.repository

import com.nexusblock.data.db.BlockedEventDao
import com.nexusblock.data.model.BlockedEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsRepository @Inject constructor(
    private val eventDao: BlockedEventDao
) {
    fun observeRecentEvents(limit: Int = 500): Flow<List<BlockedEvent>> = eventDao.observeRecent(limit)

    fun observeTotalBlocked(): Flow<Int> = eventDao.observeTotalCount()

    suspend fun getRecentEvents(limit: Int = 500): List<BlockedEvent> = withContext(Dispatchers.IO) {
        eventDao.getRecent(limit)
    }

    suspend fun logBlocked(host: String, appPackage: String? = null, type: String = "dns") = withContext(Dispatchers.IO) {
        eventDao.insert(BlockedEvent(host = host, appPackage = appPackage, type = type))
    }

    suspend fun logBlockedBatch(events: List<BlockedEvent>) = withContext(Dispatchers.IO) {
        eventDao.insertAll(events)
    }

    suspend fun getBlockedSince(since: Long): Int = withContext(Dispatchers.IO) {
        eventDao.countSince(since)
    }

    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        eventDao.clearAll()
    }

    suspend fun pruneLogs(maxRows: Int = 5000) = withContext(Dispatchers.IO) {
        eventDao.pruneToMax(maxRows)
    }
}
