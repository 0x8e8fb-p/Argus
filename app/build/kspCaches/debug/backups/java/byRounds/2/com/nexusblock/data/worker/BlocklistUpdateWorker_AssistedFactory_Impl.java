package com.nexusblock.data.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.InstanceFactory;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class BlocklistUpdateWorker_AssistedFactory_Impl implements BlocklistUpdateWorker_AssistedFactory {
  private final BlocklistUpdateWorker_Factory delegateFactory;

  BlocklistUpdateWorker_AssistedFactory_Impl(BlocklistUpdateWorker_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public BlocklistUpdateWorker create(Context p0, WorkerParameters p1) {
    return delegateFactory.get(p0, p1);
  }

  public static Provider<BlocklistUpdateWorker_AssistedFactory> create(
      BlocklistUpdateWorker_Factory delegateFactory) {
    return InstanceFactory.create(new BlocklistUpdateWorker_AssistedFactory_Impl(delegateFactory));
  }

  public static dagger.internal.Provider<BlocklistUpdateWorker_AssistedFactory> createFactoryProvider(
      BlocklistUpdateWorker_Factory delegateFactory) {
    return InstanceFactory.create(new BlocklistUpdateWorker_AssistedFactory_Impl(delegateFactory));
  }
}
