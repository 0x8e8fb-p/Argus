package com.nexusblock.ui.viewmodel;

import android.app.Application;
import com.nexusblock.data.repository.SettingsRepository;
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<Application> applicationProvider;

  private final Provider<SettingsRepository> settingsRepoProvider;

  public SettingsViewModel_Factory(Provider<Application> applicationProvider,
      Provider<SettingsRepository> settingsRepoProvider) {
    this.applicationProvider = applicationProvider;
    this.settingsRepoProvider = settingsRepoProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(applicationProvider.get(), settingsRepoProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<Application> applicationProvider,
      Provider<SettingsRepository> settingsRepoProvider) {
    return new SettingsViewModel_Factory(applicationProvider, settingsRepoProvider);
  }

  public static SettingsViewModel newInstance(Application application,
      SettingsRepository settingsRepo) {
    return new SettingsViewModel(application, settingsRepo);
  }
}
