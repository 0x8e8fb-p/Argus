package com.nexusblock.service;

import com.nexusblock.data.repository.SettingsRepository;
import com.nexusblock.engine.DnsFilterEngine;
import com.nexusblock.engine.MitmProxyManager;
import com.nexusblock.engine.PacketRouter;
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
public final class NexusVpnService_MembersInjector implements MembersInjector<NexusVpnService> {
  private final Provider<PacketRouter> packetRouterProvider;

  private final Provider<DnsFilterEngine> dnsEngineProvider;

  private final Provider<MitmProxyManager> mitmProxyProvider;

  private final Provider<SettingsRepository> settingsRepoProvider;

  public NexusVpnService_MembersInjector(Provider<PacketRouter> packetRouterProvider,
      Provider<DnsFilterEngine> dnsEngineProvider, Provider<MitmProxyManager> mitmProxyProvider,
      Provider<SettingsRepository> settingsRepoProvider) {
    this.packetRouterProvider = packetRouterProvider;
    this.dnsEngineProvider = dnsEngineProvider;
    this.mitmProxyProvider = mitmProxyProvider;
    this.settingsRepoProvider = settingsRepoProvider;
  }

  public static MembersInjector<NexusVpnService> create(Provider<PacketRouter> packetRouterProvider,
      Provider<DnsFilterEngine> dnsEngineProvider, Provider<MitmProxyManager> mitmProxyProvider,
      Provider<SettingsRepository> settingsRepoProvider) {
    return new NexusVpnService_MembersInjector(packetRouterProvider, dnsEngineProvider, mitmProxyProvider, settingsRepoProvider);
  }

  @Override
  public void injectMembers(NexusVpnService instance) {
    injectPacketRouter(instance, packetRouterProvider.get());
    injectDnsEngine(instance, dnsEngineProvider.get());
    injectMitmProxy(instance, mitmProxyProvider.get());
    injectSettingsRepo(instance, settingsRepoProvider.get());
  }

  @InjectedFieldSignature("com.nexusblock.service.NexusVpnService.packetRouter")
  public static void injectPacketRouter(NexusVpnService instance, PacketRouter packetRouter) {
    instance.packetRouter = packetRouter;
  }

  @InjectedFieldSignature("com.nexusblock.service.NexusVpnService.dnsEngine")
  public static void injectDnsEngine(NexusVpnService instance, DnsFilterEngine dnsEngine) {
    instance.dnsEngine = dnsEngine;
  }

  @InjectedFieldSignature("com.nexusblock.service.NexusVpnService.mitmProxy")
  public static void injectMitmProxy(NexusVpnService instance, MitmProxyManager mitmProxy) {
    instance.mitmProxy = mitmProxy;
  }

  @InjectedFieldSignature("com.nexusblock.service.NexusVpnService.settingsRepo")
  public static void injectSettingsRepo(NexusVpnService instance, SettingsRepository settingsRepo) {
    instance.settingsRepo = settingsRepo;
  }
}
