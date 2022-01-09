package hs.ddif.core.store;

/**
 * Thrown when attempting to add a {@link QualifiedType} which already exists in the store.
 */
public class DuplicateQualifiedTypeException extends RuntimeException {

  /**
   * Constructs a new instance.
   *
   * @param qualifiedType a {@link QualifiedType}, cannot be null
   */
  public DuplicateQualifiedTypeException(QualifiedType qualifiedType) {
    super("Duplicate qualified type: " + qualifiedType);
  }

}
