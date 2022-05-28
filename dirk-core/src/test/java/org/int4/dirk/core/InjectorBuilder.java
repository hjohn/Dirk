package org.int4.dirk.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
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
import org.int4.dirk.spi.discovery.TypeRegistrationExtension;
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

    public Builder1 injectorStrategy(InjectorStrategy injectorStrategy) {
      return new Builder1(injectorStrategy);
    }

    public Builder1 defaultInjectorStrategy() {
      return new Builder1(new DefaultInjectorStrategy(
        new ConfigurableAnnotationStrategy(Inject.class, Qualifier.class, Opt.class),
        new SimpleScopeStrategy(Scope.class, Annotations.of(Dependent.class), Annotations.of(Singleton.class), Annotations.of(Dependent.class)),
        new NoProxyStrategy(),
        new AnnotationBasedLifeCycleCallbacksFactory(PostConstruct.class, PreDestroy.class)
      ));
    }

    public Builder4 manual() {
      return defaultInjectorStrategy().defaultScopeResolvers().manual();
    }
  }

  public static class Context1 {
    public final InjectorStrategy injectorStrategy;

    Context1(InjectorStrategy injectorStrategy) {
      this.injectorStrategy = injectorStrategy;
    }
  }

  public static class Context2 extends Context1 {
    public final List<InjectionTargetExtension<?, ?>> injectionTargetExtensions;
    public final List<ScopeResolver> scopeResolvers;

    Context2(Context1 context, List<ScopeResolver> scopeResolvers, Collection<InjectionTargetExtension<?, ?>> injectionTargetExtensions) {
      super(context.injectorStrategy);

      this.scopeResolvers = scopeResolvers;
      this.injectionTargetExtensions = Collections.unmodifiableList(new ArrayList<>(injectionTargetExtensions));
    }
  }

  public static class Context4 extends Context2 {
    public final boolean autoDiscovery;

    Context4(Context2 context, boolean autoDiscovery) {
      super(context, context.scopeResolvers, context.injectionTargetExtensions);

      this.autoDiscovery = autoDiscovery;
    }
  }

  public static class Context5 extends Context4 {
    public final List<TypeRegistrationExtension> typeRegistrationExtensions;

    Context5(Context4 context, List<TypeRegistrationExtension> typeRegistrationExtensions) {
      super(context, context.autoDiscovery);

      this.typeRegistrationExtensions = typeRegistrationExtensions;
    }
  }

  public static class Builder1 {
    private final Context1 context;

    Builder1(InjectorStrategy injectorStrategy) {
      this.context = new Context1(injectorStrategy);
    }

    public Builder2 scopeResolvers(Function<Context1, List<ScopeResolver>> callback) {
      return new Builder2(context, callback.apply(context));
    }

    public Builder2 defaultScopeResolvers() {
      return scopeResolvers(context -> List.of());
    }
  }

  public static class Builder2 {
    private final Context2 context;

    Builder2(Context1 context, List<ScopeResolver> scopeResolvers) {
      List<InjectionTargetExtension<?, ?>> extensions = List.of(
        new ListInjectionTargetExtension<>(),
        new SetInjectionTargetExtension<>(),
        new ProviderInjectionTargetExtension<>(Provider.class, s -> s::get)
      );

      this.context = new Context2(context, scopeResolvers, extensions);
    }

    public Builder4 autoDiscovering() {
      return new Builder4(context, true);
    }

    public Builder4 manual() {
      return new Builder4(context, false);
    }
  }

  public static class Builder4 {
    private final Context4 context;

    Builder4(Context2 context, boolean autoDiscovery) {
      this.context = new Context4(context, autoDiscovery);
    }

    public Builder5 typeRegistrationExtensions(Function<Context4, List<TypeRegistrationExtension>> callback) {
      List<TypeRegistrationExtension> typeRegistrationExtensions = new ArrayList<>();

      typeRegistrationExtensions.add(new ProviderTypeRegistrationExtension(PROVIDER_METHOD));
      typeRegistrationExtensions.add(new ProducesTypeRegistrationExtension(Produces.class));
      typeRegistrationExtensions.addAll(callback.apply(context));

      return new Builder5(context, typeRegistrationExtensions);
    }

    public Builder5 defaultDiscoveryExtensions() {
      return typeRegistrationExtensions(context -> List.of());
    }
  }

  public static class Builder5 {
    private final Context5 context;

    Builder5(Context4 context, List<TypeRegistrationExtension> typeRegistrationExtensions) {
      this.context = new Context5(context, typeRegistrationExtensions);
    }

    public Injector build() {
      Annotation singletonAnnotation = context.injectorStrategy.getScopeStrategy().getSingletonAnnotation();
      List<ScopeResolver> scopeResolvers = context.scopeResolvers.stream().anyMatch(sr -> sr.getAnnotation().equals(singletonAnnotation)) ? context.scopeResolvers
        : Stream.concat(context.scopeResolvers.stream(), Stream.of(new SingletonScopeResolver(singletonAnnotation))).collect(Collectors.toList());

      return new StandardInjector(context.injectionTargetExtensions, context.typeRegistrationExtensions, scopeResolvers, context.injectorStrategy, context.autoDiscovery);
    }
  }
}
