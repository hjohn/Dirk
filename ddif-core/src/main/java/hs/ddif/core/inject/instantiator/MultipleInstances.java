package hs.ddif.core.inject.instantiator;

import hs.ddif.core.api.MultipleInstancesException;
import hs.ddif.core.store.Criteria;
import hs.ddif.core.store.Key;

import java.util.Set;

/**
 * Thrown when multiple matching instances were available.
 */
public class MultipleInstances extends InstanceResolutionFailure {

  /**
   * Constructs a new instance.
   *
   * @param key a {@link Key}, cannot be null
   * @param criteria an array of criteria
   * @param injectables a set of {@link hs.ddif.core.store.Injectable}s, cannot be null
   */
  public MultipleInstances(Key key, Criteria criteria, Set<?> injectables) {
    super("Multiple matching instances: " + key + toCriteriaString(criteria) + ": " + injectables);
  }

  @Override
  public MultipleInstancesException toRuntimeException() {
    return new MultipleInstancesException(getMessage(), getCause());
  }
}
