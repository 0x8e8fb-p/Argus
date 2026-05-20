package com.nexusblock.ui;

import com.nexusblock.data.repository.SettingsRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<SettingsRepository> settingsRepoProvider;

  public MainActivity_MembersInjector(Provider<SettingsRepository> settingsRepoProvider) {
    this.settingsRepoProvider = settingsRepoProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<SettingsRepository> settingsRepoProvider) {
    return new MainActivity_MembersInjector(settingsRepoProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectSettingsRepo(instance, settingsRepoProvider.get());
  }

  @InjectedFieldSignature("com.nexusblock.ui.MainActivity.settingsRepo")
  public static void injectSettingsRepo(MainActivity instance, SettingsRepository settingsRepo) {
    instance.settingsRepo = settingsRepo;
  }
}
