package com.nexusblock.di;

import com.nexusblock.data.db.AppDatabase;
import com.nexusblock.data.db.BlockedEventDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class DatabaseModule_ProvideBlockedEventDaoFactory implements Factory<BlockedEventDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvideBlockedEventDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public BlockedEventDao get() {
    return provideBlockedEventDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideBlockedEventDaoFactory create(
      Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideBlockedEventDaoFactory(dbProvider);
  }

  public static BlockedEventDao provideBlockedEventDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideBlockedEventDao(db));
  }
}
