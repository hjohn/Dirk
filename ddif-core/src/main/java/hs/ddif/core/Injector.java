package hs.ddif.core;

import hs.ddif.core.api.CandidateRegistry;
import hs.ddif.core.api.InstanceResolver;
import hs.ddif.core.config.discovery.DiscovererFactory;
import hs.ddif.core.config.standard.DefaultInstanceResolver;
import hs.ddif.core.config.standard.DefaultInstantiationContext;
import hs.ddif.core.config.standard.InjectableStoreCandidateRegistry;
import hs.ddif.core.definition.InstanceInjectableFactory;
import hs.ddif.core.inject.store.InjectableStore;
import hs.ddif.core.inject.store.InstantiatorBindingMap;
import hs.ddif.core.instantiation.DefaultInstantiatorFactory;
import hs.ddif.core.instantiation.InstantiationContext;
import hs.ddif.core.instantiation.InstantiatorFactory;
import hs.ddif.core.instantiation.TypeExtensionStore;
import hs.ddif.core.scope.ScopeResolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * An injector provides instances of classes or interfaces which have been registered with
 * it or which can be auto discovered (if enabled). Each instance returned is injected with
 * further dependencies if any of its fields or constructors are annotated with an inject annotation.<p>
 *
 * The potential candidates for injection can be registered with the injector in various ways,
 * for example by providing classes or existing instances. These candidates are called "injectables".
 * In order to successfully register an injectable, all its dependencies must be met as well, and all
 * dependencies it provides must not conflict with any injectables already registered. If a conflict
 * or cycle is detected registering will throw an exception.<p>
 *
 * For example, consider an injector for which a class A and B are registered and where class A has a
 * field that requires an instance of class B. Any of the following actions will throw an exception:
 *
 * <ul>
 * <li>Removing class B; this would make construction of A impossible as B is a requirement.</li>
 * <li>Registering a subclass of B; this would make construction of A ambiguous as there are two
 *     possible injectables for the required B.</li>
 * <li>Registering a class which either provides or produces instances of B (or a subclass); again
 *     this would make construction of A ambiguous.</li>
 * </ul>
 *
 * <h2>Scoping</h2>
 *
 * An injector may return existing instances or new instances depending on the scope of the injectable.
 * The two most common scopes are singleton, which returns the same instance each time and
 * unscoped, which returns a new instance each time. Custom scopes can be supported by this injector
 * by providing a {@link ScopeResolver} during construction. Note that instances registered directly
 * are always treated as singletons as the injector has no way of creating these itself. If an
 * instance is complicated to construct, consider registering a provider or a class containing
 * a producer annotated method or field.
 */
public class Injector implements InstanceResolver, CandidateRegistry {
  private final InstanceResolver instanceResolver;
  private final CandidateRegistry registry;

  /**
   * Constructs a new instance.
   *
   * @param typeExtensionStore a {@link TypeExtensionStore}, cannot be {@code null}
   * @param instanceInjectableFactory a {@link InstanceInjectableFactory}, cannot be {@code null}
   * @param discovererFactory a {@link DiscovererFactory}, cannot be {@code null}
   */
  public Injector(TypeExtensionStore typeExtensionStore, DiscovererFactory discovererFactory, InstanceInjectableFactory instanceInjectableFactory) {
    InstantiatorFactory instantiatorFactory = new DefaultInstantiatorFactory(typeExtensionStore);
    InstantiatorBindingMap instantiatorBindingMap = new InstantiatorBindingMap(Objects.requireNonNull(instantiatorFactory, "instantiatorFactory cannot be null"));
    InjectableStore store = new InjectableStore(instantiatorBindingMap);
    InstantiationContext instantiationContext = new DefaultInstantiationContext(store, instantiatorBindingMap);

    this.registry = new InjectableStoreCandidateRegistry(store, discovererFactory, instanceInjectableFactory);
    this.instanceResolver = new DefaultInstanceResolver(store, discovererFactory, instantiationContext, instantiatorFactory);
  }

  /**
   * Returns an {@link InstanceResolver}, which can be shared instead of this class
   * to share only methods that can be used to instantiate objects.
   *
   * @return an {@link InstanceResolver}, never {@code null}
   */
  public InstanceResolver getInstanceResolver() {
    return instanceResolver;
  }

  /**
   * Returns a {@link CandidateRegistry}, which can be shared instead of this class
   * to share only methods that can be used to register and remove objects.
   *
   * @return a {@link CandidateRegistry}, never {@code null}
   */
  public CandidateRegistry getCandidateRegistry() {
    return registry;
  }

  @Override
  public <T> T getInstance(Type type, Object... qualifiers) {
    return instanceResolver.getInstance(type, qualifiers);
  }

  @Override
  public <T> T getInstance(Class<T> cls, Object... qualifiers) {
    return instanceResolver.getInstance(cls, qualifiers);
  }

  @Override
  public <T> List<T> getInstances(Type type, Predicate<Type> typePredicate, Object... qualifiers) {
    return instanceResolver.getInstances(type, typePredicate, qualifiers);
  }

  @Override
  public <T> List<T> getInstances(Class<T> cls, Predicate<Type> typePredicate, Object... qualifiers) {
    return instanceResolver.getInstances(cls, typePredicate, qualifiers);
  }

  @Override
  public <T> List<T> getInstances(Type type, Object... qualifiers) {
    return instanceResolver.getInstances(type, qualifiers);
  }

  @Override
  public <T> List<T> getInstances(Class<T> cls, Object... qualifiers) {
    return instanceResolver.getInstances(cls, qualifiers);
  }

  @Override
  public boolean contains(Type type, Object... qualifiers) {
    return registry.contains(type, qualifiers);
  }

  @Override
  public void register(Type concreteType) {
    registry.register(concreteType);
  }

  @Override
  public void register(List<Type> concreteTypes) {
    registry.register(concreteTypes);
  }

  @Override
  public void registerInstance(Object instance, Annotation... qualifiers) {
    registry.registerInstance(instance, qualifiers);
  }

  @Override
  public void remove(Type concreteType) {
    registry.remove(concreteType);
  }

  @Override
  public void remove(List<Type> concreteTypes) {
    registry.remove(concreteTypes);
  }

  @Override
  public void removeInstance(Object instance) {
    registry.removeInstance(instance);
  }
}
