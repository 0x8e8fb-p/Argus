package com.nexusblock.di;

import com.nexusblock.data.db.AppDatabase;
import com.nexusblock.data.db.BlockedDomainDao;
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
public final class DatabaseModule_ProvideBlockedDomainDaoFactory implements Factory<BlockedDomainDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvideBlockedDomainDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public BlockedDomainDao get() {
    return provideBlockedDomainDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideBlockedDomainDaoFactory create(
      Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideBlockedDomainDaoFactory(dbProvider);
  }

  public static BlockedDomainDao provideBlockedDomainDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideBlockedDomainDao(db));
  }
}
