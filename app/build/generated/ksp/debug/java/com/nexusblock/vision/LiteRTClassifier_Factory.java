package com.nexusblock.vision;

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
public final class LiteRTClassifier_Factory implements Factory<LiteRTClassifier> {
  private final Provider<Context> contextProvider;

  public LiteRTClassifier_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public LiteRTClassifier get() {
    return newInstance(contextProvider.get());
  }

  public static LiteRTClassifier_Factory create(Provider<Context> contextProvider) {
    return new LiteRTClassifier_Factory(contextProvider);
  }

  public static LiteRTClassifier newInstance(Context context) {
    return new LiteRTClassifier(context);
  }
}
