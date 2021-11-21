package hs.ddif.core;

import hs.ddif.core.api.CandidateRegistry;
import hs.ddif.core.api.NamedParameter;
import hs.ddif.core.inject.consistency.InjectorStoreConsistencyPolicy;
import hs.ddif.core.inject.consistency.UnresolvableDependencyException;
import hs.ddif.core.inject.consistency.ViolatesSingularDependencyException;
import hs.ddif.core.inject.instantiator.BeanResolutionException;
import hs.ddif.core.inject.instantiator.Gatherer;
import hs.ddif.core.inject.instantiator.Instantiator;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.inject.store.AutoDiscoveringGatherer;
import hs.ddif.core.inject.store.InjectableStoreCandidateRegistry;
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
public class Injector implements CandidateRegistry {

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

  private final Instantiator instantiator;
  private final CandidateRegistry registry;

  public Injector(boolean autoDiscovery, ScopeResolver... scopeResolvers) {
    ScopeResolver[] standardScopeResolvers = new ScopeResolver[] {new SingletonScopeResolver(), new WeakSingletonScopeResolver()};
    ScopeResolver[] extendedScopeResolvers = Stream.of(scopeResolvers, standardScopeResolvers).flatMap(Stream::of).toArray(ScopeResolver[]::new);
    GathererExtensionAdapter adapter = new GathererExtensionAdapter(new ProducerInjectorExtension());
    InjectableStore<ResolvableInjectable> store = new InjectableStore<>(new InjectorStoreConsistencyPolicy<>(extendedScopeResolvers));

    Gatherer gatherer = new AutoDiscoveringGatherer(store, autoDiscovery, List.of(adapter, new ProviderGathererExtension(), new ProducesGathererExtension()));

    this.registry = new InjectableStoreCandidateRegistry(store, gatherer);
    this.instantiator = new Instantiator(
      store,
      gatherer,
      autoDiscovery,
      extendedScopeResolvers
    );

    adapter.setInstantiator(instantiator);  // TODO a bit cyclical... the extension needs instantiator, instantiator needs extensions...
  }

  public Injector(ScopeResolver... scopeResolvers) {
    this(false, scopeResolvers);
  }

  public Injector() {
    this(false);
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
   * Returns an {@link Instantiator}, which can be shared instead of this class
   * to share only methods that can be used to instantiate objects.
   *
   * @return an {@link Instantiator}, never null
   */
  public Instantiator getInstantiator() {
    return instantiator;
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

  /**
   * Returns an instance of the given type matching the given criteria (if any) in
   * which all dependencies and parameters are injected.
   *
   * @param <T> the type of the instance
   * @param type the type of the instance required
   * @param parameters an array of {@link NamedParameter}'s required for creating the given type, cannot be null
   * @param criteria optional list of criteria, see {@link InjectableStore#resolve(Type, Object...)}
   * @return an instance of the given class matching the given criteria, never null
   * @throws BeanResolutionException when the given class is not registered with this Injector or the bean cannot be provided
   *   or when the given class has multiple matching candidates
   */
  public <T> T getParameterizedInstance(Type type, NamedParameter[] parameters, Object... criteria) throws BeanResolutionException {
    return instantiator.getParameterizedInstance(type, parameters, criteria);
  }

  /**
   * Returns an instance of the given type matching the given criteria (if any) in
   * which all dependencies are injected.
   *
   * @param <T> the type of the instance
   * @param type the type of the instance required
   * @param criteria optional list of criteria, see {@link InjectableStore#resolve(Type, Object...)}
   * @return an instance of the given class matching the given criteria, never null
   * @throws BeanResolutionException when the given class is not registered with this Injector or the bean cannot be provided
   *   or when the given class has multiple matching candidates
   */
  public <T> T getInstance(Type type, Object... criteria) throws BeanResolutionException {
    return instantiator.getInstance(type, criteria);
  }

  /**
   * Returns an instance of the given class matching the given criteria (if any) in
   * which all dependencies are injected.
   *
   * @param <T> the type of the instance
   * @param cls the class of the instance required
   * @param criteria optional list of criteria, see {@link InjectableStore#resolve(Type, Object...)}
   * @return an instance of the given class matching the given criteria (if any)
   * @throws BeanResolutionException when the given class is not registered with this Injector or the bean cannot be provided
   *   or when the given class has multiple matching candidates
   */
  public <T> T getInstance(Class<T> cls, Object... criteria) throws BeanResolutionException {  // The signature of this method closely matches the other getInstance method as Class implements Type, however, this method will auto-cast the result thanks to the type parameter
    return instantiator.getInstance(cls, criteria);
  }

  /**
   * Returns all instances of the given type matching the given criteria (if any) in
   * which all dependencies are injected.  When there are no matches, an empty set is
   * returned.
   *
   * @param <T> the type of the instance
   * @param type the type of the instances required
   * @param criteria optional list of criteria, see {@link InjectableStore#resolve(Type, Object...)}
   * @return all instances of the given class matching the given criteria (if any)
   * @throws BeanResolutionException when a required bean could not be found
   */
  public <T> List<T> getInstances(Type type, Object... criteria) throws BeanResolutionException {
    return instantiator.getInstances(type, criteria);
  }

  /**
   * Returns all instances of the given class matching the given criteria (if any) in
   * which all dependencies are injected.  When there are no matches, an empty set is
   * returned.
   *
   * @param <T> the type of the instances
   * @param cls the class of the instances required
   * @param criteria optional list of criteria, see {@link InjectableStore#resolve(Type, Object...)}
   * @return all instances of the given class matching the given criteria (if any)
   * @throws BeanResolutionException when a required bean could not be found
   */
  public <T> List<T> getInstances(Class<T> cls, Object... criteria) throws BeanResolutionException {
    return instantiator.getInstances(cls, criteria);
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
