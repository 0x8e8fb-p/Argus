package com.nexusblock.di;

import com.nexusblock.data.db.AppDatabase;
import com.nexusblock.data.db.CustomRuleDao;
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
public final class DatabaseModule_ProvideCustomRuleDaoFactory implements Factory<CustomRuleDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvideCustomRuleDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public CustomRuleDao get() {
    return provideCustomRuleDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideCustomRuleDaoFactory create(
      Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideCustomRuleDaoFactory(dbProvider);
  }

  public static CustomRuleDao provideCustomRuleDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideCustomRuleDao(db));
  }
}
