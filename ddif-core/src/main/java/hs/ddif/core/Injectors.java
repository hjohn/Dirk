package hs.ddif.core;

import hs.ddif.core.inject.instantiator.Gatherer;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.inject.store.AssistedClassInjectableFactoryTemplate;
import hs.ddif.core.inject.store.AutoDiscoveringGatherer;
import hs.ddif.core.inject.store.ConcreteClassInjectableFactoryTemplate;
import hs.ddif.core.inject.store.ClassInjectableFactory;
import hs.ddif.core.inject.store.DelegatingClassInjectableFactory;
import hs.ddif.core.inject.store.FieldInjectableFactory;
import hs.ddif.core.inject.store.InstanceInjectableFactory;
import hs.ddif.core.inject.store.MethodInjectableFactory;
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
    ClassInjectableFactory classInjectableFactory = createClassInjectableFactory();
    MethodInjectableFactory methodInjectableFactory = new MethodInjectableFactory(ResolvableInjectable::new);
    FieldInjectableFactory fieldInjectableFactory = new FieldInjectableFactory(ResolvableInjectable::new);
    InstanceInjectableFactory instanceInjectableFactory = new InstanceInjectableFactory(ResolvableInjectable::new);

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
    ClassInjectableFactory classInjectableFactory = createClassInjectableFactory();
    MethodInjectableFactory methodInjectableFactory = new MethodInjectableFactory(ResolvableInjectable::new);
    FieldInjectableFactory fieldInjectableFactory = new FieldInjectableFactory(ResolvableInjectable::new);
    InstanceInjectableFactory instanceInjectableFactory = new InstanceInjectableFactory(ResolvableInjectable::new);

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

  private static ClassInjectableFactory createClassInjectableFactory() {
    return new DelegatingClassInjectableFactory(List.of(
      new AssistedClassInjectableFactoryTemplate(ResolvableInjectable::new),
      new ConcreteClassInjectableFactoryTemplate(ResolvableInjectable::new)
    ));
  }
}
