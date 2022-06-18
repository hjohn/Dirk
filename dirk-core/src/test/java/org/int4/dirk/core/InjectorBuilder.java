package org.int4.dirk.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.int4.dirk.annotations.Opt;
import org.int4.dirk.annotations.Produces;
import org.int4.dirk.api.Injector;
import org.int4.dirk.core.test.scope.Dependent;
import org.int4.dirk.library.AnnotationBasedLifeCycleCallbacksFactory;
import org.int4.dirk.library.ConfigurableAnnotationStrategy;
import org.int4.dirk.library.DefaultInjectorStrategy;
import org.int4.dirk.library.ListInjectionTargetExtension;
import org.int4.dirk.library.NoProxyStrategy;
import org.int4.dirk.library.ProducesTypeRegistrationExtension;
import org.int4.dirk.library.ProviderInjectionTargetExtension;
import org.int4.dirk.library.ProviderTypeRegistrationExtension;
import org.int4.dirk.library.SetInjectionTargetExtension;
import org.int4.dirk.library.SimpleScopeStrategy;
import org.int4.dirk.library.SingletonScopeResolver;
import org.int4.dirk.spi.config.InjectorStrategy;
import org.int4.dirk.spi.definition.TypeRegistrationExtension;
import org.int4.dirk.spi.instantiation.InjectionTargetExtension;
import org.int4.dirk.spi.scope.ScopeResolver;
import org.int4.dirk.util.Annotations;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;

public class InjectorBuilder {
  private static final Method PROVIDER_METHOD;

  static {
    try {
      PROVIDER_METHOD = Provider.class.getDeclaredMethod("get");
    }
    catch(NoSuchMethodException | SecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private final List<InjectionTargetExtension<?, ?>> injectionTargetExtensions = new ArrayList<>();
    private final List<TypeRegistrationExtension> typeRegistrationExtensions = new ArrayList<>();
    private final List<ScopeResolver> scopeResolvers = new ArrayList<>();

    private InjectorStrategy injectorStrategy;
    private boolean autoDiscovery;

    public Builder injectorStrategy(InjectorStrategy injectorStrategy) {
      this.injectorStrategy = injectorStrategy;

      return this;
    }

    public Builder autoDiscovery() {
      this.autoDiscovery = true;

      return this;
    }

    public Builder useDefaultTypeRegistrationExtensions() {
      this.typeRegistrationExtensions.clear();
      this.typeRegistrationExtensions.add(new ProviderTypeRegistrationExtension(PROVIDER_METHOD));
      this.typeRegistrationExtensions.add(new ProducesTypeRegistrationExtension(Produces.class));

      return this;
    }

    public Builder add(TypeRegistrationExtension extension) {
      this.typeRegistrationExtensions.add(Objects.requireNonNull(extension, "extension"));

      return this;
    }

    public Builder useDefaultInjectionTargetExtensions() {
      this.injectionTargetExtensions.clear();
      this.injectionTargetExtensions.add(new ListInjectionTargetExtension<>());
      this.injectionTargetExtensions.add(new SetInjectionTargetExtension<>());
      this.injectionTargetExtensions.add(new ProviderInjectionTargetExtension<>(Provider.class, s -> s::get));

      return this;
    }

    public Builder add(InjectionTargetExtension<?, ?> extension) {
      this.injectionTargetExtensions.add(Objects.requireNonNull(extension, "extension"));

      return this;
    }

    public Builder add(ScopeResolver scopeResolver) {
      this.scopeResolvers.add(Objects.requireNonNull(scopeResolver, "scopeResolver"));

      return this;
    }

    public Injector build() {
      this.injectorStrategy = injectorStrategy == null ? defaultInjectorStrategy() : injectorStrategy;

      return new StandardInjector(
        injectionTargetExtensions,
        typeRegistrationExtensions,
        determineScopeResolvers(),
        injectorStrategy,
        autoDiscovery
      );
    }

    private List<ScopeResolver> determineScopeResolvers() {
      Annotation singletonAnnotation = injectorStrategy.getScopeStrategy().getSingletonAnnotation();
      boolean hasSingletonScopeResolver = scopeResolvers.stream().anyMatch(sr -> sr.getAnnotation().equals(singletonAnnotation));

      return hasSingletonScopeResolver
        ? scopeResolvers
        : Stream.concat(scopeResolvers.stream(), Stream.of(new SingletonScopeResolver(singletonAnnotation))).collect(Collectors.toList());
    }

    private static InjectorStrategy defaultInjectorStrategy() {
      return new DefaultInjectorStrategy(
        new ConfigurableAnnotationStrategy(Inject.class, Qualifier.class, Opt.class),
        new SimpleScopeStrategy(Scope.class, Annotations.of(Dependent.class), Annotations.of(Singleton.class), Annotations.of(Dependent.class)),
        new NoProxyStrategy(),
        new AnnotationBasedLifeCycleCallbacksFactory(PostConstruct.class, PreDestroy.class)
      );
    }
  }
}
