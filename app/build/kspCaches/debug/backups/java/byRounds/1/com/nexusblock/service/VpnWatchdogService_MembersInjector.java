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
public final class VpnWatchdogService_MembersInjector implements MembersInjector<VpnWatchdogService> {
  private final Provider<SettingsRepository> settingsRepoProvider;

  public VpnWatchdogService_MembersInjector(Provider<SettingsRepository> settingsRepoProvider) {
    this.settingsRepoProvider = settingsRepoProvider;
  }

  public static MembersInjector<VpnWatchdogService> create(
      Provider<SettingsRepository> settingsRepoProvider) {
    return new VpnWatchdogService_MembersInjector(settingsRepoProvider);
  }

  @Override
  public void injectMembers(VpnWatchdogService instance) {
    injectSettingsRepo(instance, settingsRepoProvider.get());
  }

  @InjectedFieldSignature("com.nexusblock.service.VpnWatchdogService.settingsRepo")
  public static void injectSettingsRepo(VpnWatchdogService instance,
      SettingsRepository settingsRepo) {
    instance.settingsRepo = settingsRepo;
  }
}
