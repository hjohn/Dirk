package hs.ddif.core.inject.instantiator;

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
   * Returns an instance matching the given {@link Key} and a list of {@link Predicate}s (if any) in
   * which all dependencies are injected.
   *
   * @param <T> the type of the instance
   * @param key a {@link Key} identifying the type of the instance required, cannot be null
   * @param matchers a list of {@link Predicate}s, cannot be null
   * @return an instance of the given class matching the given matchers, never null
   * @throws NoSuchInstance when no matching instance could be found or created
   * @throws OutOfScopeException when out of scope
   * @throws MultipleInstances when multiple matching instances were found or could be created
   * @throws InstanceCreationFailure when instantiation of an instance failed
   */
  <T> T getInstance(Key key, List<Predicate<Type>> matchers) throws OutOfScopeException, NoSuchInstance, MultipleInstances, InstanceCreationFailure;

  /**
   * Returns an instance matching the given {@link Key} (if any) in
   * which all dependencies are injected.
   *
   * @param <T> the type of the instance
   * @param key a {@link Key} identifying the type of the instance required, cannot be null
   * @return an instance matching the given {@link Key}, never null
   * @throws NoSuchInstance when no matching instance could be found or created
   * @throws OutOfScopeException when out of scope
   * @throws MultipleInstances when multiple matching instances were found or could be created
   * @throws InstanceCreationFailure when instantiation of an instance failed
   */
  <T> T getInstance(Key key) throws OutOfScopeException, NoSuchInstance, MultipleInstances, InstanceCreationFailure;

  /**
   * Finds an instance matching the given {@link Key} and a list of {@link Predicate}s (if any) in
   * which all dependencies are injected. If not found, {@code null}
   * is returned.
   *
   * @param <T> the type of the instance
   * @param key a {@link Key} identifying the type of the instance required, cannot be null
   * @param matchers a list of {@link Predicate}s, cannot be null
   * @return an instance of the given class matching the given matchers, or {@code null} when no instance was found
   * @throws OutOfScopeException when out of scope
   * @throws MultipleInstances when multiple matching instances were found or could be created
   * @throws InstanceCreationFailure when instantiation of an instance failed
   */
  <T> T findInstance(Key key, List<Predicate<Type>> matchers) throws OutOfScopeException, MultipleInstances, InstanceCreationFailure;

  /**
   * Finds an instance matching the given {@link Key} (if any) in
   * which all dependencies are injected. If not found, {@code null}
   * is returned.
   *
   * @param <T> the type of the instance
   * @param key a {@link Key} identifying the type of the instance required, cannot be null
   * @return an instance matching the given {@link Key}, or {@code null} when no instance was found
   * @throws OutOfScopeException when out of scope
   * @throws MultipleInstances when multiple matching instances were found or could be created
   * @throws InstanceCreationFailure when instantiation of an instance failed
   */
  <T> T findInstance(Key key) throws OutOfScopeException, MultipleInstances, InstanceCreationFailure;

  /**
   * Returns all instances matching the given {@link Key} and a list of {@link Predicate}s (if any) and, if scoped,
   * which are active in the current scope.  When there are no matches, an empty set is returned.
   *
   * @param <T> the type of the instance
   * @param key a {@link Key} identifying the type of the instance required, cannot be null
   * @param matchers a list of {@link Predicate}s, cannot be null
   * @return all instances of the given class matching the given matchers (if any)
   * @throws InstanceCreationFailure when instantiation of an instance failed
   */
  <T> List<T> getInstances(Key key, List<Predicate<Type>> matchers) throws InstanceCreationFailure;

  /**
   * Returns all instances matching the given {@link Key} (if any) and, if scoped,
   * which are active in the current scope.  When there are no matches, an empty set is returned.
   *
   * @param <T> the type of the instance
   * @param key a {@link Key} identifying the type of the instance required, cannot be null
   * @return all instances of the given class matching the given matchers (if any)
   * @throws InstanceCreationFailure when instantiation of an instance failed
   */
  <T> List<T> getInstances(Key key) throws InstanceCreationFailure;
}
