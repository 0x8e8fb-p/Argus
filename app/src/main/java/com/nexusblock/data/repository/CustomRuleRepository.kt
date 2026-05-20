package com.nexusblock.data.repository

import com.nexusblock.data.db.CustomRuleDao
import com.nexusblock.data.model.CustomRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomRuleRepository @Inject constructor(
    private val customRuleDao: CustomRuleDao
) {
    fun observeAllRules(): Flow<List<CustomRule>> = customRuleDao.observeAll()

    fun observeEnabledRules(): Flow<List<CustomRule>> = customRuleDao.observeEnabled()

    suspend fun getEnabledRules(): List<CustomRule> = withContext(Dispatchers.IO) {
        customRuleDao.getEnabled()
    }

    suspend fun addRule(rule: CustomRule) = withContext(Dispatchers.IO) {
        customRuleDao.insert(rule)
    }

    suspend fun updateRule(rule: CustomRule) = withContext(Dispatchers.IO) {
        customRuleDao.update(rule)
    }

    suspend fun deleteRule(rule: CustomRule) = withContext(Dispatchers.IO) {
        customRuleDao.delete(rule)
    }

    suspend fun deleteRuleById(id: Long) = withContext(Dispatchers.IO) {
        customRuleDao.deleteById(id)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        customRuleDao.clearAll()
    }
}
