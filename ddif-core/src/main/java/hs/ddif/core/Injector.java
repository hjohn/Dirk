package hs.ddif.core;

import hs.ddif.core.bind.NamedParameter;
import hs.ddif.core.inject.consistency.InjectorStoreConsistencyPolicy;
import hs.ddif.core.inject.consistency.UnresolvableDependencyException;
import hs.ddif.core.inject.consistency.ViolatesSingularDependencyException;
import hs.ddif.core.inject.instantiator.BeanResolutionException;
import hs.ddif.core.inject.instantiator.Instantiator;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.inject.store.BeanDefinitionStore;
import hs.ddif.core.inject.store.ResolvableInjectableStore;
import hs.ddif.core.scope.ScopeResolver;
import hs.ddif.core.store.InjectableStore;
import hs.ddif.core.store.Resolver;
import hs.ddif.core.util.AnnotationDescriptor;

import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Provider;

// TODO JSR-330: Named without value is treated differently ... some use field name, others default to empty?
public class Injector {

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
  private final BeanDefinitionStore store;

  public Injector(boolean autoDiscovery, ScopeResolver... scopeResolvers) {
    StoreExtensionAdapter adapter = new StoreExtensionAdapter(new ProducerInjectorExtension());

    InjectableStore<ResolvableInjectable> store = new ResolvableInjectableStore(
      new InjectorStoreConsistencyPolicy<>(),
      List.of(adapter, new ProviderStoreExtension(), new ProducesStoreExtension()),
      autoDiscovery
    );

    this.store = new BeanDefinitionStore(store);
    this.instantiator = new Instantiator(store, scopeResolvers);

    adapter.setInstantiator(instantiator);  // TODO a bit cyclical... the extension needs store, store needs extensions...
  }

  public Injector(ScopeResolver... scopeResolvers) {
    this(false, scopeResolvers);
  }

  public Injector() {
    this(false);
  }

  private static class StoreExtensionAdapter implements ResolvableInjectableStore.Extension {
    private final Extension extension;

    private Instantiator instantiator;

    StoreExtensionAdapter(Extension extension) {
      this.extension = extension;
    }

    void setInstantiator(Instantiator instantiator) {
      this.instantiator = instantiator;
    }

    @Override
    public List<Supplier<ResolvableInjectable>> getDerived(Resolver<ResolvableInjectable> resolver, ResolvableInjectable injectable) {
      return extension.getDerived(instantiator, injectable).stream().map(ri -> (Supplier<ResolvableInjectable>)(() -> ri)).collect(Collectors.toList());
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
   * Returns a {@link BeanDefinitionStore}, which can be shared instead of this class
   * to share only methods that can be used to register and remove objects.
   *
   * @return a {@link BeanDefinitionStore}, never null
   */
  public BeanDefinitionStore getStore() {
    return store;
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

  /**
   * Returns <code>true</code> when the given class is part of this Injector, otherwise
   * <code>false</code>.
   *
   * @param cls a class to check for, cannot be null
   * @return <code>true</code> when the given class is part of this Injector, otherwise <code>false</code>
   */
  public boolean contains(Class<?> cls) {
    return store.contains(cls);
  }

  /**
   * Returns <code>true</code> when the given type with the given criteria is part of this
   * Injector, otherwise <code>false</code>.
   *
   * @param type a type to check for, cannot be null
   * @param criteria optional list of criteria, see {@link hs.ddif.core.store.InjectableStore#resolve(Type, Object...)}
   * @return <code>true</code> when the given type with the given criteria is part of this Injector, otherwise <code>false</code>
   */
  public boolean contains(Type type, Object... criteria) {
    return store.contains(type, criteria);
  }

  /**
   * Registers a {@link Type} with this Injector if all its dependencies can be
   * resolved and it would not cause existing registered classes to have
   * ambigious dependencies as a result of registering the given class.<p>
   *
   * If there are unresolvable dependencies, or registering this type
   * would result in ambigious dependencies for previously registered
   * classes, then this method will throw an exception.<p>
   *
   * Note that if the given class implements {@link Provider} that
   * the class it provides is held to the same restrictions or registration
   * will fail.
   *
   * @param concreteType the type to register with the Injector
   * @throws ViolatesSingularDependencyException when the registration would cause an ambigious dependency in one or more previously registered classes
   * @throws UnresolvableDependencyException when one or more dependencies of the given class cannot be resolved
   */
  public void register(Type concreteType) {
    store.register(concreteType);
  }

  /**
   * Registers an instance with this Injector as a Singleton if it would not
   * cause existing registered classes to have ambigious dependencies as a
   * result.<p>
   *
   * If registering this instance would result in ambigious dependencies for
   * previously registered classes, then this method will throw an exception.<p>
   *
   * Note that if the instance implements {@link Provider} that the class it
   * provides is held to the same restrictions or registration will fail.
   *
   * @param instance the instance to register with the Injector
   * @param qualifiers the qualifiers for this provider
   * @throws ViolatesSingularDependencyException when the registration would cause an ambigious dependency in one or more previously registered classes
   */
  public void registerInstance(Object instance, AnnotationDescriptor... qualifiers) {
    store.registerInstance(instance, qualifiers);
  }

  /**
   * Removes a {@link Type} from this Injector if doing so would not result in
   * broken dependencies in the remaining registered classes.<p>
   *
   * If there would be broken dependencies then the removal will fail
   * and an exception is thrown.<p>
   *
   * Note that if the type implements {@link Provider} that the class it
   * provides is held to the same restrictions or removal will fail.
   *
   * @param concreteType the type to remove from the Injector
   * @throws ViolatesSingularDependencyException when the removal would cause a missing dependency in one or more of the remaining registered classes
   */
  public void remove(Type concreteType) {
    store.remove(concreteType);
  }

  /**
   * Removes an instance from this Injector if doing so would not result in
   * broken dependencies in the remaining registered classes.<p>
   *
   * If there would be broken dependencies then the removal will fail
   * and an exception is thrown.<p>
   *
   * Note that if the instance implements {@link Provider} that the class it
   * provides is held to the same restrictions or removal will fail.
   *
   * @param instance the instance to remove from the Injector
   * @throws ViolatesSingularDependencyException when the removal would cause a missing dependency in one or more of the remaining registered classes
   */
  public void removeInstance(Object instance) {
    store.removeInstance(instance);
  }
}
