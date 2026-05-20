package com.nexusblock.ui.viewmodel;

import com.nexusblock.data.repository.CustomRuleRepository;
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
public final class CustomRulesViewModel_Factory implements Factory<CustomRulesViewModel> {
  private final Provider<CustomRuleRepository> customRuleRepoProvider;

  public CustomRulesViewModel_Factory(Provider<CustomRuleRepository> customRuleRepoProvider) {
    this.customRuleRepoProvider = customRuleRepoProvider;
  }

  @Override
  public CustomRulesViewModel get() {
    return newInstance(customRuleRepoProvider.get());
  }

  public static CustomRulesViewModel_Factory create(
      Provider<CustomRuleRepository> customRuleRepoProvider) {
    return new CustomRulesViewModel_Factory(customRuleRepoProvider);
  }

  public static CustomRulesViewModel newInstance(CustomRuleRepository customRuleRepo) {
    return new CustomRulesViewModel(customRuleRepo);
  }
}
