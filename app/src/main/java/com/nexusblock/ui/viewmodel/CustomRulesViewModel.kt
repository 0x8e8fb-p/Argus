package com.nexusblock.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexusblock.data.model.CustomRule
import com.nexusblock.data.repository.CustomRuleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomRulesViewModel @Inject constructor(
    private val customRuleRepo: CustomRuleRepository
) : ViewModel() {

    val rules: StateFlow<List<CustomRule>> = customRuleRepo.observeAllRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isAdding = MutableStateFlow(false)
    val isAdding: StateFlow<Boolean> = _isAdding.asStateFlow()

    fun addRule(ruleText: String, isAllow: Boolean, description: String?) {
        viewModelScope.launch {
            _isAdding.value = true
            try {
                customRuleRepo.addRule(
                    CustomRule(
                        rule = ruleText.trim(),
                        isAllow = isAllow,
                        description = description?.trim()
                    )
                )
            } finally {
                _isAdding.value = false
            }
        }
    }

    fun toggleRule(rule: CustomRule) {
        viewModelScope.launch {
            customRuleRepo.updateRule(rule.copy(enabled = !rule.enabled))
        }
    }

    fun deleteRule(rule: CustomRule) {
        viewModelScope.launch {
            customRuleRepo.deleteRule(rule)
        }
    }
}
