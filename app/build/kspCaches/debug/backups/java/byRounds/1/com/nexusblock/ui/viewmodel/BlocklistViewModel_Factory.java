package com.nexusblock.ui.viewmodel;

import android.app.Application;
import com.nexusblock.data.repository.BlocklistRepository;
import com.nexusblock.engine.DnsFilterEngine;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class BlocklistViewModel_Factory implements Factory<BlocklistViewModel> {
  private final Provider<Application> applicationProvider;

  private final Provider<BlocklistRepository> blocklistRepoProvider;

  private final Provider<DnsFilterEngine> dnsEngineProvider;

  public BlocklistViewModel_Factory(Provider<Application> applicationProvider,
      Provider<BlocklistRepository> blocklistRepoProvider,
      Provider<DnsFilterEngine> dnsEngineProvider) {
    this.applicationProvider = applicationProvider;
    this.blocklistRepoProvider = blocklistRepoProvider;
    this.dnsEngineProvider = dnsEngineProvider;
  }

  @Override
  public BlocklistViewModel get() {
    return newInstance(applicationProvider.get(), blocklistRepoProvider.get(), dnsEngineProvider.get());
  }

  public static BlocklistViewModel_Factory create(Provider<Application> applicationProvider,
      Provider<BlocklistRepository> blocklistRepoProvider,
      Provider<DnsFilterEngine> dnsEngineProvider) {
    return new BlocklistViewModel_Factory(applicationProvider, blocklistRepoProvider, dnsEngineProvider);
  }

  public static BlocklistViewModel newInstance(Application application,
      BlocklistRepository blocklistRepo, DnsFilterEngine dnsEngine) {
    return new BlocklistViewModel(application, blocklistRepo, dnsEngine);
  }
}
