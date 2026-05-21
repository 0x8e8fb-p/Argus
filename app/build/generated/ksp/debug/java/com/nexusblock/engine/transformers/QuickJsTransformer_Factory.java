package com.nexusblock.engine.transformers;

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
public final class QuickJsTransformer_Factory implements Factory<QuickJsTransformer> {
  private final Provider<Context> contextProvider;

  private final Provider<SettingsRepository> settingsRepoProvider;

  public QuickJsTransformer_Factory(Provider<Context> contextProvider,
      Provider<SettingsRepository> settingsRepoProvider) {
    this.contextProvider = contextProvider;
    this.settingsRepoProvider = settingsRepoProvider;
  }

  @Override
  public QuickJsTransformer get() {
    return newInstance(contextProvider.get(), settingsRepoProvider.get());
  }

  public static QuickJsTransformer_Factory create(Provider<Context> contextProvider,
      Provider<SettingsRepository> settingsRepoProvider) {
    return new QuickJsTransformer_Factory(contextProvider, settingsRepoProvider);
  }

  public static QuickJsTransformer newInstance(Context context, SettingsRepository settingsRepo) {
    return new QuickJsTransformer(context, settingsRepo);
  }
}
