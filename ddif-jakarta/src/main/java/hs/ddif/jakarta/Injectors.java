package hs.ddif.jakarta;

import hs.ddif.annotations.Opt;
import hs.ddif.annotations.Produces;
import hs.ddif.api.Injector;
import hs.ddif.core.StandardInjector;
import hs.ddif.core.definition.BindingProvider;
import hs.ddif.library.AnnotationBasedLifeCycleCallbacksFactory;
import hs.ddif.library.ConfigurableAnnotationStrategy;
import hs.ddif.library.DefaultInjectorStrategy;
import hs.ddif.library.ListInjectionTargetExtension;
import hs.ddif.library.NoProxyStrategy;
import hs.ddif.library.ProducesTypeRegistrationExtension;
import hs.ddif.library.ProviderTypeRegistrationExtension;
import hs.ddif.library.ProviderInjectionTargetExtension;
import hs.ddif.library.SetInjectionTargetExtension;
import hs.ddif.library.SimpleScopeStrategy;
import hs.ddif.library.SingletonScopeResolver;
import hs.ddif.spi.config.AnnotationStrategy;
import hs.ddif.spi.config.LifeCycleCallbacksFactory;
import hs.ddif.spi.discovery.TypeRegistrationExtension;
import hs.ddif.spi.instantiation.InjectionTargetExtension;
import hs.ddif.spi.scope.ScopeResolver;
import hs.ddif.util.Classes;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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
      createInjectionTargetExtensions(),
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

  private static List<InjectionTargetExtension<?>> createInjectionTargetExtensions() {
    return List.of(
      new ListInjectionTargetExtension<>(),
      new SetInjectionTargetExtension<>(),
      new ProviderInjectionTargetExtension<>(Provider.class, s -> s::get)
    );
  }

  private static List<TypeRegistrationExtension> createDiscoveryExtensions(LifeCycleCallbacksFactory lifeCycleCallbacksFactory) {
    List<TypeRegistrationExtension> extensions = new ArrayList<>();

    extensions.add(new ProviderTypeRegistrationExtension(PROVIDER_METHOD));
    extensions.add(new ProducesTypeRegistrationExtension(Produces.class));

    if(Classes.isAvailable("hs.ddif.extensions.assisted.AssistedTypeRegistrationExtension")) {
      LOGGER.info("Using AssistedTypeRegistrationExtension found on classpath");

      extensions.add(AssistedTypeRegistrationExtensionSupport.create(new BindingProvider(ANNOTATION_STRATEGY), lifeCycleCallbacksFactory));
    }

    return extensions;
  }
}

