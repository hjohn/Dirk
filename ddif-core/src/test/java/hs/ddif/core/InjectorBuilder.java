package hs.ddif.core;

import hs.ddif.annotations.Opt;
import hs.ddif.annotations.Produces;
import hs.ddif.core.config.ConfigurableAnnotationStrategy;
import hs.ddif.core.config.DirectTypeExtension;
import hs.ddif.core.config.ListTypeExtension;
import hs.ddif.core.config.ProducesDiscoveryExtension;
import hs.ddif.core.config.ProviderDiscoveryExtension;
import hs.ddif.core.config.ProviderTypeExtension;
import hs.ddif.core.config.SetTypeExtension;
import hs.ddif.core.config.discovery.DiscovererFactory;
import hs.ddif.core.config.scope.SingletonScopeResolver;
import hs.ddif.core.config.standard.AnnotationBasedLifeCycleCallbacksFactory;
import hs.ddif.core.config.standard.DefaultDiscovererFactory;
import hs.ddif.core.config.standard.DefaultInjectableFactory;
import hs.ddif.core.config.standard.DiscoveryExtension;
import hs.ddif.core.definition.ClassInjectableFactory;
import hs.ddif.core.definition.FieldInjectableFactory;
import hs.ddif.core.definition.InjectableFactory;
import hs.ddif.core.definition.InstanceInjectableFactory;
import hs.ddif.core.definition.LifeCycleCallbacksFactory;
import hs.ddif.core.definition.MethodInjectableFactory;
import hs.ddif.core.definition.bind.AnnotationStrategy;
import hs.ddif.core.definition.bind.BindingProvider;
import hs.ddif.core.instantiation.TypeExtension;
import hs.ddif.core.instantiation.TypeExtensionStore;
import hs.ddif.core.scope.ScopeResolver;
import hs.ddif.core.scope.ScopeResolverManager;
import hs.ddif.core.util.Annotations;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;

public class InjectorBuilder {
  private static final Singleton SINGLETON = Annotations.of(Singleton.class);

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

    public Builder1 annotationStrategy(AnnotationStrategy annotationStrategy) {
      return new Builder1(annotationStrategy);
    }

    public Builder1 defaultAnnotationStrategy() {
      return new Builder1(new ConfigurableAnnotationStrategy(Inject.class, Qualifier.class, Scope.class, Opt.class));
    }

