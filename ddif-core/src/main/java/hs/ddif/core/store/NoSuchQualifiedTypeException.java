package hs.ddif.core.store;

/**
 * Thrown when a {@link QualifiedType} could not be found.
 */
public class NoSuchQualifiedTypeException extends RuntimeException {

  /**
   * Constructs a new instance.
   *
   * @param qualifiedType a {@link QualifiedType}, cannot be null
   */
  public NoSuchQualifiedTypeException(QualifiedType qualifiedType) {
    super("No such qualified type: " + qualifiedType);
  }

}
