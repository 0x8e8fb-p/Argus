package com.nexusblock.data.repository;

import com.nexusblock.data.db.BlockedEventDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class StatsRepository_Factory implements Factory<StatsRepository> {
  private final Provider<BlockedEventDao> eventDaoProvider;

  public StatsRepository_Factory(Provider<BlockedEventDao> eventDaoProvider) {
    this.eventDaoProvider = eventDaoProvider;
  }

  @Override
  public StatsRepository get() {
    return newInstance(eventDaoProvider.get());
  }

  public static StatsRepository_Factory create(Provider<BlockedEventDao> eventDaoProvider) {
    return new StatsRepository_Factory(eventDaoProvider);
  }

  public static StatsRepository newInstance(BlockedEventDao eventDao) {
    return new StatsRepository(eventDao);
  }
}
