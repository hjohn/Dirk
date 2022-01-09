package hs.ddif.core;

import hs.ddif.core.config.ProducesGathererExtension;
import hs.ddif.core.config.ProviderGathererExtension;
import hs.ddif.core.config.gather.Gatherer;
import hs.ddif.core.config.standard.AssistedClassInjectableFactoryTemplate;
import hs.ddif.core.config.standard.AutoDiscoveringGatherer;
import hs.ddif.core.config.standard.ConcreteClassInjectableFactoryTemplate;
import hs.ddif.core.config.standard.DefaultBinding;
import hs.ddif.core.config.standard.DefaultInjectable;
import hs.ddif.core.config.standard.DelegatingClassInjectableFactory;
import hs.ddif.core.inject.bind.BindingProvider;
import hs.ddif.core.inject.injectable.ClassInjectableFactory;
import hs.ddif.core.inject.injectable.FieldInjectableFactory;
import hs.ddif.core.inject.injectable.InstanceInjectableFactory;
import hs.ddif.core.inject.injectable.MethodInjectableFactory;
import hs.ddif.core.scope.ScopeResolver;

import java.util.List;

/**
 * Factory for {@link Injector}s of various types.
 */
public class Injectors {

  /**
   * Creates an {@link Injector} with auto discovery activated and the given
   * {@link ScopeResolver}s.
   *
   * @param scopeResolvers an optional array of {@link ScopeResolver}s
   * @return an {@link Injector}, never null
   */
  public static Injector autoDiscovering(ScopeResolver... scopeResolvers) {
    BindingProvider bindingProvider = new BindingProvider(DefaultBinding::new);
    ClassInjectableFactory classInjectableFactory = createClassInjectableFactory(bindingProvider);
    MethodInjectableFactory methodInjectableFactory = new MethodInjectableFactory(bindingProvider, DefaultInjectable::new);
    FieldInjectableFactory fieldInjectableFactory = new FieldInjectableFactory(bindingProvider, DefaultInjectable::new);
    InstanceInjectableFactory instanceInjectableFactory = new InstanceInjectableFactory(DefaultInjectable::new);

    return new Injector(
      classInjectableFactory,
      instanceInjectableFactory,
      createGatherer(classInjectableFactory, methodInjectableFactory, fieldInjectableFactory, true),
      scopeResolvers
    );
  }

  /**
   * Creates an {@link Injector} which must be manually configured with the given
   * {@link ScopeResolver}s.
   *
   * @param scopeResolvers an optional array of {@link ScopeResolver}s
   * @return an {@link Injector}, never null
   */
  public static Injector manual(ScopeResolver... scopeResolvers) {
    BindingProvider bindingProvider = new BindingProvider(DefaultBinding::new);
    ClassInjectableFactory classInjectableFactory = createClassInjectableFactory(bindingProvider);
    MethodInjectableFactory methodInjectableFactory = new MethodInjectableFactory(bindingProvider, DefaultInjectable::new);
    FieldInjectableFactory fieldInjectableFactory = new FieldInjectableFactory(bindingProvider, DefaultInjectable::new);
    InstanceInjectableFactory instanceInjectableFactory = new InstanceInjectableFactory(DefaultInjectable::new);

    return new Injector(
      classInjectableFactory,
      instanceInjectableFactory,
      createGatherer(classInjectableFactory, methodInjectableFactory, fieldInjectableFactory, false),
      scopeResolvers
    );
  }

  private static Gatherer createGatherer(ClassInjectableFactory classInjectableFactory, MethodInjectableFactory methodInjectableFactory, FieldInjectableFactory fieldInjectableFactory, boolean autoDiscovery) {
    List<AutoDiscoveringGatherer.Extension> extensions = List.of(
      new ProviderGathererExtension(methodInjectableFactory),
      new ProducesGathererExtension(methodInjectableFactory, fieldInjectableFactory)
    );

    return new AutoDiscoveringGatherer(autoDiscovery, extensions, classInjectableFactory);
  }

  private static ClassInjectableFactory createClassInjectableFactory(BindingProvider bindingProvider) {
    return new DelegatingClassInjectableFactory(List.of(
      new AssistedClassInjectableFactoryTemplate(bindingProvider, DefaultInjectable::new),
      new ConcreteClassInjectableFactoryTemplate(bindingProvider, DefaultInjectable::new)
    ));
  }
}
