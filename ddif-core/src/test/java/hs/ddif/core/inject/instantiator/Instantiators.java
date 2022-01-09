package hs.ddif.core.inject.instantiator;

import hs.ddif.core.config.scope.SingletonScopeResolver;
import hs.ddif.core.config.scope.WeakSingletonScopeResolver;
import hs.ddif.core.config.standard.AutoDiscoveringGatherer;
import hs.ddif.core.config.standard.DefaultInstantiator;
import hs.ddif.core.inject.injectable.ClassInjectableFactory;
import hs.ddif.core.inject.injectable.InjectableFactories;
import hs.ddif.core.inject.injectable.ResolvableInjectable;
import hs.ddif.core.inject.instantiation.Instantiator;
import hs.ddif.core.scope.ScopeResolver;
import hs.ddif.core.store.InjectableStore;

import java.util.List;

public class Instantiators {
  private static final ClassInjectableFactory FACTORY = InjectableFactories.forClass();

  public static Instantiator create(InjectableStore<ResolvableInjectable> store) {
    ScopeResolver[] scopeResolvers = new ScopeResolver[] {new SingletonScopeResolver(), new WeakSingletonScopeResolver()};

    AutoDiscoveringGatherer gatherer = new AutoDiscoveringGatherer(false, List.of(), FACTORY);

    return new DefaultInstantiator(store, gatherer, scopeResolvers);
  }

  public static Instantiator create() {
    return create(new InjectableStore<>());
  }

}
