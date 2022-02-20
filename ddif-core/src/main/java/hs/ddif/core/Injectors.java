package hs.ddif.core;

import hs.ddif.core.config.ListTypeExtension;
import hs.ddif.core.config.ProducesGathererExtension;
import hs.ddif.core.config.ProviderGathererExtension;
import hs.ddif.core.config.ProviderTypeExtension;
import hs.ddif.core.config.SetTypeExtension;
import hs.ddif.core.config.gather.Gatherer;
import hs.ddif.core.config.scope.SingletonScopeResolver;
import hs.ddif.core.config.scope.WeakSingletonScopeResolver;
import hs.ddif.core.config.standard.AssistedClassInjectableFactoryTemplate;
import hs.ddif.core.config.standard.AutoDiscoveringGatherer;
import hs.ddif.core.config.standard.ConcreteClassInjectableFactoryTemplate;
import hs.ddif.core.config.standard.DefaultAnnotatedInjectableFactory;
import hs.ddif.core.config.standard.DefaultInjectable;
import hs.ddif.core.config.standard.DelegatingClassInjectableFactory;
import hs.ddif.core.definition.AnnotatedInjectableFactory;
import hs.ddif.core.definition.ClassInjectableFactory;
import hs.ddif.core.definition.FieldInjectableFactory;
import hs.ddif.core.definition.InstanceInjectableFactory;
import hs.ddif.core.definition.MethodInjectableFactory;
import hs.ddif.core.definition.bind.BindingProvider;
import hs.ddif.core.inject.store.InstantiatorBindingMap;
import hs.ddif.core.instantiation.InstantiatorFactory;
import hs.ddif.core.instantiation.TypeExtension;
import hs.ddif.core.scope.ScopeResolver;
import hs.ddif.core.scope.ScopeResolverManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Provider;

/**
 * Factory for {@link Injector}s of various types.
 */
public class Injectors {

  /**
   * Creates an {@link Injector} with auto discovery activated and the given
   * {@link ScopeResolver}s.
   *
   * @param scopeResolvers an optional array of {@link ScopeResolver}s
   * @return an {@link Injector}, never {@code null}
   */
  public static Injector autoDiscovering(ScopeResolver... scopeResolvers) {
    SingletonScopeResolver singletonScopeResolver = new SingletonScopeResolver();
    ScopeResolverManager scopeResolverManager = createScopeResolverManager(singletonScopeResolver, scopeResolvers);
    AnnotatedInjectableFactory injectableFactory = new DefaultAnnotatedInjectableFactory(scopeResolverManager);
    BindingProvider bindingProvider = new BindingProvider();
    ClassInjectableFactory classInjectableFactory = createClassInjectableFactory(bindingProvider, injectableFactory);
    MethodInjectableFactory methodInjectableFactory = new MethodInjectableFactory(bindingProvider, injectableFactory);
    FieldInjectableFactory fieldInjectableFactory = new FieldInjectableFactory(bindingProvider, injectableFactory);
    InstanceInjectableFactory instanceInjectableFactory = new InstanceInjectableFactory(DefaultInjectable::new, singletonScopeResolver);

    return new Injector(
      createInstantiatorBindingMap(),
      instanceInjectableFactory,
      createGatherer(classInjectableFactory, methodInjectableFactory, fieldInjectableFactory, true)
    );
  }

  /**
   * Creates an {@link Injector} which must be manually configured with the given
   * {@link ScopeResolver}s.
   *
   * @param scopeResolvers an optional array of {@link ScopeResolver}s
   * @return an {@link Injector}, never {@code null}
   */
  public static Injector manual(ScopeResolver... scopeResolvers) {
    SingletonScopeResolver singletonScopeResolver = new SingletonScopeResolver();
    ScopeResolverManager scopeResolverManager = createScopeResolverManager(singletonScopeResolver, scopeResolvers);
    AnnotatedInjectableFactory injectableFactory = new DefaultAnnotatedInjectableFactory(scopeResolverManager);
    BindingProvider bindingProvider = new BindingProvider();
    ClassInjectableFactory classInjectableFactory = createClassInjectableFactory(bindingProvider, injectableFactory);
    MethodInjectableFactory methodInjectableFactory = new MethodInjectableFactory(bindingProvider, injectableFactory);
    FieldInjectableFactory fieldInjectableFactory = new FieldInjectableFactory(bindingProvider, injectableFactory);
    InstanceInjectableFactory instanceInjectableFactory = new InstanceInjectableFactory(DefaultInjectable::new, singletonScopeResolver);

    return new Injector(
      createInstantiatorBindingMap(),
      instanceInjectableFactory,
      createGatherer(classInjectableFactory, methodInjectableFactory, fieldInjectableFactory, false)
    );
  }

  private static InstantiatorBindingMap createInstantiatorBindingMap() {
    return new InstantiatorBindingMap(createInstanceFactoryManager());
  }

  private static InstantiatorFactory createInstanceFactoryManager() {
    Map<Class<?>, TypeExtension<?>> typeExtensions = new HashMap<>();

    typeExtensions.put(List.class, new ListTypeExtension<>());
    typeExtensions.put(Set.class, new SetTypeExtension<>());
    typeExtensions.put(Provider.class, new ProviderTypeExtension<>());

    return new InstantiatorFactory(typeExtensions);
  }

  private static ScopeResolverManager createScopeResolverManager(SingletonScopeResolver singletonScopeResolver, ScopeResolver... scopeResolvers) {
    ScopeResolver[] standardScopeResolvers = new ScopeResolver[] {singletonScopeResolver, new WeakSingletonScopeResolver()};
    ScopeResolver[] extendedScopeResolvers = Stream.of(scopeResolvers, standardScopeResolvers).flatMap(Stream::of).toArray(ScopeResolver[]::new);

    return new ScopeResolverManager(extendedScopeResolvers);
  }

  private static Gatherer createGatherer(ClassInjectableFactory classInjectableFactory, MethodInjectableFactory methodInjectableFactory, FieldInjectableFactory fieldInjectableFactory, boolean autoDiscovery) {
    List<AutoDiscoveringGatherer.Extension> extensions = List.of(
      new ProviderGathererExtension(methodInjectableFactory),
      new ProducesGathererExtension(methodInjectableFactory, fieldInjectableFactory)
    );

    return new AutoDiscoveringGatherer(autoDiscovery, extensions, classInjectableFactory);
  }

  private static ClassInjectableFactory createClassInjectableFactory(BindingProvider bindingProvider, AnnotatedInjectableFactory injectableFactory) {
    return new DelegatingClassInjectableFactory(List.of(
      new AssistedClassInjectableFactoryTemplate(bindingProvider, injectableFactory),
      new ConcreteClassInjectableFactoryTemplate(bindingProvider, injectableFactory)
    ));
  }
}
