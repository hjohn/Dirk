package hs.ddif.core.inject.instantiation;

import hs.ddif.core.api.InstanceCreationException;
import hs.ddif.core.store.Key;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Type;

import javax.annotation.PostConstruct;

/**
 * Thrown when during (post-)construction of a dependency a problem
 * occurs.  Constructors that throw an exception, {@link PostConstruct}s
 * which trigger further dependency construction which eventually need
 * the current object under construction (causing a loop) and so on.
 */
public class InstanceCreationFailure extends InstanceResolutionFailure {

  /**
   * Constructs a new instance.
   *
   * @param accessibleObject the constructor, method or field involved, cannot be null
   * @param message a message
   * @param cause a {@link Throwable} to use as cause
   */
  public InstanceCreationFailure(AccessibleObject accessibleObject, String message, Throwable cause) {
    super(message + ": " + accessibleObject, cause);
  }

  /**
   * Constructs a new instance.
   *
   * @param accessibleObject the constructor, method or field involved, cannot be null
   * @param message a message
   */
  public InstanceCreationFailure(AccessibleObject accessibleObject, String message) {
    super(message + ": " + accessibleObject);
  }

  /**
   * Constructs a new instance.
   *
   * @param key a {@link Key}, cannot be null
   * @param message a message
   * @param cause a {@link Throwable} to use as cause
   */
  public InstanceCreationFailure(Key key, String message, Throwable cause) {
    super(message + ": " + key, cause);
  }

  /**
   * Constructs a new instance.
   *
   * @param type type involved, cannot be null
   * @param message a message
   * @param cause a {@link Throwable} to use as cause
   */
  public InstanceCreationFailure(Type type, String message, Throwable cause) {
    super(message + ": " + type, cause);
  }

  /**
   * Constructs a new instance.
   *
   * @param type type involved, cannot be null
   * @param message a message
   */
  public InstanceCreationFailure(Type type, String message) {
    super(message + ": " + type);
  }

  @Override
  public InstanceCreationException toRuntimeException() {
    return new InstanceCreationException(getMessage(), getCause());
  }
}
