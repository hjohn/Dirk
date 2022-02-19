package hs.ddif.core.instantiation.domain;

import hs.ddif.core.api.MultipleInstancesException;
import hs.ddif.core.store.Key;

import java.util.Collection;

/**
 * Thrown when multiple matching instances were available.
 */
public class MultipleInstances extends InstanceResolutionFailure {

  /**
   * Constructs a new instance.
   *
   * @param key a {@link Key}, cannot be {@code null}
   * @param injectables a set of injectables, cannot be {@code null}
   */
  public MultipleInstances(Key key, Collection<?> injectables) {
    super("Multiple matching instances: [" + key + "]: " + injectables);
  }

  @Override
  public MultipleInstancesException toRuntimeException() {
    return new MultipleInstancesException(getMessage(), this);
  }
}
