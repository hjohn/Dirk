package hs.ddif.core.definition;

import hs.ddif.core.config.standard.ConcreteClassInjectableFactoryTemplate;
import hs.ddif.core.config.standard.DefaultAnnotatedInjectableFactory;
import hs.ddif.core.config.standard.DelegatingClassInjectableFactory;
import hs.ddif.core.definition.bind.BindingProvider;

import java.util.List;

public class InjectableFactories {
  private static final BindingProvider BINDING_PROVIDER = new BindingProvider();

  public static ClassInjectableFactory forClass() {
    return new DelegatingClassInjectableFactory(List.of(new ConcreteClassInjectableFactoryTemplate(BINDING_PROVIDER, new DefaultAnnotatedInjectableFactory())));
  }

  public static FieldInjectableFactory forField() {
    return new FieldInjectableFactory(BINDING_PROVIDER, new DefaultAnnotatedInjectableFactory());
  }

  public static MethodInjectableFactory forMethod() {
    return new MethodInjectableFactory(BINDING_PROVIDER, new DefaultAnnotatedInjectableFactory());
  }
}
