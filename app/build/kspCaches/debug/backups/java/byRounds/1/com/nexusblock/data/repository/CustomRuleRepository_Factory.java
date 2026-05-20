package com.nexusblock.data.repository;

import com.nexusblock.data.db.CustomRuleDao;
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
public final class CustomRuleRepository_Factory implements Factory<CustomRuleRepository> {
  private final Provider<CustomRuleDao> customRuleDaoProvider;

  public CustomRuleRepository_Factory(Provider<CustomRuleDao> customRuleDaoProvider) {
    this.customRuleDaoProvider = customRuleDaoProvider;
  }

  @Override
  public CustomRuleRepository get() {
    return newInstance(customRuleDaoProvider.get());
  }

  public static CustomRuleRepository_Factory create(Provider<CustomRuleDao> customRuleDaoProvider) {
    return new CustomRuleRepository_Factory(customRuleDaoProvider);
  }

  public static CustomRuleRepository newInstance(CustomRuleDao customRuleDao) {
    return new CustomRuleRepository(customRuleDao);
  }
}
