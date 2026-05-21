package com.nexusblock.engine.proxy;

import com.nexusblock.engine.CertificateManager;
import com.nexusblock.engine.transformers.TransformerEngine;
import com.nexusblock.router.StrategyRouter;
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
public final class ArgusProxyServer_Factory implements Factory<ArgusProxyServer> {
  private final Provider<CertificateManager> certificateManagerProvider;

  private final Provider<TransformerEngine> transformerEngineProvider;

  private final Provider<StrategyRouter> strategyRouterProvider;

  public ArgusProxyServer_Factory(Provider<CertificateManager> certificateManagerProvider,
      Provider<TransformerEngine> transformerEngineProvider,
      Provider<StrategyRouter> strategyRouterProvider) {
    this.certificateManagerProvider = certificateManagerProvider;
    this.transformerEngineProvider = transformerEngineProvider;
    this.strategyRouterProvider = strategyRouterProvider;
  }

  @Override
  public ArgusProxyServer get() {
    return newInstance(certificateManagerProvider.get(), transformerEngineProvider.get(), strategyRouterProvider.get());
  }

  public static ArgusProxyServer_Factory create(
      Provider<CertificateManager> certificateManagerProvider,
      Provider<TransformerEngine> transformerEngineProvider,
      Provider<StrategyRouter> strategyRouterProvider) {
    return new ArgusProxyServer_Factory(certificateManagerProvider, transformerEngineProvider, strategyRouterProvider);
  }

  public static ArgusProxyServer newInstance(CertificateManager certificateManager,
      TransformerEngine transformerEngine, StrategyRouter strategyRouter) {
    return new ArgusProxyServer(certificateManager, transformerEngine, strategyRouter);
  }
}
