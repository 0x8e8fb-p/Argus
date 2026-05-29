package com.nexusblock.engine;

import com.nexusblock.data.repository.SettingsRepository;
import com.nexusblock.data.repository.StatsRepository;
import com.nexusblock.engine.network.FlowCache;
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

  private final Provider<TcpRelayEngine> tcpRelayProvider;

  private final Provider<UdpRelayEngine> udpRelayProvider;

  private final Provider<FlowCache> flowCacheProvider;

  public PacketRouter_Factory(Provider<DnsFilterEngine> dnsEngineProvider,
      Provider<StatsRepository> statsRepoProvider,
      Provider<SettingsRepository> settingsRepoProvider, Provider<TcpRelayEngine> tcpRelayProvider,
      Provider<UdpRelayEngine> udpRelayProvider, Provider<FlowCache> flowCacheProvider) {
    this.dnsEngineProvider = dnsEngineProvider;
    this.statsRepoProvider = statsRepoProvider;
    this.settingsRepoProvider = settingsRepoProvider;
    this.tcpRelayProvider = tcpRelayProvider;
    this.udpRelayProvider = udpRelayProvider;
    this.flowCacheProvider = flowCacheProvider;
  }

  @Override
  public PacketRouter get() {
    return newInstance(dnsEngineProvider.get(), statsRepoProvider.get(), settingsRepoProvider.get(), tcpRelayProvider.get(), udpRelayProvider.get(), flowCacheProvider.get());
  }

  public static PacketRouter_Factory create(Provider<DnsFilterEngine> dnsEngineProvider,
      Provider<StatsRepository> statsRepoProvider,
      Provider<SettingsRepository> settingsRepoProvider, Provider<TcpRelayEngine> tcpRelayProvider,
      Provider<UdpRelayEngine> udpRelayProvider, Provider<FlowCache> flowCacheProvider) {
    return new PacketRouter_Factory(dnsEngineProvider, statsRepoProvider, settingsRepoProvider, tcpRelayProvider, udpRelayProvider, flowCacheProvider);
  }

  public static PacketRouter newInstance(DnsFilterEngine dnsEngine, StatsRepository statsRepo,
      SettingsRepository settingsRepo, TcpRelayEngine tcpRelay, UdpRelayEngine udpRelay,
      FlowCache flowCache) {
    return new PacketRouter(dnsEngine, statsRepo, settingsRepo, tcpRelay, udpRelay, flowCache);
  }
}
