package com.nexusblock;

import androidx.hilt.work.HiltWorkerFactory;
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
public final class NexusBlockApplication_MembersInjector implements MembersInjector<NexusBlockApplication> {
  private final Provider<HiltWorkerFactory> workerFactoryProvider;

  public NexusBlockApplication_MembersInjector(Provider<HiltWorkerFactory> workerFactoryProvider) {
    this.workerFactoryProvider = workerFactoryProvider;
  }

  public static MembersInjector<NexusBlockApplication> create(
      Provider<HiltWorkerFactory> workerFactoryProvider) {
    return new NexusBlockApplication_MembersInjector(workerFactoryProvider);
  }

  @Override
  public void injectMembers(NexusBlockApplication instance) {
    injectWorkerFactory(instance, workerFactoryProvider.get());
  }

  @InjectedFieldSignature("com.nexusblock.NexusBlockApplication.workerFactory")
  public static void injectWorkerFactory(NexusBlockApplication instance,
      HiltWorkerFactory workerFactory) {
    instance.workerFactory = workerFactory;
  }
}
