package hs.ddif.core;

import hs.ddif.annotations.Opt;
import hs.ddif.annotations.Produces;
import hs.ddif.api.Injector;
import hs.ddif.api.annotation.InjectorStrategy;
import hs.ddif.api.definition.DiscoveryExtension;
import hs.ddif.api.definition.LifeCycleCallbacksFactory;
import hs.ddif.api.instantiation.TypeExtension;
import hs.ddif.api.scope.ScopeResolver;
import hs.ddif.core.config.AnnotationBasedLifeCycleCallbacksFactory;
import hs.ddif.core.config.ConfigurableAnnotationStrategy;
import hs.ddif.core.config.DefaultInjectorStrategy;
import hs.ddif.core.config.ListTypeExtension;
import hs.ddif.core.config.NoProxyStrategy;
import hs.ddif.core.config.ProducesDiscoveryExtension;
import hs.ddif.core.config.ProviderDiscoveryExtension;
import hs.ddif.core.config.ProviderTypeExtension;
import hs.ddif.core.config.SetTypeExtension;
import hs.ddif.core.config.SingletonScopeResolver;
import hs.ddif.core.definition.BindingProvider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Qualifier;

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
        InjectableFactories.SCOPE_STRATEGY,
        new NoProxyStrategy()
      ));
    }

    public Builder4 manual() {
      return defaultInjectorStrategy().defaultScopeResolvers().defaultLifeCycleCallbacksFactory().manual();
    }
  }

  public static class Context1 {
    public final InjectorStrategy injectorStrategy;

    Context1(InjectorStrategy injectorStrategy) {
      this.injectorStrategy = injectorStrategy;
    }
  }

  public static class Context2 extends Context1 {
    public final Map<Class<?>, TypeExtension<?>> typeExtensions;
    public final List<ScopeResolver> scopeResolvers;

    Context2(Context1 context, List<ScopeResolver> scopeResolvers, Map<Class<?>, TypeExtension<?>> typeExtensions) {
      super(context.injectorStrategy);

      this.scopeResolvers = scopeResolvers;
      this.typeExtensions = Collections.unmodifiableMap(new HashMap<>(typeExtensions));
    }
  }

  public static class Context3 extends Context2 {
    public final LifeCycleCallbacksFactory lifeCycleCallbacksFactory;
    public final BindingProvider bindingProvider;

    Context3(Context2 context, LifeCycleCallbacksFactory lifeCycleCallbacksFactory, BindingProvider bindingProvider) {
      super(context, context.scopeResolvers, context.typeExtensions);

      this.lifeCycleCallbacksFactory = lifeCycleCallbacksFactory;
      this.bindingProvider = bindingProvider;
    }
  }

  public static class Context4 extends Context3 {
    public final boolean autoDiscovery;

    Context4(Context3 context, boolean autoDiscovery) {
      super(context, context.lifeCycleCallbacksFactory, context.bindingProvider);

      this.autoDiscovery = autoDiscovery;
    }
  }

  public static class Context5 extends Context4 {
    public final List<DiscoveryExtension> discoveryExtensions;

    Context5(Context4 context, List<DiscoveryExtension> discoveryExtensions) {
      super(context, context.autoDiscovery);

      this.discoveryExtensions = discoveryExtensions;
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
      Map<Class<?>, TypeExtension<?>> typeExtensions = new HashMap<>();

      typeExtensions.put(List.class, new ListTypeExtension<>(context.injectorStrategy.getAnnotationStrategy()));
      typeExtensions.put(Set.class, new SetTypeExtension<>(context.injectorStrategy.getAnnotationStrategy()));
      typeExtensions.put(Provider.class, new ProviderTypeExtension<>(Provider.class, s -> s::get));

      this.context = new Context2(context, scopeResolvers, typeExtensions);
    }

    public Builder3 lifeCycleCallbacksFactory(Function<Context2, LifeCycleCallbacksFactory> callback) {
      LifeCycleCallbacksFactory lifeCycleCallbacksFactory = callback.apply(context);
      BindingProvider bindingProvider = new BindingProvider(context.injectorStrategy.getAnnotationStrategy());

      return new Builder3(context, lifeCycleCallbacksFactory, bindingProvider);
    }

    public Builder3 defaultLifeCycleCallbacksFactory() {
      return lifeCycleCallbacksFactory(context -> new AnnotationBasedLifeCycleCallbacksFactory(context.injectorStrategy.getAnnotationStrategy(), PostConstruct.class, PreDestroy.class));
    }
  }

  public static class Builder3 {
    private final Context3 context;

    Builder3(Context2 context, LifeCycleCallbacksFactory lifeCycleCallbacksFactory, BindingProvider bindingProvider) {
      this.context = new Context3(context, lifeCycleCallbacksFactory, bindingProvider);
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

    Builder4(Context3 context, boolean autoDiscovery) {
      this.context = new Context4(context, autoDiscovery);
    }

    public Builder5 discoveryExtensions(Function<Context4, List<DiscoveryExtension>> callback) {
      List<DiscoveryExtension> discoveryExtensions = new ArrayList<>();

      discoveryExtensions.add(new ProviderDiscoveryExtension(PROVIDER_METHOD));
      discoveryExtensions.add(new ProducesDiscoveryExtension(Produces.class));
      discoveryExtensions.addAll(callback.apply(context));

      return new Builder5(context, discoveryExtensions);
    }

    public Builder5 defaultDiscoveryExtensions() {
      return discoveryExtensions(context -> List.of());
    }
  }

  public static class Builder5 {
    private final Context5 context;

    Builder5(Context4 context, List<DiscoveryExtension> discoveryExtensions) {
      this.context = new Context5(context, discoveryExtensions);
    }

    public Injector build() {
      Class<? extends Annotation> singletonAnnotationClass = context.injectorStrategy.getScopeStrategy().getSingletonAnnotationClass();
      List<ScopeResolver> scopeResolvers = context.scopeResolvers.stream().anyMatch(sr -> sr.getAnnotationClass() == singletonAnnotationClass) ? context.scopeResolvers
        : Stream.concat(context.scopeResolvers.stream(), Stream.of(new SingletonScopeResolver(singletonAnnotationClass))).collect(Collectors.toList());

      return new StandardInjector(context.typeExtensions, context.discoveryExtensions, scopeResolvers, context.injectorStrategy, context.bindingProvider, context.lifeCycleCallbacksFactory, context.autoDiscovery);
    }
  }
}
