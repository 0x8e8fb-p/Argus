package com.nexusblock.engine;

import android.content.Context;
import com.nexusblock.data.db.CustomRuleDao;
import com.nexusblock.data.repository.BlocklistRepository;
import com.nexusblock.data.repository.SettingsRepository;
import com.nexusblock.data.repository.StatsRepository;
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

  private final Provider<BlocklistRepository> blocklistRepoProvider;

  private final Provider<StatsRepository> statsRepoProvider;

  private final Provider<CustomRuleDao> customRuleDaoProvider;

  private final Provider<OkHttpClient> okHttpClientProvider;

  private final Provider<ConnectionTracker> connectionTrackerProvider;

  private final Provider<SettingsRepository> settingsRepoProvider;

  public DnsFilterEngine_Factory(Provider<Context> contextProvider,
      Provider<BlocklistRepository> blocklistRepoProvider,
      Provider<StatsRepository> statsRepoProvider, Provider<CustomRuleDao> customRuleDaoProvider,
      Provider<OkHttpClient> okHttpClientProvider,
      Provider<ConnectionTracker> connectionTrackerProvider,
      Provider<SettingsRepository> settingsRepoProvider) {
    this.contextProvider = contextProvider;
    this.blocklistRepoProvider = blocklistRepoProvider;
    this.statsRepoProvider = statsRepoProvider;
    this.customRuleDaoProvider = customRuleDaoProvider;
    this.okHttpClientProvider = okHttpClientProvider;
    this.connectionTrackerProvider = connectionTrackerProvider;
    this.settingsRepoProvider = settingsRepoProvider;
  }

  @Override
  public DnsFilterEngine get() {
    return newInstance(contextProvider.get(), blocklistRepoProvider.get(), statsRepoProvider.get(), customRuleDaoProvider.get(), okHttpClientProvider.get(), connectionTrackerProvider.get(), settingsRepoProvider.get());
  }

  public static DnsFilterEngine_Factory create(Provider<Context> contextProvider,
      Provider<BlocklistRepository> blocklistRepoProvider,
      Provider<StatsRepository> statsRepoProvider, Provider<CustomRuleDao> customRuleDaoProvider,
      Provider<OkHttpClient> okHttpClientProvider,
      Provider<ConnectionTracker> connectionTrackerProvider,
      Provider<SettingsRepository> settingsRepoProvider) {
    return new DnsFilterEngine_Factory(contextProvider, blocklistRepoProvider, statsRepoProvider, customRuleDaoProvider, okHttpClientProvider, connectionTrackerProvider, settingsRepoProvider);
  }

  public static DnsFilterEngine newInstance(Context context, BlocklistRepository blocklistRepo,
      StatsRepository statsRepo, CustomRuleDao customRuleDao, OkHttpClient okHttpClient,
      ConnectionTracker connectionTracker, SettingsRepository settingsRepo) {
    return new DnsFilterEngine(context, blocklistRepo, statsRepo, customRuleDao, okHttpClient, connectionTracker, settingsRepo);
  }
}
