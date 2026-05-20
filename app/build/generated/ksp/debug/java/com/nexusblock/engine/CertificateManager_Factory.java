package com.nexusblock.engine;

import android.content.Context;
import com.nexusblock.data.repository.SettingsRepository;
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
public final class CertificateManager_Factory implements Factory<CertificateManager> {
  private final Provider<Context> contextProvider;

  private final Provider<SettingsRepository> settingsRepoProvider;

  public CertificateManager_Factory(Provider<Context> contextProvider,
      Provider<SettingsRepository> settingsRepoProvider) {
    this.contextProvider = contextProvider;
    this.settingsRepoProvider = settingsRepoProvider;
  }

  @Override
  public CertificateManager get() {
    return newInstance(contextProvider.get(), settingsRepoProvider.get());
  }

  public static CertificateManager_Factory create(Provider<Context> contextProvider,
      Provider<SettingsRepository> settingsRepoProvider) {
    return new CertificateManager_Factory(contextProvider, settingsRepoProvider);
  }

  public static CertificateManager newInstance(Context context, SettingsRepository settingsRepo) {
    return new CertificateManager(context, settingsRepo);
  }
}
