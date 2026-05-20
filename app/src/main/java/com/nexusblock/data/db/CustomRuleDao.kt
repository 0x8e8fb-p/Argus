package com.nexusblock.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nexusblock.data.model.CustomRule
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomRuleDao {

    @Query("SELECT * FROM custom_rules WHERE enabled = 1")
    fun observeEnabled(): Flow<List<CustomRule>>

    @Query("SELECT * FROM custom_rules WHERE enabled = 1")
    suspend fun getEnabled(): List<CustomRule>

    @Query("SELECT * FROM custom_rules ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CustomRule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: CustomRule)

    @Update
    suspend fun update(rule: CustomRule)

    @Delete
    suspend fun delete(rule: CustomRule)

    @Query("DELETE FROM custom_rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM custom_rules WHERE enabled = 1")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM custom_rules")
    suspend fun clearAll()
}
