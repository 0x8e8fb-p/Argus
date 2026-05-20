package com.nexusblock.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nexusblock.data.model.BlockedDomain
import kotlinx.coroutines.flow.Flow

data class BlocklistSourceState(
    val source: String,
    val enabled: Boolean,
    val count: Int
)

@Dao
interface BlockedDomainDao {

    @Query("SELECT * FROM blocked_domains WHERE enabled = 1")
    fun observeEnabled(): Flow<List<BlockedDomain>>

    @Query("SELECT * FROM blocked_domains WHERE enabled = 1")
    suspend fun getEnabled(): List<BlockedDomain>

    @Query("SELECT COUNT(*) FROM blocked_domains WHERE enabled = 1")
    fun observeCount(): Flow<Int>

    @Query("SELECT source, CASE WHEN MIN(enabled) != 0 THEN 1 ELSE 0 END AS enabled, COUNT(*) AS count FROM blocked_domains GROUP BY source")
    fun observeSourceStates(): Flow<List<BlocklistSourceState>>

    @Query("SELECT * FROM blocked_domains WHERE source = :source")
    suspend fun getBySource(source: String): List<BlockedDomain>

    @Query("SELECT COUNT(*) FROM blocked_domains WHERE source = :source")
    suspend fun countBySource(source: String): Int

    @Query("SELECT COALESCE(MIN(enabled), 1) FROM blocked_domains WHERE source = :source")
    suspend fun getSourceEnabled(source: String): Int

    @Query("DELETE FROM blocked_domains WHERE source = :source")
    suspend fun deleteBySource(source: String)

    @Query("UPDATE blocked_domains SET enabled = :enabled WHERE source = :source")
    suspend fun setEnabledBySource(source: String, enabled: Boolean)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(domains: List<BlockedDomain>)

    @Transaction
    suspend fun replaceSource(source: String, domains: List<BlockedDomain>) {
        deleteBySource(source)
        // Batch insert to avoid OOM and excessive memory pressure
        val batchSize = 2000
        domains.chunked(batchSize).forEach { batch ->
            insertAll(batch)
        }
    }

    @Query("DELETE FROM blocked_domains")
    suspend fun clearAll()
}
