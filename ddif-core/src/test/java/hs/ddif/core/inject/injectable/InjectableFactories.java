package hs.ddif.core.inject.injectable;

import hs.ddif.core.config.standard.ConcreteClassInjectableFactoryTemplate;
import hs.ddif.core.config.standard.DefaultInjectable;
import hs.ddif.core.config.standard.DelegatingClassInjectableFactory;
import hs.ddif.core.inject.bind.BindingProvider;

import java.util.List;

public class InjectableFactories {
  private static final BindingProvider BINDING_PROVIDER = new BindingProvider();

  public static ClassInjectableFactory forClass() {
    return new DelegatingClassInjectableFactory(List.of(new ConcreteClassInjectableFactoryTemplate(BINDING_PROVIDER, DefaultInjectable::new)));
  }
}
