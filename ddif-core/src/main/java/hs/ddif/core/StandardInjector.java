package hs.ddif.core;

import hs.ddif.api.CandidateRegistry;
import hs.ddif.api.Injector;
import hs.ddif.api.InstanceResolver;
import hs.ddif.api.definition.AutoDiscoveryException;
import hs.ddif.api.definition.DefinitionException;
import hs.ddif.api.definition.DependencyException;
import hs.ddif.api.instantiation.AmbiguousResolutionException;
import hs.ddif.api.instantiation.CreationException;
import hs.ddif.api.instantiation.UnsatisfiedResolutionException;
import hs.ddif.api.scope.ScopeNotActiveException;
import hs.ddif.core.definition.BindingProvider;
import hs.ddif.core.definition.ClassInjectableFactory;
import hs.ddif.core.definition.FieldInjectableFactory;
import hs.ddif.core.definition.InjectableFactory;
import hs.ddif.core.definition.InstanceInjectableFactory;
import hs.ddif.core.definition.MethodInjectableFactory;
import hs.ddif.core.discovery.DiscovererFactory;
import hs.ddif.core.store.InjectableStore;
import hs.ddif.core.store.InstantiatorBindingMap;
import hs.ddif.spi.config.InjectorStrategy;
import hs.ddif.spi.discovery.TypeRegistrationExtension;
import hs.ddif.spi.instantiation.InjectionTargetExtension;
import hs.ddif.spi.instantiation.InstantiatorFactory;
import hs.ddif.spi.scope.ScopeResolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
   * @param scopeResolvers a list of {@link ScopeResolver}s, cannot be {@code null} or contain {@code null} but can be empty
   * @param strategy an {@link InjectorStrategy}, cannot be {@code null}
   * @param autoDiscovery {@code true} if the injector should automatically register (auto discover) types encountered during instantiation that have not been explicitly registered, or {code false} to allow manual registration only
   */
  public StandardInjector(Collection<InjectionTargetExtension<?>> injectionTargetExtensions, Collection<TypeRegistrationExtension> typeRegistrationExtensions, List<ScopeResolver> scopeResolvers, InjectorStrategy strategy, boolean autoDiscovery) {
    Objects.requireNonNull(injectionTargetExtensions, "injectionTargetExtensions cannot be null");
    Objects.requireNonNull(typeRegistrationExtensions, "typeRegistrationExtensions cannot be null");
    Objects.requireNonNull(scopeResolvers, "scopeResolvers cannot be null");
    Objects.requireNonNull(strategy, "strategy cannot be null");

    InjectableFactory injectableFactory = new DefaultInjectableFactory(
      new ScopeResolverManager(scopeResolvers, strategy.getScopeStrategy().getDependentAnnotationClass()),
      strategy.getAnnotationStrategy(),
      strategy.getScopeStrategy(),
      injectionTargetExtensions.stream().map(InjectionTargetExtension::getInstantiatorType).collect(Collectors.toSet())
    );

    InstantiatorFactory instantiatorFactory = new DefaultInstantiatorFactory(new InjectionTargetExtensionStore(new DirectInjectionTargetExtension<>(), injectionTargetExtensions));
    BindingProvider bindingProvider = new BindingProvider(strategy.getAnnotationStrategy());
    DiscovererFactory discovererFactory = new DefaultDiscovererFactory(
      autoDiscovery,
      typeRegistrationExtensions,
      instantiatorFactory,
      new ClassInjectableFactory(bindingProvider, injectableFactory, strategy.getLifeCycleCallbacksFactory()),
      new MethodInjectableFactory(bindingProvider, injectableFactory),
      new FieldInjectableFactory(bindingProvider, injectableFactory)
    );

    InstantiatorBindingMap instantiatorBindingMap = new InstantiatorBindingMap(instantiatorFactory);
    InjectableStore store = new InjectableStore(instantiatorBindingMap, strategy.getProxyStrategy());
    InstanceInjectableFactory instanceInjectableFactory = new InstanceInjectableFactory(injectableFactory, strategy.getScopeStrategy().getSingletonAnnotationClass());

    this.registry = new InjectableStoreCandidateRegistry(store, discovererFactory, instanceInjectableFactory);
    this.instanceResolver = new DefaultInstanceResolver(new DefaultInstantiationContext(store, instantiatorBindingMap, strategy.getProxyStrategy()), instantiatorFactory);
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
  public <T> T getInstance(Type type, Object... qualifiers) throws UnsatisfiedResolutionException, AmbiguousResolutionException, CreationException, ScopeNotActiveException {
    return instanceResolver.getInstance(type, qualifiers);
  }

  @Override
  public <T> T getInstance(Class<T> cls, Object... qualifiers) throws UnsatisfiedResolutionException, AmbiguousResolutionException, CreationException, ScopeNotActiveException {
    return instanceResolver.getInstance(cls, qualifiers);
  }

  @Override
  public <T> List<T> getInstances(Type type, Object... qualifiers) throws CreationException {
    return instanceResolver.getInstances(type, qualifiers);
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
