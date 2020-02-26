package hs.ddif.core.inject.store;

import hs.ddif.core.inject.consistency.UnresolvableDependencyException;
import hs.ddif.core.inject.consistency.ViolatesSingularDependencyException;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.store.InjectableStore;
import hs.ddif.core.util.AnnotationDescriptor;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Provider;

public class BeanDefinitionStore {

  /**
   * Allows simple extensions to a {@link BeanDefinitionStore}.
   */
  public interface Extension {

    /**
     * Gets another {@link ResolvableInjectable} derived from the given injectable, or
     * <code>null</code> if no other injectable could be derived.
     *
     * @param injectable a {@link ResolvableInjectable}, never null
     * @return another {@link ResolvableInjectable} derived from the given injectable, or
     *   <code>null</code> if no other injectable could be derived
     */
    ResolvableInjectable getDerived(ResolvableInjectable injectable);
  }

  private final InjectableStore<ResolvableInjectable> store;
  private final List<Extension> extensions;

  public BeanDefinitionStore(InjectableStore<ResolvableInjectable> store, List<Extension> extensions) {
    this.store = store;
    this.extensions = extensions;
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
   * Registers a class with this Injector if all its dependencies can be
   * resolved and it would not cause existing registered classes to have
   * ambigious dependencies as a result of registering the given class.<p>
   *
   * If there are unresolvable dependencies, or registering this class
   * would result in ambigious dependencies for previously registered
   * classes, then this method will throw an exception.<p>
   *
   * Note that if the given class implements {@link Provider} that
   * the class it provides is held to the same restrictions or registration
   * will fail.
   *
   * @param concreteClass the class to register with the Injector
   * @throws ViolatesSingularDependencyException when the registration would cause an ambigious dependency in one or more previously registered classes
   * @throws UnresolvableDependencyException when one or more dependencies of the given class cannot be resolved
   */
  public void register(Class<?> concreteClass) {
    register(new ClassInjectable(concreteClass));
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
    register(new InstanceInjectable(instance, qualifiers));
  }

  /**
   * Removes a class from this Injector if doing so would not result in
   * broken dependencies in the remaining registered classes.<p>
   *
   * If there would be broken dependencies then the removal will fail
   * and an exception is thrown.<p>
   *
   * Note that if the class implements {@link Provider} that the class it
   * provides is held to the same restrictions or removal will fail.
   *
   * @param concreteClass the class to remove from the Injector
   * @throws ViolatesSingularDependencyException when the removal would cause a missing dependency in one or more of the remaining registered classes
   */
  public void remove(Class<?> concreteClass) {
    remove(new ClassInjectable(concreteClass));
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
    remove(new InstanceInjectable(instance));
  }

  private void register(ResolvableInjectable injectable) {
    registerSingle(injectable);

    List<ResolvableInjectable> registered = new ArrayList<>();

    registered.add(injectable);

    for(Extension extension : extensions) {
      try {
        ResolvableInjectable derived = extension.getDerived(injectable);

        if(derived != null) {
          register(derived);
          registered.add(derived);
        }
      }
      catch(Exception e) {
        for(int i = registered.size() - 1; i >= 0; i--) {
          removeSingle(registered.get(i));
        }

        throw e;
      }
    }
  }

  private void remove(ResolvableInjectable injectable) {
    removeSingle(injectable);

    List<ResolvableInjectable> removed = new ArrayList<>();

    removed.add(injectable);

    for(Extension extension : extensions) {
      try {
        ResolvableInjectable derived = extension.getDerived(injectable);

        if(derived != null) {
          remove(derived);
          removed.add(derived);
        }
      }
      catch(Exception e) {
        for(int i = removed.size() - 1; i >= 0; i--) {
          registerSingle(removed.get(i));
        }

        throw e;
      }
    }
  }

  private void registerSingle(ResolvableInjectable injectable) {
    store.put(injectable);
  }

  private void removeSingle(ResolvableInjectable injectable) {
    store.remove(injectable);
  }
}
