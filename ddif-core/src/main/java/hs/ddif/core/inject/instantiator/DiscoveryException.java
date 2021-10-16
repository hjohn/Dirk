package hs.ddif.core.inject.instantiator;

/**
 * Thrown when a {@link InjectableDiscoverer} was unable to do a complete discovery.
 */
public class DiscoveryException extends Exception {

  /**
   * Creates a new instance.
   *
   * @param message a message describing the problem
   */
  public DiscoveryException(String message) {
    super(message);
  }

}
