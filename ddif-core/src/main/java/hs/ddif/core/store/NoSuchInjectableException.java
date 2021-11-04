package hs.ddif.core.store;

/**
 * Thrown when an {@link Injectable} could not be found.
 */
public class NoSuchInjectableException extends RuntimeException {

  /**
   * Constructs a new instance.
   *
   * @param injectable an {@link Injectable}, cannot be null
   */
  public NoSuchInjectableException(Injectable injectable) {
    super(injectable + " not found");
  }

}
