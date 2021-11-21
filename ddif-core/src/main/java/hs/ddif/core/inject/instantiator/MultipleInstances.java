package hs.ddif.core.inject.instantiator;

import hs.ddif.core.api.MultipleInstancesException;

import java.lang.reflect.Type;
import java.util.Set;

/**
 * Thrown when multiple matching instances were available.
 */
public class MultipleInstances extends InstanceResolutionFailure {

  /**
   * Constructs a new instance.
   *
   * @param type a {@link Type}, cannot be null
   * @param criteria an array of criteria
   * @param injectables a set of {@link hs.ddif.core.store.Injectable}s, cannot be null
   */
  public MultipleInstances(Type type, Object[] criteria, Set<?> injectables) {
    super("Multiple matching instances: " + type + toCriteriaString(criteria) + ": " + injectables);
  }

  @Override
  public MultipleInstancesException toRuntimeException() {
    return new MultipleInstancesException(getMessage(), getCause());
  }
}
