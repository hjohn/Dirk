package hs.ddif.jsr330;

import hs.ddif.core.Injector;
import hs.ddif.core.config.AssistedInjectableExtension;
import hs.ddif.core.config.ConfigurableAnnotationStrategy;
import hs.ddif.core.config.ListTypeExtension;
import hs.ddif.core.config.ProducesInjectableExtension;
import hs.ddif.core.config.ProviderInjectableExtension;
import hs.ddif.core.config.SetTypeExtension;
import hs.ddif.core.config.discovery.DiscovererFactory;
import hs.ddif.core.config.scope.SingletonScopeResolver;
import hs.ddif.core.config.standard.DefaultDiscovererFactory;
import hs.ddif.core.config.standard.DefaultInjectableFactory;
import hs.ddif.core.config.standard.InjectableExtension;
import hs.ddif.core.definition.ClassInjectableFactory;
import hs.ddif.core.definition.FieldInjectableFactory;
import hs.ddif.core.definition.InjectableFactory;
import hs.ddif.core.definition.InstanceInjectableFactory;
import hs.ddif.core.definition.MethodInjectableFactory;
import hs.ddif.core.definition.bind.AnnotationStrategy;
import hs.ddif.core.definition.bind.BindingProvider;
import hs.ddif.core.instantiation.TypeExtension;
import hs.ddif.core.instantiation.TypeExtensionStore;
import hs.ddif.core.scope.ScopeResolver;
import hs.ddif.core.scope.ScopeResolverManager;
import hs.ddif.core.util.Annotations;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Qualifier;
import javax.inject.Scope;
import javax.inject.Singleton;

/**
 * Factory for {@link Injector}s of various types.
 */
public class Injectors {
  private static final Inject INJECT = Annotations.of(Inject.class);
  private static final Qualifier QUALIFIER = Annotations.of(Qualifier.class);
  private static final Scope SCOPE = Annotations.of(Scope.class);
  private static final Singleton SINGLETON = Annotations.of(Singleton.class);
  private static final AnnotationStrategy ANNOTATION_STRATEGY = new ConfigurableAnnotationStrategy(INJECT, QUALIFIER, SCOPE);
  private static final Method PROVIDER_METHOD;

  static {
    try {
      PROVIDER_METHOD = Provider.class.getDeclaredMethod("get");
    }
    catch(NoSuchMethodException | SecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Creates an {@link Injector} with auto discovery activated and the given
   * {@link ScopeResolver}s.
   *
   * @param scopeResolvers an optional array of {@link ScopeResolver}s
   * @return an {@link Injector}, never {@code null}
   */
  public static Injector autoDiscovering(ScopeResolver... scopeResolvers) {
    SingletonScopeResolver singletonScopeResolver = new SingletonScopeResolver(SINGLETON);
    ScopeResolverManager scopeResolverManager = createScopeResolverManager(singletonScopeResolver, scopeResolvers);
    InjectableFactory injectableFactory = new DefaultInjectableFactory(scopeResolverManager, ANNOTATION_STRATEGY);
    InstanceInjectableFactory instanceInjectableFactory = new InstanceInjectableFactory(injectableFactory, SINGLETON);

    return new Injector(
      createTypeExtensionStore(),
      createDiscoveryFactory(injectableFactory, true),
      instanceInjectableFactory
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
    SingletonScopeResolver singletonScopeResolver = new SingletonScopeResolver(SINGLETON);
    ScopeResolverManager scopeResolverManager = createScopeResolverManager(singletonScopeResolver, scopeResolvers);
    InjectableFactory injectableFactory = new DefaultInjectableFactory(scopeResolverManager, ANNOTATION_STRATEGY);
    InstanceInjectableFactory instanceInjectableFactory = new InstanceInjectableFactory(injectableFactory, SINGLETON);

    return new Injector(
      createTypeExtensionStore(),
      createDiscoveryFactory(injectableFactory, false),
      instanceInjectableFactory
    );
  }

  private static TypeExtensionStore createTypeExtensionStore() {
    Map<Class<?>, TypeExtension<?>> typeExtensions = new HashMap<>();

    typeExtensions.put(List.class, new ListTypeExtension<>());
    typeExtensions.put(Set.class, new SetTypeExtension<>());
    typeExtensions.put(Provider.class, new ProviderTypeExtension<>());

    return new TypeExtensionStore(typeExtensions);
  }

  private static ScopeResolverManager createScopeResolverManager(SingletonScopeResolver singletonScopeResolver, ScopeResolver... scopeResolvers) {
    ScopeResolver[] standardScopeResolvers = new ScopeResolver[] {singletonScopeResolver};
    ScopeResolver[] extendedScopeResolvers = Stream.of(scopeResolvers, standardScopeResolvers).flatMap(Stream::of).toArray(ScopeResolver[]::new);

    return new ScopeResolverManager(extendedScopeResolvers);
  }

  private static DiscovererFactory createDiscoveryFactory(InjectableFactory injectableFactory, boolean autoDiscovery) {
    BindingProvider bindingProvider = new BindingProvider(ANNOTATION_STRATEGY);
    ClassInjectableFactory classInjectableFactory = new ClassInjectableFactory(bindingProvider, injectableFactory);
    MethodInjectableFactory methodInjectableFactory = new MethodInjectableFactory(bindingProvider, injectableFactory);
    FieldInjectableFactory fieldInjectableFactory = new FieldInjectableFactory(bindingProvider, injectableFactory);

    List<InjectableExtension> injectableExtensions = List.of(
      new ProviderInjectableExtension(PROVIDER_METHOD, methodInjectableFactory),
      new ProducesInjectableExtension(methodInjectableFactory, fieldInjectableFactory),
      new AssistedInjectableExtension(bindingProvider, classInjectableFactory, INJECT, Provider.class, Provider::get)
    );

    return new DefaultDiscovererFactory(autoDiscovery, injectableExtensions, classInjectableFactory);
  }
}
