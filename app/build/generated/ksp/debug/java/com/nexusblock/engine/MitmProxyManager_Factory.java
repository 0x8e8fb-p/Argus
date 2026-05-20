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
public final class MitmProxyManager_Factory implements Factory<MitmProxyManager> {
  private final Provider<CertificateManager> certificateManagerProvider;

  private final Provider<StatsRepository> statsRepoProvider;

  private final Provider<SettingsRepository> settingsRepoProvider;

  public MitmProxyManager_Factory(Provider<CertificateManager> certificateManagerProvider,
      Provider<StatsRepository> statsRepoProvider,
      Provider<SettingsRepository> settingsRepoProvider) {
    this.certificateManagerProvider = certificateManagerProvider;
    this.statsRepoProvider = statsRepoProvider;
    this.settingsRepoProvider = settingsRepoProvider;
  }

  @Override
  public MitmProxyManager get() {
    return newInstance(certificateManagerProvider.get(), statsRepoProvider.get(), settingsRepoProvider.get());
  }

  public static MitmProxyManager_Factory create(
      Provider<CertificateManager> certificateManagerProvider,
      Provider<StatsRepository> statsRepoProvider,
      Provider<SettingsRepository> settingsRepoProvider) {
    return new MitmProxyManager_Factory(certificateManagerProvider, statsRepoProvider, settingsRepoProvider);
  }

  public static MitmProxyManager newInstance(CertificateManager certificateManager,
      StatsRepository statsRepo, SettingsRepository settingsRepo) {
    return new MitmProxyManager(certificateManager, statsRepo, settingsRepo);
  }
}
