package com.nexusblock.router;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class StrategyMatrix_Factory implements Factory<StrategyMatrix> {
  @Override
  public StrategyMatrix get() {
    return newInstance();
  }

  public static StrategyMatrix_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static StrategyMatrix newInstance() {
    return new StrategyMatrix();
  }

  private static final class InstanceHolder {
    private static final StrategyMatrix_Factory INSTANCE = new StrategyMatrix_Factory();
  }
}
