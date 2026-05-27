package com.nexusblock.data.repository

import com.nexusblock.data.db.BlockedDomainDao
import com.nexusblock.data.db.BlocklistSourceState
import com.nexusblock.data.model.BlockedDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlocklistRepository @Inject constructor(
    private val domainDao: BlockedDomainDao
) {
    fun observeDomainCount(): Flow<Int> = domainDao.observeCount()

    fun observeSourceStates(): Flow<List<BlocklistSourceState>> = domainDao.observeSourceStates()

    suspend fun isSourceEnabled(source: String, defaultEnabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        val hasExistingState = domainDao.countBySource(source) > 0
        if (hasExistingState) {
            domainDao.getSourceEnabled(source) != 0
        } else {
            defaultEnabled
        }
    }

    suspend fun replaceSource(
        source: String,
        domains: List<BlockedDomain>,
        defaultEnabled: Boolean = true
    ) = withContext(Dispatchers.IO) {
        val hasExistingState = domainDao.countBySource(source) > 0
        val enabled = if (hasExistingState) {
            domainDao.getSourceEnabled(source) != 0
        } else {
            defaultEnabled
        }
        val normalized = domains.map { it.copy(id = 0, source = source, enabled = enabled) }
        domainDao.replaceSource(source, normalized)
    }

    suspend fun setSourceEnabled(source: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        if (domainDao.countBySource(source) == 0) {
            domainDao.insertAll(
                listOf(
                    BlockedDomain(
                        host = "source-state-${source.lowercase().replace('_', '-')}.argus.invalid",
                        source = source,
                        enabled = enabled
                    )
                )
            )
        } else {
            domainDao.setEnabledBySource(source, enabled)
        }
    }

    suspend fun getDomainsBySource(source: String): List<BlockedDomain> = withContext(Dispatchers.IO) {
        domainDao.getBySource(source)
    }
}
