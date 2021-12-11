package hs.ddif.core.inject.instantiator;

import hs.ddif.core.inject.store.AutoDiscoveringGatherer;
import hs.ddif.core.scope.ScopeResolver;
import hs.ddif.core.scope.SingletonScopeResolver;
import hs.ddif.core.scope.WeakSingletonScopeResolver;
import hs.ddif.core.store.InjectableStore;

import java.util.List;

public class Instantiators {

  public static Instantiator create(InjectableStore<ResolvableInjectable> store) {
    ScopeResolver[] scopeResolvers = new ScopeResolver[] {new SingletonScopeResolver(), new WeakSingletonScopeResolver()};

    AutoDiscoveringGatherer gatherer = new AutoDiscoveringGatherer(store, false, List.of());

    return new Instantiator(store, gatherer, false, scopeResolvers);
  }

  public static Instantiator create() {
    return create(new InjectableStore<>());
  }

}