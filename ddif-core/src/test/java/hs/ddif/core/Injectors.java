package hs.ddif.core;

import hs.ddif.annotations.Opt;
import hs.ddif.annotations.Produces;
import hs.ddif.core.config.ConfigurableAnnotationStrategy;
import hs.ddif.core.config.ProducesInjectableExtension;
import hs.ddif.core.config.ProviderInjectableExtension;
import hs.ddif.core.config.discovery.DiscovererFactory;
import hs.ddif.core.config.scope.SingletonScopeResolver;
import hs.ddif.core.config.standard.AnnotationBasedLifeCycleCallbacksFactory;
import hs.ddif.core.config.standard.DefaultDiscovererFactory;
import hs.ddif.core.config.standard.DefaultInjectableFactory;
import hs.ddif.core.config.standard.InjectableExtension;
import hs.ddif.core.definition.ClassInjectableFactory;
import hs.ddif.core.definition.FieldInjectableFactory;
import hs.ddif.core.definition.InjectableFactory;
import hs.ddif.core.definition.InstanceInjectableFactory;
import hs.ddif.core.definition.LifeCycleCallbacksFactory;
import hs.ddif.core.definition.MethodInjectableFactory;
import hs.ddif.core.definition.bind.AnnotationStrategy;
import hs.ddif.core.definition.bind.BindingProvider;
import hs.ddif.core.instantiation.TypeExtensionStores;
import hs.ddif.core.scope.ScopeResolver;
import hs.ddif.core.scope.ScopeResolverManager;
import hs.ddif.core.util.Annotations;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;

/**
 * Factory for {@link Injector}s of various types.
 */
public class Injectors {
  private static final Singleton SINGLETON = Annotations.of(Singleton.class);
  private static final AnnotationStrategy ANNOTATION_STRATEGY = new ConfigurableAnnotationStrategy(Inject.class, Qualifier.class, Scope.class, Opt.class);
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
      TypeExtensionStores.create(ANNOTATION_STRATEGY),
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
      TypeExtensionStores.create(ANNOTATION_STRATEGY),
      createDiscoveryFactory(injectableFactory, false),
      instanceInjectableFactory
    );
  }

  private static ScopeResolverManager createScopeResolverManager(SingletonScopeResolver singletonScopeResolver, ScopeResolver... scopeResolvers) {
    ScopeResolver[] standardScopeResolvers = new ScopeResolver[] {singletonScopeResolver};
    ScopeResolver[] extendedScopeResolvers = Stream.of(scopeResolvers, standardScopeResolvers).flatMap(Stream::of).toArray(ScopeResolver[]::new);

    return new ScopeResolverManager(extendedScopeResolvers);
  }

  private static DiscovererFactory createDiscoveryFactory(InjectableFactory injectableFactory, boolean autoDiscovery) {
    BindingProvider bindingProvider = new BindingProvider(ANNOTATION_STRATEGY);
    LifeCycleCallbacksFactory lifeCycleCallbacksFactory = new AnnotationBasedLifeCycleCallbacksFactory(ANNOTATION_STRATEGY, PostConstruct.class, PreDestroy.class);
    ClassInjectableFactory classInjectableFactory = new ClassInjectableFactory(bindingProvider, injectableFactory, lifeCycleCallbacksFactory);
    MethodInjectableFactory methodInjectableFactory = new MethodInjectableFactory(bindingProvider, injectableFactory);
    FieldInjectableFactory fieldInjectableFactory = new FieldInjectableFactory(bindingProvider, injectableFactory);

    List<InjectableExtension> injectableExtensions = new ArrayList<>();

    injectableExtensions.add(new ProviderInjectableExtension(methodInjectableFactory, PROVIDER_METHOD));
    injectableExtensions.add(new ProducesInjectableExtension(methodInjectableFactory, fieldInjectableFactory, Produces.class));

    return new DefaultDiscovererFactory(autoDiscovery, injectableExtensions, classInjectableFactory);
  }
}

