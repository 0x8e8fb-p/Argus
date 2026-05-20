package com.nexusblock.ui.viewmodel;

import android.content.Context;
import com.nexusblock.data.repository.SettingsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class FirewallViewModel_Factory implements Factory<FirewallViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<SettingsRepository> settingsRepoProvider;

  public FirewallViewModel_Factory(Provider<Context> contextProvider,
      Provider<SettingsRepository> settingsRepoProvider) {
    this.contextProvider = contextProvider;
    this.settingsRepoProvider = settingsRepoProvider;
  }

  @Override
  public FirewallViewModel get() {
    return newInstance(contextProvider.get(), settingsRepoProvider.get());
  }

  public static FirewallViewModel_Factory create(Provider<Context> contextProvider,
      Provider<SettingsRepository> settingsRepoProvider) {
    return new FirewallViewModel_Factory(contextProvider, settingsRepoProvider);
  }

  public static FirewallViewModel newInstance(Context context, SettingsRepository settingsRepo) {
    return new FirewallViewModel(context, settingsRepo);
  }
}
