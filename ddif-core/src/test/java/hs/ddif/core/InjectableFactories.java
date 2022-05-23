package hs.ddif.core;

import hs.ddif.annotations.Opt;
import hs.ddif.core.definition.BindingProvider;
import hs.ddif.core.definition.ClassInjectableFactory;
import hs.ddif.core.definition.FieldInjectableFactory;
import hs.ddif.core.definition.InjectionTargetExtensionStore;
import hs.ddif.core.definition.InstanceInjectableFactory;
import hs.ddif.core.definition.MethodInjectableFactory;
import hs.ddif.core.instantiation.InjectionTargetExtensions;
import hs.ddif.core.test.scope.Dependent;
import hs.ddif.library.AnnotationBasedLifeCycleCallbacksFactory;
import hs.ddif.library.ConfigurableAnnotationStrategy;
import hs.ddif.library.NoProxyStrategy;
import hs.ddif.library.SimpleScopeStrategy;
import hs.ddif.library.SingletonScopeResolver;
import hs.ddif.spi.config.AnnotationStrategy;
import hs.ddif.spi.config.LifeCycleCallbacksFactory;
import hs.ddif.spi.config.ProxyStrategy;
import hs.ddif.spi.config.ScopeStrategy;
import hs.ddif.spi.instantiation.InjectionTargetExtension;
import hs.ddif.spi.scope.ScopeResolver;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;

public class InjectableFactories {
  public static final ProxyStrategy PROXY_STRATEGY = new NoProxyStrategy();
  public static final ScopeStrategy SCOPE_STRATEGY = new SimpleScopeStrategy(Scope.class, Singleton.class, Dependent.class);
  public static final AnnotationStrategy ANNOTATION_STRATEGY = new ConfigurableAnnotationStrategy(Inject.class, Qualifier.class, Opt.class);

  private final ScopeResolverManager scopeResolverManager;
  private final DefaultInjectableFactory factory;
  private final LifeCycleCallbacksFactory lifeCycleCallbacksFactory;
  private final List<InjectionTargetExtension<?, ?>> injectionTargetExtensions = InjectionTargetExtensions.create();
  private final InjectionTargetExtensionStore injectionTargetExtensionStore = new InjectionTargetExtensionStore(injectionTargetExtensions);
  private final BindingProvider bindingProvider = new BindingProvider(ANNOTATION_STRATEGY, injectionTargetExtensionStore);

  public InjectableFactories(ScopeResolverManager scopeResolverManager) {
    this.scopeResolverManager = scopeResolverManager;
    this.factory = new DefaultInjectableFactory(scopeResolverManager, ANNOTATION_STRATEGY, SCOPE_STRATEGY, injectionTargetExtensions.stream().map(InjectionTargetExtension::getTargetClass).collect(Collectors.toSet()));
    this.lifeCycleCallbacksFactory = new AnnotationBasedLifeCycleCallbacksFactory(PostConstruct.class, PreDestroy.class);
  }

  public InjectableFactories() {
    this(new ScopeResolverManager(List.of(new SingletonScopeResolver(Singleton.class)), Dependent.class));
  }

  public InjectionTargetExtensionStore getInjectionTargetExtensionStore() {
    return injectionTargetExtensionStore;
  }

  public ScopeResolverManager getScopeResolverManager() {
    return scopeResolverManager;
  }

  public ScopeResolver getScopeResolver(Class<? extends Annotation> annotation) {
    return scopeResolverManager.getScopeResolver(annotation);
  }

  public ClassInjectableFactory forClass() {
    return new ClassInjectableFactory(bindingProvider, factory, lifeCycleCallbacksFactory);
  }

  public FieldInjectableFactory forField() {
    return new FieldInjectableFactory(bindingProvider, factory);
  }

  public MethodInjectableFactory forMethod() {
    return new MethodInjectableFactory(bindingProvider, factory);
  }

  public InstanceInjectableFactory forInstance() {
    return new InstanceInjectableFactory(factory, Singleton.class);
  }
}
