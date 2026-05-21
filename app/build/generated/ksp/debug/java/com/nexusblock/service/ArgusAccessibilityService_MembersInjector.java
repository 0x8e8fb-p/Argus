package com.nexusblock.service;

import com.nexusblock.router.StrategyRouter;
import com.nexusblock.vision.LiteRTClassifier;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class ArgusAccessibilityService_MembersInjector implements MembersInjector<ArgusAccessibilityService> {
  private final Provider<StrategyRouter> strategyRouterProvider;

  private final Provider<LiteRTClassifier> liteRTClassifierProvider;

  public ArgusAccessibilityService_MembersInjector(Provider<StrategyRouter> strategyRouterProvider,
      Provider<LiteRTClassifier> liteRTClassifierProvider) {
    this.strategyRouterProvider = strategyRouterProvider;
    this.liteRTClassifierProvider = liteRTClassifierProvider;
  }

  public static MembersInjector<ArgusAccessibilityService> create(
      Provider<StrategyRouter> strategyRouterProvider,
      Provider<LiteRTClassifier> liteRTClassifierProvider) {
    return new ArgusAccessibilityService_MembersInjector(strategyRouterProvider, liteRTClassifierProvider);
  }

  @Override
  public void injectMembers(ArgusAccessibilityService instance) {
    injectStrategyRouter(instance, strategyRouterProvider.get());
    injectLiteRTClassifier(instance, liteRTClassifierProvider.get());
  }

  @InjectedFieldSignature("com.nexusblock.service.ArgusAccessibilityService.strategyRouter")
  public static void injectStrategyRouter(ArgusAccessibilityService instance,
      StrategyRouter strategyRouter) {
    instance.strategyRouter = strategyRouter;
  }

  @InjectedFieldSignature("com.nexusblock.service.ArgusAccessibilityService.liteRTClassifier")
  public static void injectLiteRTClassifier(ArgusAccessibilityService instance,
      LiteRTClassifier liteRTClassifier) {
    instance.liteRTClassifier = liteRTClassifier;
  }
}
