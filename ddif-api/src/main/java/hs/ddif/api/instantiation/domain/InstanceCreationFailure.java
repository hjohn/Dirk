package hs.ddif.api.instantiation.domain;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Thrown when during (post-)construction of a dependency a problem
 * occurs.  Constructors that throw an exception, setters or post constructors
 * which trigger further dependency construction which eventually need
 * the current object under construction (causing a loop) and so on.
 */
public class InstanceCreationFailure extends InstanceResolutionFailure {

  /**
   * Constructs a new instance.
   *
   * @param member the constructor, method or field involved, cannot be {@code null}
   * @param message a message
   * @param cause a {@link Throwable} to use as cause
   */
  public InstanceCreationFailure(Member member, String message, Throwable cause) {
    super(describe(member) + " " + message, cause);
  }

  /**
   * Constructs a new instance.
   *
   * @param key a {@link Key}, cannot be {@code null}
   * @param message a message
   * @param cause a {@link Throwable} to use as cause
   */
  public InstanceCreationFailure(Key key, String message, Throwable cause) {
    super("[" + key + "] " + message, cause);
  }

  /**
   * Constructs a new instance.
   *
   * @param type type involved, cannot be {@code null}
   * @param message a message
   * @param cause a {@link Throwable} to use as cause
   */
  public InstanceCreationFailure(Type type, String message, Throwable cause) {
    super("[" + type + "] " + message, cause);
  }

  /**
   * Constructs a new instance.
   *
   * @param type type involved, cannot be {@code null}
   * @param message a message
   */
  public InstanceCreationFailure(Type type, String message) {
    super("[" + type + "] " + message);
  }

  /**
   * Constructs a new instance.
   *
   * @param message a message
   * @param cause a {@link Throwable} to use as cause
   */
  protected InstanceCreationFailure(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public InstanceCreationException toRuntimeException() {
    return new InstanceCreationException(getMessage(), this);
  }

  private static String describe(Member member) {
    return (member instanceof Method ? "Method" : member instanceof Field ? "Field" : "Constructor") + " [" + member + "]";
  }
}
