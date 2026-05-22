package com.nexusblock.engine.dns;

import android.content.Context;
import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import com.nexusblock.data.repository.SettingsRepository;
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
public final class DnsProfileManager_Factory implements Factory<DnsProfileManager> {
  private final Provider<Context> contextProvider;

  private final Provider<DataStore<Preferences>> dataStoreProvider;

  private final Provider<SettingsRepository> settingsRepoProvider;

  private final Provider<OkHttpClient> okHttpClientProvider;

  public DnsProfileManager_Factory(Provider<Context> contextProvider,
      Provider<DataStore<Preferences>> dataStoreProvider,
      Provider<SettingsRepository> settingsRepoProvider,
      Provider<OkHttpClient> okHttpClientProvider) {
    this.contextProvider = contextProvider;
    this.dataStoreProvider = dataStoreProvider;
    this.settingsRepoProvider = settingsRepoProvider;
    this.okHttpClientProvider = okHttpClientProvider;
  }

  @Override
  public DnsProfileManager get() {
    return newInstance(contextProvider.get(), dataStoreProvider.get(), settingsRepoProvider.get(), okHttpClientProvider.get());
  }

  public static DnsProfileManager_Factory create(Provider<Context> contextProvider,
      Provider<DataStore<Preferences>> dataStoreProvider,
      Provider<SettingsRepository> settingsRepoProvider,
      Provider<OkHttpClient> okHttpClientProvider) {
    return new DnsProfileManager_Factory(contextProvider, dataStoreProvider, settingsRepoProvider, okHttpClientProvider);
  }

  public static DnsProfileManager newInstance(Context context, DataStore<Preferences> dataStore,
      SettingsRepository settingsRepo, OkHttpClient okHttpClient) {
    return new DnsProfileManager(context, dataStore, settingsRepo, okHttpClient);
  }
}
