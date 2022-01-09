package hs.ddif.core.inject.instantiation;

import hs.ddif.core.api.MultipleInstancesException;
import hs.ddif.core.store.Key;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Thrown when multiple matching instances were available.
 */
public class MultipleInstances extends InstanceResolutionFailure {

  /**
   * Constructs a new instance.
   *
   * @param key a {@link Key}, cannot be null
   * @param matchers a list of {@link Predicate}s, cannot be null
   * @param injectables a set of {@link hs.ddif.core.store.Injectable}s, cannot be null
   */
  public MultipleInstances(Key key, List<Predicate<Type>> matchers, Set<?> injectables) {
    super("Multiple matching instances: " + key + toCriteriaString(matchers) + ": " + injectables);
  }

  @Override
  public MultipleInstancesException toRuntimeException() {
    return new MultipleInstancesException(getMessage(), getCause());
  }
}
