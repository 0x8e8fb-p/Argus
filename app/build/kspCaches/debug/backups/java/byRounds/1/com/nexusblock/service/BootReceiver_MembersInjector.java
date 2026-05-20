package com.nexusblock.service;

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
public final class BootReceiver_MembersInjector implements MembersInjector<BootReceiver> {
  private final Provider<SettingsRepository> settingsRepoProvider;

  public BootReceiver_MembersInjector(Provider<SettingsRepository> settingsRepoProvider) {
    this.settingsRepoProvider = settingsRepoProvider;
  }

  public static MembersInjector<BootReceiver> create(
      Provider<SettingsRepository> settingsRepoProvider) {
    return new BootReceiver_MembersInjector(settingsRepoProvider);
  }

  @Override
  public void injectMembers(BootReceiver instance) {
    injectSettingsRepo(instance, settingsRepoProvider.get());
  }

  @InjectedFieldSignature("com.nexusblock.service.BootReceiver.settingsRepo")
  public static void injectSettingsRepo(BootReceiver instance, SettingsRepository settingsRepo) {
    instance.settingsRepo = settingsRepo;
  }
}
