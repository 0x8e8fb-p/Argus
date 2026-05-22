package com.nexusblock.engine;

import android.content.Context;
import com.nexusblock.data.db.CustomRuleDao;
import com.nexusblock.data.repository.BlocklistRepository;
import com.nexusblock.data.repository.SettingsRepository;
import com.nexusblock.data.repository.StatsRepository;
import com.nexusblock.engine.dns.DnsProfileManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

@ScopeMetadata("javax.inject.Singleton")
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
public final class DnsFilterEngine_Factory implements Factory<DnsFilterEngine> {
  private final Provider<Context> contextProvider;

  private final Provider<StatsRepository> statsRepoProvider;

  private final Provider<CustomRuleDao> customRuleDaoProvider;

  private final Provider<BlocklistRepository> blocklistRepoProvider;

  private final Provider<SettingsRepository> settingsRepoProvider;

  private final Provider<DnsProfileManager> dnsProfileManagerProvider;

  private final Provider<OkHttpClient> okHttpClientProvider;

  private final Provider<ConnectionTracker> connectionTrackerProvider;

  public DnsFilterEngine_Factory(Provider<Context> contextProvider,
      Provider<StatsRepository> statsRepoProvider, Provider<CustomRuleDao> customRuleDaoProvider,
      Provider<BlocklistRepository> blocklistRepoProvider,
      Provider<SettingsRepository> settingsRepoProvider,
      Provider<DnsProfileManager> dnsProfileManagerProvider,
      Provider<OkHttpClient> okHttpClientProvider,
      Provider<ConnectionTracker> connectionTrackerProvider) {
    this.contextProvider = contextProvider;
    this.statsRepoProvider = statsRepoProvider;
    this.customRuleDaoProvider = customRuleDaoProvider;
    this.blocklistRepoProvider = blocklistRepoProvider;
    this.settingsRepoProvider = settingsRepoProvider;
    this.dnsProfileManagerProvider = dnsProfileManagerProvider;
    this.okHttpClientProvider = okHttpClientProvider;
    this.connectionTrackerProvider = connectionTrackerProvider;
  }

  @Override
  public DnsFilterEngine get() {
    return newInstance(contextProvider.get(), statsRepoProvider.get(), customRuleDaoProvider.get(), blocklistRepoProvider.get(), settingsRepoProvider.get(), dnsProfileManagerProvider.get(), okHttpClientProvider.get(), connectionTrackerProvider.get());
  }

  public static DnsFilterEngine_Factory create(Provider<Context> contextProvider,
      Provider<StatsRepository> statsRepoProvider, Provider<CustomRuleDao> customRuleDaoProvider,
      Provider<BlocklistRepository> blocklistRepoProvider,
      Provider<SettingsRepository> settingsRepoProvider,
      Provider<DnsProfileManager> dnsProfileManagerProvider,
      Provider<OkHttpClient> okHttpClientProvider,
      Provider<ConnectionTracker> connectionTrackerProvider) {
    return new DnsFilterEngine_Factory(contextProvider, statsRepoProvider, customRuleDaoProvider, blocklistRepoProvider, settingsRepoProvider, dnsProfileManagerProvider, okHttpClientProvider, connectionTrackerProvider);
  }

  public static DnsFilterEngine newInstance(Context context, StatsRepository statsRepo,
      CustomRuleDao customRuleDao, BlocklistRepository blocklistRepo,
      SettingsRepository settingsRepo, DnsProfileManager dnsProfileManager,
      OkHttpClient okHttpClient, ConnectionTracker connectionTracker) {
    return new DnsFilterEngine(context, statsRepo, customRuleDao, blocklistRepo, settingsRepo, dnsProfileManager, okHttpClient, connectionTracker);
  }
}
