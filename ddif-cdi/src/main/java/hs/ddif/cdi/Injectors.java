package hs.ddif.cdi;

import hs.ddif.api.Injector;
import hs.ddif.api.util.Annotations;
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
import hs.ddif.spi.config.AnnotationStrategy;
import hs.ddif.spi.config.LifeCycleCallbacksFactory;
import hs.ddif.spi.discovery.DiscoveryExtension;
import hs.ddif.spi.instantiation.TypeExtension;
import hs.ddif.spi.scope.ScopeResolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;

/**
 * Factory for {@link Injector}s of various types.
 */
public class Injectors {
  private static final Default DEFAULT = Annotations.of(Default.class);
  private static final Any ANY = Annotations.of(Any.class);
  private static final AnnotationStrategy SOURCE_ANNOTATION_STRATEGY = createSourceAnnotationStrategy();
  private static final AnnotationStrategy TARGET_ANNOTATION_STRATEGY = createTargetAnnotationStrategy();
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
    BindingProvider bindingProvider = new BindingProvider(TARGET_ANNOTATION_STRATEGY);
    LifeCycleCallbacksFactory lifeCycleCallbacksFactory = new AnnotationBasedLifeCycleCallbacksFactory(SOURCE_ANNOTATION_STRATEGY, PostConstruct.class, PreDestroy.class);

    List<ScopeResolver> finalScopeResolvers = Arrays.stream(scopeResolvers).anyMatch(sr -> sr.getAnnotationClass() == Singleton.class) ? Arrays.asList(scopeResolvers)
      : Stream.concat(Arrays.stream(scopeResolvers), Stream.of(new SingletonScopeResolver(Singleton.class))).collect(Collectors.toList());

    return new StandardInjector(
      createTypeExtensions(),
      createDiscoveryExtensions(bindingProvider, lifeCycleCallbacksFactory),
      finalScopeResolvers,
      // TODO the NoProxyScopeStrategy here should also detect "NormalScope"
      new DefaultInjectorStrategy(
        SOURCE_ANNOTATION_STRATEGY,
        new SimpleScopeStrategy(Scope.class, Singleton.class, Dependent.class),
        new NoProxyStrategy()
      ),
      bindingProvider,
      lifeCycleCallbacksFactory,
      autoDiscovering
    );
  }

  private static Map<Class<?>, TypeExtension<?>> createTypeExtensions() {
    Map<Class<?>, TypeExtension<?>> typeExtensions = new HashMap<>();

    typeExtensions.put(List.class, new ListTypeExtension<>(TARGET_ANNOTATION_STRATEGY));
    typeExtensions.put(Set.class, new SetTypeExtension<>(TARGET_ANNOTATION_STRATEGY));
    typeExtensions.put(Provider.class, new ProviderTypeExtension<>(Provider.class, s -> s::get));

    return typeExtensions;
  }

  private static List<DiscoveryExtension> createDiscoveryExtensions(BindingProvider bindingProvider, LifeCycleCallbacksFactory lifeCycleCallbacksFactory) {
    List<DiscoveryExtension> injectableExtensions = List.of(
      new ProviderDiscoveryExtension(PROVIDER_METHOD),
      new ProducesDiscoveryExtension(Produces.class)
      //new AssistedInjectableExtension(classInjectableFactory, ASSISTED_ANNOTATION_STRATEGY) FIXME
    );
    return injectableExtensions;
  }

  private static AnnotationStrategy createSourceAnnotationStrategy() {
    return new ConfigurableAnnotationStrategy(Inject.class, Qualifier.class, null) {
      @Override
      public Set<Annotation> getQualifiers(AnnotatedElement element) {
        Set<Annotation> qualifiers = super.getQualifiers(element);

        if(qualifiers.isEmpty()) {
          qualifiers.add(DEFAULT);
        }

        qualifiers.add(ANY);

        return qualifiers;
      }
    };
  }

  private static AnnotationStrategy createTargetAnnotationStrategy() {
    return new ConfigurableAnnotationStrategy(Inject.class, Qualifier.class, null) {
      @Override
      public Set<Annotation> getQualifiers(AnnotatedElement element) {
        Set<Annotation> qualifiers = super.getQualifiers(element);

        if(qualifiers.isEmpty()) {
          qualifiers.add(DEFAULT);
        }

        return qualifiers;
      }
    };
  }
}

