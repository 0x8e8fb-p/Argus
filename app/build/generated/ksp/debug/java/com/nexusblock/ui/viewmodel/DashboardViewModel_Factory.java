package com.nexusblock.ui.viewmodel;

import android.app.Application;
import com.nexusblock.data.repository.BlocklistRepository;
import com.nexusblock.data.repository.SettingsRepository;
import com.nexusblock.data.repository.StatsRepository;
import com.nexusblock.engine.PacketRouter;
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
public final class DashboardViewModel_Factory implements Factory<DashboardViewModel> {
  private final Provider<Application> applicationProvider;

  private final Provider<SettingsRepository> settingsRepoProvider;

  private final Provider<StatsRepository> statsRepoProvider;

  private final Provider<BlocklistRepository> blocklistRepoProvider;

  private final Provider<PacketRouter> packetRouterProvider;

  public DashboardViewModel_Factory(Provider<Application> applicationProvider,
      Provider<SettingsRepository> settingsRepoProvider,
      Provider<StatsRepository> statsRepoProvider,
      Provider<BlocklistRepository> blocklistRepoProvider,
      Provider<PacketRouter> packetRouterProvider) {
    this.applicationProvider = applicationProvider;
    this.settingsRepoProvider = settingsRepoProvider;
    this.statsRepoProvider = statsRepoProvider;
    this.blocklistRepoProvider = blocklistRepoProvider;
    this.packetRouterProvider = packetRouterProvider;
  }

  @Override
  public DashboardViewModel get() {
    return newInstance(applicationProvider.get(), settingsRepoProvider.get(), statsRepoProvider.get(), blocklistRepoProvider.get(), packetRouterProvider.get());
  }

  public static DashboardViewModel_Factory create(Provider<Application> applicationProvider,
      Provider<SettingsRepository> settingsRepoProvider,
      Provider<StatsRepository> statsRepoProvider,
      Provider<BlocklistRepository> blocklistRepoProvider,
      Provider<PacketRouter> packetRouterProvider) {
    return new DashboardViewModel_Factory(applicationProvider, settingsRepoProvider, statsRepoProvider, blocklistRepoProvider, packetRouterProvider);
  }

  public static DashboardViewModel newInstance(Application application,
      SettingsRepository settingsRepo, StatsRepository statsRepo, BlocklistRepository blocklistRepo,
      PacketRouter packetRouter) {
    return new DashboardViewModel(application, settingsRepo, statsRepo, blocklistRepo, packetRouter);
  }
}
