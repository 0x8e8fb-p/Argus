package com.nexusblock.data.repository;

import com.nexusblock.data.db.BlockedDomainDao;
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
public final class BlocklistRepository_Factory implements Factory<BlocklistRepository> {
  private final Provider<BlockedDomainDao> domainDaoProvider;

  public BlocklistRepository_Factory(Provider<BlockedDomainDao> domainDaoProvider) {
    this.domainDaoProvider = domainDaoProvider;
  }

  @Override
  public BlocklistRepository get() {
    return newInstance(domainDaoProvider.get());
  }

  public static BlocklistRepository_Factory create(Provider<BlockedDomainDao> domainDaoProvider) {
    return new BlocklistRepository_Factory(domainDaoProvider);
  }

  public static BlocklistRepository newInstance(BlockedDomainDao domainDao) {
    return new BlocklistRepository(domainDao);
  }
}
