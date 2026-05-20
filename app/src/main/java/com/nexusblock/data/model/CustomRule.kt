package com.nexusblock.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "custom_rules",
    indices = [Index(value = ["rule"], unique = true)]
)
data class CustomRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rule: String,
    val isAllow: Boolean = false, // false = block, true = allow (@@)
    val enabled: Boolean = true,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
