package hs.ddif.core.inject.instantiator;

/**
 * Thrown when auto discovery fails.
 */
public class DiscoveryException extends Exception {

  /**
   * Creates a new instance.
   *
   * @param message a message describing the problem
   * @param cause an optional cause, can be null
   */
  public DiscoveryException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates a new instance.
   *
   * @param message a message describing the problem
   */
  public DiscoveryException(String message) {
    super(message);
  }
}
