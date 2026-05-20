package com.nexusblock.di

import android.content.Context
import androidx.room.Room
import com.nexusblock.data.db.AppDatabase
import com.nexusblock.data.db.BlockedDomainDao
import com.nexusblock.data.db.BlockedEventDao
import com.nexusblock.data.db.CustomRuleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "nexusblock.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideBlockedDomainDao(db: AppDatabase): BlockedDomainDao = db.blockedDomainDao()

    @Provides
    fun provideBlockedEventDao(db: AppDatabase): BlockedEventDao = db.blockedEventDao()

    @Provides
    fun provideCustomRuleDao(db: AppDatabase): CustomRuleDao = db.customRuleDao()
}
