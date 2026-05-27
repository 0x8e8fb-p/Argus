package com.nexusblock.ui.viewmodel;

import android.app.Application;
import com.nexusblock.data.repository.SettingsRepository;
import com.nexusblock.engine.PrivateDnsManager;
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

  private final Provider<PrivateDnsManager> privateDnsManagerProvider;

  public SettingsViewModel_Factory(Provider<Application> applicationProvider,
      Provider<SettingsRepository> settingsRepoProvider,
      Provider<PrivateDnsManager> privateDnsManagerProvider) {
    this.applicationProvider = applicationProvider;
    this.settingsRepoProvider = settingsRepoProvider;
    this.privateDnsManagerProvider = privateDnsManagerProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(applicationProvider.get(), settingsRepoProvider.get(), privateDnsManagerProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<Application> applicationProvider,
      Provider<SettingsRepository> settingsRepoProvider,
      Provider<PrivateDnsManager> privateDnsManagerProvider) {
    return new SettingsViewModel_Factory(applicationProvider, settingsRepoProvider, privateDnsManagerProvider);
  }

  public static SettingsViewModel newInstance(Application application,
      SettingsRepository settingsRepo, PrivateDnsManager privateDnsManager) {
    return new SettingsViewModel(application, settingsRepo, privateDnsManager);
  }
}
