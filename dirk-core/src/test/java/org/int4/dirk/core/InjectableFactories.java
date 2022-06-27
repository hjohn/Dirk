package org.int4.dirk.core;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.int4.dirk.annotations.Opt;
import org.int4.dirk.core.definition.BindingProvider;
import org.int4.dirk.core.definition.ClassInjectableFactory;
import org.int4.dirk.core.definition.FieldInjectableFactory;
import org.int4.dirk.core.definition.InjectionTargetExtensionStore;
import org.int4.dirk.core.definition.InstanceInjectableFactory;
import org.int4.dirk.core.definition.MethodInjectableFactory;
import org.int4.dirk.core.instantiation.InjectionTargetExtensions;
import org.int4.dirk.core.test.scope.Dependent;
import org.int4.dirk.library.AnnotationBasedLifeCycleCallbacksFactory;
import org.int4.dirk.library.ConfigurableAnnotationStrategy;
import org.int4.dirk.library.NoProxyStrategy;
import org.int4.dirk.library.SimpleScopeStrategy;
import org.int4.dirk.library.SingletonScopeResolver;
import org.int4.dirk.spi.config.AnnotationStrategy;
import org.int4.dirk.spi.config.LifeCycleCallbacksFactory;
import org.int4.dirk.spi.config.ProxyStrategy;
import org.int4.dirk.spi.config.ScopeStrategy;
import org.int4.dirk.spi.instantiation.InjectionTargetExtension;
import org.int4.dirk.spi.scope.ScopeResolver;
import org.int4.dirk.util.Annotations;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;

public class InjectableFactories {
  public static final ProxyStrategy PROXY_STRATEGY = new NoProxyStrategy();
  public static final ScopeStrategy SCOPE_STRATEGY = new SimpleScopeStrategy(Scope.class, Annotations.of(Dependent.class), Annotations.of(Singleton.class), Annotations.of(Dependent.class));
  public static final AnnotationStrategy ANNOTATION_STRATEGY = new ConfigurableAnnotationStrategy(Inject.class, Qualifier.class, Opt.class);

  private final ScopeResolverManager scopeResolverManager;
  private final DefaultInjectableFactory factory;
  private final LifeCycleCallbacksFactory lifeCycleCallbacksFactory;
  private final Collection<InjectionTargetExtension<?, ?>> injectionTargetExtensions;
  private final InjectionTargetExtensionStore injectionTargetExtensionStore;
  private final BindingProvider bindingProvider;
  private final InstantiationContextFactory instantiationContextFactory;

  public InjectableFactories(ScopeResolverManager scopeResolverManager, Collection<InjectionTargetExtension<?, ?>> extensions) {
    this.injectionTargetExtensions = extensions;
    this.scopeResolverManager = scopeResolverManager;
    this.injectionTargetExtensionStore = new InjectionTargetExtensionStore(injectionTargetExtensions);
    this.bindingProvider = new BindingProvider(ANNOTATION_STRATEGY);
    this.instantiationContextFactory = new InstantiationContextFactory(InjectableFactories.ANNOTATION_STRATEGY, InjectableFactories.PROXY_STRATEGY, injectionTargetExtensionStore);
    this.factory = new DefaultInjectableFactory(scopeResolverManager, instantiationContextFactory, ANNOTATION_STRATEGY, SCOPE_STRATEGY, injectionTargetExtensions.stream().map(InjectionTargetExtension::getTargetClass).collect(Collectors.toSet()));
    this.lifeCycleCallbacksFactory = new AnnotationBasedLifeCycleCallbacksFactory(PostConstruct.class, PreDestroy.class);
  }

  public InjectableFactories(ScopeResolverManager scopeResolverManager) {
    this(scopeResolverManager, InjectionTargetExtensions.create());
  }

  public InjectableFactories() {
    this(new ScopeResolverManager(List.of(new SingletonScopeResolver(Annotations.of(Singleton.class))), Annotations.of(Dependent.class)));
  }

  public InjectionTargetExtensionStore getInjectionTargetExtensionStore() {
    return injectionTargetExtensionStore;
  }

  public InstantiationContextFactory getInstantiationContextFactory() {
    return instantiationContextFactory;
  }

  public ScopeResolverManager getScopeResolverManager() {
    return scopeResolverManager;
  }

  public ScopeResolver getScopeResolver(Annotation annotation) {
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
    return new InstanceInjectableFactory(factory, Annotations.of(Singleton.class));
  }
}
