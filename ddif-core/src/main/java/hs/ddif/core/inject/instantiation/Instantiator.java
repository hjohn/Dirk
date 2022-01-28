package hs.ddif.core.inject.instantiation;

import hs.ddif.core.scope.OutOfScopeException;
import hs.ddif.core.store.Key;

import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Predicate;

/**
 * Supplies fully injected classes.
 */
public interface Instantiator {

  /**
   * Returns an instance matching the given {@link Key} in which all dependencies are injected.
   *
   * @param <T> the type of the instance
   * @param key a {@link Key} identifying the type of the instance required, cannot be {@code null}
   * @return an instance matching the given {@link Key}, never {@code null}
   * @throws NoSuchInstance when no matching instance could be found or created
   * @throws OutOfScopeException when out of scope
   * @throws MultipleInstances when multiple matching instances were found or could be created
   * @throws InstanceCreationFailure when instantiation of an instance failed
   */
  <T> T getInstance(Key key) throws OutOfScopeException, NoSuchInstance, MultipleInstances, InstanceCreationFailure;

  /**
   * Finds an instance matching the given {@link Key} in which all dependencies are
   * injected. If not found, {@code null} is returned.
   *
   * @param <T> the type of the instance
   * @param key a {@link Key} identifying the type of the instance required, cannot be {@code null}
   * @return an instance matching the given {@link Key}, or {@code null} when no instance was found
   * @throws OutOfScopeException when out of scope
   * @throws MultipleInstances when multiple matching instances were found or could be created
   * @throws InstanceCreationFailure when instantiation of an instance failed
   */
  <T> T findInstance(Key key) throws OutOfScopeException, MultipleInstances, InstanceCreationFailure;

  /**
   * Returns all instances matching the given {@link Key} and {@link Predicate} and, if scoped,
   * which are active in the current scope.  When there are no matches, an empty set is returned.
   *
   * @param <T> the type of the instance
   * @param key a {@link Key} identifying the type of the instance required, cannot be {@code null}
   * @param typePredicate a {@link Predicate}, can be {@code null} in which case no further filtering occurs
   * @return all instances of the given class matching the given matchers (if any)
   * @throws InstanceCreationFailure when instantiation of an instance failed
   */
  <T> List<T> getInstances(Key key, Predicate<Type> typePredicate) throws InstanceCreationFailure;

  /**
   * Returns all instances matching the given {@link Key} and, if scoped, which are
   * active in the current scope.  When there are no matches, an empty set is returned.
   *
   * @param <T> the type of the instance
   * @param key a {@link Key} identifying the type of the instance required, cannot be {@code null}
   * @return all instances of the given class matching the given matchers (if any)
   * @throws InstanceCreationFailure when instantiation of an instance failed
   */
  <T> List<T> getInstances(Key key) throws InstanceCreationFailure;
}