    public Builder4 manual() {
      return defaultAnnotationStrategy().defaultScopeResolvers().defaultLifeCycleCallbacksFactory().manual();
    }
  }

  public static class Context1 {
    public final AnnotationStrategy annotationStrategy;

    Context1(AnnotationStrategy annotationStrategy) {
      this.annotationStrategy = annotationStrategy;
    }
  }

  public static class Context2 extends Context1 {
    public final ScopeResolverManager scopeResolverManager;
    public final InjectableFactory injectableFactory;
    public final Map<Class<?>, TypeExtension<?>> typeExtensions;

    Context2(Context1 context, ScopeResolverManager scopeResolverManager, Map<Class<?>, TypeExtension<?>> typeExtensions) {
      super(context.annotationStrategy);

      this.scopeResolverManager = scopeResolverManager;
      this.typeExtensions = Collections.unmodifiableMap(new HashMap<>(typeExtensions));
      this.injectableFactory = new DefaultInjectableFactory(scopeResolverManager, annotationStrategy, this.typeExtensions.keySet());
    }
  }

  public static class Context3 extends Context2 {
    public final LifeCycleCallbacksFactory lifeCycleCallbacksFactory;
    public final BindingProvider bindingProvider;
    public final ClassInjectableFactory classInjectableFactory;
    public final MethodInjectableFactory methodInjectableFactory;
    public final FieldInjectableFactory fieldInjectableFactory;

    Context3(Context2 context, LifeCycleCallbacksFactory lifeCycleCallbacksFactory, BindingProvider bindingProvider, ClassInjectableFactory classInjectableFactory, MethodInjectableFactory methodInjectableFactory, FieldInjectableFactory fieldInjectableFactory) {
      super(context, context.scopeResolverManager, context.typeExtensions);

      this.lifeCycleCallbacksFactory = lifeCycleCallbacksFactory;
      this.bindingProvider = bindingProvider;
      this.classInjectableFactory = classInjectableFactory;
      this.methodInjectableFactory = methodInjectableFactory;
      this.fieldInjectableFactory = fieldInjectableFactory;
    }
  }

  public static class Context4 extends Context3 {
    public final boolean autoDiscovery;

    Context4(Context3 context, boolean autoDiscovery) {
      super(context, context.lifeCycleCallbacksFactory, context.bindingProvider, context.classInjectableFactory, context.methodInjectableFactory, context.fieldInjectableFactory);

      this.autoDiscovery = autoDiscovery;
    }
  }

  public static class Context5 extends Context4 {
    public final DiscovererFactory discovererFactory;

    Context5(Context4 context, DiscovererFactory discovererFactory) {
      super(context, context.autoDiscovery);

      this.discovererFactory = discovererFactory;
    }
  }

  public static class Builder1 {
    private final Context1 context;

    Builder1(AnnotationStrategy annotationStrategy) {
      this.context = new Context1(annotationStrategy);
    }

    public Builder2 scopeResolvers(Function<Context1, List<ScopeResolver>> callback) {
      return new Builder2(context, new SingletonScopeResolver(SINGLETON), callback.apply(context));
    }

    public Builder2 defaultScopeResolvers() {
      return scopeResolvers(context -> List.of());
    }
  }

  public static class Builder2 {
    private final Context2 context;

    Builder2(Context1 context, SingletonScopeResolver singletonScopeResolver, List<ScopeResolver> scopeResolvers) {
      Map<Class<?>, TypeExtension<?>> typeExtensions = new HashMap<>();

      typeExtensions.put(List.class, new ListTypeExtension<>(context.annotationStrategy));
      typeExtensions.put(Set.class, new SetTypeExtension<>(context.annotationStrategy));
      typeExtensions.put(Provider.class, new ProviderTypeExtension<>(Provider.class, s -> s::get));

      this.context = new Context2(context, createScopeResolverManager(singletonScopeResolver, scopeResolvers), typeExtensions);
    }

    public Builder3 lifeCycleCallbacksFactory(Function<Context2, LifeCycleCallbacksFactory> callback) {
      LifeCycleCallbacksFactory lifeCycleCallbacksFactory = callback.apply(context);

      BindingProvider bindingProvider = new BindingProvider(context.annotationStrategy);
      ClassInjectableFactory classInjectableFactory = new ClassInjectableFactory(bindingProvider, context.injectableFactory, lifeCycleCallbacksFactory);
      MethodInjectableFactory methodInjectableFactory = new MethodInjectableFactory(bindingProvider, context.injectableFactory);
      FieldInjectableFactory fieldInjectableFactory = new FieldInjectableFactory(bindingProvider, context.injectableFactory);

      return new Builder3(context, lifeCycleCallbacksFactory, bindingProvider, classInjectableFactory, methodInjectableFactory, fieldInjectableFactory);
    }

    public Builder3 defaultLifeCycleCallbacksFactory() {
      return lifeCycleCallbacksFactory(context -> new AnnotationBasedLifeCycleCallbacksFactory(context.annotationStrategy, PostConstruct.class, PreDestroy.class));
    }

    private static ScopeResolverManager createScopeResolverManager(SingletonScopeResolver singletonScopeResolver, List<ScopeResolver> scopeResolvers) {
      List<ScopeResolver> standardScopeResolvers = List.of(singletonScopeResolver);

      return new ScopeResolverManager(Stream.of(scopeResolvers, standardScopeResolvers).flatMap(Collection::stream).toArray(ScopeResolver[]::new));
    }
  }

  public static class Builder3 {
    private final Context3 context;

    Builder3(Context2 context, LifeCycleCallbacksFactory lifeCycleCallbacksFactory, BindingProvider bindingProvider, ClassInjectableFactory classInjectableFactory, MethodInjectableFactory methodInjectableFactory, FieldInjectableFactory fieldInjectableFactory) {
      this.context = new Context3(context, lifeCycleCallbacksFactory, bindingProvider, classInjectableFactory, methodInjectableFactory, fieldInjectableFactory);
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

    public Builder5 injectableExtensions(Function<Context4, List<DiscoveryExtension>> callback) {
      List<DiscoveryExtension> injectableExtensions = new ArrayList<>();

      injectableExtensions.add(new ProviderDiscoveryExtension(PROVIDER_METHOD));
      injectableExtensions.add(new ProducesDiscoveryExtension(Produces.class));
      injectableExtensions.addAll(callback.apply(context));

      return new Builder5(context, new DefaultDiscovererFactory(context.autoDiscovery, injectableExtensions, context.classInjectableFactory, context.methodInjectableFactory, context.fieldInjectableFactory));
    }

    public Builder5 defaultInjectableExtensions() {
      return injectableExtensions(context -> List.of());
    }
  }

  public static class Builder5 {
    private final Context5 context;

    Builder5(Context4 context, DiscovererFactory discovererFactory) {
      this.context = new Context5(context, discovererFactory);
    }

    public Injector build() {
      InstanceInjectableFactory instanceInjectableFactory = new InstanceInjectableFactory(context.injectableFactory, SINGLETON);

      TypeExtensionStore store = new TypeExtensionStore(new DirectTypeExtension<>(context.annotationStrategy), context.typeExtensions);

      return new Injector(store, context.discovererFactory, instanceInjectableFactory);
    }
  }
}
