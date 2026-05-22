package com.nexusblock.engine;

import com.nexusblock.data.repository.SettingsRepository;
import com.nexusblock.data.repository.StatsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class PacketRouter_Factory implements Factory<PacketRouter> {
  private final Provider<DnsFilterEngine> dnsEngineProvider;

  private final Provider<StatsRepository> statsRepoProvider;

  private final Provider<SettingsRepository> settingsRepoProvider;

  public PacketRouter_Factory(Provider<DnsFilterEngine> dnsEngineProvider,
      Provider<StatsRepository> statsRepoProvider,
      Provider<SettingsRepository> settingsRepoProvider) {
    this.dnsEngineProvider = dnsEngineProvider;
    this.statsRepoProvider = statsRepoProvider;
    this.settingsRepoProvider = settingsRepoProvider;
  }

  @Override
  public PacketRouter get() {
    return newInstance(dnsEngineProvider.get(), statsRepoProvider.get(), settingsRepoProvider.get());
  }

  public static PacketRouter_Factory create(Provider<DnsFilterEngine> dnsEngineProvider,
      Provider<StatsRepository> statsRepoProvider,
      Provider<SettingsRepository> settingsRepoProvider) {
    return new PacketRouter_Factory(dnsEngineProvider, statsRepoProvider, settingsRepoProvider);
  }

  public static PacketRouter newInstance(DnsFilterEngine dnsEngine, StatsRepository statsRepo,
      SettingsRepository settingsRepo) {
    return new PacketRouter(dnsEngine, statsRepo, settingsRepo);
  }
}
