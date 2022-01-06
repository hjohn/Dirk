package hs.ddif.core;

import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.inject.store.ConcreteClassInjectableFactoryTemplate;
import hs.ddif.core.inject.store.ClassInjectableFactory;
import hs.ddif.core.inject.store.DelegatingClassInjectableFactory;

import java.util.List;

public class InjectableFactories {

  public static ClassInjectableFactory forClass() {
    return new DelegatingClassInjectableFactory(List.of(new ConcreteClassInjectableFactoryTemplate(ResolvableInjectable::new)));
  }
}
