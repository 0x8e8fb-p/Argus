package com.nexusblock.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "blocked_domains",
    indices = [Index(value = ["host"], unique = true), Index(value = ["source"])]
)
data class BlockedDomain(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val host: String,
    val source: String,
    val enabled: Boolean = true,
    val isRegex: Boolean = false,
    val regexPattern: String? = null,
    val insertedAt: Long = System.currentTimeMillis()
)
