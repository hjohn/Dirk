package hs.ddif.core.inject.store;

import hs.ddif.core.inject.consistency.UnresolvableDependencyException;
import hs.ddif.core.inject.consistency.ViolatesSingularDependencyException;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.store.InjectableStore;
import hs.ddif.core.util.AnnotationDescriptor;

import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Provider;

public class BeanDefinitionStore {
  private final InjectableStore<ResolvableInjectable> store;

  public BeanDefinitionStore(InjectableStore<ResolvableInjectable> store) {
    this.store = store;
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
   * @param criteria optional list of criteria, see {@link InjectableStore#resolve(Type, Object...)}
   * @return <code>true</code> when the given type with the given criteria is part of this Injector, otherwise <code>false</code>
   */
  public boolean contains(Type type, Object... criteria) {
    return store.contains(type, criteria);
  }

  /**
   * Registers a type if all its dependencies can be
   * resolved and it would not cause existing registered classes to have
   * ambigious dependencies as a result of registering the given type.<p>
   *
   * If there are unresolvable dependencies, or registering this class
   * would result in ambigious dependencies for previously registered
   * classes, then this method will throw an exception.<p>
   *
   * Note that if the given class implements {@link Provider} that
   * the class it provides is held to the same restrictions or registration
   * will fail.
   *
   * @param concreteType the type to register with the Injector; the type cannot have unresolved type variables
   * @throws ViolatesSingularDependencyException when the registration would cause an ambigious dependency in one or more previously registered classes
   * @throws UnresolvableDependencyException when one or more dependencies of the given class cannot be resolved
   */
  public void register(Type concreteType) {
    registerInternal(List.of(new ClassInjectable(concreteType)));
  }

  /**
   * Registers the given types.
   *
   * @param concreteTypes a list of types to register, cannot be null or contain nulls
   * @see #register(Type)
   */
  public void register(List<Type> concreteTypes) {
    registerInternal(concreteTypes.stream().map(ClassInjectable::new).collect(Collectors.toList()));
  }

  /**
   * Registers an instance as a Singleton if it would not
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
    registerInternal(List.of(new InstanceInjectable(instance, qualifiers)));
  }

  /**
   * Removes a type if doing so would not result in
   * broken dependencies in the remaining registered types.<p>
   *
   * If there would be broken dependencies then the removal will fail
   * and an exception is thrown.<p>
   *
   * Note that if the class implements {@link Provider} that the class it
   * provides is held to the same restrictions or removal will fail.
   *
   * @param concreteType the type to remove from the Injector; the type cannot have unresolved type variables
   * @throws ViolatesSingularDependencyException when the removal would cause a missing dependency in one or more of the remaining registered classes
   */
  public void remove(Type concreteType) {
    removeInternal(List.of(new ClassInjectable(concreteType)));
  }

  /**
   * Removes the given types.
   *
   * @param concreteTypes a list of types to remove, cannot be null or contain nulls
   * @see #remove(Type)
   */
  public void remove(List<Type> concreteTypes) {
    removeInternal(concreteTypes.stream().map(ClassInjectable::new).collect(Collectors.toList()));
  }

  /**
   * Removes an instance if doing so would not result in
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
    removeInternal(List.of(new InstanceInjectable(instance)));
  }

  private void registerInternal(List<ResolvableInjectable> injectables) {
    store.putAll(injectables);
  }

  private void removeInternal(List<ResolvableInjectable> injectables) {
    store.removeAll(injectables);
  }
}
