package hs.ddif.core;

import hs.ddif.api.CandidateRegistry;
import hs.ddif.api.Injector;
import hs.ddif.api.InstanceResolver;
import hs.ddif.api.annotation.InjectorStrategy;
import hs.ddif.api.definition.AutoDiscoveryException;
import hs.ddif.api.definition.DefinitionException;
import hs.ddif.api.definition.DiscoveryExtension;
import hs.ddif.api.definition.LifeCycleCallbacksFactory;
import hs.ddif.api.instantiation.InstantiatorFactory;
import hs.ddif.api.instantiation.TypeExtension;
import hs.ddif.api.instantiation.domain.InstanceCreationException;
import hs.ddif.api.instantiation.domain.MultipleInstancesException;
import hs.ddif.api.instantiation.domain.NoSuchInstanceException;
import hs.ddif.api.scope.ScopeResolver;
import hs.ddif.core.config.DirectTypeExtension;
import hs.ddif.core.definition.BindingProvider;
import hs.ddif.core.definition.ClassInjectableFactory;
import hs.ddif.core.definition.FieldInjectableFactory;
import hs.ddif.core.definition.InjectableFactory;
import hs.ddif.core.definition.InstanceInjectableFactory;
import hs.ddif.core.definition.MethodInjectableFactory;
import hs.ddif.core.discovery.DiscovererFactory;
import hs.ddif.core.inject.store.InjectableStore;
import hs.ddif.core.inject.store.InstantiatorBindingMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A standard implementation of {@link Injector} provided with the framework.
 */
public class StandardInjector implements Injector {
  private final InstanceResolver instanceResolver;
  private final CandidateRegistry registry;

  /**
   * Constructs a new instance.
   *
   * @param typeExtensions a map of {@link TypeExtension}s, cannot be {@code null} or contain {@code null} but can be empty
   * @param discoveryExtensions a list of {@link DiscoveryExtension}s, cannot be {@code null} or contain {@code null} but can be empty
   * @param scopeResolvers a list of {@link ScopeResolver}s, cannot be {@code null} or contain {@code null} but can be empty
   * @param strategy an {@link InjectorStrategy}, cannot be {@code null}
   * @param bindingProvider a {@link BindingProvider}, cannot be {@code null}
   * @param lifeCycleCallbacksFactory a {@link LifeCycleCallbacksFactory}, cannot be @{code null}
   * @param autoDiscovery {@code true} if the injector should automatically register (auto discover) types encountered during instantiation that have not been explicitly registered, or {code false} to allow manual registration only
   */
  public StandardInjector(Map<Class<?>, TypeExtension<?>> typeExtensions, List<DiscoveryExtension> discoveryExtensions, List<ScopeResolver> scopeResolvers, InjectorStrategy strategy, BindingProvider bindingProvider, LifeCycleCallbacksFactory lifeCycleCallbacksFactory, boolean autoDiscovery) {
    Objects.requireNonNull(typeExtensions, "typeExtensions cannot be null");
    Objects.requireNonNull(discoveryExtensions, "discoveryExtensions cannot be null");
    Objects.requireNonNull(scopeResolvers, "scopeResolvers cannot be null");
    Objects.requireNonNull(strategy, "strategy cannot be null");
    Objects.requireNonNull(bindingProvider, "bindingProvider cannot be null");
    Objects.requireNonNull(lifeCycleCallbacksFactory, "lifeCycleCallbacksFactory cannot be null");

    InjectableFactory injectableFactory = new DefaultInjectableFactory(
      new ScopeResolverManager(scopeResolvers, strategy.getScopeStrategy().getDependentAnnotationClass()),
      strategy.getAnnotationStrategy(),
      strategy.getScopeStrategy(),
      typeExtensions.keySet()
    );

    InstantiatorFactory instantiatorFactory = new DefaultInstantiatorFactory(new TypeExtensionStore(new DirectTypeExtension<>(strategy.getAnnotationStrategy()), typeExtensions));

    DiscovererFactory discovererFactory = new DefaultDiscovererFactory(
      autoDiscovery,
      discoveryExtensions,
      instantiatorFactory,
      new ClassInjectableFactory(bindingProvider, injectableFactory, lifeCycleCallbacksFactory),
      new MethodInjectableFactory(bindingProvider, injectableFactory),
      new FieldInjectableFactory(bindingProvider, injectableFactory)
    );

    InstantiatorBindingMap instantiatorBindingMap = new InstantiatorBindingMap(instantiatorFactory);
    InjectableStore store = new InjectableStore(instantiatorBindingMap);
    InstanceInjectableFactory instanceInjectableFactory = new InstanceInjectableFactory(injectableFactory, strategy.getScopeStrategy().getSingletonAnnotationClass());

    this.registry = new InjectableStoreCandidateRegistry(store, discovererFactory, instanceInjectableFactory);
    this.instanceResolver = new DefaultInstanceResolver(store, discovererFactory, new DefaultInstantiationContext(store, instantiatorBindingMap), instantiatorFactory);
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
  public <T> T getInstance(Type type, Object... qualifiers) throws NoSuchInstanceException, MultipleInstancesException, InstanceCreationException, AutoDiscoveryException {
    return instanceResolver.getInstance(type, qualifiers);
  }

  @Override
  public <T> T getInstance(Class<T> cls, Object... qualifiers) throws NoSuchInstanceException, MultipleInstancesException, InstanceCreationException, AutoDiscoveryException {
    return instanceResolver.getInstance(cls, qualifiers);
  }

  @Override
  public <T> List<T> getInstances(Type type, Object... qualifiers) throws InstanceCreationException {
    return instanceResolver.getInstances(type, qualifiers);
  }

  @Override
  public <T> List<T> getInstances(Class<T> cls, Object... qualifiers) throws InstanceCreationException {
    return instanceResolver.getInstances(cls, qualifiers);
  }

  @Override
  public boolean contains(Type type, Object... qualifiers) {
    return registry.contains(type, qualifiers);
  }

  @Override
  public void register(Type concreteType) throws AutoDiscoveryException, DefinitionException {
    registry.register(concreteType);
  }

  @Override
  public void register(List<Type> concreteTypes) throws AutoDiscoveryException, DefinitionException {
    registry.register(concreteTypes);
  }

  @Override
  public void registerInstance(Object instance, Annotation... qualifiers) throws DefinitionException {
    registry.registerInstance(instance, qualifiers);
  }

  @Override
  public void remove(Type concreteType) throws AutoDiscoveryException, DefinitionException {
    registry.remove(concreteType);
  }

  @Override
  public void remove(List<Type> concreteTypes) throws AutoDiscoveryException, DefinitionException {
    registry.remove(concreteTypes);
  }

  @Override
  public void removeInstance(Object instance) throws DefinitionException {
    registry.removeInstance(instance);
  }
}
