package hs.ddif.core.definition;

import hs.ddif.annotations.Opt;
import hs.ddif.core.config.ConfigurableAnnotationStrategy;
import hs.ddif.core.config.DirectTypeExtension;
import hs.ddif.core.config.scope.SingletonScopeResolver;
import hs.ddif.core.config.standard.AnnotationBasedLifeCycleCallbacksFactory;
import hs.ddif.core.config.standard.DefaultInjectableFactory;
import hs.ddif.core.definition.bind.AnnotationStrategy;
import hs.ddif.core.definition.bind.BindingProvider;
import hs.ddif.core.instantiation.TypeExtension;
import hs.ddif.core.instantiation.TypeExtensionStore;
import hs.ddif.core.instantiation.TypeExtensions;
import hs.ddif.core.scope.ScopeResolver;
import hs.ddif.core.scope.ScopeResolverManager;
import hs.ddif.core.util.Annotations;

import java.lang.annotation.Annotation;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;

public class InjectableFactories {
  public static final AnnotationStrategy ANNOTATION_STRATEGY = new ConfigurableAnnotationStrategy(Inject.class, Qualifier.class, Scope.class, Opt.class);
  public static final BindingProvider BINDING_PROVIDER = new BindingProvider(ANNOTATION_STRATEGY);

  private final ScopeResolverManager scopeResolverManager;
  private final DefaultInjectableFactory factory;
  private final LifeCycleCallbacksFactory lifeCycleCallbacksFactory;
  private final Map<Class<?>, TypeExtension<?>> typeExtensions = TypeExtensions.create(ANNOTATION_STRATEGY);
  private final TypeExtensionStore typeExtensionStore = new TypeExtensionStore(new DirectTypeExtension<>(ANNOTATION_STRATEGY), typeExtensions);

  public InjectableFactories(ScopeResolverManager scopeResolverManager) {
    this.scopeResolverManager = scopeResolverManager;
    this.factory = new DefaultInjectableFactory(scopeResolverManager, ANNOTATION_STRATEGY, typeExtensions.keySet());
    this.lifeCycleCallbacksFactory = new AnnotationBasedLifeCycleCallbacksFactory(ANNOTATION_STRATEGY, PostConstruct.class, PreDestroy.class);
  }

  public InjectableFactories() {
    this(new ScopeResolverManager(new SingletonScopeResolver(Annotations.of(Singleton.class))));
  }

  public TypeExtensionStore getTypeExtensionStore() {
    return typeExtensionStore;
  }

  public ScopeResolverManager getScopeResolverManager() {
    return scopeResolverManager;
  }

  public ScopeResolver getScopeResolver(Annotation annotation) {
    return scopeResolverManager.getScopeResolver(annotation);
  }

  public ClassInjectableFactory forClass() {
    return new ClassInjectableFactory(BINDING_PROVIDER, factory, lifeCycleCallbacksFactory);
  }

  public FieldInjectableFactory forField() {
    return new FieldInjectableFactory(BINDING_PROVIDER, factory);
  }

  public MethodInjectableFactory forMethod() {
    return new MethodInjectableFactory(BINDING_PROVIDER, factory);
  }

  public InstanceInjectableFactory forInstance() {
    return new InstanceInjectableFactory(factory, Annotations.of(Singleton.class));
  }
}
