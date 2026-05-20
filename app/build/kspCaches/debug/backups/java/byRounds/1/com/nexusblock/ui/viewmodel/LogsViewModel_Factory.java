package com.nexusblock.ui.viewmodel;

import com.nexusblock.data.repository.StatsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class LogsViewModel_Factory implements Factory<LogsViewModel> {
  private final Provider<StatsRepository> statsRepoProvider;

  public LogsViewModel_Factory(Provider<StatsRepository> statsRepoProvider) {
    this.statsRepoProvider = statsRepoProvider;
  }

  @Override
  public LogsViewModel get() {
    return newInstance(statsRepoProvider.get());
  }

  public static LogsViewModel_Factory create(Provider<StatsRepository> statsRepoProvider) {
    return new LogsViewModel_Factory(statsRepoProvider);
  }

  public static LogsViewModel newInstance(StatsRepository statsRepo) {
    return new LogsViewModel(statsRepo);
  }
}
