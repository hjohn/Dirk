package hs.ddif.core.instantiation;

import hs.ddif.core.instantiation.domain.InstanceCreationFailure;
import hs.ddif.core.instantiation.domain.MultipleInstances;
import hs.ddif.core.instantiation.domain.NoSuchInstance;
import hs.ddif.core.store.Key;

/**
 * Produces instances of type {@code T} using a given {@link Key}. The type produced
 * is not necessarily the same type as the key specifies but could be a wrapper that
 * requires injectables matching the key.
 *
 * <p>The instantiator is not only responsible for creating instances using injectables
 * matching a given key, but also defines how these instances can be created including
 * what restrictions must apply on the number of matching injectables.
 *
 * <p>For example, an instantiator decides whether the instance it produces is
 * allowed to be optional, in which case the {@link InstantiationContext} should resolve
 * the key associated with the instantiator to 0 or 1 injectables. In contrast, instantiators that
 * produce collections could allow any number of matching injectables.
 *
 * @param <T> the produced type
 */
public interface Instantiator<T> {

  /**
   * Returns the {@link Key} this instantiator requires to produce its instances. Depending on
   * the type of instance produced, the {@link InstantiationContext} must have the key
   * available at least, at most or exactly once, or can have it available more than once.
   *
   * @return a {@link Key}, never null
   */
  Key getKey();

  /**
   * Returns an instance of the type this instantiator produces.
   *
   * <p>Note: the type of the instance produced is not necessarily the same as the
   * key the instantiator requires, for example when a collection of instances
   * matching the key is returned.
   *
   * @param context an {@link InstantiationContext}, never null
   * @return an instance of the type this instantiator produces, can be {@code null}
   * @throws InstanceCreationFailure when the instance could not be created
   * @throws MultipleInstances when multiple instances could be created but the instantiator required at most one
   * @throws NoSuchInstance when no instance could be created but the instantiator required at least one
   */
  T getInstance(InstantiationContext context) throws InstanceCreationFailure, MultipleInstances, NoSuchInstance;

  /**
   * Indicates that the instance produced is a provider which will only resolve the
   * key upon use. This allows for an indirection that will allow avoiding a circular
   * dependency during the construction of instances.
   *
   * <p>Note that this does not allow for the key to be completely unavailable in
   * the underlying store unless this instantiator does not require at least one match.
   * In other words, a lazy dependency allows to break a circular dependency during
   * instantiation, but not during registration.
   *
   * @return {@code true} if the provided value is lazily resolved, otherwise {@code false}
   */
  default boolean isLazy() {
    return false;
  }

  /**
   * Indicates that this instantiator requires at least one dependency matching the key
   * to be available.
   *
   * @return {@code true} if at least one dependency matching the key must be available, otherwise {@code false}
   */
  boolean requiresAtLeastOne();

  /**
   * Indicates that this instantiator requires at most one dependency matching the key
   * to be available.
   *
   * @return {@code true} if at most one dependency matching the key must be available, otherwise {@code false}
   */
  boolean requiresAtMostOne();
}