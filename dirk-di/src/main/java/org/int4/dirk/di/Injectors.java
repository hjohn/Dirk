package org.int4.dirk.di;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.int4.dirk.annotations.Any;
import org.int4.dirk.annotations.Default;
import org.int4.dirk.annotations.Dependent;
import org.int4.dirk.annotations.NormalScope;
import org.int4.dirk.annotations.Opt;
import org.int4.dirk.annotations.Produces;
import org.int4.dirk.api.Injector;
import org.int4.dirk.api.definition.DefinitionException;
import org.int4.dirk.core.StandardInjector;
import org.int4.dirk.library.AnnotationBasedLifeCycleCallbacksFactory;
import org.int4.dirk.library.ConfigurableAnnotationStrategy;
import org.int4.dirk.library.DefaultInjectorStrategy;
import org.int4.dirk.library.ListInjectionTargetExtension;
import org.int4.dirk.library.NoProxyStrategy;
import org.int4.dirk.library.ProducesTypeRegistrationExtension;
import org.int4.dirk.library.ProviderInjectionTargetExtension;
import org.int4.dirk.library.ProviderTypeRegistrationExtension;
import org.int4.dirk.library.SetInjectionTargetExtension;
import org.int4.dirk.library.SingletonScopeResolver;
import org.int4.dirk.spi.config.AnnotationStrategy;
import org.int4.dirk.spi.config.LifeCycleCallbacksFactory;
import org.int4.dirk.spi.config.ProxyStrategy;
import org.int4.dirk.spi.config.ScopeStrategy;
import org.int4.dirk.spi.definition.TypeRegistrationExtension;
import org.int4.dirk.spi.instantiation.InjectionTargetExtension;
import org.int4.dirk.spi.scope.ScopeResolver;
import org.int4.dirk.util.Annotations;
import org.int4.dirk.util.Classes;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;

/**
 * Factory for {@link Injector}s of various types.
 */
public class Injectors {
  private static final Logger LOGGER = Logger.getLogger(Injectors.class.getName());
  private static final Default DEFAULT = Annotations.of(Default.class);
  private static final Any ANY = Annotations.of(Any.class);
  private static final Scope SCOPE = Annotations.of(Scope.class);
  private static final Singleton SINGLETON = Annotations.of(Singleton.class);
  private static final Dependent DEPENDENT = Annotations.of(Dependent.class);
  private static final AnnotationStrategy ANNOTATION_STRATEGY = new DirkAnnotationStrategy(Inject.class, Qualifier.class, Opt.class);
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
      createDiscoveryExtensions(lifeCycleCallbacksFactory),
      finalScopeResolvers,
      new DefaultInjectorStrategy(
        ANNOTATION_STRATEGY,
        new DirkScopeStrategy(Scope.class, NormalScope.class, SINGLETON, DEPENDENT),
        proxyStrategy,
        lifeCycleCallbacksFactory
      ),
      autoDiscovering
    );
  }

  private static List<InjectionTargetExtension<?, ?>> createInjectionTargetExtensions() {
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

    if(Classes.isAvailable("org.int4.dirk.extensions.assisted.AssistedTypeRegistrationExtension")) {
      LOGGER.info("Using AssistedTypeRegistrationExtension found on classpath");

      extensions.add(AssistedTypeRegistrationExtensionSupport.create(ANNOTATION_STRATEGY, lifeCycleCallbacksFactory));
    }

    return extensions;
  }

  static class DirkAnnotationStrategy extends ConfigurableAnnotationStrategy {
    public DirkAnnotationStrategy(Class<? extends Annotation> inject, Class<? extends Annotation> qualifier, Class<? extends Annotation> optional) {
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

  static class DirkScopeStrategy implements ScopeStrategy {
    private final Class<? extends Annotation> scopeAnnotationClass;
    private final Class<? extends Annotation> normalScopeAnnotationClass;
    private final Annotation singletonAnnotation;
    private final Annotation dependentAnnotation;

    DirkScopeStrategy(Class<? extends Annotation> scopeAnnotationClass, Class<? extends Annotation> normalScopeAnnotationClass, Annotation singletonAnnotation, Annotation dependentAnnotation) {
      this.scopeAnnotationClass = Objects.requireNonNull(scopeAnnotationClass, "scopeAnnotationClass");
      this.normalScopeAnnotationClass = Objects.requireNonNull(normalScopeAnnotationClass, "normalScopeAnnotationClass");
      this.singletonAnnotation = Objects.requireNonNull(singletonAnnotation, "singletonAnnotation");
      this.dependentAnnotation = Objects.requireNonNull(dependentAnnotation, "dependentAnnotation");
    }

    @Override
    public boolean isPseudoScope(Annotation annotation) {
      return Annotations.isMetaAnnotated(annotation.annotationType(), SCOPE);
    }

    @Override
    public Annotation getDefaultAnnotation() {
      return dependentAnnotation;
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

