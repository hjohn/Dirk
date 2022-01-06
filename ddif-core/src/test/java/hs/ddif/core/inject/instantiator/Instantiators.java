package hs.ddif.core.inject.instantiator;

import hs.ddif.core.InjectableFactories;
import hs.ddif.core.inject.store.AutoDiscoveringGatherer;
import hs.ddif.core.inject.store.ClassInjectableFactory;
import hs.ddif.core.scope.ScopeResolver;
import hs.ddif.core.scope.SingletonScopeResolver;
import hs.ddif.core.scope.WeakSingletonScopeResolver;
import hs.ddif.core.store.InjectableStore;

import java.util.List;

public class Instantiators {
  private static final ClassInjectableFactory FACTORY = InjectableFactories.forClass();

  public static Instantiator create(InjectableStore<ResolvableInjectable> store) {
    ScopeResolver[] scopeResolvers = new ScopeResolver[] {new SingletonScopeResolver(), new WeakSingletonScopeResolver()};

    AutoDiscoveringGatherer gatherer = new AutoDiscoveringGatherer(false, List.of(), FACTORY);

    return new Instantiator(store, gatherer, scopeResolvers);
  }

  public static Instantiator create() {
    return create(new InjectableStore<>());
  }

}
