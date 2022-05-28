package org.int4.dirk.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.int4.dirk.api.Injector;
import org.int4.dirk.api.definition.DefinitionException;
import org.int4.dirk.core.StandardInjector;
import org.int4.dirk.library.AnnotationBasedLifeCycleCallbacksFactory;
import org.int4.dirk.library.ConfigurableAnnotationStrategy;
import org.int4.dirk.library.DefaultInjectorStrategy;
import org.int4.dirk.library.NoProxyStrategy;
import org.int4.dirk.library.ProducesTypeRegistrationExtension;
import org.int4.dirk.library.ProviderInjectionTargetExtension;
import org.int4.dirk.library.ProviderTypeRegistrationExtension;
import org.int4.dirk.library.SingletonScopeResolver;
import org.int4.dirk.spi.config.AnnotationStrategy;
import org.int4.dirk.spi.config.LifeCycleCallbacksFactory;
import org.int4.dirk.spi.config.ProxyStrategy;
import org.int4.dirk.spi.config.ScopeStrategy;
import org.int4.dirk.spi.discovery.TypeRegistrationExtension;
import org.int4.dirk.spi.instantiation.InjectionTargetExtension;
import org.int4.dirk.spi.scope.ScopeResolver;
import org.int4.dirk.util.Annotations;
import org.int4.dirk.util.Classes;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.NormalScope;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;

/**
 * Factory for CDI style {@link Injector}s.
 *
 * <p>This mimics CDI style injection, which supports all features of Jakarta Inject, and additionally:
 * <ul>
 * <li>The {@link Produces} annotation on fields and methods</li>
 * <li>The {@link Default} annotation is added to sources when not annotated with any qualifiers or only annotated with {@link Named} and/or {@link Any} annotations</li>
 * <li>The {@link Default} annotation is added to targets when not annotated with any other qualifiers</li>
 * <li>The {@link Any} annotation is always added to all sources</li>
 * </ul>
 *
 * Not supported are:
 * <ul>
 * <li>Interceptors, Decorators and Events</li>
 * <li>Extensions via Jakarta Enterprise Inject SPI; instead use the provided SPI</li>
 * <li>Lifecycle management for sources created by producers</li>
 * </ul>
 *
 * There is partial support for:
 * <ul>
 * <li>The {@link Instance} extension of {@link Provider}</li>
 * </ul>
 *
 * Other differences:
 * <ul>
 * <li>Proxies are only available when the optional proxy extension is on the class path</li>
 * <li>Proxies are only created to resolve scope conflicts, but not to resolve circular dependencies</li>
 * <li>Exceptions thrown by the injector are of a different type with different messages</li>
 * <li>Sources can still be added and removed after initialization if so desired</li>
 * </ul>
 */
public class Injectors {
  private static final Logger LOGGER = Logger.getLogger(Injectors.class.getName());
  private static final Default DEFAULT = Annotations.of(Default.class);
  private static final Any ANY = Annotations.of(Any.class);
  private static final Scope SCOPE = Annotations.of(Scope.class);
  private static final Singleton SINGLETON = Annotations.of(Singleton.class);
  private static final Dependent DEPENDENT = Annotations.of(Dependent.class);
  private static final AnnotationStrategy ANNOTATION_STRATEGY = new CdiAnnotationStrategy(Inject.class, Qualifier.class, null);
  private static final ScopeStrategy SCOPE_STRATEGY = new CdiScopeStrategy(Scope.class, NormalScope.class, SINGLETON, DEPENDENT);
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

    List<ScopeResolver> finalScopeResolvers = Arrays.stream(scopeResolvers).anyMatch(sr -> sr.getAnnotation().equals(SINGLETON)) ? Arrays.asList(scopeResolvers)
      : Stream.concat(Arrays.stream(scopeResolvers), Stream.of(new SingletonScopeResolver(SINGLETON))).collect(Collectors.toList());

    ProxyStrategy proxyStrategy = new NoProxyStrategy();

    if(Classes.isAvailable("org.int4.dirk.extensions.proxy.ByteBuddyProxyStrategy")) {
      LOGGER.info("Using ByteBuddyProxyStrategy found on classpath");

      proxyStrategy = ByteBuddyProxyStrategySupport.create();
    }

