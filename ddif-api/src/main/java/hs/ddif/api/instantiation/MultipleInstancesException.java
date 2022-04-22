package hs.ddif.api.instantiation;

import java.util.Collection;

/**
 * Thrown when multiple matching instances were available.
 */
public class MultipleInstancesException extends InstanceResolutionException {

  /**
   * Constructs a new instance.
   *
   * @param key a {@link Key}, cannot be {@code null}
   * @param injectables a set of injectables, cannot be {@code null}
   */
  public MultipleInstancesException(Key key, Collection<?> injectables) {
    super("Multiple matching instances: [" + key + "]: " + injectables);
  }
}
