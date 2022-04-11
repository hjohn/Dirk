package hs.ddif.api.instantiation;

import hs.ddif.api.instantiation.domain.InstanceCreationFailure;
import hs.ddif.api.instantiation.domain.Key;
import hs.ddif.api.instantiation.domain.MultipleInstances;

import java.util.List;

/**
 * Context used to create instances for given {@link Key}s.
 */
public interface InstantiationContext {

  /**
   * Creates the instance associated with the given {@link Key}. If the key given
   * matches multiple instances a {@link MultipleInstances} exception is thrown.
   * Returns {@code null} if nothing matched.
   *
   * @param <T> the type of the instance
   * @param key a {@link Key}, cannot be {@code null}
   * @return an instance or {@code null} if there were no matches
   * @throws InstanceCreationFailure when the creation of the instance failed
   * @throws MultipleInstances when the key matched multiple potential instances
   */
  <T> T create(Key key) throws InstanceCreationFailure, MultipleInstances;

  /**
   * Creates all instances for all known types associated with the given {@link Key}.
   * Scoped types which scope is currently inactive are excluded.
   *
   * @param <T> the type of the instances
   * @param key a {@link Key}, cannot be {@code null}
   * @return a list of instances, never {@code null} and never contains {@code null}
   * @throws InstanceCreationFailure when the creation of an instance failed
   */
  <T> List<T> createAll(Key key) throws InstanceCreationFailure;

}

