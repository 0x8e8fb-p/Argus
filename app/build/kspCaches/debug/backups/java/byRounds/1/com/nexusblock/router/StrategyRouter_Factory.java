package com.nexusblock.router;

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
public final class StrategyRouter_Factory implements Factory<StrategyRouter> {
  private final Provider<Context> contextProvider;

  private final Provider<StrategyMatrix> strategyMatrixProvider;

  public StrategyRouter_Factory(Provider<Context> contextProvider,
      Provider<StrategyMatrix> strategyMatrixProvider) {
    this.contextProvider = contextProvider;
    this.strategyMatrixProvider = strategyMatrixProvider;
  }

  @Override
  public StrategyRouter get() {
    return newInstance(contextProvider.get(), strategyMatrixProvider.get());
  }

  public static StrategyRouter_Factory create(Provider<Context> contextProvider,
      Provider<StrategyMatrix> strategyMatrixProvider) {
    return new StrategyRouter_Factory(contextProvider, strategyMatrixProvider);
  }

  public static StrategyRouter newInstance(Context context, StrategyMatrix strategyMatrix) {
    return new StrategyRouter(context, strategyMatrix);
  }
}
