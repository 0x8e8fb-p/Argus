package com.nexusblock.engine.transformers;

import android.content.Context;
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
public final class TransformerEngine_Factory implements Factory<TransformerEngine> {
  private final Provider<Context> contextProvider;

  private final Provider<QuickJsTransformer> quickJsTransformerProvider;

  public TransformerEngine_Factory(Provider<Context> contextProvider,
      Provider<QuickJsTransformer> quickJsTransformerProvider) {
    this.contextProvider = contextProvider;
    this.quickJsTransformerProvider = quickJsTransformerProvider;
  }

  @Override
  public TransformerEngine get() {
    return newInstance(contextProvider.get(), quickJsTransformerProvider.get());
  }

  public static TransformerEngine_Factory create(Provider<Context> contextProvider,
      Provider<QuickJsTransformer> quickJsTransformerProvider) {
    return new TransformerEngine_Factory(contextProvider, quickJsTransformerProvider);
  }

  public static TransformerEngine newInstance(Context context,
      QuickJsTransformer quickJsTransformer) {
    return new TransformerEngine(context, quickJsTransformer);
  }
}
