package hs.ddif.core;

import hs.ddif.core.api.CandidateRegistry;
import hs.ddif.core.api.InstanceResolver;
import hs.ddif.core.api.NamedParameter;
import hs.ddif.core.inject.consistency.InjectorStoreConsistencyPolicy;
import hs.ddif.core.inject.instantiator.Gatherer;
import hs.ddif.core.inject.instantiator.Instantiator;
import hs.ddif.core.inject.instantiator.InstantiatorBasedInstanceResolver;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.inject.store.AutoDiscoveringGatherer;
import hs.ddif.core.inject.store.ClassInjectableFactory;
import hs.ddif.core.inject.store.FieldInjectableFactory;
import hs.ddif.core.inject.store.InjectableStoreCandidateRegistry;
import hs.ddif.core.inject.store.InstanceInjectableFactory;
import hs.ddif.core.inject.store.MethodInjectableFactory;
import hs.ddif.core.scope.ScopeResolver;
import hs.ddif.core.scope.SingletonScopeResolver;
import hs.ddif.core.scope.WeakSingletonScopeResolver;
import hs.ddif.core.store.InjectableStore;
import hs.ddif.core.util.AnnotationDescriptor;

import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Stream;

import javax.inject.Provider;

// TODO JSR-330: Named without value is treated differently ... some use field name, others default to empty?
/**
 * An injector provides instances of classes or interfaces which have been registered with
 * it or which can be auto discovered (if enabled). Each instance returned is injected with
 * further dependencies if any of its fields or constructors are annotated with {@link javax.inject.Inject}.<p>
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
 * The two most common scopes are {@link javax.inject.Singleton}, which returns the same instance each time and
 * unscoped, which returns a new instance each time. Custom scopes can be supported by this injector
 * by providing a {@link ScopeResolver} during construction. Note that instances registered directly
 * are always treated as singletons as the injector has no way of creating these itself. If an
 * instance is complicated to construct, consider registering a {@link Provider} or a class containing
 * {@link hs.ddif.annotations.Produces} annotated methods or fields.
 */
public class Injector implements InstanceResolver, CandidateRegistry {

  /**
   * Allows simple extensions to a {@link Injector}.
   */
  public interface Extension {

    /**
     * Gets zero or more {@link ResolvableInjectable}s derived from the given injectable.
     *
     * @param instantiator an {@link Instantiator}, never null
     * @param injectable a {@link ResolvableInjectable}, never null
     * @return a list of {@link ResolvableInjectable} derived from the given injectable, never null, but can be empty
     */
    List<ResolvableInjectable> getDerived(Instantiator instantiator, ResolvableInjectable injectable);
  }

  private final InstanceResolver instanceResolver;
  private final CandidateRegistry registry;
  private final ClassInjectableFactory classInjectableFactory = new ClassInjectableFactory(ResolvableInjectable::new);
  private final MethodInjectableFactory methodInjectableFactory = new MethodInjectableFactory(ResolvableInjectable::new);
  private final FieldInjectableFactory fieldInjectableFactory = new FieldInjectableFactory(ResolvableInjectable::new);
  private final InstanceInjectableFactory instanceInjectableFactory = new InstanceInjectableFactory(ResolvableInjectable::new);

  Injector(boolean autoDiscovery, ScopeResolver... scopeResolvers) {
    ScopeResolver[] standardScopeResolvers = new ScopeResolver[] {new SingletonScopeResolver(), new WeakSingletonScopeResolver()};
    ScopeResolver[] extendedScopeResolvers = Stream.of(scopeResolvers, standardScopeResolvers).flatMap(Stream::of).toArray(ScopeResolver[]::new);
    GathererExtensionAdapter adapter = new GathererExtensionAdapter(new ProducerInjectorExtension(classInjectableFactory));
    InjectableStore<ResolvableInjectable> store = new InjectableStore<>(new InjectorStoreConsistencyPolicy<>(extendedScopeResolvers));
    List<AutoDiscoveringGatherer.Extension> extensions = List.of(adapter, new ProviderGathererExtension(methodInjectableFactory), new ProducesGathererExtension(methodInjectableFactory, fieldInjectableFactory));

    Gatherer gatherer = new AutoDiscoveringGatherer(store, autoDiscovery, extensions, classInjectableFactory);

    Instantiator instantiator = new Instantiator(store, gatherer, autoDiscovery, extendedScopeResolvers);

    this.registry = new InjectableStoreCandidateRegistry(store, gatherer, classInjectableFactory, instanceInjectableFactory);
    this.instanceResolver = new InstantiatorBasedInstanceResolver(instantiator);

    adapter.setInstantiator(instantiator);  // TODO a bit cyclical... the extension needs instantiator, instantiator needs extensions...
  }

  private static class GathererExtensionAdapter implements AutoDiscoveringGatherer.Extension {
    private final Extension extension;

    private Instantiator instantiator;

    GathererExtensionAdapter(Extension extension) {
      this.extension = extension;
    }

    void setInstantiator(Instantiator instantiator) {
      this.instantiator = instantiator;
    }

    @Override
    public List<ResolvableInjectable> getDerived(ResolvableInjectable injectable) {
      return extension.getDerived(instantiator, injectable);
    }
  }

  /**
   * Returns an {@link InstanceResolver}, which can be shared instead of this class
   * to share only methods that can be used to instantiate objects.
   *
   * @return an {@link InstanceResolver}, never null
   */
  public InstanceResolver getInstanceResolver() {
    return instanceResolver;
  }

  /**
   * Returns a {@link CandidateRegistry}, which can be shared instead of this class
   * to share only methods that can be used to register and remove objects.
   *
   * @return a {@link CandidateRegistry}, never null
   */
  public CandidateRegistry getCandidateRegistry() {
    return registry;
  }

  @Override
  public <T> T getParameterizedInstance(Type type, NamedParameter[] parameters, Object... criteria) {
    return instanceResolver.getParameterizedInstance(type, parameters, criteria);
  }

  @Override
  public <T> T getInstance(Type type, Object... criteria) {
    return instanceResolver.getInstance(type, criteria);
  }

  @Override
  public <T> T getInstance(Class<T> cls, Object... criteria) {
    return instanceResolver.getInstance(cls, criteria);
  }

  @Override
  public <T> List<T> getInstances(Type type, Object... criteria) {
    return instanceResolver.getInstances(type, criteria);
  }

  @Override
  public <T> List<T> getInstances(Class<T> cls, Object... criteria) {
    return instanceResolver.getInstances(cls, criteria);
  }

  @Override
  public boolean contains(Type type, Object... criteria) {
    return registry.contains(type, criteria);
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
  public void registerInstance(Object instance, AnnotationDescriptor... qualifiers) {
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
