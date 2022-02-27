package hs.ddif.core.definition;

import hs.ddif.core.config.ConfigurableAnnotationStrategy;
import hs.ddif.core.config.scope.SingletonScopeResolver;
import hs.ddif.core.config.standard.ConcreteClassInjectableFactoryTemplate;
import hs.ddif.core.config.standard.DefaultInjectableFactory;
import hs.ddif.core.config.standard.DelegatingClassInjectableFactory;
import hs.ddif.core.definition.bind.AnnotationStrategy;
import hs.ddif.core.definition.bind.BindingProvider;
import hs.ddif.core.scope.ScopeResolver;
import hs.ddif.core.scope.ScopeResolverManager;
import hs.ddif.core.util.Annotations;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.inject.Scope;
import javax.inject.Singleton;

public class InjectableFactories {
  private static final AnnotationStrategy ANNOTATION_STRATEGY = new ConfigurableAnnotationStrategy(Annotations.of(Inject.class), Annotations.of(Qualifier.class), Annotations.of(Scope.class));
  private static final BindingProvider BINDING_PROVIDER = new BindingProvider(ANNOTATION_STRATEGY);

  private final ScopeResolverManager scopeResolverManager;
  private final DefaultInjectableFactory factory;

  public InjectableFactories(ScopeResolverManager scopeResolverManager) {
    this.scopeResolverManager = scopeResolverManager;
    this.factory = new DefaultInjectableFactory(scopeResolverManager, ANNOTATION_STRATEGY);
  }

  public InjectableFactories() {
    this(new ScopeResolverManager(new SingletonScopeResolver(Annotations.of(Singleton.class))));
  }

  public ScopeResolverManager getScopeResolverManager() {
    return scopeResolverManager;
  }

  public ScopeResolver getScopeResolver(Annotation annotation) {
    return scopeResolverManager.getScopeResolver(annotation);
  }

  public ClassInjectableFactory forClass() {
    return new DelegatingClassInjectableFactory(List.of(new ConcreteClassInjectableFactoryTemplate(BINDING_PROVIDER, factory)));
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
