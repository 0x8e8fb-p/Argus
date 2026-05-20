package com.nexusblock.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nexusblock.data.model.BlockedDomain
import com.nexusblock.data.model.BlockedEvent
import com.nexusblock.data.model.CustomRule

@Database(
    entities = [BlockedDomain::class, BlockedEvent::class, CustomRule::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blockedDomainDao(): BlockedDomainDao
    abstract fun blockedEventDao(): BlockedEventDao
    abstract fun customRuleDao(): CustomRuleDao
}
