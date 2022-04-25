package hs.ddif.jsr330;

import hs.ddif.annotations.Opt;
import hs.ddif.annotations.Produces;
import hs.ddif.api.Injector;
import hs.ddif.core.StandardInjector;
import hs.ddif.core.config.AnnotationBasedLifeCycleCallbacksFactory;
import hs.ddif.core.config.ConfigurableAnnotationStrategy;
import hs.ddif.core.config.DefaultInjectorStrategy;
import hs.ddif.core.config.ListTypeExtension;
import hs.ddif.core.config.NoProxyStrategy;
import hs.ddif.core.config.ProducesDiscoveryExtension;
import hs.ddif.core.config.ProviderDiscoveryExtension;
import hs.ddif.core.config.ProviderTypeExtension;
import hs.ddif.core.config.SetTypeExtension;
import hs.ddif.core.config.SimpleScopeStrategy;
import hs.ddif.core.config.SingletonScopeResolver;
import hs.ddif.core.definition.BindingProvider;
import hs.ddif.core.util.Classes;
import hs.ddif.spi.config.AnnotationStrategy;
import hs.ddif.spi.config.LifeCycleCallbacksFactory;
import hs.ddif.spi.discovery.DiscoveryExtension;
import hs.ddif.spi.instantiation.TypeExtension;
import hs.ddif.spi.scope.ScopeResolver;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Qualifier;
import javax.inject.Scope;
import javax.inject.Singleton;

/**
 * Factory for {@link Injector}s of various types.
 */
public class Injectors {
  private static final Logger LOGGER = Logger.getLogger(Injectors.class.getName());
  private static final AnnotationStrategy ANNOTATION_STRATEGY = new ConfigurableAnnotationStrategy(Inject.class, Qualifier.class, Opt.class);
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
    return createInjector(true, scopeResolvers);
  }

  /**
   * Creates an {@link Injector} which must be manually configured with the given
   * {@link ScopeResolver}s.
   *
   * @param scopeResolvers an optional array of {@link ScopeResolver}s
   * @return an {@link Injector}, never {@code null}
   */
  public static Injector manual(ScopeResolver... scopeResolvers) {
    return createInjector(false, scopeResolvers);
  }

  private static Injector createInjector(boolean autoDiscovering, ScopeResolver... scopeResolvers) {
    LifeCycleCallbacksFactory lifeCycleCallbacksFactory = new AnnotationBasedLifeCycleCallbacksFactory(PostConstruct.class, PreDestroy.class);

    List<ScopeResolver> finalScopeResolvers = Arrays.stream(scopeResolvers).anyMatch(sr -> sr.getAnnotationClass() == Singleton.class) ? Arrays.asList(scopeResolvers)
      : Stream.concat(Arrays.stream(scopeResolvers), Stream.of(new SingletonScopeResolver(Singleton.class))).collect(Collectors.toList());

    return new StandardInjector(
      createTypeExtensions(),
      createDiscoveryExtensions(lifeCycleCallbacksFactory),
      finalScopeResolvers,
      new DefaultInjectorStrategy(
        ANNOTATION_STRATEGY,
        new SimpleScopeStrategy(Scope.class, Singleton.class, Dependent.class),
        new NoProxyStrategy(),
        lifeCycleCallbacksFactory
      ),
      autoDiscovering
    );
  }

  private static Map<Class<?>, TypeExtension<?>> createTypeExtensions() {
    Map<Class<?>, TypeExtension<?>> typeExtensions = new HashMap<>();

    typeExtensions.put(List.class, new ListTypeExtension<>(ANNOTATION_STRATEGY));
    typeExtensions.put(Set.class, new SetTypeExtension<>(ANNOTATION_STRATEGY));
    typeExtensions.put(Provider.class, new ProviderTypeExtension<>(Provider.class, s -> s::get));

    return typeExtensions;
  }

  private static List<DiscoveryExtension> createDiscoveryExtensions(LifeCycleCallbacksFactory lifeCycleCallbacksFactory) {
    List<DiscoveryExtension> injectableExtensions = new ArrayList<>();

    injectableExtensions.add(new ProviderDiscoveryExtension(PROVIDER_METHOD));
    injectableExtensions.add(new ProducesDiscoveryExtension(Produces.class));

    if(Classes.isAvailable("hs.ddif.extensions.assisted.AssistedInjectableExtension")) {
      LOGGER.info("Using AssistedInjectableExtension found on classpath");

      injectableExtensions.add(AssistedInjectableExtensionSupport.create(new BindingProvider(ANNOTATION_STRATEGY), lifeCycleCallbacksFactory));
    }

    return injectableExtensions;
  }
}

