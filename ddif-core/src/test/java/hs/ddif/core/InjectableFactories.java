package hs.ddif.core;

import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.inject.store.BindingProvider;
import hs.ddif.core.inject.store.ClassInjectableFactory;
import hs.ddif.core.inject.store.ConcreteClassInjectableFactoryTemplate;
import hs.ddif.core.inject.store.DefaultBinding;
import hs.ddif.core.inject.store.DelegatingClassInjectableFactory;

import java.util.List;

public class InjectableFactories {
  private static final BindingProvider BINDING_PROVIDER = new BindingProvider(DefaultBinding::new);

  public static ClassInjectableFactory forClass() {
    return new DelegatingClassInjectableFactory(List.of(new ConcreteClassInjectableFactoryTemplate(BINDING_PROVIDER, ResolvableInjectable::new)));
  }
}
