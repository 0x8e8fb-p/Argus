package com.nexusblock.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "blocked_events",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["host"]),
        Index(value = ["appPackage"])
    ]
)
data class BlockedEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val host: String,
    val appPackage: String? = null,
    val type: String = "dns", // dns, sni, https, etc.
    val timestamp: Long = System.currentTimeMillis()
)
