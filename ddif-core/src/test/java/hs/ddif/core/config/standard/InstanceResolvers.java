package hs.ddif.core.config.standard;

import hs.ddif.core.config.scope.SingletonScopeResolver;
import hs.ddif.core.inject.injectable.ClassInjectableFactory;
import hs.ddif.core.inject.injectable.InjectableFactories;
import hs.ddif.core.inject.store.InjectableStore;
import hs.ddif.core.instantiation.DefaultInstantiationContext;
import hs.ddif.core.instantiation.InstanceFactories;
import hs.ddif.core.instantiation.InstantiatorBindingMap;
import hs.ddif.core.instantiation.InstantiatorFactory;
import hs.ddif.core.instantiation.ScopeResolverManager;
import hs.ddif.core.instantiation.ScopeResolverManagers;

import java.util.List;

public class InstanceResolvers {
  private static final ClassInjectableFactory FACTORY = InjectableFactories.forClass();

  public static DefaultInstanceResolver create() {
    SingletonScopeResolver scopeResolver = new SingletonScopeResolver();
    InstantiatorFactory instantiatorFactory = InstanceFactories.create();
    InstantiatorBindingMap instantiatorBindingMap = new InstantiatorBindingMap(instantiatorFactory);
    ScopeResolverManager scopeResolverManager = ScopeResolverManagers.create(scopeResolver);
    InjectableStore store = new InjectableStore(instantiatorBindingMap, scopeResolverManager);
    AutoDiscoveringGatherer gatherer = new AutoDiscoveringGatherer(false, List.of(), FACTORY);
    DefaultInstantiationContext instantiationContext = new DefaultInstantiationContext(store, instantiatorBindingMap, scopeResolverManager);

    return new DefaultInstanceResolver(store, gatherer, instantiationContext, instantiatorFactory);
  }

  public static DefaultInstanceResolver createWithConsistencyPolicy() {
    SingletonScopeResolver scopeResolver = new SingletonScopeResolver();
    AutoDiscoveringGatherer gatherer = new AutoDiscoveringGatherer(false, List.of(), FACTORY);
    ScopeResolverManager scopeResolverManager = ScopeResolverManagers.create(scopeResolver);
    InstantiatorFactory instantiatorFactory = InstanceFactories.create();
    InstantiatorBindingMap instantiatorBindingMap = new InstantiatorBindingMap(instantiatorFactory);
    InjectableStore store = new InjectableStore(instantiatorBindingMap, scopeResolverManager);
    DefaultInstantiationContext instantiationContext = new DefaultInstantiationContext(store, instantiatorBindingMap, scopeResolverManager);

    return new DefaultInstanceResolver(store, gatherer, instantiationContext, instantiatorFactory);
  }
}
