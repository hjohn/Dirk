package org.int4.dirk.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.int4.dirk.api.CandidateRegistry;
import org.int4.dirk.api.Injector;
import org.int4.dirk.api.InstanceResolver;
import org.int4.dirk.api.TypeLiteral;
import org.int4.dirk.api.definition.AutoDiscoveryException;
import org.int4.dirk.api.definition.DefinitionException;
import org.int4.dirk.api.definition.DependencyException;
import org.int4.dirk.api.instantiation.AmbiguousResolutionException;
import org.int4.dirk.api.instantiation.CreationException;
import org.int4.dirk.api.instantiation.UnsatisfiedResolutionException;
import org.int4.dirk.api.scope.ScopeNotActiveException;
import org.int4.dirk.core.definition.BindingProvider;
import org.int4.dirk.core.definition.ClassInjectableFactory;
import org.int4.dirk.core.definition.FieldInjectableFactory;
import org.int4.dirk.core.definition.InjectableFactory;
import org.int4.dirk.core.definition.InjectionTargetExtensionStore;
import org.int4.dirk.core.definition.InstanceInjectableFactory;
import org.int4.dirk.core.definition.MethodInjectableFactory;
import org.int4.dirk.core.discovery.DiscovererFactory;
import org.int4.dirk.core.store.InjectableStore;
import org.int4.dirk.spi.config.InjectorStrategy;
import org.int4.dirk.spi.definition.TypeRegistrationExtension;
import org.int4.dirk.spi.instantiation.InjectionTargetExtension;
import org.int4.dirk.spi.scope.ScopeResolver;

/**
 * A standard implementation of {@link Injector} provided with the framework.
 */
public class StandardInjector implements Injector {
  private final InstanceResolver instanceResolver;
  private final CandidateRegistry registry;

  /**
   * Constructs a new instance.
   *
   * @param injectionTargetExtensions a collection of {@link InjectionTargetExtension}s, cannot be {@code null} or contain {@code null} but can be empty
   * @param typeRegistrationExtensions a collection of {@link TypeRegistrationExtension}s, cannot be {@code null} or contain {@code null} but can be empty
   * @param scopeResolvers a collection of {@link ScopeResolver}s, cannot be {@code null} or contain {@code null} but can be empty
   * @param strategy an {@link InjectorStrategy}, cannot be {@code null}
   * @param autoDiscovery {@code true} if the injector should automatically register (auto discover) types encountered during instantiation that have not been explicitly registered, or {code false} to allow manual registration only
   */
  public StandardInjector(Collection<InjectionTargetExtension<?, ?>> injectionTargetExtensions, Collection<TypeRegistrationExtension> typeRegistrationExtensions, Collection<ScopeResolver> scopeResolvers, InjectorStrategy strategy, boolean autoDiscovery) {
    Objects.requireNonNull(injectionTargetExtensions, "injectionTargetExtensions cannot be null");
    Objects.requireNonNull(typeRegistrationExtensions, "typeRegistrationExtensions cannot be null");
    Objects.requireNonNull(scopeResolvers, "scopeResolvers cannot be null");
    Objects.requireNonNull(strategy, "strategy cannot be null");

    InjectionTargetExtensionStore injectionTargetExtensionStore = new InjectionTargetExtensionStore(injectionTargetExtensions);
    InstanceFactory instanceFactory = new InstanceFactory(strategy.getAnnotationStrategy(), strategy.getProxyStrategy(), injectionTargetExtensionStore);
    InjectableFactory injectableFactory = new DefaultInjectableFactory(
      new ScopeResolverManager(scopeResolvers, strategy.getScopeStrategy().getDependentAnnotation()),
      instanceFactory,
      strategy.getAnnotationStrategy(),
      strategy.getScopeStrategy(),
      injectionTargetExtensions.stream().map(InjectionTargetExtension::getTargetClass).collect(Collectors.toSet())
    );

    BindingProvider bindingProvider = new BindingProvider(strategy.getAnnotationStrategy());
    DiscovererFactory discovererFactory = new DefaultDiscovererFactory(
      autoDiscovery,
      typeRegistrationExtensions,
      new ClassInjectableFactory(bindingProvider, injectableFactory, strategy.getLifeCycleCallbacksFactory()),
      new MethodInjectableFactory(bindingProvider, injectableFactory),
      new FieldInjectableFactory(bindingProvider, injectableFactory)
    );

    InjectableStore store = new InjectableStore(strategy.getProxyStrategy());
    InstanceInjectableFactory instanceInjectableFactory = new InstanceInjectableFactory(injectableFactory, strategy.getScopeStrategy().getSingletonAnnotation());

    this.registry = new InjectableStoreCandidateRegistry(store, discovererFactory, instanceInjectableFactory);
    this.instanceResolver = new DefaultInstanceResolver(store, instanceFactory);
  }

  @Override
  public InstanceResolver getInstanceResolver() {
    return instanceResolver;
  }

  @Override
  public CandidateRegistry getCandidateRegistry() {
    return registry;
  }

  @Override
  public <T> T getInstance(TypeLiteral<T> typeLiteral, Object... qualifiers) throws UnsatisfiedResolutionException, AmbiguousResolutionException, CreationException, ScopeNotActiveException {
    return instanceResolver.getInstance(typeLiteral, qualifiers);
  }

  @Override
  public <T> T getInstance(Class<T> cls, Object... qualifiers) throws UnsatisfiedResolutionException, AmbiguousResolutionException, CreationException, ScopeNotActiveException {
    return instanceResolver.getInstance(cls, qualifiers);
  }

  @Override
  public <T> List<T> getInstances(TypeLiteral<T> typeLiteral, Object... qualifiers) throws CreationException {
    return instanceResolver.getInstances(typeLiteral, qualifiers);
  }

  @Override
  public <T> List<T> getInstances(Class<T> cls, Object... qualifiers) throws CreationException {
    return instanceResolver.getInstances(cls, qualifiers);
  }

  @Override
  public boolean contains(Type type, Object... qualifiers) {
    return registry.contains(type, qualifiers);
  }

  @Override
  public void register(Type concreteType) throws AutoDiscoveryException, DefinitionException, DependencyException {
    registry.register(concreteType);
  }

  @Override
  public void register(Collection<Type> concreteTypes) throws AutoDiscoveryException, DefinitionException, DependencyException {
    registry.register(concreteTypes);
  }

  @Override
  public void registerInstance(Object instance, Annotation... qualifiers) throws DefinitionException, DependencyException {
    registry.registerInstance(instance, qualifiers);
  }

  @Override
  public void remove(Type concreteType) throws AutoDiscoveryException, DefinitionException, DependencyException {
    registry.remove(concreteType);
  }

  @Override
  public void remove(Collection<Type> concreteTypes) throws AutoDiscoveryException, DefinitionException, DependencyException {
    registry.remove(concreteTypes);
  }

  @Override
  public void removeInstance(Object instance, Annotation... qualifiers) throws DefinitionException, DependencyException {
    registry.removeInstance(instance, qualifiers);
  }
}
