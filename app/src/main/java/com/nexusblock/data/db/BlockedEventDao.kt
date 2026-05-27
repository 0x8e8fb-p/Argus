package com.nexusblock.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.nexusblock.data.model.BlockedEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedEventDao {

    @Query("SELECT * FROM blocked_events ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 500): Flow<List<BlockedEvent>>

    @Query("SELECT * FROM blocked_events ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 500): List<BlockedEvent>

    @Query("SELECT COUNT(*) FROM blocked_events")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM blocked_events WHERE timestamp >= :since")
    suspend fun countSince(since: Long): Int

    @Insert
    suspend fun insert(event: BlockedEvent)

    @Insert
    suspend fun insertAll(events: List<BlockedEvent>)

    @Query("DELETE FROM blocked_events")
    suspend fun clearAll()

    @Query("DELETE FROM blocked_events WHERE id NOT IN (SELECT id FROM blocked_events ORDER BY timestamp DESC LIMIT :limit)")
    suspend fun pruneToMax(limit: Int)
}
