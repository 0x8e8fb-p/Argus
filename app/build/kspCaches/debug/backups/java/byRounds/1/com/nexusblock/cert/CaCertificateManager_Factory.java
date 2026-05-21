package com.nexusblock.cert;

import android.content.Context;
import com.nexusblock.data.repository.SettingsRepository;
import com.nexusblock.engine.CertificateManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class CaCertificateManager_Factory implements Factory<CaCertificateManager> {
  private final Provider<Context> contextProvider;

  private final Provider<SettingsRepository> settingsRepoProvider;

  private final Provider<CertificateManager> certEngineProvider;

  public CaCertificateManager_Factory(Provider<Context> contextProvider,
      Provider<SettingsRepository> settingsRepoProvider,
      Provider<CertificateManager> certEngineProvider) {
    this.contextProvider = contextProvider;
    this.settingsRepoProvider = settingsRepoProvider;
    this.certEngineProvider = certEngineProvider;
  }

  @Override
  public CaCertificateManager get() {
    return newInstance(contextProvider.get(), settingsRepoProvider.get(), certEngineProvider.get());
  }

  public static CaCertificateManager_Factory create(Provider<Context> contextProvider,
      Provider<SettingsRepository> settingsRepoProvider,
      Provider<CertificateManager> certEngineProvider) {
    return new CaCertificateManager_Factory(contextProvider, settingsRepoProvider, certEngineProvider);
  }

  public static CaCertificateManager newInstance(Context context, SettingsRepository settingsRepo,
      CertificateManager certEngine) {
    return new CaCertificateManager(context, settingsRepo, certEngine);
  }
}
