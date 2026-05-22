package com.nexusblock;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import androidx.fragment.app.Fragment;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.hilt.work.WorkerAssistedFactory;
import androidx.hilt.work.WorkerFactoryModule_ProvideFactoryFactory;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;
import com.nexusblock.data.db.AppDatabase;
import com.nexusblock.data.db.BlockedDomainDao;
import com.nexusblock.data.db.BlockedEventDao;
import com.nexusblock.data.db.CustomRuleDao;
import com.nexusblock.data.repository.BlocklistRepository;
import com.nexusblock.data.repository.CustomRuleRepository;
import com.nexusblock.data.repository.SettingsRepository;
import com.nexusblock.data.repository.StatsRepository;
import com.nexusblock.data.worker.BlocklistUpdateWorker;
import com.nexusblock.data.worker.BlocklistUpdateWorker_AssistedFactory;
import com.nexusblock.di.AppModule_ProvideDataStoreFactory;
import com.nexusblock.di.AppModule_ProvideOkHttpClientFactory;
import com.nexusblock.di.AppModule_ProvideSharedPreferencesFactory;
import com.nexusblock.di.DatabaseModule_ProvideBlockedDomainDaoFactory;
import com.nexusblock.di.DatabaseModule_ProvideBlockedEventDaoFactory;
import com.nexusblock.di.DatabaseModule_ProvideCustomRuleDaoFactory;
import com.nexusblock.di.DatabaseModule_ProvideDatabaseFactory;
import com.nexusblock.engine.ConnectionTracker;
import com.nexusblock.engine.DnsFilterEngine;
import com.nexusblock.engine.PacketRouter;
import com.nexusblock.engine.dns.DnsProfileManager;
import com.nexusblock.service.BootReceiver;
import com.nexusblock.service.BootReceiver_MembersInjector;
import com.nexusblock.service.NexusVpnService;
import com.nexusblock.service.NexusVpnService_MembersInjector;
import com.nexusblock.service.VpnWatchdogService;
import com.nexusblock.service.VpnWatchdogService_MembersInjector;
import com.nexusblock.ui.MainActivity;
import com.nexusblock.ui.MainActivity_MembersInjector;
import com.nexusblock.ui.viewmodel.BlocklistViewModel;
import com.nexusblock.ui.viewmodel.BlocklistViewModel_HiltModules;
import com.nexusblock.ui.viewmodel.CustomRulesViewModel;
import com.nexusblock.ui.viewmodel.CustomRulesViewModel_HiltModules;
import com.nexusblock.ui.viewmodel.DashboardViewModel;
import com.nexusblock.ui.viewmodel.DashboardViewModel_HiltModules;
import com.nexusblock.ui.viewmodel.FirewallViewModel;
import com.nexusblock.ui.viewmodel.FirewallViewModel_HiltModules;
import com.nexusblock.ui.viewmodel.LogsViewModel;
import com.nexusblock.ui.viewmodel.LogsViewModel_HiltModules;
import com.nexusblock.ui.viewmodel.SettingsViewModel;
import com.nexusblock.ui.viewmodel.SettingsViewModel_HiltModules;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideApplicationFactory;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.IdentifierNameString;
import dagger.internal.KeepFieldType;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.MapBuilder;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.SingleCheck;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import okhttp3.OkHttpClient;

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
public final class DaggerNexusBlockApplication_HiltComponents_SingletonC {
  private DaggerNexusBlockApplication_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public NexusBlockApplication_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements NexusBlockApplication_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public NexusBlockApplication_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements NexusBlockApplication_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public NexusBlockApplication_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements NexusBlockApplication_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public NexusBlockApplication_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements NexusBlockApplication_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public NexusBlockApplication_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements NexusBlockApplication_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public NexusBlockApplication_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements NexusBlockApplication_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public NexusBlockApplication_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements NexusBlockApplication_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public NexusBlockApplication_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends NexusBlockApplication_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends NexusBlockApplication_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends NexusBlockApplication_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends NexusBlockApplication_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
      injectMainActivity2(mainActivity);
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(MapBuilder.<String, Boolean>newMapBuilder(6).put(LazyClassKeyProvider.com_nexusblock_ui_viewmodel_BlocklistViewModel, BlocklistViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_nexusblock_ui_viewmodel_CustomRulesViewModel, CustomRulesViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_nexusblock_ui_viewmodel_DashboardViewModel, DashboardViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_nexusblock_ui_viewmodel_FirewallViewModel, FirewallViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_nexusblock_ui_viewmodel_LogsViewModel, LogsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_nexusblock_ui_viewmodel_SettingsViewModel, SettingsViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    private MainActivity injectMainActivity2(MainActivity instance) {
      MainActivity_MembersInjector.injectSettingsRepo(instance, singletonCImpl.settingsRepositoryProvider.get());
      return instance;
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_nexusblock_ui_viewmodel_CustomRulesViewModel = "com.nexusblock.ui.viewmodel.CustomRulesViewModel";

      static String com_nexusblock_ui_viewmodel_LogsViewModel = "com.nexusblock.ui.viewmodel.LogsViewModel";

      static String com_nexusblock_ui_viewmodel_FirewallViewModel = "com.nexusblock.ui.viewmodel.FirewallViewModel";

      static String com_nexusblock_ui_viewmodel_DashboardViewModel = "com.nexusblock.ui.viewmodel.DashboardViewModel";

      static String com_nexusblock_ui_viewmodel_SettingsViewModel = "com.nexusblock.ui.viewmodel.SettingsViewModel";

      static String com_nexusblock_ui_viewmodel_BlocklistViewModel = "com.nexusblock.ui.viewmodel.BlocklistViewModel";

      @KeepFieldType
      CustomRulesViewModel com_nexusblock_ui_viewmodel_CustomRulesViewModel2;

      @KeepFieldType
      LogsViewModel com_nexusblock_ui_viewmodel_LogsViewModel2;

      @KeepFieldType
      FirewallViewModel com_nexusblock_ui_viewmodel_FirewallViewModel2;

      @KeepFieldType
      DashboardViewModel com_nexusblock_ui_viewmodel_DashboardViewModel2;

      @KeepFieldType
      SettingsViewModel com_nexusblock_ui_viewmodel_SettingsViewModel2;

      @KeepFieldType
      BlocklistViewModel com_nexusblock_ui_viewmodel_BlocklistViewModel2;
    }
  }

  private static final class ViewModelCImpl extends NexusBlockApplication_HiltComponents.ViewModelC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<BlocklistViewModel> blocklistViewModelProvider;

    private Provider<CustomRulesViewModel> customRulesViewModelProvider;

    private Provider<DashboardViewModel> dashboardViewModelProvider;

    private Provider<FirewallViewModel> firewallViewModelProvider;

    private Provider<LogsViewModel> logsViewModelProvider;

    private Provider<SettingsViewModel> settingsViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;

      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.blocklistViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.customRulesViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.dashboardViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.firewallViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.logsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.settingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(MapBuilder.<String, javax.inject.Provider<ViewModel>>newMapBuilder(6).put(LazyClassKeyProvider.com_nexusblock_ui_viewmodel_BlocklistViewModel, ((Provider) blocklistViewModelProvider)).put(LazyClassKeyProvider.com_nexusblock_ui_viewmodel_CustomRulesViewModel, ((Provider) customRulesViewModelProvider)).put(LazyClassKeyProvider.com_nexusblock_ui_viewmodel_DashboardViewModel, ((Provider) dashboardViewModelProvider)).put(LazyClassKeyProvider.com_nexusblock_ui_viewmodel_FirewallViewModel, ((Provider) firewallViewModelProvider)).put(LazyClassKeyProvider.com_nexusblock_ui_viewmodel_LogsViewModel, ((Provider) logsViewModelProvider)).put(LazyClassKeyProvider.com_nexusblock_ui_viewmodel_SettingsViewModel, ((Provider) settingsViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return Collections.<Class<?>, Object>emptyMap();
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_nexusblock_ui_viewmodel_SettingsViewModel = "com.nexusblock.ui.viewmodel.SettingsViewModel";

      static String com_nexusblock_ui_viewmodel_FirewallViewModel = "com.nexusblock.ui.viewmodel.FirewallViewModel";

      static String com_nexusblock_ui_viewmodel_BlocklistViewModel = "com.nexusblock.ui.viewmodel.BlocklistViewModel";

      static String com_nexusblock_ui_viewmodel_CustomRulesViewModel = "com.nexusblock.ui.viewmodel.CustomRulesViewModel";

      static String com_nexusblock_ui_viewmodel_DashboardViewModel = "com.nexusblock.ui.viewmodel.DashboardViewModel";

      static String com_nexusblock_ui_viewmodel_LogsViewModel = "com.nexusblock.ui.viewmodel.LogsViewModel";

      @KeepFieldType
      SettingsViewModel com_nexusblock_ui_viewmodel_SettingsViewModel2;

      @KeepFieldType
      FirewallViewModel com_nexusblock_ui_viewmodel_FirewallViewModel2;

      @KeepFieldType
      BlocklistViewModel com_nexusblock_ui_viewmodel_BlocklistViewModel2;

      @KeepFieldType
      CustomRulesViewModel com_nexusblock_ui_viewmodel_CustomRulesViewModel2;

      @KeepFieldType
      DashboardViewModel com_nexusblock_ui_viewmodel_DashboardViewModel2;

      @KeepFieldType
      LogsViewModel com_nexusblock_ui_viewmodel_LogsViewModel2;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.nexusblock.ui.viewmodel.BlocklistViewModel 
          return (T) new BlocklistViewModel(ApplicationContextModule_ProvideApplicationFactory.provideApplication(singletonCImpl.applicationContextModule), singletonCImpl.blocklistRepositoryProvider.get(), singletonCImpl.dnsFilterEngineProvider.get());

          case 1: // com.nexusblock.ui.viewmodel.CustomRulesViewModel 
          return (T) new CustomRulesViewModel(singletonCImpl.customRuleRepositoryProvider.get());

          case 2: // com.nexusblock.ui.viewmodel.DashboardViewModel 
          return (T) new DashboardViewModel(ApplicationContextModule_ProvideApplicationFactory.provideApplication(singletonCImpl.applicationContextModule), singletonCImpl.settingsRepositoryProvider.get(), singletonCImpl.statsRepositoryProvider.get(), singletonCImpl.blocklistRepositoryProvider.get(), singletonCImpl.packetRouterProvider.get());

          case 3: // com.nexusblock.ui.viewmodel.FirewallViewModel 
          return (T) new FirewallViewModel(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.settingsRepositoryProvider.get());

          case 4: // com.nexusblock.ui.viewmodel.LogsViewModel 
          return (T) new LogsViewModel(singletonCImpl.statsRepositoryProvider.get());

          case 5: // com.nexusblock.ui.viewmodel.SettingsViewModel 
          return (T) new SettingsViewModel(ApplicationContextModule_ProvideApplicationFactory.provideApplication(singletonCImpl.applicationContextModule), singletonCImpl.settingsRepositoryProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends NexusBlockApplication_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends NexusBlockApplication_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }

    @Override
    public void injectNexusVpnService(NexusVpnService nexusVpnService) {
      injectNexusVpnService2(nexusVpnService);
    }

    @Override
    public void injectVpnWatchdogService(VpnWatchdogService vpnWatchdogService) {
      injectVpnWatchdogService2(vpnWatchdogService);
    }

    private NexusVpnService injectNexusVpnService2(NexusVpnService instance) {
      NexusVpnService_MembersInjector.injectPacketRouter(instance, singletonCImpl.packetRouterProvider.get());
      NexusVpnService_MembersInjector.injectDnsEngine(instance, singletonCImpl.dnsFilterEngineProvider.get());
      NexusVpnService_MembersInjector.injectSettingsRepo(instance, singletonCImpl.settingsRepositoryProvider.get());
      return instance;
    }

    private VpnWatchdogService injectVpnWatchdogService2(VpnWatchdogService instance2) {
      VpnWatchdogService_MembersInjector.injectSettingsRepo(instance2, singletonCImpl.settingsRepositoryProvider.get());
      return instance2;
    }
  }

  private static final class SingletonCImpl extends NexusBlockApplication_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<AppDatabase> provideDatabaseProvider;

    private Provider<BlocklistRepository> blocklistRepositoryProvider;

    private Provider<StatsRepository> statsRepositoryProvider;

    private Provider<DataStore<Preferences>> provideDataStoreProvider;

    private Provider<SharedPreferences> provideSharedPreferencesProvider;

    private Provider<SettingsRepository> settingsRepositoryProvider;

    private Provider<OkHttpClient> provideOkHttpClientProvider;

    private Provider<DnsProfileManager> dnsProfileManagerProvider;

    private Provider<ConnectionTracker> connectionTrackerProvider;

    private Provider<DnsFilterEngine> dnsFilterEngineProvider;

    private Provider<BlocklistUpdateWorker_AssistedFactory> blocklistUpdateWorker_AssistedFactoryProvider;

    private Provider<CustomRuleRepository> customRuleRepositoryProvider;

    private Provider<PacketRouter> packetRouterProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    private BlockedDomainDao blockedDomainDao() {
      return DatabaseModule_ProvideBlockedDomainDaoFactory.provideBlockedDomainDao(provideDatabaseProvider.get());
    }

    private BlockedEventDao blockedEventDao() {
      return DatabaseModule_ProvideBlockedEventDaoFactory.provideBlockedEventDao(provideDatabaseProvider.get());
    }

    private CustomRuleDao customRuleDao() {
      return DatabaseModule_ProvideCustomRuleDaoFactory.provideCustomRuleDao(provideDatabaseProvider.get());
    }

    private Map<String, javax.inject.Provider<WorkerAssistedFactory<? extends ListenableWorker>>> mapOfStringAndProviderOfWorkerAssistedFactoryOf(
        ) {
      return Collections.<String, javax.inject.Provider<WorkerAssistedFactory<? extends ListenableWorker>>>singletonMap("com.nexusblock.data.worker.BlocklistUpdateWorker", ((Provider) blocklistUpdateWorker_AssistedFactoryProvider));
    }

    private HiltWorkerFactory hiltWorkerFactory() {
      return WorkerFactoryModule_ProvideFactoryFactory.provideFactory(mapOfStringAndProviderOfWorkerAssistedFactoryOf());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.provideDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<AppDatabase>(singletonCImpl, 2));
      this.blocklistRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<BlocklistRepository>(singletonCImpl, 1));
      this.statsRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<StatsRepository>(singletonCImpl, 4));
      this.provideDataStoreProvider = DoubleCheck.provider(new SwitchingProvider<DataStore<Preferences>>(singletonCImpl, 6));
      this.provideSharedPreferencesProvider = DoubleCheck.provider(new SwitchingProvider<SharedPreferences>(singletonCImpl, 7));
      this.settingsRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<SettingsRepository>(singletonCImpl, 5));
      this.provideOkHttpClientProvider = DoubleCheck.provider(new SwitchingProvider<OkHttpClient>(singletonCImpl, 9));
      this.dnsProfileManagerProvider = DoubleCheck.provider(new SwitchingProvider<DnsProfileManager>(singletonCImpl, 8));
      this.connectionTrackerProvider = DoubleCheck.provider(new SwitchingProvider<ConnectionTracker>(singletonCImpl, 10));
      this.dnsFilterEngineProvider = DoubleCheck.provider(new SwitchingProvider<DnsFilterEngine>(singletonCImpl, 3));
      this.blocklistUpdateWorker_AssistedFactoryProvider = SingleCheck.provider(new SwitchingProvider<BlocklistUpdateWorker_AssistedFactory>(singletonCImpl, 0));
      this.customRuleRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<CustomRuleRepository>(singletonCImpl, 11));
      this.packetRouterProvider = DoubleCheck.provider(new SwitchingProvider<PacketRouter>(singletonCImpl, 12));
    }

    @Override
    public void injectNexusBlockApplication(NexusBlockApplication nexusBlockApplication) {
      injectNexusBlockApplication2(nexusBlockApplication);
    }

    @Override
    public void injectBootReceiver(BootReceiver bootReceiver) {
      injectBootReceiver2(bootReceiver);
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return Collections.<Boolean>emptySet();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private NexusBlockApplication injectNexusBlockApplication2(NexusBlockApplication instance) {
      NexusBlockApplication_MembersInjector.injectWorkerFactory(instance, hiltWorkerFactory());
      return instance;
    }

    private BootReceiver injectBootReceiver2(BootReceiver instance2) {
      BootReceiver_MembersInjector.injectSettingsRepo(instance2, settingsRepositoryProvider.get());
      return instance2;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.nexusblock.data.worker.BlocklistUpdateWorker_AssistedFactory 
          return (T) new BlocklistUpdateWorker_AssistedFactory() {
            @Override
            public BlocklistUpdateWorker create(Context context, WorkerParameters params) {
              return new BlocklistUpdateWorker(context, params, singletonCImpl.blocklistRepositoryProvider.get(), singletonCImpl.dnsFilterEngineProvider.get(), singletonCImpl.provideOkHttpClientProvider.get());
            }
          };

          case 1: // com.nexusblock.data.repository.BlocklistRepository 
          return (T) new BlocklistRepository(singletonCImpl.blockedDomainDao());

          case 2: // com.nexusblock.data.db.AppDatabase 
          return (T) DatabaseModule_ProvideDatabaseFactory.provideDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 3: // com.nexusblock.engine.DnsFilterEngine 
          return (T) new DnsFilterEngine(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.statsRepositoryProvider.get(), singletonCImpl.customRuleDao(), singletonCImpl.blocklistRepositoryProvider.get(), singletonCImpl.settingsRepositoryProvider.get(), singletonCImpl.dnsProfileManagerProvider.get(), singletonCImpl.provideOkHttpClientProvider.get(), singletonCImpl.connectionTrackerProvider.get());

          case 4: // com.nexusblock.data.repository.StatsRepository 
          return (T) new StatsRepository(singletonCImpl.blockedEventDao());

          case 5: // com.nexusblock.data.repository.SettingsRepository 
          return (T) new SettingsRepository(singletonCImpl.provideDataStoreProvider.get(), singletonCImpl.provideSharedPreferencesProvider.get());

          case 6: // androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> 
          return (T) AppModule_ProvideDataStoreFactory.provideDataStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 7: // android.content.SharedPreferences 
          return (T) AppModule_ProvideSharedPreferencesFactory.provideSharedPreferences(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 8: // com.nexusblock.engine.dns.DnsProfileManager 
          return (T) new DnsProfileManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.provideDataStoreProvider.get(), singletonCImpl.settingsRepositoryProvider.get(), singletonCImpl.provideOkHttpClientProvider.get());

          case 9: // okhttp3.OkHttpClient 
          return (T) AppModule_ProvideOkHttpClientFactory.provideOkHttpClient();

          case 10: // com.nexusblock.engine.ConnectionTracker 
          return (T) new ConnectionTracker(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 11: // com.nexusblock.data.repository.CustomRuleRepository 
          return (T) new CustomRuleRepository(singletonCImpl.customRuleDao());

          case 12: // com.nexusblock.engine.PacketRouter 
          return (T) new PacketRouter(singletonCImpl.dnsFilterEngineProvider.get(), singletonCImpl.statsRepositoryProvider.get(), singletonCImpl.settingsRepositoryProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
