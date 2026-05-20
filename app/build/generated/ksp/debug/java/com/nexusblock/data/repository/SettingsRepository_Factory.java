package com.nexusblock.data.repository;

import android.content.SharedPreferences;
import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
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
public final class SettingsRepository_Factory implements Factory<SettingsRepository> {
  private final Provider<DataStore<Preferences>> dataStoreProvider;

  private final Provider<SharedPreferences> legacyPrefsProvider;

  public SettingsRepository_Factory(Provider<DataStore<Preferences>> dataStoreProvider,
      Provider<SharedPreferences> legacyPrefsProvider) {
    this.dataStoreProvider = dataStoreProvider;
    this.legacyPrefsProvider = legacyPrefsProvider;
  }

  @Override
  public SettingsRepository get() {
    return newInstance(dataStoreProvider.get(), legacyPrefsProvider.get());
  }

  public static SettingsRepository_Factory create(
      Provider<DataStore<Preferences>> dataStoreProvider,
      Provider<SharedPreferences> legacyPrefsProvider) {
    return new SettingsRepository_Factory(dataStoreProvider, legacyPrefsProvider);
  }

  public static SettingsRepository newInstance(DataStore<Preferences> dataStore,
      SharedPreferences legacyPrefs) {
    return new SettingsRepository(dataStore, legacyPrefs);
  }
}
