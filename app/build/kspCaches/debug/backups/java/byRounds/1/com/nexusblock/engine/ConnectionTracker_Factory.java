package com.nexusblock.engine;

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
public final class ConnectionTracker_Factory implements Factory<ConnectionTracker> {
  private final Provider<Context> contextProvider;

  public ConnectionTracker_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public ConnectionTracker get() {
    return newInstance(contextProvider.get());
  }

  public static ConnectionTracker_Factory create(Provider<Context> contextProvider) {
    return new ConnectionTracker_Factory(contextProvider);
  }

  public static ConnectionTracker newInstance(Context context) {
    return new ConnectionTracker(context);
  }
}
