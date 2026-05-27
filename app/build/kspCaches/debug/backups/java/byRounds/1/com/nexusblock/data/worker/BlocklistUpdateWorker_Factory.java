package com.nexusblock.data.worker;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.work.WorkerParameters;
import com.nexusblock.data.repository.BlocklistRepository;
import com.nexusblock.engine.DnsFilterEngine;
import dagger.internal.DaggerGenerated;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

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
public final class BlocklistUpdateWorker_Factory {
  private final Provider<BlocklistRepository> blocklistRepoProvider;

  private final Provider<DnsFilterEngine> dnsEngineProvider;

  private final Provider<OkHttpClient> okHttpClientProvider;

  private final Provider<SharedPreferences> sharedPreferencesProvider;

  public BlocklistUpdateWorker_Factory(Provider<BlocklistRepository> blocklistRepoProvider,
      Provider<DnsFilterEngine> dnsEngineProvider, Provider<OkHttpClient> okHttpClientProvider,
      Provider<SharedPreferences> sharedPreferencesProvider) {
    this.blocklistRepoProvider = blocklistRepoProvider;
    this.dnsEngineProvider = dnsEngineProvider;
    this.okHttpClientProvider = okHttpClientProvider;
    this.sharedPreferencesProvider = sharedPreferencesProvider;
  }

  public BlocklistUpdateWorker get(Context context, WorkerParameters params) {
    return newInstance(context, params, blocklistRepoProvider.get(), dnsEngineProvider.get(), okHttpClientProvider.get(), sharedPreferencesProvider.get());
  }

  public static BlocklistUpdateWorker_Factory create(
      Provider<BlocklistRepository> blocklistRepoProvider,
      Provider<DnsFilterEngine> dnsEngineProvider, Provider<OkHttpClient> okHttpClientProvider,
      Provider<SharedPreferences> sharedPreferencesProvider) {
    return new BlocklistUpdateWorker_Factory(blocklistRepoProvider, dnsEngineProvider, okHttpClientProvider, sharedPreferencesProvider);
  }

  public static BlocklistUpdateWorker newInstance(Context context, WorkerParameters params,
      BlocklistRepository blocklistRepo, DnsFilterEngine dnsEngine, OkHttpClient okHttpClient,
      SharedPreferences sharedPreferences) {
    return new BlocklistUpdateWorker(context, params, blocklistRepo, dnsEngine, okHttpClient, sharedPreferences);
  }
}