    return new StandardInjector(
      createInjectionTargetExtensions(),
      createTypeRegistrationExtensions(),
      finalScopeResolvers,
      new DefaultInjectorStrategy(
        ANNOTATION_STRATEGY,
        SCOPE_STRATEGY,
        proxyStrategy,
        lifeCycleCallbacksFactory
      ),
      autoDiscovering
    );
  }

  private static List<InjectionTargetExtension<?, ?>> createInjectionTargetExtensions() {
    return List.of(
      new ProviderInjectionTargetExtension<>(Provider.class, s -> s::get),
      new InstanceInjectionTargetExtension<>()
    );
  }

  private static List<TypeRegistrationExtension> createTypeRegistrationExtensions() {
    return List.of(
      new ProviderTypeRegistrationExtension(PROVIDER_METHOD),
      new ProducesTypeRegistrationExtension(Produces.class)
    );
  }

  static class CdiAnnotationStrategy extends ConfigurableAnnotationStrategy {
    public CdiAnnotationStrategy(Class<? extends Annotation> inject, Class<? extends Annotation> qualifier, Class<? extends Annotation> optional) {
      super(inject, qualifier, optional);
    }

    @Override
    public Set<Annotation> getQualifiers(AnnotatedElement element) {
      Set<Annotation> qualifiers = super.getQualifiers(element);
      boolean isTarget = element.isAnnotationPresent(Inject.class) || (element instanceof Parameter && ((Parameter)element).getDeclaringExecutable().isAnnotationPresent(Inject.class));

      if(isTarget) {
        if(qualifiers.isEmpty()) {
          qualifiers.add(DEFAULT);
        }
      }
      else {
        if(qualifiers.stream().noneMatch(q -> q.annotationType() != Named.class && !q.equals(ANY))) {
          qualifiers.add(DEFAULT);
        }

        qualifiers.add(ANY);
      }

      return qualifiers;
    }
  }

  static class CdiScopeStrategy implements ScopeStrategy {
    private final Class<? extends Annotation> scopeAnnotationClass;
    private final Class<? extends Annotation> normalScopeAnnotationClass;
    private final Annotation singletonAnnotation;
    private final Annotation dependentAnnotation;

    CdiScopeStrategy(Class<? extends Annotation> scopeAnnotationClass, Class<? extends Annotation> normalScopeAnnotationClass, Annotation singletonAnnotation, Annotation dependentAnnotation) {
      this.scopeAnnotationClass = Objects.requireNonNull(scopeAnnotationClass, "scopeAnnotationClass");
      this.normalScopeAnnotationClass = Objects.requireNonNull(normalScopeAnnotationClass, "normalScopeAnnotationClass");
      this.singletonAnnotation = Objects.requireNonNull(singletonAnnotation, "singletonAnnotation");
      this.dependentAnnotation = Objects.requireNonNull(dependentAnnotation, "dependentAnnotation");
    }

    @Override
    public boolean isPseudoScope(ScopeResolver scopeResolver) {
      return Annotations.isMetaAnnotated(scopeResolver.getAnnotation().annotationType(), SCOPE);
    }

    @Override
    public Annotation getDependentAnnotation() {
      return dependentAnnotation;
    }

    @Override
    public Annotation getSingletonAnnotation() {
      return singletonAnnotation;
    }

    @Override
    public Annotation getScope(AnnotatedElement element) throws DefinitionException {
      Set<Annotation> scopes = Annotations.findDirectlyMetaAnnotatedAnnotations(element, scopeAnnotationClass);

      scopes.addAll(Annotations.findDirectlyMetaAnnotatedAnnotations(element, normalScopeAnnotationClass));

      if(scopes.size() > 1) {
        throw new DefinitionException(element, "cannot have multiple scope annotations, but found: " + scopes.stream().sorted(Comparator.comparing(Object::toString)).collect(Collectors.toList()));
      }

      if(scopes.isEmpty()) {
        return null;
      }

      return scopes.iterator().next();
    }
  }
}

