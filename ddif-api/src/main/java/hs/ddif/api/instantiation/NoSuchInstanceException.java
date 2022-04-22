package hs.ddif.api.instantiation;

/**
 * Thrown when no matching instance was available or could be created.
 */
public class NoSuchInstanceException extends InstanceResolutionException {

  /**
   * Constructs a new instance.
   *
   * @param key a {@link Key}, cannot be {@code null}
   */
  public NoSuchInstanceException(Key key) {
    super("No such instance: [" + key + "]");
  }
}
